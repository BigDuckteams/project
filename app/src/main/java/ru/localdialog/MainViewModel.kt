package ru.localdialog

import android.app.Application
import android.content.ContentResolver
import android.content.Intent
import android.media.MediaPlayer
import android.net.Uri
import android.provider.OpenableColumns
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.localdialog.data.AppSettings
import ru.localdialog.data.ModelCatalog
import ru.localdialog.data.SelectedAudio
import ru.localdialog.data.SpeechModel
import ru.localdialog.data.Transcript
import ru.localdialog.export.ExportFormat
import ru.localdialog.export.TranscriptExporter
import ru.localdialog.processing.TranscriptionService
import ru.localdialog.recording.AudioRecordingService
import java.io.File
import java.util.UUID

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as DialogApplication
    val transcripts = app.store.transcripts
    val settings = app.settingsStore.settings
    val download = app.models.download
    val modelRevision = app.models.revision
    val processing = TranscriptionService.progress
    val processingResult = TranscriptionService.result
    val recording = AudioRecordingService.state

    private val _screen = MutableStateFlow<Screen>(Screen.Home)
    val screen = _screen.asStateFlow()
    private val _selectedAudio = MutableStateFlow<SelectedAudio?>(null)
    val selectedAudio = _selectedAudio.asStateFlow()
    private val _notice = MutableStateFlow<String?>(null)
    val notice = _notice.asStateFlow()
    private val _playback = MutableStateFlow(PlaybackState())
    val playback = _playback.asStateFlow()
    private val _search = MutableStateFlow("")
    val search = _search.asStateFlow()
    private var player: MediaPlayer? = null
    private var playbackJob: Job? = null
    private var modelJob: Job? = null

    init {
        viewModelScope.launch { app.store.load() }
    }

    fun navigate(screen: Screen) {
        _screen.value = screen
        if (screen !is Screen.Details) stopPlayback()
    }

    fun consumeNotice() { _notice.value = null }
    fun clearProcessingResult() = TranscriptionService.clearResult()
    fun setSearch(value: String) { _search.value = value }

    fun selectAudio(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val resolver = app.contentResolver
                runCatching {
                    resolver.takePersistableUriPermission(uri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                val name = resolver.query(uri, null, null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (index >= 0) cursor.getString(index) else null
                    } else null
                }?.substringAfterLast('/') ?: "Аудиозапись"
                val destination = File(app.filesDir, "imports/${UUID.randomUUID()}-${name.replace(Regex("[^\\p{L}\\p{N}._-]"), "_")}")
                destination.parentFile?.mkdirs()
                resolver.openInputStream(uri)?.use { input ->
                    destination.outputStream().buffered().use { output -> input.copyTo(output) }
                } ?: error("Не удалось открыть аудио")
                _selectedAudio.value = SelectedAudio(name, destination.absolutePath)
                _screen.value = Screen.NewTranscription
            } catch (error: Exception) {
                _notice.value = error.message ?: "Не удалось импортировать файл"
            }
        }
    }

    fun startTranscription() {
        val audio = _selectedAudio.value ?: return
        if (processing.value.fraction > 0 && processing.value.fraction < 1) return
        _screen.value = Screen.Processing
        val intent = Intent(app, TranscriptionService::class.java)
            .putExtra(TranscriptionService.EXTRA_PATH, audio.localPath)
            .putExtra(TranscriptionService.EXTRA_TITLE, audio.displayName)
        ContextCompat.startForegroundService(app, intent)
    }

    fun cancelTranscription() {
        app.startService(Intent(app, TranscriptionService::class.java).setAction(TranscriptionService.ACTION_CANCEL).putExtra(TranscriptionService.EXTRA_ID, processing.value.detail))
        _screen.value = Screen.Home
    }

    fun downloadModel(model: SpeechModel) {
        if (modelJob?.isActive == true) return
        modelJob = viewModelScope.launch {
            try {
                app.models.install(model)
                app.settingsStore.update { it.copy(selectedModelId = model.id) }
                _notice.value = "${model.name} готова к работе без интернета"
            } catch (error: Exception) {
                _notice.value = error.message ?: "Ошибка загрузки"
            }
        }
    }

    fun deleteModel(model: SpeechModel) {
        try {
            app.models.deleteModel(model)
            _notice.value = "Модель удалена с устройства"
        } catch (error: Exception) {
            _notice.value = error.message
        }
    }

    fun updateSettings(transform: (AppSettings) -> AppSettings) = app.settingsStore.update(transform)

    fun controlRecording(action: String) {
        ContextCompat.startForegroundService(app, Intent(app, AudioRecordingService::class.java).setAction(action))
    }

    fun startRecording() {
        _screen.value = Screen.Recorder
        if (recording.value.active) return
        ContextCompat.startForegroundService(app, Intent(app, AudioRecordingService::class.java))
    }

    fun useRecording() {
        val state = recording.value
        val path = state.filePath ?: return
        _selectedAudio.value = SelectedAudio("Запись ${java.text.SimpleDateFormat("dd.MM.yyyy HH:mm", java.util.Locale.getDefault()).format(java.util.Date())}.wav", path)
        _screen.value = Screen.NewTranscription
    }

    fun deleteTranscript(transcript: Transcript) {
        viewModelScope.launch {
            stopPlayback()
            app.store.delete(transcript)
            _screen.value = Screen.Home
        }
    }

    fun renameSpeaker(transcript: Transcript, speaker: Int, name: String) {
        val speakers = transcript.speakers.toMutableList()
        if (speaker !in speakers.indices) return
        speakers[speaker] = name.trim().ifEmpty { "Говорящий ${speaker + 1}" }
        viewModelScope.launch { app.store.save(transcript.copy(speakers = speakers)) }
    }

    fun editTurn(transcript: Transcript, index: Int, text: String) {
        if (index !in transcript.turns.indices || text.isBlank()) return
        val turns = transcript.turns.toMutableList()
        turns[index] = turns[index].copy(text = text.trim())
        viewModelScope.launch { app.store.save(transcript.copy(turns = turns)) }
    }

    fun reassignTurn(transcript: Transcript, index: Int, speaker: Int) {
        if (index !in transcript.turns.indices || speaker !in transcript.speakers.indices) return
        val turns = transcript.turns.toMutableList()
        turns[index] = turns[index].copy(speaker = speaker)
        viewModelScope.launch { app.store.save(transcript.copy(turns = turns)) }
    }

    fun export(transcript: Transcript, format: ExportFormat) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                TranscriptExporter.export(app, transcript, format)
                _notice.value = "Сохранено в Загрузки/Тихо"
            } catch (error: Exception) {
                _notice.value = error.message ?: "Не удалось экспортировать"
            }
        }
    }

    fun playPause(transcript: Transcript, seekTo: Float? = null) {
        val current = _playback.value
        if (current.transcriptId == transcript.id && player != null) {
            if (seekTo != null) {
                player?.seekTo((seekTo * 1000).toInt())
            } else if (player?.isPlaying == true) {
                player?.pause()
                _playback.value = current.copy(playing = false)
            } else {
                player?.start()
                _playback.value = current.copy(playing = true)
            }
            return
        }
        stopPlayback()
        try {
            player = MediaPlayer().apply {
                setDataSource(transcript.audioPath)
                prepare()
                seekTo(((seekTo ?: 0f) * 1000).toInt())
                setOnCompletionListener {
                    _playback.value = _playback.value.copy(playing = false, position = 0f)
                }
                start()
            }
            _playback.value = PlaybackState(transcript.id, true, seekTo ?: 0f)
            playbackJob = viewModelScope.launch {
                while (player != null) {
                    _playback.value = _playback.value.copy(position = (player?.currentPosition ?: 0) / 1000f)
                    delay(250)
                }
            }
        } catch (error: Exception) {
            _notice.value = "Не удалось воспроизвести аудио"
            stopPlayback()
        }
    }

    fun seek(transcript: Transcript, position: Float) {
        playPause(transcript, position)
        _playback.value = _playback.value.copy(position = position)
    }

    fun stopPlayback() {
        playbackJob?.cancel()
        runCatching { player?.stop() }
        player?.release()
        player = null
        _playback.value = PlaybackState()
    }

    override fun onCleared() {
        stopPlayback()
        modelJob?.cancel()
        super.onCleared()
    }
}

sealed interface Screen {
    data object Home : Screen
    data object NewTranscription : Screen
    data object Processing : Screen
    data object Models : Screen
    data object Recorder : Screen
    data object Settings : Screen
    data class Details(val id: String) : Screen
}

data class PlaybackState(val transcriptId: String? = null, val playing: Boolean = false, val position: Float = 0f)

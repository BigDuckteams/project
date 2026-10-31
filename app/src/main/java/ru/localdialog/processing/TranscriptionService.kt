package ru.localdialog.processing

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.IBinder
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.localdialog.DialogApplication
import ru.localdialog.MainActivity
import ru.localdialog.R
import ru.localdialog.audio.AudioDecoder
import ru.localdialog.data.ModelCatalog
import ru.localdialog.data.ProcessingProgress
import ru.localdialog.data.ProcessingStage
import ru.localdialog.data.Transcript
import java.util.UUID

class TranscriptionService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var job: Job? = null
    private var completedId: String? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_CANCEL) {
            intent.getStringExtra(EXTRA_ID)?.let(CancellationRegistry::cancel)
            job?.cancel()
            return START_NOT_STICKY
        }
        if (job?.isActive == true) return START_NOT_STICKY
        val path = intent?.getStringExtra(EXTRA_PATH) ?: return START_NOT_STICKY
        val title = intent.getStringExtra(EXTRA_TITLE) ?: "Новая расшифровка"
        val id = intent.getStringExtra(EXTRA_ID) ?: UUID.randomUUID().toString()
        completedId = null
        startForegroundWithNotification("Подготавливаем аудио", 0)
        job = scope.launch {
            val cancellation = CancellationRegistry.register(id)
            val application = application as DialogApplication
            val settings = application.settingsStore.settings.value
            val model = ModelCatalog.byId(settings.selectedModelId)
            try {
                if (!application.models.isInstalled(model)) application.models.install(model)
                update(ProcessingProgress(ProcessingStage.DECODING, 0.02f, "Конвертация в 16 кГц · моно"))
                val samples = AudioDecoder().decodeTo16kMono(path)
                val transcriber = GigaAmTranscriber()
                checkCancellation(cancellation)
                val turns = transcriber.transcribe(
                    samples = samples,
                    model = model,
                    speechDirectory = application.models.speechDirectory(model),
                    diarizationDirectory = application.models.diarizationDirectory(),
                    settings = settings,
                    cancellation = cancellation,
                    onProgress = { progress -> if (progress.fraction >= _progress.value.fraction || progress.stage != _progress.value.stage) update(progress) },
                )
                val speakerIds = turns.map { it.speaker }.distinct().sorted()
                val speakerLabels = speakerIds.associateWith { "Говорящий ${speakerIds.indexOf(it) + 1}" }
                val normalizedTurns = turns.map { it.copy(speaker = speakerIds.indexOf(it.speaker)) }
                val transcript = Transcript(
                    id = id,
                    title = title.substringBeforeLast('.'),
                    audioPath = path,
                    durationSeconds = samples.size.toFloat() / 16_000,
                    modelId = model.id,
                    speakers = speakerIds.map { speakerLabels.getValue(it) },
                    turns = normalizedTurns,
                )
                application.store.save(transcript)
                completedId = id
                update(ProcessingProgress(ProcessingStage.DONE, 1f, id))
                withContext(Dispatchers.Main) {
                    _result.value = Result.success(id)
                }
            } catch (cancelled: CancellationException) {
                _progress.value = ProcessingProgress()
                _result.value = Result.failure(Exception("Обработка отменена"))
            } catch (error: Throwable) {
                _result.value = Result.failure(Exception(error.message ?: "Не удалось расшифровать аудио", error))
            } finally {
                CancellationRegistry.remove(id)
                _progress.value = ProcessingProgress(ProcessingStage.DONE, 1f, completedId ?: "")
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
        return START_NOT_STICKY
    }

    private fun update(progress: ProcessingProgress) {
        _progress.value = progress
        val percent = (progress.fraction.coerceIn(0f, 1f) * 100).toInt()
        startForegroundWithNotification(progress.stage.title, percent)
    }

    private fun startForegroundWithNotification(text: String, percent: Int) {
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(NotificationChannel(CHANNEL_ID, "Расшифровка", NotificationManager.IMPORTANCE_LOW))
        val launch = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher)
            .setContentTitle("Тихо · обработка на устройстве")
            .setContentText(text)
            .setContentIntent(launch)
            .setProgress(100, percent, percent == 0)
            .setOngoing(true)
            .setSilent(true)
            .build()
        startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    companion object {
        const val EXTRA_PATH = "path"
        const val EXTRA_TITLE = "title"
        const val EXTRA_ID = "id"
        const val ACTION_CANCEL = "ru.localdialog.processing.CANCEL"
        private const val CHANNEL_ID = "transcription"
        private const val NOTIFICATION_ID = 42
        private val _progress = MutableStateFlow(ProcessingProgress())
        val progress = _progress.asStateFlow()
        private val _result = MutableStateFlow<Result<String>?>(null)
        val result = _result.asStateFlow()
        fun clearResult() { _result.value = null }
    }
}

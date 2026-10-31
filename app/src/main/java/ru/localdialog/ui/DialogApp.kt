package ru.localdialog.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import ru.localdialog.DialogApplication
import ru.localdialog.MainViewModel
import ru.localdialog.Screen
import ru.localdialog.recording.AudioRecordingService

@Composable
fun DialogApp(viewModel: MainViewModel, darkTheme: Boolean, onRequestRecording: () -> Unit) {
    LocalDialogTheme(darkTheme = darkTheme) {
        val screen by viewModel.screen.collectAsState()
        val transcripts by viewModel.transcripts.collectAsState()
        val selectedAudio by viewModel.selectedAudio.collectAsState()
        val settings by viewModel.settings.collectAsState()
        val download by viewModel.download.collectAsState()
        val revision by viewModel.modelRevision.collectAsState()
        val progress by viewModel.processing.collectAsState()
        val result by viewModel.processingResult.collectAsState()
        val recording by viewModel.recording.collectAsState()
        val playback by viewModel.playback.collectAsState()
        val notice by viewModel.notice.collectAsState()
        val snackbar = remember { SnackbarHostState() }
        val application = LocalContext.current.applicationContext as DialogApplication

        LaunchedEffect(notice) {
            notice?.let { snackbar.showSnackbar(it); viewModel.consumeNotice() }
        }
        LaunchedEffect(recording.error) {
            recording.error?.let { snackbar.showSnackbar(it) }
        }
        LaunchedEffect(result) {
            result?.onSuccess { id ->
                viewModel.navigate(Screen.Details(id))
                viewModel.clearProcessingResult()
            }?.onFailure {
                snackbar.showSnackbar(it.message ?: "Ошибка обработки")
                viewModel.navigate(Screen.Home)
                viewModel.clearProcessingResult()
            }
        }

        Scaffold(snackbarHost = { SnackbarHost(snackbar) }) { padding ->
            Box(Modifier.fillMaxSize().padding(padding).padding(horizontal = 20.dp)) {
                when (val current = screen) {
                    Screen.Home -> HomeScreen(
                        transcripts = transcripts,
                        onImport = { viewModel.navigate(Screen.NewTranscription) },
                        onRecord = onRequestRecording,
                        onOpen = { viewModel.navigate(Screen.Details(it.id)) },
                        onModels = { viewModel.navigate(Screen.Models) },
                        onSettings = { viewModel.navigate(Screen.Settings) },
                    )
                    Screen.NewTranscription -> selectedAudio?.let { audio ->
                        NewTranscriptionScreen(
                            audio = audio,
                            settings = settings,
                            installed = application.models.isInstalled(ru.localdialog.data.ModelCatalog.byId(settings.selectedModelId)),
                            onSettings = viewModel::updateSettingsValue,
                            onDownload = { viewModel.downloadModel(ru.localdialog.data.ModelCatalog.byId(settings.selectedModelId)) },
                            onBack = { viewModel.navigate(Screen.Home) },
                            onStart = viewModel::startTranscription,
                        )
                    } ?: ImportChoice(onImport = viewModel::selectAudio, onRecord = onRequestRecording, onBack = { viewModel.navigate(Screen.Home) })
                    Screen.Processing -> ProcessingScreen(progress, download, viewModel::cancelTranscription)
                    Screen.Models -> ModelsScreen(
                        repository = application.models,
                        selectedId = settings.selectedModelId,
                        download = download,
                        revision = revision,
                        onSelect = { model -> viewModel.updateSettings { it.copy(selectedModelId = model.id) } },
                        onDownload = viewModel::downloadModel,
                        onDelete = viewModel::deleteModel,
                        onBack = { viewModel.navigate(Screen.Home) },
                    )
                    Screen.Recorder -> RecorderScreen(
                        state = recording,
                        onStart = { if (recording.active) Unit else onRequestRecording() },
                        onPause = { viewModel.controlRecording(AudioRecordingService.ACTION_PAUSE) },
                        onResume = { viewModel.controlRecording(AudioRecordingService.ACTION_RESUME) },
                        onStop = { viewModel.controlRecording(AudioRecordingService.ACTION_STOP) },
                        onCancel = { viewModel.controlRecording(AudioRecordingService.ACTION_CANCEL); viewModel.navigate(Screen.Home) },
                        onUse = viewModel::useRecording,
                        onBack = { viewModel.navigate(Screen.Home) },
                    )
                    Screen.Settings -> SettingsScreen(
                        settings = settings,
                        repository = application.models,
                        revision = revision,
                        onSettings = viewModel::updateSettingsValue,
                        onBack = { viewModel.navigate(Screen.Home) },
                    )
                    is Screen.Details -> transcripts.firstOrNull { it.id == current.id }?.let { transcript ->
                        TranscriptScreen(
                            transcript = transcript,
                            playback = playback,
                            onPlay = { viewModel.playPause(transcript) },
                            onSeek = { viewModel.seek(transcript, it) },
                            onRenameSpeaker = { speaker, name -> viewModel.renameSpeaker(transcript, speaker, name) },
                            onEditTurn = { index, text -> viewModel.editTurn(transcript, index, text) },
                            onReassignTurn = { index, speaker -> viewModel.reassignTurn(transcript, index, speaker) },
                            onExport = { viewModel.export(transcript, it) },
                            onDelete = { viewModel.deleteTranscript(transcript) },
                            onBack = { viewModel.navigate(Screen.Home) },
                        )
                    } ?: HomeScreen(transcripts, {}, onRequestRecording, {}, {}, {})
                }
            }
        }
    }
}

private fun MainViewModel.updateSettingsValue(value: ru.localdialog.data.AppSettings) = updateSettings { value }

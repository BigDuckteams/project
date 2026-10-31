package ru.localdialog.recording

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import ru.localdialog.MainActivity
import ru.localdialog.R
import java.io.File
import java.io.RandomAccessFile
import kotlin.math.ln

class AudioRecordingService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var recordingJob: Job? = null
    private var output: File? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_PAUSE -> _state.value = _state.value.copy(paused = true)
            ACTION_RESUME -> _state.value = _state.value.copy(paused = false)
            ACTION_STOP -> _state.value = _state.value.copy(stopRequested = true, paused = false)
            ACTION_CANCEL -> {
                output?.delete()
                _state.value = RecordingState()
                recordingJob?.cancel()
            }
            else -> if (recordingJob?.isActive != true) startRecording()
        }
        return START_NOT_STICKY
    }

    private fun startRecording() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            _state.value = RecordingState(error = "Разрешите доступ к микрофону")
            stopSelf()
            return
        }
        startForegroundWithNotification("Идёт запись · 00:00")
        val directory = File(filesDir, "recordings").apply { mkdirs() }
        val file = File(directory, "recording-${System.currentTimeMillis()}.wav")
        output = file
        _state.value = RecordingState(active = true, filePath = file.absolutePath)
        recordingJob = scope.launch {
            var recorder: AudioRecord? = null
            var dataBytes = 0
            var seconds = 0
            var samplesSinceSecond = 0
            try {
                val minimum = AudioRecord.getMinBufferSize(SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
                require(minimum > 0) { "Микрофон недоступен" }
                recorder = AudioRecord(
                    MediaRecorder.AudioSource.MIC,
                    SAMPLE_RATE,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    maxOf(minimum, SAMPLE_RATE * 2),
                )
                require(recorder.state == AudioRecord.STATE_INITIALIZED) { "Не удалось запустить микрофон" }
                RandomAccessFile(file, "rw").use { wav ->
                    wav.setLength(44)
                    val buffer = ByteArray(SAMPLE_RATE / 5 * 2)
                    recorder.startRecording()
                    while (isActive && !_state.value.stopRequested && dataBytes < MAX_BYTES) {
                        if (_state.value.paused) {
                            recorder.stop()
                            while (_state.value.paused && !_state.value.stopRequested && isActive) kotlinx.coroutines.delay(100)
                            if (isActive && !_state.value.stopRequested) recorder.startRecording()
                            if (_state.value.stopRequested) break
                        }
                        val count = recorder.read(buffer, 0, buffer.size)
                        if (count > 0) {
                            wav.write(buffer, 0, count)
                            dataBytes += count
                            samplesSinceSecond += count / 2
                            val level = calculateLevel(buffer, count)
                            _state.value = _state.value.copy(level = level, bytes = dataBytes.toLong())
                            if (samplesSinceSecond >= SAMPLE_RATE) {
                                seconds += samplesSinceSecond / SAMPLE_RATE
                                samplesSinceSecond %= SAMPLE_RATE
                                _state.value = _state.value.copy(seconds = seconds)
                                startForegroundWithNotification("Идёт запись · ${formatElapsed(seconds)}")
                            }
                        }
                    }
                }
                finishWav(file, dataBytes)
                _state.value = _state.value.copy(active = false, completed = true, paused = false, level = 0f)
            } catch (error: Exception) {
                if (isActive) {
                    file.delete()
                    _state.value = RecordingState(error = error.message ?: "Ошибка записи")
                }
            } finally {
                runCatching { recorder?.stop() }
                recorder?.release()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
    }

    private fun calculateLevel(buffer: ByteArray, count: Int): Float {
        var sum = 0.0
        var index = 0
        while (index + 1 < count) {
            val sample = (buffer[index + 1].toInt() shl 8) or (buffer[index].toInt() and 0xff)
            sum += sample.toDouble() * sample
            index += 2
        }
        val rms = kotlin.math.sqrt(sum / (count / 2).coerceAtLeast(1)) / Short.MAX_VALUE
        return ((ln(1 + rms * 100) / ln(101.0)) * 1.7).toFloat().coerceIn(0f, 1f)
    }

    private fun finishWav(file: File, dataBytes: Int) {
        RandomAccessFile(file, "rw").use { wav ->
            wav.seek(0)
            wav.writeBytes("RIFF")
            wav.writeInt(Integer.reverseBytes(36 + dataBytes))
            wav.writeBytes("WAVEfmt ")
            wav.writeInt(Integer.reverseBytes(16))
            wav.writeShort(java.lang.Short.reverseBytes(1).toInt())
            wav.writeShort(java.lang.Short.reverseBytes(1).toInt())
            wav.writeInt(Integer.reverseBytes(SAMPLE_RATE))
            wav.writeInt(Integer.reverseBytes(SAMPLE_RATE * 2))
            wav.writeShort(java.lang.Short.reverseBytes(2).toInt())
            wav.writeShort(java.lang.Short.reverseBytes(16).toInt())
            wav.writeBytes("data")
            wav.writeInt(Integer.reverseBytes(dataBytes))
        }
    }

    private fun startForegroundWithNotification(text: String) {
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(NotificationChannel(CHANNEL_ID, "Запись голоса", NotificationManager.IMPORTANCE_LOW))
        val launch = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher)
            .setContentTitle("Тихо · локальная запись")
            .setContentText(text)
            .setContentIntent(launch)
            .setOngoing(true)
            .setSilent(true)
            .build()
        startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE)
    }

    override fun onDestroy() {
        recordingJob?.cancel()
        scope.cancel()
        super.onDestroy()
    }

    data class RecordingState(
        val active: Boolean = false,
        val paused: Boolean = false,
        val seconds: Int = 0,
        val bytes: Long = 0,
        val level: Float = 0f,
        val filePath: String? = null,
        val completed: Boolean = false,
        val error: String? = null,
        val stopRequested: Boolean = false,
    )

    companion object {
        private const val SAMPLE_RATE = 16_000
        private const val MAX_BYTES = SAMPLE_RATE * 2 * 6 * 60 * 60
        private const val CHANNEL_ID = "recording"
        private const val NOTIFICATION_ID = 41
        const val ACTION_PAUSE = "ru.localdialog.recording.PAUSE"
        const val ACTION_RESUME = "ru.localdialog.recording.RESUME"
        const val ACTION_STOP = "ru.localdialog.recording.STOP"
        const val ACTION_CANCEL = "ru.localdialog.recording.CANCEL"
        private val _state = MutableStateFlow(RecordingState())
        val state = _state.asStateFlow()

        fun formatElapsed(totalSeconds: Int): String = "%02d:%02d:%02d".format(
            totalSeconds / 3600, (totalSeconds % 3600) / 60, totalSeconds % 60,
        )
    }
}

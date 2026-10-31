package ru.localdialog.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ru.localdialog.recording.AudioRecordingService

@Composable
fun RecorderScreen(
    state: AudioRecordingService.RecordingState,
    onStart: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onStop: () -> Unit,
    onCancel: () -> Unit,
    onUse: () -> Unit,
    onBack: () -> Unit,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        BackHeader("Запись разговора", "16 кГц · моно · WAV", onBack)
        Spacer(Modifier.height(30.dp))
        SonicOrb(Modifier.size(210.dp), active = state.active && !state.paused, level = state.level)
        Spacer(Modifier.height(26.dp))
        Text(AudioRecordingService.formatElapsed(state.seconds), style = MaterialTheme.typography.displayMedium, fontWeight = FontWeight.Light)
        Text(
            when {
                state.paused -> "Пауза"
                state.active -> "Запись сохраняется на устройстве"
                state.completed -> "Запись готова к расшифровке"
                else -> "Расположите телефон между участниками"
            },
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(32.dp))
        if (!state.active && !state.completed) {
            Button(onClick = onStart, modifier = Modifier.fillMaxWidth().height(56.dp)) {
                Icon(Icons.Default.Mic, null)
                Spacer(Modifier.size(8.dp))
                Text("Начать запись")
            }
        } else if (state.active) {
            Row(horizontalArrangement = Arrangement.spacedBy(18.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedButton(onClick = onCancel) { Icon(Icons.Default.Delete, null); Text("Удалить") }
                FilledIconButton(onClick = if (state.paused) onResume else onPause, modifier = Modifier.size(64.dp)) {
                    Icon(if (state.paused) Icons.Default.PlayArrow else Icons.Default.Pause, if (state.paused) "Продолжить" else "Пауза", modifier = Modifier.size(30.dp))
                }
                FilledIconButton(onClick = onStop, colors = IconButtonDefaults.filledIconButtonColors(containerColor = MaterialTheme.colorScheme.tertiary)) {
                    Icon(Icons.Default.Stop, "Остановить")
                }
            }
        } else {
            Button(onClick = onUse, modifier = Modifier.fillMaxWidth().height(56.dp)) {
                Icon(Icons.Default.Check, null)
                Spacer(Modifier.size(8.dp))
                Text("Расшифровать запись")
            }
            Spacer(Modifier.height(10.dp))
            OutlinedButton(onClick = onStart, modifier = Modifier.fillMaxWidth()) { Text("Записать заново") }
        }
        Spacer(Modifier.height(24.dp))
        Text("Для лучшего разделения голосов избегайте сильного шума и перекрывающейся речи. Во время записи можно заблокировать экран.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

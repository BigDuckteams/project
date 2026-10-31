package ru.localdialog.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ru.localdialog.data.DownloadState
import ru.localdialog.data.ProcessingProgress
import kotlin.math.roundToInt

@Composable
fun ProcessingScreen(progress: ProcessingProgress, download: DownloadState?, onCancel: () -> Unit) {
    val activeDownload = download != null
    val fraction = if (activeDownload) download!!.fraction else progress.fraction
    val animated by animateFloatAsState(fraction.coerceIn(0f, 1f), label = "progress")
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(Modifier.height(32.dp))
        SonicOrb(Modifier.size(190.dp), active = true, level = animated)
        Spacer(Modifier.height(28.dp))
        Text(
            if (activeDownload) "Подготавливаем модели" else progress.stage.title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            if (activeDownload) download!!.target else "GigaAM и диаризация · без облака",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(28.dp))
        LinearProgressIndicator(progress = { animated }, modifier = Modifier.fillMaxWidth().height(8.dp))
        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(if (activeDownload) download!!.detail else progress.detail, style = MaterialTheme.typography.bodySmall)
            Text("${(animated * 100).roundToInt()}%", style = MaterialTheme.typography.labelMedium)
        }
        Spacer(Modifier.height(32.dp))
        Text("Длительная запись может обрабатываться несколько минут. Заряда батареи хватит надолго — используется CPU.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(20.dp))
        OutlinedButton(onClick = onCancel) { Text(if (activeDownload) "Скрыть" else "Отменить") }
    }
}

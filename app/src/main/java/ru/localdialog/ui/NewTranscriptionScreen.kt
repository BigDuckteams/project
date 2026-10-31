package ru.localdialog.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ru.localdialog.data.AppSettings
import ru.localdialog.data.ModelCatalog
import ru.localdialog.data.SelectedAudio
import kotlin.math.roundToInt

@Composable
fun NewTranscriptionScreen(
    audio: SelectedAudio,
    settings: AppSettings,
    installed: Boolean,
    onSettings: (AppSettings) -> Unit,
    onDownload: () -> Unit,
    onBack: () -> Unit,
    onStart: () -> Unit,
) {
    val model = ModelCatalog.byId(settings.selectedModelId)
    Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(20.dp)) {
        BackHeader("Новая расшифровка", onBack = onBack)
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.AudioFile, null, modifier = Modifier.size(36.dp), tint = MaterialTheme.colorScheme.secondary)
                Spacer(Modifier.size(12.dp))
                Column {
                    Text(audio.displayName, fontWeight = FontWeight.SemiBold)
                    Text("Аудио скопировано в защищённое хранилище", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SectionLabel("Модель распознавания")
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
                Column(Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Memory, null)
                        Spacer(Modifier.size(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(model.name, fontWeight = FontWeight.SemiBold)
                            Text(model.family, style = MaterialTheme.typography.bodySmall)
                        }
                        if (installed) Icon(Icons.Default.CheckCircle, "Установлена", tint = MaterialTheme.colorScheme.secondary)
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(model.description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Column(Modifier.padding(18.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Определять участников автоматически", fontWeight = FontWeight.Medium)
                        Text("Кластеризация по характеристикам голоса", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(checked = settings.automaticSpeakers, onCheckedChange = { onSettings(settings.copy(automaticSpeakers = it)) })
                }
                HorizontalDivider(Modifier.padding(vertical = 16.dp))
                if (settings.automaticSpeakers) {
                    Text("Чувствительность разделения: ${(settings.clusterThreshold * 100).roundToInt()}%", fontWeight = FontWeight.Medium)
                    Slider(value = settings.clusterThreshold, onValueChange = { onSettings(settings.copy(clusterThreshold = it)) }, valueRange = 0.1f..0.95f)
                    Text("Выше — меньше участников, ниже — больше", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    Text("Количество участников: ${settings.speakerCount}", fontWeight = FontWeight.Medium)
                    Slider(value = settings.speakerCount.toFloat(), onValueChange = { onSettings(settings.copy(speakerCount = it.roundToInt())) }, valueRange = 1f..12f, steps = 10)
                }
            }
        }
        if (!installed) {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.12f))) {
                Column(Modifier.padding(18.dp)) {
                    Text("Перед первым запуском", fontWeight = FontWeight.SemiBold)
                    Text("Нужно один раз скачать модель речи и диаризации. После этого интернет не требуется.", style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(12.dp))
                    OutlinedButton(onClick = onDownload) {
                        Icon(Icons.Default.Download, null)
                        Spacer(Modifier.size(8.dp))
                        Text("Скачать ${model.name}")
                    }
                }
            }
        }
        Button(onClick = onStart, modifier = Modifier.fillMaxWidth().height(54.dp)) {
            Text("Начать расшифровку")
        }
        Text("Не закрывайте приложение во время первой загрузки модели. Обработка продолжится в фоне.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(16.dp))
    }
}

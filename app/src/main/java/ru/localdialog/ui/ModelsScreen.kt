package ru.localdialog.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ru.localdialog.data.DownloadState
import ru.localdialog.data.ModelCatalog
import ru.localdialog.data.ModelRepository
import ru.localdialog.data.SpeechModel

@Composable
fun ModelsScreen(
    repository: ModelRepository,
    selectedId: String,
    download: DownloadState?,
    revision: Int,
    onSelect: (SpeechModel) -> Unit,
    onDownload: (SpeechModel) -> Unit,
    onDelete: (SpeechModel) -> Unit,
    onBack: () -> Unit,
) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { BackHeader("Модели", "Всё работает локально после загрузки", onBack) }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                Column(Modifier.padding(18.dp)) {
                    Text("Диаризация общая для всех моделей", fontWeight = FontWeight.SemiBold)
                    Text("Pyannote Segmentation 3.0 + 3D-Speaker. Скачивается один раз вместе с любой моделью.", style = MaterialTheme.typography.bodySmall)
                    if (revision >= 0 && repository.isDiarizationInstalled()) {
                        Spacer(Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Check, null, modifier = Modifier.size(16.dp))
                            Text(" Установлена", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }
        }
        if (download != null) item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary)) {
                Column(Modifier.padding(18.dp)) {
                    Text("Загрузка моделей", color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.SemiBold)
                    Text(download.target, color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f), style = MaterialTheme.typography.bodySmall)
                    Spacer(Modifier.height(10.dp))
                    androidx.compose.material3.LinearProgressIndicator(progress = { download.fraction }, modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(6.dp))
                    Text("${(download.fraction * 100).toInt()}% · ${download.detail}", color = Lime, style = MaterialTheme.typography.labelMedium)
                }
            }
        }
        items(ModelCatalog.models, key = { it.id }) { model ->
            val installed = revision >= 0 && repository.isSpeechInstalled(model)
            val selected = selectedId == model.id
            Card(colors = CardDefaults.cardColors(containerColor = if (selected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface)) {
                Column(Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.Top) {
                        Column(Modifier.weight(1f)) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text(model.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                                if (model.recommended) Surface(color = Lime, shape = androidx.compose.foundation.shape.CircleShape) { Text("ВЫБОР", Modifier.padding(horizontal = 6.dp, vertical = 2.dp), style = MaterialTheme.typography.labelSmall) }
                            }
                            Text(model.family, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.secondary)
                        }
                        if (selected) Icon(Icons.Default.Check, "Выбрана", tint = MaterialTheme.colorScheme.primary)
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(model.description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        AssistChip(onClick = {}, label = { Text(model.language) })
                        AssistChip(onClick = {}, label = { Text(model.size) })
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (installed) {
                            OutlinedButton(onClick = { onSelect(model) }, enabled = !selected, modifier = Modifier.weight(1f)) { Text(if (selected) "Используется" else "Выбрать") }
                            IconButton(onClick = { onDelete(model) }, enabled = download == null) { Icon(Icons.Default.Delete, "Удалить модель") }
                        } else {
                            Button(onClick = { onDownload(model) }, enabled = download == null, modifier = Modifier.weight(1f)) {
                                if (download != null) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp) else Icon(Icons.Default.Download, null, Modifier.size(18.dp))
                                Spacer(Modifier.size(8.dp))
                                Text("Скачать")
                            }
                        }
                    }
                }
            }
        }
        item { Spacer(Modifier.height(16.dp)) }
    }
}

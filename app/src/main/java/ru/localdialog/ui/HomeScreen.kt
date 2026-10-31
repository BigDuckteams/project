package ru.localdialog.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.NorthEast
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.AudioFile
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import ru.localdialog.data.Transcript
import ru.localdialog.export.formatTimestamp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HomeScreen(
    transcripts: List<Transcript>,
    onImport: () -> Unit,
    onRecord: () -> Unit,
    onOpen: (Transcript) -> Unit,
    onModels: () -> Unit,
    onSettings: () -> Unit,
) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Тихо", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold)
                    Text("Голос становится диалогом", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(onClick = onModels) { Icon(Icons.Outlined.AudioFile, "Модели") }
                IconButton(onClick = onSettings) { Icon(Icons.Default.Settings, "Настройки") }
            }
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary), shape = RoundedCornerShape(28.dp)) {
                Column(Modifier.padding(22.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                        PrivacyBadge()
                        Icon(Icons.Default.NorthEast, null, tint = Lime)
                    }
                    Spacer(Modifier.height(18.dp))
                    Text("Каждому голосу —\nсвоё место", style = MaterialTheme.typography.headlineMedium, color = Lime, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(8.dp))
                    Text("GigaAM v3 и диаризация работают локально. Ваши разговоры не покидают телефон.", color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.76f))
                    Spacer(Modifier.height(20.dp))
                    Button(
                        onClick = onImport,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Lime, contentColor = Ink),
                    ) {
                        Icon(Icons.Default.Add, null)
                        Spacer(Modifier.size(8.dp))
                        Text("Выбрать аудиозапись")
                    }
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = onRecord,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onPrimary),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.3f)),
                    ) {
                        Icon(Icons.Default.Mic, null)
                        Spacer(Modifier.size(8.dp))
                        Text("Записать разговор")
                    }
                }
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                SectionLabel(if (transcripts.isEmpty()) "Начните с записи" else "Недавние расшифровки")
                if (transcripts.isNotEmpty()) Text("${transcripts.size}", style = MaterialTheme.typography.labelMedium)
            }
        }
        if (transcripts.isEmpty()) {
            item {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                    Row(Modifier.padding(20.dp), horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        SonicOrb(Modifier.size(72.dp))
                        Column {
                            Text("Поддерживаются WAV, MP3, M4A, OGG и видео", fontWeight = FontWeight.Medium)
                            Spacer(Modifier.height(4.dp))
                            Text("Длительность — до 6 часов", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        } else {
            items(transcripts, key = { it.id }) { transcript ->
                TranscriptCard(transcript, onClick = { onOpen(transcript) })
            }
        }
    }
}

@Composable
private fun TranscriptCard(transcript: Transcript, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(20.dp),
    ) {
        Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(46.dp).clip(RoundedCornerShape(14.dp)).background(MaterialTheme.colorScheme.secondaryContainer), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.AudioFile, null, tint = MaterialTheme.colorScheme.primary)
            }
            Spacer(Modifier.size(14.dp))
            Column(Modifier.weight(1f)) {
                Text(transcript.title, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(5.dp))
                Text(
                    "${SimpleDateFormat("d MMM · HH:mm", Locale.forLanguageTag("ru")).format(Date(transcript.createdAt))}  ·  ${formatTimestamp(transcript.durationSeconds)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(6.dp))
                Text("${transcript.speakers.size} участника · ${transcript.turns.size} реплик", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
            }
            Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

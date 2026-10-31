package ru.localdialog.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.IosShare
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import ru.localdialog.PlaybackState
import ru.localdialog.data.ModelCatalog
import ru.localdialog.data.Transcript
import ru.localdialog.export.ExportFormat
import ru.localdialog.export.formatTimestamp

@Composable
fun TranscriptScreen(
    transcript: Transcript,
    playback: PlaybackState,
    onPlay: () -> Unit,
    onSeek: (Float) -> Unit,
    onRenameSpeaker: (Int, String) -> Unit,
    onEditTurn: (Int, String) -> Unit,
    onReassignTurn: (Int, Int) -> Unit,
    onExport: (ExportFormat) -> Unit,
    onDelete: () -> Unit,
    onBack: () -> Unit,
) {
    var query by remember { mutableStateOf("") }
    var speakerFilter by remember { mutableIntStateOf(-1) }
    var exportOpen by remember { mutableStateOf(false) }
    var renameSpeaker by remember { mutableIntStateOf(-1) }
    var editTurn by remember { mutableIntStateOf(-1) }
    var deleteOpen by remember { mutableStateOf(false) }
    val visible = transcript.turns.mapIndexedNotNull { index, turn ->
        if ((speakerFilter < 0 || turn.speaker == speakerFilter) &&
            (query.isBlank() || turn.text.contains(query, true) || transcript.speakers.getOrNull(turn.speaker)?.contains(query, true) == true)
        ) index to turn else null
    }
    LazyColumn(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            BackHeader(transcript.title, "${ModelCatalog.byId(transcript.modelId).name} · ${formatTimestamp(transcript.durationSeconds)}", onBack) {
                IconButton(onClick = { exportOpen = true }) { Icon(Icons.Default.IosShare, "Экспорт") }
                DropdownMenu(exportOpen, { exportOpen = false }) {
                    ExportFormat.entries.forEach { format ->
                        DropdownMenuItem(text = { Text("Экспорт .${format.extension.uppercase()}") }, onClick = { exportOpen = false; onExport(format) })
                    }
                }
                IconButton(onClick = { deleteOpen = true }) { Icon(Icons.Default.Delete, "Удалить") }
            }
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary)) {
                Column(Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onPlay) {
                            Icon(if (playback.playing) Icons.Default.Pause else Icons.Default.PlayArrow, null, tint = Lime, modifier = Modifier.size(34.dp))
                        }
                        Column(Modifier.weight(1f)) {
                            Text(if (playback.playing) "Воспроизведение" else "Прослушать оригинал", color = MaterialTheme.colorScheme.onPrimary)
                            Text("${formatTimestamp(playback.position)} / ${formatTimestamp(transcript.durationSeconds)}", style = MaterialTheme.typography.bodySmall, color = Lime)
                        }
                    }
                    Slider(value = playback.position.coerceIn(0f, transcript.durationSeconds), onValueChange = onSeek, valueRange = 0f..transcript.durationSeconds.coerceAtLeast(0.1f))
                }
            }
        }
        item {
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                transcript.speakers.forEachIndexed { index, name ->
                    AssistChip(onClick = { renameSpeaker = index }, label = { Text(name) }, leadingIcon = { Icon(Icons.Default.Person, null, Modifier.size(16.dp)) }, trailingIcon = { Icon(Icons.Default.Edit, "Переименовать", Modifier.size(14.dp)) })
                }
            }
        }
        item {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Найти в расшифровке") },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                trailingIcon = { if (query.isNotEmpty()) IconButton(onClick = { query = "" }) { Icon(Icons.Default.Close, "Очистить") } },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            )
        }
        item {
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = speakerFilter < 0, onClick = { speakerFilter = -1 }, label = { Text("Все реплики") })
                transcript.speakers.forEachIndexed { index, name ->
                    FilterChip(selected = speakerFilter == index, onClick = { speakerFilter = if (speakerFilter == index) -1 else index }, label = { Text(name) })
                }
            }
        }
        itemsIndexed(visible, key = { _, pair -> pair.first }) { _, pair ->
            val (index, turn) = pair
            val name = transcript.speakers.getOrElse(turn.speaker) { "Говорящий ${turn.speaker + 1}" }
            TurnCard(
                name = name,
                text = turn.text,
                start = turn.start,
                end = turn.end,
                colorIndex = turn.speaker,
                onClick = { onSeek(turn.start) },
                onEdit = { editTurn = index },
                onReassign = { editTurn = index },
            )
        }
        if (visible.isEmpty()) item { Text("Ничего не найдено. Измените запрос или фильтр.", modifier = Modifier.padding(20.dp)) }
        item { Spacer(Modifier.height(24.dp)) }
    }
    if (renameSpeaker >= 0) {
        var value by remember(renameSpeaker) { mutableStateOf(transcript.speakers[renameSpeaker]) }
        AlertDialog(
            onDismissRequest = { renameSpeaker = -1 },
            title = { Text("Имя участника") },
            text = { OutlinedTextField(value, { value = it }, singleLine = true) },
            confirmButton = { TextButton(onClick = { onRenameSpeaker(renameSpeaker, value); renameSpeaker = -1 }) { Text("Сохранить") } },
            dismissButton = { TextButton(onClick = { renameSpeaker = -1 }) { Text("Отмена") } },
        )
    }
    if (editTurn >= 0 && editTurn < transcript.turns.size) {
        val turn = transcript.turns[editTurn]
        var value by remember(editTurn) { mutableStateOf(turn.text) }
        var speaker by remember(editTurn) { mutableIntStateOf(turn.speaker) }
        AlertDialog(
            onDismissRequest = { editTurn = -1 },
            title = { Text("Редактировать реплику") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(value, { value = it }, minLines = 3, label = { Text("Текст") })
                    Text("Участник", style = MaterialTheme.typography.labelMedium)
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        transcript.speakers.forEachIndexed { index, name -> FilterChip(speaker == index, { speaker = index }, label = { Text(name) }) }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { onEditTurn(editTurn, value); onReassignTurn(editTurn, speaker); editTurn = -1 }) { Text("Сохранить") } },
            dismissButton = { TextButton(onClick = { editTurn = -1 }) { Text("Отмена") } },
        )
    }
    if (deleteOpen) {
        AlertDialog(
            onDismissRequest = { deleteOpen = false },
            title = { Text("Удалить расшифровку?") },
            text = { Text("Текст и локальная копия аудио будут удалены без возможности восстановления.") },
            confirmButton = { TextButton(onClick = { deleteOpen = false; onDelete() }) { Text("Удалить") } },
            dismissButton = { TextButton(onClick = { deleteOpen = false }) { Text("Отмена") } },
        )
    }
}

@Composable
private fun TurnCard(name: String, text: String, start: Float, end: Float, colorIndex: Int, onClick: () -> Unit, onEdit: () -> Unit, onReassign: () -> Unit) {
    val colors = listOf(Moss, Terracotta, androidx.compose.ui.graphics.Color(0xFF7B6DA8), androidx.compose.ui.graphics.Color(0xFF477FA0), androidx.compose.ui.graphics.Color(0xFF9A7840))
    val accent = colors[colorIndex.mod(colors.size)]
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Row(Modifier.padding(start = 14.dp, top = 14.dp, end = 8.dp, bottom = 14.dp)) {
            Box(Modifier.padding(top = 4.dp).size(4.dp, 42.dp).background(accent, CircleShape))
            Spacer(Modifier.size(12.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(name, style = MaterialTheme.typography.labelLarge, color = accent, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                    Text(formatTimestamp(start), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Spacer(Modifier.height(6.dp))
                Text(text, style = MaterialTheme.typography.bodyLarge)
                Text("до ${formatTimestamp(end)}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = onEdit) { Icon(Icons.Default.Edit, "Редактировать реплику", modifier = Modifier.size(18.dp)) }
        }
    }
}

package ru.localdialog.export

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import kotlinx.serialization.json.Json
import ru.localdialog.data.Transcript
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class ExportFormat(val extension: String, val mimeType: String) {
    TXT("txt", "text/plain"),
    JSON("json", "application/json"),
    SRT("srt", "application/x-subrip"),
}

object TranscriptExporter {
    private val json = Json { prettyPrint = true; encodeDefaults = true }

    fun render(transcript: Transcript, format: ExportFormat): String = when (format) {
        ExportFormat.JSON -> json.encodeToString(Transcript.serializer(), transcript)
        ExportFormat.TXT -> buildString {
            appendLine(transcript.title)
            appendLine("Модель: ${transcript.modelId}")
            appendLine("Длительность: ${formatTimestamp(transcript.durationSeconds)}")
            appendLine()
            transcript.turns.forEach { turn ->
                val speaker = transcript.speakers.getOrElse(turn.speaker) { "Говорящий ${turn.speaker + 1}" }
                appendLine("[${formatTimestamp(turn.start)} – ${formatTimestamp(turn.end)}] $speaker")
                appendLine(turn.text)
                appendLine()
            }
        }
        ExportFormat.SRT -> buildString {
            transcript.turns.forEachIndexed { index, turn ->
                val speaker = transcript.speakers.getOrElse(turn.speaker) { "Говорящий ${turn.speaker + 1}" }
                appendLine(index + 1)
                appendLine("${srtTimestamp(turn.start)} --> ${srtTimestamp(turn.end)}")
                appendLine("$speaker: ${turn.text}")
                appendLine()
            }
        }
    }

    fun export(context: Context, transcript: Transcript, format: ExportFormat): Uri {
        val safeName = transcript.title.replace(Regex("[^\\p{L}\\p{N} _-]"), "_").take(80)
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, "$safeName.${format.extension}")
            put(MediaStore.MediaColumns.MIME_TYPE, format.mimeType)
            put(MediaStore.MediaColumns.RELATIVE_PATH, "Download/Тихо")
        }
        val collection = MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        val uri = context.contentResolver.insert(collection, values) ?: error("Не удалось создать файл экспорта")
        try {
            context.contentResolver.openOutputStream(uri)?.bufferedWriter()?.use {
                it.write(render(transcript, format))
            } ?: error("Не удалось открыть файл экспорта")
        } catch (error: Exception) {
            context.contentResolver.delete(uri, null, null)
            throw error
        }
        return uri
    }

    fun fileName(transcript: Transcript): String {
        val date = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(transcript.createdAt))
        return "$date ${transcript.title}"
    }

    private fun srtTimestamp(seconds: Float): String {
        val milliseconds = (seconds.coerceAtLeast(0f) * 1000).toLong()
        return "%02d:%02d:%02d,%03d".format(
            milliseconds / 3_600_000,
            milliseconds / 60_000 % 60,
            milliseconds / 1000 % 60,
            milliseconds % 1000,
        )
    }
}

fun formatTimestamp(seconds: Float): String {
    val total = seconds.coerceAtLeast(0f).toInt()
    return if (total >= 3600) {
        "%d:%02d:%02d".format(total / 3600, total % 3600 / 60, total % 60)
    } else {
        "%02d:%02d".format(total / 60, total % 60)
    }
}

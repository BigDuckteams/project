package ru.localdialog

import org.junit.Assert.assertTrue
import org.junit.Test
import ru.localdialog.data.DialogueTurn
import ru.localdialog.data.Transcript
import ru.localdialog.export.ExportFormat
import ru.localdialog.export.TranscriptExporter
import ru.localdialog.export.formatTimestamp

class TranscriptExporterTest {
    private val transcript = Transcript(
        id = "test",
        title = "Встреча",
        audioPath = "audio.wav",
        durationSeconds = 4.2f,
        modelId = "gigaam-v3-e2e-rnnt",
        speakers = listOf("Анна", "Иван"),
        turns = listOf(DialogueTurn(1, 1.25f, 4.2f, "Обсудим план.")),
    )

    @Test
    fun `renders readable text with speaker and timestamps`() {
        val text = TranscriptExporter.render(transcript, ExportFormat.TXT)
        assertTrue(text.contains("[00:01 – 00:04] Иван"))
        assertTrue(text.contains("Обсудим план."))
    }

    @Test
    fun `renders valid srt sequence`() {
        val srt = TranscriptExporter.render(transcript, ExportFormat.SRT)
        assertTrue(srt.startsWith("1\n00:00:01,250 --> 00:00:04,200\nИван: Обсудим план."))
    }

    @Test
    fun `formats hours without losing minutes`() {
        assertTrue(formatTimestamp(3661f) == "1:01:01")
    }
}

package ru.localdialog

import org.junit.Assert.assertEquals
import org.junit.Test
import ru.localdialog.processing.DialogueAssembler
import ru.localdialog.processing.SpeakerSpan
import ru.localdialog.processing.TextSpan

class DialogueAssemblerTest {
    private val speakers = listOf(
        SpeakerSpan(0f, 4f, 0),
        SpeakerSpan(5f, 9f, 1),
        SpeakerSpan(10f, 13f, 0),
    )

    @Test
    fun `assigns text by longest overlap and merges same speaker`() {
        val turns = DialogueAssembler.assemble(
            speakers,
            listOf(
                TextSpan(0f, 2f, "Добрый день."),
                TextSpan(3f, 7f, "Расскажите о задаче."),
                TextSpan(8f, 11f, "Начнём сегодня."),
            ),
        )

        assertEquals(2, turns.size)
        assertEquals(0, turns[0].speaker)
        assertEquals("Добрый день.", turns[0].text)
        assertEquals(1, turns[1].speaker)
        assertEquals("Расскажите о задаче. Начнём сегодня.", turns[1].text)
        assertEquals(11f, turns[1].end)
    }

    @Test
    fun `blank recognition is ignored`() {
        val turns = DialogueAssembler.assemble(speakers, listOf(TextSpan(0f, 3f, "  ")))
        assertEquals(emptyList<Any>(), turns)
    }
}

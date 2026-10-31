package ru.localdialog.processing

import ru.localdialog.data.DialogueTurn
import kotlin.math.max
import kotlin.math.min

data class SpeakerSpan(val start: Float, val end: Float, val speaker: Int)
data class TextSpan(val start: Float, val end: Float, val text: String)

object DialogueAssembler {
    fun assemble(spans: List<SpeakerSpan>, texts: List<TextSpan>): List<DialogueTurn> {
        val turns = mutableListOf<DialogueTurn>()
        for (text in texts.sortedBy { it.start }) {
            if (text.text.isBlank()) continue
            val overlaps = spans.mapNotNull { span ->
                val duration = min(text.end, span.end) - max(text.start, span.start)
                if (duration > 0f) Triple(span.speaker, duration, span.start) else null
            }
            val speaker = overlaps.maxWithOrNull(compareBy<Triple<Int, Float, Float>> { it.second }.thenBy { -it.third })?.first
                ?: nearestSpeaker(text, spans)
            turns += DialogueTurn(speaker, text.start, text.end, text.text.trim())
        }
        return mergeConsecutive(turns)
    }

    private fun nearestSpeaker(text: TextSpan, spans: List<SpeakerSpan>): Int {
        val midpoint = (text.start + text.end) / 2f
        return spans.minByOrNull { span ->
            when {
                midpoint < span.start -> span.start - midpoint
                midpoint > span.end -> midpoint - span.end
                else -> 0f
            }
        }?.speaker ?: 0
    }

    fun mergeConsecutive(turns: List<DialogueTurn>, maxGap: Float = 1.2f): List<DialogueTurn> {
        val result = mutableListOf<DialogueTurn>()
        turns.sortedBy { it.start }.forEach { turn ->
            val previous = result.lastOrNull()
            if (previous != null && previous.speaker == turn.speaker && turn.start - previous.end <= maxGap) {
                result[result.lastIndex] = previous.copy(
                    end = maxOf(previous.end, turn.end),
                    text = joinText(previous.text, turn.text),
                )
            } else {
                result += turn
            }
        }
        return result
    }

    private fun joinText(first: String, second: String): String = when {
        first.isBlank() -> second
        second.isBlank() -> first
        else -> "$first $second"
    }
}

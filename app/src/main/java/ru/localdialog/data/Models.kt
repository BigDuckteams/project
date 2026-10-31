package ru.localdialog.data

import kotlinx.serialization.Serializable

@Serializable
data class Transcript(
    val id: String,
    val title: String,
    val audioPath: String,
    val createdAt: Long = System.currentTimeMillis(),
    val durationSeconds: Float,
    val modelId: String,
    val speakers: List<String>,
    val turns: List<DialogueTurn>,
)

@Serializable
data class DialogueTurn(
    val speaker: Int,
    val start: Float,
    val end: Float,
    val text: String,
)

enum class ProcessingStage(val title: String) {
    DECODING("Подготавливаем аудио"),
    DIARIZATION("Определяем говорящих"),
    RECOGNITION("Распознаём речь"),
    MERGING("Собираем диалог"),
    DONE("Расшифровка готова"),
}

data class ProcessingProgress(
    val stage: ProcessingStage = ProcessingStage.DECODING,
    val fraction: Float = 0f,
    val detail: String = "",
)

data class SelectedAudio(val displayName: String, val localPath: String)

sealed interface AppNotice {
    data class Message(val text: String) : AppNotice
}

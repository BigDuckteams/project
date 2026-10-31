package ru.localdialog.data

enum class ModelKind { NEMO_TRANSDUCER, NEMO_CTC, WHISPER, SENSE_VOICE, DOLPHIN }

data class RemoteFile(val name: String, val url: String)

data class SpeechModel(
    val id: String,
    val name: String,
    val family: String,
    val description: String,
    val language: String,
    val size: String,
    val kind: ModelKind,
    val files: List<RemoteFile>,
    val recommended: Boolean = false,
) {
    val tokensName: String get() = files.first { it.name.endsWith("tokens.txt") }.name
}

object ModelCatalog {
    private const val HF = "https://huggingface.co"
    private const val RELEASES = "https://github.com/k2-fsa/sherpa-onnx/releases/download"

    private fun huggingFace(repo: String, vararg files: String) =
        files.map { RemoteFile(it, "$HF/$repo/resolve/main/$it") }

    private fun gigaV3(repo: String): List<RemoteFile> =
        huggingFace(repo, "encoder.int8.onnx", "decoder.onnx", "joiner.onnx", "tokens.txt")

    val models = listOf(
        SpeechModel(
            id = "gigaam-v3-e2e-rnnt",
            name = "GigaAM v3 E2E",
            family = "GigaAM · RNNT",
            description = "Лучший выбор для русской речи. Пунктуация и нормализация текста.",
            language = "Русский",
            size = "232 МБ",
            kind = ModelKind.NEMO_TRANSDUCER,
            files = gigaV3("csukuangfj/sherpa-onnx-nemo-transducer-punct-giga-am-v3-russian-2025-12-16"),
            recommended = true,
        ),
        SpeechModel(
            id = "gigaam-v3-rnnt",
            name = "GigaAM v3 RNNT",
            family = "GigaAM · RNNT",
            description = "Распознавание русской речи без дополнительной нормализации.",
            language = "Русский",
            size = "232 МБ",
            kind = ModelKind.NEMO_TRANSDUCER,
            files = gigaV3("csukuangfj/sherpa-onnx-nemo-transducer-giga-am-v3-russian-2025-12-16"),
        ),
        SpeechModel(
            id = "gigaam-v3-e2e-ctc",
            name = "GigaAM v3 E2E CTC",
            family = "GigaAM · CTC",
            description = "Быстрый CTC-декодер с пунктуацией и нормализацией.",
            language = "Русский",
            size = "225 МБ",
            kind = ModelKind.NEMO_CTC,
            files = huggingFace(
                "csukuangfj/sherpa-onnx-nemo-ctc-punct-giga-am-v3-russian-2025-12-16",
                "model.int8.onnx", "tokens.txt",
            ),
        ),
        SpeechModel(
            id = "gigaam-v2-rnnt",
            name = "GigaAM v2 RNNT",
            family = "GigaAM · RNNT",
            description = "Проверенная модель для русских голосовых сообщений и звонков.",
            language = "Русский",
            size = "240 МБ",
            kind = ModelKind.NEMO_TRANSDUCER,
            files = huggingFace(
                "csukuangfj/sherpa-onnx-nemo-transducer-giga-am-v2-russian-2025-04-19",
                "encoder.int8.onnx", "decoder.onnx", "joiner.onnx", "tokens.txt",
            ),
        ),
        SpeechModel(
            id = "whisper-small",
            name = "Whisper Small",
            family = "Whisper · multilingual",
            description = "Высокое качество для многоязычных разговоров.",
            language = "99 языков",
            size = "484 МБ",
            kind = ModelKind.WHISPER,
            files = huggingFace(
                "csukuangfj/sherpa-onnx-whisper-small",
                "small-encoder.int8.onnx", "small-decoder.int8.onnx", "small-tokens.txt",
            ),
        ),
        SpeechModel(
            id = "whisper-base",
            name = "Whisper Base",
            family = "Whisper · multilingual",
            description = "Баланс скорости и качества на разных языках.",
            language = "99 языков",
            size = "160 МБ",
            kind = ModelKind.WHISPER,
            files = huggingFace(
                "csukuangfj/sherpa-onnx-whisper-base",
                "base-encoder.int8.onnx", "base-decoder.int8.onnx", "base-tokens.txt",
            ),
        ),
        SpeechModel(
            id = "whisper-tiny",
            name = "Whisper Tiny",
            family = "Whisper · multilingual",
            description = "Самая компактная многоязычная модель для слабых устройств.",
            language = "99 языков",
            size = "103 МБ",
            kind = ModelKind.WHISPER,
            files = huggingFace(
                "csukuangfj/sherpa-onnx-whisper-tiny",
                "tiny-encoder.int8.onnx", "tiny-decoder.int8.onnx", "tiny-tokens.txt",
            ),
        ),
        SpeechModel(
            id = "whisper-tiny-en",
            name = "Whisper Tiny English",
            family = "Whisper · English",
            description = "Быстрая модель, оптимизированная только для английского.",
            language = "Английский",
            size = "75 МБ",
            kind = ModelKind.WHISPER,
            files = huggingFace(
                "csukuangfj/sherpa-onnx-whisper-tiny.en",
                "tiny.en-encoder.int8.onnx", "tiny.en-decoder.int8.onnx", "tiny.en-tokens.txt",
            ),
        ),
        SpeechModel(
            id = "sensevoice-small",
            name = "SenseVoice Small",
            family = "SenseVoice",
            description = "Китайский, английский, японский, корейский и кантонский.",
            language = "5 языков",
            size = "234 МБ",
            kind = ModelKind.SENSE_VOICE,
            files = huggingFace(
                "csukuangfj/sherpa-onnx-sense-voice-zh-en-ja-ko-yue-2024-07-17",
                "model.int8.onnx", "tokens.txt",
            ),
        ),
        SpeechModel(
            id = "dolphin-base",
            name = "Dolphin Base",
            family = "Dolphin · CTC",
            description = "Многоязычная CTC-модель для азиатских языков и диалектов.",
            language = "40 языков",
            size = "300 МБ",
            kind = ModelKind.DOLPHIN,
            files = huggingFace(
                "csukuangfj/sherpa-onnx-dolphin-base-ctc-multi-lang-int8-2025-04-02",
                "model.int8.onnx", "tokens.txt",
            ),
        ),
    )

    val default = models.first()
    fun byId(id: String) = models.firstOrNull { it.id == id } ?: default

    val diarizationFiles = listOf(
        RemoteFile(
            "segmentation.int8.onnx",
            "$HF/csukuangfj/sherpa-onnx-pyannote-segmentation-3-0/resolve/main/model.int8.onnx",
        ),
        RemoteFile(
            "embedding.onnx",
            "$RELEASES/speaker-recongition-models/3dspeaker_speech_eres2net_base_sv_zh-cn_3dspeaker_16k.onnx",
        ),
    )
}

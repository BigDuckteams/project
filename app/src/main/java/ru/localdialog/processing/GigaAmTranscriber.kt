package ru.localdialog.processing

import com.k2fsa.sherpa.onnx.FeatureConfig
import com.k2fsa.sherpa.onnx.FastClusteringConfig
import com.k2fsa.sherpa.onnx.OfflineDolphinModelConfig
import com.k2fsa.sherpa.onnx.OfflineModelConfig
import com.k2fsa.sherpa.onnx.OfflineNemoEncDecCtcModelConfig
import com.k2fsa.sherpa.onnx.OfflineRecognizer
import com.k2fsa.sherpa.onnx.OfflineRecognizerConfig
import com.k2fsa.sherpa.onnx.OfflineSenseVoiceModelConfig
import com.k2fsa.sherpa.onnx.OfflineSpeakerDiarization
import com.k2fsa.sherpa.onnx.OfflineSpeakerDiarizationConfig
import com.k2fsa.sherpa.onnx.OfflineSpeakerSegmentationModelConfig
import com.k2fsa.sherpa.onnx.OfflineSpeakerSegmentationPyannoteModelConfig
import com.k2fsa.sherpa.onnx.OfflineTransducerModelConfig
import com.k2fsa.sherpa.onnx.OfflineWhisperModelConfig
import com.k2fsa.sherpa.onnx.SpeakerEmbeddingExtractorConfig
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import ru.localdialog.data.AppSettings
import ru.localdialog.data.ModelCatalog
import ru.localdialog.data.ModelKind
import ru.localdialog.data.ProcessingProgress
import ru.localdialog.data.ProcessingStage
import ru.localdialog.data.SpeechModel
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.roundToInt

class GigaAmTranscriber {
    suspend fun transcribe(
        samples: FloatArray,
        model: SpeechModel,
        speechDirectory: File,
        diarizationDirectory: File,
        settings: AppSettings,
        onProgress: (ProcessingProgress) -> Unit,
        cancellation: AtomicBoolean,
    ): List<ru.localdialog.data.DialogueTurn> {
        onProgress(ProcessingProgress(ProcessingStage.DIARIZATION, 0.05f, "Загрузка моделей диаризации"))
        val diarizer = OfflineSpeakerDiarization(
            config = OfflineSpeakerDiarizationConfig(
                segmentation = OfflineSpeakerSegmentationModelConfig(
                    pyannote = OfflineSpeakerSegmentationPyannoteModelConfig(
                        model = File(diarizationDirectory, "segmentation.int8.onnx").absolutePath,
                        windowShiftRatio = 0.1f,
                    ),
                    numThreads = 2,
                ),
                embedding = SpeakerEmbeddingExtractorConfig(
                    model = File(diarizationDirectory, "embedding.onnx").absolutePath,
                    numThreads = 2,
                ),
                clustering = FastClusteringConfig(
                    numClusters = if (settings.automaticSpeakers) -1 else settings.speakerCount,
                    threshold = settings.clusterThreshold,
                    computeConfidence = true,
                ),
                minDurationOn = 0.2f,
                minDurationOff = 0.5f,
            ),
        )
        try {
            val diarization = diarizer.processWithCallback(samples, callback = { processed: Int, total: Int, _: Long ->
                if (processed % 3 == 0 || processed == total) {
                    if (cancellation.get()) return@processWithCallback 1
                    val fraction = if (total > 0) processed.toFloat() / total else 0f
                    onProgress(ProcessingProgress(ProcessingStage.DIARIZATION, fraction * 0.4f, "$processed / $total"))
                }
                0
            })
            val speakerSpans = diarization.map { SpeakerSpan(it.start, it.end, it.speaker) }
            require(speakerSpans.isNotEmpty()) { "В записи не обнаружена речь" }
            onProgress(ProcessingProgress(ProcessingStage.RECOGNITION, 0.42f, "Загрузка ${model.name}"))
            val recognizer = OfflineRecognizer(config = recognizerConfig(model, speechDirectory, settings))
            try {
                val chunks = recognitionChunks(samples, speakerSpans)
                val texts = mutableListOf<TextSpan>()
                chunks.forEachIndexed { index, chunk ->
                    checkCancellation(cancellation)
                    val audio = samples.copyOfRange(chunk.first, chunk.last)
                    val stream = recognizer.createStream()
                    try {
                        stream.acceptWaveform(audio, SAMPLE_RATE)
                        recognizer.decode(stream)
                        val result = recognizer.getResult(stream)
                        if (result.text.isNotBlank()) {
                            texts += TextSpan(chunk.first.toFloat() / SAMPLE_RATE, chunk.last.toFloat() / SAMPLE_RATE, result.text)
                        }
                    } finally {
                        stream.release()
                    }
                    onProgress(
                        ProcessingProgress(
                            ProcessingStage.RECOGNITION,
                            0.42f + 0.53f * (index + 1) / chunks.size,
                            "${index + 1} / ${chunks.size} фрагментов",
                        ),
                    )
                }
                onProgress(ProcessingProgress(ProcessingStage.MERGING, 0.97f, "Сопоставляем реплики и голоса"))
                val turns = DialogueAssembler.assemble(speakerSpans, texts)
                require(turns.isNotEmpty()) { "Речь найдена, но текст не распознан. Попробуйте другую модель." }
                return turns
            } finally {
                recognizer.release()
            }
        } finally {
            diarizer.release()
        }
    }

    private fun recognizerConfig(model: SpeechModel, directory: File, settings: AppSettings): OfflineRecognizerConfig {
        fun path(name: String) = File(directory, name).absolutePath
        val modelConfig = when (model.kind) {
            ModelKind.NEMO_TRANSDUCER -> OfflineModelConfig(
                transducer = OfflineTransducerModelConfig(
                    encoder = path("encoder.int8.onnx"),
                    decoder = path("decoder.onnx"),
                    joiner = path(if (File(directory, "joiner.int8.onnx").exists()) "joiner.int8.onnx" else "joiner.onnx"),
                ),
                modelType = "nemo_transducer",
                tokens = path(model.tokensName),
                numThreads = 2,
            )
            ModelKind.NEMO_CTC -> OfflineModelConfig(
                nemo = OfflineNemoEncDecCtcModelConfig(model = path("model.int8.onnx")),
                tokens = path(model.tokensName),
                numThreads = 2,
            )
            ModelKind.WHISPER -> {
                val prefix = model.tokensName.removeSuffix("-tokens.txt")
                OfflineModelConfig(
                    whisper = OfflineWhisperModelConfig(
                        encoder = path("$prefix-encoder.int8.onnx"),
                        decoder = path("$prefix-decoder.int8.onnx"),
                        language = if (model.id.endsWith("-en")) "en" else settings.whisperLanguage,
                        task = "transcribe",
                        tailPaddings = 1000,
                    ),
                    tokens = path(model.tokensName),
                    modelType = "whisper",
                    numThreads = 2,
                )
            }
            ModelKind.SENSE_VOICE -> OfflineModelConfig(
                senseVoice = OfflineSenseVoiceModelConfig(model = path("model.int8.onnx"), language = "auto"),
                tokens = path(model.tokensName),
                numThreads = 2,
            )
            ModelKind.DOLPHIN -> OfflineModelConfig(
                dolphin = OfflineDolphinModelConfig(model = path("model.int8.onnx")),
                tokens = path(model.tokensName),
                numThreads = 2,
            )
        }
        return OfflineRecognizerConfig(
            featConfig = FeatureConfig(sampleRate = SAMPLE_RATE, featureDim = if (model.kind == ModelKind.WHISPER) 80 else 64),
            modelConfig = modelConfig,
            decodingMethod = "greedy_search",
        )
    }

    companion object {
        private const val SAMPLE_RATE = 16_000
        private const val MAX_CHUNK_SECONDS = 22
        private const val MIN_CHUNK_SECONDS = 1

        fun recognitionChunks(samples: FloatArray, spans: List<SpeakerSpan>): List<IntRange> {
            val ranges = mutableListOf<IntRange>()
            var start = 0
            var end = 0
            fun addChunk() {
                val length = end - start
                if (length <= 0) return
                if (length <= MAX_CHUNK_SECONDS * SAMPLE_RATE) {
                    ranges += start until end
                } else {
                    var position = start
                    while (position < end) {
                        val chunkEnd = (position + MAX_CHUNK_SECONDS * SAMPLE_RATE).coerceAtMost(end)
                        if (chunkEnd - position >= MIN_CHUNK_SECONDS * SAMPLE_RATE) ranges += position until chunkEnd
                        position = chunkEnd
                    }
                }
            }
            for (span in spans) {
                val spanStart = (span.start * SAMPLE_RATE).roundToInt().coerceIn(0, samples.size)
                val spanEnd = (span.end * SAMPLE_RATE).roundToInt().coerceIn(0, samples.size)
                if (end == 0) {
                    start = spanStart
                    end = spanEnd
                } else if (spanStart - end <= SAMPLE_RATE && spanEnd - start <= MAX_CHUNK_SECONDS * SAMPLE_RATE) {
                    end = maxOf(end, spanEnd)
                } else {
                    addChunk()
                    start = spanStart
                    end = spanEnd
                }
            }
            addChunk()
            return ranges.ifEmpty { listOf(0 until samples.size) }
        }
    }
}

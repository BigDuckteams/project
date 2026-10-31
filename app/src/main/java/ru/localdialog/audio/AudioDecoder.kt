package ru.localdialog.audio

import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import java.io.File
import java.nio.ByteOrder
import kotlin.math.roundToInt
import kotlin.math.sqrt

class AudioDecoder {
    suspend fun decodeTo16kMono(path: String): FloatArray {
        val extractor = MediaExtractor()
        val audioFile = File(path)
        require(audioFile.length() <= MAX_INPUT_BYTES) { "Файл слишком большой (максимум 1 ГБ)" }
        extractor.setDataSource(path)
        try {
            val trackIndex = (0 until extractor.trackCount).firstOrNull { index ->
                extractor.getTrackFormat(index).getString(MediaFormat.KEY_MIME)?.startsWith("audio/") == true
            } ?: error("В файле не найдена звуковая дорожка")
            val format = extractor.getTrackFormat(trackIndex)
            val sampleRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE)
            val channels = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
            require(sampleRate > 0 && channels in 1..2) { "Неподдерживаемый формат аудио" }
            val approximateSeconds = if (format.containsKey(MediaFormat.KEY_DURATION)) {
                format.getLong(MediaFormat.KEY_DURATION) / 1_000_000.0
            } else 0.0
            require(approximateSeconds <= MAX_DURATION_SECONDS) { "Запись длиннее шести часов" }
            val initialCapacity = (approximateSeconds * TARGET_RATE).toInt().coerceIn(TARGET_RATE * 60, MAX_SAMPLES)
            var mono = FloatArray(initialCapacity)
            var monoSize = 0
            val pcm = decodePcm(extractor, trackIndex, format, channels)
            var index = 0
            while (index < pcm.size) {
                currentCoroutineContext().ensureActive()
                val value = if (channels == 1) {
                    pcm[index].toFloat() / Short.MAX_VALUE
                } else {
                    val left = pcm[index].toInt()
                    val right = pcm[index + 1].toInt()
                    index++
                    (left + right) / (2f * Short.MAX_VALUE)
                }
                index++
                if (monoSize == mono.size) {
                    if (monoSize >= MAX_SAMPLES) error("Расшифровка ограничена шестью часами")
                    mono = mono.copyOf((mono.size * 2).coerceAtMost(MAX_SAMPLES))
                }
                mono[monoSize++] = value
            }
            require(monoSize >= TARGET_RATE / 4) { "Аудиозапись слишком короткая или повреждена" }
            return resampleLinear(mono.copyOf(monoSize), sampleRate, TARGET_RATE)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } finally {
            extractor.release()
        }
    }

    private suspend fun decodePcm(
        extractor: MediaExtractor,
        trackIndex: Int,
        format: MediaFormat,
        channels: Int,
    ): ShortArray {
        val mime = format.getString(MediaFormat.KEY_MIME)!!
        val codec = MediaCodec.createDecoderByType(mime)
        codec.configure(format, null, null, 0)
        codec.start()
        extractor.selectTrack(trackIndex)
        val info = MediaCodec.BufferInfo()
        val chunks = ArrayList<ShortArray>()
        var totalSamples = 0
        var inputDone = false
        var outputDone = false
        try {
            while (!outputDone) {
                currentCoroutineContext().ensureActive()
                if (!inputDone) {
                    val inputIndex = codec.dequeueInputBuffer(TIMEOUT_US)
                    if (inputIndex >= 0) {
                        val buffer = codec.getInputBuffer(inputIndex)!!
                        val size = extractor.readSampleData(buffer, 0)
                        if (size < 0) {
                            codec.queueInputBuffer(inputIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                            inputDone = true
                        } else {
                            codec.queueInputBuffer(inputIndex, 0, size, extractor.sampleTime, 0)
                            extractor.advance()
                        }
                    }
                }
                val outputIndex = codec.dequeueOutputBuffer(info, TIMEOUT_US)
                when {
                    outputIndex >= 0 -> {
                        if (info.size > 0) {
                            val buffer = codec.getOutputBuffer(outputIndex)!!
                            buffer.position(info.offset)
                            buffer.limit(info.offset + info.size)
                            buffer.order(ByteOrder.nativeOrder())
                            val bytes = ByteArray(info.size)
                            buffer.get(bytes)
                            val samples = ShortArray(info.size / 2)
                            java.nio.ByteBuffer.wrap(bytes).order(ByteOrder.nativeOrder()).asShortBuffer().get(samples)
                            totalSamples += samples.size
                            if (totalSamples > MAX_DURATION_SECONDS * 192_000 * channels) {
                                error("Аудиозапись слишком длинная")
                            }
                            chunks += samples
                        }
                        codec.releaseOutputBuffer(outputIndex, false)
                        outputDone = info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0
                    }
                    outputIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                        val actualChannels = codec.outputFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
                        require(actualChannels == channels) { "Изменение числа каналов не поддерживается" }
                    }
                }
            }
        } finally {
            codec.stop()
            codec.release()
        }
        val result = ShortArray(totalSamples)
        var offset = 0
        chunks.forEach { it.copyInto(result, offset); offset += it.size }
        return result
    }

    companion object {
        const val TARGET_RATE = 16_000
        private const val TIMEOUT_US = 10_000L
        private const val MAX_INPUT_BYTES = 1024L * 1024L * 1024L
        private const val MAX_DURATION_SECONDS = 6 * 60 * 60
        private const val MAX_SAMPLES = MAX_DURATION_SECONDS * TARGET_RATE

        fun resampleLinear(samples: FloatArray, sourceRate: Int, targetRate: Int): FloatArray {
            if (sourceRate == targetRate) return samples
            val outputSize = ((samples.size.toLong() * targetRate) / sourceRate).toInt().coerceAtLeast(1)
            val result = FloatArray(outputSize)
            val ratio = sourceRate.toDouble() / targetRate
            for (i in result.indices) {
                val position = i * ratio
                val left = position.toInt().coerceAtMost(samples.lastIndex)
                val right = (left + 1).coerceAtMost(samples.lastIndex)
                val fraction = (position - left).toFloat()
                result[i] = samples[left] * (1f - fraction) + samples[right] * fraction
            }
            return result
        }

        fun rms(samples: FloatArray): Float = if (samples.isEmpty()) 0f else {
            sqrt(samples.sumOf { (it * it).toDouble() } / samples.size).toFloat()
        }
    }
}

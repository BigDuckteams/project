package ru.localdialog.data

import android.content.Context
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

data class DownloadState(val target: String, val fraction: Float, val detail: String)

class ModelRepository(private val context: Context) {
    val root = File(context.filesDir, "models").apply { mkdirs() }
    private val _download = MutableStateFlow<DownloadState?>(null)
    val download = _download.asStateFlow()
    private val _revision = MutableStateFlow(0)
    val revision = _revision.asStateFlow()

    fun speechDirectory(model: SpeechModel) = File(root, model.id)
    fun diarizationDirectory() = File(root, "diarization")

    fun isSpeechInstalled(model: SpeechModel) = installed(speechDirectory(model), model.files)
    fun isDiarizationInstalled() = installed(diarizationDirectory(), ModelCatalog.diarizationFiles)
    fun isInstalled(model: SpeechModel) = isSpeechInstalled(model) && isDiarizationInstalled()

    private fun installed(directory: File, files: List<RemoteFile>) =
        files.all { File(directory, it.name).let { file -> file.isFile && file.length() > 0L } }

    suspend fun install(model: SpeechModel) = withContext(Dispatchers.IO) {
        try {
            val required = buildList {
                if (!isSpeechInstalled(model)) addAll(model.files.map { speechDirectory(model) to it })
                if (!isDiarizationInstalled()) addAll(ModelCatalog.diarizationFiles.map { diarizationDirectory() to it })
            }
            if (required.isEmpty()) return@withContext
            required.forEachIndexed { index, (directory, file) ->
                download(directory, file, index, required.size)
            }
            _revision.value++
        } finally {
            _download.value = null
        }
    }

    private suspend fun download(directory: File, remote: RemoteFile, index: Int, total: Int) {
        directory.mkdirs()
        val target = File(directory, remote.name)
        if (target.isFile && target.length() > 0L) return
        val partial = File(directory, "${remote.name}.part")
        var connection: HttpURLConnection? = null
        try {
            connection = (URL(remote.url).openConnection() as HttpURLConnection).apply {
                connectTimeout = 20_000
                readTimeout = 30_000
                instanceFollowRedirects = true
                setRequestProperty("User-Agent", "LocalDialog/1.0")
                connect()
            }
            if (connection.responseCode !in 200..299) error("Сервер моделей вернул HTTP ${connection.responseCode}")
            val expected = connection.contentLengthLong
            connection.inputStream.buffered().use { input ->
                FileOutputStream(partial).buffered().use { output ->
                    val buffer = ByteArray(256 * 1024)
                    var bytes = 0L
                    var lastUpdate = 0L
                    while (true) {
                        currentCoroutineContext().ensureActive()
                        val count = input.read(buffer)
                        if (count < 0) break
                        output.write(buffer, 0, count)
                        bytes += count
                        val now = System.currentTimeMillis()
                        if (now - lastUpdate > 250) {
                            val fileFraction = if (expected > 0) bytes.toFloat() / expected else 0f
                            _download.value = DownloadState(
                                target = remote.name,
                                fraction = ((index + fileFraction.coerceIn(0f, 1f)) / total).coerceIn(0f, 1f),
                                detail = "${index + 1}/$total · ${bytes / 1_048_576} МБ",
                            )
                            lastUpdate = now
                        }
                    }
                    output.flush()
                    if (expected > 0 && bytes != expected) error("Загрузка прервана: файл получен не полностью")
                }
            }
            if (target.exists() && !target.delete()) error("Не удалось заменить модель")
            if (!partial.renameTo(target)) error("Не удалось сохранить модель")
        } catch (cancelled: CancellationException) {
            partial.delete()
            throw cancelled
        } catch (error: Exception) {
            partial.delete()
            throw Exception("Не удалось загрузить ${remote.name}: ${error.message}", error)
        } finally {
            connection?.disconnect()
        }
    }

    fun deleteModel(model: SpeechModel) {
        if (_download.value != null) error("Дождитесь окончания загрузки")
        speechDirectory(model).deleteRecursively()
        _revision.value++
    }

    fun deleteDiarization() {
        if (_download.value != null) error("Дождитесь окончания загрузки")
        diarizationDirectory().deleteRecursively()
        _revision.value++
    }

    fun storageBytes(): Long = root.walkBottomUp().filter { it.isFile }.sumOf { it.length() }
}

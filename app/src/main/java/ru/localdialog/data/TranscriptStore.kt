package ru.localdialog.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.File

class TranscriptStore(private val context: Context) {
    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true }
    private val directory = File(context.filesDir, "transcripts").apply { mkdirs() }
    private val mutex = Mutex()
    private val _transcripts = MutableStateFlow<List<Transcript>>(emptyList())
    val transcripts = _transcripts.asStateFlow()

    suspend fun load() = withContext(Dispatchers.IO) {
        mutex.withLock {
            _transcripts.value = directory.listFiles { file -> file.extension == "json" }
                .orEmpty()
                .mapNotNull { file -> runCatching { json.decodeFromString<Transcript>(file.readText()) }.getOrNull() }
                .sortedByDescending { it.createdAt }
        }
    }

    suspend fun save(transcript: Transcript) = withContext(Dispatchers.IO) {
        mutex.withLock {
            val target = File(directory, "${transcript.id}.json")
            val temporary = File(directory, "${transcript.id}.tmp")
            temporary.writeText(json.encodeToString(Transcript.serializer(), transcript))
            if (target.exists() && !target.delete()) error("Не удалось обновить расшифровку")
            if (!temporary.renameTo(target)) error("Не удалось сохранить расшифровку")
            _transcripts.value = (_transcripts.value.filterNot { it.id == transcript.id } + transcript)
                .sortedByDescending { it.createdAt }
        }
    }

    suspend fun delete(transcript: Transcript) = withContext(Dispatchers.IO) {
        mutex.withLock {
            File(directory, "${transcript.id}.json").delete()
            if (transcript.audioPath.startsWith(context.filesDir.absolutePath)) {
                File(transcript.audioPath).delete()
            }
            _transcripts.value = _transcripts.value.filterNot { it.id == transcript.id }
        }
    }
}

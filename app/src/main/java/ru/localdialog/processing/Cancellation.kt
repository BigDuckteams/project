package ru.localdialog.processing

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

object CancellationRegistry {
    private val cancellations = ConcurrentHashMap<String, AtomicBoolean>()

    fun register(id: String): AtomicBoolean = AtomicBoolean(false).also { cancellations[id] = it }
    fun cancel(id: String) { cancellations[id]?.set(true) }
    fun remove(id: String) { cancellations.remove(id) }
}

suspend fun checkCancellation(flag: AtomicBoolean) {
    if (flag.get()) throw CancellationException("Обработка отменена")
    currentCoroutineContext().ensureActive()
}

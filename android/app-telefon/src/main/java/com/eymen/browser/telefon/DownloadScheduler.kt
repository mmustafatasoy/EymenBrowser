package com.eymen.browser.telefon

/** A bounded FIFO. An obsolete worker completion cannot release a newer job. */
internal class DownloadScheduler<T>(private val capacity: Int = 4) {
    data class Entry<T>(val id: Long, val value: T)
    private val waiting = java.util.ArrayDeque<Entry<T>>()
    private var running: Entry<T>? = null
    private var nextId = 0L

    init { require(capacity > 0) }

    @Synchronized fun offer(value: T): Entry<T>? {
        if (waiting.size + (if (running == null) 0 else 1) >= capacity) return null
        return Entry(++nextId, value).also { waiting.addLast(it) }
    }
    @Synchronized fun startNext(): Entry<T>? {
        if (running != null) return null
        return waiting.pollFirst()?.also { running = it }
    }
    @Synchronized fun finish(id: Long): Boolean {
        if (running?.id != id) return false
        running = null
        return true
    }
    @Synchronized fun removeWaiting(id: Long): Entry<T>? {
        val entry = waiting.firstOrNull { it.id == id } ?: return null
        waiting.remove(entry)
        return entry
    }
    @Synchronized fun drainWaiting(): List<Entry<T>> = waiting.toList().also { waiting.clear() }
    @Synchronized fun pending(): List<Entry<T>> = waiting.toList()
    @Synchronized fun active(): Entry<T>? = running
    @Synchronized fun hasWork(): Boolean = running != null || waiting.isNotEmpty()
}

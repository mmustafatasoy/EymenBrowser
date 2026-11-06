package com.eymen.browser.telefon

import org.junit.Assert.*
import org.junit.Test

class DownloadSchedulerTest {
    @Test fun queuedCancellationPreservesFifoAndRunningTransfer() {
        val queue = DownloadScheduler<String>()
        val first = queue.offer("first")!!
        val cancelled = queue.offer("middle")!!
        val last = queue.offer("last")!!
        assertEquals(first, queue.startNext())
        assertEquals(cancelled, queue.removeWaiting(cancelled.id))
        assertNull(queue.startNext())
        assertEquals(listOf(last), queue.pending())
        assertTrue(queue.finish(first.id))
        assertEquals(last, queue.startNext())
        assertTrue(queue.finish(last.id))
        assertFalse(queue.hasWork())
    }
    @Test fun staleCompletionCannotClearNextFileEvenWithSameName() {
        val queue = DownloadScheduler<String>()
        val first = queue.offer("same.pdf")!!
        val next = queue.offer("same.pdf")!!
        queue.startNext()
        assertTrue(queue.finish(first.id))
        assertEquals(next, queue.startNext())
        assertFalse(queue.finish(first.id))
        assertEquals(next, queue.active())
        assertNull(queue.removeWaiting(next.id))
    }
    @Test fun capacityIncludesRunningFileAndIsRecoveredAfterRemoval() {
        val queue = DownloadScheduler<Int>()
        val entries = (1..4).map { queue.offer(it)!! }
        queue.startNext()
        assertNull(queue.offer(5))
        queue.removeWaiting(entries[2].id)
        assertNotNull(queue.offer(5))
        assertNull(queue.offer(6))
        assertEquals(listOf(2, 4, 5), queue.drainWaiting().map { it.value })
        assertTrue(queue.hasWork()) // draining waiting files never releases a live stream
        queue.finish(entries[0].id)
        assertFalse(queue.hasWork())
    }
    @Test fun concurrentAdmissionNeverExceedsBound() {
        val queue = DownloadScheduler<Int>()
        val admitted = java.util.Collections.synchronizedList(mutableListOf<Long>())
        val gate = java.util.concurrent.CountDownLatch(1)
        val threads = (1..20).map { value -> Thread {
            gate.await()
            queue.offer(value)?.let { admitted.add(it.id) }
        }.apply { start() } }
        gate.countDown()
        threads.forEach { it.join(2000); assertFalse(it.isAlive) }
        assertEquals(4, admitted.size)
        assertEquals(4, admitted.toSet().size)
    }
}

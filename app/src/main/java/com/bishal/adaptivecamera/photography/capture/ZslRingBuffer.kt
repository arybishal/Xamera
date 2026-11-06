package com.bishal.adaptivecamera.photography.capture

import java.util.concurrent.ConcurrentLinkedDeque
import kotlin.math.abs

/**
 * Data structure representing a single cached Zero Shutter Lag frame.
 */
data class ZslFrame<T : AutoCloseable>(
    val timestampNs: Long,
    val payload: T,
    val iso: Int? = null,
    val exposureTimeNs: Long? = null,
    val isFocused: Boolean = true
) : AutoCloseable {
    override fun close() {
        try {
            payload.close()
        } catch (_: Exception) {}
    }
}

/**
 * Thread-safe, bounded rolling ring buffer for Zero Shutter Lag (ZSL) captures.
 * Automatically evicts and closes the oldest frame when capacity is reached,
 * guaranteeing bounded memory usage and preventing leaked image resources.
 */
class ZslRingBuffer<T : AutoCloseable>(
    val capacity: Int = 4,
    val maxAgeMs: Long = 350L
) {
    init {
        require(capacity in 2..8) { "ZSL capacity must be between 2 and 8 frames to prevent memory exhaustion" }
    }

    private val deque = ConcurrentLinkedDeque<ZslFrame<T>>()

    val currentCount: Int
        get() = deque.size

    /**
     * Pushes a new frame into the rolling buffer.
     * If capacity is exceeded, the oldest frame is safely evicted and closed.
     */
    @Synchronized
    fun push(frame: ZslFrame<T>) {
        // Evict if buffer is full
        while (deque.size >= capacity) {
            val evicted = deque.pollFirst()
            evicted?.close()
        }
        deque.offerLast(frame)
    }

    /**
     * Retrieves the best matching frame closest to [shutterTimestampNs].
     * Frames older than [maxAgeMs] relative to [currentTimeMs] are rejected as stale.
     * Removes the chosen frame from the buffer so the caller assumes ownership.
     */
    @Synchronized
    fun acquireBestFrame(
        shutterTimestampNs: Long,
        currentTimeMs: Long = System.currentTimeMillis()
    ): ZslFrame<T>? {
        if (deque.isEmpty()) return null

        var bestFrame: ZslFrame<T>? = null
        var smallestDeltaNs = Long.MAX_VALUE

        val iterator = deque.iterator()
        while (iterator.hasNext()) {
            val candidate = iterator.next()
            val frameAgeMs = (currentTimeMs - (candidate.timestampNs / 1_000_000L))

            // Reject stale frames
            if (frameAgeMs > maxAgeMs || frameAgeMs < -100L) {
                iterator.remove()
                candidate.close()
                continue
            }

            val deltaNs = abs(candidate.timestampNs - shutterTimestampNs)
            if (deltaNs < smallestDeltaNs) {
                smallestDeltaNs = deltaNs
                bestFrame = candidate
            }
        }

        if (bestFrame != null) {
            deque.remove(bestFrame)
        }

        return bestFrame
    }

    /**
     * Drains all queued frames and releases held system resources.
     */
    @Synchronized
    fun clear() {
        while (!deque.isEmpty()) {
            val frame = deque.pollFirst()
            frame?.close()
        }
    }
}

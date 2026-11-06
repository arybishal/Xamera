package com.bishal.adaptivecamera.photography.capture

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.util.concurrent.atomic.AtomicBoolean

class ZslRingBufferTest {

    private class MockPayload : AutoCloseable {
        val isClosed = AtomicBoolean(false)
        override fun close() {
            isClosed.set(true)
        }
    }

    @Test
    fun testCapacityConstraintValidation() {
        // Valid capacities 2..8
        val buf2 = ZslRingBuffer<MockPayload>(capacity = 2)
        assertEquals(2, buf2.capacity)

        val buf8 = ZslRingBuffer<MockPayload>(capacity = 8)
        assertEquals(8, buf8.capacity)

        // Invalid < 2
        try {
            ZslRingBuffer<MockPayload>(capacity = 1)
            fail("Expected IllegalArgumentException for capacity < 2")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message?.contains("between 2 and 8") == true)
        }

        // Invalid > 8
        try {
            ZslRingBuffer<MockPayload>(capacity = 9)
            fail("Expected IllegalArgumentException for capacity > 8")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message?.contains("between 2 and 8") == true)
        }
    }

    @Test
    fun testRollingBufferEvictionAndAutomaticClosing() {
        val buffer = ZslRingBuffer<MockPayload>(capacity = 3, maxAgeMs = 1000L)
        val p1 = MockPayload()
        val p2 = MockPayload()
        val p3 = MockPayload()
        val p4 = MockPayload()

        buffer.push(ZslFrame(100_000_000L, p1))
        buffer.push(ZslFrame(200_000_000L, p2))
        buffer.push(ZslFrame(300_000_000L, p3))

        assertEquals(3, buffer.currentCount)
        assertFalse("p1 should not be closed while in buffer", p1.isClosed.get())

        // Push 4th frame -> oldest (p1) must be evicted and closed
        buffer.push(ZslFrame(400_000_000L, p4))
        assertEquals(3, buffer.currentCount)
        assertTrue("p1 should be closed upon eviction", p1.isClosed.get())
        assertFalse("p2 should remain active", p2.isClosed.get())
    }

    @Test
    fun testAcquireBestFrameFindsClosestTimestamp() {
        val buffer = ZslRingBuffer<MockPayload>(capacity = 4, maxAgeMs = 500L)
        val nowMs = 1_000_000L
        val baseNs = nowMs * 1_000_000L

        val p1 = MockPayload()
        val p2 = MockPayload()
        val p3 = MockPayload()

        buffer.push(ZslFrame(baseNs - 200_000_000L, p1)) // 200ms ago
        buffer.push(ZslFrame(baseNs - 100_000_000L, p2)) // 100ms ago
        buffer.push(ZslFrame(baseNs - 50_000_000L, p3))  // 50ms ago

        // Request frame at 90ms ago (closest to p2 at 100ms ago)
        val targetNs = baseNs - 90_000_000L
        val acquired = buffer.acquireBestFrame(shutterTimestampNs = targetNs, currentTimeMs = nowMs)

        assertNotNull(acquired)
        assertEquals(p2, acquired?.payload)
        assertEquals(2, buffer.currentCount) // p2 removed from buffer
    }

    @Test
    fun testStaleFramesAreRejectedAndClosed() {
        val buffer = ZslRingBuffer<MockPayload>(capacity = 4, maxAgeMs = 350L)
        val nowMs = 1_000_000L
        val baseNs = nowMs * 1_000_000L

        val stalePayload = MockPayload()
        val freshPayload = MockPayload()

        // 500ms old frame (exceeds maxAgeMs 350ms)
        buffer.push(ZslFrame(baseNs - 500_000_000L, stalePayload))
        // 100ms old frame
        buffer.push(ZslFrame(baseNs - 100_000_000L, freshPayload))

        val acquired = buffer.acquireBestFrame(shutterTimestampNs = baseNs, currentTimeMs = nowMs)

        assertNotNull(acquired)
        assertEquals(freshPayload, acquired?.payload)
        assertTrue("Stale payload must be closed during age evaluation", stalePayload.isClosed.get())
    }

    @Test
    fun testClearDrainsAndClosesAllFrames() {
        val buffer = ZslRingBuffer<MockPayload>(capacity = 4)
        val p1 = MockPayload()
        val p2 = MockPayload()

        buffer.push(ZslFrame(1_000L, p1))
        buffer.push(ZslFrame(2_000L, p2))

        assertEquals(2, buffer.currentCount)
        buffer.clear()
        assertEquals(0, buffer.currentCount)
        assertTrue(p1.isClosed.get())
        assertTrue(p2.isClosed.get())
    }
}

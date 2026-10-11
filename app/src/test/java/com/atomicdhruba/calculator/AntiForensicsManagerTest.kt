package com.atomicdhruba.calculator

import com.atomicdhruba.calculator.vault.AntiForensicsManager
import com.atomicdhruba.calculator.vault.FlipToLockSensorListener
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AntiForensicsManagerTest {

    @Test
    fun testInstantLockWhenTimeoutIsZero() {
        val manager = AntiForensicsManager()

        // Unpaused: must NOT auto-lock even if timeout <= 0
        assertFalse(manager.shouldAutoLock(resumeTimeMillis = 10_000L, timeoutSeconds = 0))
        assertFalse(manager.shouldAutoLock(timeoutSeconds = 0))
        assertFalse(manager.shouldAutoLock(timeoutSeconds = -1))

        manager.recordPauseTime(10_000L)

        // Paused: Timeout <= 0 must always return true (instant lock)
        assertTrue(manager.shouldAutoLock(resumeTimeMillis = 10_000L, timeoutSeconds = 0))
        assertTrue(manager.shouldAutoLock(resumeTimeMillis = 10_001L, timeoutSeconds = 0))
        assertTrue(manager.shouldAutoLock(resumeTimeMillis = 10_000L, timeoutSeconds = -1))
        assertTrue(manager.shouldAutoLock(timeoutSeconds = 0))
    }

    @Test
    fun testShouldAutoLockWithinGracePeriod() {
        val manager = AntiForensicsManager()
        val pauseTime = 10_000L
        manager.recordPauseTime(pauseTime)

        // Elapsed = 5000ms, timeout = 15s (15000ms) -> should NOT auto-lock
        val resumeTime = pauseTime + 5_000L
        val shouldLock = manager.shouldAutoLock(resumeTimeMillis = resumeTime, timeoutSeconds = 15)
        assertFalse(shouldLock)
    }

    @Test
    fun testShouldAutoLockPastGracePeriod() {
        val manager = AntiForensicsManager()
        val pauseTime = 10_000L
        manager.recordPauseTime(pauseTime)

        // Elapsed = 16000ms, timeout = 15s (15000ms) -> SHOULD auto-lock
        val resumeTime = pauseTime + 16_000L
        val shouldLock = manager.shouldAutoLock(resumeTimeMillis = resumeTime, timeoutSeconds = 15)
        assertTrue(shouldLock)
    }

    @Test
    fun testShouldAutoLockAtExactGracePeriodBoundary() {
        val manager = AntiForensicsManager()
        val pauseTime = 20_000L
        manager.recordPauseTime(pauseTime)

        // Exactly at boundary: elapsed = 15000ms, timeout = 15s -> SHOULD auto-lock
        val resumeTime = pauseTime + 15_000L
        assertTrue(manager.shouldAutoLock(resumeTimeMillis = resumeTime, timeoutSeconds = 15))

        // 1 millisecond before boundary: elapsed = 14999ms, timeout = 15s -> should NOT auto-lock
        assertFalse(manager.shouldAutoLock(resumeTimeMillis = resumeTime - 1L, timeoutSeconds = 15))
    }

    @Test
    fun testRecordPauseTimeAndResetPauseTime() {
        val manager = AntiForensicsManager()

        // Initially no pause time is recorded
        assertNull(manager.pauseTimeMillis)

        // If not paused and timeout > 0, shouldAutoLock returns false
        assertFalse(manager.shouldAutoLock(resumeTimeMillis = 50_000L, timeoutSeconds = 10))

        // Record pause time
        val timestamp = 12_345_678L
        val recorded = manager.recordPauseTime(timestamp)
        assertEquals(timestamp, recorded)
        assertEquals(timestamp, manager.pauseTimeMillis)

        // Reset pause time
        manager.resetPauseTime()
        assertNull(manager.pauseTimeMillis)

        // After reset, non-zero timeout should return false
        assertFalse(manager.shouldAutoLock(resumeTimeMillis = timestamp + 30_000L, timeoutSeconds = 10))
    }

    @Test
    fun testClockInjection() {
        var simulatedTime = 100_000L
        val manager = AntiForensicsManager(clock = { simulatedTime })

        manager.recordPauseTime()
        assertEquals(100_000L, manager.pauseTimeMillis)

        // Advance 5 seconds
        simulatedTime += 5_000L
        assertFalse(manager.shouldAutoLock(timeoutSeconds = 10))

        // Advance another 6 seconds (total 11s elapsed)
        simulatedTime += 6_000L
        assertTrue(manager.shouldAutoLock(timeoutSeconds = 10))
    }

    @Test
    fun testFlipToLockSensorListenerCallback() {
        val listener = FlipToLockSensorListener()
        var faceDownTriggerCount = 0

        listener.register(context = FakeContext()) {
            faceDownTriggerCount++
        }

        assertFalse(listener.isCurrentlyFaceDown())
        assertEquals(0, faceDownTriggerCount)

        // Simulate face down detection
        listener.simulateFaceDown()

        assertTrue(listener.isCurrentlyFaceDown())
        assertEquals(1, faceDownTriggerCount)

        // Re-registering with new callback replaces old one safely
        var newTriggerCount = 0
        listener.register(context = FakeContext()) {
            newTriggerCount++
        }
        assertFalse(listener.isCurrentlyFaceDown())

        listener.simulateFaceDown()
        assertEquals(1, faceDownTriggerCount) // Old callback not invoked again
        assertEquals(1, newTriggerCount)

        // Unregister cleans up callback
        listener.unregister()
        assertFalse(listener.isCurrentlyFaceDown())
    }

    private class FakeContext : android.content.ContextWrapper(null)
}

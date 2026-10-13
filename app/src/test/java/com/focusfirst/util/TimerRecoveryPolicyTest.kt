package com.focusfirst.util

import com.focusfirst.data.model.IntervalPreset
import com.focusfirst.data.model.TimerMode
import com.focusfirst.data.model.TimerPhase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TimerRecoveryPolicyTest {

    @Test
    fun runningCountdownUsesSavedEndTimeForRemainingSeconds() {
        val result = TimerRecoveryPolicy.recover(
            snapshot = countdownSnapshot(endTimeMs = NOW + 61_001L),
            nowMs = NOW,
        )

        assertTrue(result is TimerRecoveryDecision.Running)
        val restored = (result as TimerRecoveryDecision.Running).timerState
        assertEquals(62, restored.remainingSeconds)
        assertEquals(25 * 60, restored.totalSeconds)
        assertEquals(TimerPhase.FOCUS, restored.phase)
    }

    @Test
    fun runningCountdownIgnoresStaleSavedRemainingWhenEndTimeIsValid() {
        val result = TimerRecoveryPolicy.recover(
            snapshot = countdownSnapshot(endTimeMs = NOW + 10_000L, remainingSeconds = 0),
            nowMs = NOW,
        )

        assertTrue(result is TimerRecoveryDecision.Running)
        assertEquals(10, (result as TimerRecoveryDecision.Running).timerState.remainingSeconds)
    }

    @Test
    fun countdownAtOrPastEndIsExpired() {
        assertEquals(
            TimerRecoveryDecision.Expired,
            TimerRecoveryPolicy.recover(countdownSnapshot(endTimeMs = NOW), NOW),
        )
        assertEquals(
            TimerRecoveryDecision.Expired,
            TimerRecoveryPolicy.recover(countdownSnapshot(endTimeMs = NOW - 1L), NOW),
        )
    }

    @Test
    fun pausedCountdownRestoresPausedWithoutAdvancingRemainingTime() {
        val result = TimerRecoveryPolicy.recover(
            snapshot = countdownSnapshot(
                status = TimerRunStatus.PAUSED,
                endTimeMs = 0L,
                remainingSeconds = 317,
            ),
            nowMs = NOW,
        )

        assertTrue(result is TimerRecoveryDecision.Paused)
        val restored = (result as TimerRecoveryDecision.Paused).timerState
        assertEquals(317, restored.remainingSeconds)
        assertEquals(false, restored.isRunning)
        assertEquals(true, restored.isPaused)
    }

    @Test
    fun runningFlowRestoresCountUpElapsedAcrossProcessDeath() {
        val result = TimerRecoveryPolicy.recover(
            snapshot = TimerRecoverySnapshot(
                mode = TimerMode.FLOW,
                phase = TimerPhase.FOCUS,
                preset = IntervalPreset.FLOW,
                status = TimerRunStatus.RUNNING,
                totalSeconds = 0,
                remainingSeconds = 0,
                elapsedSeconds = 75,
                sessionStartedAtMs = NOW - 75_000L,
                runStartedAtMs = NOW - 30_000L,
                endTimeMs = Long.MAX_VALUE,
                sessionsCompleted = 2,
                sessionsInCycle = 2,
            ),
            nowMs = NOW,
        )

        assertTrue(result is TimerRecoveryDecision.Running)
        val restored = (result as TimerRecoveryDecision.Running).timerState
        assertEquals(TimerMode.FLOW, restored.timerMode)
        assertEquals(105, restored.elapsedSeconds)
        assertEquals(0, restored.totalSeconds)
        assertEquals(0, restored.remainingSeconds)
    }

    @Test
    fun pausedFlowPreservesElapsedTimeWithoutAddingPausedDuration() {
        val result = TimerRecoveryPolicy.recover(
            snapshot = TimerRecoverySnapshot(
                mode = TimerMode.FLOW,
                phase = TimerPhase.FOCUS,
                preset = IntervalPreset.FLOW,
                status = TimerRunStatus.PAUSED,
                totalSeconds = 0,
                remainingSeconds = 0,
                elapsedSeconds = 65,
                sessionStartedAtMs = NOW - 100_000L,
                runStartedAtMs = 0,
                endTimeMs = 0,
                sessionsCompleted = 0,
                sessionsInCycle = 0,
            ),
            nowMs = NOW,
        )

        assertTrue(result is TimerRecoveryDecision.Paused)
        val restored = (result as TimerRecoveryDecision.Paused).timerState
        assertEquals(65, restored.elapsedSeconds)
        assertEquals(false, restored.isRunning)
        assertEquals(true, restored.isPaused)
    }

    @Test
    fun runningCountdownWithoutEndTimestampIsInvalid() {
        assertEquals(
            TimerRecoveryDecision.Invalid,
            TimerRecoveryPolicy.recover(countdownSnapshot(endTimeMs = 0L), NOW),
        )
    }

    private fun countdownSnapshot(
        status: TimerRunStatus = TimerRunStatus.RUNNING,
        endTimeMs: Long,
        remainingSeconds: Int = 25 * 60,
    ) = TimerRecoverySnapshot(
        mode = TimerMode.POMODORO,
        phase = TimerPhase.FOCUS,
        preset = IntervalPreset.CLASSIC,
        status = status,
        totalSeconds = 25 * 60,
        remainingSeconds = remainingSeconds,
        elapsedSeconds = 0,
        sessionStartedAtMs = NOW - 30_000L,
        runStartedAtMs = NOW - 30_000L,
        endTimeMs = endTimeMs,
        sessionsCompleted = 0,
        sessionsInCycle = 0,
    )

    private companion object {
        const val NOW = 1_000_000L
    }
}

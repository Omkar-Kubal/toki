package com.focusfirst.util

import com.focusfirst.data.model.IntervalPreset
import com.focusfirst.data.model.TimerMode
import com.focusfirst.data.model.TimerPhase
import com.focusfirst.data.model.TimerState

enum class TimerRunStatus { RUNNING, PAUSED }

data class TimerRecoverySnapshot(
    val mode: TimerMode,
    val phase: TimerPhase,
    val preset: IntervalPreset,
    val status: TimerRunStatus,
    val totalSeconds: Int,
    val remainingSeconds: Int,
    val elapsedSeconds: Int,
    val sessionStartedAtMs: Long,
    val runStartedAtMs: Long,
    val endTimeMs: Long,
    val sessionsCompleted: Int,
    val sessionsInCycle: Int,
)

sealed interface TimerRecoveryDecision {
    data class Running(
        val timerState: TimerState,
        val sessionStartedAtMs: Long,
        val sessionsInCycle: Int,
    ) : TimerRecoveryDecision

    data class Paused(
        val timerState: TimerState,
        val sessionStartedAtMs: Long,
        val sessionsInCycle: Int,
    ) : TimerRecoveryDecision

    data object Expired : TimerRecoveryDecision
    data object Invalid : TimerRecoveryDecision
}

/** Pure recovery rules shared by process restoration and timer tests. */
object TimerRecoveryPolicy {
    fun recover(snapshot: TimerRecoverySnapshot, nowMs: Long): TimerRecoveryDecision {
        if (nowMs <= 0L || snapshot.sessionStartedAtMs <= 0L ||
            snapshot.sessionStartedAtMs > nowMs || snapshot.sessionsCompleted < 0 ||
            snapshot.sessionsInCycle < 0 || snapshot.elapsedSeconds < 0
        ) return TimerRecoveryDecision.Invalid

        if (snapshot.mode == TimerMode.FLOW) {
            if (snapshot.phase != TimerPhase.FOCUS || snapshot.preset != IntervalPreset.FLOW ||
                snapshot.totalSeconds != 0 || snapshot.remainingSeconds != 0
            ) return TimerRecoveryDecision.Invalid

            val elapsed = when (snapshot.status) {
                TimerRunStatus.RUNNING -> {
                    if (snapshot.runStartedAtMs <= 0L || snapshot.runStartedAtMs > nowMs) {
                        return TimerRecoveryDecision.Invalid
                    }
                    snapshot.elapsedSeconds + ((nowMs - snapshot.runStartedAtMs) / 1_000L)
                }
                TimerRunStatus.PAUSED -> {
                    if (snapshot.runStartedAtMs > 0L) return TimerRecoveryDecision.Invalid
                    snapshot.elapsedSeconds.toLong()
                }
            }.coerceAtMost(Int.MAX_VALUE.toLong()).toInt()

            val state = TimerState(
                phase = TimerPhase.FOCUS,
                preset = IntervalPreset.FLOW,
                timerMode = TimerMode.FLOW,
                totalSeconds = 0,
                remainingSeconds = 0,
                elapsedSeconds = elapsed,
                isRunning = snapshot.status == TimerRunStatus.RUNNING,
                isPaused = snapshot.status == TimerRunStatus.PAUSED,
                sessionsCompleted = snapshot.sessionsCompleted,
            )
            return decision(snapshot, state)
        }

        if (snapshot.preset == IntervalPreset.FLOW || snapshot.totalSeconds <= 0) {
            return TimerRecoveryDecision.Invalid
        }

        val remaining = when (snapshot.status) {
            TimerRunStatus.PAUSED -> {
                if (snapshot.remainingSeconds <= 0 || snapshot.remainingSeconds > snapshot.totalSeconds) {
                    return TimerRecoveryDecision.Invalid
                }
                snapshot.remainingSeconds
            }
            TimerRunStatus.RUNNING -> {
                if (snapshot.endTimeMs <= 0L || snapshot.endTimeMs == Long.MAX_VALUE) {
                    return TimerRecoveryDecision.Invalid
                }
                val remainingMs = snapshot.endTimeMs - nowMs
                if (remainingMs <= 0L) return TimerRecoveryDecision.Expired
                ((remainingMs + 999L) / 1_000L)
                    .coerceAtMost(snapshot.totalSeconds.toLong())
                    .toInt()
            }
        }
        if (remaining <= 0) return TimerRecoveryDecision.Invalid

        val state = TimerState(
            phase = snapshot.phase,
            preset = snapshot.preset,
            timerMode = TimerMode.POMODORO,
            totalSeconds = snapshot.totalSeconds,
            remainingSeconds = remaining,
            elapsedSeconds = (snapshot.totalSeconds - remaining).coerceAtLeast(0),
            isRunning = snapshot.status == TimerRunStatus.RUNNING,
            isPaused = snapshot.status == TimerRunStatus.PAUSED,
            sessionsCompleted = snapshot.sessionsCompleted,
        )
        return decision(snapshot, state)
    }

    private fun decision(
        snapshot: TimerRecoverySnapshot,
        state: TimerState,
    ): TimerRecoveryDecision = when (snapshot.status) {
        TimerRunStatus.RUNNING -> TimerRecoveryDecision.Running(
            state,
            snapshot.sessionStartedAtMs,
            snapshot.sessionsInCycle,
        )
        TimerRunStatus.PAUSED -> TimerRecoveryDecision.Paused(
            state,
            snapshot.sessionStartedAtMs,
            snapshot.sessionsInCycle,
        )
    }
}

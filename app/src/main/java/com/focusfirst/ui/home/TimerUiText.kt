package com.focusfirst.ui.home

import com.focusfirst.data.model.TimerMode
import com.focusfirst.data.model.TimerPhase
import com.focusfirst.data.model.TimerState

data class TimerUiText(
    val phase: String,
    val primaryAction: String,
    val cycle: String,
    val nextAction: String,
    val accessibilitySummary: String,
) {
    companion object {
        fun from(
            state: TimerState,
            selectedMode: TimerMode,
            sessionsBeforeLongBreak: Int = 4,
        ): TimerUiText {
            val visualMode = if (state.isIdle) selectedMode else state.timerMode
            val phase = when {
                visualMode == TimerMode.FLOW -> "Flow 45"
                state.phase == TimerPhase.SHORT_BREAK -> "Short break"
                state.phase == TimerPhase.LONG_BREAK -> "Long break"
                else -> "Focus"
            }
            val primaryAction = when {
                state.isRunning -> "Pause"
                state.isPaused -> "Resume"
                else -> "Start"
            }
            val currentCycle = (state.sessionsCompleted % sessionsBeforeLongBreak) + 1
            val cycle = when {
                state.isIdle -> "Ready when you are"
                visualMode == TimerMode.FLOW -> "Open focus session"
                state.phase == TimerPhase.FOCUS ->
                    "Session $currentCycle of $sessionsBeforeLongBreak before long break"
                state.phase == TimerPhase.SHORT_BREAK -> "Next up: focus"
                else -> "Full reset before the next round"
            }
            val nextAction = when {
                state.isPaused -> "Resume when ready"
                state.isRunning && state.phase == TimerPhase.FOCUS -> "Stay with this session"
                state.isRunning -> "Rest now, focus next"
                else -> "Start a focus session"
            }
            val stateLabel = when {
                state.isRunning -> "running"
                state.isPaused -> "paused"
                else -> "ready"
            }
            val summary = buildString {
                if (state.isIdle) {
                    append(if (visualMode == TimerMode.FLOW) "Flow 45 ready to start" else "$phase ready")
                    if (visualMode != TimerMode.FLOW && state.totalSeconds > 0) {
                        append(", ${formatAccessibilityDuration(state.totalSeconds)} set")
                    }
                } else {
                    append("$phase $stateLabel, ")
                    val duration = if (visualMode == TimerMode.FLOW) {
                        state.elapsedSeconds
                    } else {
                        state.remainingSeconds
                    }
                    append(formatAccessibilityDuration(duration))
                    append(if (visualMode == TimerMode.FLOW) " elapsed" else " remaining")
                }
                if (!state.isIdle && visualMode == TimerMode.POMODORO && state.phase == TimerPhase.FOCUS) {
                    append(", session $currentCycle of $sessionsBeforeLongBreak before long break")
                }
            }
            return TimerUiText(
                phase = phase,
                primaryAction = primaryAction,
                cycle = cycle,
                nextAction = nextAction,
                accessibilitySummary = summary,
            )
        }

        private fun formatAccessibilityDuration(totalSeconds: Int): String {
            val minutes = totalSeconds / 60
            val seconds = totalSeconds % 60
            val minuteText = "$minutes minute${if (minutes == 1) "" else "s"}"
            val secondText = "$seconds second${if (seconds == 1) "" else "s"}"
            return when {
                seconds == 0 -> minuteText
                minutes == 0 -> secondText
                else -> "$minuteText $secondText"
            }
        }
    }
}

package com.focusfirst.util

/** Resolves the length of a newly started session without leaking stale idle state. */
object TimerStartDurationPolicy {
    fun secondsForStart(configuredDurationSeconds: Int, requestedDurationMinutes: Int?): Int =
        requestedDurationMinutes?.times(60) ?: configuredDurationSeconds.coerceAtLeast(5 * 60)
}

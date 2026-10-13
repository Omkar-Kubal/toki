package com.focusfirst.util

import org.junit.Assert.assertEquals
import org.junit.Test

class TimerStartDurationPolicyTest {

    @Test
    fun explicitTaskDurationOverridesTheIdleTimerDuration() {
        assertEquals(15 * 60, TimerStartDurationPolicy.secondsForStart(25 * 60, 15))
    }

    @Test
    fun ordinaryStartKeepsTheConfiguredTimerDuration() {
        assertEquals(35 * 60, TimerStartDurationPolicy.secondsForStart(35 * 60, null))
    }
}

package com.focusfirst.util

import org.junit.Assert.assertEquals
import org.junit.Test

class TaskSelectionPolicyTest {

    @Test
    fun activeTimerKeepsItsTaskWhenAnotherTaskIsRequested() {
        assertEquals(
            4,
            selectedTaskIdAfterSelectionAttempt(
                currentTaskId = 4,
                requestedTaskId = 8,
                timerIsActive = true,
            ),
        )
    }

    @Test
    fun idleTimerCanSelectOrClearATask() {
        assertEquals(
            8,
            selectedTaskIdAfterSelectionAttempt(
                currentTaskId = 4,
                requestedTaskId = 8,
                timerIsActive = false,
            ),
        )
        assertEquals(
            null,
            selectedTaskIdAfterSelectionAttempt(
                currentTaskId = 4,
                requestedTaskId = null,
                timerIsActive = false,
            ),
        )
    }
}

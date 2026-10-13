package com.focusfirst.util

/** Keeps a running or paused focus session associated with its original task. */
fun selectedTaskIdAfterSelectionAttempt(
    currentTaskId: Int?,
    requestedTaskId: Int?,
    timerIsActive: Boolean,
): Int? = if (timerIsActive) currentTaskId else requestedTaskId

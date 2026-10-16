package com.focusfirst.util

import com.focusfirst.data.model.TaskEntity
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

/** Builds the Home timer route for a specific task. */
object TaskTimerDeepLink {
    fun forTask(task: TaskEntity): String {
        val encodedTitle = URLEncoder.encode(task.title, StandardCharsets.UTF_8.name())
        return "toki://timer/start?duration=${task.durationMinutes}&taskId=${task.id}&task=$encodedTitle"
    }
}

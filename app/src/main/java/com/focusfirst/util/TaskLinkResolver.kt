package com.focusfirst.util

import com.focusfirst.data.model.TaskEntity

/** Resolves a deep-link task without guessing between duplicate task titles. */
object TaskLinkResolver {
    fun resolve(
        taskId: String?,
        taskName: String?,
        activeTasks: List<TaskEntity>,
    ): Int? {
        if (taskId != null) {
            val id = taskId.toIntOrNull() ?: return null
            return activeTasks.singleOrNull { it.id == id && !it.isCompleted }?.id
        }

        if (taskName == null) return null
        return activeTasks
            .filter { !it.isCompleted && it.title == taskName }
            .singleOrNull()
            ?.id
    }
}

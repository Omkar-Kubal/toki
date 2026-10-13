package com.focusfirst.util

import com.focusfirst.data.model.TaskEntity
import java.time.LocalDate
import java.time.ZoneId

/** User-entered task fields converted to the Room representation. */
data class TaskDraft(
    val title: String,
    val description: String = "",
    val status: String = "TODO",
    val date: LocalDate? = null,
    val timeMillis: Long? = null,
    val targetPomodoros: Int = 4,
    val durationMinutes: Int = 25,
    val tagName: String = "Focus",
) {
    fun toTaskEntity(zoneId: ZoneId = ZoneId.systemDefault()): TaskEntity? {
        val cleanTitle = title.trim().takeIf(String::isNotEmpty) ?: return null
        return TaskEntity(
            title = cleanTitle,
            description = description.trim(),
            status = status,
            dueDate = date?.atStartOfDay(zoneId)?.toInstant()?.toEpochMilli(),
            dueTime = timeMillis,
            targetPomodoros = targetPomodoros,
            durationMinutes = durationMinutes,
            isCompleted = status == "DONE",
            tag = tagName.trim().ifBlank { "Focus" },
        )
    }
}

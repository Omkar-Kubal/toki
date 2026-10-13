package com.focusfirst.util

import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TaskDraftTest {

    @Test
    fun draftCreatesATaskForTheSelectedLocalDayAndKeepsItsTimerSettings() {
        val task = TaskDraft(
            title = "  Review project proposal  ",
            description = "  Outline the risks  ",
            date = LocalDate.parse("2026-10-12"),
            durationMinutes = 45,
            tagName = "Planning",
        ).toTaskEntity(ZoneId.of("Asia/Kolkata"))

        assertEquals("Review project proposal", task?.title)
        assertEquals("Outline the risks", task?.description)
        assertEquals(
            LocalDate.parse("2026-10-12").atStartOfDay(ZoneId.of("Asia/Kolkata")).toInstant().toEpochMilli(),
            task?.dueDate,
        )
        assertEquals(45, task?.durationMinutes)
        assertEquals("Planning", task?.tag)
        assertEquals("TODO", task?.status)
    }

    @Test
    fun blankTitlesAreNotConvertedIntoTasks() {
        assertNull(TaskDraft(title = "  ").toTaskEntity())
    }
}

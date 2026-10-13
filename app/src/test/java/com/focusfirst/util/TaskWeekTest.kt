package com.focusfirst.util

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class TaskWeekTest {

    @Test
    fun weekContainingAnyDateStartsMondayAndEndsSunday() {
        assertEquals(
            listOf(
                LocalDate.parse("2026-10-12"),
                LocalDate.parse("2026-10-13"),
                LocalDate.parse("2026-10-14"),
                LocalDate.parse("2026-10-15"),
                LocalDate.parse("2026-10-16"),
                LocalDate.parse("2026-10-17"),
                LocalDate.parse("2026-10-18"),
            ),
            TaskWeek.datesFor(LocalDate.parse("2026-10-18")),
        )
    }
}

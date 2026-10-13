package com.focusfirst.util

import com.focusfirst.data.db.SessionEntity
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Test

class FocusYearInsightsTest {
    private val zone = ZoneId.of("UTC")

    @Test
    fun buildsCalendarYearAndCountsOnlyCompletedFocusSessions() {
        val today = LocalDate.of(2026, 10, 12)
        val focusDay = today.minusDays(4)
        val sessions = listOf(
            session(focusDay, 1_500, completed = true, tag = "Focus"),
            session(focusDay, 90, completed = true, tag = "Flow"),
            session(focusDay, 3_600, completed = false, tag = "Focus"),
            session(focusDay, 900, completed = true, tag = "Short Break"),
            session(today.minusDays(365), 1_500, completed = true, tag = "Focus"),
            session(today.plusDays(1), 1_500, completed = true, tag = "Focus"),
        )

        val summary = buildFocusYearSummary(sessions, today, zone)
        val day = summary.days.single { it.date == focusDay }

        assertEquals(LocalDate.of(2026, 1, 1), summary.startDate)
        assertEquals(LocalDate.of(2026, 12, 31), summary.endDate)
        assertEquals(365, summary.days.size)
        assertEquals(26, day.focusMinutes)
        assertEquals(2, day.completedSessions)
        assertEquals(26, summary.totalFocusMinutes)
        assertEquals(1, summary.activeDays)
    }

    @Test
    fun mapsDailyFocusMinutesToStableHeatmapLevels() {
        assertEquals(0, focusIntensityLevel(0))
        assertEquals(1, focusIntensityLevel(1))
        assertEquals(1, focusIntensityLevel(24))
        assertEquals(2, focusIntensityLevel(25))
        assertEquals(3, focusIntensityLevel(50))
        assertEquals(4, focusIntensityLevel(90))
    }

    @Test
    fun buildsMondayToSundayWeekAndLeavesFutureDaysEmpty() {
        val monday = LocalDate.of(2026, 10, 12)
        val throughDate = monday.plusDays(2)
        val sessions = listOf(
            session(monday, 1_500, completed = true, tag = "Focus"),
            session(monday.plusDays(1), 2_400, completed = true, tag = "Flow"),
            session(monday.plusDays(6), 1_800, completed = true, tag = "Focus"),
        )

        val week = buildFocusWeekDays(sessions, monday, throughDate, zone)

        assertEquals(7, week.size)
        assertEquals(DayOfWeek.MONDAY, week.first().date.dayOfWeek)
        assertEquals(DayOfWeek.SUNDAY, week.last().date.dayOfWeek)
        assertEquals(25, week[0].focusMinutes)
        assertEquals(40, week[1].focusMinutes)
        assertEquals(0, week.last().focusMinutes)
    }

    @Test
    fun calendarYearIncludesLeapDayAndDecemberEvenWhenTheyAreFutureDates() {
        val summary = buildFocusYearSummary(
            sessions = emptyList(),
            endDate = LocalDate.of(2024, 6, 1),
            zoneId = zone,
        )

        assertEquals(LocalDate.of(2024, 1, 1), summary.startDate)
        assertEquals(LocalDate.of(2024, 12, 31), summary.endDate)
        assertEquals(366, summary.days.size)
        assertEquals(LocalDate.of(2024, 12, 31), summary.days.last().date)
    }

    private fun session(
        date: LocalDate,
        durationSeconds: Int,
        completed: Boolean,
        tag: String,
    ) = SessionEntity(
        startedAt = date.atStartOfDay(zone).toInstant().toEpochMilli(),
        durationSeconds = durationSeconds,
        wasCompleted = completed,
        tag = tag,
    )
}

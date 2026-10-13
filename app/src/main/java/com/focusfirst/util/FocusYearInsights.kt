package com.focusfirst.util

import com.focusfirst.data.db.SessionEntity
import java.time.Instant
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import java.util.Locale
private val FOCUS_TAGS = setOf("focus", "flow")

data class FocusYearDay(
    val date: LocalDate,
    val focusMinutes: Int,
    val completedSessions: Int,
) {
    val intensityLevel: Int
        get() = focusIntensityLevel(focusMinutes)
}

data class FocusYearSummary(
    val startDate: LocalDate,
    val endDate: LocalDate,
    val days: List<FocusYearDay>,
) {
    val totalFocusMinutes: Int
        get() = days.sumOf(FocusYearDay::focusMinutes)

    val activeDays: Int
        get() = days.count { it.completedSessions > 0 }
}

/**
 * Builds a January-to-December focus summary for the year containing [endDate].
 * Only completed Focus and Flow sessions contribute; breaks and partial runs
 * are excluded so the annual grid reflects focused time rather than timer use.
 * Days after [endDate] are included as empty dates to keep the calendar shape
 * consistent for the entire year.
 */
fun buildFocusYearSummary(
    sessions: List<SessionEntity>,
    endDate: LocalDate = LocalDate.now(),
    zoneId: ZoneId = ZoneId.systemDefault(),
): FocusYearSummary {
    val startDate = LocalDate.of(endDate.year, 1, 1)
    val yearEnd = LocalDate.of(endDate.year, 12, 31)
    val totalsByDate = focusTotalsByDate(sessions, startDate, endDate, zoneId)
    return FocusYearSummary(
        startDate = startDate,
        endDate = yearEnd,
        days = buildFocusDays(startDate, yearEnd, totalsByDate),
    )
}

/** Builds seven Monday-to-Sunday days, leaving dates after [throughDate] empty. */
fun buildFocusWeekDays(
    sessions: List<SessionEntity>,
    monday: LocalDate,
    throughDate: LocalDate,
    zoneId: ZoneId = ZoneId.systemDefault(),
): List<FocusYearDay> {
    require(monday.dayOfWeek == DayOfWeek.MONDAY) { "Week must start on Monday" }
    val sunday = monday.plusDays(6)
    val totalsByDate = focusTotalsByDate(
        sessions = sessions,
        startDate = monday,
        endDate = minOf(sunday, throughDate),
        zoneId = zoneId,
    )
    return buildFocusDays(monday, sunday, totalsByDate)
}

private fun focusTotalsByDate(
    sessions: List<SessionEntity>,
    startDate: LocalDate,
    endDate: LocalDate,
    zoneId: ZoneId,
): Map<LocalDate, MutableDayTotal> {
    if (endDate.isBefore(startDate)) return emptyMap()
    val totalsByDate = mutableMapOf<LocalDate, MutableDayTotal>()
    sessions.forEach { session ->
        if (!session.wasCompleted || session.durationSeconds <= 0) return@forEach
        if (session.tag.trim().lowercase(Locale.ROOT) !in FOCUS_TAGS) return@forEach

        val date = Instant.ofEpochMilli(session.startedAt).atZone(zoneId).toLocalDate()
        if (date < startDate || date > endDate) return@forEach

        val total = totalsByDate.getOrPut(date) { MutableDayTotal() }
        total.durationSeconds += session.durationSeconds.toLong()
        total.completedSessions++
    }
    return totalsByDate
}

private fun buildFocusDays(
    startDate: LocalDate,
    endDate: LocalDate,
    totalsByDate: Map<LocalDate, MutableDayTotal>,
): List<FocusYearDay> =
    (0L..java.time.temporal.ChronoUnit.DAYS.between(startDate, endDate)).map { offset ->
        val date = startDate.plusDays(offset)
        val total = totalsByDate[date]
        FocusYearDay(
            date = date,
            focusMinutes = ((total?.durationSeconds ?: 0L) / 60L)
                .coerceAtMost(Int.MAX_VALUE.toLong())
                .toInt(),
            completedSessions = total?.completedSessions ?: 0,
        )
    }

/** Stable minute buckets used by the four-step annual activity heatmap. */
fun focusIntensityLevel(focusMinutes: Int): Int = when {
    focusMinutes <= 0 -> 0
    focusMinutes < 25 -> 1
    focusMinutes < 50 -> 2
    focusMinutes < 90 -> 3
    else -> 4
}

private data class MutableDayTotal(
    var durationSeconds: Long = 0L,
    var completedSessions: Int = 0,
)

package com.focusfirst.util

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

/** Date range shown by the Tasks week strip (Monday through Sunday). */
object TaskWeek {
    fun datesFor(date: LocalDate): List<LocalDate> {
        val monday = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        return (0..6).map { monday.plusDays(it.toLong()) }
    }
}

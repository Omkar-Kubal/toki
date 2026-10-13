package com.focusfirst.util

import com.focusfirst.data.db.SessionEntity
import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Test

class SessionInsightDetailsTest {
    @Test
    fun formatsSessionDetailsInTheSelectedLocalTimezone() {
        val zone = ZoneId.of("Asia/Kolkata")
        val date = LocalDate.of(2026, 10, 12)
        val session = SessionEntity(
            startedAt = date.atTime(10, 0).atZone(zone).toInstant().toEpochMilli(),
            durationSeconds = 3_600,
            wasCompleted = true,
            tag = "Focus",
        )

        val details = sessionInsightDetails(session, zone)

        assertEquals("Focus", details.title)
        assertEquals("Mon, Oct 12 · 10:00 – 11:00", details.timeRange)
        assertEquals("1h 0m", details.duration)
        assertEquals("Completed", details.status)
    }
}

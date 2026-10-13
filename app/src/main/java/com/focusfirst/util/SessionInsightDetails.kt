package com.focusfirst.util

import com.focusfirst.data.db.SessionEntity
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

data class SessionInsightDetails(
    val title: String,
    val timeRange: String,
    val duration: String,
    val status: String,
)

fun sessionInsightDetails(
    session: SessionEntity,
    zoneId: ZoneId = ZoneId.systemDefault(),
): SessionInsightDetails {
    val startedAt = Instant.ofEpochMilli(session.startedAt).atZone(zoneId)
    val endedAt = startedAt.plusSeconds(session.durationSeconds.coerceAtLeast(0).toLong())
    val day = startedAt.format(DateTimeFormatter.ofPattern("EEE, MMM d", Locale.US))
    val timeFormatter = DateTimeFormatter.ofPattern("h:mm", Locale.US)
    val totalMinutes = (session.durationSeconds.coerceAtLeast(0) / 60)
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    val duration = if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m"

    return SessionInsightDetails(
        title = session.tag,
        timeRange = "$day · ${startedAt.format(timeFormatter)} – ${endedAt.format(timeFormatter)}",
        duration = duration,
        status = if (session.wasCompleted) "Completed" else "Stopped early",
    )
}

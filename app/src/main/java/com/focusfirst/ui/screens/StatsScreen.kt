package com.focusfirst.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.focusfirst.data.db.SessionEntity
import com.focusfirst.data.model.Badge
import com.focusfirst.ui.components.TokiSection
import com.focusfirst.ui.theme.TokiTypography
import com.focusfirst.util.FocusYearDay
import com.focusfirst.util.FocusYearSummary
import com.focusfirst.util.buildFocusWeekDays
import com.focusfirst.util.buildFocusYearSummary
import com.focusfirst.util.sessionInsightDetails
import com.focusfirst.viewmodel.BadgeViewModel
import com.focusfirst.viewmodel.TimerViewModel
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.Month
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.TemporalAdjusters
import java.util.Locale

@Composable
fun StatsScreen(
    timerViewModel: TimerViewModel = hiltViewModel(),
    badgeViewModel: BadgeViewModel = hiltViewModel(),
) {
    val allSessions by timerViewModel.allSessions.collectAsStateWithLifecycle()
    val recentSessions by timerViewModel.recentSessions.collectAsStateWithLifecycle()
    val streakDays by timerViewModel.streakDays.collectAsStateWithLifecycle()
    val badges by badgeViewModel.badges.collectAsStateWithLifecycle()

    val zone = remember { ZoneId.systemDefault() }
    val today = remember { LocalDate.now(zone) }
    val weekStart = remember(today) {
        today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    }
    val yearly = remember(allSessions, today, zone) {
        buildFocusYearSummary(allSessions, today, zone)
    }
    val week = remember(allSessions, weekStart, today, zone) {
        buildFocusWeekDays(allSessions, weekStart, today, zone)
    }
    val todaySummary = week.firstOrNull { it.date == today }
    val weeklyMinutes = week.sumOf(FocusYearDay::focusMinutes)
    val recentFocusSessions = recentSessions
        .filter { it.wasCompleted && isFocusSession(it) }
        .take(2)
    val unlockedBadges = badges.count(Badge::isUnlocked)
    var selectedSession by remember { mutableStateOf<SessionEntity?>(null) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(start = 22.dp, top = 28.dp, end = 22.dp, bottom = 118.dp),
    ) {
        item {
            InsightsHeader(week)
        }
        item {
            WeeklyHero(
                minutes = weeklyMinutes,
                modifier = Modifier.padding(top = 28.dp),
            )
        }
        item {
            WeekBars(week, Modifier.padding(top = 18.dp))
        }
        item {
            DividerLine(Modifier.padding(top = 22.dp, bottom = 20.dp))
        }
        item {
            DailyMetrics(
                todayMinutes = todaySummary?.focusMinutes ?: 0,
                sessions = todaySummary?.completedSessions ?: 0,
                streakDays = streakDays,
            )
        }
        item {
            DividerLine(Modifier.padding(top = 22.dp, bottom = 22.dp))
        }
        item {
            Column {
                SectionTitle("Recent activity", "Your latest focus sessions")
                if (recentFocusSessions.isEmpty()) {
                    EmptyInsights("Complete a focus session to see it here.")
                } else {
                    recentFocusSessions.forEachIndexed { index, session ->
                        SessionRow(session, onClick = { selectedSession = session })
                        if (index < recentFocusSessions.lastIndex) {
                            DividerLine(Modifier.padding(start = 58.dp))
                        }
                    }
                }
            }
        }
        item {
            DividerLine(Modifier.padding(top = 22.dp, bottom = 22.dp))
        }
        item {
            YearFocusSection(yearly)
        }
        item {
            TokiSection(Modifier
                .fillMaxWidth()
                .padding(top = 22.dp)) {
                SectionTitle("Achievements", "$unlockedBadges of ${badges.size} unlocked")
                BadgePreview(badges)
            }
        }
    }

    selectedSession?.let { session ->
        SessionDetailsSheet(session, onDismiss = { selectedSession = null })
    }
}

@Composable
private fun InsightsHeader(week: List<FocusYearDay>) {
    val start = week.firstOrNull()?.date
    val end = week.lastOrNull()?.date
    val weekLabel = if (start != null && end != null) {
        "Week of ${formatWeekRange(start, end)}"
    } else {
        "This week"
    }

    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = "Insights",
            color = MaterialTheme.colorScheme.onBackground,
            style = TokiTypography.pageTitle,
        )
        Text(
            text = weekLabel,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = TokiTypography.bodyTitle,
        )
    }
}

@Composable
private fun WeeklyHero(minutes: Int, modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = formatDuration(minutes),
            color = MaterialTheme.colorScheme.onBackground,
            style = TokiTypography.heroMetric,
        )
        Text(
            text = "Focused this week",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = TokiTypography.bodyTitle,
        )
    }
}

@Composable
private fun WeekBars(days: List<FocusYearDay>, modifier: Modifier = Modifier) {
    val cs = MaterialTheme.colorScheme
    val maxMinutes = days.maxOfOrNull(FocusYearDay::focusMinutes)?.coerceAtLeast(1) ?: 1
    val description = days.joinToString(", ") { day ->
        "${day.date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.US)} ${day.date.dayOfMonth}: ${formatDuration(day.focusMinutes)}"
    }
    val chartHeight = if (days.all { it.focusMinutes == 0 }) 52.dp else 136.dp

    Column(
        modifier = modifier
            .fillMaxWidth()
            .semantics { contentDescription = "Daily focus over the last seven days. $description" },
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(chartHeight),
            verticalAlignment = Alignment.Bottom,
        ) {
            days.forEachIndexed { index, day ->
                val isToday = index == days.lastIndex
                val barHeight = if (day.focusMinutes == 0) {
                    4.dp
                } else {
                    (104f * day.focusMinutes / maxMinutes).coerceAtLeast(5f).dp
                }
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Bottom,
                ) {
                    if (day.focusMinutes > 0) {
                        Text(
                            text = "${day.focusMinutes}m",
                            color = if (isToday) cs.onBackground else cs.onSurfaceVariant,
                            style = TokiTypography.chartLabel,
                            maxLines = 1,
                        )
                    }
                    Spacer(Modifier.height(5.dp))
                    Box(
                        modifier = Modifier
                            .width(34.dp)
                            .height(barHeight)
                            .clip(RoundedCornerShape(topStart = 5.dp, topEnd = 5.dp))
                            .background(
                                if (isToday) cs.onBackground
                                else cs.onBackground.copy(alpha = 0.38f)
                            ),
                    )
                }
            }
        }
        DividerLine()
        Row(modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp)) {
            days.forEachIndexed { index, day ->
                val isToday = index == days.lastIndex
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = day.date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.US),
                        color = if (isToday) cs.onBackground else cs.onSurfaceVariant,
                        style = TokiTypography.chartLabel,
                        fontWeight = if (isToday) FontWeight.SemiBold else FontWeight.Normal,
                        maxLines = 1,
                    )
                    Text(
                        text = day.date.dayOfMonth.toString(),
                        color = if (isToday) cs.onBackground else cs.onSurfaceVariant,
                        style = TokiTypography.chartLabel,
                        fontWeight = if (isToday) FontWeight.SemiBold else FontWeight.Normal,
                    )
                }
            }
        }
    }
}

@Composable
private fun DailyMetrics(todayMinutes: Int, sessions: Int, streakDays: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MetricColumn("Today", formatDuration(todayMinutes), Modifier.weight(1.12f))
        MetricDivider()
        MetricColumn("Sessions", sessions.toString(), Modifier.weight(0.9f))
        MetricDivider()
        MetricColumn(
            label = if (streakDays == 0) "Streak" else "$streakDays-day streak",
            value = streakDays.toString(),
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun MetricColumn(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = label,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = TokiTypography.bodyTitle,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = value,
            color = MaterialTheme.colorScheme.onBackground,
            style = TokiTypography.heroMetric,
            maxLines = 1,
            softWrap = false,
        )
    }
}

@Composable
private fun MetricDivider() {
    Box(
        modifier = Modifier
            .padding(horizontal = 10.dp)
            .width(1.dp)
            .height(42.dp)
            .background(MaterialTheme.colorScheme.outline),
    )
}

@Composable
private fun SectionTitle(title: String, detail: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = title,
            color = MaterialTheme.colorScheme.onSurface,
            style = TokiTypography.cardTitle,
        )
        Text(
            text = detail,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = TokiTypography.bodyTitle,
        )
    }
}

@Composable
private fun DividerLine(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(MaterialTheme.colorScheme.outline),
    )
}

@Composable
private fun EmptyInsights(message: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 20.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        Text(
            text = message,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = TokiTypography.bodyTitle,
        )
    }
}

@Composable
private fun SessionRow(session: SessionEntity, onClick: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    val details = remember(session) { sessionInsightDetails(session) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(role = Role.Button, onClick = onClick)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(cs.surfaceContainerHigh)
                .border(1.dp, cs.outline, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Outlined.Timer,
                contentDescription = null,
                tint = cs.onSurfaceVariant,
                modifier = Modifier.size(22.dp),
            )
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = session.tag,
                color = cs.onSurface,
                style = TokiTypography.bodyTitle,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = details.timeRange,
                color = cs.onSurfaceVariant,
                style = TokiTypography.action,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Text(
            text = details.duration,
            color = cs.onSurface,
            style = TokiTypography.inlineAmount,
            maxLines = 1,
        )
        Icon(
            imageVector = Icons.Outlined.ChevronRight,
            contentDescription = null,
            tint = cs.onSurfaceVariant,
            modifier = Modifier.size(22.dp),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SessionDetailsSheet(session: SessionEntity, onDismiss: () -> Unit) {
    val details = remember(session) { sessionInsightDetails(session) }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 24.dp, top = 8.dp, end = 24.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "Session details",
                color = MaterialTheme.colorScheme.onSurface,
                style = TokiTypography.pageTitle,
            )
            Text(
                text = details.title,
                color = MaterialTheme.colorScheme.onSurface,
                style = TokiTypography.cardTitle,
            )
            Text(
                text = details.timeRange,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = TokiTypography.bodyTitle,
            )
            DividerLine(Modifier.padding(vertical = 4.dp))
            SessionDetailValue("Focus time", details.duration)
            SessionDetailValue("Status", details.status)
        }
    }
}

@Composable
private fun SessionDetailValue(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, style = TokiTypography.bodyTitle)
        Text(value, color = MaterialTheme.colorScheme.onSurface, style = TokiTypography.bodyTitle)
    }
}

@Composable
private fun YearFocusSection(summary: FocusYearSummary) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        SectionTitle(
            title = "Past year",
            detail = "${formatDuration(summary.totalFocusMinutes)} focused · ${summary.activeDays} active days",
        )
        YearHeatmap(summary)
    }
}

@Composable
private fun YearHeatmap(summary: FocusYearSummary) {
    val weeks = remember(summary) { groupIntoWeeks(summary) }
    val cellSize = 5.dp
    val cellGap = 1.dp
    val slot = cellSize + cellGap
    val gridWidth = slot * weeks.size
    val markers = remember(weeks) { monthMarkers(weeks) }
    val dayCount = summary.days.count { it.completedSessions > 0 }
    val heatmapScroll = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .semantics {
                contentDescription = "Past year focus heatmap. $dayCount active days, " +
                    "${formatDuration(summary.totalFocusMinutes)} focused."
            },
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Column(modifier = Modifier.horizontalScroll(heatmapScroll)) {
            Box(
                modifier = Modifier
                    .width(gridWidth)
                    .height(16.dp),
            ) {
                markers.forEach { (weekIndex, label) ->
                    Text(
                        text = label,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = TokiTypography.chartLabel,
                        modifier = Modifier.offset(x = slot * weekIndex),
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(cellGap),
                verticalAlignment = Alignment.Top,
            ) {
                weeks.forEach { week ->
                    Column(verticalArrangement = Arrangement.spacedBy(cellGap)) {
                        week.forEach { day ->
                            HeatmapCell(day, cellSize)
                        }
                    }
                }
            }
        }
        HeatmapLegend()
    }
}

@Composable
private fun HeatmapCell(day: FocusYearDay?, cellSize: Dp) {
    val cs = MaterialTheme.colorScheme
    val level = day?.intensityLevel ?: 0
    val color = when (level) {
        1 -> cs.onBackground.copy(alpha = 0.18f)
        2 -> cs.onBackground.copy(alpha = 0.36f)
        3 -> cs.onBackground.copy(alpha = 0.58f)
        4 -> cs.onBackground.copy(alpha = 0.82f)
        else -> cs.surfaceContainerHigh
    }
    Box(
        modifier = Modifier
            .size(cellSize)
            .clip(RoundedCornerShape(1.dp))
            .background(color)
            .then(if (level == 0) Modifier.border(0.5.dp, cs.outlineVariant, RoundedCornerShape(1.dp)) else Modifier),
    )
}

@Composable
private fun HeatmapLegend() {
    val cs = MaterialTheme.colorScheme
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("Less", color = cs.onSurfaceVariant, style = TokiTypography.chartLabel)
        Spacer(Modifier.width(6.dp))
        (0..4).forEach { level ->
            val color = when (level) {
                1 -> cs.onBackground.copy(alpha = 0.18f)
                2 -> cs.onBackground.copy(alpha = 0.36f)
                3 -> cs.onBackground.copy(alpha = 0.58f)
                4 -> cs.onBackground.copy(alpha = 0.82f)
                else -> cs.surfaceContainerHigh
            }
            Box(
                modifier = Modifier
                    .padding(horizontal = 1.dp)
                    .size(7.dp)
                    .clip(RoundedCornerShape(1.dp))
                    .background(color)
                    .then(if (level == 0) Modifier.border(0.5.dp, cs.outlineVariant, RoundedCornerShape(1.dp)) else Modifier),
            )
        }
        Spacer(Modifier.width(6.dp))
        Text("More", color = cs.onSurfaceVariant, style = TokiTypography.chartLabel)
    }
}

@Composable
private fun BadgePreview(badges: List<Badge>) {
    if (badges.isEmpty()) {
        EmptyInsights("Complete a focus session to earn an achievement.")
        return
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        badges.take(5).forEach { badge ->
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
                    .padding(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = if (badge.isUnlocked) badge.emoji else "?",
                    fontSize = 20.sp,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = if (badge.isUnlocked) badge.name else "Locked",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = TokiTypography.sectionLabel,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

private fun groupIntoWeeks(summary: FocusYearSummary): List<List<FocusYearDay?>> {
    val daysByDate = summary.days.associateBy(FocusYearDay::date)
    val firstMonday = summary.startDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    val lastMonday = summary.endDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    val weeks = mutableListOf<List<FocusYearDay?>>()
    var monday = firstMonday
    while (!monday.isAfter(lastMonday)) {
        weeks += (0L..6L).map { offset -> daysByDate[monday.plusDays(offset)] }
        monday = monday.plusWeeks(1)
    }
    return weeks
}

private fun monthMarkers(weeks: List<List<FocusYearDay?>>): List<Pair<Int, String>> {
    var previousMonth: Month? = null
    val rawMarkers = weeks.mapIndexedNotNull { index, week ->
        val month = week.firstNotNullOfOrNull { it }?.date?.month ?: return@mapIndexedNotNull null
        if (month == previousMonth) return@mapIndexedNotNull null
        previousMonth = month
        index to month.getDisplayName(TextStyle.SHORT, Locale.US).take(3)
    }
    if (rawMarkers.isEmpty()) return emptyList()

    // Keep each 3-letter month label inside the grid and prevent adjacent labels colliding.
    val lastLabelStart = (weeks.size - 3).coerceAtLeast(0)
    val positions = IntArray(rawMarkers.size)
    var latestAllowedPosition = lastLabelStart
    for (index in rawMarkers.indices.reversed()) {
        positions[index] = minOf(rawMarkers[index].first, latestAllowedPosition)
        latestAllowedPosition = positions[index] - 4
    }
    return rawMarkers.mapIndexed { index, (_, label) -> positions[index] to label }
}

private fun isFocusSession(session: SessionEntity): Boolean =
    session.tag.equals("Focus", ignoreCase = true) || session.tag.equals("Flow", ignoreCase = true)

private fun formatDuration(minutes: Int): String {
    val safeMinutes = minutes.coerceAtLeast(0)
    val hours = safeMinutes / 60
    val remaining = safeMinutes % 60
    return if (hours > 0) "${hours}h ${remaining}m" else "${safeMinutes}m"
}

private fun formatWeekRange(start: LocalDate, end: LocalDate): String {
    val startMonth = start.format(DateTimeFormatter.ofPattern("MMM", Locale.US))
    val endMonth = end.format(DateTimeFormatter.ofPattern("MMM", Locale.US))
    return if (start.month == end.month) {
        "$startMonth ${start.dayOfMonth} – ${end.dayOfMonth}"
    } else {
        "$startMonth ${start.dayOfMonth} – $endMonth ${end.dayOfMonth}"
    }
}

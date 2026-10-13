package com.focusfirst.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.focusfirst.data.model.BreakActivity
import com.focusfirst.data.model.LONG_BREAK_ACTIVITIES
import com.focusfirst.data.model.SHORT_BREAK_ACTIVITIES
import com.focusfirst.ui.theme.TokiTypography

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BreakSuggestionSheet(
    isLongBreak:          Boolean,
    breakDurationSeconds: Int,
    breakSessionCount:    Int,
    onDismiss:            () -> Unit,
) {
    val activities = remember(isLongBreak, breakSessionCount) {
        val list = if (isLongBreak) LONG_BREAK_ACTIVITIES else SHORT_BREAK_ACTIVITIES
        list.shuffled().take(3)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState       = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor   = Color(0xFF111111),
        dragHandle       = {
            Box(
                modifier = Modifier
                    .padding(top = 12.dp)
                    .size(width = 40.dp, height = 4.dp)
                    .background(
                        color = Color.White.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(2.dp),
                    ),
            )
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 40.dp),
        ) {
            Spacer(Modifier.height(8.dp))

            // ── Header ────────────────────────────────────────────────────
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text       = if (isLongBreak) "Long break" else "Short break",
                        style      = TokiTypography.cardTitle,
                        color      = Color.White,
                    )
                    Text(
                        text     = "${breakDurationSeconds / 60} min break",
                        style    = TokiTypography.supporting,
                        color    = Color.White.copy(alpha = 0.5f),
                    )
                }
                TextButton(onClick = onDismiss) {
                    Text(
                        text     = "Skip",
                        color    = Color.White.copy(alpha = 0.5f),
                        style    = TokiTypography.action,
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            Text(
                text          = "REST IDEAS",
                style         = TokiTypography.sectionLabel,
                color         = Color.White.copy(alpha = 0.5f),
                letterSpacing = 1.5.sp,
            )

            Spacer(Modifier.height(12.dp))

            // ── Activity cards ────────────────────────────────────────────
            activities.forEach { activity ->
                BreakActivityCard(activity = activity)
                Spacer(Modifier.height(8.dp))
            }

            Spacer(Modifier.height(8.dp))

            Text(
                text      = "Step away. Your work will still be there.",
                style     = TokiTypography.supporting,
                color     = Color.White.copy(alpha = 0.5f),
                textAlign = TextAlign.Center,
                modifier  = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun BreakActivityCard(activity: BreakActivity) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color    = Color(0xFF1A1A1A),
        shape    = RoundedCornerShape(12.dp),
        border   = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.08f)),
    ) {
        Row(
            modifier          = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Emoji icon bubble
            Surface(
                modifier = Modifier.size(44.dp),
                color    = Color.White.copy(alpha = 0.06f),
                shape    = RoundedCornerShape(10.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(activity.emoji, fontSize = 22.sp)
                }
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text       = activity.title,
                    style      = TokiTypography.rowTitle,
                    color      = Color.White,
                )
                Text(
                    text     = activity.description,
                    style    = TokiTypography.supporting,
                    color    = Color.White.copy(alpha = 0.5f),
                )
            }

            Spacer(Modifier.width(8.dp))

            // Duration badge
            Surface(
                color = Color.White.copy(alpha = 0.06f),
                shape = RoundedCornerShape(6.dp),
            ) {
                Text(
                    text     = formatActivityDuration(activity.durationSeconds),
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    style    = TokiTypography.chartLabel,
                    color    = Color.White.copy(alpha = 0.5f),
                )
            }
        }
    }
}

private fun formatActivityDuration(seconds: Int): String =
    if (seconds < 60) "${seconds}s" else "${seconds / 60}m"

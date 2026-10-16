package com.focusfirst.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.focusfirst.ui.home.durationAtRingPoint
import com.focusfirst.ui.home.durationDialProgress
import com.focusfirst.ui.theme.TokiTypography
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

@Composable
fun TokiChronoRing(
    displayTime: String,
    progress: Float,
    accessibilitySummary: String,
    modifier: Modifier = Modifier,
    supportingText: String,
    isPaused: Boolean,
    dialMinutes: Int? = null,
    onDialMinutesChange: (Int) -> Unit = {},
) {
    val cs = MaterialTheme.colorScheme
    val currentDialMinutes = rememberUpdatedState(dialMinutes)
    val currentDialCallback = rememberUpdatedState(onDialMinutesChange)
    val targetProgress = if (dialMinutes != null) {
        durationDialProgress(dialMinutes)
    } else {
        progress.coerceIn(0f, 1f)
    }
    val progressAnim = remember { Animatable(targetProgress) }
    LaunchedEffect(targetProgress) {
        progressAnim.animateTo(
            targetValue = targetProgress,
            animationSpec = tween(280, easing = CubicBezierEasing(0.2f, 0f, 0f, 1f)),
        )
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .semantics {
                contentDescription = accessibilitySummary
                if (dialMinutes != null) {
                    progressBarRangeInfo = ProgressBarRangeInfo(dialMinutes.toFloat(), 5f..60f, 10)
                    setProgress { value ->
                        val snapped = (value / 5f).roundToInt().times(5).coerceIn(5, 60)
                        currentDialCallback.value(snapped)
                        true
                    }
                }
            },
    ) {
        val arcRadius = ((maxWidth - 40.dp) / 2f).coerceAtLeast(96.dp)
        val totalHeight = arcRadius + if (dialMinutes != null) 112.dp else 72.dp
        val strokeWidth = 7.dp

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(totalHeight),
        ) {
            Canvas(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .height(arcRadius + strokeWidth)
                    .pointerInput(dialMinutes != null) {
                        detectTapGestures { point ->
                            if (currentDialMinutes.value != null) {
                                val actualRadius = (size.width - 40.dp.toPx()) / 2f
                                val centerY = actualRadius + 3.5.dp.toPx()
                                durationAtRingPoint(
                                    point.x,
                                    point.y,
                                    size.width.toFloat(),
                                    centerY,
                                )?.let(currentDialCallback.value)
                            }
                        }
                    }
                    .pointerInput(dialMinutes != null) {
                        var draggingRing = false
                        detectDragGestures(
                            onDragStart = { point ->
                                if (currentDialMinutes.value != null) {
                                    val actualRadius = (size.width - 40.dp.toPx()) / 2f
                                    val centerY = actualRadius + 3.5.dp.toPx()
                                    draggingRing = durationAtRingPoint(
                                        point.x,
                                        point.y,
                                        size.width.toFloat(),
                                        centerY,
                                    ) != null
                                } else {
                                    draggingRing = false
                                }
                            },
                            onDragEnd = { draggingRing = false },
                            onDragCancel = { draggingRing = false },
                        ) { change, _ ->
                            if (draggingRing && currentDialMinutes.value != null) {
                                val actualRadius = (size.width - 40.dp.toPx()) / 2f
                                val centerY = actualRadius + 3.5.dp.toPx()
                                durationAtRingPoint(
                                    change.position.x,
                                    change.position.y,
                                    size.width.toFloat(),
                                    centerY,
                                )?.let(currentDialCallback.value)
                                change.consume()
                            }
                        }
                    },
            ) {
                val stroke = strokeWidth.toPx()
                val radius = (size.width - 40.dp.toPx()) / 2f
                val center = Offset(size.width / 2f, radius + stroke / 2f)
                val bounds = Size(radius * 2f, radius * 2f)
                val topLeft = Offset(center.x - radius, center.y - radius)

                drawArc(
                    color = cs.surfaceVariant,
                    startAngle = 180f,
                    sweepAngle = 180f,
                    useCenter = false,
                    topLeft = topLeft,
                    size = bounds,
                    style = Stroke(stroke, cap = StrokeCap.Round),
                )
                if (progressAnim.value > 0f) {
                    drawArc(
                        color = cs.primary.copy(alpha = if (isPaused) 0.58f else 1f),
                        startAngle = 180f,
                        sweepAngle = 180f * progressAnim.value,
                        useCenter = false,
                        topLeft = topLeft,
                        size = bounds,
                        style = Stroke(stroke, cap = StrokeCap.Round),
                    )
                }

                val angle = Math.toRadians((180f + 180f * progressAnim.value).toDouble())
                val handle = Offset(
                    x = center.x + cos(angle).toFloat() * radius,
                    y = center.y + sin(angle).toFloat() * radius,
                )
                drawCircle(
                    color = cs.primary,
                    radius = 8.dp.toPx(),
                    center = handle,
                )
                drawCircle(
                    color = cs.onPrimary.copy(alpha = 0.35f),
                    radius = 3.dp.toPx(),
                    center = handle,
                )
            }

            Text(
                text = supportingText.uppercase(),
                color = cs.onSurfaceVariant,
                style = TokiTypography.sectionLabel.copy(letterSpacing = 1.4.sp),
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset(y = arcRadius * 0.53f),
            )
            Text(
                text = displayTime,
                color = cs.onBackground,
                style = TokiTypography.timerHeroMetric,
                textAlign = TextAlign.Center,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset(y = arcRadius * 0.69f),
            )

            if (dialMinutes != null) {
                Row(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxWidth()
                        .padding(horizontal = 6.dp)
                        .offset(y = arcRadius + 10.dp),
                ) {
                    Text(
                        text = "5 min",
                        color = cs.onSurfaceVariant,
                        style = TokiTypography.action,
                    )
                    androidx.compose.foundation.layout.Spacer(Modifier.weight(1f))
                    Text(
                        text = "60 min",
                        color = cs.onSurfaceVariant,
                        style = TokiTypography.action,
                    )
                }
                Text(
                    text = "Drag clockwise to add time",
                    color = cs.onSurfaceVariant,
                    style = TokiTypography.supporting,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .offset(y = arcRadius + 64.dp),
                )
            }
        }
    }
}

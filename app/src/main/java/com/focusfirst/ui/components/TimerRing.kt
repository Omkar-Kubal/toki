package com.focusfirst.ui.components

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Color
import com.focusfirst.data.model.IntervalPreset
import com.focusfirst.data.model.TimerMode
import com.focusfirst.data.model.TimerPhase
import com.focusfirst.data.model.TimerState
import com.focusfirst.ui.theme.FocusFirstTheme
import com.focusfirst.ui.theme.ringColor

// ── Drawing constants ─────────────────────────────────────────────────────────
// Defined at file scope so they can be referenced inside the Canvas DrawScope
// (which would otherwise require explicit density conversion).
private val STROKE_WIDTH       = 18.dp   // main ring stroke
private val PULSE_STROKE_WIDTH =  6.dp   // outer pulse ring stroke
private val RING_PADDING       =  4.dp   // gap between ring outer edge and canvas edge

// ============================================================================
// TimerRing
// ============================================================================

/**
 * Canvas-based circular countdown ring for the FocusFirst timer face.
 *
 * Visual layers (bottom → top):
 *   1. Background track  — full 360° circle at 12 % opacity.
 *   2. Pulse ring        — thin ring just outside the main ring, breathing
 *                          in/out while [TimerState.isRunning] is true.
 *   3. Progress arc      — solid phase colour sweeping clockwise from 12 o'clock.
 *   4. Center text       — MM:SS time + phase label, composited via Box overlay.
 *
 * @param timerState Live timer state (progress, displayTime, phase, isRunning).
 * @param modifier   Applied to the outer [Box] container.
 * @param size       Overall component size in dp; all ring dimensions scale with it.
 */
@Composable
fun TimerRing(
    timerState: TimerState,
    modifier:   Modifier = Modifier,
    size:       Dp       = 280.dp,
) {
    // ── Phase colour ──────────────────────────────────────────────────────────
    // Flow uses a softer neutral; all timer modes follow the active theme.
    val ringColor  = if (timerState.timerMode == TimerMode.FLOW) MaterialTheme.colorScheme.onSurfaceVariant
                     else timerState.phase.ringColor()
    val textColor  = MaterialTheme.colorScheme.onBackground
    val typography = MaterialTheme.typography

    // ── Progress animation ────────────────────────────────────────────────────
    // M3 Emphasized easing (0.2, 0.0, 0.0, 1.0) at Medium2 duration (300ms)
    // per material_design_skills.md §6.1. Previous: tween(800, LinearEasing).
    val animatedProgress by animateFloatAsState(
        targetValue   = timerState.progress,
        animationSpec = tween(
            durationMillis = 300,
            easing         = CubicBezierEasing(0.2f, 0f, 0f, 1f),
        ),
        label         = "timerProgress",
    )

    // ── Pulse animation ───────────────────────────────────────────────────────
    // The infinite transition always runs; the pulse is only *drawn* while
    // isRunning, keeping the alpha compositor cost near-zero when idle.
    // M3 Motion: FastOutSlowInEasing for organic breathing feel (was LinearEasing).
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlphaFraction by infiniteTransition.animateFloat(
        initialValue  = 0f,
        targetValue   = 1f,
        animationSpec = infiniteRepeatable(
            animation  = tween(durationMillis = 1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pulseAlphaFraction",
    )

    // ── Phase label ───────────────────────────────────────────────────────────
    val phaseLabel = when {
        timerState.timerMode == TimerMode.FLOW          -> "FLOW"
        timerState.phase == TimerPhase.FOCUS            -> "FOCUS"
        timerState.phase == TimerPhase.SHORT_BREAK      -> "SHORT BREAK"
        else                                            -> "LONG BREAK"
    }

    // ── Shadow-proof alias ────────────────────────────────────────────────────
    // Inside a Canvas lambda, `size` resolves to DrawScope.size (a Size in px),
    // which would shadow the Dp parameter above.  Capturing it here preserves
    // access to the Dp value for toPx() conversions inside the draw block.
    val componentSizeDp = size

    // ========================================================================
    // Layout
    // ========================================================================

    Box(
        modifier         = modifier.size(size),
        contentAlignment = Alignment.Center,
    ) {
        // ── Canvas — all arc / ring drawing ──────────────────────────────────
        Canvas(modifier = Modifier.size(componentSizeDp)) {

            val strokePx      = STROKE_WIDTH.toPx()
            val pulseStrokePx = PULSE_STROKE_WIDTH.toPx()
            val paddingPx     = RING_PADDING.toPx()

            // Centre-line radius of the main ring.
            // Spec: (size / 2) − strokeWidth − 4.dp
            // The ring occupies [radius − stroke/2 … radius + stroke/2], so
            // the outer edge sits at radius + stroke/2 = size/2 − 4.dp from
            // the canvas edge — comfortable padding on all screen densities.
            val ringRadiusPx  = componentSizeDp.toPx() / 2f - strokePx - paddingPx

            // Canvas centre (DrawScope.size is px, not the Dp parameter)
            val cx = this.size.width  / 2f
            val cy = this.size.height / 2f

            // Shared bounding box for the track and progress arcs.
            // drawArc positions arcs by their enclosing rectangle, not by centre+radius.
            val arcTopLeft = Offset(cx - ringRadiusPx, cy - ringRadiusPx)
            val arcSize    = Size(ringRadiusPx * 2f, ringRadiusPx * 2f)

            // ── Layer 1: Background track ────────────────────────────────────
            // Full 360° circle at low opacity acts as the "empty" portion of
            // the ring, giving visual context when progress is near zero.
            drawArc(
                color      = ringColor.copy(alpha = 0.12f),
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter  = false,
                topLeft    = arcTopLeft,
                size       = arcSize,
                style      = Stroke(width = strokePx),
            )

            // ── Layer 2: Pulse ring ──────────────────────────────────────────
            // Sits just outside the main ring's outer edge so it never overlaps
            // the progress arc.  Opacity breathes 0 → 0.25 → 0 every 1 800 ms.
            //
            // Geometry:
            //   main outer edge  = ringRadiusPx + strokePx / 2
            //   pulse centre     = main outer + gap (4dp) + pulseStrokePx / 2
            if (timerState.isRunning) {
                val pulseRadiusPx = ringRadiusPx +
                        strokePx / 2f +
                        4.dp.toPx() +
                        pulseStrokePx / 2f

                // drawCircle defaults to the canvas centre — no topLeft needed.
                drawCircle(
                    color  = ringColor.copy(alpha = pulseAlphaFraction * 0.25f),
                    radius = pulseRadiusPx,
                    style  = Stroke(width = pulseStrokePx),
                )
            }

            // ── Layer 3: Progress arc ────────────────────────────────────────
            // Sweeps clockwise from 12 o'clock (-90°).  StrokeCap.Round gives
            // a rounded leading edge that distinguishes the arc from the track
            // at a glance, especially near 0 % and 100 %.
            // Skipped entirely when animatedProgress == 0 to avoid a stray dot
            // rendered at the start angle by some GPU drivers.
            if (animatedProgress > 0f) {
                drawArc(
                    color      = ringColor,
                    startAngle = -90f,
                    sweepAngle = 360f * animatedProgress,
                    useCenter  = false,
                    topLeft    = arcTopLeft,
                    size       = arcSize,
                    style      = Stroke(
                        width = strokePx,
                        cap   = StrokeCap.Round,
                    ),
                )
            }
        }

        // ── Text overlay ──────────────────────────────────────────────────────
        // Rendered as Compose Text over the Canvas (not via drawIntoCanvas /
        // TextMeasurer) so it participates in the normal accessibility tree and
        // responds to font-scale changes automatically.
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text  = timerState.displayTime,
                style = typography.displayLarge,
                color = textColor,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text  = phaseLabel,
                style = typography.labelSmall.copy(letterSpacing = 2.sp),
                color = textColor.copy(alpha = 0.6f),
            )
        }
    }
}

// ============================================================================
// Previews
// ============================================================================

@Preview(name = "Focus — running", showBackground = true, backgroundColor = 0xFF1A1A1A)
@Composable
private fun PreviewFocusRunning() {
    FocusFirstTheme {
        TimerRing(
            timerState = TimerState(
                phase            = TimerPhase.FOCUS,
                preset           = IntervalPreset.CLASSIC,
                totalSeconds     = 25 * 60,
                remainingSeconds = 18 * 60 + 43,
                isRunning        = true,
            ),
        )
    }
}

@Preview(name = "Short break — paused", showBackground = true, backgroundColor = 0xFF1A1A1A)
@Composable
private fun PreviewShortBreakPaused() {
    FocusFirstTheme {
        TimerRing(
            timerState = TimerState(
                phase            = TimerPhase.SHORT_BREAK,
                totalSeconds     = 5 * 60,
                remainingSeconds = 3 * 60 + 12,
                isRunning        = false,
                isPaused         = true,
            ),
        )
    }
}

@Preview(name = "Long break — idle / light", showBackground = true)
@Composable
private fun PreviewLongBreakLight() {
    FocusFirstTheme {
        TimerRing(
            timerState = TimerState(
                phase            = TimerPhase.LONG_BREAK,
                totalSeconds     = 15 * 60,
                remainingSeconds = 15 * 60,
                isRunning        = false,
            ),
        )
    }
}

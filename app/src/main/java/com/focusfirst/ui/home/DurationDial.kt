package com.focusfirst.ui.home

import kotlin.math.atan2
import kotlin.math.hypot
import kotlin.math.roundToInt

/** Maps a touch on the upper semicircle from 5 minutes at the left to 60 at the right. */
fun durationAtRingPoint(
    x: Float,
    y: Float,
    width: Float,
    height: Float,
): Int? {
    val radius = width / 2f
    if (radius <= 0f) return null

    val dx = x - radius
    val dy = y - height
    if (dy > 0f) return null

    val touchRadius = hypot(dx, dy)
    if (touchRadius !in (radius * 0.84f)..(radius * 1.16f)) return null

    val rawAngle = Math.toDegrees(atan2(-dy.toDouble(), dx.toDouble()))
    val angleFromRight = if (rawAngle < 0.0) rawAngle + 360.0 else rawAngle
    if (angleFromRight !in 0.0..180.0) return null

    val progress = (180.0 - angleFromRight) / 180.0
    val step = (progress * 11.0).roundToInt().coerceIn(0, 11)
    return 5 + step * 5
}

fun durationDialProgress(minutes: Int): Float =
    (minutes.coerceIn(5, 60) - 5) / 55f

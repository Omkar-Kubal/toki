package com.focusfirst.ui.home

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DurationDialTest {
    @Test fun openArcRunsClockwiseFromFiveToSixtyMinutes() {
        assertEquals(5, durationAtRingPoint(0f, 100f, 200f, 100f))
        assertEquals(20, durationAtRingPoint(29.3f, 29.3f, 200f, 100f))
        assertEquals(35, durationAtRingPoint(100f, 0f, 200f, 100f))
        assertEquals(45, durationAtRingPoint(170.7f, 29.3f, 200f, 100f))
        assertEquals(60, durationAtRingPoint(200f, 100f, 200f, 100f))
    }

    @Test fun lowerHalfAndCenterDoNotChangeDuration() {
        assertNull(durationAtRingPoint(100f, 200f, 200f, 100f))
        assertNull(durationAtRingPoint(100f, 75f, 200f, 100f))
        assertNull(durationAtRingPoint(100f, 20f, 200f, 100f))
    }

    @Test fun progressUsesTheOpenArcEndpoints() {
        assertEquals(0f, durationDialProgress(5), 0.001f)
        assertEquals(20f / 55f, durationDialProgress(25), 0.001f)
        assertEquals(1f, durationDialProgress(60), 0.001f)
    }
}

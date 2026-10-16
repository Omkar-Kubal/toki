package com.focusfirst.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DeepLinkDurationTest {

    @Test
    fun omittedDurationUsesDefaultAndSupportedBoundariesAreAccepted() {
        assertEquals(25, DeepLinkDuration.parse(null))
        assertEquals(10, DeepLinkDuration.parse("10"))
        assertEquals(60, DeepLinkDuration.parse("60"))
    }

    @Test
    fun malformedAndOutOfRangeDurationsAreRejected() {
        assertNull(DeepLinkDuration.parse("abc"))
        assertNull(DeepLinkDuration.parse("9"))
        assertNull(DeepLinkDuration.parse("61"))
        assertNull(DeepLinkDuration.parse("-1"))
        assertNull(DeepLinkDuration.parse("2147483648"))
    }
}

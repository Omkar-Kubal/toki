package com.focusfirst

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.focusfirst.ui.components.TokiChronoRing
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TokiChronoRingLayoutTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun durationScaleLabelsSitBelowTheArcStroke() {
        composeRule.setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                TokiChronoRing(
                    displayTime = "25:00",
                    progress = 0f,
                    accessibilitySummary = "Focus ready, 25 minutes set",
                    supportingText = "Focus",
                    isPaused = false,
                    dialMinutes = 25,
                )
            }
        }
        composeRule.waitForIdle()

        val density = composeRule.density.density
        val ringBounds = composeRule
            .onNodeWithContentDescription("Focus ready, 25 minutes set")
            .fetchSemanticsNode()
            .boundsInRoot
        val arcRadius = (ringBounds.width - 40f * density) / 2f
        val arcBottom = ringBounds.top + arcRadius + 7f * density
        val minimumLabelTop = composeRule.onNodeWithText("5 min").fetchSemanticsNode().boundsInRoot.top
        val maximumLabelTop = composeRule.onNodeWithText("60 min").fetchSemanticsNode().boundsInRoot.top

        assertTrue("5 min should begin below the ring stroke", minimumLabelTop >= arcBottom)
        assertTrue("60 min should begin below the ring stroke", maximumLabelTop >= arcBottom)
    }
}

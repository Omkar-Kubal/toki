package com.focusfirst

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ProgressZeroMinuteLabelTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun zeroMinuteDaysDoNotShowBarValueLabels() {
        composeRule.onNodeWithContentDescription("Progress").performClick()
        composeRule.waitForIdle()

        composeRule.onAllNodes(
            hasText("0m") and hasAnyAncestor(
                hasContentDescription("Daily focus over the last seven days", substring = true),
            ),
            useUnmergedTree = true,
        ).assertCountEquals(0)
    }

    @Test
    fun weeklyBarsRunFromMondayThroughSunday() {
        composeRule.onNodeWithContentDescription("Progress").performClick()
        composeRule.waitForIdle()

        val monday = composeRule.onNodeWithText("Mon").fetchSemanticsNode().boundsInRoot.center.x
        val sunday = composeRule.onNodeWithText("Sun").fetchSemanticsNode().boundsInRoot.center.x

        assertTrue("Monday should appear before Sunday", monday < sunday)
    }

    @Test
    fun pastYearMonthLabelsCoverJanuaryThroughDecemberOnce() {
        composeRule.onNodeWithContentDescription("Progress").performClick()
        composeRule.waitForIdle()

        composeRule.onAllNodesWithText("Jan").assertCountEquals(1)
        composeRule.onAllNodesWithText("Dec").assertCountEquals(1)
        assertEquals("Calendar year should show October once", 1,
            composeRule.onAllNodesWithText("Oct").fetchSemanticsNodes().size)
    }
}

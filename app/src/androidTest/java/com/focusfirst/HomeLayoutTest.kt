package com.focusfirst

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HomeLayoutTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun homeBrandMarkAppearsBeforeTitle() {
        val mark = composeRule.onNodeWithTag("home-brand-mark")
        mark.assertIsDisplayed()

        val markBounds = mark.fetchSemanticsNode().boundsInRoot
        val titleBounds = composeRule.onNodeWithText("Toki").fetchSemanticsNode().boundsInRoot

        assertTrue("The launcher mark should sit beside the Toki title", markBounds.right <= titleBounds.left)
    }

    @Test
    fun homeTitleStartsCloseToTheStatusBarInset() {
        val titleBounds = composeRule.onNodeWithText("Toki").fetchSemanticsNode().boundsInRoot
        val context = composeRule.activity
        val statusBarDimension = context.resources.getIdentifier("status_bar_height", "dimen", "android")
        val statusBarHeight = context.resources.getDimensionPixelSize(statusBarDimension)
        val density = context.resources.displayMetrics.density

        assertTrue(
            "The Home title should start within 30dp of the status bar inset",
            titleBounds.top <= statusBarHeight + 30f * density,
        )
    }

    @Test
    fun timerDisplayVisuallyDominatesItsPhaseLabel() {
        val timerHeight = composeRule.onNodeWithText("25:00").fetchSemanticsNode().boundsInRoot.height
        val phaseHeight = composeRule.onNodeWithText("FOCUS").fetchSemanticsNode().boundsInRoot.height

        assertTrue("The timer should render at least three times taller than its phase label", timerHeight > phaseHeight * 3f)
    }
}

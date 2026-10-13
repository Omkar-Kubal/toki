package com.focusfirst

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SettingsDesignTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun settingsMatchesApprovedSectionsAndOmitsRemovedRows() {
        composeRule.onNodeWithContentDescription("Settings").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Make focus feel like yours").assertIsDisplayed()
        composeRule.onNodeWithText("Explore Pro").fetchSemanticsNode()
        composeRule.onNodeWithText("Focus").fetchSemanticsNode()
        composeRule.onNodeWithText("Daily goal").fetchSemanticsNode()
        composeRule.onNodeWithText("Take a longer break after 4 focus sessions").fetchSemanticsNode()
        val hasDndToggle = composeRule.onAllNodesWithText("Auto-enable DND").fetchSemanticsNodes().isNotEmpty()
        val hasDndPermissionAction = composeRule.onAllNodesWithText("Do Not Disturb").fetchSemanticsNodes().isNotEmpty()
        assertTrue("Settings should show the DND switch or its permission action", hasDndToggle || hasDndPermissionAction)

        composeRule.onNodeWithText("Sound & haptics").fetchSemanticsNode()
        composeRule.onNodeWithText("Appearance").fetchSemanticsNode()
        composeRule.onNodeWithText("Data").fetchSemanticsNode()
        composeRule.onNodeWithText("About").fetchSemanticsNode()

        assertTrue(composeRule.onAllNodesWithText("Cloud sync").fetchSemanticsNodes().isEmpty())
        assertTrue(composeRule.onAllNodesWithText("Terms of Service").fetchSemanticsNodes().isEmpty())
        assertTrue(composeRule.onAllNodesWithText("Open Source Licenses").fetchSemanticsNodes().isEmpty())
    }

    @Test
    fun longBreakControlAlsoExposesTheSessionCadenceSetting() {
        composeRule.onNodeWithContentDescription("Settings").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Long break").performClick()
        composeRule.onNodeWithText("Long break settings").assertIsDisplayed()
        composeRule.onNodeWithText("Sessions before long break").assertIsDisplayed()
        composeRule.onNodeWithText("Cancel").performClick()
    }
}

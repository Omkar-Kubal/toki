package com.focusfirst

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MainActivityBackNavigationTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun systemBackFromTasksReturnsToHome() {
        composeRule.onNodeWithContentDescription("Tasks").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithContentDescription("Tasks").assertIsSelected()

        composeRule.activity.onBackPressedDispatcher.onBackPressed()
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Toki").assertIsDisplayed()
    }

    @Test
    fun systemBackFromProgressReturnsToHome() {
        composeRule.onNodeWithContentDescription("Progress").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Insights").assertIsDisplayed()

        composeRule.activity.onBackPressedDispatcher.onBackPressed()
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Toki").assertIsDisplayed()
    }
}

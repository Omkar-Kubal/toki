package com.focusfirst

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.focusfirst.data.model.TaskEntity
import com.focusfirst.ui.components.TaskSheet
import com.focusfirst.ui.theme.FocusFirstTheme
import java.util.concurrent.atomic.AtomicInteger
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TaskSheetOverflowMenuTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val task = TaskEntity(
        id = 7,
        title = "Write release notes",
        completedPomodoros = 1,
        targetPomodoros = 4,
    )

    @Test
    fun overflowMenuExposesAndDispatchesCompleteAndDeleteActions() {
        val completedTaskId = AtomicInteger(-1)
        val deletedTaskId = AtomicInteger(-1)
        showSheet(
            onCompleteTask = { completedTaskId.set(it.id) },
            onDeleteTask = { deletedTaskId.set(it.id) },
        )

        openTaskActions()
        composeRule.onNodeWithText("Mark complete").assertIsDisplayed()
        composeRule.onNodeWithText("Delete").assertIsDisplayed()
        composeRule.onNodeWithText("Mark complete").performClick()

        composeRule.runOnIdle {
            assertEquals(task.id, completedTaskId.get())
        }

        openTaskActions()
        composeRule.onNodeWithText("Delete").performClick()

        composeRule.runOnIdle {
            assertEquals(task.id, deletedTaskId.get())
        }
    }

    @Test
    fun selectingTaskAndNoTaskDispatchesTheCorrectSelection() {
        val selectedTaskId = AtomicInteger(-1)
        showSheet(onTaskSelected = { selectedTaskId.set(it?.id ?: 0) })

        composeRule.onNodeWithText(task.title).performClick()
        composeRule.runOnIdle {
            assertEquals(task.id, selectedTaskId.get())
        }

        composeRule.onNodeWithText("No task selected").performClick()
        composeRule.runOnIdle {
            assertEquals(0, selectedTaskId.get())
        }
    }

    private fun openTaskActions() {
        composeRule.onNodeWithContentDescription("Task actions for ${task.title}").performClick()
    }

    private fun showSheet(
        onTaskSelected: (TaskEntity?) -> Unit = {},
        onCompleteTask: (TaskEntity) -> Unit = {},
        onDeleteTask: (TaskEntity) -> Unit = {},
    ) {
        composeRule.setContent {
            FocusFirstTheme {
                TaskSheet(
                    tasks = listOf(task),
                    selectedTaskId = task.id,
                    isPro = true,
                    activeCount = 1,
                    onTaskSelected = onTaskSelected,
                    onAddTask = {},
                    onDeleteTask = onDeleteTask,
                    onCompleteTask = onCompleteTask,
                    onUpgradeClick = {},
                    onManageTasks = {},
                    onDismiss = {},
                )
            }
        }
        composeRule.waitForIdle()
    }
}

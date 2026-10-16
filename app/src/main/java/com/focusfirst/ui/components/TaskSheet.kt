package com.focusfirst.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.focusfirst.data.model.TaskEntity
import com.focusfirst.ui.theme.FocusColors
import com.focusfirst.ui.theme.TokiTypography

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskSheet(
    tasks: List<TaskEntity>,
    selectedTaskId: Int?,
    isPro: Boolean,
    activeCount: Int,
    onTaskSelected: (TaskEntity?) -> Unit,
    onAddTask: (String) -> Unit,
    onDeleteTask: (TaskEntity) -> Unit,
    onCompleteTask: (TaskEntity) -> Unit,
    onUpgradeClick: () -> Unit,
    onManageTasks: () -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = ColorSheet,
    ) {
        var newTask by remember { mutableStateOf("") }
        val canAdd = isPro || activeCount < 3

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 44.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Session task",
                    style = TokiTypography.pageTitle,
                    color = MaterialTheme.colorScheme.onSurface,
                )

                TextButton(onClick = onManageTasks) {
                    Text(
                        text = "Manage tasks",
                        style = TokiTypography.action,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(22.dp),
                    )
                }
            }

            if (!isPro) {
                Text(
                    text = "$activeCount/3 free",
                    style = TokiTypography.supporting,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 1.dp, bottom = 14.dp),
                )
            } else {
                Spacer(Modifier.height(14.dp))
            }

            tasks.forEach { task ->
                TaskRow(
                    task = task,
                    isSelected = selectedTaskId == task.id,
                    onSelect = { onTaskSelected(task) },
                    onComplete = { onCompleteTask(task) },
                    onDelete = { onDeleteTask(task) },
                )
                Spacer(Modifier.height(6.dp))
            }

            if (tasks.isEmpty()) {
                Text(
                    text = "No tasks yet. Add one below.",
                    style = TokiTypography.supporting,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 20.dp),
                )
            }

            Spacer(Modifier.height(10.dp))
            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant,
                thickness = 1.dp,
            )
            NoTaskRow(
                isSelected = selectedTaskId == null,
                onClick = { onTaskSelected(null) },
            )
            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant,
                thickness = 1.dp,
            )
            Spacer(Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedTextField(
                    value = newTask,
                    onValueChange = { newTask = it },
                    placeholder = {
                        Text(
                            text = if (canAdd) "Add a focus task" else "Upgrade for more tasks",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = TokiTypography.bodyTitle,
                        )
                    },
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 56.dp),
                    enabled = canAdd,
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            if (newTask.isNotBlank() && canAdd) {
                                onAddTask(newTask.trim())
                                newTask = ""
                            }
                        },
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.outline,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                        disabledTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        disabledBorderColor = MaterialTheme.colorScheme.outlineVariant,
                    ),
                )

                Spacer(Modifier.width(10.dp))

                if (canAdd) {
                    IconButton(
                        onClick = {
                            if (newTask.isNotBlank()) {
                                onAddTask(newTask.trim())
                                newTask = ""
                            }
                        },
                        enabled = canAdd,
                        modifier = Modifier
                            .size(52.dp)
                            .background(
                                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                shape = CircleShape,
                            )
                            .semantics {
                                contentDescription = "Add task"
                                role = Role.Button
                            },
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(26.dp),
                        )
                    }
                } else {
                    TextButton(onClick = onUpgradeClick) {
                        Text("Pro", color = FocusColors.CommercialBlue)
                    }
                }
            }
        }
    }
}

@Composable
private fun TaskRow(
    task: TaskEntity,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onComplete: () -> Unit,
    onDelete: () -> Unit,
) {
    var actionsExpanded by remember(task.id) { mutableStateOf(false) }
    val colors = MaterialTheme.colorScheme

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 54.dp)
            .clickable(role = Role.RadioButton, onClick = onSelect)
            .semantics {
                selected = isSelected
                role = Role.RadioButton
            },
        color = if (isSelected) colors.surfaceContainerHigh else colors.surfaceContainerLowest,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(
            width = 1.dp,
            color = if (isSelected) colors.onSurface.copy(alpha = 0.42f) else colors.outlineVariant,
        ),
    ) {
        Row(
            modifier = Modifier.padding(start = 14.dp, end = 4.dp, top = 7.dp, bottom = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SelectionMark(isSelected = isSelected)
            Spacer(Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = task.title,
                    style = TokiTypography.bodyTitle,
                    color = colors.onSurface,
                    maxLines = 1,
                )
                if (task.targetPomodoros > 0) {
                    Text(
                        text = "${task.completedPomodoros}/${task.targetPomodoros} sessions 🍅",
                        style = TokiTypography.supporting,
                        color = colors.onSurfaceVariant,
                    )
                }
            }

            Box {
                IconButton(
                    onClick = { actionsExpanded = true },
                    modifier = Modifier
                        .size(48.dp)
                        .semantics {
                            contentDescription = "Task actions for ${task.title}"
                        },
                ) {
                    Icon(
                        imageVector = Icons.Filled.MoreVert,
                        contentDescription = null,
                        tint = colors.onSurfaceVariant,
                        modifier = Modifier.size(24.dp),
                    )
                }

                DropdownMenu(
                    expanded = actionsExpanded,
                    onDismissRequest = { actionsExpanded = false },
                    containerColor = colors.surfaceContainerHigh,
                ) {
                    DropdownMenuItem(
                        text = { Text("Mark complete") },
                        leadingIcon = {
                            Icon(Icons.Outlined.CheckCircle, contentDescription = null)
                        },
                        onClick = {
                            actionsExpanded = false
                            onComplete()
                        },
                    )
                    DropdownMenuItem(
                        text = { Text("Delete") },
                        leadingIcon = {
                            Icon(Icons.Outlined.Delete, contentDescription = null)
                        },
                        onClick = {
                            actionsExpanded = false
                            onDelete()
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun NoTaskRow(
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 66.dp)
            .clickable(role = Role.RadioButton, onClick = onClick)
            .semantics {
                selected = isSelected
                role = Role.RadioButton
            }
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SelectionMark(isSelected = isSelected)
        Spacer(Modifier.width(14.dp))
        Column {
            Text(
                text = "No task selected",
                style = TokiTypography.bodyTitle,
                color = colors.onSurface,
            )
            Text(
                text = "Focus without a task",
                style = TokiTypography.supporting,
                color = colors.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SelectionMark(isSelected: Boolean) {
    val colors = MaterialTheme.colorScheme
    Box(
        modifier = Modifier
            .size(22.dp)
            .then(
                if (isSelected) {
                    Modifier.background(colors.onSurface, CircleShape)
                } else {
                    Modifier.border(2.dp, colors.onSurfaceVariant, CircleShape)
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (isSelected) {
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = null,
                tint = colors.inverseSurface,
                modifier = Modifier.size(15.dp),
            )
        }
    }
}

private val ColorSheet = androidx.compose.ui.graphics.Color(0xFF171717)

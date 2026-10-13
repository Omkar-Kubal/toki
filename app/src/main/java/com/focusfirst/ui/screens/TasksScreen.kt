package com.focusfirst.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.focusfirst.R
import com.focusfirst.billing.BillingViewModel
import com.focusfirst.data.model.TaskEntity
import com.focusfirst.ui.components.TaskCreationSheet
import com.focusfirst.ui.components.TaskDatePickerDialog
import com.focusfirst.ui.theme.TokiTypography
import com.focusfirst.util.TaskTimerDeepLink
import com.focusfirst.util.TaskWeek
import com.focusfirst.viewmodel.TaskViewModel
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun TasksScreen(
    onNavigateToHome: () -> Unit,
    onNavigateToSettings: () -> Unit,
    viewModel: TaskViewModel = hiltViewModel(),
    billingViewModel: BillingViewModel = hiltViewModel(),
) {
    val selectedDate by viewModel.selectedDate.collectAsStateWithLifecycle()
    val tasks by viewModel.tasksForSelectedDate.collectAsStateWithLifecycle()
    val overdueTasks by viewModel.overdueTasks.collectAsStateWithLifecycle()
    val tags by viewModel.tags.collectAsStateWithLifecycle()
    val isPro by billingViewModel.isPro.collectAsStateWithLifecycle()
    val activeCount by viewModel.activeCount.collectAsStateWithLifecycle()
    val context = androidx.compose.ui.platform.LocalContext.current

    var showCreateSheet by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }

    val doneTasks = tasks.filter(::isTaskComplete)
    val openTasks = tasks.filterNot(::isTaskComplete)
    val visibleOverdue = if (selectedDate == LocalDate.now()) {
        overdueTasks.filterNot(::isTaskComplete)
    } else {
        emptyList()
    }
    val canAddTask = isPro || activeCount < TaskViewModel.FREE_TASK_LIMIT

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { TasksTopBar(onSettingsClick = onNavigateToSettings) },
        bottomBar = {
            AddTaskAction(
                canAddTask = canAddTask,
                onClick = {
                    if (canAddTask) showCreateSheet = true
                    else billingViewModel.openUpgradeSheet()
                },
            )
        },
    ) { contentPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(contentPadding)
                .padding(horizontal = 22.dp),
        ) {
            item {
                Column(modifier = Modifier.padding(top = 10.dp)) {
                    Text(
                        text = "Tasks",
                        style = TokiTypography.heroMetric.copy(fontSize = 34.sp, lineHeight = 40.sp),
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                    Spacer(Modifier.height(3.dp))
                    Text(
                        text = formatTaskHeading(selectedDate),
                        style = TokiTypography.action.copy(letterSpacing = 1.2.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .clickable { showDatePicker = true }
                            .semantics {
                                role = Role.Button
                                contentDescription = "Choose date, ${formatTaskHeading(selectedDate)}"
                            }
                            .padding(vertical = 2.dp),
                    )
                }
            }

            item {
                WeekStrip(
                    selectedDate = selectedDate,
                    onDateSelected = viewModel::selectDate,
                    modifier = Modifier.padding(top = 18.dp, bottom = 20.dp),
                )
            }

            if (visibleOverdue.isNotEmpty()) {
                item { SectionHeading(title = "OVERDUE", count = "${visibleOverdue.size} tasks") }
                items(visibleOverdue, key = { "overdue-${it.id}" }) { task ->
                    TaskRow(
                        task = task,
                        onStatusChange = { viewModel.updateTaskStatus(task, it) },
                        onStartFocus = {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(TaskTimerDeepLink.forTask(task)))
                            onNavigateToHome()
                            context.startActivity(intent)
                        },
                    )
                }
                item { Spacer(Modifier.height(20.dp)) }
            }

            item {
                SectionHeading(
                    title = if (selectedDate == LocalDate.now()) "TODAY" else "TASKS",
                    count = "${openTasks.size} ${if (openTasks.size == 1) "task" else "tasks"}",
                )
            }

            if (openTasks.isEmpty() && doneTasks.isEmpty()) {
                item {
                    Text(
                        text = "No tasks for this day",
                        style = TokiTypography.bodyTitle,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 36.dp),
                    )
                }
            } else {
                items(openTasks, key = { "open-${it.id}" }) { task ->
                    TaskRow(
                        task = task,
                        onStatusChange = { viewModel.updateTaskStatus(task, it) },
                        onStartFocus = {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(TaskTimerDeepLink.forTask(task)))
                            onNavigateToHome()
                            context.startActivity(intent)
                        },
                    )
                }

                if (doneTasks.isNotEmpty()) {
                    item {
                        Spacer(Modifier.height(22.dp))
                        SectionHeading(title = "COMPLETED", count = null)
                    }
                    items(doneTasks, key = { "done-${it.id}" }) { task ->
                        TaskRow(
                            task = task,
                            onStatusChange = { viewModel.updateTaskStatus(task, it) },
                            onStartFocus = {},
                        )
                    }
                }
            }

            item { Spacer(Modifier.height(18.dp)) }
        }
    }

    if (showCreateSheet) {
        TaskCreationSheet(
            initialDate = selectedDate,
            isPro = isPro,
            tags = tags,
            onUpgradeClick = {
                showCreateSheet = false
                billingViewModel.openUpgradeSheet()
            },
            onCreateTag = viewModel::addTag,
            onConfirm = { title, description, date, duration, tag ->
                viewModel.addTask(
                    title = title,
                    description = description,
                    date = date,
                    durationMinutes = duration,
                    tagName = tag,
                )
                showCreateSheet = false
            },
            onDismiss = { showCreateSheet = false },
        )
    }

    if (showDatePicker) {
        TaskDatePickerDialog(
            initialDate = selectedDate,
            onDateSelected = {
                viewModel.selectDate(it)
                showDatePicker = false
            },
            onDismiss = { showDatePicker = false },
        )
    }
}

@Composable
private fun TasksTopBar(onSettingsClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp)
            .padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
            painter = painterResource(R.drawable.ic_launcher_foreground),
            contentDescription = null,
            modifier = Modifier.size(dimensionResource(R.dimen.header_logo_size)),
        )
        Spacer(Modifier.width(dimensionResource(R.dimen.header_logo_title_gap)))
        Text(text = "Toki", style = TokiTypography.pageTitle, color = colors.onBackground)
        Spacer(Modifier.weight(1f))
        IconButton(
            onClick = onSettingsClick,
            modifier = Modifier
                .size(dimensionResource(R.dimen.header_settings_size))
                .semantics { contentDescription = "Settings" },
        ) {
            Icon(
                imageVector = Icons.Outlined.Settings,
                contentDescription = null,
                modifier = Modifier.size(28.dp),
                tint = colors.onBackground,
            )
        }
    }
}

@Composable
private fun WeekStrip(
    selectedDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val dates = remember(selectedDate) { TaskWeek.datesFor(selectedDate) }
    Row(modifier = modifier.fillMaxWidth()) {
        dates.forEach { date ->
            val isSelected = date == selectedDate
            Column(
                modifier = Modifier
                    .weight(1f)
                    .semantics(mergeDescendants = true) {
                        role = Role.Button
                        contentDescription = date.format(DateTimeFormatter.ofPattern("EEEE, MMMM d", Locale.getDefault()))
                        selected = isSelected
                    }
                    .clickable { onDateSelected(date) },
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = date.format(DateTimeFormatter.ofPattern("EEE", Locale.getDefault())).uppercase(Locale.getDefault()),
                    style = TokiTypography.sectionLabel,
                    color = if (isSelected) colors.onSurface else colors.onSurfaceVariant,
                )
                Spacer(Modifier.height(6.dp))
                Box(
                    modifier = Modifier
                        .size(width = 38.dp, height = 38.dp)
                        .clip(CircleShape)
                        .background(if (isSelected) colors.primary else Color.Transparent),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = date.dayOfMonth.toString(),
                        style = TokiTypography.bodyTitle,
                        color = if (isSelected) colors.onPrimary else colors.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionHeading(title: String, count: String?) {
    Column(modifier = Modifier.padding(bottom = 8.dp, top = 2.dp)) {
        Text(
            text = title,
            style = TokiTypography.action.copy(letterSpacing = 0.8.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        count?.let {
            Spacer(Modifier.height(2.dp))
            Text(text = it, style = TokiTypography.bodyTitle, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun TaskRow(
    task: TaskEntity,
    onStatusChange: (String) -> Unit,
    onStartFocus: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val completed = isTaskComplete(task)

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(70.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(
                onClick = { onStatusChange(if (completed) "TODO" else "DONE") },
                modifier = Modifier.size(42.dp),
            ) {
                Icon(
                    imageVector = if (completed) Icons.Filled.CheckCircle else Icons.Outlined.RadioButtonUnchecked,
                    contentDescription = if (completed) "Mark ${task.title} incomplete" else "Complete ${task.title}",
                    tint = if (completed) colors.onSurfaceVariant.copy(alpha = 0.7f) else colors.onSurfaceVariant,
                    modifier = Modifier.size(24.dp),
                )
            }
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = task.title,
                    style = TokiTypography.bodyTitle,
                    color = if (completed) colors.onSurfaceVariant.copy(alpha = 0.65f) else colors.onSurface,
                    textDecoration = if (completed) TextDecoration.LineThrough else null,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = taskDetailText(task),
                    style = TokiTypography.action,
                    color = colors.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.width(6.dp))
            IconButton(
                onClick = onStartFocus,
                enabled = !completed,
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(colors.surfaceContainerLow),
            ) {
                Icon(
                    imageVector = Icons.Filled.PlayArrow,
                    contentDescription = if (completed) "${task.title} completed" else "Start ${task.title}",
                    tint = if (completed) colors.onSurfaceVariant.copy(alpha = 0.35f) else colors.onSurface,
                    modifier = Modifier.size(23.dp),
                )
            }
        }
        HorizontalDivider(color = colors.outline.copy(alpha = 0.7f), thickness = 0.7.dp)
    }
}

@Composable
private fun AddTaskAction(canAddTask: Boolean, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.background)
            .padding(horizontal = 22.dp)
            .padding(top = 8.dp, bottom = 10.dp),
    ) {
        Button(
            onClick = onClick,
            modifier = Modifier.fillMaxWidth().height(58.dp),
            shape = RoundedCornerShape(32.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (canAddTask) colors.primary else colors.surfaceContainerHigh,
                contentColor = if (canAddTask) colors.onPrimary else colors.onSurfaceVariant,
            ),
        ) {
            if (canAddTask) {
                Icon(imageVector = Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(23.dp))
            }
            Spacer(Modifier.width(10.dp))
            Text(
                text = if (canAddTask) "Add task" else "Task limit reached",
                style = TokiTypography.button.copy(fontSize = 16.sp),
            )
        }
    }
}

private fun isTaskComplete(task: TaskEntity): Boolean = task.isCompleted || task.status == "DONE"

private fun formatTaskHeading(date: LocalDate): String =
    date.format(DateTimeFormatter.ofPattern("EEE, MMM d", Locale.getDefault())).uppercase(Locale.getDefault())

private fun taskDetailText(task: TaskEntity): String {
    val dueTime = task.dueTime?.let { millis ->
        val time = LocalTime.ofNanoOfDay((millis % 86_400_000L) * 1_000_000L)
        time.format(DateTimeFormatter.ofPattern("h:mm a", Locale.getDefault()))
    }
    return if (dueTime != null) {
        "${task.durationMinutes} min  •  Due $dueTime"
    } else {
        "${task.durationMinutes} min  •  ${task.completedPomodoros}/${task.targetPomodoros} focus blocks"
    }
}

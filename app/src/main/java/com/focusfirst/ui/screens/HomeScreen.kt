package com.focusfirst.ui.screens

import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.TaskAlt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.focusfirst.R
import com.focusfirst.billing.BillingViewModel
import com.focusfirst.data.model.AmbientSound
import com.focusfirst.data.model.IntervalPreset
import com.focusfirst.data.model.TimerMode
import com.focusfirst.data.model.TimerPhase
import com.focusfirst.data.model.TimerState
import com.focusfirst.ui.components.BreakSuggestionSheet
import com.focusfirst.ui.components.SoundSelectorSheet
import com.focusfirst.ui.components.TaskSheet
import com.focusfirst.ui.components.TokiChronoRing
import com.focusfirst.ui.components.TokiIconButton
import com.focusfirst.ui.home.TimerUiText
import com.focusfirst.ui.theme.TokiTypography
import com.focusfirst.viewmodel.SettingsViewModel
import com.focusfirst.viewmodel.TaskViewModel
import com.focusfirst.viewmodel.TimerViewModel
import com.focusfirst.util.selectedTaskIdAfterSelectionAttempt

@Composable
fun HomeScreen(
    viewModel: TimerViewModel = hiltViewModel(),
    settingsViewModel: SettingsViewModel = hiltViewModel(),
    billingViewModel: BillingViewModel = hiltViewModel(),
    taskViewModel: TaskViewModel = hiltViewModel(),
    onNavigateToSettings: () -> Unit = {},
    onNavigateToTasks: () -> Unit = {},
) {
    val timerState by viewModel.timerState.collectAsStateWithLifecycle()
    val totalCompleted by viewModel.totalCompleted.collectAsStateWithLifecycle()
    val ambientSound by settingsViewModel.ambientSound.collectAsStateWithLifecycle()
    val ambientVolume by settingsViewModel.ambientVolume.collectAsStateWithLifecycle()
    val sessionsBeforeLongBreak by settingsViewModel.sessionsBeforeLongBreak.collectAsStateWithLifecycle()
    val isPro by billingViewModel.isPro.collectAsStateWithLifecycle()
    val activeTasks by taskViewModel.activeTasks.collectAsStateWithLifecycle()
    val activeCount by taskViewModel.activeCount.collectAsStateWithLifecycle()

    val context = LocalContext.current
    var showCancelDialog by rememberSaveable { mutableStateOf(false) }
    var showSoundSheet by remember { mutableStateOf(false) }
    var showBreakSheet by remember { mutableStateOf(false) }
    var showTaskSheet by remember { mutableStateOf(false) }
    var isLongBreak by remember { mutableStateOf(false) }

    val selectedTask = activeTasks.find { it.id == taskViewModel.selectedTaskId }
    val timerActive = timerState.isRunning || timerState.isPaused
    val uiText = TimerUiText.from(timerState, timerState.timerMode, sessionsBeforeLongBreak)

    val window = (context as? ComponentActivity)?.window
    DisposableEffect(timerActive) {
        if (timerActive) window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose { window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) }
    }

    LaunchedEffect(timerState.phase) {
        if (timerState.isRunning && timerState.timerMode == TimerMode.POMODORO) {
            when (timerState.phase) {
                TimerPhase.SHORT_BREAK -> {
                    isLongBreak = false
                    showBreakSheet = true
                }
                TimerPhase.LONG_BREAK -> {
                    isLongBreak = true
                    showBreakSheet = true
                }
                TimerPhase.FOCUS -> Unit
            }
        }
    }

    CancelDialog(
        visible = showCancelDialog,
        onCancel = {
            viewModel.cancel()
            showCancelDialog = false
        },
        onDismiss = { showCancelDialog = false },
    )

    if (showSoundSheet) {
        SoundSelectorSheet(
            currentSound = ambientSound,
            currentVolume = ambientVolume,
            isPro = isPro,
            onSoundSelected = { sound ->
                settingsViewModel.updateAmbientSound(sound)
                com.focusfirst.analytics.TokiAnalytics.logSoundSelected(sound.displayName)
                viewModel.updateSound(sound, ambientVolume)
            },
            onVolumeChanged = { volume ->
                settingsViewModel.updateAmbientVolume(volume)
                viewModel.updateVolume(volume)
            },
            onDismiss = { showSoundSheet = false },
            onUpgradeClick = {
                showSoundSheet = false
                billingViewModel.openUpgradeSheet()
            },
        )
    }

    if (showTaskSheet) {
        TaskSheet(
            tasks = activeTasks,
            selectedTaskId = taskViewModel.selectedTaskId,
            isPro = isPro,
            activeCount = activeCount,
            onTaskSelected = { task ->
                taskViewModel.selectedTaskId = selectedTaskIdAfterSelectionAttempt(
                    currentTaskId = taskViewModel.selectedTaskId,
                    requestedTaskId = task?.id,
                    timerIsActive = timerActive,
                )
                showTaskSheet = false
            },
            onAddTask = { taskViewModel.addTask(it, date = java.time.LocalDate.now()) },
            onDeleteTask = { taskViewModel.deleteTask(it) },
            onCompleteTask = { taskViewModel.completeTask(it) },
            onUpgradeClick = {
                showTaskSheet = false
                billingViewModel.openUpgradeSheet()
            },
            onManageTasks = {
                showTaskSheet = false
                onNavigateToTasks()
            },
            onDismiss = { showTaskSheet = false },
        )
    }

    if (showBreakSheet) {
        BreakSuggestionSheet(
            isLongBreak = isLongBreak,
            breakDurationSeconds = timerState.totalSeconds,
            breakSessionCount = totalCompleted,
            onDismiss = {
                showBreakSheet = false
                com.focusfirst.analytics.TokiAnalytics.logBreakSuggestionDismissed()
            },
        )
    }

    val primaryAction: () -> Unit = when {
        timerState.isRunning -> ({ viewModel.pause() })
        timerState.isPaused -> ({ viewModel.resume() })
        else -> ({ viewModel.start() })
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        FocusTopBar(onSettingsClick = onNavigateToSettings)

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(bottom = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(if (timerActive) 12.dp else 16.dp))

            if (timerActive) {
                FocusActiveLayout(
                    timerState = timerState,
                    uiText = uiText,
                    selectedTaskTitle = selectedTask?.title,
                    ambientSound = ambientSound,
                    onSoundClick = { showSoundSheet = true },
                    onTaskClick = { showTaskSheet = true },
                    onStopClick = { viewModel.stop() },
                    onCancelClick = { showCancelDialog = true },
                    onPrimaryAction = primaryAction,
                )
            } else {
                FocusIdleLayout(
                    timerState = timerState,
                    uiText = uiText,
                    selectedTaskTitle = selectedTask?.title,
                    onPresetSelected = { minutes ->
                        val preset = when (minutes) {
                            15 -> IntervalPreset.QUICK
                            35 -> IntervalPreset.DEEP
                            else -> IntervalPreset.CLASSIC
                        }
                        viewModel.selectPreset(preset)
                    },
                    onDurationSelected = { viewModel.selectDuration(it) },
                    onTaskClick = { showTaskSheet = true },
                    onPrimaryAction = primaryAction,
                )
            }
        }
    }
}

@Composable
private fun FocusTopBar(
    onSettingsClick: () -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
            painter = painterResource(R.drawable.ic_launcher_foreground),
            contentDescription = null,
            modifier = Modifier
                .size(dimensionResource(R.dimen.header_logo_size))
                .testTag("home-brand-mark"),
        )
        Spacer(Modifier.width(dimensionResource(R.dimen.header_logo_title_gap)))
        Text(
            text = "Toki",
            color = cs.onBackground,
            style = TokiTypography.pageTitle,
        )
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
                modifier = Modifier.size(30.dp),
                tint = cs.onBackground,
            )
        }
    }
}

@Composable
private fun FocusIdleLayout(
    timerState: TimerState,
    uiText: TimerUiText,
    selectedTaskTitle: String?,
    onPresetSelected: (Int) -> Unit,
    onDurationSelected: (Int) -> Unit,
    onTaskClick: () -> Unit,
    onPrimaryAction: () -> Unit,
) {
    FocusTimerBlock(
        timerState = timerState,
        uiText = uiText,
        onDurationSelected = onDurationSelected,
    )
    Spacer(Modifier.height(14.dp))
    DurationPresetSelector(
        selectedMinutes = timerState.totalSeconds / 60,
        onPresetSelected = onPresetSelected,
    )
    Spacer(Modifier.height(24.dp))
    FocusStartButton(onClick = onPrimaryAction)
    Spacer(Modifier.height(22.dp))
    NextTaskRow(
        taskTitle = selectedTaskTitle,
        onClick = onTaskClick,
    )
}

@Composable
private fun DurationPresetSelector(
    selectedMinutes: Int,
    onPresetSelected: (Int) -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    val presets = listOf(15, 25, 35)
    Row(
        modifier = Modifier
            .fillMaxWidth(0.73f)
            .clip(CircleShape)
            .background(cs.surfaceContainerLow)
            .padding(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        presets.forEachIndexed { index, minutes ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
                    .clip(CircleShape)
                    .background(if (selectedMinutes == minutes) cs.primary else Color.Transparent)
                    .clickable(
                        role = Role.RadioButton,
                        onClick = { onPresetSelected(minutes) },
                    )
                    .semantics {
                        contentDescription = "$minutes minutes"
                        selected = selectedMinutes == minutes
                    },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = minutes.toString(),
                    color = if (selectedMinutes == minutes) cs.onPrimary else cs.onSurfaceVariant,
                    style = TokiTypography.bodyTitle,
                )
            }
            if (index < presets.lastIndex) {
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(18.dp)
                        .background(cs.outline),
                )
            }
        }
    }
}

@Composable
private fun FocusStartButton(onClick: () -> Unit) {
    Button(
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        colors = androidx.compose.material3.ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
        ),
        modifier = Modifier
            .fillMaxWidth()
            .height(62.dp)
            .semantics {
                role = Role.Button
                contentDescription = "Start focus"
            },
    ) {
        Text(
            text = "Start focus",
            // 16sp is the registry's reviewed exception for a prominent primary CTA.
            style = TokiTypography.button.copy(fontSize = 16.sp),
        )
    }
}

@Composable
private fun NextTaskRow(
    taskTitle: String?,
    onClick: () -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 68.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(role = Role.Button, onClick = onClick)
            .semantics {
                contentDescription = taskTitle?.let { "Next task: $it" } ?: "Choose next task"
            },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "NEXT TASK",
                color = cs.onSurfaceVariant,
                style = TokiTypography.sectionLabel.copy(letterSpacing = 1.2.sp),
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = taskTitle ?: "Choose a task",
                color = cs.onBackground,
                style = TokiTypography.bodyTitle,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Icon(
            imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
            contentDescription = null,
            modifier = Modifier.size(28.dp),
            tint = cs.onSurfaceVariant,
        )
    }
}

@Composable
private fun FocusActiveLayout(
    timerState: TimerState,
    uiText: TimerUiText,
    selectedTaskTitle: String?,
    ambientSound: AmbientSound,
    onSoundClick: () -> Unit,
    onTaskClick: () -> Unit,
    onStopClick: () -> Unit,
    onCancelClick: () -> Unit,
    onPrimaryAction: () -> Unit,
) {
    PhaseChip(uiText.phase, uiText.cycle, timerState.isPaused)
    Spacer(Modifier.height(18.dp))
    FocusTimerBlock(timerState, uiText)
    selectedTaskTitle?.let { taskTitle ->
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Task: $taskTitle",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = TokiTypography.action,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
    Spacer(Modifier.height(20.dp))
    FocusControls(
        primaryLabel = uiText.primaryAction,
        primaryIcon = if (timerState.isRunning) Icons.Filled.Pause else Icons.Filled.PlayArrow,
        onPrimaryAction = onPrimaryAction,
        showStop = true,
        onStopClick = onStopClick,
        onCancelClick = onCancelClick,
        ambientSound = ambientSound,
        selectedTaskTitle = selectedTaskTitle,
        onSoundClick = onSoundClick,
        onTaskClick = onTaskClick,
    )
}

@Composable
private fun PhaseChip(phase: String, cycle: String, paused: Boolean) {
    val cs = MaterialTheme.colorScheme
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(cs.surfaceContainerLow)
            .border(1.dp, cs.outline, RoundedCornerShape(999.dp))
            .padding(horizontal = 16.dp, vertical = 9.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = if (paused) "$phase paused - $cycle" else "$phase - $cycle",
            color = cs.onSurface,
            style = TokiTypography.sectionLabel,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun FocusTimerBlock(
    timerState: TimerState,
    uiText: TimerUiText,
    onDurationSelected: ((Int) -> Unit)? = null,
) {
    TokiChronoRing(
        displayTime = timerState.displayTime,
        progress = timerState.progress,
        accessibilitySummary = uiText.accessibilitySummary,
        supportingText = uiText.phase,
        isPaused = timerState.isPaused,
        dialMinutes = (timerState.totalSeconds / 60).takeIf {
            timerState.isIdle && it in 5..60
        },
        onDialMinutesChange = { minutes -> onDurationSelected?.invoke(minutes) },
    )
}

@Composable
private fun FocusControls(
    primaryLabel: String,
    primaryIcon: androidx.compose.ui.graphics.vector.ImageVector,
    onPrimaryAction: () -> Unit,
    showStop: Boolean,
    onStopClick: () -> Unit,
    onCancelClick: () -> Unit,
    ambientSound: AmbientSound,
    selectedTaskTitle: String?,
    onSoundClick: () -> Unit,
    onTaskClick: () -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    if (showStop) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(24.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TokiIconButton(Icons.Filled.Stop, "Stop", active = false, onClick = onStopClick)
            TokiIconButton(primaryIcon, primaryLabel, active = true, onClick = onPrimaryAction)
            TokiIconButton(Icons.Outlined.Close, "Cancel", active = false, onClick = onCancelClick)
        }
        Spacer(Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally),
        ) {
            TokiIconButton(
                icon = Icons.Outlined.MusicNote,
                label = if (ambientSound == AmbientSound.NONE) "Choose ambient sound" else "Ambient sound: ${ambientSound.displayName}",
                active = ambientSound != AmbientSound.NONE,
                onClick = onSoundClick,
            )
            TokiIconButton(
                icon = Icons.Outlined.TaskAlt,
                label = selectedTaskTitle?.let { "Session task: $it" } ?: "Choose session task",
                active = selectedTaskTitle != null,
                onClick = onTaskClick,
            )
        }
        return
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TokiIconButton(
            icon = Icons.Outlined.MusicNote,
            label = if (ambientSound == AmbientSound.NONE) "Choose ambient sound" else "Ambient sound: ${ambientSound.displayName}",
            active = ambientSound != AmbientSound.NONE,
            onClick = onSoundClick,
        )
        Spacer(Modifier.width(18.dp))
        Button(
            onClick = onPrimaryAction,
            shape = CircleShape,
            colors = ButtonDefaults.buttonColors(
                containerColor = cs.primary,
                contentColor = cs.onPrimary,
            ),
            modifier = Modifier
                .height(64.dp)
                .semantics {
                    role = Role.Button
                    contentDescription = primaryLabel
                },
        ) {
            Icon(
                imageVector = primaryIcon,
                contentDescription = null,
                modifier = Modifier.size(28.dp),
            )
            Spacer(Modifier.width(10.dp))
            Text(primaryLabel, style = TokiTypography.button.copy(fontSize = 16.sp))
        }
        Spacer(Modifier.width(18.dp))
        TokiIconButton(
            icon = Icons.Outlined.TaskAlt,
            label = selectedTaskTitle?.let { "Session task: $it" } ?: "Choose session task",
            active = selectedTaskTitle != null,
            onClick = onTaskClick,
        )
    }
}

@Composable
private fun CancelDialog(
    visible: Boolean,
    onCancel: () -> Unit,
    onDismiss: () -> Unit,
) {
    if (!visible) return
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Cancel this session?") },
        text = { Text("This session's progress will be discarded.") },
        confirmButton = {
            TextButton(onClick = onCancel) { Text("Cancel session") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Keep focusing") }
        },
    )
}

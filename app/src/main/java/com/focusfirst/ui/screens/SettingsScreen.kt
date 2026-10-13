package com.focusfirst.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.GpsFixed
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.MailOutline
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.NotificationsOff
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.outlined.Vibration
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.focusfirst.BuildConfig
import com.focusfirst.R
import com.focusfirst.billing.BillingViewModel
import com.focusfirst.data.model.AmbientSound
import com.focusfirst.ui.components.SoundSelectorSheet
import com.focusfirst.ui.components.TokiPill
import com.focusfirst.ui.theme.TokiTypography
import com.focusfirst.util.DndManager
import com.focusfirst.util.DndManagerEntryPoint
import com.focusfirst.util.ThemeModePolicy
import com.focusfirst.viewmodel.SettingsViewModel
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun SettingsScreen(
    settingsViewModel: SettingsViewModel = hiltViewModel(),
    billingViewModel: BillingViewModel = hiltViewModel(),
    @Suppress("UNUSED_PARAMETER") onNavigateToLicenses: () -> Unit = {},
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()
    val dndManager: DndManager = remember {
        EntryPointAccessors.fromApplication(
            context.applicationContext,
            DndManagerEntryPoint::class.java,
        ).dndManager()
    }

    val focusMinutes by settingsViewModel.focusMinutes.collectAsStateWithLifecycle()
    val shortBreak by settingsViewModel.shortBreakMinutes.collectAsStateWithLifecycle()
    val longBreak by settingsViewModel.longBreakMinutes.collectAsStateWithLifecycle()
    val sessionsBefore by settingsViewModel.sessionsBeforeLongBreak.collectAsStateWithLifecycle()
    val dailyGoal by settingsViewModel.dailyGoal.collectAsStateWithLifecycle()
    val vibrateEnabled by settingsViewModel.vibrate.collectAsStateWithLifecycle()
    val amoledMode by settingsViewModel.amoledMode.collectAsStateWithLifecycle()
    val themeMode by settingsViewModel.themeMode.collectAsStateWithLifecycle()
    val dndEnabled by settingsViewModel.dndEnabled.collectAsStateWithLifecycle()
    val ambientSound by settingsViewModel.ambientSound.collectAsStateWithLifecycle()
    val ambientVolume by settingsViewModel.ambientVolume.collectAsStateWithLifecycle()
    val isPro by billingViewModel.isPro.collectAsStateWithLifecycle()
    val proPrice by billingViewModel.proPrice.collectAsStateWithLifecycle()

    var showSoundSheet by remember { mutableStateOf(false) }
    var showFocusGuard by remember { mutableStateOf(false) }
    var dndPermissionGranted by remember { mutableStateOf(dndManager.isDndPermissionGranted()) }
    var durationToEdit by remember { mutableStateOf<DurationEditState?>(null) }
    var showDailyGoalChoices by remember { mutableStateOf(false) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                dndPermissionGranted = dndManager.isDndPermissionGranted()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    if (showFocusGuard) {
        FocusGuardScreen(onBack = { showFocusGuard = false })
        return
    }

    if (showSoundSheet) {
        SoundSelectorSheet(
            currentSound = ambientSound,
            currentVolume = ambientVolume,
            isPro = isPro,
            onSoundSelected = {
                settingsViewModel.updateAmbientSound(it)
                com.focusfirst.analytics.TokiAnalytics.logSoundSelected(it.displayName)
            },
            onVolumeChanged = { settingsViewModel.updateAmbientVolume(it) },
            onDismiss = { showSoundSheet = false },
            onUpgradeClick = {
                showSoundSheet = false
                billingViewModel.openUpgradeSheet()
            },
        )
    }

    durationToEdit?.let { editState ->
        DurationEditorDialog(
            editState = editState,
            onDismiss = { durationToEdit = null },
            onSave = { value, sessionsBeforeLongBreak ->
                editState.onSave(value)
                sessionsBeforeLongBreak?.let { editState.onSessionsBeforeLongBreakSave?.invoke(it) }
                durationToEdit = null
            },
        )
    }

    if (showDailyGoalChoices) {
        OptionSelectorDialog(
            title = "Daily goal",
            description = "Focus sessions per day",
            options = listOf(4, 6, 8, 10, 12),
            selected = dailyGoal,
            onSelect = {
                settingsViewModel.updateDailyGoal(it)
                showDailyGoalChoices = false
            },
            onDismiss = { showDailyGoalChoices = false },
        )
    }

    val privacyUrl = stringResource(R.string.privacy_policy_url)
    val supportEmail = stringResource(R.string.support_email)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        SettingsTopBar(
            onScrollToTop = { coroutineScope.launch { scrollState.animateScrollTo(0) } },
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(scrollState)
                .padding(horizontal = 22.dp)
                .padding(bottom = 56.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Spacer(Modifier.height(2.dp))
            SettingsHeader()
            ProPanel(
                isPro = isPro,
                proPrice = proPrice,
                onUpgrade = { billingViewModel.openUpgradeSheet() },
            )

            SettingsGroup(
                title = "Timer",
                subtitle = "Set how your focus sessions work",
            ) {
                SettingActionRow(
                    icon = Icons.Outlined.Timer,
                    title = "Focus",
                    subtitle = "Length of each focus session",
                    trailingText = "${focusMinutes.coerceIn(10, 60)} min",
                    onClick = {
                        durationToEdit = DurationEditState(
                            title = "Focus duration",
                            value = focusMinutes.coerceIn(10, 60),
                            range = 10..60,
                            onSave = settingsViewModel::updateFocusMinutes,
                        )
                    },
                )
                GroupDivider()
                SettingActionRow(
                    icon = Icons.Outlined.Schedule,
                    title = "Short break",
                    subtitle = "Break between focus sessions",
                    trailingText = "${shortBreak.coerceIn(1, 15)} min",
                    onClick = {
                        durationToEdit = DurationEditState(
                            title = "Short break duration",
                            value = shortBreak.coerceIn(1, 15),
                            range = 1..15,
                            onSave = settingsViewModel::updateShortBreakMinutes,
                        )
                    },
                )
                GroupDivider()
                SettingActionRow(
                    icon = Icons.Outlined.Schedule,
                    title = "Long break",
                    subtitle = "Take a longer break after $sessionsBefore focus sessions",
                    trailingText = "${longBreak.coerceIn(10, 30)} min",
                    onClick = {
                        durationToEdit = DurationEditState(
                            title = "Long break settings",
                            value = longBreak.coerceIn(10, 30),
                            range = 10..30,
                            onSave = settingsViewModel::updateLongBreakMinutes,
                            sessionsBeforeLongBreak = sessionsBefore,
                            onSessionsBeforeLongBreakSave = settingsViewModel::updateSessionsBeforeLongBreak,
                        )
                    },
                )
            }

            SettingsCard {
                SettingActionRow(
                    icon = Icons.Outlined.GpsFixed,
                    title = "Daily goal",
                    subtitle = "Target number of focus sessions",
                    trailingText = "${dailyGoal.coerceIn(1, 20)} sessions",
                    onClick = { showDailyGoalChoices = true },
                )
            }

            SettingsGroup(
                title = "Focus aids",
                subtitle = "Reduce distractions, stay in the zone",
            ) {
                SettingActionRow(
                    icon = Icons.Outlined.Lock,
                    title = "Focus Guard",
                    subtitle = if (isPro) "Block distracting apps during focus" else "Pro feature",
                    onClick = {
                        if (isPro) showFocusGuard = true else billingViewModel.openUpgradeSheet()
                    },
                )
                GroupDivider()
                if (dndPermissionGranted) {
                    SettingsSwitchRow(
                        icon = Icons.Outlined.NotificationsOff,
                        title = "Auto-enable DND",
                        subtitle = "Silence notifications during focus",
                        checked = dndEnabled,
                        onCheckedChange = {
                            settingsViewModel.updateDndEnabled(it)
                            com.focusfirst.analytics.TokiAnalytics.logDndToggled(it)
                        },
                    )
                } else {
                    SettingActionRow(
                        icon = Icons.Outlined.NotificationsOff,
                        title = "Do Not Disturb",
                        subtitle = "Permission required",
                        trailingText = "Grant",
                        onClick = { dndManager.requestDndPermission() },
                    )
                }
            }

            SettingsGroup(
                title = "Sound & haptics",
                subtitle = "Subtle feedback for your progress",
            ) {
                SettingActionRow(
                    icon = Icons.Outlined.MusicNote,
                    title = "Ambient sound",
                    trailingText = ambientSound.displayName,
                    onClick = { showSoundSheet = true },
                )
                if (ambientSound != AmbientSound.NONE) {
                    Slider(
                        value = ambientVolume,
                        onValueChange = { settingsViewModel.updateAmbientVolume(it) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .semantics { contentDescription = "Ambient sound volume" },
                        colors = SliderDefaults.colors(
                            thumbColor = MaterialTheme.colorScheme.onSurface,
                            activeTrackColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant,
                        ),
                    )
                }
                GroupDivider()
                SettingsSwitchRow(
                    icon = Icons.Outlined.Vibration,
                    title = "Haptic feedback",
                    subtitle = "Vibrate for important timer events",
                    checked = vibrateEnabled,
                    onCheckedChange = { settingsViewModel.updateVibrate(it) },
                )
            }

            SettingsGroup(
                title = "Appearance",
                subtitle = "A cleaner look for your focus space",
            ) {
                SettingsSwitchRow(
                    icon = Icons.Outlined.DarkMode,
                    title = "AMOLED black",
                    subtitle = "Use pure black surfaces",
                    checked = amoledMode && isPro,
                    badge = "PRO",
                    onCheckedChange = {
                        if (isPro) settingsViewModel.updateAmoledMode(it)
                        else billingViewModel.openUpgradeSheet()
                    },
                )
                GroupDivider()
                SettingsSwitchRow(
                    icon = Icons.Outlined.LightMode,
                    title = "Light mode",
                    subtitle = "Use the light color palette",
                    checked = themeMode == ThemeModePolicy.LIGHT,
                    onCheckedChange = {
                        settingsViewModel.updateThemeMode(
                            if (it) ThemeModePolicy.LIGHT else ThemeModePolicy.DARK,
                        )
                    },
                )
            }

            SettingsGroup(
                title = "Data",
                subtitle = "Manage your focus history",
            ) {
                SettingActionRow(
                    icon = Icons.Outlined.FileDownload,
                    title = "Export CSV",
                    subtitle = "Export all focus sessions",
                    badge = "PRO",
                    onClick = {
                        if (isPro) settingsViewModel.exportData()
                        else billingViewModel.openUpgradeSheet()
                    },
                )
            }

            SettingsGroup(
                title = "About",
                subtitle = "Support and privacy",
            ) {
                SettingActionRow(
                    icon = Icons.Outlined.MailOutline,
                    title = "Contact support",
                    onClick = {
                        val intent = Intent(Intent.ACTION_SENDTO).apply {
                            data = Uri.parse("mailto:")
                            putExtra(Intent.EXTRA_EMAIL, arrayOf(supportEmail))
                            putExtra(Intent.EXTRA_SUBJECT, "Toki Support - v${BuildConfig.VERSION_NAME}")
                        }
                        runCatching { context.startActivity(intent) }
                    },
                )
                GroupDivider()
                SettingActionRow(
                    icon = Icons.Outlined.Lock,
                    title = "Privacy Policy",
                    onClick = {
                        runCatching {
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(privacyUrl)))
                        }
                    },
                )
            }
        }
    }
}

@Composable
private fun SettingsTopBar(onScrollToTop: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
            painter = painterResource(R.drawable.ic_launcher_foreground),
            contentDescription = null,
            modifier = Modifier
                .size(32.dp)
                .semantics { contentDescription = "settings-brand-mark" },
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = "Toki",
            color = colors.onBackground,
            style = TokiTypography.pageTitle,
        )
        Spacer(Modifier.weight(1f))
        IconButton(
            onClick = onScrollToTop,
            modifier = Modifier
                .size(48.dp)
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
private fun SettingsHeader() {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = "Settings",
            color = MaterialTheme.colorScheme.onBackground,
            style = TokiTypography.pageTitle,
        )
        Text(
            text = "Make focus feel like yours",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = TokiTypography.supporting,
        )
    }
}

@Composable
private fun ProPanel(isPro: Boolean, proPrice: String?, onUpgrade: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(18.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(colors.surfaceContainerLow)
            .border(1.dp, colors.outline, shape)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = "Toki Pro",
                color = colors.onSurface,
                style = TokiTypography.cardTitle,
            )
            Text(
                text = if (isPro) {
                    "All focus tools are unlocked. Thank you for supporting Toki."
                } else {
                    "Unlock deeper focus, advanced insights, and more."
                },
                color = colors.onSurfaceVariant,
                style = TokiTypography.supporting,
            )
        }
        if (!isPro) {
            Button(
                onClick = onUpgrade,
                shape = RoundedCornerShape(999.dp),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.surfaceContainerHigh,
                    contentColor = colors.onSurface,
                ),
            ) {
                Text("Explore Pro", style = TokiTypography.button, maxLines = 1)
            }
        } else {
            Text(
                text = proPrice?.let { "Pro · $it" } ?: "Pro",
                color = colors.onSurfaceVariant,
                style = TokiTypography.supporting,
            )
        }
    }
}

@Composable
private fun SettingsGroup(
    title: String,
    subtitle: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = title,
                color = MaterialTheme.colorScheme.onSurface,
                style = TokiTypography.cardTitle,
            )
            Text(
                text = subtitle,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = TokiTypography.supporting,
            )
        }
        SettingsCard(content = content)
    }
}

@Composable
private fun SettingsCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(18.dp)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(colors.surfaceContainerLow)
            .border(1.dp, colors.outline, shape)
            .padding(horizontal = 14.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp),
        content = content,
    )
}

@Composable
private fun SettingActionRow(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    trailingText: String? = null,
    badge: String? = null,
    onClick: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = if (subtitle == null) 48.dp else 56.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = colors.onSurface,
            modifier = Modifier.size(26.dp),
        )
        Spacer(Modifier.width(14.dp))
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = title,
                    color = colors.onSurface,
                    style = TokiTypography.rowTitle,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (badge != null) ProBadge(badge)
            }
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    color = colors.onSurfaceVariant,
                    style = TokiTypography.supporting,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        if (trailingText != null) {
            Spacer(Modifier.width(8.dp))
            Text(
                text = trailingText,
                color = colors.onSurface,
                style = TokiTypography.action,
                maxLines = 1,
            )
        }
        Spacer(Modifier.width(4.dp))
        Icon(
            imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
            contentDescription = null,
            tint = colors.onSurfaceVariant,
            modifier = Modifier.size(22.dp),
        )
    }
}

@Composable
private fun SettingsSwitchRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    badge: String? = null,
    onCheckedChange: (Boolean) -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 56.dp)
            .padding(horizontal = 4.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = colors.onSurface,
            modifier = Modifier.size(26.dp),
        )
        Spacer(Modifier.width(14.dp))
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = title,
                    color = colors.onSurface,
                    style = TokiTypography.rowTitle,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (badge != null) ProBadge(badge)
            }
            Text(
                text = subtitle,
                color = colors.onSurfaceVariant,
                style = TokiTypography.supporting,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.width(8.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = colors.onSurface,
                checkedTrackColor = colors.outline,
                uncheckedThumbColor = colors.onSurfaceVariant,
                uncheckedTrackColor = colors.surfaceVariant,
                checkedBorderColor = colors.outline,
                uncheckedBorderColor = colors.outline,
            ),
        )
    }
}

@Composable
private fun ProBadge(text: String) {
    val colors = MaterialTheme.colorScheme
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(colors.surfaceContainerHigh)
            .padding(horizontal = 8.dp, vertical = 2.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color = colors.onSurfaceVariant,
            style = TokiTypography.sectionLabel,
            maxLines = 1,
        )
    }
}

@Composable
private fun GroupDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(horizontal = 2.dp, vertical = 1.dp),
        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.55f),
    )
}

@Composable
private fun DurationEditorDialog(
    editState: DurationEditState,
    onDismiss: () -> Unit,
    onSave: (Int, Int?) -> Unit,
) {
    var draftValue by remember(editState) {
        mutableStateOf(editState.value.coerceIn(editState.range).toFloat())
    }
    var selectedSessionsBefore by remember(editState) {
        mutableStateOf(editState.sessionsBeforeLongBreak)
    }
    val minValue = editState.range.first.toFloat()
    val maxValue = editState.range.last.toFloat()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(editState.title, style = TokiTypography.cardTitle) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "${draftValue.roundToInt()} min",
                    style = TokiTypography.amountInput,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Slider(
                    value = draftValue,
                    onValueChange = { draftValue = it.roundToInt().toFloat() },
                    valueRange = minValue..maxValue,
                    steps = (editState.range.last - editState.range.first - 1).coerceAtLeast(0),
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.onSurface,
                        activeTrackColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant,
                    ),
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text("${editState.range.first} min", style = TokiTypography.supporting)
                    Text("${editState.range.last} min", style = TokiTypography.supporting)
                }
                if (editState.sessionsBeforeLongBreak != null) {
                    Text(
                        text = "Sessions before long break",
                        color = MaterialTheme.colorScheme.onSurface,
                        style = TokiTypography.rowTitle,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(2, 3, 4, 6).forEach { option ->
                            TokiPill(
                                text = option.toString(),
                                selected = selectedSessionsBefore == option,
                                modifier = Modifier.weight(1f),
                                onClick = { selectedSessionsBefore = option },
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(draftValue.roundToInt(), selectedSessionsBefore) }) {
                Text("Done", style = TokiTypography.button)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", style = TokiTypography.action)
            }
        },
    )
}

@Composable
private fun OptionSelectorDialog(
    title: String,
    description: String,
    options: List<Int>,
    selected: Int,
    onSelect: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, style = TokiTypography.cardTitle) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(description, color = MaterialTheme.colorScheme.onSurfaceVariant, style = TokiTypography.supporting)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    options.forEach { option ->
                        TokiPill(
                            text = option.toString(),
                            selected = selected == option,
                            modifier = Modifier.weight(1f),
                            onClick = { onSelect(option) },
                        )
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", style = TokiTypography.action)
            }
        },
    )
}

private data class DurationEditState(
    val title: String,
    val value: Int,
    val range: IntRange,
    val onSave: (Int) -> Unit,
    val sessionsBeforeLongBreak: Int? = null,
    val onSessionsBeforeLongBreakSave: ((Int) -> Unit)? = null,
)

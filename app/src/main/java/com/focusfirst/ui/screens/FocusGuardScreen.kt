package com.focusfirst.ui.screens

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.outlined.Android
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.focusfirst.data.model.AppInfo
import com.focusfirst.ui.theme.FocusColors
import com.focusfirst.ui.theme.TokiTypography
import com.focusfirst.viewmodel.FocusGuardViewModel

// ─────────────────────────────────────────────────────────────────────────────
// FocusGuardScreen
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Settings screen for Focus Guard — a Pro-only app-blocking feature.
 *
 * Layout:
 *  1. Back navigation header
 *  2. Permission status section (Usage Access)
 *  3. Preset quick-select chips
 *  4. Installed-app list with per-app block toggle
 *  5. Info note about scheduling
 */
@Composable
fun FocusGuardScreen(
    onBack:    () -> Unit = {},
    viewModel: FocusGuardViewModel = hiltViewModel(),
) {
    val context         = LocalContext.current
    val lifecycleOwner  = LocalLifecycleOwner.current

    val installedApps        by viewModel.installedApps.collectAsStateWithLifecycle()
    val blockedApps          by viewModel.blockedApps.collectAsStateWithLifecycle()
    val appsLoading          by viewModel.appsLoading.collectAsStateWithLifecycle()
    val hasUsage             by viewModel.hasUsagePermission.collectAsStateWithLifecycle()

    // Re-check permissions every time this screen resumes (user may have just
    // toggled them in system settings)
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.refreshPermissions()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {

        // ── Header ────────────────────────────────────────────────────────
        item {
            Row(
                modifier          = Modifier
                    .fillMaxWidth()
                    .padding(start = 4.dp, top = 48.dp, end = 16.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector        = Icons.Outlined.ArrowBack,
                        contentDescription = "Back",
                        tint               = Color.White,
                    )
                }
                Spacer(Modifier.width(4.dp))
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text       = "Focus Guard",
                            style      = TokiTypography.pageTitle,
                            color      = Color.White,
                        )
                        Spacer(Modifier.width(8.dp))
                        Surface(
                            color = FocusColors.CommercialBlue.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(4.dp),
                        ) {
                            Text(
                                text     = "PRO",
                                style    = TokiTypography.sectionLabel,
                                color    = FocusColors.CommercialBlue,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            )
                        }
                    }
                    Text(
                        text     = "Block distracting apps during focus sessions",
                        style    = TokiTypography.supporting,
                        color    = Color.White.copy(alpha = 0.45f),
                    )
                }
            }
        }

        // ── Section 1: Permissions ────────────────────────────────────────
        item {
            SectionLabel("Permissions Required")
        }

        item {
            PermissionCard(
                icon        = Icons.Outlined.Security,
                title       = "Usage Access",
                description = "Lets Toki detect which apps are running",
                granted     = hasUsage,
                onGrant     = {
                    context.startActivity(
                        Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                    )
                },
            )
        }

    // Removed Accessibility Service permission card

        if (!hasUsage) {
            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    color  = Color.White.copy(alpha = 0.08f),
                    shape  = RoundedCornerShape(10.dp),
                    border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.3f)),
                ) {
                    Row(
                        modifier          = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector        = Icons.Outlined.Warning,
                            contentDescription = null,
                            tint               = Color.White,
                            modifier           = Modifier.size(16.dp),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text     = "Usage Access permission must be granted for Focus Guard to work.",
                            style    = TokiTypography.supporting,
                            color    = Color.White.copy(alpha = 0.85f),
                        )
                    }
                }
            }
        }

        // ── Section 2: Presets ────────────────────────────────────────────
        item { Spacer(Modifier.height(8.dp)) }
        item { SectionLabel("Quick Presets") }
        item {
            Row(
                modifier              = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                PresetChip(
                    emoji   = "📱",
                    label   = "Social",
                    onClick = { viewModel.applyPresetSocialMedia() },
                )
                PresetChip(
                    emoji   = "🎮",
                    label   = "Games",
                    onClick = { viewModel.applyPresetGames() },
                )
                PresetChip(
                    emoji   = "📺",
                    label   = "Entertainment",
                    onClick = { viewModel.applyPresetEntertainment() },
                )
            }
        }

        // ── Section 3: Blocked apps list ──────────────────────────────────
        item { Spacer(Modifier.height(8.dp)) }
        item {
            Row(
                modifier              = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically,
            ) {
                Text(
                    text       = "Installed Apps",
                    style      = TokiTypography.sectionLabel,
                    color      = Color.White.copy(alpha = 0.5f),
                    letterSpacing = androidx.compose.ui.unit.TextUnit(0.12f,
                        androidx.compose.ui.unit.TextUnitType.Em),
                )
                if (blockedApps.isNotEmpty()) {
                    Text(
                        text      = "${blockedApps.size} blocked",
                        style     = TokiTypography.supporting,
                        color     = Color.White.copy(alpha = 0.8f),
                        modifier  = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .clickable { viewModel.clearAll() }
                            .padding(horizontal = 8.dp, vertical = 2.dp),
                    )
                }
            }
        }

        if (appsLoading) {
            item {
                Box(
                    modifier         = Modifier.fillMaxWidth().padding(40.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(
                        color = Color.White,
                        modifier = Modifier.size(28.dp),
                        strokeWidth = 2.dp,
                    )
                }
            }
        } else {
            items(installedApps, key = { it.packageName }) { app ->
                AppBlockRow(
                    app       = app,
                    isBlocked = app.packageName in blockedApps,
                    onToggle  = { viewModel.toggleApp(app.packageName) },
                )
            }
        }

        // ── Section 4: Schedule info ──────────────────────────────────────
        item {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                color  = Color.White.copy(alpha = 0.04f),
                shape  = RoundedCornerShape(12.dp),
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text       = "📅  Schedule",
                        style      = TokiTypography.cardTitle,
                        color      = Color.White.copy(alpha = 0.7f),
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text     = "Apps are only blocked during active FOCUS phases. " +
                                "Breaks are unrestricted.",
                        style    = TokiTypography.supporting,
                        color    = Color.White.copy(alpha = 0.45f),
                    )
                }
            }
        }

        item { Spacer(Modifier.height(80.dp)) }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Sub-composables
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun SectionLabel(text: String) {
    Text(
        text          = text.uppercase(),
        style         = TokiTypography.sectionLabel,
        color         = Color.White.copy(alpha = 0.5f),
        letterSpacing = androidx.compose.ui.unit.TextUnit(0.12f,
            androidx.compose.ui.unit.TextUnitType.Em),
        modifier      = Modifier.padding(start = 20.dp, top = 16.dp, bottom = 6.dp),
    )
}

@Composable
private fun PermissionCard(
    icon:        androidx.compose.ui.graphics.vector.ImageVector,
    title:       String,
    description: String,
    granted:     Boolean,
    onGrant:     () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        color  = Color(0xFF0D0D0D),
        shape  = RoundedCornerShape(12.dp),
        border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.08f)),
    ) {
        Row(
            modifier          = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                modifier = Modifier.size(40.dp),
                shape    = RoundedCornerShape(10.dp),
                color    = if (granted) Color.White.copy(alpha = 0.15f)
                           else Color.White.copy(alpha = 0.06f),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector        = icon,
                        contentDescription = null,
                        tint               = if (granted) Color.White else Color.White.copy(alpha = 0.5f),
                        modifier           = Modifier.size(20.dp),
                    )
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text       = title,
                    style      = TokiTypography.rowTitle,
                    color      = Color.White,
                )
                Text(
                    text     = description,
                    style    = TokiTypography.supporting,
                    color    = Color.White.copy(alpha = 0.45f),
                )
            }
            Spacer(Modifier.width(8.dp))
            if (granted) {
                Surface(
                    color = Color.White.copy(alpha = 0.15f),
                    shape = CircleShape,
                ) {
                    Icon(
                        imageVector        = Icons.Outlined.Check,
                        contentDescription = "Granted",
                        tint               = Color.White,
                        modifier           = Modifier
                            .padding(4.dp)
                            .size(16.dp),
                    )
                }
            } else {
                Button(
                    onClick = onGrant,
                    colors  = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor   = Color(0xFF141414),
                    ),
                    shape  = RoundedCornerShape(8.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(
                        horizontal = 12.dp, vertical = 6.dp,
                    ),
                ) {
                    Text(text = "Grant", style = TokiTypography.button)
                }
            }
        }
    }
}

@Composable
private fun PresetChip(emoji: String, label: String, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.clickable(onClick = onClick),
        color    = Color.White.copy(alpha = 0.07f),
        shape    = RoundedCornerShape(10.dp),
        border   = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.1f)),
    ) {
        Column(
            modifier            = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(text = emoji, fontSize = 20.sp)
            Spacer(Modifier.height(4.dp))
            Text(
                text       = label,
                style      = TokiTypography.action,
                color      = Color.White.copy(alpha = 0.7f),
            )
        }
    }
}

@Composable
private fun AppBlockRow(
    app:      AppInfo,
    isBlocked: Boolean,
    onToggle: () -> Unit,
) {
    Row(
        modifier          = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // App icon — convert Drawable → Bitmap → Painter if available
        val bitmapPainter = remember(app.packageName) {
            app.icon?.let {
                runCatching {
                    BitmapPainter(it.toBitmap(48, 48).asImageBitmap())
                }.getOrNull()
            }
        }

        Surface(
            modifier = Modifier.size(40.dp),
            shape    = RoundedCornerShape(10.dp),
            color    = Color.White.copy(alpha = 0.06f),
        ) {
            Box(contentAlignment = Alignment.Center) {
                if (bitmapPainter != null) {
                    Image(
                        painter            = bitmapPainter,
                        contentDescription = app.appName,
                        modifier           = Modifier.size(32.dp),
                    )
                } else {
                    Icon(
                        imageVector        = Icons.Outlined.Android,
                        contentDescription = null,
                        tint               = Color.White.copy(alpha = 0.4f),
                        modifier           = Modifier.size(22.dp),
                    )
                }
            }
        }

        Spacer(Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text       = app.appName,
                style      = TokiTypography.rowTitle,
                color      = Color.White,
                maxLines   = 1,
            )
            Text(
                text     = app.packageName,
                style    = TokiTypography.supporting,
                color    = Color.White.copy(alpha = 0.5f),
                maxLines = 1,
            )
        }

        Switch(
            checked         = isBlocked,
            onCheckedChange = { onToggle() },
            colors          = SwitchDefaults.colors(
                checkedThumbColor      = Color(0xFF141414),
                checkedTrackColor      = Color.White,
                uncheckedThumbColor    = Color.White.copy(alpha = 0.5f),
                uncheckedTrackColor    = Color.White.copy(alpha = 0.12f),
                uncheckedBorderColor   = Color.Transparent,
            ),
        )
    }

    Divider(
        modifier  = Modifier.padding(start = 68.dp),
        color     = Color.White.copy(alpha = 0.05f),
        thickness = 0.5.dp,
    )
}

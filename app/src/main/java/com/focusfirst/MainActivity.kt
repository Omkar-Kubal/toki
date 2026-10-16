package com.focusfirst

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.ListAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.focusfirst.analytics.TokiAnalytics
import com.focusfirst.billing.BillingViewModel
import com.focusfirst.billing.ProUpgradeSheet
import com.focusfirst.data.SettingsRepository
import com.focusfirst.ui.components.FirstLaunchDialog
import androidx.activity.compose.BackHandler
import com.focusfirst.ui.screens.HomeScreen
import com.focusfirst.ui.screens.LicensesScreen
import com.focusfirst.ui.screens.SettingsScreen
import com.focusfirst.ui.screens.StatsScreen
import com.focusfirst.ui.screens.TasksScreen
import com.focusfirst.ui.theme.FocusFirstTheme
import com.focusfirst.viewmodel.SettingsViewModel
import com.focusfirst.viewmodel.TaskViewModel
import com.focusfirst.viewmodel.TimerViewModel
import com.focusfirst.util.DeepLinkDuration
import com.focusfirst.util.ThemeModePolicy
import com.focusfirst.util.selectedTaskIdAfterSelectionAttempt
import com.focusfirst.R
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

// ============================================================================
// Navigation
// ============================================================================

private enum class Tab { HOME, TASKS, STATS, SETTINGS }

private data class TabItem(
    val tab:   Tab,
    val label: String,
    val icon:  ImageVector,
)

private val navTabs = listOf(
    TabItem(Tab.HOME,  "Home",     Icons.Outlined.Home),
    TabItem(Tab.TASKS, "Tasks",    Icons.Outlined.ListAlt),
    TabItem(Tab.STATS, "Progress", Icons.Outlined.BarChart),
)
private val tabOrder = listOf(Tab.HOME, Tab.TASKS, Tab.STATS, Tab.SETTINGS)

// ============================================================================
// MainActivity
// ============================================================================

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var settingsRepository: SettingsRepository

    /** Provides access to TimerViewModel for deep link dispatch before Compose is ready. */
    private val timerViewModelByDelegate: com.focusfirst.viewmodel.TimerViewModel by viewModels()
    private val taskViewModelByDelegate: TaskViewModel by viewModels()

    private val showNotificationRationale = mutableStateOf(false)

    private val requestNotificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (!granted) {
                showNotificationRationale.value = true
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, false)
        super.onCreate(savedInstanceState)

        lifecycleScope.launch {
            timerViewModelByDelegate.focusSessionCompleted.collect {
                taskViewModelByDelegate.selectedTaskId?.let(taskViewModelByDelegate::incrementPomodoro)
            }
        }

        requestPostNotificationsPermissionIfNeeded()

        // Handle deep link if activity was cold-launched via URI
        intent?.let { handleDeepLink(it) }

        setContent {
            val settingsViewModel: SettingsViewModel = hiltViewModel()
            val billingViewModel: BillingViewModel = hiltViewModel()
            val amoledMode by settingsViewModel.amoledMode.collectAsStateWithLifecycle()
            val themeMode by settingsViewModel.themeMode.collectAsStateWithLifecycle()
            val darkTheme = ThemeModePolicy.isDark(themeMode)

            SideEffect {
                window.navigationBarColor = android.graphics.Color.TRANSPARENT
                window.statusBarColor = android.graphics.Color.TRANSPARENT
                WindowInsetsControllerCompat(window, window.decorView).apply {
                    isAppearanceLightNavigationBars = !darkTheme
                    isAppearanceLightStatusBars = !darkTheme
                }
            }

            var selectedTab by rememberSaveable { mutableStateOf(Tab.HOME) }

            // Log each tab switch to Firebase Analytics
            LaunchedEffect(selectedTab) {
                TokiAnalytics.logTabViewed(selectedTab.name)
            }

            val eulaAccepted by settingsViewModel.eulaAccepted
                .collectAsStateWithLifecycle(false)

            FocusFirstTheme(amoledMode = amoledMode, darkTheme = darkTheme) {
                FocusFirstAppContent(
                    showNotificationRationale = showNotificationRationale,
                    selectedTab               = selectedTab,
                    onTabSelected             = { selectedTab = it },
                )

                // Global Pro upgrade sheet — triggered from anywhere in the app
                val showUpgradeSheet by billingViewModel.showUpgradeSheet
                    .collectAsStateWithLifecycle()
                if (showUpgradeSheet) {
                    ProUpgradeSheet(
                        onDismiss        = { billingViewModel.dismissUpgradeSheet() },
                        billingViewModel = billingViewModel,
                    )
                }

                // EULA — shown once on first launch, non-dismissible
                if (!eulaAccepted) {
                    FirstLaunchDialog(
                        onAccept = {
                            settingsViewModel.acceptEula()
                        },
                        onLearnMore = {
                            val intent = Intent(
                                Intent.ACTION_VIEW,
                                Uri.parse(getString(R.string.privacy_policy_url)),
                            )
                            startActivity(intent)
                        },
                    )
                }
            }
        }
    }

    private fun requestPostNotificationsPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return

        val alreadyGranted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED
        if (alreadyGranted) return

        lifecycleScope.launch {
            val alreadyAsked = settingsRepository.notificationPermissionAsked.first()
            if (!alreadyAsked) {
                settingsRepository.update(
                    SettingsRepository.KEY_NOTIFICATION_PERMISSION_ASKED,
                    true,
                )
                requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    // ── Deep link handling ────────────────────────────────────────────────────

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleDeepLink(intent)
    }

    /**
     * Dispatch incoming toki:// URIs to the TimerViewModel.
     *
     * Supported URIs:
     *   toki://timer/start              — start 25-min session
     *   toki://timer/start?duration=15  — start custom duration
     *   toki://timer/start?task=Physics — start with task label
     *   toki://timer/stop               — stop current session
     *   toki://timer/cancel             — discard current session
     *   toki://timer/pause              — pause
     *   toki://timer/resume             — resume
     */
    private fun handleDeepLink(intent: Intent) {
        val uri = intent.data ?: return
        if (uri.scheme != "toki") return

        Log.d("MainActivity", "handleDeepLink: $uri")

        when (uri.host) {
            "timer" -> {
                when (uri.path) {
                    "/start" -> {
                        val duration = DeepLinkDuration.parse(uri.getQueryParameter("duration"))
                        if (duration == null) {
                            Log.w("MainActivity", "Ignoring invalid timer duration: ${uri.getQueryParameter("duration")}")
                            return
                        }

                        if (isTimerSessionActive()) return

                        val taskId = uri.getQueryParameter("taskId")
                        val taskName = uri.getQueryParameter("task")
                        lifecycleScope.launch {
                            val selectedTaskId = taskViewModelByDelegate
                                .resolveDeepLinkTaskId(taskId, taskName)
                            val timerIsActive = isTimerSessionActive()
                            taskViewModelByDelegate.selectedTaskId = selectedTaskIdAfterSelectionAttempt(
                                currentTaskId = taskViewModelByDelegate.selectedTaskId,
                                requestedTaskId = selectedTaskId,
                                timerIsActive = timerIsActive,
                            )
                            if (timerIsActive) return@launch

                            timerViewModelByDelegate.startFromDeepLink(duration)
                        }
                    }
                    "/stop"   -> timerViewModelByDelegate.stop()
                    "/cancel" -> timerViewModelByDelegate.cancel()
                    "/pause"  -> timerViewModelByDelegate.pause()
                    "/resume" -> timerViewModelByDelegate.resume()
                    else -> Log.w("MainActivity", "Unknown deep link path: ${uri.path}")
                }
            }
            else -> Log.w("MainActivity", "Unknown deep link host: ${uri.host}")
        }
    }

    private fun isTimerSessionActive(): Boolean =
        timerViewModelByDelegate.timerState.value.let { it.isRunning || it.isPaused }
}

// ============================================================================
// App shell — bottom navigation + snackbar host
// ============================================================================

@Composable
private fun FocusFirstAppContent(
    showNotificationRationale: State<Boolean>,
    selectedTab:               Tab,
    onTabSelected:             (Tab) -> Unit,
) {
    val snackbarHostState  = remember { SnackbarHostState() }
    val context            = LocalContext.current
    val shouldShowRationale by showNotificationRationale
    var showLicenses by rememberSaveable { mutableStateOf(false) }

    BackHandler(enabled = selectedTab == Tab.SETTINGS && !showLicenses) {
        onTabSelected(Tab.HOME)
    }
    BackHandler(enabled = selectedTab == Tab.TASKS || selectedTab == Tab.STATS) {
        onTabSelected(Tab.HOME)
    }

    LaunchedEffect(shouldShowRationale) {
        if (shouldShowRationale) {
            val result = snackbarHostState.showSnackbar(
                message     = "Notifications needed for timer alerts",
                actionLabel = "Enable",
                duration    = SnackbarDuration.Long,
            )
            if (result == SnackbarResult.ActionPerformed) {
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data  = Uri.fromParts("package", context.packageName, null)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
            }
        }
    }

    val scheme = MaterialTheme.colorScheme

    Scaffold(
        containerColor = scheme.background,
        snackbarHost   = {
            SnackbarHost(snackbarHostState) { data ->
                Snackbar(
                    snackbarData   = data,
                    containerColor = scheme.surfaceContainerHigh,
                    contentColor   = scheme.onSurface,
                    actionColor    = scheme.primary,
                )
            }
        },
        bottomBar = {
            FocusBottomNav(
                selectedTab   = if (selectedTab == Tab.SETTINGS) Tab.STATS else selectedTab,
                onTabSelected = onTabSelected,
            )
        },
    ) { scaffoldPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(scheme.background)
                .padding(scaffoldPadding),
        ) {
            AnimatedContent(
                    targetState   = selectedTab,
                    transitionSpec = {
                        val from = tabOrder.indexOf(initialState)
                        val to   = tabOrder.indexOf(targetState)
                        val dir  = if (to > from) 1 else -1
                        (slideInHorizontally(tween(200)) { it / 3 * dir } + fadeIn(tween(200)))
                            .togetherWith(
                                slideOutHorizontally(tween(200)) { -it / 3 * dir } + fadeOut(tween(150))
                            )
                    },
                    label = "tab_slide",
                    modifier = Modifier.fillMaxSize(),
                ) { tab ->
                    when (tab) {
                        Tab.HOME -> HomeScreen(
                            onNavigateToSettings = { onTabSelected(Tab.SETTINGS) },
                            onNavigateToTasks = { onTabSelected(Tab.TASKS) },
                        )
                        Tab.TASKS -> TasksScreen(
                            onNavigateToHome = { onTabSelected(Tab.HOME) },
                            onNavigateToSettings = { onTabSelected(Tab.SETTINGS) },
                        )
                        Tab.STATS -> StatsScreen()
                        Tab.SETTINGS -> if (showLicenses) {
                            BackHandler { showLicenses = false }
                            LicensesScreen(onBack = { showLicenses = false })
                        } else {
                            SettingsScreen(
                                onNavigateToLicenses = { showLicenses = true },
                            )
                        }
                    }
                }
        }
    }
}

// ============================================================================
// Bottom navigation bar
// ============================================================================

/**
 * Minimal three-destination navigation from the selected Home reference.
 */
@Composable
private fun FocusBottomNav(
    selectedTab:   Tab,
    onTabSelected: (Tab) -> Unit,
) {
    val cs = MaterialTheme.colorScheme

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(cs.background)
            .navigationBarsPadding()
            .padding(horizontal = 20.dp)
            .padding(bottom = 8.dp, top = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(50.dp))
                .background(cs.surfaceContainerLow)
                .border(1.dp, cs.outline, RoundedCornerShape(50.dp))
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            navTabs.forEach { item ->
                val isSelected        = selectedTab == item.tab
                val interactionSource = remember { MutableInteractionSource() }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .defaultMinSize(minHeight = 52.dp)
                        .clip(RoundedCornerShape(32.dp))
                        .indication(interactionSource, ripple(bounded = true))
                        .clickable(
                            interactionSource = interactionSource,
                            indication        = null,
                        ) { onTabSelected(item.tab) }
                        .semantics(mergeDescendants = true) {
                            contentDescription = item.label
                            role     = Role.Tab
                            selected = isSelected
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(50.dp))
                            .padding(vertical = 5.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Icon(
                            imageVector        = if (item.tab == Tab.HOME && isSelected) {
                                Icons.Filled.Home
                            } else item.icon,
                            contentDescription = null,
                            modifier           = Modifier.size(24.dp),
                            tint               = if (isSelected) cs.onBackground
                                                 else            cs.onSurfaceVariant,
                        )
                        Text(
                            text = item.label,
                            style = MaterialTheme.typography.labelMedium,
                            color = if (isSelected) cs.onBackground else cs.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            maxLines = 2,
                        )
                    }
                }
            }
        }
    }
}

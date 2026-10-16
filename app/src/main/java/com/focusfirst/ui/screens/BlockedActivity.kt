package com.focusfirst.ui.screens

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.preference.PreferenceManager
import com.focusfirst.data.repository.FocusGuardRepository
import com.focusfirst.ui.theme.TokiTypography
import com.focusfirst.ui.theme.FocusFirstTheme
import kotlinx.coroutines.delay

/**
 * BlockedActivity
 *
 * Full-screen overlay shown when the user tries to open a blocked app
 * during an active Focus Guard session.
 *
 * Displayed as a dark interstitial with:
 *  • Pulsing lock icon
 *  • Session-remaining countdown
 *  • Name of the app that was blocked
 *  • "Panic escape" button that immediately ends the guard session
 *
 * This is a plain ComponentActivity (not part of the Compose nav graph)
 * because it must be launchable from the AccessibilityService via
 * FLAG_ACTIVITY_NEW_TASK without awareness of the current nav back stack.
 */
class BlockedActivity : ComponentActivity() {

    companion object {
        /** Package name of the app that triggered the block. */
        const val EXTRA_BLOCKED_PACKAGE = "blocked_package"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        val blockedPackage = intent.getStringExtra(EXTRA_BLOCKED_PACKAGE) ?: ""
        val blockedAppName = resolveAppName(blockedPackage)
        val remainingSeconds = readRemainingSeconds()

        setContent {
            FocusFirstTheme(amoledMode = false) {
                BlockedScreen(
                    blockedAppName   = blockedAppName,
                    initialRemaining = remainingSeconds,
                    onPanicEscape    = {
                        deactivateFocusGuard()
                        finish()
                    },
                )
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────

    private fun resolveAppName(packageName: String): String {
        if (packageName.isBlank()) return "App"
        return runCatching {
            val info = packageManager.getApplicationInfo(packageName, 0)
            packageManager.getApplicationLabel(info).toString()
        }.getOrDefault(packageName)
    }

    private fun readRemainingSeconds(): Int {
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        return prefs.getInt(FocusGuardRepository.PREF_REMAINING_SECONDS, 0)
    }

    private fun deactivateFocusGuard() {
        PreferenceManager.getDefaultSharedPreferences(this)
            .edit()
            .putBoolean(FocusGuardRepository.PREF_FOCUS_GUARD_ACTIVE, false)
            .apply()
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Compose UI
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun BlockedScreen(
    blockedAppName:   String,
    initialRemaining: Int,
    onPanicEscape:    () -> Unit,
) {
    var remaining by remember { mutableIntStateOf(initialRemaining) }

    // Count down the remaining time locally (good enough UX; service drives
    // the real source of truth)
    LaunchedEffect(Unit) {
        while (remaining > 0) {
            delay(1_000)
            remaining--
        }
    }

    // Pulsing animation for the lock icon
    val infiniteTransition = rememberInfiniteTransition(label = "lock_pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue  = 1.08f,
        animationSpec = infiniteRepeatable(
            animation  = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "lock_scale",
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(Color(0xFF141414), Color(0xFF000000)),
                )
            ),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(32.dp),
        ) {

            // ── Lock icon ─────────────────────────────────────────────────
            Surface(
                modifier  = Modifier
                    .size(96.dp)
                    .scale(scale),
                shape     = CircleShape,
                color     = Color(0xFF262626),
                tonalElevation = 0.dp,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(text = "🔒", fontSize = 40.sp)
                }
            }

            Spacer(Modifier.height(32.dp))

            // ── Headline ──────────────────────────────────────────────────
            Text(
                text       = "Focus session in progress",
                style      = TokiTypography.pageTitle,
                color      = Color.White,
                textAlign  = TextAlign.Center,
            )

            Spacer(Modifier.height(8.dp))

            // ── Blocked app name ──────────────────────────────────────────
            Text(
                text      = "$blockedAppName is blocked",
                style     = TokiTypography.rowTitle,
                color     = Color.White.copy(alpha = 0.55f),
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(32.dp))

            // ── Countdown ─────────────────────────────────────────────────
            if (remaining > 0) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFF262626),
                ) {
                    Column(
                        modifier            = Modifier.padding(horizontal = 28.dp, vertical = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        val hrs  = remaining / 3600
                        val mins = (remaining % 3600) / 60
                        val secs = remaining % 60
                        val timeStr = if (hrs > 0) {
                            "%d:%02d:%02d".format(hrs, mins, secs)
                        } else {
                            "%d:%02d".format(mins, secs)
                        }
                        Text(
                            text       = timeStr,
                            style      = TokiTypography.heroMetric.copy(
                                fontWeight = FontWeight.Bold,
                            ),
                            color      = Color.White,
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text     = "remaining",
                            style    = TokiTypography.supporting,
                            color    = Color.White.copy(alpha = 0.4f),
                        )
                    }
                }
            }

            Spacer(Modifier.height(48.dp))

            // ── Panic escape ──────────────────────────────────────────────
            Button(
                onClick = onPanicEscape,
                colors  = ButtonDefaults.buttonColors(
                    containerColor = Color.White.copy(alpha = 0.08f),
                    contentColor   = Color.White.copy(alpha = 0.5f),
                ),
                shape = RoundedCornerShape(12.dp),
            ) {
                Text(
                    text       = "⚠  End session & return",
                    style      = TokiTypography.button,
                    modifier   = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                )
            }

            Spacer(Modifier.height(12.dp))

            Text(
                text      = "Stay focused — you're almost there!",
                style     = TokiTypography.supporting,
                color     = Color.White.copy(alpha = 0.25f),
                textAlign = TextAlign.Center,
            )
        }
    }
}

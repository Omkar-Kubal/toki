package com.focusfirst.billing

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.focusfirst.ui.theme.TokiTypography

// ─── ProGate ────────────────────────────────────────────────────────────────────

/**
 * Wraps [content] and either renders it normally (when the user is Pro) or
 * overlays a lock screen prompting an upgrade (when not Pro).
 *
 * [lockedContent] is an optional replacement for the default lock overlay.
 * Pass null (default) to use the built-in lock overlay.
 */
@Composable
fun ProGate(
    billingViewModel: BillingViewModel = hiltViewModel(),
    lockedContent: (@Composable () -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    val isPro by billingViewModel.isPro.collectAsStateWithLifecycle()
    val proPrice by billingViewModel.proPrice.collectAsStateWithLifecycle()

    if (isPro) {
        content()
    } else {
        Box {
            // Always render the blurred/dimmed feature content underneath
            content()

            // Lock overlay
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(Color.Black.copy(alpha = 0.72f)),
                contentAlignment = Alignment.Center,
            ) {
                if (lockedContent != null) {
                    lockedContent()
                } else {
                    LockOverlayContent(
                        proPrice = proPrice,
                        onUnlockClick = { billingViewModel.openUpgradeSheet() },
                    )
                }
            }
        }
    }
}

@Composable
private fun LockOverlayContent(
    proPrice: String?,
    onUnlockClick: () -> Unit,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(
            imageVector        = Icons.Outlined.Lock,
            contentDescription = null,
            tint               = Color.White.copy(alpha = 0.9f),
            modifier           = Modifier.padding(bottom = 12.dp),
        )
        Text(
            text       = "Pro feature",
            style      = TokiTypography.cardTitle,
            color      = Color.White,
        )
        Spacer(Modifier.height(16.dp))
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(Color.White)
                .clickable(onClick = onUnlockClick)
                .padding(horizontal = 24.dp, vertical = 10.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text       = proPrice?.let { "Unlock $it / month" } ?: "Unlock Pro",
                style      = TokiTypography.button,
                color      = Color.Black,
            )
        }
    }
}

// ─── ProBadge ───────────────────────────────────────────────────────────────────

/**
 * Small amber "PRO" pill badge.
 * Place next to setting labels that require a Pro subscription.
 *
 * Usage:
 * ```
 * Row(verticalAlignment = Alignment.CenterVertically) {
 *     Text("AMOLED black")
 *     Spacer(Modifier.width(6.dp))
 *     ProBadge()
 * }
 * ```
 */
@Composable
fun ProBadge() {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(Color(0xFFFFC107).copy(alpha = 0.18f))
            .padding(horizontal = 6.dp, vertical = 2.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text          = "PRO",
            style         = TokiTypography.sectionLabel,
            letterSpacing = 0.8.sp,
            color         = Color(0xFFFFC107),
        )
    }
}

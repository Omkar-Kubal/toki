package com.focusfirst.billing

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.focusfirst.analytics.TokiAnalytics
import com.focusfirst.ui.theme.TokiTypography
import kotlinx.coroutines.delay

private val SheetBackground = Color(0xFF121212)

private val proFeatures = listOf(
    "Ambient focus sounds",
    "Focus Guard app blocking",
    "Cloud sync and restore",
    "CSV session export",
    "Expanded insights and history",
    "AMOLED pure black mode",
    "All future Pro features",
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProUpgradeSheet(
    onDismiss:        () -> Unit,
    billingViewModel: BillingViewModel = hiltViewModel(),
) {
    val activity      = LocalContext.current as? Activity
    val isPro         by billingViewModel.isPro.collectAsStateWithLifecycle()
    val purchaseError by billingViewModel.purchaseError.collectAsStateWithLifecycle()
    val proPrice      by billingViewModel.proPrice.collectAsStateWithLifecycle()
    val productReady  by billingViewModel.isProductReady.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    var isLoading by remember { mutableStateOf(false) }

    // Reset spinner and auto-dismiss 2 s after purchase succeeds
    LaunchedEffect(isPro) {
        if (isPro) {
            isLoading = false
            delay(2_000)
            onDismiss()
        }
    }

    // Reset loading spinner if billing client goes unavailable
    LaunchedEffect(billingViewModel.billingState) {
        billingViewModel.billingState.collect { state ->
            if (state == BillingState.UNAVAILABLE) isLoading = false
        }
    }

    // Log screen view and reset spinner on billing cancellation
    LaunchedEffect(Unit) {
        TokiAnalytics.logUpgradeScreenViewed()
        billingViewModel.billingCancelled.collect { isLoading = false }
    }

    // Show error snackbar when a purchase launch fails
    LaunchedEffect(purchaseError) {
        val error = purchaseError ?: return@LaunchedEffect
        isLoading = false
        snackbarHostState.showSnackbar(error)
        billingViewModel.clearPurchaseError()
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState       = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor   = SheetBackground,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 12.dp, bottom = 4.dp)
                    .size(width = 40.dp, height = 4.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.25f)),
            )
        },
    ) {
        Box(Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                if (isPro) {
                    PurchasedContent(onDismiss = onDismiss)
                } else {
                    UpgradeContent(
                        isLoading      = isLoading,
                        proPrice       = proPrice,
                        productReady   = productReady,
                        onBuyClick     = {
                            if (activity != null && productReady && !isLoading) {
                                isLoading = true
                                billingViewModel.launchPurchase(activity)
                            }
                        },
                        onRestoreClick = { billingViewModel.restorePurchases() },
                        onDismissClick = onDismiss,
                    )
                }
            }

            SnackbarHost(
                hostState = snackbarHostState,
                modifier  = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 8.dp),
            ) { data ->
                Snackbar(
                    snackbarData   = data,
                    containerColor = Color(0xFF333333),
                    contentColor   = Color.White,
                )
            }
        }
    }
}

// ─── Purchased state ─────────────────────────────────────────────────────────

@Composable
private fun PurchasedContent(onDismiss: () -> Unit) {
    Spacer(Modifier.height(32.dp))
    Icon(
        imageVector        = Icons.Outlined.CheckCircle,
        contentDescription = null,
        tint               = Color(0xFF34C759),
        modifier           = Modifier.size(72.dp),
    )
    Spacer(Modifier.height(20.dp))
    Text(
        text       = "You're Pro!",
        style      = TokiTypography.cardTitle,
        color      = Color.White,
    )
    Spacer(Modifier.height(8.dp))
    Text(
        text     = "All Pro features are now unlocked.",
        style    = TokiTypography.supporting,
        color    = Color.White.copy(alpha = 0.65f),
    )
    Spacer(Modifier.height(24.dp))
    Button(
        onClick  = onDismiss,
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp),
        shape  = CircleShape,
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.White,
            contentColor   = Color.Black,
        ),
    ) {
        Text(
            text       = "Start focusing",
            style      = TokiTypography.button,
        )
    }
    Spacer(Modifier.height(24.dp))
}

// ─── Upgrade (pre-purchase) state ────────────────────────────────────────────

@Composable
private fun UpgradeContent(
    isLoading:      Boolean,
    proPrice:       String?,
    productReady:   Boolean,
    onBuyClick:     () -> Unit,
    onRestoreClick: () -> Unit,
    onDismissClick: () -> Unit,
) {
    Spacer(Modifier.height(8.dp))

    Text(
        text       = "Toki Pro",
        style      = TokiTypography.cardTitle,
        color      = Color.White,
    )
    Spacer(Modifier.height(4.dp))
    Text(
        text     = "Monthly subscription. Cancel anytime in Google Play.",
        style    = TokiTypography.supporting,
        color    = Color.White.copy(alpha = 0.5f),
    )

    Spacer(Modifier.height(20.dp))

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.1f))
            .padding(horizontal = 28.dp, vertical = 12.dp),
    ) {
        Text(
            text       = proPrice ?: "Loading...",
            style      = TokiTypography.amountInput,
            color      = Color.White,
        )
    }

    Spacer(Modifier.height(28.dp))

    Column(
        modifier            = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        proFeatures.forEach { feature ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector        = Icons.Outlined.CheckCircle,
                    contentDescription = null,
                    tint               = Color(0xFF34C759),
                    modifier           = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    text     = feature,
                    style    = TokiTypography.rowTitle,
                    color    = Color.White.copy(alpha = 0.85f),
                )
            }
        }
    }

    Spacer(Modifier.height(32.dp))

    Button(
        onClick  = onBuyClick,
        enabled  = productReady && !isLoading,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        shape  = CircleShape,
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.White,
            contentColor   = Color.Black,
        ),
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier    = Modifier.size(22.dp),
                color       = Color.Black,
                strokeWidth = 2.5.dp,
            )
        } else {
            Text(
                text       = proPrice?.let { "Start Pro - $it / month" } ?: "Loading price...",
                style      = TokiTypography.button.copy(fontSize = 16.sp),
            )
        }
    }

    Spacer(Modifier.height(4.dp))

    TextButton(
        onClick  = onRestoreClick,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            text     = "Restore purchase",
            style    = TokiTypography.action,
            color    = Color.White.copy(alpha = 0.5f),
        )
    }

    TextButton(
        onClick  = onDismissClick,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            text     = "Maybe later",
            style    = TokiTypography.action,
            color    = Color.White.copy(alpha = 0.35f),
        )
    }
}

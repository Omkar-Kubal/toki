package com.focusfirst.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.focusfirst.ui.theme.TokiTypography

@Composable
fun FirstLaunchDialog(
    onAccept:    () -> Unit,
    onLearnMore: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = { /* non-dismissible */ },
        containerColor   = Color(0xFF111111),
        title = {
            Text(
                text       = "Welcome to Toki",
                style      = TokiTypography.cardTitle,
                color      = Color.White,
            )
        },
        text = {
            Column {
                Text(
                    text     = "Toki helps you start focus sessions quickly and keep your history on this device.",
                    style    = TokiTypography.rowTitle,
                    color    = Color.White.copy(alpha = 0.8f),
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text     = "By continuing, you agree to the Terms of Service and Privacy Policy.",
                    style    = TokiTypography.supporting,
                    color    = Color.White.copy(alpha = 0.5f),
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onAccept,
                colors  = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    contentColor   = Color.Black,
                ),
            ) {
                Text("Continue", style = TokiTypography.button)
            }
        },
        dismissButton = {
            TextButton(onClick = onLearnMore) {
                Text(
                    text  = "Privacy Policy",
                    color = Color.White,
                    style = TokiTypography.action,
                )
            }
        },
    )
}

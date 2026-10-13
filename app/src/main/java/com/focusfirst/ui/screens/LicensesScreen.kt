package com.focusfirst.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.size
import com.focusfirst.ui.theme.TokiTypography

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LicensesScreen(onBack: () -> Unit) {
    val licenses = listOf(
        "Jetpack Compose"    to "Apache License 2.0",
        "Room Database"      to "Apache License 2.0",
        "Hilt"               to "Apache License 2.0",
        "WorkManager"        to "Apache License 2.0",
        "Kotlin"             to "Apache License 2.0",
        "Coroutines"         to "Apache License 2.0",
        "DataStore"          to "Apache License 2.0",
        "Navigation Compose" to "Apache License 2.0",
        "Google Play Billing" to "Google APIs Terms",
        "Material3"          to "Apache License 2.0",
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title           = { Text("Open Source Licenses") },
                navigationIcon  = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            modifier = Modifier.size(24.dp)
                        )
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
        ) {
            items(licenses) { (name, license) ->
                Row(
                    modifier              = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text     = name,
                        style    = TokiTypography.rowTitle,
                        color    = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text     = license,
                        style    = TokiTypography.supporting,
                        color    = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    )
                }
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                )
            }
        }
    }
}

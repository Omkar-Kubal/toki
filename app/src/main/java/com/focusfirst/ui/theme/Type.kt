package com.focusfirst.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.focusfirst.R

// The bundled Google Sans files are licensed under the SIL OFL. The full notice
// is packaged at assets/licenses/google_sans_ofl.txt.
val AppFontFamily = FontFamily(
    Font(R.font.google_sans_regular, FontWeight.Normal),
    Font(R.font.google_sans_medium, FontWeight.Medium),
    Font(R.font.google_sans_semibold, FontWeight.SemiBold),
    Font(R.font.google_sans_bold, FontWeight.Bold),
)

/** Semantic roles from android-compose-agent-kit/design-system/typography-registry.yaml. */
object TokiTypography {
    val pageTitle = TextStyle(fontFamily = AppFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 22.sp, lineHeight = 28.sp)
    val cardTitle = TextStyle(fontFamily = AppFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 22.sp)
    val sectionLabel = TextStyle(fontFamily = AppFontFamily, fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 16.sp)
    val bodyTitle = TextStyle(fontFamily = AppFontFamily, fontWeight = FontWeight.Medium, fontSize = 16.sp, lineHeight = 22.sp)
    val rowTitle = TextStyle(fontFamily = AppFontFamily, fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 20.sp)
    val supporting = TextStyle(fontFamily = AppFontFamily, fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 16.sp)
    val button = TextStyle(fontFamily = AppFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 20.sp)
    val action = TextStyle(fontFamily = AppFontFamily, fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 20.sp)
    val heroMetric = TextStyle(fontFamily = AppFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 32.sp, lineHeight = 38.sp)
    val timerHeroMetric = TextStyle(fontFamily = AppFontFamily, fontWeight = FontWeight.Bold, fontSize = 48.sp, lineHeight = 56.sp)
    val amountInput = TextStyle(fontFamily = AppFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 28.sp, lineHeight = 34.sp)
    val inlineAmount = TextStyle(fontFamily = AppFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 20.sp)
    val chartLabel = TextStyle(fontFamily = AppFontFamily, fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 16.sp)
}

/** Material 3 slots mapped to the app's semantic registry roles. */
val Typography = Typography(
    displayLarge = TokiTypography.heroMetric,
    displayMedium = TokiTypography.amountInput,
    displaySmall = TokiTypography.pageTitle,
    headlineLarge = TokiTypography.heroMetric,
    headlineMedium = TokiTypography.amountInput,
    headlineSmall = TokiTypography.pageTitle,
    titleLarge = TokiTypography.pageTitle,
    titleMedium = TokiTypography.cardTitle,
    titleSmall = TokiTypography.rowTitle,
    bodyLarge = TokiTypography.bodyTitle,
    bodyMedium = TokiTypography.rowTitle,
    bodySmall = TokiTypography.supporting,
    labelLarge = TokiTypography.button,
    labelMedium = TokiTypography.action,
    labelSmall = TokiTypography.sectionLabel,
)

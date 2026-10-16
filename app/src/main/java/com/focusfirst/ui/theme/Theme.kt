package com.focusfirst.ui.theme

import android.os.Build
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.focusfirst.data.model.TimerPhase

// ============================================================================
// Mobbin-inspired neutrals adapted to Toki's dark Home.
// ============================================================================

private val DarkBackground             = Color(0xFF141414)
private val DarkSurface                = Color(0xFF262626)
private val DarkSurfaceHigh            = Color(0xFF303030)
private val DarkSurfaceVariant         = Color(0xFF3A3A3A)
private val DarkPrimary                = Color(0xFFFFFFFF)
private val DarkOnPrimary              = Color(0xFF141414)
private val DarkOnSurface              = Color(0xFFFFFFFF)
private val DarkOnSurfaceVariant       = Color(0xFFADADAD)
private val DarkOutline                = Color(0xFF3A3A3A)
private val DarkOutlineVariant         = Color(0xFF303030)
private val DarkSurfaceContainerLowest = Color(0xFF141414)
private val DarkSurfaceContainer       = Color(0xFF2B2B2B)
private val DarkSurfaceContainerHighest= Color(0xFF3A3A3A)

private val LightBackground             = Color(0xFFFFFFFF)
private val LightSurface                = Color(0xFFFFFFFF)
private val LightSurfaceHigh            = Color(0xFFF3F3F3)
private val LightSurfaceVariant         = Color(0xFFF0F0F0)
private val LightPrimary                = Color(0xFF141414)
private val LightOnPrimary              = Color(0xFFFFFFFF)
private val LightOnSurface              = Color(0xFF141414)
private val LightOnSurfaceVariant       = Color(0xFF707070)
private val LightOutline                = Color(0xFFE0E0E0)

// ============================================================================
// Shared commercial accent. Routine timer and task states use theme neutrals.
// ============================================================================

object FocusColors {
    val CommercialBlue    = Color(0xFF0066FF)
    val ProCardBackground = Color(0xFF262626)
}

// ============================================================================
// Static color schemes (B&W brand palette — fallback for API < 31)
// ============================================================================

private val DarkColorScheme = darkColorScheme(
    primary                     = DarkPrimary,
    onPrimary                   = DarkOnPrimary,
    primaryContainer            = DarkPrimary,
    onPrimaryContainer          = DarkOnPrimary,
    secondary                   = DarkOnSurfaceVariant,
    onSecondary                 = DarkOnPrimary,
    secondaryContainer          = DarkSurfaceVariant,
    onSecondaryContainer        = DarkOnSurface,
    tertiary                    = DarkOnSurface,
    onTertiary                  = DarkOnPrimary,
    tertiaryContainer           = DarkSurfaceVariant,
    onTertiaryContainer         = DarkOnSurface,
    background                  = DarkBackground,
    onBackground                = DarkOnSurface,
    surface                     = DarkSurface,
    onSurface                   = DarkOnSurface,
    surfaceContainerLowest      = DarkSurfaceContainerLowest,
    surfaceContainerLow         = DarkSurface,
    surfaceContainer            = DarkSurfaceContainer,
    surfaceContainerHigh        = DarkSurfaceHigh,
    surfaceContainerHighest     = DarkSurfaceContainerHighest,
    surfaceVariant              = DarkSurfaceVariant,
    onSurfaceVariant            = DarkOnSurfaceVariant,
    outline                     = DarkOutline,
    outlineVariant              = DarkOutlineVariant,
)

private val DarkAmoledColorScheme = DarkColorScheme.copy(
    background                  = Color.Black,
    surface                     = Color.Black,
    surfaceContainerLowest      = Color.Black,
    surfaceContainerLow         = Color.Black,
    surfaceContainer            = Color(0xFF080808),
    surfaceContainerHigh        = Color(0xFF111111), // slight lift for usability
    surfaceContainerHighest     = Color(0xFF161616),
)

private val LightColorScheme = lightColorScheme(
    primary                     = LightPrimary,
    onPrimary                   = LightOnPrimary,
    primaryContainer            = LightPrimary,
    onPrimaryContainer          = LightOnPrimary,
    secondary                   = LightOnSurfaceVariant,
    onSecondary                 = LightOnPrimary,
    secondaryContainer          = LightSurfaceVariant,
    onSecondaryContainer        = LightOnSurface,
    tertiary                    = LightOnSurface,
    onTertiary                  = LightOnPrimary,
    tertiaryContainer           = LightSurfaceVariant,
    onTertiaryContainer         = LightOnSurface,
    background                  = LightBackground,
    onBackground                = LightOnSurface,
    surface                     = LightSurface,
    onSurface                   = LightOnSurface,
    surfaceContainerLowest      = Color.White,
    surfaceContainerLow         = LightSurface,
    surfaceContainer            = LightSurfaceHigh,
    surfaceContainerHigh        = LightSurfaceHigh,
    surfaceContainerHighest     = Color(0xFFEDEDED),
    surfaceVariant              = LightSurfaceVariant,
    onSurfaceVariant            = LightOnSurfaceVariant,
    outline                     = LightOutline,
    outlineVariant              = LightOutline.copy(alpha = 0.65f),
)

// ============================================================================
// CompositionLocal — whether the app shell is using the dark scheme
// ============================================================================

val LocalFocusDarkTheme = staticCompositionLocalOf { true }

// ============================================================================
// Theme composable
// ============================================================================

/**
 * Root Material 3 theme for FocusFirst (Toki).
 *
 * Dark is the default, while the existing Light setting uses Mobbin's white
 * canvas and ink palette. Dynamic color remains opt-in on API 31+.
 *
 * @param amoledMode   When true, forces pure-black surfaces for OLED screens.
 * @param dynamicColor Set true to use wallpaper-extracted colors on API 31+.
 *                     Defaults to false for the brand monochrome experience.
 */
@Composable
fun FocusFirstTheme(
    amoledMode:   Boolean = false,
    dynamicColor: Boolean = false,
    darkTheme:    Boolean = true,
    content:      @Composable () -> Unit,
) {
    val context = LocalContext.current

    val scheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        !darkTheme -> LightColorScheme
        amoledMode -> DarkAmoledColorScheme
        else       -> DarkColorScheme
    }

    CompositionLocalProvider(LocalFocusDarkTheme provides darkTheme) {
        MaterialTheme(
            colorScheme = scheme,
            typography  = Typography,
            content     = content,
        )
    }
}

// ============================================================================
// Phase ring color — follows active scheme
// ============================================================================

/**
 * Phase contrast is neutral in both themes; labels also identify each phase.
 */
@Composable
fun TimerPhase.ringColor(): Color = MaterialTheme.colorScheme.primary

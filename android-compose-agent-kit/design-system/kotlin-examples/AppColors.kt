// EXAMPLE semantic plumbing; palette must be reviewed before use.
package example.designsystem

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

@Immutable
data class AppColors(
    val surfaceCanvas: Color, val surfaceCard: Color,
    val textPrimary: Color, val textSecondary: Color,
    val borderSubtle: Color, val actionPrimary: Color,
    val onActionPrimary: Color, val statusError: Color
)

val LightAppColors = AppColors(
    surfaceCanvas=Color(0xFFF8F9FA), surfaceCard=Color.White,
    textPrimary=Color(0xFF16181B), textSecondary=Color(0xFF343A40),
    borderSubtle=Color(0xFFE9ECEF), actionPrimary=Color(0xFF4F46E5),
    onActionPrimary=Color.White, statusError=Color(0xFFB91C1C)
)
val DarkAppColors = AppColors(
    surfaceCanvas=Color(0xFF101215), surfaceCard=Color(0xFF16181B),
    textPrimary=Color.White, textSecondary=Color(0xFFE9ECEF),
    borderSubtle=Color(0xFF343A40), actionPrimary=Color(0xFFA5B4FC),
    onActionPrimary=Color(0xFF101215), statusError=Color(0xFFB91C1C)
)
val LocalAppColors = staticCompositionLocalOf { LightAppColors }

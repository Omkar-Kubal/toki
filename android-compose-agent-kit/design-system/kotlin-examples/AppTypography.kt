// EXAMPLE: Change package to match project. Replace fallback with legally supplied Google Sans.
package example.designsystem

import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

@Immutable
object AppTypography {
    private val family = FontFamily.SansSerif // TODO: Google Sans FontFamily via res/font after license verification
    private fun style(size: Int, weight: FontWeight, height: Int) =
        TextStyle(fontFamily = family, fontSize = size.sp, fontWeight = weight, lineHeight = height.sp)
    val pageTitle = style(22, FontWeight.SemiBold, 28)
    val cardTitle = style(16, FontWeight.SemiBold, 22)
    val sectionLabel = style(12, FontWeight.Medium, 16)
    val bodyTitle = style(16, FontWeight.Medium, 22)
    val rowTitle = style(14, FontWeight.Medium, 20)
    val supporting = style(12, FontWeight.Normal, 16)
    val button = style(14, FontWeight.SemiBold, 20)
    val action = style(14, FontWeight.Medium, 20)
    val heroMetric = style(32, FontWeight.SemiBold, 38)
    val amountInput = style(28, FontWeight.SemiBold, 34)
    val inlineAmount = style(14, FontWeight.SemiBold, 20)
    val chartLabel = style(12, FontWeight.Normal, 16)
}

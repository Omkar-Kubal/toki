// EXAMPLE: Semantic adapter deliberately does not import any unverified Hugeicons API.
package example.designsystem

import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

enum class AppIconKey { Home, Analytics, History, Settings, Back, Add, Edit, Delete, Save, Search, Filter, Share, Export, Close, More, Success, Warning, Error, Info }

object AppIconSizes { const val XS=16; const val SM=20; const val MD=24; const val LG=28; const val XL=32 }

// Connect mapping to license-verified Hugeicons ImageVectors or VectorDrawables.
// Null means asset absent: block release of that icon; do not silently substitute mismatched icons.
interface IconResolver { fun resolve(key: AppIconKey): ImageVector? }

@Composable
fun AppIcon(key: AppIconKey, resolver: IconResolver, description: String?, tint: Color, sizeDp: Int = AppIconSizes.MD) {
    val vector = requireNotNull(resolver.resolve(key)) { "Missing verified icon asset: $key" }
    Icon(imageVector = vector, contentDescription = description, tint = tint, modifier = Modifier.size(sizeDp.dp))
}

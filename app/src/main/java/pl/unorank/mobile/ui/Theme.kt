package pl.unorank.mobile.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val UnoPink = Color(0xFFFF7DFF)
private val DarkColors = darkColorScheme(
    primary = UnoPink, onPrimary = Color(0xFF321332),
    background = Color(0xFF191B20), onBackground = Color(0xFFF4F5F7),
    surface = Color(0xFF24272E), onSurface = Color(0xFFF4F5F7),
    surfaceVariant = Color(0xFF30333C), onSurfaceVariant = Color(0xFFB2B8C3),
    primaryContainer = Color(0xFF442744), onPrimaryContainer = Color(0xFFFFC6FF),
    outline = Color(0xFF606572),
)
private val LightColors = lightColorScheme(
    primary = UnoPink, onPrimary = Color(0xFF321332),
    background = Color(0xFFF5F5F2), onBackground = Color(0xFF20242A),
    surface = Color.White, onSurface = Color(0xFF20242A),
    surfaceVariant = Color(0xFFEBEBEF), onSurfaceVariant = Color(0xFF626974),
    primaryContainer = Color(0xFFFFE8FF), onPrimaryContainer = Color(0xFF702070),
    outline = Color(0xFF787B86),
)

@Composable
fun UnoRankTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (darkTheme) DarkColors else LightColors, content = content)
}

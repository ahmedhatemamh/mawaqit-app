package work.ahmedhatem.mawaqit.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Green = Color(0xFF1B6B3A)
private val Gold = Color(0xFFC9A227)

private val LightColors = lightColorScheme(
    primary = Green,
    onPrimary = Color.White,
    secondary = Gold,
    onSecondary = Color(0xFF2B2100),
    background = Color(0xFFF7F5EC),
    surface = Color(0xFFF7F5EC),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF6FCF97),
    onPrimary = Color(0xFF00391C),
    secondary = Color(0xFFE6C75A),
    onSecondary = Color(0xFF3A2D00),
    background = Color(0xFF101A14),
    surface = Color(0xFF101A14),
)

/** Green and gold, light or dark following the phone setting. */
@Composable
fun MawaqitTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
        content = content,
    )
}

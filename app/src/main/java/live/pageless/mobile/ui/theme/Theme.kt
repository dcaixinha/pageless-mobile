package live.pageless.mobile.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.core.view.WindowCompat
import live.pageless.mobile.data.local.ThemeMode

object PagelessColors {
    val Purple = Color(0xFF8B5CF6)
    val PurpleLight = Color(0xFFC4B5FD)
    val PurpleDark = Color(0xFF6D28D9)

    val DarkBackground = Color(0xFF16141F)
    val DarkSurface = Color(0xFF1E1B2E)
    val DarkSurfaceVariant = Color(0xFF352C55)
    val DarkOnSurface = Color(0xFFF5F3FF)
    val DarkOnSurfaceVariant = Color(0xFFA7A1B8)

    val LightBackground = Color(0xFFFBFAFF)
    val LightSurface = Color(0xFFFFFFFF)
    val LightSurfaceVariant = Color(0xFFEEE8FF)
    val LightOnSurface = Color(0xFF1F1B2D)
    val LightOnSurfaceVariant = Color(0xFF6F6780)
}

private val DarkColors =
    darkColorScheme(
        primary = PagelessColors.Purple,
        onPrimary = Color.White,
        secondary = PagelessColors.PurpleLight,
        onSecondary = Color(0xFF1B1530),
        background = PagelessColors.DarkBackground,
        onBackground = PagelessColors.DarkOnSurface,
        surface = PagelessColors.DarkSurface,
        onSurface = PagelessColors.DarkOnSurface,
        surfaceVariant = PagelessColors.DarkSurfaceVariant,
        onSurfaceVariant = PagelessColors.DarkOnSurfaceVariant,
    )

private val LightColors =
    lightColorScheme(
        primary = PagelessColors.Purple,
        onPrimary = Color.White,
        secondary = PagelessColors.PurpleDark,
        onSecondary = Color.White,
        background = PagelessColors.LightBackground,
        onBackground = PagelessColors.LightOnSurface,
        surface = PagelessColors.LightSurface,
        onSurface = PagelessColors.LightOnSurface,
        surfaceVariant = PagelessColors.LightSurfaceVariant,
        onSurfaceVariant = PagelessColors.LightOnSurfaceVariant,
    )

@Composable
fun PagelessTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    content: @Composable () -> Unit,
) {
    val darkTheme =
        when (themeMode) {
            ThemeMode.SYSTEM -> isSystemInDarkTheme()
            ThemeMode.DARK -> true
            ThemeMode.LIGHT -> false
        }

    val window = (LocalContext.current as? Activity)?.window
    SideEffect {
        window?.let {
            WindowCompat.getInsetsController(it, it.decorView).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content,
    )
}

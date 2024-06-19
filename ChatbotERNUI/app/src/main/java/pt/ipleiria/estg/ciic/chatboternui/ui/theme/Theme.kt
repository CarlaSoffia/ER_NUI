package pt.ipleiria.estg.ciic.chatboternui.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.ViewCompat
import pt.ipleiria.estg.ciic.chatboternui.Objects.ThemeState
import java.util.ResourceBundle

var MidnightBlue = Color(0xFF141435)
var DeepBlue = Color(0xFF252564)
var LavenderBlue = Color(0xFFA0A0C0)
var BabyBlue = Color(0xFFE0EEFF)
var LightGray = Color(0xFFEFEFEF)
var White = Color(0xFFFFFFFF)

// Revise :) - cause they were just switched
val DarkColorScheme = darkColorScheme(
    background = MidnightBlue,
    onBackground = White,

    // non-clickable / non-interactive
    surface = White,
    onSurface = MidnightBlue,

    // button: clickable / interactive - action
    primary = White,
    onPrimary = DeepBlue,

    // button: clickable / interactive - cancel
    secondary = MidnightBlue,
    onSecondary = LavenderBlue,

    // User chat container
    primaryContainer = MidnightBlue,
    onPrimaryContainer = LightGray,

    // MIMO chat container
    secondaryContainer = MidnightBlue,
    onSecondaryContainer = BabyBlue
)

val LightColorScheme = lightColorScheme(
    background = White,
    onBackground = MidnightBlue,

    // non-clickable / non-interactive
    surface = MidnightBlue,
    onSurface = White,

    // button: clickable / interactive - action
    primary = DeepBlue,
    onPrimary = White,

    // button: clickable / interactive - cancel
    secondary = LavenderBlue,
    onSecondary = MidnightBlue,

    // User chat container
    primaryContainer = LightGray,
    onPrimaryContainer = MidnightBlue,

    // MIMO chat container
    secondaryContainer = BabyBlue,
    onSecondaryContainer = MidnightBlue
)

@Composable
fun ChatbotERNUITheme(
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        ThemeState.isDarkThemeEnabled -> DarkColorScheme
        else -> LightColorScheme
    }
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            (view.context as Activity).window.statusBarColor = colorScheme.primary.toArgb()
            ViewCompat.getWindowInsetsController(view)?.isAppearanceLightStatusBars =
                !ThemeState.isDarkThemeEnabled
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
package pt.ipleiria.estg.ciic.chatboternui.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

var MidnightBlue = Color(0xFF141435)
var DeepBlue = Color(0xFF252564)
var LavenderBlue = Color(0xFFA0A0C0)
var BabyBlue = Color(0xFFE0EEFF)
var LightGray = Color(0xFFEFEFEF)
var White = Color(0xFFFFFFFF)


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
        MaterialTheme(
        colorScheme = LightColorScheme,
        typography = Typography,
        content = content
    )
}
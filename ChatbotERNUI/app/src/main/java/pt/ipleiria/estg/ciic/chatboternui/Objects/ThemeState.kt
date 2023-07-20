package pt.ipleiria.estg.ciic.chatboternui.Objects

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

object ThemeState {
    var isDarkThemeEnabled by mutableStateOf(false)
}
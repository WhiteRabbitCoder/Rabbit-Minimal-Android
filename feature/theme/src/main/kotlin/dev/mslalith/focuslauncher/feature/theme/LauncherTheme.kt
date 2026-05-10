package dev.mslalith.focuslauncher.feature.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import dev.mslalith.focuslauncher.core.model.Theme
import dev.mslalith.focuslauncher.core.ui.controller.setSystemBarsColor
import dev.mslalith.focuslauncher.core.ui.providers.LocalSystemUiController
import dev.mslalith.focuslauncher.feature.theme.data.Typography
import dev.mslalith.focuslauncher.feature.theme.data.darkColors

@Suppress("UnusedParameter")
@Composable
fun LauncherTheme(
    currentTheme: Theme,
    content: @Composable () -> Unit
) {
    val systemUiController = LocalSystemUiController.current

    LaunchedEffect(key1 = systemUiController) {
        systemUiController.setSystemBarsColor(color = darkColors.surface)
    }

    MaterialTheme(
        colorScheme = darkColors,
        typography = Typography,
        content = content
    )
}

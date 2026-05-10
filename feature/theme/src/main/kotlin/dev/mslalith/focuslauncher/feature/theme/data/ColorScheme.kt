@file:IgnoreInKoverReport

package dev.mslalith.focuslauncher.feature.theme.data

import androidx.compose.material3.darkColorScheme
import dev.mslalith.focuslauncher.core.lint.kover.IgnoreInKoverReport

internal val darkColors = darkColorScheme(
    primary = rabbit_primary,
    onPrimary = rabbit_onPrimary,
    primaryContainer = rabbit_primaryContainer,
    onPrimaryContainer = rabbit_onPrimaryContainer,
    secondary = rabbit_secondary,
    onSecondary = rabbit_onSecondary,
    secondaryContainer = rabbit_secondaryContainer,
    onSecondaryContainer = rabbit_onSecondaryContainer,
    tertiary = rabbit_tertiary,
    onTertiary = rabbit_onTertiary,
    tertiaryContainer = rabbit_tertiaryContainer,
    onTertiaryContainer = rabbit_onTertiaryContainer,
    error = rabbit_error,
    onError = rabbit_onError,
    errorContainer = rabbit_errorContainer,
    onErrorContainer = rabbit_onErrorContainer,
    background = rabbit_background,
    onBackground = rabbit_onBackground,
    surface = rabbit_surface,
    onSurface = rabbit_onSurface,
    surfaceVariant = rabbit_surfaceVariant,
    onSurfaceVariant = rabbit_onSurfaceVariant,
    outline = rabbit_outline,
    outlineVariant = rabbit_outlineVariant,
    scrim = rabbit_scrim,
    inverseSurface = rabbit_inverseSurface,
    inverseOnSurface = rabbit_inverseOnSurface,
    inversePrimary = rabbit_inversePrimary,
    surfaceTint = rabbit_surfaceTint
)

// Alias so existing references to lightColors still compile
internal val lightColors = darkColors

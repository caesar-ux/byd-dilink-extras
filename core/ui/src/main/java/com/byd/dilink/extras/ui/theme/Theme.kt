package com.byd.dilink.extras.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DiLinkDarkColorScheme = darkColorScheme(
    primary = DiLinkCyan,
    onPrimary = DiLinkBackground,
    primaryContainer = DiLinkCyanDark,
    onPrimaryContainer = DiLinkCyanLight,
    secondary = DiLinkCyanLight,
    onSecondary = DiLinkBackground,
    background = DiLinkBackground,
    onBackground = DiLinkTextPrimary,
    surface = DiLinkSurface,
    onSurface = DiLinkTextPrimary,
    surfaceVariant = DiLinkSurfaceVariant,
    onSurfaceVariant = DiLinkTextSecondary,
    error = StatusRed,
    onError = DiLinkTextPrimary,
)

@Composable
fun DiLinkExtrasTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DiLinkDarkColorScheme,
        content = content
    )
}

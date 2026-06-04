package com.elephantcos.gemmachat.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary              = AccentPurple,
    onPrimary            = TextPrimary,
    primaryContainer     = UserBubble,
    onPrimaryContainer   = TextPrimary,
    secondary            = AccentTeal,
    onSecondary          = BackgroundDark,
    secondaryContainer   = SurfaceVariant,
    onSecondaryContainer = TextPrimary,
    background           = BackgroundDark,
    onBackground         = TextPrimary,
    surface              = SurfaceDark,
    onSurface            = TextPrimary,
    surfaceVariant       = SurfaceVariant,
    onSurfaceVariant     = TextSecondary,
    outline              = DividerColor,
    error                = ErrorRed,
    onError              = TextPrimary
)

@Composable
fun GemmaChatTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography  = Typography,
        content     = content
    )
}

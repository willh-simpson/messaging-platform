package com.messaging.messagingplatform.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary          = Cyan400,
    onPrimary        = Slate950,
    primaryContainer = Slate700,
    onPrimaryContainer = Cyan400,

    background       = Slate950,
    onBackground     = Slate200,

    surface          = Slate900,
    onSurface        = Slate200,
    surfaceVariant   = Slate800,
    onSurfaceVariant = Slate400,

    outline          = Slate600,
    outlineVariant   = Slate700,

    error            = Rose500,
    onError          = Slate950,
)

@Composable
fun MessagingTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography  = Typography,
        content     = content,
    )
}
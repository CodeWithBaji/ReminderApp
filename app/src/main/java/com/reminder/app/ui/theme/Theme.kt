package com.reminder.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColorScheme = lightColorScheme(
    primary = PurplePrimary,
    onPrimary = OnPurple,
    primaryContainer = PurpleWash,
    onPrimaryContainer = Ink,
    secondary = PurpleBright,
    onSecondary = OnPurple,
    tertiary = TealComplete,
    onTertiary = OnPurple,
    background = PurpleMist,
    onBackground = Ink,
    surface = OnPurple,
    onSurface = Ink,
    surfaceVariant = PurpleWash,
    onSurfaceVariant = Ink,
    error = ColorError,
    onError = OnPurple
)

private val DarkColorScheme = darkColorScheme(
    primary = PurpleSoft,
    onPrimary = Ink,
    primaryContainer = PurpleDark,
    onPrimaryContainer = OnPurple,
    secondary = PurpleBright,
    onSecondary = OnPurple,
    tertiary = TealComplete,
    onTertiary = OnPurple,
    background = PurpleNight,
    onBackground = OnPurple,
    surface = PurpleNightSurface,
    onSurface = OnPurple,
    surfaceVariant = PurpleDark,
    onSurfaceVariant = PurpleSoft,
    error = ColorErrorDark,
    onError = Ink
)

/** App Material3 theme. Dynamic color is off so the #6200EE brand is always used. */
@Composable
fun ReminderAppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
        typography = Typography,
        content = content
    )
}

package com.reminder.app.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.FabPosition
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/**
 * Full-screen brand gradient: [PurplePrimary] (#6200EE) at the top, washing into a
 * soft lavender so cards stay readable.
 */
@Composable
fun purpleScreenGradient(darkTheme: Boolean = isSystemInDarkTheme()): Brush {
    return if (darkTheme) {
        Brush.verticalGradient(
            colorStops = arrayOf(
                0.0f to PurpleDeep,
                0.35f to PurplePrimary,
                1.0f to PurpleNight
            )
        )
    } else {
        Brush.verticalGradient(
            colorStops = arrayOf(
                0.0f to PurplePrimary,
                0.32f to PurpleBright,
                1.0f to PurpleMist
            )
        )
    }
}

fun reminderAccentGradient(isPending: Boolean): Brush = Brush.verticalGradient(
    colors = if (isPending) {
        listOf(PurplePrimary, PurpleBright)
    } else {
        listOf(PurpleWash, PurpleSoft)
    }
)

/**
 * Scaffold drawn on the purple gradient. Container is transparent so the
 * gradient shows through the top app bar and list.
 */
@Composable
fun PurpleGradientScaffold(
    topBar: @Composable () -> Unit,
    floatingActionButton: @Composable () -> Unit = {},
    content: @Composable (PaddingValues) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(purpleScreenGradient())
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.onBackground,
            topBar = topBar,
            floatingActionButton = floatingActionButton,
            floatingActionButtonPosition = FabPosition.End,
            content = content
        )
    }
}

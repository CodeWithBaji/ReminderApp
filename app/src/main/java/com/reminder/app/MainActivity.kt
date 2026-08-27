package com.reminder.app

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.reminder.app.presentation.ReminderNavHost
import com.reminder.app.ui.theme.ReminderAppTheme
import dagger.hilt.android.AndroidEntryPoint

/** Single Activity host. All UI is Compose; reminder data never lives here. */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val brand = Color.parseColor("#6200EE")
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(brand),
            navigationBarStyle = SystemBarStyle.light(
                Color.parseColor("#F6F0FF"),
                Color.parseColor("#1A1028")
            )
        )
        setContent {
            ReminderAppTheme {
                ReminderNavHost()
            }
        }
    }
}

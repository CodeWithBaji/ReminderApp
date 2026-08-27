package com.reminder.app.presentation

import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.reminder.app.presentation.create.CreateReminderScreen
import com.reminder.app.presentation.create.CreateReminderViewModel
import com.reminder.app.presentation.reminderlist.ReminderListScreen

/** Compose Navigation routes. Keep them in one place so argument names stay in sync. */
object Routes {
    const val LIST = "list"
    const val CREATE = "create/{reminderId}"
    fun create(reminderId: Long = CreateReminderViewModel.NEW_REMINDER_ID) = "create/$reminderId"
}

/** Single-activity navigation graph: list ↔ create/edit. */
@Composable
fun ReminderNavHost() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = Routes.LIST) {
        composable(Routes.LIST) {
            ReminderListScreen(
                viewModel = hiltViewModel(),
                onCreateClick = { navController.navigate(Routes.create()) },
                onReminderClick = { id -> navController.navigate(Routes.create(id)) }
            )
        }
        composable(
            route = Routes.CREATE,
            arguments = listOf(
                navArgument(CreateReminderViewModel.ARG_REMINDER_ID) {
                    type = NavType.LongType
                    defaultValue = CreateReminderViewModel.NEW_REMINDER_ID
                }
            )
        ) {
            CreateReminderScreen(
                viewModel = hiltViewModel(),
                onBack = { navController.popBackStack() }
            )
        }
    }
}

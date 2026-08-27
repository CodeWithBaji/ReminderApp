package com.reminder.app

import android.app.Application
import com.reminder.app.domain.usecase.RescheduleRemindersUseCase
import com.reminder.app.notification.ReminderNotificationManager
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Hilt application. On every process start we reschedule PENDING future reminders.
 *
 * This covers force-stop (alarms wiped, BOOT_COMPLETED will not run until the user
 * launches the app) and any OEM that drops AlarmManager entries while Room still
 * has the rows. Duplicate schedule() calls are safe: same requestCode replaces.
 */
@HiltAndroidApp
class ReminderApplication : Application() {

    @Inject lateinit var rescheduleRemindersUseCase: RescheduleRemindersUseCase
    @Inject lateinit var notificationManager: ReminderNotificationManager

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        notificationManager.ensureChannel()
        applicationScope.launch {
            runCatching { rescheduleRemindersUseCase() }
        }
    }
}

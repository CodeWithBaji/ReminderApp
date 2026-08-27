package com.reminder.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.reminder.app.domain.model.ReminderStatus
import com.reminder.app.domain.repository.ReminderRepository
import com.reminder.app.notification.ReminderNotificationManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Fired by AlarmManager at the reminder's trigger time.
 *
 * Why a BroadcastReceiver: the app process may be dead. The OS delivers this
 * even if no Activity exists. onReceive() has a short lifetime (~10s), so we
 * call goAsync() before hitting Room.
 *
 * Edge case — alarm fires after cancel/complete:
 * We always re-read Room. If status is no longer PENDING, we skip the notification.
 * This is the race where cancel() in AlarmManager lost to an alarm that was
 * already being delivered.
 */
@AndroidEntryPoint
class ReminderReceiver : BroadcastReceiver() {

    @Inject lateinit var repository: ReminderRepository
    @Inject lateinit var notificationManager: ReminderNotificationManager

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ReminderContract.ACTION_SHOW) return
        val reminderId = intent.getLongExtra(ReminderContract.EXTRA_REMINDER_ID, -1L)
        if (reminderId <= 0L) {
            Log.w(TAG, "Missing reminder id")
            return
        }

        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val reminder = repository.getReminder(reminderId)
                if (reminder == null) {
                    Log.w(TAG, "Reminder $reminderId not in Room; ignoring alarm")
                    return@launch
                }
                if (reminder.status != ReminderStatus.PENDING) {
                    Log.d(TAG, "Reminder $reminderId is ${reminder.status}; skipping notification")
                    return@launch
                }
                notificationManager.show(reminder)
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        private const val TAG = "ReminderReceiver"
    }
}

package com.reminder.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.reminder.app.domain.usecase.CompleteReminderUseCase
import com.reminder.app.domain.usecase.DeleteReminderUseCase
import com.reminder.app.notification.ReminderNotificationManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Handles "Mark Complete" and "Cancel" actions on the reminder notification.
 *
 * Why a receiver instead of starting an Activity: the user may never open the UI.
 * Updating Room here keeps the source of truth correct even if the process was
 * started only to handle the tap.
 *
 * After the status write, we dismiss the notification. The matching alarm was
 * already delivered (this tap happens after fire) but Complete/Cancel still
 * cancel any leftover PendingIntent for consistency.
 */
@AndroidEntryPoint
class ReminderActionReceiver : BroadcastReceiver() {

    @Inject lateinit var completeReminder: CompleteReminderUseCase
    @Inject lateinit var deleteReminder: DeleteReminderUseCase
    @Inject lateinit var notificationManager: ReminderNotificationManager

    override fun onReceive(context: Context, intent: Intent) {
        val reminderId = intent.getLongExtra(ReminderContract.EXTRA_REMINDER_ID, -1L)
        if (reminderId <= 0L) return

        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                when (intent.action) {
                    ReminderContract.ACTION_COMPLETE -> {
                        Log.d(TAG, "Complete reminder $reminderId from notification")
                        completeReminder(reminderId)
                    }
                    ReminderContract.ACTION_CANCEL -> {
                        Log.d(TAG, "Cancel reminder $reminderId from notification")
                        deleteReminder(reminderId)
                    }
                }
                notificationManager.dismiss(reminderId)
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        private const val TAG = "ReminderActionReceiver"
    }
}

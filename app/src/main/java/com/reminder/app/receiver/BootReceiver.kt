package com.reminder.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.reminder.app.domain.usecase.RescheduleRemindersUseCase
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Restores alarms after reboot.
 *
 * Why this exists: AlarmManager alarms are in-memory in the OS and are discarded
 * when the device reboots. BOOT_COMPLETED is the system callback to put them back.
 *
 * We do not create alarms from scratch — we read PENDING future rows from Room
 * (source of truth) and schedule each id once. Same requestCode => no duplicates.
 *
 * Manifest notes:
 * - exported=true is required so the system can send BOOT_COMPLETED.
 * - RECEIVE_BOOT_COMPLETED permission is required.
 * - Force-stop: the user force-stopping the app also wipes alarms AND prevents
 *   this receiver from running until the user opens the app again. App-start
 *   reschedule in ReminderApplication covers that case.
 */
@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {

    @Inject lateinit var rescheduleRemindersUseCase: RescheduleRemindersUseCase

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        Log.d(TAG, "BOOT_COMPLETED — rescheduling pending reminders from Room")
        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                rescheduleRemindersUseCase()
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        private const val TAG = "BootReceiver"
    }
}

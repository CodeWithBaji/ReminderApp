package com.reminder.app.scheduler

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.reminder.app.domain.model.Reminder
import com.reminder.app.receiver.ReminderContract
import com.reminder.app.receiver.ReminderReceiver
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Schedules wall-clock alarms via AlarmManager.
 *
 * Constraint this solves: Compose / ViewModel / coroutines all die with the process.
 * AlarmManager is owned by the OS and can wake us via [ReminderReceiver] even if
 * the app is not running — but it is NOT a database. Room remains the source of truth.
 *
 * PendingIntent identity (critical):
 * The OS matches PendingIntents by requestCode + Intent.filterEquals (component,
 * action, data, categories — NOT extras). We use requestCode = reminder.id.toInt()
 * so two reminders at the same timestamp still get two distinct alarms. Using a
 * constant requestCode would make the second reminder overwrite the first.
 *
 * FLAG_UPDATE_CURRENT: replacing extras if we reschedule the same id.
 * FLAG_IMMUTABLE: required targeting API 31+; our receiver does not need to mutate
 * the Intent.
 */
@Singleton
class AlarmManagerReminderScheduler @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val alarmManager: AlarmManager
) : ReminderScheduler {

    override fun schedule(reminder: Reminder) {
        val pendingIntent = pendingIntent(reminder, PendingIntent.FLAG_UPDATE_CURRENT)
        val triggerAt = reminder.triggerTimeMillis

        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                // Android 12+: exact alarms are a special app-op.
                // Android 14+: SCHEDULE_EXACT_ALARM is denied by default for most apps.
                // We never assume canScheduleExactAlarms() is true.
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerAt,
                        pendingIntent
                    )
                } else {
                    // Inexact fallback. The OS may batch this with other work (Doze /
                    // app standby). Delivery can be minutes late. setAndAllowWhileIdle
                    // is still better than set() because it can fire in Doze, but it is
                    // rate-limited (~once per 9 minutes in idle).
                    Log.w(TAG, "Exact alarms unavailable; scheduling inexact alarm for id=${reminder.id}")
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerAt,
                        pendingIntent
                    )
                }
            }
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.M -> {
                // API 23–30: setExactAndAllowWhileIdle fires in Doze. Still not 100%
                // "exact" under deep idle, but it is the strongest public API.
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAt,
                    pendingIntent
                )
            }
            else -> {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    triggerAt,
                    pendingIntent
                )
            }
        }
    }

    override fun cancel(reminder: Reminder) {
        // We must pass a matching PendingIntent for AlarmManager.cancel to find the alarm.
        val pendingIntent = pendingIntent(reminder, PendingIntent.FLAG_UPDATE_CURRENT)
        alarmManager.cancel(pendingIntent)
        pendingIntent.cancel()
    }

    private fun pendingIntent(reminder: Reminder, extraFlags: Int): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = ReminderContract.ACTION_SHOW
            putExtra(ReminderContract.EXTRA_REMINDER_ID, reminder.id)
        }
        val flags = extraFlags or PendingIntent.FLAG_IMMUTABLE
        // reminder.id is a Room autoincrement Long; overflowing Int is not realistic here.
        return PendingIntent.getBroadcast(
            context,
            reminder.id.toInt(),
            intent,
            flags
        )
    }

    companion object {
        private const val TAG = "ReminderScheduler"
    }
}

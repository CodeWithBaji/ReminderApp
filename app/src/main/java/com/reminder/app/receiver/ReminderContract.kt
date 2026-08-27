package com.reminder.app.receiver

/**
 * Shared Intent contract between the scheduler, alarm receiver, and notification actions.
 *
 * Actions are explicit (component set on the Intent) plus a unique action string so
 * PendingIntent.filterEquals stays distinct across the three broadcast types.
 */
object ReminderContract {
    const val EXTRA_REMINDER_ID = "extra_reminder_id"

    const val ACTION_SHOW = "com.reminder.app.action.SHOW_REMINDER"
    const val ACTION_COMPLETE = "com.reminder.app.action.COMPLETE_REMINDER"
    const val ACTION_CANCEL = "com.reminder.app.action.CANCEL_REMINDER"
}

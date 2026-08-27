package com.reminder.app.scheduler

import com.reminder.app.domain.model.Reminder

/**
 * Abstraction over the OS timer so use cases stay testable.
 *
 * AlarmManager is an Android framework API; faking this interface in JVM unit tests
 * avoids Robolectric. Production uses [AlarmManagerReminderScheduler].
 */
interface ReminderScheduler {
    /** Register or replace the alarm for [reminder]. Identity is [Reminder.id]. */
    fun schedule(reminder: Reminder)

    /** Cancel the PendingIntent for [reminder], if it exists. Idempotent. */
    fun cancel(reminder: Reminder)
}

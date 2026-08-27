package com.reminder.app.testfakes

import com.reminder.app.domain.model.Reminder
import com.reminder.app.scheduler.ReminderScheduler

/**
 * In-memory scheduler. Records schedule/cancel calls so tests can assert
 * AlarmManager side effects without the Android framework.
 */
class FakeReminderScheduler : ReminderScheduler {
    val scheduled = mutableListOf<Reminder>()
    val cancelled = mutableListOf<Reminder>()

    override fun schedule(reminder: Reminder) {
        scheduled.removeAll { it.id == reminder.id }
        scheduled += reminder
    }

    override fun cancel(reminder: Reminder) {
        scheduled.removeAll { it.id == reminder.id }
        cancelled += reminder
    }

    fun scheduledIds(): List<Long> = scheduled.map { it.id }
}

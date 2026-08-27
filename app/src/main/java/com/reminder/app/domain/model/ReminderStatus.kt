package com.reminder.app.domain.model

/**
 * Lifecycle of a reminder row in Room (the source of truth).
 *
 * AlarmManager is only a trigger mechanism. Status changes always go through Room first
 * so process death, reboot, or a stale alarm cannot resurrect a cancelled/completed reminder.
 */
enum class ReminderStatus {
    /** Alarm should be scheduled (or restored after reboot) if the time is still in the future. */
    PENDING,
    /** User marked it done. No alarm should remain. */
    COMPLETED,
    /** User cancelled it. No alarm should remain. The row is kept for the list. */
    CANCELLED
}

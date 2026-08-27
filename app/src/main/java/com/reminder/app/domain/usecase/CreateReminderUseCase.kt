package com.reminder.app.domain.usecase

import com.reminder.app.domain.model.Reminder
import com.reminder.app.domain.repository.ReminderRepository
import com.reminder.app.domain.time.TimeProvider
import com.reminder.app.scheduler.ReminderScheduler
import javax.inject.Inject

/**
 * Creates a reminder.
 *
 * Order is intentional: persist in Room, then schedule. If the process dies after
 * insert but before [ReminderScheduler.schedule], reboot recovery / next app launch
 * will reschedule from Room. The reverse order would produce an alarm with no row.
 *
 * Edge cases:
 * - Blank title is rejected (nothing written, no alarm).
 * - A trigger time in the past is rejected. AlarmManager would fire immediately
 *   (or already be overdue), which is surprising UX and hard to reason about.
 */
class CreateReminderUseCase @Inject constructor(
    private val repository: ReminderRepository,
    private val scheduler: ReminderScheduler,
    private val timeProvider: TimeProvider
) {
    /**
     * @param title user-entered title; blank after trim is rejected
     * @param triggerTimeMillis local wall-clock epoch millis; must be strictly in the future
     */
    suspend operator fun invoke(
        title: String,
        triggerTimeMillis: Long
    ): CreateReminderResult {
        val trimmed = title.trim()
        if (trimmed.isBlank()) return CreateReminderResult.BlankTitle
        if (triggerTimeMillis <= timeProvider.now()) return CreateReminderResult.TimeInPast

        val reminder = repository.create(trimmed, triggerTimeMillis)
        scheduler.schedule(reminder)
        return CreateReminderResult.Success(reminder)
    }
}

/** Outcome of create. Failures never write Room or schedule an alarm. */
sealed interface CreateReminderResult {
    data class Success(val reminder: Reminder) : CreateReminderResult
    data object BlankTitle : CreateReminderResult
    data object TimeInPast : CreateReminderResult
}

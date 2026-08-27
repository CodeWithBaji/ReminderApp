package com.reminder.app.domain.usecase

import com.reminder.app.domain.repository.ReminderRepository
import com.reminder.app.domain.time.TimeProvider
import com.reminder.app.scheduler.ReminderScheduler
import javax.inject.Inject

/**
 * Re-registers AlarmManager alarms from Room.
 *
 * Why this exists: AlarmManager does not survive reboot. After BOOT_COMPLETED we
 * read PENDING rows whose trigger time is still in the future and schedule them
 * again. The same use case runs on app start to recover from force-stop (which
 * also wipes alarms, and does not fire BOOT_COMPLETED until the user opens the app).
 *
 * Duplicate alarms: each PendingIntent uses requestCode = reminder.id, so calling
 * schedule() twice replaces the previous alarm instead of stacking a second one.
 *
 * Past PENDING reminders are left as-is (not auto-completed). The user can still
 * complete/cancel them from the list. Scheduling them would fire immediately.
 */
class RescheduleRemindersUseCase @Inject constructor(
    private val repository: ReminderRepository,
    private val scheduler: ReminderScheduler,
    private val timeProvider: TimeProvider
) {
    /** Safe to call more than once: same reminder id replaces the previous alarm. */
    suspend operator fun invoke() {
        val now = timeProvider.now()
        repository.getPendingReminders()
            .filter { it.triggerTimeMillis > now }
            .forEach { scheduler.schedule(it) }
    }
}

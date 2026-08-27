package com.reminder.app.domain.repository

import com.reminder.app.domain.model.Reminder
import com.reminder.app.domain.model.ReminderStatus
import kotlinx.coroutines.flow.Flow

/**
 * Contract for reminder persistence. Room is the only implementation in this app.
 *
 * Observing via [Flow] means the UI survives Activity destruction and process death:
 * the next process simply collects from Room again. Nothing is cached in memory
 * as the source of truth.
 */
interface ReminderRepository {
    /** Live list for the UI. Re-collect after process death; Room is the source of truth. */
    fun observeReminders(): Flow<List<Reminder>>

    suspend fun getReminder(id: Long): Reminder?

    /** Used by reboot / process-start recovery to rebuild AlarmManager entries. */
    suspend fun getPendingReminders(): List<Reminder>

    /** Inserts a PENDING row and returns it with the generated id. */
    suspend fun create(title: String, triggerTimeMillis: Long): Reminder

    suspend fun update(reminder: Reminder)

    suspend fun updateStatus(id: Long, status: ReminderStatus)
}

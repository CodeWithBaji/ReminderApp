package com.reminder.app.testfakes

import com.reminder.app.domain.model.Reminder
import com.reminder.app.domain.model.ReminderStatus
import com.reminder.app.domain.repository.ReminderRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/** In-memory [ReminderRepository] for JVM unit tests. */
class FakeReminderRepository : ReminderRepository {
    private val items = MutableStateFlow<List<Reminder>>(emptyList())
    var nextId = 1L

    override fun observeReminders(): Flow<List<Reminder>> = items.asStateFlow()

    override suspend fun getReminder(id: Long): Reminder? =
        items.value.find { it.id == id }

    override suspend fun getPendingReminders(): List<Reminder> =
        items.value.filter { it.status == ReminderStatus.PENDING }

    override suspend fun create(title: String, triggerTimeMillis: Long): Reminder {
        val reminder = Reminder(
            id = nextId++,
            title = title,
            triggerTimeMillis = triggerTimeMillis,
            status = ReminderStatus.PENDING
        )
        items.value = items.value + reminder
        return reminder
    }

    override suspend fun update(reminder: Reminder) {
        items.value = items.value.map { if (it.id == reminder.id) reminder else it }
    }

    override suspend fun updateStatus(id: Long, status: ReminderStatus) {
        items.value = items.value.map {
            if (it.id == id) it.copy(status = status) else it
        }
    }

    fun seed(vararg reminders: Reminder) {
        items.value = reminders.toList()
        nextId = (reminders.maxOfOrNull { it.id } ?: 0L) + 1
    }
}

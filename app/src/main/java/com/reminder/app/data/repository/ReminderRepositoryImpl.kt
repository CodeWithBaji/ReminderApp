package com.reminder.app.data.repository

import com.reminder.app.data.local.ReminderDao
import com.reminder.app.data.local.ReminderEntity
import com.reminder.app.data.local.toDomain
import com.reminder.app.data.local.toEntity
import com.reminder.app.domain.model.Reminder
import com.reminder.app.domain.model.ReminderStatus
import com.reminder.app.domain.repository.ReminderRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/** Room-backed [ReminderRepository]. Mapping stays here so use cases never see Entity types. */
@Singleton
class ReminderRepositoryImpl @Inject constructor(
    private val dao: ReminderDao
) : ReminderRepository {

    override fun observeReminders(): Flow<List<Reminder>> =
        dao.observeReminders().map { rows -> rows.map { it.toDomain() } }

    override suspend fun getReminder(id: Long): Reminder? =
        dao.getById(id)?.toDomain()

    override suspend fun getPendingReminders(): List<Reminder> =
        dao.getByStatus(ReminderStatus.PENDING).map { it.toDomain() }

    override suspend fun create(title: String, triggerTimeMillis: Long): Reminder {
        val id = dao.insert(
            ReminderEntity(
                title = title,
                triggerTimeMillis = triggerTimeMillis,
                status = ReminderStatus.PENDING
            )
        )
        return Reminder(
            id = id,
            title = title,
            triggerTimeMillis = triggerTimeMillis,
            status = ReminderStatus.PENDING
        )
    }

    override suspend fun update(reminder: Reminder) {
        dao.update(reminder.toEntity())
    }

    override suspend fun updateStatus(id: Long, status: ReminderStatus) {
        dao.updateStatus(id, status)
    }
}

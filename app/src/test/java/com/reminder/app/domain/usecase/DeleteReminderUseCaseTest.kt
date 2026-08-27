package com.reminder.app.domain.usecase

import com.reminder.app.domain.model.Reminder
import com.reminder.app.domain.model.ReminderStatus
import com.reminder.app.testfakes.FakeReminderRepository
import com.reminder.app.testfakes.FakeReminderScheduler
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class DeleteReminderUseCaseTest {

    private lateinit var repository: FakeReminderRepository
    private lateinit var scheduler: FakeReminderScheduler
    private lateinit var useCase: DeleteReminderUseCase

    @Before
    fun setUp() {
        repository = FakeReminderRepository()
        scheduler = FakeReminderScheduler()
        useCase = DeleteReminderUseCase(repository, scheduler)
    }

    @Test
    fun `sets status to cancelled and cancels the alarm`() = runTest {
        val reminder = Reminder(1L, "Call mom", 9_000L, ReminderStatus.PENDING)
        repository.seed(reminder)
        scheduler.schedule(reminder)

        useCase(1L)

        assertEquals(ReminderStatus.CANCELLED, repository.getReminder(1L)?.status)
        assertEquals(listOf(1L), scheduler.cancelled.map { it.id })
        assertTrue(scheduler.scheduled.isEmpty())
    }

    @Test
    fun `missing reminder is a no-op`() = runTest {
        useCase(99L)
        assertTrue(scheduler.cancelled.isEmpty())
    }
}

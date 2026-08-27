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

class CompleteReminderUseCaseTest {

    private lateinit var repository: FakeReminderRepository
    private lateinit var scheduler: FakeReminderScheduler
    private lateinit var useCase: CompleteReminderUseCase

    @Before
    fun setUp() {
        repository = FakeReminderRepository()
        scheduler = FakeReminderScheduler()
        useCase = CompleteReminderUseCase(repository, scheduler)
    }

    @Test
    fun `sets status to completed and cancels the alarm`() = runTest {
        val reminder = Reminder(2L, "Stretch", 9_000L, ReminderStatus.PENDING)
        repository.seed(reminder)
        scheduler.schedule(reminder)

        useCase(2L)

        assertEquals(ReminderStatus.COMPLETED, repository.getReminder(2L)?.status)
        assertEquals(listOf(2L), scheduler.cancelled.map { it.id })
        assertTrue(scheduler.scheduled.isEmpty())
    }

    @Test
    fun `missing reminder is a no-op`() = runTest {
        useCase(99L)
        assertTrue(scheduler.cancelled.isEmpty())
    }
}

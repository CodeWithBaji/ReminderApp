package com.reminder.app.domain.usecase

import com.reminder.app.domain.model.ReminderStatus
import com.reminder.app.testfakes.FakeReminderRepository
import com.reminder.app.testfakes.FakeReminderScheduler
import com.reminder.app.testfakes.FakeTimeProvider
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class CreateReminderUseCaseTest {

    private lateinit var repository: FakeReminderRepository
    private lateinit var scheduler: FakeReminderScheduler
    private lateinit var timeProvider: FakeTimeProvider
    private lateinit var useCase: CreateReminderUseCase

    @Before
    fun setUp() {
        repository = FakeReminderRepository()
        scheduler = FakeReminderScheduler()
        timeProvider = FakeTimeProvider(nowMillis = 1_000L)
        useCase = CreateReminderUseCase(repository, scheduler, timeProvider)
    }

    @Test
    fun `persists then schedules a pending reminder`() = runTest {
        val result = useCase(title = "Buy milk", triggerTimeMillis = 5_000L)

        val success = result as CreateReminderResult.Success
        assertEquals("Buy milk", success.reminder.title)
        assertEquals(ReminderStatus.PENDING, success.reminder.status)
        assertEquals(success.reminder, repository.getReminder(success.reminder.id))
        assertEquals(listOf(success.reminder.id), scheduler.scheduledIds())
    }

    @Test
    fun `rejects blank title without writing or scheduling`() = runTest {
        val result = useCase(title = "   ", triggerTimeMillis = 5_000L)

        assertEquals(CreateReminderResult.BlankTitle, result)
        assertTrue(repository.getPendingReminders().isEmpty())
        assertTrue(scheduler.scheduled.isEmpty())
    }

    @Test
    fun `rejects time in the past without writing or scheduling`() = runTest {
        val result = useCase(title = "Late", triggerTimeMillis = 1_000L)

        assertEquals(CreateReminderResult.TimeInPast, result)
        assertTrue(repository.getPendingReminders().isEmpty())
        assertTrue(scheduler.scheduled.isEmpty())
    }

    @Test
    fun `two reminders at the same time both get scheduled with distinct ids`() = runTest {
        val first = useCase("A", 5_000L) as CreateReminderResult.Success
        val second = useCase("B", 5_000L) as CreateReminderResult.Success

        assertTrue(first.reminder.id != second.reminder.id)
        assertEquals(listOf(first.reminder.id, second.reminder.id), scheduler.scheduledIds())
    }
}

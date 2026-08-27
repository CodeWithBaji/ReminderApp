package com.reminder.app.domain.usecase

import com.reminder.app.domain.model.Reminder
import com.reminder.app.domain.model.ReminderStatus
import com.reminder.app.testfakes.FakeReminderRepository
import com.reminder.app.testfakes.FakeReminderScheduler
import com.reminder.app.testfakes.FakeTimeProvider
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class UpdateReminderUseCaseTest {

    private lateinit var repository: FakeReminderRepository
    private lateinit var scheduler: FakeReminderScheduler
    private lateinit var timeProvider: FakeTimeProvider
    private lateinit var useCase: UpdateReminderUseCase

    @Before
    fun setUp() {
        repository = FakeReminderRepository()
        scheduler = FakeReminderScheduler()
        timeProvider = FakeTimeProvider(nowMillis = 1_000L)
        useCase = UpdateReminderUseCase(repository, scheduler, timeProvider)
    }

    @Test
    fun `updates room then replaces the alarm`() = runTest {
        val original = Reminder(1L, "Old", 5_000L, ReminderStatus.PENDING)
        repository.seed(original)
        scheduler.schedule(original)

        val result = useCase(1L, "New", 8_000L) as UpdateReminderResult.Success

        assertEquals("New", result.reminder.title)
        assertEquals(8_000L, result.reminder.triggerTimeMillis)
        assertEquals(result.reminder, repository.getReminder(1L))
        assertEquals(listOf(1L), scheduler.cancelled.map { it.id })
        assertEquals(listOf(1L), scheduler.scheduledIds())
        assertEquals(8_000L, scheduler.scheduled.single().triggerTimeMillis)
    }

    @Test
    fun `does not schedule when the new time is in the past`() = runTest {
        repository.seed(Reminder(1L, "Old", 5_000L, ReminderStatus.PENDING))

        val result = useCase(1L, "New", 500L)

        assertEquals(UpdateReminderResult.TimeInPast, result)
        assertEquals("Old", repository.getReminder(1L)?.title)
        assertTrue(scheduler.scheduled.isEmpty())
    }
}

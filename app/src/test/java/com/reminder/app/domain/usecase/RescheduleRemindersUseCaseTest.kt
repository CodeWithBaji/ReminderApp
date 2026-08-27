package com.reminder.app.domain.usecase

import com.reminder.app.domain.model.Reminder
import com.reminder.app.domain.model.ReminderStatus
import com.reminder.app.testfakes.FakeReminderRepository
import com.reminder.app.testfakes.FakeReminderScheduler
import com.reminder.app.testfakes.FakeTimeProvider
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class RescheduleRemindersUseCaseTest {

    private lateinit var repository: FakeReminderRepository
    private lateinit var scheduler: FakeReminderScheduler
    private lateinit var timeProvider: FakeTimeProvider
    private lateinit var useCase: RescheduleRemindersUseCase

    @Before
    fun setUp() {
        repository = FakeReminderRepository()
        scheduler = FakeReminderScheduler()
        timeProvider = FakeTimeProvider(nowMillis = 10_000L)
        useCase = RescheduleRemindersUseCase(repository, scheduler, timeProvider)
    }

    @Test
    fun `schedules only pending reminders still in the future`() = runTest {
        repository.seed(
            Reminder(1, "Future pending", 20_000L, ReminderStatus.PENDING),
            Reminder(2, "Past pending", 5_000L, ReminderStatus.PENDING),
            Reminder(3, "Future completed", 20_000L, ReminderStatus.COMPLETED),
            Reminder(4, "Future cancelled", 20_000L, ReminderStatus.CANCELLED),
            Reminder(5, "Another future", 30_000L, ReminderStatus.PENDING)
        )

        useCase()

        assertEquals(listOf(1L, 5L), scheduler.scheduledIds())
    }

    @Test
    fun `scheduling twice does not duplicate alarms for the same id`() = runTest {
        repository.seed(Reminder(1, "Once", 20_000L, ReminderStatus.PENDING))

        useCase()
        useCase()

        assertEquals(listOf(1L), scheduler.scheduledIds())
    }
}

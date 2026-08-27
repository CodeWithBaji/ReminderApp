package com.reminder.app.di

import android.app.AlarmManager
import android.content.Context
import androidx.room.Room
import com.reminder.app.data.local.ReminderDao
import com.reminder.app.data.local.ReminderDatabase
import com.reminder.app.data.repository.ReminderRepositoryImpl
import com.reminder.app.domain.repository.ReminderRepository
import com.reminder.app.domain.time.TimeProvider
import com.reminder.app.scheduler.AlarmManagerReminderScheduler
import com.reminder.app.scheduler.ReminderScheduler
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** Provides Room to Hilt. */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): ReminderDatabase =
        Room.databaseBuilder(context, ReminderDatabase::class.java, "reminders.db")
            .build()

    @Provides
    fun provideReminderDao(database: ReminderDatabase): ReminderDao = database.reminderDao()
}

/** Provides AlarmManager and the production clock. */
@Module
@InstallIn(SingletonComponent::class)
object SystemModule {

    @Provides
    @Singleton
    fun provideAlarmManager(@ApplicationContext context: Context): AlarmManager =
        context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    @Provides
    @Singleton
    fun provideTimeProvider(): TimeProvider = TimeProvider { System.currentTimeMillis() }
}

/** Binds interfaces to the production implementations. */
@Module
@InstallIn(SingletonComponent::class)
abstract class AppBindings {

    @Binds
    @Singleton
    abstract fun bindRepository(impl: ReminderRepositoryImpl): ReminderRepository

    @Binds
    @Singleton
    abstract fun bindScheduler(impl: AlarmManagerReminderScheduler): ReminderScheduler
}

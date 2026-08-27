package com.reminder.app.data.local

import androidx.room.TypeConverter
import com.reminder.app.domain.model.ReminderStatus

/** Stores [ReminderStatus] as a stable string so renaming an ordinal cannot corrupt rows. */
class ReminderConverters {
    @TypeConverter
    fun fromStatus(status: ReminderStatus): String = status.name

    @TypeConverter
    fun toStatus(value: String): ReminderStatus = ReminderStatus.valueOf(value)
}

package com.reminder.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.reminder.app.domain.model.Reminder
import com.reminder.app.domain.model.ReminderStatus

/**
 * Room row. This table is the source of truth for every reminder.
 *
 * Why not store reminders only in AlarmManager?
 * AlarmManager is a best-effort OS timer. It is wiped on reboot, can be dropped by
 * OEMs after force-stop, and does not let us query "all pending reminders".
 * We persist first, then schedule an alarm whose requestCode is [id].
 */
@Entity(tableName = "reminders")
data class ReminderEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val triggerTimeMillis: Long,
    val status: ReminderStatus
)

/** Maps a Room row to the domain model used by use cases and the UI. */
fun ReminderEntity.toDomain(): Reminder = Reminder(
    id = id,
    title = title,
    triggerTimeMillis = triggerTimeMillis,
    status = status
)

/** Maps a domain reminder back to a Room row for updates. */
fun Reminder.toEntity(): ReminderEntity = ReminderEntity(
    id = id,
    title = title,
    triggerTimeMillis = triggerTimeMillis,
    status = status
)

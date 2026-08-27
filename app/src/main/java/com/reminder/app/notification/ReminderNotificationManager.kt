package com.reminder.app.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.reminder.app.MainActivity
import com.reminder.app.R
import com.reminder.app.domain.model.Reminder
import com.reminder.app.receiver.ReminderActionReceiver
import com.reminder.app.receiver.ReminderContract
import com.reminder.app.util.DateTimeUtils
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Builds and posts reminder notifications.
 *
 * Why this class exists: notification channels (API 26+) and runtime notification
 * permission (API 33+) are easy to get wrong if scattered across receivers.
 * Recipients call [show] after confirming the Room row is still PENDING.
 *
 * If POST_NOTIFICATIONS is denied we fail quietly — AlarmManager still fired, but
 * the user opted out of the UI for alerts. The list screen still shows the reminder.
 */
@Singleton
class ReminderNotificationManager @Inject constructor(
    @param:ApplicationContext private val context: Context
) {

    /** Posts the reminder notification. Call only after confirming the row is still PENDING. */
    fun show(reminder: Reminder) {
        ensureChannel()
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(reminder.title)
            .setContentText(DateTimeUtils.format(reminder.triggerTimeMillis))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(openAppIntent(reminder.id))
            .addAction(
                0,
                context.getString(R.string.action_complete),
                actionIntent(ReminderContract.ACTION_COMPLETE, reminder.id, REQUEST_COMPLETE)
            )
            .addAction(
                0,
                context.getString(R.string.action_cancel),
                actionIntent(ReminderContract.ACTION_CANCEL, reminder.id, REQUEST_CANCEL)
            )
            .build()

        try {
            NotificationManagerCompat.from(context).notify(reminder.id.toInt(), notification)
        } catch (securityException: SecurityException) {
            // Android 13+: posting without POST_NOTIFICATIONS throws.
        }
    }

    /** Cancels the notification with id = reminderId (same id used in [show]). */
    fun dismiss(reminderId: Long) {
        NotificationManagerCompat.from(context).cancel(reminderId.toInt())
    }

    /** Creates the notification channel. Harmless to call more than once. */
    fun ensureChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.notification_channel_name),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = context.getString(R.string.notification_channel_description)
        }
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    private fun openAppIntent(reminderId: Long): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        return PendingIntent.getActivity(
            context,
            reminderId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun actionIntent(action: String, reminderId: Long, requestOffset: Int): PendingIntent {
        val intent = Intent(context, ReminderActionReceiver::class.java).apply {
            this.action = action
            putExtra(ReminderContract.EXTRA_REMINDER_ID, reminderId)
        }
        // Offset requestCodes so complete/cancel/content PendingIntents do not collide
        // with each other or with the alarm broadcast (which uses raw reminder.id).
        return PendingIntent.getBroadcast(
            context,
            reminderId.toInt() * 10 + requestOffset,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    companion object {
        private const val CHANNEL_ID = "reminders"
        private const val REQUEST_COMPLETE = 1
        private const val REQUEST_CANCEL = 2
    }
}

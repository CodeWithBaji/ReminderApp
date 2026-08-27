package com.reminder.app.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Date/time helpers for the create form and notification text.
 *
 * Material3 DatePicker reports UTC midnight; AlarmManager needs a local epoch millis.
 * These functions convert between the two without java.time (minSdk 24).
 */
object DateTimeUtils {

    private val displayFormat = SimpleDateFormat("EEE, MMM d • h:mm a", Locale.getDefault())

    /** Formats a local epoch millis for the list and notifications. */
    fun format(millis: Long): String = displayFormat.format(Date(millis))

    fun formatDate(utcDateMillis: Long): String {
        // DatePicker stores UTC midnight. Format in UTC so the calendar day does not
        // shift in timezones behind UTC.
        val format = SimpleDateFormat("EEE, MMM d, yyyy", Locale.getDefault())
        format.timeZone = TimeZone.getTimeZone("UTC")
        return format.format(Date(utcDateMillis))
    }

    fun formatTime(hour: Int, minute: Int): String {
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
        }
        return SimpleDateFormat("h:mm a", Locale.getDefault()).format(cal.time)
    }

    /**
     * Material3 DatePicker gives UTC midnight for the selected calendar date.
     * Combine that date with a local hour/minute to produce a local epoch millis.
     */
    fun combineUtcDateAndLocalTime(dateMillisUtc: Long, hour: Int, minute: Int): Long {
        val utc = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            timeInMillis = dateMillisUtc
        }
        return Calendar.getInstance().apply {
            set(
                utc.get(Calendar.YEAR),
                utc.get(Calendar.MONTH),
                utc.get(Calendar.DAY_OF_MONTH),
                hour,
                minute,
                0
            )
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    /** Inverse of [combineUtcDateAndLocalTime]: local epoch → DatePicker UTC midnight. */
    fun utcDateMillisOf(epochMillis: Long): Long {
        val local = Calendar.getInstance().apply { timeInMillis = epochMillis }
        return Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            clear()
            set(
                local.get(Calendar.YEAR),
                local.get(Calendar.MONTH),
                local.get(Calendar.DAY_OF_MONTH)
            )
        }.timeInMillis
    }
}

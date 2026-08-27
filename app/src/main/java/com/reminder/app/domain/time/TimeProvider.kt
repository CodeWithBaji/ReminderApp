package com.reminder.app.domain.time

/**
 * Clock abstraction so use cases can be unit-tested without sleeping or mocking
 * [System.currentTimeMillis]. Production uses wall-clock time.
 */
fun interface TimeProvider {
    /** Current wall-clock time in epoch milliseconds. */
    fun now(): Long
}

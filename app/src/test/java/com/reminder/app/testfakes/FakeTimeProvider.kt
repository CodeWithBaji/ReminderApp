package com.reminder.app.testfakes

import com.reminder.app.domain.time.TimeProvider

/** Mutable clock so tests can freeze "now" without waiting. */
class FakeTimeProvider(var nowMillis: Long) : TimeProvider {
    override fun now(): Long = nowMillis
}

package com.bydesigninteractive.ant.core.engine

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TickScheduleTest {
    private val ms = 1_000_000L

    @Test
    fun ticksAreDueAtTheInterval() {
        val s = TickSchedule(50 * ms)
        s.start(0)
        assertEquals(0L, s.waitNanos(0))
        s.ticked(10 * ms)
        assertEquals(40 * ms, s.waitNanos(10 * ms))
    }

    @Test
    fun aSlowTickDelaysTheNextWithoutSkipping() {
        val s = TickSchedule(50 * ms)
        s.start(0)
        s.ticked(80 * ms) // the tick overran
        assertTrue(s.waitNanos(80 * ms) < 0) // the next runs at once
        s.ticked(90 * ms)
        assertEquals(10 * ms, s.waitNanos(90 * ms)) // caught up
        assertEquals(0, s.resets)
    }

    @Test
    fun moreThanTwoSecondsBehindResetsTheSchedule() {
        val s = TickSchedule(50 * ms)
        s.start(0)
        s.ticked(2_200 * ms)
        assertEquals(1, s.resets)
        assertEquals(0L, s.waitNanos(2_200 * ms))
    }

    @Test
    fun intervalsFollowTheSpeed() {
        assertEquals(50_000_000L, intervalFor(1f))
        assertEquals(12_500_000L, intervalFor(4f))
    }

    @Test
    fun aNewIntervalRestartsFromNow() {
        val s = TickSchedule(50 * ms)
        s.start(0)
        s.ticked(5 * ms)
        s.setInterval(12_500_000L, 7 * ms)
        assertEquals(0L, s.waitNanos(7 * ms))
        s.ticked(8 * ms)
        assertEquals(11_500_000L, s.waitNanos(8 * ms))
    }
}

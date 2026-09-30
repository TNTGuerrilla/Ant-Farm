package com.bydesigninteractive.ant.core.engine

import kotlin.test.Test
import kotlin.test.assertEquals

class TickStatsTest {
    @Test
    fun summarizesTheLastWindow() {
        val stats = TickStats(windowNanos = 1_000_000_000L)
        // 20 ticks over one second, each taking 2 ms except one of 9 ms
        for (i in 1..20) stats.record(if (i == 7) 9_000_000L else 2_000_000L, i * 50_000_000L)
        val s = stats.summary(1_000_000_000L)
        assertEquals(20f, s.ticksPerSecond, 0.01f)
        assertEquals(9f, s.msPerTickMax, 0.001f)
        assertEquals((19 * 2f + 9f) / 20f, s.msPerTickAvg, 0.001f)
    }

    @Test
    fun oldTicksLeaveTheWindow() {
        val stats = TickStats(windowNanos = 1_000_000_000L)
        stats.record(5_000_000L, 100_000_000L)
        stats.record(1_000_000L, 1_900_000_000L)
        assertEquals(1f, stats.summary(2_000_000_000L).msPerTickMax, 0.001f)
    }
}

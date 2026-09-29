package com.bydesigninteractive.ant.core.stub

import kotlin.test.Test
import kotlin.test.assertEquals

class FrameStatsTest {
    @Test
    fun emptyStatsReadZero() {
        val stats = FrameStats(10)
        assertEquals(0, stats.count)
        assertEquals(0f, stats.percentile(0.5f))
        assertEquals(0f, stats.max())
    }

    @Test
    fun percentilesUseNearestRank() {
        val stats = FrameStats(100)
        for (ms in 1..100) stats.add(ms.toFloat())
        assertEquals(50f, stats.percentile(0.5f))
        assertEquals(99f, stats.percentile(0.99f))
        assertEquals(100f, stats.max())
    }

    @Test
    fun keepsOnlyTheNewestFrames() {
        val stats = FrameStats(3)
        listOf(90f, 1f, 2f, 3f).forEach(stats::add)
        assertEquals(3, stats.count)
        assertEquals(3f, stats.max())
    }

    @Test
    fun countsSlowFrames() {
        val stats = FrameStats(10)
        listOf(16f, 17f, 30f, 45f).forEach(stats::add)
        assertEquals(2, stats.countOver(25f))
    }
}

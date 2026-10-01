package com.bydesigninteractive.ant.core.render.sky

import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DayCycleTest {
    @Test
    fun theFirstFrameIsMidMorning() {
        assertEquals(DayCycle.START, DayCycle.timeOfDay(0f), 1e-6f)
        assertEquals(DayCycle.START, DayCycle.timeOfDay(DayCycle.DAY_SECONDS), 1e-5f)
    }

    @Test
    fun theCycleHasNoJumps() {
        val a = SkyState()
        val b = SkyState()
        var t = 0f
        DayCycle.sky(0f, a)
        while (t < 1f) {
            t += 0.001f
            DayCycle.sky(t % 1f, b)
            for ((x, y) in listOf(a.sunColor to b.sunColor, a.ambient to b.ambient, a.skyTop to b.skyTop, a.skyHorizon to b.skyHorizon, a.fogColor to b.fogColor, a.sunDir to b.sunDir)) {
                for (k in 0 until 3) assertTrue(abs(x[k] - y[k]) < 0.03f, "jump at t=$t")
            }
            assertTrue(abs(a.fogEnd - b.fogEnd) < 5f, "fog jump at t=$t")
            a.copyFrom(b)
        }
    }

    @Test
    fun nightStaysReadable() {
        val noon = DayCycle.sky(0.5f, SkyState()).brightness()
        val night = DayCycle.sky(0.0f, SkyState()).brightness()
        assertTrue(night >= 0.35f * noon, "night $night vs noon $noon")
        assertTrue(night <= 0.5f * noon, "night should still look like night: $night vs $noon")
    }

    @Test
    fun theSunDirectionStaysUnit() {
        val s = SkyState()
        for (i in 0 until 100) {
            DayCycle.sky(i / 100f, s)
            val len = kotlin.math.sqrt(s.sunDir[0] * s.sunDir[0] + s.sunDir[1] * s.sunDir[1] + s.sunDir[2] * s.sunDir[2])
            assertEquals(1f, len, 1e-4f)
        }
    }
}

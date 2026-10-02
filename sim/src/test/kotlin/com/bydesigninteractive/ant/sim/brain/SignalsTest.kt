package com.bydesigninteractive.ant.sim.brain

import com.bydesigninteractive.ant.sim.ant.Ant
import com.bydesigninteractive.ant.sim.ant.Role
import com.bydesigninteractive.ant.sim.ant.Space
import com.bydesigninteractive.ant.sim.world.DayClock
import com.bydesigninteractive.ant.sim.world.GroundTemperature
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SignalsTest {
    @Test
    fun theReturnerRateCountsTheLastSixtySeconds() {
        val r = RateWindow(60)
        r.add(0)
        r.add(10)
        r.add(20 * 30)
        assertEquals(3, r.count(20 * 30))
        assertEquals(3, r.count(20 * 59 + 19))
        assertEquals(1, r.count(20 * 60)) // the two events of second 0 have left the window
        assertEquals(0, r.count(20 * 1000))
    }

    @Test
    fun theDayStartsMidMorningAndTheAfternoonIsWarmest() {
        assertEquals(DayClock.START, DayClock.timeOfDay(0f), 1e-6f)
        assertEquals(DayClock.START, DayClock.timeOfDay(DayClock.DAY_SECONDS), 1e-4f)
        val peak = GroundTemperature.surface(GroundTemperature.PEAK)
        assertTrue(peak > GroundTemperature.surface(0f) + 10f)
        val nestSwing = GroundTemperature.nest(GroundTemperature.PEAK) - GroundTemperature.nest(GroundTemperature.PEAK + 0.5f)
        val surfaceSwing = peak - GroundTemperature.surface(GroundTemperature.PEAK + 0.5f)
        assertEquals(surfaceSwing * GroundTemperature.NEST_DAMPING, nestSwing, 1e-3f)
    }

    @Test
    fun reservesFallOutsideAndRefillInTheNest() {
        val a = Ant(0, Role.FORAGER, 0.5f, true)
        a.space = Space.SURFACE
        repeat(20 * 900) { Body.tick(a) }
        assertEquals(0.65f, a.reserves, 0.01f) // 15 minutes outside: tired
        a.space = Space.NEST
        repeat(20 * 30) { Body.tick(a) }
        assertEquals(1f, a.reserves, 1e-6f)
        // 20 * 900 + 20 * 30 = 18,600 ticks at DT 0.05 is exactly 930 s, derived from the tick count.
        assertEquals(930f, Body.age(20L * 900 + 20L * 30, a), 0.01f)
    }
}

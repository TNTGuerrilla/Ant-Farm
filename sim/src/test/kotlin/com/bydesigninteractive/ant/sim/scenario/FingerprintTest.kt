package com.bydesigninteractive.ant.sim.scenario

import com.bydesigninteractive.ant.sim.TickProfile
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Golden fingerprints: tier 1 cost cuts (M1b-2b) must leave these exactly as they are. Tier 2
 * changes results on purpose and records new values in the same commit.
 */
class FingerprintTest {
    @Test
    fun theStarterRunMatchesItsFingerprint() {
        val w = Scenarios.starter(5)
        repeat(3000) { w.step() }
        assertEquals(STARTER, fingerprint(w))
    }

    @Test
    fun theLargeColonyRunMatchesItsFingerprint() {
        val w = Scenarios.colony1000(3)
        repeat(600) { w.step() }
        assertEquals(COLONY, fingerprint(w))
    }

    @Test
    fun attachingAProfileDoesNotChangeTheRun() {
        val a = Scenarios.starter(5)
        val b = Scenarios.starter(5)
        val profile = TickProfile()
        b.profile = profile
        repeat(1000) {
            a.step()
            b.step()
        }
        assertEquals(fingerprint(a), fingerprint(b))
        assertEquals(1000L, profile.ticks)
        kotlin.test.assertTrue(profile.sdfEvaluations > 0L)
    }

    /** Seed 1's starter run has its first detour at about tick 431 (around a rock by a plant). */
    @Test
    fun theDetourRunMatchesItsFingerprint() {
        val w = Scenarios.starter(1)
        repeat(3000) { w.step() }
        kotlin.test.assertTrue(w.detoursStarted > 0, "no detour in the run")
        assertEquals(DETOUR, fingerprint(w))
    }

    // Re-recorded in M2a Task 8: every ant runs the instinct brain.
    private companion object {
        const val STARTER = "sum=547889.6989145946 trail=4 home=77 trailMax=4.4868712 homeMax=0.8155839 feeds=2 unloads=0 ants=80 rng=36691888687298254"
        const val DETOUR = "sum=552394.0885981531 trail=12 home=74 trailMax=3.6309822 homeMax=0.63593847 feeds=9 unloads=0 ants=80 rng=4980140498390456359"
        const val COLONY = "sum=7185262.319803611 trail=0 home=30 trailMax=0.0 homeMax=2.7777545 feeds=0 unloads=0 ants=1000 rng=-7510369823861576677"
    }
}

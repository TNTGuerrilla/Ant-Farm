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

    /** Seed 1's starter run has its first detour at tick 648 (measured in M2a Task 8b, with the instinct brain). */
    @Test
    fun theDetourRunMatchesItsFingerprint() {
        val w = Scenarios.starter(1)
        repeat(3000) { w.step() }
        kotlin.test.assertTrue(w.detoursStarted > 0, "no detour in the run")
        assertEquals(DETOUR, fingerprint(w))
    }

    // Re-recorded in M2a Task 8 (every ant runs the instinct brain) and 8b (trail following).
    private companion object {
        const val STARTER = "sum=573753.7009821638 trail=5 home=89 trailMax=0.5866279 homeMax=0.8992291 feeds=2 unloads=0 ants=80 rng=8471752498746914134"
        const val DETOUR = "sum=481446.4140669968 trail=12 home=76 trailMax=2.3364508 homeMax=0.6653569 feeds=16 unloads=0 ants=80 rng=-8468741863998431981"
        const val COLONY = "sum=7196387.902465055 trail=0 home=32 trailMax=0.0 homeMax=3.1059964 feeds=0 unloads=0 ants=1000 rng=-6538686098725575125"
    }
}

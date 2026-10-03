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

    /** Seed 1's starter run makes at least one detour within its 3000 ticks, with the instinct brain. */
    @Test
    fun theDetourRunMatchesItsFingerprint() {
        val w = Scenarios.starter(1)
        repeat(3000) { w.step() }
        kotlin.test.assertTrue(w.detoursStarted > 0, "no detour in the run")
        assertEquals(DETOUR, fingerprint(w))
    }

    // Re-recorded in M2a Task 8 (every ant runs the instinct brain) and 8b (trail following), 8c
    // (the exit trail choice as a brain sense, the homeward deposit), 8d (the deposit's
    // saturating cut), and 11a (the exit choice on a trail, feeding places, the NO_ROOM sense).
    // Re-recorded in M2a Task 11: the shipped instinct genome.
    private companion object {
        const val STARTER = "sum=574102.4157291924 trail=8 home=56 trailMax=1.3329965 homeMax=1.0653535 feeds=2 unloads=0 ants=80 rng=-4217683405031910476"
        const val DETOUR = "sum=553058.5936519476 trail=12 home=50 trailMax=1.4501745 homeMax=0.7730925 feeds=7 unloads=0 ants=80 rng=-1516477858088050375"
        const val COLONY = "sum=7200520.756003349 trail=0 home=32 trailMax=0.0 homeMax=3.232771 feeds=0 unloads=0 ants=1000 rng=-8370243337084551823"
    }
}

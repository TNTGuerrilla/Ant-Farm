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
    private companion object {
        const val STARTER = "sum=577432.2736606215 trail=15 home=70 trailMax=0.8331259 homeMax=0.8433443 feeds=4 unloads=0 ants=80 rng=-4273712978007863882"
        const val DETOUR = "sum=529957.1881607191 trail=16 home=82 trailMax=1.7595835 homeMax=0.676645 feeds=9 unloads=0 ants=80 rng=866696344843055294"
        const val COLONY = "sum=7192236.6568238605 trail=0 home=32 trailMax=0.0 homeMax=2.702457 feeds=0 unloads=0 ants=1000 rng=8403816162952503309"
    }
}

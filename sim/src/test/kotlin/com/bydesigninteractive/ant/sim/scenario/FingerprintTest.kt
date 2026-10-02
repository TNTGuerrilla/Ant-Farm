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

    /** Seed 1's starter run has its first detour at tick 476 (measured in M2a Task 8d, with the instinct brain). */
    @Test
    fun theDetourRunMatchesItsFingerprint() {
        val w = Scenarios.starter(1)
        repeat(3000) { w.step() }
        kotlin.test.assertTrue(w.detoursStarted > 0, "no detour in the run")
        assertEquals(DETOUR, fingerprint(w))
    }

    // Re-recorded in M2a Task 8 (every ant runs the instinct brain) and 8b (trail following), 8c
    // (the exit trail choice as a brain sense, the homeward deposit), and 8d (the deposit's
    // saturating cut).
    private companion object {
        const val STARTER = "sum=578529.7944118922 trail=1 home=73 trailMax=0.10524576 homeMax=0.90318245 feeds=2 unloads=0 ants=80 rng=-7086934034764895650"
        const val DETOUR = "sum=527236.6094159535 trail=16 home=89 trailMax=1.8952755 homeMax=0.7395286 feeds=12 unloads=0 ants=80 rng=2558674974469211134"
        const val COLONY = "sum=7196872.810666364 trail=0 home=30 trailMax=0.0 homeMax=2.681898 feeds=0 unloads=0 ants=1000 rng=4266499480178696266"
    }
}

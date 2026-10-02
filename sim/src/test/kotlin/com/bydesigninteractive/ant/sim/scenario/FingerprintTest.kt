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

    // Re-recorded in M2a Task 8 (every ant runs the instinct brain) and 8b (trail following), and 8c
    // (the exit trail choice as a brain sense, the homeward deposit).
    private companion object {
        const val STARTER = "sum=553038.823445081 trail=2 home=66 trailMax=0.5550268 homeMax=0.77703196 feeds=5 unloads=0 ants=80 rng=-4843803057271561394"
        const val DETOUR = "sum=529449.6958413239 trail=15 home=71 trailMax=1.2492471 homeMax=0.7412029 feeds=11 unloads=0 ants=80 rng=6129150620878513864"
        const val COLONY = "sum=7198363.902427793 trail=0 home=32 trailMax=0.0 homeMax=2.7108583 feeds=0 unloads=0 ants=1000 rng=-9208119064235116845"
    }
}

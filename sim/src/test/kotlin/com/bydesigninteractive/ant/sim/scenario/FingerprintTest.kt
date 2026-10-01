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

    // Recorded in M1b-2b Task 8, after the open-ground fast path (tier 2).
    private companion object {
        const val STARTER = "sum=550593.5773532048 trail=5 home=84 trailMax=0.6434383 homeMax=0.77056533 feeds=3 unloads=0 ants=80 rng=7019582404308401067"
        const val COLONY = "sum=7229040.3433115 trail=0 home=34 trailMax=0.0 homeMax=2.4585316 feeds=0 unloads=0 ants=1000 rng=-6800254765937369305"
    }
}

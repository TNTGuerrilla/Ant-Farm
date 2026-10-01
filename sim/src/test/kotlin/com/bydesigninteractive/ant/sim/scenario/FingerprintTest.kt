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

    // Recorded in M1b-2b Task 1 from the code before any cost cut.
    private companion object {
        const val STARTER = "sum=562502.7129050037 trail=5 home=86 trailMax=0.6638047 homeMax=0.7872241 feeds=3 unloads=0 ants=80 rng=6106405437895412435"
        const val COLONY = "sum=7229087.009711433 trail=0 home=34 trailMax=0.0 homeMax=2.458601 feeds=0 unloads=0 ants=1000 rng=-6800254765937369305"
    }
}

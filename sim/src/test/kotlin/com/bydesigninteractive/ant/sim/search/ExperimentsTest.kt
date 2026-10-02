package com.bydesigninteractive.ant.sim.search

import java.util.concurrent.Executors
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ExperimentsTest {
    @Test
    fun perSeedChecksNeedThreeQuarters() {
        assertEquals(1, Experiments.needed(1))
        assertEquals(3, Experiments.needed(4))
        assertEquals(6, Experiments.needed(8))
    }

    @Test
    fun anExperimentPassesWhenEnoughSeedsReachItsThreshold() {
        assertTrue(ExperimentResult("x", listOf(0.9, 0.8, 0.75, 0.1), 0.7).passed)
        assertTrue(!ExperimentResult("x", listOf(0.9, 0.8, 0.1, 0.1), 0.7).passed)
    }

    /** A shortened run of everything the full check uses, on one seed: it completes and gives shares. */
    @Test
    fun shortenedExperimentsRun() {
        val pool = Executors.newFixedThreadPool(4)
        try {
            val results = Experiments.all(null, listOf(1L), pool, scale = 0.05f)
            assertEquals(listOf("distance choice", "equal sources", "source quality", "depletion switch"), results.map { it.name })
            for (r in results) for (v in r.values) assertTrue(v.isFinite() && v >= 0.0, r.line())
            val g = Experiments.gruter(null, listOf(1L), pool, scale = 0.05f)
            assertTrue(g.line().startsWith("Gruter"))
        } finally {
            pool.shutdown()
        }
    }
}

package com.bydesigninteractive.ant.sim.scenario

import com.bydesigninteractive.ant.sim.search.Experiments
import java.util.concurrent.Executors
import org.junit.jupiter.api.Tag
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Gruter et al. 2012 (PLoS ONE): small colonies forage individually; above about 75 foragers a
 * trail forms and the colony locks onto one of two equal sources; with crowding it switches to
 * a richer source offered later (simulation reference sections 2 and 5). Since M2a it runs the
 * brain-driven colony on 8 seeds (the owner's decision of 2026-09-30), in parallel, through
 * Experiments, which the realism search shares. Thresholds are unchanged; per-seed counts keep
 * the same three quarters (6 of 8).
 */
@Tag("gruter")
class GruterScenarioTest {
    private companion object {
        val SEEDS = (1L..8L).toList()
        val result: Experiments.GruterResult by lazy {
            val pool = Executors.newFixedThreadPool(minOf(8, Runtime.getRuntime().availableProcessors()))
            try {
                Experiments.gruter(null, SEEDS, pool).also { println("GRUTER " + it.line()) }
            } finally {
                pool.shutdown()
            }
        }
    }

    @Test
    fun smallColoniesForageAlone() {
        assertTrue(result.smallPasses, "trail following with 30 foragers: ${result.small}")
    }

    @Test
    fun largeColoniesBreakSymmetry() {
        assertTrue(result.largeFollowingPasses, "trail following with 150 foragers: ${result.large}")
        assertTrue(result.largeSharePasses, "busier source share: ${result.large}")
    }

    @Test
    fun crowdedColoniesSwitchToARicherSource() {
        assertTrue(result.crowdedPasses, "loser share after the switch: ${result.crowded}")
    }
}

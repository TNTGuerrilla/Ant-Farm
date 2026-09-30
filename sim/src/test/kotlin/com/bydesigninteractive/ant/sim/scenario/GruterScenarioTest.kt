package com.bydesigninteractive.ant.sim.scenario

import com.bydesigninteractive.ant.sim.World
import com.bydesigninteractive.ant.sim.ant.AntState
import com.bydesigninteractive.ant.sim.ant.Role
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Gruter et al. 2012 (PLoS ONE): small colonies forage individually; above about 75 foragers a
 * trail forms and the colony locks onto one of two equal sources; with crowding it switches to
 * a richer source offered later (simulation reference sections 2 and 5).
 */
class GruterScenarioTest {
    private val seeds = listOf(1L, 2L, 3L, 4L)
    private val ticksPerMinute = 60 * 20

    private fun run(w: World, minutes: Int) = repeat(minutes * ticksPerMinute) { w.step() }

    /** Runs [minutes] more and returns the average share of searching foragers that are on a trail. */
    private fun runMeasuringFollowing(w: World, minutes: Int): Float {
        var sum = 0f
        var samples = 0
        repeat(minutes * ticksPerMinute) {
            w.step()
            if (w.tick % 200 == 0L) {
                val searching = w.ants.filter { it.role == Role.FORAGER && it.state == AntState.SEARCH }
                if (searching.isNotEmpty()) {
                    sum += searching.count { it.onTrail }.toFloat() / searching.size
                    samples++
                }
            }
        }
        return if (samples == 0) 0f else sum / samples
    }

    /** Feeding events per source id over the last [minutes]. */
    private fun feedsSince(w: World, minutes: Int): Map<Int, Int> {
        val since = w.tick - minutes * ticksPerMinute
        return w.feedEvents.filter { it.tick >= since }.groupingBy { it.foodId }.eachCount()
    }

    @Test
    fun smallColoniesForageAlone() {
        val following = seeds.map { seed ->
            val w = Scenarios.gruter(seed, foragers = 30)
            run(w, 20)
            runMeasuringFollowing(w, 10)
        }
        assertTrue(following.average() < 0.15, "trail following with 30 foragers: $following")
    }

    @Test
    fun largeColoniesBreakSymmetry() {
        val results = seeds.map { seed ->
            val w = Scenarios.gruter(seed, foragers = 150)
            run(w, 30)
            val following = runMeasuringFollowing(w, 10)
            val feeds = feedsSince(w, 10)
            val total = feeds.values.sum().coerceAtLeast(1)
            following to (feeds.values.maxOrNull() ?: 0).toFloat() / total
        }
        assertTrue(results.map { it.first }.average() > 0.3, "trail following with 150 foragers: $results")
        assertTrue(results.count { it.second >= 0.7f } >= 3, "busier source share: $results")
    }

    @Test
    fun crowdedColoniesSwitchToARicherSource() {
        val switched = seeds.count { seed ->
            val w = Scenarios.gruter(seed, foragers = 150, quality = 0.6f)
            run(w, 30)
            val feeds = feedsSince(w, 10)
            val loser = if ((feeds[0] ?: 0) <= (feeds[1] ?: 0)) 0 else 1
            w.surface.foods.first { it.id == loser }.quality = 1f
            run(w, 30)
            val after = feedsSince(w, 10)
            (after[loser] ?: 0).toFloat() / after.values.sum().coerceAtLeast(1) >= 0.5f
        }
        assertTrue(switched >= 3, "seeds that switched: $switched of ${seeds.size}")
    }
}

package com.bydesigninteractive.ant.sim.search

import com.bydesigninteractive.ant.sim.World
import com.bydesigninteractive.ant.sim.ant.AntState
import com.bydesigninteractive.ant.sim.ant.Role
import com.bydesigninteractive.ant.sim.brain.Genome
import com.bydesigninteractive.ant.sim.scenario.Scenarios
import java.util.Locale
import java.util.concurrent.Callable
import java.util.concurrent.ExecutorService
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Colony experiments on fresh worlds (M2a): the Gruter 2012 checks of GruterScenarioTest and,
 * from Task 9, four open-ground experiments. Runs are independent, so seeds run in parallel on
 * the caller's pool, with results in seed order. [scale] shortens every phase (to at least one
 * minute) for smoke tests; 1 is the real experiment. A null genome means the instinct brain.
 */
object Experiments {
    const val TICKS_PER_MINUTE = 60 * 20

    // GruterScenarioTest's thresholds, unchanged since M1 (simulation reference sections 2 and 5).
    const val SMALL_MAX = 0.15
    const val LARGE_MIN = 0.3
    const val SHARE_MIN = 0.7
    const val SWITCH_MIN = 0.5

    /** Seeds a per-seed check must pass: three quarters, as the original 3 of 4 (6 of 8). */
    fun needed(seeds: Int): Int = (seeds * 3 + 3) / 4

    /**
     * Following with 30 foragers, (following, busier share) with 150, and the loser's share after
     * the switch, per seed. (Members of Experiments are qualified in here on purpose.)
     */
    class GruterResult(val small: List<Double>, val large: List<Pair<Double, Double>>, val crowded: List<Double>) {
        val smallPasses: Boolean get() = small.average() < Experiments.SMALL_MAX
        val largeFollowingPasses: Boolean get() = large.map { it.first }.average() > Experiments.LARGE_MIN
        val largeSharePasses: Boolean get() = large.count { it.second >= Experiments.SHARE_MIN } >= Experiments.needed(large.size)
        val crowdedPasses: Boolean get() = crowded.count { it >= Experiments.SWITCH_MIN } >= Experiments.needed(crowded.size)
        val passed: Boolean get() = smallPasses && largeFollowingPasses && largeSharePasses && crowdedPasses

        fun line(): String = String.format(
            Locale.ROOT,
            "Gruter %s: 30 foragers following %s (mean %.3f, < %.2f); 150 following %s (mean %.3f, > %.2f), busier share %s (%d at %.2f or more, %d needed); crowded switch %s (%d at %.2f or more, %d needed)",
            if (passed) "pass" else "FAIL",
            Experiments.fmt(small), small.average(), Experiments.SMALL_MAX,
            Experiments.fmt(large.map { it.first }), large.map { it.first }.average(), Experiments.LARGE_MIN,
            Experiments.fmt(large.map { it.second }), large.count { it.second >= Experiments.SHARE_MIN }, Experiments.SHARE_MIN, Experiments.needed(large.size),
            Experiments.fmt(crowded), crowded.count { it >= Experiments.SWITCH_MIN }, Experiments.SWITCH_MIN, Experiments.needed(crowded.size),
        )
    }

    fun gruter(genome: Genome?, seeds: List<Long>, pool: ExecutorService, scale: Float = 1f): GruterResult {
        val small = seeds.map { s -> pool.submit(Callable { gruterSmall(genome, s, scale) }) }
        val large = seeds.map { s -> pool.submit(Callable { gruterLarge(genome, s, scale) }) }
        val crowded = seeds.map { s -> pool.submit(Callable { gruterCrowded(genome, s, scale) }) }
        return GruterResult(small.map { it.get() }, large.map { it.get() }, crowded.map { it.get() })
    }

    /** 30 foragers: 20 minutes, then the mean share of searching foragers on a trail over 10. */
    fun gruterSmall(genome: Genome?, seed: Long, scale: Float = 1f): Double {
        val w = Scenarios.gruter(seed, foragers = 30, genome = genome)
        run(w, minutes(20, scale))
        return following(w, minutes(10, scale))
    }

    /** 150 foragers: 30 minutes, then following over 10 and the busier source's share of their feeds. */
    fun gruterLarge(genome: Genome?, seed: Long, scale: Float = 1f): Pair<Double, Double> {
        val w = Scenarios.gruter(seed, foragers = 150, genome = genome)
        run(w, minutes(30, scale))
        val f = following(w, minutes(10, scale))
        val feeds = feedsSince(w, minutes(10, scale))
        return f to (feeds.values.maxOrNull() ?: 0).toDouble() / feeds.values.sum().coerceAtLeast(1)
    }

    /** 150 foragers on 0.6 sources: 30 minutes, the loser becomes 1.0, 30 more; the loser's share of the last 10 minutes' feeds. */
    fun gruterCrowded(genome: Genome?, seed: Long, scale: Float = 1f): Double {
        val w = Scenarios.gruter(seed, foragers = 150, quality = 0.6f, genome = genome)
        run(w, minutes(30, scale))
        val feeds = feedsSince(w, minutes(10, scale))
        val loser = if ((feeds[0] ?: 0) <= (feeds[1] ?: 0)) 0 else 1
        w.surface.foods.first { it.id == loser }.quality = 1f
        run(w, minutes(30, scale))
        val after = feedsSince(w, minutes(10, scale))
        return (after[loser] ?: 0).toDouble() / after.values.sum().coerceAtLeast(1)
    }

    internal fun minutes(m: Int, scale: Float): Int = max(1, (m * scale).roundToInt())

    internal fun run(w: World, minutes: Int) = repeat(minutes * TICKS_PER_MINUTE) { w.step() }

    /** Runs [minutes] more and returns the mean share of searching foragers on a trail, sampled every 200 ticks. */
    internal fun following(w: World, minutes: Int): Double {
        var sum = 0.0
        var samples = 0
        repeat(minutes * TICKS_PER_MINUTE) {
            w.step()
            if (w.tick % 200 == 0L) {
                var searching = 0
                var on = 0
                for (a in w.ants) {
                    if (a.role != Role.FORAGER || a.state != AntState.SEARCH) continue
                    searching++
                    if (a.onTrail) on++
                }
                if (searching > 0) {
                    sum += on.toDouble() / searching
                    samples++
                }
            }
        }
        return if (samples == 0) 0.0 else sum / samples
    }

    /** Feeding events per source id over the last [minutes]. */
    internal fun feedsSince(w: World, minutes: Int): Map<Int, Int> {
        val since = w.tick - minutes * TICKS_PER_MINUTE
        return w.feedEvents.filter { it.tick >= since }.groupingBy { it.foodId }.eachCount()
    }

    internal fun fmt(values: List<Double>): String = values.joinToString(", ", "[", "]") { String.format(Locale.ROOT, "%.3f", it) }
}

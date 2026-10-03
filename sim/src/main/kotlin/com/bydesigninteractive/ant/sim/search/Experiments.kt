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

/** One experiment over several seeds: it passes when [Experiments.needed] of them reach [threshold]. */
class ExperimentResult(val name: String, val values: List<Double>, val threshold: Double) {
    val passed: Boolean get() = values.count { it >= threshold } >= Experiments.needed(values.size)

    fun line(): String = String.format(
        Locale.ROOT,
        "%s %s: %s (%.2f or more in %d of %d)",
        name, if (passed) "pass" else "FAIL", Experiments.fmt(values), threshold, Experiments.needed(values.size), values.size,
    )
}

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

    /** Gruter 2012's little-crowding feeder (9 holes), for the symmetry-breaking checks. */
    private val LOW_CROWDING_RADIUS = Scenarios.feederRadius(Scenarios.GRUTER_LOW_CROWDING)

    /** Gruter 2012's high-crowding feeder (1 hole), for the switching check. */
    private val HIGH_CROWDING_RADIUS = Scenarios.feederRadius(Scenarios.GRUTER_HIGH_CROWDING)

    /** 30 foragers: 20 minutes, then the mean share of searching foragers on a trail over 10. */
    fun gruterSmall(genome: Genome?, seed: Long, scale: Float = 1f): Double {
        val w = Scenarios.gruter(seed, foragers = 30, genome = genome, feederRadius = LOW_CROWDING_RADIUS)
        run(w, minutes(20, scale))
        return following(w, minutes(10, scale))
    }

    /** 150 foragers: 30 minutes, then following over 10 and the busier source's share of their feeds. */
    fun gruterLarge(genome: Genome?, seed: Long, scale: Float = 1f): Pair<Double, Double> {
        val w = Scenarios.gruter(seed, foragers = 150, genome = genome, feederRadius = LOW_CROWDING_RADIUS)
        run(w, minutes(30, scale))
        val f = following(w, minutes(10, scale))
        val feeds = feedsSince(w, minutes(10, scale))
        return f to (feeds.values.maxOrNull() ?: 0).toDouble() / feeds.values.sum().coerceAtLeast(1)
    }

    /**
     * 150 foragers on 0.6 sources with high crowding: 30 minutes, the loser becomes 1.0, 30 more;
     * the loser's share of the last 10 minutes' feeds.
     */
    fun gruterCrowded(genome: Genome?, seed: Long, scale: Float = 1f): Double {
        val w = Scenarios.gruter(seed, foragers = 150, quality = 0.6f, genome = genome, feederRadius = HIGH_CROWDING_RADIUS)
        run(w, minutes(30, scale))
        val feeds = feedsSince(w, minutes(10, scale))
        val loser = if ((feeds[0] ?: 0) <= (feeds[1] ?: 0)) 0 else 1
        w.surface.foods.first { it.id == loser }.quality = 1f
        run(w, minutes(30, scale))
        val after = feedsSince(w, minutes(10, scale))
        return (after[loser] ?: 0).toDouble() / after.values.sum().coerceAtLeast(1)
    }

    // Pass thresholds of the open-ground experiments (spec 4.3).
    const val DISTANCE_MIN = 0.7 // the near feeder's share of feeds: the colony settles on it
    const val EQUAL_MIN = 0.7 // the busier of two equal feeders: one wins
    const val QUALITY_MIN = 0.85 // the richer feeder's share: about 90% or more
    const val DEPLETION_MIN = 0.5 // feeds at the second source after the first runs out, per feed before
    const val DEPLETION_LOADS = 400

    /** Minutes after the rich source runs out over which the depletion switch is measured (Task 11a: 30, was 10). */
    const val DEPLETION_AFTER = 30

    /** Distance choice, the open-ground double bridge: feeders at 250 and 500 mm; the near one's share of the last 10 minutes' feeds after 30. */
    fun distanceChoice(genome: Genome?, seed: Long, scale: Float = 1f): Double {
        val w = Scenarios.feeders(seed, 150, distanceA = 250f, distanceB = 500f, genome = genome)
        run(w, minutes(30, scale))
        val feeds = feedsSince(w, minutes(10, scale))
        return (feeds[0] ?: 0).toDouble() / feeds.values.sum().coerceAtLeast(1)
    }

    /** Equal sources at 250 mm: the busier one's share of the last 10 minutes' feeds after 30. */
    fun equalSources(genome: Genome?, seed: Long, scale: Float = 1f): Double {
        val w = Scenarios.feeders(seed, 150, distanceA = 250f, distanceB = 250f, genome = genome)
        run(w, minutes(30, scale))
        val feeds = feedsSince(w, minutes(10, scale))
        return (feeds.values.maxOrNull() ?: 0).toDouble() / feeds.values.sum().coerceAtLeast(1)
    }

    /** Source quality: 1.0 against 0.3 at 250 mm; the richer one's share of the last 10 minutes' feeds after 30. */
    fun quality(genome: Genome?, seed: Long, scale: Float = 1f): Double {
        val w = Scenarios.feeders(seed, 150, distanceA = 250f, distanceB = 250f, qualityA = 1f, qualityB = 0.3f, genome = genome)
        run(w, minutes(30, scale))
        val feeds = feedsSince(w, minutes(10, scale))
        return (feeds[0] ?: 0).toDouble() / feeds.values.sum().coerceAtLeast(1)
    }

    /**
     * Depletion switch: a rich feeder with [DEPLETION_LOADS] trips and a poorer one that never
     * runs out, both at 250 mm. After the rich one is gone (within 60 minutes; at the tick of its
     * last feed), the second's feeding rate over the next [DEPLETION_AFTER] minutes per the
     * first's rate over the 10 minutes before; 0 if it never ran out.
     *
     * Task 11a lengthened the window after from 10 minutes to 30 (the owner's decision of
     * 2026-10-02); the setup and the threshold are unchanged. When the rich source runs out the
     * poorer one has no trail: a colony that commits nonlinearly to the better of two sources
     * leaves the poorer one almost unvisited (Beckers 1990; Gruter 2012: one feeder won 11 of 12
     * trials at low crowding), so its foragers must first find it by searching and then build a
     * trail to it from nothing. That takes longer than 10 minutes: in the 2026-10-02 diagnosis
     * the second source still had no trail above threshold 15 minutes after the first ran out,
     * even with shortened give-ups. Comparing rates keeps the measure what it was (feeds after
     * per feed before), only averaged over a longer recovery.
     */
    fun depletion(genome: Genome?, seed: Long, scale: Float = 1f): Double {
        val w = Scenarios.feeders(
            seed, 150, distanceA = 250f, distanceB = 250f, qualityA = 1f, qualityB = 0.5f,
            loadsA = DEPLETION_LOADS, genome = genome,
        )
        val beforeTicks = minutes(10, scale) * TICKS_PER_MINUTE.toLong()
        val afterTicks = minutes(DEPLETION_AFTER, scale) * TICKS_PER_MINUTE.toLong()
        val limit = minutes(60, scale)
        var gone = -1L
        var before = 0
        var m = 0
        while ((gone < 0L && m < limit) || (gone >= 0L && w.tick < gone + afterTicks)) {
            run(w, 1)
            m++
            // The rich feeder went with its last load: the tick of its last feed, not the minute's end.
            // The feeds before are counted now: World keeps only the last FEED_WINDOW_TICKS of them.
            if (gone < 0L && w.surface.foods.none { it.id == 0 }) {
                val g = w.feedEvents.lastOrNull { it.foodId == 0 }?.tick ?: w.tick
                gone = g
                before = w.feedEvents.count { it.foodId == 0 && it.tick >= g - beforeTicks && it.tick < g }
            }
        }
        if (gone < 0L) return 0.0
        val after = w.feedEvents.count { it.foodId == 1 && it.tick >= gone && it.tick < gone + afterTicks }
        return if (before == 0) 0.0 else (after.toDouble() / afterTicks) / (before.toDouble() / beforeTicks)
    }

    /** The four open-ground experiments on every seed, in parallel. */
    fun all(genome: Genome?, seeds: List<Long>, pool: ExecutorService, scale: Float = 1f): List<ExperimentResult> {
        val names = listOf("distance choice", "equal sources", "source quality", "depletion switch")
        val thresholds = listOf(DISTANCE_MIN, EQUAL_MIN, QUALITY_MIN, DEPLETION_MIN)
        val runs = listOf<(Long) -> Double>(
            { s -> distanceChoice(genome, s, scale) },
            { s -> equalSources(genome, s, scale) },
            { s -> quality(genome, s, scale) },
            { s -> depletion(genome, s, scale) },
        )
        val futures = runs.map { f -> seeds.map { s -> pool.submit(Callable { f(s) }) } }
        return names.indices.map { i -> ExperimentResult(names[i], futures[i].map { it.get() }, thresholds[i]) }
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

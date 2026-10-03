package com.bydesigninteractive.ant.sim.search

import com.bydesigninteractive.ant.sim.World
import com.bydesigninteractive.ant.sim.ant.AntState
import com.bydesigninteractive.ant.sim.ant.Role
import com.bydesigninteractive.ant.sim.ant.Space
import com.bydesigninteractive.ant.sim.scenario.Scenarios
import java.util.Locale
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable
import kotlin.test.Test

/**
 * A calibration aid for the seed brain (M2a Task 8b), not a check: it runs the Gruter phases of
 * [Experiments] on a few seeds and prints what the pass or fail hides (following, the busier
 * share, trail marks, the strongest trail, trips and where the searchers are). It runs only when
 * the environment variable ANT_DIAG is set, for example
 * `ANT_DIAG=1 DIAG_SEEDS=1,2,3 DIAG_SCALE=0.5 ./gradlew --no-daemon :sim:test --tests '*GruterDiagnostic*'`.
 * DIAG_RUNS picks the phases: small, large, crowded, and starter (Task 8c: the starter colony for
 * 20 minutes, with its trail marks and strongest trail).
 */
@EnabledIfEnvironmentVariable(named = "ANT_DIAG", matches = ".+")
class GruterDiagnostic {
    private val seeds = (System.getenv("DIAG_SEEDS") ?: "1,2,3").split(',').map { it.trim().toLong() }
    private val scale = (System.getenv("DIAG_SCALE") ?: "0.5").toFloat()
    private val runs = (System.getenv("DIAG_RUNS") ?: "small,large").split(',').map { it.trim() }

    @Test
    fun printTheGruterPhases() {
        val pool = Executors.newFixedThreadPool(minOf(12, Runtime.getRuntime().availableProcessors()))
        try {
            val jobs = ArrayList<java.util.concurrent.Future<String>>()
            for (run in runs) for (seed in seeds) jobs += pool.submit(Callable { phase(run, seed) })
            for (j in jobs) println("DIAG " + j.get())
        } finally {
            pool.shutdown()
        }
    }

    private fun phase(run: String, seed: Long): String {
        val m = Experiments
        return when (run) {
            "small" -> {
                val w = Scenarios.gruter(seed, foragers = 30, feederRadius = Scenarios.feederRadius(Scenarios.GRUTER_LOW_CROWDING))
                m.run(w, m.minutes(20, scale))
                val s = Sampler(w)
                s.run(m.minutes(10, scale))
                "small seed $seed: " + s.line() + "; " + s.profile()
            }
            "large" -> {
                val w = Scenarios.gruter(seed, foragers = 150, feederRadius = Scenarios.feederRadius(Scenarios.GRUTER_LOW_CROWDING))
                m.run(w, m.minutes(30, scale))
                val s = Sampler(w)
                s.run(m.minutes(10, scale))
                val feeds = m.feedsSince(w, m.minutes(10, scale))
                val share = (feeds.values.maxOrNull() ?: 0).toDouble() / feeds.values.sum().coerceAtLeast(1)
                String.format(Locale.ROOT, "large seed %d: share %.3f feeds %s; ", seed, share, feeds.toSortedMap()) + s.line() + "; " + s.profile()
            }
            "crowded" -> {
                val w = Scenarios.gruter(seed, foragers = 150, quality = 0.6f, feederRadius = Scenarios.feederRadius(Scenarios.GRUTER_HIGH_CROWDING))
                m.run(w, m.minutes(30, scale))
                val before = m.feedsSince(w, m.minutes(10, scale))
                val loser = if ((before[0] ?: 0) <= (before[1] ?: 0)) 0 else 1
                w.surface.foods.first { it.id == loser }.quality = 1f
                m.run(w, m.minutes(30, scale))
                val after = m.feedsSince(w, m.minutes(10, scale))
                val share = (after[loser] ?: 0).toDouble() / after.values.sum().coerceAtLeast(1)
                String.format(
                    Locale.ROOT, "crowded seed %d: loser %d share %.3f before %s after %s marks %d trailMax %.2f",
                    seed, loser, share, before.toSortedMap(), after.toSortedMap(), w.trailMarks, w.surface.trail.max(),
                )
            }
            "starter" -> {
                // Task 8c: the colony the owner watches on the TV; 20 simulated minutes (times the scale).
                val w = Scenarios.starter(seed)
                m.run(w, m.minutes(10, scale))
                val s = Sampler(w)
                s.run(m.minutes(10, scale))
                String.format(Locale.ROOT, "starter seed %d: total marks %d trailMax %.2f trail blocks %d; ", seed, w.trailMarks, w.surface.trail.max(), w.surface.trail.blockCount()) +
                    s.line()
            }
            else -> error("unknown run $run")
        }
    }

    /** Runs a world on and samples, every 200 ticks, the same following share as [Experiments.following] plus context. */
    private class Sampler(val w: World) {
        var samples = 0
        var following = 0.0
        var searching = 0.0
        var outside = 0.0
        var returning = 0.0
        var outward = 0.0
        val marks0 = w.trailMarks
        val feeds0 = w.feedEvents.size
        val unloads0 = w.unloads
        val was = BooleanArray(w.ants.size)
        val wasOut = BooleanArray(w.ants.size) { w.ants[it].space == Space.SURFACE }
        val wasFeeding = BooleanArray(w.ants.size)
        val exitTick = LongArray(w.ants.size) { -1L }
        val found = BooleanArray(w.ants.size)
        var exits = 0
        var foundSoon = 0
        var trailFeeds = 0
        var allFeeds = 0
        var entries = 0
        var followTicks = 0L
        var near = 0.0
        var touch = 0.0
        var dist = 0.0
        val depSum = DoubleArray(3)
        val depN = IntArray(3)

        fun run(minutes: Int) {
            repeat(minutes * Experiments.TICKS_PER_MINUTE) {
                w.step()
                for ((i, a) in w.ants.withIndex()) {
                    if (a.role != Role.FORAGER) continue
                    val out = a.space == Space.SURFACE
                    if (out && !wasOut[i]) {
                        exits++
                        exitTick[i] = w.tick
                        found[i] = false
                    }
                    wasOut[i] = out
                    if (a.onTrail) {
                        followTicks++
                        if (!was[i]) entries++
                        if (!found[i] && exitTick[i] >= 0 && w.tick - exitTick[i] <= 30 * 20) {
                            found[i] = true
                            foundSoon++
                        }
                    }
                    val feeding = a.state == AntState.FEED
                    if (feeding && !wasFeeding[i]) {
                        allFeeds++
                        if (was[i]) trailFeeds++
                    }
                    wasFeeding[i] = feeding
                    was[i] = a.onTrail
                }
                if (w.tick % 200 == 0L) sample()
            }
        }

        private fun sample() {
            var s = 0
            var on = 0
            var out = 0
            var ret = 0
            var away = 0
            for (a in w.ants) {
                if (a.role != Role.FORAGER) continue
                if (a.space == Space.SURFACE) out++
                if (a.state == AntState.RETURN) ret++
                if (a.state == AntState.RETURN && a.crop > 0f && a.space == Space.SURFACE) {
                    val d = kotlin.math.hypot(a.x - w.surface.entranceX, a.y - w.surface.entranceY)
                    val bin = if (d < 50f) 0 else if (d < 150f) 1 else 2
                    depSum[bin] += a.deposit
                    depN[bin]++
                }
                if (a.state != AntState.SEARCH) continue
                s++
                val r = a.senseL + a.senseR
                val t = w.params.trailThreshold
                if (r >= t) near++
                if (r >= 0.3f * t) touch++
                dist += kotlin.math.hypot(a.x - w.surface.entranceX, a.y - w.surface.entranceY)
                if (a.onTrail) {
                    on++
                    // A follower heading away from home: its forward vector points along the home vector's negative.
                    if (a.fx * a.homeDx + a.fy * a.homeDy > 0f) away++
                }
            }
            if (s > 0) {
                following += on.toDouble() / s
                samples++
            }
            searching += s
            outside += out
            returning += ret
            outward += if (on > 0) away.toDouble() / on else 0.0
        }

        /** The strongest trail across the axis (y within 20 mm) at each distance from the entrance, west / east. */
        fun profile(): String {
            val sf = w.surface
            val sb = StringBuilder("profile")
            for (d in floatArrayOf(8f, 15f, 25f, 50f, 100f, 150f, 200f, 240f)) {
                sb.append(' ').append(d.toInt()).append(':')
                for (sign in floatArrayOf(-1f, 1f)) {
                    var best = 0f
                    val x = sf.entranceX + sign * d
                    for (k in -20..20) {
                        val y = sf.entranceY + k
                        best = maxOf(best, sf.trail.peek(x, y, sf.ground.height(x, y)))
                    }
                    sb.append(String.format(Locale.ROOT, "%.1f", best)).append(if (sign < 0) "/" else "")
                }
            }
            sb.append(String.format(Locale.ROOT, " deposit near %.2f mid %.2f far %.2f",
                depSum[0] / depN[0].coerceAtLeast(1), depSum[1] / depN[1].coerceAtLeast(1), depSum[2] / depN[2].coerceAtLeast(1)))
            return sb.toString()
        }

        fun line(): String {
            val n = samples.coerceAtLeast(1)
            val sn = searching.coerceAtLeast(1.0)
            return String.format(
                Locale.ROOT,
                "exits %d onTrailWithin30s %d feedsFromTrail %d/%d; entries %d episode %.1fs readT %.3f read0.3T %.3f searchDist %.0f; ",
                exits, foundSoon, trailFeeds, allFeeds, entries, followTicks / 20.0 / entries.coerceAtLeast(1), near / sn, touch / sn, dist / sn,
            ) + String.format(
                Locale.ROOT,
                "following %.3f searching %.1f outside %.1f returning %.1f followersOutward %.2f marks %d feeds %d unloads %d trailMax %.2f",
                following / n, searching / n, outside / n, returning / n, outward / n,
                w.trailMarks - marks0, w.feedEvents.size - feeds0, w.unloads - unloads0, w.surface.trail.max(),
            )
        }
    }
}

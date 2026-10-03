package com.bydesigninteractive.ant.sim.search

import com.bydesigninteractive.ant.sim.World
import com.bydesigninteractive.ant.sim.ant.AntParams
import com.bydesigninteractive.ant.sim.ant.AntState
import com.bydesigninteractive.ant.sim.ant.Role
import com.bydesigninteractive.ant.sim.ant.Space
import com.bydesigninteractive.ant.sim.brain.Brain
import com.bydesigninteractive.ant.sim.brain.Genome
import com.bydesigninteractive.ant.sim.brain.Mirror
import com.bydesigninteractive.ant.sim.brain.Outputs
import com.bydesigninteractive.ant.sim.brain.SeedBrain
import com.bydesigninteractive.ant.sim.brain.Senses
import com.bydesigninteractive.ant.sim.scenario.Scenarios
import java.nio.file.Paths
import java.util.Locale
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.ln
import kotlin.test.Test

/**
 * The brain diagnosis of 2026-10-02 and M2a Task 11a, kept as a calibration aid, not a check. It
 * runs only with ANT_DIAG set, for example
 * `ANT_DIAG=1 BD_RUNS=gruter,all BD_SEEDS=1,2,3,4 ./gradlew --no-daemon :sim:test --rerun --tests '*BrainDiagnosis*'`
 * (--rerun because environment variables are not Gradle inputs).
 * BD_RUNS picks parts: diff (weights against the seed), exit (the two-trail exit curve), large,
 * depl and crowded (per-minute time series), occlarge, occcrowded and occdepl (feeder occupancy
 * per minute), gruter, all (the four experiments), depletion and equal.
 * BD_GENOMES picks genomes: seed, g4, g6, g7 (the first search's genomes in build/search) or
 * `proto:<part>+<part>` (see [Proto]). BD_SEEDS and BD_SCALE as in Experiments; BD_PLACES
 * overrides the feeding places of the Gruter occupancy runs.
 */
@EnabledIfEnvironmentVariable(named = "ANT_DIAG", matches = ".+")
class BrainDiagnosis {
    private val runs = (System.getenv("BD_RUNS") ?: "diff").split(',').map { it.trim() }
    private val genomes = (System.getenv("BD_GENOMES") ?: "seed").split(',').map { it.trim() }
    private val seeds = (System.getenv("BD_SEEDS") ?: "1,2,3").split(',').map { it.trim().toLong() }
    private val scale = (System.getenv("BD_SCALE") ?: "1").toFloat()
    private val placesEnv = System.getenv("BD_PLACES")?.toInt()

    private fun genome(name: String): Genome = when (name) {
        "seed" -> SeedBrain.genome()
        "g4" -> Genome.load(Paths.get("../build/search/gen-4-best.genome"))
        "g6" -> Genome.load(Paths.get("../build/search/gen-6-best.genome"))
        "g7" -> Genome.load(Paths.get("../build/search/gen-7-best.genome"))
        else -> Proto.genome(name)
    }

    @Test
    fun diagnose() {
        val pool = Executors.newFixedThreadPool(14)
        try {
            for (run in runs) {
                when (run) {
                    "diff" -> for (g in genomes) diff(g)
                    "exit" -> exitCurve(pool)
                    "exitsides" -> exitSides(pool)
                    "occlarge", "occcrowded", "occdepl" -> {
                        val jobs = genomes.flatMap { g -> seeds.map { s -> pool.submit(Callable { occupancy(run, g, s) }) } }
                        for (j in jobs) println(j.get())
                    }
                    "large", "depl", "crowded" -> {
                        val jobs = genomes.flatMap { g -> seeds.map { s -> pool.submit(Callable { series(run, g, s) }) } }
                        for (j in jobs) println(j.get())
                    }
                    "gruter" -> for (g in genomes) {
                        val r = Experiments.gruter(genome(g), seeds, pool, scale)
                        println("BD gruter $g: " + r.line())
                    }
                    "all" -> for (g in genomes) {
                        for (r in Experiments.all(genome(g), seeds, pool, scale)) println("BD exp $g: " + r.line())
                    }
                    "depletion" -> for (g in genomes) {
                        val gn = genome(g)
                        val f = seeds.map { s -> pool.submit(Callable { Experiments.depletion(gn, s, scale) }) }
                        println("BD depletion $g: " + Experiments.fmt(f.map { it.get() }))
                    }
                    "equal" -> for (g in genomes) {
                        val gn = genome(g)
                        val f = seeds.map { s -> pool.submit(Callable { Experiments.equalSources(gn, s, scale) }) }
                        println("BD equal $g: " + Experiments.fmt(f.map { it.get() }))
                    }
                    else -> error("unknown run $run")
                }
            }
        } finally {
            pool.shutdown()
        }
    }

    // ---------------------------------------------------------------- genome diff

    private fun weightName(g: Genome, k: Int): String {
        val ni = g.inputs
        val nh = g.hidden
        val no = g.outputs
        val unit = arrayOf(
            "SEARCH", "HOME", "CARRY", "TRAIL_ON", "ODOUR", "NOISE", "STEER", "STEER'", "WANDER", "WANDER'",
            "FOOT", "FOOT'", "HOMING", "HOMING'", "AWAY", "AWAY'", "DROP", "DEPOSIT", "GO", "RING", "RING_TURN", "RING_TURN'", "CUT",
        )
        val outs = arrayOf("turn", "speed", "deposit", "goOut") + com.bydesigninteractive.ant.sim.brain.Action.ALL.map { "act." + it.name }
        val rec = Brain.recurrentWeight(ni, nh, 0, 0)
        val hb = Brain.hiddenBias(ni, nh, 0)
        val ow = Brain.outputWeight(ni, nh, 0, 0)
        val ob = Brain.outputBias(ni, nh, no, 0)
        return when {
            k < rec -> "in ${unit[k / ni]} <- ${Senses.NAMES[k % ni]}"
            k < hb -> "rec ${unit[(k - rec) / nh]} <- ${unit[(k - rec) % nh]}"
            k < ow -> "bias ${unit[k - hb]}"
            k < ob -> "out ${outs[(k - ow) / nh]} <- ${unit[(k - ow) % nh]}"
            else -> "obias ${outs[k - ob]}"
        }
    }

    private fun diff(name: String) {
        val seed = SeedBrain.genome()
        val g = genome(name)
        val m = Mirror.seed()
        val rows = ArrayList<Triple<Int, Float, Float>>()
        for (k in seed.weights.indices) {
            val p = m.partner[k]
            if (p < k) continue // each tie once
            val d = g.weights[k] - seed.weights[k]
            rows += Triple(k, seed.weights[k], d)
        }
        rows.sortByDescending { abs(it.third) }
        val total = rows.sumOf { (it.third * it.third).toDouble() }
        println(String.format(Locale.ROOT, "BD diff %s: rms change %.3f over %d tied groups; top 30:", name, kotlin.math.sqrt(total / rows.size), rows.size))
        for ((k, s, d) in rows.take(30)) {
            val tied = if (m.partner[k] != k) " (pair)" else ""
            println(String.format(Locale.ROOT, "BD diff %s   %-40s seed %8.3f  delta %+7.3f%s", name, weightName(seed, k), s, d, tied))
        }
        // Per hidden unit and per output: summed |delta| over its incoming weights.
        val byTarget = HashMap<String, Double>()
        for ((k, _, d) in rows) {
            val n = weightName(seed, k)
            val target = n.substringBefore(" <-").removePrefix("in ").removePrefix("rec ").removePrefix("bias ").removePrefix("out ").removePrefix("obias ")
            val key = if (n.startsWith("out") || n.startsWith("obias")) "output $target" else "unit $target"
            byTarget[key] = (byTarget[key] ?: 0.0) + abs(d)
        }
        println("BD diff $name by target: " + byTarget.entries.sortedByDescending { it.value }.take(14).joinToString { String.format(Locale.ROOT, "%s %.2f", it.key, it.value) })
    }

    // ---------------------------------------------------------------- exit choice curve

    /**
     * Fixed trails (no decay, no food) west and east of the entrance at given strengths (in units
     * of T at the trail's centre line), all foragers released at once; each forager's side when it
     * first reaches 80 mm from the entrance.
     */
    private fun exitCurve(pool: java.util.concurrent.ExecutorService) {
        val cases = listOf(0f to 0f, 1f to 1f, 2f to 1f, 4f to 1f, 4f to 2f, 8f to 2f, 8f to 4f, 16f to 4f, 1f to 0f, 3f to 1.5f, 1.5f to 1f)
        for (g in genomes) {
            val gn = genome(g)
            val jobs = cases.map { (a, b) -> pool.submit(Callable { seeds.map { s -> exitRun(gn, s, a, b) } }) }
            for ((i, j) in jobs.withIndex()) {
                val r = j.get()
                val west = r.sumOf { it[0] }
                val east = r.sumOf { it[1] }
                val other = r.sumOf { it[2] }
                val onTrail = r.sumOf { it[3] }
                val n = west + east + other
                val (a, b) = cases[i]
                val nEff = if (a != b && b > 0f && west > 0 && east > 0) ln(west.toDouble() / east) / ln((a / b).toDouble()) else Double.NaN
                println(String.format(
                    Locale.ROOT, "BD exit %s W %.1fT E %.1fT: west %.3f east %.3f other %.3f (n %d) onTrailAt80 %.3f  W:E %.2f  n_eff %.2f",
                    g, a, b, west.toDouble() / n, east.toDouble() / n, other.toDouble() / n, n, onTrail.toDouble() / n,
                    west.toDouble() / east.coerceAtLeast(1), nEff,
                ))
            }
        }
    }

    /** Per seed, the 4 : 1 and 2 : 1 exit counts with the stronger trail west and then east (stronger, weaker, neither). */
    private fun exitSides(pool: java.util.concurrent.ExecutorService) {
        val g = genome(genomes.first())
        for ((hi, lo) in listOf(4f to 1f, 2f to 1f)) {
            val jobs = seeds.map { s -> pool.submit(Callable { exitRun(g, s, hi, lo) to exitRun(g, s, lo, hi) }) }
            for ((i, j) in jobs.withIndex()) {
                val (wst, est) = j.get()
                println(String.format(
                    Locale.ROOT, "BD exitsides %.0f:%.0f seed %d: west-strong %d/%d/%d east-strong %d/%d/%d",
                    hi, lo, seeds[i], wst[0], wst[1], wst[2], est[1], est[0], est[2],
                ))
            }
        }
    }

    private fun exitRun(g: Genome, seed: Long, westT: Float, eastT: Float): IntArray {
        val w = World(seed, AntParams(trailDecay = 0f), rocks = false, genome = g)
        w.predig(5)
        val s = w.surface
        val t = w.params.trailThreshold
        layLine(w, -1f, westT * t)
        layLine(w, 1f, eastT * t)
        repeat(150) { w.addAnt(Role.FORAGER) }
        val side = IntArray(w.ants.size) { -1 }
        val res = IntArray(4)
        repeat(3 * Experiments.TICKS_PER_MINUTE) {
            w.step()
            for ((i, a) in w.ants.withIndex()) {
                if (side[i] >= 0 || a.space != Space.SURFACE) continue
                val dx = a.x - s.entranceX
                val dy = a.y - s.entranceY
                if (hypot(dx, dy) < 80f) continue
                side[i] = if (abs(dy) < abs(dx) * 0.5f) (if (dx < 0) 0 else 1) else 2
                res[side[i]]++
                if (a.onTrail) res[3]++
            }
        }
        return res
    }

    /** A straight trail along the x axis on one side of the entrance, reading [value] on its centre line. */
    private fun layLine(w: World, sign: Float, value: Float) {
        if (value <= 0f) return
        val s = w.surface
        val f = s.trail
        // Unit pattern, then scale to the target by linearity.
        fun lay(amount: Float) {
            var d = 6f
            while (d <= 300f) {
                val x = s.entranceX + sign * d
                f.add(x, s.entranceY, s.ground.height(x, s.entranceY), amount)
                d += 2f
            }
        }
        lay(1f)
        val x = s.entranceX + sign * 150f
        val probe = f.get(x, s.entranceY, s.ground.height(x, s.entranceY))
        lay(value / probe - 1f)
    }

    // ---------------------------------------------------------------- feeder occupancy (Task 11a)

    /** Per minute: feeds, mean and peak feeders, blocked arrivals (at a full source, not feeding) and searchers within 40 mm, per source. */
    private fun occupancy(run: String, gName: String, seed: Long): String {
        val g = genome(gName)
        val places = placesEnv ?: if (run == "occlarge") Scenarios.GRUTER_LOW_CROWDING else Scenarios.GRUTER_HIGH_CROWDING
        val w = when (run) {
            "occlarge" -> Scenarios.gruter(seed, foragers = 150, genome = g, places = places)
            "occcrowded" -> Scenarios.gruter(seed, foragers = 150, quality = 0.6f, genome = g, places = places)
            else -> Scenarios.feeders(seed, 150, 250f, 250f, qualityA = 1f, qualityB = 0.5f, loadsA = Experiments.DEPLETION_LOADS, genome = g)
        }
        val s = w.surface
        val foods = s.foods.toList()
        val sb = StringBuilder("BD occ $run $gName places $places seed $seed capacity ${foods.map { it.capacity }}\n")
        sb.append("BD   min | feeds A B | mean feeders A B | peak A B | blocked A B | near A B\n")
        val total = if (run == "occlarge") 40 else 70
        for (minute in 1..total) {
            val feeds = IntArray(2)
            val mean = DoubleArray(2)
            val peak = IntArray(2)
            val blocked = DoubleArray(2)
            val near = DoubleArray(2)
            var samples = 0
            val last = w.feedEvents.lastOrNull()?.tick ?: -1L
            repeat(Experiments.TICKS_PER_MINUTE) {
                w.step()
                if (w.tick % 20 == 0L) {
                    samples++
                    for ((k, f) in foods.withIndex()) {
                        mean[k] += f.feeders
                        peak[k] = maxOf(peak[k], f.feeders)
                    }
                    for (a in w.ants) {
                        if (a.space != Space.SURFACE || a.role != Role.FORAGER || a.crop > 0f) continue
                        for ((k, f) in foods.withIndex()) {
                            if (hypot(a.x - f.x, a.y - f.y) < 40f && a.action != com.bydesigninteractive.ant.sim.brain.Action.FEED) near[k]++
                            if (a.food === f && f.full && a.action != com.bydesigninteractive.ant.sim.brain.Action.FEED) blocked[k]++
                        }
                    }
                }
            }
            for (e in w.feedEvents) if (e.tick > last && e.foodId in 0..1) feeds[e.foodId]++
            if (run == "occcrowded" && minute == 30) {
                val before = Experiments.feedsSince(w, 10)
                val loser = if ((before[0] ?: 0) <= (before[1] ?: 0)) 0 else 1
                s.foods.first { it.id == loser }.quality = 1f
                sb.append("BD   -- loser $loser set to 1.0\n")
            }
            val n = samples.coerceAtLeast(1)
            sb.append(String.format(
                Locale.ROOT, "BD   %3d | %3d %3d | %5.1f %5.1f | %3d %3d | %5.1f %5.1f | %5.1f %5.1f%s\n",
                minute, feeds[0], feeds[1], mean[0] / n, mean[1] / n, peak[0], peak[1], blocked[0] / n, blocked[1] / n,
                near[0] / n, near[1] / n, if (run == "occdepl" && s.foods.none { it.id == 0 } && foods[0].feeders >= 0) " (A gone)" else "",
            ))
        }
        return sb.toString()
    }

    // ---------------------------------------------------------------- time series

    private fun series(run: String, gName: String, seed: Long): String {
        val g = genome(gName)
        val w = when (run) {
            "large" -> Scenarios.gruter(seed, foragers = 150, genome = g, places = Scenarios.GRUTER_LOW_CROWDING)
            "crowded" -> Scenarios.gruter(seed, foragers = 150, quality = 0.6f, genome = g, places = Scenarios.GRUTER_HIGH_CROWDING)
            else -> Scenarios.feeders(seed, 150, 250f, 250f, qualityA = 1f, qualityB = 0.5f, loadsA = Experiments.DEPLETION_LOADS, genome = g)
        }
        val sb = StringBuilder()
        sb.append("BD series $run $gName seed $seed\n")
        sb.append("BD   min | feedsW feedsE | exits W/E/O onTr | marks W/E | trail W 30/100/175/230 | E 30/100/175/230 (T) | srch W/near/E fol | giveups emptyHome | nearA srch meanRes\n")
        val s = w.surface
        val t = w.params.trailThreshold
        val n = w.ants.size
        val wasOut = BooleanArray(n)
        val side = IntArray(n) { 3 }
        val marks = IntArray(n)
        val foodA = s.foods.first { it.id == 0 }
        val ax = foodA.x
        val ay = foodA.y
        val total = when (run) {
            "large" -> 40
            "crowded" -> 60
            else -> 70
        }
        var goneMin = -1
        var loser = -1
        for (minute in 1..total) {
            val feeds0 = IntArray(2)
            val exits = IntArray(4)
            var exitOnTrail = 0
            val mk = IntArray(2)
            var giveups = 0
            var srchW = 0.0
            var srchN = 0.0
            var srchE = 0.0
            var fol = 0.0
            var nearA = 0.0
            var res = 0.0
            var resN = 0
            var samples = 0
            val f0 = w.feedEvents.size
            val lastTick0 = w.feedEvents.lastOrNull()?.tick ?: -1L
            repeat(Experiments.TICKS_PER_MINUTE) {
                w.step()
                for ((i, a) in w.ants.withIndex()) {
                    val out = a.space == Space.SURFACE
                    if (out && !wasOut[i]) side[i] = -1
                    if (!out && wasOut[i] && a.crop <= 0f) giveups++
                    wasOut[i] = out
                    if (out && side[i] < 0) {
                        val dx = a.x - s.entranceX
                        val dy = a.y - s.entranceY
                        if (hypot(dx, dy) >= 80f) {
                            side[i] = if (abs(dy) < abs(dx) * 0.5f) (if (dx < 0) 0 else 1) else 2
                            exits[side[i]]++
                            if (a.onTrail) exitOnTrail++
                        }
                    }
                    if (a.marksLaid != marks[i]) {
                        mk[if (a.x < s.entranceX) 0 else 1] += a.marksLaid - marks[i]
                        marks[i] = a.marksLaid
                    }
                }
                if (w.tick % 200 == 0L) {
                    samples++
                    var sw = 0
                    var sn = 0
                    var se = 0
                    var on = 0
                    var na = 0
                    for (a in w.ants) {
                        if (a.space == Space.SURFACE) {
                            res += a.reserves
                            resN++
                        }
                        if (a.state != AntState.SEARCH || a.role != Role.FORAGER) continue
                        val dx = a.x - s.entranceX
                        when {
                            dx < -60f -> sw++
                            dx > 60f -> se++
                            else -> sn++
                        }
                        if (a.onTrail) on++
                        if (hypot(a.x - ax, a.y - ay) < 60f) na++
                    }
                    srchW += sw
                    srchN += sn
                    srchE += se
                    fol += if (sw + sn + se > 0) on.toDouble() / (sw + sn + se) else 0.0
                    nearA += na
                }
            }
            for (e in w.feedEvents) if (e.tick > lastTick0 && e.foodId in 0..1) feeds0[e.foodId]++
            if (run == "depl" && goneMin < 0 && s.foods.none { it.id == 0 }) goneMin = minute
            if (run == "crowded" && minute == 30) {
                val before = Experiments.feedsSince(w, 10)
                loser = if ((before[0] ?: 0) <= (before[1] ?: 0)) 0 else 1
                s.foods.first { it.id == loser }.quality = 1f
                sb.append("BD   -- loser $loser set to 1.0\n")
            }
            fun prof(sign: Float) = floatArrayOf(30f, 100f, 175f, 230f).joinToString("/") { d ->
                var best = 0f
                val x = s.entranceX + sign * d
                for (k in -25..25) best = maxOf(best, s.trail.peek(x, s.entranceY + k, s.ground.height(x, s.entranceY + k)))
                String.format(Locale.ROOT, "%.1f", best / t)
            }
            val sm = samples.coerceAtLeast(1)
            sb.append(String.format(
                Locale.ROOT, "BD   %3d | %4d %4d | %3d/%3d/%3d %.2f | %4d/%4d | %s | %s | %5.1f/%5.1f/%5.1f %.2f | %3d | %5.1f %.2f%s\n",
                minute, feeds0[0], feeds0[1], exits[0], exits[1], exits[2],
                exitOnTrail.toDouble() / (exits[0] + exits[1] + exits[2]).coerceAtLeast(1),
                mk[0], mk[1], prof(-1f), prof(1f), srchW / sm, srchN / sm, srchE / sm, fol / sm, giveups, nearA / sm,
                res / resN.coerceAtLeast(1), if (goneMin == minute) "  <- A gone" else "",
            ))
            if (run == "depl" && goneMin > 0 && minute >= goneMin + 15) break
            @Suppress("UNUSED_VARIABLE") val unused = f0
        }
        return sb.toString()
    }
}

/**
 * Prototype genome changes of the 2026-10-02 diagnosis, applied to the current seed brain. They
 * were written against the seed at 0973877: `ringon1.5` is now the seed itself (Task 11a), so
 * `ringon` on today's seed would open the exit pair's gate a second time.
 */
internal object Proto {
    fun genome(name: String): Genome {
        val g = SeedBrain.genome()
        val w = g.weights.copyOf()
        // Variants are composed with '+': e.g. "proto:a+b".
        for (part in name.removePrefix("proto").removePrefix(":").split('+')) if (part.isNotEmpty()) apply(w, part)
        return Genome(g.inputs, g.hidden, g.outputs, w, g.layout)
    }

    private fun apply(w: FloatArray, part: String) {
        val u = SeedBrain
        when {
            // Give up an unsuccessful search sooner: SEARCH and HOME switch at reserves R instead
            // of 0.65 (Body.TIRED). R 0.88 is about 5 minutes outside after a full refill.
            // UNMEASURED. A shipped version must move Body.TIRED with it (display state, nudge, detour).
            part.startsWith("giveup") -> {
                val r = part.removePrefix("giveup").toFloatOrNull() ?: 0.88f
                w[inW(u.U_SEARCH, Senses.BIAS)] = -80f * r
                w[inW(u.U_HOME, Senses.BIAS)] = 80f * r
            }
            // Noisy following (Perna 2012: Weber steering plus directional noise gives the
            // Deneubourg sigmoid): the wander noise is only partly shut on a trail (gate -2a
            // instead of -16), so weak trails are lost more often than strong ones once the steer
            // term is concentration dependent (see "steerconc"). UNMEASURED.
            part.startsWith("noisytrail") -> {
                val a = part.removePrefix("noisytrail").toFloatOrNull() ?: 0.75f
                for (unit in intArrayOf(u.U_WANDER, u.U_WANDER_MIRROR)) {
                    w[recW(unit, u.U_TRAIL_ON)] = -a
                    w[biasW(unit)] = -16f - a
                }
            }
            // Concentration-dependent steering: the pair also reads TRAIL_L - TRAIL_R (each
            // s / (s + T)), which grows with the trail's strength, on top of the Weber term. UNMEASURED.
            part.startsWith("steerconc") -> {
                val k = part.removePrefix("steerconc").toFloatOrNull() ?: 3f
                w[inW(u.U_STEER, Senses.TRAIL_L)] += k
                w[inW(u.U_STEER, Senses.TRAIL_R)] -= k
                w[inW(u.U_STEER_MIRROR, Senses.TRAIL_L)] -= k
                w[inW(u.U_STEER_MIRROR, Senses.TRAIL_R)] += k
            }
            // The exit choice is made by the ring, not by the first trail the antennae touch: the
            // ring pair stays open on a trail (drop its whenOff TRAIL_ON condition). The ring is
            // only sensed within Senses.RING_NEAR of the entrance, so this acts only there.
            // Optional value scales the pair's weight on the turn output.
            part.startsWith("ringon") -> {
                val k = part.removePrefix("ringon").toFloatOrNull() ?: 1f
                for (unit in intArrayOf(u.U_RING_TURN, u.U_RING_TURN_MIRROR)) {
                    w[recW(unit, u.U_TRAIL_ON)] = 0f
                    w[biasW(unit)] += SeedBrain.GATE
                }
                w[outW(Outputs.TURN, u.U_RING_TURN)] *= k
                w[outW(Outputs.TURN, u.U_RING_TURN_MIRROR)] *= k
            }
            else -> error("unknown proto part $part")
        }
    }

    fun inW(unit: Int, input: Int) = Brain.inputWeight(Senses.COUNT, SeedBrain.HIDDEN, unit, input)
    fun recW(unit: Int, from: Int) = Brain.recurrentWeight(Senses.COUNT, SeedBrain.HIDDEN, unit, from)
    fun biasW(unit: Int) = Brain.hiddenBias(Senses.COUNT, SeedBrain.HIDDEN, unit)
    fun outW(o: Int, unit: Int) = Brain.outputWeight(Senses.COUNT, SeedBrain.HIDDEN, o, unit)
    fun outB(o: Int) = Brain.outputBias(Senses.COUNT, SeedBrain.HIDDEN, Outputs.COUNT, o)
}

package com.bydesigninteractive.ant.sim.search

import com.bydesigninteractive.ant.sim.brain.Genome
import com.bydesigninteractive.ant.sim.brain.Mirror
import com.bydesigninteractive.ant.sim.brain.SeedBrain
import com.bydesigninteractive.ant.sim.brain.Senses
import java.io.File
import java.util.Locale
import java.util.Random
import java.util.concurrent.Callable
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.math.abs

/**
 * The realism search's settings (spec 4.1 to 4.4); the defaults are the full search.
 *
 * [gates] adds the gate terms ([Gates], scored by [Litmus.gateParts]) to the screening, at
 * [gateScale] (1 is the real experiment). They run on every variant that may be full-checked
 * (viable ones, or all when [requireViable] is false), or, with [gateTop] above 0, only on the
 * best [gateTop] of those per screening batch by base score. [requireViable] also limits the full
 * check to viable variants.
 */
class SearchConfig(
    val generations: Int = 8,
    val population: Int = 40,
    val keep: Int = 8,
    val seeds: List<Long> = listOf(1L, 2L, 3L),
    val minutes: Int = 20,
    val viabilityMinute: Int = 10,
    val sigma: Float = 0.05f,
    val floor: Float = 0.02f,
    val workers: Int = defaultWorkers(),
    val searchSeed: Long = 1L,
    val fullCheck: Int = 5,
    val gruterSeeds: List<Long> = (1L..8L).toList(),
    val experimentSeeds: List<Long> = listOf(1L, 2L, 3L, 4L),
    val fullScale: Float = 1f,
    val requireViable: Boolean = true,
    val gates: Boolean = true,
    val gateTop: Int = 0,
    val gateScale: Float = 1f,
    val outDir: File = File("build/search"),
) {
    companion object {
        /** Every logical processor but two, at least one; scripts/search.ps1 passes the P-core threads minus two instead. */
        fun defaultWorkers(): Int = maxOf(1, Runtime.getRuntime().availableProcessors() - 2)
    }
}

/** A brain variant: its id (`seed`, or `gN-i` for the i-th child of generation N), its parent's id and its genome. */
class Variant(val id: String, val parent: String?, val genome: Genome)

/**
 * A screened variant: its trials (one per screening seed), their pooled measurements, its gate
 * terms (null when they did not run) and the litmus score with the gate terms.
 */
class Scored(val variant: Variant, val trials: List<TrialResult>, val measurements: Measurements, val gates: Gates? = null) {
    val baseScore: Double = Litmus.score(measurements)
    val score: Double = Litmus.score(measurements, gates)
    val alive: Boolean get() = trials.all { it.death == Death.NONE }
    val death: Death get() = trials.firstOrNull { it.death != Death.NONE }?.death ?: Death.NONE
}

/** A screened variant's full check: the Gruter test and the four open-ground experiments. */
class FullCheck(val scored: Scored, val gruter: Experiments.GruterResult, val experiments: List<ExperimentResult>) {
    val experimentsPassed: Int get() = experiments.count { it.passed }
}

/**
 * What a search found: each generation's variants, every variant ranked, the full checks, the
 * seed brain's full check as the baseline (null without a full check) and the winner (null when
 * none passed Gruter with at least the baseline's experiments).
 */
class SearchOutcome(
    val generations: List<List<Scored>>,
    val ranking: List<Scored>,
    val full: List<FullCheck>,
    val baseline: FullCheck?,
    val winner: FullCheck?,
    val elapsedSeconds: Double,
)

/**
 * The realism search (spec section 4): a simple evolution strategy from the seed brain. Trials
 * run in parallel on a pool of worker threads, each on a fresh World, so nothing is shared; the
 * only random generator here is the search's own, seeded, used on the calling thread, so the
 * result does not depend on the number of workers.
 */
object Search {
    /** Viable first, then with gate terms before without, then lower score, then id, so ties break the same way every run. */
    val RANK: Comparator<Scored> = compareBy<Scored>({ !it.alive }, { it.gates == null }, { it.score }, { it.variant.id })

    fun run(c: SearchConfig, log: (String) -> Unit = ::println): SearchOutcome {
        val start = System.nanoTime()
        c.outDir.mkdirs()
        val rng = Random(c.searchSeed)
        val pool = Executors.newFixedThreadPool(c.workers)
        try {
            val seedScored = screen(listOf(Variant("seed", null, SeedBrain.genome())), c, pool)
            var elites = seedScored
            val all = ArrayList<Scored>(seedScored)
            val generations = ArrayList<List<Scored>>()
            for (g in 0 until c.generations) {
                val count = if (g == 0) c.population - 1 else c.population
                val children = (0 until count).map { i ->
                    val parent = elites[i % elites.size].variant
                    Variant("g$g-$i", parent.id, mutate(parent.genome, rng, c.sigma, c.floor))
                }
                val genStart = System.nanoTime()
                val scored = screen(children, c, pool)
                generations += if (g == 0) seedScored + scored else scored
                all += scored
                elites = (elites + scored).sortedWith(RANK).take(c.keep)
                val best = elites.first()
                log(String.format(
                    Locale.ROOT, "generation %d: best %s score %.3f (%s), viable %d of %d, dropped %s, %.0f s",
                    g, best.variant.id, best.score, best.death.name, scored.count { it.alive }, scored.size,
                    Report.deaths(scored), (System.nanoTime() - genStart) / 1e9,
                ))
                best.variant.genome.save(File(c.outDir, "gen-$g-best.genome").toPath())
            }
            val ranking = all.sortedWith(RANK)
            val candidates = (if (c.requireViable) ranking.filter { it.alive } else ranking).take(c.fullCheck)
            // The seed brain is full-checked too, as the baseline, unless it is a candidate already.
            val seedRun = seedScored.first()
            val checked = if (c.fullCheck <= 0 || candidates.any { it === seedRun }) candidates else candidates + seedRun
            val results = fullCheck(checked, c, pool)
            val baseline = results.firstOrNull { it.scored === seedRun }
            val full = results.filter { r -> candidates.any { it === r.scored } }
            for (fc in results) {
                log("full check: ${fc.scored.variant.id}" + if (fc === baseline) " (baseline)" else "")
                log("  " + fc.gruter.line())
                for (e in fc.experiments) log("  " + e.line())
            }
            val winner = full.filter { it.gruter.passed && it.experimentsPassed >= (baseline?.experimentsPassed ?: 0) }
                .maxWithOrNull(compareBy<FullCheck>({ it.experimentsPassed }, { -it.scored.score }))
            winner?.scored?.variant?.genome?.save(File(c.outDir, "winner.genome").toPath())
            val outcome = SearchOutcome(generations, ranking, full, baseline, winner, (System.nanoTime() - start) / 1e9)
            Report.write(File(c.outDir, "report.md"), c, outcome)
            log(if (winner != null) "winner: ${winner.scored.variant.id}" else "no variant passed the Gruter test")
            return outcome
        } finally {
            pool.shutdown()
        }
    }

    /** The mirror ties for [g]: the seed brain's ([Mirror.seed]) when the genome has its layout and sizes, else null. */
    fun mirrorFor(g: Genome): Mirror? {
        if (g.layout != Senses.LAYOUT) return null
        val m = Mirror.seed()
        return if (m.fits(g)) m else null
    }

    /**
     * A child of [parent]: each free parameter plus a normal change of sd [sigma] times its size
     * plus [floor], so zero weights can grow. With [mirror] the change is drawn in tied space (M2a
     * Task 10 fix): one draw per mirror pair of weights, the partner set to the weight times the
     * pair's sign (a shared gate weight gets the same change on both members, a signal or turn
     * weight the opposite), and weights the symmetry holds at zero stay zero. The child is then
     * exactly mirror-symmetric, so its turn stays odd in every signed input and its noise does
     * not make it circle. Without a mirror every weight mutates on its own.
     */
    fun mutate(parent: Genome, rng: Random, sigma: Float, floor: Float, mirror: Mirror? = mirrorFor(parent)): Genome {
        val w = parent.weights.copyOf()
        if (mirror == null) {
            for (i in w.indices) w[i] += rng.nextGaussian().toFloat() * (sigma * abs(w[i]) + floor)
        } else {
            require(mirror.size == w.size) { "mirror does not fit the genome" }
            for (i in w.indices) {
                val p = mirror.partner[i]
                if (p < i) continue // set with its partner
                if (p == i && mirror.sign[i] < 0) {
                    w[i] = 0f
                    continue
                }
                w[i] += rng.nextGaussian().toFloat() * (sigma * abs(w[i]) + floor)
                if (p != i) w[p] = mirror.sign[i] * w[i]
            }
        }
        return Genome(parent.inputs, parent.hidden, parent.outputs, w, parent.layout)
    }

    /**
     * Screens every variant on every seed in parallel (with the Y-choice trial), then runs the gate
     * terms ([Gates]) on the variants [SearchConfig.gates] selects; results come back in variant order.
     */
    fun screen(variants: List<Variant>, c: SearchConfig, pool: ExecutorService): List<Scored> {
        val futures = variants.map { v ->
            c.seeds.map { s -> pool.submit(Callable { Trial.screen(v.genome, s, c.minutes, c.viabilityMinute, yChoice = true) }) }
        }
        val base = variants.mapIndexed { i, v ->
            val trials = futures[i].map { it.get() }
            Scored(v, trials, Measurements.mean(trials.map { it.measurements }))
        }
        if (!c.gates) return base
        var eligible = base.filter { it.alive || !c.requireViable }
        if (c.gateTop > 0) {
            eligible = eligible.sortedWith(compareBy<Scored>({ it.baseScore }, { it.variant.id })).take(c.gateTop)
        }
        val pending = eligible.associateWith { Gates.start(it.variant.genome, pool, c.gateScale) }
        return base.map { s ->
            val p = pending[s] ?: return@map s
            Scored(s.variant, s.trials, s.measurements, p.get())
        }
    }

    /**
     * The full check of every candidate at once, results in candidate order. Each candidate has
     * its own coordinating thread, which only submits runs to [pool] and waits for them, so the
     * long runs of all candidates (an hour of simulated time for the crowded switch and the
     * depletion switch) share the workers, and one candidate's last runs do not leave them idle.
     */
    private fun fullCheck(candidates: List<Scored>, c: SearchConfig, pool: ExecutorService): List<FullCheck> {
        if (candidates.isEmpty()) return emptyList()
        val coordinators = Executors.newFixedThreadPool(candidates.size)
        try {
            val futures = candidates.map { s ->
                coordinators.submit(Callable {
                    val gruter = Experiments.gruter(s.variant.genome, c.gruterSeeds, pool, c.fullScale)
                    val experiments = Experiments.all(s.variant.genome, c.experimentSeeds, pool, c.fullScale)
                    FullCheck(s, gruter, experiments)
                })
            }
            return futures.map { it.get() }
        } finally {
            coordinators.shutdown()
        }
    }
}

/** The search's markdown report: settings, the winner, each generation, the full check and every variant's measurements. */
internal object Report {
    /** The dropped variants of [list] counted by cause, for example "NO_FEED 12, NOT_MOVING 3", or "none". */
    fun deaths(list: List<Scored>): String {
        val counts = list.filter { !it.alive }.groupingBy { it.death }.eachCount()
        if (counts.isEmpty()) return "none"
        return Death.values().filter { it in counts }.joinToString(", ") { "${it.name} ${counts.getValue(it)}" }
    }

    fun write(file: File, c: SearchConfig, o: SearchOutcome) {
        val sb = StringBuilder()
        sb.appendLine("# M2a realism search report")
        sb.appendLine()
        sb.appendLine(
            "Settings: ${c.generations} generations of ${c.population}, keep ${c.keep}, screening seeds ${c.seeds}, " +
                "${c.minutes} simulated minutes per trial (viability at minute ${c.viabilityMinute}), mutation sd ${c.sigma} " +
                "of each weight plus ${c.floor} (mirror pairs tied), search seed ${c.searchSeed}, ${c.workers} workers; " +
                "gate terms ${if (c.gates) "on" else "off"} (scale ${c.gateScale}, " +
                "${if (c.gateTop > 0) "best ${c.gateTop} per batch" else "every eligible variant"}); full check of the best " +
                "${c.fullCheck} and the seed (Gruter seeds ${c.gruterSeeds}, experiment seeds ${c.experimentSeeds}, scale ${c.fullScale}).",
        )
        sb.appendLine()
        sb.appendLine("Elapsed: ${f(o.elapsedSeconds / 60.0, 1)} minutes; ${o.ranking.size} variants screened, ${o.ranking.count { it.alive }} viable.")
        sb.appendLine()
        sb.appendLine("## Winner")
        sb.appendLine()
        val w = o.winner
        if (w == null) {
            sb.appendLine(
                "No variant passed the Gruter test with at least the seed brain's experiments passed; " +
                    "the shipped genome stays the seed brain.",
            )
        } else {
            sb.appendLine(
                "${w.scored.variant.id} (parent ${w.scored.variant.parent}): screening score ${f(w.scored.score, 3)}, " +
                    "experiments passed ${w.experimentsPassed} of ${w.experiments.size}. Saved as winner.genome.",
            )
            sb.appendLine()
            sb.appendLine("Score parts: " + parts(w.scored).joinToString(", ") { "${it.first} ${f(it.second, 3)}" } + ".")
        }
        sb.appendLine()
        sb.appendLine("## Generations")
        sb.appendLine()
        sb.appendLine("| Generation | Best | Score | Median score | Viable | Dropped |")
        sb.appendLine("|---|---|---|---|---|---|")
        o.generations.forEachIndexed { g, list ->
            val sorted = list.sortedWith(Search.RANK)
            val median = list.map { it.score }.sorted()[list.size / 2]
            sb.appendLine(
                "| $g | ${sorted.first().variant.id} | ${f(sorted.first().score, 3)} | ${f(median, 3)} | " +
                    "${list.count { it.alive }} of ${list.size} | ${deaths(list)} |",
            )
        }
        sb.appendLine()
        sb.appendLine("## Full check")
        sb.appendLine()
        val b = o.baseline
        if (o.full.isEmpty() && b == null) {
            sb.appendLine("No variant was full-checked.")
            sb.appendLine()
        }
        val checks = if (b != null && o.full.none { it === b }) listOf(b) + o.full else o.full
        for (fc in checks) {
            val tag = if (fc === b) ", the baseline" else ""
            sb.appendLine("### ${fc.scored.variant.id} (screening score ${f(fc.scored.score, 3)}$tag)")
            sb.appendLine()
            sb.appendLine("- " + fc.gruter.line())
            for (e in fc.experiments) sb.appendLine("- " + e.line())
            sb.appendLine()
        }
        sb.appendLine("## Every variant")
        sb.appendLine()
        sb.appendLine(
            "Measurements are pooled over the screening seeds. foragerShare is measured but not scored in M2a " +
                "(worker roles are fixed until M3); naiveChoices is the pooled Y-choice count. The gate columns " +
                "(Gates: 30 and 150 forager following, busier share, crowded loser share per seed, depletion ratio) " +
                "are blank where the gate terms did not run; Base is the score without them.",
        )
        sb.appendLine()
        val names = Measurements().values().map { it.first }
        val gateNames = listOf("small", "large", "busier", "crowded", "depletion")
        sb.appendLine("| Rank | Variant | Parent | Dropped | Score | Base | ${(names + gateNames).joinToString(" | ")} |")
        sb.appendLine("|---|---|---|---|---|---|${(names + gateNames).joinToString("") { "---|" }}")
        o.ranking.forEachIndexed { i, s ->
            val values = s.measurements.values().joinToString(" | ") { f(it.second, 3) }
            val g = s.gates
            val gateValues = if (g == null) List(gateNames.size) { "-" } else listOf(
                f(g.small, 3), f(g.largeFollowing, 3), f(g.busierShare, 3),
                g.crowded.joinToString(" ") { f(it, 2) }, f(g.depletion, 3),
            )
            sb.appendLine(
                "| ${i + 1} | ${s.variant.id} | ${s.variant.parent ?: "-"} | ${s.death.name} | ${f(s.score, 3)} | " +
                    "${f(s.baseScore, 3)} | $values | ${gateValues.joinToString(" | ")} |",
            )
        }
        file.writeText(sb.toString())
    }

    private fun parts(s: Scored): List<Pair<String, Double>> =
        Litmus.parts(s.measurements) + (s.gates?.let { Litmus.gateParts(it) } ?: emptyList())

    private fun f(v: Double?, digits: Int): String =
        if (v == null || !v.isFinite()) "-" else String.format(Locale.ROOT, "%.${digits}f", v)
}

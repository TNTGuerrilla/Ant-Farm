package com.bydesigninteractive.ant.sim.search

import com.bydesigninteractive.ant.sim.brain.Brain
import com.bydesigninteractive.ant.sim.brain.Genome
import com.bydesigninteractive.ant.sim.brain.Outputs
import com.bydesigninteractive.ant.sim.brain.SeedBrain
import com.bydesigninteractive.ant.sim.brain.Senses
import java.util.Locale
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable
import kotlin.test.Test

/**
 * A reporting aid for the litmus test (M2a Task 9), not a check: it screens a genome on a few
 * seeds and prints every measurement, the score and its parts, the Y-choice counts and what the
 * colony run and the Y-choice trial each cost. It runs only when ANT_DIAG is set, for example
 * `ANT_DIAG=1 DIAG_SEEDS=1,2,3 DIAG_MINUTES=20 ./gradlew --no-daemon :sim:test --tests '*ScreeningDiagnostic*'`.
 * DIAG_GENOME picks the genome: seed (default), turn (a turn bias of DIAG_TURN, default 0.1, on
 * the TURN output), fedcut (time since feeding cuts the deposit strongly) fedflat (no
 * cut by time since feeding) or blind (no trail senses, the Y choice's null). The Y choice runs
 * even when the colony run was dropped.
 */
@EnabledIfEnvironmentVariable(named = "ANT_DIAG", matches = ".+")
class ScreeningDiagnostic {
    private val seeds = (System.getenv("DIAG_SEEDS") ?: "1,2,3").split(',').map { it.trim().toLong() }
    private val minutes = (System.getenv("DIAG_MINUTES") ?: "20").toInt()
    private val genomes = (System.getenv("DIAG_GENOME") ?: "seed").split(',').map { it.trim() }

    @Test
    fun printTheScreening() {
        val pool = Executors.newFixedThreadPool(minOf(12, Runtime.getRuntime().availableProcessors()))
        try {
            for (name in genomes) {
                val g = genome(name)
                val jobs = seeds.map { s -> pool.submit(Callable { timed(g, s) }) }
                val results = jobs.map { it.get() }
                for ((r, colonyMs, yMs) in results) {
                    println(String.format(Locale.ROOT, "DIAG %s seed %d: death %s at %d, colony %d ms, Y choice %d ms (%.1f%%)",
                        name, r.seed, r.death, r.minutesRun, colonyMs, yMs, 100.0 * yMs / colonyMs))
                    println("DIAG   " + (r.yChoice?.line() ?: "no Y choice"))
                    println("DIAG   " + values(r.measurements) + " score " + fmt(Litmus.score(r.measurements)))
                }
                val mean = Measurements.mean(results.map { it.first.measurements })
                val pooled = results.mapNotNull { it.first.yChoice }.reduceOrNull { x, y -> x + y }
                println("DIAG $name mean: " + values(mean))
                println("DIAG $name pooled " + (pooled?.line() ?: "none"))
                println("DIAG $name score of the mean " + fmt(Litmus.score(mean)) + ", parts " +
                    Litmus.parts(mean).joinToString(", ") { it.first + " " + fmt(it.second) })
            }
        } finally {
            pool.shutdown()
        }
    }

    private fun timed(g: Genome, seed: Long): Triple<TrialResult, Long, Long> {
        val t0 = System.nanoTime()
        val colony = Trial.screen(g, seed, minutes, viabilityMinute = 10, yChoice = false)
        val t1 = System.nanoTime()
        val y = YChoice.run(g, seed)
        val t2 = System.nanoTime()
        val m = colony.measurements.copy(naiveFollowing = y.share, naiveChoices = y.choices.toDouble(), naiveStrayShare = y.strayShare)
        return Triple(TrialResult(seed, m, colony.death, colony.minutesRun, y), (t1 - t0) / 1_000_000, (t2 - t1) / 1_000_000)
    }

    private fun genome(name: String): Genome {
        val g = SeedBrain.genome()
        val w = g.weights.copyOf()
        when (name) {
            "seed" -> Unit
            "turn" -> w[Brain.outputBias(g.inputs, g.hidden, g.outputs, Outputs.TURN)] += (System.getenv("DIAG_TURN") ?: "0.1").toFloat()
            "fedcut" -> {
                w[Brain.inputWeight(g.inputs, g.hidden, SeedBrain.U_CUT, Senses.FED_RECENT)] = -6f
                w[Brain.inputWeight(g.inputs, g.hidden, SeedBrain.U_CUT, Senses.BIAS)] = 4.5f
            }
            "fedflat" -> {
                w[Brain.inputWeight(g.inputs, g.hidden, SeedBrain.U_CUT, Senses.FED_RECENT)] = 0f
                w[Brain.inputWeight(g.inputs, g.hidden, SeedBrain.U_CUT, Senses.BIAS)] = -1.5f
            }
            "blind" -> for (unit in 0 until g.hidden) {
                for (input in intArrayOf(Senses.TRAIL_L, Senses.TRAIL_R, Senses.TRAIL_DIFF, Senses.TRAIL_SUM)) {
                    w[Brain.inputWeight(g.inputs, g.hidden, unit, input)] = 0f
                }
            }
            else -> error("unknown genome $name")
        }
        return Genome(g.inputs, g.hidden, g.outputs, w, g.layout)
    }

    private fun values(m: Measurements) = m.values().joinToString(", ") { (k, v) -> k + " " + (v?.let { fmt(it) } ?: "null") }

    private fun fmt(v: Double) = String.format(Locale.ROOT, "%.3f", v)
}

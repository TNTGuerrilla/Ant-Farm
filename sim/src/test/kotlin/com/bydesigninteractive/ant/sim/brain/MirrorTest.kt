package com.bydesigninteractive.ant.sim.brain

import com.bydesigninteractive.ant.sim.search.Search
import java.util.Random
import kotlin.math.abs
import kotlin.math.tanh
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** The mirror ties (M2a Task 10 fix): the seed brain is symmetric, and so is every tied mutant, whose turn stays odd. */
class MirrorTest {
    private val mirror = Mirror.seed()
    private val seed = SeedBrain.genome()

    /** Five mutants of the seed and five of a mutant, at the search's settings and at three times its sd. */
    private val mutants: List<Genome> = run {
        val r = Random(7)
        val first = (0 until 5).map { Search.mutate(seed, r, 0.05f, 0.02f) }
        first + first.map { Search.mutate(it, r, 0.15f, 0.06f) }
    }

    private val signed = listOf(
        Senses.TRAIL_DIFF, Senses.RING_BEARING, Senses.HOME_SIN, Senses.HONEYDEW_SIN,
        Senses.PREY_SIN, Senses.NUDGE, Senses.NOISE,
    )

    /** Inputs at rest for each mode: searching on and off a trail, homeward with food, carrying a pellet. */
    private val rests: List<Map<Int, Float>> = listOf(
        mapOf(),
        mapOf(Senses.TRAIL_L to 0.5f, Senses.TRAIL_R to 0.5f, Senses.TRAIL_SUM to 0.67f, Senses.RING to 0.6f),
        mapOf(Senses.CROP to 1f, Senses.FULL to 1f, Senses.FED_RECENT to 1f, Senses.FOOT_L to 0.3f, Senses.FOOT_R to 0.3f),
        mapOf(Senses.DIGGER to 1f, Senses.CARRYING to 1f, Senses.HOME_DIST to 0.1f),
    )

    private fun inputs(rest: Map<Int, Float>): FloatArray {
        val x = FloatArray(Senses.COUNT)
        x[Senses.BIAS] = 1f
        x[Senses.RESERVES] = 1f
        for ((i, v) in rest) x[i] = v
        return x
    }

    /** The turn rates of [steps] evaluations from rest with [x] held. */
    private fun turns(g: Genome, x: FloatArray, steps: Int = 6): List<Float> {
        val b = g.brain()
        val h = FloatArray(g.hidden)
        val s = FloatArray(g.hidden)
        val o = FloatArray(g.outputs)
        return (0 until steps).map {
            b.evaluate(x, h, s, o)
            Outputs.MAX_TURN_RATE * tanh(o[Outputs.TURN])
        }
    }

    @Test
    fun theSeedBrainIsMirrorSymmetric() {
        assertEquals(0f, mirror.asymmetry(seed.weights))
        assertTrue(mirror.fits(seed))
        println("mirror ties: ${mirror.pairs} pairs, ${mirror.zeros} held at zero, ${mirror.free} free, of ${mirror.size}")
        assertEquals(mirror.size, 2 * mirror.pairs + mirror.zeros + mirror.free)
        assertEquals(0f, seed.weights[Brain.outputBias(seed.inputs, seed.hidden, seed.outputs, Outputs.TURN)])
    }

    @Test
    fun tiedMutantsStayMirrorSymmetric() {
        for (g in mutants) assertEquals(0f, mirror.asymmetry(g.weights))
    }

    @Test
    fun aMutantsTurnIsOddInEachSteeringSignal() {
        for ((n, g) in mutants.withIndex()) {
            for (rest in rests) {
                val x = inputs(rest)
                // At rest (every signed input 0, left and right equal) a mutant does not turn.
                for (t in turns(g, x)) assertTrue(abs(t) < 1e-4f, "mutant $n turns $t at rest $rest")
                for (i in signed) {
                    for (v in floatArrayOf(0.3f, 1f)) {
                        val plus = x.copyOf().also { it[i] = v }
                        val minus = x.copyOf().also { it[i] = -v }
                        val a = turns(g, plus)
                        val b = turns(g, minus)
                        for (k in a.indices) {
                            assertTrue(abs(a[k] + b[k]) < 1e-4f, "mutant $n, input $i at $v, rest $rest: ${a[k]} and ${b[k]}")
                        }
                    }
                }
                // Swapping the antennae (and the footprints) mirrors the turn too.
                val lr = x.copyOf().also { it[Senses.TRAIL_L] = 0.7f; it[Senses.TRAIL_R] = 0.2f; it[Senses.FOOT_L] = 0.5f; it[Senses.FOOT_R] = 0.1f }
                val rl = x.copyOf().also { it[Senses.TRAIL_L] = 0.2f; it[Senses.TRAIL_R] = 0.7f; it[Senses.FOOT_L] = 0.1f; it[Senses.FOOT_R] = 0.5f }
                val a = turns(g, lr)
                val b = turns(g, rl)
                for (k in a.indices) assertTrue(abs(a[k] + b[k]) < 1e-4f, "mutant $n, swapped antennae, rest $rest")
            }
        }
    }

    /** Antithetic noise (+n then -n, as SeedBrainTest's drift check) gives a mean turn of about zero for every mutant. */
    @Test
    fun aMutantsMeanTurnUnderAntitheticNoiseIsAboutZero() {
        for ((n, g) in mutants.withIndex()) {
            val b = g.brain()
            val x = inputs(emptyMap())
            val h = FloatArray(g.hidden)
            val s = FloatArray(g.hidden)
            val o = FloatArray(g.outputs)
            repeat(5) { b.evaluate(x, h, s, o) }
            val r = Random(11)
            var noise = 0f
            var sum = 0.0
            val steps = 20_000
            for (i in 0 until steps) {
                if (i % 2 == 0) noise = r.nextGaussian().toFloat()
                x[Senses.NOISE] = if (i % 2 == 0) noise else -noise
                b.evaluate(x, h, s, o)
                sum += Outputs.MAX_TURN_RATE * tanh(o[Outputs.TURN])
            }
            val mean = sum / steps
            assertTrue(abs(mean) < 0.05, "mutant $n mean turn $mean rad/s")
        }
    }
}

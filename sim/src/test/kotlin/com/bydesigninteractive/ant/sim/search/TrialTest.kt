package com.bydesigninteractive.ant.sim.search

import com.bydesigninteractive.ant.sim.brain.Brain
import com.bydesigninteractive.ant.sim.brain.Genome
import com.bydesigninteractive.ant.sim.brain.Outputs
import com.bydesigninteractive.ant.sim.brain.SeedBrain
import com.bydesigninteractive.ant.sim.brain.Senses
import com.bydesigninteractive.ant.sim.scenario.Scenarios
import com.bydesigninteractive.ant.sim.scenario.fingerprint
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class TrialTest {
    /** The seed brain with one output bias replaced. */
    private fun seedWith(output: Int, bias: Float): Genome {
        val g = SeedBrain.genome()
        val w = g.weights.copyOf()
        w[Brain.outputBias(g.inputs, g.hidden, g.outputs, output)] = bias
        return Genome(g.inputs, g.hidden, g.outputs, w, g.layout)
    }

    @Test
    fun antsThatNeverWalkAreDroppedAtTheViabilityMinute() {
        val r = Trial.screen(seedWith(Outputs.SPEED, -30f), seed = 1, minutes = 20, viabilityMinute = 2)
        assertEquals(Death.NO_FEED, r.death)
        assertEquals(2, r.minutesRun)
    }

    @Test
    fun aBrainThatGivesNoNumberIsDroppedAtOnce() {
        val r = Trial.screen(seedWith(Outputs.TURN, Float.NaN), seed = 1, minutes = 5, viabilityMinute = 10)
        assertEquals(Death.NON_NUMERIC, r.death)
        assertEquals(1, r.minutesRun)
    }

    @Test
    fun theSeedBrainIsViable() {
        val r = Trial.screen(SeedBrain.genome(), seed = 1, minutes = 12, viabilityMinute = 10)
        assertEquals(Death.NONE, r.death, "seed brain dropped: ${r.death} at minute ${r.minutesRun}")
        val feed = assertNotNull(r.measurements.feedSeconds)
        assertTrue(feed in 59.0..61.0, "feed $feed s")
        val share = assertNotNull(r.measurements.foragerShare)
        assertTrue(share in 0.0..1.0)
        val y = assertNotNull(r.yChoice, "a viable run runs the Y choice")
        assertEquals(YChoice.RELEASES, y.marked + y.unmarked + y.strayed + y.timeouts)
        assertEquals(y.share, r.measurements.naiveFollowing)
    }

    /** The seed brain with each (hidden unit, input, weight) of [set] replaced. */
    private fun seedWithInput(vararg set: Triple<Int, Int, Float>): Genome {
        val g = SeedBrain.genome()
        val w = g.weights.copyOf()
        for ((unit, input, v) in set) w[Brain.inputWeight(g.inputs, g.hidden, unit, input)] = v
        return Genome(g.inputs, g.hidden, g.outputs, w, g.layout)
    }

    /**
     * The near-food ratio weighs marks by the amount the brain lays. A deposit that falls steeply
     * with time since feeding (the seed's cut unit reading FED_RECENT at -6, its threshold moved so
     * a fresh forager deposits as the seed's does) lays more per mm near the food than 20 to 40 s
     * on; counting marks alone gave about 1 whatever the deposit. The control has no cut by time
     * since feeding (FED_RECENT at 0, the threshold at the seed's fresh level) and stays near 1.
     */
    @Test
    fun aDepositThatFallsWithTimeSinceFeedingIsMoreNearTheFood() {
        val falling = seedWithInput(
            Triple(SeedBrain.U_CUT, Senses.FED_RECENT, -6f),
            Triple(SeedBrain.U_CUT, Senses.BIAS, 4.5f),
        )
        val flat = seedWithInput(
            Triple(SeedBrain.U_CUT, Senses.FED_RECENT, 0f),
            Triple(SeedBrain.U_CUT, Senses.BIAS, -1.5f),
        )
        val f = assertNotNull(Trial.screen(falling, seed = 1, minutes = 12, viabilityMinute = 10, yChoice = false).measurements.nearFoodRatio)
        val c = assertNotNull(Trial.screen(flat, seed = 1, minutes = 12, viabilityMinute = 10, yChoice = false).measurements.nearFoodRatio)
        assertTrue(f > c + 0.4 && c < 1.1, "near-food ratio falling $f, flat $c")
    }

    /** A steady turn to one side (the TURN output's bias raised) reads as drift well above a plain seed brain's. */
    @Test
    fun aTurnBiasShowsAsDrift() {
        val plain = Trial.screen(SeedBrain.genome(), seed = 2, minutes = 10, viabilityMinute = 10, yChoice = false)
        val g = SeedBrain.genome()
        val w = g.weights.copyOf()
        w[Brain.outputBias(g.inputs, g.hidden, g.outputs, Outputs.TURN)] += TURN_BIAS
        val biased = Trial.screen(Genome(g.inputs, g.hidden, g.outputs, w, g.layout), seed = 2, minutes = 10, viabilityMinute = 10, yChoice = false)
        val p = assertNotNull(plain.measurements.meanAbsTurnBias)
        val b = assertNotNull(biased.measurements.meanAbsTurnBias)
        assertTrue(p < Litmus.TURN_BIAS_MAX, "plain seed drift $p")
        // A full tolerance over the limit: the bias costs at least 1 in the score.
        assertTrue(b > Litmus.TURN_BIAS_MAX + Litmus.TURN_BIAS_TOLERANCE && b > 3 * p, "biased drift $b against plain $p")
    }

    private companion object {
        const val TURN_BIAS = 0.1f
    }

    @Test
    fun observingDoesNotChangeARun() {
        val a = Scenarios.starter(5)
        val b = Scenarios.starter(5)
        val observer = Observer(b)
        repeat(3000) {
            a.step()
            b.step()
            observer.afterTick()
        }
        assertEquals(fingerprint(a), fingerprint(b))
    }
}

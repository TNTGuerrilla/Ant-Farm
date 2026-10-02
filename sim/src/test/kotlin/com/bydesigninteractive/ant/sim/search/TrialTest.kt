package com.bydesigninteractive.ant.sim.search

import com.bydesigninteractive.ant.sim.brain.Brain
import com.bydesigninteractive.ant.sim.brain.Genome
import com.bydesigninteractive.ant.sim.brain.Outputs
import com.bydesigninteractive.ant.sim.brain.SeedBrain
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

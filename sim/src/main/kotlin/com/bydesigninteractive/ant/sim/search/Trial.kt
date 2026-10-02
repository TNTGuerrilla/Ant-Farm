package com.bydesigninteractive.ant.sim.search

import com.bydesigninteractive.ant.sim.brain.Genome
import com.bydesigninteractive.ant.sim.scenario.Scenarios

/** One screening run's result: its measurements, why it was dropped (or NONE), and how many minutes it ran. */
class TrialResult(val seed: Long, val measurements: Measurements, val death: Death, val minutesRun: Int)

/** A screening run (spec 4.2): the starter colony with a genome, observed, stopped early once it is not viable. */
object Trial {
    fun screen(genome: Genome, seed: Long, minutes: Int, viabilityMinute: Int): TrialResult {
        val w = Scenarios.starter(seed, genome = genome)
        val observer = Observer(w)
        for (m in 1..minutes) {
            repeat(Experiments.TICKS_PER_MINUTE) {
                w.step()
                observer.afterTick()
            }
            val d = observer.death(m, viabilityMinute, final = m == minutes)
            if (d != Death.NONE) return TrialResult(seed, observer.measurements(), d, m)
        }
        return TrialResult(seed, observer.measurements(), Death.NONE, minutes)
    }
}

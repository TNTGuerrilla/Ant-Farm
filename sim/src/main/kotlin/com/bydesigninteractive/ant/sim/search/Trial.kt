package com.bydesigninteractive.ant.sim.search

import com.bydesigninteractive.ant.sim.brain.Genome
import com.bydesigninteractive.ant.sim.scenario.Scenarios

/**
 * One screening run's result: its measurements, why it was dropped (or NONE), how many minutes it
 * ran, and the Y-choice trial's counts (null when it did not run).
 */
class TrialResult(
    val seed: Long,
    val measurements: Measurements,
    val death: Death,
    val minutesRun: Int,
    val yChoice: YChoiceResult? = null,
)

/**
 * A screening run (spec 4.2): the starter colony with a genome, observed, stopped early once it
 * is not viable. A viable run then runs the Y-choice trial ([YChoice]) on the same seed, which
 * gives naive following; it costs a few percent of the colony run (see the M2a Task 9 fix report).
 */
object Trial {
    fun screen(genome: Genome, seed: Long, minutes: Int, viabilityMinute: Int, yChoice: Boolean = true): TrialResult {
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
        if (!yChoice) return TrialResult(seed, observer.measurements(), Death.NONE, minutes)
        val y = YChoice.run(genome, seed)
        val m = observer.measurements().copy(naiveFollowing = y.share, naiveChoices = y.choices.toDouble())
        return TrialResult(seed, m, Death.NONE, minutes, y)
    }
}

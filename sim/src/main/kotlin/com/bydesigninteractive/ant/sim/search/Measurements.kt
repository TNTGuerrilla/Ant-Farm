package com.bydesigninteractive.ant.sim.search

/**
 * What a run measured for the litmus test (spec 4.3); null where the run gave no data (for
 * example no forager came home). Times are simulated seconds; the table in the M2a plan, Task 9,
 * says how each is computed.
 *
 * [meanAbsTurnBias] (rad/s) is the drift measure added after the Task 6 review: the size of a
 * searching forager's own mean signed turn rate over 30 s windows. A searcher that keeps turning
 * one way walks in circles, and circling would otherwise pass as a tortuous, realistic search in
 * [straightOut]; a real searcher's turns to either side about balance out.
 */
data class Measurements(
    val feedSeconds: Double? = null,
    val returnSeconds: Double? = null,
    val unloadSeconds: Double? = null,
    val marksPer5cm: Double? = null,
    val nearFoodRatio: Double? = null,
    val neverLaying: Double? = null,
    val naiveFollowing: Double? = null,
    val straightOut: Double? = null,
    val straightHome: Double? = null,
    val foragerShare: Double? = null,
    val meanAbsTurnBias: Double? = null,
) {
    /** Every measurement with its name, in report order. */
    fun values(): List<Pair<String, Double?>> = listOf(
        "feedSeconds" to feedSeconds,
        "returnSeconds" to returnSeconds,
        "unloadSeconds" to unloadSeconds,
        "marksPer5cm" to marksPer5cm,
        "nearFoodRatio" to nearFoodRatio,
        "neverLaying" to neverLaying,
        "naiveFollowing" to naiveFollowing,
        "straightOut" to straightOut,
        "straightHome" to straightHome,
        "foragerShare" to foragerShare,
        "meanAbsTurnBias" to meanAbsTurnBias,
    )

    companion object {
        /** Each measurement averaged over the runs that have it (finite values only). */
        fun mean(runs: List<Measurements>): Measurements {
            fun avg(f: (Measurements) -> Double?): Double? {
                val v = runs.mapNotNull(f).filter { it.isFinite() }
                return if (v.isEmpty()) null else v.average()
            }
            return Measurements(
                avg { it.feedSeconds }, avg { it.returnSeconds }, avg { it.unloadSeconds },
                avg { it.marksPer5cm }, avg { it.nearFoodRatio }, avg { it.neverLaying },
                avg { it.naiveFollowing }, avg { it.straightOut }, avg { it.straightHome },
                avg { it.foragerShare }, avg { it.meanAbsTurnBias },
            )
        }
    }
}

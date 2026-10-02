package com.bydesigninteractive.ant.sim.search

import kotlin.math.abs
import kotlin.math.max

/**
 * The realism score (spec 4.3), which M4's evolution reuses: each measurement's distance from
 * its field target in units of the target's tolerance, summed. Lower is better; 0 is on target.
 * One-sided targets count only a shortfall (or, for [atMost], an excess). A missing measurement
 * scores [MISSING]. Targets come from the simulation reference (sections 3 to 6) as the spec
 * lists them.
 *
 * The drift target ([TURN_BIAS_MAX]) is not from the field data: it keeps a searcher that turns
 * steadily one way (walking in circles) from scoring as a tortuous, realistic search. 0.15 rad/s
 * is a full circle in about 40 s. A balanced random search measures about 0.07 rad/s over the
 * 120 s or more of searching each forager needs to count ([Observer]).
 *
 * Naive following ([NAIVE_FOLLOWING]) is the share of never-fed foragers that take the marked
 * branch at a Y ([YChoice]), Gruter 2011's 62 to 70%. Its tolerance is 0.06, not the 0.04 that
 * half the field range would give: the screening pools about 300 choices (100 releases on each
 * of 3 seeds), whose binomial standard error at 0.66 is about 0.027, so 0.06 is about two
 * standard errors and a variant is not ranked on sampling noise. Fewer choices would need 0.08.
 *
 * The forager share ([FORAGER_SHARE]) is unscored in M2a: worker roles are fixed until M3
 * (castes), so the 10 to 25% target (Planckaert 2019) is a caste ratio the brain cannot change;
 * the controller and owner were told (2026-10-02). The [Observer] still measures it and reports
 * print it, and on the starter world (60 foragers of 80 workers) a genome could only lower it by
 * keeping foragers idle in the nest.
 */
object Litmus {
    const val MISSING = 5.0

    const val FEED_SECONDS = 60.0
    const val FEED_TOLERANCE = 15.0
    const val RETURN_SECONDS = 40.0
    const val RETURN_TOLERANCE = 20.0
    const val UNLOAD_SECONDS = 60.0
    const val UNLOAD_TOLERANCE = 15.0
    const val MARKS_PER_5CM = 2.25 // 1.5 to 3
    const val MARKS_TOLERANCE = 0.75
    const val NEAR_FOOD_MIN = 1.2 // more marks near the food
    const val NEAR_FOOD_TOLERANCE = 0.2
    const val NEVER_LAYING = 0.14
    const val NEVER_LAYING_TOLERANCE = 0.07
    const val NAIVE_FOLLOWING = 0.66 // 62 to 70%
    const val NAIVE_FOLLOWING_TOLERANCE = 0.06
    const val STRAIGHTER_HOME_MIN = 0.1 // the search out is more tortuous than the way home
    const val STRAIGHTER_HOME_TOLERANCE = 0.1
    const val FORAGER_SHARE = 0.175 // 10 to 25% of workers; unscored until M3, see above
    const val FORAGER_SHARE_TOLERANCE = 0.075
    const val TURN_BIAS_MAX = 0.15 // rad/s: searchers do not walk in circles
    const val TURN_BIAS_TOLERANCE = 0.1

    fun parts(m: Measurements): List<Pair<String, Double>> {
        val out = m.straightOut
        val home = m.straightHome
        val gap = if (out != null && home != null) home - out else null
        return listOf(
            "feed" to near(m.feedSeconds, FEED_SECONDS, FEED_TOLERANCE),
            "return" to near(m.returnSeconds, RETURN_SECONDS, RETURN_TOLERANCE),
            "unload" to near(m.unloadSeconds, UNLOAD_SECONDS, UNLOAD_TOLERANCE),
            "marks" to near(m.marksPer5cm, MARKS_PER_5CM, MARKS_TOLERANCE),
            "nearFood" to atLeast(m.nearFoodRatio, NEAR_FOOD_MIN, NEAR_FOOD_TOLERANCE),
            "neverLaying" to near(m.neverLaying, NEVER_LAYING, NEVER_LAYING_TOLERANCE),
            "naiveFollowing" to near(m.naiveFollowing, NAIVE_FOLLOWING, NAIVE_FOLLOWING_TOLERANCE),
            "straighterHome" to atLeast(gap, STRAIGHTER_HOME_MIN, STRAIGHTER_HOME_TOLERANCE),
            "turnBias" to atMost(m.meanAbsTurnBias, TURN_BIAS_MAX, TURN_BIAS_TOLERANCE),
        )
    }

    fun score(m: Measurements): Double = parts(m).sumOf { it.second }

    fun near(v: Double?, target: Double, tolerance: Double): Double =
        if (v == null || !v.isFinite()) MISSING else abs(v - target) / tolerance

    fun atLeast(v: Double?, min: Double, tolerance: Double): Double =
        if (v == null || !v.isFinite()) MISSING else max(0.0, (min - v) / tolerance)

    fun atMost(v: Double?, limit: Double, tolerance: Double): Double =
        if (v == null || !v.isFinite()) MISSING else max(0.0, (v - limit) / tolerance)
}

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
 * steadily one way (walking in circles) from scoring as a tortuous, realistic search. The seed
 * brain, whose turns balance out, measures 0.022 to 0.031 rad/s over the 120 s or more of
 * searching each forager needs to count ([Observer]); the limit of 0.07 rad/s (a full circle in
 * 90 s) sits just above that floor, with a tolerance of 0.05, so a steady turn of 0.14 rad/s
 * (a loop every 45 s) costs about 1.4 and the seed with +0.1 on its TURN output bias (0.165)
 * about 1.9.
 *
 * Naive following ([NAIVE_FOLLOWING]) is the share of never-fed foragers that take the marked
 * branch at a Y ([YChoice]), Gruter 2011's 62 to 70%. Its tolerance is 0.06, not the 0.04 that
 * half the field range would give: the screening pools about 300 choices (100 releases on each
 * of 3 seeds), whose binomial standard error at 0.66 is about 0.027, so 0.06 is about two
 * standard errors and a variant is not ranked on sampling noise. Fewer than [NAIVE_MIN_CHOICES]
 * pooled choices score [NAIVE_MISSING], more than any reachable value of the share's part (at
 * most 0.66 / 0.06 = 11 only at a share of 0, so a variant cannot gain by choosing rarely: a
 * handful of lucky choices is not a measurement). The ants that make no choice score on their
 * own ([NAIVE_STRAYS_MAX]): Gruter's Y-maze arms left no way off the bridge, so on open ground an
 * ant that leaves the trail, turns back or is still undecided at the timeout failed to follow.
 * Timeouts count as strays.
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
    const val NAIVE_MIN_CHOICES = 100.0 // pooled over the seeds
    const val NAIVE_MISSING = 12.0 // above the share part's largest value, 11
    const val NAIVE_STRAYS_MAX = 0.2 // releases that strayed or timed out
    const val NAIVE_STRAYS_TOLERANCE = 0.1
    const val STRAIGHTER_HOME_MIN = 0.1 // the search out is more tortuous than the way home
    const val STRAIGHTER_HOME_TOLERANCE = 0.1
    const val FORAGER_SHARE = 0.175 // 10 to 25% of workers; unscored until M3, see above
    const val FORAGER_SHARE_TOLERANCE = 0.075
    const val TURN_BIAS_MAX = 0.07 // rad/s: searchers do not walk in circles
    const val TURN_BIAS_TOLERANCE = 0.05

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
            "naiveFollowing" to naive(m),
            "naiveStrays" to atMost(m.naiveStrayShare, NAIVE_STRAYS_MAX, NAIVE_STRAYS_TOLERANCE),
            "straighterHome" to atLeast(gap, STRAIGHTER_HOME_MIN, STRAIGHTER_HOME_TOLERANCE),
            "turnBias" to atMost(m.meanAbsTurnBias, TURN_BIAS_MAX, TURN_BIAS_TOLERANCE),
        )
    }

    fun score(m: Measurements): Double = parts(m).sumOf { it.second }

    // The gate terms ([Gates]), each aimed just past its pass threshold.
    const val GATE_SMALL_MAX = 0.15
    const val GATE_SMALL_TOLERANCE = 0.05
    const val GATE_LARGE_MIN = 0.33 // the test needs above 0.30
    const val GATE_LARGE_TOLERANCE = 0.05
    const val GATE_BUSIER_MIN = 0.7
    const val GATE_BUSIER_TOLERANCE = 0.1
    const val GATE_CROWDED_MIN = 0.5
    const val GATE_CROWDED_TOLERANCE = 0.2
    const val GATE_DEPLETION_MIN = 0.5
    const val GATE_DEPLETION_TOLERANCE = 0.15

    /**
     * The gate terms in tolerance units: following with 30 foragers at most 0.15, with 150 at
     * least 0.33, the busier share at least 0.7, the crowded loser's mean share at least 0.5 and
     * the depletion ratio at least 0.5.
     *
     * Added in M2a Task 10 to put selection pressure on the spec section 1 item 5 gate; a change
     * to spec 4.2/4.3, the owner was told.
     */
    fun gateParts(g: Gates): List<Pair<String, Double>> = listOf(
        "gruterSmall" to atMost(g.small, GATE_SMALL_MAX, GATE_SMALL_TOLERANCE),
        "gruterLarge" to atLeast(g.largeFollowing, GATE_LARGE_MIN, GATE_LARGE_TOLERANCE),
        "gruterBusier" to atLeast(g.busierShare, GATE_BUSIER_MIN, GATE_BUSIER_TOLERANCE),
        "gruterCrowded" to atLeast(g.crowdedMean, GATE_CROWDED_MIN, GATE_CROWDED_TOLERANCE),
        "depletion" to atLeast(g.depletion, GATE_DEPLETION_MIN, GATE_DEPLETION_TOLERANCE),
    )

    /** The score with the gate terms added (or [score] alone without them). */
    fun score(m: Measurements, g: Gates?): Double = score(m) + (g?.let { gateParts(it).sumOf { p -> p.second } } ?: 0.0)

    /** The marked branch's share, scored only over [NAIVE_MIN_CHOICES] or more choices (else [NAIVE_MISSING]). */
    private fun naive(m: Measurements): Double {
        val choices = m.naiveChoices
        val share = m.naiveFollowing
        if (choices == null || !(choices >= NAIVE_MIN_CHOICES) || share == null || !share.isFinite()) return NAIVE_MISSING
        return near(share, NAIVE_FOLLOWING, NAIVE_FOLLOWING_TOLERANCE)
    }

    fun near(v: Double?, target: Double, tolerance: Double): Double =
        if (v == null || !v.isFinite()) MISSING else abs(v - target) / tolerance

    fun atLeast(v: Double?, min: Double, tolerance: Double): Double =
        if (v == null || !v.isFinite()) MISSING else max(0.0, (min - v) / tolerance)

    fun atMost(v: Double?, limit: Double, tolerance: Double): Double =
        if (v == null || !v.isFinite()) MISSING else max(0.0, (v - limit) / tolerance)
}

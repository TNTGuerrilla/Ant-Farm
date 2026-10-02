package com.bydesigninteractive.ant.sim.search

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LitmusTest {
    private val onTarget = Measurements(
        feedSeconds = 60.0, returnSeconds = 40.0, unloadSeconds = 60.0, marksPer5cm = 2.25,
        nearFoodRatio = 1.5, neverLaying = 0.14, naiveFollowing = 0.66,
        straightOut = 0.6, straightHome = 0.8, foragerShare = 0.175, meanAbsTurnBias = 0.05,
        naiveChoices = 300.0, naiveStrayShare = 0.1,
    )

    @Test
    fun onTargetScoresZero() {
        assertEquals(0.0, Litmus.score(onTarget), 1e-9)
    }

    @Test
    fun theScoreCountsTolerances() {
        assertEquals(1.0, Litmus.score(onTarget.copy(feedSeconds = 75.0)), 1e-9)
        assertEquals(2.0, Litmus.score(onTarget.copy(naiveFollowing = 0.54)), 1e-9)
        assertEquals(2.0, Litmus.score(onTarget.copy(feedSeconds = 45.0, unloadSeconds = 75.0)), 1e-9)
    }

    /** Worker roles are fixed until M3, so the forager share is measured but not scored. */
    @Test
    fun theForagerShareIsUnscored() {
        assertEquals(0.0, Litmus.score(onTarget.copy(foragerShare = 0.7)), 1e-9)
        assertEquals(0.0, Litmus.score(onTarget.copy(foragerShare = null)), 1e-9)
        assertTrue(Litmus.parts(onTarget).none { it.first == "foragerShare" })
        assertTrue(onTarget.values().any { it.first == "foragerShare" })
    }

    @Test
    fun aMissingMeasurementScoresMissing() {
        assertEquals(Litmus.MISSING, Litmus.score(onTarget.copy(returnSeconds = null)), 1e-9)
        assertEquals(Litmus.MISSING, Litmus.score(onTarget.copy(returnSeconds = Double.NaN)), 1e-9)
        assertEquals(Litmus.MISSING, Litmus.score(onTarget.copy(straightOut = null)), 1e-9)
    }

    @Test
    fun oneSidedTargetsCountOnlyAShortfall() {
        assertEquals(0.0, Litmus.score(onTarget.copy(nearFoodRatio = 3.0)), 1e-9)
        assertEquals(1.0, Litmus.score(onTarget.copy(nearFoodRatio = 1.0)), 1e-9)
        assertEquals(1.0, Litmus.score(onTarget.copy(straightHome = 0.6)), 1e-9)
        assertEquals(2.0, Litmus.score(onTarget.copy(straightHome = 0.5)), 1e-9)
    }

    /** Drift: a searcher that keeps turning one way (walking in circles) counts only above the limit. */
    @Test
    fun theTurnBiasCountsOnlyAnExcess() {
        assertEquals(0.0, Litmus.score(onTarget.copy(meanAbsTurnBias = 0.0)), 1e-9)
        assertEquals(0.0, Litmus.score(onTarget.copy(meanAbsTurnBias = 0.07)), 1e-9)
        assertEquals(1.0, Litmus.score(onTarget.copy(meanAbsTurnBias = 0.12)), 1e-9)
        assertEquals(1.4, Litmus.score(onTarget.copy(meanAbsTurnBias = 0.14)), 1e-9)
        assertEquals(Litmus.MISSING, Litmus.score(onTarget.copy(meanAbsTurnBias = null)), 1e-9)
    }

    @Test
    fun theMeanSkipsRunsWithoutData() {
        val m = Measurements.mean(listOf(onTarget, onTarget.copy(feedSeconds = 80.0, returnSeconds = null)))
        assertEquals(70.0, m.feedSeconds!!, 1e-9)
        assertEquals(40.0, m.returnSeconds!!, 1e-9)
        assertEquals(13, m.values().size)
    }

    /**
     * Naive following needs 100 pooled choices; fewer score more than any share could, so making
     * no choice never pays. Strays (and timeouts) score on their own above 20%.
     */
    @Test
    fun naiveFollowingNeedsChoicesAndScoresStrays() {
        assertEquals(Litmus.NAIVE_MISSING, Litmus.score(onTarget.copy(naiveChoices = 99.0)), 1e-9)
        assertEquals(Litmus.NAIVE_MISSING, Litmus.score(onTarget.copy(naiveChoices = null)), 1e-9)
        assertTrue(Litmus.NAIVE_MISSING > Litmus.NAIVE_FOLLOWING / Litmus.NAIVE_FOLLOWING_TOLERANCE)
        assertEquals(0.0, Litmus.score(onTarget.copy(naiveChoices = 100.0)), 1e-9)
        assertEquals(0.0, Litmus.score(onTarget.copy(naiveStrayShare = 0.2)), 1e-9)
        assertEquals(1.0, Litmus.score(onTarget.copy(naiveStrayShare = 0.3)), 1e-9)
        assertEquals(Litmus.MISSING, Litmus.score(onTarget.copy(naiveStrayShare = null)), 1e-9)
    }

    /** The Y-choice counts pool over seeds: the share over all choices, not the mean of the shares. */
    @Test
    fun theMeanPoolsTheYChoices() {
        val m = Measurements.mean(listOf(
            onTarget.copy(naiveFollowing = 0.5, naiveChoices = 100.0),
            onTarget.copy(naiveFollowing = 0.8, naiveChoices = 50.0),
        ))
        assertEquals(90.0 / 150.0, m.naiveFollowing!!, 1e-9)
        assertEquals(150.0, m.naiveChoices!!, 1e-9)
    }

    /** The gate terms score nothing just past each threshold and one per tolerance short of it. */
    @Test
    fun theGateTermsAimJustPastEachThreshold() {
        val past = Gates(small = 0.1, largeFollowing = 0.34, busierShare = 0.9, crowded = listOf(0.9, 0.6), depletion = 0.6)
        assertEquals(0.0, Litmus.gateParts(past).sumOf { it.second }, 1e-9)
        assertEquals(Litmus.score(onTarget), Litmus.score(onTarget, past), 1e-9)
        assertEquals(Litmus.score(onTarget), Litmus.score(onTarget, null), 1e-9)
        val short = Gates(small = 0.2, largeFollowing = 0.28, busierShare = 0.6, crowded = listOf(0.9, 0.1), depletion = 0.35)
        val parts = Litmus.gateParts(short).toMap()
        assertEquals(1.0, parts.getValue("gruterSmall"), 1e-9)
        assertEquals(1.0, parts.getValue("gruterLarge"), 1e-9)
        assertEquals(1.0, parts.getValue("gruterBusier"), 1e-9)
        assertEquals(0.0, parts.getValue("gruterCrowded"), 1e-9) // mean 0.5
        assertEquals(1.0, parts.getValue("depletion"), 1e-9)
    }
}

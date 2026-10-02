package com.bydesigninteractive.ant.sim.search

import kotlin.test.Test
import kotlin.test.assertEquals

class LitmusTest {
    private val onTarget = Measurements(
        feedSeconds = 60.0, returnSeconds = 40.0, unloadSeconds = 60.0, marksPer5cm = 2.25,
        nearFoodRatio = 1.5, neverLaying = 0.14, naiveFollowing = 0.66,
        straightOut = 0.6, straightHome = 0.8, foragerShare = 0.175, meanAbsTurnBias = 0.05,
    )

    @Test
    fun onTargetScoresZero() {
        assertEquals(0.0, Litmus.score(onTarget), 1e-9)
    }

    @Test
    fun theScoreCountsTolerances() {
        assertEquals(1.0, Litmus.score(onTarget.copy(feedSeconds = 75.0)), 1e-9)
        assertEquals(2.0, Litmus.score(onTarget.copy(naiveFollowing = 0.58)), 1e-9)
        assertEquals(3.0, Litmus.score(onTarget.copy(feedSeconds = 45.0, foragerShare = 0.325)), 1e-9)
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
        assertEquals(0.0, Litmus.score(onTarget.copy(meanAbsTurnBias = 0.15)), 1e-9)
        assertEquals(1.0, Litmus.score(onTarget.copy(meanAbsTurnBias = 0.25)), 1e-9)
        assertEquals(Litmus.MISSING, Litmus.score(onTarget.copy(meanAbsTurnBias = null)), 1e-9)
    }

    @Test
    fun theMeanSkipsRunsWithoutData() {
        val m = Measurements.mean(listOf(onTarget, onTarget.copy(feedSeconds = 80.0, returnSeconds = null)))
        assertEquals(70.0, m.feedSeconds!!, 1e-9)
        assertEquals(40.0, m.returnSeconds!!, 1e-9)
        assertEquals(11, m.values().size)
    }
}

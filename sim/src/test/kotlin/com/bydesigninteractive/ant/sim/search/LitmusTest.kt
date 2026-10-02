package com.bydesigninteractive.ant.sim.search

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

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
        assertEquals(0.0, Litmus.score(onTarget.copy(meanAbsTurnBias = 0.15)), 1e-9)
        assertEquals(1.0, Litmus.score(onTarget.copy(meanAbsTurnBias = 0.25)), 1e-9)
        assertEquals(Litmus.MISSING, Litmus.score(onTarget.copy(meanAbsTurnBias = null)), 1e-9)
    }

    @Test
    fun theMeanSkipsRunsWithoutData() {
        val m = Measurements.mean(listOf(onTarget, onTarget.copy(feedSeconds = 80.0, returnSeconds = null)))
        assertEquals(70.0, m.feedSeconds!!, 1e-9)
        assertEquals(40.0, m.returnSeconds!!, 1e-9)
        assertEquals(12, m.values().size)
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
}

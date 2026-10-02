package com.bydesigninteractive.ant.sim.search

import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class YChoiceTest {
    private fun counts(r: YChoiceResult) = listOf(r.marked, r.unmarked, r.strayed, r.timeouts, r.left)

    /** The trial draws only the world's generator, so the same genome and seed give the same counts. */
    @Test
    fun theSameSeedGivesTheSameCounts() {
        assertEquals(counts(YChoice.run(null, 4)), counts(YChoice.run(null, 4)))
    }

    /**
     * The marked side alternates between releases, so a brain that follows the mark goes left as
     * often as right: a side preference cannot read as following.
     */
    @Test
    fun theSeedBrainGoesLeftAsOftenAsRight() {
        val r = YChoice.run(null, 1)
        assertEquals(YChoice.RELEASES, r.releases)
        assertTrue(r.choices >= 90, r.line())
        assertTrue(abs(2 * r.left - r.choices) <= 4, r.line())
    }
}

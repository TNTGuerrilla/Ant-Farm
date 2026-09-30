package com.bydesigninteractive.ant.sim.world

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class NestPlanTest {
    private val plan = NestPlan(seed = 5, entranceX = 100)

    @Test
    fun startsWithAFiveWideEntranceShaft() {
        assertEquals(Rect(98, 0, 102, 39), plan.rect(0))
    }

    @Test
    fun chambersAlternateSidesAndTouchTheShaft() {
        val first = plan.rect(1)
        assertEquals(103, first.x0)
        assertEquals(5, first.y1 - first.y0)
        assertEquals(39, first.y1)
        val second = plan.rect(3)
        assertEquals(97, second.x1)
    }

    @Test
    fun shaftSegmentsContinueDownAndLengthen() {
        val a = plan.rect(2)
        val b = plan.rect(4)
        assertEquals(40, a.y0)
        assertEquals(a.y1 + 1, b.y0)
        assertTrue(b.y1 - b.y0 > a.y1 - a.y0)
    }
}

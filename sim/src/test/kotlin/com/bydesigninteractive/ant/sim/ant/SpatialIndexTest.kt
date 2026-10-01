package com.bydesigninteractive.ant.sim.ant

import kotlin.test.Test
import kotlin.test.assertEquals

class SpatialIndexTest {
    private fun ant(id: Int, x: Float, y: Float) = Ant(id, Role.FORAGER, 1f, true).apply {
        space = Space.SURFACE
        this.x = x
        this.y = y
    }

    @Test
    fun aRebuildForgetsWhereAntsWere() {
        val index = SpatialIndex()
        val ants = listOf(ant(0, 1000f, 1000f), ant(1, 1005f, 1000f), ant(2, 1000f, 1008f))
        index.rebuild(ants)
        assertEquals(2, index.countNear(ants[0], 10f))
        for (a in ants) a.x += 3000f
        index.rebuild(ants)
        val probe = ant(9, 1000f, 1000f)
        assertEquals(0, index.countNear(probe, 10f))
        assertEquals(2, index.countNear(ants[0], 10f))
    }
}

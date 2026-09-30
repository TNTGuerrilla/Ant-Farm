package com.bydesigninteractive.ant.sim.world

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ExcavationTest {
    private val grid = NestGrid(NestGenerator(3, 256, 200))
    private val plan = NestPlan(3, 128)
    private val excavation = Excavation(grid, plan)

    @Test
    fun theFirstFrontierIsTheTopRowOfTheShaft() {
        val frontier = excavation.frontier()
        assertEquals(0, excavation.block)
        assertTrue(frontier.isNotEmpty())
        assertTrue(frontier.all { it.y == 0 && it.x in 126..130 })
    }

    @Test
    fun movesToTheNextBlockWhenOneIsDug() {
        val r = plan.rect(0)
        for (y in r.y0..r.y1) for (x in r.x0..r.x1) grid.set(x, y, Material.AIR)
        val frontier = excavation.frontier()
        assertEquals(1, excavation.block)
        assertTrue(frontier.all { plan.rect(1).contains(it.x, it.y) })
    }

    @Test
    fun finishesAtTheWaterTable() {
        var guard = 0
        while (!excavation.finished && guard++ < 100_000) {
            for (c in excavation.frontier().toList()) grid.set(c.x, c.y, Material.AIR)
        }
        assertTrue(excavation.finished)
        for (y in grid.generator.waterTable until 200) for (x in 0 until 256) {
            assertTrue(!grid.isAir(x, y))
        }
    }
}

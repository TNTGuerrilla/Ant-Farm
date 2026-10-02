package com.bydesigninteractive.ant.sim.world

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class NestPathsTest {
    private val grid = NestGrid(NestGenerator(3, 256, 256))
    private val paths = NestPaths(grid, Excavation(grid, NestPlan(3, 128)))

    private fun carveL() {
        for (y in 0..20) grid.set(128, y, Material.AIR)
        for (x in 128..140) grid.set(x, 20, Material.AIR)
    }

    @Test
    fun countsStepsToTheEntrance() {
        carveL()
        paths.refresh()
        assertEquals(0, paths.up.get(128, 0))
        assertEquals(20, paths.up.get(128, 20))
        assertEquals(32, paths.up.get(140, 20))
        assertEquals(DistanceMap.UNREACHED, paths.up.get(50, 50))
    }

    @Test
    fun followsNewTunnels() {
        carveL()
        paths.refresh()
        grid.set(140, 21, Material.AIR)
        paths.refresh()
        assertEquals(33, paths.up.get(140, 21))
    }

    @Test
    fun theFrontMapLeadsToTheDiggingFrontier() {
        carveL()
        paths.refresh()
        // The shaft block (x 126..130, y 0..39) is the current block; its frontier touches the carved column.
        assertEquals(0, paths.toFront.get(128, 20))
        assertEquals(DistanceMap.UNREACHED, paths.toFront.get(50, 50))
    }

    @Test
    fun beforeAnyChamberTheRestSpotIsTheFarthestCell() {
        carveL()
        paths.refresh()
        assertEquals(0, paths.toRest.get(140, 20))
        assertEquals(32, paths.toRest.get(128, 0))
    }

    @Test
    fun aDugChamberIsTheRestSpot() {
        val w = com.bydesigninteractive.ant.sim.World(1)
        w.predig(2) // the first shaft and the first chamber
        w.paths.refresh()
        val chamber = w.plan.rect(1)
        assertEquals(0, w.paths.toRest.get(chamber.x0, chamber.y0))
        assertTrue(w.paths.toRest.get(w.nestEntranceX, 0) > 0)
    }
}

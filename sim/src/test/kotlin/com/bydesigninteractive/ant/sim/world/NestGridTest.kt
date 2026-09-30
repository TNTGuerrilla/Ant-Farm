package com.bydesigninteractive.ant.sim.world

import kotlin.math.exp
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class NestGridTest {
    private fun grid() = NestGrid(NestGenerator(7, 256, 256))

    @Test
    fun untouchedTilesHaveNoStorageAndReadTheGenerator() {
        val g = grid()
        for (y in 0 until 256 step 5) for (x in 0 until 256 step 5) {
            assertEquals(g.generator.material(x, y), g.material(x, y))
        }
        assertEquals(0, g.modifiedTileCount())
        assertEquals(0L, g.tileVersion(1, 1))
    }

    @Test
    fun changingACellCreatesOnlyItsTile() {
        val g = grid()
        g.set(70, 10, Material.AIR)
        assertEquals(1, g.modifiedTileCount())
        assertEquals(Material.AIR, g.material(70, 10))
        assertEquals(g.generator.material(71, 10), g.material(71, 10))
        assertTrue(g.tileVersion(1, 0) > 0)
    }

    @Test
    fun outsideTheSliceReadsAsStone() {
        val g = grid()
        assertEquals(Material.STONE, g.material(-1, 0))
        assertEquals(Material.STONE, g.material(256, 0))
        assertEquals(Material.STONE, g.material(0, 256))
    }

    @Test
    fun countsAirAndGrowsItsBounds() {
        val g = grid()
        g.set(10, 5, Material.AIR)
        g.set(20, 30, Material.AIR)
        assertEquals(2, g.airCells)
        assertEquals(10, g.airMinX)
        assertEquals(20, g.airMaxX)
        assertEquals(5, g.airMinY)
        assertEquals(30, g.airMaxY)
        g.set(10, 5, Material.SOIL)
        assertEquals(1, g.airCells)
    }

    @Test
    fun versionOnlyChangesOnRealChanges() {
        val g = grid()
        g.set(3, 3, g.material(3, 3))
        assertEquals(0L, g.version)
        g.set(3, 3, Material.AIR)
        assertEquals(1L, g.version)
    }

    @Test
    fun activeTilesAreAirTilesAndTheirNeighbors() {
        val g = grid()
        assertEquals(0, g.activeTileCount())
        g.set(100, 100, Material.AIR) // tile (1, 1)
        assertEquals(9, g.activeTileCount())
        assertTrue(g.isActive(0, 0))
        assertFalse(g.isActive(3, 3))
    }

    @Test
    fun buildPheromoneDecaysAndReleasesItsTile() {
        val g = grid()
        g.addBuildPheromone(5, 5, 1f)
        assertEquals(1, g.pheromoneTileCount())
        assertTrue(g.isActive(0, 0))
        g.stepPheromone(1f, 1f)
        assertEquals(exp(-1f), g.buildPheromone(5, 5), 1e-5f)
        repeat(10) { g.stepPheromone(1f, 1f) }
        assertEquals(0, g.pheromoneTileCount())
        assertEquals(0f, g.buildPheromone(5, 5))
    }
}

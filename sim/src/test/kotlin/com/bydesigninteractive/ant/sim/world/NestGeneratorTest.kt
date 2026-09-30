package com.bydesigninteractive.ant.sim.world

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class NestGeneratorTest {
    private val gen = NestGenerator(seed = 42, width = 1200, depth = 1000)

    @Test
    fun sameSeedGivesTheSameSoil() {
        val other = NestGenerator(42, 1200, 1000)
        for (y in 0 until 1000 step 7) for (x in 0 until 1200 step 7) {
            assertEquals(gen.material(x, y), other.material(x, y))
        }
    }

    @Test
    fun theWaterTableIsTheFloor() {
        assertTrue(gen.waterTable in 861..940)
        assertEquals(Material.WATER, gen.material(600, gen.waterTable))
        assertEquals(Material.WATER, gen.material(10, 999))
    }

    @Test
    fun mostlySoilWithSomeStoneAndClayAndNoAir() {
        val counts = IntArray(Material.entries.size)
        var total = 0
        for (y in 0 until gen.waterTable step 3) for (x in 0 until 1200 step 3) {
            counts[gen.material(x, y).ordinal]++
            total++
        }
        assertEquals(0, counts[Material.AIR.ordinal])
        assertTrue(counts[Material.SOIL.ordinal] > total * 0.8)
        assertTrue(counts[Material.STONE.ordinal] in (total * 0.002).toInt()..(total * 0.05).toInt())
        assertTrue(counts[Material.CLAY.ordinal] > 0)
    }

    @Test
    fun theEntranceColumnHasNoStones() {
        for (y in 0 until gen.waterTable) for (x in 592..608) {
            assertTrue(gen.material(x, y) != Material.STONE, "stone at ($x, $y)")
        }
    }
}

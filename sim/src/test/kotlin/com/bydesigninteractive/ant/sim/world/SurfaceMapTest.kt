package com.bydesigninteractive.ant.sim.world

import kotlin.math.hypot
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SurfaceMapTest {
    @Test
    fun chunksAreTheSameForTheSameSeed() {
        val a = SurfaceMap(5).chunk(3, 4)
        val b = SurfaceMap(5).chunk(3, 4)
        assertEquals(a.tufts, b.tufts)
        assertEquals(a.stones, b.stones)
        assertNotEquals(a.tufts, SurfaceMap(5).chunk(4, 3).tufts)
    }

    @Test
    fun chunkContentsLieInsideTheirChunk() {
        val c = SurfaceMap(5).chunk(3, 4)
        assertTrue(c.tufts.isNotEmpty())
        for (t in c.tufts) {
            assertTrue(t.x >= 1500f && t.x < 2000f)
            assertTrue(t.y >= 2000f && t.y < 2500f)
        }
    }

    @Test
    fun twoToFourPlantsWithinThreeMeters() {
        val m = SurfaceMap(9)
        m.placePlants()
        assertTrue(m.foods.size in 2..4)
        for (f in m.foods) {
            val d = hypot(f.x - m.entranceX, f.y - m.entranceY)
            assertTrue(d in 300f..3000f, "plant at $d mm")
            assertEquals(FoodKind.HONEYDEW, f.kind)
        }
    }

    @Test
    fun preyHasLimitedLoads() {
        val m = SurfaceMap(9)
        m.placePrey(3)
        assertEquals(3, m.foods.size)
        assertTrue(m.foods.all { it.kind == FoodKind.PREY && it.loads in 1..100 })
    }

    @Test
    fun nearestFoodHonorsTheRange() {
        val m = SurfaceMap(9)
        m.foods += FoodSource(0, FoodKind.HONEYDEW, m.entranceX + 100f, m.entranceY, 15f, 1f)
        assertNotNull(m.nearestFood(m.entranceX + 100f + 15f + 20f, m.entranceY, 25f))
        assertNull(m.nearestFood(m.entranceX + 100f + 15f + 30f, m.entranceY, 25f))
    }
}

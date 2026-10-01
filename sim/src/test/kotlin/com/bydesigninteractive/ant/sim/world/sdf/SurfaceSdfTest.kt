package com.bydesigninteractive.ant.sim.world.sdf

import com.bydesigninteractive.ant.sim.world.CHUNKS
import com.bydesigninteractive.ant.sim.world.FoodKind
import com.bydesigninteractive.ant.sim.world.FoodSource
import com.bydesigninteractive.ant.sim.world.SurfaceMap
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.max
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SurfaceSdfTest {
    /** The chunk food index must give exactly what scanning every food gives. */
    @Test
    fun foodAddedOrRemovedChangesTheSurfaceAtOnce() {
        val m = SurfaceMap(4, rocks = false)
        val sdf = m.sdf
        val x = 4500f
        val y = 4000f
        val g = m.ground.height(x, y)
        val before = sdf.distance(x, y, g + 4f)
        val food = FoodSource(0, FoodKind.PREY, x, y, 10f, 1f, loads = 1, z = g + 2f, bodyRadius = 5f)
        m.addFood(food)
        val with = sdf.distance(x, y, g + 4f)
        assertTrue(with < before - 1f, "food body should be closer than the ground: $with vs $before")
        m.removeFood(food)
        assertEquals(before, sdf.distance(x, y, g + 4f))
    }

    @Test
    fun withoutRocksTheSurfaceIsTheGround() {
        val m = SurfaceMap(3, rocks = false)
        val h = m.ground.height(4200f, 4100f)
        assertEquals(0f, m.sdf.distance(4200f, 4100f, h), 1e-4f)
    }

    @Test
    fun projectionLandsOnTheSurface() {
        val m = SurfaceMap(3)
        val p = floatArrayOf(4321f, 3987f, m.ground.height(4321f, 3987f) + 3f)
        m.sdf.project(p)
        assertTrue(abs(m.sdf.distance(p[0], p[1], p[2])) < 0.1f)
    }

    @Test
    fun theGradientPointsOutOfAPlacedRock() {
        val m = SurfaceMap(3, rocks = false)
        val g = m.ground.height(4300f, 4000f)
        m.sdf.placed += Blob(4300f, 4000f, g + 20f, 20f, 20f, 20f)
        val out = FloatArray(3)
        m.sdf.gradient(4300f, 4000f, g + 45f, out)
        assertTrue(out[2] > 0.99f)
        m.sdf.gradient(4325f, 4000f, g + 20f, out)
        assertTrue(out[0] > 0.99f)
    }

    @Test
    fun noRockIsGeneratedNearTheEntrance() {
        val m = SurfaceMap(3)
        val cx = (m.entranceX / 500f).toInt()
        val cy = (m.entranceY / 500f).toInt()
        for (y in cy - 1..cy + 1) for (x in cx - 1..cx + 1) {
            for (b in m.sdf.ownBlobs(x, y)) {
                assertTrue(hypot(b.cx - m.entranceX, b.cy - m.entranceY) - b.reach >= 30f)
            }
        }
    }

    @Test
    fun noRockCoversFood() {
        val m = SurfaceMap(3)
        var stone: com.bydesigninteractive.ant.sim.world.Stone? = null
        search@ for (cy in 0 until CHUNKS) for (cx in 0 until CHUNKS) {
            val r = m.chunk(cx, cy).rocks.firstOrNull()
            if (r != null) {
                stone = r
                break@search
            }
        }
        val s = assertNotNull(stone)
        val g = m.ground.height(s.x, s.y)
        val food = FoodSource(0, FoodKind.PREY, s.x, s.y, 6f, 1f)
        m.addFood(food)
        assertTrue(m.sdf.distance(food.x, food.y, g + 1f) > 0f)
    }

    @Test
    fun projectionLandsOnALumpyRock() {
        val m = SurfaceMap(3, rocks = false)
        val g = m.ground.height(4300f, 4000f)
        val b = Blob(4300f, 4000f, g + 10f, 30f, 28f, 20f, lump = 0.15f, phase = 1.3f)
        m.sdf.placed += b
        val dirs = listOf(
            floatArrayOf(1f, 0f, 0f), floatArrayOf(0f, 1f, 0f), floatArrayOf(-1f, 0f, 0.2f), floatArrayOf(0f, -1f, 0.3f),
            floatArrayOf(1f, 1f, 1f), floatArrayOf(-1f, 1f, 0.6f), floatArrayOf(0.3f, -1f, 1f), floatArrayOf(0f, 0f, 1f),
        )
        for (d in dirs) {
            val len = Math.sqrt((d[0] * d[0] + d[1] * d[1] + d[2] * d[2]).toDouble()).toFloat()
            val p = floatArrayOf(b.cx + d[0] / len * 40f, b.cy + d[1] / len * 40f, b.cz + d[2] / len * 40f)
            // walk inward along the direction to 3 mm outside the surface
            var step = 0
            while (m.sdf.distance(p[0], p[1], p[2]) > 3f && step++ < 200) {
                p[0] -= d[0] / len * 0.5f
                p[1] -= d[1] / len * 0.5f
                p[2] -= d[2] / len * 0.5f
            }
            m.sdf.project(p)
            val dist = m.sdf.distance(p[0], p[1], p[2])
            assertTrue(abs(dist) < 0.5f, "residual $dist from direction ${d.toList()}")
        }
    }

    @Test
    fun theMapHasPebblesAndSomeLargeRocks() {
        val m = SurfaceMap(3)
        var blobs = 0
        var rockStones = 0
        var biggest = 0f
        for (cy in 0 until CHUNKS) for (cx in 0 until CHUNKS) {
            rockStones += m.chunk(cx, cy).rocks.size
            for (b in m.sdf.ownBlobs(cx, cy)) {
                blobs++
                biggest = max(biggest, max(b.rx, b.ry))
            }
        }
        assertTrue(blobs > 100, "blobs $blobs")
        assertTrue(rockStones in 10..80, "large rocks $rockStones")
        assertTrue(biggest >= 60f, "largest blob $biggest")
    }

    @Test
    fun stemsAreWalkableAndDetected() {
        val m = SurfaceMap(3, rocks = false)
        val g = m.ground.height(4500f, 4000f)
        m.addFood(FoodSource(0, FoodKind.HONEYDEW, 4500f, 4000f, 12f, 1f, z = g + 470f, bodyRadius = 8f, stemRadius = 2.5f, stemBase = g - 5f))
        assertEquals(0f, m.sdf.distance(4502.5f, 4000f, g + 200f), 0.01f)
        assertNotNull(m.sdf.stemAt(4504f, 4000f, g + 200f, 3f))
        assertNull(m.sdf.stemAt(4520f, 4000f, g + 200f, 3f))
    }
}

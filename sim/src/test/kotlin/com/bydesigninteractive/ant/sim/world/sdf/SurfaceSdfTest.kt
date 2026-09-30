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
                assertTrue(hypot(b.cx - m.entranceX, b.cy - m.entranceY) - max(b.rx, b.ry) >= 30f)
            }
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
        m.foods += FoodSource(0, FoodKind.HONEYDEW, 4500f, 4000f, 12f, 1f, z = g + 470f, bodyRadius = 8f, stemRadius = 2.5f, stemBase = g - 5f)
        assertEquals(0f, m.sdf.distance(4502.5f, 4000f, g + 200f), 0.01f)
        assertNotNull(m.sdf.stemAt(4504f, 4000f, g + 200f, 3f))
        assertNull(m.sdf.stemAt(4520f, 4000f, g + 200f, 3f))
    }
}

package com.bydesigninteractive.ant.sim.world.sdf

import com.bydesigninteractive.ant.sim.scenario.Scenarios
import com.bydesigninteractive.ant.sim.world.CHUNK_MM
import java.util.Random
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sqrt
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class OpenGroundTest {
    private val world = Scenarios.starter(5)
    private val sdf = world.surface.sdf
    private val ground = world.surface.ground

    @Test
    fun onOpenGroundTheGroundIsTheSurface() {
        val r = Random(1)
        val slope = FloatArray(2)
        val n = FloatArray(3)
        val minDot = cos(PI / 180.0).toFloat()
        var open = 0
        repeat(5000) {
            val x = 1000f + r.nextFloat() * 6000f
            val y = 1000f + r.nextFloat() * 6000f
            if (!sdf.isOpenGround(x, y)) return@repeat
            open++
            val z = ground.heightAndSlope(x, y, slope)
            assertTrue(abs(sdf.distance(x, y, z)) <= 0.001f, "($x, $y) is not on the surface")
            // The ground is bilinear on a 4 mm grid, so it creases along the grid lines. Within
            // the gradient's 0.25 mm step of a line, the central difference averages the two
            // cells and the reference normal is off by up to about 2 degrees; skip those points.
            if (nearGridLine(x) || nearGridLine(y)) return@repeat
            sdf.gradient(x, y, z, n)
            val len = sqrt(slope[0] * slope[0] + slope[1] * slope[1] + 1f)
            val dot = (-slope[0] * n[0] - slope[1] * n[1] + n[2]) / len
            assertTrue(dot >= minDot, "normal at ($x, $y) is off by more than 1 degree")
        }
        assertTrue(open > 2500, "only $open of 5000 points were open ground")
    }

    private fun nearGridLine(v: Float): Boolean {
        val f = v % GRID_MM
        return f <= GRADIENT_STEP || f >= GRID_MM - GRADIENT_STEP
    }

    @Test
    fun nearAnyShapeItIsNeverOpen() {
        val r = Random(2)
        val ecx = (world.surface.entranceX / CHUNK_MM).toInt()
        val ecy = (world.surface.entranceY / CHUNK_MM).toInt()
        val placed = Blob(world.surface.entranceX + 400f, world.surface.entranceY, ground.height(world.surface.entranceX + 400f, world.surface.entranceY), 6f, 6f, 4f)
        sdf.placed += placed
        val boxes = ArrayList<FloatArray>()
        for (cy in ecy - 3..ecy + 3) for (cx in ecx - 3..ecx + 3) for (b in sdf.ownBlobs(cx, cy)) boxes += floatArrayOf(b.cx, b.cy, b.reach)
        boxes += floatArrayOf(placed.cx, placed.cy, placed.reach)
        for (f in world.surface.foods) boxes += floatArrayOf(f.x, f.y, maxOf(f.body?.reach ?: 0f, f.stem?.radius ?: 0f))
        assertTrue(boxes.size > 20, "too few shapes to test: ${boxes.size}")
        for (box in boxes) repeat(40) {
            val reach = box[2] + 10f
            val x = box[0] + (r.nextFloat() * 2f - 1f) * reach
            val y = box[1] + (r.nextFloat() * 2f - 1f) * reach
            assertFalse(sdf.isOpenGround(x, y), "($x, $y) is within reach of a shape at (${box[0]}, ${box[1]})")
        }
        sdf.placed -= placed
    }

    private companion object {
        const val GRID_MM = 4f
        const val GRADIENT_STEP = 0.25f
    }
}

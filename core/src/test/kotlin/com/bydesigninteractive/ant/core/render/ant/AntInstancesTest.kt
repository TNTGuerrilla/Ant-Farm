package com.bydesigninteractive.ant.core.render.ant

import com.badlogic.gdx.graphics.PerspectiveCamera
import com.badlogic.gdx.utils.GdxNativesLoader
import com.bydesigninteractive.ant.core.engine.AntPose
import com.bydesigninteractive.ant.core.engine.CARRY_PELLET
import com.bydesigninteractive.ant.core.render.AntAnimator
import com.bydesigninteractive.ant.sim.ant.Space
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AntInstancesTest {
    init {
        GdxNativesLoader.load()
    }

    private fun pose(id: Int, x: Float, y: Float = 0f, space: Space = Space.SURFACE) = AntPose().apply {
        this.id = id; this.x = x; this.y = y; this.z = 0f; this.space = space
        fx = 1f; fy = 0f; fz = 0f; nx = 0f; ny = 0f; nz = 1f; crop = 0.5f; carry = CARRY_PELLET
    }

    /** A camera 30 mm above the origin looking along +x with z up, as the chase camera might. No GL, but native matrix code. */
    private fun camera() = PerspectiveCamera(67f, 1600f, 900f).apply {
        near = 2f
        far = 3000f
        position.set(0f, 0f, 30f)
        up.set(0f, 0f, 1f)
        lookAt(100f, 0f, 30f)
        update()
    }

    private fun fill(poses: List<AntPose>, turns: TurnSmoother = TurnSmoother()): AntBatch =
        AntBatch(16).also { AntInstances.fill(poses, AntAnimator(), turns, camera(), it) }

    /** The x of record [k]. */
    private fun AntBatch.x(k: Int) = records[k * AntInstances.FLOATS]

    @Test
    fun antsSplitByDistanceAndNestAntsAreSkipped() {
        val poses = listOf(pose(0, 100f), pose(1, 900f), pose(2, 50f, space = Space.NEST), pose(3, 400f), pose(4, 1300f))
        val b = fill(poses)
        assertEquals(1, b.detailed)
        assertEquals(1, b.shadowedSimple)
        assertEquals(1, b.plainSimple)
        assertEquals(1, b.culledFar)
        assertEquals(0, b.culledView)
        // detailed first, then the shadowed simple ant, then the plain one
        assertEquals(100f, b.x(0))
        assertEquals(400f, b.x(1))
        assertEquals(900f, b.x(2))
        assertEquals(0.5f, b.records[7])
        assertEquals(CARRY_PELLET.toFloat(), b.records[11])
        assertEquals(1f, b.records[4]) // no smoothed forward yet: the pose's own
        assertEquals(AntInstances.scale(0), b.records[12])
        assertEquals(AntInstances.scale(3), b.records[AntInstances.FLOATS + 12])
        assertEquals(AntInstances.scale(1), b.records[2 * AntInstances.FLOATS + 12])
    }

    @Test
    fun antsShrinkToNothingBeforeTheDrawDistance() {
        val b = fill(listOf(pose(0, 1100f), pose(1, 1199f)))
        assertEquals(2, b.simple)
        val half = b.records[12] / AntInstances.scale(0)
        assertEquals(0.5f, half, 0.01f)
        assertTrue(b.records[AntInstances.FLOATS + 12] < 0.01f, "an ant at the edge is nearly gone")
    }

    @Test
    fun antsOutsideTheViewAreCulled() {
        // behind the eye, far off to the side, and one in view at the side
        val b = fill(listOf(pose(0, -100f), pose(1, 100f, 500f), pose(2, 100f, 60f)))
        assertEquals(2, b.culledView)
        assertEquals(1, b.detailed)
        assertEquals(60f, b.records[1])
    }

    @Test
    fun anAntJustOutsideTheViewEdgeIsKeptByItsBoundingSphere() {
        val cam = camera()
        // walk sideways until the ant's centre leaves the frustum; one step later the sphere still touches it
        var y = 0f
        while (cam.frustum.pointInFrustum(100f, y, 0f)) y += 0.5f
        val b = fill(listOf(pose(0, 100f, y + 1f)))
        assertEquals(1, b.detailed)
    }

    @Test
    fun atMostTheNearestHundredAndFiftyAntsAreDetailed() {
        val rnd = Random(3)
        val poses = (0 until 600).map { pose(it, 80f + rnd.nextFloat() * 200f, rnd.nextFloat() * 20f - 10f) }
        val b = fill(poses)
        assertEquals(AntInstances.MAX_DETAILED, b.detailed)
        assertEquals(600 - AntInstances.MAX_DETAILED, b.simple)
        val d = poses.map { (it.x * it.x + it.y * it.y + 900f) }.sorted()
        val cut = d[AntInstances.MAX_DETAILED - 1]
        for (k in 0 until b.detailed) {
            val o = k * AntInstances.FLOATS
            val d2 = b.records[o] * b.records[o] + b.records[o + 1] * b.records[o + 1] + 900f
            assertTrue(d2 <= cut, "detailed ant $k at $d2 is not among the nearest (cut $cut)")
        }
        // and the batch is reused without growing
        val records = b.records
        AntInstances.fill(poses, AntAnimator(), TurnSmoother(), camera(), b)
        assertTrue(records === b.records)
        assertEquals(AntInstances.MAX_DETAILED, b.detailed)
    }

    @Test
    fun theSmoothedForwardIsDrawn() {
        val turns = TurnSmoother()
        val p = pose(0, 100f)
        turns.observe(p, 1f / 60f)
        p.fx = -1f // a flip in one tick
        turns.observe(p, 1f / 60f)
        val b = fill(listOf(p), turns)
        assertTrue(b.records[4] > 0.9f, "drawn forward x ${b.records[4]} turns gradually")
    }

    @Test
    fun antsAreThreeToFiveMillimetresLongAndKeepTheirSize() {
        val lengths = (0 until 2000).map { AntInstances.bodyLength(it) }
        assertTrue(lengths.all { it in 3f..5f })
        assertTrue(lengths.min() < 3.05f && lengths.max() > 4.95f)
        assertEquals(4f, lengths.average().toFloat(), 0.05f)
        assertEquals(AntInstances.bodyLength(7), AntInstances.bodyLength(7))
        assertEquals(AntInstances.bodyLength(7) / AntInstances.MODEL_LENGTH_MM, AntInstances.scale(7), 1e-6f)
    }
}

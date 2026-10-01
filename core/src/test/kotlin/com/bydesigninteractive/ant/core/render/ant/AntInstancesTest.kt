package com.bydesigninteractive.ant.core.render.ant

import com.bydesigninteractive.ant.core.engine.AntPose
import com.bydesigninteractive.ant.core.engine.CARRY_PELLET
import com.bydesigninteractive.ant.core.render.AntAnimator
import com.bydesigninteractive.ant.sim.ant.Space
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AntInstancesTest {
    private fun pose(id: Int, x: Float, space: Space = Space.SURFACE) = AntPose().apply {
        this.id = id; this.x = x; this.y = 0f; this.z = 0f; this.space = space
        fx = 1f; fy = 0f; fz = 0f; nx = 0f; ny = 0f; nz = 1f; crop = 0.5f; carry = CARRY_PELLET
    }

    @Test
    fun antsSplitByDistanceAndNestAntsAreSkipped() {
        val poses = listOf(pose(0, 100f), pose(1, 900f), pose(2, 50f, Space.NEST))
        val near = FloatArray(64)
        val far = FloatArray(64)
        val counts = IntArray(2)
        AntInstances.fill(poses, AntAnimator(), TurnSmoother(), 0f, 0f, 30f, near, far, counts)
        assertEquals(1, counts[0])
        assertEquals(1, counts[1])
        assertEquals(100f, near[0])
        assertEquals(0.5f, near[7])
        assertEquals(CARRY_PELLET.toFloat(), near[11])
        assertEquals(900f, far[0])
        assertEquals(1f, near[4]) // no smoothed forward yet: the pose's own
        assertEquals(AntInstances.scale(0), near[12])
        assertEquals(AntInstances.scale(1), far[12])
    }

    @Test
    fun theSmoothedForwardIsDrawn() {
        val turns = TurnSmoother()
        val p = pose(0, 100f)
        turns.observe(p, 1f / 60f)
        p.fx = -1f // a flip in one tick
        turns.observe(p, 1f / 60f)
        val near = FloatArray(64)
        val counts = IntArray(2)
        AntInstances.fill(listOf(p), AntAnimator(), turns, 0f, 0f, 30f, near, FloatArray(64), counts)
        assertTrue(near[4] > 0.9f, "drawn forward x ${near[4]} turns gradually")
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

package com.bydesigninteractive.ant.core.render.ant

import com.bydesigninteractive.ant.core.engine.AntPose
import com.bydesigninteractive.ant.core.engine.CARRY_PELLET
import com.bydesigninteractive.ant.core.render.AntAnimator
import com.bydesigninteractive.ant.sim.ant.Space
import kotlin.test.Test
import kotlin.test.assertEquals

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
        AntInstances.fill(poses, AntAnimator(), 0f, 0f, 30f, near, far, counts)
        assertEquals(1, counts[0])
        assertEquals(1, counts[1])
        assertEquals(100f, near[0])
        assertEquals(0.5f, near[7])
        assertEquals(CARRY_PELLET.toFloat(), near[11])
        assertEquals(900f, far[0])
    }
}

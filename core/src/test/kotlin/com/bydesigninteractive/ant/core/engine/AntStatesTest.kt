package com.bydesigninteractive.ant.core.engine

import com.bydesigninteractive.ant.sim.ant.Space
import kotlin.test.Test
import kotlin.test.assertEquals

class AntStatesTest {
    private fun snapshot(tick: Long, at: Long, x: Float, space: Byte = 1): Snapshot = Snapshot().apply {
        ensure(1)
        count = 1
        this.tick = tick
        publishedAt = at
        this.x[0] = x
        this.space[0] = space
        fx[0] = 1f
        nz[0] = 1f
    }

    @Test
    fun blendsBetweenTheLastTwoTicks() {
        val st = AntStates()
        st.accept(snapshot(1, 0, 10f))
        st.accept(snapshot(2, 50, 20f))
        val p = AntPose()
        st.blend(0, 0.25f, p)
        assertEquals(12.5f, p.x, 1e-5f)
        assertEquals(Space.SURFACE, p.space)
    }

    @Test
    fun alphaIsClampedToOneTick() {
        val st = AntStates()
        st.accept(snapshot(1, 1000, 0f))
        assertEquals(0f, st.alpha(900, 50), 1e-6f)
        assertEquals(0.5f, st.alpha(1025, 50), 1e-6f)
        assertEquals(1f, st.alpha(2000, 50), 1e-6f)
    }

    @Test
    fun noBlendAcrossASpaceChangeOrForANewAnt() {
        val st = AntStates()
        st.accept(snapshot(1, 0, 10f, space = 0))
        st.accept(snapshot(2, 50, 20f, space = 1))
        val p = AntPose()
        st.blend(0, 0.25f, p)
        assertEquals(20f, p.x)
    }
}

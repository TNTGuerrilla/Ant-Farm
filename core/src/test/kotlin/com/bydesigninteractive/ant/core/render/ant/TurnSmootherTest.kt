package com.bydesigninteractive.ant.core.render.ant

import com.bydesigninteractive.ant.core.engine.AntPose
import com.bydesigninteractive.ant.sim.ant.Space
import kotlin.math.cos
import kotlin.math.sin
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TurnSmootherTest {
    private val dt = 1f / 60f

    private fun pose(x: Float, fx: Float, fy: Float, space: Space = Space.SURFACE) = AntPose().apply {
        id = 3; this.x = x; y = 0f; z = 0f; this.space = space
        this.fx = fx; this.fy = fy; fz = 0f; nx = 0f; ny = 0f; nz = 1f
    }

    @Test
    fun aFlipSwingsRoundInAQuarterSecond() {
        val s = TurnSmoother()
        s.observe(pose(0f, 1f, 0f), dt)
        var x = 0f
        repeat(12) { // 0.2 s: 144 degrees at 720 degrees per second
            x += 0.1f
            s.observe(pose(x, -1f, 0f), dt)
        }
        assertTrue(s.fx(3, 0f) > -0.85f, "after 0.2 s the flip is not finished: fx ${s.fx(3, 0f)}")
        repeat(4) { // 0.267 s in all
            x += 0.1f
            s.observe(pose(x, -1f, 0f), dt)
        }
        assertEquals(-1f, s.fx(3, 0f), 1e-4f)
        assertEquals(0f, s.fy(3, 1f), 1e-4f)
    }

    @Test
    fun aSmallTurnIsFollowedAtOnce() {
        val s = TurnSmoother()
        s.observe(pose(0f, 1f, 0f), dt)
        val a = Math.toRadians(10.0).toFloat()
        s.observe(pose(0.1f, cos(a), sin(a)), dt)
        assertEquals(cos(a), s.fx(3, 0f), 1e-5f)
        assertEquals(sin(a), s.fy(3, 0f), 1e-5f)
    }

    @Test
    fun teleportsAndReturnsFromTheNestSnap() {
        val s = TurnSmoother()
        s.observe(pose(0f, 1f, 0f), dt)
        s.observe(pose(100f, -1f, 0f), dt) // a 100 mm jump
        assertEquals(-1f, s.fx(3, 0f), 1e-5f)
        s.observe(pose(100f, -1f, 0f, Space.NEST), dt)
        s.observe(pose(100f, 1f, 0f), dt)
        assertEquals(1f, s.fx(3, 0f), 1e-5f)
    }

    @Test
    fun unseenAntsUseTheFallback() {
        val s = TurnSmoother()
        assertEquals(0.5f, s.fx(1000, 0.5f))
    }
}

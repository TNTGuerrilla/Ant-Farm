package com.bydesigninteractive.ant.sim.ant

import com.bydesigninteractive.ant.sim.World
import com.bydesigninteractive.ant.sim.world.sdf.Blob
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SurfaceWalkTest {
    private fun world(): World {
        val w = World(3, rocks = false)
        w.predig(1)
        return w
    }

    private fun walker(w: World, x: Float, y: Float, heading: Float): Ant {
        val a = w.addAnt(Role.FORAGER)
        a.space = Space.SURFACE
        SurfaceWalk.place(w, a, x, y, heading)
        return a
    }

    private fun gap(w: World, a: Ant) = abs(w.surface.sdf.distance(a.x, a.y, a.z))

    @Test
    fun placedAntsSitOnTheGroundFacingTheirHeading() {
        val w = world()
        val a = walker(w, 4300f, 4000f, 0.3f)
        assertTrue(gap(w, a) < 0.5f)
        assertEquals(0.3f, a.heading, 0.05f)
        assertTrue(a.nz > 0.9f)
    }

    @Test
    fun turningLeftFollowsTheCompass() {
        val w = world()
        val a = walker(w, 4300f, 4000f, 0f)
        SurfaceWalk.turn(a, (PI / 2).toFloat())
        assertEquals((PI / 2).toFloat(), a.heading, 0.1f)
    }

    @Test
    fun anAntWalksOverARockAndBackDown() {
        val w = world()
        val ground = w.surface.ground.height(4300f, 4000f)
        w.surface.sdf.placed += Blob(4300f, 4000f, ground + 5f, 25f, 25f, 25f)
        val a = walker(w, 4200f, 4000f, 0f)
        var top = -1e9f
        repeat(400) {
            SurfaceWalk.step(w, a, 20f)
            assertTrue(gap(w, a) < 0.5f, "left the surface at (${a.x}, ${a.y}, ${a.z})")
            top = max(top, a.z - ground)
        }
        assertTrue(top > 25f, "highest point $top")
        assertTrue(a.x > 4350f, "ended at x ${a.x}")
        assertTrue(a.z - w.surface.ground.height(a.x, a.y) < 2f)
    }

    @Test
    fun walkersStayOnTheSurfaceIncludingUnderAnOverhang() {
        val w = world()
        val ground = w.surface.ground.height(4300f, 4000f)
        w.surface.sdf.placed += Blob(4300f, 4000f, ground + 25f, 30f, 30f, 30f)
        val ants = List(40) { i ->
            val angle = i * 0.157f
            walker(w, 4300f + cos(angle) * 45f, 4000f + sin(angle) * 45f, angle + PI.toFloat())
        }
        var underside = 0
        repeat(20 * 60 * 3) {
            for (a in ants) {
                SurfaceWalk.wander(w, a, 20f, 0.9f)
                SurfaceWalk.step(w, a, 20f)
                assertTrue(gap(w, a) < 0.5f, "left the surface at (${a.x}, ${a.y}, ${a.z})")
                if (a.nz < -0.3f) underside++
            }
        }
        assertTrue(underside > 0, "nobody walked the underside")
    }

    private fun pose(fx: Float, fy: Float, fz: Float, nx: Float, ny: Float, nz: Float, heading: Float): Ant {
        val a = Ant(0, Role.FORAGER, 0f, false)
        a.fx = fx
        a.fy = fy
        a.fz = fz
        a.nx = nx
        a.ny = ny
        a.nz = nz
        a.heading = heading
        return a
    }

    @Test
    fun atAWallTheFoldFallbackHeadsUp() {
        // Walking east into a wall whose normal faces west.
        val a = pose(1f, 0f, 0f, -1f, 0f, 0f, 0f)
        SurfaceWalk.transport(a)
        assertEquals(1f, a.fz, 1e-4f)
    }

    @Test
    fun overAConvexEdgeTheFoldFallbackHeadsDown() {
        // Walking east off the top of a block whose side faces east.
        val a = pose(1f, 0f, 0f, 1f, 0f, 0f, 0f)
        SurfaceWalk.transport(a)
        assertEquals(-1f, a.fz, 1e-4f)
    }

    @Test
    fun faceCompassKeepsTheHeadingExactOnASlope() {
        val len = sqrt(0.5f * 0.5f + 0.3f * 0.3f + 1f)
        val a = pose(1f, 0f, 0f, -0.5f / len, 0.3f / len, 1f / len, 0f)
        for (angle in listOf(0.4f, 1.9f, -2.5f)) {
            SurfaceWalk.faceCompass(a, angle)
            assertEquals(angle, atan2(a.fy, a.fx), 1e-5f)
            assertEquals(angle, a.heading, 1e-5f)
            assertEquals(0f, a.fx * a.nx + a.fy * a.ny + a.fz * a.nz, 1e-5f)
            assertEquals(1f, sqrt(a.fx * a.fx + a.fy * a.fy + a.fz * a.fz), 1e-5f)
        }
    }

    @Test
    fun sensingReadsTheTrailAheadLeftAndRight() {
        val w = world()
        val a = walker(w, 4300f, 4000f, 0f)
        val left = w.params.senseAngle
        val x = a.x + cos(left) * w.params.senseAhead
        val y = a.y + sin(left) * w.params.senseAhead
        w.surface.trail.add(x, y, w.surface.ground.height(x, y), 5f)
        SurfaceWalk.sense(w, a, w.surface.trail)
        assertTrue(a.senseL > a.senseR)
    }
}

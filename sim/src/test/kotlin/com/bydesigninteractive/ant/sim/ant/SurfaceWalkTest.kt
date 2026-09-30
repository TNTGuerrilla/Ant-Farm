package com.bydesigninteractive.ant.sim.ant

import com.bydesigninteractive.ant.sim.World
import com.bydesigninteractive.ant.sim.world.sdf.Blob
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin
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

package com.bydesigninteractive.ant.core.camera

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ChaseCamera3Test {
    private fun flat(c: ChaseCamera3, x: Float, y: Float, fx: Float, fy: Float) =
        c.update(x, y, 0f, fx, fy, 0f, 0f, 0f, 1f, 0.05f)

    @Test
    fun sitsBehindAndAboveTheAnt() {
        val c = ChaseCamera3()
        flat(c, 100f, 0f, 1f, 0f)
        assertEquals(30f, c.eyeX, 1e-4f)
        assertEquals(35f, c.eyeZ, 1e-4f)
        assertEquals(130f, c.targetX, 1e-4f)
    }

    @Test
    fun turningInPlaceDoesNotSwingTheCamera() {
        val c = ChaseCamera3()
        flat(c, 0f, 0f, 1f, 0f)
        repeat(40) { flat(c, 0f, 0f, 0f, 1f) }
        assertEquals(-70f, c.eyeX, 1e-4f)
        assertEquals(0f, c.eyeY, 1e-4f)
    }

    @Test
    fun movingOffBringsTheCameraBehind() {
        val c = ChaseCamera3()
        flat(c, 0f, 0f, 1f, 0f)
        var y = 0f
        repeat(80) {
            y += 1f
            flat(c, 0f, y, 0f, 1f)
        }
        assertTrue(c.eyeY < y - 65f, "eye y ${c.eyeY}, ant y $y")
    }

    @Test
    fun aTeleportSnapsInsteadOfSwinging() {
        val c = ChaseCamera3()
        flat(c, 0f, 0f, 1f, 0f)
        flat(c, 1000f, 0f, 0f, 1f)
        assertEquals(1000f, c.eyeX, 1e-4f)
        assertEquals(-70f, c.eyeY, 1e-4f)
    }

    @Test
    fun upFollowsTheSurfaceWhenMoving() {
        val c = ChaseCamera3()
        c.update(0f, 0f, 0f, 0f, 0f, 1f, -1f, 0f, 0f, 0.05f) // on a wall facing -x, walking up
        assertEquals(-1f, c.upX, 1e-4f)
        assertEquals(-70f, c.eyeZ, 1e-4f)
    }

    @Test
    fun upEasesIntoASuddenTilt() {
        val c = ChaseCamera3()
        c.update(0f, 0f, 0f, 1f, 0f, 0f, 0f, 0f, 1f, 0.05f)
        // The ant steps onto a wall: its normal turns 90 degrees from +z to -x; its forward is now +z.
        fun angleToTarget() = Math.toDegrees(kotlin.math.acos((-c.upX).coerceIn(-1f, 1f).toDouble()))
        var x = 0f
        var t = 0f
        val dt = 1f / 60f
        while (t < 0.1f - 1e-4f) {
            x += 0.5f
            c.update(x, 0f, 0f, 0f, 0f, 1f, -1f, 0f, 0f, dt)
            t += dt
        }
        assertTrue(angleToTarget() > 45.0, "after 0.1 s the up is ${angleToTarget()} degrees from the normal")
        while (t < 2f - 1e-4f) {
            x += 0.5f
            c.update(x, 0f, 0f, 0f, 0f, 1f, -1f, 0f, 0f, dt)
            t += dt
        }
        assertTrue(angleToTarget() < 9.0, "after 2 s the up is ${angleToTarget()} degrees from the normal")
        val dot = c.upX * (c.targetX - x) + c.upY * c.targetY + c.upZ * c.targetZ
        assertEquals(0f, dot, 1e-3f) // the look direction stays perpendicular to the up
    }

    @Test
    fun anOppositeNormalStillTurnsTheUp() {
        val c = ChaseCamera3()
        c.update(0f, 0f, 0f, 1f, 0f, 0f, 0f, 0f, 1f, 0.05f)
        var x = 0f
        repeat(300) {
            x += 0.5f
            c.update(x, 0f, 0f, 1f, 0f, 0f, 0f, 0f, -1f, 1f / 60f)
        }
        assertTrue(c.upZ < -0.9f, "up z ${c.upZ}")
        assertTrue(!c.upX.isNaN() && !c.eyeX.isNaN())
    }
}

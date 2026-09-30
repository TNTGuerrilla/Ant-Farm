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
}

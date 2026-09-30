package com.bydesigninteractive.ant.core.camera

import kotlin.math.PI
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ChaseCameraTest {
    @Test
    fun turningInPlaceDoesNotSwingTheCamera() {
        val c = ChaseCamera()
        c.update(0f, 0f, 0f, 0.05f)
        repeat(40) { c.update(0f, 0f, 1.5f, 0.05f) }
        assertEquals(0f, c.yaw)
    }

    @Test
    fun movingOffBringsTheCameraBehindTheAnt() {
        val c = ChaseCamera()
        c.update(0f, 0f, 0f, 0.05f)
        var y = 0f
        repeat(80) {
            y += 1f // 20 mm/s north
            c.update(0f, y, (PI / 2).toFloat(), 0.05f)
        }
        assertTrue(abs(angleDiff(c.yaw, (PI / 2).toFloat())) < 0.1f, "yaw ${c.yaw}")
    }

    @Test
    fun sitsBehindTheAntAlongItsYaw() {
        val c = ChaseCamera(distance = 70f)
        c.update(100f, 0f, 0f, 0.05f)
        assertEquals(30f, c.x, 1e-4f)
        assertEquals(0f, c.y, 1e-4f)
        assertEquals(100f + c.lookAhead, c.targetX, 1e-4f)
    }

    @Test
    fun easesTheShortWayAroundThePole() {
        val c = ChaseCamera()
        c.update(0f, 0f, 3.1f, 0.05f)
        var x = 0f
        repeat(10) {
            x -= 1f
            c.update(x, 0f, -3.1f, 0.05f)
        }
        assertTrue(abs(c.yaw) > 3f, "yaw ${c.yaw} swung through zero")
    }
}

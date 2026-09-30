package com.bydesigninteractive.ant.core.camera

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.hypot
import kotlin.math.sin

/**
 * A third-person chase camera over the surface map, in millimeters. An ant turning in place
 * does not swing it; once the ant moves off, the camera's yaw eases around behind it with lag.
 * The eye sits [distance] behind the ant along the yaw, [height] above the ground, and looks at
 * a point [lookAhead] in front of the ant.
 */
class ChaseCamera(
    val distance: Float = 70f,
    val height: Float = 35f,
    val lookAhead: Float = 30f,
    private val followRate: Float = 2.5f,
    private val moveThreshold: Float = 2f,
) {
    var yaw = 0f
        private set
    var x = 0f
        private set
    var y = 0f
        private set
    var focusX = 0f
        private set
    var focusY = 0f
        private set
    var placed = false
        private set

    val targetX: Float get() = focusX + cos(yaw) * lookAhead
    val targetY: Float get() = focusY + sin(yaw) * lookAhead

    fun update(antX: Float, antY: Float, antHeading: Float, dt: Float) {
        if (!placed) {
            yaw = antHeading
            focusX = antX
            focusY = antY
            placed = true
        } else if (dt > 0f) {
            val speed = hypot(antX - focusX, antY - focusY) / dt
            if (speed > moveThreshold) {
                yaw += angleDiff(antHeading, yaw) * (1f - exp(-followRate * dt))
                yaw = angleDiff(yaw, 0f)
            }
            focusX = antX
            focusY = antY
        }
        x = focusX - cos(yaw) * distance
        y = focusY - sin(yaw) * distance
    }
}

/** The signed difference a - b wrapped into [-PI, PI]. */
fun angleDiff(a: Float, b: Float): Float {
    var d = (a - b) % TWO_PI
    if (d > PI) d -= TWO_PI
    if (d < -PI) d += TWO_PI
    return d
}

private const val TWO_PI = (2 * PI).toFloat()

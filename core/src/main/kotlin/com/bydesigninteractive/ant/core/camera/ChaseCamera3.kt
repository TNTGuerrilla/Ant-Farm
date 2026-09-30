package com.bydesigninteractive.ant.core.camera

import kotlin.math.exp
import kotlin.math.sqrt

/**
 * A third-person chase camera on the 3D surface, in surface millimeters (z up). It keeps a
 * smoothed forward and up; an ant turning in place does not swing it, and once the ant moves off
 * both ease toward the ant's own forward and normal with lag. The eye sits [distance] behind
 * along the forward and [height] above along the up, looking at a point [lookAhead] ahead.
 * A jump of more than [snapDistance] in one update (an entrance handoff) snaps instead of easing.
 */
class ChaseCamera3(
    val distance: Float = 70f,
    val height: Float = 35f,
    val lookAhead: Float = 30f,
    private val followRate: Float = 2.5f,
    private val moveThreshold: Float = 2f,
    private val snapDistance: Float = 50f,
) {
    private var px = 0f
    private var py = 0f
    private var pz = 0f
    private var fx = 1f
    private var fy = 0f
    private var fz = 0f

    var upX = 0f
        private set
    var upY = 0f
        private set
    var upZ = 1f
        private set
    var placed = false
        private set

    val eyeX: Float get() = px - fx * distance + upX * height
    val eyeY: Float get() = py - fy * distance + upY * height
    val eyeZ: Float get() = pz - fz * distance + upZ * height
    val targetX: Float get() = px + fx * lookAhead
    val targetY: Float get() = py + fy * lookAhead
    val targetZ: Float get() = pz + fz * lookAhead

    fun update(ax: Float, ay: Float, az: Float, afx: Float, afy: Float, afz: Float, anx: Float, any: Float, anz: Float, dt: Float) {
        val dx = ax - px
        val dy = ay - py
        val dz = az - pz
        val jump = sqrt(dx * dx + dy * dy + dz * dz)
        if (!placed || jump > snapDistance) {
            fx = afx
            fy = afy
            fz = afz
            upX = anx
            upY = any
            upZ = anz
            placed = true
        } else if (dt > 0f && jump / dt > moveThreshold) {
            val k = 1f - exp(-followRate * dt)
            fx += (afx - fx) * k
            fy += (afy - fy) * k
            fz += (afz - fz) * k
            upX += (anx - upX) * k
            upY += (any - upY) * k
            upZ += (anz - upZ) * k
        }
        px = ax
        py = ay
        pz = az
        orthonormalize()
    }

    /** Keeps up a unit vector and forward a unit vector perpendicular to it. */
    private fun orthonormalize() {
        var len = sqrt(upX * upX + upY * upY + upZ * upZ)
        if (len > 1e-6f) {
            upX /= len
            upY /= len
            upZ /= len
        }
        val d = fx * upX + fy * upY + fz * upZ
        val gx = fx - upX * d
        val gy = fy - upY * d
        val gz = fz - upZ * d
        len = sqrt(gx * gx + gy * gy + gz * gz)
        if (len > 1e-6f) {
            fx = gx / len
            fy = gy / len
            fz = gz / len
        }
    }
}

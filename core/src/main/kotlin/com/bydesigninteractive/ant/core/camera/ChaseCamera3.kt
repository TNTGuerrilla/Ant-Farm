package com.bydesigninteractive.ant.core.camera

import kotlin.math.exp
import kotlin.math.sqrt

/**
 * A third-person chase camera on the 3D surface, in surface millimeters (z up). It keeps a
 * smoothed forward and up; an ant turning in place does not swing it, and once the ant moves off
 * the forward eases toward the ant's own forward with lag. The up (so the camera's roll and
 * pitch) always eases toward the ant's normal with the time constant [upSeconds], so a step onto
 * a wall or a stem tilts the view gradually instead of at once. The forward is kept perpendicular
 * to the smoothed up. The eye sits [distance] behind along the forward and [height] above along
 * the up, looking at a point [lookAhead] ahead. A jump of more than [snapDistance] in one update
 * (an entrance handoff) snaps instead of easing.
 */
class ChaseCamera3(
    val distance: Float = 70f,
    val height: Float = 35f,
    val lookAhead: Float = 30f,
    private val followRate: Float = 2.5f,
    private val moveThreshold: Float = 2f,
    private val snapDistance: Float = 50f,
    private val upSeconds: Float = 0.6f,
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
        } else {
            if (dt > 0f && jump / dt > moveThreshold) {
                val k = 1f - exp(-followRate * dt)
                fx += (afx - fx) * k
                fy += (afy - fy) * k
                fz += (afz - fz) * k
            }
            if (dt > 0f) easeUp(anx, any, anz, 1f - exp(-dt / upSeconds))
        }
        px = ax
        py = ay
        pz = az
        orthonormalize(afx, afy, afz)
    }

    /**
     * Moves the up a fraction [k] of the way toward the normal (nx, ny, nz); [orthonormalize]
     * renormalizes it. When the two are nearly opposite, the plain blend would pass through zero,
     * so the up turns first toward the part of the normal perpendicular to it, or toward the
     * forward if that part is tiny too.
     */
    private fun easeUp(nx: Float, ny: Float, nz: Float, k: Float) {
        var tx = nx
        var ty = ny
        var tz = nz
        val d = upX * tx + upY * ty + upZ * tz
        if (d < OPPOSITE) {
            tx -= upX * d
            ty -= upY * d
            tz -= upZ * d
            val len = sqrt(tx * tx + ty * ty + tz * tz)
            if (len > 1e-3f) {
                tx /= len
                ty /= len
                tz /= len
            } else {
                tx = fx
                ty = fy
                tz = fz
            }
        }
        upX += (tx - upX) * k
        upY += (ty - upY) * k
        upZ += (tz - upZ) * k
    }

    /**
     * Keeps up a unit vector and forward a unit vector perpendicular to it. If the forward lies
     * along the up, the ant's forward (afx, afy, afz) projected into the up's plane replaces it.
     */
    private fun orthonormalize(afx: Float, afy: Float, afz: Float) {
        var len = sqrt(upX * upX + upY * upY + upZ * upZ)
        if (len > 1e-6f) {
            upX /= len
            upY /= len
            upZ /= len
        } else {
            upX = 0f
            upY = 0f
            upZ = 1f
        }
        for (pass in 0 until 2) {
            val ox = if (pass == 0) fx else afx
            val oy = if (pass == 0) fy else afy
            val oz = if (pass == 0) fz else afz
            val d = ox * upX + oy * upY + oz * upZ
            val gx = ox - upX * d
            val gy = oy - upY * d
            val gz = oz - upZ * d
            len = sqrt(gx * gx + gy * gy + gz * gz)
            if (len > 1e-4f) {
                fx = gx / len
                fy = gy / len
                fz = gz / len
                return
            }
        }
    }

    private companion object {
        /** Below this cosine between up and the target normal, [easeUp] turns through a perpendicular. */
        const val OPPOSITE = -0.95f
    }
}

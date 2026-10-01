package com.bydesigninteractive.ant.core.render.ant

import com.bydesigninteractive.ant.core.engine.AntPose
import com.bydesigninteractive.ant.sim.ant.Space
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Keeps a drawn forward per surface ant (by id) that turns toward the pose's forward at no more
 * than [maxDegreesPerSecond], within the tangent plane of the pose's normal. The simulation can
 * reverse an ant in one tick; drawn as is, that is a snap, so the model swings round instead (a
 * 180 degree flip takes 0.25 s at 720 degrees per second), while a small turn is followed at
 * once. An ant seen for the first time, back from the nest, or moved more than [SNAP_MM] in one
 * frame (a teleport) takes the pose's forward directly. Pure, so it unit-tests on the JVM.
 */
class TurnSmoother(maxDegreesPerSecond: Float = 720f) {
    private val maxRate = Math.toRadians(maxDegreesPerSecond.toDouble()).toFloat()
    private var fx = FloatArray(INITIAL)
    private var fy = FloatArray(INITIAL)
    private var fz = FloatArray(INITIAL)
    private var lastX = FloatArray(INITIAL)
    private var lastY = FloatArray(INITIAL)
    private var lastZ = FloatArray(INITIAL)
    private var seen = BooleanArray(INITIAL)

    /** Turns ant [p]'s drawn forward toward its pose forward for a frame of [dt] seconds. Nest ants are forgotten, so they snap on return. */
    fun observe(p: AntPose, dt: Float) {
        grow(p.id)
        val i = p.id
        if (p.space != Space.SURFACE) {
            seen[i] = false
            return
        }
        val dx = p.x - lastX[i]
        val dy = p.y - lastY[i]
        val dz = p.z - lastZ[i]
        lastX[i] = p.x
        lastY[i] = p.y
        lastZ[i] = p.z
        val nx = p.nx
        val ny = p.ny
        val nz = p.nz
        // The target: the pose forward, in the normal's plane.
        var tx = p.fx
        var ty = p.fy
        var tz = p.fz
        var d = tx * nx + ty * ny + tz * nz
        tx -= nx * d; ty -= ny * d; tz -= nz * d
        var len = sqrt(tx * tx + ty * ty + tz * tz)
        if (len < 1e-6f) {
            set(i, p.fx, p.fy, p.fz)
            seen[i] = true
            return
        }
        tx /= len; ty /= len; tz /= len
        if (!seen[i] || dx * dx + dy * dy + dz * dz > SNAP_MM * SNAP_MM) {
            set(i, tx, ty, tz)
            seen[i] = true
            return
        }
        // The drawn forward, in the same plane.
        var cx = fx[i]
        var cy = fy[i]
        var cz = fz[i]
        d = cx * nx + cy * ny + cz * nz
        cx -= nx * d; cy -= ny * d; cz -= nz * d
        len = sqrt(cx * cx + cy * cy + cz * cz)
        if (len < 1e-6f) {
            set(i, tx, ty, tz)
            return
        }
        cx /= len; cy /= len; cz /= len
        // Signed angle from drawn to target about the normal.
        val sx = cy * tz - cz * ty
        val sy = cz * tx - cx * tz
        val sz = cx * ty - cy * tx
        val angle = atan2(sx * nx + sy * ny + sz * nz, cx * tx + cy * ty + cz * tz)
        val step = maxRate * dt
        if (abs(angle) <= step) {
            set(i, tx, ty, tz)
            return
        }
        val a = if (angle >= 0f) step else -step
        // Rotate the drawn forward by a about the normal: c cos a + (n x c) sin a, as c is perpendicular to n.
        val px = ny * cz - nz * cy
        val py = nz * cx - nx * cz
        val pz = nx * cy - ny * cx
        val ca = cos(a)
        val sa = sin(a)
        set(i, cx * ca + px * sa, cy * ca + py * sa, cz * ca + pz * sa)
    }

    /** Ant [id]'s drawn forward x; [fallback] if it has not been observed on the surface. */
    fun fx(id: Int, fallback: Float): Float = if (id < seen.size && seen[id]) fx[id] else fallback

    fun fy(id: Int, fallback: Float): Float = if (id < seen.size && seen[id]) fy[id] else fallback

    fun fz(id: Int, fallback: Float): Float = if (id < seen.size && seen[id]) fz[id] else fallback

    private fun set(i: Int, x: Float, y: Float, z: Float) {
        fx[i] = x
        fy[i] = y
        fz[i] = z
    }

    private fun grow(id: Int) {
        if (id < seen.size) return
        val c = maxOf(id + 1, seen.size * 2)
        fx = fx.copyOf(c)
        fy = fy.copyOf(c)
        fz = fz.copyOf(c)
        lastX = lastX.copyOf(c)
        lastY = lastY.copyOf(c)
        lastZ = lastZ.copyOf(c)
        seen = seen.copyOf(c)
    }

    companion object {
        /** A move longer than this (mm) in one frame is a teleport, not walking. */
        const val SNAP_MM = 25f
        private const val INITIAL = 256
    }
}

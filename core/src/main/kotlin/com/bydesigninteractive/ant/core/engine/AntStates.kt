package com.bydesigninteractive.ant.core.engine

import com.bydesigninteractive.ant.sim.ant.AntState
import com.bydesigninteractive.ant.sim.ant.Role
import com.bydesigninteractive.ant.sim.ant.Space
import kotlin.math.PI
import kotlin.math.sqrt

/** One ant as drawn this frame (render thread). */
class AntPose {
    var id = 0
    var space = Space.NEST
    var role = Role.FORAGER
    var state = AntState.IDLE
    var speed = 0f
    var heading = 0f
    var x = 0f
    var y = 0f
    var z = 0f
    var fx = 1f
    var fy = 0f
    var fz = 0f
    var nx = 0f
    var ny = 0f
    var nz = 1f
    var crop = 0f
    var carry = CARRY_NONE
}

/**
 * The render thread's copy of the last two snapshots, for drawing ants between ticks the way
 * Minecraft's client interpolates entities. Ants that are new, or changed space between the two
 * snapshots, are drawn where they are now.
 */
class AntStates {
    var count = 0
        private set
    var tick = 0L
        private set
    var foods: List<FoodView> = emptyList()
        private set
    var airCells = 0
        private set
    var airMinX = 0
        private set
    var airMinY = 0
        private set
    var airMaxX = 0
        private set
    var airMaxY = 0
        private set
    private var publishedAt = 0L
    private var prevCount = 0
    private var cur = Arrays()
    private var prev = Arrays()

    private class Arrays {
        var id = IntArray(0)
        var space = ByteArray(0)
        var role = ByteArray(0)
        var state = ByteArray(0)
        var crop = FloatArray(0)
        var carry = ByteArray(0)
        var f = FloatArray(0) // 11 floats per ant: speed, heading, x, y, z, fx, fy, fz, nx, ny, nz

        fun ensure(n: Int) {
            if (id.size >= n) return
            val c = maxOf(n, id.size * 2, 64)
            id = id.copyOf(c)
            space = space.copyOf(c)
            role = role.copyOf(c)
            state = state.copyOf(c)
            crop = crop.copyOf(c)
            carry = carry.copyOf(c)
            f = f.copyOf(c * STRIDE)
        }
    }

    /** Copies [s] (valid only until the next `takeFresh`), keeping the previous copy for blending. */
    fun accept(s: Snapshot) {
        val t = prev
        prev = cur
        cur = t
        prevCount = count
        cur.ensure(s.count)
        for (i in 0 until s.count) {
            cur.id[i] = s.id[i]
            cur.space[i] = s.space[i]
            cur.role[i] = s.role[i]
            cur.state[i] = s.state[i]
            cur.crop[i] = s.crop[i]
            cur.carry[i] = s.carry[i]
            val o = i * STRIDE
            val f = cur.f
            f[o] = s.speed[i]
            f[o + 1] = s.heading[i]
            f[o + 2] = s.x[i]
            f[o + 3] = s.y[i]
            f[o + 4] = s.z[i]
            f[o + 5] = s.fx[i]
            f[o + 6] = s.fy[i]
            f[o + 7] = s.fz[i]
            f[o + 8] = s.nx[i]
            f[o + 9] = s.ny[i]
            f[o + 10] = s.nz[i]
        }
        count = s.count
        tick = s.tick
        publishedAt = s.publishedAt
        foods = s.foods
        airCells = s.airCells
        airMinX = s.airMinX
        airMinY = s.airMinY
        airMaxX = s.airMaxX
        airMaxY = s.airMaxY
    }

    /** How far the frame is into the current tick, 0 to 1. */
    fun alpha(now: Long, intervalNanos: Long): Float =
        ((now - publishedAt).toFloat() / intervalNanos).coerceIn(0f, 1f)

    /** Fills [out] with ant [i] drawn [alpha] of the way from the previous tick to the current one. */
    fun blend(i: Int, alpha: Float, out: AntPose) {
        val c = cur.f
        val o = i * STRIDE
        out.id = cur.id[i]
        out.space = if (cur.space[i].toInt() == 1) Space.SURFACE else Space.NEST
        out.role = Role.entries[cur.role[i].toInt()]
        out.state = AntState.entries[cur.state[i].toInt()]
        out.speed = c[o]
        out.carry = cur.carry[i].toInt()
        val blend = i < prevCount && prev.id[i] == cur.id[i] && prev.space[i] == cur.space[i]
        if (!blend) {
            out.crop = cur.crop[i]
            out.heading = c[o + 1]
            out.x = c[o + 2]
            out.y = c[o + 3]
            out.z = c[o + 4]
            out.fx = c[o + 5]
            out.fy = c[o + 6]
            out.fz = c[o + 7]
            out.nx = c[o + 8]
            out.ny = c[o + 9]
            out.nz = c[o + 10]
            return
        }
        val p = prev.f
        out.crop = lerp(prev.crop[i], cur.crop[i], alpha)
        out.heading = p[o + 1] + angleDiff(c[o + 1], p[o + 1]) * alpha
        out.x = lerp(p[o + 2], c[o + 2], alpha)
        out.y = lerp(p[o + 3], c[o + 3], alpha)
        out.z = lerp(p[o + 4], c[o + 4], alpha)
        var fx = lerp(p[o + 5], c[o + 5], alpha)
        var fy = lerp(p[o + 6], c[o + 6], alpha)
        var fz = lerp(p[o + 7], c[o + 7], alpha)
        var len = sqrt(fx * fx + fy * fy + fz * fz)
        if (len < 1e-6f) {
            fx = c[o + 5]
            fy = c[o + 6]
            fz = c[o + 7]
            len = 1f
        }
        out.fx = fx / len
        out.fy = fy / len
        out.fz = fz / len
        var nx = lerp(p[o + 8], c[o + 8], alpha)
        var ny = lerp(p[o + 9], c[o + 9], alpha)
        var nz = lerp(p[o + 10], c[o + 10], alpha)
        len = sqrt(nx * nx + ny * ny + nz * nz)
        if (len < 1e-6f) {
            nx = c[o + 8]
            ny = c[o + 9]
            nz = c[o + 10]
            len = 1f
        }
        out.nx = nx / len
        out.ny = ny / len
        out.nz = nz / len
    }

    private fun lerp(a: Float, b: Float, t: Float) = a + (b - a) * t

    private fun angleDiff(a: Float, b: Float): Float {
        var d = (a - b) % TWO_PI
        if (d > PI) d -= TWO_PI
        if (d < -PI) d += TWO_PI
        return d
    }

    private companion object {
        const val STRIDE = 11
        const val TWO_PI = (2 * PI).toFloat()
    }
}

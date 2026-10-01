package com.bydesigninteractive.ant.core.render.ant

import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * The fused low-poly ant, built in code: faceted lumps for the gaster, a thick waist joint, the
 * thorax, a neck joint and the head, plus mandibles, elbowed antennae and six two-segment legs
 * (thin three-sided prisms), and hidden carried pieces (a soil pellet and a prey piece) the shader
 * shows per ant. The simple model (`build(false)`) has fewer facets, one-segment legs, no antennae
 * or mandibles and no carried pieces; it keeps the part codes, so the same shader draws it. Every vertex carries a part code and a pivot: a leg's hip, the gaster's centre,
 * or the mandible tip, so the shader can swing legs, swell the gaster and show the load.
 * Model space is mm with x forward, y left, z up; the feet touch z = 0.
 *
 * Every triangle is counter-clockwise seen from outside its part, so the ant can be drawn with
 * back-face culling (the mandible blades are the one deliberate exception, they are double sided).
 */
object AntMesh {
    const val STRIDE = 13
    const val PART_BODY = 0
    const val PART_GASTER = 7
    const val PART_PELLET = 8
    const val PART_PREY = 9
    val TRIPOD_A = intArrayOf(0, 4, 2)

    // A near-black brown (owner's choice in M1b-2c, darker than the spec's 70/40/26).
    private const val BODY_R = 34f / 255f
    private const val BODY_G = 22f / 255f
    private const val BODY_B = 16f / 255f
    private const val LIMB = 0.6f
    private val MANDIBLE_TIP = floatArrayOf(2.75f, 0f, 0.75f)

    private class Out {
        val v = ArrayList<Float>()

        /** Adds one triangle; (a, b, c) must be counter-clockwise seen from outside, and the normal follows that. */
        fun tri(a: FloatArray, b: FloatArray, c: FloatArray, shade: Float, part: Int, pivot: FloatArray, color: FloatArray = floatArrayOf(BODY_R, BODY_G, BODY_B)) {
            val ux = b[0] - a[0]; val uy = b[1] - a[1]; val uz = b[2] - a[2]
            val wx = c[0] - a[0]; val wy = c[1] - a[1]; val wz = c[2] - a[2]
            var nx = uy * wz - uz * wy; var ny = uz * wx - ux * wz; var nz = ux * wy - uy * wx
            val l = sqrt(nx * nx + ny * ny + nz * nz).coerceAtLeast(1e-9f)
            nx /= l; ny /= l; nz /= l
            for (p in arrayOf(a, b, c)) {
                v += p[0]; v += p[1]; v += p[2]; v += nx; v += ny; v += nz
                v += color[0] * shade; v += color[1] * shade; v += color[2] * shade
                v += part.toFloat(); v += pivot[0]; v += pivot[1]; v += pivot[2]
            }
        }
    }

    fun build(detailed: Boolean): FloatArray {
        val o = Out()
        val sectors = if (detailed) 6 else 4
        val rings = if (detailed) 3 else 2
        val gasterC = floatArrayOf(-1.6f, 0f, 0.95f)
        lump(o, gasterC, 1.25f, 0.85f, 0.8f, if (detailed) 8 else 4, rings, PART_GASTER, gasterC)
        lump(o, floatArrayOf(-0.45f, 0f, 0.8f), 0.4f, 0.4f, 0.38f, if (detailed) 4 else 3, 2, PART_BODY, ZERO)
        lump(o, floatArrayOf(0.5f, 0f, 0.85f), 0.9f, 0.42f, 0.45f, sectors, rings, PART_BODY, ZERO)
        lump(o, floatArrayOf(1.3f, 0f, 0.9f), 0.32f, 0.3f, 0.3f, if (detailed) 4 else 3, 2, PART_BODY, ZERO)
        lump(o, floatArrayOf(1.95f, 0f, 0.9f), 0.65f, 0.6f, 0.55f, sectors, rings, PART_BODY, ZERO)
        val hips = arrayOf(floatArrayOf(0.9f, 0.3f, 0.65f), floatArrayOf(0.5f, 0.32f, 0.62f), floatArrayOf(0.1f, 0.3f, 0.65f))
        val feet = arrayOf(floatArrayOf(1.9f, 1.6f, 0f), floatArrayOf(0.4f, 1.8f, 0f), floatArrayOf(-1.3f, 1.6f, 0f))
        for (side in 0 until 2) for (k in 0 until 3) {
            val s = if (side == 0) 1f else -1f
            val hip = floatArrayOf(hips[k][0], hips[k][1] * s, hips[k][2])
            val foot = floatArrayOf(feet[k][0], feet[k][1] * s, feet[k][2])
            val knee = floatArrayOf((hip[0] + foot[0]) / 2f, (hip[1] + foot[1]) / 2f + 0.35f * s, 1.05f)
            val part = 1 + side * 3 + k
            if (detailed) {
                limb(o, hip, knee, 0.09f, part, hip)
                limb(o, knee, foot, 0.07f, part, hip)
            } else {
                limb(o, hip, foot, 0.09f, part, hip)
            }
        }
        if (detailed) {
            for (s in floatArrayOf(1f, -1f)) {
                val base = floatArrayOf(2.3f, 0.25f * s, 1.25f)
                val elbow = floatArrayOf(2.55f, 0.55f * s, 2.15f)
                val tip = floatArrayOf(3.55f, 0.85f * s, 1.6f)
                limb(o, base, elbow, 0.05f, PART_BODY, ZERO)
                limb(o, elbow, tip, 0.045f, PART_BODY, ZERO)
                val m0 = floatArrayOf(2.45f, 0.2f * s, 0.7f)
                val m1 = floatArrayOf(2.8f, 0.05f * s, 0.6f)
                val m2 = floatArrayOf(2.5f, 0.3f * s, 0.55f)
                o.tri(m0, m1, m2, 0.7f, PART_BODY, ZERO)
                o.tri(m0, m2, m1, 0.5f, PART_BODY, ZERO)
            }
        }
        if (detailed) {
            lump(o, MANDIBLE_TIP, 0.35f, 0.35f, 0.3f, 4, 2, PART_PELLET, MANDIBLE_TIP, floatArrayOf(0.43f, 0.31f, 0.2f))
            lump(o, MANDIBLE_TIP, 0.5f, 0.4f, 0.35f, 4, 2, PART_PREY, MANDIBLE_TIP, floatArrayOf(0.78f, 0.71f, 0.55f))
        }
        return o.v.toFloatArray()
    }

    private val ZERO = floatArrayOf(0f, 0f, 0f)

    /** A faceted ellipsoid around [c] (latitude rings by longitude sectors), shaded by a fixed key light. */
    private fun lump(o: Out, c: FloatArray, rx: Float, ry: Float, rz: Float, sectors: Int, rings: Int, part: Int, pivot: FloatArray, color: FloatArray = floatArrayOf(BODY_R, BODY_G, BODY_B)) {
        fun p(ring: Int, sector: Int): FloatArray {
            val lat = (-0.5f + ring.toFloat() / rings) * Math.PI.toFloat()
            val lon = sector * 2f * Math.PI.toFloat() / sectors
            return floatArrayOf(c[0] + cos(lat) * cos(lon) * rx, c[1] + cos(lat) * sin(lon) * ry, c[2] + sin(lat) * rz)
        }
        for (ring in 0 until rings) for (s in 0 until sectors) {
            val a = p(ring, s); val b = p(ring, s + 1); val cc = p(ring + 1, s + 1); val d = p(ring + 1, s)
            val shade = 0.85f + 0.3f * ((ring + 0.5f) / rings) - 0.1f * (s % 2)
            if (ring > 0) o.tri(a, b, cc, shade, part, pivot, color)
            if (ring < rings - 1) o.tri(a, cc, d, shade, part, pivot, color)
        }
    }

    /** A thin three-sided prism from [a] to [b]. */
    private fun limb(o: Out, a: FloatArray, b: FloatArray, r: Float, part: Int, pivot: FloatArray) {
        val dx = b[0] - a[0]; val dy = b[1] - a[1]; val dz = b[2] - a[2]
        val l = sqrt(dx * dx + dy * dy + dz * dz)
        // two unit vectors perpendicular to the limb
        var ux = -dy; var uy = dx; var uz = 0f
        var ul = sqrt(ux * ux + uy * uy)
        if (ul < 1e-6f) { ux = 1f; uy = 0f; ul = 1f }
        ux /= ul; uy /= ul
        val vx = (dy * uz - dz * uy) / l; val vy = (dz * ux - dx * uz) / l; val vz = (dx * uy - dy * ux) / l
        fun ring(p: FloatArray, k: Int): FloatArray {
            val t = k * 2f * Math.PI.toFloat() / 3f
            return floatArrayOf(p[0] + (ux * cos(t) + vx * sin(t)) * r, p[1] + (uy * cos(t) + vy * sin(t)) * r, p[2] + (uz * cos(t) + vz * sin(t)) * r)
        }
        for (k in 0 until 3) {
            val a0 = ring(a, k); val a1 = ring(a, k + 1); val b0 = ring(b, k); val b1 = ring(b, k + 1)
            o.tri(a0, a1, b1, LIMB + 0.1f * k, part, pivot)
            o.tri(a0, b1, b0, LIMB + 0.1f * k, part, pivot)
        }
    }
}

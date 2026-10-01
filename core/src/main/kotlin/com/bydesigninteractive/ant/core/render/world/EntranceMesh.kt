package com.bydesigninteractive.ant.core.render.world

import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * The nest entrance as drawn: a flat-shaded cone with a rim of radius [RADIUS] mm that dips [DEPTH] mm
 * into the ground, with a rim in darkened soil fading to near-black inside. Render only; the
 * simulation surface has no hole. The rim follows the ground height function given, lifted by
 * [LIFT] against z-fighting.
 *
 * The ground's facets are about 10 mm across, so they cover the dip. The renderer therefore draws
 * the [lid] (the rim's opening, on the ground) into the stencil buffer first, and then the [cone]
 * only where the lid was visible, ignoring depth. Every triangle is counter-clockwise seen from
 * above, so with back faces culled each pixel of the opening shows exactly one face of the cone.
 */
object EntranceMesh {
    const val RADIUS = 5f
    const val DEPTH = 3f
    const val LIFT = 0.4f
    const val SEGMENTS = 10

    /** The inner ring: its radius (mm) and how far (mm) below the lifted ground at the centre it sits. */
    private const val INNER_RADIUS = 2.8f
    private const val INNER_DROP = 1.4f

    /** The cone at (x, y) on the ground [height]: an outer band from the rim colour to dark, and a near-black fan to the bottom. */
    fun cone(x: Float, y: Float, height: (Float, Float) -> Float): MeshData {
        val b = MeshBuilder(SEGMENTS * 9)
        val g = height(x, y) + LIFT
        val rim = rim(x, y, height)
        val inner = Array(SEGMENTS) { k -> floatArrayOf(x + cos(angle(k)) * INNER_RADIUS, y + sin(angle(k)) * INNER_RADIUS, g - INNER_DROP) }
        val bottom = floatArrayOf(x, y, g - DEPTH)
        for (k in 0 until SEGMENTS) {
            val k2 = (k + 1) % SEGMENTS
            tri(b, rim[k], rim[k2], inner[k2], RIM, RIM, DARK)
            tri(b, rim[k], inner[k2], inner[k], RIM, DARK, DARK)
            tri(b, inner[k], inner[k2], bottom, DARK, DARK, BLACK)
        }
        return b.build()
    }

    /** The opening: a fan over the rim at (x, y), for the stencil pass. */
    fun lid(x: Float, y: Float, height: (Float, Float) -> Float): MeshData {
        val b = MeshBuilder(SEGMENTS * 3)
        val rim = rim(x, y, height)
        val centre = floatArrayOf(x, y, height(x, y) + LIFT)
        for (k in 0 until SEGMENTS) tri(b, rim[k], rim[(k + 1) % SEGMENTS], centre, BLACK, BLACK, BLACK)
        return b.build()
    }

    private fun angle(k: Int) = k * 6.2831855f / SEGMENTS

    /** [SEGMENTS] points on the rim, counter-clockwise from above, each on the lifted ground. */
    private fun rim(x: Float, y: Float, height: (Float, Float) -> Float): Array<FloatArray> = Array(SEGMENTS) { k ->
        val px = x + cos(angle(k)) * RADIUS
        val py = y + sin(angle(k)) * RADIUS
        floatArrayOf(px, py, height(px, py) + LIFT)
    }

    /** One triangle with its face normal and a colour per vertex. */
    private fun tri(b: MeshBuilder, p: FloatArray, q: FloatArray, r: FloatArray, cp: FloatArray, cq: FloatArray, cr: FloatArray) {
        val ux = q[0] - p[0]; val uy = q[1] - p[1]; val uz = q[2] - p[2]
        val wx = r[0] - p[0]; val wy = r[1] - p[1]; val wz = r[2] - p[2]
        var nx = uy * wz - uz * wy
        var ny = uz * wx - ux * wz
        var nz = ux * wy - uy * wx
        val len = sqrt(nx * nx + ny * ny + nz * nz)
        if (len > 1e-12f) { nx /= len; ny /= len; nz /= len } else { nx = 0f; ny = 0f; nz = 1f }
        b.vertex(p[0], p[1], p[2], nx, ny, nz, cp[0], cp[1], cp[2])
        b.vertex(q[0], q[1], q[2], nx, ny, nz, cq[0], cq[1], cq[2])
        b.vertex(r[0], r[1], r[2], nx, ny, nz, cr[0], cr[1], cr[2])
    }

    /** The rim: the soil colour, slightly darkened, so it blends into the ground. */
    private val RIM = floatArrayOf(ChunkMesher.SOIL_R * 0.8f, ChunkMesher.SOIL_G * 0.8f, ChunkMesher.SOIL_B * 0.8f)
    private val DARK = floatArrayOf(0.09f, 0.06f, 0.04f)
    private val BLACK = floatArrayOf(0.02f, 0.015f, 0.01f)
}

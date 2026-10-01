package com.bydesigninteractive.ant.core.render.world

import com.bydesigninteractive.ant.sim.util.hash
import com.bydesigninteractive.ant.sim.world.CHUNK_MM
import com.bydesigninteractive.ant.sim.world.ChunkedField
import com.bydesigninteractive.ant.sim.world.SURFACE_MM
import com.bydesigninteractive.ant.sim.world.sdf.HeightField
import java.util.Random
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.sin

/**
 * Builds the low-poly meshes of one surface chunk from the seed and published data: the ground as
 * jittered flat facets on the height field (spoil included), and baked faceted specks. Rocks and
 * food are added in later tasks. It owns its own [HeightField] and spoil mirror, so it runs on the
 * mesher thread without touching the live world. Not thread-safe.
 */
class ChunkMesher(private val seed: Long) {
    private val spoil = ChunkedField()
    val heights = HeightField(seed, spoil)

    /** Mirrors a published spoil chunk ([values] is immutable and kept without copying). */
    fun setSpoil(cx: Int, cy: Int, values: FloatArray?) = spoil.setChunk(cx, cy, values)

    /** Pellets per spoil cell at (x, y), from the mirror. */
    fun spoilAt(x: Float, y: Float): Float = spoil.get(x, y)

    fun ground(cx: Int, cy: Int): MeshData {
        val b = MeshBuilder(CELLS * CELLS * 6 + 400)
        val n = CELLS + 1
        val xs = FloatArray(n * n)
        val ys = FloatArray(n * n)
        val zs = FloatArray(n * n)
        for (j in 0 until n) for (i in 0 until n) {
            val gi = cx * CELLS + i
            val gj = cy * CELLS + j
            val k = i + j * n
            xs[k] = jittered(gi, gj, 0)
            ys[k] = jittered(gi, gj, 1)
            zs[k] = heights.height(xs[k], ys[k])
        }
        for (j in 0 until CELLS) for (i in 0 until CELLS) {
            val a = i + j * n
            val c = a + 1
            val d = a + n
            val e = d + 1
            val h = hash(seed xor FACET_SALT, cx * CELLS + i, cy * CELLS + j)
            if (h and 1L == 0L) {
                soilTri(b, xs, ys, zs, a, c, e, h ushr 8)
                soilTri(b, xs, ys, zs, a, e, d, h ushr 20)
            } else {
                soilTri(b, xs, ys, zs, a, c, d, h ushr 8)
                soilTri(b, xs, ys, zs, c, e, d, h ushr 20)
            }
        }
        specks(b, cx, cy)
        return b.build()
    }

    /** Coordinate [axis] (0 x, 1 y) of global grid vertex (gi, gj), jittered by a quarter cell, clamped to the map. */
    private fun jittered(gi: Int, gj: Int, axis: Int): Float {
        val base = (if (axis == 0) gi else gj) * CELL_MM
        val h = hash(seed xor JITTER_SALT, gi * 2 + axis, gj)
        val j = (((h ushr 11) and 0xFFFF).toFloat() / 0xFFFF - 0.5f) * 0.5f * CELL_MM
        return (base + j).coerceIn(0f, SURFACE_MM.toFloat())
    }

    private fun soilTri(b: MeshBuilder, xs: FloatArray, ys: FloatArray, zs: FloatArray, p: Int, q: Int, r: Int, h: Long) {
        val f = 0.88f + ((h and 0xFF).toFloat() / 255f) * 0.24f
        b.flat(xs[p], ys[p], zs[p], xs[q], ys[q], zs[q], xs[r], ys[r], zs[r], SOIL_R * f, SOIL_G * f, SOIL_B * f)
    }

    /** A few loose clusters and some strays of small flat flecks, 0.05 mm above the ground. */
    private fun specks(b: MeshBuilder, cx: Int, cy: Int) {
        val r = Random(hash(seed xor SPECK_SALT, cx, cy))
        val x0 = cx * CHUNK_MM.toFloat()
        val y0 = cy * CHUNK_MM.toFloat()
        repeat(CLUSTERS) {
            val px = x0 + r.nextFloat() * CHUNK_MM
            val py = y0 + r.nextFloat() * CHUNK_MM
            repeat(2 + r.nextInt(5)) { fleck(b, r, px + r.nextGaussian().toFloat() * 25f, py + r.nextGaussian().toFloat() * 10f, x0, y0) }
        }
        repeat(STRAYS) { fleck(b, r, x0 + r.nextFloat() * CHUNK_MM, y0 + r.nextFloat() * CHUNK_MM, x0, y0) }
    }

    private fun fleck(b: MeshBuilder, r: Random, x: Float, y: Float, x0: Float, y0: Float) {
        val size = exp(0.5f + r.nextGaussian().toFloat() * 0.5f).coerceIn(0.6f, 5f)
        val sides = intArrayOf(3, 3, 4, 5)[r.nextInt(4)]
        val shade = SPECK_SHADES[r.nextInt(SPECK_SHADES.size)]
        val f = 0.88f + r.nextFloat() * 0.24f
        val a0 = r.nextFloat() * 6.2832f
        val px = FloatArray(sides)
        val py = FloatArray(sides)
        for (k in 0 until sides) {
            val t = a0 + k * 6.2832f / sides + (r.nextFloat() - 0.5f) * 0.8f
            val rr = size * (0.5f + r.nextFloat() * 0.8f)
            px[k] = (x + cos(t) * rr).coerceIn(x0, x0 + CHUNK_MM)
            py[k] = (y + sin(t) * rr).coerceIn(y0, y0 + CHUNK_MM)
        }
        val cz = heights.height(x.coerceIn(x0, x0 + CHUNK_MM), y.coerceIn(y0, y0 + CHUNK_MM)) + SPECK_LIFT
        val ccx = x.coerceIn(x0, x0 + CHUNK_MM)
        val ccy = y.coerceIn(y0, y0 + CHUNK_MM)
        for (k in 0 until sides) {
            val k2 = (k + 1) % sides
            b.flat(
                ccx, ccy, cz,
                px[k], py[k], heights.height(px[k], py[k]) + SPECK_LIFT,
                px[k2], py[k2], heights.height(px[k2], py[k2]) + SPECK_LIFT,
                shade[0] * f, shade[1] * f, shade[2] * f,
            )
        }
    }

    companion object {
        const val CELLS = 40
        const val CELL_MM = 12.5f // CHUNK_MM / CELLS
        const val SPECK_LIFT = 0.05f
        const val CLUSTERS = 4
        const val STRAYS = 8
        const val SOIL_R = 139f / 255f
        const val SOIL_G = 98f / 255f
        const val SOIL_B = 62f / 255f
        private const val FACET_SALT = 0x5011L
        private const val JITTER_SALT = 0x717L
        private const val SPECK_SALT = 0x5BECL
        private val SPECK_SHADES = arrayOf(
            floatArrayOf(96f / 255f, 64f / 255f, 40f / 255f),
            floatArrayOf(116f / 255f, 80f / 255f, 50f / 255f),
            floatArrayOf(150f / 255f, 112f / 255f, 76f / 255f),
            floatArrayOf(120f / 255f, 116f / 255f, 108f / 255f),
        )
    }
}

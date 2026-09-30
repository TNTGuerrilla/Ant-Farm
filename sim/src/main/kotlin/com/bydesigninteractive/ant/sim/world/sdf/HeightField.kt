package com.bydesigninteractive.ant.sim.world.sdf

import com.bydesigninteractive.ant.sim.util.unit
import com.bydesigninteractive.ant.sim.world.CHUNKS
import com.bydesigninteractive.ant.sim.world.CHUNK_MM
import com.bydesigninteractive.ant.sim.world.ChunkedField
import com.bydesigninteractive.ant.sim.world.SURFACE_MM
import kotlin.math.floor
import kotlin.math.roundToInt
import kotlin.math.sqrt

/** One dumped pellet (1 mm3) spread over its 1 cm2 field cell raises the ground 0.01 mm. */
const val PELLET_HEIGHT = 0.01f

/**
 * The ground: z = h(x, y) in surface millimeters. The base relief is seeded value noise (rolling
 * of about +/-15 mm over about 30 cm, bumps of about 1.5 mm), cached per 50 cm chunk on a 4 mm
 * grid of 16-bit values in micrometer steps (about 31 KB per chunk, 8 MB for the whole map) the
 * first time the chunk is used. The 1.5 mm bumps have a 20 mm scale, which the grid resolves. Dumped spoil raises it. Coordinates outside the map are
 * clamped to its edge. Not thread-safe: it reuses a scratch array (one simulation thread).
 */
class HeightField(private val seed: Long, private val spoil: ChunkedField) {
    private val grids = arrayOfNulls<ShortArray>(CHUNKS * CHUNKS)
    private val slopeScratch = FloatArray(2)

    private fun at(grid: ShortArray, index: Int): Float = grid[index] * HEIGHT_STEP

    /** The seeded relief without spoil. */
    fun base(x: Float, y: Float): Float {
        val cx = x.coerceIn(0f, MAX)
        val cy = y.coerceIn(0f, MAX)
        val kx = (cx / CHUNK_MM).toInt().coerceAtMost(CHUNKS - 1)
        val ky = (cy / CHUNK_MM).toInt().coerceAtMost(CHUNKS - 1)
        val k = kx + ky * CHUNKS
        val grid = grids[k] ?: build(kx, ky).also { grids[k] = it }
        val u = (cx - kx * CHUNK_MM) / GRID_MM
        val v = (cy - ky * CHUNK_MM) / GRID_MM
        val i = floor(u).toInt().coerceAtMost(CELLS - 1)
        val j = floor(v).toInt().coerceAtMost(CELLS - 1)
        val fu = u - i
        val fv = v - j
        val row = CELLS + 1
        val h00 = at(grid, j * row + i)
        val h10 = at(grid, j * row + i + 1)
        val h01 = at(grid, (j + 1) * row + i)
        val h11 = at(grid, (j + 1) * row + i + 1)
        return (h00 + (h10 - h00) * fu) + ((h01 + (h11 - h01) * fu) - (h00 + (h10 - h00) * fu)) * fv
    }

    /** The ground height, spoil mound included. */
    fun height(x: Float, y: Float): Float = base(x, y) + spoilHeight(x, y)

    /** dh/dx and dh/dy by central differences 1 mm apart, into out[0] and out[1]. */
    fun slope(x: Float, y: Float, out: FloatArray) {
        out[0] = (height(x + 1f, y) - height(x - 1f, y)) / 2f
        out[1] = (height(x, y + 1f) - height(x, y - 1f)) / 2f
    }

    /**
     * Signed distance to the ground, close to exact for gentle slopes. It evaluates the height
     * once and takes the slope analytically from the same bilinear cells (base grid and spoil).
     */
    fun distance(x: Float, y: Float, z: Float): Float {
        val h = heightAndSlope(x, y, slopeScratch)
        val hx = slopeScratch[0]
        val hy = slopeScratch[1]
        return (z - h) / sqrt(1f + hx * hx + hy * hy)
    }

    /**
     * The same value as [height], with dh/dx and dh/dy of the bilinear patches under (x, y) into
     * out[0] and out[1]. The slope is 0 along an axis where (x, y) lies beyond the map edge.
     */
    private fun heightAndSlope(x: Float, y: Float, out: FloatArray): Float {
        val cx = x.coerceIn(0f, MAX)
        val cy = y.coerceIn(0f, MAX)
        val kx = (cx / CHUNK_MM).toInt().coerceAtMost(CHUNKS - 1)
        val ky = (cy / CHUNK_MM).toInt().coerceAtMost(CHUNKS - 1)
        val k = kx + ky * CHUNKS
        val grid = grids[k] ?: build(kx, ky).also { grids[k] = it }
        val u = (cx - kx * CHUNK_MM) / GRID_MM
        val v = (cy - ky * CHUNK_MM) / GRID_MM
        val i = floor(u).toInt().coerceAtMost(CELLS - 1)
        val j = floor(v).toInt().coerceAtMost(CELLS - 1)
        val fu = u - i
        val fv = v - j
        val row = CELLS + 1
        val h00 = at(grid, j * row + i)
        val h10 = at(grid, j * row + i + 1)
        val h01 = at(grid, (j + 1) * row + i)
        val h11 = at(grid, (j + 1) * row + i + 1)
        val a = h00 + (h10 - h00) * fu
        val b = h01 + (h11 - h01) * fu
        val base = a + (b - a) * fv
        var dx = if (x == cx) ((h10 - h00) + ((h11 - h01) - (h10 - h00)) * fv) / GRID_MM else 0f
        var dy = if (y == cy) (b - a) / GRID_MM else 0f

        val cell = spoil.cellMm.toFloat()
        val su = x / cell - 0.5f
        val sv = y / cell - 0.5f
        val si = floor(su).toInt()
        val sj = floor(sv).toInt()
        val sfu = su - si
        val sfv = sv - sj
        val s00 = spoil.cell(si, sj)
        val s10 = spoil.cell(si + 1, sj)
        val s01 = spoil.cell(si, sj + 1)
        val s11 = spoil.cell(si + 1, sj + 1)
        val sa = s00 + (s10 - s00) * sfu
        val sb = s01 + (s11 - s01) * sfu
        dx += ((s10 - s00) + ((s11 - s01) - (s10 - s00)) * sfv) * PELLET_HEIGHT / cell
        dy += (sb - sa) * PELLET_HEIGHT / cell
        out[0] = dx
        out[1] = dy
        return base + (sa + (sb - sa) * sfv) * PELLET_HEIGHT
    }

    private fun spoilHeight(x: Float, y: Float): Float {
        val cell = spoil.cellMm.toFloat()
        val u = x / cell - 0.5f
        val v = y / cell - 0.5f
        val i = floor(u).toInt()
        val j = floor(v).toInt()
        val fu = u - i
        val fv = v - j
        val a = spoil.cell(i, j) + (spoil.cell(i + 1, j) - spoil.cell(i, j)) * fu
        val b = spoil.cell(i, j + 1) + (spoil.cell(i + 1, j + 1) - spoil.cell(i, j + 1)) * fu
        return (a + (b - a) * fv) * PELLET_HEIGHT
    }

    private fun build(kx: Int, ky: Int): ShortArray {
        val row = CELLS + 1
        val grid = ShortArray(row * row)
        for (j in 0..CELLS) for (i in 0..CELLS) {
            val x = kx * CHUNK_MM + i * GRID_MM
            val y = ky * CHUNK_MM + j * GRID_MM
            val h = RELIEF * noise(seed xor RELIEF_SALT, x / RELIEF_SCALE, y / RELIEF_SCALE) +
                BUMPS * noise(seed xor BUMP_SALT, x / BUMP_SCALE, y / BUMP_SCALE)
            grid[j * row + i] = (h / HEIGHT_STEP).roundToInt().toShort()
        }
        return grid
    }

    /** Value noise in [-1, 1] with smoothstep interpolation; plain arithmetic, so platform-exact. */
    private fun noise(s: Long, x: Float, y: Float): Float {
        val ix = floor(x).toInt()
        val iy = floor(y).toInt()
        val fx = x - ix
        val fy = y - iy
        val sx = fx * fx * (3f - 2f * fx)
        val sy = fy * fy * (3f - 2f * fy)
        val v00 = (unit(s, ix, iy) * 2 - 1).toFloat()
        val v10 = (unit(s, ix + 1, iy) * 2 - 1).toFloat()
        val v01 = (unit(s, ix, iy + 1) * 2 - 1).toFloat()
        val v11 = (unit(s, ix + 1, iy + 1) * 2 - 1).toFloat()
        val a = v00 + (v10 - v00) * sx
        val b = v01 + (v11 - v01) * sx
        return a + (b - a) * sy
    }

    companion object {
        /** Bytes of one chunk's grid (for the memory budget). */
        fun bytesPerChunk(): Int = (CELLS + 1) * (CELLS + 1) * 2

        private const val MAX = SURFACE_MM.toFloat()
        private const val GRID_MM = 4f
        private const val CELLS = CHUNK_MM / 4
        private const val HEIGHT_STEP = 0.001f
        private const val RELIEF = 15f
        private const val RELIEF_SCALE = 300f
        private const val BUMPS = 1.5f
        private const val BUMP_SCALE = 20f
        private const val RELIEF_SALT = 0x6E11L
        private const val BUMP_SALT = 0x6E12L
    }
}

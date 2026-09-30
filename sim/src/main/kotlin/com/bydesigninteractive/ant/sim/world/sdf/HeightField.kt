package com.bydesigninteractive.ant.sim.world.sdf

import com.bydesigninteractive.ant.sim.util.unit
import com.bydesigninteractive.ant.sim.world.CHUNKS
import com.bydesigninteractive.ant.sim.world.CHUNK_MM
import com.bydesigninteractive.ant.sim.world.ChunkedField
import com.bydesigninteractive.ant.sim.world.SURFACE_MM
import kotlin.math.floor
import kotlin.math.sqrt

/** One dumped pellet (1 mm3) spread over its 1 cm2 field cell raises the ground 0.01 mm. */
const val PELLET_HEIGHT = 0.01f

/**
 * The ground: z = h(x, y) in surface millimeters. The base relief is seeded value noise (rolling
 * of about +/-15 mm over about 30 cm, bumps of about 1.5 mm), cached per 50 cm chunk on a 2 mm
 * grid the first time the chunk is used. Dumped spoil raises it. Coordinates outside the map are
 * clamped to its edge. Not thread-safe: it reuses a scratch array (one simulation thread).
 */
class HeightField(private val seed: Long, private val spoil: ChunkedField) {
    private val grids = arrayOfNulls<FloatArray>(CHUNKS * CHUNKS)
    private val slopeScratch = FloatArray(2)

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
        val h00 = grid[j * row + i]
        val h10 = grid[j * row + i + 1]
        val h01 = grid[(j + 1) * row + i]
        val h11 = grid[(j + 1) * row + i + 1]
        return (h00 + (h10 - h00) * fu) + ((h01 + (h11 - h01) * fu) - (h00 + (h10 - h00) * fu)) * fv
    }

    /** The ground height, spoil mound included. */
    fun height(x: Float, y: Float): Float = base(x, y) + spoilHeight(x, y)

    /** dh/dx and dh/dy by central differences 1 mm apart, into out[0] and out[1]. */
    fun slope(x: Float, y: Float, out: FloatArray) {
        out[0] = (height(x + 1f, y) - height(x - 1f, y)) / 2f
        out[1] = (height(x, y + 1f) - height(x, y - 1f)) / 2f
    }

    /** Signed distance to the ground, close to exact for gentle slopes. */
    fun distance(x: Float, y: Float, z: Float): Float {
        slope(x, y, slopeScratch)
        val hx = slopeScratch[0]
        val hy = slopeScratch[1]
        return (z - height(x, y)) / sqrt(1f + hx * hx + hy * hy)
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

    private fun build(kx: Int, ky: Int): FloatArray {
        val row = CELLS + 1
        val grid = FloatArray(row * row)
        for (j in 0..CELLS) for (i in 0..CELLS) {
            val x = kx * CHUNK_MM + i * GRID_MM
            val y = ky * CHUNK_MM + j * GRID_MM
            grid[j * row + i] = RELIEF * noise(seed xor RELIEF_SALT, x / RELIEF_SCALE, y / RELIEF_SCALE) +
                BUMPS * noise(seed xor BUMP_SALT, x / BUMP_SCALE, y / BUMP_SCALE)
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

    private companion object {
        const val MAX = SURFACE_MM.toFloat()
        const val GRID_MM = 2f
        const val CELLS = (CHUNK_MM / 2)
        const val RELIEF = 15f
        const val RELIEF_SCALE = 300f
        const val BUMPS = 1.5f
        const val BUMP_SCALE = 20f
        const val RELIEF_SALT = 0x6E11L
        const val BUMP_SALT = 0x6E12L
    }
}

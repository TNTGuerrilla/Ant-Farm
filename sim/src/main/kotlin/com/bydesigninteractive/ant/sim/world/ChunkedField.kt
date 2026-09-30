package com.bydesigninteractive.ant.sim.world

import kotlin.math.exp
import kotlin.math.floor

/**
 * A scalar field over the surface map at [cellMm] resolution, stored in square chunks that are
 * allocated when they first get a value and released when everything in them has faded.
 */
class ChunkedField(
    val sizeMm: Int = SURFACE_MM,
    val cellMm: Int = FIELD_CELL_MM,
    val chunkCells: Int = CHUNK_MM / FIELD_CELL_MM,
) {
    val cells = sizeMm / cellMm
    val chunks = cells / chunkCells
    private val data = arrayOfNulls<FloatArray>(chunks * chunks)
    private val next = arrayOfNulls<FloatArray>(chunks * chunks)

    fun get(xMm: Float, yMm: Float): Float = cell(floor(xMm / cellMm).toInt(), floor(yMm / cellMm).toInt())

    fun cell(ix: Int, iy: Int): Float {
        if (ix < 0 || iy < 0 || ix >= cells || iy >= cells) return 0f
        val chunk = data[ix / chunkCells + (iy / chunkCells) * chunks] ?: return 0f
        return chunk[(iy % chunkCells) * chunkCells + ix % chunkCells]
    }

    fun add(xMm: Float, yMm: Float, amount: Float) {
        val ix = floor(xMm / cellMm).toInt()
        val iy = floor(yMm / cellMm).toInt()
        if (ix < 0 || iy < 0 || ix >= cells || iy >= cells) return
        val ci = ix / chunkCells + (iy / chunkCells) * chunks
        val chunk = data[ci] ?: FloatArray(chunkCells * chunkCells).also { data[ci] = it }
        chunk[(iy % chunkCells) * chunkCells + ix % chunkCells] += amount
    }

    /** The values of one chunk, row by row from its low-y edge, or null if it holds nothing. */
    fun chunk(cx: Int, cy: Int): FloatArray? = data[cx + cy * chunks]

    fun allocatedChunks(): Int = data.count { it != null }

    fun max(): Float {
        var m = 0f
        for (c in data) if (c != null) for (v in c) if (v > m) m = v
        return m
    }

    fun total(): Float {
        var sum = 0.0
        for (c in data) if (c != null) for (v in c) sum += v
        return sum.toFloat()
    }

    /**
     * Decays every value by [decayPerSecond] and spreads it to its four neighbors at [diffusion]
     * (cells squared per second; keep diffusion * dt below 0.25). Whatever spreads toward an
     * unallocated chunk is lost, which is small next to the decay.
     */
    fun step(dt: Float, decayPerSecond: Float, diffusion: Float) {
        val keep = exp(-decayPerSecond * dt)
        val k = diffusion * dt
        val n = chunkCells
        for (ci in data.indices) {
            val src = data[ci] ?: continue
            val dst = next[ci] ?: FloatArray(n * n).also { next[ci] = it }
            val baseX = (ci % chunks) * n
            val baseY = (ci / chunks) * n
            for (ly in 0 until n) for (lx in 0 until n) {
                val i = ly * n + lx
                val v = src[i]
                dst[i] = if (k == 0f) {
                    v * keep
                } else {
                    val l = if (lx > 0) src[i - 1] else cell(baseX + lx - 1, baseY + ly)
                    val r = if (lx < n - 1) src[i + 1] else cell(baseX + lx + 1, baseY + ly)
                    val d = if (ly > 0) src[i - n] else cell(baseX + lx, baseY + ly - 1)
                    val u = if (ly < n - 1) src[i + n] else cell(baseX + lx, baseY + ly + 1)
                    (v + k * (l + r + d + u - 4 * v)) * keep
                }
            }
        }
        for (ci in data.indices) {
            val src = data[ci] ?: continue
            val dst = next[ci]!!
            var max = 0f
            for (v in dst) if (v > max) max = v
            if (max < FADED) {
                data[ci] = null
                next[ci] = null
            } else {
                data[ci] = dst
                next[ci] = src // the old values become next step's scratch
            }
        }
    }

    private companion object {
        const val FADED = 1e-4f
    }
}

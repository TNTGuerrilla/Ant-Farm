package com.bydesigninteractive.ant.sim.world

import kotlin.math.exp
import kotlin.math.floor
import kotlin.math.max

/**
 * A sparse 3D scalar field in [cellMm] voxels, stored in blocks of 10 x 10 x 10 voxels that are
 * allocated on first write and released when faded. Values are sampled and deposited with
 * trilinear weights, so a trail reads the same whichever voxel an ant stands in.
 *
 * With [diffusion] 0, decay is lazy: a block catches up when touched, and every block is swept
 * every [SWEEP_TICKS] ticks, so untouched ground costs nothing per tick. With diffusion, [step]
 * decays and diffuses every allocated block each tick; spread toward an unallocated block is lost.
 */
class Field3(
    private val dt: Float,
    private val decayPerSecond: Float,
    private val diffusion: Float,
    val cellMm: Float = FIELD_CELL_MM.toFloat(),
) {
    private class Block(val bx: Int, val by: Int, val bz: Int, var tick: Long) {
        var v = FloatArray(B3)
        var scratch = FloatArray(B3)
    }

    private val blocks = LinkedHashMap<Long, Block>()
    private var cacheKey = Long.MIN_VALUE
    private var cacheBlock: Block? = null

    /** Ticks stepped so far. */
    var now = 0L
        private set

    fun blockCount(): Int = blocks.size

    fun get(x: Float, y: Float, z: Float): Float {
        val u = x / cellMm - 0.5f
        val v = y / cellMm - 0.5f
        val w = z / cellMm - 0.5f
        val ix = floor(u).toInt()
        val iy = floor(v).toInt()
        val iz = floor(w).toInt()
        val fx = u - ix
        val fy = v - iy
        val fz = w - iz
        var sum = 0f
        for (c in 0 until 8) {
            val dx = c and 1
            val dy = (c shr 1) and 1
            val dz = (c shr 2) and 1
            val weight = (if (dx == 0) 1f - fx else fx) * (if (dy == 0) 1f - fy else fy) * (if (dz == 0) 1f - fz else fz)
            if (weight == 0f) continue
            sum += weight * voxel(ix + dx, iy + dy, iz + dz)
        }
        return sum
    }

    fun add(x: Float, y: Float, z: Float, amount: Float) {
        val u = x / cellMm - 0.5f
        val v = y / cellMm - 0.5f
        val w = z / cellMm - 0.5f
        val ix = floor(u).toInt()
        val iy = floor(v).toInt()
        val iz = floor(w).toInt()
        val fx = u - ix
        val fy = v - iy
        val fz = w - iz
        for (c in 0 until 8) {
            val dx = c and 1
            val dy = (c shr 1) and 1
            val dz = (c shr 2) and 1
            val weight = (if (dx == 0) 1f - fx else fx) * (if (dy == 0) 1f - fy else fy) * (if (dz == 0) 1f - fz else fz)
            if (weight == 0f) continue
            val gx = ix + dx
            val gy = iy + dy
            val gz = iz + dz
            val b = blockFor(gx, gy, gz, create = true)!!
            catchUp(b)
            b.v[index(gx, gy, gz)] += amount * weight
        }
    }

    fun step() {
        if (diffusion > 0f) {
            diffuse()
        } else if ((now + 1) % SWEEP_TICKS == 0L) {
            sweep()
        }
        now++
    }

    fun max(): Float {
        var m = 0f
        for (b in blocks.values) {
            catchUp(b)
            for (value in b.v) if (value > m) m = value
        }
        return m
    }

    /**
     * The largest value in each column of voxels over the square of [cells] x [cells] voxels whose
     * corner is (x0, y0), into [out] row by row from the low-y edge. False if all are zero.
     */
    fun projectMax(x0: Float, y0: Float, cells: Int, out: FloatArray): Boolean {
        out.fill(0f)
        val ix0 = floor(x0 / cellMm).toInt()
        val iy0 = floor(y0 / cellMm).toInt()
        var any = false
        for (b in blocks.values) {
            val gx0 = b.bx * B
            val gy0 = b.by * B
            if (gx0 + B <= ix0 || gx0 >= ix0 + cells || gy0 + B <= iy0 || gy0 >= iy0 + cells) continue
            catchUp(b)
            for (lz in 0 until B) for (ly in 0 until B) for (lx in 0 until B) {
                val gx = gx0 + lx - ix0
                val gy = gy0 + ly - iy0
                if (gx !in 0 until cells || gy !in 0 until cells) continue
                val value = b.v[(lz * B + ly) * B + lx]
                val o = gy * cells + gx
                if (value > out[o]) {
                    out[o] = value
                    any = true
                }
            }
        }
        return any
    }

    private fun voxel(gx: Int, gy: Int, gz: Int): Float {
        val b = blockFor(gx, gy, gz, create = false) ?: return 0f
        catchUp(b)
        return b.v[index(gx, gy, gz)]
    }

    private fun rawVoxel(gx: Int, gy: Int, gz: Int): Float {
        val b = blockFor(gx, gy, gz, create = false) ?: return 0f
        return b.v[index(gx, gy, gz)]
    }

    private fun blockFor(gx: Int, gy: Int, gz: Int, create: Boolean): Block? {
        val bx = Math.floorDiv(gx, B)
        val by = Math.floorDiv(gy, B)
        val bz = Math.floorDiv(gz, B)
        val key = key(bx, by, bz)
        if (key == cacheKey) return cacheBlock
        var b = blocks[key]
        if (b == null) {
            if (!create) return null
            b = Block(bx, by, bz, now)
            blocks[key] = b
        }
        cacheKey = key
        cacheBlock = b
        return b
    }

    private fun catchUp(b: Block) {
        if (b.tick == now) return
        val factor = exp(-decayPerSecond * dt * (now - b.tick))
        val v = b.v
        for (i in v.indices) v[i] *= factor
        b.tick = now
    }

    private fun diffuse() {
        val keep = exp(-decayPerSecond * dt)
        val k = diffusion * dt
        for (b in blocks.values) {
            val src = b.v
            val dst = b.scratch
            for (lz in 0 until B) for (ly in 0 until B) for (lx in 0 until B) {
                val i = (lz * B + ly) * B + lx
                val gx = b.bx * B + lx
                val gy = b.by * B + ly
                val gz = b.bz * B + lz
                val c = src[i]
                val sum = neighbor(src, lx - 1, ly, lz, gx - 1, gy, gz) + neighbor(src, lx + 1, ly, lz, gx + 1, gy, gz) +
                    neighbor(src, lx, ly - 1, lz, gx, gy - 1, gz) + neighbor(src, lx, ly + 1, lz, gx, gy + 1, gz) +
                    neighbor(src, lx, ly, lz - 1, gx, gy, gz - 1) + neighbor(src, lx, ly, lz + 1, gx, gy, gz + 1)
                dst[i] = (c + k * (sum - 6f * c)) * keep
            }
        }
        val dead = ArrayList<Long>()
        for ((key, b) in blocks) {
            val old = b.v
            b.v = b.scratch
            b.scratch = old
            b.tick = now + 1
            var m = 0f
            for (value in b.v) if (value > m) m = value
            if (m < FADED) dead += key
        }
        for (key in dead) blocks.remove(key)
        cacheKey = Long.MIN_VALUE
        cacheBlock = null
    }

    private fun neighbor(src: FloatArray, lx: Int, ly: Int, lz: Int, gx: Int, gy: Int, gz: Int): Float =
        if (lx in 0 until B && ly in 0 until B && lz in 0 until B) src[(lz * B + ly) * B + lx] else rawVoxel(gx, gy, gz)

    private fun sweep() {
        val dead = ArrayList<Long>()
        for ((key, b) in blocks) {
            catchUp(b)
            var m = 0f
            for (value in b.v) if (value > m) m = value
            if (m < FADED) dead += key
        }
        for (key in dead) blocks.remove(key)
        cacheKey = Long.MIN_VALUE
        cacheBlock = null
    }

    private fun index(gx: Int, gy: Int, gz: Int): Int =
        (Math.floorMod(gz, B) * B + Math.floorMod(gy, B)) * B + Math.floorMod(gx, B)

    private fun key(bx: Int, by: Int, bz: Int): Long =
        ((bx + OFFSET).toLong() shl 42) or ((by + OFFSET).toLong() shl 21) or (bz + OFFSET).toLong()

    private companion object {
        const val B = 10
        const val B3 = B * B * B
        const val SWEEP_TICKS = 200L
        const val FADED = 1e-4f
        const val OFFSET = 1 shl 20
    }
}

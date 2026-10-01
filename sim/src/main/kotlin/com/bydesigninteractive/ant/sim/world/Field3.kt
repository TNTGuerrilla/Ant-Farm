package com.bydesigninteractive.ant.sim.world

import com.bydesigninteractive.ant.sim.util.LongObjectMap
import kotlin.math.exp
import kotlin.math.floor

/**
 * A sparse 3D scalar field in [cellMm] voxels, stored in blocks of 10 x 10 x 10 voxels that are
 * allocated on first write and released when faded. Values are sampled and deposited with
 * trilinear weights, so a trail reads the same whichever voxel an ant stands in.
 *
 * With [diffusion] 0, decay is lazy: a block catches up when touched, and every block is swept
 * every [SWEEP_TICKS] ticks, so untouched ground costs nothing per tick. With diffusion, [step]
 * decays and diffuses every allocated block each tick; spread toward an unallocated block is lost.
 * A block is released once its strongest voxel is below [releaseBelow].
 *
 * Reads and writes: [get], [add] and [step] mutate (they catch blocks up, which changes float
 * rounding and so the simulation's results). [peek], [max], [projectMax] and [blockCount] are
 * side-effect free, and they are the only calls code outside the simulation step (the renderers
 * in `core`) may use, so a run does not depend on when or what was drawn.
 */
class Field3(
    private val dt: Float,
    private val decayPerSecond: Float,
    private val diffusion: Float,
    val cellMm: Float = FIELD_CELL_MM.toFloat(),
    private val releaseBelow: Float = 1e-4f,
) {
    private class Block(val bx: Int, val by: Int, val bz: Int, var tick: Long) {
        var v = FloatArray(B3)

        /** The diffusion buffer, allocated on the first diffusion step only. */
        var scratch: FloatArray? = null
    }

    private val blocks = LongObjectMap<Block>()
    private val cacheKeys = LongArray(CACHE) { Long.MIN_VALUE }
    private val cacheBlocks = arrayOfNulls<Block>(CACHE)
    private var cacheNext = 0

    /** Ticks stepped so far. */
    var now = 0L
        private set

    fun blockCount(): Int = blocks.size

    /** The trilinear value at (x, y, z). Mutates: catches the blocks it reads up to now. */
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
        val gx = 1f - fx
        val gy = 1f - fy
        val gz = 1f - fz
        var sum = 0f
        sum = acc(sum, gx * gy * gz, ix, iy, iz)
        sum = acc(sum, fx * gy * gz, ix + 1, iy, iz)
        sum = acc(sum, gx * fy * gz, ix, iy + 1, iz)
        sum = acc(sum, fx * fy * gz, ix + 1, iy + 1, iz)
        sum = acc(sum, gx * gy * fz, ix, iy, iz + 1)
        sum = acc(sum, fx * gy * fz, ix + 1, iy, iz + 1)
        sum = acc(sum, gx * fy * fz, ix, iy + 1, iz + 1)
        sum = acc(sum, fx * fy * fz, ix + 1, iy + 1, iz + 1)
        return sum
    }

    /**
     * The trilinear value at (x, y, z), decayed to now: the same number [get] returns, computed
     * without writing anything back. For renderers and other readers outside the simulation step.
     */
    fun peek(x: Float, y: Float, z: Float): Float {
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
            sum += weight * peekVoxel(ix + dx, iy + dy, iz + dz)
        }
        return sum
    }

    /** Deposits [amount] at (x, y, z) with trilinear weights. Mutates. */
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
        val gx = 1f - fx
        val gy = 1f - fy
        val gz = 1f - fz
        deposit(gx * gy * gz, ix, iy, iz, amount)
        deposit(fx * gy * gz, ix + 1, iy, iz, amount)
        deposit(gx * fy * gz, ix, iy + 1, iz, amount)
        deposit(fx * fy * gz, ix + 1, iy + 1, iz, amount)
        deposit(gx * gy * fz, ix, iy, iz + 1, amount)
        deposit(fx * gy * fz, ix + 1, iy, iz + 1, amount)
        deposit(gx * fy * fz, ix, iy + 1, iz + 1, amount)
        deposit(fx * fy * fz, ix + 1, iy + 1, iz + 1, amount)
    }

    private fun acc(sum: Float, weight: Float, gx: Int, gy: Int, gz: Int): Float =
        if (weight == 0f) sum else sum + weight * voxel(gx, gy, gz)

    private fun deposit(weight: Float, gx: Int, gy: Int, gz: Int, amount: Float) {
        if (weight == 0f) return
        val b = blockFor(gx, gy, gz, create = true)!!
        catchUp(b)
        b.v[index(gx, gy, gz)] += amount * weight
    }

    /** Advances one tick: diffuses, or sweeps faded blocks every [SWEEP_TICKS] ticks. Mutates. */
    fun step() {
        if (diffusion > 0f) {
            diffuse()
        } else if ((now + 1) % SWEEP_TICKS == 0L) {
            sweep()
        }
        now++
    }

    /** The strongest voxel, decayed to now. Side-effect free. */
    fun max(): Float {
        var m = 0f
        for (slot in 0 until blocks.capacity()) {
            val b = blocks.valueAt(slot) ?: continue
            val f = factor(b)
            for (value in b.v) {
                val decayed = value * f
                if (decayed > m) m = decayed
            }
        }
        return m
    }

    /**
     * The largest value in each column of voxels over the square of [cells] x [cells] voxels whose
     * corner is (x0, y0), decayed to now, into [out] row by row from the low-y edge. False if all
     * are zero. Side-effect free.
     */
    fun projectMax(x0: Float, y0: Float, cells: Int, out: FloatArray): Boolean {
        out.fill(0f)
        val ix0 = floor(x0 / cellMm).toInt()
        val iy0 = floor(y0 / cellMm).toInt()
        var any = false
        for (slot in 0 until blocks.capacity()) {
            val b = blocks.valueAt(slot) ?: continue
            val gx0 = b.bx * B
            val gy0 = b.by * B
            if (gx0 + B <= ix0 || gx0 >= ix0 + cells || gy0 + B <= iy0 || gy0 >= iy0 + cells) continue
            val f = factor(b)
            for (lz in 0 until B) for (ly in 0 until B) for (lx in 0 until B) {
                val gx = gx0 + lx - ix0
                val gy = gy0 + ly - iy0
                if (gx !in 0 until cells || gy !in 0 until cells) continue
                val value = b.v[(lz * B + ly) * B + lx] * f
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

    private fun peekVoxel(gx: Int, gy: Int, gz: Int): Float {
        val b = blocks.get(key(Math.floorDiv(gx, B), Math.floorDiv(gy, B), Math.floorDiv(gz, B))) ?: return 0f
        return b.v[index(gx, gy, gz)] * factor(b)
    }

    private fun blockFor(gx: Int, gy: Int, gz: Int, create: Boolean): Block? {
        val bx = Math.floorDiv(gx, B)
        val by = Math.floorDiv(gy, B)
        val bz = Math.floorDiv(gz, B)
        val key = key(bx, by, bz)
        for (i in 0 until CACHE) if (cacheKeys[i] == key) return cacheBlocks[i]
        var b = blocks.get(key)
        if (b == null) {
            if (!create) return null
            b = Block(bx, by, bz, now)
            blocks.put(key, b)
        }
        cacheKeys[cacheNext] = key
        cacheBlocks[cacheNext] = b
        cacheNext = (cacheNext + 1) % CACHE
        return b
    }

    /** The decay a block owes since it was last caught up; 1 if it is current. */
    private fun factor(b: Block): Float = if (b.tick == now) 1f else exp(-decayPerSecond * dt * (now - b.tick))

    private fun catchUp(b: Block) {
        if (b.tick == now) return
        val factor = factor(b)
        val v = b.v
        for (i in v.indices) v[i] *= factor
        b.tick = now
    }

    /**
     * One explicit diffusion and decay step over every block. Each block looks up its 6 face
     * neighbors once and reads their boundary voxels straight from their arrays; a missing
     * neighbor reads as 0.
     */
    private fun diffuse() {
        val keep = exp(-decayPerSecond * dt)
        val k = diffusion * dt
        for (slot in 0 until blocks.capacity()) {
            val b = blocks.valueAt(slot) ?: continue
            val src = b.v
            val dst = b.scratch ?: FloatArray(B3).also { b.scratch = it }
            val xLo = blocks.get(key(b.bx - 1, b.by, b.bz))?.v
            val xHi = blocks.get(key(b.bx + 1, b.by, b.bz))?.v
            val yLo = blocks.get(key(b.bx, b.by - 1, b.bz))?.v
            val yHi = blocks.get(key(b.bx, b.by + 1, b.bz))?.v
            val zLo = blocks.get(key(b.bx, b.by, b.bz - 1))?.v
            val zHi = blocks.get(key(b.bx, b.by, b.bz + 1))?.v
            for (lz in 0 until B) for (ly in 0 until B) for (lx in 0 until B) {
                val i = (lz * B + ly) * B + lx
                val c = src[i]
                val left = if (lx > 0) src[i - 1] else xLo?.get(i + B - 1) ?: 0f
                val right = if (lx < B - 1) src[i + 1] else xHi?.get(i - (B - 1)) ?: 0f
                val down = if (ly > 0) src[i - B] else yLo?.get(i + B * (B - 1)) ?: 0f
                val up = if (ly < B - 1) src[i + B] else yHi?.get(i - B * (B - 1)) ?: 0f
                val below = if (lz > 0) src[i - B * B] else zLo?.get(i + B * B * (B - 1)) ?: 0f
                val above = if (lz < B - 1) src[i + B * B] else zHi?.get(i - B * B * (B - 1)) ?: 0f
                val sum = left + right + down + up + below + above
                dst[i] = (c + k * (sum - 6f * c)) * keep
            }
        }
        val dead = ArrayList<Long>()
        for (slot in 0 until blocks.capacity()) {
            val b = blocks.valueAt(slot) ?: continue
            val key = blocks.keyAt(slot)
            val old = b.v
            b.v = b.scratch!!
            b.scratch = old
            b.tick = now + 1
            var m = 0f
            for (value in b.v) if (value > m) m = value
            if (m < releaseBelow) dead += key
        }
        for (key in dead) blocks.remove(key)
        cacheKeys.fill(Long.MIN_VALUE)
        cacheBlocks.fill(null)
    }

    private fun sweep() {
        val dead = ArrayList<Long>()
        for (slot in 0 until blocks.capacity()) {
            val b = blocks.valueAt(slot) ?: continue
            val key = blocks.keyAt(slot)
            catchUp(b)
            var m = 0f
            for (value in b.v) if (value > m) m = value
            if (m < releaseBelow) dead += key
        }
        for (key in dead) blocks.remove(key)
        cacheKeys.fill(Long.MIN_VALUE)
        cacheBlocks.fill(null)
    }

    private fun index(gx: Int, gy: Int, gz: Int): Int =
        (Math.floorMod(gz, B) * B + Math.floorMod(gy, B)) * B + Math.floorMod(gx, B)

    /** A unique key per block: the packed block coordinates. [LongObjectMap] mixes keys itself. */
    private fun key(bx: Int, by: Int, bz: Int): Long =
        ((bx + OFFSET).toLong() shl 42) or ((by + OFFSET).toLong() shl 21) or (bz + OFFSET).toLong()

    private companion object {
        const val B = 10
        const val B3 = B * B * B
        const val CACHE = 4
        const val SWEEP_TICKS = 200L
        const val OFFSET = 1 shl 20
    }
}

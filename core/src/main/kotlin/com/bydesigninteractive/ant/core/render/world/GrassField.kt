package com.bydesigninteractive.ant.core.render.world

import com.bydesigninteractive.ant.sim.util.hash
import com.bydesigninteractive.ant.sim.util.unit
import java.util.Random
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin

/**
 * Patchy grass: candidate spots on a jittered 62.5 mm grid, kept where a seeded 300 mm value-noise
 * "patchiness" is high, and thinned to nothing where spoil is deep (Lasius niger buries
 * vegetation under its mound). Tufts are scenery only. One base tuft of 9 blades is drawn
 * instanced; each instance picks how many blades show, its rotation, scale, tint and sway phase.
 */
class GrassField(private val seed: Long) {
    /**
     * Instance data of the tufts in chunk (cx, cy), [INSTANCE_FLOATS] floats each: x, y, z, rotation (rad),
     * scale, blade count (5 to 9), tint (0.8 to 1.15), sway phase (0 to 6.28). Deterministic per seed and chunk.
     */
    fun tufts(cx: Int, cy: Int, mesher: ChunkMesher): FloatArray {
        val out = ArrayList<Float>()
        for (j in 0 until SPOTS) for (i in 0 until SPOTS) {
            val gi = cx * SPOTS + i
            val gj = cy * SPOTS + j
            val r = Random(hash(seed xor SPOT_SALT, gi, gj))
            val x = (gi + r.nextFloat()) * SPOT_MM
            val y = (gj + r.nextFloat()) * SPOT_MM
            val patch = patchiness(x, y)
            val bare = (mesher.spoilAt(x, y) / SPOIL_BARE).coerceIn(0f, 1f)
            val keep = ((patch - PATCH_LOW) / (1f - PATCH_LOW)).coerceIn(0f, 1f) * (1f - bare)
            if (r.nextFloat() >= keep) continue
            out += x
            out += y
            out += mesher.heights.height(x, y)
            out += r.nextFloat() * 6.2832f
            out += 0.6f + r.nextFloat() * 0.7f
            out += (5 + r.nextInt(5)).toFloat()
            out += 0.8f + r.nextFloat() * 0.35f
            out += r.nextFloat() * 6.2832f
        }
        return out.toFloatArray()
    }

    /** Smooth seeded value noise, 0 to 1, at a 300 mm scale. */
    private fun patchiness(x: Float, y: Float): Float {
        val u = x / PATCH_MM
        val v = y / PATCH_MM
        val i = floor(u).toInt()
        val j = floor(v).toInt()
        val fu = smooth(u - i)
        val fv = smooth(v - j)
        val a = unit(seed xor PATCH_SALT, i, j).toFloat()
        val b = unit(seed xor PATCH_SALT, i + 1, j).toFloat()
        val c = unit(seed xor PATCH_SALT, i, j + 1).toFloat()
        val d = unit(seed xor PATCH_SALT, i + 1, j + 1).toFloat()
        return (a + (b - a) * fu) + ((c + (d - c) * fu) - (a + (b - a) * fu)) * fv
    }

    private fun smooth(t: Float) = t * t * (3f - 2f * t)

    /** The base tuft: 9 single-triangle blades around the origin, up to 1 mm wide at the root and 30 to 80 mm tall before scaling. */
    fun tuftMesh(): FloatArray {
        val r = Random(seed xor TUFT_SALT)
        val out = FloatArray(9 * 3 * TUFT_STRIDE)
        var o = 0
        for (blade in 0 until 9) {
            val a = r.nextFloat() * 6.2832f
            val d = r.nextFloat() * 6f
            val bx = cos(a) * d
            val by = sin(a) * d
            val h = 30f + r.nextFloat() * 50f
            val lean = (r.nextFloat() - 0.5f) * 0.6f * h
            val la = r.nextFloat() * 6.2832f
            val w = 0.6f + r.nextFloat() * 0.4f
            val px = -sin(la) * w
            val py = cos(la) * w
            val nx = cos(la)
            val ny = sin(la)
            for ((vx, vy, vz, f) in listOf(
                floatArrayOf(bx - px, by - py, 0f, 0f), floatArrayOf(bx + px, by + py, 0f, 0f),
                floatArrayOf(bx + cos(la) * lean, by + sin(la) * lean, h, 1f),
            )) {
                out[o] = vx; out[o + 1] = vy; out[o + 2] = vz
                out[o + 3] = nx; out[o + 4] = ny; out[o + 5] = 0.3f
                out[o + 6] = blade.toFloat(); out[o + 7] = f
                o += TUFT_STRIDE
            }
        }
        return out
    }

    companion object {
        const val INSTANCE_FLOATS = 8
        const val TUFT_STRIDE = 8
        /** Pellets per spoil cell at which no grass grows. */
        const val SPOIL_BARE = 3f
        const val SPOTS = 8
        const val SPOT_MM = 62.5f // CHUNK_MM / SPOTS
        const val PATCH_MM = 300f
        const val PATCH_LOW = 0.38f
        private const val SPOT_SALT = 0x6A55L
        private const val PATCH_SALT = 0x9A7CL
        private const val TUFT_SALT = 0x7F7L
    }
}

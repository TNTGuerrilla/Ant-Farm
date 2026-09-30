package com.bydesigninteractive.ant.sim.world.sdf

import com.bydesigninteractive.ant.sim.util.hash
import com.bydesigninteractive.ant.sim.world.CHUNKS
import com.bydesigninteractive.ant.sim.world.CHUNK_MM
import com.bydesigninteractive.ant.sim.world.FoodSource
import com.bydesigninteractive.ant.sim.world.Stone
import com.bydesigninteractive.ant.sim.world.SurfaceMap
import java.util.Random
import kotlin.math.floor
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.sqrt

/**
 * The walkable surface as a signed distance function in surface millimeters (z up), negative
 * inside matter: the ground, pebbles and rocks generated per chunk, objects [placed] at run
 * time, and food shapes (aphid plant stems and clusters, prey lumps, feeders). Shapes are joined
 * with a smooth minimum, so the crease where a rock meets the ground is rounded.
 */
class SurfaceSdf(private val map: SurfaceMap, private val rocks: Boolean = true) {
    /** Objects placed at run time (tests now; debris in a later milestone). */
    val placed = ArrayList<Blob>()
    private val own = HashMap<Int, List<Blob>>()
    private val near = HashMap<Int, List<Blob>>()
    private val g = FloatArray(3)

    fun distance(x: Float, y: Float, z: Float): Float {
        var d = map.ground.distance(x, y, z)
        for (b in blobsNear(x, y)) if (b.near(x, y, z, MARGIN)) d = smin(d, b.distance(x, y, z), BLEND)
        for (b in placed) if (b.near(x, y, z, MARGIN)) d = smin(d, b.distance(x, y, z), BLEND)
        for (f in map.foods) {
            val body = f.body
            if (body != null && body.near(x, y, z, MARGIN)) d = smin(d, body.distance(x, y, z), BLEND)
            val stem = f.stem
            if (stem != null && stem.near(x, y, z, MARGIN)) d = smin(d, stem.distance(x, y, z), BLEND)
        }
        return d
    }

    /** The unit surface normal direction at (x, y, z) into [out], by central differences. */
    fun gradient(x: Float, y: Float, z: Float, out: FloatArray) {
        val gx = distance(x + E, y, z) - distance(x - E, y, z)
        val gy = distance(x, y + E, z) - distance(x, y - E, z)
        val gz = distance(x, y, z + E) - distance(x, y, z - E)
        val len = sqrt(gx * gx + gy * gy + gz * gz)
        if (len < 1e-9f) {
            out[0] = 0f
            out[1] = 0f
            out[2] = 1f
        } else {
            out[0] = gx / len
            out[1] = gy / len
            out[2] = gz / len
        }
    }

    /** Moves the point (p[0], p[1], p[2]) onto the surface with [ITERATIONS] Newton steps. */
    fun project(p: FloatArray) {
        repeat(ITERATIONS) {
            val d = distance(p[0], p[1], p[2])
            gradient(p[0], p[1], p[2], g)
            p[0] -= g[0] * d
            p[1] -= g[1] * d
            p[2] -= g[2] * d
        }
    }

    /** The plant whose stem surface is within [within] of (x, y, z), or null. */
    fun stemAt(x: Float, y: Float, z: Float, within: Float): FoodSource? {
        for (f in map.foods) {
            val s = f.stem ?: continue
            if (s.distance(x, y, z) <= within) return f
        }
        return null
    }

    /** The pebbles and rocks generated in one chunk (empty for a world without rocks). */
    fun ownBlobs(cx: Int, cy: Int): List<Blob> =
        own.getOrPut(cx + cy * CHUNKS) { if (rocks) generate(cx, cy) else emptyList() }

    private fun blobsNear(x: Float, y: Float): List<Blob> {
        val cx = floor(x / CHUNK_MM).toInt().coerceIn(0, CHUNKS - 1)
        val cy = floor(y / CHUNK_MM).toInt().coerceIn(0, CHUNKS - 1)
        return near.getOrPut(cx + cy * CHUNKS) { gather(cx, cy) }
    }

    /** This chunk's blobs plus neighbors' blobs whose bounds reach into it. */
    private fun gather(cx: Int, cy: Int): List<Blob> {
        val x0 = cx * CHUNK_MM - MARGIN
        val y0 = cy * CHUNK_MM - MARGIN
        val x1 = (cx + 1) * CHUNK_MM + MARGIN
        val y1 = (cy + 1) * CHUNK_MM + MARGIN
        val out = ArrayList<Blob>()
        for (ny in cy - 1..cy + 1) for (nx in cx - 1..cx + 1) {
            if (nx < 0 || ny < 0 || nx >= CHUNKS || ny >= CHUNKS) continue
            for (b in ownBlobs(nx, ny)) {
                if (b.cx + b.reach >= x0 && b.cx - b.reach <= x1 && b.cy + b.reach >= y0 && b.cy - b.reach <= y1) out += b
            }
        }
        return out
    }

    private fun generate(cx: Int, cy: Int): List<Blob> {
        val content = map.chunk(cx, cy)
        val r = Random(hash(map.seed xor BLOB_SALT, cx, cy))
        val out = ArrayList<Blob>()
        for (s in content.stones) addBlob(out, r, s, PEBBLE_LUMP)
        for (s in content.rocks) addBlob(out, r, s, ROCK_LUMP)
        return out
    }

    /** Shapes a partly buried lumpy blob from a 2D stone; all draws happen before any skip. */
    private fun addBlob(out: ArrayList<Blob>, r: Random, s: Stone, lump: Float) {
        val rx = s.radius * (0.8f + 0.4f * r.nextFloat())
        val ry = s.radius * (0.8f + 0.4f * r.nextFloat())
        val rz = s.radius * (0.5f + 0.3f * r.nextFloat())
        val phase = r.nextFloat() * 6.283f
        if (hypot(s.x - map.entranceX, s.y - map.entranceY) - max(rx, ry) < ENTRANCE_CLEARANCE) return
        val cz = map.ground.base(s.x, s.y) + rz * BURIED
        out += Blob(s.x, s.y, cz, rx, ry, rz, lump, phase)
    }

    private companion object {
        const val BLEND = 1.5f
        const val MARGIN = 10f
        const val E = 0.25f
        const val ITERATIONS = 2
        const val ENTRANCE_CLEARANCE = 30f
        const val BURIED = 0.3f
        const val PEBBLE_LUMP = 0.12f
        const val ROCK_LUMP = 0.15f
        const val BLOB_SALT = 0xB10BL
    }
}

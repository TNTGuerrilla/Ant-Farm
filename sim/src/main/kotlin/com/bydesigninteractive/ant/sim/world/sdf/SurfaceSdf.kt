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
import kotlin.math.sqrt

/**
 * The walkable surface as a signed distance function in surface millimeters (z up), negative
 * inside matter: the ground, pebbles and rocks generated per chunk, objects [placed] at run
 * time, and food shapes (aphid plant stems and clusters, prey lumps, feeders). Shapes are joined
 * with a smooth minimum, so the crease where a rock meets the ground is rounded.
 *
 * Rocks are generated per chunk on first query and skip the entrance and every food in
 * [SurfaceMap.foods] and [SurfaceMap.pastFoods] (used-up food is kept there). So for any world
 * whose food is placed before it starts, as in every current scenario, the rocks are the same
 * whenever a chunk is first queried, by an ant or by a renderer. The remaining limit: food added
 * at run time does not clear rocks from a chunk that was already generated, and a chunk generated
 * before that food was added keeps them. Settle this before run-time prey spawning or snapshots.
 */
class SurfaceSdf(private val map: SurfaceMap, private val rocks: Boolean = true) {
    /** Objects placed at run time (tests now; debris in a later milestone). */
    val placed = ArrayList<Blob>()
    private val own = arrayOfNulls<List<Blob>>(CHUNKS * CHUNKS)
    private val near = arrayOfNulls<List<Blob>>(CHUNKS * CHUNKS)
    private val g = FloatArray(3)

    /**
     * The signed distance at (x, y, z). It is a bound joined by the smooth minimum, exact near the
     * surface, and it ignores shapes beyond [MARGIN].
     */
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

    /**
     * Moves the point (p[0], p[1], p[2]) onto the surface with [iterations] Newton steps (two keep
     * an ant within 0.5 mm; one is enough for a point that only needs to be near the surface). It
     * reuses a scratch array, so it is for one simulation thread.
     */
    fun project(p: FloatArray, iterations: Int = ITERATIONS) {
        repeat(iterations) {
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
        own[cx + cy * CHUNKS] ?: (if (rocks) generate(cx, cy) else emptyList()).also { own[cx + cy * CHUNKS] = it }

    private fun blobsNear(x: Float, y: Float): List<Blob> {
        val cx = floor(x / CHUNK_MM).toInt().coerceIn(0, CHUNKS - 1)
        val cy = floor(y / CHUNK_MM).toInt().coerceIn(0, CHUNKS - 1)
        return near[cx + cy * CHUNKS] ?: gather(cx, cy).also { near[cx + cy * CHUNKS] = it }
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

    /** Shapes a partly buried lumpy blob from a 2D stone; all draws happen before any skip. Skips blobs near the entrance or any food, present or used up. */
    private fun addBlob(out: ArrayList<Blob>, r: Random, s: Stone, lump: Float) {
        val rx = s.radius * (0.8f + 0.4f * r.nextFloat())
        val ry = s.radius * (0.8f + 0.4f * r.nextFloat())
        val rz = s.radius * (0.5f + 0.3f * r.nextFloat())
        val phase = r.nextFloat() * 6.283f
        val cz = map.ground.base(s.x, s.y) + rz * BURIED
        val blob = Blob(s.x, s.y, cz, rx, ry, rz, lump, phase)
        if (hypot(s.x - map.entranceX, s.y - map.entranceY) - blob.reach < ENTRANCE_CLEARANCE) return
        for (f in map.foods) if (hypot(f.x - s.x, f.y - s.y) - blob.reach < FOOD_CLEARANCE) return
        for (f in map.pastFoods) if (hypot(f.x - s.x, f.y - s.y) - blob.reach < FOOD_CLEARANCE) return
        out += blob
    }

    private companion object {
        const val BLEND = 1.5f
        const val MARGIN = 10f
        const val E = 0.25f
        const val ITERATIONS = 2
        const val ENTRANCE_CLEARANCE = 30f
        const val FOOD_CLEARANCE = 20f
        const val BURIED = 0.3f
        const val PEBBLE_LUMP = 0.12f
        const val ROCK_LUMP = 0.15f
        const val BLOB_SALT = 0xB10BL
    }
}

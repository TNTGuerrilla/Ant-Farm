package com.bydesigninteractive.ant.sim.world

import com.bydesigninteractive.ant.sim.util.hash
import com.bydesigninteractive.ant.sim.world.sdf.HeightField
import com.bydesigninteractive.ant.sim.world.sdf.SurfaceSdf
import java.util.Random
import kotlin.math.PI
import kotlin.math.hypot

/** The surface map is 8 x 8 m with the nest entrance in the middle (reference section 13). */
const val SURFACE_MM = 8000
const val CHUNK_MM = 500
const val CHUNKS = SURFACE_MM / CHUNK_MM
const val FIELD_CELL_MM = 10

/**
 * The ground around the nest: chunk content generated on demand, the scalar fields ants read and
 * write, and the food sources.
 */
class SurfaceMap(val seed: Long, rocks: Boolean = true) {
    val generator = SurfaceGenerator(seed)
    val trail = ChunkedField()
    val homeScent = ChunkedField()

    /** Soil pellets dumped by diggers, as a count per field cell. */
    val spoil = ChunkedField()

    /** The ground height field (reference section 13). */
    val ground = HeightField(seed, spoil)
    val foods = ArrayList<FoodSource>()
    val entranceX = SURFACE_MM / 2f
    val entranceY = SURFACE_MM / 2f
    private val chunks = HashMap<Int, ChunkContent>()

    /** The walkable surface as a signed distance function. */
    val sdf = SurfaceSdf(this, rocks)

    fun chunk(cx: Int, cy: Int): ChunkContent {
        require(cx in 0 until CHUNKS && cy in 0 until CHUNKS) { "chunk ($cx, $cy) is outside the map" }
        return chunks.getOrPut(cx + cy * CHUNKS) { generator.chunk(cx, cy) }
    }

    /** 2 to 4 aphid plants 0.3 to 3 m out, closer ones more likely (reference section 13). */
    fun placePlants() {
        val r = Random(hash(seed, PLANT_SALT, 0))
        repeat(2 + r.nextInt(3)) {
            val u = r.nextFloat()
            val d = 300f + u * u * 2700f
            val a = r.nextFloat() * 2f * PI.toFloat()
            val quality = 0.5f + r.nextFloat() * 0.5f
            val tall = 500f + r.nextFloat() * 700f
            val px = entranceX + d * StrictMath.cos(a.toDouble()).toFloat()
            val py = entranceY + d * StrictMath.sin(a.toDouble()).toFloat()
            val base = ground.base(px, py)
            foods += FoodSource(
                foods.size, FoodKind.HONEYDEW, px, py,
                radius = 12f, quality = quality,
                z = base + tall - 30f, bodyRadius = 8f, stemRadius = 2.5f, stemBase = base - 5f,
            )
        }
    }

    /** Scattered prey items 0.2 to 2 m out, each worth 20 trips. */
    fun placePrey(count: Int = 6) {
        val r = Random(hash(seed, PREY_SALT, 0))
        repeat(count) {
            val d = 200f + r.nextFloat() * 1800f
            val a = r.nextFloat() * 2f * PI.toFloat()
            val body = 3f + r.nextFloat() * 5f
            val px = entranceX + d * StrictMath.cos(a.toDouble()).toFloat()
            val py = entranceY + d * StrictMath.sin(a.toDouble()).toFloat()
            foods += FoodSource(
                foods.size, FoodKind.PREY, px, py,
                radius = body + 4f, quality = 0.8f, loads = 20,
                z = ground.base(px, py) + body * 0.4f, bodyRadius = body,
            )
        }
    }

    /** The closest food whose edge is within [within] of (x, y), or null. */
    fun nearestFood(x: Float, y: Float, within: Float): FoodSource? {
        var best: FoodSource? = null
        var bestGap = within
        for (f in foods) {
            val gap = hypot(f.x - x, f.y - y) - f.radius
            if (gap <= bestGap) {
                bestGap = gap
                best = f
            }
        }
        return best
    }

    fun step(dt: Float, trailDecay: Float, trailDiffusion: Float, scentDecay: Float) {
        trail.step(dt, trailDecay, trailDiffusion)
        homeScent.step(dt, scentDecay, 0f)
    }

    private companion object {
        const val PLANT_SALT = 99
        const val PREY_SALT = 98
    }
}

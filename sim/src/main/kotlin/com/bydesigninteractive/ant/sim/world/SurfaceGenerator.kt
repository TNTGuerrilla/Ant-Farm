package com.bydesigninteractive.ant.sim.world

import com.bydesigninteractive.ant.sim.util.hash
import java.util.Random

data class Tuft(val x: Float, val y: Float, val height: Float)

data class Stone(val x: Float, val y: Float, val radius: Float)

/** What lies on one 50 x 50 cm chunk of ground. [litter] is how much of it is leaf litter, 0 to 1. */
class ChunkContent(val cx: Int, val cy: Int, val litter: Float, val tufts: List<Tuft>, val stones: List<Stone>)

/** Generates chunk content from the seed and chunk coordinates, so any chunk can be rebuilt. */
class SurfaceGenerator(private val seed: Long) {
    fun chunk(cx: Int, cy: Int): ChunkContent {
        val r = Random(hash(seed, cx, cy))
        val x0 = cx * CHUNK_MM.toFloat()
        val y0 = cy * CHUNK_MM.toFloat()
        val litter = r.nextFloat()
        val tufts = List(20 + r.nextInt(40)) {
            Tuft(x0 + r.nextFloat() * CHUNK_MM, y0 + r.nextFloat() * CHUNK_MM, 30f + r.nextFloat() * 60f)
        }
        val stones = List(r.nextInt(4)) {
            Stone(x0 + r.nextFloat() * CHUNK_MM, y0 + r.nextFloat() * CHUNK_MM, 5f + r.nextFloat() * 35f)
        }
        return ChunkContent(cx, cy, litter, tufts, stones)
    }
}

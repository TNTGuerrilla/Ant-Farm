package com.bydesigninteractive.ant.core.render.world

import com.bydesigninteractive.ant.sim.world.CHUNK_MM
import com.bydesigninteractive.ant.sim.world.sdf.Blob
import kotlin.math.hypot
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GrassFieldTest {
    @Test
    fun grassIsPatchyAndRepeatable() {
        val m = ChunkMesher(4)
        val g = GrassField(4)
        var total = 0
        var empty = 0
        for (cy in 2 until 14) for (cx in 2 until 14) {
            val n = g.tufts(cx, cy, m).size / GrassField.INSTANCE_FLOATS
            total += n
            if (n < 5) empty++
        }
        val perChunk = total / 144f
        assertTrue(perChunk in 15f..120f, "tufts per chunk $perChunk")
        assertTrue(empty >= 10, "patchy grass leaves some chunks nearly bare: $empty")
        assertContentEquals(g.tufts(5, 5, m), GrassField(4).tufts(5, 5, ChunkMesher(4)))
    }

    @Test
    fun tuftsStandOnTheGround() {
        val m = ChunkMesher(4)
        val t = GrassField(4).tufts(6, 9, m)
        var i = 0
        while (i < t.size) {
            assertEquals(m.heights.height(t[i], t[i + 1]), t[i + 2], 1e-3f)
            assertTrue(t[i + 5] in 5f..9f)
            i += GrassField.INSTANCE_FLOATS
        }
    }

    @Test
    fun deepSpoilLeavesNoGrass() {
        val m = ChunkMesher(4)
        val g = GrassField(4)
        val cells = CHUNK_MM / 10
        var cx = 2
        while (g.tufts(cx, 8, m).isEmpty()) cx++
        m.setSpoil(cx, 8, FloatArray(cells * cells) { GrassField.SPOIL_BARE })
        assertEquals(0, g.tufts(cx, 8, m).size)
    }

    @Test
    fun theTuftHasNineTaggedBlades() {
        val v = GrassField(4).tuftMesh()
        assertEquals(9 * 3 * GrassField.TUFT_STRIDE, v.size)
        val blades = (0 until v.size / GrassField.TUFT_STRIDE).map { v[it * GrassField.TUFT_STRIDE + 6].toInt() }.toSet()
        assertEquals((0 until 9).toSet(), blades)
    }

    @Test
    fun noGrassGrowsInsideRocks() {
        val m = ChunkMesher(4)
        val g = GrassField(4)
        var cx = 2
        while (g.tufts(cx, 8, m).size < 3 * GrassField.INSTANCE_FLOATS) cx++
        val bare = g.tufts(cx, 8, m)
        // A rock centred on the first tuft, and one centred in the chunk to the west whose footprint crosses the edge.
        val last = bare.size - GrassField.INSTANCE_FLOATS
        val rocks = listOf(
            Blob(bare[0], bare[1], bare[2], 20f, 15f, 10f),
            Blob(cx * CHUNK_MM - 5f, bare[last + 1], 0f, 8f, 8f, 5f),
        )
        val withRocks = g.tufts(cx, 8, m, rocks)
        assertTrue(withRocks.size < bare.size)
        fun inside(x: Float, y: Float) = rocks.any { hypot(x - it.cx, y - it.cy) < it.reach + GrassField.ROCK_CLEARANCE }
        var kept = 0
        var i = 0
        while (i < bare.size) {
            if (!inside(bare[i], bare[i + 1])) kept++
            i += GrassField.INSTANCE_FLOATS
        }
        assertEquals(kept * GrassField.INSTANCE_FLOATS, withRocks.size, "every tuft outside the rocks stays")
        i = 0
        while (i < withRocks.size) {
            assertTrue(!inside(withRocks[i], withRocks[i + 1]), "tuft at ${withRocks[i]}, ${withRocks[i + 1]} is inside a rock")
            i += GrassField.INSTANCE_FLOATS
        }
    }
}

package com.bydesigninteractive.ant.core.render.world

import com.bydesigninteractive.ant.sim.world.CHUNK_MM
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
}

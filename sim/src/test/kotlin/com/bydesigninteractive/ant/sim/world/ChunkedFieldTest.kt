package com.bydesigninteractive.ant.sim.world

import kotlin.math.exp
import kotlin.test.Test
import kotlin.test.assertEquals

class ChunkedFieldTest {
    @Test
    fun addAndReadBackByMillimeters() {
        val f = ChunkedField()
        f.add(1234f, 567f, 2f)
        assertEquals(2f, f.get(1239f, 561f))
        assertEquals(0f, f.get(1241f, 567f))
        assertEquals(1, f.allocatedChunks())
    }

    @Test
    fun decaysAtTheGivenRate() {
        val f = ChunkedField()
        f.add(100f, 100f, 1f)
        repeat(10) { f.step(1f, 0.004f, 0f) }
        assertEquals(exp(-0.04f), f.get(100f, 100f), 1e-5f)
    }

    @Test
    fun diffusionSpreadsToNeighborsAndKeepsTheTotal() {
        val f = ChunkedField()
        f.add(1205f, 1205f, 1f) // cell (120, 120), well inside chunk (2, 2)
        f.step(0.05f, 0f, 1f)
        assertEquals(0.8f, f.cell(120, 120), 1e-6f)
        assertEquals(0.05f, f.cell(121, 120), 1e-6f)
        assertEquals(0.05f, f.cell(120, 119), 1e-6f)
        assertEquals(1f, f.total(), 1e-5f)
    }

    @Test
    fun diffusionCrossesIntoAllocatedNeighborChunks() {
        val f = ChunkedField()
        f.add(495f, 105f, 1f) // cell (49, 10), last column of chunk (0, 0)
        f.add(505f, 105f, 0f) // allocates chunk (1, 0)
        f.step(0.05f, 0f, 1f)
        assertEquals(0.05f, f.cell(50, 10), 1e-6f)
    }

    @Test
    fun fadedChunksAreReleased() {
        val f = ChunkedField()
        f.add(100f, 100f, 1f)
        f.step(10f, 1f, 0f)
        assertEquals(0, f.allocatedChunks())
        assertEquals(0f, f.get(100f, 100f))
    }

    @Test
    fun outsideTheMapReadsZeroAndIgnoresWrites() {
        val f = ChunkedField()
        f.add(-5f, 10f, 1f)
        f.add(8000f, 10f, 1f)
        assertEquals(0, f.allocatedChunks())
        assertEquals(0f, f.get(-5f, 10f))
    }
}

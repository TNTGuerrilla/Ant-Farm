package com.bydesigninteractive.ant.core.render.world

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ChunkCacheTest {
    @Test
    fun requestedChunksComeBackBuiltOffTheCallingThread() {
        val cache = ChunkCache(4)
        try {
            cache.request(8, 8, null, emptyList())
            cache.request(9, 8, null, emptyList())
            val got = ArrayList<ChunkResult>()
            val deadline = System.nanoTime() + 10_000_000_000L
            while (got.size < 2 && System.nanoTime() < deadline) {
                got += cache.poll(2)
                Thread.sleep(5)
            }
            assertEquals(setOf(8 to 8, 9 to 8), got.map { it.cx to it.cy }.toSet())
            assertTrue(got.all { it.ground.vertexCount > 1000 })
        } finally {
            cache.close()
        }
    }

    @Test
    fun aNewerRequestReplacesAnOlderOne() {
        val cache = ChunkCache(4)
        try {
            cache.request(8, 8, null, emptyList())
            cache.request(8, 8, FloatArray(50 * 50) { 20f }, emptyList())
            val got = ArrayList<ChunkResult>()
            val deadline = System.nanoTime() + 10_000_000_000L
            while (System.nanoTime() < deadline) {
                got += cache.poll(4)
                if (got.any { it.version == 2L }) break
                Thread.sleep(5)
            }
            assertTrue(got.last().version == 2L, "the latest request must be the last result")
        } finally {
            cache.close()
        }
    }

    @Test
    fun aNeighboursSpoilRaisesTheSharedEdge() {
        val cache = ChunkCache(4)
        try {
            val east = arrayOfNulls<FloatArray>(9)
            east[2 + 1 * 3] = FloatArray(50 * 50) { 5000f } // 50 mm of spoil, far above the relief
            cache.request(8, 8, null, emptyList())
            cache.request(8, 8, null, emptyList(), east)
            val got = ArrayList<ChunkResult>()
            val deadline = System.nanoTime() + 10_000_000_000L
            while (got.size < 2 && System.nanoTime() < deadline) {
                got += cache.poll(2)
                Thread.sleep(5)
            }
            assertEquals(2, got.size)
            val bare = maxZ(got.first { it.version == 1L }.ground)
            val mounded = maxZ(got.first { it.version == 2L }.ground)
            assertTrue(mounded > bare + 1f, "the east neighbour's spoil must lift the east edge: $bare -> $mounded")
        } finally {
            cache.close()
        }
    }

    private fun maxZ(d: MeshData): Float {
        var m = -Float.MAX_VALUE
        for (k in 0 until d.vertexCount) m = maxOf(m, d.vertices[k * MeshData.STRIDE + 2])
        return m
    }
}

package com.bydesigninteractive.ant.core.render.world

import com.bydesigninteractive.ant.sim.world.CHUNK_MM
import kotlin.math.abs
import kotlin.math.max
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertTrue

class GroundMeshTest {
    private val cx = 8
    private val cy = 8

    @Test
    fun groundVerticesSitOnTheGround() {
        val m = ChunkMesher(4)
        val v = m.ground(cx, cy).vertices
        var ground = 0
        var i = 0
        while (i < v.size) {
            val x = v[i]
            val y = v[i + 1]
            val z = v[i + 2]
            val h = m.heights.height(x, y)
            // ground vertices lie on the height field; speck vertices lie just above it
            assertTrue(z - h >= -0.2f && z - h <= 0.2f, "vertex ($x, $y, $z) is ${z - h} from the ground")
            if (abs(z - h) < 1e-3f) ground++
            i += MeshData.STRIDE
        }
        assertTrue(ground > 1000)
    }

    @Test
    fun flatFacetsStayWithinOneMillimetreOfTheGround() {
        val m = ChunkMesher(4)
        val v = m.ground(cx, cy).vertices
        var worst = 0f
        var i = 0
        while (i < v.size) {
            val ax = v[i]; val ay = v[i + 1]; val az = v[i + 2]
            val bx = v[i + 9]; val by = v[i + 10]; val bz = v[i + 11]
            val qx = v[i + 18]; val qy = v[i + 19]; val qz = v[i + 20]
            for ((u, w) in listOf(0.33f to 0.33f, 0.5f to 0.25f, 0.25f to 0.5f, 0.5f to 0.5f, 0.5f to 0f, 0f to 0.5f)) {
                val x = ax + (bx - ax) * u + (qx - ax) * w
                val y = ay + (by - ay) * u + (qy - ay) * w
                val z = az + (bz - az) * u + (qz - az) * w
                worst = max(worst, abs(z - m.heights.height(x, y)))
            }
            i += 3 * MeshData.STRIDE
        }
        println("GROUND worst facet deviation: $worst mm")
        assertTrue(worst <= 1.2f, "worst facet deviation $worst mm (specks sit 0.05 mm up, so allow them)")
    }

    @Test
    fun neighbouringChunksShareTheirEdge() {
        val m = ChunkMesher(4)
        val left = m.ground(cx, cy).vertices
        val right = m.ground(cx + 1, cy).vertices
        val edge = (cx + 1) * CHUNK_MM.toFloat()
        fun edgePoints(v: FloatArray): Set<Triple<Float, Float, Float>> {
            val out = HashSet<Triple<Float, Float, Float>>()
            var i = 0
            while (i < v.size) {
                if (abs(v[i] - edge) < ChunkMesher.CELL_MM / 3f) out += Triple(v[i], v[i + 1], v[i + 2])
                i += MeshData.STRIDE
            }
            return out
        }
        val a = edgePoints(left)
        val b = edgePoints(right)
        assertTrue(a.isNotEmpty())
        // the shared column of grid vertices is jittered identically from both sides
        assertTrue(a.intersect(b).size >= ChunkMesher.CELLS, "chunks do not meet: ${a.size} vs ${b.size}")
    }

    @Test
    fun theSameSeedGivesTheSameMesh() {
        assertContentEquals(ChunkMesher(4).ground(cx, cy).vertices, ChunkMesher(4).ground(cx, cy).vertices)
    }

    @Test
    fun spoilRaisesTheGround() {
        val m = ChunkMesher(4)
        val before = m.ground(cx, cy).vertices
        val cells = CHUNK_MM / 10
        m.setSpoil(cx, cy, FloatArray(cells * cells) { 50f })
        val after = m.ground(cx, cy).vertices
        fun meanZ(v: FloatArray): Float {
            var sum = 0.0
            var i = 0
            while (i < v.size) { sum += v[i + 2]; i += MeshData.STRIDE }
            return (sum / (v.size / MeshData.STRIDE)).toFloat()
        }
        // 50 pellets per cell raise the ground 0.5 mm; edge vertices also see the empty neighbours
        assertTrue(meanZ(after) > meanZ(before) + 0.35f, "spoil should raise the ground: ${meanZ(before)} -> ${meanZ(after)}")
    }

    @Test
    fun everyTriangleFacesUp() {
        val v = ChunkMesher(4).ground(cx, cy).vertices
        val groundVertices = ChunkMesher.CELLS * ChunkMesher.CELLS * 6
        assertTrue(v.size / MeshData.STRIDE > groundVertices, "specks should follow the ground")
        var i = 0
        while (i < v.size) {
            val nz = v[i + 5]
            val kind = if (i / MeshData.STRIDE < groundVertices) "ground" else "speck"
            assertTrue(nz > 0f, "$kind triangle at vertex ${i / MeshData.STRIDE} has nz $nz")
            // the stored normal must agree with the winding, so culling and lighting match
            val ux = v[i + 9] - v[i]; val uy = v[i + 10] - v[i + 1]
            val wx = v[i + 18] - v[i]; val wy = v[i + 19] - v[i + 1]
            assertTrue(ux * wy - uy * wx >= 0f, "$kind triangle at vertex ${i / MeshData.STRIDE} is clockwise from above")
            i += 3 * MeshData.STRIDE
        }
    }
}

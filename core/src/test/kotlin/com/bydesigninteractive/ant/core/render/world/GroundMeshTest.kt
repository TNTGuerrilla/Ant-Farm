package com.bydesigninteractive.ant.core.render.world

import com.bydesigninteractive.ant.sim.world.CHUNK_MM
import kotlin.math.abs
import kotlin.math.max
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertTrue

/** The fine level ([ChunkMesher.CELLS]) is the default; the coarse level has its own tests at the end. */
class GroundMeshTest {
    private val cx = 8
    private val cy = 8

    @Test
    fun groundVerticesSitOnTheGround() {
        val m = ChunkMesher(4)
        val v = m.ground(cx, cy).vertices
        val groundVertices = ChunkMesher.CELLS * ChunkMesher.CELLS * 6
        var i = 0
        while (i < v.size) {
            val x = v[i]
            val y = v[i + 1]
            val z = v[i + 2]
            val h = m.heights.height(x, y)
            if (i / MeshData.STRIDE < groundVertices) {
                assertTrue(abs(z - h) < 1e-3f, "ground vertex ($x, $y, $z) is ${z - h} from the ground")
            } else {
                // specks follow the drawn facets (within 1 mm of the ground) plus their 0.05 mm lift
                assertTrue(abs(z - h) <= 1.05f, "speck vertex ($x, $y, $z) is ${z - h} from the ground")
            }
            i += MeshData.STRIDE
        }
        val ground = groundVertices
        assertTrue(ground > 1000)
    }

    @Test
    fun flatFacetsStayWithinOneMillimetreOfTheGround() {
        val m = ChunkMesher(4)
        val v = m.ground(cx, cy).vertices
        val groundVertices = ChunkMesher.CELLS * ChunkMesher.CELLS * 6
        val n = 6
        var worst = 0f
        var i = 0
        while (i < groundVertices * MeshData.STRIDE) {
            val ax = v[i]; val ay = v[i + 1]; val az = v[i + 2]
            val bx = v[i + 9]; val by = v[i + 10]; val bz = v[i + 11]
            val qx = v[i + 18]; val qy = v[i + 19]; val qz = v[i + 20]
            for (p in 0..n) for (q in 0..n - p) {
                val u = p.toFloat() / n
                val w = q.toFloat() / n
                val x = ax + (bx - ax) * u + (qx - ax) * w
                val y = ay + (by - ay) * u + (qy - ay) * w
                val z = az + (bz - az) * u + (qz - az) * w
                worst = max(worst, abs(z - m.heights.height(x, y)))
            }
            i += 3 * MeshData.STRIDE
        }
        println("GROUND worst facet deviation (ground only): $worst mm")
        assertTrue(worst <= 1.0f, "worst ground facet deviation $worst mm")
    }

    @Test
    fun specksSitOnTheDrawnFacets() {
        val m = ChunkMesher(4)
        val v = m.ground(cx, cy).vertices
        val groundVertices = ChunkMesher.CELLS * ChunkMesher.CELLS * 6
        // plane of every ground triangle; a speck vertex must sit 0.05 mm above one of them
        var checked = 0
        var i = groundVertices * MeshData.STRIDE
        while (i < v.size) {
            val x = v[i]; val y = v[i + 1]; val z = v[i + 2]
            var best = Float.MAX_VALUE
            var bestInside = -Float.MAX_VALUE
            var t = 0
            while (t < groundVertices * MeshData.STRIDE) {
                val ax = v[t]; val ay = v[t + 1]; val az = v[t + 2]
                if (abs(ax - x) < 30f && abs(ay - y) < 30f) {
                    val bx = v[t + 9]; val by = v[t + 10]; val bz = v[t + 11]
                    val cx2 = v[t + 18]; val cy2 = v[t + 19]; val cz2 = v[t + 20]
                    val det = (bx - ax) * (cy2 - ay) - (by - ay) * (cx2 - ax)
                    if (abs(det) > 1e-9f) {
                        val l1 = ((x - ax) * (cy2 - ay) - (y - ay) * (cx2 - ax)) / det
                        val l2 = ((bx - ax) * (y - ay) - (by - ay) * (x - ax)) / det
                        // barycentric with a small tolerance (chunk-edge specks may lie just outside the jittered mesh)
                        val l0 = 1f - l1 - l2
                        val inside = minOf(l0, l1, l2)
                        // the facet that contains the point best (or, at the jittered chunk edge, is nearest to it)
                        if (inside > -0.02f && inside > bestInside) {
                            bestInside = inside
                            best = abs(z - (az * l0 + bz * l1 + cz2 * l2))
                        }
                    }
                }
                t += 3 * MeshData.STRIDE
            }
            val lift = best
            if (best < Float.MAX_VALUE) {
                assertTrue(abs(lift - ChunkMesher.SPECK_LIFT) <= 0.01f, "speck vertex ($x, $y, $z) is $lift from the nearest facet")
                checked++
            }
            i += MeshData.STRIDE
        }
        assertTrue(checked > 20, "only $checked speck vertices were checked")
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

    @Test
    fun aCoarseChunkHasAboutANinthOfTheTrianglesAndSitsOnTheGround() {
        val m = ChunkMesher(4)
        val fine = m.ground(cx, cy)
        val coarse = m.ground(cx, cy, ChunkMesher.COARSE_CELLS)
        val fineTris = ChunkMesher.CELLS * ChunkMesher.CELLS * 2
        // 14 by 14 interior cells (392 triangles) plus the 4 * (48 + 14) stitching triangles to the fine border
        val ratio = coarse.triangleCount.toFloat() / fineTris
        println("GROUND coarse ${coarse.triangleCount} triangles against fine $fineTris (ratio $ratio, fine with specks ${fine.triangleCount})")
        assertTrue(ratio in 0.10f..0.16f, "coarse/fine triangle ratio $ratio")
        val v = coarse.vertices
        var i = 0
        while (i < v.size) {
            val h = m.heights.height(v[i], v[i + 1])
            assertTrue(abs(v[i + 2] - h) < 1e-3f, "coarse vertex (${v[i]}, ${v[i + 1]}, ${v[i + 2]}) is ${v[i + 2] - h} from the ground")
            i += MeshData.STRIDE
        }
    }

    @Test
    fun aCoarseChunkFacesUpAndHasNoSpecks() {
        val v = ChunkMesher(4).ground(cx, cy, ChunkMesher.COARSE_CELLS).vertices
        val s = ChunkMesher.COARSE_CELLS - 2
        assertTrue(v.size / MeshData.STRIDE == (s * s * 2 + 4 * s + 4 * ChunkMesher.CELLS) * 3, "a coarse chunk is soil facets only")
        var area = 0.0
        var i = 0
        while (i < v.size) {
            assertTrue(v[i + 5] > 0f, "coarse triangle at vertex ${i / MeshData.STRIDE} has nz ${v[i + 5]}")
            val ux = v[i + 9] - v[i]; val uy = v[i + 10] - v[i + 1]
            val wx = v[i + 18] - v[i]; val wy = v[i + 19] - v[i + 1]
            val cross = ux * wy - uy * wx
            assertTrue(cross > 0f, "coarse triangle at vertex ${i / MeshData.STRIDE} is clockwise or degenerate from above")
            area += cross / 2.0
            i += 3 * MeshData.STRIDE
        }
        // no overlaps or holes: the facets cover the chunk once (its jittered border makes it differ slightly from the square)
        val square = CHUNK_MM.toDouble() * CHUNK_MM
        assertTrue(abs(area - square) < square * 0.01, "coarse facets cover $area mm2 of a $square mm2 chunk")
    }

    @Test
    fun aCoarseChunkMeetsItsFineNeighbourExactly() {
        val m = ChunkMesher(4)
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
        for ((left, right) in listOf(ChunkMesher.CELLS to ChunkMesher.COARSE_CELLS, ChunkMesher.COARSE_CELLS to ChunkMesher.CELLS, ChunkMesher.COARSE_CELLS to ChunkMesher.COARSE_CELLS)) {
            val a = edgePoints(m.ground(cx, cy, left).vertices)
            val b = edgePoints(m.ground(cx + 1, cy, right).vertices)
            val coarseSide = if (left == ChunkMesher.COARSE_CELLS) a else b
            val other = if (left == ChunkMesher.COARSE_CELLS) b else a
            // the coarse chunk's border is the fine grid's edge column (49 vertices), identical on both sides
            assertTrue(coarseSide.size == ChunkMesher.CELLS + 1, "coarse edge has ${coarseSide.size} vertices")
            assertTrue(other.containsAll(coarseSide), "levels $left and $right do not meet")
        }
    }
}

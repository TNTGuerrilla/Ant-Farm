package com.bydesigninteractive.ant.core.render.world

import com.bydesigninteractive.ant.core.engine.FoodView
import com.bydesigninteractive.ant.sim.world.FoodKind
import com.bydesigninteractive.ant.sim.world.sdf.Blob
import kotlin.math.abs
import kotlin.math.sqrt
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RockFoodMeshTest {
    private val pebble = Blob(4000f, 4000f, 2f, 4f, 3.5f, 2.5f, 0.12f, 1.3f)
    private val rock = Blob(4100f, 4000f, 8f, 30f, 24f, 18f, 0.15f, 4.1f)

    /** The rock mesh of [blob] read through its index buffer: three vertices per triangle, as a non-indexed mesh would hold them. */
    private fun ChunkMesher.rockTriangles(blob: Blob): FloatArray {
        val d = rocks(listOf(blob)).single()
        val idx = d.indices!!
        val s = MeshData.STRIDE
        val out = FloatArray(idx.size * s)
        for (t in idx.indices) d.vertices.copyInto(out, t * s, (idx[t].toInt() and 0xFFFF) * s, (idx[t].toInt() and 0xFFFF) * s + s)
        return out
    }

    @Test
    fun rockVerticesLieOnTheirRock() {
        val m = ChunkMesher(4)
        var worst = 0f
        for (b in listOf(pebble, rock)) {
            val v = m.rocks(listOf(b)).single().vertices
            var i = 0
            while (i < v.size) {
                val d = b.distance(v[i], v[i + 1], v[i + 2])
                worst = maxOf(worst, abs(d))
                assertTrue(abs(d) <= 0.2f, "vertex ${v[i]}, ${v[i + 1]}, ${v[i + 2]} is $d from its rock")
                i += MeshData.STRIDE
            }
        }
        println("worst rock vertex distance: $worst")
    }

    @Test
    fun rockTrianglesFaceOutward() {
        val m = ChunkMesher(4)
        for (b in listOf(pebble, rock)) {
            val v = m.rockTriangles(b)
            val s = MeshData.STRIDE
            var i = 0
            while (i < v.size) {
                val ux = v[i + s] - v[i]; val uy = v[i + s + 1] - v[i + 1]; val uz = v[i + s + 2] - v[i + 2]
                val wx = v[i + 2 * s] - v[i]; val wy = v[i + 2 * s + 1] - v[i + 1]; val wz = v[i + 2 * s + 2] - v[i + 2]
                val nx = uy * wz - uz * wy
                val ny = uz * wx - ux * wz
                val nz = ux * wy - uy * wx
                val mx = (v[i] + v[i + s] + v[i + 2 * s]) / 3f - b.cx
                val my = (v[i + 1] + v[i + s + 1] + v[i + 2 * s + 1]) / 3f - b.cy
                val mz = (v[i + 2] + v[i + s + 2] + v[i + 2 * s + 2]) / 3f - b.cz
                assertTrue(nx * mx + ny * my + nz * mz > 0f, "triangle at vertex ${i / s} of rock ${b.reach} faces inward")
                i += 3 * s
            }
        }
    }

    @Test
    fun rocksAreSmoothAndSizedByReach() {
        val m = ChunkMesher(4)
        val small = m.rocks(listOf(pebble)).single()
        val big = m.rocks(listOf(rock)).single()
        // indexed: one vertex per icosphere vertex, three indices per triangle
        assertEquals(42, small.vertexCount)
        assertEquals(80 * 3, small.indexCount)
        assertEquals(642, big.vertexCount)
        assertEquals(1280 * 3, big.indexCount)
        // smooth: a vertex's normal points away from the rock centre and is unit length
        val v = big.vertices
        val nx = v[3]; val ny = v[4]; val nz = v[5]
        assertEquals(1f, sqrt(nx * nx + ny * ny + nz * nz), 1e-3f)
        assertTrue((v[0] - rock.cx) * nx + (v[1] - rock.cy) * ny + (v[2] - rock.cz) * nz > 0f)
    }

    @Test
    fun rockFacetsStayCloseToTheTrueSurface() {
        val big = Blob(4300f, 4000f, 40f, 90f, 75f, 50f, 0.15f, 7.7f)
        val m = ChunkMesher(4)
        for (b in listOf(rock, big)) {
            val v = m.rockTriangles(b)
            val s = MeshData.STRIDE
            val ds = ArrayList<Float>()
            val lattice = 6
            var i = 0
            while (i < v.size) {
                for (a in 0..lattice) for (c in 0..lattice - a) {
                    val u = a.toFloat() / lattice
                    val w = c.toFloat() / lattice
                    val t = 1f - u - w
                    fun at(o: Int) = u * v[i + s + o] + w * v[i + 2 * s + o] + t * v[i + o]
                    ds += b.distance(at(0), at(1), at(2))
                }
                i += 3 * s
            }
            ds.sort()
            val p1 = ds[(ds.size * 0.01f).toInt()]
            val p99 = ds[(ds.size * 0.99f).toInt().coerceAtMost(ds.size - 1)]
            println("rock reach ${b.reach}: p1 $p1 p99 $p99 min ${ds.first()} max ${ds.last()}")
            assertTrue(p1 >= -0.5f && p99 <= 0.3f, "p1 $p1 p99 $p99 outside [-0.5, 0.3] for reach ${b.reach}")
            assertTrue(ds.first() >= -1.0f && ds.last() <= 0.5f, "min ${ds.first()} max ${ds.last()} outside [-1.0, 0.5] for reach ${b.reach}")
        }
    }

    @Test
    fun manyLargeRocksSplitIntoPartsWithSixteenBitIndices() {
        val m = ChunkMesher(4)
        // 30 large rocks of 2,562 vertices each: 76,860 vertices, more than one 16-bit part holds
        val blobs = List(30) { Blob(1000f + it * 200f, 4000f, 40f, 90f, 75f, 50f, 0.15f, 7.7f) }
        val parts = m.rocks(blobs)
        assertTrue(parts.size >= 2, "expected a split, got ${parts.size} part(s)")
        assertEquals(30 * 2562, parts.sumOf { it.vertexCount })
        assertEquals(30 * 5120 * 3, parts.sumOf { it.indexCount })
        for (p in parts) {
            assertTrue(p.vertexCount <= MeshData.MAX_INDEXED_VERTICES)
            for (i in p.indices!!) assertTrue((i.toInt() and 0xFFFF) < p.vertexCount)
        }
        assertTrue(m.rocks(emptyList()).isEmpty())
    }

    @Test
    fun foodsGetStemsClustersAndLumps() {
        val m = ChunkMesher(4)
        val plant = FoodView(4000f, 4000f, 500f, 12f, FoodKind.HONEYDEW, 8f, 2.5f, -5f, 530f)
        val prey = FoodView(4200f, 4000f, 3f, 9f, FoodKind.PREY, 5f, 0f, 0f, 0f)
        val both = m.foods(listOf(plant, prey))
        val onlyPrey = m.foods(listOf(prey))
        assertTrue(onlyPrey.vertexCount >= 80 * 3)
        assertTrue(both.vertexCount >= onlyPrey.vertexCount + 12 * 3 + 6 * 8 * 3, "stem prism and aphids missing")
        // the stem reaches from its base to its top
        var minZ = Float.MAX_VALUE
        var maxZ = -Float.MAX_VALUE
        val v = m.foods(listOf(plant)).vertices
        var i = 0
        while (i < v.size) {
            minZ = minOf(minZ, v[i + 2]); maxZ = maxOf(maxZ, v[i + 2]); i += MeshData.STRIDE
        }
        assertTrue(minZ <= -4.9f && maxZ >= 529f, "stem spans $minZ to $maxZ")
    }
}

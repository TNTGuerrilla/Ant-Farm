package com.bydesigninteractive.ant.core.render.world

import com.bydesigninteractive.ant.core.engine.FoodView
import com.bydesigninteractive.ant.sim.util.hash
import com.bydesigninteractive.ant.sim.world.CHUNK_MM
import com.bydesigninteractive.ant.sim.world.ChunkedField
import com.bydesigninteractive.ant.sim.world.FoodKind
import com.bydesigninteractive.ant.sim.world.SURFACE_MM
import com.bydesigninteractive.ant.sim.world.sdf.Blob
import com.bydesigninteractive.ant.sim.world.sdf.HeightField
import java.util.Random
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Builds the low-poly meshes of one surface chunk from the seed and published data: the ground as
 * jittered flat facets on the height field (spoil included), and baked faceted specks; rocks as
 * smooth icospheres fitted to each rock's distance function; and flat-shaded food. It owns its own [HeightField] and spoil mirror, so it runs on the
 * mesher thread without touching the live world. Not thread-safe.
 */
class ChunkMesher(private val seed: Long) {
    private val spoil = ChunkedField()
    val heights = HeightField(seed, spoil)

    /** Mirrors a published spoil chunk ([values] is immutable and kept without copying). */
    fun setSpoil(cx: Int, cy: Int, values: FloatArray?) = spoil.setChunk(cx, cy, values)

    /** Pellets per spoil cell at (x, y), from the mirror. */
    fun spoilAt(x: Float, y: Float): Float = spoil.get(x, y)

    /**
     * The ground and specks of chunk (cx, cy). A chunk's edge vertices read the spoil mirror at the
     * edge, so mirror the neighbours' spoil ([setSpoil]) before meshing for watertight seams under spoil.
     * Each cell's diagonal is the split with the smaller deviation from the height field.
     */
    fun ground(cx: Int, cy: Int): MeshData {
        val b = MeshBuilder(CELLS * CELLS * 6 + SPECK_VERTEX_BUDGET)
        val n = CELLS + 1
        val xs = FloatArray(n * n)
        val ys = FloatArray(n * n)
        val zs = FloatArray(n * n)
        for (j in 0 until n) for (i in 0 until n) {
            val gi = cx * CELLS + i
            val gj = cy * CELLS + j
            val k = i + j * n
            xs[k] = jittered(gi, gj, 0)
            ys[k] = jittered(gi, gj, 1)
            zs[k] = heights.height(xs[k], ys[k])
        }
        // true: split along a-e (a, c, e + a, e, d); false: split along c-d (a, c, d + c, e, d)
        val diag = BooleanArray(CELLS * CELLS)
        for (j in 0 until CELLS) for (i in 0 until CELLS) {
            val a = i + j * n
            val c = a + 1
            val d = a + n
            val e = d + 1
            val ae = max(triDeviation(xs, ys, zs, a, c, e), triDeviation(xs, ys, zs, a, e, d))
            val cd = max(triDeviation(xs, ys, zs, a, c, d), triDeviation(xs, ys, zs, c, e, d))
            diag[i + j * CELLS] = ae <= cd
            val h = hash(seed xor FACET_SALT, cx * CELLS + i, cy * CELLS + j)
            if (diag[i + j * CELLS]) {
                soilTri(b, xs, ys, zs, a, c, e, h ushr 8)
                soilTri(b, xs, ys, zs, a, e, d, h ushr 20)
            } else {
                soilTri(b, xs, ys, zs, a, c, d, h ushr 8)
                soilTri(b, xs, ys, zs, c, e, d, h ushr 20)
            }
        }
        specks(b, cx, cy, Facets(xs, ys, zs, diag, cx, cy))
        return b.build()
    }

    private val ico1 = Icosphere.build(1)
    private val ico2 = Icosphere.build(2)
    private val ico3 by lazy { Icosphere.build(3) }
    private val ico4 by lazy { Icosphere.build(4) }

    /** Smooth-shaded rocks: each vertex of an icosphere moved (from the rock ellipsoid) onto the rock's own surface, normals from its gradient. */
    fun rocks(blobs: List<Blob>): MeshData {
        val b = MeshBuilder(blobs.size * 1280 * 3 + 16)
        val p = FloatArray(3)
        val n = FloatArray(3)
        for (blob in blobs) {
            val level = rockSubdivision(blob.reach)
            val (verts, tris) = when (level) { 1 -> ico1; 2 -> ico2; 3 -> ico3; else -> ico4 }
            // Flat facets sag inside a convex rock; half the chord sag outward makes them straddle the surface.
            val edgeAngle = ICOSAHEDRON_EDGE_ANGLE / (1 shl level)
            val inflate = 0.5f * blob.reach * (1f - cos(edgeAngle / 2f))
            val pos = FloatArray(verts.size)
            val nor = FloatArray(verts.size)
            for (k in 0 until verts.size / 3) {
                p[0] = blob.cx + verts[k * 3] * blob.rx
                p[1] = blob.cy + verts[k * 3 + 1] * blob.ry
                p[2] = blob.cz + verts[k * 3 + 2] * blob.rz
                projectOnto(blob, p, n)
                p[0] += n[0] * inflate; p[1] += n[1] * inflate; p[2] += n[2] * inflate
                p.copyInto(pos, k * 3)
                n.copyInto(nor, k * 3)
            }
            val f = rockShade(blob)
            for (t in tris.indices step 3) for (c in 0 until 3) {
                val k = tris[t + c] * 3
                b.vertex(pos[k], pos[k + 1], pos[k + 2], nor[k], nor[k + 1], nor[k + 2], STONE_R * f, STONE_G * f, STONE_B * f)
            }
        }
        return b.build()
    }

    /** Icosphere subdivision for a rock of [reach] mm: finer for bigger rocks so the facet sag stays small. */
    fun rockSubdivision(reach: Float): Int = when {
        reach < PEBBLE_REACH -> 1
        reach < 30f -> 2
        reach < LARGE_ROCK_REACH -> 3
        else -> 4
    }

    /** The rock's colour factor on the stone grey, 0.9 to 1.1, deterministic per seed and rock position. */
    fun rockShade(blob: Blob): Float {
        val h = hash(seed xor ROCK_SALT, blob.cx.toInt(), blob.cy.toInt())
        return 0.9f + ((h and 0xFF).toFloat() / 255f) * 0.2f
    }

    /** Newton steps along the gradient of [blob]'s distance until [p] is on its surface; the unit gradient goes into [n]. */
    private fun projectOnto(blob: Blob, p: FloatArray, n: FloatArray) {
        for (step in 0 until PROJECT_STEPS) {
            val d = blob.distance(p[0], p[1], p[2])
            gradient(blob, p, n)
            p[0] -= n[0] * d
            p[1] -= n[1] * d
            p[2] -= n[2] * d
            if (abs(d) < 0.005f) break
        }
        gradient(blob, p, n)
    }

    private fun gradient(blob: Blob, p: FloatArray, n: FloatArray) {
        val e = 0.05f
        val gx = blob.distance(p[0] + e, p[1], p[2]) - blob.distance(p[0] - e, p[1], p[2])
        val gy = blob.distance(p[0], p[1] + e, p[2]) - blob.distance(p[0], p[1] - e, p[2])
        val gz = blob.distance(p[0], p[1], p[2] + e) - blob.distance(p[0], p[1], p[2] - e)
        val l = sqrt(gx * gx + gy * gy + gz * gz).coerceAtLeast(1e-9f)
        n[0] = gx / l; n[1] = gy / l; n[2] = gz / l
    }

    /** Flat-shaded food: six-sided stems with aphid clusters for plants, faceted lumps for prey and feeders. */
    fun foods(foods: List<FoodView>): MeshData {
        val b = MeshBuilder(1024)
        for (f in foods) {
            if (f.stemRadius > 0f) stem(b, f)
            if (f.bodyRadius > 0f) {
                when {
                    f.stemRadius > 0f -> aphidCluster(b, f)
                    f.kind == FoodKind.PREY -> lump(b, f.x, f.y, f.z, f.bodyRadius, PREY_R, PREY_G, PREY_B, 0)
                    else -> lump(b, f.x, f.y, f.z, f.bodyRadius, AMBER_R, AMBER_G, AMBER_B, 0)
                }
            }
        }
        return b.build()
    }

    private fun stem(b: MeshBuilder, f: FoodView) {
        val r = f.stemRadius
        for (k in 0 until 6) {
            val a0 = k * 6.2832f / 6f
            val a1 = (k + 1) * 6.2832f / 6f
            val x0 = f.x + cos(a0) * r; val y0 = f.y + sin(a0) * r
            val x1 = f.x + cos(a1) * r; val y1 = f.y + sin(a1) * r
            val g = 0.85f + 0.15f * (k % 2)
            b.flat(x0, y0, f.stemBase, x1, y1, f.stemBase, x1, y1, f.stemTop, STEM_R * g, STEM_G * g, STEM_B * g)
            b.flat(x0, y0, f.stemBase, x1, y1, f.stemTop, x0, y0, f.stemTop, STEM_R * g, STEM_G * g, STEM_B * g)
        }
    }

    private fun aphidCluster(b: MeshBuilder, f: FoodView) {
        lump(b, f.x, f.y, f.z, f.bodyRadius, STEM_R * 1.2f, STEM_G * 1.2f, STEM_B, 0)
        val r = Random(hash(seed xor APHID_SALT, f.x.toInt(), f.y.toInt()))
        repeat(APHIDS) {
            val u = r.nextFloat() * 2f - 1f
            val a = r.nextFloat() * 6.2832f
            val s = sqrt(1f - u * u)
            val px = f.x + cos(a) * s * f.bodyRadius
            val py = f.y + sin(a) * s * f.bodyRadius
            val pz = f.z + u * f.bodyRadius
            val dark = r.nextBoolean()
            octahedron(b, px, py, pz, 1f + r.nextFloat() * 0.5f, if (dark) 0.12f else 0.45f, if (dark) 0.14f else 0.62f, if (dark) 0.10f else 0.25f)
        }
    }

    private fun lump(b: MeshBuilder, x: Float, y: Float, z: Float, radius: Float, cr: Float, cg: Float, cb: Float, salt: Int) {
        val (verts, tris) = ico1
        val h = hash(seed xor LUMP_SALT, x.toInt() + salt, y.toInt())
        for (t in tris.indices step 3) {
            val i0 = tris[t] * 3; val i1 = tris[t + 1] * 3; val i2 = tris[t + 2] * 3
            val f = 0.85f + (((h ushr (t % 40)) and 0xFF).toFloat() / 255f) * 0.3f
            b.flat(
                x + verts[i0] * radius, y + verts[i0 + 1] * radius, z + verts[i0 + 2] * radius,
                x + verts[i1] * radius, y + verts[i1 + 1] * radius, z + verts[i1 + 2] * radius,
                x + verts[i2] * radius, y + verts[i2 + 1] * radius, z + verts[i2 + 2] * radius,
                cr * f, cg * f, cb * f,
            )
        }
    }

    private fun octahedron(b: MeshBuilder, x: Float, y: Float, z: Float, r: Float, cr: Float, cg: Float, cb: Float) {
        val px = floatArrayOf(r, 0f, -r, 0f)
        val py = floatArrayOf(0f, r, 0f, -r)
        for (k in 0 until 4) {
            val k2 = (k + 1) % 4
            b.flat(x + px[k], y + py[k], z, x + px[k2], y + py[k2], z, x, y, z + r * 0.7f, cr, cg, cb)
            b.flat(x + px[k2], y + py[k2], z, x + px[k], y + py[k], z, x, y, z - r * 0.7f, cr * 0.7f, cg * 0.7f, cb * 0.7f)
        }
    }

    /** Worst |facet - true ground| over a barycentric lattice of the triangle (p, q, r). */
    private fun triDeviation(xs: FloatArray, ys: FloatArray, zs: FloatArray, p: Int, q: Int, r: Int): Float {
        var worst = 0f
        for (u in 0..DEV_LATTICE) for (w in 0..DEV_LATTICE - u) {
            val fu = u.toFloat() / DEV_LATTICE
            val fw = w.toFloat() / DEV_LATTICE
            val f0 = 1f - fu - fw
            val x = xs[p] * f0 + xs[q] * fu + xs[r] * fw
            val y = ys[p] * f0 + ys[q] * fu + ys[r] * fw
            val z = zs[p] * f0 + zs[q] * fu + zs[r] * fw
            worst = max(worst, abs(z - heights.height(x, y)))
        }
        return worst
    }

    /** The drawn ground facets of one chunk, to find the facet plane under a point. */
    private class Facets(val xs: FloatArray, val ys: FloatArray, val zs: FloatArray, val diag: BooleanArray, val cx: Int, val cy: Int) {
        private val n = CELLS + 1

        private var bestZ = 0f
        private var bestScore = 0f

        /** z of the drawn facet under (x, y); outside the mesh the nearest facet plane is extended. */
        fun z(x: Float, y: Float): Float {
            val ci = floor(x / CELL_MM).toInt() - cx * CELLS
            val cj = floor(y / CELL_MM).toInt() - cy * CELLS
            bestScore = -Float.MAX_VALUE
            for (j in max(0, cj - 1)..min(CELLS - 1, cj + 1)) for (i in max(0, ci - 1)..min(CELLS - 1, ci + 1)) {
                val a = i + j * n
                val c = a + 1
                val d = a + n
                val e = d + 1
                if (diag[i + j * CELLS]) {
                    tri(x, y, a, c, e)
                    tri(x, y, a, e, d)
                } else {
                    tri(x, y, a, c, d)
                    tri(x, y, c, e, d)
                }
            }
            return bestZ
        }

        /** Keeps the plane of (p, q, r) if (x, y) is more inside it (largest minimum barycentric weight) than any so far. */
        private fun tri(x: Float, y: Float, p: Int, q: Int, r: Int) {
            val det = (xs[q] - xs[p]) * (ys[r] - ys[p]) - (ys[q] - ys[p]) * (xs[r] - xs[p])
            if (abs(det) < 1e-9f) return
            val l1 = ((x - xs[p]) * (ys[r] - ys[p]) - (y - ys[p]) * (xs[r] - xs[p])) / det
            val l2 = ((xs[q] - xs[p]) * (y - ys[p]) - (ys[q] - ys[p]) * (x - xs[p])) / det
            val l0 = 1f - l1 - l2
            val score = min(l0, min(l1, l2))
            if (score > bestScore) {
                bestScore = score
                bestZ = zs[p] * l0 + zs[q] * l1 + zs[r] * l2
            }
        }
    }

    /** Coordinate [axis] (0 x, 1 y) of global grid vertex (gi, gj), jittered by a quarter cell, clamped to the map. */
    private fun jittered(gi: Int, gj: Int, axis: Int): Float {
        val base = (if (axis == 0) gi else gj) * CELL_MM
        val h = hash(seed xor JITTER_SALT, gi * 2 + axis, gj)
        val j = (((h ushr 11) and 0xFFFF).toFloat() / 0xFFFF - 0.5f) * 0.5f * CELL_MM
        return (base + j).coerceIn(0f, SURFACE_MM.toFloat())
    }

    private fun soilTri(b: MeshBuilder, xs: FloatArray, ys: FloatArray, zs: FloatArray, p: Int, q: Int, r: Int, h: Long) {
        val f = 0.88f + ((h and 0xFF).toFloat() / 255f) * 0.24f
        b.flat(xs[p], ys[p], zs[p], xs[q], ys[q], zs[q], xs[r], ys[r], zs[r], SOIL_R * f, SOIL_G * f, SOIL_B * f)
    }

    /** A few loose clusters and some strays of small flat flecks, 0.05 mm above the ground. */
    private fun specks(b: MeshBuilder, cx: Int, cy: Int, facets: Facets) {
        val r = Random(hash(seed xor SPECK_SALT, cx, cy))
        val x0 = cx * CHUNK_MM.toFloat()
        val y0 = cy * CHUNK_MM.toFloat()
        repeat(CLUSTERS) {
            val px = x0 + r.nextFloat() * CHUNK_MM
            val py = y0 + r.nextFloat() * CHUNK_MM
            repeat(2 + r.nextInt(5)) { fleck(b, r, px + r.nextGaussian().toFloat() * 25f, py + r.nextGaussian().toFloat() * 10f, x0, y0, facets) }
        }
        repeat(STRAYS) { fleck(b, r, x0 + r.nextFloat() * CHUNK_MM, y0 + r.nextFloat() * CHUNK_MM, x0, y0, facets) }
    }

    private fun fleck(b: MeshBuilder, r: Random, x: Float, y: Float, x0: Float, y0: Float, facets: Facets) {
        val size = exp(0.5f + r.nextGaussian().toFloat() * 0.5f).coerceIn(0.6f, 5f)
        val sides = intArrayOf(3, 3, 4, 5)[r.nextInt(4)]
        val shade = SPECK_SHADES[r.nextInt(SPECK_SHADES.size)]
        val f = 0.88f + r.nextFloat() * 0.24f
        val a0 = r.nextFloat() * 6.2832f
        val px = FloatArray(sides)
        val py = FloatArray(sides)
        for (k in 0 until sides) {
            val t = a0 + k * 6.2832f / sides + (r.nextFloat() - 0.5f) * 0.8f
            val rr = size * (0.5f + r.nextFloat() * 0.8f)
            px[k] = (x + cos(t) * rr).coerceIn(x0, x0 + CHUNK_MM)
            py[k] = (y + sin(t) * rr).coerceIn(y0, y0 + CHUNK_MM)
        }
        val ccx = x.coerceIn(x0, x0 + CHUNK_MM)
        val ccy = y.coerceIn(y0, y0 + CHUNK_MM)
        val cz = facets.z(ccx, ccy) + SPECK_LIFT
        for (k in 0 until sides) {
            val k2 = (k + 1) % sides
            b.flat(
                ccx, ccy, cz,
                px[k], py[k], facets.z(px[k], py[k]) + SPECK_LIFT,
                px[k2], py[k2], facets.z(px[k2], py[k2]) + SPECK_LIFT,
                shade[0] * f, shade[1] * f, shade[2] * f,
            )
        }
    }

    companion object {
        const val CELLS = 48
        const val CELL_MM = CHUNK_MM.toFloat() / CELLS
        const val SPECK_LIFT = 0.05f
        private const val DEV_LATTICE = 6
        /** Upper bound on speck vertices per chunk: (clusters * 6 + strays) flecks, 5 triangles of 3 vertices at most. */
        private const val SPECK_VERTEX_BUDGET = (4 * 6 + 8) * 15
        const val CLUSTERS = 4
        const val STRAYS = 8
        const val SOIL_R = 139f / 255f
        const val SOIL_G = 98f / 255f
        const val SOIL_B = 62f / 255f
        private const val FACET_SALT = 0x5011L
        private const val JITTER_SALT = 0x717L
        private const val SPECK_SALT = 0x5BECL
        const val PEBBLE_REACH = 6f
        /** Reach from which rocks get the finest icosphere (5,120 triangles). */
        const val LARGE_ROCK_REACH = 80f
        private const val ICOSAHEDRON_EDGE_ANGLE = 1.1071487f // radians, about 63.43 degrees
        const val PROJECT_STEPS = 12
        const val APHIDS = 8
        const val STONE_R = 128f / 255f
        const val STONE_G = 124f / 255f
        const val STONE_B = 116f / 255f
        const val STEM_R = 79f / 255f
        const val STEM_G = 122f / 255f
        const val STEM_B = 52f / 255f
        const val PREY_R = 200f / 255f
        const val PREY_G = 182f / 255f
        const val PREY_B = 140f / 255f
        const val AMBER_R = 214f / 255f
        const val AMBER_G = 150f / 255f
        const val AMBER_B = 60f / 255f
        private const val ROCK_SALT = 0x20CL
        private const val APHID_SALT = 0xA41DL
        private const val LUMP_SALT = 0x1E3FL
        private val SPECK_SHADES = arrayOf(
            floatArrayOf(96f / 255f, 64f / 255f, 40f / 255f),
            floatArrayOf(116f / 255f, 80f / 255f, 50f / 255f),
            floatArrayOf(150f / 255f, 112f / 255f, 76f / 255f),
            floatArrayOf(120f / 255f, 116f / 255f, 108f / 255f),
        )
    }
}

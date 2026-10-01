package com.bydesigninteractive.ant.core.render.ant

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AntMeshTest {
    private fun parts(v: FloatArray) = (0 until v.size / AntMesh.STRIDE).map { v[it * AntMesh.STRIDE + 9].toInt() }

    @Test
    fun theDetailedAntIsAboutTwoHundredTriangles() {
        val tris = AntMesh.build(true).size / AntMesh.STRIDE / 3
        assertTrue(tris in 150..320, "detailed ant has $tris triangles")
        val far = AntMesh.build(false).size / AntMesh.STRIDE / 3
        assertTrue(far * 2 <= tris, "far ant ($far) should be at most half the detailed one ($tris)")
    }

    @Test
    fun everyPartIsPresentAndLegsHaveHips() {
        val v = AntMesh.build(true)
        val p = parts(v)
        for (part in 0..9) assertTrue(part in p, "part $part missing")
        for (i in p.indices) if (p[i] in 1..6) {
            val o = i * AntMesh.STRIDE
            // a leg's pivot is its hip on the thorax, above the ground and near the body axis
            assertTrue(v[o + 12] > 0.3f && kotlin.math.abs(v[o + 11]) < 0.6f, "leg pivot ${v[o + 10]}, ${v[o + 11]}, ${v[o + 12]}")
        }
    }

    @Test
    fun theAntStandsOnItsFeetFacingForward() {
        val v = AntMesh.build(true)
        var minZ = Float.MAX_VALUE
        var maxX = -Float.MAX_VALUE
        var minX = Float.MAX_VALUE
        var i = 0
        while (i < v.size) {
            minZ = minOf(minZ, v[i + 2]); maxX = maxOf(maxX, v[i]); minX = minOf(minX, v[i]); i += AntMesh.STRIDE
        }
        assertEquals(0f, minZ, 0.15f)
        assertTrue(maxX > 2f && minX < -2.5f, "head forward, gaster behind: $minX to $maxX")
    }

    @Test
    fun theTripodsSplitTheLegs() {
        val b = (0 until 6).filter { it !in AntMesh.TRIPOD_A }
        assertEquals(listOf(1, 3, 5), b)
    }

    /** Vertex [j] (0 to 2) of triangle [t] as x, y, z. */
    private fun vert(v: FloatArray, t: Int, j: Int): FloatArray {
        val o = (t * 3 + j) * AntMesh.STRIDE
        return floatArrayOf(v[o], v[o + 1], v[o + 2])
    }

    private fun faceNormal(v: FloatArray, t: Int): FloatArray {
        val a = vert(v, t, 0); val b = vert(v, t, 1); val c = vert(v, t, 2)
        val ux = b[0] - a[0]; val uy = b[1] - a[1]; val uz = b[2] - a[2]
        val wx = c[0] - a[0]; val wy = c[1] - a[1]; val wz = c[2] - a[2]
        return floatArrayOf(uy * wz - uz * wy, uz * wx - ux * wz, ux * wy - uy * wx)
    }

    private fun centroid(v: FloatArray, t: Int): FloatArray {
        val vs = (0 until 3).map { vert(v, t, it) }
        return FloatArray(3) { k -> (vs[0][k] + vs[1][k] + vs[2][k]) / 3f }
    }

    private fun dot(a: FloatArray, b: FloatArray) = a[0] * b[0] + a[1] * b[1] + a[2] * b[2]

    @Test
    fun everyLumpFacesOutwardFromItsCentre() {
        for (detailed in listOf(true, false)) {
            val v = AntMesh.build(detailed)
            val p = parts(v)
            // the gaster and the carried pieces are centred on their pivot
            val triParts = (0 until v.size / AntMesh.STRIDE / 3).map { p[it * 3] }
            for (t in triParts.indices) {
                val part = triParts[t]
                if (part != AntMesh.PART_GASTER && part != AntMesh.PART_PELLET && part != AntMesh.PART_PREY) continue
                val o = t * 3 * AntMesh.STRIDE
                val centre = floatArrayOf(v[o + 10], v[o + 11], v[o + 12])
                val c = centroid(v, t)
                val out = floatArrayOf(c[0] - centre[0], c[1] - centre[1], c[2] - centre[2])
                assertTrue(dot(faceNormal(v, t), out) > 0f, "part $part triangle $t faces inward (detailed=$detailed)")
            }
            // the body lumps come first among the part 0 triangles, in the order waist, thorax, neck, head
            val centres = arrayOf(
                floatArrayOf(-0.45f, 0f, 0.8f), floatArrayOf(0.5f, 0f, 0.85f),
                floatArrayOf(1.3f, 0f, 0.9f), floatArrayOf(1.95f, 0f, 0.9f),
            )
            val counts = if (detailed) intArrayOf(8, 24, 8, 24) else intArrayOf(6, 8, 6, 8)
            val body = triParts.indices.filter { triParts[it] == AntMesh.PART_BODY }
            var at = 0
            for (k in centres.indices) {
                repeat(counts[k]) {
                    val t = body[at++]
                    val c = centroid(v, t)
                    val out = floatArrayOf(c[0] - centres[k][0], c[1] - centres[k][1], c[2] - centres[k][2])
                    assertTrue(dot(faceNormal(v, t), out) > 0f, "body lump $k triangle $t faces inward (detailed=$detailed)")
                }
            }
        }
    }

    @Test
    fun everyLimbFacesOutwardFromItsAxis() {
        val v = AntMesh.build(true)
        val triParts = (0 until v.size / AntMesh.STRIDE / 3).map { v[it * 3 * AntMesh.STRIDE + 9].toInt() }
        // a limb is six triangles: (a0 a1 b1) and (a0 b1 b0) for each of three sides, so the ring at
        // end a is the first vertex of triangles 0, 2, 4 and the ring at end b is the third vertex of 1, 3, 5
        fun checkLimb(first: Int, label: String) {
            val a = FloatArray(3); val b = FloatArray(3)
            for (k in 0 until 3) for (d in 0 until 3) {
                a[d] += vert(v, first + 2 * k, 0)[d] / 3f
                b[d] += vert(v, first + 2 * k + 1, 2)[d] / 3f
            }
            val axis = floatArrayOf(b[0] - a[0], b[1] - a[1], b[2] - a[2])
            val len2 = dot(axis, axis)
            for (t in first until first + 6) {
                val c = centroid(v, t)
                val rel = floatArrayOf(c[0] - a[0], c[1] - a[1], c[2] - a[2])
                val along = dot(rel, axis) / len2
                val radial = floatArrayOf(rel[0] - axis[0] * along, rel[1] - axis[1] * along, rel[2] - axis[2] * along)
                assertTrue(dot(faceNormal(v, t), radial) > 0f, "$label triangle $t faces the axis")
            }
        }
        for (leg in 1..6) {
            val ts = triParts.indices.filter { triParts[it] == leg }
            assertEquals(12, ts.size, "leg $leg has two six-triangle segments")
            checkLimb(ts[0], "leg $leg upper")
            checkLimb(ts[6], "leg $leg lower")
        }
        // the antennae are the limbs of the body part that follow its four lumps (64 triangles), 12 each, then the mandibles
        val body = triParts.indices.filter { triParts[it] == AntMesh.PART_BODY }
        for (k in 0 until 2) {
            checkLimb(body[64 + k * 14], "antenna $k scape")
            checkLimb(body[64 + k * 14 + 6], "antenna $k funiculus")
        }
    }
}

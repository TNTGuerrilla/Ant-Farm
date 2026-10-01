package com.bydesigninteractive.ant.core.render.world

import kotlin.math.hypot
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class EntranceMeshTest {
    private val slope = { x: Float, y: Float -> 10f + 0.1f * x - 0.05f * y }

    @Test
    fun theConeDipsThreeMillimetresWithinTheRim() {
        val m = EntranceMesh.cone(100f, 200f, slope)
        val v = m.vertices
        var minZ = Float.MAX_VALUE
        var maxR = 0f
        for (k in 0 until m.vertexCount) {
            val o = k * MeshData.STRIDE
            minZ = minOf(minZ, v[o + 2])
            val r = hypot(v[o] - 100f, v[o + 1] - 200f)
            maxR = maxOf(maxR, r)
            if (r > EntranceMesh.RADIUS - 1e-3f) {
                assertEquals(slope(v[o], v[o + 1]) + EntranceMesh.LIFT, v[o + 2], 1e-3f) // the rim sits on the ground
            }
        }
        assertEquals(EntranceMesh.RADIUS, maxR, 1e-3f)
        assertEquals(slope(100f, 200f) + EntranceMesh.LIFT - EntranceMesh.DEPTH, minZ, 1e-3f)
    }

    @Test
    fun everyFaceIsWoundCounterClockwiseFromAbove() {
        for (m in listOf(EntranceMesh.cone(0f, 0f, slope), EntranceMesh.lid(0f, 0f, slope))) {
            val v = m.vertices
            for (t in 0 until m.vertexCount / 3) {
                val o = t * 3 * MeshData.STRIDE
                val ax = v[o]; val ay = v[o + 1]
                val bx = v[o + MeshData.STRIDE]; val by = v[o + MeshData.STRIDE + 1]
                val cx = v[o + 2 * MeshData.STRIDE]; val cy = v[o + 2 * MeshData.STRIDE + 1]
                assertTrue((bx - ax) * (cy - ay) - (by - ay) * (cx - ax) > 0f, "triangle $t")
                assertTrue(v[o + 5] > 0f, "normal of triangle $t points up")
            }
        }
    }

    @Test
    fun theInsideIsNearlyBlack() {
        val m = EntranceMesh.cone(0f, 0f) { _, _ -> 0f }
        val v = m.vertices
        for (k in 0 until m.vertexCount) {
            val o = k * MeshData.STRIDE
            if (hypot(v[o], v[o + 1]) < 1f) assertTrue(v[o + 6] + v[o + 7] + v[o + 8] < 0.1f)
        }
    }
}

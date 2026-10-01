package com.bydesigninteractive.ant.core.render.world

import kotlin.math.sqrt

/** Unit icospheres: 20 triangles subdivided [subdivisions] times (80 at 1, 320 at 2). */
object Icosphere {
    /** Vertex xyz triples on the unit sphere and triangle index triples, counter-clockwise seen from outside. */
    fun build(subdivisions: Int): Pair<FloatArray, IntArray> {
        val t = ((1.0 + sqrt(5.0)) / 2.0).toFloat()
        val verts = ArrayList<FloatArray>()
        fun add(x: Float, y: Float, z: Float): Int {
            val l = sqrt(x * x + y * y + z * z)
            verts += floatArrayOf(x / l, y / l, z / l)
            return verts.size - 1
        }
        for ((x, y, z) in listOf(
            Triple(-1f, t, 0f), Triple(1f, t, 0f), Triple(-1f, -t, 0f), Triple(1f, -t, 0f),
            Triple(0f, -1f, t), Triple(0f, 1f, t), Triple(0f, -1f, -t), Triple(0f, 1f, -t),
            Triple(t, 0f, -1f), Triple(t, 0f, 1f), Triple(-t, 0f, -1f), Triple(-t, 0f, 1f),
        )) add(x, y, z)
        var tris = intArrayOf(
            0, 11, 5, 0, 5, 1, 0, 1, 7, 0, 7, 10, 0, 10, 11,
            1, 5, 9, 5, 11, 4, 11, 10, 2, 10, 7, 6, 7, 1, 8,
            3, 9, 4, 3, 4, 2, 3, 2, 6, 3, 6, 8, 3, 8, 9,
            4, 9, 5, 2, 4, 11, 6, 2, 10, 8, 6, 7, 9, 8, 1,
        )
        repeat(subdivisions) {
            val mid = HashMap<Long, Int>()
            fun midpoint(a: Int, b: Int): Int {
                val key = minOf(a, b).toLong() shl 32 or maxOf(a, b).toLong()
                return mid.getOrPut(key) {
                    val p = verts[a]
                    val q = verts[b]
                    add((p[0] + q[0]) / 2f, (p[1] + q[1]) / 2f, (p[2] + q[2]) / 2f)
                }
            }
            val out = IntArray(tris.size * 4)
            var o = 0
            for (k in tris.indices step 3) {
                val a = tris[k]; val b = tris[k + 1]; val c = tris[k + 2]
                val ab = midpoint(a, b); val bc = midpoint(b, c); val ca = midpoint(c, a)
                for (v in intArrayOf(a, ab, ca, b, bc, ab, c, ca, bc, ab, bc, ca)) out[o++] = v
            }
            tris = out
        }
        val flat = FloatArray(verts.size * 3)
        for (i in verts.indices) verts[i].copyInto(flat, i * 3)
        return flat to tris
    }
}

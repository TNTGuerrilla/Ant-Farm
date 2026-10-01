package com.bydesigninteractive.ant.sim.ant

import com.bydesigninteractive.ant.sim.world.SURFACE_MM

/** Buckets surface ants on a coarse grid so counting neighbors is cheap. */
class SpatialIndex(private val bucketMm: Float = 20f) {
    private val size = (SURFACE_MM / bucketMm).toInt() + 1
    private val head = IntArray(size * size).also { it.fill(-1) }
    private var used = IntArray(64)
    private var usedCount = 0
    private var next = IntArray(64)
    private var members: List<Ant> = emptyList()

    /** Rebuckets the surface ants, resetting only the buckets the last rebuild filled. */
    fun rebuild(ants: List<Ant>) {
        for (i in 0 until usedCount) head[used[i]] = -1
        usedCount = 0
        if (next.size < ants.size) next = IntArray(ants.size * 2)
        if (used.size < ants.size) used = IntArray(ants.size * 2)
        members = ants
        for (i in ants.indices) {
            val a = ants[i]
            if (a.space != Space.SURFACE) continue
            val b = bucket(a.x) + bucket(a.y) * size
            if (head[b] < 0) used[usedCount++] = b
            next[i] = head[b]
            head[b] = i
        }
    }

    /** Other surface ants within [radius] of [a]; [radius] must not exceed the bucket size. */
    fun countNear(a: Ant, radius: Float): Int {
        val bx = bucket(a.x)
        val by = bucket(a.y)
        val r2 = radius * radius
        var n = 0
        for (y in by - 1..by + 1) for (x in bx - 1..bx + 1) {
            if (x < 0 || y < 0 || x >= size || y >= size) continue
            var i = head[x + y * size]
            while (i >= 0) {
                val o = members[i]
                val dx = o.x - a.x
                val dy = o.y - a.y
                if (o !== a && dx * dx + dy * dy <= r2) n++
                i = next[i]
            }
        }
        return n
    }

    private fun bucket(v: Float) = (v / bucketMm).toInt().coerceIn(0, size - 1)
}

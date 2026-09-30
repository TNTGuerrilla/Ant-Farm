package com.bydesigninteractive.ant.sim.world

/**
 * Breadth-first step counts through air cells from a set of source cells. Only the cells it
 * reached are reset on the next rebuild, so the cost follows the size of the nest, not the slice.
 */
class DistanceMap(private val grid: NestGrid) {
    private val w = grid.width
    private val dist = IntArray(grid.width * grid.depth) { UNREACHED }
    private var touched = IntArray(1024)
    private var touchedCount = 0
    private var queue = IntArray(1024)

    fun get(x: Int, y: Int): Int = if (grid.inBounds(x, y)) dist[y * w + x] else UNREACHED

    /** Recomputes from the first [count] of [sources], packed as y * width + x. */
    fun rebuild(sources: IntArray, count: Int) {
        for (i in 0 until touchedCount) dist[touched[i]] = UNREACHED
        touchedCount = 0
        var head = 0
        var tail = 0
        for (i in 0 until count) {
            val s = sources[i]
            if (dist[s] != UNREACHED) continue
            dist[s] = 0
            touch(s)
            queue = ensure(queue, tail + 1)
            queue[tail++] = s
        }
        while (head < tail) {
            val c = queue[head++]
            val x = c % w
            val y = c / w
            val d = dist[c] + 1
            for (k in 0 until 4) {
                val nx = x + DX[k]
                val ny = y + DY[k]
                if (!grid.inBounds(nx, ny)) continue
                val n = ny * w + nx
                if (dist[n] != UNREACHED || !grid.isAir(nx, ny)) continue
                dist[n] = d
                touch(n)
                queue = ensure(queue, tail + 1)
                queue[tail++] = n
            }
        }
    }

    private fun touch(i: Int) {
        touched = ensure(touched, touchedCount + 1)
        touched[touchedCount++] = i
    }

    companion object {
        const val UNREACHED = Int.MAX_VALUE
        val DX = intArrayOf(1, -1, 0, 0)
        val DY = intArrayOf(0, 0, 1, -1)
    }
}

internal fun ensure(a: IntArray, size: Int): IntArray =
    if (size <= a.size) a else a.copyOf(maxOf(size, a.size * 2))

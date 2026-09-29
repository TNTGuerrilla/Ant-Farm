package com.bydesigninteractive.ant.core.stub

/** Frame times in milliseconds over a sliding window of the newest [capacity] frames. */
class FrameStats(private val capacity: Int) {
    private val times = FloatArray(capacity)
    private val sorted = FloatArray(capacity)
    private var next = 0
    var count = 0
        private set

    fun add(ms: Float) {
        times[next] = ms
        next = (next + 1) % capacity
        if (count < capacity) count++
    }

    /** Nearest-rank percentile, with [p] from 0 to 1. */
    fun percentile(p: Float): Float {
        if (count == 0) return 0f
        times.copyInto(sorted, 0, 0, count)
        sorted.sort(0, count)
        val rank = kotlin.math.ceil(p * count).toInt().coerceIn(1, count)
        return sorted[rank - 1]
    }

    fun max(): Float {
        var max = 0f
        for (i in 0 until count) if (times[i] > max) max = times[i]
        return max
    }

    fun countOver(ms: Float): Int {
        var n = 0
        for (i in 0 until count) if (times[i] > ms) n++
        return n
    }
}

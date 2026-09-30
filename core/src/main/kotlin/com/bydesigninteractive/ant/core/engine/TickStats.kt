package com.bydesigninteractive.ant.core.engine

data class TickSummary(val ticksPerSecond: Float, val msPerTickAvg: Float, val msPerTickMax: Float)

/** Tick durations over a sliding time window, for the HUD and the log (Minecraft's TPS and MSPT). */
class TickStats(private val windowNanos: Long = 10_000_000_000L) {
    private val ends = LongArray(CAPACITY)
    private val durations = LongArray(CAPACITY)
    private var head = 0
    private var count = 0

    fun record(tickNanos: Long, endNanos: Long) {
        ends[head] = endNanos
        durations[head] = tickNanos
        head = (head + 1) % CAPACITY
        if (count < CAPACITY) count++
    }

    fun summary(now: Long): TickSummary {
        var n = 0
        var sum = 0L
        var max = 0L
        for (k in 0 until count) {
            val i = Math.floorMod(head - 1 - k, CAPACITY)
            if (now - ends[i] > windowNanos) break
            n++
            sum += durations[i]
            if (durations[i] > max) max = durations[i]
        }
        val seconds = windowNanos / 1e9f
        return TickSummary(n / seconds, if (n == 0) 0f else sum / n / 1e6f, max / 1e6f)
    }

    private companion object {
        const val CAPACITY = 8192
    }
}

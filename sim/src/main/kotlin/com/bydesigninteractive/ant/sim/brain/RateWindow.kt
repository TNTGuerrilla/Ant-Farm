package com.bydesigninteractive.ant.sim.brain

import com.bydesigninteractive.ant.sim.DT
import kotlin.math.min
import kotlin.math.roundToLong

/**
 * Counts events over the last [seconds] whole simulated seconds (the current second and the
 * [seconds] - 1 before it), in one bucket per second. Ticks passed in must never go backward.
 */
class RateWindow(private val seconds: Int) {
    private val buckets = IntArray(seconds)
    private var second = 0L
    private var total = 0

    fun add(tick: Long) {
        advance(tick)
        buckets[(second % seconds).toInt()]++
        total++
    }

    fun count(tick: Long): Int {
        advance(tick)
        return total
    }

    private fun advance(tick: Long) {
        val s = tick / TICKS_PER_SECOND
        if (s <= second) return
        val steps = min(s - second, seconds.toLong())
        for (i in 1..steps) {
            val k = ((second + i) % seconds).toInt()
            total -= buckets[k]
            buckets[k] = 0
        }
        second = s
    }

    private companion object {
        val TICKS_PER_SECOND = (1f / DT).roundToLong()
    }
}

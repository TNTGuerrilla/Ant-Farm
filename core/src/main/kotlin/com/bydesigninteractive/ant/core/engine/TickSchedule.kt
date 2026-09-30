package com.bydesigninteractive.ant.core.engine

import com.bydesigninteractive.ant.sim.DT

/** Nanoseconds per tick at a speed multiplier (1, 4 or 16). */
fun intervalFor(speed: Float): Long = (DT * 1e9 / speed).toLong()

/**
 * Minecraft-style tick timing. A tick is due every [intervalNanos]; after a tick the next is due
 * one interval later, so a slow tick makes the following ones run back to back until the world
 * has caught up. More than [maxLagNanos] behind, the schedule resets to now and the backlog is
 * dropped (Minecraft's "can't keep up" rule): the world slows, the frame rate does not.
 */
class TickSchedule(intervalNanos: Long, private val maxLagNanos: Long = MAX_LAG_NANOS) {
    var intervalNanos = intervalNanos
        private set
    var resets = 0
        private set
    private var nextDue = 0L

    /** Starts, or restarts after a pause, with a tick due at [now]. */
    fun start(now: Long) {
        nextDue = now
    }

    /** Changes the interval (a speed change) and restarts from [now]. */
    fun setInterval(nanos: Long, now: Long) {
        intervalNanos = nanos
        nextDue = now
    }

    /** How long until the next tick is due; zero or less means run one now. */
    fun waitNanos(now: Long): Long = nextDue - now

    /** Records that a tick ran, given the time it finished. */
    fun ticked(now: Long) {
        nextDue += intervalNanos
        if (now - nextDue > maxLagNanos) {
            nextDue = now
            resets++
        }
    }

    companion object {
        const val MAX_LAG_NANOS = 2_000_000_000L
    }
}

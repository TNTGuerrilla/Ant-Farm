package com.bydesigninteractive.ant.sim.world

import kotlin.math.PI
import kotlin.math.cos

/**
 * The simulated day: [DAY_SECONDS] of simulation per day (60 real minutes at 1x, the owner's
 * choice in M1b-2c), starting at time of day [START], mid-morning. Core's DayCycle reads it.
 */
object DayClock {
    const val DAY_SECONDS = 3600f
    const val START = 0.38f

    /** The time of day, 0 to 1 (0 midnight, 0.5 noon), after [seconds] of simulation. */
    fun timeOfDay(seconds: Float): Float {
        val t = (seconds / DAY_SECONDS + START) % 1f
        return if (t < 0f) t + 1f else t
    }
}

/**
 * Ground temperature through the day, a smooth curve until weather arrives (M5): [MEAN] plus
 * [AMPLITUDE] at its warmest at time of day [PEAK] (about 14:24) and coldest twelve hours later.
 * The nest swings by [NEST_DAMPING] of that.
 */
object GroundTemperature {
    const val MEAN = 18f
    const val AMPLITUDE = 8f
    const val PEAK = 0.6f
    const val NEST_DAMPING = 0.3f

    fun surface(timeOfDay: Float): Float = MEAN + AMPLITUDE * cycle(timeOfDay)

    fun nest(timeOfDay: Float): Float = MEAN + AMPLITUDE * NEST_DAMPING * cycle(timeOfDay)

    private fun cycle(t: Float): Float = cos(2f * PI.toFloat() * (t - PEAK))
}

package com.bydesigninteractive.ant.core.render

import com.bydesigninteractive.ant.sim.ant.Ant
import kotlin.math.floor

/**
 * Keeps a gait phase per ant and picks the leg frame to draw. The phase advances by distance
 * walked, so the legs speed up with the simulation speed and stop when it is paused.
 */
class AntAnimator {
    private var phases = FloatArray(INITIAL)

    /** Advances every ant's phase by `speed * simSeconds / STRIDE_MM` (one unit is one stride). */
    fun advance(ants: List<Ant>, simSeconds: Float) {
        for (a in ants) {
            if (a.id >= phases.size) phases = phases.copyOf(maxOf(a.id + 1, phases.size * 2))
            phases[a.id] += a.speed * simSeconds / STRIDE_MM
        }
    }

    /** The gait phase of [a] in strides; 0 for an ant that has not been advanced. */
    fun phase(a: Ant): Float = if (a.id < phases.size) phases[a.id] else 0f

    /** Frame 1 (neutral) while standing; otherwise the cycle 0, 1, 2, 1 by quarter strides. */
    fun frame(a: Ant): Int {
        if (a.speed == 0f) return 1
        val q = floor(phase(a) * 4f).toInt() and 3
        return CYCLE[q]
    }

    private companion object {
        /** A 5 mm worker's step. */
        const val STRIDE_MM = 3f
        const val INITIAL = 256
        val CYCLE = intArrayOf(0, 1, 2, 1)
    }
}

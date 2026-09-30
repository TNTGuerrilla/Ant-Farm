package com.bydesigninteractive.ant.core.render

import com.bydesigninteractive.ant.core.engine.AntPose
import com.bydesigninteractive.ant.sim.ant.Ant
import com.bydesigninteractive.ant.sim.ant.Space
import kotlin.math.floor
import kotlin.math.sqrt

/**
 * Keeps a gait phase per ant and picks the leg frame to draw. The phase advances by the distance
 * each ant is drawn moving between frames, so the legs keep pace with the interpolated motion and
 * stop when the ant does. A change of space (entering or leaving the nest) is not walking.
 */
class AntAnimator {
    private var phases = FloatArray(INITIAL)
    private var lastX = FloatArray(INITIAL)
    private var lastY = FloatArray(INITIAL)
    private var lastZ = FloatArray(INITIAL)
    private var lastSpace = ByteArray(INITIAL) { -1 }

    /** Advances [p]'s phase by the distance from where it was last observed. */
    fun observe(p: AntPose) {
        grow(p.id)
        val i = p.id
        val sp: Byte = if (p.space == Space.SURFACE) 1 else 0
        if (lastSpace[i] == sp) {
            val dx = p.x - lastX[i]
            val dy = p.y - lastY[i]
            val dz = p.z - lastZ[i]
            phases[i] += sqrt(dx * dx + dy * dy + dz * dz) / STRIDE_MM
            phases[i] -= floor(phases[i])
        }
        lastX[i] = p.x
        lastY[i] = p.y
        lastZ[i] = p.z
        lastSpace[i] = sp
    }

    /** The gait phase of ant [id] in strides, in [0, 1). */
    fun phase(id: Int): Float = if (id < phases.size) phases[id] else 0f

    /** Frame 1 (neutral) while standing; otherwise the cycle 0, 1, 2, 1 by quarter strides. */
    fun frame(p: AntPose): Int {
        if (p.speed == 0f) return 1
        return CYCLE[floor(phase(p.id) * 4f).toInt() and 3]
    }

    // The old API, still used by the renderers until it is removed.

    /** Advances every ant's phase by `speed * simSeconds / STRIDE_MM` (one unit is one stride). */
    fun advance(ants: List<Ant>, simSeconds: Float) {
        for (a in ants) {
            grow(a.id)
            phases[a.id] += a.speed * simSeconds / STRIDE_MM
            phases[a.id] -= floor(phases[a.id])
        }
    }

    /** The gait phase of [a] in strides; 0 for an ant that has not been advanced. */
    fun phase(a: Ant): Float = phase(a.id)

    /** Frame 1 (neutral) while standing; otherwise the cycle 0, 1, 2, 1 by quarter strides. */
    fun frame(a: Ant): Int {
        if (a.speed == 0f) return 1
        val q = floor(phase(a) * 4f).toInt() and 3
        return CYCLE[q]
    }

    private fun grow(id: Int) {
        if (id < phases.size) return
        val c = maxOf(id + 1, phases.size * 2)
        phases = phases.copyOf(c)
        lastX = lastX.copyOf(c)
        lastY = lastY.copyOf(c)
        lastZ = lastZ.copyOf(c)
        val old = lastSpace.size
        lastSpace = lastSpace.copyOf(c)
        for (k in old until c) lastSpace[k] = -1
    }

    private companion object {
        /** A 5 mm worker's step. */
        const val STRIDE_MM = 3f
        const val INITIAL = 256
        val CYCLE = intArrayOf(0, 1, 2, 1)
    }
}

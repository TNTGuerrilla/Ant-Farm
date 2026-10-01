package com.bydesigninteractive.ant.sim.ant

import com.bydesigninteractive.ant.sim.DT
import com.bydesigninteractive.ant.sim.World
import kotlin.math.PI

/**
 * Obstacle detours for directed walking. Steering straight at a target projected into the
 * tangent plane can park an ant where the surface normal points at the target (the widest point
 * of a rock between it and a plant). Real ants that make no headway walk around the obstacle:
 * when the horizontal distance to the target has not dropped by `detourProgressMm` within
 * `detourStallSeconds`, the ant turns about 90 degrees to a random side and walks a run of
 * exponential length (mean `detourRunMm`) by the normal walking rules, then steers again.
 *
 * Used where an ant re-aims at its target every tick: a forager heading for a plant, and a
 * forager or digger heading for the entrance in sight. Homing by the path integration vector
 * re-aims only once per run, so it cannot be parked this way, and a stall detector there fired
 * on ordinary runs that drift off course.
 */
internal object Detour {
    const val NONE = Int.MIN_VALUE

    /** The nest entrance in sight ([AntParams.homeSightRadius]). Plants use their own food id. */
    const val NEST_SIGHT = -1

    private const val QUARTER_TURN = (PI / 2).toFloat()

    /**
     * Tracks progress toward target [key], now [dist] mm away horizontally. Returns true while
     * the ant is on a detour, when the caller must not steer toward the target this tick. A new
     * target, or directed walking that was interrupted for a tick or more, starts tracking afresh.
     */
    fun detouring(w: World, a: Ant, key: Int, dist: Float): Boolean {
        val p = w.params
        if (a.detourKey != key || a.detourTick != w.tick - 1) {
            a.detourKey = key
            a.detourMark = dist
            a.detourTimer = 0f
            a.detourLeft = 0f
        }
        a.detourTick = w.tick
        if (a.detourLeft > 0f) {
            a.detourLeft -= p.surfaceSpeed * DT
            if (a.detourLeft > 0f) return true
            a.detourLeft = 0f
            a.detourMark = dist
            a.detourTimer = 0f
            a.runLeft = 0f
            return false
        }
        if (dist <= a.detourMark - p.detourProgressMm) {
            a.detourMark = dist
            a.detourTimer = 0f
            return false
        }
        a.detourTimer += DT
        if (a.detourTimer < p.detourStallSeconds) return false
        SurfaceWalk.turn(a, if (w.rng.nextBoolean()) QUARTER_TURN else -QUARTER_TURN)
        a.detourLeft = w.exponential(p.detourRunMm)
        a.runLeft = a.detourLeft
        a.detourTimer = 0f
        return true
    }
}

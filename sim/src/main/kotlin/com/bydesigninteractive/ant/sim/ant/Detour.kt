package com.bydesigninteractive.ant.sim.ant

import com.bydesigninteractive.ant.sim.DT
import com.bydesigninteractive.ant.sim.World
import kotlin.math.PI
import kotlin.math.min
import kotlin.math.pow

/**
 * Obstacle detours for directed walking. Steering straight at a target projected into the
 * tangent plane can park an ant where the surface normal points at the target (the widest point
 * of a rock between it and a plant). Real ants that make no headway walk around the obstacle:
 * when the horizontal distance to the target has not dropped by `detourProgressMm` within
 * `detourStallSeconds`, the ant turns about 90 degrees to a random side and walks a run of
 * exponential length (mean `detourRunMm`) by the normal walking rules, then steers again.
 * Consecutive detours for the same target escalate: the mean doubles each time (up to
 * `detourRunCapMm`) and the turn widens toward 135 degrees. The count resets when the target
 * changes, when tracking restarts, or when the ant gets `detourResetMm` closer than it was at
 * the first stall.
 *
 * A detour walks its own run ([Ant.detourLeft]) and never touches the search or homing run
 * ([Ant.runLeft]), so its leftover can never become a home-vector run (M2a, spec 5.3).
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
    private const val MAX_TURN = (PI * 3 / 4).toFloat() // 135 degrees

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
            a.detourCount = 0
        }
        a.detourTick = w.tick
        if (a.detourLeft > 0f) {
            a.detourLeft -= p.surfaceSpeed * DT
            if (a.detourLeft > 0f) return true
            a.detourLeft = 0f
            a.detourMark = dist
            a.detourTimer = 0f
            return false
        }
        if (dist <= a.detourMark - p.detourProgressMm) {
            a.detourMark = dist
            a.detourTimer = 0f
            // Real progress (well past where the first stall was) ends the escalation.
            if (a.detourCount > 0 && dist <= a.detourBase - p.detourResetMm) a.detourCount = 0
            return false
        }
        a.detourTimer += DT
        if (a.detourTimer < p.detourStallSeconds) return false
        // Each further detour for the same target runs further and turns wider (90 toward 135
        // degrees), so a rock too big for a short one is gone round, not re-entered.
        if (a.detourCount == 0) a.detourBase = a.detourMark
        val turn = min(MAX_TURN, QUARTER_TURN + a.detourCount * p.detourTurnStep)
        SurfaceWalk.turn(a, if (w.rng.nextBoolean()) turn else -turn)
        val mean = min(p.detourRunCapMm, p.detourRunMm * p.detourRunGrowth.pow(a.detourCount))
        a.detourCount++
        a.detourLeft = w.exponential(mean)
        a.detourTimer = 0f
        w.detoursStarted++
        return true
    }

    /** True while [a] is walking a detour that [detouring] gave it this tick. */
    fun walking(w: World, a: Ant): Boolean = a.detourLeft > 0f && a.detourTick == w.tick
}

package com.bydesigninteractive.ant.sim.search

import com.bydesigninteractive.ant.sim.DT
import com.bydesigninteractive.ant.sim.World
import com.bydesigninteractive.ant.sim.ant.Ant
import com.bydesigninteractive.ant.sim.ant.AntState
import com.bydesigninteractive.ant.sim.ant.Role
import com.bydesigninteractive.ant.sim.ant.Space
import com.bydesigninteractive.ant.sim.ant.SurfaceWalk
import com.bydesigninteractive.ant.sim.brain.Action
import com.bydesigninteractive.ant.sim.brain.Genome
import com.bydesigninteractive.ant.sim.brain.Outputs
import com.bydesigninteractive.ant.sim.world.Field3
import java.util.Locale
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * What the Y-choice trial counted: releases that took the [marked] or the [unmarked] branch,
 * those that [strayed] (left the stem before the fork, turned back, or went off sideways), and
 * those still undecided at the timeout; [left] of the choices took the left branch. The marked
 * share and the share that made no choice ([strayShare], timeouts included) are scored.
 */
class YChoiceResult(val marked: Int, val unmarked: Int, val strayed: Int, val timeouts: Int, val left: Int) {
    val choices: Int get() = marked + unmarked
    val releases: Int get() = choices + strayed + timeouts

    /** Releases that made no choice (strayed or timed out) over all releases, or null with none. */
    val strayShare: Double? get() = if (releases > 0) (strayed + timeouts).toDouble() / releases else null

    /** The marked branch's share of the choices, or null with none. */
    val share: Double? get() = if (choices > 0) marked.toDouble() / choices else null

    operator fun plus(o: YChoiceResult) =
        YChoiceResult(marked + o.marked, unmarked + o.unmarked, strayed + o.strayed, timeouts + o.timeouts, left + o.left)

    fun line(): String = String.format(
        Locale.ROOT, "Y choice: marked %d, unmarked %d (share %s), strayed %d, timeouts %d, left %d of %d choices",
        marked, unmarked, share?.let { String.format(Locale.ROOT, "%.3f", it) } ?: "none", strayed, timeouts, left, choices,
    )
}

/**
 * The Y-choice trial for naive trail following (Gruter 2011: 62 to 70% of naive foragers take the
 * marked branch at a bifurcation). On open ground with no other ants, an artificial trail is laid
 * into the trail field: a stem of [STEM_MM] at about [STEM_STRENGTH] T (the two antennae
 * together, T the trail threshold) pointing away from the nest, then a fork into two branches
 * [BRANCH_ANGLE] either side of the stem's line (60 degrees apart), one marked as strongly as the
 * stem and the other bare. A never-fed forager is put at the stem's start, heading along it, and
 * runs alone until it is [PAST_MM] past the fork, having come within [REACH_MM] of it, inside
 * [CLASSIFY_ANGLE] of the stem's line (a choice, by which side it is on), or until it strays or
 * [TIMEOUT_TICKS] pass. The marked side alternates between releases, so a turning bias does not
 * read as following.
 *
 * Each release gets a fresh Y on a grid point [GRID_MM] from the others, laid just before it, so
 * every ant meets the same trail (old Ys fade, and a stray is stopped well before the next Y).
 * Each release's ant is removed from the world when it is done, and carries the personal brain
 * of ant number k (the release index) of this seed, as if each were a different worker. Never-fed
 * foragers lay no trail, so the trails change only by decay. All randomness is the world's.
 */
object YChoice {
    const val RELEASES = 100
    const val STEM_MM = 80f
    const val BRANCH_MM = 100f
    const val BRANCH_ANGLE = (PI / 6).toFloat()
    const val STEM_STRENGTH = 2f
    const val MARK_SPACING_MM = 2f
    const val REACH_MM = 15f
    const val PAST_MM = 40f
    const val STRAY_MM = 150f
    const val CLASSIFY_ANGLE = PI / 3
    const val TIMEOUT_TICKS = 600 // 30 s
    const val GRID = 10
    const val GRID_MM = 400f

    /** Runs [releases] (at most GRID x GRID) on a fresh open-ground world of [seed] with [genome] (the instinct brain if null). */
    fun run(genome: Genome?, seed: Long, releases: Int = RELEASES): YChoiceResult {
        require(releases in 1..GRID * GRID)
        val w = World(seed, rocks = false, genome = genome)
        val s = w.surface
        var marked = 0
        var unmarked = 0
        var strayed = 0
        var timeouts = 0
        var left = 0
        for (k in 0 until releases) {
            // The fork on a grid around the entrance, the stem pointing away from the nest.
            val forkX = s.entranceX + (k % GRID - (GRID - 1) / 2f) * GRID_MM
            val forkY = s.entranceY + (k / GRID - (GRID - 1) / 2f) * GRID_MM
            var ux = forkX - s.entranceX
            var uy = forkY - s.entranceY
            val len = sqrt(ux * ux + uy * uy)
            ux /= len
            uy /= len
            val heading = atan2(uy, ux)
            val startX = forkX - ux * STEM_MM
            val startY = forkY - uy * STEM_MM
            val markedLeft = k % 2 == 0
            val branch = heading + if (markedLeft) BRANCH_ANGLE else -BRANCH_ANGLE
            val amount = markAmount(w, startX, startY, ux, uy)
            lay(w, s.trail, startX, startY, forkX, forkY, amount)
            lay(w, s.trail, forkX, forkY, forkX + cos(branch) * BRANCH_MM, forkY + sin(branch) * BRANCH_MM, amount)

            val a = release(w, k, startX, startY, heading)
            var reached = false
            var outcome = TIMEOUT
            for (t in 0 until TIMEOUT_TICKS) {
                w.step()
                if (a.space != Space.SURFACE) {
                    outcome = STRAYED
                    break
                }
                val px = a.x - forkX
                val py = a.y - forkY
                val d = sqrt(px * px + py * py)
                if (d <= REACH_MM) reached = true
                if (reached && d >= PAST_MM) {
                    // The side of the stem's line the ant is on, positive to the left.
                    val angle = atan2((ux * py - uy * px).toDouble(), (ux * px + uy * py).toDouble())
                    outcome = when {
                        abs(angle) > CLASSIFY_ANGLE -> STRAYED
                        (angle > 0.0) == markedLeft -> MARKED
                        else -> UNMARKED
                    }
                    if (outcome != STRAYED && angle > 0.0) left++
                    break
                }
                if (d > STRAY_MM) {
                    outcome = STRAYED
                    break
                }
            }
            when (outcome) {
                MARKED -> marked++
                UNMARKED -> unmarked++
                STRAYED -> strayed++
                else -> timeouts++
            }
            w.ants.remove(a)
        }
        return YChoiceResult(marked, unmarked, strayed, timeouts, left)
    }

    /** Puts a never-fed forager at (x, y) on the surface, searching, facing [heading], with release [k]'s personal brain. */
    private fun release(w: World, k: Int, x: Float, y: Float, heading: Float): Ant {
        val a = w.addAnt(Role.FORAGER)
        a.brain = w.genome.personal(w.seed, k, biasFrom = Outputs.DEPOSIT)
        a.space = Space.SURFACE
        SurfaceWalk.place(w, a, x, y, heading)
        a.homeDx = a.x - w.surface.entranceX
        a.homeDy = a.y - w.surface.entranceY
        a.action = Action.WALK
        a.state = AntState.SEARCH
        return a
    }

    /** Lays marks of [amount] every [MARK_SPACING_MM] on the ground from (x0, y0) to (x1, y1). */
    private fun lay(w: World, field: Field3, x0: Float, y0: Float, x1: Float, y1: Float, amount: Float) {
        val dx = x1 - x0
        val dy = y1 - y0
        val n = (sqrt(dx * dx + dy * dy) / MARK_SPACING_MM).toInt()
        for (i in 0..n) {
            val x = x0 + dx * i / n
            val y = y0 + dy * i / n
            field.add(x, y, w.surface.ground.height(x, y), amount)
        }
    }

    /**
     * The mark amount that gives the stem from (x, y) along (ux, uy) a reading of
     * [STEM_STRENGTH] T: laid with amount 1 into a scratch field with no decay, the two antennae
     * of an ant on the stem facing along it are read at points along its middle, and the amount
     * scales their mean sum to the target (the field is linear in what is laid).
     */
    private fun markAmount(w: World, x: Float, y: Float, ux: Float, uy: Float): Float {
        val scratch = Field3(DT, 0f, 0f)
        lay(w, scratch, x, y, x + ux * STEM_MM, y + uy * STEM_MM, 1f)
        val p = w.params
        val along = p.senseAhead * cos(p.senseAngle)
        val side = p.senseAhead * sin(p.senseAngle)
        var sum = 0f
        var samples = 0
        var d = 10f
        while (d <= STEM_MM - 10f - along) {
            val cx = x + ux * (d + along)
            val cy = y + uy * (d + along)
            for (sign in intArrayOf(1, -1)) {
                val ax = cx - uy * side * sign
                val ay = cy + ux * side * sign
                sum += scratch.peek(ax, ay, w.surface.ground.height(ax, ay))
            }
            samples++
            d += 3f
        }
        return STEM_STRENGTH * p.trailThreshold / (sum / samples)
    }

    private const val TIMEOUT = 0
    private const val MARKED = 1
    private const val UNMARKED = 2
    private const val STRAYED = 3
}

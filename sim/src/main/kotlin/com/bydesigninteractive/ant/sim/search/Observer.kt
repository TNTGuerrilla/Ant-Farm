package com.bydesigninteractive.ant.sim.search

import com.bydesigninteractive.ant.sim.DT
import com.bydesigninteractive.ant.sim.World
import com.bydesigninteractive.ant.sim.ant.Ant
import com.bydesigninteractive.ant.sim.ant.AntState
import com.bydesigninteractive.ant.sim.ant.Role
import com.bydesigninteractive.ant.sim.ant.Space
import com.bydesigninteractive.ant.sim.brain.Body
import com.bydesigninteractive.ant.sim.world.SURFACE_MM
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sqrt

/** Why a variant's trial was dropped (spec 4.2). */
enum class Death { NONE, NO_FEED, NO_RETURN, NOT_MOVING, EDGE_PILE, NON_NUMERIC }

/**
 * Watches a run from outside, after each tick, and turns what the ants do into the litmus
 * measurements (spec 4.3) and the viability checks (spec 4.2); the M2a plan's Task 9 table says
 * how. It reads ant fields only and never draws from the world's generator, so watching never
 * changes a run. The colony's size must not change after it is created.
 *
 * Naive trail following is not measured here: never-fed searchers on the starter world meet too
 * few trails (about 5 per seed), so it has its own trial, [YChoice].
 *
 * The drift measure ([Measurements.meanAbsTurnBias]) follows each searching forager's compass
 * heading tick by tick and adds up its signed turn and its time over all its search bouts
 * together, leaving out ticks on a trail, on an obstacle detour, and on or by a plant stem
 * (where the turn is the trail's, the detour's or the climb's, not the search's). Each forager
 * with at least 120 s of such searching gives |turn| / time; the measure is their mean. A
 * searcher whose turns balance out has a floor of about 0.07 rad/s at 120 s from its random
 * turns alone, half that of 30 s windows, so a steady bias stands out above it.
 */
class Observer(private val w: World) {
    private val n = w.ants.size
    private val lastState = IntArray(n) { -1 }
    private val lastSpace = IntArray(n) { -1 }
    private val lastCrop = FloatArray(n)
    private val lastMarks = IntArray(n)
    private val lastX = FloatArray(n)
    private val lastY = FloatArray(n)
    private val lastZ = FloatArray(n)
    private val lastHeading = FloatArray(n)
    private val feedStart = LongArray(n) { -1L }
    private val returnStart = LongArray(n) { -1L }
    private val unloadStart = LongArray(n) { -1L }
    private val everFed = BooleanArray(n)
    private val eligibleMm = DoubleArray(n)
    private val fullReturns = IntArray(n)
    private val windowKind = IntArray(n)
    private val windowTicks = IntArray(n)
    private val windowLength = DoubleArray(n)
    private val windowX = FloatArray(n)
    private val windowY = FloatArray(n)
    private val driftTicks = IntArray(n)
    private val driftTurn = DoubleArray(n)
    private val lastNz = FloatArray(n)

    private var feedSum = 0.0
    private var feeds = 0
    private var returnSum = 0.0
    private var returns = 0
    private var unloadSum = 0.0
    private var unloadsSeen = 0
    private var nearAmount = 0.0
    private var nearMm = 0.0
    private var farAmount = 0.0
    private var farMm = 0.0
    private var outSum = 0.0
    private var outWindows = 0
    private var homeSum = 0.0
    private var homeWindows = 0
    private var shareSum = 0.0
    private var shareSamples = 0
    private var movingSum = 0.0
    private var movingSamples = 0
    private var edge = false
    private var nonNumeric = false

    /** True once any ant has fed. */
    var anyFed = false
        private set

    fun afterTick() {
        val tick = w.tick
        val markAmount = w.params.markAmount
        val sample = tick % SAMPLE_TICKS == 0L
        var surfaceAnts = 0
        var walkers = 0
        var moving = 0
        var nearEdge = 0
        var foragersOut = 0
        for (a in w.ants) {
            val i = a.id
            val state = a.state
            val surface = a.space == Space.SURFACE
            val space = a.space.ordinal

            // Trip timings.
            if (state == AntState.FEED && lastState[i] != AntState.FEED.ordinal) feedStart[i] = tick
            if (surface && lastCrop[i] <= 0f && a.crop > 0f) {
                if (feedStart[i] >= 0L) {
                    feedSum += (tick - feedStart[i]) * DT
                    feeds++
                }
                feedStart[i] = -1L
                everFed[i] = true
                anyFed = true
                returnStart[i] = tick
            }
            if (lastSpace[i] == Space.SURFACE.ordinal && !surface && a.crop > 0f) {
                if (returnStart[i] >= 0L) {
                    returnSum += (tick - returnStart[i]) * DT
                    returns++
                }
                returnStart[i] = -1L
                unloadStart[i] = tick
                if (a.crop >= a.desiredCrop) fullReturns[i]++
            }
            if (!surface && lastCrop[i] > 0f && a.crop <= 0f) {
                if (unloadStart[i] >= 0L) {
                    unloadSum += (tick - unloadStart[i]) * DT
                    unloadsSeen++
                }
                unloadStart[i] = -1L
            }

            // Distance walked on the surface this tick.
            val step = if (surface && lastSpace[i] == space) {
                val dx = a.x - lastX[i]
                val dy = a.y - lastY[i]
                val dz = a.z - lastZ[i]
                sqrt((dx * dx + dy * dy + dz * dz).toDouble())
            } else 0.0

            // Trail marks while full: overall (a count), and the amount laid near versus farther
            // from the food. The brain sets each mark's amount (markAmount times its deposit
            // strength, Actions.deposit), so the near-food ratio weighs marks by it: counting marks
            // alone gives about 1 for any brain that lays at all.
            val marks = a.marksLaid - lastMarks[i]
            if (surface && a.crop > 0f && a.crop >= a.desiredCrop) {
                eligibleMm[i] += step
                val sinceFed = Body.sinceFed(tick, a)
                val amount = marks * markAmount * a.deposit.toDouble()
                if (sinceFed < NEAR_SECONDS) {
                    nearAmount += amount
                    nearMm += step
                } else if (sinceFed < 2 * NEAR_SECONDS) {
                    farAmount += amount
                    farMm += step
                }
            }

            // Straightness in 5 s windows, outbound searching versus homeward.
            val kind = when {
                surface && a.role == Role.FORAGER && state == AntState.SEARCH -> OUT
                surface && a.role == Role.FORAGER && state == AntState.RETURN -> HOME
                else -> NONE
            }
            if (kind == NONE || kind != windowKind[i]) {
                windowKind[i] = kind
                windowTicks[i] = 0
                windowLength[i] = 0.0
                windowX[i] = a.x
                windowY[i] = a.y
            } else {
                windowTicks[i]++
                windowLength[i] += step
                if (windowTicks[i] >= WINDOW_TICKS) {
                    if (windowLength[i] > MIN_WINDOW_MM) {
                        val dx = (a.x - windowX[i]).toDouble()
                        val dy = (a.y - windowY[i]).toDouble()
                        val straightness = sqrt(dx * dx + dy * dy) / windowLength[i]
                        if (kind == OUT) {
                            outSum += straightness
                            outWindows++
                        } else {
                            homeSum += straightness
                            homeWindows++
                        }
                    }
                    windowTicks[i] = 0
                    windowLength[i] = 0.0
                    windowX[i] = a.x
                    windowY[i] = a.y
                }
            }

            // Drift: the signed turn of a searching forager this tick, added to its total over
            // all its search bouts, unless the turn was the trail's, a detour's or a stem's.
            if (kind == OUT && lastSpace[i] == space && lastState[i] == AntState.SEARCH.ordinal &&
                !a.onTrail && !detouring(a, tick) && a.nz >= STEEP_NZ && lastNz[i] >= STEEP_NZ && !byStem(a)
            ) {
                var d = (a.heading - lastHeading[i]).toDouble()
                while (d > PI) d -= 2 * PI
                while (d < -PI) d += 2 * PI
                driftTurn[i] += d
                driftTicks[i]++
            }

            if (sample) {
                if (!a.x.isFinite() || !a.y.isFinite() || !a.z.isFinite()) nonNumeric = true
                if (surface) {
                    surfaceAnts++
                    if (a.role == Role.FORAGER) foragersOut++
                    if (state != AntState.FEED) {
                        walkers++
                        if (a.speed > 0f) moving++
                    }
                    if (a.x < EDGE_MM || a.y < EDGE_MM || a.x > SURFACE_MM - EDGE_MM || a.y > SURFACE_MM - EDGE_MM) nearEdge++
                }
            }

            lastState[i] = state.ordinal
            lastSpace[i] = space
            lastCrop[i] = a.crop
            lastMarks[i] = a.marksLaid
            lastX[i] = a.x
            lastY[i] = a.y
            lastZ[i] = a.z
            lastHeading[i] = a.heading
            lastNz[i] = a.nz
        }
        if (sample) {
            shareSum += foragersOut.toDouble() / n
            shareSamples++
            if (walkers > 0) {
                movingSum += moving.toDouble() / walkers
                movingSamples++
            }
            if (surfaceAnts > 0 && nearEdge > EDGE_SHARE * surfaceAnts) edge = true
        }
    }

    /** The viability verdict after simulated minute [minute] (spec 4.2); [final] for the run's last minute. */
    fun death(minute: Int, viabilityMinute: Int, final: Boolean): Death {
        if (nonNumeric || w.nonFinite > 0) return Death.NON_NUMERIC
        if (edge) return Death.EDGE_PILE
        if (minute >= viabilityMinute && !anyFed) return Death.NO_FEED
        if (final) {
            if (w.unloads == 0) return Death.NO_RETURN
            if (movingSamples > 0 && movingSum / movingSamples < MOVING_MIN) return Death.NOT_MOVING
        }
        return Death.NONE
    }

    /** True if [a] walked an obstacle detour in the tick just stepped (`Detour.walking`, read one tick on). */
    private fun detouring(a: Ant, tick: Long): Boolean = a.detourLeft > 0f && a.detourTick == tick - 1

    /** True if [a] is within [STEM_MM] of an aphid plant's stem, where it climbs (`Primitives.faceUpStem`). */
    private fun byStem(a: Ant): Boolean {
        val foods = w.surface.foods
        for (k in foods.indices) {
            val f = foods[k]
            if (!f.hasStem) continue
            val dx = f.x - a.x
            val dy = f.y - a.y
            if (dx * dx + dy * dy < STEM_MM * STEM_MM) return true
        }
        return false
    }

    fun measurements(): Measurements {
        var marks = 0L
        var mm = 0.0
        var fullReturners = 0
        var never = 0
        var driftSum = 0.0
        var drifters = 0
        for (a in w.ants) {
            val i = a.id
            if (driftTicks[i] >= DRIFT_MIN_TICKS) {
                driftSum += abs(driftTurn[i]) / (driftTicks[i] * DT)
                drifters++
            }
            if (a.marksLaid > 0) {
                marks += a.marksLaid
                mm += eligibleMm[i]
            }
            if (fullReturns[i] > 0) {
                fullReturners++
                if (a.marksLaid == 0) never++
            }
        }
        return Measurements(
            feedSeconds = if (feeds > 0) feedSum / feeds else null,
            returnSeconds = if (returns > 0) returnSum / returns else null,
            unloadSeconds = if (unloadsSeen > 0) unloadSum / unloadsSeen else null,
            marksPer5cm = if (mm > 0.0) marks / mm * 50.0 else null,
            nearFoodRatio = if (nearMm > 0.0 && farMm > 0.0 && farAmount > 0.0) (nearAmount / nearMm) / (farAmount / farMm) else null,
            neverLaying = if (fullReturners > 0) never.toDouble() / fullReturners else null,
            straightOut = if (outWindows > 0) outSum / outWindows else null,
            straightHome = if (homeWindows > 0) homeSum / homeWindows else null,
            foragerShare = if (shareSamples > 0) shareSum / shareSamples else null,
            meanAbsTurnBias = if (drifters > 0) driftSum / drifters else null,
        )
    }

    private companion object {
        const val SAMPLE_TICKS = 200L
        const val WINDOW_TICKS = 100 // 5 s
        const val DRIFT_MIN_TICKS = 2400 // 120 s of searching, all bouts together
        const val STEEP_NZ = 0.5f // steeper than this is a stem or rock wall, where the compass heading means little
        const val STEM_MM = 10f // this close to a stem (horizontally) the ant may be turned to face up it
        const val MIN_WINDOW_MM = 20.0
        const val NEAR_SECONDS = 20f
        const val EDGE_MM = 50f
        const val EDGE_SHARE = 0.1
        const val MOVING_MIN = 0.2
        const val NONE = 0
        const val OUT = 1
        const val HOME = 2
    }
}

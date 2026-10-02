package com.bydesigninteractive.ant.sim.search

import com.bydesigninteractive.ant.sim.DT
import com.bydesigninteractive.ant.sim.World
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
 * The trail readings it uses for naive following are the ants' own antenna readings
 * (senseL and senseR), which each ant refreshes at its brain evaluation, every other tick.
 * The drift measure ([Measurements.meanAbsTurnBias]) follows each searching forager's compass
 * heading tick by tick in 30 s windows and keeps the size of its net turn per second.
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
    private val armed = BooleanArray(n) { true }
    private val encounterStart = LongArray(n) { -1L }
    private val windowKind = IntArray(n)
    private val windowTicks = IntArray(n)
    private val windowLength = DoubleArray(n)
    private val windowX = FloatArray(n)
    private val windowY = FloatArray(n)
    private val driftTicks = IntArray(n)
    private val driftTurn = DoubleArray(n)

    private var feedSum = 0.0
    private var feeds = 0
    private var returnSum = 0.0
    private var returns = 0
    private var unloadSum = 0.0
    private var unloadsSeen = 0
    private var nearMarks = 0L
    private var nearMm = 0.0
    private var farMarks = 0L
    private var farMm = 0.0
    private var encounters = 0
    private var follows = 0
    private var outSum = 0.0
    private var outWindows = 0
    private var homeSum = 0.0
    private var homeWindows = 0
    private var driftSum = 0.0
    private var driftWindows = 0
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
        val theta = w.params.trailThreshold
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

            // Trail marks while full: overall, and near versus farther from the food.
            val marks = a.marksLaid - lastMarks[i]
            if (surface && a.crop > 0f && a.crop >= a.desiredCrop) {
                eligibleMm[i] += step
                val sinceFed = Body.sinceFed(tick, a)
                if (sinceFed < NEAR_SECONDS) {
                    nearMarks += marks
                    nearMm += step
                } else if (sinceFed < 2 * NEAR_SECONDS) {
                    farMarks += marks
                    farMm += step
                }
            }

            // Naive trail following.
            if (surface && a.role == Role.FORAGER && !everFed[i] && state == AntState.SEARCH) {
                val r = a.senseL + a.senseR
                if (encounterStart[i] >= 0L) {
                    if (r < theta / 2f) {
                        encounters++
                        encounterStart[i] = -1L
                        armed[i] = true
                    } else if (tick - encounterStart[i] >= FOLLOW_TICKS) {
                        encounters++
                        follows++
                        encounterStart[i] = -1L
                    }
                } else if (armed[i] && r >= theta) {
                    encounterStart[i] = tick
                    armed[i] = false
                } else if (r < theta / 2f) {
                    armed[i] = true
                }
            } else {
                encounterStart[i] = -1L
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

            // Drift: the net signed turn of a searching forager over 30 s windows.
            if (kind == OUT && lastSpace[i] == space && lastState[i] == AntState.SEARCH.ordinal) {
                var d = (a.heading - lastHeading[i]).toDouble()
                while (d > PI) d -= 2 * PI
                while (d < -PI) d += 2 * PI
                driftTurn[i] += d
                driftTicks[i]++
                if (driftTicks[i] >= DRIFT_TICKS) {
                    driftSum += abs(driftTurn[i]) / (DRIFT_TICKS * DT)
                    driftWindows++
                    driftTicks[i] = 0
                    driftTurn[i] = 0.0
                }
            } else {
                driftTicks[i] = 0
                driftTurn[i] = 0.0
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

    fun measurements(): Measurements {
        var marks = 0L
        var mm = 0.0
        var fullReturners = 0
        var never = 0
        for (a in w.ants) {
            val i = a.id
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
            nearFoodRatio = if (nearMm > 0.0 && farMm > 0.0 && farMarks > 0L) (nearMarks / nearMm) / (farMarks / farMm) else null,
            neverLaying = if (fullReturners > 0) never.toDouble() / fullReturners else null,
            naiveFollowing = if (encounters > 0) follows.toDouble() / encounters else null,
            straightOut = if (outWindows > 0) outSum / outWindows else null,
            straightHome = if (homeWindows > 0) homeSum / homeWindows else null,
            foragerShare = if (shareSamples > 0) shareSum / shareSamples else null,
            meanAbsTurnBias = if (driftWindows > 0) driftSum / driftWindows else null,
        )
    }

    private companion object {
        const val SAMPLE_TICKS = 200L
        const val FOLLOW_TICKS = 60L // 3 s
        const val WINDOW_TICKS = 100 // 5 s
        const val DRIFT_TICKS = 600 // 30 s
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

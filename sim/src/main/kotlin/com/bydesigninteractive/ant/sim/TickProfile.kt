package com.bydesigninteractive.ant.sim

import java.util.Locale

/**
 * Where a tick's time goes, for benchmarks. Attach one with [World.profile]; [World.step] then
 * adds the nanoseconds of each phase and the surface counters, and a caller that publishes adds
 * [publishNanos]. A world with no profile pays only a null check per phase.
 */
class TickProfile {
    val phaseNanos = LongArray(PHASES)
    var publishNanos = 0L
    var ticks = 0L
    var sdfEvaluations = 0L
    var heightSamples = 0L
    var fastHits = 0L
    var fastMisses = 0L

    fun reset() {
        phaseNanos.fill(0L)
        publishNanos = 0L
        ticks = 0L
        sdfEvaluations = 0L
        heightSamples = 0L
        fastHits = 0L
        fastMisses = 0L
    }

    /** One line: ms per tick for each phase, SDF evaluations and height samples per tick, and the fast-path share. */
    fun summary(): String {
        val t = ticks.coerceAtLeast(1L).toDouble()
        val fast = fastHits + fastMisses
        val share = if (fast == 0L) 0.0 else 100.0 * fastHits / fast
        return String.format(
            Locale.ROOT,
            "index %.3f fields %.3f nest %.3f surfaceAnts %.3f nestAnts %.3f publish %.3f ms/tick; sdf %.0f height %.0f per tick; fast %.1f%%",
            phaseNanos[INDEX] / 1e6 / t, phaseNanos[FIELDS] / 1e6 / t, phaseNanos[NEST] / 1e6 / t,
            phaseNanos[SURFACE_ANTS] / 1e6 / t, phaseNanos[NEST_ANTS] / 1e6 / t, publishNanos / 1e6 / t,
            sdfEvaluations / t, heightSamples / t, share,
        )
    }

    companion object {
        const val INDEX = 0
        const val FIELDS = 1
        const val NEST = 2
        const val SURFACE_ANTS = 3
        const val NEST_ANTS = 4
        const val PHASES = 5
    }
}

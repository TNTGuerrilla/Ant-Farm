package com.bydesigninteractive.ant.sim.scenario

import com.bydesigninteractive.ant.sim.ant.AntState
import com.bydesigninteractive.ant.sim.ant.Role
import com.bydesigninteractive.ant.sim.ant.Space
import java.util.Locale
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.fail

/**
 * The seed brain against the scripted ants on the starter world (spec section 3, checks before
 * the search), and the open-ground hesitation check (spec 5.2).
 */
class BrainColonyTest {
    private class Colony(val unloads: Int, val outside: Double, val marks: Long, val air: Int)

    private fun colony(brains: Boolean, seed: Long): Colony {
        val w = Scenarios.starter(seed, brains = brains)
        var outside = 0.0
        var samples = 0
        repeat(MINUTES * TICKS_PER_MINUTE) {
            w.step()
            if (w.tick % 200 == 0L) {
                var out = 0
                var all = 0
                for (a in w.ants) if (a.role == Role.FORAGER) {
                    all++
                    if (a.space == Space.SURFACE) out++
                }
                outside += out.toDouble() / all
                samples++
            }
        }
        return Colony(w.unloads, outside / samples, w.trailMarks, w.nest.airCells)
    }

    /**
     * Tolerances (brains against scripts, seeds 1 to 3 summed or averaged): trips per hour within
     * a factor of 2, the share of foragers outside within 0.15, trail marks within a factor of 3,
     * nest air cells within 25%. Wide on purpose: the brain walks differently by design; this
     * only checks the seed is in the scripts' regime before the search refines it.
     */
    @Test
    fun theSeedBrainColonyIsInTheScriptsRegime() {
        val scripts = SEEDS.map { colony(false, it) }
        val brains = SEEDS.map { colony(true, it) }
        fun trips(c: List<Colony>) = c.sumOf { it.unloads } * 60.0 / MINUTES / c.size
        val sTrips = trips(scripts)
        val bTrips = trips(brains)
        val sOut = scripts.map { it.outside }.average()
        val bOut = brains.map { it.outside }.average()
        val sMarks = scripts.sumOf { it.marks }
        val bMarks = brains.sumOf { it.marks }
        val sAir = scripts.map { it.air }.average()
        val bAir = brains.map { it.air }.average()
        val line = String.format(
            Locale.ROOT,
            "trips/h scripts %.1f brains %.1f; outside %.2f vs %.2f; marks %d vs %d; air %.0f vs %.0f",
            sTrips, bTrips, sOut, bOut, sMarks, bMarks, sAir, bAir,
        )
        println("COMPARISON $line")
        assertTrue(sTrips >= 5.0, "the scripted colonies made too few trips to compare: $line")
        assertTrue(bTrips in sTrips * 0.5..sTrips * 2.0, line)
        assertTrue(abs(bOut - sOut) <= 0.15, line)
        assertTrue(sMarks > 0 && bMarks.toDouble() in sMarks / 3.0..sMarks * 3.0, line)
        assertTrue(bAir in sAir * 0.75..sAir * 1.25, line)
    }

    /** Spec 5.2: no moving surface ant pauses on open ground for more than 5 s. */
    @Test
    fun noMovingSurfaceAntPausesOnOpenGround() {
        val w = Scenarios.starter(1, brains = true)
        val moving = setOf(AntState.SEARCH, AntState.RETURN, AntState.DUMP, AntState.GO_HOME)
        val n = w.ants.size
        val ax = FloatArray(n)
        val ay = FloatArray(n)
        val since = LongArray(n) { -1L }
        repeat(10 * TICKS_PER_MINUTE) {
            w.step()
            for (a in w.ants) {
                val i = a.id
                if (a.space != Space.SURFACE || a.state !in moving || !w.surface.sdf.isOpenGround(a.x, a.y)) {
                    since[i] = -1L
                    continue
                }
                val dx = a.x - ax[i]
                val dy = a.y - ay[i]
                if (since[i] < 0L || dx * dx + dy * dy > PAUSE_MM * PAUSE_MM) {
                    ax[i] = a.x
                    ay[i] = a.y
                    since[i] = w.tick
                } else if (w.tick - since[i] >= PAUSE_TICKS) {
                    fail("ant ${a.id} (${a.state}) paused within $PAUSE_MM mm of (${ax[i]}, ${ay[i]}) from tick ${since[i]} to ${w.tick}")
                }
            }
        }
    }

    @Test
    fun theSameSeedGivesTheSameBrainRun() {
        val a = Scenarios.starter(4, brains = true)
        val b = Scenarios.starter(4, brains = true)
        repeat(3000) {
            a.step()
            b.step()
        }
        assertEquals(fingerprint(a), fingerprint(b))
    }

    private companion object {
        val SEEDS = listOf(1L, 2L, 3L)
        const val MINUTES = 20
        const val TICKS_PER_MINUTE = 60 * 20
        const val PAUSE_MM = 2f
        const val PAUSE_TICKS = 100L // 5 s
    }
}

package com.bydesigninteractive.ant.sim.scenario

import com.bydesigninteractive.ant.sim.ant.AntState
import com.bydesigninteractive.ant.sim.ant.Space
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.fail

/** The brain-driven colony: the open-ground hesitation check (spec 5.2) and determinism. */
class BrainColonyTest {
    /** Spec 5.2: no moving surface ant pauses on open ground for more than 5 s. */
    @Test
    fun noMovingSurfaceAntPausesOnOpenGround() {
        val w = Scenarios.starter(1)
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
        val a = Scenarios.starter(4)
        val b = Scenarios.starter(4)
        repeat(3000) {
            a.step()
            b.step()
        }
        assertEquals(fingerprint(a), fingerprint(b))
    }

    private companion object {
        const val TICKS_PER_MINUTE = 60 * 20
        const val PAUSE_MM = 2f
        const val PAUSE_TICKS = 100L // 5 s
    }
}

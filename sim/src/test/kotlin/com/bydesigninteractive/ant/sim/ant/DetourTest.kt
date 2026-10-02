package com.bydesigninteractive.ant.sim.ant

import com.bydesigninteractive.ant.sim.World
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DetourTest {
    /**
     * Spec 5.3: a detour walks its own run. Before M2a it copied its run into runLeft, so a
     * detour's leftover became the next home-vector run and the ant walked on in the detour's
     * direction instead of re-aiming home.
     */
    @Test
    fun aDetourNeverTouchesTheSearchOrHomingRun() {
        val w = World(1, rocks = false) // no ants: step() only advances the clock
        w.predig(1)
        val a = Ant(0, Role.FORAGER, 0.5f, true)
        a.space = Space.SURFACE
        SurfaceWalk.place(w, a, w.surface.entranceX + 200f, w.surface.entranceY, 0f)
        a.runLeft = 123f
        var detoured = 0
        repeat(400) {
            if (Detour.detouring(w, a, 7, 100f)) detoured++ // no progress: it must stall and detour
            assertEquals(123f, a.runLeft, "runLeft changed at tick ${w.tick}")
            w.step()
        }
        assertTrue(detoured > 0, "no detour started")
        assertTrue(w.detoursStarted >= 1)
    }
}

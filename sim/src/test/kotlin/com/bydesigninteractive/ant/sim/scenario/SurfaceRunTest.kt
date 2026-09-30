package com.bydesigninteractive.ant.sim.scenario

import com.bydesigninteractive.ant.sim.World
import com.bydesigninteractive.ant.sim.ant.AntState
import com.bydesigninteractive.ant.sim.ant.Role
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SurfaceRunTest {
    @Test
    fun theSameSeedGivesTheSameStarterRun() {
        val a = Scenarios.starter(5)
        val b = Scenarios.starter(5)
        repeat(5000) {
            a.step()
            b.step()
        }
        for (i in a.ants.indices) {
            assertEquals(a.ants[i].x, b.ants[i].x)
            assertEquals(a.ants[i].y, b.ants[i].y)
            assertEquals(a.ants[i].z, b.ants[i].z)
        }
    }

    /** 1,000 searching foragers for 60 simulated seconds; reports the cost per tick. */
    @Test
    fun benchmarkOneThousandSurfaceAnts() {
        val w = World(11)
        w.surface.placePlants()
        w.surface.placePrey()
        w.predig(1)
        repeat(1000) {
            val a = w.addAnt(Role.FORAGER)
            w.exitNest(a)
            a.state = AntState.SEARCH
        }
        repeat(100) { w.step() } // warm up the JIT and the height grids
        val ticks = 60 * 20
        val start = System.nanoTime()
        repeat(ticks) { w.step() }
        val msPerTick = (System.nanoTime() - start) / 1e6 / ticks
        println("BENCHMARK 1000 surface ants: %.2f ms per tick".format(java.util.Locale.ROOT, msPerTick))
        assertTrue(msPerTick < 20.0, "$msPerTick ms per tick")
    }
}

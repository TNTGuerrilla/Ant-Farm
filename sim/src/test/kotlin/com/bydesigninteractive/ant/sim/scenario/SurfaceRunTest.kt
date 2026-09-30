package com.bydesigninteractive.ant.sim.scenario

import com.bydesigninteractive.ant.sim.World
import com.bydesigninteractive.ant.sim.ant.AntState
import com.bydesigninteractive.ant.sim.ant.Role
import com.bydesigninteractive.ant.sim.world.CHUNK_MM
import com.bydesigninteractive.ant.sim.world.FIELD_CELL_MM
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

    /** Render-style reads (trail overlay, HUD, home scent, rock meshes) must not change a run. */
    @Test
    fun renderStyleReadsDoNotChangeTheRun() {
        val a = Scenarios.starter(5)
        val b = Scenarios.starter(5)
        val s = b.surface
        val ecx = (s.entranceX / CHUNK_MM).toInt()
        val ecy = (s.entranceY / CHUNK_MM).toInt()
        val cells = CHUNK_MM / FIELD_CELL_MM
        val out = FloatArray(cells * cells)
        repeat(5000) { t ->
            a.step()
            b.step()
            if (t % 20 == 0) {
                for (cy in ecy - 1..ecy + 1) for (cx in ecx - 1..ecx + 1) {
                    s.trail.projectMax(cx * CHUNK_MM.toFloat(), cy * CHUNK_MM.toFloat(), cells, out)
                }
                s.trail.max()
                for (dx in listOf(-60f, 0f, 45f)) for (dy in listOf(-30f, 20f)) {
                    s.homeScent.peek(s.entranceX + dx, s.entranceY + dy, 0f)
                }
                for (cy in ecy - 2..ecy + 2) for (cx in ecx - 2..ecx + 2) s.sdf.ownBlobs(cx, cy)
            }
        }
        assertEquals(a.ants.size, b.ants.size)
        for (i in a.ants.indices) {
            assertEquals(a.ants[i].x, b.ants[i].x)
            assertEquals(a.ants[i].y, b.ants[i].y)
            assertEquals(a.ants[i].z, b.ants[i].z)
            assertEquals(a.ants[i].state, b.ants[i].state)
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

    /** The starter colony for 30 simulated minutes, so trails really exist; reports the cost. */
    @Test
    fun benchmarkTheStarterColonyWithTrails() {
        val w = Scenarios.starter(7)
        repeat(20 * 60 * 25) { w.step() } // 25 minutes to build trails and scent
        val ticks = 20 * 60 * 5
        val start = System.nanoTime()
        repeat(ticks) { w.step() }
        val msPerTick = (System.nanoTime() - start) / 1e6 / ticks
        val rt = Runtime.getRuntime()
        System.gc()
        val heapMb = (rt.totalMemory() - rt.freeMemory()) / 1_048_576.0
        println(
            "BENCHMARK starter with trails: %.2f ms per tick, trail blocks %d, scent blocks %d, heap %.0f MB, feeds %d".format(
                java.util.Locale.ROOT, msPerTick, w.surface.trail.blockCount(), w.surface.homeScent.blockCount(), heapMb, w.feedEvents.size,
            ),
        )
        assertTrue(msPerTick < 20.0, "$msPerTick ms per tick")
    }
}

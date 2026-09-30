package com.bydesigninteractive.ant.core.engine

import com.bydesigninteractive.ant.sim.scenario.Scenarios
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.thread
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SimRunnerTest {
    @Test
    fun aThreadedRunEqualsASingleThreadedRun() {
        val direct = Scenarios.starter(5)
        repeat(5000) { direct.step() }

        val world = Scenarios.starter(5)
        val published = Published(world)
        val runner = SimRunner(world, published, tickLimit = 5000)
        runner.send(Command.SetSpeed(1e6f)) // no sleeping between ticks
        val done = AtomicBoolean(false)
        var snapshots = 0
        val reader = thread {
            while (!done.get()) {
                if (published.snapshots.takeFresh() != null) snapshots++
                for (t in published.tiles.values) t.cells.size
                for (o in published.overlays.values) o.trail?.size
                for (r in published.rocks.values) r.size
                published.hud.tick
            }
        }
        runner.start()
        runner.join(300_000)
        done.set(true)
        reader.join()

        assertEquals(5000L, world.tick)
        assertEquals(direct.ants.size, world.ants.size)
        for (i in direct.ants.indices) {
            assertEquals(direct.ants[i].x, world.ants[i].x)
            assertEquals(direct.ants[i].y, world.ants[i].y)
            assertEquals(direct.ants[i].z, world.ants[i].z)
            assertEquals(direct.ants[i].state, world.ants[i].state)
        }
        assertTrue(snapshots > 0)
    }

    @Test
    fun pauseStopsTicking() {
        val world = Scenarios.starter(5)
        val runner = SimRunner(world, Published(world))
        runner.send(Command.SetSpeed(1e6f))
        runner.start()
        Thread.sleep(200)
        runner.send(Command.Pause)
        Thread.sleep(100)
        val at = world.tick
        Thread.sleep(200)
        assertEquals(at, world.tick)
        runner.stop()
    }
}

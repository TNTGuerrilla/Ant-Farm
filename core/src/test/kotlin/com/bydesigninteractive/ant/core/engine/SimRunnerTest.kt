package com.bydesigninteractive.ant.core.engine

import com.bydesigninteractive.ant.sim.scenario.Scenarios
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.thread
import kotlin.test.Test
import com.bydesigninteractive.ant.sim.ant.Ant
import kotlin.test.assertEquals
import kotlin.test.assertNull
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
            assertSameAnt(direct.ants[i], world.ants[i])
        }
        assertEquals(direct.surface.trail.max(), world.surface.trail.max())
        assertEquals(direct.surface.trail.blockCount(), world.surface.trail.blockCount())
        assertEquals(direct.surface.homeScent.max(), world.surface.homeScent.max())
        assertEquals(direct.surface.homeScent.blockCount(), world.surface.homeScent.blockCount())
        assertNull(published.failed)
        assertTrue(snapshots > 0)
    }

    private fun assertSameAnt(a: Ant, b: Ant) {
        val id = "ant ${a.id}"
        assertEquals(a.id, b.id, id)
        assertEquals(a.space, b.space, id)
        assertEquals(a.x, b.x, id)
        assertEquals(a.y, b.y, id)
        assertEquals(a.z, b.z, id)
        assertEquals(a.heading, b.heading, id)
        assertEquals(a.nx, b.nx, id)
        assertEquals(a.ny, b.ny, id)
        assertEquals(a.nz, b.nz, id)
        assertEquals(a.fx, b.fx, id)
        assertEquals(a.fy, b.fy, id)
        assertEquals(a.fz, b.fz, id)
        assertEquals(a.speed, b.speed, id)
        assertEquals(a.state, b.state, id)
        assertEquals(a.carriesPellet, b.carriesPellet, id)
        assertEquals(a.crop, b.crop, id)
        assertEquals(a.timer, b.timer, id)
        assertEquals(a.arrived, b.arrived, id)
        assertEquals(a.runLeft, b.runLeft, id)
        assertEquals(a.onTrail, b.onTrail, id)
        assertEquals(a.homeDx, b.homeDx, id)
        assertEquals(a.homeDy, b.homeDy, id)
        assertEquals(a.senseL, b.senseL, id)
        assertEquals(a.senseR, b.senseR, id)
        assertEquals(a.cellX, b.cellX, id)
        assertEquals(a.cellY, b.cellY, id)
        assertEquals(a.nextX, b.nextX, id)
        assertEquals(a.nextY, b.nextY, id)
        assertEquals(a.digX, b.digX, id)
        assertEquals(a.digY, b.digY, id)
        assertEquals(a.food?.x, b.food?.x, id)
        assertEquals(a.food?.y, b.food?.y, id)
        assertEquals(a.food?.z, b.food?.z, id)
    }

    @Test
    fun aStoppedRunnerLeavesFailedNull() {
        val world = Scenarios.starter(5)
        val published = Published(world)
        val runner = SimRunner(world, published)
        runner.start()
        runner.start() // must not start a second thread
        Thread.sleep(100)
        runner.stop()
        assertNull(published.failed)
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

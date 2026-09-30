package com.bydesigninteractive.ant.sim.ant

import com.bydesigninteractive.ant.sim.World
import kotlin.math.hypot
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DiggerTest {
    private fun world(diggers: Int): World {
        val w = World(11, AntParams(digSecondsPerCell = 1f))
        w.predig(1)
        repeat(diggers) { w.addAnt(Role.DIGGER) }
        return w
    }

    private fun run(w: World, minutes: Int) = repeat(minutes * 60 * 20) { w.step() }

    @Test
    fun diggersMakeRoomUntilThereIsEnough() {
        val w = world(10) // wants 250 cells, starts with 200
        run(w, 10)
        assertTrue(w.nest.airCells in 250..260, "air ${w.nest.airCells}")
    }

    @Test
    fun everyDugPelletIsCarriedOrOnTheSpoilPile() {
        val w = world(10)
        run(w, 10)
        val dug = w.nest.airCells - 200
        val carried = w.ants.count { it.carriesPellet }
        assertEquals(dug.toFloat(), w.surface.spoil.total() + carried, 1e-3f)
    }

    @Test
    fun spoilIsPiledNearTheEntrance() {
        val w = world(10)
        run(w, 10)
        val s = w.surface
        for (cy in 0 until s.spoil.chunks) for (cx in 0 until s.spoil.chunks) {
            val chunk = s.spoil.chunk(cx, cy) ?: continue
            for (i in chunk.indices) {
                if (chunk[i] <= 0f) continue
                val x = (cx * s.spoil.chunkCells + i % s.spoil.chunkCells + 0.5f) * s.spoil.cellMm
                val y = (cy * s.spoil.chunkCells + i / s.spoil.chunkCells + 0.5f) * s.spoil.cellMm
                assertTrue(hypot(x - s.entranceX, y - s.entranceY) < 60f)
            }
        }
    }

    @Test
    fun theNestFollowsThePlan() {
        val w = world(10)
        run(w, 10)
        val g = w.nest
        for (y in g.airMinY..g.airMaxY) for (x in g.airMinX..g.airMaxX) {
            if (!g.isAir(x, y)) continue
            assertTrue((0..w.excavation.block).any { w.plan.rect(it).contains(x, y) }, "stray air at ($x, $y)")
        }
    }

    @Test
    fun nothingIsDugWhenThereIsRoom() {
        val w = world(1) // wants 25 cells, has 200
        run(w, 1)
        assertEquals(200, w.nest.airCells)
    }
}

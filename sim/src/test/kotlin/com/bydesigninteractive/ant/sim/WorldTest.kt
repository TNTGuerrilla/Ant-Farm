package com.bydesigninteractive.ant.sim

import com.bydesigninteractive.ant.sim.ant.Ant
import com.bydesigninteractive.ant.sim.ant.Role
import com.bydesigninteractive.ant.sim.ant.Space
import com.bydesigninteractive.ant.sim.ant.SpatialIndex
import kotlin.math.hypot
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class WorldTest {
    @Test
    fun predigOpensTheEntranceShaft() {
        val w = World(1)
        w.predig(1)
        assertTrue(w.nest.isAir(600, 0))
        assertTrue(w.nest.isAir(600, 39))
        assertEquals(200, w.nest.airCells)
    }

    @Test
    fun antsStartAtTheEntranceInsideTheNest() {
        val w = World(1)
        w.predig(1)
        val a = w.addAnt(Role.FORAGER)
        assertEquals(Space.NEST, a.space)
        assertEquals(600, a.cellX)
        assertEquals(0, a.cellY)
    }

    @Test
    fun exitAndEnterHandTheAntBetweenSpacesKeepingItsLoad() {
        val w = World(1)
        w.predig(1)
        val a = w.addAnt(Role.DIGGER)
        a.carriesPellet = true
        w.exitNest(a)
        assertEquals(Space.SURFACE, a.space)
        val d = hypot(a.x - w.surface.entranceX, a.y - w.surface.entranceY)
        assertEquals(w.params.entranceRadius + 1f, d, 1e-3f)
        assertTrue(a.carriesPellet)
        w.enterNest(a)
        assertEquals(Space.NEST, a.space)
        assertEquals(600.5f, a.x)
        assertTrue(a.carriesPellet)
    }

    @Test
    fun digNeededFollowsRoomPerAnt() {
        val w = World(1)
        w.predig(1) // 200 cells of air
        repeat(7) { w.addAnt(Role.DIGGER) } // wants 175
        assertTrue(!w.digNeeded())
        repeat(2) { w.addAnt(Role.DIGGER) } // wants 225
        assertTrue(w.digNeeded())
    }

    @Test
    fun spatialIndexCountsNeighborsOnTheSurface() {
        val ants = List(4) { Ant(it, Role.FORAGER, 0.5f, true).apply { space = Space.SURFACE } }
        ants[0].x = 100f; ants[0].y = 100f
        ants[1].x = 105f; ants[1].y = 100f
        ants[2].x = 130f; ants[2].y = 100f
        ants[3].x = 101f; ants[3].y = 100f; ants[3].space = Space.NEST
        val index = SpatialIndex()
        index.rebuild(ants)
        assertEquals(1, index.countNear(ants[0], 10f))
    }
}

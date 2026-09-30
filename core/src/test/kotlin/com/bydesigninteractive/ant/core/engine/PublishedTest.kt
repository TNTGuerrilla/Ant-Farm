package com.bydesigninteractive.ant.core.engine

import com.bydesigninteractive.ant.sim.scenario.Scenarios
import com.bydesigninteractive.ant.sim.world.Material
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class PublishedTest {
    @Test
    fun aPublishCopiesAntsTilesAndHud() {
        val w = Scenarios.starter(5)
        val p = Published(w)
        repeat(100) {
            w.step()
            p.publish(w, it * 50_000_000L, TickStats(), TickSchedule(50_000_000L))
        }
        val s = assertNotNull(p.snapshots.takeFresh())
        assertEquals(w.tick, s.tick)
        assertEquals(w.ants.size, s.count)
        for (i in 0 until s.count) {
            assertEquals(w.ants[i].x, s.x[i])
            assertEquals(w.ants[i].z, s.z[i])
        }
        val entranceTile = (w.nestEntranceX / 64) + 0
        val tile = assertNotNull(p.tiles[entranceTile])
        assertEquals(Material.AIR.ordinal.toByte(), tile.cells[w.nestEntranceX % 64])
        assertTrue(p.hud.tick > 0)
    }

    @Test
    fun rocksArePublishedWhenGenerated() {
        val w = Scenarios.starter(5)
        val p = Published(w)
        w.surface.sdf.ownBlobs(9, 9)
        assertNotNull(p.rocks[9 + 9 * 16])
    }
}

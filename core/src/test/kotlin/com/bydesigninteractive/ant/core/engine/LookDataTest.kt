package com.bydesigninteractive.ant.core.engine

import com.bydesigninteractive.ant.sim.ant.Role
import com.bydesigninteractive.ant.sim.scenario.Scenarios
import com.bydesigninteractive.ant.sim.world.CHUNKS
import com.bydesigninteractive.ant.sim.world.CHUNK_MM
import com.bydesigninteractive.ant.sim.world.FoodKind
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class LookDataTest {
    @Test
    fun theSnapshotCarriesCropAndCarry() {
        val w = Scenarios.starter(3)
        val digger = w.ants.first { it.role == Role.DIGGER }
        digger.carriesPellet = true
        val forager = w.ants.first { it.role == Role.FORAGER }
        forager.crop = forager.desiredCrop / 2f
        forager.lastFoodKind = FoodKind.HONEYDEW
        val hunter = w.ants.last { it.role == Role.FORAGER }
        hunter.crop = 0.5f
        hunter.lastFoodKind = FoodKind.PREY
        val s = Snapshot()
        s.fill(w, 0L, emptyList())
        assertEquals(CARRY_PELLET, s.carry[digger.id].toInt())
        assertEquals(0.5f, s.crop[forager.id], 1e-6f)
        assertEquals(CARRY_NONE, s.carry[forager.id].toInt())
        assertEquals(CARRY_PREY, s.carry[hunter.id].toInt())
        assertEquals(0f, s.crop[hunter.id])
    }

    @Test
    fun posesBlendCropAndTakeCarryFromTheCurrentTick() {
        val w = Scenarios.starter(3)
        val a = w.ants.first { it.role == Role.FORAGER }
        val states = AntStates()
        val s = Snapshot()
        s.fill(w, 0L, emptyList())
        states.accept(s)
        a.crop = a.desiredCrop
        a.lastFoodKind = FoodKind.HONEYDEW
        s.fill(w, 1L, emptyList())
        states.accept(s)
        val pose = AntPose()
        states.blend(a.id, 0.5f, pose)
        assertEquals(0.5f, pose.crop, 1e-6f)
        assertEquals(CARRY_NONE, pose.carry)
    }

    @Test
    fun rocksAroundTheFocusArePublished() {
        val w = Scenarios.starter(3)
        val p = Published(w)
        p.setFocus(w.surface.entranceX + 900f, w.surface.entranceY)
        repeat(20) {
            w.step()
            p.publish(w, it.toLong(), TickStats(), TickSchedule(intervalFor(1f)))
        }
        val cx = ((w.surface.entranceX + 900f) / CHUNK_MM).toInt()
        val cy = (w.surface.entranceY / CHUNK_MM).toInt()
        for (dy in -2..2) for (dx in -2..2) assertNotNull(p.rocks[(cx + dx) + (cy + dy) * CHUNKS], "chunk ${cx + dx}, ${cy + dy}")
        assertTrue(p.rocks.size >= 25)
    }
}

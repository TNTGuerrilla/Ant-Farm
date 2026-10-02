package com.bydesigninteractive.ant.sim.brain

import com.bydesigninteractive.ant.sim.World
import com.bydesigninteractive.ant.sim.ant.Ant
import com.bydesigninteractive.ant.sim.ant.AntParams
import com.bydesigninteractive.ant.sim.ant.Role
import com.bydesigninteractive.ant.sim.ant.Space
import com.bydesigninteractive.ant.sim.ant.SurfaceWalk
import com.bydesigninteractive.ant.sim.world.FoodKind
import com.bydesigninteractive.ant.sim.world.FoodSource
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertSame
import kotlin.test.assertTrue

class SensesTest {
    private fun world(params: AntParams = AntParams()): World {
        val w = World(1, params, rocks = false)
        w.predig(1)
        w.paths.refresh()
        return w
    }

    private fun onSurface(w: World, x: Float, y: Float, heading: Float): Ant {
        val a = w.addAnt(Role.FORAGER)
        a.space = Space.SURFACE
        SurfaceWalk.place(w, a, x, y, heading)
        return a
    }

    private fun read(w: World, a: Ant) = FloatArray(Senses.COUNT).also { Senses.fill(w, a, it) }

    @Test
    fun everyInputHasOneName() {
        assertEquals(Senses.COUNT, Senses.NAMES.size)
        assertEquals(Senses.COUNT, Senses.NAMES.toSet().size)
        assertEquals(Senses.COUNT - 1, Senses.BIAS)
    }

    @Test
    fun theEntranceOnTheLeftIsAPositiveHomeBearing() {
        val w = world()
        val s = w.surface
        val a = onSurface(w, s.entranceX, s.entranceY - 40f, 0f) // facing +x, entrance toward +y
        val x = read(w, a)
        assertTrue(x[Senses.HOME_SIN] > 0.9f, "sin ${x[Senses.HOME_SIN]}")
        assertTrue(abs(x[Senses.HOME_COS]) < 0.15f, "cos ${x[Senses.HOME_COS]}")
        assertEquals(40f / 540f, x[Senses.HOME_DIST], 0.01f)
        assertEquals(0f, x[Senses.IN_NEST])
        assertEquals(1f, x[Senses.BIAS])
        assertEquals(0f, x[Senses.HEALTH])
    }

    @Test
    fun foodWithinReachIsTheTarget() {
        val w = world()
        val s = w.surface
        val a = onSurface(w, s.entranceX + 300f, s.entranceY, 0f)
        val food = FoodSource(9, FoodKind.PREY, a.x, a.y, 10f, 0.8f)
        s.addFood(food)
        val x = read(w, a)
        assertEquals(1f, x[Senses.AT_FOOD])
        assertEquals(1f, x[Senses.PREY], 1e-6f)
        assertSame(food, a.food)
    }

    @Test
    fun theCropReadsAgainstTheDesiredFill() {
        val w = world()
        val a = onSurface(w, w.surface.entranceX + 300f, w.surface.entranceY, 0f)
        a.crop = a.desiredCrop / 2f
        var x = read(w, a)
        assertEquals(0.5f, x[Senses.CROP], 1e-6f)
        assertEquals(0f, x[Senses.FULL])
        a.crop = a.desiredCrop
        x = read(w, a)
        assertEquals(1f, x[Senses.CROP], 1e-6f)
        assertEquals(1f, x[Senses.FULL])
    }

    @Test
    fun aNestAntFeelsTheReturnersAndTheDigSite() {
        val w = world(AntParams(volumePerAnt = 1000))
        val a = w.addAnt(Role.DIGGER) // in the nest, at the entrance cell
        repeat(3) { w.returners.add(w.tick) }
        val x = read(w, a)
        assertEquals(1f, x[Senses.IN_NEST])
        assertEquals(1f, x[Senses.AT_ENTRANCE])
        assertEquals(1f, x[Senses.DIGGER])
        assertEquals(3f / 8f, x[Senses.RETURNERS], 1e-6f)
        assertEquals(1f, x[Senses.DIG_SITE])
        assertEquals(1f, x[Senses.ROOM_NEEDED])
    }

    @Test
    fun theNoiseInputComesFromTheWorldsGenerator() {
        val w1 = world()
        val w2 = world()
        val a1 = onSurface(w1, 4300f, 4000f, 0f)
        val a2 = onSurface(w2, 4300f, 4000f, 0f)
        val first = read(w1, a1)[Senses.NOISE]
        assertEquals(first, read(w2, a2)[Senses.NOISE])
        assertNotEquals(first, read(w1, a1)[Senses.NOISE])
    }
}

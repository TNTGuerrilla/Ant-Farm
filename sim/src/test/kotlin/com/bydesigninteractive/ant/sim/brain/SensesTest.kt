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

    // Task 8b: the normalised trail difference, the same at any strength, shut far below the threshold.
    @Test
    fun theTrailDifferenceIsNormalisedAndGatedByTheReading() {
        val t = AntParams().trailThreshold
        val weak = Senses.trailDiff(0.6f * t, 0.4f * t, t)
        val strong = Senses.trailDiff(6f * t, 4f * t, t)
        assertEquals(0.2f, weak, 0.001f)
        assertEquals(weak, strong, 0.001f)
        assertEquals(-weak, Senses.trailDiff(0.4f * t, 0.6f * t, t), 1e-6f)
        assertEquals(0f, Senses.trailDiff(0.06f * t, 0.03f * t, t))
        assertEquals(0f, Senses.trailDiff(0f, 0f, t))
        val half = Senses.trailDiff(0.2f * t, 0f, t) // a reading of 0.2 T: the gate half open
        assertEquals(0.5f, half, 0.01f)
    }

    @Test
    fun aTrailAheadLeftReadsAsAPositiveDifference() {
        val w = world()
        val s = w.surface
        val a = onSurface(w, s.entranceX + 200f, s.entranceY, 0f) // facing +x
        var x = a.x
        while (x < a.x + 40f) {
            s.trail.add(x, a.y + 5f, s.ground.height(x, a.y + 5f), 10f) // a trail 5 mm to the ant's left
            x += 5f
        }
        val v = read(w, a)
        assertTrue(v[Senses.TRAIL_DIFF] > 0.1f, "trail difference ${v[Senses.TRAIL_DIFF]}")
        assertEquals(Senses.trailDiff(a.senseL, a.senseR, w.params.trailThreshold), v[Senses.TRAIL_DIFF])
    }

    // Task 8c: the saturated total the trail relay reads.
    @Test
    fun theTrailSumIsTheSaturatedTotal() {
        val w = world()
        val s = w.surface
        val a = onSurface(w, s.entranceX + 200f, s.entranceY, 0f)
        var x = a.x
        while (x < a.x + 40f) {
            s.trail.add(x, a.y + 5f, s.ground.height(x, a.y + 5f), 10f)
            x += 5f
        }
        val v = read(w, a)
        val sum = a.senseL + a.senseR
        assertEquals(sum / (sum + w.params.trailThreshold), v[Senses.TRAIL_SUM], 1e-6f)
    }

    // Task 8c: the trail ring near the entrance.
    @Test
    fun theRingPointsAtTheStrongestTrailNearTheEntrance() {
        val w = world()
        val s = w.surface
        val a = onSurface(w, s.entranceX, s.entranceY + 8f, 0f) // just out, facing +x
        var empty = read(w, a)
        assertEquals(0f, empty[Senses.RING])
        assertEquals(0f, empty[Senses.RING_BEARING])
        assertEquals(0f, empty[Senses.RING_COS])
        // A trail leaving the entrance toward -y (to the ant's right and behind it) and a weaker one toward +y.
        var y = s.entranceY - 10f
        while (y > s.entranceY - 200f) {
            s.trail.add(s.entranceX, y, s.ground.height(s.entranceX, y), 10f)
            y -= 10f
        }
        y = s.entranceY + 40f
        while (y < s.entranceY + 200f) {
            s.trail.add(s.entranceX + 3f, y, s.ground.height(s.entranceX + 3f, y), 2f)
            y += 10f
        }
        val v = read(w, a)
        assertEquals(-0.5f, v[Senses.RING_BEARING], 0.1f, "bearing ${v[Senses.RING_BEARING]}")
        assertTrue(abs(v[Senses.RING_COS]) < 0.4f, "bearing cos ${v[Senses.RING_COS]}")
        assertTrue(v[Senses.RING] > 0.5f && v[Senses.RING] < 1f, "strength ${v[Senses.RING]}")
        // The same trail is not sensed beyond RING_NEAR of the entrance.
        val far = onSurface(w, s.entranceX + Senses.RING_NEAR + 5f, s.entranceY - 30f, 0f)
        empty = read(w, far)
        assertEquals(0f, empty[Senses.RING])
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

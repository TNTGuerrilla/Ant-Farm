package com.bydesigninteractive.ant.sim.ant

import com.bydesigninteractive.ant.sim.World
import com.bydesigninteractive.ant.sim.world.FoodKind
import com.bydesigninteractive.ant.sim.world.FoodSource
import kotlin.math.hypot
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ForagerTest {
    private fun world(seed: Long, quality: Float, foragers: Int): World {
        val w = World(seed)
        w.predig(3)
        w.surface.addFood(FoodSource(0, FoodKind.HONEYDEW, w.surface.entranceX + 150f, w.surface.entranceY, 15f, quality))
        repeat(foragers) { w.addAnt(Role.FORAGER) }
        return w
    }

    private fun run(w: World, minutes: Int) = repeat(minutes * 60 * 20) { w.step() }

    @Test
    fun foragersFindFoodAndBringItHome() {
        val w = world(5, 1f, 10)
        run(w, 20)
        assertTrue(w.feedEvents.size >= 5, "feeds ${w.feedEvents.size}")
        assertTrue(w.unloads >= 3, "unloads ${w.unloads}")
    }

    @Test
    fun onlyAntsThatFilledUpLayTrail() {
        val w = world(5, 0f, 10) // nobody can fill to their desired volume
        run(w, 20)
        assertTrue(w.feedEvents.isNotEmpty())
        assertEquals(0f, w.surface.trail.max())
    }

    @Test
    fun fullForagersLayATrail() {
        val w = world(5, 1f, 10)
        run(w, 20)
        assertTrue(w.surface.trail.max() > 0f)
    }

    // Task 8c: the trail choice at the nest exit is the brain's (the trail ring and the seed
    // brain's exit circuit); Task 8b's innate exit primitive is gone. With no trail the way out
    // stays random; with a trail on one side most foragers head out along it.
    @Test
    fun emergingForagersHeadForTheTrailSideAtTheExit() {
        fun count(trail: Boolean): Pair<Int, Int> {
            val w = World(7, rocks = false)
            w.predig(1)
            val s = w.surface
            if (trail) { // a strong trail (about 5 T) on the +x side of the entrance only
                var x = s.entranceX + 10f
                while (x < s.entranceX + 200f) {
                    s.trail.add(x, s.entranceY, s.ground.height(x, s.entranceY), 10f)
                    x += 10f
                }
            }
            val ants = List(200) { w.addAnt(Role.FORAGER) }
            for (a in ants) w.exitNest(a)
            repeat(60) { w.step() } // 3 s, about 60 mm of walking
            var plus = 0
            var minus = 0
            for (a in ants) {
                if (a.space != Space.SURFACE) continue
                val dx = a.x - s.entranceX
                val d = hypot(dx, a.y - s.entranceY)
                if (d < 1f) continue
                if (dx / d > 0.5f) plus++
                if (dx / d < -0.5f) minus++
            }
            println("exit choice, trail $trail: toward +x $plus, toward -x $minus")
            return plus to minus
        }
        val (bareP, bareM) = count(false)
        assertTrue(bareP in 40..95 && bareM in 40..95, "no trail: toward +x $bareP, toward -x $bareM")
        val (plus, minus) = count(true)
        assertTrue(plus > 120 && plus > 4 * minus, "with a trail toward +x: toward +x $plus, toward -x $minus")
    }

    @Test
    fun pathIntegrationErrorGrowsWithDistance() {
        val w = World(3)
        w.predig(1)
        fun meanError(steps: Int): Float {
            var sum = 0f
            repeat(200) {
                val a = w.addAnt(Role.FORAGER)
                w.exitNest(a)
                SurfaceWalk.faceCompass(a, 0f)
                repeat(steps) { SurfaceWalk.step(w, a, 20f) }
                sum += hypot(a.homeDx - (a.x - w.surface.entranceX), a.homeDy - (a.y - w.surface.entranceY))
            }
            return sum / 200
        }
        assertTrue(meanError(2000) > meanError(100) * 2)
    }

    @Test
    fun theSameSeedGivesTheSameRun() {
        val a = world(9, 1f, 30)
        val b = world(9, 1f, 30)
        repeat(5000) {
            a.step()
            b.step()
        }
        for (i in a.ants.indices) {
            assertEquals(a.ants[i].x, b.ants[i].x)
            assertEquals(a.ants[i].y, b.ants[i].y)
            assertEquals(a.ants[i].z, b.ants[i].z)
            assertEquals(a.ants[i].state, b.ants[i].state)
        }
    }
}

package com.bydesigninteractive.ant.sim.ant

import com.bydesigninteractive.ant.sim.World
import com.bydesigninteractive.ant.sim.world.FoodKind
import com.bydesigninteractive.ant.sim.world.FoodSource
import kotlin.math.max
import kotlin.test.Test
import kotlin.test.assertTrue

class StemTest {
    private fun world(): World {
        val w = World(7, rocks = false)
        w.predig(3)
        return w
    }

    private fun plant(w: World, dx: Float, tall: Float): FoodSource {
        val s = w.surface
        val x = s.entranceX + dx
        val y = s.entranceY
        val g = s.ground.height(x, y)
        val f = FoodSource(
            s.foods.size, FoodKind.HONEYDEW, x, y, 12f, 1f,
            z = g + tall - 30f, bodyRadius = 8f, stemRadius = 2.5f, stemBase = g - 5f,
        )
        s.addFood(f)
        return f
    }

    @Test
    fun foragersClimbAStemFeedAtTheAphidsAndBringItHome() {
        val w = world()
        val f = plant(w, 100f, 500f)
        repeat(5) { w.addAnt(Role.FORAGER) }
        var highest = -1e9f
        repeat(20 * 60 * 15) {
            w.step()
            for (a in w.ants) if (a.space == Space.SURFACE) highest = max(highest, a.z)
        }
        assertTrue(w.feedEvents.any { it.foodId == f.id }, "no feeds at the plant")
        assertTrue(highest > f.z - 20f, "highest climb ${highest - f.z} below the aphids")
        assertTrue(w.unloads >= 1, "nobody brought honeydew home")
    }

    @Test
    fun aTrailLeadsForagersToAPlantBeyondItsOdour() {
        fun feeds(withTrail: Boolean): Int {
            val w = world()
            val f = plant(w, 600f, 300f)
            val s = w.surface
            if (withTrail) {
                var x = s.entranceX + 10f
                while (x < f.x - 3f) {
                    s.trail.add(x, s.entranceY, s.ground.height(x, s.entranceY), 10f)
                    x += 5f
                }
                var z = f.stemBase + 5f
                while (z < f.z) {
                    s.trail.add(f.x - 2.5f, f.y, z, 10f)
                    z += 5f
                }
            }
            repeat(20) { w.addAnt(Role.FORAGER) }
            repeat(20 * 60 * 15) { w.step() }
            return w.feedEvents.count { it.foodId == f.id }
        }
        val with = feeds(true)
        val without = feeds(false)
        assertTrue(with >= 5 && with > without, "feeds with a trail $with, without $without")
    }
}

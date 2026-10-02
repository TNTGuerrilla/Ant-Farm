package com.bydesigninteractive.ant.sim.brain

import com.bydesigninteractive.ant.sim.World
import com.bydesigninteractive.ant.sim.ant.Ant
import com.bydesigninteractive.ant.sim.ant.Role
import com.bydesigninteractive.ant.sim.ant.Space
import com.bydesigninteractive.ant.sim.ant.SurfaceWalk
import com.bydesigninteractive.ant.sim.world.FoodKind
import kotlin.math.PI
import kotlin.math.exp
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ContactsTest {
    // A world with no ants of its own: step() only advances the clock, and the index is filled by hand.
    private val w = World(1, rocks = false).also { it.predig(1) }

    private fun ant(id: Int, x: Float, y: Float, heading: Float): Ant {
        val a = Ant(id, Role.FORAGER, 0.5f, true)
        a.space = Space.SURFACE
        SurfaceWalk.place(w, a, x, y, heading)
        return a
    }

    @Test
    fun touchingANestmateCountsOnceUntilTheyPart() {
        val a = ant(0, 4000f, 4100f, 0f)
        val b = ant(1, 4002f, 4100f, PI.toFloat())
        b.crop = 0.8f
        b.lastFoodKind = FoodKind.HONEYDEW
        val both = listOf(a, b)
        w.surfaceIndex.rebuild(both)
        Contacts.update(w, a)
        assertEquals(1f, a.contactRate, 1e-6f)
        assertEquals(1, a.lastContactId)
        assertTrue(a.metSuccess)
        assertTrue(a.metHoneydew)
        assertFalse(a.metPrey)
        Contacts.update(w, a)
        assertEquals(1f, a.contactRate, 1e-6f) // still the same touch
        SurfaceWalk.place(w, b, 4100f, 4100f, PI.toFloat())
        w.surfaceIndex.rebuild(both)
        Contacts.update(w, a)
        assertEquals(-1, a.lastContactId)
    }

    @Test
    fun theContactCountFadesOverTenSeconds() {
        val a = ant(0, 4000f, 4100f, 0f)
        val b = ant(1, 4002f, 4100f, PI.toFloat())
        w.surfaceIndex.rebuild(listOf(a, b))
        Contacts.update(w, a)
        SurfaceWalk.place(w, b, 4100f, 4100f, PI.toFloat())
        repeat(200) { w.step() } // 10 s
        w.surfaceIndex.rebuild(listOf(a, b))
        Contacts.update(w, a)
        assertEquals(exp(-1f), a.contactRate, 1e-3f)
        assertEquals(10f, a.metSeconds, 1e-3f)
    }

    @Test
    fun anOncomingTiredForagerNudgesAwayFromItsDeadEnd() {
        val a = ant(0, 4000f, 4100f, 0f)
        val b = ant(1, 4002f, 4101f, (PI - 0.6).toFloat()) // heading back and to the left of a: it came from a's right
        b.reserves = 0.5f
        w.surfaceIndex.rebuild(listOf(a, b))
        Contacts.update(w, a)
        assertEquals(Contacts.NUDGE_SECONDS, a.nudgeLeft)
        assertTrue(a.nudge < -0.3f, "nudge ${a.nudge}")
    }

    @Test
    fun nestAntsTouchInTheNestIndex() {
        val a = Ant(0, Role.FORAGER, 0.5f, true)
        val b = Ant(1, Role.FORAGER, 0.5f, true)
        a.x = 600.5f
        a.y = 10.5f
        b.x = 601.5f
        b.y = 11.5f
        w.nestIndex.rebuild(listOf(a, b))
        Contacts.update(w, a)
        assertEquals(1, a.lastContactId)
    }
}

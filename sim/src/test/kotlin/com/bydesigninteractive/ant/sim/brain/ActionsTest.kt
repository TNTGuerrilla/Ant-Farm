package com.bydesigninteractive.ant.sim.brain

import com.bydesigninteractive.ant.sim.World
import com.bydesigninteractive.ant.sim.ant.Ant
import com.bydesigninteractive.ant.sim.ant.AntParams
import com.bydesigninteractive.ant.sim.ant.Role
import com.bydesigninteractive.ant.sim.ant.Space
import com.bydesigninteractive.ant.sim.ant.SurfaceWalk
import com.bydesigninteractive.ant.sim.world.FoodKind
import com.bydesigninteractive.ant.sim.world.FoodSource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ActionsTest {
    /** A world that wants far more room than it has, so diggers have a dig site. */
    private fun world(volumePerAnt: Int = 1000): World {
        val w = World(1, AntParams(volumePerAnt = volumePerAnt), rocks = false)
        w.predig(1)
        w.paths.refresh()
        return w
    }

    private fun inputs(w: World, a: Ant) = FloatArray(Senses.COUNT).also { Senses.fill(w, a, it) }

    private fun allowed(w: World, a: Ant): Set<Action> {
        val m = BooleanArray(Action.COUNT)
        Actions.mask(w, a, inputs(w, a), m)
        return Action.ALL.filter { m[it.ordinal] }.toSet()
    }

    private fun onSurface(w: World, role: Role, dx: Float): Ant {
        val a = w.addAnt(role)
        a.space = Space.SURFACE
        SurfaceWalk.place(w, a, w.surface.entranceX + dx, w.surface.entranceY, 0f)
        return a
    }

    @Test
    fun thereIsOneLogitPerAction() {
        assertEquals(Outputs.ACTION + Action.COUNT, Outputs.COUNT)
    }

    @Test
    fun anEmptyForagerAtFoodMayFeed() {
        val w = world()
        val a = onSurface(w, Role.FORAGER, 300f)
        w.surface.addFood(FoodSource(9, FoodKind.HONEYDEW, a.x, a.y, 10f, 1f))
        assertEquals(setOf(Action.WALK, Action.FEED), allowed(w, a))
        a.crop = 0.5f
        assertEquals(setOf(Action.WALK), allowed(w, a))
    }

    @Test
    fun aDiggerNeverFeeds() {
        val w = world()
        val a = onSurface(w, Role.DIGGER, 300f)
        w.surface.addFood(FoodSource(9, FoodKind.HONEYDEW, a.x, a.y, 10f, 1f))
        assertEquals(setOf(Action.WALK), allowed(w, a))
    }

    @Test
    fun atTheEntranceAnAntMayEnterButNotWithAPellet() {
        val w = world()
        val a = onSurface(w, Role.DIGGER, 2f)
        assertEquals(setOf(Action.WALK, Action.ENTER), allowed(w, a))
        a.carriesPellet = true
        assertEquals(setOf(Action.WALK), allowed(w, a)) // and too close to drop it
    }

    @Test
    fun aPelletIsDroppedOnlyClearOfTheEntrance() {
        val w = world()
        val a = onSurface(w, Role.DIGGER, 20f)
        a.carriesPellet = true
        assertEquals(setOf(Action.WALK, Action.DROP), allowed(w, a))
    }

    @Test
    fun aDiggerOnSpoilMayPickUpAPellet() {
        val w = world()
        val a = onSurface(w, Role.DIGGER, 20f)
        w.surface.spoil.add(a.x, a.y, 2f)
        assertEquals(setOf(Action.WALK, Action.PICK_UP), allowed(w, a))
    }

    @Test
    fun inTheNestAForagerUnloadsBeforeItLeaves() {
        val w = world()
        val a = w.addAnt(Role.FORAGER)
        a.crop = 0.5f
        assertEquals(setOf(Action.WALK, Action.UNLOAD, Action.REST), allowed(w, a))
        a.crop = 0f
        assertEquals(setOf(Action.WALK, Action.LEAVE, Action.REST), allowed(w, a))
    }

    @Test
    fun aDiggerDigsWhereThereIsASiteAndLeavesOnlyWithAPellet() {
        val w = world()
        val a = w.addAnt(Role.DIGGER)
        assertEquals(setOf(Action.WALK, Action.DIG, Action.REST), allowed(w, a))
        a.carriesPellet = true
        assertEquals(setOf(Action.WALK, Action.LEAVE), allowed(w, a))
        val roomy = world(volumePerAnt = 1)
        assertEquals(setOf(Action.WALK, Action.REST), allowed(roomy, roomy.addAnt(Role.DIGGER)))
    }

    @Test
    fun maskedActionsAreNeverChosen() {
        val w = world()
        val a = w.addAnt(Role.FORAGER)
        a.crop = 0.5f
        a.goOut = 1f
        val x = inputs(w, a)
        val o = FloatArray(Outputs.COUNT) // every logit equal
        repeat(500) {
            a.action = Action.REST
            Actions.choose(w, a, x, o)
            assertTrue(a.action == Action.REST || a.action == Action.UNLOAD, "chose ${a.action}")
        }
    }

    @Test
    fun theGoOutDriveScalesTheChanceToLeave() {
        val w = world()
        val a = w.addAnt(Role.FORAGER)
        val x = inputs(w, a)
        val o = FloatArray(Outputs.COUNT)
        a.goOut = 1f
        val high = w.actionWeights.let { Actions.weights(w, a, x, o); it[Action.LEAVE.ordinal] / it.sum() }
        a.goOut = 0.1f
        val low = w.actionWeights.let { Actions.weights(w, a, x, o); it[Action.LEAVE.ordinal] / it.sum() }
        assertTrue(low < high / 5f, "leave share $low vs $high")
    }

    @Test
    fun feedingIsATimedPrimitiveTheBrainCannotInterrupt() {
        val w = world()
        val a = onSurface(w, Role.FORAGER, 300f)
        w.surface.addFood(FoodSource(9, FoodKind.HONEYDEW, a.x, a.y, 10f, 1f))
        val x = inputs(w, a)
        val o = FloatArray(Outputs.COUNT)
        o[Outputs.ACTION + Action.FEED.ordinal] = 50f
        Actions.choose(w, a, x, o)
        assertEquals(Action.FEED, a.action)
        assertTrue(Actions.busy(a))
        assertEquals(w.params.feedSeconds, a.timer)
        repeat((w.params.feedSeconds / com.bydesigninteractive.ant.sim.DT).toInt() + 1) { Actions.run(w, a) }
        assertEquals(Action.WALK, a.action)
        assertEquals(1f, a.crop)
        assertEquals(1, w.feedEvents.size)
    }
}

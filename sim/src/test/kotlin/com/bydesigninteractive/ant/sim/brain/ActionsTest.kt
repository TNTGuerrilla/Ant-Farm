package com.bydesigninteractive.ant.sim.brain

import com.bydesigninteractive.ant.sim.DT
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
import kotlin.test.assertSame
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

    /** Sensing never swaps or drops the source an ant is feeding at; the feed is recorded at the source it chose. */
    @Test
    fun theFeedingSourceStaysPutThroughAFeed() {
        val w = world()
        val a = onSurface(w, Role.FORAGER, 300f)
        val chosen = FoodSource(9, FoodKind.HONEYDEW, a.x + 4f, a.y, 10f, 0.6f)
        w.surface.addFood(chosen)
        inputs(w, a)
        assertSame(chosen, a.food)
        Actions.begin(w, a, Action.FEED)
        assertTrue(Actions.busy(a))
        // A second source appears right under the ant, nearer than the chosen one.
        val other = FoodSource(10, FoodKind.PREY, a.x, a.y, 10f, 1f)
        w.surface.addFood(other)
        val ticks = (w.params.feedSeconds / DT).toInt() + 1
        repeat(ticks) {
            if (a.action == Action.FEED) {
                inputs(w, a)
                assertSame(chosen, a.food)
            }
            Actions.run(w, a)
        }
        assertEquals(Action.WALK, a.action)
        assertEquals(0.6f, a.crop)
        assertEquals(chosen.id, w.feedEvents.last().foodId)
        inputs(w, a)
        assertSame(other, a.food) // free to pick a source again once the feed is over
    }

    // Task 11a: feeding places. With every place taken a forager cannot feed and is turned away
    // (Senses.NO_ROOM) until it is back in the nest; a place is free again when a feed ends.
    @Test
    fun aForagerAtAFullSourceCannotFeedAndIsTurnedAway() {
        val w = world()
        val first = onSurface(w, Role.FORAGER, 300f)
        val food = FoodSource(9, FoodKind.HONEYDEW, first.x, first.y, 10f, 1f, bodyRadius = 0.3f)
        assertEquals(1, food.capacity)
        w.surface.addFood(food)
        inputs(w, first)
        Actions.begin(w, first, Action.FEED)
        assertEquals(1, food.feeders)
        assertTrue(food.full)
        val late = onSurface(w, Role.FORAGER, 300f)
        val x = inputs(w, late)
        assertEquals(setOf(Action.WALK), allowed(w, late))
        assertTrue(late.turnedAway)
        assertEquals(1f, x[Senses.NO_ROOM])
        repeat((w.params.feedSeconds / DT).toInt() + 1) { Actions.run(w, first) }
        assertEquals(0, food.feeders)
        assertEquals(setOf(Action.WALK, Action.FEED), allowed(w, late))
        assertTrue(late.turnedAway, "turned away for the rest of the trip")
        w.enterNest(late)
        assertEquals(0f, inputs(w, late)[Senses.NO_ROOM])
        assertTrue(!late.turnedAway)
    }

    // Task 11a: taking a source's last load removes it, and everyone else feeding there stops
    // empty-handed and frees its place; the place where it was turns empty foragers away.
    @Test
    fun theLastLoadEndsEveryOtherFeedAtTheSource() {
        val w = world()
        val a = onSurface(w, Role.FORAGER, 300f)
        val b = onSurface(w, Role.FORAGER, 300f)
        val food = FoodSource(9, FoodKind.PREY, a.x, a.y, 10f, 0.8f, loads = 1, bodyRadius = 3f)
        w.surface.addFood(food)
        inputs(w, a)
        Actions.begin(w, a, Action.FEED)
        repeat(20) { Actions.run(w, a) } // a is 1 s ahead of b
        inputs(w, b)
        Actions.begin(w, b, Action.FEED)
        assertEquals(2, food.feeders)
        repeat((w.params.feedSeconds / DT).toInt()) {
            Actions.run(w, a)
            Actions.run(w, b)
        }
        assertEquals(0.8f, a.crop)
        assertEquals(0f, b.crop)
        assertEquals(Action.WALK, b.action)
        assertEquals(null, b.food)
        assertEquals(0, food.feeders)
        assertTrue(w.surface.foods.none { it === food })
        assertEquals(1, w.feedEvents.count { it.foodId == 9 })
        // Where it was, nothing is left: an empty forager there is turned away.
        val late = onSurface(w, Role.FORAGER, 300f)
        assertEquals(1f, inputs(w, late)[Senses.NO_ROOM])
        assertTrue(late.turnedAway)
        assertEquals(listOf(food), w.surface.deadEnds)
        // Only for a while: once its trail has faded the place is ordinary ground again.
        w.surface.forgetDeadEnds(food.emptiedTick + 1)
        assertTrue(w.surface.deadEnds.isEmpty())
        val later = onSurface(w, Role.FORAGER, 300f)
        assertEquals(0f, inputs(w, later)[Senses.NO_ROOM])
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

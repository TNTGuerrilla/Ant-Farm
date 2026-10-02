package com.bydesigninteractive.ant.sim.ant

import com.bydesigninteractive.ant.sim.FeedEvent
import com.bydesigninteractive.ant.sim.World
import com.bydesigninteractive.ant.sim.world.FoodSource
import com.bydesigninteractive.ant.sim.world.Material
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.sqrt

/**
 * The innate primitives (spec 2.4): what an ant's body does once a decision is made. Extracted
 * from the scripted forager and digger in M2a so the brain-driven ant runs exactly the same code.
 */
internal object Primitives {
    /** Steeper than this (|nz| below it) is still a stem's wall, not the ground at its base. */
    const val STEM_WALL_NZ = 0.5f

    fun startFeeding(w: World, a: Ant, food: FoodSource) {
        a.food = food
        a.onTrail = false
        a.speed = 0f
        a.timer = w.params.feedSeconds
    }

    /** Ends a feed: the crop fills to the source's quality, the event is recorded and a load is used up. */
    fun finishFeeding(w: World, a: Ant) {
        val food = a.food
        if (food != null) {
            a.crop = food.quality
            a.lastFoodKind = food.kind
            w.feedEvents += FeedEvent(w.tick, food.id)
            if (food.loads != Int.MAX_VALUE) {
                food.loads--
                if (food.loads <= 0) w.surface.removeFood(food)
            }
        }
        a.food = null
    }

    /** True if [plant]'s aphid cluster is within its feeding reach of the ant. */
    fun atCluster(a: Ant, plant: FoodSource): Boolean {
        val dx = plant.x - a.x
        val dy = plant.y - a.y
        val dz = plant.z - a.z
        return dx * dx + dy * dy + dz * dz <= plant.radius * plant.radius
    }

    /**
     * Faces up a stem the ant touches. Up alone is ill defined on the sloping ground at the stem
     * base (it can point away from the stem), so it presses toward the axis as well; on the stem
     * wall that part is removed by the tangent projection and only the climb remains.
     */
    fun faceUpStem(a: Ant, plant: FoodSource) {
        val ax = plant.x - a.x
        val ay = plant.y - a.y
        val ah = sqrt(ax * ax + ay * ay)
        if (ah > 1e-3f) SurfaceWalk.faceDirection(a, ax / ah, ay / ah, 1f)
        else SurfaceWalk.faceDirection(a, 0f, 0f, 1f)
    }

    /** Still on a plant: touching a stem above the leave height, or on its steep base. */
    fun onPlant(w: World, a: Ant): Boolean {
        val p = w.params
        val s = w.surface
        return s.sdf.stemAt(a.x, a.y, a.z, p.stemTouch) != null &&
            (a.z > s.ground.height(a.x, a.y) + p.stemLeaveHeight || a.nz < STEM_WALL_NZ)
    }

    /** Lays one trail mark of [amount] where the ant stands, and counts it. */
    fun deposit(w: World, a: Ant, amount: Float) {
        w.surface.trail.add(a.x, a.y, a.z, amount)
        w.trailMarks++
        a.marksLaid++
    }

    /**
     * Starts digging a frontier cell next to the ant, chosen at random: sets the cell, the time
     * it takes (longer in clay) and the heading. False if no frontier cell is next to the ant
     * (the map is a tick behind the frontier; it catches up next tick).
     */
    fun startDigging(w: World, a: Ant): Boolean {
        val next = w.excavation.frontier().filter { abs(it.x - a.cellX) + abs(it.y - a.cellY) == 1 }
        if (next.isEmpty()) return false
        val target = next[w.rng.nextInt(next.size)]
        a.digX = target.x
        a.digY = target.y
        val clay = w.nest.material(target.x, target.y) == Material.CLAY
        a.timer = w.params.digSecondsPerCell * (if (clay) w.params.clayFactor else 1f)
        a.heading = atan2((target.y - a.cellY).toFloat(), (target.x - a.cellX).toFloat())
        return true
    }

    /** Ends a dig: the cell becomes air and the ant holds the pellet. False if someone else dug it first. */
    fun finishDigging(w: World, a: Ant): Boolean {
        if (!w.nest.material(a.digX, a.digY).diggable) return false
        w.dig(a.digX, a.digY)
        a.carriesPellet = true
        return true
    }

    fun dropPellet(w: World, a: Ant) {
        w.surface.spoil.add(a.x, a.y, 1f)
        a.carriesPellet = false
    }

    fun pickUpPellet(w: World, a: Ant) {
        w.surface.spoil.add(a.x, a.y, -1f)
        a.carriesPellet = true
    }
}

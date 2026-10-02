package com.bydesigninteractive.ant.sim.ant

import com.bydesigninteractive.ant.sim.brain.Action
import com.bydesigninteractive.ant.sim.brain.Body
import com.bydesigninteractive.ant.sim.brain.Brain
import com.bydesigninteractive.ant.sim.world.FoodKind
import com.bydesigninteractive.ant.sim.world.FoodSource

enum class Role { FORAGER, DIGGER }

enum class Space { SURFACE, NEST }

enum class AntState { IDLE, UNLOAD, EXIT, SEARCH, FEED, RETURN, GO_DIG, DIG, CARRY_OUT, DUMP, GO_HOME }

/**
 * One worker. Positions are millimeters: on the 3D surface in [Space.SURFACE] (x, y on the map,
 * z up), and on the nest slice (y = depth) in [Space.NEST], where the ant also walks cell to cell.
 *
 * @param desiredCrop how full the ant wants its crop before it will lay trail (reference section 5).
 * @param laysTrail false for the ants that never lay trail.
 */
class Ant(val id: Int, val role: Role, val desiredCrop: Float, val laysTrail: Boolean) {
    var space = Space.NEST
    var x = 0f
    var y = 0f
    var heading = 0f

    // Surface pose: height, unit normal (the ant's up) and unit forward in the tangent plane.
    // For a surface ant, heading is the compass angle of the forward vector.
    var z = 0f
    var nx = 0f
    var ny = 0f
    var nz = 1f
    var fx = 1f
    var fy = 0f
    var fz = 0f

    /** Current speed in mm/s; zero while standing. For renderers and the chase camera. */
    var speed = 0f
    var state = AntState.IDLE
    var carriesPellet = false

    /** How full the crop is with liquid food, 0 to 1. */
    var crop = 0f
    var timer = 0f
    var arrived = false
    var runLeft = 0f
    var onTrail = false

    /** Path integration: the ant's own noisy estimate of its displacement from the entrance. */
    var homeDx = 0f
    var homeDy = 0f
    var senseL = 0f
    var senseR = 0f

    // Obstacle detours ([Detour]): the target being tracked, the tick it was last tracked, the
    // closest horizontal distance to it so far, the time since that last improved, and how much
    // of the current detour is left to walk (mm).
    var detourKey = Detour.NONE
    var detourTick = -1L
    var detourMark = 0f
    var detourTimer = 0f
    var detourLeft = 0f

    /** Consecutive detours for the same target, and the closest distance when the first began. */
    var detourCount = 0
    var detourBase = 0f

    // Nest walking: the cell the ant stands on and the one it is heading for.
    var cellX = 0
    var cellY = 0
    var nextX = 0
    var nextY = 0
    var digX = 0
    var digY = 0
    var food: FoodSource? = null

    /** The kind of the last food this ant fed at: for the renderer, and for contacts while the crop is full. */
    var lastFoodKind: FoodKind? = null

    /** Trail marks this ant has laid. */
    var marksLaid = 0

    // Brain state (M2a), read by Senses and written by Body and Contacts.
    /** Energy reserves, 0 to 1: fall outside, refill in the nest and on feeding ([Body]). */
    var reserves = 1f

    /** The tick this ant's age counts from; age is `(tick - bornTick) * DT` ([Body.age]). Set it when the ant is spawned. */
    var bornTick = 0L

    /** The tick this ant last fed at a source, or [Body.NEVER_TICK] ([Body.sinceFed]). */
    var fedTick = Body.NEVER_TICK

    // Contacts: the count decaying over 10 s and the tick it was last decayed, the last nestmate
    // touched (-1 when touching none), what that nestmate carried, how long ago, and the nudge.
    var contactRate = 0f
    var contactTick = 0L
    var lastContactId = -1
    var metSuccess = false
    var metHoneydew = false
    var metPrey = false
    var metTick = Body.NEVER_TICK
    var nudge = 0f
    var nudgeLeft = 0f

    /** The brain's current action (M2a), and its last read outputs: turn rate (rad/s, left positive), speed factor, deposit strength and go-out drive. */
    var action = Action.WALK
    var turnRate = 0f
    var speedOut = 1f
    var deposit = 0f
    var goOut = 0f

    /** This ant's network (its colony's genome with its personal variation) and its hidden state; null only for ants made outside World.addAnt. */
    var brain: Brain? = null
    var hidden = FloatArray(0)
}

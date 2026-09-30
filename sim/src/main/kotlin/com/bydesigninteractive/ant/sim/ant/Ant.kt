package com.bydesigninteractive.ant.sim.ant

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

    // Nest walking: the cell the ant stands on and the one it is heading for.
    var cellX = 0
    var cellY = 0
    var nextX = 0
    var nextY = 0
    var digX = 0
    var digY = 0
    var food: FoodSource? = null
}

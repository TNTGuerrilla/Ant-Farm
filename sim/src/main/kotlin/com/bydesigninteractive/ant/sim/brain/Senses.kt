package com.bydesigninteractive.ant.sim.brain

import com.bydesigninteractive.ant.sim.World
import com.bydesigninteractive.ant.sim.ant.Ant
import com.bydesigninteractive.ant.sim.ant.Role
import com.bydesigninteractive.ant.sim.ant.Space
import com.bydesigninteractive.ant.sim.ant.SurfaceWalk
import com.bydesigninteractive.ant.sim.world.DayClock
import com.bydesigninteractive.ant.sim.world.DistanceMap
import com.bydesigninteractive.ant.sim.world.FoodKind
import com.bydesigninteractive.ant.sim.world.FoodSource
import com.bydesigninteractive.ant.sim.world.GroundTemperature
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/**
 * The brain's input vector (spec 2.1): senses, body state, the colony's shared channels and
 * context flags, each scaled to about -1..1 (the table in the M2a plan, Task 4). [fill] writes
 * every value and draws exactly one number (the noise input) from the world's random generator.
 * On the surface it also sets the ant's food target ([Ant.food]) to the source within reach, except
 * while the ant is feeding, when the source it chose stays its target.
 * Changing the order or meaning of any input must change [LAYOUT], which retires saved genomes.
 *
 * Two readings of the spec: the home-scent level of spec 2.1 is carried by [FOOT_L] and [FOOT_R]
 * (their mean is the level), so it has no input of its own; and [RETURNERS] is the colony-wide
 * count given to every nest ant, a simplification of "felt by contact in the nest".
 */
internal object Senses {
    /** m2a-2: the seed brain grew to 19 hidden units and personal variation became per unit, which retires m2a-1 genomes. */
    const val LAYOUT = "m2a-2"

    const val TRAIL_L = 0
    const val TRAIL_R = 1
    const val FOOT_L = 2
    const val FOOT_R = 3
    const val HOME_SIN = 4
    const val HOME_COS = 5
    const val HOME_DIST = 6
    const val HONEYDEW = 7
    const val HONEYDEW_SIN = 8
    const val PREY = 9
    const val PREY_SIN = 10
    const val CROP = 11
    const val FULL = 12
    const val RESERVES = 13
    const val AGE = 14
    const val TEMPERATURE = 15
    const val HEALTH = 16
    const val DAMAGE = 17
    const val CONTACT_RATE = 18
    const val RETURNERS = 19
    const val MET_SUCCESS = 20
    const val MET_HONEYDEW = 21
    const val MET_PREY = 22
    const val NUDGE = 23
    const val FED_RECENT = 24
    const val SPOIL = 25
    const val ROOM_NEEDED = 26
    const val DIGGER = 27
    const val ON_STEM = 28
    const val AT_FOOD = 29
    const val IN_NEST = 30
    const val CARRYING = 31
    const val DIG_SITE = 32
    const val AT_ENTRANCE = 33
    const val NOISE = 34
    const val BIAS = 35
    const val COUNT = 36

    val NAMES = arrayOf(
        "trailL", "trailR", "footL", "footR", "homeSin", "homeCos", "homeDist",
        "honeydew", "honeydewSin", "prey", "preySin",
        "crop", "full", "reserves", "age", "temperature", "health", "damage",
        "contactRate", "returners", "metSuccess", "metHoneydew", "metPrey", "nudge",
        "fedRecent", "spoil", "roomNeeded", "digger",
        "onStem", "atFood", "inNest", "carrying", "digSite", "atEntrance",
        "noise", "bias",
    )

    const val FOOT_HALF = 0.5f
    const val HOME_HALF = 500f
    const val HOME_VECTOR_DONE = 15f
    const val AGE_HALF = 3600f
    const val TEMP_MID = 20f
    const val TEMP_SPAN = 10f
    const val CONTACT_HALF = 5f
    const val RETURNER_HALF = 5f
    const val FED_TAU = 60f
    const val SPOIL_HALF = 5f
    const val ROOM_GAIN = 4f

    fun fill(w: World, a: Ant, x: FloatArray) {
        x.fill(0f)
        if (a.space == Space.SURFACE) surface(w, a, x) else nest(w, a, x)
        x[CROP] = if (a.desiredCrop > 0f) min(1f, a.crop / a.desiredCrop) else 0f
        x[FULL] = flag(a.crop > 0f && a.crop >= a.desiredCrop)
        x[RESERVES] = a.reserves
        val age = Body.age(w.tick, a)
        x[AGE] = age / (age + AGE_HALF)
        val t = DayClock.timeOfDay(w.seconds)
        val celsius = if (a.space == Space.SURFACE) GroundTemperature.surface(t) else GroundTemperature.nest(t)
        x[TEMPERATURE] = (celsius - TEMP_MID) / TEMP_SPAN
        x[CONTACT_RATE] = a.contactRate / (a.contactRate + CONTACT_HALF)
        if (Body.metSeconds(w.tick, a) < Contacts.MET_SECONDS) {
            x[MET_SUCCESS] = flag(a.metSuccess)
            x[MET_HONEYDEW] = flag(a.metHoneydew)
            x[MET_PREY] = flag(a.metPrey)
        }
        if (a.nudgeLeft > 0f) x[NUDGE] = a.nudge * (a.nudgeLeft / Contacts.NUDGE_SECONDS)
        x[FED_RECENT] = exp(-Body.sinceFed(w.tick, a) / FED_TAU)
        x[DIGGER] = flag(a.role == Role.DIGGER)
        x[CARRYING] = flag(a.carriesPellet)
        x[NOISE] = w.gaussian()
        x[BIAS] = 1f
    }

    private fun surface(w: World, a: Ant, x: FloatArray) {
        val p = w.params
        val s = w.surface
        SurfaceWalk.sense(w, a, s.homeScent)
        x[FOOT_L] = a.senseL / (a.senseL + FOOT_HALF)
        x[FOOT_R] = a.senseR / (a.senseR + FOOT_HALF)
        // Trail last, so senseL and senseR keep the trail for the renderer, tests and onTrail.
        SurfaceWalk.sense(w, a, s.trail)
        x[TRAIL_L] = a.senseL / (a.senseL + p.trailThreshold)
        x[TRAIL_R] = a.senseR / (a.senseR + p.trailThreshold)

        val ex = s.entranceX - a.x
        val ey = s.entranceY - a.y
        val toEntrance = sqrt(ex * ex + ey * ey)
        val inSight = toEntrance <= p.homeSightRadius
        val hx = if (inSight) ex else -a.homeDx
        val hy = if (inSight) ey else -a.homeDy
        val hl = sqrt(hx * hx + hy * hy)
        if (inSight || hl > HOME_VECTOR_DONE) {
            x[HOME_SIN] = bearingSin(a, hx, hy)
            x[HOME_COS] = bearingCos(a, hx, hy)
            x[HOME_DIST] = hl / (hl + HOME_HALF)
        }

        val plant = s.nearestPlant(a.x, a.y, p.plantOdourRadius)
        if (plant != null) {
            val dx = plant.x - a.x
            val dy = plant.y - a.y
            x[HONEYDEW] = 1f - sqrt(dx * dx + dy * dy) / p.plantOdourRadius
            x[HONEYDEW_SIN] = bearingSin(a, dx, dy)
        }
        var target: FoodSource? = null
        val food = s.nearestFood(a.x, a.y, p.foodSenseRadius)
        if (food != null) {
            val dx = food.x - a.x
            val dy = food.y - a.y
            val d = sqrt(dx * dx + dy * dy)
            val strength = 1f - max(0f, d - food.radius) / p.foodSenseRadius
            if (food.kind == FoodKind.PREY) {
                x[PREY] = strength
                x[PREY_SIN] = bearingSin(a, dx, dy)
            } else if (strength > x[HONEYDEW]) {
                x[HONEYDEW] = strength
                x[HONEYDEW_SIN] = bearingSin(a, dx, dy)
            }
            if (d <= food.radius) target = food
        }
        val stem = s.sdf.stemAt(a.x, a.y, a.z, p.stemTouch)
        if (stem != null) {
            x[ON_STEM] = 1f
            val dx = stem.x - a.x
            val dy = stem.y - a.y
            val dz = stem.z - a.z
            if (dx * dx + dy * dy + dz * dz <= stem.radius * stem.radius) target = stem
        }
        // The source an ant is feeding at is its own until the feed ends: sensing never swaps or
        // drops it mid-feed (another source in reach, or the ant nudged off the cluster).
        if (!feeding(a)) a.food = target
        x[AT_FOOD] = flag(target != null)
        val spoil = s.spoil.get(a.x, a.y)
        x[SPOIL] = spoil / (spoil + SPOIL_HALF)
        x[AT_ENTRANCE] = flag(toEntrance <= p.entranceRadius)
    }

    /** True while the ant is committed to a feed (the FEED primitive is running). */
    private fun feeding(a: Ant): Boolean = a.action == Action.FEED || Actions.busy(a)

    private fun nest(w: World, a: Ant, x: FloatArray) {
        a.food = null
        x[IN_NEST] = 1f
        val n = w.returners.count(w.tick).toFloat()
        x[RETURNERS] = n / (n + RETURNER_HALF)
        x[ROOM_NEEDED] = roomNeeded(w)
        x[DIG_SITE] = flag(w.digNeeded() && w.paths.toFront.get(a.cellX, a.cellY) != DistanceMap.UNREACHED)
        x[AT_ENTRANCE] = flag(a.cellY == 0)
    }

    /** The colony's shortfall of air cells as a share of what it wants, times [ROOM_GAIN], 0 to 1. */
    fun roomNeeded(w: World): Float {
        if (w.excavation.finished) return 0f
        val want = w.ants.size * w.params.volumePerAnt
        if (want <= 0) return 0f
        return ((want - w.nest.airCells).toFloat() / want * ROOM_GAIN).coerceIn(0f, 1f)
    }

    /** Sine of the angle from the ant's horizontal heading to (vx, vy), positive to its left; 0 if either is vertical or zero. */
    fun bearingSin(a: Ant, vx: Float, vy: Float): Float {
        val fh = sqrt(a.fx * a.fx + a.fy * a.fy)
        val vl = sqrt(vx * vx + vy * vy)
        if (fh < 1e-3f || vl < 1e-6f) return 0f
        return (a.fx * vy - a.fy * vx) / (fh * vl)
    }

    /** Cosine of the same angle as [bearingSin]. */
    fun bearingCos(a: Ant, vx: Float, vy: Float): Float {
        val fh = sqrt(a.fx * a.fx + a.fy * a.fy)
        val vl = sqrt(vx * vx + vy * vy)
        if (fh < 1e-3f || vl < 1e-6f) return 0f
        return (a.fx * vx + a.fy * vy) / (fh * vl)
    }

    private fun flag(b: Boolean) = if (b) 1f else 0f
}

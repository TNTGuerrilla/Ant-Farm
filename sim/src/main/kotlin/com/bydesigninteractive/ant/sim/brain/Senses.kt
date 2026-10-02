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
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
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
    /**
     * m2a-2: the seed brain grew to 19 hidden units and personal variation became per unit, which
     * retires m2a-1 genomes. m2a-3: the normalised trail difference [TRAIL_DIFF] was added after
     * [TRAIL_R], which moves every later input and retires m2a-2 genomes. m2a-4 (Task 8c): the
     * saturated total [TRAIL_SUM] and the trail ring ([RING_BEARING], [RING_COS], [RING]) follow
     * [TRAIL_DIFF], which retires m2a-3 genomes.
     */
    const val LAYOUT = "m2a-4"

    const val TRAIL_L = 0
    const val TRAIL_R = 1
    const val TRAIL_DIFF = 2
    const val TRAIL_SUM = 3
    const val RING_BEARING = 4
    const val RING_COS = 5
    const val RING = 6
    const val FOOT_L = 7
    const val FOOT_R = 8
    const val HOME_SIN = 9
    const val HOME_COS = 10
    const val HOME_DIST = 11
    const val HONEYDEW = 12
    const val HONEYDEW_SIN = 13
    const val PREY = 14
    const val PREY_SIN = 15
    const val CROP = 16
    const val FULL = 17
    const val RESERVES = 18
    const val AGE = 19
    const val TEMPERATURE = 20
    const val HEALTH = 21
    const val DAMAGE = 22
    const val CONTACT_RATE = 23
    const val RETURNERS = 24
    const val MET_SUCCESS = 25
    const val MET_HONEYDEW = 26
    const val MET_PREY = 27
    const val NUDGE = 28
    const val FED_RECENT = 29
    const val SPOIL = 30
    const val ROOM_NEEDED = 31
    const val DIGGER = 32
    const val ON_STEM = 33
    const val AT_FOOD = 34
    const val IN_NEST = 35
    const val CARRYING = 36
    const val DIG_SITE = 37
    const val AT_ENTRANCE = 38
    const val NOISE = 39
    const val BIAS = 40
    const val COUNT = 41

    val NAMES = arrayOf(
        "trailL", "trailR", "trailDiff", "trailSum", "ringBearing", "ringCos", "ring", "footL", "footR", "homeSin", "homeCos", "homeDist",
        "honeydew", "honeydewSin", "prey", "preySin",
        "crop", "full", "reserves", "age", "temperature", "health", "damage",
        "contactRate", "returners", "metSuccess", "metHoneydew", "metPrey", "nudge",
        "fedRecent", "spoil", "roomNeeded", "digger",
        "onStem", "atFood", "inNest", "carrying", "digSite", "atEntrance",
        "noise", "bias",
    )

    /**
     * [TRAIL_DIFF]'s gate, in units of the trail threshold T for the two antennae together: shut
     * below [DIFF_FROM] T (a stray mark or two, whose ratio is mostly noise), fully open from
     * [DIFF_FULL] T, the reading at which a follower loses the trail (`Actions.FOLLOW_EXIT`).
     */
    const val DIFF_FROM = 0.1f
    const val DIFF_FULL = 0.3f

    /** Keeps (L - R) / (L + R) finite; far below any reading the gate lets through. */
    const val DIFF_EPS = 1e-3f

    /** Directions sampled on the trail ring ([ring]), evenly spaced by compass bearing. */
    const val RING_DIRECTIONS = 16

    /**
     * The trail ring's radius (mm). It stands for what a worker learns in its first moments
     * outside: at the nest mouth Lasius workers pause, sweep their antennae and take a few short
     * scanning steps before setting off, and with a body of about 4 mm and antennae of about 3 mm a
     * few such steps reach about 20 mm. It is also twice the trail field's 10 mm voxel, so the
     * samples (7.9 mm apart on the ring) always catch a trail that crosses it, and the trails that
     * converge on the entrance are already apart at that distance. (Task 8b's innate exit choice
     * sampled 40 mm around the entrance, which no antenna reaches.)
     */
    const val RING_RADIUS = 20f

    /**
     * The trail ring is sensed only within this distance (mm) of the entrance, the junction where
     * every trail of the colony meets. Further out trails part and an ant meets them one at a time
     * with its antennae; sampling the ring there would cost 32 field and height reads per
     * evaluation for every surface ant (the TV budget, spec 1.6) for little gain.
     */
    const val RING_NEAR = 40f

    private val ringCos = FloatArray(RING_DIRECTIONS) { cos(it * 2.0 * PI / RING_DIRECTIONS).toFloat() }
    private val ringSin = FloatArray(RING_DIRECTIONS) { sin(it * 2.0 * PI / RING_DIRECTIONS).toFloat() }

    /** Ring samples at or below this (far below any trail an ant can follow) count as no trail. */
    const val RING_FLOOR = 1e-4f

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
        x[TRAIL_DIFF] = trailDiff(a.senseL, a.senseR, p.trailThreshold)
        val sum = a.senseL + a.senseR
        x[TRAIL_SUM] = sum / (sum + p.trailThreshold)

        val ex = s.entranceX - a.x
        val ey = s.entranceY - a.y
        val toEntrance = sqrt(ex * ex + ey * ey)
        if (toEntrance <= RING_NEAR) ring(w, a, x)
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

    /**
     * The normalised trail difference (L - R) / (L + R) of the simulation reference (section 4,
     * Perna 2012), the turn signal of spec section 3, circuit 2: the same on a faint trail as on a
     * strong one, so a follower holds a busy trail as firmly as a new one. Gated by the total
     * reading (0 below [DIFF_FROM] T, rising linearly to 1 at [DIFF_FULL] T), so stray marks far
     * below the threshold do not steer. Positive when the left antenna reads more.
     */
    fun trailDiff(l: Float, r: Float, threshold: Float): Float {
        val sum = l + r
        val gate = ((sum / threshold - DIFF_FROM) / (DIFF_FULL - DIFF_FROM)).coerceIn(0f, 1f)
        if (gate <= 0f) return 0f
        return gate * (l - r) / (sum + DIFF_EPS)
    }

    /**
     * The trail ring (Task 8c): the strongest trail on a circle of [RING_RADIUS] around the ant,
     * as an ant sweeping its antennae at the nest mouth finds it. Writes [RING] (the strongest
     * sample s as s / (s + T), 0 to 1), [RING_BEARING] (its bearing from the ant's heading as an
     * angle over pi, -1 to 1, positive to the left) and [RING_COS] (the bearing's cosine). The
     * bearing is an angle, not its sine, so a trail behind the ant reads as a full turn rather
     * than as none: a sine is 0 both ahead and behind. The bearing is refined between the [RING_DIRECTIONS] samples
     * by a parabola through the strongest one and its neighbours, so it varies continuously as the
     * ant turns and moves, not in 22.5 degree steps. With no trail on the ring all three stay 0.
     * Reads the trail field and the ground height [RING_DIRECTIONS] times each; draws no random
     * number. Called only within [RING_NEAR] of the entrance.
     */
    fun ring(w: World, a: Ant, x: FloatArray) {
        val s = w.surface
        val v = w.ringSamples
        var best = 0
        for (i in 0 until RING_DIRECTIONS) {
            val px = a.x + ringCos[i] * RING_RADIUS
            val py = a.y + ringSin[i] * RING_RADIUS
            v[i] = s.trail.get(px, py, s.ground.height(px, py))
            if (v[i] > v[best]) best = i
        }
        val top = v[best]
        if (top <= RING_FLOOR) return
        val l = v[(best + RING_DIRECTIONS - 1) % RING_DIRECTIONS]
        val r = v[(best + 1) % RING_DIRECTIONS]
        val curve = l - 2f * top + r
        val shift = if (curve < 0f) (0.5f * (l - r) / curve).coerceIn(-0.5f, 0.5f) else 0f
        val angle = (best + shift) * 2f * PI.toFloat() / RING_DIRECTIONS
        val dx = cos(angle)
        val dy = sin(angle)
        val bs = bearingSin(a, dx, dy)
        val bc = bearingCos(a, dx, dy)
        if (bs != 0f || bc != 0f) x[RING_BEARING] = atan2(bs, bc) / PI.toFloat()
        x[RING_COS] = bc
        x[RING] = top / (top + w.params.trailThreshold)
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

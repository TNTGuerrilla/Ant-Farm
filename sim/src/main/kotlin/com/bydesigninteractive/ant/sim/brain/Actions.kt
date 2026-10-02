package com.bydesigninteractive.ant.sim.brain

import com.bydesigninteractive.ant.sim.DT
import com.bydesigninteractive.ant.sim.World
import com.bydesigninteractive.ant.sim.ant.Ant
import com.bydesigninteractive.ant.sim.ant.AntState
import com.bydesigninteractive.ant.sim.ant.Detour
import com.bydesigninteractive.ant.sim.ant.NestMotion
import com.bydesigninteractive.ant.sim.ant.Primitives
import com.bydesigninteractive.ant.sim.ant.Role
import com.bydesigninteractive.ant.sim.ant.Space
import com.bydesigninteractive.ant.sim.ant.SurfaceWalk
import com.bydesigninteractive.ant.sim.world.DistanceMap
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.sqrt
import kotlin.math.tanh

/** What a brain can choose (spec 2.4). WALK keeps doing the current action. */
enum class Action {
    WALK, FEED, UNLOAD, LEAVE, ENTER, DIG, PICK_UP, DROP, REST;

    companion object {
        val ALL = entries.toTypedArray()
        val COUNT = ALL.size
    }
}

/** The brain's outputs (spec 2.2), in network order, and how each is read. */
object Outputs {
    /** tanh, times [MAX_TURN_RATE] rad/s, positive to the ant's left. */
    const val TURN = 0

    /** sigmoid, times the species speed. */
    const val SPEED = 1

    /** sigmoid: trail deposit strength; below [DEPOSIT_MIN] nothing is laid. */
    const val DEPOSIT = 2

    /** sigmoid: the go-out drive, which multiplies LEAVE's choice weight (response threshold, spec 3.1). */
    const val GO_OUT = 3

    /** The first action logit; one per [Action], in order. */
    const val ACTION = 4
    const val COUNT = ACTION + 9
    const val MAX_TURN_RATE = 4f
    const val DEPOSIT_MIN = 0.05f
}

/**
 * Masks, choice and the running of each action (spec 2.4). The brain samples among the possible
 * actions every evaluation; code carries the chosen one out with the innate primitives.
 */
internal object Actions {
    /** A pellet is dropped only this far beyond the entrance rim (mm), as the scripted digger did. */
    const val DROP_CLEARANCE = 3f

    /** A follower leaves the trail when its reading falls below this share of the trail threshold (M1's trailLossFraction). */
    const val FOLLOW_EXIT = 0.3f

    fun sigmoid(v: Float): Float = 1f / (1f + exp(-v))

    /** Timed primitives run to the end without the brain: feeding, and unloading and digging once arrived. */
    fun busy(a: Ant): Boolean =
        a.arrived && (a.action == Action.FEED || a.action == Action.UNLOAD || a.action == Action.DIG)

    /** Reads the continuous outputs into the ant; they hold until its next evaluation. */
    fun read(a: Ant, o: FloatArray) {
        a.turnRate = Outputs.MAX_TURN_RATE * tanh(o[Outputs.TURN])
        a.speedOut = sigmoid(o[Outputs.SPEED])
        a.deposit = sigmoid(o[Outputs.DEPOSIT])
        a.goOut = sigmoid(o[Outputs.GO_OUT])
    }

    /** Which actions are possible now, from the inputs [x] Senses filled and the ant. */
    fun mask(w: World, a: Ant, x: FloatArray, out: BooleanArray) {
        out.fill(false)
        out[Action.WALK.ordinal] = true
        val forager = a.role == Role.FORAGER
        if (a.space == Space.SURFACE) {
            out[Action.FEED.ordinal] = forager && a.crop <= 0f && a.food != null && x[Senses.AT_FOOD] > 0f
            out[Action.ENTER.ordinal] = !a.carriesPellet && x[Senses.AT_ENTRANCE] > 0f
            out[Action.PICK_UP.ordinal] = !forager && !a.carriesPellet && w.surface.spoil.get(a.x, a.y) >= 1f
            out[Action.DROP.ordinal] = a.carriesPellet && distanceToEntrance(w, a) > w.params.entranceRadius + DROP_CLEARANCE
        } else {
            out[Action.UNLOAD.ordinal] = forager && a.crop > 0f
            out[Action.LEAVE.ordinal] = (forager && a.crop <= 0f) || a.carriesPellet
            out[Action.DIG.ordinal] = !forager && !a.carriesPellet && x[Senses.DIG_SITE] > 0f
            out[Action.REST.ordinal] = !a.carriesPellet
        }
    }

    /**
     * Fills `w.actionWeights` with each action's choice weight (softmax numerators over the
     * possible actions, LEAVE times the go-out drive) and returns their sum. Fills `w.mask` too.
     */
    fun weights(w: World, a: Ant, x: FloatArray, o: FloatArray): Float {
        val m = w.mask
        mask(w, a, x, m)
        val wt = w.actionWeights
        var top = Float.NEGATIVE_INFINITY
        for (i in 0 until Action.COUNT) if (m[i]) top = max(top, o[Outputs.ACTION + i])
        var sum = 0f
        for (i in 0 until Action.COUNT) {
            var v = if (m[i]) exp(o[Outputs.ACTION + i] - top) else 0f
            if (i == Action.LEAVE.ordinal) v *= a.goOut
            wt[i] = v
            sum += v
        }
        return sum
    }

    /** Samples an action (one draw from the world's generator) and starts it if it is new. */
    fun choose(w: World, a: Ant, x: FloatArray, o: FloatArray) {
        val sum = weights(w, a, x, o)
        val wt = w.actionWeights
        var pick = w.rng.nextFloat() * sum
        var chosen = Action.WALK
        for (i in 0 until Action.COUNT) {
            if (wt[i] <= 0f) continue
            pick -= wt[i]
            if (pick < 0f) {
                chosen = Action.ALL[i]
                break
            }
        }
        if (chosen == Action.WALK) {
            if (!w.mask[a.action.ordinal]) begin(w, a, fallback(a))
            return
        }
        if (chosen != a.action) begin(w, a, chosen)
    }

    /** The action an ant falls back to when its current one has become impossible. */
    private fun fallback(a: Ant): Action = when {
        a.space == Space.SURFACE -> Action.WALK
        a.carriesPellet -> Action.LEAVE
        a.role == Role.FORAGER && a.crop > 0f -> Action.UNLOAD
        else -> Action.REST
    }

    fun begin(w: World, a: Ant, action: Action) {
        a.action = action
        a.arrived = false
        when (action) {
            Action.FEED -> {
                val food = a.food
                if (food == null) {
                    a.action = Action.WALK
                    return
                }
                Primitives.startFeeding(w, a, food)
                a.arrived = true
            }
            Action.ENTER -> {
                val returning = a.role == Role.FORAGER && a.crop > 0f
                w.enterNest(a)
                if (returning) w.returners.add(w.tick)
                a.action = Action.REST
            }
            Action.PICK_UP -> {
                Primitives.pickUpPellet(w, a)
                a.action = Action.WALK
            }
            Action.DROP -> {
                Primitives.dropPellet(w, a)
                a.action = Action.WALK
            }
            else -> Unit
        }
    }

    /** Carries out the current action for one tick. */
    fun run(w: World, a: Ant) {
        if (a.space == Space.SURFACE) {
            if (a.action == Action.FEED) feed(w, a) else walk(w, a)
            return
        }
        when (a.action) {
            Action.UNLOAD -> unload(w, a)
            Action.LEAVE -> leave(w, a)
            Action.DIG -> dig(w, a)
            else -> if (NestMotion.move(w, a)) NestMotion.goRest(w, a)
        }
    }

    private fun feed(w: World, a: Ant) {
        a.speed = 0f
        a.timer -= DT
        if (a.timer > 0f) return
        Primitives.finishFeeding(w, a)
        a.fedTick = w.tick
        a.reserves = 1f
        a.timer = 0f
        a.arrived = false
        a.action = Action.WALK
    }

    /**
     * Walking on the surface: stems are innate (an empty forager climbs to the aphids, anyone else
     * comes down), and so are obstacle detours toward a plant or the entrance in sight; otherwise
     * the ant turns at the brain's rate. Trail is laid by the deposit primitive as it walks.
     */
    private fun walk(w: World, a: Ant) {
        val p = w.params
        val speed = p.surfaceSpeed * a.speedOut * (if (a.carriesPellet) p.loadedFactor else 1f)
        val stem = w.surface.sdf.stemAt(a.x, a.y, a.z, p.stemTouch)
        if (stem != null && a.role == Role.FORAGER && a.crop <= 0f) {
            // Climbing to the aphids ends any trail following; only a full reading starts it again.
            a.onTrail = false
            Primitives.faceUpStem(a, stem)
            SurfaceWalk.step(w, a, speed)
            return
        }
        if (stem != null && Primitives.onPlant(w, a)) {
            SurfaceWalk.faceDirection(a, 0f, 0f, -1f)
            deposit(w, a, speed)
            SurfaceWalk.step(w, a, speed)
            return
        }
        if (!detouring(w, a)) SurfaceWalk.turn(a, a.turnRate * DT)
        deposit(w, a, speed)
        SurfaceWalk.step(w, a, speed)
    }

    /** Innate obstacle detours (spec 2.4): toward a plant an empty, fresh forager smells, or toward the entrance in sight when homeward. */
    private fun detouring(w: World, a: Ant): Boolean {
        val p = w.params
        val s = w.surface
        if (a.role == Role.FORAGER && a.crop <= 0f && a.reserves >= Body.TIRED) {
            val plant = s.nearestPlant(a.x, a.y, p.plantOdourRadius)
            if (plant != null) {
                val dx = plant.x - a.x
                val dy = plant.y - a.y
                return Detour.detouring(w, a, plant.id, sqrt(dx * dx + dy * dy))
            }
        }
        if (homeward(a)) {
            val d = distanceToEntrance(w, a)
            if (d <= p.homeSightRadius) return Detour.detouring(w, a, Detour.NEST_SIGHT, d)
        }
        return false
    }

    private fun homeward(a: Ant): Boolean =
        !a.carriesPellet && (a.role == Role.DIGGER || a.crop > 0f || a.reserves < Body.TIRED)

    /** The deposit primitive: marks at the species' rate per mm walked, scaled by the brain's strength (spec 3.4). */
    private fun deposit(w: World, a: Ant, speed: Float) {
        if (a.crop <= 0f || !a.laysTrail || a.deposit < Outputs.DEPOSIT_MIN) return
        if (w.rng.nextFloat() >= w.params.markChancePerMm * speed * DT) return
        Primitives.deposit(w, a, w.params.markAmount * a.deposit)
    }

    private fun unload(w: World, a: Ant) {
        val p = w.params
        if (!a.arrived) {
            if (!NestMotion.goDown(w, a, p.unloadDepth)) {
                a.arrived = true
                a.timer = p.unloadSeconds
            }
            return
        }
        a.speed = 0f
        a.timer -= DT
        if (a.timer > 0f) return
        a.crop = 0f
        a.arrived = false
        w.unloads++
        a.action = Action.REST
    }

    private fun leave(w: World, a: Ant) {
        if (!NestMotion.goUp(w, a)) return
        // The heading out is the random one exitNest gives; the brain turns toward a trail it
        // senses on the ring around it (Senses.ring, the seed brain's exit circuit).
        w.exitNest(a)
        a.action = Action.WALK
    }

    private fun dig(w: World, a: Ant) {
        if (a.arrived) {
            a.speed = 0f
            a.timer -= DT
            if (a.timer > 0f) return
            a.arrived = false
            if (Primitives.finishDigging(w, a)) a.action = Action.LEAVE
            return
        }
        if (!NestMotion.move(w, a)) return
        if (!w.digNeeded()) {
            a.action = Action.REST
            return
        }
        val front = w.paths.toFront
        val d = front.get(a.cellX, a.cellY)
        when {
            d == DistanceMap.UNREACHED -> a.action = Action.REST
            d == 0 -> if (Primitives.startDigging(w, a)) a.arrived = true
            else -> NestMotion.stepTo(w, a, front, d - 1)
        }
    }

    /**
     * Sets the display state the renderer, HUD and tests read from the action, then the following
     * state ([follow]). An empty forager that is tired ([Body.TIRED]) is shown as returning: it has
     * given up the search and is going home, as the scripted forager's give-up did.
     */
    fun syncState(w: World, a: Ant) {
        a.state = if (a.space == Space.NEST) {
            when (a.action) {
                Action.UNLOAD -> AntState.UNLOAD
                Action.LEAVE -> if (a.carriesPellet) AntState.CARRY_OUT else AntState.EXIT
                Action.DIG -> if (a.arrived) AntState.DIG else AntState.GO_DIG
                else -> AntState.IDLE
            }
        } else when {
            a.action == Action.FEED -> AntState.FEED
            a.carriesPellet -> AntState.DUMP
            a.role == Role.DIGGER -> AntState.GO_HOME
            a.crop > 0f || a.reserves < Body.TIRED -> AntState.RETURN
            else -> AntState.SEARCH
        }
        follow(w, a)
    }

    /**
     * The trail-following state ([Ant.onTrail]) that GruterScenarioTest measures, with the scripted
     * forager's thresholds from M1: a searching forager enters it when its two antennae together
     * read at least the trail threshold T, and keeps it until the reading falls below
     * [FOLLOW_EXIT] T or it stops searching. The scripts entered only with a chance that rose
     * steeply with the reading; that chance was the script's decision to follow, which is now the
     * brain's (it steers along the trail or wanders off it), so it does not come back here. The
     * state only records what the ant is doing and never feeds back into the brain.
     *
     * The readings are the trail senses of the last evaluation, refreshed every other tick (spec
     * 2.3). They are at most one tick old: 1 mm of walking, a tenth of the 10 mm the antennae
     * reach ahead, while the ant holds the turn it chose from those same readings. Sampling the
     * field every tick would add two field reads per surface ant per tick to the TV budget for no
     * change a 200-tick sample can see. [World.exitNest] clears the readings, so an ant leaving the
     * nest never starts with the ones it had when it went in.
     */
    private fun follow(w: World, a: Ant) {
        if (a.state != AntState.SEARCH || a.role != Role.FORAGER) {
            a.onTrail = false
            return
        }
        val reading = a.senseL + a.senseR
        val t = w.params.trailThreshold
        a.onTrail = if (a.onTrail) reading >= FOLLOW_EXIT * t else reading >= t
    }

    private fun distanceToEntrance(w: World, a: Ant): Float {
        val dx = a.x - w.surface.entranceX
        val dy = a.y - w.surface.entranceY
        return sqrt(dx * dx + dy * dy)
    }
}

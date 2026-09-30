package com.bydesigninteractive.ant.sim.ant

import com.bydesigninteractive.ant.sim.DT
import com.bydesigninteractive.ant.sim.FeedEvent
import com.bydesigninteractive.ant.sim.World
import com.bydesigninteractive.ant.sim.world.FoodSource
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * The scripted forager: leave the nest, search (joining trails with a chance that rises steeply
 * with their strength), feed, walk home by path integration laying trail if it filled up, unload
 * inside the nest, and go again. Values come from sections 3 to 5 of the simulation reference.
 * On the 3D surface it steers through [SurfaceWalk] turns and facings.
 */
internal object Forager {
    private const val HOME_VECTOR_DONE = 15f
    private const val OUTWARD_FROM = 30f
    private const val STEM_WALL_NZ = 0.5f // steeper than this is still the stem wall
    private const val CROWD_CUT = 4.6f // up to 5.6x less deposition when crowded

    fun update(w: World, a: Ant) {
        when (a.state) {
            AntState.EXIT -> if (NestMotion.goUp(w, a)) {
                w.exitNest(a)
                chooseExitTrail(w, a)
                a.state = AntState.SEARCH
            }
            AntState.UNLOAD -> unload(w, a)
            AntState.SEARCH -> search(w, a)
            AntState.FEED -> feed(w, a)
            AntState.RETURN -> goHome(w, a)
            else -> a.state = AntState.EXIT
        }
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
        a.state = AntState.EXIT
    }

    private fun search(w: World, a: Ant) {
        val p = w.params
        val s = w.surface
        a.timer += DT
        if (a.timer > p.searchGiveUp) {
            a.onTrail = false
            a.runLeft = 0f
            a.state = AntState.RETURN
            return
        }
        if (climbOrApproachPlant(w, a)) return
        val food = s.nearestFood(a.x, a.y, p.foodSenseRadius)
        if (food != null) {
            val dx = food.x - a.x
            val dy = food.y - a.y
            if (dx * dx + dy * dy <= food.radius * food.radius) {
                startFeeding(w, a, food)
                return
            }
            SurfaceWalk.faceToward(a, food.x, food.y, food.z)
            SurfaceWalk.step(w, a, p.surfaceSpeed)
            return
        }
        SurfaceWalk.sense(w, a, s.trail)
        val strength = a.senseL + a.senseR
        if (a.onTrail) {
            if (strength < p.trailThreshold * p.trailLossFraction) {
                a.onTrail = false
            } else {
                SurfaceWalk.steerByGradient(w, a, p.trailTurnGain)
                keepOutward(a)
            }
        }
        if (!a.onTrail) {
            a.runLeft -= p.surfaceSpeed * DT
            if (a.runLeft <= 0f) {
                // A decision point: join the trail here with a chance that rises steeply with its strength.
                val s2 = strength * strength
                val follow = p.maxFollow * s2 / (s2 + p.trailThreshold * p.trailThreshold)
                if (w.rng.nextFloat() < follow) {
                    a.onTrail = true
                } else {
                    SurfaceWalk.turn(a, w.gaussian() * p.outTurnSd)
                    a.runLeft = w.exponential(p.outRunMean)
                }
            }
        }
        SurfaceWalk.step(w, a, p.surfaceSpeed)
    }

    /**
     * Plants: an empty forager touching a stem climbs it and feeds at the aphid cluster; one in
     * range of a plant's honeydew odour heads for the stem. True if it acted this tick.
     */
    private fun climbOrApproachPlant(w: World, a: Ant): Boolean {
        val p = w.params
        val s = w.surface
        if (a.crop > 0f) return false
        val onStem = s.sdf.stemAt(a.x, a.y, a.z, p.stemTouch)
        if (onStem != null) {
            val dx = onStem.x - a.x
            val dy = onStem.y - a.y
            val dz = onStem.z - a.z
            if (dx * dx + dy * dy + dz * dz <= onStem.radius * onStem.radius) {
                startFeeding(w, a, onStem)
                return true
            }
            a.onTrail = false
            // Up alone is ill defined on the sloping ground at the stem base (it can point away
            // from the stem), so press toward the axis as well; on the stem wall that part is
            // removed by the tangent projection and only the climb remains.
            val ax = onStem.x - a.x
            val ay = onStem.y - a.y
            val ah = sqrt(ax * ax + ay * ay)
            if (ah > 1e-3f) SurfaceWalk.faceDirection(a, ax / ah, ay / ah, 1f)
            else SurfaceWalk.faceDirection(a, 0f, 0f, 1f)
            SurfaceWalk.step(w, a, p.surfaceSpeed)
            return true
        }
        val plant = s.nearestPlant(a.x, a.y, p.plantOdourRadius) ?: return false
        SurfaceWalk.faceDirection(a, plant.x - a.x, plant.y - a.y, 0f)
        SurfaceWalk.step(w, a, p.surfaceSpeed)
        return true
    }

    private fun startFeeding(w: World, a: Ant, food: FoodSource) {
        a.food = food
        a.onTrail = false
        a.speed = 0f
        a.timer = w.params.feedSeconds
        a.state = AntState.FEED
    }

    /**
     * A forager just out of the nest samples the trail on a ring around the entrance. It joins a
     * trail with a chance that rises steeply with the total, and picks a direction by Deneubourg's
     * choice function (k + s_i)^n / sum (k + s_j)^n, so stronger trails win more than their share
     * (section 5). Otherwise it keeps its random exit heading.
     */
    fun chooseExitTrail(w: World, a: Ant) {
        val p = w.params
        val s = w.surface
        val n = p.exitDirections
        val samples = FloatArray(n)
        var total = 0f
        for (i in 0 until n) {
            val angle = i * 2f * PI.toFloat() / n
            val x = s.entranceX + cos(angle) * p.exitSenseRadius
            val y = s.entranceY + sin(angle) * p.exitSenseRadius
            samples[i] = s.trail.get(x, y, s.ground.height(x, y))
            total += samples[i]
        }
        val t2 = total * total
        val join = p.maxFollow * t2 / (t2 + p.trailThreshold * p.trailThreshold)
        if (w.rng.nextFloat() >= join) return
        var sum = 0f
        for (i in 0 until n) {
            samples[i] = (p.exitChoiceK + samples[i]).pow(p.exitChoiceExponent)
            sum += samples[i]
        }
        var pick = w.rng.nextFloat() * sum
        var chosen = n - 1
        for (i in 0 until n) {
            pick -= samples[i]
            if (pick < 0f) {
                chosen = i
                break
            }
        }
        SurfaceWalk.faceCompass(a, chosen * 2f * PI.toFloat() / n)
        a.onTrail = true
    }

    /** An outbound ant on a trail keeps walking away from home, never back along it. */
    private fun keepOutward(a: Ant) {
        if (a.homeDx * a.homeDx + a.homeDy * a.homeDy < OUTWARD_FROM * OUTWARD_FROM) return
        if (SurfaceWalk.horizontalForward(a) < 0.5f) return // climbing: no compass sense of outward
        if (SurfaceWalk.horizontalDot(a, a.homeDx, a.homeDy) < 0f) SurfaceWalk.turn(a, PI.toFloat())
    }

    private fun feed(w: World, a: Ant) {
        a.speed = 0f
        a.timer -= DT
        if (a.timer > 0f) return
        val food = a.food
        if (food != null) {
            a.crop = food.quality
            w.feedEvents += FeedEvent(w.tick, food.id)
            if (food.loads != Int.MAX_VALUE) {
                food.loads--
                if (food.loads <= 0) w.surface.foods.remove(food)
            }
        }
        a.food = null
        a.runLeft = 0f
        a.timer = 0f
        a.state = AntState.RETURN
    }

    private fun goHome(w: World, a: Ant) {
        val p = w.params
        val s = w.surface
        val ex = s.entranceX - a.x
        val ey = s.entranceY - a.y
        val toEntrance = sqrt(ex * ex + ey * ey)
        if (toEntrance <= p.entranceRadius) {
            w.enterNest(a)
            a.arrived = false
            a.state = AntState.UNLOAD
            return
        }
        if (s.sdf.stemAt(a.x, a.y, a.z, p.stemTouch) != null &&
            (a.z > s.ground.height(a.x, a.y) + p.stemLeaveHeight || a.nz < STEM_WALL_NZ)
        ) {
            // Still on the plant (above the leave height, or on its steep base where a horizontal
            // homing heading points into the wall): come down the stem before homing.
            SurfaceWalk.faceDirection(a, 0f, 0f, -1f)
            layTrail(w, a)
            SurfaceWalk.step(w, a, p.surfaceSpeed)
            return
        }
        when {
            toEntrance <= p.homeSightRadius ->
                SurfaceWalk.faceToward(a, s.entranceX, s.entranceY, s.ground.height(s.entranceX, s.entranceY))
            a.homeDx * a.homeDx + a.homeDy * a.homeDy > HOME_VECTOR_DONE * HOME_VECTOR_DONE -> {
                a.runLeft -= p.surfaceSpeed * DT
                if (a.runLeft <= 0f) {
                    SurfaceWalk.faceCompass(a, atan2(-a.homeDy, -a.homeDx) + w.gaussian() * p.homeTurnSd)
                    a.runLeft = w.exponential(p.homeRunMean)
                }
            }
            else -> {
                // The home vector has run out short of the nest: climb the home-range scent.
                SurfaceWalk.sense(w, a, s.homeScent)
                SurfaceWalk.steerByGradient(w, a, p.trailTurnGain)
                SurfaceWalk.wander(w, a, p.outRunMean, p.outTurnSd)
            }
        }
        layTrail(w, a)
        SurfaceWalk.step(w, a, p.surfaceSpeed)
    }

    /**
     * Marks the trail on the way home, only if the ant filled its crop to its desired volume.
     * Less is laid where the trail is already strong and where it is crowded (section 5).
     */
    private fun layTrail(w: World, a: Ant) {
        val p = w.params
        if (!a.laysTrail || a.crop < a.desiredCrop) return
        if (w.rng.nextFloat() >= p.markChancePerMm * p.surfaceSpeed * DT) return
        val trail = w.surface.trail
        val saturation = 1f / (1f + trail.get(a.x, a.y, a.z) / p.saturationLevel)
        val crowd = w.surfaceIndex.countNear(a, p.crowdRadius)
        val crowding = 1f / (1f + CROWD_CUT * min(1f, crowd / p.crowdLevel.toFloat()))
        trail.add(a.x, a.y, a.z, p.markAmount * saturation * crowding)
    }
}

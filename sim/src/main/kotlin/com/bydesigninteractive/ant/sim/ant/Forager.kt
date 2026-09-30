package com.bydesigninteractive.ant.sim.ant

import com.bydesigninteractive.ant.sim.DT
import com.bydesigninteractive.ant.sim.FeedEvent
import com.bydesigninteractive.ant.sim.World
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * The scripted forager: leave the nest, search (joining trails with a chance that rises steeply
 * with their strength), feed, walk home by path integration laying trail if it filled up, unload
 * inside the nest, and go again. Values come from sections 3 to 5 of the simulation reference.
 */
internal object Forager {
    private const val HOME_VECTOR_DONE = 15f
    private const val OUTWARD_FROM = 30f
    private const val CROWD_CUT = 4.6f // up to 5.6x less deposition when crowded

    fun update(w: World, a: Ant) {
        when (a.state) {
            AntState.EXIT -> if (NestMotion.goUp(w, a)) {
                w.exitNest(a)
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
        val food = s.nearestFood(a.x, a.y, p.foodSenseRadius)
        if (food != null) {
            val dx = food.x - a.x
            val dy = food.y - a.y
            if (dx * dx + dy * dy <= food.radius * food.radius) {
                a.food = food
                a.onTrail = false
                a.speed = 0f
                a.timer = p.feedSeconds
                a.state = AntState.FEED
                return
            }
            a.heading = atan2(dy, dx)
            SurfaceMotion.advance(w, a, p.surfaceSpeed)
            return
        }
        SurfaceMotion.sense(w, a, s.trail)
        val strength = a.senseL + a.senseR
        if (a.onTrail) {
            if (strength < p.trailThreshold * p.trailLossFraction) {
                a.onTrail = false
            } else {
                SurfaceMotion.steerByGradient(w, a, p.trailTurnGain)
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
                    a.heading += w.gaussian() * p.outTurnSd
                    a.runLeft = w.exponential(p.outRunMean)
                }
            }
        }
        SurfaceMotion.advance(w, a, p.surfaceSpeed)
    }

    /** An outbound ant on a trail keeps walking away from home, never back along it. */
    private fun keepOutward(a: Ant) {
        if (a.homeDx * a.homeDx + a.homeDy * a.homeDy < OUTWARD_FROM * OUTWARD_FROM) return
        if (cos(a.heading) * a.homeDx + sin(a.heading) * a.homeDy < 0f) a.heading += PI.toFloat()
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
        when {
            toEntrance <= p.homeSightRadius -> a.heading = atan2(ey, ex)
            a.homeDx * a.homeDx + a.homeDy * a.homeDy > HOME_VECTOR_DONE * HOME_VECTOR_DONE -> {
                a.runLeft -= p.surfaceSpeed * DT
                if (a.runLeft <= 0f) {
                    a.heading = atan2(-a.homeDy, -a.homeDx) + w.gaussian() * p.homeTurnSd
                    a.runLeft = w.exponential(p.homeRunMean)
                }
            }
            else -> {
                // The home vector has run out short of the nest: climb the home-range scent.
                SurfaceMotion.sense(w, a, s.homeScent)
                SurfaceMotion.steerByGradient(w, a, p.trailTurnGain)
                SurfaceMotion.wander(w, a, p.outRunMean, p.outTurnSd)
            }
        }
        layTrail(w, a)
        SurfaceMotion.advance(w, a, p.surfaceSpeed)
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
        val saturation = 1f / (1f + trail.get(a.x, a.y) / p.saturationLevel)
        val crowd = w.surfaceIndex.countNear(a, p.crowdRadius)
        val crowding = 1f / (1f + CROWD_CUT * min(1f, crowd / p.crowdLevel.toFloat()))
        trail.add(a.x, a.y, p.markAmount * saturation * crowding)
    }
}

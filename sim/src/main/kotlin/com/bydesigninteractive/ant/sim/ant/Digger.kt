package com.bydesigninteractive.ant.sim.ant

import com.bydesigninteractive.ant.sim.DT
import com.bydesigninteractive.ant.sim.World
import com.bydesigninteractive.ant.sim.world.DistanceMap
import com.bydesigninteractive.ant.sim.world.Material
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.sqrt

/**
 * The scripted digger: when the colony needs room it walks to the frontier, digs one cell into
 * a pellet, carries it out and drops it on the spoil pile, then comes back. Drops and pick-ups
 * on the pile follow the Khuong 2016 rates (section 10).
 */
internal object Digger {
    fun update(w: World, a: Ant) {
        when (a.state) {
            AntState.IDLE -> idle(w, a)
            AntState.GO_DIG -> goDig(w, a)
            AntState.DIG -> dig(w, a)
            AntState.CARRY_OUT -> if (NestMotion.goUp(w, a)) {
                w.exitNest(a)
                a.state = AntState.DUMP
            }
            AntState.DUMP -> dump(w, a)
            AntState.GO_HOME -> goHome(w, a)
            else -> a.state = AntState.IDLE
        }
    }

    private fun idle(w: World, a: Ant) {
        if (!NestMotion.move(w, a)) return
        if (w.digNeeded() && w.excavation.frontier().isNotEmpty()) a.state = AntState.GO_DIG
    }

    private fun goDig(w: World, a: Ant) {
        if (!NestMotion.move(w, a)) return
        if (!w.digNeeded()) {
            a.state = AntState.IDLE
            return
        }
        val front = w.paths.toFront
        val d = front.get(a.cellX, a.cellY)
        when {
            d == DistanceMap.UNREACHED -> a.state = AntState.IDLE
            d == 0 -> startDigging(w, a)
            else -> NestMotion.stepTo(w, a, front, d - 1)
        }
    }

    private fun startDigging(w: World, a: Ant) {
        val next = w.excavation.frontier().filter { abs(it.x - a.cellX) + abs(it.y - a.cellY) == 1 }
        if (next.isEmpty()) return // the map is a tick behind the frontier; it catches up next tick
        val target = next[w.rng.nextInt(next.size)]
        a.digX = target.x
        a.digY = target.y
        val clay = w.nest.material(target.x, target.y) == Material.CLAY
        a.timer = w.params.digSecondsPerCell * (if (clay) w.params.clayFactor else 1f)
        a.heading = atan2((target.y - a.cellY).toFloat(), (target.x - a.cellX).toFloat())
        a.state = AntState.DIG
    }

    private fun dig(w: World, a: Ant) {
        a.speed = 0f
        a.timer -= DT
        if (a.timer > 0f) return
        if (w.nest.material(a.digX, a.digY).diggable) {
            w.dig(a.digX, a.digY)
            a.carriesPellet = true
            a.state = AntState.CARRY_OUT
        } else {
            a.state = AntState.GO_DIG
        }
    }

    private fun dump(w: World, a: Ant) {
        val p = w.params
        val s = w.surface
        val ox = a.x - s.entranceX
        val oy = a.y - s.entranceY
        val dist = sqrt(ox * ox + oy * oy)
        if (dist > p.entranceRadius + 3f) {
            val rate = p.spoilDropBase + p.spoilDropPerPellet * s.spoil.get(a.x, a.y)
            if (dist >= p.spoilMaxDistance || w.rng.nextFloat() < rate * DT) {
                s.spoil.add(a.x, a.y, 1f)
                a.carriesPellet = false
                a.state = AntState.GO_HOME
                return
            }
        }
        SurfaceWalk.wander(w, a, p.outRunMean, p.outTurnSd)
        // Keep walking away from the entrance so the pile never buries it.
        if (SurfaceWalk.horizontalDot(a, ox, oy) < 0f) SurfaceWalk.faceCompass(a, atan2(oy, ox) + w.gaussian() * 0.5f)
        SurfaceWalk.step(w, a, p.surfaceSpeed * p.loadedFactor)
    }

    private fun goHome(w: World, a: Ant) {
        val p = w.params
        val s = w.surface
        val dx = s.entranceX - a.x
        val dy = s.entranceY - a.y
        val dist = sqrt(dx * dx + dy * dy)
        if (dist <= p.entranceRadius) {
            w.enterNest(a)
            a.state = AntState.IDLE
            return
        }
        if (s.spoil.get(a.x, a.y) >= 1f && w.rng.nextFloat() < p.spoilPickUp * DT) {
            s.spoil.add(a.x, a.y, -1f)
            a.carriesPellet = true
            a.state = AntState.DUMP
            return
        }
        if (!Detour.detouring(w, a, Detour.NEST_SIGHT, dist)) {
            SurfaceWalk.faceToward(a, s.entranceX, s.entranceY, s.ground.height(s.entranceX, s.entranceY))
        }
        SurfaceWalk.step(w, a, p.surfaceSpeed)
    }
}

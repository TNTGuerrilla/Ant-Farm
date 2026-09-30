package com.bydesigninteractive.ant.sim.ant

import com.bydesigninteractive.ant.sim.DT
import com.bydesigninteractive.ant.sim.World
import com.bydesigninteractive.ant.sim.world.ChunkedField
import com.bydesigninteractive.ant.sim.world.SURFACE_MM
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** Walking on the surface map: moving, search runs, sensing fields and path integration. */
internal object SurfaceMotion {
    /**
     * Moves the ant along its heading, bouncing off the map edges. Its home vector gains the
     * true displacement plus noise that grows with distance walked, and it leaves home-range
     * scent behind (section 4).
     */
    fun advance(w: World, a: Ant, speed: Float) {
        val step = speed * DT
        var nx = a.x + cos(a.heading) * step
        var ny = a.y + sin(a.heading) * step
        val max = SURFACE_MM.toFloat()
        if (nx < 0f || nx > max) {
            a.heading = PI.toFloat() - a.heading
            nx = nx.coerceIn(0f, max)
        }
        if (ny < 0f || ny > max) {
            a.heading = -a.heading
            ny = ny.coerceIn(0f, max)
        }
        val noise = w.params.piErrorPerMm * step
        a.homeDx += (nx - a.x) + w.gaussian() * noise
        a.homeDy += (ny - a.y) + w.gaussian() * noise
        a.x = nx
        a.y = ny
        a.speed = speed
        w.surface.homeScent.add(nx, ny, w.params.homeScentPerSecond * DT)
    }

    /** A search walk: straight runs of exponential length, then a random turn (section 5). */
    fun wander(w: World, a: Ant, runMean: Float, turnSd: Float) {
        a.runLeft -= w.params.surfaceSpeed * DT
        if (a.runLeft > 0f) return
        a.heading += w.gaussian() * turnSd
        a.runLeft = w.exponential(runMean)
    }

    /** Samples [field] ahead-left and ahead-right into senseL and senseR. */
    fun sense(w: World, a: Ant, field: ChunkedField) {
        val p = w.params
        val left = a.heading + p.senseAngle
        val right = a.heading - p.senseAngle
        a.senseL = field.get(a.x + cos(left) * p.senseAhead, a.y + sin(left) * p.senseAhead)
        a.senseR = field.get(a.x + cos(right) * p.senseAhead, a.y + sin(right) * p.senseAhead)
    }

    /** Turns toward the stronger side in proportion to (L - R) / (L + R) (section 4). */
    fun steerByGradient(w: World, a: Ant, gain: Float) {
        val s = a.senseL + a.senseR
        if (s <= 0f) return
        a.heading += gain * (a.senseL - a.senseR) / s * DT
    }
}

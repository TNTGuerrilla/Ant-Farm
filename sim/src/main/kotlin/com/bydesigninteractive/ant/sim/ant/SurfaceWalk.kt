package com.bydesigninteractive.ant.sim.ant

import com.bydesigninteractive.ant.sim.DT
import com.bydesigninteractive.ant.sim.World
import com.bydesigninteractive.ant.sim.world.Field3
import com.bydesigninteractive.ant.sim.world.SURFACE_MM
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Walking on the 3D surface. A step moves along the forward vector, projects back onto the
 * surface, reads the new normal and carries the forward vector into the new tangent plane, so
 * walking from the ground onto a rock, under an overhang or up a stem needs no special cases.
 * Ants never fall. Path integration uses the horizontal part of each step (section 4).
 */
internal object SurfaceWalk {
    private const val FOLD = 0.2f

    /** Puts a surface ant on the surface at (x, y), facing compass angle [heading]. */
    fun place(w: World, a: Ant, x: Float, y: Float, heading: Float) {
        val p = w.pos
        p[0] = x
        p[1] = y
        p[2] = w.surface.ground.height(x, y)
        w.surface.sdf.project(p)
        a.x = p[0]
        a.y = p[1]
        a.z = p[2]
        a.heading = heading
        normal(w, a)
        a.fx = cos(heading)
        a.fy = sin(heading)
        a.fz = 0f
        transport(a)
    }

    fun step(w: World, a: Ant, speed: Float) {
        val s = speed * DT
        val ox = a.x
        val oy = a.y
        val max = SURFACE_MM.toFloat()
        var x = a.x + a.fx * s
        var y = a.y + a.fy * s
        val bounced = x < 0f || x > max || y < 0f || y > max
        x = x.coerceIn(0f, max)
        y = y.coerceIn(0f, max)
        val p = w.pos
        p[0] = x
        p[1] = y
        p[2] = a.z + a.fz * s
        w.surface.sdf.project(p)
        a.x = p[0]
        a.y = p[1]
        a.z = p[2]
        normal(w, a)
        transport(a)
        if (bounced) turn(a, PI.toFloat())
        val noise = w.params.piErrorPerMm * s
        a.homeDx += (a.x - ox) + w.gaussian() * noise
        a.homeDy += (a.y - oy) + w.gaussian() * noise
        a.speed = speed
        w.surface.homeScent.add(a.x, a.y, a.z, w.params.homeScentPerSecond * DT)
    }

    /** Rotates the forward vector about the normal; positive is to the ant's left. */
    fun turn(a: Ant, angle: Float) {
        val c = cos(angle)
        val s = sin(angle)
        val cx = a.ny * a.fz - a.nz * a.fy
        val cy = a.nz * a.fx - a.nx * a.fz
        val cz = a.nx * a.fy - a.ny * a.fx
        a.fx = a.fx * c + cx * s
        a.fy = a.fy * c + cy * s
        a.fz = a.fz * c + cz * s
        transport(a)
    }

    /** Faces the direction (dx, dy, dz) as projected into the tangent plane, if it has one. */
    fun faceDirection(a: Ant, dx: Float, dy: Float, dz: Float) {
        val d = dx * a.nx + dy * a.ny + dz * a.nz
        val px = dx - a.nx * d
        val py = dy - a.ny * d
        val pz = dz - a.nz * d
        val len = sqrt(px * px + py * py + pz * pz)
        if (len < 1e-4f) return
        a.fx = px / len
        a.fy = py / len
        a.fz = pz / len
        updateHeading(a)
    }

    fun faceToward(a: Ant, x: Float, y: Float, z: Float) = faceDirection(a, x - a.x, y - a.y, z - a.z)

    fun faceCompass(a: Ant, angle: Float) = faceDirection(a, cos(angle), sin(angle), 0f)

    /** A search walk: straight runs of exponential length, then a random turn (section 5). */
    fun wander(w: World, a: Ant, runMean: Float, turnSd: Float) {
        a.runLeft -= w.params.surfaceSpeed * DT
        if (a.runLeft > 0f) return
        turn(a, w.gaussian() * turnSd)
        a.runLeft = w.exponential(runMean)
    }

    /** Samples [field] ahead-left and ahead-right on the surface into senseL and senseR (section 4). */
    fun sense(w: World, a: Ant, field: Field3) {
        a.senseL = sample(w, a, field, w.params.senseAngle)
        a.senseR = sample(w, a, field, -w.params.senseAngle)
    }

    /** Turns toward the stronger side in proportion to (L - R) / (L + R) (section 4). */
    fun steerByGradient(w: World, a: Ant, gain: Float) {
        val s = a.senseL + a.senseR
        if (s <= 0f) return
        turn(a, gain * (a.senseL - a.senseR) / s * DT)
    }

    /** The horizontal part of the forward vector dotted with (vx, vy). */
    fun horizontalDot(a: Ant, vx: Float, vy: Float): Float = a.fx * vx + a.fy * vy

    /** How much of the forward vector is horizontal, 0 (straight up or down) to 1. */
    fun horizontalForward(a: Ant): Float = sqrt(a.fx * a.fx + a.fy * a.fy)

    private fun sample(w: World, a: Ant, field: Field3, angle: Float): Float {
        val c = cos(angle)
        val s = sin(angle)
        val cx = a.ny * a.fz - a.nz * a.fy
        val cy = a.nz * a.fx - a.nx * a.fz
        val cz = a.nx * a.fy - a.ny * a.fx
        val ahead = w.params.senseAhead
        val p = w.pos
        p[0] = a.x + (a.fx * c + cx * s) * ahead
        p[1] = a.y + (a.fy * c + cy * s) * ahead
        p[2] = a.z + (a.fz * c + cz * s) * ahead
        w.surface.sdf.project(p)
        return field.get(p[0], p[1], p[2])
    }

    private fun normal(w: World, a: Ant) {
        val n = w.norm
        w.surface.sdf.gradient(a.x, a.y, a.z, n)
        a.nx = n[0]
        a.ny = n[1]
        a.nz = n[2]
    }

    /** Carries the forward vector into the tangent plane; rebuilds it at a sharp fold. */
    private fun transport(a: Ant) {
        val d = a.fx * a.nx + a.fy * a.ny + a.fz * a.nz
        var fx = a.fx - a.nx * d
        var fy = a.fy - a.ny * d
        var fz = a.fz - a.nz * d
        var len = sqrt(fx * fx + fy * fy + fz * fz)
        if (len < FOLD) {
            // The old forward points along the normal: fall back to the compass heading.
            fx = cos(a.heading)
            fy = sin(a.heading)
            val e = fx * a.nx + fy * a.ny
            fx -= a.nx * e
            fy -= a.ny * e
            fz = -a.nz * e
            len = sqrt(fx * fx + fy * fy + fz * fz)
            if (len < FOLD) {
                // The normal is horizontal along the heading: head up the face.
                fx = -a.nx * a.nz
                fy = -a.ny * a.nz
                fz = 1f - a.nz * a.nz
                len = sqrt(fx * fx + fy * fy + fz * fz)
            }
        }
        a.fx = fx / len
        a.fy = fy / len
        a.fz = fz / len
        updateHeading(a)
    }

    private fun updateHeading(a: Ant) {
        if (a.fx * a.fx + a.fy * a.fy > 1e-6f) a.heading = atan2(a.fy, a.fx)
    }
}

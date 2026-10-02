package com.bydesigninteractive.ant.sim.ant

import com.bydesigninteractive.ant.sim.DT
import com.bydesigninteractive.ant.sim.World
import com.bydesigninteractive.ant.sim.world.Field3
import com.bydesigninteractive.ant.sim.world.SURFACE_MM
import kotlin.math.PI
import kotlin.math.abs
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
    private const val STEEP_NZ = 0.2f

    /** Puts a surface ant on the surface at (x, y), facing compass angle [heading]. */
    fun place(w: World, a: Ant, x: Float, y: Float, heading: Float) {
        val p = w.pos
        p[0] = x
        p[1] = y
        p[2] = w.surface.ground.height(x, y)
        val open = onOpenGround(w, p, w.norm)
        if (!open) w.surface.sdf.project(p)
        a.x = p[0]
        a.y = p[1]
        a.z = p[2]
        a.heading = heading
        if (open) takeNormal(w, a) else normal(w, a)
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
        val open = onOpenGround(w, p, w.norm)
        if (!open) w.surface.sdf.project(p)
        a.x = p[0]
        a.y = p[1]
        a.z = p[2]
        if (open) takeNormal(w, a) else normal(w, a)
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

    /**
     * Faces compass angle [angle]. On a slope the forward vector is the tangent whose horizontal
     * part points exactly along the compass angle; on a steep face (|nz| of 0.2 or less) it falls
     * back to projecting the horizontal direction into the tangent plane.
     */
    fun faceCompass(a: Ant, angle: Float) {
        val c = cos(angle)
        val s = sin(angle)
        if (abs(a.nz) <= STEEP_NZ) {
            faceDirection(a, c, s, 0f)
            return
        }
        val fz = -(c * a.nx + s * a.ny) / a.nz
        val len = sqrt(1f + fz * fz)
        a.fx = c / len
        a.fy = s / len
        a.fz = fz / len
        updateHeading(a)
    }

    /** A search walk: straight runs of exponential length, then a random turn (section 5). */
    fun wander(w: World, a: Ant, runMean: Float, turnSd: Float) {
        a.runLeft -= w.params.surfaceSpeed * DT
        if (a.runLeft > 0f) return
        turn(a, w.gaussian() * turnSd)
        a.runLeft = w.exponential(runMean)
    }

    /** Samples [field] ahead-left and ahead-right on the surface into senseL and senseR (section 4). */
    fun sense(w: World, a: Ant, field: Field3) {
        a.senseL = sample(w, a, field, w.senseCos[0], w.senseSin[0])
        a.senseR = sample(w, a, field, w.senseCos[1], w.senseSin[1])
    }

    /** The horizontal part of the forward vector dotted with (vx, vy). */
    fun horizontalDot(a: Ant, vx: Float, vy: Float): Float = a.fx * vx + a.fy * vy

    /** How much of the forward vector is horizontal, 0 (straight up or down) to 1. */
    fun horizontalForward(a: Ant): Float = sqrt(a.fx * a.fx + a.fy * a.fy)

    private fun sample(w: World, a: Ant, field: Field3, c: Float, s: Float): Float {
        val cx = a.ny * a.fz - a.nz * a.fy
        val cy = a.nz * a.fx - a.nx * a.fz
        val cz = a.nx * a.fy - a.ny * a.fx
        val ahead = w.params.senseAhead
        val p = w.pos
        p[0] = a.x + (a.fx * c + cx * s) * ahead
        p[1] = a.y + (a.fy * c + cy * s) * ahead
        p[2] = a.z + (a.fz * c + cz * s) * ahead
        // An antenna only needs to be near the surface, so one Newton step is enough.
        if (!onOpenGround(w, p, null)) w.surface.sdf.project(p, iterations = 1)
        return field.get(p[0], p[1], p[2])
    }

    /**
     * On open ground, puts the point (p[0], p[1]) onto the surface, z = h(x, y), with the unit
     * normal (-hx, -hy, 1) into [n] if given, from one height-and-slope sample, and returns true.
     * Near any shape it returns false and leaves the point alone (the caller projects it).
     */
    private fun onOpenGround(w: World, p: FloatArray, n: FloatArray?): Boolean {
        val sdf = w.surface.sdf
        if (!sdf.isOpenGround(p[0], p[1])) {
            sdf.fastMisses++
            return false
        }
        sdf.fastHits++
        val s = w.slope
        p[2] = w.surface.ground.heightAndSlope(p[0], p[1], s)
        if (n != null) {
            val len = sqrt(s[0] * s[0] + s[1] * s[1] + 1f)
            n[0] = -s[0] / len
            n[1] = -s[1] / len
            n[2] = 1f / len
        }
        return true
    }

    private fun takeNormal(w: World, a: Ant) {
        a.nx = w.norm[0]
        a.ny = w.norm[1]
        a.nz = w.norm[2]
    }

    private fun normal(w: World, a: Ant) {
        val n = w.norm
        w.surface.sdf.gradient(a.x, a.y, a.z, n)
        a.nx = n[0]
        a.ny = n[1]
        a.nz = n[2]
    }

    /** Carries the forward vector into the tangent plane; rebuilds it at a sharp fold. */
    internal fun transport(a: Ant) {
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
                // The normal is horizontal along the heading. Head up a wall the ant walked into
                // (the old forward pointed against the normal) and down over a convex edge.
                val up = if (d > 0f) -1f else 1f
                fx = -a.nx * a.nz * up
                fy = -a.ny * a.nz * up
                fz = (1f - a.nz * a.nz) * up
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

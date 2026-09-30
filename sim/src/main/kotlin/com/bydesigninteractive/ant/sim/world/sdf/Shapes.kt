package com.bydesigninteractive.ant.sim.world.sdf

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

/** Polynomial smooth minimum: min(a, b), rounded over a blend width [k] (Inigo Quilez). */
fun smin(a: Float, b: Float, k: Float): Float {
    val h = max(k - abs(a - b), 0f) / k
    return min(a, b) - h * h * k * 0.25f
}

/**
 * A lumpy, axis-aligned ellipsoid: a pebble, rock or food lump. [lump] is the bump amplitude as a
 * fraction of the smallest radius, so rocks do not look machined; [phase] varies the pattern.
 * The distance is Quilez's ellipsoid bound, exact for a sphere.
 */
class Blob(
    val cx: Float,
    val cy: Float,
    val cz: Float,
    val rx: Float,
    val ry: Float,
    val rz: Float,
    private val lump: Float = 0f,
    private val phase: Float = 0f,
) {
    private val rMin = min(rx, min(ry, rz))

    /** Half the size of the blob's bounding box, bumps included. */
    val reach = max(rx, max(ry, rz)) + lump * rMin
    private val frequency = LUMP_FREQUENCY / reach

    fun near(x: Float, y: Float, z: Float, margin: Float): Boolean {
        val r = reach + margin
        return abs(x - cx) <= r && abs(y - cy) <= r && abs(z - cz) <= r
    }

    fun distance(x: Float, y: Float, z: Float): Float {
        val px = x - cx
        val py = y - cy
        val pz = z - cz
        val ax = px / rx
        val ay = py / ry
        val az = pz / rz
        val k0 = sqrt(ax * ax + ay * ay + az * az)
        val bx = px / (rx * rx)
        val by = py / (ry * ry)
        val bz = pz / (rz * rz)
        val k1 = sqrt(bx * bx + by * by + bz * bz)
        val d = if (k1 < 1e-6f) -rMin else k0 * (k0 - 1f) / k1
        if (lump == 0f) return d
        val bumps = sin(px * frequency + phase) * sin(py * frequency + phase * 1.7f) * sin(pz * frequency + phase * 2.3f)
        return d - lump * rMin * bumps
    }

    private companion object {
        const val LUMP_FREQUENCY = 5f
    }
}

/** A vertical capsule around (x, y) from [z0] to [z1]: a plant stem. */
class Stem(val x: Float, val y: Float, val z0: Float, val z1: Float, val radius: Float) {
    fun near(px: Float, py: Float, pz: Float, margin: Float): Boolean {
        val r = radius + margin
        return abs(px - x) <= r && abs(py - y) <= r && pz >= z0 - r && pz <= z1 + r
    }

    fun distance(px: Float, py: Float, pz: Float): Float {
        val dx = px - x
        val dy = py - y
        val dz = pz - pz.coerceIn(z0, z1)
        return sqrt(dx * dx + dy * dy + dz * dz) - radius
    }
}

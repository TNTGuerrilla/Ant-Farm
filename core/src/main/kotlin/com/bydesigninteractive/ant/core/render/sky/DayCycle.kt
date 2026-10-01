package com.bydesigninteractive.ant.core.render.sky

import kotlin.math.sqrt

/**
 * The sky through one simulated day: night, dawn, midday, golden afternoon and dusk keyframes,
 * blended linearly and wrapping at midnight. A day is [DAY_SECONDS] of simulation time (60 real
 * minutes per day at 1x, the owner's choice in M1b-2c); the run starts at [START], mid-morning. Night keeps a
 * dim blue moonlight and a raised blue ambient so the scene stays readable.
 */
object DayCycle {
    const val DAY_SECONDS = 3600f
    const val START = 0.38f

    private class Key(
        val t: Float,
        val sun: FloatArray, val sunColor: FloatArray, val ambient: FloatArray,
        val top: FloatArray, val horizon: FloatArray, val fog: FloatArray,
        val fogStart: Float, val fogEnd: Float,
    )

    private fun v(x: Float, y: Float, z: Float) = floatArrayOf(x, y, z)

    private val keys = arrayOf(
        Key(0.00f, v(0.3f, 0.2f, -0.93f), v(0.25f, 0.30f, 0.45f), v(0.26f, 0.30f, 0.44f), v(0.05f, 0.08f, 0.18f), v(0.12f, 0.16f, 0.28f), v(0.12f, 0.15f, 0.25f), 400f, 1300f),
        Key(0.25f, v(-0.9f, 0.1f, -0.3f), v(1.00f, 0.70f, 0.50f), v(0.40f, 0.35f, 0.35f), v(0.45f, 0.55f, 0.75f), v(0.98f, 0.72f, 0.55f), v(0.85f, 0.70f, 0.60f), 600f, 1500f),
        Key(0.50f, v(-0.25f, 0.15f, -0.95f), v(1.00f, 0.98f, 0.92f), v(0.50f, 0.50f, 0.52f), v(0.60f, 0.75f, 0.88f), v(0.85f, 0.88f, 0.85f), v(0.82f, 0.84f, 0.82f), 700f, 1600f),
        Key(0.68f, v(0.8f, -0.2f, -0.5f), v(1.00f, 0.80f, 0.55f), v(0.45f, 0.40f, 0.36f), v(0.50f, 0.60f, 0.80f), v(0.96f, 0.82f, 0.60f), v(0.90f, 0.80f, 0.65f), 650f, 1550f),
        Key(0.78f, v(0.95f, -0.1f, -0.15f), v(0.90f, 0.50f, 0.40f), v(0.32f, 0.28f, 0.35f), v(0.25f, 0.28f, 0.50f), v(0.85f, 0.50f, 0.45f), v(0.55f, 0.42f, 0.45f), 550f, 1450f),
    )

    /** The time of day, 0 to 1 (0 midnight, 0.5 noon), for [seconds] of simulation. */
    fun timeOfDay(seconds: Float): Float {
        val t = (seconds / DAY_SECONDS + START) % 1f
        return if (t < 0f) t + 1f else t
    }

    /** Fills [out] with the sky at time of day [t] and returns it. */
    fun sky(t: Float, out: SkyState): SkyState {
        var i = keys.size - 1
        for (k in keys.indices) if (keys[k].t <= t) i = k
        val a = keys[i]
        val b = keys[(i + 1) % keys.size]
        val span = (if (b.t > a.t) b.t else b.t + 1f) - a.t
        val f = ((t - a.t) / span).coerceIn(0f, 1f)
        lerp(a.sun, b.sun, f, out.sunDir)
        val len = sqrt(out.sunDir[0] * out.sunDir[0] + out.sunDir[1] * out.sunDir[1] + out.sunDir[2] * out.sunDir[2])
        for (k in 0 until 3) out.sunDir[k] /= len
        lerp(a.sunColor, b.sunColor, f, out.sunColor)
        lerp(a.ambient, b.ambient, f, out.ambient)
        lerp(a.top, b.top, f, out.skyTop)
        lerp(a.horizon, b.horizon, f, out.skyHorizon)
        lerp(a.fog, b.fog, f, out.fogColor)
        out.fogStart = a.fogStart + (b.fogStart - a.fogStart) * f
        out.fogEnd = a.fogEnd + (b.fogEnd - a.fogEnd) * f
        return out
    }

    private fun lerp(a: FloatArray, b: FloatArray, f: Float, out: FloatArray) {
        for (k in 0 until 3) out[k] = a[k] + (b[k] - a[k]) * f
    }
}

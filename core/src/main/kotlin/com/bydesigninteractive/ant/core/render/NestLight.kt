package com.bydesigninteractive.ant.core.render

import com.bydesigninteractive.ant.core.render.sky.SkyState
import kotlin.math.max

/** Pure lighting for the nest view's above-ground parts; no GL, so it tests on the JVM. */
object NestLight {
    /**
     * Fills [out] with a flat ground colour ([r], [g], [b]) lit like a surface in the 3D view:
     * colour times (ambient + sunColor * max(0, -sunDir.z)) * [Shaders.EXPOSURE], clamped to 0..1.
     */
    fun lit(sky: SkyState, r: Float, g: Float, b: Float, out: FloatArray): FloatArray {
        val sun = max(0f, -sky.sunDir[2])
        out[0] = (r * (sky.ambient[0] + sky.sunColor[0] * sun) * Shaders.EXPOSURE).coerceIn(0f, 1f)
        out[1] = (g * (sky.ambient[1] + sky.sunColor[1] * sun) * Shaders.EXPOSURE).coerceIn(0f, 1f)
        out[2] = (b * (sky.ambient[2] + sky.sunColor[2] * sun) * Shaders.EXPOSURE).coerceIn(0f, 1f)
        return out
    }
}

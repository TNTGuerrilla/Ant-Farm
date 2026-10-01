package com.bydesigninteractive.ant.core.render.sky

import kotlin.math.max

/**
 * Everything the world shader needs about the sky: a directional light (the sun, or the moon at
 * night), ambient light, the sky gradient and distance fog. The day cycle fills one; a weather
 * system can later fill or adjust one without touching the renderers. Colours are linear RGB 0..1.
 */
class SkyState {
    /** The direction the light travels (pointing down for an overhead sun), unit length. */
    val sunDir = floatArrayOf(0f, 0f, -1f)
    val sunColor = FloatArray(3)
    val ambient = FloatArray(3)
    val skyTop = FloatArray(3)
    val skyHorizon = FloatArray(3)
    val fogColor = FloatArray(3)
    var fogStart = 600f
    var fogEnd = 1500f

    /** Overall brightness of flat ground: ambient plus the light's contribution, by luminance. */
    fun brightness(): Float = lum(ambient) + lum(sunColor) * max(0f, -sunDir[2])

    fun copyFrom(o: SkyState) {
        o.sunDir.copyInto(sunDir)
        o.sunColor.copyInto(sunColor)
        o.ambient.copyInto(ambient)
        o.skyTop.copyInto(skyTop)
        o.skyHorizon.copyInto(skyHorizon)
        o.fogColor.copyInto(fogColor)
        fogStart = o.fogStart
        fogEnd = o.fogEnd
    }

    private fun lum(c: FloatArray) = 0.2126f * c[0] + 0.7152f * c[1] + 0.0722f * c[2]
}

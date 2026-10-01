package com.bydesigninteractive.ant.core.render

import com.bydesigninteractive.ant.core.render.sky.DayCycle
import com.bydesigninteractive.ant.core.render.sky.SkyState
import kotlin.test.Test
import kotlin.test.assertTrue

class NestLightTest {
    private fun strip(t: Float): FloatArray {
        val sky = DayCycle.sky(t, SkyState())
        return NestLight.lit(sky, 0.30f, 0.45f, 0.20f, FloatArray(3))
    }

    @Test
    fun noonStripIsBrighterThanMidnightAndBothAreInRange() {
        val noon = strip(0.5f)
        val night = strip(0.0f)
        assertTrue(noon.sum() > night.sum())
        for (c in noon + night) assertTrue(c in 0f..1f)
    }
}

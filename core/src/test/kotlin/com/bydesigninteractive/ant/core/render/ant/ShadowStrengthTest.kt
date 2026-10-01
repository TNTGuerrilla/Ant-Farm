package com.bydesigninteractive.ant.core.render.ant

import com.bydesigninteractive.ant.core.render.sky.DayCycle
import com.bydesigninteractive.ant.core.render.sky.SkyState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ShadowStrengthTest {
    @Test
    fun noonIsFullAndMidnightIsFaint() {
        val noon = AntRenderer.shadowStrength(DayCycle.sky(0.5f, SkyState()))
        val midnight = AntRenderer.shadowStrength(DayCycle.sky(0f, SkyState()))
        assertEquals(0.45f, noon, 0.02f)
        assertTrue(midnight <= 0.15f, "midnight shadow strength was $midnight")
    }
}

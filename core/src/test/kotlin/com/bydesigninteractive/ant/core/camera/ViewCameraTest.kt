package com.bydesigninteractive.ant.core.camera

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ViewCameraTest {
    private val cam = ViewCamera()

    @Test
    fun neverZoomsOutPastTheReadableLimit() {
        // 5 mm ant at 8 px on a 1080p screen: 0.625 mm per pixel at most.
        assertEquals(0.625f, cam.fitZoom(1200f, 1000f, 1920, 1080), 1e-5f)
    }

    @Test
    fun smallAreasAreFramedClose() {
        assertEquals(5f / 60f, cam.fitZoom(10f, 10f, 1920, 1080), 1e-5f)
        assertEquals(340f / 1080f, cam.fitZoom(200f, 300f, 1920, 1080), 1e-5f)
    }

    @Test
    fun limitsScaleWithScreenHeight() {
        assertEquals(cam.maxMmPerPx(1080) * 2, cam.maxMmPerPx(540), 1e-5f)
    }

    @Test
    fun theFirstFrameSnapsAndLaterFramesEase() {
        cam.frame(0f, 0f, 100f, 100f, 1920, 1080, 0.016f)
        assertEquals(50f, cam.centerX)
        cam.frame(100f, 0f, 200f, 100f, 1920, 1080, 0.016f)
        assertTrue(cam.centerX > 50f && cam.centerX < 150f)
    }

    @Test
    fun zoomStaysWithinLimits() {
        cam.placeAt(0f, 0f, 0.3f)
        repeat(50) { cam.zoomBy(2f, 1080) }
        assertEquals(cam.maxMmPerPx(1080), cam.mmPerPx, 1e-5f)
    }
}

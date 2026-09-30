package com.bydesigninteractive.ant.sim.world.sdf

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ShapesTest {
    @Test
    fun smoothMinIsTheMinimumAwayFromTheBlend() {
        assertEquals(2f, smin(2f, 10f, 1.5f))
        assertEquals(smin(3f, 3.5f, 1.5f), smin(3.5f, 3f, 1.5f))
        assertTrue(smin(3f, 3f, 1.5f) < 3f)
    }

    @Test
    fun aRoundBlobIsAnExactSphere() {
        val b = Blob(10f, 20f, 30f, 5f, 5f, 5f)
        assertEquals(5f, b.distance(20f, 20f, 30f), 1e-4f)
        assertEquals(-5f, b.distance(10f, 20f, 30f), 1e-4f)
        assertEquals(0f, b.distance(10f, 20f, 35f), 1e-4f)
    }

    @Test
    fun aFlatBlobIsNearerAlongItsShortAxis() {
        val b = Blob(0f, 0f, 0f, 10f, 10f, 4f)
        assertTrue(b.distance(0f, 0f, 6f) < b.distance(12f, 0f, 0f) + 0.5f)
        assertEquals(0f, b.distance(0f, 0f, 4f), 0.05f)
    }

    @Test
    fun lumpsStayWithinTheirAmplitude() {
        val plain = Blob(0f, 0f, 0f, 20f, 20f, 20f)
        val lumpy = Blob(0f, 0f, 0f, 20f, 20f, 20f, lump = 0.1f, phase = 1f)
        for (i in 0 until 50) {
            val x = 25f * kotlin.math.cos(i * 0.7f)
            val z = 25f * kotlin.math.sin(i * 0.7f)
            assertTrue(kotlin.math.abs(lumpy.distance(x, 3f, z) - plain.distance(x, 3f, z)) <= 2.0001f)
        }
    }

    @Test
    fun nearUsesTheBoundsPlusMargin() {
        val b = Blob(0f, 0f, 0f, 10f, 10f, 10f)
        assertTrue(b.near(14f, 0f, 0f, 5f))
        assertFalse(b.near(16f, 0f, 0f, 5f))
    }

    @Test
    fun aStemIsAVerticalCapsule() {
        val s = Stem(0f, 0f, 0f, 100f, 2f)
        assertEquals(3f, s.distance(5f, 0f, 50f), 1e-4f)
        assertEquals(8f, s.distance(0f, 0f, 110f), 1e-4f)
        assertEquals(-2f, s.distance(0f, 0f, 50f), 1e-4f)
        assertTrue(s.near(4f, 0f, 50f, 3f))
        assertFalse(s.near(0f, 0f, 120f, 3f))
    }
}

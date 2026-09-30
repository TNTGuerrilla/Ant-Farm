package com.bydesigninteractive.ant.sim.world.sdf

import com.bydesigninteractive.ant.sim.world.ChunkedField
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class HeightFieldTest {
    private val spoil = ChunkedField()
    private val ground = HeightField(4, spoil)

    @Test
    fun theSameSeedGivesTheSameGround() {
        val other = HeightField(4, ChunkedField())
        for (i in 0 until 200) {
            val x = 3000f + i * 7.3f
            val y = 4100f - i * 3.1f
            assertEquals(ground.base(x, y), other.base(x, y))
        }
    }

    @Test
    fun reliefStaysWithinItsAmplitude() {
        for (i in 0 until 2000) {
            val h = ground.base(1000f + i * 3.7f, 2000f + i * 2.9f)
            assertTrue(abs(h) <= 16.5f, "height $h")
        }
    }

    @Test
    fun theGroundIsContinuousAcrossChunkEdges() {
        for (y in listOf(1234f, 4000f, 6789f)) {
            assertEquals(ground.base(499.9f, y), ground.base(500.1f, y), 0.2f)
            assertEquals(ground.base(y, 3999.9f), ground.base(y, 4000.1f), 0.2f)
        }
    }

    @Test
    fun spoilRaisesTheGround() {
        val before = ground.height(4005f, 4005f)
        spoil.add(4005f, 4005f, 100f)
        assertEquals(before + 100 * PELLET_HEIGHT, ground.height(4005f, 4005f), 1e-4f)
    }

    @Test
    fun slopeMatchesTheHeightDifferences() {
        val out = FloatArray(2)
        ground.slope(3210f, 4321f, out)
        val dx = (ground.height(3211f, 4321f) - ground.height(3209f, 4321f)) / 2f
        assertEquals(dx, out[0], 1e-3f)
    }

    @Test
    fun distanceIsZeroOnTheGroundAndPositiveAbove() {
        val h = ground.height(2500f, 2500f)
        assertEquals(0f, ground.distance(2500f, 2500f, h), 1e-4f)
        assertTrue(ground.distance(2500f, 2500f, h + 5f) in 4f..5.0001f)
        assertTrue(ground.distance(2500f, 2500f, h - 5f) < 0f)
    }
}

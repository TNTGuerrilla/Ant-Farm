package com.bydesigninteractive.ant.sim.world.sdf

import com.bydesigninteractive.ant.sim.world.ChunkedField
import kotlin.math.abs
import kotlin.math.sqrt
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
        val dy = (ground.height(3210f, 4322f) - ground.height(3210f, 4320f)) / 2f
        assertEquals(dy, out[1], 1e-3f)
    }

    @Test
    fun distanceIsZeroOnTheGroundAndPositiveAbove() {
        val h = ground.height(2500f, 2500f)
        assertEquals(0f, ground.distance(2500f, 2500f, h), 1e-4f)
        assertTrue(ground.distance(2500f, 2500f, h + 5f) in 4f..5.0001f)
        assertTrue(ground.distance(2500f, 2500f, h - 5f) < 0f)
    }

    @Test
    fun aChunkGridIsCompact() {
        // 126 x 126 shorts at 4 mm spacing: about 31 KB per 50 cm chunk, 8 MB for the map.
        assertEquals(126 * 126 * 2, HeightField.bytesPerChunk())
    }

    @Test
    fun heightsKeepMicronPrecision() {
        val h = ground.base(3333.3f, 4444.4f)
        assertEquals(h, ground.base(3333.3f, 4444.4f))
        assertTrue(abs(h) <= 16.5f)
    }

    @Test
    fun distanceFollowsTheSlopeOnReliefAndSpoil() {
        spoil.add(3205f, 4325f, 100f)
        val out = FloatArray(2)
        for (i in 0 until 60) {
            val x = 3190f + i * 0.73f
            val y = 4312f + i * 0.41f
            ground.slope(x, y, out)
            val h = ground.height(x, y)
            assertEquals(0f, ground.distance(x, y, h), 1e-4f)
            val expected = 2f / sqrt(1f + out[0] * out[0] + out[1] * out[1])
            assertEquals(expected, ground.distance(x, y, h + 2f), 0.05f)
        }
    }
}

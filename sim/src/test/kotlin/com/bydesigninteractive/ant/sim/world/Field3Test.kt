package com.bydesigninteractive.ant.sim.world

import kotlin.math.exp
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class Field3Test {
    @Test
    fun aVoxelCenterReadsBackWhatWasAdded() {
        val f = Field3(0.05f, 0f, 0f)
        f.add(55f, 55f, 55f, 2f)
        assertEquals(2f, f.get(55f, 55f, 55f), 1e-5f)
        assertEquals(1, f.blockCount())
    }

    @Test
    fun depositsBetweenVoxelsAreSplitTrilinearly() {
        val f = Field3(0.05f, 0f, 0f)
        f.add(10f, 5f, 5f, 1f) // halfway between voxels 0 and 1 in x
        assertEquals(0.5f, f.get(5f, 5f, 5f), 1e-5f)
        assertEquals(0.5f, f.get(15f, 5f, 5f), 1e-5f)
        assertEquals(0.5f, f.get(10f, 5f, 5f), 1e-5f)
    }

    @Test
    fun negativeHeightsWork() {
        val f = Field3(0.05f, 0f, 0f)
        f.add(5f, 5f, -15f, 1f)
        assertEquals(1f, f.get(5f, 5f, -15f), 1e-5f)
    }

    @Test
    fun lazyDecayCatchesUpOnRead() {
        val f = Field3(1f, 0.01f, 0f)
        f.add(55f, 55f, 55f, 1f)
        repeat(50) { f.step() }
        assertEquals(exp(-0.5f), f.get(55f, 55f, 55f), 1e-5f)
    }

    @Test
    fun fadedBlocksAreSweptAway() {
        val f = Field3(1f, 1f, 0f)
        f.add(55f, 55f, 55f, 1f)
        repeat(200) { f.step() }
        assertEquals(0, f.blockCount())
    }

    @Test
    fun diffusionSpreadsToSixNeighborsAndKeepsTheTotal() {
        val f = Field3(0.05f, 0f, 1f)
        f.add(55f, 55f, 55f, 1f) // voxel (5, 5, 5)
        f.step()
        assertEquals(0.7f, f.get(55f, 55f, 55f), 1e-5f)
        assertEquals(0.05f, f.get(65f, 55f, 55f), 1e-5f)
        assertEquals(0.05f, f.get(55f, 55f, 45f), 1e-5f)
    }

    @Test
    fun diffusionCrossesBlockFaces() {
        val f = Field3(0.05f, 0f, 1f)
        f.add(95f, 55f, 55f, 1f) // voxel (9, 5, 5), the high-x face of block (0, 0, 0)
        f.add(105f, 55f, 55f, 0f) // allocates block (1, 0, 0)
        f.add(55f, 55f, 5f, 1f) // voxel (5, 5, 0), the low-z face of block (0, 0, 0)
        f.add(55f, 55f, -5f, 0f) // allocates block (0, 0, -1)
        f.step()
        assertEquals(0.7f, f.get(95f, 55f, 55f), 1e-5f)
        assertEquals(0.05f, f.get(105f, 55f, 55f), 1e-5f)
        assertEquals(0.05f, f.get(85f, 55f, 55f), 1e-5f)
        assertEquals(0.7f, f.get(55f, 55f, 5f), 1e-5f)
        assertEquals(0.05f, f.get(55f, 55f, -5f), 1e-5f)
        f.step()
        // The spread back from the neighbor block arrives too: 0.7 * 0.7 + 6 * 0.05 * 0.05.
        assertEquals(0.505f, f.get(95f, 55f, 55f), 1e-5f)
    }

    @Test
    fun eagerFieldsDecayEveryStep() {
        val f = Field3(1f, 0.01f, 0.1f)
        f.add(55f, 55f, 55f, 1f)
        f.step()
        assertTrue(f.max() < 1f)
    }

    @Test
    fun peekReadsTheSameValueAsGet() {
        val f = Field3(1f, 0.01f, 0f)
        f.add(52f, 57f, 55f, 1f)
        repeat(50) { f.step() }
        val peeked = f.peek(58f, 51f, 53f)
        assertTrue(peeked > 0f)
        assertEquals(f.get(58f, 51f, 53f), peeked)
    }

    @Test
    fun readsWithoutSideEffectsLeaveTheFieldUnchanged() {
        fun script(f: Field3, render: Boolean): List<Float> {
            val out = FloatArray(50 * 50)
            val seen = ArrayList<Float>()
            f.add(52f, 57f, 55f, 1f)
            f.add(147f, 33f, -4f, 0.7f)
            repeat(300) { t ->
                f.step()
                if (render) {
                    f.peek(52f, 57f, 55f)
                    f.max()
                    f.projectMax(0f, 0f, 50, out)
                }
                if (t % 97 == 0) {
                    seen += f.get(52f, 57f, 55f)
                    f.add(149f, 31f, -2f, 0.3f)
                }
            }
            seen += f.get(52f, 57f, 55f)
            seen += f.get(147f, 33f, -4f)
            return seen
        }
        val plain = script(Field3(0.05f, 0.37f, 0f), render = false)
        val rendered = script(Field3(0.05f, 0.37f, 0f), render = true)
        assertEquals(plain, rendered)
    }

    @Test
    fun maxAndProjectMaxSeeLazyDecay() {
        val f = Field3(1f, 0.01f, 0f)
        f.add(55f, 55f, 55f, 1f)
        repeat(50) { f.step() }
        assertEquals(exp(-0.5f), f.max(), 1e-5f)
        val out = FloatArray(50 * 50)
        assertTrue(f.projectMax(0f, 0f, 50, out))
        assertEquals(exp(-0.5f), out[5 * 50 + 5], 1e-5f)
    }

    @Test
    fun blocksAreReleasedBelowTheGivenStrength() {
        // Decay 0.01 per second: after 600 s a unit mark is e^-6, about 0.0025.
        val coarse = Field3(1f, 0.01f, 0f, releaseBelow = 1e-2f)
        val fine = Field3(1f, 0.01f, 0f)
        coarse.add(55f, 55f, 55f, 1f)
        fine.add(55f, 55f, 55f, 1f)
        repeat(600) {
            coarse.step()
            fine.step()
        }
        assertEquals(0, coarse.blockCount())
        assertEquals(1, fine.blockCount())
    }

    @Test
    fun projectMaxLooksStraightDown() {
        val f = Field3(0.05f, 0f, 0f)
        f.add(105f, 205f, -5f, 2f)
        f.add(105f, 205f, 305f, 3f)
        val out = FloatArray(50 * 50)
        assertTrue(f.projectMax(0f, 0f, 50, out))
        assertEquals(3f, out[20 * 50 + 10], 1e-5f)
    }
}

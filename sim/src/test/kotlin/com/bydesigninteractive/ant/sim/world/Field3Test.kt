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
    fun eagerFieldsDecayEveryStep() {
        val f = Field3(1f, 0.01f, 0.1f)
        f.add(55f, 55f, 55f, 1f)
        f.step()
        assertTrue(f.max() < 1f)
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

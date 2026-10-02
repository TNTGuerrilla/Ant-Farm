package com.bydesigninteractive.ant.sim.brain

import java.lang.management.ManagementFactory
import kotlin.math.tanh
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BrainTest {
    /** 2 inputs, 2 hidden units, 1 output, with a few weights set by hand. */
    private fun small(recurrent: Float): Brain {
        val w = FloatArray(Brain.size(2, 2, 1))
        w[Brain.inputWeight(2, 2, 0, 0)] = 0.5f
        w[Brain.inputWeight(2, 2, 0, 1)] = -1f
        w[Brain.inputWeight(2, 2, 1, 1)] = 2f
        w[Brain.recurrentWeight(2, 2, 0, 1)] = recurrent
        w[Brain.hiddenBias(2, 2, 0)] = 0.1f
        w[Brain.outputWeight(2, 2, 0, 0)] = 1f
        w[Brain.outputWeight(2, 2, 0, 1)] = -1f
        w[Brain.outputBias(2, 2, 1, 0)] = 0.2f
        return Brain(2, 2, 1, w)
    }

    @Test
    fun theLayoutCountsEveryWeightOnce() {
        assertEquals(2 * 3 + 2 * 2 + 2 + 1 * 2 + 1, Brain.size(3, 2, 1))
        assertEquals(Brain.size(3, 2, 1) - 1, Brain.outputBias(3, 2, 1, 0))
    }

    @Test
    fun knownWeightsGiveKnownOutputs() {
        val b = small(recurrent = 0f)
        val x = floatArrayOf(1f, 0.5f)
        val h = FloatArray(2)
        val s = FloatArray(2)
        val out = FloatArray(1)
        b.evaluate(x, h, s, out)
        val h0 = tanh(0.5f * 1f - 1f * 0.5f + 0.1f)
        val h1 = tanh(2f * 0.5f)
        assertEquals(h0, h[0], 1e-6f)
        assertEquals(h1, h[1], 1e-6f)
        assertEquals(0.2f + h0 - h1, out[0], 1e-6f)
    }

    @Test
    fun theHiddenUnitsSeeTheirOwnPreviousState() {
        val b = small(recurrent = 0.5f)
        val x = floatArrayOf(1f, 0.5f)
        val h = FloatArray(2)
        val s = FloatArray(2)
        val out = FloatArray(1)
        b.evaluate(x, h, s, out)
        val firstH1 = h[1]
        b.evaluate(x, h, s, out)
        assertEquals(tanh(0.1f + 0.5f * firstH1), h[0], 1e-6f)
    }

    @Test
    fun evaluatingAllocatesNothing() {
        val n = 36
        val b = Brain(n, 16, 13, FloatArray(Brain.size(n, 16, 13)) { ((it * 37) % 11 - 5) * 0.05f })
        val x = FloatArray(n) { it * 0.01f }
        val h = FloatArray(16)
        val s = FloatArray(16)
        val out = FloatArray(13)
        repeat(20_000) { b.evaluate(x, h, s, out) } // warm up the JIT
        val first = out.copyOf()
        val mx = ManagementFactory.getThreadMXBean() as com.sun.management.ThreadMXBean
        val id = Thread.currentThread().id
        val before = mx.getThreadAllocatedBytes(id)
        repeat(100_000) { b.evaluate(x, h, s, out) }
        val allocated = mx.getThreadAllocatedBytes(id) - before
        assertTrue(allocated < 64 * 1024, "evaluate allocated $allocated bytes over 100,000 calls")
        assertTrue(out.all { it.isFinite() } && first.all { it.isFinite() })
    }
}

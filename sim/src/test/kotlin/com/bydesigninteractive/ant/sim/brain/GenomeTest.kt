package com.bydesigninteractive.ant.sim.brain

import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GenomeTest {
    private fun genome(): Genome {
        val n = Brain.size(4, 3, 5)
        return Genome(4, 3, 5, FloatArray(n) { (it - n / 2) * 0.137f + 1e-7f * it }, "test-1")
    }

    @Test
    fun savingAndLoadingKeepsEveryBit() {
        val g = genome()
        val back = Genome.read(g.write())
        assertEquals("test-1", back.layout)
        assertEquals(4, back.inputs)
        assertEquals(3, back.hidden)
        assertEquals(5, back.outputs)
        assertContentEquals(g.weights, back.weights)
        val file = Files.createTempFile("genome", ".genome")
        g.save(file)
        assertContentEquals(g.weights, Genome.load(file).weights)
        Files.deleteIfExists(file)
    }

    @Test
    fun aMalformedFileIsRejected() {
        assertFailsWith<IllegalArgumentException> { Genome.read("not a genome") }
        val text = genome().write().lines().dropLast(3).joinToString("\n")
        assertFailsWith<IllegalArgumentException> { Genome.read(text) }
    }

    @Test
    fun personalVariationIsSeededAndLeavesTheGatesExact() {
        val g = genome()
        val a = g.personal(worldSeed = 5, id = 3, biasFrom = 2).weights
        val again = g.personal(worldSeed = 5, id = 3, biasFrom = 2).weights
        val other = g.personal(worldSeed = 5, id = 4, biasFrom = 2).weights
        assertContentEquals(a, again)
        assertFalse(a.contentEquals(other))
        // Input weights vary; recurrent, hidden-bias and hidden-to-output weights do not.
        assertTrue((0 until 3 * 4).any { a[it] != g.weights[it] })
        for (k in 3 * 4 until Brain.outputBias(4, 3, 5, 0)) assertEquals(g.weights[k], a[k], "weight $k")
        // Output biases below biasFrom stay exact; those from biasFrom on vary.
        for (o in 0 until 2) assertEquals(g.weights[Brain.outputBias(4, 3, 5, o)], a[Brain.outputBias(4, 3, 5, o)])
        assertTrue((2 until 5).any { a[Brain.outputBias(4, 3, 5, it)] != g.weights[Brain.outputBias(4, 3, 5, it)] })
    }

    @Test
    fun theHashGaussianLooksNormal() {
        var sum = 0.0
        var sq = 0.0
        val n = 20_000
        for (i in 0 until n) {
            val v = Genome.gaussian(9L, 1, i).toDouble()
            sum += v
            sq += v * v
        }
        val mean = sum / n
        val sd = kotlin.math.sqrt(sq / n - mean * mean)
        assertTrue(kotlin.math.abs(mean) < 0.03, "mean $mean")
        assertTrue(sd in 0.97..1.03, "sd $sd")
    }
}

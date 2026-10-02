package com.bydesigninteractive.ant.sim.brain

import com.bydesigninteractive.ant.sim.persist.AtomicFile
import com.bydesigninteractive.ant.sim.util.unit
import java.nio.ByteBuffer
import java.nio.file.Files
import java.nio.file.Path
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.sqrt

/**
 * A colony's brain weights (spec 2.5) for networks of the given size, tagged with the input
 * [layout] they were made for (`Senses.LAYOUT`), so a genome saved for other inputs is never
 * misread. Treat it as immutable: [brain] and [personal] copy the weights.
 */
class Genome(val inputs: Int, val hidden: Int, val outputs: Int, val weights: FloatArray, val layout: String) {
    init {
        require(weights.size == Brain.size(inputs, hidden, outputs)) {
            "expected ${Brain.size(inputs, hidden, outputs)} weights, got ${weights.size}"
        }
        require(layout.isNotEmpty() && layout.none { it.isWhitespace() }) { "bad layout tag '$layout'" }
    }

    /** The genome itself as a network. */
    fun brain(): Brain = Brain(inputs, hidden, outputs, weights.copyOf())

    /**
     * The network ant [id] of a world with [worldSeed] is born with: each hidden unit's input
     * weights scaled together by (1 + [weightSd] g), one g per unit, and the biases of outputs
     * [biasFrom] and up shifted by [biasSd] g, every g a standard normal from a seeded hash (no
     * draw from the world's generator). The bias spread is the spread in response thresholds.
     *
     * Scaling a unit's whole input row, not each weight on its own, changes how sensitive the unit
     * is but keeps the ratios between its inputs: a unit comparing the left and right antennae
     * stays balanced when both read the same, and a threshold set through the BIAS input stays
     * where it is. Recurrent, hidden-bias and hidden-to-output weights stay exact, because the seed
     * brain's mirror pairs share their gates and noise through them and cancel through them.
     */
    fun personal(
        worldSeed: Long,
        id: Int,
        biasFrom: Int,
        weightSd: Float = PERSONAL_WEIGHT_SD,
        biasSd: Float = PERSONAL_BIAS_SD,
    ): Brain {
        val w = weights.copyOf()
        val salt = worldSeed xor PERSONAL_SALT
        for (j in 0 until hidden) {
            val f = 1f + weightSd * gaussian(salt, id, j)
            for (i in 0 until inputs) w[j * inputs + i] *= f
        }
        for (o in biasFrom until outputs) {
            val k = Brain.outputBias(inputs, hidden, outputs, o)
            w[k] += biasSd * gaussian(salt, id, k)
        }
        return Brain(inputs, hidden, outputs, w)
    }

    /** The genome as text: three header lines, then one weight per line in [Brain]'s order. Floats round-trip exactly. */
    fun write(): String {
        val sb = StringBuilder()
        sb.append(MAGIC).append('\n')
        sb.append("layout ").append(layout).append('\n')
        sb.append("inputs ").append(inputs).append(" hidden ").append(hidden).append(" outputs ").append(outputs).append('\n')
        for (v in weights) sb.append(v.toString()).append('\n')
        return sb.toString()
    }

    /** Writes [write]'s text to [path] atomically. */
    fun save(path: Path) {
        AtomicFile.write(path, ByteBuffer.wrap(write().toByteArray(Charsets.UTF_8)))
    }

    companion object {
        const val MAGIC = "antfarm-genome 1"
        const val PERSONAL_WEIGHT_SD = 0.1f
        const val PERSONAL_BIAS_SD = 0.3f
        private const val PERSONAL_SALT = 0x6E0E5L

        /** Parses [write]'s format; throws IllegalArgumentException on anything else. */
        fun read(text: String): Genome {
            val lines = text.lines().map { it.trim() }.filter { it.isNotEmpty() }
            require(lines.size >= 3 && lines[0] == MAGIC) { "not a genome file" }
            require(lines[1].startsWith("layout ")) { "missing layout line" }
            val layout = lines[1].removePrefix("layout ").trim()
            val sizes = lines[2].split(' ')
            require(sizes.size == 6 && sizes[0] == "inputs" && sizes[2] == "hidden" && sizes[4] == "outputs") {
                "bad size line: ${lines[2]}"
            }
            val inputs = sizes[1].toInt()
            val hidden = sizes[3].toInt()
            val outputs = sizes[5].toInt()
            val n = Brain.size(inputs, hidden, outputs)
            require(lines.size == 3 + n) { "expected $n weights, found ${lines.size - 3}" }
            val w = FloatArray(n) { lines[3 + it].toFloat() }
            return Genome(inputs, hidden, outputs, w, layout)
        }

        fun load(path: Path): Genome = read(String(Files.readAllBytes(path), Charsets.UTF_8))

        /** A standard normal value from a seeded hash (Box-Muller on two hashed uniforms). */
        internal fun gaussian(seed: Long, a: Int, b: Int): Float {
            val u1 = unit(seed, a, 2 * b).coerceAtLeast(1e-12)
            val u2 = unit(seed, a, 2 * b + 1)
            return (sqrt(-2.0 * ln(u1)) * cos(2.0 * PI * u2)).toFloat()
        }
    }
}

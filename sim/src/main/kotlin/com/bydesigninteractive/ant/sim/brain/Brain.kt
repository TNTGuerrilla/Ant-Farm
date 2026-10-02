package com.bydesigninteractive.ant.sim.brain

import kotlin.math.tanh

/**
 * One ant's network (spec 2.2): an Elman network with one recurrent hidden layer of tanh units.
 * Each [evaluate] feeds the inputs and the hidden units' previous values into the hidden layer,
 * then the new hidden values into linear outputs; the caller squashes each output its own way
 * ([Outputs] in Actions.kt).
 *
 * The weights are one flat array in this order: input to hidden (one row of [inputs] per hidden
 * unit), hidden to hidden (one row of [hidden] per unit, indexed by the previous unit), hidden
 * biases, hidden to output (one row of [hidden] per output), output biases. The companion's
 * helpers give each weight's index. Evaluation allocates nothing.
 */
class Brain(val inputs: Int, val hidden: Int, val outputs: Int, val weights: FloatArray) {
    init {
        require(weights.size == size(inputs, hidden, outputs)) {
            "expected ${size(inputs, hidden, outputs)} weights, got ${weights.size}"
        }
    }

    private val recurrentBase = recurrentWeight(inputs, hidden, 0, 0)
    private val hiddenBiasBase = hiddenBias(inputs, hidden, 0)
    private val outputBase = outputWeight(inputs, hidden, 0, 0)
    private val outputBiasBase = outputBias(inputs, hidden, outputs, 0)

    /**
     * Runs the network once: [x] holds [inputs] values, [h] the hidden state (updated in place),
     * [scratch] is working space of at least [hidden] values, and [out] receives [outputs] raw
     * (unsquashed) values.
     */
    fun evaluate(x: FloatArray, h: FloatArray, scratch: FloatArray, out: FloatArray) {
        val w = weights
        for (j in 0 until hidden) {
            var s = w[hiddenBiasBase + j]
            var k = j * inputs
            for (i in 0 until inputs) {
                s += w[k] * x[i]
                k++
            }
            k = recurrentBase + j * hidden
            for (i in 0 until hidden) {
                s += w[k] * h[i]
                k++
            }
            scratch[j] = tanh(s)
        }
        System.arraycopy(scratch, 0, h, 0, hidden)
        for (o in 0 until outputs) {
            var s = w[outputBiasBase + o]
            var k = outputBase + o * hidden
            for (i in 0 until hidden) {
                s += w[k] * h[i]
                k++
            }
            out[o] = s
        }
    }

    companion object {
        fun size(inputs: Int, hidden: Int, outputs: Int): Int =
            hidden * inputs + hidden * hidden + hidden + outputs * hidden + outputs

        fun inputWeight(inputs: Int, @Suppress("UNUSED_PARAMETER") hidden: Int, unit: Int, input: Int): Int =
            unit * inputs + input

        fun recurrentWeight(inputs: Int, hidden: Int, unit: Int, from: Int): Int =
            hidden * inputs + unit * hidden + from

        fun hiddenBias(inputs: Int, hidden: Int, unit: Int): Int =
            hidden * inputs + hidden * hidden + unit

        fun outputWeight(inputs: Int, hidden: Int, output: Int, unit: Int): Int =
            hidden * inputs + hidden * hidden + hidden + output * hidden + unit

        fun outputBias(inputs: Int, hidden: Int, outputs: Int, output: Int): Int =
            hidden * inputs + hidden * hidden + hidden + outputs * hidden + output
    }
}

package com.bydesigninteractive.ant.sim.brain

/**
 * The brain's left-right mirror symmetry, as ties between weights (M2a Task 10 fix, for the
 * realism search and M4's evolution).
 *
 * Mirroring the world swaps the left and right antennae, negates every signed bearing and the
 * noise, and should negate the turn and nothing else. A network does exactly that when its hidden
 * units mirror too: each steering pair's members swap, odd relays (the noise relay) change sign
 * and every other unit stays. The network is then equivariant: in a mirrored world each hidden
 * unit takes its mirror's value, so the turn output is odd in each signed input and zero-mean
 * noise never makes the walk circle. That holds when every weight equals its mirror weight times
 * a sign: for the weight from source s to target t, the mirror weight runs from mirror(s) to
 * mirror(t), with the sign sign(s) sign(t). So:
 *
 * - [partner] holds each weight's mirror weight (itself for a fixed weight), and [sign] the sign.
 * - A pair of weights is one parameter: a pair's shared gate weights get the same change, its
 *   signal and turn weights opposite changes.
 * - A fixed weight with sign -1 must be zero (for example a mode unit's weight on the turn
 *   output, or the turn output's bias); a fixed weight with sign +1 is free.
 *
 * Personal variation (`Genome.personal`) scales each unit's input row on its own, so a single ant
 * is not exactly mirror-symmetric; the genome is.
 */
class Mirror(
    val inputs: Int,
    val hidden: Int,
    val outputs: Int,
    inputMirror: IntArray,
    inputSign: IntArray,
    hiddenMirror: IntArray,
    hiddenSign: IntArray,
    outputSign: IntArray,
) {
    val size = Brain.size(inputs, hidden, outputs)
    val partner = IntArray(size)
    val sign = IntArray(size)

    init {
        require(inputMirror.size == inputs && inputSign.size == inputs)
        require(hiddenMirror.size == hidden && hiddenSign.size == hidden && outputSign.size == outputs)
        checkInvolution(inputMirror, inputSign)
        checkInvolution(hiddenMirror, hiddenSign)
        for (j in 0 until hidden) {
            val mj = hiddenMirror[j]
            for (i in 0 until inputs) {
                set(Brain.inputWeight(inputs, hidden, j, i), Brain.inputWeight(inputs, hidden, mj, inputMirror[i]), hiddenSign[j] * inputSign[i])
            }
            for (k in 0 until hidden) {
                set(Brain.recurrentWeight(inputs, hidden, j, k), Brain.recurrentWeight(inputs, hidden, mj, hiddenMirror[k]), hiddenSign[j] * hiddenSign[k])
            }
            set(Brain.hiddenBias(inputs, hidden, j), Brain.hiddenBias(inputs, hidden, mj), hiddenSign[j])
        }
        for (o in 0 until outputs) {
            for (j in 0 until hidden) {
                set(Brain.outputWeight(inputs, hidden, o, j), Brain.outputWeight(inputs, hidden, o, hiddenMirror[j]), outputSign[o] * hiddenSign[j])
            }
            set(Brain.outputBias(inputs, hidden, outputs, o), Brain.outputBias(inputs, hidden, outputs, o), outputSign[o])
        }
    }

    private fun set(a: Int, b: Int, s: Int) {
        partner[a] = b
        sign[a] = s
    }

    /** Weights tied in pairs (each pair counted once). */
    val pairs: Int get() = (0 until size).count { partner[it] > it }

    /** Fixed weights that symmetry holds at zero. */
    val zeros: Int get() = (0 until size).count { partner[it] == it && sign[it] < 0 }

    /** Fixed weights free to take any value. */
    val free: Int get() = (0 until size).count { partner[it] == it && sign[it] > 0 }

    fun fits(g: Genome): Boolean = g.inputs == inputs && g.hidden == hidden && g.outputs == outputs

    /** The largest difference between a weight and its mirror weight times the sign; 0 for a symmetric genome. */
    fun asymmetry(weights: FloatArray): Float {
        var worst = 0f
        for (a in 0 until size) worst = maxOf(worst, kotlin.math.abs(weights[a] - sign[a] * weights[partner[a]]))
        return worst
    }

    companion object {
        private fun checkInvolution(map: IntArray, sign: IntArray) {
            for (i in map.indices) {
                require(map[map[i]] == i) { "mirror of $i is not an involution" }
                require(sign[map[i]] == sign[i]) { "mirror partners $i and ${map[i]} differ in sign" }
            }
        }

        /**
         * The mirror of the seed brain's layout ([Senses.LAYOUT], [SeedBrain.HIDDEN] units): the
         * antennae and footprint readings swap; the trail difference, the ring bearing, the
         * sines of the home, honeydew and prey bearings, the nudge and the noise are odd; every
         * other input is even. Hidden: the steering pairs swap, the noise relay is odd. Output:
         * the turn is odd.
         */
        fun seed(): Mirror {
            val n = Senses.COUNT
            val im = IntArray(n) { it }
            val isg = IntArray(n) { 1 }
            fun swap(a: Int, b: Int) {
                im[a] = b
                im[b] = a
            }
            swap(Senses.TRAIL_L, Senses.TRAIL_R)
            swap(Senses.FOOT_L, Senses.FOOT_R)
            for (i in intArrayOf(
                Senses.TRAIL_DIFF, Senses.RING_BEARING, Senses.HOME_SIN, Senses.HONEYDEW_SIN,
                Senses.PREY_SIN, Senses.NUDGE, Senses.NOISE,
            )) isg[i] = -1
            val h = SeedBrain.HIDDEN
            val hm = IntArray(h) { it }
            val hsg = IntArray(h) { 1 }
            for ((a, b) in listOf(
                SeedBrain.U_STEER to SeedBrain.U_STEER_MIRROR,
                SeedBrain.U_WANDER to SeedBrain.U_WANDER_MIRROR,
                SeedBrain.U_FOOT to SeedBrain.U_FOOT_MIRROR,
                SeedBrain.U_HOMING to SeedBrain.U_HOMING_MIRROR,
                SeedBrain.U_AWAY to SeedBrain.U_AWAY_MIRROR,
                SeedBrain.U_RING_TURN to SeedBrain.U_RING_TURN_MIRROR,
            )) {
                hm[a] = b
                hm[b] = a
            }
            hsg[SeedBrain.U_NOISE] = -1
            val osg = IntArray(Outputs.COUNT) { 1 }
            osg[Outputs.TURN] = -1
            return Mirror(n, h, Outputs.COUNT, im, isg, hm, hsg, osg)
        }
    }
}

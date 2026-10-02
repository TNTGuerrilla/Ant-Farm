package com.bydesigninteractive.ant.sim.brain

/**
 * The hand-wired instinct brain (spec section 3): the starting point of the realism search and
 * the fallback when no shipped genome fits. Every weight is set below with its reason; the
 * circuit table in the M2a plan (Task 6) lists them all.
 *
 * Mode units (SEARCH, HOME, CARRY) threshold the inputs and hold themselves on. A gated unit
 * gets [GATE] times a mode unit's previous value and a bias of -[GATE]: it passes its signal
 * while the mode was on and sits near -1 while it was off. Its zero unit has the same gate and
 * no signal and enters the turn output with the opposite sign, so a shut gate adds nothing.
 * Gates act one evaluation (0.1 s) late, through the recurrent weights.
 */
internal object SeedBrain {
    const val HIDDEN = 16
    const val GATE = 8f

    const val U_SEARCH = 0
    const val U_HOME = 1
    const val U_CARRY = 2
    const val U_SEARCH_ZERO = 3
    const val U_STEER = 4
    const val U_NOISE = 5
    const val U_NOISE_ZERO = 6
    const val U_FOOT = 7
    const val U_FOOT_ZERO = 8
    const val U_DROP = 9
    const val U_DEPOSIT = 10
    const val U_HOMING = 11
    const val U_HOMING_ZERO = 12
    const val U_AWAY = 13
    const val U_AWAY_ZERO = 14
    const val U_GO = 15

    fun genome(): Genome {
        val b = Builder()
        modes(b)
        goOut(b)
        search(b)
        feedAndUnload(b)
        returnHome(b)
        dig(b)
        b.outputBias(Outputs.SPEED, 6f) // walk at the species speed (sigmoid 0.998)
        return b.build()
    }

    /** The three modes. Thresholds: a crop above about 0.3 of the desired fill, reserves at 0.65 (Body.TIRED). */
    private fun modes(b: Builder) {
        // SEARCH: an outbound forager. Any food in the crop, the digger role or being in the
        // nest shut it; reserves below about 0.65 shut it (the forager gives up and goes home).
        b.input(U_SEARCH, Senses.CROP, -160f)
        b.input(U_SEARCH, Senses.DIGGER, -40f)
        b.input(U_SEARCH, Senses.IN_NEST, -40f)
        b.input(U_SEARCH, Senses.RESERVES, 80f)
        b.bias(U_SEARCH, -52f)
        b.recurrent(U_SEARCH, U_SEARCH, 4f) // hysteresis around the reserves threshold
        // HOME: on the surface with food, tired, or a digger without a pellet.
        b.input(U_HOME, Senses.CROP, 160f)
        b.input(U_HOME, Senses.DIGGER, 40f)
        b.input(U_HOME, Senses.RESERVES, -80f)
        b.input(U_HOME, Senses.IN_NEST, -100f)
        b.input(U_HOME, Senses.CARRYING, -80f)
        b.bias(U_HOME, 52f)
        b.recurrent(U_HOME, U_HOME, 4f)
        // CARRY: a pellet in the jaws on the surface.
        b.input(U_CARRY, Senses.CARRYING, 16f)
        b.input(U_CARRY, Senses.IN_NEST, -16f)
        b.bias(U_CARRY, -8f)
    }

    /**
     * Circuit 1 (and 7a). The go-out drive is the response-threshold rule s^2 / (s^2 + theta^2)
     * approximated by a sigmoid: about 0.3 with no stimulus, near 1 when foragers are coming
     * home or a successful forager was just touched, near 0 until reserves are refilled.
     */
    private fun goOut(b: Builder) {
        b.input(U_GO, Senses.RETURNERS, 3f)
        b.input(U_GO, Senses.MET_SUCCESS, 2f)
        b.input(U_GO, Senses.RESERVES, 20f) // reserves under about 0.9 hold the ant in
        b.bias(U_GO, -20.5f)
        b.output(Outputs.GO_OUT, U_GO, 4f)
        b.outputBias(Outputs.GO_OUT, 1f)
        // LEAVE's choice weight is the go-out drive times exp(-1.5): about one leave per 2 s of rest at baseline.
        b.outputBias(b.action(Action.LEAVE), -1.5f)
        // REST is entered by finishing an unload or entering the nest; the brain never picks it over carrying on.
        b.outputBias(b.action(Action.REST), -10f)
    }

    /** Circuit 2 and 7b, 7c: steering while searching. */
    private fun search(b: Builder) {
        b.gate(U_SEARCH_ZERO, U_SEARCH)
        // Trail: turn toward the stronger antenna, about 1.5 (L - R) / theta rad/s near the threshold,
        // as the scripted gain of 3 (L - R) / (L + R) did. Odour: turn toward the plant or food.
        // Nudge: turn away from the dead end an oncoming tired forager came from.
        b.gate(U_STEER, U_SEARCH)
        b.input(U_STEER, Senses.TRAIL_L, 1.5f)
        b.input(U_STEER, Senses.TRAIL_R, -1.5f)
        b.input(U_STEER, Senses.HONEYDEW_SIN, 3f)
        b.input(U_STEER, Senses.PREY_SIN, 3f)
        b.input(U_STEER, Senses.NUDGE, -3f)
        b.turnPair(U_STEER, U_SEARCH_ZERO)
        // Noise: 0.71 per unit gives about 0.9 rad of turning per second walked, the scripted
        // outbound tortuosity (turns of sd 0.9 every 20 mm at 20 mm/s). A trail at the antennae
        // closes this gate (the ant follows instead of wandering), and so does a food odour just
        // received from a nestmate: a primed recruit searches straighter.
        for (u in intArrayOf(U_NOISE, U_NOISE_ZERO)) {
            b.gate(u, U_SEARCH)
            b.input(u, Senses.TRAIL_L, -4f)
            b.input(u, Senses.TRAIL_R, -4f)
            b.input(u, Senses.MET_HONEYDEW, -2f)
            b.input(u, Senses.MET_PREY, -2f)
        }
        b.input(U_NOISE, Senses.NOISE, 0.71f)
        b.turnPair(U_NOISE, U_NOISE_ZERO)
        // Footprints repel only where a trail is present: the gate opens at a trail reading of
        // about the threshold on both antennae (TRAIL_L + TRAIL_R near 1).
        for (u in intArrayOf(U_FOOT, U_FOOT_ZERO)) {
            b.gate(u, U_SEARCH)
            b.bias(u, -4f)
            b.input(u, Senses.TRAIL_L, 4f)
            b.input(u, Senses.TRAIL_R, 4f)
        }
        b.input(U_FOOT, Senses.FOOT_L, -2f)
        b.input(U_FOOT, Senses.FOOT_R, 2f)
        b.turnPair(U_FOOT, U_FOOT_ZERO)
    }

    /** Circuits 3 and 5: the masks hold the conditions, so a high bias is the whole rule. */
    private fun feedAndUnload(b: Builder) {
        b.outputBias(b.action(Action.FEED), 6f) // at food with an empty crop: feed (P about 0.998)
        b.outputBias(b.action(Action.UNLOAD), 6f) // in the nest with food: unload
    }

    /** Circuit 4: going home, laying trail, entering. */
    private fun returnHome(b: Builder) {
        b.gate(U_HOMING_ZERO, U_HOME)
        // Steer to the home bearing; noise 0.14 (about 0.17 rad per second walked, the scripted
        // homeward runs of sd 0.3 every 60 mm) makes the way home straighter than the search;
        // climb the footprint (home scent) gradient when the home vector has run out.
        b.gate(U_HOMING, U_HOME)
        b.input(U_HOMING, Senses.HOME_SIN, 3f)
        b.input(U_HOMING, Senses.NOISE, 0.14f)
        b.input(U_HOMING, Senses.FOOT_L, 2f)
        b.input(U_HOMING, Senses.FOOT_R, -2f)
        b.turnPair(U_HOMING, U_HOMING_ZERO)
        // Deposit: only at the desired fill (FULL); more right after feeding (near the food),
        // less when crowded and on a trail that is already strong.
        b.input(U_DEPOSIT, Senses.FULL, 3f)
        b.input(U_DEPOSIT, Senses.FED_RECENT, 1f)
        b.input(U_DEPOSIT, Senses.CONTACT_RATE, -1.5f)
        b.input(U_DEPOSIT, Senses.TRAIL_L, -0.75f)
        b.input(U_DEPOSIT, Senses.TRAIL_R, -0.75f)
        b.bias(U_DEPOSIT, -2.5f)
        b.output(Outputs.DEPOSIT, U_DEPOSIT, 4f)
        b.outputBias(Outputs.DEPOSIT, -0.5f)
        // Enter at the entrance when homeward (logit +6); a searcher passing by does not (-6).
        b.output(b.action(Action.ENTER), U_HOME, 6f)
    }

    /** Circuit 6: digging and carrying spoil out (replacing the scripted digger one for one). */
    private fun dig(b: Builder) {
        b.outputBias(b.action(Action.DIG), 4f) // a resting digger with a dig site goes to dig
        // Walk away from the entrance with the pellet, wandering a little (outbound noise 0.3).
        b.gate(U_AWAY_ZERO, U_CARRY)
        b.gate(U_AWAY, U_CARRY)
        b.input(U_AWAY, Senses.HOME_SIN, -3f)
        b.input(U_AWAY, Senses.NOISE, 0.3f)
        b.turnPair(U_AWAY, U_AWAY_ZERO)
        // Drop: about 0.2% per evaluation 8 mm out on bare ground, rising with spoil already
        // there (Khuong 2016) and near certain by 40 mm (the scripted spoilMaxDistance).
        b.input(U_DROP, Senses.HOME_DIST, 25f)
        b.input(U_DROP, Senses.SPOIL, 1.5f)
        b.bias(U_DROP, -1.2f)
        b.output(b.action(Action.DROP), U_DROP, 8f)
        b.outputBias(b.action(Action.DROP), -1f)
        // A homeward digger on the pile picks a pellet back up now and then: about 0.29% per
        // evaluation, the scripted 0.029 per second.
        b.outputBias(b.action(Action.PICK_UP), -5.8f)
    }

    private class Builder {
        val inputs = Senses.COUNT
        val outputs = Outputs.COUNT
        val w = FloatArray(Brain.size(inputs, HIDDEN, outputs))

        fun input(unit: Int, input: Int, v: Float) {
            w[Brain.inputWeight(inputs, HIDDEN, unit, input)] += v
        }

        fun recurrent(unit: Int, from: Int, v: Float) {
            w[Brain.recurrentWeight(inputs, HIDDEN, unit, from)] += v
        }

        fun bias(unit: Int, v: Float) {
            w[Brain.hiddenBias(inputs, HIDDEN, unit)] += v
        }

        fun output(output: Int, unit: Int, v: Float) {
            w[Brain.outputWeight(inputs, HIDDEN, output, unit)] += v
        }

        fun outputBias(output: Int, v: Float) {
            w[Brain.outputBias(inputs, HIDDEN, outputs, output)] += v
        }

        fun action(a: Action): Int = Outputs.ACTION + a.ordinal

        /** Opens [unit] while mode unit [mode] was on at the last evaluation; pins it near -1 otherwise. */
        fun gate(unit: Int, mode: Int) {
            recurrent(unit, mode, GATE)
            bias(unit, -GATE)
        }

        /** Adds [unit] to the turn output, cancelled by [zero] (same gate, no signal) while the gate is shut. */
        fun turnPair(unit: Int, zero: Int) {
            output(Outputs.TURN, unit, 1f)
            output(Outputs.TURN, zero, -1f)
        }

        fun build(): Genome = Genome(inputs, HIDDEN, outputs, w, Senses.LAYOUT)
    }
}

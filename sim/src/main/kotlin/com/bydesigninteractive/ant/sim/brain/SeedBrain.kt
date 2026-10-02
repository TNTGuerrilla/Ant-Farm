package com.bydesigninteractive.ant.sim.brain

/**
 * The hand-wired instinct brain (spec section 3): the starting point of the realism search and
 * the fallback when no shipped genome fits. Every weight is set below with its reason.
 *
 * Mode units (SEARCH, HOME, CARRY) threshold the inputs and hold themselves on. Relay units
 * threshold or pass on a signal that several units need: trail present (TRAIL_ON), odour received
 * (ODOUR) and the noise (NOISE). Steering is done by mirror pairs: the plus member gets a signal
 * and the minus member gets its negative, both get the same gate, and the turn output reads them
 * with weights +w and -w. An open gate (0) gives 2w tanh(signal); a shut gate (-2 [GATE] or less)
 * pins both members at -1, where they cancel. For any gate value the pair's output is odd in its
 * signal, so zero-mean noise never makes the walk circle.
 *
 * Gates and noise reach the pairs only through recurrent weights, which personal variation
 * leaves exact (`Genome.personal`), so both members of a pair always see the same gate and the
 * same noise. Thresholds that must not move between ants (modes, relays, the go-out drive) are set
 * through the BIAS input, which scales with the rest of the unit's inputs. Gates act one or two
 * evaluations (0.1 to 0.2 s) late, through the recurrent weights: that is the Elman "mode held by
 * recurrence" of spec section 3.
 */
internal object SeedBrain {
    const val HIDDEN = 19
    const val GATE = 8f

    // Modes.
    const val U_SEARCH = 0
    const val U_HOME = 1
    const val U_CARRY = 2

    // Relays.
    const val U_TRAIL_ON = 3
    const val U_ODOUR = 4
    const val U_NOISE = 5

    // Mirror pairs for the turn output.
    const val U_STEER = 6
    const val U_STEER_MIRROR = 7
    const val U_WANDER = 8
    const val U_WANDER_MIRROR = 9
    const val U_FOOT = 10
    const val U_FOOT_MIRROR = 11
    const val U_HOMING = 12
    const val U_HOMING_MIRROR = 13
    const val U_AWAY = 14
    const val U_AWAY_MIRROR = 15

    // Drives.
    const val U_DROP = 16
    const val U_DEPOSIT = 17
    const val U_GO = 18

    fun genome(): Genome {
        val b = Builder()
        modes(b)
        relays(b)
        goOut(b)
        search(b)
        feedAndUnload(b)
        returnHome(b)
        dig(b)
        b.outputBias(Outputs.SPEED, 6f) // walk at the species speed (sigmoid 0.998)
        return b.build()
    }

    /**
     * The three modes. Thresholds: a crop above about 0.175 of the desired fill (160 c = 80 - 52),
     * reserves at 0.65 (Body.TIRED, 52 / 80). The self-connection of 4 latches each mode across a
     * band of about 0.05 in reserves (0.625 to 0.675), so an ant near the threshold does not flicker.
     */
    private fun modes(b: Builder) {
        // SEARCH: an outbound forager. Any food in the crop, the digger role or being in the
        // nest shut it; reserves below about 0.65 shut it (the forager gives up and goes home).
        b.input(U_SEARCH, Senses.CROP, -160f)
        b.input(U_SEARCH, Senses.DIGGER, -40f)
        b.input(U_SEARCH, Senses.IN_NEST, -40f)
        b.input(U_SEARCH, Senses.RESERVES, 80f)
        b.input(U_SEARCH, Senses.BIAS, -52f)
        b.recurrent(U_SEARCH, U_SEARCH, 4f)
        // HOME: on the surface with food, tired, or a digger without a pellet. A pellet (-80)
        // hands the ant to CARRY instead; being in the nest (-200) outweighs a full crop plus the
        // bias, so no mode is on underground.
        b.input(U_HOME, Senses.CROP, 160f)
        b.input(U_HOME, Senses.DIGGER, 40f)
        b.input(U_HOME, Senses.RESERVES, -80f)
        b.input(U_HOME, Senses.IN_NEST, -200f)
        b.input(U_HOME, Senses.CARRYING, -80f)
        b.input(U_HOME, Senses.BIAS, 52f)
        b.recurrent(U_HOME, U_HOME, 4f)
        // CARRY: a pellet in the jaws on the surface.
        b.input(U_CARRY, Senses.CARRYING, 16f)
        b.input(U_CARRY, Senses.IN_NEST, -16f)
        b.input(U_CARRY, Senses.BIAS, -8f)
    }

    /** Signals several pairs share, so each pair reads them through exact recurrent weights. */
    private fun relays(b: Builder) {
        // Trail present: TRAIL_L + TRAIL_R above 0.75 (each antenna at about 0.6 of the trail
        // threshold concentration). The steep slope (20) saturates the unit within 0.25 either
        // side, so the trail sums 0.5 and 1.0 read clearly off and on.
        b.input(U_TRAIL_ON, Senses.TRAIL_L, 20f)
        b.input(U_TRAIL_ON, Senses.TRAIL_R, 20f)
        b.input(U_TRAIL_ON, Senses.BIAS, -15f)
        // Odour received from a nestmate in the last Contacts.MET_SECONDS: either flag turns it
        // fully on (8 - 4 = +4), none leaves it fully off (-4).
        b.input(U_ODOUR, Senses.MET_HONEYDEW, 8f)
        b.input(U_ODOUR, Senses.MET_PREY, 8f)
        b.input(U_ODOUR, Senses.BIAS, -4f)
        // Noise: tanh of the standard normal noise input, odd in it and bounded, read by every
        // pair that wanders.
        b.input(U_NOISE, Senses.NOISE, 1f)
    }

    /**
     * Circuit 1 (and 7a). The go-out drive is the response-threshold rule s^2 / (s^2 + theta^2)
     * approximated by a sigmoid: about 0.3 with no stimulus, near 1 when foragers are coming
     * home or a successful forager was just touched, near 0 until reserves are refilled. The
     * slope in reserves is gentle (5) so that personal variation spreads the baseline only a
     * little; the bias goes through the BIAS input so it scales with the slope.
     */
    private fun goOut(b: Builder) {
        b.input(U_GO, Senses.RETURNERS, 3f)
        b.input(U_GO, Senses.MET_SUCCESS, 2f)
        b.input(U_GO, Senses.RESERVES, 5f) // full reserves: -0.5; below about 0.85 the ant mostly stays in
        b.input(U_GO, Senses.BIAS, -5.5f)
        // GO_OUT spans logits -3 to +5 (a drive of 0.05 to 0.99); the baseline unit value -0.46 gives 0.30.
        b.output(Outputs.GO_OUT, U_GO, 4f)
        b.outputBias(Outputs.GO_OUT, 1f)
        // LEAVE's choice weight is the go-out drive times exp(-1.5): about one leave per 2 s of rest at baseline.
        b.outputBias(b.action(Action.LEAVE), -1.5f)
        // REST is entered by finishing an unload or entering the nest; the brain never picks it over carrying on.
        b.outputBias(b.action(Action.REST), -10f)
    }

    /** Circuit 2 and 7b, 7c: steering while searching. */
    private fun search(b: Builder) {
        // Steer: trail, odour and nudge, open while searching. Trail: turn toward the stronger
        // antenna, about 1.5 (L - R) / theta rad/s near the threshold, as the scripted gain of
        // 3 (L - R) / (L + R) did. Odour (3): about 2.9 rad/s toward a plant or food 30 degrees off
        // (sin 0.5), proportional below about 15 degrees, so the ant swings onto the bearing in
        // well under a second and then holds it. Nudge (-3): a fresh nudge (0.8 to 1) turns at about
        // 3 rad/s away from the dead end an oncoming tired forager came from, fading over
        // Contacts.NUDGE_SECONDS as the nudge input decays.
        b.whenOn(U_STEER, U_STEER_MIRROR, U_SEARCH)
        b.signal(U_STEER, U_STEER_MIRROR, Senses.TRAIL_L, 1.5f)
        b.signal(U_STEER, U_STEER_MIRROR, Senses.TRAIL_R, -1.5f)
        b.signal(U_STEER, U_STEER_MIRROR, Senses.HONEYDEW_SIN, 3f)
        b.signal(U_STEER, U_STEER_MIRROR, Senses.PREY_SIN, 3f)
        b.signal(U_STEER, U_STEER_MIRROR, Senses.NUDGE, -3f)
        b.turn(U_STEER, U_STEER_MIRROR, 0.5f)
        // Wander: the outbound random walk. The noise relay at 2 into a pair read at 0.75 gives an
        // angular diffusion of about 0.94 rad^2/s (held 0.1 s per evaluation, after the output
        // tanh), near the scripted walk's 0.81 (turns of sd 0.9 rad every 20 mm at 20 mm/s).
        // A trail at the antennae shuts it (the ant follows instead of wandering). So does a food
        // odour received from a nestmate: a received odour steadies the search, the owner's
        // approved simplification of steering toward that odour.
        b.whenOn(U_WANDER, U_WANDER_MIRROR, U_SEARCH)
        b.whenOff(U_WANDER, U_WANDER_MIRROR, U_TRAIL_ON)
        b.whenOff(U_WANDER, U_WANDER_MIRROR, U_ODOUR)
        b.recurrentSignal(U_WANDER, U_WANDER_MIRROR, U_NOISE, 2f)
        b.turn(U_WANDER, U_WANDER_MIRROR, 0.75f)
        // Footprints repel only where a trail is present (the TRAIL_ON relay), however strong the
        // trail. A footprint difference across the antennae of 0.6 turns about 2.7 rad/s away from
        // the more walked side (2 per unit, the same scale as the trail gain), 0.2 about 1.5.
        b.whenOn(U_FOOT, U_FOOT_MIRROR, U_SEARCH)
        b.whenOn(U_FOOT, U_FOOT_MIRROR, U_TRAIL_ON)
        b.signal(U_FOOT, U_FOOT_MIRROR, Senses.FOOT_L, -2f)
        b.signal(U_FOOT, U_FOOT_MIRROR, Senses.FOOT_R, 2f)
        b.turn(U_FOOT, U_FOOT_MIRROR, 0.5f)
    }

    /** Circuits 3 and 5: the masks hold the conditions, so a high bias is the whole rule. */
    private fun feedAndUnload(b: Builder) {
        b.outputBias(b.action(Action.FEED), 6f) // at food with an empty crop: feed (P about 0.998)
        b.outputBias(b.action(Action.UNLOAD), 6f) // in the nest with food: unload
    }

    /** Circuit 4: going home, laying trail, entering. */
    private fun returnHome(b: Builder) {
        // Homing: steer to the home bearing (3, the odour's scale: about 2.9 rad/s at 30 degrees
        // off). The noise relay at 0.22 gives about 0.03 rad^2/s (0.17 rad per square-root second,
        // the scripted homeward runs of sd 0.3 every 60 mm), a way home far straighter than the
        // search. The footprint (home scent) term is always on: it draws the ant up the home-scent
        // gradient (2 per unit), which matters most once the home vector has run out and the
        // bearing inputs read zero.
        b.whenOn(U_HOMING, U_HOMING_MIRROR, U_HOME)
        b.signal(U_HOMING, U_HOMING_MIRROR, Senses.HOME_SIN, 3f)
        b.signal(U_HOMING, U_HOMING_MIRROR, Senses.FOOT_L, 2f)
        b.signal(U_HOMING, U_HOMING_MIRROR, Senses.FOOT_R, -2f)
        b.recurrentSignal(U_HOMING, U_HOMING_MIRROR, U_NOISE, 0.22f)
        b.turn(U_HOMING, U_HOMING_MIRROR, 0.5f)
        // Deposit: only at the desired fill (FULL); more right after feeding (near the food),
        // less when crowded and on a trail that is already strong. The unit runs from about
        // -0.9 without a full crop to +0.9 when full and freshly fed; DEPOSIT = 4 u - 0.5 maps
        // that to 0.016 (below Outputs.DEPOSIT_MIN, nothing laid) and 0.96.
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
        // Walk away from the entrance with the pellet (HOME_SIN -3, homing reversed), wandering a
        // little (noise relay at 0.4, about 0.09 rad^2/s).
        b.whenOn(U_AWAY, U_AWAY_MIRROR, U_CARRY)
        b.signal(U_AWAY, U_AWAY_MIRROR, Senses.HOME_SIN, -3f)
        b.recurrentSignal(U_AWAY, U_AWAY_MIRROR, U_NOISE, 0.4f)
        b.turn(U_AWAY, U_AWAY_MIRROR, 0.5f)
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

        /**
         * Adds one gate condition to both members of a pair: open while unit [mode] was on (+1) at
         * the last evaluation. Each condition adds [GATE] times the mode's value and -[GATE] as a
         * hidden bias, so the gate sums to 0 when every condition holds and to -2 [GATE] or less
         * when any fails.
         */
        fun whenOn(plus: Int, minus: Int, mode: Int) {
            for (u in intArrayOf(plus, minus)) {
                recurrent(u, mode, GATE)
                bias(u, -GATE)
            }
        }

        /** As [whenOn], but open while unit [mode] was off (-1). */
        fun whenOff(plus: Int, minus: Int, mode: Int) {
            for (u in intArrayOf(plus, minus)) {
                recurrent(u, mode, -GATE)
                bias(u, -GATE)
            }
        }

        /** Feeds input [sense] to the pair: +[v] to [plus], -[v] to [minus]. */
        fun signal(plus: Int, minus: Int, sense: Int, v: Float) {
            input(plus, sense, v)
            input(minus, sense, -v)
        }

        /** Feeds hidden unit [from]'s previous value to the pair: +[v] to [plus], -[v] to [minus]. */
        fun recurrentSignal(plus: Int, minus: Int, from: Int, v: Float) {
            recurrent(plus, from, v)
            recurrent(minus, from, -v)
        }

        /** Adds the pair to the turn output with weights +[v] and -[v]. */
        fun turn(plus: Int, minus: Int, v: Float) {
            output(Outputs.TURN, plus, v)
            output(Outputs.TURN, minus, -v)
        }

        fun build(): Genome = Genome(inputs, HIDDEN, outputs, w, Senses.LAYOUT)
    }
}

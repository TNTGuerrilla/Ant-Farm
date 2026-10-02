package com.bydesigninteractive.ant.sim.brain

/**
 * The hand-wired instinct brain (spec section 3): the starting point of the realism search and
 * the fallback when no shipped genome fits. Every weight is set below with its reason.
 *
 * Mode units (SEARCH, HOME, CARRY) threshold the inputs and hold themselves on. Relay units
 * threshold or pass on a signal that several units need: trail present (TRAIL_ON), odour received
 * (ODOUR), the noise (NOISE) and a trail on the ring (RING). Steering is done by mirror pairs: the plus member gets a signal
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
    const val HIDDEN = 23
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

    // The exit choice (Task 8c): a relay for the ring's strength and its mirror pair.
    const val U_RING = 19
    const val U_RING_TURN = 20
    const val U_RING_TURN_MIRROR = 21

    // The deposit's cut (Task 8d): crowding, a strong trail and time since feeding, saturated together.
    const val U_CUT = 22

    /**
     * The ring relay's slope and midpoint in the RING input (s / (s + T)): about -0.76 at half the
     * threshold, 0 at the threshold, 0.76 at twice it and 0.95 at four times.
     */
    const val RING_SLOPE = 6f
    const val RING_MID = 0.5f

    /** How far the ring relay opens the exit pair's gate: shut (-2 [RING_GATE]) with no trail, open with a strong one. */
    const val RING_GATE = 2f

    /**
     * The exit pair's gain on the ring bearing (RING_BEARING, the angle over pi): 3 pi, the
     * odour's 3 per unit of sine for small angles, so the pair is near saturation 30 degrees off
     * and fully saturated toward a trail behind.
     */
    const val RING_TURN = 9.42f

    /**
     * The exit pair's weight on the turn output, which sets how decisive the exit choice is: about
     * 2.3 rad/s toward a strong trail 30 degrees off (0.5, the other pairs' weight, gave 2.9).
     * Calibrated in Task 8c to the fork accuracy of the simulation reference (section 5: 74 to 83%
     * per junction, Czaczkes 2017): with one strong trail (marks of 10, five times T, every 10 mm)
     * leaving the entrance, 78% of foragers are out along it (within 45 degrees) 5 s later (25% with the pair off, by chance and by meeting the
     * trail; 94% at 0.5). See the Task 8c report for the Gruter runs behind the choice.
     */
    const val RING_OUT = 0.35f

    fun genome(): Genome {
        val b = Builder()
        modes(b)
        relays(b)
        goOut(b)
        search(b)
        exitChoice(b)
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
        // Trail present, latched with the thresholds of the following state (Actions.follow): it
        // reads the saturated total TRAIL_SUM = (L + R) / (L + R + T), the same total the
        // following state reads, so the two agree however unequally the antennae read. It turns
        // on at TRAIL_SUM 0.5 (L + R = T) and, through its self-connection (6.12), stays on until
        // TRAIL_SUM falls below 0.231 (L + R = 0.3 T), both thresholds checked numerically to
        // 0.001. Slope 30 and bias -10.96 put the band's centre at 0.365; the self-connection
        // sets its width: a tanh latch u = tanh(x + r u) flips where its other fixed point
        // vanishes, at x = +-(r v - atanh v) with v = sqrt(1 - 1 / r), which is 4.04 for r = 6.12,
        // half of 30 (0.5 - 0.231). Task 8b: it
        // switched at 0.75 with no latch (about 1.2 T). Task 8c: it summed the two saturated
        // antennae, L / (L + T) + R / (R + T), which matches L + R = T only when they read the
        // same; with one antenna at T and the other at 0 the following state was on and the relay off.
        b.input(U_TRAIL_ON, Senses.TRAIL_SUM, 30f)
        b.input(U_TRAIL_ON, Senses.BIAS, -10.96f)
        b.recurrent(U_TRAIL_ON, U_TRAIL_ON, 6.12f)
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
        // antenna in proportion to the normalised difference (L - R) / (L + R) (spec 3.2, the
        // TRAIL_DIFF input), 0.75 giving about 3 rad/s per unit, the scripted trailTurnGain of 3,
        // and the same on a strong trail as on a faint one. Odour (3): about 2.9 rad/s toward a
        // plant or food 30 degrees off (sin 0.5), proportional below about 15 degrees, so the ant
        // swings onto the bearing in well under a second and then holds it. Nudge (-3): a fresh nudge (0.8 to 1) turns at about
        // 3 rad/s away from the dead end an oncoming tired forager came from, fading over
        // Contacts.NUDGE_SECONDS as the nudge input decays.
        b.whenOn(U_STEER, U_STEER_MIRROR, U_SEARCH)
        b.signal(U_STEER, U_STEER_MIRROR, Senses.TRAIL_DIFF, 0.75f)
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
        // On a trail (the TRAIL_ON relay), and only there, two more terms. Footprints repel,
        // however strong the trail: a footprint difference across the antennae of 0.6 turns about
        // 2 rad/s away from the more walked side, 0.2 about 0.8 (1 per unit; Task 8b halved it
        // from 2, because home scent peaks along a busy trail, so its difference across the
        // antennae pushes a follower off the trail's centre, and at 2 that push matched the
        // trail's own steering). And an outward bias (HOME_SIN -0.5): a follower turns away from
        // home, about 1.7 rad/s with home square to one side and nothing with home straight
        // behind, so it walks out along the trail rather than back to the nest. This is path
        // integration, which Lasius niger has (simulation reference section 4), doing what the
        // scripted forager's keepOutward did; trails leave the nest outward, so away from home is
        // along them.
        b.whenOn(U_FOOT, U_FOOT_MIRROR, U_SEARCH)
        b.whenOn(U_FOOT, U_FOOT_MIRROR, U_TRAIL_ON)
        b.signal(U_FOOT, U_FOOT_MIRROR, Senses.FOOT_L, -1f)
        b.signal(U_FOOT, U_FOOT_MIRROR, Senses.FOOT_R, 1f)
        b.signal(U_FOOT, U_FOOT_MIRROR, Senses.HOME_SIN, -0.5f)
        b.turn(U_FOOT, U_FOOT_MIRROR, 0.5f)
    }

    /**
     * Circuit 2 at the nest mouth (Task 8c): turn toward the strongest trail on the ring around
     * the ant (Senses.ring), sensed only near the entrance, where all the colony's trails meet.
     * The ring relay grades the pair's gate by the trail's strength: with no trail it is shut
     * (the heading stays the random one the ant came out with); at half the threshold T it turns
     * at under 0.1 rad/s toward a trail 30 degrees off, at T about 0.8 rad/s, and from twice T
     * nearly the full 2.3 rad/s ([RING_OUT]). Open only while searching and not yet on a trail:
     * once the antennae read the trail, the trail steering and the outward bias take over.
     *
     * Why this reproduces Deneubourg's choice (k + s)^n at the junction (simulation reference
     * section 5: forks taken by relative trail strength, 74 to 83% per junction, Czaczkes 2017;
     * collective choice, Beckers 1990), which Task 8b's innate exit rule computed directly: the
     * turn always points at the strongest trail, but its strength grows steeply with s / (s + T)
     * while the outbound walk's noise stays the same. A trail well below T barely bends the walk,
     * so the ant leaves in a random direction (the k term); a strong one wins against the noise
     * nearly every time; between, the ant reaches the trail it turned toward only some of the
     * time, and the trail following (the TRAIL_DIFF steer) then holds whichever trail its
     * antennae meet first. The choice is therefore graded and increasing in s, steeper than
     * linear, like (k + s)^n, without any extra randomness: the only draw is the noise input.
     * Measured with trails on both sides of the entrance (marks of 10 against 5 every 10 mm), 62%
     * of foragers go out along the stronger and 34% along the weaker; 10 against 2, 71% and 19%.
     * The pair is odd in RING_BEARING, so it never biases the turn when the ring is empty.
     */
    private fun exitChoice(b: Builder) {
        b.input(U_RING, Senses.RING, RING_SLOPE)
        b.input(U_RING, Senses.BIAS, -RING_SLOPE * RING_MID)
        b.whenOn(U_RING_TURN, U_RING_TURN_MIRROR, U_SEARCH)
        b.whenOff(U_RING_TURN, U_RING_TURN_MIRROR, U_TRAIL_ON)
        b.graded(U_RING_TURN, U_RING_TURN_MIRROR, U_RING, RING_GATE)
        b.signal(U_RING_TURN, U_RING_TURN_MIRROR, Senses.RING_BEARING, RING_TURN)
        b.turn(U_RING_TURN, U_RING_TURN_MIRROR, RING_OUT)
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
        // less when crowded and on a trail that is already strong. Two units: U_DEPOSIT is the
        // gate (FULL), U_CUT the cut, both read by the output. A fresh, uncrowded forager's
        // deposit is 0.30, a mark of 0.67 (a third of the trail threshold T at the default
        // markAmount), so a trail reaches T only where several foragers lay over each other
        // within the trail's lifetime: below about 75 workers trails barely form (Mailleux 2003,
        // simulation reference section 2). Without a full crop the gate sits at -1 and the
        // deposit at 0.014, below Outputs.DEPOSIT_MIN (nothing laid).
        //
        // The cut is one saturating unit, so the cuts together can never take a full forager
        // below a floor of about 0.071, 4.2 times less than fresh (Czaczkes 2013: up to 5.6
        // times less when crowded; high pheromone suppresses deposition; simulation reference
        // section 5). Alone, each cut reaches most of that range: crowding (CONTACT_RATE 0.67,
        // about 10 contacts in 10 s) cuts the fresh deposit to 0.072 (0.20 at CONTACT_RATE 0.2),
        // a strong trail (each antenna at 4 T) to 0.081 (0.14 at T), and time since feeding
        // (FED_RECENT, tau 60 s) to 0.12 at 120 s and 0.10 at 300 s. Any combination of
        // crowding up to CONTACT_RATE 1, trails up to 3 T and up to 300 s since feeding stays at
        // 0.071 or more, so a full forager keeps
        // marking all the way to the nest, through the crowded entrance where all trails meet.
        // Task 8d: with the cuts summed in one unit (crowding -0.8, saturation -0.3 per antenna,
        // FED_RECENT 0.4), a crowded returner 20 s after feeding deposited 0.047, below
        // DEPOSIT_MIN, so the trail stopped short of the entrance.
        //
        // Personal variation: the output bias (sd 0.3) shifts the whole band in logit, so the
        // floor holds for an ant whose bias is at most 0.37 below the seed's (about 1.25 sd, nine
        // ants in ten); the rest stop marking only in the worst combination, near the 14% of
        // foragers that never lay trail (Mailleux 2005). The not-full deposit stays below
        // DEPOSIT_MIN up to about +1.2 in bias. The input-row scaling (sd 0.1) moves the cut's
        // midpoint a little but not its saturated floor.
        //
        // Weights: the cut unit sits at tanh(-1.5) = -0.905 when fresh, uncrowded and off trail
        // (bias 0.7 minus FED_RECENT 2.2), and the output reads it at -0.9, so its full range
        // (-0.905 to +1) is 1.71 in logit: 0.298 to 0.071. Crowding 6, saturation 1.7 per
        // antenna. Task 8d calibration: crowding at 4.2 (0.080 when crowded) raised the
        // 150-forager following to 0.34 but let only 3 of 8 crowded colonies switch; at 6, 6 of 8
        // switch (see the Task 8d report). Task 8c had strengthened the crowding and saturation
        // cuts (to 4.8 and 4.1 times less) because a busy trail kept growing and a crowded colony never left it for a better
        // source (Gruter 2012: crowding is the negative feedback that lets colonies switch). Task
        // 8b lowered the fresh deposit from 0.96 (a mark above T on its own) because a 30-forager
        // colony kept its trails at several times T. Thresholds go through the BIAS input so
        // personal variation keeps them.
        b.input(U_DEPOSIT, Senses.FULL, 6f)
        b.input(U_DEPOSIT, Senses.BIAS, -3f)
        b.input(U_CUT, Senses.CONTACT_RATE, 6f)
        b.input(U_CUT, Senses.TRAIL_L, 1.7f)
        b.input(U_CUT, Senses.TRAIL_R, 1.7f)
        b.input(U_CUT, Senses.FED_RECENT, -2.2f)
        b.input(U_CUT, Senses.BIAS, 0.7f)
        b.output(Outputs.DEPOSIT, U_DEPOSIT, 2f)
        b.output(Outputs.DEPOSIT, U_CUT, -0.9f)
        b.outputBias(Outputs.DEPOSIT, -3.66f)
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

        /**
         * A graded gate condition: adds [v] times unit [relay]'s previous value and -[v] as a
         * hidden bias to both members, so the gate is 0 (open) when the relay is +1, -2 [v] when
         * it is -1, and in between otherwise. The pair stays odd in its signal at any gate value.
         */
        fun graded(plus: Int, minus: Int, relay: Int, v: Float) {
            for (u in intArrayOf(plus, minus)) {
                recurrent(u, relay, v)
                bias(u, -v)
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

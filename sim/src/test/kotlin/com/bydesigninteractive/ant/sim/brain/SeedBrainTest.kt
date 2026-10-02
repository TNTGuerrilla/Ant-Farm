package com.bydesigninteractive.ant.sim.brain

import com.bydesigninteractive.ant.sim.ant.AntParams
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.cos
import kotlin.math.tanh
import kotlin.test.Test
import java.util.Random
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** One test per circuit of spec section 3 (and the M2a plan, Task 6): inputs in, steering and choices out. */
class SeedBrainTest {
    private val genome = SeedBrain.genome()

    /** Runs the seed brain (no personal variation) three times on the same inputs so the modes settle; RESERVES 1 and BIAS 1 unless set. */
    private fun outputs(vararg set: Pair<Int, Float>): FloatArray = outputs(genome.brain(), *set)

    private fun outputs(b: Brain, vararg set: Pair<Int, Float>): FloatArray {
        val x = inputs(*set)
        val h = FloatArray(genome.hidden)
        val s = FloatArray(genome.hidden)
        val o = FloatArray(genome.outputs)
        repeat(3) { b.evaluate(x, h, s, o) }
        return o
    }

    /**
     * The input vector. Unless TRAIL_SUM is set, it is derived from TRAIL_L and TRAIL_R as Senses
     * computes it from the raw readings (Task 8c added it; the relay reads it), so the older tests
     * that set only the two antennae read the same trail as before.
     */
    private fun inputs(vararg set: Pair<Int, Float>): FloatArray {
        val x = FloatArray(Senses.COUNT)
        x[Senses.BIAS] = 1f
        x[Senses.RESERVES] = 1f
        for ((i, v) in set) x[i] = v
        if (set.none { it.first == Senses.TRAIL_SUM }) x[Senses.TRAIL_SUM] = trailSum(x[Senses.TRAIL_L], x[Senses.TRAIL_R])
        return x
    }

    /** TRAIL_SUM from the saturated antennae: each reads r / (r + T), so r = T v / (1 - v). */
    private fun trailSum(l: Float, r: Float): Float {
        val sum = l / (1f - l) + r / (1f - r) // in units of T
        return sum / (sum + 1f)
    }

    /** Ant [id]'s personal network, as World gives it. */
    private fun personal(id: Int) = genome.personal(WORLD_SEED, id, biasFrom = Outputs.DEPOSIT)

    /**
     * Steps [b] [steps] times on fixed inputs with persistent hidden state, the noise input drawn
     * antithetically (+n, then -n) from a seeded generator, and returns the mean turn rate (rad/s).
     */
    private fun meanTurn(b: Brain, steps: Int, seed: Long, vararg set: Pair<Int, Float>): Double {
        val x = inputs(*set)
        val h = FloatArray(genome.hidden)
        val s = FloatArray(genome.hidden)
        val o = FloatArray(genome.outputs)
        repeat(5) { b.evaluate(x, h, s, o) }
        val r = Random(seed)
        var n = 0f
        var sum = 0.0
        for (i in 0 until steps) {
            if (i % 2 == 0) n = r.nextGaussian().toFloat()
            x[Senses.NOISE] = if (i % 2 == 0) n else -n
            b.evaluate(x, h, s, o)
            sum += turn(o)
        }
        return sum / steps
    }

    /** Angular diffusion (rad^2/s) of the seed brain's walk: the turn rate's variance times the evaluation interval. */
    private fun diffusion(vararg set: Pair<Int, Float>): Double {
        val b = genome.brain()
        val x = inputs(*set)
        val h = FloatArray(genome.hidden)
        val s = FloatArray(genome.hidden)
        val o = FloatArray(genome.outputs)
        repeat(5) { b.evaluate(x, h, s, o) }
        val r = Random(11L)
        var sum = 0.0
        var sq = 0.0
        val steps = 20_000
        repeat(steps) {
            x[Senses.NOISE] = r.nextGaussian().toFloat()
            b.evaluate(x, h, s, o)
            val t = turn(o).toDouble()
            sum += t
            sq += t * t
        }
        val mean = sum / steps
        return (sq / steps - mean * mean) * EVAL_SECONDS
    }

    private fun turn(o: FloatArray) = Outputs.MAX_TURN_RATE * tanh(o[Outputs.TURN])
    private fun logit(o: FloatArray, a: Action) = o[Outputs.ACTION + a.ordinal]
    private fun sig(v: Float) = 1f / (1f + exp(-v))
    private fun goOut(vararg set: Pair<Int, Float>) = sig(outputs(Senses.IN_NEST to 1f, *set)[Outputs.GO_OUT])
    private fun deposit(vararg set: Pair<Int, Float>) = sig(outputs(*set)[Outputs.DEPOSIT])

    /** Turn rate with the noise input at +1 minus at -1, other inputs as given. */
    private fun noiseSpread(vararg set: Pair<Int, Float>) =
        turn(outputs(Senses.NOISE to 1f, *set)) - turn(outputs(Senses.NOISE to -1f, *set))

    private val full = arrayOf(Senses.CROP to 1f, Senses.FULL to 1f, Senses.FED_RECENT to 1f)
    private val carrying = arrayOf(Senses.DIGGER to 1f, Senses.CARRYING to 1f)

    @Test
    fun theGenomeFitsTheLayout() {
        assertEquals(Senses.COUNT, genome.inputs)
        assertEquals(SeedBrain.HIDDEN, genome.hidden)
        assertEquals(Outputs.COUNT, genome.outputs)
        assertEquals(Senses.LAYOUT, genome.layout)
    }

    // Circuit 1: go out.
    @Test
    fun theGoOutDriveRisesWithReturnersAndFallsWhenTired() {
        val base = goOut()
        assertTrue(base in 0.2f..0.4f, "baseline $base")
        assertTrue(goOut(Senses.RETURNERS to 0.5f) > 0.9f)
        assertTrue(goOut(Senses.RESERVES to 0.8f) < 0.1f)
    }

    @Test
    fun personalVariationSpreadsTheGoOutBaselineOnlyModerately() {
        val drives = (0 until 50).map { sig(outputs(personal(it), Senses.IN_NEST to 1f)[Outputs.GO_OUT]) }
        val lo = drives.min()
        val hi = drives.max()
        println("go-out baseline over 50 ants: min $lo, max $hi, mean ${drives.average()}")
        assertTrue(lo >= 0.15f && hi <= 0.6f, "baseline go-out from $lo to $hi")
    }

    // The search/return latch: at reserves of 0.64, a searcher keeps searching and a tired ant keeps going home.
    @Test
    fun theSearchAndReturnModesHoldThroughHiddenHistory() {
        fun after(start: Float): FloatArray {
            val b = genome.brain()
            val h = FloatArray(genome.hidden)
            val s = FloatArray(genome.hidden)
            val o = FloatArray(genome.outputs)
            val first = inputs(Senses.RESERVES to start)
            repeat(5) { b.evaluate(first, h, s, o) }
            val then = inputs(Senses.RESERVES to 0.64f)
            repeat(20) { b.evaluate(then, h, s, o) }
            return h
        }
        val wasSearching = after(1f)
        val wasTired = after(0.5f)
        assertTrue(wasSearching[SeedBrain.U_SEARCH] > 0.9f && wasSearching[SeedBrain.U_HOME] < -0.9f, "from searching")
        assertTrue(wasTired[SeedBrain.U_SEARCH] < -0.9f && wasTired[SeedBrain.U_HOME] > 0.9f, "from tired")
    }

    // Circuit 2: search. Task 8b: the turn reads the normalised difference (L - R) / (L + R)
    // (TRAIL_DIFF) instead of each antenna's saturating reading, so it no longer weakens on a
    // strong trail; the test checks that too.
    @Test
    fun theSearchTurnsTowardTheStrongerTrailAsFirmlyOnAStrongTrailAsOnAFaintOne() {
        for (level in floatArrayOf(0.3f, 0.8f)) { // each antenna at about 0.4 T and at 4 T
            val left = turn(outputs(Senses.TRAIL_L to level, Senses.TRAIL_R to level, Senses.TRAIL_DIFF to 0.3f))
            val right = turn(outputs(Senses.TRAIL_L to level, Senses.TRAIL_R to level, Senses.TRAIL_DIFF to -0.3f))
            assertTrue(left > 0.5f && right < -0.5f, "trail level $level: left $left, right $right")
        }
        val faint = turn(outputs(Senses.TRAIL_L to 0.3f, Senses.TRAIL_R to 0.3f, Senses.TRAIL_DIFF to 0.3f))
        val strong = turn(outputs(Senses.TRAIL_L to 0.8f, Senses.TRAIL_R to 0.8f, Senses.TRAIL_DIFF to 0.3f))
        assertEquals(faint, strong, 0.05f * faint, "faint $faint, strong $strong")
    }

    // The trail relay holds between the following state's thresholds: on at T, off below 0.3 T.
    @Test
    fun theTrailRelayLatchesLikeTheFollowingState() {
        fun relay(first: Float, then: Float): Float {
            val b = genome.brain()
            val h = FloatArray(genome.hidden)
            val s = FloatArray(genome.hidden)
            val o = FloatArray(genome.outputs)
            val x = inputs(Senses.TRAIL_L to first / 2f, Senses.TRAIL_R to first / 2f)
            repeat(5) { b.evaluate(x, h, s, o) }
            x[Senses.TRAIL_L] = then / 2f
            x[Senses.TRAIL_R] = then / 2f
            x[Senses.TRAIL_SUM] = trailSum(then / 2f, then / 2f)
            repeat(20) { b.evaluate(x, h, s, o) }
            return h[SeedBrain.U_TRAIL_ON]
        }
        assertTrue(relay(0f, 0.8f) > 0.9f, "a reading above T turns it on")
        assertTrue(relay(0.8f, 0.4f) > 0.9f, "between 0.3 T and T it stays on")
        assertTrue(relay(0f, 0.4f) < -0.9f, "between 0.3 T and T it stays off")
        assertTrue(relay(0.8f, 0.2f) < -0.9f, "below 0.3 T it turns off")
    }

    // Task 8c: the relay reads the total, so it agrees with the following state however unequally
    // the antennae read: one antenna at T and the other at 0 is a trail, as Actions.follow says.
    @Test
    fun theTrailRelayReadsTheTotalOfBothAntennae() {
        val t = AntParams().trailThreshold
        fun relay(l: Float, r: Float): Float {
            val b = genome.brain()
            val h = FloatArray(genome.hidden)
            val s = FloatArray(genome.hidden)
            val o = FloatArray(genome.outputs)
            repeat(5) { b.evaluate(inputs(), h, s, o) } // off the trail first, as an ant comes to one
            val sum = l + r
            val x = inputs(Senses.TRAIL_L to l / (l + t), Senses.TRAIL_R to r / (r + t), Senses.TRAIL_SUM to sum / (sum + t))
            repeat(5) { b.evaluate(x, h, s, o) }
            return h[SeedBrain.U_TRAIL_ON]
        }
        assertTrue(relay(1.1f * t, 0f) > 0.9f, "one antenna above T")
        assertTrue(relay(0.55f * t, 0.55f * t) > 0.9f, "both at half T and a bit")
        assertTrue(relay(0.9f * t, 0f) < -0.9f, "one antenna below T")
    }

    // Task 8c: the exit choice. Near the entrance, a searcher not yet on a trail turns toward the
    // strongest trail on the ring, harder the stronger it is, and not at all with no trail.
    @Test
    fun aSearcherTurnsTowardTheStrongestTrailOnTheRing() {
        fun ring(strengthInT: Float, degrees: Float) = arrayOf(
            Senses.RING to strengthInT / (strengthInT + 1f), Senses.RING_BEARING to degrees / 180f,
            Senses.RING_COS to cos(Math.toRadians(degrees.toDouble())).toFloat(),
        )
        val weak = turn(outputs(*ring(0.5f, 30f)))
        val atT = turn(outputs(*ring(1f, 30f)))
        val strong = turn(outputs(*ring(4f, 30f)))
        val strongRight = turn(outputs(*ring(4f, -30f)))
        val behind = turn(outputs(*ring(4f, 170f)))
        println("ring turn at 30 degrees: 0.5 T $weak, T $atT, 4 T $strong rad/s")
        assertTrue(abs(weak) < 0.2f, "half the threshold $weak")
        assertTrue(atT > 0.5f && atT < 0.6f * strong, "at the threshold $atT, strong $strong")
        assertTrue(strong > 2f, "four times the threshold $strong")
        assertEquals(-strong, strongRight, 1e-4f)
        assertTrue(behind > strong, "a strong trail behind $behind")
        assertEquals(0f, turn(outputs(Senses.RING to 0f, Senses.RING_BEARING to 0f)), 1e-6f)
    }

    @Test
    fun theRingTurnActsOnlyWhileSearchingOffTheTrail() {
        val ring = arrayOf(Senses.RING to 0.8f, Senses.RING_BEARING to 1f / 6f, Senses.RING_COS to 0.87f)
        val searching = turn(outputs(*ring))
        val onTrail = turn(outputs(*ring, Senses.TRAIL_L to 0.5f, Senses.TRAIL_R to 0.5f))
        val homeward = turn(outputs(*ring, *full))
        assertTrue(searching > 2f, "searching $searching")
        assertTrue(abs(onTrail) < 0.05f, "on a trail $onTrail")
        assertTrue(abs(homeward) < 0.05f, "homeward $homeward")
    }

    // Task 8b: on a trail, a searcher turns away from home (path integration), so it follows the trail outward.
    @Test
    fun aFollowerTurnsAwayFromHomeAndASearcherOffTheTrailDoesNot() {
        val trail = arrayOf(Senses.TRAIL_L to 0.5f, Senses.TRAIL_R to 0.5f)
        val homeLeft = turn(outputs(*trail, Senses.HOME_SIN to 0.5f, Senses.HOME_COS to -0.87f))
        val homeRight = turn(outputs(*trail, Senses.HOME_SIN to -0.5f, Senses.HOME_COS to -0.87f))
        assertTrue(homeLeft < -0.5f && homeRight > 0.5f, "home left $homeLeft, home right $homeRight")
        val off = turn(outputs(Senses.HOME_SIN to 0.5f, Senses.HOME_COS to -0.87f))
        assertTrue(abs(off) < 0.05f, "off the trail $off")
    }

    @Test
    fun noiseDrivesTheOutboundWalkOnlyOffTheTrail() {
        assertTrue(noiseSpread() > 3f, "off trail ${noiseSpread()}")
        val onTrail = noiseSpread(Senses.TRAIL_L to 0.6f, Senses.TRAIL_R to 0.6f)
        assertTrue(abs(onTrail) < 0.1f, "on trail $onTrail")
    }

    @Test
    fun footprintsRepelOnlyWhereThereIsATrail() {
        val off = turn(outputs(Senses.FOOT_L to 0.8f, Senses.FOOT_R to 0.2f))
        assertTrue(abs(off) < 0.05f, "off trail $off")
        val on = turn(outputs(Senses.FOOT_L to 0.8f, Senses.FOOT_R to 0.2f, Senses.TRAIL_L to 0.5f, Senses.TRAIL_R to 0.5f))
        assertTrue(on < -1f, "on trail $on")
        val strong = turn(outputs(Senses.FOOT_L to 0.8f, Senses.FOOT_R to 0.2f, Senses.TRAIL_L to 0.9f, Senses.TRAIL_R to 0.9f))
        assertTrue(strong < 0.5f * on, "strong trail $strong, near the threshold $on")
    }

    // C1 and I1 of the Task 6 review: zero-mean noise must not make any ant circle.
    @Test
    fun theWalkHasNoTurningBiasInAnyModeOrForAnyAnt() {
        val modes = mapOf("search" to emptyArray<Pair<Int, Float>>(), "home" to full, "carry" to carrying)
        var worstSeed = 0.0
        var worstPersonal = 0.0
        for ((name, mode) in modes) for (trail in floatArrayOf(0f, 0.25f, 0.5f, 1f)) for (met in floatArrayOf(0f, 1f)) {
            val set = arrayOf(*mode, Senses.TRAIL_L to trail / 2f, Senses.TRAIL_R to trail / 2f, Senses.MET_HONEYDEW to met)
            val label = "$name, trail $trail, metHoneydew $met"
            val seed = meanTurn(genome.brain(), 2_000, 3L, *set)
            worstSeed = maxOf(worstSeed, abs(seed))
            assertTrue(abs(seed) < 0.05, "seed brain, $label: mean turn $seed")
            for (id in 0 until 50) {
                val m = meanTurn(personal(id), 2_000, 100L + id, *set)
                worstPersonal = maxOf(worstPersonal, abs(m))
                assertTrue(abs(m) < 0.1, "ant $id, $label: mean turn $m")
            }
        }
        println("worst mean turn: seed brain $worstSeed rad/s, personal $worstPersonal rad/s")
    }

    @Test
    fun theOutboundWalkIsAsTortuousAsTheScriptedOneAndTheWayHomeStraighter() {
        val out = diffusion()
        val home = diffusion(*full)
        val carry = diffusion(*carrying)
        println("angular diffusion: outbound $out, homeward $home, carrying $carry rad^2/s")
        assertTrue(out in 0.6..1.1, "outbound $out rad^2/s")
        assertTrue(home < 0.3, "homeward $home rad^2/s")
    }

    @Test
    fun foodOdourDrawsTheSearch() {
        assertTrue(turn(outputs(Senses.HONEYDEW to 0.5f, Senses.HONEYDEW_SIN to 0.5f)) > 1f)
        assertTrue(turn(outputs(Senses.PREY to 0.5f, Senses.PREY_SIN to -0.5f)) < -1f)
    }

    // Circuit 3: feed.
    @Test
    fun atFoodFeedingWins() {
        val o = outputs(Senses.AT_FOOD to 1f)
        assertTrue(logit(o, Action.FEED) - logit(o, Action.WALK) >= 5f)
    }

    // Circuit 4: return.
    @Test
    fun aFullForagerSteersHomeStraighterThanItSearched() {
        assertTrue(turn(outputs(*full, Senses.HOME_SIN to 0.5f, Senses.HOME_COS to 0.87f)) > 1f)
        val home = noiseSpread(*full)
        val out = noiseSpread()
        assertTrue(home > 0f && home < 0.3f * out, "noise spread home $home, out $out")
    }

    // Task 8b changed this circuit's level: a fresh forager's mark is now a fraction of the trail
    // threshold, so that only several foragers together make a trail (below about 75 workers
    // trails barely form), and the crowding and saturation cuts are milder (at most 5.6 times).
    @Test
    fun onlyAFullForagerLaysTrailAndLessWhenCrowdedOrOnAStrongTrail() {
        val p = AntParams()
        val fresh = deposit(*full)
        assertTrue(fresh > 2f * Outputs.DEPOSIT_MIN, "full $fresh")
        val mark = p.markAmount * fresh
        assertTrue(mark in 0.2f * p.trailThreshold..0.5f * p.trailThreshold, "a fresh mark $mark against T ${p.trailThreshold}")
        assertTrue(deposit(Senses.CROP to 0.6f, Senses.FED_RECENT to 1f) < Outputs.DEPOSIT_MIN)
        val crowded = deposit(*full, Senses.CONTACT_RATE to 0.67f)
        assertTrue(crowded < 0.8f * fresh && crowded > fresh / 5.6f, "crowded $crowded, fresh $fresh")
        val onTrail = deposit(*full, Senses.TRAIL_L to 0.8f, Senses.TRAIL_R to 0.8f)
        assertTrue(onTrail < 0.9f * fresh && onTrail > fresh / 5.6f, "on a strong trail $onTrail, fresh $fresh")
    }

    // Task 8c: the homeward deposit holds all the way to the nest, highest just after feeding.
    @Test
    fun aFullForagerStillLaysTrailLongAfterFeeding() {
        fun later(seconds: Float) = deposit(Senses.CROP to 1f, Senses.FULL to 1f, Senses.FED_RECENT to exp(-seconds / Senses.FED_TAU))
        val fresh = later(0f)
        val at120 = later(120f)
        val at300 = later(300f)
        println("deposit after feeding: 0 s $fresh, 120 s $at120, 300 s $at300")
        assertTrue(at120 >= Outputs.DEPOSIT_MIN, "120 s after feeding $at120")
        assertTrue(at300 >= Outputs.DEPOSIT_MIN, "300 s after feeding $at300")
        assertTrue(fresh > at300, "fresh $fresh, 300 s $at300")
    }

    // Task 8d: crowding is highest at the entrance, so the cuts together must never stop a full
    // forager marking there; and the strongest deposit is at most 5.6 times the weakest (Czaczkes 2013).
    @Test
    fun theHomewardDepositHoldsUnderCrowdingAndStrongTrails() {
        fun trail(t: Float) = t / (t + 1f) // a reading of t times T, as Senses normalises it
        fun at(contact: Float, t: Float, seconds: Float) = deposit(
            Senses.CROP to 1f, Senses.FULL to 1f,
            Senses.FED_RECENT to exp(-seconds / Senses.FED_TAU),
            Senses.CONTACT_RATE to contact,
            Senses.TRAIL_L to trail(t), Senses.TRAIL_R to trail(t),
        )
        val fresh = at(0f, 0f, 0f)
        val cases = mapOf(
            "crowded, 20 s" to at(0.67f, 0f, 20f),
            "crowded, 60 s" to at(0.67f, 0f, 60f),
            "trail at T, 120 s" to at(0f, 1f, 120f),
            "trail at 2 T, 60 s" to at(0f, 2f, 60f),
            "crowded on a trail at T, fresh" to at(0.67f, 1f, 0f),
        )
        var worst = Float.MAX_VALUE
        for (contact in floatArrayOf(0f, 0.67f, 1f)) {
            for (t in floatArrayOf(0f, 1f, 2f, 3f)) {
                for (seconds in floatArrayOf(0f, 20f, 60f, 120f, 300f)) worst = minOf(worst, at(contact, t, seconds))
            }
        }
        val combined = at(1f, 3f, 300f)
        println("homeward deposit: fresh $fresh, $cases, worst $worst, crowding 1 + 3 T + 300 s $combined")
        for ((name, d) in cases) assertTrue(d >= Outputs.DEPOSIT_MIN, "$name: $d")
        assertTrue(combined >= Outputs.DEPOSIT_MIN, "crowding 1, 3 T, 300 s: $combined")
        assertTrue(worst >= Outputs.DEPOSIT_MIN, "worst $worst")
        assertTrue(fresh / worst <= 5.6f, "fresh $fresh over worst $worst")
    }

    @Test
    fun aHomewardAntEntersAndASearcherDoesNot() {
        assertTrue(logit(outputs(*full), Action.ENTER) >= 5f)
        assertTrue(logit(outputs(), Action.ENTER) <= -5f)
    }

    // Circuit 5: unload and rest.
    @Test
    fun inTheNestAFullCropIsUnloaded() {
        val o = outputs(Senses.IN_NEST to 1f, Senses.CROP to 1f, Senses.FULL to 1f)
        assertTrue(logit(o, Action.UNLOAD) - logit(o, Action.WALK) >= 5f)
        assertTrue(logit(o, Action.REST) <= -9f)
    }

    // Circuit 6: dig.
    @Test
    fun aDiggerDigsWhereThereIsASite() {
        val o = outputs(Senses.IN_NEST to 1f, Senses.DIGGER to 1f, Senses.DIG_SITE to 1f, Senses.ROOM_NEEDED to 1f)
        assertTrue(logit(o, Action.DIG) - logit(o, Action.WALK) >= 3f)
    }

    @Test
    fun aPelletIsCarriedAwayAndDroppedClearOfTheEntrance() {
        val near = outputs(*carrying, Senses.HOME_DIST to 8f / 508f)
        val far = outputs(*carrying, Senses.HOME_DIST to 40f / 540f)
        assertTrue(logit(near, Action.DROP) - logit(near, Action.WALK) <= -5f)
        assertTrue(logit(far, Action.DROP) - logit(far, Action.WALK) >= 3f)
        assertTrue(turn(outputs(*carrying, Senses.HOME_SIN to 0.5f, Senses.HOME_COS to 0.87f)) < -1f)
    }

    // Circuit 7: contacts.
    @Test
    fun meetingASuccessfulForagerRaisesTheGoOutDrive() {
        assertTrue(goOut(Senses.MET_SUCCESS to 1f) > goOut() + 0.5f)
    }

    @Test
    fun aReceivedOdourSteadiesTheSearch() {
        assertTrue(noiseSpread(Senses.MET_HONEYDEW to 1f) < 0.5f * noiseSpread())
        assertTrue(noiseSpread(Senses.MET_PREY to 1f) < 0.5f * noiseSpread())
    }

    @Test
    fun aNudgeTurnsTheSearchAwayFromADeadEnd() {
        assertTrue(turn(outputs(Senses.NUDGE to 0.8f)) < -1f)
    }

    private companion object {
        const val WORLD_SEED = 20261002L

        /** Seconds between two evaluations of a brain (the turn rate holds that long). */
        const val EVAL_SECONDS = 0.1
    }
}

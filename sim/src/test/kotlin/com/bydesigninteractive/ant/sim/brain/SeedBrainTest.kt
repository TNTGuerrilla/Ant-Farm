package com.bydesigninteractive.ant.sim.brain

import com.bydesigninteractive.ant.sim.ant.AntParams
import kotlin.math.abs
import kotlin.math.exp
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

    private fun inputs(vararg set: Pair<Int, Float>): FloatArray {
        val x = FloatArray(Senses.COUNT)
        x[Senses.BIAS] = 1f
        x[Senses.RESERVES] = 1f
        for ((i, v) in set) x[i] = v
        return x
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
            repeat(20) { b.evaluate(x, h, s, o) }
            return h[SeedBrain.U_TRAIL_ON]
        }
        assertTrue(relay(0f, 0.8f) > 0.9f, "a reading above T turns it on")
        assertTrue(relay(0.8f, 0.4f) > 0.9f, "between 0.3 T and T it stays on")
        assertTrue(relay(0f, 0.4f) < -0.9f, "between 0.3 T and T it stays off")
        assertTrue(relay(0.8f, 0.2f) < -0.9f, "below 0.3 T it turns off")
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

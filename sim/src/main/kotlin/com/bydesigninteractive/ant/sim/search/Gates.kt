package com.bydesigninteractive.ant.sim.search

import com.bydesigninteractive.ant.sim.brain.Genome
import java.util.concurrent.Callable
import java.util.concurrent.ExecutorService
import java.util.concurrent.Future

/**
 * The gate terms of the screening (M2a Task 10 fix): short versions of the Gruter test and the
 * depletion switch, the checks a winner must pass, run on one or two seeds so the screening puts
 * selection pressure on them. [Litmus.gateParts] scores them.
 *
 * Added in M2a Task 10 to put selection pressure on the spec section 1 item 5 gate; a change to
 * spec 4.2/4.3, the owner was told.
 */
class Gates(
    /** Following with 30 foragers ([Experiments.gruterSmall], seed 1). */
    val small: Double,
    /** Following with 150 foragers ([Experiments.gruterLarge], seed 1). */
    val largeFollowing: Double,
    /** The busier source's share with 150 foragers (same run). */
    val busierShare: Double,
    /** The loser's share after the switch, per seed ([Experiments.gruterCrowded], seeds 1 and 2; it is bimodal). */
    val crowded: List<Double>,
    /** Feeds at the second source per feed at the first after the first runs out ([Experiments.depletion], seed 1). */
    val depletion: Double,
) {
    val crowdedMean: Double get() = crowded.average()

    companion object {
        const val SMALL_SEED = 1L
        const val LARGE_SEED = 1L
        val CROWDED_SEEDS = listOf(1L, 2L)
        const val DEPLETION_SEED = 1L

        /** Starts the gate runs for [genome] on [pool] at [scale] (1 is the real experiment); [Pending.get] waits for them. */
        fun start(genome: Genome, pool: ExecutorService, scale: Float): Pending = Pending(
            pool.submit(Callable { Experiments.gruterSmall(genome, SMALL_SEED, scale) }),
            pool.submit(Callable { Experiments.gruterLarge(genome, LARGE_SEED, scale) }),
            CROWDED_SEEDS.map { s -> pool.submit(Callable { Experiments.gruterCrowded(genome, s, scale) }) },
            pool.submit(Callable { Experiments.depletion(genome, DEPLETION_SEED, scale) }),
        )
    }

    /** Gate runs in flight. */
    class Pending internal constructor(
        private val small: Future<Double>,
        private val large: Future<Pair<Double, Double>>,
        private val crowded: List<Future<Double>>,
        private val depletion: Future<Double>,
    ) {
        fun get(): Gates {
            val l = large.get()
            return Gates(small.get(), l.first, l.second, crowded.map { it.get() }, depletion.get())
        }
    }
}

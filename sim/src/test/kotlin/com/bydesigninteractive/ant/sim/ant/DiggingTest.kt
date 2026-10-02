package com.bydesigninteractive.ant.sim.ant

import com.bydesigninteractive.ant.sim.World
import com.bydesigninteractive.ant.sim.scenario.Scenarios
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * The nest digging "stall" the owner saw on 2026-10-01 (spec 5.1). Digging keeps going until
 * the colony has its room (ants x volumePerAnt air cells, about minute 104 on seed 1); after
 * that the scripted digger stood still wherever it was, leaving motionless ants down the shaft.
 * Both halves are checked on one 130-minute run of the starter colony.
 */
class DiggingTest {
    private class Run(
        val airAt: IntArray,
        val neededAt: BooleanArray,
        val blockAt: IntArray,
        val parked: List<String>,
        val neededAtEnd: Boolean,
    )

    private companion object {
        const val TICKS_PER_MINUTE = 60 * 20
        const val MINUTES = 130
        val run: Run by lazy { record() }

        fun record(): Run {
            val w = Scenarios.starter(1)
            val n = MINUTES / 10 + 1
            val air = IntArray(n)
            val needed = BooleanArray(n)
            val block = IntArray(n)
            air[0] = w.nest.airCells
            needed[0] = w.digNeeded()
            block[0] = w.excavation.block
            for (m in 1..MINUTES) {
                repeat(TICKS_PER_MINUTE) { w.step() }
                if (m % 10 == 0) {
                    air[m / 10] = w.nest.airCells
                    needed[m / 10] = w.digNeeded()
                    block[m / 10] = w.excavation.block
                }
            }
            val parked = w.ants
                .filter { it.role == Role.DIGGER && it.space == Space.NEST && it.state == AntState.IDLE && !inChamber(w, it) }
                .map { "ant ${it.id} at (${it.cellX}, ${it.cellY})" }
            return Run(air, needed, block, parked, w.digNeeded())
        }

        fun inChamber(w: World, a: Ant): Boolean =
            (0..w.excavation.block).any { w.plan.isChamber(it) && w.plan.rect(it).contains(a.cellX, a.cellY) }
    }

    /** A guard: this passed before M2a too (the probe in Task 1); digging never slows below 100 cells per 10 minutes while room is needed. */
    @Test
    fun theNestGrowsWhileTheColonyNeedsRoom() {
        val r = run
        for (k in 1 until r.airAt.size) {
            if (!r.neededAt[k - 1]) continue
            val grown = r.airAt[k] - r.airAt[k - 1]
            assertTrue(
                grown >= 100 || !r.neededAt[k],
                "minutes ${(k - 1) * 10} to ${k * 10}: air grew by $grown (air ${r.airAt.toList()})",
            )
        }
        assertTrue(r.blockAt[6] >= 6, "plan block at minute 60: ${r.blockAt[6]} (blocks ${r.blockAt.toList()})")
    }

    @Test
    fun onceThereIsRoomIdleDiggersRestInAChamber() {
        val r = run
        assertTrue(!r.neededAtEnd, "the colony still needs room at minute $MINUTES; this test assumes it has its room by then")
        assertTrue(r.parked.isEmpty(), "idle diggers standing outside a chamber: ${r.parked}")
    }
}

package com.bydesigninteractive.ant.sim.scenario

import com.bydesigninteractive.ant.sim.World
import com.bydesigninteractive.ant.sim.ant.AntParams
import com.bydesigninteractive.ant.sim.ant.Role
import com.bydesigninteractive.ant.sim.ant.Space
import com.bydesigninteractive.ant.sim.search.Experiments
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * The exit trail choice between two live trails (M2a Task 11a): Deneubourg's nonlinear choice
 * at the junction (simulation reference section 5; Czaczkes 2017: 74 to 83% per junction). Two
 * fixed straight trails (no decay, no food) leave the entrance west and east, one 4 times the
 * other; 150 foragers are released at once and each one's side is recorded when it first gets
 * 80 mm from the entrance. With n about 2 at least 70% must go out along the stronger trail (82%
 * measured on 8 seeds in the 2026-10-02 diagnosis). One run with the stronger trail west and one
 * with it east, so a side bias cannot pass the test.
 */
class ExitChoiceTest {
    @Test
    fun mostForagersLeaveAlongTheStrongerOfTwoTrails() {
        val west = exits(seed = 1, westT = 4f, eastT = 1f)
        val east = exits(seed = 2, westT = 1f, eastT = 4f)
        val stronger = west[0] + east[1]
        val weaker = west[1] + east[0]
        val total = west.sum() + east.sum()
        val share = stronger.toDouble() / total
        println("exit choice at 4 : 1: stronger $stronger, weaker $weaker, neither ${total - stronger - weaker} of $total (stronger share ${"%.3f".format(share)})")
        assertTrue(share >= 0.7, "stronger trail share $share")
        assertTrue(west[0] > west[1] && east[1] > east[0], "per run: west-strong $west, east-strong $east")
    }

    /** West, east and neither counts for one release of 150 foragers with trails of [westT] and [eastT] times T. */
    private fun exits(seed: Long, westT: Float, eastT: Float): IntArray {
        val w = World(seed, AntParams(trailDecay = 0f), rocks = false)
        w.predig(5)
        val s = w.surface
        layLine(w, -1f, westT * w.params.trailThreshold)
        layLine(w, 1f, eastT * w.params.trailThreshold)
        repeat(FORAGERS) { w.addAnt(Role.FORAGER) }
        val decided = BooleanArray(w.ants.size)
        val res = IntArray(3)
        repeat(3 * Experiments.TICKS_PER_MINUTE) {
            w.step()
            for ((i, a) in w.ants.withIndex()) {
                if (decided[i] || a.space != Space.SURFACE) continue
                val dx = a.x - s.entranceX
                val dy = a.y - s.entranceY
                if (hypot(dx, dy) < DECIDED_AT) continue
                decided[i] = true
                res[if (abs(dy) < abs(dx) * 0.5f) (if (dx < 0) 0 else 1) else 2]++
            }
        }
        return res
    }

    /** A straight trail along the x axis from 6 to 300 mm on one side ([sign]) of the entrance, reading [value] on its centre line 150 mm out. */
    private fun layLine(w: World, sign: Float, value: Float) {
        val s = w.surface
        val f = s.trail
        fun lay(amount: Float) {
            var d = 6f
            while (d <= 300f) {
                val x = s.entranceX + sign * d
                f.add(x, s.entranceY, s.ground.height(x, s.entranceY), amount)
                d += 2f
            }
        }
        lay(1f)
        val x = s.entranceX + sign * 150f
        lay(value / f.get(x, s.entranceY, s.ground.height(x, s.entranceY)) - 1f)
    }

    private companion object {
        const val FORAGERS = 150
        const val DECIDED_AT = 80f
    }
}

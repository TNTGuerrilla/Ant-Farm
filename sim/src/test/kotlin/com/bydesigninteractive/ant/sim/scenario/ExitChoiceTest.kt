package com.bydesigninteractive.ant.sim.scenario

import com.bydesigninteractive.ant.sim.World
import com.bydesigninteractive.ant.sim.ant.AntParams
import com.bydesigninteractive.ant.sim.ant.Role
import com.bydesigninteractive.ant.sim.ant.Space
import com.bydesigninteractive.ant.sim.search.Experiments
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.ln
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * The exit trail choice between two live trails (M2a Task 11a): Deneubourg's nonlinear choice
 * at the junction, (k + s)^n with n about 2 (simulation reference section 5; Czaczkes 2017: 74
 * to 83% per junction). Two fixed straight trails (no decay, no food) leave the entrance west and
 * east, one 4 times the other; 150 foragers are released at once and each one's side is recorded
 * when it first gets 80 mm from the entrance.
 *
 * A linear choice (n = 1) sends 4 times as many out along the stronger trail as along the weaker;
 * the test asks for at least 8 times (4^1.5), pooled over 8 releases: seeds 1 to 4, each with the
 * stronger trail west and then east, so a side bias cannot pass. One release is not 150
 * independent choices (followers meet each other and the footprints of those ahead, and the
 * ground differs by seed), so single releases scatter: in the 2026-10-02 fix round the exponent
 * of one release ranged from 1.0 to 3.1 at 4 : 1 over seeds 1 to 8, and pooling all 16 gave
 * 1938 against 170, an n of 1.76. Seeds 1 to 4 pooled give about 12 to 1 (n about 1.8).
 */
class ExitChoiceTest {
    @Test
    fun aStrongerTrailWinsMoreThanInProportion() {
        val pool = Executors.newFixedThreadPool(minOf(8, Runtime.getRuntime().availableProcessors()))
        val runs = try {
            (1L..4L).flatMap { s -> listOf(s to true, s to false) }
                .map { (s, westStrong) -> pool.submit(Callable { s to (westStrong to exits(s, if (westStrong) 4f else 1f, if (westStrong) 1f else 4f)) }) }
                .map { it.get() }
        } finally {
            pool.shutdown()
        }
        var stronger = 0
        var weaker = 0
        var neither = 0
        for ((seed, r) in runs) {
            val (westStrong, c) = r
            val s = if (westStrong) c[0] else c[1]
            val wk = if (westStrong) c[1] else c[0]
            println("exit choice at 4 : 1, seed $seed, stronger ${if (westStrong) "west" else "east"}: stronger $s, weaker $wk, neither ${c[2]}")
            stronger += s
            weaker += wk
            neither += c[2]
            assertTrue(s > wk, "seed $seed: stronger $s, weaker $wk")
        }
        val n = ln(stronger.toDouble() / weaker) / ln(4.0)
        println("exit choice at 4 : 1 pooled: stronger $stronger, weaker $weaker, neither $neither, exponent ${"%.2f".format(n)}")
        assertTrue(stronger >= 8 * weaker, "stronger $stronger against weaker $weaker (n $n)")
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

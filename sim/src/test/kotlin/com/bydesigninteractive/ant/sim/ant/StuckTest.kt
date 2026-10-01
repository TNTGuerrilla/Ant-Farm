package com.bydesigninteractive.ant.sim.ant

import com.bydesigninteractive.ant.sim.scenario.Scenarios
import kotlin.test.Test
import kotlin.test.fail

/**
 * No surface ant that should be walking stays parked in one spot. Before obstacle detours, a
 * forager heading for a plant behind a rock stopped at the rock's widest point, where the plant
 * direction is almost normal to the surface, and shuttled 1 mm back and forth for 15 minutes.
 */
class StuckTest {
    private val moving = setOf(AntState.SEARCH, AntState.RETURN, AntState.DUMP, AntState.GO_HOME)

    @Test
    fun noWalkingSurfaceAntStaysInOneSpot() {
        val w = Scenarios.starter(1)
        val n = w.ants.size
        val ax = FloatArray(n)
        val ay = FloatArray(n)
        val az = FloatArray(n)
        val since = LongArray(n) { -1L }
        repeat(TICKS) {
            w.step()
            for (a in w.ants) {
                val i = a.id
                if (a.space != Space.SURFACE || a.state !in moving) {
                    since[i] = -1L
                    continue
                }
                val dx = a.x - ax[i]
                val dy = a.y - ay[i]
                val dz = a.z - az[i]
                if (since[i] < 0L || dx * dx + dy * dy + dz * dz > RADIUS * RADIUS) {
                    ax[i] = a.x
                    ay[i] = a.y
                    az[i] = a.z
                    since[i] = w.tick
                } else if (w.tick - since[i] >= LIMIT) {
                    fail(
                        "ant ${a.id} (${a.state}) stayed within $RADIUS mm of " +
                            "(${ax[i]}, ${ay[i]}, ${az[i]}) from tick ${since[i]} to ${w.tick}",
                    )
                }
            }
        }
    }

    private companion object {
        const val TICKS = 12_000
        const val LIMIT = 400L // 20 s
        const val RADIUS = 3f
    }
}

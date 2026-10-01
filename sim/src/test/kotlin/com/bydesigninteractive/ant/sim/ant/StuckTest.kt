package com.bydesigninteractive.ant.sim.ant

import com.bydesigninteractive.ant.sim.scenario.Scenarios
import kotlin.test.Test
import kotlin.math.sqrt
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

    /**
     * A detour that runs up a big rock's face, or is short, re-enters the same basin, and the ant
     * cycles (approach, stall, detour) for ever while moving more than [RADIUS] each time, which
     * the test above cannot see. Here an ant in SEARCH within a plant's odour radius, off the
     * stems and not feeding, must get at least [PROGRESS] mm closer to that plant than its best
     * distance within [WINDOW] ticks.
     */
    @Test
    fun noAntCyclesForeverAroundAPlantTarget() {
        val w = Scenarios.starter(1)
        val n = w.ants.size
        val key = IntArray(n) { Int.MIN_VALUE }
        val best = FloatArray(n)
        val since = LongArray(n)
        val odour = w.params.plantOdourRadius
        repeat(WINDOW_TICKS) {
            w.step()
            for (a in w.ants) {
                val i = a.id
                val plant = if (a.space == Space.SURFACE && a.state == AntState.SEARCH && a.crop <= 0f &&
                    w.surface.sdf.stemAt(a.x, a.y, a.z, w.params.stemTouch) == null
                ) w.surface.nearestPlant(a.x, a.y, odour) else null
                if (plant == null) {
                    key[i] = Int.MIN_VALUE
                    continue
                }
                val dx = plant.x - a.x
                val dy = plant.y - a.y
                val d = sqrt(dx * dx + dy * dy)
                if (key[i] != plant.id || d <= best[i] - PROGRESS) {
                    key[i] = plant.id
                    best[i] = d
                    since[i] = w.tick
                } else if (w.tick - since[i] >= WINDOW) {
                    fail(
                        "ant ${a.id} made no progress toward plant ${plant.id} from tick ${since[i]} " +
                            "to ${w.tick}: best ${best[i]} mm, now $d mm at (${a.x}, ${a.y})",
                    )
                }
            }
        }
    }

    private companion object {
        const val WINDOW = 1_200L // 60 s
        const val PROGRESS = 20f
        const val WINDOW_TICKS = 25_000
        const val TICKS = 12_000
        const val LIMIT = 400L // 20 s
        const val RADIUS = 3f
    }
}

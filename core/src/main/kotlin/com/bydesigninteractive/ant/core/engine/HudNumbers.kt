package com.bydesigninteractive.ant.core.engine

import com.bydesigninteractive.ant.sim.World
import com.bydesigninteractive.ant.sim.ant.Role
import com.bydesigninteractive.ant.sim.ant.Space

/** The numbers the HUD shows, computed on the simulation thread a few times per second. */
data class HudNumbers(
    val tick: Long = 0,
    val seconds: Float = 0f,
    val foragersOut: Int = 0,
    val foragersIn: Int = 0,
    val diggersOut: Int = 0,
    val diggersIn: Int = 0,
    val airCells: Int = 0,
    val activeTiles: Int = 0,
    val storedTiles: Int = 0,
    val planBlock: Int = 0,
    val strongestTrail: Float = 0f,
    val trailBlocks: Int = 0,
    val feeds: Int = 0,
    val unloads: Int = 0,
    val ticksPerSecond: Float = 0f,
    val msPerTickAvg: Float = 0f,
    val msPerTickMax: Float = 0f,
    val resets: Int = 0,
) {
    companion object {
        fun of(w: World, t: TickSummary, resets: Int): HudNumbers {
            var fo = 0
            var fi = 0
            var dout = 0
            var din = 0
            for (i in w.ants.indices) {
                val a = w.ants[i]
                val out = a.space == Space.SURFACE
                if (a.role == Role.FORAGER) { if (out) fo++ else fi++ } else { if (out) dout++ else din++ }
            }
            return HudNumbers(
                w.tick, w.seconds, fo, fi, dout, din,
                w.nest.airCells, w.nest.activeTileCount(), w.nest.modifiedTileCount(), w.excavation.block,
                w.surface.trail.max(), w.surface.trail.blockCount(), w.feedEvents.size, w.unloads,
                t.ticksPerSecond, t.msPerTickAvg, t.msPerTickMax, resets,
            )
        }
    }
}

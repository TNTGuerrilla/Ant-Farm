package com.bydesigninteractive.ant.core.ui

import com.bydesigninteractive.ant.core.stub.FrameStats
import com.bydesigninteractive.ant.sim.World
import com.bydesigninteractive.ant.sim.ant.Ant
import com.bydesigninteractive.ant.sim.ant.Role
import com.bydesigninteractive.ant.sim.ant.Space

/** The debug overlay text: what the world is doing and how fast it runs. */
object DebugReadout {
    fun lines(w: World, view: String, speed: String, fps: Int, frames: FrameStats, followed: Ant?, help: String): List<String> {
        fun count(role: Role, space: Space) = w.ants.count { it.role == role && it.space == space }
        val minutes = (w.seconds / 60).toInt()
        val secs = (w.seconds % 60).toInt()
        return listOf(
            "Ant Farm M1   view: $view   speed: $speed   sim time ${minutes}m ${secs}s   tick ${w.tick}",
            "fps $fps   frame ms p50 ${f1(frames.percentile(0.5f))} p99 ${f1(frames.percentile(0.99f))} max ${f1(frames.max())}",
            "foragers ${count(Role.FORAGER, Space.SURFACE)} out / ${count(Role.FORAGER, Space.NEST)} in   " +
                "diggers ${count(Role.DIGGER, Space.SURFACE)} out / ${count(Role.DIGGER, Space.NEST)} in",
            "nest air ${w.nest.airCells} mm2   active tiles ${w.nest.activeTileCount()}   " +
                "stored tiles ${w.nest.modifiedTileCount()}   plan block ${w.excavation.block}",
            "strongest trail ${f1(w.surface.trail.max())}   trail chunks ${w.surface.trail.allocatedChunks()}   " +
                "feeds ${w.feedEvents.size}   unloads ${w.unloads}",
            followed?.let { "following ant ${it.id} (${it.role.name.lowercase()}, ${it.state.name.lowercase()})" } ?: "following nobody",
            help,
        )
    }

    private fun f1(v: Float) = "%.1f".format(v)
}

package com.bydesigninteractive.ant.core.ui

import com.bydesigninteractive.ant.core.engine.AntPose
import com.bydesigninteractive.ant.core.engine.HudNumbers
import com.bydesigninteractive.ant.core.stub.FrameStats
import java.util.Locale

/**
 * The debug overlay text: what the world is doing and how fast it runs. It reads only the
 * [HudNumbers] the simulation thread publishes, never the live world.
 */
object DebugReadout {
    fun lines(h: HudNumbers, view: String, speed: String, fps: Int, frames: FrameStats, followed: AntPose?, help: String): List<String> {
        val minutes = (h.seconds / 60).toInt()
        val secs = (h.seconds % 60).toInt()
        return listOf(
            "Ant Farm   view: $view   speed: $speed   sim time ${minutes}m ${secs}s   tick ${h.tick}",
            "fps $fps   frame ms p50 ${f1(frames.percentile(0.5f))} p99 ${f1(frames.percentile(0.99f))} max ${f1(frames.max())}   " +
                "ticks/s ${f1(h.ticksPerSecond)}   ms/tick ${f2(h.msPerTickAvg)} max ${f2(h.msPerTickMax)}   resets ${h.resets}",
            "foragers ${h.foragersOut} out / ${h.foragersIn} in   diggers ${h.diggersOut} out / ${h.diggersIn} in",
            "nest air ${h.airCells} mm2   active tiles ${h.activeTiles}   stored tiles ${h.storedTiles}   plan block ${h.planBlock}",
            "strongest trail ${f1(h.strongestTrail)}   trail blocks ${h.trailBlocks}   feeds ${h.feeds}   unloads ${h.unloads}",
            followed?.let { "following ant ${it.id} (${it.role.name.lowercase()}, ${it.state.name.lowercase()})" } ?: "following nobody",
            help,
        )
    }

    private fun f1(v: Float) = "%.1f".format(Locale.ROOT, v)

    private fun f2(v: Float) = "%.2f".format(Locale.ROOT, v)
}

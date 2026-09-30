package com.bydesigninteractive.ant.core.engine

import com.bydesigninteractive.ant.sim.World
import com.bydesigninteractive.ant.sim.world.CHUNKS
import com.bydesigninteractive.ant.sim.world.CHUNK_MM
import com.bydesigninteractive.ant.sim.world.sdf.Blob
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.abs

/** A changed nest tile's cells, immutable once published. */
class TileCopy(val version: Long, val cells: ByteArray)

/** One surface chunk's overlay: trail (column maxima) and spoil counts, 50 x 50 cells each, or null. */
class OverlayChunk(val stamp: Long, val trail: FloatArray?, val spoil: FloatArray?)

/**
 * Everything the render thread may read, filled on the simulation thread: ant snapshots, changed
 * nest tiles, the top-down overlay around the focus, generated rocks, and HUD numbers. The live
 * [World] is never touched by the render thread while the simulation thread runs.
 */
class Published(world: World) {
    val snapshots = SnapshotBuffer()
    val tiles = ConcurrentHashMap<Int, TileCopy>()
    val overlays = ConcurrentHashMap<Int, OverlayChunk>()
    val rocks = ConcurrentHashMap<Int, List<Blob>>()

    @Volatile var placed: List<Blob> = emptyList()

    @Volatile var hud = HudNumbers()

    @Volatile var intervalNanos: Long = intervalFor(1f)

    @Volatile var paused = false

    private var focusX = world.surface.entranceX
    private var focusY = world.surface.entranceY
    private val tileVersions = LongArray(world.nest.tilesX * world.nest.tilesY)
    private var foodViews: List<FoodView> = emptyList()
    private var foodCount = -1
    private var sinceOverlay = OVERLAY_TICKS
    private var sinceHud = HUD_TICKS

    init {
        world.surface.sdf.onGenerated = { cx, cy, blobs -> rocks[cx + cy * CHUNKS] = blobs }
        for (cy in 0 until CHUNKS) for (cx in 0 until CHUNKS) {
            world.surface.sdf.generatedBlobs(cx, cy)?.let { rocks[cx + cy * CHUNKS] = it }
        }
    }

    /** Where the camera looks, for the overlay. Simulation thread only (sent as a command). */
    fun setFocus(x: Float, y: Float) {
        focusX = x
        focusY = y
        sinceOverlay = OVERLAY_TICKS
    }

    /** Publishes the state after a tick. Simulation thread only. */
    fun publish(w: World, now: Long, stats: TickStats, schedule: TickSchedule) {
        if (w.surface.foods.size != foodCount) {
            foodCount = w.surface.foods.size
            foodViews = w.surface.foods.map {
                FoodView(
                    it.x, it.y, it.z, it.radius, it.kind, it.bodyRadius, it.stemRadius, it.stemBase,
                    it.stem?.z1 ?: it.z,
                )
            }
        }
        snapshots.writable().fill(w, now, foodViews)
        snapshots.publish()
        publishTiles(w)
        if (++sinceOverlay >= OVERLAY_TICKS) {
            sinceOverlay = 0
            publishOverlay(w)
            placed = w.surface.sdf.placed.toList()
        }
        if (++sinceHud >= HUD_TICKS) {
            sinceHud = 0
            hud = HudNumbers.of(w, stats.summary(now), schedule.resets)
        }
        intervalNanos = schedule.intervalNanos
    }

    private fun publishTiles(w: World) {
        val g = w.nest
        for (ty in 0 until g.tilesY) for (tx in 0 until g.tilesX) {
            val i = tx + ty * g.tilesX
            val v = g.tileVersion(tx, ty)
            if (v == tileVersions[i]) continue
            tileVersions[i] = v
            val cells = g.copyTile(tx, ty) ?: continue
            tiles[i] = TileCopy(v, cells)
        }
    }

    private fun publishOverlay(w: World) {
        val fcx = (focusX / CHUNK_MM).toInt()
        val fcy = (focusY / CHUNK_MM).toInt()
        val cells = CHUNK_MM / 10
        for (cy in fcy - RING..fcy + RING) for (cx in fcx - RING..fcx + RING) {
            if (cx < 0 || cy < 0 || cx >= CHUNKS || cy >= CHUNKS) continue
            val trail = FloatArray(cells * cells)
            val hasTrail = w.surface.trail.projectMax(cx * CHUNK_MM.toFloat(), cy * CHUNK_MM.toFloat(), cells, trail)
            val spoil = w.surface.spoil.chunk(cx, cy)?.copyOf()
            overlays[cx + cy * CHUNKS] = OverlayChunk(w.tick, if (hasTrail) trail else null, spoil)
        }
        overlays.keys.removeIf { k -> abs(k % CHUNKS - fcx) > RING + 1 || abs(k / CHUNKS - fcy) > RING + 1 }
    }

    private companion object {
        const val OVERLAY_TICKS = 10 // twice a second at 1x
        const val HUD_TICKS = 5
        const val RING = 2
    }
}

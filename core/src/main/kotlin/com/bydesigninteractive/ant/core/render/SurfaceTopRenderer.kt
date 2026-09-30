package com.bydesigninteractive.ant.core.render

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.utils.Disposable
import com.bydesigninteractive.ant.core.engine.AntPose
import com.bydesigninteractive.ant.core.engine.FoodView
import com.bydesigninteractive.ant.core.engine.OverlayChunk
import com.bydesigninteractive.ant.core.engine.Published
import com.bydesigninteractive.ant.sim.ant.Space
import com.bydesigninteractive.ant.sim.world.CHUNKS
import com.bydesigninteractive.ant.sim.world.CHUNK_MM
import com.bydesigninteractive.ant.sim.world.FoodKind
import kotlin.math.floor
import kotlin.math.min

/**
 * Draws the surface from above in millimeters, y up: ground chunks in view, an overlay of spoil
 * and (optionally) trail pheromone, food, the entrance and surface ants. Overlays come from
 * [Published.overlays] (the simulation thread refreshes them around the focus twice a second); a
 * chunk's texture is rebuilt when its published stamp changes.
 */
class SurfaceTopRenderer(
    private val published: Published,
    private val chunks: ChunkTextures,
    private val entranceX: Float,
    private val entranceY: Float,
) : Disposable {
    private val overlays = HashMap<Int, Texture>()
    private val stamps = HashMap<Int, Long>()
    private var builtWithTrail = true

    fun draw(
        batch: SpriteBatch,
        cam: OrthographicCamera,
        poses: List<AntPose>,
        foods: List<FoodView>,
        regions: Array<TextureRegion>,
        animator: AntAnimator,
        pixel: Texture,
        showTrail: Boolean,
    ) {
        val halfW = cam.viewportWidth * cam.zoom / 2
        val halfH = cam.viewportHeight * cam.zoom / 2
        val cx0 = floor((cam.position.x - halfW) / CHUNK_MM).toInt().coerceAtLeast(0)
        val cx1 = floor((cam.position.x + halfW) / CHUNK_MM).toInt().coerceAtMost(CHUNKS - 1)
        val cy0 = floor((cam.position.y - halfH) / CHUNK_MM).toInt().coerceAtLeast(0)
        val cy1 = floor((cam.position.y + halfH) / CHUNK_MM).toInt().coerceAtMost(CHUNKS - 1)
        if (showTrail != builtWithTrail) {
            builtWithTrail = showTrail
            stamps.clear() // the trail toggle changes every overlay
        }
        dropUnpublished()

        for (cy in cy0..cy1) for (cx in cx0..cx1) {
            val x = (cx * CHUNK_MM).toFloat()
            val y = (cy * CHUNK_MM).toFloat()
            val tex = chunks.get(cx, cy)
            if (tex == null) {
                batch.setColor(0.33f, 0.24f, 0.17f, 1f)
                batch.draw(pixel, x, y, CHUNK_MM.toFloat(), CHUNK_MM.toFloat())
                batch.color = Color.WHITE
            } else {
                batch.draw(tex, x, y, CHUNK_MM.toFloat(), CHUNK_MM.toFloat())
            }
            overlay(cx, cy, showTrail)?.let { batch.draw(it, x, y, CHUNK_MM.toFloat(), CHUNK_MM.toFloat()) }
        }

        for (f in foods) {
            if (f.kind == FoodKind.HONEYDEW) batch.setColor(0.25f, 0.55f, 0.20f, 1f) else batch.setColor(0.75f, 0.45f, 0.20f, 1f)
            batch.draw(pixel, f.x - f.radius, f.y - f.radius, f.radius * 2, f.radius * 2)
        }
        batch.setColor(0.05f, 0.04f, 0.03f, 1f)
        batch.draw(pixel, entranceX - 4f, entranceY - 4f, 8f, 8f)
        batch.color = Color.WHITE
        for (p in poses) {
            if (p.space != Space.SURFACE) continue
            batch.draw(regions[animator.frame(p)], p.x - 2.5f, p.y - 1.25f, 2.5f, 1.25f, 5f, 2.5f, 1f, 1f, p.heading * MathUtils.radiansToDegrees)
        }
    }

    /** Frees overlay textures of chunks the simulation thread no longer publishes. */
    private fun dropUnpublished() {
        val it = overlays.entries.iterator()
        while (it.hasNext()) {
            val e = it.next()
            if (published.overlays.containsKey(e.key)) continue
            e.value.dispose()
            it.remove()
            stamps.remove(e.key)
        }
    }

    /** One pixel per field cell: spoil in brown, trail pheromone in yellow. Row 0 is the high-y edge. */
    private fun overlay(cx: Int, cy: Int, showTrail: Boolean): Texture? {
        val key = cx + cy * CHUNKS
        val chunk: OverlayChunk = published.overlays[key] ?: return null
        if (stamps[key] == chunk.stamp) return overlays[key]
        stamps[key] = chunk.stamp
        val trail = if (showTrail) chunk.trail else null
        val spoil = chunk.spoil
        if (trail == null && spoil == null) {
            overlays.remove(key)?.dispose()
            return null
        }
        val n = OVERLAY_CELLS
        val p = Pixmap(n, n, Pixmap.Format.RGBA8888)
        p.blending = Pixmap.Blending.None
        for (ly in 0 until n) for (lx in 0 until n) {
            val i = ly * n + lx
            val row = n - 1 - ly
            val pellets = spoil?.get(i) ?: 0f
            val t = trail?.get(i) ?: 0f
            val color = when {
                pellets > 0f -> Color.rgba8888(0.55f, 0.40f, 0.25f, min(1f, pellets / 3f))
                t > 0f -> Color.rgba8888(0.95f, 0.85f, 0.20f, min(0.8f, t / 8f))
                else -> 0
            }
            p.drawPixel(lx, row, color)
        }
        val tex = overlays[key]?.also { it.draw(p, 0, 0) } ?: Texture(p).also {
            it.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest)
            overlays[key] = it
        }
        p.dispose()
        return tex
    }

    override fun dispose() {
        overlays.values.forEach { it.dispose() }
        overlays.clear()
        stamps.clear()
    }

    private companion object {
        const val OVERLAY_CELLS = CHUNK_MM / 10
    }
}

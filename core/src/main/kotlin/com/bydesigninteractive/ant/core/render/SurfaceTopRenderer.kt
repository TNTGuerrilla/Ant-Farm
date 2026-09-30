package com.bydesigninteractive.ant.core.render

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.utils.Disposable
import com.bydesigninteractive.ant.sim.World
import com.bydesigninteractive.ant.sim.ant.Space
import com.bydesigninteractive.ant.sim.world.CHUNKS
import com.bydesigninteractive.ant.sim.world.CHUNK_MM
import com.bydesigninteractive.ant.sim.world.FoodKind
import kotlin.math.floor
import kotlin.math.min

/**
 * Draws the surface from above in millimeters, y up: ground chunks in view, an overlay of spoil
 * and (optionally) trail pheromone rebuilt twice a second, food, the entrance and surface ants.
 */
class SurfaceTopRenderer(private val world: World, private val chunks: ChunkTextures) : Disposable {
    private val overlays = HashMap<Int, Texture>()
    private var overlayAge = OVERLAY_EVERY
    private val trailCells = FloatArray(OVERLAY_CELLS * OVERLAY_CELLS)

    fun draw(batch: SpriteBatch, cam: OrthographicCamera, ant: TextureRegion, pixel: Texture, showTrail: Boolean, dt: Float) {
        val halfW = cam.viewportWidth * cam.zoom / 2
        val halfH = cam.viewportHeight * cam.zoom / 2
        val cx0 = floor((cam.position.x - halfW) / CHUNK_MM).toInt().coerceAtLeast(0)
        val cx1 = floor((cam.position.x + halfW) / CHUNK_MM).toInt().coerceAtMost(CHUNKS - 1)
        val cy0 = floor((cam.position.y - halfH) / CHUNK_MM).toInt().coerceAtLeast(0)
        val cy1 = floor((cam.position.y + halfH) / CHUNK_MM).toInt().coerceAtMost(CHUNKS - 1)
        overlayAge += dt
        val rebuild = overlayAge >= OVERLAY_EVERY
        if (rebuild) overlayAge = 0f

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
            overlay(cx, cy, showTrail, rebuild)?.let { batch.draw(it, x, y, CHUNK_MM.toFloat(), CHUNK_MM.toFloat()) }
        }

        for (f in world.surface.foods) {
            if (f.kind == FoodKind.HONEYDEW) batch.setColor(0.25f, 0.55f, 0.20f, 1f) else batch.setColor(0.75f, 0.45f, 0.20f, 1f)
            batch.draw(pixel, f.x - f.radius, f.y - f.radius, f.radius * 2, f.radius * 2)
        }
        val s = world.surface
        batch.setColor(0.05f, 0.04f, 0.03f, 1f)
        batch.draw(pixel, s.entranceX - 4f, s.entranceY - 4f, 8f, 8f)
        batch.color = Color.WHITE
        for (a in world.ants) {
            if (a.space != Space.SURFACE) continue
            batch.draw(ant, a.x - 2.5f, a.y - 1.25f, 2.5f, 1.25f, 5f, 2.5f, 1f, 1f, a.heading * MathUtils.radiansToDegrees)
        }
    }

    /** One pixel per field cell: spoil in brown, trail pheromone in yellow. Row 0 is the high-y edge. */
    private fun overlay(cx: Int, cy: Int, showTrail: Boolean, rebuild: Boolean): Texture? {
        val key = cx + cy * CHUNKS
        if (!rebuild) return overlays[key]
        val hasTrail = showTrail &&
            world.surface.trail.projectMax(cx * CHUNK_MM.toFloat(), cy * CHUNK_MM.toFloat(), OVERLAY_CELLS, trailCells)
        val spoil = world.surface.spoil.chunk(cx, cy)
        if (!hasTrail && spoil == null) {
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
            val t = if (hasTrail) trailCells[i] else 0f
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
    }

    private companion object {
        const val OVERLAY_EVERY = 0.5f
        const val OVERLAY_CELLS = CHUNK_MM / 10
    }
}

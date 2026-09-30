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
import com.bydesigninteractive.ant.sim.util.unit
import com.bydesigninteractive.ant.sim.world.Material
import com.bydesigninteractive.ant.sim.world.TILE
import kotlin.math.floor

/**
 * Draws the nest slice in world coordinates of millimeters with y = -depth. Only tiles in view
 * get textures; a tile's texture is rebuilt when its version changes, and at most a few new
 * tiles are built per frame.
 */
class NestRenderer(private val world: World) : Disposable {
    private val textures = HashMap<Int, Texture>()
    private val versions = HashMap<Int, Long>()
    private var built = 0

    fun draw(batch: SpriteBatch, cam: OrthographicCamera, ant: TextureRegion, pixel: Texture) {
        built = 0
        val g = world.nest
        val halfW = cam.viewportWidth * cam.zoom / 2
        val halfH = cam.viewportHeight * cam.zoom / 2
        val tx0 = floor((cam.position.x - halfW) / TILE).toInt().coerceAtLeast(0)
        val tx1 = floor((cam.position.x + halfW) / TILE).toInt().coerceAtMost(g.tilesX - 1)
        val ty0 = floor(-(cam.position.y + halfH) / TILE).toInt().coerceAtLeast(0)
        val ty1 = floor(-(cam.position.y - halfH) / TILE).toInt().coerceAtMost(g.tilesY - 1)

        batch.setColor(0.30f, 0.45f, 0.20f, 1f) // the ground surface above the slice
        batch.draw(pixel, 0f, 0f, g.width.toFloat(), 6f)
        for (ty in ty0..ty1) for (tx in tx0..tx1) {
            val x = (tx * TILE).toFloat()
            val y = -((ty + 1) * TILE).toFloat()
            val tex = tile(tx, ty)
            if (tex == null) {
                batch.setColor(SOIL)
                batch.draw(pixel, x, y, TILE.toFloat(), TILE.toFloat())
            } else {
                batch.color = Color.WHITE
                batch.draw(tex, x, y, TILE.toFloat(), TILE.toFloat())
            }
        }
        batch.color = Color.WHITE
        for (a in world.ants) {
            if (a.space != Space.NEST) continue
            batch.draw(ant, a.x - 2.5f, -a.y - 1.25f, 2.5f, 1.25f, 5f, 2.5f, 1f, 1f, -a.heading * MathUtils.radiansToDegrees)
        }
    }

    private fun tile(tx: Int, ty: Int): Texture? {
        val key = tx + ty * world.nest.tilesX
        val version = world.nest.tileVersion(tx, ty)
        val existing = textures[key]
        if (existing != null && versions[key] == version) return existing
        if (existing == null && built >= BUILDS_PER_FRAME) return null
        built++
        val p = pixmap(tx, ty)
        val tex = existing?.also { it.draw(p, 0, 0) } ?: Texture(p).also {
            it.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest)
            textures[key] = it
        }
        p.dispose()
        versions[key] = version
        return tex
    }

    /** Pixel row 0 is the tile's top edge (the smallest depth). */
    private fun pixmap(tx: Int, ty: Int): Pixmap {
        val p = Pixmap(TILE, TILE, Pixmap.Format.RGBA8888)
        val g = world.nest
        for (cy in 0 until TILE) for (cx in 0 until TILE) {
            val x = tx * TILE + cx
            val y = ty * TILE + cy
            val v = 0.88f + unit(world.seed, x, y).toFloat() * 0.24f
            val c = when (g.material(x, y)) {
                Material.AIR -> AIR
                Material.SOIL -> SOIL
                Material.CLAY -> CLAY
                Material.STONE -> STONE
                Material.WATER -> WATER
            }
            p.drawPixel(cx, cy, Color.rgba8888(c.r * v, c.g * v, c.b * v, 1f))
        }
        return p
    }

    override fun dispose() {
        textures.values.forEach { it.dispose() }
        textures.clear()
    }

    private companion object {
        const val BUILDS_PER_FRAME = 16
        val AIR = Color(0.09f, 0.07f, 0.05f, 1f)
        val SOIL = Color(0.40f, 0.29f, 0.20f, 1f)
        val CLAY = Color(0.55f, 0.38f, 0.25f, 1f)
        val STONE = Color(0.50f, 0.50f, 0.48f, 1f)
        val WATER = Color(0.20f, 0.28f, 0.35f, 1f)
    }
}

package com.bydesigninteractive.ant.core.render

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.utils.Disposable
import com.bydesigninteractive.ant.sim.util.unit
import com.bydesigninteractive.ant.sim.world.CHUNKS
import com.bydesigninteractive.ant.sim.world.CHUNK_MM
import com.bydesigninteractive.ant.sim.world.ChunkContent
import com.bydesigninteractive.ant.sim.world.SurfaceMap
import kotlin.math.abs
import kotlin.math.floor

/**
 * Ground textures for surface chunks, built on demand (a few per frame) and freed when the
 * camera moves away. Pixel row 0 is the chunk's high-y edge, so the texture draws upright in
 * both the top-down view and the 2.5D view.
 */
class ChunkTextures(private val surface: SurfaceMap) : Disposable {
    private val textures = HashMap<Int, Texture>()
    private var builtThisFrame = 0

    fun newFrame() {
        builtThisFrame = 0
    }

    /** The chunk's texture, or null while it waits its turn to be built. */
    fun get(cx: Int, cy: Int): Texture? {
        val key = cx + cy * CHUNKS
        textures[key]?.let { return it }
        if (builtThisFrame >= BUILDS_PER_FRAME) return null
        builtThisFrame++
        return build(surface.chunk(cx, cy)).also { textures[key] = it }
    }

    /** Frees textures of chunks more than [keep] chunks away from (cx, cy). */
    fun evict(cx: Int, cy: Int, keep: Int) {
        val it = textures.entries.iterator()
        while (it.hasNext()) {
            val e = it.next()
            val kx = e.key % CHUNKS
            val ky = e.key / CHUNKS
            if (abs(kx - cx) > keep || abs(ky - cy) > keep) {
                e.value.dispose()
                it.remove()
            }
        }
    }

    private fun build(c: ChunkContent): Texture {
        val p = Pixmap(PX, PX, Pixmap.Format.RGBA8888)
        val mm = CHUNK_MM / PX.toFloat()
        val x0 = c.cx * CHUNK_MM
        val y0 = c.cy * CHUNK_MM
        for (row in 0 until PX) for (col in 0 until PX) {
            val gy = PX - 1 - row
            val grain = unit(surface.seed, c.cx * PX + col, c.cy * PX + gy).toFloat()
            val wx = x0 + (col + 0.5f) * mm
            val wy = y0 + (gy + 0.5f) * mm
            val patch = unit(surface.seed xor LITTER_SALT, floor(wx / 40f).toInt(), floor(wy / 40f).toInt()).toFloat()
            val v = 0.85f + grain * 0.3f
            val color = if (patch < c.litter * 0.4f) {
                Color.rgba8888(0.36f * v, 0.27f * v, 0.13f * v, 1f)
            } else {
                Color.rgba8888(0.33f * v, 0.24f * v, 0.17f * v, 1f)
            }
            p.drawPixel(col, row, color)
        }
        p.setColor(0.46f, 0.45f, 0.42f, 1f)
        for (s in c.stones) p.fillCircle(col(s.x, x0, mm), row(s.y, y0, mm), (s.radius / mm).toInt())
        p.setColor(0.40f, 0.39f, 0.37f, 1f)
        for (s in c.rocks) p.fillCircle(col(s.x, x0, mm), row(s.y, y0, mm), (s.radius / mm).toInt())
        p.setColor(0.22f, 0.35f, 0.12f, 1f)
        for (t in c.tufts) p.fillCircle(col(t.x, x0, mm), row(t.y, y0, mm), 3)
        return Texture(p).also {
            p.dispose()
            it.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear)
        }
    }

    private fun col(x: Float, x0: Int, mm: Float) = ((x - x0) / mm).toInt()

    private fun row(y: Float, y0: Int, mm: Float) = PX - 1 - ((y - y0) / mm).toInt()

    override fun dispose() {
        textures.values.forEach { it.dispose() }
        textures.clear()
    }

    private companion object {
        const val PX = 256
        const val BUILDS_PER_FRAME = 2
        const val LITTER_SALT = 0x1177L
    }
}

package com.bydesigninteractive.ant.core.render

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.utils.Disposable
import com.bydesigninteractive.ant.core.render.world.ChunkMesher
import com.bydesigninteractive.ant.sim.util.hash
import com.bydesigninteractive.ant.sim.util.unit
import com.bydesigninteractive.ant.sim.world.CHUNKS
import com.bydesigninteractive.ant.sim.world.CHUNK_MM
import kotlin.math.abs
import kotlin.math.floor

/**
 * Bare-ground textures for surface chunks, built on demand (a few per frame) and freed when the
 * camera moves away. Pixel row 0 is the chunk's high-y edge, so the texture draws upright.
 *
 * The colour is the 3D view's soil (about RGB 139/98/62) with the same kind of per-facet variation
 * (0.88 to 1.12): the ground is split into small seeded facets (the nearest of jittered points on
 * a [FACET_MM] grid), each with its own factor, under a gentle broad shading and a faint grain.
 * Every term is a function of world coordinates at texel centres, never of the chunk, so
 * neighbouring textures meet without a seam. Rocks and grass are drawn on top by
 * [SurfaceTopRenderer], not baked in, so nothing is clipped at chunk borders.
 */
class ChunkTextures(private val seed: Long) : Disposable {
    private val textures = HashMap<Int, Texture>()
    private var builtThisFrame = 0

    /** Scratch: the facet points around one chunk (x, y and factor per point). */
    private val pointX = FloatArray(GRID * GRID)
    private val pointY = FloatArray(GRID * GRID)
    private val pointF = FloatArray(GRID * GRID)

    fun newFrame() {
        builtThisFrame = 0
    }

    /** The chunk's texture, or null while it waits its turn to be built. */
    fun get(cx: Int, cy: Int): Texture? {
        val key = cx + cy * CHUNKS
        textures[key]?.let { return it }
        if (builtThisFrame >= BUILDS_PER_FRAME) return null
        builtThisFrame++
        return build(cx, cy).also { textures[key] = it }
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

    private fun build(cx: Int, cy: Int): Texture {
        val mm = CHUNK_MM / PX.toFloat()
        val x0 = cx * CHUNK_MM
        val y0 = cy * CHUNK_MM
        // Facet points for the chunk's facet cells plus a one-cell border, in world facet indices.
        val fi0 = floor(x0 / FACET_MM).toInt() - 1
        val fj0 = floor(y0 / FACET_MM).toInt() - 1
        for (j in 0 until GRID) for (i in 0 until GRID) {
            val gi = fi0 + i
            val gj = fj0 + j
            val h = hash(seed xor FACET_SALT, gi, gj)
            val k = i + j * GRID
            pointX[k] = (gi + 0.1f + ((h and 0xFFF).toFloat() / 4095f) * 0.8f) * FACET_MM
            pointY[k] = (gj + 0.1f + (((h ushr 12) and 0xFFF).toFloat() / 4095f) * 0.8f) * FACET_MM
            pointF[k] = 0.88f + (((h ushr 24) and 0xFF).toFloat() / 255f) * 0.24f
        }
        val p = Pixmap(PX, PX, Pixmap.Format.RGBA8888)
        p.blending = Pixmap.Blending.None
        for (row in 0 until PX) for (col in 0 until PX) {
            val gy = PX - 1 - row
            val wx = x0 + (col + 0.5f) * mm
            val wy = y0 + (gy + 0.5f) * mm
            val f = facet(wx, wy, fi0, fj0) * broad(wx, wy) *
                (0.98f + unit(seed xor GRAIN_SALT, cx * PX + col, cy * PX + gy).toFloat() * 0.04f)
            p.drawPixel(col, row, Color.rgba8888(SOIL_R * f, SOIL_G * f, SOIL_B * f, 1f))
        }
        return Texture(p).also {
            p.dispose()
            it.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear)
        }
    }

    /** The factor of the facet (nearest jittered point) holding world point (x, y). */
    private fun facet(x: Float, y: Float, fi0: Int, fj0: Int): Float {
        val ci = floor(x / FACET_MM).toInt() - fi0
        val cj = floor(y / FACET_MM).toInt() - fj0
        var best = Float.MAX_VALUE
        var f = 1f
        for (j in cj - 1..cj + 1) for (i in ci - 1..ci + 1) {
            val k = i + j * GRID
            val dx = pointX[k] - x
            val dy = pointY[k] - y
            val d = dx * dx + dy * dy
            if (d < best) {
                best = d
                f = pointF[k]
            }
        }
        return f
    }

    /** Smooth seeded value noise over [BROAD_MM], 0.95 to 1.05, for gentle patches of lighter and darker ground. */
    private fun broad(x: Float, y: Float): Float {
        val u = x / BROAD_MM
        val v = y / BROAD_MM
        val i = floor(u).toInt()
        val j = floor(v).toInt()
        val fu = smooth(u - i)
        val fv = smooth(v - j)
        val a = unit(seed xor BROAD_SALT, i, j).toFloat()
        val b = unit(seed xor BROAD_SALT, i + 1, j).toFloat()
        val c = unit(seed xor BROAD_SALT, i, j + 1).toFloat()
        val d = unit(seed xor BROAD_SALT, i + 1, j + 1).toFloat()
        val n = (a + (b - a) * fu) + ((c + (d - c) * fu) - (a + (b - a) * fu)) * fv
        return 0.95f + n * 0.1f
    }

    private fun smooth(t: Float) = t * t * (3f - 2f * t)

    override fun dispose() {
        textures.values.forEach { it.dispose() }
        textures.clear()
    }

    companion object {
        /** The soil colour of the 3D view, shared so both views match. */
        const val SOIL_R = ChunkMesher.SOIL_R
        const val SOIL_G = ChunkMesher.SOIL_G
        const val SOIL_B = ChunkMesher.SOIL_B
        private const val PX = 256
        private const val BUILDS_PER_FRAME = 2

        /** Facet spacing in mm, about the size of the 3D ground's triangles. */
        private const val FACET_MM = CHUNK_MM / 60f
        private const val BROAD_MM = 160f

        /** Facet cells covered by one chunk plus a cell of border each side and one more for rounding. */
        private const val GRID = 60 + 4
        private const val FACET_SALT = 0x6FACL
        private const val GRAIN_SALT = 0x62A1L
        private const val BROAD_SALT = 0xB20AL
    }
}

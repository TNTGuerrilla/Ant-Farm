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
import com.bydesigninteractive.ant.core.render.world.ChunkMesher
import com.bydesigninteractive.ant.core.render.world.GrassField
import com.bydesigninteractive.ant.sim.ant.Space
import com.bydesigninteractive.ant.sim.world.CHUNKS
import com.bydesigninteractive.ant.sim.world.CHUNK_MM
import com.bydesigninteractive.ant.sim.world.FoodKind
import com.bydesigninteractive.ant.sim.world.sdf.Blob
import kotlin.math.floor
import kotlin.math.min
import kotlin.math.sqrt

/**
 * Draws the surface from above in millimeters, y up: ground chunks in view, an overlay of spoil
 * and (optionally) trail pheromone, grass, rocks, food, the entrance and surface ants. Overlays
 * come from [Published.overlays] (the simulation thread refreshes them around the focus twice a
 * second); a chunk's overlay texture is rebuilt when its published stamp changes.
 *
 * Rocks and grass match the 3D view and are drawn over the ground as sprites, so none is clipped
 * at a chunk border. Rocks are the published lists the 3D view meshes ([Published.rocks] and
 * [Published.placed]), drawn as stone-grey ellipses rx by ry. Grass is the 3D view's own
 * [GrassField] placement, computed with a render-thread [ChunkMesher] whose spoil mirrors the
 * published overlays (as the 3D view's does) and cached per chunk until its spoil or the rocks
 * near it change. Render thread only; [seed] is the immutable value read before the simulation
 * thread started.
 */
class SurfaceTopRenderer(
    seed: Long,
    private val published: Published,
    private val chunks: ChunkTextures,
    private val entranceX: Float,
    private val entranceY: Float,
) : Disposable {
    private val overlays = HashMap<Int, Texture>()
    private val stamps = HashMap<Int, Long>()
    private var builtWithTrail = true

    /** The render thread's own mesher for grass placement: heights and the mirrored spoil. */
    private val mesher = ChunkMesher(seed)
    private val grassField = GrassField(seed)

    /** A white disc with a soft edge, stretched into rock ellipses and grass marks. */
    private val disc = discTexture()

    /** One chunk's grass instances and the inputs they were placed with. */
    private class GrassChunk(
        var sources: Array<List<Blob>?>,
        val spoil: FloatArray?,
        val rocks: List<Blob>,
        val tufts: FloatArray,
    )

    private val grass = HashMap<Int, GrassChunk>()

    /** The last published spoil of each chunk near the view, mirrored into [mesher]. */
    private val knownSpoil = HashMap<Int, FloatArray?>()

    /** The overlay object last seen per chunk: its spoil is compared only when a new one is published. */
    private val seenOverlay = HashMap<Int, OverlayChunk>()

    /** Scratch for the sources of one chunk's grass rocks (3 by 3 published lists, then the placed list). */
    private val sourceScratch = arrayOfNulls<List<Blob>>(10)
    private var grassBuilt = 0

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
                batch.setColor(ChunkTextures.SOIL_R, ChunkTextures.SOIL_G, ChunkTextures.SOIL_B, 1f)
                batch.draw(pixel, x, y, CHUNK_MM.toFloat(), CHUNK_MM.toFloat())
                batch.color = Color.WHITE
            } else {
                batch.draw(tex, x, y, CHUNK_MM.toFloat(), CHUNK_MM.toFloat())
            }
            overlay(cx, cy, showTrail)?.let { batch.draw(it, x, y, CHUNK_MM.toFloat(), CHUNK_MM.toFloat()) }
        }

        mirrorSpoil(cx0, cy0, cx1, cy1)
        grassBuilt = 0
        for (cy in cy0..cy1) for (cx in cx0..cx1) grassFor(cx, cy)?.let { drawGrass(batch, it) }
        dropFar(cx0, cy0, cx1, cy1)
        drawRocks(batch, cam.position.x - halfW, cam.position.y - halfH, cam.position.x + halfW, cam.position.y + halfH, cx0, cy0, cx1, cy1)

        for (k in foods.indices) {
            val f = foods[k]
            if (f.kind == FoodKind.HONEYDEW) batch.setColor(0.25f, 0.55f, 0.20f, 1f) else batch.setColor(0.75f, 0.45f, 0.20f, 1f)
            batch.draw(pixel, f.x - f.radius, f.y - f.radius, f.radius * 2, f.radius * 2)
        }
        batch.setColor(0.05f, 0.04f, 0.03f, 1f)
        batch.draw(pixel, entranceX - 4f, entranceY - 4f, 8f, 8f)
        batch.color = Color.WHITE
        for (k in poses.indices) {
            val p = poses[k]
            if (p.space != Space.SURFACE) continue
            batch.draw(regions[animator.frame(p)], p.x - 2.5f, p.y - 1.25f, 2.5f, 1.25f, 5f, 2.5f, 1f, 1f, p.heading * MathUtils.radiansToDegrees)
        }
    }

    /**
     * Mirrors the published spoil of the chunks in view (and one beyond) into [mesher], as the 3D
     * view does. A new overlay object is compared by content, so an unchanged chunk keeps its array.
     */
    private fun mirrorSpoil(cx0: Int, cy0: Int, cx1: Int, cy1: Int) {
        for (cy in cy0 - 1..cy1 + 1) for (cx in cx0 - 1..cx1 + 1) {
            if (cx < 0 || cy < 0 || cx >= CHUNKS || cy >= CHUNKS) continue
            val key = cx + cy * CHUNKS
            val overlay = published.overlays[key] ?: continue
            if (seenOverlay[key] === overlay) continue
            seenOverlay[key] = overlay
            val old = knownSpoil[key]
            val now = overlay.spoil
            if (knownSpoil.containsKey(key) && (old === now || (old != null && now != null && old.contentEquals(now)))) continue
            knownSpoil[key] = now
            mesher.setSpoil(cx, cy, now)
        }
    }

    /**
     * The grass instances of chunk (cx, cy), as [GrassField.tufts] gives them, or null while the
     * chunk waits its turn. Placed again when the chunk's mirrored spoil or the rocks reaching into
     * it change; at most [GRASS_BUILDS_PER_FRAME] chunks are placed per frame.
     */
    private fun grassFor(cx: Int, cy: Int): FloatArray? {
        val key = cx + cy * CHUNKS
        val sources = sourceScratch
        java.util.Arrays.fill(sources, null)
        for (dy in -1..1) for (dx in -1..1) {
            val nx = cx + dx
            val ny = cy + dy
            if (nx < 0 || ny < 0 || nx >= CHUNKS || ny >= CHUNKS) continue
            sources[(dx + 1) + (dy + 1) * 3] = published.rocks[nx + ny * CHUNKS]
        }
        sources[9] = published.placed
        val spoil = knownSpoil[key]
        val entry = grass[key]
        if (entry != null && entry.spoil === spoil && sameSources(entry.sources, sources)) return entry.tufts
        val rocks = grassRocks(cx, cy, sources)
        if (entry != null && entry.spoil === spoil && entry.rocks == rocks) {
            entry.sources = sources.copyOf()
            return entry.tufts
        }
        if (grassBuilt >= GRASS_BUILDS_PER_FRAME) return entry?.tufts
        grassBuilt++
        val tufts = grassField.tufts(cx, cy, mesher, rocks)
        grass[key] = GrassChunk(sources.copyOf(), spoil, rocks, tufts)
        return tufts
    }

    private fun sameSources(a: Array<List<Blob>?>, b: Array<List<Blob>?>): Boolean {
        for (i in a.indices) if (a[i] !== b[i]) return false
        return true
    }

    /**
     * The rocks grass in chunk (cx, cy) avoids, as in the 3D view: the chunk's own published rocks,
     * and the neighbours' and placed ones whose footprint, widened by [GrassField.ROCK_CLEARANCE],
     * reaches into the chunk.
     */
    private fun grassRocks(cx: Int, cy: Int, sources: Array<List<Blob>?>): List<Blob> {
        val pad = GrassField.ROCK_CLEARANCE
        val x0 = cx * CHUNK_MM - pad
        val y0 = cy * CHUNK_MM - pad
        val x1 = (cx + 1) * CHUNK_MM + pad
        val y1 = (cy + 1) * CHUNK_MM + pad
        val out = ArrayList<Blob>(sources[4] ?: emptyList())
        for (i in sources.indices) {
            if (i == 4) continue
            sources[i]?.forEach { b ->
                if (b.cx + b.reach >= x0 && b.cx - b.reach <= x1 && b.cy + b.reach >= y0 && b.cy - b.reach <= y1) out += b
            }
        }
        return out
    }

    /** A small dark-green mark per tuft: three blade strokes over a core, sized by the tuft's scale and tinted by its tint. */
    private fun drawGrass(batch: SpriteBatch, tufts: FloatArray) {
        val n = GrassField.INSTANCE_FLOATS
        var i = 0
        while (i + n <= tufts.size) {
            val x = tufts[i]
            val y = tufts[i + 1]
            val rot = tufts[i + 3] * MathUtils.radiansToDegrees
            val scale = tufts[i + 4]
            val tint = tufts[i + 6]
            batch.setColor(GRASS_R * tint, GRASS_G * tint, GRASS_B * tint, 1f)
            val len = BLADE_MM * scale
            for (k in 0 until 3) {
                batch.draw(disc, x - len / 2, y - BLADE_W / 2, len / 2, BLADE_W / 2, len, BLADE_W, 1f, 1f, rot + k * 60f, 0, 0, disc.width, disc.height, false, false)
            }
            val core = CORE_MM * scale
            batch.draw(disc, x - core, y - core, core * 2, core * 2)
            i += n
        }
        batch.color = Color.WHITE
    }

    /** Stone-grey ellipses rx by ry for the published rocks of the chunks in view (and their neighbours) and the placed objects. */
    private fun drawRocks(batch: SpriteBatch, vx0: Float, vy0: Float, vx1: Float, vy1: Float, cx0: Int, cy0: Int, cx1: Int, cy1: Int) {
        for (cy in cy0 - 1..cy1 + 1) for (cx in cx0 - 1..cx1 + 1) {
            if (cx < 0 || cy < 0 || cx >= CHUNKS || cy >= CHUNKS) continue
            val list = published.rocks[cx + cy * CHUNKS] ?: continue
            for (b in list) drawRock(batch, b, vx0, vy0, vx1, vy1)
        }
        for (b in published.placed) drawRock(batch, b, vx0, vy0, vx1, vy1)
        batch.color = Color.WHITE
    }

    private fun drawRock(batch: SpriteBatch, b: Blob, vx0: Float, vy0: Float, vx1: Float, vy1: Float) {
        if (b.cx + b.rx < vx0 || b.cx - b.rx > vx1 || b.cy + b.ry < vy0 || b.cy - b.ry > vy1) return
        val f = mesher.rockShade(b)
        batch.setColor(ChunkMesher.STONE_R * f, ChunkMesher.STONE_G * f, ChunkMesher.STONE_B * f, 1f)
        batch.draw(disc, b.cx - b.rx, b.cy - b.ry, b.rx * 2, b.ry * 2)
    }

    /** Forgets the grass and the mirrored spoil of chunks well outside the view. */
    private fun dropFar(cx0: Int, cy0: Int, cx1: Int, cy1: Int) {
        fun far(key: Int, margin: Int): Boolean {
            val kx = key % CHUNKS
            val ky = key / CHUNKS
            return kx < cx0 - margin || kx > cx1 + margin || ky < cy0 - margin || ky > cy1 + margin
        }
        grass.keys.removeIf { far(it, 2) }
        val it = knownSpoil.keys.iterator()
        while (it.hasNext()) {
            val key = it.next()
            if (!far(key, 3)) continue
            mesher.setSpoil(key % CHUNKS, key / CHUNKS, null)
            it.remove()
        }
        seenOverlay.keys.removeIf { far(it, 3) }
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
                pellets > 0f -> Color.rgba8888(SPOIL_R, SPOIL_G, SPOIL_B, min(1f, pellets / 3f))
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
        grass.clear()
        disc.dispose()
    }

    private companion object {
        const val OVERLAY_CELLS = CHUNK_MM / 10
        const val GRASS_BUILDS_PER_FRAME = 4
        const val DISC_PX = 64

        /** Spoil: excavated subsoil, a lighter brown than the ground (139/98/62) so the mound shows. */
        const val SPOIL_R = 0.68f
        const val SPOIL_G = 0.52f
        const val SPOIL_B = 0.34f

        /** Grass marks: the 3D grass (96/132/70) darkened, as tufts look from above among their own shade. */
        const val GRASS_R = 96f / 255f * 0.72f
        const val GRASS_G = 132f / 255f * 0.72f
        const val GRASS_B = 70f / 255f * 0.72f

        /** Blade stroke length (mm) at scale 1, its width, and the core radius at scale 1. */
        const val BLADE_MM = 14f
        const val BLADE_W = 1.4f
        const val CORE_MM = 2.5f

        /** A white disc filling the texture, its alpha falling off over the outermost pixel. */
        fun discTexture(): Texture {
            val p = Pixmap(DISC_PX, DISC_PX, Pixmap.Format.RGBA8888)
            p.blending = Pixmap.Blending.None
            val r = DISC_PX / 2f
            for (y in 0 until DISC_PX) for (x in 0 until DISC_PX) {
                val dx = x + 0.5f - r
                val dy = y + 0.5f - r
                val a = (r - sqrt(dx * dx + dy * dy)).coerceIn(0f, 1f)
                p.drawPixel(x, y, Color.rgba8888(1f, 1f, 1f, a))
            }
            return Texture(p).also {
                p.dispose()
                it.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear)
            }
        }
    }
}

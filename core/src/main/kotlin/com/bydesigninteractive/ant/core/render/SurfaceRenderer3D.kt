package com.bydesigninteractive.ant.core.render

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.Mesh
import com.badlogic.gdx.graphics.PerspectiveCamera
import com.badlogic.gdx.graphics.VertexAttribute
import com.badlogic.gdx.graphics.VertexAttributes
import com.badlogic.gdx.graphics.glutils.ShaderProgram
import com.badlogic.gdx.utils.Disposable
import com.bydesigninteractive.ant.core.camera.ChaseCamera3
import com.bydesigninteractive.ant.core.engine.AntPose
import com.bydesigninteractive.ant.core.engine.FoodView
import com.bydesigninteractive.ant.core.engine.OverlayChunk
import com.bydesigninteractive.ant.core.engine.Published
import com.bydesigninteractive.ant.core.render.ant.AntRenderer
import com.bydesigninteractive.ant.core.render.sky.DayCycle
import com.bydesigninteractive.ant.core.render.sky.SkyState
import com.bydesigninteractive.ant.core.render.world.ChunkCache
import com.bydesigninteractive.ant.core.render.world.ChunkMesher
import com.bydesigninteractive.ant.core.render.world.GrassRenderer
import com.bydesigninteractive.ant.core.render.world.MeshData
import com.bydesigninteractive.ant.sim.world.CHUNKS
import com.bydesigninteractive.ant.sim.world.CHUNK_MM
import com.bydesigninteractive.ant.sim.world.sdf.Blob
import kotlin.math.abs
import kotlin.math.floor

/**
 * The low-poly 3D surface view. Chunks in a 5 by 5 ring around the camera focus are built by a
 * [ChunkCache] from published data and uploaded here, at most [UPLOADS_PER_FRAME] per frame; a
 * chunk is re-requested when its own or a neighbour's published spoil, or its rock list, changes,
 * at most every [REBUILD_NANOS] per chunk. The sky state comes from the [DayCycle]. Works in
 * simulation coordinates (mm, z up). Render thread only, except the cache's own thread.
 *
 * GL state on return matches what the debug renderer's ModelBatch leaves: depth test, face culling
 * and blending off, depth writes on, so the HUD's SpriteBatch draws over it unchanged.
 */
class SurfaceRenderer3D(seed: Long, private val published: Published) : Disposable {
    val camera = PerspectiveCamera(FOV, 1f, 1f).apply {
        near = 2f
        far = 3000f
    }
    private val sky = SkyState()

    /** The render thread's own mesher: camera heights (with mirrored spoil) and the food mesh, which reads no heights. */
    private val local = ChunkMesher(seed)
    private val world: ShaderProgram
    private val skyShader: ShaderProgram
    private val skyQuad: Mesh
    private val ants: AntRenderer
    private val grass: GrassRenderer

    /** Started last, so a failure above leaves no thread running. */
    private val cache: ChunkCache

    init {
        // GL objects first; if any step fails, dispose what was made and rethrow, so the caller can fall back.
        val made = ArrayList<Disposable>()
        try {
            world = Shaders.world().also { made += it }
            skyShader = Shaders.sky().also { made += it }
            skyQuad = Mesh(true, 4, 6, VertexAttribute(VertexAttributes.Usage.Position, 2, "a_position")).also { made += it }
            skyQuad.setVertices(floatArrayOf(-1f, -1f, 1f, -1f, 1f, 1f, -1f, 1f))
            skyQuad.setIndices(shortArrayOf(0, 1, 2, 0, 2, 3))
            ants = AntRenderer().also { made += it }
            grass = GrassRenderer(seed).also { made += it }
            cache = ChunkCache(seed)
        } catch (e: Exception) {
            for (d in made.asReversed()) d.dispose()
            throw e
        }
    }

    /**
     * One chunk's uploaded meshes and the published inputs it was last requested with: the 3 by 3
     * spoil block around it ([spoils], index (dx + 1) + (dy + 1) * 3) and its rock list
     * ([rockList]); [combined] is the current rock list, cached with the identities ([ownRocks],
     * [placed]) it was combined from.
     */
    private class Slot {
        var version = 0L
        var ground: Mesh? = null
        var rocks: Mesh? = null
        var grass = FloatArray(0)
        var spoils: Array<FloatArray?>? = null
        var rockList: List<Blob>? = null
        var combined: List<Blob>? = null
        var ownRocks: List<Blob>? = null
        var placed: List<Blob>? = null
        var requestedAt = 0L
        var requested = 0L
    }

    private val slots = HashMap<Int, Slot>()

    /** The last published spoil of each chunk near the focus, kept when the overlay ring moves on. */
    private val knownSpoil = HashMap<Int, FloatArray?>()

    /** The overlay object last seen per chunk: its spoil is compared only when a new one is published. */
    private val seenOverlay = HashMap<Int, OverlayChunk>()

    /** Scratch for one chunk's 3 by 3 spoil block; copied only when a request is made. */
    private val spoilScratch = arrayOfNulls<FloatArray>(9)

    /** This frame's grass instance arrays, one per slot (reused). */
    private val grassChunks = ArrayList<FloatArray>()
    private var lastFcx = Int.MIN_VALUE
    private var lastFcy = Int.MIN_VALUE
    private var foodMesh: Mesh? = null
    private var foodsDrawn: List<FoodView>? = null

    fun resize(w: Int, h: Int) {
        camera.viewportWidth = w.toFloat()
        camera.viewportHeight = h.toFloat()
    }

    /** The ground height at (x, y) as drawn: base relief plus the spoil mirrored so far. */
    fun groundHeight(x: Float, y: Float): Float = local.heights.height(x, y)

    /**
     * Draws the sky, ground, specks, rocks, food, grass and ants seen from [chase]. [seconds] is the time into
     * the day cycle in seconds (keep it small, wrapped by the caller, for float precision).
     */
    fun draw(chase: ChaseCamera3, poses: List<AntPose>, foods: List<FoodView>, seconds: Float, animator: AntAnimator) {
        camera.position.set(chase.eyeX, chase.eyeY, chase.eyeZ)
        camera.up.set(chase.upX, chase.upY, chase.upZ)
        camera.lookAt(chase.targetX, chase.targetY, chase.targetZ)
        camera.update()
        DayCycle.sky(DayCycle.timeOfDay(seconds), sky)
        val fcx = floor(chase.targetX / CHUNK_MM).toInt()
        val fcy = floor(chase.targetY / CHUNK_MM).toInt()
        refreshChunks(fcx, fcy)
        uploadFinished()
        if (foods !== foodsDrawn) {
            foodMesh?.dispose()
            foodMesh = upload(local.foods(foods))
            foodsDrawn = foods
        }
        val gl = Gdx.gl
        gl.glClear(GL20.GL_DEPTH_BUFFER_BIT)
        gl.glDisable(GL20.GL_BLEND)
        gl.glDisable(GL20.GL_DEPTH_TEST)
        gl.glDisable(GL20.GL_CULL_FACE)
        skyShader.bind()
        skyShader.setUniformf("u_top", sky.skyTop[0], sky.skyTop[1], sky.skyTop[2])
        skyShader.setUniformf("u_horizon", sky.skyHorizon[0], sky.skyHorizon[1], sky.skyHorizon[2])
        skyQuad.render(skyShader, GL20.GL_TRIANGLES)
        gl.glEnable(GL20.GL_DEPTH_TEST)
        gl.glDepthFunc(GL20.GL_LEQUAL)
        gl.glDepthMask(true)
        gl.glEnable(GL20.GL_CULL_FACE)
        gl.glCullFace(GL20.GL_BACK)
        gl.glFrontFace(GL20.GL_CCW)
        world.bind()
        Shaders.applySky(world, sky, camera)
        for (slot in slots.values) {
            slot.ground?.render(world, GL20.GL_TRIANGLES)
            slot.rocks?.render(world, GL20.GL_TRIANGLES)
        }
        gl.glDisable(GL20.GL_CULL_FACE) // stems and food lumps are thin; draw both sides
        foodMesh?.render(world, GL20.GL_TRIANGLES)
        grassChunks.clear()
        for (slot in slots.values) if (slot.grass.isNotEmpty()) grassChunks += slot.grass
        grass.draw(grassChunks, sky, camera, seconds) // blades are single triangles; culling is still off
        ants.draw(poses, animator, sky, camera)
        gl.glDisable(GL20.GL_DEPTH_TEST)
        gl.glDisable(GL20.GL_CULL_FACE)
        gl.glDisable(GL20.GL_BLEND)
        gl.glDepthMask(true)
    }

    private fun refreshChunks(fcx: Int, fcy: Int) {
        // Mirror the published spoil one chunk beyond the ring, so ring chunks see their neighbours'.
        for (cy in fcy - RING - 1..fcy + RING + 1) for (cx in fcx - RING - 1..fcx + RING + 1) {
            if (cx < 0 || cy < 0 || cx >= CHUNKS || cy >= CHUNKS) continue
            val key = cx + cy * CHUNKS
            val overlay = published.overlays[key] ?: continue
            if (seenOverlay[key] === overlay) continue
            seenOverlay[key] = overlay
            if (knownSpoil.containsKey(key) && sameSpoil(knownSpoil[key], overlay.spoil)) continue
            knownSpoil[key] = overlay.spoil
            local.setSpoil(cx, cy, overlay.spoil)
        }
        val now = System.nanoTime()
        for (cy in fcy - RING..fcy + RING) for (cx in fcx - RING..fcx + RING) {
            if (cx < 0 || cy < 0 || cx >= CHUNKS || cy >= CHUNKS) continue
            val key = cx + cy * CHUNKS
            val slot = slots.getOrPut(key) { Slot() }
            val rocks = rocksFor(slot, cx, cy)
            val old = slot.spoils
            var changed = slot.requested == 0L || old == null || rocks !== slot.rockList
            val spoils = spoilScratch
            for (dy in -1..1) for (dx in -1..1) {
                val nx = cx + dx
                val ny = cy + dy
                val i = (dx + 1) + (dy + 1) * 3
                spoils[i] = if (nx < 0 || ny < 0 || nx >= CHUNKS || ny >= CHUNKS) null else knownSpoil[nx + ny * CHUNKS]
                if (!changed && !sameSpoil(spoils[i], old!![i])) changed = true
            }
            if (changed && (slot.requested == 0L || now - slot.requestedAt >= REBUILD_NANOS)) {
                val copy = spoils.copyOf()
                slot.spoils = copy
                slot.rockList = rocks
                slot.requested = cache.request(cx, cy, copy[4], rocks, copy)
                slot.requestedAt = now
            }
        }
        // Slots and mirrored spoil are added only near the focus, so they go stale only when it moves.
        if (fcx != lastFcx || fcy != lastFcy) {
            lastFcx = fcx
            lastFcy = fcy
            val it = slots.entries.iterator()
            while (it.hasNext()) {
                val e = it.next()
                if (abs(e.key % CHUNKS - fcx) > RING + 1 || abs(e.key / CHUNKS - fcy) > RING + 1) {
                    e.value.ground?.dispose()
                    e.value.rocks?.dispose()
                    it.remove()
                }
            }
            knownSpoil.keys.removeIf { abs(it % CHUNKS - fcx) > RING + 2 || abs(it / CHUNKS - fcy) > RING + 2 }
            seenOverlay.keys.removeIf { abs(it % CHUNKS - fcx) > RING + 2 || abs(it / CHUNKS - fcy) > RING + 2 }
        }
    }

    /**
     * This chunk's published rocks plus placed objects whose centre falls in it. The combined list
     * is cached in [slot] and rebuilt only when the published lists change identity, so an
     * unchanged chunk keeps the same list and is not re-requested.
     */
    private fun rocksFor(slot: Slot, cx: Int, cy: Int): List<Blob> {
        val own = published.rocks[cx + cy * CHUNKS] ?: emptyList()
        val placed = published.placed
        val cached = slot.combined
        if (cached != null && own === slot.ownRocks && placed === slot.placed) return cached
        slot.ownRocks = own
        slot.placed = placed
        val mine = placed.filter { floor(it.cx / CHUNK_MM).toInt() == cx && floor(it.cy / CHUNK_MM).toInt() == cy }
        val combined = if (mine.isEmpty()) own else own + mine
        // Same content as before (placed objects elsewhere changed): keep the old list, so no rebuild.
        val result = if (cached != null && cached == combined) cached else combined
        slot.combined = result
        return result
    }

    private fun uploadFinished() {
        for (r in cache.poll(UPLOADS_PER_FRAME)) {
            val slot = slots[r.cx + r.cy * CHUNKS] ?: continue
            if (r.version < slot.version) continue
            slot.version = r.version
            slot.ground?.dispose()
            slot.rocks?.dispose()
            slot.ground = upload(r.ground)
            slot.rocks = upload(r.rocks)
            slot.grass = r.grass
        }
    }

    private fun upload(d: MeshData): Mesh? {
        if (d.vertexCount == 0) return null
        return Mesh(
            true, d.vertexCount, 0,
            VertexAttribute(VertexAttributes.Usage.Position, 3, "a_position"),
            VertexAttribute(VertexAttributes.Usage.Normal, 3, "a_normal"),
            VertexAttribute(VertexAttributes.Usage.Generic, 3, "a_color"),
        ).apply { setVertices(d.vertices) }
    }

    override fun dispose() {
        cache.close()
        for (s in slots.values) {
            s.ground?.dispose()
            s.rocks?.dispose()
        }
        foodMesh?.dispose()
        ants.dispose()
        grass.dispose()
        world.dispose()
        skyShader.dispose()
        skyQuad.dispose()
    }

    private companion object {
        const val FOV = 60f
        const val RING = 2
        const val UPLOADS_PER_FRAME = 2
        const val REBUILD_NANOS = 3_000_000_000L

        /** Whether two published spoil arrays hold the same values (null is no spoil). */
        fun sameSpoil(a: FloatArray?, b: FloatArray?): Boolean = a === b || (a != null && b != null && a.contentEquals(b))
    }
}

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
import com.bydesigninteractive.ant.core.render.ant.AntBatch
import com.bydesigninteractive.ant.core.render.ant.AntRenderer
import com.bydesigninteractive.ant.core.render.ant.TurnSmoother
import com.bydesigninteractive.ant.core.render.sky.DayCycle
import com.bydesigninteractive.ant.core.render.sky.SkyState
import com.bydesigninteractive.ant.core.render.world.ChunkCache
import com.bydesigninteractive.ant.core.render.world.ChunkMesher
import com.bydesigninteractive.ant.core.render.world.EntranceMesh
import com.bydesigninteractive.ant.core.render.world.GrassField
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
 * at most every [REBUILD_NANOS] per chunk. The inner 3 by 3 chunks get the fine ground
 * ([ChunkMesher.CELLS]) and the outer ring the coarse one ([ChunkMesher.COARSE_CELLS]); a chunk
 * whose ring changes as the focus moves is requested again at once, and its old meshes stay drawn
 * until the new ones arrive. Both levels share the fine border, so they meet without cracks. Rocks
 * are indexed meshes. The sky state comes from the [DayCycle]. Works in
 * simulation coordinates (mm, z up). Render thread only, except the cache's own thread.
 *
 * The nest entrance at ([entranceX], [entranceY]) (immutable, read before the simulation thread
 * starts) is drawn as an [EntranceMesh] dip, through the stencil buffer when the window has one.
 *
 * GL state on return matches what the debug renderer's ModelBatch leaves: depth test, face culling,
 * blending and stencil test off, depth writes on, so the HUD's SpriteBatch draws over it unchanged.
 */
class SurfaceRenderer3D(
    seed: Long,
    private val published: Published,
    private val entranceX: Float,
    private val entranceY: Float,
    skipLayers: Set<String> = emptySet(),
) : Disposable {
    private val drawAnts = "ants" !in skipLayers
    private val drawShadows = "shadows" !in skipLayers
    private val drawRocks = "rocks" !in skipLayers
    private val drawGround = "ground" !in skipLayers
    private val drawGrass = "grass" !in skipLayers
    private val drawFood = "food" !in skipLayers
    private val drawSky = "sky" !in skipLayers

    /** Running vertex totals per layer since the last [vertexSummary], and the frames they cover. */
    private var vGround = 0L
    private var vRocks = 0L
    private var vRocksIdx = 0L
    private var vFood = 0L
    private var vGrass = 0L
    private var vAntsDetailed = 0L
    private var vAntsSimple = 0L
    private var vShadows = 0L
    private var vFrames = 0

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

    /** The last drawn frame's ant instance counts. */
    val antBatch: AntBatch get() = ants.batch

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
            ants = AntRenderer(drawAnts, drawShadows).also { made += it }
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
     * [placed]) it was combined from. [cells] is the ground level last requested (0 before any).
     */
    private class Slot {
        var version = 0L
        var cells = 0
        var ground: Mesh? = null
        var rocks: Array<Mesh> = NO_MESHES
        var grass = FloatArray(0)
        var spoils: Array<FloatArray?>? = null
        var rockList: List<Blob>? = null
        var combined: List<Blob>? = null
        var ownRocks: List<Blob>? = null
        var placed: List<Blob>? = null
        var grassRockList: List<Blob>? = null
        var grassRocks: List<Blob>? = null
        var grassSources: Array<List<Blob>?>? = null
        var requestedAt = 0L
        var requested = 0L
    }

    private val slots = HashMap<Int, Slot>()

    /** The last published spoil of each chunk near the focus, kept when the overlay ring moves on. */
    private val knownSpoil = HashMap<Int, FloatArray?>()

    /** The overlay object last seen per chunk: its spoil is compared only when a new one is published. */
    private val seenOverlay = HashMap<Int, OverlayChunk>()

    /** Scratch for the sources of one chunk's grass rocks (3 by 3 published lists, then the placed list). */
    private val grassScratch = arrayOfNulls<List<Blob>>(10)

    /** Scratch for one chunk's 3 by 3 spoil block; copied only when a request is made. */
    private val spoilScratch = arrayOfNulls<FloatArray>(9)

    /** This frame's grass instance arrays, one per slot (reused). */
    private val grassChunks = ArrayList<FloatArray>()
    private var lastFcx = Int.MIN_VALUE
    private var lastFcy = Int.MIN_VALUE
    private var foodMesh: Mesh? = null
    private var foodsDrawn: List<FoodView>? = null
    private var entranceCone: Mesh? = null
    private var entranceLid: Mesh? = null

    /** Set when mirrored spoil changes, as the ground height at the entrance may have moved. */
    private var entranceDirty = true

    /** Whether the window has a stencil buffer, so the entrance can show its dip below the ground facets. */
    private val stencil = Gdx.graphics.bufferFormat.stencil > 0

    fun resize(w: Int, h: Int) {
        camera.viewportWidth = w.toFloat()
        camera.viewportHeight = h.toFloat()
    }

    /**
     * The per-frame average vertices submitted per layer since the last call, for the log (for example
     * ` verts ground 155k rocks 24k (idx 140k) ...`), then resets the totals. Empty when no frame was
     * drawn. Non-indexed layers count their vertices; the indexed rocks count unique vertices (the
     * vertex shader's work with a perfect post-transform cache) and show their index count in brackets
     * (vertices as a non-indexed mesh would submit them).
     */
    fun vertexSummary(): String {
        val n = vFrames
        if (n == 0) return ""
        val s = " verts ground ${k(vGround / n)} rocks ${k(vRocks / n)} (idx ${k(vRocksIdx / n)}) food ${k(vFood / n)} grass ${k(vGrass / n)} " +
            "ants detailed ${k(vAntsDetailed / n)} simple ${k(vAntsSimple / n)} shadows ${k(vShadows / n)}"
        vGround = 0; vRocks = 0; vRocksIdx = 0; vFood = 0; vGrass = 0; vAntsDetailed = 0; vAntsSimple = 0; vShadows = 0; vFrames = 0
        return s
    }

    private fun k(v: Long): String = if (v < 1000) "$v" else "${v / 1000}k"

    /** The ground height at (x, y) as drawn: base relief plus the spoil mirrored so far. */
    fun groundHeight(x: Float, y: Float): Float = local.heights.height(x, y)

    /**
     * Draws the sky, ground, specks, rocks, the entrance, food, grass and ants seen from [chase]. [seconds] is the time into
     * the day cycle in seconds (keep it small, wrapped by the caller, for float precision).
     */
    fun draw(chase: ChaseCamera3, poses: List<AntPose>, foods: List<FoodView>, seconds: Float, animator: AntAnimator, turns: TurnSmoother) {
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
        if (entranceDirty) {
            entranceDirty = false
            entranceCone?.dispose()
            entranceLid?.dispose()
            entranceCone = upload(EntranceMesh.cone(entranceX, entranceY, local.heights::height))
            entranceLid = upload(EntranceMesh.lid(entranceX, entranceY, local.heights::height))
        }
        val gl = Gdx.gl
        gl.glClear(if (stencil) GL20.GL_DEPTH_BUFFER_BIT or GL20.GL_STENCIL_BUFFER_BIT else GL20.GL_DEPTH_BUFFER_BIT)
        gl.glDisable(GL20.GL_BLEND)
        gl.glDisable(GL20.GL_DEPTH_TEST)
        gl.glDisable(GL20.GL_CULL_FACE)
        if (drawSky) {
            skyShader.bind()
            skyShader.setUniformf("u_top", sky.skyTop[0], sky.skyTop[1], sky.skyTop[2])
            skyShader.setUniformf("u_horizon", sky.skyHorizon[0], sky.skyHorizon[1], sky.skyHorizon[2])
            skyQuad.render(skyShader, GL20.GL_TRIANGLES)
        }
        gl.glEnable(GL20.GL_DEPTH_TEST)
        gl.glDepthFunc(GL20.GL_LEQUAL)
        gl.glDepthMask(true)
        gl.glEnable(GL20.GL_CULL_FACE)
        gl.glCullFace(GL20.GL_BACK)
        gl.glFrontFace(GL20.GL_CCW)
        world.bind()
        Shaders.applySky(world, sky, camera)
        for ((key, slot) in slots) {
            if (abs(key % CHUNKS - fcx) > RING || abs(key / CHUNKS - fcy) > RING) continue // kept only for hysteresis
            if (drawGround) slot.ground?.let { it.render(world, GL20.GL_TRIANGLES); vGround += it.numVertices }
            if (drawRocks) for (m in slot.rocks) {
                m.render(world, GL20.GL_TRIANGLES)
                vRocks += m.numVertices
                vRocksIdx += m.numIndices
            }
        }
        // Beyond the ring there is no ground drawn (or spoil mirrored) to set it in.
        if (drawGround && abs(floor(entranceX / CHUNK_MM).toInt() - fcx) <= RING && abs(floor(entranceY / CHUNK_MM).toInt() - fcy) <= RING) drawEntrance()
        gl.glDisable(GL20.GL_CULL_FACE) // stems and food lumps are thin; draw both sides
        if (drawFood) foodMesh?.let { it.render(world, GL20.GL_TRIANGLES); vFood += it.numVertices }
        if (drawGrass) {
            grassChunks.clear()
            for (slot in slots.values) if (slot.grass.isNotEmpty()) grassChunks += slot.grass
            vGrass += grass.draw(grassChunks, sky, camera, seconds) // blades are single triangles; culling is still off
        }
        if (drawAnts) {
            ants.draw(poses, animator, turns, sky, camera)
            vAntsDetailed += ants.vertsDetailed
            vAntsSimple += ants.vertsSimple
            vShadows += ants.vertsShadows
        }
        vFrames++
        gl.glDisable(GL20.GL_DEPTH_TEST)
        gl.glDisable(GL20.GL_CULL_FACE)
        gl.glDisable(GL20.GL_BLEND)
        gl.glDepthMask(true)
    }

    /**
     * Draws the entrance with the world shader bound and back faces culled. With a stencil buffer,
     * the lid marks where the opening is visible (depth tested against the ground, no colour or
     * depth written), then the cone is drawn there regardless of depth, which shows the dip under
     * the ground's facets; culling leaves one interior face per pixel. Without one, the cone is
     * drawn plainly and only its part above the facets shows. Leaves culling on and depth as found.
     */
    private fun drawEntrance() {
        val cone = entranceCone ?: return
        vGround += cone.numVertices
        val lid = entranceLid
        val gl = Gdx.gl
        if (!stencil || lid == null) {
            cone.render(world, GL20.GL_TRIANGLES)
            return
        }
        gl.glEnable(GL20.GL_STENCIL_TEST)
        gl.glStencilMask(0xFF)
        gl.glStencilFunc(GL20.GL_ALWAYS, 1, 0xFF)
        gl.glStencilOp(GL20.GL_KEEP, GL20.GL_KEEP, GL20.GL_REPLACE)
        gl.glColorMask(false, false, false, false)
        gl.glDepthMask(false)
        gl.glDisable(GL20.GL_CULL_FACE)
        lid.render(world, GL20.GL_TRIANGLES)
        gl.glColorMask(true, true, true, true)
        gl.glEnable(GL20.GL_CULL_FACE)
        gl.glStencilFunc(GL20.GL_EQUAL, 1, 0xFF)
        gl.glStencilOp(GL20.GL_KEEP, GL20.GL_KEEP, GL20.GL_KEEP)
        gl.glDepthFunc(GL20.GL_ALWAYS)
        cone.render(world, GL20.GL_TRIANGLES)
        gl.glDepthFunc(GL20.GL_LEQUAL)
        gl.glDepthMask(true)
        gl.glDisable(GL20.GL_STENCIL_TEST)
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
            entranceDirty = true
        }
        val now = System.nanoTime()
        for (cy in fcy - RING..fcy + RING) for (cx in fcx - RING..fcx + RING) {
            if (cx < 0 || cy < 0 || cx >= CHUNKS || cy >= CHUNKS) continue
            val key = cx + cy * CHUNKS
            val slot = slots.getOrPut(key) { Slot() }
            val rocks = rocksFor(slot, cx, cy)
            val grassRocks = grassRocksFor(slot, cx, cy, rocks)
            val old = slot.spoils
            val cells = if (abs(cx - fcx) <= 1 && abs(cy - fcy) <= 1) ChunkMesher.CELLS else ChunkMesher.COARSE_CELLS
            val relevel = cells != slot.cells
            var changed = slot.requested == 0L || old == null || relevel || rocks !== slot.rockList || grassRocks !== slot.grassRockList
            val spoils = spoilScratch
            for (dy in -1..1) for (dx in -1..1) {
                val nx = cx + dx
                val ny = cy + dy
                val i = (dx + 1) + (dy + 1) * 3
                spoils[i] = if (nx < 0 || ny < 0 || nx >= CHUNKS || ny >= CHUNKS) null else knownSpoil[nx + ny * CHUNKS]
                if (!changed && !sameSpoil(spoils[i], old!![i])) changed = true
            }
            // A new level is requested at once; the old meshes stay drawn until the new ones arrive.
            if (changed && (slot.requested == 0L || relevel || now - slot.requestedAt >= REBUILD_NANOS)) {
                val copy = spoils.copyOf()
                slot.spoils = copy
                slot.rockList = rocks
                slot.grassRockList = grassRocks
                slot.cells = cells
                slot.requested = cache.request(cx, cy, copy[4], rocks, copy, grassRocks, cells)
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
                    for (m in e.value.rocks) m.dispose()
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

    /**
     * The rocks grass in chunk (cx, cy) must avoid: [own] (the chunk's combined list) plus the
     * neighbours' published rocks and the placed objects whose footprint, widened by
     * [GrassField.ROCK_CLEARANCE], reaches into the chunk. Cached in [slot] by the identity of its
     * sources, and kept as the same list when the content is unchanged, so no needless rebuild.
     */
    private fun grassRocksFor(slot: Slot, cx: Int, cy: Int, own: List<Blob>): List<Blob> {
        val sources = grassScratch
        java.util.Arrays.fill(sources, null)
        for (dy in -1..1) for (dx in -1..1) {
            val nx = cx + dx
            val ny = cy + dy
            if ((dx == 0 && dy == 0) || nx < 0 || ny < 0 || nx >= CHUNKS || ny >= CHUNKS) continue
            sources[(dx + 1) + (dy + 1) * 3] = published.rocks[nx + ny * CHUNKS]
        }
        sources[4] = own
        sources[9] = published.placed
        val cached = slot.grassRocks
        val old = slot.grassSources
        if (cached != null && old != null) {
            var same = true
            for (i in sources.indices) if (old[i] !== sources[i]) same = false
            if (same) return cached
        }
        slot.grassSources = sources.copyOf()
        val pad = GrassField.ROCK_CLEARANCE
        val x0 = cx * CHUNK_MM - pad
        val y0 = cy * CHUNK_MM - pad
        val x1 = (cx + 1) * CHUNK_MM + pad
        val y1 = (cy + 1) * CHUNK_MM + pad
        fun reaches(b: Blob) = b.cx + b.reach >= x0 && b.cx - b.reach <= x1 && b.cy + b.reach >= y0 && b.cy - b.reach <= y1
        val out = ArrayList<Blob>(own)
        for (i in 0 until 9) if (i != 4) sources[i]?.forEach { if (reaches(it)) out += it }
        for (b in published.placed) if (b !in own && reaches(b)) out += b
        val result = if (cached != null && cached == out) cached else if (out.size == own.size) own else out
        slot.grassRocks = result
        return result
    }

    private fun uploadFinished() {
        for (r in cache.poll(UPLOADS_PER_FRAME)) {
            val slot = slots[r.cx + r.cy * CHUNKS] ?: continue
            if (r.version < slot.version) continue
            slot.version = r.version
            slot.ground?.dispose()
            for (m in slot.rocks) m.dispose()
            slot.ground = upload(r.ground)
            slot.rocks = if (r.rocks.isEmpty()) NO_MESHES else r.rocks.mapNotNull { upload(it) }.toTypedArray()
            slot.grass = r.grass
        }
    }

    /** A static mesh of [d], indexed (16-bit) when [d] has indices; null when it has no vertices. */
    private fun upload(d: MeshData): Mesh? {
        if (d.vertexCount == 0) return null
        val indices = d.indices
        return Mesh(
            true, d.vertexCount, indices?.size ?: 0,
            VertexAttribute(VertexAttributes.Usage.Position, 3, "a_position"),
            VertexAttribute(VertexAttributes.Usage.Normal, 3, "a_normal"),
            VertexAttribute(VertexAttributes.Usage.Generic, 3, "a_color"),
        ).apply {
            setVertices(d.vertices)
            if (indices != null) setIndices(indices)
        }
    }

    override fun dispose() {
        cache.close()
        for (s in slots.values) {
            s.ground?.dispose()
            for (m in s.rocks) m.dispose()
        }
        foodMesh?.dispose()
        entranceCone?.dispose()
        entranceLid?.dispose()
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
        val NO_MESHES = emptyArray<Mesh>()

        /** Whether two published spoil arrays hold the same values (null is no spoil). */
        fun sameSpoil(a: FloatArray?, b: FloatArray?): Boolean = a === b || (a != null && b != null && a.contentEquals(b))
    }
}

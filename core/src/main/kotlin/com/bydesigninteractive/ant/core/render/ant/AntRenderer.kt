package com.bydesigninteractive.ant.core.render.ant

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.Mesh
import com.badlogic.gdx.graphics.VertexAttribute
import com.badlogic.gdx.graphics.VertexAttributes
import com.badlogic.gdx.graphics.glutils.ShaderProgram
import com.badlogic.gdx.utils.Disposable
import com.bydesigninteractive.ant.core.engine.AntPose
import com.bydesigninteractive.ant.core.render.AntAnimator
import com.bydesigninteractive.ant.core.render.Shaders
import com.bydesigninteractive.ant.core.render.sky.DayCycle
import com.bydesigninteractive.ant.core.render.sky.SkyState
import com.bydesigninteractive.ant.sim.ant.Space
import com.bydesigninteractive.ant.sim.util.unit
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Packs surface ants into instance records of [FLOATS] floats: position and body bob; the unit
 * forward (the [TurnSmoother]'s drawn forward) and gaster fill (crop); the unit up and carry code;
 * the unit left (up cross forward) and the model scale; and the gait values from [gait]. The
 * gait trigonometry and the frame vectors are worked out here once per ant, so the vertex shader
 * does none of it per vertex. Nest ants are skipped, and so are ants [ANT_DRAW_MM] or more from the eye and ants whose
 * bounding sphere lies outside the view; ants shrink to nothing over the last [FADE_MM] before
 * [ANT_DRAW_MM], so none pops. At most [MAX_DETAILED] ants, the nearest within [NEAR_MM], get the
 * detailed model; every other drawn ant gets the simple one. Pure, so it unit-tests on the JVM.
 */
object AntInstances {
    const val FLOATS = 19
    const val NEAR_MM = 250f
    const val MAX_DETAILED = 80

    /** Record offsets: body bob, crop, carry code, scale and the first gait value. */
    const val BOB = 3
    const val CROP = 7
    const val CARRY = 11
    const val SCALE = 15
    const val GAIT = 16

    /** Leg swing amplitude (rad), foot lift and body bob (mm at model scale). */
    const val SWING_RAD = 0.35f
    const val LIFT_MM = 0.25f
    const val BOB_MM = 0.05f
    private const val TAU = 6.2831853f

    /** Ants this far from the eye or farther are not drawn. */
    const val ANT_DRAW_MM = 1200f

    /** The band before [ANT_DRAW_MM] over which ants shrink to nothing. */
    const val FADE_MM = 200f

    /** Only ants nearer than this cast a shadow. */
    const val SHADOW_MM = 400f

    /** The bounding sphere's radius at model scale (mm): the model reaches 3.55 mm from its origin (an antenna tip). */
    const val BOUND_MM = 4f

    /** The model's body length (mm), from the gaster tip (x = -2.85) to the front of the head (x = 2.6). */
    const val MODEL_LENGTH_MM = 5.45f
    const val MIN_LENGTH_MM = 3f
    const val MAX_LENGTH_MM = 5f
    private const val SIZE_SALT = 0x512EL

    /** Ant [id]'s drawn body length (mm), fixed per id: uniform from [MIN_LENGTH_MM] to [MAX_LENGTH_MM] by a hash. */
    fun bodyLength(id: Int): Float = MIN_LENGTH_MM + (MAX_LENGTH_MM - MIN_LENGTH_MM) * unit(SIZE_SALT, id, 0).toFloat()

    /** The uniform model scale that draws ant [id] at its [bodyLength]. */
    fun scale(id: Int): Float = bodyLength(id) / MODEL_LENGTH_MM

    /** The size factor for an ant at squared distance [d2] from the eye: 1 nearer than the fade band, falling to 0 at [ANT_DRAW_MM]. */
    fun fade(d2: Float): Float = ((ANT_DRAW_MM - sqrt(d2)) / FADE_MM).coerceIn(0f, 1f)

    /**
     * Writes the gait for [phase] (0 to 1 per stride) into [dst]: the body bob at [o], and from
     * [gaitAt] the swing's cosine, the swing's sine for a left leg of tripod A, and the lift factor
     * for tripod A. Tripod B runs half a cycle later, so its sine of the stride angle and its
     * cosine are the negations of A's: the swing's cosine is the same for both groups and both sides
     * (cosine is even), and the shader gets a leg's swing sine as `dst[gaitAt + 1] * group * side` and
     * its lift as `max(0, group * dst[gaitAt + 2])`, with group 1 for A and -1 for B and side the sign of the
     * hip's y. This matches the old per-vertex formulas exactly:
     * swing = -[SWING_RAD] sin(TAU (phase + g)) side, lift = [LIFT_MM] max(0, cos(TAU (phase + g))),
     * bob = [BOB_MM] sin(2 TAU phase), with g = 0 for A and 0.5 for B.
     */
    fun gait(phase: Float, dst: FloatArray, o: Int, gaitAt: Int) {
        val sA = sin(TAU * phase)
        val cA = cos(TAU * phase)
        val swing = -SWING_RAD * sA
        dst[o] = BOB_MM * 2f * sA * cA
        dst[gaitAt] = cos(swing)
        dst[gaitAt + 1] = sin(swing)
        dst[gaitAt + 2] = LIFT_MM * cA
    }

    /**
     * Fills [out] from [poses] as seen from [camera], whose position and frustum must be up to
     * date. Two passes: the first culls and keeps the squared distances of the [MAX_DETAILED]
     * nearest ants within [NEAR_MM] in a fixed-size max-heap, whose top is then the detail
     * threshold; the second writes the records. Allocates nothing unless [out] has to grow.
     */
    fun fill(poses: List<AntPose>, animator: AntAnimator, turns: TurnSmoother, camera: Camera, out: AntBatch) {
        val n = poses.size
        out.ensure(n)
        val eye = camera.position
        val frustum = camera.frustum
        val dist = out.dist
        val heap = out.heap
        var heapN = 0
        var culledFar = 0
        var culledView = 0
        val draw2 = ANT_DRAW_MM * ANT_DRAW_MM
        val near2 = NEAR_MM * NEAR_MM
        for (i in 0 until n) {
            dist[i] = -1f
            val p = poses[i]
            if (p.space != Space.SURFACE) continue
            val dx = p.x - eye.x
            val dy = p.y - eye.y
            val dz = p.z - eye.z
            val d2 = dx * dx + dy * dy + dz * dz
            if (d2 >= draw2) {
                culledFar++
                continue
            }
            if (!frustum.sphereInFrustum(p.x, p.y, p.z, BOUND_MM * scale(p.id))) {
                culledView++
                continue
            }
            dist[i] = d2
            if (d2 < near2) heapN = offer(heap, heapN, d2)
        }
        // With the heap full, its top is the largest distance among the nearest MAX_DETAILED.
        val detailMax = if (heapN == MAX_DETAILED) heap[0] else Float.MAX_VALUE
        val shadow2 = SHADOW_MM * SHADOW_MM
        val records = out.records
        val mid = out.mid
        val tail = out.tail
        var nd = 0
        var nm = 0
        var nt = 0
        for (i in 0 until n) {
            val d2 = dist[i]
            if (d2 < 0f) continue
            val p = poses[i]
            val o: Int
            val dst: FloatArray
            if (d2 < near2 && d2 <= detailMax && nd < MAX_DETAILED) {
                dst = records; o = nd * FLOATS; nd++
            } else if (d2 < shadow2) {
                dst = mid; o = nm * FLOATS; nm++
            } else {
                dst = tail; o = nt * FLOATS; nt++
            }
            var fx = turns.fx(p.id, p.fx)
            var fy = turns.fy(p.id, p.fy)
            var fz = turns.fz(p.id, p.fz)
            var k = 1f / sqrt(fx * fx + fy * fy + fz * fz).coerceAtLeast(1e-9f)
            fx *= k; fy *= k; fz *= k
            var ux = p.nx
            var uy = p.ny
            var uz = p.nz
            k = 1f / sqrt(ux * ux + uy * uy + uz * uz).coerceAtLeast(1e-9f)
            ux *= k; uy *= k; uz *= k
            var lx = uy * fz - uz * fy
            var ly = uz * fx - ux * fz
            var lz = ux * fy - uy * fx
            k = 1f / sqrt(lx * lx + ly * ly + lz * lz).coerceAtLeast(1e-9f)
            lx *= k; ly *= k; lz *= k
            dst[o] = p.x; dst[o + 1] = p.y; dst[o + 2] = p.z
            dst[o + 4] = fx; dst[o + 5] = fy; dst[o + 6] = fz; dst[o + CROP] = p.crop
            dst[o + 8] = ux; dst[o + 9] = uy; dst[o + 10] = uz; dst[o + CARRY] = p.carry.toFloat()
            dst[o + 12] = lx; dst[o + 13] = ly; dst[o + 14] = lz; dst[o + SCALE] = scale(p.id) * fade(d2)
            gait(animator.phase(p.id), dst, o + BOB, o + GAIT)
        }
        System.arraycopy(mid, 0, records, nd * FLOATS, nm * FLOATS)
        System.arraycopy(tail, 0, records, (nd + nm) * FLOATS, nt * FLOATS)
        out.detailed = nd
        out.shadowedSimple = nm
        out.plainSimple = nt
        out.culledFar = culledFar
        out.culledView = culledView
    }

    /** Offers [d2] to the max-heap [heap] of [size] entries, capped at [MAX_DETAILED]; returns the new size. */
    private fun offer(heap: FloatArray, size: Int, d2: Float): Int {
        if (size < MAX_DETAILED) {
            var k = size
            heap[k] = d2
            while (k > 0) {
                val parent = (k - 1) / 2
                if (heap[parent] >= heap[k]) break
                val t = heap[parent]; heap[parent] = heap[k]; heap[k] = t
                k = parent
            }
            return size + 1
        }
        if (d2 >= heap[0]) return size
        heap[0] = d2
        var k = 0
        while (true) {
            val l = 2 * k + 1
            if (l >= size) break
            val r = l + 1
            val c = if (r < size && heap[r] > heap[l]) r else l
            if (heap[k] >= heap[c]) break
            val t = heap[c]; heap[c] = heap[k]; heap[k] = t
            k = c
        }
        return size
    }
}

/**
 * One frame's ant instance records and counts, filled by [AntInstances.fill]. [records] holds the
 * [detailed] ants first, then the [shadowedSimple] simple-model ants nearer than
 * [AntInstances.SHADOW_MM], then the [plainSimple] ones beyond, so the shadows, the detailed model
 * and the simple model each draw one contiguous run. Grows only when more poses arrive than it holds.
 */
class AntBatch(capacity: Int = 2048) {
    var capacity = capacity
        private set
    var records = FloatArray(capacity * AntInstances.FLOATS)
        private set
    internal var mid = FloatArray(capacity * AntInstances.FLOATS)
    internal var tail = FloatArray(capacity * AntInstances.FLOATS)
    internal var dist = FloatArray(capacity)
    internal val heap = FloatArray(AntInstances.MAX_DETAILED)
    var detailed = 0
    var shadowedSimple = 0
    var plainSimple = 0

    /** Surface ants skipped for being [AntInstances.ANT_DRAW_MM] or more from the eye. */
    var culledFar = 0

    /** Surface ants within range skipped for lying outside the view. */
    var culledView = 0

    val simple: Int get() = shadowedSimple + plainSimple
    val shadowed: Int get() = detailed + shadowedSimple

    /** Grows the arrays to hold at least [n] ants (to twice [n], so growth is rare). */
    fun ensure(n: Int) {
        if (n <= capacity) return
        capacity = n * 2
        records = FloatArray(capacity * AntInstances.FLOATS)
        mid = FloatArray(capacity * AntInstances.FLOATS)
        tail = FloatArray(capacity * AntInstances.FLOATS)
        dist = FloatArray(capacity)
    }
}

/**
 * Draws the surface ants [AntInstances] selects with GL 3 instancing: soft shadows first (blended,
 * no depth write) under ants nearer than [AntInstances.SHADOW_MM], then the detailed model for the
 * nearest ants and the simple one for the rest.
 * The vertex shader poses the legs, swells the gaster, shows the carried piece and scales each ant
 * to its own size. Render thread only.
 *
 * GL state on return: blending and face culling off, depth writes on, depth test unchanged.
 */
class AntRenderer(private val drawAnts: Boolean = true, private val drawShadows: Boolean = true) : Disposable {
    private val shader: ShaderProgram
    private val shadowShader: ShaderProgram
    private var capacity = 2048

    /** The last frame's records and counts (the counts are logged). */
    val batch = AntBatch(capacity)

    /** Vertices submitted by the last [draw]: detailed ants, simple ants and shadow discs (instances times mesh vertices). */
    var vertsDetailed = 0
        private set
    var vertsSimple = 0
        private set
    var vertsShadows = 0
        private set
    private val detailed: Mesh
    private val simple: Mesh
    private val disc: Mesh

    init {
        val made = ArrayList<Disposable>()
        try {
            shader = Shaders.ants().also { made += it }
            shadowShader = Shaders.shadows().also { made += it }
            detailed = model(AntMesh.build(true)).also { made += it }
            simple = model(AntMesh.build(false)).also { made += it }
            disc = discMesh().also { made += it }
        } catch (e: Exception) {
            for (d in made.asReversed()) d.dispose()
            throw e
        }
    }

    fun draw(poses: List<AntPose>, animator: AntAnimator, turns: TurnSmoother, sky: SkyState, camera: Camera) {
        vertsDetailed = 0
        vertsSimple = 0
        vertsShadows = 0
        if (!drawAnts) return
        AntInstances.fill(poses, animator, turns, camera, batch)
        if (batch.capacity > capacity) grow(batch.capacity)
        val records = batch.records
        if (batch.detailed + batch.simple == 0) return
        val gl = Gdx.gl
        val strength = shadowStrength(sky)
        gl.glDisable(GL20.GL_CULL_FACE)
        gl.glEnable(GL20.GL_BLEND)
        gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA)
        gl.glDepthMask(false)
        if (drawShadows) {
            shadowShader.bind()
            shadowShader.setUniformMatrix("u_projView", camera.combined)
            shadowShader.setUniformf("u_sunDir", sky.sunDir[0], sky.sunDir[1], sky.sunDir[2])
            shadowShader.setUniformf("u_strength", strength)
            drawInstances(disc, records, 0, batch.shadowed, shadowShader)
            vertsShadows = batch.shadowed * disc.numVertices
        }
        gl.glDepthMask(true)
        gl.glDisable(GL20.GL_BLEND)
        // The model is closed and wound counter-clockwise from outside, and (forward, left, up) is right-handed.
        gl.glEnable(GL20.GL_CULL_FACE)
        gl.glCullFace(GL20.GL_BACK)
        shader.bind()
        Shaders.applySky(shader, sky, camera)
        drawInstances(detailed, records, 0, batch.detailed, shader)
        drawInstances(simple, records, batch.detailed, batch.simple, shader)
        vertsDetailed = batch.detailed * detailed.numVertices
        vertsSimple = batch.simple * simple.numVertices
        gl.glDisable(GL20.GL_CULL_FACE)
    }

    /** Recreates the meshes' instance buffers at [newCapacity] ants. */
    private fun grow(newCapacity: Int) {
        capacity = newCapacity
        for (m in arrayOf(detailed, simple, disc)) {
            m.disableInstancedRendering()
            m.enableInstancedRendering(false, capacity, *instanceAttributes())
        }
    }

    /** Draws [count] instances of [mesh] from the records of [data] starting at record [first]. */
    private fun drawInstances(mesh: Mesh, data: FloatArray, first: Int, count: Int, program: ShaderProgram) {
        if (count == 0) return
        mesh.setInstanceData(data, first * AntInstances.FLOATS, count * AntInstances.FLOATS)
        mesh.render(program, GL20.GL_TRIANGLES)
    }

    private fun instanceAttributes() = arrayOf(
        VertexAttribute(VertexAttributes.Usage.Generic, 4, "i_pos"),
        VertexAttribute(VertexAttributes.Usage.Generic, 4, "i_fwd"),
        VertexAttribute(VertexAttributes.Usage.Generic, 4, "i_up"),
        VertexAttribute(VertexAttributes.Usage.Generic, 4, "i_left"),
        VertexAttribute(VertexAttributes.Usage.Generic, 3, "i_gait"),
    )

    private fun model(v: FloatArray): Mesh {
        val m = Mesh(
            true, v.size / AntMesh.STRIDE, 0,
            VertexAttribute(VertexAttributes.Usage.Position, 3, "a_position"),
            VertexAttribute(VertexAttributes.Usage.Normal, 3, "a_normal"),
            VertexAttribute(VertexAttributes.Usage.Generic, 3, "a_color"),
            VertexAttribute(VertexAttributes.Usage.Generic, 1, "a_part"),
            VertexAttribute(VertexAttributes.Usage.Generic, 3, "a_pivot"),
            VertexAttribute(VertexAttributes.Usage.Generic, 1, "a_group"),
        )
        try {
            m.setVertices(v)
            m.enableInstancedRendering(false, capacity, *instanceAttributes())
        } catch (e: Exception) {
            m.dispose()
            throw e
        }
        return m
    }

    /** A soft elliptic disc, 5.2 by 2.8 mm at model scale (the shader scales it per ant): opaque centre fading to nothing at the rim. */
    private fun discMesh(): Mesh {
        val seg = 12
        val v = FloatArray(seg * 3 * 4)
        var o = 0
        for (k in 0 until seg) {
            val a0 = k * 6.2832f / seg
            val a1 = (k + 1) * 6.2832f / seg
            v[o++] = 0f; v[o++] = 0f; v[o++] = 0f; v[o++] = 1f
            v[o++] = cos(a0) * 2.6f; v[o++] = sin(a0) * 1.4f; v[o++] = 0f; v[o++] = 0f
            v[o++] = cos(a1) * 2.6f; v[o++] = sin(a1) * 1.4f; v[o++] = 0f; v[o++] = 0f
        }
        val m = Mesh(
            true, seg * 3, 0,
            VertexAttribute(VertexAttributes.Usage.Position, 3, "a_position"),
            VertexAttribute(VertexAttributes.Usage.Generic, 1, "a_alpha"),
        )
        try {
            m.setVertices(v)
            m.enableInstancedRendering(false, capacity, *instanceAttributes())
        } catch (e: Exception) {
            m.dispose()
            throw e
        }
        return m
    }

    companion object {
        /** Shadow opacity at midday. */
        const val SHADOW_MAX = 0.45f

        /** Direct light strength (luminance times how much it points down) at the midday keyframe. */
        private val NOON_DIRECT: Float = direct(DayCycle.sky(0.5f, SkyState()))

        private fun direct(sky: SkyState): Float =
            (0.2126f * sky.sunColor[0] + 0.7152f * sky.sunColor[1] + 0.0722f * sky.sunColor[2]) * max(0f, -sky.sunDir[2])

        /** Shadow opacity for [sky]: [SHADOW_MAX] at midday, fading with the direct light's strength. Pure. */
        fun shadowStrength(sky: SkyState): Float = (SHADOW_MAX * direct(sky) / NOON_DIRECT).coerceIn(0f, SHADOW_MAX)
    }

    override fun dispose() {
        detailed.dispose()
        simple.dispose()
        disc.dispose()
        shader.dispose()
        shadowShader.dispose()
    }
}

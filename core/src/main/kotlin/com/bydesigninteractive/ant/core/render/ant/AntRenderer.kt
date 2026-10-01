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
import com.bydesigninteractive.ant.core.render.sky.SkyState
import com.bydesigninteractive.ant.sim.ant.Space
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

/**
 * Packs surface ants into instance records of [FLOATS] floats: position and gait phase, forward
 * and gaster fill (crop), up and carry code. Ants within [NEAR_MM] of the eye go to the near
 * array, the rest to the far one; nest ants are skipped. Pure, so it unit-tests on the JVM.
 */
object AntInstances {
    const val FLOATS = 12
    const val NEAR_MM = 500f

    /** Fills [near] and [far] from [poses]; [counts] receives the near (0) and far (1) record counts. Records past an array's end are dropped. */
    fun fill(poses: List<AntPose>, animator: AntAnimator, eyeX: Float, eyeY: Float, eyeZ: Float, near: FloatArray, far: FloatArray, counts: IntArray) {
        counts[0] = 0
        counts[1] = 0
        for (i in poses.indices) {
            val p = poses[i]
            if (p.space != Space.SURFACE) continue
            val dx = p.x - eyeX
            val dy = p.y - eyeY
            val dz = p.z - eyeZ
            val isNear = dx * dx + dy * dy + dz * dz < NEAR_MM * NEAR_MM
            val out = if (isNear) near else far
            val k = if (isNear) 0 else 1
            val o = counts[k] * FLOATS
            if (o + FLOATS > out.size) continue
            out[o] = p.x; out[o + 1] = p.y; out[o + 2] = p.z; out[o + 3] = animator.phase(p.id)
            out[o + 4] = p.fx; out[o + 5] = p.fy; out[o + 6] = p.fz; out[o + 7] = p.crop
            out[o + 8] = p.nx; out[o + 9] = p.ny; out[o + 10] = p.nz; out[o + 11] = p.carry.toFloat()
            counts[k]++
        }
    }
}

/**
 * Draws all surface ants with GL 3 instancing: soft shadows first (blended, no depth write), then
 * the detailed model for ants within [AntInstances.NEAR_MM] of the eye and the simple one beyond.
 * The vertex shader poses the legs, swells the gaster and shows the carried piece. Render thread only.
 *
 * GL state on return: blending and face culling off, depth writes on, depth test unchanged.
 */
class AntRenderer : Disposable {
    private val shader: ShaderProgram
    private val shadowShader: ShaderProgram
    private var capacity = 2048
    private var near = FloatArray(capacity * AntInstances.FLOATS)
    private var far = FloatArray(capacity * AntInstances.FLOATS)
    private val counts = IntArray(2)
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

    fun draw(poses: List<AntPose>, animator: AntAnimator, sky: SkyState, camera: Camera) {
        if (poses.size > capacity) grow(poses.size * 2)
        AntInstances.fill(poses, animator, camera.position.x, camera.position.y, camera.position.z, near, far, counts)
        if (counts[0] + counts[1] == 0) return
        val gl = Gdx.gl
        val strength = 0.35f * max(0f, -sky.sunDir[2]).coerceAtLeast(0.3f)
        gl.glDisable(GL20.GL_CULL_FACE)
        gl.glEnable(GL20.GL_BLEND)
        gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA)
        gl.glDepthMask(false)
        shadowShader.bind()
        shadowShader.setUniformMatrix("u_projView", camera.combined)
        shadowShader.setUniformf("u_sunDir", sky.sunDir[0], sky.sunDir[1], sky.sunDir[2])
        shadowShader.setUniformf("u_strength", strength)
        drawInstances(disc, near, counts[0], shadowShader)
        drawInstances(disc, far, counts[1], shadowShader)
        gl.glDepthMask(true)
        gl.glDisable(GL20.GL_BLEND)
        // The model is closed and wound counter-clockwise from outside, and (forward, left, up) is right-handed.
        gl.glEnable(GL20.GL_CULL_FACE)
        gl.glCullFace(GL20.GL_BACK)
        shader.bind()
        Shaders.applySky(shader, sky, camera)
        drawInstances(detailed, near, counts[0], shader)
        drawInstances(simple, far, counts[1], shader)
        gl.glDisable(GL20.GL_CULL_FACE)
    }

    /** Grows the record arrays and recreates the meshes' instance buffers at [newCapacity] ants. */
    private fun grow(newCapacity: Int) {
        capacity = newCapacity
        near = FloatArray(capacity * AntInstances.FLOATS)
        far = FloatArray(capacity * AntInstances.FLOATS)
        for (m in arrayOf(detailed, simple, disc)) {
            m.disableInstancedRendering()
            m.enableInstancedRendering(false, capacity, *instanceAttributes())
        }
    }

    private fun drawInstances(mesh: Mesh, data: FloatArray, count: Int, program: ShaderProgram) {
        if (count == 0) return
        mesh.setInstanceData(data, 0, count * AntInstances.FLOATS)
        mesh.render(program, GL20.GL_TRIANGLES)
    }

    private fun instanceAttributes() = arrayOf(
        VertexAttribute(VertexAttributes.Usage.Generic, 4, "i_pos"),
        VertexAttribute(VertexAttributes.Usage.Generic, 4, "i_fwd"),
        VertexAttribute(VertexAttributes.Usage.Generic, 4, "i_up"),
    )

    private fun model(v: FloatArray): Mesh {
        val m = Mesh(
            true, v.size / AntMesh.STRIDE, 0,
            VertexAttribute(VertexAttributes.Usage.Position, 3, "a_position"),
            VertexAttribute(VertexAttributes.Usage.Normal, 3, "a_normal"),
            VertexAttribute(VertexAttributes.Usage.Generic, 3, "a_color"),
            VertexAttribute(VertexAttributes.Usage.Generic, 1, "a_part"),
            VertexAttribute(VertexAttributes.Usage.Generic, 3, "a_pivot"),
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

    /** A soft elliptic disc, 5.2 by 2.8 mm: opaque centre fading to nothing at the rim. */
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

    override fun dispose() {
        detailed.dispose()
        simple.dispose()
        disc.dispose()
        shader.dispose()
        shadowShader.dispose()
    }
}

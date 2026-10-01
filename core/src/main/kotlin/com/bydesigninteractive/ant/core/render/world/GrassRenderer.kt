package com.bydesigninteractive.ant.core.render.world

import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.Mesh
import com.badlogic.gdx.graphics.VertexAttribute
import com.badlogic.gdx.graphics.VertexAttributes
import com.badlogic.gdx.graphics.glutils.ShaderProgram
import com.badlogic.gdx.utils.Disposable
import com.bydesigninteractive.ant.core.render.Shaders
import com.bydesigninteractive.ant.core.render.sky.SkyState

/**
 * Draws every visible tuft in one instanced call: one base tuft from [GrassField.tuftMesh], with
 * per-instance place and style. Blades are single triangles, so the caller draws with face
 * culling off. Tufts beyond [CAPACITY] are left out. Render thread only.
 */
class GrassRenderer(seed: Long) : Disposable {
    private val shader: ShaderProgram = Shaders.grass()
    private val data = FloatArray(CAPACITY * GrassField.INSTANCE_FLOATS)
    private val tuft: Mesh

    init {
        var m: Mesh? = null
        try {
            val v = GrassField(seed).tuftMesh()
            m = Mesh(
                true, v.size / GrassField.TUFT_STRIDE, 0,
                VertexAttribute(VertexAttributes.Usage.Position, 3, "a_position"),
                VertexAttribute(VertexAttributes.Usage.Normal, 3, "a_normal"),
                VertexAttribute(VertexAttributes.Usage.Generic, 1, "a_blade"),
                VertexAttribute(VertexAttributes.Usage.Generic, 1, "a_tip"),
            )
            m.setVertices(v)
            m.enableInstancedRendering(
                false, CAPACITY,
                VertexAttribute(VertexAttributes.Usage.Generic, 4, "i_place"),
                VertexAttribute(VertexAttributes.Usage.Generic, 4, "i_style"),
            )
            tuft = m
        } catch (e: Exception) {
            m?.dispose()
            shader.dispose()
            throw e
        }
    }

    /** Draws the tufts of [chunks] (instance arrays from [GrassField.tufts]); [time] drives the sway, in seconds. */
    fun draw(chunks: Collection<FloatArray>, sky: SkyState, camera: Camera, time: Float) {
        var n = 0
        for (c in chunks) {
            if (n + c.size > data.size) break
            c.copyInto(data, n)
            n += c.size
        }
        if (n == 0) return
        shader.bind()
        Shaders.applySky(shader, sky, camera)
        shader.setUniformf("u_time", time)
        tuft.setInstanceData(data, 0, n)
        tuft.render(shader, GL20.GL_TRIANGLES)
    }

    override fun dispose() {
        tuft.dispose()
        shader.dispose()
    }

    private companion object {
        /** Up to 64 tufts per chunk in a 5 by 5 ring is 1,600; this leaves room. */
        const val CAPACITY = 8192
    }
}

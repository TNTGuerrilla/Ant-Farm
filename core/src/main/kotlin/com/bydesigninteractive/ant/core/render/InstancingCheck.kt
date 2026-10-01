package com.bydesigninteractive.ant.core.render

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.Mesh
import com.badlogic.gdx.graphics.VertexAttribute
import com.badlogic.gdx.graphics.VertexAttributes
import com.badlogic.gdx.graphics.glutils.ShaderProgram
import com.bydesigninteractive.ant.core.APP_LOG_TAG

/**
 * Draws two instances of one triangle off-screen-sized (into the current target) with the
 * instancing path the ant, shadow and grass renderers use, and reports whether GL accepted it.
 * Run once at start; a false result means the 3D view falls back to the debug renderer.
 */
object InstancingCheck {
    fun run(): Boolean {
        if (Gdx.gl30 == null) return false
        var mesh: Mesh? = null
        var shader: ShaderProgram? = null
        return try {
            val m = Mesh(true, 3, 0, VertexAttribute(VertexAttributes.Usage.Position, 3, "a_position"))
            mesh = m
            m.setVertices(floatArrayOf(0f, 0f, 0f, 1f, 0f, 0f, 0f, 1f, 0f))
            m.enableInstancedRendering(true, 2, VertexAttribute(VertexAttributes.Usage.Generic, 4, "i_pos"))
            m.setInstanceData(floatArrayOf(0f, 0f, 0f, 0f, 0.5f, 0f, 0f, 0f))
            val sh = ShaderProgram(
                "attribute vec3 a_position;\nattribute vec4 i_pos;\nvoid main() { gl_Position = vec4(a_position + i_pos.xyz, 1.0) * 0.0; }\n",
                "#ifdef GL_ES\nprecision mediump float;\n#endif\nvoid main() { gl_FragColor = vec4(0.0); }\n",
            )
            shader = sh
            check(sh.isCompiled) { sh.log }
            // Drain errors left by earlier GL calls so only this draw is judged.
            while (Gdx.gl.glGetError() != GL20.GL_NO_ERROR) {
            }
            sh.bind()
            m.render(sh, GL20.GL_TRIANGLES)
            Gdx.gl.glGetError() == GL20.GL_NO_ERROR
        } catch (t: Throwable) {
            Gdx.app.error(APP_LOG_TAG, "instancing check failed", t)
            false
        } finally {
            shader?.dispose()
            mesh?.dispose()
        }
    }
}

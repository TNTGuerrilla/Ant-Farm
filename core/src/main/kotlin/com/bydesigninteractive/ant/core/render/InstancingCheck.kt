package com.bydesigninteractive.ant.core.render

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.Mesh
import com.badlogic.gdx.graphics.VertexAttribute
import com.badlogic.gdx.graphics.VertexAttributes
import com.badlogic.gdx.graphics.glutils.ShaderProgram

/**
 * Draws two instances of one triangle off-screen-sized (into the current target) with the
 * instancing path the ant, shadow and grass renderers use, and reports whether GL accepted it.
 * Run once at start; a false result means the 3D view falls back to the debug renderer.
 */
object InstancingCheck {
    fun run(): Boolean {
        if (Gdx.gl30 == null) return false
        return try {
            val mesh = Mesh(true, 3, 0, VertexAttribute(VertexAttributes.Usage.Position, 3, "a_position"))
            mesh.setVertices(floatArrayOf(0f, 0f, 0f, 1f, 0f, 0f, 0f, 1f, 0f))
            mesh.enableInstancedRendering(true, 2, VertexAttribute(VertexAttributes.Usage.Generic, 4, "i_pos"))
            mesh.setInstanceData(floatArrayOf(0f, 0f, 0f, 0f, 0.5f, 0f, 0f, 0f))
            val shader = ShaderProgram(
                "attribute vec3 a_position;\nattribute vec4 i_pos;\nvoid main() { gl_Position = vec4(a_position + i_pos.xyz, 1.0) * 0.0; }\n",
                "#ifdef GL_ES\nprecision mediump float;\n#endif\nvoid main() { gl_FragColor = vec4(0.0); }\n",
            )
            check(shader.isCompiled) { shader.log }
            shader.bind()
            mesh.render(shader, GL20.GL_TRIANGLES)
            val error = Gdx.gl.glGetError()
            shader.dispose()
            mesh.dispose()
            error == GL20.GL_NO_ERROR
        } catch (t: Throwable) {
            Gdx.app.error("AntFarm", "instancing check failed", t)
            false
        }
    }
}

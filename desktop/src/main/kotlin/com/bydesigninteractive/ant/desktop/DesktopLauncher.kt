package com.bydesigninteractive.ant.desktop

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration
import com.badlogic.gdx.graphics.glutils.ShaderProgram
import com.bydesigninteractive.ant.core.AntApp
import com.bydesigninteractive.ant.core.stub.StubApp
import com.bydesigninteractive.ant.sim.scenario.Scenarios

/** Runs the M1 app; `--stub` runs the M0 stub instead, and `--seed=N` picks the world seed. */
fun main(args: Array<String>) {
    useLegacyShadersOnCoreProfile()
    val config = Lwjgl3ApplicationConfiguration().apply {
        setTitle("Ant Farm")
        setOpenGLEmulation(Lwjgl3ApplicationConfiguration.GLEmulation.GL30, 3, 3)
        setWindowedMode(1600, 900)
        // 8 stencil bits: the 3D view draws the nest entrance's dip through the stencil buffer.
        setBackBufferConfig(8, 8, 8, 8, 16, 8, 0)
        useVsync(true)
        setForegroundFPS(0)
    }
    val seed = args.firstOrNull { it.startsWith("--seed=") }?.substringAfter('=')?.toLongOrNull() ?: 1L
    val app = if ("--stub" in args) StubApp("desktop") else AntApp("desktop", Scenarios.starter(seed), tv = false)
    Lwjgl3Application(app, config)
}

/**
 * The window asks for GL 3.3 core, the first desktop version with `glVertexAttribDivisor` (which
 * instancing needs). The core profile rejects GLSL without a `#version` line, and libGDX 1.14.2
 * does not add one on desktop, so every shader (ours and SpriteBatch's, ModelBatch's and BitmapFont's) gets this
 * GLSL 1.50 header that maps the legacy keywords onto their core equivalents. Must run before any
 * [ShaderProgram] is created.
 */
private fun useLegacyShadersOnCoreProfile() {
    ShaderProgram.prependVertexCode = "#version 150\n#define attribute in\n#define varying out\n"
    ShaderProgram.prependFragmentCode = "#version 150\n#define varying in\nout vec4 fragColor;\n" +
        "#define gl_FragColor fragColor\n#define texture2D texture\n#define textureCube texture\n"
}

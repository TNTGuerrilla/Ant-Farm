package com.bydesigninteractive.ant.core

import com.badlogic.gdx.ApplicationAdapter
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input.Keys
import com.badlogic.gdx.InputAdapter
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Matrix4
import com.bydesigninteractive.ant.core.camera.ChaseCamera3
import com.bydesigninteractive.ant.core.camera.ViewCamera
import com.bydesigninteractive.ant.core.render.ChunkTextures
import com.bydesigninteractive.ant.core.render.DebugSurfaceRenderer
import com.bydesigninteractive.ant.core.render.NestRenderer
import com.bydesigninteractive.ant.core.render.Sprites
import com.bydesigninteractive.ant.core.render.SurfaceTopRenderer
import com.bydesigninteractive.ant.core.stub.FrameStats
import com.bydesigninteractive.ant.core.ui.DebugReadout
import com.bydesigninteractive.ant.sim.DT
import com.bydesigninteractive.ant.sim.World
import com.bydesigninteractive.ant.sim.ant.Ant
import com.bydesigninteractive.ant.sim.ant.Role
import com.bydesigninteractive.ant.sim.ant.Space
import com.bydesigninteractive.ant.sim.world.CHUNK_MM

const val APP_LOG_TAG = "AntFarm"

/**
 * The M1 app: runs the world at a fixed 20 ticks per second (sped up on request) and shows it
 * in one of three views. On the TV ([tv] true) it starts in the 3D view and the remote picks
 * ants and views; on desktop the keyboard adds panning, zoom and speed.
 */
class AntApp(private val label: String, private val world: World, private val tv: Boolean) : ApplicationAdapter() {
    private enum class View(val title: String) { NEST("nest"), SURFACE_TOP("surface, top"), SURFACE_3D("surface, 3D debug") }

    private val frames = FrameStats(600)
    private val nestCam = ViewCamera()
    private val topCam = ViewCamera()
    private val chase = ChaseCamera3()
    private val ortho = OrthographicCamera()
    private val hud = Matrix4()
    private lateinit var batch: SpriteBatch
    private lateinit var font: BitmapFont
    private lateinit var pixel: Texture
    private lateinit var antTexture: Texture
    private lateinit var antRegion: TextureRegion
    private lateinit var chunkTextures: ChunkTextures
    private lateinit var nestRenderer: NestRenderer
    private lateinit var topRenderer: SurfaceTopRenderer
    private lateinit var renderer3d: DebugSurfaceRenderer
    private var view = if (tv) View.SURFACE_3D else View.NEST
    private var speedIndex = 0
    private var paused = false
    private var followed: Ant? = null
    private var manualNest = false
    private var manualTop = false
    private var showTrail = true
    private var accumulator = 0f
    private var logClock = 0f
    private var hudLines: List<String> = emptyList()
    private var hudAge = HUD_EVERY_S

    override fun create() {
        batch = SpriteBatch()
        font = BitmapFont().apply {
            region.texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear)
        }
        pixel = Sprites.pixel()
        antTexture = Sprites.ant()
        antRegion = TextureRegion(antTexture)
        chunkTextures = ChunkTextures(world.surface)
        nestRenderer = NestRenderer(world)
        topRenderer = SurfaceTopRenderer(world, chunkTextures)
        renderer3d = DebugSurfaceRenderer(world)
        followed = world.ants.firstOrNull { it.role == Role.FORAGER }
        Gdx.input.inputProcessor = object : InputAdapter() {
            override fun keyDown(keycode: Int): Boolean = onKey(keycode)

            override fun scrolled(amountX: Float, amountY: Float): Boolean {
                zoom(if (amountY > 0) 1.15f else 1f / 1.15f)
                return true
            }
        }
        Gdx.app.log(APP_LOG_TAG, "start $label ${Gdx.graphics.width}x${Gdx.graphics.height} seed ${world.seed}")
    }

    override fun resize(width: Int, height: Int) {
        if (width <= 0 || height <= 0) return
        ortho.viewportWidth = width.toFloat()
        ortho.viewportHeight = height.toFloat()
        renderer3d.resize(width, height)
        hud.setToOrtho2D(0f, 0f, width.toFloat(), height.toFloat())
        font.data.setScale(1.4f * height / 1080f)
    }

    override fun render() {
        val raw = Gdx.graphics.deltaTime
        val dt = raw.coerceAtMost(0.25f)
        frames.add(raw * 1000f)
        advance(dt)
        if (!tv) pollPan(dt)
        chunkTextures.newFrame()
        Gdx.gl.glClearColor(0.05f, 0.04f, 0.03f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT or GL20.GL_DEPTH_BUFFER_BIT)
        when (view) {
            View.NEST -> drawNest(dt)
            View.SURFACE_TOP -> drawTop(dt)
            View.SURFACE_3D -> draw3d(dt)
        }
        drawHud(dt)
        logClock += raw
        if (logClock >= LOG_EVERY_S) {
            logClock = 0f
            Gdx.app.log(
                APP_LOG_TAG,
                "view ${view.title} fps ${Gdx.graphics.framesPerSecond} p50 ${frames.percentile(0.5f)} " +
                    "p99 ${frames.percentile(0.99f)} max ${frames.max()} slow ${frames.countOver(25f)}/${frames.count} tick ${world.tick}",
            )
        }
    }

    private fun advance(dt: Float) {
        if (paused) return
        accumulator += dt * SPEEDS[speedIndex]
        val start = System.nanoTime()
        while (accumulator >= DT) {
            world.step()
            accumulator -= DT
            if (System.nanoTime() - start > STEP_BUDGET_NS) {
                accumulator = 0f // behind: slow the simulation down, not the frame rate
                break
            }
        }
    }

    private fun drawNest(dt: Float) {
        val w = Gdx.graphics.width
        val h = Gdx.graphics.height
        val a = followed
        val g = world.nest
        if (!manualNest) {
            if (a != null && a.space == Space.NEST) {
                nestCam.follow(a.x, a.y, h, dt)
            } else if (g.airCells > 0) {
                nestCam.frame(g.airMinX.toFloat(), g.airMinY.toFloat(), g.airMaxX + 1f, g.airMaxY + 1f, w, h, dt)
            }
        }
        ortho.position.set(nestCam.centerX, -nestCam.centerY, 0f)
        ortho.zoom = nestCam.mmPerPx
        ortho.update()
        batch.projectionMatrix = ortho.combined
        batch.begin()
        nestRenderer.draw(batch, ortho, antRegion, pixel)
        batch.end()
    }

    private fun drawTop(dt: Float) {
        val h = Gdx.graphics.height
        if (!topCam.placed) topCam.placeAt(world.surface.entranceX, world.surface.entranceY, 0.4f, h)
        val a = followed
        if (!manualTop && a != null && a.space == Space.SURFACE) topCam.follow(a.x, a.y, h, dt)
        ortho.position.set(topCam.centerX, topCam.centerY, 0f)
        ortho.zoom = topCam.mmPerPx
        ortho.update()
        batch.projectionMatrix = ortho.combined
        batch.begin()
        topRenderer.draw(batch, ortho, antRegion, pixel, showTrail, dt)
        batch.end()
        chunkTextures.evict((topCam.centerX / CHUNK_MM).toInt(), (topCam.centerY / CHUNK_MM).toInt(), 3)
    }

    private fun draw3d(dt: Float) {
        val a = followed
        val s = world.surface
        when {
            a != null && a.space == Space.SURFACE -> chase.update(a.x, a.y, a.z, a.fx, a.fy, a.fz, a.nx, a.ny, a.nz, dt)
            !chase.placed -> chase.update(
                s.entranceX, s.entranceY, s.ground.height(s.entranceX, s.entranceY),
                1f, 0f, 0f, 0f, 0f, 1f, dt,
            )
        }
        renderer3d.draw(chase)
    }

    private fun drawHud(dt: Float) {
        val help = if (tv) {
            "Left/Right: pick ant   OK: next view   Back: exit"
        } else {
            "Tab view   Space pause   1/2/3 speed   WASD/arrows pan   +/- or wheel zoom   F follow   P trail   (3D: arrows pick ant)"
        }
        hudAge += dt
        if (hudAge >= HUD_EVERY_S) {
            hudAge = 0f
            val speed = if (paused) "paused" else "${SPEEDS[speedIndex].toInt()}x"
            hudLines = DebugReadout.lines(world, view.title, speed, Gdx.graphics.framesPerSecond, frames, followed, help)
        }
        val lines = hudLines
        val lineH = font.lineHeight
        val top = Gdx.graphics.height - 12f
        batch.projectionMatrix = hud
        batch.begin()
        batch.setColor(0f, 0f, 0f, 0.55f)
        batch.draw(pixel, 6f, top - lines.size * lineH - 10f, Gdx.graphics.width * 0.62f, lines.size * lineH + 16f)
        batch.color = Color.WHITE
        lines.forEachIndexed { i, line -> font.draw(batch, line, 16f, top - i * lineH) }
        batch.end()
    }

    private fun onKey(keycode: Int): Boolean {
        val picking = tv || view == View.SURFACE_3D
        when (keycode) {
            Keys.TAB, Keys.V, Keys.CENTER, Keys.ENTER -> view = View.entries[(view.ordinal + 1) % View.entries.size]
            Keys.SPACE -> paused = !paused
            Keys.NUM_1 -> speedIndex = 0
            Keys.NUM_2 -> speedIndex = 1
            Keys.NUM_3 -> speedIndex = 2
            Keys.P -> showTrail = !showTrail
            Keys.F -> {
                followNext(1)
                manualNest = false
                manualTop = false
            }
            Keys.PLUS, Keys.EQUALS -> zoom(1f / 1.25f)
            Keys.MINUS -> zoom(1.25f)
            Keys.RIGHT, Keys.UP -> if (picking) followNext(1) else return false
            Keys.LEFT, Keys.DOWN -> if (picking) followNext(-1) else return false
            else -> return false
        }
        return true
    }

    /** Held keys pan the nest and top-down views (desktop only). */
    private fun pollPan(dt: Float) {
        if (view == View.SURFACE_3D) return
        val input = Gdx.input
        var dx = 0f
        var dy = 0f
        if (input.isKeyPressed(Keys.A) || input.isKeyPressed(Keys.LEFT)) dx -= 1f
        if (input.isKeyPressed(Keys.D) || input.isKeyPressed(Keys.RIGHT)) dx += 1f
        if (input.isKeyPressed(Keys.W) || input.isKeyPressed(Keys.UP)) dy += 1f
        if (input.isKeyPressed(Keys.S) || input.isKeyPressed(Keys.DOWN)) dy -= 1f
        if (dx == 0f && dy == 0f) return
        val px = PAN_PX_PER_S * dt
        if (view == View.NEST) {
            manualNest = true
            nestCam.pan(dx * px, -dy * px) // nest y is depth, so up on screen is less depth
        } else {
            manualTop = true
            topCam.pan(dx * px, dy * px)
        }
    }

    private fun zoom(factor: Float) {
        val h = Gdx.graphics.height
        when (view) {
            View.NEST -> {
                manualNest = true
                nestCam.zoomBy(factor, h)
            }
            View.SURFACE_TOP -> topCam.zoomBy(factor, h)
            View.SURFACE_3D -> Unit
        }
    }

    /** Follows the next (or previous) forager; in the 3D view, prefers ants outside. */
    private fun followNext(dir: Int) {
        val foragers = world.ants.filter { it.role == Role.FORAGER }
        if (foragers.isEmpty()) return
        val candidates = if (view == View.SURFACE_3D) foragers.filter { it.space == Space.SURFACE }.ifEmpty { foragers } else foragers
        val i = followed?.let { candidates.indexOf(it) } ?: -1
        followed = candidates[Math.floorMod(i + dir, candidates.size)]
    }

    override fun dispose() {
        renderer3d.dispose()
        topRenderer.dispose()
        nestRenderer.dispose()
        chunkTextures.dispose()
        antTexture.dispose()
        pixel.dispose()
        font.dispose()
        batch.dispose()
    }

    private companion object {
        val SPEEDS = floatArrayOf(1f, 4f, 16f)
        const val STEP_BUDGET_NS = 6_000_000L
        const val HUD_EVERY_S = 0.25f
        const val PAN_PX_PER_S = 700f
        const val LOG_EVERY_S = 10f
    }
}

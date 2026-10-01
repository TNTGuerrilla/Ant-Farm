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
import com.bydesigninteractive.ant.core.engine.AntPose
import com.bydesigninteractive.ant.core.engine.AntStates
import com.bydesigninteractive.ant.core.engine.Command
import com.bydesigninteractive.ant.core.engine.Published
import com.bydesigninteractive.ant.core.engine.SimRunner
import com.bydesigninteractive.ant.core.render.AntAnimator
import com.bydesigninteractive.ant.core.render.ChunkTextures
import com.bydesigninteractive.ant.core.render.DebugSurfaceRenderer
import com.bydesigninteractive.ant.core.render.InstancingCheck
import com.bydesigninteractive.ant.core.render.NestRenderer
import com.bydesigninteractive.ant.core.render.Shaders
import com.bydesigninteractive.ant.core.render.Sprites
import com.bydesigninteractive.ant.core.render.SurfaceTopRenderer
import com.bydesigninteractive.ant.core.stub.FrameStats
import com.bydesigninteractive.ant.core.ui.DebugReadout
import com.bydesigninteractive.ant.sim.World
import com.bydesigninteractive.ant.sim.ant.Role
import com.bydesigninteractive.ant.sim.ant.Space
import com.bydesigninteractive.ant.sim.world.CHUNKS
import com.bydesigninteractive.ant.sim.world.CHUNK_MM
import java.util.Locale

const val APP_LOG_TAG = "AntFarm"

/**
 * The M1 app: a [SimRunner] runs the world on its own thread at a fixed 20 ticks per second (sped
 * up on request), and this class shows it in one of three views, drawing ants between the last
 * two snapshots. On the TV ([tv] true) it starts in the 3D view and the remote picks ants and
 * views; on desktop the keyboard adds panning, zoom and speed.
 *
 * [world] is read only in [create], for immutable values, before the runner starts. After that
 * only the simulation thread touches it, and this class reads [Published] state.
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
    private lateinit var antTextures: Array<Texture>
    private lateinit var antRegions: Array<TextureRegion>
    private val animator = AntAnimator()
    private lateinit var chunkTextures: ChunkTextures
    private lateinit var nestRenderer: NestRenderer
    private lateinit var topRenderer: SurfaceTopRenderer
    private lateinit var renderer3d: DebugSurfaceRenderer
    private lateinit var published: Published
    private lateinit var runner: SimRunner
    private val states = AntStates()
    private val poses = ArrayList<AntPose>() // a pool; the first posesCount entries are this frame's
    private var posesCount = 0
    private val drawn = PoseList()
    private var followedId = -1
    private var seed = 0L
    private var nestWidth = 0
    private var nestDepth = 0
    private var entranceX = 0f
    private var entranceY = 0f
    private var view = if (tv) View.SURFACE_3D else View.NEST
    private var speedIndex = 0
    private var paused = false
    private var manualNest = false
    private var manualTop = false
    private var showTrail = true
    private var logClock = 0f
    private var focusClock = FOCUS_EVERY_S
    private var sentFocusView = -1
    private var sentFocusChunk = -1
    private var failureLogged = false
    private var hudLines: List<String> = emptyList()
    private var hudAge = HUD_EVERY_S
    private var instanced = false

    override fun create() {
        batch = SpriteBatch()
        font = BitmapFont().apply {
            region.texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear)
        }
        pixel = Sprites.pixel()
        antTextures = Sprites.antFrames()
        antRegions = Array(antTextures.size) { TextureRegion(antTextures[it]) }
        // Immutable values, read before the simulation thread starts; the world is not read after.
        seed = world.seed
        nestWidth = world.nest.width
        nestDepth = world.nest.depth
        entranceX = world.surface.entranceX
        entranceY = world.surface.entranceY
        followedId = world.ants.firstOrNull { it.role == Role.FORAGER }?.id ?: -1
        published = Published(world)
        chunkTextures = ChunkTextures(seed)
        nestRenderer = NestRenderer(seed, nestWidth, nestDepth, published)
        topRenderer = SurfaceTopRenderer(published, chunkTextures, entranceX, entranceY)
        renderer3d = DebugSurfaceRenderer(seed, published)
        Gdx.input.inputProcessor = object : InputAdapter() {
            override fun keyDown(keycode: Int): Boolean = onKey(keycode)

            override fun scrolled(amountX: Float, amountY: Float): Boolean {
                zoom(if (amountY > 0) 1.15f else 1f / 1.15f)
                return true
            }
        }
        Gdx.app.log(APP_LOG_TAG, "start $label ${Gdx.graphics.width}x${Gdx.graphics.height} seed $seed")
        instanced = InstancingCheck.run()
        Gdx.app.log(APP_LOG_TAG, "gl ${Gdx.graphics.glVersion.majorVersion}.${Gdx.graphics.glVersion.minorVersion} instancing $instanced")
        checkShaders()
        runner = SimRunner(world, published)
        runner.start()
    }

    /** Compiles each of the look's programs once and logs the result, so a shader error shows on the first run. */
    private fun checkShaders() {
        try {
            listOf(Shaders::world, Shaders::ants, Shaders::grass, Shaders::shadows, Shaders::sky).forEach { it().dispose() }
            Gdx.app.log(APP_LOG_TAG, "shaders ok")
        } catch (e: IllegalStateException) {
            Gdx.app.error(APP_LOG_TAG, "shaders failed: ${e.message}")
        }
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
        updatePoses()
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
        focusClock += raw
        if (focusClock >= FOCUS_EVERY_S) {
            focusClock = 0f
            sendFocus()
        }
        val failure = published.failed
        if (failure != null && !failureLogged) {
            failureLogged = true
            Gdx.app.error(APP_LOG_TAG, "simulation stopped: $failure")
        }
        logClock += raw
        if (logClock >= LOG_EVERY_S) {
            logClock = 0f
            val h = published.hud
            Gdx.app.log(
                APP_LOG_TAG,
                "view ${view.title} fps ${Gdx.graphics.framesPerSecond} p50 ${frames.percentile(0.5f)} " +
                    "p99 ${frames.percentile(0.99f)} max ${frames.max()} slow ${frames.countOver(25f)}/${frames.count} tick ${h.tick} " +
                    "ticks/s ${f1(h.ticksPerSecond)} ms/tick ${f2(h.msPerTickAvg)} max ${f2(h.msPerTickMax)} resets ${h.resets}",
            )
        }
    }

    /** Takes the newest snapshot, if any, and fills the pose pool for this frame. */
    private fun updatePoses() {
        published.snapshots.takeFresh()?.let { states.accept(it) }
        val alpha = if (paused) 1f else states.alpha(System.nanoTime(), published.intervalNanos)
        while (poses.size < states.count) poses += AntPose()
        posesCount = states.count
        for (i in 0 until posesCount) {
            val p = poses[i]
            states.blend(i, alpha, p)
            animator.observe(p)
        }
        drawn.size = posesCount
    }

    /** The followed ant as drawn this frame, or null if it is not in the snapshot. */
    private fun followed(): AntPose? {
        for (i in 0 until posesCount) if (poses[i].id == followedId) return poses[i]
        return null
    }

    /** Tells the simulation thread where the camera looks, for the published overlay. */
    private fun sendFocus() {
        val x: Float
        val y: Float
        when (view) {
            View.NEST -> {
                x = entranceX
                y = entranceY
            }
            View.SURFACE_TOP -> {
                x = topCam.centerX
                y = topCam.centerY
            }
            View.SURFACE_3D -> {
                x = chase.targetX
                y = chase.targetY
            }
        }
        // Each Focus resets the overlay schedule, so send one only when the chunk or view changes.
        val chunk = (x / CHUNK_MM).toInt().coerceIn(0, CHUNKS - 1) + (y / CHUNK_MM).toInt().coerceIn(0, CHUNKS - 1) * CHUNKS
        if (view.ordinal == sentFocusView && chunk == sentFocusChunk) return
        sentFocusView = view.ordinal
        sentFocusChunk = chunk
        runner.send(Command.Focus(x, y))
    }

    private fun drawNest(dt: Float) {
        val w = Gdx.graphics.width
        val h = Gdx.graphics.height
        val a = followed()
        if (!manualNest) {
            if (a != null && a.space == Space.NEST) {
                nestCam.follow(a.x, a.y, h, dt)
            } else if (states.airCells > 0) {
                nestCam.frame(states.airMinX.toFloat(), states.airMinY.toFloat(), states.airMaxX + 1f, states.airMaxY + 1f, w, h, dt)
            }
        }
        ortho.position.set(nestCam.centerX, -nestCam.centerY, 0f)
        ortho.zoom = nestCam.mmPerPx
        ortho.update()
        batch.projectionMatrix = ortho.combined
        batch.begin()
        nestRenderer.draw(batch, ortho, drawn, antRegions, animator, pixel)
        batch.end()
    }

    private fun drawTop(dt: Float) {
        val h = Gdx.graphics.height
        if (!topCam.placed) topCam.placeAt(entranceX, entranceY, 0.4f, h)
        val a = followed()
        if (!manualTop && a != null && a.space == Space.SURFACE) topCam.follow(a.x, a.y, h, dt)
        ortho.position.set(topCam.centerX, topCam.centerY, 0f)
        ortho.zoom = topCam.mmPerPx
        ortho.update()
        batch.projectionMatrix = ortho.combined
        batch.begin()
        topRenderer.draw(batch, ortho, drawn, states.foods, antRegions, animator, pixel, showTrail)
        batch.end()
        chunkTextures.evict((topCam.centerX / CHUNK_MM).toInt(), (topCam.centerY / CHUNK_MM).toInt(), 3)
    }

    private fun draw3d(dt: Float) {
        val a = followed()
        when {
            a != null && a.space == Space.SURFACE -> chase.update(a.x, a.y, a.z, a.fx, a.fy, a.fz, a.nx, a.ny, a.nz, dt)
            !chase.placed -> chase.update(entranceX, entranceY, renderer3d.groundHeight(entranceX, entranceY), 1f, 0f, 0f, 0f, 0f, 1f, dt)
        }
        renderer3d.draw(chase, drawn, states.foods)
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
            val lines = DebugReadout.lines(published.hud, view.title, speed, Gdx.graphics.framesPerSecond, frames, followed(), help)
            val failure = published.failed
            hudLines = if (failure == null) lines else lines + "simulation stopped: ${failure.message ?: failure.javaClass.simpleName}"
        }
        val lines = hudLines
        val lineH = font.lineHeight
        val top = Gdx.graphics.height - 12f
        batch.projectionMatrix = hud
        batch.begin()
        batch.setColor(0f, 0f, 0f, 0.55f)
        batch.draw(pixel, 6f, top - lines.size * lineH - 10f, Gdx.graphics.width * 0.62f, lines.size * lineH + 16f)
        batch.color = Color.WHITE
        for (i in lines.indices) font.draw(batch, lines[i], 16f, top - i * lineH)
        batch.end()
    }

    private fun onKey(keycode: Int): Boolean {
        val picking = tv || view == View.SURFACE_3D
        when (keycode) {
            Keys.TAB, Keys.V, Keys.CENTER, Keys.ENTER -> view = View.entries[(view.ordinal + 1) % View.entries.size]
            Keys.SPACE -> {
                paused = !paused
                runner.send(if (paused) Command.Pause else Command.Resume)
            }
            Keys.NUM_1 -> setSpeed(0)
            Keys.NUM_2 -> setSpeed(1)
            Keys.NUM_3 -> setSpeed(2)
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

    private fun setSpeed(index: Int) {
        speedIndex = index
        runner.send(Command.SetSpeed(SPEEDS[index]))
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
        val foragers = drawn.filter { it.role == Role.FORAGER }
        if (foragers.isEmpty()) return
        val candidates = if (view == View.SURFACE_3D) foragers.filter { it.space == Space.SURFACE }.ifEmpty { foragers } else foragers
        val i = candidates.indexOfFirst { it.id == followedId }
        followedId = candidates[Math.floorMod(i + dir, candidates.size)].id
    }

    override fun pause() {
        runner.send(Command.Pause)
    }

    override fun resume() {
        if (!paused) runner.send(Command.Resume)
    }

    /**
     * Stops the simulation thread. Safe to call more than once, after [dispose], or before
     * [create] has run (then there is nothing to stop).
     */
    fun shutdown() {
        if (::runner.isInitialized) runner.stop()
    }

    override fun dispose() {
        shutdown()
        renderer3d.dispose()
        topRenderer.dispose()
        nestRenderer.dispose()
        chunkTextures.dispose()
        antTextures.forEach { it.dispose() }
        pixel.dispose()
        font.dispose()
        batch.dispose()
    }

    private fun f1(v: Float) = "%.1f".format(Locale.ROOT, v)

    private fun f2(v: Float) = "%.2f".format(Locale.ROOT, v)

    /** The first [size] entries of the pose pool, as a list the renderers iterate without copying. */
    private inner class PoseList : AbstractList<AntPose>() {
        override var size = 0

        override fun get(index: Int): AntPose = poses[index]
    }

    private companion object {
        val SPEEDS = floatArrayOf(1f, 4f, 16f)
        const val HUD_EVERY_S = 0.25f
        const val FOCUS_EVERY_S = 0.25f
        const val PAN_PX_PER_S = 700f
        const val LOG_EVERY_S = 10f
    }
}

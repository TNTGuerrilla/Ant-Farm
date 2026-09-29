package com.bydesigninteractive.ant.core.stub

import com.badlogic.gdx.ApplicationAdapter
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input.Keys
import com.badlogic.gdx.InputAdapter
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.utils.viewport.ExtendViewport

const val LOG_TAG = "AntM0"

private const val WORLD_W = 1920f
private const val WORLD_H = 1080f
private const val START_SPRITES = 1000
private const val MAX_SPRITES = 32000
private const val WRITE_BYTES = 20 * 1024 * 1024
private const val FIRST_WRITE_S = 10f
private const val WRITE_EVERY_S = 30f
private const val LOG_EVERY_S = 10f
private const val SLOW_FRAME_MS = 25f // a missed 60 Hz vsync or worse

/**
 * Milestone M0: the TV stub. It checks three things on real hardware:
 * whether D-pad keys reach an interactive dream, whether 1,000 moving ant sprites hold the
 * frame rate, and whether a 20 MB background save causes visible hitches.
 *
 * Remote: Up doubles the sprites, Down halves them, Center starts a write now. Back exits.
 *
 * @param label where it runs ("dream", "activity" or "desktop"), shown in the overlay.
 * @param rawKeys filled by the Android host with keys as the window received them, before
 *   libGDX; null where there is no such layer.
 */
class StubApp(private val label: String, val rawKeys: KeyLog? = null) : ApplicationAdapter() {
    private val gdxKeys = KeyLog()
    private val frames = FrameStats(600)
    private lateinit var viewport: ExtendViewport
    private lateinit var batch: SpriteBatch
    private lateinit var font: BitmapFont
    private lateinit var pixel: Texture
    private lateinit var swarm: Swarm
    private lateinit var probe: WriteProbe
    private var clock = 0f
    private var nextWrite = FIRST_WRITE_S
    private var nextLog = LOG_EVERY_S

    // Frames rendered while the current or last write was running.
    private var writeFrames = 0
    private var writeFrameMax = 0f
    private var writeSlowFrames = 0
    private var trackedWrite = 0

    override fun create() {
        viewport = ExtendViewport(WORLD_W, WORLD_H)
        batch = SpriteBatch()
        font = BitmapFont().apply {
            region.texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear)
            data.setScale(1.6f)
        }
        pixel = Pixmap(1, 1, Pixmap.Format.RGBA8888).let { p ->
            p.setColor(Color.WHITE)
            p.fill()
            Texture(p).also { p.dispose() }
        }
        swarm = Swarm()
        swarm.resize(START_SPRITES, WORLD_W, WORLD_H)
        probe = WriteProbe(Gdx.files.local("m0").file(), WRITE_BYTES)
        Gdx.input.inputProcessor = object : InputAdapter() {
            override fun keyDown(keycode: Int): Boolean {
                gdxKeys.add("down ${Keys.toString(keycode)} ($keycode)")
                Gdx.app.log(LOG_TAG, "gdx keyDown ${Keys.toString(keycode)} ($keycode)")
                return act(keycode)
            }

            override fun keyUp(keycode: Int): Boolean {
                gdxKeys.add("up   ${Keys.toString(keycode)} ($keycode)")
                Gdx.app.log(LOG_TAG, "gdx keyUp ${Keys.toString(keycode)} ($keycode)")
                return false
            }
        }
        Gdx.app.log(
            LOG_TAG,
            "start $label ${Gdx.graphics.width}x${Gdx.graphics.height} " +
                "GL ${Gdx.gl.glGetString(GL20.GL_RENDERER)} / ${Gdx.gl.glGetString(GL20.GL_VERSION)}",
        )
    }

    private fun act(keycode: Int): Boolean = when (keycode) {
        Keys.UP -> {
            swarm.resize((swarm.size * 2).coerceAtMost(MAX_SPRITES), viewport.worldWidth, viewport.worldHeight)
            true
        }
        Keys.DOWN -> {
            swarm.resize((swarm.size / 2).coerceAtLeast(START_SPRITES / 8), viewport.worldWidth, viewport.worldHeight)
            true
        }
        Keys.CENTER, Keys.ENTER -> {
            startWrite()
            true
        }
        else -> false
    }

    private fun startWrite() {
        if (probe.start()) {
            nextWrite = clock + WRITE_EVERY_S
            writeFrames = 0
            writeFrameMax = 0f
            writeSlowFrames = 0
        }
    }

    override fun resize(width: Int, height: Int) {
        viewport.update(width, height, true)
    }

    override fun render() {
        val dt = Gdx.graphics.deltaTime
        val ms = dt * 1000f
        clock += dt
        frames.add(ms)
        if (probe.busy) {
            writeFrames++
            if (ms > writeFrameMax) writeFrameMax = ms
            if (ms > SLOW_FRAME_MS) writeSlowFrames++
        }
        if (clock >= nextWrite) startWrite()
        reportFinishedWrite()
        if (clock >= nextLog) {
            nextLog += LOG_EVERY_S
            Gdx.app.log(LOG_TAG, "frames ${statsLine()}")
        }

        swarm.update(dt.coerceAtMost(0.1f), viewport.worldWidth, viewport.worldHeight)

        Gdx.gl.glClearColor(0.35f, 0.26f, 0.19f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)
        viewport.apply()
        batch.projectionMatrix = viewport.camera.combined
        batch.begin()
        swarm.draw(batch)
        drawOverlay()
        batch.end()
    }

    private fun reportFinishedWrite() {
        val result = probe.last ?: return
        if (result.index == trackedWrite || probe.busy) return
        trackedWrite = result.index
        Gdx.app.log(LOG_TAG, "write ${writeLine(result)} | ${duringWriteLine()}")
    }

    private fun statsLine(): String =
        "sprites ${swarm.size}  fps ${Gdx.graphics.framesPerSecond}  " +
            "ms p50 ${f1(frames.percentile(0.5f))} p99 ${f1(frames.percentile(0.99f))} " +
            "max ${f1(frames.max())}  slow ${frames.countOver(SLOW_FRAME_MS)}/${frames.count}"

    private fun writeLine(result: WriteProbe.Result): String {
        val t = result.timing ?: return "#${result.index} failed: ${result.error}"
        return "#${result.index} 20 MB in ${ms(t.totalNanos)} ms " +
            "(write ${ms(t.writeNanos)}, sync ${ms(t.syncNanos)}, rename ${ms(t.renameNanos)})"
    }

    private fun duringWriteLine(): String =
        "frames during it: $writeFrames, max ${f1(writeFrameMax)} ms, slow $writeSlowFrames"

    private fun drawOverlay() {
        val lines = buildList {
            add("Ant Farm M0 stub ($label)  ${Gdx.graphics.width}x${Gdx.graphics.height}")
            add(statsLine())
            val result = probe.last
            add(
                when {
                    probe.busy -> "Writing 20 MB...  ${duringWriteLine()}"
                    result == null -> "First 20 MB write in ${(nextWrite - clock).toInt().coerceAtLeast(0)} s"
                    else -> writeLine(result)
                },
            )
            if (result != null && !probe.busy) add(duringWriteLine())
            add("Up: sprites x2   Down: sprites / 2   Center: write now   Back: exit")
            rawKeys?.let { raw ->
                add("Keys at the window (${raw.total}):")
                raw.lines().forEach { add("   $it") }
            }
            add("Keys at libGDX (${gdxKeys.total}):")
            gdxKeys.lines().forEach { add("   $it") }
        }
        val lineH = font.lineHeight
        val pad = 16f
        val top = viewport.worldHeight - 24f
        batch.setColor(0f, 0f, 0f, 0.6f)
        batch.draw(pixel, 8f, top - lines.size * lineH - pad, 1100f, lines.size * lineH + pad * 2)
        batch.color = Color.WHITE
        lines.forEachIndexed { i, line -> font.draw(batch, line, 24f, top - i * lineH) }
    }

    override fun dispose() {
        probe.dispose()
        swarm.dispose()
        font.dispose()
        pixel.dispose()
        batch.dispose()
    }
}

private fun f1(value: Float): String = "%.1f".format(value)

private fun ms(nanos: Long): String = (nanos / 1_000_000).toString()

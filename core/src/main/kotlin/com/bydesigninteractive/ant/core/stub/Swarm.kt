package com.bydesigninteractive.ant.core.stub

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.MathUtils
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

// About 2.5 px per mm at 1080p: a 5 mm worker is 12 px long and walks 20 mm/s.
private const val ANT_W = 12f
private const val ANT_H = 6f
private const val SPEED = 50f
private const val TURN_JITTER = 2.5f // radians per sqrt(second)

/** Correlated random walkers drawn as ant sprites: the render-load stand-in for M0. */
class Swarm {
    private var x = FloatArray(0)
    private var y = FloatArray(0)
    private var heading = FloatArray(0)
    var size = 0
        private set

    private val texture = antPixmap().let { pixmap -> Texture(pixmap).also { pixmap.dispose() } }.apply {
        setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear)
    }
    private val region = TextureRegion(texture)

    fun resize(count: Int, width: Float, height: Float) {
        if (count > x.size) {
            x = x.copyOf(count)
            y = y.copyOf(count)
            heading = heading.copyOf(count)
        }
        for (i in size until count) {
            x[i] = MathUtils.random(width)
            y[i] = MathUtils.random(height)
            heading[i] = MathUtils.random(MathUtils.PI2)
        }
        size = count
    }

    fun update(dt: Float, width: Float, height: Float) {
        val jitter = TURN_JITTER * sqrt(dt)
        val step = SPEED * dt
        for (i in 0 until size) {
            var h = heading[i] + MathUtils.randomTriangular(-jitter, jitter)
            var nx = x[i] + cos(h) * step
            var ny = y[i] + sin(h) * step
            if (nx < 0f || nx > width) {
                h = MathUtils.PI - h
                nx = nx.coerceIn(0f, width)
            }
            if (ny < 0f || ny > height) {
                h = -h
                ny = ny.coerceIn(0f, height)
            }
            x[i] = nx
            y[i] = ny
            heading[i] = h
        }
    }

    fun draw(batch: SpriteBatch) {
        for (i in 0 until size) {
            batch.draw(
                region, x[i] - ANT_W / 2, y[i] - ANT_H / 2, ANT_W / 2, ANT_H / 2,
                ANT_W, ANT_H, 1f, 1f, heading[i] * MathUtils.radiansToDegrees,
            )
        }
    }

    fun dispose() {
        texture.dispose()
    }
}

/** A 32x16 top-down worker facing +x: gaster, thorax, head, six legs and two antennae. */
private fun antPixmap(): Pixmap {
    val p = Pixmap(32, 16, Pixmap.Format.RGBA8888)
    p.setColor(Color(0.10f, 0.07f, 0.05f, 1f))
    fillEllipse(p, 8f, 8f, 6.5f, 4.5f)
    fillEllipse(p, 17f, 8f, 4f, 2.5f)
    fillEllipse(p, 24f, 8f, 3.5f, 3f)
    p.drawLine(15, 6, 12, 1); p.drawLine(17, 6, 17, 1); p.drawLine(19, 6, 22, 1)
    p.drawLine(15, 10, 12, 15); p.drawLine(17, 10, 17, 15); p.drawLine(19, 10, 22, 15)
    p.drawLine(26, 6, 30, 2); p.drawLine(26, 10, 30, 14)
    return p
}

private fun fillEllipse(p: Pixmap, cx: Float, cy: Float, rx: Float, ry: Float) {
    for (py in 0 until p.height) for (px in 0 until p.width) {
        val dx = (px + 0.5f - cx) / rx
        val dy = (py + 0.5f - cy) / ry
        if (dx * dx + dy * dy <= 1f) p.drawPixel(px, py)
    }
}

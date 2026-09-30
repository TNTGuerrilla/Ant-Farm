package com.bydesigninteractive.ant.core.stub

import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.MathUtils
import com.bydesigninteractive.ant.core.render.Sprites
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

    private val texture = Sprites.ant()
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

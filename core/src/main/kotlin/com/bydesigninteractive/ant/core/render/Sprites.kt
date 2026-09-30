package com.bydesigninteractive.ant.core.render

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture

/**
 * Procedural placeholder art. The ant faces +x. Each call returns a new texture the caller owns
 * and must dispose.
 */
object Sprites {
    fun ant(): Texture = texture(antPixmap())

    fun pixel(): Texture = texture(Pixmap(1, 1, Pixmap.Format.RGBA8888).apply {
        setColor(Color.WHITE)
        fill()
    })

    private fun texture(p: Pixmap): Texture = Texture(p).also {
        p.dispose()
        it.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear)
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

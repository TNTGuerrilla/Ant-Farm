package com.bydesigninteractive.ant.core.render

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture

/** Procedural placeholder art. The ant faces +x; tufts and plants stand on their bottom edge. */
object Sprites {
    fun ant(): Texture = texture(antPixmap())

    fun pixel(): Texture = texture(Pixmap(1, 1, Pixmap.Format.RGBA8888).apply {
        setColor(Color.WHITE)
        fill()
    })

    /** A clump of grass blades, 32 x 64, transparent around them. */
    fun tuft(): Texture {
        val p = Pixmap(32, 64, Pixmap.Format.RGBA8888)
        val blades = intArrayOf(-12, -8, -5, -2, 1, 4, 7, 11)
        for ((i, dx) in blades.withIndex()) {
            val shade = 0.30f + (i % 3) * 0.06f
            p.setColor(0.18f, shade + 0.15f, 0.08f, 1f)
            val top = 4 + (i * 7) % 18
            p.drawLine(16, 63, 16 + dx, top)
            p.drawLine(17, 63, 17 + dx, top)
        }
        return texture(p)
    }

    /** An aphid plant: a stem with leaves and a cluster of aphids near the top, 32 x 256. */
    fun plant(): Texture {
        val p = Pixmap(32, 256, Pixmap.Format.RGBA8888)
        p.setColor(0.32f, 0.42f, 0.16f, 1f)
        p.fillRectangle(15, 20, 3, 236)
        p.setColor(0.25f, 0.50f, 0.18f, 1f)
        for (y in 60 until 240 step 36) {
            fillEllipse(p, 8f, y.toFloat(), 7f, 3f)
            fillEllipse(p, 24f, y + 18f, 7f, 3f)
        }
        p.setColor(0.12f, 0.18f, 0.10f, 1f)
        for (i in 0 until 14) p.fillCircle(13 + (i * 5) % 8, 24 + i * 5, 2)
        return texture(p)
    }

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

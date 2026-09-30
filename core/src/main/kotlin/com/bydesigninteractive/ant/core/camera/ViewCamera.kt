package com.bydesigninteractive.ant.core.camera

import kotlin.math.exp
import kotlin.math.max

/** A worker is about 5 mm long. */
const val ANT_LENGTH_MM = 5f

/** The zoom-out limit: an ant stays at least this many pixels long on a 1080-pixel-tall screen. */
const val MIN_ANT_PX = 8f

/** The zoom-in limit. */
const val MAX_ANT_PX = 60f

/**
 * A 2D camera in millimeters for the nest and top-down views. It frames a region or follows a
 * point with easing, and never zooms out so far that an ant drops below [MIN_ANT_PX]; beyond
 * that the view pans instead. Limits scale with screen height.
 */
class ViewCamera {
    var centerX = 0f
        private set
    var centerY = 0f
        private set
    var mmPerPx = 0.2f
        private set
    var placed = false
        private set

    fun maxMmPerPx(screenH: Int): Float = ANT_LENGTH_MM / (MIN_ANT_PX * screenH / 1080f)

    fun minMmPerPx(screenH: Int): Float = ANT_LENGTH_MM / (MAX_ANT_PX * screenH / 1080f)

    /** The zoom that fits [w] by [h] millimeters plus a margin, within the readable limits. */
    fun fitZoom(w: Float, h: Float, screenW: Int, screenH: Int): Float {
        val fit = max((w + 2 * MARGIN_MM) / screenW, (h + 2 * MARGIN_MM) / screenH)
        return fit.coerceIn(minMmPerPx(screenH), maxMmPerPx(screenH))
    }

    /** Eases toward framing the box from (minX, minY) to (maxX, maxY). */
    fun frame(minX: Float, minY: Float, maxX: Float, maxY: Float, screenW: Int, screenH: Int, dt: Float) {
        ease((minX + maxX) / 2, (minY + maxY) / 2, fitZoom(maxX - minX, maxY - minY, screenW, screenH), dt)
    }

    /** Eases toward centering on (x, y) at the current zoom. */
    fun follow(x: Float, y: Float, screenH: Int, dt: Float) {
        ease(x, y, mmPerPx.coerceIn(minMmPerPx(screenH), maxMmPerPx(screenH)), dt)
    }

    /** Moves the center by a distance in screen pixels. */
    fun pan(dxPx: Float, dyPx: Float) {
        centerX += dxPx * mmPerPx
        centerY += dyPx * mmPerPx
    }

    fun zoomBy(factor: Float, screenH: Int) {
        mmPerPx = (mmPerPx * factor).coerceIn(minMmPerPx(screenH), maxMmPerPx(screenH))
    }

    fun placeAt(x: Float, y: Float, zoom: Float) {
        centerX = x
        centerY = y
        mmPerPx = zoom
        placed = true
    }

    private fun ease(x: Float, y: Float, zoom: Float, dt: Float) {
        if (!placed) {
            placeAt(x, y, zoom)
            return
        }
        val k = 1f - exp(-EASE_RATE * dt)
        centerX += (x - centerX) * k
        centerY += (y - centerY) * k
        mmPerPx += (zoom - mmPerPx) * k
    }

    private companion object {
        const val MARGIN_MM = 20f
        const val EASE_RATE = 2f
    }
}

package com.bydesigninteractive.ant.sim.world

import com.bydesigninteractive.ant.sim.util.unit

/** An axis-aligned block of cells, inclusive of both corners. */
data class Rect(val x0: Int, val y0: Int, val x1: Int, val y1: Int) {
    fun contains(x: Int, y: Int): Boolean = x in x0..x1 && y in y0..y1
}

/**
 * The scripted architect: the order in which the nest is excavated. A shaft 5 mm wide runs down
 * from the entrance; chambers 6 mm tall and 12 to 21 mm wide bud off it on alternating sides,
 * and the shaft segments between them lengthen with depth (reference section 7). In M2 the ants'
 * own rules replace this plan.
 */
class NestPlan(private val seed: Long, private val entranceX: Int) {
    private val rects = ArrayList<Rect>()
    private var shaftBottom = 0
    private var chambers = 0

    fun rect(i: Int): Rect {
        while (rects.size <= i) extend()
        return rects[i]
    }

    private fun extend() {
        if (rects.isEmpty()) {
            rects += shaft(FIRST_SHAFT)
            return
        }
        chambers++
        val w = 12 + (unit(seed, CHAMBER_SALT, chambers) * 10).toInt()
        val top = shaftBottom - CHAMBER_HEIGHT
        rects += if (chambers % 2 == 1) {
            Rect(entranceX + 3, top, entranceX + 2 + w, top + CHAMBER_HEIGHT - 1)
        } else {
            Rect(entranceX - 2 - w, top, entranceX - 3, top + CHAMBER_HEIGHT - 1)
        }
        rects += shaft(30 + 8 * chambers)
    }

    private fun shaft(length: Int): Rect {
        val r = Rect(entranceX - 2, shaftBottom, entranceX + 2, shaftBottom + length - 1)
        shaftBottom += length
        return r
    }

    private companion object {
        const val FIRST_SHAFT = 40
        const val CHAMBER_HEIGHT = 6
        const val CHAMBER_SALT = 10
    }
}

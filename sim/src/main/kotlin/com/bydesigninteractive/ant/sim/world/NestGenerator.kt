package com.bydesigninteractive.ant.sim.world

import com.bydesigninteractive.ant.sim.util.unit
import kotlin.math.abs
import kotlin.math.sin

/**
 * The untouched soil of the nest slice, computed from the seed on demand so it never needs
 * storing: soil, a wavy clay band, scattered stones, and the water table as a hard floor.
 * Coordinates are millimeters: x across the slice, y downward from the surface.
 *
 * The column under the entrance is kept free of stones so the scripted excavation plan always
 * has a way down; in M2 the ants dig around stones themselves.
 */
class NestGenerator(private val seed: Long, val width: Int, val depth: Int) {
    /** The depth where the soil is waterlogged. Nothing at or below it can be dug. */
    val waterTable: Int = depth - 60 - (unit(seed, 1, 0) * 80).toInt()
    private val clayTop = depth * 3 / 10 + (unit(seed, 2, 0) * depth / 5).toInt()
    private val clayPhase = unit(seed, 3, 0) * 6.283
    private val entranceX = width / 2

    fun material(x: Int, y: Int): Material = when {
        y >= waterTable -> Material.WATER
        abs(x - entranceX) > CLEAR_COLUMN && stone(x, y) -> Material.STONE
        clay(x, y) -> Material.CLAY
        else -> Material.SOIL
    }

    private fun clay(x: Int, y: Int): Boolean {
        val top = clayTop + (sin(x / 90.0 + clayPhase) * 15).toInt()
        return y >= top && y < top + CLAY_THICKNESS
    }

    /** Stones sit on a coarse lattice: a few lattice cells hold a disc of 2 to 6 mm radius. */
    private fun stone(x: Int, y: Int): Boolean {
        val gx0 = Math.floorDiv(x, STONE_CELL)
        val gy0 = Math.floorDiv(y, STONE_CELL)
        for (gy in gy0 - 1..gy0 + 1) for (gx in gx0 - 1..gx0 + 1) {
            if (unit(seed xor STONE_SALT, gx, gy) >= STONE_CHANCE) continue
            val sx = gx * STONE_CELL + (unit(seed xor (STONE_SALT + 1), gx, gy) * STONE_CELL).toInt()
            val sy = gy * STONE_CELL + (unit(seed xor (STONE_SALT + 2), gx, gy) * STONE_CELL).toInt()
            val r = 2 + (unit(seed xor (STONE_SALT + 3), gx, gy) * 5).toInt()
            val dx = x - sx
            val dy = y - sy
            if (dx * dx + dy * dy <= r * r) return true
        }
        return false
    }

    private companion object {
        const val CLAY_THICKNESS = 40
        const val CLEAR_COLUMN = 8
        const val STONE_CELL = 16
        const val STONE_CHANCE = 0.05
        const val STONE_SALT = 0x5703L
    }
}

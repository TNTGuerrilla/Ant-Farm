package com.bydesigninteractive.ant.sim.world

data class Cell(val x: Int, val y: Int)

/**
 * Works through the [NestPlan] block by block. The frontier is the cells of the current block
 * that can be dug and touch air (or the open surface above the top row), so an ant can stand
 * next to them. A block is done when nothing diggable is left in it, or when what is left
 * cannot be reached.
 */
class Excavation(private val grid: NestGrid, private val plan: NestPlan) {
    var block = 0
        private set

    /** True once the plan reaches the water table: the nest stops growing downward. */
    var finished = false
        private set

    /** Increases whenever the frontier is recomputed. */
    var version = 0L
        private set

    private var seenGrid = -1L
    private val cells = ArrayList<Cell>()

    fun frontier(): List<Cell> {
        refresh()
        return cells
    }

    fun refresh() {
        if (seenGrid == grid.version) return
        seenGrid = grid.version
        cells.clear()
        while (!finished) {
            val r = plan.rect(block)
            if (r.y1 >= grid.generator.waterTable || !grid.inBounds(r.x0, r.y0) || !grid.inBounds(r.x1, r.y1)) {
                finished = true
                break
            }
            var remaining = 0
            for (y in r.y0..r.y1) for (x in r.x0..r.x1) {
                if (!grid.material(x, y).diggable) continue
                remaining++
                if (touchesAir(x, y)) cells += Cell(x, y)
            }
            if (remaining > 0 && cells.isNotEmpty()) break
            cells.clear()
            block++
        }
        version++
    }

    private fun touchesAir(x: Int, y: Int) =
        y == 0 || grid.isAir(x + 1, y) || grid.isAir(x - 1, y) || grid.isAir(x, y + 1) || grid.isAir(x, y - 1)
}

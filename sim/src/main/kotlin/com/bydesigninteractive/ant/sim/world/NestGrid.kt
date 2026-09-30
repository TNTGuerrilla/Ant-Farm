package com.bydesigninteractive.ant.sim.world

import kotlin.math.exp

const val TILE = 64

/**
 * The nest slice on a 1 mm grid, stored in 64 x 64 tiles that are created lazily: a tile nobody
 * has changed has no storage and reads straight from the [generator]. Building pheromone lives
 * on the same tiles and is also only stored where it exists.
 */
class NestGrid(val generator: NestGenerator) {
    val width = generator.width
    val depth = generator.depth
    val tilesX = (width + TILE - 1) / TILE
    val tilesY = (depth + TILE - 1) / TILE
    private val cells = arrayOfNulls<ByteArray>(tilesX * tilesY)
    private val pheromone = arrayOfNulls<FloatArray>(tilesX * tilesY)
    private val airInTile = IntArray(tilesX * tilesY)
    private val tileVersions = LongArray(tilesX * tilesY)

    /** Increases on every change of material, so caches know when to refresh. */
    var version = 0L
        private set
    var airCells = 0
        private set

    // The bounding box of all air ever dug. It only grows: M1 never fills air back in.
    var airMinX = Int.MAX_VALUE
        private set
    var airMinY = Int.MAX_VALUE
        private set
    var airMaxX = Int.MIN_VALUE
        private set
    var airMaxY = Int.MIN_VALUE
        private set

    fun inBounds(x: Int, y: Int): Boolean = x in 0 until width && y in 0 until depth

    /** Outside the slice reads as stone, so nothing walks or digs out of it. */
    fun material(x: Int, y: Int): Material {
        if (!inBounds(x, y)) return Material.STONE
        val tile = cells[tileIndex(x, y)] ?: return generator.material(x, y)
        return Material.of(tile[cellIndex(x, y)].toInt())
    }

    fun isAir(x: Int, y: Int): Boolean = material(x, y) == Material.AIR

    fun set(x: Int, y: Int, m: Material) {
        require(inBounds(x, y)) { "($x, $y) is outside the nest" }
        val ti = tileIndex(x, y)
        val tile = cells[ti] ?: materialize(ti)
        val ci = cellIndex(x, y)
        val old = Material.of(tile[ci].toInt())
        if (old == m) return
        tile[ci] = m.ordinal.toByte()
        if (old == Material.AIR) {
            airInTile[ti]--
            airCells--
        }
        if (m == Material.AIR) {
            airInTile[ti]++
            airCells++
            if (x < airMinX) airMinX = x
            if (x > airMaxX) airMaxX = x
            if (y < airMinY) airMinY = y
            if (y > airMaxY) airMaxY = y
        }
        version++
        tileVersions[ti] = version
    }

    /** 0 for a tile that was never changed; otherwise the [version] of its latest change. */
    fun tileVersion(tx: Int, ty: Int): Long = tileVersions[tx + ty * tilesX]

    fun modifiedTileCount(): Int = cells.count { it != null }

    /** A copy of a changed tile's cells (material ordinals, row by row), or null if never changed. */
    fun copyTile(tx: Int, ty: Int): ByteArray? = cells[tx + ty * tilesX]?.copyOf()

    /** A tile is active if it holds air, touches a tile that does, or holds building pheromone. */
    fun isActive(tx: Int, ty: Int): Boolean {
        if (pheromone[tx + ty * tilesX] != null) return true
        for (ny in ty - 1..ty + 1) for (nx in tx - 1..tx + 1) {
            if (nx in 0 until tilesX && ny in 0 until tilesY && airInTile[nx + ny * tilesX] > 0) return true
        }
        return false
    }

    fun activeTileCount(): Int {
        var n = 0
        for (ty in 0 until tilesY) for (tx in 0 until tilesX) if (isActive(tx, ty)) n++
        return n
    }

    fun buildPheromone(x: Int, y: Int): Float {
        if (!inBounds(x, y)) return 0f
        return pheromone[tileIndex(x, y)]?.get(cellIndex(x, y)) ?: 0f
    }

    fun addBuildPheromone(x: Int, y: Int, amount: Float) {
        if (!inBounds(x, y)) return
        val ti = tileIndex(x, y)
        val field = pheromone[ti] ?: FloatArray(TILE * TILE).also { pheromone[ti] = it }
        field[cellIndex(x, y)] += amount
    }

    /**
     * Decays building pheromone with the given mean lifetime. Only tiles that hold pheromone are
     * visited, and a tile whose pheromone has faded drops its storage.
     */
    fun stepPheromone(dt: Float, lifetimeSeconds: Float) {
        val keep = exp(-dt / lifetimeSeconds)
        for (i in pheromone.indices) {
            val field = pheromone[i] ?: continue
            var max = 0f
            for (j in field.indices) {
                val v = field[j] * keep
                field[j] = v
                if (v > max) max = v
            }
            if (max < FADED) pheromone[i] = null
        }
    }

    fun pheromoneTileCount(): Int = pheromone.count { it != null }

    private fun materialize(ti: Int): ByteArray {
        val tx = ti % tilesX
        val ty = ti / tilesX
        val tile = ByteArray(TILE * TILE)
        for (cy in 0 until TILE) for (cx in 0 until TILE) {
            val x = tx * TILE + cx
            val y = ty * TILE + cy
            val m = if (inBounds(x, y)) generator.material(x, y) else Material.STONE
            tile[cy * TILE + cx] = m.ordinal.toByte()
        }
        cells[ti] = tile
        return tile
    }

    private fun tileIndex(x: Int, y: Int) = x / TILE + (y / TILE) * tilesX

    private fun cellIndex(x: Int, y: Int) = (y % TILE) * TILE + x % TILE

    private companion object {
        const val FADED = 1e-3f
    }
}

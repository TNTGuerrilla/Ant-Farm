package com.bydesigninteractive.ant.sim.ant

import com.bydesigninteractive.ant.sim.DT
import com.bydesigninteractive.ant.sim.World
import com.bydesigninteractive.ant.sim.world.DistanceMap
import kotlin.math.atan2
import kotlin.math.sqrt

/** Walking the nest grid: cell to cell through air, choosing each next cell from a distance map. */
internal object NestMotion {
    /**
     * Moves the ant toward its next cell. Returns true when it stands on its current cell's
     * center and needs a new next cell. Vertical steps use the slower shaft speed (section 10).
     */
    fun move(w: World, a: Ant): Boolean {
        if (a.nextX == a.cellX && a.nextY == a.cellY) {
            a.speed = 0f
            return true
        }
        val p = w.params
        val base = if (a.nextY != a.cellY) p.shaftSpeed else p.surfaceSpeed
        val speed = if (a.carriesPellet) base * p.loadedFactor else base
        val tx = a.nextX + 0.5f
        val ty = a.nextY + 0.5f
        val dx = tx - a.x
        val dy = ty - a.y
        val d = sqrt(dx * dx + dy * dy)
        val step = speed * DT
        a.heading = atan2(dy, dx)
        a.speed = speed
        if (d <= step) {
            a.x = tx
            a.y = ty
            a.cellX = a.nextX
            a.cellY = a.nextY
            return true
        }
        a.x += dx / d * step
        a.y += dy / d * step
        return false
    }

    /** Sets the next cell to a neighbor whose [map] value is [want], at random among several. */
    fun stepTo(w: World, a: Ant, map: DistanceMap, want: Int): Boolean {
        var count = 0
        var pick = -1
        for (k in 0 until 4) {
            if (map.get(a.cellX + DistanceMap.DX[k], a.cellY + DistanceMap.DY[k]) != want) continue
            count++
            if (w.rng.nextInt(count) == 0) pick = k
        }
        if (pick < 0) return false
        a.nextX = a.cellX + DistanceMap.DX[pick]
        a.nextY = a.cellY + DistanceMap.DY[pick]
        return true
    }

    /** Walks toward the entrance. True once the ant stands in the top row, ready to leave. */
    fun goUp(w: World, a: Ant): Boolean {
        if (!move(w, a)) return false
        if (a.cellY == 0) return true
        val d = w.paths.up.get(a.cellX, a.cellY)
        if (d != DistanceMap.UNREACHED) stepTo(w, a, w.paths.up, d - 1)
        return false
    }

    /** Walks deeper until [steps] from the entrance or a dead end. True while still walking. */
    fun goDown(w: World, a: Ant, steps: Int): Boolean {
        if (!move(w, a)) return true
        val d = w.paths.up.get(a.cellX, a.cellY)
        if (d == DistanceMap.UNREACHED || d >= steps) return false
        return stepTo(w, a, w.paths.up, d + 1)
    }
}

package com.bydesigninteractive.ant.sim.world

/** The distance maps nest ants walk by, rebuilt only when the nest or the frontier changes. */
class NestPaths(private val grid: NestGrid, private val excavation: Excavation) {
    /** Steps from each air cell to the entrance opening in the top row. */
    val up = DistanceMap(grid)

    /** Steps from each air cell to a cell next to the digging frontier. */
    val toFront = DistanceMap(grid)

    private var upFor = -1L
    private var frontFor = -1L
    private var sources = IntArray(256)

    fun refresh() {
        if (upFor != grid.version) {
            upFor = grid.version
            var n = 0
            for (x in 0 until grid.width) {
                if (!grid.isAir(x, 0)) continue
                sources = ensure(sources, n + 1)
                sources[n++] = x
            }
            up.rebuild(sources, n)
        }
        val frontier = excavation.frontier()
        if (frontFor != excavation.version) {
            frontFor = excavation.version
            var n = 0
            for (c in frontier) for (k in 0 until 4) {
                val x = c.x + DistanceMap.DX[k]
                val y = c.y + DistanceMap.DY[k]
                if (!grid.isAir(x, y)) continue
                sources = ensure(sources, n + 1)
                sources[n++] = y * grid.width + x
            }
            toFront.rebuild(sources, n)
        }
    }
}

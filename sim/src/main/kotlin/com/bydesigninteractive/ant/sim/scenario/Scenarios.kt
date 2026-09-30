package com.bydesigninteractive.ant.sim.scenario

import com.bydesigninteractive.ant.sim.World
import com.bydesigninteractive.ant.sim.ant.AntParams
import com.bydesigninteractive.ant.sim.ant.Role
import com.bydesigninteractive.ant.sim.world.FoodKind
import com.bydesigninteractive.ant.sim.world.FoodSource

/** Ready-made worlds for watching and for tests. */
object Scenarios {
    /**
     * A young colony to watch: the entrance shaft already dug, seeded plants and prey, diggers
     * and foragers. Digging is compressed to 4 s per cell so it shows at real time; realistic
     * digging (about 30 min per cell per ant) waits for the calendar clock in M3.
     */
    fun starter(seed: Long, foragers: Int = 60, diggers: Int = 20): World {
        val w = World(seed, AntParams(digSecondsPerCell = 4f))
        w.predig(1)
        w.surface.placePlants()
        w.surface.placePrey()
        repeat(diggers) { w.addAnt(Role.DIGGER) }
        repeat(foragers) { w.addAnt(Role.FORAGER) }
        return w
    }

    /** Gruter 2012: two identical feeders at equal [distance] on either side of the entrance. */
    fun gruter(seed: Long, foragers: Int, distance: Float = 250f, quality: Float = 1f): World {
        val w = World(seed)
        w.predig(5)
        val s = w.surface
        s.foods += FoodSource(0, FoodKind.HONEYDEW, s.entranceX - distance, s.entranceY, 15f, quality)
        s.foods += FoodSource(1, FoodKind.HONEYDEW, s.entranceX + distance, s.entranceY, 15f, quality)
        repeat(foragers) { w.addAnt(Role.FORAGER) }
        return w
    }
}

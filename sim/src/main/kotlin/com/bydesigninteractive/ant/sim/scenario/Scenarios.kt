package com.bydesigninteractive.ant.sim.scenario

import com.bydesigninteractive.ant.sim.World
import com.bydesigninteractive.ant.sim.ant.AntParams
import com.bydesigninteractive.ant.sim.ant.Role
import com.bydesigninteractive.ant.sim.brain.Genome
import com.bydesigninteractive.ant.sim.world.FoodKind
import com.bydesigninteractive.ant.sim.world.FoodSource

/** Ready-made worlds for watching and for tests. */
object Scenarios {
    /**
     * A young colony to watch: the entrance shaft already dug, seeded plants and prey, diggers
     * and foragers. Digging is compressed to 4 s per cell so it shows at real time; realistic
     * digging (about 30 min per cell per ant) waits for the calendar clock in M3. The ants run
     * [genome] (the instinct brain unless given).
     */
    fun starter(
        seed: Long,
        foragers: Int = 60,
        diggers: Int = 20,
        genome: Genome? = null,
    ): World {
        val w = World(seed, AntParams(digSecondsPerCell = 4f), genome = genome)
        w.predig(1)
        w.surface.placePlants()
        w.surface.placePrey()
        repeat(diggers) { w.addAnt(Role.DIGGER) }
        repeat(foragers) { w.addAnt(Role.FORAGER) }
        return w
    }

    /**
     * The 1,000-ant benchmark colony (M1b-2b): the starter layout with 800 foragers and 200
     * diggers. Deliberately surface-heavy, a conservative stand-in for a mature colony.
     */
    fun colony1000(seed: Long): World = starter(seed, foragers = 800, diggers = 200)

    /**
     * Gruter 2012: two identical feeders at equal [distance] on either side of the entrance, as
     * small food blobs on gently rolling ground with no rocks, so the geometry matches M1's test.
     * The ants run [genome] (the instinct brain unless given).
     */
    fun gruter(
        seed: Long,
        foragers: Int,
        distance: Float = 250f,
        quality: Float = 1f,
        genome: Genome? = null,
    ): World {
        val w = World(seed, rocks = false, genome = genome)
        w.predig(5)
        val s = w.surface
        for ((id, x) in listOf(0 to s.entranceX - distance, 1 to s.entranceX + distance)) {
            s.addFood(FoodSource(
                id, FoodKind.HONEYDEW, x, s.entranceY, 15f, quality,
                z = s.ground.height(x, s.entranceY) + 3f, bodyRadius = 8f,
            ))
        }
        repeat(foragers) { w.addAnt(Role.FORAGER) }
        return w
    }
}

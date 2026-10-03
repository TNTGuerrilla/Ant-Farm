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
     * small food drops on gently rolling ground with no rocks, so the geometry matches M1's test.
     * The ants run [genome] (the instinct brain unless given). [places] sets how many can feed at
     * each feeder at once (FoodSource.capacity), as Gruter's feeding holes did, independently of
     * the drop's size; 0 derives it from the drop ([GRUTER_LOW_CROWDING], [GRUTER_HIGH_CROWDING]).
     */
    fun gruter(
        seed: Long,
        foragers: Int,
        distance: Float = 250f,
        quality: Float = 1f,
        genome: Genome? = null,
        places: Int = 0,
    ): World {
        val w = World(seed, rocks = false, genome = genome)
        w.predig(5)
        val s = w.surface
        for ((id, x) in listOf(0 to s.entranceX - distance, 1 to s.entranceX + distance)) {
            s.addFood(feeder(w, id, x, quality, places = places))
        }
        repeat(foragers) { w.addAnt(Role.FORAGER) }
        return w
    }

    /**
     * Two honeydew feeders on gently rolling open ground (no rocks), as in [gruter]: feeder 0 at
     * [distanceA] west of the entrance with [qualityA] and [loadsA] trips, feeder 1 at
     * [distanceB] east with [qualityB], never running out. For the realism search's experiments (M2a).
     */
    fun feeders(
        seed: Long,
        foragers: Int,
        distanceA: Float,
        distanceB: Float,
        qualityA: Float = 1f,
        qualityB: Float = 1f,
        loadsA: Int = Int.MAX_VALUE,
        genome: Genome? = null,
    ): World {
        val w = World(seed, rocks = false, genome = genome)
        w.predig(5)
        val s = w.surface
        val xa = s.entranceX - distanceA
        val xb = s.entranceX + distanceB
        s.addFood(feeder(w, 0, xa, qualityA, loadsA))
        s.addFood(feeder(w, 1, xb, qualityB))
        repeat(foragers) { w.addAnt(Role.FORAGER) }
        return w
    }

    /**
     * Gruter 2012 covered its feeders with 1, 9 or 27 feeding holes, and its model, validated
     * against the colonies, turned them into crowding thresholds of 8, 72 and 216 ants, 8 per hole
     * (research notes 04, 2.4). Little crowding (9 or 27 holes): the feeder leading after 5 minutes
     * won 11 of 12 trials. High crowding (1 hole): no symmetry breaking (51 : 49), and crowded
     * colonies moved most foragers to a better feeder within about 10 minutes; the model showed
     * that colonies of about 150 foraging agents still switch at a threshold of 8. A covered
     * feeder is reached only through its holes, so its places are set by them, not by the drop's
     * edge. GruterScenarioTest's symmetry-breaking checks (30 and 150 foragers) model little
     * crowding with 9 holes (72 places); its switching check models high crowding with 1 hole
     * (8 places). The drop stays the 8 mm one of the other experiments, so only the crowding
     * differs between the checks.
     */
    const val GRUTER_PLACES_PER_HOLE = 8
    const val GRUTER_LOW_CROWDING = 9 * GRUTER_PLACES_PER_HOLE
    const val GRUTER_HIGH_CROWDING = 1 * GRUTER_PLACES_PER_HOLE

    /**
     * A honeydew feeder drop of 8 mm on the entrance's row at [x], 3 mm above the ground, reached
     * within 15 mm; open (25 places, FoodSource.spots) unless [places] covers it.
     */
    private fun feeder(w: World, id: Int, x: Float, quality: Float, loads: Int = Int.MAX_VALUE, places: Int = 0): FoodSource {
        val s = w.surface
        return FoodSource(
            id, FoodKind.HONEYDEW, x, s.entranceY, 15f, quality, loads = loads,
            z = s.ground.height(x, s.entranceY) + 3f, bodyRadius = 8f, places = places,
        )
    }
}

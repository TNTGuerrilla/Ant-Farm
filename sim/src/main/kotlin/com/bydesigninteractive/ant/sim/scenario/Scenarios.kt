package com.bydesigninteractive.ant.sim.scenario

import com.bydesigninteractive.ant.sim.World
import com.bydesigninteractive.ant.sim.ant.AntParams
import com.bydesigninteractive.ant.sim.ant.Role
import com.bydesigninteractive.ant.sim.brain.Genome
import com.bydesigninteractive.ant.sim.world.FoodKind
import com.bydesigninteractive.ant.sim.world.FoodSource
import kotlin.math.PI

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
     * food drops of [feederRadius] on gently rolling ground with no rocks, so the geometry matches
     * M1's test. The ants run [genome] (the instinct brain unless given). The drop's size sets how
     * many can feed at once (FoodSource.capacity): [GRUTER_LOW_CROWDING] and [GRUTER_HIGH_CROWDING]
     * give the two conditions of the paper.
     */
    fun gruter(
        seed: Long,
        foragers: Int,
        distance: Float = 250f,
        quality: Float = 1f,
        genome: Genome? = null,
        feederRadius: Float = FEEDER_RADIUS,
    ): World {
        val w = World(seed, rocks = false, genome = genome)
        w.predig(5)
        val s = w.surface
        for ((id, x) in listOf(0 to s.entranceX - distance, 1 to s.entranceX + distance)) {
            s.addFood(feeder(w, id, x, quality, feederRadius = feederRadius))
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

    /** The experiments' feeder drop (mm): 25 feeding places (FoodSource.spots). */
    const val FEEDER_RADIUS = 8f

    /**
     * Gruter 2012's feeders were covered with 1, 9 or 27 feeding holes, and its model, validated
     * against the colonies, turned the holes into crowding thresholds of 8, 72 and 216 ants
     * (research notes 04, 2.4). Little crowding (9 or 27 holes): the feeder leading after 5
     * minutes won 11 of 12 trials. High crowding (1 hole): no symmetry breaking (51 : 49), and
     * crowded colonies moved most foragers to a better feeder within about 10 minutes; the model
     * showed that colonies of about 150 foraging agents still switch at a threshold of 8.
     * GruterScenarioTest's symmetry-breaking checks (30 and 150 foragers) model little crowding:
     * a drop of 23 mm radius with 72 places (9 holes). Its switching check models high
     * crowding with its 150 foragers: a drop of 2.5 mm radius with 8 places (1 hole).
     * Places are the drop's circumference over FoodSource.SPOT_WIDTH.
     */
    const val GRUTER_LOW_CROWDING = 72
    const val GRUTER_HIGH_CROWDING = 8

    /** The radius (mm) of a feeder drop with [places] feeding places around its edge (FoodSource.spots). */
    fun feederRadius(places: Int): Float = (places * FoodSource.SPOT_WIDTH / (2.0 * PI)).toFloat()

    /** The feeding reach beyond a feeder drop's edge (mm): 15 mm for the 8 mm drop, as since M1b-1. */
    private const val FEEDER_REACH = 7f

    /**
     * A honeydew feeder drop of [feederRadius] on the entrance's row at [x]: a dome whose centre
     * is 3/8 of its radius above the ground (3 mm for the 8 mm drop).
     */
    private fun feeder(w: World, id: Int, x: Float, quality: Float, loads: Int = Int.MAX_VALUE, feederRadius: Float = FEEDER_RADIUS): FoodSource {
        val s = w.surface
        return FoodSource(
            id, FoodKind.HONEYDEW, x, s.entranceY, feederRadius + FEEDER_REACH, quality, loads = loads,
            z = s.ground.height(x, s.entranceY) + feederRadius * 0.375f, bodyRadius = feederRadius,
        )
    }
}

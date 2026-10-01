package com.bydesigninteractive.ant.sim

import com.bydesigninteractive.ant.sim.ant.Ant
import com.bydesigninteractive.ant.sim.ant.Digger
import com.bydesigninteractive.ant.sim.ant.Forager
import com.bydesigninteractive.ant.sim.ant.AntParams
import com.bydesigninteractive.ant.sim.ant.AntState
import com.bydesigninteractive.ant.sim.ant.Role
import com.bydesigninteractive.ant.sim.ant.Space
import com.bydesigninteractive.ant.sim.ant.SpatialIndex
import com.bydesigninteractive.ant.sim.ant.SurfaceWalk
import com.bydesigninteractive.ant.sim.world.Excavation
import com.bydesigninteractive.ant.sim.world.Material
import com.bydesigninteractive.ant.sim.world.NestGenerator
import com.bydesigninteractive.ant.sim.world.NestGrid
import com.bydesigninteractive.ant.sim.world.NestPaths
import com.bydesigninteractive.ant.sim.world.NestPlan
import com.bydesigninteractive.ant.sim.world.SurfaceMap
import java.util.Random
import kotlin.collections.ArrayDeque
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.sin

/** Seconds per tick: the behavior clock runs at 20 ticks per second. */
const val DT = 0.05f

/** Feed events older than this many ticks (30 simulated minutes) are dropped. */
const val FEED_WINDOW_TICKS = 36_000L

data class FeedEvent(val tick: Long, val foodId: Int)

/**
 * The whole simulation: the surface map, the nest slice, and the ants moving between them
 * through the entrance. Fully determined by [seed]; [step] advances it by one tick.
 */
class World(
    val seed: Long,
    val params: AntParams = AntParams(),
    nestWidth: Int = 1200,
    nestDepth: Int = 1000,
    rocks: Boolean = true,
) {
    val rng = Random(seed)
    val nest = NestGrid(NestGenerator(seed, nestWidth, nestDepth))
    val nestEntranceX = nestWidth / 2
    val plan = NestPlan(seed, nestEntranceX)
    val excavation = Excavation(nest, plan)
    val paths = NestPaths(nest, excavation)
    val surface = SurfaceMap(seed, params, rocks)
    val ants = ArrayList<Ant>()
    val surfaceIndex = SpatialIndex()
    /** Recent feeding events, oldest first, for the last [FEED_WINDOW_TICKS] ticks. */
    val feedEvents = ArrayDeque<FeedEvent>()

    /** Set to measure where each tick's time goes; null (no cost) otherwise. */
    var profile: TickProfile? = null

    // Scratch vectors for surface walking (one simulation thread).
    internal val pos = FloatArray(3)
    internal val norm = FloatArray(3)

    /** Foragers that have brought food home and unloaded it. */
    var unloads = 0

    var tick = 0L
        private set
    val seconds: Float get() = tick * DT

    fun addAnt(role: Role): Ant {
        val desired = params.desiredMin + rng.nextFloat() * (params.desiredMax - params.desiredMin)
        val lays = rng.nextFloat() >= params.nonLayerFraction
        val a = Ant(ants.size, role, desired, lays)
        placeAtEntrance(a)
        a.state = if (role == Role.FORAGER) AntState.EXIT else AntState.IDLE
        ants += a
        return a
    }

    /** Digs out the first [blocks] blocks of the plan at once, for scenarios that start with a nest. */
    fun predig(blocks: Int) {
        for (i in 0 until blocks) {
            val r = plan.rect(i)
            for (y in r.y0..r.y1) for (x in r.x0..r.x1) {
                if (nest.material(x, y).diggable) nest.set(x, y, Material.AIR)
            }
        }
    }

    fun dig(x: Int, y: Int) {
        nest.set(x, y, Material.AIR)
        nest.addBuildPheromone(x, y, 1f)
    }

    /** The colony wants more room: too little air per ant, and the plan still has somewhere to go. */
    fun digNeeded(): Boolean = !excavation.finished && nest.airCells < ants.size * params.volumePerAnt

    /** Moves a surface ant into the top of the entrance shaft. It keeps whatever it carries. */
    fun enterNest(a: Ant) {
        a.space = Space.NEST
        placeAtEntrance(a)
        a.onTrail = false
    }

    /** Moves a nest ant out onto the surface just beside the entrance, facing away from it. */
    fun exitNest(a: Ant) {
        a.space = Space.SURFACE
        val angle = rng.nextFloat() * 2f * PI.toFloat()
        val r = params.entranceRadius + 1f
        SurfaceWalk.place(this, a, surface.entranceX + cos(angle) * r, surface.entranceY + sin(angle) * r, angle)
        a.homeDx = a.x - surface.entranceX
        a.homeDy = a.y - surface.entranceY
        a.runLeft = 0f
        a.onTrail = false
        a.timer = 0f
        a.arrived = false
    }

    fun gaussian(): Float = rng.nextGaussian().toFloat()

    fun exponential(mean: Float): Float = -ln(1f - rng.nextFloat()) * mean

    fun step() {
        val p = profile
        val sdf = surface.sdf
        val e0 = sdf.evaluations
        val h0 = surface.ground.samples
        val f0 = sdf.fastHits
        val m0 = sdf.fastMisses
        timed(p, TickProfile.NEST) { paths.refresh() }
        timed(p, TickProfile.FIELDS) { surface.step() }
        timed(p, TickProfile.NEST) { nest.stepPheromone(DT, params.buildPheromoneLifetime) }
        timed(p, TickProfile.INDEX) { surfaceIndex.rebuild(ants) }
        if (p == null) {
            for (a in ants) behave(a)
        } else {
            for (a in ants) {
                val phase = if (a.space == Space.SURFACE) TickProfile.SURFACE_ANTS else TickProfile.NEST_ANTS
                val t0 = System.nanoTime()
                behave(a)
                p.phaseNanos[phase] += System.nanoTime() - t0
            }
        }
        tick++
        while (feedEvents.isNotEmpty() && feedEvents.first().tick < tick - FEED_WINDOW_TICKS) feedEvents.removeFirst()
        if (p != null) {
            p.ticks++
            p.sdfEvaluations += sdf.evaluations - e0
            p.heightSamples += surface.ground.samples - h0
            p.fastHits += sdf.fastHits - f0
            p.fastMisses += sdf.fastMisses - m0
        }
    }

    private inline fun timed(p: TickProfile?, phase: Int, block: () -> Unit) {
        if (p == null) {
            block()
            return
        }
        val t0 = System.nanoTime()
        block()
        p.phaseNanos[phase] += System.nanoTime() - t0
    }

    private fun behave(a: Ant) = when (a.role) {
        Role.FORAGER -> Forager.update(this, a)
        Role.DIGGER -> Digger.update(this, a)
    }

    private fun placeAtEntrance(a: Ant) {
        a.cellX = nestEntranceX
        a.cellY = 0
        a.nextX = nestEntranceX
        a.nextY = 0
        a.x = nestEntranceX + 0.5f
        a.y = 0.5f
        a.arrived = false
    }
}

package com.bydesigninteractive.ant.core.engine

import com.bydesigninteractive.ant.sim.World
import com.bydesigninteractive.ant.sim.ant.Space
import com.bydesigninteractive.ant.sim.world.FoodKind

/** What an ant visibly carries in its mandibles. */
const val CARRY_NONE = 0
const val CARRY_PELLET = 1
const val CARRY_PREY = 2

/** Food as the renderers need it; immutable. */
data class FoodView(
    val x: Float,
    val y: Float,
    val z: Float,
    val radius: Float,
    val kind: FoodKind,
    val bodyRadius: Float,
    val stemRadius: Float,
    val stemBase: Float,
    val stemTop: Float,
)

/**
 * Everything about the ants and the nest outline after one tick, copied on the simulation thread
 * for the render thread. Arrays are reused and only grow, so filling allocates nothing once
 * they fit the colony.
 */
class Snapshot {
    var tick = 0L
    var publishedAt = 0L
    var seconds = 0f
    var count = 0
    var id = IntArray(0)
    var space = ByteArray(0) // 0 nest, 1 surface
    var role = ByteArray(0)
    var state = ByteArray(0)
    var speed = FloatArray(0)
    var heading = FloatArray(0)
    var x = FloatArray(0)
    var y = FloatArray(0)
    var z = FloatArray(0)
    var fx = FloatArray(0)
    var fy = FloatArray(0)
    var fz = FloatArray(0)
    var nx = FloatArray(0)
    var ny = FloatArray(0)
    var nz = FloatArray(0)
    var crop = FloatArray(0)
    var carry = ByteArray(0)
    var foods: List<FoodView> = emptyList()
    var airCells = 0
    var airMinX = 0
    var airMinY = 0
    var airMaxX = 0
    var airMaxY = 0

    fun ensure(n: Int) {
        if (x.size >= n) return
        val c = maxOf(n, x.size * 2, 64)
        id = id.copyOf(c)
        space = space.copyOf(c)
        role = role.copyOf(c)
        state = state.copyOf(c)
        speed = speed.copyOf(c)
        heading = heading.copyOf(c)
        x = x.copyOf(c)
        y = y.copyOf(c)
        z = z.copyOf(c)
        fx = fx.copyOf(c)
        fy = fy.copyOf(c)
        fz = fz.copyOf(c)
        nx = nx.copyOf(c)
        ny = ny.copyOf(c)
        nz = nz.copyOf(c)
        crop = crop.copyOf(c)
        carry = carry.copyOf(c)
    }

    /** Copies the world's ants and nest outline. Simulation thread only. */
    fun fill(w: World, now: Long, foodViews: List<FoodView>) {
        val ants = w.ants
        ensure(ants.size)
        count = ants.size
        tick = w.tick
        publishedAt = now
        seconds = w.seconds
        for (i in 0 until count) {
            val a = ants[i]
            id[i] = a.id
            space[i] = if (a.space == Space.SURFACE) 1 else 0
            role[i] = a.role.ordinal.toByte()
            state[i] = a.state.ordinal.toByte()
            speed[i] = a.speed
            heading[i] = a.heading
            x[i] = a.x
            y[i] = a.y
            z[i] = a.z
            fx[i] = a.fx
            fy[i] = a.fy
            fz[i] = a.fz
            nx[i] = a.nx
            ny[i] = a.ny
            nz[i] = a.nz
            val prey = a.lastFoodKind == FoodKind.PREY && a.crop > 0f
            carry[i] = when {
                a.carriesPellet -> CARRY_PELLET.toByte()
                prey -> CARRY_PREY.toByte()
                else -> CARRY_NONE.toByte()
            }
            crop[i] = if (prey || a.desiredCrop <= 0f) 0f else (a.crop / a.desiredCrop).coerceIn(0f, 1f)
        }
        foods = foodViews
        val g = w.nest
        airCells = g.airCells
        airMinX = g.airMinX
        airMinY = g.airMinY
        airMaxX = g.airMaxX
        airMaxY = g.airMaxY
    }
}

package com.bydesigninteractive.ant.sim.scenario

import com.bydesigninteractive.ant.sim.World

/**
 * A whole-run summary that changes if any ant, field count or random draw changes. It draws one
 * number from the world's random generator, so call it only at the end of a run.
 */
fun fingerprint(w: World): String {
    var sum = 0.0
    for (a in w.ants) {
        sum += a.x.toDouble() + a.y + a.z + a.fx + a.fy + a.fz + a.nx + a.ny + a.nz + a.heading +
            a.crop + a.timer + a.runLeft + a.homeDx + a.homeDy + a.senseL + a.senseR + a.speed +
            a.state.ordinal + a.space.ordinal + a.cellX + a.cellY
    }
    val draw = w.rng.nextLong()
    return "sum=$sum trail=${w.surface.trail.blockCount()} home=${w.surface.homeScent.blockCount()} " +
        "trailMax=${w.surface.trail.max()} homeMax=${w.surface.homeScent.max()} " +
        "feeds=${w.feedEvents.size} unloads=${w.unloads} ants=${w.ants.size} rng=$draw"
}

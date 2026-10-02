package com.bydesigninteractive.ant.sim.brain

import com.bydesigninteractive.ant.sim.DT
import com.bydesigninteractive.ant.sim.ant.Ant
import com.bydesigninteractive.ant.sim.ant.Space
import kotlin.math.max
import kotlin.math.min

/**
 * The body state the brain reads (spec 2.1). Reserves fall slowly outside and are refilled in the
 * nest (fed by nestmates) and to full by feeding; in M2a they never kill. At [RESERVE_DECAY] a
 * forager outside for 15 minutes reaches [TIRED], where the seed brain gives up searching, which
 * is the scripted forager's give-up time.
 */
internal object Body {
    const val RESERVE_DECAY = 0.35f / 900f
    const val RESERVE_REFILL = 1f / 60f
    const val TIRED = 0.65f

    /** "Never happened": a time-since value that reads as long ago. */
    const val NEVER = 1e6f

    fun tick(a: Ant) {
        a.ageSeconds += DT
        if (a.sinceFed < NEVER) a.sinceFed += DT
        a.reserves = if (a.space == Space.SURFACE) max(0f, a.reserves - RESERVE_DECAY * DT)
        else min(1f, a.reserves + RESERVE_REFILL * DT)
    }
}

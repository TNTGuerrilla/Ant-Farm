package com.bydesigninteractive.ant.sim.brain

import com.bydesigninteractive.ant.sim.DT
import com.bydesigninteractive.ant.sim.World
import com.bydesigninteractive.ant.sim.ant.Ant
import com.bydesigninteractive.ant.sim.ant.Role
import com.bydesigninteractive.ant.sim.ant.Space
import com.bydesigninteractive.ant.sim.world.FoodKind
import kotlin.math.exp
import kotlin.math.max

/**
 * Antennal contacts (spec 2.1, shared channels). A contact is a nestmate within [CONTACT_MM]
 * (about an antenna's reach); touching the same nestmate on consecutive checks counts once. Each
 * contact adds 1 to a count that decays with a [RATE_SECONDS] time constant (the contact rate),
 * and records whether the nestmate was a successful forager (carrying food) and which food odour
 * it carried, for [MET_SECONDS]. On the surface, an oncoming forager that gave up (tired and
 * empty, or turned away from a full or used-up source, Task 11a) leaves a nudge: the bearing of the direction it came from, its dead end, for [NUDGE_SECONDS].
 *
 * [update] must run once per tick for every ant: the decay, and the rule that the same nestmate
 * on consecutive checks counts once, both assume it.
 */
internal object Contacts {
    const val CONTACT_MM = 3f
    const val RATE_SECONDS = 10f
    const val MET_SECONDS = 10f
    const val NUDGE_SECONDS = 5f

    /** Headings with a dot product below this are oncoming. */
    private const val ONCOMING = -0.5f

    fun update(w: World, a: Ant) {
        val elapsed = (w.tick - a.contactTick) * DT
        a.contactTick = w.tick
        if (elapsed > 0f) {
            a.contactRate *= exp(-elapsed / RATE_SECONDS)
            a.nudgeLeft = max(0f, a.nudgeLeft - elapsed)
        }
        val index = if (a.space == Space.SURFACE) w.surfaceIndex else w.nestIndex
        val other = index.nearest(a, CONTACT_MM)
        if (other == null) {
            a.lastContactId = -1
            return
        }
        if (other.id == a.lastContactId) return
        a.lastContactId = other.id
        a.contactRate += 1f
        a.metTick = w.tick
        val loaded = other.crop > 0f
        a.metSuccess = loaded && other.role == Role.FORAGER
        a.metHoneydew = loaded && other.lastFoodKind == FoodKind.HONEYDEW
        a.metPrey = loaded && other.lastFoodKind == FoodKind.PREY
        if (a.space == Space.SURFACE && other.role == Role.FORAGER && !loaded && (other.reserves < Body.TIRED || other.turnedAway) &&
            a.fx * other.fx + a.fy * other.fy < ONCOMING
        ) {
            a.nudge = Senses.bearingSin(a, -other.fx, -other.fy)
            a.nudgeLeft = NUDGE_SECONDS
        }
    }
}

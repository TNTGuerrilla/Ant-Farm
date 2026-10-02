package com.bydesigninteractive.ant.sim.ant

import com.bydesigninteractive.ant.sim.World
import com.bydesigninteractive.ant.sim.brain.Actions
import com.bydesigninteractive.ant.sim.brain.Body
import com.bydesigninteractive.ant.sim.brain.Contacts
import com.bydesigninteractive.ant.sim.brain.Senses

/**
 * The brain-driven worker (M2a). Every tick it moves its reserves and updates its contacts (both
 * assume one call per tick). Every other tick, staggered by id so each tick evaluates half the
 * colony (spec 2.3), it senses, runs its network and samples an action among the possible ones;
 * between evaluations it keeps its turn rate, speed and action. Every tick it carries out the
 * current action with the innate primitives. During a timed primitive (feeding, unloading,
 * digging) it does not think. Foragers and diggers share this code.
 */
internal object BrainAnt {
    fun update(w: World, a: Ant) {
        Body.tick(a)
        Contacts.update(w, a)
        if (!Actions.busy(a) && (w.tick + a.id) % 2L == 0L) think(w, a)
        Actions.run(w, a)
        Actions.syncState(w, a)
    }

    private fun think(w: World, a: Ant) {
        val brain = a.brain ?: return
        val x = w.inputs
        Senses.fill(w, a, x)
        val o = w.outputs
        brain.evaluate(x, a.hidden, w.hiddenScratch, o)
        var bad = false
        for (i in o.indices) {
            if (!o[i].isFinite()) {
                o[i] = 0f
                bad = true
            }
        }
        if (bad) {
            w.nonFinite++
            a.hidden.fill(0f)
        }
        Actions.read(a, o)
        Actions.choose(w, a, x, o)
    }
}

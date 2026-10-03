package com.bydesigninteractive.ant.sim.brain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class InstinctTest {
    @Test
    fun theInstinctGenomeFitsTheLayout() {
        val g = Instinct.genome
        assertEquals(Senses.LAYOUT, g.layout)
        assertEquals(Senses.COUNT, g.inputs)
        assertEquals(Outputs.COUNT, g.outputs)
        assertEquals("resource", Instinct.source)
    }
}

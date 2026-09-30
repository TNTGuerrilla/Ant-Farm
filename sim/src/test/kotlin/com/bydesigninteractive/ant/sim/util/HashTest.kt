package com.bydesigninteractive.ant.sim.util

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class HashTest {
    @Test
    fun sameInputsGiveTheSameValue() {
        assertEquals(hash(7, 3, 4), hash(7, 3, 4))
    }

    @Test
    fun anyChangedInputChangesTheValue() {
        assertNotEquals(hash(7, 3, 4), hash(7, 4, 3))
        assertNotEquals(hash(7, 3, 4), hash(8, 3, 4))
    }

    @Test
    fun unitStaysInRangeAndIsEvenlySpread() {
        var sum = 0.0
        for (i in 0 until 10_000) {
            val u = unit(1, i, -i)
            assertTrue(u >= 0.0 && u < 1.0)
            sum += u
        }
        assertEquals(0.5, sum / 10_000, 0.02)
    }
}

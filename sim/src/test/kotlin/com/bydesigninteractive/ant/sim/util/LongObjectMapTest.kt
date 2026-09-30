package com.bydesigninteractive.ant.sim.util

import java.util.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class LongObjectMapTest {
    @Test
    fun putGetAndReplace() {
        val m = LongObjectMap<String>()
        m.put(5L, "a")
        m.put(-7L, "b")
        m.put(5L, "c")
        assertEquals("c", m.get(5L))
        assertEquals("b", m.get(-7L))
        assertNull(m.get(6L))
        assertEquals(2, m.size)
    }

    @Test
    fun agreesWithAHashMapUnderRandomOperations() {
        val m = LongObjectMap<Long>(4)
        val ref = HashMap<Long, Long>()
        val r = Random(3)
        repeat(20_000) {
            val k = r.nextInt(300).toLong() - 150L
            when (r.nextInt(3)) {
                0, 1 -> { m.put(k, k * 10); ref[k] = k * 10 }
                else -> assertEquals(ref.remove(k), m.remove(k))
            }
            assertEquals(ref.size, m.size)
        }
        for ((k, v) in ref) assertEquals(v, m.get(k))
        var seen = 0
        for (slot in 0 until m.capacity()) {
            val v = m.valueAt(slot) ?: continue
            assertEquals(ref[m.keyAt(slot)], v)
            seen++
        }
        assertEquals(ref.size, seen)
    }
}

package com.bydesigninteractive.ant.core.stub

import kotlin.test.Test
import kotlin.test.assertEquals

class KeyLogTest {
    @Test
    fun keepsTheNewestLinesAndCountsAll() {
        val log = KeyLog(capacity = 2)
        log.add("a")
        log.add("b")
        log.add("c")
        assertEquals(listOf("b", "c"), log.lines())
        assertEquals(3, log.total)
    }
}

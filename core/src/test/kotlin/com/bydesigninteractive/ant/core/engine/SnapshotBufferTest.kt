package com.bydesigninteractive.ant.core.engine

import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.thread
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SnapshotBufferTest {
    @Test
    fun nothingFreshUntilPublished() {
        val b = SnapshotBuffer()
        assertNull(b.takeFresh())
        b.writable().tick = 3
        b.publish()
        assertEquals(3L, b.takeFresh()!!.tick)
        assertNull(b.takeFresh())
    }

    @Test
    fun aReaderNeverSeesATornSnapshot() {
        val b = SnapshotBuffer()
        val done = AtomicBoolean(false)
        var torn = 0
        var seen = 0
        val reader = thread {
            // Checking done before taking guarantees the final publish is read after done is set.
            while (true) {
                val finished = done.get()
                val s = b.takeFresh()
                if (s == null) {
                    if (finished) break
                    continue
                }
                seen++
                for (i in 0 until s.count) if (s.x[i] != s.tick.toFloat()) torn++
            }
        }
        for (t in 1..20_000L) {
            val s = b.writable()
            s.ensure(64)
            s.count = 64
            s.tick = t
            for (i in 0 until 64) s.x[i] = t.toFloat()
            b.publish()
        }
        done.set(true)
        reader.join()
        assertEquals(0, torn)
        assertTrue(seen > 0)
    }
}

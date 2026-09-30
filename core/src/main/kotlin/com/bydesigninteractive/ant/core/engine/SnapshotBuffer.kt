package com.bydesigninteractive.ant.core.engine

import java.util.concurrent.atomic.AtomicInteger

/**
 * Triple buffering between the simulation thread (writer) and the render thread (reader). The
 * writer fills [writable] and calls [publish]; the reader calls [takeFresh]. Publication is one
 * atomic swap, so neither side blocks and the reader never sees a half-written snapshot.
 *
 * A snapshot returned by [takeFresh] is valid only until the next [takeFresh], because its slot
 * then goes back to the writer. A reader that needs it longer must copy it (as `AntStates` does).
 */
class SnapshotBuffer {
    private val slots = Array(3) { Snapshot() }
    private val middle = AtomicInteger(1)
    private var back = 0 // writer only
    private var front = 2 // reader only

    /** The snapshot the writer may fill. Writer thread only. */
    fun writable(): Snapshot = slots[back]

    /** Publishes the filled snapshot. Writer thread only. */
    fun publish() {
        back = middle.getAndSet(back or FRESH) and INDEX
    }

    /** The newest snapshot if one was published since the last call, else null. Reader thread only. */
    fun takeFresh(): Snapshot? {
        if (middle.get() and FRESH == 0) return null
        front = middle.getAndSet(front) and INDEX
        return slots[front]
    }

    private companion object {
        const val FRESH = 4
        const val INDEX = 3
    }
}

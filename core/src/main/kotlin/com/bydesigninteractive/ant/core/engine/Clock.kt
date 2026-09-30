package com.bydesigninteractive.ant.core.engine

/** Time for the simulation thread; tests substitute a fake. */
interface Clock {
    fun nanos(): Long

    fun sleepNanos(nanos: Long)
}

object SystemClock : Clock {
    override fun nanos(): Long = System.nanoTime()

    override fun sleepNanos(nanos: Long) {
        if (nanos > 0) Thread.sleep(nanos / 1_000_000L, (nanos % 1_000_000L).toInt())
    }
}

package com.bydesigninteractive.ant.core.engine

import com.bydesigninteractive.ant.sim.scenario.Scenarios
import kotlin.test.Test
import kotlin.test.assertTrue

class BenchmarkTest {
    @Test
    fun costMeasuresTheRequestedTicks() {
        val r = Benchmark.cost(Scenarios.starter(1), warmupTicks = 20, measureTicks = 40)
        println(r.line)
        assertTrue(r.line.startsWith("BENCH cost ants 80 ticks 40:"), r.line)
        assertTrue(r.msP50 <= r.msP99 && r.msP99 <= r.msMax, r.line)
        assertTrue(r.line.contains("surfaceAnts"), r.line)
    }

    @Test
    fun aSmallColonyHoldsFourTimesSpeed() {
        val r = Benchmark.speed(Scenarios.starter(1), speed = 4f, seconds = 12f)
        println(r.line)
        assertTrue(r.ticksPerSecond > 60f, r.line)
        assertTrue(r.line.startsWith("BENCH speed 4x ants 80:"), r.line)
    }
}

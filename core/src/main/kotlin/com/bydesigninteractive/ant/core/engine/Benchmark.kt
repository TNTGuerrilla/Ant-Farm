package com.bydesigninteractive.ant.core.engine

import com.bydesigninteractive.ant.sim.TickProfile
import com.bydesigninteractive.ant.sim.World
import java.util.Locale

/** What [Benchmark.cost] measured: ms per tick (step plus publish) and the full report line. */
data class CostResult(val msAvg: Double, val msP50: Double, val msP99: Double, val msMax: Double, val line: String)

/** What [Benchmark.speed] achieved, and the full report line. */
data class SpeedResult(val ticksPerSecond: Float, val line: String)

/**
 * Headless benchmarks of the simulation, shared by the desktop tests and the TV's
 * BenchmarkActivity. [cost] steps a world as fast as it can on the calling thread, publishing
 * after each tick as the app does, and reports where the time goes. [speed] runs a world through
 * a [SimRunner] at a speed setting and reports the rate it achieved.
 */
object Benchmark {
    fun cost(world: World, warmupTicks: Int, measureTicks: Int): CostResult {
        require(measureTicks > 0) { "measureTicks must be positive" }
        val published = Published(world)
        val stats = TickStats()
        val schedule = TickSchedule(intervalFor(1f))
        repeat(warmupTicks) {
            world.step()
            published.publish(world, System.nanoTime(), stats, schedule)
        }
        val profile = TickProfile()
        world.profile = profile
        val times = LongArray(measureTicks)
        for (i in 0 until measureTicks) {
            val t0 = System.nanoTime()
            world.step()
            val t1 = System.nanoTime()
            published.publish(world, t1, stats, schedule)
            val t2 = System.nanoTime()
            profile.publishNanos += t2 - t1
            times[i] = t2 - t0
        }
        world.profile = null
        val avg = times.average() / 1e6
        times.sort()
        val p50 = times[percentileIndex(measureTicks, 0.5)] / 1e6
        val p99 = times[percentileIndex(measureTicks, 0.99)] / 1e6
        val max = times[measureTicks - 1] / 1e6
        val rt = Runtime.getRuntime()
        val heapMb = (rt.totalMemory() - rt.freeMemory()) / 1_048_576.0
        val line = String.format(
            Locale.ROOT,
            "BENCH cost ants %d ticks %d: ms/tick avg %.2f p50 %.2f p99 %.2f max %.2f; %s; trail blocks %d scent blocks %d heap %.0f MB",
            world.ants.size, measureTicks, avg, p50, p99, max, profile.summary(),
            world.surface.trail.blockCount(), world.surface.homeScent.blockCount(), heapMb,
        )
        return CostResult(avg, p50, p99, max, line)
    }

    /** Runs [world] through a [SimRunner] at [speed] for [seconds] of real time (at least 12, so the 10 s statistics window fills). */
    fun speed(world: World, speed: Float, seconds: Float): SpeedResult {
        require(seconds >= 12f) { "seconds must be at least 12" }
        val published = Published(world)
        val runner = SimRunner(world, published)
        runner.send(Command.SetSpeed(speed))
        runner.start()
        Thread.sleep((seconds * 1000f).toLong())
        val hud = published.hud
        runner.stop()
        val failed = published.failed?.let { " FAILED $it" } ?: ""
        val line = String.format(
            Locale.ROOT,
            "BENCH speed %.0fx ants %d: ticks/s %.1f (target %.1f) ms/tick avg %.2f max %.2f resets %d%s",
            speed, world.ants.size, hud.ticksPerSecond, 20f * speed, hud.msPerTickAvg, hud.msPerTickMax, hud.resets, failed,
        )
        return SpeedResult(hud.ticksPerSecond, line)
    }

    private fun percentileIndex(n: Int, q: Double): Int = ((n - 1) * q).toInt().coerceIn(0, n - 1)
}

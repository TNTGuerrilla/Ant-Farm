package com.bydesigninteractive.ant.core.engine

import com.badlogic.gdx.Gdx
import com.bydesigninteractive.ant.core.APP_LOG_TAG
import com.bydesigninteractive.ant.sim.World
import java.util.concurrent.ConcurrentLinkedQueue

/** Requests from the render thread, applied between ticks. */
sealed interface Command {
    data class SetSpeed(val speed: Float) : Command
    data object Pause : Command
    data object Resume : Command
    data class Focus(val x: Float, val y: Float) : Command
}

/**
 * Runs the [World] on its own thread, the way Minecraft runs its integrated server: a fixed tick
 * rate ([TickSchedule]), a snapshot published after every tick, and commands applied only between
 * ticks. [tickLimit] ends the thread after that many ticks (tests).
 */
class SimRunner(
    private val world: World,
    private val published: Published,
    private val clock: Clock = SystemClock,
    private val tickLimit: Long = Long.MAX_VALUE,
) {
    private val commands = ConcurrentLinkedQueue<Command>()
    private val schedule = TickSchedule(intervalFor(1f))
    private val stats = TickStats()
    private var paused = false
    private var thread: Thread? = null

    @Volatile private var running = false

    /** Starts the thread. A second call while it is alive does nothing. */
    fun start() {
        if (thread?.isAlive == true) return
        running = true
        thread = Thread(::loop, "simulation").apply {
            isDaemon = true
            start()
        }
    }

    fun send(command: Command) {
        commands.add(command)
    }

    /** Ends the thread after its current tick and waits for it. */
    fun stop() {
        running = false
        val t = thread
        t?.join(STOP_WAIT_MS)
        if (t != null && t.isAlive) log("simulation thread did not stop within $STOP_WAIT_MS ms")
        thread = null
    }

    /** Waits up to [millis] for the thread to end (for runs with a tick limit). */
    fun join(millis: Long) {
        thread?.join(millis)
    }

    private fun log(message: String) {
        Gdx.app?.log(APP_LOG_TAG, message)
    }

    private fun loop() {
        try {
            runLoop()
        } catch (e: InterruptedException) {
            // asked to end
        } catch (e: Throwable) {
            published.failed = e
            Gdx.app?.error(APP_LOG_TAG, "simulation thread failed at tick ${world.tick}", e)
        }
    }

    private fun runLoop() {
        schedule.start(clock.nanos())
        published.publish(world, clock.nanos(), stats, schedule)
        while (running && world.tick < tickLimit) {
            applyCommands()
            if (paused) {
                clock.sleepNanos(IDLE_NANOS)
                continue
            }
            val wait = schedule.waitNanos(clock.nanos())
            if (wait > 0) {
                clock.sleepNanos(minOf(wait, IDLE_NANOS))
                continue
            }
            val t0 = clock.nanos()
            world.step()
            val t1 = clock.nanos()
            stats.record(t1 - t0, t1)
            published.publish(world, t1, stats, schedule)
            val resetsBefore = schedule.resets
            schedule.ticked(clock.nanos())
            if (schedule.resets > resetsBefore) {
                log("tick schedule reset to now at tick ${world.tick} (resets: ${schedule.resets})")
            }
        }
    }

    private fun applyCommands() {
        while (true) {
            when (val c = commands.poll() ?: return) {
                is Command.SetSpeed -> if (c.speed > 0f) schedule.setInterval(intervalFor(c.speed), clock.nanos())
                Command.Pause -> {
                    paused = true
                    published.paused = true
                }
                Command.Resume -> if (paused) {
                    paused = false
                    published.paused = false
                    schedule.start(clock.nanos())
                }
                is Command.Focus -> published.setFocus(c.x, c.y)
            }
        }
    }

    private companion object {
        const val IDLE_NANOS = 2_000_000L
        const val STOP_WAIT_MS = 2000L
    }
}

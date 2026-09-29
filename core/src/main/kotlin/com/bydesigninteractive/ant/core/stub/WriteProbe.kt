package com.bydesigninteractive.ant.core.stub

import com.bydesigninteractive.ant.sim.persist.AtomicFile
import java.io.File
import java.nio.ByteBuffer
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.random.Random

/**
 * Stands in for a world snapshot: writes [sizeBytes] atomically on a low-priority background
 * thread, so the renderer can check whether a save of that size causes visible hitches.
 */
class WriteProbe(private val dir: File, private val sizeBytes: Int) {
    class Result(val index: Int, val timing: AtomicFile.Timing?, val error: String?)

    private val executor: ExecutorService = Executors.newSingleThreadExecutor { job ->
        Thread(job, "snapshot-writer").apply {
            isDaemon = true
            priority = Thread.MIN_PRIORITY
        }
    }
    private var buffer: ByteBuffer? = null
    private var started = 0

    @Volatile
    var busy = false
        private set

    @Volatile
    var last: Result? = null
        private set

    /** Starts a write unless one is already running. Call from one thread only. */
    fun start(): Boolean {
        if (busy) return false
        busy = true
        val index = ++started
        executor.execute {
            last = try {
                Result(index, AtomicFile.write(File(dir, "probe.bin").toPath(), data()), null)
            } catch (e: Exception) {
                Result(index, null, e.toString())
            }
            busy = false
        }
        return true
    }

    /** Built on the writer thread the first time, so filling 20 MB never stalls a frame. */
    private fun data(): ByteBuffer = buffer ?: ByteBuffer.allocateDirect(sizeBytes).also {
        dir.mkdirs()
        it.put(Random(42).nextBytes(sizeBytes))
        it.flip()
        buffer = it
    }

    fun dispose() {
        executor.shutdownNow()
    }
}

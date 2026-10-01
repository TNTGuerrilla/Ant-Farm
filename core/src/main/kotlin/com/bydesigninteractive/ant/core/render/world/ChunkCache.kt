package com.bydesigninteractive.ant.core.render.world

import com.bydesigninteractive.ant.sim.world.CHUNKS
import com.bydesigninteractive.ant.sim.world.sdf.Blob
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.LinkedBlockingDeque
import java.util.concurrent.atomic.AtomicLong

/** One built chunk: vertex arrays for the ground (with specks) and rocks, and grass instances. */
class ChunkResult(val cx: Int, val cy: Int, val ground: MeshData, val rocks: MeshData, val grass: FloatArray, val version: Long)

/**
 * Builds chunks on one background thread. The render thread requests a chunk with immutable
 * published data (its spoil array, its neighbours' spoil arrays, its rock list and the rocks
 * reaching in from its neighbours) and polls
 * finished results to upload. Requests are applied in order, so a chunk's spoil mirror is current
 * before it is rebuilt; a result is tagged with its request's version, and the renderer keeps only
 * the newest per chunk.
 */
class ChunkCache(seed: Long) {
    private class Job(val cx: Int, val cy: Int, val spoil: FloatArray?, val neighbours: Array<FloatArray?>?, val rocks: List<Blob>, val grassRocks: List<Blob>, val version: Long)

    private val mesher = ChunkMesher(seed)
    private val grass = GrassField(seed)
    private val jobs = LinkedBlockingDeque<Job>()
    private val results = ConcurrentLinkedQueue<ChunkResult>()
    private val versions = AtomicLong()
    @Volatile private var running = true
    private val thread = Thread(::loop, "chunk-mesher").apply {
        isDaemon = true
        start()
    }

    /**
     * Queues chunk (cx, cy) for building with its published spoil (immutable, may be null) and
     * rocks. [neighbours], if given, holds the spoil of the 3 by 3 block around the chunk at index
     * (dx + 1) + (dy + 1) * 3 (the centre entry is ignored in favour of [spoil]); the edge vertices
     * read it, so seams under spoil stay watertight. A null entry means no spoil there. [grassRocks]
     * are the rocks grass must stay out of: the chunk's own plus any neighbour's that reach into it
     * (by default just [rocks]). Returns the request's version.
     */
    fun request(cx: Int, cy: Int, spoil: FloatArray?, rocks: List<Blob>, neighbours: Array<FloatArray?>? = null, grassRocks: List<Blob> = rocks): Long {
        val v = versions.incrementAndGet()
        jobs.addLast(Job(cx, cy, spoil, neighbours, rocks, grassRocks, v))
        return v
    }

    /** Up to [maxResults] finished chunks, oldest first. */
    fun poll(maxResults: Int): List<ChunkResult> {
        val out = ArrayList<ChunkResult>(maxResults)
        while (out.size < maxResults) out += results.poll() ?: break
        return out
    }

    fun close() {
        running = false
        thread.interrupt()
        thread.join(2000)
    }

    private fun loop() {
        try {
            while (running) {
                val job = jobs.takeFirst()
                if (job.cx !in 0 until CHUNKS || job.cy !in 0 until CHUNKS) continue
                job.neighbours?.let { n ->
                    for (dy in -1..1) for (dx in -1..1) {
                        val nx = job.cx + dx
                        val ny = job.cy + dy
                        if ((dx == 0 && dy == 0) || nx !in 0 until CHUNKS || ny !in 0 until CHUNKS) continue
                        mesher.setSpoil(nx, ny, n[(dx + 1) + (dy + 1) * 3])
                    }
                }
                mesher.setSpoil(job.cx, job.cy, job.spoil)
                results += ChunkResult(job.cx, job.cy, mesher.ground(job.cx, job.cy), mesher.rocks(job.rocks), grass.tufts(job.cx, job.cy, mesher, job.grassRocks), job.version)
            }
        } catch (e: InterruptedException) {
            // closing
        }
    }
}

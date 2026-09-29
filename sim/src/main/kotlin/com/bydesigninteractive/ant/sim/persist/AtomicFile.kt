package com.bydesigninteractive.ant.sim.persist

import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.nio.file.StandardOpenOption

/**
 * Whole-file writes that never leave a half-written file behind: the data goes to
 * `<name>.tmp`, is flushed to the device, then renamed over the target. A crash at any point
 * leaves either the old file or the new one.
 */
object AtomicFile {
    class Timing(val writeNanos: Long, val syncNanos: Long, val renameNanos: Long) {
        val totalNanos: Long get() = writeNanos + syncNanos + renameNanos
    }

    /** Writes [data] from its position to its limit. The buffer's own position is not moved. */
    fun write(target: Path, data: ByteBuffer): Timing {
        val tmp = target.resolveSibling("${target.fileName}.tmp")
        val source = data.duplicate()
        val start = System.nanoTime()
        val written: Long
        FileChannel.open(
            tmp,
            StandardOpenOption.CREATE,
            StandardOpenOption.WRITE,
            StandardOpenOption.TRUNCATE_EXISTING,
        ).use { channel ->
            while (source.hasRemaining()) channel.write(source)
            written = System.nanoTime()
            channel.force(true)
        }
        val synced = System.nanoTime()
        Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
        val renamed = System.nanoTime()
        return Timing(written - start, synced - written, renamed - synced)
    }
}

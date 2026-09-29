package com.bydesigninteractive.ant.sim.persist

import java.nio.ByteBuffer
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.readBytes
import kotlin.io.path.writeBytes
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AtomicFileTest {
    private val dir: Path = Files.createTempDirectory("atomic")

    @AfterTest
    fun cleanUp() {
        dir.toFile().deleteRecursively()
    }

    @Test
    fun writesTheWholeBuffer() {
        val target = dir.resolve("snapshot.bin")
        val bytes = ByteArray(300_000) { it.toByte() }
        AtomicFile.write(target, ByteBuffer.wrap(bytes))
        assertContentEquals(bytes, target.readBytes())
    }

    @Test
    fun replacesAnExistingFileAndLeavesNoTemporary() {
        val target = dir.resolve("snapshot.bin")
        target.writeBytes(ByteArray(10) { 7 })
        AtomicFile.write(target, ByteBuffer.wrap(byteArrayOf(1, 2, 3)))
        assertContentEquals(byteArrayOf(1, 2, 3), target.readBytes())
        assertEquals(listOf(target), dir.listDirectoryEntries())
    }

    @Test
    fun leavesTheBufferPositionUntouched() {
        val buffer = ByteBuffer.wrap(ByteArray(64))
        AtomicFile.write(dir.resolve("a.bin"), buffer)
        assertEquals(0, buffer.position())
    }

    @Test
    fun reportsEachPhase() {
        val timing = AtomicFile.write(dir.resolve("a.bin"), ByteBuffer.wrap(ByteArray(1024)))
        assertTrue(timing.writeNanos >= 0 && timing.syncNanos >= 0 && timing.renameNanos >= 0)
        assertEquals(timing.writeNanos + timing.syncNanos + timing.renameNanos, timing.totalNanos)
        assertFalse(dir.resolve("a.bin.tmp").exists())
    }
}

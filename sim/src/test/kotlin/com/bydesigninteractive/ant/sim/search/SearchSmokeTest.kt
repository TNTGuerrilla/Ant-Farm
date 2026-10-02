package com.bydesigninteractive.ant.sim.search

import com.bydesigninteractive.ant.sim.brain.Genome
import java.io.File
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SearchSmokeTest {
    private fun config(dir: File, workers: Int, full: Int) = SearchConfig(
        generations = 2, population = 4, keep = 2, seeds = listOf(1L), minutes = 2,
        viabilityMinute = 99, workers = workers, fullCheck = full, gruterSeeds = listOf(1L),
        experimentSeeds = listOf(1L), fullScale = 0.05f, requireViable = false, outDir = dir,
    )

    @Test
    fun aTinySearchRunsAndWritesItsReport() {
        val dir = Files.createTempDirectory("search").toFile()
        val out = Search.run(config(dir, workers = 2, full = 1)) {}
        assertEquals(2, out.generations.size)
        assertEquals(4, out.generations[0].size) // the seed and 3 mutants
        assertEquals(4, out.generations[1].size)
        assertEquals(8, out.ranking.size)
        assertEquals(1, out.full.size)
        val report = File(dir, "report.md").readText()
        for (heading in listOf("## Winner", "## Generations", "## Full check", "## Every variant")) {
            assertTrue(report.contains(heading), "report lacks $heading")
        }
        assertTrue(report.contains("foragerShare"), "report lacks the unscored forager share")
        Genome.load(File(dir, "gen-1-best.genome").toPath())
        if (out.winner == null) {
            assertTrue(report.contains("No variant passed the Gruter test"))
            assertFalse(File(dir, "winner.genome").exists())
        } else {
            Genome.load(File(dir, "winner.genome").toPath())
        }
        dir.deleteRecursively()
    }

    /** No full check means no winner: the search must still finish and say so (the seed brain fails Gruter today). */
    @Test
    fun aSearchWithNoWinnerSaysSo() {
        val dir = Files.createTempDirectory("search").toFile()
        val out = Search.run(config(dir, workers = 2, full = 0)) {}
        assertNull(out.winner)
        val report = File(dir, "report.md").readText()
        assertTrue(report.contains("No variant passed the Gruter test"), "report does not say there is no winner")
        assertTrue(report.contains("No variant was full-checked."))
        assertFalse(File(dir, "winner.genome").exists())
        dir.deleteRecursively()
    }

    @Test
    fun oneWorkerOrTwoGiveTheSameRanking() {
        val a = Files.createTempDirectory("search").toFile()
        val b = Files.createTempDirectory("search").toFile()
        val one = Search.run(config(a, workers = 1, full = 0)) {}
        val two = Search.run(config(b, workers = 2, full = 0)) {}
        assertEquals(one.ranking.map { it.variant.id to it.score }, two.ranking.map { it.variant.id to it.score })
        a.deleteRecursively()
        b.deleteRecursively()
    }

    @Test
    fun aMutantKeepsTheLayoutAndChangesTheWeights() {
        val parent = com.bydesigninteractive.ant.sim.brain.SeedBrain.genome()
        val child = Search.mutate(parent, java.util.Random(1), 0.05f, 0.02f)
        assertEquals(parent.layout, child.layout)
        assertEquals(parent.weights.size, child.weights.size)
        assertTrue(parent.weights.indices.count { parent.weights[it] != child.weights[it] } > parent.weights.size * 0.9)
    }
}

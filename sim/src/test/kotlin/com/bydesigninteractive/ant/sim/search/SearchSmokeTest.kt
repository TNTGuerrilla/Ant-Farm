package com.bydesigninteractive.ant.sim.search

import com.bydesigninteractive.ant.sim.brain.Genome
import java.io.File
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SearchSmokeTest {
    /** Gate terms on the best variant of each batch only, at a twentieth of their length, to keep the test short. */
    private fun config(dir: File, workers: Int, full: Int) = SearchConfig(
        generations = 2, population = 4, keep = 2, seeds = listOf(1L), minutes = 2,
        viabilityMinute = 99, workers = workers, fullCheck = full, gruterSeeds = listOf(1L),
        experimentSeeds = listOf(1L), fullScale = 0.05f, requireViable = false, gateTop = 1, gateScale = 0.05f, outDir = dir,
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
        assertNotNull(out.baseline, "the seed brain is full-checked as the baseline")
        assertEquals("seed", out.baseline!!.scored.variant.id)
        // With gateTop 1 the best of each screening batch (the seed, generation 0, generation 1) gets the gate terms.
        assertEquals(3, out.ranking.count { it.gates != null })
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
    fun oneWorkerOrTwoGiveTheSameFullCheck() {
        val a = Files.createTempDirectory("search").toFile()
        val b = Files.createTempDirectory("search").toFile()
        fun lines(o: SearchOutcome) = (o.full + listOfNotNull(o.baseline)).map { fc ->
            listOf(fc.scored.variant.id, fc.gruter.line()) + fc.experiments.map { it.line() }
        }
        val one = Search.run(config(a, workers = 1, full = 1)) {}
        val two = Search.run(config(b, workers = 2, full = 1)) {}
        assertEquals(lines(one), lines(two))
        assertEquals(one.winner?.scored?.variant?.id, two.winner?.scored?.variant?.id)
        a.deleteRecursively()
        b.deleteRecursively()
    }

    @Test
    fun onlyTheBestGetTheGateTermsWithAGateTop() {
        val dir = Files.createTempDirectory("search").toFile()
        val c = SearchConfig(
            generations = 0, population = 4, keep = 2, seeds = listOf(1L), minutes = 2, viabilityMinute = 99,
            workers = 2, fullCheck = 0, requireViable = false, gateTop = 2, gateScale = 0.05f, outDir = dir,
        )
        val variants = listOf(Variant("seed", null, com.bydesigninteractive.ant.sim.brain.SeedBrain.genome())) +
            (0 until 3).map { Variant("m$it", "seed", Search.mutate(com.bydesigninteractive.ant.sim.brain.SeedBrain.genome(), java.util.Random(it.toLong()), 0.05f, 0.02f)) }
        val pool = java.util.concurrent.Executors.newFixedThreadPool(2)
        try {
            val scored = Search.screen(variants, c, pool)
            assertEquals(2, scored.count { it.gates != null })
            val best = scored.sortedWith(compareBy<Scored>({ it.baseScore }, { it.variant.id })).take(2)
            assertTrue(best.all { it.gates != null })
            assertTrue(scored.filter { it.gates != null }.all { it.score >= it.baseScore })
        } finally {
            pool.shutdown()
            dir.deleteRecursively()
        }
    }

    @Test
    fun aMutantKeepsTheLayoutAndChangesTheWeights() {
        val parent = com.bydesigninteractive.ant.sim.brain.SeedBrain.genome()
        val child = Search.mutate(parent, java.util.Random(1), 0.05f, 0.02f)
        assertEquals(parent.layout, child.layout)
        assertEquals(parent.weights.size, child.weights.size)
        // Every weight but those the mirror symmetry holds at zero is free to change.
        val mirror = Search.mirrorFor(parent)!!
        val changed = parent.weights.indices.count { parent.weights[it] != child.weights[it] }
        assertTrue(changed > (parent.weights.size - mirror.zeros) * 0.9, "$changed of ${parent.weights.size} changed")
        assertEquals(0f, mirror.asymmetry(child.weights))
    }
}

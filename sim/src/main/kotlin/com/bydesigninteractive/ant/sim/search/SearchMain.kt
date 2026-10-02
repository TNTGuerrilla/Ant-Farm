package com.bydesigninteractive.ant.sim.search

import java.io.File
import kotlin.system.exitProcess

/**
 * Runs the realism search. Options (all optional): --generations 8 --population 40 --keep 8
 * --seeds 1,2,3 --minutes 20 --viability-minute 10 --sigma 0.05 --floor 0.02 --workers N
 * --search-seed 1 --full 5 --full-scale 1 --out build/search. Exits 0 with a winner, 2 without.
 */
fun main(args: Array<String>) {
    val known = setOf(
        "generations", "population", "keep", "seeds", "minutes", "viability-minute", "sigma",
        "floor", "workers", "search-seed", "full", "full-scale", "out",
    )
    val o = HashMap<String, String>()
    var i = 0
    while (i < args.size) {
        val key = args[i].removePrefix("--")
        require(args[i].startsWith("--") && key in known) { "unknown option ${args[i]}; known: $known" }
        require(i + 1 < args.size) { "${args[i]} needs a value" }
        o[key] = args[i + 1]
        i += 2
    }
    val d = SearchConfig()
    val c = SearchConfig(
        generations = o["generations"]?.toInt() ?: d.generations,
        population = o["population"]?.toInt() ?: d.population,
        keep = o["keep"]?.toInt() ?: d.keep,
        seeds = o["seeds"]?.split(',')?.map { it.trim().toLong() } ?: d.seeds,
        minutes = o["minutes"]?.toInt() ?: d.minutes,
        viabilityMinute = o["viability-minute"]?.toInt() ?: d.viabilityMinute,
        sigma = o["sigma"]?.toFloat() ?: d.sigma,
        floor = o["floor"]?.toFloat() ?: d.floor,
        workers = o["workers"]?.toInt() ?: d.workers,
        searchSeed = o["search-seed"]?.toLong() ?: d.searchSeed,
        fullCheck = o["full"]?.toInt() ?: d.fullCheck,
        fullScale = o["full-scale"]?.toFloat() ?: d.fullScale,
        outDir = o["out"]?.let { File(it) } ?: d.outDir,
    )
    println("search: ${c.workers} workers, output in ${c.outDir.absolutePath}")
    val outcome = Search.run(c)
    println("report: ${File(c.outDir, "report.md").absolutePath}")
    exitProcess(if (outcome.winner != null) 0 else 2)
}

package com.bydesigninteractive.ant.sim.search

import java.io.File
import kotlin.system.exitProcess

/**
 * Runs the realism search. Options (all optional): --generations 8 --population 40 --keep 8
 * --seeds 1,2,3 --minutes 20 --viability-minute 10 --sigma 0.05 --floor 0.02 --workers N
 * --search-seed 1 --full 5 --gruter-seeds 1,2,3,4,5,6,7,8 --experiment-seeds 1,2,3,4 --full-scale 1
 * --require-viable true --gates true --gate-top 0 --gate-scale 1 --out build/search. Exits 0 with a
 * winner, 2 without.
 */
fun main(args: Array<String>) {
    val known = setOf(
        "generations", "population", "keep", "seeds", "minutes", "viability-minute", "sigma",
        "floor", "workers", "search-seed", "full", "gruter-seeds", "experiment-seeds", "full-scale",
        "require-viable", "gates", "gate-top", "gate-scale", "out",
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
    fun seeds(v: String): List<Long> = v.split(',').map { it.trim().toLong() }
    fun flag(v: String): Boolean = when (v.lowercase()) {
        "true", "yes", "1" -> true
        "false", "no", "0" -> false
        else -> throw IllegalArgumentException("not a true or false value: $v")
    }
    val d = SearchConfig()
    val c = SearchConfig(
        generations = o["generations"]?.toInt() ?: d.generations,
        population = o["population"]?.toInt() ?: d.population,
        keep = o["keep"]?.toInt() ?: d.keep,
        seeds = o["seeds"]?.let(::seeds) ?: d.seeds,
        minutes = o["minutes"]?.toInt() ?: d.minutes,
        viabilityMinute = o["viability-minute"]?.toInt() ?: d.viabilityMinute,
        sigma = o["sigma"]?.toFloat() ?: d.sigma,
        floor = o["floor"]?.toFloat() ?: d.floor,
        workers = o["workers"]?.toInt() ?: d.workers,
        searchSeed = o["search-seed"]?.toLong() ?: d.searchSeed,
        fullCheck = o["full"]?.toInt() ?: d.fullCheck,
        gruterSeeds = o["gruter-seeds"]?.let(::seeds) ?: d.gruterSeeds,
        experimentSeeds = o["experiment-seeds"]?.let(::seeds) ?: d.experimentSeeds,
        fullScale = o["full-scale"]?.toFloat() ?: d.fullScale,
        requireViable = o["require-viable"]?.let(::flag) ?: d.requireViable,
        gates = o["gates"]?.let(::flag) ?: d.gates,
        gateTop = o["gate-top"]?.toInt() ?: d.gateTop,
        gateScale = o["gate-scale"]?.toFloat() ?: d.gateScale,
        outDir = o["out"]?.let { File(it) } ?: d.outDir,
    )
    println("search: ${c.workers} workers, output in ${c.outDir.absolutePath}")
    val outcome = Search.run(c)
    println("report: ${File(c.outDir, "report.md").absolutePath}")
    exitProcess(if (outcome.winner != null) 0 else 2)
}

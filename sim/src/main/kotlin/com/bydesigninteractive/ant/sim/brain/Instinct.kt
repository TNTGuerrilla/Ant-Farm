package com.bydesigninteractive.ant.sim.brain

/**
 * The instinct brain new colonies start from: the genome the realism search shipped as the
 * resource [RESOURCE] (M2a Task 11), or [SeedBrain] when there is none or it does not fit the
 * current input layout and outputs.
 */
object Instinct {
    const val RESOURCE = "/com/bydesigninteractive/ant/sim/brain/instinct.genome"

    private var from = "seed"

    val genome: Genome by lazy { load() }

    /** Where [genome] came from: "resource", "seed", or "seed (resource rejected: reason)". */
    val source: String
        get() {
            genome
            return from
        }

    private fun load(): Genome {
        val stream = Instinct::class.java.getResourceAsStream(RESOURCE) ?: return SeedBrain.genome()
        val text = stream.bufferedReader(Charsets.UTF_8).use { it.readText() }
        val g = try {
            Genome.read(text)
        } catch (e: IllegalArgumentException) {
            from = "seed (resource rejected: ${e.message})"
            return SeedBrain.genome()
        }
        if (g.layout != Senses.LAYOUT || g.inputs != Senses.COUNT || g.outputs != Outputs.COUNT) {
            from = "seed (resource rejected: layout ${g.layout}, ${g.inputs} inputs, ${g.outputs} outputs)"
            return SeedBrain.genome()
        }
        from = "resource"
        return g
    }
}

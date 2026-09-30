package com.bydesigninteractive.ant.sim.world

enum class Material {
    AIR, SOIL, STONE, CLAY, WATER;

    /** Whether an ant can dig this material out. */
    val diggable: Boolean get() = this == SOIL || this == CLAY

    companion object {
        private val byCode = entries.toTypedArray()

        fun of(code: Int): Material = byCode[code]
    }
}

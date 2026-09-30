package com.bydesigninteractive.ant.sim.world

enum class FoodKind { HONEYDEW, PREY }

/**
 * Something ants collect. Honeydew plants never run out; a prey item holds [loads] trips' worth.
 * [quality] is how full a forager can fill its crop there, from 0 to 1.
 */
class FoodSource(
    val id: Int,
    val kind: FoodKind,
    val x: Float,
    val y: Float,
    val radius: Float,
    var quality: Float,
    var loads: Int = Int.MAX_VALUE,
)

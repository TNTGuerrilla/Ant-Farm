package com.bydesigninteractive.ant.sim.world

import com.bydesigninteractive.ant.sim.world.sdf.Blob
import com.bydesigninteractive.ant.sim.world.sdf.Stem

enum class FoodKind { HONEYDEW, PREY }

/**
 * Something ants collect. Honeydew plants never run out; a prey item holds [loads] trips' worth.
 * [quality] is how full a forager can fill its crop there, from 0 to 1.
 *
 * On the 3D surface, food at height [z] can have a solid [body] (an aphid cluster, prey lump or
 * feeder) and an aphid plant also has a [stem] from [stemBase] to a little above the cluster.
 * [radius] is the feeding reach: horizontal for food on the ground, 3D from the cluster for a plant.
 */
class FoodSource(
    val id: Int,
    val kind: FoodKind,
    val x: Float,
    val y: Float,
    val radius: Float,
    var quality: Float,
    var loads: Int = Int.MAX_VALUE,
    val z: Float = 0f,
    val bodyRadius: Float = 0f,
    val stemRadius: Float = 0f,
    val stemBase: Float = 0f,
) {
    val body: Blob? = if (bodyRadius > 0f) Blob(x, y, z, bodyRadius, bodyRadius, bodyRadius) else null
    val stem: Stem? = if (stemRadius > 0f) Stem(x, y, stemBase, z + STEM_ABOVE_CLUSTER, stemRadius) else null
    val hasStem: Boolean get() = stem != null

    private companion object {
        const val STEM_ABOVE_CLUSTER = 30f
    }
}

package com.bydesigninteractive.ant.sim.world

import com.bydesigninteractive.ant.sim.world.sdf.Blob
import com.bydesigninteractive.ant.sim.world.sdf.Stem
import kotlin.math.PI
import kotlin.math.max
import kotlin.math.roundToInt

enum class FoodKind { HONEYDEW, PREY }

/**
 * Something ants collect. Honeydew plants never run out; a prey item holds [loads] trips' worth.
 * [quality] is how full a forager can fill its crop there, from 0 to 1.
 *
 * On the 3D surface, food at height [z] can have a solid [body] (an aphid cluster, prey lump or
 * feeder) and an aphid plant also has a [stem] from [stemBase] to a little above the cluster.
 * [radius] is the feeding reach: horizontal for food on the ground, 3D from the cluster for a plant.
 *
 * Only [capacity] workers can feed at once (M2a Task 11a, the owner's decision of 2026-10-02):
 * ants feeding at a source cover it, and a forager that arrives when every place is taken cannot
 * feed. [feeders] counts the ants feeding now; `Primitives` takes and releases the places.
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

    /** How many workers can feed here at once: [spots] of its modelled size. */
    val capacity: Int = spots(bodyRadius, stemRadius, radius)

    /** Workers feeding here now, at most [capacity]. */
    var feeders: Int = 0
        internal set

    /** True while every feeding place is taken. */
    val full: Boolean get() = feeders >= capacity

    companion object {
        private const val STEM_ABOVE_CLUSTER = 30f

        /**
         * The width of food edge one feeding worker takes (mm). A Lasius niger worker's head is
         * about 1.1 mm wide (1.08 to 1.15 mm, Kramer 2016; research notes 01), and a feeding worker
         * faces the food with its forelegs braced on either side of the head, so it needs a little
         * under twice its head width: 2 mm, half the 4 mm body of the modelled 3 to 5 mm workers.
         * Derived, not measured (tag G).
         */
        const val SPOT_WIDTH = 2f

        /** The ground one worker standing on food covers (mm^2): [SPOT_WIDTH] times a 4 mm body. Derived (tag G). */
        const val SPOT_AREA = SPOT_WIDTH * 4f

        /**
         * The feeding places of a source from its modelled size, at least 1. Food on the ground
         * (a feeder's drop or a prey item, [bodyRadius] mm) is reached from its edge: its
         * circumference over [SPOT_WIDTH]. An aphid cluster on a stem is walked over: the cluster's
         * surface (a sphere of [bodyRadius]) over [SPOT_AREA]. A source with no body (tests only)
         * uses its feeding reach [radius] as the edge. Feeder of 8 mm: 25; prey of 3 to 8 mm: 9 to
         * 25; aphid cluster of 8 mm: 101.
         */
        fun spots(bodyRadius: Float, stemRadius: Float, radius: Float): Int {
            val n = when {
                stemRadius > 0f && bodyRadius > 0f -> 4.0 * PI * bodyRadius * bodyRadius / SPOT_AREA
                bodyRadius > 0f -> 2.0 * PI * bodyRadius / SPOT_WIDTH
                else -> 2.0 * PI * radius / SPOT_WIDTH
            }
            return max(1, n.roundToInt())
        }
    }
}

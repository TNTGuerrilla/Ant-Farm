package com.bydesigninteractive.ant.sim.world

import com.bydesigninteractive.ant.sim.scenario.Scenarios
import kotlin.test.Test
import kotlin.test.assertEquals

/** Feeding places from a source's modelled size (M2a Task 11a). */
class FoodTest {
    @Test
    fun feedingPlacesFollowTheFoodsSize() {
        // An experiment feeder drop of 8 mm: its 50 mm edge at 2 mm per worker.
        assertEquals(25, FoodSource.spots(bodyRadius = 8f, stemRadius = 0f, radius = 15f))
        // Prey items of 3 to 8 mm (SurfaceMap.placePrey).
        assertEquals(9, FoodSource.spots(bodyRadius = 3f, stemRadius = 0f, radius = 7f))
        assertEquals(25, FoodSource.spots(bodyRadius = 8f, stemRadius = 0f, radius = 12f))
        // An aphid cluster of 8 mm on a stem: its upper half at 8 mm^2 per worker.
        assertEquals(50, FoodSource.spots(bodyRadius = 8f, stemRadius = 2.5f, radius = 12f))
        // Never none.
        assertEquals(1, FoodSource.spots(bodyRadius = 0.1f, stemRadius = 0f, radius = 1f))
    }

    /** Gruter 2012's covered feeders: the same 8 mm drop, places set by the holes (8 per hole). */
    @Test
    fun aCoveredFeedersPlacesComeFromItsHoles() {
        assertEquals(72, Scenarios.GRUTER_LOW_CROWDING)
        assertEquals(8, Scenarios.GRUTER_HIGH_CROWDING)
        for (places in intArrayOf(Scenarios.GRUTER_LOW_CROWDING, Scenarios.GRUTER_HIGH_CROWDING)) {
            val w = Scenarios.gruter(1, foragers = 0, places = places)
            for (f in w.surface.foods) {
                assertEquals(places, f.capacity)
                assertEquals(8f, f.bodyRadius)
                assertEquals(15f, f.radius)
            }
        }
        assertEquals(25, Scenarios.gruter(1, foragers = 0).surface.foods.first().capacity)
    }
}

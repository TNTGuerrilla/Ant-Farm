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
        // An aphid cluster of 8 mm on a stem: its surface at 8 mm^2 per worker.
        assertEquals(101, FoodSource.spots(bodyRadius = 8f, stemRadius = 2.5f, radius = 12f))
        // Gruter 2012's two crowding conditions.
        val low = Scenarios.feederRadius(Scenarios.GRUTER_LOW_CROWDING)
        val high = Scenarios.feederRadius(Scenarios.GRUTER_HIGH_CROWDING)
        assertEquals(72, FoodSource.spots(bodyRadius = low, stemRadius = 0f, radius = low + 7f))
        assertEquals(8, FoodSource.spots(bodyRadius = high, stemRadius = 0f, radius = high + 7f))
        // Never none.
        assertEquals(1, FoodSource.spots(bodyRadius = 0.1f, stemRadius = 0f, radius = 1f))
    }
}

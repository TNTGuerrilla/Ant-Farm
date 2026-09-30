package com.bydesigninteractive.ant.core.render

import com.bydesigninteractive.ant.sim.ant.Ant
import com.bydesigninteractive.ant.sim.ant.Role
import kotlin.test.Test
import kotlin.test.assertEquals

class AntAnimatorTest {
    private fun ant(id: Int = 0, speed: Float = 0f) = Ant(id, Role.FORAGER, 1f, false).also { it.speed = speed }

    @Test
    fun standingAntsShowTheNeutralFrame() {
        val animator = AntAnimator()
        val a = ant(speed = 0f)
        animator.advance(listOf(a), 10f)
        assertEquals(1, animator.frame(a))
    }

    @Test
    fun walkingAntCyclesThroughQuarterStrides() {
        val animator = AntAnimator()
        val a = ant(speed = 3f)
        val seen = ArrayList<Int>()
        seen += animator.frame(a)
        repeat(4) {
            animator.advance(listOf(a), 0.25f) // speed 3 mm/s, stride 3 mm: a quarter stride
            seen += animator.frame(a)
        }
        assertEquals(listOf(0, 1, 2, 1, 0), seen)
    }

    @Test
    fun phaseIsProportionalToSpeedAndTime() {
        val slow = AntAnimator()
        val fast = AntAnimator()
        val a = ant(speed = 2f)
        val b = ant(speed = 4f)
        slow.advance(listOf(a), 0.3f)
        fast.advance(listOf(b), 0.3f)
        assertEquals(2f * slow.phase(a), fast.phase(b), 1e-5f)
        fast.advance(listOf(b), 0.3f)
        assertEquals(4f * slow.phase(a), fast.phase(b), 1e-5f)
    }

    @Test
    fun zeroSimulatedTimeDoesNotAdvance() {
        val animator = AntAnimator()
        val a = ant(speed = 5f)
        animator.advance(listOf(a), 0.2f)
        val before = animator.phase(a)
        animator.advance(listOf(a), 0f)
        assertEquals(before, animator.phase(a), 0f)
    }

    @Test
    fun growsForHighIds() {
        val animator = AntAnimator()
        val a = ant(id = 500, speed = 3f)
        animator.advance(listOf(a), 0.25f)
        assertEquals(0, animator.frame(ant(id = 900, speed = 3f))) // never advanced: phase 0
        assertEquals(1, animator.frame(a))
    }
}

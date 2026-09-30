package com.bydesigninteractive.ant.core.render

import com.bydesigninteractive.ant.sim.ant.Ant
import com.bydesigninteractive.ant.core.engine.AntPose
import com.bydesigninteractive.ant.sim.ant.Role
import com.bydesigninteractive.ant.sim.ant.Space
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

    @Test
    fun phaseStaysWrappedForLargeTimeAdvances() {
        val animator = AntAnimator()
        val a = ant(speed = 20f) // 20 mm/s
        animator.advance(listOf(a), 1e6f) // advance by 1 million seconds
        val phase = animator.phase(a)
        assert(phase >= 0f && phase < 1f) { "phase $phase must be in [0, 1)" }
        val frameBeforeQuarter = animator.frame(a)
        animator.advance(listOf(a), 0.0375f) // quarter stride: 0.75 mm at 20 mm/s
        val frameAfterQuarter = animator.frame(a)
        assert(frameBeforeQuarter != frameAfterQuarter) { "quarter stride should change frame" }
    }

    @Test
    fun legsAdvanceWithDistanceWalked() {
        val an = AntAnimator()
        val p = AntPose().apply { id = 3; speed = 20f; space = Space.SURFACE }
        an.observe(p)
        p.x += 0.75f // a quarter stride
        an.observe(p)
        assertEquals(0.25f, an.phase(3), 1e-5f)
    }

    @Test
    fun standingShowsTheNeutralFrame() {
        val an = AntAnimator()
        val p = AntPose().apply { id = 0; speed = 0f }
        an.observe(p)
        assertEquals(1, an.frame(p))
    }

    @Test
    fun aSpaceChangeDoesNotCountAsWalking() {
        val an = AntAnimator()
        val p = AntPose().apply { id = 1; speed = 20f; space = Space.SURFACE; x = 4000f }
        an.observe(p)
        p.space = Space.NEST
        p.x = 600f
        an.observe(p)
        assertEquals(0f, an.phase(1), 1e-6f)
    }
}

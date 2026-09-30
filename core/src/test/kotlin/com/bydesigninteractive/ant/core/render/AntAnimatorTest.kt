package com.bydesigninteractive.ant.core.render

import com.bydesigninteractive.ant.core.engine.AntPose
import com.bydesigninteractive.ant.sim.ant.Space
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class AntAnimatorTest {
    private fun pose(id: Int = 0, speed: Float = 20f) = AntPose().apply {
        this.id = id
        this.speed = speed
        space = Space.SURFACE
    }

    @Test
    fun walkingAntCyclesThroughQuarterStrides() {
        val animator = AntAnimator()
        val p = pose()
        animator.observe(p)
        val seen = ArrayList<Int>()
        seen += animator.frame(p)
        repeat(4) {
            p.x += 0.75f // stride 3 mm: a quarter stride
            animator.observe(p)
            seen += animator.frame(p)
        }
        assertEquals(listOf(0, 1, 2, 1, 0), seen)
    }

    @Test
    fun growsForHighIds() {
        val animator = AntAnimator()
        val p = pose(id = 500)
        animator.observe(p)
        p.x += 0.75f
        animator.observe(p)
        assertEquals(0, animator.frame(pose(id = 900))) // never observed: phase 0
        assertEquals(1, animator.frame(p))
    }

    @Test
    fun phaseStaysWrappedAfterAVeryLongWalk() {
        val animator = AntAnimator()
        val p = pose()
        animator.observe(p)
        p.x = 2e7f // 20 km in one step: millions of strides
        animator.observe(p)
        val phase = animator.phase(p.id)
        assertTrue(phase >= 0f && phase < 1f, "phase $phase must be in [0, 1)")
        val before = animator.frame(p)
        p.y += 0.75f // a quarter stride, along y so the large x does not cost precision
        animator.observe(p)
        assertNotEquals(before, animator.frame(p), "a quarter stride should change the frame")
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

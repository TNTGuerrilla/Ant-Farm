package com.bydesigninteractive.ant.sim.ant

/**
 * Every tunable constant of the ants' bodies and their world. Section numbers refer to the
 * simulation reference.
 *
 * Since M2a each ant decides with its own network (sim/brain), so the scripted rules' knobs are
 * gone: search runs and turns, the trail-following chance and gain, deposit saturation and
 * crowding, and the spoil drop and pick-up rates. Their values and the M1 to M1b-2b calibration
 * history against GruterScenarioTest are in git and in the seed brain's KDoc, which carried them
 * into circuits. The trail choice at the exit came back in M2a Task 8b as an innate primitive
 * with its own constants (`Primitives.faceExitTrail`). trailThreshold (1.96) stays: the senses
 * scale trail readings by it, and a searching forager starts following a trail when it reads at
 * least that much (`Actions.follow`).
 */
data class AntParams(
    // Movement (section 10)
    val surfaceSpeed: Float = 20f,
    val shaftSpeed: Float = 8f,
    val loadedFactor: Float = 0.8f,
    // Path integration error: standard deviation per millimeter walked (section 4)
    val piErrorPerMm: Float = 0.03f,
    // Trail sensing and following (sections 3 and 4)
    val senseAhead: Float = 10f,
    val senseAngle: Float = 0.5f,
    val trailThreshold: Float = 1.96f,
    // Trail laying (section 5)
    val markChancePerMm: Float = 0.045f,
    val markAmount: Float = 2.25f,
    val nonLayerFraction: Float = 0.14f,
    val desiredMin: Float = 0.3f,
    val desiredMax: Float = 0.9f,
    // Trips (section 5)
    val feedSeconds: Float = 60f,
    val unloadSeconds: Float = 60f,
    val unloadDepth: Int = 15,
    val foodSenseRadius: Float = 25f,
    // Plants: honeydew odour draws searching foragers to the stem, and an empty forager touching
    // a stem climbs to the aphids 0.5 to 1.2 m up (section 13).
    val plantOdourRadius: Float = 150f,
    val stemTouch: Float = 3f,
    val stemLeaveHeight: Float = 10f,
    val homeSightRadius: Float = 60f,
    val entranceRadius: Float = 5f,
    // Obstacle detours: an ant steering every tick for a plant or the nest entrance in sight that
    // has not got at least detourProgressMm closer (horizontally) within detourStallSeconds turns
    // about 90 degrees to a random side and walks a run of exponential length, mean detourRunMm,
    // without steering for the target, then tries again. Without it, the plant steering parked
    // ants on a rock's widest point.
    val detourProgressMm: Float = 2f,
    val detourStallSeconds: Float = 2f,
    val detourRunMm: Float = 40f,
    // Repeated detours for the same target escalate: the mean run is detourRunMm times
    // detourRunGrowth per earlier detour, capped at detourRunCapMm, and the turn widens by
    // detourTurnStep (radians) each time from 90 up to 135 degrees. The escalation ends when the
    // ant gets detourResetMm closer than it was at the first stall.
    val detourRunGrowth: Float = 2f,
    val detourRunCapMm: Float = 320f,
    val detourTurnStep: Float = 0.3927f,
    val detourResetMm: Float = 20f,
    // Fields (section 5): weak trails decay 0.4% per second
    val trailDecay: Float = 0.004f,
    val trailDiffusion: Float = 0f,
    val homeScentDecay: Float = 0.0002f,
    val homeScentPerSecond: Float = 0.01f,
    // Digging (sections 7 and 10): one 1 mm3 cell at about 2 mm3 per ant per hour
    val digSecondsPerCell: Float = 1800f,
    val clayFactor: Float = 2f,
    val volumePerAnt: Int = 25,
    val buildPheromoneLifetime: Float = 960f,
)

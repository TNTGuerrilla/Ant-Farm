package com.bydesigninteractive.ant.sim.ant

/**
 * Every tunable constant of the scripted ants. Section numbers refer to the simulation reference.
 *
 * Calibrated against the Gruter 2012 scenario test (GruterScenarioTest). No stable trail forms
 * with 30 foragers; a trail with symmetry breaking forms with 150 foragers on one of two equal
 * sources, and a crowded colony switches to a richer source offered later:
 * - trailDiffusion 0 (M1 0.025, from 0.05): no diffusion on the 3D surface. The trail is a
 *   narrow line on the ground; diffusing it in 3D spread each mark sideways and also up into
 *   the air and down into the soil, where the surface never reads it, so a 5-minute-old mark
 *   read about 10x weaker than in M1's 2D field. That forced M1b-1's first calibration to
 *   markAmount 17.09 and trailThreshold 1.25 as compensation. Without diffusion the trail field
 *   is lazy (decay on touch, periodic sweep) and its blocks are released below 1e-2.
 * - trailThreshold 1.96 (from 4) and markAmount 2.25 (from 1): M1's values, restored in M1b-1
 *   Task 12 with no further change. All three checks pass (seeds 1 to 4):
 *   - 30 foragers: following 0.009, 0.055, 0.009, 0.001, mean 0.018 against a 0.15 limit.
 *   - 150 foragers: following 0.402, 0.475, 0.412, 0.450, mean 0.435 against a 0.3 limit;
 *     busier-source share 0.83, 0.88, 0.81, 0.86, all 4 seeds at 0.7 or more (3 needed).
 *   - Crowded switch: loser share after the switch 0.89, 0.15, 0.63, 0.68, 3 seeds at 0.5 or
 *     more (3 needed). This check has no spare seed; it is the thinnest margin.
 * - crowdLevel 3 (from 4) and saturationLevel 10 (from 20): recalibrated in M1b-2a Task 1, when
 *   the 4 mm height grid changed the ground and the crowded switch fell to 0 of 4 (loser share
 *   0.10, 0.19, 0.34, 0.31). crowdLevel 3 alone gave 2 of 4 (0.69, 0.27, 0.80, 0.34); adding
 *   saturationLevel 10 gave 3 of 4. Final values, seeds 1 to 4:
 *   - 30 foragers: following 0.001, 0.113, 0.001, 0.004, mean 0.030 against a 0.15 limit.
 *   - 150 foragers: following 0.450, 0.379, 0.347, 0.397, mean 0.393 against a 0.3 limit;
 *     busier-source share 0.68, 0.91, 0.76, 0.84, 3 seeds at 0.7 or more (3 needed).
 *   - Crowded switch: loser share 0.76, 0.22, 0.84, 0.67, 3 seeds at 0.5 or more (3 needed).
 *   Margins are thin: two checks have no spare seed.
 * - crowdLevel 2 (from 3): recalibrated in M1b-2b Task 8, when the open-ground fast path put
 *   ants on the height grid from one sample and the crowded switch fell to 2 of 4 (loser share
 *   0.89, 0.26, 0.91, 0.41). Tried one knob at a time: saturationLevel 7 left it at 2 of 4
 *   (0.83, 0.23, 0.77, 0.44); crowdLevel 2 alone passes. Final values, seeds 1 to 4:
 *   - 30 foragers: following 0.004, 0.031, 0.001, 0.036, mean 0.018 against a 0.15 limit.
 *   - 150 foragers: following 0.454, 0.343, 0.433, 0.315, mean 0.386 against a 0.3 limit;
 *     busier-source share 0.80, 0.87, 0.72, 0.86, all 4 seeds at 0.7 or more (3 needed).
 *   - Crowded switch: loser share 0.63, 0.23, 0.91, 0.65, 3 seeds at 0.5 or more (3 needed).
 *   The crowded switch still has no spare seed.
 * - maxFollow 0.7 (from 0.66): a slightly higher ceiling on joining a trail.
 * - exitChoiceExponent 3 (from 2): a steeper choice among trails at the exit, so the colony
 *   breaks symmetry between equal sources.
 */
data class AntParams(
    // Movement (section 10)
    val surfaceSpeed: Float = 20f,
    val shaftSpeed: Float = 8f,
    val loadedFactor: Float = 0.8f,
    // Search walk: exponential straight runs, then turns; tortuous out, straighter home (section 5)
    val outRunMean: Float = 20f,
    val outTurnSd: Float = 0.9f,
    val homeRunMean: Float = 60f,
    val homeTurnSd: Float = 0.3f,
    val searchGiveUp: Float = 900f,
    // Path integration error: standard deviation per millimeter walked (section 4)
    val piErrorPerMm: Float = 0.03f,
    // Trail sensing and following (sections 3 and 4)
    val senseAhead: Float = 10f,
    val senseAngle: Float = 0.5f,
    val trailThreshold: Float = 1.96f,
    val maxFollow: Float = 0.7f,
    val trailTurnGain: Float = 3f,
    val trailLossFraction: Float = 0.3f,
    // Trail choice at the nest exit (section 5): forks are chosen by relative trail strength, 74 to
    // 83% correct per junction, and Beckers 1990 collective choice follows Deneubourg's
    // (k + s)^n choice function over the sampled directions.
    val exitDirections: Int = 16,
    val exitSenseRadius: Float = 40f,
    val exitChoiceExponent: Float = 3f,
    val exitChoiceK: Float = 1f,
    // Trail laying (section 5)
    val markChancePerMm: Float = 0.045f,
    val markAmount: Float = 2.25f,
    val saturationLevel: Float = 10f,
    val crowdRadius: Float = 10f,
    val crowdLevel: Int = 2,
    val nonLayerFraction: Float = 0.14f,
    val desiredMin: Float = 0.3f,
    val desiredMax: Float = 0.9f,
    // Trips (section 5)
    val feedSeconds: Float = 60f,
    val unloadSeconds: Float = 60f,
    val unloadDepth: Int = 15,
    val foodSenseRadius: Float = 25f,
    // Plants: honeydew odour draws searching foragers to the stem, and an empty forager touching
    // a stem climbs to the aphids 0.5 to 1.2 m up (section 13). Scripted stand-ins for M2 brains.
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
    // Spoil pellets (section 10, Khuong 2016)
    val spoilDropBase: Float = 0.025f,
    val spoilDropPerPellet: Float = 0.11f,
    val spoilPickUp: Float = 0.029f,
    val spoilMaxDistance: Float = 40f,
)

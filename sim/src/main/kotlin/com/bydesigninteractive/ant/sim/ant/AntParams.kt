package com.bydesigninteractive.ant.sim.ant

/** Every tunable constant of the scripted ants. Section numbers refer to the simulation reference. */
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
    val trailThreshold: Float = 4f,
    val maxFollow: Float = 0.66f,
    val trailTurnGain: Float = 3f,
    val trailLossFraction: Float = 0.3f,
    // Trail choice at the nest exit (section 5): forks are chosen by relative trail strength, 74 to
    // 83% correct per junction, and Beckers 1990 collective choice follows Deneubourg's
    // (k + s)^n choice function over the sampled directions.
    val exitDirections: Int = 16,
    val exitSenseRadius: Float = 40f,
    val exitChoiceExponent: Float = 2f,
    val exitChoiceK: Float = 1f,
    // Trail laying (section 5)
    val markChancePerMm: Float = 0.045f,
    val markAmount: Float = 1f,
    val saturationLevel: Float = 20f,
    val crowdRadius: Float = 10f,
    val crowdLevel: Int = 4,
    val nonLayerFraction: Float = 0.14f,
    val desiredMin: Float = 0.3f,
    val desiredMax: Float = 0.9f,
    // Trips (section 5)
    val feedSeconds: Float = 60f,
    val unloadSeconds: Float = 60f,
    val unloadDepth: Int = 15,
    val foodSenseRadius: Float = 25f,
    val homeSightRadius: Float = 60f,
    val entranceRadius: Float = 5f,
    // Fields (section 5): weak trails decay 0.4% per second
    val trailDecay: Float = 0.004f,
    val trailDiffusion: Float = 0.05f,
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

package com.bydesigninteractive.ant.sim.util

/** The SplitMix64 finalizer: spreads every input bit across the whole 64-bit result. */
fun mix64(value: Long): Long {
    var z = value + -0x61c8864680b583ebL
    z = (z xor (z ushr 30)) * -0x40a7b892e31b1a47L
    z = (z xor (z ushr 27)) * -0x6b2fb644ecceee15L
    return z xor (z ushr 31)
}

/** A deterministic hash of a seed and two integer coordinates. */
fun hash(seed: Long, a: Int, b: Int): Long = mix64(mix64(seed + a.toLong()) + b.toLong() * 0x9E3779B1L)

/** [hash] mapped to a double in [0, 1). */
fun unit(seed: Long, a: Int, b: Int): Double = (hash(seed, a, b) ushr 11) * UNIT_SCALE

private const val UNIT_SCALE = 1.0 / (1L shl 53)

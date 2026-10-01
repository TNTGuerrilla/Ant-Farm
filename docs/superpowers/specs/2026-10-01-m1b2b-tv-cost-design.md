# M1b-2b: simulation cost on the TV

Status: approved in design discussion on 2026-10-01, awaiting spec review.
Parent design: `docs/design/poc-design.md` (sections 11, 12 and 14). Follows M1b-2a (`docs/superpowers/specs/2026-09-30-m1b2a-engine-design.md`). The numbers that motivate it are in `docs/superpowers/notes/2026-09-30-m1b2a-tv-session.md`.

## 1. Goal

M1b-2 was split three ways. M1b-2a built the engine and measured the TV. **M1b-2b (this spec)** cuts the simulation's cost on the TV. **M1b-2c (a later spec)** builds the low-poly look and ends with the final 60 fps TV test.

The M1b-2a TV run measured about 13 ms per tick for the 80-ant starter colony on a debuggable build, about 40 times the desktop. That is roughly 0.16 ms per ant. The design budget is 1,000 ants at 20 ticks per second (section 11 of the design doc), which at that rate would need about 160 ms per tick, more than three times the whole 50 ms tick.

M1b-2b is done when:

1. A non-debuggable benchmark build runs on the TV over adb (no remote, no screensaver settings touched) and logs a per-phase breakdown of the tick and the number of SDF evaluations per tick.
2. On the TV, the 1,000-ant scenario (section 2.2) averages **at most 25 ms per tick, with the 99th percentile at most 50 ms**, counting step plus publish, measured after the warm-up. Half of each tick is left for M2's brains and for the render thread.
3. On the TV, the 80-ant starter colony holds 4x (at most 12.5 ms per tick).
4. Tier 1 changes (section 3) leave the simulation fingerprint bit-exact. After tier 2 (section 4), every surface test, the Gruter test (recalibrated by the M1 rules if needed) and every determinism test pass.
5. The TV screensaver and the preview activity install from the non-debuggable build, so what the owner sees matches what was measured.

Out of scope: the low-poly look and the final 60 fps TV test (M1b-2c); updating ants in parallel across cores (a separate decision, section 4.4); more seeds for the Gruter test (M2, by the owner's decision of 2026-09-30); cheaper simulation for ants far from the camera (rejected: it would make a run depend on the view).

## 2. Measuring

### 2.1 The phase profile (`sim`)

- `TickProfile` is a pure-Kotlin phase timer. `World` holds an optional one (null by default); `World.step` fills it only when one is attached, so normal runs pay nothing beyond a null check per phase.
- Phases: spatial-index rebuild, surface fields step, nest (paths, excavation, nest pheromone), surface ants, nest ants. `SimRunner` adds the publish time.
- Counters: `SurfaceSdf.distance` evaluations, `HeightField` samples, and (after tier 2) fast-path hits and misses. A counter is a plain `Int` increment.
- The JVM benchmarks in `SurfaceRunTest` print the same breakdown as the TV, so ideas are tried on desktop before an install.

### 2.2 The 1,000-ant scenario

`Scenarios.colony1000(seed)`: the starter layout (plants, prey, rocks, entrance) with 800 foragers and 200 diggers. It is deliberately surface-heavy, a conservative stand-in for a mature colony. A benchmark runs a warm-up of 5 simulated minutes, so trails and home scent exist, then measures.

### 2.3 The benchmark build and activity (`android`)

- A `profile` build type: not debuggable, signed with the debug keystore, no minification for now. It is separate from the real release signing, which comes in M8.
- `BenchmarkActivity`: exported, with no launcher entry, started with `adb shell am start -n com.bydesigninteractive.ant/.BenchmarkActivity` and extras for the scenario (`starter` or `colony1000`), the speed, the warm-up seconds and the measure seconds. It runs the simulation headless on a worker thread (no rendering), shows a single "benchmarking" line, and logs `BENCH` lines under the `AntFarm` tag: the per-phase averages, ms per tick (average, p50, p99, max), SDF evaluations per tick, fast-path hit rate, and heap in use.
- For the starter at 4x, the activity runs the scenario through `SimRunner` at speed 4 and reports achieved ticks per second, so the 4x check includes the scheduler.
- The TV screensaver and preview activity install from the `profile` build from now on (done item 5).

### 2.4 TV runs

Three benchmark sessions: the baseline (the `profile` build before any code change), after tier 1, and after tier 2. They need no remote, but the benchmark screen takes over the TV for a few minutes, so Claude asks the owner before each run, then reads the log and uninstalls. The milestone ends with one short hands-on screensaver check of the `profile` build, run the usual way: Claude sets up, the owner watches, presses Back and says done, and only then does Claude read the log and restore the TV.

## 3. Tier 1: bit-exact speedups

Each change must leave the fingerprint identical (the M1b-2a check: run a fixed scenario, compare the sum and counts before and after). A change that is not bit-exact moves to tier 2.

1. **The release build** is measured first, before any code change, so later gains have the right baseline.
2. **Shape data in plain arrays.** The per-chunk blob lists and the per-chunk food lists that `SurfaceSdf.distance` loops over become plain arrays (`Array<Blob>`, `Array<FoodSource>`), so the hottest loop makes no interface calls on `List`. The order of shapes is unchanged.
3. **Food indexed by chunk.** Each chunk keeps the list of foods whose shapes can reach it, in the same relative order as `map.foods`. `distance` loops over that list instead of every food. The near test is unchanged, so only foods that would have failed it are skipped, and the result is bit-exact. Food is added and removed only through `SurfaceMap.addFood` and `removeFood`, which bump a version that tells the index to rebuild.
4. **The spatial index clears only what it used.** `SpatialIndex` remembers which buckets it filled and resets those, instead of filling 160,801 entries every tick.
5. **Smaller trims.** Cache the cos and sin of the constant antenna angle. Unroll the 8-corner loop in `Field3.get` and `Field3.add`, and give `Field3` a small cache of recent blocks (4 entries) instead of 1.
6. **Not tier 1**, because each could change the last bit of a result: `hypot` to `sqrt`, float-only or approximate trig, a cheaper Gaussian. These are tier 2 candidates, taken only if the TV breakdown shows they matter.

## 4. Tier 2: the open-ground fast path

### 4.1 The idea

Where no rock, pebble, stem, food or placed shape is within reach, the SDF equals the ground's distance function, whose zero set is exactly `z = h(x, y)`. There the surface point is `(x, y, h(x, y))` and the normal is `normalize(-hx, -hy, 1)`, from one height-and-slope sample, with no Newton iteration.

### 4.2 Where it applies

- `SurfaceWalk.step`: today two projection iterations plus a normal (about 20 evaluations); on open ground, one sample.
- `SurfaceWalk.place` (an ant leaving the nest).
- The two antenna samples in `SurfaceWalk.sense`: today 7 evaluations each; on open ground, one sample each.

A searching forager in the open drops from about 34 evaluations per tick to 3 samples.

### 4.3 The clearance test

`SurfaceSdf.isOpenGround(x, y, margin)` is conservative: it returns true only if no shape's reach (its near radius) comes within `margin` of the point. The margin covers one step's travel plus the antenna spread. It reuses the per-chunk shape arrays from tier 1 and costs a few comparisons. Near any shape, the full projection runs exactly as today, so climbing rocks, overhangs and stems keep their behaviour.

### 4.4 Consequences

- Results change: the Newton projection lands within a hair of the surface, the fast path lands exactly on it, and the difference compounds. After tier 2, every surface test is rerun, and the Gruter test is rerun and, if it fails, recalibrated by the M1 rules (listed knobs, one at a time, at most eight attempts, never loosening the thresholds).
- The tier-1 items that were not bit-exact (section 3, item 6) ride along here, but only those the TV breakdown shows to matter.
- If tier 2 still misses 25 ms per tick on the TV, Claude stops and brings the owner the breakdown. Parallel updates across cores (sense in parallel, act in sequence) would then be a separate decision, not something this milestone grows into.

## 5. Testing

- Tier 1: the fingerprint test (bit-exact) after each change, plus all existing tests.
- Fast path, property tests: at sampled open-ground points, the fast point lies on the SDF surface within 0.001 mm and its normal is within 1 degree of the SDF gradient there (the gradient is a central difference across bilinear cells, so it differs slightly from the analytic slope); at sampled points near shapes (rocks, pebbles, stems, prey, placed shapes), `isOpenGround` never returns true.
- After tier 2: all surface tests, the Gruter test, the stepped and threaded determinism tests.
- `TickProfile`: a test that attaching a profile does not change results (a run with and without one gives the same fingerprint).
- The JVM benchmarks print the breakdown; the TV benchmark is the acceptance measurement.

## 6. Code structure

- `sim/.../TickProfile.kt` (new); `World.step` phase hooks; counters in `SurfaceSdf` and `HeightField`.
- `sim/.../scenario/Scenarios.kt`: `colony1000`.
- Tier 1: `SurfaceSdf`, `SurfaceMap` (food index), `SpatialIndex`, `Field3`, `SurfaceWalk`.
- Tier 2: `SurfaceWalk` (fast path), `SurfaceSdf.isOpenGround`.
- `android/build.gradle.kts`: the `profile` build type. `android/.../BenchmarkActivity.kt` (new) and its manifest entry.
- `core/.../engine/SimRunner.kt`: publish time in the profile.

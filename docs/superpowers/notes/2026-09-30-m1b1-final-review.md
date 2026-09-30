# M1b-1 final review and follow-ups

The whole-branch review of M1b-1 (2026-09-30, through commit 731564b). After it, items I1, I2 (per-tick diffusion cost), I4 and I6 (minimal) were fixed in Task 12 (no trail diffusion, M1 calibration restored, side-effect-free reads, food-history-independent rocks). The rest, especially I3 (height-grid memory) and I5 (TV-scale tick cost), are M1b-2 work.


Reviewer scope: whole-branch, cross-task. Per-task conformance was already reviewed and is not repeated here. The full suite was not re-run. One scratch probe was run outside the repository (a copy under the session scratchpad; the working tree, index, HEAD and branches were not touched), and its results are quoted in the calibration section.

## Assessment

**Ready to merge: yes, with follow-ups.** There are no Critical issues. The six Important issues below are latent or scale-related. None is a correctness bug at M1b-1 scale, and none needs a change before merge. Four of them should be settled in M1b-2, before or during the TV test. Two belong to M2 or M3. The calibration is acceptable to merge, and it also passes on unseen seeds 5 to 8. Its written diagnosis is incomplete, though: 3D diffusion, not lost searchers, is the main structural cause. With diffusion off, M1's own values pass with wide margins (see the calibration section).

Counts: Critical 0, Important 6, Minor 9.

## Strengths

- **A clean, general surface model.** `SurfaceWalk` (move, project twice, read the normal, parallel-transport the forward vector) handles ground, rocks, overhangs, stems and food lumps with no special cases. The two fold fallbacks in `transport` are well reasoned, and the tests pin them.
- **Good split between the pure and the cached.** The ground grid, per-chunk blob lists and neighbor gathers are all pure functions of the seed (with one exception, Important 6), cached in fixed-size arrays indexed by chunk, so they are bounded.
- **Field3 is compact and correct.** It uses trilinear deposit and read, lazy decay for the home scent, a Jacobi diffusion step with neighbor blocks resolved once per block, and a bijective mixed hash key. The block-face stencil indices check out, and the Jacobi double buffer is order-independent, so the `LinkedHashMap` iteration order cannot affect results.
- **The determinism discipline holds.** Generation uses `StrictMath` and plain-arithmetic value noise. Every sim random draw comes from `world.rng`. There is no `HashMap` iteration in the sim step. Both determinism tests compare `z` as well as x and y.
- **The approved deviations are sound.** The rock clearance around food and the entrance, the climb heading (pressing toward the stem axis on the sloped base), the descent condition using `nz`, the analytic ground slope and exact compass facing are all improvements, and each has a comment explaining why.
- **The core changes are small and safe.** The wall-clock step budget behaves as specified, and the throttled HUD, `placeAt` clamping and the snap-on-teleport chase camera have tests. The 3D debug view maps coordinates with a proper rotation (`(x, y, z) -> (x, z, -y)`, determinant +1), and ants are drawn with a right-handed `(f, n, f x n)` basis.
- **The calibration record is honest.** The Task 8 report and the `AntParams` KDoc state the thin margins and the artifact status of `markAmount`, which is what a later milestone needs.

## Issues

Severity key: Critical blocks the merge. Important must be fixed in the milestone named, and the next milestone's plan should carry it. Minor is optional or can be done when the file is next touched.

### Critical

None.

### Important

**I1. 3D diffusion dilutes trails, and this is a structural cause the calibration diagnosis misses.**
- Where: `sim/.../world/Field3.kt:169-205` (`diffuse`, the 6-neighbor stencil) and `SurfaceMap.kt:26`.
- What is wrong: M1 diffused the trail in 2D (4 neighbors). `Field3` diffuses in 3D (6 neighbors) with the same `trailDiffusion` 0.025. For a trail, which is a line on the ground, the cross-section now spreads in two dimensions (sideways and up/down into air and soil) instead of one. The mass that spreads into the soil and the air is never read at the surface. A discrete simulation of this stencil (k = 0.025 x 0.05 per tick) gives the peak reading of a mark relative to M1:

  | Age of the mark | M1 (2D) peak | M1b-1 (3D) peak | M1 / M1b-1 |
  |---|---|---|---|
  | 5 s | 0.79 | 0.63 | 1.3x |
  | 20 s | 0.47 | 0.22 | 2.2x |
  | 100 s | 0.18 | 0.034 | 5.5x |
  | 300 s | 0.10 | 0.011 | 9.6x |

  On top of this, trilinear deposit and read cost about another third of the peak on average. Together these explain most of the 7.6x rise in `markAmount`. The scratch probe confirms it: with `trailDiffusion = 0`, M1's own values pass both colony-size Gruter checks with wide margins (see the calibration section). Weaker, faster-fading trails far from the nest also produce the "lost searcher" pool that Task 8 measured (more trail losses, fewer rejoins, more give-ups), so that pool is at least partly a symptom rather than an independent cause.
- Why it matters: the calibration now compensates for a modelling artifact with a deposit size near saturation. It also leaves trails with a strong young head and a weak old tail, which is not how a Lasius trail behaves. M2's brains will learn on these readings.
- Fix: in M2's recalibration (first step), stop diffusion across the surface normal. The simplest option is `trailDiffusion = 0` in 3D. The design doc wants narrow trails, M1 already halved diffusion for that reason, and it makes the trail field lazy, which also fixes I2's per-tick cost. The alternative is to diffuse in x and y only. Then recalibrate `markAmount` downward. The scratch probe (calibration section) shows what diffusion 0 does to the Gruter numbers. Pair this with I4, because a lazy trail field makes the renderer's reads mutate sim state.

**I2. The per-tick cost of trails is unmeasured, and dead blocks live about 35 minutes too long.**
- Where: `sim/src/test/.../scenario/SurfaceRunTest.kt:28-45`, `Field3.kt:169-205` and `Field3.kt:236` (`FADED = 1e-4f`).
- What is wrong: the benchmark runs 1,000 searchers for 65 s. A forager needs at least 15 s to reach a plant 300 mm out, then feeds for `feedSeconds` = 60 s, so no ant ever returns full and no trail is laid. The benchmark therefore measures zero trail diffusion. At runtime, every allocated trail block is diffused every tick (1,000 voxels, 6 hash lookups and a second pass for the maximum). A block is released only when its maximum falls below 1e-4. A trail stops being followed at about `trailThreshold x trailLossFraction` = 0.38, and joining at s = 0.05 has a chance of about 0.001. Decay alone takes ln(0.38 / 1e-4) / 0.004 = about 2,060 s (34 minutes) to go from behaviorally dead to released. With `markAmount` at 17.09, a fresh mark also lives about 500 s longer than at M1's 2.25. Trail blocks straddle z = 0 on rolling ground, so a trail usually allocates two block layers.
- Why it matters: at TV scale, over hours, the trail block count (and so the per-tick cost and memory, 8 KB per block with the scratch buffer) is the one cost that grows with colony history rather than with the ant count, and nothing measures it.
- Fix (M1b-2, before the TV test):
  - Add a benchmark variant that pre-lays a realistic trail network, or runs the starter for 30 simulated minutes, and reports `trail.blockCount()` next to the ms per tick.
  - Raise `FADED` to something tied to the behavior scale, for example 1e-2. Following at that strength is about 1e-4.
  - Fold the maximum into the diffusion loop, as the spec intended ("a per-block running maximum").
  - If I1's `trailDiffusion = 0` is adopted, most of this goes away.

**I3. The height-field cache is 64.5 MB at full coverage, with no eviction.**
- Where: `sim/.../world/sdf/HeightField.kt:21` and `:158-159`.
- What is wrong: each chunk grid is 251 x 251 floats, which is 252 KB. The array is never evicted, so the whole 8 x 8 m map costs 64.5 MB. Ants on hours-long runs, the debug ground mesh, blob generation (which calls `base` for the stones of every gathered neighbor chunk) and, in M1b-2, the marching-cubes meshes and the minimap all push toward full coverage.
- Measured: the scratch probe retained 49 MB of heap for one `Scenarios.starter(5)` world after 30 simulated minutes. Of that, the trail (175 blocks, about 1.4 MB) and the home scent (461 blocks, about 1.8 MB) are small, and the height grids are the largest component the review can account for. A first attempt to run 24 Gruter worlds on Gradle's default 512 MB test heap ran out of memory.
- Why it matters: the design budget is about 150 MB total, and the TV is a 32-bit ARM Android process with a per-app heap limit. A 2 mm float grid is far finer than the 1.5 mm bumps at a 20 mm scale need.
- Fix (M1b-2): use a 4 mm grid (bumps at a 20 mm scale survive bilinear interpolation), store it as a `ShortArray` in 0.001 mm steps, or evict least-recently-used chunks (the grid regenerates exactly from the seed). The first two together bring the full map to about 8 MB. Re-run the surface tests, because the ground shape changes slightly.

**I4. Reading a lazily decayed field mutates it, so renderer reads could change the simulation.**
- Where: `Field3.kt:95-125` (`max`, `projectMax`) and `:156-163` (`catchUp`). Callers: `core/.../render/SurfaceTopRenderer.kt:72` and `core/.../ui/DebugReadout.kt:22`. A related case is `DebugSurfaceRenderer.kt:79`, which calls `sdf.ownBlobs` and so triggers blob generation.
- What is wrong: `get`, `max` and `projectMax` call `catchUp`, which multiplies a block by `exp(-k x elapsed)` and writes it back. Catching up in one step and in several steps give different float roundings. Today this is harmless only because the renderer reads the trail field, which diffuses every tick, so `catchUp` is a no-op there. If anything in `core` reads `homeScent`, or if the trail becomes lazy (I1's recommended fix), the simulation's results depend on when frames were drawn and what the view showed. The determinism tests cannot catch this, because they never render.
- Why it matters: M1b-2 adds renderers and M6 adds overlays, and the "while you were away" catch-up and M7 snapshots assume a run is reproducible.
- Fix (M1b-2, or together with I1): add a side-effect-free read path to `Field3`, a `peek` that computes `v x exp(-k x (now - tick))` without writing back, and use it everywhere in `core`. Document on `World` that `core` may call only side-effect-free sim APIs. Add a determinism test that interleaves render-style reads (`projectMax`, `max`, `homeScent` reads, `ownBlobs` over random chunks) into one of the two runs.

**I5. At TV scale a single tick cannot be split, and the hot path allocates in ways the desktop benchmark hides.**
- Where: `core/.../AntApp.kt:130-137` (the budget is checked only after a whole `world.step()`), `SurfaceSdf.kt:33-35` and `:78` (iterator loops in `distance`, which runs about 34 times per ant per tick), and `Field3.kt:145-149` and `:172-180` (`LinkedHashMap<Long, Block>` lookups box the `Long` key on every cache miss, 6 per block per tick in `diffuse`, plus a `dead` list every tick).
- What is wrong:
  - The desktop costs about 3.25 ms per tick for 1,000 ants. A Cortex-A55-class TV core is plausibly 5 to 8 times slower, so a tick would take about 15 to 25 ms. The 6 ms budget cannot cut into one tick, so each frame that runs a tick would miss 60 fps.
  - HotSpot's escape analysis removes the iterator allocations (about 2 million a second at 1,000 ants), but ART generally does not, so GC churn would appear only on the device.
- Why it matters: the M1b-2 TV test is the next milestone's acceptance criterion.
- Fix (M1b-2):
  - Run the sim benchmark on the TV first thing.
  - Use indexed loops over `ArrayList` or arrays in `SurfaceSdf.distance`, `stemAt`, `nearestFood` and `nearestPlant`, and a primitive long-keyed open-addressing map in `Field3`. These are bit-exact refactors that do not disturb the calibration.
  - Plan for spreading a tick over frames. For example, do `surface.step()` and the index rebuild in slice 0, then run the ants in fixed thirds over three frames. The results are identical to a whole tick, so it is deterministic.
  - Alternatively, move the sim to a worker thread with a render snapshot. That also requires I4, and today's single-thread scratch arrays and lazy caches (`HeightField.slopeScratch`, `SurfaceSdf.g`, `World.pos`, the chunk caches) make it a bigger change.
  - Numeric speedups, such as reusing the last Newton gradient as the normal (saves 6 SDF evaluations per step) or a 4-sample tetrahedral gradient, change trajectories. Given the thin calibration margins, keep them for M2's recalibration.

**I6. Rock placement depends on which foods exist when a chunk is first queried.**
- Where: `SurfaceSdf.kt:86-87` (lazy `ownBlobs`) and `:121-131` (`addBlob` skips blobs near `map.foods` at generation time). Food is removed at `Forager.kt:210`.
- What is wrong: world geometry is a function of the seed plus the history of `foods` at the moment each chunk is first touched, by an ant or by the renderer. Today it is safe in practice: foods are placed before any SDF query in `Scenarios`, and a prey item's neighbors are generated while ants feed there, before it runs out. It breaks as soon as food is added at run time (a rock can already sit where new prey lands) or a world is restored from a snapshot after prey ran out (a rock appears where the prey was, possibly around ants standing there).
- Why it matters: M3 adds food over time, and M7 adds snapshots and restore.
- Fix (before M3 prey spawning or M7): keep a grow-only list of exclusion discs (entrance and every food ever placed) in `SurfaceSdf`. When a disc is added, invalidate `own` and `near` for the chunks within blob reach. Persist the list with the snapshot. Geometry is then a pure function of the seed plus that list.

### Minor

- **M1.** `Forager.kt:70` and `:114-143`: plant odour takes priority over nearby ground food and over trail following for any empty searcher within 150 mm of a stem. A trail to prey that passes near a plant is abandoned there. This is acceptable for a scripted stand-in; note it for M2.
- **M2.** `Forager.kt:225`, `Digger.kt:103`: entrance arrival uses a 2D `hypot`, but spec 3.4 says "using 3D distance". It is harmless while no overhang or stem can be within 30 mm of the entrance. Align it when M2 touches homing.
- **M3.** `Digger.kt` (DUMP and GO_HOME): spoil is read, dropped and picked up at the ant's (x, y), so a digger on top of a rock or pebble drops into the ground field under the rock. It is rare, because `spoilMaxDistance` is 40 mm and the entrance clearance is 30 mm, but it matters if the clearance ever shrinks.
- **M4.** `Field3.kt:1` KDoc and the spec: blocks are 10^3, not the spec's 16^3. This is fine (it aligns with the 50-cell overlay), but the spec or the design note should say so. `Field3.kt:5` has an unused `import kotlin.math.max`.
- **M5.** `ChunkedField.step` (`ChunkedField.kt:59`) is now dead code (spoil is never stepped). Remove it or note that it is kept for M3.
- **M6.** `SurfaceMap.kt` and `SurfaceSdf.kt:35`: every SDF evaluation scans all foods. This is fine at about 10 foods, but index foods by chunk when M3 adds many prey items.
- **M7.** `SurfaceRunTest.kt:14-23`: the determinism test should also assert `ants.size`, `state`, `crop`, the forward vector, `feedEvents.size`, `trail.blockCount()` and one `rng.nextLong()` from each world. A matching rng is the cheapest whole-run fingerprint.
- **M8.** `AntApp.kt:318-324`: reset `hudAge` to `HUD_EVERY_S` on a view change, so the title updates at once (T10 minor).
- **M9.** `DebugSurfaceRenderer.kt:122-128`: `next` allocates a `ColorAttribute` and boxes an `Integer` per instance per frame. This is debug only, but M1b-2 should not carry the pattern into the real renderer.

## Triage of minor-findings.md

| Task | Finding | Decision |
|---|---|---|
| T1 | Duplicated StrictMath cos/sin expressions in SurfaceMap | Drop (two call sites; a helper is optional) |
| T1 | Import order; redundant `ArrayDeque` import (`World.kt:20`) | Fix when next touched |
| T2 | Lumpy blob gradient not unit (Newton overshoot) | Drop: the lumpy-rock projection, overhang and over-a-rock tests all hold within 0.5 mm with 2 iterations |
| T2 | Weak flat and lump tests | Drop |
| T2 | No guards for k = 0, zero radius, z0 > z1 | M1b-2: add `require` checks when the mesh builder starts consuming shapes |
| T3 | HeightField.distance evaluates height 5 times | Resolved in 6b (analytic slope). Close |
| T3 | Weak distance and edge tests | Drop |
| T3 | spoilHeight not clamped | Drop for M1b (about 2,000 pellets over about 50 cells gives about 0.4 mm). Revisit in M3, when colony-scale spoil could make local slopes steep enough to degrade the `(z - h) / sqrt(1 + g^2)` bound |
| T4 | Rock food clearance depends on food placed before the first SDF query | Promoted to Important I6 (before M3 or M7) |
| T4 | Weak margin test; long KDoc line | Drop |
| T4 | Generation constants lack citations | Drop: they are design choices, not reference values. Optionally label them as such |
| T5 | Diffusion test lacks a total assert | M2 (with I1's diffusion change) |
| T5 | No tests for catch-up across adds or the cache after a sweep | M1b-2 (with I4's `peek`) |
| T5 | Cross-block diffusion test | Resolved (`diffusionCrossesBlockFaces`) |
| T5 | Unused `max` import | Fix when next touched (Minor M4) |
| T5 | No stability `require` (diffusion x dt < 1/6) | M1b-2: one line in the `Field3` init. Cheap insurance before anyone tunes diffusion |
| T5 | Diffuse cost per block | Promoted to Important I2 |
| T6 | Map-edge bounce reverses fully; projection can push slightly outside | Drop: every consumer clamps (`HeightField`, `SpatialIndex`, `ChunkedField`) or accepts it (`Field3`) |
| T6 | Test gaps: underside handedness, PI on a climb, sensing on slopes | M2: brains sense on this surface, so sensing on slopes and undersides needs tests then |
| T6 | Test gap: fold fallbacks | Resolved (both fallbacks have tests) |
| T6b | Analytic slope discontinuous off the surface at 2 mm grid lines | Drop, with a KDoc note. Revisit only if I3 changes the grid |
| T6b | Weak spoil-slope test; y-face diffusion untested | Drop and M2 (diffusion) respectively |
| T6b | Duplicated interpolation in `heightAndSlope` | M1b-2 (with I3) |
| T6b | `scratch!!` invariant comment | Fix when next touched |
| T6b | Steep `faceCompass` branch untested | M2 |
| T7 | `STEM_WALL_NZ` unsourced in Forager | Drop (the M2 brains replace it) |
| T7 | `nearestPlant` ignores depletion | Drop (honeydew does not deplete). Revisit if M5 adds seasonal plants |
| T7 | No direct descent test | Drop: `foragersClimbAStemFeedAtTheAphidsAndBringItHome` asserts an unload, which needs the descent |
| T8 | Determinism test lacks `ants.size`; compares only x, y and z | M1b-2 (Minor M7, and I4's render-interleaved variant) |
| T8 | Wall-clock benchmark assertion | Keep the generous 20 ms ceiling. Add the trail-laden variant (I2) |
| T9 | `moveThreshold` unit undocumented (mm/s) | Fix when next touched |
| T9 | Up-vector lerp could shrink near opposite normals | M1b-2 (the real chase camera): use a slerp or a guarded fallback |
| T9 | No slow-ant or dt = 0 tests | M1b-2 |
| T10 | Per-frame `ColorAttribute` and `Integer` boxing | M1b-2: do not carry the pattern forward (Minor M9) |
| T10 | Ground rebuild hitch (10k height and slope samples in one frame) | M1b-2: chunk meshes replace it; build them incrementally or off-frame |
| T10 | Placed blobs not culled | Drop (debug only) |
| T10 | HUD lags a view switch by 0.25 s | Fix when next touched (Minor M8) |

## Calibration view

**Verdict: acceptable to merge as is. Revisit in M2, starting with I1, not with more knob turning.**

**Process.** The hard rules held:
- only listed knobs were changed;
- each attempt changed one knob;
- there were at most eight attempts;
- the thresholds and assertions are untouched (`GruterScenarioTest.kt` is not in the diff).

The search did not follow the table's "markAmount, then trailThreshold" order: it interleaved the two knobs and reverted attempt 3. That breaks the spirit of the rule rather than its letter. It is disclosed in the report and in the `AntParams` KDoc, so no action is needed beyond the owner's acknowledgement.

**Margins.** On the calibration seeds (1 to 4), small-colony following is 0.1435 against 0.15 and large-colony following is 0.324 against 0.3. The per-seed spread is wide (small: 0.10 to 0.21; large: 0.29 to 0.37), so a mean over four seeds carries a standard error of about 0.026 (small) and 0.017 (large). To test for overfitting to seeds 1 to 4, the scratch probe ran the head parameters on unseen seeds 5 to 8, with the same scenario and measurement code:

| Config | Small following, seeds (mean) | Large following (mean) | Large share at 0.7 or more |
|---|---|---|---|
| Head, seeds 5 to 8 | 0.140, 0.176, 0.091, 0.096 (0.126) | 0.303, 0.334, 0.375, 0.379 (0.348) | 3 of 4 (0.84, 0.88, 0.69, 0.79) |

The head values also pass out of sample, with somewhat better margins. The calibration is thin but not a lucky fit to four seeds. It is still a single realization of a chaotic run, so any change that alters trajectories (numeric changes to the SDF or walk, a JDK change, a new rng draw anywhere in `sim`) redraws the outcome. For that reason M1b-2's sim-side work should be bit-exact refactors only.

**Diagnosis.** The Task 8 report attributes the following ceiling mainly to lost searchers, with the trail spread contributing less. I1 shows a larger structural cause: diffusion across the surface normal, which M1 did not have. The scratch probe tested this directly by setting `trailDiffusion = 0` (not a change the Task 8 rules allowed, so this is a diagnosis, not a proposed calibration):

| Config | Small following (mean) | Large following (mean) | Large share at 0.7 or more |
|---|---|---|---|
| Diffusion 0, **M1 values** (markAmount 2.25, trailThreshold 1.96), seeds 1 to 4 | 0.009, 0.055, 0.013, 0.006 (**0.021**) | 0.331, 0.319, 0.412, 0.375 (**0.359**) | 4 of 4 |
| Diffusion 0, head values, seeds 1 to 4 | 0.176, 0.165, 0.086, 0.296 (0.181, fails) | 0.462, 0.320, 0.330, 0.465 (0.394) | 4 of 4 |

With 3D diffusion off, **M1's original markAmount and trailThreshold pass both colony-size checks with wide margins**: 0.021 against 0.15 and 0.359 against 0.3. The large-colony margin is 2.5 times today's, and the small-colony margin is 20 times. The crowded-switch test was not probed. With today's values and no diffusion, trails are too strong and small colonies fail. So the 7.6x `markAmount` and the lowered threshold are compensation for normal-direction diffusion. The lost-searcher pool is largely a consequence of the resulting weak, fast-fading trails, not an independent structural limit.

**Recommendation.**
1. Merge now. The test passes, the rules held, and the KDoc is honest.
2. Amend the calibration note in `AntParams`, or in the M2 plan, to record this finding, so M2 does not inherit "lost searchers" as the explanation.
3. As the first step of M2's recalibration, set `trailDiffusion = 0` (or diffuse in x and y only) and return to M1's values. Then confirm the crowded-switch test and run seeds 5 to 8 as a hold-out. This also removes the per-tick diffusion cost (I2), but it makes the trail field lazy, so I4's side-effect-free reads must land first.
4. Optionally, the owner may decide to run the Gruter test on 8 seeds in M2. That is not loosening the test, but it changes it, so it needs the owner's approval.

## Merge readiness

Ready to merge. All spec "done" criteria are met: ants stay on every surface; the Gruter test passes with M1's thresholds; foragers climb, feed and follow a trail up a stem; the benchmark reports about 3.25 ms per tick; and the 3D and top-down desktop views work. No Critical issues were found.

Carry these into planning:
- **M1b-2 plan:** I2, I3, I4 and I5, plus the M1b-2 rows of the triage table. Rule for M1b-2: sim-side changes must be bit-exact (pure refactors) unless the milestone budgets a recalibration.
- **M2 plan:** I1 (first step of the recalibration) and the Gruter seed-count decision.
- **M3 or M7 plan:** I6.

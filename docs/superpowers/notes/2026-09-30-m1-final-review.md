# M1 final review and follow-ups

The whole-branch review of M1 (2026-09-30). The two items marked "fix before merge" were fixed in commit ba08294. The rest are scheduled into later milestones in the triage table below; M1b and M2 should start from this list.


## Strengths

- The module boundary holds: `sim` has no libGDX imports, and `core` only reads `sim` state. The desktop launcher is a thin launcher, and `--stub` keeps the M0 app available as the spec asked.
- The seams are good for M2. Decisions (`Forager`, `Digger`) are separated from primitives (`SurfaceMotion.advance/sense/steerByGradient`, `NestMotion.move/stepTo`, `World.dig/enterNest/exitNest`), so a brain can replace the state machines without touching the world. `SurfaceMotion.advance` is the one place surface movement happens, which is the right hook for M1b terrain collision.
- Determinism on one platform is sound. All randomness goes through `World.rng` (a `java.util.Random`, whose algorithm is specified) or through `hash`/`unit`, ants update in list order, and no `HashMap` is iterated inside the sim. The `ChunkedField` step is a correct Jacobi update (old buffers are swapped only after every chunk is computed).
- Storage is lazy everywhere it should be: nest tiles, building pheromone, field chunks, chunk textures. `DistanceMap` resets only the cells it touched.
- The Gruter calibration is explained in `AntParams`, and the switching mechanism (trail laying gated on crop >= desired volume, so a richer source recruits more layers) matches the paper's mechanism rather than being a tuning trick.
- GL resource lifetimes are handled with care: evictions run after `batch.flush()`/`batch.end()`, and `Surface3dRenderer.ground` checks decal texture identity, so a chunk texture evicted by the top-down view cannot be drawn after disposal.

## Issues

### Critical

None.

### Important

**I1. The desktop app crashes when the window is minimized.** `core/.../AntApp.kt:94-99`
- What: on Windows, GLFW reports a 0 x 0 framebuffer on minimize, and libGDX 1.14.2 forwards it unguarded (`Lwjgl3Graphics$1.invoke` calls `listener.resize(getWidth(), getHeight())` with no zero check; verified with javap). `resize` then calls `font.data.setScale(1.4f * 0 / 1080f)`, and `BitmapFontData.setScale` throws `IllegalArgumentException("scaleX cannot be 0.")` (also verified with javap). The per-task review logged this as a minor "resize(0,0) guard"; it is a crash.
- Why: the desktop build is a shipping target (and later a `.scr` screensaver), and minimizing is routine. `renderer3d.resize(0, 0)` would also give the perspective camera a NaN aspect.
- Fix: `if (width <= 0 || height <= 0) return` at the top of `AntApp.resize`. **Fix before merge.**

**I2. Surface field stepping cost grows with the explored area, not with activity.** `sim/.../world/SurfaceMap.kt:77-79`, `ChunkedField.kt:59-98`
- What: every walking ant lays home scent (`homeScentPerSecond` 0.01, so about 0.005 per 10 mm cell per pass), which decays at 0.0002/s. Falling from 0.005 to `FADED` (1e-4) takes ln(50)/0.0002, about 19,500 s (5.4 h of sim time), so every chunk an ant ever walked on stays allocated for hours. `ChunkedField.step` then makes two full passes (update, then max) over 2,500 cells per chunk at 20 Hz. With the map covered (256 chunks) that is about 1.3M cell operations per tick: 26M/s at 1x and about 400M/s at 16x, before the trail field and the per-frame `trail.max()` in `DebugReadout`.
- Why: it is harmless on desktop at 1x, but M1b ends with the TV test (32-bit ARM) running this sim, and M3 adds far more foragers covering more ground.
- Fix: a pure decay composes exactly (`exp(-k * a) * exp(-k * b) = exp(-k * (a + b))`), so either step slow, non-diffusing fields every N ticks with `dt * N`, or store a per-chunk "last decayed at tick" and decay lazily on read and write (no per-tick cost at all). Also keep a per-chunk running max, so the release check does not need a second pass. **Fix in M1b, before the TV test.**

**I3. There is no real sim/render boundary, and the frame loop has no time budget.** `sim/.../World.kt:39-51`, `core/.../AntApp.kt:128-138`, all renderers and `DebugReadout`
- What: spec 2.4 asks for "read-only views" of sim state, but `World` exposes `rng`, a mutable `ants: ArrayList<Ant>`, a public `var unloads`, and a mutable `feedEvents`. `Ant` exposes every field as a public `var`. Five `core` classes iterate `world.ants`, `world.surface.foods` and the fields directly. The sim steps on the render thread, and `MAX_STEPS = 400` can never be reached (at most 0.25 s x 16 / 0.05 = 80 steps a frame), so the only brake is the 0.25 s dt cap. If a tick costs more than about 3 ms at 16x (thousands of ants plus brains in M2), each frame runs 80 steps, takes longer than 0.25 s, and the app sits at a few frames per second instead of slowing the sim.
- Why: `poc-design.md` section 7 plans a simulation thread that "copies flat arrays at a tick boundary" for snapshots, and M2 adds per-ant weights and a few thousand ants. As written, moving the sim off the render thread would race every renderer, and object-per-ant state will need a flat-array snapshot layer anyway.
- Fix: before M2 builds on this, decide one of two options. (a) Keep a single thread, and replace `MAX_STEPS` with a wall-clock budget (for example 6 ms of stepping per frame, dropping the backlog). (b) Add a render snapshot (flat `FloatArray`/`IntArray` of x, y, heading, role, space and state, filled at a tick boundary), and have `core` read only that. Make `rng` private, give `unloads` a private setter, and expose `ants` as `List<Ant>`. **The time budget in M1b (the TV test needs it); the boundary decision at the start of M2.**

**I4. `World.feedEvents` grows without bound.** `sim/.../World.kt:48`, `Forager.kt:164`
- What: every feed appends a `FeedEvent` that is never removed. With 60 foragers that is roughly 20,000 to 30,000 objects a day of sim time; with the thousands of foragers planned later, it is tens of MB a day. `feedsSince` in the Gruter test also scans the whole list.
- Why: a screensaver accumulates run time over weeks, and M7 would snapshot this list. It is test instrumentation living in the world state.
- Fix: keep per-source counters plus a bounded ring buffer (or route feeds through the M6 event bus and let the test subscribe). **Fix in M2.**

**I5. The world generators are not guaranteed to be bit-stable across platforms or JDKs.** `sim/.../world/NestGenerator.kt:30` (also `SurfaceMap.placePlants/placePrey`, and `cos`/`sin`/`atan2`/`exp`/`ln`/`pow` throughout the ant rules)
- What: `kotlin.math.sin` is `Math.sin`, which may differ by 1 ulp between HotSpot intrinsics, Android's libm and JDK versions. The clay band boundary `(sin(...) * 15).toInt()` can therefore move by a cell. Untouched nest tiles are never stored, because the design relies on regenerating them from the seed.
- Why: M7 persistence (snapshots holding only modified tiles) and "a world is reproducible from its seed" both assume the generator gives identical output on the TV, on desktop and after a JDK upgrade. Ant behavior drifting between platforms is acceptable, because snapshots carry ant state, but that should be stated.
- Fix: use `StrictMath` (or integer-only noise) in `NestGenerator` and in the plant and prey placement; a one-line change now. Document that a behavior run is reproducible per platform, not across platforms. **Fix in M1b (cheap), and in any case before M7.**

### Minor

- **M1. Building pheromone is written but never read.** `World.dig` adds it (`World.kt:77`) and `NestGrid.stepPheromone` decays it every tick, but nothing samples it. Spec 3.3 says it "attracts drops"; drops only happen on the surface spoil field. Record it as deferred to M2 (when brains sense it) so nobody assumes it works. M2.
- **M2. The world only works after `predig`.** Ants spawn at cell (600, 0) whether or not it is air, and with no air next to the first frontier `toFront` has no sources, so diggers bounce between IDLE and GO_DIG forever. Both scenarios predig, but M3's founding queen starts from the surface. Add a `require` or document the invariant now; handle founding in M3.
- **M3. Two full BFS rebuilds per dug cell.** `Excavation.refresh` bumps `version` (`Excavation.kt:51`) on every grid change, even when the frontier set is unchanged, so `NestPaths` rebuilds both `up` and `toFront`. That is cheap at M1 sizes (about 2,000 air cells), but it is O(air) per dig. Only bump `version` when `cells` actually changed; consider an incremental `up` update (a newly opened cell only needs min(neighbors) + 1 and a local relaxation). M3.
- **M4. The Gruter test does not check spec 4, check 1 in full.** `GruterScenarioTest.kt:50` asserts only the trail-following share for 30 foragers. The spec also says "both sources stay in use at similar rates". Add a feed-share assertion (for example the busier source below 0.7 in most seeds). M2, when the test is re-run with brains.
- **M5. There is no world-level determinism test.** `ForagerTest.theSameSeedGivesTheSameRun` covers only foragers, and only positions and state. Add one on `Scenarios.starter` (diggers, spoil, trail, nest version, the rng's next value) comparing a state hash after a few thousand ticks. It is the regression guard M2 most needs. Start of M2.
- **M6. `SpatialIndex.rebuild` clears 160,801 bucket heads every tick** (`SpatialIndex.kt:13`) regardless of ant count. Clear only the buckets used last tick. M2.
- **M7. `DebugReadout` scans everything every frame** (`trail.max()` over all trail chunks, `activeTileCount()` over all tiles x 9, four `ants.count` passes, `String.format`). Compute it at a few Hz. M1b (the TV shows the HUD).
- **M8. The nest camera flips between follow and frame.** `followed` defaults to the first forager (`AntApp.kt:82`), so the nest view follows it while it is inside and snaps to framing when it leaves (`AntApp.kt:147-149`). Start with `followed = null` in the nest view, or follow only after an explicit F. M6.

## Triage of minor-findings.md

| Finding | Verdict |
|---|---|
| T1 Material KDoc; stone radius "2, 5" vs "2 to 6 mm" | Drop the radius point: `2 + (u * 5).toInt()` gives 2 to 6, so the KDoc is right. KDoc on Material: M2 cleanup. |
| T2 `set` no-op materializes a tile | M2 (check `material(x, y) == m` before `materialize`). It matters for M7 snapshot size. |
| T2 generator never emits AIR (undocumented) | M2: document it, or assert it in `materialize`. The NestGenerator test already guards it. |
| T2 `stepPheromone` lacks `require(lifetime > 0)` | M2, folded into one `AntParams` `init` validation block. |
| T3 Excavation skips an unreachable block; aliased frontier list; weak tests; `Cell` churn | M2. The scripted plan is replaced there, so most of this goes away; until then return `cells.toList()`, or document the aliasing. |
| T4 PLAN-MANDATED clipped diffusion halo | Drop: resolved by poc-design 14.4. |
| T4 missing `require`s (diffusion * dt < 0.25, chunk bounds, divisibility); NaN positions; missing tests | M2. NaN matters there: `floor(NaN).toInt()` is 0, so a NaN ant would silently write into chunk (0, 0). Assert finite positions in `SurfaceMotion.advance`. |
| T5 `SurfaceMap.chunk` key aliases out-of-range coords | M1b (the 3D surface reworks chunks): add a bounds `require`. |
| T5 `placePlants`/`placePrey` not idempotent | Drop (M3 replaces food placement). |
| T5 no clearance around the entrance | M1b (collidable rocks and grass must not block the entrance). |
| T5 prey citation | M2 docs pass. |
| T6 `countNear` radius <= bucket not enforced | M2 (`require` in `countNear`, or size buckets from `crowdRadius`). |
| T6 `surfaceIndex` stale within a tick | M2: document it. |
| T6 `World.dig` lacks a diggable check | M2, must fix: brains will call `dig` directly, and it currently turns stone and water into air. |
| T6 KDoc gaps | M2 cleanup. |
| T7 import order | Drop, or let a formatter handle it. |
| T7 `goUp` stalls on UNREACHED | M2 (possible once brains or fills disconnect cells). |
| T7 loaded diggers use unloaded speed for run length | Drop (scripted digger is replaced in M2). |
| T7 unused `w` in `steerByGradient` | Drop, or clean up in passing. |
| T8 FEED on a depleted source still credits crop and a FeedEvent | M2. It is a real bug in the starter (prey loads go negative and phantom food is carried); fix by checking `food.loads > 0 && food in foods` at the end of FEED. |
| T8 constants outside `AntParams` | M2 (the parameter set becomes the genome's scripted baseline). |
| T8 determinism test too narrow | Superseded by M5 above; start of M2. |
| T9 `AntParams` KDoc claims a 75-forager crossover that no test measures | **Fix before merge**: reword to "30 foragers do not form trails, 150 do", or add a sweep. The KDoc is the calibration record M2 will rely on. |
| T9 loose rationale; `FloatArray` per emergence; no S = 0 test; thin seed margins | M2, when the calibration is redone with brains. |
| T10 chase camera teleport reads as speed; `placeAt` unclamped; pan y sign; missing tests | M1b (the camera is carried into the 3D surface). |
| T11 chunk build cost on the TV; overlay rebuild spike; pop-in; batch color reliance; size literals vs `ANT_LENGTH_MM`; Sprites KDoc | M1b. Ground textures and billboards are replaced; re-check whatever survives on the TV. |
| T12 Surface3dRenderer items | Drop with the renderer in M1b; carry over only the mipmap point. |
| T13 `MAX_STEPS` unreachable | M1b, as part of I3 (wall-clock budget). |
| T13 evict only from `drawTop` | M1b. Bounded today (worst case 256 chunk textures, 64 MB); fix when chunk rendering is rebuilt. |
| T13 `DebugReadout` locale | M6 (UI text pass). |
| T13 `resize(0,0)` guard | **Fix before merge**: it is a crash (I1). |
| T13 `manualNest` not shown in the HUD | M6. |
| T13 visual: grass occludes the ant | Drop: accepted by poc-design 14.2. |

## Assessment

Ready to merge: yes, after two small fixes (the `resize(0,0)` crash guard, I1, and the `AntParams` KDoc crossover claim). The architecture is sound for M1b and M2. The remaining Important items (field stepping cost, a frame time budget and the sim/render boundary, unbounded feed events, generator float stability) are not merge blockers, but should be scheduled into M1b and the start of M2 as noted.

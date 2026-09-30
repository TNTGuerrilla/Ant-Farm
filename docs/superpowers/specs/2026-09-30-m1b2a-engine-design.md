# M1b-2a: the engine for the TV

Status: approved in design discussion on 2026-09-30, awaiting spec review.
Parent design: `docs/design/poc-design.md` (sections 12 to 14). Follows M1b-1 (`docs/superpowers/specs/2026-09-30-m1b1-surface-sim-design.md`). The problems it fixes come from the M1b-1 final review, `docs/superpowers/notes/2026-09-30-m1b1-final-review.md` (items I3 and I5, and the render/simulation boundary under I4).

## 1. Goal

M1b-2 is split in two. **M1b-2a (this spec)** makes the engine ready for the TV: the simulation on its own thread in the manner of Minecraft's integrated server, rendering interpolated between ticks, a render-safe boundary between the simulation and `core`, the memory and allocation fixes the review scheduled, and an early benchmark run on the TV. **M1b-2b (a later spec)** builds the low-poly look against the budget measured here and ends with the full 60 fps TV test.

M1b-2a is done when:

1. The simulation runs on its own thread at a fixed 20 ticks per second times the speed setting, slows down rather than dropping frames when a tick runs long, and resets its schedule when more than 2 s behind.
2. Ants move smoothly at the display frame rate, interpolated between ticks.
3. `core` never reads the live `World` while the simulation thread runs; a test shows a threaded run equals a single-threaded run of the same seed.
4. The full-map height cache is about 8 MB instead of 64.5 MB, with every surface test and the Gruter test still passing.
5. The hot paths named by the review allocate nothing per call.
6. The TV runs the M1 app in its dream, and a hands-on session records milliseconds per tick, frames per second per view, and memory on the device.

Out of scope: low-poly meshes built from the SDF, ant models and walk cycles in 3D, grass and plants as models, lighting and colors, and the final 60 fps TV test (all M1b-2b).

## 2. The loop (Minecraft model)

Minecraft's server runs a fixed 20 ticks per second, sleeps for the rest of each 50 ms, lets a slow tick delay the next (so the world slows while frames continue), resets its schedule when more than 2 s behind, runs on its own "integrated server" thread in single player, and the client renders interpolated between ticks. M1b-2a adopts all five.

### 2.1 `SimRunner` (new, `core`)

- Owns the `World` and one simulation thread.
- **Schedule:** target tick interval `DT / speed` seconds of real time (50 ms at 1x, 12.5 ms at 4x, 3.125 ms at 16x). After each tick it sleeps until the next tick is due. If a tick ends after the next one was due, the next starts immediately (the world slows; no ticks are run in parallel or skipped silently).
- **Reset:** if the schedule falls more than 2 s behind real time, the next due time is reset to now (the backlog is dropped), and the event is logged.
- **Clock:** the runner reads time through a small clock interface, so tests use a fake clock.
- **Statistics:** milliseconds per tick (average and maximum over the last 10 s) and achieved ticks per second, exposed to the HUD and logged every 10 s.
- **Commands:** the render thread sends commands (set speed, pause, resume, set the overlay focus) through a thread-safe queue that the simulation thread applies between ticks. Commands never run mid-tick.
- **Lifecycle:** `pause()` finishes the current tick and stops ticking; `resume()` restarts the schedule from now (no catch-up after a pause); `stop()` ends the thread and joins it. `AntApp.pause/resume/dispose` call these, which covers the screensaver ending and restarting (the simulation pauses while the screensaver is not running).

### 2.2 Snapshots

- After every tick, the simulation thread fills a `Snapshot` and publishes it. Three buffers rotate (one being written, the latest published, one held by the renderer), and publication is a single atomic reference swap, so the renderer never sees a partly written snapshot and neither side blocks.
- A snapshot holds the tick number and the time it was published, and per ant: id, space, role, state, speed, heading, position (x, y, z), forward (fx, fy, fz) and normal (nx, ny, nz). It also holds the food list (position, body and stem sizes, kind), because prey can be eaten.
- Arrays in a snapshot are reused between ticks; filling one allocates nothing once the arrays have grown to the ant count.

### 2.3 Interpolation

- The renderer keeps the previous and the current snapshot. Each frame it computes `alpha = (now - current.publishedAt) / tickInterval`, clamped to [0, 1], and draws each ant at the linear blend of its previous and current position, forward and normal (vectors renormalized).
- An ant whose space changed between the two snapshots, or that is new, is drawn at its current state without blending.
- The 2D walk animation advances by interpolated distance, so legs keep pace with the smooth motion.
- When paused, the renderer draws the current snapshot (alpha 1).

## 3. The render-safe boundary

After M1b-2a, `core` reads only snapshots and published stores while the simulation thread runs.

- **Nest tiles:** when a tile's version changes, the simulation thread copies its cells into a published tile store (a map from tile index to an immutable byte array and version). `NestRenderer` reads only that store. The nest's air bounding box, air cell count and plan block also travel in the snapshot for the camera and the HUD.
- **Top-down overlay:** twice a second, the simulation thread computes the trail and spoil overlay for the chunks around the current overlay focus (sent by the renderer as a command) and publishes it as immutable per-chunk arrays.
- **Rocks:** when the simulation generates a chunk's rocks, it publishes the immutable list into a concurrent store; the debug renderer draws what has been published.
- **Ground:** the renderer keeps its own `HeightField` built from the seed (the base relief is a pure function of the seed); spoil heights come from the published spoil overlay. With two instances the height caches cost about 16 MB at full coverage, which is within budget. The overlay focus is the camera's focus in whichever view is shown, so the overlay always covers the ground being drawn.
- **HUD numbers** (counts by role and space, strongest trail, trail blocks, feeds, unloads, nest numbers) are computed by the simulation thread a few times per second and published.
- **Test:** a determinism test runs the same seed twice for 5,000 ticks: once stepped directly on one thread, once through `SimRunner` on its thread with a reader thread consuming every snapshot and store. Final ant states must be identical.

## 4. Memory and allocation fixes (from the review)

- **Height cache (I3):** the base relief grid becomes 4 mm spacing stored as `ShortArray` in steps of 0.001 mm (range covers the +/-16.5 mm relief). The full map is about 8 MB. The analytic slope and bilinear interpolation stay. Every surface test and the Gruter test are rerun; if the Gruter test fails, recalibrate by the M1 rules (listed knobs, one at a time, never loosen the test).
- **Hot paths (I5):** indexed loops instead of iterators in `SurfaceSdf.distance`, `stemAt`, `SurfaceMap.nearestFood` and `nearestPlant`; a primitive long-keyed open-addressing map in `Field3` (no boxed keys); preallocated color attributes and an int counter per pool in `DebugSurfaceRenderer`. None of these change results.
- **Trail-cost benchmark (I2):** a benchmark variant runs `Scenarios.starter` for 30 simulated minutes and reports milliseconds per tick, trail and home-scent block counts, and heap in use.

## 5. The early TV run

- The TV dream and the launcher's preview activity run the M1 app, starting in the 3D debug view (nest and top-down views one OK press away).
- **Hands-on session**, as in M0: Claude installs the app and sets the TV up (screensaver component, auto-start permission, a short timeout, with the old values recorded), the owner watches for a few minutes, optionally presses OK to cycle views and Back at the end, and says when done; Claude then reads the log and restores the TV.
- **Recorded:** milliseconds per tick at 1x and 16x, achieved ticks per second, frames per second in each view, and memory in use. These become the budget for M1b-2b. If the TV cannot hold 20 ticks per second for the starter colony, M1b-2b begins with simulation cost cuts.

## 6. Testing

- `SimRunner` schedule with a fake clock: ticks at the target interval, a slow tick delays the next without skipping, a lag beyond 2 s resets the schedule, pause and resume restart from now, speed changes apply between ticks.
- Snapshot hand-off: under concurrent publishing and reading, the reader never sees a snapshot whose tick number disagrees with its contents.
- Interpolation math: blend and clamping of alpha, no blending across a space change.
- Threaded equals single-threaded (section 3).
- The compact height grid: continuity across chunk edges, amplitude, spoil, slope (the existing `HeightFieldTest`), plus memory per chunk.
- The trail-cost benchmark (section 4).
- All existing tests, with the Gruter test rerun after the height-grid change.

## 7. Code structure

- `core/engine/SimRunner.kt`, `core/engine/Snapshot.kt`, `core/engine/Published.kt` (tile store, overlay store, rock store, HUD numbers), `core/engine/Clock.kt`.
- `core/AntApp.kt` owns a `SimRunner` instead of stepping the world; renderers take snapshots and stores instead of the `World`.
- `sim` changes: the height grid (`HeightField`), the long-keyed map (`Field3`), indexed loops; small read hooks the runner needs (for example a callback or listener when a chunk's rocks are generated), all side-effect free for the simulation.

# Ant Farm (placeholder name): ant colony screensaver

A passive, realistic simulation of a wild Lasius niger colony in 2D cross-section. Each ant has a small neural network that evolves across colony generations and learns during its life. It ships as a Google TV screensaver (libGDX `AndroidDaydream`) on the owner's TCL 65QM6K, with a Windows desktop build.

## Status

**M1b-2b (simulation cost on the TV) is done (2026-10-01).** The TV build is now the non-debuggable `profile` build type (`:android:assembleProfile`, debug-signed until M8); the debuggable build had been most of M1b-2a's slowness. A headless `BenchmarkActivity` (started over adb, no remote; see the README) runs `core/engine/Benchmark` and logs a per-phase breakdown from `sim/TickProfile`. `Scenarios.colony1000` is the 1,000-ant benchmark colony. Tier 1 cuts are bit-exact, guarded by golden values in `FingerprintTest` (plain arrays and food indexed by chunk in `SurfaceSdf.distance`, food changes only through `SurfaceMap.addFood`/`removeFood`, a spatial index that clears only used buckets, antenna trig once, unrolled `Field3` corners and a 4-block cache). Tier 2 is an open-ground fast path: where `SurfaceSdf.isOpenGround` finds no shape within reach, `SurfaceWalk` puts the ant straight on the height grid (about 80% of steps); the Gruter test needed `crowdLevel` 2, and its crowded-switch check has no spare seed (more seeds wait for M2, by the owner's decision). On the TCL, colony1000 runs at 12.3 ms per tick (p99 17.5) against a 25 ms target (baseline 24.3, tier 1 17.3), the starter holds 4x, and the dream holds 60 fps in all three views at about 2 ms per tick, with the M1b-2a 3D hitch gone. Details: `docs/superpowers/notes/2026-10-01-m1b2b-tv-cost.md`. On TV sessions, stream logcat to a file (the TCL's buffer wraps within minutes). Next is **M1b-2c: the low-poly look and the final 60 fps TV test**.

**M1b-2a (the engine) is done (2026-09-30, with an early TV run).** `core/engine` runs the `World` on its own thread in the manner of Minecraft's integrated server (`SimRunner`: a fixed 20 ticks/s times the speed, sleep when ahead, run at once when behind, reset when more than 2 s behind, commands applied between ticks, failures logged and shown on the HUD). After every tick it fills a `Snapshot` in a lock-free triple buffer and refreshes the `Published` stores (nest tiles, trail and spoil overlays, rocks, HUD numbers). `core` never reads the live `World` after the runner starts; renderers draw ants interpolated between the last two snapshots (`AntStates`), and the 2D legs advance by interpolated distance. A test shows a threaded run equals a single-threaded one. The height cache is a 4 mm grid of 16-bit values (about 8 MB for the full map instead of 64.5 MB); after it, `GruterScenarioTest` needed `crowdLevel` 3 and `saturationLevel` 10 and has no seed to spare on the share and switch checks. `Field3` uses a primitive long-keyed map. Desktop benchmark: the starter colony with 30 minutes of real trails runs at 0.23 ms per tick with 23 MB of heap. **On the TCL** the dream now runs the app (3D view first; OK cycles views; Back exits): 20 ticks/s at 1x with about 13 ms per tick (so it tops out near 3.8x), 60 fps with a flat p99 in the nest and top-down views, and a periodic 130 ms hitch in the 3D debug view (likely the ground rebuild on spoil changes); PSS 58 to 68 MB. These numbers are the budget for **M1b-2b: the low-poly look and the final 60 fps TV test**; details and follow-ups are in `docs/superpowers/notes/2026-09-30-m1b2a-tv-session.md`.

**M1b-1 is done (desktop, 2026-09-30; no TV run).** `sim` now holds a signed-distance-function surface: ground relief with a spoil mound, pebbles and rocks with overhangs, and aphid-plant stems and prey lumps. A sparse 3D pheromone field (`Field3`: 10 mm trail voxels, 25 mm home-scent voxels) replaces the 2D fields, and surface walking (`SurfaceWalk`) keeps ants within 0.5 mm of the surface. Foragers smell plants within 150 mm, climb stems to the aphids and come back down. Trails do not diffuse (a narrow line on the ground, as in the owner's model); with that, `GruterScenarioTest` passes on the 3D surface with M1's own values (markAmount 2.25, trailThreshold 1.96), though the crowded-switch check has no seed to spare (see `AntParams` KDoc). `core` reads fields only through side-effect-free calls (`peek`, `max`, `projectMax`); a determinism test checks that rendering never changes a run. `core` has a 3D debug view (ground mesh, rocks, stems and ants as simple shapes, with the chase camera along the ant's own normal), a 6 ms per-frame simulation budget, a throttled HUD, and tripod-gait leg animation for the 2D ants. The desktop benchmark runs 1,000 surface ants at about 3.25 ms per tick. M1b-2 took its work list from `docs/superpowers/notes/2026-09-30-m1b1-final-review.md` (TV-scale cost, height-grid memory, a Minecraft-style sim thread with interpolated rendering, meshes built from the SDF so ants no longer appear to clip into rocks).

**Milestone M0** (the TV stub) is built: the four Gradle modules exist, and `core/.../stub/StubApp.kt` logs D-pad keys in an interactive dream, draws 1,000 moving sprites and times a 20 MB background atomic write. **M0 passed on the TCL on 2026-09-29.** Every remote key (arrows, OK, Back) reached the interactive dream, and Back ends it. On the Mali-G52, 8,000 sprites held 60 fps; 16,000 dropped to 39 to 50 fps. A 20 MB atomic write takes about 170 ms (mostly the fsync) with no slow frames. TCL firmware blocks the dream until the app gets `appops ... AUTO_START allow`. Next is M1 on desktop.

Build notes: Gradle needs JDK 17 (`JAVA_HOME` defaults to 1.8 on this machine). libGDX 1.14.2 pulls in AndroidX and needs compileSdk 36. The TCL's userspace is 32-bit ARM (`armeabi-v7a`), and the emulator is x86.

## Read first

- `docs/design/poc-design.md`: the proof-of-concept design. It covers architecture, systems, the build order (M0 to M8) and open decisions.
- `docs/research/simulation-reference.md`: biology rules and parameters with confidence tags. This is the source of truth for simulation values.
- `docs/research/notes/01` to `08`: detailed literature notes with full citations.

## Settled decisions

These were decided with the owner. Do not re-litigate them.

- Kotlin everywhere that ships. Gradle modules: `sim` (pure Kotlin core), `core` (libGDX rendering and UI), `desktop` (LWJGL3, jpackage .exe/.scr), `android` (`AndroidDaydream`). Python is for offline analysis only.
- Lasius niger, as realistic as possible, in a wild setting.
- Evolution between colonies through a regional gene pool. Learning is Baldwinian: the genome holds starting weights plus plasticity rules, and learned weights are never inherited.
- Two clocks: real-time ant movement, and a compressed calendar (start at 10 real minutes per simulated day). Memory decay runs on the calendar clock.
- The simulation pauses while the screensaver is not running.
- Camera: constant level of detail. Start close, ease out to a fixed comfortable zoom, then pan between points of interest as the nest grows. A minimap shows the whole nest.
- Follower camera, newspaper-style event log with simulation dates, "while you were away" catch-up, counters by caste plus colony generation, and remote D-pad controls.
- When a colony dies, a new queen from the gene pool moves into the abandoned nest and cleans it up.
- Persistence: an append-only genome log, periodic atomic snapshots and a best-effort flush on exit.

## Related project

The owner's Labyrinth screensaver is a working Kotlin + OpenGL `DreamService` on the same TV: `J:\Documents\Development\Programming\Python\MazeScreensaver\android`. Use it as a reference for the manifest, dream settings and build scripts.

## Owner preferences

- Never use em-dashes or emojis anywhere, including in code, comments, docs and commits.
- Run builds (Gradle and similar) as background tasks.

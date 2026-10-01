# Ant colony screensaver: proof-of-concept design

Status: draft for review. Biology and parameters come from `docs/research/simulation-reference.md`.

## 1. Goal

A passive, realistic simulation of a wild Lasius niger colony, shown in 2D cross-section like an ant farm. Each ant has a small neural network that is evolved across colony generations and adapts during the ant's life. The viewer watches the colony found, grow, learn, expand, suffer setbacks, and eventually die and be replaced. The target is a Google TV screensaver (`DreamService`) on a TCL 65QM6K, with a desktop build for development.

**Proof-of-concept success criteria:**

1. A colony goes from founding queen to about 75 workers, and switches from solo foraging to trail recruitment on its own.
2. Lifetime learning is visible: experienced foragers run straighter, faster routes than naive ones.
3. Evolution is measurable: later colony generations outperform earlier ones (for example, time to reach 75 workers).
4. It runs on the TV at a stable frame rate with no visible save hitches.
5. It survives the screensaver being interrupted without losing evolutionary history.

**Not in the proof of concept:** multiple species, switching between colonies, store release.

## 2. Platform and stack

**Decided: Kotlin everywhere that ships, with libGDX for rendering.** Google TV runs Android, and Nuitka cannot target it. One Kotlin core keeps behavior, saves and genomes identical across platforms.

Gradle modules:

- **`sim`:** a pure Kotlin/JVM core with no libGDX or Android dependencies. It holds the world, ants, brains, evolution, events and persistence. It is a deterministic fixed-tick simulation, unit-testable on desktop.
- **`core`:** the libGDX rendering, camera, UI overlay (event log, counters, minimap) and input mapping. Shared by both platforms.
- **`desktop`:** the libGDX LWJGL3 launcher. Used for development and debugging, and for headless fast-forward runs that seed evolution offline. It is packaged for Windows as an .exe/.scr with a bundled runtime (jpackage).
- **`android`:** the libGDX `AndroidDaydream` backend as an interactive screensaver, plus a preview activity. It follows the Labyrinth project's structure for the manifest and the dream settings.

libGDX's daydream backend is still maintained (1.14.1 fixed an input bug in it). Python stays in the toolbox for offline analysis of exported run data. It does not ship.

## 3. Time

- **Behavior clock:** real time. Walking at about 20 mm/s, 3-minute foraging trips, pheromone decay.
- **Calendar clock:** compressed: 1 simulated day = 60 real minutes (changed from 10 by the owner on 2026-10-01, section 15). It drives development, aging, lifespans, seasons, weather, day and night, and memory decay.
- **Winter** (4 to 5 months) is fast-forwarded with a short overwintering sequence. So is any empty-nest period.

## 4. World

- **Space:** a 2D vertical slice. A fine underground grid (about 1 mm cells, so tunnels are 4 to 5 cells wide) covers roughly 1 to 2 m around the nest down to the water table. A chunked surface strip about 8 m long carries plants, aphids and trails (reference section 13). Neighbor colonies sit just beyond the strip edges.
- **Soil cells:** material (air, soil, stone, clay, water), moisture, temperature, and a decay timer for abandoned tunnels.
- **Scalar fields** (coarser grids, with diffusion and decay):
  - trail pheromone (decay scales with trail strength and temperature)
  - home-range scent (slow, colony-specific)
  - building pheromone (13 to 20 min lifetime)
  - optional weak CO2
- **Environment:**
  - a soil temperature profile (large swings at the surface, damped below about 40 cm)
  - a water table as a hard floor
  - weather: rain, heat, drought and cold, driven by a seasonal calendar
- **Surface:** plants with aphid colonies (the main sugar source), scattered prey and food items, sun-warmed stones.

## 5. Ants

**Castes and stages:** queen, workers (callow, nurse, middle, forager), brood (egg, larva, pupa), alates (gynes and males), corpses.

**Innate motor primitives, executed by code:** walk and steer, pick up, drop, dig pellet, deposit pheromone, feed or receive (trophallaxis), carry brood, groom, attack or flee, rest.

**The brain chooses and modulates; the primitives execute.** An untrained network would produce random behavior, so the brain drives the decision layer: where to steer, which primitive to perform, and when to lay trail. The body handles the rest.

### Brain

- **Inputs** (about 24, see reference section 4):
  - left and right pheromone
  - odour channels
  - home scent
  - home vector (noisy path integration)
  - coarse vision times light level
  - temperature and wetness
  - contact state
  - crop fullness
  - crowding count
  - age or experience
  - a colony-state signal (brood hunger, returner rate)
- **Network:** a small recurrent network (1 hidden layer of about 16 to 24 units, plus recurrent state). That is about 1,000 to 2,000 weights.
- **Outputs:** turn, speed, primitive selection (softmax), deposit amount, and a "go outside" drive.

### Plasticity (Baldwinian)

- The genome stores initial weights plus per-connection Hebbian plasticity coefficients, gated by a reward or punishment neuromodulator signal (food found, near-drowning, attack).
- Each ant has two memory timescales: **fast weights** (one-trial, decaying within about 1 calendar day) and **slow weights** (after about 3 or 4 reinforcements; last for days).
- Updates apply only to actions the ant chose.
- Aversive place learning is slower than aversive odour learning.
- The pheromone-following gain is clamped to be non-negative.

**Colony-level genome:** parameters shared by all workers of a colony:
- the distribution of response thresholds
- the "desired crop volume" mean and variance
- nest volume per worker
- brood target temperature
- alate allocation

These are where heritable colony "personality" lives.

## 6. Colony life and evolution

**Life cycle:**
1. A nuptial flight (July to August, weather-gated) lands a queen.
2. Claustral founding: nanitics emerge at about day 40, and the colony grows.
3. At about 75 workers the colony switches to recruitment.
4. At about 3 years and several thousand workers, it starts producing alates.
5. The queen dies (after 10 to 29 years), and the colony fades out over about a year.

Growth and depth limit themselves (reference section 12). There are no hard caps.

**Regional gene pool:** a small population of genomes (for example 32) stands in for the colonies around the visible one.
- When the on-screen colony produces alates, the gynes' genomes (the queen's genome, mutated and recombined with a male drawn from the pool) enter the pool, weighted by how many alates the colony produced.
- Background pool members age out over time.
- A new founding queen is always drawn from the pool, so evolution keeps accumulating across colony deaths. The "colony generation" counter increments with each new founding.

**Offline seeding:** the desktop build runs headless at high speed to evolve a starting pool, so the first colony on a new install already behaves plausibly. Later colonies improve on it.

**Restart:** when the last ant dies, the screen shows the empty nest decaying while seasons pass. At the next flight season, a queen lands, attracted to the abandoned entrance, and moves in. The newcomers clear old corpses and brood (or eat them if starving), then reuse and extend the old tunnels.

## 7. Threats

These are the functional archetypes from reference section 11:

- background forager hazard
- sit-and-wait predators
- rival ants
- phorid flies
- fungus
- woodpeckers
- nuptial-flight losses
- colony death
- floods

Each archetype has a generic look and a realistic rate. Colony responses are innate and emerge from the brain inputs: pausing foraging after heavy losses, plugging entrances in rain, moving brood.

## 8. Persistence

- **Genomes and the gene pool:** an append-only log. Each birth writes a genome record, and each death writes a tombstone. The log is compacted on a background thread.
- **World and ant state:** a periodic snapshot every 30 to 60 seconds. The simulation thread copies flat arrays at a tick boundary, and a writer thread saves them with `FileChannel`.
- **Terrain:** chunked; only changed chunks are written.
- **Every file is written atomically:** write `*.tmp`, flush it, then rename it over the old file.
- **`onDreamingStopped`:** a best-effort flush, never relied on.
- **Time off-screen (decided):** the simulation pauses while the screensaver is not running, so a week with the TV off never changes the colony's story. "While you were away" covers everything since the viewer last interacted, because the screensaver often runs to an empty room.

## 9. Presentation

- **Camera: constant level of detail, panning instead of zooming out.**
  - A new colony starts close in, then eases out to a fixed comfortable zoom where ants stay readable. For example, an 8 to 12 px ant at 1080p is 2 to 3 px per mm, so the view is about 0.6 to 1 m wide.
  - Once the nest outgrows the view, a camera director pans between points of interest: active digging fronts, busy chambers, the brood pile, surface trails.
  - A small minimap shows the whole nest and trail network, with the current view outlined.
  - Remote control: pan manually, or pick a chamber to watch it being built.
- **Follower camera:** every ant has an interest score, for example:
  - producing an event
  - in danger
  - carrying food
  - on its first trip outside
  - lost
  - fighting
  - carrying brood during a flood

  Periodically, the camera picks a top-scoring ant, zooms in, follows it for 2 to 4 minutes, then fades back to the colony view.
- **Event log:** newspaper-style headlines with in-simulation dates, from a typed event bus. The simulation emits structured events, and a text layer formats them. Importance ranking decides what is shown live and what goes into the catch-up summary.
- **Catch-up:** when the screensaver starts, a "While you were away" card shows the top headlines since the last viewing.
- **Counters:**
  - queen status
  - workers by role (nurse, middle, forager, idle)
  - brood (eggs, larvae, pupae)
  - alates
  - colony age
  - colony generation

## 10. Remote input

The dream runs with `isInteractive = true` and handles D-pad keys itself. Back and Home exit.

- **Proof-of-concept controls:** scroll the event log, start the follower camera on the current top ant, cycle through the top 10 interesting ants.
- **Later:** a settings drawer, switching between colonies, other species.

This needs an early test on the TV (milestone M0) to confirm the firmware passes D-pad keys to an interactive dream.

## 11. Performance budget (TV)

- **Simulation:** 20 ticks/s, with brains evaluated at about 10 Hz per ant (staggered). 1,000 ants x 2,000 weights x 10 Hz is about 20M multiply-adds/s. That is trivial.
- **Rendering:** at 1080p (the TV upscales), with instanced sprites and field textures uploaded as needed.
- **Memory:** under about 150 MB. Snapshots are a few tens of MB at most.

## 12. Build order

| Milestone | Deliverable | Proves |
|---|---|---|
| M0 | TV stub dream: logs key presses; draws 1,000 moving sprites; times a 20 MB background write | Remote input works; the render and save budgets hold |
| M1 | Desktop: world grid, soil, fields, scripted ants (no brains) that dig, forage and lay trails; renderer with auto-fit camera | The world model and rendering are sound; reproduces the Gruter 2012 foraging switch with scripted rules |
| M1b-1 | Surface simulation: a signed-distance-function heightfield ground with pebbles and rocks, 3D pheromone fields (10 mm trail voxels, 25 mm home-scent voxels), surface walking keeping ants within 0.5 mm, scripted surface foraging with stem climbing | The 3D surface works; Gruter test passes on the surface after recalibration |
| M1b-2a | The engine: a simulation thread at a fixed 20 ticks/s (Minecraft model), snapshots and interpolated rendering, a render-safe boundary, a 4 mm 16-bit height grid; the TV dream runs the app | Done 2026-09-30. The TV holds 20 ticks/s at 1x with about 13 ms per tick, and 60 fps in the nest and top-down views (notes: `docs/superpowers/notes/2026-09-30-m1b2a-tv-session.md`) |
| M1b-2b | Simulation cost on the TV: a non-debuggable profile build, a headless TV benchmark, bit-exact cuts (tier 1) and an open-ground fast path (tier 2) | Done 2026-10-01. The 1,000-ant colony runs at 12.3 ms per tick on the TV (target 25 ms); the starter holds 4x; the dream holds 60 fps in every view (notes: `docs/superpowers/notes/2026-10-01-m1b2b-tv-cost.md`) |
| M1b-2c | Low-poly look (RuneScape style): low-poly models for ants, rocks, grass and plants; the final TV test | Done 2026-10-01. The 3D view holds 60 fps on the QM6K with the starter and with 1,000 ants (0 frames over 25 ms); ants stand on rocks and ground as drawn (notes: `docs/superpowers/notes/2026-10-01-m1b2c-tv-final.md`). M1b is complete |
| M2 | Brains replace the scripts; plasticity; fast and slow memory | Learning targets from the reference (75% after 1 visit, and so on) |
| M3 | Colony life cycle, brood, castes, seasons, the two clocks | Founding to 75 workers and the recruitment switch |
| M4 | Gene pool, alates, restart sequence; headless offline evolution | Later colony generations measurably improve |
| M5 | Threat archetypes and weather | Realistic mortality; floods are setbacks |
| M6 | Event bus, log, catch-up, counters, follower camera | Presentation |
| M7 | Persistence (log, snapshots, atomic writes) | No hitches; survives interruption |
| M8 | Android `DreamService` port, remote controls | Runs on the QM6K |

M0 runs first because it is cheap and de-risks the TV. M1 to M7 are desktop-first.

## 13. Decisions from review (2026-09-29)

1. **The outside world:** the owner cares less about its size than about how it looks.
   - The surface loads as chunks, and each chunk shows environmental features: plants, stones, food, resources, and the ant lines running to and from them.
   - **Follower camera, third-person style** (as in GTA 5, Elden Ring or Pragmata): the camera trails the ant with lag and damping. An ant turning in place does not pan the camera; the camera only follows once the ant moves off. This replaces the "zooms in, follows" wording in section 9.
   - **Distance view:** the M0 emulator look (small ant silhouettes on a plain soil color, 12 px at 1080p) is right from a distance, and the ant shape is approved.
   - **Close views and the nest:** sprites need more detail, so a viewer can tell what each ant is doing (carrying brood, food or a soil pellet, digging, feeding another ant). This needs walk-cycle and action animation.
   - **Art:** the owner is not an artist. Claude either produces the sprites and animation (procedural or hand-authored pixel art) or finds an open-source, MIT-style licensed sprite set that is good quality and fits the environment. Pick one art style and keep everything in it.
   - **Later, not now:** 3D. The TV may get a 2.5D version (flat sprites with depth and parallax), and the desktop a real 3D version with modelled environment. Decide when that point comes.
2. **Gene pool tuning: a litmus test.** Build a repeatable headless test that runs several genome samples through the same scenarios and ranks how they do. Use it to tune mutation rates and learning parameters so evolution keeps real uncertainty: tuned too far one way the colonies stay dumb, too far the other way they become smart too fast. Evolution is the point of the project, so this test is a first-class tool (M4).
3. **Neighbor colonies** touch the on-screen colony in only two ways: they **attack** it, and when it dies a new queen from the region **takes over** the nest. No trade, territory map or visible neighbor nests otherwise.

## 14. Decisions after M1 (2026-09-30)

1. **The surface becomes low-poly 3D (RuneScape style)** in a new milestone, M1b, before the brains. The reasons are that rocks and similar objects need collision and must be crawlable, and flat 2D ant sprites do not look good up close. The nest stays a 2D cross-section. Low-poly models with simple shading are within the TV's budget; M1b ends with the TV test that M1 skipped.
2. **Keep the M1 chase-camera angle** (about 70 mm behind and 35 mm above the ant). Grass clipping between the camera and the ant is expected and fine.
3. **Debris for a later milestone:** leaf litter, pine-needle-like twigs and similar objects that ants interact with and crawl over, to make the surface more interesting.
4. **Trails are narrow.** Trail pheromone is laid in short streaks on the ground and followed by comparing two antennae a few millimeters apart, so the trail field stays about 1 to 3 cm wide (1 cm grid, small diffusion). Diffusion lost at the edge of an unallocated chunk is negligible at that width and is left as is.
5. **M1 closed without a TV run.** The TV dream keeps the M0 stub until M1b.

## 15. Decisions after the M1b-2c desktop look check (2026-10-01)

1. **A simulated day lasts 60 real minutes**, for the whole calendar (light, development, seasons, memory decay), not 10. A year is about 365 hours of screensaver time.
2. **Aphid plant stems are 25 to 60 cm tall** (were 50 to 120 cm).
3. **Look adjustments in M1b-2c:** darker ants; a follow camera that eases into the ant's roll and pitch; no grass inside rocks; smoothed visible turning; ant sizes varying from 3 to 5 mm; a dark entrance hole with a divot; lighter nest tunnels; matching dirt colours, rocks and grass between the 3D and overhead views, with nothing cut off at chunk borders.
4. **Backlog for their own design passes** (suggested placement):
   - **Mound building (after M1b, before or with M3):** a visible mound that grows around the entrance, built the way Lasius niger really builds it (research where spoil is deposited and whether the mound grows from the inside out), shown on the surface and in the nest cross-section. Today spoil raises the ground by only 0.01 mm per pellet per square centimetre and diggers drop pellets up to 40 mm out by a chance rule.
   - **Nest architecture (M3):** slanted and curved tunnels, dips and rises, chambers of different kinds, instead of the scripted rectangular plan.
   - **Per-ant collision (with M2 or its own milestone):** ants block each other, queue at aphids, find gaps or wait, and occasionally climb over one another; it changes behaviour, calibration and TV cost.
   - **Speed varying with size (M3):** per-ant speed linked to the 3 to 5 mm size range.
   - **Weather and a short winter scene (M5 or later):** the sky state is built to be driven by it.
   - **Nest digging stall (M2, observed 2026-10-01 on desktop):** after a while the nest ants stopped moving below the top centimetre or so and digging made no visible progress. Investigate with the M2 behaviour work, since the brains replace the scripted digger.
5. **M1b-2c look, as built:** flat-faceted ground (10 mm facets near the camera, coarser in the outer ring) with irregular faceted specks; smooth rocks fitted to the simulation surface; faceted aphid stems, aphid clusters and prey; patchy instanced grass that the spoil mound buries; the fused low-poly ant (instanced, shader walk cycle, gaster swelling with the crop, carried pellet or prey piece, sizes 3 to 5 mm, soft shadows); a day cycle with a blue day sky and a near-black night sky that stays readable; the same sky over the nest view; matching soil, rocks and grass between the 3D and overhead views.
6. **Obstacle detours:** ants that stop making progress toward a plant or the entrance in sight detour to a random side, longer each time, instead of sticking on rock faces.

# M1: world model, scripted ants and first renderers

Status: approved in design discussion on 2026-09-29, awaiting spec review.
Parent design: `docs/design/poc-design.md` (milestone M1 in section 12). Parameter values come from `docs/research/simulation-reference.md`; section numbers below refer to that file.

## 1. Goal

Build the world the colony lives in, fill it with scripted ants (no neural networks yet) that dig, forage and lay trails, and show it on desktop in three views. M1 also proves that a rough 2.5D surface view runs on the TV.

M1 is done when:

1. Scripted ants dig a plausible starter nest: tunnels 4 to 5 mm wide, chambers about 6 mm tall, spoil carried out and piled.
2. A seeded headless test reproduces the Gruter 2012 foraging results: little trail formation below about 75 foragers; symmetry breaking onto one of two equal food sources above it; switching to a richer source added later.
3. Desktop shows the nest view, the top-down surface view and the 2.5D surface prototype, with debug controls.
4. The 2.5D prototype runs in the TV dream, and a hands-on test on the TV reports its frame rate.

Out of scope: brains and plasticity (M2), brood, castes by age, the calendar clock, seasons (M3), gene pool (M4), threats and weather (M5), event log and polished follower camera (M6), persistence (M7).

## 2. World model (`sim`)

The world is two connected spaces. Every ant is in exactly one of them.

### 2.1 Surface

- A flat 2D map in simulation terms, 8 x 8 m, with the nest entrance at the center. (Reference section 13: territory about 36 m2, trails 0.5 to 2 m, rarely up to 5 m.)
- Divided into chunks of 50 x 50 cm (16 x 16 chunks).
- Each chunk's contents are generated from the world seed and the chunk coordinates, so any chunk can be rebuilt without storing it:
  - ground cover: bare soil, grass tufts, leaf litter;
  - stones;
  - 2 to 4 aphid plants per world, placed 0.3 to 3 m from the entrance (reference section 13); each is a food source with a honeydew rate;
  - scattered prey items (solid food).
- Scalar fields at 1 cm resolution, stored per chunk and allocated only when a chunk first holds a nonzero value:
  - **trail pheromone:** decays 0.4% per second (reference section 5, Gruter 2012 model); a small diffusion term;
  - **home-range scent:** laid slowly by every walking ant, decays slowly; used by the scripted ants only as a weak "near home" cue.
- A chunk whose fields have all decayed below a threshold drops its field storage again.

### 2.2 Nest

- A vertical cross-section through the entrance: 1.2 m wide, 1 m deep, on a 1 mm grid. The water table is a hard floor at a seeded depth near the bottom.
- Cell data: material (air, soil, stone, clay, water) and moisture. A building-pheromone field (lifetime 13 to 20 min, reference section 7) lives on the same grid.
- **Tiles, created lazily.** The grid is split into 64 x 64 mm tiles. A tile that has never changed has no storage: its cells come from a seeded generator (soil layers, occasional stones and clay lenses, the water table). A tile gets real storage the first time a cell in it changes.
- **Active set.** Per-tick work (field decay and diffusion, frontier bookkeeping) only visits active tiles: those containing air, those adjacent to air (the digging frontier), and those with nonzero building pheromone. Solid tiles far from tunnels are never iterated.
- The entrance is a short vertical shaft at the top center of the slice.

### 2.3 Entrance handoff

- An ant that walks into the entrance area on the surface leaves the surface and appears at the top of the entrance shaft in the nest, and the reverse.
- The handoff happens at a tick boundary and keeps what the ant carries.

### 2.4 Ticks and determinism

- Fixed 20 ticks per second of behavior time. M1 runs only the behavior clock.
- All randomness comes from seeded generators owned by the world, so a run is fully reproducible from its seed.
- The sim exposes `step()` and read-only views of its state. It can run faster than real time in headless mode.

## 3. Scripted ants (`sim`)

Each ant is a small state machine over the innate primitives in the parent design (walk and steer, pick up, drop, dig pellet, deposit pheromone). In M2 the decision layer is replaced by a neural network; the primitives and the world stay.

### 3.1 Movement

| Rule | Value | Source |
|---|---|---|
| Surface speed | 20 mm/s | Ref. section 10 |
| Shaft speed (vertical travel) | 8 mm/s | Ref. section 10 |
| Carrying a pellet or solid food | 0.8x speed | Ref. section 10 |
| Carrying liquid (crop) | no slowdown | Ref. section 10 |
| Search walk | straight runs of exponential length, then turns; more tortuous going out, straighter going home | Ref. section 5 |
| Homing | path integration with error that grows with distance travelled | Ref. section 4 |

Underground, ants move through air cells only, steering along tunnels.

### 3.2 Foraging and trails

- Steering on trails: turn in proportion to (L - R) / (L + R), with L and R sampled about 1 cm ahead to either side (ref. section 4).
- A forager that finds food feeds (about 60 s for honeydew), returns, and unloads at the nest (about 60 s).
- Trail laying on the return trip only, and only if the ant filled its crop to its individual desired volume (ref. section 5). Each ant draws its desired volume at creation.
- About 14% of foragers never lay trail.
- Deposition: about 1.5 to 3 marks per 5 cm; reduced when existing pheromone is high (saturation) and when crowded (up to 5.6x less).

### 3.3 Digging and building

- Digging starts when the nest has too little volume per ant and stops when there is enough (ref. sections 7 and 12).
- Diggers remove frontier cells as pellets, carry them up and out, and drop them near the entrance. One pellet is one 1 mm cell (1 mm3). Real pellets are about 0.2 mm3, so digging time per cell is set to about 5 real pellets' worth, which keeps excavation speed at the reference rate of 1 to 3 mm3 per ant per hour of digging.
- Pellet drop and pick-up follow the Khuong 2016 rules (ref. section 10): drop 0.025/s, plus 0.11/s per pellet already present, pick-up 0.029/s; building pheromone attracts drops.
- Target geometry: tunnels 4 to 5 mm wide, chambers about 6 mm tall and 12 mm wide.

### 3.4 Roles

- In M1 each ant has a fixed role: digger or forager. The role mix is a scenario setting. Age, task switching and response thresholds arrive in M3.

## 4. Validation: the Gruter 2012 foraging test

A headless, seeded test in `sim`:

- Scenario: nest plus two identical food sources at equal distance.
- Runs at several forager counts, from well below 75 to well above it. Several seeds each.
- Checks:
  1. **Below about 75:** no stable trail. Both sources stay in use at similar rates.
  2. **Above about 75:** a trail forms and most traffic locks onto one source (symmetry breaking).
  3. **Switching:** a richer source added after the lock-in draws the colony over within tens of minutes, as long as crowding is present.
- Pass criteria are statistical (majority of seeds), with thresholds written into the test.

## 5. Rendering and cameras (`core`, `desktop`, `android`)

### 5.1 Views (cycled with a key on desktop)

1. **Nest view.** The cross-section. Soil drawn from the tile grid; only visible tiles are uploaded as textures and re-uploaded when changed. Ants use the M0 sprite.
2. **Surface top-down.** The chunked map with plants, stones and food, and an optional pheromone overlay.
3. **Surface 2.5D prototype.** A perspective camera behind and above one chosen ant, over a textured ground plane. Ants, grass and plants are upright flat sprites (billboards). Placeholder art.

### 5.2 Nest camera

- Frames the active part of the nest and zooms out as the nest grows.
- Never zooms out past a minimum on-screen ant size, so every ant stays visible and trackable. Starting value: 8 px of ant length at 1080p, as one tunable constant.
- Once the nest is bigger than that zoom can show, it pans instead of zooming further. In M1 the pan is manual or follows the selected ant; automatic points of interest come in M6.

### 5.3 2.5D chase camera

- Third-person game style: an ant turning in place does not swing the camera. When the ant moves off, the camera eases in behind it with lag and damping.
- Height, distance and pitch are tunable constants.

### 5.4 Debug controls (desktop)

- Pan, zoom, pause, speed 1x / 4x / 16x, select an ant to follow, cycle views, toggle the pheromone overlay.
- A text readout: tick rate, ant counts by role and location, strongest trail value, active tile count.

### 5.5 TV

- The dream runs the M1 world and shows the 2.5D surface view with the M0 frame-rate overlay.
- Remote: arrows pick the next or previous ant to follow; OK cycles views; Back exits.
- The TV test is hands-on: Claude sets it up, the owner presses the remote and says when done, Claude reads the log and restores the TV.
- If the 2.5D view cannot hold 60 fps on the TV, the top-down view is the fallback and the finding is recorded.

## 6. Code structure

- `sim`: `world/` (surface map, chunks, fields, nest grid, tiles, active set, entrance), `ant/` (ant state, primitives, scripted decision rules), `scenario/` (seeded world setup, including the Gruter scenario). No libGDX.
- `core`: `render/` (nest renderer, surface top-down renderer, 2.5D renderer), `camera/` (nest camera, chase camera), `ui/` (debug readout), and the app that ties them together. The M0 stub stays available as a separate app for later TV checks.
- `desktop` and `android`: launchers only.

## 7. Testing

- Unit tests in `sim`: grid and tile storage (lazy creation, seeded regeneration), the active set, field decay and diffusion, chunk field allocation and release, the entrance handoff, the dig and pellet rules, path-integration error.
- The Gruter scenario test (section 4).
- Unit tests in `core` for camera math that does not need a GL context (chase camera lag and turn-in-place rule, nest zoom limit).
- Rendering is checked by running the desktop app and, for the 2.5D view, the TV test.

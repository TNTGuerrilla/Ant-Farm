# M1b-1: the 3D surface simulation

Status: approved in design discussion on 2026-09-30, awaiting spec review.
Parent design: `docs/design/poc-design.md` (sections 12 to 14). Builds on M1 (`docs/superpowers/specs/2026-09-29-m1-world-design.md`). Parameter values come from `docs/research/simulation-reference.md`; section numbers below refer to that file. Review follow-ups come from `docs/superpowers/notes/2026-09-30-m1-final-review.md`.

## 1. Goal

Replace M1's flat surface map with a 3D surface that ants walk on, over and under: ground with relief, rocks, aphid plant stems and prey. The world is described by a signed distance function (SDF). M1b is split in two:

- **M1b-1 (this spec): the simulation.** The SDF world, surface walking, 3D fields, the scripted behaviors on the new surface, validation, a plain 3D debug view on desktop, and the M1 review follow-ups assigned to M1b.
- **M1b-2 (a later spec): looks and TV.** Low-poly chunk meshes (marching cubes, flat shading), the 3D ant model with a walk cycle, rocks, grass and plants as models, the chase camera, and the TV test at 60 fps.

M1b-1 is done when:

1. Ants walk on every walkable surface without leaving it: over rocks and back down, around and under overhangs, up and down plant stems, onto prey.
2. The Gruter 2012 test passes again with M1's thresholds, on the new surface.
3. A forager can find an aphid plant, climb to the aphids, and a trail laid on the way down is followed up the stem.
4. A headless benchmark reports the cost of 1,000 surface ants at 20 ticks per second on desktop.
5. The desktop debug view shows the terrain, objects and ants in 3D, and the top-down view still works.

Out of scope: the low-poly art, ant models and animation, marching-cubes meshes, the TV test (all M1b-2); grass collision; debris such as leaf litter and twigs (a later milestone); brains (M2).

## 2. The SDF world (`sim`)

### 2.1 Coordinates

Surface space is 3D millimeters: x and y as in M1 (0..8000, the map), z up. The entrance is at the map center on the ground. The nest slice is unchanged.

### 2.2 Shapes

The world surface is the zero level of `sdf(p)`: negative inside solid matter, positive in the air. It is the smooth union (smooth minimum with a blend radius of about 1.5 mm) of:

| Shape | Form | Sizes | Source |
|---|---|---|---|
| Ground | height field z = h(x, y) | Rolling of about +/-15 mm over about 30 cm, small bumps of about 3 mm, plus the spoil mound | Seeded noise; spoil from digging |
| Pebbles | lumpy ellipsoids, partly buried | 5 to 40 mm radius (M1's stones) | Seeded per chunk |
| Rocks | lumpy ellipsoids, partly buried, may overhang | 5 to 20 cm, a few per map | Seeded per chunk |
| Aphid plant stems | capsule from the ground up | 2 to 3 mm radius, 0.5 to 1.2 m tall (ref. section 13) | M1's plant placement |
| Aphid cluster | a small blob on the stem near the top | about 10 mm | The food source |
| Prey | small lumps | 3 to 8 mm | M1's prey placement |

- The ground's SDF is `(z - h) / sqrt(1 + |grad h|^2)`, which is close to true distance for gentle slopes.
- **Spoil mound:** each dumped pellet raises the ground height where it lies. The existing spoil count field is converted to height (1 mm3 per pellet spread over its 1 cm2 cell).
- **Entrance clearance:** no pebble, rock, stem or prey is generated within 30 mm of the entrance.
- Grass is not part of the SDF. It is drawn only (M1b-2) and ants pass through it.

### 2.3 Spatial index and cost

Shapes are indexed by the existing 50 cm chunks, and each chunk's shape list includes shapes whose bounds reach into it. `sdf(p)` evaluates the ground plus the shapes of p's chunk only. Chunk content stays generated from the seed. `SurfaceMap.chunk` gains a bounds check (M1 review item).

### 2.4 Numeric stability

All seeded generation, including the M1 nest generator and plant and prey placement, uses `StrictMath` for transcendental functions, so it is identical on the TV and the desktop (M1 review). A behavior run is reproducible per platform; bit-identity across platforms is not promised.

### 2.5 Fields

- Trail pheromone and home scent move to a sparse 3D field with 10 mm voxels, stored in blocks of 16 x 16 x 16 voxels. A block is allocated on first write and released when it has faded.
- Decay rates are as in M1. Trail diffusion is small and only spreads within and between allocated blocks, so trails stay narrow (design doc section 14).
- **Lazy decay (M1 review):** each block records the tick at which it was last decayed. Reads and writes first bring the block up to date with one `exp(-k * elapsed)` factor. Fields without diffusion (home scent) therefore cost nothing per tick. The trail field steps its diffusion on allocated blocks only, and a per-block running maximum drives release without a second pass.

## 3. Moving on surfaces (`sim`)

### 3.1 Ant pose

A surface ant has a 3D position `p`, a unit normal `n` (its up), and a unit forward vector `f` perpendicular to `n`. Turning rotates `f` about `n`. M1's heading angle is replaced by `f` for surface ants; nest ants keep M1's 2D pose.

### 3.2 One step

1. `p += f * speed * DT`.
2. Project onto the surface twice: `p -= grad(p) * sdf(p)`, with the gradient from central differences at 0.25 mm, normalized.
3. `n = grad(p)`.
4. Re-project `f` into the new tangent plane and normalize (parallel transport). If `f` becomes nearly parallel to `n` (a sharp fold), rebuild it from the previous `f` crossed twice with `n`.

Ants never fall. A test asserts `|sdf(p)| < 0.5 mm` for every surface ant after every step.

### 3.3 Sensing

The antennae sample points 10 mm ahead at +/-30 degrees in the tangent plane, each projected onto the surface; the 3D trail field is read there. The steering rule is M1's (turn by (L - R) / (L + R)).

### 3.4 Path integration and homing

The home vector integrates the horizontal (x, y) part of each step with M1's noise model. Homing aims at the home direction projected into the tangent plane. The entrance and home-scent rules are as in M1, using 3D distance.

### 3.5 Finding food

- Each aphid plant gives off an odour within about 150 mm (horizontal) of its stem base. A searching forager inside that range steers toward the stem.
- An empty forager touching a stem (within 3 mm of the stem surface) turns to climb upward. At the aphid cluster it feeds exactly as in M1.
- A forager reaching a prey lump climbs onto it and loads as in M1.
- These cues are scripted stand-ins; M2's brains replace them.

### 3.6 Performance

A step costs a few dozen SDF evaluations. A benchmark test runs 1,000 surface ants for 60 simulated seconds and reports the time per tick; it asserts a generous ceiling on desktop (20 ms per tick) so regressions show, and the number is recorded for the M1b-2 TV budget.

## 4. Behaviors

Diggers' and foragers' surface states call the new motion code in place of M1's `SurfaceMotion`. Rules and `AntParams` values are unchanged. The nest side, the excavation plan and the entrance handoff are unchanged, except that surface positions are 3D.

## 5. Validation (`sim` tests)

- **Gruter 2012:** the M1 test with the same thresholds, on a scenario with gently rolling ground, no rocks between the nest and the two feeders, and the feeders as small food blobs sitting on the ground (no stems), found with M1's food-sense radius, so the geometry matches M1's test. If it fails, recalibrate by M1's rules (only the listed knobs, never loosen the test).
- **Staying on the surface:** random walkers on a world with rocks, stems and prey stay within 0.5 mm of the surface for 10 simulated minutes.
- **Over a rock:** an ant walking straight at a rock climbs it and comes back down to the ground on the far side.
- **Overhang:** an ant walking around an overhanging rock stays on its underside without falling.
- **Stem climbing:** a forager near a plant climbs its stem and feeds at the aphid cluster.
- **Trail up a stem:** a trail laid along a stem is followed up it by searching foragers.
- **Determinism:** two worlds from the same seed stay identical.
- **Benchmark:** section 3.6.
- Unit tests for the SDF shapes, smooth union, projection, the 3D field (lazy decay, release, diffusion) and the spoil mound.

## 6. Desktop views (`core`)

- **Debug 3D view** (replaces the 2.5D sprite view): the ground as a grid mesh rebuilt near the camera, rocks and prey as scaled spheres, stems as cylinders, ants as small boxes oriented by `f` and `n`. The chase camera keeps M1's angle (about 70 mm behind and 35 mm above), measured along the ant's own normal and forward, with the M1 turning rule.
- **Top-down view:** kept; trails and scent are drawn from the 3D field projected straight down.
- **Nest view:** unchanged.
- **Frame budget (M1 review):** `AntApp` replaces `MAX_STEPS` with a wall-clock budget of 6 ms of simulation stepping per frame; any backlog beyond it is dropped, so a heavy moment slows the simulation, not the frame rate.
- **HUD cost (M1 review):** the debug readout is recomputed a few times per second, not every frame.
- **Carried M1 review items:** the chase camera ignores teleports (an entrance handoff resets it), `placeAt` clamps zoom, and the pan sign convention is documented with tests. The 2.5D sprite renderer, its decals and the chunk ground textures are removed; the top-down view keeps its own texture path, and chunk-texture eviction runs from whichever view uses them.

## 7. Carried fixes from the M1 review

- Lazy field decay (section 2.5) and a per-block running maximum.
- The frame time budget (section 6).
- `StrictMath` in generation (section 2.4).
- `World.feedEvents` keeps only the last 30 simulated minutes.
- `SurfaceMap.chunk` bounds check and entrance clearance (sections 2.2 and 2.3).

## 8. Code structure

- `sim/world/sdf/`: shapes (`HeightField`, `Blob`, `Capsule`), smooth union, the chunked `SurfaceSdf` with projection and gradient.
- `sim/world/Field3.kt`: the sparse 3D field with lazy decay.
- `sim/ant/SurfaceWalk.kt`: pose, step, sensing, homing on surfaces (replaces `SurfaceMotion`).
- `sim/world/SurfaceMap.kt`: gains shape generation per chunk; plants and prey become shapes plus food sources.
- `core/render/DebugSurfaceRenderer.kt`: the debug 3D view. `Surface3dRenderer` is deleted.
- Existing files change only where they call the replaced pieces.

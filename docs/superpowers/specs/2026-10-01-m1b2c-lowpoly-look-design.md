# M1b-2c: the low-poly look and the final TV test

Status: approved in design discussion on 2026-10-01, awaiting spec review.
Parent design: `docs/design/poc-design.md` (sections 12 to 14). Follows M1b-2a (`docs/superpowers/specs/2026-09-30-m1b2a-engine-design.md`) and M1b-2b (`docs/superpowers/specs/2026-10-01-m1b2b-tv-cost-design.md`). The visual choices were made with mockups in the brainstorming companion (kept under `.superpowers/brainstorm/`, not in git).

## 1. Goal

The 3D surface view gets a RuneScape-style low-poly look, ants stop clipping into rocks, and the TV holds 60 fps. M1b-2c is the last part of M1b.

M1b-2c is done when:

1. **The world** is drawn from per-chunk meshes fitted to the simulation's surface: faceted ground with irregular faceted specks, smooth-shaded rocks with their real bumps, faceted aphid-plant stems, aphid clusters and prey, and patchy grass tufts that thin out on the spoil mound. A test checks that ground and rock mesh vertices lie within 0.2 mm of the simulation's surface.
2. **The ants** use the fused low-poly model, with a walk cycle driven by distance walked, visible carrying (a soil pellet or a piece of prey in the mandibles, and a gaster that swells as the crop fills), and a soft shadow.
3. **Lighting** comes from one sky state (sun direction and colour, ambient, sky colours, fog colour and distances), following a day cycle over the simulated day (600 real seconds), with a readable night.
4. **On the TV**, in the 3D view, the frame rate averages at least 58 fps with at most 1% of frames over 25 ms after the first 10 s. This holds both for the starter colony in the screensaver and for the 1,000-ant colony, launched in the preview activity over adb. It is measured in a hands-on session with the log streamed to a file.
5. **The nest and top-down views** work as before.

Out of scope: weather and the winter scene (M5 or later; the sky state is designed so weather can drive it); debris such as leaf litter and twigs; grass collision and climbing; the real calendar clock (M3; until then the time of day comes from simulation seconds); restyling the nest and top-down views; cast shadows from rocks and stems.

## 2. Look and lighting

### 2.1 Palette and shading

- A muted, earthy palette (the colours of the brainstorm sketches, not the more saturated variant): soil about RGB 139/98/62 with per-facet variation of plus or minus 10 to 15%; stone greys about 128/124/116; grass greens about 96/132/70; ants a dark brown-black about 70/40/26 before shading.
- **Flat-shaded** (each triangle one colour, per-face normals): ground, specks, ants, stems, aphids, prey and grass.
- **Smooth-shaded** (shared vertex normals): rocks and pebbles.
- Colour is per vertex. There are no textures; the specks are geometry.

### 2.2 The sky state

`SkyState` is a small data class: sun direction, sun colour, ambient colour, sky top colour, sky horizon colour, fog colour, fog start and fog end distance (mm). One shader family reads it: diffuse sun plus ambient, linear distance fog toward the fog colour, and a sky gradient drawn behind everything. A later weather system supplies its own `SkyState` (or modifies the day cycle's) without touching the renderers.

### 2.3 The day cycle

`DayCycle` maps the time of day to a `SkyState` by blending keyframes: dawn (low warm sun, pink-orange horizon), midday (high neutral sun, light haze), golden afternoon, dusk, and night. At night there is no sun; a dim blue directional "moon" light and a raised blue ambient keep shapes and ants readable, with overall brightness about 35 to 45% of midday.

One simulated day lasts 600 real seconds at 1x (the design's compressed calendar). Until the calendar clock arrives in M3, the time of day is `(simulation seconds + start offset) mod 600 / 600`, with the start offset putting the first frame at mid-morning.

### 2.4 Shadows

Each ant gets a soft dark disc under its body, offset away from the sun and faded at night. Nothing else casts shadows.

## 3. World meshes

### 3.1 Chunks and threading

- The world is built per 500 mm chunk (the simulation's chunks), in a 5 by 5 ring of chunks around the camera focus. Fog hides the edge at about 1.5 m.
- `ChunkMesher` turns one chunk into plain vertex arrays. It is pure Kotlin with no GL calls, so it is unit-tested, and it runs on one background thread.
- The render thread uploads finished arrays to GL meshes, at most 2 chunk uploads per frame, and evicts chunks that leave the ring.
- The mesher owns its own `HeightField` built from the seed, with spoil mirrored from the published spoil overlays. It reads rocks from the published rock store and foods from the latest snapshot. All of these are immutable published data, so the mesher thread never touches the live `World`, and the M1b-2a boundary holds.

### 3.2 Ground

- A triangle grid with about 16 mm facets. Vertices sit exactly on the ground height (base relief plus spoil); their x and y are jittered a little with a seeded hash, so the facets look irregular rather than like a grid.
- Between vertices, a flat facet can deviate from the true (bilinear) ground by up to about 1 mm. A test measures and reports the worst case, which must stay at or under 1 mm. 16 mm is the trade-off between the low-poly look and keeping ants' feet visibly on the ground.
- Specks are small flat triangles of 3 to 5 vertices, in mixed sizes, clustered in loose patches plus strays (the approved sketch), in a few soil shades and a greyish grit shade. They are baked into the chunk mesh 0.05 mm above the ground.
- When a chunk's published spoil changes, its ground is rebuilt in the background, at most once every 3 s per chunk.

### 3.3 Rocks and pebbles

- Each rock is a subdivided icosahedron: about 80 triangles for a pebble, up to about 320 for a large rock, chosen by size.
- Each vertex is projected onto that rock's own distance function (`Blob.distance`, bumps included) with Newton steps, and its normal is the normalized gradient there, which gives the smooth shading.
- Each rock gets a slight seeded colour variation.
- The smooth crease where a rock meets the ground in the SDF (about 1.5 mm) is not modelled; the meshes simply intersect.

### 3.4 Food

- Aphid plants: faceted six-sided stems from the stem base to the stem top, dark green; each aphid cluster is a handful of small faceted green and black aphid lumps around the cluster body.
- Prey: a faceted lump in a pale contrasting colour, at the food's body size.

### 3.5 Grass

- Tufts are placed per chunk from a seeded patchiness pattern, so grass grows in clumps with bare soil between (the "patchy" choice).
- Each tuft has 5 to 9 single-triangle blades, 30 to 80 mm tall, from 4 tuft variants. All tufts are drawn instanced in one draw call, with a gentle sway in the vertex shader.
- Tuft density falls to zero where the published spoil is deep, so the nest mound stays bare (Lasius niger buries vegetation under its spoil; it does not clear grass along trails).
- Grass is scenery only: ants do not collide with it, so an ant sometimes passes through a tuft.

## 4. Ants

### 4.1 The model

`AntMesh` builds the fused low-poly ant procedurally (no art files), about 200 triangles: the gaster, thorax and head as faceted lumps joined by thick joints, mandibles, elbowed antennae of thin prisms, and six legs of two segments each. A simpler version of about 60 triangles is used beyond about 0.5 m.

### 4.2 Instanced drawing and the gait

- All ants are drawn instanced, one draw call per level of detail. Each ant sends per frame: position, forward and normal (from the interpolated snapshot), walk phase, gaster fill, and what it carries.
- Leg vertices carry a leg index. The vertex shader swings the legs in a tripod gait (front-left, middle-right and back-left together, the other three opposite) and bobs the body slightly per step.
- The walk phase advances with distance walked, as for the 2D sprites (`AntAnimator`), so feet do not slide; an ant standing still stops mid-stride.

### 4.3 Carrying

- Honeydew: the gaster scales up to about 1.4 times as the crop fills (Lasius niger foragers visibly swell, showing pale bands between the plates).
- A soil pellet (diggers): a small faceted lump in the mandibles.
- Prey: a forager that collected from a prey item carries a small pale piece in its mandibles.
- The carried pieces are part of the instanced model, shown or hidden per ant.

### 4.4 Snapshot additions

Two per-ant values are added to `Snapshot` and blended by `AntStates`: `crop` (0 to 1, blended between ticks) and `carry` (0 none, 1 pellet, 2 prey; not blended). They are written by the simulation thread when it fills the snapshot and do not change the simulation.

## 5. Performance and the final TV test

### 5.1 Budget

The render thread has 16.6 ms per frame; the simulation runs on its own thread (about 2 ms per tick for the starter, 12 ms for the 1,000-ant colony on the TV). The scene budget for the 3D view:

| Item | Triangles | Draw calls |
|---|---|---|
| Ground and specks, 25 chunks | about 50,000 | 25 |
| Rocks and food | a few thousand | a few |
| Grass | about 15,000 | 1 |
| Ants (two levels of detail) | 60,000 to 200,000 | 2 |
| Shadows | discs | 1 |
| Sky | 2 | 1 |

That is under about 40 draw calls in total.

### 5.2 OpenGL ES 3

Instancing needs GLES 3; the Mali-G52 supports 3.2. Both the Android and desktop configurations enable GL 3 (`useGL30`). The shaders are short GLSL strings in Kotlin (no asset loading), written for GLSL ES 3.00 with a version line chosen per platform. If GL 3 is not available at start-up, the app logs an error and falls back to `DebugSurfaceRenderer` for the 3D view.

### 5.3 The final TV test

On the `profile` build, with the log streamed to a file on the PC for the whole session:

- the screensaver with the starter colony (the owner watches the 3D, nest and top-down views, about a minute each, then presses Back);
- `PreviewActivity` launched over adb with the 1,000-ant colony (`--es scenario colony1000`; the preview activity gains this extra), watched in the 3D view.

Recorded per view: fps, frame p50 and p99, frames over 25 ms, ticks per second and ms per tick.

## 6. Testing

- `ChunkMesher`: ground and rock vertices within 0.2 mm of the simulation's surface (ground: the height field with spoil; rocks: the blob's own distance); the ground's worst flat-facet deviation reported and at or under 1 mm; the same seed gives identical arrays; grass density falls to zero on deep spoil; specks lie above the ground.
- `AntMesh`: triangle counts for both levels of detail; every leg vertex tagged with a leg index from 0 to 5; the tripod groups.
- `DayCycle`: continuous blending across every keyframe (no jumps); night brightness at least 35% of midday; the start offset gives mid-morning at second 0.
- `Snapshot` and `AntStates`: `crop` and `carry` filled from the world and blended (crop) or taken from the current snapshot (carry).
- All existing determinism tests unchanged: the renderers still read only published data.
- Desktop: the owner checks the look on the desktop build before the TV test.

## 7. Code structure

- `core/render/sky/`: `SkyState`, `DayCycle`, `SkyRenderer` (the gradient).
- `core/render/world/`: `ChunkMesher` (ground, specks, rocks, food; pure Kotlin), `GrassField` (tuft placement and instancing), `ChunkCache` (the background thread, uploads and eviction).
- `core/render/ant/`: `AntMesh` (procedural model, pure Kotlin), `AntRenderer` (instancing and the gait shader, shadows).
- `core/render/Shaders.kt`: the GLSL sources and the shared lighting and fog code.
- `core/render/SurfaceRenderer3D.kt`: the new 3D view, replacing `DebugSurfaceRenderer`, which stays only as the GL 2 fallback.
- `core/engine/Snapshot.kt`, `AntStates.kt`: `crop` and `carry`.
- `android`: `useGL30` in the AndroidApplicationConfiguration, the `scenario` extra in `PreviewActivity`. `desktop`: GL 3 in the LWJGL3 configuration.

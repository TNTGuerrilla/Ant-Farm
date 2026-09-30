# M1b-1: The 3D Surface Simulation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace M1's flat surface with a signed-distance-function world (ground relief, pebbles, rocks with overhangs, aphid plant stems, prey) that ants walk on, over and under, with 3D pheromone fields, the Gruter test passing again, and a plain 3D debug view on desktop.

**Architecture:** `sim` gains an `sdf` package (shapes, a cached height field, and `SurfaceSdf` with distance, gradient and projection), a sparse 3D field (`Field3`), and `SurfaceWalk`, which replaces `SurfaceMotion`. A surface ant carries a position, a normal and a forward vector; each step moves forward, projects onto the surface, and carries the forward vector into the new tangent plane. Foragers and diggers keep M1's rules, expressed through `SurfaceWalk` turns and facings. `core` gets a 3D chase camera and a debug renderer built from libGDX primitives.

**Tech Stack:** Kotlin 2.2.0, libGDX 1.14.2 (ModelBatch, ModelBuilder, PerspectiveCamera), Gradle 8.14.3, JUnit 5 via `kotlin("test")`, JDK 17.

**Spec:** `docs/superpowers/specs/2026-09-30-m1b1-surface-sim-design.md`

## Global Constraints

- Never use em-dashes or emojis anywhere: code, comments, docs, commit messages.
- Commit messages carry no Claude attribution (no `Co-Authored-By`, no "Generated with" line).
- Gradle needs JDK 17: prefix every Gradle command with `JAVA_HOME="C:/Program Files/Eclipse Adoptium/jdk-17.0.10.7-hotspot"`. Run Gradle as background tasks.
- `sim` must not depend on libGDX or Android.
- Surface space is 3D millimeters: x and y as in M1 (0..8000), z up. The nest slice is unchanged (x across, y = depth).
- Fixed tick `DT = 0.05f`. All randomness comes from seeded sources.
- Seeded generation uses `StrictMath` for transcendental functions (TV and desktop identical). A behavior run is reproducible per platform.
- A surface ant stays within 0.5 mm of the surface after every step.
- The Gruter test keeps M1's thresholds; recalibrate only by M1's rules (listed knobs, one at a time, never loosen the test).
- Chase camera angle as in M1: about 70 mm behind and 35 mm above the ant, measured along the ant's own forward and normal.
- Reference values come from `docs/research/simulation-reference.md`; cite the section where a constant comes from it. Package root `com.bydesigninteractive.ant`. KDoc on public classes and non-obvious functions.

## File Structure

`sim/src/main/kotlin/com/bydesigninteractive/ant/sim/`
- `world/sdf/Shapes.kt` (new) - `smin`, `Blob` (lumpy ellipsoid), `Stem` (vertical capsule).
- `world/sdf/HeightField.kt` (new) - ground height: value noise cached per chunk on a 2 mm grid, plus the spoil mound; ground distance.
- `world/sdf/SurfaceSdf.kt` (new) - the world SDF: ground, generated rocks, placed objects, food shapes; gradient, projection, stem touch.
- `world/Field3.kt` (new) - sparse 3D field, trilinear, lazy decay or eager diffusion.
- `world/Food.kt` (modify) - food gains z, a body blob and a stem.
- `world/SurfaceGenerator.kt` (modify) - chunks gain rare large rocks.
- `world/SurfaceMap.kt` (modify) - ground, SDF, 3D fields, plants and prey in 3D, `nearestPlant`.
- `world/NestGenerator.kt` (modify) - `StrictMath`.
- `ant/Ant.kt` (modify) - surface pose fields.
- `ant/SurfaceWalk.kt` (new, replaces `ant/SurfaceMotion.kt`) - placing, stepping, turning, facing, sensing on surfaces.
- `ant/Forager.kt`, `ant/Digger.kt` (modify) - use `SurfaceWalk`; foragers climb stems.
- `ant/AntParams.kt` (modify) - plant odour and stem constants.
- `World.kt` (modify) - scratch arrays, `rocks` flag, feed-event window, surface placement on exit.
- `scenario/Scenarios.kt` (modify) - Gruter feeders as ground blobs on a rock-free world.

`core/src/main/kotlin/com/bydesigninteractive/ant/core/`
- `camera/ChaseCamera3.kt` (new, replaces `camera/ChaseCamera.kt`) - 3D chase camera math.
- `camera/ViewCamera.kt` (modify) - `placeAt` clamps; pan convention documented.
- `render/DebugSurfaceRenderer.kt` (new, replaces `render/Surface3dRenderer.kt`) - debug 3D view.
- `render/SurfaceTopRenderer.kt`, `render/ChunkTextures.kt`, `render/Sprites.kt`, `ui/DebugReadout.kt`, `AntApp.kt` (modify).

---

### Task 1: M1 review follow-ups in `sim`

**Files:**
- Modify: `sim/src/main/kotlin/com/bydesigninteractive/ant/sim/world/NestGenerator.kt`
- Modify: `sim/src/main/kotlin/com/bydesigninteractive/ant/sim/world/SurfaceMap.kt`
- Modify: `sim/src/main/kotlin/com/bydesigninteractive/ant/sim/World.kt`
- Test: `sim/src/test/kotlin/com/bydesigninteractive/ant/sim/WorldTest.kt`, `sim/src/test/kotlin/com/bydesigninteractive/ant/sim/world/SurfaceMapTest.kt`

**Interfaces:**
- Produces: `const val FEED_WINDOW_TICKS = 36_000L` (in `World.kt`); `World.feedEvents` becomes `kotlin.collections.ArrayDeque<FeedEvent>` and keeps only events from the last `FEED_WINDOW_TICKS`. `SurfaceMap.chunk(cx, cy)` requires `cx, cy in 0 until CHUNKS`.

- [ ] **Step 1: Write the failing tests**

Add to `WorldTest.kt` (add `import kotlin.test.assertFailsWith` is not needed here):
```kotlin
    @Test
    fun feedEventsKeepOnlyTheLastHalfHour() {
        val w = World(1)
        w.predig(1)
        w.feedEvents += FeedEvent(0, 0)
        repeat(FEED_WINDOW_TICKS.toInt()) { w.step() }
        assertEquals(1, w.feedEvents.size)
        w.step()
        assertEquals(0, w.feedEvents.size)
    }
```
Add to `SurfaceMapTest.kt` (with `import kotlin.test.assertFailsWith`):
```kotlin
    @Test
    fun chunksOutsideTheMapAreRejected() {
        val m = SurfaceMap(5)
        assertFailsWith<IllegalArgumentException> { m.chunk(16, 0) }
        assertFailsWith<IllegalArgumentException> { m.chunk(0, -1) }
    }
```

- [ ] **Step 2: Run the tests to verify they fail**

Run: `JAVA_HOME="C:/Program Files/Eclipse Adoptium/jdk-17.0.10.7-hotspot" ./gradlew :sim:test --tests "*WorldTest" --tests "*SurfaceMapTest"`
Expected: compilation fails (`FEED_WINDOW_TICKS` unresolved).

- [ ] **Step 3: Implement**

`NestGenerator.kt`: remove `import kotlin.math.sin` and change the clay line to
```kotlin
        val top = clayTop + (StrictMath.sin(x / 90.0 + clayPhase) * 15).toInt()
```

`SurfaceMap.kt`:
- In `chunk`, add a bounds check:
```kotlin
    fun chunk(cx: Int, cy: Int): ChunkContent {
        require(cx in 0 until CHUNKS && cy in 0 until CHUNKS) { "chunk ($cx, $cy) is outside the map" }
        return chunks.getOrPut(cx + cy * CHUNKS) { generator.chunk(cx, cy) }
    }
```
- In `placePlants` and `placePrey`, replace `cos(a)` and `sin(a)` with `StrictMath.cos(a.toDouble()).toFloat()` and `StrictMath.sin(a.toDouble()).toFloat()`, and remove the now unused `kotlin.math.cos`/`sin` imports.

`World.kt`:
- Below `const val DT`, add:
```kotlin
/** Feed events older than this many ticks (30 simulated minutes) are dropped. */
const val FEED_WINDOW_TICKS = 36_000L
```
- Change the field to `val feedEvents = ArrayDeque<FeedEvent>()` with the KDoc `/** Recent feeding events, oldest first, for the last [FEED_WINDOW_TICKS] ticks. */`.
- At the end of `step()`, after `tick++`, add:
```kotlin
        while (feedEvents.isNotEmpty() && feedEvents.first().tick < tick - FEED_WINDOW_TICKS) feedEvents.removeFirst()
```

- [ ] **Step 4: Run all sim tests**

Run: `JAVA_HOME="C:/Program Files/Eclipse Adoptium/jdk-17.0.10.7-hotspot" ./gradlew :sim:test`
Expected: BUILD SUCCESSFUL (the Gruter test uses the last 10 minutes of feed events, well inside the window).

- [ ] **Step 5: Commit**

```bash
git add sim/src
git commit -m "M1b-1: StrictMath in generation, a feed-event window and chunk bounds"
```

---

### Task 2: SDF shapes

**Files:**
- Create: `sim/src/main/kotlin/com/bydesigninteractive/ant/sim/world/sdf/Shapes.kt`
- Test: `sim/src/test/kotlin/com/bydesigninteractive/ant/sim/world/sdf/ShapesTest.kt`

**Interfaces:**
- Produces: `fun smin(a: Float, b: Float, k: Float): Float`; `class Blob(cx, cy, cz, rx, ry, rz, lump = 0f, phase = 0f)` with `reach`, `near(x, y, z, margin): Boolean`, `distance(x, y, z): Float`; `class Stem(x, y, z0, z1, radius)` with `near(...)`, `distance(...)`.

- [ ] **Step 1: Write the failing test**

```kotlin
package com.bydesigninteractive.ant.sim.world.sdf

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ShapesTest {
    @Test
    fun smoothMinIsTheMinimumAwayFromTheBlend() {
        assertEquals(2f, smin(2f, 10f, 1.5f))
        assertEquals(smin(3f, 3.5f, 1.5f), smin(3.5f, 3f, 1.5f))
        assertTrue(smin(3f, 3f, 1.5f) < 3f)
    }

    @Test
    fun aRoundBlobIsAnExactSphere() {
        val b = Blob(10f, 20f, 30f, 5f, 5f, 5f)
        assertEquals(5f, b.distance(20f, 20f, 30f), 1e-4f)
        assertEquals(-5f, b.distance(10f, 20f, 30f), 1e-4f)
        assertEquals(0f, b.distance(10f, 20f, 35f), 1e-4f)
    }

    @Test
    fun aFlatBlobIsNearerAlongItsShortAxis() {
        val b = Blob(0f, 0f, 0f, 10f, 10f, 4f)
        assertTrue(b.distance(0f, 0f, 6f) < b.distance(12f, 0f, 0f) + 0.5f)
        assertEquals(0f, b.distance(0f, 0f, 4f), 0.05f)
    }

    @Test
    fun lumpsStayWithinTheirAmplitude() {
        val plain = Blob(0f, 0f, 0f, 20f, 20f, 20f)
        val lumpy = Blob(0f, 0f, 0f, 20f, 20f, 20f, lump = 0.1f, phase = 1f)
        for (i in 0 until 50) {
            val x = 25f * kotlin.math.cos(i * 0.7f)
            val z = 25f * kotlin.math.sin(i * 0.7f)
            assertTrue(kotlin.math.abs(lumpy.distance(x, 3f, z) - plain.distance(x, 3f, z)) <= 2.0001f)
        }
    }

    @Test
    fun nearUsesTheBoundsPlusMargin() {
        val b = Blob(0f, 0f, 0f, 10f, 10f, 10f)
        assertTrue(b.near(14f, 0f, 0f, 5f))
        assertFalse(b.near(16f, 0f, 0f, 5f))
    }

    @Test
    fun aStemIsAVerticalCapsule() {
        val s = Stem(0f, 0f, 0f, 100f, 2f)
        assertEquals(3f, s.distance(5f, 0f, 50f), 1e-4f)
        assertEquals(8f, s.distance(0f, 0f, 110f), 1e-4f)
        assertEquals(-2f, s.distance(0f, 0f, 50f), 1e-4f)
        assertTrue(s.near(4f, 0f, 50f, 3f))
        assertFalse(s.near(0f, 0f, 120f, 3f))
    }
}
```

- [ ] **Step 2: Run to verify it fails**

Run: `JAVA_HOME="C:/Program Files/Eclipse Adoptium/jdk-17.0.10.7-hotspot" ./gradlew :sim:test --tests "*ShapesTest"`
Expected: compilation fails (unresolved `smin`, `Blob`, `Stem`).

- [ ] **Step 3: Implement**

```kotlin
package com.bydesigninteractive.ant.sim.world.sdf

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

/** Polynomial smooth minimum: min(a, b), rounded over a blend width [k] (Inigo Quilez). */
fun smin(a: Float, b: Float, k: Float): Float {
    val h = max(k - abs(a - b), 0f) / k
    return min(a, b) - h * h * k * 0.25f
}

/**
 * A lumpy, axis-aligned ellipsoid: a pebble, rock or food lump. [lump] is the bump amplitude as a
 * fraction of the smallest radius, so rocks do not look machined; [phase] varies the pattern.
 * The distance is Quilez's ellipsoid bound, exact for a sphere.
 */
class Blob(
    val cx: Float,
    val cy: Float,
    val cz: Float,
    val rx: Float,
    val ry: Float,
    val rz: Float,
    private val lump: Float = 0f,
    private val phase: Float = 0f,
) {
    private val rMin = min(rx, min(ry, rz))

    /** Half the size of the blob's bounding box, bumps included. */
    val reach = max(rx, max(ry, rz)) + lump * rMin
    private val frequency = LUMP_FREQUENCY / reach

    fun near(x: Float, y: Float, z: Float, margin: Float): Boolean {
        val r = reach + margin
        return abs(x - cx) <= r && abs(y - cy) <= r && abs(z - cz) <= r
    }

    fun distance(x: Float, y: Float, z: Float): Float {
        val px = x - cx
        val py = y - cy
        val pz = z - cz
        val ax = px / rx
        val ay = py / ry
        val az = pz / rz
        val k0 = sqrt(ax * ax + ay * ay + az * az)
        val bx = px / (rx * rx)
        val by = py / (ry * ry)
        val bz = pz / (rz * rz)
        val k1 = sqrt(bx * bx + by * by + bz * bz)
        val d = if (k1 < 1e-6f) -rMin else k0 * (k0 - 1f) / k1
        if (lump == 0f) return d
        val bumps = sin(px * frequency + phase) * sin(py * frequency + phase * 1.7f) * sin(pz * frequency + phase * 2.3f)
        return d - lump * rMin * bumps
    }

    private companion object {
        const val LUMP_FREQUENCY = 5f
    }
}

/** A vertical capsule around (x, y) from [z0] to [z1]: a plant stem. */
class Stem(val x: Float, val y: Float, val z0: Float, val z1: Float, val radius: Float) {
    fun near(px: Float, py: Float, pz: Float, margin: Float): Boolean {
        val r = radius + margin
        return abs(px - x) <= r && abs(py - y) <= r && pz >= z0 - r && pz <= z1 + r
    }

    fun distance(px: Float, py: Float, pz: Float): Float {
        val dx = px - x
        val dy = py - y
        val dz = pz - pz.coerceIn(z0, z1)
        return sqrt(dx * dx + dy * dy + dz * dz) - radius
    }
}
```

- [ ] **Step 4: Run to verify it passes**

Run: `JAVA_HOME="C:/Program Files/Eclipse Adoptium/jdk-17.0.10.7-hotspot" ./gradlew :sim:test --tests "*ShapesTest"`
Expected: BUILD SUCCESSFUL, 6 tests passed.

- [ ] **Step 5: Commit**

```bash
git add sim/src
git commit -m "M1b-1: SDF shapes (smooth minimum, lumpy blobs and stems)"
```

---

### Task 3: The ground height field

**Files:**
- Create: `sim/src/main/kotlin/com/bydesigninteractive/ant/sim/world/sdf/HeightField.kt`
- Test: `sim/src/test/kotlin/com/bydesigninteractive/ant/sim/world/sdf/HeightFieldTest.kt`

**Interfaces:**
- Consumes: `unit` (sim.util), `ChunkedField`, `SURFACE_MM`, `CHUNK_MM`, `CHUNKS` (sim.world).
- Produces: `class HeightField(seed: Long, spoil: ChunkedField)` with `base(x, y): Float`, `height(x, y): Float`, `slope(x, y, out: FloatArray)` (dh/dx, dh/dy into out[0], out[1]), `distance(x, y, z): Float`; `const val PELLET_HEIGHT = 0.01f`.

- [ ] **Step 1: Write the failing test**

```kotlin
package com.bydesigninteractive.ant.sim.world.sdf

import com.bydesigninteractive.ant.sim.world.ChunkedField
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class HeightFieldTest {
    private val spoil = ChunkedField()
    private val ground = HeightField(4, spoil)

    @Test
    fun theSameSeedGivesTheSameGround() {
        val other = HeightField(4, ChunkedField())
        for (i in 0 until 200) {
            val x = 3000f + i * 7.3f
            val y = 4100f - i * 3.1f
            assertEquals(ground.base(x, y), other.base(x, y))
        }
    }

    @Test
    fun reliefStaysWithinItsAmplitude() {
        for (i in 0 until 2000) {
            val h = ground.base(1000f + i * 3.7f, 2000f + i * 2.9f)
            assertTrue(abs(h) <= 16.5f, "height $h")
        }
    }

    @Test
    fun theGroundIsContinuousAcrossChunkEdges() {
        for (y in listOf(1234f, 4000f, 6789f)) {
            assertEquals(ground.base(499.9f, y), ground.base(500.1f, y), 0.2f)
            assertEquals(ground.base(y, 3999.9f), ground.base(y, 4000.1f), 0.2f)
        }
    }

    @Test
    fun spoilRaisesTheGround() {
        val before = ground.height(4005f, 4005f)
        spoil.add(4005f, 4005f, 100f)
        assertEquals(before + 100 * PELLET_HEIGHT, ground.height(4005f, 4005f), 1e-4f)
    }

    @Test
    fun slopeMatchesTheHeightDifferences() {
        val out = FloatArray(2)
        ground.slope(3210f, 4321f, out)
        val dx = (ground.height(3211f, 4321f) - ground.height(3209f, 4321f)) / 2f
        assertEquals(dx, out[0], 1e-3f)
    }

    @Test
    fun distanceIsZeroOnTheGroundAndPositiveAbove() {
        val h = ground.height(2500f, 2500f)
        assertEquals(0f, ground.distance(2500f, 2500f, h), 1e-4f)
        assertTrue(ground.distance(2500f, 2500f, h + 5f) in 4f..5.0001f)
        assertTrue(ground.distance(2500f, 2500f, h - 5f) < 0f)
    }
}
```

- [ ] **Step 2: Run to verify it fails**

Run: `JAVA_HOME="C:/Program Files/Eclipse Adoptium/jdk-17.0.10.7-hotspot" ./gradlew :sim:test --tests "*HeightFieldTest"`
Expected: compilation fails (unresolved `HeightField`).

- [ ] **Step 3: Implement**

```kotlin
package com.bydesigninteractive.ant.sim.world.sdf

import com.bydesigninteractive.ant.sim.util.unit
import com.bydesigninteractive.ant.sim.world.CHUNKS
import com.bydesigninteractive.ant.sim.world.CHUNK_MM
import com.bydesigninteractive.ant.sim.world.ChunkedField
import com.bydesigninteractive.ant.sim.world.SURFACE_MM
import kotlin.math.floor
import kotlin.math.sqrt

/** One dumped pellet (1 mm3) spread over its 1 cm2 field cell raises the ground 0.01 mm. */
const val PELLET_HEIGHT = 0.01f

/**
 * The ground: z = h(x, y) in surface millimeters. The base relief is seeded value noise (rolling
 * of about +/-15 mm over about 30 cm, bumps of about 1.5 mm), cached per 50 cm chunk on a 2 mm
 * grid the first time the chunk is used. Dumped spoil raises it. Coordinates outside the map are
 * clamped to its edge.
 */
class HeightField(private val seed: Long, private val spoil: ChunkedField) {
    private val grids = HashMap<Int, FloatArray>()
    private val slopeScratch = FloatArray(2)

    /** The seeded relief without spoil. */
    fun base(x: Float, y: Float): Float {
        val cx = x.coerceIn(0f, MAX)
        val cy = y.coerceIn(0f, MAX)
        val kx = (cx / CHUNK_MM).toInt().coerceAtMost(CHUNKS - 1)
        val ky = (cy / CHUNK_MM).toInt().coerceAtMost(CHUNKS - 1)
        val grid = grids.getOrPut(kx + ky * CHUNKS) { build(kx, ky) }
        val u = (cx - kx * CHUNK_MM) / GRID_MM
        val v = (cy - ky * CHUNK_MM) / GRID_MM
        val i = floor(u).toInt().coerceAtMost(CELLS - 1)
        val j = floor(v).toInt().coerceAtMost(CELLS - 1)
        val fu = u - i
        val fv = v - j
        val row = CELLS + 1
        val h00 = grid[j * row + i]
        val h10 = grid[j * row + i + 1]
        val h01 = grid[(j + 1) * row + i]
        val h11 = grid[(j + 1) * row + i + 1]
        return (h00 + (h10 - h00) * fu) + ((h01 + (h11 - h01) * fu) - (h00 + (h10 - h00) * fu)) * fv
    }

    /** The ground height, spoil mound included. */
    fun height(x: Float, y: Float): Float = base(x, y) + spoilHeight(x, y)

    /** dh/dx and dh/dy by central differences 1 mm apart, into out[0] and out[1]. */
    fun slope(x: Float, y: Float, out: FloatArray) {
        out[0] = (height(x + 1f, y) - height(x - 1f, y)) / 2f
        out[1] = (height(x, y + 1f) - height(x, y - 1f)) / 2f
    }

    /** Signed distance to the ground, close to exact for gentle slopes. */
    fun distance(x: Float, y: Float, z: Float): Float {
        slope(x, y, slopeScratch)
        val hx = slopeScratch[0]
        val hy = slopeScratch[1]
        return (z - height(x, y)) / sqrt(1f + hx * hx + hy * hy)
    }

    private fun spoilHeight(x: Float, y: Float): Float {
        val cell = spoil.cellMm.toFloat()
        val u = x / cell - 0.5f
        val v = y / cell - 0.5f
        val i = floor(u).toInt()
        val j = floor(v).toInt()
        val fu = u - i
        val fv = v - j
        val a = spoil.cell(i, j) + (spoil.cell(i + 1, j) - spoil.cell(i, j)) * fu
        val b = spoil.cell(i, j + 1) + (spoil.cell(i + 1, j + 1) - spoil.cell(i, j + 1)) * fu
        return (a + (b - a) * fv) * PELLET_HEIGHT
    }

    private fun build(kx: Int, ky: Int): FloatArray {
        val row = CELLS + 1
        val grid = FloatArray(row * row)
        for (j in 0..CELLS) for (i in 0..CELLS) {
            val x = kx * CHUNK_MM + i * GRID_MM
            val y = ky * CHUNK_MM + j * GRID_MM
            grid[j * row + i] = RELIEF * noise(seed xor RELIEF_SALT, x / RELIEF_SCALE, y / RELIEF_SCALE) +
                BUMPS * noise(seed xor BUMP_SALT, x / BUMP_SCALE, y / BUMP_SCALE)
        }
        return grid
    }

    /** Value noise in [-1, 1] with smoothstep interpolation; plain arithmetic, so platform-exact. */
    private fun noise(s: Long, x: Float, y: Float): Float {
        val ix = floor(x).toInt()
        val iy = floor(y).toInt()
        val fx = x - ix
        val fy = y - iy
        val sx = fx * fx * (3f - 2f * fx)
        val sy = fy * fy * (3f - 2f * fy)
        val v00 = (unit(s, ix, iy) * 2 - 1).toFloat()
        val v10 = (unit(s, ix + 1, iy) * 2 - 1).toFloat()
        val v01 = (unit(s, ix, iy + 1) * 2 - 1).toFloat()
        val v11 = (unit(s, ix + 1, iy + 1) * 2 - 1).toFloat()
        val a = v00 + (v10 - v00) * sx
        val b = v01 + (v11 - v01) * sx
        return a + (b - a) * sy
    }

    private companion object {
        const val MAX = SURFACE_MM.toFloat()
        const val GRID_MM = 2f
        const val CELLS = (CHUNK_MM / 2)
        const val RELIEF = 15f
        const val RELIEF_SCALE = 300f
        const val BUMPS = 1.5f
        const val BUMP_SCALE = 20f
        const val RELIEF_SALT = 0x6E11L
        const val BUMP_SALT = 0x6E12L
    }
}
```

- [ ] **Step 4: Run to verify it passes**

Run: `JAVA_HOME="C:/Program Files/Eclipse Adoptium/jdk-17.0.10.7-hotspot" ./gradlew :sim:test --tests "*HeightFieldTest"`
Expected: BUILD SUCCESSFUL, 6 tests passed.

- [ ] **Step 5: Commit**

```bash
git add sim/src
git commit -m "M1b-1: the ground height field with the spoil mound"
```

---

### Task 4: Rocks, food shapes and the surface SDF

**Files:**
- Modify: `sim/src/main/kotlin/com/bydesigninteractive/ant/sim/world/SurfaceGenerator.kt`
- Modify: `sim/src/main/kotlin/com/bydesigninteractive/ant/sim/world/Food.kt`
- Modify: `sim/src/main/kotlin/com/bydesigninteractive/ant/sim/world/SurfaceMap.kt`
- Create: `sim/src/main/kotlin/com/bydesigninteractive/ant/sim/world/sdf/SurfaceSdf.kt`
- Test: `sim/src/test/kotlin/com/bydesigninteractive/ant/sim/world/sdf/SurfaceSdfTest.kt`

**Interfaces:**
- Consumes: `Blob`, `Stem`, `smin` (Task 2), `HeightField` (Task 3), `hash` (sim.util).
- Produces: `ChunkContent.rocks: List<Stone>`; `FoodSource(..., loads, z = 0f, bodyRadius = 0f, stemRadius = 0f, stemBase = 0f)` with `body: Blob?`, `stem: Stem?`, `hasStem`; `SurfaceMap(seed, rocks = true)` with `ground: HeightField`, `sdf: SurfaceSdf`; `class SurfaceSdf(map, rocks)` with `placed: ArrayList<Blob>`, `distance(x, y, z)`, `gradient(x, y, z, out)`, `project(p: FloatArray)`, `ownBlobs(cx, cy): List<Blob>`, `stemAt(x, y, z, within): FoodSource?`.

- [ ] **Step 1: Write the failing test**

```kotlin
package com.bydesigninteractive.ant.sim.world.sdf

import com.bydesigninteractive.ant.sim.world.CHUNKS
import com.bydesigninteractive.ant.sim.world.FoodKind
import com.bydesigninteractive.ant.sim.world.FoodSource
import com.bydesigninteractive.ant.sim.world.SurfaceMap
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.max
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SurfaceSdfTest {
    @Test
    fun withoutRocksTheSurfaceIsTheGround() {
        val m = SurfaceMap(3, rocks = false)
        val h = m.ground.height(4200f, 4100f)
        assertEquals(0f, m.sdf.distance(4200f, 4100f, h), 1e-4f)
    }

    @Test
    fun projectionLandsOnTheSurface() {
        val m = SurfaceMap(3)
        val p = floatArrayOf(4321f, 3987f, m.ground.height(4321f, 3987f) + 3f)
        m.sdf.project(p)
        assertTrue(abs(m.sdf.distance(p[0], p[1], p[2])) < 0.1f)
    }

    @Test
    fun theGradientPointsOutOfAPlacedRock() {
        val m = SurfaceMap(3, rocks = false)
        val g = m.ground.height(4300f, 4000f)
        m.sdf.placed += Blob(4300f, 4000f, g + 20f, 20f, 20f, 20f)
        val out = FloatArray(3)
        m.sdf.gradient(4300f, 4000f, g + 45f, out)
        assertTrue(out[2] > 0.99f)
        m.sdf.gradient(4325f, 4000f, g + 20f, out)
        assertTrue(out[0] > 0.99f)
    }

    @Test
    fun noRockIsGeneratedNearTheEntrance() {
        val m = SurfaceMap(3)
        val cx = (m.entranceX / 500f).toInt()
        val cy = (m.entranceY / 500f).toInt()
        for (y in cy - 1..cy + 1) for (x in cx - 1..cx + 1) {
            for (b in m.sdf.ownBlobs(x, y)) {
                assertTrue(hypot(b.cx - m.entranceX, b.cy - m.entranceY) - max(b.rx, b.ry) >= 30f)
            }
        }
    }

    @Test
    fun theMapHasPebblesAndSomeLargeRocks() {
        val m = SurfaceMap(3)
        var blobs = 0
        var rockStones = 0
        var biggest = 0f
        for (cy in 0 until CHUNKS) for (cx in 0 until CHUNKS) {
            rockStones += m.chunk(cx, cy).rocks.size
            for (b in m.sdf.ownBlobs(cx, cy)) {
                blobs++
                biggest = max(biggest, max(b.rx, b.ry))
            }
        }
        assertTrue(blobs > 100, "blobs $blobs")
        assertTrue(rockStones in 10..80, "large rocks $rockStones")
        assertTrue(biggest >= 60f, "largest blob $biggest")
    }

    @Test
    fun stemsAreWalkableAndDetected() {
        val m = SurfaceMap(3, rocks = false)
        val g = m.ground.height(4500f, 4000f)
        m.foods += FoodSource(0, FoodKind.HONEYDEW, 4500f, 4000f, 12f, 1f, z = g + 470f, bodyRadius = 8f, stemRadius = 2.5f, stemBase = g - 5f)
        assertEquals(0f, m.sdf.distance(4502.5f, 4000f, g + 200f), 0.01f)
        assertNotNull(m.sdf.stemAt(4504f, 4000f, g + 200f, 3f))
        assertNull(m.sdf.stemAt(4520f, 4000f, g + 200f, 3f))
    }
}
```

- [ ] **Step 2: Run to verify it fails**

Run: `JAVA_HOME="C:/Program Files/Eclipse Adoptium/jdk-17.0.10.7-hotspot" ./gradlew :sim:test --tests "*SurfaceSdfTest"`
Expected: compilation fails (unresolved `sdf`, `ground`, `rocks`, new `FoodSource` parameters).

- [ ] **Step 3: Implement**

`SurfaceGenerator.kt`: add `rocks` to `ChunkContent` and generate them after the stones, so existing tufts and stones are unchanged:
```kotlin
/**
 * What lies on one 50 x 50 cm chunk of ground. [litter] is how much of it is leaf litter, 0 to 1.
 * [stones] become pebbles and [rocks] (rare, 5 to 20 cm) become large rocks on the 3D surface.
 */
class ChunkContent(
    val cx: Int,
    val cy: Int,
    val litter: Float,
    val tufts: List<Tuft>,
    val stones: List<Stone>,
    val rocks: List<Stone>,
)
```
and in `chunk`, after the `stones` list:
```kotlin
        val rocks = if (r.nextFloat() < ROCK_CHANCE) {
            listOf(Stone(x0 + r.nextFloat() * CHUNK_MM, y0 + r.nextFloat() * CHUNK_MM, 50f + r.nextFloat() * 150f))
        } else {
            emptyList()
        }
        return ChunkContent(cx, cy, litter, tufts, stones, rocks)
```
with `private companion object { const val ROCK_CHANCE = 0.15f }` in `SurfaceGenerator`.

`Food.kt`:
```kotlin
package com.bydesigninteractive.ant.sim.world

import com.bydesigninteractive.ant.sim.world.sdf.Blob
import com.bydesigninteractive.ant.sim.world.sdf.Stem

enum class FoodKind { HONEYDEW, PREY }

/**
 * Something ants collect. Honeydew plants never run out; a prey item holds [loads] trips' worth.
 * [quality] is how full a forager can fill its crop there, from 0 to 1.
 *
 * On the 3D surface, food at height [z] can have a solid [body] (an aphid cluster, prey lump or
 * feeder) and an aphid plant also has a [stem] from [stemBase] to a little above the cluster.
 * [radius] is the feeding reach: horizontal for food on the ground, 3D from the cluster for a plant.
 */
class FoodSource(
    val id: Int,
    val kind: FoodKind,
    val x: Float,
    val y: Float,
    val radius: Float,
    var quality: Float,
    var loads: Int = Int.MAX_VALUE,
    val z: Float = 0f,
    val bodyRadius: Float = 0f,
    val stemRadius: Float = 0f,
    val stemBase: Float = 0f,
) {
    val body: Blob? = if (bodyRadius > 0f) Blob(x, y, z, bodyRadius, bodyRadius, bodyRadius) else null
    val stem: Stem? = if (stemRadius > 0f) Stem(x, y, stemBase, z + STEM_ABOVE_CLUSTER, stemRadius) else null
    val hasStem: Boolean get() = stem != null

    private companion object {
        const val STEM_ABOVE_CLUSTER = 30f
    }
}
```

`SurfaceMap.kt`:
- Change the constructor to `class SurfaceMap(val seed: Long, rocks: Boolean = true)`.
- Declare, in this order after `spoil`: `val ground = HeightField(seed, spoil)`; and after `chunks`: `val sdf = SurfaceSdf(this, rocks)`. Import `com.bydesigninteractive.ant.sim.world.sdf.HeightField` and `...sdf.SurfaceSdf`.
- `placePlants`: after the quality draw, draw the plant height and build a stemmed plant (reference section 13: aphids 0.5 to 1.2 m up):
```kotlin
            val quality = 0.5f + r.nextFloat() * 0.5f
            val tall = 500f + r.nextFloat() * 700f
            val px = entranceX + d * StrictMath.cos(a.toDouble()).toFloat()
            val py = entranceY + d * StrictMath.sin(a.toDouble()).toFloat()
            val base = ground.base(px, py)
            foods += FoodSource(
                foods.size, FoodKind.HONEYDEW, px, py,
                radius = 12f, quality = quality,
                z = base + tall - 30f, bodyRadius = 8f, stemRadius = 2.5f, stemBase = base - 5f,
            )
```
- `placePrey`: draw the body size after the angle and build a lump:
```kotlin
            val body = 3f + r.nextFloat() * 5f
            val px = entranceX + d * StrictMath.cos(a.toDouble()).toFloat()
            val py = entranceY + d * StrictMath.sin(a.toDouble()).toFloat()
            foods += FoodSource(
                foods.size, FoodKind.PREY, px, py,
                radius = body + 4f, quality = 0.8f, loads = 20,
                z = ground.base(px, py) + body * 0.4f, bodyRadius = body,
            )
```

`SurfaceSdf.kt`:
```kotlin
package com.bydesigninteractive.ant.sim.world.sdf

import com.bydesigninteractive.ant.sim.util.hash
import com.bydesigninteractive.ant.sim.world.CHUNKS
import com.bydesigninteractive.ant.sim.world.CHUNK_MM
import com.bydesigninteractive.ant.sim.world.FoodSource
import com.bydesigninteractive.ant.sim.world.Stone
import com.bydesigninteractive.ant.sim.world.SurfaceMap
import java.util.Random
import kotlin.math.floor
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.sqrt

/**
 * The walkable surface as a signed distance function in surface millimeters (z up), negative
 * inside matter: the ground, pebbles and rocks generated per chunk, objects [placed] at run
 * time, and food shapes (aphid plant stems and clusters, prey lumps, feeders). Shapes are joined
 * with a smooth minimum, so the crease where a rock meets the ground is rounded.
 */
class SurfaceSdf(private val map: SurfaceMap, private val rocks: Boolean = true) {
    /** Objects placed at run time (tests now; debris in a later milestone). */
    val placed = ArrayList<Blob>()
    private val own = HashMap<Int, List<Blob>>()
    private val near = HashMap<Int, List<Blob>>()
    private val g = FloatArray(3)

    fun distance(x: Float, y: Float, z: Float): Float {
        var d = map.ground.distance(x, y, z)
        for (b in blobsNear(x, y)) if (b.near(x, y, z, MARGIN)) d = smin(d, b.distance(x, y, z), BLEND)
        for (b in placed) if (b.near(x, y, z, MARGIN)) d = smin(d, b.distance(x, y, z), BLEND)
        for (f in map.foods) {
            val body = f.body
            if (body != null && body.near(x, y, z, MARGIN)) d = smin(d, body.distance(x, y, z), BLEND)
            val stem = f.stem
            if (stem != null && stem.near(x, y, z, MARGIN)) d = smin(d, stem.distance(x, y, z), BLEND)
        }
        return d
    }

    /** The unit surface normal direction at (x, y, z) into [out], by central differences. */
    fun gradient(x: Float, y: Float, z: Float, out: FloatArray) {
        val gx = distance(x + E, y, z) - distance(x - E, y, z)
        val gy = distance(x, y + E, z) - distance(x, y - E, z)
        val gz = distance(x, y, z + E) - distance(x, y, z - E)
        val len = sqrt(gx * gx + gy * gy + gz * gz)
        if (len < 1e-9f) {
            out[0] = 0f
            out[1] = 0f
            out[2] = 1f
        } else {
            out[0] = gx / len
            out[1] = gy / len
            out[2] = gz / len
        }
    }

    /** Moves the point (p[0], p[1], p[2]) onto the surface with [ITERATIONS] Newton steps. */
    fun project(p: FloatArray) {
        repeat(ITERATIONS) {
            val d = distance(p[0], p[1], p[2])
            gradient(p[0], p[1], p[2], g)
            p[0] -= g[0] * d
            p[1] -= g[1] * d
            p[2] -= g[2] * d
        }
    }

    /** The plant whose stem surface is within [within] of (x, y, z), or null. */
    fun stemAt(x: Float, y: Float, z: Float, within: Float): FoodSource? {
        for (f in map.foods) {
            val s = f.stem ?: continue
            if (s.distance(x, y, z) <= within) return f
        }
        return null
    }

    /** The pebbles and rocks generated in one chunk (empty for a world without rocks). */
    fun ownBlobs(cx: Int, cy: Int): List<Blob> =
        own.getOrPut(cx + cy * CHUNKS) { if (rocks) generate(cx, cy) else emptyList() }

    private fun blobsNear(x: Float, y: Float): List<Blob> {
        val cx = floor(x / CHUNK_MM).toInt().coerceIn(0, CHUNKS - 1)
        val cy = floor(y / CHUNK_MM).toInt().coerceIn(0, CHUNKS - 1)
        return near.getOrPut(cx + cy * CHUNKS) { gather(cx, cy) }
    }

    /** This chunk's blobs plus neighbors' blobs whose bounds reach into it. */
    private fun gather(cx: Int, cy: Int): List<Blob> {
        val x0 = cx * CHUNK_MM - MARGIN
        val y0 = cy * CHUNK_MM - MARGIN
        val x1 = (cx + 1) * CHUNK_MM + MARGIN
        val y1 = (cy + 1) * CHUNK_MM + MARGIN
        val out = ArrayList<Blob>()
        for (ny in cy - 1..cy + 1) for (nx in cx - 1..cx + 1) {
            if (nx < 0 || ny < 0 || nx >= CHUNKS || ny >= CHUNKS) continue
            for (b in ownBlobs(nx, ny)) {
                if (b.cx + b.reach >= x0 && b.cx - b.reach <= x1 && b.cy + b.reach >= y0 && b.cy - b.reach <= y1) out += b
            }
        }
        return out
    }

    private fun generate(cx: Int, cy: Int): List<Blob> {
        val content = map.chunk(cx, cy)
        val r = Random(hash(map.seed xor BLOB_SALT, cx, cy))
        val out = ArrayList<Blob>()
        for (s in content.stones) addBlob(out, r, s, PEBBLE_LUMP)
        for (s in content.rocks) addBlob(out, r, s, ROCK_LUMP)
        return out
    }

    /** Shapes a partly buried lumpy blob from a 2D stone; all draws happen before any skip. */
    private fun addBlob(out: ArrayList<Blob>, r: Random, s: Stone, lump: Float) {
        val rx = s.radius * (0.8f + 0.4f * r.nextFloat())
        val ry = s.radius * (0.8f + 0.4f * r.nextFloat())
        val rz = s.radius * (0.5f + 0.3f * r.nextFloat())
        val phase = r.nextFloat() * 6.283f
        if (hypot(s.x - map.entranceX, s.y - map.entranceY) - max(rx, ry) < ENTRANCE_CLEARANCE) return
        val cz = map.ground.base(s.x, s.y) + rz * BURIED
        out += Blob(s.x, s.y, cz, rx, ry, rz, lump, phase)
    }

    private companion object {
        const val BLEND = 1.5f
        const val MARGIN = 10f
        const val E = 0.25f
        const val ITERATIONS = 2
        const val ENTRANCE_CLEARANCE = 30f
        const val BURIED = 0.3f
        const val PEBBLE_LUMP = 0.12f
        const val ROCK_LUMP = 0.15f
        const val BLOB_SALT = 0xB10BL
    }
}
```

- [ ] **Step 4: Run all sim tests**

Run: `JAVA_HOME="C:/Program Files/Eclipse Adoptium/jdk-17.0.10.7-hotspot" ./gradlew :sim:test`
Expected: BUILD SUCCESSFUL. The new 6 tests pass; M1 tests are unaffected because M1 behavior still reads only the 2D positions of food.

- [ ] **Step 5: Commit**

```bash
git add sim/src
git commit -m "M1b-1: rocks, food shapes and the surface SDF"
```

---

### Task 5: The sparse 3D field

**Files:**
- Create: `sim/src/main/kotlin/com/bydesigninteractive/ant/sim/world/Field3.kt`
- Test: `sim/src/test/kotlin/com/bydesigninteractive/ant/sim/world/Field3Test.kt`

**Interfaces:**
- Produces: `class Field3(dt: Float, decayPerSecond: Float, diffusion: Float, cellMm: Float = 10f)` with `now: Long`, `blockCount()`, `get(x, y, z)`, `add(x, y, z, amount)`, `step()`, `max()`, `projectMax(x0: Float, y0: Float, cells: Int, out: FloatArray): Boolean`.

Blocks are 10 x 10 x 10 voxels (100 mm) rather than the spec's 16, so five blocks tile a 50 cm chunk exactly for the top-down overlay.

- [ ] **Step 1: Write the failing test**

```kotlin
package com.bydesigninteractive.ant.sim.world

import kotlin.math.exp
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class Field3Test {
    @Test
    fun aVoxelCenterReadsBackWhatWasAdded() {
        val f = Field3(0.05f, 0f, 0f)
        f.add(55f, 55f, 55f, 2f)
        assertEquals(2f, f.get(55f, 55f, 55f), 1e-5f)
        assertEquals(1, f.blockCount())
    }

    @Test
    fun depositsBetweenVoxelsAreSplitTrilinearly() {
        val f = Field3(0.05f, 0f, 0f)
        f.add(10f, 5f, 5f, 1f) // halfway between voxels 0 and 1 in x
        assertEquals(0.5f, f.get(5f, 5f, 5f), 1e-5f)
        assertEquals(0.5f, f.get(15f, 5f, 5f), 1e-5f)
        assertEquals(0.5f, f.get(10f, 5f, 5f), 1e-5f)
    }

    @Test
    fun negativeHeightsWork() {
        val f = Field3(0.05f, 0f, 0f)
        f.add(5f, 5f, -15f, 1f)
        assertEquals(1f, f.get(5f, 5f, -15f), 1e-5f)
    }

    @Test
    fun lazyDecayCatchesUpOnRead() {
        val f = Field3(1f, 0.01f, 0f)
        f.add(55f, 55f, 55f, 1f)
        repeat(50) { f.step() }
        assertEquals(exp(-0.5f), f.get(55f, 55f, 55f), 1e-5f)
    }

    @Test
    fun fadedBlocksAreSweptAway() {
        val f = Field3(1f, 1f, 0f)
        f.add(55f, 55f, 55f, 1f)
        repeat(200) { f.step() }
        assertEquals(0, f.blockCount())
    }

    @Test
    fun diffusionSpreadsToSixNeighborsAndKeepsTheTotal() {
        val f = Field3(0.05f, 0f, 1f)
        f.add(55f, 55f, 55f, 1f) // voxel (5, 5, 5)
        f.step()
        assertEquals(0.7f, f.get(55f, 55f, 55f), 1e-5f)
        assertEquals(0.05f, f.get(65f, 55f, 55f), 1e-5f)
        assertEquals(0.05f, f.get(55f, 55f, 45f), 1e-5f)
    }

    @Test
    fun eagerFieldsDecayEveryStep() {
        val f = Field3(1f, 0.01f, 0.1f)
        f.add(55f, 55f, 55f, 1f)
        f.step()
        assertTrue(f.max() < 1f)
    }

    @Test
    fun projectMaxLooksStraightDown() {
        val f = Field3(0.05f, 0f, 0f)
        f.add(105f, 205f, -5f, 2f)
        f.add(105f, 205f, 305f, 3f)
        val out = FloatArray(50 * 50)
        assertTrue(f.projectMax(0f, 0f, 50, out))
        assertEquals(3f, out[20 * 50 + 10], 1e-5f)
    }
}
```

- [ ] **Step 2: Run to verify it fails**

Run: `JAVA_HOME="C:/Program Files/Eclipse Adoptium/jdk-17.0.10.7-hotspot" ./gradlew :sim:test --tests "*Field3Test"`
Expected: compilation fails (unresolved `Field3`).

- [ ] **Step 3: Implement**

```kotlin
package com.bydesigninteractive.ant.sim.world

import kotlin.math.exp
import kotlin.math.floor
import kotlin.math.max

/**
 * A sparse 3D scalar field in [cellMm] voxels, stored in blocks of 10 x 10 x 10 voxels that are
 * allocated on first write and released when faded. Values are sampled and deposited with
 * trilinear weights, so a trail reads the same whichever voxel an ant stands in.
 *
 * With [diffusion] 0, decay is lazy: a block catches up when touched, and every block is swept
 * every [SWEEP_TICKS] ticks, so untouched ground costs nothing per tick. With diffusion, [step]
 * decays and diffuses every allocated block each tick; spread toward an unallocated block is lost.
 */
class Field3(
    private val dt: Float,
    private val decayPerSecond: Float,
    private val diffusion: Float,
    val cellMm: Float = FIELD_CELL_MM.toFloat(),
) {
    private class Block(val bx: Int, val by: Int, val bz: Int, var tick: Long) {
        var v = FloatArray(B3)
        var scratch = FloatArray(B3)
    }

    private val blocks = LinkedHashMap<Long, Block>()
    private var cacheKey = Long.MIN_VALUE
    private var cacheBlock: Block? = null

    /** Ticks stepped so far. */
    var now = 0L
        private set

    fun blockCount(): Int = blocks.size

    fun get(x: Float, y: Float, z: Float): Float {
        val u = x / cellMm - 0.5f
        val v = y / cellMm - 0.5f
        val w = z / cellMm - 0.5f
        val ix = floor(u).toInt()
        val iy = floor(v).toInt()
        val iz = floor(w).toInt()
        val fx = u - ix
        val fy = v - iy
        val fz = w - iz
        var sum = 0f
        for (c in 0 until 8) {
            val dx = c and 1
            val dy = (c shr 1) and 1
            val dz = (c shr 2) and 1
            val weight = (if (dx == 0) 1f - fx else fx) * (if (dy == 0) 1f - fy else fy) * (if (dz == 0) 1f - fz else fz)
            if (weight == 0f) continue
            sum += weight * voxel(ix + dx, iy + dy, iz + dz)
        }
        return sum
    }

    fun add(x: Float, y: Float, z: Float, amount: Float) {
        val u = x / cellMm - 0.5f
        val v = y / cellMm - 0.5f
        val w = z / cellMm - 0.5f
        val ix = floor(u).toInt()
        val iy = floor(v).toInt()
        val iz = floor(w).toInt()
        val fx = u - ix
        val fy = v - iy
        val fz = w - iz
        for (c in 0 until 8) {
            val dx = c and 1
            val dy = (c shr 1) and 1
            val dz = (c shr 2) and 1
            val weight = (if (dx == 0) 1f - fx else fx) * (if (dy == 0) 1f - fy else fy) * (if (dz == 0) 1f - fz else fz)
            if (weight == 0f) continue
            val gx = ix + dx
            val gy = iy + dy
            val gz = iz + dz
            val b = blockFor(gx, gy, gz, create = true)!!
            catchUp(b)
            b.v[index(gx, gy, gz)] += amount * weight
        }
    }

    fun step() {
        if (diffusion > 0f) {
            diffuse()
        } else if ((now + 1) % SWEEP_TICKS == 0L) {
            sweep()
        }
        now++
    }

    fun max(): Float {
        var m = 0f
        for (b in blocks.values) {
            catchUp(b)
            for (value in b.v) if (value > m) m = value
        }
        return m
    }

    /**
     * The largest value in each column of voxels over the square of [cells] x [cells] voxels whose
     * corner is (x0, y0), into [out] row by row from the low-y edge. False if all are zero.
     */
    fun projectMax(x0: Float, y0: Float, cells: Int, out: FloatArray): Boolean {
        out.fill(0f)
        val ix0 = floor(x0 / cellMm).toInt()
        val iy0 = floor(y0 / cellMm).toInt()
        var any = false
        for (b in blocks.values) {
            val gx0 = b.bx * B
            val gy0 = b.by * B
            if (gx0 + B <= ix0 || gx0 >= ix0 + cells || gy0 + B <= iy0 || gy0 >= iy0 + cells) continue
            catchUp(b)
            for (lz in 0 until B) for (ly in 0 until B) for (lx in 0 until B) {
                val gx = gx0 + lx - ix0
                val gy = gy0 + ly - iy0
                if (gx !in 0 until cells || gy !in 0 until cells) continue
                val value = b.v[(lz * B + ly) * B + lx]
                val o = gy * cells + gx
                if (value > out[o]) {
                    out[o] = value
                    any = true
                }
            }
        }
        return any
    }

    private fun voxel(gx: Int, gy: Int, gz: Int): Float {
        val b = blockFor(gx, gy, gz, create = false) ?: return 0f
        catchUp(b)
        return b.v[index(gx, gy, gz)]
    }

    private fun rawVoxel(gx: Int, gy: Int, gz: Int): Float {
        val b = blockFor(gx, gy, gz, create = false) ?: return 0f
        return b.v[index(gx, gy, gz)]
    }

    private fun blockFor(gx: Int, gy: Int, gz: Int, create: Boolean): Block? {
        val bx = Math.floorDiv(gx, B)
        val by = Math.floorDiv(gy, B)
        val bz = Math.floorDiv(gz, B)
        val key = key(bx, by, bz)
        if (key == cacheKey) return cacheBlock
        var b = blocks[key]
        if (b == null) {
            if (!create) return null
            b = Block(bx, by, bz, now)
            blocks[key] = b
        }
        cacheKey = key
        cacheBlock = b
        return b
    }

    private fun catchUp(b: Block) {
        if (b.tick == now) return
        val factor = exp(-decayPerSecond * dt * (now - b.tick))
        val v = b.v
        for (i in v.indices) v[i] *= factor
        b.tick = now
    }

    private fun diffuse() {
        val keep = exp(-decayPerSecond * dt)
        val k = diffusion * dt
        for (b in blocks.values) {
            val src = b.v
            val dst = b.scratch
            for (lz in 0 until B) for (ly in 0 until B) for (lx in 0 until B) {
                val i = (lz * B + ly) * B + lx
                val gx = b.bx * B + lx
                val gy = b.by * B + ly
                val gz = b.bz * B + lz
                val c = src[i]
                val sum = neighbor(src, lx - 1, ly, lz, gx - 1, gy, gz) + neighbor(src, lx + 1, ly, lz, gx + 1, gy, gz) +
                    neighbor(src, lx, ly - 1, lz, gx, gy - 1, gz) + neighbor(src, lx, ly + 1, lz, gx, gy + 1, gz) +
                    neighbor(src, lx, ly, lz - 1, gx, gy, gz - 1) + neighbor(src, lx, ly, lz + 1, gx, gy, gz + 1)
                dst[i] = (c + k * (sum - 6f * c)) * keep
            }
        }
        val dead = ArrayList<Long>()
        for ((key, b) in blocks) {
            val old = b.v
            b.v = b.scratch
            b.scratch = old
            b.tick = now + 1
            var m = 0f
            for (value in b.v) if (value > m) m = value
            if (m < FADED) dead += key
        }
        for (key in dead) blocks.remove(key)
        cacheKey = Long.MIN_VALUE
        cacheBlock = null
    }

    private fun neighbor(src: FloatArray, lx: Int, ly: Int, lz: Int, gx: Int, gy: Int, gz: Int): Float =
        if (lx in 0 until B && ly in 0 until B && lz in 0 until B) src[(lz * B + ly) * B + lx] else rawVoxel(gx, gy, gz)

    private fun sweep() {
        val dead = ArrayList<Long>()
        for ((key, b) in blocks) {
            catchUp(b)
            var m = 0f
            for (value in b.v) if (value > m) m = value
            if (m < FADED) dead += key
        }
        for (key in dead) blocks.remove(key)
        cacheKey = Long.MIN_VALUE
        cacheBlock = null
    }

    private fun index(gx: Int, gy: Int, gz: Int): Int =
        (Math.floorMod(gz, B) * B + Math.floorMod(gy, B)) * B + Math.floorMod(gx, B)

    private fun key(bx: Int, by: Int, bz: Int): Long =
        ((bx + OFFSET).toLong() shl 42) or ((by + OFFSET).toLong() shl 21) or (bz + OFFSET).toLong()

    private companion object {
        const val B = 10
        const val B3 = B * B * B
        const val SWEEP_TICKS = 200L
        const val FADED = 1e-4f
        const val OFFSET = 1 shl 20
    }
}
```

Note on the diffusion test: after `add`, voxel (5,5,5) holds 1; one step with k = 0.05 gives 1 - 6 * 0.05 = 0.7 at the center and 0.05 at each of the six neighbors.

- [ ] **Step 4: Run to verify it passes**

Run: `JAVA_HOME="C:/Program Files/Eclipse Adoptium/jdk-17.0.10.7-hotspot" ./gradlew :sim:test --tests "*Field3Test"`
Expected: BUILD SUCCESSFUL, 8 tests passed.

- [ ] **Step 5: Commit**

```bash
git add sim/src
git commit -m "M1b-1: a sparse 3D field with lazy decay"
```

---

### Task 6: Walking on the surface

This task switches surface ants to 3D. It touches several files at once because they only compile together.

**Files:**
- Create: `sim/src/main/kotlin/com/bydesigninteractive/ant/sim/ant/SurfaceWalk.kt`
- Delete: `sim/src/main/kotlin/com/bydesigninteractive/ant/sim/ant/SurfaceMotion.kt`
- Modify: `sim/.../ant/Ant.kt`, `sim/.../ant/Forager.kt`, `sim/.../ant/Digger.kt`, `sim/.../World.kt`, `sim/.../world/SurfaceMap.kt`
- Modify: `core/.../render/SurfaceTopRenderer.kt`, `core/.../ui/DebugReadout.kt` (compile against the 3D trail)
- Modify tests: `sim/.../ant/ForagerTest.kt`
- Test: `sim/src/test/kotlin/com/bydesigninteractive/ant/sim/ant/SurfaceWalkTest.kt`

**Interfaces:**
- Consumes: `SurfaceSdf` (`distance`, `gradient`, `project`, `placed`), `HeightField.height`, `Field3` (Tasks 3 to 5).
- Produces: `Ant` gains `z`, `nx`, `ny`, `nz` (default 1), `fx` (default 1), `fy`, `fz`; `heading` stays and is derived for surface ants as the compass angle of `f`. `internal object SurfaceWalk` with `place(w, a, x, y, heading)`, `step(w, a, speed)`, `turn(a, angle)`, `faceDirection(a, dx, dy, dz)`, `faceToward(a, x, y, z)`, `faceCompass(a, angle)`, `wander(w, a, runMean, turnSd)`, `sense(w, a, field: Field3)`, `steerByGradient(w, a, gain)`, `horizontalDot(a, vx, vy)`, `horizontalForward(a)`. `World(seed, params, nestWidth, nestDepth, rocks = true)` with `internal val pos`, `internal val norm` (FloatArray(3) scratch). `SurfaceMap(seed, params = AntParams(), rocks = true)` whose `trail` and `homeScent` are `Field3` and `step()` has no parameters. `SurfaceMap.nearestFood` skips stemmed plants.

- [ ] **Step 1: Write the failing test**

`SurfaceWalkTest.kt`:
```kotlin
package com.bydesigninteractive.ant.sim.ant

import com.bydesigninteractive.ant.sim.World
import com.bydesigninteractive.ant.sim.world.sdf.Blob
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SurfaceWalkTest {
    private fun world(): World {
        val w = World(3, rocks = false)
        w.predig(1)
        return w
    }

    private fun walker(w: World, x: Float, y: Float, heading: Float): Ant {
        val a = w.addAnt(Role.FORAGER)
        a.space = Space.SURFACE
        SurfaceWalk.place(w, a, x, y, heading)
        return a
    }

    private fun gap(w: World, a: Ant) = abs(w.surface.sdf.distance(a.x, a.y, a.z))

    @Test
    fun placedAntsSitOnTheGroundFacingTheirHeading() {
        val w = world()
        val a = walker(w, 4300f, 4000f, 0.3f)
        assertTrue(gap(w, a) < 0.5f)
        assertEquals(0.3f, a.heading, 0.05f)
        assertTrue(a.nz > 0.9f)
    }

    @Test
    fun turningLeftFollowsTheCompass() {
        val w = world()
        val a = walker(w, 4300f, 4000f, 0f)
        SurfaceWalk.turn(a, (PI / 2).toFloat())
        assertEquals((PI / 2).toFloat(), a.heading, 0.1f)
    }

    @Test
    fun anAntWalksOverARockAndBackDown() {
        val w = world()
        val ground = w.surface.ground.height(4300f, 4000f)
        w.surface.sdf.placed += Blob(4300f, 4000f, ground + 5f, 25f, 25f, 25f)
        val a = walker(w, 4200f, 4000f, 0f)
        var top = -1e9f
        repeat(400) {
            SurfaceWalk.step(w, a, 20f)
            assertTrue(gap(w, a) < 0.5f, "left the surface at (${a.x}, ${a.y}, ${a.z})")
            top = max(top, a.z - ground)
        }
        assertTrue(top > 25f, "highest point $top")
        assertTrue(a.x > 4350f, "ended at x ${a.x}")
        assertTrue(a.z - w.surface.ground.height(a.x, a.y) < 2f)
    }

    @Test
    fun walkersStayOnTheSurfaceIncludingUnderAnOverhang() {
        val w = world()
        val ground = w.surface.ground.height(4300f, 4000f)
        w.surface.sdf.placed += Blob(4300f, 4000f, ground + 25f, 30f, 30f, 30f)
        val ants = List(40) { i ->
            val angle = i * 0.157f
            walker(w, 4300f + cos(angle) * 45f, 4000f + sin(angle) * 45f, angle + PI.toFloat())
        }
        var underside = 0
        repeat(20 * 60 * 3) {
            for (a in ants) {
                SurfaceWalk.wander(w, a, 20f, 0.9f)
                SurfaceWalk.step(w, a, 20f)
                assertTrue(gap(w, a) < 0.5f, "left the surface at (${a.x}, ${a.y}, ${a.z})")
                if (a.nz < -0.3f) underside++
            }
        }
        assertTrue(underside > 0, "nobody walked the underside")
    }

    @Test
    fun sensingReadsTheTrailAheadLeftAndRight() {
        val w = world()
        val a = walker(w, 4300f, 4000f, 0f)
        val left = w.params.senseAngle
        val x = a.x + cos(left) * w.params.senseAhead
        val y = a.y + sin(left) * w.params.senseAhead
        w.surface.trail.add(x, y, w.surface.ground.height(x, y), 5f)
        SurfaceWalk.sense(w, a, w.surface.trail)
        assertTrue(a.senseL > a.senseR)
    }
}
```

- [ ] **Step 2: Run to verify it fails**

Run: `JAVA_HOME="C:/Program Files/Eclipse Adoptium/jdk-17.0.10.7-hotspot" ./gradlew :sim:test --tests "*SurfaceWalkTest"`
Expected: compilation fails (unresolved `SurfaceWalk`, `rocks`, `a.z`).

- [ ] **Step 3: Add the pose to `Ant`**

In `Ant.kt`, below `var heading = 0f`, add:
```kotlin
    // Surface pose: height, unit normal (the ant's up) and unit forward in the tangent plane.
    // For a surface ant, heading is the compass angle of the forward vector.
    var z = 0f
    var nx = 0f
    var ny = 0f
    var nz = 1f
    var fx = 1f
    var fy = 0f
    var fz = 0f
```
and update the class KDoc's first sentence to: "Positions are millimeters: on the 3D surface in [Space.SURFACE] (x, y on the map, z up), and on the nest slice (y = depth) in [Space.NEST], where the ant also walks cell to cell."

- [ ] **Step 4: Write `SurfaceWalk.kt` and delete `SurfaceMotion.kt`**

```kotlin
package com.bydesigninteractive.ant.sim.ant

import com.bydesigninteractive.ant.sim.DT
import com.bydesigninteractive.ant.sim.World
import com.bydesigninteractive.ant.sim.world.Field3
import com.bydesigninteractive.ant.sim.world.SURFACE_MM
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Walking on the 3D surface. A step moves along the forward vector, projects back onto the
 * surface, reads the new normal and carries the forward vector into the new tangent plane, so
 * walking from the ground onto a rock, under an overhang or up a stem needs no special cases.
 * Ants never fall. Path integration uses the horizontal part of each step (section 4).
 */
internal object SurfaceWalk {
    private const val FOLD = 0.2f

    /** Puts a surface ant on the surface at (x, y), facing compass angle [heading]. */
    fun place(w: World, a: Ant, x: Float, y: Float, heading: Float) {
        val p = w.pos
        p[0] = x
        p[1] = y
        p[2] = w.surface.ground.height(x, y)
        w.surface.sdf.project(p)
        a.x = p[0]
        a.y = p[1]
        a.z = p[2]
        a.heading = heading
        normal(w, a)
        a.fx = cos(heading)
        a.fy = sin(heading)
        a.fz = 0f
        transport(a)
    }

    fun step(w: World, a: Ant, speed: Float) {
        val s = speed * DT
        val ox = a.x
        val oy = a.y
        val max = SURFACE_MM.toFloat()
        var x = a.x + a.fx * s
        var y = a.y + a.fy * s
        val bounced = x < 0f || x > max || y < 0f || y > max
        x = x.coerceIn(0f, max)
        y = y.coerceIn(0f, max)
        val p = w.pos
        p[0] = x
        p[1] = y
        p[2] = a.z + a.fz * s
        w.surface.sdf.project(p)
        a.x = p[0]
        a.y = p[1]
        a.z = p[2]
        normal(w, a)
        transport(a)
        if (bounced) turn(a, PI.toFloat())
        val noise = w.params.piErrorPerMm * s
        a.homeDx += (a.x - ox) + w.gaussian() * noise
        a.homeDy += (a.y - oy) + w.gaussian() * noise
        a.speed = speed
        w.surface.homeScent.add(a.x, a.y, a.z, w.params.homeScentPerSecond * DT)
    }

    /** Rotates the forward vector about the normal; positive is to the ant's left. */
    fun turn(a: Ant, angle: Float) {
        val c = cos(angle)
        val s = sin(angle)
        val cx = a.ny * a.fz - a.nz * a.fy
        val cy = a.nz * a.fx - a.nx * a.fz
        val cz = a.nx * a.fy - a.ny * a.fx
        a.fx = a.fx * c + cx * s
        a.fy = a.fy * c + cy * s
        a.fz = a.fz * c + cz * s
        transport(a)
    }

    /** Faces the direction (dx, dy, dz) as projected into the tangent plane, if it has one. */
    fun faceDirection(a: Ant, dx: Float, dy: Float, dz: Float) {
        val d = dx * a.nx + dy * a.ny + dz * a.nz
        val px = dx - a.nx * d
        val py = dy - a.ny * d
        val pz = dz - a.nz * d
        val len = sqrt(px * px + py * py + pz * pz)
        if (len < 1e-4f) return
        a.fx = px / len
        a.fy = py / len
        a.fz = pz / len
        updateHeading(a)
    }

    fun faceToward(a: Ant, x: Float, y: Float, z: Float) = faceDirection(a, x - a.x, y - a.y, z - a.z)

    fun faceCompass(a: Ant, angle: Float) = faceDirection(a, cos(angle), sin(angle), 0f)

    /** A search walk: straight runs of exponential length, then a random turn (section 5). */
    fun wander(w: World, a: Ant, runMean: Float, turnSd: Float) {
        a.runLeft -= w.params.surfaceSpeed * DT
        if (a.runLeft > 0f) return
        turn(a, w.gaussian() * turnSd)
        a.runLeft = w.exponential(runMean)
    }

    /** Samples [field] ahead-left and ahead-right on the surface into senseL and senseR (section 4). */
    fun sense(w: World, a: Ant, field: Field3) {
        a.senseL = sample(w, a, field, w.params.senseAngle)
        a.senseR = sample(w, a, field, -w.params.senseAngle)
    }

    /** Turns toward the stronger side in proportion to (L - R) / (L + R) (section 4). */
    fun steerByGradient(w: World, a: Ant, gain: Float) {
        val s = a.senseL + a.senseR
        if (s <= 0f) return
        turn(a, gain * (a.senseL - a.senseR) / s * DT)
    }

    /** The horizontal part of the forward vector dotted with (vx, vy). */
    fun horizontalDot(a: Ant, vx: Float, vy: Float): Float = a.fx * vx + a.fy * vy

    /** How much of the forward vector is horizontal, 0 (straight up or down) to 1. */
    fun horizontalForward(a: Ant): Float = sqrt(a.fx * a.fx + a.fy * a.fy)

    private fun sample(w: World, a: Ant, field: Field3, angle: Float): Float {
        val c = cos(angle)
        val s = sin(angle)
        val cx = a.ny * a.fz - a.nz * a.fy
        val cy = a.nz * a.fx - a.nx * a.fz
        val cz = a.nx * a.fy - a.ny * a.fx
        val ahead = w.params.senseAhead
        val p = w.pos
        p[0] = a.x + (a.fx * c + cx * s) * ahead
        p[1] = a.y + (a.fy * c + cy * s) * ahead
        p[2] = a.z + (a.fz * c + cz * s) * ahead
        w.surface.sdf.project(p)
        return field.get(p[0], p[1], p[2])
    }

    private fun normal(w: World, a: Ant) {
        val n = w.norm
        w.surface.sdf.gradient(a.x, a.y, a.z, n)
        a.nx = n[0]
        a.ny = n[1]
        a.nz = n[2]
    }

    /** Carries the forward vector into the tangent plane; rebuilds it at a sharp fold. */
    private fun transport(a: Ant) {
        val d = a.fx * a.nx + a.fy * a.ny + a.fz * a.nz
        var fx = a.fx - a.nx * d
        var fy = a.fy - a.ny * d
        var fz = a.fz - a.nz * d
        var len = sqrt(fx * fx + fy * fy + fz * fz)
        if (len < FOLD) {
            // The old forward points along the normal: fall back to the compass heading.
            fx = cos(a.heading)
            fy = sin(a.heading)
            val e = fx * a.nx + fy * a.ny
            fx -= a.nx * e
            fy -= a.ny * e
            fz = -a.nz * e
            len = sqrt(fx * fx + fy * fy + fz * fz)
            if (len < FOLD) {
                // The normal is horizontal along the heading: head up the face.
                fx = -a.nx * a.nz
                fy = -a.ny * a.nz
                fz = 1f - a.nz * a.nz
                len = sqrt(fx * fx + fy * fy + fz * fz)
            }
        }
        a.fx = fx / len
        a.fy = fy / len
        a.fz = fz / len
        updateHeading(a)
    }

    private fun updateHeading(a: Ant) {
        if (a.fx * a.fx + a.fy * a.fy > 1e-6f) a.heading = atan2(a.fy, a.fx)
    }
}
```
Delete `SurfaceMotion.kt`.

- [ ] **Step 5: Wire the world and the surface map**

`SurfaceMap.kt`:
- Constructor: `class SurfaceMap(val seed: Long, params: AntParams = AntParams(), rocks: Boolean = true)` (import `com.bydesigninteractive.ant.sim.ant.AntParams` and `com.bydesigninteractive.ant.sim.DT`).
- Replace the two 2D fields with 3D fields:
```kotlin
    /** Trail pheromone on the 3D surface (section 5): narrow, decays 0.4% per second. */
    val trail = Field3(DT, params.trailDecay, params.trailDiffusion)

    /** Home-range scent laid by every walking ant; slow, lazily decayed. */
    val homeScent = Field3(DT, params.homeScentDecay, 0f)
```
- Replace `step(dt, trailDecay, trailDiffusion, scentDecay)` with:
```kotlin
    fun step() {
        trail.step()
        homeScent.step()
    }
```
- In `nearestFood`, skip plants: first line of the loop body `if (f.hasStem) continue`, and update its KDoc to "The closest food on the ground (not a plant) whose edge is within [within] of (x, y), or null."

`World.kt`:
- Constructor gains `rocks: Boolean = true` (last parameter) and builds `val surface = SurfaceMap(seed, params, rocks)`.
- Add scratch arrays:
```kotlin
    // Scratch vectors for surface walking (one simulation thread).
    internal val pos = FloatArray(3)
    internal val norm = FloatArray(3)
```
- `step()`: replace the surface line with `surface.step()`.
- `exitNest`: replace the position and heading lines with
```kotlin
        SurfaceWalk.place(this, a, surface.entranceX + cos(angle) * r, surface.entranceY + sin(angle) * r, angle)
```
keeping the home vector, `runLeft`, `onTrail`, `timer` and `arrived` lines after it (the home vector uses the placed `a.x`/`a.y`). Import `com.bydesigninteractive.ant.sim.ant.SurfaceWalk`.

- [ ] **Step 6: Port the forager**

Replace the body of `Forager.kt` with:
```kotlin
package com.bydesigninteractive.ant.sim.ant

import com.bydesigninteractive.ant.sim.DT
import com.bydesigninteractive.ant.sim.FeedEvent
import com.bydesigninteractive.ant.sim.World
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * The scripted forager: leave the nest, search (joining trails with a chance that rises steeply
 * with their strength), feed, walk home by path integration laying trail if it filled up, unload
 * inside the nest, and go again. Values come from sections 3 to 5 of the simulation reference.
 * On the 3D surface it steers through [SurfaceWalk] turns and facings.
 */
internal object Forager {
    private const val HOME_VECTOR_DONE = 15f
    private const val OUTWARD_FROM = 30f
    private const val CROWD_CUT = 4.6f // up to 5.6x less deposition when crowded

    fun update(w: World, a: Ant) {
        when (a.state) {
            AntState.EXIT -> if (NestMotion.goUp(w, a)) {
                w.exitNest(a)
                chooseExitTrail(w, a)
                a.state = AntState.SEARCH
            }
            AntState.UNLOAD -> unload(w, a)
            AntState.SEARCH -> search(w, a)
            AntState.FEED -> feed(w, a)
            AntState.RETURN -> goHome(w, a)
            else -> a.state = AntState.EXIT
        }
    }

    private fun unload(w: World, a: Ant) {
        val p = w.params
        if (!a.arrived) {
            if (!NestMotion.goDown(w, a, p.unloadDepth)) {
                a.arrived = true
                a.timer = p.unloadSeconds
            }
            return
        }
        a.speed = 0f
        a.timer -= DT
        if (a.timer > 0f) return
        a.crop = 0f
        a.arrived = false
        w.unloads++
        a.state = AntState.EXIT
    }

    private fun search(w: World, a: Ant) {
        val p = w.params
        val s = w.surface
        a.timer += DT
        if (a.timer > p.searchGiveUp) {
            a.onTrail = false
            a.runLeft = 0f
            a.state = AntState.RETURN
            return
        }
        val food = s.nearestFood(a.x, a.y, p.foodSenseRadius)
        if (food != null) {
            val dx = food.x - a.x
            val dy = food.y - a.y
            if (dx * dx + dy * dy <= food.radius * food.radius) {
                startFeeding(w, a, food)
                return
            }
            SurfaceWalk.faceToward(a, food.x, food.y, food.z)
            SurfaceWalk.step(w, a, p.surfaceSpeed)
            return
        }
        SurfaceWalk.sense(w, a, s.trail)
        val strength = a.senseL + a.senseR
        if (a.onTrail) {
            if (strength < p.trailThreshold * p.trailLossFraction) {
                a.onTrail = false
            } else {
                SurfaceWalk.steerByGradient(w, a, p.trailTurnGain)
                keepOutward(a)
            }
        }
        if (!a.onTrail) {
            a.runLeft -= p.surfaceSpeed * DT
            if (a.runLeft <= 0f) {
                // A decision point: join the trail here with a chance that rises steeply with its strength.
                val s2 = strength * strength
                val follow = p.maxFollow * s2 / (s2 + p.trailThreshold * p.trailThreshold)
                if (w.rng.nextFloat() < follow) {
                    a.onTrail = true
                } else {
                    SurfaceWalk.turn(a, w.gaussian() * p.outTurnSd)
                    a.runLeft = w.exponential(p.outRunMean)
                }
            }
        }
        SurfaceWalk.step(w, a, p.surfaceSpeed)
    }

    private fun startFeeding(w: World, a: Ant, food: com.bydesigninteractive.ant.sim.world.FoodSource) {
        a.food = food
        a.onTrail = false
        a.speed = 0f
        a.timer = w.params.feedSeconds
        a.state = AntState.FEED
    }

    /**
     * A forager just out of the nest samples the trail on a ring around the entrance. It joins a
     * trail with a chance that rises steeply with the total, and picks a direction by Deneubourg's
     * choice function (k + s_i)^n / sum (k + s_j)^n, so stronger trails win more than their share
     * (section 5). Otherwise it keeps its random exit heading.
     */
    fun chooseExitTrail(w: World, a: Ant) {
        val p = w.params
        val s = w.surface
        val n = p.exitDirections
        val samples = FloatArray(n)
        var total = 0f
        for (i in 0 until n) {
            val angle = i * 2f * PI.toFloat() / n
            val x = s.entranceX + cos(angle) * p.exitSenseRadius
            val y = s.entranceY + sin(angle) * p.exitSenseRadius
            samples[i] = s.trail.get(x, y, s.ground.height(x, y))
            total += samples[i]
        }
        val t2 = total * total
        val join = p.maxFollow * t2 / (t2 + p.trailThreshold * p.trailThreshold)
        if (w.rng.nextFloat() >= join) return
        var sum = 0f
        for (i in 0 until n) {
            samples[i] = (p.exitChoiceK + samples[i]).pow(p.exitChoiceExponent)
            sum += samples[i]
        }
        var pick = w.rng.nextFloat() * sum
        var chosen = n - 1
        for (i in 0 until n) {
            pick -= samples[i]
            if (pick < 0f) {
                chosen = i
                break
            }
        }
        SurfaceWalk.faceCompass(a, chosen * 2f * PI.toFloat() / n)
        a.onTrail = true
    }

    /** An outbound ant on a trail keeps walking away from home, never back along it. */
    private fun keepOutward(a: Ant) {
        if (a.homeDx * a.homeDx + a.homeDy * a.homeDy < OUTWARD_FROM * OUTWARD_FROM) return
        if (SurfaceWalk.horizontalForward(a) < 0.5f) return // climbing: no compass sense of outward
        if (SurfaceWalk.horizontalDot(a, a.homeDx, a.homeDy) < 0f) SurfaceWalk.turn(a, PI.toFloat())
    }

    private fun feed(w: World, a: Ant) {
        a.speed = 0f
        a.timer -= DT
        if (a.timer > 0f) return
        val food = a.food
        if (food != null) {
            a.crop = food.quality
            w.feedEvents += FeedEvent(w.tick, food.id)
            if (food.loads != Int.MAX_VALUE) {
                food.loads--
                if (food.loads <= 0) w.surface.foods.remove(food)
            }
        }
        a.food = null
        a.runLeft = 0f
        a.timer = 0f
        a.state = AntState.RETURN
    }

    private fun goHome(w: World, a: Ant) {
        val p = w.params
        val s = w.surface
        val ex = s.entranceX - a.x
        val ey = s.entranceY - a.y
        val toEntrance = sqrt(ex * ex + ey * ey)
        if (toEntrance <= p.entranceRadius) {
            w.enterNest(a)
            a.arrived = false
            a.state = AntState.UNLOAD
            return
        }
        when {
            toEntrance <= p.homeSightRadius ->
                SurfaceWalk.faceToward(a, s.entranceX, s.entranceY, s.ground.height(s.entranceX, s.entranceY))
            a.homeDx * a.homeDx + a.homeDy * a.homeDy > HOME_VECTOR_DONE * HOME_VECTOR_DONE -> {
                a.runLeft -= p.surfaceSpeed * DT
                if (a.runLeft <= 0f) {
                    SurfaceWalk.faceCompass(a, atan2(-a.homeDy, -a.homeDx) + w.gaussian() * p.homeTurnSd)
                    a.runLeft = w.exponential(p.homeRunMean)
                }
            }
            else -> {
                // The home vector has run out short of the nest: climb the home-range scent.
                SurfaceWalk.sense(w, a, s.homeScent)
                SurfaceWalk.steerByGradient(w, a, p.trailTurnGain)
                SurfaceWalk.wander(w, a, p.outRunMean, p.outTurnSd)
            }
        }
        layTrail(w, a)
        SurfaceWalk.step(w, a, p.surfaceSpeed)
    }

    /**
     * Marks the trail on the way home, only if the ant filled its crop to its desired volume.
     * Less is laid where the trail is already strong and where it is crowded (section 5).
     */
    private fun layTrail(w: World, a: Ant) {
        val p = w.params
        if (!a.laysTrail || a.crop < a.desiredCrop) return
        if (w.rng.nextFloat() >= p.markChancePerMm * p.surfaceSpeed * DT) return
        val trail = w.surface.trail
        val saturation = 1f / (1f + trail.get(a.x, a.y, a.z) / p.saturationLevel)
        val crowd = w.surfaceIndex.countNear(a, p.crowdRadius)
        val crowding = 1f / (1f + CROWD_CUT * min(1f, crowd / p.crowdLevel.toFloat()))
        trail.add(a.x, a.y, a.z, p.markAmount * saturation * crowding)
    }
}
```
Replace the fully qualified `com.bydesigninteractive.ant.sim.world.FoodSource` in `startFeeding` with an import of `com.bydesigninteractive.ant.sim.world.FoodSource`.

- [ ] **Step 7: Port the digger's surface states**

In `Digger.kt`, replace `dump` and `goHome` and fix the imports (remove `cos` and `sin`; keep `atan2`, `abs`, `hypot`):
```kotlin
    private fun dump(w: World, a: Ant) {
        val p = w.params
        val s = w.surface
        val ox = a.x - s.entranceX
        val oy = a.y - s.entranceY
        val dist = hypot(ox, oy)
        if (dist > p.entranceRadius + 3f) {
            val rate = p.spoilDropBase + p.spoilDropPerPellet * s.spoil.get(a.x, a.y)
            if (dist >= p.spoilMaxDistance || w.rng.nextFloat() < rate * DT) {
                s.spoil.add(a.x, a.y, 1f)
                a.carriesPellet = false
                a.state = AntState.GO_HOME
                return
            }
        }
        SurfaceWalk.wander(w, a, p.outRunMean, p.outTurnSd)
        // Keep walking away from the entrance so the pile never buries it.
        if (SurfaceWalk.horizontalDot(a, ox, oy) < 0f) SurfaceWalk.faceCompass(a, atan2(oy, ox) + w.gaussian() * 0.5f)
        SurfaceWalk.step(w, a, p.surfaceSpeed * p.loadedFactor)
    }

    private fun goHome(w: World, a: Ant) {
        val p = w.params
        val s = w.surface
        val dx = s.entranceX - a.x
        val dy = s.entranceY - a.y
        if (hypot(dx, dy) <= p.entranceRadius) {
            w.enterNest(a)
            a.state = AntState.IDLE
            return
        }
        if (s.spoil.get(a.x, a.y) >= 1f && w.rng.nextFloat() < p.spoilPickUp * DT) {
            s.spoil.add(a.x, a.y, -1f)
            a.carriesPellet = true
            a.state = AntState.DUMP
            return
        }
        SurfaceWalk.faceToward(a, s.entranceX, s.entranceY, s.ground.height(s.entranceX, s.entranceY))
        SurfaceWalk.step(w, a, p.surfaceSpeed)
    }
```

- [ ] **Step 8: Update the forager tests**

In `ForagerTest.kt`:
- In `emergingForagersChooseTheStrongerSideAtTheExit`, lay the trail at ground height: `s.trail.add(x, s.entranceY + 1f, s.ground.height(x, s.entranceY + 1f), 10f)`.
- In `pathIntegrationErrorGrowsWithDistance`, replace `a.heading = 0f` with `SurfaceWalk.faceCompass(a, 0f)` and `SurfaceMotion.advance(w, a, 20f)` with `SurfaceWalk.step(w, a, 20f)`.
- In `theSameSeedGivesTheSameRun`, also compare `z`: `assertEquals(a.ants[i].z, b.ants[i].z)`.

- [ ] **Step 9: Make `core` compile against the 3D trail**

`DebugReadout.kt`: replace the trail line's pieces with `"strongest trail ${f1(w.surface.trail.max())}   trail blocks ${w.surface.trail.blockCount()}   "`.

`SurfaceTopRenderer.kt`: replace the trail read in `overlay`. Keep a reusable buffer `private val trailCells = FloatArray(OVERLAY_CELLS * OVERLAY_CELLS)` and change the start of `overlay` to:
```kotlin
        val key = cx + cy * CHUNKS
        if (!rebuild) return overlays[key]
        val hasTrail = showTrail &&
            world.surface.trail.projectMax(cx * CHUNK_MM.toFloat(), cy * CHUNK_MM.toFloat(), OVERLAY_CELLS, trailCells)
        val spoil = world.surface.spoil.chunk(cx, cy)
        if (!hasTrail && spoil == null) {
            overlays.remove(key)?.dispose()
            return null
        }
        val n = OVERLAY_CELLS
```
and inside the pixel loop use `val t = if (hasTrail) trailCells[i] else 0f`. Add `const val OVERLAY_CELLS = CHUNK_MM / 10` to its companion. The spoil field keeps its 50 cells per chunk, so the indices match.

- [ ] **Step 10: Run the tests**

Run: `JAVA_HOME="C:/Program Files/Eclipse Adoptium/jdk-17.0.10.7-hotspot" ./gradlew :sim:test :core:compileKotlin :core:test`
Expected: BUILD SUCCESSFUL for everything except, possibly, `GruterScenarioTest`: the new geometry and the trilinear field change trail strengths, and Task 8 recalibrates. Record the Gruter failure messages (they print the measured values) in the report. Every other test must pass. If `walkersStayOnTheSurfaceIncludingUnderAnOverhang` or `anAntWalksOverARockAndBackDown` fails the 0.5 mm check, raise `SurfaceSdf.ITERATIONS` to 3 before anything else and report it.

- [ ] **Step 11: Commit**

```bash
git add sim/src core/src
git commit -m "M1b-1: ants walk on the 3D surface"
```

---

### Task 7: Plants: odour, climbing and coming down

**Files:**
- Modify: `sim/.../ant/AntParams.kt`, `sim/.../ant/Forager.kt`, `sim/.../world/SurfaceMap.kt`
- Test: `sim/src/test/kotlin/com/bydesigninteractive/ant/sim/ant/StemTest.kt`

**Interfaces:**
- Consumes: `SurfaceSdf.stemAt`, `FoodSource.hasStem`, `SurfaceWalk` (Task 6).
- Produces: `AntParams.plantOdourRadius = 150f`, `stemTouch = 3f`, `stemLeaveHeight = 10f`; `SurfaceMap.nearestPlant(x, y, within): FoodSource?`.

- [ ] **Step 1: Write the failing test**

```kotlin
package com.bydesigninteractive.ant.sim.ant

import com.bydesigninteractive.ant.sim.World
import com.bydesigninteractive.ant.sim.world.FoodKind
import com.bydesigninteractive.ant.sim.world.FoodSource
import kotlin.math.max
import kotlin.test.Test
import kotlin.test.assertTrue

class StemTest {
    private fun world(): World {
        val w = World(7, rocks = false)
        w.predig(3)
        return w
    }

    private fun plant(w: World, dx: Float, tall: Float): FoodSource {
        val s = w.surface
        val x = s.entranceX + dx
        val y = s.entranceY
        val g = s.ground.height(x, y)
        val f = FoodSource(
            s.foods.size, FoodKind.HONEYDEW, x, y, 12f, 1f,
            z = g + tall - 30f, bodyRadius = 8f, stemRadius = 2.5f, stemBase = g - 5f,
        )
        s.foods += f
        return f
    }

    @Test
    fun foragersClimbAStemFeedAtTheAphidsAndBringItHome() {
        val w = world()
        val f = plant(w, 100f, 500f)
        repeat(5) { w.addAnt(Role.FORAGER) }
        var highest = -1e9f
        repeat(20 * 60 * 15) {
            w.step()
            for (a in w.ants) if (a.space == Space.SURFACE) highest = max(highest, a.z)
        }
        assertTrue(w.feedEvents.any { it.foodId == f.id }, "no feeds at the plant")
        assertTrue(highest > f.z - 20f, "highest climb ${highest - f.z} below the aphids")
        assertTrue(w.unloads >= 1, "nobody brought honeydew home")
    }

    @Test
    fun aTrailLeadsForagersToAPlantBeyondItsOdour() {
        fun feeds(withTrail: Boolean): Int {
            val w = world()
            val f = plant(w, 600f, 300f)
            val s = w.surface
            if (withTrail) {
                var x = s.entranceX + 10f
                while (x < f.x - 3f) {
                    s.trail.add(x, s.entranceY, s.ground.height(x, s.entranceY), 10f)
                    x += 5f
                }
                var z = f.stemBase + 5f
                while (z < f.z) {
                    s.trail.add(f.x - 2.5f, f.y, z, 10f)
                    z += 5f
                }
            }
            repeat(20) { w.addAnt(Role.FORAGER) }
            repeat(20 * 60 * 15) { w.step() }
            return w.feedEvents.count { it.foodId == f.id }
        }
        val with = feeds(true)
        val without = feeds(false)
        assertTrue(with >= 5 && with > without, "feeds with a trail $with, without $without")
    }
}
```

- [ ] **Step 2: Run to verify it fails**

Run: `JAVA_HOME="C:/Program Files/Eclipse Adoptium/jdk-17.0.10.7-hotspot" ./gradlew :sim:test --tests "*StemTest"`
Expected: both tests fail (no feeds at the plant: foragers ignore plants since Task 6).

- [ ] **Step 3: Implement**

`AntParams.kt`, after `foodSenseRadius`:
```kotlin
    // Plants: honeydew odour draws searching foragers to the stem, and an empty forager touching
    // a stem climbs to the aphids 0.5 to 1.2 m up (section 13). Scripted stand-ins for M2 brains.
    val plantOdourRadius: Float = 150f,
    val stemTouch: Float = 3f,
    val stemLeaveHeight: Float = 10f,
```

`SurfaceMap.kt`, after `nearestFood`:
```kotlin
    /** The closest aphid plant whose stem is within [within] of (x, y) horizontally, or null. */
    fun nearestPlant(x: Float, y: Float, within: Float): FoodSource? {
        var best: FoodSource? = null
        var bestGap = within
        for (f in foods) {
            if (!f.hasStem) continue
            val gap = hypot(f.x - x, f.y - y)
            if (gap <= bestGap) {
                bestGap = gap
                best = f
            }
        }
        return best
    }
```

`Forager.kt`:
- In `search`, right after the give-up block and before `val food = s.nearestFood(...)`, insert:
```kotlin
        if (climbOrApproachPlant(w, a)) return
```
- Add:
```kotlin
    /**
     * Plants: an empty forager touching a stem climbs it and feeds at the aphid cluster; one in
     * range of a plant's honeydew odour heads for the stem. True if it acted this tick.
     */
    private fun climbOrApproachPlant(w: World, a: Ant): Boolean {
        val p = w.params
        val s = w.surface
        if (a.crop > 0f) return false
        val onStem = s.sdf.stemAt(a.x, a.y, a.z, p.stemTouch)
        if (onStem != null) {
            val dx = onStem.x - a.x
            val dy = onStem.y - a.y
            val dz = onStem.z - a.z
            if (dx * dx + dy * dy + dz * dz <= onStem.radius * onStem.radius) {
                startFeeding(w, a, onStem)
                return true
            }
            a.onTrail = false
            SurfaceWalk.faceDirection(a, 0f, 0f, 1f)
            SurfaceWalk.step(w, a, p.surfaceSpeed)
            return true
        }
        val plant = s.nearestPlant(a.x, a.y, p.plantOdourRadius) ?: return false
        SurfaceWalk.faceDirection(a, plant.x - a.x, plant.y - a.y, 0f)
        SurfaceWalk.step(w, a, p.surfaceSpeed)
        return true
    }
```
- In `goHome`, after the entrance check and before `when`, insert:
```kotlin
        if (s.sdf.stemAt(a.x, a.y, a.z, p.stemTouch) != null &&
            a.z > s.ground.height(a.x, a.y) + p.stemLeaveHeight
        ) {
            // Still on the plant: come down the stem before homing.
            SurfaceWalk.faceDirection(a, 0f, 0f, -1f)
            layTrail(w, a)
            SurfaceWalk.step(w, a, p.surfaceSpeed)
            return
        }
```

- [ ] **Step 4: Run to verify it passes**

Run: `JAVA_HOME="C:/Program Files/Eclipse Adoptium/jdk-17.0.10.7-hotspot" ./gradlew :sim:test --tests "*StemTest" --tests "*ForagerTest" --tests "*SurfaceWalkTest"`
Expected: BUILD SUCCESSFUL. If `foragersClimb...` fails, print per-minute counts of ants touching the stem, their highest z, and states, and find the root cause (for example the aphid blob stopping the climb short of feeding reach) before changing anything; do not loosen the assertions.

- [ ] **Step 5: Commit**

```bash
git add sim/src
git commit -m "M1b-1: foragers smell plants, climb stems and come back down"
```

---

### Task 8: Scenarios, the Gruter test again, determinism and the benchmark

**Files:**
- Modify: `sim/.../scenario/Scenarios.kt`
- Possibly modify: `sim/.../ant/AntParams.kt` (calibration)
- Test: `sim/src/test/kotlin/com/bydesigninteractive/ant/sim/scenario/SurfaceRunTest.kt` (new)

**Interfaces:**
- Consumes: everything above.
- Produces: `Scenarios.gruter` builds a rock-free world with feeders as small ground blobs.

- [ ] **Step 1: Update the Gruter scenario**

```kotlin
    /**
     * Gruter 2012: two identical feeders at equal [distance] on either side of the entrance, as
     * small food blobs on gently rolling ground with no rocks, so the geometry matches M1's test.
     */
    fun gruter(seed: Long, foragers: Int, distance: Float = 250f, quality: Float = 1f): World {
        val w = World(seed, rocks = false)
        w.predig(5)
        val s = w.surface
        for ((id, x) in listOf(0 to s.entranceX - distance, 1 to s.entranceX + distance)) {
            s.foods += FoodSource(
                id, FoodKind.HONEYDEW, x, s.entranceY, 15f, quality,
                z = s.ground.height(x, s.entranceY) + 3f, bodyRadius = 8f,
            )
        }
        repeat(foragers) { w.addAnt(Role.FORAGER) }
        return w
    }
```

- [ ] **Step 2: Write the determinism and benchmark tests**

```kotlin
package com.bydesigninteractive.ant.sim.scenario

import com.bydesigninteractive.ant.sim.World
import com.bydesigninteractive.ant.sim.ant.AntState
import com.bydesigninteractive.ant.sim.ant.Role
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SurfaceRunTest {
    @Test
    fun theSameSeedGivesTheSameStarterRun() {
        val a = Scenarios.starter(5)
        val b = Scenarios.starter(5)
        repeat(5000) {
            a.step()
            b.step()
        }
        for (i in a.ants.indices) {
            assertEquals(a.ants[i].x, b.ants[i].x)
            assertEquals(a.ants[i].y, b.ants[i].y)
            assertEquals(a.ants[i].z, b.ants[i].z)
        }
    }

    /** 1,000 searching foragers for 60 simulated seconds; reports the cost per tick. */
    @Test
    fun benchmarkOneThousandSurfaceAnts() {
        val w = World(11)
        w.surface.placePlants()
        w.surface.placePrey()
        w.predig(1)
        repeat(1000) {
            val a = w.addAnt(Role.FORAGER)
            w.exitNest(a)
            a.state = AntState.SEARCH
        }
        repeat(100) { w.step() } // warm up the JIT and the height grids
        val ticks = 60 * 20
        val start = System.nanoTime()
        repeat(ticks) { w.step() }
        val msPerTick = (System.nanoTime() - start) / 1e6 / ticks
        println("BENCHMARK 1000 surface ants: %.2f ms per tick".format(java.util.Locale.ROOT, msPerTick))
        assertTrue(msPerTick < 20.0, "$msPerTick ms per tick")
    }
}
```

- [ ] **Step 3: Run the Gruter, determinism and benchmark tests**

Run (background, long timeout): `JAVA_HOME="C:/Program Files/Eclipse Adoptium/jdk-17.0.10.7-hotspot" ./gradlew :sim:test --tests "*GruterScenarioTest" --tests "*SurfaceRunTest" -i`
Expected: BUILD SUCCESSFUL, and a `BENCHMARK` line in the output. Record the benchmark number. If all pass, skip Step 4.

- [ ] **Step 4: Recalibrate if the Gruter test fails**

Follow M1's rules exactly: adjust only `AntParams` defaults, one knob at a time, rerun after each change, at most eight attempts, never loosen the test.

| Failing check | Knob, in order | Direction |
|---|---|---|
| `smallColoniesForageAlone` (following too high) | `trailThreshold` | raise by 30% |
| `largeColoniesBreakSymmetry` following too low | `markAmount`, then `trailThreshold` | raise by 50%, then lower by 20% |
| `largeColoniesBreakSymmetry` share too low | `exitChoiceExponent` (up to 4), then `trailDiffusion` | raise by 0.5, then halve |
| `crowdedColoniesSwitchToARicherSource` | `crowdLevel`, then `saturationLevel` | lower to 3, then halve |

The trilinear field spreads each mark over 8 voxels, so expect trail readings lower than M1's; `markAmount` is the first lever for that. Document every changed value and its reason in the `AntParams` KDoc, replacing the M1 notes for that knob. If eight attempts do not pass, stop and report the measured values.

- [ ] **Step 5: Run the whole sim suite and commit**

Run (background): `JAVA_HOME="C:/Program Files/Eclipse Adoptium/jdk-17.0.10.7-hotspot" ./gradlew :sim:test`
Expected: BUILD SUCCESSFUL.

```bash
git add sim/src
git commit -m "M1b-1: the Gruter test on the 3D surface, determinism and a 1,000-ant benchmark"
```

---

### Task 9: Camera math

**Files:**
- Create: `core/src/main/kotlin/com/bydesigninteractive/ant/core/camera/ChaseCamera3.kt`
- Delete: `core/.../camera/ChaseCamera.kt` and `core/src/test/.../camera/ChaseCameraTest.kt`
- Modify: `core/.../camera/ViewCamera.kt`
- Test: `core/src/test/kotlin/com/bydesigninteractive/ant/core/camera/ChaseCamera3Test.kt`, `ViewCameraTest.kt`

The app still references `ChaseCamera` until Task 10; in this task, keep `ChaseCamera.kt` and its test in place and only add `ChaseCamera3`. Task 10 deletes them.

**Interfaces:**
- Produces: `class ChaseCamera3(distance = 70f, height = 35f, lookAhead = 30f, followRate = 2.5f, moveThreshold = 2f, snapDistance = 50f)` with `placed`, `eyeX/eyeY/eyeZ`, `targetX/targetY/targetZ`, `upX/upY/upZ`, `update(px, py, pz, fx, fy, fz, nx, ny, nz, dt)`. `ViewCamera.placeAt(x, y, zoom, screenH)` clamps the zoom.

- [ ] **Step 1: Write the failing tests**

`ChaseCamera3Test.kt`:
```kotlin
package com.bydesigninteractive.ant.core.camera

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ChaseCamera3Test {
    private fun flat(c: ChaseCamera3, x: Float, y: Float, fx: Float, fy: Float) =
        c.update(x, y, 0f, fx, fy, 0f, 0f, 0f, 1f, 0.05f)

    @Test
    fun sitsBehindAndAboveTheAnt() {
        val c = ChaseCamera3()
        flat(c, 100f, 0f, 1f, 0f)
        assertEquals(30f, c.eyeX, 1e-4f)
        assertEquals(35f, c.eyeZ, 1e-4f)
        assertEquals(130f, c.targetX, 1e-4f)
    }

    @Test
    fun turningInPlaceDoesNotSwingTheCamera() {
        val c = ChaseCamera3()
        flat(c, 0f, 0f, 1f, 0f)
        repeat(40) { flat(c, 0f, 0f, 0f, 1f) }
        assertEquals(-70f, c.eyeX, 1e-4f)
        assertEquals(0f, c.eyeY, 1e-4f)
    }

    @Test
    fun movingOffBringsTheCameraBehind() {
        val c = ChaseCamera3()
        flat(c, 0f, 0f, 1f, 0f)
        var y = 0f
        repeat(80) {
            y += 1f
            flat(c, 0f, y, 0f, 1f)
        }
        assertTrue(c.eyeY < y - 65f, "eye y ${c.eyeY}, ant y $y")
    }

    @Test
    fun aTeleportSnapsInsteadOfSwinging() {
        val c = ChaseCamera3()
        flat(c, 0f, 0f, 1f, 0f)
        flat(c, 1000f, 0f, 0f, 1f)
        assertEquals(1000f, c.eyeX, 1e-4f)
        assertEquals(-70f, c.eyeY, 1e-4f)
    }

    @Test
    fun upFollowsTheSurfaceWhenMoving() {
        val c = ChaseCamera3()
        c.update(0f, 0f, 0f, 0f, 0f, 1f, -1f, 0f, 0f, 0.05f) // on a wall facing -x, walking up
        assertEquals(-1f, c.upX, 1e-4f)
        assertEquals(-70f, c.eyeZ, 1e-4f)
    }
}
```
Add to `ViewCameraTest.kt`:
```kotlin
    @Test
    fun placeAtClampsTheZoom() {
        cam.placeAt(0f, 0f, 5f, 1080)
        assertEquals(cam.maxMmPerPx(1080), cam.mmPerPx, 1e-5f)
    }

    @Test
    fun panMovesAlongTheCameraAxes() {
        cam.placeAt(0f, 0f, 0.5f, 1080)
        cam.pan(10f, -4f)
        assertEquals(5f, cam.centerX, 1e-5f)
        assertEquals(-2f, cam.centerY, 1e-5f)
    }
```
and change the existing `zoomStaysWithinLimits` call to `cam.placeAt(0f, 0f, 0.3f, 1080)`.

- [ ] **Step 2: Run to verify they fail**

Run: `JAVA_HOME="C:/Program Files/Eclipse Adoptium/jdk-17.0.10.7-hotspot" ./gradlew :core:test --tests "*Camera*"`
Expected: compilation fails (unresolved `ChaseCamera3`, 4-argument `placeAt`).

- [ ] **Step 3: Implement**

`ChaseCamera3.kt`:
```kotlin
package com.bydesigninteractive.ant.core.camera

import kotlin.math.exp
import kotlin.math.sqrt

/**
 * A third-person chase camera on the 3D surface, in surface millimeters (z up). It keeps a
 * smoothed forward and up; an ant turning in place does not swing it, and once the ant moves off
 * both ease toward the ant's own forward and normal with lag. The eye sits [distance] behind
 * along the forward and [height] above along the up, looking at a point [lookAhead] ahead.
 * A jump of more than [snapDistance] in one update (an entrance handoff) snaps instead of easing.
 */
class ChaseCamera3(
    val distance: Float = 70f,
    val height: Float = 35f,
    val lookAhead: Float = 30f,
    private val followRate: Float = 2.5f,
    private val moveThreshold: Float = 2f,
    private val snapDistance: Float = 50f,
) {
    private var px = 0f
    private var py = 0f
    private var pz = 0f
    private var fx = 1f
    private var fy = 0f
    private var fz = 0f

    var upX = 0f
        private set
    var upY = 0f
        private set
    var upZ = 1f
        private set
    var placed = false
        private set

    val eyeX: Float get() = px - fx * distance + upX * height
    val eyeY: Float get() = py - fy * distance + upY * height
    val eyeZ: Float get() = pz - fz * distance + upZ * height
    val targetX: Float get() = px + fx * lookAhead
    val targetY: Float get() = py + fy * lookAhead
    val targetZ: Float get() = pz + fz * lookAhead

    fun update(ax: Float, ay: Float, az: Float, afx: Float, afy: Float, afz: Float, anx: Float, any: Float, anz: Float, dt: Float) {
        val dx = ax - px
        val dy = ay - py
        val dz = az - pz
        val jump = sqrt(dx * dx + dy * dy + dz * dz)
        if (!placed || jump > snapDistance) {
            fx = afx
            fy = afy
            fz = afz
            upX = anx
            upY = any
            upZ = anz
            placed = true
        } else if (dt > 0f && jump / dt > moveThreshold) {
            val k = 1f - exp(-followRate * dt)
            fx += (afx - fx) * k
            fy += (afy - fy) * k
            fz += (afz - fz) * k
            upX += (anx - upX) * k
            upY += (any - upY) * k
            upZ += (anz - upZ) * k
        }
        px = ax
        py = ay
        pz = az
        orthonormalize()
    }

    /** Keeps up a unit vector and forward a unit vector perpendicular to it. */
    private fun orthonormalize() {
        var len = sqrt(upX * upX + upY * upY + upZ * upZ)
        if (len > 1e-6f) {
            upX /= len
            upY /= len
            upZ /= len
        }
        val d = fx * upX + fy * upY + fz * upZ
        val gx = fx - upX * d
        val gy = fy - upY * d
        val gz = fz - upZ * d
        len = sqrt(gx * gx + gy * gy + gz * gz)
        if (len > 1e-6f) {
            fx = gx / len
            fy = gy / len
            fz = gz / len
        }
    }
}
```

`ViewCamera.kt`:
- Replace `placeAt` with
```kotlin
    fun placeAt(x: Float, y: Float, zoom: Float, screenH: Int) {
        centerX = x
        centerY = y
        mmPerPx = zoom.coerceIn(minMmPerPx(screenH), maxMmPerPx(screenH))
        placed = true
    }
```
- In `ease`, the first placement becomes `placeAt(x, y, zoom, screenH)`; pass `screenH` through `ease(x, y, zoom, screenH, dt)` from `frame` and `follow`.
- `pan` KDoc: `/** Moves the center by screen pixels: dxPx along the camera's x axis, dyPx along its y axis (y up on the surface map, depth downward in the nest). */`
- In `AntApp.kt`, update the one call to `topCam.placeAt(world.surface.entranceX, world.surface.entranceY, 0.4f, Gdx.graphics.height)`.

- [ ] **Step 4: Run to verify they pass**

Run: `JAVA_HOME="C:/Program Files/Eclipse Adoptium/jdk-17.0.10.7-hotspot" ./gradlew :core:test :desktop:classes`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 5: Commit**

```bash
git add core/src
git commit -m "M1b-1: a 3D chase camera, and view camera clamping"
```

---

### Task 10: The debug 3D view and the app

**Files:**
- Create: `core/src/main/kotlin/com/bydesigninteractive/ant/core/render/DebugSurfaceRenderer.kt`
- Delete: `core/.../render/Surface3dRenderer.kt`, `core/.../camera/ChaseCamera.kt`, `core/src/test/.../camera/ChaseCameraTest.kt`
- Modify: `core/.../AntApp.kt`, `core/.../render/ChunkTextures.kt`, `core/.../render/Sprites.kt`

**Interfaces:**
- Consumes: `ChaseCamera3` (Task 9), `SurfaceSdf.ownBlobs`/`placed`, `FoodSource.body`/`stem`, `HeightField.height`/`slope`, ant pose fields.
- Produces: `class DebugSurfaceRenderer(world: World) : Disposable` with `resize(w, h)` and `draw(chase: ChaseCamera3)`.

Surface-to-GL mapping: surface (x, y, z) is GL (x, z, -y), with GL y up.

- [ ] **Step 1: Write the renderer**

```kotlin
package com.bydesigninteractive.ant.core.render

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.PerspectiveCamera
import com.badlogic.gdx.graphics.VertexAttributes
import com.badlogic.gdx.graphics.g3d.Environment
import com.badlogic.gdx.graphics.g3d.Material
import com.badlogic.gdx.graphics.g3d.Model
import com.badlogic.gdx.graphics.g3d.ModelBatch
import com.badlogic.gdx.graphics.g3d.ModelInstance
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight
import com.badlogic.gdx.graphics.g3d.utils.MeshPartBuilder
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.utils.Disposable
import com.bydesigninteractive.ant.core.camera.ChaseCamera3
import com.bydesigninteractive.ant.sim.World
import com.bydesigninteractive.ant.sim.ant.Space
import com.bydesigninteractive.ant.sim.world.CHUNKS
import com.bydesigninteractive.ant.sim.world.CHUNK_MM
import kotlin.math.abs
import kotlin.math.floor

/**
 * A plain 3D view of the surface for checking the simulation: the ground as a grid mesh around
 * the camera, rocks and food lumps as spheres, stems as cylinders, ants as small boxes tilted to
 * their normals. M1b-2 replaces it with the low-poly look.
 */
class DebugSurfaceRenderer(private val world: World) : Disposable {
    val camera = PerspectiveCamera(FOV, 1f, 1f).apply {
        near = 2f
        far = VIEW_MM * 2f
    }
    private val batch = ModelBatch()
    private val environment = Environment().apply {
        set(ColorAttribute(ColorAttribute.AmbientLight, 0.45f, 0.45f, 0.45f, 1f))
        add(DirectionalLight().set(0.75f, 0.72f, 0.65f, -0.35f, -1f, -0.25f))
    }
    private val builder = ModelBuilder()
    private val attributes = (VertexAttributes.Usage.Position or VertexAttributes.Usage.Normal).toLong()
    private val sphere = builder.createSphere(2f, 2f, 2f, 16, 12, Material(), attributes)
    private val cylinder = builder.createCylinder(2f, 1f, 2f, 10, Material(), attributes)
    private val box = builder.createBox(1f, 1f, 1f, Material(), attributes)
    private val pools = HashMap<Model, ArrayList<ModelInstance>>()
    private val used = HashMap<Model, Int>()
    private var ground: Model? = null
    private var groundInstance: ModelInstance? = null
    private var groundX = Float.NaN
    private var groundY = Float.NaN
    private val xAxis = Vector3()
    private val yAxis = Vector3()
    private val zAxis = Vector3()
    private val origin = Vector3()
    private val slope = FloatArray(2)

    fun resize(w: Int, h: Int) {
        camera.viewportWidth = w.toFloat()
        camera.viewportHeight = h.toFloat()
    }

    fun draw(chase: ChaseCamera3) {
        camera.position.set(chase.eyeX, chase.eyeZ, -chase.eyeY)
        camera.up.set(chase.upX, chase.upZ, -chase.upY)
        camera.lookAt(chase.targetX, chase.targetZ, -chase.targetY)
        camera.update()
        val fx = chase.targetX
        val fy = chase.targetY
        if (groundX.isNaN() || abs(fx - groundX) > REBUILD_MM || abs(fy - groundY) > REBUILD_MM) rebuildGround(fx, fy)
        used.clear()
        batch.begin(camera)
        groundInstance?.let { batch.render(it, environment) }
        val sdf = world.surface.sdf
        val ccx = floor(fx / CHUNK_MM).toInt()
        val ccy = floor(fy / CHUNK_MM).toInt()
        for (cy in ccy - RING..ccy + RING) for (cx in ccx - RING..ccx + RING) {
            if (cx < 0 || cy < 0 || cx >= CHUNKS || cy >= CHUNKS) continue
            for (b in sdf.ownBlobs(cx, cy)) blob(b.cx, b.cy, b.cz, b.rx, b.ry, b.rz, ROCK)
        }
        for (b in sdf.placed) blob(b.cx, b.cy, b.cz, b.rx, b.ry, b.rz, ROCK)
        for (f in world.surface.foods) {
            f.body?.let { blob(it.cx, it.cy, it.cz, it.rx, it.ry, it.rz, FOOD) }
            f.stem?.let {
                val i = next(cylinder, STEM)
                i.transform.setToTranslationAndScaling(it.x, (it.z0 + it.z1) / 2f, -it.y, it.radius, it.z1 - it.z0, it.radius)
                batch.render(i, environment)
            }
        }
        for (a in world.ants) {
            if (a.space != Space.SURFACE) continue
            if (abs(a.x - fx) > VIEW_MM || abs(a.y - fy) > VIEW_MM) continue
            val i = next(box, ANT)
            xAxis.set(a.fx, a.fz, -a.fy)
            yAxis.set(a.nx, a.nz, -a.ny)
            zAxis.set(xAxis).crs(yAxis)
            origin.set(a.x + a.nx * 0.75f, a.z + a.nz * 0.75f, -(a.y + a.ny * 0.75f))
            i.transform.set(xAxis, yAxis, zAxis, origin)
            i.transform.scale(5f, 1.5f, 2.5f)
            batch.render(i, environment)
        }
        batch.end()
    }

    private fun blob(x: Float, y: Float, z: Float, rx: Float, ry: Float, rz: Float, color: Color) {
        val i = next(sphere, color)
        i.transform.setToTranslationAndScaling(x, z, -y, rx, rz, ry)
        batch.render(i, environment)
    }

    private fun next(model: Model, color: Color): ModelInstance {
        val pool = pools.getOrPut(model) { ArrayList() }
        val n = used[model] ?: 0
        val instance = if (n < pool.size) pool[n] else ModelInstance(model).also { pool += it }
        used[model] = n + 1
        instance.materials[0].set(ColorAttribute.createDiffuse(color))
        return instance
    }

    /** A grid of the ground heights, 2 m across at 20 mm spacing, centered on (cx, cy). */
    private fun rebuildGround(cx: Float, cy: Float) {
        ground?.dispose()
        val g = world.surface.ground
        builder.begin()
        val part = builder.part("ground", GL20.GL_TRIANGLES, attributes, Material(ColorAttribute.createDiffuse(GROUND)))
        val n = GROUND_CELLS + 1
        val x0 = cx - GROUND_CELLS * SPACING / 2f
        val y0 = cy - GROUND_CELLS * SPACING / 2f
        val v = MeshPartBuilder.VertexInfo()
        for (j in 0 until n) for (i in 0 until n) {
            val x = x0 + i * SPACING
            val y = y0 + j * SPACING
            g.slope(x, y, slope)
            v.setPos(x, g.height(x, y), -y)
            v.setNor(-slope[0], 1f, slope[1])
            v.normal.nor()
            part.vertex(v)
        }
        for (j in 0 until GROUND_CELLS) for (i in 0 until GROUND_CELLS) {
            val i00 = (j * n + i).toShort()
            val i10 = (j * n + i + 1).toShort()
            val i01 = ((j + 1) * n + i).toShort()
            val i11 = ((j + 1) * n + i + 1).toShort()
            part.triangle(i00, i10, i11)
            part.triangle(i00, i11, i01)
        }
        val model = builder.end()
        ground = model
        groundInstance = ModelInstance(model)
        groundX = cx
        groundY = cy
    }

    override fun dispose() {
        ground?.dispose()
        sphere.dispose()
        cylinder.dispose()
        box.dispose()
        batch.dispose()
    }

    private companion object {
        const val FOV = 60f
        const val VIEW_MM = 1500f
        const val RING = 2
        const val GROUND_CELLS = 100
        const val SPACING = 20f
        const val REBUILD_MM = 400f
        val GROUND = Color(0.40f, 0.30f, 0.20f, 1f)
        val ROCK = Color(0.52f, 0.51f, 0.48f, 1f)
        val FOOD = Color(0.80f, 0.55f, 0.20f, 1f)
        val STEM = Color(0.30f, 0.50f, 0.18f, 1f)
        val ANT = Color(0.08f, 0.06f, 0.05f, 1f)
    }
}
```

- [ ] **Step 2: Rewire `AntApp` and remove the 2.5D pieces**

In `AntApp.kt`:
- Replace `ChaseCamera` with `ChaseCamera3` (`private val chase = ChaseCamera3()`), and `Surface3dRenderer` with `DebugSurfaceRenderer` (`renderer3d = DebugSurfaceRenderer(world)`); remove `tuftTexture` and `plantTexture` and their creation and disposal.
- Rename the view title of `SURFACE_3D` to `"surface, 3D debug"`.
- `draw3d`:
```kotlin
    private fun draw3d(dt: Float) {
        val a = followed
        val s = world.surface
        when {
            a != null && a.space == Space.SURFACE -> chase.update(a.x, a.y, a.z, a.fx, a.fy, a.fz, a.nx, a.ny, a.nz, dt)
            !chase.placed -> chase.update(
                s.entranceX, s.entranceY, s.ground.height(s.entranceX, s.entranceY),
                1f, 0f, 0f, 0f, 0f, 1f, dt,
            )
        }
        renderer3d.draw(chase)
    }
```
- Frame budget (M1 review): replace `advance` and remove `MAX_STEPS`:
```kotlin
    private fun advance(dt: Float) {
        if (paused) return
        accumulator += dt * SPEEDS[speedIndex]
        val start = System.nanoTime()
        while (accumulator >= DT) {
            world.step()
            accumulator -= DT
            if (System.nanoTime() - start > STEP_BUDGET_NS) {
                accumulator = 0f // behind: slow the simulation down, not the frame rate
                break
            }
        }
    }
```
with `const val STEP_BUDGET_NS = 6_000_000L` in the companion.
- HUD throttling (M1 review): keep `private var hudLines: List<String> = emptyList()` and `private var hudAge = HUD_EVERY_S`; in `drawHud(dt)` add `hudAge += dt` and recompute `hudLines` from `DebugReadout.lines(...)` only when `hudAge >= HUD_EVERY_S` (then reset to 0); draw `hudLines`. `const val HUD_EVERY_S = 0.25f`. Pass `dt` from `render`.
- In `drawTop`, keep calling `chunkTextures.evict(...)`; the 3D debug view does not use chunk textures.

Delete `Surface3dRenderer.kt`, `ChaseCamera.kt` and `ChaseCameraTest.kt`. In `Sprites.kt`, delete `tuft()` and `plant()` (no longer used) and add to the object KDoc: "Each call returns a new texture the caller owns and must dispose."

`ChunkTextures.kt`: draw large rocks too, after the stones:
```kotlin
        p.setColor(0.40f, 0.39f, 0.37f, 1f)
        for (s in c.rocks) p.fillCircle(col(s.x, x0, mm), row(s.y, y0, mm), (s.radius / mm).toInt())
```

- [ ] **Step 3: Build and run on desktop**

Run (background): `JAVA_HOME="C:/Program Files/Eclipse Adoptium/jdk-17.0.10.7-hotspot" ./gradlew :core:test :desktop:classes`
Expected: BUILD SUCCESSFUL. The visual check (Tab to the 3D debug view: brown ground mesh with relief, gray rock spheres, green stems, dark ant boxes walking and tilting over rocks) is done by the controller.

- [ ] **Step 4: Commit**

```bash
git add -A core/src
git commit -m "M1b-1: the 3D debug view, a frame time budget and a throttled HUD"
```

---

### Task 11: Close out M1b-1

**Files:**
- Modify: `CLAUDE.md` (Status), `README.md` (views and keys), `docs/design/poc-design.md` (M1b row: note the split)

- [ ] **Step 1: Update the docs**

- `CLAUDE.md` Status: M1b-1 done (date), what `sim` now holds (SDF world, `Field3`, `SurfaceWalk`, plants), the benchmark number from Task 8, any recalibrated values, and "Next is M1b-2: the low-poly look and the TV test".
- `README.md`: the third view is now "Surface, 3D debug" (ground mesh, rocks, stems and ants as simple shapes; the low-poly look arrives in M1b-2).
- `docs/design/poc-design.md` section 12: split the M1b row into M1b-1 (simulation, done) and M1b-2 (looks and TV).

- [ ] **Step 2: Run everything**

Run (background): `JAVA_HOME="C:/Program Files/Eclipse Adoptium/jdk-17.0.10.7-hotspot" ./gradlew :sim:test :core:test :desktop:classes :android:assembleDebug`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**

```bash
git add CLAUDE.md README.md docs
git commit -m "M1b-1: status and notes"
```

---

## Notes for the implementer

- **Performance.** A surface step is about 20 to 35 SDF evaluations. Each evaluation is the ground (a cached grid read plus the spoil lookup, five heights for the slope) and the few shapes near the point. The Task 8 benchmark is the number to watch; M1b-2 decides the TV budget from it.
- **Stem climbing is scripted.** The odour cue and climb-on-touch rule are stand-ins; M2's brains replace them.
- **Spoil height** is small per pellet (0.01 mm); a visible mound needs thousands of pellets, which is realistic.

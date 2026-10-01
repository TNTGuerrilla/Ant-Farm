# M1b-2c: the Low-Poly Look and the Final TV Test Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** The 3D surface view gets the approved low-poly look (faceted ground with specks, smooth rocks fitted to the simulation surface, faceted food, patchy instanced grass, an instanced fused ant with a shader walk cycle, a day cycle), and the TV holds 60 fps.

**Architecture:** Pure-Kotlin geometry builders (`ChunkMesher`, `GrassField`, `AntMesh`, `DayCycle`) produce plain float arrays and are unit-tested without GL. A background `ChunkCache` thread runs the mesher on immutable published data; the render thread uploads and draws with a few small GLSL shaders through libGDX `Mesh` and `ShaderProgram`, using GL 3 instancing for ants, shadows and grass. The new `SurfaceRenderer3D` replaces `DebugSurfaceRenderer`, which stays as the GL 2 fallback.

**Tech Stack:** Kotlin, libGDX 1.14.2 (`Mesh`, `ShaderProgram`, `PerspectiveCamera`, GL 3 instancing), LWJGL3 desktop backend, Android GLES 3, JDK 17, kotlin.test.

Spec: `docs/superpowers/specs/2026-10-01-m1b2c-lowpoly-look-design.md`.

## Global Constraints

- Never use em-dashes (U+2014) or emojis anywhere: code, comments, docs, commit messages. Do not substitute en-dashes or spaced hyphens in prose; rewrite the sentence.
- Commit messages carry NO Claude attribution: no `Co-Authored-By` line, no "Generated with" line, even if your environment suggests adding one. Use exactly the commit message the task gives. Do not change git config.
- Gradle needs JDK 17. Prefix every Gradle command with `JAVA_HOME="C:/Program Files/Eclipse Adoptium/jdk-17.0.10.7-hotspot"` (the machine default is JDK 1.8). Run from the repo root `J:/Documents/Development/Programming/Kotlin/Ant` with `./gradlew` in the Bash tool. The full `:sim:test` includes the Gruter test and benchmarks (several minutes); use a 10-minute timeout. Run long builds as background tasks and check the result before continuing.
- `sim` must not depend on libGDX or Android. Pure geometry classes in `core` (`ChunkMesher`, `GrassField`, `AntMesh`, `DayCycle`, `SkyState`, `MeshBuilder`) must not call `Gdx` or GL, so they unit-test on the JVM.
- After M1b-2a, `core` never reads the live `World` while the simulation thread runs. Renderers and the mesher thread read only `Published` stores, `AntStates`, and immutable values from before the thread starts (seed).
- Changes to `sim` in this milestone must not change simulation results: `FingerprintTest` must pass with its golden values unchanged.
- World coordinates are the simulation's: millimetres, x east, y north, z up. The 3D camera works directly in these coordinates (no axis swap).
- Palette (spec 2.1): soil about RGB 139/98/62 with per-facet variation of plus or minus 10 to 15%; stone about 128/124/116; grass about 96/132/70; ants about 70/40/26. Flat shading (per-face normals) for ground, specks, ants, stems, aphids, prey and grass; smooth shading for rocks. Colour per vertex; no textures.
- One simulated day lasts 600 real seconds at 1x; the first frame is mid-morning; night overall brightness is at least 35% of midday.
- Shaders are GLSL in the legacy style (`attribute`, `varying`, `gl_FragColor`, no `#version` line, `#ifdef GL_ES precision mediump float; #endif`), so libGDX's GL 3 compatibility prepend on desktop and GLES 3 on Android both accept them.
- Package root `com.bydesigninteractive.ant`. KDoc on public classes and non-obvious functions; match the existing style.
- Work on branch `m1b2c-lowpoly`. Do not push, do not switch branches, do not rewrite history. Read each file before editing it.
- Visual checks on the desktop and the TV are run by the controller with the owner, not by a subagent. Subagents may launch `:desktop:run` only to check the log for errors, and must kill only that run's JVM (command line containing `DesktopLauncherKt`).
- adb: never run adb without `-s <serial>`; subagents use only `-s emulator-5554` and never touch the TV.

---

### Task 1: Published data for the look (crop, carry, rocks around the focus)

**Files:**
- Modify: `sim/src/main/kotlin/com/bydesigninteractive/ant/sim/ant/Ant.kt` (`lastFoodKind`)
- Modify: `sim/src/main/kotlin/com/bydesigninteractive/ant/sim/ant/Forager.kt` (set it when feeding)
- Modify: `core/src/main/kotlin/com/bydesigninteractive/ant/core/engine/Snapshot.kt` (`crop`, `carry`)
- Modify: `core/src/main/kotlin/com/bydesigninteractive/ant/core/engine/AntStates.kt` (`AntPose.crop`, `AntPose.carry`)
- Modify: `core/src/main/kotlin/com/bydesigninteractive/ant/core/engine/Published.kt` (generate rocks around the focus)
- Test: `core/src/test/kotlin/com/bydesigninteractive/ant/core/engine/LookDataTest.kt` (create)

**Interfaces:**
- Produces: `Ant.lastFoodKind: FoodKind?`; `Snapshot.crop: FloatArray` (0 to 1), `Snapshot.carry: ByteArray` (`CARRY_NONE` 0, `CARRY_PELLET` 1, `CARRY_PREY` 2, top-level consts in `Snapshot.kt`); `AntPose.crop: Float`, `AntPose.carry: Int`; `Published` generates (and so publishes) rocks for every chunk in its focus ring.

- [ ] **Step 1: Write the failing tests**

```kotlin
package com.bydesigninteractive.ant.core.engine

import com.bydesigninteractive.ant.sim.ant.Role
import com.bydesigninteractive.ant.sim.scenario.Scenarios
import com.bydesigninteractive.ant.sim.world.CHUNKS
import com.bydesigninteractive.ant.sim.world.CHUNK_MM
import com.bydesigninteractive.ant.sim.world.FoodKind
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class LookDataTest {
    @Test
    fun theSnapshotCarriesCropAndCarry() {
        val w = Scenarios.starter(3)
        val digger = w.ants.first { it.role == Role.DIGGER }
        digger.carriesPellet = true
        val forager = w.ants.first { it.role == Role.FORAGER }
        forager.crop = forager.desiredCrop / 2f
        forager.lastFoodKind = FoodKind.HONEYDEW
        val hunter = w.ants.last { it.role == Role.FORAGER }
        hunter.crop = 0.5f
        hunter.lastFoodKind = FoodKind.PREY
        val s = Snapshot()
        s.fill(w, 0L, emptyList())
        assertEquals(CARRY_PELLET, s.carry[digger.id].toInt())
        assertEquals(0.5f, s.crop[forager.id], 1e-6f)
        assertEquals(CARRY_NONE, s.carry[forager.id].toInt())
        assertEquals(CARRY_PREY, s.carry[hunter.id].toInt())
        assertEquals(0f, s.crop[hunter.id])
    }

    @Test
    fun posesBlendCropAndTakeCarryFromTheCurrentTick() {
        val w = Scenarios.starter(3)
        val a = w.ants.first { it.role == Role.FORAGER }
        val states = AntStates()
        val s = Snapshot()
        s.fill(w, 0L, emptyList())
        states.accept(s)
        a.crop = a.desiredCrop
        a.lastFoodKind = FoodKind.HONEYDEW
        s.fill(w, 1L, emptyList())
        states.accept(s)
        val pose = AntPose()
        states.blend(a.id, 0.5f, pose)
        assertEquals(0.5f, pose.crop, 1e-6f)
        assertEquals(CARRY_NONE, pose.carry)
    }

    @Test
    fun rocksAroundTheFocusArePublished() {
        val w = Scenarios.starter(3)
        val p = Published(w)
        p.setFocus(w.surface.entranceX + 900f, w.surface.entranceY)
        repeat(20) {
            w.step()
            p.publish(w, it.toLong(), TickStats(), TickSchedule(intervalFor(1f)))
        }
        val cx = ((w.surface.entranceX + 900f) / CHUNK_MM).toInt()
        val cy = (w.surface.entranceY / CHUNK_MM).toInt()
        for (dy in -2..2) for (dx in -2..2) assertNotNull(p.rocks[(cx + dx) + (cy + dy) * CHUNKS], "chunk ${cx + dx}, ${cy + dy}")
        assertTrue(p.rocks.size >= 25)
    }
}
```
Read `Published.kt` first: if `setFocus` has a different signature, or the focus is applied on publish, adapt the test call to the real API and note it. Run: `./gradlew :core:test --tests "*LookDataTest*"`. Expected: compile failure.

- [ ] **Step 2: The ant remembers what it fed on (write-only for the renderer)**

In `Ant.kt`, after `var food: FoodSource? = null` add:
```kotlin
    /** The kind of the last food this ant fed at; written for the renderer, never read by the simulation. */
    var lastFoodKind: FoodKind? = null
```
(import `com.bydesigninteractive.ant.sim.world.FoodKind` if `FoodSource` is imported from there). In `Forager.feed`, where `a.crop = food.quality` is set (around line 206), add `a.lastFoodKind = food.kind` on the next line.

- [ ] **Step 3: Snapshot fields**

In `Snapshot.kt`, add top-level constants above the class:
```kotlin
/** What an ant visibly carries in its mandibles. */
const val CARRY_NONE = 0
const val CARRY_PELLET = 1
const val CARRY_PREY = 2
```
Add `var crop = FloatArray(0)` and `var carry = ByteArray(0)` beside the other arrays, grow them in `ensure` (`crop = crop.copyOf(c)`, `carry = carry.copyOf(c)`), and in `fill`'s loop:
```kotlin
            val prey = a.lastFoodKind == FoodKind.PREY && a.crop > 0f
            carry[i] = when {
                a.carriesPellet -> CARRY_PELLET.toByte()
                prey -> CARRY_PREY.toByte()
                else -> CARRY_NONE.toByte()
            }
            crop[i] = if (prey || a.desiredCrop <= 0f) 0f else (a.crop / a.desiredCrop).coerceIn(0f, 1f)
```

- [ ] **Step 4: Poses**

In `AntStates.kt`: add `var crop = 0f` and `var carry = CARRY_NONE` to `AntPose`. In the private `Arrays` class add `var crop = FloatArray(0)` and `var carry = ByteArray(0)`, grown in its `ensure`. In `accept`, copy `cur.crop[i] = s.crop[i]` and `cur.carry[i] = s.carry[i]`. In `blend`, set `out.carry = cur.carry[i].toInt()`, and `out.crop = cur.crop[i]` in the unblended branch, `out.crop = lerp(prev.crop[i], cur.crop[i], alpha)` in the blended branch.

- [ ] **Step 5: Rocks around the focus**

In `Published`, where the overlay for the chunks around the focus is computed (the loop over the ring in `publishOverlay`), call `w.surface.sdf.ownBlobs(cx, cy)` for every chunk in the ring before reading anything else. Generation is a pure function of the seed and the foods, and `onGenerated` already publishes into `rocks`, so this changes no simulation result (the M1b-1 test `renderStyleReadsDoNotChangeTheRun` relies on the same fact). Add a one-line comment saying so.

- [ ] **Step 6: Run the tests**

Run: `./gradlew :core:test --tests "*LookDataTest*"` then `./gradlew :sim:test :core:test` (10-minute timeout, background). Expected: all pass, `FingerprintTest` unchanged.

- [ ] **Step 7: Commit**

```bash
git add sim/src core/src
git commit -m "M1b-2c: crop, carry and rocks around the focus for the renderer"
```

---

### Task 2: Sky state and the day cycle

**Files:**
- Create: `core/src/main/kotlin/com/bydesigninteractive/ant/core/render/sky/SkyState.kt`
- Create: `core/src/main/kotlin/com/bydesigninteractive/ant/core/render/sky/DayCycle.kt`
- Test: `core/src/test/kotlin/com/bydesigninteractive/ant/core/render/sky/DayCycleTest.kt`

**Interfaces:**
- Produces: `class SkyState` with `FloatArray(3)` fields `sunDir` (the direction light travels, unit), `sunColor`, `ambient`, `skyTop`, `skyHorizon`, `fogColor`, and `var fogStart: Float`, `var fogEnd: Float` (mm); `fun brightness(): Float`; `fun copyFrom(o: SkyState)`.
- Produces: `object DayCycle` with `const val DAY_SECONDS = 600f`, `const val START = 0.38f`, `fun timeOfDay(seconds: Float): Float` (0 to 1), `fun sky(t: Float, out: SkyState): SkyState`.

- [ ] **Step 1: Write the failing tests**

```kotlin
package com.bydesigninteractive.ant.core.render.sky

import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DayCycleTest {
    @Test
    fun theFirstFrameIsMidMorning() {
        assertEquals(DayCycle.START, DayCycle.timeOfDay(0f), 1e-6f)
        assertEquals(DayCycle.START, DayCycle.timeOfDay(DayCycle.DAY_SECONDS), 1e-5f)
    }

    @Test
    fun theCycleHasNoJumps() {
        val a = SkyState()
        val b = SkyState()
        var t = 0f
        DayCycle.sky(0f, a)
        while (t < 1f) {
            t += 0.001f
            DayCycle.sky(t % 1f, b)
            for ((x, y) in listOf(a.sunColor to b.sunColor, a.ambient to b.ambient, a.skyTop to b.skyTop, a.skyHorizon to b.skyHorizon, a.fogColor to b.fogColor, a.sunDir to b.sunDir)) {
                for (k in 0 until 3) assertTrue(abs(x[k] - y[k]) < 0.03f, "jump at t=$t")
            }
            assertTrue(abs(a.fogEnd - b.fogEnd) < 5f, "fog jump at t=$t")
            a.copyFrom(b)
        }
    }

    @Test
    fun nightStaysReadable() {
        val noon = DayCycle.sky(0.5f, SkyState()).brightness()
        val night = DayCycle.sky(0.0f, SkyState()).brightness()
        assertTrue(night >= 0.35f * noon, "night $night vs noon $noon")
        assertTrue(night <= 0.5f * noon, "night should still look like night: $night vs $noon")
    }

    @Test
    fun theSunDirectionStaysUnit() {
        val s = SkyState()
        for (i in 0 until 100) {
            DayCycle.sky(i / 100f, s)
            val len = kotlin.math.sqrt(s.sunDir[0] * s.sunDir[0] + s.sunDir[1] * s.sunDir[1] + s.sunDir[2] * s.sunDir[2])
            assertEquals(1f, len, 1e-4f)
        }
    }
}
```
Run: `./gradlew :core:test --tests "*DayCycleTest*"`. Expected: compile failure.

- [ ] **Step 2: Implement**

`SkyState.kt`:
```kotlin
package com.bydesigninteractive.ant.core.render.sky

import kotlin.math.max

/**
 * Everything the world shader needs about the sky: a directional light (the sun, or the moon at
 * night), ambient light, the sky gradient and distance fog. The day cycle fills one; a weather
 * system can later fill or adjust one without touching the renderers. Colours are linear RGB 0..1.
 */
class SkyState {
    /** The direction the light travels (pointing down for an overhead sun), unit length. */
    val sunDir = floatArrayOf(0f, 0f, -1f)
    val sunColor = FloatArray(3)
    val ambient = FloatArray(3)
    val skyTop = FloatArray(3)
    val skyHorizon = FloatArray(3)
    val fogColor = FloatArray(3)
    var fogStart = 600f
    var fogEnd = 1500f

    /** Overall brightness of flat ground: ambient plus the light's contribution, by luminance. */
    fun brightness(): Float = lum(ambient) + lum(sunColor) * max(0f, -sunDir[2])

    fun copyFrom(o: SkyState) {
        o.sunDir.copyInto(sunDir)
        o.sunColor.copyInto(sunColor)
        o.ambient.copyInto(ambient)
        o.skyTop.copyInto(skyTop)
        o.skyHorizon.copyInto(skyHorizon)
        o.fogColor.copyInto(fogColor)
        fogStart = o.fogStart
        fogEnd = o.fogEnd
    }

    private fun lum(c: FloatArray) = 0.2126f * c[0] + 0.7152f * c[1] + 0.0722f * c[2]
}
```
`DayCycle.kt`:
```kotlin
package com.bydesigninteractive.ant.core.render.sky

import kotlin.math.sqrt

/**
 * The sky through one simulated day: night, dawn, midday, golden afternoon and dusk keyframes,
 * blended linearly and wrapping at midnight. A day is [DAY_SECONDS] of simulation time (the
 * design's 10 real minutes per day at 1x); the run starts at [START], mid-morning. Night keeps a
 * dim blue moonlight and a raised blue ambient so the scene stays readable.
 */
object DayCycle {
    const val DAY_SECONDS = 600f
    const val START = 0.38f

    private class Key(
        val t: Float,
        val sun: FloatArray, val sunColor: FloatArray, val ambient: FloatArray,
        val top: FloatArray, val horizon: FloatArray, val fog: FloatArray,
        val fogStart: Float, val fogEnd: Float,
    )

    private fun v(x: Float, y: Float, z: Float) = floatArrayOf(x, y, z)

    private val keys = arrayOf(
        Key(0.00f, v(0.3f, 0.2f, -0.93f), v(0.25f, 0.30f, 0.45f), v(0.26f, 0.30f, 0.44f), v(0.05f, 0.08f, 0.18f), v(0.12f, 0.16f, 0.28f), v(0.12f, 0.15f, 0.25f), 400f, 1300f),
        Key(0.25f, v(-0.9f, 0.1f, -0.3f), v(1.00f, 0.70f, 0.50f), v(0.40f, 0.35f, 0.35f), v(0.45f, 0.55f, 0.75f), v(0.98f, 0.72f, 0.55f), v(0.85f, 0.70f, 0.60f), 600f, 1500f),
        Key(0.50f, v(-0.25f, 0.15f, -0.95f), v(1.00f, 0.98f, 0.92f), v(0.50f, 0.50f, 0.52f), v(0.60f, 0.75f, 0.88f), v(0.85f, 0.88f, 0.85f), v(0.82f, 0.84f, 0.82f), 700f, 1600f),
        Key(0.68f, v(0.8f, -0.2f, -0.5f), v(1.00f, 0.80f, 0.55f), v(0.45f, 0.40f, 0.36f), v(0.50f, 0.60f, 0.80f), v(0.96f, 0.82f, 0.60f), v(0.90f, 0.80f, 0.65f), 650f, 1550f),
        Key(0.78f, v(0.95f, -0.1f, -0.15f), v(0.90f, 0.50f, 0.40f), v(0.32f, 0.28f, 0.35f), v(0.25f, 0.28f, 0.50f), v(0.85f, 0.50f, 0.45f), v(0.55f, 0.42f, 0.45f), 550f, 1450f),
    )

    /** The time of day, 0 to 1 (0 midnight, 0.5 noon), for [seconds] of simulation. */
    fun timeOfDay(seconds: Float): Float {
        val t = (seconds / DAY_SECONDS + START) % 1f
        return if (t < 0f) t + 1f else t
    }

    /** Fills [out] with the sky at time of day [t] and returns it. */
    fun sky(t: Float, out: SkyState): SkyState {
        var i = keys.size - 1
        for (k in keys.indices) if (keys[k].t <= t) i = k
        val a = keys[i]
        val b = keys[(i + 1) % keys.size]
        val span = (if (b.t > a.t) b.t else b.t + 1f) - a.t
        val f = ((t - a.t) / span).coerceIn(0f, 1f)
        lerp(a.sun, b.sun, f, out.sunDir)
        val len = sqrt(out.sunDir[0] * out.sunDir[0] + out.sunDir[1] * out.sunDir[1] + out.sunDir[2] * out.sunDir[2])
        for (k in 0 until 3) out.sunDir[k] /= len
        lerp(a.sunColor, b.sunColor, f, out.sunColor)
        lerp(a.ambient, b.ambient, f, out.ambient)
        lerp(a.top, b.top, f, out.skyTop)
        lerp(a.horizon, b.horizon, f, out.skyHorizon)
        lerp(a.fog, b.fog, f, out.fogColor)
        out.fogStart = a.fogStart + (b.fogStart - a.fogStart) * f
        out.fogEnd = a.fogEnd + (b.fogEnd - a.fogEnd) * f
        return out
    }

    private fun lerp(a: FloatArray, b: FloatArray, f: Float, out: FloatArray) {
        for (k in 0 until 3) out[k] = a[k] + (b[k] - a[k]) * f
    }
}
```
If `theCycleHasNoJumps` fails because a keyframe step is too large for a 0.001 step, the tolerance (0.03) is right and the keyframes must not be edited to pass; instead report it (each keyframe pair spans at least 0.1 of the day, so the largest per-step change is about 0.01).

- [ ] **Step 3: Run the tests**

Run: `./gradlew :core:test --tests "*DayCycleTest*"`. Expected: 4 PASS.

- [ ] **Step 4: Commit**

```bash
git add core/src
git commit -m "M1b-2c: a sky state and a day cycle with a readable night"
```

---

### Task 3: The mesh builder and the ground mesher

**Files:**
- Create: `core/src/main/kotlin/com/bydesigninteractive/ant/core/render/world/MeshBuilder.kt`
- Create: `core/src/main/kotlin/com/bydesigninteractive/ant/core/render/world/ChunkMesher.kt`
- Test: `core/src/test/kotlin/com/bydesigninteractive/ant/core/render/world/GroundMeshTest.kt`

**Interfaces:**
- Produces: `class MeshData(val vertices: FloatArray)` with `vertexCount`, and `companion object { const val STRIDE = 9 }` (x, y, z, nx, ny, nz, r, g, b per vertex, world mm; triangles as consecutive vertex triples).
- Produces: `class MeshBuilder` with `fun flat(ax, ay, az, bx, by, bz, cx, cy, cz, r, g, b)` (one triangle, face normal, counter-clockwise seen from the outside), `fun vertex(x, y, z, nx, ny, nz, r, g, b)`, `fun build(): MeshData`, `val vertexCount: Int`.
- Produces: `class ChunkMesher(seed: Long)` with `val heights: HeightField`, `fun setSpoil(cx: Int, cy: Int, values: FloatArray?)`, `fun spoilAt(x: Float, y: Float): Float`, `fun ground(cx: Int, cy: Int): MeshData` (ground plus specks); companion `const val CELLS = 32`, `const val CELL_MM = 15.625f`. Not thread-safe: one mesher per thread.

- [ ] **Step 1: Write the failing tests**

```kotlin
package com.bydesigninteractive.ant.core.render.world

import com.bydesigninteractive.ant.sim.world.CHUNK_MM
import kotlin.math.abs
import kotlin.math.max
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertTrue

class GroundMeshTest {
    private val cx = 8
    private val cy = 8

    @Test
    fun groundVerticesSitOnTheGround() {
        val m = ChunkMesher(4)
        val v = m.ground(cx, cy).vertices
        var ground = 0
        var i = 0
        while (i < v.size) {
            val x = v[i]
            val y = v[i + 1]
            val z = v[i + 2]
            val h = m.heights.height(x, y)
            // ground vertices lie on the height field; speck vertices lie just above it
            assertTrue(z - h >= -0.2f && z - h <= 0.2f, "vertex ($x, $y, $z) is ${z - h} from the ground")
            if (abs(z - h) < 1e-3f) ground++
            i += MeshData.STRIDE
        }
        assertTrue(ground > 1000)
    }

    @Test
    fun flatFacetsStayWithinOneMillimetreOfTheGround() {
        val m = ChunkMesher(4)
        val v = m.ground(cx, cy).vertices
        var worst = 0f
        var i = 0
        while (i < v.size) {
            val ax = v[i]; val ay = v[i + 1]; val az = v[i + 2]
            val bx = v[i + 9]; val by = v[i + 10]; val bz = v[i + 11]
            val qx = v[i + 18]; val qy = v[i + 19]; val qz = v[i + 20]
            for ((u, w) in listOf(0.33f to 0.33f, 0.5f to 0.25f, 0.25f to 0.5f, 0.5f to 0.5f, 0.5f to 0f, 0f to 0.5f)) {
                val x = ax + (bx - ax) * u + (qx - ax) * w
                val y = ay + (by - ay) * u + (qy - ay) * w
                val z = az + (bz - az) * u + (qz - az) * w
                worst = max(worst, abs(z - m.heights.height(x, y)))
            }
            i += 3 * MeshData.STRIDE
        }
        println("GROUND worst facet deviation: $worst mm")
        assertTrue(worst <= 1.2f, "worst facet deviation $worst mm (specks sit 0.05 mm up, so allow them)")
    }

    @Test
    fun neighbouringChunksShareTheirEdge() {
        val m = ChunkMesher(4)
        val left = m.ground(cx, cy).vertices
        val right = m.ground(cx + 1, cy).vertices
        val edge = (cx + 1) * CHUNK_MM.toFloat()
        fun edgePoints(v: FloatArray): Set<Triple<Float, Float, Float>> {
            val out = HashSet<Triple<Float, Float, Float>>()
            var i = 0
            while (i < v.size) {
                if (abs(v[i] - edge) < ChunkMesher.CELL_MM / 3f) out += Triple(v[i], v[i + 1], v[i + 2])
                i += MeshData.STRIDE
            }
            return out
        }
        val a = edgePoints(left)
        val b = edgePoints(right)
        assertTrue(a.isNotEmpty())
        // the shared column of grid vertices is jittered identically from both sides
        assertTrue(a.intersect(b).size >= ChunkMesher.CELLS, "chunks do not meet: ${a.size} vs ${b.size}")
    }

    @Test
    fun theSameSeedGivesTheSameMesh() {
        assertContentEquals(ChunkMesher(4).ground(cx, cy).vertices, ChunkMesher(4).ground(cx, cy).vertices)
    }

    @Test
    fun spoilRaisesTheGround() {
        val m = ChunkMesher(4)
        val before = m.ground(cx, cy).vertices
        val cells = CHUNK_MM / 10
        m.setSpoil(cx, cy, FloatArray(cells * cells) { 50f })
        val after = m.ground(cx, cy).vertices
        fun meanZ(v: FloatArray): Float {
            var sum = 0.0
            var i = 0
            while (i < v.size) { sum += v[i + 2]; i += MeshData.STRIDE }
            return (sum / (v.size / MeshData.STRIDE)).toFloat()
        }
        // 50 pellets per cell raise the ground 0.5 mm; edge vertices also see the empty neighbours
        assertTrue(meanZ(after) > meanZ(before) + 0.35f, "spoil should raise the ground: ${meanZ(before)} -> ${meanZ(after)}")
    }
}
```
Run: `./gradlew :core:test --tests "*GroundMeshTest*"`. Expected: compile failure.

- [ ] **Step 2: `MeshBuilder`**

```kotlin
package com.bydesigninteractive.ant.core.render.world

import kotlin.math.sqrt

/** Triangles for the world shader as interleaved floats: x, y, z, nx, ny, nz, r, g, b per vertex (mm, linear RGB). */
class MeshData(val vertices: FloatArray) {
    val vertexCount: Int get() = vertices.size / STRIDE

    companion object {
        const val STRIDE = 9
    }
}

/** Grows a vertex array triangle by triangle. Not thread-safe. */
class MeshBuilder(initialVertices: Int = 1024) {
    private var v = FloatArray(initialVertices * MeshData.STRIDE)
    private var size = 0

    val vertexCount: Int get() = size / MeshData.STRIDE

    fun vertex(x: Float, y: Float, z: Float, nx: Float, ny: Float, nz: Float, r: Float, g: Float, b: Float) {
        if (size + MeshData.STRIDE > v.size) v = v.copyOf(v.size * 2)
        v[size] = x; v[size + 1] = y; v[size + 2] = z
        v[size + 3] = nx; v[size + 4] = ny; v[size + 5] = nz
        v[size + 6] = r; v[size + 7] = g; v[size + 8] = b
        size += MeshData.STRIDE
    }

    /** One flat-shaded triangle; its normal is the face normal of (a, b, c) taken counter-clockwise. */
    fun flat(ax: Float, ay: Float, az: Float, bx: Float, by: Float, bz: Float, cx: Float, cy: Float, cz: Float, r: Float, g: Float, b: Float) {
        val ux = bx - ax; val uy = by - ay; val uz = bz - az
        val wx = cx - ax; val wy = cy - ay; val wz = cz - az
        var nx = uy * wz - uz * wy
        var ny = uz * wx - ux * wz
        var nz = ux * wy - uy * wx
        val len = sqrt(nx * nx + ny * ny + nz * nz)
        if (len > 1e-12f) { nx /= len; ny /= len; nz /= len } else { nx = 0f; ny = 0f; nz = 1f }
        vertex(ax, ay, az, nx, ny, nz, r, g, b)
        vertex(bx, by, bz, nx, ny, nz, r, g, b)
        vertex(cx, cy, cz, nx, ny, nz, r, g, b)
    }

    fun build(): MeshData = MeshData(v.copyOf(size))
}
```

- [ ] **Step 3: `ChunkMesher` with the ground**

```kotlin
package com.bydesigninteractive.ant.core.render.world

import com.bydesigninteractive.ant.sim.util.hash
import com.bydesigninteractive.ant.sim.world.CHUNK_MM
import com.bydesigninteractive.ant.sim.world.ChunkedField
import com.bydesigninteractive.ant.sim.world.SURFACE_MM
import com.bydesigninteractive.ant.sim.world.sdf.HeightField
import java.util.Random
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.sin

/**
 * Builds the low-poly meshes of one surface chunk from the seed and published data: the ground as
 * jittered flat facets on the height field (spoil included), and baked faceted specks. Rocks and
 * food are added in later tasks. It owns its own [HeightField] and spoil mirror, so it runs on the
 * mesher thread without touching the live world. Not thread-safe.
 */
class ChunkMesher(private val seed: Long) {
    private val spoil = ChunkedField()
    val heights = HeightField(seed, spoil)

    /** Mirrors a published spoil chunk ([values] is immutable and kept without copying). */
    fun setSpoil(cx: Int, cy: Int, values: FloatArray?) = spoil.setChunk(cx, cy, values)

    /** Pellets per spoil cell at (x, y), from the mirror. */
    fun spoilAt(x: Float, y: Float): Float = spoil.get(x, y)

    fun ground(cx: Int, cy: Int): MeshData {
        val b = MeshBuilder(CELLS * CELLS * 6 + 400)
        val n = CELLS + 1
        val xs = FloatArray(n * n)
        val ys = FloatArray(n * n)
        val zs = FloatArray(n * n)
        for (j in 0 until n) for (i in 0 until n) {
            val gi = cx * CELLS + i
            val gj = cy * CELLS + j
            val k = i + j * n
            xs[k] = jittered(gi, gj, 0)
            ys[k] = jittered(gi, gj, 1)
            zs[k] = heights.height(xs[k], ys[k])
        }
        for (j in 0 until CELLS) for (i in 0 until CELLS) {
            val a = i + j * n
            val c = a + 1
            val d = a + n
            val e = d + 1
            val h = hash(seed xor FACET_SALT, cx * CELLS + i, cy * CELLS + j)
            if (h and 1L == 0L) {
                soilTri(b, xs, ys, zs, a, c, e, h ushr 8)
                soilTri(b, xs, ys, zs, a, e, d, h ushr 20)
            } else {
                soilTri(b, xs, ys, zs, a, c, d, h ushr 8)
                soilTri(b, xs, ys, zs, c, e, d, h ushr 20)
            }
        }
        specks(b, cx, cy)
        return b.build()
    }

    /** Coordinate [axis] (0 x, 1 y) of global grid vertex (gi, gj), jittered by a quarter cell, clamped to the map. */
    private fun jittered(gi: Int, gj: Int, axis: Int): Float {
        val base = (if (axis == 0) gi else gj) * CELL_MM
        val h = hash(seed xor JITTER_SALT, gi * 2 + axis, gj)
        val j = (((h ushr 11) and 0xFFFF).toFloat() / 0xFFFF - 0.5f) * 0.5f * CELL_MM
        return (base + j).coerceIn(0f, SURFACE_MM.toFloat())
    }

    private fun soilTri(b: MeshBuilder, xs: FloatArray, ys: FloatArray, zs: FloatArray, p: Int, q: Int, r: Int, h: Long) {
        val f = 0.88f + ((h and 0xFF).toFloat() / 255f) * 0.24f
        b.flat(xs[p], ys[p], zs[p], xs[q], ys[q], zs[q], xs[r], ys[r], zs[r], SOIL_R * f, SOIL_G * f, SOIL_B * f)
    }

    /** A few loose clusters and some strays of small flat flecks, 0.05 mm above the ground. */
    private fun specks(b: MeshBuilder, cx: Int, cy: Int) {
        val r = Random(hash(seed xor SPECK_SALT, cx, cy))
        val x0 = cx * CHUNK_MM.toFloat()
        val y0 = cy * CHUNK_MM.toFloat()
        repeat(CLUSTERS) {
            val px = x0 + r.nextFloat() * CHUNK_MM
            val py = y0 + r.nextFloat() * CHUNK_MM
            repeat(2 + r.nextInt(5)) { fleck(b, r, px + r.nextGaussian().toFloat() * 25f, py + r.nextGaussian().toFloat() * 10f, x0, y0) }
        }
        repeat(STRAYS) { fleck(b, r, x0 + r.nextFloat() * CHUNK_MM, y0 + r.nextFloat() * CHUNK_MM, x0, y0) }
    }

    private fun fleck(b: MeshBuilder, r: Random, x: Float, y: Float, x0: Float, y0: Float) {
        val size = exp(0.5f + r.nextGaussian().toFloat() * 0.5f).coerceIn(0.6f, 5f)
        val sides = intArrayOf(3, 3, 4, 5)[r.nextInt(4)]
        val shade = SPECK_SHADES[r.nextInt(SPECK_SHADES.size)]
        val f = 0.88f + r.nextFloat() * 0.24f
        val a0 = r.nextFloat() * 6.2832f
        val px = FloatArray(sides)
        val py = FloatArray(sides)
        for (k in 0 until sides) {
            val t = a0 + k * 6.2832f / sides + (r.nextFloat() - 0.5f) * 0.8f
            val rr = size * (0.5f + r.nextFloat() * 0.8f)
            px[k] = (x + cos(t) * rr).coerceIn(x0, x0 + CHUNK_MM)
            py[k] = (y + sin(t) * rr).coerceIn(y0, y0 + CHUNK_MM)
        }
        val cz = heights.height(x.coerceIn(x0, x0 + CHUNK_MM), y.coerceIn(y0, y0 + CHUNK_MM)) + SPECK_LIFT
        val ccx = x.coerceIn(x0, x0 + CHUNK_MM)
        val ccy = y.coerceIn(y0, y0 + CHUNK_MM)
        for (k in 0 until sides) {
            val k2 = (k + 1) % sides
            b.flat(
                ccx, ccy, cz,
                px[k], py[k], heights.height(px[k], py[k]) + SPECK_LIFT,
                px[k2], py[k2], heights.height(px[k2], py[k2]) + SPECK_LIFT,
                shade[0] * f, shade[1] * f, shade[2] * f,
            )
        }
    }

    companion object {
        const val CELLS = 32
        const val CELL_MM = 15.625f // CHUNK_MM / CELLS
        const val SPECK_LIFT = 0.05f
        const val CLUSTERS = 4
        const val STRAYS = 8
        const val SOIL_R = 139f / 255f
        const val SOIL_G = 98f / 255f
        const val SOIL_B = 62f / 255f
        private const val FACET_SALT = 0x5011L
        private const val JITTER_SALT = 0x717L
        private const val SPECK_SALT = 0x5BECL
        private val SPECK_SHADES = arrayOf(
            floatArrayOf(96f / 255f, 64f / 255f, 40f / 255f),
            floatArrayOf(116f / 255f, 80f / 255f, 50f / 255f),
            floatArrayOf(150f / 255f, 112f / 255f, 76f / 255f),
            floatArrayOf(120f / 255f, 116f / 255f, 108f / 255f),
        )
    }
}
```
`ChunkedField.setChunk` exists (M1b-2a). If `CELLS` changes (see the facet size rule), change `CELL_MM` to match (`CHUNK_MM / CELLS`).

**Facet size rule.** If `flatFacetsStayWithinOneMillimetreOfTheGround` reports a worst deviation above 1.0 mm on the ground facets, change `CELLS` to 40 (`CELL_MM` 12.5) and rerun; if still above 1.0 mm, report it with the measured value instead of loosening the test. The test allows 1.2 mm because speck vertices sit 0.05 mm up and flecks are flat across bumps; the worst value is printed and goes in the report.

- [ ] **Step 4: Run the tests**

Run: `./gradlew :core:test --tests "*GroundMeshTest*" -i`. Expected: 5 PASS; the `GROUND worst facet deviation` line printed (copy it into the report).

- [ ] **Step 5: Commit**

```bash
git add core/src
git commit -m "M1b-2c: a mesh builder and the faceted ground mesher with specks"
```

---

### Task 4: Rock and food meshes

**Files:**
- Modify: `core/src/main/kotlin/com/bydesigninteractive/ant/core/render/world/ChunkMesher.kt`
- Create: `core/src/main/kotlin/com/bydesigninteractive/ant/core/render/world/Icosphere.kt`
- Test: `core/src/test/kotlin/com/bydesigninteractive/ant/core/render/world/RockFoodMeshTest.kt`

**Interfaces:**
- Consumes: `MeshBuilder`, `MeshData` (Task 3); `FoodView` (`core.engine`); `Blob` (`sim.world.sdf`: `cx`, `cy`, `cz`, `reach`, `distance(x, y, z)`).
- Produces: `ChunkMesher.rocks(blobs: List<Blob>): MeshData` (smooth-shaded) and `ChunkMesher.foods(foods: List<FoodView>): MeshData` (flat-shaded); `object Icosphere` with `fun build(subdivisions: Int): Pair<FloatArray, IntArray>` (unit-sphere vertex xyz triples and triangle index triples, counter-clockwise from outside).

- [ ] **Step 1: Write the failing tests**

```kotlin
package com.bydesigninteractive.ant.core.render.world

import com.bydesigninteractive.ant.core.engine.FoodView
import com.bydesigninteractive.ant.sim.world.FoodKind
import com.bydesigninteractive.ant.sim.world.sdf.Blob
import kotlin.math.abs
import kotlin.math.sqrt
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RockFoodMeshTest {
    private val pebble = Blob(4000f, 4000f, 2f, 4f, 3.5f, 2.5f, 0.12f, 1.3f)
    private val rock = Blob(4100f, 4000f, 8f, 30f, 24f, 18f, 0.15f, 4.1f)

    @Test
    fun rockVerticesLieOnTheirRock() {
        val m = ChunkMesher(4)
        for (b in listOf(pebble, rock)) {
            val v = m.rocks(listOf(b)).vertices
            var i = 0
            while (i < v.size) {
                val d = b.distance(v[i], v[i + 1], v[i + 2])
                assertTrue(abs(d) <= 0.2f, "vertex ${v[i]}, ${v[i + 1]}, ${v[i + 2]} is $d from its rock")
                i += MeshData.STRIDE
            }
        }
    }

    @Test
    fun rocksAreSmoothAndSizedByReach() {
        val m = ChunkMesher(4)
        val small = m.rocks(listOf(pebble))
        val big = m.rocks(listOf(rock))
        assertEquals(80 * 3, small.vertexCount)
        assertEquals(320 * 3, big.vertexCount)
        // smooth: a vertex's normal points away from the rock centre and is unit length
        val v = big.vertices
        val nx = v[3]; val ny = v[4]; val nz = v[5]
        assertEquals(1f, sqrt(nx * nx + ny * ny + nz * nz), 1e-3f)
        assertTrue((v[0] - rock.cx) * nx + (v[1] - rock.cy) * ny + (v[2] - rock.cz) * nz > 0f)
    }

    @Test
    fun foodsGetStemsClustersAndLumps() {
        val m = ChunkMesher(4)
        val plant = FoodView(4000f, 4000f, 500f, 12f, FoodKind.HONEYDEW, 8f, 2.5f, -5f, 530f)
        val prey = FoodView(4200f, 4000f, 3f, 9f, FoodKind.PREY, 5f, 0f, 0f, 0f)
        val both = m.foods(listOf(plant, prey))
        val onlyPrey = m.foods(listOf(prey))
        assertTrue(onlyPrey.vertexCount >= 80 * 3)
        assertTrue(both.vertexCount >= onlyPrey.vertexCount + 12 * 3 + 6 * 8 * 3, "stem prism and aphids missing")
        // the stem reaches from its base to its top
        var minZ = Float.MAX_VALUE
        var maxZ = -Float.MAX_VALUE
        val v = m.foods(listOf(plant)).vertices
        var i = 0
        while (i < v.size) {
            minZ = minOf(minZ, v[i + 2]); maxZ = maxOf(maxZ, v[i + 2]); i += MeshData.STRIDE
        }
        assertTrue(minZ <= -4.9f && maxZ >= 529f, "stem spans $minZ to $maxZ")
    }
}
```
Read `Shapes.kt` for the `Blob` constructor order (cx, cy, cz, rx, ry, rz, lump, phase) and `Snapshot.kt` for `FoodView`'s order; adapt the test constructors if they differ. Run: `./gradlew :core:test --tests "*RockFoodMeshTest*"`. Expected: compile failure.

- [ ] **Step 2: `Icosphere`**

```kotlin
package com.bydesigninteractive.ant.core.render.world

import kotlin.math.sqrt

/** Unit icospheres: 20 triangles subdivided [subdivisions] times (80 at 1, 320 at 2). */
object Icosphere {
    fun build(subdivisions: Int): Pair<FloatArray, IntArray> {
        val t = ((1.0 + sqrt(5.0)) / 2.0).toFloat()
        val verts = ArrayList<FloatArray>()
        fun add(x: Float, y: Float, z: Float): Int {
            val l = sqrt(x * x + y * y + z * z)
            verts += floatArrayOf(x / l, y / l, z / l)
            return verts.size - 1
        }
        for ((x, y, z) in listOf(
            Triple(-1f, t, 0f), Triple(1f, t, 0f), Triple(-1f, -t, 0f), Triple(1f, -t, 0f),
            Triple(0f, -1f, t), Triple(0f, 1f, t), Triple(0f, -1f, -t), Triple(0f, 1f, -t),
            Triple(t, 0f, -1f), Triple(t, 0f, 1f), Triple(-t, 0f, -1f), Triple(-t, 0f, 1f),
        )) add(x, y, z)
        var tris = intArrayOf(
            0, 11, 5, 0, 5, 1, 0, 1, 7, 0, 7, 10, 0, 10, 11,
            1, 5, 9, 5, 11, 4, 11, 10, 2, 10, 7, 6, 7, 1, 8,
            3, 9, 4, 3, 4, 2, 3, 2, 6, 3, 6, 8, 3, 8, 9,
            4, 9, 5, 2, 4, 11, 6, 2, 10, 8, 6, 7, 9, 8, 1,
        )
        repeat(subdivisions) {
            val mid = HashMap<Long, Int>()
            fun midpoint(a: Int, b: Int): Int {
                val key = minOf(a, b).toLong() shl 32 or maxOf(a, b).toLong()
                return mid.getOrPut(key) {
                    val p = verts[a]
                    val q = verts[b]
                    add((p[0] + q[0]) / 2f, (p[1] + q[1]) / 2f, (p[2] + q[2]) / 2f)
                }
            }
            val out = IntArray(tris.size * 4)
            var o = 0
            for (k in tris.indices step 3) {
                val a = tris[k]; val b = tris[k + 1]; val c = tris[k + 2]
                val ab = midpoint(a, b); val bc = midpoint(b, c); val ca = midpoint(c, a)
                for (v in intArrayOf(a, ab, ca, b, bc, ab, c, ca, bc, ab, bc, ca)) out[o++] = v
            }
            tris = out
        }
        val flat = FloatArray(verts.size * 3)
        for (i in verts.indices) verts[i].copyInto(flat, i * 3)
        return flat to tris
    }
}
```
(The base triangles above are the standard icosahedron and are counter-clockwise seen from outside; Step 1's normal-direction test catches a wrong winding only through the smooth normals, so also check one flat face normal points outward while implementing.)

- [ ] **Step 3: Rocks and foods in `ChunkMesher`**

Add to `ChunkMesher` (imports: `Blob`, `FoodView`, `FoodKind`, `kotlin.math.sqrt`):
```kotlin
    private val ico1 = Icosphere.build(1)
    private val ico2 = Icosphere.build(2)

    /** Smooth-shaded rocks: each vertex of an icosphere moved onto the rock's own surface, normals from its gradient. */
    fun rocks(blobs: List<Blob>): MeshData {
        val b = MeshBuilder(blobs.size * 320 * 3 + 16)
        val p = FloatArray(3)
        val n = FloatArray(3)
        for (blob in blobs) {
            val (verts, tris) = if (blob.reach < PEBBLE_REACH) ico1 else ico2
            val pos = FloatArray(verts.size)
            val nor = FloatArray(verts.size)
            for (k in 0 until verts.size / 3) {
                p[0] = blob.cx + verts[k * 3] * blob.reach
                p[1] = blob.cy + verts[k * 3 + 1] * blob.reach
                p[2] = blob.cz + verts[k * 3 + 2] * blob.reach
                projectOnto(blob, p, n)
                p.copyInto(pos, k * 3)
                n.copyInto(nor, k * 3)
            }
            val h = hash(seed xor ROCK_SALT, blob.cx.toInt(), blob.cy.toInt())
            val f = 0.9f + ((h and 0xFF).toFloat() / 255f) * 0.2f
            for (t in tris.indices step 3) for (c in 0 until 3) {
                val k = tris[t + c] * 3
                b.vertex(pos[k], pos[k + 1], pos[k + 2], nor[k], nor[k + 1], nor[k + 2], STONE_R * f, STONE_G * f, STONE_B * f)
            }
        }
        return b.build()
    }

    /** Newton steps along the gradient of [blob]'s distance until [p] is on its surface; the unit gradient goes into [n]. */
    private fun projectOnto(blob: Blob, p: FloatArray, n: FloatArray) {
        repeat(PROJECT_STEPS) {
            val d = blob.distance(p[0], p[1], p[2])
            gradient(blob, p, n)
            p[0] -= n[0] * d
            p[1] -= n[1] * d
            p[2] -= n[2] * d
            if (kotlin.math.abs(d) < 0.005f) return@repeat
        }
        gradient(blob, p, n)
    }

    private fun gradient(blob: Blob, p: FloatArray, n: FloatArray) {
        val e = 0.05f
        val gx = blob.distance(p[0] + e, p[1], p[2]) - blob.distance(p[0] - e, p[1], p[2])
        val gy = blob.distance(p[0], p[1] + e, p[2]) - blob.distance(p[0], p[1] - e, p[2])
        val gz = blob.distance(p[0], p[1], p[2] + e) - blob.distance(p[0], p[1], p[2] - e)
        val l = sqrt(gx * gx + gy * gy + gz * gz).coerceAtLeast(1e-9f)
        n[0] = gx / l; n[1] = gy / l; n[2] = gz / l
    }

    /** Flat-shaded food: six-sided stems with aphid clusters for plants, faceted lumps for prey and feeders. */
    fun foods(foods: List<FoodView>): MeshData {
        val b = MeshBuilder(1024)
        for (f in foods) {
            if (f.stemRadius > 0f) stem(b, f)
            if (f.bodyRadius > 0f) {
                when {
                    f.stemRadius > 0f -> aphidCluster(b, f)
                    f.kind == FoodKind.PREY -> lump(b, f.x, f.y, f.z, f.bodyRadius, PREY_R, PREY_G, PREY_B, 0)
                    else -> lump(b, f.x, f.y, f.z, f.bodyRadius, AMBER_R, AMBER_G, AMBER_B, 0)
                }
            }
        }
        return b.build()
    }

    private fun stem(b: MeshBuilder, f: FoodView) {
        val r = f.stemRadius
        for (k in 0 until 6) {
            val a0 = k * 6.2832f / 6f
            val a1 = (k + 1) * 6.2832f / 6f
            val x0 = f.x + cos(a0) * r; val y0 = f.y + sin(a0) * r
            val x1 = f.x + cos(a1) * r; val y1 = f.y + sin(a1) * r
            val g = 0.85f + 0.15f * (k % 2)
            b.flat(x0, y0, f.stemBase, x1, y1, f.stemBase, x1, y1, f.stemTop, STEM_R * g, STEM_G * g, STEM_B * g)
            b.flat(x0, y0, f.stemBase, x1, y1, f.stemTop, x0, y0, f.stemTop, STEM_R * g, STEM_G * g, STEM_B * g)
        }
    }

    private fun aphidCluster(b: MeshBuilder, f: FoodView) {
        lump(b, f.x, f.y, f.z, f.bodyRadius, STEM_R * 1.2f, STEM_G * 1.2f, STEM_B, 0)
        val r = Random(hash(seed xor APHID_SALT, f.x.toInt(), f.y.toInt()))
        repeat(APHIDS) {
            val u = r.nextFloat() * 2f - 1f
            val a = r.nextFloat() * 6.2832f
            val s = sqrt(1f - u * u)
            val px = f.x + cos(a) * s * f.bodyRadius
            val py = f.y + sin(a) * s * f.bodyRadius
            val pz = f.z + u * f.bodyRadius
            val dark = r.nextBoolean()
            octahedron(b, px, py, pz, 1f + r.nextFloat() * 0.5f, if (dark) 0.12f else 0.45f, if (dark) 0.14f else 0.62f, if (dark) 0.10f else 0.25f)
        }
    }

    private fun lump(b: MeshBuilder, x: Float, y: Float, z: Float, radius: Float, cr: Float, cg: Float, cb: Float, salt: Int) {
        val (verts, tris) = ico1
        val h = hash(seed xor LUMP_SALT, x.toInt() + salt, y.toInt())
        for (t in tris.indices step 3) {
            val i0 = tris[t] * 3; val i1 = tris[t + 1] * 3; val i2 = tris[t + 2] * 3
            val f = 0.85f + (((h ushr (t % 40)) and 0xFF).toFloat() / 255f) * 0.3f
            b.flat(
                x + verts[i0] * radius, y + verts[i0 + 1] * radius, z + verts[i0 + 2] * radius,
                x + verts[i1] * radius, y + verts[i1 + 1] * radius, z + verts[i1 + 2] * radius,
                x + verts[i2] * radius, y + verts[i2 + 1] * radius, z + verts[i2 + 2] * radius,
                cr * f, cg * f, cb * f,
            )
        }
    }

    private fun octahedron(b: MeshBuilder, x: Float, y: Float, z: Float, r: Float, cr: Float, cg: Float, cb: Float) {
        val px = floatArrayOf(r, 0f, -r, 0f)
        val py = floatArrayOf(0f, r, 0f, -r)
        for (k in 0 until 4) {
            val k2 = (k + 1) % 4
            b.flat(x + px[k], y + py[k], z, x + px[k2], y + py[k2], z, x, y, z + r * 0.7f, cr, cg, cb)
            b.flat(x + px[k2], y + py[k2], z, x + px[k], y + py[k], z, x, y, z - r * 0.7f, cr * 0.7f, cg * 0.7f, cb * 0.7f)
        }
    }
```
and to the companion object:
```kotlin
        const val PEBBLE_REACH = 6f
        const val PROJECT_STEPS = 12
        const val APHIDS = 8
        const val STONE_R = 128f / 255f
        const val STONE_G = 124f / 255f
        const val STONE_B = 116f / 255f
        const val STEM_R = 79f / 255f
        const val STEM_G = 122f / 255f
        const val STEM_B = 52f / 255f
        const val PREY_R = 200f / 255f
        const val PREY_G = 182f / 255f
        const val PREY_B = 140f / 255f
        const val AMBER_R = 214f / 255f
        const val AMBER_G = 150f / 255f
        const val AMBER_B = 60f / 255f
        private const val ROCK_SALT = 0x20CL
        private const val APHID_SALT = 0xA41DL
        private const val LUMP_SALT = 0x1E3FL
```
(`repeat` with `return@repeat` only skips one iteration; replace the early exit with a `for` loop and `break` if you prefer.)

- [ ] **Step 4: Run the tests**

Run: `./gradlew :core:test --tests "*RockFoodMeshTest*" --tests "*GroundMeshTest*"`. Expected: all PASS. If `rockVerticesLieOnTheirRock` fails for the lumpy rock, raise `PROJECT_STEPS` (up to 24) and report the worst distance; do not loosen 0.2 mm.

- [ ] **Step 5: Commit**

```bash
git add core/src
git commit -m "M1b-2c: smooth rocks fitted to their surface and faceted food"
```

---

### Task 5: Grass placement and the tuft model

**Files:**
- Create: `core/src/main/kotlin/com/bydesigninteractive/ant/core/render/world/GrassField.kt`
- Test: `core/src/test/kotlin/com/bydesigninteractive/ant/core/render/world/GrassFieldTest.kt`

**Interfaces:**
- Consumes: `ChunkMesher.heights`, `ChunkMesher.spoilAt` (Task 3).
- Produces: `class GrassField(seed: Long)` with `fun tufts(cx: Int, cy: Int, mesher: ChunkMesher): FloatArray` (instance data, `INSTANCE_FLOATS` = 8 per tuft: x, y, z, rotation (rad), scale, blades (5 to 9), tint (0.8 to 1.15), sway phase (0 to 6.28)); `fun tuftMesh(): FloatArray` (the base tuft: 9 blades, one triangle each, vertices x, y, z, nx, ny, nz, blade index, height fraction = 8 floats per vertex, `TUFT_STRIDE` = 8); companion `const val SPOIL_BARE = 3f` (pellets per spoil cell at which no grass grows).

- [ ] **Step 1: Write the failing tests**

```kotlin
package com.bydesigninteractive.ant.core.render.world

import com.bydesigninteractive.ant.sim.world.CHUNK_MM
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GrassFieldTest {
    @Test
    fun grassIsPatchyAndRepeatable() {
        val m = ChunkMesher(4)
        val g = GrassField(4)
        var total = 0
        var empty = 0
        for (cy in 2 until 14) for (cx in 2 until 14) {
            val n = g.tufts(cx, cy, m).size / GrassField.INSTANCE_FLOATS
            total += n
            if (n < 5) empty++
        }
        val perChunk = total / 144f
        assertTrue(perChunk in 15f..120f, "tufts per chunk $perChunk")
        assertTrue(empty >= 10, "patchy grass leaves some chunks nearly bare: $empty")
        assertContentEquals(g.tufts(5, 5, m), GrassField(4).tufts(5, 5, ChunkMesher(4)))
    }

    @Test
    fun tuftsStandOnTheGround() {
        val m = ChunkMesher(4)
        val t = GrassField(4).tufts(6, 9, m)
        var i = 0
        while (i < t.size) {
            assertEquals(m.heights.height(t[i], t[i + 1]), t[i + 2], 1e-3f)
            assertTrue(t[i + 5] in 5f..9f)
            i += GrassField.INSTANCE_FLOATS
        }
    }

    @Test
    fun deepSpoilLeavesNoGrass() {
        val m = ChunkMesher(4)
        val g = GrassField(4)
        val cells = CHUNK_MM / 10
        var cx = 2
        while (g.tufts(cx, 8, m).isEmpty()) cx++
        m.setSpoil(cx, 8, FloatArray(cells * cells) { GrassField.SPOIL_BARE })
        assertEquals(0, g.tufts(cx, 8, m).size)
    }

    @Test
    fun theTuftHasNineTaggedBlades() {
        val v = GrassField(4).tuftMesh()
        assertEquals(9 * 3 * GrassField.TUFT_STRIDE, v.size)
        val blades = (0 until v.size / GrassField.TUFT_STRIDE).map { v[it * GrassField.TUFT_STRIDE + 6].toInt() }.toSet()
        assertEquals((0 until 9).toSet(), blades)
    }
}
```
Run: `./gradlew :core:test --tests "*GrassFieldTest*"`. Expected: compile failure.

- [ ] **Step 2: Implement**

```kotlin
package com.bydesigninteractive.ant.core.render.world

import com.bydesigninteractive.ant.sim.util.hash
import com.bydesigninteractive.ant.sim.util.unit
import com.bydesigninteractive.ant.sim.world.CHUNK_MM
import java.util.Random
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin

/**
 * Patchy grass: candidate spots on a jittered 60 mm grid, kept where a seeded 300 mm value-noise
 * "patchiness" is high, and thinned to nothing where spoil is deep (Lasius niger buries
 * vegetation under its mound). Tufts are scenery only. One base tuft of 9 blades is drawn
 * instanced; each instance picks how many blades show, its rotation, scale, tint and sway phase.
 */
class GrassField(private val seed: Long) {
    fun tufts(cx: Int, cy: Int, mesher: ChunkMesher): FloatArray {
        val out = ArrayList<Float>()
        for (j in 0 until SPOTS) for (i in 0 until SPOTS) {
            val gi = cx * SPOTS + i
            val gj = cy * SPOTS + j
            val r = Random(hash(seed xor SPOT_SALT, gi, gj))
            val x = (gi + r.nextFloat()) * SPOT_MM
            val y = (gj + r.nextFloat()) * SPOT_MM
            val patch = patchiness(x, y)
            val bare = (mesher.spoilAt(x, y) / SPOIL_BARE).coerceIn(0f, 1f)
            val keep = ((patch - PATCH_LOW) / (1f - PATCH_LOW)).coerceIn(0f, 1f) * (1f - bare)
            if (r.nextFloat() >= keep) continue
            out += x
            out += y
            out += mesher.heights.height(x, y)
            out += r.nextFloat() * 6.2832f
            out += 0.6f + r.nextFloat() * 0.7f
            out += (5 + r.nextInt(5)).toFloat()
            out += 0.8f + r.nextFloat() * 0.35f
            out += r.nextFloat() * 6.2832f
        }
        return out.toFloatArray()
    }

    /** Smooth seeded value noise, 0 to 1, at a 300 mm scale. */
    private fun patchiness(x: Float, y: Float): Float {
        val u = x / PATCH_MM
        val v = y / PATCH_MM
        val i = floor(u).toInt()
        val j = floor(v).toInt()
        val fu = smooth(u - i)
        val fv = smooth(v - j)
        val a = unit(seed xor PATCH_SALT, i, j).toFloat()
        val b = unit(seed xor PATCH_SALT, i + 1, j).toFloat()
        val c = unit(seed xor PATCH_SALT, i, j + 1).toFloat()
        val d = unit(seed xor PATCH_SALT, i + 1, j + 1).toFloat()
        return (a + (b - a) * fu) + ((c + (d - c) * fu) - (a + (b - a) * fu)) * fv
    }

    private fun smooth(t: Float) = t * t * (3f - 2f * t)

    /** The base tuft: 9 single-triangle blades around the origin, up to 1 mm wide at the root and 30 to 80 mm tall before scaling. */
    fun tuftMesh(): FloatArray {
        val r = Random(seed xor TUFT_SALT)
        val out = FloatArray(9 * 3 * TUFT_STRIDE)
        var o = 0
        for (blade in 0 until 9) {
            val a = r.nextFloat() * 6.2832f
            val d = r.nextFloat() * 6f
            val bx = cos(a) * d
            val by = sin(a) * d
            val h = 30f + r.nextFloat() * 50f
            val lean = (r.nextFloat() - 0.5f) * 0.6f * h
            val la = r.nextFloat() * 6.2832f
            val w = 0.6f + r.nextFloat() * 0.4f
            val px = -sin(la) * w
            val py = cos(la) * w
            val nx = cos(la)
            val ny = sin(la)
            for ((vx, vy, vz, f) in listOf(
                floatArrayOf(bx - px, by - py, 0f, 0f), floatArrayOf(bx + px, by + py, 0f, 0f),
                floatArrayOf(bx + cos(la) * lean, by + sin(la) * lean, h, 1f),
            )) {
                out[o] = vx; out[o + 1] = vy; out[o + 2] = vz
                out[o + 3] = nx; out[o + 4] = ny; out[o + 5] = 0.3f
                out[o + 6] = blade.toFloat(); out[o + 7] = f
                o += TUFT_STRIDE
            }
        }
        return out
    }

    companion object {
        const val INSTANCE_FLOATS = 8
        const val TUFT_STRIDE = 8
        const val SPOIL_BARE = 3f
        const val SPOTS = 8
        const val SPOT_MM = 62.5f // CHUNK_MM / SPOTS
        const val PATCH_MM = 300f
        const val PATCH_LOW = 0.5f
        private const val SPOT_SALT = 0x6A55L
        private const val PATCH_SALT = 0x9A7CL
        private const val TUFT_SALT = 0x7F7L
    }
}
```
`unit(seed, a, b)` is the public helper in `sim/util/Hash.kt`. Spots tile the map exactly: 8 per chunk side at 62.5 mm.

- [ ] **Step 3: Run the tests**

Run: `./gradlew :core:test --tests "*GrassFieldTest*"`. Expected: 4 PASS. If `grassIsPatchyAndRepeatable` fails only on the density band or the bare-chunk count, tune `PATCH_LOW` (0.4 to 0.6) and report the final value and the measured tufts per chunk.

- [ ] **Step 4: Commit**

```bash
git add core/src
git commit -m "M1b-2c: patchy grass placement that thins on the spoil mound"
```

---

### Task 6: The procedural ant model

**Files:**
- Create: `core/src/main/kotlin/com/bydesigninteractive/ant/core/render/ant/AntMesh.kt`
- Test: `core/src/test/kotlin/com/bydesigninteractive/ant/core/render/ant/AntMeshTest.kt`

**Interfaces:**
- Produces: `object AntMesh` with `fun build(detailed: Boolean): FloatArray`, `const val STRIDE = 13` (x, y, z, nx, ny, nz, r, g, b, part, pivotX, pivotY, pivotZ; model space: mm, x forward, y left, z up along the ant's normal, feet at z 0), part codes `PART_BODY = 0`, legs `1..6` (leg index + 1: 0 front-left, 1 middle-left, 2 rear-left, 3 front-right, 4 middle-right, 5 rear-right), `PART_GASTER = 7`, `PART_PELLET = 8`, `PART_PREY = 9`; `val TRIPOD_A = intArrayOf(0, 4, 2)` (legs that swing together; the others are half a cycle apart).

- [ ] **Step 1: Write the failing tests**

```kotlin
package com.bydesigninteractive.ant.core.render.ant

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AntMeshTest {
    private fun parts(v: FloatArray) = (0 until v.size / AntMesh.STRIDE).map { v[it * AntMesh.STRIDE + 9].toInt() }

    @Test
    fun theDetailedAntIsAboutTwoHundredTriangles() {
        val tris = AntMesh.build(true).size / AntMesh.STRIDE / 3
        assertTrue(tris in 150..320, "detailed ant has $tris triangles")
        val far = AntMesh.build(false).size / AntMesh.STRIDE / 3
        assertTrue(far * 2 <= tris, "far ant ($far) should be at most half the detailed one ($tris)")
    }

    @Test
    fun everyPartIsPresentAndLegsHaveHips() {
        val v = AntMesh.build(true)
        val p = parts(v)
        for (part in 0..9) assertTrue(part in p, "part $part missing")
        for (i in p.indices) if (p[i] in 1..6) {
            val o = i * AntMesh.STRIDE
            // a leg's pivot is its hip on the thorax, above the ground and near the body axis
            assertTrue(v[o + 12] > 0.3f && kotlin.math.abs(v[o + 11]) < 0.6f, "leg pivot ${v[o + 10]}, ${v[o + 11]}, ${v[o + 12]}")
        }
    }

    @Test
    fun theAntStandsOnItsFeetFacingForward() {
        val v = AntMesh.build(true)
        var minZ = Float.MAX_VALUE
        var maxX = -Float.MAX_VALUE
        var minX = Float.MAX_VALUE
        var i = 0
        while (i < v.size) {
            minZ = minOf(minZ, v[i + 2]); maxX = maxOf(maxX, v[i]); minX = minOf(minX, v[i]); i += AntMesh.STRIDE
        }
        assertEquals(0f, minZ, 0.15f)
        assertTrue(maxX > 2f && minX < -2.5f, "head forward, gaster behind: $minX to $maxX")
    }

    @Test
    fun theTripodsSplitTheLegs() {
        val b = (0 until 6).filter { it !in AntMesh.TRIPOD_A }
        assertEquals(listOf(1, 3, 5), b)
    }
}
```
Run: `./gradlew :core:test --tests "*AntMeshTest*"`. Expected: compile failure.

- [ ] **Step 2: Implement**

```kotlin
package com.bydesigninteractive.ant.core.render.ant

import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * The fused low-poly ant, built in code: faceted lumps for the gaster, a thick waist joint, the
 * thorax, a neck joint and the head, plus mandibles, elbowed antennae and six two-segment legs
 * (thin three-sided prisms), and hidden carried pieces (a soil pellet and a prey piece) the shader
 * shows per ant. Every vertex carries a part code and a pivot: a leg's hip, the gaster's centre,
 * or the mandible tip, so the shader can swing legs, swell the gaster and show the load.
 * Model space is mm with x forward, y left, z up; the feet touch z = 0.
 */
object AntMesh {
    const val STRIDE = 13
    const val PART_BODY = 0
    const val PART_GASTER = 7
    const val PART_PELLET = 8
    const val PART_PREY = 9
    val TRIPOD_A = intArrayOf(0, 4, 2)

    private const val BODY_R = 70f / 255f
    private const val BODY_G = 40f / 255f
    private const val BODY_B = 26f / 255f
    private const val LIMB = 0.6f
    private val MANDIBLE_TIP = floatArrayOf(2.75f, 0f, 0.75f)

    private class Out {
        val v = ArrayList<Float>()
        fun tri(a: FloatArray, b: FloatArray, c: FloatArray, shade: Float, part: Int, pivot: FloatArray, color: FloatArray = floatArrayOf(BODY_R, BODY_G, BODY_B)) {
            val ux = b[0] - a[0]; val uy = b[1] - a[1]; val uz = b[2] - a[2]
            val wx = c[0] - a[0]; val wy = c[1] - a[1]; val wz = c[2] - a[2]
            var nx = uy * wz - uz * wy; var ny = uz * wx - ux * wz; var nz = ux * wy - uy * wx
            val l = sqrt(nx * nx + ny * ny + nz * nz).coerceAtLeast(1e-9f)
            nx /= l; ny /= l; nz /= l
            for (p in arrayOf(a, b, c)) {
                v += p[0]; v += p[1]; v += p[2]; v += nx; v += ny; v += nz
                v += color[0] * shade; v += color[1] * shade; v += color[2] * shade
                v += part.toFloat(); v += pivot[0]; v += pivot[1]; v += pivot[2]
            }
        }
    }

    fun build(detailed: Boolean): FloatArray {
        val o = Out()
        val sectors = if (detailed) 6 else 4
        val rings = if (detailed) 3 else 2
        val gasterC = floatArrayOf(-1.6f, 0f, 0.95f)
        lump(o, gasterC, 1.25f, 0.85f, 0.8f, if (detailed) 8 else 4, rings, PART_GASTER, gasterC)
        lump(o, floatArrayOf(-0.45f, 0f, 0.8f), 0.4f, 0.4f, 0.38f, if (detailed) 4 else 3, 2, PART_BODY, ZERO)
        lump(o, floatArrayOf(0.5f, 0f, 0.85f), 0.9f, 0.42f, 0.45f, sectors, rings, PART_BODY, ZERO)
        lump(o, floatArrayOf(1.3f, 0f, 0.9f), 0.32f, 0.3f, 0.3f, if (detailed) 4 else 3, 2, PART_BODY, ZERO)
        lump(o, floatArrayOf(1.95f, 0f, 0.9f), 0.65f, 0.6f, 0.55f, sectors, rings, PART_BODY, ZERO)
        val hips = arrayOf(floatArrayOf(0.9f, 0.3f, 0.65f), floatArrayOf(0.5f, 0.32f, 0.62f), floatArrayOf(0.1f, 0.3f, 0.65f))
        val feet = arrayOf(floatArrayOf(1.9f, 1.6f, 0f), floatArrayOf(0.4f, 1.8f, 0f), floatArrayOf(-1.3f, 1.6f, 0f))
        for (side in 0 until 2) for (k in 0 until 3) {
            val s = if (side == 0) 1f else -1f
            val hip = floatArrayOf(hips[k][0], hips[k][1] * s, hips[k][2])
            val foot = floatArrayOf(feet[k][0], feet[k][1] * s, feet[k][2])
            val knee = floatArrayOf((hip[0] + foot[0]) / 2f, (hip[1] + foot[1]) / 2f + 0.35f * s, 1.05f)
            val part = 1 + side * 3 + k
            if (detailed) {
                limb(o, hip, knee, 0.09f, part, hip)
                limb(o, knee, foot, 0.07f, part, hip)
            } else {
                limb(o, hip, foot, 0.09f, part, hip)
            }
        }
        if (detailed) {
            for (s in floatArrayOf(1f, -1f)) {
                val base = floatArrayOf(2.3f, 0.25f * s, 1.25f)
                val elbow = floatArrayOf(2.55f, 0.55f * s, 2.15f)
                val tip = floatArrayOf(3.55f, 0.85f * s, 1.6f)
                limb(o, base, elbow, 0.05f, PART_BODY, ZERO)
                limb(o, elbow, tip, 0.045f, PART_BODY, ZERO)
                val m0 = floatArrayOf(2.45f, 0.2f * s, 0.7f)
                val m1 = floatArrayOf(2.8f, 0.05f * s, 0.6f)
                val m2 = floatArrayOf(2.5f, 0.3f * s, 0.55f)
                o.tri(m0, m1, m2, 0.7f, PART_BODY, ZERO)
                o.tri(m0, m2, m1, 0.5f, PART_BODY, ZERO)
            }
        }
        lump(o, MANDIBLE_TIP, 0.35f, 0.35f, 0.3f, 4, 2, PART_PELLET, MANDIBLE_TIP, floatArrayOf(0.43f, 0.31f, 0.2f))
        lump(o, MANDIBLE_TIP, 0.5f, 0.4f, 0.35f, 4, 2, PART_PREY, MANDIBLE_TIP, floatArrayOf(0.78f, 0.71f, 0.55f))
        return o.v.toFloatArray()
    }

    private val ZERO = floatArrayOf(0f, 0f, 0f)

    /** A faceted ellipsoid around [c] (latitude rings by longitude sectors), shaded by a fixed key light. */
    private fun lump(o: Out, c: FloatArray, rx: Float, ry: Float, rz: Float, sectors: Int, rings: Int, part: Int, pivot: FloatArray, color: FloatArray = floatArrayOf(BODY_R, BODY_G, BODY_B)) {
        fun p(ring: Int, sector: Int): FloatArray {
            val lat = (-0.5f + ring.toFloat() / rings) * Math.PI.toFloat()
            val lon = sector * 2f * Math.PI.toFloat() / sectors
            return floatArrayOf(c[0] + cos(lat) * cos(lon) * rx, c[1] + cos(lat) * sin(lon) * ry, c[2] + sin(lat) * rz)
        }
        for (ring in 0 until rings) for (s in 0 until sectors) {
            val a = p(ring, s); val b = p(ring, s + 1); val cc = p(ring + 1, s + 1); val d = p(ring + 1, s)
            val shade = 0.85f + 0.3f * ((ring + 0.5f) / rings) - 0.1f * (s % 2)
            if (ring > 0) o.tri(a, b, cc, shade, part, pivot, color)
            if (ring < rings - 1) o.tri(a, cc, d, shade, part, pivot, color)
        }
    }

    /** A thin three-sided prism from [a] to [b]. */
    private fun limb(o: Out, a: FloatArray, b: FloatArray, r: Float, part: Int, pivot: FloatArray) {
        val dx = b[0] - a[0]; val dy = b[1] - a[1]; val dz = b[2] - a[2]
        val l = sqrt(dx * dx + dy * dy + dz * dz)
        // two unit vectors perpendicular to the limb
        var ux = -dy; var uy = dx; var uz = 0f
        var ul = sqrt(ux * ux + uy * uy)
        if (ul < 1e-6f) { ux = 1f; uy = 0f; ul = 1f }
        ux /= ul; uy /= ul
        val vx = (dy * uz - dz * uy) / l; val vy = (dz * ux - dx * uz) / l; val vz = (dx * uy - dy * ux) / l
        fun ring(p: FloatArray, k: Int): FloatArray {
            val t = k * 2f * Math.PI.toFloat() / 3f
            return floatArrayOf(p[0] + (ux * cos(t) + vx * sin(t)) * r, p[1] + (uy * cos(t) + vy * sin(t)) * r, p[2] + (uz * cos(t) + vz * sin(t)) * r)
        }
        for (k in 0 until 3) {
            val a0 = ring(a, k); val a1 = ring(a, k + 1); val b0 = ring(b, k); val b1 = ring(b, k + 1)
            o.tri(a0, a1, b1, LIMB + 0.1f * k, part, pivot)
            o.tri(a0, b1, b0, LIMB + 0.1f * k, part, pivot)
        }
    }
}
```
If the triangle counts fall outside the test's band, change only `sectors`/`rings` for the lumps and report the counts.

- [ ] **Step 3: Run the tests**

Run: `./gradlew :core:test --tests "*AntMeshTest*"`. Expected: 4 PASS; report both triangle counts.

- [ ] **Step 4: Commit**

```bash
git add core/src
git commit -m "M1b-2c: the procedural fused low-poly ant"
```

---

### Task 7: GL 3, the shaders and an instancing smoke test

**Files:**
- Modify: `desktop/src/main/kotlin/com/bydesigninteractive/ant/desktop/DesktopLauncher.kt` (GL 3)
- Modify: `android/src/main/kotlin/com/bydesigninteractive/ant/AntDream.kt` (`gdxConfig`: GL 3)
- Create: `core/src/main/kotlin/com/bydesigninteractive/ant/core/render/Shaders.kt`
- Create: `core/src/main/kotlin/com/bydesigninteractive/ant/core/render/InstancingCheck.kt`
- Modify: `core/src/main/kotlin/com/bydesigninteractive/ant/core/AntApp.kt` (run the check once at start and log it)

**Interfaces:**
- Consumes: `SkyState` (Task 2).
- Produces: `object Shaders` with `fun world(): ShaderProgram`, `fun ants(): ShaderProgram`, `fun grass(): ShaderProgram`, `fun shadows(): ShaderProgram`, `fun sky(): ShaderProgram`, and `fun applySky(p: ShaderProgram, sky: SkyState, camera: Camera)` (sets `u_projView`, `u_sunDir`, `u_sunColor`, `u_ambient`, `u_fogColor`, `u_fogStart`, `u_fogEnd`, `u_eye`). Each `fun` compiles a new program and throws `IllegalStateException` with the log if it fails. `object InstancingCheck` with `fun run(): Boolean` (true if GL 3 instancing drew without a GL error).

- [ ] **Step 1: Enable GL 3**

Desktop: in `DesktopLauncher`, inside `apply { }`, add `setOpenGLEmulation(Lwjgl3ApplicationConfiguration.GLEmulation.GL30, 3, 2)`. Android: in `gdxConfig()` add `useGL30 = true`. libGDX's LWJGL3 backend then prepends a GLSL 1.40 compatibility header to every shader (including SpriteBatch's and ModelBatch's), which is why our shaders use the legacy style (see Global Constraints).

- [ ] **Step 2: The shaders**

```kotlin
package com.bydesigninteractive.ant.core.render

import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.graphics.glutils.ShaderProgram
import com.bydesigninteractive.ant.core.render.sky.SkyState

/**
 * The GLSL for the low-poly look, in the legacy style that both libGDX's desktop GL 3 prepend and
 * GLES 3 accept. All world shaders share one light model: a directional light plus ambient, then
 * linear fog toward the fog colour by distance from the eye. Positions are world millimetres.
 */
object Shaders {
    private const val PRECISION = "#ifdef GL_ES\nprecision mediump float;\n#endif\n"

    private const val LIGHT = """
uniform vec3 u_sunDir;
uniform vec3 u_sunColor;
uniform vec3 u_ambient;
uniform vec3 u_fogColor;
uniform float u_fogStart;
uniform float u_fogEnd;
uniform vec3 u_eye;
vec3 lit(vec3 color, vec3 n) { return color * (u_ambient + u_sunColor * max(dot(n, -u_sunDir), 0.0)); }
float fogAmount(vec3 p) { return clamp((distance(p, u_eye) - u_fogStart) / (u_fogEnd - u_fogStart), 0.0, 1.0); }
"""

    private const val FRAG = PRECISION + """
varying vec3 v_color;
varying float v_fog;
uniform vec3 u_fogColor;
void main() { gl_FragColor = vec4(mix(v_color, u_fogColor, v_fog), 1.0); }
"""

    private const val WORLD_VERT = """
attribute vec3 a_position;
attribute vec3 a_normal;
attribute vec3 a_color;
uniform mat4 u_projView;
""" + LIGHT + """
varying vec3 v_color;
varying float v_fog;
void main() {
    v_color = lit(a_color, normalize(a_normal));
    v_fog = fogAmount(a_position);
    gl_Position = u_projView * vec4(a_position, 1.0);
}
"""

    // Ants: model space x forward, y left, z up; instances carry position + gait phase, forward + gaster fill, up + carry.
    private const val ANT_VERT = """
attribute vec3 a_position;
attribute vec3 a_normal;
attribute vec3 a_color;
attribute float a_part;
attribute vec3 a_pivot;
attribute vec4 i_pos;
attribute vec4 i_fwd;
attribute vec4 i_up;
uniform mat4 u_projView;
""" + LIGHT + """
varying vec3 v_color;
varying float v_fog;
const float TAU = 6.2831853;
void main() {
    vec3 p = a_position;
    vec3 n = a_normal;
    float part = floor(a_part + 0.5);
    float phase = i_pos.w;
    if (part >= 1.0 && part <= 6.0) {
        float leg = part - 1.0;
        float group = (leg == 0.0 || leg == 4.0 || leg == 2.0) ? 0.0 : 0.5;
        float s = sin(TAU * (phase + group));
        float swing = 0.35 * s;
        vec3 d = p - a_pivot;
        float c = cos(swing);
        float sn = sin(swing);
        p = a_pivot + vec3(c * d.x - sn * d.y, sn * d.x + c * d.y, d.z);
        n = vec3(c * n.x - sn * n.y, sn * n.x + c * n.y, n.z);
        p.z += 0.25 * max(0.0, cos(TAU * (phase + group))) * clamp(-d.z / 0.65, 0.0, 1.0);
    } else if (part == 7.0) {
        p = a_pivot + (p - a_pivot) * (1.0 + 0.4 * i_fwd.w);
    } else if (part == 8.0) {
        if (abs(i_up.w - 1.0) > 0.5) p = a_pivot;
    } else if (part == 9.0) {
        if (abs(i_up.w - 2.0) > 0.5) p = a_pivot;
    }
    p.z += 0.05 * sin(2.0 * TAU * phase);
    vec3 f = normalize(i_fwd.xyz);
    vec3 u = normalize(i_up.xyz);
    vec3 l = cross(u, f);
    vec3 world = i_pos.xyz + f * p.x + l * p.y + u * p.z;
    vec3 wn = normalize(f * n.x + l * n.y + u * n.z);
    v_color = lit(a_color, wn);
    v_fog = fogAmount(world);
    gl_Position = u_projView * vec4(world, 1.0);
}
"""

    // Shadows: a disc under each ant, pushed away from the sun, darker by day.
    private const val SHADOW_VERT = """
attribute vec3 a_position;
attribute float a_alpha;
attribute vec4 i_pos;
attribute vec4 i_fwd;
attribute vec4 i_up;
uniform mat4 u_projView;
uniform vec3 u_sunDir;
uniform float u_strength;
varying float v_alpha;
void main() {
    vec3 f = normalize(i_fwd.xyz);
    vec3 u = normalize(i_up.xyz);
    vec3 l = cross(u, f);
    vec3 offset = (u_sunDir - u * dot(u_sunDir, u)) * 0.8;
    vec3 world = i_pos.xyz + offset + f * a_position.x + l * a_position.y + u * 0.08;
    v_alpha = a_alpha * u_strength;
    gl_Position = u_projView * vec4(world, 1.0);
}
"""

    private const val SHADOW_FRAG = PRECISION + """
varying float v_alpha;
void main() { gl_FragColor = vec4(0.0, 0.0, 0.0, v_alpha); }
"""

    // Grass: one base tuft, instances carry place (x, y, z, rotation) and style (scale, blades, tint, sway phase).
    private const val GRASS_VERT = """
attribute vec3 a_position;
attribute vec3 a_normal;
attribute float a_blade;
attribute float a_tip;
attribute vec4 i_place;
attribute vec4 i_style;
uniform mat4 u_projView;
uniform float u_time;
""" + LIGHT + """
varying vec3 v_color;
varying float v_fog;
void main() {
    float c = cos(i_place.w);
    float s = sin(i_place.w);
    vec3 p = a_position * i_style.x;
    if (a_blade >= i_style.y) p = vec3(0.0);
    float sway = sin(u_time * 1.3 + i_style.w + a_blade) * 2.5 * a_tip * i_style.x;
    vec3 world = i_place.xyz + vec3(c * p.x - s * p.y + sway, s * p.x + c * p.y, p.z);
    vec3 n = normalize(vec3(c * a_normal.x - s * a_normal.y, s * a_normal.x + c * a_normal.y, a_normal.z));
    vec3 base = vec3(96.0, 132.0, 70.0) / 255.0 * i_style.z * (0.75 + 0.35 * a_tip);
    v_color = lit(base, n);
    v_fog = fogAmount(world);
    gl_Position = u_projView * vec4(world, 1.0);
}
"""

    // Sky: a full-screen quad, top colour to horizon colour by screen height.
    private const val SKY_VERT = """
attribute vec2 a_position;
varying float v_t;
void main() { v_t = a_position.y * 0.5 + 0.5; gl_Position = vec4(a_position, 0.9999, 1.0); }
"""

    private const val SKY_FRAG = PRECISION + """
varying float v_t;
uniform vec3 u_top;
uniform vec3 u_horizon;
void main() { gl_FragColor = vec4(mix(u_horizon, u_top, smoothstep(0.35, 1.0, v_t)), 1.0); }
"""

    fun world() = compile(WORLD_VERT, FRAG)
    fun ants() = compile(ANT_VERT, FRAG)
    fun grass() = compile(GRASS_VERT, FRAG)
    fun shadows() = compile(SHADOW_VERT, SHADOW_FRAG)
    fun sky() = compile(SKY_VERT, SKY_FRAG)

    /** Sets the light, fog and camera uniforms every world shader shares. The program must be bound. */
    fun applySky(p: ShaderProgram, sky: SkyState, camera: Camera) {
        p.setUniformMatrix("u_projView", camera.combined)
        p.setUniformf("u_sunDir", sky.sunDir[0], sky.sunDir[1], sky.sunDir[2])
        p.setUniformf("u_sunColor", sky.sunColor[0], sky.sunColor[1], sky.sunColor[2])
        p.setUniformf("u_ambient", sky.ambient[0], sky.ambient[1], sky.ambient[2])
        p.setUniformf("u_fogColor", sky.fogColor[0], sky.fogColor[1], sky.fogColor[2])
        p.setUniformf("u_fogStart", sky.fogStart)
        p.setUniformf("u_fogEnd", sky.fogEnd)
        p.setUniformf("u_eye", camera.position.x, camera.position.y, camera.position.z)
    }

    private fun compile(vert: String, frag: String): ShaderProgram {
        ShaderProgram.pedantic = false
        val p = ShaderProgram(vert, frag)
        check(p.isCompiled) { "shader failed to compile: ${p.log}" }
        return p
    }
}
```
`ShaderProgram.pedantic = false` lets a program omit uniforms it does not use (for example `u_sunDir` in the shadow shader is used, but `u_eye` is not). Uniforms set on a program that lacks them are ignored when pedantic is off.

- [ ] **Step 3: The instancing check**

```kotlin
package com.bydesigninteractive.ant.core.render

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.Mesh
import com.badlogic.gdx.graphics.VertexAttribute
import com.badlogic.gdx.graphics.VertexAttributes
import com.badlogic.gdx.graphics.glutils.ShaderProgram

/**
 * Draws two instances of one triangle off-screen-sized (into the current target) with the
 * instancing path the ant, shadow and grass renderers use, and reports whether GL accepted it.
 * Run once at start; a false result means the 3D view falls back to the debug renderer.
 */
object InstancingCheck {
    fun run(): Boolean {
        if (Gdx.gl30 == null) return false
        return try {
            val mesh = Mesh(true, 3, 0, VertexAttribute(VertexAttributes.Usage.Position, 3, "a_position"))
            mesh.setVertices(floatArrayOf(0f, 0f, 0f, 1f, 0f, 0f, 0f, 1f, 0f))
            mesh.enableInstancedRendering(true, 2, VertexAttribute(VertexAttributes.Usage.Generic, 4, "i_pos"))
            mesh.setInstanceData(floatArrayOf(0f, 0f, 0f, 0f, 0.5f, 0f, 0f, 0f))
            val shader = ShaderProgram(
                "attribute vec3 a_position;\nattribute vec4 i_pos;\nvoid main() { gl_Position = vec4(a_position + i_pos.xyz, 1.0) * 0.0; }\n",
                "#ifdef GL_ES\nprecision mediump float;\n#endif\nvoid main() { gl_FragColor = vec4(0.0); }\n",
            )
            check(shader.isCompiled) { shader.log }
            shader.bind()
            mesh.render(shader, GL20.GL_TRIANGLES)
            val error = Gdx.gl.glGetError()
            shader.dispose()
            mesh.dispose()
            error == GL20.GL_NO_ERROR
        } catch (t: Throwable) {
            Gdx.app.error("AntFarm", "instancing check failed", t)
            false
        }
    }
}
```
In libGDX 1.14 the instancing API is `Mesh.enableInstancedRendering(isStatic: Boolean, maxInstances: Int, vararg attributes: VertexAttribute)` and `Mesh.setInstanceData(data: FloatArray)`; if the real names or signatures differ, use the real ones and say so in the report.

- [ ] **Step 4: Log it at start**

In `AntApp.create()`, after the GL objects are created and before `runner.start()`, add:
```kotlin
        instanced = InstancingCheck.run()
        Gdx.app.log(APP_LOG_TAG, "gl ${Gdx.graphics.glVersion.majorVersion}.${Gdx.graphics.glVersion.minorVersion} instancing $instanced")
```
with `private var instanced = false` as a field (Task 8 uses it to pick the renderer). Also compile each `Shaders` program once there, dispose them, and log `shaders ok` (or the exception message), so a shader error shows on the first run.

- [ ] **Step 5: Check desktop and emulator**

1. Run `./gradlew :core:test :desktop:classes :android:assembleProfile` in the background; expect BUILD SUCCESSFUL.
2. Launch `:desktop:run` in the background with output to `build/task7-desktop.log` for about 30 s, then kill only its JVM. Expected log lines: `gl 3.2 instancing true` (or higher), `shaders ok`, then the usual 10 s lines, with no exceptions. The existing views (nest, top-down via SpriteBatch; the 3D debug view via ModelBatch) must still run under GL 3; any GL error or shader-compile exception in the log is a failure to fix.
3. Install the profile APK on `emulator-5554` only, start `PreviewActivity` with `adb -s emulator-5554 shell am start -n com.bydesigninteractive.ant/.PreviewActivity`, wait 20 s, read `adb -s emulator-5554 logcat -d -s AntFarm AndroidRuntime`. Expected: `instancing true`, `shaders ok`, no `FATAL`. Uninstall from the emulator.

- [ ] **Step 6: Commit**

```bash
git add desktop android core
git commit -m "M1b-2c: GL 3 on both platforms, the look's shaders and an instancing check"
```

---

### Task 8: The chunk cache and the new 3D world view

**Files:**
- Create: `core/src/main/kotlin/com/bydesigninteractive/ant/core/render/world/ChunkCache.kt`
- Create: `core/src/main/kotlin/com/bydesigninteractive/ant/core/render/SurfaceRenderer3D.kt`
- Modify: `core/src/main/kotlin/com/bydesigninteractive/ant/core/AntApp.kt` (use it when `instanced`; keep `DebugSurfaceRenderer` as the fallback)
- Test: `core/src/test/kotlin/com/bydesigninteractive/ant/core/render/world/ChunkCacheTest.kt`

**Interfaces:**
- Consumes: `ChunkMesher` (`ground`, `rocks`, `foods`, `setSpoil`), `GrassField.tufts` (Tasks 3 to 5); `Published.overlays` (`OverlayChunk(stamp, trail, spoil)`), `Published.rocks`, `Published.placed`; `Shaders`, `SkyState`, `DayCycle`; `ChaseCamera3` (`eyeX/Y/Z`, `targetX/Y/Z`, `upX/Y/Z`, `placed`).
- Produces: `class ChunkCache(seed: Long)` with `fun request(cx, cy, spoil: FloatArray?, rocks: List<Blob>)`, `fun poll(maxResults: Int): List<ChunkResult>`, `fun close()`; `class ChunkResult(val cx: Int, val cy: Int, val ground: MeshData, val rocks: MeshData, val grass: FloatArray, val version: Long)`. `class SurfaceRenderer3D(seed: Long, published: Published) : Disposable` with `val camera: PerspectiveCamera`, `fun resize(w, h)`, `fun groundHeight(x, y): Float`, `fun draw(chase: ChaseCamera3, poses: List<AntPose>, foods: List<FoodView>, seconds: Float, animator: AntAnimator)` (ants are drawn in Task 9; this task draws sky, ground, specks, rocks and food).

- [ ] **Step 1: Write the failing cache test**

```kotlin
package com.bydesigninteractive.ant.core.render.world

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ChunkCacheTest {
    @Test
    fun requestedChunksComeBackBuiltOffTheCallingThread() {
        val cache = ChunkCache(4)
        try {
            cache.request(8, 8, null, emptyList())
            cache.request(9, 8, null, emptyList())
            val got = ArrayList<ChunkResult>()
            val deadline = System.nanoTime() + 10_000_000_000L
            while (got.size < 2 && System.nanoTime() < deadline) {
                got += cache.poll(2)
                Thread.sleep(5)
            }
            assertEquals(setOf(8 to 8, 9 to 8), got.map { it.cx to it.cy }.toSet())
            assertTrue(got.all { it.ground.vertexCount > 1000 })
        } finally {
            cache.close()
        }
    }

    @Test
    fun aNewerRequestReplacesAnOlderOne() {
        val cache = ChunkCache(4)
        try {
            cache.request(8, 8, null, emptyList())
            cache.request(8, 8, FloatArray(50 * 50) { 20f }, emptyList())
            val got = ArrayList<ChunkResult>()
            val deadline = System.nanoTime() + 10_000_000_000L
            while (System.nanoTime() < deadline) {
                got += cache.poll(4)
                if (got.any { it.version == 2L }) break
                Thread.sleep(5)
            }
            assertTrue(got.last().version == 2L, "the latest request must be the last result")
        } finally {
            cache.close()
        }
    }
}
```
Run: `./gradlew :core:test --tests "*ChunkCacheTest*"`. Expected: compile failure.

- [ ] **Step 2: `ChunkCache`**

```kotlin
package com.bydesigninteractive.ant.core.render.world

import com.bydesigninteractive.ant.sim.world.CHUNKS
import com.bydesigninteractive.ant.sim.world.sdf.Blob
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.LinkedBlockingDeque
import java.util.concurrent.atomic.AtomicLong

/** One built chunk: vertex arrays for the ground (with specks) and rocks, and grass instances. */
class ChunkResult(val cx: Int, val cy: Int, val ground: MeshData, val rocks: MeshData, val grass: FloatArray, val version: Long)

/**
 * Builds chunks on one background thread. The render thread requests a chunk with immutable
 * published data (its spoil array and rock list) and polls finished results to upload. Requests
 * are applied in order, so a chunk's spoil mirror is current before it is rebuilt; a result is
 * tagged with its request's version, and the renderer keeps only the newest per chunk.
 */
class ChunkCache(seed: Long) {
    private class Job(val cx: Int, val cy: Int, val spoil: FloatArray?, val rocks: List<Blob>, val version: Long)

    private val mesher = ChunkMesher(seed)
    private val grass = GrassField(seed)
    private val jobs = LinkedBlockingDeque<Job>()
    private val results = ConcurrentLinkedQueue<ChunkResult>()
    private val versions = AtomicLong()
    @Volatile private var running = true
    private val thread = Thread(::loop, "chunk-mesher").apply {
        isDaemon = true
        start()
    }

    /** Queues chunk (cx, cy) for building with its published spoil (immutable, may be null) and rocks. Returns the request's version. */
    fun request(cx: Int, cy: Int, spoil: FloatArray?, rocks: List<Blob>): Long {
        val v = versions.incrementAndGet()
        jobs.addLast(Job(cx, cy, spoil, rocks, v))
        return v
    }

    /** Up to [maxResults] finished chunks, oldest first. */
    fun poll(maxResults: Int): List<ChunkResult> {
        val out = ArrayList<ChunkResult>(maxResults)
        while (out.size < maxResults) out += results.poll() ?: break
        return out
    }

    fun close() {
        running = false
        thread.interrupt()
        thread.join(2000)
    }

    private fun loop() {
        try {
            while (running) {
                val job = jobs.takeFirst()
                if (job.cx !in 0 until CHUNKS || job.cy !in 0 until CHUNKS) continue
                mesher.setSpoil(job.cx, job.cy, job.spoil)
                results += ChunkResult(job.cx, job.cy, mesher.ground(job.cx, job.cy), mesher.rocks(job.rocks), grass.tufts(job.cx, job.cy, mesher), job.version)
            }
        } catch (e: InterruptedException) {
            // closing
        }
    }
}
```
The test's second case relies on each request producing a result in order (versions 1 then 2); keep it that way (no coalescing in this task).

- [ ] **Step 3: `SurfaceRenderer3D` (world only)**

Read `DebugSurfaceRenderer.kt` and `AntApp.draw3d` first; the new renderer has the same entry points. Write:

```kotlin
package com.bydesigninteractive.ant.core.render

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.Mesh
import com.badlogic.gdx.graphics.PerspectiveCamera
import com.badlogic.gdx.graphics.VertexAttribute
import com.badlogic.gdx.graphics.VertexAttributes
import com.badlogic.gdx.graphics.glutils.ShaderProgram
import com.badlogic.gdx.utils.Disposable
import com.bydesigninteractive.ant.core.camera.ChaseCamera3
import com.bydesigninteractive.ant.core.engine.AntPose
import com.bydesigninteractive.ant.core.engine.FoodView
import com.bydesigninteractive.ant.core.engine.Published
import com.bydesigninteractive.ant.core.render.sky.DayCycle
import com.bydesigninteractive.ant.core.render.sky.SkyState
import com.bydesigninteractive.ant.core.render.world.ChunkCache
import com.bydesigninteractive.ant.core.render.world.ChunkMesher
import com.bydesigninteractive.ant.core.render.world.MeshData
import com.bydesigninteractive.ant.sim.world.CHUNKS
import com.bydesigninteractive.ant.sim.world.CHUNK_MM
import com.bydesigninteractive.ant.sim.world.sdf.Blob
import kotlin.math.abs
import kotlin.math.floor

/**
 * The low-poly 3D surface view. Chunks in a 5 by 5 ring around the camera focus are built by a
 * [ChunkCache] from published data and uploaded here, at most [UPLOADS_PER_FRAME] per frame; a
 * chunk is re-requested when its published spoil or rock list changes, at most every
 * [REBUILD_NANOS] per chunk. The sky state comes from the [DayCycle]. Works in simulation
 * coordinates (mm, z up). Render thread only, except the cache's own thread.
 */
class SurfaceRenderer3D(private val seed: Long, private val published: Published) : Disposable {
    val camera = PerspectiveCamera(FOV, 1f, 1f).apply {
        near = 2f
        far = 3000f
    }
    private val sky = SkyState()
    private val cache = ChunkCache(seed)
    private val heights = ChunkMesher(seed) // render thread's own copy, for camera heights only
    private val world: ShaderProgram = Shaders.world()
    private val skyShader: ShaderProgram = Shaders.sky()
    private val skyQuad = Mesh(true, 4, 6, VertexAttribute(VertexAttributes.Usage.Position, 2, "a_position")).apply {
        setVertices(floatArrayOf(-1f, -1f, 1f, -1f, 1f, 1f, -1f, 1f))
        setIndices(shortArrayOf(0, 1, 2, 0, 2, 3))
    }

    private class Slot(var version: Long = 0L, var ground: Mesh? = null, var rocks: Mesh? = null, var grass: FloatArray = FloatArray(0), var spoil: FloatArray? = null, var rockList: List<Blob>? = null, var requestedAt: Long = 0L, var requested: Long = 0L)

    private val slots = HashMap<Int, Slot>()
    private var foodMesh: Mesh? = null
    private var foodsDrawn: List<FoodView>? = null

    fun resize(w: Int, h: Int) {
        camera.viewportWidth = w.toFloat()
        camera.viewportHeight = h.toFloat()
    }

    /** The ground height at (x, y) as drawn (base relief; spoil is mirrored only by the mesher thread). */
    fun groundHeight(x: Float, y: Float): Float = heights.heights.height(x, y)

    fun draw(chase: ChaseCamera3, poses: List<AntPose>, foods: List<FoodView>, seconds: Float, animator: AntAnimator) {
        camera.position.set(chase.eyeX, chase.eyeY, chase.eyeZ)
        camera.up.set(chase.upX, chase.upY, chase.upZ)
        camera.lookAt(chase.targetX, chase.targetY, chase.targetZ)
        camera.update()
        DayCycle.sky(DayCycle.timeOfDay(seconds), sky)
        val fcx = floor(chase.targetX / CHUNK_MM).toInt()
        val fcy = floor(chase.targetY / CHUNK_MM).toInt()
        refreshChunks(fcx, fcy)
        uploadFinished()
        if (foods !== foodsDrawn) {
            foodMesh?.dispose()
            foodMesh = upload(ChunkMesher(seed).foods(foods))
            foodsDrawn = foods
        }
        val gl = Gdx.gl
        gl.glClear(GL20.GL_DEPTH_BUFFER_BIT)
        gl.glDisable(GL20.GL_DEPTH_TEST)
        skyShader.bind()
        skyShader.setUniformf("u_top", sky.skyTop[0], sky.skyTop[1], sky.skyTop[2])
        skyShader.setUniformf("u_horizon", sky.skyHorizon[0], sky.skyHorizon[1], sky.skyHorizon[2])
        skyQuad.render(skyShader, GL20.GL_TRIANGLES)
        gl.glEnable(GL20.GL_DEPTH_TEST)
        gl.glEnable(GL20.GL_CULL_FACE)
        world.bind()
        Shaders.applySky(world, sky, camera)
        for (slot in slots.values) {
            slot.ground?.render(world, GL20.GL_TRIANGLES)
            slot.rocks?.render(world, GL20.GL_TRIANGLES)
        }
        gl.glDisable(GL20.GL_CULL_FACE) // stems and food lumps are thin; draw both sides
        foodMesh?.render(world, GL20.GL_TRIANGLES)
        drawAnts(poses, animator)
        gl.glDisable(GL20.GL_DEPTH_TEST)
    }

    /** Ants, shadows and grass arrive in Task 9. */
    private fun drawAnts(poses: List<AntPose>, animator: AntAnimator) {}

    private fun refreshChunks(fcx: Int, fcy: Int) {
        val now = System.nanoTime()
        for (cy in fcy - RING..fcy + RING) for (cx in fcx - RING..fcx + RING) {
            if (cx < 0 || cy < 0 || cx >= CHUNKS || cy >= CHUNKS) continue
            val key = cx + cy * CHUNKS
            val slot = slots.getOrPut(key) { Slot() }
            val spoil = published.overlays[key]?.spoil
            val rocks = rocksFor(cx, cy)
            val changed = slot.requested == 0L || (spoil !== slot.spoil && !(spoil contentEquals slot.spoil)) || rocks !== slot.rockList
            if (changed && (slot.requested == 0L || now - slot.requestedAt >= REBUILD_NANOS)) {
                slot.spoil = spoil
                slot.rockList = rocks
                slot.requested = cache.request(cx, cy, spoil, rocks)
                slot.requestedAt = now
            }
        }
        val drop = slots.keys.filter { abs(it % CHUNKS - fcx) > RING + 1 || abs(it / CHUNKS - fcy) > RING + 1 }
        for (k in drop) slots.remove(k)?.let { it.ground?.dispose(); it.rocks?.dispose() }
    }

    /** This chunk's published rocks plus placed objects whose centre falls in it; immutable lists from [Published]. */
    private fun rocksFor(cx: Int, cy: Int): List<Blob> {
        val own = published.rocks[cx + cy * CHUNKS] ?: emptyList()
        val placed = published.placed.filter { floor(it.cx / CHUNK_MM).toInt() == cx && floor(it.cy / CHUNK_MM).toInt() == cy }
        return if (placed.isEmpty()) own else own + placed
    }

    private fun uploadFinished() {
        for (r in cache.poll(UPLOADS_PER_FRAME)) {
            val slot = slots[r.cx + r.cy * CHUNKS] ?: continue
            if (r.version < slot.version) continue
            slot.version = r.version
            slot.ground?.dispose()
            slot.rocks?.dispose()
            slot.ground = upload(r.ground)
            slot.rocks = upload(r.rocks)
            slot.grass = r.grass
        }
    }

    private fun upload(d: MeshData): Mesh? {
        if (d.vertexCount == 0) return null
        return Mesh(
            true, d.vertexCount, 0,
            VertexAttribute(VertexAttributes.Usage.Position, 3, "a_position"),
            VertexAttribute(VertexAttributes.Usage.Normal, 3, "a_normal"),
            VertexAttribute(VertexAttributes.Usage.Generic, 3, "a_color"),
        ).apply { setVertices(d.vertices) }
    }

    override fun dispose() {
        cache.close()
        for (s in slots.values) { s.ground?.dispose(); s.rocks?.dispose() }
        foodMesh?.dispose()
        world.dispose()
        skyShader.dispose()
        skyQuad.dispose()
    }

    private companion object {
        const val FOV = 60f
        const val RING = 2
        const val UPLOADS_PER_FRAME = 2
        const val REBUILD_NANOS = 3_000_000_000L
    }
}
```
Notes for the implementer:
- `rocksFor` builds a new list when placed objects exist, which would re-request every frame; cache the combined list per chunk and rebuild it only when `published.rocks[key]` or `published.placed` changes identity.
- `ChunkMesher(seed).foods(foods)` constructs a mesher (and its height field) on the render thread each time the food list changes; keep one render-thread `ChunkMesher` for that instead (`heights` above), since `foods()` does not read heights.
- Face culling: the ground and rock triangles must be counter-clockwise seen from above/outside. If the ground disappears with culling on, the winding in `ChunkMesher.ground` is clockwise; fix the winding there (and its test still passes), not by disabling culling.

- [ ] **Step 4: Use it in `AntApp`**

In `AntApp`: keep `renderer3d: DebugSurfaceRenderer` for the fallback, add `private var look: SurfaceRenderer3D? = null`, created in `create()` when `instanced` is true. In `resize`, resize both. In `draw3d`, when `look != null`, call `look.draw(chase, drawn, states.foods, states.tick * DT, animator)` and use `look.groundHeight` for the camera's start height; otherwise the existing debug path. Dispose both. Keep the existing GL clear colour handling; the sky quad covers the background.

- [ ] **Step 5: Tests and a desktop run**

Run `./gradlew :core:test :desktop:classes` in the background; expect BUILD SUCCESSFUL. Launch `:desktop:run` for 30 s (log to `build/task8-desktop.log`), kill only its JVM; expect `instancing true`, `shaders ok`, no exceptions, ticks/s near 20. The visual check is the controller's.

- [ ] **Step 6: Commit**

```bash
git add core
git commit -m "M1b-2c: a background chunk cache and the low-poly world view"
```

---

### Task 9: Instanced ants, shadows and grass

**Files:**
- Create: `core/src/main/kotlin/com/bydesigninteractive/ant/core/render/ant/AntRenderer.kt`
- Create: `core/src/main/kotlin/com/bydesigninteractive/ant/core/render/world/GrassRenderer.kt`
- Modify: `core/src/main/kotlin/com/bydesigninteractive/ant/core/render/SurfaceRenderer3D.kt` (`drawAnts`, grass)
- Test: `core/src/test/kotlin/com/bydesigninteractive/ant/core/render/ant/AntInstancesTest.kt`

**Interfaces:**
- Consumes: `AntMesh` (Task 6), `GrassField.tuftMesh`, `ChunkResult.grass` (Tasks 5 and 8), `Shaders.ants/shadows/grass`, `Shaders.applySky`, `AntPose` (with `crop`, `carry` from Task 1), `AntAnimator.phase(id)`.
- Produces: `object AntInstances` with `fun fill(poses: List<AntPose>, animator: AntAnimator, eyeX: Float, eyeY: Float, eyeZ: Float, near: FloatArray, far: FloatArray, counts: IntArray)` (pure; writes 12 floats per surface ant into `near` or `far` by distance `NEAR_MM` = 500, `counts[0]` near, `counts[1]` far), `const val FLOATS = 12`; `class AntRenderer : Disposable` with `fun draw(poses, animator, sky: SkyState, camera: Camera)`; `class GrassRenderer(seed: Long) : Disposable` with `fun draw(chunks: Collection<FloatArray>, sky: SkyState, camera: Camera, time: Float)`.

- [ ] **Step 1: Write the failing test (the pure instance packing)**

```kotlin
package com.bydesigninteractive.ant.core.render.ant

import com.bydesigninteractive.ant.core.engine.AntPose
import com.bydesigninteractive.ant.core.engine.CARRY_PELLET
import com.bydesigninteractive.ant.core.render.AntAnimator
import com.bydesigninteractive.ant.sim.ant.Space
import kotlin.test.Test
import kotlin.test.assertEquals

class AntInstancesTest {
    private fun pose(id: Int, x: Float, space: Space = Space.SURFACE) = AntPose().apply {
        this.id = id; this.x = x; this.y = 0f; this.z = 0f; this.space = space
        fx = 1f; fy = 0f; fz = 0f; nx = 0f; ny = 0f; nz = 1f; crop = 0.5f; carry = CARRY_PELLET
    }

    @Test
    fun antsSplitByDistanceAndNestAntsAreSkipped() {
        val poses = listOf(pose(0, 100f), pose(1, 900f), pose(2, 50f, Space.NEST))
        val near = FloatArray(64)
        val far = FloatArray(64)
        val counts = IntArray(2)
        AntInstances.fill(poses, AntAnimator(), 0f, 0f, 30f, near, far, counts)
        assertEquals(1, counts[0])
        assertEquals(1, counts[1])
        assertEquals(100f, near[0])
        assertEquals(0.5f, near[7])
        assertEquals(CARRY_PELLET.toFloat(), near[11])
        assertEquals(900f, far[0])
    }
}
```
Run: `./gradlew :core:test --tests "*AntInstancesTest*"`. Expected: compile failure.

- [ ] **Step 2: `AntInstances` and `AntRenderer`**

In `AntRenderer.kt`:
```kotlin
package com.bydesigninteractive.ant.core.render.ant

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.Mesh
import com.badlogic.gdx.graphics.VertexAttribute
import com.badlogic.gdx.graphics.VertexAttributes
import com.badlogic.gdx.utils.Disposable
import com.bydesigninteractive.ant.core.engine.AntPose
import com.bydesigninteractive.ant.core.render.AntAnimator
import com.bydesigninteractive.ant.core.render.Shaders
import com.bydesigninteractive.ant.core.render.sky.SkyState
import com.bydesigninteractive.ant.sim.ant.Space
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

/** Packs surface ants into instance records: position + gait phase, forward + gaster fill, up + carry. Pure. */
object AntInstances {
    const val FLOATS = 12
    const val NEAR_MM = 500f

    fun fill(poses: List<AntPose>, animator: AntAnimator, eyeX: Float, eyeY: Float, eyeZ: Float, near: FloatArray, far: FloatArray, counts: IntArray) {
        counts[0] = 0
        counts[1] = 0
        for (i in poses.indices) {
            val p = poses[i]
            if (p.space != Space.SURFACE) continue
            val dx = p.x - eyeX; val dy = p.y - eyeY; val dz = p.z - eyeZ
            val isNear = dx * dx + dy * dy + dz * dz < NEAR_MM * NEAR_MM
            val out = if (isNear) near else far
            val k = if (isNear) 0 else 1
            val o = counts[k] * FLOATS
            if (o + FLOATS > out.size) continue
            out[o] = p.x; out[o + 1] = p.y; out[o + 2] = p.z; out[o + 3] = animator.phase(p.id)
            out[o + 4] = p.fx; out[o + 5] = p.fy; out[o + 6] = p.fz; out[o + 7] = p.crop
            out[o + 8] = p.nx; out[o + 9] = p.ny; out[o + 10] = p.nz; out[o + 11] = p.carry.toFloat()
            counts[k]++
        }
    }
}

/**
 * Draws all surface ants with GL 3 instancing: shadows first (blended, no depth write), then the
 * detailed model for ants within [AntInstances.NEAR_MM] of the eye and the simple one beyond.
 * The vertex shader poses legs, swells the gaster and shows the carried piece.
 */
class AntRenderer : Disposable {
    private val shader = Shaders.ants()
    private val shadowShader = Shaders.shadows()
    private var capacity = 2048
    private var near = FloatArray(capacity * AntInstances.FLOATS)
    private var far = FloatArray(capacity * AntInstances.FLOATS)
    private val counts = IntArray(2)
    private val detailed = model(AntMesh.build(true))
    private val simple = model(AntMesh.build(false))
    private val disc = discMesh()

    fun draw(poses: List<AntPose>, animator: AntAnimator, sky: SkyState, camera: Camera) {
        if (poses.size > capacity) {
            capacity = poses.size * 2
            near = FloatArray(capacity * AntInstances.FLOATS)
            far = FloatArray(capacity * AntInstances.FLOATS)
            // instance buffers are re-created at the new capacity
            detailed.enableInstancedRendering(false, capacity, *instanceAttributes())
            simple.enableInstancedRendering(false, capacity, *instanceAttributes())
            disc.enableInstancedRendering(false, capacity, *instanceAttributes())
        }
        AntInstances.fill(poses, animator, camera.position.x, camera.position.y, camera.position.z, near, far, counts)
        val gl = Gdx.gl
        val strength = 0.35f * max(0f, -sky.sunDir[2]).coerceAtLeast(0.3f)
        gl.glEnable(GL20.GL_BLEND)
        gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA)
        gl.glDepthMask(false)
        shadowShader.bind()
        shadowShader.setUniformMatrix("u_projView", camera.combined)
        shadowShader.setUniformf("u_sunDir", sky.sunDir[0], sky.sunDir[1], sky.sunDir[2])
        shadowShader.setUniformf("u_strength", strength)
        drawInstances(disc, near, counts[0], shadowShader)
        drawInstances(disc, far, counts[1], shadowShader)
        gl.glDepthMask(true)
        gl.glDisable(GL20.GL_BLEND)
        shader.bind()
        Shaders.applySky(shader, sky, camera)
        drawInstances(detailed, near, counts[0], shader)
        drawInstances(simple, far, counts[1], shader)
    }

    private fun drawInstances(mesh: Mesh, data: FloatArray, count: Int, program: com.badlogic.gdx.graphics.glutils.ShaderProgram) {
        if (count == 0) return
        mesh.setInstanceData(data, 0, count * AntInstances.FLOATS)
        mesh.render(program, GL20.GL_TRIANGLES)
    }

    private fun instanceAttributes() = arrayOf(
        VertexAttribute(VertexAttributes.Usage.Generic, 4, "i_pos"),
        VertexAttribute(VertexAttributes.Usage.Generic, 4, "i_fwd"),
        VertexAttribute(VertexAttributes.Usage.Generic, 4, "i_up"),
    )

    private fun model(v: FloatArray): Mesh = Mesh(
        true, v.size / AntMesh.STRIDE, 0,
        VertexAttribute(VertexAttributes.Usage.Position, 3, "a_position"),
        VertexAttribute(VertexAttributes.Usage.Normal, 3, "a_normal"),
        VertexAttribute(VertexAttributes.Usage.Generic, 3, "a_color"),
        VertexAttribute(VertexAttributes.Usage.Generic, 1, "a_part"),
        VertexAttribute(VertexAttributes.Usage.Generic, 3, "a_pivot"),
    ).apply {
        setVertices(v)
        enableInstancedRendering(false, capacity, *instanceAttributes())
    }

    /** A soft disc, 1.8 mm across the long axis: an opaque-ish centre fading to 0 at the rim. */
    private fun discMesh(): Mesh {
        val seg = 12
        val v = ArrayList<Float>()
        for (k in 0 until seg) {
            val a0 = k * 6.2832f / seg
            val a1 = (k + 1) * 6.2832f / seg
            v += listOf(0f, 0f, 0f, 1f)
            v += listOf(cos(a0) * 2.6f, sin(a0) * 1.4f, 0f, 0f)
            v += listOf(cos(a1) * 2.6f, sin(a1) * 1.4f, 0f, 0f)
        }
        return Mesh(
            true, seg * 3, 0,
            VertexAttribute(VertexAttributes.Usage.Position, 3, "a_position"),
            VertexAttribute(VertexAttributes.Usage.Generic, 1, "a_alpha"),
        ).apply {
            setVertices(v.toFloatArray())
            enableInstancedRendering(false, capacity, *instanceAttributes())
        }
    }

    override fun dispose() {
        detailed.dispose()
        simple.dispose()
        disc.dispose()
        shader.dispose()
        shadowShader.dispose()
    }
}
```
If libGDX's `setInstanceData` has no `(data, offset, count)` overload, copy the used range into a reusable array sized for `count` records and call the real overload; do not allocate per frame. If re-enabling instancing on a mesh is not allowed, recreate the three meshes at the new capacity instead.

- [ ] **Step 3: `GrassRenderer`**

```kotlin
package com.bydesigninteractive.ant.core.render.world

import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.Mesh
import com.badlogic.gdx.graphics.VertexAttribute
import com.badlogic.gdx.graphics.VertexAttributes
import com.badlogic.gdx.utils.Disposable
import com.bydesigninteractive.ant.core.render.Shaders
import com.bydesigninteractive.ant.core.render.sky.SkyState

/** Draws every visible tuft in one instanced call: one base tuft, per-instance place and style. */
class GrassRenderer(seed: Long) : Disposable {
    private val shader = Shaders.grass()
    private var capacity = 8192
    private var data = FloatArray(capacity * GrassField.INSTANCE_FLOATS)
    private val tuft = Mesh(
        true, 27, 0,
        VertexAttribute(VertexAttributes.Usage.Position, 3, "a_position"),
        VertexAttribute(VertexAttributes.Usage.Normal, 3, "a_normal"),
        VertexAttribute(VertexAttributes.Usage.Generic, 1, "a_blade"),
        VertexAttribute(VertexAttributes.Usage.Generic, 1, "a_tip"),
    ).apply {
        setVertices(GrassField(seed).tuftMesh())
        enableInstancedRendering(false, capacity, VertexAttribute(VertexAttributes.Usage.Generic, 4, "i_place"), VertexAttribute(VertexAttributes.Usage.Generic, 4, "i_style"))
    }

    fun draw(chunks: Collection<FloatArray>, sky: SkyState, camera: Camera, time: Float) {
        var n = 0
        for (c in chunks) {
            if (n + c.size > data.size) break
            c.copyInto(data, n)
            n += c.size
        }
        if (n == 0) return
        shader.bind()
        Shaders.applySky(shader, sky, camera)
        shader.setUniformf("u_time", time)
        tuft.setInstanceData(data, 0, n)
        tuft.render(shader, GL20.GL_TRIANGLES)
    }

    override fun dispose() {
        tuft.dispose()
        shader.dispose()
    }
}
```
Grass is drawn with face culling off (blades are single triangles).

- [ ] **Step 4: Wire them into `SurfaceRenderer3D`**

Create `AntRenderer()` and `GrassRenderer(seed)` in the constructor; replace the empty `drawAnts` with `ants.draw(poses, animator, sky, camera)`; after the food draw, with culling off, call `grass.draw(slots.values.map { it.grass }, sky, camera, seconds)` (use a reusable list, not a new one per frame). Dispose both.

- [ ] **Step 5: Tests and a desktop run**

Run `./gradlew :core:test :desktop:classes :android:assembleProfile` in the background; expect BUILD SUCCESSFUL. Launch `:desktop:run` for 30 s (log to `build/task9-desktop.log`), kill only its JVM; expect no exceptions and ticks/s near 20.

- [ ] **Step 6: Commit**

```bash
git add core
git commit -m "M1b-2c: instanced ants with a shader walk cycle, shadows and grass"
```

---

### Task 10: The owner's desktop look check (controller)

- [ ] **Step 1:** Run `:desktop:run` in the background (seed 1) and ask the owner to look at the 3D view: ground facets and specks, rocks (ants on them, no clipping), stems with aphids, prey, grass clumps and the bare mound, the fused ant, its walk, carried pellets and swollen gasters, shadows, fog, and the day cycle (at 16x speed, key 3, a full day passes in about 40 s).
- [ ] **Step 2:** Collect the owner's changes. Small adjustments (colours, sizes, counts, the far-detail distance, sway, shadow strength) go to one fix subagent with the complete list; anything that changes the spec's choices is asked first. Repeat until the owner approves the look.
- [ ] **Step 3:** Record the approval and any changed values in the ledger; commit any fixes as `M1b-2c: look adjustments from the desktop check`.

---

### Task 11: The preview scenario extra and the final TV test (controller)

**Files:**
- Modify: `android/src/main/kotlin/com/bydesigninteractive/ant/PreviewActivity.kt`

- [ ] **Step 1: The scenario extra (a subagent may do this step)**

In `PreviewActivity.onCreate`, choose the world from an optional extra:
```kotlin
        val world = when (intent.getStringExtra("scenario")) {
            "colony1000" -> Scenarios.colony1000(WORLD_SEED)
            else -> Scenarios.starter(WORLD_SEED)
        }
        initialize(AntApp("activity", world, tv = true), gdxConfig())
```
Build `:android:assembleProfile` in the background; commit `M1b-2c: the preview activity takes a scenario extra`.

- [ ] **Step 2: The TV session (controller, with the owner)**

With the owner's go-ahead: connect, record `screensaver_components` and `screen_off_timeout`, install the profile APK, `appops set com.bydesigninteractive.ant AUTO_START allow`, clear logcat and start streaming `logcat -s AntFarm AndroidRuntime` to `build/tv-m1b2c-final.log` in the background. If Labyrinth is dreaming, ask the owner to dismiss it.

1. Screensaver with the starter: select the dream, set a 15 s timeout, confirm `mCurrentDream` names `AntDream`; the owner watches the 3D view about two minutes, presses OK for the nest and top-down views (a minute each), presses Back and says done.
2. Restore the screensaver settings first (so the timeout does not restart the dream), then start `adb shell am start -n com.bydesigninteractive.ant/.PreviewActivity --es scenario colony1000`; the owner watches the 3D view for about two minutes (it starts in the 3D view) and presses Back, then says done.

Stop the log stream, uninstall, and confirm the settings are restored.

- [ ] **Step 3: Record and judge**

Write `docs/superpowers/notes/2026-10-01-m1b2c-tv-final.md` (use the session's actual date in the name if it differs) with a table per run and view: fps, frame p50 and p99, frames over 25 ms per 600, ticks/s and ms per tick. Done-when item 4 needs, in the 3D view after the first 10 s, an average of at least 58 fps and at most 1% of frames over 25 ms, for both runs. If it misses, stop and bring the owner the numbers with the likely causes (from the frame breakdown and triangle counts) before any further work. Commit `M1b-2c: the final TV test`.

---

### Task 12: Close out

- [ ] **Step 1:** Update `CLAUDE.md` Status: M1b-2c done (date), the low-poly look (what is drawn and how), GL 3 and instancing, the day cycle, the final TV numbers, and that M1b is complete; next is M2 (brains replace the scripts; recalibrate, and add Gruter seeds per the owner's decision). Update `docs/design/poc-design.md` section 12: the M1b-2c row marked done with the measured numbers, and section 14 noting the look decisions (hybrid shading, fused ant, muted palette, day cycle with a readable night, patchy grass buried by the mound, weather later).
- [ ] **Step 2:** Run in the background: `./gradlew :sim:test :core:test :desktop:classes :android:assembleProfile :android:assembleDebug`; expect BUILD SUCCESSFUL.
- [ ] **Step 3:** Commit `M1b-2c: status and notes`.

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
import com.bydesigninteractive.ant.core.engine.AntPose
import com.bydesigninteractive.ant.core.engine.FoodView
import com.bydesigninteractive.ant.core.engine.Published
import com.bydesigninteractive.ant.sim.ant.Space
import com.bydesigninteractive.ant.sim.world.CHUNKS
import com.bydesigninteractive.ant.sim.world.CHUNK_MM
import com.bydesigninteractive.ant.sim.world.ChunkedField
import com.bydesigninteractive.ant.sim.world.sdf.HeightField
import kotlin.math.abs
import kotlin.math.floor

/**
 * A plain 3D view of the surface for checking the simulation: the ground as a grid mesh around
 * the camera, rocks and food lumps as spheres, stems as cylinders, ants as a gaster box, a thorax
 * box and a head sphere tilted to their normals. M1b-2 replaces it with the low-poly look.
 *
 * Ants can appear to sink into rocks here: the simulation surface is the lumpy, blended rock,
 * while this view draws smooth spheres. M1b-2 builds meshes from the same SDF, which fixes it.
 *
 * It reads only published state: rocks from [Published.rocks] and [Published.placed], foods and
 * ants from the caller. The ground is its own [HeightField] from the seed (the base relief is a
 * pure function of the seed), with a spoil field mirrored from the published overlays, since the
 * simulation's height field is not thread-safe. The ground mesh is rebuilt when the published
 * spoil near the camera changes.
 */
class DebugSurfaceRenderer(seed: Long, private val published: Published) : Disposable {
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
    private val spoil = ChunkedField()
    private val ground = HeightField(seed, spoil)
    private val spoilStamps = LongArray(CHUNKS * CHUNKS) { -1L }
    private var groundDirty = false
    private val models = arrayOf(sphere, cylinder, box)
    private val pools = Array(models.size) { ArrayList<ModelInstance>() }
    private val used = IntArray(models.size)
    private var groundModel: Model? = null
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

    /** The ground height in mm at (x, y) as drawn, spoil included. Render thread only. */
    fun groundHeight(x: Float, y: Float): Float = ground.height(x, y)

    fun draw(chase: ChaseCamera3, poses: List<AntPose>, foods: List<FoodView>) {
        camera.position.set(chase.eyeX, chase.eyeZ, -chase.eyeY)
        camera.up.set(chase.upX, chase.upZ, -chase.upY)
        camera.lookAt(chase.targetX, chase.targetZ, -chase.targetY)
        camera.update()
        val fx = chase.targetX
        val fy = chase.targetY
        val ccx = floor(fx / CHUNK_MM).toInt()
        val ccy = floor(fy / CHUNK_MM).toInt()
        syncSpoil(ccx, ccy)
        if (groundDirty || groundX.isNaN() || abs(fx - groundX) > REBUILD_MM || abs(fy - groundY) > REBUILD_MM) rebuildGround(fx, fy)
        used.fill(0)
        batch.begin(camera)
        groundInstance?.let { batch.render(it, environment) }
        for (cy in ccy - RING..ccy + RING) for (cx in ccx - RING..ccx + RING) {
            if (cx < 0 || cy < 0 || cx >= CHUNKS || cy >= CHUNKS) continue
            val rocks = published.rocks[cx + cy * CHUNKS] ?: continue
            for (k in rocks.indices) {
                val b = rocks[k]
                blob(b.cx, b.cy, b.cz, b.rx, b.ry, b.rz, ROCK)
            }
        }
        val placed = published.placed
        for (k in placed.indices) {
            val b = placed[k]
            blob(b.cx, b.cy, b.cz, b.rx, b.ry, b.rz, ROCK)
        }
        for (k in foods.indices) {
            val f = foods[k]
            if (f.bodyRadius > 0f) blob(f.x, f.y, f.z, f.bodyRadius, f.bodyRadius, f.bodyRadius, FOOD)
            if (f.stemRadius > 0f) {
                val i = next(CYLINDER, STEM)
                i.transform.setToTranslationAndScaling(f.x, (f.stemBase + f.stemTop) / 2f, -f.y, f.stemRadius, f.stemTop - f.stemBase, f.stemRadius)
                batch.render(i, environment)
            }
        }
        for (k in poses.indices) {
            val a = poses[k]
            if (a.space != Space.SURFACE) continue
            if (abs(a.x - fx) > VIEW_MM || abs(a.y - fy) > VIEW_MM) continue
            xAxis.set(a.fx, a.fz, -a.fy)
            yAxis.set(a.nx, a.nz, -a.ny)
            zAxis.set(xAxis).crs(yAxis)
            antPart(BOX, ANT, a, -1.6f, 0.8f, 2.4f, 1.6f, 2.2f)
            antPart(BOX, ANT, a, 0f, 0.6f, 1.8f, 1.2f, 1.2f)
            antPart(SPHERE, HEAD, a, 1.8f, 0.8f, 0.8f, 0.8f, 0.8f)
        }
        batch.end()
    }

    /**
     * Draws one ant part along the basis set up in [xAxis], [yAxis] and [zAxis]: [along] mm ahead
     * of the ant position, lifted [lift] mm along the normal, scaled by the given sizes (the
     * sphere model has radius 1, so pass the radius for it).
     */
    private fun antPart(model: Int, color: ColorAttribute, a: AntPose, along: Float, lift: Float, sx: Float, sy: Float, sz: Float) {
        val i = next(model, color)
        origin.set(
            a.x + a.fx * along + a.nx * lift,
            a.z + a.fz * along + a.nz * lift,
            -(a.y + a.fy * along + a.ny * lift),
        )
        i.transform.set(xAxis, yAxis, zAxis, origin)
        i.transform.scale(sx, sy, sz)
        batch.render(i, environment)
    }

    private fun blob(x: Float, y: Float, z: Float, rx: Float, ry: Float, rz: Float, color: ColorAttribute) {
        val i = next(SPHERE, color)
        i.transform.setToTranslationAndScaling(x, z, -y, rx, rz, ry)
        batch.render(i, environment)
    }

    /** The next pooled instance of model [model] (an index into [models]), set to [color]. */
    private fun next(model: Int, color: ColorAttribute): ModelInstance {
        val pool = pools[model]
        val n = used[model]
        val instance = if (n < pool.size) pool[n] else ModelInstance(models[model]).also { pool += it }
        used[model] = n + 1
        instance.materials[0].set(color)
        return instance
    }

    /**
     * Copies the published spoil of the chunks around the camera chunk ([ccx], [ccy]) into this
     * renderer's own spoil field whenever a chunk's overlay stamp changes, and marks the ground
     * mesh stale if a chunk's values really changed. Published arrays are immutable, so they are
     * shared, not copied.
     */
    private fun syncSpoil(ccx: Int, ccy: Int) {
        for (cy in ccy - RING..ccy + RING) for (cx in ccx - RING..ccx + RING) {
            if (cx < 0 || cy < 0 || cx >= CHUNKS || cy >= CHUNKS) continue
            val key = cx + cy * CHUNKS
            val chunk = published.overlays[key] ?: continue
            if (spoilStamps[key] == chunk.stamp) continue
            spoilStamps[key] = chunk.stamp
            val fresh = chunk.spoil
            val old = spoil.chunk(cx, cy)
            if (fresh == null && old == null) continue
            if (fresh != null && old != null && fresh.contentEquals(old)) continue
            spoil.setChunk(cx, cy, fresh)
            groundDirty = true
        }
    }

    /** A grid of the ground heights, 2 m across at 20 mm spacing, centered on (cx, cy). */
    private fun rebuildGround(cx: Float, cy: Float) {
        groundModel?.dispose()
        val g = ground
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
        groundModel = model
        groundInstance = ModelInstance(model)
        groundX = cx
        groundY = cy
        groundDirty = false
    }

    override fun dispose() {
        groundModel?.dispose()
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
        const val SPHERE = 0
        const val CYLINDER = 1
        const val BOX = 2
        val GROUND = Color(0.40f, 0.30f, 0.20f, 1f)

        // One shared diffuse attribute per color, assigned to pooled instances in next().
        val ROCK: ColorAttribute = ColorAttribute.createDiffuse(Color(0.52f, 0.51f, 0.48f, 1f))
        val FOOD: ColorAttribute = ColorAttribute.createDiffuse(Color(0.80f, 0.55f, 0.20f, 1f))
        val STEM: ColorAttribute = ColorAttribute.createDiffuse(Color(0.30f, 0.50f, 0.18f, 1f))
        val ANT: ColorAttribute = ColorAttribute.createDiffuse(Color(0.08f, 0.06f, 0.05f, 1f))
        val HEAD: ColorAttribute = ColorAttribute.createDiffuse(Color(0.25f, 0.12f, 0.08f, 1f))
    }
}

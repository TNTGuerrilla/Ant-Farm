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

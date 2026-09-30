package com.bydesigninteractive.ant.core.render

import com.badlogic.gdx.graphics.PerspectiveCamera
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.graphics.g3d.decals.CameraGroupStrategy
import com.badlogic.gdx.graphics.g3d.decals.Decal
import com.badlogic.gdx.graphics.g3d.decals.DecalBatch
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.utils.Disposable
import com.bydesigninteractive.ant.core.camera.ChaseCamera
import com.bydesigninteractive.ant.sim.World
import com.bydesigninteractive.ant.sim.ant.Space
import com.bydesigninteractive.ant.sim.world.CHUNKS
import com.bydesigninteractive.ant.sim.world.CHUNK_MM
import com.bydesigninteractive.ant.sim.world.FoodKind
import kotlin.math.atan2

/**
 * The 2.5D surface: a perspective camera behind and above the followed ant, the ground as flat
 * chunk decals, grass tufts and aphid plants as upright billboards, and ants as flat sprites on
 * the ground. Placeholder art; the look gets real attention later.
 */
class Surface3dRenderer(
    private val world: World,
    private val chunks: ChunkTextures,
    ant: Texture,
    tuft: Texture,
    plant: Texture,
) : Disposable {
    val camera = PerspectiveCamera(FOV, 1f, 1f).apply {
        near = 2f
        far = VIEW_MM * 1.5f
    }
    private val batch = DecalBatch(CameraGroupStrategy(camera))
    private val antRegion = TextureRegion(ant)
    private val tuftRegion = TextureRegion(tuft)
    private val plantRegion = TextureRegion(plant)
    private val grounds = HashMap<Int, Decal>()
    private val pool = ArrayList<Decal>()
    private var used = 0

    fun resize(w: Int, h: Int) {
        camera.viewportWidth = w.toFloat()
        camera.viewportHeight = h.toFloat()
    }

    fun draw(chase: ChaseCamera) {
        camera.position.set(chase.x, chase.height, -chase.y)
        camera.up.set(Vector3.Y)
        camera.lookAt(chase.targetX, 0f, -chase.targetY)
        camera.up.set(Vector3.Y)
        camera.update()
        used = 0
        val fx = chase.focusX
        val fy = chase.focusY
        val ccx = (fx / CHUNK_MM).toInt()
        val ccy = (fy / CHUNK_MM).toInt()
        for (cy in ccy - RING..ccy + RING) for (cx in ccx - RING..ccx + RING) {
            if (cx < 0 || cy < 0 || cx >= CHUNKS || cy >= CHUNKS) continue
            ground(cx, cy)?.let(batch::add)
            for (t in world.surface.chunk(cx, cy).tufts) {
                if (!near(t.x, t.y, fx, fy)) continue
                billboard(tuftRegion, t.x, t.y, t.height * 0.6f, t.height)
            }
        }
        for (f in world.surface.foods) {
            if (!near(f.x, f.y, fx, fy)) continue
            if (f.kind == FoodKind.HONEYDEW) {
                billboard(plantRegion, f.x, f.y, PLANT_W, PLANT_H)
            } else {
                flat(antRegion, f.x, f.y, f.radius * 2, f.radius, 0f).setColor(0.8f, 0.45f, 0.2f, 1f)
            }
        }
        for (a in world.ants) {
            if (a.space != Space.SURFACE || !near(a.x, a.y, fx, fy)) continue
            flat(antRegion, a.x, a.y, 5f, 2.5f, a.heading * MathUtils.radiansToDegrees)
        }
        batch.flush()
        chunks.evict(ccx, ccy, RING + 1)
        val stale = grounds.keys.filter { key -> kotlin.math.abs(key % CHUNKS - ccx) > RING + 1 || kotlin.math.abs(key / CHUNKS - ccy) > RING + 1 }
        stale.forEach { grounds.remove(it) }
    }

    private fun near(x: Float, y: Float, fx: Float, fy: Float): Boolean {
        val dx = x - fx
        val dy = y - fy
        return dx * dx + dy * dy < VIEW_MM * VIEW_MM
    }

    /** The chunk's ground as a flat, opaque decal; null until its texture is built. */
    private fun ground(cx: Int, cy: Int): Decal? {
        val key = cx + cy * CHUNKS
        val tex = chunks.get(cx, cy) ?: return null
        // A cached decal is only valid while it still holds the live chunk texture.
        grounds[key]?.let { if (it.textureRegion.texture === tex) return it }
        val d = Decal.newDecal(CHUNK_MM.toFloat(), CHUNK_MM.toFloat(), TextureRegion(tex), false)
        d.setRotation(0f, -90f, 0f)
        d.setPosition(cx * CHUNK_MM + CHUNK_MM / 2f, 0f, -(cy * CHUNK_MM + CHUNK_MM / 2f))
        grounds[key] = d
        return d
    }

    /** An upright sprite standing at (x, y), turned to face the camera about the vertical axis. */
    private fun billboard(region: TextureRegion, x: Float, y: Float, w: Float, h: Float) {
        val d = next(region, w, h)
        d.setPosition(x, h / 2f, -y)
        val dx = camera.position.x - x
        val dz = camera.position.z + y
        d.setRotation(atan2(dx, dz) * MathUtils.radiansToDegrees, 0f, 0f)
        batch.add(d)
    }

    /** A sprite lying flat on the ground at (x, y), rotated to [headingDeg]. */
    private fun flat(region: TextureRegion, x: Float, y: Float, w: Float, h: Float, headingDeg: Float): Decal {
        val d = next(region, w, h)
        d.setPosition(x, 0.3f, -y)
        d.setRotation(0f, -90f, headingDeg)
        batch.add(d)
        return d
    }

    private fun next(region: TextureRegion, w: Float, h: Float): Decal {
        val d = if (used < pool.size) {
            pool[used].also { it.textureRegion = region }
        } else {
            Decal.newDecal(w, h, region, true).also { pool += it }
        }
        used++
        d.setDimensions(w, h)
        d.setColor(1f, 1f, 1f, 1f)
        return d
    }

    override fun dispose() {
        batch.dispose()
    }

    private companion object {
        const val FOV = 60f
        const val VIEW_MM = 1500f
        const val RING = 3
        const val PLANT_W = 80f
        const val PLANT_H = 600f
    }
}

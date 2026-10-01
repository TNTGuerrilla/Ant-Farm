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

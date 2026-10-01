package com.bydesigninteractive.ant.core.render

import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.graphics.glutils.ShaderProgram
import com.bydesigninteractive.ant.core.render.sky.SkyState

/**
 * The GLSL for the low-poly look, in the legacy style that both the desktop launcher's GL 3.3 core
 * prepend and GLES 3 accept. All world shaders share one light model: a directional light plus
 * ambient, then linear fog toward the fog colour by distance from the eye. Positions are world
 * millimetres.
 *
 * `u_fogColor` is declared only in the fragment shader: GLSL ES 1.00 requires a uniform declared in
 * both stages to have the same precision, and the stages' default float precisions differ.
 */
object Shaders {
    /**
     * Scales the day cycle's sun and ambient light before shading. Midday ambient plus sun is about
     * 1.45 on flat ground; times 0.7 that is about 1, so lit ground shows its palette colour.
     */
    const val EXPOSURE = 0.7f

    private const val PRECISION = "#ifdef GL_ES\nprecision mediump float;\n#endif\n"

    private const val LIGHT = """
uniform vec3 u_sunDir;
uniform vec3 u_sunColor;
uniform vec3 u_ambient;
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

    // Ants: model space x forward, y left, z up; instances carry position + gait phase, forward + gaster fill, up + carry, and model scale.
    private const val ANT_VERT = """
attribute vec3 a_position;
attribute vec3 a_normal;
attribute vec3 a_color;
attribute float a_part;
attribute vec3 a_pivot;
attribute vec4 i_pos;
attribute vec4 i_fwd;
attribute vec4 i_up;
attribute float i_scale;
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
        // Yaw by angle t about z moves a foot at offset d from its pivot by -t * d.y along x (small t).
        // A left foot has d.y > 0 and a right foot d.y < 0, so the sign of the swing must follow the
        // side: with swing = -0.35 * s * sign(pivot.y), the foot's x velocity is proportional to
        // 0.35 * cos(TAU * (phase + group)) for both sides. The lift below happens while that cosine
        // is positive, so the foot is raised exactly while it moves toward +x (the forward stroke).
        float swing = -0.35 * s * sign(a_pivot.y);
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
    vec3 l = normalize(cross(u, f));
    p *= i_scale;
    vec3 world = i_pos.xyz + f * p.x + l * p.y + u * p.z;
    vec3 wn = normalize(f * n.x + l * n.y + u * n.z);
    v_color = lit(a_color, wn);
    v_fog = fogAmount(world);
    gl_Position = u_projView * vec4(world, 1.0);
}
"""

    // Shadows: a disc under each ant, scaled with it, pushed away from the sun, darker by day.
    private const val SHADOW_VERT = """
attribute vec3 a_position;
attribute float a_alpha;
attribute vec4 i_pos;
attribute vec4 i_fwd;
attribute vec4 i_up;
attribute float i_scale;
uniform mat4 u_projView;
uniform vec3 u_sunDir;
uniform float u_strength;
varying float v_alpha;
void main() {
    vec3 f = normalize(i_fwd.xyz);
    vec3 u = normalize(i_up.xyz);
    vec3 l = normalize(cross(u, f));
    vec3 offset = (u_sunDir - u * dot(u_sunDir, u)) * 0.8 * i_scale;
    vec3 world = i_pos.xyz + offset + (f * a_position.x + l * a_position.y) * i_scale + u * 0.2;
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

    /** Sets the light (scaled by [EXPOSURE]), fog and camera uniforms every world shader shares. The program must be bound. */
    fun applySky(p: ShaderProgram, sky: SkyState, camera: Camera) {
        p.setUniformMatrix("u_projView", camera.combined)
        p.setUniformf("u_sunDir", sky.sunDir[0], sky.sunDir[1], sky.sunDir[2])
        p.setUniformf("u_sunColor", sky.sunColor[0] * EXPOSURE, sky.sunColor[1] * EXPOSURE, sky.sunColor[2] * EXPOSURE)
        p.setUniformf("u_ambient", sky.ambient[0] * EXPOSURE, sky.ambient[1] * EXPOSURE, sky.ambient[2] * EXPOSURE)
        p.setUniformf("u_fogColor", sky.fogColor[0], sky.fogColor[1], sky.fogColor[2])
        p.setUniformf("u_fogStart", sky.fogStart)
        p.setUniformf("u_fogEnd", sky.fogEnd)
        p.setUniformf("u_eye", camera.position.x, camera.position.y, camera.position.z)
    }

    private fun compile(vert: String, frag: String): ShaderProgram {
        ShaderProgram.pedantic = false
        val p = ShaderProgram(vert, frag)
        if (!p.isCompiled) {
            val log = p.log
            p.dispose()
            error("shader failed to compile: $log")
        }
        return p
    }
}

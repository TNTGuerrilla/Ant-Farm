// Rendering, camera, overlay and input mapping, shared by the desktop and TV builds.
plugins {
    id("org.jetbrains.kotlin.jvm")
}

val gdxVersion: String by project

kotlin {
    jvmToolchain(17)
}

dependencies {
    api(project(":sim"))
    api("com.badlogicgames.gdx:gdx:$gdxVersion")
    testImplementation(kotlin("test"))
    // Camera.update calls libGDX's native matrix code; tests that build a camera load it with GdxNativesLoader.
    testImplementation("com.badlogicgames.gdx:gdx-platform:$gdxVersion:natives-desktop")
}

tasks.test {
    useJUnitPlatform()
}

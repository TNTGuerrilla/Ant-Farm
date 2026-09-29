// The Windows build: an LWJGL3 window for development. Packaging (jpackage .exe/.scr) comes later.
plugins {
    id("org.jetbrains.kotlin.jvm")
    application
}

val gdxVersion: String by project

kotlin {
    jvmToolchain(17)
}

application {
    mainClass.set("com.bydesigninteractive.ant.desktop.DesktopLauncherKt")
}

dependencies {
    implementation(project(":core"))
    implementation("com.badlogicgames.gdx:gdx-backend-lwjgl3:$gdxVersion")
    implementation("com.badlogicgames.gdx:gdx-platform:$gdxVersion:natives-desktop")
}

tasks.named<JavaExec>("run") {
    workingDir = rootProject.file("desktop/run").apply { mkdirs() }
}

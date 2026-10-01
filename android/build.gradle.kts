// The TV build: the libGDX AndroidDaydream screensaver plus a launcher activity that runs the
// same app in the foreground.
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

val gdxVersion: String by project

// The TCL QM6K runs a 32-bit ARM userspace (armeabi-v7a only); x86 and x86_64 are for emulators.
val abis = listOf("armeabi-v7a", "arm64-v8a", "x86", "x86_64")
val gdxNatives = abis.associateWith { abi -> configurations.create("gdxNatives-$abi") }

android {
    namespace = "com.bydesigninteractive.ant"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.bydesigninteractive.ant"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "0.0.1"
    }

    sourceSets["main"].jniLibs.srcDir(layout.buildDirectory.dir("gdx-natives"))

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildTypes {
        // Not debuggable and signed with the debug key: what the TV runs and what is measured
        // there, until release signing arrives (M8). Debuggable builds run several times slower.
        create("profile") {
            initWith(getByName("debug"))
            isDebuggable = false
            isJniDebuggable = false
            isMinifyEnabled = false
            matchingFallbacks += listOf("release")
        }
    }
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation(project(":core"))
    implementation("com.badlogicgames.gdx:gdx-backend-android:$gdxVersion")
    gdxNatives.forEach { (abi, conf) ->
        add(conf.name, "com.badlogicgames.gdx:gdx-platform:$gdxVersion:natives-$abi")
    }
}

// Each natives jar holds libgdx.so at its root; unpack it into the folder named for its ABI.
val copyGdxNatives by tasks.registering(Sync::class) {
    into(layout.buildDirectory.dir("gdx-natives"))
    gdxNatives.forEach { (abi, conf) ->
        from(conf.elements.map { files -> files.map { zipTree(it.asFile) } }) {
            include("*.so")
            into(abi)
        }
    }
}

tasks.matching { it.name.contains("JniLibFolders") }.configureEach {
    dependsOn(copyGdxNatives)
}

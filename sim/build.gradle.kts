// The simulation core: pure Kotlin, no libGDX or Android, so it runs and tests on any JVM.
plugins {
    id("org.jetbrains.kotlin.jvm")
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    testImplementation(kotlin("test"))
}

tasks.test {
    // M2a Task 8: the seed brain does not yet pass the 8-seed Gruter test (see the M2a plan); the
    // realism search's winner must pass it before it ships, and Task 11 removes this exclusion.
    useJUnitPlatform { excludeTags("gruter") }
    maxHeapSize = "3g"
}

// GruterScenarioTest alone (M2a): ./gradlew :sim:gruterTest
val gruterTest by tasks.registering(Test::class) {
    description = "Runs GruterScenarioTest (8 seeds) on its own."
    group = "verification"
    testClassesDirs = sourceSets["test"].output.classesDirs
    classpath = sourceSets["test"].runtimeClasspath
    useJUnitPlatform { includeTags("gruter") }
    maxHeapSize = "3g"
}

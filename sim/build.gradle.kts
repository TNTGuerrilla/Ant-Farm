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
    // GruterScenarioTest (8 seeds) is part of the suite again since M2a Task 11a, when the seed
    // brain passed all four checks; :sim:gruterTest still runs it on its own.
    useJUnitPlatform()
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

// The M2a realism search: ./gradlew :sim:search -PsearchArgs="--generations 8 --workers 14"
// scripts/search.ps1 runs the same main class with the JVM held to the P-cores.
tasks.register<JavaExec>("search") {
    group = "application"
    description = "Runs the M2a realism search (report and genomes in build/search)."
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("com.bydesigninteractive.ant.sim.search.SearchMainKt")
    workingDir = rootProject.projectDir
    maxHeapSize = "6g"
    val extra = (project.findProperty("searchArgs") as String?)?.trim().orEmpty()
    if (extra.isNotEmpty()) args(extra.split(Regex("\\s+")))
}

// Writes the search's runtime classpath for scripts/search.ps1, which starts the JVM itself so it can set the affinity.
tasks.register("searchClasspath") {
    description = "Writes the search's runtime classpath to sim/build/search-classpath.txt."
    val cp = sourceSets["main"].runtimeClasspath
    val out = layout.buildDirectory.file("search-classpath.txt")
    dependsOn(tasks.named("classes"))
    inputs.files(cp)
    outputs.file(out)
    doLast { out.get().asFile.writeText(cp.asPath) }
}

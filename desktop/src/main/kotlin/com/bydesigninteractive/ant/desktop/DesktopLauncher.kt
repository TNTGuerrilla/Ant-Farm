package com.bydesigninteractive.ant.desktop

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration
import com.bydesigninteractive.ant.core.AntApp
import com.bydesigninteractive.ant.core.stub.StubApp
import com.bydesigninteractive.ant.sim.scenario.Scenarios

/** Runs the M1 app; `--stub` runs the M0 stub instead, and `--seed=N` picks the world seed. */
fun main(args: Array<String>) {
    val config = Lwjgl3ApplicationConfiguration().apply {
        setTitle("Ant Farm")
        setWindowedMode(1600, 900)
        useVsync(true)
        setForegroundFPS(0)
    }
    val seed = args.firstOrNull { it.startsWith("--seed=") }?.substringAfter('=')?.toLongOrNull() ?: 1L
    val app = if ("--stub" in args) StubApp("desktop") else AntApp("desktop", Scenarios.starter(seed), tv = false)
    Lwjgl3Application(app, config)
}

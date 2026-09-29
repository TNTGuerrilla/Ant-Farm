package com.bydesigninteractive.ant.desktop

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration
import com.bydesigninteractive.ant.core.stub.StubApp

fun main() {
    val config = Lwjgl3ApplicationConfiguration().apply {
        setTitle("Ant Farm")
        setWindowedMode(1600, 900)
        useVsync(true)
        setForegroundFPS(0)
    }
    Lwjgl3Application(StubApp("desktop"), config)
}

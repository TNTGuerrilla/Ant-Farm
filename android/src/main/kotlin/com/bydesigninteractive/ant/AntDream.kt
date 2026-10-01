package com.bydesigninteractive.ant

import android.util.Log
import android.view.KeyEvent
import com.badlogic.gdx.backends.android.AndroidApplicationConfiguration
import com.badlogic.gdx.backends.android.AndroidDaydream
import com.bydesigninteractive.ant.core.APP_LOG_TAG
import com.bydesigninteractive.ant.core.AntApp
import com.bydesigninteractive.ant.sim.scenario.Scenarios

/**
 * The screensaver. It is interactive, so the remote's keys reach the app instead of ending the
 * dream; Back still ends it (DreamService handles that itself) and Home leaves as usual.
 */
class AntDream : AndroidDaydream() {
    private var app: AntApp? = null

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        isInteractive = true
        isScreenBright = true
        val created = AntApp("dream", Scenarios.starter(WORLD_SEED), tv = true)
        app = created
        initialize(created, gdxConfig())
    }

    /** A dream cancelled between attach and start never gets dispose, so stop the simulation here too. */
    override fun onDetachedFromWindow() {
        app?.shutdown()
        super.onDetachedFromWindow()
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        Log.i(APP_LOG_TAG, "window ${describe(event)}")
        return super.dispatchKeyEvent(event)
    }
}

/** Until persistence arrives (M7), every run starts the same young colony. */
internal const val WORLD_SEED = 1L

internal fun gdxConfig() = AndroidApplicationConfiguration().apply {
    useAccelerometer = false
    useCompass = false
    useGL30 = true
    stencil = 8 // the 3D view draws the nest entrance's dip through the stencil buffer
}

internal fun describe(event: KeyEvent): String {
    val action = if (event.action == KeyEvent.ACTION_DOWN) "down" else "up  "
    val repeat = if (event.repeatCount > 0) " repeat ${event.repeatCount}" else ""
    return "$action ${KeyEvent.keyCodeToString(event.keyCode)} (${event.keyCode})$repeat"
}

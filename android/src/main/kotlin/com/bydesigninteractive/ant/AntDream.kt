package com.bydesigninteractive.ant

import android.util.Log
import android.view.KeyEvent
import com.badlogic.gdx.backends.android.AndroidApplicationConfiguration
import com.badlogic.gdx.backends.android.AndroidDaydream
import com.bydesigninteractive.ant.core.stub.KeyLog
import com.bydesigninteractive.ant.core.stub.LOG_TAG
import com.bydesigninteractive.ant.core.stub.StubApp

/**
 * The screensaver. It is interactive, so the remote's keys reach the app instead of ending
 * the dream; Back still ends it (DreamService handles that itself) and Home leaves as usual.
 */
class AntDream : AndroidDaydream() {
    private var rawKeys: KeyLog? = null

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        isInteractive = true
        isScreenBright = true
        val keys = KeyLog()
        rawKeys = keys
        initialize(StubApp("dream", keys), gdxConfig())
    }

    /** Logs every key the firmware delivers to the dream's window, before libGDX sees it. */
    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        rawKeys?.add(describe(event))
        Log.i(LOG_TAG, "window ${describe(event)}")
        return super.dispatchKeyEvent(event)
    }
}

internal fun gdxConfig() = AndroidApplicationConfiguration().apply {
    useAccelerometer = false
    useCompass = false
}

internal fun describe(event: KeyEvent): String {
    val action = if (event.action == KeyEvent.ACTION_DOWN) "down" else "up  "
    val repeat = if (event.repeatCount > 0) " repeat ${event.repeatCount}" else ""
    return "$action ${KeyEvent.keyCodeToString(event.keyCode)} (${event.keyCode})$repeat"
}

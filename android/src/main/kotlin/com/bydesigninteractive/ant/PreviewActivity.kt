package com.bydesigninteractive.ant

import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import com.badlogic.gdx.backends.android.AndroidApplication
import com.bydesigninteractive.ant.core.stub.KeyLog
import com.bydesigninteractive.ant.core.stub.LOG_TAG
import com.bydesigninteractive.ant.core.stub.StubApp

/** The launcher entry: the same app as the dream, as a normal foreground activity. */
class PreviewActivity : AndroidApplication() {
    private val rawKeys = KeyLog()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initialize(StubApp("activity", rawKeys), gdxConfig())
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        rawKeys.add(describe(event))
        Log.i(LOG_TAG, "window ${describe(event)}")
        return super.dispatchKeyEvent(event)
    }
}

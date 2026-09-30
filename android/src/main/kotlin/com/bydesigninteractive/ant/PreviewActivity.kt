package com.bydesigninteractive.ant

import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import com.badlogic.gdx.backends.android.AndroidApplication
import com.bydesigninteractive.ant.core.APP_LOG_TAG
import com.bydesigninteractive.ant.core.AntApp
import com.bydesigninteractive.ant.sim.scenario.Scenarios

/** The launcher entry: the same app as the dream, as a normal foreground activity. */
class PreviewActivity : AndroidApplication() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initialize(AntApp("activity", Scenarios.starter(WORLD_SEED), tv = true), gdxConfig())
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        Log.i(APP_LOG_TAG, "window ${describe(event)}")
        return super.dispatchKeyEvent(event)
    }
}

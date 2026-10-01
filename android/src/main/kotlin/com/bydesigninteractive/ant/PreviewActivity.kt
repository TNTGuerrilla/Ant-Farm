package com.bydesigninteractive.ant

import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import com.badlogic.gdx.backends.android.AndroidApplication
import com.bydesigninteractive.ant.core.APP_LOG_TAG
import com.bydesigninteractive.ant.core.AntApp
import com.bydesigninteractive.ant.sim.scenario.Scenarios

/**
 * The launcher entry: the same app as the dream, as a normal foreground activity. Started over adb
 * with `--es scenario colony1000` it runs the 1,000-ant benchmark colony instead of the starter, and `--es layers ants,rocks` skips those 3D layers (ants, shadows, rocks,
 * ground, grass, food, sky) for profiling.
 */
class PreviewActivity : AndroidApplication() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val world = when (intent.getStringExtra("scenario")) {
            "colony1000" -> Scenarios.colony1000(WORLD_SEED)
            else -> Scenarios.starter(WORLD_SEED)
        }
        val skip = intent.getStringExtra("layers")?.split(',')?.map { it.trim() }?.filter { it.isNotEmpty() }?.toSet() ?: emptySet()
        initialize(AntApp("activity", world, tv = true, skipLayers = skip), gdxConfig())
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        Log.i(APP_LOG_TAG, "window ${describe(event)}")
        return super.dispatchKeyEvent(event)
    }
}

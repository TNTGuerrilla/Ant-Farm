package com.bydesigninteractive.ant

import android.app.Activity
import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import android.widget.TextView
import com.bydesigninteractive.ant.core.APP_LOG_TAG
import com.bydesigninteractive.ant.core.engine.Benchmark
import com.bydesigninteractive.ant.sim.DT
import com.bydesigninteractive.ant.sim.scenario.Scenarios
import kotlin.concurrent.thread
import kotlin.math.roundToInt

/**
 * A headless simulation benchmark for the TV, started over adb (it has no launcher entry):
 *
 *     adb shell am start -n com.bydesigninteractive.ant/.BenchmarkActivity \
 *         --es scenario colony1000 --es mode cost --ei warmup 300 --ei measure 60
 *
 * scenario is starter or colony1000. In cost mode it steps the world as fast as it can, warmup
 * and measure being simulated seconds; in speed mode it runs the world through the simulation
 * thread at --ef speed for measure real seconds. Results are logged as BENCH lines under the
 * AntFarm tag, and the activity closes itself when done.
 */
class BenchmarkActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        setContentView(TextView(this).apply {
            textSize = 32f
            text = "Ant Farm: benchmarking"
        })
        val scenario = intent.getStringExtra("scenario") ?: "colony1000"
        val mode = intent.getStringExtra("mode") ?: "cost"
        val warmup = intent.getIntExtra("warmup", 300)
        val measure = intent.getIntExtra("measure", 60)
        val speed = intent.getFloatExtra("speed", 4f)
        val seed = intent.getLongExtra("seed", WORLD_SEED)
        thread(name = "benchmark") {
            try {
                Log.i(APP_LOG_TAG, "BENCH start scenario $scenario mode $mode warmup $warmup measure $measure speed $speed seed $seed")
                val world = when (scenario) {
                    "starter" -> Scenarios.starter(seed)
                    "colony1000" -> Scenarios.colony1000(seed)
                    else -> error("unknown scenario $scenario")
                }
                val line = when (mode) {
                    "cost" -> Benchmark.cost(world, (warmup / DT).roundToInt(), (measure / DT).roundToInt()).line
                    "speed" -> Benchmark.speed(world, speed, measure.toFloat()).line
                    else -> error("unknown mode $mode")
                }
                Log.i(APP_LOG_TAG, line)
            } catch (t: Throwable) {
                Log.e(APP_LOG_TAG, "BENCH failed", t)
            }
            Log.i(APP_LOG_TAG, "BENCH done")
            runOnUiThread { finish() }
        }
    }
}

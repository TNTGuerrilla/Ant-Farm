# Ant Farm

Ant Farm (a placeholder name) is a passive, realistic simulation of a wild Lasius niger colony, shipped as a Google TV screensaver with a Windows desktop build. See `docs/design/poc-design.md` for the design.

## Modules

- `sim` - the pure Kotlin simulation core (no libGDX, no Android).
- `core` - libGDX rendering, overlay and input, shared by both platforms.
- `desktop` - the LWJGL3 window for Windows.
- `android` - the `AndroidDaydream` screensaver and a launcher activity.

## Building

Gradle needs JDK 17. If `JAVA_HOME` points at an older JDK, point it at a JDK 17 install for the session first.

- Tests: `.\gradlew.bat :sim:test :core:test`
- Desktop: `.\gradlew.bat :desktop:run`
- TV APK: `.\gradlew.bat :android:assembleProfile` (output in `android\build\outputs\apk\profile\`; not debuggable, which is what the TV runs and what is measured there)

## The app (milestone M1, desktop)

The desktop app runs a young colony: scripted diggers extend the nest and scripted foragers search, feed and lay trails. There are three views:

- **Nest:** the cross-section. The camera frames the dug part and zooms out as it grows, but never so far that an ant is shorter than 8 px on a 1080p screen.
- **Surface, top:** the ground from above, with spoil and trail pheromone overlays.
- **Surface, 3D debug:** ground mesh, rocks, stems and ants as simple shapes; the low-poly look arrives in M1b-2.

Keys: Tab cycles views, Space pauses, 1/2/3 set speed 1x/4x/16x, WASD or arrows pan, +/- or the mouse wheel zoom, F follows the next forager, P toggles the trail overlay; in the 3D debug view the arrows pick the ant. `--seed=N` picks the world, and `--stub` runs the M0 stub (`.\gradlew.bat :desktop:run --args="--stub"`).

## Realism search (M2a)

The instinct brain is refined by a realism search that ranks brain variants against field observations and classic experiments. On Windows run it on the performance cores, at below-normal priority, with:

```powershell
.\scripts\search.ps1
```

It takes up to an hour with 14 workers on a 16-thread P-core CPU and writes `build/search/report.md`, the best genome of each generation, and `winner.genome` when a variant passes the Gruter test. `-DryRun` only prints the detected P-core threads. Without the script, `.\gradlew.bat :sim:search -PsearchArgs="--workers 6"` runs the same search with no affinity.

## On the TV

The TV dream and the launcher activity run the app (milestone M1b-2a). It starts in the 3D debug view; OK cycles the views and Back ends the dream. The simulation runs on its own thread at 20 ticks per second, and the log (tag `AntFarm`) prints ticks per second, milliseconds per tick and frame times every 10 s.

## Milestone M0: the TV stub

The M0 stub (`--stub` on desktop) checked a TV before real work:

1. Remote keys reach an interactive dream. The overlay lists keys twice: as the dream's window received them, and as libGDX delivered them.
2. 1,000 moving ant sprites hold the frame rate. Up doubles the sprite count and Down halves it, so the headroom can be found.
3. A 20 MB background save causes no visible hitch. The first write runs 10 s after start and then every 30 s; Center starts one now. The overlay shows the write time and the frames rendered while it ran.

To try the app on a Google TV, connect adb to the TV (network debugging), then install it, note the current screensaver so it can be restored, and select this one:

```powershell
$adb = "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe"
& $adb install -r android\build\outputs\apk\profile\android-profile.apk
& $adb shell settings get secure screensaver_components
& $adb shell settings put secure screensaver_components com.bydesigninteractive.ant/.AntDream
& $adb logcat -s AntFarm
```

Some TV firmware (TCL, for one) refuses to start a third-party dream until the app is allowed to auto-start:

```powershell
& $adb shell appops set com.bydesigninteractive.ant AUTO_START allow
```

Google TV does not let adb start a dream directly (`cmd dreams` needs root and `Somnambulator` does nothing), so let it start on its own: note the current `screen_off_timeout`, set it to 15 s, and wait with the TV awake and untouched.

```powershell
& $adb shell settings get system screen_off_timeout
& $adb shell settings put system screen_off_timeout 15000
```

Afterwards, put the old timeout and `screensaver_components` values back with `settings put`.

The profile build is not debuggable and is what the TV runs; a debuggable build runs several times slower.

To measure the simulation on a device without the screensaver, run the headless benchmark (it takes over the screen for a few minutes and closes itself):

```powershell
& $adb shell am start -n com.bydesigninteractive.ant/.BenchmarkActivity --es scenario colony1000 --es mode cost --ei warmup 300 --ei measure 60
& $adb logcat -s AntFarm
```

`scenario` is `starter` or `colony1000`; `mode speed` with `--ef speed 4 --ei measure 30` runs it through the simulation thread instead.

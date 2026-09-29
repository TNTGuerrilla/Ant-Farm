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
- TV APK: `.\gradlew.bat :android:assembleDebug` (output in `android\build\outputs\apk\debug\`)

## Milestone M0: the TV stub

The current app is a stub that checks the TV before the real work starts:

1. Remote keys reach an interactive dream. The overlay lists keys twice: as the dream's window received them, and as libGDX delivered them.
2. 1,000 moving ant sprites hold the frame rate. Up doubles the sprite count and Down halves it, so the headroom can be found.
3. A 20 MB background save causes no visible hitch. The first write runs 10 s after start and then every 30 s; Center starts one now. The overlay shows the write time and the frames rendered while it ran.

To try it on a Google TV, connect adb to the TV (network debugging), then install it, note the current screensaver so it can be restored, and select this one:

```powershell
$adb = "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe"
& $adb install -r android\build\outputs\apk\debug\android-debug.apk
& $adb shell settings get secure screensaver_components
& $adb shell settings put secure screensaver_components com.bydesigninteractive.ant/.AntDream
& $adb logcat -s AntM0
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

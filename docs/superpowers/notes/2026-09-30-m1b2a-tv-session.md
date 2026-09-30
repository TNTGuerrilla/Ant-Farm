# M1b-2a: the early TV session (2026-09-30)

The TCL 65QM6K (Mali-G52, 32-bit ARM userspace) ran the M1b-2a app in its dream with `Scenarios.starter(1)` (60 foragers, 20 diggers) at 1x. The owner watched the 3D debug view for about three minutes, pressed OK for the nest view and again for the top-down view (about a minute each), then pressed Back. The log was read with `logcat -s AntFarm`; memory was sampled with `dumpsys meminfo` every 30 s while the dream ran. No crash, no `FATAL`, no schedule resets.

The TV's logcat buffer rotated, so the first 10 s lines of the 3D view (11:26:00 to 11:27:20) are missing; the numbers below come from the lines that survived. After Back, the 15 s test timeout started the dream again, which gave a second, fresh 3D run of one minute.

## Results

| View | fps | Frame p50 | Frame p99 | Frames over 25 ms | Ticks/s | ms per tick (avg, max) |
|---|---|---|---|---|---|---|
| 3D debug | 49 to 61 | 16.65 ms | 28 to 126 ms | about 22 per 600 (4%), max about 130 ms every window | 20.0 to 20.1 | 11.7 to 14.1, max 17 to 157 |
| 3D debug, fresh run | 45 to 61 | 16.65 ms | 18 to 126 ms | 1 to 22 per 600 | 20.0 to 20.1 (19.5 in the first window) | 17.1 to 19.1, max 21 to 389 (first window) |
| Nest | 59 to 61 | 16.67 ms | 19 ms | 0 to 1 per 600 (11 in the window with the switch) | 20.0 to 20.1 | 12.6 to 13.5, max 18 to 91 |
| Top-down | 60 to 61 | 16.67 ms | 18.5 ms | 0 to 1 per 600 (6 in the window with the switch) | 20.0 to 20.1 | 13.1 to 13.7, max 18 to 124 |

Memory (while the dream ran, ten samples over five minutes): total PSS 58 to 68 MB, Java heap 23 to 32 MB (it rises and falls with collections; no upward trend), native heap about 10 MB, RSS 150 to 160 MB. The Mali driver reports graphics memory as 0.

## What it means for M1b-2b

- **The simulation holds 20 ticks/s at 1x** with about 13 ms per tick (18 ms in the fresh run), a quarter to a third of the 50 ms budget, on its own thread while the render thread holds 60 fps in the 2D views. The desktop runs the same colony at about 0.35 ms per tick, so the TV is roughly 40 times slower.
- **Higher speeds cannot hold.** 4x needs ticks under 12.5 ms and 16x under 3.1 ms; at about 13 ms per tick the TV tops out near 3.8x real time. The remote has no speed control yet, so 16x was not measured; the Minecraft model makes it slow down rather than drop frames. Simulation cost cuts are needed before 4x or larger colonies are usable on the TV (profile the forager step, the SDF queries and `Field3` on ARM first).
- **The 3D view has a periodic hitch** of about 130 ms roughly twice a second (about 22 slow frames per 10 s) that the 2D views do not have. The likely cause is the ground mesh rebuild when a chunk's published spoil changes (the overlay is republished every 0.5 s and diggers add spoil continuously). M1b-2b replaces the debug ground with meshes built from the SDF; build them incrementally or off the render thread.
- **The TV's ms per tick left out the publish step** (snapshot fill, HUD every 5 ticks, overlay every 10), so the thread's real cost was somewhat higher. After the final review, the logged tick time covers step plus publish; remeasure at the start of M1b-2b.
- **The final review added a stopgap** for the 3D hitch: the ground rebuilds at once when the camera moves to a new patch, and at most every 3 s for spoil changes under the patch. It has not been measured on the TV.
- **Occasional tick spikes** of 50 to 157 ms (and 389 ms at start-up) did not cause resets; they are likely garbage collection or JIT on ARM.
- **Memory is well within the 150 MB budget** in PSS terms.

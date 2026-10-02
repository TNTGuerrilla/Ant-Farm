# M2a: brains replace the scripts

Status: approved in design discussion on 2026-10-02, awaiting spec review.
Parent design: `docs/design/poc-design.md` (section 5 "Brain", section 12 M2, section 15). Biology targets: `docs/research/simulation-reference.md` (sections 3 to 6). Follows M1b-2c (`docs/superpowers/specs/2026-10-01-m1b2c-lowpoly-look-design.md`).

## 1. Goal

M2 is split in two. **M2a (this spec)** gives every forager and digger its own small neural brain in place of the scripted decision rules, fed by its body state and by what the colony shares through trails, footprints, contacts and food exchange. The starting "instinct" brain is hand-wired from a documented circuit table, then refined by a realism search on the owner's PC that ranks brain variants against field observations and classic experiments. **M2b (a later spec)** adds learning and memory (reward-gated plasticity modelled on the insect mushroom body, fast and slow weights, route memory) and the learning-target test suite.

M2a is done when:

1. Foragers and diggers decide with a per-ant network: steering, speed, which action, trail deposit strength and the go-out drive. Code carries the actions out (the innate primitives); the scripted decision rules are gone.
2. The brain's inputs cover the body state, the senses and the colony's shared channels (section 2.1).
3. A hand-wired seed brain exists, with every circuit documented (section 3).
4. A realism search runs on the desktop in under an hour, on the performance cores only, discards non-viable variants, ranks the rest against field numbers and experiments, and writes a report and the winning genome, which ships as the instinct brain (section 4). The scoring is the litmus test that M4's evolution reuses.
5. `GruterScenarioTest` passes on 8 world seeds (up from 4, the owner's decision of 2026-09-30) with its thresholds unchanged.
6. On the TV (`profile` build), the 1,000-ant colony runs at 25 ms per tick or less on average (p99 at most 50 ms) with brains, and the 3D view still holds 60 fps.
7. The carry-overs are fixed (section 5).

Out of scope: learning and memory (M2b); brood, castes, the queen and nest tasks beyond digging (M3); the gene pool and evolution across colonies (M4); threats, so the health and damage inputs stay inactive (M5); ant collision (backlog); running the search on the GPU (section 4.4).

### 1.1 References

Two external simulators are used as design references only (owner decision 2026-10-02): their ideas are reimplemented in our own Kotlin from the papers they cite, and none of their code is ported. Where they disagree with `simulation-reference.md`, the reference wins.

- `151henry151/anthill` (Godot, GPL-3.0): footprint scent as a repellent at forks when a trail is present; antenna contact passing food odour; an experienced forager "nudging" a recruit; switching away from a depleted source as a validation goal.
- `MattMills/ant_simulator` (Rust, no license): validation experiments with Lasius numbers (double bridge, equal branches, source quality, crowding).

## 2. The brain

### 2.1 Inputs (about 28)

- **Senses:** trail left and right, footprint (home scent) left and right, home-scent level, home-vector direction (as a turn relative to heading) and length, food odour (2 channels: honeydew, prey).
- **Body state:** crop fill (crop / desired crop), reserves (an energy level that falls slowly and is refilled by feeding; it does not kill in M2a), age (normalized), ground temperature (from the day cycle's time of day, a smooth day and night curve until weather arrives), contact rate (nestmates touched in the last 10 s). Health and damage inputs exist and read 0 until M5.
- **Shared channels:** the returner rate (successful foragers returning to the nest in the last 60 s, felt by contact in the nest); contact flags for the last contact (the nestmate was a successful forager; the nestmate carried a food odour, with the odour's channel); a nudge (an oncoming experienced forager's heading, when one was just met on the surface).
- **Context flags:** on a stem, at food, in the nest, carrying a pellet, dig site in reach, at the entrance.
- **Noise:** one value drawn from the world's random generator at each evaluation (the random-walk drive).
- **Bias:** a constant 1.

### 2.2 Network

One recurrent hidden layer of 16 tanh units (an Elman network): each evaluation the hidden units see the inputs and their own previous state. Outputs: turn rate (tanh, scaled to a maximum turn rate), speed (sigmoid, times the species speed), action logits (one per action, masked and sampled), trail deposit strength (sigmoid) and the go-out drive (sigmoid). About 900 weights per ant (inputs to hidden, hidden to hidden, hidden to outputs, biases). The size can grow if the TV budget allows.

### 2.3 Evaluation

Every other tick (10 Hz), staggered by ant id, so each tick evaluates half the ants. Between evaluations an ant keeps its last turn rate, speed and action. The network runs on flat float arrays and allocates nothing per evaluation.

### 2.4 Actions and masks

The brain picks one of: keep walking, feed, unload, leave the nest, enter the nest, dig, pick up a pellet, drop a pellet, rest. Actions that are impossible in the moment are masked (feed only at food with room in the crop, dig only at a dig site, unload only in the nest with something in the crop, and so on); the brain samples among the possible ones. Surface walking, stem climbing, obstacle detours and nest path-following stay innate code. In the nest the brain chooses a destination (the dig frontier, the entrance, a rest spot) and the existing path-following moves the ant there.

### 2.5 Genome and individual variation

The weights are the colony's genome. Each ant gets the genome plus a small personal variation drawn from a hash of the world seed and its id, giving the individual differences the research reports (about 14% of foragers never laying trail; spread in response thresholds). Colony-level parameters that are already in `AntParams` (desired crop mean and spread, nest volume per worker) stay there in M2a.

### 2.6 Determinism

The brain is arithmetic on the inputs; the only randomness (the noise input, action sampling, personal variation) comes from the world's random generator in fixed ant order or from seeded hashes. Runs stay reproducible; the threaded-equals-single-threaded test and the fingerprint tests keep working.

## 3. The hand-wired seed brain

The seed is built from a written circuit table; every weight has a stated reason. Hidden units are grouped into mode circuits (searching, returning, feeding, nest work, digging) held on by their recurrent connections and switched by threshold units on the inputs.

1. **Go out:** an idle forager in the nest leaves with a probability that rises with the returner rate and an empty crop (the response-threshold rule s^2 / (s^2 + theta^2), approximated by a sigmoid).
2. **Search:** turn in proportion to (left minus right) over (left plus right) on the trail; footprint repels only where a trail is present; turn toward plant or food odour when detected; the noise input gives the tortuous outbound walk.
3. **Feed:** at food with the crop below its desired fill, feed.
4. **Return:** when the crop reaches its desired fill, steer along the home vector, straighter than when searching; lay trail only if the desired fill was reached; deposit more near the food, less when crowded or when the trail is already strong.
5. **Unload and rest:** in the nest with a full crop, unload; then rest or go out per circuit 1.
6. **Dig:** when the colony needs room and a dig site is in reach, dig; carrying a pellet, go to the entrance, go out, and drop it away from the entrance (replacing the digger script one for one).
7. **Contacts:** touching a successful forager raises the go-out drive; a received food odour biases the search toward that odour; the nudge turns a recruit away from the nudging forager's dead end.

Checks before the search: a unit test per circuit (inputs in, action and steering out); a colony comparison with today's scripted ants on the starter world (trips per hour, share of foragers outside, trail-laying, digging progress, within stated tolerances); the Gruter test on 8 seeds. The old scripts remain only until that comparison passes, then they are deleted.

## 4. The realism search (the litmus test)

### 4.1 The loop

A simple evolution strategy started by a desktop command (`.\gradlew.bat :sim:search`, or the Windows script in section 4.4): from the seed, each generation makes about 40 variants by adding small Gaussian changes to the weights, screens them, keeps the best 8 and breeds the next generation from them, for about 8 generations (about 320 to 400 variants). The best 5 overall get the full check.

### 4.2 Screening, viability and early stopping

Each variant runs the starter colony on 3 world seeds for 20 simulated minutes. It is dropped (and its run stopped early, at simulated minute 10 where possible) if on any seed: no ant has fed by minute 10; foragers never come home; fewer than 20% of surface ants are moving; ants pile up at the map edge; any value becomes non-numeric.

### 4.3 Ranking

Each measurement is scored by its distance from the field target in units of the target's tolerance; the scores are summed. Measurements: trip timings (feed about 60 s, return about 40 s, unload about 60 s); trail-laying (1.5 to 3 marks per 5 cm, more near food, about 14% never laying); naive ants following trails 62 to 70% of the time; search walks more tortuous going out than coming home; foragers about 10 to 25% of workers.

The full check (best 5): the Gruter test on 8 seeds, and four experiments on open ground: **distance choice** (two feeders, one twice as far; the colony settles on the near one, the open-ground form of the double bridge); **equal sources** (one source wins in most runs); **source quality** (the richer source gets about 90% or more of visits); **depletion switch** (the colony moves to a second source after the first runs out).

### 4.4 Compute

CPU only, on the performance cores: a small Windows launcher script starts the search's JVM with its processor affinity set to the P-core threads, detected from Windows' CPU set information (the highest efficiency class; on the owner's i9-12900K, logical processors 0 to 15), and below-normal priority; the search uses 14 worker threads, leaving one P-core and all E-cores free so the PC stays responsive. The whole search (screening and full check) fits in under an hour. The GPU is not used: the search must run the exact simulation the TV runs, and a GPU copy would be a second simulator that drifts from it; the brain arithmetic alone is too small a share of a trial to gain from offloading.

### 4.5 Output

A ranked markdown report with every measurement per variant, and the winning genome written to a resource file in `sim` that the app loads at start. `SeedBrain` stays in the code as the fallback and the reference.

## 5. Carry-overs

1. **The nest digging stall** (owner, 2026-10-01: nest ants stopped below the top centimetre and digging made no progress): reproduced headless and root-caused first; fixed in whichever layer it lives (dig plan, frontier, path-following) before the dig circuit builds on it.
2. **Open-ground hesitations** (trail steering against the scripted "keep outward" rule): the brain replaces both with one search circuit; a test fails on any open-ground pause longer than 5 s by a moving surface ant.
3. **Detour and homing interaction:** detours get their own run counter, so a detour's leftover run no longer becomes a home-vector run.
4. **A fingerprint covering a detour:** a golden test over a seed-1 run long enough to include one.

## 6. Testing

- Unit tests: the network arithmetic (a known input gives a known output), the action masks, each seed circuit.
- The seed against the scripts (colony comparison), the Gruter test on 8 seeds.
- Determinism: the threaded-equals-single-threaded test, fingerprints (re-recorded once when brains take over, since behaviour changes on purpose).
- The search: the viability filter, the scoring against hand-made measurements, a tiny smoke search (2 generations of 4 variants, short trials).
- TV: the benchmark activity with brains on the 1,000-ant colony (25 ms average, p99 50 ms), and a 1,000-ant preview run checking the 3D view still holds 60 fps; run by Claude under the owner's standing permission for preview-only runs.

## 7. Code structure

- `sim/brain/`: `Brain` (the network on flat arrays), `Genome` (weights, personal variation, save and load), `SeedBrain` (the circuit table), `Senses` (fills the inputs), `Actions` (masks and running the chosen primitive).
- `sim/ant/`: the primitives stay (`SurfaceWalk`, digging, feeding, nest paths, `Detour`); forager and digger decisions move to a brain-driven ant; the scripted `Forager` and `Digger` decision code is deleted once the comparison passes.
- `sim/search/`: `Litmus` (scoring), `Experiments` (distance choice, equal sources, quality, depletion), `Search` (the evolution loop), run by a `:sim:search` Gradle task and a Windows launcher script (`scripts/search.ps1`) that sets affinity and priority.
- The shipped genome: a resource under `sim/src/main/resources/`, loaded at start with `SeedBrain` as the fallback.

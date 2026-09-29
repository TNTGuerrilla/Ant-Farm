# 02: Individual learning, memory and navigation in ants (focus: Lasius niger)

Research notes to ground a Lasius niger colony simulation in which each ant carries a small neural network that is evolved across generations and adapts within its lifetime.

Confidence key:
- **verified**: numbers read from the paper itself, its abstract on the publisher page, or PMC full text during this session.
- **verified (secondary)**: the source was found, but the numbers came from a search-engine summary of the abstract or from a citing paper, not read directly. Treat the numbers as probably right but re-check before relying on them heavily.
- **unverified**: plausible, but no primary source was confirmed.

Species are Lasius niger (L. niger) unless stated otherwise.

---

## 1. Speed of learning: food locations, odours, routes

### 1.1 Side/location learning at a single T-junction
- (a) At a T bifurcation, 74.6% of L. niger foragers chose the rewarded branch after 1 previous visit, and 95.3% after 3 visits.
- (b) Grüter C., Czaczkes T.J., Ratnieks F.L.W. (2011). Decision making in ant foragers (Lasius niger) facing conflicting private and social information. Behavioral Ecology and Sociobiology 65: 141-148. https://doi.org/10.1007/s00265-010-1020-2
- (c) verified (secondary): figures from the abstract as returned by search; the Springer page itself blocked fetching.
- (d) Simulation implication: location memory should reach about 75% reliability after one rewarded trip and about 95% after three. A per-visit Hebbian or reward-modulated update with an effective learning rate near 0.5 to 0.7 per rewarded trip (on a 0 to 1 preference scale) reproduces this curve.

### 1.2 Complex routes (two bifurcations)
- (a) In a double-bifurcation maze, repeating routes (left-left, right-right) were solved with 97% correct choices per bifurcation. Alternating routes (left-right, right-left) reached only 68% overall (55% at the first bifurcation, 74% at the second). Trail pheromone raised alternating-route accuracy from 56% to 73%. Experienced ants with pheromone scored 84% at the first bifurcation versus 46% without. Ants with pheromone reached the feeder about 34% faster (13.7 s versus 18.3 s). Ants were tested over 7 visits and improved progressively. Ants deposited more pheromone on the harder (alternating) routes and after errors. High existing pheromone suppressed further deposition (negative feedback).
- (b) Czaczkes T.J., Grüter C., Ellis L., Wood E., Ratnieks F.L.W. (2013). Ant foraging on complex trails: route learning and the role of trail pheromones in Lasius niger. Journal of Experimental Biology 216: 188-197. https://doi.org/10.1242/jeb.076570
- (c) verified
- (d) Simulation implication: route memory should work best when each decision point can be solved by one consistent heading (a "go toward the remembered view/direction" rule). Sequence-dependent turns need more visits. Pheromone should act both as a steering input and as a learning facilitator (for example, raise the plasticity rate while pheromone is present). Deposit rate should increase after an error and decrease when local pheromone is already high.

### 1.3 Confinement does not teach routes
- (a) In a follow-up, the authors found no evidence that pheromone supported learning of a complex route in that setup, and ants physically confined to the correct route did not learn it at all.
- (b) Czaczkes T.J., Weichselgartner T., et al. (2016). The effect of trail pheromone and path confinement on learning of complex routes in the ant Lasius niger. PLoS ONE 11(3): e0149720. https://doi.org/10.1371/journal.pone.0149720
- (c) verified (secondary): the finding is from the search summary of the abstract. Only the first two authors were confirmed.
- (d) Simulation implication: learning should require the ant's own choice (an active decision followed by reward), not passive exposure. Update weights only on actions the network actually selected (policy-gradient or eligibility-trace style), not on forced movements.

### 1.4 Odour learning is faster than spatial learning
- (a) Odour-quality associations formed after 1 visit to each odour/quality pair. Side associations without odour needed at least 2 visits to each side. When odour memory and side memory conflicted, 100% of ants followed the odour linked to high-quality food, even though this sent them to the side that had given low-quality food.
- (b) Oberhauser F.B., et al. (Czaczkes lab) (2019). Private information conflict: Lasius niger ants prefer olfactory cues to route memory. Animal Cognition 22: 355-364. https://doi.org/10.1007/s10071-019-01248-3
- (c) verified (secondary): numbers are from the abstract text returned by search.
- (d) Simulation implication: give odour-to-value associations a higher learning rate (about 2x) than spatial/side associations. When both are present and conflict, weight the odour channel more heavily.

### 1.5 Multi-odour discrimination
- (a) Individual workers fed sequentially on up to 4 food qualities, each paired with a distinct odour. They learned at least 2, and probably 3, odour/quality associations, with as little as one exposure to each.
- (b) Czaczkes T.J., Kumar P. (2020). Very rapid multi-odour discrimination learning in the ant Lasius niger. Insectes Sociaux 67: 541-545. https://doi.org/10.1007/s00040-020-00787-0
- (c) verified (secondary)
- (d) Simulation implication: a small network can hold about 2 to 3 distinct odour-value memories simultaneously. A capacity limit of about 3 food-odour slots per ant is biologically defensible.

### 1.6 Primacy: first-learned option wins
- (a) Ants trained alternately to lemon- and rosemary-scented food preferred the first odour they had encountered: 86% after 2 visits and 87% after 6 visits (no familiarisation visit). They chose it regardless of position (84% left, 82% right). Pheromone presence did not change the choice.
- (b) Czaczkes lab (Oberhauser et al., author list unverified) (2022). Ants prefer the option they are trained to first. Journal of Experimental Biology 225(24): jeb243984. https://doi.org/10.1242/jeb.243984
- (c) verified (secondary): percentages from search summary.
- (d) Simulation implication: the learning rate should decay with experience, or early memories should consolidate more strongly. This produces a primacy effect: a young forager's first food source biases it for a long time.

### 1.7 Memory phases after a single trial
- (a) After one rewarded Y-maze trial, 34/50 ants chose the rewarded side. The memory persisted at 2 h (36/50), was gone at 24 h (18/30, chance). Cold anaesthesia disrupted the memory for up to about 15 min after training (30/50 at 10 min, not significant); by 30 min it was anaesthesia-resistant (34/50). The authors call this mid-term memory. A single trial did not produce long-term memory.
- (b) Mathada J., Romrig L., Poissonnier L.-A. (2025). Single-trial learning leads to mid-term memory formation in ants during an appetitive, but not an aversive, task. Proceedings of the Royal Society B 292(2045): 20243054. https://doi.org/10.1098/rspb.2024.3054 (PMC: https://pmc.ncbi.nlm.nih.gov/articles/PMC12014227/)
- (c) verified
- (d) Simulation implication: use two memory timescales. Fast weights form from one trial, consolidate in about 15 to 30 min of sim time and decay to baseline within about 24 h. Slow weights require repeated trials (see 1.9). One visit alone should not create permanent memory.

### 1.8 Odour-cued recall the next day
- (a) Foragers trained to two scented feeders at the two ends of a T-maze were tested the next day with one of the odours presented in the air, on the substrate, or via trophallaxis with nestmates. They chose the matching side 89% of the time, but only after the first 10 min of testing.
- (b) Czaczkes T.J., et al. (2014). Ants use directionless odour cues to recall odour-associated locations. Behavioral Ecology and Sociobiology 68: 981-988. https://doi.org/10.1007/s00265-014-1710-2
- (c) verified (secondary): the 89% and 10 min figures come from the abstract via search.
- (d) Simulation implication: an odour arriving in the nest (for example on a returning forager's crop contents) can act as a retrieval cue that re-activates a stored location memory. Memories of multiple trained feeders survive overnight after repeated training.

### 1.9 Long retention with repeated training (other species)
- (a) Formica fusca: olfactory association learned in about 3 to 4 trials, retained for up to 3 days, and required about 6 unrewarded trials to extinguish.
- (b) Piqueret B., et al. (2019). Ants learn fast and do not forget: associative olfactory learning, memory and extinction in Formica fusca. Royal Society Open Science 6: 190778. https://doi.org/10.1098/rsos.190778
- (c) verified (numbers summarised from the full-text PDF)
- (d) Simulation implication: after about 3 or 4 reinforced exposures, move the memory into a slow, multi-day store. Extinction should need about 6 unrewarded exposures rather than 1.

- (a) Linepithema humile (Argentine ant): 65% correct Y-maze arm after 1 visit, 84% after 2. Odour learning reached up to 85% after one exposure. 73% correct choices after 48 h.
- (b) Wagner T., Galante H., Josens R., Czaczkes T.J. (2023). Systematic examination of learning in the invasive ant Linepithema humile reveals fast learning and long-lasting memory. Animal Behaviour 203. https://www.sciencedirect.com/science/article/abs/pii/S0003347223001628 (preprint: https://doi.org/10.1101/2022.04.12.487867)
- (c) verified (secondary): numbers from search summary; volume number unverified.
- (d) Simulation implication: an alternative benchmark for learning curves. Memory at 48 h is realistic for multiply-trained foragers.

- (a) Myrmica sabuleti operant conditioning: sugar-reward conditioning was short-lasting; reconditioning lasted about 4 h. Meat-reward conditioning was detectable after 7 h, reconditioning still significant after 8 h. Extinction was fast (response not significant after 1 h; below control after 4 h). Under normal conditions conditioned responses were lost over a few days.
- (b) Cammaerts M.-C. (2004). Operant conditioning in the ant Myrmica sabuleti. Behavioural Processes. https://www.sciencedirect.com/science/article/abs/pii/S0376635704001603 (the "few days" retention statement is from a separate Cammaerts study found in the same search; the exact paper is unverified)
- (c) verified (secondary)
- (d) Simulation implication: the reward type (protein versus sugar) may modulate memory strength. Reward magnitude should scale the learning rate.

### 1.10 Social route to odour learning (trophallaxis)
- (a) Camponotus mus learned an odour-sucrose association from a single trophallaxis lasting as little as 2 to 3 s, whereas individual foragers needed up to 12 visits to a food source to form the same association.
- (b) Provecho Y., Josens R. (2009). Olfactory memory established during trophallaxis affects food search behaviour in ants. Journal of Experimental Biology 212: 3221-3227. https://doi.org/10.1242/jeb.033506
- (c) verified
- (d) Simulation implication: include a "received food odour" input during trophallaxis in the nest, and let the receiver update odour-value weights from it. This lets information spread without the receiver leaving the nest.

---

## 2. Route memory versus pheromone: how conflicts are resolved

### 2.1 Memory overrides pheromone after one visit (in bright light)
- (a) When private memory and pheromone conflicted, 82% to 100% of foragers chose the branch where they had previously been fed. The proportion depended on number of previous visits (1 versus 3), not on pheromone strength (trail laid by 1 versus 20 foragers). Naive ants followed pheromone more weakly: 61.6% (1 forager's trail) and 70.2% (20 foragers' trail) chose the marked branch over an unmarked one.
- (b) Grüter et al. (2011), see 1.1.
- (c) verified (secondary)
- (d) Simulation implication: after even one rewarded visit, the memory channel should outweigh the pheromone channel. Pheromone gain should be highest for naive ants and drop steeply once a route memory exists. Pheromone input can saturate quickly (1 versus 20 depositors make little difference).

### 2.2 Trail-following accuracy itself
- (a) With improved methods, L. niger trail-following accuracy at a T was 83% (naive scouts), 82% (recruits) and 74% (experienced "shuttlers"). Differences were not significant. Earlier studies reported only 60% to 70%. Inter-trial variation was very high.
- (b) Czaczkes T.J., Castorena M., Schürch R., Heinze J. (2017). Pheromone trail following in the ant Lasius niger: high accuracy and variability but no effect of task state. Physiological Entomology 42: 91-97. https://doi.org/10.1111/phen.12174
- (c) verified (secondary)
- (d) Simulation implication: on a single unambiguous trail, pheromone following should be about 75% to 85% accurate, with substantial random noise per decision. Do not make ants perfect trail followers.

### 2.3 Trail following is stable across contexts
- (a) In an open arena and Y-maze, trail following fidelity was not modulated by distance travelled, direction of travel, or a previous visit to the food location. However, distance travelled before meeting the trail changed walking straightness, speed and total distance.
- (b) Anonymous in this check (authors not retrieved) (2026). Pheromone trail following is not modulated by previous visit to food location, distance travelled, or travel direction in the ant Lasius niger. Insectes Sociaux. https://doi.org/10.1007/s00040-026-01106-9
- (c) verified (secondary): title, journal and finding confirmed via search; authors unverified.
- (d) Simulation implication: pheromone response can be largely innate (evolved weights), while locomotion parameters (speed, straightness) are the plastic part. Note this partly contrasts with 2.1; in 2.1 the pheromone and memory directly conflicted.

### 2.4 Light level shifts the balance ("copy when uncertain")
- (a) Under 3200 lux, foragers mostly followed their route memory when it conflicted with pheromone. At dusk-like 10 lux and near-darkness 0.007 lux, reliance on pheromone increased. In darkness ants failed to learn the food location at all. Pheromone deposition increased at low light. Route memory in L. niger is primarily visual in bright light.
- (b) Jones S., Czaczkes T.J., et al. (2019). Copy when uncertain: lower light levels increase trail pheromone depositing and reliance on pheromone trails in ants. Animal Behaviour. https://www.sciencedirect.com/science/article/abs/pii/S0003347219302520 (preprint: https://doi.org/10.1101/473579)
- (c) verified (secondary): lux values and findings confirmed from search summary.
- (d) Simulation implication: add a light-level input. Gate the visual/memory channel by light (multiply by a light-dependent reliability term) and increase the pheromone gain and deposition rate at night. A day-night cycle in the screensaver will then naturally switch the colony from memory-driven to trail-driven foraging.

### 2.5 Review of the weighting across species
- (a) Naive ants prioritise pheromone; experienced ants usually prioritise learned cues when they conflict. Visual landmarks dominate once routes are familiar. Adding sucrose to trails increased pheromone following. Lower light increased reliance on pheromone. The weighting depends on species and the reliability of each information stream.
- (b) Freas C.A., Buehlmann C., Spetch M.L. (2026). Combining social and private information: how ants use pheromones and learnt cues to navigate. Learning and Behavior 54(1): 23-36. https://doi.org/10.3758/s13420-025-00697-w
- (c) verified
- (d) Simulation implication: implement cue weighting as reliability-weighted integration; the network can learn this if its inputs include both the cue values and a reliability proxy (light, experience count).

### 2.6 Pheromone deposition rules
- (a) L. niger deposited up to 22 times more pheromone within 10 cm of food than when close to the nest, and up to 4 times more for food 100 cm away than 20 cm away. Ants that had followed an erroneous trail did not upregulate deposition toward the correct food; they cannot "correct" outdated trails.
- (b) Anonymous in this check (Czaczkes lab, 2024). Ants (Lasius niger) deposit more pheromone close to food sources and further from the nest but do not attempt to update erroneous pheromone trails. Insectes Sociaux. https://doi.org/10.1007/s00040-024-00995-y
- (c) verified (secondary): numbers from the abstract via search; authors unverified.
- (d) Simulation implication: deposition should be a function of distance-from-food (high near food) and food distance (higher for far food). Trails decay rather than being actively corrected.

### 2.7 Pheromone lifetime
- (a) L. niger trail pheromone has a mean lifetime of about 47 min at room temperature.
- (b) Beckers R., Deneubourg J.-L., Goss S. (1993). Modulation of trail laying in the ant Lasius niger and its role in the collective selection of a food source. Journal of Insect Behavior 6: 751-759. (Also Beckers et al. 1992, Trail laying behaviour during food recruitment in the ant Lasius niger, Insectes Sociaux 39: 59-71.) No DOI retrieved.
- (c) verified (secondary): the 47 min figure is quoted by citing papers; the original was not read.
- (d) Simulation implication: pheromone grid decay with a mean lifetime around 47 min of sim time (exponential decay constant about 1/2820 per second). Speed this up with temperature (see 7.5).

### 2.8 Memory and motivation modulate trail following (multi-species)
- (a) Route memory modulated trail pheromone responses in L. niger, Euprenolepis procera and Linepithema humile; motivation (hunger) also modulated responses in E. procera and L. humile.
- (b) von Thienen W., Metzler D., Witte V. (2016). How memory and motivation modulate the responses to trail pheromones in three ant species. Behavioral Ecology and Sociobiology 70: 393-407. https://doi.org/10.1007/s00265-016-2059-5
- (c) verified (secondary)
- (d) Simulation implication: include a colony-hunger or individual-motivation input that scales pheromone following.

### 2.9 Olfactory cues beat route memory; multimodal conditional cues
- (a) See 1.4 (odour beats side memory 100%). In addition, L. niger can learn conditional combinations of odour, colour and side: when odour was made uninformative, ants still found the reward via the correct colour-side combination. A single trial was not enough for this multimodal integration (only a non-significant trend).
- (b) De Agrò M., Oberhauser F.B., Loconsole M., et al. (2020). Multi-modal cue integration in the black garden ant. Animal Cognition 23(6): 1119-1127. https://doi.org/10.1007/s10071-020-01360-9
- (c) verified (secondary): finding and citation from search.
- (d) Simulation implication: a hidden layer is justified (ants form conjunctive codes such as odour x colour x side), but conjunctive associations should need multiple trials, unlike simple odour associations.

---

## 3. Path integration, visual landmarks and learning walks

### 3.1 Path integration memory decay (Cataglyphis fortis)
- (a) Home vector direction information decays exponentially with a time constant of about 4.5 days and is lost from about the 6th day; vector information persists more than 2 days. Homing distance error grows at a constant rate with captivity time.
- (b) Ziegler P.E., Wehner R. (1997). Time-courses of memory decay in vector-based and landmark-based systems of navigation in desert ants, Cataglyphis fortis. Journal of Comparative Physiology A 181. https://doi.org/10.1007/s003590050088
- (c) verified (secondary): the tau = 4.5 days value appeared in the search snippet summarising this paper; the paper itself was not read.
- (d) Simulation implication: a path-integration accumulator (dx, dy to nest) is a fair built-in input. Add noise proportional to distance travelled; it does not need to decay within a single foraging trip.

### 3.2 Distance by stride counting (Cataglyphis fortis)
- (a) Ants given stilts overshot and ants on stumps undershot the nest, in quantitative agreement with a stride-integrator (pedometer) model.
- (b) Wittlinger M., Wehner R., Wolf H. (2006). The ant odometer: stepping on stilts and stumps. Science 312: 1965-1967. https://doi.org/10.1126/science.1126912
- (c) verified (secondary)
- (d) Simulation implication: odometry based on steps taken, not on optic flow. An error term proportional to path length is realistic.

### 3.3 Path integration gates olfactory homing (Cataglyphis fortis)
- (a) Homing ants follow nest-odour plumes upwind, including CO2 alone at adequate concentrations, but only when their path integrator says they are near the nest. Far from home, path integration overrides odour, which prevents them entering foreign nests' CO2 plumes. Ants also learn environmental odours associated with the nest entrance as olfactory landmarks.
- (b) (2012). Path integration controls nest-plume following in desert ants. Current Biology (authors unverified). https://www.sciencedirect.com/science/article/pii/S0960982212001789 ; and (2009). Smells like home: desert ants, Cataglyphis fortis, use olfactory landmarks to pinpoint the nest. Frontiers in Zoology 6: 5 (authors unverified; search attributes this line of work to Buehlmann, Steck and Hansson). https://doi.org/10.1186/1742-9994-6-5
- (c) verified (secondary): finding confirmed; author lists not confirmed.
- (d) Simulation implication: gate the nest-odour/CO2 input by the magnitude of the home vector (multiplicative gating). A network with a PI-magnitude input can learn this gating.

### 3.4 Learning walks by naive ants (Cataglyphis fortis)
- (a) Naive ants performed 3 to 7 learning walks before starting to forage. First walks reached less than 0.3 m from the entrance and lasted under 1 minute (about 14 s on average per a related study); later walks reached up to about 2.18 m. 27 of 42 marked ants reappeared the next day, and ants typically began foraging on about their second day outside. Learning walks contained many more full turns (pirouettes; median 8) than experienced forager paths (median 1). During pirouettes ants look back toward the nest entrance.
- (b) Fleischmann P.N., Christian M., Müller V.L., Rössler W., Wehner R. (2016). Ontogeny of learning walks and the acquisition of landmark information in desert ants, Cataglyphis fortis. Journal of Experimental Biology 219: 3137-3145. https://doi.org/10.1242/jeb.140459
- (c) verified (the 14 s average is verified (secondary), from a related Cataglyphis summary)
- (d) Simulation implication: new outdoor workers should spend about 1 to 2 days on short looping excursions of increasing radius (0.3 m -> 2 m scaled to the sim) before real foraging. During these, visual-memory plasticity should be high and the nest view should be stored.

### 3.5 Landmark memory acquisition and retention (Melophorus bagoti)
- (a) Ants were tested after 1, 3, 7 or 15 training trials spread over 1, 2 or at least 3 days, and after delays of 0 to 192 h. More trials and more training days both improved acquisition. Once learned, landmark memories were long-term and lasted throughout the foraging lifetime. Ants searched at the centre of the landmark array, consistent with snapshot matching.
- (b) Narendra A., Si A., Sulikowski D., Cheng K. (2007). Learning, retention and coding of nest-associated visual cues by the Australian desert ant, Melophorus bagoti. Behavioral Ecology and Sociobiology. https://doi.org/10.1007/s00265-007-0386-2
- (c) verified (secondary)
- (d) Simulation implication: visual nest memories should consolidate over several days of exposure and then become essentially permanent (no decay over a forager's life).

### 3.6 View-based (snapshot) guidance (Formica rufa)
- (a) Wood ants store landmark views from particular vantage points, including retinal positions of landmark edges, and steer to minimise the mismatch between the current and stored view. Apparent landmark width is used to pick which snapshot to recall.
- (b) Collett lab (Graham, Collett and colleagues); e.g., View-based navigation in insects: how wood ants (Formica rufa L.) look at and are guided by extended landmarks (2002), Journal of Experimental Biology. https://pubmed.ncbi.nlm.nih.gov/12124373/ ; Visual cues for the retrieval of landmark memories by navigating wood ants. https://pmc.ncbi.nlm.nih.gov/articles/PMC1885948/
- (c) verified (secondary): mechanism confirmed; exact authors and years unverified.
- (d) Simulation implication: instead of rendering real images, give each ant a coarse "panorama" input (for example 8 to 16 sectors of landmark/skyline intensity). Familiarity with stored views can drive steering.

### 3.7 L. niger uses vision for route memory
- (a) Route memories in L. niger are based on visual cues at high light levels (see 2.4); ants making return journeys likely match learned landmarks or panorama.
- (b) Jones et al. (2019), see 2.4; Czaczkes et al. (2013), see 1.2. Also: Combined use of pheromone trails and visual landmarks by the common garden ant Lasius niger. https://www.researchgate.net/publication/225812023 (authors and year unverified).
- (c) verified (secondary)
- (d) Simulation implication: include visual inputs for L. niger even though it is a trail-laying species.

### 3.8 Naive L. niger search behaviour
- (a) Released L. niger walked about 2.27 mm/s slower when moving away from their start point (white light) and about 1.70 mm/s slower under red light. Turning angles were narrower when heading back toward the release point, and free-path segments were shorter when heading away (odds ratio 0.46). This area-restricted search took 4.06 times longer to reach 200 mm net displacement than an isotropic random walk. The homing bias persisted under red light, so it does not depend on vision alone.
- (b) Bonavita et al. (2026). Discovering search behaviour in black garden ant trajectories. PLoS ONE. https://doi.org/10.1371/journal.pone.0327957
- (c) verified
- (d) Simulation implication: the default (untrained) exploration policy should be a correlated random walk biased by the home vector: slower and more tortuous when heading away, straighter when heading home. This is a good target for the evolved baseline behaviour.

### 3.9 Collective exploration and home-range marking
- (a) L. niger scouts are mobilised within the first 5 min of exposure to a new area, with exploratory activity stabilising after about 40 min. Walking ants passively deposit home-range marks (cuticular hydrocarbons from the tarsi). The intensity of home-range marking tells scouts about distance from the nest and local activity, and modulates their trail recruitment.
- (b) Devigne C., Detrain C. (2002). Collective exploration and area marking in the ant Lasius niger. Insectes Sociaux. https://doi.org/10.1007/PL00012659 ; Devigne C., Detrain C. (2006). How does food distance influence foraging in the ant Lasius niger: the importance of home-range marking. Insectes Sociaux. https://doi.org/10.1007/s00040-005-0834-9
- (c) verified (secondary): years inferred from DOIs, unverified.
- (d) Simulation implication: add a second, slow-decaying "footprint" (home-range) field laid by every walking ant. It gives a familiarity/territory input distinct from trail pheromone.

---

## 4. Naive versus experienced workers; age polyethism

### 4.1 Nurses younger than foragers in L. niger
- (a) In L. niger the youngest workers mostly work inside the nest and the oldest outside. Proteome and metabolome differences were mainly linked to task (nest worker versus forager), and secondarily to age. The study used workers aged 0 to 1 month ("young") and 11 to 12 months ("old"), and created young foragers experimentally, so task can be decoupled from age.
- (b) Quque M. et al. (2023). Both age and social environment shape the phenotype of ant workers. Scientific Reports 13. https://doi.org/10.1038/s41598-022-26515-1
- (c) verified
- (d) Simulation implication: age-based role assignment should be a soft bias that the colony can override (for example if foragers die, young workers can be recruited to forage).

### 4.2 Worker lifespan in L. niger
- (a) Workers produced early in colony founding had a life expectancy of about 430 days (max 1129 days); workers produced later about 310 days (max 1094 days). Foragers are the fastest-ageing subcaste. Queens can live up to about 28 years.
- (b) (2016, authors unverified). Worker lifespan is an adaptive trait during colony establishment in the long-lived ant Lasius niger. Experimental Gerontology. https://www.sciencedirect.com/science/article/pii/S0531556516303345
- (c) verified (secondary): numbers from search summary.
- (d) Simulation implication: an ant's "lifetime learning" window is on the order of a year; foraging is the dangerous final phase. Forager mortality should be much higher than nurse mortality.

### 4.3 Age at onset of foraging (other species)
- (a) Across ant species including Myrmica, onset of foraging is commonly reported at about 2 to 3 weeks of adult age; Formica subsericea workers may take about 30 days to begin foraging. Switching to more dangerous tasks is age-influenced, but switching back to safer tasks is age-independent.
- (b) Search results summarising several age-polyethism papers, including: Diversity in identity: behavioral flexibility, dominance, and age polyethism... Behavioral Ecology and Sociobiology (2015). https://doi.org/10.1007/s00265-015-1950-9 ; and Frontiers in Psychology (2019) https://www.frontiersin.org/journals/psychology/articles/10.3389/fpsyg.2019.00710/pdf
- (c) unverified: the specific numbers could not be tied to a single primary paper during this session.
- (d) Simulation implication: a plausible default is nurse from eclosion to about day 14 to 30, then a transition phase (digging/nest maintenance), then foraging, with thresholds modulated by colony need.

### 4.4 Size and task in founding L. niger colonies
- (a) In newly founded L. niger colonies, smaller workers were more efficient at brood rearing and larger workers more efficient at foraging.
- (b) Mentioned in search results linked to Kramer et al. 2016 (see 4.2); original source unverified.
- (c) unverified
- (d) Simulation implication: optional body-size trait that biases task choice.

### 4.5 Naive versus experienced behavioural differences (summary of sections above)
- Naive ants follow pheromone more weakly than experienced ants follow memory (61.6% to 70.2% versus 82% to 100%; Grüter et al. 2011).
- Naive Cataglyphis make learning walks with many pirouettes before foraging (Fleischmann et al. 2016).
- Experienced ants rely on vision in daylight and switch to pheromone in the dark (Jones et al. 2019).
- Experience makes routes faster: experienced ants with pheromone reach food in 13.7 s versus 18.3 s without (Czaczkes et al. 2013).
- Simulation implication: an "experience count" or age input lets evolution discover different policies for naive and experienced ants, or plasticity alone can produce the shift.

---

## 5. Aversive learning

### 5.1 Single aversive trial fails in L. niger; five trials succeed
- (a) One Y-maze trial punished with 10 s of water immersion produced no avoidance (24/50). Five consecutive punished trials produced avoidance (21/30, p = 0.043). One trial with quinine also failed (26/50).
- (b) Mathada et al. (2025), see 1.7.
- (c) verified
- (d) Simulation implication: aversive (negative) learning rate should be much lower than appetitive: about 5 punishments to learn avoidance versus 1 reward to learn attraction. Notably the punishment was water immersion, the closest published analogue to learning to avoid flooding.

### 5.2 L. niger can learn to avoid odours but not pheromone trails
- (a) With quinine (60 mM) and/or 7.5 V electric shock as punishment, L. niger showed one-trial learning with 100% accuracy for avoiding a lemon odour associated with punishment. However, when the pheromone trail predicted punishment, ants did not learn to avoid it: after about 10 trials they stopped following it, but even after 25 trials they never exceeded chance. Instead most ants developed a side bias (always going to one arm), which improved food retrieval compared to their first visits.
- (b) Wenig K., Bach R., Czaczkes T.J. (2021). Hard limits to cognitive flexibility: ants can learn to ignore but not avoid pheromone trails. Journal of Experimental Biology 224(11): jeb242454. https://doi.org/10.1242/jeb.242454
- (c) verified
- (d) Simulation implication: this is a strong design constraint. Make the pheromone-following weight partly hard-wired (evolved, not plastic below zero): lifetime learning may reduce pheromone gain to zero but not invert it. Odour-to-danger associations can be plastic and fast. Note the contrast with 5.1 (one-trial aversive learning of odour works in Wenig et al. but one-trial spatial aversive learning fails in Mathada et al.), so aversive learning appears fast for odours and slow for places.

### 5.3 Aversive odour learning in harnessed Camponotus
- (a) Camponotus aethiops learned an odour-heat association (75 degrees C applied to the legs) measured by the mandible opening response. With 12 differential trials (6 punished, 6 unpunished) about 60% of ants responded to the punished odour by the end, with good short-term (10 min) retention. Differential conditioning was better than absolute conditioning.
- (b) Desmedt L., Baracchi D., Devaud J.-M., Giurfa M., d'Ettorre P. (2017). Aversive learning of odor-heat associations in ants. Journal of Experimental Biology 220: 4661-4668. https://doi.org/10.1242/jeb.161737
- (c) verified
- (d) Simulation implication: aversive associations benefit from contrast (punished versus safe cue). Heat/high temperature is a valid punishment signal.

### 5.4 Predators, flooding and rain
- (a) No study was found showing individual L. niger learning to avoid predators, flooding or rain. Colony-level flood responses exist in other species: Pheidole workers modify nest entrances or abandon subterranean nests during monsoon; fire ants (Solenopsis invicta) raft, relocate or "bunker down" depending on the rate of water rise; some species build levees; workers of some species block entrances with their heads and remove water by drinking and regurgitating. Lasius flavus survives winter inundation on tidal meadows through low metabolic rate and air bubbles trapped in soil.
- (b) Dual response to nest flooding during monsoon in an Indian ant (2015). Scientific Reports. https://pmc.ncbi.nlm.nih.gov/articles/PMC4562246/ ; Floods and fire ants, Solenopsis invicta (2024). Austral Entomology. https://onlinelibrary.wiley.com/doi/full/10.1111/aen.12692 ; Convergent evolution of levee building behavior among distantly related ant species. Insectes Sociaux. https://doi.org/10.1007/s00040-011-0151-4 ; Lasius flavus note from Myrmecological News 14: 113-121 (2011), https://myrmecologicalnews.org/cms/index.php?option=com_download&view=download&filename=volume14/mn14_113-121_printable.pdf&format=raw
- (c) verified (secondary) for the behaviours described; unverified that any of them is learned by individuals
- (d) Simulation implication: model flood responses as evolved (innate) rules or colony-level responses (evacuate brood upward, block entrances), with at most slow individual aversive learning of wet locations (5.1 suggests about 5 exposures).

---

## 6. Social learning and information transfer

### 6.1 Tandem running as teaching (Temnothorax albipennis)
- (a) Leaders walk about 4 times slower with a follower (median 1.8 mm/s) than alone (8.4 mm/s). The leader continues only when the follower frequently taps its legs and gaster with its antennae. Followers found food faster when led (mean 201 s) than when searching alone (310 s). The follower's return path is usually faster and straighter than the leader's route (6 of 8 cases). Leader and follower regulate the gap: at about the follower's antennal reach (about 1 mm) both move at constant speed; if the gap exceeds about twice the antennal reach, the leader stops and waits while the follower loops. Speed between pauses was higher with conspicuous landmarks (7.6 versus 5.8 mm/s). Carried ants (3 times faster than tandems) do not learn the route because their heads face backwards.
- (b) Franks N.R., Richardson T. (2006). Teaching in tandem-running ants. Nature 439: 153. https://doi.org/10.1038/439153a
- (c) verified (read from the PDF)
- (d) Simulation implication: Lasius does not tandem run, but the gap-control rule (antennal contact as a feedback input) is a clean model for any follower behaviour. Learning should require the learner to face forward and see the route.

### 6.2 Route learning by tandem followers (Temnothorax albipennis)
- (a) From 74 tandem runs by 28 leaders (55 followers analysed), followers' later solo outward trips to food resembled their tandem route significantly more than random pairs; homeward trips did not. Leaders had individual-specific routes, and routes got shorter over successive trips. Improved routes were not passed on socially.
- (b) Sasaki T., et al. (2020). Route learning during tandem running in the rock ant Temnothorax albipennis. Journal of Experimental Biology 223: jeb221408. https://doi.org/10.1242/jeb.221408
- (c) verified
- (d) Simulation implication: route memory is direction-specific (outbound and homebound are learned separately). Consider separate memory heads for "to food" and "to nest".

### 6.3 Quorum and switching recruitment mode (Temnothorax)
- (a) During emigration, ants use tandem runs until a quorum is reached at the new site, then switch to carrying nestmates. Tandem-led ants learn the route and can later lead; carried ants do not.
- (b) Pratt S.C., Mallon E.B., Sumpter D.J.T., Franks N.R. (2002). Quorum sensing, recruitment, and collective decision-making during colony emigration by the ant Leptothorax albipennis. Behavioral Ecology and Sociobiology 52: 117-127.
- (c) verified (secondary)
- (d) Simulation implication: a local-density (nestmate encounter rate) input enables quorum-like behaviour; useful if the sim includes nest relocation after flooding.

### 6.4 Recruitment and trophallaxis in L. niger
- (a) In L. niger, trophallaxis frequency varies with food type while the duration of each exchange stays constant. Recruited workers' search paths depend more on food type (prey versus sugar) than sugar concentration; recruits to sugar search more sinuously. Czaczkes et al. 2014 (see 1.8) showed that food odour delivered by trophallaxis can trigger recall of a learned location.
- (b) (authors and year unverified). Trophallaxis in Lasius niger: a variable frequency and constant duration for three food types. Insectes Sociaux. https://doi.org/10.1007/s00040-010-0133-y ; Le Breton J., Fourcassié V. (year unverified). Information transfer during recruitment in the ant Lasius niger. Behavioral Ecology and Sociobiology. https://doi.org/10.1007/s00265-003-0704-2
- (c) verified (secondary): titles and findings confirmed; bibliographic details partly unverified.
- (d) Simulation implication: returning foragers should broadcast food-type and food-odour information in the nest; recruits get the odour and food type but not the location, so they still depend on trails and search.

### 6.5 No tactile direction signal in Lasius
- (a) A study titled "No evidence for tactile communication of direction in foraging Lasius ants" found no evidence that direction is communicated by touch.
- (b) Insectes Sociaux (2017/2018). https://doi.org/10.1007/s00040-017-0583-6
- (c) verified (secondary): title only; details not read.
- (d) Simulation implication: do not give antennation contacts a directional payload in L. niger; contacts carry identity/state/odour, not direction.

### 6.6 Trail geometry encodes direction (Monomorium pharaonis)
- (a) Foragers can use the angle at trail bifurcations to tell which way leads home; correct reorientation is maximised at bifurcation angles of about 60 degrees, as found in natural networks.
- (b) Jackson D.E., Holcombe M., Ratnieks F.L.W. (2004). Trail geometry gives polarity to ant foraging networks. Nature 432: 907-909. https://doi.org/10.1038/nature03105
- (c) verified (secondary)
- (d) Simulation implication: not needed for L. niger, but a local trail-curvature input would allow this.

---

## 7. Sensory inputs (candidate neural network inputs)

### 7.1 Odour and trail pheromone (antennae)
- (a) Trail pheromone following: 74% to 83% accurate at a T (2.2), saturating response (2.1), mean trail lifetime about 47 min (2.7), innate and not invertible by learning (5.2). Food odours are learned in 1 trial (1.4, 1.5). Home-range cuticular hydrocarbon marks give familiarity/distance information (3.9).
- (d) Simulation implication: inputs = left/right antenna pheromone concentration (bilateral difference drives turning), food-odour identity vector (a few channels), home-range mark density.

### 7.2 CO2
- (a) Leaf-cutting ants (Atta) have CO2-sensitive sensilla ampullacea on the distal antennal segment whose neurons fire continuously without adapting, allowing absolute CO2 measurement; ants use CO2 to choose where to place brood and fungus. Cataglyphis uses the nest's CO2 plume to pinpoint the entrance, gated by path integration (3.3).
- (b) Ultrastructure and physiology of the CO2 sensitive sensillum ampullaceum in the leaf-cutting ant Atta sexdens. Arthropod Structure and Development (2000). https://sciencedirect.com/science/article/abs/pii/S1467803900000128 (authors unverified); Carbon dioxide sensing in an obligate insect-fungus symbiosis (2017), PLoS ONE. https://doi.org/10.1371/journal.pone.0174597
- (c) verified (secondary); not shown for L. niger (unverified for L. niger)
- (d) Simulation implication: a CO2 field emitted by the nest (and brood chambers) gives a nest-direction gradient and a "deep inside nest" signal for nurses placing brood.

### 7.3 Temperature and humidity
- (a) Camponotus rufipes has two cold-sensitive neurons in the antennal sensillum coelocapitulum, coding different parameters of the thermal environment. Insect antennae sense temperature, humidity and CO2. For L. niger, keepers report foraging-area temperatures of about 18 to 28 degrees C and humidity tolerance over a broad range.
- (b) Two cold-sensitive neurons within one sensillum code for different parameters of the thermal environment in the ant Camponotus rufipes. https://pmc.ncbi.nlm.nih.gov/articles/PMC4558426/ (authors unverified). L. niger care-sheet temperatures from hobbyist sources (for example https://brumaants.com/how-to-care-for-lasius-niger).
- (c) verified (secondary) for Camponotus; unverified (hobby sources only) for L. niger ranges
- (d) Simulation implication: temperature and humidity inputs (local soil cell values) let nurses move brood along gradients and foragers stay inside during heat or rain.

### 7.4 Temperature limits trails (Tapinoma nigerrimum)
- (a) Foraging drops above 30 degrees C; above 40 degrees C workers no longer discriminate previously marked substrate because the pheromone degrades; most gaster secretions vanish at 25 degrees C with iridodials persisting to 55 degrees C.
- (b) van Oudenhove L., et al. (2011). Temperature limits trail following behaviour through pheromone decay in ants. Naturwissenschaften 98: 1009-1017. https://doi.org/10.1007/s00114-011-0852-6
- (c) verified (secondary)
- (d) Simulation implication: pheromone decay rate should increase with ground temperature.

### 7.5 Vision
- (a) L. niger route memories are visual in bright light and fail in darkness (2.4); L. niger can learn colour cues in conditional combinations (2.9). L. niger workers reportedly have about 120 ommatidia per eye, which is low resolution.
- (b) Jones et al. 2019; De Agrò et al. 2020; ommatidia count from a hobbyist page: https://www.antnest.co.uk/lasius-niger/
- (c) verified (secondary) for behaviour; unverified for the 120 ommatidia figure
- (d) Simulation implication: vision input should be very coarse (a few angular sectors) and scaled by light level.

### 7.6 Vibration and sound
- (a) Ants produce substrate-borne vibrations by stridulation (plectrum against pars stridens on the gaster), drumming and scraping. Buried ants stridulate to attract rescuers. Myrmica workers and queens modulate stridulation rhythm, speed and intensity; queen signals differ from those of workers and males and may set rescue priorities. Atta can sense vibration direction.
- (b) Ciaralli S. et al. (2026). Multimodal communication in ants. Myrmecological News 36. https://antwiki.org/w/images/3/37/Ciaralli,_S._et_al._(2026)_Multimodal_communication_in_ants_(10.25849@myrmecol.news_036&025).pdf ; Directional vibration sensing in the leafcutter ant Atta sexdens. https://www.ncbi.nlm.nih.gov/pmc/articles/PMC5769659/
- (c) verified (secondary). L. niger is a formicine, and formicines generally lack a stridulatory organ; this last statement is unverified in this session.
- (d) Simulation implication: an optional "vibration/alarm" input for disturbances (digging collapse, flooding, predator); for L. niger rely on chemical alarm rather than stridulation.

### 7.7 Antennation and contact
- (a) Tandem followers must tap the leader's legs and gaster frequently for the leader to continue (6.1); trophallaxis contacts of 2 to 3 s transfer odour memories (1.10); no directional tactile code in Lasius (6.5).
- (d) Simulation implication: a contact input carrying the other ant's state (task, crop fullness, food odour, alarm) but not direction.

### 7.8 Suggested input vector (derived, not from literature)
Left and right pheromone concentration; food-odour identity (2 to 3 channels); home-range mark density; nest CO2 gradient (L, R); path-integration home vector (dx, dy, or angle plus distance); coarse visual panorama (4 to 8 sectors) multiplied by light level; light level; temperature; humidity or wetness; contact present plus contacted ant's odour/state; own crop fullness; internal age or experience counter.

---

## 8. Suggested plasticity parameters (derived from the above)

| Quantity | Value to target | Basis |
|---|---|---|
| Appetitive place learning | about 75% after 1 visit, about 95% after 3 | Grüter 2011 |
| Odour-value learning | 1 trial, about 2x faster than place | Oberhauser 2019, Czaczkes and Kumar 2020 |
| Single-trial memory lifetime | consolidates in 15 to 30 min, gone by 24 h | Mathada 2025 |
| Multi-trial memory | multi-day (48 h to 3 days) | Piqueret 2019, Wagner 2023 |
| Extinction | about 6 unrewarded trials | Piqueret 2019 (F. fusca) |
| Aversive place learning | about 5 trials | Mathada 2025 |
| Aversive odour learning | 1 trial | Wenig 2021 |
| Pheromone gain | innate; learnable down to 0, never negative | Wenig 2021 |
| Memory versus pheromone | memory wins after 1 visit in daylight | Grüter 2011, Jones 2019 |
| Pheromone reliance at night | increases; visual learning fails in darkness | Jones 2019 |
| Trail accuracy | 74% to 83% per junction | Czaczkes 2017 |
| Pheromone lifetime | about 47 min, faster when hot | Beckers 1993, van Oudenhove 2011 |
| Learning walk phase | 3 to 7 walks over about 2 days | Fleischmann 2016 (Cataglyphis) |
| Primacy | first food odour preferred about 85% | Oberhauser 2022 |

---

## Gaps

1. **Age of the nurse to forager transition in L. niger** is not documented in any source found; only the qualitative pattern (young inside, old outside) and lifespans (about 310 to 430 days) are confirmed. The 2 to 4 week figure is from other species and is unverified. Digging as an intermediate task stage was not found for L. niger.
2. **Learning walks and path integration in L. niger specifically**: no L. niger learning-walk study was found; all numbers are from Cataglyphis and Melophorus. L. niger does show home-directed search biases without vision (Bonavita 2026), which hints at path integration.
3. **Long-term memory in L. niger**: single-trial memory lasts less than 24 h; the retention of multi-trial memories in L. niger (days or weeks) was not quantified beyond overnight odour recall (Czaczkes 2014).
4. **Aversive learning about predators, flooding or rain** by individual ants: no study found. The only related data are L. niger avoiding water immersion after 5 trials (Mathada 2025). Flood responses in the literature are colony-level and appear innate.
5. **L. niger sensory physiology**: CO2, humidity and temperature receptor data are from Atta, Camponotus and Cataglyphis; the 120 ommatidia figure is from a hobbyist source.
6. **Several citations lack verified author lists or volume numbers** because Springer and Nature pages blocked direct fetching; these are marked above.
7. **Conflict between studies on pheromone modulation**: Grüter 2011 shows memory overriding pheromone, while the 2026 Insectes Sociaux study found pheromone following itself is not modulated by a prior visit. Both can be true (following strength fixed, but memory wins in a direct conflict), but the exact integration rule is not settled.
8. **Social learning in L. niger** beyond trophallaxis-delivered odour cues and pheromone trails (for example, whether naive L. niger learn routes by following experienced nestmates) was not found. A 2025 Scientific Reports paper titled "Naive ants reach a food source quicker when encountering returning..." (https://www.nature.com/articles/s41598-025-02058-z) may address this, but it could not be fetched, so its species and numbers are unverified.

# 04. Foraging, pheromone communication, task allocation and colony-level decisions

Scope: grounding a Lasius niger colony screensaver (neural-network ants, colonies evolving across generations).
Species is Lasius niger unless stated otherwise.

Confidence key:
- **verified**: the number or claim was read in an abstract, full text, or publisher/repository metadata fetched during this research.
- **verified (secondary)**: the claim was read in a fetched paper that cites the original, but the original itself was not read.
- **unverified**: the claim came only from a search-engine summary, or I could not locate the primary source text.

Method note: Springer, Wiley and Nature pages blocked direct fetching, so abstracts were pulled from Europe PMC, OpenAlex, Semantic Scholar, Crossref and figshare APIs. Open-access full texts came from Europe PMC. Several classic papers (Beckers et al. 1990, 1992, 1993; Devigne & Detrain 2002, 2006; Buffin et al. 2010) have no machine-readable abstract. For those I list only what search snippets reported, and mark those claims accordingly.

---

## 1. Trail pheromone: deposition, decay, following

### 1.1 Trail persistence (decay) in Lasius niger
- (a) Colonies formed trails to a 1 M sucrose feeder, and trail sections were later presented back to outgoing foragers. On fresh trails made by **500 ant passages**, **77%** of foragers walked the correct direction for 15 cm, against **31%** on control paper with no pheromone. Trails **decayed to control levels in 20-24 h**. Trails made with **125 or 30 passages decayed faster**. Visual landmarks added about **20%** more correct choices in the familiar room; that difference shrank to about 10% after 2 h.
- (b) Evison SEF, Petchey OL, Beckerman AP, Ratnieks FLW (2008). Combined use of pheromone trails and visual landmarks by the common garden ant Lasius niger. Behavioral Ecology and Sociobiology 63:261-267. DOI 10.1007/s00265-008-0657-6
- (c) verified (abstract read via figshare API)
- (d) Simulation implication: a strong trail's *behavioral* effect can last many hours, so decay time should scale with how much pheromone was laid, not follow one fixed short lifetime.

### 1.2 Trail persistence cited as "at least 40-60 min"
- (a) Grüter et al. write that "trail pheromones in L. niger persist for at least 40-60 minutes", citing Beckers et al. Their agent-based model uses exponential decay C(t) = C(t-1) x (100 - r)/100 with **r = 0.4% per 1 s step**, so a 60-unit deposit falls below the 0.05 detection threshold in about **2700 steps (45 min)**.
- (b) Grüter C, Schürch R, Czaczkes TJ, Taylor K, Durance T, Jones SM, Ratnieks FLW (2012). Negative feedback enables fast and flexible collective decision-making in ants. PLoS ONE 7(9):e44501. DOI 10.1371/journal.pone.0044501
- (c) verified (secondary) for the 40-60 min figure; verified for the model parameters (full text read)
- (d) Simulation implication: an evaporation half-life of about 10-20 min for weak trails reproduces lab-scale dynamics. Strong trails need either a longer half-life or saturation (see 1.1).
- Related: a search snippet said L. niger pheromone "becomes undetectable in 47 minutes at room temperature". I could not find the primary source for this. **unverified**

### 1.3 Pheromone evaporation half-life estimated from imaging (Argentine ant)
- (a) Linepithema humile groups of 1,000 workers were tracked. The analysis treats **30 min as a lower bound on the evaporation half-life**, and results were insensitive to assuming it. Mean walking speed was **slightly under 2 cm/s**, with a few ants reaching **6 cm/s**. Turning depended on pheromone within a **1 cm radius** in front of the ant, following **Weber's law**: the turning angle scales with (L - R)/(L + R). Speed was largely unaffected by pheromone. The agent-based version used 1 mm patches, 0.1 s steps, 2 patches per step, and 45-degree sensing sectors of 2 cm radius.
- (b) Perna A, Granovskiy B, Garnier S, Nicolis SC, Labédan M, Theraulaz G, Fourcassié V, Sumpter DJT (2012). Individual rules for trail pattern formation in Argentine ants (Linepithema humile). PLoS Computational Biology 8(7):e1002592. DOI 10.1371/journal.pcbi.1002592
- (c) verified (full text read)
- (d) Simulation implication: this is the best-validated sensor model available. Feed the network left and right antenna samples about 1 cm ahead, and expect it to learn a turn roughly proportional to (L - R)/(L + R).

### 1.4 Attractive vs repellent pheromone decay (Pharaoh's ant, Monomorium pharaonis)
- (a) The repellent ("no entry") pheromone's effect lasts **78 min**, against **33 min** for the short-lived attractive pheromone. The two decay at about the same rate, but the repellent's initial effect on branch choice is about twice as strong (**48% vs 25%** above control). The species uses at least 3 trail pheromones: one long-lasting attractive, one short-lived attractive and one short-lived repellent.
- (b) Robinson EJH, Green KE, Jenner EA, Holcombe M, Ratnieks FLW (2008). Decay rates of attractive and repellent pheromones in an ant foraging trail network. Insectes Sociaux 55:246-251. DOI 10.1007/s00040-008-0994-5
- (c) verified (search-result abstract text; the repository PDF fetch failed)
- (d) Simulation implication: if you add a negative pheromone channel, give it a stronger initial weight and a longer life than the short-term attractive one. Note this is not documented for L. niger.

### 1.5 "No entry" negative pheromone (Monomorium pharaonis)
- (a) Foragers mark an unrewarding branch with a repellent pheromone, the first empirical evidence of a negative trail pheromone in ants.
- (b) Robinson EJH, Jackson DE, Holcombe M, Ratnieks FLW (2005). Insect communication: 'no entry' signal in ant foraging. Nature 438:442. DOI 10.1038/438442a
- (c) verified (abstract)
- (d) Simulation implication: an optional second pheromone channel could be an evolvable trait. For L. niger realism, keep it off by default (see Gaps).

### 1.6 Where and how much L. niger deposits
- (a) Ants deposit **up to 22x more pheromone within 10 cm of the food** than when about to reach the nest. They deposit **up to 4x more** next to a feeder **100 cm** from the nest than one at **20 cm**. Ants that followed a trail to an unrewarded location did **not** increase deposition to the correct location to compete with the wrong trail.
- (b) Czaczkes TJ, Olivera-Rodriguez FJ, Poissonnier LA (2024). Ants (Lasius niger) deposit more pheromone close to food sources and further from the nest but do not attempt to update erroneous pheromone trails. Insectes Sociaux. DOI 10.1007/s00040-024-00995-y
- (c) verified (abstract via OpenAlex)
- (d) Simulation implication: give each ant inputs for "distance since food" and "distance from nest" (path integration or home-range marking strength). Deposition should fall off with distance from food and rise with total trip length.

### 1.7 Deposition rates on complex routes, and pheromone-driven negative feedback
- (a) The setup was a doubly bifurcating maze. Error rates were **32%** on alternating routes vs **3%** on repeating routes, and trail pheromone cut errors on alternating routes by **30%**. Return-trip deposition (from full text): **2.1 depositions per 5 cm** on alternating routes vs **1.5 per 5 cm** on repeating routes. Ants that had made an error laid **3.0 per 5 cm**, against **2.2 per 5 cm** for error-free ants. **High existing pheromone suppressed further deposition.**
- (b) Czaczkes TJ, Grüter C, Ellis L, Wood E, Ratnieks FLW (2013). Ant foraging on complex trails: route learning and the role of trail pheromones in Lasius niger. Journal of Experimental Biology 216:188-197. DOI 10.1242/jeb.076570
- (c) verified (abstract; per-5-cm numbers came from a WebFetch summary of the full text)
- (d) Simulation implication: realistic deposition is a few marks per 5 cm (about 1 mark per 2 cm). Make deposition probability decrease with the local pheromone already present (saturation).

### 1.8 Crowding reduces deposition
- (a) With trail width and ant numbers manipulated, **5.6-fold** fewer ants deposited pheromone in the most crowded condition than the least. After **10 encounters** with nestmate-CHC-coated beads **over 20 cm**, foragers were **45% less likely** to deposit.
- (b) Czaczkes TJ, Grüter C, Ratnieks FLW (2013). Negative feedback in ants: crowding results in less trail pheromone deposition. Journal of the Royal Society Interface 10:20121009. DOI 10.1098/rsif.2012.1009
- (c) verified (abstract)
- (d) Simulation implication: feed a "recent contacts" counter into the network. Real ants lay less when crowded, which keeps colonies from locking in.

### 1.9 Trail-following accuracy
- (a) At a T bifurcation, correct choices were **83% (naive scouts), 82% (recruits) and 74% (experienced shuttlers)**; the differences were not significant. An earlier estimate of 60-70% was attributed to method error. Inter-trial variation was very high and could not be explained by trail strength or colony.
- (b) Czaczkes TJ, Castorena M, Schürch R, Heinze J (2017). Pheromone trail following in the ant Lasius niger: high accuracy and variability but no effect of task state. Physiological Entomology 42:91-97. DOI 10.1111/phen.12174
- (c) verified (abstract)
- (d) Simulation implication: target about 75-85% correct choice at a single fork on a strong trail. Perfect following is unrealistic.

### 1.10 Branch angle and pheromone at bifurcations
- (a) With equal pheromone on both arms, the share choosing the less-deviating branch rose with asymmetry: **47% (0 deg), 54% (30), 57% (45), 66% (60), 73% (90)**. With pheromone on one arm only, **65%** chose the marked arm overall. That was 65% when pheromone was on the straighter arm and 53% when it was on the more-deviating arm (n = 570 ants; 4 colonies of about 500-1000 workers).
- (b) Forster A, Czaczkes TJ, Warner E, Woodall T, Martin E, Ratnieks FLW (2014). Effect of trail bifurcation asymmetry and pheromone presence or absence on trail choice by Lasius niger ants. Ethology 120:768-775. DOI 10.1111/eth.12248 (PMC4204274)
- (c) verified (full text summary via WebFetch; author list beyond Forster is from memory: **unverified**)
- (d) Simulation implication: the ant's movement should carry forward momentum (a straight-ahead bias) that competes with the pheromone signal at forks.

### 1.11 Stability of trail following across context
- (a) Trail following fidelity was **not** modulated by distance from nest, travel direction or recent food discovery. Path length before meeting a trail did change meander, speed and distance walked.
- (b) Poissonnier L, Winter D, Olivera-Rodriguez FF, Werneke C, Czaczkes TJ (2025). Pheromone trail following is not modulated by previous visit to food location, distance travelled, or travel direction in the ant Lasius niger. Preprint, Research Square, DOI 10.21203/rs.3.rs-7630446/v1. A 2026 Insectes Sociaux version was seen in search results (DOI 10.1007/s00040-026-01106-9) but not read.
- (c) verified (preprint abstract)
- (d) Simulation implication: a single fixed pheromone-response gain per genotype is defensible. Vary locomotion (meander) with state instead.

### 1.12 Memory vs pheromone
- (a) At a T maze, **74.6%** chose correctly after 1 visit and **95.3%** after 3 visits. When memory and pheromone conflicted, **memory overrode pheromone after a single visit**.
- (b) Grüter C, Czaczkes TJ, Ratnieks FLW (2011). Decision making in ant foragers (Lasius niger) facing conflicting private and social information. Behavioral Ecology and Sociobiology 65:141-148. DOI 10.1007/s00265-010-1020-2
- (c) verified (search-result abstract text)
- (d) Simulation implication: give ants a recurrent state or route memory. Experienced foragers should rely mostly on memory, with pheromone mattering mainly to naive recruits.

### 1.13 Synergy of memory and pheromone
- (a) Using trail pheromone and route memory together raised **walking speed by 25%** and **straightness by 30%**, and maintained pheromone deposition, but only when both were available. From full text: experienced ants with pheromone took **13.7 ± 1.5 s** for a run, against **18.3 ± 2.2 s** without pheromone and **17.8 ± 1 s** for naive ants with pheromone.
- (b) Czaczkes TJ, Grüter C, Jones SM, Ratnieks FLW (2011). Synergy between social and private information increases foraging efficiency in ants. Biology Letters 7:521-524. DOI 10.1098/rsbl.2011.0067
- (c) verified (abstract). The seconds values were reported in the WebFetch summary of Czaczkes et al. 2013 and may belong to that paper. **unverified** attribution.
- (d) Simulation implication: reward speed and straightness when memory and trail agree. This is an emergent fitness advantage the evolution can discover.

### 1.14 Light level changes reliance on pheromone
- (a) Light levels tested were 3200 lux, 10 lux and 0.007 lux. In darkness ants failed to learn the food location. As light fell, they relied more on the pheromone trail and **deposited more pheromone**.
- (b) Jones SM, Czaczkes TJ, Gallager AJ, Bacon JP (2018). Copy when uncertain: lower light levels result in higher trail pheromone deposition and stronger reliance on pheromone trails in the ant Lasius niger. bioRxiv. DOI 10.1101/473579
- (c) verified (preprint abstract)
- (d) Simulation implication: if the screensaver has day/night, night should increase pheromone weighting and deposition.

### 1.15 U-turns
- (a) Lasius niger colonies selected the shorter of two paths. U-turns contributed significantly, and the U-turn proportion depended on path geometry, length and trail strength. No numeric U-turn probabilities were recovered.
- (b) Beckers R, Deneubourg JL, Goss S (1992). Trails and U-turns in the selection of a path by the ant Lasius niger. Journal of Theoretical Biology 159:397-415. DOI 10.1016/S0022-5193(05)80686-1
- (c) Citation verified via Crossref; findings **unverified** (search-snippet summary only)
- (d) Simulation implication: allow an explicit "turn back" action and let evolution tune it. U-turns on weak or off-trail paths help shortest-path selection.
- Related (Monomorium pharaonis): Hart AG, Jackson DE (2006). U-turns on ant pheromone trails. Current Biology 16:R42-R43. DOI 10.1016/j.cub.2006.01.015. Citation verified; content not read.
- Related (Monomorium pharaonis): at trail bifurcations, the geometry itself gives polarity. Correct reorientation peaks at a bifurcation angle of about **60 degrees**. Jackson DE, Holcombe M, Ratnieks FLW (2004). Trail geometry gives polarity to ant foraging networks. Nature 432:907-909. DOI 10.1038/nature03105. verified (abstract).

### 1.16 Home-range marking (cuticular hydrocarbon footprints)
- (a) The colony-specific CHC profile is deposited passively by the tarsi on the substrate in front of the nest and on the foraging arena. The relative n-alkane content rises progressively with distance. Footprints are colony-specific only for methyl-branched alkanes. Nest inner walls carry the same hydrocarbons in different proportions.
- (b) Lenoir A, Depickère S, Devers S, Christidès JP, Detrain C (2009). Hydrocarbons in the ant Lasius niger: from the cuticle to the nest and home range marking. Journal of Chemical Ecology 35:913-921. DOI 10.1007/s10886-009-9669-6
- (c) verified (abstract)
- (d) Simulation implication: add a slow, non-evaporating "home scent" field that every walking ant increments. It builds a nest-centered gradient and a colony-specific territory map.

### 1.17 Home-range marking modulates recruitment with distance
- (a) The intensity of the recruitment trail reaching the nest declines with food distance, and declines further without home-range marking. Scouts use the marking to judge distance from the nest and local activity or risk, and adjust trail laying accordingly. Test distances were 30 cm reference vs 30, 33, 60 or 120 cm.
- (b) Devigne C, Detrain C (2006). How does food distance influence foraging in the ant Lasius niger: the importance of home-range marking. Insectes Sociaux 53:46-55. DOI 10.1007/s00040-005-0834-9
- (c) Citation verified; findings are from search snippets. **unverified** in detail.
- (d) Simulation implication: home-scent strength is a useful network input, both as a "how far out am I" cue and as a switch that triggers or sustains trail laying.
- Related: Devigne C, Detrain C (2002). Collective exploration and area marking in the ant Lasius niger. Insectes Sociaux 49. DOI 10.1007/PL00012659. Citation verified; content not read.

### 1.18 Trail pheromone chemistry
- (a) 3,4-dihydroisocoumarins were reported as a new class of ant trail pheromones. The paper is commonly cited for Lasius, but I did not confirm the L. niger compound in the text.
- (b) Bestmann HJ, Kern F, Schäfer D, Witschel MC (1992). 3,4-Dihydroisocoumarins, a new class of ant trail pheromones. Angewandte Chemie International Edition 31:795-796. DOI 10.1002/anie.199207951
- (c) Citation verified; link to L. niger **unverified**
- (d) Simulation implication: none needed beyond a single attractive trail channel.

---

## 2. Recruitment dynamics and collective choice

### 2.1 Choosing the richer source, and lock-in
- (a) Offered **1 M vs 0.1 M** sucrose simultaneously, colonies concentrate on the 1 M source. Exception: if the trail to the 0.1 M source is already well developed before the 1 M source appears, the colony stays with the poorer source.
- (b) Beckers R, Deneubourg JL, Goss S, Pasteels JM (1990). Collective decision making through food recruitment. Insectes Sociaux 37:258-267. DOI 10.1007/BF02224053
- (c) verified (publisher summary text in search results; citation verified via Semantic Scholar)
- (d) Simulation implication: this is the core emergent behavior to reproduce. With strong nonlinear trail following and no negative feedback, colonies should lock in, so a late-arriving better source goes unexploited.

### 2.2 Trail-laying modulated by food quality
- (a) Foragers modulate trail laying according to food quality, and this modulation produces collective selection of the better source.
- (b) Beckers R, Deneubourg JL, Goss S (1993). Modulation of trail laying in the ant Lasius niger (Hymenoptera: Formicidae) and its role in the collective selection of a food source. Journal of Insect Behavior 6:751-759. DOI 10.1007/BF01201674
- (c) Citation verified; content **unverified** (no abstract available; summary came from citing papers)
- (d) Simulation implication: the deposit amount (or probability) should be an output the network scales with the food quality it has just tasted.

### 2.3 Trail-laying frequency across trips
- (a) Trail-laying intensity was similar on trips to and from the nest. Trail frequency fell rapidly with each individual's trip count, and was 2-3x higher for recruiters than for recruits.
- (b) Beckers R, Deneubourg JL, Goss S (1992). Trail laying behaviour during food recruitment in the ant Lasius niger (L.). Insectes Sociaux 39:59-72. DOI 10.1007/BF01240531
- (c) Citation verified; findings **unverified** (search snippet)
- (d) Simulation implication: individual trail laying should decay over successive trips to the same source, a built-in negative feedback.

### 2.4 Crowding-based negative feedback enables switching
- (a) Six colonies of 2,400-4,700 workers were tested. With two identical feeders, the feeder leading after 5 min won in **11 of 12** trials when there was little crowding (9 or 27 feeding holes). High crowding (1 hole) prevented symmetry breaking (about **51:49**), while low crowding (27 holes) gave about 63% bias. When a 3x-larger feeder was added 15 min after the first, crowded colonies **reallocated the majority of foragers within about 10 min**, faster than pheromone decay allows.
  - Validated ABM parameters: 500 agents; leaving rate (2/1000 x agents in nest) per s; drinking 60 s; return trip about 40 s; unloading 60 s; deposit 60 units per patch; decay 0.4% per s; detection threshold 0.05; sensing 3 patches (0 and 45 degrees left and right); crowding thresholds 8, 24, 72, 216 agents.
  - Faster decay gave faster switching. Colonies of about 150 foraging agents could still switch under high crowding.
- (b) Grüter et al. (2012), PLoS ONE 7:e44501 (full citation in 1.2)
- (c) verified (full text read)
- (d) Simulation implication: this is a ready-made, validated parameter set for L. niger. A per-food-source capacity (crowding) plus a "dissatisfied" state is enough to get realistic switching.

### 2.5 Ants avoid occupied feeders
- (a) Given equal-quality, differently scented feeders, L. niger tended to avoid feeders occupied by nestmates and **deposited less pheromone** toward them. Dummy lipid-coated beads did not trigger avoidance.
- (b) Wendt S, Kleinhoelting N, Czaczkes TJ (2020). Negative feedback: ants choose unoccupied over occupied food sources and lay more pheromone to them. Journal of the Royal Society Interface 17:20190661. DOI 10.1098/rsif.2019.0661
- (c) verified (abstract)
- (d) Simulation implication: include a local nestmate density sense near food. It helps colonies spread across sources.

### 2.6 Traffic splitting under crowding (L. niger)
- (a) At low density, pheromone attraction produces one trail. At high crowding, a second route is established before flow drops, keeping food return near optimal. The mechanism is inhibitory interactions (collisions).
- (b) Dussutour A, Fourcassié V, Helbing D, Deneubourg JL (2004). Optimal traffic organization in ants under crowded conditions. Nature 428:70-73. DOI 10.1038/nature02345
- (c) verified (abstract). The species is Lasius niger per the literature; the species name was not in the abstract text I read, so that detail is **unverified**.
- (d) Simulation implication: physical collisions and head-on pushing on narrow paths should be modeled; they generate multi-lane or multi-route traffic for free.

### 2.7 Colony-size threshold for organized trail foraging (Monomorium pharaonis)
- (a) Small colonies forage in a disorganized way. Colonies of **600 ants or fewer** could not organize trail foraging to a distant feeder. The transition is first-order with hysteresis when food is hard to find.
- (b) Beekman M, Sumpter DJT, Ratnieks FLW (2001). Phase transition between disordered and ordered foraging in Pharaoh's ants. PNAS 98:9703-9706. DOI 10.1073/pnas.161285298
- (c) verified (abstract; the "600" figure is from search snippets of the paper)
- (d) Simulation implication: expect founding and small colonies not to form trails. That is realistic, not a bug.

### 2.8 Colony growth changes L. niger foraging strategy
- (a) As L. niger colonies grow, foraging shifts from individual exploitation to mass recruitment. Colony **size, not age**, drives this. Trail laying rises with incipient-nest size as minims are replaced by larger workers with bigger crops.
- (b) Mailleux AC, Deneubourg JL, Detrain C (2003). How does colony growth influence communication in ants? Insectes Sociaux 50:24-31. DOI 10.1007/s000400300004
- (c) Citation verified; findings **unverified** (search snippet)
- (d) Simulation implication: across generations, founding colonies should rely on solo foraging, with trail recruitment payoff appearing only above a few hundred workers.

### 2.9 Noise helps tracking in changing environments (Pheidole megacephala)
- (a) Unlike many mass recruiters, P. megacephala tracked the better of two sources when the environment changed. A stochastic model showed noise plays a functional role.
- (b) Dussutour A, Beekman M, Nicolis SC, Meyer B (2009). Noise improves collective decision-making by ants in dynamic environments. Proceedings of the Royal Society B 276:4353-4361. DOI 10.1098/rspb.2009.1235
- (c) verified (abstract; page numbers from memory, **unverified**)
- (d) Simulation implication: keep some exploration noise (for example a trail-following error of about 15-25%, see 1.9). It improves colony adaptability.

---

## 3. Movement, distances, loads and trophallaxis

### 3.1 Walking speed
- (a) Argentine ant: mean slightly under **2 cm/s**, maximum about **6 cm/s** (Perna et al. 2012, see 1.3). **verified**
- (a) Lasius niger: search snippets said "mean speed of Lasius niger ants is 2 cm/s" and "1.4 m/min", but I could not identify the source paper. **unverified**
- (a) Lasius niger: ants walked **2.27 mm/s slower** moving away from their start point than toward it (median-speed effect, 25 C). Khuong et al. (2013) found speed about **3x slower** on steep inclines.
  - Bonavita P, Albino M, Gautrais J, Fourcassié V, Combe M, Lacour L, Eibner S, Jost C (2026). Discovering search behaviour in black garden ant trajectories. PLoS ONE. DOI 10.1371/journal.pone.0327957. verified (full text)
  - Khuong A, Lecheval V, Fournier R, Blanco S, Weitz S, Bézian JJ, Gautrais J (2013). How do ants make sense of gravity? A Boltzmann walker analysis of Lasius niger trajectories on various inclines. PLoS ONE 8:e76531. DOI 10.1371/journal.pone.0076531. verified (abstract). The absolute speed values sat in figures and were not recovered.
- (d) Simulation implication: use about 1.5-2.5 cm/s as a base speed, a little slower when laden or walking outward, and much slower on slopes. With a 3-4 mm worker that is about 5 body lengths per second.

### 3.2 Search movement model (Lasius niger)
- (a) Exploration fits a Boltzmann walker: straight segments with exponentially distributed lengths (fitted above 10 mm), separated by reorientation events. Segment lengths were shorter when heading away from the start (odds ratio 0.46). Ants took **4.06x** longer to reach 200 mm net displacement than a naive isotropic model predicted, which amounts to area-restricted search. Tracking used a 50 cm arena, 25 C and 50% RH.
- (b) Bonavita et al. (2026) above; Khuong et al. (2013) above
- (c) verified
- (d) Simulation implication: scouts without pheromone should do a correlated random walk with a homeward bias. A good default is "run straight, then turn" with exponential run lengths.

### 3.3 Foraging distance
- (a) L. niger foraging trails can reach **2.9 m**, and workers regularly forage several meters from the nest. Honeydew foragers show high site fidelity to the same aphid group.
- (b) Search-snippet summary citing multiple sources; the primary source was not identified.
- (c) **unverified**
- (d) Simulation implication: at 2 cm/s, a 3 m one-way trip takes about 2.5 min. Lab-scale feeders are typically 20-120 cm away (Czaczkes 2024; Devigne & Detrain 2006).

### 3.4 Trip timing (lab)
- (a) The food-to-nest walk took about **40 s** on the Grüter et al. setup. Drinking took about **60 s**, and unloading in the nest about **60 s** (model values chosen to match observations).
- (b) Grüter et al. (2012), PLoS ONE (see 1.2)
- (c) verified (full text)
- (d) Simulation implication: a full cycle is about 3 min for a lab-scale trail. Model feeding and unloading as time costs.

### 3.5 Crop load and the trail-laying threshold
- (a) When scouts found food volumes larger than their crop (**3 or 6 µl**), **90%** returned immediately laying a trail. With small droplets (**0.3, 0.7, 1 µl**), many kept exploring and returned without trail laying if they found nothing more. Droplet volume set the *percentage* of trail layers, not marking intensity. The key criterion is whether the ant could ingest its own **"desired volume"**, an individual threshold.
- (b) Mailleux AC, Deneubourg JL, Detrain C (2000). How do ants assess food volume? Animal Behaviour 59:1061-1069. DOI 10.1006/anbe.2000.1396
- (c) verified (abstract)
- (d) Simulation implication: this is a direct response-threshold rule for recruitment. Each ant gets an evolvable desired-volume threshold, and it lays trail only if it filled to that threshold.

### 3.6 Desired volume is individual and stable; some ants never lay trails
- (a) Each forager's desired volume is **constant over successive trips** but varies between individuals. **14%** of foragers **never** lay trail over successive trips. Among the rest, persistence varies but is not a fixed specialization.
- (b) Mailleux AC, Detrain C, Deneubourg JL (2005). Triggering and persistence of trail-laying in foragers of the ant Lasius niger. Journal of Insect Physiology 51:297-304. DOI 10.1016/j.jinsphys.2004.12.001
- (c) verified (abstract; page numbers from memory, **unverified**)
- (d) Simulation implication: individual variation in thresholds is real, so per-ant variation in network weights or thresholds is justified. About 1 in 7 foragers never lays trail.

### 3.7 Partial crop filling
- (a) Trail-laying ants do not fill their crop to maximum and can still drink more on the way home. Partial filling may shorten trips and speed up information flow.
- (b) Mailleux AC, Deneubourg JL, Detrain C (2009). Food transport in ants: do Lasius niger foragers maximize their individual load? Comptes Rendus Biologies 332:500-507. DOI 10.1016/j.crvi.2008.10.005
- (c) verified (abstract; volume and pages **unverified**)
- (d) Simulation implication: fitness should not simply reward "fill to max". There is a real tradeoff between load size and trip frequency.

### 3.8 Ingested volumes and sugar acceptance
- (a) Search snippets gave ingested volumes of about **0.9 µl sucrose, 0.5 µl protein and 0.6 µl melezitose** (source: likely Buffin et al. 2010 or a related Mailleux/Detrain paper; **unverified**). Separately: sucrose from **0.1 to 2.5 M** triggers acceptance, and intake efficiency is enhanced at **1 M or more**, where energy intake is maximized. Melezitose (an aphid sugar) gives the highest sensitivity, and trehalose is ignored.
- (b) Detrain C, Prieur J (2014). Sensitivity and feeding efficiency of the black garden ant Lasius niger to sugar resources. Journal of Insect Physiology 64:74-80. DOI 10.1016/j.jinsphys.2014.03.010
- (c) verified for Detrain & Prieur (abstract; pages **unverified**); **unverified** for the µl values
- (d) Simulation implication: food quality can be a single scalar (energy per µl). A realistic "good" source is about 1 M sucrose.

### 3.9 Trophallaxis
- (a) Food type changes trophallaxis **frequency**, while trophallaxis **duration stays constant** across food types. The per-unit-time probability of ending an exchange is constant (memoryless), so durations are exponential. Food distribution is regulated mainly by frequency.
- (b) Buffin A, Mailleux AC, Detrain C, Deneubourg JL (2010/2011). Trophallaxis in Lasius niger: a variable frequency and constant duration for three food types. Insectes Sociaux 58:107-115 (volume and pages **unverified**). DOI 10.1007/s00040-010-0133-y
- (c) Findings verified via search snippets of the publisher abstract; exact numbers were not recovered
- (d) Simulation implication: model trophallaxis as contact events with an exponential duration (a constant stop probability per tick). Let a hunger or crop-state signal control whether an exchange starts.

### 3.10 Trophallactic network in starved L. niger subcolonies
- (a) Subcolonies of **53.4 ± 5.2** ants were tested; **44.2** took part in exchanges and made **99.0 ± 17.4** exchanges over 90 min after 1 M sucrose was offered to 4-day-starved ants. Foragers (at least 5 s at the feeder) numbered **12.2 of 53.4 (about 23%)**. Foragers made **77.4** events per colony (60.2 as donor); non-foragers made **120.6** (81.8 as recipient). About **20% of ants performed over 60% of exchanges**. An exchange was counted as mandible contact lasting more than 5 s. The network did not differ from random. Background cited in the paper: in starved colonies, a single donor passes almost its whole crop to about **100 workers**.
- (b) Planckaert J, Nicolis SC, Deneubourg JL, Sueur C, Bles O (2019). A spatiotemporal analysis of the food dissemination process and the trophallactic network in the ant Lasius niger. Scientific Reports 9:15620. DOI 10.1038/s41598-019-52019-6
- (c) verified (full text); the "about 100 workers" figure is verified (secondary)
- (d) Simulation implication: inside-nest food sharing should be heavy-tailed, with a few ants doing most exchanges. Foragers act as donors; nest workers mostly receive, and some relay.
- Related: Quque M, Bles O, Bénard A, et al. (2021). Hierarchical networks of food exchange in the black garden ant Lasius niger. Insect Science 28:825-838 (pages **unverified**). DOI 10.1111/1744-7917.12792. The study built networks from 34 experiments. Brood presence raised network resilience, and some domestics acted as intermediaries between foragers and other workers. verified (abstract)

### 3.11 Crop-load control of food flow (Camponotus sanctus)
- (a) Recipient crop loads control food flow rates. Forager crop loads regulate how often foragers make trips. No global colony-state assessment is needed.
- (b) Greenwald EE, Baltiansky L, Feinerman O (2018). Individual crop loads provide local control for collective food intake in ant colonies. eLife 7:e31730. DOI 10.7554/eLife.31730
- (c) verified (abstract)
- (d) Simulation implication: link "leave the nest to forage" to how easily the forager can unload. Full nestmates mean slow unloading, which means fewer trips. This gives realistic colony satiation.

---

## 4. Task allocation

### 4.1 Response-threshold model
- (a) An individual engages in a task with probability T(s) = s^n / (s^n + theta^n), typically with **n = 2**, where s is task stimulus intensity and theta is the individual threshold. Performing the task reduces s. Variation in theta across workers produces division of labor.
- (b) Bonabeau E, Theraulaz G, Deneubourg JL (1996). Quantitative study of the fixed threshold model for the regulation of division of labour in insect societies. Proceedings of the Royal Society B 263:1565-1569. DOI 10.1098/rspb.1996.0229. Extension: Theraulaz G, Bonabeau E, Deneubourg JL (1998). Response threshold reinforcements and division of labour in insect societies. Proceedings of the Royal Society B 265:327-332. DOI 10.1098/rspb.1998.0299
- (c) Citations verified via Crossref; the formula with n = 2 comes from search snippets, **unverified** against the paper text
- (d) Simulation implication: even with per-ant neural networks, a "stimulus in, sigmoid engagement out" motif is a proven baseline. Individual thresholds can be encoded in a bias weight.

### 4.2 L. niger recruitment thresholds are real individual thresholds
- (a) See 3.5 and 3.6. The "desired volume" threshold is individual-specific and stable, and colony regulation emerges from how thresholds are distributed across workers.
- (b) Mailleux et al. 2000, 2005
- (c) verified
- (d) Simulation implication: evolve the threshold distribution at the colony level (for example its mean and variance), not a single value.

### 4.3 Age polyethism in L. niger
- (a) Proteome and metabolome differences between young and old foragers and nest workers related **primarily to subcaste and only secondarily to age**. The design separated age from role (young foragers, old foragers, young nest workers, old nest workers).
- (b) Quque M, Brun C, Villette C, Sueur C, Criscuolo F, Heintz D, Bertile F (2023). Both age and social environment shape the phenotype of ant workers. Scientific Reports 13. DOI 10.1038/s41598-022-26515-1
- (c) verified (abstract)
- (d) Simulation implication: role can be decoupled from age. Use age as a bias toward foraging, not as a hard switch.
- Search snippets also stated that "the majority of foragers are old individuals in L. niger" and that colonies keep "10-20% foraging workers". I could not identify the sources. **unverified**
- Supporting: Kramer BH, Schaible R, Scheuerlein A (2016). Worker lifespan is an adaptive trait during colony establishment in the long-lived ant Lasius niger. Experimental Gerontology 85:18-23 (pages **unverified**). DOI 10.1016/j.exger.2016.09.008. Workers from early-stage colonies were smaller and had lower mortality in their first 400 days; later-stage workers were larger and shorter-lived. verified (abstract)

### 4.4 Spatial fidelity drives age-based task groups (Camponotus fellah)
- (a) Tracking 6 colonies for 41 days (more than 9 million interactions) revealed **3 behavioral groups**. Workers move from one group to the next as they age, and interaction structure is mediated mainly by age-related changes in spatial location.
- (b) Mersch DP, Crespi A, Keller L (2013). Tracking individuals shows spatial fidelity is a key regulator of ant social organization. Science 340:1090-1093. DOI 10.1126/science.1234316
- (c) verified (abstract)
- (d) Simulation implication: task can emerge from *where* an ant spends time (nest interior, entrance, outside). Letting the network pick a preferred zone may be simpler than explicit task labels.

### 4.5 Idle workers
- (a) Temnothorax rugatulus: the mean proportion of time inactive was **0.607**, against **0.170** on active tasks. Clustering gave **Inactives 40.8%, Foragers 9.8%, Nurses 16.8%, Walkers 32.6%**. After the 20% most active workers were removed, inactive workers became active and colony activity recovered **within 1 week**. When inactive workers were removed, they were **not** replaced. The paper states that social insect colonies typically have upwards of 50% of workers inactive at any time.
- (b) Charbonneau D, Sasaki T, Dornhaus A (2017). Who needs 'lazy' workers? Inactive workers act as a 'reserve' labor force replacing active workers, but inactive workers are not replaced when they are removed. PLoS ONE 12:e0184074. DOI 10.1371/journal.pone.0184074
- (c) verified (full text)
- (d) Simulation implication: a realistic colony has about half its workers idle at any moment. Fitness should not penalize idleness per se; reserve workers pay off when foragers die.
- Related (field confirmation, same group): Charbonneau D, Hillis N, Dornhaus A (2014). 'Lazy' in nature: ant colony time budgets show high 'inactivity' in the field as well as in the lab. Insectes Sociaux. DOI 10.1007/s00040-014-0370-6. Citation verified; content not read.
- Related (model): Hasegawa E, Ishii Y, Tada K, Kobayashi K, Yoshimura J (2016). Lazy workers are necessary for long-term sustainability in insect societies. Scientific Reports 6:20846. DOI 10.1038/srep20846. In their simulation, colonies with variable thresholds persist longer because inactive workers replace fatigued ones. verified (abstract)

### 4.6 Task switching and reallocation (Pogonomyrmex barbatus)
- (a) Workers were marked in 4 exterior tasks (foraging, patrolling, nest maintenance, midden work). In older undisturbed colonies each task had a distinct worker group; in younger colonies nest-maintenance workers were likely to forage. Under perturbations, workers tended to **switch into foraging**, and nest-maintenance workers switched to other tasks. Task switching alone did not explain colony responses, so information must pass between groups within hours.
- (b) Gordon DM (1989). Dynamics of task switching in harvester ants. Animal Behaviour 38:194-204. DOI 10.1016/S0003-3472(89)80082-X
- (c) verified (search summary consistent with the paper; citation verified via Crossref)
- (d) Simulation implication: allow one-way-biased transitions (inside -> outside tasks are easy; the reverse is rare).
- Related: Gordon DM (1996). The organization of work in social insect colonies. Nature 380:121-124. DOI 10.1038/380121a0. Gordon DM, Mehdiabadi NJ (1999). Encounter rate and task allocation in harvester ants. Behavioral Ecology and Sociobiology 45:370-377. DOI 10.1007/s002650050573. Citations verified; content not read.

### 4.7 Interaction-rate regulation of foraging (Pogonomyrmex barbatus)
- (a) Outgoing foragers leave **3-8 s** after a substantial rise in antennal contacts with returning foragers. If returns are interrupted for more than **4-5 min**, waiting foragers retreat deeper into the nest. Returns follow a Poisson process, and a simple feedback model reproduces colony foraging rates.
- (b) Pinter-Wollman N, Bala A, Merrell A, Queirolo J, Stumpe MC, Holmes S, Gordon DM (2013). Harvester ants use interactions to regulate forager activation and availability. Animal Behaviour 86:197-207 (pages **unverified**). DOI 10.1016/j.anbehav.2013.05.012. Prabhakar B, Dektar KN, Gordon DM (2012). The regulation of ant colony foraging activity without spatial information. PLoS Computational Biology 8:e1002670. DOI 10.1371/journal.pcbi.1002670. Pagliara R, Gordon DM, Leonard NE (2018). Regulation of harvester ant foraging as a closed-loop excitable system. PLoS Computational Biology 14:e1006200. DOI 10.1371/journal.pcbi.1006200
- (c) verified (abstracts)
- (d) Simulation implication: the rate of successful returners (a "returning with food" contact count) is a good nest-side network input for the decision to go out. It gives colony-level foraging regulation without any central controller.

---

## 5. Colony-level heritable variation (key for between-colony evolution)

### 5.1 Heritable restraint in foraging (Pogonomyrmex barbatus)
- (a) The population was about 300 colonies near Rodeo, NM, with colony life of about **25 years** and a mature size of about **10,000 workers** reached at about 5 years. Colonies with offspring colonies fluctuated **less** in foraging on humid days: the min/max foraging ratio was **0.51 vs 0.36** (P < 0.049), while mean foraging rate did not differ (32.7 vs 34.5 returning ants per 30 s). Colonies that forage less on dry days had more offspring colonies. Not foraging carried no survival cost (no relation between proportion of days foraged and later lifespan).
  - Transmissibility: **42 offspring colonies of 17 parents** resembled their parents in *which day* they reduced foraging (Fisher's exact P = 0.04). There was **no** parent-offspring correlation in total foraging (P = 0.4) or its SD (P = 0.5). Gordon notes that daughter queens found colonies far from parents, which rules out cultural transmission.
  - Only about 25% of colonies had daughter-founded offspring colonies.
- (b) Gordon DM (2013). The rewards of restraint in the collective regulation of foraging by harvester ant colonies. Nature 498:91-93. DOI 10.1038/nature12137. An addendum exists: Gordon DM (2017), Nature 542:260, DOI 10.1038/nature21057. **I could not read the addendum; check it before relying on the heritability result.**
- (c) verified (full text PDF read); addendum content **unverified**
- (d) Simulation implication: evidence that a colony-level *reaction norm* (how foraging responds to conditions), not the average foraging level, is transmitted to offspring and linked to fitness. This supports evolving response-to-conditions parameters between colonies.

### 5.2 Lifetime reproductive success (Pogonomyrmex barbatus)
- (a) The population was about 265 colonies aged 1-28 years. R0 = **1.69**, generation time **7.8 years**. Only **49 of 199 (about 25%)** possible parents had offspring colonies on site. The mean was **2.94 offspring colonies per parent (range 1-8)**, and there was no reproductive senescence.
- (b) Ingram KK, Pilko A, Heer J, Gordon DM (2013). Colony life history and lifetime reproductive success of red harvester ant colonies. Journal of Animal Ecology 82:540-550 (pages **unverified**). DOI 10.1111/1365-2656.12036
- (c) verified (abstract)
- (d) Simulation implication: reproductive skew between colonies is strong (about 1 in 4 reproduces). A selection scheme where roughly the top quarter of colonies produce offspring is biologically plausible.

### 5.3 Colony differences persist across years
- (a) In 95 colonies over 5 years (2016-2021), about **40%** consistently reduced foraging on low-humidity days year after year, and about **20%** never did. Differences persisted even though workers live only about a year.
- (b) Gordon DM, Steiner E, Das B, Walker NS (2023). Harvester ant colonies differ in collective behavioural plasticity to regulate water loss. Royal Society Open Science 10:230726. DOI 10.1098/rsos.230726
- (c) verified (abstract)
- (d) Simulation implication: colony "personality" should be stable across worker turnover. That is good justification for encoding behavior in a colony genome shared by all workers.

### 5.4 Physiological basis of colony differences
- (a) Foragers from colonies that reduce foraging in dry conditions lose water and motor coordination faster. These desiccation-sensitive colonies were more likely to produce offspring colonies. Hydrated foragers made more trips.
- (b) Friedman DA, Greene MJ, Gordon DM (2019). The physiology of forager hydration and variation among harvester ant (Pogonomyrmex barbatus) colonies in collective foraging behavior. Scientific Reports 9:5126 (article number **unverified**). DOI 10.1038/s41598-019-41586-3
- (c) verified (abstract)
- (d) Simulation implication: a per-colony physiological parameter (for example a cost of being outside) can underlie behavioral differences and be under selection.

### 5.5 Colony-level behavioral syndrome (Temnothorax rugatulus)
- (a) Across field and lab assays (foraging effort, response to resources, activity, response to threat, aggression), colonies consistently differed along a **risk-prone vs risk-averse** axis. Populations across North America differed in how this was maintained.
- (b) Bengston SE, Dornhaus A (2014). Be meek or be bold? A colony-level behavioural syndrome in ants. Proceedings of the Royal Society B 281:20140518. DOI 10.1098/rspb.2014.0518
- (c) verified (abstract)
- (d) Simulation implication: expect evolved colonies to differ along a boldness axis (exploration, risk-taking, aggression). A visible "colony personality" is realistic.

### 5.6 Genetic correlate of colony differences (Solenopsis invicta)
- (a) Across 21 colonies under common conditions, the most active colony had up to **10x** more active foragers and more than **16x** more workers outside the nest than the least active. Forager expression of the foraging gene (sifor) correlated with colony foraging, exploration and nectar recruitment.
- (b) Bockoven AA, Coates CJ, Eubanks MD (2017). Colony-level behavioural variation correlates with differences in expression of the foraging gene in red imported fire ants. Molecular Ecology 26:5953-5960 (pages **unverified**). DOI 10.1111/mec.14347
- (c) verified (abstract)
- (d) Simulation implication: an order-of-magnitude spread in colony activity is within the natural range, so don't clamp evolved variation too tightly.

### 5.7 Review
- Jandt JM, Bengston S, Pinter-Wollman N, Pruitt JN, Raine NE, Dornhaus A, Sih A (2014). Behavioural syndromes and social insects: personality at multiple levels. Biological Reviews 89:48-67 (pages **unverified**). DOI 10.1111/brv.12042. verified (abstract). Caution: J.N. Pruitt is a co-author, and some of his other papers were retracted for data problems. This review itself is not known to be affected (**unverified**), but avoid Pruitt-led primary data.
- Duarte A, Weissing FJ, Pen I, Keller L (2011). An evolutionary perspective on self-organized division of labor in social insects. Annual Review of Ecology, Evolution, and Systematics 42:91-110 (pages **unverified**). DOI 10.1146/annurev-ecolsys-102710-145017. verified (abstract). Relevant: it discusses combining self-organization models with evolution, which is exactly this project's design.

### 5.8 Lasius niger-specific colony variation
- (a) L. niger has a colony-specific CHC profile (Lenoir et al. 2009). Pathogen exposure changes the colony contact network (Stroeymeyt et al. 2018). I found **no** study of heritable colony-level foraging or behavioral variation in L. niger specifically.
- (b) Stroeymeyt N, Grasse AV, Crespi A, Mersch DP, Cremer S, Keller L (2018). Social network plasticity decreases disease transmission in a eusocial insect. Science 362:941-945. DOI 10.1126/science.aat4793
- (c) verified (abstract)
- (d) Simulation implication: between-colony heritable variation must be borrowed from Pogonomyrmex, Temnothorax and Solenopsis. Say so in any "scientific notes" UI.

---

## 6. Computational models with validated parameters

### 6.1 Double-bridge choice function (Argentine ant)
- (a) The probability of choosing branch A is (k + A)^n / ((k + A)^n + (k + B)^n), where A and B are the numbers of ants that previously used each branch. Commonly cited values are **k = 20, n = 2**, fitted to bridge experiments. Colonies self-organize onto the shorter branch.
- (b) Goss S, Aron S, Deneubourg JL, Pasteels JM (1989). Self-organized shortcuts in the Argentine ant. Naturwissenschaften 76:579-581. DOI 10.1007/BF00462870. Deneubourg JL, Aron S, Goss S, Pasteels JM (1990). The self-organizing exploratory pattern of the Argentine ant. Journal of Insect Behavior 3:159-168 (pages **unverified**). DOI 10.1007/BF01417909
- (c) Citations verified; the k = 20, n = 2 values are from search snippets and Perna et al.'s discussion. **unverified** against the original text.
- (d) Simulation implication: use this as a sanity test. A trained population on a two-branch setup should show a sigmoid choice curve of about this shape.

### 6.2 Weber's-law individual rule (Argentine ant)
- See 1.3. This is the only model I found where the individual response function was directly measured from pheromone maps. Perna et al. show analytically that the Weber rule plus directional noise gives the Deneubourg-type sigmoid at colony scale. verified.

### 6.3 Validated L. niger ABM (NetLogo)
- See 2.4 for the full parameter table. It was validated against 6 L. niger colonies for symmetry breaking and switching. The NetLogo file is in the paper's supplement (NetLogoFile S1). verified.

### 6.4 Analytical recruitment models
- Nicolis SC, Deneubourg JL (1999). Emerging patterns and food recruitment in ants: an analytical study. Journal of Theoretical Biology 198:575-592 (pages **unverified**). DOI 10.1006/jtbi.1999.0934. This is a multi-source trail competition model with a full bifurcation diagram of which sources get exploited; the predictions are generic across trail-laying species. verified (abstract).
- Sumpter DJT, Beekman M (2003). From nonlinearity to optimality: pheromone trail foraging by ants. Animal Behaviour 66:273-280. DOI 10.1006/anbe.2003.2224. Citation verified; content not read.
- Dodoková K, Malíčková M, Yates CA, Dussutour A, Boďová K (2024). A stochastic model of ant trail formation and maintenance in static and dynamic environments. Swarm Intelligence. DOI 10.1007/s11721-024-00237-8. The model couples off-lattice agents with on-lattice pheromone diffusion and compares one pheromone, one pheromone plus an imperfect compass, and two directional pheromones. Raising deposition for richer food had a larger effect than raising recruitment. verified (abstract).
- Detrain C, Deneubourg JL (2008). Collective decision-making and foraging patterns in ants and honeybees. Advances in Insect Physiology 35:123-173 (pages **unverified**). DOI 10.1016/S0065-2806(08)00002-7. Citation verified; content not read.

### 6.5 Ant Colony Optimization (engineering, not biology)
- Dorigo M, Maniezzo V, Colorni A (1996). Ant system: optimization by a colony of cooperating agents. IEEE Transactions on Systems, Man, and Cybernetics B 26:29-41. DOI 10.1109/3477.484436. Citation verified. ACO parameters are tuned for optimization, **not** validated against real ants; do not use them as biological values.

---

## 7. Suggested default parameter sheet (derived from the above)

| Parameter | Suggested value | Basis |
|---|---|---|
| Walking speed (unladen) | 2 cm/s (range 1-6) | Perna 2012 (Argentine ant, verified); L. niger 2 cm/s unverified |
| Speed with memory plus trail | +25% | Czaczkes 2011 |
| Sensing radius for pheromone | about 1 cm ahead, left and right sectors | Perna 2012 |
| Turning rule | angle ∝ (L - R)/(L + R) plus noise | Perna 2012 |
| Deposition frequency | about 1.5-3 marks per 5 cm on return | Czaczkes 2013 JEB |
| Deposition near food vs near nest | up to 22x higher near food | Czaczkes 2024 |
| Deposition vs food distance | up to 4x at 100 cm vs 20 cm | Czaczkes 2024 |
| Crowding suppression | down to 1/5.6 of baseline | Czaczkes 2013 Interface |
| Weak-trail decay | e-folding about 15-20 min (0.4%/s gives threshold crossing at about 45 min) | Grüter 2012 |
| Strong-trail persistence | behavioral effect up to 20-24 h | Evison 2008 |
| Fork accuracy on trail | 74-83% | Czaczkes 2017 |
| Memory learning | 75% after 1 visit, 95% after 3 | Grüter 2011 |
| Recruit threshold | individual desired crop volume, stable per ant | Mailleux 2000, 2005 |
| Never-trail-laying foragers | about 14% | Mailleux 2005 |
| Feed, return, unload times (lab) | 60 s, 40 s, 60 s | Grüter 2012 |
| Trophallaxis duration | exponential (constant stop rate) | Buffin 2010 |
| Foragers as share of workers | about 10-25% | Planckaert 2019 (23%, small subcolonies); 10-20% unverified |
| Idle workers at any time | about 40-60% | Charbonneau 2017 (Temnothorax) |
| Colony-size threshold for trails | a few hundred workers (600 in Monomorium) | Beekman 2001; Mailleux 2003 |
| Colonies that reproduce | about 25% | Ingram 2013; Gordon 2013 (Pogonomyrmex) |

---

## Gaps

1. **L. niger pheromone half-life in physical units.** No primary L. niger measurement of chemical evaporation rate was read. The 40-60 min figure is secondary (Grüter 2012 citing Beckers), the 47 min figure is unsourced, and Evison 2008's 20-24 h is a behavioral persistence measure for strong trails. Half-life should be treated as a tunable parameter between about 15 min and several hours depending on trail strength.
2. **Beckers, Deneubourg and Goss papers (1990, 1992, 1993).** Numeric values (trail-laying probabilities by sucrose concentration, U-turn rates, the fitted choice function for L. niger) were not accessible; only qualitative findings were confirmed.
3. **L. niger walking speed.** No primary source was read for an absolute L. niger speed. About 2 cm/s rests on Argentine ant data and an unsourced snippet. Speed vs temperature data for L. niger were not found.
4. **Trophallaxis numbers.** Exact durations, frequencies and volume per exchange for L. niger (Buffin et al. 2010) were not recovered; only the qualitative "constant duration, variable frequency" result was.
5. **Foraging distances in the field.** The 2.9 m trail length and "several meters" were not traced to a primary source.
6. **Negative pheromones in L. niger.** None documented in anything I read. The "no entry" signal is Monomorium-only evidence, and the L. niger literature points instead to crowding-based negative feedback (reduced deposition, avoidance of occupied feeders).
7. **L. niger age polyethism and forager fraction.** Qualitative support only (Quque 2023). The age at which L. niger workers start foraging, and the forager fraction in field colonies, were not found in primary sources.
8. **Heritable colony-level variation in L. niger.** None found. All heritability evidence comes from Pogonomyrmex barbatus (Gordon), with colony-personality evidence from Temnothorax and Solenopsis. Even Gordon 2013 shows transmission of *which conditions trigger restraint*, not of overall foraging level, and has a 2017 addendum I could not read. No narrow-sense heritability (h^2) value for a colony-level foraging trait was found.
9. **Response-threshold formula details.** The n = 2 exponent and the k = 20, n = 2 choice-function values were confirmed only from secondary sources and search snippets.
10. **Colony size and demography for L. niger** (mature worker count, worker lifespan in days, queen lifespan) are outside this file's scope and were not researched here.

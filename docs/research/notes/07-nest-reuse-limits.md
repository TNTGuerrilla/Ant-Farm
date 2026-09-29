# 07: Nest reuse, colony size limits, nest depth limits

Research notes for the Lasius niger colony simulation. Species studied is named for every finding. Preference order: Lasius niger, other Lasius, any ant.

Confidence tags:

- verified - I read the primary paper text (full text or abstract) myself.
- secondary - taken from a citing paper, review, or search-engine summary of the paper; the primary text was not read.
- unverified - hobbyist, care-sheet, or uncited claim; use only as a placeholder.

---

## 1. Reuse of abandoned nests

### 1.1 Other colonies move into abandoned mounds (Pogonomyrmex barbatus)

- (a) About 10% of P. barbatus colonies older than 2 years relocate each year (17 of 142 in 1987, 19 of 207 in 1988, 25 of 302 in 1989). Mean move distance 7 to 8 m. Some colonies built new entrances on mounds abandoned by other colonies at least 1 year earlier; 20% of all completed moves in 1989 went onto such old mounds. Workers were once seen taking stored seeds from the abandoned nest of a conspecific neighbour that had just moved. 38% to 52% of attempted moves were aborted.
- (b) Gordon, D.M. 1992. Nest relocation in harvester ants. Annals of the Entomological Society of America 85(1): 44-47. PDF: https://web.stanford.edu/~dmgordon/old2/Gordon1992Nest.pdf
- (c) verified
- (d) Simulation implication: an empty nest should be an attractor for relocating or newly founding colonies, and any food left in it is lootable by neighbours.

### 1.2 Other species occupy abandoned nests (Novomessor cockerelli in P. barbatus nests)

- (a) Large abandoned P. barbatus nests are sometimes occupied by another granivorous ant, Novomessor (Aphaenogaster) cockerelli.
- (b) Gordon 1992 (as above).
- (c) verified (single sentence, no numbers)
- (d) Simulation implication: allow heterospecific (non-player) colonies or a later Lasius generation to take over dead nests; the tunnels are a free resource.

### 1.3 Lasius flavus mounds outlive their colonies and are recolonised by new queens

- (a) Of 31 L. flavus ant-hills that produced queens in 1962 (mapped by Pontin), 26 were still prominent in 2018 (56 years later) but only 14 appeared to hold L. flavus colonies. 10 new ant-hills appeared. King argues no mound was likely continuously occupied: colonies probably die with the queen (lab max queen lifespan 23 years, Prescott 1973), and the bare soil on the mound is invaded by newly mated queens after the nuptial flight (a single colony can produce up to 410 queens a year, Pontin 1963). Some summits have shifted or split in two, consistent with re-founding.
- (b) King, T.J. 2020. The persistence of Lasius flavus ant-hills and their influence on biodiversity in grasslands. British Journal of Entomology and Natural History 33(3): 215-221. https://ora.ox.ac.uk/objects/uuid:57fbce82-4488-4745-b065-cf860e6d32af
- (c) verified (the re-founding mechanism is the author's inference, not a direct observation)
- (d) Simulation implication: after the colony dies, the nest structure persists for years; founding queens landing nearby should preferentially settle into it.

### 1.4 Abandoned L. flavus mounds shrink; about a quarter are recolonised within 8 years

- (a) Over 45 years of monitoring 200+ mounds, occupied mounds kept growing while abandoned mounds decreased in volume and some disappeared entirely. Of mounds abandoned in 2007, the majority were still empty in 2015 but 25% had been recolonised.
- (b) King, T.J. 2021. Ant-hill heterogeneity and grassland management. Ecological Solutions and Evidence 2: e12037. https://doi.org/10.1002/2688-8319.12037 ; dataset DOI 10.5061/dryad.fqz612jrm
- (c) secondary (search summary and dataset page; full paper blocked). The 25% figure is from the search summary only.
- (d) Simulation implication: recolonisation probability of an empty nest is modest (roughly 3% per year if spread over 8 years), and the structure slowly degrades if nobody maintains it.

### 1.5 Nest-site limitation drives takeover, fusion and queen adoption (Temnothorax nylanderi)

- (a) In a German deciduous forest, nest sites (twigs, acorns) decay rapidly in summer so both established colonies and founding queens face severe nest-site shortage. Consequences: unrelated colonies fuse after initial fighting; young mated queens seek adoption into alien nests instead of founding alone; after winter a temporary surplus of empty sites lets large colonies split into buds.
- (b) Foitzik, S. and Heinze, J. 1998. Nest site limitation and colony takeover in the ant Leptothorax nylanderi. Behavioral Ecology 9(4): 367-375. https://doi.org/10.1093/beheco/9.4.367
- (c) secondary (abstract via search summary)
- (d) Simulation implication: if a cavity is scarce, an empty pre-built nest has high value and may be contested.

### 1.6 Lasius niger founding queens use small burrows and stones, and cluster

- (a) After mating, L. niger queens land, shed wings and quickly find a nest site (small burrows in open soil or under stones). Given two empty chambers, queens grouped into one chamber in 91% of trials with 2 queens, 78% with 4, 76% with 8 (P < 0.0001). In nature 18% to 25% of L. niger colonies are co-founded by 2 to 5 unrelated queens.
- (b) Aron, S. and Deneubourg, J.-L. 2020. Colony co-founding in ants is an active process by queens. Scientific Reports 10: 13539. https://doi.org/10.1038/s41598-020-70497-x
- (c) verified
- (d) Simulation implication: founding queens should be attracted to existing holes (and to each other), so a dead colony's entrance is a natural landing target.

### 1.7 Where Lasius niger nests are found

- (a) In a field sample (Gosswald), 625 L. niger nests were under stones, 350 in mounds and 130 in rotting wood.
- (b) Reported via search summary of AntWiki (Lasius niger page, citing Gosswald). https://www.antwiki.org/wiki/Lasius_niger
- (c) secondary
- (d) Simulation implication: surface objects (stones) are a preferred founding and nesting feature; they also act as heat stores.

### 1.8 Social parasites take over Lasius niger and congeneric nests (Lasius umbratus)

- (a) Newly mated L. umbratus (and L. orientalis) queens enter host colonies of congeneric Lasius (hosts include L. niger, L. flavus, L. japonicus), approach the host queen and spray her with abdominal fluid (apparently formic acid); host workers then kill their own queen, accept the parasite queen and rear her brood. The host workers die off and are replaced by the parasite species.
- (b) Shimada, T., Tanaka, Y. and Takasuka, K. 2025. Socially parasitic ant queens chemically induce queen-matricide in host workers. Current Biology. https://www.cell.com/current-biology/fulltext/S0960-9822(25)01207-2 (PubMed 41253115). Host list for L. umbratus from AntWiki via search summary.
- (c) secondary (abstract via search summaries; Cell and AntWiki pages blocked)
- (d) Simulation implication: optional late-game event: a living L. niger nest can be "inherited" by another species, not only dead ones.

### 1.9 Corpse handling in Lasius niger: nestmate vs alien vs prey

- (a) Using individually marked workers, Ataya and Lenoir showed L. niger corpse transport is not ritualised and the "cemetery" is simply a refuse pile. L. niger distinguishes (1) a Drosophila corpse, which it eats, (2) a nestmate corpse, which it carries quickly out of the nest to the refuse pile, and (3) a corpse from another L. niger nest, which it ejects with aggression (attacks, bites).
- (b) Ataya, H. and Lenoir, A. 1984. Le comportement necrophorique chez la fourmi Lasius niger L. Insectes Sociaux 31: 20-33. https://link.springer.com/article/10.1007/BF02223690 . Summary taken from Renucci, Tirard and Provost 2011, Complex undertaking behavior in Temnothorax lichtensteini, Insectes Sociaux, https://doi.org/10.1007/s00040-010-0109-y
- (c) secondary
- (d) Simulation implication: incoming ants treat leftover corpses of the previous (alien) colony as "foreign": carry them out to a midden, possibly with aggressive animation, rather than eating them.

### 1.10 Oleic acid releases necrophoresis (Pogonomyrmex badius, Solenopsis)

- (a) Oleic acid on paper squares elicited corpse-carrying in P. badius indistinguishable from the response to real corpses.
- (b) Wilson, E.O., Durlach, N.I. and Roth, L.M. 1958. Chemical releaser of necrophoric behavior in ants. Psyche 65: 108-114. https://biostor.org/reference/173892
- (c) secondary
- (d) Simulation implication: model "corpse" as a chemical flag that appears some time after death; any flagged object gets carried to the midden.

### 1.11 Ants eat nestmate corpses when starved (Formica polyctena)

- (a) Fluorescently marked nestmate corpses fed to starved F. polyctena fragments were found in the crops of live workers in 3 of 5 fragments; cited unpublished work reports up to 83.9% of corpses consumed after starvation. Corpses were dismantled or had holes gnawed into the ventral gaster.
- (b) Lorincz, A., Ban, K.A., Maruzs, T. and Maak, I.E. 2025. Direct evidence for cannibalistic necrophagy as a way of nitrogen recycling in ants. Ecology and Evolution. https://doi.org/10.1002/ece3.72253
- (c) verified (abstract and results read via PMC)
- (d) Simulation implication: in a food shortage, corpses (and brood) become a protein source instead of midden waste.

### 1.12 Lasius niger queens cannibalise infected brood and recycle it into eggs

- (a) L. niger founding queens cannibalised up to 92% of fungus-infected brood versus 6% of healthy brood; cannibalising queens later laid 55% more eggs than controls.
- (b) Reported in press summaries (University of Oxford news 2024-09-24) of a 2024 Current Biology paper: "Ant queens cannibalise infected brood to contain disease spread and recycle nutrients." https://www.cell.com/current-biology/fulltext/S0960-9822(24)01001-7
- (c) secondary
- (d) Simulation implication: brood in a failing nest is a resource; brood left in a dead nest could be eaten by incoming ants (not observed directly, see Gaps).

### 1.13 How long empty nests persist

- (a) P. badius: vacated nest discs ("ghosts") remain visible for more than a year until levelled by wind and rain (Tschinkel 2014). Excavated research nests were refilled and hard to detect after about a year (Tschinkel 2013). Ants themselves backfill chambers: P. imparis chambers near the surface were found filled with red clay sand from much deeper chambers; water can also wash fine material into chambers (Tschinkel 2003). L. flavus mounds persist for decades (see 1.3).
- (b) Tschinkel, W.R. 2014. Nest relocation and excavation in the Florida harvester ant, Pogonomyrmex badius. PLoS ONE 9(11): e112981. https://doi.org/10.1371/journal.pone.0112981 ; Tschinkel, W.R. 2013. Florida harvester ant nest architecture, nest relocation and soil carbon dioxide gradients. PLoS ONE 8(3): e59911. https://doi.org/10.1371/journal.pone.0059911 ; Tschinkel, W.R. 2003. Subterranean ant nests: trace fossils past and future? Palaeogeography, Palaeoclimatology, Palaeoecology 192: 321-333. https://doi.org/10.1016/S0031-0182(02)00690-9
- (c) verified
- (d) Simulation implication: give each empty tunnel cell a decay timer on the order of 1 year (sandy soil) to decades (mounds); rain events accelerate infill; upper chambers collapse first.

### 1.14 Colonies of P. badius move about once a year and rebuild a copy

- (a) 400+ colonies tracked 2010 to 2013: mean 0.72 moves per colony in 2012, 1.15 in 2013 (range one move in 2 years to 4 per year); moves average 4.1 m, rarely over 10 m, random direction; peak July when over 1% of colonies moved per day; move takes 4 to 6 days (up to 14). A large colony excavates 5 to 6 L of sand (about 20 kg). Seeds, brood and charcoal are carried to the new nest. The new nest replicates the old one's architecture.
- (b) Tschinkel 2014 (as above).
- (c) verified
- (d) Simulation implication: relocation (and hence nest abandonment) is a normal event, not only a death event; a moving colony leaves an empty nest behind.

---

## 2. Limits to colony size

### 2.1 Lasius niger colony sizes and timelines

- (a) Mature L. niger colonies are monogynous; typical size 4,000 to 7,000 workers, rare colonies to 40,000; some sources give 15,000 to 40,000 for mature wild colonies, and in an exceptional population a monogynous nest reached about 60,000. About 10,000 workers are needed before gynes (new queens) are produced; first sexuals about 3 years after founding. Gynes are produced when the worker to larva ratio is high.
- (b) Wikipedia "Black garden ant"; AntWiki Lasius niger; ScienceDirect Topics "Lasius" overview (all via search summaries, primary sources not identified).
- (c) secondary to unverified (numbers vary between sources)
- (d) Simulation implication: target equilibrium around 5,000 to 10,000 workers with alate production switching on above roughly 10,000 (or a scaled equivalent).

### 2.2 Lasius niger queen and worker lifespan

- (a) L. niger queens: maximum lifespan 28 years (lab). Workers in early-stage colonies: mean 434 days (max 1,129); intermediate-stage colonies: mean 309 days (max 1,094). Early colonies make smaller, longer-lived workers.
- (b) Kramer, B.H., Schaible, R. and Scheuerlein, A. 2016. Worker lifespan is an adaptive trait during colony establishment in the long-lived ant Lasius niger. Experimental Gerontology (2016). PubMed 27620822, https://pubmed.ncbi.nlm.nih.gov/27620822/
- (c) secondary (abstract via search summary)
- (d) Simulation implication: steady-state colony size is roughly (worker production per day) x (mean worker lifespan, about 300 to 450 days); size stops growing when births equal deaths.

### 2.3 Lasius niger egg-laying rate

- (a) No peer-reviewed eggs-per-day figure for L. niger was found. Hobbyist sources only say queens begin laying 1 to 3 days after sealing in and that output rises sharply once workers appear.
- (b) AntWiki and hobbyist care pages via search summaries.
- (c) unverified
- (d) Simulation implication: treat queen egg rate as a tunable parameter that scales with brood demand (see 2.5), not a fixed constant.

### 2.4 Declining per-capita productivity with colony size (Solenopsis invicta)

- (a) Fire ant colonies grow roughly logistically: half size at 2.5 to 3.5 years, maximum about 220,000 workers at 4 to 6 years. All production rates increase with colony size but less than proportionally, so efficiency and natality rate fall. Tschinkel states the declining worker efficiency (Michener 1964) "is a major factor in limiting the size of colonies." Fourth-instar larvae stimulate queen egg laying in a log-log relation; efficiency (eggs per hour per larva) falls continuously as larvae go from 1 to 10,000. In field colonies natality rose 3.6-fold per 10-fold increase in larvae; lab queen egg rate rose 4.9-fold per 10-fold larvae. In founding groups, brood per queen fell 50% or more as queens increased from 1 to 10.
- (b) Tschinkel, W.R. 1993. Sociometry and sociogenesis of colonies of the fire ant Solenopsis invicta during one annual cycle. Ecological Monographs 63(4): 425-457. https://doi.org/10.2307/2937154 ; cites Tschinkel 1988, Social control of egg-laying rate in queens of the fire ant, Physiological Entomology 13: 327-350.
- (c) verified
- (d) Simulation implication: make brood output per worker a sublinear function of colony size (for example output proportional to N^0.5 to N^0.8), which automatically yields logistic-looking growth.

### 2.5 Metabolic and brood scaling (Temnothorax rugatulus, Pogonomyrmex californicus)

- (a) T. rugatulus lab colonies: colony metabolic rate scales with colony size^0.78; brood number scales with worker number^0.49. P. californicus whole colonies: metabolic rate scales with colony mass^0.75 (hypometric).
- (b) Cao, T.T. and Dornhaus, A. 2013. Larger laboratory colonies consume proportionally less energy and have lower per capita brood production in Temnothorax ants. Insectes Sociaux 60: 1-5. https://doi.org/10.1007/s00040-012-0256-4 ; Waters, J.S., Holbrook, C.T., Fewell, J.H. and Harrison, J.F. 2010. Allometric scaling of metabolism, growth, and activity in whole colonies of the seed-harvester ant Pogonomyrmex californicus. American Naturalist 176: 501-510. https://doi.org/10.1086/656266
- (c) secondary
- (d) Simulation implication: a brood-per-worker exponent near 0.5 is a defensible default for the sublinear production curve.

### 2.6 Michener's paradox across Temnothorax species

- (a) Per-capita productivity declined with colony size in 6 of 8 cavity-nesting species studied (T. crassispinus was an exception). Resource availability or nest-site limitation are proposed causes.
- (b) Kramer, B.H., Scharf, I. and Foitzik, S. 2014. The role of per-capita productivity in the evolution of small colony sizes in ants. Behavioral Ecology and Sociobiology 68: 41-53. https://doi.org/10.1007/s00265-013-1620-8
- (c) secondary
- (d) Simulation implication: the decline is general but not universal; do not make it too steep.

### 2.7 Sharp switch from workers to sexuals (Solenopsis invicta) vs bang-bang theory

- (a) Percent of annual production invested in sexuals rose sharply in colonies of 20,000 to 50,000 workers, then held at about 33% for the rest of colony growth; in spring, colonies put 50% of daily production into sexuals, fuelled by stored worker fat, and colonies shrank to a midsummer minimum (bigger colonies shrank more). Tschinkel: the ergonomic to reproductive transition is sharp, and colonies must grow to produce more sexuals.
- (b) Tschinkel 1993 (as above).
- (c) verified
- (d) Simulation implication: once above a size threshold, divert a fixed fraction (about one third per year, concentrated in a spring pulse) of production to alates; this caps worker growth.

### 2.8 Bang-bang allocation model (theory, annual colonies)

- (a) Macevicz and Oster predicted that annual colonies maximise fitness by pure worker production (ergonomic phase) followed by a single switch to pure sexual production. Empirically, graded allocation (both at once) is common and is usually explained as bet-hedging. Perennial ants such as L. niger are better described by the Tschinkel pattern (threshold then steady fraction each year).
- (b) Macevicz, S. and Oster, G. 1976. Modeling social insect populations II: Optimal reproductive strategies in annual eusocial insect colonies. Behavioral Ecology and Sociobiology 1: 265-282. Context from: Adaptive dynamic resource allocation in annual eusocial insects: environmental variation will not necessarily promote graded control (2007/2008, BMC Ecology/Evolution, PMC2242787, https://pmc.ncbi.nlm.nih.gov/articles/PMC2242787/). Also Oster, G.F. and Wilson, E.O. 1978. Caste and Ecology in the Social Insects. Princeton University Press.
- (c) secondary
- (d) Simulation implication: annual cycle: grow workers most of the season, then a reproductive pulse; for a perennial species repeat every year once mature.

### 2.9 Colony size and reproduction probability (Pogonomyrmex occidentalis)

- (a) Over 4 years, the probability that a colony reproduced increased significantly with colony size, but the total reproductive biomass of reproducing colonies was unrelated to size; the size threshold varied between years.
- (b) Cole, B.J. and Wiernasz, D.C. 2000. Colony size and reproduction in the western harvester ant, Pogonomyrmex occidentalis. Insectes Sociaux. https://doi.org/10.1007/PL00001711
- (c) secondary
- (d) Simulation implication: make the alate switch a probabilistic threshold that depends on colony size and on that year's food.

### 2.10 Neighbours limit growth and reproduction (Lasius flavus, L. niger, Solenopsis, Pogonomyrmex)

- (a) Crowded L. flavus colonies have lower reproductive output, and removing neighbours (including L. niger) increases sexual production (Pontin 1961, 1969). Removing neighbours of Veromessor pergandei and P. californicus increased alate production. For S. invicta, killing colonies in plots made surviving colonies just outside expand territory and grow faster than same-size controls. Removing mature colonies increases new colony establishment; mature colonies kill founding queens and incipient colonies, even digging them out of their founding chambers. Territorial fights can wipe out colonies, especially when one is much larger.
- (b) Adams, E.S. 2016. Territoriality in ants (Hymenoptera: Formicidae): a review. Myrmecological News 23: 101-118 (read full text). Primary: Pontin, A.J. 1961. Population stabilization and competition between the ants Lasius flavus (F.) and L. niger (L.). Journal of Animal Ecology 30: 47-54; Adams, E.S. and Tschinkel, W.R. 2001. Mechanisms of population regulation in the fire ant Solenopsis invicta. Journal of Animal Ecology 70. https://doi.org/10.1046/j.1365-2656.2001.00501.x
- (c) verified (review text); primary papers secondary
- (d) Simulation implication: foraging area overlap with neighbours reduces food intake per worker, which is a strong emergent brake on size; a neighbour's death frees territory and boosts growth.

### 2.11 Is there evidence of colonies being "too big"?

- (a) Tschinkel (1993) reports that the spring sexual-production decline is larger in bigger colonies and that efficiency falls well before half size. Search summaries of older Lasius literature state that very large colonies with workers far in excess of brood-rearing needs are uncommon, possibly because sexual production destabilises the population. Per-capita decline (2.4 to 2.6) means extra workers add less and less, but no study found here shows total output falling with size.
- (b) Tschinkel 1993 (verified); "too big" Lasius statement from ScienceDirect Topics summary (source not identified).
- (c) verified (Solenopsis part); unverified (Lasius statement)
- (d) Simulation implication: do not hard-cap size; let diminishing returns plus territory and alate drain produce a soft ceiling.

---

## 3. Limits to nest depth

### 3.1 Lasius niger and L. neoniger typical depths

- (a) L. neoniger (Wisconsin, field casts): galleries and chambers concentrated in the upper 0.3 m with a few vertical galleries to about 0.7 m; galleries 1.5 to 5.0 mm diameter; chambers 10 to 20 mm wide and 30 to 50 mm long; nest volume 20 to 250 cm3.
- (b) Wang, D., McSweeney, K., Lowery, B. and Norman, J.M. 1995. Nest structure of ant Lasius neoniger Emery and its implications to soil modification. Geoderma 66: 259-272. https://doi.org/10.1016/0016-7061(94)00082-L
- (c) verified (abstract)
- (d) Simulation implication: for a Lasius nest use a dense zone 0 to 30 cm and sparse deep shafts to about 70 cm.

### 3.2 Lasius niger: temperature drives depth in young colonies

- (a) 17 young L. niger colonies (mean 106 workers at start) dug in 24 cm soil columns for 100 days at three surface temperatures (daytime surface about 22, 36, 48 C). Max depth: 13.7 +/- 3.5 cm (mild), 17.2 +/- 3.2 (medium), 19.3 +/- 2.2 cm (high); no nest reached the column bottom. Maximum depth was reached by day 7 and did not change after; later digging enlarged chambers. Soil excavated: 12.8 g (mild), 26.4 g (medium), 19.5 g (high). Under high temperature chambers sat deeper and some upper chambers were refilled. Colonies grew to 253 +/- 122 workers (mild) vs 503 +/- 93 (medium and high). L. niger CTmax measured at 47.5 C.
- (b) Garcia Ibarra, F., Jouquet, P., Bottinelli, N., Bultelle, A. and Monnin, T. 2024. Experimental evidence that increased surface temperature affects bioturbation by ants. Journal of Animal Ecology 93: 319-332. https://doi.org/10.1111/1365-2656.14040
- (c) verified (full text read)
- (d) Simulation implication: depth target should be driven by surface heat: hotter surface means deeper brood chambers and backfilling of hot upper chambers.

### 3.3 Lasius niger: nest volume scales with population, digging self-limits

- (a) Final nest volume and digging rate grow almost proportionally with the number of ants; digging acts as negative feedback on nest enlargement (driven by nest volume and by changes in ants after digging). Digging shows a fast amplification phase then a saturation phase.
- (b) Rasse, P. and Deneubourg, J.L. 2001. Dynamics of nest excavation and nest size regulation of Lasius niger (Hymenoptera: Formicidae). Journal of Insect Behavior 14(4): 433-448. https://doi.org/10.1023/A:1011163804217
- (c) secondary
- (d) Simulation implication: digging stimulus = crowding (ants per unit nest volume); when volume per ant reaches a target, digging stops. This is the core self-limiting rule.

### 3.4 Lasius niger overwintering depth

- (a) Hobby and review snippets say ants overwinter from 0 cm to 1.5 m depending on species and site, and that L. niger soil nests can reach up to 2 m; workers concentrate in a few deep central chambers in winter.
- (b) Search summaries (ResearchGate page for "Effects of overwintering temperature on the survival of the black garden ant Lasius niger", Journal of Thermal Biology 2015, plus care sheets).
- (c) unverified
- (d) Simulation implication: allow a seasonal deepening (winter chamber) below frost depth, but the 2 m figure needs confirmation.

### 3.5 Pogonomyrmex badius: depth scales with colony size

- (a) Incipient colonies 29 to 37 cm deep (single chamber at end of a 20 cm tunnel when newly founded); mature colonies commonly 2.5 to 3.0 m (deepest 3.06 m). Each 10-fold increase in workers gives a 2.4-fold increase in max depth (R2 = 72%). Up to 150 chambers; half of chamber area in the top quarter (top-heavy); chambers 2 to 4 cm apart near the surface, 20 to 30 cm apart at depth; chamber height about 1 cm. Soil removed: about 40 g (incipient) to 40 kg (largest). Up to 8,000 workers. Brood kept deep (98% below 80 cm); foragers in upper 15 cm.
- (b) Tschinkel, W.R. 2004. The nest architecture of the Florida harvester ant, Pogonomyrmex badius. Journal of Insect Science 4: 21. https://doi.org/10.1093/jis/4.1.21 ; Tschinkel, W.R. and Hanley, N. 2017. Vertical organization of the division of labor within nests of the Florida harvester ant, Pogonomyrmex badius. PLoS ONE 12: e0188630. https://doi.org/10.1371/journal.pone.0188630
- (c) verified
- (d) Simulation implication: depth_max proportional to N^0.38 (since 10^0.38 is about 2.4); chamber spacing increases with depth.

### 3.6 Other species depths vs colony size (Tschinkel casts)

- (a) Prenolepis imparis: up to 11,000 workers, nests 2.5 to 3.6 m (up to 4 m), no chambers shallower than 60 cm to 1 m, most chambers in the bottom half, which stays 16 to 24 C year-round. Solenopsis invicta: up to 250,000 workers, nests up to 2 m. Formica pallidefulva: small colonies, nests 30 to 45 cm, volume declines exponentially with depth, nest volume correlates with worker number (R2 = 0.87). Pheidole morrissi up to 6,000 workers, chambers concentrated near surface.
- (b) Tschinkel 2003 (Palaeo 192: 321-333, read full text); Tschinkel, W.R. 1987. Seasonal life history and nest architecture of a winter-active ant, Prenolepis imparis. Insectes Sociaux 34. https://doi.org/10.1007/BF02224081 (secondary for temperature numbers); Mikheyev, A.S. and Tschinkel, W.R. 2004. Nest architecture of the ant Formica pallidefulva: structure, costs and rules of excavation. Insectes Sociaux 51: 30-36. https://doi.org/10.1007/s00040-003-0703-3 (read full text)
- (c) verified (depths and colony sizes); secondary (P. imparis temperature band)
- (d) Simulation implication: depth is species-specific and not just a function of colony size; a Lasius colony of 10,000 should not dig as deep as P. imparis.

### 3.7 Water table is a hard floor

- (a) In north Florida flatwoods the water table lies 20 cm to 1.2 m below ground; in sandhills it is over 4 m. Deep-nesting species (Solenopsis geminata, Dorymyrmex bureni, Prenolepis imparis, all with nests 4 m or more) are entirely absent from flatwoods; P. badius (2 to 3 m nests in sandhills) is restricted to the highest parts of the flatwoods. Species with nests under 1.5 m (Formica spp., Trachymyrmex septentrionalis, Camponotus socius) persist.
- (b) Tschinkel, W.R., Murdock, T., King, J.R. and Kwapich, C. 2012. Ant distribution in relation to ground water in north Florida pine flatwoods. Journal of Insect Science 12: 114. https://doi.org/10.1673/031.012.11401
- (c) verified (abstract/summary)
- (d) Simulation implication: a saturated soil layer at the bottom of the world is an absolute depth limit; a rising water table (after rain) floods deep chambers.

### 3.8 CO2 rises with depth but P. badius does not use it to decide depth

- (a) Soil CO2 rises roughly logarithmically with depth: in sandy upland about 0.25 to 0.8% down to 175 cm, rarely over 1% except near the water table; just above a shallow water table 4 to 5%. Tschinkel 2003 reported CO2 at nest bottom about 5 times the surface. Experiments that eliminated or reversed the CO2 gradient produced identical nests: no evidence P. badius uses CO2 for excavation depth or vertical positioning.
- (b) Tschinkel 2013 (PLoS ONE 8: e59911), verified; Tschinkel 2003, verified.
- (c) verified
- (d) Simulation implication: CO2 is not a strong depth limiter for small, non-fungus-growing ants; do not make it the main brake for Lasius.

### 3.9 CO2 in nests of other ants, and tolerance

- (a) Field nest CO2: 0.2% in P. badius; 1.5 to 4.5% in Atta capiguara and A. laevigata; 5.7% in A. vollenweideri (atmosphere about 0.04%); Atta sexdens initial nests median 1.40%. Acromyrmex lundii digging rates were unchanged from atmospheric up to 7% CO2 and fell only at 11%; workers preferred digging at 1% and avoided 4%. Mangrove ant Camponotus anderseni sustains aerobic respiration up to about 15% CO2. For L. niger and L. flavus, only a wetland study noting nests contributed measurable soil CO2 emission was found.
- (b) Sousa, K.K.A. et al. 2021. Carbon dioxide levels in initial nests of the leaf-cutting ant Atta sexdens. Scientific Reports 11. https://doi.org/10.1038/s41598-021-00099-8 (verified via PMC8523712); Romer, D., Halboth, F., Bollazzi, M. and Roces, F. 2018. Underground nest building: the effect of CO2 on digging rates, soil transport and choice of a digging site in leaf-cutting ants. Insectes Sociaux 65: 305-313. https://doi.org/10.1007/s00040-018-0615-x (secondary); C. anderseni figures from search summary of "The mangrove ant, Camponotus anderseni, switches to anaerobic respiration in response to elevated CO2 levels", Journal of Insect Physiology 2007 (secondary).
- (c) verified (field CO2 values); secondary (digging tolerance, C. anderseni)
- (d) Simulation implication: if CO2 is modelled at all, use it as a mild digging penalty that only bites above several percent (deep, crowded, poorly ventilated chambers).

### 3.10 Energetic and time cost of carrying soil up (Formica pallidefulva)

- (a) A worker (7.54 mg live mass) carries about 2.26 mg of sand per trip (about 30% of body mass). Trip time rises linearly with depth: loading time 15 +/- 3 s, climbing speed parameter 3.1 +/- 0.54 cm/s. Total work to excavate a field nest: 130 to 8,800 J (median 790 J), equal to 10,000 to 460,000 ant-hours (median 81,000) at 28 C, about 158 h per worker. If a colony moved twice a year, excavation would take about 20% of its energy intake and at least 6% of worker time. Soil excavated per unit time increases with soil temperature and moisture; 0.4% CO2 had no detectable effect.
- (b) Mikheyev and Tschinkel 2004 (as above), full text read.
- (c) verified
- (d) Simulation implication: cost per load = constant + k x depth; deeper digging is slower and costlier, a natural brake that grows linearly with depth.

### 3.11 P. badius excavation effort

- (a) A mature colony digs about 20 kg of sand in 4 to 5 days during a move, i.e. sand equal to about 1,000 times the mass of all workers, mostly carried up by foragers.
- (b) Tschinkel 2004 and 2014 (as above).
- (c) verified
- (d) Simulation implication: digging is fast when the whole colony commits; the brake is not absolute capacity but opportunity cost against foraging.

---

## Simulation rules (proposed emergent limits)

These are suggested mechanics that make growth and depth self-limiting without hard caps. Numbers are starting points for tuning.

### Nest reuse

1. Dead nests persist. When a colony dies, its tunnels stay as "abandoned" cells with a decay timer (default: upper 10 cm collapses/fills over about 1 simulated year; deeper cells last longer; rain events advance decay). Source: 1.3, 1.4, 1.13.
2. Attractor for founders. A newly mated queen that lands within some radius of an abandoned entrance moves into it with high probability (entrance acts like a pre-dug burrow, saving her digging cost). Multiple queens landing near the same hole may co-found (18% to 25% baseline). Source: 1.6.
3. Recolonisation chance. Each season, an abandoned nest has a small chance (about 3% to 10% per year) of being claimed by a founding queen or by a relocating neighbour colony. Source: 1.1, 1.4.
4. Neighbour looting. Any surviving food store in a dead nest can be scavenged by nearby colonies. Source: 1.1.
5. Cleanup by newcomers. Incoming ants treat old corpses as alien: carry them to a midden outside, with aggressive animation (bite/drag). If the new colony is food-starved, corpses and leftover brood are eaten instead (protein source). Source: 1.9, 1.11, 1.12.

### Colony size

6. Sublinear production. Brood produced per day = a x N^b with b about 0.5 to 0.8 (N = workers). Queen egg rate follows larval demand, not a fixed max. Source: 2.4, 2.5.
7. Births equal deaths. Workers die after a lifespan drawn around 300 to 450 days plus hazard (predation, foraging). With sublinear births and linear deaths, the colony converges to an equilibrium size. Source: 2.2, 2.4.
8. Territory brake. Food intake depends on unshared foraging area; overlap with neighbours or other species splits the food. Neighbour death frees area and speeds growth. Source: 2.10.
9. Alate drain. Above a size threshold (L. niger about 10,000 workers, or scaled), each year divert about one third of production to alates, concentrated in a pre-flight pulse; colony shrinks after the flight. Switch is probabilistic and food dependent. Source: 2.1, 2.7, 2.9.
10. Founding suppression. Mature colonies kill founding queens found inside their territory, so new colonies appear mostly in gaps and on abandoned nests. Source: 2.10.

### Nest depth

11. Crowding drives digging. Dig when ants per unit nest volume exceed a target; stop when below it (Rasse and Deneubourg). Nest volume thus tracks population. Source: 3.3, 3.6.
12. Depth cost grows with depth. Each soil load costs a fixed handling time plus travel time proportional to depth; workers weigh this against foraging. Deep digging naturally slows. Source: 3.10.
13. Thermal target. Brood chambers seek a preferred temperature; the soil temperature profile (hot, variable at surface, stable at depth) sets how deep brood goes. Hot summers push brood deeper and cause refilling of hot upper chambers; winter pulls the colony to its deepest chambers. Source: 3.2, 3.6.
14. Depth scales weakly with size. Target max depth proportional to N^0.38 (from P. badius), scaled so a 5,000 to 10,000 worker L. niger nest is dense in the top 30 cm with a few shafts to about 70 cm. Source: 3.1, 3.5.
15. Hard floors. Water table (and optionally rock/clay layers with high dig cost) are absolute limits; flooding after rain kills or evicts ants in the deepest cells. Source: 3.7.
16. Optional CO2. Model CO2 as rising with depth and with ants per chamber; apply a digging penalty only above several percent. Keep it weak for Lasius. Source: 3.8, 3.9.
17. Top-heavy shape. More and larger chambers near the surface, spacing increasing with depth. Source: 3.5, 3.6.

---

## Gaps

- No peer-reviewed Lasius niger egg-laying rate (eggs per day) was found; needs a targeted search (for example German literature, or Kutter and Stumper, or Sommer and Holldobler 1995 founding data).
- No measured Lasius niger mature field nest depth from excavation or casting; the only field cast data are for L. neoniger (0.3 m dense, 0.7 m max). The "up to 2 m" L. niger claim is unverified.
- No direct observation of what incoming ants do with old brood or corpses in an inherited nest; rules 5 are extrapolated from nestmate/alien corpse handling and starvation cannibalism.
- The source for "10,000 workers needed before gynes" in L. niger was not identified.
- No CO2 measurements inside Lasius niger nests; the CO2 tolerance of Lasius is unknown.
- No data on how quickly abandoned Lasius niger (non-mound) nests collapse in loam or clay soils; persistence data are from Florida sand (P. badius) and L. flavus mounds.
- Pontin 1961 and Adams and Tschinkel 2001 primary texts were not read; effect sizes for neighbour density on L. niger growth are not quantified here.
- Choe et al. 2009 (PNAS, Argentine ants: life-associated chemicals inhibit necrophoresis) appeared in search results but was not read; it would give timing of when a corpse starts being treated as dead.

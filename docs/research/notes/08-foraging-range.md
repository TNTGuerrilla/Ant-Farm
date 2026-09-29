# 08. Foraging range, territory, nest spacing and aphid placement (Lasius niger focus)

Research notes for sizing the simulated world of the Lasius niger screensaver (2D vertical cross-section: underground nest plus a surface strip that foragers travel along). Compiled 2026-09-29.

Confidence key (same scheme as notes 01 to 07):
- **verified**: I read the number in the source itself (full text, abstract, or publisher/indexer record) during this session.
- **secondary**: the number comes from a paper I read that cites the original, or from a search-engine summary of a source I located but could not open. The original was not read.
- **unverified**: search snippet or popular source without a traceable primary citation. Placeholder only.

Species are named on every finding. Priority: Lasius niger, then other Lasius, then Formica.

Honest summary up front: there is surprisingly little published field data giving L. niger foraging distances in metres. Most L. niger foraging papers are lab studies with food 20 to 120 cm away. The field-scale picture below is assembled from nest density and territory data (which constrain spacing), a few direct statements, and congeners (L. fuliginosus, Formica) that give upper bounds.

---

## 1. Foraging distance from the nest

### 1.1 L. niger "regularly forages several metres from the nest"
- (a) Lasius niger regularly forages at locations several metres from the nest, and in trees several metres high. The authors state it is unknown whether recruitment keeps rising for food more than 1 m away. Their own experiments used food 20 cm and 100 cm from the nest; ants deposited up to 4x more pheromone near food at 100 cm than at 20 cm, and up to 22x more within 10 cm of food than near the nest.
- (b) Czaczkes TJ, Olivera-Rodriguez FJ, Poissonnier LA (2024). Ants (Lasius niger) deposit more pheromone close to food sources and further from the nest but do not attempt to update erroneous pheromone trails. Insectes Sociaux 71. DOI 10.1007/s00040-024-00995-y. Full text: https://epub.uni-regensburg.de/59034/ . The "several metres" statement cites Devigne C, Detrain C (2005). Foraging responses of the aphid tending ant Lasius niger to spatio-temporal changes in aphid colonies Cinara cedri. Acta Zoologica Sinica 51: 161 to 166 (not read). The "trees several metres high" is the first author's personal observation.
- (c) verified (sentence read in full text); the underlying 2005 field numbers are secondary/not read.
- (d) Simulation implication: typical L. niger trail lengths should be on the order of 1 to a few metres, not tens of metres; trails may continue vertically up plants and trees for metres.

### 1.2 The "2.9 m trail" figure
- (a) Search summaries repeatedly state that L. niger foraging trails "can reach up to 2.9 m long". I could not find this number in any paper I opened (it is not in Czaczkes et al. 2024). It may come from Devigne & Detrain 2005 (Cinara cedri field study) but that is a guess.
- (b) No traceable source.
- (c) unverified.
- (d) Simulation implication: fine as a "typical long trail" value, but do not treat it as a hard maximum.

### 1.3 Expected patrol distance derived from territory size
- (a) Hertzog et al. argued that with an average L. niger territory of 36 m2 (citing Elmes 1971), patrolling ants would be recorded up to 18 m from their nests, with numbers declining as distance to the nest increases (citing Devigne & Detrain 2006). Note: a circular 36 m2 territory has a radius of only about 3.4 m, so their 18 m figure looks like a generous upper bound rather than a derived value (my calculation).
- (b) Hertzog LR, Ebeling A, Meyer ST, Eisenhauer N, Fischer C, Hildebrandt A, Wagg C, Weisser WW (2016). High survival of Lasius niger during summer flooding in a European grassland. PLoS ONE 11(11): e0152777. DOI 10.1371/journal.pone.0152777 (full text via Europe PMC, PMC5112897). Original territory figure: Elmes GW (1971). An experimental study on the distribution of heathland ants. Journal of Animal Ecology 40: 495 to 499. DOI 10.2307/3258 (not read).
- (c) verified (sentence read); 36 m2 is secondary (Elmes original not read).
- (d) Simulation implication: territory radius about 3 to 4 m is the best-supported typical value; allow rare excursions up to about 10 to 18 m.

### 1.4 Congener upper bound: Lasius fuliginosus trunk trails 20 to 30 m
- (a) L. fuliginosus foraging trails form a network of trunk routes running as far as 20 to 30 m from the nest. One studied trail was 26 m long, leading to a Robinia tree 25 m from the nest; beetles were collected on trails up to 30 m from 5 nests. Foraging runs from mid to late March until October.
- (b) Hölldobler B, Kwapich CL, et al. (2017). Amphotis marginata (Coleoptera: Nitidulidae) a highwayman of the ant Lasius fuliginosus. PLoS ONE 12(8): e0180847. DOI 10.1371/journal.pone.0180847 (full text, PMC5546582). The 20 to 30 m statement cites Dobrzanska J (1966) The control of territory by Lasius fuliginosus. Acta Biol. Exp. 26: 193 to 213, and Quinet Y, Pasteels JM (1991) Spatiotemporal evolution of the trail network in Lasius fuliginosus. Belg. J. Zool. 121: 55 to 72.
- (c) verified (text read in 2017 paper); originals secondary.
- (d) Simulation implication: L. fuliginosus (colonies of order 10^5 workers, carton nests) is the long-range extreme within Lasius. L. niger colonies are 10 to 100x smaller, so their max range should be well below 20 to 30 m.

### 1.5 Congener: Stomaphis quercus only on oaks within 17 m of an L. fuliginosus nest
- (a) The obligately ant-attended oak aphid Stomaphis quercus occupied only trees within 17 m of a Lasius fuliginosus nest. Also, Aphis serpylli occurred only where the sward held a high density of Lasius niger nests, mostly in quadrats that contained ant nests.
- (b) Hopkins GW, Thacker JI (1999). Ants and habitat specificity in aphids. Journal of Insect Conservation 3: 25 to 31. DOI 10.1023/A:1009626405307 (existence and authors confirmed via Semantic Scholar; text not read).
- (c) secondary (numbers from search summaries).
- (d) Simulation implication: ant-dependent aphid colonies cluster close to nests; for L. niger place root and herb aphids essentially within the nest's immediate surroundings.

### 1.6 Lasius neoniger (North American sister of L. niger): foraging field 2 to 3 yards radius
- (a) The "trophophoric field" of many L. neoniger nests did not seem to exceed two or three yards (about 1.8 to 2.7 m) in radius; workers foraged above ground early in the night collecting dead and crippled insects.
- (b) Wilson EO (1955), as quoted on AntWiki (Lasius neoniger page). AntWiki blocked direct fetch; content seen only through search summary.
- (c) secondary.
- (d) Simulation implication: an ecologically very similar small Lasius forages within about 2 to 3 m of its nest, matching the L. niger 36 m2 territory estimate.

### 1.7 Formica (red wood ants) for contrast
- (a) Formica lugubris (polydomous, Britain): nests with foraging trails were larger and in shadier areas; the minimum foraging trail length (nest to nearest used tree) was 4.52 m plus or minus 0.33 (mean plus or minus SE, n = 135 nests). Inter-nest distances within one polydomous colony: longest 52 m, average 2.5 m, more than 90% of inter-nest trails below 8 m (S. Ellis preliminary survey, cited). Colonies had 1 to 22 nests.
- (b) Chen YH, Robinson EJH (2014). The relationship between canopy cover and colony size of the wood ant Formica lugubris: implications for the thermal effects on a keystone ant species. PLoS ONE 9(12): e116113. DOI 10.1371/journal.pone.0116113 (full text, PMC4281126).
- (c) verified.
- (a2) Formica aquilonia: study trees with heavy ant traffic were chosen within 20 m of each nest. Worker crop load increased with distance; forager density is higher close to colonies (citing De Vita 1979; Savolainen & Vepsäläinen 1988).
- (b2) Gibb H, Andersson J, Johansson T (2016). Foraging loads of red wood ants: Formica aquilonia in relation to tree characteristics and stand age. PeerJ 4: e2049. DOI 10.7717/peerj.2049 (full text, PMC4878371).
- (c2) verified.
- (a3) Formica rufa foraging trails "may extend 100 m" (Wikipedia, via search summary).
- (c3) unverified.
- (d) Simulation implication: even the big mound builders typically use trees 4 to 20 m away. For a small L. niger colony, scale this down: nearest tree or shrub aphids about 1 to 5 m away is realistic.

### 1.8 How range changes with colony size and season
- (a) Novgorodova found that L. niger workers show no task specialization among honeydew collectors despite colony sizes of order 10^3 (L. fuliginosus 10^5). Protected (continuously guarded) aphid colonies were usually located close to the nest (r < 0.8 m); this distance figure was reported for Formica (Serviformica) cunicularia and F. candida, and the paper says the protective strategy in Lasius is adopted only for aphid colonies "quite close to ant nests" (or near main trails for L. fuliginosus). In the field, aphid colonies studied were on plants at heights up to 1.3 m, usually 0.6 to 1.1 m above ground. After mowing destroyed most aphid colonies, L. niger switched to continuous protection of the 5 surviving colonies.
- (b) Novgorodova TA (2015). Organization of honeydew collection by foragers of different species of ants (Hymenoptera: Formicidae): effect of colony size and species specificity. European Journal of Entomology 112(4): 688 to 697. DOI 10.14411/eje.2015.077 (full text PDF read).
- (c) verified.
- (a2) Temnothorax rugatulus: colony size did not predict foraging distance (included only as a caution that range does not necessarily scale with colony size).
- (b2) Bengston SE, Dornhaus A (2013). Colony size does not predict foraging distance in the ant Temnothorax rugatulus. Insectes Sociaux 60. DOI 10.1007/s00040-012-0272-4 (search summary only).
- (c2) secondary.
- (a3) Seasonal: aphids that overwinter in L. niger nests are carried out onto host plants in spring; at first they are brought back into the nest each evening, later (warmer nights) left in place. Above-ground honeydew solicitation peaks at midnight (Eidmann 1926, 1943, as summarized).
- (b3) Navajo Nature ant pages (navajonature.org, Lasius niger), summarizing Eidmann (1926, 1943). Primary not read.
- (c3) secondary.
- (d) Simulation implication: model range as growing weakly with colony size, and seasonally: spring foraging close to the nest (aphids ferried out and back), summer extended to the farthest plants, autumn contracting. No numeric seasonal curve for L. niger was found.

---

## 2. Territory, home range, nest density and spacing

### 2.1 L. niger territory about 36 m2
- See 1.3. Average L. niger territory 36 m2 (Elmes 1971 via Hertzog et al. 2016). Equivalent circle radius about 3.4 m; square side 6 m.
- (c) secondary.
- (d) Simulation implication: one colony "owns" roughly 6 m of a 1D transect through its centre.

### 2.2 L. niger home-range marking rather than defended territory
- (a) Devigne & Detrain conclude that exploratory area marking in L. niger is a home-range marking rather than a true territorial marking, and that collective exploration, home-range marking and intraspecific interactions structure the use of foraging space by neighbouring L. niger colonies. Recruitment intensity falls with food distance and falls further without home-range marking.
- (b) Devigne C, Detrain C (2002). Collective exploration and area marking in the ant Lasius niger. Insectes Sociaux 49: 357 to 362. DOI 10.1007/PL00012659. And Devigne C, Detrain C (2006). How does food distance influence foraging in the ant Lasius niger: the importance of home-range marking. Insectes Sociaux 53: 46 to 55. DOI 10.1007/s00040-005-0834-9. Both paywalled; metadata confirmed via Semantic Scholar.
- (c) secondary (conclusions from abstracts shown in search summaries).
- (d) Simulation implication: neighbour boundaries should emerge from overlapping home-scent fields and encounters, not from a hard-coded fence.

### 2.3 Neighbour aggression and boundaries (L. niger)
- (a) In a semi-regular array of L. niger colonies under concrete slabs in a grass field (Regensburg), overt aggression did not vary with genetic relatedness or spatial distance, and direct neighbours were not treated differently. Antennation and jerking declined with distance and relatedness. The paper states L. niger nest density can reach almost 400 colonies per hectare (Boomsma et al. 1982), colonies compete for nest sites and food, raid each other for brood, and tournaments are reported (Czechowski 1984). Sexual production falls with increasing nest density and rises if neighbours are poisoned (Pontin 1961), indicating strong intraspecific competition.
- (b) Czaczkes TJ, Koch A, Schmid S, Trindl A, Heinze J, Cordonnier M (2023). Not dear neighbours: antennation and jerking, but not aggression, correlate with genetic relatedness and spatial distance in the ant Lasius niger. Ecological Entomology 49: 166 to 179. DOI 10.1111/een.13291. Full text: https://epub.uni-regensburg.de/54939/ . Density original: Boomsma JJ, Van der Lee GA, Van der Have TM (1982). On the production ecology of Lasius niger in successive coastal dune valleys. Journal of Animal Ecology 51: 975 to 991. DOI 10.2307/4017 (not read).
- (c) verified (full text read); 400/ha is secondary.
- (d) Simulation implication: neighbouring colonies meeting at the boundary should fight, with occasional brood raids; 400 colonies/ha means about 25 m2 per colony, i.e. neighbour spacing about 5 m.

### 2.4 Nest density across Central Europe (Seifert)
- (a) In Seifert's 37-year survey (232 plots, about 17,000 nests), L. niger occupied 75 plots; the summed nest density across those plots was 1271.5, i.e. a mean of about 17 nests per 100 m2 in occupied plots (my division; units are nests/100 m2 as used throughout the paper). Mean nest population used for L. niger: 5,000 workers (L. flavus: 10,000). A separately cited summary of Seifert's data gives mean 17.6 and maximum 108 nests/100 m2 on 68 Central European test plots.
- (b) Seifert B (2017). The ecology of Central European non-arboreal ants: 37 years of a broad-spectrum analysis under permanent taxonomic control. Soil Organisms 89(1): 1 to 69. DOI 10.25674/so89103 (open access PDF read). The 17.6/108 figures are as cited in Stukalyuk (2024 preprint, see 2.5).
- (c) verified (table values read); mean per plot is my calculation; 17.6/108 secondary.
- (d) Simulation implication: at 17 nests/100 m2, each nest has about 5.9 m2, so nearest-neighbour spacing about 2.4 m. At the 108/100 m2 maximum, spacing is about 1 m.

### 2.5 Huge L. niger nest complexes (Ukraine)
- (a) Two neighbouring L. niger nest complexes near Kyiv: complex A 14,655 mounds on 11.8 ha (12.4 nests/100 m2); complex B 15,599 mounds on 13.3 ha (11.7 nests/100 m2); together they stretch about 1,200 m, 86 to 450 m wide, separated by a 20 to 120 m wide ploughed strip. Density classes in complex B: 15.9, 11.9, 11.5 and 4.4 mounds/100 m2 by zone. Mean mound diameter 34 cm, height 23 cm. Nests are connected by above-ground trails and subterranean tunnels; one 100 m2 plot held 13 major mounds plus 7 auxiliary nests linked by trails. Workers from nests 30, 250, 500 and 1,000 m apart within complex B showed low aggression; aggression against a remote garden population was higher. A 35 cm mound is estimated at about 14,000 workers; maximum monogynous nest about 60,000 (citing Boomsma et al. 1982).
- (b) Stukalyuk S (2024). A new trait in a well-studied ant: low aggression between workers from Lasius niger (Hymenoptera, Formicidae) nest complexes. bioRxiv preprint, DOI 10.1101/2024.07.23.604725 (full text PDF read; not peer reviewed). Also Stukalyuk S, Goncharenko I, Kozyr M (2023). Study of ecological characteristics of Lasius niger using vegetation data. Zoodiversity 57(6): 529 onward. DOI 10.15407/zoo2023.06.529 (full text read): 4 to 15 mounds/100 m2 in complex B; distance between mounds proportional to mound size class; mounds along one transect clustered at about 1.2 m; "as a rule, L. niger colonies are polycalic, with a central nest and several auxiliary nests"; single mound usually not above 10,000 workers, up to 70,000 in the most favourable habitats (citing Zakharov 2015); L. niger tends 43 to 65 aphid species.
- (c) verified (both read), but note: the preprint is not peer reviewed, and nest-complex (supercolonial) behaviour is unusual for L. niger. Most literature treats L. niger as monogynous and monodomous.
- (d) Simulation implication: a normal (non-complex) L. niger setting has neighbour mounds 1 to 5 m apart. For a screensaver, show 1 to 2 neighbour colonies at the strip edges, 3 to 6 m from the home nest.

### 2.6 L. flavus (underground congener) worker density
- (a) L. flavus workers, counting those in mounds plus those beneath the surrounding grassland, reach a mean density of 6,122 workers/m2 (95% CL 3,038 to 9,206; mean of ten studies 1961 to 2018). Workers forage beneath the grassland for honeydew of root aphids. Mound surfaces can cover 5 to 24% of mature grassland.
- (b) King TJ. The persistence of Lasius flavus ant-hills and their influence on biodiversity in grasslands. Manuscript (Oxford ORA repository, uuid:57fbce82-4488-4745-b065-cf860e6d32af), citing Odum & Pontin 1961, Pontin 1978 and others.
- (c) verified (text read); year of the manuscript not confirmed.
- (d) Simulation implication: if the sim includes an L. flavus neighbour, its foraging is almost entirely underground in the root zone between mounds.

### 2.7 Lasius japonicus foraging range (unverified)
- (a) A search summary attributed to the AntWiki L. japonicus page: a colony with a territory of 0.8 ha, about 7,000 workers spread over an average of 11 nests; foraging ranges not actively defended but used almost exclusively by one colony, extended into areas abandoned by neighbours; mainly nocturnal foraging into the canopy. The species attribution could not be confirmed (AntWiki blocked).
- (c) unverified.
- (d) Simulation implication: do not rely on it. It does illustrate that range boundaries can shift when a neighbour disappears.

---

## 3. Aphids: where they are and how far away

### 3.1 Height of tended aphid colonies
- (a) Aphid colonies tended by 12 ant species including L. niger (Siberia) were on plants at heights up to 1.3 m, usually 0.6 to 1.1 m above ground.
- (b) Novgorodova 2015 (see 1.8).
- (c) verified.
- (d) Simulation implication: in the cross-section, put herb/shrub aphid patches at 0.5 to 1.2 m above ground on plant stems.

### 3.2 Aphids on herbs, shrubs, trees and roots
- (a) Collingwood: L. niger tends aphids on shrubs and herbs as well as subterranean species; foraging tracks are frequently covered by surface tunnels of earth; occasionally earth mounds are formed; nests single-queened with several hundred up to 10,000 workers.
- (b) Collingwood CA (1979). The Formicidae (Hymenoptera) of Fennoscandia and Denmark. Fauna Entomologica Scandinavica 8. Treatment on Zenodo: https://zenodo.org/records/6283850
- (c) verified (treatment text read).
- (a2) Of 43 aphid species associated with L. niger in Ukraine, 32 were on aerial plant parts, the rest on roots and root necks; 4 plant species received 70 to 95% of L. niger forager visits.
- (b2) Stukalyuk S, Kozyr M, Zhuravlev V (2022), as cited in Stukalyuk et al. 2023 and in search summaries.
- (c2) secondary.
- (d) Simulation implication: roughly three quarters of aphid sites above ground (on stems) and one quarter on roots underground.

### 3.3 Trees
- (a) L. niger forages in trees several metres high (Czaczkes, pers. obs., see 1.1); in apple orchards L. niger tends Dysaphis plantaginea and Aphis pomi, and visits Aphis fabae more intensively when it is closer to the nest (search summary of orchard studies, e.g. Nagy et al. 2013 Biological Control; Scientific Reports 2020 s41598-020-64973-7).
- (c) verified for the Czaczkes statement; orchard statements secondary.
- (d) Simulation implication: an optional tree or shrub 2 to 6 m from the nest with a vertical trail several metres up the trunk is realistic.

### 3.4 Fidelity to specific aphid colonies
- (a) Individual honeydew foragers of L. niger (and a dozen other species) return repeatedly to the same location and aphid group.
- (b) Novgorodova 2015 (see 1.8), and L. fuliginosus fidelity coefficients 83 to 96% to particular trails and trees: Quinet Y, Pasteels JM (1996). Spatial specialization of the foragers and foraging strategy in Lasius fuliginosus. Insectes Sociaux 43: 333 to 346. DOI 10.1007/BF01258407 (search summary).
- (c) verified (Novgorodova); secondary (Quinet & Pasteels).
- (d) Simulation implication: each forager should keep a remembered target aphid patch and revisit it.

---

## 4. Underground and covered foraging

### 4.1 Earth-covered runways and aphid shelters (L. niger)
- (a) L. niger builds turret-shaped soil shelters around the bases of aphid-infested plants and connects them to the nest by covered pathways; aphids that overwinter in the nest are carried out in spring (Eidmann 1926, 1943, as summarized). BWARS: L. niger sometimes covers foraging trails and plant stems bearing aphids with surface tunnels of earth. Collingwood 1979: foraging tracks frequently covered by surface tunnels of earth.
- (b) Collingwood 1979 (verified, see 3.2); BWARS species account https://bwars.com/ant/formicidae/formicinae/lasius-niger (verified, read); Eidmann via navajonature.org (secondary).
- (c) verified (existence of behaviour); no distances given.
- (d) Simulation implication: allow a surface trail segment to be converted into a "covered runway" (soil tube just at the surface) and a small soil pavilion at the base of an aphid plant.

### 4.2 Auxiliary nests grow from aphid shelters; tunnels between mounds
- (a) In L. niger nest complexes, auxiliary nests are usually initiated by constructions of mineral soil covering aphid colonies, both subterranean and at the base of plant stems, and may develop into major nests. Mounds are linked by above-ground trails and subterranean tunnels.
- (b) Stukalyuk 2024 preprint (see 2.5).
- (c) verified (text read; preprint).
- (d) Simulation implication: a soil shelter at an aphid plant can later become a satellite chamber of the colony (polydomy), connected by a shallow tunnel.

### 4.3 Distance of underground foraging
- (a) No source found giving a distance in metres for L. niger underground foraging tunnels. L. flavus forages beneath grassland between mounds for root aphids (King, 2.6); Seifert notes L. flavus-type root-aphid food is concentrated in the upper 15 cm of soil (in the context of L. psammophilus vs L. niger).
- (c) verified for the qualitative statements; distance is a gap.
- (d) Simulation implication: keep L. niger underground foraging tunnels shallow (top 5 to 15 cm) and short (within the colony's 2 to 4 m radius).

---

## 5. Polydomy

- (a) Mixed evidence. Standard view: L. niger is monogynous, and "ants of the genus Lasius are considered to be rarely polygynous and thus the likelihood to form polydomous structures is low" (Stukalyuk 2024, citing Seifert). But Stukalyuk et al. 2023 state that "as a rule, L. niger colonies are polycalic, with a central nest and several auxiliary nests", and the Ukrainian complexes show thousands of interconnected mounds. Soil pavilions over aphids act as auxiliary structures (4.2).
- (b) Stukalyuk 2024 preprint; Stukalyuk et al. 2023 Zoodiversity (both verified).
- (c) verified (statements read), but interpretation is contested.
- (d) Simulation implication: a single main nest plus 0 to 3 small auxiliary chambers or aphid shelters within a few metres is a defensible default. Supercolonies are an exception.

---

## 6. Distribution of trip distances and trip timing

### 6.1 Most activity close to the nest
- (a) Patrolling L. niger numbers decline as distance to the nest increases (Hertzog et al. 2016, citing Devigne & Detrain 2006). For Formica, forager density is higher close to colonies (Gibb et al. 2016 citing De Vita 1979 and Savolainen & Vepsäläinen 1988). For F. lugubris, more than 90% of inter-nest trails are below 8 m while the longest is 52 m (Chen & Robinson 2014): a strongly right-skewed distribution.
- (c) verified (statements read); the shape of the L. niger distribution is not quantified anywhere I found.
- (d) Simulation implication: draw trip target distances from a right-skewed distribution (e.g. exponential or log-normal) with median about 1 m, 90th percentile about 3 m, and a hard tail near 8 to 10 m.

### 6.2 Trip timing at field scale (derived)
- (a) Using L. niger trail speed of about 2 cm/s at 25 C (note 05, B1, verified), a one-way walk of 1 m takes about 50 s, 3 m about 2.5 min, 10 m about 8 min, 20 m about 17 min. Add drinking time at aphids or feeders (about 1 min in lab studies, note 04, 3.4) and unloading in the nest (about 1 min). Speed drops about 3x on steep inclines (note 04, 3.1), so climbing 2 m up a stem costs roughly as much as 6 m on flat ground.
- (c) secondary (my calculation from verified inputs).
- (d) Simulation implication: a typical trip to an aphid plant 1 to 3 m away lasts about 3 to 8 min round trip; a tree visit 5 m away plus 2 m climb takes about 15 to 20 min.

### 6.3 Crop load increases with distance (Formica)
- (a) F. aquilonia crop load increased with distance to the tree; F. rufa larger workers visit farther trees and return with heavier loads.
- (b) Gibb et al. 2016 (verified); Wright PJ, Bonser R, Chukwu UO (2000). The size-distance relationship in the wood ant Formica rufa. Ecological Entomology 25: 226 to 233. DOI 10.1046/j.1365-2311.2000.00253.x (abstract via search summary).
- (c) verified / secondary.
- (d) Simulation implication: allow foragers to fill more fully on longer trips (fits the L. niger "desired volume" threshold in note 04).

---

## Recommended world dimensions

All values are for a single normal (not supercolonial) L. niger colony of a few thousand workers. Real-world metres; the renderer can compress the horizontal scale if needed.

| Parameter | Recommended value | Basis |
|---|---|---|
| Surface strip length | 8 m (nest in the middle, 4 m each side); 6 m minimum, 12 m if a tree is included | territory 36 m2 (radius 3.4 m); neighbours 2.4 to 5 m apart |
| Typical trail length (nest to food) | 0.5 to 2 m (median about 1 m) | lab and field statements, "several metres" upper typical |
| Long trail | 3 to 5 m | "several metres", 2.9 m (unverified) |
| Maximum excursion | 8 to 10 m (rare) | Formica 90% below 8 m; Hertzog 18 m upper bound; well below L. fuliginosus 20 to 30 m |
| Herb/shrub aphid patches | 2 to 4 plants at 0.3 to 3 m from the nest; aphids 0.5 to 1.2 m up the stems | Novgorodova 2015 heights 0.6 to 1.1 m |
| Protected (guarded, sheltered) aphid patch | the closest plant, within about 1 m of the nest, with a soil pavilion at its base | Novgorodova r < 0.8 m (Formica), Eidmann shelters |
| Root aphids | 1 to 3 underground patches in the top 5 to 15 cm of soil, within 0.2 to 2 m laterally of the nest | Collingwood, Stukalyuk (about a quarter of aphid species on roots) |
| Tree or large shrub (optional) | 3 to 6 m from the nest, trail climbing 2 to 5 m up the trunk | Czaczkes pers. obs. "trees several metres high" |
| Neighbour colonies | one at each strip edge, 3 to 6 m from the home nest (dense sites 1 to 2.5 m) | 17 nests/100 m2 gives 2.4 m; 400/ha gives 5 m; 36 m2 territory gives 6 m |
| Boundary zone | overlap band about 0.5 to 1 m wide midway between nests where encounters and fights happen | home-range marking, no dear-enemy effect |
| Satellite chambers | 0 to 3 small auxiliary chambers or aphid shelters, 0.5 to 3 m from the main nest, linked by shallow tunnels or covered runways | Stukalyuk 2023, 2024 |
| Trip time | typical 3 to 8 min round trip; tree trips 15 to 20 min | 2 cm/s speed |
| Trip-distance distribution | right-skewed: median about 1 m, 90th percentile about 3 m, max about 8 to 10 m | Formica trail distribution shape; L. niger density decline with distance |

Seasonal suggestion: spring range about half of summer range (aphids ferried out daily and returned); peak range in mid summer; contraction in autumn. This is an extrapolation, not a measured curve.

---

## Gaps

1. **No primary field measurement of L. niger foraging distance (median, mean, max) was found.** The "several metres" statement is verified only as a citation of Devigne & Detrain 2005 (Acta Zool. Sinica), which I could not read. The widely repeated "2.9 m" trail length remains untraced.
2. **Territory area** rests on one old value (Elmes 1971: 36 m2) seen only through a citing paper. Boomsma et al. 1982 (J. Anim. Ecol. 51: 975 to 991) probably has nest density and colony size by dune valley, but it was not read.
3. **Colony size vs range**: no L. niger data. Only a general statement that L. niger colonies are of order 10^3 workers (Novgorodova) and a mean of 5,000 (Seifert 2017).
4. **Seasonal range changes**: only qualitative (Eidmann via secondary source; L. fuliginosus active March to October). No L. niger numbers.
5. **Underground foraging distance** for L. niger (tunnels to root aphids) is undocumented in metres.
6. **Number of trails per colony and total trail network length** for L. niger: nothing found. L. fuliginosus and Formica network papers (Quinet & Pasteels 1991; Chen & Robinson 2014) give related data only.
7. **Trip-distance distribution** for L. niger is not quantified; the recommended skewed distribution is inferred from Formica trail data and qualitative statements.
8. **Polydomy** in L. niger is contested; the strongest evidence is a non-peer-reviewed preprint from an unusual population.
9. **L. japonicus 0.8 ha territory** claim could not be traced to a species or primary source.
10. Sources blocked this session: Springer (Insectes Sociaux), ResearchGate, AntWiki, Academia.edu, Wiley HTML. Unpaywalled copies via Europe PMC, Regensburg ePub, and journal PDFs were used wherever possible.

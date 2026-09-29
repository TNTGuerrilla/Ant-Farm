# 03. Nest construction and environmental threats (Lasius niger focus)

Confidence legend:
- verified: numbers read from the paper itself (full text, PDF, or official abstract) during this research session.
- verified (abstract/secondary): the source exists and was found, but the numbers came from an abstract, a search-engine summary of the paper, or a secondary article quoting it. Treat numbers as likely but re-check before hard-coding.
- unverified: seen only in a snippet with no identifiable primary source, or in hobbyist care sheets.

Species are named for each finding. When a finding is not from Lasius niger, it is flagged "(other species)".

---

## 1. Nest excavation and architecture

### 1.1 Self-organized building in L. niger: pheromone plus body-size template
(a) In groups of 500 L. niger workers building for 96 h (48 h for pillar tests) on a moistened sand/clay mix (equal parts, 10 g dry weight, layer 2 to 3 mm high) at 26 C and 30 to 40% RH, ants built regularly spaced pillars (nearest-neighbour distance 9.93 +/- 0.66 mm; density about 0.3 pillars per cm2 after 48 h). Once pillars reached about 4 mm (worker body length 4.1 +/- 0.14 mm), ants began building a roof laterally from them (body-size template). Deposition is amplified by a building pheromone added to the material: model parameters were a base deposition rate of 0.025 per s, increase of 0.11 per s per pellet already present, pick-up rate parameter 0.029 per s, and a pheromone lifetime of 800 to 1,200 s (about 13 to 20 min). The 3D model ran on a 200 x 200 x 200 lattice with 0.5 mm cells, 1 s time step. Pheromone lifetime changed the final architecture (short lifetime gives pillars/walls; the paper reports that evaporation-dependent lifetime can shift structures), and field nests (X-ray tomography of two nests) had chamber heights of 5.9 mm and 5.5 mm, chamber widths centred around 12 mm, total volumes of 5,390 cm3 and 7,099 cm3 of which 1,730 cm3 and 2,134 cm3 were chambers/galleries (about 30% void).
(b) Khuong A., Gautrais J., Perna A., Sbai C., Combe M., Kuntz P., Jost C., Theraulaz G. (2016). Stigmergic construction and topochemical information shape ant nest architecture. PNAS 113(5): 1303-1308. DOI 10.1073/pnas.1509829113. Full text: https://pmc.ncbi.nlm.nih.gov/articles/PMC4747701/
(c) verified
(d) Simulation implication: model digging/depositing as a stochastic local rule where probability of dropping a soil pellet rises with nearby freshly marked pellets (pheromone decaying with half-life on the order of 10 to 20 min), and cap wall/pillar height at about 1 body length before switching to roof building. Chambers should be flat (about 5 to 6 mm tall, about 1.4 body lengths) and wide, with about 30% of the nest volume empty.

### 1.2 L. niger nest size is regulated by population (digging stops when space per ant is reached)
(a) In L. niger, both final nest volume and digging rate grew almost proportionally with the number of workers. When the number of ants in an already dug nest was artificially increased, ants resumed digging and adjusted the nest to the new population in the same way as in initial excavation; digging acts as a negative-feedback loop that regulates nest size to colony size.
(b) Rasse P., Deneubourg J.L. (2001). Dynamics of nest excavation and nest size regulation of Lasius niger (Hymenoptera: Formicidae). Journal of Insect Behavior 14(4): 433-449. DOI 10.1023/A:1011163804217
(c) verified (abstract/secondary): exact volume per ant not retrieved (paywalled).
(d) Simulation implication: tie a colony's "target nest volume" to worker count (linear), trigger digging when volume per ant falls below target (e.g. after a brood cohort ecloses or after flood damage), and stop when target is reached.

### 1.3 Excavation dynamics are logistic and proportional to group size (other species: Messor sancta)
(a) Groups of Messor sancta workers dug gallery networks in a thin sand disk in under 3 days. Excavated volume over time followed a logistic curve (early exponential phase from positive feedback via recruitment, then saturation from negative feedback), and total excavated volume was almost proportional to the number of workers.
(b) Buhl J., Gautrais J., Deneubourg J.L., Theraulaz G. (2005, online 2004). Self-organized digging activity in ant colonies. Behavioral Ecology and Sociobiology 58: 9-17. DOI 10.1007/s00265-004-0906-2
(c) verified (abstract/secondary)
(d) Simulation implication: a new dig front should accelerate (more diggers recruited to active faces) then taper off; use a logistic curve for each excavation episode rather than a constant rate.

### 1.4 Temperature controls depth and speed of digging (L. niger and L. flavus)
(a) L. niger: 17 young colonies dug in soil columns for 100 days under three surface temperatures (mild n = 5, medium n = 6, high n = 6), with X-ray scans at days 7, 14, 28, 88. Medium and high temperature colonies dug larger and deeper nests than mild ones. Maximum depth was the same for medium and high, but at high temperature the chambers were located deeper and some upper chambers were back-filled. Colonies grew in all treatments, less so under mild temperature.
(b) Garcia Ibarra F., Jouquet P., Bottinelli N., Bultelle A., Monnin T. (2024). Experimental evidence that increased surface temperature affects bioturbation by ants. Journal of Animal Ecology. DOI 10.1111/1365-2656.14040
(c) verified (abstract read in full via Europe PMC; absolute temperatures and depths not retrieved)
(d) Simulation implication: in hot weather, colonies should dig deeper chambers and may refill (abandon) the uppermost chambers; in cool weather they dig less.

(a2) Lasius flavus (other species, same genus): workers dug 1.8 to 3.2 times faster at 25 C than at 15 C (range tested 15 to 30 C) but produced the same nest shapes; temperature acts like a "playback speed". A related report states walking speed doubled for about a 12 C rise.
(b2) Rathery A., Facchini G., Halsey L.G., et al. (2025). High environmental temperatures put nest excavation by ants on fast forward: they dig the same nests, faster. Insectes Sociaux 73: 55-62. DOI 10.1007/s00040-025-01049-7. Preprint: https://www.biorxiv.org/content/10.1101/2025.03.24.645078
(c2) verified (abstract/secondary)
(d2) Simulation implication: multiply all digging and walking rates by a temperature factor (roughly Q10 of 2 to 3 between 15 and 25 C) instead of changing the rules.

### 1.5 Field L. niger nest dimensions and mounds
(a) Reported values (from search summaries, primary source not opened): galleries concentrated in the top 0.3 m of soil with a few vertical galleries to about 0.7 m; gallery diameter 1.5 to 5.0 mm; chambers 10 to 20 mm wide and 30 to 50 mm long; meadow earth mounds up to 40 cm high and 60 to 70 cm diameter; one study's mounds measured about 27 cm tall by 26 cm wide and 21 cm by 27 cm. L. niger mounds consist of many bubble-shaped chambers; the species builds mounds especially where soil is moist and vegetation dense, and also nests under stones, in pavement cracks and dead wood.
(b) Possibly from: Nest building activity and bioturbation of the ant Lasius niger, EGU General Assembly 2015 abstract, https://ui.adsabs.harvard.edu/abs/2015EGUGA..1714899T/abstract ; and Impact of Biogenic Structures of the Soil-Nesting Ants Lasius niger..., Insects (2025) 16(10):1058, DOI 10.3390/insects16101058. Neither page could be opened.
(c) unverified (numbers are plausible and consistent with Khuong 2016 chamber widths of about 12 mm, but source attribution not confirmed)
(d) Simulation implication: a mature L. niger cross-section should be shallow and top-heavy: most chambers within 30 cm, a few shafts to about 70 cm, plus an above-ground soil mound full of small chambers used for solar warming.

### 1.6 Nest scaling with colony size (other species, Tschinkel casting)
(a) Pogonomyrmex badius (Florida harvester ant): each 10-fold increase in workers increased max depth 2.4-fold and total chamber area about 7.5-fold (so area per worker falls as colonies grow). Incipient nests 29 to 37 cm deep; mature 2.5 to 3.0 m (max 3.06 m); 5 to 150 chambers. About half of total chamber area lies in the top quarter of the nest; chamber area per depth decile falls about 75% per 10-fold depth increase. Vertical chamber spacing grows from 2 to 4 cm near the surface to 20 to 30 cm deep. Nests grow by deepening, enlarging chambers and adding new shaft-chamber series.
(b) Tschinkel W.R. (2004). The nest architecture of the Florida harvester ant, Pogonomyrmex badius. Journal of Insect Science 4: 21. DOI 10.1093/jis/4.1.21
(c) verified
(a2) Formica pallidefulva: shallow nests 30 to 45 cm deep made of more or less vertical shafts bearing chambers; shafts are modular growth units; volume declines exponentially with depth (top-heavy); total nest volume correlates with worker number (R2 = 0.87). Rules: chambers form in the direction of the approach tunnel; soil excavated per unit time increases with soil temperature and moisture. Estimated cost: moving twice a year would use about 20% of energy intake and at least 6% of worker time.
(b2) Mikheyev A.S., Tschinkel W.R. (2004). Nest architecture of the ant Formica pallidefulva: structure, costs and rules of excavation. Insectes Sociaux 51: 30-36. DOI 10.1007/s00040-003-0703-3
(c2) verified
(d) Simulation implication: grow the nest by extending vertical shafts and budding chambers off them, keep it top-heavy (exponential decline of chamber area with depth), space chambers further apart deeper down, and let area per worker decline slowly as the colony grows. Digging speed should increase with soil moisture and temperature.

### 1.7 Soil moisture preference
(a) Formica pallidefulva digs faster in moister soil (see 1.6). For L. niger, lab sources used moistened sand/clay (Khuong 2016). Mound soils of L. niger are reported to be drier than surrounding soil, with lower decomposition. Hobbyist care sheets quote 50 to 60% nest humidity or 10 to 50% "moisture"; these are not scientific.
(b) Mikheyev & Tschinkel 2004 (above); soil moisture in mounds: Frouz et al.? The effect of two ant species Lasius niger and Lasius flavus on soil properties in two contrasting habitats (European Journal of Soil Biology, about 2006), https://www.researchgate.net/publication/248542490 (only the title and a search summary were seen).
(c) verified for F. pallidefulva; unverified for L. niger numeric moisture preferences.
(d) Simulation implication: give soil cells a moisture value; digging probability and speed increase with moisture up to a point (saturated soil is flooded, see section 3); dry sand digs slowly and collapses.

---

## 2. Thermoregulation and humidity: brood relocation

### 2.1 Daily brood transport tracks temperature (other species: Solenopsis invicta)
(a) On cool mornings fire ants move brood up into the mound on the sun-heated side (usually south); when mound temperature exceeds the optimum (about 32 C) they move brood down the gradient. Below about 40 cm, nest temperature barely differs from ground temperature, and deep chambers stay between 16 and 24 C year round. Mirror and night-heating experiments showed brood placement tracks temperature directly, not a circadian clock. At mid-morning in spring about 60 to 65% of workers and 90% of brood are in the mound. Development speeds up with temperature until about 32 C, above which brood mortality rises. The paper also notes that ground-nesting ants use stones the same way, moving brood under warm stones by day and deeper at night.
(b) Penick C.A., Tschinkel W.R. (2008). Thermoregulatory brood transport in the fire ant, Solenopsis invicta. Insectes Sociaux 55: 176-182. DOI 10.1007/s00040-008-0987-4. PDF: https://www.bio.fsu.edu/~tschink/publications/Thermoregulatory%20brood%20transport%20Penick%20and%20Tschinkel%202008.pdf
(c) verified
(d) Simulation implication: nurses should carry brood toward the chamber whose temperature is closest to a target (e.g. up to the shallow/mound chambers in the morning, down at midday heat and at night). Drive it from a simulated soil temperature profile that swings strongly near the surface and is damped below about 40 cm.

### 2.2 Circadian temperature preference for brood (other species: Camponotus mus)
(a) In a 20 to 40 C lab gradient, nurses translocated brood daily between two preferred temperatures: 30.8 C selected at mid-photophase and 27.5 C during the scotophase, 8 h later; the preference showed a circadian rhythm.
(b) Roces F., Nunez J.A. (1989). Brood translocation and circadian variation of temperature preference in the ant Camponotus mus. Oecologia 81: 33-37. DOI 10.1007/BF00377006
(c) verified (abstract/secondary)
(d) Simulation implication: a small day/night offset in target brood temperature (about 3 C warmer by day) is a realistic touch.

### 2.3 L. niger brood placement under stones and in mounds
(a) L. niger places brood directly under sun-warmed stones by day and carries it to deeper levels with nocturnal cooling; in mounds (L. niger, Formica exsecta) brood is moved to the sector where insolation gives optimal temperature.
(b) Search-engine summary of a ScienceDirect topic page ("Lasius - an overview", https://www.sciencedirect.com/topics/biochemistry-genetics-and-molecular-biology/lasius). Primary source not identified. The same behavior is described for ground nesters in general by Penick & Tschinkel 2008 citing Seeley & Heinrich 1981.
(c) unverified for L. niger specifically (general pattern verified via Penick & Tschinkel 2008)
(d) Simulation implication: add a surface stone or mound that heats quickly by day; a brood chamber right under it should be occupied in daytime and emptied at night.

### 2.4 Development temperature for L. niger founding queens
(a) Founding-queen first-brood development was compared across latitudes: northern populations develop more slowly than southern ones at 17 to 20 C, but faster at 22 to 27 C (latitude-dependent reaction norm).
(b) Kipyatkov V.E., Lopatina E.B., et al. Effect of temperature on rearing of the first brood by the founder females of the ant Lasius niger: latitude-dependent variability of the response norm. Journal of Evolutionary Biochemistry and Physiology (2004). DOI 10.1023/B:JOEY.0000033808.45455.75
(c) verified (abstract/secondary); author list from memory of the journal page is not confirmed, treat authors as unverified.
(d) Simulation implication: brood development rate should be a function of temperature in the 17 to 27 C band; hobby sources cite 20 to 24 C as optimal (unverified).

---

## 3. Rain and flooding

### 3.1 L. niger is exceptionally flood tolerant
(a) After a June 2013 summer flood in a German floodplain grassland (plots submerged 4 to 24 days, mean 14 days), L. niger occupied the same proportion of plots (85%) as before; 57 of 58 ants collected after the flood were L. niger. Submersion length did not predict presence or abundance. Cited lab work: L. niger workers survive 9 to 10 weeks submerged (survival drops below 50% only after that), and queens kept laying eggs for about 8 weeks under water; no anaerobic physiology was found, so survival likely depends on air bubbles/air pockets trapped in the nest. During the first flood days ants were seen clinging to vegetation tips and forming rafts. L. niger survived floods better than Myrmica rubra; winter inundation did not affect Myrmica or L. niger populations.
(b) Hertzog L.R., Ebeling A., Meyer S.T., Eisenhauer N., Fischer C., Hildebrandt A., Wagg C., Weisser W.W. (2016). High survival of Lasius niger during summer flooding in a European grassland. PLoS ONE 11(11): e0152777. DOI 10.1371/journal.pone.0152777. https://pmc.ncbi.nlm.nih.gov/articles/PMC5112897/
(c) verified (author list beyond Hertzog not checked, marked unverified)
(d) Simulation implication: flooding a L. niger nest should rarely kill the colony outright. Model trapped air pockets in upper/dead-end chambers where ants and brood cluster; mortality should rise slowly with weeks, not hours, of submersion. Surface ants may climb vegetation or raft.

### 3.2 Entrance fortification and nest relocation in rain (other species: Diacamma indicum)
(a) During monsoon, colonies built soil mounds and decorations around entrances (building index median 2, range 2 to 4, versus lower in other seasons; entrance height averaged 17.8 cm versus 0 cm otherwise). Workers carried soil balls from inside and deposited them around the entrance after showers. Light rain (about 8.6 mm per day) triggered entrance modification without abandonment; severe flooding caused relocation. Only 25.6% of colonies occupied subterranean nests in monsoon versus 70.3% pre-monsoon and 82.8% post-monsoon. Lab relocations took a median of about 210 min; 8 of 11 colonies chose higher alternative nests (not significant).
(b) Kolay S., Annagiri S. (2015). Dual response to nest flooding during monsoon in an Indian ant. Scientific Reports 5: 13716. DOI 10.1038/srep13716
(c) verified
(d) Simulation implication: a two-level response. Light rain: ants build up and narrow the entrance with soil (a small crater/turret). Heavy rain with water entering: carry brood and queen upward or to a dry chamber, and if the nest stays flooded, emigrate (hours-long process).

### 3.3 Rafting (other species: Formica selysi and Solenopsis invicta)
(a) Formica selysi (floodplain species): in rafts of 60 workers, 100% of brood items were placed at the raft base and queens occupied the raft centre. After 8 h of rafting, 79% of workers recovered; brood survival about 83% versus 79% (no significant cost). Rafts with brood had fewer unresponsive workers afterwards (0.6 +/- 0.2 versus 3 +/- 0.8). Brood is buoyant and acts as flotation.
(b) Purcell J., Avril A., Jaffuel G., Bates S., Chapuisat M. (2014). Ant brood function as life preservers during floods. PLoS ONE 9(2): e89211. DOI 10.1371/journal.pone.0089211
(c) verified (co-author list beyond Purcell unverified)
(a2) Solenopsis invicta: groups of 1,000 to 8,000 ants dropped into water flattened into a waterproof raft within about 3 min and can float for days (reported "months" in popular summaries); trapped air makes the raft far less dense than individual ants (popular sources quote 75%); ants move up from the bottom to maintain constant thickness.
(b2) Mlot N.J., Tovey C.A., Hu D.L. (2011). Fire ants self-assemble into waterproof rafts to survive floods. PNAS 108(19): 7669-7673. DOI 10.1073/pnas.1016658108
(c2) verified (abstract); timing and density numbers from National Geographic / LiveScience coverage are verified (abstract/secondary)
(d) Simulation implication: rafting is not documented as a routine L. niger response (only anecdotal clinging and rafting in Hertzog 2016). For a cross-section view, prefer "retreat to air pockets, carry brood upward, plug entrances". Rafting could be a rare surface-only event.

### 3.4 Evidence of learning after flood experience (other species: Formica selysi)
(a) Groups of F. selysi workers subjected to two consecutive floods showed individual specialization: the same workers repeatedly took top, middle, base or side positions. Workers' experience in a first rafting trial with brood changed their behaviour and raft shape in a second trial without brood, i.e. a memory effect.
(b) Avril A., Purcell J., Chapuisat M. (2016). Ant workers exhibit specialization and memory during raft formation. The Science of Nature 103: 36. DOI 10.1007/s00114-016-1360-5
(c) verified (abstract/secondary)
(d) Simulation implication: it is defensible to give individual ants a persistent "flood role" and to let flood-experienced colonies respond faster or more consistently in a second flood. There is no L. niger-specific evidence for colony-level learning after floods; treat any such mechanic as an extrapolation.

### 3.5 Rain stops foraging (other species: leaf-cutting ants)
(a) Rain strongly reduces foraging in leaf-cutting ants, partly because water loads and raindrop impacts affect laden workers.
(b) Farji-Brener A.G., et al. (2018). Working in the rain? Why leaf-cutting ants stop foraging when it's raining. Insectes Sociaux. DOI 10.1007/s00040-018-0605-z
(c) verified (abstract/secondary); author list unverified
(d) Simulation implication: suspend foraging and recall surface workers when rain starts; resume and clear entrance plugs afterwards.

---

## 4. Other threats: drought, cold, predators, parasites, competitors

### 4.1 Cold and overwintering (L. niger)
(a) 280 newly mated L. niger queens from urban Finland were overwintered at +7 to 8 C or +2 C over two years; one-year-old colonies were tested the same way. Overwintering at +2 C lowered worker survival; queen survival did not differ; colder queens had more body fat (lower metabolism); all surviving queens produced workers at equal rates. Additional search-summary figures (not confirmed to be from this paper): worker survival about 73.2% at 5 C versus 53.2% at 0 C, and 0% at -24 C; worker CTmin about 1.7 C.
(b) Haatanen M.K., van Ooik T., Sorvari J. (2015). Effects of overwintering temperature on the survival of the black garden ant (Lasius niger). Journal of Thermal Biology 49-50: 112-118 (pages unverified). DOI 10.1016/j.jtherbio.2015.02.012
(c) verified for abstract statements; unverified for the 73.2/53.2/0% and CTmin figures.
(d) Simulation implication: in winter the colony clusters in a few deep chambers; cold near-freezing temperatures cause gradual worker attrition, queens are hardier; founding queens in shallow cells face the harshest cold.

### 4.2 Drought (L. niger and congeners)
(a) Among closely related Lasius species, L. niger (open habitats with large daily temperature swings) showed the strongest acclimation response and highest plasticity in drought survival; fluctuating-temperature acclimation gave drought resistance comparable to warm acclimation.
(b) "Closely related ants exhibit species-specific transcriptional ..." PMC article, https://pmc.ncbi.nlm.nih.gov/articles/PMC13232500/ (page blocked by a captcha; full citation not retrieved)
(c) unverified (citation incomplete)
(d) Simulation implication: in drought, ants should move deeper toward moist soil (consistent with 1.4, deeper chambers under heat), reduce surface activity, and suffer higher mortality only in prolonged, extreme drought.

### 4.3 Founding-phase mortality and pleometrosis (L. niger)
(a) Incipient colony mortality is estimated as high as 95% (predation, competition, disease, desiccation). About 18% of natural incipient colonies had multiple queens (2 to 5 queens), another source says about 25%. Metarhizium brunneum caused strong mortality in founding queens regardless of group size; co-founding queens showed almost no allogrooming (12 of 17,004 scans, 0.07%) and relied on self-grooming.
(b) Brutsch T., Jaffuel G., Vallat A., Turlings T.C.J., Chapuisat M. (2017). No evidence for social immunity in co-founding queen associations. Scientific Reports 7: 16262. DOI 10.1038/s41598-017-16368-4 (co-authors and article number unverified). 25% figure: Sommer K., Holldobler B. (1995). Colony founding by queen association and determinants of reduction in queen number in the ant Lasius niger. Animal Behaviour 50: 287-294 (seen via secondary citation only).
(c) verified (Brutsch, via PMC5701228); Sommer & Holldobler 25% is verified (abstract/secondary)
(d) Simulation implication: most founding attempts fail; a screensaver can show several foundress cells nearby, most dying. Occasional 2 to 5 queen cells are realistic; extra queens are later eliminated.

### 4.4 Brood raiding between young colonies (L. niger)
(a) Young L. niger colonies raid each other's brood in the lab; queen associations produce more workers sooner, and the colony with the most workers wins raids. Nest densities: mean 17.6 and max 108 nests per 100 m2 across 68 Central European plots (Seifert); up to about 1 mature colony per m2 in meadows; almost 400 colonies per hectare in suitable habitat.
(b) Sommer & Holldobler 1995 (above); densities from search summaries citing Seifert (Die Ameisen Mittel- und Nordeuropas) and AntWiki (https://www.antwiki.org/wiki/Lasius_niger, blocked).
(c) verified (abstract/secondary) for raiding; unverified for exact density figures
(d) Simulation implication: neighbouring incipient colonies are a real threat; a raid event (workers entering, carrying off pupae and larvae) is realistic in the first months, favouring the colony with more workers.

### 4.5 Competition with neighbours and other species (L. niger, Tetramorium)
(a) L. niger and Tetramorium caespitum (pavement ant) overlap in diet and fight at food sources; a 2024 study describes competition through ritualized aggressive interactions. L. niger defends territory whose size depends on colony size and food; aggression toward conspecific neighbours is seasonal (higher in spring) while aggression toward other species stays high. Some L. niger populations form nest complexes with low aggression between workers.
(b) Competition through ritualized aggressive interactions between ... Science of Nature (2024), DOI 10.1007/s00114-024-01891-y (authors not retrieved); low aggression in nest complexes: bioRxiv 2024, https://www.biorxiv.org/content/10.1101/2024.07.23.604725 ; review: Champer J., Schlenoff D. (2024). Battles between ants (Hymenoptera: Formicidae): a review. Journal of Insect Science, DOI 10.1093/jisesa/ieae064
(c) verified (abstract/secondary), with author lists incomplete
(d) Simulation implication: surface skirmishes with a Tetramorium or neighbouring L. niger colony can cost foragers; underground raids are mainly a risk for small colonies.

### 4.6 Social parasites (L. niger as host)
(a) L. niger is host to temporary social parasites of the L. umbratus/mixtus group (e.g. Lasius umbratus, L. mixtus). An L. umbratus queen enters a host nest, kills the host queen and has host workers raise her brood; she also kills host workers encountered outside, carrying them in her mandibles. Parasite queens are sometimes detected and rejected after more than a week.
(b) AntWiki Lasius umbratus page (https://www.antwiki.org/wiki/Lasius_umbratus) and The Ants chapter 12 (Holldobler & Wilson 1990) via AntWiki, both seen via search summary only.
(c) verified (abstract/secondary)
(d) Simulation implication: optional rare late-game event: a foreign queen infiltrates and the colony gradually shifts to yellow workers.

### 4.7 Predators and parasitoids
(a) The European green woodpecker (Picus viridis) feeds mainly on ants, especially Lasius and Formica; in half-open Dutch landscape its diet was mainly L. niger in summer and winter, while UK feeding sites were associated with high densities of L. flavus. Other listed natural enemies of L. niger: the hoverfly Microdon mutabilis (larvae live in nests), the parasitoid Elasmosoma, and a tachinid fly (names from a GBIF/AntWiki-type listing).
(b) Diet of the European Green Woodpecker Picus viridis in the Southern Netherlands (Ardea, about 2008), https://www.researchgate.net/publication/289348206 (title only seen). Feeding sites: Alder D., Marsden S. (2010). Characteristics of feeding-site selection by breeding Green Woodpeckers Picus viridis in a UK agricultural landscape. Bird Study, DOI 10.1080/00063650903437511 (authors unverified). Parasite list: https://www.gbif.org/species/144095179 (search summary only).
(c) verified (abstract/secondary) for woodpecker; unverified for the parasite list
(d) Simulation implication: surface disturbance events where a woodpecker probes the mound and removes workers and brood from the top chambers are grounded. Spider or bird predation of foragers and of newly mated queens is plausible but I found no L. niger-specific numbers.

---

## 5. Hygiene and social immunity

### 5.1 Toilets inside, refuse piles outside (L. niger)
(a) In 21 lab nests of 150 to 300 L. niger workers kept 2 months with coloured sugar water, 1 to 4 faecal patches formed per nest (mean 2.32, median 2; 41 total). 34 of 41 (83%) were in chamber corners. Toilets never contained corpses or food waste; all 21 colonies built distinct refuse piles outside the nest containing dead ants, nest debris and protein food remains.
(b) Czaczkes T.J., Heinze J., Ruther J. (2015). Nest etiquette: where ants go when nature calls. PLoS ONE 10(2): e0118376. DOI 10.1371/journal.pone.0118376
(c) verified
(d) Simulation implication: draw 1 to 4 dark faecal spots in corners/ends of chambers inside the nest; carry corpses and food scraps to a refuse pile outside the entrance (not an internal midden chamber).

### 5.2 Corpse removal (necrophoresis): timing and benefit (other species)
(a) Myrmica rubra: with free removal, no corpse remained in the nest after 4 days; when removal was blocked, ants moved corpses to the nest periphery, away from larvae, by day 12; worker survival after 50 days was about 94.0% with removal versus 87.4% without.
(b) Diez L., Lejeune P., Detrain C. (2014). Keep the nest clean: survival advantages of corpse removal in ants. Biology Letters 10: 20140306. DOI 10.1098/rsbl.2014.0306
(c) verified
(a2) Linepithema humile: life-associated chemicals (dolichodial, iridomyrmecin) vanish within about 40 min of death and triggers necrophoresis; freshly killed workers were carried to refuse piles within 1 to 2 h. M. rubra removed about 15% of freshly killed corpses but about 80% of corpses 1 to 6 days old. Oleic and linoleic acids accumulate post mortem.
(b2) Choe D.H., Millar J.G., Rust M.K. (2009). Chemical signals associated with life inhibit necrophoresis in Argentine ants. PNAS 106: 8251-8255. DOI 10.1073/pnas.0901270106 (pages unverified). M. rubra removal percentages: Diez L. et al. (2013), Post-mortem changes in chemical profile and their influence on corpse removal in ants, Journal of Chemical Ecology (seen as title only).
(c2) verified (abstract/secondary)
(d) Simulation implication: a dead ant becomes a "corpse" object that is picked up after a delay of roughly 1 h to 1 day, carried out to the refuse pile; if the exit is blocked (flood, winter), corpses are moved to a peripheral chamber far from brood. A popular claim that L. niger dumps corpses 2 to 5 m away is unverified.

### 5.3 Pathogen response: social network plasticity (L. niger)
(a) 22 tracked L. niger colonies; 10% of workers (chosen from foragers) were exposed to Metarhizium brunneum spores (n = 11) or sham (n = 11), then tracked for 24 h after a 2 h recovery. Spores penetrate the cuticle 1 to 2 days after exposure, so changes were active host responses. Constitutively, frequent foragers had fewer contacts and were segregated from nurses and the queen. After exposure, colonies increased modularity and clustering, reduced transmission efficiency, increased task segregation and distance between queen and workers, and exposed foragers reduced their contacts. In a 9-day survival experiment, pathogen mortality was higher in foragers than nurses and all queens survived. Metarhizium brunneum is described as a natural pathogen of L. niger.
(b) Stroeymeyt N., Grasse A.V., Crespi A., Mersch D.P., Cremer S., Keller L. (2018). Social network plasticity decreases disease transmission in a eusocial insect. Science 362: 941-945. DOI 10.1126/science.aat4793
(c) verified
(d) Simulation implication: when a forager gets infected, it (and its nestmates) should keep more distance from the brood/queen chambers; foragers mostly stay near the entrance zone; the queen chamber should be deep and rarely visited by foragers.

### 5.4 Architectural immunity (L. niger)
(a) L. niger digging groups exposed to pathogen-treated nestmates dug faster, initially made more tunnels, and after 6 days had entrances spaced about 0.62 cm further apart, network topology less favourable to transmission, and lower chamber centrality. Simulations confirmed reduced transmission.
(b) Architectural immunity: ants alter their nest networks to prevent epidemics. Science (October 2025). DOI 10.1126/science.ads5930 ; preprint https://www.biorxiv.org/content/10.1101/2024.08.30.610481 (author list not retrieved; University of Bristol affiliated).
(c) verified (abstract/secondary)
(d) Simulation implication: after a disease event, bias new digging toward more spread-out entrances and less central, more tree-like chamber connections.

### 5.5 Grooming and social prophylaxis (Lasius)
(a) Nestmates of ants exposed to Metarhizium became about 1.7 times more resistant to infection after interacting with them (social prophylaxis, Lasius neglectus). Exposed ants are heavily groomed, removing spores. In L. neglectus, low-dose spore transfer during grooming causes protective low-level infections.
(b) Ugelvig L.V., Cremer S. (2007). Social prophylaxis: group interaction promotes collective immunity in ant colonies. Current Biology 17: 1967-1971 (pages unverified). Konrad M., et al. (2012). Social transfer of pathogenic fungus promotes active immunisation in ant colonies. PLoS Biology 10(4): e1001300. DOI 10.1371/journal.pbio.1001300
(c) verified (abstract/secondary); species is L. neglectus, not L. niger
(d) Simulation implication: a visible "grooming" interaction where nestmates lick a contaminated ant is realistic in Lasius.

---

## 6. Mutualisms: aphids

### 6.1 L. niger tends aphids above ground but also eats them
(a) Lab L. niger colony (1 queen, about 500 workers) tending Aphis fabae on broad bean: ants accepted honey solution as a substitute for honeydew; when given alternative sugar, predation on aphids increased about 8-fold and tending intensity decreased; alternative prey had no significant effect. Ant tending can raise aphid honeydew output by up to 50% and L. niger mainly benefits A. fabae by removing predators and competitors (latter figures from search summaries).
(b) Offenberg J. (2001). Balancing between mutualism and exploitation: the symbiotic interaction between Lasius ants and aphids. Behavioral Ecology and Sociobiology 49: 304-310. DOI 10.1007/s002650000303
(c) verified (main result); "up to 50%" is verified (abstract/secondary) and its primary source not identified
(d) Simulation implication: honeydew is the colony's main sugar income; when sugar is abundant, foragers should switch to "harvesting" some aphids as protein.

### 6.2 L. niger ants adjust predation to aphid density; colony demand affects aphids
(a) L. niger regulates its predation on attended aphids according to the number of aphids per worker (density-dependent predation). Presence of ant larvae in the colony reduced the growth rate of attended A. fabae colonies (larval protein demand).
(b) Sakata H. (1995). Density-dependent predation of the ant Lasius niger on its mutualistic aphid. Researches on Population Ecology / Ecological Research, DOI 10.1007/BF02515816 (author and year unverified). Larval demand: Ant larval demand reduces aphid colony growth rates in an ant-aphid interaction, PMC4553619 (authors not retrieved).
(c) verified (abstract/secondary)
(d) Simulation implication: more larvae in the nest leads to more aphid predation; fewer aphids per worker leads to more tending and less predation.

### 6.3 Root aphids underground (mainly Lasius flavus; L. niger also tends some)
(a) L. flavus colonies depend on root aphids (e.g. Geoica utricularia, Tetraneura ulmi, Forda marginata, Trama rara, Forda formicaria) kept in special chambers built around grass roots; a few workers stay with the aphids. Pontin found about 3000 aphids (excluding first instars) per L. flavus nest year round in English pasture; 13 root-aphid species are frequently recorded. More than half of mounds had a single aphid species and over 60% of those a single clone persisting across years. L. flavus rescues aphids in a clear preference order and piles and grooms aphid eggs over winter.
(b) Pontin A.J. (1978). The numbers and distribution of subterranean aphids and their exploitation by the ant Lasius flavus (Fabr.). Ecological Entomology 3: 203-207. Ivens A.B.F., Kronauer D.J.C., Pen I., Weissing F.J., Boomsma J.J. (2012). Ants farm subterranean aphids mostly in single clone groups. BMC Evolutionary Biology 12: 106. DOI 10.1186/1471-2148-12-106. Differential transport of a guild of mutualistic root aphids by the ant Lasius flavus, Current Zoology 69(4): 409 (2023), https://pmc.ncbi.nlm.nih.gov/articles/PMC10443613/. Lasius flavus ants protect root aphid eggs from predators and ..., Royal Society Open Science 12: 250217 (2025), DOI 10.1098/rsos.250217.
(c) verified (abstract/secondary); co-author lists partly unverified
(d) Simulation implication: for L. niger, root aphid chambers are a plausible but secondary feature (search summaries say L. niger also tends subterranean aphids; no L. niger numbers found). A few small side chambers along roots holding aphids with 1 to 2 attendants is a reasonable visual.

---

## Gaps

- L. niger digging rate in absolute units (mm3 per ant per hour, or cm3 per day per colony) was not retrieved; Rasse & Deneubourg 2001 is paywalled. Use relative (logistic, proportional-to-workers) behavior.
- Field L. niger nest depth, gallery diameter, chamber size and mound size (0.3 m / 0.7 m, 1.5 to 5 mm, 10 to 20 mm) are from summaries with unconfirmed sources. Khuong 2016 chamber heights (about 5.5 to 6 mm) and widths (about 12 mm) are the only verified L. niger chamber dimensions.
- No verified L. niger-specific data on daily brood relocation timing, preferred brood temperature, or humidity preference; the patterns are borrowed from Solenopsis invicta and Camponotus mus.
- No L. niger study found on rain-triggered entrance plugging or brood evacuation during flooding; mechanisms borrowed from Diacamma indicum and Formica selysi. Flood "learning" is shown only in F. selysi (individual role memory), not at colony level in L. niger.
- Flood mortality curves for L. niger come from a cited lab study (9 to 10 weeks survival) that I did not open directly.
- No quantitative predation rates on L. niger (spiders, birds, beetles), nor predation on founding queens after nuptial flights.
- Drought tolerance: only qualitative (L. niger most plastic among Lasius); the paper's citation could not be completed.
- Cold: worker survival percentages and CTmin need confirmation from the primary text.
- Necrophoresis latency and refuse-pile distance for L. niger specifically are not verified; timing comes from Myrmica rubra and Linepithema humile.
- L. niger root aphid numbers and how much of the L. niger diet is honeydew (versus prey) were not found.
- Several author lists and page numbers are incomplete and marked unverified inline.

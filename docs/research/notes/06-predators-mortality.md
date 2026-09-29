# 06. Predators, enemies and mortality (for a Lasius niger colony simulation)

Scope: forager mortality in the field, the enemies of Lasius niger and similar temperate ants grouped into functional archetypes, colony-level mortality, and ant defence. Purpose: give the simulation a small set of threat archetypes with defensible rates.

Confidence tags:

- **verified**: I read the primary text or the publisher abstract directly and the number appears there.
- **secondary**: the number comes from a citing paper, a publisher landing summary, or a search-engine summary of the paper. The citation is real, but I did not read the number in the original.
- **unverified**: plausible, but the source could not be confirmed, or the claim comes only from hobbyist or popular pages.

A general caveat: almost all hard numbers on forager mortality come from desert or harvester ants (Cataglyphis, Pogonomyrmex). No field study of daily forager mortality in Lasius niger was found. Rates for Lasius below are extrapolations and are labelled as such.

---

## 1. Forager mortality in the field

### 1.1 Cataglyphis bicolor: 16.4% of foragers lost per day, mostly to predators

- (a) Marked foragers in southern Tunisia disappeared at a constant **16.4% per day**. Half-life as a forager was **4.2 days** and life expectancy was **6.1 days**. The authors attribute the losses mainly to **predation, chiefly by spiders and robber flies (Asilidae)**. A constant daily loss rate means the risk does not depend on age: foragers do not die of old age.
- (b) Schmid-Hempel, P. & Schmid-Hempel, R. (1984). Life duration and turnover of foragers in the ant Cataglyphis bicolor (Hymenoptera, Formicidae). *Insectes Sociaux* 31: 345 to 360. DOI 10.1007/BF02223652. https://link.springer.com/article/10.1007/BF02223652
- (c) **verified** (publisher abstract). Gordon & Hölldobler (1987) independently cite a half-life of about 6 days for these foragers.
- (d) Simulation implication: model forager death as a constant per-trip or per-hour hazard, not as an age limit. 16%/day is an upper bound for a hot, open, predator-rich habitat.

### 1.2 Pogonomyrmex owyheei: forager life expectancy about 14 days ("disposable caste")

- (a) Fewer than **10%** of a colony's workers forage at any one time. Foragers and defenders had an average life expectancy of about **14 days** (roughly 7% loss per day). Workers lost about 40% of their dry weight and showed increasing mandible wear as they moved from interior tasks to foraging. The authors argue that colonies make foragers "disposable" by sending out only old, depleted workers.
- (b) Porter, S.D. & Jorgensen, C.D. (1981). Foragers of the harvester ant, Pogonomyrmex owyheei: a disposable caste? *Behavioral Ecology and Sociobiology* 9: 247 to 256. DOI 10.1007/BF00299879. https://link.springer.com/article/10.1007/BF00299879
- (c) **verified** for the 14-day figure (it is restated in the primary text of Gordon & Hölldobler 1987, which I read). **secondary** for the <10% and 40% figures (publisher summary).
- (d) Simulation implication: only a small outside fraction (about 10%) is exposed to surface hazards, and it consists of the oldest workers. Losing them costs the colony little future labour.

### 1.3 Pogonomyrmex barbatus and P. rugosus: exterior workers survive at least 33 days; patrollers die fastest

- (a) 3521 workers were marked in 38 mature P. barbatus colonies (1169 foragers, 895 patrollers, 1150 nest maintenance workers, 307 midden workers). Exterior workers were resighted up to **33 days** after marking. Longevity ranked **nest maintenance > foraging > patrolling**. Patrollers disappear fastest because they respond most actively to intruders and disturbance. Foragers are subject to predation by horned lizards (Phrynosoma).
- (b) Gordon, D.M. & Hölldobler, B. (1987). Worker longevity in harvester ants (Pogonomyrmex). *Psyche* 94: 341 to 346. PDF: https://web.stanford.edu/~dmgordon/previous/Gordon1987Longevity.pdf
- (c) **verified** (full text read).
- (d) Simulation implication: the defensive or patrolling role should carry a higher hazard than foraging, and foraging a higher hazard than in-nest work.

### 1.4 Pogonomyrmex badius: forager longevity averaged 18 days in the field versus months in the lab

- (a) Florida harvester ant foragers died within **27 days** of starting to forage, and forager longevity averaged **18 days**. The forager population declined when mortality exceeded **4% per day**. Residual lifespan rose by 57% when colonies were penned for 20 days, and up to 8-fold when foragers were kept in the lab. In nature, therefore, foragers die of extrinsic causes, not senescence.
- (b) Kwapich, C.L. & Tschinkel, W.R. (2016). Limited flexibility and unusual longevity shape forager allocation in the Florida harvester ant (Pogonomyrmex badius). *Behavioral Ecology and Sociobiology* 70: 221 to 235. DOI 10.1007/s00265-015-2039-1
- (c) **secondary** (search summary of the abstract; the publisher page redirected to a login).
- (d) Simulation implication: a baseline outside hazard of roughly 4 to 6% per forager per active day is realistic for a temperate or subtropical ant. Pulling foragers indoors (bad weather, alarm) directly extends their life.

### 1.5 Predator-induced shutdowns: harvester ants stop foraging after losing 25 to 50% of foragers

- (a) In artificial predation experiments (foragers removed at 5 or 10 per day), Pogonomyrmex desertorum stopped activity for up to **5 days** after losing about **25%** of its forager population. P. rugosus did not respond to a 25% loss, stopped at a **50%** loss, and showed "frenzied" activity at a **75%** loss. Texas horned lizards (Phrynosoma cornutum) sit beside foraging trails and eat passing foragers.
- (b) Whitford, W.G. & Bryant, M. (1979). Behavior of a predator and its prey: the horned lizard (Phrynosoma cornutum) and harvester ants (Pogonomyrmex spp.). *Ecology* 60: 686 to 694. PDF: https://jornada.nmsu.edu/files/bibliography/79-Ecology-Whitford.pdf
- (c) **secondary** (search summary of the PDF; the page and volume numbers are from my memory of the citation and are **unverified**).
- (d) Simulation implication: a "sit-and-wait predator at the trail" archetype should cause the colony to suppress foraging for a few days once cumulative losses pass a threshold of about 25%.

### 1.6 Lasius niger worker lifespan (laboratory) and the implication for field mortality

- (a) Under laboratory conditions, workers produced early in colony founding had a life expectancy of about **430 days** (maximum 1129 days). Workers produced later had about **310 days** (maximum 1094 days). The authors describe L. niger as having typical age polyethism, with foragers being the oldest workers, and note higher extrinsic mortality in foraging. Kutter & Stumper (1969) are cited for a 1 to 2 year average worker lifespan and a 28-year maximum queen lifespan.
- (b) Kramer, B.H., Schrempf, A., Scheuerlein, A. & Heinze, J. (2016). Worker lifespan is an adaptive trait during colony establishment in the long-lived ant Lasius niger. *Experimental Gerontology* 85: 18 to 23. DOI 10.1016/j.exger.2016.09.008. https://www.sciencedirect.com/science/article/pii/S0531556516303345
- (c) **secondary** (search summaries of the paper; author list and volume are from the PubMed record listing 27620822, which I saw only as a search result).
- (d) Simulation implication: in-nest workers should almost never die (a lab lifespan of about 1 year). Nearly all worker turnover should happen on the surface, which the harvester ant data put at weeks, not months.

### 1.7 Causes-of-death breakdown

- (a) No study found gives a quantitative split of forager deaths into predation, desiccation, getting lost, and fights. Schmid-Hempel & Schmid-Hempel (1984) attribute most Cataglyphis losses to predation (spiders, robber flies). Gordon & Hölldobler (1987) list energy expenditure, predation and environmental fluctuation as candidate causes without quantifying them.
- (c) **verified** (absence of the breakdown in the sources I read). Any split is an assumption.
- (d) Simulation implication: treat the split as a tunable design parameter. A reasonable default (assumption, not data) is predation about 60 to 70%, heat or desiccation 10 to 20%, fights with other ants 10 to 20%, getting lost under 5%.

---

## 2. Enemies of Lasius niger, grouped by function

### 2.1 Archetype A: surface predators of individual foragers

**A1. Ant-specialist spiders (Zodarion).**
- (a) Zodarion spiders are ant specialists. Z. rubidum is a Formicinae specialist that paralyses Lasius and Formica more efficiently than myrmicine ants. Capture success was best on medium-sized ants such as Lasius and Formica. Juvenile Z. rubidum consumed at most **2 Lasius** in a row. Juveniles of Z. styliferum captured **21 ants in 26 days** (about 0.8 per spider per day).
- (b) Pekár, S. (2005). Predatory characteristics of ant-eating Zodarion spiders (Araneae: Zodariidae): potential biological control agents. *Biological Control* 34: 196 to 203. https://www.sciencedirect.com/science/article/abs/pii/S1049964405001325. Also (authors and year not confirmed; DOI prefix suggests 2007 or 2008): Dietary and prey-capture adaptations by which Zodarion germanicum, an ant-eating spider, specialises on the Formicinae. *Naturwissenschaften*. DOI 10.1007/s00114-007-0322-3
- (c) **secondary** (search summaries; volume and pages for the Biological Control paper are **unverified**).
- (d) Simulation implication: one ant-eating spider near the nest takes roughly **1 forager per day**. With a few spiders in the foraging area, this gives a few foragers per day out of hundreds.

**A2. General spiders, robber flies, wolf spiders.**
- (a) Spiders and robber flies were the main predators of Cataglyphis foragers (see 1.1). General field spiders are reported to eat about one appropriately sized insect per day and wolf spiders show low field feeding rates.
- (b) Schmid-Hempel & Schmid-Hempel (1984) as above. Wolf spider feeding: "Evidence of low daily food consumption by wolf spiders in meadowland" (1990), *Journal of Applied Entomology*, https://onlinelibrary.wiley.com/doi/abs/10.1111/j.1439-0418.1990.tb00097.x
- (c) Cataglyphis: **verified**. Wolf spider feeding rate: **secondary** (search summary; authors not confirmed).
- (d) Simulation implication: a generic "surface predator" entity with about 1 kill per day, preferring solitary or slow ants.

**A3. Antlions (Euroleon nostras, Myrmeleon formicarius).**
- (a) Pit-building antlion larvae take mainly ants. Ants that fall into the pit have their escape time affected by the pit and the larva.
- (b) "Effects of larval antlions Euroleon nostras (Neuroptera, Myrmeleontidae) and their pits on the escape-time of ants" (ResearchGate record 227534432). https://www.researchgate.net/publication/227534432
- (c) **secondary** (search summary; authors and capture rates not obtained). The claim that ants dominate antlion diets is **secondary**.
- (d) Simulation implication: a static trap tile in sandy, dry surface patches. Low rate (well under 1 ant per day per pit), but a good visual feature for a screensaver.

**A4. Ladybirds and other aphid predators (not a threat to ants).**
- (a) Adalia bipunctata ladybirds avoid patches with Lasius niger and avoid laying eggs there. L. niger deters ladybirds and increases the number of surviving aphids.
- (b) Oliver, T.H., Jones, I., Cook, J.M. & Leather, S.R. (2008). Avoidance responses of an aphidophagous ladybird, Adalia bipunctata, to aphid-tending ants. *Ecological Entomology* 33. DOI 10.1111/j.1365-2311.2008.01009.x. Also: "Black garden ants reduce aphid predation by native and invasive ..." *Ecological Entomology*, DOI 10.1111/een.70033.
- (c) **secondary** (search summaries; authors of the 2008 paper are from my memory and **unverified**).
- (d) Simulation implication: ladybirds belong in the "competitor at aphid colonies" role. Ants chase them away and rarely die.

### 2.2 Archetype B: nest raiders and diggers

**B1. Green woodpecker (Picus viridis).**
- (a) Ants are the main food of the green woodpecker, especially Lasius and Formica. In half-open landscapes in the southern Netherlands the diet was mainly **Lasius niger**, in both summer and winter. Formica mound species made up only **14%** of identified prey in the winter woodland diet. In UK farmland, feeding sites were short, species-rich grassland with high ant density, especially Lasius flavus. The bird digs into nests and takes workers and brood with its long sticky tongue.
- (b) Kolsters, J., Wouters, P. & de Veer, W. (2014). Diet of the European Green Woodpecker Picus viridis in the southern Netherlands. *Limosa* 87(2): 74 to 81. https://www.researchgate.net/publication/289348206. Feeding sites: Alder, D. & Marsden, S. (2010). Characteristics of feeding-site selection by breeding Green Woodpeckers Picus viridis in a UK agricultural landscape. *Bird Study* 57. DOI 10.1080/00063650903437511
- (c) **secondary** (search summaries; author names for Bird Study are from my memory and **unverified**; no per-nest attack frequency found).
- (d) Simulation implication: a rare, dramatic "dig from above" event that removes a cone of soil and takes brood and workers from the top chambers. Winter attacks (snow-free ground) are plausible.

**B2. Temporary social parasite queens (Lasius umbratus and relatives).**
- (a) Lasius umbratus queens found colonies by entering an established nest of another Lasius species, including L. niger, killing the resident queen, and using the host workers to raise their brood. The host colony is gradually replaced.
- (b) AntWiki, Lasius umbratus page, and Wikipedia; no primary paper was reached. https://www.antwiki.org/wiki/Lasius_umbratus
- (c) **unverified** as to rate. The biology is well known, but I have no frequency data.
- (d) Simulation implication: a rare colony-ending event (queen replaced, workers change colour over a year or two). Probably too rare to include by default.

**B3. Slave-making and raiding ants (Formica sanguinea, Polyergus).**
- (a) Formica sanguinea is a facultative slave-maker that raids nests of Formica fusca, F. cunicularia, Lasius emarginatus and others for pupae. Evidence of raids specifically on Lasius niger was not found.
- (b) (Authors and year not confirmed.) Raiding and foraging behavior of the blood-red ant, Formica sanguinea. *Journal of Insect Behavior*. DOI 10.1023/A:1007766303588
- (c) **secondary** for the host list; **unverified** for any L. niger raids.
- (d) Simulation implication: optional brood-raid event by a column of larger red ants. Keep it rare.

**B4. Badgers and other mammals.**
- (a) No source found giving rates of badger or mammal digging on Lasius niger nests.
- (c) **unverified**.
- (d) Simulation implication: omit, or fold into B1 as a generic "large digger".

### 2.3 Archetype C: threats to reproductives (alates and founding queens)

**C1. Aerial predators during the nuptial flight (gulls, swifts, swallows, dragonflies).**
- (a) Swifts, swallows and gulls feed heavily on rising swarms of winged Lasius niger in July. Dragonflies (e.g. Anax junius, Pachydiplax longipennis in North America) catch queens in flight and eat the gaster. No study found quantifies the fraction of alates eaten.
- (b) Natural History Museum, London: "Flying ant day: when winged ants take their nuptial flight." https://www.nhm.ac.uk/discover/when-why-winged-ants-swarm-nuptial-flight.html. AntWiki "Nuptial flights and mating" page (403 on fetch).
- (c) **unverified** for any rate; the behaviour itself is **secondary** (popular and museum sources).
- (d) Simulation implication: on flight day, spawn bird sprites over the surface strip that pick off a large share of airborne alates. The exact fraction is a design choice.

**C2. Founding-queen mortality overall.**
- (a) After the flight, L. niger queens reach very high densities on the ground and avoid areas used by workers of established colonies. **18%** of founding nests in the field held more than one queen. In the lab, brood raiding between incipient colonies occurred, and colonies with more workers won. Queens in associations fight until one remains.
- (b) Sommer, K. & Hölldobler, B. (1995). Colony founding by queen association and determinants of reduction in queen number in the ant Lasius niger. *Animal Behaviour* 50: 287 to 294. https://www.sciencedirect.com/science/article/abs/pii/S0003347285702448
- (c) **secondary** (search summary of the abstract).
- (a2) In a 2024 lab study, L. niger queens from a "normal" population had **under 20% survival after 130 days** in pleometrotic groups, and queen fights killed **all** resident queens in over 60% of colonies.
- (b2) Queens from a unique hyper-dense Lasius niger population tolerate pleometrosis better than queens from a "normal" population. bioRxiv preprint, 2024. DOI 10.1101/2024.07.16.603683
- (c2) **secondary** (search summary; author list not obtained; preprint, not peer-reviewed).
- (a3) For comparison, in Pogonomyrmex barbatus there are **many hundreds** of founding nests after each mating flight but only **10 to 50** survive to become 1-year-old colonies. Survival to year 1 depends on rainfall and on distance to the nearest established neighbour. Gordon has summarised that about **1%** of female alates found viable colonies.
- (b3) Sundaram, M., Steiner, E. & Gordon, D.M. (2022). Rainfall, neighbors, and foraging: the dynamics of a population of red harvester ant colonies 1988 to 2019. *Ecological Monographs* 92: e1503. DOI 10.1002/ecm.1503. The 1% figure is attributed to Gordon & Kulig (1996), Founding, foraging, and fighting: colony size and the spatial distribution of harvester ant nests, *Ecology* 77: 2393 to 2409.
- (c3) Sundaram et al.: **verified** (full text read). The 1% figure: **secondary**.
- (a4) First-year mortality of young colonies has been reported as about **0.04** in P. barbatus versus **0.4** in P. occidentalis.
- (c4) **secondary** (search summary; primary source not identified). Treat with caution.
- (d) Simulation implication: of queens that land, well over 95% should fail before year 1. Main killers: established-colony workers near landing sites, other founding colonies raiding brood, fights between co-founding queens, and weather. The simulated colony's own alates should almost all "leave and vanish".

### 2.4 Archetype D: parasites, parasitoids and pathogens

**D1. Phorid decapitating fly Pseudacteon formicarum.**
- (a) P. formicarum is a common European parasitoid of Lasius niger and L. emarginatus. It parasitises only workers and is host-specific to Lasius sensu stricto. The fly finds hosts using the **formic acid** released by the ant's venom gland. The larva develops in the head capsule, and the head eventually falls off. An overall infestation rate of **at least 50%** was estimated for the studied populations.
- (b) Maschwitz, U., Weissflog, A., Seebauer, S., Disney, R.H.L. & Witte, V. (2008). Studies on European ant decapitating flies (Diptera: Phoridae): I. Releasers and phenology of parasitism of Pseudacteon formicarum. *Sociobiology* 51: 127 to 140. https://www.researchgate.net/publication/286515041
- (c) **secondary** (search summaries; ResearchGate returned 403). What "50% infestation" is measured over (colonies versus workers) is unclear.
- (d) Simulation implication: small flies hovering over foraging trails and fights, attracted by alarm and formic acid. The effect is slow worker loss plus reduced foraging while flies are present. Tie phorid attraction to recent formic acid use.

**D2. Generalist entomopathogenic fungi (Metarhizium, Beauveria).**
- (a) In Formica selysi, **Paecilomyces lilacinus** was found in **44%** of colonies, **Beauveria bassiana** in **17%**, and **Metarhizium brunneum** in none of the active colonies. All three killed experimentally challenged ants. Separately, a 1-year field study of leaf-cutting ants found **under 0.1%** of live individuals infected despite high Metarhizium levels in the soil (up to 5000 conidiospores per gram). Natural colony-wide epidemics have not been recorded in ants.
- (b) Reber, A. & Chapuisat, M. (2012). Diversity, prevalence and virulence of fungal entomopathogens in colonies of the ant Formica selysi. *Insectes Sociaux* 59: 231 to 239. DOI 10.1007/s00040-011-0209-3. The <0.1% figure is cited in Pull, C.D. et al. (2018). Destructive disinfection of infected brood prevents systemic disease spread in ant colonies. *eLife* 7: e32073. https://elifesciences.org/articles/32073
- (c) Reber & Chapuisat: **secondary** (search summary of the abstract). The <0.1% figure: **secondary** (search summary quoting eLife).
- (d) Simulation implication: fungus is a low-rate, background killer of individuals (well under 1% of workers carrying infection at any time). Hygiene (grooming, corpse removal, killing infected brood) should reliably stop spread. Colony-wide collapse from fungus should not happen.

**D3. Laboulbeniales (ectoparasitic fungi).**
- (a) Laboulbenia formicarum is **rare on native Lasius** but infected **58%** of colonies of the invasive Lasius neglectus, with prevalence rising about **14% per year** until about **80%** of ants were infected. Rickia wasmannii on Myrmica reaches an average prevalence of **38%** in Myrmica sabuleti and up to 100% within some Myrmica scabrinodis colonies, and increases infected workers' need for water.
- (b) Tragust, S., Feldhaar, H., Espadaler, X. & Pedersen, J.S. (2015). Rapid increase of the parasitic fungus Laboulbenia formicarum in supercolonies of the invasive garden ant Lasius neglectus. *Biological Invasions* 17: 2795 to 2801. DOI 10.1007/s10530-015-0917-0. Gippet, J.M.W. et al. (2021). Land-cover and climate factors contribute to the prevalence of the ectoparasitic fungus Laboulbenia formicarum in its invasive ant host Lasius neglectus. *Fungal Ecology*. https://www.sciencedirect.com/science/article/pii/S1754504821000076. Haelewaters, D. et al. (2015). Studies of Laboulbeniales on Myrmica ants (II). *Contributions to Zoology*. https://jhr.pensoft.net/article_preview.php?id=4951
- (c) **secondary** (search summaries).
- (d) Simulation implication: for native L. niger, omit or use a cosmetic low-prevalence effect (a slight increase in desiccation risk).

**D4. Microdon hoverflies (brood predators inside the nest).**
- (a) Microdon larvae live inside ant nests and eat small and medium ant brood, avoiding large larvae and pupae. Microdon mutabilis is associated mainly with Formica lemani, and is also listed from Formica fusca, Lasius niger and Myrmica ruginodis. M. myrmicae larvae can be numerous in Myrmica nests but probably have little effect on host colony fitness.
- (b) "Life cycle and growth pattern of the endangered myrmecophilous Microdon myrmicae (Diptera: Syrphidae)" (2012). *European Journal of Entomology* 109(3). https://www.eje.cz/pdfs/eje/2012/03/20.pdf. (Authors and year not confirmed.) Host recognition by the specialist hoverfly Microdon mutabilis, a social parasite of the ant Formica lemani. *Journal of Chemical Ecology*. https://www.researchgate.net/publication/5664703
- (c) **secondary**. The Lasius niger host record is **unverified** (it appears in general summaries, and M. mutabilis is known to be highly host-specific).
- (d) Simulation implication: optional slow "brood thief" inside chambers. Low impact, good for visual interest (slug-like larvae in brood chambers).

**D5. Acute bee paralysis virus and insecticides.**
- (a) A PMC paper reports varying impact of neonicotinoid insecticide and acute bee paralysis virus across castes and colonies of Lasius niger.
- (b) "Varying impact of neonicotinoid insecticide and acute bee paralysis virus across castes and colonies of black garden ants, Lasius niger." https://www.ncbi.nlm.nih.gov/pmc/articles/PMC8519937/
- (c) **unverified** (title only; no numbers read).
- (d) Simulation implication: omit.

### 2.5 Archetype E: competitors (other ants)

**E1. Neighbouring Lasius niger colonies.**
- (a) Pontin's field experiments on Lasius niger and L. flavus showed that when neighbouring colonies were poisoned, sexual production increased, indicating strong intraspecific competition. Declines of L. niger coincided with increases of L. flavus. A 2024 preprint reports **low aggression** between workers from different L. niger nests within nest complexes.
- (b) Pontin, A.J. (1961). Population stabilization and competition between the ants Lasius flavus (F.) and L. niger (L.). *Journal of Animal Ecology* 30. Pontin, A.J. (1963). Further considerations of competition and the ecology of the ants Lasius flavus (F.) and L. niger (L.). *Journal of Animal Ecology* 32. Pontin, A.J. (1960). Field experiments on colony foundation by Lasius niger (L.) and L. flavus (F.). *Insectes Sociaux* 7: 227 to 230. Preprint: A new trait in a well-studied ant: low aggression between workers from Lasius niger nest complexes. bioRxiv 2024, DOI 10.1101/2024.07.23.604725
- (c) **secondary** (citing summaries; I did not read Pontin's text).
- (d) Simulation implication: neighbours compete mainly by reducing food and growth, not by killing. Border skirmishes can be infrequent and low-lethality.

**E2. Pavement ant Tetramorium caespitum.**
- (a) Pavement ant territorial battles between T. caespitum colonies are large but ritualised. Pushing dominates, and "few, if any, ants die". The cost is mainly lost foraging time while fighting. A search summary claims that T. caespitum aggression was highest toward Lasius niger, but I could not confirm which paper this came from.
- (b) Hoover, K.M. et al. (2016). The organization of societal conflicts by pavement ants Tetramorium caespitum: an agent-based model of amine-mediated decision making. *Current Zoology* 62. DOI 10.1093/cz/zow041. https://pmc.ncbi.nlm.nih.gov/articles/PMC5829439/
- (c) Low mortality in conspecific battles: **verified** (full text read). Aggression toward L. niger being highest: **unverified**.
- (d) Simulation implication: Tetramorium encounters at food sources produce mass wrestling with little death. Lasius should lose some foragers and retreat from contested baits.

**E3. Formica (wood ants and relatives).**
- (a) No quantitative data on Formica killing Lasius niger foragers was found in this pass. Battles between ants are reviewed in Champer & Schlenoff (2024).
- (b) Champer, J. & Schlenoff, D. (2024). Battles between ants (Hymenoptera: Formicidae): a review. *Journal of Insect Science* 24(3): 25. DOI 10.1093/jisesa/ieae064
- (c) Review exists: **verified** (bibliographic record). No numbers extracted.
- (d) Simulation implication: a larger, more lethal competitor than Tetramorium. Use sparingly.

---

## 3. Colony-level mortality of established colonies

### 3.1 Pogonomyrmex badius: 25%/year for small colonies, 6%/year for medium, about 3% for large

- (a) Over 400 colonies were resurveyed for 6 years. Annual mortality fell from about **25%** for the smallest colonies (<500 workers) to about **6%** for mid-sized colonies. These extrapolate to lifespans of about **4** and **17 years**. Only **28%** of colonies under 500 workers survived 3 years. Over **90%** of the largest colonies (43 of 47) were alive after 6 years. If the largest colonies live about 30 years, their annual turnover would be about **3%**. Mean colony size was about 2500 workers.
- (b) Tschinkel, W.R. (2017). Lifespan, age, size-specific mortality and dispersion of colonies of the Florida harvester ant, Pogonomyrmex badius. *Insectes Sociaux* 64: 285 to 296. DOI 10.1007/s00040-017-0544-0
- (c) **verified** (full text read).
- (d) Simulation implication: colony death risk should depend strongly on colony size. A small colony should be fragile, while a colony of several thousand should have only a few percent annual risk.

### 3.2 Other colony lifespans reviewed in Tschinkel (2017)

- (a) P. owyheei: 122 colonies over 9 years, average lifespan **17 years** (Porter & Jorgensen 1988). P. barbatus: annual mortality suggests **20 to 25 years** (Gordon & Kulig 1996; Sanders & Gordon 2004). P. occidentalis: about **5 years** for small colonies to about **40** for large ones (Wiernasz & Cole 1995), and proximity to larger colonies greatly increased small-colony mortality. Formica montana: 69 colonies over 33 years, lifespans of **23** and **14 years** for two cohorts. Myrmecocystus depilis about **17 years**, M. mexicanus **39**, Novomessor cockerelli **8**. Monogyne Formica selysi nests about **10 years**, and polygyne nests about 3 times as long.
- (b) Tschinkel (2017), as above, citing the originals.
- (c) **verified** (read in Tschinkel 2017) as a secondary statement of the originals.
- (d) Simulation implication: a mature colony lifespan of 10 to 30 years is the realistic range. For monogyne L. niger, queen lifespan (up to 28 years in the lab) sets an upper bound.

### 3.3 Pogonomyrmex barbatus: 750 of 1057 colonies died between 1988 and 2020; drought and crowding drive mortality

- (a) Around **300** colonies were present each year. Of **1057** colonies identified from 1988 to 2020, **750** died. Colonies live about **30 years**. Annual survival depended on colony age and on foraging area (Voronoi area relative to neighbours). As rainfall fell after 2001 to 2003, survival and recruitment declined, and in **2016** survival of "young adult" colonies aged **5 to 10 years** dropped significantly. A colony is scored dead after two successive years of inactivity. Because workers live about a year, the colony can persist about a year after the queen dies. Earlier data (1997 to 2002) showed higher colony mortality after low summer rainfall. Only about **25%** of mature colonies had ever produced an offspring colony.
- (b) Sundaram, M., Steiner, E. & Gordon, D.M. (2022). *Ecological Monographs* 92: e1503. DOI 10.1002/ecm.1503. Also Glinka, F., Steiner, E.B., Privman, E. & Gordon, D.M. (2026). Shifts in demography in changing ecological conditions in a dependent-lineage population of harvester ant colonies. *Ecology and Evolution* 16(8): e74228. DOI 10.1002/ece3.74228 (155 colony deaths 2012 to 2024; median survival 19 and 22 years for the two lineages).
- (c) **verified** (full text of Sundaram et al.; Glinka et al. read via PMC summary).
- (d) Simulation implication: with a stable population of about 300 and 10 to 50 recruits per year, the implied adult colony mortality is roughly **3 to 17% per year** (my derivation). Colony death is mostly slow starvation from drought and crowding, not a sudden catastrophe. When the queen dies, the colony should fade over about a year as workers die off.

### 3.4 Pogonomyrmex occidentalis: about 2% annual mortality of established colonies

- (a) Of 56 colonies marked in 1977, 37 were alive in 1991: about **1.27 deaths per year**, **66.1%** survival over 15 years, and a projected colony life expectancy of **43.5 years**. Colonies marked between 1977 and 1994 lived a mean of about 15.65 years.
- (b) Keeler, K.H. (1993). Fifteen years of colony dynamics in Pogonomyrmex occidentalis, the western harvester ant, in western Nebraska. *Southwestern Naturalist*. https://digitalcommons.unl.edu/cgi/viewcontent.cgi?article=1288&context=bioscifacpub
- (c) **secondary** (search summary of the PDF; author, year and journal are from my memory of this citation and are **unverified**).
- (d) Simulation implication: a well-established colony in a stable site can have an annual death risk as low as 2%.

### 3.5 Lasius niger survives three weeks of summer flooding

- (a) In the Jena Experiment grassland, **85%** of plots were occupied by L. niger both before (2006) and after (2013) a 3-week summer flood that submerged plots for **4 to 24 days** (mean 14). The authors rule out recolonisation and suggest individual tolerance of submersion (earlier lab work: workers survive underwater over 9 weeks with air bubbles) plus air trapped in nests.
- (b) Hertzog, L.R., Ebeling, A., Meyer, S.T., Eisenhauer, N., Fischer, C., Hildebrandt, A., Wagg, C. & Weisser, W.W. (2016). High survival of Lasius niger during summer flooding in a European grassland. *PLOS ONE* 11(11): e0152777. DOI 10.1371/journal.pone.0152777
- (c) **verified** (full text read).
- (d) Simulation implication: flooding should not kill an L. niger colony. It is a good "disaster the colony survives" event (water fills lower tunnels, ants cluster in air pockets).

### 3.6 Lasius niger established-colony mortality

- (a) No field study of annual mortality of established L. niger colonies was found. Queens can live up to 28 years in the lab (Kutter & Stumper 1969, cited in Kramer et al. 2016).
- (c) **secondary** for the queen lifespan. The field colony mortality rate is a **gap**.
- (d) Simulation implication: borrow the size-dependent harvester ant curve (3.1) as a stand-in.

---

## 4. Defence

### 4.1 Formic acid and the formicine alarm-defence system

- (a) Formicine workers eject a mixture of formic acid (poison gland) and Dufour's gland secretion at enemies. In L. niger, L. alienus and L. flavus, the abdominal volatiles identified come from **Dufour's gland**, whose main component is **n-undecane**. Formic acid alone raises general activity, while undecane causes aggression and attraction toward the source. Lasius species that release only small droplets reportedly contain formic acid at about **2 to 5%**, compared with up to 60% in some formicine venoms.
- (b) Regnier, F.E. & Wilson, E.O. (1969). The alarm-defence system of the ant Lasius alienus. *Journal of Insect Physiology* 15: 893 to 898. https://www.sciencedirect.com/science/article/abs/pii/0022191069901292. Regnier, F.E. & Wilson, E.O. (1968). The alarm-defence system of the ant Acanthomyops claviger. Wilson, E.O. & Regnier, F.E. (1971). The evolution of the alarm-defense system in the formicine ants. *American Naturalist* 105: 279 to 289. DOI 10.1086/282724. Koch, L. et al. (2025). Acid reign: formicine ants and their venoms. *Myrmecological News* 35. https://www.antwiki.org/w/images/a/a9/Koch,_L._et_al._(2025)_Acid_reign_(10.25849@myrmecol.news_035&001).pdf
- (c) Undecane as the Lasius alarm pheromone and its role versus formic acid: **secondary** (search summaries). Authorship and pages of Regnier & Wilson 1969: **unverified** (ScienceDirect returned 403). The 2 to 5% figure: **unverified** (origin unclear).
- (d) Simulation implication: alarm = an undecane-like volatile that attracts nestmates and makes them aggressive near the source. Formic acid spray is a short-range, local attack that also raises activity. Use of formic acid can attract phorid flies (D1).

### 4.2 Lasius flavus alarm and trail pheromones identified (2025)

- (a) In Lasius flavus, **2,6-dimethyl-5-heptenol (DMH)** increased aggression at **1 pg/l or more**, consistent with an alarm pheromone. **(R)-mellein** is the trail pheromone and was active at **0.01 pg per cm**, the lowest known detection threshold for an ant pheromone. DMH was 100-fold less active as a trail signal.
- (b) Butterfield, T., Bacon, J. & Hill, E.M. (2025). Identification of trail following and alarm pheromones of Lasius flavus using bioassay-directed fractionation. *Journal of Chemical Ecology*. DOI 10.1007/s10886-025-01651-w. https://pmc.ncbi.nlm.nih.gov/articles/PMC12513911/
- (c) **secondary** (search summary of the abstract).
- (d) Simulation implication: a head-derived alarm component exists in Lasius alongside the Dufour's gland one. Treat "alarm" as one short-lived, fast-spreading field.

### 4.3 Social immunity: formic acid as a disinfectant

- (a) Lasius neglectus workers groom fungus-exposed brood, removing spores mechanically. They also take up poison from the acidopore into the mouth and apply it to brood. Formic acid is the main active component and inhibits germination of spores left on brood and in the groomer's mouth. Infected pupae are later destroyed ("destructive disinfection") before the fungus can sporulate.
- (b) Tragust, S., Mitteregger, B., Barone, V., Konrad, M., Ugelvig, L.V. & Cremer, S. (2013). Ants disinfect fungus-exposed brood by oral uptake and spread of their poison. *Current Biology* 23: 76 to 82. DOI 10.1016/j.cub.2012.11.034. Pull, C.D. et al. (2018). *eLife* 7: e32073.
- (c) **secondary** (search summaries; DOI of Tragust et al. is from my memory and **unverified**).
- (d) Simulation implication: a visible "grooming" behaviour on brood, and occasional removal of a sick pupa to the midden, keep fungus from spreading.

### 4.4 Closing nest entrances

- (a) Hobbyist reports state that L. niger workers block entrances with soil particles when it is cold (a figure of below 14 degrees C is quoted) and seal the nest over winter, leaving small holes. Harvester and other ants are reported to plug entrances with soil or heads when threatened.
- (b) Hobbyist colony journal: https://www.antnest.co.uk/wp-content/uploads/2019/09/lniger1.pdf and forum posts.
- (c) **unverified** (no peer-reviewed source found).
- (d) Simulation implication: entrance plugging at night, in cold weather, and before winter is a plausible visual behaviour. Do not make it a response to predators without better evidence.

### 4.5 Aphid defence

- (a) L. niger drives off ladybirds (Adalia bipunctata) and increases aphid survival (see A4).
- (c) **secondary**.
- (d) Simulation implication: ants at aphid colonies should attack approaching ladybird sprites.

---

## Recommended threat archetypes

Rates are for a mature L. niger colony of a few thousand workers during the active season (roughly April to October), unless noted. Where the basis is another species, the rate is an extrapolation.

| Archetype | Targets | Suggested rate or frequency | Basis species | Confidence |
|---|---|---|---|---|
| 1. Background surface hazard (spiders, beetles, robber flies, heat) | Foragers only | Constant hazard of about 3 to 6% of active foragers per active day (upper bound 16%/day in hot, open habitat). No in-nest deaths. | Pogonomyrmex badius (18 d, 4%/day threshold), P. owyheei (14 d), Cataglyphis bicolor (16.4%/day) | Medium: rates verified for these species, extrapolation to Lasius is an assumption |
| 2. Sit-and-wait predator (ant-eating spider, antlion pit, horned-lizard analogue) | Foragers on a trail or patch | About 1 kill per predator per day. Colony pauses foraging on that side for 1 to 5 days after losing about 25% of foragers. | Zodarion spp. (about 0.8 ants/day), Pogonomyrmex spp. with horned lizards | Medium to low |
| 3. Ant competitor at food (Tetramorium, neighbour Lasius, occasional Formica) | Foragers at baits and borders | Skirmish at a contested food source every few days. Low lethality (a few deaths per battle for Tetramorium); Formica rarer but more lethal. | Tetramorium caespitum (ritualised battles), L. niger and L. flavus (Pontin) | Low: behaviour verified, rates not found |
| 4. Parasitoid flies (Pseudacteon formicarum) | Foragers, especially those that sprayed formic acid or are fighting | Flies present on warm summer days. Each hovering fly reduces local foraging and parasitises a small number of workers, which die days to weeks later. | Lasius niger | Medium: host link secondary, rate unclear |
| 5. Pathogen (Metarhizium, Beauveria) | Individual workers and brood | Well under 1% of workers infected at any time. Hygiene contains it; never colony-wide. | Formica selysi, leaf-cutting ants, Lasius neglectus (lab) | Medium |
| 6. Nest digger (green woodpecker) | Upper chambers: workers and brood | Rare event, on the order of 0 to a few visits per colony per year. Removes a soil cone, kills or takes tens to hundreds of workers and brood near the surface. | Picus viridis on Lasius niger and L. flavus | Low: diet verified secondary, frequency not found |
| 7. Nuptial flight predators (birds, dragonflies) plus founding failure | Alates and newly mated queens | On flight day, most alates are eaten or fail. Fewer than about 1 to 5% of queens that land found a colony that survives a year. | Pogonomyrmex barbatus (hundreds of nests, 10 to 50 survive), L. niger lab founding (<20% survive 130 days in groups) | Medium for failure rate, low for the predation share |
| 8. Colony death (drought, crowding, queen death) | Whole colony | Annual death risk about 25% for small colonies (<500 workers), about 6% for mid-sized, about 2 to 3% for large. Queen death leads to a fade-out over about a year. | P. badius, P. barbatus, P. occidentalis | Medium: verified for Pogonomyrmex, not measured for L. niger |
| 9. Flood (non-lethal disaster) | Lower tunnels | Colony survives even 2 to 3 weeks of submersion. | Lasius niger | High (verified) |
| Optional: social parasite queen (Lasius umbratus) or slave raider (Formica sanguinea) | Queen or brood | Very rare. Omit by default. | L. umbratus, F. sanguinea | Low |

Suggested split of forager deaths (design assumption, no data): predation 60 to 70%, heat or desiccation 10 to 20%, ant fights 10 to 20%, getting lost under 5%.

---

## Gaps

1. **No field measurement of daily forager mortality for Lasius niger** (or any temperate Lasius). All rates come from desert or harvester ants. Surface activity of L. niger under cover of vegetation and at night may lower predation well below harvester ant values.
2. **No cause-of-death breakdown** (predation versus desiccation versus getting lost versus fights) for any ant species.
3. **No quantified fraction of Lasius alates eaten** by birds or dragonflies during nuptial flights. Popular sources describe heavy predation but give no numbers. The Pontin (1960) field experiments on L. niger colony founding were not read.
4. **No field data on annual mortality of established L. niger colonies** or on its causes. Pogonomyrmex data are used as a stand-in.
5. **No frequency data** for green woodpecker or badger attacks on individual nests.
6. **Phorid parasitism rate**: the "at least 50%" figure for Pseudacteon formicarum is unclear (per colony or per worker, seasonal peak or average). Original paper not read.
7. **Tetramorium versus Lasius niger** battle lethality is not quantified; the claim that pavement ants are most aggressive toward L. niger could not be traced to a source.
8. **Entrance closing** as a defence is supported only by hobbyist observations.
9. Several citations above carry author lists, volume or page numbers filled in from memory and are flagged **unverified**. Check them before quoting in public documentation.
10. Pages that could not be read (HTTP 403 or 429): AntWiki species pages, ResearchGate records, the Regnier & Wilson abstract, and the two L. niger bioRxiv preprints.

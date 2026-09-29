# Lasius niger: colony founding, development and life history

Research notes for a colony-simulation screensaver. Compiled 2026-09-29.

Confidence key:
- **verified**: I read the number in the source itself (full text, abstract, or publisher/indexer record).
- **verified (secondary)**: I read it in a verified paper that cites the original, but did not read the original.
- **partially verified**: paper located and confirmed to exist, but the specific number comes from a search-engine snippet of it, not text I read.
- **unverified**: hobbyist sources, encyclopedias without traceable citation, or search snippets without a locatable source. Use only as a placeholder.

Many primary sources were paywalled or blocked (Springer, ScienceDirect, AntWiki, ResearchGate). Where that happened it is noted.

---

## 1. Nuptial flights

### 1.1 Season and national synchrony (UK citizen-science data)
- (a) Across 2012 to 2014, UK flying-ant records: July 64.6%, August 32.6%, June 2.1%, September 0.7%. National peak days: 24 July 2012, 1 August 2013, 17 July 2014. Every year had at least 5 days of mass records, but flights at different sites were poorly synchronised (local peaks differed by weeks). Colonies can be "flight ready" by July and may release multiple batches of alates in a season.
- (b) Hart AG, Hesselberg T, Nesbit R, Goodenough AE (2018). The spatial distribution and environmental triggers of ant mating flights: using citizen-science data to reveal national patterns. *Ecography* 41(6): 877-888. doi:10.1111/ecog.03140. Post-print: https://eprints.glos.ac.uk/4780/
- (c) verified (full text read).
- (d) Simulation implication: schedule the flight window as a probability distribution over roughly 1 July to 31 August (peak late July), with several possible flight days per season rather than one fixed date.

### 1.2 Weather triggers
- (a) Flights observed only when temperature > 13 C and wind < 6.3 m/s (14 mph). Probability of flights rises linearly with temperature above 13 C; every day with a mean daily temperature > 25 C had flights somewhere in the UK. Flights were more likely when temperature was rising and wind falling relative to the previous day. Barometric pressure was not significant. Recent rainfall was discussed but the authors did not find it a key predictor in the UK's wet climate. Urban/sheltered colonies may fly earlier.
- (b) Hart et al. 2018, as above.
- (c) verified.
- (d) Simulation implication: flight trigger = (temp > 13 C) AND (wind < 6.3 m/s), with probability increasing with temperature and with a positive day-over-day temperature change; urban heat can shift timing earlier.

### 1.3 Older weather study (Netherlands)
- (a) Four European species including L. niger; flights before 20 August occurred at wind speeds < 1.7 m/s at 2 m height; after 20 August species flew at higher wind speeds. Larger-queened L. niger required higher air temperatures than small-queened species such as Myrmica rubra.
- (b) Boomsma JJ, Leusink A (1981). Weather conditions during nuptial flights of four European ant species. *Oecologia* 50: 236-241. doi:10.1007/BF00348045
- (c) verified (secondary): numbers read in Hart et al. 2018's literature review; original paywalled.
- (d) Simulation implication: early-season flights need calm air; tolerance of wind can increase late in the season as colonies "run out of time".

### 1.4 Time of day
- (a) Afternoon and early evening, activity peaking around 17:00; activity window roughly 10:00 to 20:00.
- (b) Hobbyist/aggregator sites (antflights.com, antkeepers.com) via search snippet. No peer-reviewed source located.
- (c) unverified.
- (d) Simulation implication: release alates in the afternoon (bias around 15:00 to 18:00 sim time) on trigger days.

### 1.5 Rainfall as a trigger (not L. niger)
- (a) A leaf-cutter study found flights only after at least 64 mm rain in the preceding month and > 26 C on days after rainfall.
- (b) Staab M, Kleineidam CJ (2014), cited in Hart et al. 2018. This is Atta vollenweideri, not Lasius.
- (c) verified (secondary), but wrong species.
- (d) Simulation implication: do not hard-code "after rain" for L. niger; at most use humidity/recent rain as a weak modifier.

### 1.6 Mating
- (a) Queens mostly mate once; effective queen mating frequency averages 1.16 (range 1.04 to 1.42 across populations); double mating in all populations, triple mating seen once. Multiply-mated colonies were not larger and did not produce more sexuals. No diploid males found.
- (b) Mating-frequency numbers: the "1.16" figure came from a search summary of a paper titled "Queen mating and paternity in the ant Lasius niger" (*Molecular Ecology*; authors and year not confirmed), which I could not open. The "no fitness benefit" finding: Fjerdingstad EJ, Gertsch PJ, Keller L (2003). The relationship between multiple mating by queens, within-colony genetic variability and fitness in the ant Lasius niger. *J. Evol. Biol.* 16(5): 844-853. doi:10.1046/j.1420-9101.2003.00589.x
- (c) Fjerdingstad et al. 2003 abstract: verified. Mating frequency 1.16 and sperm store of about 2.6 million: unverified (search snippet only, source not opened).
- (d) Simulation implication: model each queen as mated to 1 male (small chance of 2); no need to simulate sperm depletion within a 29-year horizon.

### 1.7 Flight scale and male fate
- (a) Thousands of sexuals from many colonies gather for a few hours; males die soon after mating (a day or two).
- (b) Gathering: Aron S, Deneubourg J-L (2020). Colony co-founding in ants is an active process by queens. *Scientific Reports* 10: 13539. doi:10.1038/s41598-020-70497-x (PMC7419493). Male death: Matte and Billen 2021 intro ("soon after mating, the males die"); "a day or two" from hobbyist sites.
- (c) verified (qualitative); "a day or two" unverified.
- (d) Simulation implication: males can be despawned within 1 to 2 sim-days after the flight.

### 1.8 Dealation
- (a) Wings are torn off shortly after the nuptial flight, then the queen searches for a site. Queens land "in an unknown environment" and the ground density of dealate queens is very high after a flight; queens avoid areas used by workers of established colonies, which clumps foundations.
- (b) Matte A, Billen J (2021). Flight muscle histolysis in Lasius niger queens. *Asian Myrmecology* 13: e013003. doi:10.20362/am.013003. Clumping/avoidance: Sommer K, Hölldobler B (1995). Colony founding by queen association and determinants of reduction in queen number in the ant Lasius niger. *Animal Behaviour* 50: 287-294. doi:10.1006/anbe.1995.0244
- (c) Matte and Billen: verified. Sommer and Hölldobler clumping: partially verified (search summary; original paywalled).
- (d) Simulation implication: after landing, queen drops wings within minutes to hours and wanders away from existing colony territories before digging.

---

## 2. Claustral founding

### 2.1 Mode
- (a) Most colonies are founded by a single claustral queen relying entirely on body reserves (gaster fat body, storage proteins, histolysed flight muscles) with no foraging. Queens have a much higher fat content than dependent-founding Formica rufa queens.
- (b) Szabó N, Nagy J, Tartally A (2023). Claustral colony founding is limited by body condition: experimental feeding increases brood size of Lasius niger queens. *Biol. J. Linn. Soc.* 142(4): 397-401. doi:10.1093/biolinnean/blad149. Also Brütsch T, Avril A, Chapuisat M (2017). No evidence for social immunity in co-founding queen associations. *Sci. Rep.* 7: 16262. doi:10.1038/s41598-017-16368-4
- (c) verified.
- (d) Simulation implication: founding queen has a finite energy budget; no food intake until first workers forage.

### 2.2 Flight muscle histolysis timeline (25 C, queens isolated, no food)
- (a) Day 0 intact; day 3 no visible change; day 6 fibres decompact; day 9 sarcomeres disorganised; day 12 Z-discs gone; day 15 sarcolytes form; day 18 nearly all muscle content released, adipocytes appear; day 21 to 24 adipocytes fill thorax. Almost all muscle content depleted within 24 days of the flight. The thorax becomes a permanent fat store used later to restart laying after hibernation.
- (b) Matte and Billen 2021 (Table 1).
- (c) verified (full text read).
- (d) Simulation implication: queen energy can be modelled as a reserve pool that is converted into larval food over the first ~3 weeks, timed to match larval growth.

### 2.3 Body mass loss
- (a) A successful founding queen loses around 50% of body weight before workers forage.
- (b) Wikipedia "Black garden ant" (no traceable primary citation for this sentence).
- (c) unverified.
- (d) Simulation implication: use 50% mass loss as a plausible placeholder; a queen hitting ~0 reserve dies.

### 2.4 Feeding during founding
- (a) 240 queens, 24.5 C, 49 days. Weekly protein (crickets) significantly increased pupae+workers; sugar-only weekly feeding slightly increased pupae but reduced workers; a single early feeding had little effect.
- (b) Szabó et al. 2023.
- (c) verified.
- (d) Simulation implication: founding output is reserve-limited; if the sim ever lets a founding queen get protein, first-brood size should increase.

### 2.5 Chamber depth and sealing
- (a) No quantitative depth found for the L. niger founding chamber. Queens use small burrows in open soil or under stones. Mature nests may extend up to about 2 m deep (hobbyist claim).
- (b) Search snippets (antkeepers.com, AntWiki summary) only.
- (c) unverified.
- (d) Simulation implication: pick a shallow chamber (a few cm) sealed immediately; treat depth as an art choice. See Gaps.

### 2.6 Founding-queen mortality
- (a) Lab, 22 C, 70% RH, hibernation 4 C from day 83 to day 221-225: mortality before day 35 was 4% to 16% by soil treatment; cumulative mortality to end of hibernation 34% to 62% (46% in sterile controls and untreated soil). 66% to 84% of queens produced workers before hibernation.
- (b) Tragust S, Brinker P, Rossel N, Otti O (2020). Balancing life history investment decisions in founding ant queens. *Front. Ecol. Evol.* 8: 76. doi:10.3389/fevo.2020.00076
- (c) verified.
- (d) Simulation implication: roughly half of founding queens should die before their first spring even in favourable (lab) conditions; field mortality is certainly higher (predation, desiccation) but I found no field number.

### 2.7 Overwintering founding queens
- (a) 280 founding queens from Turku, Finland, overwintered at +7 to +8 C or +2 C. Queen survival did not depend on overwintering temperature (but differed between years); queens at +2 C kept more body fat. In 1-year-old colonies, +2 C reduced worker survival. All surviving queens produced workers at the same rate.
- (b) Haatanen M-K, van Ooik T, Sorvari J (2015). Effects of overwintering temperature on the survival of the black garden ant (Lasius niger). *J. Therm. Biol.* 49-50: 112-118. doi:10.1016/j.jtherbio.2015.02.012
- (c) verified (abstract).
- (d) Simulation implication: winter is survivable for queens across 2 to 8 C; cold is harder on workers, so small colonies lose some workers over winter.

---

## 3. Brood development

### 3.1 First-brood timeline at 25 C
- (a) First eggs 3 days after flight; egg number rises until larvae hatch; first larvae hatch 15 to 18 days post-flight; after day 21 eggs decline faster than larvae increase (oophagy: eggs eaten as food); first cocoons spun day 21 to 24 (mean ~25 days per Kipyatkov and Lopatina 2015); first workers after almost 40 days.
- (b) Matte and Billen 2021; cites Kipyatkov VE, Lopatina EB (2015). Comparative study of thermal reaction norms for development in ants. *Entomological Science* 18: 174-192. doi:10.1111/ens.12098
- (c) verified (Matte and Billen full text); Kipyatkov and Lopatina original paywalled.
- (d) Simulation implication at 25 C: egg stage ~13 to 15 days, larva ~6 to 9 days to cocoon for first brood (first brood is rushed), pupa ~15 days; total ~40 days.

### 3.2 First workers at 21 to 24 C
- (a) Brütsch et al. 2017: first workers emerged day 43 to 51 (mean day 45); 85.5% of control nests produced workers; first-brood worker counts (within 87 days) 14.6 +/- 2.1 (1 queen), 18.6 +/- 2.8 (2 queens), 26.3 +/- 3.1 (4 queens). Teggers et al. 2021 (21 C, dark): max ~15 workers per single queen vs ~30 per 2-queen colony before hibernation.
- (b) Brütsch et al. 2017 (see 2.1). Teggers EM, Deegener F, Libbrecht R (2021). Fecundity determines the outcome of founding queen associations in ants. *Sci. Rep.* 11: 2986. doi:10.1038/s41598-021-82559-9 (PMC7862224).
- (c) verified. (Brütsch rearing temperature not captured.)
- (d) Simulation implication: first cohort = about 10 to 20 nanitics per single queen, emerging ~6 to 7 weeks after the flight.

### 3.3 Temperature dependence and latitude
- (a) Northern (St Petersburg) vs southern (Belgorod) populations: northern brood develops slower at 17 to 20 C but faster at 22 to 27 C, with a higher lower development threshold and lower sum of effective temperatures. At 17 C larvae entered diapause and did not pupate (only egg development measured). Lower temperatures induce larval diapause more readily in the north. Founders need warm conditions to produce workers in the same summer.
- (b) Kipyatkov VE, Lopatina EB, Imamgaliev AA, Shirokova LA (2004). Effect of temperature on rearing of the first brood by the founder females of the ant Lasius niger: latitude-dependent variability of the response norm. *J. Evol. Biochem. Physiol.* 40: 165-175. doi:10.1023/B:JOEY.0000033808.45455.75
- (c) partially verified (bibliographic record verified; qualitative findings from search summaries; numeric thresholds and degree-day sums NOT obtained, paywalled).
- (d) Simulation implication: use a degree-day model: development rate = k * (T - T0) for T > T0, with T0 around 12 to 14 C (placeholder, unverified) and larvae entering diapause below ~17 to 20 C late in the season.

### 3.4 Generic stage durations
- (a) Egg 10 to 20 days, larva 14 to 24 days, pupa 14 to 21 days; egg to adult 8 to 10 weeks in general (non-first-brood, room temperature). At 24 C, development 4 to 6 weeks (up to 10).
- (b) Wikipedia and hobbyist care sheets (search snippets).
- (c) unverified.
- (d) Simulation implication: use for later broods at cooler nest temperatures; first brood at 25 C is faster (see 3.1).

### 3.5 Callows
- (a) Newly eclosed workers are white and darken to black within about an hour. Larvae moult about 3 times.
- (b) Wikipedia (no primary citation).
- (c) unverified. (Kipyatkov/Lopatina classification cites 3 to 6 larval instars for Lasius-type ants, see 6.2.)
- (d) Simulation implication: render callows pale and fade to black; callows should not forage.

---

## 4. Nanitics (first workers)

### 4.1 Size
- (a) Workers from early-stage colonies: head width 1.08 +/- 0.07 mm; from intermediate-stage colonies: 1.15 +/- 0.06 mm (significantly larger).
- (b) Kramer BH, Schaible R, Scheuerlein A (2016). Worker lifespan is an adaptive trait during colony establishment in the long-lived ant Lasius niger. *Exp. Gerontol.* 85: 18-23. doi:10.1016/j.exger.2016.09.008
- (c) partially verified (abstract verified: early workers are smaller; exact head widths from search snippet).
- (d) Simulation implication: nanitics ~6% smaller in head width (roughly 15 to 20% smaller in mass, rough scaling); render slightly smaller.

### 4.2 Lifespan of nanitics vs later workers
- (a) Early-stage workers had lower mortality over the first 400 days. Life expectancy ~430 days (max 1129 days) for early workers vs ~310 days (max 1094 days) for later workers, lab conditions.
- (b) Kramer et al. 2016.
- (c) Qualitative result: verified (abstract). The 430/310/1129/1094 day figures: partially verified (snippet; PDF blocked).
- (d) Simulation implication: nanitics are durable (mean ~14 months in lab); later workers are cheaper and more expendable (mean ~10 months in lab). Field lifespan of foragers is surely shorter (not found).

### 4.3 Foraging behaviour in incipient colonies
- (a) As colonies grow, foraging shifts from individual exploitation to mass recruitment via trails. Colony size, not age, drives the shift. Minims modulate trail laying according to colony size. Colonies of 50 to 75 workers used 13 to 14 foragers; colonies of 75 to 200 used about 20. A threshold near 75 workers marks the switch to cooperative (recruitment) foraging and to regular-sized workers.
- (b) Mailleux A-C, Deneubourg J-L, Detrain C (2003). How does colony growth influence communication in ants? *Insectes Sociaux* 50: 24-31. doi:10.1007/s000400300004
- (c) partially verified (paper and citation verified; numbers from search summaries; full text paywalled).
- (d) Simulation implication: this is the key behavioural gate. Below ~75 workers, foragers search individually and lay little trail; above it, enable pheromone recruitment. Foragers ~10 to 25% of workers in small colonies.

### 4.4 Heat and young colonies
- (a) Young L. niger colonies: 10-minute exposures to 25 to 40 C had no effect on foraging; 1-hour exposure to 40 C delayed foraging and cut trip numbers; 2 weeks at 35 C:30 C (day:night) vs 25 C:20 C raised forager and colony mortality.
- (b) da Silva RC, Prost M, Bultelle A, Monnin T (2026). Effects of heat stress on the foraging activity of incipient ant colonies. *J. Anim. Ecol.* doi:10.1111/1365-2656.70337
- (c) verified (abstract).
- (d) Simulation implication: suppress foraging during prolonged hot spells (>35 to 40 C at ground level) and add mortality risk for small colonies.

### 4.5 Nanitic inexperience and timing of first foraging
- (a) No L. niger data found on how quickly nanitics begin foraging or how inexperienced they are. A general claim that minims are more efficient brood carers than larger workers appeared in a search snippet without a traceable source.
- (c) unverified.
- (d) Simulation implication: assume nanitics open the seal and begin foraging within days of eclosion (hobbyist observation); give them weaker navigation/learning to start.

---

## 5. Colony growth, reproduction, lifespans

### 5.1 Growth curve
- (a) Year 1 (end of first summer): 10 to 30 workers (see 3.2). Year 2: 200 to 500 workers by the end of the second season "if fed daily" (captive). Captive colonies 5,000+ within 3 to 4 years. Full-size workers after about 1 year.
- (b) Year 1: Brütsch 2017, Teggers 2021 (verified). Years 2 to 4: hobbyist sources (antkeepers/antshack, search snippet).
- (c) Year 1 verified; later years unverified.
- (d) Simulation implication: rough logistic curve: ~15 (Y1), ~100 to 500 (Y2), ~1,000 to 2,000 (Y3), ~5,000 (Y4 to 5), plateau ~5,000 to 10,000.

### 5.2 Mature size
- (a) Field nests (Poland) contain from 100 to more than 10,000 workers; mound nests 40 to 60 cm diameter. Other sources: typically 4,000 to 7,000, rarely up to 40,000.
- (b) Okrutniak M, Rom B, Turza F, Grześ IM (2020). Body size differences between foraging and intranidal workers of the monomorphic ant Lasius niger. *Insects* 11(7): 433. doi:10.3390/insects11070433 (verified, cites earlier literature). 4,000 to 7,000 and 40,000: Wikipedia (unverified).
- (c) verified (100 to >10,000); unverified (4,000 to 7,000; 40,000).
- (d) Simulation implication: cap around 10,000 workers (screensaver budget), with rare outliers.

### 5.3 Onset of alate production
- (a) About 10,000 workers are needed before producing reproductive females, so first reproductives about 3 years after founding. Colonies generally at least 2 to 3 years old before producing sexuals.
- (b) ScienceDirect Topics "Lasius" overview (search snippet; page blocked; underlying chapter unknown).
- (c) unverified.
- (d) Simulation implication: enable alate production at colony age >= 3 years AND worker count above a threshold (a few thousand); allow males earlier and at lower size than gynes (standard ant pattern, not verified for L. niger).

### 5.4 Sex allocation
- (a) Sex investment ratio biased toward females, matching the worker optimum.
- (b) Search snippet associated with Fjerdingstad and coworkers' L. niger work; the specific paper was not opened ("Control of body size of Lasius niger ant sexuals: worker interests, genes and environment", *Mol. Ecol.* 2005, doi:10.1111/j.1365-294X.2005.02648.x, title verified only).
- (c) unverified.
- (d) Simulation implication: allocate more biomass to gynes than males in the alate crop.

### 5.5 Queen lifespan
- (a) Record 28 years 9 months (28.75 years) in captivity (Hermann Appel's queen). Typical queen lifespan around 15 years is often quoted.
- (b) Kutter H, Stumper R (1969). Hermann Appel, ein leidgeadelter Entomologe (1892-1966). Proc. VI Congr. IUSSI, Bern: 275-279. Record confirmed via AnAge database (https://genomics.senescence.info/species/entry.php?species=Lasius_niger) and multiple secondary sources. "About 15 years typical": Wikipedia.
- (c) record: verified (secondary). "Typical 15 years": unverified. Kramer et al. 2016 abstract states queens reach 20 years and more (verified).
- (d) Simulation implication: queen can live the whole screensaver run; draw a lifespan from roughly 10 to 29 years; queen death ends a monogynous colony (workers may rear males only).

### 5.6 Worker lifespan
- (a) Lab: see 4.2 (~10 to 14 months mean, max ~3 years). Wikipedia cites unpublished lab data of at least 4 years.
- (c) see 4.2; the 4-year claim unverified.
- (d) Simulation implication: worker life expectancy ~1 year (lab); shorten for foragers exposed to field hazards.

### 5.7 Monogyny
- (a) Mature colonies are strictly monogynous.
- (b) Brütsch et al. 2017; Okrutniak et al. 2020.
- (c) verified.
- (d) Simulation implication: one queen per mature colony.

---

## 6. Seasonality

### 6.1 Hibernation in the lab
- (a) Founding colonies hibernated at 4 C for about 4.5 to 5 months (day 83 to day 221-225), then held at 15 C and raised to 22 C to restart brood.
- (b) Tragust et al. 2020.
- (c) verified.
- (d) Simulation implication: winter dormancy of ~4 to 5 months with near-zero activity; restart brood when nest temperature climbs above ~15 to 20 C.

### 6.2 Seasonal cycle and larval diapause
- (a) L. niger is a temperate species with obligatory winter diapause. Larvae (in any of 3 to 6 instars) can diapause; they are the overwintering brood stage. In summer colonies moved to ~5 C, eggs, prepupae and pupae died within 1 to 3 weeks but larvae survived. Strategy of prolonged brood rearing: overwintered larvae pupate in spring and first half of summer (producing alates), followed by rapid non-diapause worker broods, then a late brood that enters diapause for winter. Workers partly control larval diapause.
- (b) Chapter on ant seasonal cycles and diapause, IntechOpen (2018), https://www.intechopen.com/chapters/60505 , which cites Lopatina (2018) and Kipyatkov's work.
- (c) verified (secondary; read the chapter summary, did not read the underlying Kipyatkov papers).
- (d) Simulation implication: overwinter a cohort of larvae; in spring they become the alate crop and first workers; late-summer larvae stop developing and wait for spring. Eggs and pupae should not persist through winter.

### 6.3 Activity temperature thresholds
- (a) Flight threshold 13 C (1.2). CTmax about 47 C. Foraging unaffected by brief heat up to 40 C; prolonged 40 C impairs it (4.4). Founding queens and colonies tolerate 2 to 8 C overwintering (2.7).
- (b) CTmax 47 C: search snippet from a 2025 J. Therm. Biol. thermal-limits methods paper (https://www.sciencedirect.com/science/article/pii/S0306456525000841), not opened.
- (c) CTmax: unverified. Minimum foraging temperature for L. niger workers: not found.
- (d) Simulation implication: foraging off below ~8 to 10 C (placeholder) and above ~40 C; full activity 20 to 30 C.

### 6.4 Winter energy use
- (a) Queens restart laying after hibernation from thoracic fat reserves built after founding, before workers bring in spring food (Janet 1907, reported by Matte and Billen 2021).
- (c) verified (secondary).
- (d) Simulation implication: spring laying can start before first foraging; queen keeps a reserve buffer.

---

## 7. Pleometrosis

### 7.1 Frequency
- (a) In the field, 18% of founding nests contained more than one queen (Sommer and Hölldobler 1995); other summaries give 18 to 25%, with 2 to 5 unrelated queens per group.
- (b) Sommer and Hölldobler 1995 (via Brütsch 2017 and Aron and Deneubourg 2020, both verified).
- (c) verified (secondary).
- (d) Simulation implication: ~20% of founding sites have 2 to 5 queens, more likely where landing density is high.

### 7.2 Queens aggregate actively
- (a) Given two chambers, newly mated queens grouped in one chamber more often than random; larger groups more common than chance.
- (b) Aron and Deneubourg 2020.
- (c) verified.
- (d) Simulation implication: queens nearby should be attracted to each other's burrows (mutual attraction).

### 7.3 Benefits
- (a) Two-queen colonies produce first pupae and workers sooner (P = 0.01), ~30 vs ~15 max first-brood workers, and grow faster afterwards (slope 1.01 vs 0.59 workers per unit time). Benefit arises from nutritional boost to larvae. Four-queen nests reared ~26 workers vs ~15 for one queen. No social-immunity benefit against fungal pathogen.
- (b) Teggers et al. 2021; Brütsch et al. 2017.
- (c) verified.
- (d) Simulation implication: pooled queen reserves -> bigger, earlier first brood.

### 7.4 Reduction to one queen
- (a) After first workers emerge queens fight and only one survives. The most fecund queen survived 75% of the time (21/28) when size was controlled; size alone was not significant (63%, 17/27). Queens chosen as partners were less fecund. In one lab study of a "normal" population, under 20% of queens survived 130 days in pleometrotic groups and mortality was NOT centred on first-worker emergence; hyper-dense-population queens had 75% survival to 227 days.
- (b) Teggers et al. 2021; Aron and Deneubourg 2020; Stukalyuk S, Czaczkes T (2024). Queens from a unique hyper-dense Lasius niger population tolerate pleometrosis better than queens from a 'normal' population. bioRxiv doi:10.1101/2024.07.16.603683 (preprint).
- (c) verified (Stukalyuk and Czaczkes is a preprint).
- (d) Simulation implication: in multi-queen nests, after first workers appear, eliminate queens (by fights or worker execution) over weeks to months, with survival weighted by fecundity, until one remains.

---

## 8. Behaviour changes with colony age/size

- 8.1 Individual -> mass-recruitment foraging at ~75 workers (Mailleux et al. 2003; partially verified). Implication: gate trail recruitment on colony size.
- 8.2 Early colonies make small, long-lived workers; later colonies make larger, shorter-lived, "redundant" workers (Kramer et al. 2016; verified qualitatively). Implication: worker size and lifespan parameters should shift with colony size.
- 8.3 Workers in the nest are slightly larger than foragers attracted to tuna baits; L. niger is monomorphic but body size correlates weakly with task (Okrutniak et al. 2020; verified). Implication: small size variation with a weak bias of smaller workers toward foraging.
- 8.4 As colonies grow, overwintering chambers may extend deeper, so workers face less cold than founding queens (Haatanen et al. 2015, author speculation; verified as statement). Implication: larger colonies suffer less winter worker mortality.
- 8.5 Alate production begins only in large, older colonies (5.3; unverified).

---

## 9. Substitute species where L. niger data are thin

- **Lasius japonicus** (sister species, East Asia; formerly treated as L. niger): good studies of thoracic crop formation coordinated with flight muscle histolysis (weeks 2 to 5) (bioRxiv doi:10.1101/2022.03.03.482739, seen in search only), geographic variation of temperature effects on initial colony development (Kuroki I, Tagawa J, Nakamura K 2019, *Appl. Entomol. Zool.*, doi:10.1007/s13355-019-00610-8, record verified, abstract restricted), and temperature effects on queen oviposition and seasonal colony development (title seen only). Best substitute for degree-day and oviposition parameters.
- **Lasius flavus** and **Lasius platythorax**: congeners in the same habitats; L. flavus has nest microclimate data (Int. J. Biometeorol. 2016, doi:10.1007/s00484-016-1275-z, seen in search only).
- **Solenopsis invicta**: the classic nanitic/incipient colony literature (Porter and Tschinkel 1986, cited in Matte and Billen) for how first-worker number drives colony survival and for incipient growth curves.
- **Kipyatkov and Lopatina 2015** (33 species, 97 regression lines) is the best source for generic Lasius thermal constants if the paywall can be passed.

---

## Gaps

1. **Development thresholds and degree-day sums**: Kipyatkov et al. 2004 and Kipyatkov and Lopatina 2015 hold these for L. niger, but both were paywalled. Only the 25 C timeline (Matte and Billen) is solid.
2. **Founding chamber depth, sealing behaviour and time from landing to seal**: no data found.
3. **Field mortality of founding queens** (predation by birds and ants, desiccation): only lab mortality (~34 to 62% by first spring) found.
4. **Colony growth in years 2 to 5 and age at first alates**: only hobbyist/unsourced numbers. No field time-series of worker counts located.
5. **Worker activity temperature thresholds** (minimum foraging temperature, daily activity curves) for L. niger: not found. CTmax 47 C unverified.
6. **Nanitic behaviour**: timing of first foraging after eclosion, learning/inexperience, and task allocation in incipient colonies not found for L. niger.
7. **Queen egg-laying rates** in mature colonies and their seasonal course: not found for L. niger (L. japonicus likely substitute).
8. **Field worker lifespan**: only lab values (~310 to 430 days mean).
9. **Time of day of flights and mating details** (in-air vs ground mating, male numbers): only hobbyist sources.
10. Several numbers flagged "partially verified" (Kramer head widths and life expectancies, Mailleux forager counts, mating frequency 1.16) should be checked against full texts if accessible.

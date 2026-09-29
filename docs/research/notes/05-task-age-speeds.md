# 05. Task age (polyethism), movement speeds, and digging rates

Scope: parameters that earlier research could not find for Lasius niger. Where L. niger data do not exist, the closest well-studied species is named explicitly.

Confidence tags:
- verified: read in the paper full text or in the published abstract.
- secondary: taken from a citing paper, a search-engine summary of the abstract, or a number derived by me from verified raw values.
- unverified: seen only in a summary with unclear origin; do not rely on it without checking.

---

## TOPIC A: Age polyethism timing

### A1. Lasius niger: qualitative age polyethism, and young workers can forage when the colony lacks old ones
- (a) Fact: In lab colonies founded by a queen and allowed to grow for about a month, all workers were 0 to 1 month old, yet a subset responded to sugar water and was classed as foragers ("young foragers"). The same colonies, rerun 11 months later, still separated into nest workers and foragers aged 11 to 12 months. Protein and metabolite profiles differed primarily by task (subcaste) and only secondarily by age. The introduction restates the general pattern: youngest workers perform in-nest tasks (feeding queen, nest building, brood care), oldest perform outside tasks, and demographic manipulation can reverse this.
- (b) Quque M, Brun C, Villette C, Sueur C, Criscuolo F, Heintz D, Bertile F. 2023. Both age and social environment shape the phenotype of ant workers. Scientific Reports 13. DOI 10.1038/s41598-022-26515-1 (PMC9814961).
- (c) verified (full text).
- (d) Simulation implication: task should be driven by an age-biased probability plus colony need, not a hard age gate; in a young founding colony, workers under 30 days old must be allowed to forage.

### A2. Lasius niger: worker lifespan in the lab
- (a) Lab-reared L. niger workers born in early-stage colonies were smaller and had lower mortality over the first 400 days than workers from later-stage colonies. A search summary of the same work reports lab worker lifespans of roughly 309 to 434 days depending on colony size and mean longevities around 108 to 123 days for size classes in another dataset (numbers not checked in full text).
- (b) Kramer BH, Schaible R, Scheuerlein A. 2016. Worker lifespan is an adaptive trait during colony establishment in the long-lived ant Lasius niger. Experimental Gerontology. DOI 10.1016/j.exger.2016.09.008.
- (c) verified (qualitative abstract claims); secondary (the specific day counts).
- (d) Simulation implication: in-nest (protected) L. niger workers can live about a year; field lifespan is set by forager mortality, not senescence.

### A3. Camponotus fellah (Formicinae): transition is stochastic, not a fixed age
- (a) Tracking of more than 500 individually tagged workers for more than 100 days: workers sit in one of two steady states (nurse, forager). Transition age varied widely and the transition probability was age independent. Once started, the transition followed a stereotyped S-shaped trajectory, and ants began foraging only after completing it.
- (b) Richardson TO, Kay T, Braunschweig R, Journeau OA, Ruegg M, McGregor S, De Los Rios P, Keller L. 2021. Ant behavioral maturation is mediated by a stochastic transition between two fundamental states. Current Biology 31: 2253 to 2260. DOI 10.1016/j.cub.2020.05.038.
- (c) verified (abstract).
- (d) Simulation implication: model nurse to forager as a per-day hazard (constant or weakly age dependent) followed by a short transition phase of a few days, rather than a deterministic age threshold.

### A4. Camponotus fellah: three spatial groups (nurse, cleaner, forager) ordered by age
- (a) Six colonies tracked for 41 days (more than 9 million interactions) formed three groups: nurses, cleaners (nest maintenance), foragers. Workers move from one group to the next as they age. A search summary reports mean ages of about 93.8 days (nurses), 124.2 days (cleaners), 159.4 days (foragers) in these lab colonies.
- (b) Mersch DP, Crespi A, Keller L. 2013. Tracking individuals shows spatial fidelity is a key regulator of ant social organization. Science 340: 1090 to 1093. DOI 10.1126/science.1234316.
- (c) verified (three groups, age ordered); secondary (mean ages; long-lived lab colony values, likely inflated compared to field).
- (d) Simulation implication: use three sequential roles, nurse then maintenance/cleaner then forager, each with its own spatial zone (brood chambers, mid-nest, entrance).

### A5. Camponotus fellah: youngest workers do most of the digging; digging lags population growth
- (a) 2D sand nests (0.8 cm thick). Young cohorts were 40 plus or minus 16 days old, old cohorts 172 plus or minus 20 days. The target excavated area per ant decreases linearly with age; young ants dig most, old ants contribute little except after nest collapse. Colony-level excavation follows population growth by 15 plus or minus 12 days. Founding queen dug 23.8 cm2 alone and stopped digging when the first workers eclosed; the very first workers did not dig immediately.
- (b) Rajendran H, Weinberger R, Fonio E, Feinerman O. 2024/2026. Colony demographics shape nest construction in Camponotus fellah ants. eLife 13: RP100706. DOI 10.7554/eLife.100706 (PMC13102390).
- (c) verified (full text).
- (d) Simulation implication: digging should be done mostly by young and intermediate workers (the "maintenance" band), triggered when space per ant falls below target, with about a two-week lag after brood eclosion bursts.

### A6. Pogonomyrmex badius: age at first foraging depends on season of birth; forager loss is not replaced
- (a) Field colonies (Florida). Summer-born workers darkened quickly and became foragers at 43 days of age; autumn-born workers took 200 or more days. Foragers were lost at about 3% per day in late summer. Removing 50% of foragers did not pull workers from other castes into foraging (at the expense of larval survival). Foragers sampled March to July survived an average of 38 days (SD 10.1) in the forager population; forager lifespan in nature about 3 weeks versus many months in the lab. Foragers occupy chambers within the top 12 cm of nests up to 145 cm deep.
- (b) Kwapich CL, Tschinkel WR. 2013. Demography, demand, death, and the seasonal allocation of labor in the Florida harvester ant (Pogonomyrmex badius). Behavioral Ecology and Sociobiology 67: 2011 to 2027. DOI 10.1007/s00265-013-1611-9. PDF: https://www.bio.fsu.edu/~tschink/publications/Kwapich%20and%20Tschinkel%20Pogonomyrmex%202013.pdf
- (c) verified (full text).
- (d) Simulation implication: a daily forager loss of about 3% is a solid default; workers born before winter should stay inside much longer (overwintering cohort forages first next spring).

### A7. Solenopsis invicta: nurse to forager at about 30 to 50 days; reserves; reversible
- (a) Fire ant workers start adult life as nurses on the brood pile and transition to foraging at about 30 to 50 days of age; an intermediate "reserve" group stores and relays food and may nurse or forage. Starkey and Tamborindeguy (2023) showed both task acceleration (young to forager) and task reversion (forager back to nurse) driven by brood presence, strongest with larvae, and workers tended to keep the new role.
- (b) Mirenda JT, Vinson SB. 1981. Division of labour and specification of castes in the red imported fire ant Solenopsis invicta Buren. Animal Behaviour. https://www.sciencedirect.com/science/article/abs/pii/S0003347281801005 (30 to 50 day figure as cited in later molecular papers). Starkey J, Tamborindeguy C. 2023. Family before work: task reversion in workers of the red imported fire ant, Solenopsis invicta, in the presence of brood. Scientific Reports 13. DOI 10.1038/s41598-023-29246-z.
- (c) secondary (30 to 50 days, from citing paper); verified (reversion and acceleration, abstract).
- (d) Simulation implication: allow reversion: if brood per nurse is high and nurses are few, foragers can switch back with some probability per day.

### A8. Pheidole dentata minors: foraging onset 14 to 20 days; repertoire expansion
- (a) Minor workers show a six-fold increase in repertoire during the first 3 weeks. Newly eclosed to 2 to 3 day old minors do brood care and queen attendance; mature minors (more than 20 days) keep brood care in their repertoire while adding nest maintenance, foraging, and defense. About 52% switch to foraging at 14 to 20 days.
- (b) Seid MA, Traniello JFA. 2006. Behavioral Ecology and Sociobiology (repertoire expansion in Pheidole dentata; title and pages not checked). Also Muscedere ML, Willey TA, Traniello JFA. 2009. Age and task efficiency in the ant Pheidole dentata: young minor workers are not specialist nurses. Animal Behaviour. https://www.sciencedirect.com/science/article/abs/pii/S000334720900013X
- (c) secondary (search summaries of these papers).
- (d) Simulation implication: older workers should keep a nonzero probability of nursing ("repertoire expansion"), which gives graceful recovery when nurses die.

### A9. Formica (wood ants): foragers mostly older than about 30 days
- (a) A search summary attributes to classic wood ant work (Otto 1958, Formica rufa group) that about 97% of foragers were more than 30 days past eclosion, while workers under 30 days handled incoming material in the nest.
- (b) Otto D. 1958. Uber die Arbeitsteilung im Staate von Formica rufa rufo-pratensis minor (title as cited in search results; not read; origin of the 97% figure not confirmed).
- (c) unverified.
- (d) Simulation implication: consistent with a Formicinae foraging onset near 30 days; use only as a sanity check.

### A10. Temnothorax: inactivity, reserve labor, and callows foraging when needed
- (a) Temnothorax rugatulus: colonies replaced removed highly active workers (mainly foragers and brood carers) within 1 week, mostly from previously inactive workers; removed inactive workers were not replaced within 2 weeks. A companion field and lab study reports a mean colony-wide inactive fraction of about 0.61 of time. In Temnothorax albipennis, callows can be seen foraging when demand is high (search summary).
- (b) Charbonneau D, Sasaki T, Dornhaus A. 2017. Who needs "lazy" workers? Inactive workers act as a "reserve" labor force replacing active workers, but inactive workers are not replaced when they are removed. PLoS ONE 12: e0184074. DOI 10.1371/journal.pone.0184074. Charbonneau D, Hillis N, Dornhaus A. 2015. "Lazy" in nature: ant colony time budgets show high "inactivity" in the field as well as in the lab. Insectes Sociaux. DOI 10.1007/s00040-014-0370-6.
- (c) verified (2017 abstract); secondary (0.61 inactive fraction; albipennis callows).
- (d) Simulation implication: at any moment about half of workers should be idle; idle workers are the pool that refills forager losses within about a week.

### A11. What "nursing" consists of
- (a) Nursing behaviors counted in fire ant studies: antennation, mandibular prodding, carrying, and feeding of brood (Starkey and Tamborindeguy 2023, methods). Young workers tend the queen and brood deep in the nest (Richardson et al. 2021). Pharaoh ant (Monomorium pharaonis) nurses specialize on larval instar: 56% showed significant instar specialization short term, 22 to 27% over ten days; nurse to larva food transfer is by trophallaxis. In Camponotus mus (Formicinae), nurses break open cocoons and help callows eclose; prey shortage delays cocoon opening. Formicines feed larvae by regurgitation (trophallaxis).
- (b) Starkey and Tamborindeguy 2023 (above). Walsh JT, Warner MR, Kase A, Cushing BJ, Linksvayer TA. 2017. Ant nurse workers exhibit behavioral and transcriptomic signatures of specialization on larval stage. bioRxiv. DOI 10.1101/218834. Roces F, Protomastro JJ. 1988. Prey availability and eclosion-help of callow workers in the formicine ant Camponotus mus. Oecologia. DOI 10.1007/BF00378048.
- (c) verified (abstracts and methods).
- (d) Simulation implication: nurse actions = feed larva (trophallaxis), groom/antennate brood, carry brood between chambers (temperature/humidity), attend queen, open cocoons at eclosion. No numeric time budget per action was found.

### A12. Callow (newly eclosed) workers
- (a) Atta vollenweideri callows have bite forces more than an order of magnitude below mature foragers of the same size; mature foragers have a sixfold larger mandible closer muscle and a stiffer, thicker head cuticle. Callows cannot cut leaves and stay inside the nest in their first days. Cuticle darkens as it sclerotizes. In P. badius, cuticle color classes (callow, midcolored, dark) track age and nest depth, and no callow-colored workers remained after 2 months post-winter.
- (b) Puffel F, Meyer L, Imirzian N, Roces F, Johnston R, Labonte D. 2023. Developmental biomechanics and age polyethism in leaf-cutter ants. Proceedings of the Royal Society B 290. DOI 10.1098/rspb.2023.0355. Kwapich and Tschinkel 2013 (above).
- (c) verified (abstract, full text).
- (d) Simulation implication: render callows paler for their first days to weeks; limit them to low-force tasks (brood tending, staying on brood pile), no heavy digging or prey carrying.

### A13. Forager life expectancy and daily mortality in the field
- (a)
  - Cataglyphis bicolor (Tunisia): forager half-life 4.2 days, life expectancy 6.1 days after marking, constant daily loss 16.4%.
  - Pogonomyrmex owyheei: foragers and defenders average life expectancy about 14 days (implies about 7% per day).
  - Pogonomyrmex barbatus and P. rugosus: some marked exterior workers survived up to about 30 days; longevity ranked nest maintenance > foraging > patrolling.
  - Pogonomyrmex badius: about 3% of foragers lost per day; 38 days mean survival in forager population (March to July cohort).
- (b) Schmid-Hempel P, Schmid-Hempel R. 1984. Life duration and turnover of foragers in the ant Cataglyphis bicolor (Hymenoptera, Formicidae). Insectes Sociaux. DOI 10.1007/BF02223652. Porter SD, Jorgensen CD. 1981. Foragers of the harvester ant, Pogonomyrmex owyheei: a disposable caste? Behavioral Ecology and Sociobiology 9: 247 to 256. DOI 10.1007/BF00299879. Gordon DM, Holldobler B. 1987. Worker longevity in harvester ants (Pogonomyrmex). Psyche 94. PDF: https://web.stanford.edu/~dmgordon/previous/Gordon1987Longevity.pdf. Kwapich and Tschinkel 2013 (above).
- (c) Cataglyphis: secondary (abstract via search summary). P. owyheei: secondary (as cited in Gordon and Holldobler 1987, which I read). P. barbatus: verified. P. badius: verified.
- (d) Simulation implication: forager daily mortality 3 to 7% for a temperate, trail-following species; 16% is an extreme hot-desert solitary forager.

---

## TOPIC B: Movement and digging rates

### B1. Lasius niger walking speed on a bridge (25 C)
- (a) At 25 plus or minus 1 C, L. niger foragers crossed the bridge section between the two bottlenecks without interactions in 2.96 plus or minus 0.61 s (10 mm wide bridge) and 2.93 plus or minus 0.56 s (3 mm wide), N = 50; at high traffic (120 ants/min) contact-free crossings took 2.46 to 2.49 s. Bridge layout: access ramp 95 mm, bottleneck 15 mm and entrance 15 mm at each end, central part 60 mm. Each head-on contact cost 0.43 s (3 mm bridge) to 0.48 s (10 mm bridge). Crop load did not change speed: outbound empty ants and nestbound loaded ants were not significantly different. Derived speed: 60 mm / 2.96 s = about 20 mm/s if the timed section is the central part; about 30 mm/s if it also includes both 15 mm entrances (90 mm). Speed increases with trail pheromone concentration (stated qualitatively).
- (b) Dussutour A, Deneubourg JL, Fourcassie V. 2005. Temporal organization of bi-directional traffic in the ant Lasius niger (L.). Journal of Experimental Biology 208(15): 2903 onward. DOI 10.1242/jeb.01711.
- (c) verified (raw times, layout, load result); secondary (the mm/s conversion, which depends on which section was timed).
- (d) Simulation implication: L. niger trail speed about 20 mm/s (2 cm/s) at 25 C; liquid-laden returning foragers move at the same speed; each head-on encounter costs about 0.45 s.

### B2. Lasius niger speed used in models
- (a) A trail formation model explicitly parametrized for ants uses ant speed c = 2 cm/s (plus pheromone lifetime 100 s, detection radius 1 cm). This is a model parameter, not a measurement, but it matches B1.
- (b) Boissard E, Degond P, Motsch S. 2011. Trail formation based on directed pheromone deposition. arXiv:1108.3495. https://arxiv.org/abs/1108.3495
- (c) secondary.
- (d) Simulation implication: 2 cm/s is the accepted modeling value for L. niger.

### B3. Temperature dependence of running speed across 24 ant species
- (a) Running speed rises with temperature following a Boltzmann-Arrhenius relation; fitted activation energies ranged 0.15 to 0.93 eV, mean 0.47 eV. In many species speed levels off above some critical temperature. Speed scales with body mass to the power 0.14 to 0.34 (central tendency about 0.25). Formicinae included (Formica rufa, Formica fusca, Cataglyphis bicolor). Army ants were fastest at a standardized 5 mg; Formica rufa and Acromyrmex versicolor were among the slowest. Converting E = 0.47 eV to a Q10 over 20 to 30 C gives about 1.85 (my calculation: exp(E/k x (1/293.15 minus 1/303.15))). The metabolic theory expectation of 0.65 eV gives Q10 about 2.3.
- (b) Hurlbert AH, Ballantyne F, Powell S. 2008. Shaking a leg and hot to trot: the effects of body size and temperature on running speed in ants. Ecological Entomology 33: 144 to 154. DOI 10.1111/j.1365-2311.2007.00962.x. PDF: https://labs.bio.unc.edu/hurlbert/pubs/Hurlbert,%20Ballantyne%20and%20Powell%202008.pdf
- (c) verified (full text for E values and allometry); secondary (Q10 conversion). Per-species speeds at 28 C could not be reliably read from the extracted table.
- (d) Simulation implication: speed(T) = speed(25 C) x exp(-E/k x (1/T minus 1/298.15)) with E about 0.47 eV, capped by a plateau and a decline near the thermal maximum.

### B4. Walking speed vs temperature in Prenolepis imparis (Formicinae, close to Lasius) and Linepithema humile
- (a) Field trail speeds over a 10 cm mark. Winter ant P. imparis measured at 3.6 to 14.6 C: linear slope 0.033 cm/s per C, Q10 = 2.05 (6.4 to 14.4 C). Argentine ant: slope 0.077 cm/s per C, Q10 = 4.07. Regression lines crossed at 7.14 C (below that the winter ant was faster). CTmax similar between species; winter ant CTmin lower.
- (b) Nelson RA, MacArthur-Waltz DJ, Gordon DM. 2023. Critical thermal limits and temperature-dependent walking speed may mediate coexistence between the native winter ant (Prenolepis imparis) and the invasive Argentine ant (Linepithema humile). Journal of Thermal Biology 111: 103392. DOI 10.1016/j.jtherbio.2022.103392.
- (c) verified (full text).
- (d) Simulation implication: for a cool-temperate formicine like L. niger, Q10 about 2 is well supported; a linear slope of 0.03 to 0.08 cm/s per C is a simpler alternative.

### B5. Speed inside tunnels (Solenopsis invicta)
- (a) Fire ants (body length L = 3.5 plus or minus 0.5 mm) climbed vertical glass tunnels at 2.0 plus or minus 0.8 L/s ascending (n = 1621) and 2.3 plus or minus 0.7 L/s descending (n = 990). In their own tunnels under alarm they descended at 6.9 plus or minus 2.1 L/s and ascended at 4.1 plus or minus 1.8 L/s, exceeding 9 L/s at times. Speed dropped sharply only when D/L fell below about 0.5 (head width is 0.24 L). Falls were always arrested by antenna and leg bracing when D was below 1.31 L.
- (b) Gravish N, Monaenkova D, Goodisman MAD, Goldman DI. 2013. Climbing, falling, and jamming during ant locomotion in confined environments. PNAS 110. DOI 10.1073/pnas.1302428110.
- (c) verified (full text).
- (d) Simulation implication: vertical tunnel travel about 2 body lengths/s normally (about 8 mm/s for a 4 mm L. niger), so underground speed is about 0.4x surface trail speed; alarm speeds up to about 2x that.

### B6. Absolute excavation rates (Solenopsis invicta)
- (a) 3D containers with 100 workers: excavation rates within the first 12 h ranged 0.05 to 0.3 cm3/h (0.5 to 3 mm3 per ant per hour). 2D cells with 150 ants (Gravish et al. 2012): about 0.4 cm3/h (about 2.7 mm3 per ant per hour). Nest growth rate increased with substrate coarseness and moisture; excavation fastest in the first epoch. Pellets: two modes (pellet formation in fine, wet grains; pulling single particles in coarse grains). Mean carried pellet area 0.43 plus or minus 0.03 mm2 (clay-like substrate), nearly independent of moisture, grain size, and head size, although some pellets were up to 7 times the mean; about 1/3 of large loads were trimmed by about 20% during transport.
- (b) Monaenkova D, Gravish N, Rodriguez G, Kutner R, Goodisman MAD, Goldman DI. 2015. Behavioral and mechanical determinants of collective subsurface nest excavation. Journal of Experimental Biology 218: 1295 to 1305. DOI 10.1242/jeb.113795.
- (c) verified (full text).
- (d) Simulation implication: a digging L. niger-sized worker removes about 1 to 3 mm3 per hour on average; pellet size is fixed (about 0.4 to 0.5 mm2 cross section, about 0.1 to 0.3 mm3) rather than soil dependent.

### B7. Tunnel diameter relative to body size
- (a) S. invicta tunnels have diameter comparable to body length: D = 1.06 plus or minus 0.23 L in incipient nests, 3 to 5 mm absolute, independent of substrate. L. niger (mean body length 4.1 plus or minus 0.14 mm) stop piling pellets on pillars at about their body length (minimum deposition height 4.02 mm, maximum 8.46 mm) and then build a roof; lab pillars were spaced 9.93 plus or minus 0.66 mm apart, comparable to chamber and gallery widths in field nests.
- (b) Gravish et al. 2013 (above); Monaenkova et al. 2015 (above); Khuong A, Gautrais J, Perna A, Sbai C, Combe M, Kuntz P, Jost C, Theraulaz G. 2016. Stigmergic construction and topochemical information shape ant nest architecture. PNAS 113: 1303 to 1308. DOI 10.1073/pnas.1509829113 (PMC4747701).
- (c) verified (full text).
- (d) Simulation implication: L. niger tunnels about 4 to 5 mm wide (1.0 to 1.3 body lengths); chamber ceilings about 4 to 8 mm; pillar/chamber spacing about 10 mm.

### B8. Lasius niger pellet handling rates during building
- (a) 500 L. niger workers built pillars over 96 h (density about 0.3 pillars/cm2 after 48 h, plateau around 72 h). Measured rates: ant contact rate with a pile 0.22 per s; fraction of contacting ants carrying a pellet 0.32; spontaneous pellet drop rate for a carrying ant 0.025 per s (mean carry time about 40 s before dropping anywhere); drop rate increases by 0.11 per s per pellet already on the pile; pick-up rate 0.029 per s on a single-pellet pile, decreasing on larger piles. Workers add a building pheromone to pellets with an estimated lifetime of 800 to 1200 s (about 13 to 20 min).
- (b) Khuong et al. 2016 (above).
- (c) verified (full text).
- (d) Simulation implication: use these as direct per-second probabilities for pellet pick-up and drop, with a 15 to 20 minute decaying "fresh soil" pheromone that attracts further deposition.

### B9. Lasius niger nest volume regulation and digging rate vs group size
- (a) Final nest volume and digging rate both grow almost proportionally with the number of workers; enlarging the population triggers new digging that readjusts volume; digging acts as negative feedback. A citing summary reports maximal digging rate scaling with group size with exponent 0.73 and gives a per-ant maximal rate in cm3 per ant per day, but I could not read the actual values.
- (b) Rasse P, Deneubourg JL. 2001. Dynamics of nest excavation and nest size regulation of Lasius niger (Hymenoptera: Formicidae). Journal of Insect Behavior. DOI 10.1023/A:1011163804217.
- (c) secondary (abstract via search summary; exponent 0.73 via citing summary).
- (d) Simulation implication: dig while (nest volume / worker count) is below a target; slow down as it approaches the target.

### B10. Messor sancta and Camponotus fellah: saturating (logistic) excavation, volume per ant
- (a) Messor sancta groups in 2D sand dug with logistic dynamics: exponential start, saturation after about 3 days, total volume almost proportional to worker number (Buhl et al. 2004; Buhl et al. 2005). Gautrais et al. 2014 used group sizes of 50, 100, 200 over 72 h; with 200 ants, total tunnel length about 4000 mm (about 20 mm of tunnel per ant, my derivation) with tunnel width about 2 mm. C. fellah (7.8 to 17.2 mm body length): colonies regulate about 11 cm2 of 2D nest per ant (0.8 cm thick, so about 8.8 cm3 per ant); maximal per-ant digging rate constant r = 2.2 plus or minus 0.43 cm2 per ant per day (about 1.8 cm3 per ant per day).
- (b) Buhl J, Gautrais J, Deneubourg JL, Theraulaz G. 2004. Nest excavation in ants: group size effects on the size and structure of tunneling networks. Naturwissenschaften. DOI 10.1007/s00114-004-0577-x. Buhl J, Deneubourg JL, Grimal A, Theraulaz G. 2005. Self-organized digging activity in ant colonies. Behavioral Ecology and Sociobiology. DOI 10.1007/s00265-004-0906-2. Gautrais J, Buhl C, Valverde S, Kuntz P, Theraulaz G. 2014. The role of colony size on tunnel branching morphogenesis in ant nests. PLoS ONE 9: e109436. DOI 10.1371/journal.pone.0109436. Rajendran et al. (A5).
- (c) Messor: secondary (search summaries), Gautrais group sizes and totals: secondary (fetched page, derivation mine). C. fellah: verified.
- (d) Simulation implication: scaling C. fellah volumes by body volume ((4 mm / 12 mm)^3, about 1/27) gives about 0.3 cm3 of nest per L. niger worker and about 2.7 mm3 per ant per hour peak digging, which agrees with the fire ant value in B6.

---

## Recommended values

| Parameter | Value | Basis species | Confidence |
|---|---|---|---|
| Callow phase (pale, on brood pile, no foraging or heavy digging) | first 3 to 7 days | Atta vollenweideri, Pheidole dentata, P. badius color classes | low (qualitative, no L. niger data) |
| Nurse phase typical end (median) | about 30 days (range 14 to 60) | P. dentata 14 to 20 d, S. invicta 30 to 50 d, P. badius 43 d (summer), Formica about 30 d | medium for range, low for L. niger |
| Nurse to forager model | daily hazard about 1/30 per day after callow phase, weakly age dependent, plus 2 to 4 day transition | Camponotus fellah (stochastic transition) | medium |
| Intermediate role (maintenance, digging, cleaning) | between nurse and forager; young and intermediate workers do most digging | C. fellah | medium |
| Overwintering or late-season cohort | foraging delayed until next spring (200+ days) | P. badius | medium |
| Task acceleration when foragers scarce | young (under 30 d) workers may forage in small or young colonies | L. niger (Quque 2023), S. invicta | verified qualitatively |
| Task reversion when brood demand high | foragers may return to nursing | S. invicta, P. dentata | medium |
| Active worker loss replacement time | about 7 days, drawn from idle workers | Temnothorax rugatulus | medium |
| Idle fraction of workers at any moment | about 0.4 to 0.6 | Temnothorax rugatulus | medium |
| Forager daily mortality (field, temperate) | 0.03 to 0.05 per day (default 0.03) | P. badius 3%/d, P. owyheei about 7%/d | medium |
| Forager life expectancy after first foraging | about 20 to 35 days (default 30) | P. badius 38 d, P. owyheei 14 d | medium |
| In-nest worker lifespan (protected) | about 300 to 400 days | L. niger (lab) | medium |
| Surface trail walking speed at 25 C | 20 mm/s (range 20 to 30) | L. niger | medium (derived from verified times) |
| Laden vs unladen speed (liquid crop load) | no difference (factor 1.0) | L. niger | verified |
| Laden speed with solid item or soil pellet | factor about 0.8 (placeholder) | none found | unverified |
| Head-on encounter cost | 0.45 s per contact | L. niger | verified |
| Temperature scaling of speed | Arrhenius E about 0.47 eV (Q10 about 1.9 to 2.0), plateau above about 30 C | 24 species; Prenolepis imparis Q10 2.05 | medium |
| Tunnel (vertical) travel speed | about 2 body lengths/s (about 8 mm/s), alarm up to about 4 to 7 L/s | S. invicta | medium |
| Tunnel diameter | 1.0 to 1.3 body lengths (4 to 5 mm for L. niger) | S. invicta; L. niger body 4.1 mm | medium |
| Chamber ceiling height | about 4 to 8 mm | L. niger (pellet deposition height) | medium |
| Pillar/chamber spacing | about 10 mm | L. niger | verified (lab) |
| Per-ant digging rate while digging | about 1 to 3 mm3/h (default 2.5) | S. invicta; scaled C. fellah | medium |
| Pellet size | about 0.4 to 0.5 mm2 cross section (about 0.2 mm3) | S. invicta | medium |
| Pellet carry: spontaneous drop rate | 0.025 per s (mean about 40 s carry) | L. niger | verified |
| Pellet drop boost per pellet already on pile | +0.11 per s per pellet | L. niger | verified |
| Pellet pick-up rate from small pile | 0.029 per s | L. niger | verified |
| Building pheromone lifetime | 800 to 1200 s | L. niger | verified (model fit) |
| Target nest volume per worker | about 0.3 cm3 (scaled) | C. fellah scaled to L. niger size | low |
| Digging lag after population increase | about 15 days (plus or minus 12) | C. fellah | verified |
| Digging dynamics | logistic, stops as volume per ant reaches target | L. niger, Messor sancta | medium |

---

## Gaps

- No Lasius niger study giving the age in days at nurse to forager transition, or age at first digging. The 30 day default is an interpolation from Formicinae (Camponotus, Formica) and Myrmicinae (Pheidole, Solenopsis, Pogonomyrmex).
- No numeric per-task time budget for nurses (fraction of time feeding vs grooming vs carrying brood) in any Lasius; only qualitative lists and Temnothorax inactivity fractions.
- No field forager mortality for Lasius or any temperate Formicinae; values come from harvester ants and Cataglyphis.
- Rasse and Deneubourg 2001 contains the actual L. niger digging rates and volume per ant, but the full text was paywalled; only the scaling exponent (0.73) and qualitative results are available here.
- No L. niger speed vs temperature curve, upper thermal plateau, or CTmax used for slowing; the plateau temperature is a guess.
- No data on speed while carrying solid loads (prey, soil pellets) for Lasius; the 0.8 factor is a placeholder.
- Pellet mass, pellets per hour per ant, and pellet size for L. niger in natural soil were not found; Khuong et al. give only probabilities in plaster/clay setups.
- Minter et al. 2012 (Lasius flavus 4D CT) and Toffin et al. 2009 (Messor sancta shape transition) were located but their absolute rates could not be read.
- Per-species running speeds at 28 C in Hurlbert et al. 2008 Table 2 could not be read reliably from the PDF text layer (includes Formica rufa); worth checking the table by eye.

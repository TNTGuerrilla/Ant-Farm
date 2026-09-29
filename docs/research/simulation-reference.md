# Lasius niger simulation reference

This is the design-facing summary of the literature research for the ant colony screensaver. It turns the four detailed research files into the rules and parameters the simulation should use.

Detailed notes, with full citations and per-finding confidence, are in `notes/`:

| File | Covers |
|---|---|
| `notes/01-founding-lifecycle.md` | Nuptial flights, claustral founding, brood development, nanitics, growth, lifespans, seasons |
| `notes/02-learning-navigation.md` | Individual learning, memory, pheromone vs memory, path integration, aversive learning, senses |
| `notes/03-nest-threats.md` | Nest building, brood thermoregulation, rain and flooding, predators, hygiene, aphids |
| `notes/04-foraging-colony.md` | Pheromone deposition and decay, recruitment, task allocation, heritable colony variation, validated models |
| `notes/05-task-age-speeds.md` | Callow and nurse phases, nurse-to-forager transition, forager mortality, walking speed, digging rates |
| `notes/06-predators-mortality.md` | Threat archetypes, forager and colony mortality rates, defenses |
| `notes/07-nest-reuse-limits.md` | Reuse of abandoned nests, limits to colony size, limits to nest depth |
| `notes/08-foraging-range.md` | Foraging distances, territory, aphid placement, neighbor spacing, world dimensions |

Confidence tags used below:

- **V**: verified. The number was read in the paper or its abstract.
- **S**: secondary. The number is from a citing paper or a search summary. It is probably right, but check it before relying on it.
- **G**: informed guess. It is borrowed from another species or from hobbyist sources, or it was derived by us.

Species is Lasius niger unless noted.

---

## 1. Design decisions the research settles

1. **Species: Lasius niger.** It is the best-studied ant for individual learning and trail foraging, it is a soil nester with a clear founding story, and it is the most common beginner species in antkeeping.

2. **Evolution happens between colonies.** Workers are sterile. Selection acts on colonies, which reproduce by producing alates (new queens and males) that found daughter colonies. In the best field data (harvester ants, Pogonomyrmex barbatus), only about 25% of colonies ever reproduce (V). What gets inherited is how a colony *responds to conditions*, not its average activity level (V, but see caution in section 9).

3. **Hybrid learning is Baldwinian.** The genome encodes starting weights plus plasticity rules. Weights adapt during an ant's life. Offspring inherit the genome only, never learned weights.

4. **Three layers of memory:**
   - **Genetic (evolution):** innate responses such as trail following, flood response and brood care.
   - **Individual (plasticity):** food locations, food odours, routes, places to avoid.
   - **Stigmergic (environment):** pheromone trails, home-range scent, nest architecture.

   Colony "experience" such as flood-safe chambers lives mostly in layer 3. Layer 3 is also free to persist, because it is part of the saved world.

5. **Some behavior must be innate, not learnable.** Ants can learn to *ignore* a pheromone trail but never to *avoid* one (Wenig 2021, V). Plasticity may reduce pheromone gain to zero but must never invert it.

6. **Floods are a setback, not a catastrophe.** Lasius niger is exceptionally flood tolerant: it was present in 85% of plots after 4 to 24 days of summer flooding (Hertzog 2016, V). Survival relies on air pockets trapped in the nest. Mass drowning would be unrealistic.

---

## 2. Life cycle and colony growth

| Parameter | Value | Conf. | Source |
|---|---|---|---|
| Nuptial flight season | July (65%), August (33%); peak late July | V | Hart 2018 |
| Flight trigger | Temp > 13 C AND wind < 6.3 m/s; more likely on warming days | V | Hart 2018 |
| Flight time of day | Afternoon, peak around 17:00 | G | Hobbyist |
| Queen mating | Usually 1 male (effective 1.16) | S | Mol. Ecol. |
| Males die | Within about 1 to 2 days | G | Hobbyist |
| Founding mode | Claustral: sealed chamber, no food, lives on reserves | V | Szabo 2023 |
| Flight muscle breakdown | Nearly complete by day 24 (fuels first brood) | V | Matte & Billen 2021 |
| First eggs | Day 3 after flight (25 C) | V | Matte & Billen 2021 |
| First larvae | Day 15 to 18 | V | Matte & Billen 2021 |
| First cocoons | Day 21 to 25 | V | Matte & Billen 2021 |
| First workers | About day 40 at 25 C; day 43 to 51 at 21 to 24 C | V | Matte & Billen 2021; Brutsch 2017 |
| First brood size | About 15 nanitics (1 queen), about 30 (2 queens) | V | Teggers 2021 |
| Founding queen mortality | 34 to 62% dead by first spring (lab). Field incipient mortality up to 95% | V / S | Tragust 2020; Brutsch 2017 |
| Pleometrosis | About 18 to 25% of founding nests have 2 to 5 queens; one survives (most fecund wins 75%) | V / S | Sommer & Holldobler 1995; Teggers 2021 |
| Nanitic size | About 6% smaller head width | S | Kramer 2016 |
| Worker lifespan (lab) | Nanitics about 430 days, later workers about 310 days (max about 3 years) | S | Kramer 2016 |
| Forager lifespan | Much shorter (fastest-ageing subcaste); no field number | G | Kramer 2016 |
| Growth curve | Y1 about 15; Y2 100 to 500; Y3 1,000 to 2,000; Y4 to 5 about 5,000; plateau 5,000 to 10,000 | Y1 V, rest G | Hobbyist |
| Mature size | 100 to more than 10,000 workers | V | Okrutniak 2020 |
| First alates | At about 3 years old and several thousand workers | G | Unsourced |
| Queen lifespan | Commonly 15 to 20+ years; record 28.75 years | S | Kutter & Stumper 1969; Kramer 2016 |
| Mature colony | Strictly one queen | V | Brutsch 2017 |
| Winter | 4 to 5 months dormant; larvae are the overwintering stage | V / S | Tragust 2020; Kipyatkov |
| Spring | Overwintered larvae become the alate crop and first spring workers | S | Kipyatkov |

**Visible milestone:** at about **75 workers**, foraging switches from individual searching to trail recruitment (Mailleux 2003, S). Colony *size*, not age, drives this. Below that, trails barely form. That is realistic, not a bug.

---

## 3. Individual learning (plasticity targets)

| Behavior | Target | Conf. | Source |
|---|---|---|---|
| Place learning | 75% correct after 1 rewarded visit, 95% after 3 | S | Gruter 2011 |
| Odour-value learning | 1 trial; about 2x faster than place | S | Oberhauser 2019 |
| Odour memory capacity | 2 to 3 odour-value pairs at once | S | Czaczkes & Kumar 2020 |
| Primacy | First-learned odour preferred about 85% | S | Oberhauser 2022 |
| Single-trial memory | Consolidates in 15 to 30 min; gone by 24 h | V | Mathada 2025 |
| Repeated-trial memory | Days (48 h to 3 days) | V / S | Piqueret 2019 (Formica); Wagner 2023 (Linepithema) |
| Extinction | About 6 unrewarded trials | V | Piqueret 2019 (Formica) |
| Aversive odour learning | 1 trial | V | Wenig 2021 |
| Aversive place learning | About 5 trials (punishment was water immersion) | V | Mathada 2025 |
| Pheromone following | Innate; learnable down to 0, never negative | V | Wenig 2021 |
| Memory vs trail conflict | Memory wins after 1 visit (82 to 100%) in daylight | S | Gruter 2011 |
| Naive ants on trail | Follow at 62 to 70% | S | Gruter 2011 |
| Light | Low light raises pheromone reliance and deposition; darkness blocks route learning | S | Jones 2019 |
| Passive exposure | Being carried or confined on a route teaches nothing; learning needs the ant's own choice | S | Czaczkes 2016 |
| Social odour learning | Food odour received by trophallaxis can cue recall of a location | S | Czaczkes 2014 |
| Learning walks | 3 to 7 short looping walks over about 2 days before foraging | V | Fleischmann 2016 (Cataglyphis) |
| Visual nest memory | Consolidates over days, then lasts for life | S | Narendra 2007 (Melophorus) |

**Plasticity design this implies:**

- Two memory timescales. *Fast weights* form in one trial and decay within about 24 h. *Slow weights* form after about 3 or 4 reinforced repeats and last for days.
- Learning rate decays with experience, which produces the primacy effect.
- Update only on actions the ant chose (eligibility traces or policy gradient style), never on forced movement.
- Aversive learning rate is much lower for places than for odours.
- The pheromone channel gain is clamped to be non-negative.

**Rain and flood learning:** no study shows an individual ant learning to avoid floods, rain or predators. The closest evidence is Lasius niger learning to avoid a place after about 5 water immersions, and Formica selysi workers keeping consistent raft roles across repeated floods (Avril 2016, S). Flood response should be mostly innate and colony-level, with slow individual avoidance of wet places layered on top.

---

## 4. Senses (network inputs)

Suggested input vector. The individual items are grounded in the research; the combination is our derivation.

- Left and right pheromone concentration, sampled about 1 cm ahead. The turn is roughly proportional to (L - R) / (L + R) (Perna 2012, V, Argentine ant).
- Food-odour identity (2 to 3 channels).
- Home-range scent density. This is a slow, colony-specific footprint field laid by every walking ant (Lenoir 2009, V).
- Nest CO2 gradient (left and right), gated by home-vector magnitude (Cataglyphis, S).
- Path-integration home vector, with error growing with distance travelled (Cataglyphis, S).
- Coarse visual panorama (4 to 8 sectors), multiplied by light level. There is no vision underground.
- Light level, temperature, and humidity or wetness.
- Contact with a nestmate, carrying that ant's odour and state but **no direction** (Lasius has no tactile direction code, S).
- Own crop fullness, a "recent contacts" (crowding) count, and an age or experience counter.

---

## 5. Foraging and pheromones

| Parameter | Value | Conf. | Source |
|---|---|---|---|
| Walking speed | About 2 cm/s (range 1 to 6). No primary L. niger value | G | Perna 2012 (Argentine ant) |
| Search walk | Straight runs with exponential lengths plus turns; slower and more tortuous heading out, straighter heading home | V | Bonavita 2026 |
| Weak-trail decay | 0.4% per second (about 45 min to detection threshold) | V | Gruter 2012 model |
| Strong-trail persistence | Behavioral effect up to 20 to 24 h after 500 passages | V | Evison 2008 |
| Pheromone decay vs heat | Faster when hot | S | van Oudenhove 2011 (Tapinoma) |
| Fork accuracy | 74 to 83% per junction | S | Czaczkes 2017 |
| Deposition | About 1.5 to 3 marks per 5 cm on the return trip | S | Czaczkes 2013 |
| Deposition near food | Up to 22x more within 10 cm of food | S | Czaczkes 2024 |
| Deposition vs distance | Up to 4x more for food at 100 cm than at 20 cm | S | Czaczkes 2024 |
| Saturation | High existing pheromone suppresses more deposition | V | Czaczkes 2013 |
| Crowding | Up to 5.6x less deposition when crowded | V | Czaczkes 2013 |
| Recruit trigger | Lay trail only if the ant filled its own individual "desired volume" | V | Mailleux 2000 |
| Non-recruiters | About 14% of foragers never lay trail | V | Mailleux 2005 |
| Trips | Feed about 60 s, return about 40 s, unload about 60 s (lab scale) | V | Gruter 2012 |
| Collective choice | Colonies pick the richer source, but lock in if a poor trail is established first | V | Beckers 1990 |
| Switching | With crowding, colonies move to a better source in about 10 min | V | Gruter 2012 |
| Trophallaxis | Duration exponential (constant stop rate); a few ants do most exchanges | V | Buffin 2010; Planckaert 2019 |
| Foragers | About 10 to 25% of workers | V / G | Planckaert 2019 |
| Idle workers | About 40 to 60% at any time (a reserve labor pool) | V | Charbonneau 2017 (Temnothorax) |
| Negative pheromone | Not documented in L. niger. Leave it off, or make it an evolvable trait | V | Robinson 2005 (Monomorium) |

**Ready-made validation target:** Gruter et al. 2012 (PLoS ONE) publish a full NetLogo model validated against Lasius niger colonies. If the neural ants reproduce its symmetry-breaking and switching results, the foraging layer is realistic.

---

## 6. Task allocation

- Age biases task (young ants work inside, old ants outside), but role can be decoupled from age (Quque 2023, V). Treat age as a soft bias.
- Switching into foraging is easy; switching back is rare (Gordon 1989, V, Pogonomyrmex).
- Task can emerge from *where* an ant spends time (Mersch 2013, V, Camponotus).
- The response-threshold motif (engage with probability s^2 / (s^2 + theta^2)) is a proven baseline. Individual thresholds map to bias weights (Bonabeau 1996, S).
- Rate of successful returners is a good nest-side input for "go out and forage" (Gordon lab, V).
- **Gap:** the age at which Lasius niger workers switch from nursing to foraging is not documented. The 2 to 4 week figure comes from other species (G).

---

## 7. Nest building and environment

| Topic | Rule | Conf. | Source |
|---|---|---|---|
| Building | Drop soil pellets where fresh building pheromone is (lifetime 13 to 20 min). Walls stop at about 1 body length (4 mm), then roofs start | V | Khuong 2016 (PNAS) |
| Chambers | About 5.5 to 6 mm tall, about 12 mm wide; about 30% of nest volume is void | V | Khuong 2016 |
| Nest size | Target volume scales with worker count; dig when crowded, stop when space is sufficient | S | Rasse & Deneubourg 2001 |
| Excavation episodes | Logistic: fast start, then taper | S | Buhl 2005 (Messor) |
| Shape | Top-heavy; grows by adding shafts with chambers budding off; chamber spacing widens with depth | V | Tschinkel 2004 (Pogonomyrmex) |
| Depth | Most galleries within about 30 cm, a few shafts to about 70 cm, plus an above-ground mound | G | Unconfirmed |
| Heat | Hot surface means deeper nests, and upper chambers may be back-filled | V | Garcia Ibarra 2024 |
| Temperature speed | Digging and walking about 2 to 3x faster at 25 C than 15 C, same shapes | S | Rathery 2025 (L. flavus) |
| Brood placement | Carry brood toward the ideal temperature: up (or under warm stones) by day, down at night and in midday heat | V / G | Penick & Tschinkel 2008 (fire ant) |
| Brood target temp | About 3 C warmer by day than by night | S | Roces 1989 (Camponotus) |
| Light rain | Stop foraging, recall surface ants, build up or plug the entrance | S / V | Farji-Brener 2018; Kolay 2015 (Diacamma) |
| Heavy flooding | Move brood and queen up or into air pockets; relocate only if flooding persists | V | Hertzog 2016; Kolay 2015 |
| Rafting | Rare, surface only, for Lasius niger | V | Hertzog 2016 |
| Cold | Colony clusters deep; near-freezing temperatures cause slow worker losses; queens are hardier | V | Haatanen 2015 |
| Drought | Move deeper toward moist soil; reduce surface activity | G | |
| Toilets | 1 to 4 faecal spots, 83% in chamber corners | V | Czaczkes 2015 |
| Refuse | Corpses and food waste carried to a pile *outside* the nest | V | Czaczkes 2015 |
| Corpse removal | Delay of about 1 h to 1 day; if blocked, move to a peripheral chamber | S | Diez 2014; Choe 2009 |
| Disease | Colony segregates foragers from nurses and queen; digs more spread-out, tree-like tunnels | V | Stroeymeyt 2018; Science 2025 |
| Grooming | Nestmates groom exposed ants; this builds social immunity | S | Ugelvig 2007 (L. neglectus) |
| Aphids | Honeydew is the main sugar source. When sugar is plentiful, ants eat 8x more aphids | V | Offenberg 2001 |

**Threats and events worth modelling:**

- Brood raids between neighboring young colonies; the larger colony wins (S).
- Surface fights with pavement ants (Tetramorium caespitum) or neighboring Lasius niger colonies (S).
- Green woodpeckers digging into mounds; they feed mainly on Lasius (S).
- Rare late-game social parasitism by Lasius umbratus: a foreign queen takes over and the colony gradually turns yellow (S).

---

## 8. Time compression

Real timescales span seconds (a fork decision) to decades (queen lifespan). The research suggests **two clocks**:

- **Behavior clock (near real time).** Movement, foraging trips, trophallaxis, pheromone decay, building pheromone. These look wrong if sped up. A 3-minute foraging cycle and 45-minute trail decay should feel real to someone watching.
- **Calendar clock (compressed).** Brood development, aging, lifespans, seasons, nuptial flights, colony growth.

A starting point to tune: **1 calendar day = 10 real minutes.**

| Event | Calendar time | Screensaver time at 10 min/day |
|---|---|---|
| Founding to first workers | About 40 days | About 7 hours |
| Worker lifespan | About 1 year | About 60 hours |
| Colony to first alates | About 3 years | About 180 hours (weeks of evening use) |
| Winter | 4 to 5 months | About 20 to 25 hours, so fast-forward it with a short "overwintering" sequence |

**Decided:** memory decay runs on the calendar clock, so memory stays in proportion to lifespan, seasons and food availability.

---

## 9. Cautions and gaps

**Cautions:**

- Heritable colony-level variation has been shown only in harvester ants (Gordon 2013). A 2017 addendum to that paper was not read; check it before citing the heritability result.
- The often-quoted "47 min" Lasius niger pheromone lifetime traces back to Beckers et al., which could not be read. The Gruter 2012 model decay and the Evison 2008 persistence data are the usable values.
- Some review co-authors have had other papers retracted (J.N. Pruitt). Do not use Pruitt-led primary data.

**Gaps where we must use informed guesses:**

1. Degree-day thresholds for brood development (Kipyatkov papers are paywalled). Lasius japonicus is the best stand-in species.
2. Founding chamber depth, and the depth of a mature Lasius niger field nest.
3. Queen egg-laying rate (no Lasius niger eggs-per-day figure).
4. Daily brood movement, rain response and entrance plugging in Lasius niger specifically.
5. Minimum foraging temperature (a placeholder of about 8 to 10 C).
6. Heritable colony behavior in Lasius niger (borrowed from Pogonomyrmex, Temnothorax and Solenopsis).
7. Woodpecker attack frequency, the share of queens eaten after flights, and the causes of forager death.
8. What incoming ants actually do with an old nest's leftover brood and corpses (inferred from how Lasius niger treats corpses in general).

Colony growth, nest depth, nurse-to-forager age, speeds and predation were gaps in the first round. Sections 10 to 12 cover them.

---

## 10. Task timing and movement (gap follow-up)

| Parameter | Value | Conf. | Basis |
|---|---|---|---|
| Callow phase | First 3 to 7 days: pale, on the brood pile, no heavy work | G | Several species |
| Nurse to forager | Daily switch chance of about 1/30 after the callow phase (median about 30 days, range 14 to 60), with a 2 to 4 day transition | S | Camponotus fellah; others |
| Middle role | Young and middle-aged workers do most digging, cleaning and maintenance | S | Camponotus fellah |
| Flexibility | Young workers forage early in small colonies; foragers can revert to nursing when brood demand is high | V / S | Lasius niger (Quque 2023); Solenopsis |
| Replacement | Idle workers replace lost active workers within about 7 days | S | Temnothorax |
| Forager mortality | About 3% per day (life expectancy about 30 days after foraging starts) | S | Pogonomyrmex |
| In-nest lifespan | About 300 to 400 days | S | Lasius niger (lab) |
| Walking speed | About 20 mm/s at 25 C (range 20 to 30) | S | Lasius niger (derived from crossing times) |
| Liquid load | No slowdown | V | Lasius niger |
| Solid load or soil pellet | About 0.8x speed | G | Placeholder |
| Head-on encounter | Costs about 0.45 s | V | Lasius niger |
| Temperature | Q10 about 2; plateau above about 30 C | S | 24 species |
| Vertical tunnel travel | About 2 body lengths/s (8 mm/s) | S | Solenopsis |
| Tunnel width | 1.0 to 1.3 body lengths (4 to 5 mm) | S | Solenopsis; Lasius niger body size |
| Digging | About 1 to 3 mm3 per ant per hour while digging; pellet about 0.2 mm3 | S | Solenopsis; Camponotus |
| Pellet rules | Drop 0.025/s, +0.11/s per pellet already on pile, pick-up 0.029/s | V | Lasius niger (Khuong 2016) |
| Dig lag | Colonies take about 15 days to start digging after a population jump | V | Camponotus fellah |

## 11. Threats and mortality (gap follow-up)

Threats are modelled as functional archetypes rather than individual species.

| Archetype | Targets | Rate | Conf. |
|---|---|---|---|
| Background surface hazard | Foragers only | About 3 to 6% of active foragers per active day | S (Pogonomyrmex, Cataglyphis) |
| Sit-and-wait predator (spider, antlion) | Foragers on a trail | About 1 kill per predator per day. The colony pauses foraging on that side for 1 to 5 days after losing about 25% of foragers | S / G |
| Rival ants (Tetramorium, neighbor Lasius) | Foragers at food and borders | A skirmish every few days; few deaths | G |
| Phorid fly (Pseudacteon formicarum) | Foragers, drawn to formic acid | Warm summer days; parasitized workers die days to weeks later | S (Lasius niger host) |
| Fungal pathogen | Individual workers | Under 1% infected; grooming contains it | S |
| Green woodpecker | Top chambers | Rare, 0 to a few per year; tens to hundreds of workers and brood lost | G |
| Nuptial flight losses | Alates, new queens | Fewer than about 1 to 5% of landed queens found a colony that survives a year | S |
| Colony death | Whole colony | About 25% per year under 500 workers, about 6% mid-sized, 2 to 3% large. Queen death leads to a fade-out over about a year | S (Pogonomyrmex) |
| Flood | Lower tunnels | Survivable, even 2 to 3 weeks submerged | V |

A forager-death split of about 60 to 70% predation, 10 to 20% heat, 10 to 20% fights and under 5% lost is a design assumption; no data exists for it. Earlier notes cite 9 to 10 weeks of flood survival from a secondary source. Use the more conservative 2 to 3 weeks.

## 12. Nest reuse and natural limits (gap follow-up)

**Nest reuse is real.** 20% of harvester-ant colony moves went onto nests abandoned at least a year earlier (V). About 25% of abandoned Lasius flavus mounds were reoccupied within 8 years (S). Empty nests in sand fill in over about a year (S). Lasius niger eats prey corpses, carries dead nestmates to the refuse pile and bites and ejects foreign corpses. Starved ants eat corpses and brood (S).

**Colony size limits itself** (no hard cap needed):

- Brood production per worker falls as colonies grow; total brood output scales at about N^0.5 to N^0.8 (S).
- Worker deaths scale linearly while births scale sublinearly, so colonies converge to an equilibrium size.
- Neighbors split foraging territory and suppress growth and queen production (S).
- Above a size threshold, about a third of each year's output goes to alates, and the colony shrinks after the flight (S, Solenopsis).
- Mature colonies kill founding queens inside their territory, so new colonies appear in gaps and on abandoned nests (S).

**Depth limits itself:**

- Digging is triggered by crowding and stops when there is enough volume per ant (S).
- Hauling soil costs more the deeper the nest, so deep digging slows naturally (S).
- Brood seeks a target temperature: hot summers push it deeper, and winter pulls the colony to its deepest chambers (V).
- Maximum depth scales weakly with size (about N^0.38, from Pogonomyrmex). Tune it so a 5,000 to 10,000 worker nest is dense in the top 30 cm with a few shafts to about 70 cm (S).
- The water table is a hard floor. CO2 does not steer Lasius digging, so keep any CO2 effect weak (S).

## 13. Foraging range and world dimensions (gap follow-up)

No primary field measurement of Lasius niger foraging distance exists, so these values are an informed synthesis (mostly S and G). The detail is in `notes/08-foraging-range.md`.

| Parameter | Value | Conf. |
|---|---|---|
| Surface strip | About 8 m with the nest in the middle (6 m minimum; 12 m if a tree is included) | G |
| Trail lengths | Typical 0.5 to 2 m (median about 1 m), long 3 to 5 m, rare maximum 8 to 10 m. Draw from a right-skewed distribution | S / G |
| Aphid plants | 2 to 4 plants 0.3 to 3 m from the nest, with aphids 0.5 to 1.2 m up the stem | S / G |
| Aphid shelter | The closest plant (within about 1 m) gets a soil pavilion and a covered runway | S |
| Root aphids | 1 to 3 patches in the top 5 to 15 cm of soil | S |
| Optional tree | 3 to 6 m out, with a trail 2 to 5 m up the trunk | G |
| Neighbor colonies | One at each strip edge, 3 to 6 m away (1 to 2.5 m at dense sites) | S |
| Trip time | 3 to 8 min round trip at 2 cm/s; 15 to 20 min to a tree | Derived |
| Territory | About 36 m2 per colony | S (Elmes 1971, cited only) |

Satellite nests (polydomy) are contested for Lasius niger, so leave them out of the proof of concept.

# Six dimension implementation

The latest conversation values take precedence over the older Google Docs. World generation is seeded and intended for settlement production and trade. Existing generated chunks are not rewritten; use a fresh test world to see complete terrain changes.

## Bounds and terrain

Maximum Y below is inclusive. The dimension IDs use the `aoemod` namespace.

| Dimension | Min Y | Max Y | Sea level | Main terrain and underground |
|---|---:|---:|---:|---|
| `tropical_dimension` | 0 | 255 | 64 | Rainforest, mangrove, cherry and coral reef; moss over mud; no caves or mineral ores |
| `savanna_dimension` | 0 | 255 | 64 | Green, rainy savanna; grass/rooted dirt/dirt over moss; no caves or mineral ores |
| `plains_dimension` | -64 | 319 | 64 | Grass and three dirt layers over stone, dry mining tunnels; no natural lava or deepslate |
| `snowy_dimension` | -96 | 319 | 96 | Spruce/poplar forest, deep open lakes, mushroom galleries at -32 through -1, deepslate transition below -32 |
| `desert_dimension` | -32 | 319 | 64 | Sand, red sand and layered badlands selected by elevation, sparse oases, Deep Dark at -32 through -1 |
| `nether_dimension` | 0 | 127 | 12 | Four 32-block biome bands, connected galleries and ramps, additional local lava pools in upper bands |

Tropical inland targets are approximately 55% rainforest, 25% mangrove and 20% cherry. A three-seed density/biome sample measured 55.18%, 24.74% and 20.08%, with reefs occupying 29.20% of the whole dimension. These are sampling results, not quotas imposed on every map. Rivers and lake banks blend continuously into surrounding terrain. Snow's large basins bottom out at Y16, leaving solid rock above the mushroom caves. Only shallow shore material uses packed/blue ice; central fishing water remains open.

The Nether bands, from bottom to top, are basalt deltas 0–31, crimson forest 32–63, warped forest 64–95 and soul sand valley 96–127. Typical dry gallery floors are around Y18, 38, 70 and 102. Supported ramps connect the bands, with dry corridors and banks separating them from upper lava pools. Natural decoration preserves a narrow central passage and its supporting floor: basalt columns, tree trunks, magma, fire and thorny bushes cannot obstruct it. Harmless flora can remain, and player building is unrestricted. Upper pools use fluid thresholds 39, 71 and 103; sufficiently deep pools support the documented five-block-clearance large lava fish. Soul sand/soil and basalt/blackstone use three-dimensional noise. Nether floor bedrock remains; its ceiling and the desert floor are diggable.

Immersive Portals joins Desert Y=-32 to Nether Y=128 at the same X/Z, with scale 1. The paired build range is 480 blocks, including sky. A desert settlement near Y70 is roughly 212 vertical blocks above a basalt gallery at Nether Y18. Physical stairs, ramps and safe work routes still need to be built.

## Ores and materials

| Region | Mining role |
|---|---|
| Tropical / Savanna | No mineral ores |
| Plains | Coal, iron, copper and some gold; stone remains usable at every depth |
| Desert | Coal, copper, abundant gold and exclusive redstone; no iron, diamond, emerald or lapis |
| Snow | Rich coal/iron, copper, more gold than Plains, and exclusive diamond/emerald/lapis; no redstone |
| All Nether bands | Gold, quartz and ancient debris; basalt has three times the placement attempts of each upper band |

Snow's precious-ore concentration peaks near Y=-64, with secondary higher deposits. Mushroom caves have extra coal. Desert redstone concentrates around Y=-16, and its large gold population extends into the highlands. Nether ore targets preserve each host material, and every placed ore block checks the biome immediately above it. Sulfur deposits must touch lava; basalt also has underground glowstone veins, while ceiling glowstone remains available throughout the Nether.

Exact vein sizes, attempts, height ranges and exposure rules are data-driven under `src/main/resources/data/aoemod/worldgen/{configured_feature,placed_feature}/dimension_ores/`. Material blobs are under `dimension_blobs/`.

## Placement, growth and wildlife

The six `tags/blocks/dimension_sources/` files are the authored block-origin lists. `DocumentedPlacementRules` applies the stricter biome rules; crafted products inherit availability through the existing material/recipe rules. Tropical content can move freely between tropical biomes. Snow beetroots and wild beetroots belong to mushroom caves. Nether roots, nylium, fungi, stems, wart blocks and crops retain their biome restrictions.

A tree must start in its native biome. Its generated trunk/leaves/other canopy blocks may extend at most 16 blocks into the adjacent biome above, within the same dimension. Horizontal biome boundaries do not gain this allowance. Growth validates the whole generated tree before committing, so rejected growth does not leave partial trees. Soul-valley trees cannot cross the desert seam.

Natural fauna lists and fluid-clearance requirements are defined in `DocumentedFauna`. Spawn eggs and commands retain their normal use. Small fish need fluid above and below; large fish need two fluid blocks above and below; bottom dwellers need solid ground beneath and two fluid blocks above. Snow shallow/deep fauna use 24 blocks of water above as the initial dividing point.

Original `aoemod:swordfish` and `aoemod:arthropleura` implementations are included. Arthropleura is a recognizable segmented herbivore inspired by the requested Fossils and Archeology version, with original model geometry and behavior. Missing named seafood harvests, cooking recipes and a custom plum are registered through `DimensionFoods`. See `DEPENDENCY_DIMENSIONS.md` for source-mod IDs, versions and the unavailable Angling sea-snail/starfish names.

Adult cold cows act as yaks: wheat tames them, one chest adds 27 slots, an empty hand opens the owner's pack, and sneaking with an empty hand toggles follow/stay. Cargo persists across saves, has a Forge item-handler capability and drops on death after resource-drop replacement.

## Initial economy tuning

These are initial design estimates, not results of a long-running economy simulation. Existing configured behavior is retained unless a documented addition requires a new entry. The central tuning file is `config/DocumentedContentBalance.java`.

| Added crop group | Regional growth setting | Mature harvest where overridden |
|---|---:|---|
| Tropical pineapple, corn, cotton, beans | 1.00 | Cotton: 3 cotton + 2 seeds; complex crops retain native harvest logic |
| Tropical mulberry | 0.55 | Native harvesting |
| Savanna oats / cabbage | 0.50 | Oats: 3 oats + 2 seeds; cabbage: 2 + 3 seeds |
| Plains cabbage | 0.34 | 1 cabbage + 2 seeds |
| Desert peanut | 0.20 | 4 peanuts total, including the planting return |
| Desert dragon fruit / bull kelp | 0.15 / 0.28 | Native harvesting |
| Snow mushgloom / poplar | 0.60 / 0.14 | Existing mushroom/sapling behavior |
| Basalt powder cane | 0.10 | Native harvesting |

Growth settings feed the existing growth-attempt system; they are not universally comparable wall-clock multipliers across different mods. Dry, non-fertile crop soil halves the crop setting. Immature configured crops return one planting item and no mature produce.

Selected new livestock examples: capybara yields 3 beef, wildebeest 5 beef plus 2 leather, bison 6 meat plus 2 leather, Arthropleura 4 pachyderm meat, and stalker 2 meat. Typical home-region fish yield 2 servings, large fish 3; desert fish generally yield 1–2. Fractions are probabilistic expected amounts. The new raw/cooked foods generally use nutrition 3/6 and saturation modifiers 0.2/0.6. Fire kills select their cooked counterpart.

## VillagerRealms pairing

VillagerRealms' default `data/villager_realms/stacked_village_dimensions/default.json` contains `{"links":[]}`. Installing VillagerRealms alone therefore enables no custom dimension link.

To enable the pair, copy the complete `examples/aoe-stacked-villages/` folder from the VillagerRealms repository into the world's `datapacks/` folder, then enable/reload the pack. Its JSON names the two dimensions, seam heights, ticking/travel flags and crossing offsets relative to the village center. The supplied crossing uses center X/Z with Desert landing Y=-30 and Nether landing Y=125. Build solid floors, two-block headroom and walkable approaches to both landings; the configuration does not excavate passages. Further crossings can be declared in the same JSON.

Linked columns use the same 256-by-256 village area at every Y in both dimensions. Depending on chunk alignment, that is 256–289 chunks per dimension. Entities and block entities stay active without a player. The integration validates loaded safe landings, navigates workers through short waypoints to an entrance before transferring them, preserves inventory/identity, and backs off failed routes. Waypoints allow long stairs and switchbacks without treating a temporary movement away from the final entrance as a stalled trip. Removing the opt-in releases the extra paired tickets.

The paired updated builds also share a reusable worker loot provider. Regional AoE crop/ore/leaf yields become the base roll; VillagerRealms profession extras reroll that same base for their matching item IDs before global loot modifiers and work-condition effects. An existing apple-specific profession bonus does not automatically become a plum bonus; such profile targets need an explicit fruit update. AoE uses the API only when the updated VillagerRealms is installed. This avoids relying on player-only break events for worker production.

The October 23 production benchmark kept one 256-by-256 village active in both dimensions with no players: 289 ticking chunks per dimension. Ten-minute daytime stages with 32, 64 and 128 residents each sustained 20 TPS, averaging 11.7, 13.3 and 15.8 ms per full server tick. Three ordinary couriers completed physical food/cane deliveries from Desert Y74 to Nether Y18 and gold deliveries upward at every population, with exact input/output conservation. First delivery latencies were about 7.6–8.5 minutes, including travel and processing. Occasional tick spikes remained (149 ms maximum across those stages).

The night stage exposed a distant-house path limit. After the bounded home-route fix, a separate check loaded a copy of that same saved village: the stranded courier walked to its actual bed, all 128 residents completed a continuous minute asleep in their assigned houses, and no measured recovery tick exceeded 50 ms. The daytime population timings and corrected saved-world recovery use separately identified VillagerRealms builds; see [the complete benchmark results](tools/stacked-village-benchmark/BENCHMARK_RESULTS.md) for hashes and retained evidence. This tests one mixed-activity village with 40 shared workstations and three long-distance couriers. It does not establish client FPS, a maximum village count or the cost of 128 simultaneous haulers.

## Validation

Use a new world for a consistent result across the new dimension heights and terrain. Previously generated chunks retain their saved terrain. The complete tested pack is staged in `run/september-production/mods/`: 74 jars, including both updated mods, for Minecraft 1.20.1 / Forge 47.4.22 / Java 17. All 74 staged hashes match `run/september-production/staged-mods.json`. Use these staged copies, which include the production refmap-name compatibility fix documented in `DEPENDENCY_DIMENSIONS.md`.

Run `gradlew.bat build`, `gradlew.bat runGameTestServer` and `gradlew.bat runData` in the AoE repository. The generated-dimension test samples decorated chunks in all six dimensions and rejects forbidden ores/materials. Other tests cover canopy boundaries, ore writes, fluid habitats, native fruit/crop mappings, new mob/food registrations, yak cargo and stack geometry/idempotence.

VillagerRealms validation uses `gradlew.bat build runGameTestServer -PgameTestNamespaces=villager_realms_stacked,villager_realms`. The runtime tests cover empty defaults, configuration reload, full-height paired ownership, cargo transfer, playerless furnace ticking, worker-loot composition and ordinary village behavior.

The October 22 AoE verification passed `build`, `runGameTestServer` and `runData`. The final build/runtime rerun passed **94/94 tests**, including 369 consecutive fully decorated ramp columns with safe support and three-block headroom, the corresponding terrain-only route, and the lowest-floor powder-cane regression. The ramp checks also reject natural basalt columns, fire, soul berry bushes, magma and support removal while allowing runtime building. Resource validation passed **4,170 checks across 1,524 JSON files** against the loaded mod registry. Data generation produced no authored resource changes. Logs are `build/dimensions-final-verification.log`, `build/dimensions-ramp-final-verification.log` and `build/dimension-registry-validation.json`.

VillagerRealms passed **392 unit tests and all 976 required runtime tests** on October 23, including eight focused stacked-dimension tests. The long-route test uses ordinary workers and the normal travel dispatcher on winding staircases exceeding 400 path nodes: a 100-block descent and a 107-block ascent, with physical movement and intact village identity/cargo after transfer. Additional tests verify remote supply/field discovery through custom stacked links, physical ingredient pickup after a winding local route exceeds the old distance-based timeout, and a stationary control that must still time out. The home-route regression follows a 100-rise helix and a distant house, checks obstruction/repair, and requires physical arrival at the actual bed. Home routes may use bounded intermediate stages when the exact path search hits its distance or node budget; genuinely blocked searches retain their normal retry behavior. Global path-search limits are unchanged. These regression tests validate route correctness rather than maximum sustainable population.

The full Forge production pack also passed client/server startup and visual checks of all six dimensions, both underground cave types, the original mobs, seafood items and yak chests. The persistent Immersive Portals seam passed actual downward and upward player crossings before and after a server restart, retaining X/Z exactly. Captures are under `run/portal-production-client/game/screenshots/`; crossing samples are in `run/portal-production-client/dimension-seam-results.json`.

The final production build also reproduced the previously obstructed Basalt-to-Crimson ramp in a fresh world with the same seed and camera coordinates. Its central passage is now clear and dry; compare `aoe-portal-qa-dimensions-ramp-basalt-crimson.png` with `aoe-portal-qa-dimensions-ramp-basalt-crimson-corrected-same-camera.png` in that screenshot folder. All three Nether interlayer ramps were visually inspected.

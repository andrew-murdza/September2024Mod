# Implemented document changes

This records the implementation of the annotations in `NON_WORLDGEN_CHANGES.md` and the supplied `assets_lendal mods2-v8.zip`. The annotated source document is preserved for comparison.

## Decisions confirmed in chat

- New dimension placement restrictions apply only in the six AOE dimensions. Vanilla and other mods' dimensions do not receive those restrictions.
- `snow_dimension` in the annotations means `aoemod:snowy_dimension`.
- Ore products may transfer to the Nether only when they are available in the desert. Lapis, emerald and diamond products remain snowy-only. Explicit material restrictions take priority over general transfer rules.
- Ordinary farmland is allowed in savanna, plains and snowy. Farmer's Delight rich soil farmland retains the separate tropical-only rule stated in the farming annotations.
- Soul soil quartz ore was recolored by an explicitly authorized pixel script. Only quartz-colored pixels changed; its soil background, transparency and image dimensions were preserved.

## Placement and materials

`GameplayDimensions`, `BiomePlacementRestrictions` and `DimensionMaterialAvailability` share the rules across player placement, direct block changes, plant spread and feature placement. Recipe reachability considers alternative ingredients and chains of crafting, smelting and stonecutting recipes. Ingredient cycles do not create materials from nothing. Results are cached and refreshed after recipe/tag reloads.

- The old named restriction groups now select dimensions. Specific Nether plants retain local biome requirements; their stems, wood and foliage can cross biome boundaries within the Nether.
- Mycelium checks the biome above it. Crimson and warped nylium check their respective biome above. Soul sand/soil accept their own biome or the biome immediately above; netherrack similarly accepts crimson or warped forest.
- Material families, ores, crops, bee blocks, the note-block exception, fluids, fire, mushroom blocks, and wood families use the annotated rules. Products also need a reachable recipe using local or explicitly transferred ingredients.
- Sunflower and blueberry placement now uses the same checks, including both sunflower halves, before consuming an item.
- Broken placement tag IDs were corrected.
- Blocks with neither a recipe nor an explicit restriction are treated as natural sources pending further restrictions. The generated availability lists expose these cases for the requested review. The rule system does not remove items from inventories or rewrite existing chunks.

## Farming, drops and trees

- Natural growth, crop/berry yields, spread attempts and bone-meal multipliers now use dimensions. Unconfigured growth and drops fall back to their ordinary behavior.
- Snowy grass, fern and large fern use only beetroot seeds in the configured seed pool. Shears detection also accepts the Forge shears tag.
- Cocoa and rice use their actual maturity properties. Cave-vine tips and turtle eggs now pass the correct state/original growth decision into their growth hooks.
- Rich soil farmland has the fixed tropical hydration range and no automatic fertilization. Ordinary rich soil keeps its separate boosting behavior.
- Small mushrooms and Nether fungi still spread; dense patches no longer grow giant forms spontaneously. Giant mushroom bone meal uses tropical/mushroom-cave habitat checks and accepts sturdy terrain.
- Removed custom large-tree and custom two-by-two growth overrides, generic cocoa attachment to non-jungle logs, and the moss-to-podzol prevention for giant spruce trees. Active generated tree selectors use standard vanilla trees.
- Sculk bone meal uses the configured deep-dark floor feature's distribution.

## Equipment and block safety

- Standard netherite equipment now uses the midpoint of copper and iron durability, enchantability, mining speed and combat/armor attributes. Integer-only values round to the nearest whole value. Its harvest tier sits between copper and iron. The netherite shield is also between copper and iron shields. Existing diamond equipment's original-netherite stat mapping is preserved.
- Lapis-headed iron golems form only in the snowy AOE dimension. Ordinary pumpkin iron/snow golem construction remains unchanged outside the six AOE dimensions.
- Both regular and soul campfires use the existing shared contact-damage prevention.
- Lava is limited to desert/Nether; water placement in the AOE Nether is rejected. Tropical fire protection now follows the tropical dimension. Ordinary vanilla ultra-warm bucket behavior is preserved outside AOE dimensions.

## Supplied assets

The requested models, animations and textures were integrated using the repository's actual namespace and renderer paths. Banana pie, mangosteen pie, soul berry cookie and template pie from the ZIP were excluded. Existing content with those names was not deleted.

The supplied bone bucket and boss axe now have registered usable items. Bone buckets retain their bone container through filling/emptying, cauldrons, dispensers and fluid capabilities. The Illager King uses the supplied boss-axe model with ordinary iron-axe combat properties. The bone spear's held-texture property is registered. Invalid model texture references and animation/root-bone compatibility were corrected during import.

## Six dimensions and seventeen biomes

| Dimension | Biomes |
| --- | --- |
| Tropical | Coral reef, mangrove swamp, rainforest, lush caves, cherry grove |
| Savanna | Savanna |
| Plains | Plains |
| Snowy | Snowy forest, mushroom caves, dripstone caves |
| Desert | Desert, mesa, deep dark |
| Nether | Crimson forest, warped forest, soul sand valley, basalt deltas |

The previous global Overworld override and old AOE biome/dimension IDs were removed. Each new dimension has its own type/noise settings. New worlds generate these dimensions independently; existing terrain is not retroactively converted. Surface rules and active ore/lava/carver features were adjusted to the permitted materials. The five surface dimensions use ordinary skies; the custom Nether uses Nether visuals. Vanilla dimension effects remain unchanged.

## Review lists

The reviewed [block availability inventory](BLOCK_AVAILABILITY.md) lists all 3,052 registered blocks individually across the six dimensions, including separate lists of blocks allowed everywhere and nowhere. The [random-ticking block inventory](RANDOM_TICK_BLOCKS.md) lists every installed block with random-ticking states but no growth-table override; [behavior notes](RANDOM_TICK_REVIEW.md) explain which ticks represent growth, decay, oxidation and other behavior. These reports distinguish dimension eligibility from support/survival, local-biome checks and ordinary acquisition.

These snapshots came from the successful gameplay test server with the complete dependency set loaded. A new server start refreshes the corresponding reports under `build/reports/` from its actual registry, tags and recipes.

## Later-version content

The integration uses VanillaBackport `1.2.0.2640.1`, Platform `1.4.0.2640.1`, Tiny Takeover Backport `1.6.3` with a reproducible compatibility transformation, and YetAnotherConfigLib `3.6.6+1.20.1-forge`. Forge was updated to `47.4.22`; Quark and Zeta were updated to versions that load on the dedicated server. Exact source revisions, local dependency setup, and the detailed feature list are in `FUTURE_BACKPORT_SETUP.md`.

- Sulfur, cinnabar, potent sulfur, sulfur spikes and sulfur cubes retain the dependency's interactions. Sulfur cubes spawn in AOE basalt deltas. Their terrain-generation features are disabled.
- Poplar wood/foliage, shelf mushrooms, red shrubs, cushions, straw beds, wool/concrete stairs and slabs are included. Wandering traders provide poplar saplings, shelf mushrooms and wildflowers. Red shrubs use crimson-nylium support and the same AOE bone-meal spread route as crimson roots.
- Nonbone spears use the full backport attack implementation. Old stacks preserve their NBT during migration. Parched, camel husks and their riders, zombie horses, mount floating and netherite horse armor are included. AOE's limited nautilus remains; the new nautilus and zombie nautilus are excluded.
- Copper golems sort items using copper chests; copper statues, weathering, waxing, scraping, copper lights/chains/bars, shelves, and the newer tuff/copper building variants are included. Existing AOE copper equipment remains canonical. Dye textures are preserved.
- Tiny Takeover supplies baby animal appearance/sounds and golden-dandelion age locks. Rabbit textures and their compatible renderer are preserved; nonvisual rabbit size changes remain enabled. AOE's dolphin breeding and age persistence are retained.
- Armadillos, scutes, wolf armor and all nine wolf variants are included. Tamed wolves receive the newer health pool and food healing without the old damage reduction. Warm pigs display as Boar. Adult temperate cows and both mooshrooms use the updated textures and matching model layout; bats use the updated appearance. Existing AOE Bogged is retained.
- Colored bundles and the string-plus-leather or string-plus-rabbit-hide bundle recipe are enabled. Unrequested Happy Ghasts, dried ghasts, harnesses and duplicate pale/creaking content are suppressed.

## Validation

- `.\gradlew.bat build` passed and produced the mod JARs in `build/libs/`.
- `.\gradlew.bat runGameTestServer` passed all 24 required tests. Coverage includes dimension/material placement, crop growth and drops, local substrate checks, rich farmland, giant mushrooms, sculk bone meal, red shrubs, normal/lapis/copper golem construction, bone-bucket containers and fluid capabilities, bundle recipes, spear migration, wolf health/healing, age locks, netherite equipment and campfire damage.
- The static resource check passed for six dimension/type/noise pairs, seventeen biomes, all 108 reachable AOE configured features and obsolete-ID removal. The four excluded assets retain their pre-import hashes. The ore edit changes only quartz pixels.
- `.\gradlew.bat build runClient` rebuilt the final assets and completed client resource/model loading through title-screen initialization. The updated cow model and baby-armadillo layer initialized successfully; the new copper subtitle and sulfur sound warnings are resolved. The client was then closed. This was a startup smoke check, not an in-world visual review of every mob and item.
- `.\gradlew.bat runData` reaches an existing Fruits Delight data-provider failure: its optional Cuisine Delight `IngredientConfig` class is absent. AOE registers no data providers, and these changes use handwritten resources. Details are in [the dependency setup notes](FUTURE_BACKPORT_SETUP.md#data-generation-limitation).

The runtime still reports unrelated existing recipe, loot, tag and dependency-asset warnings described in the original inventory. The animation parsing warnings were traced to literal `-` values in unchanged Born in Chaos animation files; the imported AOE animation transforms are valid numeric values. The successful build and focused gameplay tests do not establish that every preexisting pack feature or visual asset is correct.

Local validation evidence is retained in `build/gametest-final.log`, `build/reports/game-test-results.xml`, `build/client-resource-validation.log` and `build/final-validation.log` (the data-generation failure). Startup-generated changes to tracked `run/` and `run-data/` files were restored after testing; your annotated source document was not edited.

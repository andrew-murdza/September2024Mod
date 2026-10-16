# Non-worldgen change inventory

Source snapshot: 12 October 2026, repository commit `529d3ed` and the current source files. This is a review of what AOE Mod implements for Minecraft Forge 1.20.1, organized so individual mechanics can be kept, revised or removed.

## Scope and how to read this

- **Included:** added blocks/items/mobs, farming and growth, placement restrictions, drops, animal husbandry, combat, runtime mob spawning, food, recipes, enchanting, presentation and changes to other mods.
- **Excluded:** dimension definitions and their layer/sky/fog setup, noise and terrain settings, surface rules, structures, configured/placed features, tree shapes and generated feature placement. A biome-dependent gameplay rule is still included: planting restrictions and crop growth continue to matter after the world exists.
- **Numerical balance values are omitted.** Relationships, affected objects, ingredients, required conditions and exceptions are retained. Counts are used only where they explain a mechanic, such as a sapling arrangement or twins.
- These are changes made **by this repository**. It does not attempt to catalog every feature that its dependency mods supply on their own. Changes to dependencies are identified where applicable.
- **Implementation caveats** identify code or data that is unused, broken, conditional, or different from its apparent intention. They are not presented as confirmed working gameplay changes. This is a source audit, not a fresh playthrough of every mechanic.
- Source links point to the files that implement each group. The `config` Java classes contain hard-coded rules; their names do not mean all these mechanics have user-editable configuration switches.

## Contents

- [Biome names used by the gameplay rules](#biome-names-used-by-the-gameplay-rules)
- [Added blocks and building materials](#added-blocks-and-building-materials)
- [Planting and building restrictions](#planting-and-building-restrictions)
- [Growth and farming](#growth-and-farming)
- [Bone meal](#bone-meal)
- [Breaking blocks and harvesting](#breaking-blocks-and-harvesting)
- [Block tags and ordinary loot tables](#block-tags-and-ordinary-loot-tables)
- [Block safety, fluids, fire, light and appearance](#block-safety-fluids-fire-light-and-appearance)
- [Equipment and combat](#equipment-and-combat)
- [Enchanting, anvils, and stacking](#enchanting-anvils-and-stacking)
- [Food, seeds, and containers](#food-seeds-and-containers)
- [Recipe availability and crafting changes](#recipe-availability-and-crafting-changes)
- [Food and crafting tag membership](#food-and-crafting-tag-membership)
- [Animals, breeding, and taming](#animals-breeding-and-taming)
- [Combat, hostile mobs, and mob interaction rules](#combat-hostile-mobs-and-mob-interaction-rules)
- [Added mobs](#added-mobs)
- [Mob drops](#mob-drops)
- [Runtime mob spawning and despawning](#runtime-mob-spawning-and-despawning)
- [Animal food and spawning tags](#animal-food-and-spawning-tags)
- [Travel, presentation and compatibility](#travel-presentation-and-compatibility)
- [Mob implementation caveats and inactive code](#mob-implementation-caveats-and-inactive-code)
- [Incomplete or inactive resource entries](#incomplete-or-inactive-resource-entries)
- [Audit coverage and validation](#audit-coverage-and-validation)

## Biome names used by the gameplay rules

These names refer to the mod's biome tags, not every vanilla biome with the same name. This distinction affects planting, harvesting, growth, fluids and fire even though terrain generation is outside this document's scope.

| Name in this document | Biomes actually included |
| --- | --- |
| Rainforest / tropical | `aoemod:jungle`, vanilla Jungle, Bamboo Jungle and Sparse Jungle |
| AOE jungle | Only `aoemod:jungle` |
| Grove / mountains | Only `aoemod:grove` |
| Savanna | Only `aoemod:savanna` |
| Plains | Only `aoemod:plains` |
| Desert | Only `aoemod:desert` |
| Deep dark | Only `aoemod:deep_dark` |
| Crimson forest | Only `aoemod:crimson_forest` |
| Warped forest | Only `aoemod:warped_forest` |
| Soul sand valley | Only `aoemod:soul_sand_valley` |
| Basalt deltas | Only `aoemod:basalt_deltas` |
| Nether biomes | The four AOE crimson forest, warped forest, soul sand valley and basalt deltas biomes above |

"Soul sediments" means soul sand, soul soil, soul sand gold ore, soul soil gold ore, soul sand quartz ore and soul soil quartz ore. Rules using this category accept all six blocks.

"Basalt stones" means basalt, smooth basalt, blackstone, gilded blackstone, basalt gold ore, basalt quartz ore, blackstone gold ore and blackstone quartz ore. This is the substrate group used in the strider spawning rule.

Sources: [src/main/resources/data/aoemod/tags/worldgen/biome/](src/main/resources/data/aoemod/tags/worldgen/biome), [src/main/resources/data/aoemod/tags/blocks/soul_sediments.json](src/main/resources/data/aoemod/tags/blocks/soul_sediments.json), [util/ModTags.java](src/main/java/net/amurdza/examplemod/util/ModTags.java).

## Added blocks and building materials

These are block additions, independent of where terrain generation places them. Their growth, harvesting and recipe rules are described in the corresponding sections below.

| Addition | What is added |
| --- | --- |
| Pale oak building set | Pale oak log, stripped log, wood, stripped wood, planks, stairs, slab, fence, fence gate, door, trapdoor, pressure plate and button. Logs and wood can be stripped with an axe. The building blocks copy warped-wood block properties; doors, trapdoors, buttons and gates use ordinary wooden interaction types. |
| Pale oak vegetation | Pale oak leaves and sapling. The sapling is a separate plant from the vanilla oak sapling. Tree shapes and generated placement are outside this inventory. |
| Pale moss | Pale moss block, pale moss carpet and pale hanging moss. |
| Eyeblossoms | Separate open and closed eyeblossom blocks; the open form emits light. |
| Azalea sapling | A separate azalea sapling item/block, also compostable. |
| Desert grasses | Desert grass and desert tallgrass. |
| Berry bushes | Soul berry bush, blue berry bush and jungle sweet berry bush. The jungle variant displays the ordinary “Sweet Berry Bush” name. |
| Rotatable sunflower | A custom sunflower block used when placing the vanilla sunflower item. |
| Gold ore variants | Basalt gold ore, blackstone gold ore, soul soil gold ore and soul sand gold ore. |
| Quartz ore variants | Basalt quartz ore, blackstone quartz ore, soul soil quartz ore and soul sand quartz ore. The two basalt ore blocks have pillar axes and can be oriented like logs. |
| Copper chain | A copper-colored chain. Quark's chain block-connection/piston mechanic is explicitly extended to recognize this chain alongside the vanilla chain. |

Sources: [ModBlocks](src/main/java/net/amurdza/examplemod/registry/ModBlocks.java), [stripping behavior](src/main/java/net/amurdza/examplemod/block/StrippableRotatedPillarBlock.java), [copper-chain compatibility](src/main/java/net/amurdza/examplemod/mixins/othermods/quark/ChainsConnectCopperChains.java), [common setup](src/main/java/net/amurdza/examplemod/AOEMod.java).

## Planting and building restrictions

Block items check the destination biome before placement and display a message naming the block and biome when placement is rejected. This is a player/item placement check, not a blanket prohibition on every way blocks can appear. If a block belongs to multiple allowed-biome groups, matching any one group is enough.

The following restriction groups have valid entries in the checked-in data:

- Add the six dimensions: aoemod:... tropical_dimension, savanna_dimension, plains_dimension, snowy_dimension, desert_dimension, and nether_dimension and remove any other aoemod dimensions
- Add the biomes: aoemod:... coral_reef, mangrove_swamp, rainforest, tropical river, cherry_grove, lush_caves, savanna, savanna_river, plains, plains_river, frozen_river, frozen_lake, poplar_forest, snowy_taiga, mushroom_caves, dripstone_caves, desert, mesa, deep_dark, crimson_forest, warped_forest, soul_sand_valley, basalt_deltas and remove any other aoemod biomes

#CHANGES:
- Leave the vanilla and other mod's dimensions the same. All blocks follow the normal rules for those. When I say "only" I meant out of the 6 custom dimensions: tropical_dimension, savanna_dimension, plains_dimension, snowy_dimension, desert_dimension, and nether_dimension.
- A block can only exist in a dimension if it can be crafted from the blocks available in that dimension with the exception of non-living nether materials and non-living world materials (stones, sediments, ores) and products made by the nether can be transferred to the desert. Also things made from ores (like blocks of diamond but not diamond ore itself because diamond ore would look weird surrounded in netherrack) and charcoal can be transferred to the nether. Assume that wood products count as living materials. For example, since spruce saplings are the only way to grow spruce trees, spruce leaves, spruce logs, and everything made from spruce logs only exist there. As a another example, torches can be made with both coal and charcoal, so it can exist in dimensions that don't have coal as long as there are logs that can be made into charcoal. Since coal is a non-living material in the desert and sticks can be made from nether trees, torches can exist in the nether even though there is no coal or charcoal in the nether since nether logs cannot be burned into charcoal.
- If something grows from a nether sapling (like crimson fungus), the logs and "leaves" (e.g. nether wart blocks) can exist anywhere in the nether, not just in the crimson forest. This allows trees to grow from one biome to another biome in the nether above it.
- Grass block should not exist in rainforest; Grass stays in rainforest.
- Giant red mushroom and brown mushroom blocks and mushroom stems only exist in tropical_dimension and snow_dimension
- Giant mushgloom blocks and stems only exist in aoemod:snow_dimension
- Spruce saplings only exist in aoemod:snow_dimension instead of just grove
- Poplar saplings and red bush only exist in aoemod:snow_dimension
- Everything else that says rainforest only, savanna only, plains only, grove only, desert only, or nether only should instead point to aoemod:tropical_dimension, aoemod:savanna_dimension, aoemod:plains_dimension; aoemod:snowy_dimension, aoemod:desert_dimension, aoemod:nether_dimension
- Trollber and ripe trollber only exists in snow_dimension
- Fix the broken rules
- Ancient debris only exists in nether and desert
- Bee nest and beehive only exist in tropical, savanna, and plains
- Bell does not exist in biomes without gold ore
- Terracotta only exists in desert and nether
- Amethyst family blocks only exist in snow
- Ice and snow blocks only exist in snow
- Clay only exists in plains, snow
- Mud only exists in savanna
- Cinnabar and sulfur related blocks only in desert and nether
- Add the pickaxe rule to savanna, also removing gravel, sand, red sand (with the same exceptions other than those). This means that all things that need ores that are from pickaxes are not allowed either.
- Red sand and red sandstone is only in desert and nether
- Fire doesn't exist in tropical
- Farmland doesn't exist in tropical, desert, or nether
- Noteblock allowed everywhere even though it requires redstone as I will add another recipe
- Lapis, emerald, and diamond ores (and therefore products by the rule) only in snowy
- Redstone ores only in desert and nether
- Lava only in desert and nether
- netherrack, soul sand, soul soil, crying obsidian, nether ores only in nether (I know this is against the rule I set since they are not living and not in desert, this is an explicit exception; other blocks like blackstone, blocks crafted from Quartz remain in both)
- Obsidian, Magma block only in desert and nether
- Prismarine-related only in snow
- Frogspawn only in tropical
- Polished basalt exists in snow, so things that can be made from it that do not require the regular basalt can be in snow
- Calcite is only in snow
- Deepslate, stone, andesite, diorite, granite, gravel in plains, snow, and desert
- Gold, copper, coal ores in plains, snow, desert
- Iron ore in plains, snow
- Coarse dirt in plains, snow
- End items: desert and nether only
- Beetroots are allowed in tropical and snow
- Wheat is allowed everywhere except snow and nether
- Potatoes, carrots allowed in tropical, savanna, plains
- Firefly Bush only in tropical
- Dried Ghast only in nether and desert
- Dripstone block, pointed dripstone only in snow
- Soul sand/soil only in soul sand valley or the block directly below the soul sand valley
- Netherrack can only be be placed in warped forest, crimson forest, or a block directly below one of those biomes
- Crimson nylium can only be placed if the block above it is crimson forest. Similarly for warped nylium and the warped forest.
- Soul sand/soil can only be placed in the soul sand valley or a block directly below it.
- Prevent mycelium from being placed or dirt growing into mycelium unless the block above it is the mushroom caves
| Blocks | Allowed placement biomes |
| --- | --- |
| AOE azalea sapling | AOE jungle |
| Crimson nylium, crimson fungus, crimson roots, weeping vines | Crimson forest |
| Warped nylium, warped fungus, twisting vines, nether sprouts, warped roots | Warped forest |
| Sculk, sculk vein, sculk sensor, calibrated sculk sensor, sculk catalyst, sculk shrieker, chorus flower, chorus plant | Deep dark |
| Dead bush, desert grass, desert tallgrass | Desert |
| Spruce leaves, spruce sapling, Quark glow shroom, glow shroom stem, glow shroom block and glow shroom ring | Grove |
| Nether wart block, warped wart block, shroomlight | Nether biomes |
| Sugar cane, seagrass, tall seagrass, red mushroom, brown mushroom, glow lichen | Anywhere outside the Nether-biome tag; this check does not literally require the Overworld dimension |
| Birch leaves and birch sapling | Plains |
| Cactus | Rainforest or desert |
| Fern, large fern, mushroom stem, brown mushroom block, red mushroom block, sweet berry bush, blueberry bush | Rainforest or grove |
| Oak leaves and oak sapling | Rainforest or plains |
| Sea pickle, wet sponge, kelp and kelp plant | Rainforest, savanna or grove |
| Grass and grass block | Rainforest, savanna, plains or grove |
| Hanging roots, rooted dirt, acacia leaves and acacia sapling | Savanna |

Please give of blocks allowed in all biomes and a list of blocks allow no biomes (including past 1.20.1). That will allow me to see what I am missing.

Four additional rule groups are written in the data but contain required, nonexistent identifiers. They must be treated as **configured intent, currently broken**, rather than working restrictions. An existing August 2026 game log reports that the whole tags could not load; the same bad identifiers remain in the source examined for this inventory.

| Broken rule group | Intended restrictions and exact affected blocks |
| --- | --- |
| Rainforest only | Azalea, flowering azalea, big dripleaf, small dripleaf, lily pad, mangrove roots, spore blossom, moss carpet, moss block, frogspawn, rose bush, peony, lilac, pitcher plant, allium, pink petals, blue orchid, torchflower; live tube, brain, bubble, fire and horn coral plants/fans/wall fans and coral blocks through the coral tags; mangrove, jungle, cherry, azalea, flowering azalea and dark oak leaves; Farmer's Delight rich soil, rich soil farmland, rice, cabbages, onions and tomatoes; melon, melon stem, cocoa, vine, cave vines and cave-vine body, bamboo and bamboo sapling; dark oak, cherry and jungle saplings and mangrove propagules. The bad reference is `minecraft:frog_spawn`, while the actual block is `minecraft:frogspawn`. |
| Rainforest, savanna or plains | Tall grass, poppy, dandelion, azure bluet, oxeye daisy, cornflower, red/orange/pink/white tulips, lily of the valley, sunflower, hay bale, wheat, potatoes, beetroots, carrots, pumpkin, carved pumpkin and pumpkin stem. The bad reference is `minecraft:hay_bale`, while the actual block is `minecraft:hay_block`. |
| Soul sand valley only | Nether wart, soul berry bush, pale oak sapling, pale oak leaves, pale hanging moss and an intended eyeblossom. The bad reference is `aoemod:eye_blossom`; the registered flowers are `aoemod:open_eyeblossom` and `aoemod:closed_eyeblossom`. |
| Cannot be placed in rainforest | Every block in the pickaxe-mineable, shovel-mineable, Forge glass and Forge glass-pane tags, plus comparator, redstone lamp, intended repeater and intended redstone wire. Bone block, moss block and rich soil farmland are explicit exceptions, and a successful explicit allowed-biome rule takes precedence. `minecraft:redstone_repeater` and `minecraft:redstone` are invalid block IDs. This broad building ban currently fails to load as a tag. |

#CHANGES: Please fix this
The sunflower/blueberry placement handler directly places blocks and bypasses the block-item restriction hook. In particular, the blueberry planting item does not reliably enforce the blueberry bush's listed rainforest/grove restriction. These restrictions do not by themselves prevent bone meal or random ticking from placing a block.

Sources: [util/BiomePlacementRestrictions.java](src/main/java/net/amurdza/examplemod/util/BiomePlacementRestrictions.java), [mixins/block_placement/BlockItemBiomeRestrictions.java](src/main/java/net/amurdza/examplemod/mixins/block_placement/BlockItemBiomeRestrictions.java), [event_handlers/ItemsPlaceModBlocks.java](src/main/java/net/amurdza/examplemod/event_handlers/ItemsPlaceModBlocks.java), [src/main/resources/data/aoemod/tags/blocks/biome_restrictions/](src/main/resources/data/aoemod/tags/blocks/biome_restrictions). Historical diagnostic: [run/logs/latest.log](run/logs/latest.log), August 21, 2026; this is not a fresh runtime test.

## Growth and farming

#CHANGES: Same biome -> dimension changes as listed for allowed blocks.

### Exactly which blocks have biome-controlled natural growth

The mod configures replacement growth decisions for the blocks below. When a growth hook identifies a tracked block, no matching biome entry means natural growth is denied. Blocks absent from this table retain their ordinary growth decision; sugar cane, for example, is not registered for the biome-growth override. Crop and stem growth is further reduced when their supporting block is not fertile. Bone meal is handled separately.

#CHANGES: You do not have to set the growth rate to 0 for a block in a dimension where it cannot be placed. This can shorten the code.

#Give a list of blocks that have a list of random ticking blocks that are not listed below (including post 1.20.1 blocks)


| Block or process | Biomes with a positive natural growth setting |
| --- | --- |
| Wheat, carrots, potatoes, pumpkin stems | Rainforest, savanna and plains |
| Beetroots | Rainforest and grove |
| Melon stems, cocoa | Rainforest |
| Farmer's Delight onion, tomato, cabbage and rice crops | Rainforest |
| Sweet berry bush, jungle sweet berry bush, blueberry bush | Rainforest and grove |
| Soul berry bush | Soul sand valley |
| Cactus and Ecologics prickly pear | Rainforest and desert |
| Cave-vine tips, cave-vine bodies, ordinary vines, bamboo and bamboo saplings | Rainforest |
| Kelp | Rainforest, savanna and grove |
| Sea pickles | Rainforest, savanna and grove |
| Red and brown mushrooms | Rainforest, savanna, plains, grove and desert |
| Quark glow shroom | Grove |
| Crimson fungus and weeping vines | Crimson forest |
| Warped fungus and twisting vines | Warped forest |
| Nether wart | Soul sand valley |
| Chorus flowers | Deep dark |
| Nether Depths Upgrade crimson kelp and crimson kelp plant | Grove, basalt deltas and crimson forest |
| Nether Depths Upgrade warped kelp and warped kelp plant | Grove, warped forest and soul sand valley |
| Budding amethyst's bud/crystal growth | Grove |
| Pointed dripstone's random-tick growth/dripping operations | Grove |
| Oak saplings | Rainforest and plains |
| Birch saplings | Plains |
| Spruce saplings | Grove |
| Acacia saplings | Savanna |
| Cherry saplings, mangrove propagules, dark oak saplings and jungle saplings | Rainforest |
| AOE azalea saplings | AOE jungle |
| Pale oak saplings | Soul sand valley |
| Turtle egg hatch progression | Rainforest; the ordinary time-of-day hatch decision is replaced with this growth decision |

Within the permitted biomes, the rates can still differ. For example, wheat/carrot/potato/pumpkin growth differs between rainforest, savanna and plains; berries differ between rainforest and grove; cactus and prickly pear differ between rainforest and desert. This table intentionally omits balance numbers while preserving exactly which blocks are governed by the system.

Please fix this.
**Cave-vine tip exception:** the tip passes the block below itself to the growth hook. When that block is air or water, the handler often fails to identify the vine and keeps the original chance. Consequently tip extension and tip berry production can bypass the intended rainforest gate. Existing vine-body berry growth passes the correct vine state.

Sources: [config/BlockGrowthConfig.java](src/main/java/net/amurdza/examplemod/config/BlockGrowthConfig.java), [config/BlockConfig.java](src/main/java/net/amurdza/examplemod/config/BlockConfig.java), [event_handlers/BlockGrowthRate.java](src/main/java/net/amurdza/examplemod/event_handlers/BlockGrowthRate.java), [mixins/growth_rate/](src/main/java/net/amurdza/examplemod/mixins/growth_rate), [util/Helper.java](src/main/java/net/amurdza/examplemod/util/Helper.java).


#CHANGES:
- Remove the dependence on the hydration range, since it can only be replaced in the tropical dimension, just use the value for that dimension and similar cases of things that only exist in one-dimension.
- Remove all bonemealing ability from rich soil farmland
- Remove behavior from blocks that only happen in dimensions that they cannot be placed in. Like grass blocks not spreading in certain biomes and dying in those biomes despite not being placeable there.
- Change the mycelium, and warped/crimson nylium to check for the biome above the block for placement restrictions.
- Skip the nylium water check and the survival requirements should also include being in a biome in can be placed in.
- Keep the crimson/warped fungi spreading but remove giant mushrooms and fungi growing on their own without bonemeal.
- Remove the cocoa being available on non-jungle logs
- Remove the prevention of podzol for giant spruce trees
- Allow pale oak saplings to be placed on grass. That may its behavior isn't restricted in non-aoemod dimensions and since the only aoemod dimension it can be placed on is the nether which cannot have dirt or grass
- Make all custom behavior for block placement only apply in the aoemod dimensions. Other changes can work anywhere.

### Soil, support and growth behavior
- **Farmland cannot be trampled.** Pumpkins and melons placed or grown above farmland no longer make it revert simply because those blocks are solid.
- **Hoeing moss makes Farmer's Delight rich soil farmland; hoeing podzol makes ordinary farmland.** The hoe takes wear and plays the tilling sound.
- **Rich soil farmland hydration range depends on biome.** Rainforest, savanna, plains, grove and desert have separate settings; other biomes keep Farmer's Delight's original range. This modification targets rich soil farmland, not ordinary farmland.
- **Rich soil farmland reverts to moss instead of rich soil.** Rich soil also gains support for boosting tall flowers and dropping extra copies of bone-meal-duplicable non-bonemealable plants: lily pad, spore blossom and mangrove roots.
- **Grass-block spreading is biome-dependent.** It spreads in rainforest, savanna, plains and grove. Grass cannot remain grass through its random-tick survival check in desert, deep dark or the Nether-biome set, and turns to dirt there.
- **Mycelium is confined to the designated lower mushroom-cave floor in the grove biome.** Outside that biome/floor combination it turns to dirt on random ticks. It spreads to nearby dirt only within the same allowed floor area and does not require the normal bright-light spreading condition. This is a survival/farming restriction, even for player-placed mycelium. The separate configured mycelium attempt-count map is not used by the replacement random-tick loop.
- **Crimson and warped nylium spread to nearby netherrack naturally**, provided the target meets the nylium survival checks and is not covered by water.
- **Red and brown mushrooms survive daylight.** Both spread using the custom growth gate. Dense patches may attempt giant growth on a mushroom-growing floor within the designated mushroom-cave floor region; giant red/brown mushrooms require mycelium there when bone-mealed.
- **Crimson and warped fungi receive natural random ticks and mushroom-like spreading.** Their permitted natural-growth biomes are listed above. They require their own matching nylium as substrate: crimson fungus on crimson nylium, warped fungus on warped nylium. Their huge-fungus growth uses the mod's configured templates; template shapes are outside this inventory.
- **Quark glow shrooms can stand on any block with a sturdy upper face.** They use mushroom-like spreading and can grow into a giant glow shroom only on mycelium in the designated mushroom-cave floor area, including when fertilized.
- **Ordinary vines grow downward only.** Random upward/sideways expansion is removed.
- **Cactus can survive next to solid blocks.** The mod replaces cactus ticking and Ecologics' separate fruit trigger: cactus grows through the shared age-growth handler and places a prickly pear above a tall-enough column with open space. This combines cactus height growth and fruit production into the mod's biome growth rules.
- **Sugar cane gains waterlogging on the bottom block only.** Upper stalk blocks cannot be waterlogged through liquid placement. Sugar cane planted on moss does not need adjacent water; a waterlogged bottom also satisfies its water requirement. Ordinary soil still uses the usual hydration check.
- **Cocoa can attach to any log in `minecraft:logs_that_burn`**, instead of being limited to jungle logs. This includes eligible logs contributed by other mods.
- **Nether wart can stand on soul soil as well as soul sand.** Crimson roots require crimson nylium. Warped roots accept warped nylium or any soul sediment. Nether sprouts require warped nylium. These support checks are separate from item-placement biome restrictions.
- **Chorus flowers and chorus stems support sculk and sculk catalyst as additional bases.** Chorus-plant survival checks for ground attachment require the deep-dark biome. Natural flower growth uses the deep-dark growth gate. Blocked upward growth or failed branching caps growth with a mature flower; a small remaining ceiling gap can be filled and capped. Side branches require a sufficiently established stem, and upward growth can reduce the flower's growth age. The separate world-generation chorus shape routine is excluded.
- **Spore blossoms and cave vines can hang beneath leaves.** They are no longer limited to their ordinary solid support.
- **Crimson and warped lava kelp, crimson and warped seagrass, and both tall lava-seagrass forms from Nether Depths Upgrade can grow on magma blocks.**
- **Ecologics azalea logs no longer randomly become flowering azalea logs.**
- **Growing giant spruce trees no longer converts moss into podzol.**

Sources: [event_handlers/HoeOnPodzolMoss.java](src/main/java/net/amurdza/examplemod/event_handlers/HoeOnPodzolMoss.java), [event_handlers/NoFarmlandTrample.java](src/main/java/net/amurdza/examplemod/event_handlers/NoFarmlandTrample.java), [mixins/block_placement/](src/main/java/net/amurdza/examplemod/mixins/block_placement), [mixins/block_behavior/randomTicks/](src/main/java/net/amurdza/examplemod/mixins/block_behavior/randomTicks), [mixins/growth_rate/](src/main/java/net/amurdza/examplemod/mixins/growth_rate), [block/BlockHelper.java](src/main/java/net/amurdza/examplemod/block/BlockHelper.java), [mixins/othermods/farmersdelight/](src/main/java/net/amurdza/examplemod/mixins/othermods/farmersdelight), [mixins/othermods/ecologics/](src/main/java/net/amurdza/examplemod/mixins/othermods/ecologics), [mixins/othermods/netherdepthsupgrade/LavaKelpOnMagma.java](src/main/java/net/amurdza/examplemod/mixins/othermods/netherdepthsupgrade/LavaKelpOnMagma.java), [mixins/othermods/netherdepthsupgrade/LavaPlantsOnMagma.java](src/main/java/net/amurdza/examplemod/mixins/othermods/netherdepthsupgrade/LavaPlantsOnMagma.java), [mixins/othermods/quark/GlowShroomBlockChanges.java](src/main/java/net/amurdza/examplemod/mixins/othermods/quark/GlowShroomBlockChanges.java), [mixins/trees/GiantSpruceDontConvertMoss.java](src/main/java/net/amurdza/examplemod/mixins/trees/GiantSpruceDontConvertMoss.java).

### Sapling planting requirements and decorative plants

- Remove the custom 2x2 rules and custom trees.
- Replace the desert (tall) grass with the vanilla block, except they can only be placed in desert dimension and only on sand. (They can still be placed regularly in all vanilla dimensions).

- **Rainforest oak, dark oak, mangrove, cherry and jungle tree growth requires a 2-by-2 square of matching saplings/propagules.** A solitary plant no longer grows through that rainforest route. Failed tree placement restores the saplings. Outside rainforest, oak, birch, spruce and acacia use the mod's selected ordinary-tree growth route; the actual tree designs are excluded here.
- **AOE azalea saplings require a 2-by-2 square of matching azalea saplings entirely inside AOE jungle.** They refuse tree advancement elsewhere. They can be composted.
- **Pale oak saplings require a 2-by-2 square planting to become a tree.** Their grower has no single-sapling tree. They grow on soul sediments and can naturally advance without the ordinary sapling light check, subject to the soul-sand-valley growth gate.
- **Azalea and flowering azalea are decorative, spreading bushes when bone-mealed.** They no longer become azalea trees through that action; the separate azalea sapling supplies the tree-growing route.
- **Sunflower items place the mod's rotatable sunflower**, facing opposite the player's horizontal direction. Picking it gives the ordinary sunflower item; structure rotation/mirroring preserves the intended facing.
- **Fruits Delight blueberries can be planted as the mod's blueberry bush.** It grows/harvests like a berry bush but yields the Fruits Delight blueberry item. Jungle sweet berry bushes supply a separate sweet-berry-bush variant.
- **Soul berry bushes grow on soul sediments and yield soul berries.** They do not need the ordinary sweet berry bush light condition for natural growth. Harvest yield is configured only for soul sand valley.
- **Desert grass and desert tallgrass grow on sand.** Bone meal turns desert grass into desert tallgrass. Despite the name, this desert tallgrass class is a single block, not an ordinary two-block-tall grass plant.
- **Pale hanging moss attaches below sturdy ceilings, leaves or more pale hanging moss.** Unsupported strands break, and bone meal on a strand extends its lowest tip downward into empty space. It passes skylight.
- **Open and closed eyeblossoms switch with day/night.** They open at night, close during the day, chime when changing and schedule nearby matching flowers to change after a delay. Open flowers emit light and end-rod particles. Both forms poison bees touching them outside Peaceful difficulty. They grow on soul sediments; their suspicious-stew effects are blindness for the open flower and nausea for the closed flower.
- **Pale oak logs and wood can be stripped with an axe**, preserving their axis. Pale moss is a plain block; it does not inherit the ordinary moss block's bone-meal spreading behavior.
- **Two-block plant placement requires clear space for both halves.** Placing a neighboring block also removes orphaned upper/lower halves instead of leaving broken half-plants behind.

Sources: [worldgen/RainforestSaplingHelper.java](src/main/java/net/amurdza/examplemod/worldgen/RainforestSaplingHelper.java), [worldgen/AzaleaSaplingTreeGrower.java](src/main/java/net/amurdza/examplemod/worldgen/AzaleaSaplingTreeGrower.java), [worldgen/PaleOakTreeGrower.java](src/main/java/net/amurdza/examplemod/worldgen/PaleOakTreeGrower.java), [mixins/trees/CustomTrees.java](src/main/java/net/amurdza/examplemod/mixins/trees/CustomTrees.java), [mixins/trees/RainforestTreeGrower.java](src/main/java/net/amurdza/examplemod/mixins/trees/RainforestTreeGrower.java), [mixins/trees/RegularSpruceSaplingsUseConfiguredTree.java](src/main/java/net/amurdza/examplemod/mixins/trees/RegularSpruceSaplingsUseConfiguredTree.java), [mixins/trees/AzaleaRainforestTree.java](src/main/java/net/amurdza/examplemod/mixins/trees/AzaleaRainforestTree.java), [block/](src/main/java/net/amurdza/examplemod/block), [event_handlers/ItemsPlaceModBlocks.java](src/main/java/net/amurdza/examplemod/event_handlers/ItemsPlaceModBlocks.java), [event_handlers/DoublePlantOrphanCleanup.java](src/main/java/net/amurdza/examplemod/event_handlers/DoublePlantOrphanCleanup.java), [mixins/block_behavior/DoublePlantsAreNotMissingHalves.java](src/main/java/net/amurdza/examplemod/mixins/block_behavior/DoublePlantsAreNotMissingHalves.java), [AOEMod.java](src/main/java/net/amurdza/examplemod/AOEMod.java).

### Cave vines, sea pickles and growth-stopping tools

- Cave-vine tips and bodies are waterloggable and can grow into water. Their berry and waterlogged state is preserved when changing between tip and body.
- Both cave-vine tips and existing body segments can naturally grow berries. Berryless body segments need sufficient light; the growth gate is configured for rainforest, with the tip bypass caveat noted above. A body segment does not have to wait for the vine to extend to get berries.
- Using an axe on a cave-vine segment disables that segment's automatic berry production. Using shears on a tip makes that tip fully aged and stops further length growth. These actions do not consume tool durability in the cave-vine helper.
- Right-clicking a berry-bearing cave vine harvests biome-configured glow berries and clears its berries. Holding glow berries skips this harvest action, allowing placement interactions. The configured harvest yield exists only in rainforest; a harvested vine outside that set can lose its berries without yielding fruit.
- Waterlogged sea pickles naturally increase the pickle count in their existing block. Bone meal also increases the count, without requiring coral beneath them. Bone meal does not perform vanilla's surrounding sea-pickle spreading.
- Shearing a sea-pickle block disables its natural count growth. Bone meal can still increase its count. This action damages the shears.
- Shearing any bamboo stalk finds the top of its column and stops that top's natural growth. Shearing a bamboo sapling turns it into a stopped, leafless bamboo stalk. These actions damage the shears.

Sources: [mixins/block_behavior/CaveVineHeadBehavior.java](src/main/java/net/amurdza/examplemod/mixins/block_behavior/CaveVineHeadBehavior.java), [mixins/block_behavior/CaveVinePlantBehavior.java](src/main/java/net/amurdza/examplemod/mixins/block_behavior/CaveVinePlantBehavior.java), [mixins/block_behavior/CaveVinePlantBehaviorPart2.java](src/main/java/net/amurdza/examplemod/mixins/block_behavior/CaveVinePlantBehaviorPart2.java), [block/BlockHelper.java](src/main/java/net/amurdza/examplemod/block/BlockHelper.java), [mixins/block_behavior/SeaPickleBehavior.java](src/main/java/net/amurdza/examplemod/mixins/block_behavior/SeaPickleBehavior.java), [event_handlers/BambooShearing.java](src/main/java/net/amurdza/examplemod/event_handlers/BambooShearing.java).

## Bone meal
- Fix Bonemealing sculk to place sculk shriekers, sculk sensors and sculk catalysts in the same ratio as the current forest floor json.
- Allow giant mushrooms on any terrain only only in tropical or mushroom caves
- 

- **Bone meal effectiveness depends on the biome above the clicked block.** Ordinary plants receive enhanced repeated attempts in rainforest and savanna. Mushrooms, mycelium and Quark glow shrooms use a separate mushroom multiplier, enhanced in rainforest and grove. One use consumes one bone meal outside creative mode even if its attempts accomplish nothing, and plays a single effect burst. If no configured biome matches, the current lookup returns no successful attempts rather than falling back to ordinary effectiveness.
- **Lily pads, spore blossoms and mangrove roots duplicate as dropped items.**
- **Flowers and several flower-like blocks spread copies nearby instead of merely dropping duplicates.** The handler covers small flowers; tall flowers such as sunflower, lilac, rose bush and peony; applicable double plants; desert tallgrass; crimson roots, warped roots, nether sprouts and dead bushes; sculk sensors, sculk catalysts and sculk shriekers; and wet sponges. New copies require a sturdy supporting surface and valid survival conditions. Double plants must fit both halves. Existing water/lava environments are respected by the spreading search.
- **Live tube, brain, bubble, fire and horn coral plants and floor fans can be spread with bone meal.** This explicit list does not include wall fans or dead coral.
- **Rooted dirt spreads hanging roots beneath nearby rooted dirt**, rather than only affecting the single clicked column.
- **Small dripleaf creates additional small dripleaf**, including targets in air or water, instead of using the ordinary conversion-to-big-dripleaf action.
- **Sugar cane and cactus can be bone-mealed into taller columns**, up to the handler's column limit and only when the target blocks can survive.
- **Nether wart can be advanced with bone meal.**
- **Chorus flowers can be bone-mealed while not fully mature.** Applying bone meal to a chorus stem searches its connected chorus plant and ticks the lowest reachable non-mature flower. Those ticks still use the chorus growth rules.
- **Red/brown mushrooms cannot be fertilized into giant mushrooms outside mycelium in the designated lower mushroom-cave floor.** The player-use handler rejects that invalid action. Quark glow shrooms also require that mycelium/floor combination to actually become giant.
- **Bone meal on matching ground uses that biome's ground-cover selection.** Matching targets are rainforest moss; savanna/plains grass blocks; grove grass blocks and valid mushroom-cave mycelium; desert sand/red sand/grass; deep-dark sculk; crimson nylium in crimson forest; warped nylium in warped forest; and soul sediments in soul sand valley. The placement search checks its targets too: notably desert land plants are placed on sand, even when the clicked block was red sand or grass. Plant contents of the configured features are excluded as requested.
- **Deep-dark sculk is currently an incomplete bone-meal target.** It passes the ground-target check, but there is no deep-dark entry in the subsequent feature-selection method, so that path consumes bone meal without placing ground cover in deep dark.
- **Bone meal used in water selects the local biome's seafloor/shallow-water planting routine.** A matching route also exists for lava in the AOE Nether biomes. Rainforest, savanna, plains, grove, desert, crimson forest, warped forest, soul sand valley and basalt deltas each have an explicit route; grove liquid areas above the designated mushroom floor use its mushroom-cave route.  A submerged use does not spill its planting onto dry land,and the underside-click case retains the ordinary fallback.
- **Sea pickles, pale hanging moss and decorative azaleas use the special behaviors described above.** Other valid bone-mealable blocks receive their original growth action, repeated according to the biome multiplier.

Sources: [event_handlers/BoneMeal.java](src/main/java/net/amurdza/examplemod/event_handlers/BoneMeal.java), [config/BoneMealConfig.java](src/main/java/net/amurdza/examplemod/config/BoneMealConfig.java), [mixins/item/UnderwaterBoneMealUsesBiomeSeafloor.java](src/main/java/net/amurdza/examplemod/mixins/item/UnderwaterBoneMealUsesBiomeSeafloor.java), [src/main/resources/data/aoemod/tags/blocks/bone_meal_duplicates.json](src/main/resources/data/aoemod/tags/blocks/bone_meal_duplicates.json), [mixins/trees/AzaleaRainforestTree.java](src/main/java/net/amurdza/examplemod/mixins/trees/AzaleaRainforestTree.java), [block/PaleHangingMossBlock.java](src/main/java/net/amurdza/examplemod/block/PaleHangingMossBlock.java).

## Breaking blocks and harvesting

### How the override works

#CHANGES:
- Add the shear tag as well the ShearsItem
- You don't need to change the drops for a block that cannot exist in the aoemod dimension or in a vanilla dimension.
- Make grass, ferns, tall ferns drop beetroot seeds only in snow instead of both wheat seeds and beetroot seeds
- Remove preventing drops from blocks in dimensions where they cannot be placed. This means it should only affect the quantity and type of drops, not whether drops occur.

The following tables describe **player-breaking overrides**. They replace the normal drops of configured blocks; a configured block broken in a biome with no entry produces no configured item drops. They do not universally replace decay, explosions or every other destruction path. Creative breaking gives no drops. Only the ore entries below apply Fortune to their configured yields; the plant entries do not. Silk Touch does not restore the original block-item drops through this handler, although it still affects the block's experience calculation.

Shears bypass this replacement for leaves, grass, tall grass, ferns, large ferns, dead bushes, desert grass and desert tallgrass, preserving their normal shearing route. Shearing a red/brown mushroom block or Quark glow-shroom block/stem gives the block item **as well as** any applicable configured mushroom drops. Tool-tag-only shears do not qualify here unless they are actually a `ShearsItem`.

### Ore products

Both stone and deepslate variants are included for every Overworld ore named below. These are drop rules, not ore-generation changes.

| Ore | Product | Biomes with a drop entry |
| --- | --- | --- |
| Coal ore | Coal | Grove, plains, desert, deep dark |
| Copper ore | Raw copper | Grove, plains, desert, deep dark |
| Gold ore | Raw gold | Grove, plains, desert, deep dark |
| Iron ore | Raw iron | Grove and plains |
| Lapis ore | Lapis lazuli | Grove |
| Diamond ore | Diamond | Grove |
| Emerald ore | Emerald | Grove |
| Redstone ore | Redstone dust | Desert and deep dark |
| Nether quartz ore | Quartz | Crimson forest and warped forest |
| Blackstone quartz ore, basalt quartz ore | Quartz | Basalt deltas |
| Soul soil quartz ore, soul sand quartz ore | Nourished Nether soul quartz | Soul sand valley |
| Nether gold ore | Gold ingots instead of nuggets | Crimson forest and warped forest |
| Blackstone gold ore, basalt gold ore | Gold ingots | Basalt deltas |
| Soul soil gold ore, soul sand gold ore | Gold ingots | Soul sand valley |

Yields differ by biome for coal, gold and iron. All listed ores use the mod's custom Fortune scaling.

### Leaves, mushroom blocks and fungus blocks

| Broken block | Configured products | Biomes |
| --- | --- | --- |
| Spruce leaves | Spruce saplings, sticks | Grove |
| Acacia leaves | Acacia saplings, sticks | Savanna |
| Birch leaves | Birch saplings, sticks | Plains |
| Oak leaves | Oak saplings, sticks, apples | Rainforest and plains |
| Dark oak leaves | Dark oak saplings, sticks, Fruits Delight mangosteen | Rainforest |
| Cherry leaves | Cherry saplings and sticks; a configured cherry-fruit lookup is broken with the installed Fruits Delight version, as explained below | Rainforest |
| Mangrove leaves | Mangrove propagules, sticks, Fruits Delight orange | Rainforest |
| Azalea leaves | AOE azalea saplings, sticks, Fruits Delight pear | Rainforest |
| Flowering azalea leaves | Flowering azalea bushes, sticks, Fruits Delight pear | Rainforest |
| Jungle leaves | Jungle saplings, sticks, Alex's Mobs bananas | Rainforest |
| Warped wart block | Warped fungus | Desert, deep dark, basalt deltas, crimson forest, warped forest, soul sand valley |
| Nether wart block | Crimson fungus | Desert, deep dark, basalt deltas, crimson forest, warped forest, soul sand valley |
| Red mushroom block | Red mushrooms | Rainforest and grove |
| Brown mushroom block | Brown mushrooms | Rainforest and grove |
| Quark glow shroom block and glow shroom stem | Quark glow shrooms | Grove |

Leaf sapling/stick yields are higher in rainforest than in the lower-yield sets, and the fruit entries are chance drops. Cherry leaves request `fruitsdelight:cherries`, then `fruitsdelight:cherry`; the installed Fruits Delight has Bayberry rather than either of those fruit items. The helper checks only for null, but Forge returns its air-item default for unknown item IDs, so the nominal apple fallback does not repair that lookup. Cherry leaves therefore have no working fruit drop through this entry. Dark-oak mangosteen is displayed as Plum by this mod's language override.

### Crops and berries

| Broken block | Configured products | Biomes |
| --- | --- | --- |
| Wheat | Wheat when mature, plus wheat seeds | Rainforest, savanna, plains |
| Carrot crop | Carrots, with maturity-dependent yield | Rainforest, savanna, plains |
| Potato crop | Potatoes, with maturity-dependent yield; chance of poisonous potatoes only for mature plains crops | Rainforest, savanna, plains |
| Beetroot crop | Beetroot when mature, plus beetroot seeds | Rainforest and grove |
| Melon stem | Melon seeds, with maturity-dependent yield | Rainforest |
| Pumpkin stem | Pumpkin seeds, with maturity-dependent yield | Rainforest, savanna, plains |
| Melon | A whole melon block instead of slices | Rainforest |
| Pumpkin | Pumpkin block | Rainforest, savanna, plains |
| Cocoa | Intended cocoa beans with maturity-dependent yield; see implementation caveat below | Rainforest |
| Vanilla sweet berry bush | Both Fruits Delight blueberries and sweet berries; amount depends on maturity | Rainforest and grove |
| Jungle sweet berry bush | Sweet berries; amount depends on maturity | Rainforest and grove |
| Nether wart | Nether wart; amount depends on maturity | Soul sand valley |
| Farmer's Delight onion crop | Onions | Rainforest |
| Farmer's Delight rice crop | Intended rice; see implementation caveat below | Rainforest |
| Farmer's Delight tomato crop | Tomatoes when mature, plus tomato seeds | Rainforest |
| Farmer's Delight cabbage crop | Cabbages when mature, plus cabbage seeds | Rainforest |
| Ecologics prickly pear | Prickly pears when mature | Rainforest; both configured entries use rainforest, so no desert breaking yield is configured |
| Cave-vine tips and bodies | Glow berries even when not visibly bearing fruit, with an extra yield when bearing berries | Rainforest |
| Dead bush | Sticks | Desert |

Right-click harvesting is separate from breaking. Vanilla sweet berry bushes and jungle sweet berry bushes yield sweet berries, while blueberry bushes yield blueberries. Their custom right-click amounts depend on maturity and whether the biome is rainforest or grove; unsupported-biome berry uses can fall through to the underlying bush behavior. Soul berry bushes harvest soul berries only with a positive soul-sand-valley yield. Cave-vine right-click behavior is described above.

**Implementation caveat:** cocoa's drop helper delegates to an `AGE_3` check even though cocoa uses `AGE_2`; rice's helper reads `AGE_7` even though the installed Farmer's Delight rice crop uses `AGE_3`. Breaking these crops through the configured handler can therefore fail instead of reliably returning the intended products. These are source/dependency inspection findings, not requested code fixes.

### Seeds from grass and ferns

The mod replaces the normal seed choice with a grouped weighted selection. A successful drop selects from that biome's pool; it does not independently guarantee every seed listed.

| Broken vegetation and biome | Possible seed/food drops |
| --- | --- |
| Grass, tall grass, fern or large fern in rainforest | Wheat seeds, beetroot seeds, pumpkin seeds, carrot seeds, potato seeds, melon seeds, rice seeds, Farmer's Delight cabbage seeds, Farmer's Delight tomato seeds, onion seeds |
| Grass or tall grass in savanna | Wheat seeds, pumpkin seeds, potatoes and carrots themselves |
| Grass or tall grass in plains | Wheat seeds, pumpkin seeds, carrot seeds, potato seeds |
| Grass, fern or large fern in grove | Beetroot seeds and wheat seeds |
| Desert grass or desert tallgrass in desert | Wheat seeds |

Tall grass, large ferns and desert tallgrass use increased seed-drop amounts relative to their shorter counterparts. Ordinary tall grass has no grove seed entry. The custom carrot/potato/rice/onion seeds are distinct from the corresponding foods.

Sources for all drop tables: [config/BlockConfig.java](src/main/java/net/amurdza/examplemod/config/BlockConfig.java), [config/BlockBehaviorConfig.java](src/main/java/net/amurdza/examplemod/config/BlockBehaviorConfig.java), [event_handlers/CustomBlockDrops.java](src/main/java/net/amurdza/examplemod/event_handlers/CustomBlockDrops.java), [event_handlers/BerryDrops.java](src/main/java/net/amurdza/examplemod/event_handlers/BerryDrops.java), [block/BlueBerryBushBlock.java](src/main/java/net/amurdza/examplemod/block/BlueBerryBushBlock.java), [block/SoulBerryBushBlock.java](src/main/java/net/amurdza/examplemod/block/SoulBerryBushBlock.java), [block/BlockHelper.java](src/main/java/net/amurdza/examplemod/block/BlockHelper.java).

## Block tags and ordinary loot tables

- The **dirt block tag** is extended to include moss, Pale Moss, mud, Farmer's Delight rich soil/rich farmland, and farmland, in addition to the listed vanilla dirt-family blocks. The **sand tag includes the entire dirt tag** as well as sand/red sand; anything checking that tag may consequently accept those soils.
- The **leaves block tag** adds Pale Oak Leaves, Quark Glow Shroom Block, red/brown mushroom blocks, and Nether Wart/Warped Wart blocks. The **mushroom-grow-block tag** adds moss. Pale-oak logs/wood variants enter logs and logs-that-burn block/item tags; Pale Oak Planks enter planks block/item tags. Pale-oak and azalea saplings enter the block sapling tag, but only azalea enters the added item sapling file. Open/Closed Eyeblossom enter the small-flower block tag.
- Pale-oak wood building pieces enter the corresponding wooden door/trapdoor/stair/slab/button/pressure-plate/fence/fence-gate block tags and axe-mining tag. Pale Moss Block/Carpet, Pale Hanging Moss, and Pale Oak Leaves enter the hoe-mining tag. Copper Chain, all added basalt/blackstone/soul-sand/soul-soil gold/quartz ores, and **vanilla gravel** enter the pickaxe-mining tag. Soul-sand/soul-soil gold/quartz ores also enter the shovel-mining tag.
- A custom `minecraft:mineable/shears` tag names red/brown mushroom blocks, mushroom stem, and Quark glow-shroom block/ring/stem. The tag itself does not guarantee vanilla shears consult it; runtime shears/drop behavior must be read from the separate block code.
- Snow! Real Magic's containable-block tag lists **Quark glow shrooms; vanilla grass, brown/red mushrooms, dead bushes; the vanilla small-flower, flower, tall-flower, and sapling tags; and Snow! Real Magic's `offset_y` tag** for plants that can be embedded in its snow. Optional additions are the common `c:grass`, `c:grass_like`, `c:flowers`, `c:mushrooms`, `c:saplings`, and `c:bushes` tags; Regions Unexplored's `shrubs` tag; Biomes O' Plenty dune grass, desert grass, dead grass, sprout, and bush; BYG winter grass, leaf pile, flower patch, and clover patch; and Twigs bamboo leaves and azalea flowers. Entries merge with the dependency's tag because the file does not replace it.
- **Ordinary custom block loot:** pale-oak log/wood/stripped variants, planks/building pieces, pale moss block/carpet/hanging moss, open/closed eyeblossom, pale-oak/azalea saplings, and copper chain have self-drop tables (ordinary explosion handling; door lower-half and double-slab handling). Pale-oak leaves use shears/Silk Touch for the leaves themselves and otherwise chance-based saplings, sticks, and apples with Fortune affecting chances. Blue Berry Bush drops blueberries at fruit-bearing maturity stages. Jungle Sweet Berry Bush and Soul Berry Bush have berry tables. Desert Grass/Tallgrass have self + wheat-seed tables. The listed basalt/blackstone gold/quartz and Soul Soil Gold Ore tables give the ore item plus its nugget/quartz. Runtime `CustomBlockDrops` can supersede these tables for configured blocks; see **Breaking blocks and harvesting** for the effective player-break result.

Sources: [src/main/resources/data/minecraft/tags/blocks/](src/main/resources/data/minecraft/tags/blocks), [data/minecraft/tags/items/](src/main/resources/data/minecraft/tags/items), [data/snowrealmagic/tags/blocks/containables.json](src/main/resources/data/snowrealmagic/tags/blocks/containables.json), [data/aoemod/loot_tables/blocks/](src/main/resources/data/aoemod/loot_tables/blocks).

## Block safety, fluids, fire, light and appearance

#CHANGES:
- Make sure that soul campfires also don't deal damage
- Change the lapis iron golem condition to only apply in snow and prevent it from forming in other aoemod dimensions and having vanilla behavior stay the same. You can base it on the dimension instead.

- **Cactus no longer damages touching entities or destroys dropped items through its contact damage.**
- **Sweet berry bushes no longer slow or hurt touching entities**, including the custom bush subclasses. Snow-covered berry behavior is also disabled through the Snow! Real Magic compatibility mixin.
- **Campfires no longer inflict their contact damage.**
- **Soul sand and honey blocks lose the speed/jump penalties set in vanilla block properties.** The mixin removes all vanilla block initialization calls to speed/jump-factor modifiers; it does not remove every other honey interaction, such as side sliding.
- **Carved pumpkins no longer create vanilla iron golems or snow golems through their normal placement pattern.**
- **Iron golems are built with a lapis block as the head.** Placing lapis atop the usual T-shaped iron-block body consumes the lapis and iron frame and creates a player-built iron golem. The arms can face either horizontal axis; no replacement snow-golem recipe is supplied. Source: [LapisGolemPlacement](src/main/java/net/amurdza/examplemod/event_handlers/LapisGolemPlacement.java).
- **Water no longer freezes naturally through the biome freezing check.**
- **Vanilla snow layers do not melt from random ticking/light.** Snow! Real Magic's light-melting thresholds are raised to the maximum light level; other Snow! Real Magic melting causes are not removed by that specific mixin.
- **Lava no longer ignites nearby blocks through lava random ticks, anywhere.**
- **Lava buckets cannot place lava in rainforest; water buckets cannot place water in the AOE Nether-biome set.** These checks use biome identity instead of the dimension's ordinary ultra-warm rule. Flowing lava entering rainforest becomes moss; flowing water entering the AOE Nether-biome set becomes obsidian.
- **Rainforest prevents fire placement/spread and lightning-started fire.** Both ordinary and soul-fire placement events are blocked there, and ordinary fire ticks remove existing fire in that biome.
- **Living entities in rainforest are protected from burning, fire contact, lava, magma/hot-floor damage and lightning damage.** Lightning-strike events are canceled, and fire is cleared during living ticks and hurt handling. Entity ignition setters also reject new fire outside direct lava contact; the living-entity protection still cancels lava damage and extinguishes them.
- **Pale moss blocks, pale moss carpet, pale hanging moss and Nourished Nether soul weeds are nonflammable.** They are excluded from lava-ignitable properties; attempts to light fire on/against them are blocked, and ordinary fire touching these protected blocks is removed. Soul weeds also explicitly report no flammability/fire spread.
- **Ordinary moss blocks, live coral blocks, rich soil and rich soil farmland emit strong light.** Cave vines with berries emit stronger light than vanilla. Leaves no longer block light; ordinary moss passes skylight. Pale moss is a separate plain block and does not automatically inherit ordinary moss's glow.
- **Plants are centered instead of using the horizontal random offset.** This affects mangrove propagules, tall seagrass, dandelions, torchflowers, poppies, blue orchids, alliums, azure bluets, every tulip color, oxeye daisies, cornflowers, wither roses, lilies of the valley, sunflowers, lilacs, rose bushes, peonies, tall grass, large ferns, pitcher plants, bamboo and bamboo saplings, warped roots, crimson roots, nether sprouts, pointed dripstone and hanging roots. The generic horizontal-offset change also affects modded blocks requesting the same offset type. Ferns additionally lose their ordinary full random offset; short grass keeps its full random offset. Custom blocks copying affected properties inherit those changes.

Sources: [mixins/block_behavior/entity_inside/](src/main/java/net/amurdza/examplemod/mixins/block_behavior/entity_inside), [mixins/block_behavior/CarvedPumpkinNoGolems.java](src/main/java/net/amurdza/examplemod/mixins/block_behavior/CarvedPumpkinNoGolems.java), [mixins/block_behavior/SoulSandSoilHoneyDontSlow.java](src/main/java/net/amurdza/examplemod/mixins/block_behavior/SoulSandSoilHoneyDontSlow.java), [mixins/block_behavior/NoIceOnWaterPart1.java](src/main/java/net/amurdza/examplemod/mixins/block_behavior/NoIceOnWaterPart1.java), [mixins/block_behavior/SnowDoesNotMelt.java](src/main/java/net/amurdza/examplemod/mixins/block_behavior/SnowDoesNotMelt.java), [mixins/block_behavior/LessBlockOffsetsPart1.java](src/main/java/net/amurdza/examplemod/mixins/block_behavior/LessBlockOffsetsPart1.java), [mixins/block_behavior/LessBlockOffsetsPart2.java](src/main/java/net/amurdza/examplemod/mixins/block_behavior/LessBlockOffsetsPart2.java), [mixins/block_behavior/light/](src/main/java/net/amurdza/examplemod/mixins/block_behavior/light), [mixins/lava_and_water/](src/main/java/net/amurdza/examplemod/mixins/lava_and_water), [mixins/no_fire_in_tropical/](src/main/java/net/amurdza/examplemod/mixins/no_fire_in_tropical), [event_handlers/NoTropicalFire.java](src/main/java/net/amurdza/examplemod/event_handlers/NoTropicalFire.java), [mixins/othermods/snowrealmagic/](src/main/java/net/amurdza/examplemod/mixins/othermods/snowrealmagic), [mixins/othermods/nourishednether/SoulWeedsNotFlammable.java](src/main/java/net/amurdza/examplemod/mixins/othermods/nourishednether/SoulWeedsNotFlammable.java), [AOEMod.java](src/main/java/net/amurdza/examplemod/AOEMod.java).

## Equipment and combat
- Replace non-bone spears with the vanilla versions of 1.21+
- Change netherite to halfway between copper and iron


- **Bone equipment:** adds bone sword, shovel, pickaxe, axe, hoe, shears, arrow, and spear. Bone tools share the copper tool tier's stats, use stone-level harvesting, and sit between stone and iron in Forge's tier order. Bone tiered tools repair with bones. Bone shears use normal shears behavior with their own durability; bone arrows use the ordinary arrow item implementation. The bone-shears recipe is broken, as explained below.
- **Copper equipment:** adds copper sword, shovel, pickaxe, axe, hoe, helmet, chestplate, leggings, boots, spear, and shield. Copper tools have stone-level harvesting with improved speed, durability, and enchantability compared with stone; tiered tools and armor repair with copper ingots. Copper armor has its own protection/durability and does not add armor toughness or knockback resistance.
- **Spears:** adds wooden, stone, bone, copper, iron, golden, diamond, and netherite spears. Holding one in the main hand increases entity attack reach. Normal attacks work as melee attacks. Holding use starts a delayed thrust; aiming at a living target after the delay can hit it, with extra damage based on the relative movement speed of wielder and target. Sufficient relative speed also knocks back the target and can dismount a riding target. Each target can be damaged only once during a continuous use; releasing and using again resets that list. Successful hits consume durability. A spear at its last remaining durability cannot start the held-use action. The held-use hit check traces entities and does not explicitly trace blocking terrain. These are held thrust weapons, not thrown projectiles. Materials differ in attack timing, damage multiplier, and knockback/dismount thresholds. Netherite spear uses iron-tier combat/tool stats and is fire resistant.
- **Material shields:** adds wooden, copper, iron, golden, diamond, and netherite shields plus a boss shield. Each has its own texture and durability. Repair ingredients are respectively planks, copper ingots, iron ingots, gold ingots, diamonds, netherite ingots, and iron ingots. The boss shield uses a larger rendered model. Netherite shield is fire resistant. The boss shield has no crafting recipe in the checked-in resources.
- **Mace and breeze rod:** adds a durable, enchantable mace and a breeze rod ingredient. A hit credited to an airborne living attacker holding a mace gains extra damage once the attacker has fallen far enough; damage increases with fall distance, with diminishing increases for longer falls. The successful event resets the attacker's fall distance. The handler checks the attacker and its main-hand mace, not specifically that the damage was a melee strike.
- **Mace enchantments:** Density adds fall-distance-scaled bonus damage; Breach adds bonus damage proportional to the victim's armor attribute; Wind Burst launches the attacker upward after a qualifying falling hit. Breach is implemented as added damage, not a direct reduction of the armor value. These enchantments are mace-specific and do not declare mutual incompatibility with one another. A mixin also broadly allows the mace through the base enchantment applicability check, so the table can offer it enchantments ordinarily intended for other equipment; this does not give every such enchantment a useful mace effect.
- **Diamond equipment is promoted to original netherite stats:** diamond sword, shovel, pickaxe, axe, hoe, helmet, chestplate, leggings, and boots use the corresponding original netherite item's durability, mining speed, enchantability, and default equipment attributes. Armor therefore inherits original netherite defense/toughness/knockback-resistance attributes.
- **Netherite equipment is reduced to iron stats:** netherite sword, shovel, pickaxe, axe, hoe, helmet, chestplate, leggings, and boots use the corresponding iron item's durability, mining speed, enchantability, and default equipment attributes. The stat substitution deliberately bypasses the other substitutions, so diamond copies original netherite rather than recursively copying iron. These overrides do not change the items' registered harvest tiers, fire-resistance flags, repair materials, or identities. Custom diamond/netherite spears and shields are not included in that standard-equipment substitution.
- **Wooden Bucket compatibility:** the specific `woodenbucket:wooden_bucket` is non-damageable and has no durability depletion. It cannot hold source or flowing lava. Other BucketLib bucket items are not changed by this mixin.

Sources: [src/main/java/net/amurdza/examplemod/registry/ModItems.java](src/main/java/net/amurdza/examplemod/registry/ModItems.java), [registry/ModEnchantments.java](src/main/java/net/amurdza/examplemod/registry/ModEnchantments.java), [item/ModToolTiers.java](src/main/java/net/amurdza/examplemod/item/ModToolTiers.java), [item/CopperArmorMaterial.java](src/main/java/net/amurdza/examplemod/item/CopperArmorMaterial.java), [item/SpearItem.java](src/main/java/net/amurdza/examplemod/item/SpearItem.java), [item/MaterialShieldItem.java](src/main/java/net/amurdza/examplemod/item/MaterialShieldItem.java), [item/MaceItem.java](src/main/java/net/amurdza/examplemod/item/MaceItem.java), [enchantment/MaceEnchantment.java](src/main/java/net/amurdza/examplemod/enchantment/MaceEnchantment.java), [event_handlers/MaceCombat.java](src/main/java/net/amurdza/examplemod/event_handlers/MaceCombat.java), [util/NetheriteIronStats.java](src/main/java/net/amurdza/examplemod/util/NetheriteIronStats.java), [standard-equipment stat mixins](src/main/java/net/amurdza/examplemod/mixins/item), [mixins/othermods/wooden_bucket/WoodenBucketMixin.java](src/main/java/net/amurdza/examplemod/mixins/othermods/wooden_bucket/WoodenBucketMixin.java).

## Enchanting, anvils, and stacking

- **Enchanting does not require or consume experience levels.** The completion hook restores the player's level while preserving the enchantment seed, statistics, advancement trigger, and normal completion side effects. The custom table menu and client screen ignore level requirements.
- **Experience-refund caveat:** only the level number is restored. If the player's level was below the selected cost, the normal deduction can clear fractional XP-bar progress and total-experience bookkeeping before the level number is restored; those fields are not refunded by this mixin.
- **Lapis is still required and consumed**, with a different lapis cost for each offer and the strongest offer costing substantially more. Creative players bypass the lapis requirement.
- **A table action applies one disclosed enchantment.** The offer displayed is the one selected for application, rather than a hint to a hidden bundle of enchantments. Selecting an offer rerolls the enchantment seed and rebuilds offers.
- **Previously enchanted gear and enchanted books can return to the table.** Gear can gain another compatible enchantment or upgrade an existing one when the offered level is higher. There is a limit on distinct enchantments for non-book items; existing-enchantment upgrades remain possible at that limit. New enchantments must pass compatibility checks in both directions. Books bypass that distinct-enchantment/compatibility filter and can accumulate enchantments.
- **Bookshelf detection is expanded and simplified.** Both ordinary and chiseled bookshelves count in a box around the table, including shelves above it. The count does not check that chiseled shelves contain books or require the usual clear gap between table and shelf. More shelves are needed for comparable standard enchanting power; enough shelves make the strongest slot offer an enchantment at its own maximum level.
- **Treasure-only status does not keep an enchantment out of this table.** The custom candidate selection does not use the treasure-only restriction; the base enchantment flag is also overridden. Curses are excluded from table offers.
- **Axes can receive Sharpness, Smite, and Bane of Arthropods from the table.** Diversity's Vacuum, Selective Vacuum, Capacity, and Refill are restricted to bundle items in the custom table candidate selection; they are excluded from books by that special case.
- **Anvils have no level cost or pickup level restriction.** Repair/combination/renaming still goes through the anvil's item-processing logic, but the reported level cost is forced to zero and the result can be taken without the usual level check. This does not remove the material input or ordinary anvil wear behavior.
- **Bottle, stew, and soup items stack to a full ordinary stack.** The rule matches any registered item whose path contains `bottle`, `stew`, or `soup`, across all mods. It is a substring rule rather than a hand-maintained item list. For example it includes glass/honey/experience bottles and mushroom/rabbit/beetroot stews or soups, but does not match ordinary `potion` IDs or the mod's `honey_bowl` ID.

Sources: [src/main/java/net/amurdza/examplemod/mixins/monoxide/AnvilAlwaysHasNoCost.java](src/main/java/net/amurdza/examplemod/mixins/monoxide/AnvilAlwaysHasNoCost.java), [BottlesAndStewsStackTo64.java](src/main/java/net/amurdza/examplemod/mixins/monoxide/BottlesAndStewsStackTo64.java), [EnchantmentAlwaysHasNoCost.java](src/main/java/net/amurdza/examplemod/mixins/monoxide/EnchantmentAlwaysHasNoCost.java), [EnchantmentMenu20Lapis.java](src/main/java/net/amurdza/examplemod/mixins/monoxide/EnchantmentMenu20Lapis.java), [TreasureEnchantmentsAreOrdinary.java](src/main/java/net/amurdza/examplemod/mixins/monoxide/TreasureEnchantmentsAreOrdinary.java), and [mixins/client/EnchantmentScreenNoLevelLimit.java](src/main/java/net/amurdza/examplemod/mixins/client/EnchantmentScreenNoLevelLimit.java).

## Food, seeds, and containers

- Adds edible **End Fish, Cooked End Fish, Nether Fish, Cooked Pufferfish, Cooked Tropical Fish, Cooked Squid, and Cooked Glow Squid**. All variants of an End Fish use the same raw/cooked food items; all Nether Fish variants use the same Nether Fish item, including the helper's cooked selection. There is no separately registered cooked Nether Fish item.
- Adds **Melon Pie, Glow Berry Pie, Orange Pie, Pear Pie, Cherry Pie, Banana Pie, and Plum Pie**, all based on pumpkin-pie food properties.
- Adds **Soul Berries**, which are both food and the planting item for Soul Berry Bush; **Soul Berry Cookie**; **Blueberry Cookie**, which has the faster cookie eating behavior; **Seared Cod Roll**; **Nether Fish Roll**; **Glowshroom Stew**; and **Honey Bowl**. Glowshroom Stew and Honey Bowl return bowls when eaten. Honey Bowl is a generic bowl food and does not implement honey bottle's special poison-removal behavior.
- Adds planting-only **Carrot Seeds, Potato Seeds, Onion Seeds, and Rice Seeds** for vanilla carrots/potatoes and Farmer's Delight onion/rice crops. They do not become the canonical pick-block items; picking the crop still uses its original carrot, potato, onion, or rice item. Their acquisition is covered by custom block drops, not recipe files.
- Adds **Bucket of End Fish** using water, **Bucket of Nether Fish** using lava, and **Bucket of Cubozoa** using lava.
- **Popped chorus fruit is edible.** Ordinary chorus fruit has adjusted food values and remains edible when already full. Chorus-fruit teleportation is canceled for crouching players and for every non-player entity; a standing player can still teleport.
- **Ecologics' cooked prickly pear has adjusted hunger/saturation values.** These, popped chorus fruit, and ordinary chorus fruit are the only actual overrides in `ItemFoodProperties`; its comments mentioning other vanilla foods do not implement additional overrides.
- **Bowls harvest honey directly:** right-clicking a full bee nest or beehive with a bowl yields a Honey Bowl and empties the hive's honey level. This custom handler does not invoke the usual bottle/shears harvest bee-anger path.

Sources: [src/main/java/net/amurdza/examplemod/registry/ModItems.java](src/main/java/net/amurdza/examplemod/registry/ModItems.java), [item/CropSeedItem.java](src/main/java/net/amurdza/examplemod/item/CropSeedItem.java), [mixins/food/ItemFoodProperties.java](src/main/java/net/amurdza/examplemod/mixins/food/ItemFoodProperties.java), [event_handlers/PreventTeleportFromChorus.java](src/main/java/net/amurdza/examplemod/event_handlers/PreventTeleportFromChorus.java), [event_handlers/HoneyBowlInteraction.java](src/main/java/net/amurdza/examplemod/event_handlers/HoneyBowlInteraction.java).

## Recipe availability and crafting changes

### Global recipe removal

- On every recipe reload, **all recipe types with IDs outside the `minecraft`, `aoemod`, and `quark` namespaces are removed**. This covers crafting, cooking, smithing, and mod-specific recipe types alike. It is the recipe ID namespace that matters, not which mod owns the output item. Thus Farmer's Delight and other mods' original recipes disappear unless recreated under an allowed namespace; Quark's recipes survive.
- Explicit overrides additionally disable Better Carrots recipes that were incorrectly/atypically published in the `minecraft` namespace: **Cut Carrots, Carrot Cake, Cooked Carrot by furnace/smoker/campfire, Carrot Juice, the old Carrot Soup recipe, Enchanted Golden Carrot, Energy Drink, and Wild Salad**. A further `minecraft:carrot_soup` ID is explicitly disabled. The new soup recipe below is separate.
- Disables vanilla **Netherite Ingot crafting from scrap and gold** and **diamond-to-netherite smithing recipes for sword, shovel, pickaxe, axe, hoe, helmet, chestplate, leggings, and boots**.

Sources: [src/main/java/net/amurdza/examplemod/mixins/recipes/RemoveOtherModRecipes.java](src/main/java/net/amurdza/examplemod/mixins/recipes/RemoveOtherModRecipes.java); [src/main/resources/data/minecraft/recipes/](src/main/resources/data/minecraft/recipes). The Better Carrots result names were verified against its locally cached dependency JAR's [data/minecraft/recipes/](src/main/resources/data/minecraft/recipes) entries.

### Equipment, material, and block recipes

- Adds bone sword/shovel/pickaxe/axe/hoe recipes using **bones and sticks**; bone spear uses the same ingredients in a spear pattern. Bone arrow uses **bone, stick, and feather**. Bone shears' intended recipe does not load (see incomplete resources).
- Adds conventional copper sword/shovel/pickaxe/axe/hoe recipes from **copper ingots and sticks**, and copper helmet/chestplate/leggings/boots recipes from **copper ingots**.
- Adds wooden, stone, copper, iron, golden, and diamond spear recipes from **sticks plus the matching head material**: planks, cobblestone, copper ingot, iron ingot, gold ingot, or diamond. Netherite spear is also **crafted directly from netherite ingot and sticks**, despite its recipe filename ending in `_smithing`.
- Adds wooden/copper/iron/golden/diamond shields crafted from **planks/copper ingots/iron ingots/gold ingots/diamonds**, respectively. Netherite shield still uses **diamond shield + netherite ingot + netherite upgrade template in a smithing table**.
- Mace uses **iron block and breeze rod**.
- Netherite ingot is now obtained by **smelting netherite scrap**. Standard netherite sword/shovel/pickaxe/axe/hoe and helmet/chestplate/leggings/boots are **crafted directly from netherite ingots**, with sticks for weapons/tools, using the conventional equipment shapes.
- Adds **copper alternatives for ordinary rail, detector rail, activator rail, minecart, hopper, piston, heavy weighted pressure plate, and tripwire hook**. They yield the standard vanilla items. Quark's **crafter** also receives a copper-based recipe, conditional on Quark's crafter feature being enabled. These `aoemod` recipes are additions; their presence does not itself delete vanilla/Quark original recipes.
- Adds **Copper Chain from copper ingots**.
- Adds Pale Oak **planks from any pale-oak log/wood variant**, **wood from logs**, **stripped wood from stripped logs**, and the usual **button, pressure plate, slab, stairs, fence, fence gate, door, and trapdoor** recipes from pale-oak planks, with sticks where appropriate.
- Adds **Pale Moss Carpet from Pale Moss Blocks**, **orange dye from Open Eyeblossom**, and **gray dye from Closed Eyeblossom**.
- Adds **tube, brain, bubble, fire, and horn coral block recipes** using matching live coral plants and/or fans. Dead coral forms are not included in their ingredient tags.

Sources: [src/main/resources/data/aoemod/recipes/](src/main/resources/data/aoemod/recipes), particularly [tools/](src/main/resources/data/aoemod/recipes/tools), [redstone/](src/main/resources/data/aoemod/recipes/redstone), [integration/quark/](src/main/resources/data/aoemod/recipes/integration/quark), [coral/](src/main/resources/data/aoemod/recipes/coral), and [smelting/netherite_ingot_from_netherite_scrap.json](src/main/resources/data/aoemod/recipes/smelting/netherite_ingot_from_netherite_scrap.json).

### Food crafting recipes

Ingredient quantities and output counts are intentionally omitted. Recipes are shapeless unless otherwise stated. The table lists ingredient identities because several outputs have surprising ingredients.

| Output | Ingredients / change |
| --- | --- |
| Pumpkin Pie | Pumpkin, wheat, sugar, egg; replaces the vanilla recipe and adds wheat. |
| Beetroot Soup | Beetroot and bowl; replaces vanilla with fewer beetroots. |
| Farmer's Delight Apple Pie | Apple, wheat, sugar, egg. |
| Melon Pie | Melon slice, wheat, sugar, egg. |
| Glow Berry Pie | Glow berries, wheat, sugar, egg. |
| Orange Pie | Fruits Delight orange, wheat, sugar, egg. |
| Pear Pie | Fruits Delight pear, wheat, sugar, egg. |
| Cherry Pie | **Fruits Delight bayberry**, wheat, sugar, egg; the recipe does not use a cherry item. |
| Banana Pie | Any `forge:fruits/banana` item, wheat, sugar, egg. |
| Plum Pie | Fruits Delight mangosteen (renamed Plum), wheat, sugar, egg. |
| Farmer's Delight Chocolate Pie | Cocoa beans, milk bucket, wheat, sugar. |
| Farmer's Delight Sweet Berry Cheesecake | Sweet berries, milk bucket, wheat, sugar. |
| Farmer's Delight Sweet Berry Cookie | Sweet berries and wheat. |
| Blueberry Cookie | Fruits Delight blueberry and wheat. |
| Soul Berry Cookie | Soul berries between wheat in a horizontal shaped recipe. |
| Mushroom Stew | Red mushroom, brown mushroom, bowl; adds an `aoemod` recipe duplicating the familiar ingredients. |
| Rabbit Stew | **Raw rabbit and raw potato**, carrot, any `forge:mushrooms` item, bowl. This is an additional recipe rather than an override of vanilla rabbit stew recipe IDs. |
| Glowshroom Stew | Quark glow shrooms and bowl. |
| Nourished Nether Fungus Stew | Crimson fungus, warped fungus, bowl. |
| Better Carrots Carrot Soup | Carrots and bowl, replacing availability of its removed original recipe. |
| Farmer's Delight Fried Rice | Farmer's Delight rice and onion, egg, carrot; no cooking-pot recipe required. |
| Farmer's Delight Tomato Sauce | Farmer's Delight tomatoes and bowl. |
| Farmer's Delight Salmon Roll (displayed as Tropical Fish Roll) | **Tropical fish and Farmer's Delight rice**. |
| Seared Cod Roll | Nether Depths Upgrade searing cod and warped kelp. |
| Nether Fish Roll | AOEMod Nether Fish and Nether Depths Upgrade warped kelp. |
| Farmer's Delight Cabbage Rolls | Cabbage and a food from `aoemod:food_wrappable`; currently blocked by its invalid dependent cooked-fish tag. |
| Farmer's Delight Dumplings | Cabbage, wheat, onion, and a food from `aoemod:food_wrappable`; same tag issue. |

Sources: [src/main/resources/data/aoemod/recipes/food/](src/main/resources/data/aoemod/recipes/food), [recipes/integration/](src/main/resources/data/aoemod/recipes/integration), and [beetroot soup override](src/main/resources/data/minecraft/recipes/beetroot_soup.json) and [pumpkin pie override](src/main/resources/data/minecraft/recipes/pumpkin_pie.json).

### Furnace, smoker, and campfire foods

All of the following have recipes in **each** of the furnace, smoker, and campfire categories:

- **Pufferfish → Cooked Pufferfish; Tropical Fish → Cooked Tropical Fish; End Fish → Cooked End Fish.**
- **Cultural Delights Squid → AOEMod Cooked Squid; Cultural Delights Glow Squid → AOEMod Cooked Glow Squid.**
- **Alex's Mobs Lobster Tail → Cooked Lobster Tail.**
- **Naturalist Duck → Cooked Duck; Venison → Cooked Venison.**
- **Egg → Farmer's Delight Fried Egg.**
- **Ecologics Prickly Pear → Cooked Prickly Pear.**
- The renamed Drop the Meat products **Raw Arrow Squid → Cooked Arrow Squid** (`raw_wolf`/`cooked_wolf`), **Raw Bison → Cooked Bison** (`bear` IDs), **Raw Boar → Cooked Boar** (`turtle` IDs), **Raw Gazelle → Cooked Gazelle** (`goat` IDs), **Raw Moose → Cooked Moose** (`llama` IDs), **Raw Squirrel → Cooked Squirrel** (`red_meat` IDs), and **Raw Warped Toad → Cooked Warped Toad** (`squid` IDs).

Sources: [src/main/resources/data/aoemod/recipes/smelting/](src/main/resources/data/aoemod/recipes/smelting), [smoking/](src/main/resources/data/aoemod/recipes/smoking), and [campfire_cooking/](src/main/resources/data/aoemod/recipes/campfire_cooking). Netherite scrap smelting is separate and has no smoker/campfire equivalent.

## Food and crafting tag membership

- **Leaf fruits / fruits:** apple; Fruits Delight bayberry, cherry/cherries item IDs when present, orange, pear, mangosteen (Plum), plum/plums item IDs when present; Alex's Mobs banana. `fruits` aliases `leaf_fruits`. These are tree fruits, not a catch-all tag for berries or melon.
- **Raw fish / fish:** cod, salmon, tropical fish, End Fish; optional raw Arrow Squid (`dropthemeat:raw_wolf`), Cultural Delights squid and glow squid, and Alex's Mobs lobster tail. `fish` aliases `raw_fish`. Pufferfish and Nether Fish are absent from this list.
- **Mob raw meat:** beef, porkchop, mutton, chicken, rabbit; optional Naturalist duck and venison; Drop the Meat raw Bison, Gazelle, Moose, Squirrel, Boar, and Warped Toad. Raw Arrow Squid is in the fish tag instead.
- **Mob cooked meat:** the cooked equivalents of the raw meat list, plus Cooked Arrow Squid, AOEMod Cooked Squid/Cooked Glow Squid, and Alex's Mobs Cooked Lobster Tail.
- **Cooked fish intent:** cooked cod, cooked salmon, Cooked Tropical Fish, Cooked Pufferfish, Cooked End Fish, Cooked Squid, Cooked Glow Squid, Cooked Arrow Squid, and Cooked Lobster Tail. **The actual tag is invalid** because it spells the required End Fish item `aoemod:cooked_endfish` instead of the registered `aoemod:cooked_end_fish`. Consequently its dependent `food_wrappable` union of cooked meat + cooked fish is not usable, blocking the cabbage-roll/dumpling ingredient defined above until repaired.
- **Vegetables tag contains only potato.** It is not an automatic group of all vegetable foods.
- **Mushrooms tag contains red mushroom, brown mushroom, and Quark glow shroom.** This is `aoemod:mushrooms`, distinct from the Forge mushroom tag used by the added rabbit-stew recipe.
- **Small flowers item tag:** poppy, dandelion, azure bluet, cornflower, oxeye daisy, allium, blue orchid, lily of the valley, red/white/orange/pink tulips, pink petals, and torchflower.
- Adds mangosteen (Plum) to `forge:fruits/plum`.
- Adds copper chains to Forge's block and item **chains** tags alongside vanilla chain; bone shears to Forge's **shears** item tag alongside vanilla shears; wooden/stone/copper/iron/golden/diamond/netherite spears to Minecraft's **spears** item tag. **Bone spear is not included** in that spear tag.
- **Restricts common Forge ingredient categories to vanilla items** using `replace: true`: copper/iron/gold/netherite ingots; diamond and amethyst gems; wooden rods (sticks only); string (vanilla string only); wooden chests (chest and trapped chest only); normal cobblestone and deepslate cobblestone. Other mods' substitutes are removed from these specific tags when this pack has priority.

Sources: [src/main/resources/data/aoemod/tags/items/](src/main/resources/data/aoemod/tags/items), [data/forge/tags/items/](src/main/resources/data/forge/tags/items), [data/forge/tags/blocks/chains.json](src/main/resources/data/forge/tags/blocks/chains.json), and [data/minecraft/tags/items/spears.json](src/main/resources/data/minecraft/tags/items/spears.json).

## Animals, breeding, and taming

### Biome-dependent maturation and breeding

The current biome changes the natural baby-aging rate and breeding-cooldown recovery of ageable mobs. Feeding babies also uses the biome's growth multiplier. Breeding events can fail to produce a child or produce extra children, according to the species and the breeding parent's current biome. Moving an animal therefore changes these ongoing rates; these are not permanent birth-biome traits.

The following table lists the configured nonzero biomes. In other biomes, a configured species has no natural aging/cooldown progress and no successful offspring through the modified breeding-event path. Relative rates differ between the listed biomes even though numerical values are omitted here.

| Species | Biomes with aging/cooldown progress | Biomes with a positive offspring multiplier |
|---|---|---|
| Cow, pig, sheep | Savanna, plains | Savanna, plains |
| Mooshroom, goat, polar bear | Mountains | Mountains |
| Chicken | Tropical, savanna, plains | Tropical, savanna, plains |
| Rabbit | Tropical, savanna, plains, mountains, desert | Same |
| Hoglin | Crimson forest | Crimson forest |
| Strider | Soul sand valley | Soul sand valley |
| Fox | Tropical, savanna, mountains | Same |
| Horse | Tropical, savanna, plains | Same |
| Donkey | Plains | Plains |
| Mule | Plains | No offspring multiplier configured; this does not make mules breedable |
| Camel | Desert | Desert |
| Bee | Tropical, savanna, plains | Same |
| Llama and trader llama | Savanna | Savanna |
| Wolf | Tropical, savanna, plains, mountains | Same |
| Cat | Tropical, savanna | Same |
| Ocelot, panda, turtle, frog, axolotl, parrot, sniffer, gorilla, nautilus | Tropical | Tropical |
| Dolphin | Tropical | A tropical multiplier is configured, but its separate breeding code does not use that multiplier; see below |
| Villager | Tropical, savanna, plains, mountains, desert, deep dark, soul sand valley, warped forest, crimson forest, basalt deltas | The same biomes are configured; the offspring multiplier only applies to offspring created through the Forge breeding event |
| Emu, bison, gazelle, boar | Savanna | Savanna |
| Moose | Savanna, mountains | Same |
| Warped toad | Warped forest | Soul sand valley, warped forest |
| Squirrel, deer | Tropical, savanna | Same |

- Species eligible for extra children receive additional freshly created babies beside the parent. Turtle and frog events can fail, but the handler does not create additional children for them.
- Where a same-species child and both parents expose compatible variant getters/setters, the child usually inherits a parent's variant, with a smaller chance of another variant. This is reflective compatibility logic, so it is not a blanket guarantee for every modded color system.
- Food interactions through the base animal handler fail when both growth and fertility are nonpositive. Extra-food interactions have a separate event handler, so the restriction is not consistently applied to every added food.
- Animals and dolphins in love stop following a player who is holding temptation food, allowing breeding to take priority.
- A tadpole aging row exists, but the age-change mixin targets `AgeableMob`; it does not itself change the separate tadpole age system.

Sources: [MobConfig](src/main/java/net/amurdza/examplemod/config/MobConfig.java), [AnimalAgingRate](src/main/java/net/amurdza/examplemod/mixins/mob_behavior/AnimalAgingRate.java), [AnimalFoodGrowth](src/main/java/net/amurdza/examplemod/mixins/mob_behavior/AnimalFoodGrowth.java), [AnimalFeedingBlocked](src/main/java/net/amurdza/examplemod/mixins/mob_behavior/AnimalFeedingBlocked.java), [TwinsAndFailedBreeding](src/main/java/net/amurdza/examplemod/event_handlers/TwinsAndFailedBreeding.java), [TemptGoalStopsForBreeding](src/main/java/net/amurdza/examplemod/mixins/mob_behavior/TemptGoalStopsForBreeding.java).

### Feeding and following food

The tag-based food groups in this table refer to the mod's actual item tags, not arbitrary edible items. Tameable animals normally must be tamed before the added food puts them in love; horses also require taming. Babies can consume the added foods for growth.

| Animal | Added or replacement food behavior |
|---|---|
| Pig, goat, moose, deer, boar | Their configured feeding list is wheat, sweet berries, potato, carrot, beetroot, Farmer's Delight onion, Fruit's Delight blueberry, and the `aoemod:fruits` group. Existing base-animal food outside this list is rejected. Pig/goat/moose/boar temptation goals are replaced with this list; deer retain their earlier temptation behavior and additionally follow wheat. |
| Rabbit | Accepts the same herbivore list, plus the mod's small-flower group. Adds matching temptation goals. Existing rabbit foods remain available. |
| Fox | Adds raw chicken, raw rabbit, the raw-fish group, and blueberry as foods, and follows these foods plus sweet berries. |
| Horse, donkey, llama | Can enter love mode through wheat; horse/chested-horse/llama AI follows wheat. Llamas specifically reject their former base-animal foods outside wheat. |
| Camel | Prickly pear replaces its configured breeding/temptation food. |
| Warped toad | Warped fungus replaces its configured breeding/temptation food. |
| Frog, turtle, axolotl | Seagrass and kelp replace their configured breeding/temptation foods. |
| Cat, ocelot | The full raw-fish tag is added, rather than limiting the extra handler to cod and salmon. They follow the raw-fish group. |
| Mooshroom | Mushroom-tag items are added for feeding; mushroom temptation is prioritized ahead of wheat. |
| Chicken, squirrel | Adds Farmer's Delight cabbage and tomato seeds alongside wheat, beetroot, melon, and pumpkin seeds for feeding and following. |
| Gorilla | Leaf-fruit-tag items replace its configured breeding/temptation foods. |
| Parrot | Tamed parrots can breed with leaf-fruit-tag items. They also follow the six-seed list above, with seed temptation prioritized ahead of fruit temptation. |
| Polar bear | Raw fish tames an untamed bear; a tamed bear accepts the mob-meat-drop tag for breeding. Bears follow raw fish and mob-meat items. |
| Dolphin | Fish-tag items initiate breeding, tempt adults, and accelerate baby growth. |
| Nautilus | Pufferfish can tame an adult. Tamed adults and babies accept fish-tag food for breeding/growth. |

Sources: [BreedingItems](src/main/java/net/amurdza/examplemod/event_handlers/BreedingItems.java), [AnimalFoodOverrides](src/main/java/net/amurdza/examplemod/util/AnimalFoodOverrides.java), [MobAI](src/main/java/net/amurdza/examplemod/event_handlers/MobAI.java), [food tags](src/main/resources/data/aoemod/tags/items).

### New tameable/breedable behavior and animal handling

- **Parrots:** breed and have actual baby age state. Babies cannot sit on shoulders. Offspring inherit the initiating tamed parent's owner, subject to the normal breeding-event variant handling. Baby parrots have a separate custom rendered model.
- **Dolphins:** gain saved baby age, breeding cooldown, love state, baby-sized body/model, feeding particles, partner-seeking behavior, and breeding advancement/stat credit. A successful mating creates one baby and can award experience. Unlike the general animal system, dolphin mating does not consult the configured fertility/extra-child multiplier; growth and cooldown still use the biome multiplier.
- **Polar bears:** raw fish tames them without a random failure roll, unless another mod cancels the tame event. Only the owner can mount an adult with an empty hand while not using secondary interaction; no saddle is needed. The owner controls movement and facing. Offspring inherit an available parent's owner. Normal anger, standing threats, and normal targeting are cleared. Owner-defense/owner-assist attack goals are added, with wolf-like damage, and a ridden bear only attacks targets already within reach. The attack rules exclude creepers, ghasts, friendly players, tamed horses, tamed animals, and the owner's allies. There is an interaction conflict with the global target filter described in the caveats below.
- **Rabbits:** have greater minimum jump strength and can step onto taller obstacles. Their special avoidance and garden-raiding goals are removed.
- **Deer:** use a lower step height and lose Naturalist's group-alert panic goal.
- **Mooshrooms:** ordinary shears interaction is canceled, preventing conversion into a cow through that interaction.
- **Foxes:** cannot pick up carried items and have their held item cleared at spawn.
- **Goats:** lose their long-jump activity and ram activity.
- **Hoglins:** their brain treats them as pacified and will not start retaliation. Piglin hoglin-hunting behavior is disabled.
- **Striders:** the warmth check is forced true, removing normal shivering caused by the block-tag test.
- **Pufferfish:** no longer sting nearby mobs through their proximity tick, and contact with a player fails its damage call, preventing the associated sting/poison path.
- **Bees:** breaking a hive uses a positive enchantment-check result for the anger check, and hive harvesting always treats the hive as smoked. This suppresses those hive-triggered anger paths.
- **Turtles:** pregnant turtles periodically reset their home to the nearest suitable exposed sand with room for eggs, instead of insisting on the original home beach. Their long-distance travel depth check is based on water above them rather than the dimension's nominal sea level. Zombies lose turtle-egg attack goals; zombie/skeleton turtle-target goals are removed. Mobs using the turtle-egg griefing check cannot break eggs through that check.

Sources: [animal behavior mixins](src/main/java/net/amurdza/examplemod/mixins/mob_behavior), [AI-removal mixins](src/main/java/net/amurdza/examplemod/mixins/remove_ai), [breeding mixins](src/main/java/net/amurdza/examplemod/mixins/add_ai), [dolphin breeding AI](src/main/java/net/amurdza/examplemod/entity/ai/DolphinBreedGoal.java), [MobAI](src/main/java/net/amurdza/examplemod/event_handlers/MobAI.java), [NoMooshroomShearing](src/main/java/net/amurdza/examplemod/event_handlers/NoMooshroomShearing.java).

### Products and animal appearance/stats

- Adult **chickens**, **parrots**, and **emus** shed feathers periodically; emus shed emu feathers. The timers follow each species' biome growth rate. Baby birds do not shed, and chicken jockeys do not shed.
- Adult **frogs** periodically shed slimeballs in tropical biomes, based on the configured frog slimeball-drop amount.
- **Chicken and emu egg laying** accelerates in favorable biomes using the growth multiplier. These edits only subtract extra timer ticks; they do not stop normal egg laying when the configured multiplier is zero.
- **Sheep shearing yield** uses the first matching wool-drop configuration entry and preserves the sheep's color. That first entry is plains-only; the separate savanna wool entry affects death drops but never increases shearing yield. The implementation still gives at least the baseline single wool even where the amount read is zero.
- **Sheep eating animation** counts down faster according to the sheep biome-growth multiplier. Despite its class name, this mixin changes the client eating-animation timer rather than the grass-eating goal or actual wool-regrowth frequency. Source: [SheepRegrowWoolRate](src/main/java/net/amurdza/examplemod/mixins/growth_rate/SheepRegrowWoolRate.java).
- **Axolotls and frogs** receive a random variant when first processed after joining a world, unless tagged as having an inherited parent variant. Axolotl selection includes all axolotl variants; frog selection includes cold, temperate, and warm variants.
- **Horse:** maximum health, movement speed, and jump strength are set from its biome when first processed. Savanna values are strongest overall; tropical values are also enhanced; plains has its own speed/jump settings; the remaining configured biome groups use minimum settings.
- **Donkey:** maximum health, movement speed, and jump strength are similarly set from biome. Tropical/savanna have enhanced health/speed/jump and plains has enhanced health/jump.
- **Llama/trader llama:** carrying strength is set to the maximum fixed strength. Maximum health and speed depend on biome, with tropical/savanna enhancements.
- **Mule:** receives fixed minimum health, movement speed, and jump strength throughout the configured biome groups; its table does not actually vary those values between those groups.
- These mount attributes are set once per mob and saved with a processed tag; walking to another biome does not continuously retune them.
- **Alex's Mobs lobsters:** natural spawning selects only the default variant outside mountain biomes; mountain biomes can select the first two variants.
- **Snow foxes and white rabbits:** the mod's grove biome is added to their vanilla variant tags. **Gold rabbits:** the mod's desert, deep dark, soul sand valley, warped forest, crimson forest, and basalt deltas biomes are added to their vanilla variant tag.

Sources: [MobCharacteristics](src/main/java/net/amurdza/examplemod/event_handlers/MobCharacteristics.java), [MobStatsConfig](src/main/java/net/amurdza/examplemod/config/MobStatsConfig.java), [MobConfig](src/main/java/net/amurdza/examplemod/config/MobConfig.java), [animal behavior mixins](src/main/java/net/amurdza/examplemod/mixins/mob_behavior), [Alex's Mobs mixins](src/main/java/net/amurdza/examplemod/mixins/othermods/alexsmobs), [vanilla biome variant tags](src/main/resources/data/minecraft/tags/worldgen/biome).

## Combat, hostile mobs, and mob interaction rules

### Rules affecting many mobs

- The global target/direct-attack filter rejects targets and ordinary `Mob.doHurtTarget` attacks when **both participants are ordinary mobs**. Exceptions are players, villagers, piglins, piglin brutes, tamed `TamableAnimal` mobs, and tamed horses: having one of these on either side permits the target through this filter. This stops much ordinary mob-on-mob predation and fighting without making all mobs passive to players; it is not universal immunity to incidental projectile, explosion, or custom damage paths.
- The same participant rule suppresses `AvoidEntityGoal` fleeing and clears forbidden attack, revenge, anger, and avoidance memories in brain-based mobs. Wardens have corresponding anger cleared for forbidden targets.
- Animals, water animals, and Ice and Fire sirens have the exact standard hunting, retaliation, avoidance, panic, and leap goal classes removed, together with specified Alex's Mobs prey/flee/herd panic/item-target/turtle-egg goals and Quark's rave goal. Goal subclasses are not automatically removed by this exact-class removal.
- **Moose** additionally lose their jostling and ordinary melee-attack goals.
- Forge's mob-griefing query returns false for mobs other than **villagers**. Actions that use this query, including many mob block changes and item collection behaviors, are suppressed; villagers retain the ordinary query behavior. This is broader than explosion protection alone.
- **Zombified piglins** gain an ordinary nearest-player hostile target goal.
- **Zoglins** clear disallowed targets and combat memories and use calm idle sounds when they have no allowed target.

Sources: [MobAI](src/main/java/net/amurdza/examplemod/event_handlers/MobAI.java), [MobTargetFilterMixin](src/main/java/net/amurdza/examplemod/mixins/remove_ai/MobTargetFilterMixin.java), [AvoidEntityGoalFilterMixin](src/main/java/net/amurdza/examplemod/mixins/remove_ai/AvoidEntityGoalFilterMixin.java), [MobGriefingOnlyVillagers](src/main/java/net/amurdza/examplemod/mixins/mob_behavior/MobGriefingOnlyVillagers.java), [ZoglinQuietWhenIdle](src/main/java/net/amurdza/examplemod/mixins/mob_behavior/ZoglinQuietWhenIdle.java).

### Stat and equipment overhaul

The server assigns custom maximum health, melee attack, speed, armor, and knockback resistance where those attributes exist for these specific mobs:

- **Vanilla:** spider, cave spider, zombie, skeleton, pillager, ravager, zombie villager, witch, evoker, vex, vindicator, stray, drowned, husk, enderman, shulker, warden, ghast, zoglin, zombified piglin, wither skeleton, magma cube, blaze.
- **Born in Chaos:** Senor Pumpkin, Sir Pumpkinhead, Lord Pumpkinhead.
- **Ice and Fire:** Dread Thrall, Dread Lich, Dread Knight, troll.
- **Alex's Mobs:** centipede head.
- **Quark:** wraith.
- **This mod:** Creaking, Bogged, Spider Queen, zombie/skeleton horseman mounts, Illager King, Breeze, Archlich, Parched, Camel Husk Jockey; tagged zombie/skeleton horseman riders also have separate stat definitions.

Additional conditions:

- **Skeletons** have a stronger separate profile in soul sand valley biomes.
- **Creepers** get a special profile only in warped forest biomes.
- **Ordinary slimes** get a size-dependent profile only in tropical biomes; the smallest has no assigned melee damage.
- **Magma cubes** get size-dependent health/damage/speed/armor; speed and armor growth are capped by size.
- **Wither** maximum health is explicitly assigned.
- **Zombie/skeleton horseman mounts** also receive a fixed jump strength.
- Health/stat tuning runs once per saved mob. The equipment handler runs on each world join, so its assigned equipment can replace later equipment when a mob reloads.

| Mob | Assigned equipment |
|---|---|
| Sir Pumpkinhead, Lord Pumpkinhead | Iron sword |
| Tagged zombie horseman rider | Iron spear and full leather armor |
| Tagged skeleton horseman rider | Power-enchanted bow and full leather armor |
| Drowned | Copper sword and full copper armor |
| Dread Thrall | All equipment cleared |
| Zombified piglin | Golden sword |
| Wither skeleton | Iron sword |

The assigned equipment has its normal equipment-drop chance disabled. The rider-tag timing caveat below affects the initial rider loadout.

Source: [MobTierBalancing](src/main/java/net/amurdza/examplemod/event_handlers/MobTierBalancing.java).

### Specific hostile mob behavior

- **Husks:** successful direct melee hits add Weakness, in addition to any behavior inherited from vanilla.
- **Drowned:** successful direct melee hits add Slowness.
- **Wither skeletons:** successful direct melee hits explicitly add Wither.
- **Quark wraiths:** successful direct melee hits add Weakness and Slowness.
- **Endermen:** their normal random, toward-target, and coordinate teleport attempts return false.
- **Wardens:** cannot start sonic-boom attacks. Idle wardens periodically acquire a nearby attackable player and increase anger, making them pursue players more like conventional hostile mobs.
- **Ravagers:** their shield-block response is canceled and their special immobility check always returns false, suppressing the associated shield/stun response.
- **Shulkers:** have a custom closed-shell armor bonus, cannot duplicate through shulker-bullet hits, and can despawn when far from players.
- **Guardians:** the ordinary target selector no longer includes squid. Although it still lists axolotls, the broader ordinary-mob target filter can reject guardian-versus-axolotl targeting.
- **Wither:** periodically summons a capped nearby contingent of wither skeletons while fighting and not in its invulnerable spawn phase. Its skulls do not directly hit `Monster` entities, their explosions exclude `Monster` entities, and skull explosions do not damage blocks. A non-monster mob killed by a direct skull hit from a Wither is replaced with a newly spawned wither skeleton.
- **Ordinary Dread Liches:** summon Dread Thralls instead of choosing among their original minion types.
- **Ice and Fire trolls:** always use the axe weapon type, do not turn to stone through the normal sky-visibility test, and clear their attack target while exposed to bright daytime sky.
- **Sir Pumpkinhead:** its legacy form-change and summon procedures are canceled; it remains in its original form.
- **Lord Pumpkinhead:** legacy arena-block, transformation, and damage-triggered summon procedures are canceled. It is equipped with an iron sword, is mounted on a persistent tamed skeleton horse, and periodically summons capped groups of Sir Pumpkinhead/Senor Pumpkin while it has a live target. If dismounted, its tick logic attempts to create another mount.
- **Born in Chaos zombie maggots:** the zombie-maggot spawning procedure is canceled entirely.

Sources: [MobTierBalancing](src/main/java/net/amurdza/examplemod/event_handlers/MobTierBalancing.java), [hostile behavior mixins](src/main/java/net/amurdza/examplemod/mixins/mob_behavior), [despawning mixins](src/main/java/net/amurdza/examplemod/mixins/despawning), [Born in Chaos mixins](src/main/java/net/amurdza/examplemod/mixins/othermods/born_in_chaos), [Ice and Fire mixins](src/main/java/net/amurdza/examplemod/mixins/othermods/iceandfire).

## Added mobs

These entities and their spawn eggs are registered; their existence should not be confused with a guarantee that all of them naturally spawn in every world.

| Added mob | Implemented behavior |
|---|---|
| **Bogged** | Skeleton-style ranged enemy whose arrows apply Poison. Uses stray-like sounds. |
| **Parched** | Skeleton-style ranged enemy whose arrows apply Weakness; immune to Weakness itself. |
| **Breeze** | Ranged enemy that shoots wind charges and periodically jumps toward its target; immune to fall damage. Its wind charges directly damage a hit entity, push nearby living entities/other alive entities away and upward, and explode without block destruction. |
| **Creaking** | Conventional damageable hostile melee mob that targets players. It has no heart connection, gaze-freezing mechanic, or heart-based invulnerability implemented. |
| **Camel Husk** | Undead camel with husk sounds, distance despawning, and no food/mating/offspring support in its own entity class. |
| **Camel Husk Jockey** | Camel Husk that spawns a husk carrying a golden spear and a Parched as passengers. |
| **Zombie Horseman** | Zombie-horse mount that spawns a zombie rider carrying an iron spear. Separate mount/rider balance rules are configured. |
| **Skeleton Horseman** | Skeleton-horse mount that spawns a skeleton rider carrying a bow. Separate mount/rider balance rules are configured. |
| **Nautilus** | Aquatic tameable/breedable mount. Adult taming uses pufferfish with a chance of success; breeding/growth food uses the fish tag. Any player can ride an unoccupied adult with an empty hand, even if it is untamed or owned by someone else. Rider facing controls swimming direction, including pitch-driven vertical movement; the rider receives Water Breathing. It dries out on land, cannot be leashed, ignores fluid pushing, and offspring inherit the initiating tamed parent's owner. |
| **End Fish** | Water-schooling fish with multiple colored variants and size variation, captured in a matching bucket that retains variant/size. Mountain spawning selects blue/normal variants; desert selects its yellow/sulphur variant; tropical and fallback biomes select the other sulphur variants. All End Fish variants drop the same End Fish food item, cooked when burning. |
| **Nether Fish** | Lava-schooling fish with two natural variants and size variation, and a matching variant/size-preserving bucket. All variants drop the same Nether Fish food item, including when burning; no separate cooked Nether Fish is registered. |
| **Cubozoa** | Lava-schooling jellyfish-like mob with size variation and a variant/size-preserving bucket. Crimson forest and basalt deltas choose its alternate variant; other biomes use the default variant. |
| **Sea Serpent** | Large multipart aquatic creature with custom swimming, segmented motion/hitboxes, reduced arrow damage, reduced damage passed through body parts, and resistance to knockback in water. It ignores block-sticking slowdown and has no attack/taming/breeding behavior implemented in this class; it wanders through water. |
| **Spider Queen** | Large spider boss with a boss bar and no distance despawning. Successful melee hits add Weakness, Poison, and Slowness. During combat it periodically summons capped groups of spiders and cave spiders targeting its current enemy. |
| **Archlich** | Dread-Lich-derived enemy with custom model/animations. Fires Dread Lich skulls and periodically summons a capped number of Dread Liches or Dread Knights. Summoned Dread Liches are initialized with their summon counter already filled to prevent another layer of summons. |
| **Illager King** (`aoemod:illager_lord`) | Vindicator-derived boss with a boss bar, iron axe, full iron armor, and Boss Shield. Its shield blocks frontal damage except damage bypassing invulnerability; repeated blocked hits break it permanently. It summons capped groups of vindicators and evokers during combat and does not despawn with distance. Its entity class tries to drop an unbroken shield on death, but the general drop replacement subsequently removes it (see caveats). |

Sources: [ModEntities](src/main/java/net/amurdza/examplemod/registry/ModEntities.java), [entity classes](src/main/java/net/amurdza/examplemod/entity), [future mob classes](src/main/java/net/amurdza/examplemod/entity/future), [SeaSerpentEntity](src/main/java/net/amurdza/examplemod/entity/sea_serpent/SeaSerpentEntity.java).

## Mob drops

### Replacement rules

- For a mob with at least one configured drop entry, its original death-drop collection is replaced, **even if the current biome makes all configured drop amounts zero**. This can remove ordinary equipment and rare drops as well as normal loot.
- The preservation whitelist is music discs, skeleton skulls, zombie heads, creeper heads, piglin heads, player heads, dragon heads, and the dragon egg. **Wither skeleton skulls are not on that whitelist.**
- Configured drops are based on the mob's biome at death. Fractional amounts are treated as chances. There is no requirement in this handler that a player killed the mob.
- Where a raw/cooked pair is configured, dying while on fire selects the cooked entry. For several undead mobs that pair is **rotten flesh → leather**, so fire changes the resource rather than merely cooking meat.
- Sheep's white-wool entries become wool matching the sheep's color.
- Armor on these affected mobs is additionally converted to material compensation: leather has a chance to return leather; iron/chain return iron nuggets/ingots; gold returns gold nuggets/ingots; diamond/netherite return their material. This compensation's amount follows biome, with no metal compensation in tropical/savanna, and no custom copper-armor conversion branch.
- The code records a flag describing which drops should be affected by Looting, but the death-drop handler never reads that flag or the Looting level. The replacement drops currently do not gain a Looting bonus through this system.

### Animals and aquatic mobs

Raw meat/fish below uses its cooked equivalent when on fire unless stated otherwise. For repurposed Drop the Meat items, the displayed mob-meat name is used, with unusual backing items noted.

| Mob | Replacement drop items | Biomes with those drops |
|---|---|---|
| Cow | Beef, leather | Plains, savanna |
| Mooshroom | Beef, leather | Mountains |
| Pig | Pork | Plains, savanna; bones additionally in savanna |
| Sheep | Mutton, color-matched wool, bone | Mutton only in plains (two separate entries); wool/bone in plains and savanna |
| Chicken | Chicken meat, feather, bone | Meat only in plains (two separate entries); feather in plains/savanna/tropical; bone in savanna/tropical |
| Goat | Mutton, white wool, bone | Mountains; its meat entry remains raw mutton even when burning |
| Rabbit | Rabbit meat, rabbit hide, rabbit foot | Tropical, savanna, plains, mountains, desert; bones only tropical/savanna |
| Hoglin | Pork, leather | Crimson forest |
| Strider | Mutton, string | Soul sand valley |
| Cod | Cod, bone meal | Tropical, savanna, plains, mountains |
| Salmon | Salmon, bone meal | Tropical, savanna, plains, mountains |
| Tropical fish | Tropical fish, bone meal | Tropical, savanna; the fish entry stays raw on fire |
| Pufferfish | Pufferfish/cooked pufferfish, bone meal | Tropical, savanna |
| Squid | Squid/cooked squid, ink sac, bone meal | Tropical, savanna, plains, mountains |
| Glow squid | Glow squid/cooked glow squid, glow ink sac, bone meal | Mountains, deep dark |
| End Fish, Nether Fish | End Fish/Cooked End Fish, or the shared Nether Fish item regardless of variant | Special handler guarantees fish at death in every biome; tropical/mountain amounts are higher. It clears all prior drops, and does not add the End Fish config's bone meal. |
| Cubozoa | Slimeball | Soul sand valley, warped forest, crimson forest, basalt deltas |
| Emu | Duck meat, emu feather, bone | Savanna |
| Bison | Bison meat (`dropthemeat:raw_bear`), brown wool, bone | Savanna |
| Gazelle | Gazelle meat (`dropthemeat:raw_goat`), leather, bone | Savanna |
| Moose | Moose meat (`dropthemeat:raw_llama`), leather, bone | Savanna, mountains |
| Warped toad | Warped-toad meat (`dropthemeat:raw_squid`), shroomlight, bone | Meat/shroomlight in warped forest; bone in plains and warped forest |
| Lobster | Lobster tail, bone meal | Tropical, savanna, plains, mountains |
| Squirrel | Squirrel meat (`dropthemeat:raw_red_meat`), rabbit hide, bone | Tropical, savanna |
| Deer | Venison, leather, bone | Tropical, savanna |
| Boar | Boar meat (`dropthemeat:raw_turtle`), bone | Savanna |
| Arrow squid | Arrow-squid meat (`dropthemeat:raw_wolf`), ink sac, bone meal | Tropical, savanna, mountains |
| Frog | Slimeball | Tropical |

### Hostile mobs and bosses

For compactness, **the six dry/underground/Nether groups** below means **desert, deep dark, soul sand valley, warped forest, crimson forest, and basalt deltas**. Where a row says only some of these, the others have zero configured drops.

| Mob | Replacement drop items | Biomes with those drops |
|---|---|---|
| Zombie | Rotten flesh → leather on fire; bone | Savanna |
| Zombie villager | Rotten flesh → leather on fire; bone, gold ingot | Plains |
| Husk | Rotten flesh → leather on fire; bone, gold ingot | All six groups |
| Drowned | Rotten flesh → leather on fire; seagrass, kelp, bone | Mountains |
| Zombified piglin | Rotten flesh, bone; gold nugget | Flesh/bone in desert/deep dark/soul sand valley/warped forest/crimson forest; gold in soul sand valley/warped forest/crimson forest |
| Zoglin | Rotten flesh, bone | Desert, deep dark, soul sand valley, warped forest, crimson forest |
| Zombie horse | Rotten flesh → leather on fire; bone | Savanna |
| Skeleton | Arrow, bone, bow | Savanna |
| Stray | Bone | Mountains |
| Breeze | Breeze rod, blue ice, snowball | Mountains |
| Wither skeleton | Coal, bone | All six groups; its normal skull drop is removed by replacement |
| Skeleton horse | Bone | Savanna |
| Spider | String, spider eye | Tropical |
| Cave spider | String, spider eye, emerald | Tropical |
| Bogged | Arrow, bow, emerald | Tropical |
| Spider Queen | White wool, spider eye, bone, emerald | Tropical |
| Centipede head | Bone, emerald | Tropical |
| Parched | Bone, sand, red sand | All six groups |
| Camel Husk | Bone, rotten flesh | All six groups |
| Camel Husk Jockey | Bone, rotten flesh, emerald | All six groups |
| Creaking | Oak/mangrove/cherry/dark-oak/jungle/azalea logs; oak/cherry/dark-oak/jungle/azalea saplings and mangrove propagules | Tropical |
| Zombie Horseman mount | Rotten flesh → leather on fire; bone, emerald | Savanna |
| Skeleton Horseman mount | Bow, arrow, bone, emerald | Savanna |
| Archlich | Bone, diamond, lapis lazuli, emerald | Mountains |
| Illager King | Iron ingot, emerald | Plains |
| Creeper | Gunpowder | All six groups |
| Slime | Slimeball | Tropical |
| Magma cube | Magma cream | Soul sand valley, warped forest, crimson forest, basalt deltas |
| Blaze | Glowstone dust, blaze rod | Soul sand valley, warped forest, crimson forest, basalt deltas |
| Ghast | Ghast tear, gunpowder | Deep dark, soul sand valley, warped forest, crimson forest, basalt deltas |
| Enderman | Ender pearl | All six groups |
| Shulker | Shulker shell, purple terracotta | All six groups |
| Pillager | Crossbow, arrow | Plains |
| Vindicator | Iron ingot, emerald | Plains |
| Evoker | Emerald | Plains; its ordinary totem drop is not retained |
| Illusioner | Emerald, bow, arrow | Plains |
| Ravager | Leather, saddle, bone | Plains |
| Witch | Sugar, red mushroom, brown mushroom, stick | Plains |
| Guardian, elder guardian | Prismarine shard, prismarine crystals | Mountains; other normal drops are not retained |
| Phantom | Phantom membrane, bone | All six groups |
| Warden | Sculk, sculk catalyst, sculk sensor | All six groups |
| Wither | Soul sand, Nether Star, wither skeleton skull, emerald | All six groups |
| Giant | Rotten flesh → leather on fire; bone | Flesh in savanna/plains/mountains/desert/deep dark; bone in plains/mountains/desert/deep dark |
| Senor Pumpkin | Pumpkin slice, bone | Savanna |
| Sir Pumpkinhead, Lord Pumpkinhead | Pumpkin, bone, emerald | Savanna |
| Wraith | Ghast tear | All six groups |
| Troll | Iron ingot, spruce log, bone, emerald | Mountains |
| Dread Knight | Iron ingot, bone, emerald | Mountains |
| Dread Lich | Diamond, bone, emerald | Mountains |

Mobs registered in `MobConfig` with **no item entries** do not have their death drops replaced by this handler; a registration alone is not a removal of all loot. This includes examples such as fox, horse, villager, piglin, allay, bat, silverfish, endermite, piglin brute, Ender Dragon, and vex.

Sources: [MobConfig](src/main/java/net/amurdza/examplemod/config/MobConfig.java), [CustomMobDrops](src/main/java/net/amurdza/examplemod/event_handlers/CustomMobDrops.java), [displayed food names](src/main/resources/assets/aoemod/lang/en_us.json).

## Runtime mob spawning and despawning

These are checks performed when mobs try to spawn, rather than terrain/structure/feature generation settings.

- **Hoglins, striders, mooshrooms, and warped toads** are assigned the creature spawning category.
- **Mooshrooms** bypass the normal brightness requirement in their mushroom spawn predicate.
- **Striders:** an early mixin registers soul-sediment ground spawning, but the later Forge spawn-placement replacement uses **nylium or basalt-stone-tag blocks**. That later replacement is the effective registered ground predicate.
- **Sniffers:** receive ordinary ground animal spawning rules.
- **Camels:** require sand, grass block, or moss beneath them and sufficient brightness.
- **Turtles:** require bright sand, with an additional repeating restriction to selected X-direction chunk columns.
- **Dread Knights, Dread Thralls, and gorgons:** receive ground placement with monster spawning rules.
- **Drowned, slimes, husks, strays, pillagers, zoglins, and piglin brutes:** use a ground-placement monster predicate requiring non-Peaceful difficulty, darkness, and the common mob ground predicate. In particular, the Drowned registration replaces its normal water-spawn placement.
- **Axolotls:** use in-water placement with the mod's local water-column helper instead of a fixed world sea level. The helper is intended to select a shallow band, but only counts a particular falling-water state (see caveat).
- **Glow squid:** use in-water placement and require water above and beneath them; the replacement removes the original depth/darkness predicate.
- **Shulkers:** require adjacent terracotta, darkness, and non-Peaceful difficulty, with ground placement.
- **Guardians:** their predicate checks non-Peaceful difficulty and water at/below the spawn point (with a spawner exception at the spawn point), without its old sea-level/random/sky tests.
- **Surface-water animals using vanilla's shared water-animal predicate:** use the mod's local water-column helper rather than absolute world sea level, with the falling-water limitation below. The same change is inherited by the mod's End Fish and Sea Serpent placement checks.
- **Drowned beach-seeking:** uses the local water-column helper instead of nominal sea level. Its old deep-water spawning helper is also patched, although the later replacement registration no longer uses the original Drowned spawn predicate.
- **Nether Depths Upgrade lava animals:** their shared surface-lava spawning predicate accepts any height, provided lava is below and a lava block is above; spawners bypass those fluid checks.
- **Nether Fish and Cubozoa:** their registered predicate requires lava at the spawn point and above it.
- **Nautilus:** has its own near-sea-level water predicate, separate from the shared water-animal depth mixin.
- **Sea Serpent:** additionally rejects spawn positions above its own low-altitude ceiling.
- **Farseers:** both their instance spawn check and their static natural-spawn predicate return false.
- **Senor Pumpkin and Sir Pumpkinhead:** seasonal predicates are replaced with ordinary ground monster rules. The replacement biome-modifier files contain empty biome lists, disabling those particular original add-spawn lists; other spawn-list code can still add them.
- **Moss blocks:** their spawn-validity predicate is changed to accept mobs when the upper face is sturdy, despite moss's other lighting changes.
- **Red mushroom blocks, brown mushroom blocks, Nether wart blocks, warped wart blocks, and Quark glowshroom blocks:** their spawn-validity predicate accepts only ocelots and parrots.
- **Natural/chunk-generation spawning guards:** deny exposed-surface spawns in deep-dark and soul-sand-valley biomes; creature-category mobs are additionally denied in the configured outer underground mushroom/Nether cave river-bank areas. Spawner, command, breeding, and summoned spawn reasons are not covered by this guard.
- **Shulkers** are allowed to despawn when distant; the companion golem-base method keeps other abstract golems from distance-despawning.

Sources: [NoFloatingMobSpawns](src/main/java/net/amurdza/examplemod/event_handlers/NoFloatingMobSpawns.java), [MobCategoryChanges](src/main/java/net/amurdza/examplemod/event_handlers/MobCategoryChanges.java), [spawning mixins](src/main/java/net/amurdza/examplemod/mixins/mob_spawning), [sea-level behavior mixins](src/main/java/net/amurdza/examplemod/mixins/sea_level), [ModEntities](src/main/java/net/amurdza/examplemod/registry/ModEntities.java), [AOEMod](src/main/java/net/amurdza/examplemod/AOEMod.java), [UndergroundCaveSurfaceSpawnGuard](src/main/java/net/amurdza/examplemod/event_handlers/UndergroundCaveSurfaceSpawnGuard.java), [pumpkin spawn overrides](src/main/resources/data/born_in_chaos_v1/forge/biome_modifier).

### Natural-spawn candidates in the custom biomes

These tables describe runtime mob populations declared in the biome data, without describing terrain or generated features. They list the explicit candidates, not a guarantee that every candidate succeeds: the spawning restrictions above still apply, and other mods may add their own entries. Empty land-animal lists below mean this mod declares no `creature` entries for that biome; structure-spawned mobs are outside this inventory.

| AOE biome | Land animals and ambient mobs | Hostile-category candidates | Aquatic candidates |
| --- | --- | --- | --- |
| Rainforest (`aoemod:jungle`) | Bat, squirrel (Ecologics), rabbit, deer (Naturalist), chicken, fox, gorilla, parrot, cat, panda, frog, ocelot, turtle | Spider, slime, creaking | Axolotl, cod, pufferfish, tropical fish, salmon, arrow squid (Hybrid Aquatic), lobster (Alex's Mobs), end fish, dolphin, squid |
| Savanna | Bat, squirrel, bison, emu, gazelle, moose, rabbit, llama, horse, donkey, deer, chicken, cow, sheep, pig, wolf, boar (Naturalist) | Skeleton, zombie, Senor Pumpkin | Glow squid, cod, tropical fish, pufferfish, lobster, salmon, arrow squid, squid |
| Plains | Bat, rabbit, sheep, pig, chicken, cow, horse, donkey, deer, squirrel | Zombie villager, pillager, ravager | Cod, salmon, lobster, squid |
| Grove | Bat; no land-animal entries | Stray, drowned, breeze | Glow squid, salmon, cod, arrow squid, lobster, end fish, dolphin, squid |
| Desert | Bat; no land-animal entries | Husk, parched | End fish |
| Crimson forest | No land-animal/ambient entries | No explicit monster entries | Nether fish |
| Warped forest | No land-animal/ambient entries | Creeper, zoglin | Searing cod (Nether Depths Upgrade) |
| Soul sand valley | No land-animal/ambient entries | Wraith (Quark), skeleton | Cubozoa |
| Basalt deltas | No land-animal/ambient entries | Magma cube, blaze | Glowdine (Nether Depths Upgrade), cubozoa |
| Deep dark | No land-animal/ambient entries | Enderman, shulker | Glow squid |

Source: the `spawners` fields in [AOE biome data](src/main/resources/data/aoemod/worldgen/biome). No terrain, structure or feature settings from these files are cataloged here.

## Animal food and spawning tags

- Alex's Mobs gorilla **breeding, healing-food, and taming** tags are replaced by the leaf-fruits list above. Bison, emu, gazelle, and moose breeding tags gain wheat. Warped-toad breeding, healing-food, and taming tags gain warped fungus. Except the gorilla replacements, these additions preserve the dependency's existing tag members.
- The **axolotl hunt-target entity tag is replaced with an empty list**, removing tagged hunting prey.
- **General animal spawn surfaces** gain snow layers, snow blocks, moss, grass blocks, and podzol. Fox, goat, and wolf spawn-surface tags include that animal-surface tag. Frog spawn surfaces include those plus mud, mangrove roots, and muddy mangrove roots. Rabbit surfaces include those plus powder snow and the expanded sand tag. Parrot surfaces include those plus air, leaves, and logs. These tag additions are subject to the separate entity spawning checks.
- **Alex's Mobs spawn-surface replacements:** emus get moss/grass; gorillas get moss; lobsters get moss/mud.
- Gold-rabbit biome variants are allowed in AOEMod Desert, Deep Dark, Soul Sand Valley, Warped Forest, Crimson Forest, and Basalt Deltas. White rabbits and snow foxes are allowed in AOEMod Grove. These are appearance/spawning variant tags, not terrain generation.

Sources: [src/main/resources/data/alexsmobs/tags/](src/main/resources/data/alexsmobs/tags), [data/minecraft/tags/entity_types/axolotl_hunt_targets.json](src/main/resources/data/minecraft/tags/entity_types/axolotl_hunt_targets.json), [spawn-surface block tags](src/main/resources/data/minecraft/tags/blocks), [animal-variant biome tags](src/main/resources/data/minecraft/tags/worldgen/biome).

## Travel, presentation and compatibility

### Portal interaction

- Fire can no longer create a Nether portal. This changes portal ignition; it does not remove an already existing portal block or add a replacement travel interaction.

Source: [DisableNetherPortalFire](src/main/java/net/amurdza/examplemod/mixins/dimension/DisableNetherPortalFire.java).

### Creative inventory and clouds

- Adds the **AOE Mod** creative tab, containing all registered `aoemod` items and using the pale oak sapling as its icon.
- Also places the mod's vegetation, ores, foods, equipment, building materials, fish buckets and spawn eggs into their corresponding vanilla creative tabs. The dedicated AOE tab includes registered items even when the vanilla-tab lists omit them.
- Hides clouds when the camera's biome has no precipitation. This is a rendering rule based on the current biome, rather than the current rain state.

Sources: [ModCreativeTabs](src/main/java/net/amurdza/examplemod/registry/ModCreativeTabs.java), [vanilla-tab additions](src/main/java/net/amurdza/examplemod/AOEMod.java), [cloud rendering](src/main/java/net/amurdza/examplemod/mixins/client/CloudsRequirePrecipitation.java).

### Renamed existing items and mobs

The English language resources change these displayed names; registry IDs remain the original IDs.

| Original identity | Displayed identity |
| --- | --- |
| Drop the Meat raw/cooked llama | Raw/cooked moose |
| Drop the Meat raw/cooked bear | Raw/cooked bison |
| Drop the Meat raw/cooked goat | Raw/cooked gazelle |
| Drop the Meat raw/cooked squid | Raw/cooked warped toad |
| Drop the Meat raw/cooked red meat | Raw/cooked squirrel |
| Drop the Meat raw/cooked wolf | Raw/cooked arrow squid |
| Drop the Meat raw/cooked turtle | Raw/cooked boar |
| Farmer's Delight salmon roll | Tropical Fish Roll |
| Fruits Delight mangosteen | Plum; also renames its cake, cauldron, crate, jello block/cauldron, jelly/jelly block/jelly cauldron, leaves, sapling, potted sapling, jam fluid, melted jello fluid, tea fluid, jello item and tea item. “Jelly” products display as plum jam. |
| AOE entity ID `illager_lord` | Illager King, including its spawn egg |
| Born in Chaos `stalkerwarning` entity name | An empty display name |

- Explicit English labels are supplied for Nether Depths Upgrade's crimson and warped kelp, kelp plants, seagrass and tall seagrass.
- Soul soil quartz ore and soul sand quartz ore currently have the duplicated word “Soul” in their English names (“Soul Soil Soul Quartz Ore” and “Soul Sand Soul Quartz Ore”).

Sources: [AOE English language file](src/main/resources/assets/aoemod/lang/en_us.json), [Fruits Delight English overrides](src/main/resources/assets/fruitsdelight/lang/en_us.json).

### Existing block appearance overrides

- Supplies replacement textures for **moss blocks, shroomlights, cave vines and cave-vine plants (both lit and unlit), azalea shrubs, flowering azalea shrubs, azalea leaves, flowering azalea leaves, potted azalea shrubs, potted flowering azalea shrubs, big dripleaves, small dripleaves, and the spore blossom base**. The dripleaf replacements include their top/side/stem parts.
- Overrides Quark's glowshroom blockstate with a single `quark:block/glow_shroom` model variant.
- New AOE blocks, items and mobs have their own models/textures; those assets support the additions listed in this inventory rather than introducing additional gameplay mechanics.

Sources: [Minecraft block texture overrides](src/main/resources/assets/minecraft/textures/block), [Quark glowshroom blockstate](src/main/resources/assets/quark/blockstates/glow_shroom.json), [AOE assets](src/main/resources/assets/aoemod).

### Data-fixer compatibility

- Bypasses the missing data-fixer lookup for Snow Real Magic's snow entity names.
- Also catches the specific “Data fixer not registered for” failure for other missing data-fixer choices and returns no fixer instead of propagating that exception. This is compatibility handling, not a snow gameplay rule.

Source: [SnowEntityDatafixerFallback](src/main/java/net/amurdza/examplemod/mixins/othermods/snowrealmagic/SnowEntityDatafixerFallback.java).

## Mob implementation caveats and inactive code

These are differences between apparent intent and the actual active paths, useful when deciding what to keep.

- **Unconfigured ageable species do not fall back to vanilla aging.** `mobGrowthChance` returns a negative sentinel when a species has no growth row, but the active aging mixin converts that into zero progress. Its feeding override can even apply a negative age increment. The mixin does not guard against that sentinel.
- **Some vanilla-only feeding restrictions can be bypassed through the added-food event path.** That path can consume food and set love without checking biome fertility first; offspring can still fail later.
- **Dolphin fertility rows are unused by dolphin mating.** The custom dolphin breeding goal always attempts one baby, regardless of the configured biome offspring multiplier.
- **Polar bear owner-defense is partly blocked by the universal filter.** The specialized bear behavior permits appropriate owner targets, but `MobTargetFilterMixin` does not recognize the bear's custom tame/owner interface as a tamed-animal exemption. It can reject the bear's attempt to target an ordinary hostile mob.
- **Horseman riders get their identifying tag after the initial spawn call.** The join event can therefore already have assigned ordinary zombie/skeleton stats before the special rider tag exists. The spear/bow is set explicitly afterward, but the intended special armor/stats are not guaranteed on that initial spawn; equipment is retuned on a later world join, while one-time stats remain marked as processed.
- **Fox/wolf/bee specialized removal branches in `MobAI` are unreachable after the earlier broad animal branch.** Do not interpret those later branches as proof that all fox berry/pounce/perch behavior, wolf-specific panic, or bee target goals are removed. The independent global filtering and active mixins still apply.
- **Returned ghast-fireball damage reduction is unreachable.** Its hurt-event branch first requires the damage source's direct entity to be the living attacker, then tests for a fireball as that direct entity. Those conditions cannot both hold.
- **The water-depth helper counts falling water, not general water depth.** `WorldGenUtils.getTotalWaterAbove` only recognizes a water block whose `LEVEL` property is the falling-water value. Ordinary source water and waterlogged blocks are not counted. The fish/axolotl spawning, Drowned beach, and turtle travel patches therefore do not implement a general depth-below-water-surface rule in ordinary source-water lakes.
- **Death-drop replacement removes the Illager King's shield** despite the boss's own conditional shield-drop implementation. It also removes ordinary wither-skeleton skulls, evoker totems, and other non-whitelisted old loot.
- **Looting metadata is stored but not applied** by the replacement handler.
- **Sheep meat and chicken meat each have two plains-only drop rows.** The actual code adds both rows in plains; it does not use the second row as a savanna entry.
- **`disabled.DayTimeDoesntHurtMobs` and `disabled.MobCap` are listed in the active mixin JSON, but their changes are entirely commented out.** They do not globally disable sunlight burning or alter mob caps.

## Incomplete or inactive resource entries

These files are present, but do not establish the working changes their names suggest.

- **Vanilla block loot files are in [data/minecraft/loot_table/blocks/](src/main/resources/data/minecraft/loot_table/blocks) (singular).** Minecraft 1.20.1 loads `loot_tables` (plural), so these files do not replace vanilla tables. They appear intended to remove ordinary Silk Touch/shears requirements from blue/packed ice, living/dead tube/brain/bubble/fire/horn coral plants/fans/blocks, glass/panes, glow lichen, hanging roots, mushroom stems, Nether sprouts, sea lanterns, seagrass/tall seagrass, normal/soul campfires, chiseled bookshelves, turtle eggs, ordinary vines, and weeping/twisting vines including body blocks. Other inactive files implement shears-or-Silk-Touch sculk/sculk vein/sculk catalyst/sensor/shrieker drops and biome-sensitive red/brown mushroom-block drops. Separate runtime drop code may implement overlapping behavior, but these files cannot be cited as its active mechanism.
- **Bone shears recipe fails to load:** its ingredient uses a newer string shorthand and its result uses `id` instead of 1.20.1's `item`. It also names vanilla shears rather than the registered bone shears. The bone-shears item itself is registered and available through other means such as creative inventory.
- **Glow Berry Soup recipe fails to load:** its output `aoemod:glow_berry_soup` is not registered. Do not list soup as an actual added item.
- **Sweet Berry Pie recipe is conditional on an unbundled optional mod in practice:** it refers to `pies:sweet_berry_pie`, with no load condition guarding the recipe, and uses a **whole melon**, wheat, sugar, and egg rather than sweet berries. The checked-in Gradle dependencies do not supply Pies; without an externally installed Pies mod it fails to load. `mods.toml` marks Pies optional.
- **Cooked Fish tag misspells Cooked End Fish**, invalidating cooked-fish and dependent food-wrappable behavior as described above.
- **Sniffer digging resource is inactive/broken:** `aoemod:gameplay/sniffer_digging` is not an override of vanilla's `minecraft:gameplay/sniffer_digging`, has no code reference here, and has an invalid nested entries array. Its apparent rice/onion/potato/carrot selection is not a working sniffer change.
- **Cherry vine is an unused leftover, not an added block.** Its [self-drop table](src/main/resources/data/aoemod/loot_tables/blocks/cherry_vine.json) refers to `aoemod:cherry_vine`, but no such block/item is registered.
- **Leftover custom entity loot files:** Giant Spider Crab, Isopod, Shrimp, and Squat Lobster tables reference unregistered `aoemod:crab_legs`, `aoemod:isopod`, `aoemod:shrimp`, and `aoemod:squat_lobster` items; Giraffe references `wildernature:venison`, whose mod is absent from the current dependency list. Do not mistake these files for the working event-driven mob drop configuration. Archlich's table has no pools; Zebra's file is an empty object.
- **Snow-survival tag is invalid:** `minecraft:snow_layer_cannot_survive_on` references required `#minecraft:barrier`, but no such tag definition is present, so its intended ice/packed-ice/blue-ice list is not reliably loaded. Existing historical logs corroborate this and the recipe/tag/loot failures above, but those logs are not a fresh test of this audit.
- **Several biome-placement block tags are invalid**, affecting their apparent placement restrictions: `not_rainforest` has nonexistent block IDs `minecraft:redstone_repeater` and `minecraft:redstone`; `rainforest_only` has `minecraft:frog_spawn`; `rainforest_savanna_plains_only` has `minecraft:hay_bale`; `soul_sand_valley_only` has `aoemod:eye_blossom`. Correct registered forms differ. The **Planting and building restrictions** section separates these intended rules from the valid tags.

Sources: individual resource paths named above; [src/main/java/net/amurdza/examplemod/registry/ModItems.java](src/main/java/net/amurdza/examplemod/registry/ModItems.java); [run/logs/latest.log](run/logs/latest.log) contains historical parse failures dated August 21, 2026. No production files were changed by this audit.

## Audit coverage and validation

This inventory follows the enabled mixin list, registered content, subscribed event handlers, their helper/configuration classes, and packaged recipes, loot tables, tags, language files and appearance overrides. `src/generated/resources` contains no additional generated gameplay data. Loose root-level `assets/`, `data/`, extraction folders and temporary files are not additional packaged resource roots in the Gradle configuration.

The repository build completed successfully with `.\gradlew.bat build`. This change only adds documentation, so no client or server gameplay session was launched. Where mentioned, the existing August 2026 logs corroborate specific resource failures; they are historical evidence rather than a new test. A successful Java/Gradle build does not validate every runtime recipe, tag or behavior described in the caveats.

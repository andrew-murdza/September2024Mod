# Random-ticking blocks missing from the growth-rate table

This is a companion to the annotated change inventory. “Missing” means absent from `BlockGrowthConfig`; it does **not** mean the block is broken or should receive a crop-growth multiplier. Leaf decay, oxidation, melting and portal spawning use random ticks too.

The reviewed installed-registry snapshot is [RANDOM_TICK_BLOCKS.md](RANDOM_TICK_BLOCKS.md), exported by the successful 24-test server run on October 17, 2026. It checks **every possible state** of every registered block and lists 262 blocks without growth overrides, including installed backports/dependency mods. [BLOCK_AVAILABILITY.md](BLOCK_AVAILABILITY.md) covers all 3,052 registered blocks, with all-six-dimensions, no-dimensions and complete per-dimension lists derived from loaded tags and recipes. Each server start refreshes the generated versions in `build/reports/`; the root-level files are the reviewed snapshots.

## Existing Minecraft 1.20.1 blocks

These blocks are not in the natural-growth table from the original inventory:

| Exact blocks | What random ticks do | Existing AOE treatment |
| --- | --- | --- |
| `minecraft:sugar_cane` | Advance the stalk's age and grow a new top segment. | Waterlogging and support are customized; no dimension growth multiplier is registered. |
| `minecraft:torchflower_crop` | Advance an immature crop toward a torchflower. | Uses the ordinary crop hook, but no table entry. The mature `minecraft:torchflower` is a different block. |
| `minecraft:pitcher_crop` | Advance the immature lower half; upper-half and mature states do not all request random ticks. | No table entry. The decorative `minecraft:pitcher_plant` is separate. |
| `minecraft:grass_block` | Spread to dirt or die under unsuitable cover. | Separate dimension-specific spread-attempt settings; no crop-growth entry. |
| `minecraft:mycelium` | Spread to dirt or die under unsuitable cover. | Separate spreading loop; targets must have mushroom caves above them. |
| `minecraft:crimson_nylium`, `minecraft:warped_nylium` | Revert under unsuitable cover; AOE additionally spreads them to netherrack. | Separate spread chance; the target's biome above must match the nylium. |
| `minecraft:farmland` | Gain/lose moisture and eventually revert when dry and unplanted. | Trampling and solid-block support rules are modified; no crop-growth entry. |
| `minecraft:oak_leaves`, `minecraft:spruce_leaves`, `minecraft:birch_leaves`, `minecraft:jungle_leaves`, `minecraft:acacia_leaves`, `minecraft:dark_oak_leaves`, `minecraft:mangrove_leaves`, `minecraft:cherry_leaves`, `minecraft:azalea_leaves`, `minecraft:flowering_azalea_leaves` | Decay when sufficiently far from logs and not persistent. | Their player-break loot is customized separately. Decay is not controlled by the growth table. |
| `minecraft:snow` | Melt when the relevant block light is high enough. | AOE cancels melting. Still distinguish tick eligibility from a tick that performs work. |
| `minecraft:ice` | Melt in sufficient block light. | No growth-table entry. |
| `minecraft:frosted_ice` | Advance melting/aging behavior; scheduled ticks also participate. | No growth-table entry. |
| `minecraft:redstone_ore`, `minecraft:deepslate_redstone_ore` | Lit states stop glowing. | The ore product/yield override is separate. |
| `minecraft:copper_block`, `minecraft:exposed_copper`, `minecraft:weathered_copper` | Progress oxidation. | No growth-table entry. Fully oxidized and waxed blocks do not need another oxidation step. |
| `minecraft:cut_copper`, `minecraft:exposed_cut_copper`, `minecraft:weathered_cut_copper` | Progress oxidation. | No growth-table entry. |
| `minecraft:cut_copper_stairs`, `minecraft:exposed_cut_copper_stairs`, `minecraft:weathered_cut_copper_stairs` | Progress oxidation. | No growth-table entry. |
| `minecraft:cut_copper_slab`, `minecraft:exposed_cut_copper_slab`, `minecraft:weathered_cut_copper_slab` | Progress oxidation. | No growth-table entry. |
| `minecraft:nether_portal` | May spawn a zombified piglin when the world's conditions allow it. | Mob spawning, not plant growth. |
| `minecraft:lava` | The lava fluid can attempt to ignite nearby blocks. | AOE disables lava ignition. Fluid ticking must not be confused with ordinary crop growth. |
| `minecraft:mangrove_roots` | Registered with random ticks, but the base roots class has no meaningful random-tick work. | Tick-eligible, not a missing natural-growth rule. |
| `minecraft:small_amethyst_bud`, `minecraft:medium_amethyst_bud`, `minecraft:large_amethyst_bud`, `minecraft:amethyst_cluster` | Tick properties are inherited from the cluster registration, but these crystal blocks do not themselves grow the next stage. | The nearby `minecraft:budding_amethyst` performs growth and **is already in the table**. |

The vanilla entries were checked against the project's mapped Forge 1.20.1 sources (`Blocks`, the corresponding block classes and `LavaFluid`), rather than inferred from whether a block looks alive. These are source paths inside the dependency source JAR, not repository production edits.

## Existing AOE and dependency blocks to review separately

| Blocks | Behavior outside the table |
| --- | --- |
| `aoemod:pale_oak_leaves` | Ordinary leaf decay; the pale-oak **sapling** already has a growth entry. |
| `aoemod:open_eyeblossom`, `aoemod:closed_eyeblossom` | Day/night switching and delayed neighboring flower changes. |
| `farmersdelight:rich_soil` | Farmer's Delight's soil boosting; AOE's tall-flower/duplication extension remains. |
| `farmersdelight:rich_soil_farmland` | Moisture updates remain; automatic crop fertilization has been removed. |
| Farmer's Delight upper rice panicles and tomato-vine blocks, Fruits Delight fruit-tree leaves/saplings, and other dependency plants | Consult the generated registry report for the exact installed block IDs and tick-eligible states. The AOE table directly registers Farmer's Delight's onion, tomato, cabbage and lower rice crops, not every secondary block used by those plants. |

The report deliberately includes blocks whose random-tick method another mixin cancels, such as snow. It is an eligibility inventory, and the table above explains the known differences between eligibility and effective behavior.

## Blocks added after 1.20.1

These need checking when choosing or implementing backports. The automatically generated report determines whether the **installed backport's** block actually requests random ticks; the original Java release and a backport can implement the same visible behavior with different timers.

| Blocks/process | Relevance to the growth table |
| --- | --- |
| Pale oak sapling and pale oak leaves | Tree growth and leaf decay. AOE already has its own versions; the sapling is registered, leaves are not. Avoid adding a second overlapping sapling-rate rule when integrating another backport. |
| Open and closed eyeblossoms | Day/night changes use random block ticks in modern Java. These are flower state changes, not crop-age growth. [Official random-tick behavior documentation](https://feedback.minecraft.net/hc/en-us/articles/41225261004941-Minecraft-Java-Edition-Snapshot-25w45a). |
| Chiseled copper, copper grate, copper bulb, copper door, copper trapdoor | Review all unwaxed, not-yet-fully-oxidized states added with 1.21. The process to consider is oxidation, not crop growth. |
| Copper chest, copper bars, copper chain, copper lantern, oxidizable lightning rods | Later copper families oxidize over time. Include their unwaxed intermediate stages when examining the loaded registry. Copper torches do not join this group merely because their recipe uses copper. [Official Copper Age release notes](https://feedback.minecraft.net/hc/en-us/articles/39966147272589-Minecraft-Java-Edition-1-21-9-The-Copper-Age). |
| Cactus flower production | The **cactus** can produce a flower instead of becoming taller; this is not independent natural growth by an already placed cactus-flower block. AOE's replacement cactus/prickly-pear tick needs explicit integration if cactus flowers are added. [Official 1.21.5 release notes](https://www.minecraft.net/en-us/article/minecraft-java-edition-1-21-5). |
| Poplar sapling; red, orange and yellow poplar leaves | Saplings grow trees with one of three leaf colors; the leaves are candidates for ordinary leaf decay. Check the selected backport's actual tick implementation in the generated report. [Official Java 26.3 release notes](https://www.minecraft.net/en-us/article/minecraft-java-edition-26-3). |
| Sulfur spike | Downward spikes can grow beneath sulfur blocks. This is a natural-growth process to evaluate; the release notes alone do not establish its internal tick scheduling. [Official Java 26.2 release notes](https://feedback.minecraft.net/hc/en-us/articles/46690753273997-Minecraft-Java-Edition-26-2). |
| Potent sulfur | Magma-heated geysers erupt at random intervals, while lava heating is continuous. Random intervals do not by themselves prove random **block** ticks; inspect the installed implementation before adding a growth multiplier. [Official Java 26.2 release notes](https://feedback.minecraft.net/hc/en-us/articles/46690753273997-Minecraft-Java-Edition-26-2). |

Do not automatically add wildflowers, bush, firefly bush, leaf litter, short/tall dry grass, red shrub or shelf mushroom to a natural-growth table. The release notes describe bone-meal reproduction/growth for the plants and shelf mushroom; leaf litter does not regenerate naturally. Sounds and particles are another separate process. [Official Spring to Life notes](https://www.minecraft.net/en-us/article/minecraft-java-edition-1-21-5), [official Wilderness Bound notes](https://www.minecraft.net/en-us/article/minecraft-java-edition-26-3).

Dried-ghast rehydration, trial spawners, vaults and creaking-heart mob management also require their own timing review; do not assume block-entity activity is governed by `randomTickSpeed` or the crop-growth event.

## Confirmed installed-backport results

The runtime registry report confirms random-ticking states for `minecraft:poplar_sapling`, `minecraft:red_poplar_leaves`, `minecraft:orange_poplar_leaves`, `minecraft:yellow_poplar_leaves`, and `minecraft:sulfur_spike`. These blocks currently have no growth-table override.

It also confirms oxidation ticks for the unwaxed initial, exposed and weathered stages of chiseled copper, copper bars, bulbs, chains, chests, doors, golem statues, grates, lanterns, trapdoors and lightning rods. The exact block IDs appear individually in the report.

`minecraft:potent_sulfur`, `minecraft:cactus_flower`, `minecraft:red_shrub`, `minecraft:wildflowers`, `minecraft:golden_dandelion`, and `minecraft:shelf_mushroom` have no random-ticking state in this installed backport. This does not rule out scheduled, block-entity, player-interaction or visual behavior. The cactus block remains covered by its existing growth-table entry.

The report also lists the backport's registered pale-oak and eyeblossom copies even though normal acquisition of those duplicates is disabled in favor of AOE's existing versions. Registry presence is separate from whether the pack exposes an item to players.

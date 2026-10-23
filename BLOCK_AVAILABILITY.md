# Loaded block availability

Snapshot from the successful 24-test server run on October 17, 2026, at 20:10 America/New_York. The inventory contains 3,052 registered blocks: 691 qualify in all six dimensions and 173 qualify in none. See the separate [random-ticking block inventory](RANDOM_TICK_BLOCKS.md) and [behavior notes](RANDOM_TICK_REVIEW.md).

The [report generator](src/main/java/net/amurdza/examplemod/event_handlers/BlockAvailabilityReport.java) reads the [placement rules](src/main/java/net/amurdza/examplemod/util/BiomePlacementRestrictions.java) and [recipe/material availability rules](src/main/java/net/amurdza/examplemod/util/DimensionMaterialAvailability.java). A new server start refreshes `build/reports/BLOCK_AVAILABILITY.md`; this root-level file is the reviewed snapshot.

Generated from the running server's block registry, tags, and recipes. Includes all installed backports. ‘All’ and ‘none’ refer to the six AOE dimensions, not vanilla or other mods' dimensions, which retain their original placement. These are dimension/material eligibility results, not a guarantee of placement on every surface. This does not promise that a block is obtainable: hidden dependency content and technical blocks remain in the registry and are listed too. Mycelium, nylium, netherrack, soul sediments and living Nether plants also have local biome checks. Block survival, collisions, water, support, and two-block clearance still apply.

An entry in ‘none’ can be intentional when no dimension allows every ingredient and the finished block. Quark blackstone/netherrack bricks require their Nether raw stone together with cobblestone bricks available outside Nether. Packed mud needs mud and wheat, whose shared region is savanna, where the pickaxe-block restriction rejects packed mud. Enchanting tables need snowy-only diamonds together with Nether-only obsidian. These combinations currently have no permitted dimension.

## Allowed in all six dimensions

- `alexsmobs:banana_peel`
- `alexsmobs:bison_carpet`
- `alexsmobs:bison_fur_block`
- `alexsmobs:caiman_egg`
- `alexsmobs:crocodile_egg`
- `alexsmobs:ender_residue`
- `alexsmobs:platypus_egg`
- `alexsmobs:red_sand_circle`
- `alexsmobs:sand_circle`
- `alexsmobs:sculk_boomer`
- `alexsmobs:skunk_spray`
- `alexsmobs:terrapin_egg`
- `alexsmobs:triops_eggs`
- `alexsmobs:void_worm_effigy`
- `bettercarrots:nether_carrot_roots`
- `born_in_chaos_v1:argillite_lamp`
- `born_in_chaos_v1:bundle_of_bones`
- `born_in_chaos_v1:closed_hound_trap`
- `born_in_chaos_v1:cobweb_cover`
- `born_in_chaos_v1:creepy_nutcracker`
- `born_in_chaos_v1:cultivated_pumpkin`
- `born_in_chaos_v1:dark_stained_glass`
- `born_in_chaos_v1:dark_stained_glass_panel`
- `born_in_chaos_v1:evil_carved_pumpkin`
- `born_in_chaos_v1:flaming_evil_pumpkin`
- `born_in_chaos_v1:fragile_scorched_boards`
- `born_in_chaos_v1:gnawed_bones`
- `born_in_chaos_v1:green_stained_glass`
- `born_in_chaos_v1:green_stained_glass_panel`
- `born_in_chaos_v1:hound_trap`
- `born_in_chaos_v1:infernal_evil_pumpkin`
- `born_in_chaos_v1:infernal_evil_pumpkin_s`
- `born_in_chaos_v1:lifestealer_skull`
- `born_in_chaos_v1:marigolds`
- `born_in_chaos_v1:nightmare_stalker_skull`
- `born_in_chaos_v1:orange_stained_glass`
- `born_in_chaos_v1:orange_stained_glass_panel`
- `born_in_chaos_v1:pile_of_skulls`
- `born_in_chaos_v1:puddleof_stimulation`
- `born_in_chaos_v1:puddleofintoxication`
- `born_in_chaos_v1:purple_stained_glass`
- `born_in_chaos_v1:purple_stained_glass_panel`
- `born_in_chaos_v1:river_mint`
- `born_in_chaos_v1:rotten_infernal_pumpkin`
- `born_in_chaos_v1:scarlet_stained_glass`
- `born_in_chaos_v1:scarlet_stained_glass_panel`
- `born_in_chaos_v1:scorched_log`
- `born_in_chaos_v1:scorched_planks`
- `born_in_chaos_v1:scorched_planks_button`
- `born_in_chaos_v1:scorched_planks_door`
- `born_in_chaos_v1:scorched_planks_fence`
- `born_in_chaos_v1:scorched_planks_fence_gate`
- `born_in_chaos_v1:scorched_planks_pressure_plates`
- `born_in_chaos_v1:scorched_planks_slab`
- `born_in_chaos_v1:scorched_planks_stairs`
- `born_in_chaos_v1:scorched_planks_trapdoor`
- `born_in_chaos_v1:scorched_wood`
- `born_in_chaos_v1:smoldering_scorched_log`
- `born_in_chaos_v1:smoldering_scorched_wood`
- `born_in_chaos_v1:stripped_scorched_log`
- `born_in_chaos_v1:stripped_scorched_wood`
- `born_in_chaos_v1:tombstone_plug`
- `citadel:lectern`
- `culturaldelights:avocado_crate`
- `culturaldelights:avocado_leaves`
- `culturaldelights:avocado_log`
- `culturaldelights:avocado_pit`
- `culturaldelights:avocado_sapling`
- `culturaldelights:avocado_wood`
- `culturaldelights:corn`
- `culturaldelights:corn_cob_crate`
- `culturaldelights:corn_upper`
- `culturaldelights:cucumber_crate`
- `culturaldelights:cucumbers`
- `culturaldelights:eggplant_crate`
- `culturaldelights:eggplants`
- `culturaldelights:exotic_roll_medley`
- `culturaldelights:fruiting_avocado_leaves`
- `culturaldelights:pickle_crate`
- `culturaldelights:white_eggplant_crate`
- `culturaldelights:wild_corn`
- `culturaldelights:wild_cucumbers`
- `culturaldelights:wild_eggplants`
- `ecologics:coconut`
- `ecologics:coconut_button`
- `ecologics:coconut_door`
- `ecologics:coconut_fence`
- `ecologics:coconut_fence_gate`
- `ecologics:coconut_hanging_sign`
- `ecologics:coconut_leaves`
- `ecologics:coconut_log`
- `ecologics:coconut_planks`
- `ecologics:coconut_pressure_plate`
- `ecologics:coconut_seedling`
- `ecologics:coconut_sign`
- `ecologics:coconut_slab`
- `ecologics:coconut_stairs`
- `ecologics:coconut_trapdoor`
- `ecologics:coconut_wall_hanging_sign`
- `ecologics:coconut_wall_sign`
- `ecologics:coconut_wood`
- `ecologics:hanging_coconut`
- `ecologics:moss_layer`
- `ecologics:prickly_pear`
- `ecologics:seashell`
- `ecologics:stripped_coconut_log`
- `ecologics:stripped_coconut_wood`
- `ecologics:stripped_walnut_log`
- `ecologics:stripped_walnut_wood`
- `ecologics:surface_moss`
- `ecologics:walnut_button`
- `ecologics:walnut_door`
- `ecologics:walnut_fence`
- `ecologics:walnut_fence_gate`
- `ecologics:walnut_hanging_sign`
- `ecologics:walnut_leaves`
- `ecologics:walnut_log`
- `ecologics:walnut_planks`
- `ecologics:walnut_pressure_plate`
- `ecologics:walnut_sapling`
- `ecologics:walnut_sign`
- `ecologics:walnut_slab`
- `ecologics:walnut_stairs`
- `ecologics:walnut_trapdoor`
- `ecologics:walnut_wall_hanging_sign`
- `ecologics:walnut_wall_sign`
- `ecologics:walnut_wood`
- `farmersdelight:basket`
- `farmersdelight:beetroot_crate`
- `farmersdelight:black_canvas_sign`
- `farmersdelight:black_canvas_wall_sign`
- `farmersdelight:black_hanging_canvas_sign`
- `farmersdelight:black_wall_hanging_canvas_sign`
- `farmersdelight:blue_canvas_sign`
- `farmersdelight:blue_canvas_wall_sign`
- `farmersdelight:blue_hanging_canvas_sign`
- `farmersdelight:blue_wall_hanging_canvas_sign`
- `farmersdelight:brown_canvas_sign`
- `farmersdelight:brown_canvas_wall_sign`
- `farmersdelight:brown_hanging_canvas_sign`
- `farmersdelight:brown_mushroom_colony`
- `farmersdelight:brown_wall_hanging_canvas_sign`
- `farmersdelight:cabbage_crate`
- `farmersdelight:canvas_rug`
- `farmersdelight:canvas_sign`
- `farmersdelight:canvas_wall_sign`
- `farmersdelight:carrot_crate`
- `farmersdelight:cutting_board`
- `farmersdelight:cyan_canvas_sign`
- `farmersdelight:cyan_canvas_wall_sign`
- `farmersdelight:cyan_hanging_canvas_sign`
- `farmersdelight:cyan_wall_hanging_canvas_sign`
- `farmersdelight:full_tatami_mat`
- `farmersdelight:gray_canvas_sign`
- `farmersdelight:gray_canvas_wall_sign`
- `farmersdelight:gray_hanging_canvas_sign`
- `farmersdelight:gray_wall_hanging_canvas_sign`
- `farmersdelight:green_canvas_sign`
- `farmersdelight:green_canvas_wall_sign`
- `farmersdelight:green_hanging_canvas_sign`
- `farmersdelight:green_wall_hanging_canvas_sign`
- `farmersdelight:half_tatami_mat`
- `farmersdelight:hanging_canvas_sign`
- `farmersdelight:honey_glazed_ham_block`
- `farmersdelight:light_blue_canvas_sign`
- `farmersdelight:light_blue_canvas_wall_sign`
- `farmersdelight:light_blue_hanging_canvas_sign`
- `farmersdelight:light_blue_wall_hanging_canvas_sign`
- `farmersdelight:light_gray_canvas_sign`
- `farmersdelight:light_gray_canvas_wall_sign`
- `farmersdelight:light_gray_hanging_canvas_sign`
- `farmersdelight:light_gray_wall_hanging_canvas_sign`
- `farmersdelight:lime_canvas_sign`
- `farmersdelight:lime_canvas_wall_sign`
- `farmersdelight:lime_hanging_canvas_sign`
- `farmersdelight:lime_wall_hanging_canvas_sign`
- `farmersdelight:magenta_canvas_sign`
- `farmersdelight:magenta_canvas_wall_sign`
- `farmersdelight:magenta_hanging_canvas_sign`
- `farmersdelight:magenta_wall_hanging_canvas_sign`
- `farmersdelight:onion_crate`
- `farmersdelight:orange_canvas_sign`
- `farmersdelight:orange_canvas_wall_sign`
- `farmersdelight:orange_hanging_canvas_sign`
- `farmersdelight:orange_wall_hanging_canvas_sign`
- `farmersdelight:pink_canvas_sign`
- `farmersdelight:pink_canvas_wall_sign`
- `farmersdelight:pink_hanging_canvas_sign`
- `farmersdelight:pink_wall_hanging_canvas_sign`
- `farmersdelight:potato_crate`
- `farmersdelight:purple_canvas_sign`
- `farmersdelight:purple_canvas_wall_sign`
- `farmersdelight:purple_hanging_canvas_sign`
- `farmersdelight:purple_wall_hanging_canvas_sign`
- `farmersdelight:red_canvas_sign`
- `farmersdelight:red_canvas_wall_sign`
- `farmersdelight:red_hanging_canvas_sign`
- `farmersdelight:red_mushroom_colony`
- `farmersdelight:red_wall_hanging_canvas_sign`
- `farmersdelight:rice_bag`
- `farmersdelight:rice_bale`
- `farmersdelight:rice_roll_medley_block`
- `farmersdelight:roast_chicken_block`
- `farmersdelight:rope`
- `farmersdelight:safety_net`
- `farmersdelight:sandy_shrub`
- `farmersdelight:shepherds_pie_block`
- `farmersdelight:straw_bale`
- `farmersdelight:stuffed_pumpkin_block`
- `farmersdelight:tatami`
- `farmersdelight:tomato_crate`
- `farmersdelight:wall_hanging_canvas_sign`
- `farmersdelight:white_canvas_sign`
- `farmersdelight:white_canvas_wall_sign`
- `farmersdelight:white_hanging_canvas_sign`
- `farmersdelight:white_wall_hanging_canvas_sign`
- `farmersdelight:wild_beetroots`
- `farmersdelight:wild_cabbages`
- `farmersdelight:wild_carrots`
- `farmersdelight:wild_onions`
- `farmersdelight:wild_potatoes`
- `farmersdelight:wild_rice`
- `farmersdelight:wild_tomatoes`
- `farmersdelight:yellow_canvas_sign`
- `farmersdelight:yellow_canvas_wall_sign`
- `farmersdelight:yellow_hanging_canvas_sign`
- `farmersdelight:yellow_wall_hanging_canvas_sign`
- `fruitsdelight:apple_crate`
- `fruitsdelight:apple_jello_block`
- `fruitsdelight:apple_jelly`
- `fruitsdelight:apple_jelly_block`
- `fruitsdelight:apple_leaves`
- `fruitsdelight:apple_sapling`
- `fruitsdelight:attached_hamimelon_stem`
- `fruitsdelight:bayberry_crate`
- `fruitsdelight:bayberry_jello_block`
- `fruitsdelight:bayberry_jelly`
- `fruitsdelight:bayberry_jelly_block`
- `fruitsdelight:bayberry_leaves`
- `fruitsdelight:bayberry_sapling`
- `fruitsdelight:blueberry_bush`
- `fruitsdelight:blueberry_crate`
- `fruitsdelight:blueberry_jello_block`
- `fruitsdelight:blueberry_jelly`
- `fruitsdelight:blueberry_jelly_block`
- `fruitsdelight:cranberry_bush`
- `fruitsdelight:cranberry_crate`
- `fruitsdelight:cranberry_jello_block`
- `fruitsdelight:cranberry_jelly`
- `fruitsdelight:cranberry_jelly_block`
- `fruitsdelight:durian`
- `fruitsdelight:durian_jello_block`
- `fruitsdelight:durian_jelly`
- `fruitsdelight:durian_jelly_block`
- `fruitsdelight:durian_leaves`
- `fruitsdelight:durian_sapling`
- `fruitsdelight:fig_crate`
- `fruitsdelight:fig_jello_block`
- `fruitsdelight:fig_jelly`
- `fruitsdelight:fig_jelly_block`
- `fruitsdelight:fig_leaves`
- `fruitsdelight:fig_pudding`
- `fruitsdelight:fig_sapling`
- `fruitsdelight:glowberry_jello_block`
- `fruitsdelight:glowberry_jelly`
- `fruitsdelight:glowberry_jelly_block`
- `fruitsdelight:hamimelon`
- `fruitsdelight:hamimelon_jello_block`
- `fruitsdelight:hamimelon_jelly`
- `fruitsdelight:hamimelon_jelly_block`
- `fruitsdelight:hamimelon_stem`
- `fruitsdelight:hawberry_crate`
- `fruitsdelight:hawberry_jello_block`
- `fruitsdelight:hawberry_jelly`
- `fruitsdelight:hawberry_jelly_block`
- `fruitsdelight:hawberry_leaves`
- `fruitsdelight:hawberry_sapling`
- `fruitsdelight:kiwi_crate`
- `fruitsdelight:kiwi_jello_block`
- `fruitsdelight:kiwi_jelly`
- `fruitsdelight:kiwi_jelly_block`
- `fruitsdelight:kiwi_leaves`
- `fruitsdelight:kiwi_sapling`
- `fruitsdelight:lemon_crate`
- `fruitsdelight:lemon_jello_block`
- `fruitsdelight:lemon_jelly`
- `fruitsdelight:lemon_jelly_block`
- `fruitsdelight:lemon_tree`
- `fruitsdelight:lychee_crate`
- `fruitsdelight:lychee_jello_block`
- `fruitsdelight:lychee_jelly`
- `fruitsdelight:lychee_jelly_block`
- `fruitsdelight:lychee_leaves`
- `fruitsdelight:lychee_sapling`
- `fruitsdelight:mango_crate`
- `fruitsdelight:mango_jello_block`
- `fruitsdelight:mango_jelly`
- `fruitsdelight:mango_jelly_block`
- `fruitsdelight:mango_leaves`
- `fruitsdelight:mango_sapling`
- `fruitsdelight:mangosteen_cake`
- `fruitsdelight:mangosteen_crate`
- `fruitsdelight:mangosteen_jello_block`
- `fruitsdelight:mangosteen_jelly`
- `fruitsdelight:mangosteen_jelly_block`
- `fruitsdelight:mangosteen_leaves`
- `fruitsdelight:mangosteen_sapling`
- `fruitsdelight:melon_jello_block`
- `fruitsdelight:melon_jelly`
- `fruitsdelight:melon_jelly_block`
- `fruitsdelight:orange_crate`
- `fruitsdelight:orange_jello_block`
- `fruitsdelight:orange_jelly`
- `fruitsdelight:orange_jelly_block`
- `fruitsdelight:orange_leaves`
- `fruitsdelight:orange_sapling`
- `fruitsdelight:peach_crate`
- `fruitsdelight:peach_jello_block`
- `fruitsdelight:peach_jelly`
- `fruitsdelight:peach_jelly_block`
- `fruitsdelight:peach_leaves`
- `fruitsdelight:peach_sapling`
- `fruitsdelight:pear_crate`
- `fruitsdelight:pear_jello_block`
- `fruitsdelight:pear_jelly`
- `fruitsdelight:pear_jelly_block`
- `fruitsdelight:pear_leaves`
- `fruitsdelight:pear_sapling`
- `fruitsdelight:persimmon_crate`
- `fruitsdelight:persimmon_jello_block`
- `fruitsdelight:persimmon_jelly`
- `fruitsdelight:persimmon_jelly_block`
- `fruitsdelight:persimmon_leaves`
- `fruitsdelight:persimmon_sapling`
- `fruitsdelight:pineapple`
- `fruitsdelight:pineapple_crate`
- `fruitsdelight:pineapple_fried_rice`
- `fruitsdelight:pineapple_jello_block`
- `fruitsdelight:pineapple_jelly`
- `fruitsdelight:pineapple_jelly_block`
- `fruitsdelight:sweetberry_jello_block`
- `fruitsdelight:sweetberry_jelly`
- `fruitsdelight:sweetberry_jelly_block`
- `fruitsdelight:wild_pineapple`
- `geckolib:fertilizer`
- `geckolib:gecko_habitat`
- `hybrid-aquatic:anemone`
- `hybrid-aquatic:basking_shark_plushie`
- `hybrid-aquatic:bull_kelp`
- `hybrid-aquatic:bull_kelp_plant`
- `hybrid-aquatic:bull_shark_plushie`
- `hybrid-aquatic:buoy`
- `hybrid-aquatic:button_coral_fan`
- `hybrid-aquatic:button_coral_wall_fan`
- `hybrid-aquatic:cattail`
- `hybrid-aquatic:crab_pot`
- `hybrid-aquatic:dead_button_coral`
- `hybrid-aquatic:dead_button_coral_fan`
- `hybrid-aquatic:dead_button_coral_wall_fan`
- `hybrid-aquatic:dead_leaf_coral`
- `hybrid-aquatic:dead_leaf_coral_fan`
- `hybrid-aquatic:dead_leaf_coral_wall_fan`
- `hybrid-aquatic:dead_lophelia_coral`
- `hybrid-aquatic:dead_lophelia_coral_fan`
- `hybrid-aquatic:dead_lophelia_coral_wall_fan`
- `hybrid-aquatic:dead_rose_coral`
- `hybrid-aquatic:dead_rose_coral_fan`
- `hybrid-aquatic:dead_rose_coral_wall_fan`
- `hybrid-aquatic:dead_sun_coral`
- `hybrid-aquatic:dead_sun_coral_fan`
- `hybrid-aquatic:dead_sun_coral_wall_fan`
- `hybrid-aquatic:dead_thorn_coral`
- `hybrid-aquatic:dead_thorn_coral_fan`
- `hybrid-aquatic:dead_thorn_coral_wall_fan`
- `hybrid-aquatic:driftwood_button`
- `hybrid-aquatic:driftwood_door`
- `hybrid-aquatic:driftwood_fence`
- `hybrid-aquatic:driftwood_fence_gate`
- `hybrid-aquatic:driftwood_log`
- `hybrid-aquatic:driftwood_planks`
- `hybrid-aquatic:driftwood_pressure_plate`
- `hybrid-aquatic:driftwood_slab`
- `hybrid-aquatic:driftwood_stairs`
- `hybrid-aquatic:driftwood_trapdoor`
- `hybrid-aquatic:driftwood_wood`
- `hybrid-aquatic:dunegrass`
- `hybrid-aquatic:floating_sargassum`
- `hybrid-aquatic:frilled_shark_plushie`
- `hybrid-aquatic:giant_green_anemone`
- `hybrid-aquatic:glowing_plankton`
- `hybrid-aquatic:glowslime_block`
- `hybrid-aquatic:glowstick`
- `hybrid-aquatic:great_white_shark_plushie`
- `hybrid-aquatic:hammerhead_shark_plushie`
- `hybrid-aquatic:hybrid_crate`
- `hybrid-aquatic:leaf_coral_fan`
- `hybrid-aquatic:leaf_coral_wall_fan`
- `hybrid-aquatic:lophelia_coral_fan`
- `hybrid-aquatic:lophelia_coral_wall_fan`
- `hybrid-aquatic:message_in_a_bottle`
- `hybrid-aquatic:raft`
- `hybrid-aquatic:rose_coral_fan`
- `hybrid-aquatic:rose_coral_wall_fan`
- `hybrid-aquatic:sargassum`
- `hybrid-aquatic:sargassum_plant`
- `hybrid-aquatic:strawberry_anemone`
- `hybrid-aquatic:stripped_driftwood_log`
- `hybrid-aquatic:stripped_driftwood_wood`
- `hybrid-aquatic:sun_coral_fan`
- `hybrid-aquatic:sun_coral_wall_fan`
- `hybrid-aquatic:tall_dunegrass`
- `hybrid-aquatic:tall_red_algae`
- `hybrid-aquatic:tall_sea_lettuce`
- `hybrid-aquatic:thorn_coral_fan`
- `hybrid-aquatic:thorn_coral_wall_fan`
- `hybrid-aquatic:thresher_shark_plushie`
- `hybrid-aquatic:tiger_shark_plushie`
- `hybrid-aquatic:tube_sponge`
- `hybrid-aquatic:tube_worm`
- `hybrid-aquatic:wall_glowstick`
- `hybrid-aquatic:water_lettuce`
- `hybrid-aquatic:whale_shark_plushie`
- `iceandfire:burnt_torch`
- `iceandfire:burnt_torch_wall`
- `iceandfire:desert_myrmex_cocoon`
- `iceandfire:dragonforge_fire_core_disabled`
- `iceandfire:dragonforge_ice_core_disabled`
- `iceandfire:dragonforge_lightning_core_disabled`
- `iceandfire:dread_portal`
- `iceandfire:dread_stone_face`
- `iceandfire:dread_stone_slab`
- `iceandfire:dread_stone_stairs`
- `iceandfire:dread_torch`
- `iceandfire:dread_torch_wall`
- `iceandfire:dreadwood_log`
- `iceandfire:dreadwood_planks`
- `iceandfire:dreadwood_planks_lock`
- `iceandfire:egginice`
- `iceandfire:fire_lily`
- `iceandfire:frost_lily`
- `iceandfire:ghost_chest`
- `iceandfire:lectern`
- `iceandfire:lightning_lily`
- `iceandfire:myrmex_desert_biolight`
- `iceandfire:myrmex_desert_resin_block`
- `iceandfire:myrmex_desert_resin_glass`
- `iceandfire:myrmex_resin_desert`
- `iceandfire:myrmex_resin_sticky_desert`
- `iceandfire:nest`
- `iceandfire:pixie_house_mushroom_brown`
- `iceandfire:pixie_house_mushroom_red`
- `minecraft:air`
- `minecraft:barrel`
- `minecraft:barrier`
- `minecraft:bedrock`
- `minecraft:black_banner`
- `minecraft:black_bed`
- `minecraft:black_candle_cake`
- `minecraft:black_carpet`
- `minecraft:black_wall_banner`
- `minecraft:black_wool`
- `minecraft:black_wool_slab`
- `minecraft:black_wool_stairs`
- `minecraft:blue_candle_cake`
- `minecraft:bone_block`
- `minecraft:brown_candle_cake`
- `minecraft:bubble_column`
- `minecraft:bush`
- `minecraft:cactus_flower`
- `minecraft:campfire`
- `minecraft:candle_cake`
- `minecraft:cave_air`
- `minecraft:chain_command_block`
- `minecraft:chest`
- `minecraft:chiseled_bookshelf`
- `minecraft:closed_eyeblossom`
- `minecraft:cobweb`
- `minecraft:command_block`
- `minecraft:composter`
- `minecraft:crafting_table`
- `minecraft:creaking_heart`
- `minecraft:creeper_head`
- `minecraft:creeper_wall_head`
- `minecraft:cyan_candle_cake`
- `minecraft:dried_ghast`
- `minecraft:golden_dandelion`
- `minecraft:gray_banner`
- `minecraft:gray_bed`
- `minecraft:gray_candle_cake`
- `minecraft:gray_carpet`
- `minecraft:gray_wall_banner`
- `minecraft:gray_wool`
- `minecraft:gray_wool_slab`
- `minecraft:gray_wool_stairs`
- `minecraft:green_candle_cake`
- `minecraft:jigsaw`
- `minecraft:leaf_litter`
- `minecraft:light`
- `minecraft:light_blue_candle_cake`
- `minecraft:light_gray_banner`
- `minecraft:light_gray_bed`
- `minecraft:light_gray_candle_cake`
- `minecraft:light_gray_carpet`
- `minecraft:light_gray_wall_banner`
- `minecraft:light_gray_wool`
- `minecraft:light_gray_wool_slab`
- `minecraft:light_gray_wool_stairs`
- `minecraft:lime_candle_cake`
- `minecraft:loom`
- `minecraft:magenta_candle_cake`
- `minecraft:moving_piston`
- `minecraft:nether_portal`
- `minecraft:note_block`
- `minecraft:ochre_froglight`
- `minecraft:open_eyeblossom`
- `minecraft:orange_banner`
- `minecraft:orange_bed`
- `minecraft:orange_candle_cake`
- `minecraft:orange_carpet`
- `minecraft:orange_wall_banner`
- `minecraft:orange_wool`
- `minecraft:orange_wool_slab`
- `minecraft:orange_wool_stairs`
- `minecraft:pale_hanging_moss`
- `minecraft:pearlescent_froglight`
- `minecraft:piglin_head`
- `minecraft:piglin_wall_head`
- `minecraft:pink_banner`
- `minecraft:pink_bed`
- `minecraft:pink_candle_cake`
- `minecraft:pink_carpet`
- `minecraft:pink_wall_banner`
- `minecraft:pink_wool`
- `minecraft:pink_wool_slab`
- `minecraft:pink_wool_stairs`
- `minecraft:player_head`
- `minecraft:player_wall_head`
- `minecraft:purple_candle_cake`
- `minecraft:red_candle_cake`
- `minecraft:repeating_command_block`
- `minecraft:resin_block`
- `minecraft:resin_clump`
- `minecraft:shelf_mushroom`
- `minecraft:short_dry_grass`
- `minecraft:skeleton_skull`
- `minecraft:skeleton_wall_skull`
- `minecraft:slime_block`
- `minecraft:sniffer_egg`
- `minecraft:sponge`
- `minecraft:structure_block`
- `minecraft:structure_void`
- `minecraft:tall_dry_grass`
- `minecraft:torch`
- `minecraft:tripwire`
- `minecraft:turtle_egg`
- `minecraft:verdant_froglight`
- `minecraft:void_air`
- `minecraft:wall_torch`
- `minecraft:water`
- `minecraft:white_banner`
- `minecraft:white_bed`
- `minecraft:white_candle_cake`
- `minecraft:white_carpet`
- `minecraft:white_wall_banner`
- `minecraft:white_wool`
- `minecraft:white_wool_slab`
- `minecraft:white_wool_stairs`
- `minecraft:wildflowers`
- `minecraft:wither_rose`
- `minecraft:wither_skeleton_skull`
- `minecraft:wither_skeleton_wall_skull`
- `minecraft:yellow_banner`
- `minecraft:yellow_bed`
- `minecraft:yellow_candle_cake`
- `minecraft:yellow_carpet`
- `minecraft:yellow_wall_banner`
- `minecraft:yellow_wool`
- `minecraft:yellow_wool_slab`
- `minecraft:yellow_wool_stairs`
- `minecraft:zombie_head`
- `minecraft:zombie_wall_head`
- `naturalist:alligator_egg`
- `naturalist:azure_froglass`
- `naturalist:cattail`
- `naturalist:chrysalis`
- `naturalist:duckweed`
- `naturalist:glow_goop`
- `naturalist:snail_eggs`
- `naturalist:teddy_bear`
- `naturalist:tortoise_egg`
- `naturalist:verdant_froglass`
- `netherdepthsupgrade:lava_glass`
- `netherdepthsupgrade:lava_sponge`
- `netherdepthsupgrade:nether_rice_roll_medley_block`
- `netherdepthsupgrade:wet_lava_sponge`
- `nourished_nether:basalt_ash_spawner`
- `nourished_nether:ectoplasm_block`
- `nourished_nether:ectoplasm_block_active`
- `nourished_nether:ghoulflower`
- `nourished_nether:hoglin_hide_block`
- `quark:ancient_button`
- `quark:ancient_chest`
- `quark:ancient_door`
- `quark:ancient_fence`
- `quark:ancient_fence_gate`
- `quark:ancient_hedge`
- `quark:ancient_ladder`
- `quark:ancient_leaf_carpet`
- `quark:ancient_leaves`
- `quark:ancient_log`
- `quark:ancient_planks`
- `quark:ancient_planks_slab`
- `quark:ancient_planks_stairs`
- `quark:ancient_planks_vertical_slab`
- `quark:ancient_post`
- `quark:ancient_pressure_plate`
- `quark:ancient_sapling`
- `quark:ancient_sign`
- `quark:ancient_trapdoor`
- `quark:ancient_wall_sign`
- `quark:ancient_wood`
- `quark:apple_crate`
- `quark:black_stool`
- `quark:blossom_button`
- `quark:blossom_chest`
- `quark:blossom_door`
- `quark:blossom_fence`
- `quark:blossom_fence_gate`
- `quark:blossom_ladder`
- `quark:blossom_log`
- `quark:blossom_planks`
- `quark:blossom_planks_slab`
- `quark:blossom_planks_stairs`
- `quark:blossom_planks_vertical_slab`
- `quark:blossom_post`
- `quark:blossom_pressure_plate`
- `quark:blossom_sign`
- `quark:blossom_trapdoor`
- `quark:blossom_wall_sign`
- `quark:blossom_wood`
- `quark:blue_blossom_hedge`
- `quark:blue_blossom_leaf_carpet`
- `quark:blue_blossom_leaves`
- `quark:blue_blossom_sapling`
- `quark:bonded_leather`
- `quark:bonded_rabbit_hide`
- `quark:bonded_ravager_hide`
- `quark:chute`
- `quark:cloud`
- `quark:crate`
- `quark:cut_vine`
- `quark:glow_lichen_growth`
- `quark:gray_stool`
- `quark:gunpowder_sack`
- `quark:hollow_ancient_log`
- `quark:hollow_blossom_log`
- `quark:lavender_blossom_hedge`
- `quark:lavender_blossom_leaf_carpet`
- `quark:lavender_blossom_leaves`
- `quark:lavender_blossom_sapling`
- `quark:light_gray_stool`
- `quark:magnetized_block`
- `quark:monster_box`
- `quark:orange_blossom_hedge`
- `quark:orange_blossom_leaf_carpet`
- `quark:orange_blossom_leaves`
- `quark:orange_blossom_sapling`
- `quark:orange_stool`
- `quark:pink_stool`
- `quark:red_blossom_hedge`
- `quark:red_blossom_leaf_carpet`
- `quark:red_blossom_leaves`
- `quark:red_blossom_sapling`
- `quark:rope`
- `quark:stick_block`
- `quark:stripped_ancient_log`
- `quark:stripped_ancient_post`
- `quark:stripped_ancient_wood`
- `quark:stripped_blossom_log`
- `quark:stripped_blossom_post`
- `quark:stripped_blossom_wood`
- `quark:tiny_potato`
- `quark:vertical_ancient_planks`
- `quark:vertical_blossom_planks`
- `quark:water_pink_petals`
- `quark:white_stool`
- `quark:yellow_blossom_hedge`
- `quark:yellow_blossom_leaf_carpet`
- `quark:yellow_blossom_leaves`
- `quark:yellow_blossom_sapling`
- `quark:yellow_stool`

## Allowed in none of the six dimensions

- `ecologics:potted_azalea_flower`
- `minecraft:acacia_hanging_sign`
- `minecraft:acacia_wall_hanging_sign`
- `minecraft:bamboo_hanging_sign`
- `minecraft:bamboo_wall_hanging_sign`
- `minecraft:blue_glazed_terracotta`
- `minecraft:blue_terracotta`
- `minecraft:brown_concrete_powder`
- `minecraft:brown_glazed_terracotta`
- `minecraft:brown_terracotta`
- `minecraft:calibrated_sculk_sensor`
- `minecraft:cherry_hanging_sign`
- `minecraft:cherry_wall_hanging_sign`
- `minecraft:crimson_hanging_sign`
- `minecraft:crimson_wall_hanging_sign`
- `minecraft:cyan_concrete_powder`
- `minecraft:cyan_glazed_terracotta`
- `minecraft:cyan_terracotta`
- `minecraft:dark_oak_hanging_sign`
- `minecraft:dark_oak_wall_hanging_sign`
- `minecraft:enchanting_table`
- `minecraft:ender_chest`
- `minecraft:jungle_hanging_sign`
- `minecraft:jungle_wall_hanging_sign`
- `minecraft:lava_cauldron`
- `minecraft:light_blue_glazed_terracotta`
- `minecraft:light_blue_terracotta`
- `minecraft:magenta_glazed_terracotta`
- `minecraft:magenta_terracotta`
- `minecraft:mangrove_hanging_sign`
- `minecraft:mangrove_wall_hanging_sign`
- `minecraft:mossy_cobblestone`
- `minecraft:mossy_cobblestone_slab`
- `minecraft:mossy_cobblestone_stairs`
- `minecraft:mossy_cobblestone_wall`
- `minecraft:mossy_stone_brick_slab`
- `minecraft:mossy_stone_brick_stairs`
- `minecraft:mossy_stone_brick_wall`
- `minecraft:mossy_stone_bricks`
- `minecraft:mud_brick_slab`
- `minecraft:mud_brick_stairs`
- `minecraft:mud_brick_wall`
- `minecraft:mud_bricks`
- `minecraft:muddy_mangrove_roots`
- `minecraft:packed_mud`
- `minecraft:potted_acacia_sapling`
- `minecraft:potted_allium`
- `minecraft:potted_azalea_bush`
- `minecraft:potted_bamboo`
- `minecraft:potted_blue_orchid`
- `minecraft:potted_cactus`
- `minecraft:potted_cherry_sapling`
- `minecraft:potted_crimson_fungus`
- `minecraft:potted_crimson_roots`
- `minecraft:potted_dark_oak_sapling`
- `minecraft:potted_dead_bush`
- `minecraft:potted_fern`
- `minecraft:potted_flowering_azalea_bush`
- `minecraft:potted_jungle_sapling`
- `minecraft:potted_mangrove_propagule`
- `minecraft:potted_pale_oak_sapling`
- `minecraft:potted_poplar_sapling`
- `minecraft:potted_spruce_sapling`
- `minecraft:potted_torchflower`
- `minecraft:potted_warped_fungus`
- `minecraft:potted_warped_roots`
- `minecraft:purple_glazed_terracotta`
- `minecraft:purple_terracotta`
- `minecraft:red_glazed_terracotta`
- `minecraft:red_terracotta`
- `minecraft:soul_lantern`
- `minecraft:target`
- `minecraft:warped_hanging_sign`
- `minecraft:warped_wall_hanging_sign`
- `minecraft:waxed_copper_bulb`
- `minecraft:waxed_exposed_copper_bulb`
- `minecraft:waxed_oxidized_copper_bulb`
- `minecraft:waxed_weathered_copper_bulb`
- `quark:acacia_trapped_chest`
- `quark:azalea_hanging_sign`
- `quark:azalea_trapped_chest`
- `quark:azalea_wall_hanging_sign`
- `quark:bamboo_trapped_chest`
- `quark:blackstone_bricks`
- `quark:blackstone_bricks_slab`
- `quark:blackstone_bricks_stairs`
- `quark:blackstone_bricks_vertical_slab`
- `quark:blackstone_bricks_wall`
- `quark:blue_shingles`
- `quark:blue_shingles_slab`
- `quark:blue_shingles_stairs`
- `quark:blue_shingles_vertical_slab`
- `quark:brown_framed_glass`
- `quark:brown_framed_glass_pane`
- `quark:brown_shingles`
- `quark:brown_shingles_slab`
- `quark:brown_shingles_stairs`
- `quark:brown_shingles_vertical_slab`
- `quark:carved_mud_bricks`
- `quark:cherry_trapped_chest`
- `quark:crimson_bookshelf`
- `quark:cyan_framed_glass`
- `quark:cyan_framed_glass_pane`
- `quark:cyan_shingles`
- `quark:cyan_shingles_slab`
- `quark:cyan_shingles_stairs`
- `quark:cyan_shingles_vertical_slab`
- `quark:dark_oak_trapped_chest`
- `quark:ender_watcher`
- `quark:green_framed_glass`
- `quark:green_framed_glass_pane`
- `quark:iron_rod`
- `quark:jungle_trapped_chest`
- `quark:light_blue_shingles`
- `quark:light_blue_shingles_slab`
- `quark:light_blue_shingles_stairs`
- `quark:light_blue_shingles_vertical_slab`
- `quark:magenta_shingles`
- `quark:magenta_shingles_slab`
- `quark:magenta_shingles_stairs`
- `quark:magenta_shingles_vertical_slab`
- `quark:mangrove_trapped_chest`
- `quark:midori_block`
- `quark:midori_block_slab`
- `quark:midori_block_stairs`
- `quark:midori_block_vertical_slab`
- `quark:midori_pillar`
- `quark:mossy_cobblestone_bricks`
- `quark:mossy_cobblestone_bricks_slab`
- `quark:mossy_cobblestone_bricks_stairs`
- `quark:mossy_cobblestone_bricks_vertical_slab`
- `quark:mossy_cobblestone_bricks_wall`
- `quark:mossy_cobblestone_vertical_slab`
- `quark:mossy_stone_brick_vertical_slab`
- `quark:mud_brick_lattice`
- `quark:mud_brick_vertical_slab`
- `quark:mud_pillar`
- `quark:netherrack_bricks`
- `quark:netherrack_bricks_slab`
- `quark:netherrack_bricks_stairs`
- `quark:netherrack_bricks_vertical_slab`
- `quark:netherrack_bricks_wall`
- `quark:paper_lantern`
- `quark:paper_lantern_sakura`
- `quark:potted_berries`
- `quark:potted_cave_vines`
- `quark:potted_chorus`
- `quark:potted_chorus_twist`
- `quark:potted_chorus_weeds`
- `quark:potted_cocoa_bean`
- `quark:potted_glow_shroom`
- `quark:potted_large_fern`
- `quark:potted_lilac`
- `quark:potted_melon`
- `quark:potted_nether_sprouts`
- `quark:potted_nether_wart`
- `quark:potted_peony`
- `quark:potted_pitcher_plant`
- `quark:potted_rose`
- `quark:potted_sea_pickle`
- `quark:potted_twisting_vines`
- `quark:potted_vine`
- `quark:potted_weeping_vines`
- `quark:purple_shingles`
- `quark:purple_shingles_slab`
- `quark:purple_shingles_stairs`
- `quark:purple_shingles_vertical_slab`
- `quark:red_shingles`
- `quark:red_shingles_slab`
- `quark:red_shingles_stairs`
- `quark:red_shingles_vertical_slab`
- `quark:redstone_randomizer`
- `quark:warped_bookshelf`

## Every registered block

| Block ID | Tropical | Savanna | Plains | Snowy | Desert | Nether |
| --- | --- | --- | --- | --- | --- | --- |
| `alexsmobs:banana_peel` | Yes | Yes | Yes | Yes | Yes | Yes |
| `alexsmobs:banana_slug_slime_block` | — | Yes | Yes | Yes | Yes | Yes |
| `alexsmobs:bison_carpet` | Yes | Yes | Yes | Yes | Yes | Yes |
| `alexsmobs:bison_fur_block` | Yes | Yes | Yes | Yes | Yes | Yes |
| `alexsmobs:caiman_egg` | Yes | Yes | Yes | Yes | Yes | Yes |
| `alexsmobs:capsid` | — | — | Yes | Yes | Yes | Yes |
| `alexsmobs:crocodile_egg` | Yes | Yes | Yes | Yes | Yes | Yes |
| `alexsmobs:crystalized_banana_slug_mucus` | — | — | Yes | Yes | Yes | Yes |
| `alexsmobs:ender_residue` | Yes | Yes | Yes | Yes | Yes | Yes |
| `alexsmobs:gustmaker` | — | — | Yes | Yes | Yes | Yes |
| `alexsmobs:hummingbird_feeder` | — | — | Yes | Yes | Yes | Yes |
| `alexsmobs:leafcutter_ant_chamber` | — | Yes | Yes | Yes | Yes | Yes |
| `alexsmobs:leafcutter_anthill` | — | Yes | Yes | Yes | Yes | Yes |
| `alexsmobs:platypus_egg` | Yes | Yes | Yes | Yes | Yes | Yes |
| `alexsmobs:rainbow_glass` | — | — | Yes | Yes | Yes | Yes |
| `alexsmobs:red_sand_circle` | Yes | Yes | Yes | Yes | Yes | Yes |
| `alexsmobs:sand_circle` | Yes | Yes | Yes | Yes | Yes | Yes |
| `alexsmobs:sculk_boomer` | Yes | Yes | Yes | Yes | Yes | Yes |
| `alexsmobs:skunk_spray` | Yes | Yes | Yes | Yes | Yes | Yes |
| `alexsmobs:straddlite_block` | — | — | Yes | Yes | Yes | Yes |
| `alexsmobs:terrapin_egg` | Yes | Yes | Yes | Yes | Yes | Yes |
| `alexsmobs:transmutation_table` | — | — | Yes | Yes | Yes | Yes |
| `alexsmobs:triops_eggs` | Yes | Yes | Yes | Yes | Yes | Yes |
| `alexsmobs:void_worm_beak` | — | — | Yes | Yes | Yes | Yes |
| `alexsmobs:void_worm_effigy` | Yes | Yes | Yes | Yes | Yes | Yes |
| `aoemod:azalea_sapling` | Yes | — | — | — | — | — |
| `aoemod:basalt_gold_ore` | — | — | — | — | — | Yes |
| `aoemod:basalt_quartz_ore` | — | — | — | — | — | Yes |
| `aoemod:blackstone_gold_ore` | — | — | — | — | — | Yes |
| `aoemod:blackstone_quartz_ore` | — | — | — | — | — | Yes |
| `aoemod:blue_berry_bush` | Yes | — | — | Yes | — | — |
| `aoemod:closed_eyeblossom` | — | — | — | — | — | Yes |
| `aoemod:copper_chain` | — | — | Yes | Yes | Yes | Yes |
| `aoemod:desert_grass` | — | — | — | — | Yes | — |
| `aoemod:desert_tallgrass` | — | — | — | — | Yes | — |
| `aoemod:jungle_sweet_berry_bush` | Yes | — | — | — | — | — |
| `aoemod:open_eyeblossom` | — | — | — | — | — | Yes |
| `aoemod:pale_hanging_moss` | — | — | — | — | — | Yes |
| `aoemod:pale_moss_block` | — | — | — | — | — | Yes |
| `aoemod:pale_moss_carpet` | — | — | — | — | — | Yes |
| `aoemod:pale_oak_button` | — | — | — | — | — | Yes |
| `aoemod:pale_oak_door` | — | — | — | — | — | Yes |
| `aoemod:pale_oak_fence` | — | — | — | — | — | Yes |
| `aoemod:pale_oak_fence_gate` | — | — | — | — | — | Yes |
| `aoemod:pale_oak_leaves` | — | — | — | — | — | Yes |
| `aoemod:pale_oak_log` | — | — | — | — | — | Yes |
| `aoemod:pale_oak_planks` | — | — | — | — | — | Yes |
| `aoemod:pale_oak_pressure_plate` | — | — | — | — | — | Yes |
| `aoemod:pale_oak_sapling` | — | — | — | — | — | Yes |
| `aoemod:pale_oak_slab` | — | — | — | — | — | Yes |
| `aoemod:pale_oak_stairs` | — | — | — | — | — | Yes |
| `aoemod:pale_oak_trapdoor` | — | — | — | — | — | Yes |
| `aoemod:pale_oak_wood` | — | — | — | — | — | Yes |
| `aoemod:soul_berry_bush` | — | — | — | — | — | Yes |
| `aoemod:soul_sand_gold_ore` | — | — | — | — | — | Yes |
| `aoemod:soul_sand_quartz_ore` | — | — | — | — | — | Yes |
| `aoemod:soul_soil_gold_ore` | — | — | — | — | — | Yes |
| `aoemod:soul_soil_quartz_ore` | — | — | — | — | — | Yes |
| `aoemod:stripped_pale_oak_log` | — | — | — | — | — | Yes |
| `aoemod:stripped_pale_oak_wood` | — | — | — | — | — | Yes |
| `aoemod:sunflower` | Yes | Yes | Yes | — | — | — |
| `bettercarrots:nether_carrot_roots` | Yes | Yes | Yes | Yes | Yes | Yes |
| `born_in_chaos_v1:argillite_lamp` | Yes | Yes | Yes | Yes | Yes | Yes |
| `born_in_chaos_v1:black_argillite` | — | — | Yes | Yes | Yes | Yes |
| `born_in_chaos_v1:black_argillite_brick` | — | — | Yes | Yes | Yes | Yes |
| `born_in_chaos_v1:black_argillite_brick_n` | — | — | Yes | Yes | Yes | Yes |
| `born_in_chaos_v1:black_argillite_brick_slab` | — | — | Yes | Yes | Yes | Yes |
| `born_in_chaos_v1:black_argillite_brick_slab_n` | — | — | Yes | Yes | Yes | Yes |
| `born_in_chaos_v1:black_argillite_brick_stairs` | — | — | Yes | Yes | Yes | Yes |
| `born_in_chaos_v1:black_argillite_brick_stairs_n` | — | — | Yes | Yes | Yes | Yes |
| `born_in_chaos_v1:black_argillite_brick_wall` | — | — | Yes | Yes | Yes | Yes |
| `born_in_chaos_v1:black_argillite_brick_wall_n` | — | — | Yes | Yes | Yes | Yes |
| `born_in_chaos_v1:black_argillite_button` | — | — | Yes | Yes | Yes | Yes |
| `born_in_chaos_v1:black_argillite_column` | — | — | Yes | Yes | Yes | Yes |
| `born_in_chaos_v1:black_argillite_column_n` | — | — | Yes | Yes | Yes | Yes |
| `born_in_chaos_v1:black_argillite_column_slab` | — | — | Yes | Yes | Yes | Yes |
| `born_in_chaos_v1:black_argillite_column_slab_n` | — | — | Yes | Yes | Yes | Yes |
| `born_in_chaos_v1:black_argillite_n` | — | — | Yes | Yes | Yes | Yes |
| `born_in_chaos_v1:black_argillite_slab` | — | — | Yes | Yes | Yes | Yes |
| `born_in_chaos_v1:black_argillite_slab_n` | — | — | Yes | Yes | Yes | Yes |
| `born_in_chaos_v1:black_argillite_stairs` | — | — | Yes | Yes | Yes | Yes |
| `born_in_chaos_v1:black_argillite_stairs_n` | — | — | Yes | Yes | Yes | Yes |
| `born_in_chaos_v1:black_argillite_wall` | — | — | Yes | Yes | Yes | Yes |
| `born_in_chaos_v1:black_argillite_wall_n` | — | — | Yes | Yes | Yes | Yes |
| `born_in_chaos_v1:black_argillitepressureplate` | — | — | Yes | Yes | Yes | Yes |
| `born_in_chaos_v1:bundle_of_bones` | Yes | Yes | Yes | Yes | Yes | Yes |
| `born_in_chaos_v1:c_black_argillite_brick_n` | — | — | Yes | Yes | Yes | Yes |
| `born_in_chaos_v1:carved_black_argillite` | — | — | Yes | Yes | Yes | Yes |
| `born_in_chaos_v1:carved_black_argillite_n` | — | — | Yes | Yes | Yes | Yes |
| `born_in_chaos_v1:chipped_black_argillite_brick` | — | — | Yes | Yes | Yes | Yes |
| `born_in_chaos_v1:closed_hound_trap` | Yes | Yes | Yes | Yes | Yes | Yes |
| `born_in_chaos_v1:cobweb_cover` | Yes | Yes | Yes | Yes | Yes | Yes |
| `born_in_chaos_v1:creepy_nutcracker` | Yes | Yes | Yes | Yes | Yes | Yes |
| `born_in_chaos_v1:cultivated_pumpkin` | Yes | Yes | Yes | Yes | Yes | Yes |
| `born_in_chaos_v1:cursed_scarlet_ornate_glass_pane` | — | — | Yes | Yes | Yes | Yes |
| `born_in_chaos_v1:dark_grid` | — | — | Yes | Yes | Yes | Yes |
| `born_in_chaos_v1:dark_ice` | — | — | — | Yes | — | — |
| `born_in_chaos_v1:dark_metal_block` | — | — | Yes | Yes | Yes | Yes |
| `born_in_chaos_v1:dark_metal_deposit` | — | — | Yes | Yes | Yes | Yes |
| `born_in_chaos_v1:dark_stained_glass` | Yes | Yes | Yes | Yes | Yes | Yes |
| `born_in_chaos_v1:dark_stained_glass_panel` | Yes | Yes | Yes | Yes | Yes | Yes |
| `born_in_chaos_v1:evil_carved_pumpkin` | Yes | Yes | Yes | Yes | Yes | Yes |
| `born_in_chaos_v1:fel_soil` | — | — | Yes | Yes | Yes | Yes |
| `born_in_chaos_v1:fel_soil_destructible` | — | — | Yes | Yes | Yes | Yes |
| `born_in_chaos_v1:fired_black_argillite` | — | — | Yes | Yes | Yes | Yes |
| `born_in_chaos_v1:fired_black_argillite_n` | — | — | Yes | Yes | Yes | Yes |
| `born_in_chaos_v1:flaming_evil_pumpkin` | Yes | Yes | Yes | Yes | Yes | Yes |
| `born_in_chaos_v1:fragile_scorched_boards` | Yes | Yes | Yes | Yes | Yes | Yes |
| `born_in_chaos_v1:gnawed_bones` | Yes | Yes | Yes | Yes | Yes | Yes |
| `born_in_chaos_v1:green_stained_glass` | Yes | Yes | Yes | Yes | Yes | Yes |
| `born_in_chaos_v1:green_stained_glass_panel` | Yes | Yes | Yes | Yes | Yes | Yes |
| `born_in_chaos_v1:hound_trap` | Yes | Yes | Yes | Yes | Yes | Yes |
| `born_in_chaos_v1:infected_deepslate_diamond_ore` | — | — | — | Yes | — | — |
| `born_in_chaos_v1:infected_diamond_ore` | — | — | — | Yes | — | — |
| `born_in_chaos_v1:infernal_evil_pumpkin` | Yes | Yes | Yes | Yes | Yes | Yes |
| `born_in_chaos_v1:infernal_evil_pumpkin_s` | Yes | Yes | Yes | Yes | Yes | Yes |
| `born_in_chaos_v1:lifestealer_skull` | Yes | Yes | Yes | Yes | Yes | Yes |
| `born_in_chaos_v1:marigolds` | Yes | Yes | Yes | Yes | Yes | Yes |
| `born_in_chaos_v1:mesh_door` | — | — | Yes | Yes | Yes | Yes |
| `born_in_chaos_v1:mossy_black_argillite_brick` | — | — | Yes | Yes | Yes | Yes |
| `born_in_chaos_v1:mossy_black_argillite_brick_n` | — | — | Yes | Yes | Yes | Yes |
| `born_in_chaos_v1:nightmare_stalker_skull` | Yes | Yes | Yes | Yes | Yes | Yes |
| `born_in_chaos_v1:orange_stained_glass` | Yes | Yes | Yes | Yes | Yes | Yes |
| `born_in_chaos_v1:orange_stained_glass_panel` | Yes | Yes | Yes | Yes | Yes | Yes |
| `born_in_chaos_v1:pile_of_skulls` | Yes | Yes | Yes | Yes | Yes | Yes |
| `born_in_chaos_v1:puddleof_stimulation` | Yes | Yes | Yes | Yes | Yes | Yes |
| `born_in_chaos_v1:puddleofintoxication` | Yes | Yes | Yes | Yes | Yes | Yes |
| `born_in_chaos_v1:purple_stained_glass` | Yes | Yes | Yes | Yes | Yes | Yes |
| `born_in_chaos_v1:purple_stained_glass_panel` | Yes | Yes | Yes | Yes | Yes | Yes |
| `born_in_chaos_v1:river_mint` | Yes | Yes | Yes | Yes | Yes | Yes |
| `born_in_chaos_v1:rotten_infernal_pumpkin` | Yes | Yes | Yes | Yes | Yes | Yes |
| `born_in_chaos_v1:rotten_soil` | — | Yes | Yes | Yes | Yes | Yes |
| `born_in_chaos_v1:scarlet_stained_glass` | Yes | Yes | Yes | Yes | Yes | Yes |
| `born_in_chaos_v1:scarlet_stained_glass_panel` | Yes | Yes | Yes | Yes | Yes | Yes |
| `born_in_chaos_v1:scorched_log` | Yes | Yes | Yes | Yes | Yes | Yes |
| `born_in_chaos_v1:scorched_planks` | Yes | Yes | Yes | Yes | Yes | Yes |
| `born_in_chaos_v1:scorched_planks_button` | Yes | Yes | Yes | Yes | Yes | Yes |
| `born_in_chaos_v1:scorched_planks_door` | Yes | Yes | Yes | Yes | Yes | Yes |
| `born_in_chaos_v1:scorched_planks_fence` | Yes | Yes | Yes | Yes | Yes | Yes |
| `born_in_chaos_v1:scorched_planks_fence_gate` | Yes | Yes | Yes | Yes | Yes | Yes |
| `born_in_chaos_v1:scorched_planks_pressure_plates` | Yes | Yes | Yes | Yes | Yes | Yes |
| `born_in_chaos_v1:scorched_planks_slab` | Yes | Yes | Yes | Yes | Yes | Yes |
| `born_in_chaos_v1:scorched_planks_stairs` | Yes | Yes | Yes | Yes | Yes | Yes |
| `born_in_chaos_v1:scorched_planks_trapdoor` | Yes | Yes | Yes | Yes | Yes | Yes |
| `born_in_chaos_v1:scorched_wood` | Yes | Yes | Yes | Yes | Yes | Yes |
| `born_in_chaos_v1:smoldering_scorched_log` | Yes | Yes | Yes | Yes | Yes | Yes |
| `born_in_chaos_v1:smoldering_scorched_wood` | Yes | Yes | Yes | Yes | Yes | Yes |
| `born_in_chaos_v1:spiny_shell_trap` | — | — | Yes | Yes | Yes | Yes |
| `born_in_chaos_v1:spooky_snowman_head` | — | Yes | Yes | Yes | Yes | Yes |
| `born_in_chaos_v1:stained_black_argillite_brick` | — | — | Yes | Yes | Yes | Yes |
| `born_in_chaos_v1:stained_black_argillite_brick_n` | — | — | Yes | Yes | Yes | Yes |
| `born_in_chaos_v1:stripped_scorched_log` | Yes | Yes | Yes | Yes | Yes | Yes |
| `born_in_chaos_v1:stripped_scorched_wood` | Yes | Yes | Yes | Yes | Yes | Yes |
| `born_in_chaos_v1:tombstone_2003wise` | — | — | Yes | Yes | Yes | Yes |
| `born_in_chaos_v1:tombstone_2_dling` | — | — | Yes | Yes | Yes | Yes |
| `born_in_chaos_v1:tombstone_carrion` | — | — | Yes | Yes | Yes | Yes |
| `born_in_chaos_v1:tombstone_d_4rkdevilx` | — | — | Yes | Yes | Yes | Yes |
| `born_in_chaos_v1:tombstone_dark_titan_066` | — | — | Yes | Yes | Yes | Yes |
| `born_in_chaos_v1:tombstone_derivas` | — | — | Yes | Yes | Yes | Yes |
| `born_in_chaos_v1:tombstone_euthymia` | — | — | Yes | Yes | Yes | Yes |
| `born_in_chaos_v1:tombstone_feral` | — | — | Yes | Yes | Yes | Yes |
| `born_in_chaos_v1:tombstone_fubuki_banzai` | — | — | Yes | Yes | Yes | Yes |
| `born_in_chaos_v1:tombstone_memesus` | — | — | Yes | Yes | Yes | Yes |
| `born_in_chaos_v1:tombstone_mike_rory` | — | — | Yes | Yes | Yes | Yes |
| `born_in_chaos_v1:tombstone_nino_4416` | — | — | Yes | Yes | Yes | Yes |
| `born_in_chaos_v1:tombstone_orion` | — | — | Yes | Yes | Yes | Yes |
| `born_in_chaos_v1:tombstone_petasi` | — | — | Yes | Yes | Yes | Yes |
| `born_in_chaos_v1:tombstone_plug` | Yes | Yes | Yes | Yes | Yes | Yes |
| `born_in_chaos_v1:tombstone_rotborne` | — | — | Yes | Yes | Yes | Yes |
| `born_in_chaos_v1:tombstone_somfunambulist` | — | — | Yes | Yes | Yes | Yes |
| `born_in_chaos_v1:tombstone_tem_187` | — | — | Yes | Yes | Yes | Yes |
| `born_in_chaos_v1:tombstone_the_gentleman_frog` | — | — | Yes | Yes | Yes | Yes |
| `born_in_chaos_v1:webbed_cobblestone` | — | — | Yes | Yes | Yes | Yes |
| `citadel:lectern` | Yes | Yes | Yes | Yes | Yes | Yes |
| `culturaldelights:avocado_crate` | Yes | Yes | Yes | Yes | Yes | Yes |
| `culturaldelights:avocado_leaves` | Yes | Yes | Yes | Yes | Yes | Yes |
| `culturaldelights:avocado_log` | Yes | Yes | Yes | Yes | Yes | Yes |
| `culturaldelights:avocado_pit` | Yes | Yes | Yes | Yes | Yes | Yes |
| `culturaldelights:avocado_sapling` | Yes | Yes | Yes | Yes | Yes | Yes |
| `culturaldelights:avocado_wood` | Yes | Yes | Yes | Yes | Yes | Yes |
| `culturaldelights:corn` | Yes | Yes | Yes | Yes | Yes | Yes |
| `culturaldelights:corn_cob_crate` | Yes | Yes | Yes | Yes | Yes | Yes |
| `culturaldelights:corn_upper` | Yes | Yes | Yes | Yes | Yes | Yes |
| `culturaldelights:cucumber_crate` | Yes | Yes | Yes | Yes | Yes | Yes |
| `culturaldelights:cucumbers` | Yes | Yes | Yes | Yes | Yes | Yes |
| `culturaldelights:eggplant_crate` | Yes | Yes | Yes | Yes | Yes | Yes |
| `culturaldelights:eggplants` | Yes | Yes | Yes | Yes | Yes | Yes |
| `culturaldelights:exotic_roll_medley` | Yes | Yes | Yes | Yes | Yes | Yes |
| `culturaldelights:fruiting_avocado_leaves` | Yes | Yes | Yes | Yes | Yes | Yes |
| `culturaldelights:pickle_crate` | Yes | Yes | Yes | Yes | Yes | Yes |
| `culturaldelights:white_eggplant_crate` | Yes | Yes | Yes | Yes | Yes | Yes |
| `culturaldelights:wild_corn` | Yes | Yes | Yes | Yes | Yes | Yes |
| `culturaldelights:wild_cucumbers` | Yes | Yes | Yes | Yes | Yes | Yes |
| `culturaldelights:wild_eggplants` | Yes | Yes | Yes | Yes | Yes | Yes |
| `ecologics:azalea_button` | Yes | — | — | — | — | — |
| `ecologics:azalea_door` | Yes | — | — | — | — | — |
| `ecologics:azalea_fence` | Yes | — | — | — | — | — |
| `ecologics:azalea_fence_gate` | Yes | — | — | — | — | — |
| `ecologics:azalea_flower` | Yes | — | — | — | — | — |
| `ecologics:azalea_hanging_sign` | Yes | — | — | — | — | — |
| `ecologics:azalea_log` | Yes | — | — | — | — | — |
| `ecologics:azalea_planks` | Yes | — | — | — | — | — |
| `ecologics:azalea_pressure_plate` | Yes | — | — | — | — | — |
| `ecologics:azalea_sign` | Yes | — | — | — | — | — |
| `ecologics:azalea_slab` | Yes | — | — | — | — | — |
| `ecologics:azalea_stairs` | Yes | — | — | — | — | — |
| `ecologics:azalea_trapdoor` | Yes | — | — | — | — | — |
| `ecologics:azalea_wall_hanging_sign` | Yes | — | — | — | — | — |
| `ecologics:azalea_wall_sign` | Yes | — | — | — | — | — |
| `ecologics:azalea_wood` | Yes | — | — | — | — | — |
| `ecologics:coconut` | Yes | Yes | Yes | Yes | Yes | Yes |
| `ecologics:coconut_button` | Yes | Yes | Yes | Yes | Yes | Yes |
| `ecologics:coconut_door` | Yes | Yes | Yes | Yes | Yes | Yes |
| `ecologics:coconut_fence` | Yes | Yes | Yes | Yes | Yes | Yes |
| `ecologics:coconut_fence_gate` | Yes | Yes | Yes | Yes | Yes | Yes |
| `ecologics:coconut_hanging_sign` | Yes | Yes | Yes | Yes | Yes | Yes |
| `ecologics:coconut_leaves` | Yes | Yes | Yes | Yes | Yes | Yes |
| `ecologics:coconut_log` | Yes | Yes | Yes | Yes | Yes | Yes |
| `ecologics:coconut_planks` | Yes | Yes | Yes | Yes | Yes | Yes |
| `ecologics:coconut_pressure_plate` | Yes | Yes | Yes | Yes | Yes | Yes |
| `ecologics:coconut_seedling` | Yes | Yes | Yes | Yes | Yes | Yes |
| `ecologics:coconut_sign` | Yes | Yes | Yes | Yes | Yes | Yes |
| `ecologics:coconut_slab` | Yes | Yes | Yes | Yes | Yes | Yes |
| `ecologics:coconut_stairs` | Yes | Yes | Yes | Yes | Yes | Yes |
| `ecologics:coconut_trapdoor` | Yes | Yes | Yes | Yes | Yes | Yes |
| `ecologics:coconut_wall_hanging_sign` | Yes | Yes | Yes | Yes | Yes | Yes |
| `ecologics:coconut_wall_sign` | Yes | Yes | Yes | Yes | Yes | Yes |
| `ecologics:coconut_wood` | Yes | Yes | Yes | Yes | Yes | Yes |
| `ecologics:flowering_azalea_door` | Yes | — | — | — | — | — |
| `ecologics:flowering_azalea_fence` | Yes | — | — | — | — | — |
| `ecologics:flowering_azalea_fence_gate` | Yes | — | — | — | — | — |
| `ecologics:flowering_azalea_hanging_sign` | Yes | — | — | — | — | — |
| `ecologics:flowering_azalea_log` | Yes | — | — | — | — | — |
| `ecologics:flowering_azalea_planks` | Yes | — | — | — | — | — |
| `ecologics:flowering_azalea_sign` | Yes | — | — | — | — | — |
| `ecologics:flowering_azalea_slab` | Yes | — | — | — | — | — |
| `ecologics:flowering_azalea_stairs` | Yes | — | — | — | — | — |
| `ecologics:flowering_azalea_trapdoor` | Yes | — | — | — | — | — |
| `ecologics:flowering_azalea_wall_hanging_sign` | Yes | — | — | — | — | — |
| `ecologics:flowering_azalea_wall_sign` | Yes | — | — | — | — | — |
| `ecologics:flowering_azalea_wood` | Yes | — | — | — | — | — |
| `ecologics:hanging_coconut` | Yes | Yes | Yes | Yes | Yes | Yes |
| `ecologics:ice_brick_slab` | — | — | — | Yes | — | — |
| `ecologics:ice_brick_stairs` | — | — | — | Yes | — | — |
| `ecologics:ice_brick_wall` | — | — | — | Yes | — | — |
| `ecologics:ice_bricks` | — | — | — | Yes | — | — |
| `ecologics:moss_layer` | Yes | Yes | Yes | Yes | Yes | Yes |
| `ecologics:pot` | — | — | Yes | Yes | Yes | Yes |
| `ecologics:potted_azalea_flower` | — | — | — | — | — | — |
| `ecologics:potted_coconut_seedling` | — | — | Yes | — | — | — |
| `ecologics:potted_walnut_sapling` | — | — | Yes | — | — | — |
| `ecologics:prickly_pear` | Yes | Yes | Yes | Yes | Yes | Yes |
| `ecologics:sandcastle` | — | Yes | Yes | Yes | Yes | Yes |
| `ecologics:seashell` | Yes | Yes | Yes | Yes | Yes | Yes |
| `ecologics:seashell_block` | — | — | Yes | Yes | Yes | Yes |
| `ecologics:seashell_tile_slab` | — | — | Yes | Yes | Yes | Yes |
| `ecologics:seashell_tile_stairs` | — | — | Yes | Yes | Yes | Yes |
| `ecologics:seashell_tile_wall` | — | — | Yes | Yes | Yes | Yes |
| `ecologics:seashell_tiles` | — | — | Yes | Yes | Yes | Yes |
| `ecologics:snow_brick_slab` | — | — | — | Yes | — | — |
| `ecologics:snow_brick_stairs` | — | — | — | Yes | — | — |
| `ecologics:snow_brick_wall` | — | — | — | Yes | — | — |
| `ecologics:snow_bricks` | — | — | — | Yes | — | — |
| `ecologics:stripped_azalea_log` | Yes | — | — | — | — | — |
| `ecologics:stripped_azalea_wood` | Yes | — | — | — | — | — |
| `ecologics:stripped_coconut_log` | Yes | Yes | Yes | Yes | Yes | Yes |
| `ecologics:stripped_coconut_wood` | Yes | Yes | Yes | Yes | Yes | Yes |
| `ecologics:stripped_walnut_log` | Yes | Yes | Yes | Yes | Yes | Yes |
| `ecologics:stripped_walnut_wood` | Yes | Yes | Yes | Yes | Yes | Yes |
| `ecologics:surface_moss` | Yes | Yes | Yes | Yes | Yes | Yes |
| `ecologics:thin_ice` | — | — | — | Yes | — | — |
| `ecologics:walnut_button` | Yes | Yes | Yes | Yes | Yes | Yes |
| `ecologics:walnut_door` | Yes | Yes | Yes | Yes | Yes | Yes |
| `ecologics:walnut_fence` | Yes | Yes | Yes | Yes | Yes | Yes |
| `ecologics:walnut_fence_gate` | Yes | Yes | Yes | Yes | Yes | Yes |
| `ecologics:walnut_hanging_sign` | Yes | Yes | Yes | Yes | Yes | Yes |
| `ecologics:walnut_leaves` | Yes | Yes | Yes | Yes | Yes | Yes |
| `ecologics:walnut_log` | Yes | Yes | Yes | Yes | Yes | Yes |
| `ecologics:walnut_planks` | Yes | Yes | Yes | Yes | Yes | Yes |
| `ecologics:walnut_pressure_plate` | Yes | Yes | Yes | Yes | Yes | Yes |
| `ecologics:walnut_sapling` | Yes | Yes | Yes | Yes | Yes | Yes |
| `ecologics:walnut_sign` | Yes | Yes | Yes | Yes | Yes | Yes |
| `ecologics:walnut_slab` | Yes | Yes | Yes | Yes | Yes | Yes |
| `ecologics:walnut_stairs` | Yes | Yes | Yes | Yes | Yes | Yes |
| `ecologics:walnut_trapdoor` | Yes | Yes | Yes | Yes | Yes | Yes |
| `ecologics:walnut_wall_hanging_sign` | Yes | Yes | Yes | Yes | Yes | Yes |
| `ecologics:walnut_wall_sign` | Yes | Yes | Yes | Yes | Yes | Yes |
| `ecologics:walnut_wood` | Yes | Yes | Yes | Yes | Yes | Yes |
| `farmersdelight:acacia_cabinet` | — | Yes | — | — | — | — |
| `farmersdelight:apple_pie` | Yes | Yes | Yes | — | — | — |
| `farmersdelight:bamboo_cabinet` | Yes | — | — | — | — | — |
| `farmersdelight:basket` | Yes | Yes | Yes | Yes | Yes | Yes |
| `farmersdelight:beetroot_crate` | Yes | Yes | Yes | Yes | Yes | Yes |
| `farmersdelight:birch_cabinet` | — | — | Yes | — | — | — |
| `farmersdelight:black_canvas_sign` | Yes | Yes | Yes | Yes | Yes | Yes |
| `farmersdelight:black_canvas_wall_sign` | Yes | Yes | Yes | Yes | Yes | Yes |
| `farmersdelight:black_hanging_canvas_sign` | Yes | Yes | Yes | Yes | Yes | Yes |
| `farmersdelight:black_wall_hanging_canvas_sign` | Yes | Yes | Yes | Yes | Yes | Yes |
| `farmersdelight:blue_canvas_sign` | Yes | Yes | Yes | Yes | Yes | Yes |
| `farmersdelight:blue_canvas_wall_sign` | Yes | Yes | Yes | Yes | Yes | Yes |
| `farmersdelight:blue_hanging_canvas_sign` | Yes | Yes | Yes | Yes | Yes | Yes |
| `farmersdelight:blue_wall_hanging_canvas_sign` | Yes | Yes | Yes | Yes | Yes | Yes |
| `farmersdelight:brown_canvas_sign` | Yes | Yes | Yes | Yes | Yes | Yes |
| `farmersdelight:brown_canvas_wall_sign` | Yes | Yes | Yes | Yes | Yes | Yes |
| `farmersdelight:brown_hanging_canvas_sign` | Yes | Yes | Yes | Yes | Yes | Yes |
| `farmersdelight:brown_mushroom_colony` | Yes | Yes | Yes | Yes | Yes | Yes |
| `farmersdelight:brown_wall_hanging_canvas_sign` | Yes | Yes | Yes | Yes | Yes | Yes |
| `farmersdelight:budding_tomatoes` | Yes | — | — | — | — | — |
| `farmersdelight:cabbage_crate` | Yes | Yes | Yes | Yes | Yes | Yes |
| `farmersdelight:cabbages` | Yes | — | — | — | — | — |
| `farmersdelight:canvas_rug` | Yes | Yes | Yes | Yes | Yes | Yes |
| `farmersdelight:canvas_sign` | Yes | Yes | Yes | Yes | Yes | Yes |
| `farmersdelight:canvas_wall_sign` | Yes | Yes | Yes | Yes | Yes | Yes |
| `farmersdelight:carrot_crate` | Yes | Yes | Yes | Yes | Yes | Yes |
| `farmersdelight:cherry_cabinet` | Yes | — | — | — | — | — |
| `farmersdelight:chocolate_pie` | Yes | — | — | — | — | — |
| `farmersdelight:cooking_pot` | — | — | Yes | Yes | Yes | Yes |
| `farmersdelight:crimson_cabinet` | — | — | — | — | — | Yes |
| `farmersdelight:cutting_board` | Yes | Yes | Yes | Yes | Yes | Yes |
| `farmersdelight:cyan_canvas_sign` | Yes | Yes | Yes | Yes | Yes | Yes |
| `farmersdelight:cyan_canvas_wall_sign` | Yes | Yes | Yes | Yes | Yes | Yes |
| `farmersdelight:cyan_hanging_canvas_sign` | Yes | Yes | Yes | Yes | Yes | Yes |
| `farmersdelight:cyan_wall_hanging_canvas_sign` | Yes | Yes | Yes | Yes | Yes | Yes |
| `farmersdelight:dark_oak_cabinet` | Yes | — | — | — | — | — |
| `farmersdelight:full_tatami_mat` | Yes | Yes | Yes | Yes | Yes | Yes |
| `farmersdelight:gray_canvas_sign` | Yes | Yes | Yes | Yes | Yes | Yes |
| `farmersdelight:gray_canvas_wall_sign` | Yes | Yes | Yes | Yes | Yes | Yes |
| `farmersdelight:gray_hanging_canvas_sign` | Yes | Yes | Yes | Yes | Yes | Yes |
| `farmersdelight:gray_wall_hanging_canvas_sign` | Yes | Yes | Yes | Yes | Yes | Yes |
| `farmersdelight:green_canvas_sign` | Yes | Yes | Yes | Yes | Yes | Yes |
| `farmersdelight:green_canvas_wall_sign` | Yes | Yes | Yes | Yes | Yes | Yes |
| `farmersdelight:green_hanging_canvas_sign` | Yes | Yes | Yes | Yes | Yes | Yes |
| `farmersdelight:green_wall_hanging_canvas_sign` | Yes | Yes | Yes | Yes | Yes | Yes |
| `farmersdelight:half_tatami_mat` | Yes | Yes | Yes | Yes | Yes | Yes |
| `farmersdelight:hanging_canvas_sign` | Yes | Yes | Yes | Yes | Yes | Yes |
| `farmersdelight:honey_glazed_ham_block` | Yes | Yes | Yes | Yes | Yes | Yes |
| `farmersdelight:jungle_cabinet` | Yes | — | — | — | — | — |
| `farmersdelight:light_blue_canvas_sign` | Yes | Yes | Yes | Yes | Yes | Yes |
| `farmersdelight:light_blue_canvas_wall_sign` | Yes | Yes | Yes | Yes | Yes | Yes |
| `farmersdelight:light_blue_hanging_canvas_sign` | Yes | Yes | Yes | Yes | Yes | Yes |
| `farmersdelight:light_blue_wall_hanging_canvas_sign` | Yes | Yes | Yes | Yes | Yes | Yes |
| `farmersdelight:light_gray_canvas_sign` | Yes | Yes | Yes | Yes | Yes | Yes |
| `farmersdelight:light_gray_canvas_wall_sign` | Yes | Yes | Yes | Yes | Yes | Yes |
| `farmersdelight:light_gray_hanging_canvas_sign` | Yes | Yes | Yes | Yes | Yes | Yes |
| `farmersdelight:light_gray_wall_hanging_canvas_sign` | Yes | Yes | Yes | Yes | Yes | Yes |
| `farmersdelight:lime_canvas_sign` | Yes | Yes | Yes | Yes | Yes | Yes |
| `farmersdelight:lime_canvas_wall_sign` | Yes | Yes | Yes | Yes | Yes | Yes |
| `farmersdelight:lime_hanging_canvas_sign` | Yes | Yes | Yes | Yes | Yes | Yes |
| `farmersdelight:lime_wall_hanging_canvas_sign` | Yes | Yes | Yes | Yes | Yes | Yes |
| `farmersdelight:magenta_canvas_sign` | Yes | Yes | Yes | Yes | Yes | Yes |
| `farmersdelight:magenta_canvas_wall_sign` | Yes | Yes | Yes | Yes | Yes | Yes |
| `farmersdelight:magenta_hanging_canvas_sign` | Yes | Yes | Yes | Yes | Yes | Yes |
| `farmersdelight:magenta_wall_hanging_canvas_sign` | Yes | Yes | Yes | Yes | Yes | Yes |
| `farmersdelight:mangrove_cabinet` | Yes | — | — | — | — | — |
| `farmersdelight:oak_cabinet` | Yes | — | Yes | — | — | — |
| `farmersdelight:onion_crate` | Yes | Yes | Yes | Yes | Yes | Yes |
| `farmersdelight:onions` | Yes | — | — | — | — | — |
| `farmersdelight:orange_canvas_sign` | Yes | Yes | Yes | Yes | Yes | Yes |
| `farmersdelight:orange_canvas_wall_sign` | Yes | Yes | Yes | Yes | Yes | Yes |
| `farmersdelight:orange_hanging_canvas_sign` | Yes | Yes | Yes | Yes | Yes | Yes |
| `farmersdelight:orange_wall_hanging_canvas_sign` | Yes | Yes | Yes | Yes | Yes | Yes |
| `farmersdelight:organic_compost` | — | Yes | Yes | Yes | Yes | Yes |
| `farmersdelight:pink_canvas_sign` | Yes | Yes | Yes | Yes | Yes | Yes |
| `farmersdelight:pink_canvas_wall_sign` | Yes | Yes | Yes | Yes | Yes | Yes |
| `farmersdelight:pink_hanging_canvas_sign` | Yes | Yes | Yes | Yes | Yes | Yes |
| `farmersdelight:pink_wall_hanging_canvas_sign` | Yes | Yes | Yes | Yes | Yes | Yes |
| `farmersdelight:potato_crate` | Yes | Yes | Yes | Yes | Yes | Yes |
| `farmersdelight:purple_canvas_sign` | Yes | Yes | Yes | Yes | Yes | Yes |
| `farmersdelight:purple_canvas_wall_sign` | Yes | Yes | Yes | Yes | Yes | Yes |
| `farmersdelight:purple_hanging_canvas_sign` | Yes | Yes | Yes | Yes | Yes | Yes |
| `farmersdelight:purple_wall_hanging_canvas_sign` | Yes | Yes | Yes | Yes | Yes | Yes |
| `farmersdelight:red_canvas_sign` | Yes | Yes | Yes | Yes | Yes | Yes |
| `farmersdelight:red_canvas_wall_sign` | Yes | Yes | Yes | Yes | Yes | Yes |
| `farmersdelight:red_hanging_canvas_sign` | Yes | Yes | Yes | Yes | Yes | Yes |
| `farmersdelight:red_mushroom_colony` | Yes | Yes | Yes | Yes | Yes | Yes |
| `farmersdelight:red_wall_hanging_canvas_sign` | Yes | Yes | Yes | Yes | Yes | Yes |
| `farmersdelight:rice` | Yes | — | — | — | — | — |
| `farmersdelight:rice_bag` | Yes | Yes | Yes | Yes | Yes | Yes |
| `farmersdelight:rice_bale` | Yes | Yes | Yes | Yes | Yes | Yes |
| `farmersdelight:rice_panicles` | Yes | — | — | — | — | — |
| `farmersdelight:rice_roll_medley_block` | Yes | Yes | Yes | Yes | Yes | Yes |
| `farmersdelight:rich_soil` | Yes | — | — | — | — | — |
| `farmersdelight:rich_soil_farmland` | Yes | — | — | — | — | — |
| `farmersdelight:roast_chicken_block` | Yes | Yes | Yes | Yes | Yes | Yes |
| `farmersdelight:rope` | Yes | Yes | Yes | Yes | Yes | Yes |
| `farmersdelight:safety_net` | Yes | Yes | Yes | Yes | Yes | Yes |
| `farmersdelight:sandy_shrub` | Yes | Yes | Yes | Yes | Yes | Yes |
| `farmersdelight:shepherds_pie_block` | Yes | Yes | Yes | Yes | Yes | Yes |
| `farmersdelight:skillet` | — | — | Yes | Yes | Yes | Yes |
| `farmersdelight:spruce_cabinet` | — | — | — | Yes | — | — |
| `farmersdelight:stove` | — | — | Yes | Yes | Yes | Yes |
| `farmersdelight:straw_bale` | Yes | Yes | Yes | Yes | Yes | Yes |
| `farmersdelight:stuffed_pumpkin_block` | Yes | Yes | Yes | Yes | Yes | Yes |
| `farmersdelight:sweet_berry_cheesecake` | Yes | — | — | — | — | — |
| `farmersdelight:tatami` | Yes | Yes | Yes | Yes | Yes | Yes |
| `farmersdelight:tomato_crate` | Yes | Yes | Yes | Yes | Yes | Yes |
| `farmersdelight:tomatoes` | Yes | — | — | — | — | — |
| `farmersdelight:wall_hanging_canvas_sign` | Yes | Yes | Yes | Yes | Yes | Yes |
| `farmersdelight:warped_cabinet` | — | — | — | — | — | Yes |
| `farmersdelight:white_canvas_sign` | Yes | Yes | Yes | Yes | Yes | Yes |
| `farmersdelight:white_canvas_wall_sign` | Yes | Yes | Yes | Yes | Yes | Yes |
| `farmersdelight:white_hanging_canvas_sign` | Yes | Yes | Yes | Yes | Yes | Yes |
| `farmersdelight:white_wall_hanging_canvas_sign` | Yes | Yes | Yes | Yes | Yes | Yes |
| `farmersdelight:wild_beetroots` | Yes | Yes | Yes | Yes | Yes | Yes |
| `farmersdelight:wild_cabbages` | Yes | Yes | Yes | Yes | Yes | Yes |
| `farmersdelight:wild_carrots` | Yes | Yes | Yes | Yes | Yes | Yes |
| `farmersdelight:wild_onions` | Yes | Yes | Yes | Yes | Yes | Yes |
| `farmersdelight:wild_potatoes` | Yes | Yes | Yes | Yes | Yes | Yes |
| `farmersdelight:wild_rice` | Yes | Yes | Yes | Yes | Yes | Yes |
| `farmersdelight:wild_tomatoes` | Yes | Yes | Yes | Yes | Yes | Yes |
| `farmersdelight:yellow_canvas_sign` | Yes | Yes | Yes | Yes | Yes | Yes |
| `farmersdelight:yellow_canvas_wall_sign` | Yes | Yes | Yes | Yes | Yes | Yes |
| `farmersdelight:yellow_hanging_canvas_sign` | Yes | Yes | Yes | Yes | Yes | Yes |
| `farmersdelight:yellow_wall_hanging_canvas_sign` | Yes | Yes | Yes | Yes | Yes | Yes |
| `fruitsdelight:apple_cauldron` | — | — | Yes | Yes | Yes | Yes |
| `fruitsdelight:apple_crate` | Yes | Yes | Yes | Yes | Yes | Yes |
| `fruitsdelight:apple_jello_block` | Yes | Yes | Yes | Yes | Yes | Yes |
| `fruitsdelight:apple_jello_cauldron` | — | — | Yes | Yes | Yes | Yes |
| `fruitsdelight:apple_jelly` | Yes | Yes | Yes | Yes | Yes | Yes |
| `fruitsdelight:apple_jelly_block` | Yes | Yes | Yes | Yes | Yes | Yes |
| `fruitsdelight:apple_jelly_cauldron` | — | — | Yes | Yes | Yes | Yes |
| `fruitsdelight:apple_leaves` | Yes | Yes | Yes | Yes | Yes | Yes |
| `fruitsdelight:apple_sapling` | Yes | Yes | Yes | Yes | Yes | Yes |
| `fruitsdelight:attached_hamimelon_stem` | Yes | Yes | Yes | Yes | Yes | Yes |
| `fruitsdelight:bayberry_cauldron` | — | — | Yes | Yes | Yes | Yes |
| `fruitsdelight:bayberry_crate` | Yes | Yes | Yes | Yes | Yes | Yes |
| `fruitsdelight:bayberry_jello_block` | Yes | Yes | Yes | Yes | Yes | Yes |
| `fruitsdelight:bayberry_jello_cauldron` | — | — | Yes | Yes | Yes | Yes |
| `fruitsdelight:bayberry_jelly` | Yes | Yes | Yes | Yes | Yes | Yes |
| `fruitsdelight:bayberry_jelly_block` | Yes | Yes | Yes | Yes | Yes | Yes |
| `fruitsdelight:bayberry_jelly_cauldron` | — | — | Yes | Yes | Yes | Yes |
| `fruitsdelight:bayberry_leaves` | Yes | Yes | Yes | Yes | Yes | Yes |
| `fruitsdelight:bayberry_sapling` | Yes | Yes | Yes | Yes | Yes | Yes |
| `fruitsdelight:blueberry_bush` | Yes | Yes | Yes | Yes | Yes | Yes |
| `fruitsdelight:blueberry_cauldron` | — | — | Yes | Yes | Yes | Yes |
| `fruitsdelight:blueberry_crate` | Yes | Yes | Yes | Yes | Yes | Yes |
| `fruitsdelight:blueberry_jello_block` | Yes | Yes | Yes | Yes | Yes | Yes |
| `fruitsdelight:blueberry_jello_cauldron` | — | — | Yes | Yes | Yes | Yes |
| `fruitsdelight:blueberry_jelly` | Yes | Yes | Yes | Yes | Yes | Yes |
| `fruitsdelight:blueberry_jelly_block` | Yes | Yes | Yes | Yes | Yes | Yes |
| `fruitsdelight:blueberry_jelly_cauldron` | — | — | Yes | Yes | Yes | Yes |
| `fruitsdelight:chorus_cauldron` | — | — | — | — | Yes | — |
| `fruitsdelight:chorus_jello_block` | — | — | — | — | Yes | — |
| `fruitsdelight:chorus_jello_cauldron` | — | — | — | — | Yes | — |
| `fruitsdelight:chorus_jelly` | — | — | — | — | Yes | — |
| `fruitsdelight:chorus_jelly_block` | — | — | — | — | Yes | — |
| `fruitsdelight:chorus_jelly_cauldron` | — | — | — | — | Yes | — |
| `fruitsdelight:cranberry_bush` | Yes | Yes | Yes | Yes | Yes | Yes |
| `fruitsdelight:cranberry_cauldron` | — | — | Yes | Yes | Yes | Yes |
| `fruitsdelight:cranberry_crate` | Yes | Yes | Yes | Yes | Yes | Yes |
| `fruitsdelight:cranberry_jello_block` | Yes | Yes | Yes | Yes | Yes | Yes |
| `fruitsdelight:cranberry_jello_cauldron` | — | — | Yes | Yes | Yes | Yes |
| `fruitsdelight:cranberry_jelly` | Yes | Yes | Yes | Yes | Yes | Yes |
| `fruitsdelight:cranberry_jelly_block` | Yes | Yes | Yes | Yes | Yes | Yes |
| `fruitsdelight:cranberry_jelly_cauldron` | — | — | Yes | Yes | Yes | Yes |
| `fruitsdelight:durian` | Yes | Yes | Yes | Yes | Yes | Yes |
| `fruitsdelight:durian_cauldron` | — | — | Yes | Yes | Yes | Yes |
| `fruitsdelight:durian_jello_block` | Yes | Yes | Yes | Yes | Yes | Yes |
| `fruitsdelight:durian_jello_cauldron` | — | — | Yes | Yes | Yes | Yes |
| `fruitsdelight:durian_jelly` | Yes | Yes | Yes | Yes | Yes | Yes |
| `fruitsdelight:durian_jelly_block` | Yes | Yes | Yes | Yes | Yes | Yes |
| `fruitsdelight:durian_jelly_cauldron` | — | — | Yes | Yes | Yes | Yes |
| `fruitsdelight:durian_leaves` | Yes | Yes | Yes | Yes | Yes | Yes |
| `fruitsdelight:durian_sapling` | Yes | Yes | Yes | Yes | Yes | Yes |
| `fruitsdelight:fig_cauldron` | — | — | Yes | Yes | Yes | Yes |
| `fruitsdelight:fig_crate` | Yes | Yes | Yes | Yes | Yes | Yes |
| `fruitsdelight:fig_jello_block` | Yes | Yes | Yes | Yes | Yes | Yes |
| `fruitsdelight:fig_jello_cauldron` | — | — | Yes | Yes | Yes | Yes |
| `fruitsdelight:fig_jelly` | Yes | Yes | Yes | Yes | Yes | Yes |
| `fruitsdelight:fig_jelly_block` | Yes | Yes | Yes | Yes | Yes | Yes |
| `fruitsdelight:fig_jelly_cauldron` | — | — | Yes | Yes | Yes | Yes |
| `fruitsdelight:fig_leaves` | Yes | Yes | Yes | Yes | Yes | Yes |
| `fruitsdelight:fig_pudding` | Yes | Yes | Yes | Yes | Yes | Yes |
| `fruitsdelight:fig_sapling` | Yes | Yes | Yes | Yes | Yes | Yes |
| `fruitsdelight:glowberry_cauldron` | — | — | Yes | Yes | Yes | Yes |
| `fruitsdelight:glowberry_jello_block` | Yes | Yes | Yes | Yes | Yes | Yes |
| `fruitsdelight:glowberry_jello_cauldron` | — | — | Yes | Yes | Yes | Yes |
| `fruitsdelight:glowberry_jelly` | Yes | Yes | Yes | Yes | Yes | Yes |
| `fruitsdelight:glowberry_jelly_block` | Yes | Yes | Yes | Yes | Yes | Yes |
| `fruitsdelight:glowberry_jelly_cauldron` | — | — | Yes | Yes | Yes | Yes |
| `fruitsdelight:hamimelon` | Yes | Yes | Yes | Yes | Yes | Yes |
| `fruitsdelight:hamimelon_cauldron` | — | — | Yes | Yes | Yes | Yes |
| `fruitsdelight:hamimelon_jello_block` | Yes | Yes | Yes | Yes | Yes | Yes |
| `fruitsdelight:hamimelon_jello_cauldron` | — | — | Yes | Yes | Yes | Yes |
| `fruitsdelight:hamimelon_jelly` | Yes | Yes | Yes | Yes | Yes | Yes |
| `fruitsdelight:hamimelon_jelly_block` | Yes | Yes | Yes | Yes | Yes | Yes |
| `fruitsdelight:hamimelon_jelly_cauldron` | — | — | Yes | Yes | Yes | Yes |
| `fruitsdelight:hamimelon_stem` | Yes | Yes | Yes | Yes | Yes | Yes |
| `fruitsdelight:hawberry_cauldron` | — | — | Yes | Yes | Yes | Yes |
| `fruitsdelight:hawberry_crate` | Yes | Yes | Yes | Yes | Yes | Yes |
| `fruitsdelight:hawberry_jello_block` | Yes | Yes | Yes | Yes | Yes | Yes |
| `fruitsdelight:hawberry_jello_cauldron` | — | — | Yes | Yes | Yes | Yes |
| `fruitsdelight:hawberry_jelly` | Yes | Yes | Yes | Yes | Yes | Yes |
| `fruitsdelight:hawberry_jelly_block` | Yes | Yes | Yes | Yes | Yes | Yes |
| `fruitsdelight:hawberry_jelly_cauldron` | — | — | Yes | Yes | Yes | Yes |
| `fruitsdelight:hawberry_leaves` | Yes | Yes | Yes | Yes | Yes | Yes |
| `fruitsdelight:hawberry_sapling` | Yes | Yes | Yes | Yes | Yes | Yes |
| `fruitsdelight:kiwi_cauldron` | — | — | Yes | Yes | Yes | Yes |
| `fruitsdelight:kiwi_crate` | Yes | Yes | Yes | Yes | Yes | Yes |
| `fruitsdelight:kiwi_jello_block` | Yes | Yes | Yes | Yes | Yes | Yes |
| `fruitsdelight:kiwi_jello_cauldron` | — | — | Yes | Yes | Yes | Yes |
| `fruitsdelight:kiwi_jelly` | Yes | Yes | Yes | Yes | Yes | Yes |
| `fruitsdelight:kiwi_jelly_block` | Yes | Yes | Yes | Yes | Yes | Yes |
| `fruitsdelight:kiwi_jelly_cauldron` | — | — | Yes | Yes | Yes | Yes |
| `fruitsdelight:kiwi_leaves` | Yes | Yes | Yes | Yes | Yes | Yes |
| `fruitsdelight:kiwi_sapling` | Yes | Yes | Yes | Yes | Yes | Yes |
| `fruitsdelight:lemon_cauldron` | — | — | Yes | Yes | Yes | Yes |
| `fruitsdelight:lemon_crate` | Yes | Yes | Yes | Yes | Yes | Yes |
| `fruitsdelight:lemon_jello_block` | Yes | Yes | Yes | Yes | Yes | Yes |
| `fruitsdelight:lemon_jello_cauldron` | — | — | Yes | Yes | Yes | Yes |
| `fruitsdelight:lemon_jelly` | Yes | Yes | Yes | Yes | Yes | Yes |
| `fruitsdelight:lemon_jelly_block` | Yes | Yes | Yes | Yes | Yes | Yes |
| `fruitsdelight:lemon_jelly_cauldron` | — | — | Yes | Yes | Yes | Yes |
| `fruitsdelight:lemon_tree` | Yes | Yes | Yes | Yes | Yes | Yes |
| `fruitsdelight:lemonade_cauldron` | — | — | Yes | Yes | Yes | Yes |
| `fruitsdelight:lychee_cauldron` | — | — | Yes | Yes | Yes | Yes |
| `fruitsdelight:lychee_crate` | Yes | Yes | Yes | Yes | Yes | Yes |
| `fruitsdelight:lychee_jello_block` | Yes | Yes | Yes | Yes | Yes | Yes |
| `fruitsdelight:lychee_jello_cauldron` | — | — | Yes | Yes | Yes | Yes |
| `fruitsdelight:lychee_jelly` | Yes | Yes | Yes | Yes | Yes | Yes |
| `fruitsdelight:lychee_jelly_block` | Yes | Yes | Yes | Yes | Yes | Yes |
| `fruitsdelight:lychee_jelly_cauldron` | — | — | Yes | Yes | Yes | Yes |
| `fruitsdelight:lychee_leaves` | Yes | Yes | Yes | Yes | Yes | Yes |
| `fruitsdelight:lychee_sapling` | Yes | Yes | Yes | Yes | Yes | Yes |
| `fruitsdelight:mango_cauldron` | — | — | Yes | Yes | Yes | Yes |
| `fruitsdelight:mango_crate` | Yes | Yes | Yes | Yes | Yes | Yes |
| `fruitsdelight:mango_jello_block` | Yes | Yes | Yes | Yes | Yes | Yes |
| `fruitsdelight:mango_jello_cauldron` | — | — | Yes | Yes | Yes | Yes |
| `fruitsdelight:mango_jelly` | Yes | Yes | Yes | Yes | Yes | Yes |
| `fruitsdelight:mango_jelly_block` | Yes | Yes | Yes | Yes | Yes | Yes |
| `fruitsdelight:mango_jelly_cauldron` | — | — | Yes | Yes | Yes | Yes |
| `fruitsdelight:mango_leaves` | Yes | Yes | Yes | Yes | Yes | Yes |
| `fruitsdelight:mango_sapling` | Yes | Yes | Yes | Yes | Yes | Yes |
| `fruitsdelight:mangosteen_cake` | Yes | Yes | Yes | Yes | Yes | Yes |
| `fruitsdelight:mangosteen_cauldron` | — | — | Yes | Yes | Yes | Yes |
| `fruitsdelight:mangosteen_crate` | Yes | Yes | Yes | Yes | Yes | Yes |
| `fruitsdelight:mangosteen_jello_block` | Yes | Yes | Yes | Yes | Yes | Yes |
| `fruitsdelight:mangosteen_jello_cauldron` | — | — | Yes | Yes | Yes | Yes |
| `fruitsdelight:mangosteen_jelly` | Yes | Yes | Yes | Yes | Yes | Yes |
| `fruitsdelight:mangosteen_jelly_block` | Yes | Yes | Yes | Yes | Yes | Yes |
| `fruitsdelight:mangosteen_jelly_cauldron` | — | — | Yes | Yes | Yes | Yes |
| `fruitsdelight:mangosteen_leaves` | Yes | Yes | Yes | Yes | Yes | Yes |
| `fruitsdelight:mangosteen_sapling` | Yes | Yes | Yes | Yes | Yes | Yes |
| `fruitsdelight:melon_cauldron` | — | — | Yes | Yes | Yes | Yes |
| `fruitsdelight:melon_jello_block` | Yes | Yes | Yes | Yes | Yes | Yes |
| `fruitsdelight:melon_jello_cauldron` | — | — | Yes | Yes | Yes | Yes |
| `fruitsdelight:melon_jelly` | Yes | Yes | Yes | Yes | Yes | Yes |
| `fruitsdelight:melon_jelly_block` | Yes | Yes | Yes | Yes | Yes | Yes |
| `fruitsdelight:melon_jelly_cauldron` | — | — | Yes | Yes | Yes | Yes |
| `fruitsdelight:orange_cauldron` | — | — | Yes | Yes | Yes | Yes |
| `fruitsdelight:orange_crate` | Yes | Yes | Yes | Yes | Yes | Yes |
| `fruitsdelight:orange_jello_block` | Yes | Yes | Yes | Yes | Yes | Yes |
| `fruitsdelight:orange_jello_cauldron` | — | — | Yes | Yes | Yes | Yes |
| `fruitsdelight:orange_jelly` | Yes | Yes | Yes | Yes | Yes | Yes |
| `fruitsdelight:orange_jelly_block` | Yes | Yes | Yes | Yes | Yes | Yes |
| `fruitsdelight:orange_jelly_cauldron` | — | — | Yes | Yes | Yes | Yes |
| `fruitsdelight:orange_leaves` | Yes | Yes | Yes | Yes | Yes | Yes |
| `fruitsdelight:orange_sapling` | Yes | Yes | Yes | Yes | Yes | Yes |
| `fruitsdelight:peach_cauldron` | — | — | Yes | Yes | Yes | Yes |
| `fruitsdelight:peach_crate` | Yes | Yes | Yes | Yes | Yes | Yes |
| `fruitsdelight:peach_jello_block` | Yes | Yes | Yes | Yes | Yes | Yes |
| `fruitsdelight:peach_jello_cauldron` | — | — | Yes | Yes | Yes | Yes |
| `fruitsdelight:peach_jelly` | Yes | Yes | Yes | Yes | Yes | Yes |
| `fruitsdelight:peach_jelly_block` | Yes | Yes | Yes | Yes | Yes | Yes |
| `fruitsdelight:peach_jelly_cauldron` | — | — | Yes | Yes | Yes | Yes |
| `fruitsdelight:peach_leaves` | Yes | Yes | Yes | Yes | Yes | Yes |
| `fruitsdelight:peach_sapling` | Yes | Yes | Yes | Yes | Yes | Yes |
| `fruitsdelight:pear_cauldron` | — | — | Yes | Yes | Yes | Yes |
| `fruitsdelight:pear_crate` | Yes | Yes | Yes | Yes | Yes | Yes |
| `fruitsdelight:pear_jello_block` | Yes | Yes | Yes | Yes | Yes | Yes |
| `fruitsdelight:pear_jello_cauldron` | — | — | Yes | Yes | Yes | Yes |
| `fruitsdelight:pear_jelly` | Yes | Yes | Yes | Yes | Yes | Yes |
| `fruitsdelight:pear_jelly_block` | Yes | Yes | Yes | Yes | Yes | Yes |
| `fruitsdelight:pear_jelly_cauldron` | — | — | Yes | Yes | Yes | Yes |
| `fruitsdelight:pear_leaves` | Yes | Yes | Yes | Yes | Yes | Yes |
| `fruitsdelight:pear_sapling` | Yes | Yes | Yes | Yes | Yes | Yes |
| `fruitsdelight:persimmon_cauldron` | — | — | Yes | Yes | Yes | Yes |
| `fruitsdelight:persimmon_crate` | Yes | Yes | Yes | Yes | Yes | Yes |
| `fruitsdelight:persimmon_jello_block` | Yes | Yes | Yes | Yes | Yes | Yes |
| `fruitsdelight:persimmon_jello_cauldron` | — | — | Yes | Yes | Yes | Yes |
| `fruitsdelight:persimmon_jelly` | Yes | Yes | Yes | Yes | Yes | Yes |
| `fruitsdelight:persimmon_jelly_block` | Yes | Yes | Yes | Yes | Yes | Yes |
| `fruitsdelight:persimmon_jelly_cauldron` | — | — | Yes | Yes | Yes | Yes |
| `fruitsdelight:persimmon_leaves` | Yes | Yes | Yes | Yes | Yes | Yes |
| `fruitsdelight:persimmon_sapling` | Yes | Yes | Yes | Yes | Yes | Yes |
| `fruitsdelight:pineapple` | Yes | Yes | Yes | Yes | Yes | Yes |
| `fruitsdelight:pineapple_cauldron` | — | — | Yes | Yes | Yes | Yes |
| `fruitsdelight:pineapple_crate` | Yes | Yes | Yes | Yes | Yes | Yes |
| `fruitsdelight:pineapple_fried_rice` | Yes | Yes | Yes | Yes | Yes | Yes |
| `fruitsdelight:pineapple_jello_block` | Yes | Yes | Yes | Yes | Yes | Yes |
| `fruitsdelight:pineapple_jello_cauldron` | — | — | Yes | Yes | Yes | Yes |
| `fruitsdelight:pineapple_jelly` | Yes | Yes | Yes | Yes | Yes | Yes |
| `fruitsdelight:pineapple_jelly_block` | Yes | Yes | Yes | Yes | Yes | Yes |
| `fruitsdelight:pineapple_jelly_cauldron` | — | — | Yes | Yes | Yes | Yes |
| `fruitsdelight:potted_apple_sapling` | — | — | Yes | — | — | — |
| `fruitsdelight:potted_bayberry_sapling` | — | — | Yes | — | — | — |
| `fruitsdelight:potted_durian_sapling` | — | — | Yes | — | — | — |
| `fruitsdelight:potted_fig_sapling` | — | — | Yes | — | — | — |
| `fruitsdelight:potted_hawberry_sapling` | — | — | Yes | — | — | — |
| `fruitsdelight:potted_kiwi_sapling` | — | — | Yes | — | — | — |
| `fruitsdelight:potted_lychee_sapling` | — | — | Yes | — | — | — |
| `fruitsdelight:potted_mango_sapling` | — | — | Yes | — | — | — |
| `fruitsdelight:potted_mangosteen_sapling` | — | — | Yes | — | — | — |
| `fruitsdelight:potted_orange_sapling` | — | — | Yes | — | — | — |
| `fruitsdelight:potted_peach_sapling` | — | — | Yes | — | — | — |
| `fruitsdelight:potted_pear_sapling` | — | — | Yes | — | — | — |
| `fruitsdelight:potted_persimmon_sapling` | — | — | Yes | — | — | — |
| `fruitsdelight:sweetberry_cauldron` | — | — | Yes | Yes | Yes | Yes |
| `fruitsdelight:sweetberry_jello_block` | Yes | Yes | Yes | Yes | Yes | Yes |
| `fruitsdelight:sweetberry_jello_cauldron` | — | — | Yes | Yes | Yes | Yes |
| `fruitsdelight:sweetberry_jelly` | Yes | Yes | Yes | Yes | Yes | Yes |
| `fruitsdelight:sweetberry_jelly_block` | Yes | Yes | Yes | Yes | Yes | Yes |
| `fruitsdelight:sweetberry_jelly_cauldron` | — | — | Yes | Yes | Yes | Yes |
| `fruitsdelight:wild_pineapple` | Yes | Yes | Yes | Yes | Yes | Yes |
| `geckolib:fertilizer` | Yes | Yes | Yes | Yes | Yes | Yes |
| `geckolib:gecko_habitat` | Yes | Yes | Yes | Yes | Yes | Yes |
| `hybrid-aquatic:acacia_crate` | — | Yes | — | — | — | — |
| `hybrid-aquatic:anemone` | Yes | Yes | Yes | Yes | Yes | Yes |
| `hybrid-aquatic:bamboo_crate` | Yes | — | — | — | — | — |
| `hybrid-aquatic:basking_shark_plushie` | Yes | Yes | Yes | Yes | Yes | Yes |
| `hybrid-aquatic:birch_crate` | — | — | Yes | — | — | — |
| `hybrid-aquatic:black_pearl_block` | — | — | Yes | Yes | Yes | Yes |
| `hybrid-aquatic:bull_kelp` | Yes | Yes | Yes | Yes | Yes | Yes |
| `hybrid-aquatic:bull_kelp_plant` | Yes | Yes | Yes | Yes | Yes | Yes |
| `hybrid-aquatic:bull_shark_plushie` | Yes | Yes | Yes | Yes | Yes | Yes |
| `hybrid-aquatic:buoy` | Yes | Yes | Yes | Yes | Yes | Yes |
| `hybrid-aquatic:button_coral` | Yes | — | — | — | — | — |
| `hybrid-aquatic:button_coral_block` | Yes | — | — | — | — | — |
| `hybrid-aquatic:button_coral_fan` | Yes | Yes | Yes | Yes | Yes | Yes |
| `hybrid-aquatic:button_coral_wall_fan` | Yes | Yes | Yes | Yes | Yes | Yes |
| `hybrid-aquatic:cattail` | Yes | Yes | Yes | Yes | Yes | Yes |
| `hybrid-aquatic:cherry_crate` | Yes | — | — | — | — | — |
| `hybrid-aquatic:crab_pot` | Yes | Yes | Yes | Yes | Yes | Yes |
| `hybrid-aquatic:dark_oak_crate` | Yes | — | — | — | — | — |
| `hybrid-aquatic:dead_button_coral` | Yes | Yes | Yes | Yes | Yes | Yes |
| `hybrid-aquatic:dead_button_coral_block` | — | — | Yes | Yes | Yes | Yes |
| `hybrid-aquatic:dead_button_coral_fan` | Yes | Yes | Yes | Yes | Yes | Yes |
| `hybrid-aquatic:dead_button_coral_wall_fan` | Yes | Yes | Yes | Yes | Yes | Yes |
| `hybrid-aquatic:dead_leaf_coral` | Yes | Yes | Yes | Yes | Yes | Yes |
| `hybrid-aquatic:dead_leaf_coral_block` | — | — | Yes | Yes | Yes | Yes |
| `hybrid-aquatic:dead_leaf_coral_fan` | Yes | Yes | Yes | Yes | Yes | Yes |
| `hybrid-aquatic:dead_leaf_coral_wall_fan` | Yes | Yes | Yes | Yes | Yes | Yes |
| `hybrid-aquatic:dead_lophelia_coral` | Yes | Yes | Yes | Yes | Yes | Yes |
| `hybrid-aquatic:dead_lophelia_coral_block` | — | — | Yes | Yes | Yes | Yes |
| `hybrid-aquatic:dead_lophelia_coral_fan` | Yes | Yes | Yes | Yes | Yes | Yes |
| `hybrid-aquatic:dead_lophelia_coral_wall_fan` | Yes | Yes | Yes | Yes | Yes | Yes |
| `hybrid-aquatic:dead_rose_coral` | Yes | Yes | Yes | Yes | Yes | Yes |
| `hybrid-aquatic:dead_rose_coral_block` | — | — | Yes | Yes | Yes | Yes |
| `hybrid-aquatic:dead_rose_coral_fan` | Yes | Yes | Yes | Yes | Yes | Yes |
| `hybrid-aquatic:dead_rose_coral_wall_fan` | Yes | Yes | Yes | Yes | Yes | Yes |
| `hybrid-aquatic:dead_sun_coral` | Yes | Yes | Yes | Yes | Yes | Yes |
| `hybrid-aquatic:dead_sun_coral_block` | — | — | Yes | Yes | Yes | Yes |
| `hybrid-aquatic:dead_sun_coral_fan` | Yes | Yes | Yes | Yes | Yes | Yes |
| `hybrid-aquatic:dead_sun_coral_wall_fan` | Yes | Yes | Yes | Yes | Yes | Yes |
| `hybrid-aquatic:dead_thorn_coral` | Yes | Yes | Yes | Yes | Yes | Yes |
| `hybrid-aquatic:dead_thorn_coral_block` | — | — | Yes | Yes | Yes | Yes |
| `hybrid-aquatic:dead_thorn_coral_fan` | Yes | Yes | Yes | Yes | Yes | Yes |
| `hybrid-aquatic:dead_thorn_coral_wall_fan` | Yes | Yes | Yes | Yes | Yes | Yes |
| `hybrid-aquatic:driftwood_button` | Yes | Yes | Yes | Yes | Yes | Yes |
| `hybrid-aquatic:driftwood_door` | Yes | Yes | Yes | Yes | Yes | Yes |
| `hybrid-aquatic:driftwood_fence` | Yes | Yes | Yes | Yes | Yes | Yes |
| `hybrid-aquatic:driftwood_fence_gate` | Yes | Yes | Yes | Yes | Yes | Yes |
| `hybrid-aquatic:driftwood_log` | Yes | Yes | Yes | Yes | Yes | Yes |
| `hybrid-aquatic:driftwood_planks` | Yes | Yes | Yes | Yes | Yes | Yes |
| `hybrid-aquatic:driftwood_pressure_plate` | Yes | Yes | Yes | Yes | Yes | Yes |
| `hybrid-aquatic:driftwood_slab` | Yes | Yes | Yes | Yes | Yes | Yes |
| `hybrid-aquatic:driftwood_stairs` | Yes | Yes | Yes | Yes | Yes | Yes |
| `hybrid-aquatic:driftwood_trapdoor` | Yes | Yes | Yes | Yes | Yes | Yes |
| `hybrid-aquatic:driftwood_wood` | Yes | Yes | Yes | Yes | Yes | Yes |
| `hybrid-aquatic:dunegrass` | Yes | Yes | Yes | Yes | Yes | Yes |
| `hybrid-aquatic:floating_sargassum` | Yes | Yes | Yes | Yes | Yes | Yes |
| `hybrid-aquatic:frilled_shark_plushie` | Yes | Yes | Yes | Yes | Yes | Yes |
| `hybrid-aquatic:giant_clam` | — | — | Yes | Yes | Yes | Yes |
| `hybrid-aquatic:giant_green_anemone` | Yes | Yes | Yes | Yes | Yes | Yes |
| `hybrid-aquatic:glowing_plankton` | Yes | Yes | Yes | Yes | Yes | Yes |
| `hybrid-aquatic:glowslime_block` | Yes | Yes | Yes | Yes | Yes | Yes |
| `hybrid-aquatic:glowstick` | Yes | Yes | Yes | Yes | Yes | Yes |
| `hybrid-aquatic:great_white_shark_plushie` | Yes | Yes | Yes | Yes | Yes | Yes |
| `hybrid-aquatic:hammerhead_shark_plushie` | Yes | Yes | Yes | Yes | Yes | Yes |
| `hybrid-aquatic:hybrid_crate` | Yes | Yes | Yes | Yes | Yes | Yes |
| `hybrid-aquatic:hydrothermal_vent_shaft` | — | — | Yes | Yes | Yes | Yes |
| `hybrid-aquatic:jungle_crate` | Yes | — | — | — | — | — |
| `hybrid-aquatic:jungle_lily_pad` | Yes | — | — | — | — | — |
| `hybrid-aquatic:leaf_coral` | Yes | — | — | — | — | — |
| `hybrid-aquatic:leaf_coral_block` | Yes | — | — | — | — | — |
| `hybrid-aquatic:leaf_coral_fan` | Yes | Yes | Yes | Yes | Yes | Yes |
| `hybrid-aquatic:leaf_coral_wall_fan` | Yes | Yes | Yes | Yes | Yes | Yes |
| `hybrid-aquatic:lophelia_coral` | Yes | — | — | — | — | — |
| `hybrid-aquatic:lophelia_coral_block` | Yes | — | — | — | — | — |
| `hybrid-aquatic:lophelia_coral_fan` | Yes | Yes | Yes | Yes | Yes | Yes |
| `hybrid-aquatic:lophelia_coral_wall_fan` | Yes | Yes | Yes | Yes | Yes | Yes |
| `hybrid-aquatic:mangrove_crate` | Yes | — | — | — | — | — |
| `hybrid-aquatic:message_in_a_bottle` | Yes | Yes | Yes | Yes | Yes | Yes |
| `hybrid-aquatic:oak_crate` | Yes | — | Yes | — | — | — |
| `hybrid-aquatic:pearl_block` | — | — | Yes | Yes | Yes | Yes |
| `hybrid-aquatic:raft` | Yes | Yes | Yes | Yes | Yes | Yes |
| `hybrid-aquatic:red_algae` | Yes | — | — | — | — | — |
| `hybrid-aquatic:rose_coral` | Yes | — | — | — | — | — |
| `hybrid-aquatic:rose_coral_block` | Yes | — | — | — | — | — |
| `hybrid-aquatic:rose_coral_fan` | Yes | Yes | Yes | Yes | Yes | Yes |
| `hybrid-aquatic:rose_coral_wall_fan` | Yes | Yes | Yes | Yes | Yes | Yes |
| `hybrid-aquatic:sargassum` | Yes | Yes | Yes | Yes | Yes | Yes |
| `hybrid-aquatic:sargassum_plant` | Yes | Yes | Yes | Yes | Yes | Yes |
| `hybrid-aquatic:sea_lettuce` | Yes | — | — | — | — | — |
| `hybrid-aquatic:spruce_crate` | — | — | — | Yes | — | — |
| `hybrid-aquatic:strawberry_anemone` | Yes | Yes | Yes | Yes | Yes | Yes |
| `hybrid-aquatic:stripped_driftwood_log` | Yes | Yes | Yes | Yes | Yes | Yes |
| `hybrid-aquatic:stripped_driftwood_wood` | Yes | Yes | Yes | Yes | Yes | Yes |
| `hybrid-aquatic:sun_coral` | Yes | — | — | — | — | — |
| `hybrid-aquatic:sun_coral_block` | Yes | — | — | — | — | — |
| `hybrid-aquatic:sun_coral_fan` | Yes | Yes | Yes | Yes | Yes | Yes |
| `hybrid-aquatic:sun_coral_wall_fan` | Yes | Yes | Yes | Yes | Yes | Yes |
| `hybrid-aquatic:tall_dunegrass` | Yes | Yes | Yes | Yes | Yes | Yes |
| `hybrid-aquatic:tall_red_algae` | Yes | Yes | Yes | Yes | Yes | Yes |
| `hybrid-aquatic:tall_sea_lettuce` | Yes | Yes | Yes | Yes | Yes | Yes |
| `hybrid-aquatic:thorn_coral` | Yes | — | — | — | — | — |
| `hybrid-aquatic:thorn_coral_block` | Yes | — | — | — | — | — |
| `hybrid-aquatic:thorn_coral_fan` | Yes | Yes | Yes | Yes | Yes | Yes |
| `hybrid-aquatic:thorn_coral_wall_fan` | Yes | Yes | Yes | Yes | Yes | Yes |
| `hybrid-aquatic:thresher_shark_plushie` | Yes | Yes | Yes | Yes | Yes | Yes |
| `hybrid-aquatic:tiger_shark_plushie` | Yes | Yes | Yes | Yes | Yes | Yes |
| `hybrid-aquatic:tube_sponge` | Yes | Yes | Yes | Yes | Yes | Yes |
| `hybrid-aquatic:tube_worm` | Yes | Yes | Yes | Yes | Yes | Yes |
| `hybrid-aquatic:wall_glowstick` | Yes | Yes | Yes | Yes | Yes | Yes |
| `hybrid-aquatic:water_lettuce` | Yes | Yes | Yes | Yes | Yes | Yes |
| `hybrid-aquatic:whale_shark_plushie` | Yes | Yes | Yes | Yes | Yes | Yes |
| `iceandfire:ash` | — | Yes | Yes | Yes | Yes | Yes |
| `iceandfire:burnt_torch` | Yes | Yes | Yes | Yes | Yes | Yes |
| `iceandfire:burnt_torch_wall` | Yes | Yes | Yes | Yes | Yes | Yes |
| `iceandfire:chared_cobblestone` | — | — | Yes | Yes | Yes | Yes |
| `iceandfire:chared_dirt` | — | Yes | Yes | Yes | Yes | Yes |
| `iceandfire:chared_dirt_path` | — | Yes | Yes | Yes | Yes | Yes |
| `iceandfire:chared_grass` | — | Yes | Yes | Yes | Yes | Yes |
| `iceandfire:chared_gravel` | — | Yes | Yes | Yes | Yes | Yes |
| `iceandfire:chared_stone` | — | — | Yes | Yes | Yes | Yes |
| `iceandfire:copper_pile` | — | — | Yes | Yes | Yes | Yes |
| `iceandfire:crackled_cobblestone` | — | — | Yes | Yes | Yes | Yes |
| `iceandfire:crackled_dirt` | — | Yes | Yes | Yes | Yes | Yes |
| `iceandfire:crackled_dirt_path` | — | Yes | Yes | Yes | Yes | Yes |
| `iceandfire:crackled_grass` | — | Yes | Yes | Yes | Yes | Yes |
| `iceandfire:crackled_gravel` | — | Yes | Yes | Yes | Yes | Yes |
| `iceandfire:crackled_stone` | — | — | Yes | Yes | Yes | Yes |
| `iceandfire:deepslate_silver_ore` | — | — | Yes | Yes | Yes | Yes |
| `iceandfire:desert_myrmex_cocoon` | Yes | Yes | Yes | Yes | Yes | Yes |
| `iceandfire:dragon_bone_block` | — | — | Yes | Yes | Yes | Yes |
| `iceandfire:dragon_bone_wall` | — | — | Yes | Yes | Yes | Yes |
| `iceandfire:dragon_ice` | — | — | — | Yes | — | — |
| `iceandfire:dragon_ice_spikes` | — | — | Yes | Yes | Yes | Yes |
| `iceandfire:dragonforge_fire_brick` | — | — | Yes | Yes | Yes | Yes |
| `iceandfire:dragonforge_fire_core` | — | — | Yes | Yes | Yes | Yes |
| `iceandfire:dragonforge_fire_core_disabled` | Yes | Yes | Yes | Yes | Yes | Yes |
| `iceandfire:dragonforge_fire_input` | — | — | Yes | Yes | Yes | Yes |
| `iceandfire:dragonforge_ice_brick` | — | — | Yes | Yes | Yes | Yes |
| `iceandfire:dragonforge_ice_core` | — | — | Yes | Yes | Yes | Yes |
| `iceandfire:dragonforge_ice_core_disabled` | Yes | Yes | Yes | Yes | Yes | Yes |
| `iceandfire:dragonforge_ice_input` | — | — | Yes | Yes | Yes | Yes |
| `iceandfire:dragonforge_lightning_brick` | — | — | Yes | Yes | Yes | Yes |
| `iceandfire:dragonforge_lightning_core` | — | — | Yes | Yes | Yes | Yes |
| `iceandfire:dragonforge_lightning_core_disabled` | Yes | Yes | Yes | Yes | Yes | Yes |
| `iceandfire:dragonforge_lightning_input` | — | — | Yes | Yes | Yes | Yes |
| `iceandfire:dragonscale_amythest` | — | — | Yes | Yes | Yes | Yes |
| `iceandfire:dragonscale_black` | — | — | Yes | Yes | Yes | Yes |
| `iceandfire:dragonscale_blue` | — | — | Yes | Yes | Yes | Yes |
| `iceandfire:dragonscale_bronze` | — | — | Yes | Yes | Yes | Yes |
| `iceandfire:dragonscale_copper` | — | — | Yes | Yes | Yes | Yes |
| `iceandfire:dragonscale_electric` | — | — | Yes | Yes | Yes | Yes |
| `iceandfire:dragonscale_gray` | — | — | Yes | Yes | Yes | Yes |
| `iceandfire:dragonscale_green` | — | — | Yes | Yes | Yes | Yes |
| `iceandfire:dragonscale_red` | — | — | Yes | Yes | Yes | Yes |
| `iceandfire:dragonscale_sapphire` | — | — | Yes | Yes | Yes | Yes |
| `iceandfire:dragonscale_silver` | — | — | Yes | Yes | Yes | Yes |
| `iceandfire:dragonscale_white` | — | — | Yes | Yes | Yes | Yes |
| `iceandfire:dragonsteel_fire_block` | — | — | Yes | Yes | Yes | Yes |
| `iceandfire:dragonsteel_ice_block` | — | — | Yes | Yes | Yes | Yes |
| `iceandfire:dragonsteel_lightning_block` | — | — | Yes | Yes | Yes | Yes |
| `iceandfire:dread_portal` | Yes | Yes | Yes | Yes | Yes | Yes |
| `iceandfire:dread_spawner` | — | — | Yes | Yes | Yes | Yes |
| `iceandfire:dread_stone` | — | — | Yes | Yes | Yes | Yes |
| `iceandfire:dread_stone_bricks` | — | — | Yes | Yes | Yes | Yes |
| `iceandfire:dread_stone_bricks_chiseled` | — | — | Yes | Yes | Yes | Yes |
| `iceandfire:dread_stone_bricks_cracked` | — | — | Yes | Yes | Yes | Yes |
| `iceandfire:dread_stone_bricks_mossy` | — | — | Yes | Yes | Yes | Yes |
| `iceandfire:dread_stone_face` | Yes | Yes | Yes | Yes | Yes | Yes |
| `iceandfire:dread_stone_slab` | Yes | Yes | Yes | Yes | Yes | Yes |
| `iceandfire:dread_stone_stairs` | Yes | Yes | Yes | Yes | Yes | Yes |
| `iceandfire:dread_stone_tile` | — | — | Yes | Yes | Yes | Yes |
| `iceandfire:dread_torch` | Yes | Yes | Yes | Yes | Yes | Yes |
| `iceandfire:dread_torch_wall` | Yes | Yes | Yes | Yes | Yes | Yes |
| `iceandfire:dreadwood_log` | Yes | Yes | Yes | Yes | Yes | Yes |
| `iceandfire:dreadwood_planks` | Yes | Yes | Yes | Yes | Yes | Yes |
| `iceandfire:dreadwood_planks_lock` | Yes | Yes | Yes | Yes | Yes | Yes |
| `iceandfire:egginice` | Yes | Yes | Yes | Yes | Yes | Yes |
| `iceandfire:fire_lily` | Yes | Yes | Yes | Yes | Yes | Yes |
| `iceandfire:frost_lily` | Yes | Yes | Yes | Yes | Yes | Yes |
| `iceandfire:frozen_cobblestone` | — | — | Yes | Yes | Yes | Yes |
| `iceandfire:frozen_dirt` | — | Yes | Yes | Yes | Yes | Yes |
| `iceandfire:frozen_dirt_path` | — | Yes | Yes | Yes | Yes | Yes |
| `iceandfire:frozen_grass` | — | Yes | Yes | Yes | Yes | Yes |
| `iceandfire:frozen_gravel` | — | Yes | Yes | Yes | Yes | Yes |
| `iceandfire:frozen_splinters` | — | — | Yes | Yes | Yes | Yes |
| `iceandfire:frozen_stone` | — | — | Yes | Yes | Yes | Yes |
| `iceandfire:ghost_chest` | Yes | Yes | Yes | Yes | Yes | Yes |
| `iceandfire:gold_pile` | — | — | Yes | Yes | Yes | Yes |
| `iceandfire:graveyard_soil` | — | Yes | Yes | Yes | Yes | Yes |
| `iceandfire:jungle_myrmex_cocoon` | Yes | — | — | — | — | — |
| `iceandfire:lectern` | Yes | Yes | Yes | Yes | Yes | Yes |
| `iceandfire:lightning_lily` | Yes | Yes | Yes | Yes | Yes | Yes |
| `iceandfire:myrmex_desert_biolight` | Yes | Yes | Yes | Yes | Yes | Yes |
| `iceandfire:myrmex_desert_resin_block` | Yes | Yes | Yes | Yes | Yes | Yes |
| `iceandfire:myrmex_desert_resin_glass` | Yes | Yes | Yes | Yes | Yes | Yes |
| `iceandfire:myrmex_jungle_biolight` | Yes | — | — | — | — | — |
| `iceandfire:myrmex_jungle_resin_block` | Yes | — | — | — | — | — |
| `iceandfire:myrmex_jungle_resin_glass` | Yes | — | — | — | — | — |
| `iceandfire:myrmex_resin_desert` | Yes | Yes | Yes | Yes | Yes | Yes |
| `iceandfire:myrmex_resin_jungle` | Yes | — | — | — | — | — |
| `iceandfire:myrmex_resin_sticky_desert` | Yes | Yes | Yes | Yes | Yes | Yes |
| `iceandfire:myrmex_resin_sticky_jungle` | Yes | — | — | — | — | — |
| `iceandfire:nest` | Yes | Yes | Yes | Yes | Yes | Yes |
| `iceandfire:pixie_house_birch` | — | — | Yes | — | — | — |
| `iceandfire:pixie_house_dark_oak` | Yes | — | — | — | — | — |
| `iceandfire:pixie_house_mushroom_brown` | Yes | Yes | Yes | Yes | Yes | Yes |
| `iceandfire:pixie_house_mushroom_red` | Yes | Yes | Yes | Yes | Yes | Yes |
| `iceandfire:pixie_house_oak` | Yes | — | Yes | — | — | — |
| `iceandfire:pixie_house_spruce` | — | — | — | Yes | — | — |
| `iceandfire:pixie_jar_0` | — | — | Yes | Yes | Yes | Yes |
| `iceandfire:pixie_jar_1` | — | — | Yes | Yes | Yes | Yes |
| `iceandfire:pixie_jar_2` | — | — | Yes | Yes | Yes | Yes |
| `iceandfire:pixie_jar_3` | — | — | Yes | Yes | Yes | Yes |
| `iceandfire:pixie_jar_4` | — | — | Yes | Yes | Yes | Yes |
| `iceandfire:pixie_jar_empty` | — | — | Yes | Yes | Yes | Yes |
| `iceandfire:podium_acacia` | — | Yes | — | — | — | — |
| `iceandfire:podium_birch` | — | — | Yes | — | — | — |
| `iceandfire:podium_dark_oak` | Yes | — | — | — | — | — |
| `iceandfire:podium_jungle` | Yes | — | — | — | — | — |
| `iceandfire:podium_oak` | Yes | — | Yes | — | — | — |
| `iceandfire:podium_spruce` | — | — | — | Yes | — | — |
| `iceandfire:raw_silver_block` | — | — | Yes | Yes | Yes | Yes |
| `iceandfire:sapphire_block` | — | — | Yes | Yes | Yes | Yes |
| `iceandfire:sapphire_ore` | — | — | Yes | Yes | Yes | Yes |
| `iceandfire:sea_serpent_scale_block_blue` | — | — | Yes | Yes | Yes | Yes |
| `iceandfire:sea_serpent_scale_block_bronze` | — | — | Yes | Yes | Yes | Yes |
| `iceandfire:sea_serpent_scale_block_deepblue` | — | — | Yes | Yes | Yes | Yes |
| `iceandfire:sea_serpent_scale_block_green` | — | — | Yes | Yes | Yes | Yes |
| `iceandfire:sea_serpent_scale_block_purple` | — | — | Yes | Yes | Yes | Yes |
| `iceandfire:sea_serpent_scale_block_red` | — | — | Yes | Yes | Yes | Yes |
| `iceandfire:sea_serpent_scale_block_teal` | — | — | Yes | Yes | Yes | Yes |
| `iceandfire:silver_block` | — | — | Yes | Yes | Yes | Yes |
| `iceandfire:silver_ore` | — | — | Yes | Yes | Yes | Yes |
| `iceandfire:silver_pile` | — | — | Yes | Yes | Yes | Yes |
| `minecraft:acacia_button` | — | Yes | — | — | — | — |
| `minecraft:acacia_door` | — | Yes | — | — | — | — |
| `minecraft:acacia_fence` | — | Yes | — | — | — | — |
| `minecraft:acacia_fence_gate` | — | Yes | — | — | — | — |
| `minecraft:acacia_hanging_sign` | — | — | — | — | — | — |
| `minecraft:acacia_leaves` | — | Yes | — | — | — | — |
| `minecraft:acacia_log` | — | Yes | — | — | — | — |
| `minecraft:acacia_planks` | — | Yes | — | — | — | — |
| `minecraft:acacia_pressure_plate` | — | Yes | — | — | — | — |
| `minecraft:acacia_sapling` | — | Yes | — | — | — | — |
| `minecraft:acacia_shelf` | — | Yes | — | — | — | — |
| `minecraft:acacia_sign` | — | Yes | — | — | — | — |
| `minecraft:acacia_slab` | — | Yes | — | — | — | — |
| `minecraft:acacia_stairs` | — | Yes | — | — | — | — |
| `minecraft:acacia_trapdoor` | — | Yes | — | — | — | — |
| `minecraft:acacia_wall_hanging_sign` | — | — | — | — | — | — |
| `minecraft:acacia_wall_sign` | — | Yes | — | — | — | — |
| `minecraft:acacia_wood` | — | Yes | — | — | — | — |
| `minecraft:activator_rail` | — | — | — | — | Yes | Yes |
| `minecraft:air` | Yes | Yes | Yes | Yes | Yes | Yes |
| `minecraft:allium` | Yes | — | — | — | — | — |
| `minecraft:amethyst_block` | — | — | — | Yes | — | — |
| `minecraft:amethyst_cluster` | — | — | — | Yes | — | — |
| `minecraft:ancient_debris` | — | — | — | — | Yes | Yes |
| `minecraft:andesite` | — | — | Yes | Yes | Yes | — |
| `minecraft:andesite_slab` | — | — | Yes | Yes | Yes | — |
| `minecraft:andesite_stairs` | — | — | Yes | Yes | Yes | — |
| `minecraft:andesite_wall` | — | — | Yes | Yes | Yes | — |
| `minecraft:anvil` | — | — | Yes | Yes | — | — |
| `minecraft:attached_melon_stem` | Yes | — | — | — | — | — |
| `minecraft:attached_pumpkin_stem` | Yes | Yes | Yes | — | — | — |
| `minecraft:azalea` | Yes | — | — | — | — | — |
| `minecraft:azalea_leaves` | Yes | — | — | — | — | — |
| `minecraft:azure_bluet` | Yes | Yes | Yes | — | — | — |
| `minecraft:bamboo` | Yes | — | — | — | — | — |
| `minecraft:bamboo_block` | Yes | — | — | — | — | — |
| `minecraft:bamboo_button` | Yes | — | — | — | — | — |
| `minecraft:bamboo_door` | Yes | — | — | — | — | — |
| `minecraft:bamboo_fence` | Yes | — | — | — | — | — |
| `minecraft:bamboo_fence_gate` | Yes | — | — | — | — | — |
| `minecraft:bamboo_hanging_sign` | — | — | — | — | — | — |
| `minecraft:bamboo_mosaic` | Yes | — | — | — | — | — |
| `minecraft:bamboo_mosaic_slab` | Yes | — | — | — | — | — |
| `minecraft:bamboo_mosaic_stairs` | Yes | — | — | — | — | — |
| `minecraft:bamboo_planks` | Yes | — | — | — | — | — |
| `minecraft:bamboo_pressure_plate` | Yes | — | — | — | — | — |
| `minecraft:bamboo_sapling` | Yes | — | — | — | — | — |
| `minecraft:bamboo_shelf` | Yes | — | — | — | — | — |
| `minecraft:bamboo_sign` | Yes | — | — | — | — | — |
| `minecraft:bamboo_slab` | Yes | — | — | — | — | — |
| `minecraft:bamboo_stairs` | Yes | — | — | — | — | — |
| `minecraft:bamboo_trapdoor` | Yes | — | — | — | — | — |
| `minecraft:bamboo_wall_hanging_sign` | — | — | — | — | — | — |
| `minecraft:bamboo_wall_sign` | Yes | — | — | — | — | — |
| `minecraft:barrel` | Yes | Yes | Yes | Yes | Yes | Yes |
| `minecraft:barrier` | Yes | Yes | Yes | Yes | Yes | Yes |
| `minecraft:basalt` | — | — | — | — | — | Yes |
| `minecraft:beacon` | — | — | — | — | — | Yes |
| `minecraft:bedrock` | Yes | Yes | Yes | Yes | Yes | Yes |
| `minecraft:bee_nest` | Yes | Yes | Yes | — | — | — |
| `minecraft:beehive` | Yes | Yes | Yes | — | — | — |
| `minecraft:beetroots` | Yes | Yes | Yes | Yes | — | — |
| `minecraft:bell` | — | — | Yes | Yes | Yes | — |
| `minecraft:big_dripleaf` | Yes | — | — | — | — | — |
| `minecraft:big_dripleaf_stem` | Yes | — | — | — | — | — |
| `minecraft:birch_button` | — | — | Yes | — | — | — |
| `minecraft:birch_door` | — | — | Yes | — | — | — |
| `minecraft:birch_fence` | — | — | Yes | — | — | — |
| `minecraft:birch_fence_gate` | — | — | Yes | — | — | — |
| `minecraft:birch_hanging_sign` | — | — | Yes | — | — | — |
| `minecraft:birch_leaves` | — | — | Yes | — | — | — |
| `minecraft:birch_log` | — | — | Yes | — | — | — |
| `minecraft:birch_planks` | — | — | Yes | — | — | — |
| `minecraft:birch_pressure_plate` | — | — | Yes | — | — | — |
| `minecraft:birch_sapling` | — | — | Yes | — | — | — |
| `minecraft:birch_shelf` | — | — | Yes | — | — | — |
| `minecraft:birch_sign` | — | — | Yes | — | — | — |
| `minecraft:birch_slab` | — | — | Yes | — | — | — |
| `minecraft:birch_stairs` | — | — | Yes | — | — | — |
| `minecraft:birch_trapdoor` | — | — | Yes | — | — | — |
| `minecraft:birch_wall_hanging_sign` | — | — | Yes | — | — | — |
| `minecraft:birch_wall_sign` | — | — | Yes | — | — | — |
| `minecraft:birch_wood` | — | — | Yes | — | — | — |
| `minecraft:black_banner` | Yes | Yes | Yes | Yes | Yes | Yes |
| `minecraft:black_bed` | Yes | Yes | Yes | Yes | Yes | Yes |
| `minecraft:black_candle` | Yes | Yes | Yes | — | — | — |
| `minecraft:black_candle_cake` | Yes | Yes | Yes | Yes | Yes | Yes |
| `minecraft:black_carpet` | Yes | Yes | Yes | Yes | Yes | Yes |
| `minecraft:black_concrete` | — | — | Yes | Yes | Yes | Yes |
| `minecraft:black_concrete_powder` | — | — | Yes | Yes | Yes | — |
| `minecraft:black_concrete_slab` | — | — | Yes | Yes | Yes | Yes |
| `minecraft:black_concrete_stairs` | — | — | Yes | Yes | Yes | Yes |
| `minecraft:black_glazed_terracotta` | — | — | — | — | Yes | — |
| `minecraft:black_shulker_box` | — | — | — | — | Yes | — |
| `minecraft:black_stained_glass` | — | — | Yes | Yes | Yes | Yes |
| `minecraft:black_stained_glass_pane` | — | — | Yes | Yes | Yes | Yes |
| `minecraft:black_terracotta` | — | — | — | — | Yes | — |
| `minecraft:black_wall_banner` | Yes | Yes | Yes | Yes | Yes | Yes |
| `minecraft:black_wool` | Yes | Yes | Yes | Yes | Yes | Yes |
| `minecraft:black_wool_slab` | Yes | Yes | Yes | Yes | Yes | Yes |
| `minecraft:black_wool_stairs` | Yes | Yes | Yes | Yes | Yes | Yes |
| `minecraft:blackstone` | — | — | — | — | — | Yes |
| `minecraft:blackstone_slab` | — | — | — | — | Yes | Yes |
| `minecraft:blackstone_stairs` | — | — | — | — | Yes | Yes |
| `minecraft:blackstone_wall` | — | — | — | — | Yes | Yes |
| `minecraft:blast_furnace` | — | — | Yes | Yes | — | — |
| `minecraft:blue_banner` | Yes | Yes | Yes | Yes | — | — |
| `minecraft:blue_bed` | Yes | Yes | Yes | Yes | — | — |
| `minecraft:blue_candle` | Yes | Yes | Yes | — | — | — |
| `minecraft:blue_candle_cake` | Yes | Yes | Yes | Yes | Yes | Yes |
| `minecraft:blue_carpet` | Yes | Yes | Yes | Yes | — | — |
| `minecraft:blue_concrete` | — | — | Yes | Yes | Yes | Yes |
| `minecraft:blue_concrete_powder` | — | — | Yes | Yes | — | — |
| `minecraft:blue_concrete_slab` | — | — | Yes | Yes | Yes | Yes |
| `minecraft:blue_concrete_stairs` | — | — | Yes | Yes | Yes | Yes |
| `minecraft:blue_glazed_terracotta` | — | — | — | — | — | — |
| `minecraft:blue_ice` | — | — | — | Yes | — | — |
| `minecraft:blue_orchid` | Yes | — | — | — | — | — |
| `minecraft:blue_shulker_box` | — | — | — | — | Yes | — |
| `minecraft:blue_stained_glass` | — | — | Yes | Yes | Yes | Yes |
| `minecraft:blue_stained_glass_pane` | — | — | Yes | Yes | Yes | Yes |
| `minecraft:blue_terracotta` | — | — | — | — | — | — |
| `minecraft:blue_wall_banner` | Yes | Yes | Yes | Yes | — | — |
| `minecraft:blue_wool` | Yes | Yes | Yes | Yes | — | — |
| `minecraft:blue_wool_slab` | Yes | Yes | Yes | Yes | — | — |
| `minecraft:blue_wool_stairs` | Yes | Yes | Yes | Yes | — | — |
| `minecraft:bone_block` | Yes | Yes | Yes | Yes | Yes | Yes |
| `minecraft:bookshelf` | Yes | — | Yes | — | — | — |
| `minecraft:brain_coral` | Yes | — | — | — | — | — |
| `minecraft:brain_coral_block` | Yes | — | — | — | — | — |
| `minecraft:brain_coral_fan` | Yes | — | — | — | — | — |
| `minecraft:brain_coral_wall_fan` | Yes | — | — | — | — | — |
| `minecraft:brewing_stand` | — | — | — | — | Yes | Yes |
| `minecraft:brick_slab` | — | — | Yes | — | — | — |
| `minecraft:brick_stairs` | — | — | Yes | — | — | — |
| `minecraft:brick_wall` | — | — | Yes | — | — | — |
| `minecraft:bricks` | — | — | Yes | — | — | — |
| `minecraft:brown_banner` | Yes | — | — | — | — | — |
| `minecraft:brown_bed` | Yes | — | — | — | — | — |
| `minecraft:brown_candle` | Yes | — | — | — | — | — |
| `minecraft:brown_candle_cake` | Yes | Yes | Yes | Yes | Yes | Yes |
| `minecraft:brown_carpet` | Yes | — | — | — | — | — |
| `minecraft:brown_concrete` | — | — | Yes | Yes | Yes | Yes |
| `minecraft:brown_concrete_powder` | — | — | — | — | — | — |
| `minecraft:brown_concrete_slab` | — | — | Yes | Yes | Yes | Yes |
| `minecraft:brown_concrete_stairs` | — | — | Yes | Yes | Yes | Yes |
| `minecraft:brown_glazed_terracotta` | — | — | — | — | — | — |
| `minecraft:brown_mushroom` | Yes | Yes | Yes | Yes | Yes | — |
| `minecraft:brown_mushroom_block` | Yes | — | — | Yes | — | — |
| `minecraft:brown_shulker_box` | — | — | — | — | Yes | — |
| `minecraft:brown_stained_glass` | — | — | Yes | Yes | Yes | Yes |
| `minecraft:brown_stained_glass_pane` | — | — | Yes | Yes | Yes | Yes |
| `minecraft:brown_terracotta` | — | — | — | — | — | — |
| `minecraft:brown_wall_banner` | Yes | — | — | — | — | — |
| `minecraft:brown_wool` | Yes | — | — | — | — | — |
| `minecraft:brown_wool_slab` | Yes | — | — | — | — | — |
| `minecraft:brown_wool_stairs` | Yes | — | — | — | — | — |
| `minecraft:bubble_column` | Yes | Yes | Yes | Yes | Yes | Yes |
| `minecraft:bubble_coral` | Yes | — | — | — | — | — |
| `minecraft:bubble_coral_block` | Yes | — | — | — | — | — |
| `minecraft:bubble_coral_fan` | Yes | — | — | — | — | — |
| `minecraft:bubble_coral_wall_fan` | Yes | — | — | — | — | — |
| `minecraft:budding_amethyst` | — | — | — | Yes | — | — |
| `minecraft:bush` | Yes | Yes | Yes | Yes | Yes | Yes |
| `minecraft:cactus` | Yes | — | — | — | Yes | — |
| `minecraft:cactus_flower` | Yes | Yes | Yes | Yes | Yes | Yes |
| `minecraft:cake` | Yes | Yes | Yes | — | — | — |
| `minecraft:calcite` | — | — | — | Yes | — | — |
| `minecraft:calibrated_sculk_sensor` | — | — | — | — | — | — |
| `minecraft:campfire` | Yes | Yes | Yes | Yes | Yes | Yes |
| `minecraft:candle` | Yes | Yes | Yes | — | — | — |
| `minecraft:candle_cake` | Yes | Yes | Yes | Yes | Yes | Yes |
| `minecraft:carrots` | Yes | Yes | Yes | — | — | — |
| `minecraft:cartography_table` | Yes | Yes | Yes | Yes | Yes | — |
| `minecraft:carved_pumpkin` | Yes | Yes | Yes | — | — | — |
| `minecraft:cauldron` | — | — | Yes | Yes | — | — |
| `minecraft:cave_air` | Yes | Yes | Yes | Yes | Yes | Yes |
| `minecraft:cave_vines` | Yes | — | — | — | — | — |
| `minecraft:cave_vines_plant` | Yes | — | — | — | — | — |
| `minecraft:chain` | — | — | Yes | Yes | — | — |
| `minecraft:chain_command_block` | Yes | Yes | Yes | Yes | Yes | Yes |
| `minecraft:cherry_button` | Yes | — | — | — | — | — |
| `minecraft:cherry_door` | Yes | — | — | — | — | — |
| `minecraft:cherry_fence` | Yes | — | — | — | — | — |
| `minecraft:cherry_fence_gate` | Yes | — | — | — | — | — |
| `minecraft:cherry_hanging_sign` | — | — | — | — | — | — |
| `minecraft:cherry_leaves` | Yes | — | — | — | — | — |
| `minecraft:cherry_log` | Yes | — | — | — | — | — |
| `minecraft:cherry_planks` | Yes | — | — | — | — | — |
| `minecraft:cherry_pressure_plate` | Yes | — | — | — | — | — |
| `minecraft:cherry_sapling` | Yes | — | — | — | — | — |
| `minecraft:cherry_shelf` | Yes | — | — | — | — | — |
| `minecraft:cherry_sign` | Yes | — | — | — | — | — |
| `minecraft:cherry_slab` | Yes | — | — | — | — | — |
| `minecraft:cherry_stairs` | Yes | — | — | — | — | — |
| `minecraft:cherry_trapdoor` | Yes | — | — | — | — | — |
| `minecraft:cherry_wall_hanging_sign` | — | — | — | — | — | — |
| `minecraft:cherry_wall_sign` | Yes | — | — | — | — | — |
| `minecraft:cherry_wood` | Yes | — | — | — | — | — |
| `minecraft:chest` | Yes | Yes | Yes | Yes | Yes | Yes |
| `minecraft:chipped_anvil` | — | — | Yes | Yes | Yes | Yes |
| `minecraft:chiseled_bookshelf` | Yes | Yes | Yes | Yes | Yes | Yes |
| `minecraft:chiseled_cinnabar` | — | — | — | — | Yes | — |
| `minecraft:chiseled_copper` | — | — | Yes | Yes | Yes | Yes |
| `minecraft:chiseled_deepslate` | — | — | — | Yes | Yes | — |
| `minecraft:chiseled_nether_bricks` | — | — | — | — | Yes | Yes |
| `minecraft:chiseled_polished_blackstone` | — | — | — | — | Yes | Yes |
| `minecraft:chiseled_quartz_block` | — | — | — | — | Yes | Yes |
| `minecraft:chiseled_red_sandstone` | — | — | — | — | Yes | Yes |
| `minecraft:chiseled_resin_bricks` | — | — | Yes | Yes | Yes | Yes |
| `minecraft:chiseled_sandstone` | — | — | Yes | Yes | Yes | — |
| `minecraft:chiseled_stone_bricks` | — | — | Yes | Yes | Yes | — |
| `minecraft:chiseled_sulfur` | — | — | — | — | — | Yes |
| `minecraft:chiseled_tuff` | — | — | — | Yes | Yes | — |
| `minecraft:chiseled_tuff_bricks` | — | — | — | Yes | Yes | — |
| `minecraft:chorus_flower` | — | — | — | — | Yes | — |
| `minecraft:chorus_plant` | — | — | — | — | Yes | — |
| `minecraft:cinnabar` | — | — | — | — | Yes | — |
| `minecraft:cinnabar_brick_slab` | — | — | — | — | Yes | — |
| `minecraft:cinnabar_brick_stairs` | — | — | — | — | Yes | — |
| `minecraft:cinnabar_brick_wall` | — | — | — | — | Yes | — |
| `minecraft:cinnabar_bricks` | — | — | — | — | Yes | — |
| `minecraft:cinnabar_slab` | — | — | — | — | Yes | — |
| `minecraft:cinnabar_stairs` | — | — | — | — | Yes | — |
| `minecraft:cinnabar_wall` | — | — | — | — | Yes | — |
| `minecraft:clay` | — | — | Yes | — | — | — |
| `minecraft:closed_eyeblossom` | Yes | Yes | Yes | Yes | Yes | Yes |
| `minecraft:coal_block` | — | — | Yes | Yes | Yes | Yes |
| `minecraft:coal_ore` | — | — | Yes | Yes | Yes | — |
| `minecraft:coarse_dirt` | — | — | Yes | Yes | — | — |
| `minecraft:cobbled_deepslate` | — | — | — | Yes | Yes | — |
| `minecraft:cobbled_deepslate_slab` | — | — | — | Yes | Yes | — |
| `minecraft:cobbled_deepslate_stairs` | — | — | — | Yes | Yes | — |
| `minecraft:cobbled_deepslate_wall` | — | — | — | Yes | Yes | — |
| `minecraft:cobblestone` | — | — | Yes | Yes | Yes | — |
| `minecraft:cobblestone_slab` | — | — | Yes | Yes | Yes | — |
| `minecraft:cobblestone_stairs` | — | — | Yes | Yes | Yes | — |
| `minecraft:cobblestone_wall` | — | — | Yes | Yes | Yes | — |
| `minecraft:cobweb` | Yes | Yes | Yes | Yes | Yes | Yes |
| `minecraft:cocoa` | Yes | — | — | — | — | — |
| `minecraft:command_block` | Yes | Yes | Yes | Yes | Yes | Yes |
| `minecraft:comparator` | — | — | — | — | Yes | — |
| `minecraft:composter` | Yes | Yes | Yes | Yes | Yes | Yes |
| `minecraft:conduit` | — | — | Yes | Yes | Yes | Yes |
| `minecraft:copper_bars` | — | — | Yes | Yes | Yes | Yes |
| `minecraft:copper_block` | — | — | Yes | Yes | Yes | Yes |
| `minecraft:copper_bulb` | — | — | — | — | Yes | Yes |
| `minecraft:copper_chain` | — | — | Yes | Yes | Yes | Yes |
| `minecraft:copper_chest` | — | — | Yes | Yes | Yes | Yes |
| `minecraft:copper_door` | — | — | Yes | Yes | Yes | Yes |
| `minecraft:copper_golem_statue` | — | — | Yes | Yes | Yes | Yes |
| `minecraft:copper_grate` | — | — | Yes | Yes | Yes | Yes |
| `minecraft:copper_lantern` | — | — | Yes | Yes | Yes | Yes |
| `minecraft:copper_ore` | — | — | Yes | Yes | Yes | — |
| `minecraft:copper_torch` | — | — | Yes | Yes | Yes | Yes |
| `minecraft:copper_trapdoor` | — | — | Yes | Yes | Yes | Yes |
| `minecraft:copper_wall_torch` | — | — | Yes | Yes | Yes | Yes |
| `minecraft:cornflower` | Yes | Yes | Yes | — | — | — |
| `minecraft:cracked_deepslate_bricks` | — | — | — | Yes | Yes | — |
| `minecraft:cracked_deepslate_tiles` | — | — | — | Yes | Yes | — |
| `minecraft:cracked_nether_bricks` | — | — | — | — | Yes | Yes |
| `minecraft:cracked_polished_blackstone_bricks` | — | — | — | — | Yes | Yes |
| `minecraft:cracked_stone_bricks` | — | — | Yes | Yes | Yes | — |
| `minecraft:crafting_table` | Yes | Yes | Yes | Yes | Yes | Yes |
| `minecraft:creaking_heart` | Yes | Yes | Yes | Yes | Yes | Yes |
| `minecraft:creeper_head` | Yes | Yes | Yes | Yes | Yes | Yes |
| `minecraft:creeper_wall_head` | Yes | Yes | Yes | Yes | Yes | Yes |
| `minecraft:crimson_button` | — | — | — | — | — | Yes |
| `minecraft:crimson_door` | — | — | — | — | — | Yes |
| `minecraft:crimson_fence` | — | — | — | — | — | Yes |
| `minecraft:crimson_fence_gate` | — | — | — | — | — | Yes |
| `minecraft:crimson_fungus` | — | — | — | — | — | Yes |
| `minecraft:crimson_hanging_sign` | — | — | — | — | — | — |
| `minecraft:crimson_hyphae` | — | — | — | — | — | Yes |
| `minecraft:crimson_nylium` | — | — | — | — | — | Yes |
| `minecraft:crimson_planks` | — | — | — | — | — | Yes |
| `minecraft:crimson_pressure_plate` | — | — | — | — | — | Yes |
| `minecraft:crimson_roots` | — | — | — | — | — | Yes |
| `minecraft:crimson_shelf` | — | — | — | — | — | Yes |
| `minecraft:crimson_sign` | — | — | — | — | — | Yes |
| `minecraft:crimson_slab` | — | — | — | — | — | Yes |
| `minecraft:crimson_stairs` | — | — | — | — | — | Yes |
| `minecraft:crimson_stem` | — | — | — | — | — | Yes |
| `minecraft:crimson_trapdoor` | — | — | — | — | — | Yes |
| `minecraft:crimson_wall_hanging_sign` | — | — | — | — | — | — |
| `minecraft:crimson_wall_sign` | — | — | — | — | — | Yes |
| `minecraft:crying_obsidian` | — | — | — | — | — | Yes |
| `minecraft:cut_copper` | — | — | Yes | Yes | Yes | Yes |
| `minecraft:cut_copper_slab` | — | — | Yes | Yes | Yes | Yes |
| `minecraft:cut_copper_stairs` | — | — | Yes | Yes | Yes | Yes |
| `minecraft:cut_red_sandstone` | — | — | — | — | Yes | Yes |
| `minecraft:cut_red_sandstone_slab` | — | — | — | — | Yes | Yes |
| `minecraft:cut_sandstone` | — | — | Yes | Yes | Yes | — |
| `minecraft:cut_sandstone_slab` | — | — | Yes | Yes | Yes | — |
| `minecraft:cyan_banner` | Yes | — | — | — | — | — |
| `minecraft:cyan_bed` | Yes | — | — | — | — | — |
| `minecraft:cyan_candle` | Yes | — | — | — | — | — |
| `minecraft:cyan_candle_cake` | Yes | Yes | Yes | Yes | Yes | Yes |
| `minecraft:cyan_carpet` | Yes | — | — | — | — | — |
| `minecraft:cyan_concrete` | — | — | Yes | Yes | Yes | Yes |
| `minecraft:cyan_concrete_powder` | — | — | — | — | — | — |
| `minecraft:cyan_concrete_slab` | — | — | Yes | Yes | Yes | Yes |
| `minecraft:cyan_concrete_stairs` | — | — | Yes | Yes | Yes | Yes |
| `minecraft:cyan_glazed_terracotta` | — | — | — | — | — | — |
| `minecraft:cyan_shulker_box` | — | — | — | — | Yes | — |
| `minecraft:cyan_stained_glass` | — | — | Yes | Yes | Yes | Yes |
| `minecraft:cyan_stained_glass_pane` | — | — | Yes | Yes | Yes | Yes |
| `minecraft:cyan_terracotta` | — | — | — | — | — | — |
| `minecraft:cyan_wall_banner` | Yes | — | — | — | — | — |
| `minecraft:cyan_wool` | Yes | — | — | — | — | — |
| `minecraft:cyan_wool_slab` | Yes | — | — | — | — | — |
| `minecraft:cyan_wool_stairs` | Yes | — | — | — | — | — |
| `minecraft:damaged_anvil` | — | — | Yes | Yes | Yes | Yes |
| `minecraft:dandelion` | Yes | Yes | Yes | — | — | — |
| `minecraft:dark_oak_button` | Yes | — | — | — | — | — |
| `minecraft:dark_oak_door` | Yes | — | — | — | — | — |
| `minecraft:dark_oak_fence` | Yes | — | — | — | — | — |
| `minecraft:dark_oak_fence_gate` | Yes | — | — | — | — | — |
| `minecraft:dark_oak_hanging_sign` | — | — | — | — | — | — |
| `minecraft:dark_oak_leaves` | Yes | — | — | — | — | — |
| `minecraft:dark_oak_log` | Yes | — | — | — | — | — |
| `minecraft:dark_oak_planks` | Yes | — | — | — | — | — |
| `minecraft:dark_oak_pressure_plate` | Yes | — | — | — | — | — |
| `minecraft:dark_oak_sapling` | Yes | — | — | — | — | — |
| `minecraft:dark_oak_shelf` | Yes | — | — | — | — | — |
| `minecraft:dark_oak_sign` | Yes | — | — | — | — | — |
| `minecraft:dark_oak_slab` | Yes | — | — | — | — | — |
| `minecraft:dark_oak_stairs` | Yes | — | — | — | — | — |
| `minecraft:dark_oak_trapdoor` | Yes | — | — | — | — | — |
| `minecraft:dark_oak_wall_hanging_sign` | — | — | — | — | — | — |
| `minecraft:dark_oak_wall_sign` | Yes | — | — | — | — | — |
| `minecraft:dark_oak_wood` | Yes | — | — | — | — | — |
| `minecraft:dark_prismarine` | — | — | — | Yes | — | — |
| `minecraft:dark_prismarine_slab` | — | — | — | Yes | — | — |
| `minecraft:dark_prismarine_stairs` | — | — | — | Yes | — | — |
| `minecraft:daylight_detector` | — | — | — | — | Yes | Yes |
| `minecraft:dead_brain_coral` | — | — | Yes | Yes | Yes | Yes |
| `minecraft:dead_brain_coral_block` | — | — | Yes | Yes | Yes | Yes |
| `minecraft:dead_brain_coral_fan` | — | — | Yes | Yes | Yes | Yes |
| `minecraft:dead_brain_coral_wall_fan` | — | — | Yes | Yes | Yes | Yes |
| `minecraft:dead_bubble_coral` | — | — | Yes | Yes | Yes | Yes |
| `minecraft:dead_bubble_coral_block` | — | — | Yes | Yes | Yes | Yes |
| `minecraft:dead_bubble_coral_fan` | — | — | Yes | Yes | Yes | Yes |
| `minecraft:dead_bubble_coral_wall_fan` | — | — | Yes | Yes | Yes | Yes |
| `minecraft:dead_bush` | — | — | — | — | Yes | — |
| `minecraft:dead_fire_coral` | — | — | Yes | Yes | Yes | Yes |
| `minecraft:dead_fire_coral_block` | — | — | Yes | Yes | Yes | Yes |
| `minecraft:dead_fire_coral_fan` | — | — | Yes | Yes | Yes | Yes |
| `minecraft:dead_fire_coral_wall_fan` | — | — | Yes | Yes | Yes | Yes |
| `minecraft:dead_horn_coral` | — | — | Yes | Yes | Yes | Yes |
| `minecraft:dead_horn_coral_block` | — | — | Yes | Yes | Yes | Yes |
| `minecraft:dead_horn_coral_fan` | — | — | Yes | Yes | Yes | Yes |
| `minecraft:dead_horn_coral_wall_fan` | — | — | Yes | Yes | Yes | Yes |
| `minecraft:dead_tube_coral` | — | — | Yes | Yes | Yes | Yes |
| `minecraft:dead_tube_coral_block` | — | — | Yes | Yes | Yes | Yes |
| `minecraft:dead_tube_coral_fan` | — | — | Yes | Yes | Yes | Yes |
| `minecraft:dead_tube_coral_wall_fan` | — | — | Yes | Yes | Yes | Yes |
| `minecraft:decorated_pot` | — | — | Yes | — | — | — |
| `minecraft:deepslate` | — | — | — | Yes | Yes | — |
| `minecraft:deepslate_brick_slab` | — | — | — | Yes | Yes | — |
| `minecraft:deepslate_brick_stairs` | — | — | — | Yes | Yes | — |
| `minecraft:deepslate_brick_wall` | — | — | — | Yes | Yes | — |
| `minecraft:deepslate_bricks` | — | — | — | Yes | Yes | — |
| `minecraft:deepslate_coal_ore` | — | — | Yes | Yes | Yes | — |
| `minecraft:deepslate_copper_ore` | — | — | Yes | Yes | Yes | — |
| `minecraft:deepslate_diamond_ore` | — | — | — | Yes | — | — |
| `minecraft:deepslate_emerald_ore` | — | — | — | Yes | — | — |
| `minecraft:deepslate_gold_ore` | — | — | Yes | Yes | Yes | — |
| `minecraft:deepslate_iron_ore` | — | — | Yes | Yes | — | — |
| `minecraft:deepslate_lapis_ore` | — | — | — | Yes | — | — |
| `minecraft:deepslate_redstone_ore` | — | — | — | — | Yes | — |
| `minecraft:deepslate_tile_slab` | — | — | — | Yes | Yes | — |
| `minecraft:deepslate_tile_stairs` | — | — | — | Yes | Yes | — |
| `minecraft:deepslate_tile_wall` | — | — | — | Yes | Yes | — |
| `minecraft:deepslate_tiles` | — | — | — | Yes | Yes | — |
| `minecraft:detector_rail` | — | — | — | — | Yes | — |
| `minecraft:diamond_block` | — | — | — | Yes | — | — |
| `minecraft:diamond_ore` | — | — | — | Yes | — | — |
| `minecraft:diorite` | — | — | Yes | Yes | Yes | — |
| `minecraft:diorite_slab` | — | — | Yes | Yes | Yes | — |
| `minecraft:diorite_stairs` | — | — | Yes | Yes | Yes | — |
| `minecraft:diorite_wall` | — | — | Yes | Yes | Yes | — |
| `minecraft:dirt` | — | Yes | Yes | Yes | Yes | Yes |
| `minecraft:dirt_path` | — | Yes | Yes | Yes | Yes | Yes |
| `minecraft:dispenser` | — | — | — | — | Yes | — |
| `minecraft:dragon_egg` | — | — | — | — | Yes | — |
| `minecraft:dragon_head` | — | — | — | — | Yes | — |
| `minecraft:dragon_wall_head` | — | — | — | — | Yes | — |
| `minecraft:dried_ghast` | Yes | Yes | Yes | Yes | Yes | Yes |
| `minecraft:dried_kelp_block` | Yes | Yes | — | Yes | — | — |
| `minecraft:dripstone_block` | — | — | — | Yes | — | — |
| `minecraft:dropper` | — | — | — | — | Yes | — |
| `minecraft:emerald_block` | — | — | — | Yes | — | — |
| `minecraft:emerald_ore` | — | — | — | Yes | — | — |
| `minecraft:enchanting_table` | — | — | — | — | — | — |
| `minecraft:end_gateway` | — | — | — | — | Yes | — |
| `minecraft:end_portal` | — | — | — | — | Yes | — |
| `minecraft:end_portal_frame` | — | — | — | — | Yes | — |
| `minecraft:end_rod` | — | — | — | — | Yes | — |
| `minecraft:end_stone` | — | — | — | — | Yes | — |
| `minecraft:end_stone_brick_slab` | — | — | — | — | Yes | — |
| `minecraft:end_stone_brick_stairs` | — | — | — | — | Yes | — |
| `minecraft:end_stone_brick_wall` | — | — | — | — | Yes | — |
| `minecraft:end_stone_bricks` | — | — | — | — | Yes | — |
| `minecraft:ender_chest` | — | — | — | — | — | — |
| `minecraft:exposed_chiseled_copper` | — | — | Yes | Yes | Yes | Yes |
| `minecraft:exposed_copper` | — | — | Yes | Yes | Yes | Yes |
| `minecraft:exposed_copper_bars` | — | — | Yes | Yes | Yes | Yes |
| `minecraft:exposed_copper_bulb` | — | — | — | — | Yes | Yes |
| `minecraft:exposed_copper_chain` | — | — | Yes | Yes | Yes | Yes |
| `minecraft:exposed_copper_chest` | — | — | Yes | Yes | Yes | Yes |
| `minecraft:exposed_copper_door` | — | — | Yes | Yes | Yes | Yes |
| `minecraft:exposed_copper_golem_statue` | — | — | Yes | Yes | Yes | Yes |
| `minecraft:exposed_copper_grate` | — | — | Yes | Yes | Yes | Yes |
| `minecraft:exposed_copper_lantern` | — | — | Yes | Yes | Yes | Yes |
| `minecraft:exposed_copper_trapdoor` | — | — | Yes | Yes | Yes | Yes |
| `minecraft:exposed_cut_copper` | — | — | Yes | Yes | Yes | Yes |
| `minecraft:exposed_cut_copper_slab` | — | — | Yes | Yes | Yes | Yes |
| `minecraft:exposed_cut_copper_stairs` | — | — | Yes | Yes | Yes | Yes |
| `minecraft:exposed_lightning_rod` | — | — | Yes | Yes | Yes | Yes |
| `minecraft:farmland` | — | Yes | Yes | Yes | — | — |
| `minecraft:fern` | Yes | — | — | Yes | — | — |
| `minecraft:fire` | — | Yes | Yes | Yes | Yes | Yes |
| `minecraft:fire_coral` | Yes | — | — | — | — | — |
| `minecraft:fire_coral_block` | Yes | — | — | — | — | — |
| `minecraft:fire_coral_fan` | Yes | — | — | — | — | — |
| `minecraft:fire_coral_wall_fan` | Yes | — | — | — | — | — |
| `minecraft:firefly_bush` | Yes | — | — | — | — | — |
| `minecraft:fletching_table` | — | — | Yes | Yes | Yes | — |
| `minecraft:flower_pot` | — | — | Yes | — | — | — |
| `minecraft:flowering_azalea` | Yes | — | — | — | — | — |
| `minecraft:flowering_azalea_leaves` | Yes | — | — | — | — | — |
| `minecraft:frogspawn` | Yes | — | — | — | — | — |
| `minecraft:frosted_ice` | — | — | — | Yes | — | — |
| `minecraft:furnace` | — | — | Yes | Yes | Yes | — |
| `minecraft:gilded_blackstone` | — | — | — | — | — | Yes |
| `minecraft:glass` | — | — | Yes | Yes | Yes | Yes |
| `minecraft:glass_pane` | — | — | Yes | Yes | Yes | Yes |
| `minecraft:glow_lichen` | Yes | Yes | Yes | Yes | Yes | — |
| `minecraft:glowstone` | — | — | — | — | Yes | Yes |
| `minecraft:gold_block` | — | — | Yes | Yes | Yes | Yes |
| `minecraft:gold_ore` | — | — | Yes | Yes | Yes | — |
| `minecraft:golden_dandelion` | Yes | Yes | Yes | Yes | Yes | Yes |
| `minecraft:granite` | — | — | Yes | Yes | Yes | — |
| `minecraft:granite_slab` | — | — | Yes | Yes | Yes | — |
| `minecraft:granite_stairs` | — | — | Yes | Yes | Yes | — |
| `minecraft:granite_wall` | — | — | Yes | Yes | Yes | — |
| `minecraft:grass` | Yes | Yes | Yes | Yes | — | — |
| `minecraft:grass_block` | — | Yes | Yes | Yes | — | — |
| `minecraft:gravel` | — | — | Yes | Yes | Yes | — |
| `minecraft:gray_banner` | Yes | Yes | Yes | Yes | Yes | Yes |
| `minecraft:gray_bed` | Yes | Yes | Yes | Yes | Yes | Yes |
| `minecraft:gray_candle` | Yes | Yes | Yes | — | — | — |
| `minecraft:gray_candle_cake` | Yes | Yes | Yes | Yes | Yes | Yes |
| `minecraft:gray_carpet` | Yes | Yes | Yes | Yes | Yes | Yes |
| `minecraft:gray_concrete` | — | — | Yes | Yes | Yes | Yes |
| `minecraft:gray_concrete_powder` | — | — | Yes | Yes | Yes | — |
| `minecraft:gray_concrete_slab` | — | — | Yes | Yes | Yes | Yes |
| `minecraft:gray_concrete_stairs` | — | — | Yes | Yes | Yes | Yes |
| `minecraft:gray_glazed_terracotta` | — | — | — | — | Yes | — |
| `minecraft:gray_shulker_box` | — | — | — | — | Yes | — |
| `minecraft:gray_stained_glass` | — | — | Yes | Yes | Yes | Yes |
| `minecraft:gray_stained_glass_pane` | — | — | Yes | Yes | Yes | Yes |
| `minecraft:gray_terracotta` | — | — | — | — | Yes | — |
| `minecraft:gray_wall_banner` | Yes | Yes | Yes | Yes | Yes | Yes |
| `minecraft:gray_wool` | Yes | Yes | Yes | Yes | Yes | Yes |
| `minecraft:gray_wool_slab` | Yes | Yes | Yes | Yes | Yes | Yes |
| `minecraft:gray_wool_stairs` | Yes | Yes | Yes | Yes | Yes | Yes |
| `minecraft:green_banner` | Yes | — | — | — | Yes | — |
| `minecraft:green_bed` | Yes | — | — | — | Yes | — |
| `minecraft:green_candle` | Yes | — | — | — | — | — |
| `minecraft:green_candle_cake` | Yes | Yes | Yes | Yes | Yes | Yes |
| `minecraft:green_carpet` | Yes | — | — | — | Yes | — |
| `minecraft:green_concrete` | — | — | Yes | Yes | Yes | Yes |
| `minecraft:green_concrete_powder` | — | — | — | — | Yes | — |
| `minecraft:green_concrete_slab` | — | — | Yes | Yes | Yes | Yes |
| `minecraft:green_concrete_stairs` | — | — | Yes | Yes | Yes | Yes |
| `minecraft:green_glazed_terracotta` | — | — | — | — | Yes | — |
| `minecraft:green_shulker_box` | — | — | — | — | Yes | — |
| `minecraft:green_stained_glass` | — | — | Yes | Yes | Yes | Yes |
| `minecraft:green_stained_glass_pane` | — | — | Yes | Yes | Yes | Yes |
| `minecraft:green_terracotta` | — | — | — | — | Yes | — |
| `minecraft:green_wall_banner` | Yes | — | — | — | Yes | — |
| `minecraft:green_wool` | Yes | — | — | — | Yes | — |
| `minecraft:green_wool_slab` | Yes | — | — | — | Yes | — |
| `minecraft:green_wool_stairs` | Yes | — | — | — | Yes | — |
| `minecraft:grindstone` | — | — | Yes | Yes | Yes | — |
| `minecraft:hanging_roots` | — | Yes | — | — | — | — |
| `minecraft:hay_block` | Yes | Yes | Yes | — | — | — |
| `minecraft:heavy_weighted_pressure_plate` | — | — | Yes | Yes | Yes | Yes |
| `minecraft:honey_block` | Yes | Yes | Yes | — | — | — |
| `minecraft:honeycomb_block` | Yes | Yes | Yes | — | — | — |
| `minecraft:hopper` | — | — | Yes | Yes | Yes | Yes |
| `minecraft:horn_coral` | Yes | — | — | — | — | — |
| `minecraft:horn_coral_block` | Yes | — | — | — | — | — |
| `minecraft:horn_coral_fan` | Yes | — | — | — | — | — |
| `minecraft:horn_coral_wall_fan` | Yes | — | — | — | — | — |
| `minecraft:ice` | — | — | — | Yes | — | — |
| `minecraft:infested_chiseled_stone_bricks` | — | — | Yes | Yes | Yes | — |
| `minecraft:infested_cobblestone` | — | — | Yes | Yes | Yes | — |
| `minecraft:infested_cracked_stone_bricks` | — | — | Yes | Yes | Yes | — |
| `minecraft:infested_deepslate` | — | — | — | Yes | Yes | — |
| `minecraft:infested_mossy_stone_bricks` | — | — | Yes | Yes | Yes | — |
| `minecraft:infested_stone` | — | — | Yes | Yes | Yes | — |
| `minecraft:infested_stone_bricks` | — | — | Yes | Yes | Yes | — |
| `minecraft:iron_bars` | — | — | Yes | Yes | — | — |
| `minecraft:iron_block` | — | — | Yes | Yes | — | — |
| `minecraft:iron_door` | — | — | Yes | Yes | — | — |
| `minecraft:iron_ore` | — | — | Yes | Yes | — | — |
| `minecraft:iron_trapdoor` | — | — | Yes | Yes | — | — |
| `minecraft:jack_o_lantern` | Yes | Yes | Yes | — | — | — |
| `minecraft:jigsaw` | Yes | Yes | Yes | Yes | Yes | Yes |
| `minecraft:jukebox` | — | — | — | Yes | — | — |
| `minecraft:jungle_button` | Yes | — | — | — | — | — |
| `minecraft:jungle_door` | Yes | — | — | — | — | — |
| `minecraft:jungle_fence` | Yes | — | — | — | — | — |
| `minecraft:jungle_fence_gate` | Yes | — | — | — | — | — |
| `minecraft:jungle_hanging_sign` | — | — | — | — | — | — |
| `minecraft:jungle_leaves` | Yes | — | — | — | — | — |
| `minecraft:jungle_log` | Yes | — | — | — | — | — |
| `minecraft:jungle_planks` | Yes | — | — | — | — | — |
| `minecraft:jungle_pressure_plate` | Yes | — | — | — | — | — |
| `minecraft:jungle_sapling` | Yes | — | — | — | — | — |
| `minecraft:jungle_shelf` | Yes | — | — | — | — | — |
| `minecraft:jungle_sign` | Yes | — | — | — | — | — |
| `minecraft:jungle_slab` | Yes | — | — | — | — | — |
| `minecraft:jungle_stairs` | Yes | — | — | — | — | — |
| `minecraft:jungle_trapdoor` | Yes | — | — | — | — | — |
| `minecraft:jungle_wall_hanging_sign` | — | — | — | — | — | — |
| `minecraft:jungle_wall_sign` | Yes | — | — | — | — | — |
| `minecraft:jungle_wood` | Yes | — | — | — | — | — |
| `minecraft:kelp` | Yes | Yes | — | Yes | — | — |
| `minecraft:kelp_plant` | Yes | Yes | — | Yes | — | — |
| `minecraft:ladder` | Yes | — | Yes | — | — | — |
| `minecraft:lantern` | — | — | Yes | Yes | — | — |
| `minecraft:lapis_block` | — | — | — | Yes | — | — |
| `minecraft:lapis_ore` | — | — | — | Yes | — | — |
| `minecraft:large_amethyst_bud` | — | — | — | Yes | — | — |
| `minecraft:large_fern` | Yes | — | — | Yes | — | — |
| `minecraft:lava` | — | — | — | — | Yes | Yes |
| `minecraft:lava_cauldron` | — | — | — | — | — | — |
| `minecraft:leaf_litter` | Yes | Yes | Yes | Yes | Yes | Yes |
| `minecraft:lectern` | Yes | Yes | Yes | Yes | Yes | — |
| `minecraft:lever` | — | — | Yes | Yes | Yes | — |
| `minecraft:light` | Yes | Yes | Yes | Yes | Yes | Yes |
| `minecraft:light_blue_banner` | Yes | Yes | Yes | Yes | — | — |
| `minecraft:light_blue_bed` | Yes | Yes | Yes | Yes | — | — |
| `minecraft:light_blue_candle` | Yes | Yes | Yes | — | — | — |
| `minecraft:light_blue_candle_cake` | Yes | Yes | Yes | Yes | Yes | Yes |
| `minecraft:light_blue_carpet` | Yes | Yes | Yes | Yes | — | — |
| `minecraft:light_blue_concrete` | — | — | Yes | Yes | Yes | Yes |
| `minecraft:light_blue_concrete_powder` | — | — | Yes | Yes | — | — |
| `minecraft:light_blue_concrete_slab` | — | — | Yes | Yes | Yes | Yes |
| `minecraft:light_blue_concrete_stairs` | — | — | Yes | Yes | Yes | Yes |
| `minecraft:light_blue_glazed_terracotta` | — | — | — | — | — | — |
| `minecraft:light_blue_shulker_box` | — | — | — | — | Yes | — |
| `minecraft:light_blue_stained_glass` | — | — | Yes | Yes | Yes | Yes |
| `minecraft:light_blue_stained_glass_pane` | — | — | Yes | Yes | Yes | Yes |
| `minecraft:light_blue_terracotta` | — | — | — | — | — | — |
| `minecraft:light_blue_wall_banner` | Yes | Yes | Yes | Yes | — | — |
| `minecraft:light_blue_wool` | Yes | Yes | Yes | Yes | — | — |
| `minecraft:light_blue_wool_slab` | Yes | Yes | Yes | Yes | — | — |
| `minecraft:light_blue_wool_stairs` | Yes | Yes | Yes | Yes | — | — |
| `minecraft:light_gray_banner` | Yes | Yes | Yes | Yes | Yes | Yes |
| `minecraft:light_gray_bed` | Yes | Yes | Yes | Yes | Yes | Yes |
| `minecraft:light_gray_candle` | Yes | Yes | Yes | — | — | — |
| `minecraft:light_gray_candle_cake` | Yes | Yes | Yes | Yes | Yes | Yes |
| `minecraft:light_gray_carpet` | Yes | Yes | Yes | Yes | Yes | Yes |
| `minecraft:light_gray_concrete` | — | — | Yes | Yes | Yes | Yes |
| `minecraft:light_gray_concrete_powder` | — | — | Yes | Yes | Yes | — |
| `minecraft:light_gray_concrete_slab` | — | — | Yes | Yes | Yes | Yes |
| `minecraft:light_gray_concrete_stairs` | — | — | Yes | Yes | Yes | Yes |
| `minecraft:light_gray_glazed_terracotta` | — | — | — | — | Yes | — |
| `minecraft:light_gray_shulker_box` | — | — | — | — | Yes | — |
| `minecraft:light_gray_stained_glass` | — | — | Yes | Yes | Yes | Yes |
| `minecraft:light_gray_stained_glass_pane` | — | — | Yes | Yes | Yes | Yes |
| `minecraft:light_gray_terracotta` | — | — | — | — | Yes | — |
| `minecraft:light_gray_wall_banner` | Yes | Yes | Yes | Yes | Yes | Yes |
| `minecraft:light_gray_wool` | Yes | Yes | Yes | Yes | Yes | Yes |
| `minecraft:light_gray_wool_slab` | Yes | Yes | Yes | Yes | Yes | Yes |
| `minecraft:light_gray_wool_stairs` | Yes | Yes | Yes | Yes | Yes | Yes |
| `minecraft:light_weighted_pressure_plate` | — | — | Yes | Yes | Yes | Yes |
| `minecraft:lightning_rod` | — | — | Yes | Yes | Yes | Yes |
| `minecraft:lilac` | Yes | — | — | — | — | — |
| `minecraft:lily_of_the_valley` | Yes | Yes | Yes | — | — | — |
| `minecraft:lily_pad` | Yes | — | — | — | — | — |
| `minecraft:lime_banner` | Yes | Yes | — | Yes | Yes | — |
| `minecraft:lime_bed` | Yes | Yes | — | Yes | Yes | — |
| `minecraft:lime_candle` | Yes | Yes | — | — | — | — |
| `minecraft:lime_candle_cake` | Yes | Yes | Yes | Yes | Yes | Yes |
| `minecraft:lime_carpet` | Yes | Yes | — | Yes | Yes | — |
| `minecraft:lime_concrete` | — | — | Yes | Yes | Yes | Yes |
| `minecraft:lime_concrete_powder` | — | — | — | Yes | Yes | — |
| `minecraft:lime_concrete_slab` | — | — | Yes | Yes | Yes | Yes |
| `minecraft:lime_concrete_stairs` | — | — | Yes | Yes | Yes | Yes |
| `minecraft:lime_glazed_terracotta` | — | — | — | — | Yes | — |
| `minecraft:lime_shulker_box` | — | — | — | — | Yes | — |
| `minecraft:lime_stained_glass` | — | — | Yes | Yes | Yes | Yes |
| `minecraft:lime_stained_glass_pane` | — | — | Yes | Yes | Yes | Yes |
| `minecraft:lime_terracotta` | — | — | — | — | Yes | — |
| `minecraft:lime_wall_banner` | Yes | Yes | — | Yes | Yes | — |
| `minecraft:lime_wool` | Yes | Yes | — | Yes | Yes | — |
| `minecraft:lime_wool_slab` | Yes | Yes | — | Yes | Yes | — |
| `minecraft:lime_wool_stairs` | Yes | Yes | — | Yes | Yes | — |
| `minecraft:lodestone` | — | — | — | — | Yes | — |
| `minecraft:loom` | Yes | Yes | Yes | Yes | Yes | Yes |
| `minecraft:magenta_banner` | Yes | Yes | Yes | Yes | — | — |
| `minecraft:magenta_bed` | Yes | Yes | Yes | Yes | — | — |
| `minecraft:magenta_candle` | Yes | Yes | Yes | — | — | — |
| `minecraft:magenta_candle_cake` | Yes | Yes | Yes | Yes | Yes | Yes |
| `minecraft:magenta_carpet` | Yes | Yes | Yes | Yes | — | — |
| `minecraft:magenta_concrete` | — | — | Yes | Yes | Yes | Yes |
| `minecraft:magenta_concrete_powder` | — | — | Yes | Yes | — | — |
| `minecraft:magenta_concrete_slab` | — | — | Yes | Yes | Yes | Yes |
| `minecraft:magenta_concrete_stairs` | — | — | Yes | Yes | Yes | Yes |
| `minecraft:magenta_glazed_terracotta` | — | — | — | — | — | — |
| `minecraft:magenta_shulker_box` | — | — | — | — | Yes | — |
| `minecraft:magenta_stained_glass` | — | — | Yes | Yes | Yes | Yes |
| `minecraft:magenta_stained_glass_pane` | — | — | Yes | Yes | Yes | Yes |
| `minecraft:magenta_terracotta` | — | — | — | — | — | — |
| `minecraft:magenta_wall_banner` | Yes | Yes | Yes | Yes | — | — |
| `minecraft:magenta_wool` | Yes | Yes | Yes | Yes | — | — |
| `minecraft:magenta_wool_slab` | Yes | Yes | Yes | Yes | — | — |
| `minecraft:magenta_wool_stairs` | Yes | Yes | Yes | Yes | — | — |
| `minecraft:magma_block` | — | — | — | — | — | Yes |
| `minecraft:mangrove_button` | Yes | — | — | — | — | — |
| `minecraft:mangrove_door` | Yes | — | — | — | — | — |
| `minecraft:mangrove_fence` | Yes | — | — | — | — | — |
| `minecraft:mangrove_fence_gate` | Yes | — | — | — | — | — |
| `minecraft:mangrove_hanging_sign` | — | — | — | — | — | — |
| `minecraft:mangrove_leaves` | Yes | — | — | — | — | — |
| `minecraft:mangrove_log` | Yes | — | — | — | — | — |
| `minecraft:mangrove_planks` | Yes | — | — | — | — | — |
| `minecraft:mangrove_pressure_plate` | Yes | — | — | — | — | — |
| `minecraft:mangrove_propagule` | Yes | — | — | — | — | — |
| `minecraft:mangrove_roots` | Yes | — | — | — | — | — |
| `minecraft:mangrove_shelf` | Yes | — | — | — | — | — |
| `minecraft:mangrove_sign` | Yes | — | — | — | — | — |
| `minecraft:mangrove_slab` | Yes | — | — | — | — | — |
| `minecraft:mangrove_stairs` | Yes | — | — | — | — | — |
| `minecraft:mangrove_trapdoor` | Yes | — | — | — | — | — |
| `minecraft:mangrove_wall_hanging_sign` | — | — | — | — | — | — |
| `minecraft:mangrove_wall_sign` | Yes | — | — | — | — | — |
| `minecraft:mangrove_wood` | Yes | — | — | — | — | — |
| `minecraft:medium_amethyst_bud` | — | — | — | Yes | — | — |
| `minecraft:melon` | Yes | — | — | — | — | — |
| `minecraft:melon_stem` | Yes | — | — | — | — | — |
| `minecraft:moss_block` | Yes | — | — | — | — | — |
| `minecraft:moss_carpet` | Yes | — | — | — | — | — |
| `minecraft:mossy_cobblestone` | — | — | — | — | — | — |
| `minecraft:mossy_cobblestone_slab` | — | — | — | — | — | — |
| `minecraft:mossy_cobblestone_stairs` | — | — | — | — | — | — |
| `minecraft:mossy_cobblestone_wall` | — | — | — | — | — | — |
| `minecraft:mossy_stone_brick_slab` | — | — | — | — | — | — |
| `minecraft:mossy_stone_brick_stairs` | — | — | — | — | — | — |
| `minecraft:mossy_stone_brick_wall` | — | — | — | — | — | — |
| `minecraft:mossy_stone_bricks` | — | — | — | — | — | — |
| `minecraft:moving_piston` | Yes | Yes | Yes | Yes | Yes | Yes |
| `minecraft:mud` | — | Yes | — | Yes | — | — |
| `minecraft:mud_brick_slab` | — | — | — | — | — | — |
| `minecraft:mud_brick_stairs` | — | — | — | — | — | — |
| `minecraft:mud_brick_wall` | — | — | — | — | — | — |
| `minecraft:mud_bricks` | — | — | — | — | — | — |
| `minecraft:muddy_mangrove_roots` | — | — | — | — | — | — |
| `minecraft:mushroom_stem` | Yes | — | — | Yes | — | — |
| `minecraft:mycelium` | — | — | — | Yes | — | — |
| `minecraft:nether_brick_fence` | — | — | — | — | Yes | Yes |
| `minecraft:nether_brick_slab` | — | — | — | — | Yes | Yes |
| `minecraft:nether_brick_stairs` | — | — | — | — | Yes | Yes |
| `minecraft:nether_brick_wall` | — | — | — | — | Yes | Yes |
| `minecraft:nether_bricks` | — | — | — | — | Yes | Yes |
| `minecraft:nether_gold_ore` | — | — | — | — | — | Yes |
| `minecraft:nether_portal` | Yes | Yes | Yes | Yes | Yes | Yes |
| `minecraft:nether_quartz_ore` | — | — | — | — | — | Yes |
| `minecraft:nether_sprouts` | — | — | — | — | — | Yes |
| `minecraft:nether_wart` | — | — | — | — | — | Yes |
| `minecraft:nether_wart_block` | — | — | — | — | — | Yes |
| `minecraft:netherite_block` | — | — | — | — | Yes | Yes |
| `minecraft:netherrack` | — | — | — | — | — | Yes |
| `minecraft:note_block` | Yes | Yes | Yes | Yes | Yes | Yes |
| `minecraft:oak_button` | Yes | — | Yes | — | — | — |
| `minecraft:oak_door` | Yes | — | Yes | — | — | — |
| `minecraft:oak_fence` | Yes | — | Yes | — | — | — |
| `minecraft:oak_fence_gate` | Yes | — | Yes | — | — | — |
| `minecraft:oak_hanging_sign` | — | — | Yes | — | — | — |
| `minecraft:oak_leaves` | Yes | — | Yes | — | — | — |
| `minecraft:oak_log` | Yes | — | Yes | — | — | — |
| `minecraft:oak_planks` | Yes | — | Yes | — | — | — |
| `minecraft:oak_pressure_plate` | Yes | — | Yes | — | — | — |
| `minecraft:oak_sapling` | Yes | — | Yes | — | — | — |
| `minecraft:oak_shelf` | Yes | — | Yes | — | — | — |
| `minecraft:oak_sign` | Yes | — | Yes | — | — | — |
| `minecraft:oak_slab` | Yes | — | Yes | — | — | — |
| `minecraft:oak_stairs` | Yes | — | Yes | — | — | — |
| `minecraft:oak_trapdoor` | Yes | — | Yes | — | — | — |
| `minecraft:oak_wall_hanging_sign` | — | — | Yes | — | — | — |
| `minecraft:oak_wall_sign` | Yes | — | Yes | — | — | — |
| `minecraft:oak_wood` | Yes | — | Yes | — | — | — |
| `minecraft:observer` | — | — | — | — | Yes | — |
| `minecraft:obsidian` | — | — | — | — | — | Yes |
| `minecraft:ochre_froglight` | Yes | Yes | Yes | Yes | Yes | Yes |
| `minecraft:open_eyeblossom` | Yes | Yes | Yes | Yes | Yes | Yes |
| `minecraft:orange_banner` | Yes | Yes | Yes | Yes | Yes | Yes |
| `minecraft:orange_bed` | Yes | Yes | Yes | Yes | Yes | Yes |
| `minecraft:orange_candle` | Yes | Yes | Yes | — | — | — |
| `minecraft:orange_candle_cake` | Yes | Yes | Yes | Yes | Yes | Yes |
| `minecraft:orange_carpet` | Yes | Yes | Yes | Yes | Yes | Yes |
| `minecraft:orange_concrete` | — | — | Yes | Yes | Yes | Yes |
| `minecraft:orange_concrete_powder` | — | — | Yes | Yes | Yes | — |
| `minecraft:orange_concrete_slab` | — | — | Yes | Yes | Yes | Yes |
| `minecraft:orange_concrete_stairs` | — | — | Yes | Yes | Yes | Yes |
| `minecraft:orange_glazed_terracotta` | — | — | — | — | Yes | — |
| `minecraft:orange_poplar_leaves` | — | — | — | Yes | — | — |
| `minecraft:orange_shulker_box` | — | — | — | — | Yes | — |
| `minecraft:orange_stained_glass` | — | — | Yes | Yes | Yes | Yes |
| `minecraft:orange_stained_glass_pane` | — | — | Yes | Yes | Yes | Yes |
| `minecraft:orange_terracotta` | — | — | — | — | Yes | — |
| `minecraft:orange_tulip` | Yes | Yes | Yes | — | — | — |
| `minecraft:orange_wall_banner` | Yes | Yes | Yes | Yes | Yes | Yes |
| `minecraft:orange_wool` | Yes | Yes | Yes | Yes | Yes | Yes |
| `minecraft:orange_wool_slab` | Yes | Yes | Yes | Yes | Yes | Yes |
| `minecraft:orange_wool_stairs` | Yes | Yes | Yes | Yes | Yes | Yes |
| `minecraft:oxeye_daisy` | Yes | Yes | Yes | — | — | — |
| `minecraft:oxidized_chiseled_copper` | — | — | Yes | Yes | Yes | Yes |
| `minecraft:oxidized_copper` | — | — | Yes | Yes | Yes | Yes |
| `minecraft:oxidized_copper_bars` | — | — | Yes | Yes | Yes | Yes |
| `minecraft:oxidized_copper_bulb` | — | — | — | — | Yes | Yes |
| `minecraft:oxidized_copper_chain` | — | — | Yes | Yes | Yes | Yes |
| `minecraft:oxidized_copper_chest` | — | — | Yes | Yes | Yes | Yes |
| `minecraft:oxidized_copper_door` | — | — | Yes | Yes | Yes | Yes |
| `minecraft:oxidized_copper_golem_statue` | — | — | Yes | Yes | Yes | Yes |
| `minecraft:oxidized_copper_grate` | — | — | Yes | Yes | Yes | Yes |
| `minecraft:oxidized_copper_lantern` | — | — | Yes | Yes | Yes | Yes |
| `minecraft:oxidized_copper_trapdoor` | — | — | Yes | Yes | Yes | Yes |
| `minecraft:oxidized_cut_copper` | — | — | Yes | Yes | Yes | Yes |
| `minecraft:oxidized_cut_copper_slab` | — | — | Yes | Yes | Yes | Yes |
| `minecraft:oxidized_cut_copper_stairs` | — | — | Yes | Yes | Yes | Yes |
| `minecraft:oxidized_lightning_rod` | — | — | Yes | Yes | Yes | Yes |
| `minecraft:packed_ice` | — | — | — | Yes | — | — |
| `minecraft:packed_mud` | — | — | — | — | — | — |
| `minecraft:pale_hanging_moss` | Yes | Yes | Yes | Yes | Yes | Yes |
| `minecraft:pale_moss_block` | — | — | — | — | — | Yes |
| `minecraft:pale_moss_carpet` | — | — | — | — | — | Yes |
| `minecraft:pale_oak_button` | — | — | — | — | — | Yes |
| `minecraft:pale_oak_door` | — | — | — | — | — | Yes |
| `minecraft:pale_oak_fence` | — | — | — | — | — | Yes |
| `minecraft:pale_oak_fence_gate` | — | — | — | — | — | Yes |
| `minecraft:pale_oak_hanging_sign` | — | — | — | — | — | Yes |
| `minecraft:pale_oak_leaves` | — | — | — | — | — | Yes |
| `minecraft:pale_oak_log` | — | — | — | — | — | Yes |
| `minecraft:pale_oak_planks` | — | — | — | — | — | Yes |
| `minecraft:pale_oak_pressure_plate` | — | — | — | — | — | Yes |
| `minecraft:pale_oak_sapling` | — | — | — | — | — | Yes |
| `minecraft:pale_oak_shelf` | — | — | — | — | — | Yes |
| `minecraft:pale_oak_sign` | — | — | — | — | — | Yes |
| `minecraft:pale_oak_slab` | — | — | — | — | — | Yes |
| `minecraft:pale_oak_stairs` | — | — | — | — | — | Yes |
| `minecraft:pale_oak_trapdoor` | — | — | — | — | — | Yes |
| `minecraft:pale_oak_wall_hanging_sign` | — | — | — | — | — | Yes |
| `minecraft:pale_oak_wall_sign` | — | — | — | — | — | Yes |
| `minecraft:pale_oak_wood` | — | — | — | — | — | Yes |
| `minecraft:pearlescent_froglight` | Yes | Yes | Yes | Yes | Yes | Yes |
| `minecraft:peony` | Yes | — | — | — | — | — |
| `minecraft:petrified_oak_slab` | Yes | — | Yes | — | — | — |
| `minecraft:piglin_head` | Yes | Yes | Yes | Yes | Yes | Yes |
| `minecraft:piglin_wall_head` | Yes | Yes | Yes | Yes | Yes | Yes |
| `minecraft:pink_banner` | Yes | Yes | Yes | Yes | Yes | Yes |
| `minecraft:pink_bed` | Yes | Yes | Yes | Yes | Yes | Yes |
| `minecraft:pink_candle` | Yes | Yes | Yes | — | — | — |
| `minecraft:pink_candle_cake` | Yes | Yes | Yes | Yes | Yes | Yes |
| `minecraft:pink_carpet` | Yes | Yes | Yes | Yes | Yes | Yes |
| `minecraft:pink_concrete` | — | — | Yes | Yes | Yes | Yes |
| `minecraft:pink_concrete_powder` | — | — | Yes | Yes | Yes | — |
| `minecraft:pink_concrete_slab` | — | — | Yes | Yes | Yes | Yes |
| `minecraft:pink_concrete_stairs` | — | — | Yes | Yes | Yes | Yes |
| `minecraft:pink_glazed_terracotta` | — | — | — | — | Yes | — |
| `minecraft:pink_petals` | Yes | — | — | — | — | — |
| `minecraft:pink_shulker_box` | — | — | — | — | Yes | — |
| `minecraft:pink_stained_glass` | — | — | Yes | Yes | Yes | Yes |
| `minecraft:pink_stained_glass_pane` | — | — | Yes | Yes | Yes | Yes |
| `minecraft:pink_terracotta` | — | — | — | — | Yes | — |
| `minecraft:pink_tulip` | Yes | Yes | Yes | — | — | — |
| `minecraft:pink_wall_banner` | Yes | Yes | Yes | Yes | Yes | Yes |
| `minecraft:pink_wool` | Yes | Yes | Yes | Yes | Yes | Yes |
| `minecraft:pink_wool_slab` | Yes | Yes | Yes | Yes | Yes | Yes |
| `minecraft:pink_wool_stairs` | Yes | Yes | Yes | Yes | Yes | Yes |
| `minecraft:piston` | — | — | — | — | Yes | — |
| `minecraft:piston_head` | — | — | Yes | Yes | Yes | Yes |
| `minecraft:pitcher_crop` | Yes | — | — | — | — | — |
| `minecraft:pitcher_plant` | Yes | — | — | — | — | — |
| `minecraft:player_head` | Yes | Yes | Yes | Yes | Yes | Yes |
| `minecraft:player_wall_head` | Yes | Yes | Yes | Yes | Yes | Yes |
| `minecraft:podzol` | — | — | — | Yes | — | — |
| `minecraft:pointed_dripstone` | — | — | — | Yes | — | — |
| `minecraft:polished_andesite` | — | — | Yes | Yes | Yes | — |
| `minecraft:polished_andesite_slab` | — | — | Yes | Yes | Yes | — |
| `minecraft:polished_andesite_stairs` | — | — | Yes | Yes | Yes | — |
| `minecraft:polished_basalt` | — | — | — | Yes | Yes | Yes |
| `minecraft:polished_blackstone` | — | — | — | — | Yes | Yes |
| `minecraft:polished_blackstone_brick_slab` | — | — | — | — | Yes | Yes |
| `minecraft:polished_blackstone_brick_stairs` | — | — | — | — | Yes | Yes |
| `minecraft:polished_blackstone_brick_wall` | — | — | — | — | Yes | Yes |
| `minecraft:polished_blackstone_bricks` | — | — | — | — | Yes | Yes |
| `minecraft:polished_blackstone_button` | — | — | — | — | Yes | Yes |
| `minecraft:polished_blackstone_pressure_plate` | — | — | — | — | Yes | Yes |
| `minecraft:polished_blackstone_slab` | — | — | — | — | Yes | Yes |
| `minecraft:polished_blackstone_stairs` | — | — | — | — | Yes | Yes |
| `minecraft:polished_blackstone_wall` | — | — | — | — | Yes | Yes |
| `minecraft:polished_cinnabar` | — | — | — | — | Yes | — |
| `minecraft:polished_cinnabar_slab` | — | — | — | — | Yes | — |
| `minecraft:polished_cinnabar_stairs` | — | — | — | — | Yes | — |
| `minecraft:polished_cinnabar_wall` | — | — | — | — | Yes | — |
| `minecraft:polished_deepslate` | — | — | — | Yes | Yes | — |
| `minecraft:polished_deepslate_slab` | — | — | — | Yes | Yes | — |
| `minecraft:polished_deepslate_stairs` | — | — | — | Yes | Yes | — |
| `minecraft:polished_deepslate_wall` | — | — | — | Yes | Yes | — |
| `minecraft:polished_diorite` | — | — | Yes | Yes | Yes | — |
| `minecraft:polished_diorite_slab` | — | — | Yes | Yes | Yes | — |
| `minecraft:polished_diorite_stairs` | — | — | Yes | Yes | Yes | — |
| `minecraft:polished_granite` | — | — | Yes | Yes | Yes | — |
| `minecraft:polished_granite_slab` | — | — | Yes | Yes | Yes | — |
| `minecraft:polished_granite_stairs` | — | — | Yes | Yes | Yes | — |
| `minecraft:polished_sulfur` | — | — | — | — | — | Yes |
| `minecraft:polished_sulfur_slab` | — | — | — | — | — | Yes |
| `minecraft:polished_sulfur_stairs` | — | — | — | — | — | Yes |
| `minecraft:polished_sulfur_wall` | — | — | — | — | — | Yes |
| `minecraft:polished_tuff` | — | — | — | Yes | Yes | — |
| `minecraft:polished_tuff_slab` | — | — | — | Yes | Yes | — |
| `minecraft:polished_tuff_stairs` | — | — | — | Yes | Yes | — |
| `minecraft:polished_tuff_wall` | — | — | — | Yes | Yes | — |
| `minecraft:poplar_button` | — | — | — | Yes | — | — |
| `minecraft:poplar_door` | — | — | — | Yes | — | — |
| `minecraft:poplar_fence` | — | — | — | Yes | — | — |
| `minecraft:poplar_fence_gate` | — | — | — | Yes | — | — |
| `minecraft:poplar_hanging_sign` | — | — | — | Yes | — | — |
| `minecraft:poplar_log` | — | — | — | Yes | — | — |
| `minecraft:poplar_planks` | — | — | — | Yes | — | — |
| `minecraft:poplar_pressure_plate` | — | — | — | Yes | — | — |
| `minecraft:poplar_sapling` | — | — | — | Yes | — | — |
| `minecraft:poplar_shelf` | — | — | — | Yes | — | — |
| `minecraft:poplar_sign` | — | — | — | Yes | — | — |
| `minecraft:poplar_slab` | — | — | — | Yes | — | — |
| `minecraft:poplar_stairs` | — | — | — | Yes | — | — |
| `minecraft:poplar_trapdoor` | — | — | — | Yes | — | — |
| `minecraft:poplar_wall_hanging_sign` | — | — | — | Yes | — | — |
| `minecraft:poplar_wall_sign` | — | — | — | Yes | — | — |
| `minecraft:poplar_wood` | — | — | — | Yes | — | — |
| `minecraft:poppy` | Yes | Yes | Yes | — | — | — |
| `minecraft:potatoes` | Yes | Yes | Yes | — | — | — |
| `minecraft:potent_sulfur` | — | — | — | — | — | Yes |
| `minecraft:potted_acacia_sapling` | — | — | — | — | — | — |
| `minecraft:potted_allium` | — | — | — | — | — | — |
| `minecraft:potted_azalea_bush` | — | — | — | — | — | — |
| `minecraft:potted_azure_bluet` | — | — | Yes | — | — | — |
| `minecraft:potted_bamboo` | — | — | — | — | — | — |
| `minecraft:potted_birch_sapling` | — | — | Yes | — | — | — |
| `minecraft:potted_blue_orchid` | — | — | — | — | — | — |
| `minecraft:potted_brown_mushroom` | — | — | Yes | — | — | — |
| `minecraft:potted_cactus` | — | — | — | — | — | — |
| `minecraft:potted_cherry_sapling` | — | — | — | — | — | — |
| `minecraft:potted_closed_eyeblossom` | — | — | Yes | — | — | — |
| `minecraft:potted_cornflower` | — | — | Yes | — | — | — |
| `minecraft:potted_crimson_fungus` | — | — | — | — | — | — |
| `minecraft:potted_crimson_roots` | — | — | — | — | — | — |
| `minecraft:potted_dandelion` | — | — | Yes | — | — | — |
| `minecraft:potted_dark_oak_sapling` | — | — | — | — | — | — |
| `minecraft:potted_dead_bush` | — | — | — | — | — | — |
| `minecraft:potted_fern` | — | — | — | — | — | — |
| `minecraft:potted_flowering_azalea_bush` | — | — | — | — | — | — |
| `minecraft:potted_golden_dandelion` | — | — | Yes | — | — | — |
| `minecraft:potted_jungle_sapling` | — | — | — | — | — | — |
| `minecraft:potted_lily_of_the_valley` | — | — | Yes | — | — | — |
| `minecraft:potted_mangrove_propagule` | — | — | — | — | — | — |
| `minecraft:potted_oak_sapling` | — | — | Yes | — | — | — |
| `minecraft:potted_open_eyeblossom` | — | — | Yes | — | — | — |
| `minecraft:potted_orange_tulip` | — | — | Yes | — | — | — |
| `minecraft:potted_oxeye_daisy` | — | — | Yes | — | — | — |
| `minecraft:potted_pale_oak_sapling` | — | — | — | — | — | — |
| `minecraft:potted_pink_tulip` | — | — | Yes | — | — | — |
| `minecraft:potted_poplar_sapling` | — | — | — | — | — | — |
| `minecraft:potted_poppy` | — | — | Yes | — | — | — |
| `minecraft:potted_red_mushroom` | — | — | Yes | — | — | — |
| `minecraft:potted_red_tulip` | — | — | Yes | — | — | — |
| `minecraft:potted_spruce_sapling` | — | — | — | — | — | — |
| `minecraft:potted_torchflower` | — | — | — | — | — | — |
| `minecraft:potted_warped_fungus` | — | — | — | — | — | — |
| `minecraft:potted_warped_roots` | — | — | — | — | — | — |
| `minecraft:potted_white_tulip` | — | — | Yes | — | — | — |
| `minecraft:potted_wither_rose` | — | — | Yes | — | — | — |
| `minecraft:powder_snow` | — | — | — | Yes | — | — |
| `minecraft:powder_snow_cauldron` | — | — | — | Yes | — | — |
| `minecraft:powered_rail` | — | — | — | — | Yes | Yes |
| `minecraft:prismarine` | — | — | — | Yes | — | — |
| `minecraft:prismarine_brick_slab` | — | — | — | Yes | — | — |
| `minecraft:prismarine_brick_stairs` | — | — | — | Yes | — | — |
| `minecraft:prismarine_bricks` | — | — | — | Yes | — | — |
| `minecraft:prismarine_slab` | — | — | — | Yes | — | — |
| `minecraft:prismarine_stairs` | — | — | — | Yes | — | — |
| `minecraft:prismarine_wall` | — | — | — | Yes | — | — |
| `minecraft:pumpkin` | Yes | Yes | Yes | — | — | — |
| `minecraft:pumpkin_stem` | Yes | Yes | Yes | — | — | — |
| `minecraft:purple_banner` | Yes | Yes | Yes | Yes | — | — |
| `minecraft:purple_bed` | Yes | Yes | Yes | Yes | — | — |
| `minecraft:purple_candle` | Yes | Yes | Yes | — | — | — |
| `minecraft:purple_candle_cake` | Yes | Yes | Yes | Yes | Yes | Yes |
| `minecraft:purple_carpet` | Yes | Yes | Yes | Yes | — | — |
| `minecraft:purple_concrete` | — | — | Yes | Yes | Yes | Yes |
| `minecraft:purple_concrete_powder` | — | — | Yes | Yes | — | — |
| `minecraft:purple_concrete_slab` | — | — | Yes | Yes | Yes | Yes |
| `minecraft:purple_concrete_stairs` | — | — | Yes | Yes | Yes | Yes |
| `minecraft:purple_glazed_terracotta` | — | — | — | — | — | — |
| `minecraft:purple_shulker_box` | — | — | — | — | Yes | — |
| `minecraft:purple_stained_glass` | — | — | Yes | Yes | Yes | Yes |
| `minecraft:purple_stained_glass_pane` | — | — | Yes | Yes | Yes | Yes |
| `minecraft:purple_terracotta` | — | — | — | — | — | — |
| `minecraft:purple_wall_banner` | Yes | Yes | Yes | Yes | — | — |
| `minecraft:purple_wool` | Yes | Yes | Yes | Yes | — | — |
| `minecraft:purple_wool_slab` | Yes | Yes | Yes | Yes | — | — |
| `minecraft:purple_wool_stairs` | Yes | Yes | Yes | Yes | — | — |
| `minecraft:purpur_block` | — | — | — | — | Yes | — |
| `minecraft:purpur_pillar` | — | — | — | — | Yes | — |
| `minecraft:purpur_slab` | — | — | — | — | Yes | — |
| `minecraft:purpur_stairs` | — | — | — | — | Yes | — |
| `minecraft:quartz_block` | — | — | — | — | Yes | Yes |
| `minecraft:quartz_bricks` | — | — | — | — | Yes | Yes |
| `minecraft:quartz_pillar` | — | — | — | — | Yes | Yes |
| `minecraft:quartz_slab` | — | — | — | — | Yes | Yes |
| `minecraft:quartz_stairs` | — | — | — | — | Yes | Yes |
| `minecraft:rail` | — | — | Yes | Yes | Yes | Yes |
| `minecraft:raw_copper_block` | — | — | Yes | Yes | Yes | Yes |
| `minecraft:raw_gold_block` | — | — | Yes | Yes | Yes | Yes |
| `minecraft:raw_iron_block` | — | — | Yes | Yes | — | — |
| `minecraft:red_banner` | Yes | Yes | Yes | Yes | — | — |
| `minecraft:red_bed` | Yes | Yes | Yes | Yes | — | — |
| `minecraft:red_candle` | Yes | Yes | Yes | — | — | — |
| `minecraft:red_candle_cake` | Yes | Yes | Yes | Yes | Yes | Yes |
| `minecraft:red_carpet` | Yes | Yes | Yes | Yes | — | — |
| `minecraft:red_concrete` | — | — | Yes | Yes | Yes | Yes |
| `minecraft:red_concrete_powder` | — | — | Yes | Yes | — | — |
| `minecraft:red_concrete_slab` | — | — | Yes | Yes | Yes | Yes |
| `minecraft:red_concrete_stairs` | — | — | Yes | Yes | Yes | Yes |
| `minecraft:red_glazed_terracotta` | — | — | — | — | — | — |
| `minecraft:red_mushroom` | Yes | Yes | Yes | Yes | Yes | — |
| `minecraft:red_mushroom_block` | Yes | — | — | Yes | — | — |
| `minecraft:red_nether_brick_slab` | — | — | Yes | Yes | Yes | Yes |
| `minecraft:red_nether_brick_stairs` | — | — | Yes | Yes | Yes | Yes |
| `minecraft:red_nether_brick_wall` | — | — | Yes | Yes | Yes | Yes |
| `minecraft:red_nether_bricks` | — | — | Yes | Yes | Yes | Yes |
| `minecraft:red_poplar_leaves` | — | — | — | Yes | — | — |
| `minecraft:red_sand` | — | — | — | — | Yes | Yes |
| `minecraft:red_sandstone` | — | — | — | — | Yes | Yes |
| `minecraft:red_sandstone_slab` | — | — | — | — | Yes | Yes |
| `minecraft:red_sandstone_stairs` | — | — | — | — | Yes | Yes |
| `minecraft:red_sandstone_wall` | — | — | — | — | Yes | Yes |
| `minecraft:red_shrub` | — | — | — | — | — | Yes |
| `minecraft:red_shulker_box` | — | — | — | — | Yes | — |
| `minecraft:red_stained_glass` | — | — | Yes | Yes | Yes | Yes |
| `minecraft:red_stained_glass_pane` | — | — | Yes | Yes | Yes | Yes |
| `minecraft:red_terracotta` | — | — | — | — | — | — |
| `minecraft:red_tulip` | Yes | Yes | Yes | — | — | — |
| `minecraft:red_wall_banner` | Yes | Yes | Yes | Yes | — | — |
| `minecraft:red_wool` | Yes | Yes | Yes | Yes | — | — |
| `minecraft:red_wool_slab` | Yes | Yes | Yes | Yes | — | — |
| `minecraft:red_wool_stairs` | Yes | Yes | Yes | Yes | — | — |
| `minecraft:redstone_block` | — | — | — | — | Yes | Yes |
| `minecraft:redstone_lamp` | — | — | — | — | Yes | Yes |
| `minecraft:redstone_ore` | — | — | — | — | Yes | — |
| `minecraft:redstone_torch` | — | — | — | — | Yes | Yes |
| `minecraft:redstone_wall_torch` | — | — | — | — | Yes | Yes |
| `minecraft:redstone_wire` | — | — | — | — | Yes | Yes |
| `minecraft:reinforced_deepslate` | — | — | — | Yes | Yes | — |
| `minecraft:repeater` | — | — | — | — | Yes | — |
| `minecraft:repeating_command_block` | Yes | Yes | Yes | Yes | Yes | Yes |
| `minecraft:resin_block` | Yes | Yes | Yes | Yes | Yes | Yes |
| `minecraft:resin_brick_slab` | — | — | Yes | Yes | Yes | Yes |
| `minecraft:resin_brick_stairs` | — | — | Yes | Yes | Yes | Yes |
| `minecraft:resin_brick_wall` | — | — | Yes | Yes | Yes | Yes |
| `minecraft:resin_bricks` | — | — | Yes | Yes | Yes | Yes |
| `minecraft:resin_clump` | Yes | Yes | Yes | Yes | Yes | Yes |
| `minecraft:respawn_anchor` | — | — | — | — | Yes | Yes |
| `minecraft:rooted_dirt` | — | Yes | — | — | — | — |
| `minecraft:rose_bush` | Yes | — | — | — | — | — |
| `minecraft:sand` | — | — | Yes | Yes | Yes | — |
| `minecraft:sandstone` | — | — | Yes | Yes | Yes | — |
| `minecraft:sandstone_slab` | — | — | Yes | Yes | Yes | — |
| `minecraft:sandstone_stairs` | — | — | Yes | Yes | Yes | — |
| `minecraft:sandstone_wall` | — | — | Yes | Yes | Yes | — |
| `minecraft:scaffolding` | Yes | — | — | — | — | — |
| `minecraft:sculk` | — | — | — | — | Yes | — |
| `minecraft:sculk_catalyst` | — | — | — | — | Yes | — |
| `minecraft:sculk_sensor` | — | — | — | — | Yes | — |
| `minecraft:sculk_shrieker` | — | — | — | — | Yes | — |
| `minecraft:sculk_vein` | — | — | — | — | Yes | — |
| `minecraft:sea_lantern` | — | — | — | Yes | — | — |
| `minecraft:sea_pickle` | Yes | Yes | — | Yes | — | — |
| `minecraft:seagrass` | Yes | Yes | Yes | Yes | Yes | — |
| `minecraft:shelf_mushroom` | Yes | Yes | Yes | Yes | Yes | Yes |
| `minecraft:short_dry_grass` | Yes | Yes | Yes | Yes | Yes | Yes |
| `minecraft:shroomlight` | — | — | — | — | — | Yes |
| `minecraft:shulker_box` | — | — | — | — | Yes | — |
| `minecraft:skeleton_skull` | Yes | Yes | Yes | Yes | Yes | Yes |
| `minecraft:skeleton_wall_skull` | Yes | Yes | Yes | Yes | Yes | Yes |
| `minecraft:slime_block` | Yes | Yes | Yes | Yes | Yes | Yes |
| `minecraft:small_amethyst_bud` | — | — | — | Yes | — | — |
| `minecraft:small_dripleaf` | Yes | — | — | — | — | — |
| `minecraft:smithing_table` | — | — | Yes | Yes | — | — |
| `minecraft:smoker` | — | — | Yes | Yes | Yes | Yes |
| `minecraft:smooth_basalt` | — | — | — | — | — | Yes |
| `minecraft:smooth_quartz` | — | — | — | — | Yes | Yes |
| `minecraft:smooth_quartz_slab` | — | — | — | — | Yes | Yes |
| `minecraft:smooth_quartz_stairs` | — | — | — | — | Yes | Yes |
| `minecraft:smooth_red_sandstone` | — | — | — | — | Yes | Yes |
| `minecraft:smooth_red_sandstone_slab` | — | — | — | — | Yes | Yes |
| `minecraft:smooth_red_sandstone_stairs` | — | — | — | — | Yes | Yes |
| `minecraft:smooth_sandstone` | — | — | Yes | Yes | Yes | — |
| `minecraft:smooth_sandstone_slab` | — | — | Yes | Yes | Yes | — |
| `minecraft:smooth_sandstone_stairs` | — | — | Yes | Yes | Yes | — |
| `minecraft:smooth_stone` | — | — | Yes | Yes | Yes | — |
| `minecraft:smooth_stone_slab` | — | — | Yes | Yes | Yes | — |
| `minecraft:sniffer_egg` | Yes | Yes | Yes | Yes | Yes | Yes |
| `minecraft:snow` | — | — | — | Yes | — | — |
| `minecraft:snow_block` | — | — | — | Yes | — | — |
| `minecraft:soul_campfire` | — | — | — | — | — | Yes |
| `minecraft:soul_fire` | — | Yes | Yes | Yes | Yes | Yes |
| `minecraft:soul_lantern` | — | — | — | — | — | — |
| `minecraft:soul_sand` | — | — | — | — | — | Yes |
| `minecraft:soul_soil` | — | — | — | — | — | Yes |
| `minecraft:soul_torch` | — | — | — | — | — | Yes |
| `minecraft:soul_wall_torch` | — | — | — | — | — | Yes |
| `minecraft:spawner` | — | — | Yes | Yes | Yes | Yes |
| `minecraft:sponge` | Yes | Yes | Yes | Yes | Yes | Yes |
| `minecraft:spore_blossom` | Yes | — | — | — | — | — |
| `minecraft:spruce_button` | — | — | — | Yes | — | — |
| `minecraft:spruce_door` | — | — | — | Yes | — | — |
| `minecraft:spruce_fence` | — | — | — | Yes | — | — |
| `minecraft:spruce_fence_gate` | — | — | — | Yes | — | — |
| `minecraft:spruce_hanging_sign` | — | — | — | Yes | — | — |
| `minecraft:spruce_leaves` | — | — | — | Yes | — | — |
| `minecraft:spruce_log` | — | — | — | Yes | — | — |
| `minecraft:spruce_planks` | — | — | — | Yes | — | — |
| `minecraft:spruce_pressure_plate` | — | — | — | Yes | — | — |
| `minecraft:spruce_sapling` | — | — | — | Yes | — | — |
| `minecraft:spruce_shelf` | — | — | — | Yes | — | — |
| `minecraft:spruce_sign` | — | — | — | Yes | — | — |
| `minecraft:spruce_slab` | — | — | — | Yes | — | — |
| `minecraft:spruce_stairs` | — | — | — | Yes | — | — |
| `minecraft:spruce_trapdoor` | — | — | — | Yes | — | — |
| `minecraft:spruce_wall_hanging_sign` | — | — | — | Yes | — | — |
| `minecraft:spruce_wall_sign` | — | — | — | Yes | — | — |
| `minecraft:spruce_wood` | — | — | — | Yes | — | — |
| `minecraft:sticky_piston` | — | — | — | — | Yes | — |
| `minecraft:stone` | — | — | Yes | Yes | Yes | — |
| `minecraft:stone_brick_slab` | — | — | Yes | Yes | Yes | — |
| `minecraft:stone_brick_stairs` | — | — | Yes | Yes | Yes | — |
| `minecraft:stone_brick_wall` | — | — | Yes | Yes | Yes | — |
| `minecraft:stone_bricks` | — | — | Yes | Yes | Yes | — |
| `minecraft:stone_button` | — | — | Yes | Yes | Yes | — |
| `minecraft:stone_pressure_plate` | — | — | Yes | Yes | Yes | — |
| `minecraft:stone_slab` | — | — | Yes | Yes | Yes | — |
| `minecraft:stone_stairs` | — | — | Yes | Yes | Yes | — |
| `minecraft:stonecutter` | — | — | Yes | Yes | — | — |
| `minecraft:straw_bed` | Yes | Yes | Yes | — | — | — |
| `minecraft:stripped_acacia_log` | — | Yes | — | — | — | — |
| `minecraft:stripped_acacia_wood` | — | Yes | — | — | — | — |
| `minecraft:stripped_bamboo_block` | Yes | — | — | — | — | — |
| `minecraft:stripped_birch_log` | — | — | Yes | — | — | — |
| `minecraft:stripped_birch_wood` | — | — | Yes | — | — | — |
| `minecraft:stripped_cherry_log` | Yes | — | — | — | — | — |
| `minecraft:stripped_cherry_wood` | Yes | — | — | — | — | — |
| `minecraft:stripped_crimson_hyphae` | — | — | — | — | — | Yes |
| `minecraft:stripped_crimson_stem` | — | — | — | — | — | Yes |
| `minecraft:stripped_dark_oak_log` | Yes | — | — | — | — | — |
| `minecraft:stripped_dark_oak_wood` | Yes | — | — | — | — | — |
| `minecraft:stripped_jungle_log` | Yes | — | — | — | — | — |
| `minecraft:stripped_jungle_wood` | Yes | — | — | — | — | — |
| `minecraft:stripped_mangrove_log` | Yes | — | — | — | — | — |
| `minecraft:stripped_mangrove_wood` | Yes | — | — | — | — | — |
| `minecraft:stripped_oak_log` | Yes | — | Yes | — | — | — |
| `minecraft:stripped_oak_wood` | Yes | — | Yes | — | — | — |
| `minecraft:stripped_pale_oak_log` | — | — | — | — | — | Yes |
| `minecraft:stripped_pale_oak_wood` | — | — | — | — | — | Yes |
| `minecraft:stripped_poplar_log` | — | — | — | Yes | — | — |
| `minecraft:stripped_poplar_wood` | — | — | — | Yes | — | — |
| `minecraft:stripped_spruce_log` | — | — | — | Yes | — | — |
| `minecraft:stripped_spruce_wood` | — | — | — | Yes | — | — |
| `minecraft:stripped_warped_hyphae` | — | — | — | — | — | Yes |
| `minecraft:stripped_warped_stem` | — | — | — | — | — | Yes |
| `minecraft:structure_block` | Yes | Yes | Yes | Yes | Yes | Yes |
| `minecraft:structure_void` | Yes | Yes | Yes | Yes | Yes | Yes |
| `minecraft:sugar_cane` | Yes | Yes | Yes | Yes | Yes | — |
| `minecraft:sulfur` | — | — | — | — | — | Yes |
| `minecraft:sulfur_brick_slab` | — | — | — | — | — | Yes |
| `minecraft:sulfur_brick_stairs` | — | — | — | — | — | Yes |
| `minecraft:sulfur_brick_wall` | — | — | — | — | — | Yes |
| `minecraft:sulfur_bricks` | — | — | — | — | — | Yes |
| `minecraft:sulfur_slab` | — | — | — | — | — | Yes |
| `minecraft:sulfur_spike` | — | — | — | — | — | Yes |
| `minecraft:sulfur_stairs` | — | — | — | — | — | Yes |
| `minecraft:sulfur_wall` | — | — | — | — | — | Yes |
| `minecraft:sunflower` | Yes | Yes | Yes | — | — | — |
| `minecraft:suspicious_gravel` | — | — | Yes | Yes | Yes | — |
| `minecraft:suspicious_sand` | — | — | Yes | Yes | Yes | — |
| `minecraft:sweet_berry_bush` | Yes | — | — | Yes | — | — |
| `minecraft:tall_dry_grass` | Yes | Yes | Yes | Yes | Yes | Yes |
| `minecraft:tall_grass` | Yes | Yes | Yes | — | — | — |
| `minecraft:tall_seagrass` | Yes | Yes | Yes | Yes | Yes | — |
| `minecraft:target` | — | — | — | — | — | — |
| `minecraft:terracotta` | — | — | — | — | Yes | — |
| `minecraft:tinted_glass` | — | — | — | Yes | — | — |
| `minecraft:tnt` | — | — | Yes | Yes | Yes | Yes |
| `minecraft:torch` | Yes | Yes | Yes | Yes | Yes | Yes |
| `minecraft:torchflower` | Yes | — | — | — | — | — |
| `minecraft:torchflower_crop` | Yes | — | — | — | — | — |
| `minecraft:trapped_chest` | — | — | Yes | Yes | Yes | Yes |
| `minecraft:tripwire` | Yes | Yes | Yes | Yes | Yes | Yes |
| `minecraft:tripwire_hook` | — | — | Yes | Yes | Yes | Yes |
| `minecraft:tube_coral` | Yes | — | — | — | — | — |
| `minecraft:tube_coral_block` | Yes | — | — | — | — | — |
| `minecraft:tube_coral_fan` | Yes | — | — | — | — | — |
| `minecraft:tube_coral_wall_fan` | Yes | — | — | — | — | — |
| `minecraft:tuff` | — | — | — | Yes | Yes | — |
| `minecraft:tuff_brick_slab` | — | — | — | Yes | Yes | — |
| `minecraft:tuff_brick_stairs` | — | — | — | Yes | Yes | — |
| `minecraft:tuff_brick_wall` | — | — | — | Yes | Yes | — |
| `minecraft:tuff_bricks` | — | — | — | Yes | Yes | — |
| `minecraft:tuff_slab` | — | — | — | Yes | Yes | — |
| `minecraft:tuff_stairs` | — | — | — | Yes | Yes | — |
| `minecraft:tuff_wall` | — | — | — | Yes | Yes | — |
| `minecraft:turtle_egg` | Yes | Yes | Yes | Yes | Yes | Yes |
| `minecraft:twisting_vines` | — | — | — | — | — | Yes |
| `minecraft:twisting_vines_plant` | — | — | — | — | — | Yes |
| `minecraft:verdant_froglight` | Yes | Yes | Yes | Yes | Yes | Yes |
| `minecraft:vine` | Yes | — | — | — | — | — |
| `minecraft:void_air` | Yes | Yes | Yes | Yes | Yes | Yes |
| `minecraft:wall_torch` | Yes | Yes | Yes | Yes | Yes | Yes |
| `minecraft:warped_button` | — | — | — | — | — | Yes |
| `minecraft:warped_door` | — | — | — | — | — | Yes |
| `minecraft:warped_fence` | — | — | — | — | — | Yes |
| `minecraft:warped_fence_gate` | — | — | — | — | — | Yes |
| `minecraft:warped_fungus` | — | — | — | — | — | Yes |
| `minecraft:warped_hanging_sign` | — | — | — | — | — | — |
| `minecraft:warped_hyphae` | — | — | — | — | — | Yes |
| `minecraft:warped_nylium` | — | — | — | — | — | Yes |
| `minecraft:warped_planks` | — | — | — | — | — | Yes |
| `minecraft:warped_pressure_plate` | — | — | — | — | — | Yes |
| `minecraft:warped_roots` | — | — | — | — | — | Yes |
| `minecraft:warped_shelf` | — | — | — | — | — | Yes |
| `minecraft:warped_sign` | — | — | — | — | — | Yes |
| `minecraft:warped_slab` | — | — | — | — | — | Yes |
| `minecraft:warped_stairs` | — | — | — | — | — | Yes |
| `minecraft:warped_stem` | — | — | — | — | — | Yes |
| `minecraft:warped_trapdoor` | — | — | — | — | — | Yes |
| `minecraft:warped_wall_hanging_sign` | — | — | — | — | — | — |
| `minecraft:warped_wall_sign` | — | — | — | — | — | Yes |
| `minecraft:warped_wart_block` | — | — | — | — | — | Yes |
| `minecraft:water` | Yes | Yes | Yes | Yes | Yes | Yes |
| `minecraft:water_cauldron` | — | — | Yes | Yes | — | — |
| `minecraft:waxed_chiseled_copper` | — | — | Yes | — | — | — |
| `minecraft:waxed_copper_bars` | — | — | Yes | Yes | Yes | Yes |
| `minecraft:waxed_copper_block` | — | — | Yes | — | — | — |
| `minecraft:waxed_copper_bulb` | — | — | — | — | — | — |
| `minecraft:waxed_copper_chain` | — | — | Yes | Yes | Yes | Yes |
| `minecraft:waxed_copper_chest` | — | — | Yes | — | — | — |
| `minecraft:waxed_copper_door` | — | — | Yes | — | — | — |
| `minecraft:waxed_copper_golem_statue` | — | — | Yes | — | — | — |
| `minecraft:waxed_copper_grate` | — | — | Yes | — | — | — |
| `minecraft:waxed_copper_lantern` | — | — | Yes | Yes | Yes | Yes |
| `minecraft:waxed_copper_trapdoor` | — | — | Yes | — | — | — |
| `minecraft:waxed_cut_copper` | — | — | Yes | — | — | — |
| `minecraft:waxed_cut_copper_slab` | — | — | Yes | — | — | — |
| `minecraft:waxed_cut_copper_stairs` | — | — | Yes | — | — | — |
| `minecraft:waxed_exposed_chiseled_copper` | — | — | Yes | — | — | — |
| `minecraft:waxed_exposed_copper` | — | — | Yes | — | — | — |
| `minecraft:waxed_exposed_copper_bars` | — | — | Yes | Yes | Yes | Yes |
| `minecraft:waxed_exposed_copper_bulb` | — | — | — | — | — | — |
| `minecraft:waxed_exposed_copper_chain` | — | — | Yes | Yes | Yes | Yes |
| `minecraft:waxed_exposed_copper_chest` | — | — | Yes | — | — | — |
| `minecraft:waxed_exposed_copper_door` | — | — | Yes | — | — | — |
| `minecraft:waxed_exposed_copper_golem_statue` | — | — | Yes | — | — | — |
| `minecraft:waxed_exposed_copper_grate` | — | — | Yes | — | — | — |
| `minecraft:waxed_exposed_copper_lantern` | — | — | Yes | Yes | Yes | Yes |
| `minecraft:waxed_exposed_copper_trapdoor` | — | — | Yes | — | — | — |
| `minecraft:waxed_exposed_cut_copper` | — | — | Yes | — | — | — |
| `minecraft:waxed_exposed_cut_copper_slab` | — | — | Yes | — | — | — |
| `minecraft:waxed_exposed_cut_copper_stairs` | — | — | Yes | — | — | — |
| `minecraft:waxed_exposed_lightning_rod` | — | — | Yes | — | — | — |
| `minecraft:waxed_lightning_rod` | — | — | Yes | — | — | — |
| `minecraft:waxed_oxidized_chiseled_copper` | — | — | Yes | — | — | — |
| `minecraft:waxed_oxidized_copper` | — | — | Yes | — | — | — |
| `minecraft:waxed_oxidized_copper_bars` | — | — | Yes | Yes | Yes | Yes |
| `minecraft:waxed_oxidized_copper_bulb` | — | — | — | — | — | — |
| `minecraft:waxed_oxidized_copper_chain` | — | — | Yes | Yes | Yes | Yes |
| `minecraft:waxed_oxidized_copper_chest` | — | — | Yes | — | — | — |
| `minecraft:waxed_oxidized_copper_door` | — | — | Yes | — | — | — |
| `minecraft:waxed_oxidized_copper_golem_statue` | — | — | Yes | — | — | — |
| `minecraft:waxed_oxidized_copper_grate` | — | — | Yes | — | — | — |
| `minecraft:waxed_oxidized_copper_lantern` | — | — | Yes | Yes | Yes | Yes |
| `minecraft:waxed_oxidized_copper_trapdoor` | — | — | Yes | — | — | — |
| `minecraft:waxed_oxidized_cut_copper` | — | — | Yes | — | — | — |
| `minecraft:waxed_oxidized_cut_copper_slab` | — | — | Yes | — | — | — |
| `minecraft:waxed_oxidized_cut_copper_stairs` | — | — | Yes | — | — | — |
| `minecraft:waxed_oxidized_lightning_rod` | — | — | Yes | — | — | — |
| `minecraft:waxed_weathered_chiseled_copper` | — | — | Yes | — | — | — |
| `minecraft:waxed_weathered_copper` | — | — | Yes | — | — | — |
| `minecraft:waxed_weathered_copper_bars` | — | — | Yes | Yes | Yes | Yes |
| `minecraft:waxed_weathered_copper_bulb` | — | — | — | — | — | — |
| `minecraft:waxed_weathered_copper_chain` | — | — | Yes | Yes | Yes | Yes |
| `minecraft:waxed_weathered_copper_chest` | — | — | Yes | — | — | — |
| `minecraft:waxed_weathered_copper_door` | — | — | Yes | — | — | — |
| `minecraft:waxed_weathered_copper_golem_statue` | — | — | Yes | — | — | — |
| `minecraft:waxed_weathered_copper_grate` | — | — | Yes | — | — | — |
| `minecraft:waxed_weathered_copper_lantern` | — | — | Yes | Yes | Yes | Yes |
| `minecraft:waxed_weathered_copper_trapdoor` | — | — | Yes | — | — | — |
| `minecraft:waxed_weathered_cut_copper` | — | — | Yes | — | — | — |
| `minecraft:waxed_weathered_cut_copper_slab` | — | — | Yes | — | — | — |
| `minecraft:waxed_weathered_cut_copper_stairs` | — | — | Yes | — | — | — |
| `minecraft:waxed_weathered_lightning_rod` | — | — | Yes | — | — | — |
| `minecraft:weathered_chiseled_copper` | — | — | Yes | Yes | Yes | Yes |
| `minecraft:weathered_copper` | — | — | Yes | Yes | Yes | Yes |
| `minecraft:weathered_copper_bars` | — | — | Yes | Yes | Yes | Yes |
| `minecraft:weathered_copper_bulb` | — | — | — | — | Yes | Yes |
| `minecraft:weathered_copper_chain` | — | — | Yes | Yes | Yes | Yes |
| `minecraft:weathered_copper_chest` | — | — | Yes | Yes | Yes | Yes |
| `minecraft:weathered_copper_door` | — | — | Yes | Yes | Yes | Yes |
| `minecraft:weathered_copper_golem_statue` | — | — | Yes | Yes | Yes | Yes |
| `minecraft:weathered_copper_grate` | — | — | Yes | Yes | Yes | Yes |
| `minecraft:weathered_copper_lantern` | — | — | Yes | Yes | Yes | Yes |
| `minecraft:weathered_copper_trapdoor` | — | — | Yes | Yes | Yes | Yes |
| `minecraft:weathered_cut_copper` | — | — | Yes | Yes | Yes | Yes |
| `minecraft:weathered_cut_copper_slab` | — | — | Yes | Yes | Yes | Yes |
| `minecraft:weathered_cut_copper_stairs` | — | — | Yes | Yes | Yes | Yes |
| `minecraft:weathered_lightning_rod` | — | — | Yes | Yes | Yes | Yes |
| `minecraft:weeping_vines` | — | — | — | — | — | Yes |
| `minecraft:weeping_vines_plant` | — | — | — | — | — | Yes |
| `minecraft:wet_sponge` | Yes | Yes | — | Yes | — | — |
| `minecraft:wheat` | Yes | Yes | Yes | — | — | — |
| `minecraft:white_banner` | Yes | Yes | Yes | Yes | Yes | Yes |
| `minecraft:white_bed` | Yes | Yes | Yes | Yes | Yes | Yes |
| `minecraft:white_candle` | Yes | Yes | Yes | — | — | — |
| `minecraft:white_candle_cake` | Yes | Yes | Yes | Yes | Yes | Yes |
| `minecraft:white_carpet` | Yes | Yes | Yes | Yes | Yes | Yes |
| `minecraft:white_concrete` | — | — | Yes | Yes | Yes | Yes |
| `minecraft:white_concrete_powder` | — | — | Yes | Yes | Yes | — |
| `minecraft:white_concrete_slab` | — | — | Yes | Yes | Yes | Yes |
| `minecraft:white_concrete_stairs` | — | — | Yes | Yes | Yes | Yes |
| `minecraft:white_glazed_terracotta` | — | — | — | — | Yes | — |
| `minecraft:white_shulker_box` | — | — | — | — | Yes | — |
| `minecraft:white_stained_glass` | — | — | Yes | Yes | Yes | Yes |
| `minecraft:white_stained_glass_pane` | — | — | Yes | Yes | Yes | Yes |
| `minecraft:white_terracotta` | — | — | — | — | Yes | — |
| `minecraft:white_tulip` | Yes | Yes | Yes | — | — | — |
| `minecraft:white_wall_banner` | Yes | Yes | Yes | Yes | Yes | Yes |
| `minecraft:white_wool` | Yes | Yes | Yes | Yes | Yes | Yes |
| `minecraft:white_wool_slab` | Yes | Yes | Yes | Yes | Yes | Yes |
| `minecraft:white_wool_stairs` | Yes | Yes | Yes | Yes | Yes | Yes |
| `minecraft:wildflowers` | Yes | Yes | Yes | Yes | Yes | Yes |
| `minecraft:wither_rose` | Yes | Yes | Yes | Yes | Yes | Yes |
| `minecraft:wither_skeleton_skull` | Yes | Yes | Yes | Yes | Yes | Yes |
| `minecraft:wither_skeleton_wall_skull` | Yes | Yes | Yes | Yes | Yes | Yes |
| `minecraft:yellow_banner` | Yes | Yes | Yes | Yes | Yes | Yes |
| `minecraft:yellow_bed` | Yes | Yes | Yes | Yes | Yes | Yes |
| `minecraft:yellow_candle` | Yes | Yes | Yes | — | — | — |
| `minecraft:yellow_candle_cake` | Yes | Yes | Yes | Yes | Yes | Yes |
| `minecraft:yellow_carpet` | Yes | Yes | Yes | Yes | Yes | Yes |
| `minecraft:yellow_concrete` | — | — | Yes | Yes | Yes | Yes |
| `minecraft:yellow_concrete_powder` | — | — | Yes | Yes | Yes | — |
| `minecraft:yellow_concrete_slab` | — | — | Yes | Yes | Yes | Yes |
| `minecraft:yellow_concrete_stairs` | — | — | Yes | Yes | Yes | Yes |
| `minecraft:yellow_glazed_terracotta` | — | — | — | — | Yes | — |
| `minecraft:yellow_poplar_leaves` | — | — | — | Yes | — | — |
| `minecraft:yellow_shulker_box` | — | — | — | — | Yes | — |
| `minecraft:yellow_stained_glass` | — | — | Yes | Yes | Yes | Yes |
| `minecraft:yellow_stained_glass_pane` | — | — | Yes | Yes | Yes | Yes |
| `minecraft:yellow_terracotta` | — | — | — | — | Yes | — |
| `minecraft:yellow_wall_banner` | Yes | Yes | Yes | Yes | Yes | Yes |
| `minecraft:yellow_wool` | Yes | Yes | Yes | Yes | Yes | Yes |
| `minecraft:yellow_wool_slab` | Yes | Yes | Yes | Yes | Yes | Yes |
| `minecraft:yellow_wool_stairs` | Yes | Yes | Yes | Yes | Yes | Yes |
| `minecraft:zombie_head` | Yes | Yes | Yes | Yes | Yes | Yes |
| `minecraft:zombie_wall_head` | Yes | Yes | Yes | Yes | Yes | Yes |
| `naturalist:alligator_egg` | Yes | Yes | Yes | Yes | Yes | Yes |
| `naturalist:azure_froglass` | Yes | Yes | Yes | Yes | Yes | Yes |
| `naturalist:azure_froglass_pane` | — | Yes | Yes | Yes | Yes | Yes |
| `naturalist:cattail` | Yes | Yes | Yes | Yes | Yes | Yes |
| `naturalist:chrysalis` | Yes | Yes | Yes | Yes | Yes | Yes |
| `naturalist:crimson_froglass` | — | — | — | — | — | Yes |
| `naturalist:crimson_froglass_pane` | — | — | — | — | — | Yes |
| `naturalist:cut_shellstone` | — | — | Yes | Yes | Yes | Yes |
| `naturalist:cut_shellstone_slab` | — | — | Yes | Yes | Yes | Yes |
| `naturalist:cut_shellstone_stairs` | — | — | Yes | Yes | Yes | Yes |
| `naturalist:cut_shellstone_wall` | — | — | Yes | Yes | Yes | Yes |
| `naturalist:duckweed` | Yes | Yes | Yes | Yes | Yes | Yes |
| `naturalist:glow_goop` | Yes | Yes | Yes | Yes | Yes | Yes |
| `naturalist:shellstone` | — | — | Yes | Yes | Yes | Yes |
| `naturalist:shellstone_brick_slab` | — | — | Yes | Yes | Yes | Yes |
| `naturalist:shellstone_brick_stairs` | — | — | Yes | Yes | Yes | Yes |
| `naturalist:shellstone_brick_wall` | — | — | Yes | Yes | Yes | Yes |
| `naturalist:shellstone_bricks` | — | — | Yes | Yes | Yes | Yes |
| `naturalist:shellstone_slab` | — | — | Yes | Yes | Yes | Yes |
| `naturalist:shellstone_stairs` | — | — | Yes | Yes | Yes | Yes |
| `naturalist:shellstone_wall` | — | — | Yes | Yes | Yes | Yes |
| `naturalist:smooth_shellstone` | — | — | Yes | Yes | Yes | Yes |
| `naturalist:smooth_shellstone_slab` | — | — | Yes | Yes | Yes | Yes |
| `naturalist:smooth_shellstone_stairs` | — | — | Yes | Yes | Yes | Yes |
| `naturalist:smooth_shellstone_wall` | — | — | Yes | Yes | Yes | Yes |
| `naturalist:snail_eggs` | Yes | Yes | Yes | Yes | Yes | Yes |
| `naturalist:teddy_bear` | Yes | Yes | Yes | Yes | Yes | Yes |
| `naturalist:tortoise_egg` | Yes | Yes | Yes | Yes | Yes | Yes |
| `naturalist:verdant_froglass` | Yes | Yes | Yes | Yes | Yes | Yes |
| `naturalist:verdant_froglass_pane` | — | Yes | Yes | Yes | Yes | Yes |
| `netherdepthsupgrade:crimson_kelp` | — | — | — | — | — | Yes |
| `netherdepthsupgrade:crimson_kelp_block` | — | — | — | — | — | Yes |
| `netherdepthsupgrade:crimson_kelp_carpet_block` | — | — | — | — | — | Yes |
| `netherdepthsupgrade:crimson_kelp_plant` | — | — | — | — | — | Yes |
| `netherdepthsupgrade:crimson_seagrass` | — | — | — | — | — | Yes |
| `netherdepthsupgrade:lava_glass` | Yes | Yes | Yes | Yes | Yes | Yes |
| `netherdepthsupgrade:lava_sponge` | Yes | Yes | Yes | Yes | Yes | Yes |
| `netherdepthsupgrade:nether_rice_roll_medley_block` | Yes | Yes | Yes | Yes | Yes | Yes |
| `netherdepthsupgrade:tall_crimson_seagrass` | — | — | — | — | — | Yes |
| `netherdepthsupgrade:tall_warped_seagrass` | — | — | — | — | — | Yes |
| `netherdepthsupgrade:warped_kelp` | — | — | — | — | — | Yes |
| `netherdepthsupgrade:warped_kelp_block` | — | — | — | — | — | Yes |
| `netherdepthsupgrade:warped_kelp_carpet_block` | — | — | — | — | — | Yes |
| `netherdepthsupgrade:warped_kelp_plant` | — | — | — | — | — | Yes |
| `netherdepthsupgrade:warped_seagrass` | — | — | — | — | — | Yes |
| `netherdepthsupgrade:wet_lava_sponge` | Yes | Yes | Yes | Yes | Yes | Yes |
| `nourished_nether:ash` | — | Yes | Yes | Yes | Yes | Yes |
| `nourished_nether:ashy_basalt` | — | — | Yes | Yes | Yes | Yes |
| `nourished_nether:basalt_ash_spawner` | Yes | Yes | Yes | Yes | Yes | Yes |
| `nourished_nether:basalt_brick_slab` | — | — | Yes | Yes | Yes | Yes |
| `nourished_nether:basalt_brick_stairs` | — | — | Yes | Yes | Yes | Yes |
| `nourished_nether:basalt_brick_wall` | — | — | Yes | Yes | Yes | Yes |
| `nourished_nether:basalt_bricks` | — | — | Yes | Yes | Yes | Yes |
| `nourished_nether:basalt_slab` | — | — | Yes | Yes | Yes | Yes |
| `nourished_nether:basalt_stairs` | — | — | Yes | Yes | Yes | Yes |
| `nourished_nether:basalt_wall` | — | — | Yes | Yes | Yes | Yes |
| `nourished_nether:chiseled_basalt_bricks` | — | — | Yes | Yes | Yes | Yes |
| `nourished_nether:chiseled_obsidian_bricks` | — | — | Yes | Yes | Yes | Yes |
| `nourished_nether:chiseled_red_nether_bricks` | — | — | Yes | Yes | Yes | Yes |
| `nourished_nether:chiseled_sludge_bricks` | — | — | Yes | Yes | Yes | Yes |
| `nourished_nether:chiseled_soul_quartz` | — | — | — | — | Yes | Yes |
| `nourished_nether:chiseled_warped_nether_bricks` | — | — | — | — | — | Yes |
| `nourished_nether:cracked_basalt_bricks` | — | — | Yes | Yes | Yes | Yes |
| `nourished_nether:cracked_obsidian_bricks` | — | — | Yes | Yes | Yes | Yes |
| `nourished_nether:cracked_quartz_bricks` | — | — | — | — | Yes | Yes |
| `nourished_nether:cracked_red_nether_bricks` | — | — | Yes | Yes | Yes | Yes |
| `nourished_nether:cracked_sludge_bricks` | — | — | Yes | Yes | Yes | Yes |
| `nourished_nether:cracked_smooth_basalt_bricks` | — | — | Yes | Yes | Yes | Yes |
| `nourished_nether:cracked_soul_quartz_bricks` | — | — | — | — | Yes | Yes |
| `nourished_nether:cracked_warped_bricks` | — | — | — | — | — | Yes |
| `nourished_nether:ectoplasm_block` | Yes | Yes | Yes | Yes | Yes | Yes |
| `nourished_nether:ectoplasm_block_active` | Yes | Yes | Yes | Yes | Yes | Yes |
| `nourished_nether:ghoulflower` | Yes | Yes | Yes | Yes | Yes | Yes |
| `nourished_nether:gilded_blackstone_brick_slab` | — | — | Yes | Yes | Yes | Yes |
| `nourished_nether:gilded_blackstone_brick_stairs` | — | — | Yes | Yes | Yes | Yes |
| `nourished_nether:gilded_blackstone_brick_wall` | — | — | Yes | Yes | Yes | Yes |
| `nourished_nether:gilded_blackstone_bricks` | — | — | Yes | Yes | Yes | Yes |
| `nourished_nether:glowing_obsidian` | — | — | Yes | Yes | Yes | Yes |
| `nourished_nether:hoglin_hide_block` | Yes | Yes | Yes | Yes | Yes | Yes |
| `nourished_nether:necronium_block` | — | — | Yes | Yes | Yes | Yes |
| `nourished_nether:nether_brick_fence_gate` | — | — | Yes | Yes | Yes | Yes |
| `nourished_nether:nether_reactor_core` | — | — | Yes | Yes | Yes | Yes |
| `nourished_nether:nether_reactor_rod` | — | — | Yes | Yes | Yes | Yes |
| `nourished_nether:netherite_scrap_block` | — | — | Yes | Yes | Yes | Yes |
| `nourished_nether:netherrack_slab` | — | — | Yes | Yes | Yes | Yes |
| `nourished_nether:netherrack_stairs` | — | — | Yes | Yes | Yes | Yes |
| `nourished_nether:netherrack_wall` | — | — | Yes | Yes | Yes | Yes |
| `nourished_nether:obsidian_brick_slab` | — | — | Yes | Yes | Yes | Yes |
| `nourished_nether:obsidian_brick_stairs` | — | — | Yes | Yes | Yes | Yes |
| `nourished_nether:obsidian_brick_wall` | — | — | Yes | Yes | Yes | Yes |
| `nourished_nether:obsidian_bricks` | — | — | Yes | Yes | Yes | Yes |
| `nourished_nether:obsidian_slab` | — | — | Yes | Yes | Yes | Yes |
| `nourished_nether:obsidian_stairs` | — | — | Yes | Yes | Yes | Yes |
| `nourished_nether:obsidian_wall` | — | — | Yes | Yes | Yes | Yes |
| `nourished_nether:polished_basalt_slab` | — | — | Yes | Yes | Yes | Yes |
| `nourished_nether:polished_basalt_stairs` | — | — | Yes | Yes | Yes | Yes |
| `nourished_nether:polished_basalt_wall` | — | — | Yes | Yes | Yes | Yes |
| `nourished_nether:quartz_brick_slab` | — | — | — | — | Yes | Yes |
| `nourished_nether:quartz_brick_stairs` | — | — | — | — | Yes | Yes |
| `nourished_nether:quartz_brick_wall` | — | — | — | — | Yes | Yes |
| `nourished_nether:quartz_wall` | — | — | — | — | Yes | Yes |
| `nourished_nether:red_nether_brick_fence` | — | — | Yes | Yes | Yes | Yes |
| `nourished_nether:red_nether_brick_fence_gate` | — | — | Yes | Yes | Yes | Yes |
| `nourished_nether:rich_soul_sludge` | — | Yes | Yes | Yes | Yes | Yes |
| `nourished_nether:sludge_brick_fence` | — | — | Yes | Yes | Yes | Yes |
| `nourished_nether:sludge_brick_fence_gate` | — | — | Yes | Yes | Yes | Yes |
| `nourished_nether:sludge_brick_slab` | — | — | Yes | Yes | Yes | Yes |
| `nourished_nether:sludge_brick_stairs` | — | — | Yes | Yes | Yes | Yes |
| `nourished_nether:sludge_brick_wall` | — | — | Yes | Yes | Yes | Yes |
| `nourished_nether:sludge_bricks` | — | — | Yes | Yes | Yes | Yes |
| `nourished_nether:smooth_basalt_brick_slab` | — | — | Yes | Yes | Yes | Yes |
| `nourished_nether:smooth_basalt_brick_stairs` | — | — | Yes | Yes | Yes | Yes |
| `nourished_nether:smooth_basalt_brick_wall` | — | — | Yes | Yes | Yes | Yes |
| `nourished_nether:smooth_basalt_bricks` | — | — | Yes | Yes | Yes | Yes |
| `nourished_nether:smooth_basalt_slab` | — | — | Yes | Yes | Yes | Yes |
| `nourished_nether:smooth_basalt_stairs` | — | — | Yes | Yes | Yes | Yes |
| `nourished_nether:smooth_basalt_wall` | — | — | Yes | Yes | Yes | Yes |
| `nourished_nether:smooth_quartz_wall` | — | — | — | — | Yes | Yes |
| `nourished_nether:smooth_soul_quartz` | — | — | — | — | Yes | Yes |
| `nourished_nether:smooth_soul_quartz_slab` | — | — | — | — | Yes | Yes |
| `nourished_nether:smooth_soul_quartz_stairs` | — | — | — | — | Yes | Yes |
| `nourished_nether:smooth_soul_quartz_wall` | — | — | — | — | Yes | Yes |
| `nourished_nether:soul_forge` | — | — | Yes | Yes | Yes | Yes |
| `nourished_nether:soul_quartz_block` | — | — | — | — | Yes | Yes |
| `nourished_nether:soul_quartz_brick_slab` | — | — | — | — | Yes | Yes |
| `nourished_nether:soul_quartz_brick_stairs` | — | — | — | — | Yes | Yes |
| `nourished_nether:soul_quartz_brick_wall` | — | — | — | — | Yes | Yes |
| `nourished_nether:soul_quartz_bricks` | — | — | — | — | Yes | Yes |
| `nourished_nether:soul_quartz_pillar` | — | — | — | — | Yes | Yes |
| `nourished_nether:soul_quartz_slab` | — | — | — | — | Yes | Yes |
| `nourished_nether:soul_quartz_stairs` | — | — | — | — | Yes | Yes |
| `nourished_nether:soul_quartz_wall` | — | — | — | — | Yes | Yes |
| `nourished_nether:soul_sludge` | — | Yes | Yes | Yes | Yes | Yes |
| `nourished_nether:soul_weeds` | — | — | — | — | — | Yes |
| `nourished_nether:warped_brick_fence` | — | — | — | — | — | Yes |
| `nourished_nether:warped_brick_fence_gate` | — | — | — | — | — | Yes |
| `nourished_nether:warped_brick_slab` | — | — | — | — | — | Yes |
| `nourished_nether:warped_brick_stairs` | — | — | — | — | — | Yes |
| `nourished_nether:warped_brick_wall` | — | — | — | — | — | Yes |
| `nourished_nether:warped_bricks` | — | — | — | — | — | Yes |
| `nourished_nether:wither_bone_block` | — | — | Yes | Yes | Yes | Yes |
| `quark:acacia_bookshelf` | — | Yes | — | — | — | — |
| `quark:acacia_chest` | — | Yes | — | — | — | — |
| `quark:acacia_hedge` | — | Yes | — | — | — | — |
| `quark:acacia_ladder` | — | Yes | — | — | — | — |
| `quark:acacia_leaf_carpet` | — | Yes | — | — | — | — |
| `quark:acacia_post` | — | Yes | — | — | — | — |
| `quark:acacia_trapped_chest` | — | — | — | — | — | — |
| `quark:acacia_vertical_slab` | — | Yes | — | — | — | — |
| `quark:ancient_bookshelf` | Yes | Yes | Yes | Yes | Yes | — |
| `quark:ancient_button` | Yes | Yes | Yes | Yes | Yes | Yes |
| `quark:ancient_chest` | Yes | Yes | Yes | Yes | Yes | Yes |
| `quark:ancient_door` | Yes | Yes | Yes | Yes | Yes | Yes |
| `quark:ancient_fence` | Yes | Yes | Yes | Yes | Yes | Yes |
| `quark:ancient_fence_gate` | Yes | Yes | Yes | Yes | Yes | Yes |
| `quark:ancient_hanging_sign` | — | — | Yes | Yes | — | — |
| `quark:ancient_hedge` | Yes | Yes | Yes | Yes | Yes | Yes |
| `quark:ancient_ladder` | Yes | Yes | Yes | Yes | Yes | Yes |
| `quark:ancient_leaf_carpet` | Yes | Yes | Yes | Yes | Yes | Yes |
| `quark:ancient_leaves` | Yes | Yes | Yes | Yes | Yes | Yes |
| `quark:ancient_log` | Yes | Yes | Yes | Yes | Yes | Yes |
| `quark:ancient_planks` | Yes | Yes | Yes | Yes | Yes | Yes |
| `quark:ancient_planks_slab` | Yes | Yes | Yes | Yes | Yes | Yes |
| `quark:ancient_planks_stairs` | Yes | Yes | Yes | Yes | Yes | Yes |
| `quark:ancient_planks_vertical_slab` | Yes | Yes | Yes | Yes | Yes | Yes |
| `quark:ancient_post` | Yes | Yes | Yes | Yes | Yes | Yes |
| `quark:ancient_pressure_plate` | Yes | Yes | Yes | Yes | Yes | Yes |
| `quark:ancient_sapling` | Yes | Yes | Yes | Yes | Yes | Yes |
| `quark:ancient_sign` | Yes | Yes | Yes | Yes | Yes | Yes |
| `quark:ancient_trapdoor` | Yes | Yes | Yes | Yes | Yes | Yes |
| `quark:ancient_trapped_chest` | — | — | Yes | Yes | Yes | Yes |
| `quark:ancient_wall_hanging_sign` | — | — | Yes | Yes | — | — |
| `quark:ancient_wall_sign` | Yes | Yes | Yes | Yes | Yes | Yes |
| `quark:ancient_wood` | Yes | Yes | Yes | Yes | Yes | Yes |
| `quark:andesite_bricks` | — | — | Yes | Yes | Yes | — |
| `quark:andesite_bricks_slab` | — | — | Yes | Yes | Yes | — |
| `quark:andesite_bricks_stairs` | — | — | Yes | Yes | Yes | — |
| `quark:andesite_bricks_vertical_slab` | — | — | Yes | Yes | Yes | — |
| `quark:andesite_bricks_wall` | — | — | Yes | Yes | Yes | — |
| `quark:andesite_pillar` | — | — | Yes | Yes | Yes | — |
| `quark:andesite_vertical_slab` | — | — | Yes | Yes | Yes | — |
| `quark:apple_crate` | Yes | Yes | Yes | Yes | Yes | Yes |
| `quark:azalea_bookshelf` | Yes | — | — | — | — | — |
| `quark:azalea_button` | Yes | — | — | — | — | — |
| `quark:azalea_chest` | Yes | — | — | — | — | — |
| `quark:azalea_door` | Yes | — | — | — | — | — |
| `quark:azalea_fence` | Yes | — | — | — | — | — |
| `quark:azalea_fence_gate` | Yes | — | — | — | — | — |
| `quark:azalea_hanging_sign` | — | — | — | — | — | — |
| `quark:azalea_hedge` | Yes | — | — | — | — | — |
| `quark:azalea_ladder` | Yes | — | — | — | — | — |
| `quark:azalea_leaf_carpet` | Yes | — | — | — | — | — |
| `quark:azalea_log` | Yes | — | — | — | — | — |
| `quark:azalea_planks` | Yes | — | — | — | — | — |
| `quark:azalea_planks_slab` | Yes | — | — | — | — | — |
| `quark:azalea_planks_stairs` | Yes | — | — | — | — | — |
| `quark:azalea_planks_vertical_slab` | Yes | — | — | — | — | — |
| `quark:azalea_post` | Yes | — | — | — | — | — |
| `quark:azalea_pressure_plate` | Yes | — | — | — | — | — |
| `quark:azalea_sign` | Yes | — | — | — | — | — |
| `quark:azalea_trapdoor` | Yes | — | — | — | — | — |
| `quark:azalea_trapped_chest` | — | — | — | — | — | — |
| `quark:azalea_wall_hanging_sign` | — | — | — | — | — | — |
| `quark:azalea_wall_sign` | Yes | — | — | — | — | — |
| `quark:azalea_wood` | Yes | — | — | — | — | — |
| `quark:bamboo_bookshelf` | Yes | — | — | — | — | — |
| `quark:bamboo_chest` | Yes | — | — | — | — | — |
| `quark:bamboo_ladder` | Yes | — | — | — | — | — |
| `quark:bamboo_mat` | Yes | — | — | — | — | — |
| `quark:bamboo_mat_carpet` | Yes | — | — | — | — | — |
| `quark:bamboo_mosaic_vertical_slab` | Yes | — | — | — | — | — |
| `quark:bamboo_post` | Yes | — | — | — | — | — |
| `quark:bamboo_trapped_chest` | — | — | — | — | — | — |
| `quark:bamboo_vertical_slab` | Yes | — | — | — | — | — |
| `quark:beetroot_crate` | Yes | Yes | Yes | Yes | — | — |
| `quark:berry_sack` | Yes | — | — | Yes | — | — |
| `quark:birch_bookshelf` | — | — | Yes | — | — | — |
| `quark:birch_chest` | — | — | Yes | — | — | — |
| `quark:birch_hedge` | — | — | Yes | — | — | — |
| `quark:birch_ladder` | — | — | Yes | — | — | — |
| `quark:birch_leaf_carpet` | — | — | Yes | — | — | — |
| `quark:birch_post` | — | — | Yes | — | — | — |
| `quark:birch_trapped_chest` | — | — | Yes | — | — | — |
| `quark:birch_vertical_slab` | — | — | Yes | — | — | — |
| `quark:black_corundum` | — | — | Yes | Yes | Yes | Yes |
| `quark:black_corundum_cluster` | — | — | Yes | Yes | Yes | Yes |
| `quark:black_corundum_pane` | — | — | Yes | Yes | Yes | Yes |
| `quark:black_crystal_lamp` | — | — | — | — | Yes | Yes |
| `quark:black_framed_glass` | — | — | Yes | Yes | — | — |
| `quark:black_framed_glass_pane` | — | — | Yes | Yes | — | — |
| `quark:black_shingles` | — | — | — | — | Yes | — |
| `quark:black_shingles_slab` | — | — | — | — | Yes | — |
| `quark:black_shingles_stairs` | — | — | — | — | Yes | — |
| `quark:black_shingles_vertical_slab` | — | — | — | — | Yes | — |
| `quark:black_stool` | Yes | Yes | Yes | Yes | Yes | Yes |
| `quark:blackstone_bricks` | — | — | — | — | — | — |
| `quark:blackstone_bricks_slab` | — | — | — | — | — | — |
| `quark:blackstone_bricks_stairs` | — | — | — | — | — | — |
| `quark:blackstone_bricks_vertical_slab` | — | — | — | — | — | — |
| `quark:blackstone_bricks_wall` | — | — | — | — | — | — |
| `quark:blackstone_furnace` | — | — | — | — | Yes | Yes |
| `quark:blackstone_vertical_slab` | — | — | — | — | Yes | Yes |
| `quark:blaze_lantern` | — | — | — | — | Yes | Yes |
| `quark:blossom_bookshelf` | Yes | Yes | Yes | Yes | Yes | — |
| `quark:blossom_button` | Yes | Yes | Yes | Yes | Yes | Yes |
| `quark:blossom_chest` | Yes | Yes | Yes | Yes | Yes | Yes |
| `quark:blossom_door` | Yes | Yes | Yes | Yes | Yes | Yes |
| `quark:blossom_fence` | Yes | Yes | Yes | Yes | Yes | Yes |
| `quark:blossom_fence_gate` | Yes | Yes | Yes | Yes | Yes | Yes |
| `quark:blossom_hanging_sign` | — | — | Yes | Yes | — | — |
| `quark:blossom_ladder` | Yes | Yes | Yes | Yes | Yes | Yes |
| `quark:blossom_log` | Yes | Yes | Yes | Yes | Yes | Yes |
| `quark:blossom_planks` | Yes | Yes | Yes | Yes | Yes | Yes |
| `quark:blossom_planks_slab` | Yes | Yes | Yes | Yes | Yes | Yes |
| `quark:blossom_planks_stairs` | Yes | Yes | Yes | Yes | Yes | Yes |
| `quark:blossom_planks_vertical_slab` | Yes | Yes | Yes | Yes | Yes | Yes |
| `quark:blossom_post` | Yes | Yes | Yes | Yes | Yes | Yes |
| `quark:blossom_pressure_plate` | Yes | Yes | Yes | Yes | Yes | Yes |
| `quark:blossom_sign` | Yes | Yes | Yes | Yes | Yes | Yes |
| `quark:blossom_trapdoor` | Yes | Yes | Yes | Yes | Yes | Yes |
| `quark:blossom_trapped_chest` | — | — | Yes | Yes | Yes | Yes |
| `quark:blossom_wall_hanging_sign` | — | — | Yes | Yes | — | — |
| `quark:blossom_wall_sign` | Yes | Yes | Yes | Yes | Yes | Yes |
| `quark:blossom_wood` | Yes | Yes | Yes | Yes | Yes | Yes |
| `quark:blue_blossom_hedge` | Yes | Yes | Yes | Yes | Yes | Yes |
| `quark:blue_blossom_leaf_carpet` | Yes | Yes | Yes | Yes | Yes | Yes |
| `quark:blue_blossom_leaves` | Yes | Yes | Yes | Yes | Yes | Yes |
| `quark:blue_blossom_sapling` | Yes | Yes | Yes | Yes | Yes | Yes |
| `quark:blue_corundum` | — | — | Yes | Yes | Yes | Yes |
| `quark:blue_corundum_cluster` | — | — | Yes | Yes | Yes | Yes |
| `quark:blue_corundum_pane` | — | — | Yes | Yes | Yes | Yes |
| `quark:blue_crystal_lamp` | — | — | — | — | Yes | Yes |
| `quark:blue_framed_glass` | — | — | Yes | Yes | — | — |
| `quark:blue_framed_glass_pane` | — | — | Yes | Yes | — | — |
| `quark:blue_nether_bricks` | — | — | — | — | Yes | Yes |
| `quark:blue_nether_bricks_slab` | — | — | — | — | Yes | Yes |
| `quark:blue_nether_bricks_stairs` | — | — | — | — | Yes | Yes |
| `quark:blue_nether_bricks_vertical_slab` | — | — | — | — | Yes | Yes |
| `quark:blue_nether_bricks_wall` | — | — | — | — | Yes | Yes |
| `quark:blue_shingles` | — | — | — | — | — | — |
| `quark:blue_shingles_slab` | — | — | — | — | — | — |
| `quark:blue_shingles_stairs` | — | — | — | — | — | — |
| `quark:blue_shingles_vertical_slab` | — | — | — | — | — | — |
| `quark:blue_stool` | Yes | Yes | Yes | Yes | — | — |
| `quark:bonded_leather` | Yes | Yes | Yes | Yes | Yes | Yes |
| `quark:bonded_rabbit_hide` | Yes | Yes | Yes | Yes | Yes | Yes |
| `quark:bonded_ravager_hide` | Yes | Yes | Yes | Yes | Yes | Yes |
| `quark:brick_vertical_slab` | — | — | Yes | — | — | — |
| `quark:brown_framed_glass` | — | — | — | — | — | — |
| `quark:brown_framed_glass_pane` | — | — | — | — | — | — |
| `quark:brown_shingles` | — | — | — | — | — | — |
| `quark:brown_shingles_slab` | — | — | — | — | — | — |
| `quark:brown_shingles_stairs` | — | — | — | — | — | — |
| `quark:brown_shingles_vertical_slab` | — | — | — | — | — | — |
| `quark:brown_stool` | Yes | — | — | — | — | — |
| `quark:cactus_block` | Yes | — | — | — | Yes | — |
| `quark:calcite_bricks` | — | — | — | Yes | — | — |
| `quark:calcite_bricks_slab` | — | — | — | Yes | — | — |
| `quark:calcite_bricks_stairs` | — | — | — | Yes | — | — |
| `quark:calcite_bricks_vertical_slab` | — | — | — | Yes | — | — |
| `quark:calcite_bricks_wall` | — | — | — | Yes | — | — |
| `quark:calcite_pillar` | — | — | — | Yes | — | — |
| `quark:calcite_slab` | — | — | — | Yes | — | — |
| `quark:calcite_stairs` | — | — | — | Yes | — | — |
| `quark:calcite_vertical_slab` | — | — | — | Yes | — | — |
| `quark:calcite_wall` | — | — | — | Yes | — | — |
| `quark:carrot_crate` | Yes | Yes | Yes | — | — | — |
| `quark:carved_mud_bricks` | — | — | — | — | — | — |
| `quark:charcoal_block` | — | — | Yes | Yes | Yes | Yes |
| `quark:cherry_bookshelf` | Yes | — | — | — | — | — |
| `quark:cherry_chest` | Yes | — | — | — | — | — |
| `quark:cherry_hedge` | Yes | — | — | — | — | — |
| `quark:cherry_ladder` | Yes | — | — | — | — | — |
| `quark:cherry_leaf_carpet` | Yes | — | — | — | — | — |
| `quark:cherry_post` | Yes | — | — | — | — | — |
| `quark:cherry_trapped_chest` | — | — | — | — | — | — |
| `quark:cherry_vertical_slab` | Yes | — | — | — | — | — |
| `quark:chiseled_andesite_bricks` | — | — | Yes | Yes | Yes | — |
| `quark:chiseled_calcite_bricks` | — | — | — | Yes | — | — |
| `quark:chiseled_diorite_bricks` | — | — | Yes | Yes | Yes | — |
| `quark:chiseled_dripstone_bricks` | — | — | — | Yes | — | — |
| `quark:chiseled_granite_bricks` | — | — | Yes | Yes | Yes | — |
| `quark:chiseled_jasper_bricks` | — | — | Yes | Yes | Yes | Yes |
| `quark:chiseled_limestone_bricks` | — | — | Yes | Yes | Yes | Yes |
| `quark:chiseled_myalite_bricks` | — | — | Yes | Yes | Yes | Yes |
| `quark:chiseled_shale_bricks` | — | — | Yes | Yes | Yes | Yes |
| `quark:chiseled_soul_sandstone` | — | — | — | — | Yes | Yes |
| `quark:chiseled_tuff_bricks` | — | — | — | Yes | Yes | — |
| `quark:chorus_fruit_block` | — | — | — | — | Yes | — |
| `quark:chorus_twist` | — | — | — | — | Yes | — |
| `quark:chorus_weeds` | — | — | — | — | Yes | — |
| `quark:chute` | Yes | Yes | Yes | Yes | Yes | Yes |
| `quark:cloud` | Yes | Yes | Yes | Yes | Yes | Yes |
| `quark:cobbled_deepslate_vertical_slab` | — | — | — | Yes | Yes | — |
| `quark:cobblestone_bricks` | — | — | Yes | Yes | Yes | — |
| `quark:cobblestone_bricks_slab` | — | — | Yes | Yes | Yes | — |
| `quark:cobblestone_bricks_stairs` | — | — | Yes | Yes | Yes | — |
| `quark:cobblestone_bricks_vertical_slab` | — | — | Yes | Yes | Yes | — |
| `quark:cobblestone_bricks_wall` | — | — | Yes | Yes | Yes | — |
| `quark:cobblestone_vertical_slab` | — | — | Yes | Yes | Yes | — |
| `quark:cocoa_beans_sack` | Yes | — | — | — | — | — |
| `quark:crafter` | — | — | — | — | Yes | — |
| `quark:crate` | Yes | Yes | Yes | Yes | Yes | Yes |
| `quark:crimson_bookshelf` | — | — | — | — | — | — |
| `quark:crimson_chest` | — | — | — | — | — | Yes |
| `quark:crimson_ladder` | — | — | — | — | — | Yes |
| `quark:crimson_post` | — | — | — | — | — | Yes |
| `quark:crimson_trapped_chest` | — | — | — | — | — | Yes |
| `quark:crimson_vertical_slab` | — | — | — | — | — | Yes |
| `quark:cut_copper_vertical_slab` | — | — | Yes | Yes | Yes | Yes |
| `quark:cut_red_sandstone_vertical_slab` | — | — | — | — | Yes | Yes |
| `quark:cut_sandstone_vertical_slab` | — | — | Yes | Yes | Yes | — |
| `quark:cut_soul_sandstone` | — | — | — | — | Yes | Yes |
| `quark:cut_soul_sandstone_slab` | — | — | — | — | Yes | Yes |
| `quark:cut_soul_sandstone_vertical_slab` | — | — | — | — | Yes | Yes |
| `quark:cut_vine` | Yes | Yes | Yes | Yes | Yes | Yes |
| `quark:cyan_framed_glass` | — | — | — | — | — | — |
| `quark:cyan_framed_glass_pane` | — | — | — | — | — | — |
| `quark:cyan_shingles` | — | — | — | — | — | — |
| `quark:cyan_shingles_slab` | — | — | — | — | — | — |
| `quark:cyan_shingles_stairs` | — | — | — | — | — | — |
| `quark:cyan_shingles_vertical_slab` | — | — | — | — | — | — |
| `quark:cyan_stool` | Yes | — | — | — | — | — |
| `quark:dark_oak_bookshelf` | Yes | — | — | — | — | — |
| `quark:dark_oak_chest` | Yes | — | — | — | — | — |
| `quark:dark_oak_hedge` | Yes | — | — | — | — | — |
| `quark:dark_oak_ladder` | Yes | — | — | — | — | — |
| `quark:dark_oak_leaf_carpet` | Yes | — | — | — | — | — |
| `quark:dark_oak_post` | Yes | — | — | — | — | — |
| `quark:dark_oak_trapped_chest` | — | — | — | — | — | — |
| `quark:dark_oak_vertical_slab` | Yes | — | — | — | — | — |
| `quark:dark_prismarine_vertical_slab` | — | — | — | Yes | — | — |
| `quark:deepslate_brick_vertical_slab` | — | — | — | Yes | Yes | — |
| `quark:deepslate_furnace` | — | — | — | Yes | Yes | — |
| `quark:deepslate_tile_vertical_slab` | — | — | — | Yes | Yes | — |
| `quark:diorite_bricks` | — | — | Yes | Yes | Yes | — |
| `quark:diorite_bricks_slab` | — | — | Yes | Yes | Yes | — |
| `quark:diorite_bricks_stairs` | — | — | Yes | Yes | Yes | — |
| `quark:diorite_bricks_vertical_slab` | — | — | Yes | Yes | Yes | — |
| `quark:diorite_bricks_wall` | — | — | Yes | Yes | Yes | — |
| `quark:diorite_pillar` | — | — | Yes | Yes | Yes | — |
| `quark:diorite_vertical_slab` | — | — | Yes | Yes | Yes | — |
| `quark:dirt_bricks` | — | — | Yes | Yes | Yes | — |
| `quark:dirt_bricks_slab` | — | — | Yes | Yes | Yes | — |
| `quark:dirt_bricks_stairs` | — | — | Yes | Yes | Yes | — |
| `quark:dirt_bricks_vertical_slab` | — | — | Yes | Yes | Yes | — |
| `quark:dirt_bricks_wall` | — | — | Yes | Yes | Yes | — |
| `quark:dirty_glass` | — | Yes | Yes | Yes | Yes | Yes |
| `quark:dirty_glass_pane` | — | Yes | Yes | Yes | Yes | Yes |
| `quark:dripstone_block_slab` | — | — | — | Yes | — | — |
| `quark:dripstone_block_stairs` | — | — | — | Yes | — | — |
| `quark:dripstone_block_vertical_slab` | — | — | — | Yes | — | — |
| `quark:dripstone_block_wall` | — | — | — | Yes | — | — |
| `quark:dripstone_bricks` | — | — | — | Yes | — | — |
| `quark:dripstone_bricks_slab` | — | — | — | Yes | — | — |
| `quark:dripstone_bricks_stairs` | — | — | — | Yes | — | — |
| `quark:dripstone_bricks_vertical_slab` | — | — | — | Yes | — | — |
| `quark:dripstone_bricks_wall` | — | — | — | Yes | — | — |
| `quark:dripstone_pillar` | — | — | — | Yes | — | — |
| `quark:duskbound_block` | — | — | — | — | Yes | — |
| `quark:duskbound_block_slab` | — | — | — | — | Yes | — |
| `quark:duskbound_block_stairs` | — | — | — | — | Yes | — |
| `quark:duskbound_block_vertical_slab` | — | — | — | — | Yes | — |
| `quark:duskbound_lantern` | — | — | — | — | Yes | — |
| `quark:dusky_myalite` | — | — | Yes | Yes | Yes | Yes |
| `quark:encased_pipe` | — | — | Yes | Yes | Yes | Yes |
| `quark:end_stone_brick_vertical_slab` | — | — | — | — | Yes | — |
| `quark:ender_watcher` | — | — | — | — | — | — |
| `quark:exposed_cut_copper_vertical_slab` | — | — | Yes | Yes | Yes | Yes |
| `quark:feeding_trough` | Yes | Yes | Yes | — | — | — |
| `quark:flowering_azalea_hedge` | Yes | — | — | — | — | — |
| `quark:flowering_azalea_leaf_carpet` | Yes | — | — | — | — | — |
| `quark:framed_glass` | — | — | Yes | Yes | — | — |
| `quark:framed_glass_pane` | — | — | Yes | Yes | — | — |
| `quark:glow_lichen_growth` | Yes | Yes | Yes | Yes | Yes | Yes |
| `quark:glow_shroom` | — | — | — | Yes | — | — |
| `quark:glow_shroom_block` | — | — | — | Yes | — | — |
| `quark:glow_shroom_ring` | — | — | — | Yes | — | — |
| `quark:glow_shroom_stem` | — | — | — | Yes | — | — |
| `quark:glowberry_sack` | Yes | — | — | — | — | — |
| `quark:gold_bars` | — | — | Yes | Yes | Yes | Yes |
| `quark:gold_button` | — | — | Yes | Yes | Yes | Yes |
| `quark:golden_apple_crate` | — | — | Yes | Yes | Yes | Yes |
| `quark:golden_carrot_crate` | — | — | Yes | — | — | — |
| `quark:granite_bricks` | — | — | Yes | Yes | Yes | — |
| `quark:granite_bricks_slab` | — | — | Yes | Yes | Yes | — |
| `quark:granite_bricks_stairs` | — | — | Yes | Yes | Yes | — |
| `quark:granite_bricks_vertical_slab` | — | — | Yes | Yes | Yes | — |
| `quark:granite_bricks_wall` | — | — | Yes | Yes | Yes | — |
| `quark:granite_pillar` | — | — | Yes | Yes | Yes | — |
| `quark:granite_vertical_slab` | — | — | Yes | Yes | Yes | — |
| `quark:grate` | — | — | Yes | Yes | — | — |
| `quark:gravisand` | — | — | — | — | Yes | — |
| `quark:gray_framed_glass` | — | — | Yes | Yes | — | — |
| `quark:gray_framed_glass_pane` | — | — | Yes | Yes | — | — |
| `quark:gray_shingles` | — | — | — | — | Yes | — |
| `quark:gray_shingles_slab` | — | — | — | — | Yes | — |
| `quark:gray_shingles_stairs` | — | — | — | — | Yes | — |
| `quark:gray_shingles_vertical_slab` | — | — | — | — | Yes | — |
| `quark:gray_stool` | Yes | Yes | Yes | Yes | Yes | Yes |
| `quark:green_corundum` | — | — | Yes | Yes | Yes | Yes |
| `quark:green_corundum_cluster` | — | — | Yes | Yes | Yes | Yes |
| `quark:green_corundum_pane` | — | — | Yes | Yes | Yes | Yes |
| `quark:green_crystal_lamp` | — | — | — | — | Yes | Yes |
| `quark:green_framed_glass` | — | — | — | — | — | — |
| `quark:green_framed_glass_pane` | — | — | — | — | — | — |
| `quark:green_shingles` | — | — | — | — | Yes | — |
| `quark:green_shingles_slab` | — | — | — | — | Yes | — |
| `quark:green_shingles_stairs` | — | — | — | — | Yes | — |
| `quark:green_shingles_vertical_slab` | — | — | — | — | Yes | — |
| `quark:green_stool` | Yes | — | — | — | Yes | — |
| `quark:gunpowder_sack` | Yes | Yes | Yes | Yes | Yes | Yes |
| `quark:hollow_acacia_log` | — | Yes | — | — | — | — |
| `quark:hollow_ancient_log` | Yes | Yes | Yes | Yes | Yes | Yes |
| `quark:hollow_azalea_log` | Yes | — | — | — | — | — |
| `quark:hollow_birch_log` | — | — | Yes | — | — | — |
| `quark:hollow_blossom_log` | Yes | Yes | Yes | Yes | Yes | Yes |
| `quark:hollow_cherry_log` | Yes | — | — | — | — | — |
| `quark:hollow_crimson_stem` | — | — | — | — | — | Yes |
| `quark:hollow_dark_oak_log` | Yes | — | — | — | — | — |
| `quark:hollow_jungle_log` | Yes | — | — | — | — | — |
| `quark:hollow_mangrove_log` | Yes | — | — | — | — | — |
| `quark:hollow_oak_log` | Yes | — | Yes | — | — | — |
| `quark:hollow_spruce_log` | — | — | — | Yes | — | — |
| `quark:hollow_warped_stem` | — | — | — | — | — | Yes |
| `quark:indigo_corundum` | — | — | Yes | Yes | Yes | Yes |
| `quark:indigo_corundum_cluster` | — | — | Yes | Yes | Yes | Yes |
| `quark:indigo_corundum_pane` | — | — | Yes | Yes | Yes | Yes |
| `quark:indigo_crystal_lamp` | — | — | — | — | Yes | Yes |
| `quark:iron_button` | — | — | Yes | Yes | — | — |
| `quark:iron_ladder` | — | — | Yes | Yes | — | — |
| `quark:iron_pillar` | — | — | Yes | Yes | — | — |
| `quark:iron_plate` | — | — | Yes | Yes | — | — |
| `quark:iron_plate_slab` | — | — | Yes | Yes | — | — |
| `quark:iron_plate_stairs` | — | — | Yes | Yes | — | — |
| `quark:iron_plate_vertical_slab` | — | — | Yes | Yes | — | — |
| `quark:iron_rod` | — | — | — | — | — | — |
| `quark:jasper` | — | — | Yes | Yes | Yes | Yes |
| `quark:jasper_bricks` | — | — | Yes | Yes | Yes | Yes |
| `quark:jasper_bricks_slab` | — | — | Yes | Yes | Yes | Yes |
| `quark:jasper_bricks_stairs` | — | — | Yes | Yes | Yes | Yes |
| `quark:jasper_bricks_vertical_slab` | — | — | Yes | Yes | Yes | Yes |
| `quark:jasper_bricks_wall` | — | — | Yes | Yes | Yes | Yes |
| `quark:jasper_pillar` | — | — | Yes | Yes | Yes | Yes |
| `quark:jasper_slab` | — | — | Yes | Yes | Yes | Yes |
| `quark:jasper_stairs` | — | — | Yes | Yes | Yes | Yes |
| `quark:jasper_vertical_slab` | — | — | Yes | Yes | Yes | Yes |
| `quark:jasper_wall` | — | — | Yes | Yes | Yes | Yes |
| `quark:jungle_bookshelf` | Yes | — | — | — | — | — |
| `quark:jungle_chest` | Yes | — | — | — | — | — |
| `quark:jungle_hedge` | Yes | — | — | — | — | — |
| `quark:jungle_ladder` | Yes | — | — | — | — | — |
| `quark:jungle_leaf_carpet` | Yes | — | — | — | — | — |
| `quark:jungle_post` | Yes | — | — | — | — | — |
| `quark:jungle_trapped_chest` | — | — | — | — | — | — |
| `quark:jungle_vertical_slab` | Yes | — | — | — | — | — |
| `quark:lavender_blossom_hedge` | Yes | Yes | Yes | Yes | Yes | Yes |
| `quark:lavender_blossom_leaf_carpet` | Yes | Yes | Yes | Yes | Yes | Yes |
| `quark:lavender_blossom_leaves` | Yes | Yes | Yes | Yes | Yes | Yes |
| `quark:lavender_blossom_sapling` | Yes | Yes | Yes | Yes | Yes | Yes |
| `quark:light_blue_framed_glass` | — | — | Yes | Yes | — | — |
| `quark:light_blue_framed_glass_pane` | — | — | Yes | Yes | — | — |
| `quark:light_blue_shingles` | — | — | — | — | — | — |
| `quark:light_blue_shingles_slab` | — | — | — | — | — | — |
| `quark:light_blue_shingles_stairs` | — | — | — | — | — | — |
| `quark:light_blue_shingles_vertical_slab` | — | — | — | — | — | — |
| `quark:light_blue_stool` | Yes | Yes | Yes | Yes | — | — |
| `quark:light_gray_framed_glass` | — | — | Yes | Yes | — | — |
| `quark:light_gray_framed_glass_pane` | — | — | Yes | Yes | — | — |
| `quark:light_gray_shingles` | — | — | — | — | Yes | — |
| `quark:light_gray_shingles_slab` | — | — | — | — | Yes | — |
| `quark:light_gray_shingles_stairs` | — | — | — | — | Yes | — |
| `quark:light_gray_shingles_vertical_slab` | — | — | — | — | Yes | — |
| `quark:light_gray_stool` | Yes | Yes | Yes | Yes | Yes | Yes |
| `quark:lime_framed_glass` | — | — | — | Yes | — | — |
| `quark:lime_framed_glass_pane` | — | — | — | Yes | — | — |
| `quark:lime_shingles` | — | — | — | — | Yes | — |
| `quark:lime_shingles_slab` | — | — | — | — | Yes | — |
| `quark:lime_shingles_stairs` | — | — | — | — | Yes | — |
| `quark:lime_shingles_vertical_slab` | — | — | — | — | Yes | — |
| `quark:lime_stool` | Yes | Yes | — | Yes | Yes | — |
| `quark:limestone` | — | — | Yes | Yes | Yes | Yes |
| `quark:limestone_bricks` | — | — | Yes | Yes | Yes | Yes |
| `quark:limestone_bricks_slab` | — | — | Yes | Yes | Yes | Yes |
| `quark:limestone_bricks_stairs` | — | — | Yes | Yes | Yes | Yes |
| `quark:limestone_bricks_vertical_slab` | — | — | Yes | Yes | Yes | Yes |
| `quark:limestone_bricks_wall` | — | — | Yes | Yes | Yes | Yes |
| `quark:limestone_pillar` | — | — | Yes | Yes | Yes | Yes |
| `quark:limestone_slab` | — | — | Yes | Yes | Yes | Yes |
| `quark:limestone_stairs` | — | — | Yes | Yes | Yes | Yes |
| `quark:limestone_vertical_slab` | — | — | Yes | Yes | Yes | Yes |
| `quark:limestone_wall` | — | — | Yes | Yes | Yes | Yes |
| `quark:magenta_framed_glass` | — | — | Yes | Yes | — | — |
| `quark:magenta_framed_glass_pane` | — | — | Yes | Yes | — | — |
| `quark:magenta_shingles` | — | — | — | — | — | — |
| `quark:magenta_shingles_slab` | — | — | — | — | — | — |
| `quark:magenta_shingles_stairs` | — | — | — | — | — | — |
| `quark:magenta_shingles_vertical_slab` | — | — | — | — | — | — |
| `quark:magenta_stool` | Yes | Yes | Yes | Yes | — | — |
| `quark:magnet` | — | — | Yes | Yes | Yes | Yes |
| `quark:magnetized_block` | Yes | Yes | Yes | Yes | Yes | Yes |
| `quark:mangrove_bookshelf` | Yes | — | — | — | — | — |
| `quark:mangrove_chest` | Yes | — | — | — | — | — |
| `quark:mangrove_hedge` | Yes | — | — | — | — | — |
| `quark:mangrove_ladder` | Yes | — | — | — | — | — |
| `quark:mangrove_leaf_carpet` | Yes | — | — | — | — | — |
| `quark:mangrove_post` | Yes | — | — | — | — | — |
| `quark:mangrove_trapped_chest` | — | — | — | — | — | — |
| `quark:mangrove_vertical_slab` | Yes | — | — | — | — | — |
| `quark:matrix_enchanter` | — | — | Yes | Yes | Yes | Yes |
| `quark:midori_block` | — | — | — | — | — | — |
| `quark:midori_block_slab` | — | — | — | — | — | — |
| `quark:midori_block_stairs` | — | — | — | — | — | — |
| `quark:midori_block_vertical_slab` | — | — | — | — | — | — |
| `quark:midori_pillar` | — | — | — | — | — | — |
| `quark:monster_box` | Yes | Yes | Yes | Yes | Yes | Yes |
| `quark:mossy_cobblestone_bricks` | — | — | — | — | — | — |
| `quark:mossy_cobblestone_bricks_slab` | — | — | — | — | — | — |
| `quark:mossy_cobblestone_bricks_stairs` | — | — | — | — | — | — |
| `quark:mossy_cobblestone_bricks_vertical_slab` | — | — | — | — | — | — |
| `quark:mossy_cobblestone_bricks_wall` | — | — | — | — | — | — |
| `quark:mossy_cobblestone_vertical_slab` | — | — | — | — | — | — |
| `quark:mossy_stone_brick_vertical_slab` | — | — | — | — | — | — |
| `quark:mud_brick_lattice` | — | — | — | — | — | — |
| `quark:mud_brick_vertical_slab` | — | — | — | — | — | — |
| `quark:mud_pillar` | — | — | — | — | — | — |
| `quark:myalite` | — | — | Yes | Yes | Yes | Yes |
| `quark:myalite_bricks` | — | — | Yes | Yes | Yes | Yes |
| `quark:myalite_bricks_slab` | — | — | Yes | Yes | Yes | Yes |
| `quark:myalite_bricks_stairs` | — | — | Yes | Yes | Yes | Yes |
| `quark:myalite_bricks_vertical_slab` | — | — | Yes | Yes | Yes | Yes |
| `quark:myalite_bricks_wall` | — | — | Yes | Yes | Yes | Yes |
| `quark:myalite_crystal` | — | — | Yes | Yes | Yes | Yes |
| `quark:myalite_pillar` | — | — | Yes | Yes | Yes | Yes |
| `quark:myalite_slab` | — | — | Yes | Yes | Yes | Yes |
| `quark:myalite_stairs` | — | — | Yes | Yes | Yes | Yes |
| `quark:myalite_vertical_slab` | — | — | Yes | Yes | Yes | Yes |
| `quark:myalite_wall` | — | — | Yes | Yes | Yes | Yes |
| `quark:nether_brick_chest` | — | — | — | — | Yes | Yes |
| `quark:nether_brick_fence_gate` | — | — | — | — | Yes | Yes |
| `quark:nether_brick_trapped_chest` | — | — | — | — | Yes | Yes |
| `quark:nether_brick_vertical_slab` | — | — | — | — | Yes | Yes |
| `quark:nether_wart_sack` | — | — | — | — | — | Yes |
| `quark:netherrack_bricks` | — | — | — | — | — | — |
| `quark:netherrack_bricks_slab` | — | — | — | — | — | — |
| `quark:netherrack_bricks_stairs` | — | — | — | — | — | — |
| `quark:netherrack_bricks_vertical_slab` | — | — | — | — | — | — |
| `quark:netherrack_bricks_wall` | — | — | — | — | — | — |
| `quark:oak_chest` | Yes | — | Yes | — | — | — |
| `quark:oak_hedge` | Yes | — | Yes | — | — | — |
| `quark:oak_leaf_carpet` | Yes | — | Yes | — | — | — |
| `quark:oak_post` | Yes | — | Yes | — | — | — |
| `quark:oak_trapped_chest` | — | — | Yes | — | — | — |
| `quark:oak_vertical_slab` | Yes | — | Yes | — | — | — |
| `quark:obsidian_pressure_plate` | — | — | — | — | Yes | Yes |
| `quark:orange_blossom_hedge` | Yes | Yes | Yes | Yes | Yes | Yes |
| `quark:orange_blossom_leaf_carpet` | Yes | Yes | Yes | Yes | Yes | Yes |
| `quark:orange_blossom_leaves` | Yes | Yes | Yes | Yes | Yes | Yes |
| `quark:orange_blossom_sapling` | Yes | Yes | Yes | Yes | Yes | Yes |
| `quark:orange_corundum` | — | — | Yes | Yes | Yes | Yes |
| `quark:orange_corundum_cluster` | — | — | Yes | Yes | Yes | Yes |
| `quark:orange_corundum_pane` | — | — | Yes | Yes | Yes | Yes |
| `quark:orange_crystal_lamp` | — | — | — | — | Yes | Yes |
| `quark:orange_framed_glass` | — | — | Yes | Yes | — | — |
| `quark:orange_framed_glass_pane` | — | — | Yes | Yes | — | — |
| `quark:orange_shingles` | — | — | — | — | Yes | — |
| `quark:orange_shingles_slab` | — | — | — | — | Yes | — |
| `quark:orange_shingles_stairs` | — | — | — | — | Yes | — |
| `quark:orange_shingles_vertical_slab` | — | — | — | — | Yes | — |
| `quark:orange_stool` | Yes | Yes | Yes | Yes | Yes | Yes |
| `quark:oxidized_cut_copper_vertical_slab` | — | — | Yes | Yes | Yes | Yes |
| `quark:paper_lantern` | — | — | — | — | — | — |
| `quark:paper_lantern_sakura` | — | — | — | — | — | — |
| `quark:paper_wall` | Yes | — | — | — | — | — |
| `quark:paper_wall_big` | Yes | — | — | — | — | — |
| `quark:paper_wall_sakura` | Yes | — | — | — | — | — |
| `quark:permafrost` | — | — | Yes | Yes | Yes | Yes |
| `quark:permafrost_bricks` | — | — | Yes | Yes | Yes | Yes |
| `quark:permafrost_bricks_slab` | — | — | Yes | Yes | Yes | Yes |
| `quark:permafrost_bricks_stairs` | — | — | Yes | Yes | Yes | Yes |
| `quark:permafrost_bricks_vertical_slab` | — | — | Yes | Yes | Yes | Yes |
| `quark:permafrost_bricks_wall` | — | — | Yes | Yes | Yes | Yes |
| `quark:permafrost_slab` | — | — | Yes | Yes | Yes | Yes |
| `quark:permafrost_stairs` | — | — | Yes | Yes | Yes | Yes |
| `quark:permafrost_vertical_slab` | — | — | Yes | Yes | Yes | Yes |
| `quark:permafrost_wall` | — | — | Yes | Yes | Yes | Yes |
| `quark:pink_framed_glass` | — | — | Yes | Yes | — | — |
| `quark:pink_framed_glass_pane` | — | — | Yes | Yes | — | — |
| `quark:pink_shingles` | — | — | — | — | Yes | — |
| `quark:pink_shingles_slab` | — | — | — | — | Yes | — |
| `quark:pink_shingles_stairs` | — | — | — | — | Yes | — |
| `quark:pink_shingles_vertical_slab` | — | — | — | — | Yes | — |
| `quark:pink_stool` | Yes | Yes | Yes | Yes | Yes | Yes |
| `quark:pipe` | — | — | Yes | Yes | Yes | Yes |
| `quark:polished_andesite_vertical_slab` | — | — | Yes | Yes | Yes | — |
| `quark:polished_blackstone_brick_vertical_slab` | — | — | — | — | Yes | Yes |
| `quark:polished_blackstone_vertical_slab` | — | — | — | — | Yes | Yes |
| `quark:polished_calcite` | — | — | — | Yes | — | — |
| `quark:polished_calcite_slab` | — | — | — | Yes | — | — |
| `quark:polished_calcite_stairs` | — | — | — | Yes | — | — |
| `quark:polished_calcite_vertical_slab` | — | — | — | Yes | — | — |
| `quark:polished_deepslate_vertical_slab` | — | — | — | Yes | Yes | — |
| `quark:polished_diorite_vertical_slab` | — | — | Yes | Yes | Yes | — |
| `quark:polished_dripstone` | — | — | — | Yes | — | — |
| `quark:polished_dripstone_slab` | — | — | — | Yes | — | — |
| `quark:polished_dripstone_stairs` | — | — | — | Yes | — | — |
| `quark:polished_dripstone_vertical_slab` | — | — | — | Yes | — | — |
| `quark:polished_granite_vertical_slab` | — | — | Yes | Yes | Yes | — |
| `quark:polished_jasper` | — | — | Yes | Yes | Yes | Yes |
| `quark:polished_jasper_slab` | — | — | Yes | Yes | Yes | Yes |
| `quark:polished_jasper_stairs` | — | — | Yes | Yes | Yes | Yes |
| `quark:polished_jasper_vertical_slab` | — | — | Yes | Yes | Yes | Yes |
| `quark:polished_limestone` | — | — | Yes | Yes | Yes | Yes |
| `quark:polished_limestone_slab` | — | — | Yes | Yes | Yes | Yes |
| `quark:polished_limestone_stairs` | — | — | Yes | Yes | Yes | Yes |
| `quark:polished_limestone_vertical_slab` | — | — | Yes | Yes | Yes | Yes |
| `quark:polished_myalite` | — | — | Yes | Yes | Yes | Yes |
| `quark:polished_myalite_slab` | — | — | Yes | Yes | Yes | Yes |
| `quark:polished_myalite_stairs` | — | — | Yes | Yes | Yes | Yes |
| `quark:polished_myalite_vertical_slab` | — | — | Yes | Yes | Yes | Yes |
| `quark:polished_shale` | — | — | Yes | Yes | Yes | Yes |
| `quark:polished_shale_slab` | — | — | Yes | Yes | Yes | Yes |
| `quark:polished_shale_stairs` | — | — | Yes | Yes | Yes | Yes |
| `quark:polished_shale_vertical_slab` | — | — | Yes | Yes | Yes | Yes |
| `quark:polished_tuff` | — | — | — | Yes | Yes | — |
| `quark:polished_tuff_slab` | — | — | — | Yes | Yes | — |
| `quark:polished_tuff_stairs` | — | — | — | Yes | Yes | — |
| `quark:polished_tuff_vertical_slab` | — | — | — | Yes | Yes | — |
| `quark:potato_crate` | Yes | Yes | Yes | — | — | — |
| `quark:potted_ancient_sapling` | — | — | Yes | — | — | — |
| `quark:potted_beetroot` | — | — | Yes | — | — | — |
| `quark:potted_berries` | — | — | — | — | — | — |
| `quark:potted_blue_blossom_sapling` | — | — | Yes | — | — | — |
| `quark:potted_carrot` | — | — | Yes | — | — | — |
| `quark:potted_cave_vines` | — | — | — | — | — | — |
| `quark:potted_chorus` | — | — | — | — | — | — |
| `quark:potted_chorus_twist` | — | — | — | — | — | — |
| `quark:potted_chorus_weeds` | — | — | — | — | — | — |
| `quark:potted_cocoa_bean` | — | — | — | — | — | — |
| `quark:potted_glow_lichen_growth` | — | — | Yes | — | — | — |
| `quark:potted_glow_shroom` | — | — | — | — | — | — |
| `quark:potted_grass` | — | — | Yes | — | — | — |
| `quark:potted_large_fern` | — | — | — | — | — | — |
| `quark:potted_lavender_blossom_sapling` | — | — | Yes | — | — | — |
| `quark:potted_lilac` | — | — | — | — | — | — |
| `quark:potted_melon` | — | — | — | — | — | — |
| `quark:potted_nether_sprouts` | — | — | — | — | — | — |
| `quark:potted_nether_wart` | — | — | — | — | — | — |
| `quark:potted_orange_blossom_sapling` | — | — | Yes | — | — | — |
| `quark:potted_peony` | — | — | — | — | — | — |
| `quark:potted_pitcher_plant` | — | — | — | — | — | — |
| `quark:potted_potato` | — | — | Yes | — | — | — |
| `quark:potted_pumpkin` | — | — | Yes | — | — | — |
| `quark:potted_red_blossom_sapling` | — | — | Yes | — | — | — |
| `quark:potted_rose` | — | — | — | — | — | — |
| `quark:potted_sea_pickle` | — | — | — | — | — | — |
| `quark:potted_sugar_cane` | — | — | Yes | — | — | — |
| `quark:potted_sunflower` | — | — | Yes | — | — | — |
| `quark:potted_tall_grass` | — | — | Yes | — | — | — |
| `quark:potted_twisting_vines` | — | — | — | — | — | — |
| `quark:potted_vine` | — | — | — | — | — | — |
| `quark:potted_weeping_vines` | — | — | — | — | — | — |
| `quark:potted_wheat` | — | — | Yes | — | — | — |
| `quark:potted_yellow_blossom_sapling` | — | — | Yes | — | — | — |
| `quark:prismarine_brick_vertical_slab` | — | — | — | Yes | — | — |
| `quark:prismarine_chest` | — | — | — | Yes | — | — |
| `quark:prismarine_trapped_chest` | — | — | — | Yes | — | — |
| `quark:prismarine_vertical_slab` | — | — | — | Yes | — | — |
| `quark:purple_framed_glass` | — | — | Yes | Yes | — | — |
| `quark:purple_framed_glass_pane` | — | — | Yes | Yes | — | — |
| `quark:purple_shingles` | — | — | — | — | — | — |
| `quark:purple_shingles_slab` | — | — | — | — | — | — |
| `quark:purple_shingles_stairs` | — | — | — | — | — | — |
| `quark:purple_shingles_vertical_slab` | — | — | — | — | — | — |
| `quark:purple_stool` | Yes | Yes | Yes | Yes | — | — |
| `quark:purpur_chest` | — | — | — | — | Yes | — |
| `quark:purpur_trapped_chest` | — | — | — | — | Yes | — |
| `quark:purpur_vertical_slab` | — | — | — | — | Yes | — |
| `quark:quartz_vertical_slab` | — | — | — | — | Yes | Yes |
| `quark:raw_copper_bricks` | — | — | Yes | Yes | Yes | Yes |
| `quark:raw_copper_bricks_slab` | — | — | Yes | Yes | Yes | Yes |
| `quark:raw_copper_bricks_stairs` | — | — | Yes | Yes | Yes | Yes |
| `quark:raw_copper_bricks_vertical_slab` | — | — | Yes | Yes | Yes | Yes |
| `quark:raw_copper_bricks_wall` | — | — | Yes | Yes | Yes | Yes |
| `quark:raw_gold_bricks` | — | — | Yes | Yes | Yes | Yes |
| `quark:raw_gold_bricks_slab` | — | — | Yes | Yes | Yes | Yes |
| `quark:raw_gold_bricks_stairs` | — | — | Yes | Yes | Yes | Yes |
| `quark:raw_gold_bricks_vertical_slab` | — | — | Yes | Yes | Yes | Yes |
| `quark:raw_gold_bricks_wall` | — | — | Yes | Yes | Yes | Yes |
| `quark:raw_iron_bricks` | — | — | Yes | Yes | — | — |
| `quark:raw_iron_bricks_slab` | — | — | Yes | Yes | — | — |
| `quark:raw_iron_bricks_stairs` | — | — | Yes | Yes | — | — |
| `quark:raw_iron_bricks_vertical_slab` | — | — | Yes | Yes | — | — |
| `quark:raw_iron_bricks_wall` | — | — | Yes | Yes | — | — |
| `quark:red_blossom_hedge` | Yes | Yes | Yes | Yes | Yes | Yes |
| `quark:red_blossom_leaf_carpet` | Yes | Yes | Yes | Yes | Yes | Yes |
| `quark:red_blossom_leaves` | Yes | Yes | Yes | Yes | Yes | Yes |
| `quark:red_blossom_sapling` | Yes | Yes | Yes | Yes | Yes | Yes |
| `quark:red_corundum` | — | — | Yes | Yes | Yes | Yes |
| `quark:red_corundum_cluster` | — | — | Yes | Yes | Yes | Yes |
| `quark:red_corundum_pane` | — | — | Yes | Yes | Yes | Yes |
| `quark:red_crystal_lamp` | — | — | — | — | Yes | Yes |
| `quark:red_framed_glass` | — | — | Yes | Yes | — | — |
| `quark:red_framed_glass_pane` | — | — | Yes | Yes | — | — |
| `quark:red_nether_brick_vertical_slab` | — | — | Yes | Yes | Yes | Yes |
| `quark:red_sandstone_bricks` | — | — | — | — | Yes | Yes |
| `quark:red_sandstone_bricks_slab` | — | — | — | — | Yes | Yes |
| `quark:red_sandstone_bricks_stairs` | — | — | — | — | Yes | Yes |
| `quark:red_sandstone_bricks_vertical_slab` | — | — | — | — | Yes | Yes |
| `quark:red_sandstone_bricks_wall` | — | — | — | — | Yes | Yes |
| `quark:red_sandstone_vertical_slab` | — | — | — | — | Yes | Yes |
| `quark:red_shingles` | — | — | — | — | — | — |
| `quark:red_shingles_slab` | — | — | — | — | — | — |
| `quark:red_shingles_stairs` | — | — | — | — | — | — |
| `quark:red_shingles_vertical_slab` | — | — | — | — | — | — |
| `quark:red_stool` | Yes | Yes | Yes | Yes | — | — |
| `quark:redstone_randomizer` | — | — | — | — | — | — |
| `quark:rope` | Yes | Yes | Yes | Yes | Yes | Yes |
| `quark:rusty_iron_plate` | — | — | Yes | Yes | — | — |
| `quark:rusty_iron_plate_slab` | — | — | Yes | Yes | — | — |
| `quark:rusty_iron_plate_stairs` | — | — | Yes | Yes | — | — |
| `quark:rusty_iron_plate_vertical_slab` | — | — | Yes | Yes | — | — |
| `quark:sandstone_bricks` | — | — | Yes | Yes | Yes | — |
| `quark:sandstone_bricks_slab` | — | — | Yes | Yes | Yes | — |
| `quark:sandstone_bricks_stairs` | — | — | Yes | Yes | Yes | — |
| `quark:sandstone_bricks_vertical_slab` | — | — | Yes | Yes | Yes | — |
| `quark:sandstone_bricks_wall` | — | — | Yes | Yes | Yes | — |
| `quark:sandstone_vertical_slab` | — | — | Yes | Yes | Yes | — |
| `quark:shale` | — | — | Yes | Yes | Yes | Yes |
| `quark:shale_bricks` | — | — | Yes | Yes | Yes | Yes |
| `quark:shale_bricks_slab` | — | — | Yes | Yes | Yes | Yes |
| `quark:shale_bricks_stairs` | — | — | Yes | Yes | Yes | Yes |
| `quark:shale_bricks_vertical_slab` | — | — | Yes | Yes | Yes | Yes |
| `quark:shale_bricks_wall` | — | — | Yes | Yes | Yes | Yes |
| `quark:shale_pillar` | — | — | Yes | Yes | Yes | Yes |
| `quark:shale_slab` | — | — | Yes | Yes | Yes | Yes |
| `quark:shale_stairs` | — | — | Yes | Yes | Yes | Yes |
| `quark:shale_vertical_slab` | — | — | Yes | Yes | Yes | Yes |
| `quark:shale_wall` | — | — | Yes | Yes | Yes | Yes |
| `quark:shingles` | — | — | — | — | Yes | — |
| `quark:shingles_slab` | — | — | — | — | Yes | — |
| `quark:shingles_stairs` | — | — | — | — | Yes | — |
| `quark:shingles_vertical_slab` | — | — | — | — | Yes | — |
| `quark:smooth_quartz_vertical_slab` | — | — | — | — | Yes | Yes |
| `quark:smooth_red_sandstone_vertical_slab` | — | — | — | — | Yes | Yes |
| `quark:smooth_sandstone_vertical_slab` | — | — | Yes | Yes | Yes | — |
| `quark:smooth_soul_sandstone` | — | — | — | — | Yes | Yes |
| `quark:smooth_soul_sandstone_slab` | — | — | — | — | Yes | Yes |
| `quark:smooth_soul_sandstone_stairs` | — | — | — | — | Yes | Yes |
| `quark:smooth_soul_sandstone_vertical_slab` | — | — | — | — | Yes | Yes |
| `quark:smooth_stone_vertical_slab` | — | — | Yes | Yes | Yes | — |
| `quark:soul_sandstone` | — | — | — | — | Yes | Yes |
| `quark:soul_sandstone_bricks` | — | — | — | — | Yes | Yes |
| `quark:soul_sandstone_bricks_slab` | — | — | — | — | Yes | Yes |
| `quark:soul_sandstone_bricks_stairs` | — | — | — | — | Yes | Yes |
| `quark:soul_sandstone_bricks_vertical_slab` | — | — | — | — | Yes | Yes |
| `quark:soul_sandstone_bricks_wall` | — | — | — | — | Yes | Yes |
| `quark:soul_sandstone_slab` | — | — | — | — | Yes | Yes |
| `quark:soul_sandstone_stairs` | — | — | — | — | Yes | Yes |
| `quark:soul_sandstone_vertical_slab` | — | — | — | — | Yes | Yes |
| `quark:soul_sandstone_wall` | — | — | — | — | Yes | Yes |
| `quark:spruce_bookshelf` | — | — | — | Yes | — | — |
| `quark:spruce_chest` | — | — | — | Yes | — | — |
| `quark:spruce_hedge` | — | — | — | Yes | — | — |
| `quark:spruce_ladder` | — | — | — | Yes | — | — |
| `quark:spruce_leaf_carpet` | — | — | — | Yes | — | — |
| `quark:spruce_post` | — | — | — | Yes | — | — |
| `quark:spruce_trapped_chest` | — | — | — | Yes | — | — |
| `quark:spruce_vertical_slab` | — | — | — | Yes | — | — |
| `quark:stick_block` | Yes | Yes | Yes | Yes | Yes | Yes |
| `quark:stone_brick_lamp` | — | — | Yes | Yes | Yes | — |
| `quark:stone_brick_vertical_slab` | — | — | Yes | Yes | Yes | — |
| `quark:stone_lamp` | — | — | Yes | Yes | Yes | — |
| `quark:stone_vertical_slab` | — | — | Yes | Yes | Yes | — |
| `quark:stripped_acacia_post` | — | Yes | — | — | — | — |
| `quark:stripped_ancient_log` | Yes | Yes | Yes | Yes | Yes | Yes |
| `quark:stripped_ancient_post` | Yes | Yes | Yes | Yes | Yes | Yes |
| `quark:stripped_ancient_wood` | Yes | Yes | Yes | Yes | Yes | Yes |
| `quark:stripped_azalea_log` | Yes | — | — | — | — | — |
| `quark:stripped_azalea_post` | Yes | — | — | — | — | — |
| `quark:stripped_azalea_wood` | Yes | — | — | — | — | — |
| `quark:stripped_bamboo_post` | Yes | — | — | — | — | — |
| `quark:stripped_birch_post` | — | — | Yes | — | — | — |
| `quark:stripped_blossom_log` | Yes | Yes | Yes | Yes | Yes | Yes |
| `quark:stripped_blossom_post` | Yes | Yes | Yes | Yes | Yes | Yes |
| `quark:stripped_blossom_wood` | Yes | Yes | Yes | Yes | Yes | Yes |
| `quark:stripped_cherry_post` | Yes | — | — | — | — | — |
| `quark:stripped_crimson_post` | — | — | — | — | — | Yes |
| `quark:stripped_dark_oak_post` | Yes | — | — | — | — | — |
| `quark:stripped_jungle_post` | Yes | — | — | — | — | — |
| `quark:stripped_mangrove_post` | Yes | — | — | — | — | — |
| `quark:stripped_oak_post` | Yes | — | Yes | — | — | — |
| `quark:stripped_spruce_post` | — | — | — | Yes | — | — |
| `quark:stripped_warped_post` | — | — | — | — | — | Yes |
| `quark:sturdy_stone` | — | — | Yes | Yes | Yes | — |
| `quark:sugar_cane_block` | Yes | Yes | Yes | Yes | Yes | — |
| `quark:thatch` | Yes | Yes | Yes | — | — | — |
| `quark:thatch_slab` | Yes | Yes | Yes | — | — | — |
| `quark:thatch_stairs` | Yes | Yes | Yes | — | — | — |
| `quark:thatch_vertical_slab` | Yes | Yes | Yes | — | — | — |
| `quark:tiny_potato` | Yes | Yes | Yes | Yes | Yes | Yes |
| `quark:tuff_bricks` | — | — | — | Yes | Yes | — |
| `quark:tuff_bricks_slab` | — | — | — | Yes | Yes | — |
| `quark:tuff_bricks_stairs` | — | — | — | Yes | Yes | — |
| `quark:tuff_bricks_vertical_slab` | — | — | — | Yes | Yes | — |
| `quark:tuff_bricks_wall` | — | — | — | Yes | Yes | — |
| `quark:tuff_pillar` | — | — | — | Yes | Yes | — |
| `quark:tuff_slab` | — | — | — | Yes | Yes | — |
| `quark:tuff_stairs` | — | — | — | Yes | Yes | — |
| `quark:tuff_vertical_slab` | — | — | — | Yes | Yes | — |
| `quark:tuff_wall` | — | — | — | Yes | Yes | — |
| `quark:vertical_acacia_planks` | — | Yes | — | — | — | — |
| `quark:vertical_ancient_planks` | Yes | Yes | Yes | Yes | Yes | Yes |
| `quark:vertical_azalea_planks` | Yes | — | — | — | — | — |
| `quark:vertical_bamboo_planks` | Yes | — | — | — | — | — |
| `quark:vertical_birch_planks` | — | — | Yes | — | — | — |
| `quark:vertical_blossom_planks` | Yes | Yes | Yes | Yes | Yes | Yes |
| `quark:vertical_cherry_planks` | Yes | — | — | — | — | — |
| `quark:vertical_crimson_planks` | — | — | — | — | — | Yes |
| `quark:vertical_dark_oak_planks` | Yes | — | — | — | — | — |
| `quark:vertical_jungle_planks` | Yes | — | — | — | — | — |
| `quark:vertical_mangrove_planks` | Yes | — | — | — | — | — |
| `quark:vertical_oak_planks` | Yes | — | Yes | — | — | — |
| `quark:vertical_spruce_planks` | — | — | — | Yes | — | — |
| `quark:vertical_warped_planks` | — | — | — | — | — | Yes |
| `quark:violet_corundum` | — | — | Yes | Yes | Yes | Yes |
| `quark:violet_corundum_cluster` | — | — | Yes | Yes | Yes | Yes |
| `quark:violet_corundum_pane` | — | — | Yes | Yes | Yes | Yes |
| `quark:violet_crystal_lamp` | — | — | — | — | Yes | Yes |
| `quark:warped_bookshelf` | — | — | — | — | — | — |
| `quark:warped_chest` | — | — | — | — | — | Yes |
| `quark:warped_ladder` | — | — | — | — | — | Yes |
| `quark:warped_post` | — | — | — | — | — | Yes |
| `quark:warped_trapped_chest` | — | — | — | — | — | Yes |
| `quark:warped_vertical_slab` | — | — | — | — | — | Yes |
| `quark:water_pink_petals` | Yes | Yes | Yes | Yes | Yes | Yes |
| `quark:waxed_black_corundum` | — | — | Yes | — | — | — |
| `quark:waxed_blue_corundum` | — | — | Yes | — | — | — |
| `quark:waxed_cut_copper_vertical_slab` | — | — | Yes | — | — | — |
| `quark:waxed_exposed_cut_copper_vertical_slab` | — | — | Yes | — | — | — |
| `quark:waxed_green_corundum` | — | — | Yes | — | — | — |
| `quark:waxed_indigo_corundum` | — | — | Yes | — | — | — |
| `quark:waxed_orange_corundum` | — | — | Yes | — | — | — |
| `quark:waxed_oxidized_cut_copper_vertical_slab` | — | — | Yes | — | — | — |
| `quark:waxed_red_corundum` | — | — | Yes | — | — | — |
| `quark:waxed_violet_corundum` | — | — | Yes | — | — | — |
| `quark:waxed_weathered_cut_copper_vertical_slab` | — | — | Yes | — | — | — |
| `quark:waxed_white_corundum` | — | — | Yes | — | — | — |
| `quark:waxed_yellow_corundum` | — | — | Yes | — | — | — |
| `quark:weathered_cut_copper_vertical_slab` | — | — | Yes | Yes | Yes | Yes |
| `quark:white_corundum` | — | — | Yes | Yes | Yes | Yes |
| `quark:white_corundum_cluster` | — | — | Yes | Yes | Yes | Yes |
| `quark:white_corundum_pane` | — | — | Yes | Yes | Yes | Yes |
| `quark:white_crystal_lamp` | — | — | — | — | Yes | Yes |
| `quark:white_framed_glass` | — | — | Yes | Yes | — | — |
| `quark:white_framed_glass_pane` | — | — | Yes | Yes | — | — |
| `quark:white_shingles` | — | — | — | — | Yes | — |
| `quark:white_shingles_slab` | — | — | — | — | Yes | — |
| `quark:white_shingles_stairs` | — | — | — | — | Yes | — |
| `quark:white_shingles_vertical_slab` | — | — | — | — | Yes | — |
| `quark:white_stool` | Yes | Yes | Yes | Yes | Yes | Yes |
| `quark:yellow_blossom_hedge` | Yes | Yes | Yes | Yes | Yes | Yes |
| `quark:yellow_blossom_leaf_carpet` | Yes | Yes | Yes | Yes | Yes | Yes |
| `quark:yellow_blossom_leaves` | Yes | Yes | Yes | Yes | Yes | Yes |
| `quark:yellow_blossom_sapling` | Yes | Yes | Yes | Yes | Yes | Yes |
| `quark:yellow_corundum` | — | — | Yes | Yes | Yes | Yes |
| `quark:yellow_corundum_cluster` | — | — | Yes | Yes | Yes | Yes |
| `quark:yellow_corundum_pane` | — | — | Yes | Yes | Yes | Yes |
| `quark:yellow_crystal_lamp` | — | — | — | — | Yes | Yes |
| `quark:yellow_framed_glass` | — | — | Yes | Yes | — | — |
| `quark:yellow_framed_glass_pane` | — | — | Yes | Yes | — | — |
| `quark:yellow_shingles` | — | — | — | — | Yes | — |
| `quark:yellow_shingles_slab` | — | — | — | — | Yes | — |
| `quark:yellow_shingles_stairs` | — | — | — | — | Yes | — |
| `quark:yellow_shingles_vertical_slab` | — | — | — | — | Yes | — |
| `quark:yellow_stool` | Yes | Yes | Yes | Yes | Yes | Yes |
| `snowrealmagic:fence` | — | — | — | Yes | — | — |
| `snowrealmagic:fence2` | — | — | — | Yes | — | — |
| `snowrealmagic:fence_gate` | — | — | — | Yes | — | — |
| `snowrealmagic:slab` | — | — | — | Yes | — | — |
| `snowrealmagic:snow` | — | — | — | Yes | — | — |
| `snowrealmagic:stairs` | — | — | — | Yes | — | — |
| `snowrealmagic:wall` | — | — | — | Yes | — | — |

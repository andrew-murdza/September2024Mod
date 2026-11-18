Historical source audit; removed asset rows have been omitted. Counts below describe the earlier audit, not this revised archive.

# Texture inventory — updated 7 November 2026

The available project evidence verifies **65 PNG textures created or reworked for this mod: 36 item textures, 23 block textures, and 6 mob/entity textures**. One of those mob textures, the Spider Queen edit, remains in the repository but is no longer used by its renderer. There are also **4 texture resources derived at runtime**: the two tropical azalea leaf palettes, the portal animation, and the unspotted stingray.

This is an attribution audit, not a claim that every custom-namespaced texture was drawn by the assistant. Of the **244 texture PNGs currently in `src/main/resources/assets`**, **126 match supplied or upstream artwork** and **53 have unverified authorship**. The list below accounts for every one of those 244 files. Runtime azalea resources share paths with two source-matched fallback PNGs, so the four runtime resources must not simply be added to the PNG count.

The first 7 November refresh found two additional Orcz texture recolors introduced on 6 November. This follow-up moves those atlases from `orcz/textures/entities` to `aoemod/textures/entity/orcz`, recolors 60 additional shield-edge pixels, and adds eight blueberry bush PNGs: four berry-stage recolors and four unchanged young-stage images. All 233 textures from the original inventory still retain their exact SHA-256 hashes; the two old Orcz paths have been replaced by their new paths. A later 7 November revision adds the restored Orcz boss atlas, with isolated sword UVs and a bone recolor.

“Created or reworked” includes original generated artwork, pixel edits, recolors, and source-based composites. It does not imply ownership of the underlying upstream artwork. Historical drafts, preview sheets, removed files, and model-only changes are not counted as additional current textures. Available history cannot establish a complete lifetime list of every texture ever created in previous conversations.

## Verified mob textures

| Texture | What was created or changed | Evidence |
| --- | --- | --- |
| [aoemod/textures/entity/arthropleura.png](src/main/resources/assets/aoemod/textures/entity/arthropleura.png) | Original generated material sheet projected onto the new model atlas; the older mod atlas was a visual reference only. | [Generation and projection record](tmp/seafood-refinements/arthropleura-artwork/projection.json) |
| [aoemod/textures/entity/cephalopod/arrow_squid/arrow_squid.png](src/main/resources/assets/aoemod/textures/entity/cephalopod/arrow_squid/arrow_squid.png) | Edited Hybrid Aquatic atlas: generated mantle/head colors and an eye patch, retaining native UV islands and other pixels. | [Artwork manifest](tmp/seafood-32/artwork-manifest.json) |
| [aoemod/textures/entity/spider_queen.png](src/main/resources/assets/aoemod/textures/entity/spider_queen.png) | Twilight Forest king-spider recolor removing gold/orange bands. **Retired:** the current renderer uses vanilla spider artwork. | [Edit record](tmp/spider-queen-qa/texture-edit.txt), [current renderer](src/main/java/net/amurdza/examplemod/entity/render/SpiderQueenRenderer.java) |
| [aoemod/textures/entity/orcz/orc_savage.png](src/main/resources/assets/aoemod/textures/entity/orcz/orc_savage.png) | Native Orcz iron gear recolored to ivory bone: 512 pixels changed, preserving all other pixels and transparency. | [Recolor script](scripts/recolor-orc-bone-gear.ps1), [current pixel audit](tmp/orc-bone-gear-assets/pixel-audit.json), [6 November audit](tmp/nov6-snowy-orc-assets/pixel-audit.json) |
| [aoemod/textures/entity/orcz/orc_savage_shield.png](src/main/resources/assets/aoemod/textures/entity/orcz/orc_savage_shield.png) | Native Orcz shield-bearing mob gear recolored to ivory bone: 1,880 pixels changed, including the 60-pixel edge correction to the earlier 1,820-pixel edit. All other pixels and transparency are preserved. | [Recolor script](scripts/recolor-orc-bone-gear.ps1), [current pixel audit](tmp/orc-bone-gear-assets/pixel-audit.json), [6 November audit](tmp/nov6-snowy-orc-assets/pixel-audit.json) |
| [aoemod/textures/entity/orcz/orc_savage_big_2sword.png](src/main/resources/assets/aoemod/textures/entity/orcz/orc_savage_big_2sword.png) | Native Orcz boss swords recolored to ivory bone: 1,151 metal pixels. The sword UV island shared with head details is copied into unused atlas space (775 visible source pixels), preserving all head/body pixels, sword geometry, and source alpha. | [Recolor and UV-copy script](scripts/recolor-orc-boss-swords.ps1), [pixel audit](tmp/orc-boss-swords/pixel-audit.json), [native sword UV redirect](src/main/java/net/amurdza/examplemod/mixins/othermods/orcz/BoneBossSwordUV.java) |

The stingray recolor is listed separately below because it is generated in memory rather than stored as a production PNG.

## Verified item textures

These 36 files include the generated seafood artwork and its subsequent pixel/palette edits. The raw/cooked pairs are separate textures. Some retain an upstream silhouette; they are adaptations rather than wholly original drawings.

| Texture | Work and evidence |
| --- | --- |
| [aoemod/textures/item/cooked_anglerfish.png](src/main/resources/assets/aoemod/textures/item/cooked_anglerfish.png) | Generated/revised seafood artwork and matched raw/cooked palette work; [artwork manifest](tmp/seafood-refinements/artwork-manifest.json), [generation records](tmp/seafood-32/artwork-manifest.json). |
| [aoemod/textures/item/cooked_anomalocaris.png](src/main/resources/assets/aoemod/textures/item/cooked_anomalocaris.png) | Generated/revised seafood artwork and matched raw/cooked palette work; [artwork manifest](tmp/seafood-refinements/artwork-manifest.json), [generation records](tmp/seafood-32/artwork-manifest.json). |
| [aoemod/textures/item/cooked_arrow_squid.png](src/main/resources/assets/aoemod/textures/item/cooked_arrow_squid.png) | Generated/revised seafood artwork and matched raw/cooked palette work; [artwork manifest](tmp/seafood-refinements/artwork-manifest.json), [generation records](tmp/seafood-32/artwork-manifest.json). |
| [aoemod/textures/item/cooked_blue_spotted_stingray.png](src/main/resources/assets/aoemod/textures/item/cooked_blue_spotted_stingray.png) | Generated/revised seafood artwork and matched raw/cooked palette work; [artwork manifest](tmp/seafood-refinements/artwork-manifest.json), [generation records](tmp/seafood-32/artwork-manifest.json). |
| [aoemod/textures/item/cooked_carp.png](src/main/resources/assets/aoemod/textures/item/cooked_carp.png) | Approved 16px food artwork retained through later revisions; flying fish preserves the native source silhouette; [revision notes](IMPLEMENTED_CHANGES.md), [approved hashes and source-mask check](scripts/validate-food-textures.py). |
| [aoemod/textures/item/cooked_crab_meat.png](src/main/resources/assets/aoemod/textures/item/cooked_crab_meat.png) | Cooked palette edit preserving the native source silhouette; [artwork manifest](tmp/seafood-refinements/artwork-manifest.json), [mask validation](scripts/validate-food-textures.py). |
| [aoemod/textures/item/cooked_flying_fish.png](src/main/resources/assets/aoemod/textures/item/cooked_flying_fish.png) | Approved 16px food artwork retained through later revisions; flying fish preserves the native source silhouette; [revision notes](IMPLEMENTED_CHANGES.md), [approved hashes and source-mask check](scripts/validate-food-textures.py). |
| [aoemod/textures/item/cooked_mahi_mahi.png](src/main/resources/assets/aoemod/textures/item/cooked_mahi_mahi.png) | Approved 16px food artwork retained through later revisions; flying fish preserves the native source silhouette; [revision notes](IMPLEMENTED_CHANGES.md), [approved hashes and source-mask check](scripts/validate-food-textures.py). |
| [aoemod/textures/item/cooked_octopus.png](src/main/resources/assets/aoemod/textures/item/cooked_octopus.png) | Generated/revised seafood artwork and matched raw/cooked palette work; [artwork manifest](tmp/seafood-refinements/artwork-manifest.json), [generation records](tmp/seafood-32/artwork-manifest.json). |
| [aoemod/textures/item/cooked_rubber_belly_glider.png](src/main/resources/assets/aoemod/textures/item/cooked_rubber_belly_glider.png) | Generated/revised seafood artwork and matched raw/cooked palette work; [artwork manifest](tmp/seafood-refinements/artwork-manifest.json), [generation records](tmp/seafood-32/artwork-manifest.json). |
| [aoemod/textures/item/cooked_starfish.png](src/main/resources/assets/aoemod/textures/item/cooked_starfish.png) | Cooked palette edit preserving the native source silhouette; [artwork manifest](tmp/seafood-refinements/artwork-manifest.json), [mask validation](scripts/validate-food-textures.py). |
| [aoemod/textures/item/cooked_swamp_mucker.png](src/main/resources/assets/aoemod/textures/item/cooked_swamp_mucker.png) | Approved 16px food artwork retained through later revisions; flying fish preserves the native source silhouette; [revision notes](IMPLEMENTED_CHANGES.md), [approved hashes and source-mask check](scripts/validate-food-textures.py). |
| [aoemod/textures/item/cooked_triops.png](src/main/resources/assets/aoemod/textures/item/cooked_triops.png) | Generated/revised seafood artwork and matched raw/cooked palette work; [artwork manifest](tmp/seafood-refinements/artwork-manifest.json), [generation records](tmp/seafood-32/artwork-manifest.json). |
| [aoemod/textures/item/cooked_tuna.png](src/main/resources/assets/aoemod/textures/item/cooked_tuna.png) | Cooked palette edit with the native tuna silhouette; [artwork record](tmp/seafood-resolution-review/tuna-artwork.json). |
| [aoemod/textures/item/cooked_umbrella_octopus.png](src/main/resources/assets/aoemod/textures/item/cooked_umbrella_octopus.png) | Generated/revised seafood artwork and matched raw/cooked palette work; [artwork manifest](tmp/seafood-refinements/artwork-manifest.json), [generation records](tmp/seafood-32/artwork-manifest.json). |
| [aoemod/textures/item/cooked_vampire_squid.png](src/main/resources/assets/aoemod/textures/item/cooked_vampire_squid.png) | Generated seafood sprite with a later tentacle refinement; [follow-up artwork record](tmp/vampire-tentacle-followup/artwork-manifest.json). |
| [aoemod/textures/item/plum.png](src/main/resources/assets/aoemod/textures/item/plum.png) | Custom pixel sprite using plum-pie colors; [pixel script](scripts/normalize-food-textures.py). |
| [aoemod/textures/item/raw_anglerfish.png](src/main/resources/assets/aoemod/textures/item/raw_anglerfish.png) | Generated/revised seafood artwork and matched raw/cooked palette work; [artwork manifest](tmp/seafood-refinements/artwork-manifest.json), [generation records](tmp/seafood-32/artwork-manifest.json). |
| [aoemod/textures/item/raw_anomalocaris.png](src/main/resources/assets/aoemod/textures/item/raw_anomalocaris.png) | Generated/revised seafood artwork and matched raw/cooked palette work; [artwork manifest](tmp/seafood-refinements/artwork-manifest.json), [generation records](tmp/seafood-32/artwork-manifest.json). |
| [aoemod/textures/item/raw_arrow_squid.png](src/main/resources/assets/aoemod/textures/item/raw_arrow_squid.png) | Generated/revised seafood artwork and matched raw/cooked palette work; [artwork manifest](tmp/seafood-refinements/artwork-manifest.json), [generation records](tmp/seafood-32/artwork-manifest.json). |
| [aoemod/textures/item/raw_blue_spotted_stingray.png](src/main/resources/assets/aoemod/textures/item/raw_blue_spotted_stingray.png) | Generated/revised seafood artwork and matched raw/cooked palette work; [artwork manifest](tmp/seafood-refinements/artwork-manifest.json), [generation records](tmp/seafood-32/artwork-manifest.json). |
| [aoemod/textures/item/raw_mahi_mahi.png](src/main/resources/assets/aoemod/textures/item/raw_mahi_mahi.png) | Source-based custom palette edit; [replacement script](tmp/texture-references/replace-food-placeholders.py). |
| [aoemod/textures/item/raw_octopus.png](src/main/resources/assets/aoemod/textures/item/raw_octopus.png) | Generated/revised seafood artwork and matched raw/cooked palette work; [artwork manifest](tmp/seafood-refinements/artwork-manifest.json), [generation records](tmp/seafood-32/artwork-manifest.json). |
| [aoemod/textures/item/raw_rubber_belly_glider.png](src/main/resources/assets/aoemod/textures/item/raw_rubber_belly_glider.png) | Generated/revised seafood artwork and matched raw/cooked palette work; [artwork manifest](tmp/seafood-refinements/artwork-manifest.json), [generation records](tmp/seafood-32/artwork-manifest.json). |
| [aoemod/textures/item/raw_triops.png](src/main/resources/assets/aoemod/textures/item/raw_triops.png) | Generated/revised seafood artwork and matched raw/cooked palette work; [artwork manifest](tmp/seafood-refinements/artwork-manifest.json), [generation records](tmp/seafood-32/artwork-manifest.json). |
| [aoemod/textures/item/raw_umbrella_octopus.png](src/main/resources/assets/aoemod/textures/item/raw_umbrella_octopus.png) | Generated/revised seafood artwork and matched raw/cooked palette work; [artwork manifest](tmp/seafood-refinements/artwork-manifest.json), [generation records](tmp/seafood-32/artwork-manifest.json). |
| [aoemod/textures/item/raw_vampire_squid.png](src/main/resources/assets/aoemod/textures/item/raw_vampire_squid.png) | Generated seafood sprite with a later tentacle refinement; [follow-up artwork record](tmp/vampire-tentacle-followup/artwork-manifest.json). |
| [aoemod/textures/item/soul_beetroot_soup.png](src/main/resources/assets/aoemod/textures/item/soul_beetroot_soup.png) | Vanilla beetroot-soup bowl with the soul-beet flesh palette; [pixel script](scripts/normalize-food-textures.py). |

## Verified block textures

Soul-soil quartz ore was recolored and later composited with the soul-sand quartz pixels over the pristine soul-soil background. All 18 corn stage/section textures were recolored to lusher green foliage while preserving kernel/tassel colors and transparency. These are edits to existing artwork. [Recorded changes](DIMENSIONS.md), [earlier quartz edit record](build/assets_changes.md).

The four ripe blueberry stages recolor only red berries from Minecraft 1.20.1 and Snow! Real Magic 10.5.2 artwork using the five blue colors from Fruits Delight's blueberry item. Leaves, snow, silhouettes and alpha remain unchanged. Their four young-stage companions are unchanged upstream artwork and appear in the source-matched section. [Reproduction script](scripts/generate-blueberry-textures.ps1), [pixel audit](tmp/blueberry-textures/pixel-audit.json).

| Texture | Work |
| --- | --- |
| [aoemod/textures/block/blue_berry_bush_stage2.png](src/main/resources/assets/aoemod/textures/block/blue_berry_bush_stage2.png) | Vanilla sweet-berry stage 2: 14 red berry pixels recolored blue |
| [aoemod/textures/block/blue_berry_bush_stage3.png](src/main/resources/assets/aoemod/textures/block/blue_berry_bush_stage3.png) | Vanilla sweet-berry stage 3: 37 red berry pixels recolored blue |
| [aoemod/textures/block/snowy_blue_berry_bush_stage2.png](src/main/resources/assets/aoemod/textures/block/snowy_blue_berry_bush_stage2.png) | Snow! Real Magic sweet-berry stage 2: 13 red berry pixels recolored blue |
| [aoemod/textures/block/snowy_blue_berry_bush_stage3.png](src/main/resources/assets/aoemod/textures/block/snowy_blue_berry_bush_stage3.png) | Snow! Real Magic sweet-berry stage 3: 31 red berry pixels recolored blue |
| [aoemod/textures/block/soul_soil_quartz_ore.png](src/main/resources/assets/aoemod/textures/block/soul_soil_quartz_ore.png) | Quartz palette/composite edit |
| [hearthandharvest/textures/block/corn_bottom_stage_0.png](src/main/resources/assets/hearthandharvest/textures/block/corn_bottom_stage_0.png) | Corn foliage palette edit |
| [hearthandharvest/textures/block/corn_bottom_stage_1.png](src/main/resources/assets/hearthandharvest/textures/block/corn_bottom_stage_1.png) | Corn foliage palette edit |
| [hearthandharvest/textures/block/corn_bottom_stage_2.png](src/main/resources/assets/hearthandharvest/textures/block/corn_bottom_stage_2.png) | Corn foliage palette edit |
| [hearthandharvest/textures/block/corn_bottom_stage_3.png](src/main/resources/assets/hearthandharvest/textures/block/corn_bottom_stage_3.png) | Corn foliage palette edit |
| [hearthandharvest/textures/block/corn_bottom_stage_4.png](src/main/resources/assets/hearthandharvest/textures/block/corn_bottom_stage_4.png) | Corn foliage palette edit |
| [hearthandharvest/textures/block/corn_bottom_stage_5.png](src/main/resources/assets/hearthandharvest/textures/block/corn_bottom_stage_5.png) | Corn foliage palette edit |
| [hearthandharvest/textures/block/corn_middle_stage_0.png](src/main/resources/assets/hearthandharvest/textures/block/corn_middle_stage_0.png) | Corn foliage palette edit |
| [hearthandharvest/textures/block/corn_middle_stage_1.png](src/main/resources/assets/hearthandharvest/textures/block/corn_middle_stage_1.png) | Corn foliage palette edit |
| [hearthandharvest/textures/block/corn_middle_stage_2.png](src/main/resources/assets/hearthandharvest/textures/block/corn_middle_stage_2.png) | Corn foliage palette edit |
| [hearthandharvest/textures/block/corn_middle_stage_3.png](src/main/resources/assets/hearthandharvest/textures/block/corn_middle_stage_3.png) | Corn foliage palette edit |
| [hearthandharvest/textures/block/corn_middle_stage_4.png](src/main/resources/assets/hearthandharvest/textures/block/corn_middle_stage_4.png) | Corn foliage palette edit |
| [hearthandharvest/textures/block/corn_middle_stage_5.png](src/main/resources/assets/hearthandharvest/textures/block/corn_middle_stage_5.png) | Corn foliage palette edit |
| [hearthandharvest/textures/block/corn_top_stage_0.png](src/main/resources/assets/hearthandharvest/textures/block/corn_top_stage_0.png) | Corn foliage palette edit |
| [hearthandharvest/textures/block/corn_top_stage_1.png](src/main/resources/assets/hearthandharvest/textures/block/corn_top_stage_1.png) | Corn foliage palette edit |
| [hearthandharvest/textures/block/corn_top_stage_2.png](src/main/resources/assets/hearthandharvest/textures/block/corn_top_stage_2.png) | Corn foliage palette edit |
| [hearthandharvest/textures/block/corn_top_stage_3.png](src/main/resources/assets/hearthandharvest/textures/block/corn_top_stage_3.png) | Corn foliage palette edit |
| [hearthandharvest/textures/block/corn_top_stage_4.png](src/main/resources/assets/hearthandharvest/textures/block/corn_top_stage_4.png) | Corn foliage palette edit |
| [hearthandharvest/textures/block/corn_top_stage_5.png](src/main/resources/assets/hearthandharvest/textures/block/corn_top_stage_5.png) | Corn foliage palette edit |

## Textures derived at runtime

| Resource | Work and evidence |
| --- | --- |
| `aoemod:textures/block/tropical_azalea_leaves.png` | Tropical palette derived from installed vanilla leaves, preserving geometry and alpha. The on-disk fallback PNG is unmodified vanilla artwork. [Implementation](src/main/java/net/amurdza/examplemod/client/TropicalAzaleaTexturePack.java). |
| `aoemod:textures/block/tropical_flowering_azalea_leaves.png` | Same tropical palette treatment, preserving the pink flower pixels. The on-disk fallback PNG is unmodified vanilla artwork. [Implementation](src/main/java/net/amurdza/examplemod/client/TropicalAzaleaTexturePack.java). |
| `aoemod:textures/block/wooden_portal.png` | Neutral palette derived from the installed vanilla portal animation; destination colors come from block tint. [Implementation](src/main/java/net/amurdza/examplemod/client/PortalTexturePack.java). |
| `ambientadditions:textures/entity/blue_spotted_stingray.png` | Native stingray texture edited in memory to remove the blue spots. [Implementation](src/main/java/net/amurdza/examplemod/client/StingrayTexturePack.java). |

## Supplied or upstream artwork — not counted as assistant-created

The following 126 PNGs match an available source archive, either byte-for-byte or as identical decoded RGBA pixels. “Matches” establishes the artwork relationship, not who originally authored a supplied ZIP or the precise historical import route. A filename under `aoemod` does not make an upstream image newly created. Equipment/shield/armor textures are included for completeness.

| Texture | Matching source |
| --- | --- |
| [aoemod/textures/block/azalea_sapling.png](src/main/resources/assets/aoemod/textures/block/azalea_sapling.png) | `assets_lendal mods2-v8.zip` → `modid/textures/block/azalea_sapling.png` (identical PNG) |
| [aoemod/textures/block/blackstone_gold_ore_side.png](src/main/resources/assets/aoemod/textures/block/blackstone_gold_ore_side.png) | `assets_lendal mods2-v8.zip` → `modid/textures/block/blackstone_gold_ore.png` (identical PNG) |
| [aoemod/textures/block/blackstone_gold_ore_top.png](src/main/resources/assets/aoemod/textures/block/blackstone_gold_ore_top.png) | `assets_lendal mods2-v8.zip` → `modid/textures/block/blackstone_gold_ore_top.png` (identical PNG) |
| [aoemod/textures/block/blackstone_quartz_ore_side.png](src/main/resources/assets/aoemod/textures/block/blackstone_quartz_ore_side.png) | `assets_lendal mods2-v8.zip` → `modid/textures/block/blackstone_quartz_ore.png` (identical PNG) |
| [aoemod/textures/block/blackstone_quartz_ore_top.png](src/main/resources/assets/aoemod/textures/block/blackstone_quartz_ore_top.png) | `assets_lendal mods2-v8.zip` → `modid/textures/block/blackstone_quartz_ore_top.png` (identical PNG) |
| [aoemod/textures/block/blue_berry_bush_stage0.png](src/main/resources/assets/aoemod/textures/block/blue_berry_bush_stage0.png) | Minecraft 1.20.1 `client.jar` → `minecraft/textures/block/sweet_berry_bush_stage0.png` (identical RGBA pixels; [audit](tmp/blueberry-textures/pixel-audit.json)) |
| [aoemod/textures/block/blue_berry_bush_stage1.png](src/main/resources/assets/aoemod/textures/block/blue_berry_bush_stage1.png) | Minecraft 1.20.1 `client.jar` → `minecraft/textures/block/sweet_berry_bush_stage1.png` (identical RGBA pixels; [audit](tmp/blueberry-textures/pixel-audit.json)) |
| [aoemod/textures/block/closed_eyeblossom.png](src/main/resources/assets/aoemod/textures/block/closed_eyeblossom.png) | `minecraft-26.3-client.jar` → `minecraft/textures/block/closed_eyeblossom.png` (identical PNG) |
| [aoemod/textures/block/open_eyeblossom_emissive.png](src/main/resources/assets/aoemod/textures/block/open_eyeblossom_emissive.png) | `minecraft-26.3-client.jar` → `minecraft/textures/block/open_eyeblossom_emissive.png` (identical PNG) |
| [aoemod/textures/block/pale_hanging_moss.png](src/main/resources/assets/aoemod/textures/block/pale_hanging_moss.png) | `minecraft-26.3-client.jar` → `minecraft/textures/block/pale_hanging_moss.png` (identical PNG) |
| [aoemod/textures/block/pale_hanging_moss_tip.png](src/main/resources/assets/aoemod/textures/block/pale_hanging_moss_tip.png) | `minecraft-26.3-client.jar` → `minecraft/textures/block/pale_hanging_moss_tip.png` (identical PNG) |
| [aoemod/textures/block/pale_moss_block.png](src/main/resources/assets/aoemod/textures/block/pale_moss_block.png) | `minecraft-26.3-client.jar` → `minecraft/textures/block/pale_moss_block.png` (identical PNG) |
| [aoemod/textures/block/pale_moss_carpet.png](src/main/resources/assets/aoemod/textures/block/pale_moss_carpet.png) | `minecraft-26.3-client.jar` → `minecraft/textures/block/pale_moss_block.png` (identical PNG) |
| [aoemod/textures/block/pale_moss_carpet_side_small.png](src/main/resources/assets/aoemod/textures/block/pale_moss_carpet_side_small.png) | `minecraft-26.3-client.jar` → `minecraft/textures/block/pale_moss_carpet_side_small.png` (identical PNG) |
| [aoemod/textures/block/pale_moss_carpet_side_tall.png](src/main/resources/assets/aoemod/textures/block/pale_moss_carpet_side_tall.png) | `minecraft-26.3-client.jar` → `minecraft/textures/block/pale_moss_carpet_side_tall.png` (identical PNG) |
| [aoemod/textures/block/pale_oak_door_bottom.png](src/main/resources/assets/aoemod/textures/block/pale_oak_door_bottom.png) | `minecraft-26.3-client.jar` → `minecraft/textures/block/pale_oak_door_bottom.png` (identical PNG) |
| [aoemod/textures/block/pale_oak_door_top.png](src/main/resources/assets/aoemod/textures/block/pale_oak_door_top.png) | `minecraft-26.3-client.jar` → `minecraft/textures/block/pale_oak_door_top.png` (identical PNG) |
| [aoemod/textures/block/pale_oak_leaves.png](src/main/resources/assets/aoemod/textures/block/pale_oak_leaves.png) | `minecraft-26.3-client.jar` → `minecraft/textures/block/pale_oak_leaves.png` (identical PNG) |
| [aoemod/textures/block/pale_oak_log.png](src/main/resources/assets/aoemod/textures/block/pale_oak_log.png) | `minecraft-26.3-client.jar` → `minecraft/textures/block/pale_oak_log.png` (identical PNG) |
| [aoemod/textures/block/pale_oak_log_top.png](src/main/resources/assets/aoemod/textures/block/pale_oak_log_top.png) | `minecraft-26.3-client.jar` → `minecraft/textures/block/pale_oak_log_top.png` (identical PNG) |
| [aoemod/textures/block/pale_oak_planks.png](src/main/resources/assets/aoemod/textures/block/pale_oak_planks.png) | `minecraft-26.3-client.jar` → `minecraft/textures/block/pale_oak_planks.png` (identical PNG) |
| [aoemod/textures/block/pale_oak_sapling.png](src/main/resources/assets/aoemod/textures/block/pale_oak_sapling.png) | `minecraft-26.3-client.jar` → `minecraft/textures/block/pale_oak_sapling.png` (identical PNG) |
| [aoemod/textures/block/pale_oak_trapdoor.png](src/main/resources/assets/aoemod/textures/block/pale_oak_trapdoor.png) | `minecraft-26.3-client.jar` → `minecraft/textures/block/pale_oak_trapdoor.png` (identical PNG) |
| [aoemod/textures/block/snowy_blue_berry_bush_stage0.png](src/main/resources/assets/aoemod/textures/block/snowy_blue_berry_bush_stage0.png) | `snow-real-magic-308663-5801189.jar` → `snowrealmagic/textures/block/snowy_sweet_berry_bush_stage0.png` (identical RGBA pixels; [audit](tmp/blueberry-textures/pixel-audit.json)) |
| [aoemod/textures/block/snowy_blue_berry_bush_stage1.png](src/main/resources/assets/aoemod/textures/block/snowy_blue_berry_bush_stage1.png) | `snow-real-magic-308663-5801189.jar` → `snowrealmagic/textures/block/snowy_sweet_berry_bush_stage1.png` (identical RGBA pixels; [audit](tmp/blueberry-textures/pixel-audit.json)) |
| [aoemod/textures/block/soul_sand_gold_ore.png](src/main/resources/assets/aoemod/textures/block/soul_sand_gold_ore.png) | `assets_lendal mods2-v8.zip` → `modid/textures/block/soul_sand_gold_ore.png` (identical PNG) |
| [aoemod/textures/block/soul_sand_quartz_ore.png](src/main/resources/assets/aoemod/textures/block/soul_sand_quartz_ore.png) | `assets_lendal mods2-v8.zip` → `modid/textures/block/soul_sand_quartz_ore.png` (identical PNG) |
| [aoemod/textures/block/stripped_pale_oak_log.png](src/main/resources/assets/aoemod/textures/block/stripped_pale_oak_log.png) | `minecraft-26.3-client.jar` → `minecraft/textures/block/stripped_pale_oak_log.png` (identical PNG) |
| [aoemod/textures/block/stripped_pale_oak_log_top.png](src/main/resources/assets/aoemod/textures/block/stripped_pale_oak_log_top.png) | `minecraft-26.3-client.jar` → `minecraft/textures/block/stripped_pale_oak_log_top.png` (identical PNG) |
| [aoemod/textures/block/tropical_azalea_leaves.png](src/main/resources/assets/aoemod/textures/block/tropical_azalea_leaves.png) | `minecraft-26.3-client.jar` → `minecraft/textures/block/azalea_leaves.png` (identical RGBA pixels) |
| [aoemod/textures/block/tropical_flowering_azalea_leaves.png](src/main/resources/assets/aoemod/textures/block/tropical_flowering_azalea_leaves.png) | `minecraft-26.3-client.jar` → `minecraft/textures/block/flowering_azalea_leaves.png` (identical RGBA pixels) |
| [aoemod/textures/entity/baby_dolphin/dolphin_baby.png](src/main/resources/assets/aoemod/textures/entity/baby_dolphin/dolphin_baby.png) | `assets_lendal mods2-v8.zip` → `modid/textures/entity/baby_dolphin/dolphin_baby.png` (identical PNG) |
| [aoemod/textures/entity/baby_parrot/parrot_blue.png](src/main/resources/assets/aoemod/textures/entity/baby_parrot/parrot_blue.png) | `minecraft-26.3-client.jar` → `minecraft/textures/entity/parrot/parrot_blue.png` (identical RGBA pixels) |
| [aoemod/textures/entity/baby_parrot/parrot_green.png](src/main/resources/assets/aoemod/textures/entity/baby_parrot/parrot_green.png) | `minecraft-26.3-client.jar` → `minecraft/textures/entity/parrot/parrot_green.png` (identical RGBA pixels) |
| [aoemod/textures/entity/baby_parrot/parrot_grey.png](src/main/resources/assets/aoemod/textures/entity/baby_parrot/parrot_grey.png) | `minecraft-26.3-client.jar` → `minecraft/textures/entity/parrot/parrot_grey.png` (identical RGBA pixels) |
| [aoemod/textures/entity/baby_parrot/parrot_red_blue.png](src/main/resources/assets/aoemod/textures/entity/baby_parrot/parrot_red_blue.png) | `minecraft-26.3-client.jar` → `minecraft/textures/entity/parrot/parrot_red_blue.png` (identical RGBA pixels) |
| [aoemod/textures/entity/baby_parrot/parrot_yellow_blue.png](src/main/resources/assets/aoemod/textures/entity/baby_parrot/parrot_yellow_blue.png) | `minecraft-26.3-client.jar` → `minecraft/textures/entity/parrot/parrot_yellow_blue.png` (identical RGBA pixels) |
| [aoemod/textures/entity/bogged.png](src/main/resources/assets/aoemod/textures/entity/bogged.png) | `minecraft-26.3-client.jar` → `minecraft/textures/entity/skeleton/bogged.png` (identical PNG) |
| [aoemod/textures/entity/bogged_overlay.png](src/main/resources/assets/aoemod/textures/entity/bogged_overlay.png) | `minecraft-26.3-client.jar` → `minecraft/textures/entity/skeleton/bogged_overlay.png` (identical PNG) |
| [aoemod/textures/entity/boss_shield.png](src/main/resources/assets/aoemod/textures/entity/boss_shield.png) | `assets_lendal mods2-v8.zip` → `modid/textures/entity/boss_shield.png` (identical PNG) |
| [aoemod/textures/entity/breeze.png](src/main/resources/assets/aoemod/textures/entity/breeze.png) | `minecraft-26.3-client.jar` → `minecraft/textures/entity/breeze/breeze.png` (identical PNG) |
| [aoemod/textures/entity/breeze_wind.png](src/main/resources/assets/aoemod/textures/entity/breeze_wind.png) | `minecraft-26.3-client.jar` → `minecraft/textures/entity/breeze/breeze_wind.png` (identical PNG) |
| [aoemod/textures/entity/camel_husk.png](src/main/resources/assets/aoemod/textures/entity/camel_husk.png) | `minecraft-26.3-client.jar` → `minecraft/textures/entity/camel/camel_husk.png` (identical PNG) |
| [aoemod/textures/entity/creaking.png](src/main/resources/assets/aoemod/textures/entity/creaking.png) | `minecraft-26.3-client.jar` → `minecraft/textures/entity/creaking/creaking.png` (identical PNG) |
| [aoemod/textures/entity/creaking_eyes.png](src/main/resources/assets/aoemod/textures/entity/creaking_eyes.png) | `minecraft-26.3-client.jar` → `minecraft/textures/entity/creaking/creaking_eyes.png` (identical PNG) |
| [aoemod/textures/entity/cubozoa/cubozoa.png](src/main/resources/assets/aoemod/textures/entity/cubozoa/cubozoa.png) | `libs__pack__betterend-20.0.11.jar` → `betterend/textures/entity/cubozoa/cubozoa.png` (identical PNG) |
| [aoemod/textures/entity/cubozoa/cubozoa_glow.png](src/main/resources/assets/aoemod/textures/entity/cubozoa/cubozoa_glow.png) | `libs__pack__betterend-20.0.11.jar` → `betterend/textures/entity/cubozoa/cubozoa_glow.png` (identical PNG) |
| [aoemod/textures/entity/cubozoa/cubozoa_sulphur.png](src/main/resources/assets/aoemod/textures/entity/cubozoa/cubozoa_sulphur.png) | `libs__pack__betterend-20.0.11.jar` → `betterend/textures/entity/cubozoa/cubozoa_sulphur.png` (identical PNG) |
| [aoemod/textures/entity/cubozoa/cubozoa_sulphur_glow.png](src/main/resources/assets/aoemod/textures/entity/cubozoa/cubozoa_sulphur_glow.png) | `libs__pack__betterend-20.0.11.jar` → `betterend/textures/entity/cubozoa/cubozoa_sulphur_glow.png` (identical PNG) |
| [aoemod/textures/entity/illager_lord.png](src/main/resources/assets/aoemod/textures/entity/illager_lord.png) | `assets_lendal mods2-v8.zip` → `modid/textures/entity/illager_boss_shield.png` (identical PNG) |
| [aoemod/textures/entity/nautilus.png](src/main/resources/assets/aoemod/textures/entity/nautilus.png) | `minecraft-26.3-client.jar` → `minecraft/textures/entity/nautilus/nautilus.png` (identical PNG) |
| [aoemod/textures/entity/shield_copper.png](src/main/resources/assets/aoemod/textures/entity/shield_copper.png) | `assets_lendal mods2-v8.zip` → `modid/textures/entity/shield_copper.png` (identical PNG) |
| [aoemod/textures/entity/shield_diamond.png](src/main/resources/assets/aoemod/textures/entity/shield_diamond.png) | `assets_lendal mods2-v8.zip` → `modid/textures/entity/shield_diamond.png` (identical PNG) |
| [aoemod/textures/entity/shield_gold.png](src/main/resources/assets/aoemod/textures/entity/shield_gold.png) | `assets_lendal mods2-v8.zip` → `modid/textures/entity/shield_gold.png` (identical PNG) |
| [aoemod/textures/entity/shield_iron.png](src/main/resources/assets/aoemod/textures/entity/shield_iron.png) | `assets_lendal mods2-v8.zip` → `modid/textures/entity/shield_iron.png` (identical PNG) |
| [aoemod/textures/entity/shield_netherite.png](src/main/resources/assets/aoemod/textures/entity/shield_netherite.png) | `assets_lendal mods2-v8.zip` → `modid/textures/entity/shield_netherite.png` (identical PNG) |
| [aoemod/textures/entity/shield_wooden.png](src/main/resources/assets/aoemod/textures/entity/shield_wooden.png) | `assets_lendal mods2-v8.zip` → `modid/textures/entity/shield_wooden.png` (identical PNG) |
| [aoemod/textures/item/banana_pie.png](src/main/resources/assets/aoemod/textures/item/banana_pie.png) | `assets_lendal mods2-v8.zip` → `modid/textures/item/banana_pie.png` (identical PNG) |
| [aoemod/textures/item/blueberry_cookie.png](src/main/resources/assets/aoemod/textures/item/blueberry_cookie.png) | `assets_lendal mods2-v8.zip` → `modid/textures/item/blueberry_cookie.png` (identical PNG) |
| [aoemod/textures/item/bone_arrow.png](src/main/resources/assets/aoemod/textures/item/bone_arrow.png) | `assets_lendal mods2-v8.zip` → `modid/textures/item/bone_arrow.png` (identical PNG) |
| [aoemod/textures/item/bone_axe.png](src/main/resources/assets/aoemod/textures/item/bone_axe.png) | `assets_lendal mods2-v8.zip` → `modid/textures/item/bone_axe.png` (identical PNG) |
| [aoemod/textures/item/bone_hoe.png](src/main/resources/assets/aoemod/textures/item/bone_hoe.png) | `assets_lendal mods2-v8.zip` → `modid/textures/item/bone_hoe.png` (identical PNG) |
| [aoemod/textures/item/bone_pickaxe.png](src/main/resources/assets/aoemod/textures/item/bone_pickaxe.png) | `assets_lendal mods2-v8.zip` → `modid/textures/item/bone_pickaxe.png` (identical PNG) |
| [aoemod/textures/item/bone_shears.png](src/main/resources/assets/aoemod/textures/item/bone_shears.png) | `assets_lendal mods2-v8.zip` → `modid/textures/item/bone_shears.png` (identical PNG) |
| [aoemod/textures/item/bone_shovel.png](src/main/resources/assets/aoemod/textures/item/bone_shovel.png) | `assets_lendal mods2-v8.zip` → `modid/textures/item/bone_shovel.png` (identical PNG) |
| [aoemod/textures/item/bone_spear.png](src/main/resources/assets/aoemod/textures/item/bone_spear.png) | `assets_lendal mods2-v8.zip` → `modid/textures/item/bone_spear.png` (identical PNG) |
| [aoemod/textures/item/bone_spear_in_hand.png](src/main/resources/assets/aoemod/textures/item/bone_spear_in_hand.png) | `assets_lendal mods2-v8.zip` → `modid/textures/item/bone_spear_in_hand.png` (identical PNG) |
| [aoemod/textures/item/bone_sword.png](src/main/resources/assets/aoemod/textures/item/bone_sword.png) | `assets_lendal mods2-v8.zip` → `modid/textures/item/bone_sword.png` (identical PNG) |
| [aoemod/textures/item/breeze_rod.png](src/main/resources/assets/aoemod/textures/item/breeze_rod.png) | `minecraft-26.3-client.jar` → `minecraft/textures/item/breeze_rod.png` (identical RGBA pixels) |
| [aoemod/textures/item/bucket_cubozoa.png](src/main/resources/assets/aoemod/textures/item/bucket_cubozoa.png) | `libs__pack__betterend-20.0.11.jar` → `betterend/textures/item/bucket_cubozoa.png` (identical PNG) |
| [aoemod/textures/item/carrot_seeds.png](src/main/resources/assets/aoemod/textures/item/carrot_seeds.png) | `assets_lendal mods2-v8.zip` → `modid/textures/item/carrot_seeds.png` (identical PNG) |
| [aoemod/textures/item/cherry_pie.png](src/main/resources/assets/aoemod/textures/item/cherry_pie.png) | `assets_lendal mods2-v8.zip` → `modid/textures/item/cherry_pie.png` (identical PNG) |
| [aoemod/textures/item/glowberry_pie.png](src/main/resources/assets/aoemod/textures/item/glowberry_pie.png) | `assets_lendal mods2-v8.zip` → `modid/textures/item/glowberry_pie.png` (identical PNG) |
| [aoemod/textures/item/glowshroom_stew.png](src/main/resources/assets/aoemod/textures/item/glowshroom_stew.png) | `assets_lendal mods2-v8.zip` → `modid/textures/item/glowshroom_stew.png` (identical PNG) |
| [aoemod/textures/item/honey_bowl.png](src/main/resources/assets/aoemod/textures/item/honey_bowl.png) | `assets_lendal mods2-v8.zip` → `modid/textures/item/honey_bowl.png` (identical PNG) |
| [aoemod/textures/item/iron_boss_axe.png](src/main/resources/assets/aoemod/textures/item/iron_boss_axe.png) | `assets_lendal mods2-v8.zip` → `modid/textures/item/iron_boss_axe.png` (identical PNG) |
| [aoemod/textures/item/melon_pie.png](src/main/resources/assets/aoemod/textures/item/melon_pie.png) | `assets_lendal mods2-v8.zip` → `modid/textures/item/melon_pie.png` (identical PNG) |
| [aoemod/textures/item/onion_seeds.png](src/main/resources/assets/aoemod/textures/item/onion_seeds.png) | `assets_lendal mods2-v8.zip` → `modid/textures/item/onion_seeds.png` (identical PNG) |
| [aoemod/textures/item/orange_pie.png](src/main/resources/assets/aoemod/textures/item/orange_pie.png) | `assets_lendal mods2-v8.zip` → `modid/textures/item/orange_pie.png` (identical PNG) |
| [aoemod/textures/item/pale_oak_door.png](src/main/resources/assets/aoemod/textures/item/pale_oak_door.png) | `minecraft-26.3-client.jar` → `minecraft/textures/item/pale_oak_door.png` (identical PNG) |
| [aoemod/textures/item/pear_pie.png](src/main/resources/assets/aoemod/textures/item/pear_pie.png) | `assets_lendal mods2-v8.zip` → `modid/textures/item/pear_pie.png` (identical PNG) |
| [aoemod/textures/item/plum_pie.png](src/main/resources/assets/aoemod/textures/item/plum_pie.png) | `assets_lendal mods2-v8.zip` → `modid/textures/item/plum_pie.png` (identical PNG) |
| [aoemod/textures/item/potato_seeds.png](src/main/resources/assets/aoemod/textures/item/potato_seeds.png) | `assets_lendal mods2-v8.zip` → `modid/textures/item/potato_seeds.png` (identical PNG) |
| [aoemod/textures/item/rice_seeds.png](src/main/resources/assets/aoemod/textures/item/rice_seeds.png) | `assets_lendal mods2-v8.zip` → `modid/textures/item/rice_seeds.png` (identical PNG) |
| [aoemod/textures/item/tipped_wooden_arrow_base.png](src/main/resources/assets/aoemod/textures/item/tipped_wooden_arrow_base.png) | `assets_lendal mods2-v8.zip` → `modid/textures/item/tipped_wooden_arrow_base.png` (identical PNG) |
| [aoemod/textures/models/armor/copper_layer_1.png](src/main/resources/assets/aoemod/textures/models/armor/copper_layer_1.png) | `minecraft-26.3-client.jar` → `minecraft/textures/entity/equipment/humanoid/copper.png` (identical PNG) |
| [aoemod/textures/models/armor/copper_layer_2.png](src/main/resources/assets/aoemod/textures/models/armor/copper_layer_2.png) | `minecraft-26.3-client.jar` → `minecraft/textures/entity/equipment/humanoid_leggings/copper.png` (identical PNG) |
| [betterend/textures/block/charnia_cyan.png](src/main/resources/assets/betterend/textures/block/charnia_cyan.png) | `libs__pack__betterend-20.0.11.jar` → `betterend/textures/block/charnia_cyan.png` (identical PNG) |
| [betterend/textures/block/charnia_light_blue.png](src/main/resources/assets/betterend/textures/block/charnia_light_blue.png) | `libs__pack__betterend-20.0.11.jar` → `betterend/textures/block/charnia_light_blue.png` (identical PNG) |
| [betterend/textures/item/charnia_cyan.png](src/main/resources/assets/betterend/textures/item/charnia_cyan.png) | `libs__pack__betterend-20.0.11.jar` → `betterend/textures/item/charnia_cyan.png` (identical PNG) |
| [betterend/textures/item/charnia_light_blue.png](src/main/resources/assets/betterend/textures/item/charnia_light_blue.png) | `libs__pack__betterend-20.0.11.jar` → `betterend/textures/item/charnia_light_blue.png` (identical PNG) |
| [collectorsreap/textures/block/budding_pink_dragon_fruits_stage0.png](src/main/resources/assets/collectorsreap/textures/block/budding_pink_dragon_fruits_stage0.png) | `libs__pack__collectorsreap-1.5.5.jar` → `collectorsreap/textures/block/budding_pink_dragon_fruits_stage0.png` (identical PNG) |
| [collectorsreap/textures/block/budding_pink_dragon_fruits_stage1.png](src/main/resources/assets/collectorsreap/textures/block/budding_pink_dragon_fruits_stage1.png) | `libs__pack__collectorsreap-1.5.5.jar` → `collectorsreap/textures/block/budding_pink_dragon_fruits_stage1.png` (identical PNG) |
| [collectorsreap/textures/block/budding_pink_dragon_fruits_stage2.png](src/main/resources/assets/collectorsreap/textures/block/budding_pink_dragon_fruits_stage2.png) | `libs__pack__collectorsreap-1.5.5.jar` → `collectorsreap/textures/block/budding_pink_dragon_fruits_stage2.png` (identical PNG) |
| [collectorsreap/textures/block/budding_pink_dragon_fruits_stage3.png](src/main/resources/assets/collectorsreap/textures/block/budding_pink_dragon_fruits_stage3.png) | `libs__pack__collectorsreap-1.5.5.jar` → `collectorsreap/textures/block/budding_pink_dragon_fruits_stage3.png` (identical PNG) |
| [collectorsreap/textures/block/dragon_bush.png](src/main/resources/assets/collectorsreap/textures/block/dragon_bush.png) | `libs__pack__collectorsreap-1.5.5.jar` → `collectorsreap/textures/block/dragon_bush.png` (identical PNG) |
| [collectorsreap/textures/block/pink_dragon_fruits_coiled_rope.png](src/main/resources/assets/collectorsreap/textures/block/pink_dragon_fruits_coiled_rope.png) | `libs__pack__collectorsreap-1.5.5.jar` → `collectorsreap/textures/block/pink_dragon_fruits_coiled_rope.png` (identical PNG) |
| [collectorsreap/textures/block/pink_dragon_fruits_stage0.png](src/main/resources/assets/collectorsreap/textures/block/pink_dragon_fruits_stage0.png) | `libs__pack__collectorsreap-1.5.5.jar` → `collectorsreap/textures/block/pink_dragon_fruits_stage0.png` (identical PNG) |
| [collectorsreap/textures/block/pink_dragon_fruits_stage1.png](src/main/resources/assets/collectorsreap/textures/block/pink_dragon_fruits_stage1.png) | `libs__pack__collectorsreap-1.5.5.jar` → `collectorsreap/textures/block/pink_dragon_fruits_stage1.png` (identical PNG) |
| [collectorsreap/textures/block/pink_dragon_fruits_stage2.png](src/main/resources/assets/collectorsreap/textures/block/pink_dragon_fruits_stage2.png) | `libs__pack__collectorsreap-1.5.5.jar` → `collectorsreap/textures/block/pink_dragon_fruits_stage2.png` (identical PNG) |
| [collectorsreap/textures/block/pink_dragon_fruits_stage3.png](src/main/resources/assets/collectorsreap/textures/block/pink_dragon_fruits_stage3.png) | `libs__pack__collectorsreap-1.5.5.jar` → `collectorsreap/textures/block/pink_dragon_fruits_stage3.png` (identical PNG) |
| [collectorsreap/textures/block/pink_dragon_fruits_vine_stage0.png](src/main/resources/assets/collectorsreap/textures/block/pink_dragon_fruits_vine_stage0.png) | `libs__pack__collectorsreap-1.5.5.jar` → `collectorsreap/textures/block/pink_dragon_fruits_vine_stage0.png` (identical PNG) |
| [collectorsreap/textures/block/pink_dragon_fruits_vine_stage1.png](src/main/resources/assets/collectorsreap/textures/block/pink_dragon_fruits_vine_stage1.png) | `libs__pack__collectorsreap-1.5.5.jar` → `collectorsreap/textures/block/pink_dragon_fruits_vine_stage1.png` (identical PNG) |
| [collectorsreap/textures/block/pink_dragon_fruits_vine_stage2.png](src/main/resources/assets/collectorsreap/textures/block/pink_dragon_fruits_vine_stage2.png) | `libs__pack__collectorsreap-1.5.5.jar` → `collectorsreap/textures/block/pink_dragon_fruits_vine_stage2.png` (identical PNG) |
| [collectorsreap/textures/block/pink_dragon_fruits_vine_stage3.png](src/main/resources/assets/collectorsreap/textures/block/pink_dragon_fruits_vine_stage3.png) | `libs__pack__collectorsreap-1.5.5.jar` → `collectorsreap/textures/block/pink_dragon_fruits_vine_stage3.png` (identical PNG) |
| [collectorsreap/textures/item/dragon_fruit_seeds.png](src/main/resources/assets/collectorsreap/textures/item/dragon_fruit_seeds.png) | `libs__pack__collectorsreap-1.5.5.jar` → `collectorsreap/textures/item/dragon_fruit_seeds.png` (identical PNG) |
| [collectorsreap/textures/item/pink_dragon_fruit.png](src/main/resources/assets/collectorsreap/textures/item/pink_dragon_fruit.png) | `libs__pack__collectorsreap-1.5.5.jar` → `collectorsreap/textures/item/pink_dragon_fruit.png` (identical PNG) |
| [minecraft/textures/block/azalea_leaves.png](src/main/resources/assets/minecraft/textures/block/azalea_leaves.png) | `minecraft-26.3-client.jar` → `minecraft/textures/block/azalea_leaves.png` (identical RGBA pixels) |
| [minecraft/textures/block/flowering_azalea_leaves.png](src/main/resources/assets/minecraft/textures/block/flowering_azalea_leaves.png) | `minecraft-26.3-client.jar` → `minecraft/textures/block/flowering_azalea_leaves.png` (identical RGBA pixels) |
| [minecraft/textures/block/shroomlight.png](src/main/resources/assets/minecraft/textures/block/shroomlight.png) | `minecraft-26.3-client.jar` → `minecraft/textures/block/shroomlight.png` (identical RGBA pixels) |
| [minecraft/textures/blocks/shroomlight.png](src/main/resources/assets/minecraft/textures/blocks/shroomlight.png) | `minecraft-26.3-client.jar` → `minecraft/textures/block/shroomlight.png` (identical RGBA pixels) |
| [minecraft/textures/entity/cow/brown_mooshroom.png](src/main/resources/assets/minecraft/textures/entity/cow/brown_mooshroom.png) | `minecraft-26.3-client.jar` → `minecraft/textures/entity/cow/mooshroom_brown.png` (identical PNG) |
| [minecraft/textures/entity/cow/cow.png](src/main/resources/assets/minecraft/textures/entity/cow/cow.png) | `minecraft-26.3-client.jar` → `minecraft/textures/entity/cow/cow_temperate.png` (identical PNG) |
| [minecraft/textures/entity/cow/red_mooshroom.png](src/main/resources/assets/minecraft/textures/entity/cow/red_mooshroom.png) | `minecraft-26.3-client.jar` → `minecraft/textures/entity/cow/mooshroom_red.png` (identical PNG) |

## Existing textures with unverified authorship

These 53 PNGs are already present in commit `e484294` (23 October 2026). No available creation record or identical source match established their author during this audit, so they are not included in the verified count. Some are clearly edits or custom content, but that alone is insufficient to attribute their creation to the assistant.

| Texture | Category |
| --- | --- |
| [aoemod/textures/block/basalt_gold_ore_side.png](src/main/resources/assets/aoemod/textures/block/basalt_gold_ore_side.png) | Block |
| [aoemod/textures/block/basalt_gold_ore_top.png](src/main/resources/assets/aoemod/textures/block/basalt_gold_ore_top.png) | Block |
| [aoemod/textures/block/basalt_quartz_ore_side.png](src/main/resources/assets/aoemod/textures/block/basalt_quartz_ore_side.png) | Block |
| [aoemod/textures/block/basalt_quartz_ore_top.png](src/main/resources/assets/aoemod/textures/block/basalt_quartz_ore_top.png) | Block |
| [aoemod/textures/block/jungle_sweet_berry_bush_stage0.png](src/main/resources/assets/aoemod/textures/block/jungle_sweet_berry_bush_stage0.png) | Block |
| [aoemod/textures/block/jungle_sweet_berry_bush_stage1.png](src/main/resources/assets/aoemod/textures/block/jungle_sweet_berry_bush_stage1.png) | Block |
| [aoemod/textures/block/jungle_sweet_berry_bush_stage2.png](src/main/resources/assets/aoemod/textures/block/jungle_sweet_berry_bush_stage2.png) | Block |
| [aoemod/textures/block/jungle_sweet_berry_bush_stage3.png](src/main/resources/assets/aoemod/textures/block/jungle_sweet_berry_bush_stage3.png) | Block |
| [aoemod/textures/block/open_eyeblossom.png](src/main/resources/assets/aoemod/textures/block/open_eyeblossom.png) | Block |
| [aoemod/textures/block/soul_beetroots_stage0.png](src/main/resources/assets/aoemod/textures/block/soul_beetroots_stage0.png) | Block |
| [aoemod/textures/block/soul_beetroots_stage1.png](src/main/resources/assets/aoemod/textures/block/soul_beetroots_stage1.png) | Block |
| [aoemod/textures/block/soul_beetroots_stage2.png](src/main/resources/assets/aoemod/textures/block/soul_beetroots_stage2.png) | Block |
| [aoemod/textures/block/soul_beetroots_stage3.png](src/main/resources/assets/aoemod/textures/block/soul_beetroots_stage3.png) | Block |
| [aoemod/textures/block/soul_soil_gold_ore.png](src/main/resources/assets/aoemod/textures/block/soul_soil_gold_ore.png) | Block |
| [aoemod/textures/entity/parched.png](src/main/resources/assets/aoemod/textures/entity/parched.png) | Mob |
| [aoemod/textures/entity/sea_serpent.png](src/main/resources/assets/aoemod/textures/entity/sea_serpent.png) | Mob |
| [aoemod/textures/item/mace.png](src/main/resources/assets/aoemod/textures/item/mace.png) | Item |
| [aoemod/textures/item/soul_beetroot.png](src/main/resources/assets/aoemod/textures/item/soul_beetroot.png) | Item |
| [aoemod/textures/item/soul_beetroot_seeds.png](src/main/resources/assets/aoemod/textures/item/soul_beetroot_seeds.png) | Item |
| [minecraft/textures/block/azalea_leaves_1.png](src/main/resources/assets/minecraft/textures/block/azalea_leaves_1.png) | Block |
| [minecraft/textures/block/azalea_plant.png](src/main/resources/assets/minecraft/textures/block/azalea_plant.png) | Block |
| [minecraft/textures/block/azalea_side.png](src/main/resources/assets/minecraft/textures/block/azalea_side.png) | Block |
| [minecraft/textures/block/azalea_top.png](src/main/resources/assets/minecraft/textures/block/azalea_top.png) | Block |
| [minecraft/textures/block/big_dripleaf_side.png](src/main/resources/assets/minecraft/textures/block/big_dripleaf_side.png) | Block |
| [minecraft/textures/block/big_dripleaf_stem.png](src/main/resources/assets/minecraft/textures/block/big_dripleaf_stem.png) | Block |
| [minecraft/textures/block/big_dripleaf_tip.png](src/main/resources/assets/minecraft/textures/block/big_dripleaf_tip.png) | Block |
| [minecraft/textures/block/big_dripleaf_top.png](src/main/resources/assets/minecraft/textures/block/big_dripleaf_top.png) | Block |
| [minecraft/textures/block/cave_vines.png](src/main/resources/assets/minecraft/textures/block/cave_vines.png) | Block |
| [minecraft/textures/block/cave_vines_lit.png](src/main/resources/assets/minecraft/textures/block/cave_vines_lit.png) | Block |
| [minecraft/textures/block/cave_vines_plant.png](src/main/resources/assets/minecraft/textures/block/cave_vines_plant.png) | Block |
| [minecraft/textures/block/cave_vines_plant_lit.png](src/main/resources/assets/minecraft/textures/block/cave_vines_plant_lit.png) | Block |
| [minecraft/textures/block/flowering_azalea_side.png](src/main/resources/assets/minecraft/textures/block/flowering_azalea_side.png) | Block |
| [minecraft/textures/block/flowering_azalea_top.png](src/main/resources/assets/minecraft/textures/block/flowering_azalea_top.png) | Block |
| [minecraft/textures/block/moss_block.png](src/main/resources/assets/minecraft/textures/block/moss_block.png) | Block |
| [minecraft/textures/block/potted_azalea_bush_plant.png](src/main/resources/assets/minecraft/textures/block/potted_azalea_bush_plant.png) | Block |
| [minecraft/textures/block/potted_azalea_bush_side.png](src/main/resources/assets/minecraft/textures/block/potted_azalea_bush_side.png) | Block |
| [minecraft/textures/block/potted_azalea_bush_top.png](src/main/resources/assets/minecraft/textures/block/potted_azalea_bush_top.png) | Block |
| [minecraft/textures/block/potted_flowering_azalea_bush_plant.png](src/main/resources/assets/minecraft/textures/block/potted_flowering_azalea_bush_plant.png) | Block |
| [minecraft/textures/block/potted_flowering_azalea_bush_side.png](src/main/resources/assets/minecraft/textures/block/potted_flowering_azalea_bush_side.png) | Block |
| [minecraft/textures/block/small_dripleaf_side.png](src/main/resources/assets/minecraft/textures/block/small_dripleaf_side.png) | Block |
| [minecraft/textures/block/small_dripleaf_stem_bottom.png](src/main/resources/assets/minecraft/textures/block/small_dripleaf_stem_bottom.png) | Block |
| [minecraft/textures/block/small_dripleaf_stem_top.png](src/main/resources/assets/minecraft/textures/block/small_dripleaf_stem_top.png) | Block |
| [minecraft/textures/block/small_dripleaf_top.png](src/main/resources/assets/minecraft/textures/block/small_dripleaf_top.png) | Block |
| [minecraft/textures/block/spore_blossom_base.png](src/main/resources/assets/minecraft/textures/block/spore_blossom_base.png) | Block |

## Audit details

- Enumerated all **244** PNGs in production asset texture directories; animation metadata and model JSONs are not additional textures.
- Compared bytes and decoded RGBA against **72** available local archives, including installed dependencies, remapped copies, Minecraft 1.20.1/26.3 clients, and the supplied v8 asset ZIP. Duplicate archives do not increase texture counts.
- The [earlier machine-readable audit](tmp/texture-provenance-audit.json) records the previous 235-file inventory, its SHA-256 hashes, dimensions, Git state, and matching archive entries. This follow-up verified the 233 unchanged hashes, relocated Orcz atlases, and eight added blueberry textures against their source archives and the [Orcz](tmp/orc-bone-gear-assets/pixel-audit.json) and [blueberry](tmp/blueberry-textures/pixel-audit.json) pixel audits.
- [Audit script](tmp/audit-texture-provenance.py) reads source files and archives without changing textures. Its Pillow dependency is available in the bundled Codex Python runtime.
- [Supplied asset mapping](build/assets_v8_manifest.json) and [official client provenance](build/future-assets/official-client-source.json) distinguish integration work from artwork creation.
- This report does not certify that every retained PNG is currently rendered. The Spider Queen edit is explicitly inactive; other legacy files can remain after gameplay changes.
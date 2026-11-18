# Dimension dependency audit

October 31 follow-up: Floral Flair and Ramadan Delight are removed from the active dependency lock and production manifest; their JARs are archived under `libs/retired-source-archives/floral-ramadan-removal-20261031/`. My Nether's Delight remains installed, but `mynethersdelight:nether_bricks_cabinet` is excluded from creative catalogs and recipe outputs. The blackstone cabinet remains. Only Collector's Reap dragon-fruit content is restored locally under its original IDs: `pink_dragon_fruit`, `dragon_fruit_seeds`, `dragon_bush`, `budding_pink_dragon_fruits`, and `pink_dragon_fruits`. The full Collector's Reap dependency remains retired. This supersedes the earlier Ramadan catalog revision below.

October 31 inventory revision: Ramadan Delight 1.7 (Forge 1.20.1) is pinned from https://modrinth.com/mod/ramadan-delight/version/J3ZyybgD. Its published SHA-512 was verified; required Farmer's Delight >=1.2.8 is already satisfied. All Ramadan items and native recipes are retained. Its plantable chickpea is `ramadandelight:chickpea`; this release has no peanut item. Collector's Reap was removed from active dependencies, integrations and data, and its original JAR was archived locally. AoE's existing Triops placeholder sprite was copied unchanged with its MIT license, eliminating that item's dependency on the removed mod. The earlier audit below is historical.

Audited against the six current dimension texts cached in `build/dimension-source-docs.json`, and the real loaded registry at `run/reports/loaded-registry-audit.json`. The older Other Items candidate list was not treated as an install list.

October 28 update: Aquatica 0.0.1 was removed from both dependency manifests and AoE's required dependencies. Its isopod integration and the custom Swordfish implementation/content were removed. No remaining locked mod requires Aquatica. Natural snowy lobsters use native variants 0 (red) and 3 (red-blue); Endfish and Cubozoa remain installed but no longer spawn naturally in Snowy.

## Added

| Dependency | Exact version | Source | Reason |
|---|---|---|---|
| My Nether's Delight | 1.20.1-1.10.4-backport.1, Forge | https://modrinth.com/mod/my-nethers-delight/version/pOBasFQT | Nether powder cane |
| Immersive Portals | 3.0.7, Forge 1.20.1 | https://modrinth.com/mod/immersive-portals-neoforge/version/Q6Z7eOLL | Persistent 1:1 Desert floor (-32) to custom Nether ceiling (128) connection |
| Cloth Config API | 11.1.136, Forge | https://modrinth.com/mod/cloth-config/version/t8TXrZvZ | Required by Immersive Portals' bundled q_misc_util |
| Embeddium | 0.3.31+mc1.20.1, Forge | https://modrinth.com/mod/embeddium/version/UTbfe5d1 | Sodium-derived client renderer and NVIDIA driver workaround |

Immersive Portals and Cloth Config are unmodified official Forge binaries, pinned in both dependency manifests. Their SHA256 values are `deb233d1e530e5e208ba6f3529a3b90ad071a06a2dc8d7ea60f14b0d67912efc` and `1e895e85cf5b1e1905ef3178ec155c8badfe22a1577b92c09143a5aa1f4ce0f2`, respectively. The stack uses the installed version's `VerticalConnectingPortal` and `GlobalPortalStorage` APIs, preserves unrelated portals, and checks both directions before modifying storage.

The official My Nether's Delight Modrinth download was checked against its published SHA512. Local SHA256: `489bd70663a71f6c54c16de6d48a8e8778969bba40108a2b9b981c3c47138db2`. Required Farmer's Delight >=1.3.2 is already satisfied by 1.3.4. Optional integrations were not installed. The project page states All Rights Reserved while this jar's Forge metadata states MIT; the unmodified binary is used locally and no assets or source were copied. The existing Gradle lock-file loop includes the new dependency automatically. With Embeddium, the production manifest contains 73 artifacts including the embedded MixinExtras entry.

Actual block IDs: `mynethersdelight:powdery_cane`, `mynethersdelight:powdery_cannon`; planting item: `mynethersdelight:powder_cannon`. These were confirmed in the combined Forge runtime registry. The additive substrate tag permits their documented basalt/blackstone habitat.

## Already present

All other named dependency families in the six documents already had native Forge jars in the locked local pack: Alex's Caves 2.0.2; Alex's Mobs 1.22.9; Ambient Additions 1.20.1-1.1.2; Angling 1.0.1-forge.1; BetterEnd 20.0.11; Biodiverse 1.1.0; Biomes O' Plenty 18.0.0.592; Biomes We've Gone 1.8.2; Bountiful Critters 1.5.1; Collector's Reap 1.5.5; Cooking Expanded 1.0.0; Ecologics 2.2.7; Environmental 4.1.2; Farmer's Delight 1.20.1-1.3.4; Ferdinand's Flowers 3.0; Fins and Tails 1.20.1-1.1.6; Floral 1.0.0; Floral Flair 1.20.1-2.2.2; Fruits Delight 1.1.3; Hearth and Harvest 1.4.1; Hybrid Aquatic 1.7.4; Legume Delight 1.20.1-1.4.0; Wilder Nature 1.0.6; Pineapple Delight 1.2.0-1.20.1-forge; Quark 4.0-462; Outer End 1.0.0; Twilight Forest 4.3.2508; Untamed Wilds 4.0.4; Upgrade Aquatic 6.0.3; Vintage Delight 0.1.6. Existing custom Forge ports and the future-content backports were preserved. Nether Depths Upgrade 3.1.4-1.20 was already a retained Maven dependency.

## Names needing custom implementation or clarification

- `aoemod:arthropleura` is an original Forge implementation with original Java model geometry and vanilla texture references. It has fourteen armored body segments, twenty-eight animated leg pairs and bent antennae, and grazes and breeds on leaves, ferns and mushrooms. The former custom Swordfish implementation and its food/recipe content were removed on October 28. No external models, textures or code were copied.
- The installed Angling port supplies `angling:sea_slug`, but neither sea snail nor starfish appears in its registered entities. These document names must not silently resolve to a different species.
- The documented Alex's Mobs Leviathan is registered as `alexsmobs:laviathan`.
- Collector's Reap's documented giant river prawn is registered as `collectorsreap:tiger_prawn`.
- Alex's Caves' tripod fish is registered as `alexscaves:tripodfish`.
- Yaks naturally spawn in the snowy dimension's dappled forest and display **Yak**. Their saved entity ID remains `minecraft:cow`, with the existing backport's cold variant supplying the appearance and the AoE pack behavior supplying storage.
- The existing AOE nautilus is `aoemod:nautilus`; the future-content policy intentionally excludes the duplicate backport nautilus.
- Glowing cattail is tentative in the source document; installed BWG has ordinary cattail, not a separately registered glowing cattail.
- The existing `aoemod:cubozoa` already extends the Nether Depths Upgrade lava-fish base and is reused as Lava Jellyfish; the original `betterend:cubozoa` remains registered, but is excluded from natural snowy spawning.

No source/asset copying or new Fabric jar was used in this audit.

## Production mapping compatibility

Ferdinand's Flowers and Huge Structure Blocks both ship the default resource `mixin.refmap.json` without naming it explicitly in their mixin configurations. In the combined production classloader, this can give either mod the other mod's mappings; the stock Forge launch failed on Ferdinand's `CandleCakeBlockMixin.byCandle` before world loading. `scripts/prepare-production-mods.py` now gives each bundled mapping a unique resource path and updates its configuration when staging the pack. Original binaries and both dependency manifests remain unchanged. The staged manifest records original and resulting SHA256 values and the compatibility patch; every class, asset and license entry remains byte-identical. The transformation is deterministic and verified against both actual jars. Use the staging script when preparing a production pack rather than copying the original conflicting jars directly.

The staging script accepts an explicit `--optional-mod` for the separate VillagerRealms integration fixture. This does not add VillagerRealms to either AoE dependency manifest.

## Documented food and yak behavior

`DimensionFoods` supplies 36 edible items for missing document harvests, including an actual `aoemod:plum`, named raw/cooked seafood and the sulfur Endfish harvest. Each added cooked food has furnace, smoker and campfire recipes; species drops use their named foods, and fire kills select cooked results. Existing working squid/glow-squid foods are retained; Swordfish and isopod foods have been removed. Models reference installed or vanilla icons directly, without copying assets; these are functional items with reused icons, not a finished original seafood sprite set. Plum pie now consumes the plum, and the custom fruit joins the existing plum tag.

Natural dappled-forest yaks receive the Backport cold variant. Feed an adult yak wheat to tame it, then equip one chest for 27 inventory slots. The owner can open the pack with an empty hand; sneaking with an empty hand toggles following/staying. The pack preserves slot positions and item NBT across saves, exposes Forge's item-handler capability, and releases its chest and cargo after configured mob-drop replacement on death. Yaks produce no meat of their own; carried meat is retained with the rest of the cargo. The translated **Yak** type name leaves custom name tags available. Paired chest models are rendered on the yak. Ordinary cows retain their normal wheat behavior. Yak aging/breeding multipliers in the snowy dimension remain 0.75/1; plains/savanna cow rates are retained. Runtime regressions cover storage, ownership, meat suppression, cargo preservation and cooking paths. The earlier stock Forge production client visual check confirmed the corrected chest size and flank attachment, plus the original Arthropleura and swordfish models and both swordfish steak items. Those earlier captures are saved under `run/portal-production-client/game/screenshots/`, including `aoe-portal-qa-dimensions-yak-side-corrected.png` and `aoe-portal-qa-dimensions-swordfish-side.png`.

The dappled forest reuses the installed `VanillaBackport-forge-1.20.1-1.2.0.2640.1` poplars, red shrubs and shelf-mushroom decorator. Its reference tree count is halved from six to three; vanilla forest-grass patch count is doubled from two to four. No additional biome dependency is required. The old crimson-root overrides for red shrubs are removed so native grass/dirt support, shape and spreading apply. Snowlogged berries use the existing Snow! Real Magic container and overlay support, extended to AoE's blue-berry bush.

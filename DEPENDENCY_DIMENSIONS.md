# Dimension dependency audit

Audited against the six current dimension texts cached in `build/dimension-source-docs.json`, and the real loaded registry at `run/reports/loaded-registry-audit.json`. The older Other Items candidate list was not treated as an install list.

## Added

| Dependency | Exact version | Source | Reason |
|---|---|---|---|
| My Nether's Delight | 1.20.1-1.10.4-backport.1, Forge | https://modrinth.com/mod/my-nethers-delight/version/pOBasFQT | Nether powder cane |
| Immersive Portals | 3.0.7, Forge 1.20.1 | https://modrinth.com/mod/immersive-portals-neoforge/version/Q6Z7eOLL | Persistent 1:1 Desert floor (-32) to custom Nether ceiling (128) connection |
| Cloth Config API | 11.1.136, Forge | https://modrinth.com/mod/cloth-config/version/t8TXrZvZ | Required by Immersive Portals' bundled q_misc_util |

Immersive Portals and Cloth Config are unmodified official Forge binaries, pinned in both dependency manifests. Their SHA256 values are `deb233d1e530e5e208ba6f3529a3b90ad071a06a2dc8d7ea60f14b0d67912efc` and `1e895e85cf5b1e1905ef3178ec155c8badfe22a1577b92c09143a5aa1f4ce0f2`, respectively. The stack uses the installed version's `VerticalConnectingPortal` and `GlobalPortalStorage` APIs, preserves unrelated portals, and checks both directions before modifying storage.

The official Modrinth download was checked against its published SHA512. Local SHA256: `489bd70663a71f6c54c16de6d48a8e8778969bba40108a2b9b981c3c47138db2`. Required Farmer's Delight >=1.3.2 is already satisfied by 1.3.4. Optional integrations were not installed. The project page states All Rights Reserved while this jar's Forge metadata states MIT; the unmodified binary is used locally and no assets or source were copied. The existing Gradle lock-file loop includes the new dependency automatically. The production manifest now contains 73 artifacts including the embedded MixinExtras entry.

Actual block IDs: `mynethersdelight:powdery_cane`, `mynethersdelight:powdery_cannon`; planting item: `mynethersdelight:powder_cannon`. These were confirmed in the combined Forge runtime registry. The additive substrate tag permits their documented basalt/blackstone habitat.

## Already present

All other named dependency families in the six documents already had native Forge jars in the locked local pack: Alex's Caves 2.0.2; Alex's Mobs 1.22.9; Ambient Additions 1.20.1-1.1.2; Angling 1.0.1-forge.1; Aquatica 0.0.1; BetterEnd 20.0.11; Biodiverse 1.1.0; Biomes O' Plenty 18.0.0.592; Biomes We've Gone 1.8.2; Bountiful Critters 1.5.1; Collector's Reap 1.5.5; Cooking Expanded 1.0.0; Ecologics 2.2.7; Environmental 4.1.2; Farmer's Delight 1.20.1-1.3.4; Ferdinand's Flowers 3.0; Fins and Tails 1.20.1-1.1.6; Floral 1.0.0; Floral Flair 1.20.1-2.2.2; Fruits Delight 1.1.3; Hearth and Harvest 1.4.1; Hybrid Aquatic 1.7.4; Legume Delight 1.20.1-1.4.0; Wilder Nature 1.0.6; Pineapple Delight 1.2.0-1.20.1-forge; Quark 4.0-462; Outer End 1.0.0; Twilight Forest 4.3.2508; Untamed Wilds 4.0.4; Upgrade Aquatic 6.0.3; Vintage Delight 0.1.6. Existing custom Forge ports and the future-content backports were preserved. Nether Depths Upgrade 3.1.4-1.20 was already a retained Maven dependency.

## Names needing custom implementation or clarification

- `aoemod:arthropleura` and `aoemod:swordfish` are original Forge implementations, with original Java model geometry and vanilla texture references. Arthropleura has fourteen armored body segments, twenty-eight animated leg pairs and bent antennae; it grazes and breeds on leaves, ferns and mushrooms. Swordfish has a long bill, forked tail, swimming navigation, raw/cooked steaks and three cooking methods. These are recognizable adaptations, not ports of the source mods' models, textures or code. [Fossils and Archeology Revival's 1.12.2 README](https://github.com/TeamFossilsArcheology/FossilsArcheologyRevival/tree/1.12.2) reserves model/texture assets despite its separate code license; [Abyssal Depths](https://github.com/Chumbanotz/AbyssalDepths) uses AGPL-3.0. Neither project's assets or code were copied into these implementations, and AoE's license metadata is unchanged.
- The installed Angling port supplies `angling:sea_slug`, but neither sea snail nor starfish appears in its registered entities. These document names must not silently resolve to a different species.
- The documented Alex's Mobs Leviathan is registered as `alexsmobs:laviathan`.
- Collector's Reap's documented giant river prawn is registered as `collectorsreap:tiger_prawn`.
- Alex's Caves' tripod fish is registered as `alexscaves:tripodfish`.
- The snowy yak is the cold variant of `minecraft:cow` supplied by the existing backport, not a separate entity ID.
- The existing AOE nautilus is `aoemod:nautilus`; the future-content policy intentionally excludes the duplicate backport nautilus.
- Glowing cattail is tentative in the source document; installed BWG has ordinary cattail, not a separately registered glowing cattail.
- The existing `aoemod:cubozoa` already extends the Nether Depths Upgrade lava-fish base and is reused as Lava Jellyfish; snowy water uses the original `betterend:cubozoa` non-sulfur variant.

No source/asset copying or new Fabric jar was used in this audit.

## Production mapping compatibility

Ferdinand's Flowers and Huge Structure Blocks both ship the default resource `mixin.refmap.json` without naming it explicitly in their mixin configurations. In the combined production classloader, this can give either mod the other mod's mappings; the stock Forge launch failed on Ferdinand's `CandleCakeBlockMixin.byCandle` before world loading. `scripts/prepare-production-mods.py` now gives each bundled mapping a unique resource path and updates its configuration when staging the pack. Original binaries and both dependency manifests remain unchanged. The staged manifest records original and resulting SHA256 values and the compatibility patch; every class, asset and license entry remains byte-identical. The transformation is deterministic and verified against both actual jars. Use the staging script when preparing a production pack rather than copying the original conflicting jars directly.

The staging script accepts an explicit `--optional-mod` for the separate VillagerRealms integration fixture. This does not add VillagerRealms to either AoE dependency manifest.

## Documented food and yak behavior

`DimensionFoods` supplies 38 edible items for missing document harvests, including an actual `aoemod:plum`, named raw/cooked seafood and the sulfur Endfish harvest. Each added cooked food has furnace, smoker and campfire recipes; species drops use their named foods, and fire kills select cooked results. Existing working squid/glow-squid foods and the swordfish implementation are retained. Models reference installed or vanilla icons directly, without copying assets; these are functional items with reused icons, not a finished original seafood sprite set. Plum pie now consumes the plum, and the custom fruit joins the existing plum tag.

Natural snowy cows receive the Backport cold variant. Feed an adult cold cow wheat to tame it, then equip one chest for 27 inventory slots. The owner can open the pack with an empty hand; sneaking with an empty hand toggles following/staying. The pack preserves slot positions and item NBT across saves, exposes Forge's item-handler capability, and releases its chest and cargo after configured mob-drop replacement on death. Paired chest models are rendered on the cow. Ordinary cows retain their normal wheat behavior. Snow cow aging/breeding multipliers are 0.75/1; their plains/savanna rates are retained. Runtime tests cover these storage, ownership and cooking paths. The stock Forge production client visual check confirmed the corrected chest size and flank attachment, plus the original Arthropleura and swordfish models and both swordfish steak items. Captures are saved under `run/portal-production-client/game/screenshots/`, including `aoe-portal-qa-dimensions-yak-side-corrected.png` and `aoe-portal-qa-dimensions-swordfish-side.png`.

# Ore and blob comparison

**No caves or large noise-generated mineral veins on either side.** Numbers are estimated custom blocks, followed by their percentage change from vanilla 1.20.1. **Blocks/Y** averages a 16×16 slice across the indicated rock region. **Blocks/blob** counts the portion of a successful deposit inside that region; deposits crossing a boundary can contribute to both rows. “New” means the compared vanilla region has zero supply.

Each Overworld material has an **overall** row plus the applicable **stone** and **deepslate** rows. Overall density is total blocks divided by the full column's layer count, not an average of the two percentages. Overall blob size counts each successful placement once. Overall comparisons include vanilla Y0…15, which the split comparisons omit.

Use the same representative terrain height in both versions. Plains' stone comparison uses vanilla Y≥16; its overall comparison uses the full vanilla column:

| Area / rock | Custom Y range | Vanilla Y range |
|---|---|---|
| Plains overall | −64…64 | −64…64 |
| Plains stone | −64…64 | 16…64 |
| Snowy overall | −96…128 | −64…128 |
| Snowy stone | −40…128 | 16…128 |
| Snowy deepslate | −96…−41 | −64…−1 |
| Desert / Deep Dark overall | −32…96 | −64…96 |
| Desert / Deep Dark stone | 8…96 | 16…96 |
| Desert / Deep Dark deepslate | −32…7 | −64…−1 |

These are height scenarios, not biome-average elevations. Snowy/Desert transitions are approximated by sharp boundaries at Y−40/Y8. Vanilla comparators are plains, a snowy mountain with emeralds, and badlands including its extra gold.

Discard is **vanilla → custom**, with changes in percentage points (pp); a lone value is unchanged. Covered, cave-free rock has no air-discard losses.

## Plains

| Material / rock | Blocks/Y (change) | Blocks/blob (change) | Air discard |
|---|---:|---:|---|
| **Coal (overall)** | 2.35 (+187%) | 21.9 (+2%) | 50% → 0% (−50 pp) |
| Coal (stone) | 2.35 (+18%) | 21.9 (+3%) | 50% → 0% (−50 pp) |
| **Iron (overall)** | 0.782 (+17%) | 6.77 (+34%) | 0% |
| Iron (stone) | 0.782 (-3%) | 6.77 (+32%) | 0% |
| **Copper (overall)** | 0.665 (-13%) | 8.33 (0%) | 0% |
| Copper (stone) | 0.665 (-60%) | 8.33 (+2%) | 0% |
| **Gold (overall)** | 0.21 (-10%) | 6.79 (0%) | 50% → 25% (−25 pp) |
| Gold (stone) | 0.21 (+611%) | 6.79 (+8%) | 50% → 25% (−25 pp) |
| **Andesite / diorite / granite, each (overall)** | 20 (+88%) | 663 (-2%) | 0% |
| Andesite / diorite / granite, each (stone) | 20 (0%) | 663 (+6%) | 0% |
| **Gravel, both passes (overall)** | 4.94 (+21%) | 96.7 (-12%) | 0% → 0/100% (0/+100 pp) |
| Gravel, both passes (stone) | 4.94 (+21%) | 96.7 (-7%) | 0% → 0/100% (0/+100 pp) |

Diamond, lapis, redstone and dirt blobs are absent; emerald is absent in both.

## Snowy / mountains

| Material / rock | Blocks/Y (change) | Blocks/blob (change) | Air discard |
|---|---:|---:|---|
| **Coal (overall, no mushroom bonus)** | 3.73 (+106%) | 22.1 (0%) | 50% → 0% (−50 pp) |
| **Coal (overall, mushroom bonus)** | 5.98 (+230%) | 28 (+27%) | 50% → 0% (−50 pp) |
| Coal (stone, no mushroom bonus) | 3.99 (+32%) | 21.9 (0%) | 50% → 0% (−50 pp) |
| Coal (stone, mushroom bonus) | 6.99 (+131%) | 29 (+32%) | 50% → 0% (−50 pp) |
| Coal (deepslate, no mushroom bonus) | 2.96 (+176,063%) | 21.7 (+90%) | 50% → 0% (−50 pp) |
| Coal (deepslate, mushroom bonus) | 2.96 (+176,063%) | 21.7 (+90%) | 50% → 0% (−50 pp) |
| **Iron (overall)** | 1.21 (+92%) | 6.79 (+27%) | 0% |
| Iron (stone) | 1.03 (+56%) | 6.76 (+20%) | 0% |
| Iron (deepslate) | 1.74 (+386%) | 6.74 (+67%) | 0% |
| **Copper (overall)** | 0.746 (+7%) | 8.4 (0%) | 0% |
| Copper (stone) | 0.984 (-4%) | 8.39 (+1%) | 0% |
| Copper (deepslate) | 0.0282 (-65%) | 7.29 (-6%) | 0% |
| **Gold (overall)** | 0.332 (+112%) | 6.79 (0%) | 50% → 25% (−25 pp) |
| Gold (stone) | 0.202 (+1,476%) | 6.72 (+7%) | 50% → 25% (−25 pp) |
| Gold (deepslate) | 0.723 (+89%) | 6.73 (+1%) | 50% → 25% (−25 pp) |
| **Diamond (overall)** | 0.158 (+65%) | 4.14 (+4%) | 50/70/100% → 50% (0/−20/−50 pp) |
| Diamond (stone) | 0.0341 (+814,792%) | 4.1 (+163%) | 50/70/100% → 50% (0/−20/−50 pp) |
| Diamond (deepslate) | 0.532 (+91%) | 4.13 (+4%) | 50/70/100% → 50% (0/−20/−50 pp) |
| **Emerald (overall)** | 0.0617 (+48%) | 1.31 (0%) | 0% → 25% (+25 pp) |
| Emerald (stone) | 0.0617 (-8%) | 1.31 (0%) | 0% → 25% (+25 pp) |
| Emerald (deepslate) | 0.0617 (+3,182%) | 1.31 (0%) | 0% → 25% (+25 pp) |
| **Lapis (overall)** | 0.22 (+44%) | 4.97 (0%) | 0/100% → 25% (+25/−75 pp) |
| Lapis (stone) | 0.131 (+76%) | 4.87 (0%) | 0/100% → 25% (+25/−75 pp) |
| Lapis (deepslate) | 0.487 (+107%) | 4.89 (+1%) | 0/100% → 25% (+25/−75 pp) |
| **Andesite / diorite / granite, each (overall)** | 7.23 (-6%) | 668 (-2%) | 0% |
| Andesite / diorite / granite, each (stone) | 9.63 (0%) | 668 (+5%) | 0% |
| **Tuff (overall)** | 5.13 (-26%) | 638 (-4%) | 0% |
| Tuff (deepslate) | 20.6 (0%) | 638 (-3%) | 0% |
| **Calcite vs. vanilla andesite (overall)** | 7.23 (-6%) | 668 (-2%) | 0% |
| Calcite vs. vanilla andesite (stone) | 9.63 (0%) | 668 (+5%) | 0% |
| **Polished basalt vs. vanilla tuff (overall)** | 5.13 (-26%) | 638 (-4%) | 0% |
| Polished basalt vs. vanilla tuff (deepslate) | 20.6 (0%) | 638 (-3%) | 0% |

The mushroom bonus changes stone coal only under the simplified Y−40 split, so the two deepslate coal rows match. The actual noisy transition can admit a little bonus coal into deepslate. Redstone, gravel and dirt blobs are absent in both custom rock regions.

All four Snowy biomes also share vanilla amethyst geodes: a 1-in-24 attempt per chunk, with origins Y−90…30. Their smooth-basalt shell, calcite, amethyst and budding amethyst are enabled. Geodes are separate from the ore/blob estimates below.

Huge percentages reflect tiny vanilla baselines: **deepslate coal ≈0.00168 blocks/Y**, from spill below Y0, and **stone diamond ≈0.00000419 blocks/Y**, at the upper edge of its distribution. They are not zero.

## Desert / Deep Dark

| Material / rock | Blocks/Y (change) | Blocks/blob (change) | Air discard |
|---|---:|---:|---|
| **Coal (overall)** | 2.4 (+67%) | 21.8 (0%) | 50% → 0/50% (−50/0 pp) |
| Coal (stone) | 2.95 (+7%) | 21.7 (0%) | 50% |
| Coal (deepslate) | 1.17 (+69,349%) | 21.9 (+93%) | 50% → 0/50% (−50/0 pp) |
| **Copper (overall)** | 3.61 (+345%) | 17.3 (+106%) | 0% |
| Copper (stone) | 4.17 (+200%) | 17.1 (+106%) | 0% |
| Copper (deepslate) | 2.37 (+2,845%) | 16.7 (+116%) | 0% |
| **Gold (overall)** | 1.93 (+139%) | 6.75 (0%) | 0/50% → 0/25% (0/−25 pp) |
| Gold (stone) | 2.04 (+63%) | 6.69 (0%) | 0/50% → 0/25% (0/−25 pp) |
| Gold (deepslate) | 1.7 (+345%) | 6.67 (0%) | 50% → 25% (−25 pp) |
| **Redstone (overall)** | 0.707 (+160%) | 5.71 (+2%) | 0% |
| Redstone (stone) | 0.0757 (new) | 5.23 (new) | 0% (new) |
| Redstone (deepslate) | 2.11 (+241%) | 5.67 (+2%) | 0% |
| **Andesite / diorite / granite, each (overall)** | 8.82 (-1%) | 631 (-7%) | 0% |
| Andesite / diorite / granite, each (stone) | 12.8 (0%) | 631 (0%) | 0% |
| **Tuff (overall)** | 1.57 (-81%) | 101 (-85%) | 0% |
| Tuff (deepslate) | 5.07 (-75%) | 101 (-85%) | 0% |
| **Cinnabar vs. vanilla andesite (overall)** | 8.82 (-1%) | 631 (-7%) | 0% |
| Cinnabar vs. vanilla andesite (stone) | 12.8 (0%) | 631 (0%) | 0% |
| **Sulfur vs. vanilla andesite (overall)** | 8.82 (-1%) | 631 (-7%) | 0% |
| Sulfur vs. vanilla andesite (stone) | 12.8 (0%) | 631 (0%) | 0% |

Iron, diamond, lapis, gravel and dirt blobs are absent in both custom rock regions; emerald is absent in both versions. Deepslate coal uses the same tiny vanilla baseline as Snowy. Sulfur replaces literal stone only; these buried deposits use vanilla andesite as their comparator.

## Nether

Each custom biome averages over its own **32 rock layers**; its vanilla comparator averages over the **full 128-layer Nether, 0≤Y<128**, with no caves. Use the corresponding vanilla biome, including Basalt Deltas’ doubled quartz/gold placements. Vanilla host is solid netherrack; custom hosts follow their biome. Density changes therefore describe concentration per layer, not total ore per column.

| Custom biome | Material | Blocks/Y (change) | Blocks/blob (change) |
|---|---|---:|---:|
| Basalt Deltas | Quartz | 5.72 (+50%) | 15.2 (0%) |
| Basalt Deltas | Gold | 2.36 (+80%) | 8.4 (0%) |
| Basalt Deltas | Ancient debris | 1 (+5,926%) | 4.95 (+230%) |
| Basalt Deltas | Magma | 5.1 (+45%) | 40.8 (-64%) |
| Basalt Deltas | Obsidian | 2.04 (new) | 32.7 (new) |
| Basalt Deltas | Embedded glowstone | 4.72 (new) | 50.4 (new) |
| Crimson Forest | Quartz | 1.91 (0%) | 14.4 (-5%) |
| Crimson Forest | Gold | 0.787 (+20%) | 8.02 (-4%) |
| Crimson Forest | Ancient debris | 0.5 (+2,914%) | 4.66 (+211%) |
| Warped Forest | Quartz | 1.82 (-5%) | 14.8 (-3%) |
| Warped Forest | Gold | 0.75 (+14%) | 8.2 (-2%) |
| Warped Forest | Ancient debris | 0.5 (+2,913%) | 4.66 (+211%) |
| Soul Sand Valley | Quartz | 2.89 (+51%) | 14.7 (-3%) |
| Soul Sand Valley | Gold | 0.447 (-32%) | 8.17 (-3%) |
| Soul Sand Valley | Ancient debris | 0.2 (+1,107%) | 4.86 (+224%) |

Air discard for **quartz/gold in Crimson Forest, Warped Forest and Soul Sand Valley is 0% → 100% (+100 pp)**. Basalt Deltas quartz/gold and magma stay at 0%; debris stays at 100% (Δ0 pp). New obsidian/glowstone blobs use 0%. Debris deposits can contain separate blocks. Magma keeps four attempts per chunk; size 22 gives about 60% less volume and density than the previous custom size 32. Nether sulfur needs lava and is excluded from this lava-free model; Desert sulfur above is an ordinary stone deposit. Hanging glowstone needs cavities and is also excluded.

## Blob size

The restored stone blobs use **size 64**, averaging about **683 blocks** in unrestricted solid host rock. Vanilla tuff averages about **665** after world-floor clipping, or **659** within Y<0. The former size-32 blobs averaged about **104 blocks**, roughly 85% less. Size controls length and sphere radii, not a block limit. Regional clipping explains the remaining differences in blocks/blob; counts are calibrated to match regional density. These are ordinary placed blobs, unrelated to large copper/iron mineral veins.

## Method and limits

50,000 geometry simulations per size, weighted by the exact placement-height probabilities and clipped to terrain/host regions, including cross-boundary spill. Weighted count providers realize fractional expected attempts across chunks. The estimates precede overlap and competition between features. Bedrock/soil losses, geodes, sediment disks, third-party additions and mining drops are excluded; a covering layer prevents air exposure. Large noise veins are excluded from vanilla calculations and disabled in the custom dimensions.

Sources: [ore placements](C:/Users/andre/Downloads/Games/September2024Mod/src/main/resources/data/aoemod/worldgen/placed_feature/dimension_ores), [blob placements](C:/Users/andre/Downloads/Games/September2024Mod/src/main/resources/data/aoemod/worldgen/placed_feature/dimension_blobs), their configured features, and local Minecraft 1.20.1 data/source JARs. [Simulation and per-Y results](C:/Users/andre/Downloads/Games/September2024Mod/build/ore-report) are generated local files; `split_comparison.py` and `write_split_report.py` produce this revision.

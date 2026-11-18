package net.amurdza.examplemod.worldgen;

import net.amurdza.examplemod.worldgen.tree_decorators.MangroveMulberryDecorator;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

/** A single pass owns tropical land vegetation, including chunks that contain several biomes. */
public final class TropicalVegetation {
    public static final int PATCH_WIDTH = 6;
    public static final double CHERRY_FLOWER_PATCH_CHANCE = 0.625;
    private static final List<String> RAINFOREST_TREES = List.of(
            "aoemod:rainforest/tree/natural_jungle", "aoemod:rainforest/tree/natural_jungle", "aoemod:rainforest/tree/natural_jungle",
            "aoemod:rainforest/tree/natural_dark_oak", "aoemod:rainforest/tree/natural_dark_oak",
            "aoemod:rainforest/tree/natural_fancy_oak", "aoemod:rainforest/tree/natural_fancy_oak",
            "minecraft:huge_red_mushroom", "minecraft:huge_brown_mushroom");
    private static final Set<String> GRASSES = Set.of("minecraft:grass", "minecraft:fern", "minecraft:large_fern");
    private static final Set<String> CHERRY_FLOWERS = Set.of("minecraft:allium", "minecraft:peony", "minecraft:lilac",
            "minecraft:pink_petals", "biomesoplenty:pink_daffodil", "biomeswevegone:japanese_orchid");
    private record MushroomAttempt(BlockPos origin, String feature) { }

    private TropicalVegetation() { }

    public static boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        RandomSource random = context.random();
        int chunkX = Math.floorDiv(context.origin().getX(), 16) * 16;
        int chunkZ = Math.floorDiv(context.origin().getZ(), 16) * 16;
        TropicalLayout layout = layout(level);
        TropicalIslandPlan plan = TropicalIslandPlan.forChunk(layout, Math.floorDiv(chunkX, 16), Math.floorDiv(chunkZ, 16));
        boolean placed = false;
        String forestPlant = plan.rainforestSpecies(Math.floorDiv(chunkX, 16), Math.floorDiv(chunkZ, 16));
        if (forestPlant != null) placed |= placeReservedLandPatch(level, chunkX, chunkZ, "rainforest", forestPlant, random) > 0;
        String crop = plan.cherryCrop(Math.floorDiv(chunkX, 16), Math.floorDiv(chunkZ, 16));
        if (crop != null) placed |= placeRareCropPatch(level, chunkX, chunkZ, crop, random) > 0;
        placed |= placeMoundPatch(level, chunkX, chunkZ, random) > 0;
        String wetCrop = plan.wetCrop(Math.floorDiv(chunkX, 16), Math.floorDiv(chunkZ, 16));
        if (wetCrop != null) {
            var candidates = wetCandidates(level, chunkX, chunkZ, true, true);
            int roots = placeReservedWetPatch(level, candidates, wetCrop, random, chunkX, chunkZ,
                    pos -> plan.wetCropRoot(pos.getX(), pos.getZ()));
            // Old neighboring chunks can already contain reeds on the new footprint.
            if (roots == 0) roots = placeReservedWetPatch(level, candidates, wetCrop, random, chunkX, chunkZ, pos -> true);
            placed |= roots > 0;
        }
        List<MushroomAttempt> mushrooms = new java.util.ArrayList<>();
        // Restore the sixteen planting cells from the native corner-log revision.
        for (int cellX = 0; cellX < 4; cellX++) {
            for (int cellZ = 0; cellZ < 4; cellZ++) {
                BlockPos floor = findGround(level, chunkX + cellX * 4 + random.nextInt(4),
                        chunkZ + cellZ * 4 + random.nextInt(4));
                if (floor == null) continue;
                String biome = biome(level, floor);
                var sample = layout.sample(floor.getX(), floor.getZ());
                if (biome.equals("rainforest") && level.getFluidState(floor).isEmpty()
                        && sample.distance() >= sample.cherryEdge() + TropicalLayout.RAINFOREST_TREE_SETBACK) {
                    String feature = RAINFOREST_TREES.get(random.nextInt(RAINFOREST_TREES.size()));
                    if (feature.equals("minecraft:huge_red_mushroom") || feature.equals("minecraft:huge_brown_mushroom")) {
                        mushrooms.add(new MushroomAttempt(floor, feature));
                    } else {
                        placed |= placeTree(context, floor, feature);
                    }
                }
            }
        }
        placed |= placeMangroveTree(context, chunkX, chunkZ);
        // Finish woody crowns before mushrooms test their complete stem columns.
        // Trees from later neighbouring chunks still preserve stems and caps via
        // the shared atomic collision and native foliage-placement rules.
        for (MushroomAttempt mushroom : mushrooms) {
            placed |= placeTree(context, mushroom.origin(), mushroom.feature());
        }
        // Cherry groves use their own seeded spatial pass with the vanilla tree
        // shape, keeping independent crowns apart on the gentler terrain.
        // This pass adds understory without a second layer of cherry trees.
        // Grasses remain scattered; flowers and wild crops share compact patches.
        for (int i = 0; i < 64; i++) {
            BlockPos floor = findGround(level, chunkX + random.nextInt(16), chunkZ + random.nextInt(16));
            if (floor == null || !level.isEmptyBlock(floor)) continue;
            String biome = biome(level, floor);
            if (!List.of("rainforest", "cherry_grove").contains(biome)) continue;
            String grass = biome.equals("cherry_grove") ? "minecraft:grass"
                    : random.nextInt(4) == 0 ? "minecraft:large_fern"
                    : random.nextBoolean() ? "minecraft:fern" : "minecraft:grass";
            placed |= DocumentedFloraFeature.placePlant(level, floor, grass, random);
        }
        // Count the original ferns before the established grass expansion. Adding
        // the second fern pass afterwards leaves grass and large-fern counts alone.
        int originalFerns = countRoots(level, chunkX, chunkZ, Blocks.FERN);
        placed |= expandRainforestGrass(level, chunkX, chunkZ, random) > 0;
        placed |= addFerns(level, chunkX, chunkZ, originalFerns, random) > 0;
        if (crop == null) {
            placed |= placeCherryFlowerPatch(level, chunkX, chunkZ, random) > 0;
            placed |= placeCherryFlowerPatch(level, chunkX, chunkZ, random) > 0;
        }
        placed |= placeMoundGrass(level, chunkX, chunkZ, random) > 0;
        placed |= placeWetPatches(level, chunkX, chunkZ, random);
        placed |= TropicalMoundFauna.place(context, layout);
        return placed;
    }

    public static int placePatch(WorldGenLevel level, BlockPos centre, String biome, String species, RandomSource random) {
        return placePatch(level, centre, biome, species, random, PATCH_WIDTH, pos -> true);
    }

    private static int placePatch(WorldGenLevel level, BlockPos centre, String biome, String species, RandomSource random,
                                  int width, java.util.function.Predicate<BlockPos> allowed) {
        boolean spacedFlowers = biome.equals("cherry_grove") && isFlower(species);
        boolean tall = isTallFlower(species);
        int target = species.equals("minecraft:azalea") || species.equals("minecraft:flowering_azalea") ? 6 + random.nextInt(4)
                : tall ? 3 + random.nextInt(3)
                : species.equals("minecraft:pink_petals") ? 6 + random.nextInt(5)
                : isFlower(species) ? 5 + random.nextInt(4)
                : species.equals("minecraft:melon") ? 2 : 5;
        int count = 0;
        // Visit each of the 36 possible rooted cells once, in random order. The
        // footprint stays six blocks wide even on a slope or a blocked floor.
        for (int offset : patchOffsets(random, width)) {
            if (count >= target) break;
            int low = (width - 1) / 2;
            int x = centre.getX() - low + offset % width;
            int z = centre.getZ() - low + offset / width;
            BlockPos floor = findGround(level, x, z);
            if (floor == null || !allowed.test(floor) || !patchBiome(level, floor, biome, species)
                    || !level.getBlockState(floor).canBeReplaced()
                    || !level.getFluidState(floor).isEmpty()) continue;
            if (spacedFlowers && hasNearbyFlower(level, floor)) continue;
            if (placePatchPlant(level, floor, biome, species, random)) {
                count++;
            }
        }
        return count;
    }

    public static int placeMoundPatch(WorldGenLevel level, int minX, int minZ, RandomSource random) {
        TropicalLayout layout = DimensionTerrain.tropicalLayout(level.getLevel().getChunkSource().randomState().router().temperature());
        int chunkX = Math.floorDiv(minX, 16), chunkZ = Math.floorDiv(minZ, 16);
        if (!layout.mangroveLandChunk(chunkX, chunkZ)) return 0;
        String species = TropicalIslandPlan.forChunk(layout, chunkX, chunkZ).moundSpecies(chunkX, chunkZ);
        return species == null ? 0 : placeReservedLandPatch(level, minX, minZ, "mangrove_swamp", species, random);
    }

    /** Keep the established fern pass, then add only grass until its actual cover doubles. */
    public static int expandRainforestGrass(WorldGenLevel level, int minX, int minZ, RandomSource random) {
        return expandForestGrass(level, minX, minZ, "rainforest", random, Set.of());
    }

    private static int expandForestGrass(WorldGenLevel level, int minX, int minZ, String habitat,
                                         RandomSource random, Set<BlockPos> skippedFerns) {
        List<BlockPos> open = new java.util.ArrayList<>();
        int cover = 0;
        for (int offset : patchOffsets(random, 16)) {
            BlockPos floor = findGround(level, minX + offset % 16, minZ + offset / 16);
            if (floor == null || !biome(level, floor).equals(habitat)) continue;
            BlockState state = level.getBlockState(floor);
            if (state.is(Blocks.GRASS) || state.is(Blocks.FERN) || state.is(Blocks.LARGE_FERN)
                    || skippedFerns.contains(floor)) cover++;
            else if (state.isAir()) open.add(floor);
        }
        int added = 0;
        for (BlockPos pos : open) {
            if (added >= cover) break;
            if (DocumentedFloraFeature.placePlant(level, pos, "minecraft:grass", random)) added++;
        }
        return added;
    }

    private static int countRoots(WorldGenLevel level, int minX, int minZ, net.minecraft.world.level.block.Block plant) {
        int count = 0;
        for (int x = minX; x < minX + 16; x++) for (int z = minZ; z < minZ + 16; z++) {
            BlockPos floor = findGround(level, x, z);
            if (floor != null && biome(level, floor).equals("rainforest") && level.getBlockState(floor).is(plant)) count++;
        }
        return count;
    }

    public static int addFerns(WorldGenLevel level, int minX, int minZ, int target, RandomSource random) {
        return addFerns(level, minX, minZ, target, "rainforest", random);
    }

    private static int addFerns(WorldGenLevel level, int minX, int minZ, int target, String habitat, RandomSource random) {
        int added = 0;
        for (int offset : patchOffsets(random, 16)) {
            if (added >= target) break;
            BlockPos floor = findGround(level, minX + offset % 16, minZ + offset / 16);
            if (floor != null && biome(level, floor).equals(habitat) && level.isEmptyBlock(floor)
                    && DocumentedFloraFeature.placePlant(level, floor, "minecraft:fern", random)) added++;
        }
        return added;
    }

    private static int placeMoundGrass(WorldGenLevel level, int minX, int minZ, RandomSource random) {
        // A dry mangrove column needs understory even when its chunk centre is
        // rainforest or water. The old centre-based shore gate left bare strips.
        int total = 0;
        Set<BlockPos> skippedFerns = new java.util.HashSet<>();
        int extraFerns = 0;
        for (int i = 0; i < 64; i++) {
            BlockPos pos = findGround(level, minX + random.nextInt(16), minZ + random.nextInt(16));
            if (pos == null || !biome(level, pos).equals("mangrove_swamp")
                    || !level.getFluidState(pos).isEmpty() || !level.isEmptyBlock(pos) || skippedFerns.contains(pos)) continue;
            boolean formerLargeFern = random.nextInt(4) == 0;
            boolean fern = !formerLargeFern && random.nextBoolean();
            if (fern || formerLargeFern) {
                if (!Blocks.FERN.defaultBlockState().canSurvive(level, pos)) continue;
                if (fern && random.nextBoolean()) extraFerns++;
                if (!random.nextBoolean()) {
                    skippedFerns.add(pos);
                    continue;
                }
            }
            if (DocumentedFloraFeature.placePlant(level, pos,
                    formerLargeFern || fern ? "minecraft:fern" : "minecraft:grass", random)) total++;
        }
        // Count the original footprint for the independent grass expansion.
        total += expandForestGrass(level, minX, minZ, "mangrove_swamp", random, skippedFerns);
        total += addFerns(level, minX, minZ, extraFerns, "mangrove_swamp", random);
        return total;
    }

    /** Each independent sparse footprint stays wholly contained in its owning chunk. */
    public static int placeCherryFlowerPatch(WorldGenLevel level, int chunkX, int chunkZ, RandomSource random) {
        if (random.nextDouble() >= CHERRY_FLOWER_PATCH_CHANCE) return 0;
        // The selected two-by-two central square starts at local coordinates 3..11.
        // Extending it by two cells on each side keeps all six cells off the next chunk.
        int localX = 3 + random.nextInt(9), localZ = 3 + random.nextInt(9);
        BlockPos centre = findGround(level, chunkX + localX, chunkZ + localZ);
        if (centre == null || !biome(level, centre).equals("cherry_grove")) return 0;
        var sample = layout(level).sample(centre.getX(), centre.getZ());
        if (sample.distance() > sample.cherryEdge() - 9) return 0;
        String selected = TropicalIslandPlan.forChunk(layout(level), Math.floorDiv(chunkX, 16), Math.floorDiv(chunkZ, 16))
                .cherryFlower(Math.floorDiv(chunkX, 16), Math.floorDiv(chunkZ, 16));
        if (selected == null) return 0;
        return placePatch(level, centre, "cherry_grove", selected, random);
    }

    public static boolean placeMangroveTree(FeaturePlaceContext<NoneFeatureConfiguration> context, int minX, int minZ) {
        var level = context.level();
        TropicalLayout layout = DimensionTerrain.tropicalLayout(level.getLevel().getChunkSource().randomState().router().temperature());
        int chunkX = Math.floorDiv(minX, 16), chunkZ = Math.floorDiv(minZ, 16);
        if (!selectMangroveTreeChunk(level.getSeed(), chunkX, chunkZ)) return false;
        var island = layout.nearest(minX + 8, minZ + 8);
        var fruitingSites = TropicalMulberryTrees.sites(layout, island);
        if (TropicalMulberryTrees.selected(fruitingSites, chunkX, chunkZ)) {
            // A seed belongs to the reservation, not to chunk decoration order.
            // Failed roots or fewer than six low fruit anchors discard the entire shape.
            RandomSource treeRandom = RandomSource.create(TropicalLayout.mix(layout.seed() ^ island.hash()
                    ^ chunkX * 341873128712L ^ chunkZ * 132897987541L));
            for (int attempt = 0; attempt < 256; attempt++) {
                int x = minX + 8 + (attempt < 64 ? 0 : treeRandom.nextInt(5) - 2);
                int z = minZ + 8 + (attempt < 64 ? 0 : treeRandom.nextInt(5) - 2);
                BlockPos floor = findGround(level, x, z);
                if (floor == null || !biome(level, floor).equals("mangrove_swamp") || waterDepth(level, floor) > 6) continue;
                var selected = new FeaturePlaceContext<>(context.topFeature(), level, context.chunkGenerator(),
                        treeRandom, floor, context.config());
                if (placeTree(selected, floor, "aoemod:rainforest/tree/natural_mangrove", true)) return true;
            }
            return false;
        }
        // Retry positions within the selected chunk so an obstructed shoreline
        // origin does not turn the requested half of chunks into a tiny frequency.
        int[] candidates = patchOffsets(context.random(), 10);
        for (int attempt = 0; attempt < 64; attempt++) {
            int offset = candidates[attempt];
            BlockPos floor = findGround(level, minX + 3 + offset % 10, minZ + 3 + offset / 10);
            if (floor != null && biome(level, floor).equals("mangrove_swamp") && waterDepth(level, floor) <= 6
                    && !TropicalMulberryTrees.reserved(fruitingSites, floor.getX(), floor.getZ())
                    && placeTree(context, floor, "aoemod:rainforest/tree/natural_mangrove")) return true;
        }
        return false;
    }

    public static boolean selectMangroveTreeChunk(long seed, int chunkX, int chunkZ) {
        return !TropicalLayout.moundCheckerboardChunk(chunkX, chunkZ);
    }

    public static boolean selectRareCropChunk(long seed, int chunkX, int chunkZ, String crop) {
        return crop.equals(TropicalIslandPlan.forChunk(new TropicalLayout(seed), chunkX, chunkZ).cherryCrop(chunkX, chunkZ));
    }

    public static int placeRareCropPatch(WorldGenLevel level, int chunkX, int chunkZ, String crop, RandomSource random) {
        return placeReservedLandPatch(level, chunkX, chunkZ, "cherry_grove", crop, random);
    }

    private static int placeReservedLandPatch(WorldGenLevel level, int chunkX, int chunkZ, String habitat,
                                               String species, RandomSource random) {
        List<BlockPos> candidates = new java.util.ArrayList<>();
        for (int x = chunkX + 2; x <= chunkX + 12; x += 2) {
            for (int z = chunkZ + 2; z <= chunkZ + 12; z += 2) {
                BlockPos floor = findGround(level, x, z);
                if (floor != null && patchBiome(level, floor, habitat, species)
                        && level.getFluidState(floor).isEmpty()) candidates.add(floor);
            }
        }
        GrowthPlacementBuffer best = null;
        int bestCount = 0;
        // Stage each attempt so retries never produce extra patches or plants.
        while (!candidates.isEmpty()) {
            BlockPos centre = candidates.remove(random.nextInt(candidates.size()));
            GrowthPlacementBuffer buffer = new GrowthPlacementBuffer(level);
            int count = placePatch(buffer, centre, habitat, species, random);
            int minimum = species.endsWith(":azalea") || species.endsWith(":flowering_azalea") ? 6
                    : isTallFlower(species) ? 3 : species.equals("minecraft:melon") ? 2 : 5;
            if (count >= minimum && buffer.commit()) return count;
            if (count > bestCount) { best = buffer; bestCount = count; }
        }
        return best != null && best.commit() ? bestCount : 0;
    }

    private static boolean placeWetPatches(WorldGenLevel level, int chunkX, int chunkZ, RandomSource random) {
        List<BlockPos> shallow = wetCandidates(level, chunkX, chunkZ, true);
        List<BlockPos> wet = wetCandidates(level, chunkX, chunkZ, false);
        boolean placed = false;
        TropicalIslandPlan plan = TropicalIslandPlan.forChunk(layout(level), Math.floorDiv(chunkX, 16), Math.floorDiv(chunkZ, 16));
        // The two-block crop/reed separation also applies before an adjacent
        // chunk decorates. Deep plants below that vertical range stay eligible.
        Predicate<BlockPos> outsideCrop = pos -> pos.getY() < TropicalChannelSurface.WATER_SURFACE_Y - 2
                || !plan.nearWetCropRoot(pos.getX(), pos.getZ());
        int attempts = plan.pickerelweedAttempts(Math.floorDiv(chunkX, 16), Math.floorDiv(chunkZ, 16));
        // The plan halves the previous shallow allocation again while retaining
        // every deep-water opportunity. Start shallow patches at their edge.
        for (int patch = 0; patch < attempts && !wet.isEmpty(); patch++) {
            List<BlockPos> candidates = shallow.isEmpty() ? wet : shallow;
            for (int retry = 0; retry < 8; retry++) {
                BlockPos centre = candidates.get(random.nextInt(candidates.size()));
                if (!level.getBlockState(centre).is(Blocks.WATER)) continue;
                if (placeWetPatch(level, centre, "upgrade_aquatic:tall_pickerelweed", random, outsideCrop) > 0) { placed = true; break; }
            }
        }
        String dripleaf = plan.dripleafSpecies(Math.floorDiv(chunkX, 16), Math.floorDiv(chunkZ, 16));
        return placeDripleafPatches(level, shallow, chunkX, chunkZ, dripleaf, random, outsideCrop) > 0 || placed;
    }

    public static int placeDripleafPatches(WorldGenLevel level, int minX, int minZ, String species, RandomSource random) {
        return placeDripleafPatches(level, wetCandidates(level, minX, minZ, true), minX, minZ, species, random, pos -> true);
    }

    private static int placeDripleafPatches(WorldGenLevel level, List<BlockPos> shallow, int minX, int minZ,
                                          String species, RandomSource random, Predicate<BlockPos> allowed) {
        if (!isDripleaf(species)) return 0;
        // Both attempts use the chunk's one selected type. At 75% small / 25%
        // large this yields 1.5 small and 0.5 large opportunities per chunk,
        // exactly half the previous three-small / one-large expectation.
        int placed = 0;
        for (int patch = 0; patch < 2 && !shallow.isEmpty(); patch++) {
            placed += placeReservedWetPatch(level, shallow, species, random, minX, minZ, allowed);
        }
        // The later seabed pass fills the untouched water between large roots
        // with seagrass or pickles; small dripleaves never fill those gaps.
        return placed;
    }

    private static List<BlockPos> wetCandidates(WorldGenLevel level, int chunkX, int chunkZ, boolean shallowOnly) {
        return wetCandidates(level, chunkX, chunkZ, shallowOnly, false);
    }

    private static List<BlockPos> wetCandidates(WorldGenLevel level, int chunkX, int chunkZ, boolean shallowOnly,
                                                boolean crop) {
        List<BlockPos> result = new java.util.ArrayList<>();
        for (int x = chunkX; x < chunkX + 16; x++) {
            for (int z = chunkZ; z < chunkZ + 16; z++) {
                BlockPos floor = findGround(level, x, z);
                if (floor != null && biome(level, floor).equals("mangrove_swamp")
                        && (level.getBlockState(floor).is(Blocks.WATER)
                            || crop && replaceableSeabed(level.getBlockState(floor)))) {
                    if (!shallowOnly || AquaticPlantPlacement.waterColumnHeight(level, floor, 2) == 1) result.add(floor);
                }
            }
        }
        return result;
    }

    private static int placeReservedWetPatch(WorldGenLevel level, List<BlockPos> candidates, String species,
                                             RandomSource random, int minX, int minZ, Predicate<BlockPos> allowed) {
        // Opposite dripleaf types in neighbouring chunks remain at least five
        // blocks apart, independently of which chunk decorates first. Crops
        // retain their entire chunk and all existing reserved patch locations.
        int margin = isDripleaf(species) ? 2 : 0;
        Predicate<BlockPos> inside = pos -> allowed.test(pos) && pos.getX() >= minX + margin && pos.getX() < minX + 16 - margin
                && pos.getZ() >= minZ + margin && pos.getZ() < minZ + 16 - margin;
        List<BlockPos> remaining = new java.util.ArrayList<>(candidates.stream().filter(inside).toList());
        if (isWetCrop(species)) {
            while (!remaining.isEmpty()) {
                BlockPos centre = remaining.remove(random.nextInt(remaining.size()));
                if (placeCropCluster(level, centre, species, random, inside)) return 5;
            }
            return 0;
        }
        GrowthPlacementBuffer best = null;
        int bestCount = 0;
        while (!remaining.isEmpty()) {
            BlockPos centre = remaining.remove(random.nextInt(remaining.size()));
            GrowthPlacementBuffer buffer = new GrowthPlacementBuffer(level);
            int count = placeWetPatch(buffer, centre, species, random, inside);
            if (count >= 5 && buffer.commit()) return count;
            if (count > bestCount) { best = buffer; bestCount = count; }
        }
        return best != null && best.commit() ? bestCount : 0;
    }

    public static int placeWetPatch(WorldGenLevel level, BlockPos centre, String species, RandomSource random) {
        return placeWetPatch(level, centre, species, random, pos -> true);
    }

    private static int placeWetPatch(WorldGenLevel level, BlockPos centre, String species, RandomSource random,
                                     java.util.function.Predicate<BlockPos> allowed) {
        if (isWetCrop(species)) return placeCropCluster(level, centre, species, random, allowed) ? 5 : 0;
        if (species.equals("upgrade_aquatic:pickerelweed") || species.equals("upgrade_aquatic:tall_pickerelweed")) {
            return placePickerelweedPatch(level, centre, random, allowed);
        }
        int count = 0;
        int target = 4 + random.nextInt(4);
        for (int offset : patchOffsets(random)) {
            if (count >= target) break;
            BlockPos floor = findGround(level, centre.getX() - 2 + offset % PATCH_WIDTH,
                    centre.getZ() - 2 + offset / PATCH_WIDTH);
            if (floor != null && allowed.test(floor) && biome(level, floor).equals("mangrove_swamp")
                    && level.getBlockState(floor).is(Blocks.WATER)
                    && AquaticPlantPlacement.waterColumnHeight(level, floor, 2) == 1
                    && (!isDripleaf(species) || !hasOppositeDripleafNearby(level, floor, species)
                        && !nearShorePlant(level, floor, false))
                    && (!species.equals("minecraft:big_dripleaf") || !hasAdjacentLargeDripleaf(level, floor))
                    && placePatchPlant(level, floor, "mangrove_swamp", species, random)) count++;
        }
        return count;
    }

    private static boolean isDripleaf(String species) {
        return species.equals("minecraft:small_dripleaf") || species.equals("minecraft:big_dripleaf");
    }

    private static boolean isWetCrop(String species) {
        return species.equals("farmersdelight:wild_rice") || species.equals("minecraft:sugar_cane");
    }

    private static boolean replaceableSeabed(BlockState state) {
        return state.is(Blocks.SEA_PICKLE) || state.is(Blocks.SEAGRASS);
    }

    private static boolean nearShorePlant(WorldGenLevel level, BlockPos pos, boolean checkingCrop) {
        for (int dx = -2; dx <= 2; dx++) for (int dz = -2; dz <= 2; dz++) {
            for (int dy = -1; dy <= 1; dy++) {
                BlockState state = level.getBlockState(pos.offset(dx, dy, dz));
                if (state.isAir() || state.is(Blocks.WATER)) continue;
                var id = ForgeRegistries.BLOCKS.getKey(state.getBlock());
                if (id == null) continue;
                if (checkingCrop ? isDripleaf(id.toString()) || id.getPath().equals("big_dripleaf_stem")
                        || id.getPath().endsWith("pickerelweed") : isWetCrop(id.toString())) return true;
            }
        }
        return false;
    }

    private static boolean placeCropCluster(WorldGenLevel level, BlockPos centre, String species,
                                             RandomSource random, java.util.function.Predicate<BlockPos> allowed) {
        GrowthPlacementBuffer cluster = new GrowthPlacementBuffer(level);
        var frontier = new java.util.ArrayList<BlockPos>();
        var visited = new java.util.HashSet<BlockPos>();
        frontier.add(centre);
        visited.add(centre);
        int planted = 0;
        while (!frontier.isEmpty()) {
            BlockPos pos = frontier.remove(random.nextInt(frontier.size()));
            if (!allowed.test(pos) || !biome(cluster, pos).equals("mangrove_swamp")
                    || AquaticPlantPlacement.waterColumnHeight(cluster, pos, 2) != 1
                    || nearShorePlant(cluster, pos, true)) continue;
            BlockState old = cluster.getBlockState(pos);
            if (replaceableSeabed(old)) cluster.setBlock(pos, Blocks.WATER.defaultBlockState(), 18);
            else if (!old.is(Blocks.WATER)) continue;
            if (!placePatchPlant(cluster, pos, "mangrove_swamp", species, random)) {
                if (replaceableSeabed(old)) cluster.setBlock(pos, old, 18);
                continue;
            }
            if (++planted == 5) return cluster.commit();
            for (var dir : net.minecraft.core.Direction.Plane.HORIZONTAL) {
                BlockPos next = pos.relative(dir);
                if (next.getX() >= centre.getX() - 2 && next.getX() <= centre.getX() + 3
                        && next.getZ() >= centre.getZ() - 2 && next.getZ() <= centre.getZ() + 3
                        && visited.add(next)) frontier.add(next);
            }
        }
        return false;
    }

    private static boolean hasOppositeDripleafNearby(WorldGenLevel level, BlockPos pos, String species) {
        boolean placingLarge = species.equals("minecraft:big_dripleaf");
        for (int dx = -3; dx <= 3; dx++) for (int dz = -3; dz <= 3; dz++) {
            for (int dy = -2; dy <= 2; dy++) {
                BlockState state = level.getBlockState(pos.offset(dx, dy, dz));
                if (placingLarge ? state.is(Blocks.SMALL_DRIPLEAF)
                        : state.is(Blocks.BIG_DRIPLEAF) || state.is(Blocks.BIG_DRIPLEAF_STEM)) return true;
            }
        }
        return false;
    }

    public static boolean hasAdjacentLargeDripleaf(WorldGenLevel level, BlockPos pos) {
        for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) {
            for (int dy = -2; dy <= 2; dy++) {
                BlockState state = level.getBlockState(pos.offset(dx, dy, dz));
                if (state.is(Blocks.BIG_DRIPLEAF) || state.is(Blocks.BIG_DRIPLEAF_STEM)) return true;
            }
        }
        return false;
    }

    /** A growing four-connected footprint, with no enclosed gaps, keeps the reeds continuous. */
    private static int placePickerelweedPatch(WorldGenLevel level, BlockPos centre, RandomSource random, Predicate<BlockPos> allowed) {
        boolean[] occupied = new boolean[PATCH_WIDTH * PATCH_WIDTH];
        int target = 10 + random.nextInt(9), count = 0;
        int deepRoots = 0;
        int[] order = patchOffsets(random);
        // Start at the chosen shoreline centre. A random first cell could move
        // the entire patch underwater even when its selected centre was shallow.
        int central = 2 + 2 * PATCH_WIDTH;
        for (int i = 0; i < order.length; i++) if (order[i] == central) {
            order[i] = order[0];
            order[0] = central;
            break;
        }
        boolean progress = true;
        while (count < target && progress) {
            progress = false;
            for (int offset : order) {
                if (occupied[offset] || count > 0 && !touchesPatch(occupied, offset)) continue;
                occupied[offset] = true;
                boolean hole = hasEnclosedGap(occupied);
                occupied[offset] = false;
                if (hole) continue;
                BlockPos floor = findGround(level, centre.getX() - 2 + offset % PATCH_WIDTH,
                        centre.getZ() - 2 + offset / PATCH_WIDTH);
                if (floor == null || !allowed.test(floor) || !biome(level, floor).equals("mangrove_swamp")
                        || !level.getBlockState(floor).is(Blocks.WATER) || nearShorePlant(level, floor, false)) continue;
                boolean deep = AquaticPlantPlacement.waterColumnHeight(level, floor, 2) >= 2;
                String plant = !deep || deepRoots == 1 || deepRoots > 1 && random.nextBoolean()
                        ? "upgrade_aquatic:tall_pickerelweed" : "upgrade_aquatic:pickerelweed";
                if (!placePatchPlant(level, floor, "mangrove_swamp", plant, random)) continue;
                if (deep) deepRoots++;
                occupied[offset] = true;
                progress = true;
                if (++count >= target) break;
            }
        }
        return count;
    }

    private static boolean touchesPatch(boolean[] cells, int index) {
        int x = index % PATCH_WIDTH, z = index / PATCH_WIDTH;
        return x > 0 && cells[index - 1] || x + 1 < PATCH_WIDTH && cells[index + 1]
                || z > 0 && cells[index - PATCH_WIDTH] || z + 1 < PATCH_WIDTH && cells[index + PATCH_WIDTH];
    }

    private static boolean hasEnclosedGap(boolean[] cells) {
        boolean[] reached = new boolean[cells.length];
        java.util.ArrayDeque<Integer> queue = new java.util.ArrayDeque<>();
        for (int i = 0; i < cells.length; i++) {
            if (!cells[i] && (i % PATCH_WIDTH == 0 || i % PATCH_WIDTH == PATCH_WIDTH - 1
                    || i / PATCH_WIDTH == 0 || i / PATCH_WIDTH == PATCH_WIDTH - 1)) {
                reached[i] = true;
                queue.add(i);
            }
        }
        while (!queue.isEmpty()) {
            int index = queue.removeFirst(), x = index % PATCH_WIDTH, z = index / PATCH_WIDTH;
            for (int next : new int[]{x > 0 ? index - 1 : -1, x + 1 < PATCH_WIDTH ? index + 1 : -1,
                    z > 0 ? index - PATCH_WIDTH : -1, z + 1 < PATCH_WIDTH ? index + PATCH_WIDTH : -1}) {
                if (next >= 0 && !cells[next] && !reached[next]) { reached[next] = true; queue.add(next); }
            }
        }
        for (int i = 0; i < cells.length; i++) if (!cells[i] && !reached[i]) return true;
        return false;
    }

    private static int[] patchOffsets(RandomSource random) {
        return patchOffsets(random, PATCH_WIDTH);
    }

    private static int[] patchOffsets(RandomSource random, int width) {
        int[] cells = new int[width * width];
        for (int i = 0; i < cells.length; i++) cells[i] = i;
        for (int i = cells.length - 1; i > 0; i--) {
            int swap = random.nextInt(i + 1);
            int previous = cells[swap];
            cells[swap] = cells[i];
            cells[i] = previous;
        }
        return cells;
    }

    private static boolean isFlower(String species) {
        // Farmer's Delight and companion wild crops keep their original target,
        // even when a compatibility tag also describes their flowering stage.
        String path = new ResourceLocation(species).getPath();
        if (path.startsWith("wild_") || path.endsWith("_wild_crop")) return false;
        var block = ForgeRegistries.BLOCKS.getValue(new ResourceLocation(species));
        return CHERRY_FLOWERS.contains(species) || block != null && block.defaultBlockState().is(BlockTags.FLOWERS);
    }

    private static boolean isTallFlower(String species) {
        var block = ForgeRegistries.BLOCKS.getValue(new ResourceLocation(species));
        return isFlower(species) && block != null && (block instanceof DoublePlantBlock
                || block.defaultBlockState().hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF));
    }

    private static boolean hasNearbyFlower(WorldGenLevel level, BlockPos pos) {
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if (Math.abs(dx) + Math.abs(dz) > 1) continue;
                for (int dy = -2; dy <= 2; dy++) {
                    BlockState state = level.getBlockState(pos.offset(dx, dy, dz));
                    if (state.is(BlockTags.FLOWERS)) return true;
                    ResourceLocation id = ForgeRegistries.BLOCKS.getKey(state.getBlock());
                    if (id != null && CHERRY_FLOWERS.contains(id.toString())) return true;
                }
            }
        }
        return false;
    }

    private static boolean placeTree(FeaturePlaceContext<NoneFeatureConfiguration> context, BlockPos origin, String id) {
        return placeTree(context, origin, id, false);
    }

    private static boolean placeTree(FeaturePlaceContext<NoneFeatureConfiguration> context, BlockPos origin, String id, boolean sixMulberries) {
        var plan = TropicalIslandPlan.forChunk(layout(context.level()), origin.getX() >> 4, origin.getZ() >> 4);
        BlockState current = context.level().getBlockState(origin);
        boolean mangroveFlower = id.equals("aoemod:rainforest/tree/natural_mangrove")
                && TropicalPlantIntegrity.flowerRootMayReplace(current);
        if (current.is(BlockTags.LOGS) || current.is(BlockTags.LEAVES)
                || !current.canBeReplaced() && !current.is(Blocks.WATER) && !mangroveFlower) return false;
        var tree = context.level().registryAccess().registryOrThrow(Registries.CONFIGURED_FEATURE)
                .get(new ResourceLocation(id));
        if (tree == null) return false;
        GrowthPlacementBuffer buffer = new GrowthPlacementBuffer(context.level());
        boolean generated = sixMulberries
                ? MangroveMulberryDecorator.withSixMulberries(
                        () -> tree.place(buffer, context.chunkGenerator(), context.random(), origin))
                : tree.place(buffer, context.chunkGenerator(), context.random(), origin);
        return generated
                && (!sixMulberries || MangroveMulberryDecorator
                        .hasSixSupportedMulberries(buffer))
                && buffer.isValid((pos, proposed) -> {
            var previous = context.level().getBlockState(pos);
            boolean wetCrop = pos.getY() >= TropicalChannelSurface.WATER_SURFACE_Y - 1
                    && pos.getY() <= TropicalChannelSurface.WATER_SURFACE_Y + 2 && plan.wetCropRoot(pos.getX(), pos.getZ());
            boolean displacedFlower = TropicalPlantIntegrity.flowerRootMayReplace(previous) && (TropicalPlantIntegrity.isMangroveRoot(proposed)
                    || previous.hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF)
                    && (proposed.isAir() || proposed.is(Blocks.MOSS_CARPET)
                    && previous.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF)
                    == net.minecraft.world.level.block.state.properties.DoubleBlockHalf.UPPER)
                    && TropicalPlantIntegrity.isMangroveRoot(buffer.getBlockState(previous.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF)
                    == net.minecraft.world.level.block.state.properties.DoubleBlockHalf.LOWER ? pos.above() : pos.below())));
            return proposed.equals(previous) || displacedFlower || !wetCrop && !reservedUnderstory(previous);
        }) && buffer.commit();
    }

    private static boolean reservedUnderstory(BlockState state) {
        ResourceLocation id = ForgeRegistries.BLOCKS.getKey(state.getBlock());
        if (id == null) return false;
        String path = id.getPath();
        return path.startsWith("wild_") || path.endsWith("_wild_crop") || state.is(BlockTags.FLOWERS)
                || state.is(Blocks.MELON) || state.is(Blocks.RED_MUSHROOM) || state.is(Blocks.BROWN_MUSHROOM)
                || state.is(Blocks.AZALEA) || state.is(Blocks.FLOWERING_AZALEA) || state.is(Blocks.SUGAR_CANE);
    }

    /** Ignore canopies and trunks; a tree can only start on actual soil. */
    public static BlockPos findGround(WorldGenLevel level, int x, int z) {
        int top = Math.min(level.getMaxBuildHeight() - 2,
                // WORLD_SURFACE_WG stops updating after carving. Structures
                // added during FEATURES are reflected only by WORLD_SURFACE.
                level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z));
        for (int y = top; y > level.getMinBuildHeight(); y--) {
            BlockPos ground = new BlockPos(x, y - 1, z);
            BlockState state = level.getBlockState(ground);
            if (state.is(BlockTags.DIRT) && !state.is(BlockTags.LOGS) && !state.is(BlockTags.LEAVES)
                    || state.is(Blocks.MOSS_BLOCK) || state.is(Blocks.MUD) || state.is(Blocks.CLAY)) {
                BlockPos surface = ground.above();
                // A roof opening must not send flowers, crops, ferns or tree
                // roots into the arena. The cave piece owns its grass/mushrooms.
                return net.amurdza.examplemod.worldgen.structure.BossLairPiece.isInsideSpiderCave(level, surface)
                        ? null : surface;
            }
            if (state.isFaceSturdy(level, ground, net.minecraft.core.Direction.UP)
                    && !state.is(BlockTags.LOGS) && !state.is(BlockTags.LEAVES)
                    && !state.is(Blocks.MUSHROOM_STEM) && !state.is(Blocks.RED_MUSHROOM_BLOCK)
                    && !state.is(Blocks.BROWN_MUSHROOM_BLOCK)) return null;
        }
        return null;
    }

    private static String biome(WorldGenLevel level, BlockPos pos) {
        return level.getBiome(pos).unwrapKey().map(key -> key.location())
                .filter(id -> id.getNamespace().equals("aoemod"))
                .map(ResourceLocation::getPath).orElse("");
    }

    private static TropicalLayout layout(WorldGenLevel level) {
        return DimensionTerrain.tropicalLayout(level.getLevel().getChunkSource().randomState().router().temperature());
    }

    private static boolean patchBiome(WorldGenLevel level, BlockPos pos, String expected, String species) {
        return biome(level, pos).equals(expected);
    }

    private static boolean placePatchPlant(WorldGenLevel level, BlockPos pos, String habitat, String species,
                                          RandomSource random) {
        GrowthPlacementBuffer buffer = new GrowthPlacementBuffer(level);
        return DocumentedFloraFeature.placePlant(buffer, pos, species, random)
                && buffer.isValid((target, state) -> patchBiome(level, target, habitat, species))
                && buffer.commit();
    }

    private static int waterDepth(WorldGenLevel level, BlockPos pos) {
        int depth = 0;
        while (depth <= 6 && level.getFluidState(pos.above(depth)).is(FluidTags.WATER)) depth++;
        return depth;
    }
}

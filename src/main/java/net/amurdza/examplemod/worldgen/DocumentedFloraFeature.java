package net.amurdza.examplemod.worldgen;

import alabaster.hearthandharvest.common.block.CornStalkBlock;
import net.amurdza.examplemod.util.DocumentedPlacementRules;
import net.amurdza.examplemod.util.GameplayDimensions;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.List;
import java.util.Map;
import java.util.Set;

/** Modest, local patches of the documented understory and wild crop sources on actual terrain floors. */
public final class DocumentedFloraFeature extends Feature<NoneFeatureConfiguration> {
    private static final Map<String, List<String>> LAND = Map.ofEntries(
            Map.entry("rainforest", ids("minecraft:fern minecraft:grass minecraft:large_fern minecraft:rose_bush minecraft:azalea minecraft:flowering_azalea minecraft:torchflower environmental:blue_delphinium environmental:purple_delphinium minecraft:torchflower minecraft:melon minecraft:red_mushroom minecraft:brown_mushroom pineapple_delight:pineapple_wild_crop farmersdelight:wild_tomatoes")),
            Map.entry("mangrove_swamp", ids("minecraft:blue_orchid minecraft:pitcher_plant minecraft:red_mushroom minecraft:brown_mushroom farmersdelight:wild_rice hearthandharvest:wild_cotton farmersdelight:wild_cabbages")),
            Map.entry("cherry_grove", ids("minecraft:grass minecraft:allium minecraft:peony minecraft:lilac minecraft:pink_petals biomesoplenty:pink_daffodil biomeswevegone:japanese_orchid legumedelight:wild_beans farmersdelight:wild_carrots")),
            Map.entry("savanna", ids("minecraft:grass minecraft:tall_grass minecraft:red_tulip minecraft:orange_tulip minecraft:pink_tulip minecraft:white_tulip floral:blue_tulip floral:yellow_tulip minecraft:red_mushroom minecraft:brown_mushroom farmersdelight:wild_carrots minecraft:pumpkin farmersdelight:wild_cabbages")),
            Map.entry("plains", ids("minecraft:grass minecraft:tall_grass minecraft:sunflower minecraft:dandelion minecraft:poppy minecraft:cornflower minecraft:oxeye_daisy minecraft:lily_of_the_valley farmersdelight:wild_carrots minecraft:pumpkin minecraft:red_mushroom minecraft:brown_mushroom farmersdelight:wild_cabbages minecraft:glow_lichen")),
            Map.entry("mushroom_caves", ids("minecraft:red_mushroom minecraft:brown_mushroom twilightforest:mushgloom quark:glow_shroom minecraft:glow_lichen")),
            Map.entry("desert", List.of()),
            Map.entry("mesa", List.of()),
            Map.entry("deep_dark", List.of()),
            Map.entry("crimson_forest", ids("minecraft:crimson_fungus minecraft:crimson_roots")),
            Map.entry("warped_forest", ids("minecraft:warped_fungus minecraft:warped_roots minecraft:nether_sprouts")),
            Map.entry("soul_sand_valley", ids("nourished_nether:soul_weeds")),
            Map.entry("basalt_deltas", ids("mynethersdelight:powdery_cane mynethersdelight:powdery_cannon"))
    );
    private static final List<String> TROPICAL_WATER = ids("minecraft:kelp minecraft:seagrass minecraft:tall_seagrass minecraft:wet_sponge minecraft:sea_pickle");
    private static final List<String> SHALLOW_WATER = ids("minecraft:kelp minecraft:seagrass minecraft:tall_seagrass minecraft:sea_pickle minecraft:wet_sponge");
    private static final List<String> DEEP_WATER = ids("alexscaves:ping_pong_sponge betterend:charnia_light_blue betterend:charnia_cyan alexscaves:bone_worms");
    // Tropical emergent cane, rice and dripleaf use dedicated six-by-six wet patches.
    private static final Set<String> WATER_ROOTED_SHORE = Set.of("minecraft:sugar_cane", "minecraft:big_dripleaf",
            "minecraft:small_dripleaf", "upgrade_aquatic:pickerelweed", "upgrade_aquatic:tall_pickerelweed");

    public DocumentedFloraFeature() { super(NoneFeatureConfiguration.CODEC); }

    private static List<String> ids(String text) { return List.of(text.split(" ")); }

    static List<String> landPalette(String biome) { return LAND.getOrDefault(biome, List.of()); }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        if (!GameplayDimensions.isCustom(level)) return false;
        if (GameplayDimensions.is(level, GameplayDimensions.NETHER)) return NetherVegetation.place(context);
        boolean beeTree = BeeNestTrees.applies(level) && BeeNestTrees.place(context);
        if (GameplayDimensions.is(level, GameplayDimensions.SAVANNA)) {
            return beeTree | SavannaVegetation.place(context) | ShoreSugarCane.placeChunk(level,
                    Math.floorDiv(context.origin().getX(), 16), Math.floorDiv(context.origin().getZ(), 16)) > 0;
        }
        RandomSource random = context.random();
        BlockPos origin = context.origin();
        int chunkX = Math.floorDiv(origin.getX(), 16) * 16;
        int chunkZ = Math.floorDiv(origin.getZ(), 16) * 16;
        boolean tropical = GameplayDimensions.is(level, GameplayDimensions.TROPICAL);
        boolean plains = GameplayDimensions.is(level, GameplayDimensions.PLAINS);
        if (plains) PlainsSurfaceWater.drainDisconnected(level, chunkX, chunkZ);
        if (GameplayDimensions.is(level, GameplayDimensions.SNOWY)) SnowyLakeVegetation.prepareBuriedBeds(level, chunkX, chunkZ);
        if (GameplayDimensions.is(level, GameplayDimensions.DESERT)) DeepDarkEcology.prepareSurfaces(level, chunkX, chunkZ);
        boolean placed = beeTree | (tropical && (TropicalVegetation.place(context) | TropicalCoralReefs.place(context)));
        for (int attempt = 0; attempt < 40; attempt++) {
            int x = chunkX + random.nextInt(16);
            int z = chunkZ + random.nextInt(16);
            int floors = 0;
            for (int y = level.getMaxBuildHeight() - 2; y > level.getMinBuildHeight(); y--) {
                BlockPos pos = new BlockPos(x, y, z);
                if (level.isEmptyBlock(pos) && GameplayDimensions.is(level, GameplayDimensions.TROPICAL)) {
                    if (level.getBlockState(pos.above()).is(BlockTags.LEAVES) && random.nextInt(12) == 0) {
                        if (GameplayDimensions.biomeIs(level, pos, "rainforest")) {
                            placed |= placePlant(level, pos, random.nextBoolean()
                                    ? "minecraft:cave_vines" : "minecraft:spore_blossom", random);
                        }
                    }
                    if (GameplayDimensions.biomeIs(level, pos, "rainforest") && random.nextInt(24) == 0) {
                        for (Direction direction : Direction.Plane.HORIZONTAL) {
                            if (level.getBlockState(pos.relative(direction)).is(BlockTags.LOGS_THAT_BURN)) {
                                BlockState cocoa = Blocks.COCOA.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, direction)
                                        .setValue(BlockStateProperties.AGE_2, 2);
                                if (cocoa.canSurvive(level, pos)) {
                                    level.setBlock(pos, cocoa, 2);
                                    placed = true;
                                }
                                break;
                            }
                        }
                    }
                }
                BlockState below = level.getBlockState(pos.below());
                if (!below.isFaceSturdy(level, pos.below(), Direction.UP) || below.is(BlockTags.LEAVES)
                        || below.is(BlockTags.LOGS) || below.is(Blocks.MUSHROOM_STEM)) continue;
                BlockState current = level.getBlockState(pos);
                if (!current.isAir() && !current.is(Blocks.WATER) && !current.is(Blocks.LAVA)) continue;
                String biome = level.getBiome(pos).unwrapKey().filter(key -> key.location().getNamespace().equals("aoemod"))
                        .map(key -> key.location().getPath()).orElse("");
                List<String> palette = palette(level, pos, biome);
                if (biome.equals("soul_sand_valley") && current.isAir() && random.nextInt(48) == 0) {
                    var tree = level.registryAccess().registryOrThrow(Registries.CONFIGURED_FEATURE)
                            .get(new ResourceLocation("aoemod:soul_sand_valley/pale_oak"));
                    if (tree != null) placed |= tree.place(level, context.chunkGenerator(), random, pos);
                }
                // Tropical land plants use compact same-species patches rather than
                // independent per-column picks, which made mod flowers singletons.
                if (plains && current.is(Blocks.WATER) && !palette.isEmpty()) {
                    String name = plainsWaterSelection(random, AquaticPlantPlacement.waterColumnHeight(level, pos, 2) == 1);
                    if (!name.isEmpty()) placed |= placePlant(level, pos, name, random);
                } else if (!palette.isEmpty() && (!tropical || !current.isAir()) && random.nextInt(3) != 0) {
                    int selection = random.nextInt(palette.size() + (plains && current.isAir() ? 2 : 0));
                    String name = selection < palette.size() ? palette.get(selection) : "";
                    // Retain the original lottery (including its grass/flower odds).
                    // Replaced Plains entries belong to compact surface/cave passes,
                    // so freeing their slots must not make short grass more common.
                    if (!name.isEmpty() && (!plains || !current.isAir() || !(PlainsVegetation.replacesScatteredPlant(name)
                            || PlainsFlowers.replacesScatteredPlant(name)))) {
                        placed |= placePlant(level, pos, name, random);
                    }
                }
                // The stacked Nether can have extra ledges/fungus caps above its
                // lowest gallery. Every band needs a chance to receive its plants.
                if (++floors >= 4 && !GameplayDimensions.is(level, GameplayDimensions.NETHER)
                        && !GameplayDimensions.is(level, GameplayDimensions.SNOWY)) break;
            }
        }
        if (plains) {
            placed |= PlainsVegetation.place(context);
            placed |= PlainsCaveVegetation.place(context);
            placed |= PlainsFlowers.place(context);
        }
        if (GameplayDimensions.is(level, GameplayDimensions.SNOWY)) {
            placed |= SnowyVegetation.place(context);
        }
        if (GameplayDimensions.is(level, GameplayDimensions.DESERT)) {
            placed |= DesertCaveVegetation.place(context);
            placed |= DeepDarkVegetation.place(context);
        }
        placed |= ShoreSugarCane.placeChunk(level, Math.floorDiv(chunkX, 16), Math.floorDiv(chunkZ, 16)) > 0;
        return placed;
    }

    /** Double short-grass and halve tall-grass opportunities in the secondary water pass. */
    public static String plainsWaterSelection(RandomSource random, boolean shallow) {
        // Previously each grass had probability 1/3, or 2/9 in one-deep water
        // because of the retired wet-cane slot. Keep its unused share unused.
        int selection = random.nextInt(shallow ? 18 : 6);
        if (selection < (shallow ? 8 : 4)) return "minecraft:seagrass";
        if (selection < (shallow ? 10 : 5)) return "minecraft:tall_seagrass";
        return "";
    }

    private static List<String> palette(WorldGenLevel level, BlockPos pos, String biome) {
        if (level.getFluidState(pos).is(FluidTags.LAVA)) {
            return switch (biome) {
                case "crimson_forest" -> ids("netherdepthsupgrade:crimson_kelp netherdepthsupgrade:crimson_seagrass netherdepthsupgrade:tall_crimson_seagrass");
                case "warped_forest" -> ids("netherdepthsupgrade:warped_kelp netherdepthsupgrade:warped_seagrass netherdepthsupgrade:tall_warped_seagrass");
                default -> List.of();
            };
        }
        if (level.getFluidState(pos).is(FluidTags.WATER)) {
            // One surface-floor pass owns cover and the single sponge
            // opportunity across all tropical biome seams. Do not decorate
            // forest-edge water again or reach buried pools through this scan.
            if (GameplayDimensions.is(level, GameplayDimensions.TROPICAL)) return List.of();
            // Coral trees and the reef's weighted floor have one owner. Shoreline
            // reeds likewise belong only to the continuous wet-patch pass.
            if (biome.equals("coral_reef") || biome.equals("mangrove_swamp")) return List.of();
            // The original savanna seafloor feature owns every underwater column
            // and its weighted palette; a second equal-weight pass changes both.
            if (biome.equals("savanna") || biome.equals("savanna_lake")) return List.of();
            // Preserve the original sparse, equal-weight deep-water distribution.
            // The dense Savanna mixture belongs only to sky-exposed shallow beds.
            if (GameplayDimensions.is(level, GameplayDimensions.SNOWY)) {
                return SnowyLakeVegetation.usesDeepPalette(level, pos) ? DEEP_WATER : List.of();
            }
            if (GameplayDimensions.is(level, GameplayDimensions.PLAINS)
                    && !AquaticPlantPlacement.hasSkyExposure(level, pos)) return List.of();
            return switch (biome) {
                case "rainforest", "mangrove_swamp", "cherry_grove" -> TROPICAL_WATER;
                case "plains", "plains_lake" -> ids("minecraft:seagrass minecraft:tall_seagrass");
                case "snowy_forest", "dappled_forest", "cold_ocean", "mushroom_caves" -> DocumentedFauna.waterDepth(level, pos) >= DocumentedFauna.DEEP_WATER_THRESHOLD ? DEEP_WATER : SHALLOW_WATER;
                // Keep half the former bull-kelp opportunities empty.
                case "desert", "desert_lake", "mesa", "deep_dark" -> List.of("hybrid_aquatic:bull_kelp", "");
                default -> List.of();
            };
        }
        // The single chunk lottery owns the complete four-species mushroom budget.
        return biome.equals("mushroom_caves") ? List.of() : LAND.getOrDefault(biome, List.of());
    }

    public static boolean placeDeepWaterPlant(WorldGenLevel level, BlockPos pos, RandomSource random) {
        return placePlant(level, pos, DEEP_WATER.get(random.nextInt(DEEP_WATER.size())), random);
    }

    public static boolean placePlant(WorldGenLevel level, BlockPos pos, String name, RandomSource random) {
        if (GameplayDimensions.is(level, GameplayDimensions.TROPICAL)
                && TropicalPlantIntegrity.isMangroveRoot(level.getBlockState(pos))) return false;
        if ((name.contains("mushroom") || name.contains("wild_") || name.equals("minecraft:pumpkin")
                || name.equals("hearthandharvest:corn_stalk")) && StructurePlantExclusion.excludes(level, pos)) return false;
        if (GameplayDimensions.is(level, GameplayDimensions.PLAINS)
                && level.getFluidState(pos).is(FluidTags.WATER)
                && !AquaticPlantPlacement.hasSkyExposure(level, pos)) return false;
        if (name.equals("minecraft:sugar_cane") && ShoreSugarCane.hasDryShoreGeneration(level)) {
            return ShoreSugarCane.placeStalk(level, pos);
        }
        if (GameplayDimensions.is(level, GameplayDimensions.TROPICAL)) {
            if (name.equals("upgrade_aquatic:pickerelweed") && AquaticPlantPlacement.waterColumnHeight(level, pos, 2) == 1) {
                name = "upgrade_aquatic:tall_pickerelweed";
            }
            if (name.equals("minecraft:spore_blossom") && random.nextInt(4) != 0) return false;
            int depth = AquaticPlantPlacement.waterColumnHeight(level, pos, 3);
            if (name.equals("minecraft:wet_sponge") && depth < 2
                    || name.equals("minecraft:tall_seagrass") && depth < 3) {
                name = random.nextBoolean() ? "minecraft:seagrass" : "minecraft:sea_pickle";
            }
        }
        // Tropical shoreline origins remain wet; this does not alter block-item placement.
        if (WATER_ROOTED_SHORE.contains(name) && !level.getFluidState(pos).is(FluidTags.WATER)) return false;
        if (name.equals("farmersdelight:wild_rice") && GameplayDimensions.is(level, GameplayDimensions.TROPICAL)
                && (!level.getFluidState(pos).isSource() || AquaticPlantPlacement.waterColumnHeight(level, pos, 2) != 1)) return false;
        if (name.equals("minecraft:glow_lichen") && GameplayDimensions.is(level, GameplayDimensions.DESERT)) return false;
        if (name.equals("minecraft:cave_vines")) return CaveVinePlacement.place(level, pos, 25, true);
        if (AquaticPlantPlacement.isKelp(name)) return AquaticPlantPlacement.placeKelp(level, pos, name, random);
        Block block = ForgeRegistries.BLOCKS.getValue(new ResourceLocation(name));
        if (block == null || block == Blocks.AIR) return false;
        BlockState state = block.defaultBlockState();
        if (!net.amurdza.examplemod.util.PlantGenerationSupport.isAllowed(level, pos, state)) return false;
        if (name.equals("ecologics:prickly_pear") && level.getBlockState(pos.below()).is(BlockTags.SAND) && level.isEmptyBlock(pos.above())
                && Blocks.CACTUS.defaultBlockState().canSurvive(level, pos)) {
            level.setBlock(pos, Blocks.CACTUS.defaultBlockState(), 2);
            return placePlant(level, pos.above(), name, random);
        }
        if (state.hasProperty(BlockStateProperties.WATERLOGGED)) state = state.setValue(BlockStateProperties.WATERLOGGED, level.getFluidState(pos).is(FluidTags.WATER));
        if (state.hasProperty(BlockStateProperties.PICKLES)) state = state.setValue(BlockStateProperties.PICKLES, 4);
        if (block == Blocks.PINK_PETALS && GameplayDimensions.is(level, GameplayDimensions.TROPICAL)) {
            state = state.setValue(BlockStateProperties.FLOWER_AMOUNT, 4);
        }
        if (state.hasProperty(BlockStateProperties.DOWN)) state = state.setValue(BlockStateProperties.DOWN, true);
        if (state.hasProperty(BlockStateProperties.BERRIES)) state = state.setValue(BlockStateProperties.BERRIES, true);
        for (Property<?> property : state.getProperties()) {
            if (property instanceof IntegerProperty age && property.getName().equals("age")) {
                int maximum = age.getPossibleValues().stream().mapToInt(Integer::intValue).max().orElse(0);
                state = state.setValue(age, maximum);
            }
        }
        if (!DocumentedPlacementRules.isBiomeAllowed(level, pos, state, false)) return false;
        if (level.getFluidState(pos).is(FluidTags.WATER)) {
            if (block == Blocks.SUGAR_CANE || block == Blocks.BIG_DRIPLEAF) {
                return AquaticPlantPlacement.placeShallowColumn(level, pos, state, random);
            }
            if (block == Blocks.SMALL_DRIPLEAF && level.getFluidState(pos.above()).is(FluidTags.WATER)) return false;
        }
        // Corn is cultivated from grass seeds; no wild stalks are generated.
        if (block instanceof CornStalkBlock) return false;
        if (!state.canSurvive(level, pos)) return false;
        if (block instanceof DoublePlantBlock || state.hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF)) {
            return AquaticPlantPlacement.placeDouble(level, pos, state);
        } else {
            if (!AquaticPlantPlacement.preservesFluid(level, pos, state)) return false;
            return level.setBlock(pos, state, 2);
        }
    }
}

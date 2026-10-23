package net.amurdza.examplemod.worldgen;

import alabaster.hearthandharvest.common.block.CornStalkBlock;
import net.amurdza.examplemod.util.DocumentedPlacementRules;
import net.amurdza.examplemod.util.GameplayDimensions;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.List;
import java.util.Map;

/** Modest, local patches of the documented understory and wild crop sources on actual terrain floors. */
public final class DocumentedFloraFeature extends Feature<NoneFeatureConfiguration> {
    private static final Map<String, List<String>> LAND = Map.ofEntries(
            Map.entry("rainforest", ids("minecraft:fern minecraft:grass minecraft:large_fern minecraft:rose_bush minecraft:azalea minecraft:flowering_azalea minecraft:torchflower ferdinandsflowers:red_geranium ferdinandsflowers:light_pink_geranium biomesoplenty:orange_cosmos minecraft:melon minecraft:red_mushroom minecraft:brown_mushroom pineapple_delight:pineapple_wild_crop farmersdelight:wild_tomatoes hearthandharvest:corn_stalk")),
            Map.entry("mangrove_swamp", ids("minecraft:fern minecraft:grass minecraft:big_dripleaf minecraft:small_dripleaf minecraft:blue_orchid minecraft:pitcher_plant upgrade_aquatic:pickerelweed upgrade_aquatic:tall_pickerelweed floral_flair:yellow_hyacinth floral_flair:pink_hyacinth floral_flair:blue_hyacinth floral_flair:lunula minecraft:red_mushroom minecraft:brown_mushroom minecraft:sugar_cane farmersdelight:wild_rice hearthandharvest:wild_cotton farmersdelight:wild_cabbages")),
            Map.entry("cherry_grove", ids("minecraft:grass minecraft:allium minecraft:peony minecraft:lilac minecraft:pink_petals biomesoplenty:pink_daffodil biomeswevegone:japanese_orchid legumedelight:wild_beans farmersdelight:wild_carrots")),
            Map.entry("savanna", ids("minecraft:grass minecraft:tall_grass minecraft:red_tulip minecraft:orange_tulip minecraft:pink_tulip minecraft:white_tulip floral:blue_tulip floral:yellow_tulip minecraft:red_mushroom minecraft:brown_mushroom farmersdelight:wild_potatoes farmersdelight:wild_carrots farmersdelight:wild_beetroots minecraft:pumpkin minecraft:sugar_cane vintagedelight:wild_oats farmersdelight:wild_cabbages minecraft:glow_lichen")),
            Map.entry("plains", ids("minecraft:grass minecraft:tall_grass minecraft:sunflower minecraft:dandelion minecraft:poppy minecraft:cornflower minecraft:oxeye_daisy minecraft:lily_of_the_valley farmersdelight:wild_potatoes farmersdelight:wild_carrots farmersdelight:wild_beetroots minecraft:pumpkin minecraft:red_mushroom minecraft:brown_mushroom minecraft:sugar_cane farmersdelight:wild_cabbages minecraft:glow_lichen")),
            Map.entry("snowy_forest", ids("minecraft:sweet_berry_bush aoemod:blue_berry_bush")),
            Map.entry("mushroom_caves", ids("farmersdelight:wild_beetroots minecraft:red_mushroom minecraft:brown_mushroom twilightforest:mushgloom quark:glow_shroom minecraft:glow_lichen")),
            Map.entry("desert", ids("minecraft:red_mushroom minecraft:brown_mushroom vintagedelight:wild_peanuts collectorsreap:dragon_bush ecologics:prickly_pear minecraft:glow_lichen")),
            Map.entry("mesa", ids("minecraft:red_mushroom minecraft:brown_mushroom vintagedelight:wild_peanuts collectorsreap:dragon_bush ecologics:prickly_pear minecraft:glow_lichen")),
            Map.entry("deep_dark", ids("minecraft:red_mushroom minecraft:brown_mushroom minecraft:chorus_flower minecraft:glow_lichen")),
            Map.entry("crimson_forest", ids("minecraft:crimson_fungus minecraft:crimson_roots minecraft:red_shrub")),
            Map.entry("warped_forest", ids("minecraft:warped_fungus minecraft:warped_roots minecraft:nether_sprouts")),
            Map.entry("soul_sand_valley", ids("aoemod:soul_beetroots aoemod:soul_berry_bush minecraft:nether_wart nourished_nether:soul_weeds")),
            Map.entry("basalt_deltas", ids("mynethersdelight:powdery_cane mynethersdelight:powdery_cannon"))
    );
    private static final List<String> TROPICAL_WATER = ids("minecraft:kelp minecraft:seagrass minecraft:tall_seagrass minecraft:wet_sponge minecraft:sea_pickle minecraft:tube_coral minecraft:brain_coral minecraft:bubble_coral minecraft:fire_coral minecraft:horn_coral upgrade_aquatic:star_coral upgrade_aquatic:petal_coral upgrade_aquatic:finger_coral upgrade_aquatic:star_coral_block upgrade_aquatic:petal_coral_block upgrade_aquatic:finger_coral_block upgrade_aquatic:star_coral_fan upgrade_aquatic:petal_coral_fan upgrade_aquatic:finger_coral_fan");
    private static final List<String> SHALLOW_WATER = ids("minecraft:kelp minecraft:seagrass minecraft:tall_seagrass minecraft:sea_pickle minecraft:wet_sponge");
    private static final List<String> DEEP_WATER = ids("alexscaves:ping_pong_sponge betterend:charnia_light_blue betterend:charnia_cyan alexscaves:bone_worms");

    public DocumentedFloraFeature() { super(NoneFeatureConfiguration.CODEC); }

    private static List<String> ids(String text) { return List.of(text.split(" ")); }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        if (!GameplayDimensions.isCustom(level)) return false;
        RandomSource random = context.random();
        BlockPos origin = context.origin();
        int chunkX = Math.floorDiv(origin.getX(), 16) * 16;
        int chunkZ = Math.floorDiv(origin.getZ(), 16) * 16;
        boolean placed = false;
        for (int attempt = 0; attempt < 40; attempt++) {
            int x = chunkX + random.nextInt(16);
            int z = chunkZ + random.nextInt(16);
            int floors = 0;
            for (int y = level.getMaxBuildHeight() - 2; y > level.getMinBuildHeight(); y--) {
                BlockPos pos = new BlockPos(x, y, z);
                if (level.isEmptyBlock(pos) && GameplayDimensions.is(level, GameplayDimensions.TROPICAL)) {
                    if (level.getBlockState(pos.above()).is(BlockTags.LEAVES) && random.nextInt(12) == 0) {
                        String hanging = GameplayDimensions.biomeIs(level, pos, "mangrove_swamp")
                                ? "upgrade_aquatic:mulberry_vine" : random.nextBoolean() ? "minecraft:cave_vines" : "minecraft:spore_blossom";
                        placed |= placePlant(level, pos, hanging, random);
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
                if (!palette.isEmpty() && random.nextInt(3) != 0) {
                    String name = palette.get(random.nextInt(palette.size()));
                    placed |= placePlant(level, pos, name, random);
                }
                // The stacked Nether can have extra ledges/fungus caps above its
                // lowest gallery. Every band needs a chance to receive its plants.
                if (++floors >= 4 && !GameplayDimensions.is(level, GameplayDimensions.NETHER)) break;
            }
        }
        if (GameplayDimensions.is(level, GameplayDimensions.SNOWY)) {
            for (int x = chunkX; x < chunkX + 16; x++) for (int z = chunkZ; z < chunkZ + 16; z++) {
                BlockPos surface = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, new BlockPos(x, 0, z));
                if (GameplayDimensions.biomeIs(level, surface, "snowy_forest") && level.isEmptyBlock(surface)
                        && Blocks.SNOW.defaultBlockState().canSurvive(level, surface)) {
                    level.setBlock(surface, Blocks.SNOW.defaultBlockState(), 2);
                    placed = true;
                }
            }
        }
        return placed;
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
            return switch (biome) {
                case "rainforest", "mangrove_swamp", "cherry_grove", "coral_reef" -> TROPICAL_WATER;
                case "savanna" -> SHALLOW_WATER;
                case "plains" -> ids("minecraft:seagrass minecraft:tall_seagrass");
                case "snowy_forest", "mushroom_caves" -> DocumentedFauna.waterDepth(level, pos) >= DocumentedFauna.DEEP_WATER_THRESHOLD ? DEEP_WATER : SHALLOW_WATER;
                case "desert", "mesa", "deep_dark" -> ids("hybrid_aquatic:bull_kelp");
                default -> List.of();
            };
        }
        return LAND.getOrDefault(biome, List.of());
    }

    public static boolean placePlant(WorldGenLevel level, BlockPos pos, String name, RandomSource random) {
        Block block = ForgeRegistries.BLOCKS.getValue(new ResourceLocation(name));
        if (block == null || block == Blocks.AIR) return false;
        BlockState state = block.defaultBlockState();
        if (name.equals("ecologics:prickly_pear") && level.getBlockState(pos.below()).is(BlockTags.SAND) && level.isEmptyBlock(pos.above())
                && Blocks.CACTUS.defaultBlockState().canSurvive(level, pos)) {
            level.setBlock(pos, Blocks.CACTUS.defaultBlockState(), 2);
            return placePlant(level, pos.above(), name, random);
        }
        if (state.hasProperty(BlockStateProperties.WATERLOGGED)) state = state.setValue(BlockStateProperties.WATERLOGGED, level.getFluidState(pos).is(FluidTags.WATER));
        if (state.hasProperty(BlockStateProperties.PICKLES)) state = state.setValue(BlockStateProperties.PICKLES, 4);
        if (state.hasProperty(BlockStateProperties.DOWN)) state = state.setValue(BlockStateProperties.DOWN, true);
        if (state.hasProperty(BlockStateProperties.BERRIES)) state = state.setValue(BlockStateProperties.BERRIES, true);
        for (Property<?> property : state.getProperties()) {
            if (property instanceof IntegerProperty age && property.getName().equals("age")) {
                int maximum = age.getPossibleValues().stream().mapToInt(Integer::intValue).max().orElse(0);
                state = state.setValue(age, maximum);
            }
        }
        if (!DocumentedPlacementRules.isBiomeAllowed(level, pos, state, false)) return false;
        // Chunk decoration precedes lighting. Corn's native survival check requires
        // propagated light, although the tropical moss floor itself emits light 15.
        boolean unlitCornGeneration = block instanceof CornStalkBlock && level instanceof net.minecraft.server.level.WorldGenRegion
                && level.getBlockState(pos.below()).is(BlockTags.DIRT);
        if (!unlitCornGeneration && !state.canSurvive(level, pos)) return false;
        if (block instanceof CornStalkBlock) {
            // Mature corn is a three-section plant. A mature bottom alone cannot produce
            // a harvestable ear; validate the whole stalk before changing any blocks.
            for (int i = 1; i <= 2; i++) {
                if (!level.isEmptyBlock(pos.above(i))
                        || !DocumentedPlacementRules.isBiomeAllowed(level, pos.above(i), state, false)) return false;
            }
            var sections = new CornStalkBlock.CornSection[]{CornStalkBlock.CornSection.BOTTOM,
                    CornStalkBlock.CornSection.MIDDLE, CornStalkBlock.CornSection.TOP};
            // Stage the whole column without shape updates. Native onRemove lowers
            // the parent's age if an unsupported upper section gets removed midway.
            for (int i = 0; i < sections.length; i++) {
                level.setBlock(pos.above(i), state.setValue(CornStalkBlock.SECTION, sections[i])
                        .setValue(CornStalkBlock.AGE, 2), Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
            }
            for (int i = 0; i < sections.length; i++) {
                level.setBlock(pos.above(i), state.setValue(CornStalkBlock.SECTION, sections[i]),
                        Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
            }
            return true;
        }
        if (block instanceof DoublePlantBlock || state.hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF)) {
            BlockPos upper = pos.above();
            if (!level.getBlockState(upper).canBeReplaced() || !DocumentedPlacementRules.isBiomeAllowed(level, upper, state, false)) return false;
            level.setBlock(pos, state.setValue(BlockStateProperties.DOUBLE_BLOCK_HALF, DoubleBlockHalf.LOWER), 2);
            level.setBlock(upper, state.setValue(BlockStateProperties.DOUBLE_BLOCK_HALF, DoubleBlockHalf.UPPER), 2);
        } else {
            level.setBlock(pos, state, 2);
        }
        return true;
    }
}

package net.amurdza.examplemod.worldgen;

import net.amurdza.examplemod.util.GameplayDimensions;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.GrowingPlantHeadBlock;

/** Dense Savanna plants on shallow beds; the original sparse deep palette also covers buried aquifers. */
public final class SnowyLakeVegetation {
    private SnowyLakeVegetation() { }

    public static boolean place(WorldGenLevel level, int minX, int minZ) {
        int waterY = level.getLevel().getChunkSource().getGenerator().getSeaLevel() - 1;
        boolean placed = false;
        for (int x = minX; x < minX + 16; x++) for (int z = minZ; z < minZ + 16; z++) {
            BlockPos top = new BlockPos(x, waterY, z);
            if (level.getBlockState(top).is(Blocks.ICE)) top = top.below();
            if (!level.getFluidState(top).is(FluidTags.WATER)) continue;
            BlockPos bottom = top;
            int depth = 1;
            while (bottom.getY() > 0 && level.getFluidState(bottom.below()).is(FluidTags.WATER)) {
                bottom = bottom.below();
                depth++;
            }
            if (bottom.getY() <= 0 || !level.getBlockState(bottom).is(Blocks.WATER)
                    || !level.getFluidState(bottom).isSource()
                    || !level.getBlockState(bottom.below()).isFaceSturdy(level, bottom.below(), Direction.UP)
                    || !(GameplayDimensions.biomeIs(level, bottom, "snowy_forest")
                    || GameplayDimensions.biomeIs(level, bottom, "dappled_forest")
                    || GameplayDimensions.biomeIs(level, bottom, "cold_ocean")
                    || GameplayDimensions.biomeIs(level, bottom, "mountain_lake"))
                    || usesDeepPalette(level, bottom)) continue;
            RandomSource random = RandomSource.create(TropicalLayout.mix(level.getSeed() ^ 0x2b48ca913L
                    ^ TropicalLayout.mix(x * 0x632be59bd9b4e019L) ^ TropicalLayout.mix(z * 0x9e3779b97f4a7c15L)));
            String plant = plant(random, depth);
            if (depth == 1 && plant.equals("minecraft:kelp")) {
                var kelp = Blocks.KELP.defaultBlockState().setValue(GrowingPlantHeadBlock.AGE, 25);
                if (kelp.canSurvive(level, bottom)) placed |= level.setBlock(bottom, kelp, Block.UPDATE_CLIENTS);
            } else {
                placed |= DocumentedFloraFeature.placePlant(level, bottom, plant, random);
            }
        }
        return placed;
    }

    public static boolean usesDeepPalette(WorldGenLevel level, BlockPos pos) {
        return DocumentedFauna.waterDepth(level, pos) >= DocumentedFauna.DEEP_WATER_THRESHOLD
                || !AquaticPlantPlacement.hasSkyExposure(level, pos);
    }

    /** Carvers can expose new beds after surface rules; finish every buried water floor before plants. */
    public static void prepareBuriedBeds(WorldGenLevel level, int minX, int minZ) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int x = minX; x < minX + 16; x++) for (int z = minZ; z < minZ + 16; z++) {
            boolean roof = false;
            for (int y = level.getMaxBuildHeight() - 1; y > level.getMinBuildHeight() + 1; y--) {
                pos.set(x, y, z);
                var state = level.getBlockState(pos);
                if (state.getFluidState().is(FluidTags.WATER)) {
                    BlockPos bed = pos.below();
                    if (roof && !level.getBlockState(bed).is(Blocks.BEDROCK)
                            && level.getBlockState(bed).isFaceSturdy(level, bed, Direction.UP)) {
                        level.setBlock(bed, Blocks.MUD.defaultBlockState(), Block.UPDATE_CLIENTS);
                        // The requested two-block bed also thickens one-block cave ledges.
                        if (bed.getY() - 1 >= level.getMinBuildHeight()
                                && !level.getBlockState(bed.below()).is(Blocks.BEDROCK)) {
                            level.setBlock(bed.below(), Blocks.MUD.defaultBlockState(), Block.UPDATE_CLIENTS);
                        }
                    }
                } else if (!state.is(Blocks.ICE) && !state.is(Blocks.FROSTED_ICE)
                        && !state.is(Blocks.SNOW) && state.getLightBlock(level, pos) >= 15) {
                    roof = true;
                }
            }
        }
    }

    public static String plant(RandomSource random, int depth) {
        // Savanna's deep mixture is 1/320 sponge, 10/320 pickles, 12/320 kelp,
        // and 297/320 seagrass (30% tall where three water blocks are available).
        int choice = random.nextInt(320);
        if (depth > 1 && choice == 0) return "minecraft:wet_sponge";
        if (choice < (depth == 1 ? 12 : 11)) return "minecraft:sea_pickle";
        if (choice < (depth == 1 ? 24 : 23)) return "minecraft:kelp";
        return depth >= 3 && random.nextFloat() < .3F ? "minecraft:tall_seagrass" : "minecraft:seagrass";
    }
}

package net.amurdza.examplemod.worldgen.feature.features;

import net.amurdza.examplemod.util.GameplayDimensions;
import net.amurdza.examplemod.worldgen.SnowyVegetation;
import net.amurdza.examplemod.worldgen.TropicalLayout;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/** Evenly spread native poplar candidates, with the previous total tree budget. */
public final class DappledTreesFeature extends Feature<NoneFeatureConfiguration> {
    public static final int CELL_SIZE = 12;
    public static final double TREES_PER_CHUNK = 3.0 * 611.0 / 1114.0;
    public static final double CELL_CHANCE = TREES_PER_CHUNK * CELL_SIZE * CELL_SIZE / 256.0;

    public DappledTreesFeature() { super(NoneFeatureConfiguration.CODEC); }

    public static BlockPos candidate(long seed, int cellX, int cellZ) {
        RandomSource random = random(seed, cellX, cellZ);
        if (random.nextDouble() >= CELL_CHANCE) return null;
        return new BlockPos(cellX * CELL_SIZE + CELL_SIZE / 2 + random.nextInt(3) - 1, 0,
                cellZ * CELL_SIZE + CELL_SIZE / 2 + random.nextInt(3) - 1);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        if (!GameplayDimensions.is(context.level(), GameplayDimensions.SNOWY)) return false;
        int minX = Math.floorDiv(context.origin().getX(), 16) * 16;
        int minZ = Math.floorDiv(context.origin().getZ(), 16) * 16;
        var selector = context.level().registryAccess().registryOrThrow(Registries.CONFIGURED_FEATURE)
                .get(new ResourceLocation("aoemod:dappled/trees"));
        if (selector == null) return false;
        boolean placed = false;
        for (int cx = Math.floorDiv(minX, CELL_SIZE); cx <= Math.floorDiv(minX + 15, CELL_SIZE); cx++) {
            for (int cz = Math.floorDiv(minZ, CELL_SIZE); cz <= Math.floorDiv(minZ + 15, CELL_SIZE); cz++) {
                BlockPos candidate = candidate(context.level().getSeed(), cx, cz);
                if (candidate == null || candidate.getX() < minX || candidate.getX() >= minX + 16
                        || candidate.getZ() < minZ || candidate.getZ() >= minZ + 16) continue;
                BlockPos root = SnowyVegetation.surfaceRoot(context.level(), candidate.getX(), candidate.getZ());
                if (root != null && GameplayDimensions.biomeIs(context.level(), root, "dappled_forest")) {
                    placed |= selector.place(context.level(), context.chunkGenerator(),
                            random(context.level().getSeed() ^ 0x61b7a52L, cx, cz), root);
                }
            }
        }
        return placed;
    }

    private static RandomSource random(long seed, int cellX, int cellZ) {
        return RandomSource.create(TropicalLayout.mix(seed ^ 0x72dba341L
                ^ cellX * 341873128712L ^ cellZ * 132897987541L));
    }
}

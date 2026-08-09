package net.amurdza.examplemod.worldgen.feature.features;

import com.mojang.serialization.Codec;
import net.amurdza.examplemod.worldgen.feature.configs.SpiralCaveFeatureConfig;
import net.amurdza.examplemod.worldgen.feature.configs.SpiralCaveFeatureConfig.NetherLayers;
import net.amurdza.examplemod.worldgen.feature.configs.SpiralCaveFeatureConfig.Placement;
import net.amurdza.examplemod.worldgen.feature.configs.SpiralCaveFeatureConfig.Shape;
import net.amurdza.examplemod.worldgen.structure.AbstractSpiralCavePiece;
import net.amurdza.examplemod.worldgen.structure.MushroomCavePiece;
import net.amurdza.examplemod.worldgen.structure.NetherCavePiece;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

import java.util.HashSet;
import java.util.Set;

public class SpiralCaveChunkFeature extends Feature<SpiralCaveFeatureConfig> {
    private static final double Z_START_BLOCKS = 0.0D;

    public SpiralCaveChunkFeature(Codec<SpiralCaveFeatureConfig> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<SpiralCaveFeatureConfig> context) {
        SpiralCaveFeatureConfig config = context.config();
        Shape shape = config.shape();
        ChunkPos chunkPos = new ChunkPos(context.origin());
        BoundingBox chunkBox = new BoundingBox(
                chunkPos.getMinBlockX(),
                context.level().getMinBuildHeight(),
                chunkPos.getMinBlockZ(),
                chunkPos.getMaxBlockX(),
                context.level().getMaxBuildHeight() - 1,
                chunkPos.getMaxBlockZ()
        );

        boolean processedCave = false;

        for (CaveCenter center : nearbyCaveCenters(chunkPos, config.placement())) {
            BlockPos caveOrigin = new BlockPos(
                    center.x(),
                    shape.startY() + Mth.ceil(shape.verticalRadius() + 1.5F),
                    center.z()
            );
            long caveSeed = caveSeed(context.level().getSeed(), caveOrigin, config.caveType().ordinal());
            AbstractSpiralCavePiece piece = createPiece(config, caveOrigin, caveSeed);

            if (piece == null || !piece.getBoundingBox().intersects(
                    chunkPos.getMinBlockX(),
                    chunkPos.getMinBlockZ(),
                    chunkPos.getMaxBlockX(),
                    chunkPos.getMaxBlockZ()
            )) {
                continue;
            }

            RandomSource chunkRandom = RandomSource.create(caveSeed ^ chunkPos.toLong());
            WorldGenLevel level = context.level();

            piece.postProcess(
                    level,
                    level.getLevel().structureManager(),
                    context.chunkGenerator(),
                    chunkRandom,
                    chunkBox,
                    chunkPos,
                    caveOrigin
            );
            processedCave = true;
        }

        return processedCave;
    }

    private AbstractSpiralCavePiece createPiece(
            SpiralCaveFeatureConfig config,
            BlockPos origin,
            long seed
    ) {
        Shape shape = config.shape();

        return switch (config.caveType()) {
            case MUSHROOM -> createMushroomPiece(config, shape, origin, seed);
            case NETHER -> config.netherLayers()
                    .<AbstractSpiralCavePiece>map(layers -> createNetherPiece(config, shape, layers, origin, seed))
                    .orElse(null);
        };
    }

    private MushroomCavePiece createMushroomPiece(
            SpiralCaveFeatureConfig config,
            Shape shape,
            BlockPos origin,
            long seed
    ) {
        return new MushroomCavePiece(
                origin,
                seed,
                shape.endY(),
                shape.endX(),
                shape.horizontalRadius(),
                shape.verticalRadius(),
                shape.centralPillarDiameter(),
                shape.minFloorThickness(),
                shape.liquidDepth(),
                shape.liquidRadius(),
                config.placedFeatures()
        );
    }

    private NetherCavePiece createNetherPiece(
            SpiralCaveFeatureConfig config,
            Shape shape,
            NetherLayers layers,
            BlockPos origin,
            long seed
    ) {
        return new NetherCavePiece(
                origin,
                seed,
                shape.endY(),
                shape.endX(),
                shape.horizontalRadius(),
                shape.verticalRadius(),
                shape.centralPillarDiameter(),
                shape.minFloorThickness(),
                shape.liquidDepth(),
                shape.liquidRadius(),
                layers.maxDeepDarkY(),
                layers.maxSoulSandValleyY(),
                layers.maxWarpedForestY(),
                layers.maxCrimsonForestY(),
                layers.maxBasaltDeltasY(),
                config.placedFeatures()
        );
    }

    private Set<CaveCenter> nearbyCaveCenters(ChunkPos chunkPos, Placement placement) {
        Set<CaveCenter> centers = new HashSet<>();
        double chunkCenterX = chunkPos.getMinBlockX() + 8.0D;
        double chunkCenterZ = chunkPos.getMinBlockZ() + 8.0D;
        double xSpacingBlocks = placement.xSpacing() * 16.0D;
        double periodBlocks = placement.biomeWidth() * 10.0D * 16.0D;
        long nearestXIndex = Math.round((chunkCenterX - placement.xStart()) / xSpacingBlocks);
        long baseCycle = Mth.floor((chunkCenterZ - Z_START_BLOCKS) / periodBlocks);

        for (long xOffset = -1; xOffset <= 1; xOffset++) {
            double targetX = placement.xStart() + (nearestXIndex + xOffset) * xSpacingBlocks;
            int originX = Mth.floor(targetX / 16.0D) * 16;

            for (long cycleOffset = -1; cycleOffset <= 1; cycleOffset++) {
                double cycleStart = Z_START_BLOCKS + (baseCycle + cycleOffset) * periodBlocks;
                addCenter(centers, originX, cycleStart + placement.targetContinents() * periodBlocks);
                addCenter(centers, originX, cycleStart + (1.0D - placement.targetContinents()) * periodBlocks);
            }
        }

        return centers;
    }

    private void addCenter(Set<CaveCenter> centers, int originX, double targetZ) {
        if (targetZ < Z_START_BLOCKS) {
            return;
        }

        int originZ = Mth.floor(targetZ / 16.0D) * 16;
        centers.add(new CaveCenter(originX, originZ));
    }

    private long caveSeed(long worldSeed, BlockPos origin, int caveType) {
        long seed = worldSeed;
        seed ^= (long) origin.getX() * 341873128712L;
        seed ^= (long) origin.getY() * 132897987541L;
        seed ^= (long) origin.getZ() * 42317861L;
        seed ^= (long) caveType * -7046029254386353131L;
        seed ^= seed >>> 33;
        seed *= 0xff51afd7ed558ccdL;
        seed ^= seed >>> 33;
        seed *= 0xc4ceb9fe1a85ec53L;
        seed ^= seed >>> 33;
        return seed;
    }

    private record CaveCenter(int x, int z) {
    }
}

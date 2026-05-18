package net.amurdza.examplemod.worldgen.carver;

import com.mojang.serialization.Codec;
import net.amurdza.examplemod.AOEMod;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.SectionPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.CarvingMask;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.Aquifer;
import net.minecraft.world.level.levelgen.carver.CarvingContext;
import net.minecraft.world.level.levelgen.carver.CaveCarverConfiguration;
import net.minecraft.world.level.levelgen.carver.CaveWorldCarver;
import net.minecraft.world.level.levelgen.carver.WorldCarver;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;

public class LongNetherLayerCaveCarver extends CaveWorldCarver {
    private static final ResourceLocation DEEP_DARK = new ResourceLocation(AOEMod.MOD_ID, "deep_dark");
    private static final ResourceLocation CRIMSON_FOREST = new ResourceLocation(AOEMod.MOD_ID, "crimson_forest");
    private static final ResourceLocation WARPED_FOREST = new ResourceLocation(AOEMod.MOD_ID, "warped_forest");
    private static final ResourceLocation MUSHROOM_CAVES = new ResourceLocation(AOEMod.MOD_ID, "mushroom_caves");

    private static final double TARGET_Y = -100.0D;
    private static final double MIN_EFFECTIVE_SLOPE = -0.4D;
    private static final double MAX_EFFECTIVE_SLOPE = 0.2D;

    public LongNetherLayerCaveCarver(Codec<CaveCarverConfiguration> codec) {
        super(codec);
    }

    @Override
    public int getRange() {
        return 18;
    }

    @Override
    public boolean carve(
            @NotNull CarvingContext context,
            CaveCarverConfiguration config,
            @NotNull ChunkAccess chunk,
            @NotNull Function<BlockPos, Holder<Biome>> biomeGetter,
            RandomSource random,
            @NotNull Aquifer aquifer,
            ChunkPos chunkPos,
            @NotNull CarvingMask carvingMask
    ) {
        int maxTunnelLength = SectionPos.sectionToBlockCoord(this.getRange() * 2 - 1);
        int tunnelLength = maxTunnelLength - random.nextInt(maxTunnelLength / 4);

        double x = chunkPos.getBlockX(random.nextInt(16));
        double y = config.y.sample(random, context);
        double z = chunkPos.getBlockZ(random.nextInt(16));

        double horizontalRadiusMultiplier = config.horizontalRadiusMultiplier.sample(random);
        double verticalRadiusMultiplier = config.verticalRadiusMultiplier.sample(random);
        double yScale = config.yScale.sample(random);

        float yaw = random.nextFloat() * ((float)Math.PI * 2F);

        double targetEffectiveSlope = (TARGET_Y - y) / tunnelLength;
        targetEffectiveSlope = Mth.clamp(targetEffectiveSlope, MIN_EFFECTIVE_SLOPE, MAX_EFFECTIVE_SLOPE);

        float pitch = pitchForEffectiveSlope(
                targetEffectiveSlope,
                horizontalRadiusMultiplier,
                verticalRadiusMultiplier
        );

        float thickness = this.getThickness(random);

        this.createTunnel(
                context,
                config,
                chunk,
                biomeGetter,
                random.nextLong(),
                aquifer,
                x,
                y,
                z,
                horizontalRadiusMultiplier,
                verticalRadiusMultiplier,
                thickness,
                yaw,
                pitch,
                0,
                tunnelLength,
                yScale,
                carvingMask,
                this::shouldSkipCustom
        );

        return true;
    }

    protected static boolean canReachNew(
            ChunkPos chunkPos,
            double x,
            double z,
            int branchIndex,
            int branchCount,
            double horizontalRadius
    ) {
        double chunkX = chunkPos.getMiddleBlockX();
        double chunkZ = chunkPos.getMiddleBlockZ();

        double dx = x - chunkX;
        double dz = z - chunkZ;

        double remainingSteps = branchCount - branchIndex;
        double buffer = horizontalRadius + 2.0D + 16.0D;

        return dx * dx + dz * dz <= (remainingSteps + buffer) * (remainingSteps + buffer);
    }

    protected void createTunnel(
            @NotNull CarvingContext pContext,
            @NotNull CaveCarverConfiguration pConfig,
            @NotNull ChunkAccess pChunk,
            @NotNull Function<BlockPos, Holder<Biome>> pBiomeAccessor,
            long pSeed,
            @NotNull Aquifer pAquifer,
            double pX,
            double pY,
            double pZ,
            double pHorizontalRadiusMultiplier,
            double pVerticalRadiusMultiplier,
            float pThickness,
            float pYaw,
            float pPitch,
            int pBranchIndex,
            int pBranchCount,
            double pHorizontalVerticalRatio,
            @NotNull CarvingMask pCarvingMask,
            WorldCarver.@NotNull CarveSkipChecker pSkipChecker
    ) {
        RandomSource randomsource = RandomSource.create(pSeed);

        float f = 0.0F;
        float f1 = 0.0F;

        float minPitch = pitchForEffectiveSlope(
                MIN_EFFECTIVE_SLOPE,
                pHorizontalRadiusMultiplier,
                pVerticalRadiusMultiplier
        );

        float maxPitch = pitchForEffectiveSlope(
                MAX_EFFECTIVE_SLOPE,
                pHorizontalRadiusMultiplier,
                pVerticalRadiusMultiplier
        );

        float turnPerStep = randomsource.nextBoolean() ? 0.045F : -0.045F;
        turnPerStep *= Mth.randomBetween(randomsource, 0.8F, 1.2F);

        for (int j = pBranchIndex; j < pBranchCount; ++j) {
            pPitch = Mth.clamp(pPitch, minPitch, maxPitch);

            float horizontalStep = Mth.cos(pPitch);

            pX += Mth.cos(pYaw) * horizontalStep;
            pY += Mth.sin(pPitch);
            pZ += Mth.sin(pYaw) * horizontalStep;

            this.carveEllipsoid(
                    pContext,
                    pConfig,
                    pChunk,
                    pBiomeAccessor,
                    pAquifer,
                    pX,
                    pY,
                    pZ,
                    pHorizontalRadiusMultiplier,
                    pVerticalRadiusMultiplier,
                    pCarvingMask,
                    pSkipChecker
            );

//            if (!canReachNew(pChunk.getPos(), pX, pZ, j, pBranchCount, pHorizontalRadiusMultiplier)) {
//                return;
//            }

            pPitch *= 0.995F;
            pPitch += f1 * 0.005F;

            f1 *= 0.98F;
            f1 += (randomsource.nextFloat() - randomsource.nextFloat())
                    * randomsource.nextFloat()
                    * 0.05F;

            pYaw += turnPerStep + f * 0.02F;

            f *= 0.8F;
            f += (randomsource.nextFloat() - randomsource.nextFloat())
                    * randomsource.nextFloat()
                    * 0.3F;
        }
    }

    private boolean shouldSkipCustom(
            CarvingContext context,
            double relativeX,
            double relativeY,
            double relativeZ,
            int y
    ) {
        if (relativeY <= -1.0D) {
            return true;
        }

        return relativeX * relativeX
                + relativeY * relativeY
                + relativeZ * relativeZ >= 1.0D;
    }

    @Override
    protected boolean carveBlock(
            @NotNull CarvingContext context,
            @NotNull CaveCarverConfiguration config,
            ChunkAccess chunk,
            @NotNull Function<BlockPos, Holder<Biome>> biomeGetter,
            @NotNull CarvingMask carvingMask,
            BlockPos.@NotNull MutableBlockPos pos,
            BlockPos.@NotNull MutableBlockPos checkPos,
            @NotNull Aquifer aquifer,
            @NotNull MutableBoolean reachedSurface
    ) {
        BlockState oldState = chunk.getBlockState(pos);

        if (!this.canReplaceBlock(config, oldState)) {
            return false;
        }

        BlockState carvedState = pos.getY() <= config.lavaLevel.resolveY(context)
                ? Blocks.LAVA.defaultBlockState()
                : Blocks.CAVE_AIR.defaultBlockState();

        chunk.setBlockState(pos, carvedState, false);

        decorateCaveSurface(chunk, biomeGetter, pos, carvedState);

        return true;
    }

    private static void decorateCaveSurface(
            ChunkAccess chunk,
            Function<BlockPos, Holder<Biome>> biomeGetter,
            BlockPos carvedPos,
            BlockState carvedState
    ) {
        ResourceLocation biomeId = biomeGetter.apply(carvedPos)
                .unwrapKey()
                .map(ResourceKey::location)
                .orElse(null);

        ResourceLocation biomeId1 = biomeGetter.apply(carvedPos.below())
                .unwrapKey()
                .map(ResourceKey::location)
                .orElse(null);

        if (biomeId == null) {
            return;
        }

        boolean isLiquid = !carvedState.getFluidState().isEmpty();

        if (biomeId.equals(DEEP_DARK)) {
            replaceAdjacentSolids(chunk, carvedPos, Blocks.SCULK.defaultBlockState());
            return;
        }

        assert biomeId1 != null;
        BlockState floorState = getFloorState(biomeId1, isLiquid);

        if (floorState != null) {
            BlockPos below = carvedPos.below();
            BlockState belowState = chunk.getBlockState(below);

            if (isNaturalReplaceableSurface(belowState)) {
                chunk.setBlockState(below, floorState, false);
            }
        }
    }

    private static @Nullable BlockState getFloorState(ResourceLocation biomeId1, boolean isLiquid) {
        BlockState floorState = null;

        assert biomeId1 != null;

        if (biomeId1.equals(CRIMSON_FOREST)) {
            floorState = Blocks.CRIMSON_NYLIUM.defaultBlockState();
        } else if (biomeId1.equals(WARPED_FOREST)) {
            floorState = Blocks.WARPED_NYLIUM.defaultBlockState();
        } else if (biomeId1.equals(MUSHROOM_CAVES)) {
            floorState = isLiquid
                    ? Blocks.GRAVEL.defaultBlockState()
                    : Blocks.MYCELIUM.defaultBlockState();
        }

        return floorState;
    }

    private static void replaceAdjacentSolids(
            ChunkAccess chunk,
            BlockPos carvedPos,
            BlockState replacement
    ) {
        for (Direction direction : Direction.values()) {
            BlockPos sidePos = carvedPos.relative(direction);
            BlockState sideState = chunk.getBlockState(sidePos);

            if (isNaturalReplaceableSurface(sideState)) {
                chunk.setBlockState(sidePos, replacement, false);
            }
        }
    }

    private static boolean isNaturalReplaceableSurface(BlockState state) {
        return state.is(Blocks.NETHERRACK)
                || state.is(Blocks.SOUL_SAND)
                || state.is(Blocks.SOUL_SOIL)
                || state.is(Blocks.BASALT)
                || state.is(Blocks.SMOOTH_BASALT)
                || state.is(Blocks.BLACKSTONE)
                || state.is(Blocks.DEEPSLATE)
                || state.is(Blocks.STONE)
                || state.is(Blocks.TUFF)
                || state.is(Blocks.CRIMSON_NYLIUM)
                || state.is(Blocks.WARPED_NYLIUM)
                || state.is(Blocks.MYCELIUM)
                || state.is(Blocks.SCULK);
    }

    private static float pitchForEffectiveSlope(
            double effectiveSlope,
            double horizontalRadius,
            double verticalRadius
    ) {
        if (verticalRadius == 0.0D) {
            return 0.0F;
        }

        return (float)Math.atan(effectiveSlope * horizontalRadius / verticalRadius);
    }
}
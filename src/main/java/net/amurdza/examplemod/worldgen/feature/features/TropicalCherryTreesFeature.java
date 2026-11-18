package net.amurdza.examplemod.worldgen.feature.features;

import com.mojang.serialization.Codec;
import net.amurdza.examplemod.util.GameplayDimensions;
import net.amurdza.examplemod.worldgen.TropicalVegetation;
import net.amurdza.examplemod.worldgen.DimensionTerrain;
import net.amurdza.examplemod.worldgen.TropicalLayout;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.Registries;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.TreeConfiguration;
import net.minecraft.world.level.levelgen.feature.treedecorators.BeehiveDecorator;

import java.util.ArrayList;

/** One seeded candidate in each twenty-block cell, with bounded natural jitter. */
public final class TropicalCherryTreesFeature extends Feature<NoneFeatureConfiguration> {
    public static final int CELL_SIZE = 20;
    public static final float NEST_CHANCE = 0;

    public TropicalCherryTreesFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    public static BlockPos candidate(long seed, int cellX, int cellZ) {
        RandomSource random = RandomSource.create(seed ^ cellX * 341873128712L ^ cellZ * 132897987541L ^ 0x7C8A45DFL);
        return new BlockPos(cellX * CELL_SIZE + CELL_SIZE / 2 + random.nextInt(5) - 2, 0,
                cellZ * CELL_SIZE + CELL_SIZE / 2 + random.nextInt(5) - 2);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        TropicalLayout layout = DimensionTerrain.tropicalLayout(context.level().getLevel().getChunkSource()
                .randomState().router().temperature());
        return placeInLayout(context, layout);
    }

    public boolean placeInLayout(FeaturePlaceContext<NoneFeatureConfiguration> context, TropicalLayout layout) {
        if (!GameplayDimensions.is(context.level(), GameplayDimensions.TROPICAL)) return false;
        int minX = Math.floorDiv(context.origin().getX(), 16) * 16;
        int minZ = Math.floorDiv(context.origin().getZ(), 16) * 16;
        var cherry = context.level().registryAccess().registryOrThrow(Registries.CONFIGURED_FEATURE)
                .get(new ResourceLocation("minecraft:cherry_bees_005"));
        if (cherry == null || !(cherry.config() instanceof TreeConfiguration original)) return false;
        var regular = withNestChance(original, NEST_CHANCE);
        var central = withNestChance(original, 1);
        boolean placed = false;
        for (int cellX = Math.floorDiv(minX, CELL_SIZE); cellX <= Math.floorDiv(minX + 15, CELL_SIZE); cellX++) {
            for (int cellZ = Math.floorDiv(minZ, CELL_SIZE); cellZ <= Math.floorDiv(minZ + 15, CELL_SIZE); cellZ++) {
                BlockPos candidate = candidate(context.level().getSeed(), cellX, cellZ);
                if (candidate.getX() < minX || candidate.getX() >= minX + 16
                        || candidate.getZ() < minZ || candidate.getZ() >= minZ + 16) continue;
                var sample = layout.sample(candidate.getX(), candidate.getZ());
                if (sample.distance() > sample.cherryEdge() - TropicalLayout.CHERRY_TREE_SETBACK) continue;
                BlockPos ground = TropicalVegetation.findGround(context.level(), candidate.getX(), candidate.getZ());
                if (ground == null || !GameplayDimensions.biomeIs(context.level(), ground, "cherry_grove")
                        || !context.level().getFluidState(ground).isEmpty()
                        || !Blocks.CHERRY_SAPLING.defaultBlockState().canSurvive(context.level(), ground)) continue;
                var tree = guaranteedNestCandidate(layout, context.level().getSeed(), cellX, cellZ) ? central : regular;
                placed |= tree.place(context.level(), context.chunkGenerator(), context.random(), ground);
            }
        }
        return placed;
    }

    /** Keep the complete vanilla cherry shape and change only its bee decorator. */
    public static ConfiguredFeature<TreeConfiguration, ?> withNestChance(TreeConfiguration original, float chance) {
        var decorators = new ArrayList<>(original.decorators);
        decorators.removeIf(decorator -> decorator instanceof BeehiveDecorator);
        decorators.add(new BeehiveDecorator(chance));
        var builder = new TreeConfiguration.TreeConfigurationBuilder(original.trunkProvider, original.trunkPlacer,
                original.foliageProvider, original.foliagePlacer, original.rootPlacer, original.minimumSize)
                .dirt(original.dirtProvider).decorators(decorators);
        if (original.ignoreVines) builder.ignoreVines();
        if (original.forceDirt) builder.forceDirt();
        return new ConfiguredFeature<>(Feature.TREE, builder.build());
    }

    /** The two closest central cells receive nests; all other trees have no nest decorator. */
    public static boolean guaranteedNestCandidate(TropicalLayout layout, long treeSeed, int cellX, int cellZ) {
        BlockPos at = candidate(treeSeed, cellX, cellZ);
        TropicalLayout.Island island = layout.nearest(at.getX(), at.getZ());
        return centralCandidates(island, treeSeed).contains(at);
    }

    private static java.util.List<BlockPos> centralCandidates(TropicalLayout.Island island, long treeSeed) {
        int centerCellX = Math.floorDiv((int) Math.floor(island.x()), CELL_SIZE);
        int centerCellZ = Math.floorDiv((int) Math.floor(island.z()), CELL_SIZE);
        var centralCandidates = new ArrayList<BlockPos>();
        for (int x = centerCellX - 1; x <= centerCellX + 1; x++) {
            for (int z = centerCellZ - 1; z <= centerCellZ + 1; z++) {
                centralCandidates.add(candidate(treeSeed, x, z));
            }
        }
        centralCandidates.sort(java.util.Comparator.comparingDouble((BlockPos test) -> {
            double dx = test.getX() - island.x(), dz = test.getZ() - island.z();
            return dx * dx + dz * dz;
        }).thenComparingInt(BlockPos::getX).thenComparingInt(BlockPos::getZ));
        return centralCandidates.subList(0, 2);
    }
}

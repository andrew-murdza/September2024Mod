package net.amurdza.examplemod.worldgen;

import net.amurdza.examplemod.util.GameplayDimensions;
import net.amurdza.examplemod.util.BiomePlacementRestrictions;
import net.amurdza.examplemod.util.GrowthPlacementContext;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChorusFlowerBlock;
import net.minecraft.world.level.block.SculkShriekerBlock;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

import java.util.ArrayList;
import java.util.List;

/** Per-chunk chorus opportunities and evenly spaced, compact single-species sculk patches. */
public final class DeepDarkVegetation {
    public static final int PATCH_RARITY = 20;
    public static final int PATCHES_PER_CELL = 3;
    public static final int SHRIEKER_PATCH_PERCENT = 15;
    public static final int PATCH_WIDTH = 10;
    public static final int PLANTS_PER_PATCH = 5;

    private DeepDarkVegetation() { }

    public static boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        if (!GameplayDimensions.is(level, GameplayDimensions.DESERT)) return false;
        RandomSource random = context.random();
        int minX = Math.floorDiv(context.origin().getX(), 16) * 16;
        int minZ = Math.floorDiv(context.origin().getZ(), 16) * 16;
        // One in-square column per chunk; try each actual cave floor,
        // including ledges less than thirty blocks apart.
        int x = minX + random.nextInt(16), z = minZ + random.nextInt(16);
        boolean placed = false;
        for (int y = level.getMaxBuildHeight() - 2; y > level.getMinBuildHeight(); y--) {
            BlockPos root = new BlockPos(x, y, z);
            if (!isFloor(level, root)) continue;
            placed |= placeTree(level, root, random);
        }
        // Three separated opportunities in the same former 5x4 chunk cell:
        // exactly three times the patch density, still spread down the corridor.
        BlockPos scheduled = null;
        for (int patch = 0; patch < PATCHES_PER_CELL; patch++) {
            BlockPos candidate = patchAnchor(level.getSeed(), Math.floorDiv(minX, 80), Math.floorDiv(minZ, 64), patch);
            if (Math.floorDiv(candidate.getX(), 16) == Math.floorDiv(minX, 16)
                    && Math.floorDiv(candidate.getZ(), 16) == Math.floorDiv(minZ, 16)) scheduled = candidate;
        }
        if (scheduled == null) return placed;
        RandomSource patchRandom = RandomSource.create(TropicalLayout.mix(level.getSeed() ^ scheduled.asLong()));
        Block patchBlock = patchBlock(patchRandom);
        List<BlockPos> floors = new ArrayList<>();
        // Search the whole permitted anchor square so one unlucky column or
        // a chorus root cannot erase a tile's otherwise usable patch.
        for (int dx = 4; dx <= 10; dx++) for (int dz = 4; dz <= 10; dz++) {
            for (int y = -1; y > level.getMinBuildHeight(); y--) {
                BlockPos root = new BlockPos(minX + dx, y, minZ + dz);
                if (isFloor(level, root)) floors.add(root);
            }
        }
        while (!floors.isEmpty()) {
            BlockPos anchor = floors.remove(patchRandom.nextInt(floors.size()));
            if (placePatch(level, anchor, patchBlock, patchRandom) > 0) return true;
        }
        return placed;
    }

    public static BlockPos patchAnchor(long seed, int tileX, int tileZ) {
        return patchAnchor(seed, tileX, tileZ, 0);
    }

    public static BlockPos patchAnchor(long seed, int tileX, int tileZ, int patch) {
        if (patch < 0 || patch >= PATCHES_PER_CELL) throw new IllegalArgumentException("Invalid sculk patch index");
        RandomSource random = RandomSource.create(TropicalLayout.mix(seed ^ tileX * 341873128712L
                ^ tileZ * 132897987541L ^ 0x5343554C4BL ^ patch * 0x9E3779B97F4A7C15L));
        int chunkX = tileX * 5 + patch * 2;
        int chunkZ = tileZ * 4 + random.nextInt(4);
        return new BlockPos(chunkX * 16 + 7, 0, chunkZ * 16 + 7);
    }

    public static Block patchBlock(RandomSource random) {
        return random.nextInt(100) < SHRIEKER_PATCH_PERCENT ? Blocks.SCULK_SHRIEKER : Blocks.SCULK_SENSOR;
    }

    public static boolean isFloor(WorldGenLevel level, BlockPos root) {
        return level.isEmptyBlock(root) && level.getBlockState(root.below()).is(Blocks.SCULK)
                && GameplayDimensions.biomeIs(level, root, "deep_dark");
    }

    public static boolean placeTree(WorldGenLevel level, BlockPos root, RandomSource random) {
        if (!isFloor(level, root)) return false;
        for (int height = 1; height <= 4; height++) if (!level.isEmptyBlock(root.above(height))) return false;
        GrowthPlacementBuffer tree = new GrowthPlacementBuffer(level);
        tree.setBlock(root.below(), Blocks.SCULK_CATALYST.defaultBlockState(), Block.UPDATE_CLIENTS);
        ChorusFlowerBlock.generatePlant(tree, root, random, 8);
        if (!tree.getBlockState(root).is(Blocks.CHORUS_PLANT)) return false;
        boolean[] accepted = {false};
        GrowthPlacementContext.runTree(tree, root, () -> {
            // Natural chorus growth may extend above the deep-dark boundary.
            // Validate and commit the entire tree in the same growth scope so
            // direct worldgen placement does not silently reject its flowers.
            int[] flowers = {0};
            boolean valid = tree.isValid((pos, state) -> {
                if (state.is(Blocks.CHORUS_FLOWER)) flowers[0]++;
                return BiomePlacementRestrictions.isAllowed(tree, pos, state)
                        && (!(state.is(Blocks.CHORUS_PLANT) || state.is(Blocks.CHORUS_FLOWER)) || state.canSurvive(tree, pos));
            });
            if (valid && flowers[0] > 0) accepted[0] = tree.commit();
        });
        return accepted[0];
    }

    public static int placePatch(WorldGenLevel level, BlockPos anchor, Block block, RandomSource random) {
        if ((block != Blocks.SCULK_SENSOR && block != Blocks.SCULK_SHRIEKER) || !isFloor(level, anchor)) return 0;
        Block other = block == Blocks.SCULK_SENSOR ? Blocks.SCULK_SHRIEKER : Blocks.SCULK_SENSOR;
        // Do not merge unlike patches at neighbouring chunk edges or on steps.
        for (BlockPos pos : BlockPos.betweenClosed(anchor.offset(-6, -2, -6), anchor.offset(7, 2, 7))) {
            if (level.getBlockState(pos).is(other)) return 0;
        }
        List<BlockPos> candidates = new ArrayList<>();
        for (int dx = -4; dx <= 5; dx++) for (int dz = -4; dz <= 5; dz++) {
            for (int dy : new int[]{0, 1, -1, 2, -2}) {
                BlockPos root = anchor.offset(dx, dy, dz);
                if (isFloor(level, root)) {
                    candidates.add(root);
                    break;
                }
            }
        }
        int size = PLANTS_PER_PATCH;
        if (candidates.size() < size) return 0;
        // Vanilla worldgen shriekers can summon wardens; player/XP-grown ones cannot.
        var state = block == Blocks.SCULK_SHRIEKER
                ? block.defaultBlockState().setValue(SculkShriekerBlock.CAN_SUMMON, true) : block.defaultBlockState();
        GrowthPlacementBuffer buffer = new GrowthPlacementBuffer(level);
        for (int i = 0; i < size; i++) {
            BlockPos root = candidates.remove(random.nextInt(candidates.size()));
            buffer.setBlock(root, state, Block.UPDATE_CLIENTS);
        }
        return buffer.commit() ? size : 0;
    }
}

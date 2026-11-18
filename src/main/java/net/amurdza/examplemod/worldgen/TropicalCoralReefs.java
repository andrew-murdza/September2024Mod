package net.amurdza.examplemod.worldgen;

import net.amurdza.examplemod.util.GameplayDimensions;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/** Four-branched coral trees and the weighted, coral-free floor of tropical reefs. */
public final class TropicalCoralReefs {
    public static final float OPEN_WATER_TALL_SHARE = 0.499F;
    public static final float FOREST_WATER_TALL_SHARE = 0.399F;
    public static final int CELL_SIZE = 16;
    public static final int MINIMUM_WATER_DEPTH = 7;
    private static final List<String> FAMILIES = List.of("minecraft:tube", "minecraft:brain", "minecraft:bubble",
            "minecraft:fire", "minecraft:horn", "upgrade_aquatic:star", "upgrade_aquatic:petal", "upgrade_aquatic:finger");
    private static final Set<String> SURFACE_BIOMES = Set.of("coral_reef", "mangrove_swamp", "rainforest", "cherry_grove");
    private static final long SALT = 0x6A09E667F3BCC909L;

    private TropicalCoralReefs() { }

    private static RandomSource random(long seed, int cellX, int cellZ) {
        return RandomSource.create(seed ^ SALT ^ cellX * 341873128712L ^ cellZ * 132897987541L);
    }

    public static BlockPos anchor(long seed, int cellX, int cellZ) {
        RandomSource random = random(seed, cellX, cellZ);
        return new BlockPos(cellX * CELL_SIZE + 4 + random.nextInt(8), 0,
                cellZ * CELL_SIZE + 4 + random.nextInt(8));
    }

    /** Only the chunk containing an anchor owns that colony, including at negative coordinates. */
    public static List<BlockPos> anchorsForChunk(long seed, int chunkX, int chunkZ) {
        return List.of(anchor(seed, chunkX, chunkZ));
    }

    public static boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        if (!GameplayDimensions.is(level, GameplayDimensions.TROPICAL)) return false;
        int chunkX = Math.floorDiv(context.origin().getX(), 16);
        int chunkZ = Math.floorDiv(context.origin().getZ(), 16);
        // Trees in this or a neighbouring chunk may already cover the heightmap.
        // Find the actual surface substrate before adding a new coral tree.
        List<BlockPos> floor = new ArrayList<>();
        for (int x = chunkX * 16; x < chunkX * 16 + 16; x++) {
            for (int z = chunkZ * 16; z < chunkZ * 16 + 16; z++) {
                BlockPos pos = findSurfaceFloor(level, x, z);
                if (pos != null && surfaceBiome(level, pos)) floor.add(pos);
            }
        }
        boolean placed = false;
        for (BlockPos anchor : anchorsForChunk(level.getSeed(), chunkX, chunkZ)) {
            RandomSource random = random(level.getSeed(), Math.floorDiv(anchor.getX(), CELL_SIZE),
                    Math.floorDiv(anchor.getZ(), CELL_SIZE));
            // Shelf-edge chunks can contain both six- and seven-deep cells.
            // Retry the central square without moving a trunk to a chunk edge.
            for (int attempt = 0; attempt < 64; attempt++) {
                int index = Math.floorMod(Math.floorMod(anchor.getX(), 16) - 4
                        + (Math.floorMod(anchor.getZ(), 16) - 4) * 8 + attempt * 17, 64);
                int x = chunkX * 16 + 4 + index % 8, z = chunkZ * 16 + 4 + index / 8;
                BlockPos bottom = findSurfaceFloor(level, x, z);
                if (bottom != null && placeColony(level, bottom, random, 0)) { placed = true; break; }
            }
        }
        RandomSource floorRandom = RandomSource.create(level.getSeed() ^ (SALT >>> 1)
                ^ chunkX * 341873128712L ^ chunkZ * 132897987541L);
        placed |= placeSponge(level, floor, chunkX, chunkZ, floorRandom);
        for (BlockPos pos : floor) placed |= placeFloorColumn(level, pos, floorRandom);
        return placed;
    }

    private static boolean surfaceBiome(WorldGenLevel level, BlockPos pos) {
        return level.getBiome(pos).unwrapKey().map(key -> key.location())
                .filter(id -> id.getNamespace().equals("aoemod"))
                .map(id -> SURFACE_BIOMES.contains(id.getPath())).orElse(false);
    }

    private static BlockPos findSurfaceFloor(WorldGenLevel level, int x, int z) {
        int top = Math.min(level.getMaxBuildHeight() - 1,
                level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z));
        for (int y = top; y >= level.getMinBuildHeight(); y--) {
            BlockPos ground = new BlockPos(x, y, z);
            BlockState state = level.getBlockState(ground);
            if (!state.blocksMotion() || !state.getFluidState().isEmpty()
                    || state.is(BlockTags.LOGS) || state.is(BlockTags.LEAVES)
                    || state.is(Blocks.MANGROVE_ROOTS) || state.is(Blocks.MUDDY_MANGROVE_ROOTS)
                    || state.is(Blocks.MUSHROOM_STEM) || state.is(Blocks.RED_MUSHROOM_BLOCK)
                    || state.is(Blocks.BROWN_MUSHROOM_BLOCK) || state.is(Blocks.WET_SPONGE)
                    || isDocumentedCoral(state)) continue;
            // The first terrain surface ends the search even on dry land. A
            // buried pool below that surface must never receive this pass.
            return ground.above();
        }
        return null;
    }

    /** One choice per clear seabed cell; trees are already present and cannot be overwritten. */
    public static boolean placeFloorColumn(WorldGenLevel level, BlockPos pos, RandomSource random) {
        if (!GameplayDimensions.is(level, GameplayDimensions.TROPICAL)
                || !surfaceBiome(level, pos)
                || !level.getBlockState(pos).is(Blocks.WATER) || !level.getFluidState(pos).isSource()
                || !level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), Direction.UP)) return false;
        int depth = AquaticPlantPlacement.waterColumnHeight(level, pos, 3);
        int choice = random.nextInt(16);
        if (choice == 0) return placePickles(level, pos, random) || placeShortGrass(level, pos);
        if (choice == 1 && GameplayDimensions.biomeIs(level, pos, "coral_reef") && depth >= 2
                && AquaticPlantPlacement.placeKelp(level, pos, "minecraft:kelp", random)) return true;
        // Shallow kelp choices fall back to grass. Tall
        // grass always leaves another water cell above its complete two halves.
        float tallShare = GameplayDimensions.biomeIs(level, pos, "coral_reef")
                || GameplayDimensions.biomeIs(level, pos, "mangrove_swamp") ? OPEN_WATER_TALL_SHARE : FOREST_WATER_TALL_SHARE;
        if (random.nextFloat() < tallShare && depth >= 3) {
            if (AquaticPlantPlacement.placeDouble(level, pos, Blocks.TALL_SEAGRASS.defaultBlockState())) return true;
        }
        return placeShortGrass(level, pos);
    }

    private static boolean placePickles(LevelAccessor level, BlockPos pos, RandomSource random) {
        BlockState state = Blocks.SEA_PICKLE.defaultBlockState()
                .setValue(BlockStateProperties.PICKLES, 4);
        return state.canSurvive(level, pos) && level.setBlock(pos, state, 2);
    }

    private static boolean placeShortGrass(LevelAccessor level, BlockPos pos) {
        BlockState state = Blocks.SEAGRASS.defaultBlockState();
        return state.canSurvive(level, pos) && level.setBlock(pos, state, 2);
    }

    public static boolean spongeChunk(long seed, int chunkX, int chunkZ) {
        // Previously half of eligible chunks received one sponge; now all do.
        return true;
    }

    public static boolean placeSponge(WorldGenLevel level, List<BlockPos> candidates, int chunkX, int chunkZ, RandomSource random) {
        if (!GameplayDimensions.is(level, GameplayDimensions.TROPICAL) || !spongeChunk(level.getSeed(), chunkX, chunkZ)) return false;
        List<BlockPos> valid = candidates.stream().filter(pos -> level.getBlockState(pos).is(Blocks.WATER)
                && surfaceBiome(level, pos)
                && level.getFluidState(pos).isSource()
                && AquaticPlantPlacement.waterColumnHeight(level, pos, 2) >= 2
                && level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), Direction.UP)).toList();
        return !valid.isEmpty() && level.setBlock(valid.get(random.nextInt(valid.size())), Blocks.WET_SPONGE.defaultBlockState(), 2);
    }

    public static boolean placeColony(WorldGenLevel level, BlockPos bottom, RandomSource random, int shape) {
        if (!GameplayDimensions.is(level, GameplayDimensions.TROPICAL)
                || !GameplayDimensions.biomeIs(level, bottom, "coral_reef")
                || !level.getBlockState(bottom.below()).isFaceSturdy(level, bottom.below(), Direction.UP)
                || AquaticPlantPlacement.waterColumnHeight(level, bottom, MINIMUM_WATER_DEPTH) < MINIMUM_WATER_DEPTH) return false;
        GrowthPlacementBuffer buffer = new GrowthPlacementBuffer(level);
        // Retain the old shape parameter for callers, but every tropical colony
        // now has one upright trunk and all four horizontal branches.
        boolean generated = placeTree(buffer, bottom, random, body(random), 3, 2);
        return generated && buffer.isValid((pos, state) -> isDocumentedCoral(state)
                && state.is(BlockTags.create(new ResourceLocation("aoemod:dimension_sources/tropical")))
                && level.getFluidState(pos).is(FluidTags.WATER) && level.getFluidState(pos).isSource()
                && state.canSurvive(buffer, pos)) && buffer.commit();
    }

    /** Grow the clicked living coral into a colony of the same family, committing only a complete tree. */
    public static boolean growFromCoral(WorldGenLevel level, BlockPos origin, RandomSource random) {
        BlockState source = level.getBlockState(origin);
        String family = coralFamily(source);
        if (family == null || !level.getFluidState(origin).isSource()) return false;
        BlockState body = ForgeRegistries.BLOCKS.getValue(new ResourceLocation(family + "_coral_block")).defaultBlockState();
        GrowthPlacementBuffer buffer = new GrowthPlacementBuffer(level);
        BlockPos root = origin;
        if (source.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
            // Extend from the wall far enough that the two-block branches can open into water.
            Direction facing = source.getValue(BlockStateProperties.HORIZONTAL_FACING);
            BlockPos stem = origin.relative(facing);
            if (!openWater(level.getBlockState(stem))) return false;
            root = stem.relative(facing);
        }
        int depth = AquaticPlantPlacement.waterColumnHeight(level, root, MINIMUM_WATER_DEPTH);
        if (depth < 3) return false;
        int junction = Math.min(3, depth - 3);
        int armHeight = Math.min(2, depth - 2 - junction);
        buffer.setBlock(origin, Blocks.WATER.defaultBlockState(), 2);
        if (!placeTree(buffer, root, random, body, junction, armHeight)) return false;
        if (!root.equals(origin)) {
            buffer.setBlock(origin, body, 2);
            buffer.setBlock(origin.relative(source.getValue(BlockStateProperties.HORIZONTAL_FACING)), body, 2);
        }
        return buffer.isValid((pos, state) -> level.getFluidState(pos).isSource()
                && level.getFluidState(pos).is(FluidTags.WATER) && state.canSurvive(buffer, pos))
                && buffer.commit();
    }

    public static String coralFamily(BlockState state) {
        var id = ForgeRegistries.BLOCKS.getKey(state.getBlock());
        if (id == null) return null;
        String name = id.toString();
        for (String family : FAMILIES) {
            if (name.equals(family + "_coral") || name.equals(family + "_coral_fan")
                    || name.equals(family + "_coral_wall_fan")) return family;
        }
        return null;
    }

    public static boolean isDocumentedCoral(BlockState state) {
        if (state.is(Blocks.SEA_PICKLE) || state.is(Blocks.SEAGRASS) || state.is(Blocks.TALL_SEAGRASS)) return true;
        ResourceLocation id = ForgeRegistries.BLOCKS.getKey(state.getBlock());
        if (id == null) return false;
        for (String family : FAMILIES) {
            String coral = family + "_coral";
            if (id.toString().equals(coral) || id.toString().equals(coral + "_block")
                    || id.toString().equals(coral + "_fan") || id.toString().equals(coral + "_wall_fan")) return true;
        }
        return false;
    }

    private static BlockState body(RandomSource random) {
        return ForgeRegistries.BLOCKS.getValue(new ResourceLocation(FAMILIES.get(random.nextInt(FAMILIES.size()))
                + "_coral_block")).defaultBlockState();
    }

    private static BlockState decoration(String suffix, RandomSource random) {
        return ForgeRegistries.BLOCKS.getValue(new ResourceLocation(FAMILIES.get(random.nextInt(FAMILIES.size()))
                + "_coral" + suffix)).defaultBlockState();
    }

    private static boolean decorateTop(WorldGenLevel level, RandomSource random, BlockPos pos, boolean pickles) {
        BlockPos top = pos.above();
        if (!openWater(level.getBlockState(top))) return false;
        return pickles ? placePickles(level, top, random)
                : level.setBlock(top, decoration(random.nextBoolean() ? "" : "_fan", random), 2);
    }

    /** Slot zero is the junction; odd slots are the four low arms, even slots the high arms. */
    public static boolean[] pickleSlots(RandomSource random) {
        boolean[] selected = new boolean[9];
        selected[1 + random.nextInt(4) * 2] = true;
        selected[2 + random.nextInt(4) * 2] = true;
        int count = 3 + random.nextInt(2);
        for (int placed = 2; placed < count;) {
            int slot = random.nextInt(selected.length);
            if (!selected[slot]) { selected[slot] = true; placed++; }
        }
        return selected;
    }

    private static void decorateSides(LevelAccessor level, RandomSource random, BlockPos pos) {
        for (Direction side : Direction.Plane.HORIZONTAL) {
            if (random.nextFloat() < 0.2F && level.getBlockState(pos.relative(side)).is(Blocks.WATER)) {
                BlockState fan = decoration("_wall_fan", random);
                level.setBlock(pos.relative(side), fan
                        .setValue(BlockStateProperties.HORIZONTAL_FACING, side), 2);
            }
        }
    }

    private static boolean openWater(BlockState state) {
        return state.is(Blocks.WATER) || state.is(Blocks.SEAGRASS) || state.is(Blocks.TALL_SEAGRASS);
    }

    private static boolean placeTree(WorldGenLevel level, BlockPos bottom, RandomSource random, BlockState body,
                                     int junctionHeight, int armHeight) {
        List<BlockPos> skeleton = new ArrayList<>();
        // The rooted cell is one above the seafloor; the branching junction is
        // therefore exactly four blocks above the seafloor.
        for (int height = 0; height <= junctionHeight; height++) skeleton.add(bottom.above(height));
        BlockPos junction = bottom.above(junctionHeight);
        List<BlockPos> tops = new ArrayList<>();
        tops.add(junction);
        for (Direction side : Direction.Plane.HORIZONTAL) {
            skeleton.add(junction.relative(side));
            tops.add(junction.relative(side));
            BlockPos outer = junction.relative(side, 2);
            skeleton.add(outer);
            for (int y = 1; y <= armHeight; y++) skeleton.add(outer.above(y));
            tops.add(outer.above(armHeight));
        }
        // Build the entire skeleton before decorations. A fan from an earlier
        // branch can never displace the body of a later branch.
        for (BlockPos pos : skeleton) {
            if (!openWater(level.getBlockState(pos)) || !openWater(level.getBlockState(pos.above()))) return false;
        }
        for (BlockPos pos : skeleton) level.setBlock(pos, body, 2);
        boolean[] pickles = pickleSlots(random);
        for (int slot = 0; slot < tops.size(); slot++) {
            if (!decorateTop(level, random, tops.get(slot), pickles[slot])) return false;
        }
        for (BlockPos pos : skeleton) decorateSides(level, random, pos);
        return true;
    }
}

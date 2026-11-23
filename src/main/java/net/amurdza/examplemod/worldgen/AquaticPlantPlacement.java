package net.amurdza.examplemod.worldgen;

import net.amurdza.examplemod.util.DocumentedPlacementRules;
import net.amurdza.examplemod.util.GameplayDimensions;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.GrowingPlantHeadBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraftforge.registries.ForgeRegistries;

/** Multi-block plants retain the fluid that was already present in each individual cell. */
public final class AquaticPlantPlacement {
    private AquaticPlantPlacement() { }

    /** Decoration runs before lighting; inspect the real column instead of an uninitialized skylight array. */
    public static boolean hasSkyExposure(LevelReader level, BlockPos pos) {
        BlockPos.MutableBlockPos cursor = pos.mutable();
        for (int y = pos.getY() + 1; y < level.getMaxBuildHeight(); y++) {
            cursor.setY(y);
            BlockState state = level.getBlockState(cursor);
            if (state.getFluidState().is(FluidTags.WATER) || state.is(Blocks.ICE)
                    || state.is(Blocks.FROSTED_ICE) || state.is(Blocks.SNOW)) continue;
            if (state.getLightBlock(level, cursor) >= 15) return false;
        }
        return true;
    }

    public static int waterColumnHeight(LevelReader level, BlockPos bottom, int limit) {
        int depth = 0;
        while (depth < limit && level.getFluidState(bottom.above(depth)).is(FluidTags.WATER)) depth++;
        return depth;
    }

    public static boolean isKelp(String name) {
        return switch (name) {
            case "minecraft:kelp", "hybrid_aquatic:bull_kelp",
                    "netherdepthsupgrade:crimson_kelp", "netherdepthsupgrade:warped_kelp" -> true;
            default -> false;
        };
    }

    public static boolean placeKelp(WorldGenLevel level, BlockPos pos, String name, RandomSource random) {
        if (!isKelp(name)) return false;
        Block head = ForgeRegistries.BLOCKS.getValue(new ResourceLocation(name));
        Block body = ForgeRegistries.BLOCKS.getValue(new ResourceLocation(name + "_plant"));
        if (head == null || head == Blocks.AIR || body == null || body == Blocks.AIR) return false;
        return placeKelpColumn(level, pos, head.defaultBlockState(), body.defaultBlockState(),
                name.startsWith("netherdepthsupgrade:"), random);
    }

    /** Restore the legacy 2–11-block stalk, capped one cell below the existing fluid surface. */
    public static boolean placeKelpColumn(WorldGenLevel level, BlockPos pos, BlockState head, BlockState body,
                                           boolean lava, RandomSource random) {
        boolean bullKelp = new ResourceLocation("hybrid_aquatic", "bull_kelp").equals(ForgeRegistries.BLOCKS.getKey(head.getBlock()));
        int depth = bullKelp ? waterColumnHeight(level, pos, level.getMaxBuildHeight() - pos.getY()) : 0;
        if (bullKelp && depth < 3) return false;
        int desiredHeight = bullKelp ? Math.max(2, depth - 1) : 2 + random.nextInt(10);
        int height = 0;
        while (height < desiredHeight && kelpFluid(level, pos.above(height), lava)
                && (bullKelp || kelpFluid(level, pos.above(height + 1), lava))) height++;
        if (height == 0) return false;
        if (head.hasProperty(GrowingPlantHeadBlock.AGE)) {
            head = head.setValue(GrowingPlantHeadBlock.AGE, bullKelp ? 25 : 20 + random.nextInt(4));
        }
        GrowthPlacementBuffer buffer = new GrowthPlacementBuffer(level);
        for (int y = 0; y < height; y++) {
            buffer.setBlock(pos.above(y), y == height - 1 ? head : body, Block.UPDATE_CLIENTS);
        }
        return commit(level, buffer);
    }

    private static boolean kelpFluid(WorldGenLevel level, BlockPos pos, boolean lava) {
        BlockState state = level.getBlockState(pos);
        return state.is(lava ? Blocks.LAVA : Blocks.WATER) && state.getFluidState().isSource();
    }

    public static BlockState copyWater(WorldGenLevel level, BlockPos pos, BlockState state) {
        return state.hasProperty(BlockStateProperties.WATERLOGGED)
                ? state.setValue(BlockStateProperties.WATERLOGGED,
                level.getFluidState(pos).is(FluidTags.WATER) && level.getFluidState(pos).isSource()) : state;
    }

    public static boolean preservesFluid(WorldGenLevel level, BlockPos pos, BlockState proposed) {
        var fluid = proposed.getFluidState();
        // Tall seagrass and lava seagrass carry fluid intrinsically, without a
        // WATERLOGGED property. Their upper half must replace existing fluid too.
        return fluid.isEmpty() || level.getFluidState(pos).isSource()
                && level.getFluidState(pos).getType() == fluid.getType();
    }

    public static boolean placeDouble(WorldGenLevel level, BlockPos pos, BlockState state) {
        BlockPos upper = pos.above();
        if (!replaceable(level.getBlockState(pos)) || !replaceable(level.getBlockState(upper))) return false;
        BlockState lowerState = copyWater(level, pos,
                state.setValue(BlockStateProperties.DOUBLE_BLOCK_HALF, DoubleBlockHalf.LOWER));
        BlockState upperState = copyWater(level, upper,
                state.setValue(BlockStateProperties.DOUBLE_BLOCK_HALF, DoubleBlockHalf.UPPER));
        // Upgrade Aquatic uses this property only for choosing its wet artwork;
        // actual WATERLOGGED remains independent for the two cells.
        for (var property : state.getProperties()) {
            if (property instanceof BooleanProperty wetAppearance && property.getName().equals("fake_waterlogged")) {
                boolean wet = level.getFluidState(pos).is(FluidTags.WATER)
                        || level.getFluidState(upper).is(FluidTags.WATER);
                lowerState = lowerState.setValue(wetAppearance, wet);
                upperState = upperState.setValue(wetAppearance, wet);
            }
        }
        GrowthPlacementBuffer buffer = new GrowthPlacementBuffer(level);
        buffer.setBlock(pos, lowerState, Block.UPDATE_CLIENTS);
        buffer.setBlock(upper, upperState, Block.UPDATE_CLIENTS);
        return commit(level, buffer);
    }

    /** Emergent cane and large dripleaf put only their rooted bottom cell in shallow water. */
    public static boolean placeShallowColumn(WorldGenLevel level, BlockPos pos, BlockState state,
                                              RandomSource random) {
        if (!level.getFluidState(pos).is(FluidTags.WATER)
                || !level.getFluidState(pos).isSource()
                || !level.getFluidState(pos.above()).isEmpty()) return false;
        int height = state.is(Blocks.SUGAR_CANE) ? 3
                : GameplayDimensions.is(level, GameplayDimensions.TROPICAL) ? 2 : 2 + random.nextInt(3);
        Direction facing = Direction.Plane.HORIZONTAL.getRandomDirection(random);
        GrowthPlacementBuffer buffer = new GrowthPlacementBuffer(level);
        for (int y = 0; y < height; y++) {
            BlockPos target = pos.above(y);
            if (!replaceable(level.getBlockState(target)) || y > 0 && !level.getFluidState(target).isEmpty()) return false;
            BlockState part = state;
            if (state.is(Blocks.BIG_DRIPLEAF)) {
                part = (y == height - 1 ? Blocks.BIG_DRIPLEAF : Blocks.BIG_DRIPLEAF_STEM)
                        .defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, facing);
            }
            buffer.setBlock(target, copyWater(level, target, part), Block.UPDATE_CLIENTS);
        }
        return commit(level, buffer);
    }

    private static boolean replaceable(BlockState state) {
        return state.isAir() || state.is(Blocks.WATER) || state.is(Blocks.LAVA) || state.canBeReplaced();
    }

    private static boolean commit(WorldGenLevel original, GrowthPlacementBuffer buffer) {
        return buffer.isValid((pos, state) -> preservesFluid(original, pos, state)
                && DocumentedPlacementRules.isGenerationBiomeAllowed(buffer, pos, state, false)
                && state.canSurvive(buffer, pos)) && buffer.commit();
    }
}

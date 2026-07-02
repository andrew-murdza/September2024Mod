package net.amurdza.examplemod.worldgen.tree_decorators;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.amurdza.examplemod.registry.ModTreeDecorators;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CaveVinesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.levelgen.feature.treedecorators.TreeDecorator;
import net.minecraft.world.level.levelgen.feature.treedecorators.TreeDecoratorType;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class RainforestCanopyDecorator extends TreeDecorator {
    public static final Codec<RainforestCanopyDecorator> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    Codec.intRange(0, 1000).fieldOf("nothing_weight").forGetter(decorator -> decorator.nothingWeight),
                    Codec.intRange(0, 1000).fieldOf("spore_blossom_weight").forGetter(decorator -> decorator.sporeBlossomWeight),
                    Codec.intRange(0, 1000).fieldOf("cave_vines_weight").forGetter(decorator -> decorator.caveVinesWeight),
                    IntProvider.CODEC.fieldOf("cave_vines_length").forGetter(decorator -> decorator.caveVinesLength)
            ).apply(instance, RainforestCanopyDecorator::new)
    );

    private final int nothingWeight;
    private final int sporeBlossomWeight;
    private final int caveVinesWeight;
    private final IntProvider caveVinesLength;

    public RainforestCanopyDecorator(int nothingWeight, int sporeBlossomWeight, int caveVinesWeight, IntProvider caveVinesLength) {
        this.nothingWeight = nothingWeight;
        this.sporeBlossomWeight = sporeBlossomWeight;
        this.caveVinesWeight = caveVinesWeight;
        this.caveVinesLength = caveVinesLength;
    }

    @Override
    protected @NotNull TreeDecoratorType<?> type() {
        return ModTreeDecorators.RAINFOREST_CANOPY.get();
    }

    @Override
    public void place(Context context) {
        RandomSource random = context.random();

        for (BlockPos leaf : context.leaves()) {
            BlockPos start = leaf.below();
            if (!context.isAir(start)) {
                continue;
            }

            PlacementChoice choice = pick(random);
            if (choice == PlacementChoice.NOTHING) {
                continue;
            }

            if (isTooCloseToBlockingBelow(context, start)) {
                continue;
            }

            if (choice == PlacementChoice.SPORE_BLOSSOM) {
                placeSporeBlossom(context, start);
            } else {
                placeCaveVines(context, start, Math.max(1, this.caveVinesLength.sample(random)));
            }
        }
    }

    private PlacementChoice pick(RandomSource random) {
        int total = this.nothingWeight + this.sporeBlossomWeight + this.caveVinesWeight;
        if (total <= 0) {
            return PlacementChoice.NOTHING;
        }

        int roll = random.nextInt(total);
        if (roll < this.nothingWeight) {
            return PlacementChoice.NOTHING;
        }

        roll -= this.nothingWeight;
        if (roll < this.sporeBlossomWeight) {
            return PlacementChoice.SPORE_BLOSSOM;
        }

        return PlacementChoice.CAVE_VINES;
    }

    private void placeSporeBlossom(Context context, BlockPos pos) {
        BlockState state = Blocks.SPORE_BLOSSOM.defaultBlockState();
        if (context.level() instanceof LevelReader level && !state.canSurvive(level, pos)) {
            return;
        }

        context.setBlock(pos, state);
    }

    private void placeCaveVines(Context context, BlockPos start, int length) {
        List<BlockPos> positions = new ArrayList<>();
        BlockPos.MutableBlockPos cursor = start.mutable();

        for (int i = 0; i < length; i++) {
            if (!context.isAir(cursor) || isTooCloseToBlockingBelow(context, cursor)) {
                break;
            }

            positions.add(cursor.immutable());
            cursor.move(Direction.DOWN);
        }

        if (positions.isEmpty()) {
            return;
        }

        for (int i = 0; i < positions.size(); i++) {
            boolean last = i == positions.size() - 1;
            BlockState state = last
                    ? Blocks.CAVE_VINES.defaultBlockState()
                    : Blocks.CAVE_VINES_PLANT.defaultBlockState();

            if (state.hasProperty(BlockStateProperties.BERRIES)) {
                state = state.setValue(BlockStateProperties.BERRIES, true);
            }

            if (state.hasProperty(BlockStateProperties.WATERLOGGED)) {
                state = state.setValue(BlockStateProperties.WATERLOGGED, false);
            }

            if (state.is(Blocks.CAVE_VINES)) {
                state = state.setValue(CaveVinesBlock.AGE, 25);
            }

            context.setBlock(positions.get(i), state);
        }
    }

    private static boolean isTooCloseToBlockingBelow(Context context, BlockPos pos) {
        for (int dy = 0; dy <= 4; dy++) {
            BlockPos checkPos = pos.below(dy);
            if (context.level().isStateAtPosition(checkPos, state ->
                    !state.getFluidState().isEmpty() || !isPassThrough(context, checkPos, state)
            )) {
                return true;
            }
        }

        return false;
    }

    private static boolean isPassThrough(Context context, BlockPos pos, BlockState state) {
        if (state.isAir()) {
            return true;
        }

        if (context.level() instanceof LevelReader level) {
            return state.getCollisionShape(level, pos).isEmpty();
        }

        return false;
    }

    private enum PlacementChoice {
        NOTHING,
        SPORE_BLOSSOM,
        CAVE_VINES
    }
}

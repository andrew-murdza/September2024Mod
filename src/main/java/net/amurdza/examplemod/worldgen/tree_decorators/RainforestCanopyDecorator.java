package net.amurdza.examplemod.worldgen.tree_decorators;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.amurdza.examplemod.registry.ModTreeDecorators;
import net.amurdza.examplemod.util.GameplayDimensions;
import net.amurdza.examplemod.worldgen.CaveVinePlacement;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.treedecorators.TreeDecorator;
import net.minecraft.world.level.levelgen.feature.treedecorators.TreeDecoratorType;
import org.jetbrains.annotations.NotNull;

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

            if (choice == PlacementChoice.SPORE_BLOSSOM) {
                if (context.level() instanceof LevelReader level && GameplayDimensions.is(level, GameplayDimensions.TROPICAL)
                        && GameplayDimensions.biomeIs(level, start, "mangrove_swamp")) continue;
                if (context.level() instanceof LevelReader level && GameplayDimensions.is(level, GameplayDimensions.TROPICAL)
                        && random.nextInt(4) != 0) continue;
                if (!isTooCloseToBlockingBelow(context, start)) placeSporeBlossom(context, start);
            } else {
                CaveVinePlacement.place(context, start, this.caveVinesLength.sample(random));
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

        return !state.isSolid() && !state.is(net.minecraft.tags.BlockTags.LEAVES);
    }

    private enum PlacementChoice {
        NOTHING,
        SPORE_BLOSSOM,
        CAVE_VINES
    }
}

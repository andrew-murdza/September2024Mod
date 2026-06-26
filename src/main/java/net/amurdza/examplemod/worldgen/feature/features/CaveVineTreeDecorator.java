package net.amurdza.examplemod.worldgen.feature.features;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.amurdza.examplemod.registry.ModTreeDecorators;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.FloatProvider;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CaveVinesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.levelgen.feature.treedecorators.TreeDecorator;
import net.minecraft.world.level.levelgen.feature.treedecorators.TreeDecoratorType;
import org.jetbrains.annotations.NotNull;

public class CaveVineTreeDecorator extends TreeDecorator {
    public static final Codec<CaveVineTreeDecorator> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    FloatProvider.CODEC.fieldOf("probability").forGetter(decorator -> decorator.probability),
                    IntProvider.CODEC.fieldOf("length").forGetter(decorator -> decorator.length),
                    FloatProvider.CODEC.fieldOf("berries_probability").forGetter(decorator -> decorator.berriesProbability)
            ).apply(instance, CaveVineTreeDecorator::new)
    );

    private final FloatProvider probability;
    private final IntProvider length;
    private final FloatProvider berriesProbability;

    public CaveVineTreeDecorator(
            FloatProvider probability,
            IntProvider length,
            FloatProvider berriesProbability
    ) {
        this.probability = probability;
        this.length = length;
        this.berriesProbability = berriesProbability;
    }

    @Override
    protected @NotNull TreeDecoratorType<?> type() {
        return ModTreeDecorators.CAVE_VINES.get();
    }

    @Override
    public void place(TreeDecorator.Context context) {
        RandomSource random = context.random();

        float probabilitySample = this.probability.sample(random);
        int lengthSample = this.length.sample(random);
        BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();

        for (BlockPos log : context.logs()) {
            if (!(random.nextFloat() < probabilitySample)) {
                continue;
            }

            mutable.set(log).move(Direction.DOWN);

            if (!context.isAir(mutable)) {
                continue;
            }

            mutable.move(Direction.DOWN);

            if (!context.isAir(mutable)) {
                continue;
            }

            mutable.move(Direction.UP);

            boolean shouldBreak = false;

            for (int i = 1; i <= lengthSample; i++) {
                BlockState state;

                if (context.isAir(mutable.offset(0, -1, 0))
                        && context.isAir(mutable.offset(0, -2, 0))) {
                    state = i == lengthSample
                            ? Blocks.CAVE_VINES.defaultBlockState()
                            : Blocks.CAVE_VINES_PLANT.defaultBlockState();
                } else {
                    state = Blocks.CAVE_VINES.defaultBlockState();
                    shouldBreak = true;
                }

                state = state.setValue(BlockStateProperties.BERRIES, true);

                if (state.hasProperty(BlockStateProperties.WATERLOGGED)) {
                    state = state.setValue(BlockStateProperties.WATERLOGGED, false);
                }

                // Mixin behavior:
                // Any actual CAVE_VINES block gets age 25.
                // CAVE_VINES_PLANT does not have AGE_25.
                if (state.is(Blocks.CAVE_VINES)) {
                    state = state.setValue(CaveVinesBlock.AGE, 25);
                }

                // Mixin behavior:
                // Do not place cave vines too close to moss or fluid below.
                if (!hasMossOrFluidWithinFourBelow(context, mutable)&&!shouldBreak) {
                    context.setBlock(mutable, state);
                    mutable.move(Direction.DOWN);
                }
                else {
                    break;
                }
            }
        }
    }

    private static boolean hasMossOrFluidWithinFourBelow(TreeDecorator.Context context, BlockPos pos) {
        for (int dy = 1; dy <= 4; dy++) {
            if (context.level().isStateAtPosition(pos.below(dy), state ->
                    state.is(Blocks.MOSS_BLOCK) || !state.getFluidState().isEmpty()
            )) {
                return true;
            }
        }

        return false;
    }
}

package net.amurdza.examplemod.worldgen.blockstates;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProviderType;

import java.util.List;

public class DensityRangeBlockStateProvider extends BlockStateProvider {
    public static final Codec<DensityRangeBlockStateProvider> CODEC =
            RecordCodecBuilder.create(instance -> instance.group(
                    BlockState.CODEC.listOf()
                            .fieldOf("states")
                            .forGetter(provider -> provider.states),

                    Codec.DOUBLE
                            .fieldOf("min_value")
                            .forGetter(provider -> provider.minValue),

                    Codec.DOUBLE
                            .fieldOf("max_value")
                            .forGetter(provider -> provider.maxValue),

                    DensityFunction.HOLDER_HELPER_CODEC
                            .fieldOf("density")
                            .forGetter(provider -> provider.density)
            ).apply(instance, DensityRangeBlockStateProvider::new));

    private final List<BlockState> states;
    private final double minValue;
    private final double maxValue;
    private final DensityFunction density;

    public DensityRangeBlockStateProvider(
            List<BlockState> states,
            double minValue,
            double maxValue,
            DensityFunction density
    ) {
        if (states.isEmpty()) {
            throw new IllegalArgumentException("DensityRangeBlockStateProvider requires at least one block state");
        }

        if (maxValue <= minValue) {
            throw new IllegalArgumentException("max_value must be greater than min_value");
        }

        this.states = List.copyOf(states);
        this.minValue = minValue;
        this.maxValue = maxValue;
        this.density = density;
    }

    @Override
    protected BlockStateProviderType<?> type() {
        return ModBlockStateProviderTypes.DENSITY_RANGE;
    }

    @Override
    public BlockState getState(RandomSource random, BlockPos pos) {
        double value = density.compute(
                new DensityFunction.SinglePointContext(
                        pos.getX(),
                        pos.getY(),
                        pos.getZ()
                )
        );

        return states.get(getIndex(value));
    }

    private int getIndex(double value) {
        double normalized = (value - minValue) / (maxValue - minValue);
        int index = (int) Math.floor(normalized * states.size());

        if (index < 0) {
            return 0;
        }

        if (index >= states.size()) {
            return states.size() - 1;
        }

        return index;
    }
}
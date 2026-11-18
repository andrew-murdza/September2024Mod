package net.amurdza.examplemod.worldgen.feature.features;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.amurdza.examplemod.registry.ModTreeDecorators;
import net.amurdza.examplemod.worldgen.CaveVinePlacement;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.FloatProvider;
import net.minecraft.util.valueproviders.IntProvider;
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

    public CaveVineTreeDecorator(FloatProvider probability, IntProvider length, FloatProvider berriesProbability) {
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
        float chance = probability.sample(random);
        for (BlockPos log : context.logs()) {
            BlockPos start = log.below();
            if (random.nextFloat() < chance && context.isAir(start)) {
                CaveVinePlacement.place(context, start, length.sample(random));
            }
        }
    }
}

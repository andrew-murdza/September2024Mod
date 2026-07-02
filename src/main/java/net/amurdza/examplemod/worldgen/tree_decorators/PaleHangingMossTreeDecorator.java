package net.amurdza.examplemod.worldgen.tree_decorators;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.amurdza.examplemod.block.PaleHangingMossBlock;
import net.amurdza.examplemod.registry.ModBlocks;
import net.amurdza.examplemod.registry.ModTreeDecorators;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.treedecorators.TreeDecorator;
import net.minecraft.world.level.levelgen.feature.treedecorators.TreeDecoratorType;
import org.jetbrains.annotations.NotNull;

public class PaleHangingMossTreeDecorator extends TreeDecorator {
    public static final Codec<PaleHangingMossTreeDecorator> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    Codec.floatRange(0.0F, 1.0F).optionalFieldOf("probability", 0.18F).forGetter(decorator -> decorator.probability),
                    Codec.intRange(1, 16).optionalFieldOf("min_length", 1).forGetter(decorator -> decorator.minLength),
                    Codec.intRange(1, 16).optionalFieldOf("max_length", 3).forGetter(decorator -> decorator.maxLength)
            ).apply(instance, PaleHangingMossTreeDecorator::new)
    );

    private final float probability;
    private final int minLength;
    private final int maxLength;

    public PaleHangingMossTreeDecorator(float probability, int minLength, int maxLength) {
        this.probability = probability;
        this.minLength = Math.max(1, minLength);
        this.maxLength = Math.max(this.minLength, maxLength);
    }

    @Override
    protected @NotNull TreeDecoratorType<?> type() {
        return ModTreeDecorators.PALE_HANGING_MOSS.get();
    }

    @Override
    public void place(Context context) {
        RandomSource random = context.random();
        BlockState tip = ModBlocks.PALE_HANGING_MOSS.get().defaultBlockState().setValue(PaleHangingMossBlock.TIP, true);
        BlockState plant = tip.setValue(PaleHangingMossBlock.TIP, false);

        for (BlockPos leaf : context.leaves()) {
            if (random.nextFloat() >= this.probability) {
                continue;
            }

            BlockPos.MutableBlockPos cursor = leaf.mutable().move(Direction.DOWN);
            if (!context.isAir(cursor)) {
                continue;
            }

            int length = this.minLength + random.nextInt(this.maxLength - this.minLength + 1);
            BlockPos previous = null;

            for (int i = 0; i < length && context.isAir(cursor); i++) {
                BlockPos current = cursor.immutable();
                if (previous != null) {
                    context.setBlock(previous, plant);
                }
                context.setBlock(current, tip);
                previous = current;
                cursor.move(Direction.DOWN);
            }
        }
    }
}

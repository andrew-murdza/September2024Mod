package net.amurdza.examplemod.worldgen.tree_decorators;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.amurdza.examplemod.registry.ModTreeDecorators;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CocoaBlock;
import net.minecraft.world.level.block.VineBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.levelgen.feature.treedecorators.TreeDecorator;
import net.minecraft.world.level.levelgen.feature.treedecorators.TreeDecoratorType;
import org.jetbrains.annotations.NotNull;

import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;

public class RainforestTrunkDecorator extends TreeDecorator {
    public static final Codec<RainforestTrunkDecorator> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    Codec.BOOL.optionalFieldOf("has_cocoa", false).forGetter(decorator -> decorator.hasCocoa)
            ).apply(instance, RainforestTrunkDecorator::new)
    );

    private final boolean hasCocoa;

    public RainforestTrunkDecorator(boolean hasCocoa) {
        this.hasCocoa = hasCocoa;
    }

    @Override
    protected @NotNull TreeDecoratorType<?> type() {
        return ModTreeDecorators.RAINFOREST_TRUNK.get();
    }

    @Override
    public void place(Context context) {
        if (context.logs().isEmpty()) {
            return;
        }

        int baseY = context.logs().stream()
                .mapToInt(BlockPos::getY)
                .min()
                .orElse(0);

        Set<Long> baseColumns = new HashSet<>();
        for (BlockPos log : context.logs()) {
            if (log.getY() == baseY) {
                baseColumns.add(columnKey(log));
            }
        }

        context.logs().stream()
                .sorted(Comparator.comparingInt(BlockPos::getY))
                .forEach(log -> {
                    if (!baseColumns.contains(columnKey(log))) {
                        return;
                    }

                    int yDelta = log.getY() - baseY;
                    for (Direction direction : Direction.Plane.HORIZONTAL) {
                        BlockPos placePos = log.relative(direction);
                        if (!context.isAir(placePos)) {
                            continue;
                        }

                        if (this.hasCocoa && yDelta < 2) {
                            placeCocoa(context, placePos, direction.getOpposite());
                        } else {
                            placeVine(context, placePos, direction.getOpposite());
                        }
                    }
                });
    }

    private static long columnKey(BlockPos pos) {
        return (((long) pos.getX()) << 32) ^ (pos.getZ() & 0xffffffffL);
    }

    private static void placeCocoa(Context context, BlockPos pos, Direction facing) {
        BlockState state = Blocks.COCOA.defaultBlockState()
                .setValue(CocoaBlock.FACING, facing)
                .setValue(CocoaBlock.AGE, 2);

        if (context.level() instanceof LevelReader level && !state.canSurvive(level, pos)) {
            return;
        }

        context.setBlock(pos, state);
    }

    private static void placeVine(Context context, BlockPos pos, Direction faceTowardLog) {
        BooleanProperty face = VineBlock.getPropertyForFace(faceTowardLog);
        BlockState state = Blocks.VINE.defaultBlockState().setValue(face, true);

        if (state.hasProperty(BlockStateProperties.WATERLOGGED)
                && context.level() instanceof LevelReader level) {
            state = state.setValue(BlockStateProperties.WATERLOGGED, !level.getFluidState(pos).isEmpty());
        }

        if (context.level() instanceof LevelReader level && !state.canSurvive(level, pos)) {
            return;
        }

        context.setBlock(pos, state);
    }
}

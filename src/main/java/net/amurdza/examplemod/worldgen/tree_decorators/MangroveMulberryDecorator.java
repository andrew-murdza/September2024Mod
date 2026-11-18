package net.amurdza.examplemod.worldgen.tree_decorators;

import com.mojang.serialization.Codec;
import net.amurdza.examplemod.registry.ModTreeDecorators;
import net.amurdza.examplemod.util.GameplayDimensions;
import net.amurdza.examplemod.worldgen.TropicalMulberryTrees;
import net.amurdza.examplemod.worldgen.GrowthPlacementBuffer;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.feature.treedecorators.TreeDecorator;
import net.minecraft.world.level.levelgen.feature.treedecorators.TreeDecoratorType;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Comparator;
import java.util.function.BooleanSupplier;

/** Six accessible fruiting vines, only while placing a deliberately selected tree. */
public final class MangroveMulberryDecorator extends TreeDecorator {
    public static final MangroveMulberryDecorator INSTANCE = new MangroveMulberryDecorator();
    public static final Codec<MangroveMulberryDecorator> CODEC = Codec.unit(() -> INSTANCE);
    private static final ThreadLocal<FruitingAttempt> ATTEMPT = new ThreadLocal<>();
    private static final class FruitingAttempt { private int placed; }

    private MangroveMulberryDecorator() { }

    @Override
    protected TreeDecoratorType<?> type() {
        return ModTreeDecorators.MANGROVE_MULBERRY.get();
    }

    /** Call inside an uncommitted placement buffer; incomplete decorations reject the whole tree. */
    public static boolean withSixMulberries(BooleanSupplier generate) {
        FruitingAttempt previous = ATTEMPT.get();
        FruitingAttempt attempt = new FruitingAttempt();
        ATTEMPT.set(attempt);
        try {
            return generate.getAsBoolean() && attempt.placed == TropicalMulberryTrees.FRUIT_PER_TREE;
        } finally {
            if (previous == null) ATTEMPT.remove();
            else ATTEMPT.set(previous);
        }
    }

    /** Validate the finished tree, after other decorators and native leaf-distance updates. */
    public static boolean hasSixSupportedMulberries(GrowthPlacementBuffer buffer) {
        var mulberry = ForgeRegistries.BLOCKS.getValue(new ResourceLocation("upgrade_aquatic:mulberry_vine"));
        if (mulberry == null) return false;
        int[] count = {0};
        boolean supported = buffer.isValid((pos, state) -> {
            if (!state.is(mulberry)) return true;
            count[0]++;
            BlockState leaf = buffer.getBlockState(pos.above());
            return leaf.is(Blocks.MANGROVE_LEAVES) && leaf.getValue(BlockStateProperties.DISTANCE) < 7
                    && state.canSurvive(buffer, pos);
        });
        return supported && count[0] == TropicalMulberryTrees.FRUIT_PER_TREE;
    }

    @Override
    public void place(Context context) {
        FruitingAttempt attempt = ATTEMPT.get();
        if (attempt == null) return;
        attempt.placed = 0;
        if (!(context.level() instanceof LevelReader level)
                || !GameplayDimensions.is(level, GameplayDimensions.TROPICAL)
                || context.leaves().isEmpty()) return;
        var block = ForgeRegistries.BLOCKS.getValue(new ResourceLocation("upgrade_aquatic:mulberry_vine"));
        if (block == null) return;
        BlockState state = block.defaultBlockState();
        for (var property : state.getProperties()) {
            if (property.getName().equals("age") && property instanceof IntegerProperty age) {
                state = state.setValue(age, age.getPossibleValues().stream().max(Integer::compareTo).orElse(0));
            }
        }
        final BlockState fruit = state;
        int bottom = context.leaves().stream().mapToInt(BlockPos::getY).min().orElseThrow();
        var candidates = context.leaves().stream().filter(leaf -> leaf.getY() == bottom)
                .map(BlockPos::below).filter(pos -> context.isAir(pos) && context.isAir(pos.below())
                        && fruit.canSurvive(level, pos))
                .sorted(Comparator.comparingLong(BlockPos::asLong)).toList();
        if (candidates.size() < TropicalMulberryTrees.FRUIT_PER_TREE) return;
        var available = new java.util.ArrayList<>(candidates);
        for (int i = 0; i < TropicalMulberryTrees.FRUIT_PER_TREE; i++) {
            context.setBlock(available.remove(context.random().nextInt(available.size())), fruit);
            attempt.placed++;
        }
    }
}

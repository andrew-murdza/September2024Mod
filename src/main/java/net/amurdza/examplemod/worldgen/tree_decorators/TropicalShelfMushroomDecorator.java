package net.amurdza.examplemod.worldgen.tree_decorators;

import com.blackgear.vanillabackport.common.level.blocks.ShelfMushroomBlock;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.amurdza.examplemod.registry.ModTreeDecorators;
import net.amurdza.examplemod.worldgen.TropicalLayout;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.treedecorators.TreeDecorator;
import net.minecraft.world.level.levelgen.feature.treedecorators.TreeDecoratorType;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Comparator;

/** Decorates exposed low trunks while leaving mangroves free of shelf mushrooms. */
public final class TropicalShelfMushroomDecorator extends TreeDecorator {
    public static final Codec<TropicalShelfMushroomDecorator> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(Codec.floatRange(0.0F, 1.0F).fieldOf("probability")
                    .forGetter(decorator -> decorator.probability)).apply(instance, TropicalShelfMushroomDecorator::new));

    private final float probability;

    public TropicalShelfMushroomDecorator(float probability) {
        this.probability = probability;
    }

    @Override
    protected @NotNull TreeDecoratorType<?> type() {
        return ModTreeDecorators.TROPICAL_SHELF_MUSHROOM.get();
    }

    @Override
    public void place(Context context) {
        if (context.logs().isEmpty() || !(context.level() instanceof LevelReader level)) return;
        if (context.logs().stream().anyMatch(pos -> level.getBlockState(pos).is(Blocks.MANGROVE_LOG)
                || level.getBlockState(pos).is(Blocks.MANGROVE_WOOD))) return;
        var mushroom = ForgeRegistries.BLOCKS.getValue(new ResourceLocation("minecraft:shelf_mushroom"));
        if (mushroom == null || mushroom == Blocks.AIR) return;

        // New decoration must not change the caller's later tree choices or trunk shapes.
        // Include the full shape so trees at the same origin can still differ when regrown.
        var logs = context.logs().stream().sorted(Comparator.comparingLong(BlockPos::asLong)).toList();
        long seed = context.level() instanceof WorldGenLevel world ? world.getSeed() : 0L;
        for (BlockPos log : logs) seed = TropicalLayout.mix(seed ^ log.asLong());
        RandomSource random = RandomSource.create(seed ^ 0x5368656C6646756EL);
        if (random.nextFloat() >= probability) return;

        int baseY = logs.stream().mapToInt(BlockPos::getY).min().orElseThrow();
        var candidates = new ArrayList<Attachment>();
        for (BlockPos log : logs) {
            if (log.getY() > baseY + 4) continue;
            for (Direction direction : Direction.Plane.HORIZONTAL) {
                BlockPos pos = log.relative(direction);
                BlockState state = mushroom.defaultBlockState().setValue(HorizontalDirectionalBlock.FACING, direction);
                if (canAttach(level, pos, state)) candidates.add(new Attachment(pos, state));
            }
        }
        int count = 1 + random.nextInt(3);
        while (count > 0 && !candidates.isEmpty()) {
            Attachment attachment = candidates.remove(random.nextInt(candidates.size()));
            if (!canAttach(level, attachment.pos(), attachment.state())) continue;
            context.setBlock(attachment.pos(), attachment.state().setValue(ShelfMushroomBlock.AGE, random.nextInt(2)));
            count--;
        }
    }

    private static boolean canAttach(LevelReader level, BlockPos pos, BlockState state) {
        if (!level.getBlockState(pos).isAir() || !state.canSurvive(level, pos)) return false;
        if (level.getBlockState(pos.below()).getBlock() instanceof ShelfMushroomBlock
                || level.getBlockState(pos.above()).getBlock() instanceof ShelfMushroomBlock) return false;
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            if (level.getFluidState(pos.relative(direction)).is(FluidTags.WATER)) return false;
        }
        return true;
    }

    private record Attachment(BlockPos pos, BlockState state) { }
}

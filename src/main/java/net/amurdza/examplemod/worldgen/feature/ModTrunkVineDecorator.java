package net.amurdza.examplemod.worldgen.feature;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.amurdza.examplemod.block.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.MultifaceBlock;
import net.minecraft.world.level.block.VineBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.levelgen.feature.treedecorators.TreeDecorator;
import net.minecraft.world.level.levelgen.feature.treedecorators.TreeDecoratorType;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class ModTrunkVineDecorator extends TreeDecorator {

    // Register this in your ModTreeDecorators (example below)
    public static final Codec<ModTrunkVineDecorator> CODEC = RecordCodecBuilder.create(inst ->
            inst.group(
                    Codec.floatRange(0.0F, 1.0F).fieldOf("probability").forGetter(d -> d.probability),
                    Codec.intRange(1, 1000).fieldOf("grape_weight").forGetter(d -> d.grapeWeight),
                    Codec.intRange(1, 1000).fieldOf("lichen_weight").forGetter(d -> d.lichenWeight),
                    Codec.intRange(1, 1000).fieldOf("vine_weight").forGetter(d -> d.vineWeight),
                    Codec.intRange(1, 1000).optionalFieldOf("cherry_vine_weight",0).forGetter(d -> d.cherryVineWeight),
                    Codec.intRange(1, 1000).optionalFieldOf("cocoa_weight",0).forGetter(d -> d.cocoaWeight),
                    Codec.intRange(0, 1000).optionalFieldOf("max_log_y_delta", 2).forGetter(d -> d.maxLogYDelta),
                    Codec.floatRange(0.0F, 1.0F).optionalFieldOf("per_side_chance", 0.25F).forGetter(d -> d.perSideChance)
            ).apply(inst, ModTrunkVineDecorator::new)
    );

    private final float probability;
    private final int grapeWeight;
    private final int lichenWeight;
    private final int vineWeight;
    private final int cherryVineWeight;
    private final int cocoaWeight;
    private final int maxLogYDelta;
    private final float perSideChance;

    // Convenience properties (only applied if present on the chosen block)
    private static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
    private static final BooleanProperty BERRIES = BlockStateProperties.BERRIES;

    public ModTrunkVineDecorator(
            float probability,
            int grapeWeight,
            int lichenWeight,
            int vineWeight,
            int cherryVineWeight,
            int cocoaWeight,
            int maxLogYDelta,
            float perSideChance
    ) {
        this.probability = probability;
        this.grapeWeight = grapeWeight;
        this.lichenWeight = lichenWeight;
        this.vineWeight = vineWeight;
        this.cherryVineWeight = cherryVineWeight;
        this.cocoaWeight = cocoaWeight;
        this.maxLogYDelta = maxLogYDelta;
        this.perSideChance = perSideChance;
    }

    @Override
    protected @NotNull TreeDecoratorType<?> type() {
        return ModTreeDecorators.WEIGHTED_MULTIFACE_ON_LOGS.get();
    }

    @Override
    public void place(Context ctx) {
        RandomSource rand = ctx.random();

        // Roll once per tree like CocoaDecorator
        if (rand.nextFloat() >= this.probability) return;

        List<BlockPos> logs = ctx.logs();
        if (logs.isEmpty()) return;

        int baseY = logs.get(0).getY();

        for (BlockPos logPos : logs) {
            if (logPos.getY() - baseY > maxLogYDelta) continue;

            for (Direction dir : Direction.Plane.HORIZONTAL) {
                if (rand.nextFloat() > perSideChance) continue;

                // Candidate position is adjacent to this log
                BlockPos placePos = logPos.relative(dir);
                if (!ctx.isAir(placePos)) continue;

                // The block will attach TO the log, so the face points back at the log
                Direction faceTowardLog = dir.getOpposite();

                BlockChoice choice = pick(rand);
                tryPlace(ctx, placePos, faceTowardLog, choice, rand);
            }
        }
    }

    private enum BlockChoice { GRAPE, LICHEN, VINE, CHERRY_VINE, COCOA}

    private BlockChoice pick(RandomSource rand) {
        int total = grapeWeight + lichenWeight + vineWeight +cherryVineWeight + cocoaWeight;
        int r = rand.nextInt(total);
        if (r < grapeWeight) return BlockChoice.GRAPE;
        r -= grapeWeight;
        if (r < lichenWeight) return BlockChoice.LICHEN;
        r -= lichenWeight;
        if (r < cherryVineWeight) return BlockChoice.CHERRY_VINE;
        r -= cherryVineWeight;
        if (r < cocoaWeight) return BlockChoice.COCOA;
        return BlockChoice.VINE;
    }

    private void tryPlace(Context ctx, BlockPos pos, Direction faceTowardLog, BlockChoice choice, RandomSource rand) {
        if (!ctx.isAir(pos)) return;

        BlockState state;
        switch (choice) {
            case GRAPE -> state = ModBlocks.GRAPE_VINE.get().defaultBlockState();
            case LICHEN -> state = ModBlocks.JUNGLE_GLOW_LICHEN.get().defaultBlockState();
            case VINE -> state = Blocks.VINE.defaultBlockState();
            case CHERRY_VINE -> state = Blocks.VINE.defaultBlockState();
            case COCOA -> state = Blocks.COCOA.defaultBlockState().setValue(BlockStateProperties.AGE_2,2);
            default -> { return; }
        }

        // If the state has BERRIES, choose a value (example: 50/50)
        if (state.hasProperty(BERRIES)) {
            state = state.setValue(BERRIES, rand.nextBoolean());
        }

        // Only do WATERLOGGED / canSurvive / canAttachTo checks if we can view full world state
        var reader = ctx.level();
        if (reader instanceof net.minecraft.world.level.LevelReader level) {

            // WATERLOGGED: match fluid if property exists
            if (state.hasProperty(WATERLOGGED)) {
                boolean wl = !level.getFluidState(pos).isEmpty();
                state = state.setValue(WATERLOGGED, wl);
            }

            Block block = state.getBlock();

            if (block instanceof MultifaceBlock) {
                BlockPos neighborPos = pos.relative(faceTowardLog);
                BlockState neighborState = level.getBlockState(neighborPos);

                if (!MultifaceBlock.canAttachTo(level, faceTowardLog, neighborPos, neighborState)) return;

                BooleanProperty faceProp = MultifaceBlock.getFaceProperty(faceTowardLog);
                if (!state.hasProperty(faceProp)) return;

                BlockState placed = state.setValue(faceProp, true);
                if (!placed.canSurvive(level, pos)) return;

                ctx.setBlock(pos, placed);
                return;
            }
            BlockState placed = state;
            // Vines use face props for the 4 horizontal directions (no up/down props the same way).
            for (Direction dir : Direction.Plane.HORIZONTAL) {
                BooleanProperty prop = VineBlock.getPropertyForFace(dir);
                if (!placed.hasProperty(prop)) continue;

                BlockPos neighborPos = pos.relative(dir);

                // Same idea: only set the face if it can attach there.
                // VineBlock.canSupportAtFace exists in vanilla (1.20.x) and is what vines use internally.
                if (!((VineBlock)Blocks.VINE).canSupportAtFace(level, neighborPos, dir)) continue;

                if (!placed.getValue(prop)) {
                    placed = placed.setValue(prop, true);
                }
            }

            // fallback
            if (!state.canSurvive(level, pos)) return;
            ctx.setBlock(pos, state);
            return;
        }

        // If we don't have LevelReader, do a safe best-effort placement (no survival checks)
        Block block = state.getBlock();

        if (block instanceof MultifaceBlock) {
            BooleanProperty faceProp = MultifaceBlock.getFaceProperty(faceTowardLog);
            if (!state.hasProperty(faceProp)) return;
            ctx.setBlock(pos, state.setValue(faceProp, true));
            return;
        }

        if (block instanceof VineBlock) {
            BooleanProperty prop = VineBlock.getPropertyForFace(faceTowardLog);
            ctx.setBlock(pos, state.setValue(prop, true));
            return;
        }

        ctx.setBlock(pos, state);
    }
}
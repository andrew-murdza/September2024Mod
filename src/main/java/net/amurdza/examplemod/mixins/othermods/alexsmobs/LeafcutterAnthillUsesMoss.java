package net.amurdza.examplemod.mixins.othermods.alexsmobs;

import com.github.alexthe666.alexsmobs.block.AMBlockRegistry;
import com.github.alexthe666.alexsmobs.world.FeatureLeafcutterAnthill;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = FeatureLeafcutterAnthill.class, remap = false)
public abstract class LeafcutterAnthillUsesMoss {

    @Redirect(
            method = "place",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/WorldGenLevel;setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;I)Z",
                    remap = true
            )
    )
    private boolean aoemod$replaceAnthillMossBehavior(
            WorldGenLevel level,
            BlockPos pos,
            BlockState state,
            int flags
    ) {
        if (state.is(Blocks.COARSE_DIRT) || state.is(Blocks.DIRT)) {
            return level.setBlock(pos,Blocks.MOSS_BLOCK.defaultBlockState(),flags);
        }

        boolean result = level.setBlock(pos, state, flags);

        if (september2024Mod$isLeafcutterAnthillBlock(state)) {
            september2024Mod$placeMossAroundAnthill(level, pos, flags);
        } else if (september2024Mod$isLeafcutterChamberBlock(state)) {
            september2024Mod$placeMossAroundChamber(level, pos, flags);
        }

        return result;
    }

    @Unique
    private static void september2024Mod$placeMossAroundAnthill(WorldGenLevel level, BlockPos anthillPos, int flags) {
        for (Direction direction : Direction.values()) {
            if (direction == Direction.UP) {
                continue;
            }

            september2024Mod$placeMossIfNotAnthillOrChamber(level, anthillPos.relative(direction), flags);
        }
    }

    @Unique
    private static void september2024Mod$placeMossAroundChamber(WorldGenLevel level, BlockPos chamberPos, int flags) {
        for (Direction direction : Direction.values()) {
            september2024Mod$placeMossIfNotAnthillOrChamber(level, chamberPos.relative(direction), flags);
        }
    }

    @Unique
    private static void september2024Mod$placeMossIfNotAnthillOrChamber(WorldGenLevel level, BlockPos mossPos, int flags) {
        BlockState existingState = level.getBlockState(mossPos);

        if (september2024Mod$isLeafcutterAnthillBlock(existingState) || september2024Mod$isLeafcutterChamberBlock(existingState)) {
            return;
        }

        level.setBlock(mossPos, Blocks.MOSS_BLOCK.defaultBlockState(), flags);
    }

    @Unique
    private static boolean september2024Mod$isLeafcutterAnthillBlock(BlockState state) {
        return state.is(AMBlockRegistry.LEAFCUTTER_ANTHILL.get());
    }

    @Unique
    private static boolean september2024Mod$isLeafcutterChamberBlock(BlockState state) {
        return state.is(AMBlockRegistry.LEAFCUTTER_ANT_CHAMBER.get());
    }
}
package net.amurdza.examplemod.mixins.block_behavior;

import net.amurdza.examplemod.util.BiomePlacementRestrictions;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Covers dispensers, pistons, growth, commands and direct event placement as well as BlockItem. */
@Mixin({Level.class, WorldGenRegion.class})
public class DimensionBlockChanges {
    @Inject(method = "setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;II)Z",
            at = @At("HEAD"), cancellable = true)
    private void aoemod$enforceDimension(BlockPos pos, BlockState state, int flags, int recursion,
                                        CallbackInfoReturnable<Boolean> cir) {
        LevelReader level = (LevelReader) (Object) this;
        if (net.amurdza.examplemod.worldgen.structure.GoblinFortressBlocks.isAuthored(level, pos, state)) return;
        if ((state.is(net.minecraft.world.level.block.Blocks.GRASS) || state.is(net.minecraft.world.level.block.Blocks.TALL_GRASS))
                && level instanceof net.minecraft.world.level.WorldGenLevel world
                && net.amurdza.examplemod.util.GameplayDimensions.is(level, net.amurdza.examplemod.util.GameplayDimensions.SAVANNA)) {
            BlockPos ground = state.is(net.minecraft.world.level.block.Blocks.TALL_GRASS)
                    && state.getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.DOUBLE_BLOCK_HALF)
                        == net.minecraft.world.level.block.state.properties.DoubleBlockHalf.UPPER ? pos.below(2) : pos.below();
            if (level.getBlockState(ground).is(net.minecraft.world.level.block.Blocks.PODZOL)
                    && net.amurdza.examplemod.worldgen.StructurePlantExclusion.excludes(world, pos)) {
                cir.setReturnValue(false);
                return;
            }
        }
        if ((Object) this instanceof WorldGenRegion
                && !net.amurdza.examplemod.util.PlantGenerationSupport.isAllowed(level, pos, state)) {
            cir.setReturnValue(false);
            return;
        }
        if (!BiomePlacementRestrictions.isAllowed(level, pos, state)) cir.setReturnValue(false);
    }
}

package net.amurdza.examplemod.mixins;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.NyliumBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.lighting.LightEngine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(NyliumBlock.class)
public class NyliumNotKilledByGiantFungus {
    @Redirect(method = "canBeNylium",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/level/lighting/LightEngine;getLightBlockInto(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/Direction;I)I"))
    private static int stayAlive(BlockGetter world, BlockState state1, BlockPos pos1, BlockState state2, BlockPos pos2, Direction direction, int blockLight) {
        boolean b=!state2.is(BlockTags.LOGS_THAT_BURN)&&state2.is(BlockTags.LOGS);
        return b?0: LightEngine.getLightBlockInto(world, state1, pos1, state2, pos2, direction, blockLight);
    }
}

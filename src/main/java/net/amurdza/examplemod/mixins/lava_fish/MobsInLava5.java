package net.amurdza.examplemod.mixins.lava_fish;

import net.amurdza.examplemod.lava_fish.LavaMobs;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.behavior.BehaviorUtils;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.PathComputationType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(BehaviorUtils.class)
public class MobsInLava5 {
    @Redirect(
            method = "getRandomSwimmablePos",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/block/state/BlockState;isPathfindable(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/pathfinder/PathComputationType;)Z"
            )
    )
    private static boolean aoemod$lavaIsRandomSwimmablePos(
            BlockState state,
            BlockGetter level,
            BlockPos pos,
            PathComputationType type,
            PathfinderMob mob,
            int radius,
            int verticalDistance
    ) {
        if (LavaMobs.isLavaMob(mob)) {
            return mob.level().getFluidState(pos).is(FluidTags.LAVA);
        }

        return state.isPathfindable(level, pos, type);
    }
}

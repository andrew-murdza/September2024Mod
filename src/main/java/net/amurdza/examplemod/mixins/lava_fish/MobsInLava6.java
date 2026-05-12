package net.amurdza.examplemod.mixins.lava_fish;

import net.amurdza.examplemod.lava_fish.LavaMobs;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.TryFindWaterGoal;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(TryFindWaterGoal.class)
public abstract class MobsInLava6 {

    @Shadow @Final
    private PathfinderMob mob;

    @Redirect(
            method = {"canUse", "start"},
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/material/FluidState;is(Lnet/minecraft/tags/TagKey;)Z"
            )
    )
    private boolean aoemod$lavaMobsFindLavaInstead(
            FluidState fluidState,
            TagKey<Fluid> tag
    ) {
        if (LavaMobs.isLavaMob(this.mob)) {
            return fluidState.is(FluidTags.LAVA);
        }

        return fluidState.is(tag);
    }
}

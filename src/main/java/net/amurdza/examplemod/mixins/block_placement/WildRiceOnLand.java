package net.amurdza.examplemod.mixins.block_placement;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import vectorwing.farmersdelight.common.block.WildRiceBlock;

@Mixin(WildRiceBlock.class)
public class WildRiceOnLand {
    @Redirect(method = "canSurvive",at= @At(value = "INVOKE", target = "Lnet/minecraft/world/level/material/FluidState;getAmount()I"))
    private int hi(FluidState instance){
        return instance.is(FluidTags.WATER)?instance.getAmount():8;
    }
    @Redirect(method = "canSurvive",at= @At(value = "INVOKE", target = "Lnet/minecraft/world/level/material/FluidState;is(Lnet/minecraft/tags/TagKey;)Z"))
    private boolean hi(FluidState instance, TagKey<Fluid> pTag, BlockState state, LevelReader level, BlockPos pos){
        return instance.isEmpty()||instance.is(pTag);
    }
}

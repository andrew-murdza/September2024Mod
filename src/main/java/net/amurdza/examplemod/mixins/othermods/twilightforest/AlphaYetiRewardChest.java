package net.amurdza.examplemod.mixins.othermods.twilightforest;

import net.minecraft.core.BlockPos;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import twilightforest.entity.boss.AlphaYeti;

/** The shared boss-death handler supplies one ordinary chest with mountain rewards. */
@Mixin(AlphaYeti.class)
public abstract class AlphaYetiRewardChest {
    @Redirect(method = "die", at = @At(value = "INVOKE", remap = false,
            target = "Ltwilightforest/loot/TFLootTables;entityDropsIntoContainer(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/damagesource/DamageSource;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/BlockPos;)V"))
    private void aoemod$noDuplicateNativeChest(LivingEntity entity, DamageSource source, BlockState state, BlockPos pos) { }
}

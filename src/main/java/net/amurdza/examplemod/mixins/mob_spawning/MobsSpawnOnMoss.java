package net.amurdza.examplemod.mixins.mob_spawning;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(BlockBehaviour.Properties.class)
public class MobsSpawnOnMoss {
    @Shadow BlockBehaviour.StateArgumentPredicate<EntityType<?>> isValidSpawn;
    @Redirect(method = "lambda$new$2",at= @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/state/BlockState;getLightEmission(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;)I"))
    private static int hi(BlockState instance, BlockGetter blockGetter, BlockPos blockPos) {
        return 0;
    }
}
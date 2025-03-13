package net.amurdza.examplemod.mixins.othermods.netherific;

import net.mcreator.nourishednether.block.GhoulflowerBlock;
import net.minecraft.world.level.LevelAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(GhoulflowerBlock.class)
public class GhoulFlowerNotDuplicatedBonemeal {
    @Redirect(method = "performBonemeal",at= @At(value = "INVOKE", target = "Lnet/mcreator/nourishednether/procedures/GhoulflowerOnBoneMealSuccessProcedure;execute(Lnet/minecraft/world/level/LevelAccessor;DDD)V"))
    private void hi(LevelAccessor entityToSpawn, double _level, double world, double x){

    }
}

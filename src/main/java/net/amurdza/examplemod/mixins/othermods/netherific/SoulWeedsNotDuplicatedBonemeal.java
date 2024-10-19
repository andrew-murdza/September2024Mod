package net.amurdza.examplemod.mixins.othermods.netherific;

import net.mcreator.nourishednether.block.SoulWeedsBlock;
import net.minecraft.world.level.LevelAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(SoulWeedsBlock.class)
public class SoulWeedsNotDuplicatedBonemeal {
//    @Redirect(method = "performBonemeal",at= @At(value = "INVOKE", target = "Lnet/mcreator/nourishednether/procedures/SoulWeedsOnBoneMealSuccessProcedure;execute(Lnet/minecraft/world/level/LevelAccessor;DDD)V"))
//    private void hi(LevelAccessor entityToSpawn, double _level, double world, double x){
//
//    }
}

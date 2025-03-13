package net.amurdza.examplemod.mixins.othermods.nethers_exoticism;

import net.amurdza.examplemod.Config;
import net.amurdza.examplemod.util.Helper;
import net.mcreator.nethersexoticism.block.PitayaStemBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(PitayaStemBlock.class)
public class PitayaNoDamage {
    @Redirect(method = "entityInside",at= @At(value = "INVOKE", target = "Lnet/mcreator/nethersexoticism/procedures/PitayaStemEntityCollidesInTheBlockProcedure;execute(Lnet/minecraft/world/level/LevelAccessor;Lnet/minecraft/world/entity/Entity;)V"))
    private void hi(LevelAccessor world, Entity entity){

    }
}

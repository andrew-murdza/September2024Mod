package net.amurdza.examplemod.mixins.sea_level;

import net.amurdza.examplemod.worldgen.WorldGenUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(targets = "net.minecraft.world.entity.monster.Phantom$PhantomAttackStrategyGoal")
public class Phantom1 {
    @Redirect(method = "setAnchorAboveTarget",at= @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;getSeaLevel()I"))
    private int hi(Level instance){
        return WorldGenUtils.getSeaLevelWorldGen(new BlockPos(0,0,0),instance);//replace with Desert Pos
    }
}

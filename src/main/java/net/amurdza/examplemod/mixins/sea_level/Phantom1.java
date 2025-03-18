package net.amurdza.examplemod.mixins.sea_level;

import net.amurdza.examplemod.worldgen.WorldGenUtils;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(targets = "net.minecraft.world.entity.monster.Phantom$PhantomAttackStrategyGoal")
public class Phantom1 {
    @Redirect(method = "setAnchorAboveTarget",at= @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;getSeaLevel()I"))
    private int hi(Level instance){
        return WorldGenUtils.getSeaLevelAtPos(0,0,0);//replace with Desert Pos
    }
}

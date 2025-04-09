package net.amurdza.examplemod.mixins.sea_level;

import net.amurdza.examplemod.worldgen.WorldGenUtils;
import net.minecraft.world.entity.animal.Turtle;
import net.minecraft.world.entity.monster.Drowned;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(targets = "net.minecraft.world.entity.monster.Drowned$DrownedGoToBeachGoal")
public class Drowned2 {
    @Shadow @Final private Drowned drowned;

    @Redirect(method = "canUse",at= @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;getSeaLevel()I"))
    private int hi(Level instance){
        return WorldGenUtils.getTotalWaterAbove(drowned.blockPosition(),instance)>=3?300:-300;
    }
}

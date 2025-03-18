package net.amurdza.examplemod.mixins.sea_level;

import net.amurdza.examplemod.worldgen.WorldGenUtils;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.pathfinder.AmphibiousNodeEvaluator;
import net.minecraft.world.level.pathfinder.NodeEvaluator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(AmphibiousNodeEvaluator.class)
public abstract class AmphibiousNodeEvaluator1 extends NodeEvaluator {
    @Redirect(method = "getNeighbors",at= @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;getSeaLevel()I"))
    private int hi(Level instance){
        return WorldGenUtils.getSeaLevelAtPos(mob.blockPosition());
    }
}

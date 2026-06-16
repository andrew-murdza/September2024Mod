package net.amurdza.examplemod.mixins.accessor;

import net.minecraft.world.level.levelgen.SurfaceRules;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(SurfaceRules.Context.class)
public interface SurfaceRulesContextAccessor {

    @Accessor("stoneDepthAbove")
    int aoemod$getStoneDepthAbove();
}
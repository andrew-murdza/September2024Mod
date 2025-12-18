package net.amurdza.examplemod.mixins.access;

import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.SurfaceRules;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(SurfaceRules.Context.class)
public interface SurfaceRulesContext {
    @Accessor("blockX") int aoe$getBlockX();
    @Accessor("blockY") int aoe$getBlockY();
    @Accessor("blockZ") int aoe$getBlockZ();
    @Accessor("randomState")
    RandomState aoe$getRandomState();
}

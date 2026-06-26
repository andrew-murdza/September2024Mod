package net.amurdza.examplemod.mixins.accessor;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.SurfaceRules;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.function.Function;

@Mixin(SurfaceRules.Context.class)
public interface SurfaceRulesContextAccessor {

    @Accessor("stoneDepthAbove")
    int aoemod$getStoneDepthAbove();

    @Accessor("blockX")
    int aoemod$getBlockX();

    @Accessor("blockY")
    int aoemod$getBlockY();

    @Accessor("blockZ")
    int aoemod$getBlockZ();

    @Accessor("biomeGetter")
    Function<BlockPos, Holder<Biome>> aoemod$getBiomeGetter();
}
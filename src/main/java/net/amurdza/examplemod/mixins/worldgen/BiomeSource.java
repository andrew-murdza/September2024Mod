package net.amurdza.examplemod.mixins.worldgen;

import com.mojang.datafixers.util.Pair;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.biome.MultiNoiseBiomeSource;
import org.spongepowered.asm.mixin.Debug;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Debug(export = true)
@Mixin(value = MultiNoiseBiomeSource.class, priority = 1001)
public abstract class BiomeSource {
    @Shadow protected abstract Climate.ParameterList<Holder<Biome>> parameters();

    @Unique
    private static final ResourceKey<Biome> september2024Mod$BADLANDS = ResourceKey.create(Registries.BIOME, new ResourceLocation("aoemod", "badlands"));
    @Unique
    private static final ResourceKey<Biome> september2024Mod$CRIMSON = ResourceKey.create(Registries.BIOME, new ResourceLocation("aoemod", "crimson_forest"));
    @Unique
    private static final ResourceKey<Biome> september2024Mod$WARPED = ResourceKey.create(Registries.BIOME, new ResourceLocation("aoemod", "warped_forest"));
    @Unique
    private static final ResourceKey<Biome> september2024Mod$SOUL = ResourceKey.create(Registries.BIOME, new ResourceLocation("aoemod", "soul_sand_valley"));
    @Unique
    private static final ResourceKey<Biome> september2024Mod$BASALT = ResourceKey.create(Registries.BIOME, new ResourceLocation("aoemod", "basalt_deltas"));
    @Unique
    private static final ResourceKey<Biome> september2024Mod$GROVE = ResourceKey.create(Registries.BIOME, new ResourceLocation("aoemod", "grove"));
    @Unique
    private static final ResourceKey<Biome> MUSHROOM_CAVES   = ResourceKey.create(Registries.BIOME, new ResourceLocation("aoemod", "mushroom_caves"));

    @Unique
    private Holder<Biome> september2024Mod$biome(ResourceKey<Biome> key) {
        return parameters().values().stream()
                .map(Pair::getSecond)
                .filter(h -> h.is(key))
                .findFirst()
                .orElseThrow();
    }
    @Unique
    private Holder<Biome> aoemod$stackNetherUnderBadlands(int x, int y, int z, Climate.Sampler sampler) {
        Holder<Biome> base = parameters().findValue(sampler.sample(x, y, z));

        // Only stack when the climate picked your "surface" biome
        if(base.is(september2024Mod$GROVE)&&y>16&&y<20){
            return september2024Mod$biome(MUSHROOM_CAVES);
        }

        if (!base.is(september2024Mod$BADLANDS)) return base;

        // thresholds: 16, 8, 0, -8  => 5 layers
        if (y > 16)  return base;            // badlands
        if (y > 8)   return september2024Mod$biome(september2024Mod$CRIMSON);  // crimson_forest
        if (y > 0)   return september2024Mod$biome(september2024Mod$WARPED);   // warped_forest
        if (y > -8)  return september2024Mod$biome(september2024Mod$SOUL);     // soul_sand_valley
        return september2024Mod$biome(september2024Mod$BASALT);                // basalt_deltas
    }

    @Inject(
            method = "getNoiseBiome(IIILnet/minecraft/world/level/biome/Climate$Sampler;)Lnet/minecraft/core/Holder;",
            at = @At("RETURN"),
            cancellable = true
    )
    private void aoemod$getNoiseBiome(int x, int y, int z, Climate.Sampler sampler, CallbackInfoReturnable<Holder<Biome>> cir) {
        cir.setReturnValue(aoemod$stackNetherUnderBadlands(x, y, z, sampler));
    }
}


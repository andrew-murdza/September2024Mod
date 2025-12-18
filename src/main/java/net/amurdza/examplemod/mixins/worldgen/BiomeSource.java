package net.amurdza.examplemod.mixins.worldgen;

import com.mojang.datafixers.util.Pair;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.biome.MultiNoiseBiomeSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(value = MultiNoiseBiomeSource.class, priority = 1001)
public abstract class BiomeSource {
    @Shadow protected abstract Climate.ParameterList<Holder<Biome>> parameters();

    private static final ResourceKey<Biome> BADLANDS = ResourceKey.create(Registries.BIOME, new ResourceLocation("aoemod", "badlands"));
    private static final ResourceKey<Biome> CRIMSON  = ResourceKey.create(Registries.BIOME, new ResourceLocation("aoemod", "crimson_forest"));
    private static final ResourceKey<Biome> WARPED   = ResourceKey.create(Registries.BIOME, new ResourceLocation("aoemod", "warped_forest"));
    private static final ResourceKey<Biome> SOUL     = ResourceKey.create(Registries.BIOME, new ResourceLocation("aoemod", "soul_sand_valley"));
    private static final ResourceKey<Biome> BASALT   = ResourceKey.create(Registries.BIOME, new ResourceLocation("aoemod", "basalt_deltas"));
    private static final ResourceKey<Biome> GROVE   = ResourceKey.create(Registries.BIOME, new ResourceLocation("aoemod", "grove"));
    private static final ResourceKey<Biome> MUSHROOM_CAVES   = ResourceKey.create(Registries.BIOME, new ResourceLocation("aoemod", "mushroom_caves"));

    private Holder<Biome> biome(ResourceKey<Biome> key) {
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
        if(base.is(GROVE)&&y>16&&y<20){
            return biome(MUSHROOM_CAVES);
        }

        if (!base.is(BADLANDS)) return base;

        // thresholds: 16, 8, 0, -8  => 5 layers
        if (y > 16)  return base;            // badlands
        if (y > 8)   return biome(CRIMSON);  // crimson_forest
        if (y > 0)   return biome(WARPED);   // warped_forest
        if (y > -8)  return biome(SOUL);     // soul_sand_valley
        return biome(BASALT);                // basalt_deltas
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


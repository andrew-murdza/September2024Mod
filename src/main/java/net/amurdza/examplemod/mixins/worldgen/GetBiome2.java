package net.amurdza.examplemod.mixins.worldgen;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.biome.MultiNoiseBiomeSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.HashMap;
import java.util.Map;

@Mixin(MultiNoiseBiomeSource.class)
public abstract class GetBiome2 extends BiomeSource {

    @Unique
    private static final ResourceKey<Biome> AOE_JUNGLE =
            ResourceKey.create(Registries.BIOME, new ResourceLocation("aoemod", "jungle"));
    @Unique
    private static final ResourceKey<Biome> AOE_SAVANNA =
            ResourceKey.create(Registries.BIOME, new ResourceLocation("aoemod", "savanna"));
    @Unique
    private static final ResourceKey<Biome> AOE_PLAINS =
            ResourceKey.create(Registries.BIOME, new ResourceLocation("aoemod", "plains"));
    @Unique
    private static final ResourceKey<Biome> AOE_GROVE =
            ResourceKey.create(Registries.BIOME, new ResourceLocation("aoemod", "grove"));
    @Unique
    private static final ResourceKey<Biome> AOE_MUSHROOM_CAVES =
            ResourceKey.create(Registries.BIOME, new ResourceLocation("aoemod", "mushroom_caves"));
    @Unique
    private static final ResourceKey<Biome> AOE_BADLANDS =
            ResourceKey.create(Registries.BIOME, new ResourceLocation("aoemod", "badlands"));
    @Unique
    private static final ResourceKey<Biome> AOE_DEEP_DARK =
            ResourceKey.create(Registries.BIOME, new ResourceLocation("aoemod", "deep_dark"));
    @Unique
    private static final ResourceKey<Biome> AOE_CRIMSON =
            ResourceKey.create(Registries.BIOME, new ResourceLocation("aoemod", "crimson_forest"));
    @Unique
    private static final ResourceKey<Biome> AOE_WARPED =
            ResourceKey.create(Registries.BIOME, new ResourceLocation("aoemod", "warped_forest"));
    @Unique
    private static final ResourceKey<Biome> AOE_SOUL =
            ResourceKey.create(Registries.BIOME, new ResourceLocation("aoemod", "soul_sand_valley"));
    @Unique
    private static final ResourceKey<Biome> AOE_BASALT =
            ResourceKey.create(Registries.BIOME, new ResourceLocation("aoemod", "basalt_deltas"));

    @Unique
    private Map<ResourceKey<Biome>, Holder<Biome>> aoe$biomeCache;

    @Inject(
            method = "getNoiseBiome(IIILnet/minecraft/world/level/biome/Climate$Sampler;)Lnet/minecraft/core/Holder;",
            at = @At("HEAD"),
            cancellable = true
    )
    private void aoe$useBlockYForBiome(
            int x, int y, int z, Climate.Sampler pSampler, CallbackInfoReturnable<Holder<Biome>> cir
    ) {
        Map<ResourceKey<Biome>, Holder<Biome>> biomes = this.aoe$getBiomeCache();
        y *= 4;
        z *= 4;

        if (!aoe$hasRequiredBiomes(biomes)) {
            return;
        }

        /*
         * IMPORTANT:
         * Use floorMod, not %, or negative Z values produce negative cycle positions.
         *
         * This gives:
         * z = 0       -> cyclePos = 0.0
         * z = 4800    -> cyclePos = 5.0
         * z = 9599    -> cyclePos ~= 9.999
         */
        int biomeWidth=1024;

        int cyclePos = z % (biomeWidth * 10);
        if(cyclePos >= biomeWidth * 5){
            cyclePos = cyclePos - 5 * biomeWidth;
        }

        if (cyclePos < biomeWidth) {
            cir.setReturnValue(biomes.get(AOE_JUNGLE));
        }
        else if (cyclePos < 2 * biomeWidth) {
            cir.setReturnValue(biomes.get(AOE_SAVANNA));
        }
        else if (cyclePos < 3 * biomeWidth) {
            cir.setReturnValue(biomes.get(AOE_PLAINS));
        }
        else if (cyclePos < 4 * biomeWidth) {
            if (y <= 56 && y > 12) {
                cir.setReturnValue(biomes.get(AOE_MUSHROOM_CAVES));
            } else {
                cir.setReturnValue(biomes.get(AOE_GROVE));
            }
        }
        else {
            if (y < -40) {
                cir.setReturnValue(biomes.get(AOE_BASALT));
            } else if (y < -16) {
                cir.setReturnValue(biomes.get(AOE_CRIMSON));
            } else if (y < 8) {
                cir.setReturnValue(biomes.get(AOE_WARPED));
            } else if (y < 32) {
                cir.setReturnValue(biomes.get(AOE_SOUL));
            } else if (y < 56) {
                cir.setReturnValue(biomes.get(AOE_DEEP_DARK));
            } else {
                cir.setReturnValue(biomes.get(AOE_BADLANDS));
            }
        }
    }

    @Unique
    private static boolean aoe$hasRequiredBiomes(Map<ResourceKey<Biome>, Holder<Biome>> biomes) {
        return biomes.containsKey(AOE_JUNGLE)
                && biomes.containsKey(AOE_SAVANNA)
                && biomes.containsKey(AOE_PLAINS)
                && biomes.containsKey(AOE_GROVE)
                && biomes.containsKey(AOE_MUSHROOM_CAVES)
                && biomes.containsKey(AOE_BADLANDS)
                && biomes.containsKey(AOE_DEEP_DARK)
                && biomes.containsKey(AOE_CRIMSON)
                && biomes.containsKey(AOE_WARPED)
                && biomes.containsKey(AOE_SOUL)
                && biomes.containsKey(AOE_BASALT);
    }

    @Unique
    private Map<ResourceKey<Biome>, Holder<Biome>> aoe$getBiomeCache() {
        if (this.aoe$biomeCache == null) {
            Map<ResourceKey<Biome>, Holder<Biome>> cache = new HashMap<>();

            for (Holder<Biome> holder : possibleBiomes()) {
                holder.unwrapKey().ifPresent(key -> cache.put(key, holder));
            }

            this.aoe$biomeCache = Map.copyOf(cache);
        }

        return this.aoe$biomeCache;
    }
}
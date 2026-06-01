package net.amurdza.examplemod.mixins.worldgen;

import net.amurdza.examplemod.Config;
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
public abstract class MultiNoiseBiomeSourceLayeredNetherMixin {

//    @Unique private static final ResourceKey<Biome> AOE_WARM_OCEAN =
//            ResourceKey.create(Registries.BIOME, new ResourceLocation("aoemod", "warm_ocean"));
//    @Unique private static final ResourceKey<Biome> AOE_JUNGLE =
//            ResourceKey.create(Registries.BIOME, new ResourceLocation("aoemod", "jungle"));
//    @Unique private static final ResourceKey<Biome> AOE_SAVANNA =
//            ResourceKey.create(Registries.BIOME, new ResourceLocation("aoemod", "savanna"));
//    @Unique private static final ResourceKey<Biome> AOE_PLAINS =
//            ResourceKey.create(Registries.BIOME, new ResourceLocation("aoemod", "plains"));
//    @Unique private static final ResourceKey<Biome> AOE_GROVE =
//            ResourceKey.create(Registries.BIOME, new ResourceLocation("aoemod", "grove"));
//    @Unique private static final ResourceKey<Biome> AOE_MUSHROOM_CAVES =
//            ResourceKey.create(Registries.BIOME, new ResourceLocation("aoemod", "mushroom_caves"));
//    @Unique private static final ResourceKey<Biome> AOE_BADLANDS =
//            ResourceKey.create(Registries.BIOME, new ResourceLocation("aoemod", "badlands"));
//    @Unique private static final ResourceKey<Biome> AOE_DEEP_DARK =
//            ResourceKey.create(Registries.BIOME, new ResourceLocation("aoemod", "deep_dark"));
//    @Unique private static final ResourceKey<Biome> AOE_CRIMSON =
//            ResourceKey.create(Registries.BIOME, new ResourceLocation("aoemod", "crimson_forest"));
//    @Unique private static final ResourceKey<Biome> AOE_WARPED =
//            ResourceKey.create(Registries.BIOME, new ResourceLocation("aoemod", "warped_forest"));
//    @Unique private static final ResourceKey<Biome> AOE_SOUL =
//            ResourceKey.create(Registries.BIOME, new ResourceLocation("aoemod", "soul_sand_valley"));
//    @Unique private static final ResourceKey<Biome> AOE_BASALT =
//            ResourceKey.create(Registries.BIOME, new ResourceLocation("aoemod", "basalt_deltas"));
//
//    @Unique private Map<ResourceKey<Biome>, Holder<Biome>> aoe$biomeCache;
//
//    @Inject(
//            method = "getNoiseBiome(IIILnet/minecraft/world/level/biome/Climate$Sampler;)Lnet/minecraft/core/Holder;",
//            at = @At("HEAD"),
//            cancellable = true
//    )
//    private void aoe$forceHardLayeredNetherBiomes(
//            int x,
//            int y,
//            int z,
//            Climate.Sampler sampler,
//            CallbackInfoReturnable<Holder<Biome>> cir
//    ) {
//        Map<ResourceKey<Biome>, Holder<Biome>> biomes = this.aoe$getBiomeCache();
//
//        // Only affect your AOE biome source, not every multi-noise source.
//        if (!biomes.containsKey(AOE_WARM_OCEAN)
//                || !biomes.containsKey(AOE_JUNGLE)
//                || !biomes.containsKey(AOE_SAVANNA)
//                || !biomes.containsKey(AOE_PLAINS)
//                || !biomes.containsKey(AOE_GROVE)
//                || !biomes.containsKey(AOE_MUSHROOM_CAVES)
//                || !biomes.containsKey(AOE_BADLANDS)
//                || !biomes.containsKey(AOE_DEEP_DARK)
//                || !biomes.containsKey(AOE_CRIMSON)
//                || !biomes.containsKey(AOE_WARPED)
//                || !biomes.containsKey(AOE_SOUL)
//                || !biomes.containsKey(AOE_BASALT)) {
//            return;
//        }
//
//        double width = Config.BAND_WIDTH;
//        double dist = 4.0D * Math.sqrt((double) x * (double) x + (double) z * (double) z);
//        double bandPos = dist/width % 12;
//        double buffer = 32;
//        double bufferNormalized = buffer/width;
//        y *= 4;
//
//        if(bandPos>6){
//            bandPos=12-bandPos;
//        }
//        if(bandPos < 1){
//            cir.setReturnValue(biomes.get(AOE_WARM_OCEAN));
//        }
//        else if(bandPos < 2){
//            cir.setReturnValue(biomes.get(AOE_JUNGLE));
//        }
//        else if(bandPos < 3){
//            cir.setReturnValue(biomes.get(AOE_SAVANNA));
//        }
//        else if(bandPos < 4){
//            cir.setReturnValue(biomes.get(AOE_PLAINS));
//        }
//        else if(bandPos < 4+bufferNormalized){
//            cir.setReturnValue(biomes.get(AOE_GROVE));
//        }
//        else if(bandPos < 5-bufferNormalized){
//            if(y < 121 && y> 63){
//                cir.setReturnValue(biomes.get(AOE_MUSHROOM_CAVES));
//            }
//            else {
//                cir.setReturnValue(biomes.get(AOE_GROVE));
//            }
//        }
//        else if(bandPos < 5){
//            cir.setReturnValue(biomes.get(AOE_GROVE));
//        }
//        else if(bandPos < 5 + bufferNormalized){
//            cir.setReturnValue(biomes.get(AOE_BADLANDS));
//        }
//        else {
//            if (y < -96) {
//                cir.setReturnValue(biomes.get(AOE_BASALT));
//            } else if (y < -64) {
//                cir.setReturnValue(biomes.get(AOE_CRIMSON));
//
//            } else if (y < -32) {
//                cir.setReturnValue(biomes.get(AOE_WARPED));
//            } else if (y < 0) {
//                cir.setReturnValue(biomes.get(AOE_SOUL));
//            } else if (y < 32) {
//                cir.setReturnValue(biomes.get(AOE_DEEP_DARK));
//            } else {
//                cir.setReturnValue(biomes.get(AOE_BADLANDS));
//            }
//        }
//    }
//
//    @Unique
//    private Map<ResourceKey<Biome>, Holder<Biome>> aoe$getBiomeCache() {
//        if (this.aoe$biomeCache == null) {
//            Map<ResourceKey<Biome>, Holder<Biome>> cache = new HashMap<>();
//
//            for (Holder<Biome> holder : ((BiomeSource)(Object)this).possibleBiomes()) {
//                holder.unwrapKey().ifPresent(key -> cache.put(key, holder));
//            }
//
//            this.aoe$biomeCache = Map.copyOf(cache);
//        }
//
//        return this.aoe$biomeCache;
//    }
}
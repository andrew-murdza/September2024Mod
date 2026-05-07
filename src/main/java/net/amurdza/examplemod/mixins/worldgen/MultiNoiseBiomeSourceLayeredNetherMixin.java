package net.amurdza.examplemod.mixins.worldgen;

import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
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

    @Unique private static final long AOE_BADLANDS_CONTINENTALNESS = Climate.quantizeCoord(0.27F);

    @Unique private static final ResourceKey<Biome> AOE_BADLANDS =
            ResourceKey.create(Registries.BIOME, new ResourceLocation("aoemod", "badlands"));
    @Unique private static final ResourceKey<Biome> AOE_DEEP_DARK =
            ResourceKey.create(Registries.BIOME, new ResourceLocation("aoemod", "deep_dark"));
    @Unique private static final ResourceKey<Biome> AOE_CRIMSON =
            ResourceKey.create(Registries.BIOME, new ResourceLocation("aoemod", "crimson_forest"));
    @Unique private static final ResourceKey<Biome> AOE_WARPED =
            ResourceKey.create(Registries.BIOME, new ResourceLocation("aoemod", "warped_forest"));
    @Unique private static final ResourceKey<Biome> AOE_SOUL =
            ResourceKey.create(Registries.BIOME, new ResourceLocation("aoemod", "soul_sand_valley"));
    @Unique private static final ResourceKey<Biome> AOE_BASALT =
            ResourceKey.create(Registries.BIOME, new ResourceLocation("aoemod", "basalt_deltas"));

    @Unique private Map<ResourceKey<Biome>, Holder<Biome>> aoe$biomeCache;

    @Inject(
            method = "getNoiseBiome(IIILnet/minecraft/world/level/biome/Climate$Sampler;)Lnet/minecraft/core/Holder;",
            at = @At("HEAD"),
            cancellable = true
    )
    private void aoe$forceHardLayeredNetherBiomes(
            int quartX,
            int quartY,
            int quartZ,
            Climate.Sampler sampler,
            CallbackInfoReturnable<Holder<Biome>> cir
    ) {
        Map<ResourceKey<Biome>, Holder<Biome>> biomes = this.aoe$getBiomeCache();

        // Only affect your AOE biome source, not every multi-noise source.
        if (!biomes.containsKey(AOE_BADLANDS)
                || !biomes.containsKey(AOE_DEEP_DARK)
                || !biomes.containsKey(AOE_CRIMSON)
                || !biomes.containsKey(AOE_WARPED)
                || !biomes.containsKey(AOE_SOUL)
                || !biomes.containsKey(AOE_BASALT)) {
            return;
        }

        Climate.TargetPoint target = sampler.sample(quartX, quartY, quartZ);

        // Only under the badlands/high-continentalness region.

        if (target.continentalness() < AOE_BADLANDS_CONTINENTALNESS) {
            return;
        }

        int blockY = QuartPos.toBlock(quartY);

        if (blockY < -96) {
            cir.setReturnValue(biomes.get(AOE_BASALT));
        } else if (blockY < -64) {
            cir.setReturnValue(biomes.get(AOE_SOUL));
        } else if (blockY < -32) {
            cir.setReturnValue(biomes.get(AOE_WARPED));
        } else if (blockY < 0) {
            cir.setReturnValue(biomes.get(AOE_CRIMSON));
        } else if (blockY < 32) {
            cir.setReturnValue(biomes.get(AOE_DEEP_DARK));
        } else {
            cir.setReturnValue(biomes.get(AOE_BADLANDS));
        }
    }

    @Unique
    private Map<ResourceKey<Biome>, Holder<Biome>> aoe$getBiomeCache() {
        if (this.aoe$biomeCache == null) {
            this.aoe$biomeCache = new HashMap<>();

            for (Holder<Biome> holder : ((BiomeSource)(Object)this).possibleBiomes()) {
                holder.unwrapKey().ifPresent(key -> this.aoe$biomeCache.put(key, holder));
            }
        }

        return this.aoe$biomeCache;
    }
}
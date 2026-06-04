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
public abstract class MultiNoiseBiomeSourceLayeredNetherMixin {

    @Unique private static final ResourceKey<Biome> AOE_WARM_OCEAN =
            ResourceKey.create(Registries.BIOME, new ResourceLocation("aoemod", "warm_ocean"));
    @Unique private static final ResourceKey<Biome> AOE_JUNGLE =
            ResourceKey.create(Registries.BIOME, new ResourceLocation("aoemod", "jungle"));
    @Unique private static final ResourceKey<Biome> AOE_SAVANNA =
            ResourceKey.create(Registries.BIOME, new ResourceLocation("aoemod", "savanna"));
    @Unique private static final ResourceKey<Biome> AOE_PLAINS =
            ResourceKey.create(Registries.BIOME, new ResourceLocation("aoemod", "plains"));
    @Unique private static final ResourceKey<Biome> AOE_GROVE =
            ResourceKey.create(Registries.BIOME, new ResourceLocation("aoemod", "grove"));
    @Unique private static final ResourceKey<Biome> AOE_MUSHROOM_CAVES =
            ResourceKey.create(Registries.BIOME, new ResourceLocation("aoemod", "mushroom_caves"));
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

    @Unique
    private static double september2024Mod$positiveMod(double value, double modulus) {
        double result = value % modulus;
        if (result < 0.0D) {
            result += modulus;
        }
        return result;
    }

    @Inject(
            method = "getNoiseBiome(IIILnet/minecraft/world/level/biome/Climate$Sampler;)Lnet/minecraft/core/Holder;",
            at = @At("HEAD"),
            cancellable = true
    )
    private void aoe$forceJsonMatchingBiomes(
            int x,
            int y,
            int z,
            Climate.Sampler sampler,
            CallbackInfoReturnable<Holder<Biome>> cir
    ) {
        Map<ResourceKey<Biome>, Holder<Biome>> biomes = this.aoe$getBiomeCache();

        if (!biomes.containsKey(AOE_WARM_OCEAN)
                || !biomes.containsKey(AOE_JUNGLE)
                || !biomes.containsKey(AOE_SAVANNA)
                || !biomes.containsKey(AOE_PLAINS)
                || !biomes.containsKey(AOE_GROVE)
                || !biomes.containsKey(AOE_MUSHROOM_CAVES)
                || !biomes.containsKey(AOE_BADLANDS)
                || !biomes.containsKey(AOE_DEEP_DARK)
                || !biomes.containsKey(AOE_CRIMSON)
                || !biomes.containsKey(AOE_WARPED)
                || !biomes.containsKey(AOE_SOUL)
                || !biomes.containsKey(AOE_BASALT)) {
            return;
        }

        /*
         * getNoiseBiome receives quart coordinates.
         * Convert them back to approximate block coordinates so the mixin matches
         * the density/noise-router values used by the JSON.
         */
        int blockY = y * 4;
        int blockZ = z * 4;

        double cyclePos = (blockZ % 7680.0D)/640.0D;
        double continentalness = 0.6D - 0.1D * Math.abs(cyclePos - 6.0D);

        /*
         * Match this JSON climate parameter:
         *
         * "temperature": {
         *   "type": "minecraft:y_clamped_gradient",
         *   "from_value": -0.6,
         *   "from_y": -128,
         *   "to_value": 0.6,
         *   "to_y": 256
         * }
         */
        double climateTemperature = aoe$yClampedGradient(
                blockY,
                -128.0D,
                256.0D,
                -0.6D,
                0.6D
        );

        /*
         * Match the biome_source JSON continentalness ranges.
         */
        if (continentalness < 0.1D) {
            cir.setReturnValue(biomes.get(AOE_WARM_OCEAN));
            return;
        }

        if (continentalness < 0.2D) {
            cir.setReturnValue(biomes.get(AOE_JUNGLE));
            return;
        }

        if (continentalness < 0.3D) {
            cir.setReturnValue(biomes.get(AOE_SAVANNA));
            return;
        }

        if (continentalness < 0.4D) {
            cir.setReturnValue(biomes.get(AOE_PLAINS));
            return;
        }

        /*
         * Match:
         *
         * grove:
         * continentalness [0.4, 0.405]
         */
        if (continentalness < 0.42D) {
            cir.setReturnValue(biomes.get(AOE_GROVE));
            return;
        }

        /*
         * Match mountain interior:
         *
         * grove:
         * continentalness [0.42, 0.48]
         * temperature [-0.1, 0]
         *
         * mushroom_caves:
         * continentalness [0.42, 0.48]
         * temperature [0.05, 0.25]
         *
         * grove:
         * continentalness [0.42, 0.48]
         * temperature [0.25, 0.6]
         *
         * There is a small JSON gap from temperature 0.0 to 0.05.
         * I assign that gap to grove so the mountain surface does not accidentally
         * become mushroom_caves too early.
         */
        if (continentalness < 0.48D) {
            if (climateTemperature >= 0.05D && climateTemperature < 0.25D) {
                cir.setReturnValue(biomes.get(AOE_MUSHROOM_CAVES));
            } else {
                cir.setReturnValue(biomes.get(AOE_GROVE));
            }
            return;
        }

        /*
         * Match:
         *
         * grove:
         * continentalness [0.495, 0.5]
         */
        if (continentalness < 0.5D) {
            cir.setReturnValue(biomes.get(AOE_GROVE));
            return;
        }

        /*
         * Match:
         *
         * badlands:
         * continentalness [0.5, 0.505]
         */
        if (continentalness < 0.505D) {
            cir.setReturnValue(biomes.get(AOE_BADLANDS));
            return;
        }

        /*
         * Match badlands/nether vertical layering from the JSON:
         *
         * badlands:
         * temperature [-0.1, 0.6]
         *
         * deep_dark:
         * temperature [-0.2, -0.1]
         *
         * soul_sand_valley:
         * temperature [-0.3, -0.2]
         *
         * warped_forest:
         * temperature [-0.4, -0.3]
         *
         * crimson_forest:
         * temperature [-0.5, -0.4]
         *
         * basalt_deltas:
         * temperature [-0.6, -0.5]
         */
        if (climateTemperature < -0.5D) {
            cir.setReturnValue(biomes.get(AOE_BASALT));
        } else if (climateTemperature < -0.4D) {
            cir.setReturnValue(biomes.get(AOE_CRIMSON));
        } else if (climateTemperature < -0.3D) {
            cir.setReturnValue(biomes.get(AOE_WARPED));
        } else if (climateTemperature < -0.2D) {
            cir.setReturnValue(biomes.get(AOE_SOUL));
        } else if (climateTemperature < -0.1D) {
            cir.setReturnValue(biomes.get(AOE_DEEP_DARK));
        } else {
            cir.setReturnValue(biomes.get(AOE_BADLANDS));
        }
    }

    @Unique
    private static double aoe$yClampedGradient(
            double y,
            double fromY,
            double toY,
            double fromValue,
            double toValue
    ) {
        if (y <= fromY) {
            return fromValue;
        }

        if (y >= toY) {
            return toValue;
        }

        double t = (y - fromY) / (toY - fromY);
        return fromValue + t * (toValue - fromValue);
    }

    @Unique
    private Map<ResourceKey<Biome>, Holder<Biome>> aoe$getBiomeCache() {
        if (this.aoe$biomeCache == null) {
            Map<ResourceKey<Biome>, Holder<Biome>> cache = new HashMap<>();

            for (Holder<Biome> holder : ((BiomeSource) (Object) this).possibleBiomes()) {
                holder.unwrapKey().ifPresent(key -> cache.put(key, holder));
            }

            this.aoe$biomeCache = Map.copyOf(cache);
        }

        return this.aoe$biomeCache;
    }
}
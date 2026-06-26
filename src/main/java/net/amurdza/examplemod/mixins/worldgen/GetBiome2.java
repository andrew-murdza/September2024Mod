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

    /*
     * Must match your density function JSON:
     *
     * "biome_width": 64 means 64 chunks = 1024 blocks.
     * "z_0": 0 means the jungle/jungle wrap seam is centered at z = 0.
     */
    @Unique
    private static final int AOE_BIOME_WIDTH_BLOCKS = 64 * 16;

    @Unique
    private static final int AOE_Z_0 = 0;

    @Unique
    private Map<ResourceKey<Biome>, Holder<Biome>> aoe$biomeCache;

    @Inject(
            method = "getNoiseBiome(IIILnet/minecraft/world/level/biome/Climate$Sampler;)Lnet/minecraft/core/Holder;",
            at = @At("HEAD"),
            cancellable = true
    )
    private void aoe$useBlockYForBiome(
            int x, int y, int z, Climate.Sampler sampler, CallbackInfoReturnable<Holder<Biome>> cir
    ) {
        Map<ResourceKey<Biome>, Holder<Biome>> biomes = this.aoe$getBiomeCache();

        if (!aoe$hasRequiredBiomes(biomes)) {
            return;
        }

        /*
         * getNoiseBiome receives quart coordinates.
         * Convert Y and Z back to block coordinates.
         */
        int blockY = y * 4;
        int blockZ = z * 4;

        int periodBlocks = AOE_BIOME_WIDTH_BLOCKS * 10;
        int shiftedZ = blockZ - AOE_Z_0;
        int wrappedZ = Math.floorMod(shiftedZ, periodBlocks);

        int band = wrappedZ / AOE_BIOME_WIDTH_BLOCKS;

        /*
         * Band order:
         *
         * 0: jungle
         * 1: savanna
         * 2: plains
         * 3: grove / mushroom caves
         * 4: badlands / nether layers
         * 5: badlands / nether layers
         * 6: grove / mushroom caves
         * 7: plains
         * 8: savanna
         * 9: jungle
         *
         * Because bands 9 and 0 touch across the wrap, jungle is 2 bands wide.
         * Because bands 4 and 5 touch in the middle, badlands is 2 bands wide.
         */
        switch (band) {
            case 0, 9 -> cir.setReturnValue(biomes.get(AOE_JUNGLE));

            case 1, 8 -> cir.setReturnValue(biomes.get(AOE_SAVANNA));

            case 2, 7 -> cir.setReturnValue(biomes.get(AOE_PLAINS));

            case 3, 6 -> {
                if (blockY <= 39) {
                    cir.setReturnValue(biomes.get(AOE_MUSHROOM_CAVES));
                } else {
                    cir.setReturnValue(biomes.get(AOE_GROVE));
                }
            }

            case 4, 5 -> cir.setReturnValue(aoe$getBadlandsLayerBiome(biomes, blockY));
        }
    }

    @Unique
    private static Holder<Biome> aoe$getBadlandsLayerBiome(
            Map<ResourceKey<Biome>, Holder<Biome>> biomes,
            int blockY
    ) {
        if (blockY < -40) {
            return biomes.get(AOE_BASALT);
        } else if (blockY < -16) {
            return biomes.get(AOE_CRIMSON);
        } else if (blockY < 8) {
            return biomes.get(AOE_WARPED);
        } else if (blockY < 32) {
            return biomes.get(AOE_SOUL);
        } else if (blockY < 56) {
            return biomes.get(AOE_DEEP_DARK);
        } else {
            return biomes.get(AOE_BADLANDS);
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
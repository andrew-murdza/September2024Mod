package net.amurdza.examplemod.worldgen.surface_rule;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.amurdza.examplemod.mixins.accessor.SurfaceRulesContextAccessor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.core.registries.Registries;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.SurfaceRules;

import java.util.function.Function;

public record BiomeAboveConditionSource(
        HolderSet<Biome> biomeIs
) implements SurfaceRules.ConditionSource {

    public static final Codec<BiomeAboveConditionSource> DIRECT_CODEC =
            RecordCodecBuilder.create(inst -> inst.group(
                    RegistryCodecs.homogeneousList(Registries.BIOME)
                            .fieldOf("biome_is")
                            .forGetter(BiomeAboveConditionSource::biomeIs)
            ).apply(inst, BiomeAboveConditionSource::new));

    public static final KeyDispatchDataCodec<BiomeAboveConditionSource> CODEC =
            KeyDispatchDataCodec.of(DIRECT_CODEC);

    @Override
    public SurfaceRules.Condition apply(SurfaceRules.Context context) {
        return new SurfaceRules.LazyYCondition(context) {
            @Override
            protected boolean compute() {
                SurfaceRulesContextAccessor accessor = (SurfaceRulesContextAccessor) (Object) context;

                Function<BlockPos, Holder<Biome>> biomeGetter = accessor.aoemod$getBiomeGetter();

                BlockPos abovePos = new BlockPos(
                        accessor.aoemod$getBlockX(),
                        accessor.aoemod$getBlockY() + 1,
                        accessor.aoemod$getBlockZ()
                );

                Holder<Biome> biomeAbove = biomeGetter.apply(abovePos);

                return biomeIs.contains(biomeAbove);
            }
        };
    }

    @Override
    public KeyDispatchDataCodec<? extends SurfaceRules.ConditionSource> codec() {
        return CODEC;
    }
}
package net.amurdza.examplemod.worldgen;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Climate;

import java.util.List;
import java.util.stream.Stream;

/** Absolute underground bands and surface habitats aligned with the terrain's drainage fields. */
public final class DimensionBiomeSource extends BiomeSource {
    public static final Codec<DimensionBiomeSource> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("dimension").forGetter(source -> source.dimension),
            Biome.CODEC.listOf().fieldOf("biomes").forGetter(source -> source.biomes)
    ).apply(instance, DimensionBiomeSource::new));
    private final String dimension;
    private final List<Holder<Biome>> biomes;

    public DimensionBiomeSource(String dimension, List<Holder<Biome>> biomes) {
        this.dimension = dimension;
        this.biomes = List.copyOf(biomes);
        int expected = switch (dimension) { case "tropical", "nether" -> 4; case "snowy", "desert" -> 2; default -> 1; };
        if (biomes.size() != expected) throw new IllegalArgumentException(dimension + " requires " + expected + " biomes");
    }

    @Override protected Codec<? extends BiomeSource> codec() { return CODEC; }
    @Override protected Stream<Holder<Biome>> collectPossibleBiomes() { return biomes.stream(); }

    @Override
    public Holder<Biome> getNoiseBiome(int x, int y, int z, Climate.Sampler sampler) {
        int blockY = y * 4;
        int index = switch (dimension) {
            case "nether" -> Math.max(0, Math.min(3, Math.floorDiv(blockY, 32)));
            case "snowy" -> blockY >= -32 && blockY < 0 ? 1 : 0;
            case "desert" -> blockY < 0 ? 1 : 0;
            case "tropical" -> Math.max(0, Math.min(3, (int) Math.round(
                    Climate.unquantizeCoord(sampler.sample(x, 0, z).continentalness()))));
            default -> 0;
        };
        return biomes.get(index);
    }
}

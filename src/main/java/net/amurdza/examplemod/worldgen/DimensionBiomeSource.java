package net.amurdza.examplemod.worldgen;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Climate;

import java.util.List;
import java.util.ArrayList;
import java.util.stream.Stream;

/** Absolute underground bands and surface habitats aligned with the terrain's drainage fields. */
public final class DimensionBiomeSource extends BiomeSource {
    public static final Codec<DimensionBiomeSource> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("dimension").forGetter(source -> source.dimension),
            Biome.CODEC.listOf().fieldOf("biomes").forGetter(source -> source.biomes),
            RegistryOps.<Biome, DimensionBiomeSource>retrieveGetter(Registries.BIOME)
    ).apply(instance, DimensionBiomeSource::new));
    private final String dimension;
    private final List<Holder<Biome>> biomes;

    public DimensionBiomeSource(String dimension, List<Holder<Biome>> biomes) {
        this.dimension = dimension;
        this.biomes = List.copyOf(biomes);
        int expected = switch (dimension) { case "tropical", "nether", "snowy" -> 4; case "desert" -> 3; case "plains", "savanna" -> 2; default -> 1; };
        if (biomes.size() != expected) {
            throw new IllegalArgumentException(dimension + " requires " + expected + " biomes");
        }
    }

    private DimensionBiomeSource(String dimension, List<Holder<Biome>> biomes, HolderGetter<Biome> registry) {
        this(dimension, migrateBiomes(dimension, biomes, registry));
    }

    private static List<Holder<Biome>> migrateBiomes(String dimension, List<Holder<Biome>> biomes, HolderGetter<Biome> registry) {
        int legacySize = switch (dimension) { case "plains", "savanna" -> 1; case "desert" -> 2; default -> -1; };
        if (biomes.size() == legacySize) {
            var migrated = new ArrayList<>(biomes);
            migrated.add(registry.getOrThrow(ResourceKey.create(Registries.BIOME,
                    new ResourceLocation("aoemod", dimension + "_lake"))));
            return migrated;
        }
        if (!dimension.equals("snowy") || biomes.size() < 2 || biomes.size() > 4) return biomes;
        // level.dat may contain the original forest/cave pair, the later ocean
        // triple, or an interim dappled triple. Keep their existing holders while
        // normalizing the new surface slots to ocean=2 and dappled=3.
        var legacyOcean = ResourceKey.create(Registries.BIOME, new ResourceLocation("aoemod", "mountain_lake"));
        var ocean = ResourceKey.create(Registries.BIOME, new ResourceLocation("aoemod", "cold_ocean"));
        var dappled = ResourceKey.create(Registries.BIOME, new ResourceLocation("aoemod", "dappled_forest"));
        if (biomes.size() >= 3 && !biomes.get(2).is(ocean) && !biomes.get(2).is(legacyOcean) && !biomes.get(2).is(dappled)) {
            throw new IllegalArgumentException("Unknown third biome in saved snowy generator");
        }
        var migrated = new ArrayList<>(biomes.subList(0, 2));
        migrated.add(biomes.stream().filter(holder -> holder.is(ocean)).findFirst().orElseGet(() -> registry.getOrThrow(ocean)));
        migrated.add(biomes.stream().filter(holder -> holder.is(dappled)).findFirst().orElseGet(() -> registry.getOrThrow(dappled)));
        return migrated;
    }

    @Override protected Codec<? extends BiomeSource> codec() { return CODEC; }
    @Override protected Stream<Holder<Biome>> collectPossibleBiomes() {
        return biomes.stream();
    }

    @Override
    public Holder<Biome> getNoiseBiome(int x, int y, int z, Climate.Sampler sampler) {
        int blockY = y * 4;
        int index = switch (dimension) {
            case "nether" -> Math.max(0, Math.min(3, Math.floorDiv(blockY, 32)));
            case "snowy" -> blockY >= -32 && blockY < 0 ? 1 : snowySurfaceIndex(x, z, sampler);
            case "desert" -> blockY < 0 ? 1 : surfaceIndex(x, z, sampler, 2);
            case "savanna" -> surfaceIndex(x, z, sampler, 1);
            // Plains retains the vanilla terrain climate inputs; its unused
            // humidity channel carries the actual sea-surface density test.
            case "plains" -> sampler.sample(x, 0, z).humidity() > 5000 ? 1 : 0;
            case "tropical" -> Math.max(0, Math.min(3, (int) Math.round(
                    Climate.unquantizeCoord(sampler.sample(x, 0, z).continentalness()))));
            default -> 0;
        };
        return biomes.get(index);
    }

    private static int surfaceIndex(int x, int z, Climate.Sampler sampler, int maximum) {
        return Math.max(0, Math.min(maximum, (int) Math.round(
                Climate.unquantizeCoord(sampler.sample(x, 0, z).continentalness()))));
    }

    private static int snowySurfaceIndex(int x, int z, Climate.Sampler sampler) {
        long weirdness = Math.abs(sampler.sample(x, 0, z).weirdness());
        // Compare the quantized shoreline before converting to float: 0.3214f
        // rounds slightly below the exact contour and would add a wet column.
        return weirdness < MountainLakeOffset.LAKE_QUANTIZED_WEIRDNESS ? 2
                : SnowyTerrainProfile.height(Climate.unquantizeCoord(weirdness)) > 70.5 ? 0 : 3;
    }

    public static int snowySurfaceIndex(double weirdness) {
        double contour = Math.abs(weirdness);
        return contour < MountainLakeOffset.LAKE_WEIRDNESS ? 2
                : SnowyTerrainProfile.height(contour) > 70.5 ? 0 : 3;
    }
}

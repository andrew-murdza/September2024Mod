package net.amurdza.examplemod.worldgen;

import com.mojang.serialization.Dynamic;
import net.minecraft.nbt.CompoundTag;

/** Load-time compatibility for the two vanilla dimensions saved by the retired BCLib. */
public final class RetiredBclibWorldgen {
    private RetiredBclibWorldgen() { }

    @SuppressWarnings("unchecked")
    public static <T> Dynamic<T> migrate(Dynamic<T> settings) {
        // Loading works on a separate tree. No level.dat or caller-owned tag is
        // edited; Minecraft persists the decoded result on its ordinary next save.
        Dynamic<T> result = settings.getValue() instanceof CompoundTag tag
                ? new Dynamic<>(settings.getOps(), (T) tag.copy()) : settings;
        var dimensions = result.get("dimensions").result();
        if (dimensions.isEmpty()) return result;
        Dynamic<T> repaired = migrateDimension(dimensions.get(), "minecraft:the_nether", false);
        repaired = migrateDimension(repaired, "minecraft:the_end", true);
        return result.set("dimensions", repaired);
    }

    private static <T> Dynamic<T> migrateDimension(Dynamic<T> dimensions, String name, boolean end) {
        var dimension = dimensions.get(name).result();
        if (dimension.isEmpty()) return dimensions;
        var savedGenerator = dimension.get().get("generator").result();
        if (savedGenerator.isEmpty()) return dimensions;
        Dynamic<T> generator = savedGenerator.get();
        if (generator.get("type").asString("").equals("bclib:betterx")) {
            // BCLChunkGenerator extends vanilla NoiseBasedChunkGenerator and its
            // codec has the same biome_source/settings fields. Keep settings and
            // all other fields, including seeds, exactly as saved.
            generator = generator.set("type", generator.createString("minecraft:noise"));
        }
        var source = generator.get("biome_source").result();
        if (source.isPresent()) generator = generator.set("biome_source", migrateSource(source.get(), end));
        return dimensions.set(name, dimension.get().set("generator", generator));
    }

    private static <T> Dynamic<T> migrateSource(Dynamic<T> source, boolean end) {
        String type = source.get("type").asString("");
        if (type.equals(end ? "bclib:end_biome_source" : "bclib:nether_biome_source")) {
            Dynamic<T> repaired = source.set("type", source.createString(end ? "minecraft:the_end" : "minecraft:multi_noise"));
            return end ? repaired : repaired.set("preset", repaired.createString("minecraft:nether"));
        }
        // Blueprint's codec delegates original_biome_source to BiomeSource.CODEC.
        // Preserve the valid wrapper and only repair its removed nested source.
        if (type.equals("blueprint:modded")) {
            var original = source.get("original_biome_source").result();
            if (original.isPresent()) return source.set("original_biome_source", migrateSource(original.get(), end));
        }
        return source;
    }
}

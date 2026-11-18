package net.amurdza.examplemod.gametest;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.util.GameplayDimensions;
import net.amurdza.examplemod.worldgen.DimensionBiomeSource;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(AOEMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class DimensionBiomeMigrationTests {
    @GameTest(template = "plant_gameplay_empty")
    public static void legacySnowyGeneratorsGainOceanAndDappledWithoutReorderingSavedHabitats(GameTestHelper helper) {
        var level = helper.getLevel().getServer().getLevel(GameplayDimensions.SNOWY);
        var ops = RegistryOps.create(JsonOps.INSTANCE, level.registryAccess());
        var generator = (NoiseBasedChunkGenerator) level.getChunkSource().getGenerator();
        var random = RandomState.create(generator.generatorSettings().value(),
                level.registryAccess().lookupOrThrow(Registries.NOISE), 135792468L);
        String[] canonical = {"aoemod:snowy_forest", "aoemod:mushroom_caves", "aoemod:cold_ocean", "aoemod:dappled_forest"};
        for (String previousThird : new String[]{"", ",\"aoemod:cold_ocean\"", ",\"aoemod:mountain_lake\"", ",\"aoemod:dappled_forest\"",
                ",\"aoemod:cold_ocean\",\"aoemod:dappled_forest\"", ",\"aoemod:mountain_lake\",\"aoemod:dappled_forest\""}) {
            var legacy = JsonParser.parseString("{\"dimension\":\"snowy\",\"biomes\":[\"aoemod:snowy_forest\","
                    + "\"aoemod:mushroom_caves\"" + previousThird + "]}");
            var migrated = DimensionBiomeSource.CODEC.parse(ops, legacy).getOrThrow(false,
                    error -> { throw new IllegalStateException(error); });
            var encoded = DimensionBiomeSource.CODEC.encodeStart(ops, migrated).getOrThrow(false,
                    error -> { throw new IllegalStateException(error); }).getAsJsonObject();
            helper.assertTrue(encoded.getAsJsonArray("biomes").size() == 4,
                    "Legacy save must persist its upgraded four-biome generator");
            for (int index = 0; index < canonical.length; index++) {
                helper.assertTrue(encoded.getAsJsonArray("biomes").get(index).getAsString().equals(canonical[index]),
                        "Migrated biome slot " + index + " must be " + canonical[index]);
            }
            var roundTrip = DimensionBiomeSource.CODEC.parse(ops, encoded).getOrThrow(false,
                    error -> { throw new IllegalStateException(error); });
            helper.assertTrue(roundTrip.possibleBiomes().size() == 4, "Repeated save/load must not append duplicate biomes");
            var found = new java.util.HashSet<String>();
            for (int x = -16; x <= 16; x++) for (int z = -16; z <= 16; z++) {
                int quartX = x * 32, quartZ = z * 32;
                var actual = roundTrip.getNoiseBiome(quartX, 40, quartZ, random.sampler());
                var expected = generator.getBiomeSource().getNoiseBiome(quartX, 40, quartZ, random.sampler());
                helper.assertTrue(actual.equals(expected), "Migrated and new worlds must select identical surface habitats");
                found.add(actual.unwrapKey().orElseThrow().location().getPath());
                helper.assertTrue(roundTrip.getNoiseBiome(quartX, -4, quartZ, random.sampler())
                                .is(ResourceKey.create(Registries.BIOME, new ResourceLocation("aoemod", "mushroom_caves"))),
                        "Migration must preserve the underground cave band");
                helper.assertTrue(roundTrip.getNoiseBiome(quartX, -9, quartZ, random.sampler())
                                .is(ResourceKey.create(Registries.BIOME, new ResourceLocation("aoemod", "snowy_forest"))),
                        "Migration must preserve the deeper underground biome");
            }
            helper.assertTrue(found.containsAll(java.util.Set.of("cold_ocean", "dappled_forest", "snowy_forest")),
                    "Migration must exercise ocean, dappled lowlands and snowy uplands");
        }
        helper.succeed();
    }
}

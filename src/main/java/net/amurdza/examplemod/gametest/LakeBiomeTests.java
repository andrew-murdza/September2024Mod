package net.amurdza.examplemod.gametest;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.event_handlers.DocumentedNaturalSpawns;
import net.amurdza.examplemod.util.GameplayDimensions;
import net.amurdza.examplemod.worldgen.DimensionBiomeSource;
import net.amurdza.examplemod.worldgen.DocumentedFauna;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraftforge.event.entity.living.MobSpawnEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.registries.ForgeRegistries;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

@GameTestHolder(AOEMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class LakeBiomeTests {
    @GameTest(template = "plant_gameplay_empty")
    public static void dryBiomesHaveNoWaterSpawnEntriesAndLakeOctopusesUseCreatureBudget(GameTestHelper helper) {
        var registry = helper.getLevel().registryAccess().registryOrThrow(Registries.BIOME);
        for (String biome : List.of("plains", "savanna", "desert", "mesa", "rainforest", "cherry_grove")) {
            var loaded = registry.get(AOEMod.makeID(biome));
            helper.assertTrue(DocumentedFauna.forBiome(biome).stream().noneMatch(s -> DocumentedFauna.isWaterSpecies(s.type())),
                    "Dry habitat still owns aquatic species: " + biome);
            helper.assertTrue(Arrays.stream(MobCategory.values()).flatMap(category -> loaded.getMobSettings().getMobs(category).unwrap().stream())
                            .noneMatch(entry -> DocumentedFauna.isWaterSpecies(entry.type)),
                    "Loaded dry habitat still contains aquatic spawns: " + biome);
        }
        for (String biome : List.of("plains_lake", "savanna_lake", "desert_lake", "mangrove_swamp", "coral_reef")) {
            helper.assertTrue(DocumentedFauna.forBiome(biome).stream().anyMatch(s -> DocumentedFauna.isWaterSpecies(s.type())),
                    "Water habitat lost its aquatic roster: " + biome);
        }
        for (String biome : List.of("plains_lake", "savanna_lake")) {
            var octopus = DocumentedFauna.forBiome(biome).stream().filter(s -> s.id().toString().equals("hybrid_aquatic:octopus")).findFirst().orElseThrow();
            helper.assertTrue(DocumentedFauna.spawnCategory(octopus) == MobCategory.WATER_CREATURE
                            && registry.get(AOEMod.makeID(biome)).getMobSettings().getMobs(MobCategory.WATER_CREATURE)
                            .unwrap().stream().anyMatch(s -> s.type == octopus.type()),
                    "Lake octopuses must occupy the water-creature budget previously used by squid");
            helper.assertTrue(DocumentedFauna.forBiome(biome).stream().anyMatch(s -> s.type() == EntityType.SALMON)
                            && DocumentedFauna.forBiome(biome).stream().noneMatch(s -> s.type() == EntityType.SQUID),
                    "Both lake rosters need salmon and must replace squid");
        }
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty", timeoutTicks = 400)
    public static void oldGeneratorsGainWaterBiomesAndNewSamplesFollowWaterColumns(GameTestHelper helper) {
        for (String dimension : List.of("plains", "savanna", "desert")) {
            var level = helper.getLevel().getServer().getLevel(switch (dimension) {
                case "plains" -> GameplayDimensions.PLAINS;
                case "savanna" -> GameplayDimensions.SAVANNA;
                default -> GameplayDimensions.DESERT;
            });
            var ops = RegistryOps.create(JsonOps.INSTANCE, level.registryAccess());
            String previous = "\"aoemod:" + dimension + "\"" + (dimension.equals("desert") ? ",\"aoemod:deep_dark\"" : "");
            var source = DimensionBiomeSource.CODEC.parse(ops, JsonParser.parseString(
                    "{\"dimension\":\"" + dimension + "\",\"biomes\":[" + previous + "]}")).result().orElseThrow();
            int expected = dimension.equals("desert") ? 3 : 2;
            helper.assertTrue(source.possibleBiomes().size() == expected, "Saved generator did not gain its lake holder");
            var encoded = DimensionBiomeSource.CODEC.encodeStart(ops, source).result().orElseThrow();
            helper.assertTrue(DimensionBiomeSource.CODEC.parse(ops, encoded).result().orElseThrow().possibleBiomes().size() == expected,
                    "Repeated migration must not duplicate lake biomes");
            var generator = (NoiseBasedChunkGenerator) level.getChunkSource().getGenerator();
            var state = RandomState.create(generator.generatorSettings().value(), level.registryAccess().lookupOrThrow(Registries.NOISE), 817231L);
            int wet = 0, dry = 0;
            for (int x = -8; x <= 8; x++) for (int z = -8; z <= 8; z++) {
                int blockX = x * 128, blockZ = z * 128;
                var biome = source.getNoiseBiome(blockX / 4, 24, blockZ / 4, state.sampler());
                boolean lake = biome.unwrapKey().orElseThrow().location().getPath().equals(dimension + "_lake");
                var column = generator.getBaseColumn(blockX, blockZ, level, state);
                boolean water = column.getBlock(generator.getSeaLevel() - 1).is(Blocks.WATER);
                for (int y = generator.getSeaLevel(); water && y < level.getMaxBuildHeight(); y++) {
                    if (column.getBlock(y).blocksMotion()) water = false;
                }
                if (lake != water && dimension.equals("plains")) {
                    var settings = generator.generatorSettings().value();
                    var disabled = new net.minecraft.world.level.levelgen.NoiseGeneratorSettings(settings.noiseSettings(),
                            settings.defaultBlock(), settings.defaultFluid(), settings.noiseRouter(), settings.surfaceRule(),
                            settings.spawnTarget(), settings.seaLevel(), settings.disableMobGeneration(), false,
                            settings.oreVeinsEnabled(), settings.useLegacyRandomSource());
                    var withoutAquifers = new NoiseBasedChunkGenerator(generator.getBiomeSource(), net.minecraft.core.Holder.direct(disabled));
                    var dryColumn = withoutAquifers.getBaseColumn(blockX, blockZ, level, state);
                    AOEMod.LOGGER.info("Lake diagnostic {},{}: seaTop with aquifers={}, without aquifers={}",
                            blockX, blockZ, column.getBlock(generator.getSeaLevel() - 1), dryColumn.getBlock(generator.getSeaLevel() - 1));
                }
                helper.assertTrue(lake == water, "Lake classification must match terrain surface water at " + dimension + " " + blockX + "," + blockZ
                        + "; lake=" + lake + ", water=" + water + ", seaTop=" + column.getBlock(generator.getSeaLevel() - 1)
                        + ", humidity=" + state.sampler().sample(blockX / 4, 0, blockZ / 4).humidity());
                if (lake) wet++; else dry++;
                if (dimension.equals("desert")) helper.assertTrue(source.getNoiseBiome(blockX / 4, -2, blockZ / 4, state.sampler())
                                .unwrapKey().orElseThrow().location().getPath().equals("deep_dark"),
                        "New water biome must preserve the Deep Dark band");
            }
            helper.assertTrue(wet > 0 && dry > 0, "Samples must include water and land: " + dimension);
        }
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void surfaceClassifierKeepsIndependentWorldSeeds(GameTestHelper helper) {
        var level = helper.getLevel().getServer().getLevel(GameplayDimensions.PLAINS);
        var generator = (NoiseBasedChunkGenerator) level.getChunkSource().getGenerator();
        var settings = generator.generatorSettings().value();
        var noises = level.registryAccess().lookupOrThrow(Registries.NOISE);
        var first = RandomState.create(settings, noises, 817231L);
        var second = RandomState.create(settings, noises, 728193L);
        for (int x : new int[]{-128, 0, 128}) for (int z : new int[]{-128, 0, 128}) {
            long before = first.sampler().sample(x / 4, 0, z / 4).humidity();
            second.sampler().sample(x / 4, 0, z / 4);
            helper.assertTrue(first.sampler().sample(x / 4, 0, z / 4).humidity() == before,
                    "Sampling a second world must not overwrite the first classifier's seed or cache");
            var point = new DensityFunction.SinglePointContext(x, 0, z);
            helper.assertTrue(first.router().vegetation().compute(point) == before / 10000.0,
                    "Router and climate-sampler classifiers must retain the same seeded terrain context");
        }
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void fourDeepDesertWaterAdmitsSharksButStillRejectsCollision(GameTestHelper helper) {
        var level = helper.getLevel().getServer().getLevel(GameplayDimensions.DESERT);
        var type = ForgeRegistries.ENTITY_TYPES.getValue(new ResourceLocation("hybrid_aquatic:hound_shark"));
        var base = AquaticPlantGenerationTests.fixture(helper, "desert_lake", new HashMap<>(), 4, level,
                Blocks.SAND.defaultBlockState(), Blocks.WATER.defaultBlockState());
        boolean[] collision = {false};
        var water = (WorldGenLevel) Proxy.newProxyInstance(LakeBiomeTests.class.getClassLoader(), new Class<?>[]{WorldGenLevel.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("noCollision")) return !collision[0];
                    try { return method.invoke(base, args); }
                    catch (InvocationTargetException failure) { throw failure.getCause(); }
                });
        Mob shark = (Mob) type.create(level);
        shark.setPos(.5, 102, .5);
        try {
            var species = DocumentedFauna.find(water, shark.blockPosition(), type);
            helper.assertTrue(species != null && DocumentedFauna.hasSpace(water, shark.blockPosition(), species),
                    "Four-deep desert water must admit a swimming shark");
            var allowed = new MobSpawnEvent.PositionCheck(shark, water, MobSpawnType.CHUNK_GENERATION, null);
            DocumentedNaturalSpawns.position(allowed);
            helper.assertTrue(allowed.getResult() == Event.Result.ALLOW, "Unobstructed maximum-size shark must pass");
            collision[0] = true;
            var blocked = new MobSpawnEvent.PositionCheck(shark, water, MobSpawnType.CHUNK_GENERATION, null);
            DocumentedNaturalSpawns.position(blocked);
            helper.assertTrue(blocked.getResult() == Event.Result.DENY, "Shark's actual body collision must still be enforced");
        } finally { shark.discard(); }
        helper.succeed();
    }
}

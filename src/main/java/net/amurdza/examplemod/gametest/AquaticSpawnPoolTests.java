package net.amurdza.examplemod.gametest;

import com.github.alexthe666.alexsmobs.entity.EntityFlyingFish;
import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.event_handlers.DocumentedNaturalSpawns;
import net.amurdza.examplemod.util.GameplayDimensions;
import net.amurdza.examplemod.util.RetainedContent;
import net.amurdza.examplemod.worldgen.DocumentedFauna;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BiomeTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.level.LocalMobCapCalculator;
import net.minecraft.world.level.NaturalSpawner;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.ChunkAccess;
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
import java.util.Set;
import java.util.stream.Collectors;

@GameTestHolder(AOEMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class AquaticSpawnPoolTests {
    @GameTest(template = "plant_gameplay_empty")
    public static void aquaticBiomesSeparateSmallFishFromLowerCapWaterCreatures(GameTestHelper helper) {
        for (String name : List.of("snowy_forest", "dappled_forest", "mushroom_caves", "cold_ocean", "mountain_lake",
                "coral_reef", "rainforest", "cherry_grove", "mangrove_swamp", "plains", "savanna", "desert", "mesa", "deep_dark", "plains_lake", "savanna_lake", "desert_lake")) {
            var holder = helper.getLevel().registryAccess().registryOrThrow(Registries.BIOME).getHolderOrThrow(
                    ResourceKey.create(Registries.BIOME, new ResourceLocation("aoemod", name)));
            var biome = holder.value();
            if (!biome.getMobSettings().getMobs(MobCategory.WATER_AMBIENT).isEmpty()) {
                helper.assertFalse(holder.is(BiomeTags.REDUCED_WATER_AMBIENT_SPAWNS),
                        "Custom small-fish habitats must not receive vanilla's 98-percent ambient-spawn rejection: " + name);
            }
            for (var species : DocumentedFauna.forBiome(name)) {
                helper.assertTrue(species.type() != null && RetainedContent.isKeptEntity(species.id()), "Missing or filtered species " + species.id());
                if (!DocumentedFauna.isWaterSpecies(species.type())) continue;
                var entries = Arrays.stream(MobCategory.values()).flatMap(category -> biome.getMobSettings().getMobs(category).unwrap().stream())
                        .filter(entry -> entry.type == species.type()).toList();
                boolean large = species.habitat() == DocumentedFauna.Habitat.LARGE_WATER;
                var category = large ? MobCategory.WATER_CREATURE : MobCategory.WATER_AMBIENT;
                if (species.depth() == DocumentedFauna.Depth.DEEP || species.depth() == DocumentedFauna.Depth.BELOW_Y39) {
                    var regular = category;
                    category = large ? net.amurdza.examplemod.registry.ModMobCategories.DEEP_WATER_CREATURE
                            : net.amurdza.examplemod.registry.ModMobCategories.DEEP_WATER_AMBIENT;
                    helper.assertTrue(category.getMaxInstancesPerChunk() == regular.getMaxInstancesPerChunk(),
                            "The independent deep-water category must have the same capacity as its shallow counterpart");
                }
                if (net.amurdza.examplemod.registry.ModMobCategories.isTropicalFish(DocumentedFauna.spawnCategory(species))) {
                    category = DocumentedFauna.spawnCategory(species);
                }
                var expected = switch (species.id().toString()) {
                    case "minecraft:tropical_fish", "hybrid_aquatic:starfish" -> DocumentedFauna.GroupSize.SCHOOL;
                    case "minecraft:turtle" -> DocumentedFauna.GroupSize.SINGLE;
                    case "alexsmobs:orca" -> DocumentedFauna.GroupSize.VERY_LARGE;
                    case "hybrid_aquatic:octopus" -> DocumentedFauna.GroupSize.LARGE;
                    case "hybrid_aquatic:umbrella_octopus" -> DocumentedFauna.GroupSize.MEDIUM;
                    default -> large ? DocumentedFauna.GroupSize.LARGE : DocumentedFauna.GroupSize.MEDIUM;
                };
                helper.assertTrue(entries.size() == 1 && entries.get(0).getWeight().asInt() == DocumentedFauna.spawnWeight(species)
                                && entries.get(0).minCount == expected.minimum && entries.get(0).maxCount == expected.maximum,
                        "Incorrect loaded weight/group for " + name + "/" + species.id());
                helper.assertTrue(DocumentedFauna.spawnCategory(species) == category
                                && biome.getMobSettings().getMobs(category).unwrap().contains(entries.get(0)),
                        "Large creatures and small fish must compete in separate native categories: " + species.id());
            }
        }
        for (String name : List.of("desert_lake", "deep_dark")) {
            Set<String> aquatic = DocumentedFauna.forBiome(name).stream().filter(s -> DocumentedFauna.isWaterSpecies(s.type()))
                    .map(s -> s.id().toString()).collect(Collectors.toSet());
            helper.assertTrue(aquatic.equals(Set.of("hybrid_aquatic:hound_shark", "finsandtails:golden_river_ray",
                    "ambientadditions:blue_spotted_stingray", "alexsmobs:triops", "hybrid_aquatic:fiddler_crab")), "Incorrect desert water roster " + aquatic);
        }
        for (var species : DocumentedFauna.forBiome("snowy_forest")) {
            if (!DocumentedFauna.isWaterSpecies(species.type())) continue;
            String id = species.id().toString();
            var expected = id.equals("alexsmobs:orca") ? DocumentedFauna.GroupSize.VERY_LARGE
                    : species.habitat() == DocumentedFauna.Habitat.LARGE_WATER
                    ? DocumentedFauna.GroupSize.LARGE : DocumentedFauna.GroupSize.MEDIUM;
            helper.assertTrue(DocumentedFauna.groupSize(species) == expected,
                    "Large snowy species must keep their smaller packs across the shallow/deep partition");
        }
        var reef = helper.getLevel().registryAccess().registryOrThrow(Registries.BIOME).get(new ResourceLocation("aoemod:coral_reef"));
        for (String id : List.of("minecraft:turtle", "minecraft:axolotl", "aoemod:nautilus", "minecraft:dolphin")) {
            var type = ForgeRegistries.ENTITY_TYPES.getValue(new ResourceLocation(id));
            helper.assertTrue(type.getCategory() == MobCategory.WATER_CREATURE
                            && reef.getMobSettings().getMobs(MobCategory.WATER_CREATURE).unwrap().stream()
                            .anyMatch(entry -> entry.type == type && entry.minCount == 1 && entry.maxCount == (type == EntityType.TURTLE ? 1 : 3))
                            && reef.getMobSettings().getMobs(MobCategory.WATER_AMBIENT).unwrap().stream()
                            .noneMatch(entry -> entry.type == type),
                    "The four requested water creatures must use 1-3 packs in the lower-cap pool: " + id);
        }
        for (String id : List.of("minecraft:pufferfish")) {
            var type = ForgeRegistries.ENTITY_TYPES.getValue(new ResourceLocation(id));
            helper.assertTrue(type.getCategory() == MobCategory.WATER_AMBIENT
                            && DocumentedFauna.spawnCategory(type) == net.amurdza.examplemod.registry.ModMobCategories.COMMON_AQUATIC
                            && reef.getMobSettings().getMobs(net.amurdza.examplemod.registry.ModMobCategories.COMMON_AQUATIC).unwrap().stream()
                            .anyMatch(entry -> entry.type == type && entry.getWeight().asInt() == 10
                                    && entry.minCount == 2 && entry.maxCount == 5)
                            && reef.getMobSettings().getMobs(MobCategory.WATER_CREATURE).unwrap().stream()
                            .noneMatch(entry -> entry.type == type),
                    "Pufferfish must use the expanded tropical fish pool and schools: " + id);
        }
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void bottomAnimalsSettleFromOrdinaryWaterCandidatesWithoutLosingCollisionChecks(GameTestHelper helper) {
        var tropical = helper.getLevel().getServer().getLevel(GameplayDimensions.TROPICAL);
        for (String id : List.of("hybrid_aquatic:starfish")) {
            var type = ForgeRegistries.ENTITY_TYPES.getValue(new ResourceLocation(id));
            var water = AquaticPlantGenerationTests.fixture(helper, "coral_reef", new HashMap<>(), 32, tropical,
                    Blocks.SAND.defaultBlockState(), Blocks.WATER.defaultBlockState());
            var species = DocumentedFauna.find(water, new BlockPos(0, 115, 0), type);
            helper.assertTrue(species != null, "Requested bottom species must survive retained-content filtering: " + id);
            for (int y : new int[]{101, 106, 115, 125}) {
                BlockPos candidate = new BlockPos(0, y, 0);
                helper.assertTrue(DocumentedFauna.groundedSpawnPos(water, candidate, species).equals(new BlockPos(0, 101, 0)),
                        "All water-column choices must settle to their seabed: " + id);
                helper.assertTrue(NaturalSpawner.isSpawnPositionOk(SpawnPlacements.getPlacementType(type), water, candidate, type)
                                && SpawnPlacements.checkSpawnRules(type, water, MobSpawnType.NATURAL, candidate, RandomSource.create(y)),
                        "Actual natural placement chain must admit the selected bottom animal: " + id);
            }
        }
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void bottomPositionCheckMovesOnlySafeNaturalCandidatesToTheSeafloor(GameTestHelper helper) {
        var tropical = helper.getLevel().getServer().getLevel(GameplayDimensions.TROPICAL);
        var player = helper.makeMockPlayer();
        final double x = 100000.5, z = 100000.5;
        for (String id : List.of("hybrid_aquatic:starfish")) {
            var type = ForgeRegistries.ENTITY_TYPES.getValue(new ResourceLocation(id));
            Mob mob = (Mob) type.create(tropical);
            var base = AquaticPlantGenerationTests.fixture(helper, "coral_reef", new HashMap<>(), 32, tropical,
                    Blocks.SAND.defaultBlockState(), Blocks.WATER.defaultBlockState());
            boolean[] obstructed = {false}, hasPlayer = {true};
            var checkedBounds = new java.util.concurrent.atomic.AtomicReference<net.minecraft.world.phys.AABB>();
            var water = (WorldGenLevel) Proxy.newProxyInstance(AquaticSpawnPoolTests.class.getClassLoader(),
                    new Class<?>[]{WorldGenLevel.class}, (proxy, method, args) -> {
                        if (method.getName().equals("getNearestPlayer")) return hasPlayer[0] ? player : null;
                        if (method.getName().equals("noCollision")) {
                            var bounds = (net.minecraft.world.phys.AABB) args[args.length - 1];
                            checkedBounds.set(bounds);
                            return !obstructed[0];
                        }
                        try { return method.invoke(base, args); }
                        catch (InvocationTargetException failure) { throw failure.getCause(); }
                    });
            try {
                player.setPos(x + 40, 101, z);
                mob.setPos(x, 130, z);
                var safe = new MobSpawnEvent.PositionCheck(mob, water, MobSpawnType.NATURAL, null);
                DocumentedNaturalSpawns.position(safe);
                helper.assertTrue(safe.getResult() == Event.Result.ALLOW && mob.getY() == 101
                                && checkedBounds.get() != null && checkedBounds.get().minY == 101,
                        "PositionCheck must test the final bounds and actually settle " + id);

                mob.setPos(x, 130, z);
                player.setPos(x, 101, z);
                helper.assertTrue(player.distanceToSqr(mob.position()) > 24 * 24,
                        "Original random water position must pass the old distance check");
                var tooClose = new MobSpawnEvent.PositionCheck(mob, water, MobSpawnType.NATURAL, null);
                DocumentedNaturalSpawns.position(tooClose);
                helper.assertTrue(tooClose.getResult() == Event.Result.DENY && mob.getY() == 130,
                        "Grounding cannot move a natural animal into the player's 24-block exclusion sphere");

                player.setPos(x + net.amurdza.examplemod.worldgen.AquaticSpawnDistanceRules.category(tropical, type).getDespawnDistance() + 20, 101, z);
                var tooFar = new MobSpawnEvent.PositionCheck(mob, water, MobSpawnType.NATURAL, null);
                DocumentedNaturalSpawns.position(tooFar);
                helper.assertTrue(tooFar.getResult() == Event.Result.DENY && mob.getY() == 130,
                        "Grounded position must still respect the native distance cap");

                player.setPos(x + 40, 101, z);
                obstructed[0] = true;
                var blocked = new MobSpawnEvent.PositionCheck(mob, water, MobSpawnType.NATURAL, null);
                DocumentedNaturalSpawns.position(blocked);
                helper.assertTrue(blocked.getResult() == Event.Result.DENY && mob.getY() == 130,
                        "Bottom relocation must not skip final-position collision rejection");

                obstructed[0] = false;
                hasPlayer[0] = false;
                var nobodyNearby = new MobSpawnEvent.PositionCheck(mob, water, MobSpawnType.NATURAL, null);
                DocumentedNaturalSpawns.position(nobodyNearby);
                helper.assertTrue(nobodyNearby.getResult() == Event.Result.DENY,
                        "Natural spawning still requires an eligible player");
                var generated = new MobSpawnEvent.PositionCheck(mob, water, MobSpawnType.CHUNK_GENERATION, null);
                DocumentedNaturalSpawns.position(generated);
                helper.assertTrue(generated.getResult() == Event.Result.ALLOW && mob.getY() == 101,
                        "Chunk generation does not inherit natural spawning's player requirement");
            } finally { mob.discard(); }
        }
        player.discard();
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void aquaticCensusAndImmediateSpawnCountersUseMatchingSeparateCaps(GameTestHelper helper) throws Exception {
        var tropical = helper.getLevel().getServer().getLevel(GameplayDimensions.TROPICAL);
        var chunk = helper.getLevel().getChunkAt(helper.absolutePos(BlockPos.ZERO));
        Mob cod = EntityType.COD.create(tropical);
        Mob squid = EntityType.SQUID.create(tropical);
        Mob starfish = (Mob) ForgeRegistries.ENTITY_TYPES.getValue(new ResourceLocation("hybrid_aquatic:starfish")).create(tropical);
        var player = new net.minecraftforge.common.util.FakePlayer(tropical,
                new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), "AquaticCensusTest"));
        player.setPos(40, 0, 0);
        tropical.players().add(player);
        try {
            var state = NaturalSpawner.createState(289, List.of(cod, squid, starfish), (key, consumer) -> consumer.accept(chunk),
                    new LocalMobCapCalculator(tropical.getChunkSource().chunkMap));
            helper.assertTrue(state.getMobCategoryCounts().getInt(MobCategory.WATER_AMBIENT) == 2
                            && state.getMobCategoryCounts().getInt(net.amurdza.examplemod.registry.ModMobCategories.COMMON_AQUATIC) == 0
                            && state.getMobCategoryCounts().getInt(MobCategory.WATER_CREATURE) == 1,
                    "Small fish and starfish must count separately from larger water creatures");
            var afterSpawn = Arrays.stream(NaturalSpawner.SpawnState.class.getDeclaredMethods())
                    .filter(method -> Arrays.equals(method.getParameterTypes(), new Class<?>[]{Mob.class, ChunkAccess.class})).findFirst().orElseThrow();
            afterSpawn.setAccessible(true);
            afterSpawn.invoke(state, squid, chunk);
            helper.assertTrue(state.getMobCategoryCounts().getInt(MobCategory.WATER_AMBIENT) == 2
                            && state.getMobCategoryCounts().getInt(net.amurdza.examplemod.registry.ModMobCategories.COMMON_AQUATIC) == 0
                            && state.getMobCategoryCounts().getInt(MobCategory.WATER_CREATURE) == 2,
                    "A successful squid spawn must charge its native cap immediately");
            helper.assertTrue(EntityType.SQUID.getCategory() == MobCategory.WATER_CREATURE,
                    "Scoped custom pools must preserve source entity categories elsewhere");
            var outside = EntityType.SQUID.create(helper.getLevel());
            helper.assertTrue(DocumentedFauna.spawnCategory(outside, outside.getType().getCategory()) == MobCategory.WATER_CREATURE,
                    "Other dimensions must retain native accounting");
            outside.discard();
        } finally {
            tropical.players().remove(player);
            player.discard(); cod.discard(); squid.discard(); starfish.discard();
        }
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void snowyFlyingFishUseOnlyBlueAndPreserveSavedManualVariants(GameTestHelper helper) {
        var snowy = helper.getLevel().getServer().getLevel(GameplayDimensions.SNOWY);
        var water = AquaticPlantGenerationTests.fixture(helper, "mountain_lake", new HashMap<>(), 32, snowy);
        var type = ForgeRegistries.ENTITY_TYPES.getValue(new ResourceLocation("alexsmobs:flying_fish"));
        for (var reason : new MobSpawnType[]{MobSpawnType.NATURAL, MobSpawnType.CHUNK_GENERATION}) {
            for (int seed = 0; seed < 32; seed++) {
                var fish = (EntityFlyingFish) type.create(snowy);
                fish.getRandom().setSeed(seed * 179L);
                fish.finalizeSpawn(water, snowy.getCurrentDifficultyAt(BlockPos.ZERO), reason, null, null);
                helper.assertTrue(fish.getVariant() == 0, "Natural snowy flying fish must use the blue texture");
                fish.setVariant(2);
                CompoundTag saved = new CompoundTag(); fish.addAdditionalSaveData(saved); fish.readAdditionalSaveData(saved);
                helper.assertTrue(fish.getVariant() == 2, "Loading a saved/manual variant must not recolor it");
                fish.discard();
            }
        }
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void mushroomCavesPermitMooshroomsOnDarkCaveFloors(GameTestHelper helper) {
        var snowy = helper.getLevel().getServer().getLevel(GameplayDimensions.SNOWY);
        var base = AquaticPlantGenerationTests.fixture(helper, "mushroom_caves", new HashMap<>(), 0, snowy,
                Blocks.MYCELIUM.defaultBlockState(), Blocks.WATER.defaultBlockState());
        var dark = (WorldGenLevel) Proxy.newProxyInstance(AquaticSpawnPoolTests.class.getClassLoader(), new Class<?>[]{WorldGenLevel.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("getRawBrightness") || method.getName().equals("getMaxLocalRawBrightness")) return 0;
                    if (method.getName().equals("canSeeSky")) return false;
                    try { return method.invoke(base, args); }
                    catch (InvocationTargetException error) { throw error.getCause(); }
                });
        BlockPos feet = new BlockPos(0, 101, 0);
        helper.assertTrue(dark.getRawBrightness(feet, 0) == 0, "Fixture must represent an unlit underground cave");
        helper.assertTrue(NaturalSpawner.isSpawnPositionOk(SpawnPlacements.Type.ON_GROUND, dark, feet, EntityType.MOOSHROOM)
                        && SpawnPlacements.checkSpawnRules(EntityType.MOOSHROOM, dark, MobSpawnType.NATURAL, feet, RandomSource.create(17)),
                "Neither darkness nor the custom cave floor policy may block mooshrooms");
        var biome = snowy.registryAccess().registryOrThrow(Registries.BIOME).get(new ResourceLocation("aoemod:mushroom_caves"));
        helper.assertTrue(biome.getMobSettings().getMobs(net.amurdza.examplemod.registry.ModMobCategories.MUSHROOM_CAVE)
                        .unwrap().stream().anyMatch(entry -> entry.type == EntityType.MOOSHROOM),
                "Cave mooshrooms must use their independent natural spawn pool");
        helper.succeed();
    }
}

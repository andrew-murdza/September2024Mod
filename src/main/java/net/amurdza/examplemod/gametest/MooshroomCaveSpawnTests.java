package net.amurdza.examplemod.gametest;

import com.mojang.authlib.GameProfile;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.event_handlers.DocumentedNaturalSpawns;
import net.amurdza.examplemod.registry.ModMobCategories;
import net.amurdza.examplemod.util.GameplayDimensions;
import net.amurdza.examplemod.worldgen.AquaticSpawnCensus;
import net.amurdza.examplemod.worldgen.DocumentedFauna;
import net.amurdza.examplemod.worldgen.MushroomCaveSpawns;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.entity.animal.MushroomCow;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LocalMobCapCalculator;
import net.minecraft.world.level.NaturalSpawner;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.entity.living.MobSpawnEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

@GameTestHolder(AOEMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class MooshroomCaveSpawnTests {
    @GameTest(template = "plant_gameplay_empty")
    public static void caveAttemptsRunTwentyTimesPerPassiveCycleWithLargerIndependentBudget(GameTestHelper helper) {
        int attempts = 0;
        for (long tick = 1; tick <= 400; tick++) {
            if (MushroomCaveSpawns.isAttemptTick(tick)) attempts++;
            helper.assertTrue(MushroomCaveSpawns.isAttemptTick(tick) == MushroomCaveSpawns.isAttemptTick(tick + 400),
                    "The cave cadence must remain stable across passive spawn cycles");
        }
        helper.assertTrue(attempts == 20 && MushroomCaveSpawns.ATTEMPT_INTERVAL_TICKS == 20,
                "Scarce cave floors need twenty attempts per old 400-tick passive cycle");
        helper.assertTrue(ModMobCategories.MUSHROOM_CAVE.getMaxInstancesPerChunk() == 40
                        && ModMobCategories.MUSHROOM_CAVE.isPersistent()
                        && ModMobCategories.MUSHROOM_CAVE != MobCategory.CREATURE,
                "Faster opportunities must preserve the separate forty-animal budget and passive persistence");
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void undergroundMyceliumSpawnsIgnoreDarknessButRejectOtherFloors(GameTestHelper helper) {
        var snowy = helper.getLevel().getServer().getLevel(GameplayDimensions.SNOWY);
        BlockPos feet = new BlockPos(0, -19, 0);
        for (var floor : new BlockState[]{Blocks.MYCELIUM.defaultBlockState(), Blocks.GRASS_BLOCK.defaultBlockState(),
                Blocks.STONE.defaultBlockState(), Blocks.RED_MUSHROOM_BLOCK.defaultBlockState()}) {
            var cave = cave(helper, floor, false);
            boolean expected = floor.is(Blocks.MYCELIUM);
            helper.assertTrue(cave.getRawBrightness(feet, 0) == 0 && !cave.canSeeSky(feet),
                    "Fixture must be a genuinely dark underground floor");
            helper.assertTrue(MushroomCow.checkMushroomSpawnRules(EntityType.MOOSHROOM, cave,
                            MobSpawnType.NATURAL, feet, RandomSource.create(19)) == expected,
                    "Native mooshroom rules must accept dark mycelium and reject other blocks");
            helper.assertTrue(NaturalSpawner.isSpawnPositionOk(SpawnPlacements.Type.ON_GROUND, cave, feet,
                            EntityType.MOOSHROOM) == expected
                            && SpawnPlacements.checkSpawnRules(EntityType.MOOSHROOM, cave, MobSpawnType.NATURAL,
                            feet, RandomSource.create(19)) == expected,
                    "Documented spawn overrides must preserve the mycelium requirement");
            var cow = EntityType.MOOSHROOM.create(snowy);
            cow.setPos(feet.getX() + 0.5D, feet.getY(), feet.getZ() + 0.5D);
            var position = new MobSpawnEvent.PositionCheck(cow, cave, MobSpawnType.NATURAL, null);
            DocumentedNaturalSpawns.position(position);
            helper.assertTrue(position.getResult() == (expected ? Event.Result.ALLOW : Event.Result.DENY),
                    "Final position checks must agree with placement on the dark cave floor");
            cow.discard();
        }
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void caveAttemptsFindExposedFloorsAndRejectBlockedOrWrongSubstrate(GameTestHelper helper) {
        var open = cave(helper, Blocks.MYCELIUM.defaultBlockState(), false);
        for (int seed = 0; seed < 64; seed++) {
            ChunkPos chunk = new ChunkPos(seed - 32, 7 - seed);
            BlockPos chosen = MushroomCaveSpawns.findSpawnPosition(open, chunk, RandomSource.create(seed));
            helper.assertTrue(chosen != null && chosen.getY() == -19 && new ChunkPos(chosen).equals(chunk),
                    "Each attempt must target real underground mycelium in its current chunk");
        }
        helper.assertTrue(MushroomCaveSpawns.findSpawnPosition(cave(helper, Blocks.STONE.defaultBlockState(), false),
                        new ChunkPos(0, 0), RandomSource.create(1)) == null,
                "No cave attempt may invent a spawn on stone");
        helper.assertTrue(MushroomCaveSpawns.findSpawnPosition(cave(helper, Blocks.MYCELIUM.defaultBlockState(), true),
                        new ChunkPos(0, 0), RandomSource.create(1)) == null,
                "One-block crawl spaces cannot receive mooshrooms");
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    @SuppressWarnings("unchecked")
    public static void caveCategoryHasIndependentNativeGlobalAndLocalCaps(GameTestHelper helper) throws Exception {
        MobCategory category = ModMobCategories.MUSHROOM_CAVE;
        helper.assertTrue(category != MobCategory.CREATURE && category.getMaxInstancesPerChunk() == 40
                        && category.isFriendly() && category.isPersistent(),
                "Mooshrooms need an independent livestock-sized budget with passive persistence");
        var categoryField = Arrays.stream(NaturalSpawner.class.getDeclaredFields())
                .filter(field -> field.getType() == MobCategory[].class).findFirst().orElseThrow();
        categoryField.setAccessible(true);
        helper.assertTrue(Arrays.asList((MobCategory[]) categoryField.get(null)).contains(category),
                "The live natural spawner must include the new enum in its cached category list");
        var snowy = helper.getLevel().getServer().getLevel(GameplayDimensions.SNOWY);
        var biome = snowy.registryAccess().registryOrThrow(Registries.BIOME).get(new ResourceLocation("aoemod:mushroom_caves"));
        helper.assertTrue(biome.getMobSettings().getMobs(category).unwrap().stream()
                        .anyMatch(entry -> entry.type == EntityType.MOOSHROOM && entry.minCount == 2 && entry.maxCount == 4)
                        && biome.getMobSettings().getMobs(MobCategory.CREATURE).unwrap().stream()
                        .noneMatch(entry -> entry.type == EntityType.MOOSHROOM),
                "Loaded cave biome tables must put mooshroom packs only in their separate pool");

        var chunk = helper.getLevel().getChunkAt(helper.absolutePos(BlockPos.ZERO));
        double x = chunk.getPos().getMiddleBlockX() + .5;
        double z = chunk.getPos().getMiddleBlockZ() + .5;
        var player = new FakePlayer(snowy, new GameProfile(UUID.randomUUID(), "MooshroomCaveCapTest"));
        player.setPos(x + 40, 0, z);
        snowy.players().add(player);
        var cow = EntityType.MOOSHROOM.create(snowy);
        cow.setPos(x, 0, z);
        List<Entity> residents = new ArrayList<>();
        residents.add(cow);
        for (int i = 0; i < 20; i++) {
            var surfaceCow = EntityType.COW.create(snowy);
            surfaceCow.setPos(x, 0, z);
            residents.add(surfaceCow);
        }
        try {
            var local = new LocalMobCapCalculator(snowy.getChunkSource().chunkMap);
            var nearbyField = Arrays.stream(LocalMobCapCalculator.class.getDeclaredFields())
                    .filter(field -> Long2ObjectMap.class.isAssignableFrom(field.getType())).findFirst().orElseThrow();
            nearbyField.setAccessible(true);
            ((Long2ObjectMap<List<ServerPlayer>>) nearbyField.get(local)).put(chunk.getPos().toLong(), List.of(player));
            var state = NaturalSpawner.createState(289, residents, (key, consumer) -> consumer.accept(chunk), local);
            helper.assertTrue(state.getMobCategoryCounts().getInt(MobCategory.CREATURE) == 20
                            && state.getMobCategoryCounts().getInt(category) == 1,
                    "Surface livestock must not consume the cave-mooshroom census budget");
            var canSpawn = Arrays.stream(NaturalSpawner.SpawnState.class.getDeclaredMethods())
                    .filter(method -> Arrays.equals(method.getParameterTypes(), new Class<?>[]{MobCategory.class, ChunkPos.class}))
                    .findFirst().orElseThrow();
            canSpawn.setAccessible(true);
            helper.assertTrue((boolean) canSpawn.invoke(state, category, chunk.getPos())
                            && !(boolean) canSpawn.invoke(state, MobCategory.CREATURE, chunk.getPos())
                            && local.canSpawn(category, chunk.getPos()) && !local.canSpawn(MobCategory.CREATURE, chunk.getPos()),
                    "Both native cap checks must allow cave mooshrooms when the livestock budget is full");
            var afterSpawn = Arrays.stream(NaturalSpawner.SpawnState.class.getDeclaredMethods())
                    .filter(method -> Arrays.equals(method.getParameterTypes(), new Class<?>[]{Mob.class, ChunkAccess.class}))
                    .findFirst().orElseThrow();
            afterSpawn.setAccessible(true);
            helper.assertTrue(new ChunkPos(cow.blockPosition()).equals(chunk.getPos()),
                    "Census and successful-spawn callbacks must charge the same actual fixture chunk");
            for (int i = 1; i < 40; i++) afterSpawn.invoke(state, cow, chunk);
            helper.assertTrue(state.getMobCategoryCounts().getInt(category) == 40
                            && state.getMobCategoryCounts().getInt(MobCategory.CREATURE) == 20
                            && !(boolean) canSpawn.invoke(state, category, chunk.getPos())
                            && !local.canSpawn(category, chunk.getPos()),
                    "Successful spawns must immediately fill and enforce their own global and local caps");
            player.setPos(x + 300, 0, z);
            helper.assertTrue(AquaticSpawnCensus.category(cow, MobCategory.CREATURE) == category,
                    "Distant non-despawning cave residents must still consume their budget instead of refilling it");
            for (int i = 1; i < 40; i++) {
                var additional = EntityType.MOOSHROOM.create(snowy);
                additional.setPos(x, 0, z);
                residents.add(additional);
            }
            var nextTick = NaturalSpawner.createState(289, residents, (key, consumer) -> consumer.accept(chunk), local);
            helper.assertTrue(nextTick.getMobCategoryCounts().getInt(category) == 40
                            && !(boolean) canSpawn.invoke(nextTick, category, chunk.getPos()),
                    "The next tick must retain the full mooshroom cap even when the player moves over 128 blocks away");
            var outside = EntityType.MOOSHROOM.create(helper.getLevel());
            helper.assertTrue(DocumentedFauna.spawnCategory(outside, MobCategory.CREATURE) == MobCategory.CREATURE
                            && EntityType.MOOSHROOM.getCategory() == MobCategory.CREATURE,
                    "Vanilla mushroom fields and their entity category must keep native behavior");
            outside.discard();
        } finally {
            snowy.players().remove(player);
            player.discard();
            residents.forEach(Entity::discard);
        }
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void naturalMooshroomsUseBothVariantsAndKeepSavedAndManualColors(GameTestHelper helper) {
        var snowy = helper.getLevel().getServer().getLevel(GameplayDimensions.SNOWY);
        int red = 0;
        int brown = 0;
        for (var reason : new MobSpawnType[]{MobSpawnType.NATURAL, MobSpawnType.CHUNK_GENERATION}) {
            for (int seed = 0; seed < 256; seed++) {
                var cow = EntityType.MOOSHROOM.create(snowy);
                cow.getRandom().setSeed(seed * 1000003L);
                var difficulty = snowy.getCurrentDifficultyAt(BlockPos.ZERO);
                DocumentedNaturalSpawns.finalizeSpawn(new MobSpawnEvent.FinalizeSpawn(cow, snowy, 0, -19, 0,
                        difficulty, reason, null, null, null));
                cow.finalizeSpawn(snowy, difficulty, reason, null, null);
                DocumentedNaturalSpawns.finishedSpawn(new net.minecraftforge.event.entity.EntityJoinLevelEvent(cow, snowy,
                        reason == MobSpawnType.CHUNK_GENERATION));
                if (cow.getVariant() == MushroomCow.MushroomType.RED) red++; else brown++;
                var variant = cow.getVariant();
                var saved = new net.minecraft.nbt.CompoundTag();
                cow.addAdditionalSaveData(saved);
                cow.readAdditionalSaveData(saved);
                DocumentedNaturalSpawns.finishedSpawn(new net.minecraftforge.event.entity.EntityJoinLevelEvent(cow, snowy, true));
                helper.assertTrue(cow.getVariant() == variant, "Reloading a natural mooshroom must preserve its selected variant");
                cow.discard();
            }
        }
        helper.assertTrue(red > 210 && red < 302 && brown == 512 - red,
                "Natural colors must use the equal-probability red/brown split: red=" + red + ", brown=" + brown);
        for (var variant : MushroomCow.MushroomType.values()) {
            var manual = EntityType.MOOSHROOM.create(snowy);
            manual.setVariant(variant);
            DocumentedNaturalSpawns.finishedSpawn(new net.minecraftforge.event.entity.EntityJoinLevelEvent(manual, snowy));
            helper.assertTrue(manual.getVariant() == variant, "Egg/command mooshrooms must retain their chosen color");
            manual.discard();
        }
        helper.succeed();
    }

    private static WorldGenLevel cave(GameTestHelper helper, BlockState floor, boolean blocked) {
        var snowy = helper.getLevel().getServer().getLevel(GameplayDimensions.SNOWY);
        var base = AquaticPlantGenerationTests.fixture(helper, "mushroom_caves", new HashMap<>(), 0, snowy);
        java.util.function.Function<BlockPos, BlockState> stateAt = pos -> pos.getY() == -20 ? floor
                : pos.getY() > -20 && pos.getY() < (blocked ? -18 : -14)
                ? Blocks.AIR.defaultBlockState() : Blocks.STONE.defaultBlockState();
        return (WorldGenLevel) Proxy.newProxyInstance(MooshroomCaveSpawnTests.class.getClassLoader(),
                new Class<?>[]{WorldGenLevel.class}, (proxy, method, args) -> {
                    switch (method.getName()) {
                        case "getRawBrightness", "getMaxLocalRawBrightness": return 0;
                        case "canSeeSky", "canSeeSkyFromBelowWater": return false;
                        case "getBlockState": return stateAt.apply((BlockPos) args[0]);
                        case "getFluidState": return stateAt.apply((BlockPos) args[0]).getFluidState();
                        case "isEmptyBlock": return stateAt.apply((BlockPos) args[0]).isAir();
                        case "noCollision": return !blocked;
                        default:
                            try { return method.invoke(base, args); }
                            catch (InvocationTargetException failure) { throw failure.getCause(); }
                    }
                });
    }
}

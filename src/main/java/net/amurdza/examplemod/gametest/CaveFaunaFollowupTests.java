package net.amurdza.examplemod.gametest;

import com.mojang.authlib.GameProfile;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.event_handlers.DocumentedNaturalSpawns;
import net.amurdza.examplemod.util.GameplayDimensions;
import net.amurdza.examplemod.worldgen.DocumentedCaveSpawns;
import net.amurdza.examplemod.worldgen.DocumentedFauna;
import net.amurdza.examplemod.worldgen.DocumentedMonsters;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LocalMobCapCalculator;
import net.minecraft.world.level.NaturalSpawner;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.MobSpawnEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.registries.ForgeRegistries;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@GameTestHolder(AOEMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class CaveFaunaFollowupTests {
    private static final Method CANDIDATE_GATE = method(EntityType.class, BlockPos.class, ChunkAccess.class);
    private static final Method AFTER_SPAWN = method(Mob.class, ChunkAccess.class);

    @GameTest(template = "plant_gameplay_empty", timeoutTicks = 400)
    public static void gossamerWormPassesTheActualDeepWaterSpawnChain(GameTestHelper helper) throws Exception {
        ServerLevel snowy = helper.getLevel().getServer().getLevel(GameplayDimensions.SNOWY);
        BlockPos feet = new BlockPos(7448, 151, 7448);
        LevelChunk chunk = biomeChunk(snowy, feet, "cold_ocean");
        for (BlockPos pos : BlockPos.betweenClosed(feet.getX() - 8, 140, feet.getZ() - 8,
                feet.getX() + 7, 191, feet.getZ() + 7)) {
            chunk.setBlockState(pos, pos.getY() == 140 ? Blocks.STONE.defaultBlockState()
                    : pos.getY() <= 190 ? Blocks.WATER.defaultBlockState() : Blocks.AIR.defaultBlockState(), false);
        }
        var type = ForgeRegistries.ENTITY_TYPES.getValue(new ResourceLocation("alexscaves:gossamer_worm"));
        helper.assertTrue(type != null && DocumentedFauna.spawnCategory(type) == net.amurdza.examplemod.registry.ModMobCategories.DEEP_WATER_CREATURE,
                "Gossamer worms must use the independent large-water pool");
        helper.assertTrue(SpawnPlacements.checkSpawnRules(type, snowy, MobSpawnType.NATURAL, feet, snowy.random),
                "Connected deep water must override the source mod's absolute sea-level restriction");
        assertNativeSpawn(helper, snowy, chunk, feet, type, net.amurdza.examplemod.registry.ModMobCategories.DEEP_WATER_CREATURE, false);
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty", timeoutTicks = 600)
    public static void netherAndDeepDarkMobsSpawnFromRealFloorsWithNativeCaps(GameTestHelper helper) throws Exception {
        var server = helper.getLevel().getServer();
        var oldDifficulty = server.getWorldData().getDifficulty();
        server.setDifficulty(Difficulty.NORMAL, true);
        try {
            int index = 0;
            for (String biome : List.of("soul_sand_valley", "crimson_forest", "warped_forest", "basalt_deltas", "deep_dark")) {
                boolean deep = biome.equals("deep_dark");
                ServerLevel level = server.getLevel(deep ? GameplayDimensions.DESERT : GameplayDimensions.NETHER);
                BlockPos feet = new BlockPos(7608 + index++ * 64, deep ? level.getMinBuildHeight() + 16 : 51, 7608);
                LevelChunk chunk = biomeChunk(level, feet, biome);
                for (BlockPos pos : BlockPos.betweenClosed(feet.getX() - 8, feet.getY() - 1, feet.getZ() - 8,
                        feet.getX() + 7, feet.getY() + 15, feet.getZ() + 7)) {
                    chunk.setBlockState(pos, pos.getY() == feet.getY() - 1 || pos.getY() == feet.getY() + 15
                            ? Blocks.STONE.defaultBlockState() : Blocks.AIR.defaultBlockState(), false);
                }
                var names = new java.util.ArrayList<>(DocumentedMonsters.forBiome(biome));
                DocumentedFauna.forBiome(biome).stream().filter(species -> species.habitat() == DocumentedFauna.Habitat.LAND)
                        .forEach(species -> names.add(species.id().toString()));
                for (String name : names) {
                    var type = ForgeRegistries.ENTITY_TYPES.getValue(new ResourceLocation(name));
                    helper.assertTrue(type != null, "Requested cave mob must remain registered: " + name);
                    MobCategory category = DocumentedFauna.spawnCategory(type);
                    BlockPos floor = DocumentedCaveSpawns.findSpawnPosition(level, chunk, category, level.random);
                    helper.assertTrue(floor != null && DocumentedMonsters.hasSpace(level, floor),
                            "Actual cave floors must be available to " + name);
                    helper.assertTrue(SpawnPlacements.checkSpawnRules(type, level, MobSpawnType.NATURAL, feet, level.random),
                            "The native placement/light/biome gate must admit the requested room inhabitant: " + name);
                    assertNativeSpawn(helper, level, chunk, feet, type, category, true);
                }
            }
            helper.assertTrue(MobCategory.MONSTER.getMaxInstancesPerChunk() == 70
                            && MobCategory.CREATURE.getMaxInstancesPerChunk() == 10,
                    "Better cave targeting must preserve the existing hostile and livestock population budgets");
        } finally {
            server.setDifficulty(oldDifficulty, true);
        }
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void snowyLandAnimalsRejectSnowBlocksButAcceptSnowLayers(GameTestHelper helper) {
        ServerLevel snowy = helper.getLevel().getServer().getLevel(GameplayDimensions.SNOWY);
        BlockPos feet = new BlockPos(7448, 210, 7512);
        LevelChunk chunk = biomeChunk(snowy, feet, "snowy_forest");
        for (BlockPos pos : BlockPos.betweenClosed(feet.offset(-3, -2, -3), feet.offset(3, 5, 3))) {
            chunk.setBlockState(pos, pos.getY() < feet.getY() ? Blocks.STONE.defaultBlockState() : Blocks.AIR.defaultBlockState(), false);
        }
        for (var species : DocumentedFauna.forBiome("snowy_forest")) {
            if (species.habitat() != DocumentedFauna.Habitat.LAND) continue;
            chunk.setBlockState(feet.below(), Blocks.SNOW_BLOCK.defaultBlockState(), false);
            helper.assertFalse(DocumentedFauna.hasSpace(snowy, feet, species), "Yeti snow-block roofs must reject " + species.id());
            chunk.setBlockState(feet, Blocks.SNOW.defaultBlockState(), false);
            helper.assertFalse(DocumentedFauna.hasSpace(snowy, feet, species), "A snow layer must not disguise a snow-block roof");
            chunk.setBlockState(feet.below(), Blocks.STONE.defaultBlockState(), false);
            for (int layers : new int[]{1, 4, 8}) {
                chunk.setBlockState(feet, Blocks.SNOW.defaultBlockState().setValue(SnowLayerBlock.LAYERS, layers), false);
                helper.assertTrue(DocumentedFauna.hasSpace(snowy, feet, species), "Ordinary snow layers must remain valid: " + species.id());
                Mob mob = (Mob) species.type().create(snowy);
                mob.moveTo(feet.getX() + .5, feet.getY(), feet.getZ() + .5);
                var check = new MobSpawnEvent.PositionCheck(mob, snowy, MobSpawnType.CHUNK_GENERATION, null);
                DocumentedNaturalSpawns.position(check);
                helper.assertTrue(check.getResult() == Event.Result.ALLOW && mob.getY() == feet.getY() + 1,
                        "Snow-layer spawning must move the actual animal clear of collision: " + species.id());
                mob.discard();
            }
            chunk.setBlockState(feet, Blocks.AIR.defaultBlockState(), false);
        }
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void savannaRosterAndZombieJockeysCannotIntroduceChickens(GameTestHelper helper) {
        var expected = Set.of("biodiverse:wildebeest", "biodiverse:oryx", "alexsmobs:bison", "alexsmobs:emu",
                "alexsmobs:kangaroo", "twilightforest:boar", "minecraft:rabbit", "minecraft:sheep", "minecraft:horse", "minecraft:llama");
        helper.assertTrue(DocumentedFauna.forBiome("savanna").stream().filter(species -> species.habitat() == DocumentedFauna.Habitat.LAND)
                        .map(species -> species.id().toString()).collect(Collectors.toSet()).equals(expected),
                "Savanna livestock must contain exactly the eight selected species plus horses and llamas");
        var savanna = helper.getLevel().getServer().getLevel(GameplayDimensions.SAVANNA);
        var chicken = EntityType.CHICKEN.create(savanna);
        chicken.setChickenJockey(true);
        var rider = EntityType.ZOMBIE.create(savanna);
        rider.setBaby(true);
        rider.startRiding(chicken);
        var join = new EntityJoinLevelEvent(chicken, savanna);
        DocumentedNaturalSpawns.finishedSpawn(join);
        helper.assertTrue(join.isCanceled() && !rider.isPassenger(),
                "Zombie jockey creation must not bypass the Savanna animal roster or strand its rider");
        var manual = new EntityJoinLevelEvent(EntityType.CHICKEN.create(savanna), savanna);
        DocumentedNaturalSpawns.finishedSpawn(manual);
        helper.assertFalse(manual.isCanceled(), "Manual ordinary chicken placement must remain available");
        chicken.discard(); rider.discard(); manual.getEntity().discard();
        helper.succeed();
    }

    private static LevelChunk biomeChunk(ServerLevel level, BlockPos pos, String name) {
        LevelChunk chunk = level.getChunkAt(pos);
        var biome = level.registryAccess().registryOrThrow(Registries.BIOME).getHolderOrThrow(
                ResourceKey.create(Registries.BIOME, AOEMod.makeID(name)));
        chunk.fillBiomesFromNoise((x, y, z, sampler) -> biome, level.getChunkSource().randomState().sampler());
        return chunk;
    }

    private static void assertNativeSpawn(GameTestHelper helper, ServerLevel level, LevelChunk chunk, BlockPos feet,
            EntityType<?> target, MobCategory category, boolean chooseFloor) throws Exception {
        FakePlayer player = new FakePlayer(level, new GameProfile(UUID.randomUUID(), "CaveFaunaTest"));
        player.setPos(feet.getX() + 40.5, feet.getY(), feet.getZ() + .5);
        level.players().add(player);
        var local = new LocalMobCapCalculator(level.getChunkSource().chunkMap);
        var field = Arrays.stream(LocalMobCapCalculator.class.getDeclaredFields())
                .filter(candidate -> Long2ObjectMap.class.isAssignableFrom(candidate.getType())).findFirst().orElseThrow();
        field.setAccessible(true);
        ((Long2ObjectMap<List<ServerPlayer>>) field.get(local)).put(chunk.getPos().toLong(), List.of(player));
        var state = NaturalSpawner.createState(289, List.of(), (key, consumer) -> consumer.accept(chunk), local);
        int[] spawned = {0};
        try {
            level.random.setSeed(8735612L);
            NaturalSpawner.SpawnPredicate predicate = (type, pos, candidateChunk) -> type == target
                    && (boolean) invoke(CANDIDATE_GATE, state, type, pos, candidateChunk);
            NaturalSpawner.AfterSpawnCallback callback = (mob, candidateChunk) -> {
                invoke(AFTER_SPAWN, state, mob, candidateChunk);
                spawned[0]++;
                mob.discard();
            };
            for (int tries = 0; tries < 600 && spawned[0] == 0; tries++) {
                if (chooseFloor) NaturalSpawner.spawnCategoryForChunk(category, level, chunk, predicate, callback);
                else NaturalSpawner.spawnCategoryForPosition(category, level, chunk, feet, predicate, callback);
            }
            helper.assertTrue(spawned[0] > 0 && state.getMobCategoryCounts().getInt(category) == spawned[0],
                    "Native placement, position, finalization, join and immediate census must succeed for " + target);
            Mob census = (Mob) target.create(level);
            census.moveTo(feet.getX() + .5, feet.getY(), feet.getZ() + .5);
            while (state.getMobCategoryCounts().getInt(category) < category.getMaxInstancesPerChunk()) {
                invoke(AFTER_SPAWN, state, census, chunk);
            }
            helper.assertFalse(local.canSpawn(category, chunk.getPos()),
                    "Successful cave/water spawns must close their existing local cap: " + target);
            census.discard();
        } finally {
            level.players().remove(player);
            player.discard();
        }
    }

    private static Method method(Class<?>... parameters) {
        Method method = Arrays.stream(NaturalSpawner.SpawnState.class.getDeclaredMethods())
                .filter(candidate -> Arrays.equals(candidate.getParameterTypes(), parameters)).findFirst().orElseThrow();
        method.setAccessible(true);
        return method;
    }

    private static Object invoke(Method method, Object target, Object... args) {
        try { return method.invoke(target, args); }
        catch (ReflectiveOperationException failure) { throw new IllegalStateException(failure); }
    }
}

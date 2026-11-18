package net.amurdza.examplemod.gametest;

import com.google.gson.GsonBuilder;
import com.mojang.authlib.GameProfile;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.util.GameplayDimensions;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LocalMobCapCalculator;
import net.minecraft.world.level.NaturalSpawner;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.registries.ForgeRegistries;

import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@GameTestHolder(AOEMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class AquaticSpawnRateTests {
    private static final Method CATEGORY_GATE = method(MobCategory.class, ChunkPos.class);
    private static final Method CANDIDATE_GATE = method(EntityType.class, BlockPos.class, ChunkAccess.class);
    private static final Method AFTER_SPAWN = method(Mob.class, ChunkAccess.class);

    @GameTest(template = "plant_gameplay_empty", timeoutTicks = 400)
    public static void nativeSpawnChainFillsIndependentSmallAndLargeAquaticBudgets(GameTestHelper helper) throws Exception {
        var level = helper.getLevel().getServer().getLevel(GameplayDimensions.PLAINS);
        helper.assertTrue(MobCategory.WATER_AMBIENT.getMaxInstancesPerChunk() == 20
                        && MobCategory.WATER_CREATURE.getMaxInstancesPerChunk() == 5,
                "Small fish must have four times the population budget of larger water creatures");

        BlockPos center = new BlockPos(2056, 185, 2056);
        LevelChunk chunk = level.getChunkAt(center);
        var biome = level.registryAccess().registryOrThrow(Registries.BIOME).getHolderOrThrow(
                ResourceKey.create(Registries.BIOME, new ResourceLocation("aoemod:plains_lake")));
        chunk.fillBiomesFromNoise((x, y, z, sampler) -> biome, level.getChunkSource().randomState().sampler());
        for (BlockPos pos : BlockPos.betweenClosed(center.getX() - 8, 180, center.getZ() - 8,
                center.getX() + 7, 191, center.getZ() + 7)) {
            chunk.setBlockState(pos, pos.getY() == 180 ? Blocks.STONE.defaultBlockState()
                    : pos.getY() <= 190 ? Blocks.WATER.defaultBlockState() : Blocks.AIR.defaultBlockState(), false);
        }
        FakePlayer player = new FakePlayer(level, new GameProfile(UUID.randomUUID(), "AquaticSpawnTest"));
        player.setPos(center.getX() + 40.5, 185, center.getZ() + 0.5);
        level.players().add(player);
        var local = caps(level, chunk.getPos(), player);
        var state = NaturalSpawner.createState(289, List.of(), (key, consumer) -> consumer.accept(chunk), local);
        List<Mob> spawned = new ArrayList<>();
        var attempts = new java.util.LinkedHashMap<String, Integer>();
        try {
            level.random.setSeed(7823931L);
            for (var category : List.of(MobCategory.WATER_AMBIENT, MobCategory.WATER_CREATURE)) {
                int tries = 0;
                int before = spawned.size();
                while (gate(state, category, chunk.getPos()) && tries++ < 200) {
                    NaturalSpawner.spawnCategoryForPosition(category, level, chunk, center,
                            (type, pos, candidateChunk) -> (boolean) invoke(CANDIDATE_GATE, state, type, pos, candidateChunk),
                            (mob, candidateChunk) -> {
                                invoke(AFTER_SPAWN, state, mob, candidateChunk);
                                spawned.add(mob);
                                // Keep geometry fixed; the native counters still retain every successful spawn.
                                mob.discard();
                            });
                }
                attempts.put(category.getName(), tries);
                int count = state.getMobCategoryCounts().getInt(category);
                int cap = category.getMaxInstancesPerChunk();
                int packMaximum = category == MobCategory.WATER_AMBIENT ? 5 : 3;
                helper.assertTrue(count == spawned.size() - before && count >= cap && count <= cap + packMaximum - 1,
                        "The real spawn chain must fill only its category, with at most one pack's overshoot: " + category + "=" + count);
                helper.assertTrue(!gate(state, category, chunk.getPos()) && !local.canSpawn(category, chunk.getPos()),
                        "Global and local gates must close at their respective category caps: " + category);
                if (category == MobCategory.WATER_AMBIENT) {
                    helper.assertTrue(gate(state, MobCategory.WATER_CREATURE, chunk.getPos())
                                    && local.canSpawn(MobCategory.WATER_CREATURE, chunk.getPos()),
                            "Filling the small-fish pool must not consume large-creature capacity");
                }
            }
            Files.writeString(Path.of("aquatic-spawn-rate-revision.json"), new GsonBuilder().setPrettyPrinting().create().toJson(
                    Map.of("smallFishCap", 20, "largeCreatureCap", 5,
                            "smallSpawned", state.getMobCategoryCounts().getInt(MobCategory.WATER_AMBIENT),
                            "largeSpawned", state.getMobCategoryCounts().getInt(MobCategory.WATER_CREATURE),
                            "nativeAttempts", attempts, "species", spawned.stream().map(mob -> mob.getType().toShortString()).distinct().toList())));
        } finally {
            spawned.forEach(Entity::discard);
            level.players().remove(player);
            player.discard();
        }
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void realCensusAndLocalCapsPreservePersistentAnimalsAndOutsideCategories(GameTestHelper helper) throws Exception {
        ServerLevel tropical = helper.getLevel().getServer().getLevel(GameplayDimensions.TROPICAL);
        var chunk = helper.getLevel().getChunkAt(helper.absolutePos(BlockPos.ZERO));
        FakePlayer player = new FakePlayer(tropical, new GameProfile(UUID.randomUUID(), "AquaticCapTest"));
        player.setPos(chunk.getPos().getMiddleBlockX() + 40, 100, chunk.getPos().getMiddleBlockZ());
        tropical.players().add(player);
        List<Entity> animals = new ArrayList<>();
        try {
            EntityType<?>[] largeTypes = {EntityType.TURTLE, EntityType.AXOLOTL, EntityType.DOLPHIN,
                    ForgeRegistries.ENTITY_TYPES.getValue(new ResourceLocation("aoemod:nautilus")),
                    EntityType.SQUID};
            for (int i = 0; i < 15; i++) {
                Mob mob = (Mob) (i < 10 ? EntityType.TROPICAL_FISH : largeTypes[i - 10]).create(tropical);
                mob.setPos(chunk.getPos().getMiddleBlockX(), 100, chunk.getPos().getMiddleBlockZ());
                animals.add(mob);
            }
            var local = caps(tropical, chunk.getPos(), player);
            var state = NaturalSpawner.createState(289, animals, (key, consumer) -> consumer.accept(chunk), local);
            helper.assertTrue(state.getMobCategoryCounts().getInt(net.amurdza.examplemod.registry.ModMobCategories.COMMON_AQUATIC) == 10
                            && state.getMobCategoryCounts().getInt(MobCategory.WATER_CREATURE) == 5
                            && gate(state, net.amurdza.examplemod.registry.ModMobCategories.COMMON_AQUATIC, chunk.getPos())
                            && local.canSpawn(net.amurdza.examplemod.registry.ModMobCategories.COMMON_AQUATIC, chunk.getPos())
                            && !gate(state, MobCategory.WATER_CREATURE, chunk.getPos())
                            && !local.canSpawn(MobCategory.WATER_CREATURE, chunk.getPos()),
                    "Five large creatures must close their own cap while leaving room for thirty more small fish");
            for (int i = 10; i < 40; i++) invoke(AFTER_SPAWN, state, animals.get(0), chunk);
            helper.assertTrue(!gate(state, net.amurdza.examplemod.registry.ModMobCategories.COMMON_AQUATIC, chunk.getPos())
                            && !local.canSpawn(net.amurdza.examplemod.registry.ModMobCategories.COMMON_AQUATIC, chunk.getPos())
                            && state.getMobCategoryCounts().getInt(MobCategory.WATER_CREATURE) == 5,
                    "Immediate small-fish accounting must fill only its own 40-animal budget");

            Mob owned = EntityType.TURTLE.create(tropical);
            owned.setPersistenceRequired();
            owned.setPos(chunk.getPos().getMiddleBlockX(), 100, chunk.getPos().getMiddleBlockZ());
            animals.add(owned);
            var persistentState = NaturalSpawner.createState(289, List.of(owned), (key, consumer) -> consumer.accept(chunk), caps(tropical, chunk.getPos(), player));
            helper.assertTrue(persistentState.getMobCategoryCounts().getInt(MobCategory.WATER_CREATURE) == 0 && !owned.isRemoved(),
                    "Player-persistent animals keep vanilla exemption and must never be deleted to free space");

            Mob squid = EntityType.SQUID.create(tropical);
            squid.setPos(chunk.getPos().getMiddleBlockX(), 100, chunk.getPos().getMiddleBlockZ());
            animals.add(squid);
            var squidLocal = caps(tropical, chunk.getPos(), player);
            var squidState = NaturalSpawner.createState(289, List.of(squid), (key, consumer) -> consumer.accept(chunk), squidLocal);
            helper.assertTrue(squidState.getMobCategoryCounts().getInt(MobCategory.WATER_CREATURE) == 1
                            && squidState.getMobCategoryCounts().getInt(net.amurdza.examplemod.registry.ModMobCategories.COMMON_AQUATIC) == 0,
                    "Custom-dimension squid must use its native census without consuming fish capacity");
            for (int i = 1; i < MobCategory.WATER_CREATURE.getMaxInstancesPerChunk(); i++) invoke(AFTER_SPAWN, squidState, squid, chunk);
            helper.assertTrue(!squidLocal.canSpawn(MobCategory.WATER_CREATURE, chunk.getPos())
                            && squidLocal.canSpawn(net.amurdza.examplemod.registry.ModMobCategories.COMMON_AQUATIC, chunk.getPos()),
                    "Squid share the large-creature local cap independently of the small-fish cap");

            Mob outside = EntityType.SQUID.create(helper.getLevel());
            animals.add(outside);
            var outsideState = NaturalSpawner.createState(289, List.of(outside), (key, consumer) -> consumer.accept(chunk),
                    caps(helper.getLevel(), chunk.getPos(), player));
            helper.assertTrue(outsideState.getMobCategoryCounts().getInt(MobCategory.WATER_CREATURE) == 1
                            && outsideState.getMobCategoryCounts().getInt(net.amurdza.examplemod.registry.ModMobCategories.COMMON_AQUATIC) == 0,
                    "Native squid accounting must remain untouched outside the custom dimensions");
        } finally {
            tropical.players().remove(player);
            animals.forEach(Entity::discard); player.discard();
        }
        helper.succeed();
    }

    @SuppressWarnings("unchecked")
    private static LocalMobCapCalculator caps(ServerLevel level, ChunkPos chunk, ServerPlayer player) throws Exception {
        var local = new LocalMobCapCalculator(level.getChunkSource().chunkMap);
        var cache = Arrays.stream(LocalMobCapCalculator.class.getDeclaredFields())
                .filter(field -> Long2ObjectMap.class.isAssignableFrom(field.getType())).findFirst().orElseThrow();
        cache.setAccessible(true);
        ((Long2ObjectMap<List<ServerPlayer>>) cache.get(local)).put(chunk.toLong(), List.of(player));
        return local;
    }

    private static Method method(Class<?>... parameters) {
        Method method = Arrays.stream(NaturalSpawner.SpawnState.class.getDeclaredMethods())
                .filter(candidate -> Arrays.equals(candidate.getParameterTypes(), parameters)).findFirst().orElseThrow();
        method.setAccessible(true);
        return method;
    }

    private static Object invoke(Method method, Object target, Object... arguments) {
        try { return method.invoke(target, arguments); }
        catch (ReflectiveOperationException failure) { throw new IllegalStateException(failure); }
    }

    private static boolean gate(NaturalSpawner.SpawnState state, MobCategory category, ChunkPos chunk) {
        return (boolean) invoke(CATEGORY_GATE, state, category, chunk);
    }
}

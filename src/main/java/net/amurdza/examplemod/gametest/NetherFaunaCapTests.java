package net.amurdza.examplemod.gametest;

import com.mojang.authlib.GameProfile;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.registry.ModMobCategories;
import net.amurdza.examplemod.util.GameplayDimensions;
import net.amurdza.examplemod.worldgen.DocumentedCaveSpawns;
import net.amurdza.examplemod.worldgen.DocumentedFauna;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LocalMobCapCalculator;
import net.minecraft.world.level.NaturalSpawner;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@GameTestHolder(AOEMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class NetherFaunaCapTests {
    @GameTest(template = "plant_gameplay_empty")
    @SuppressWarnings("unchecked")
    public static void hoglinsAndStridersHaveSeparateTwentyAnimalGlobalAndLocalCaps(GameTestHelper helper) throws Exception {
        var nether = helper.getLevel().getServer().getLevel(GameplayDimensions.NETHER);
        var type = EntityType.HOGLIN;
        var category = ModMobCategories.HOGLIN;
        var striderCategory = ModMobCategories.STRIDER;
        helper.assertTrue(category.getMaxInstancesPerChunk() == 20 && striderCategory.getMaxInstancesPerChunk() == 20,
                "Both Nether livestock budgets must be twenty");
        var biomes = nether.registryAccess().registryOrThrow(Registries.BIOME);
        var crimson = biomes.get(AOEMod.makeID("crimson_forest"));
        var warped = biomes.get(AOEMod.makeID("warped_forest"));
        helper.assertTrue(crimson.getMobSettings().getMobs(category).unwrap().stream().anyMatch(entry -> entry.type == type),
                "Crimson hoglins must occupy the independent Nether fauna pool");
        helper.assertTrue(warped.getMobSettings().getMobs(striderCategory).unwrap().stream().anyMatch(entry -> entry.type == EntityType.STRIDER),
                "Warped striders must occupy the independent Nether fauna pool");
        helper.assertTrue(warped.getMobSettings().getMobs(category).unwrap().stream().noneMatch(entry -> entry.type == type),
                "Hoglins remain exclusive to crimson forest");
        for (var biome : List.of(crimson, warped)) for (MobCategory other : MobCategory.values()) {
            if (other != (biome == crimson ? category : striderCategory)) helper.assertTrue(biome.getMobSettings().getMobs(other).unwrap().stream()
                            .noneMatch(entry -> DocumentedFauna.isNetherFauna(entry.type)),
                    "Hoglins and striders must not also occupy another pool: " + other);
        }
        helper.assertTrue(DocumentedCaveSpawns.usesFloorSampling(nether, category)
                        && DocumentedCaveSpawns.usesFloorSampling(nether, striderCategory),
                "The separate Nether fauna pool must use the floor sampler");
        var categories = Arrays.stream(NaturalSpawner.class.getDeclaredFields())
                .filter(field -> field.getType() == MobCategory[].class).findFirst().orElseThrow();
        categories.setAccessible(true);
        helper.assertTrue(Arrays.asList((MobCategory[]) categories.get(null)).containsAll(List.of(category, striderCategory)),
                "NaturalSpawner's cached categories must include the independent Nether fauna pool");
        var chunk = helper.getLevel().getChunkAt(helper.absolutePos(BlockPos.ZERO));
        double x = chunk.getPos().getMiddleBlockX() + .5, z = chunk.getPos().getMiddleBlockZ() + .5;
        var player = new FakePlayer(nether, new GameProfile(UUID.randomUUID(), "NetherFaunaCap"));
        player.setPos(x + 40, 0, z);
        nether.players().add(player);
        List<Entity> residents = new ArrayList<>();
        var hoglin = type.create(nether);
        hoglin.setPos(x, 0, z);
        residents.add(hoglin);
        for (int i = 0; i < 70; i++) {
            var hostile = EntityType.ZOMBIE.create(nether);
            hostile.setPos(x, 0, z);
            residents.add(hostile);
        }
        try {
            var local = new LocalMobCapCalculator(nether.getChunkSource().chunkMap);
            var nearby = Arrays.stream(LocalMobCapCalculator.class.getDeclaredFields())
                    .filter(field -> Long2ObjectMap.class.isAssignableFrom(field.getType())).findFirst().orElseThrow();
            nearby.setAccessible(true);
            ((Long2ObjectMap<List<ServerPlayer>>) nearby.get(local)).put(chunk.getPos().toLong(), List.of(player));
            var state = NaturalSpawner.createState(289, residents, (key, consumer) -> consumer.accept(chunk), local);
            var canSpawn = Arrays.stream(NaturalSpawner.SpawnState.class.getDeclaredMethods())
                    .filter(method -> Arrays.equals(method.getParameterTypes(), new Class<?>[]{MobCategory.class, ChunkPos.class}))
                    .findFirst().orElseThrow();
            canSpawn.setAccessible(true);
            helper.assertTrue(state.getMobCategoryCounts().getInt(category) == 1
                            && state.getMobCategoryCounts().getInt(MobCategory.MONSTER) == 70
                            && (boolean) canSpawn.invoke(state, category, chunk.getPos())
                            && !(boolean) canSpawn.invoke(state, MobCategory.MONSTER, chunk.getPos())
                            && local.canSpawn(category, chunk.getPos()),
                    "A full monster cap must not consume the Nether fauna global or local budget");
            var afterSpawn = Arrays.stream(NaturalSpawner.SpawnState.class.getDeclaredMethods())
                    .filter(method -> Arrays.equals(method.getParameterTypes(), new Class<?>[]{Mob.class, ChunkAccess.class}))
                    .findFirst().orElseThrow();
            afterSpawn.setAccessible(true);
            var strider = EntityType.STRIDER.create(nether);
            strider.setPos(x, 0, z);
            residents.add(strider);
            for (int i = 1; i < 20; i++) afterSpawn.invoke(state, hoglin, chunk);
            helper.assertTrue(state.getMobCategoryCounts().getInt(category) == 20
                            && !(boolean) canSpawn.invoke(state, category, chunk.getPos())
                            && !local.canSpawn(category, chunk.getPos())
                            && (boolean) canSpawn.invoke(state, striderCategory, chunk.getPos())
                            && local.canSpawn(striderCategory, chunk.getPos()),
                    "Twenty hoglins must fill both hoglin caps while leaving room for striders");
            for (int i = 0; i < 20; i++) afterSpawn.invoke(state, strider, chunk);
            helper.assertTrue(state.getMobCategoryCounts().getInt(striderCategory) == 20
                            && !(boolean) canSpawn.invoke(state, striderCategory, chunk.getPos())
                            && !local.canSpawn(striderCategory, chunk.getPos()),
                    "Twenty striders must independently fill both strider caps");
            var outside = type.create(helper.getLevel());
            helper.assertTrue(DocumentedFauna.spawnCategory(outside, type.getCategory()) == type.getCategory(),
                    "Other dimensions retain the source mod's category");
            outside.discard();
        } finally {
            nether.players().remove(player);
            player.discard();
            residents.forEach(Entity::discard);
        }
        helper.succeed();
    }

}

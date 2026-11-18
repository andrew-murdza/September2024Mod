package net.amurdza.examplemod.gametest;

import blueduck.outer_end.entity.Stalker;
import com.mojang.authlib.GameProfile;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.event_handlers.DocumentedNaturalSpawns;
import net.amurdza.examplemod.registry.ModMobCategories;
import net.amurdza.examplemod.util.GameplayDimensions;
import net.amurdza.examplemod.worldgen.DocumentedCaveSpawns;
import net.amurdza.examplemod.worldgen.DocumentedFauna;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
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
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@GameTestHolder(AOEMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class DeepDarkFaunaRevisionTests {
    @GameTest(template = "plant_gameplay_empty")
    @SuppressWarnings("unchecked")
    public static void stalkersHaveAnIndependentTwentyAnimalGlobalAndLocalCap(GameTestHelper helper) throws Exception {
        var desert = helper.getLevel().getServer().getLevel(GameplayDimensions.DESERT);
        var type = ForgeRegistries.ENTITY_TYPES.getValue(new ResourceLocation("outer_end", "stalker"));
        var category = ModMobCategories.DEEP_DARK;
        helper.assertTrue(category.getMaxInstancesPerChunk() == ModMobCategories.HOGLIN.getMaxInstancesPerChunk()
                        && category.getMaxInstancesPerChunk() == ModMobCategories.STRIDER.getMaxInstancesPerChunk()
                        && category.getMaxInstancesPerChunk() * 2 == ModMobCategories.MUSHROOM_CAVE.getMaxInstancesPerChunk(),
                "Stalkers and both Nether livestock species have individual twenty-animal budgets");
        var biome = desert.registryAccess().registryOrThrow(Registries.BIOME).get(AOEMod.makeID("deep_dark"));
        helper.assertTrue(biome.getMobSettings().getMobs(category).unwrap().stream().anyMatch(entry -> entry.type == type)
                        && biome.getMobSettings().getMobs(MobCategory.MONSTER).unwrap().stream().noneMatch(entry -> entry.type == type)
                        && biome.getMobSettings().getMobs(MobCategory.MONSTER).unwrap().stream().anyMatch(entry -> entry.type == EntityType.WARDEN)
                        && DocumentedCaveSpawns.usesFloorSampling(desert, category),
                "Stalkers need their own cave-floor pool while ordinary hostiles retain MONSTER");
        var categories = Arrays.stream(NaturalSpawner.class.getDeclaredFields())
                .filter(field -> field.getType() == MobCategory[].class).findFirst().orElseThrow();
        categories.setAccessible(true);
        helper.assertTrue(Arrays.asList((MobCategory[]) categories.get(null)).contains(category),
                "NaturalSpawner's cached categories must include the independent Stalker pool");
        var chunk = helper.getLevel().getChunkAt(helper.absolutePos(BlockPos.ZERO));
        double x = chunk.getPos().getMiddleBlockX() + .5, z = chunk.getPos().getMiddleBlockZ() + .5;
        var player = new FakePlayer(desert, new GameProfile(UUID.randomUUID(), "DeepDarkFaunaCap"));
        player.setPos(x + 40, 0, z);
        desert.players().add(player);
        List<Entity> residents = new ArrayList<>();
        var stalker = (Stalker) type.create(desert);
        stalker.setPos(x, 0, z);
        residents.add(stalker);
        for (int i = 0; i < 70; i++) {
            var hostile = EntityType.ZOMBIE.create(desert);
            hostile.setPos(x, 0, z);
            residents.add(hostile);
        }
        try {
            var local = new LocalMobCapCalculator(desert.getChunkSource().chunkMap);
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
                    "A full monster cap must not consume the Stalker global or local budget");
            var afterSpawn = Arrays.stream(NaturalSpawner.SpawnState.class.getDeclaredMethods())
                    .filter(method -> Arrays.equals(method.getParameterTypes(), new Class<?>[]{Mob.class, ChunkAccess.class}))
                    .findFirst().orElseThrow();
            afterSpawn.setAccessible(true);
            for (int i = 1; i < 20; i++) afterSpawn.invoke(state, stalker, chunk);
            helper.assertTrue(state.getMobCategoryCounts().getInt(category) == 20
                            && !(boolean) canSpawn.invoke(state, category, chunk.getPos())
                            && !local.canSpawn(category, chunk.getPos()),
                    "The twentieth Stalker must immediately fill both native caps");
            var outside = type.create(helper.getLevel());
            helper.assertTrue(DocumentedFauna.spawnCategory(outside, type.getCategory()) == type.getCategory(),
                    "Other dimensions retain the source mod's category");
            outside.discard();
        } finally {
            desert.players().remove(player);
            player.discard();
            residents.forEach(Entity::discard);
        }
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void coldWaterBiomesRejectLandFaunaAndStalkersRestoreCobalt(GameTestHelper helper) {
        var snowy = helper.getLevel().getServer().getLevel(GameplayDimensions.SNOWY);
        for (String name : List.of("cold_ocean", "mountain_lake")) {
            helper.assertTrue(DocumentedFauna.forBiome(name).stream().noneMatch(species -> species.habitat() == DocumentedFauna.Habitat.LAND),
                    "Cold water biomes must not declare land animals: " + name);
            var biome = snowy.registryAccess().registryOrThrow(Registries.BIOME).get(AOEMod.makeID(name));
            var loaded = Arrays.stream(MobCategory.values()).flatMap(category -> biome.getMobSettings().getMobs(category).unwrap().stream());
            helper.assertTrue(loaded.allMatch(entry -> DocumentedFauna.isWaterSpecies(entry.type)),
                    "Only aquatic animals may remain in the loaded cold-water spawn tables: " + name);
        }
        var desert = helper.getLevel().getServer().getLevel(GameplayDimensions.DESERT);
        var stalker = (Stalker) ForgeRegistries.ENTITY_TYPES.getValue(new ResourceLocation("outer_end", "stalker")).create(desert);
        for (String color : List.of("", "mint", "rose", "cobalt")) {
            stalker.setColor(color);
            DocumentedNaturalSpawns.finishedSpawn(new EntityJoinLevelEvent(stalker, desert, true));
            helper.assertTrue(stalker.getColor().equals("cobalt"), "Saved and new Stalkers must normalize to cobalt");
        }
        stalker.discard();
        helper.assertTrue(Blocks.SCULK_CATALYST.defaultBlockState().getLightEmission(desert, BlockPos.ZERO) == 8,
                "Sculk catalysts must emit two levels more than their original six");
        helper.succeed();
    }
}

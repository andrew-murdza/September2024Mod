package net.amurdza.examplemod.gametest;

import com.mojang.authlib.GameProfile;
import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.util.GameplayDimensions;
import net.amurdza.examplemod.worldgen.AquaticSpawnDistanceRules;
import net.amurdza.examplemod.worldgen.DocumentedFauna;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.animal.Pufferfish;
import net.minecraft.world.level.NaturalSpawner;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.registries.ForgeRegistries;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

@GameTestHolder(AOEMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class AquaticSpawnDistanceTests {
    private static final Method CANDIDATE_GATE = method(ServerLevel.class, MobCategory.class,
            StructureManager.class, ChunkGenerator.class, MobSpawnSettings.SpawnerData.class,
            BlockPos.MutableBlockPos.class, double.class);
    private static final Method POSITION_GATE = method(ServerLevel.class, Mob.class, double.class);

    @GameTest(template = "plant_gameplay_empty", timeoutTicks = 200)
    public static void nativeSpawnGatesUseSixtyFourForTropicalFishAndOneTwentyEightForLargeCreatures(GameTestHelper helper) throws Exception {
        var tropical = helper.getLevel().getServer().getLevel(GameplayDimensions.TROPICAL);
        BlockPos center = new BlockPos(3128, 185, 3128);
        var chunk = tropical.getChunkAt(center);
        var biome = tropical.registryAccess().registryOrThrow(Registries.BIOME).getHolderOrThrow(
                ResourceKey.create(Registries.BIOME, new ResourceLocation("aoemod:coral_reef")));
        chunk.fillBiomesFromNoise((x, y, z, sampler) -> biome, tropical.getChunkSource().randomState().sampler());
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-3, -3, -3), center.offset(3, 4, 3))) {
            chunk.setBlockState(pos, pos.getY() == 182 ? Blocks.SAND.defaultBlockState()
                    : pos.getY() <= 188 ? Blocks.WATER.defaultBlockState() : Blocks.AIR.defaultBlockState(), false);
        }
        List<Entity> animals = new ArrayList<>();
        Pufferfish outside = EntityType.PUFFERFISH.create(helper.getLevel());
        try {
            for (String id : List.of("minecraft:pufferfish", "minecraft:tropical_fish")) {
                var type = ForgeRegistries.ENTITY_TYPES.getValue(new ResourceLocation(id));
                var entry = biome.value().getMobSettings().getMobs(DocumentedFauna.spawnCategory(type)).unwrap().stream()
                        .filter(spawn -> spawn.type == type).findFirst().orElseThrow();
                Mob fish = (Mob) type.create(tropical);
                animals.add(fish);
                fish.setPos(center.getX() + 0.5, center.getY(), center.getZ() + 0.5);
                helper.assertTrue(type.getCategory() == MobCategory.WATER_AMBIENT
                                && AquaticSpawnDistanceRules.category(tropical, type).getDespawnDistance() == 64,
                        "The requested small fish must use a dedicated 64-block range: " + id);
                for (double distance : new double[]{28, 64}) {
                    helper.assertTrue(candidate(tropical, entry, center, distance)
                                    && (boolean) POSITION_GATE.invoke(null, tropical, fish, distance * distance),
                            "Both actual spawn gates must admit small fish through the 64-block boundary: " + id);
                }
                helper.assertFalse(candidate(tropical, entry, center, 65),
                        "The actual natural candidate gate must reject small fish beyond 64 blocks: " + id);
                if (fish.removeWhenFarAway(65.0 * 65.0)) {
                    helper.assertFalse((boolean) POSITION_GATE.invoke(null, tropical, fish, 65.0 * 65.0),
                            "The final native distance gate must also reject despawnable small fish beyond 64 blocks: " + id);
                }
            }
            var largeEntry = biome.value().getMobSettings().getMobs(MobCategory.WATER_CREATURE).unwrap().stream()
                    .filter(spawn -> spawn.type == EntityType.DOLPHIN).findFirst().orElseThrow();
            var dolphin = EntityType.DOLPHIN.create(tropical);
            animals.add(dolphin);
            dolphin.setPos(center.getX() + 0.5, center.getY(), center.getZ() + 0.5);
            helper.assertTrue(candidate(tropical, largeEntry, center, 96)
                            && (boolean) POSITION_GATE.invoke(null, tropical, dolphin, 96.0 * 96.0),
                    "Large water creatures must keep their 128-block range");
            helper.assertFalse(candidate(tropical, largeEntry, center, 129)
                            || (boolean) POSITION_GATE.invoke(null, tropical, dolphin, 129.0 * 129.0),
                    "Large water creatures must still stop at 128 blocks");
            outside.setPos(dolphin.position());
            var smallEntry = biome.value().getMobSettings().getMobs(DocumentedFauna.spawnCategory(EntityType.PUFFERFISH)).unwrap().stream()
                    .filter(spawn -> spawn.type == EntityType.PUFFERFISH).findFirst().orElseThrow();
            helper.assertFalse(candidate(helper.getLevel(), smallEntry, center, 65)
                            || (boolean) POSITION_GATE.invoke(null, helper.getLevel(), outside, 65.0 * 65.0),
                    "Outside custom dimensions the native small-fish range must remain intact");
            helper.assertTrue(EntityType.PUFFERFISH.getCategory() == MobCategory.WATER_AMBIENT
                            && EntityType.PUFFERFISH.getCategory().getDespawnDistance() == 64
                            && AquaticSpawnDistanceRules.category(tropical, EntityType.SQUID) == MobCategory.WATER_CREATURE
                            && AquaticSpawnDistanceRules.category(tropical, EntityType.PIG) == EntityType.PIG.getCategory(),
                    "Range correction must not mutate native categories or affect unrelated animals");
        } finally {
            animals.forEach(Entity::discard);
            outside.discard();
        }
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void fishDespawnRangeMatchesSpawningAndPreservesPlayerProtection(GameTestHelper helper) {
        var tropical = helper.getLevel().getServer().getLevel(GameplayDimensions.TROPICAL);
        var outsideLevel = helper.getLevel();
        FakePlayer tropicalPlayer = player(tropical, "ReefRangeTest");
        FakePlayer outsidePlayer = player(outsideLevel, "NativeRangeTest");
        List<Entity> animals = new ArrayList<>();
        tropical.players().add(tropicalPlayer);
        outsideLevel.players().add(outsidePlayer);
        try {
            Pufferfish fish = fish(tropical, tropicalPlayer, 64, animals);
            fish.checkDespawn();
            helper.assertFalse(fish.isRemoved(), "A natural small reef fish must survive at the 64-block boundary");
            fish.setPos(tropicalPlayer.getX() + 65, tropicalPlayer.getY(), tropicalPlayer.getZ());
            fish.checkDespawn();
            helper.assertTrue(fish.isRemoved(), "An unprotected small reef fish must despawn beyond 64 blocks");

            Pufferfish diagonal = fish(tropical, tropicalPlayer, 48, animals);
            diagonal.setPos(tropicalPlayer.getX() + 48, tropicalPlayer.getY(), tropicalPlayer.getZ() + 48);
            diagonal.checkDespawn();
            helper.assertTrue(diagonal.isRemoved(), "Euclidean distance must reject a diagonal outside radius 64 even when both axes are within 64");

            var dolphin = EntityType.DOLPHIN.create(tropical);
            dolphin.setPos(tropicalPlayer.getX() + 96, tropicalPlayer.getY(), tropicalPlayer.getZ());
            animals.add(dolphin);
            dolphin.checkDespawn();
            helper.assertFalse(dolphin.isRemoved(), "Large creatures retain their longer despawn range");
            dolphin.setPos(tropicalPlayer.getX() + 129, tropicalPlayer.getY(), tropicalPlayer.getZ());
            dolphin.checkDespawn();
            helper.assertTrue(dolphin.isRemoved(), "Large creatures remain bounded at 128 blocks");

            Pufferfish outside = fish(outsideLevel, outsidePlayer, 96, animals);
            outside.checkDespawn();
            helper.assertTrue(outside.isRemoved(), "Fish outside custom dimensions retain vanilla's 64-block despawn distance");

            List<Consumer<Pufferfish>> protections = List.of(Mob::setPersistenceRequired,
                    protectedFish -> protectedFish.setFromBucket(true),
                    protectedFish -> protectedFish.setCustomName(Component.literal("Kept reef fish")));
            for (Consumer<Pufferfish> protection : protections) {
                Pufferfish kept = fish(tropical, tropicalPlayer, 160, animals);
                protection.accept(kept);
                kept.checkDespawn();
                helper.assertFalse(kept.isRemoved(), "Named, bucket-released and explicitly persistent fish must remain protected");
            }
            var turtle = EntityType.TURTLE.create(tropical);
            turtle.setPos(tropicalPlayer.getX() + 160, tropicalPlayer.getY(), tropicalPlayer.getZ());
            animals.add(turtle);
            turtle.checkDespawn();
            helper.assertFalse(turtle.isRemoved(), "The range correction must not make naturally permanent animals despawn");
            var glider = (Mob) ForgeRegistries.ENTITY_TYPES.getValue(new ResourceLocation("minecraft:tropical_fish")).create(tropical);
            glider.setPos(tropicalPlayer.getX() + 64, tropicalPlayer.getY(), tropicalPlayer.getZ());
            animals.add(glider);
            glider.checkDespawn();
            helper.assertFalse(glider.isRemoved(), "The tropical fish must remain within the ambient fish radius");
            glider.setPos(tropicalPlayer.getX() + 65, tropicalPlayer.getY(), tropicalPlayer.getZ());
            glider.checkDespawn();
            helper.assertTrue(glider.isRemoved(), "Rubber belly tropical fish must share the 64-block tropical fish despawn distance");
        } finally {
            animals.forEach(Entity::discard);
            tropical.players().remove(tropicalPlayer);
            outsideLevel.players().remove(outsidePlayer);
            tropicalPlayer.discard();
            outsidePlayer.discard();
        }
        helper.succeed();
    }

    private static boolean candidate(ServerLevel level, MobSpawnSettings.SpawnerData entry,
                                     BlockPos pos, double distance) throws Exception {
        return (boolean) CANDIDATE_GATE.invoke(null, level, DocumentedFauna.spawnCategory(entry.type), level.structureManager(),
                level.getChunkSource().getGenerator(), entry, pos.mutable(), distance * distance);
    }

    private static FakePlayer player(ServerLevel level, String name) {
        FakePlayer player = new FakePlayer(level, new GameProfile(UUID.randomUUID(), name));
        player.setPos(240000.5, 185, 240000.5);
        return player;
    }

    private static Pufferfish fish(ServerLevel level, FakePlayer player, double distance, List<Entity> animals) {
        Pufferfish fish = EntityType.PUFFERFISH.create(level);
        fish.setPos(player.getX() + distance, player.getY(), player.getZ());
        animals.add(fish);
        return fish;
    }

    private static Method method(Class<?>... parameters) {
        Method method = Arrays.stream(NaturalSpawner.class.getDeclaredMethods())
                .filter(candidate -> Arrays.equals(candidate.getParameterTypes(), parameters)).findFirst().orElseThrow();
        method.setAccessible(true);
        return method;
    }
}

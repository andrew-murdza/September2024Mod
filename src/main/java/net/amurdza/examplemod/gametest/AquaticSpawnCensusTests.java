package net.amurdza.examplemod.gametest;

import com.mojang.authlib.GameProfile;
import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.util.GameplayDimensions;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LocalMobCapCalculator;
import net.minecraft.world.level.NaturalSpawner;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@GameTestHolder(AOEMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class AquaticSpawnCensusTests {
    @GameTest(template = "plant_gameplay_empty")
    public static void exploringReefFreesCapacityWithoutDeletingDistantResidents(GameTestHelper helper) {
        ServerLevel level = helper.getLevel().getServer().getLevel(GameplayDimensions.TROPICAL);
        FakePlayer player = player(level, "ReefExplorer");
        FakePlayer other = player(level, "ReefObserver");
        List<Mob> animals = new ArrayList<>();
        try {
            // Preserve an old saved population even when it exceeds the new separate caps.
            for (int i = 0; i < 35; i++) {
                String id = i < 21 ? "minecraft:pufferfish"
                        : i < 32 ? "minecraft:tropical_fish"
                        : i == 32 ? "minecraft:axolotl" : "minecraft:turtle";
                Mob mob = (Mob) ForgeRegistries.ENTITY_TYPES.getValue(new ResourceLocation(id)).create(level);
                mob.setPos(i < 21 ? 0 : i < 32 ? 100 : 175, 100, 0);
                animals.add(mob);
            }
            player.setPos(50, 100, 0);
            var full = census(level, animals).getMobCategoryCounts();
            helper.assertTrue(full.getInt(net.amurdza.examplemod.registry.ModMobCategories.COMMON_AQUATIC) == 32
                            && full.getInt(MobCategory.WATER_CREATURE) == 3,
                    "Existing residents must be counted in their own pools even above the new caps");
            player.setPos(0, 100, 0);
            var nearby = census(level, animals).getMobCategoryCounts();
            helper.assertTrue(nearby.getInt(net.amurdza.examplemod.registry.ModMobCategories.COMMON_AQUATIC) == 21
                            && nearby.getInt(MobCategory.WATER_CREATURE) == 0,
                    "Exploring away from eleven permanent small tropical fish and three large residents must free their respective pools");
            helper.assertTrue(animals.stream().noneMatch(Entity::isRemoved),
                    "Releasing spawn capacity must never delete resident or player-kept animals");

            other.setPos(100, 100, 0);
            helper.assertTrue(count(level, animals) == 35,
                    "Residents near a second player still belong in the global census");
            other.setGameMode(GameType.SPECTATOR);
            helper.assertTrue(count(level, animals) == 21,
                    "Spectators cannot keep an offscreen aquatic population in the active cap");
            animals.get(0).setPersistenceRequired();
            helper.assertTrue(count(level, animals) == 20 && !animals.get(0).isRemoved(),
                    "Vanilla persistence exemptions must still protect owned animals");
        } finally {
            animals.forEach(Entity::discard);
            level.players().remove(player);
            level.players().remove(other);
            player.discard(); other.discard();
        }
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void bothAquaticCapsUseTheRangeBoundaryWithoutChangingOutsideDimensions(GameTestHelper helper) {
        ServerLevel tropical = helper.getLevel().getServer().getLevel(GameplayDimensions.TROPICAL);
        FakePlayer player = player(tropical, "ReefBoundary");
        Mob turtle = EntityType.TURTLE.create(tropical);
        Mob dolphin = EntityType.DOLPHIN.create(tropical);
        List<Mob> smallFish = new ArrayList<>();
        for (String id : List.of("minecraft:pufferfish", "minecraft:tropical_fish")) {
            smallFish.add((Mob) ForgeRegistries.ENTITY_TYPES.getValue(new ResourceLocation(id)).create(tropical));
        }
        Mob outside = EntityType.TROPICAL_FISH.create(helper.getLevel());
        try {
            player.setPos(0, 100, 0);
            turtle.setPos(128, 100, 0);
            helper.assertTrue(count(tropical, List.of(turtle)) == 1, "The 128-block boundary must still count");
            turtle.setPos(128.01, 100, 0);
            helper.assertTrue(count(tropical, List.of(turtle)) == 0, "Animals beyond the spawning radius must not fill the cap");
            for (Mob small : smallFish) {
                small.setPos(64, 100, 0);
                helper.assertTrue(census(tropical, List.of(small)).getMobCategoryCounts().getInt(net.amurdza.examplemod.worldgen.DocumentedFauna.spawnCategory(small.getType())) == 1,
                        "Small fish, including tropical fish and pufferfish, must count through 64 blocks: " + small.getType());
                small.setPos(64.01, 100, 0);
                helper.assertTrue(count(tropical, List.of(small)) == 0 && !small.isRemoved(),
                        "Small fish beyond 64 blocks must release capacity without census deleting them: " + small.getType());
            }
            dolphin.setPos(200, 100, 0);
            helper.assertTrue(count(tropical, List.of(dolphin)) == 0,
                    "Distant dolphins must release the same water-creature budget as other large animals");
            dolphin.setPos(128, 100, 0);
            helper.assertTrue(census(tropical, List.of(dolphin)).getMobCategoryCounts().getInt(MobCategory.WATER_CREATURE) == 1,
                    "Nearby dolphins must count in the large-animal pool");
            outside.setPos(200, 100, 0);
            helper.assertTrue(census(helper.getLevel(), List.of(outside)).getMobCategoryCounts()
                            .getInt(EntityType.TROPICAL_FISH.getCategory()) == 1,
                    "The distant-resident exemption must not change vanilla dimensions");
        } finally {
            tropical.players().remove(player);
            player.discard(); turtle.discard(); dolphin.discard(); outside.discard();
            smallFish.forEach(Entity::discard);
        }
        helper.succeed();
    }

    private static FakePlayer player(ServerLevel level, String name) {
        FakePlayer player = new FakePlayer(level, new GameProfile(UUID.randomUUID(), name));
        player.setPos(4096, 100, 4096);
        level.players().add(player);
        return player;
    }

    private static int count(ServerLevel level, List<? extends Entity> animals) {
        var counts = census(level, animals).getMobCategoryCounts();
        return counts.getInt(net.amurdza.examplemod.registry.ModMobCategories.COMMON_AQUATIC)
                + counts.getInt(MobCategory.WATER_CREATURE);
    }

    private static NaturalSpawner.SpawnState census(ServerLevel level, List<? extends Entity> animals) {
        var chunk = level.getChunkAt(new BlockPos(0, 100, 0));
        return NaturalSpawner.createState(289, new ArrayList<Entity>(animals), (key, consumer) -> consumer.accept(chunk),
                new LocalMobCapCalculator(level.getChunkSource().chunkMap));
    }
}

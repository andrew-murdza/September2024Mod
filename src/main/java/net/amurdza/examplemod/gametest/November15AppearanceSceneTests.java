package net.amurdza.examplemod.gametest;

import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.registry.ModEntities;
import net.amurdza.examplemod.util.GameplayDimensions;
import net.amurdza.examplemod.worldgen.DimensionTerrain;
import net.amurdza.examplemod.worldgen.NetherRampDecorationGuard;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.registries.ForgeRegistries;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Small persistent native-entity scene, isolated from user worlds and other QA fixtures. */
@GameTestHolder(AOEMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class November15AppearanceSceneTests {
    private static final String MARKER = "aoemod:nov15_appearance";
    private November15AppearanceSceneTests() { }

    @GameTest(template = "plant_gameplay_empty", timeoutTicks = 600)
    public static void exportNativeYakCrabAndDesertViews(GameTestHelper helper) {
        var level = helper.getLevel();
        var origin = new BlockPos(24000, 240, 24000);
        var chunks = List.of(new ChunkPos(origin), new ChunkPos(origin.south(16)));
        var forced = new ArrayList<ChunkPos>();
        for (var chunk : chunks) if (level.setChunkForced(chunk.x, chunk.z, true)) forced.add(chunk);
        helper.startSequence().thenWaitUntil(() -> {
            for (var chunk : chunks) helper.assertTrue(level.areEntitiesLoaded(chunk.toLong()), "Wait for old appearance actors before cleanup");
        }).thenExecute(() -> {
            try {
                var area = new AABB(origin, origin.offset(16, 8, 28));
                for (var old : level.getEntitiesOfClass(Mob.class, area, mob -> mob.getPersistentData().getBoolean(MARKER))) old.discard();
                for (int x = 0; x < 16; x++) for (int z = 0; z < 28; z++) {
                    level.setBlock(origin.offset(x, 0, z), (z < 14 ? Blocks.OAK_PLANKS : Blocks.WHITE_CONCRETE).defaultBlockState(), 18);
                    for (int y = 1; y <= 6; y++) level.setBlock(origin.offset(x, y, z), Blocks.AIR.defaultBlockState(), 18);
                }
                var yak = ModEntities.YAK.get().create(level);
                yak.setTamed(true);
                yak.equipPack();
                yak.packInventory().setItem(0, new ItemStack(Items.SADDLE));
                prepare(yak, origin, 4.5, 4.5, 180, new UUID(0xA0E1515L, 1000));
                helper.assertTrue(level.addFreshEntity(yak), "Add exactly one equipped native yak");
                var donkey = EntityType.DONKEY.create(level);
                donkey.setTamed(true);
                donkey.setChest(true);
                donkey.equipSaddle(null);
                prepare(donkey, origin, 4.5, 10.5, 180, new UUID(0xA0E1515L, 1001));
                helper.assertTrue(level.addFreshEntity(donkey), "Add one native donkey for direct chest comparison");
                String[] variants = {"blue", "purple", "red"};
                var views = new ArrayList<Map<String, Object>>();
                views.add(view(level, "yak-native-donkey-panniers", origin.getX() + 11, origin.getY() + 1,
                        origin.getZ() + 7.5, 90, 15));
                for (int variant = 0; variant < variants.length; variant++) {
                    int seed = 0;
                    while (kotlin.random.RandomKt.Random((long) seed).nextInt(3) != variant) seed++;
                    var crab = (Mob) ForgeRegistries.ENTITY_TYPES.getValue(new ResourceLocation("hybrid_aquatic:fiddler_crab")).create(level);
                    prepare(crab, origin, 3.5 + variant * 4, 20.5, 0, new UUID(0xA0E1515L, seed));
                    helper.assertTrue(level.addFreshEntity(crab), "Add the actual UUID-selected " + variants[variant] + " fiddler crab");
                    views.add(view(level, "fiddler-native-" + variants[variant], origin.getX() + 3.5 + variant * 4,
                            origin.getY() + .25, origin.getZ() + 24.5, 180, 17));
                }
                helper.assertTrue(level.getEntitiesOfClass(Mob.class, area, mob -> mob.getPersistentData().getBoolean(MARKER)).size() == 5,
                        "The loaded scene must contain one yak, one donkey and exactly three native crabs");
                addDesertViews(helper, views);
                Files.writeString(Path.of("nov15-native-appearance-views.json"),
                        new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(views));
            } catch (java.io.IOException failure) {
                throw new java.io.UncheckedIOException(failure);
            } finally {
                for (var chunk : forced) level.setChunkForced(chunk.x, chunk.z, false);
            }
        }).thenSucceed();
    }

    private static void prepare(Mob mob, BlockPos origin, double x, double z, float yaw, UUID uuid) {
        mob.setUUID(uuid);
        mob.moveTo(origin.getX() + x, origin.getY() + 1, origin.getZ() + z, yaw, 0);
        mob.setYBodyRot(yaw);
        mob.setYHeadRot(yaw);
        mob.setNoAi(true); mob.setPersistenceRequired(); mob.setInvulnerable(true); mob.setSilent(true);
        mob.getPersistentData().putBoolean(MARKER, true);
    }

    private static void addDesertViews(GameTestHelper helper, List<Map<String, Object>> views) {
        var level = helper.getLevel().getServer().getLevel(GameplayDimensions.DESERT);
        var terrain = NetherRampDecorationGuard.terrain(level.getChunkSource().randomState()).orElseThrow();
        for (int cycle = 0; cycle < 4; cycle++) {
            var route = terrain.desertOutlet(cycle, 0);
            for (int distance = 20; distance < route.length(); distance++) {
                int x = (int) Math.round(route.xAt(distance)), z = (int) Math.round(route.zAt(distance));
                int top = terrain.desertUncarvedSurfaceY(x, z);
                double depth = top - route.floorAt(distance);
                if (depth < 3 || depth > 6) continue;
                double heading = route.headingAt(distance);
                for (int range : new int[]{32, 16}) {
                    double dx = range * Math.cos(heading), dz = -range * Math.sin(heading);
                    float yaw = (float) Math.toDegrees(Math.atan2(dx, -dz));
                    views.add(view(level, range == 32 ? "desert-cut-stone-natural-rim" : "desert-untouched-rim-closeup",
                            x + dx + .5, top + (range == 32 ? 23 : 8), z + dz + .5, yaw, range == 32 ? 37 : 29));
                }
                return;
            }
        }
        helper.fail("The current seeded desert needs an exposed outlet for native terrain screenshots");
    }

    private static Map<String, Object> view(net.minecraft.server.level.ServerLevel level, String name,
            double x, double y, double z, float yaw, float pitch) {
        return Map.of("label", name, "dimension", level.dimension().location().toString(),
                "x", x, "y", y, "z", z, "yaw", yaw, "pitch", pitch);
    }
}

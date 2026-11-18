package net.amurdza.examplemod.gametest;

import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.event_handlers.RetainedMobSpawns;
import net.amurdza.examplemod.registry.ModMobCategories;
import net.amurdza.examplemod.util.GameplayDimensions;
import net.amurdza.examplemod.util.RetainedContent;
import net.amurdza.examplemod.worldgen.DocumentedMonsters;
import net.amurdza.examplemod.worldgen.structure.BossLairPiece;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
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
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.registries.ForgeRegistries;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

@GameTestHolder(AOEMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class TropicalNovemberRevisionTests {
    @GameTest(template = "plant_gameplay_empty")
    public static void tropicalHostilesUseDistinctBiomesAndDrownedUseDryGround(GameTestHelper helper) {
        var expected = java.util.Map.of("rainforest", Set.of("twilightforest:slime_beetle", "minecraft:spider", "minecraft:cave_spider"),
                "mangrove_swamp", Set.of("aoemod:bogged", "minecraft:slime", "minecraft:drowned"));
        BlockPos feet = new BlockPos(8, 101, 8);
        for (var entry : expected.entrySet()) {
            var biome = helper.getLevel().registryAccess().registryOrThrow(Registries.BIOME).get(new ResourceLocation("aoemod", entry.getKey()));
            Set<String> actual = new java.util.HashSet<>();
            biome.getMobSettings().getMobs(MobCategory.MONSTER).unwrap().forEach(spawn -> actual.add(ForgeRegistries.ENTITY_TYPES.getKey(spawn.type).toString()));
            helper.assertTrue(actual.equals(entry.getValue()), "Loaded hostile roster differs in " + entry.getKey() + ": " + actual);
            WorldGenLevel dry = dark(AquaticPlantGenerationTests.fixture(helper, entry.getKey(), new HashMap<>(), 0));
            for (String id : entry.getValue()) {
                var type = ForgeRegistries.ENTITY_TYPES.getValue(new ResourceLocation(id));
                helper.assertTrue(DocumentedMonsters.canSpawn(dry, feet, type), "Dark dry floor must allow " + id);
            }
        }
        for (int depth : new int[]{0, 3}) {
            var world = dark(AquaticPlantGenerationTests.fixture(helper, "mangrove_swamp", new HashMap<>(), depth));
            helper.assertTrue(NaturalSpawner.isSpawnPositionOk(SpawnPlacements.Type.IN_WATER, world, feet, EntityType.DROWNED) == (depth == 0),
                    "The actual native drowned placement gate must require land");
            helper.assertTrue(SpawnPlacements.checkSpawnRules(EntityType.DROWNED, world, MobSpawnType.NATURAL, feet, RandomSource.create(71)) == (depth == 0),
                    "Forge's actual placement rule must agree with the land-only drowned gate");
        }
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void fishSchoolsChargeIndependentExpandedNativeCaps(GameTestHelper helper) throws Exception {
        var level = helper.getLevel().getServer().getLevel(GameplayDimensions.TROPICAL);
        var chunk = level.getChunkAt(BlockPos.ZERO);
        var player = new net.minecraftforge.common.util.FakePlayer(level,
                new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), "FishPools"));
        player.setPos(0, 0, 0);
        level.players().add(player);
        List<Mob> mobs = new ArrayList<>();
        try {
            for (String id : List.of("minecraft:tropical_fish", "minecraft:pufferfish", "hybrid_aquatic:coconut_crab",
                    "hybrid_aquatic:lightfoot_crab", "angling:anomalocaris", "hybrid_aquatic:umbrella_octopus",
                    "hybrid_aquatic:starfish", "angling:sea_slug", "minecraft:dolphin")) {
                Mob mob = (Mob) ForgeRegistries.ENTITY_TYPES.getValue(new ResourceLocation(id)).create(level);
                mob.setPos(28, 0, 0);
                mobs.add(mob);
            }
            var state = NaturalSpawner.createState(289, new ArrayList<net.minecraft.world.entity.Entity>(mobs), (key, consumer) -> consumer.accept(chunk),
                    new LocalMobCapCalculator(level.getChunkSource().chunkMap));
            var counts = state.getMobCategoryCounts();
            helper.assertTrue(counts.getInt(ModMobCategories.COMMON_AQUATIC) == 8
                            && counts.getInt(MobCategory.WATER_AMBIENT) == 0 && counts.getInt(MobCategory.WATER_CREATURE) == 1,
                    "Fish, bottom creatures, and large animals must occupy separate common and rare live counters");
            helper.assertTrue(ModMobCategories.COMMON_AQUATIC.getMaxInstancesPerChunk() == 2 * MobCategory.WATER_AMBIENT.getMaxInstancesPerChunk()
                            && !ModMobCategories.COMMON_AQUATIC.isPersistent(),
                    "Expanded fish caps must retain the frequent ambient spawning cadence");
            var after = Arrays.stream(NaturalSpawner.SpawnState.class.getDeclaredMethods()).filter(method ->
                    Arrays.equals(method.getParameterTypes(), new Class<?>[]{Mob.class, ChunkAccess.class})).findFirst().orElseThrow();
            after.setAccessible(true);
            after.invoke(state, mobs.get(0), chunk);
            after.invoke(state, mobs.get(3), chunk);
            helper.assertTrue(counts.getInt(ModMobCategories.COMMON_AQUATIC) == 10
                            && counts.getInt(MobCategory.WATER_CREATURE) == 1,
                    "New fish must charge the same separate caps immediately after spawning");
        } finally {
            mobs.forEach(Mob::discard);
            level.players().remove(player);
            player.discard();
        }
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void removedSeaSerpentCannotJoinOrAppearAsASpawnEgg(GameTestHelper helper) {
        var type = ForgeRegistries.ENTITY_TYPES.getValue(new ResourceLocation("aoemod:sea_serpent"));
        var serpent = type.create(helper.getLevel());
        var join = new EntityJoinLevelEvent(serpent, helper.getLevel());
        RetainedMobSpawns.joined(join);
        helper.assertTrue(join.isCanceled() && !RetainedContent.isKeptEntity(type), "Sea serpent must be excluded from actual entity joins");
        var egg = net.amurdza.examplemod.registry.ModEntities.SEA_SERPENT_SPAWN_EGG.get();
        helper.assertTrue(egg != null && !RetainedContent.isKeptItem(new net.minecraft.world.item.ItemStack(egg)), "Its spawn egg must also leave creative inventory");
        var tab = net.amurdza.examplemod.registry.ModCreativeTabs.AOE_MOD.get();
        tab.buildContents(new net.minecraft.world.item.CreativeModeTab.ItemDisplayParameters(helper.getLevel().enabledFeatures(), true, helper.getLevel().registryAccess()));
        helper.assertTrue(tab.getDisplayItems().stream().noneMatch(stack -> stack.is(egg)), "The built AOE creative tab must omit the actual registered Forge spawn egg");
        serpent.discard();
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void spiderEntrancesSealBothSlitsAndBossRoomEdgesAreStraight(GameTestHelper helper) throws Exception {
        var hill = BossLairPiece.class.getDeclaredMethod("placeSpiderHillColumn", WorldGenLevel.class, BoundingBox.class, int.class, int.class);
        var branch = BossLairPiece.class.getDeclaredMethod("placeBranchColumn", WorldGenLevel.class, BoundingBox.class, int.class, int.class);
        hill.setAccessible(true); branch.setAccessible(true);
        BlockPos center = new BlockPos(0, 150, 0);
        for (int seed = 0; seed < 4; seed++) {
            var piece = new BossLairPiece("spider", center, seed);
            var states = new HashMap<BlockPos, BlockState>();
            var world = AquaticPlantGenerationTests.fixture(helper, "rainforest", states, 0);
            int sx = (seed & 1) == 0 ? 1 : -1, sz = (seed & 2) == 0 ? 1 : -1;
            for (int[] slit : new int[][]{{57, 49}, {49, 57}}) {
                int x = slit[0] * sx, z = slit[1] * sz;
                hill.invoke(piece, world, piece.getBoundingBox(), x, z);
                branch.invoke(piece, world, piece.getBoundingBox(), x, z);
                for (int y = 1; y <= 8; y++) helper.assertTrue(world.getBlockState(center.offset(x, y, z)).is(Blocks.MOSS_BLOCK),
                        "Both exterior air slits must be filled through their full height in every orientation");
            }
            hill.invoke(piece, world, piece.getBoundingBox(), 54 * sx, 54 * sz);
            branch.invoke(piece, world, piece.getBoundingBox(), 54 * sx, 54 * sz);
            for (int y = 1; y <= 6; y++) helper.assertTrue(world.getBlockState(center.offset(54 * sx, y, 54 * sz)).isAir(),
                    "Sealing exterior slits must preserve the entrance passage");
            for (int offset = -1; offset <= 1; offset++) for (int[] edge : new int[][]{{-41, -28 + offset}, {-15, -28 + offset}, {-28 + offset, -41}, {-28 + offset, -15}}) {
                int x = edge[0] * sx, z = edge[1] * sz;
                branch.invoke(piece, world, piece.getBoundingBox(), x, z);
                helper.assertTrue(world.getBlockState(center.offset(x, -30, z)).is(Blocks.MOSS_BLOCK)
                                && world.getBlockState(center.offset(x, -31, z)).isAir(),
                        "Each of the twelve top-edge blocks must become moss while retaining the walking space below");
            }
        }
        helper.succeed();
    }

    private static WorldGenLevel dark(WorldGenLevel base) {
        return (WorldGenLevel) Proxy.newProxyInstance(TropicalNovemberRevisionTests.class.getClassLoader(), new Class<?>[]{WorldGenLevel.class},
                (proxy, method, args) -> {
                    if (Set.of("getBrightness", "getMaxLocalRawBrightness", "getRawBrightness").contains(method.getName())) return 0;
                    if (method.getName().equals("canSeeSky")) return false;
                    try { return method.invoke(base, args); }
                    catch (InvocationTargetException error) { throw error.getCause(); }
                });
    }
}

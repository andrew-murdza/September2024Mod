package net.amurdza.examplemod.gametest;

import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.event_handlers.DocumentedNaturalSpawns;
import net.amurdza.examplemod.util.GameplayDimensions;
import net.amurdza.examplemod.worldgen.DocumentedMonsters;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.level.NaturalSpawner;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.entity.living.MobSpawnEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.registries.ForgeRegistries;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@GameTestHolder(AOEMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class RainforestMonsterRevisionTests {
    private static final List<String> LOWLAND = List.of("aoemod:bogged", "minecraft:slime", "minecraft:drowned");
    private static final List<String> UPLAND = List.of("minecraft:spider", "minecraft:cave_spider", "twilightforest:slime_beetle");

    @GameTest(template = "plant_gameplay_empty")
    public static void rainforestHostilesRespectGroundHeightAndPeacefulCherryBiome(GameTestHelper helper) {
        for (int groundY : new int[]{62, 63, 64, 65, 66, 67, 69, 96}) {
            BlockPos feet = new BlockPos(8, groundY + 1, 8);
            var rainforest = fixture(helper, "rainforest", groundY, new HashMap<>());
            var cherry = fixture(helper, "cherry_grove", groundY, new HashMap<>());
            for (String id : DocumentedMonsters.forBiome("rainforest")) {
                EntityType<?> type = ForgeRegistries.ENTITY_TYPES.getValue(new ResourceLocation(id));
                helper.assertTrue(type != null, "Missing retained rainforest monster: " + id);
                boolean expected = LOWLAND.contains(id) ? groundY == 65 || groundY == 66 : groundY > 66;
                helper.assertTrue(DocumentedMonsters.canSpawn(rainforest, feet, type) == expected,
                        id + " has the wrong natural spawn decision above ground y=" + groundY);
                for (MobSpawnType reason : new MobSpawnType[]{MobSpawnType.NATURAL, MobSpawnType.CHUNK_GENERATION}) {
                    var placement = new MobSpawnEvent.SpawnPlacementCheck(type, rainforest, reason, feet,
                            RandomSource.create(1), !expected);
                    DocumentedNaturalSpawns.placement(placement);
                    helper.assertTrue(placement.getResult() == (expected ? Event.Result.ALLOW : Event.Result.DENY),
                            "The actual placement event must enforce the rainforest altitude split: " + id);
                }
                helper.assertFalse(DocumentedMonsters.canSpawn(cherry, feet, type),
                        "Cherry grove must be peaceful at every elevation: " + id);
            }
        }
        helper.assertTrue(DocumentedMonsters.forBiome("rainforest").size() == LOWLAND.size() + UPLAND.size()
                        && DocumentedMonsters.forBiome("rainforest").containsAll(UPLAND),
                "Rainforest must include all six species, with a slime beetle replacing the centipede");
        var cave = helper.getLevel().registryAccess().registryOrThrow(Registries.STRUCTURE)
                .get(new ResourceLocation(AOEMod.MOD_ID, "spider_cave"));
        helper.assertTrue(cave != null && cave.spawnOverrides().get(MobCategory.MONSTER).spawns().unwrap().stream()
                        .map(entry -> String.valueOf(ForgeRegistries.ENTITY_TYPES.getKey(entry.type)))
                        .collect(java.util.stream.Collectors.toSet()).equals(java.util.Set.copyOf(UPLAND)),
                "The spider-cave structure must retain the complete upland trio, including slime beetles");
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void drownedCanSpawnOnLowlandGroundWithoutBypassingSurfaceSafety(GameTestHelper helper) {
        BlockPos feet = new BlockPos(8, 67, 8);
        Map<BlockPos, BlockState> blocks = new HashMap<>();
        var rainforest = fixture(helper, "rainforest", 66, blocks);
        helper.assertTrue(NaturalSpawner.isSpawnPositionOk(SpawnPlacements.Type.IN_WATER,
                        rainforest, feet, EntityType.DROWNED),
                "NaturalSpawner must allow the documented dry-land drowned before its placement event");
        for (BlockState obstruction : new BlockState[]{Blocks.WATER.defaultBlockState(), Blocks.STONE.defaultBlockState()}) {
            blocks.put(feet, obstruction);
            helper.assertFalse(NaturalSpawner.isSpawnPositionOk(SpawnPlacements.Type.IN_WATER,
                            rainforest, feet, EntityType.DROWNED),
                    "The land override must still reject submerged or obstructed spawn positions");
        }
        blocks.remove(feet);
        for (BlockState support : new BlockState[]{Blocks.AIR.defaultBlockState(), Blocks.RED_MUSHROOM_BLOCK.defaultBlockState()}) {
            blocks.put(feet.below(), support);
            helper.assertFalse(NaturalSpawner.isSpawnPositionOk(SpawnPlacements.Type.IN_WATER,
                            rainforest, feet, EntityType.DROWNED),
                    "The land override must reject unsupported positions and giant mushroom canopy");
        }
        helper.succeed();
    }

    private static WorldGenLevel fixture(GameTestHelper helper, String biomeName, int groundY,
                                         Map<BlockPos, BlockState> blocks) {
        var world = helper.getLevel().getServer().getLevel(GameplayDimensions.TROPICAL);
        var biome = world.registryAccess().registryOrThrow(Registries.BIOME).getHolderOrThrow(
                ResourceKey.create(Registries.BIOME, new ResourceLocation(AOEMod.MOD_ID, biomeName)));
        java.util.function.Function<BlockPos, BlockState> stateAt = pos -> blocks.getOrDefault(pos,
                (pos.getY() <= groundY ? Blocks.MOSS_BLOCK : Blocks.AIR).defaultBlockState());
        return (WorldGenLevel) Proxy.newProxyInstance(RainforestMonsterRevisionTests.class.getClassLoader(),
                new Class<?>[]{WorldGenLevel.class}, (proxy, method, args) -> {
                    switch (method.getName()) {
                        case "getLevel": return world;
                        case "getBiome": return biome;
                        case "getDifficulty": return Difficulty.NORMAL;
                        case "getBrightness", "getMaxLocalRawBrightness": return 0;
                        case "getBlockState": return stateAt.apply((BlockPos) args[0]);
                        case "getFluidState": return stateAt.apply((BlockPos) args[0]).getFluidState();
                        default:
                            try { return method.invoke(world, args); }
                            catch (InvocationTargetException failure) { throw failure.getCause(); }
                    }
                });
    }
}

package net.amurdza.examplemod.gametest;

import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.event_handlers.DocumentedNaturalSpawns;
import net.amurdza.examplemod.util.GameplayDimensions;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.entity.living.MobSpawnEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.List;

@GameTestHolder(AOEMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class DesertShulkerSpawnTests {
    @GameTest(template = "plant_gameplay_empty")
    public static void shulkersUseTerracottaHeightThroughPlacementEvents(GameTestHelper helper) {
        var desert = helper.getLevel().getServer().getLevel(GameplayDimensions.DESERT);
        var biome = desert.registryAccess().registryOrThrow(Registries.BIOME).getHolderOrThrow(
                ResourceKey.create(Registries.BIOME, AOEMod.makeID("desert")));
        var blocks = new HashMap<BlockPos, BlockState>();
        var world = (ServerLevelAccessor) Proxy.newProxyInstance(DesertShulkerSpawnTests.class.getClassLoader(),
                new Class<?>[]{ServerLevelAccessor.class}, (proxy, method, args) -> {
                    return switch (method.getName()) {
                        case "getLevel" -> desert;
                        case "getBiome" -> biome;
                        case "getDifficulty" -> Difficulty.NORMAL;
                        case "getBrightness", "getMaxLocalRawBrightness" -> 0;
                        case "getBlockState" -> blocks.getOrDefault(args[0], Blocks.AIR.defaultBlockState());
                        case "getFluidState" -> blocks.getOrDefault(args[0], Blocks.AIR.defaultBlockState()).getFluidState();
                        case "noCollision" -> true;
                        default -> {
                            try { yield method.invoke(desert, args); }
                            catch (InvocationTargetException failure) { throw failure.getCause(); }
                        }
                    };
                });
        record Candidate(int y, Block floor, boolean terracottaWall, boolean allowed) { }
        for (var candidate : List.of(new Candidate(62, Blocks.TERRACOTTA, true, false),
                new Candidate(63, Blocks.RED_SAND, false, false),
                new Candidate(63, Blocks.RED_SAND, true, true),
                new Candidate(64, Blocks.TERRACOTTA, false, true))) {
            BlockPos feet = new BlockPos(8, candidate.y(), 8);
            blocks.clear();
            blocks.put(feet.below(), candidate.floor().defaultBlockState());
            if (candidate.terracottaWall()) blocks.put(feet.east(), Blocks.TERRACOTTA.defaultBlockState());
            for (var reason : List.of(MobSpawnType.NATURAL, MobSpawnType.CHUNK_GENERATION)) {
                helper.assertTrue(SpawnPlacements.checkSpawnRules(EntityType.SHULKER, world, reason, feet,
                                RandomSource.create(19)) == candidate.allowed(),
                        "Registered rules and the final regional placement event disagree at " + candidate);
                var event = new MobSpawnEvent.SpawnPlacementCheck(EntityType.SHULKER, world, reason, feet,
                        RandomSource.create(19), true);
                event.setResult(Event.Result.ALLOW);
                DocumentedNaturalSpawns.placement(event);
                helper.assertTrue(event.getResult() == (candidate.allowed() ? Event.Result.ALLOW : Event.Result.DENY),
                        "An earlier ALLOW must not bypass the terracotta height rule");
                var shulker = EntityType.SHULKER.create(desert);
                shulker.moveTo(feet.getX(), feet.getY(), feet.getZ());
                var position = new MobSpawnEvent.PositionCheck(shulker, world, reason, null);
                DocumentedNaturalSpawns.position(position);
                helper.assertTrue(position.getResult() == (candidate.allowed() ? Event.Result.ALLOW : Event.Result.DENY),
                        "The final position check must use the same shulker habitat");
                shulker.discard();
            }
        }
        helper.succeed();
    }
}

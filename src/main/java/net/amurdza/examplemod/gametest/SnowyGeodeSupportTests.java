package net.amurdza.examplemod.gametest;

import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.util.GameplayDimensions;
import net.amurdza.examplemod.worldgen.SnowyCaveColumns;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;

@GameTestHolder(AOEMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class SnowyGeodeSupportTests {
    @GameTest(template = "plant_gameplay_empty", timeoutTicks = 800)
    public static void actualGeodeChambersPreserveOnlySnowySolidCores(GameTestHelper helper) {
        var snowy = helper.getLevel().getServer().getLevel(GameplayDimensions.SNOWY);
        var supports = SnowyCaveColumns.supports(snowy.getChunkSource().randomState());
        var geode = snowy.registryAccess().registryOrThrow(Registries.PLACED_FEATURE)
                .get(AOEMod.makeID("snowy/amethyst_geode")).feature().value();
        BlockPos center = null;
        search: for (int x = -80; x < 80; x++) for (int z = -80; z < 80; z++) {
            if (supports.clearance(x, z) < -1.5) { center = new BlockPos(x, -60, z); break search; }
        }
        helper.assertTrue(center != null, "Find a real seeded support core for the geode intersection");
        Map<BlockPos, BlockState> ordinary = new HashMap<>();
        BlockPos origin = null;
        search: for (int dx = -10; dx <= 0; dx += 2) for (int dz = -10; dz <= 0; dz += 2) {
            ordinary.clear();
            BlockPos candidate = center.offset(dx, 0, dz);
            if (!geode.place(host(helper.getLevel(), snowy.getSeed(), ordinary), snowy.getChunkSource().getGenerator(),
                    RandomSource.create(7), candidate)) continue;
            long intersections = ordinary.entrySet().stream().filter(e -> e.getValue().isAir()
                    && supports.clearance(e.getKey().getX(), e.getKey().getZ()) <= 0).count();
            if (intersections >= 8) { origin = candidate; break search; }
        }
        helper.assertTrue(origin != null, "The unguarded other-dimension control must actually carve through the core");
        Map<BlockPos, BlockState> protectedBlocks = new HashMap<>();
        helper.assertTrue(geode.place(host(snowy, snowy.getSeed(), protectedBlocks), snowy.getChunkSource().getGenerator(),
                RandomSource.create(7), origin), "The intersecting Snowy geode must still generate");
        int protectedAirWrites = 0, openOutside = 0;
        for (var entry : ordinary.entrySet()) {
            if (!entry.getValue().isAir()) continue;
            BlockPos pos = entry.getKey();
            BlockState actual = protectedBlocks.getOrDefault(pos, Blocks.STONE.defaultBlockState());
            if (supports.clearance(pos.getX(), pos.getZ()) <= 0) {
                helper.assertTrue(actual.is(Blocks.STONE), "Geode AIR writes must preserve the existing solid core: " + pos);
                protectedAirWrites++;
            } else if (actual.isAir()) openOutside++;
        }
        helper.assertTrue(protectedAirWrites >= 8 && openOutside > 20,
                "Protect the intersecting core while the surrounding geode chamber remains open");
        for (var block : new net.minecraft.world.level.block.Block[]{Blocks.SMOOTH_BASALT, Blocks.CALCITE,
                Blocks.AMETHYST_BLOCK, Blocks.BUDDING_AMETHYST})
            helper.assertTrue(protectedBlocks.values().stream().anyMatch(s -> s.is(block)),
                    "The guard must preserve normal geode shells and crystal sources: " + block);
        AOEMod.LOGGER.info("Snowy geode core guard: origin={} protectedAirWrites={} unchangedOutsideAir={}",
                origin, protectedAirWrites, openOutside);
        helper.succeed();
    }

    @SuppressWarnings("unchecked")
    private static WorldGenLevel host(ServerLevel backing, long seed, Map<BlockPos, BlockState> blocks) {
        return (WorldGenLevel) Proxy.newProxyInstance(SnowyGeodeSupportTests.class.getClassLoader(),
                new Class<?>[]{WorldGenLevel.class}, (proxy, method, args) -> {
                    if (method.getName().equals("getLevel")) return backing;
                    if (method.getName().equals("getSeed")) return seed;
                    if (method.getName().equals("getBlockState")) return blocks.getOrDefault(args[0], Blocks.STONE.defaultBlockState());
                    if (method.getName().equals("getFluidState")) return blocks.getOrDefault(args[0], Blocks.STONE.defaultBlockState()).getFluidState();
                    if (method.getName().equals("getBlockEntity")) return null;
                    if (method.getName().equals("setBlock")) {
                        blocks.put(((BlockPos) args[0]).immutable(), (BlockState) args[1]);
                        return true;
                    }
                    if (method.getName().equals("ensureCanWrite")) return true;
                    if (method.getName().equals("isStateAtPosition")) return ((Predicate<BlockState>) args[1])
                            .test(blocks.getOrDefault(args[0], Blocks.STONE.defaultBlockState()));
                    try { return method.invoke(backing, args); }
                    catch (InvocationTargetException failure) { throw failure.getCause(); }
                });
    }
}

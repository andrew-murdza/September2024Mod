package net.amurdza.examplemod.gametest;

import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.util.GameplayDimensions;
import net.amurdza.examplemod.worldgen.structure.BossLairPiece;
import net.amurdza.examplemod.worldgen.structure.BossLairStructure;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.pieces.PiecesContainer;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.registries.ForgeRegistries;
import twilightforest.world.components.structures.YetiCaveComponent;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

@GameTestHolder(AOEMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class SnowyTreeAndYetiFollowupTests {
    private SnowyTreeAndYetiFollowupTests() { }

    @GameTest(template = "plant_gameplay_empty", timeoutTicks = 800)
    public static void completeSpruceCrownsCarrySnowAcrossEveryChunkEdge(GameTestHelper helper) {
        int crossingColumns = 0, snowyLeavesAcrossDappledTrunk = 0, dryLeavesAboveDappledTrunk = 0;
        for (String habitat : List.of("snowy_forest", "dappled_forest", "mixed")) {
            for (String feature : List.of("minecraft:spruce", "minecraft:mega_spruce")) {
                for (long seed = 0; seed < 12; seed++) {
                    Map<BlockPos, BlockState> blocks = new HashMap<>();
                    BlockPos origin = new BlockPos(15, 101, 15);
                    for (BlockPos soil : BlockPos.betweenClosed(origin.offset(-8, -1, -8), origin.offset(8, -1, 8)))
                        blocks.put(soil.immutable(), Blocks.GRASS_BLOCK.defaultBlockState());
                    boolean mixed = habitat.equals("mixed");
                    boolean[] showBoundary = {false};
                    var base = TropicalTreeShapeTests.fixture(helper, blocks, mixed ? "dappled_forest" : habitat, 0, false);
                    var snowy = helper.getLevel().getServer().getLevel(GameplayDimensions.SNOWY);
                    WorldGenLevel world = (WorldGenLevel) Proxy.newProxyInstance(getLoader(), new Class<?>[]{WorldGenLevel.class},
                            (proxy, method, args) -> {
                                if (method.getName().equals("getLevel")) return snowy;
                                if (method.getName().equals("getBlockEntity")) return null;
                                if (showBoundary[0] && method.getName().equals("getBiome") && ((BlockPos) args[0]).getX() >= 16) {
                                    return snowy.registryAccess().registryOrThrow(Registries.BIOME).getHolderOrThrow(
                                            net.minecraft.resources.ResourceKey.create(Registries.BIOME, AOEMod.makeID("snowy_forest")));
                                }
                                try { return method.invoke(base, args); }
                                catch (InvocationTargetException failure) { throw failure.getCause(); }
                            });
                    var configured = world.registryAccess().registryOrThrow(Registries.CONFIGURED_FEATURE).get(new ResourceLocation(feature));
                    helper.assertTrue(configured.place(world, snowy.getChunkSource().getGenerator(), RandomSource.create(seed), origin),
                            "A complete border tree must generate on clear " + habitat + " soil: " + feature + ", " + seed);
                    if (mixed) {
                        // A new spruce rejects a horizontally split planting area. Use
                        // the same real, complete crown to test decoration at a boundary
                        // (for example an existing crown after terrain habitats change).
                        showBoundary[0] = true;
                        var crown = new net.amurdza.examplemod.worldgen.GrowthPlacementBuffer(world);
                        blocks.forEach((pos, state) -> {
                            if (state.is(BlockTags.LEAVES)) crown.setBlock(pos, state, 2);
                        });
                        net.amurdza.examplemod.worldgen.SnowyCanopySnow.cover(crown);
                        helper.assertTrue(crown.commit(), "Commit complete column-specific crown snow");
                    }
                    Map<Long, BlockPos> tops = new HashMap<>();
                    blocks.forEach((pos, state) -> {
                        if (!state.is(BlockTags.LEAVES)) return;
                        long column = ((long) pos.getX() << 32) ^ (pos.getZ() & 0xffffffffL);
                        tops.merge(column, pos, (a, b) -> a.getY() > b.getY() ? a : b);
                    });
                    helper.assertTrue(tops.size() >= 12, "Audit an actual complete crown");
                    for (BlockPos top : tops.values()) {
                        boolean snowyTop = world.getBlockState(top.above()).is(Blocks.SNOW);
                        helper.assertTrue(snowyTop == (habitat.equals("snowy_forest") || mixed && top.getX() >= 16),
                                "Snow coverage must follow the whole crown and remain scoped to snowy taiga: " + top);
                        if (habitat.equals("snowy_forest") && (top.getX() >= 16 || top.getZ() >= 16)) crossingColumns++;
                        if (mixed && snowyTop) snowyLeavesAcrossDappledTrunk++;
                        if (mixed && !snowyTop) dryLeavesAboveDappledTrunk++;
                    }
                }
            }
        }
        helper.assertTrue(crossingColumns >= 100, "The audit must cover crowns in already-decorated neighboring chunks");
        helper.assertTrue(snowyLeavesAcrossDappledTrunk > 50 && dryLeavesAboveDappledTrunk > 50,
                "A tree rooted at dappled X15 must have snow only on the crown columns crossing into snowy X16 and beyond");
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty", timeoutTicks = 1200)
    @SuppressWarnings("unchecked")
    public static void reloadedYetiCaveHasSnowExteriorBlueIceInteriorAndSupportedCorners(GameTestHelper helper) {
        var level = helper.getLevel().getServer().getLevel(GameplayDimensions.SNOWY);
        BlockPos center = new BlockPos(8, 100, 8);
        Map<BlockPos, BlockState> blocks = new HashMap<>();
        java.util.function.ToIntBiFunction<Integer, Integer> ground = (x, z) -> 82
                - Math.abs(x - center.getX()) / 8 - Math.abs(z - center.getZ()) / 8;
        java.util.function.Function<BlockPos, BlockState> state = pos -> blocks.getOrDefault(pos,
                pos.getY() <= ground.applyAsInt(pos.getX(), pos.getZ()) ? Blocks.STONE.defaultBlockState() : Blocks.AIR.defaultBlockState());
        blocks.put(center.below(6), Blocks.SPRUCE_LEAVES.defaultBlockState());
        blocks.put(center.below(7), Blocks.SNOW.defaultBlockState());
        blocks.put(center.below(8), Blocks.WATER.defaultBlockState());
        BoundingBox[] active = {null};
        WorldGenLevel world = (WorldGenLevel) Proxy.newProxyInstance(getLoader(), new Class<?>[]{WorldGenLevel.class},
                (proxy, method, args) -> {
                    switch (method.getName()) {
                        case "getLevel": return level;
                        case "getHeight": return args == null || args.length == 0 ? level.getHeight() : ground.applyAsInt((Integer) args[1], (Integer) args[2]) + 1;
                        case "getBlockState": return state.apply((BlockPos) args[0]);
                        case "getFluidState": return state.apply((BlockPos) args[0]).getFluidState();
                        case "isEmptyBlock": return state.apply((BlockPos) args[0]).isAir();
                        case "isStateAtPosition": return ((Predicate<BlockState>) args[1]).test(state.apply((BlockPos) args[0]));
                        case "isFluidAtPosition": return ((Predicate<net.minecraft.world.level.material.FluidState>) args[1]).test(state.apply((BlockPos) args[0]).getFluidState());
                        case "setBlock": {
                            BlockPos pos = ((BlockPos) args[0]).immutable();
                            helper.assertTrue(active[0].isInside(pos), "Every lair write must stay within its currently generating chunk: " + pos);
                            blocks.put(pos, (BlockState) args[1]);
                            return true;
                        }
                        case "addFreshEntityWithPassengers": return null;
                        case "addFreshEntity": return true;
                    }
                    try { return method.invoke(level, args); }
                    catch (InvocationTargetException failure) { throw failure.getCause(); }
                });
        var serialization = StructurePieceSerializationContext.fromLevel(level);
        var envelope = new BossLairPiece("yeti", center, 98231);
        envelope = new BossLairPiece(serialization, envelope.createTag(serialization));
        var nativeCave = new YetiCaveComponent(0, center.getX(), center.getY() + 5, center.getZ());
        var pieces = new PiecesContainer(List.of(envelope, nativeCave));
        var structure = (BossLairStructure) level.registryAccess().registryOrThrow(Registries.STRUCTURE)
                .get(new ResourceLocation("aoemod:yeti_cave"));
        for (int cx = Math.floorDiv(center.getX() - 44, 16); cx <= Math.floorDiv(center.getX() + 44, 16); cx++) {
            for (int cz = Math.floorDiv(center.getZ() - 44, 16); cz <= Math.floorDiv(center.getZ() + 44, 16); cz++) {
                var chunk = new ChunkPos(cx, cz);
                var clip = new BoundingBox(chunk.getMinBlockX(), level.getMinBuildHeight(), chunk.getMinBlockZ(),
                        chunk.getMaxBlockX(), level.getMaxBuildHeight() - 1, chunk.getMaxBlockZ());
                active[0] = clip;
                envelope.postProcess(world, level.structureManager(), level.getChunkSource().getGenerator(), RandomSource.create(17), clip, chunk, center);
                nativeCave.postProcess(world, level.structureManager(), level.getChunkSource().getGenerator(), RandomSource.create(17), clip, chunk, center);
                structure.afterPlace(world, level.structureManager(), level.getChunkSource().getGenerator(), RandomSource.create(17), clip, chunk, pieces);
            }
        }
        Map<Long, Integer> shellTops = new HashMap<>();
        blocks.forEach((pos, placed) -> {
            if (placed.is(Blocks.SNOW_BLOCK) || placed.is(Blocks.PACKED_ICE) || placed.is(Blocks.ICE) || placed.is(Blocks.BLUE_ICE)) {
                long column = ((long) pos.getX() << 32) ^ (pos.getZ() & 0xffffffffL);
                shellTops.merge(column, pos.getY(), Math::max);
            }
        });
        int supported = 0;
        for (int dx = -44; dx <= 44; dx++) for (int dz = -44; dz <= 44; dz++) {
            int groundY = ground.applyAsInt(center.getX() + dx, center.getZ() + dz);
            long column = ((long) (center.getX() + dx) << 32) ^ ((center.getZ() + dz) & 0xffffffffL);
            int solidFloor = Math.min(center.getY() - 3, shellTops.getOrDefault(column, groundY));
            for (int y = groundY + 1; y <= solidFloor; y++) {
                var at = new BlockPos(center.getX() + dx, y, center.getZ() + dz);
                helper.assertTrue(!state.apply(at).isAir() && state.apply(at).getFluidState().isEmpty(),
                        "The complete icy apron must reach real ground without a floating gap: " + at);
                supported++;
            }
        }
        var spawner = ForgeRegistries.BLOCKS.getValue(new ResourceLocation("twilightforest:alpha_yeti_boss_spawner"));
        helper.assertTrue(blocks.values().stream().filter(block -> block.is(spawner)).count() == 1,
                "Retain exactly one native Alpha Yeti boss spawner");
        for (var entry : blocks.entrySet()) {
            var pos = entry.getKey();
            var placed = entry.getValue();
            helper.assertTrue(placed.isAir() || placed.is(Blocks.SNOW_BLOCK) || placed.is(Blocks.BLUE_ICE) || placed.is(Blocks.CHEST) || placed.is(spawner),
                    "Yeti shell, foundations and native stalactites must contain only snow and blue ice: " + pos + "=" + placed);
            if (Math.max(Math.abs(pos.getX() - center.getX()), Math.abs(pos.getZ() - center.getZ())) > 25) {
                boolean icyWalkingSurface = Math.min(Math.abs(pos.getX() - center.getX()), Math.abs(pos.getZ() - center.getZ())) <= 7
                         && Math.abs(pos.getY() - center.getY()) <= 2
                        && state.apply(pos.above()).isAir();
                helper.assertTrue(placed.isAir() || placed.is(Blocks.SNOW_BLOCK) || icyWalkingSurface && placed.is(Blocks.BLUE_ICE),
                        "The outside hill and every vertical step must be snow: " + pos + "=" + placed);
            }
        }
        for (int dx : new int[]{-24, 24}) for (int dz : new int[]{-24, 24}) {
            for (int dy : new int[]{4, 12}) helper.assertTrue(state.apply(center.offset(dx, dy, dz)).isAir(),
                    "All four room corners must be clear of the raised floor and hanging ceiling cubes");
        }
        helper.assertTrue(supported > 10000, "Exercise foundations much deeper than the previous three-block slab");
        helper.succeed();
    }

    private static ClassLoader getLoader() { return SnowyTreeAndYetiFollowupTests.class.getClassLoader(); }
}

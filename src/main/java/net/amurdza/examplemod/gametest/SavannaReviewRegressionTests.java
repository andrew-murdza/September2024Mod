package net.amurdza.examplemod.gametest;

import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.registry.ModEntities;
import net.amurdza.examplemod.worldgen.SavannaCampColumns;
import net.amurdza.examplemod.worldgen.SavannaCampVegetation;
import net.amurdza.examplemod.worldgen.structure.*;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.BitSet;
import java.util.List;
import java.util.UUID;

@GameTestHolder(AOEMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class SavannaReviewRegressionTests {
    @GameTest(template = "plant_gameplay_empty")
    public static void capturedSaddledHorsemenSurviveDistanceAndPeacefulAfterReload(GameTestHelper h) throws Exception {
        for (var type : List.of(ModEntities.SKELETON_HORSEMAN.get(), ModEntities.ZOMBIE_HORSEMAN.get())) {
            var horse = type.create(h.getLevel());
            var peaceful = horse.getClass().getDeclaredMethod("shouldDespawnInPeaceful");
            peaceful.setAccessible(true);
            h.assertTrue(horse.removeWhenFarAway(20000) && (boolean) peaceful.invoke(horse), "Unclaimed wild horsemen must remain transient");
            horse.equipSaddle(net.minecraft.sounds.SoundSource.PLAYERS);
            h.assertTrue(horse.getOwnerUUID() == null, "Saddling an already-tame horse does not assign a vanilla owner");
            var tag = new CompoundTag();
            horse.saveWithoutId(tag);
            var loaded = type.create(h.getLevel());
            loaded.load(tag);
            h.assertTrue(loaded.isSaddled() && !loaded.removeWhenFarAway(20000) && !(boolean) peaceful.invoke(loaded),
                    "A player-captured saddle must protect the saved mount from distance and peaceful despawn");
            loaded.setOwnerUUID(UUID.randomUUID());
            var ownedTag = new CompoundTag();
            loaded.saveWithoutId(ownedTag);
            horse.load(ownedTag);
            h.assertTrue(horse.getOwnerUUID().equals(loaded.getOwnerUUID()) && !horse.removeWhenFarAway(20000)
                    && !(boolean) peaceful.invoke(horse), "Explicit ownership must also survive reload");
        }
        h.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void replacedCampReservationsReleaseOldLawnsAndRetainUntouchedNeighbors(GameTestHelper h) throws Exception {
        var level = h.getLevel();
        var center = new BlockPos(8, 100, 8);
        var square = new SavannaOrcCampPiece(level.getStructureManager(), center);
        var serial = StructurePieceSerializationContext.fromLevel(level);
        var oldTag = square.createTag(serial);
        oldTag.putInt("SettlementVersion", 7);
        oldTag.putInt("TPX", center.getX() - 107);
        oldTag.putInt("TPZ", center.getZ() - 107);
        var old = new SavannaOrcCampPiece(serial, oldTag);
        // Supply the real completed-plan geometry without placing 650,000 template
        // cells; the full camp fixture exercises the production postProcess ordering.
        var plan = SavannaOrcCampPiece.class.getDeclaredField("groundPlan");
        plan.setAccessible(true);
        plan.set(square, SavannaOrcVillage.planGround(102, (x, z) -> 102));
        var lawn = center.offset(82, 0, 82);
        var chunk = level.getChunkAt(lawn);
        SavannaCampColumns.loadVegetation(chunk, null);
        SavannaCampColumns.record(chunk, old);
        h.assertTrue(SavannaCampColumns.reservesVegetation(chunk, lawn, false)
                        && !SavannaCampVegetation.reserved(square, lawn, false),
                "The old podzol corner must become open v8 lawn");
        SavannaCampColumns.record(chunk, square);
        h.assertTrue(!SavannaCampColumns.reservesVegetation(chunk, lawn, false), "Replacement retained the old podzol's groundcover reservation");
        SavannaCampColumns.loadVegetation(chunk, SavannaCampColumns.saveVegetation(chunk));
        h.assertTrue(!SavannaCampColumns.reservesVegetation(chunk, lawn, false), "Stale reservations returned after mask reload");
        var neighbor = center.offset(111, 0, 40);
        var edgeChunk = level.getChunkAt(neighbor);
        var existing = new BitSet(512);
        int bit = ((neighbor.getZ() & 15) << 4) | (neighbor.getX() & 15);
        existing.set(bit); existing.set(bit + 256);
        SavannaCampColumns.loadVegetation(edgeChunk, existing.toLongArray());
        h.assertTrue(!square.hasMoundColumn(neighbor), "The neighboring reservation must lie outside the actually overwritten ground");
        SavannaCampColumns.record(edgeChunk, square);
        h.assertTrue(SavannaCampColumns.reservesVegetation(edgeChunk, neighbor, false)
                        && SavannaCampColumns.reservesVegetation(edgeChunk, neighbor, true),
                "Replacing one camp erased an untouched neighboring camp's columns in the same edge chunk");
        h.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void sparseCampClearsTallInteriorObstaclesWithoutTouchingOutsideOrOtherChunks(GameTestHelper h) {
        var level = h.getLevel();
        var origin = new BlockPos(32000, 100, 32000);
        var interior = origin.offset(SavannaOrcVillage.CENTER + 40, 17, SavannaOrcVillage.CENTER + 40);
        var untouchedNeighbor = interior.east();
        var exterior = origin.offset(SavannaOrcVillage.CENTER + 111, 17, SavannaOrcVillage.CENTER + 30);
        var states = new java.util.HashMap<BlockPos, net.minecraft.world.level.block.state.BlockState>();
        for (var pos : List.of(interior, untouchedNeighbor, exterior)) states.put(pos, net.minecraft.world.level.block.Blocks.ACACIA_LEAVES.defaultBlockState());
        net.minecraft.world.level.levelgen.structure.BoundingBox[] active = {null};
        boolean[] flatColumn = {false};
        var world = (net.minecraft.world.level.WorldGenLevel) java.lang.reflect.Proxy.newProxyInstance(SavannaReviewRegressionTests.class.getClassLoader(),
                new Class<?>[]{net.minecraft.world.level.WorldGenLevel.class}, (proxy, method, arguments) -> {
                    switch (method.getName()) {
                        case "getLevel": {
                            h.assertFalse(flatColumn[0], "Flat mound placement must not query the native terrain generator");
                            return level;
                        }
                        case "getBlockState": return states.getOrDefault(arguments[0], net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
                        case "getFluidState": return states.getOrDefault(arguments[0], net.minecraft.world.level.block.Blocks.AIR.defaultBlockState()).getFluidState();
                        case "setBlock": {
                            var pos = ((BlockPos) arguments[0]).immutable();
                            h.assertTrue(active[0].isInside(pos), "Procedural air clearing escaped its generating chunk clip");
                            states.put(pos, (net.minecraft.world.level.block.state.BlockState) arguments[1]);
                            return true;
                        }
                    }
                    try { return method.invoke(level, arguments); }
                    catch (java.lang.reflect.InvocationTargetException failure) { throw failure.getCause(); }
                });
        var plan = SavannaOrcVillage.planGround(102, (x, z) -> 102);
        for (var column : List.of(interior, exterior)) {
            flatColumn[0] = column.equals(interior);
            active[0] = new net.minecraft.world.level.levelgen.structure.BoundingBox(column.getX(), level.getMinBuildHeight(), column.getZ(),
                    column.getX(), level.getMaxBuildHeight() - 1, column.getZ());
            SavannaOrcVillage.placeGround(world, active[0], origin, plan);
        }
        for (int y = 103; y <= 117; y++) h.assertTrue(states.get(interior.atY(y)).isAir(),
                "Sparse template left an old tree or structure in its indoor headroom at " + y);
        h.assertTrue(states.get(interior.atY(102)).is(net.minecraft.world.level.block.Blocks.GRASS_BLOCK)
                        && states.get(interior.atY(101)).is(net.minecraft.world.level.block.Blocks.ROOTED_DIRT)
                        && states.get(interior.atY(100)).is(net.minecraft.world.level.block.Blocks.ROOTED_DIRT),
                "The three omitted default template layers must be supplied identically by the ground pass");
        h.assertTrue(states.get(untouchedNeighbor).is(net.minecraft.world.level.block.Blocks.ACACIA_LEAVES)
                        && states.get(exterior).is(net.minecraft.world.level.block.Blocks.ACACIA_LEAVES),
                "Clearing must retain outside vegetation and columns owned by another generating chunk");
        h.succeed();
    }
}

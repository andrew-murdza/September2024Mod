package net.amurdza.examplemod.gametest;

import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.util.BiomePlacementRestrictions;
import net.amurdza.examplemod.util.GameplayDimensions;
import net.amurdza.examplemod.worldgen.structure.MansionBasementPiece;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(AOEMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class MansionSoilPlacementTests {
    @GameTest(template = "plant_gameplay_empty", timeoutTicks = 200)
    public static void actualMansionPlacementReplacesNaturalSoilWithPodzolAcrossChunkSeams(GameTestHelper helper) {
        var level = helper.getLevel().getServer().getLevel(GameplayDimensions.PLAINS);
        helper.assertTrue(BiomePlacementRestrictions.allowedMask(Blocks.PODZOL)
                        == (BiomePlacementRestrictions.SAVANNA | BiomePlacementRestrictions.PLAINS | BiomePlacementRestrictions.SNOWY),
                "Source tags must retain podzol for the camps, mansion and snowy forests");
        var piece = new MansionBasementPiece(new BlockPos(21000, 220, 21000), net.minecraft.world.level.block.Rotation.NONE, 3);
        for (int x = -28; x <= -18; x++) for (int z = 35; z <= 43; z++) {
            var pos = piece.world(x, 0, z);
            level.setBlock(pos, ((x + z) % 2 == 0 ? Blocks.DIRT : Blocks.STONE).defaultBlockState(), 18);
        }
        var room = BoundingBox.fromCorners(piece.world(-28, -2, 35), piece.world(-18, 5, 43));
        int chunks = 0;
        for (int cx = room.minX() >> 4; cx <= room.maxX() >> 4; cx++) for (int cz = room.minZ() >> 4; cz <= room.maxZ() >> 4; cz++) {
            var chunk = new ChunkPos(cx, cz);
            var clip = new BoundingBox(Math.max(room.minX(), chunk.getMinBlockX()), room.minY(), Math.max(room.minZ(), chunk.getMinBlockZ()),
                    Math.min(room.maxX(), chunk.getMaxBlockX()), room.maxY(), Math.min(room.maxZ(), chunk.getMaxBlockZ()));
            piece.postProcess(level, level.structureManager(), level.getChunkSource().getGenerator(), RandomSource.create(0), clip, chunk, piece.keyAnchor());
            chunks++;
        }
        helper.assertTrue(chunks > 1, "Exercise an authored room across actual chunk boundaries");
        int soil = 0;
        for (int x = -28; x <= -18; x++) for (int z = 35; z <= 43; z++) {
            var pos = piece.world(x, 0, z);
            boolean water = x == -23 && z >= 36 && z <= 42;
            helper.assertTrue(level.getBlockState(pos).is(water ? Blocks.WATER : Blocks.PODZOL)
                            && level.getBlockState(pos.below()).is(Blocks.STONE_BRICKS),
                    "Actual ServerLevel placement left original terrain beneath the mushroom room at " + pos + ": " + level.getBlockState(pos));
            if (!water) soil++;
        }
        helper.assertTrue(soil == 92, "All 92 mushroom growing cells must use their authored podzol");
        helper.succeed();
    }
}

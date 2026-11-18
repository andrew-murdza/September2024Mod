package net.amurdza.examplemod.gametest;

import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.util.GameplayDimensions;
import net.amurdza.examplemod.worldgen.structure.SavannaOrcCampPiece;
import net.amurdza.examplemod.worldgen.structure.SavannaOrcVillage;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(AOEMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class SavannaNovember15RoundTwoTests {
    private SavannaNovember15RoundTwoTests() { }

    @GameTest(template = "plant_gameplay_empty", timeoutTicks = 300)
    public static void shallowCropPondsKeepTwoDryCaneSectionsAfterFluidTicks(GameTestHelper helper) {
        var level = helper.getLevel().getServer().getLevel(GameplayDimensions.SAVANNA);
        var origin = new BlockPos(32768, 200, 32768);
        var piece = new SavannaOrcCampPiece(level.getStructureManager(), origin.offset(SavannaOrcVillage.CENTER, 0, SavannaOrcVillage.CENTER));
        var pond = origin.offset(SavannaOrcVillage.CENTER + 46, 2, SavannaOrcVillage.CENTER + 46);
        var chunk = new ChunkPos(pond);
        level.getChunk(chunk.x, chunk.z);
        var clip = new BoundingBox(chunk.getMinBlockX(), level.getMinBuildHeight(), chunk.getMinBlockZ(),
                chunk.getMaxBlockX(), level.getMaxBuildHeight() - 1, chunk.getMaxBlockZ());
        piece.postProcess(level, level.structureManager(), level.getChunkSource().getGenerator(), RandomSource.create(1937), clip, chunk, origin);
        helper.startSequence().thenIdle(100).thenExecute(() -> {
            int canes = 0;
            for (int x = 0; x < 4; x++) for (int z = 0; z < 4; z++) {
                var bottom = pond.offset(x, 0, z);
                for (int y = bottom.getY() - 1; y > level.getMinBuildHeight() + 4; y--) {
                    var below = new BlockPos(bottom.getX(), y, bottom.getZ());
                    helper.assertTrue(level.getBlockState(below).is(Blocks.MUD), "Pond mud must remain continuous down to bedrock at " + below);
                }
                boolean edge = x == 0 || x == 3 || z == 0 || z == 3;
                if (edge) {
                    for (int y = 0; y < 3; y++) {
                        var state = level.getBlockState(bottom.above(y));
                        helper.assertTrue(state.is(Blocks.SUGAR_CANE)
                                        && state.getValue(BlockStateProperties.WATERLOGGED) == (y == 0),
                                "Cane must have one submerged and two dry surviving sections at " + bottom.above(y));
                    }
                    canes++;
                } else helper.assertTrue(level.getBlockState(bottom).is(Blocks.WATER) && level.getBlockState(bottom.above()).isAir(),
                        "The four center columns must remain unobstructed one-deep water");
            }
            helper.assertTrue(canes == 12, "Exactly twelve pond-edge columns carry cane");
        }).thenSucceed();
    }

    @GameTest(template = "plant_gameplay_empty", timeoutTicks = 300)
    public static void widePenGatesRemainClosedAndCanOpenIndividually(GameTestHelper helper) {
        var level = helper.getLevel().getServer().getLevel(GameplayDimensions.SAVANNA);
        var origin = new BlockPos(33280, 200, 33280);
        var piece = new SavannaOrcCampPiece(level.getStructureManager(), origin.offset(SavannaOrcVillage.CENTER, 0, SavannaOrcVillage.CENTER));
        var gates = new java.util.ArrayList<BlockPos>();
        for (int a : new int[]{62, 87}) for (int b = 15; b <= 18; b++) gates.add(origin.offset(SavannaOrcVillage.CENTER + a, 3, SavannaOrcVillage.CENTER + b));
        for (int b : new int[]{4, 29}) for (int a = 73; a <= 76; a++) gates.add(origin.offset(SavannaOrcVillage.CENTER + a, 3, SavannaOrcVillage.CENTER + b));
        var chunks = gates.stream().map(ChunkPos::new).distinct().toList();
        for (var chunk : chunks) {
            level.getChunk(chunk.x, chunk.z);
            var clip = new BoundingBox(chunk.getMinBlockX(), level.getMinBuildHeight(), chunk.getMinBlockZ(),
                    chunk.getMaxBlockX(), level.getMaxBuildHeight() - 1, chunk.getMaxBlockZ());
            piece.postProcess(level, level.structureManager(), level.getChunkSource().getGenerator(), RandomSource.create(1937), clip, chunk, origin);
        }
        helper.startSequence().thenIdle(40).thenExecute(() -> {
            for (var gate : gates) {
                var state = level.getBlockState(gate);
                helper.assertTrue(state.is(Blocks.ACACIA_FENCE_GATE) && !state.getValue(BlockStateProperties.OPEN),
                        "All sixteen gate blocks must survive closed at " + gate);
                helper.assertFalse(state.getCollisionShape(level, gate).isEmpty(), "Closed gates must contain livestock");
                var open = state.setValue(BlockStateProperties.OPEN, true);
                level.setBlock(gate, open, 2);
                helper.assertTrue(level.getBlockState(gate).getCollisionShape(level, gate).isEmpty(), "An opened gate must admit the player");
                level.setBlock(gate, state, 2);
            }
        }).thenSucceed();
    }
}

package net.amurdza.examplemod.gametest;

import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.event_handlers.MansionProtection;
import net.amurdza.examplemod.util.GameplayDimensions;
import net.amurdza.examplemod.worldgen.structure.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.WallTorchBlock;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.*;

@GameTestHolder(AOEMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class MansionNovember22MineTests {
    @GameTest(template = "plant_gameplay_empty", timeoutTicks = 600)
    public static void mineTorchesFaceOuterWallsAndFourOresPreserveEveryBrick(GameTestHelper h) {
        var serial = StructurePieceSerializationContext.fromLevel(h.getLevel());
        for (var rotation : Rotation.values()) {
            var entrance = new BlockPos(11, 150, -7);
            var fresh = new MansionBasementPiece(entrance, rotation);
            var current = (MansionBasementPiece) MansionBasementPiece.load(serial, fresh.createTag(serial));
            h.assertTrue(current.layoutVersion() == 14, "New mines must save and reload as V14");
            var old = new MansionBasementPiece(entrance, rotation, 13);
            var oldBlocks = MansionBasementV3Tests.place(h, old);
            var blocks = MansionBasementV3Tests.place(h, current);
            h.assertTrue(blocks.keySet().equals(oldBlocks.keySet()), "Mining changes must not expand or carve the cellar shell");
            var ores = new HashSet<BlockPos>();
            var types = new HashSet<net.minecraft.world.level.block.Block>();
            for (var entry : blocks.entrySet()) {
                var before = oldBlocks.get(entry.getKey());
                var after = entry.getValue();
                if (MansionBasementLayoutV14.isOre(after)) {
                    h.assertTrue(before.is(Blocks.STONE), "Ore replaced masonry or furniture: " + entry.getKey());
                    ores.add(entry.getKey()); types.add(after.getBlock());
                } else if (!before.equals(after))
                    h.assertTrue(before.is(Blocks.WALL_TORCH) && after.is(Blocks.WALL_TORCH),
                            "Only mine ore and torch direction may differ from saved V13: " + entry.getKey());
            }
            h.assertTrue(types.equals(Set.of(Blocks.COAL_ORE, Blocks.IRON_ORE, Blocks.GOLD_ORE, Blocks.COPPER_ORE)),
                    "Mines must expose all four regional ores and no others");
            // Flood from existing air through ore only: every reward must be
            // obtainable without breaking permanently protected plain stone.
            var reachable = new HashSet<BlockPos>();
            var queue = new ArrayDeque<BlockPos>();
            blocks.forEach((pos, state) -> { if (state.isAir()) { reachable.add(pos); queue.add(pos); } });
            while (!queue.isEmpty()) {
                var pos = queue.remove();
                for (var direction : Direction.values()) {
                    var next = pos.relative(direction);
                    if (ores.contains(next) && reachable.add(next)) queue.add(next);
                }
            }
            h.assertTrue(reachable.containsAll(ores), "Every ore pocket must be reachable from an existing shaft");
            for (int layer = 0; layer < 2; layer++) for (int row = 0; row < 3; row++)
                for (int across : new int[]{2, 8}) for (int depth : new int[]{5, 10, 15}) {
                    var pos = current.world(-30 - depth, 2 - layer * 7, 14 + row * 12 + across + (layer == 0 ? 5 : 12));
                    var state = blocks.get(pos);
                    var facing = rotation.rotate(across == 2 ? Direction.SOUTH : Direction.NORTH);
                    h.assertTrue(state.is(Blocks.WALL_TORCH) && state.getValue(WallTorchBlock.FACING) == facing,
                            "A mine torch is attached to its inner wall: " + pos + "/" + state);
                    var support = blocks.get(pos.relative(facing.getOpposite()));
                    h.assertTrue(support != null && (support.is(Blocks.STONE) || MansionBasementLayoutV14.isOre(support)),
                            "An outer-wall torch must retain solid backing");
                }
            var restoredOld = (MansionBasementPiece) MansionBasementPiece.load(serial, old.createTag(serial));
            h.assertTrue(restoredOld.layoutVersion() == 13 && MansionBasementV3Tests.place(h, restoredOld).equals(oldBlocks),
                    "Saved V13 mines must keep their original stone and lights");
        }
        h.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void onlyAuthoredOreRewardsUnlockWhileMineStoneAndBrickStayProtected(GameTestHelper h) {
        var level = h.getLevel().getServer().getLevel(GameplayDimensions.PLAINS);
        var cellar = new MansionBasementPiece(new BlockPos(6200, 150, 6200));
        var id = UUID.randomUUID();
        cellar.encounter(id, 1);
        var upperRoom = BoundingBox.fromCorners(cellar.world(-4, 7, 0), cellar.world(4, 14, 8));
        PlacedMansions.get(level).record(cellar, List.of(upperRoom));
        var progress = AuthoredEncounterProgress.get(level);
        progress.register(id, cellar.keyAnchor(), 1, "mansion");
        var ore = cellar.world(-34, 2, 20);
        var stone = cellar.world(-32, 2, 20);
        var brick = cellar.world(-48, 2, 20);
        for (var pos : List.of(ore, stone, brick)) level.getChunkAt(pos);
        level.setBlock(ore, Blocks.COAL_ORE.defaultBlockState(), 18);
        level.setBlock(stone, Blocks.STONE.defaultBlockState(), 18);
        level.setBlock(brick, Blocks.STONE_BRICKS.defaultBlockState(), 18);
        h.assertTrue(cellar.isMineOre(ore) && !cellar.isMineOre(stone) && !cellar.isMineOre(brick), "Reward scope must identify authored ore only");
        for (var pos : List.of(ore, stone, brick)) h.assertTrue(MansionProtection.protectedAt(level, pos), "Uncleared mines must remain locked");
        var defender = EntityType.VINDICATOR.create(level);
        AuthoredEncounterProgress.markDefender(defender, id, cellar.keyAnchor(), 1, "mansion", 0);
        progress.track(defender); progress.defeat(defender);
        h.assertTrue(!MansionProtection.protectedAt(level, ore) && MansionProtection.protectedAt(level, stone)
                        && MansionProtection.protectedAt(level, brick), "Victory must release ore while retaining plain stone and masonry");
        var chunk = level.getChunkAt(ore);
        var packet = new CompoundTag(); MansionProtectionSnapshot.write(chunk, packet); MansionProtectionSnapshot.read(chunk, packet);
        h.assertTrue(MansionProtectionSnapshot.rewardsUnlocked(level, ore) && !MansionProtectionSnapshot.protects(level, ore)
                        && MansionProtectionSnapshot.protects(level, stone), "Client prediction must release exactly the authored ore rewards");
        MansionProtectionSnapshot.read(chunk, new CompoundTag());
        h.succeed();
    }
}

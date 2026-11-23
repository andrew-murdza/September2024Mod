package net.amurdza.examplemod.gametest;

import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.worldgen.SnowyPlantSnowlogging;
import net.amurdza.examplemod.worldgen.SnowyVegetation;
import net.amurdza.examplemod.worldgen.structure.BossStructureBounds;
import net.amurdza.examplemod.worldgen.structure.GoblinFortressBlocks;
import net.amurdza.examplemod.worldgen.structure.GoblinFortressLayout;
import net.amurdza.examplemod.worldgen.structure.GoblinFortressPiece;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.UUID;

@GameTestHolder(AOEMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class GoblinFortressIndoorSnowTests {
    private GoblinFortressIndoorSnowTests() { }

    @GameTest(template = "plant_gameplay_empty")
    public static void naturalGroundSnowStopsAtFortressRoofsButKeepsOutdoorSnow(GameTestHelper helper) {
        var level = helper.getLevel();
        var origin = helper.absolutePos(new BlockPos(3, 0, 3)).atY(210);
        var previous = new LinkedHashMap<BlockPos, BlockState>();
        var boundaries = new ArrayList<String>();
        java.util.function.BiConsumer<BlockPos, BlockState> set = (pos, state) -> {
            previous.putIfAbsent(pos, level.getBlockState(pos));
            GoblinFortressBlocks.set(level, pos, state);
        };
        try {
            var rooms = GoblinFortressLayout.rooms().stream()
                    .filter(room -> room.name().contains("mushroom") || room.name().equals("mooshrooms")).toList();
            helper.assertTrue(rooms.size() == 4, "Exercise all four authored mycelium rooms");
            for (int sample = 0; sample < rooms.size(); sample++) {
                var room = rooms.get(sample);
                var slot = GoblinFortressLayout.spawnPositions(room).get(0);
                var column = origin.east(sample * 2);
                for (int y = GoblinFortressLayout.MIN_Y; y <= GoblinFortressLayout.MAX_Y; y++) {
                    var state = GoblinFortressLayout.stateAt(slot.getX(), y, slot.getZ());
                    set.accept(column.above(y), state == null ? Blocks.AIR.defaultBlockState() : state);
                }
                String key = GoblinFortressPiece.ENCOUNTER_KIND + ":snow-qa:" + UUID.randomUUID();
                boundaries.add(key);
                BossStructureBounds.get(level).record(key, new AABB(column.offset(0, -51, 0), column.offset(1, 49, 1)), null);
                var mycelium = column.above(room.floor());
                helper.assertTrue(level.getBlockState(mycelium).is(Blocks.MYCELIUM)
                                && level.getBlockState(mycelium.above()).isAir(),
                        "The fixture must contain the real clear mycelium floor below all authored upper levels: " + room.name());
                helper.assertTrue(SnowyVegetation.surfaceRoot(level, column.getX(), column.getZ()) == null,
                        "Natural ground decoration must not tunnel through masonry to " + room.name());
                helper.assertTrue(level.getBlockState(mycelium.above()).isAir(),
                        "The rejected underground surface must remain clear of snow");
            }

            // The independent heightmap pass still covers the outdoor snowy
            // terrace, even when the indoor ground scan correctly returns null.
            var pen = origin.east(10);
            for (int y = -1; y <= 9; y++) set.accept(pen.above(y),
                    (y == 0 ? Blocks.SNOW_BLOCK : Blocks.AIR).defaultBlockState());
            String penKey = GoblinFortressPiece.ENCOUNTER_KIND + ":snow-qa:" + UUID.randomUUID();
            boundaries.add(penKey);
            BossStructureBounds.get(level).record(penKey, new AABB(pen.below(), pen.offset(1, 10, 1)), null);
            var top = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, pen);
            helper.assertTrue(top.equals(pen.above()) && SnowyPlantSnowlogging.coverSurface(level, top)
                            && level.getBlockState(top).is(Blocks.SNOW),
                    "The exposed snowy livestock terrace must still receive its ordinary surface snow");

            var forest = origin.east(13);
            for (int y = -1; y <= 9; y++) set.accept(forest.above(y),
                    (y == 0 ? Blocks.GRASS_BLOCK : y == 7 ? Blocks.SPRUCE_LEAVES : Blocks.AIR).defaultBlockState());
            var root = SnowyVegetation.surfaceRoot(level, forest.getX(), forest.getZ());
            helper.assertTrue(forest.above().equals(root) && SnowyPlantSnowlogging.coverSurface(level, root)
                            && level.getBlockState(root).is(Blocks.SNOW),
                    "Outdoor forest soil must retain snow beneath its native tree canopy");
        } finally {
            boundaries.forEach(key -> BossStructureBounds.get(level).remove(key));
            previous.forEach((pos, state) -> GoblinFortressBlocks.set(level, pos, state));
        }
        helper.succeed();
    }
}

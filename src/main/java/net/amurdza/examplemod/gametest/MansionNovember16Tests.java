package net.amurdza.examplemod.gametest;

import com.google.gson.JsonParser;
import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.util.GameplayDimensions;
import net.amurdza.examplemod.worldgen.structure.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.structure.structures.WoodlandMansionPieces.WoodlandMansionPiece;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

@GameTestHolder(AOEMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class MansionNovember16Tests {
    @GameTest(template = "plant_gameplay_empty", timeoutTicks = 600)
    public static void mineThresholdsAndFarStoneCapsSurviveClippingAndBlueprintReload(GameTestHelper h) {
        var serial = StructurePieceSerializationContext.fromLevel(h.getLevel());
        for (var rotation : Rotation.values()) {
            var original = new MansionBasementPiece(new BlockPos(8, 150, 8), rotation, 6);
            var piece = (MansionBasementPiece) MansionBasementPiece.load(serial, original.createTag(serial));
            h.assertTrue(piece.layoutVersion() == 6, "New shafts need a separate saved layout version");
            var placed = MansionBasementV3Tests.place(h, piece);
            for (int row = 0; row < 3; row++) {
                int firstZ = 14 + row * 12;
                for (int a = 0; a <= 10; a++) {
                    for (int y : new int[]{0, 4}) assertBlock(h, placed, piece.world(-30, y, firstZ + a), Blocks.STONE_BRICKS,
                            "The complete threshold and lintel rows must be brick");
                    for (int y = 0; y <= 4; y++) {
                        assertBlock(h, placed, piece.world(-47, y, firstZ + a), Blocks.STONE, "A mineable stone column must precede the backing");
                        var backing = piece.world(-48, y, firstZ + a);
                        assertBlock(h, placed, backing, Blocks.STONE_BRICKS, "The backing must be one block farther away");
                        h.assertTrue(piece.getBoundingBox().isInside(backing) && piece.isBuiltCell(backing), "The farther backing needs clipping bounds and protection");
                        h.assertTrue(!piece.isBuiltCell(piece.world(-47, y, firstZ + a)), "Players must be able to mine the last stone column");
                    }
                }
                for (int y = 0; y <= 5; y++) assertBlock(h, placed, piece.world(-30, y, firstZ + 5), Blocks.STONE_BRICKS,
                        "The marked central face column must be entirely brick without red wool");
                for (int a : new int[]{2, 8}) for (int depth = 0; depth <= 16; depth++) {
                    assertBlock(h, placed, piece.world(-30 - depth, 1, firstZ + a), Blocks.AIR, "Both original tunnel routes must remain open");
                    assertBlock(h, placed, piece.world(-30 - depth, 3, firstZ + a), Blocks.AIR, "Tunnel headroom must remain clear");
                }
                assertBlock(h, placed, piece.world(-31, 3, firstZ + 5), Blocks.STONE, "The ore-bearing interior must not become a brick-lined tunnel");
            }
            for (int x = -29; x <= 30; x++) for (int z = 0; z <= 73; z++) for (int y = -2; y <= 13; y++) {
                var expected = MansionBasementLayoutV5.cell(x, y, z);
                if (expected != null) h.assertTrue(expected.rotate(rotation).equals(placed.get(piece.world(x, y, z))),
                        "Mine changes must preserve every authored room, stair and lantern cell at " + x + "/" + y + "/" + z);
            }
            var legacy = new MansionBasementPiece(new BlockPos(8, 150, 8), rotation, 5);
            var restored = (MansionBasementPiece) MansionBasementPiece.load(serial, legacy.createTag(serial));
            h.assertTrue(!restored.isBuiltCell(restored.world(-30, 0, 16)) && restored.isBuiltCell(restored.world(-47, 1, 16))
                    && !restored.containsAuthoredSpace(restored.world(-48, 1, 16)), "Saved V5 thresholds and backing must not shift");
        }
        h.succeed();
    }

    @GameTest(template = "plant_gameplay_empty", timeoutTicks = 300)
    public static void authoredBirchEntranceSurvivesEveryOrientationWithoutChangingLegacyTemplates(GameTestHelper h) throws Exception {
        var level = h.getLevel().getServer().getLevel(GameplayDimensions.PLAINS);
        var manager = level.getStructureManager();
        var serial = StructurePieceSerializationContext.fromLevel(level);
        String name = "aoe_compact_entrance_v2";
        var template = manager.getOrCreate(new ResourceLocation("minecraft", "woodland_mansion/" + name));
        var legacy = manager.getOrCreate(new ResourceLocation("minecraft", "woodland_mansion/aoe_compact_entrance"));
        var legacyBefore = legacy.save(new CompoundTag());
        var before = template.save(new CompoundTag());
        var original = new ArrayList<StructureTemplate.StructureBlockInfo>();
        var palette = before.getList("palette", Tag.TAG_COMPOUND);
        for (var item : before.getList("blocks", Tag.TAG_COMPOUND)) {
            var data = (CompoundTag) item;
            var p = data.getList("pos", Tag.TAG_INT);
            original.add(new StructureTemplate.StructureBlockInfo(new BlockPos(p.getInt(0), p.getInt(1), p.getInt(2)),
                    NbtUtils.readBlockState(level.holderLookup(Registries.BLOCK), palette.getCompound(data.getInt("state"))),
                    data.contains("nbt") ? data.getCompound("nbt") : null));
        }
        var expected = new HashMap<BlockPos, BlockState>();
        try (var stream = MansionNovember16Tests.class.getResourceAsStream("/data/aoemod/structure_data/mansion_entrance_v2.json")) {
            var capture = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
            var states = new ArrayList<BlockState>();
            for (var item : capture.getAsJsonArray("palette")) {
                var object = item.getAsJsonObject();
                var tag = new CompoundTag(); tag.putString("Name", object.get("Name").getAsString());
                if (object.has("Properties")) {
                    var properties = new CompoundTag();
                    object.getAsJsonObject("Properties").entrySet().forEach(e -> properties.putString(e.getKey(), e.getValue().getAsString()));
                    tag.put("Properties", properties);
                }
                states.add(NbtUtils.readBlockState(level.holderLookup(Registries.BLOCK), tag));
            }
            for (var item : capture.getAsJsonArray("blocks")) {
                var p = item.getAsJsonArray();
                expected.put(new BlockPos(p.get(0).getAsInt(), p.get(1).getAsInt(), p.get(2).getAsInt()), states.get(p.get(3).getAsInt()));
            }
        }
        for (var rotation : Rotation.values()) for (var mirror : Mirror.values()) {
            var settings = new StructurePlaceSettings().setRotation(rotation).setMirror(mirror).addProcessor(IllagerMansionProcessor.INSTANCE);
            var placed = new HashMap<BlockPos, BlockState>();
            StructureTemplate.processBlockInfos(level, BlockPos.ZERO, BlockPos.ZERO, settings, original, template)
                    .forEach(info -> placed.put(info.pos(), info.state()));
            for (var entry : expected.entrySet()) {
                var point = StructureTemplate.calculateRelativePosition(settings, entry.getKey());
                h.assertTrue(entry.getValue().equals(placed.get(point)), "Processing altered the player's captured foyer at " + entry.getKey() + "/" + rotation + "/" + mirror);
            }
            for (int y = 1; y <= 8; y++) for (int z = 6; z <= 8; z++) {
                var pos = new BlockPos(11 - y, y, z);
                var state = placed.get(StructureTemplate.calculateRelativePosition(settings, pos));
                h.assertTrue(state.is(Blocks.BIRCH_STAIRS) && state.getValue(StairBlock.FACING) == Direction.WEST,
                        "The three-wide eight-tread birch flight must retain its ascent direction");
                for (int head = 1; head <= 2; head++) assertBlock(h, placed,
                        StructureTemplate.calculateRelativePosition(settings, pos.above(head)), Blocks.AIR, "Every ascending lane needs full headroom");
            }
            var piece = new WoodlandMansionPiece(manager, name, new BlockPos(8, 150, 8), rotation, mirror);
            var restored = new WoodlandMansionPiece(manager, piece.createTag(serial));
            h.assertTrue(restored.template() == template && restored.getRotation() == rotation && restored.placeSettings().getMirror() == mirror,
                    "Saving and reloading must preserve the new entrance identity and orientation");
        }
        h.assertTrue(legacy.save(new CompoundTag()).equals(legacyBefore) && template.save(new CompoundTag()).equals(before), "Processing must leave both shared template versions unchanged");
        h.succeed();
    }

    private static void assertBlock(GameTestHelper h, Map<BlockPos, BlockState> states, BlockPos point, Block expected, String message) {
        h.assertTrue(states.containsKey(point) && states.get(point).is(expected), message + " at " + point + ": " + states.get(point));
    }
}

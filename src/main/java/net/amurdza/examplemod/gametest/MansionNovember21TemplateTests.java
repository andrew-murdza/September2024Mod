package net.amurdza.examplemod.gametest;

import net.amurdza.examplemod.AOEMod;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.HashMap;
import java.util.Map;

@GameTestHolder(AOEMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class MansionNovember21TemplateTests {
    @GameTest(template = "plant_gameplay_empty", timeoutTicks = 300)
    public static void newTemplatesKeepOakFloorsAndOnlyTheTopWindowLosesItsStoneHeader(GameTestHelper h) {
        var manager = h.getLevel().getStructureManager();
        var oldIds = manager.listTemplates().filter(id -> id.getNamespace().equals("minecraft")
                && id.getPath().startsWith("woodland_mansion/aoe_v6_")).toList();
        h.assertTrue(oldIds.size() >= 79, "Keep the complete saved V6 template collection");
        for (var oldId : oldIds) {
            var newId = new ResourceLocation("minecraft", oldId.getPath().replace("aoe_v6_", "aoe_v7_"));
            var revised = manager.get(newId);
            h.assertTrue(revised.isPresent(), "Every generated piece needs its own saved V7 identity: " + newId);
            h.assertTrue(cells(revised.orElseThrow()).values().stream().noneMatch(state -> state.is(Blocks.BIRCH_PLANKS)),
                    "The current version must keep the prior all-oak plank change: " + newId);
        }

        var old = cells(template(manager, "aoe_v6_wall_window"));
        var lower = cells(template(manager, "aoe_v7_wall_window"));
        var top = cells(template(manager, "aoe_v7_wall_window_top"));
        h.assertTrue(lower.equals(old), "All lower-floor window glass, logs and stone decorations must remain unchanged");
        int changed = 0;
        for (var entry : old.entrySet()) {
            var expected = entry.getValue();
            if (entry.getKey().getY() == 7 && stoneDecoration(expected)) {
                expected = Blocks.OAK_PLANKS.defaultBlockState();
                changed++;
            }
            h.assertTrue(expected.equals(top.get(entry.getKey())),
                    "Only the fifth-floor stone header may change at " + entry.getKey());
        }
        h.assertTrue(changed > 0 && top.size() == old.size(), "Replace the top stone band without resizing the window");
        h.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void blockPlatformsKeepTheirNativeOutlineAndContinuousRedThreshold(GameTestHelper h) {
        var manager = h.getLevel().getStructureManager();
        int filledSteps = 0;
        for (String name : new String[]{"entrance", "entrance_platform"}) {
            var oldTemplate = template(manager, "aoe_v6_" + name);
            var revisedTemplate = template(manager, "aoe_v7_" + name);
            var old = cells(oldTemplate);
            var revised = cells(revisedTemplate);
            h.assertTrue(oldTemplate.getSize().equals(revisedTemplate.getSize()) && old.size() == revised.size(),
                    "Keep the native entrance footprint and doorway dimensions");
            for (var entry : old.entrySet()) {
                var expected = entry.getValue();
                if (expected.is(Blocks.COBBLESTONE_STAIRS)) {
                    expected = Blocks.COBBLESTONE.defaultBlockState();
                    filledSteps++;
                }
                h.assertTrue(expected.equals(revised.get(entry.getKey())),
                        "Only the platform stair blocks may change; retain posts, slabs and carpet at " + name + "/" + entry.getKey());
            }
            h.assertTrue(revised.values().stream().noneMatch(state -> state.is(Blocks.COBBLESTONE_STAIRS)),
                    "The photographed platform uses complete stone blocks");
        }
        h.assertTrue(filledSteps == 7, "Replace all seven steps while preserving the saved V6 stair platform");
        var entrance = cells(template(manager, "aoe_v7_entrance"));
        var platform = cells(template(manager, "aoe_v7_entrance_platform"));
        for (int x = 2; x <= 4; x++) for (int z = 0; z <= 1; z++) {
            h.assertTrue(at(entrance, x, 1, z).is(Blocks.RED_CARPET)
                            && at(platform, x + 2, 1, z + 4).is(Blocks.RED_CARPET),
                    "The three-wide red carpet must continue across the platform/entrance overlap");
        }
        for (int x : new int[]{1, 5}) h.assertTrue(at(entrance, x, 1, 1).is(Blocks.WHITE_CARPET)
                        && at(platform, x + 2, 1, 5).is(Blocks.WHITE_CARPET),
                "Keep the white border outside the red walkway");
        h.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void flatFlowerRoomCeilingPreservesFurnitureAndSavedCornices(GameTestHelper h) {
        var manager = h.getLevel().getStructureManager();
        var old = cells(template(manager, "aoe_v6_1x2_c4"));
        var revisedTemplate = template(manager, "aoe_v7_1x2_c4");
        var revised = cells(revisedTemplate);
        for (var entry : old.entrySet()) if (entry.getKey().getY() < 5)
            h.assertTrue(entry.getValue().equals(revised.get(entry.getKey())),
                    "Keep flowers, bookshelves and lower furniture while removing the overhead cornice");
        var size = revisedTemplate.getSize();
        for (int x = 0; x < size.getX(); x++) for (int z = 0; z < size.getZ(); z++) {
            h.assertTrue(at(revised, x, 5, z).isAir() && at(revised, x, 6, z).isAir()
                            && at(revised, x, 7, z).is(Blocks.OAK_PLANKS),
                    "The flower/library room must have clear headroom beneath one flat oak ceiling");
        }
        h.assertTrue(old.entrySet().stream().anyMatch(entry -> entry.getKey().getY() >= 5
                        && entry.getValue().is(Blocks.OAK_STAIRS)),
                "Saved V6 rooms must retain their former cornice");
        h.assertTrue(cells(template(manager, "aoe_v7_1x2_c1")).equals(cells(template(manager, "aoe_v6_1x2_c1"))),
                "The other native library already has a flat ceiling and must remain unchanged");
        h.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void insetRoofHasSolidWoodenSlopesWithoutTheFlatUndersideBoard(GameTestHelper h) {
        var manager = h.getLevel().getStructureManager();
        var flat = template(manager, "aoe_v7_roof");
        h.assertTrue(flat.getSize().equals(new BlockPos(9, 2, 9)),
                "The flat cap must stop at the slope peak instead of covering the next stair row");
        for (String name : new String[]{"roof_front", "roof_corner", "roof_inner_corner"}) {
            var revisedTemplate = template(manager, "aoe_v7_" + name);
            var size = revisedTemplate.getSize();
            h.assertTrue(size.equals(new BlockPos(4, 5, name.equals("roof_front") ? 10 : 4)),
                    "Each inset edge and corner must retain the matching slope profile");
            var roof = cells(revisedTemplate);
            h.assertTrue(roof.values().stream().noneMatch(MansionNovember21TemplateTests::stoneDecoration),
                    "No exposed cobblestone may remain in the top roof edge or corners: " + name);
            h.assertTrue(roof.values().stream().anyMatch(state -> state.is(Blocks.OAK_STAIRS) || state.is(Blocks.DARK_OAK_STAIRS)),
                    "Keep a shaped stair edge rather than replacing the roof with a flat slab");
            for (int x = 0; x < size.getX(); x++) for (int z = 0; z < size.getZ(); z++) {
                h.assertTrue(at(roof, x, 0, z).is(Blocks.STRUCTURE_VOID),
                        "Omit the flat under-eave board without clearing the fifth-floor facade: " + name);
                int top = 0;
                for (int y = 1; y < size.getY(); y++) {
                    var state = at(roof, x, y, z);
                    if (!state.isAir() && !state.is(Blocks.STRUCTURE_VOID)) top = y;
                }
                for (int y = 1; y < size.getY(); y++) {
                    var state = at(roof, x, y, z);
                    h.assertTrue(y <= top ? !state.isAir() && !state.is(Blocks.STRUCTURE_VOID)
                                    : name.equals("roof_corner") ? state.isAir() : state.is(Blocks.STRUCTURE_VOID),
                            "Fill the slope, preserve overlapping fronts, and trim protruding corner caps: " + name + "/" + x + "," + y + "," + z);
                }
            }
        }
        h.assertTrue(cells(template(manager, "aoe_v6_roof_corner")).values().stream().anyMatch(MansionNovember21TemplateTests::stoneDecoration),
                "Earlier saved roof corners must keep their original stone instead of being rewritten");
        h.succeed();
    }

    private static boolean stoneDecoration(BlockState state) {
        return state.is(Blocks.COBBLESTONE) || state.is(Blocks.COBBLESTONE_WALL) || state.is(Blocks.COBBLESTONE_STAIRS);
    }

    private static BlockState at(Map<BlockPos, BlockState> cells, int x, int y, int z) {
        return cells.getOrDefault(new BlockPos(x, y, z), Blocks.AIR.defaultBlockState());
    }

    private static StructureTemplate template(StructureTemplateManager manager, String name) {
        return manager.get(new ResourceLocation("minecraft", "woodland_mansion/" + name)).orElseThrow();
    }

    private static Map<BlockPos, BlockState> cells(StructureTemplate template) {
        var data = template.save(new CompoundTag());
        var palette = data.getList("palette", Tag.TAG_COMPOUND);
        var result = new HashMap<BlockPos, BlockState>();
        for (var entry : data.getList("blocks", Tag.TAG_COMPOUND)) {
            var block = (CompoundTag) entry;
            var pos = block.getList("pos", Tag.TAG_INT);
            result.put(new BlockPos(pos.getInt(0), pos.getInt(1), pos.getInt(2)),
                    NbtUtils.readBlockState(BuiltInRegistries.BLOCK.asLookup(), palette.getCompound(block.getInt("state"))));
        }
        return result;
    }
}

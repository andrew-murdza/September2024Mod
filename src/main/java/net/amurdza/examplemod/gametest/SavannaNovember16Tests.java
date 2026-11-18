package net.amurdza.examplemod.gametest;

import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.registry.ModBlocks;
import net.amurdza.examplemod.util.GameplayDimensions;
import net.amurdza.examplemod.worldgen.SavannaCampVegetation;
import net.amurdza.examplemod.worldgen.structure.SavannaOrcCampPiece;
import net.amurdza.examplemod.worldgen.structure.SavannaOrcVillage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SkullBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

@GameTestHolder(AOEMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class SavannaNovember16Tests {
    private SavannaNovember16Tests() { }

    @GameTest(template = "plant_gameplay_empty")
    public static void compactCampKeepsCorrectedTentsEightWideGatesAndSixPanelIntervals(GameTestHelper helper) {
        var level = helper.getLevel().getServer().getLevel(GameplayDimensions.SAVANNA);
        var piece = new SavannaOrcCampPiece(level.getStructureManager(), new BlockPos(0, 200, 0));
        var blocks = blocks(level, SavannaOrcCampPiece.TEMPLATE);
        helper.assertTrue(piece.isCompactSquareVillage() && piece.template().getSize().getX() == 201,
                "Fresh camps must use the compact version-eight template");
        assertGeometry(helper, p -> blocks.getOrDefault(p, defaultGround(p)));
        helper.assertTrue(blocks.values().stream().filter(s -> s.is(Blocks.CHEST)).count() == 36
                        && blocks.values().stream().filter(s -> s.is(Blocks.RED_BED)).count() == 144,
                "The correction must retain all four chests and eight whole beds in every tent");
        for (int sx : new int[]{-1, 1}) for (int sz : new int[]{-1, 1}) {
            for (int x = 72; x <= 78; x++) for (int z = 72; z <= 78; z++) {
                BlockPos p = relative(mirror(x, sx), 2, mirror(z, sz));
                helper.assertTrue(defaultGround(p).is(Blocks.GRASS_BLOCK) && !blocks.containsKey(p)
                                && !SavannaCampVegetation.reservedLocal(p.getX(), p.getZ(), false)
                                && !SavannaCampVegetation.reservedLocal(p.getX(), p.getZ(), true),
                        "Removed corner patches must be ordinary unreserved lawn, not invisible tree exclusions: " + p);
            }
        }
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void savedVersionSevenRetainsItsTentFasciaCornersAndPlantingMask(GameTestHelper helper) {
        var level = helper.getLevel();
        var current = new SavannaOrcCampPiece(level.getStructureManager(), new BlockPos(0, 200, 0));
        var context = StructurePieceSerializationContext.fromLevel(level);
        var tag = current.createTag(context);
        tag.putInt("SettlementVersion", 7);
        tag.putInt("TPX", -107); tag.putInt("TPZ", -107);
        tag.putLongArray("VillageResidents", new long[]{0xFFFFFFFFFL});
        var old = new SavannaOrcCampPiece(context, tag);
        var oldBlocks = blocks(level, new ResourceLocation("aoemod:orc_village_v7"));
        helper.assertTrue(!old.isCompactSquareVillage() && old.isExpandedSquareVillage()
                        && old.template().getSize().getX() == 215 && old.getBoundingBox().getXSpan() == 255,
                "Saved V7 camps must retain their larger template, footprint and population branch");
        for (int x = 103; x <= 110; x++) helper.assertTrue(oldBlocks.get(new BlockPos(x, 7, 101)).is(Blocks.RED_WOOL),
                "The pre-correction roof must stay intact in the archived V7 template");
        var corner = new BlockPos(82, 202, 82);
        helper.assertTrue(SavannaCampVegetation.reserved(old, corner, true)
                        && !SavannaCampVegetation.reserved(current, corner, true),
                "An old podzol corner remains protected only for its original saved camp");
        var saved = old.createTag(context);
        helper.assertTrue(saved.getInt("SettlementVersion") == 7 && saved.getString("Template").equals("aoemod:orc_village_v7")
                        && java.util.Arrays.equals(saved.getLongArray("VillageResidents"), tag.getLongArray("VillageResidents")),
                "Saving an archived piece must retain its version, template and completed population bits");
        helper.assertTrue(new SavannaOrcCampPiece(context, saved).createTag(context).equals(saved),
                "Repeated old-piece reloads must remain stable");
        helper.succeed();
    }

    static void assertPlacedRevision(GameTestHelper helper, ServerLevel level, SavannaOrcCampPiece piece) {
        assertGeometry(helper, p -> level.getBlockState(piece.templatePosition().offset(p)));
        for (int sx : new int[]{-1, 1}) for (int sz : new int[]{-1, 1}) {
            var pos = piece.templatePosition().offset(relative(mirror(75, sx), 2, mirror(75, sz)));
            helper.assertFalse(level.getBlockState(pos).is(Blocks.PODZOL),
                    "The removed outer patch must not return after real placement and vegetation: " + pos);
        }
    }

    private static void assertGeometry(GameTestHelper h, Function<BlockPos, BlockState> state) {
        for (int cx : new int[]{-28, 0, 28}) for (int cz : new int[]{-28, 0, 28}) {
            for (int x = -4; x <= 3; x++) {
                h.assertTrue(state.apply(relative(cx + x, 7, cz - 6)).isAir(),
                        "Remove only the accidental north fascia, including both chunk-edge ends");
                h.assertTrue(state.apply(relative(cx + x, 6, cz - 6)).is(Blocks.RED_WOOL),
                        "The correct north lintel must remain below the removed row");
            }
        }
        for (int sign : new int[]{-1, 1}) for (int center : new int[]{-28, 0, 28}) {
            for (int a = 34; a <= 41; a++) {
                h.assertTrue(state.apply(relative(mirror(a, sign), 2, center)).is(Blocks.DIRT_PATH)
                                && state.apply(relative(center, 2, mirror(a, sign))).is(Blocks.DIRT_PATH),
                        "Eight clear path blocks must separate the outer tents and farmland");
            }
            h.assertTrue(state.apply(relative(mirror(42, sign), 2, center == 0 ? 12 : center)).is(Blocks.FARMLAND),
                    "The first farm row must begin immediately beyond the eight-block gap");
        }
        for (int direction = 0; direction < 4; direction++) for (int a = -7; a <= 6; a++) {
            BlockPos p = gate(direction, a, 3);
            if (a >= -4 && a <= 3) {
                for (int y = 0; y < 4; y++) h.assertTrue(state.apply(p.above(y)).isAir(), "Gate passage must retain eight clear lanes at " + p);
                h.assertTrue(state.apply(p.above(4)).is(Blocks.ACACIA_LOG)
                                && state.apply(p.above(5)).isAir() && state.apply(p.above(6)).isAir(),
                        "The beam spans the road without a floating center ornament");
            } else {
                for (int y = 0; y < 5; y++) h.assertTrue(state.apply(p.above(y)).is(Blocks.ACACIA_LOG), "Gate uprights must each be three logs wide");
                BlockState skull = state.apply(p.above(6));
                h.assertTrue(state.apply(p.above(5)).is(Blocks.ACACIA_FENCE) && skull.is(Blocks.SKELETON_SKULL)
                                && skull.getValue(SkullBlock.ROTATION) == (8 + direction * 4) % 16,
                        "Every gate pillar needs three outward-facing skulls above its connected fence cap");
            }
        }
        for (int side : new int[]{-98, 97}) for (boolean swap : new boolean[]{false, true}) {
            for (int a = -98; a < 97; a++) {
                if (a >= -7 && a < 6) continue;
                int phase = a < -7 ? (a + 98) % 7 : (a - 6) % 7;
                BlockPos p = relative(swap ? side : a, 3, swap ? a : side);
                h.assertTrue(state.apply(p).is(phase == 0 ? Blocks.ACACIA_LOG : ModBlocks.ACACIA_PALISADE.get()),
                        "Six palisade columns must separate every regular post and corner: " + p);
            }
        }
        for (int x = -42; x <= 41; x++) for (int z = -42; z <= 41; z++) {
            var floor = state.apply(relative(x, 2, z));
            h.assertFalse(floor.is(Blocks.GRASS_BLOCK) || floor.is(Blocks.FARMLAND),
                    "The complete central courtyard must stay path or authored tent flooring");
        }
    }

    static void exportNaturalViews(ServerLevel level, SavannaOrcCampPiece piece) throws java.io.IOException {
        level.setDayTime(6000);
        var center = piece.templatePosition().offset(SavannaOrcVillage.CENTER, 2, SavannaOrcVillage.CENTER);
        var counts = new java.util.TreeMap<String, Integer>();
        for (int x = -98; x <= 98; x++) for (int z = -98; z <= 98; z++) {
            var root = center.offset(x, 1, z);
            if (level.getBlockState(root).is(Blocks.ACACIA_LOG)
                    && (level.getBlockState(root.below()).is(Blocks.GRASS_BLOCK) || level.getBlockState(root.below()).is(Blocks.DIRT))
                    && !SavannaCampVegetation.reserved(piece, root, true)) {
                var chunk = new ChunkPos(root); counts.merge(chunk.x + "," + chunk.z, 1, Integer::sum);
            }
        }
        Files.writeString(Path.of("camp-nov16-natural-tree-counts.json"), new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(counts));
        List<Map<String, Object>> views = List.of(
                view("camp_nov16_overhead", center, 0, 110, 78, 180, 58),
                view("camp_nov16_gate", center, -.5, 5, 107, 180, 8),
                view("camp_nov16_corrected_tent", center, -.5, 6, -14, 0, 20),
                view("camp_nov16_tree_lawn", center, 91, 15, 91, 135, 32));
        Files.writeString(Path.of("camp-nov16-natural-views.json"), new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(views));
        AOEMod.LOGGER.info("November16 natural camp: {} native roots across {} chunks: {}", counts.values().stream().mapToInt(Integer::intValue).sum(), counts.size(), counts);
        level.getServer().saveEverything(true, true, true);
    }

    private static Map<String, Object> view(String label, BlockPos center, double dx, double dy, double dz, float yaw, float pitch) {
        return Map.of("label", label, "dimension", GameplayDimensions.SAVANNA.location().toString(),
                "x", center.getX() + dx, "y", center.getY() + dy, "z", center.getZ() + dz, "yaw", yaw, "pitch", pitch, "dayTime", 6000);
    }

    private static int mirror(int value, int sign) { return sign > 0 ? value : -value - 1; }
    private static BlockPos relative(int x, int y, int z) { return new BlockPos(x + SavannaOrcVillage.CENTER, y, z + SavannaOrcVillage.CENTER); }
    private static BlockPos gate(int direction, int across, int y) {
        int x = across, z = 97;
        for (int i = 0; i < direction; i++) { int previous = x; x = -z - 1; z = previous; }
        return relative(x, y, z);
    }
    private static BlockState defaultGround(BlockPos pos) {
        return pos.getY() == 2 ? Blocks.GRASS_BLOCK.defaultBlockState() : Blocks.AIR.defaultBlockState();
    }
    private static Map<BlockPos, BlockState> blocks(ServerLevel level, ResourceLocation id) {
        var data = level.getStructureManager().getOrCreate(id).save(new CompoundTag());
        var palette = data.getList("palette", Tag.TAG_COMPOUND);
        var result = new HashMap<BlockPos, BlockState>();
        for (var entry : data.getList("blocks", Tag.TAG_COMPOUND)) {
            var block = (CompoundTag) entry; var pos = block.getList("pos", Tag.TAG_INT);
            result.put(new BlockPos(pos.getInt(0), pos.getInt(1), pos.getInt(2)),
                    NbtUtils.readBlockState(level.holderLookup(Registries.BLOCK), palette.getCompound(block.getInt("state"))));
        }
        return result;
    }
}

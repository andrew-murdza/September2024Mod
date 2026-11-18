package net.amurdza.examplemod.gametest;

import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.util.GameplayDimensions;
import net.amurdza.examplemod.worldgen.structure.IllagerMansionProcessor;
import net.amurdza.examplemod.worldgen.structure.MansionEnvelopePiece;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@GameTestHolder(AOEMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class MansionUpperStairRevisionTests {
    @GameTest(template = "plant_gameplay_empty")
    public static void nativeStairRepairsPreserveThreeWideRoutesInEveryOrientation(GameTestHelper helper) {
        var level = helper.getLevel().getServer().getLevel(GameplayDimensions.PLAINS);
        for (String name : List.of("entrance", "1x2_c_stairs", "1x2_d_stairs")) {
            var template = level.getStructureManager().getOrCreate(new ResourceLocation("minecraft:woodland_mansion/" + name));
            var original = blocks(template);
            for (Rotation rotation : Rotation.values()) for (Mirror mirror : Mirror.values()) {
                var settings = new StructurePlaceSettings().setRotation(rotation).setMirror(mirror).addProcessor(IllagerMansionProcessor.INSTANCE);
                var processed = StructureTemplate.processBlockInfos(level, BlockPos.ZERO, BlockPos.ZERO, settings, original, template);
                Map<BlockPos, BlockState> states = new HashMap<>();
                processed.forEach(info -> states.put(info.pos(), info.state()));
                if (name.equals("entrance")) {
                    for (int z : new int[]{0, 14}) helper.assertTrue(at(states, settings, 7, 9, z).isAir(), "Remove both isolated upstairs stone posts");
                    for (int y = 6; y <= 8; y++) for (int z = 5; z <= 9; z++)
                        helper.assertTrue(at(states, settings, 1, y, z).is(Blocks.COBBLESTONE), "Complete the stone back of the split staircase");
                    for (int x = 2; x <= 4; x++) for (int z = 6; z <= 8; z++) {
                        helper.assertTrue(at(states, settings, x, 5, z).is(Blocks.COBBLESTONE)
                                        && at(states, settings, x, 6, z).is(Blocks.RED_CARPET)
                                        && at(states, settings, x, 7, z).isAir(),
                                "Keep the main stair's carpeted middle landing and headroom");
                    }
                } else {
                    int z = name.equals("1x2_c_stairs") ? 13 : 1;
                    for (int x : new int[]{1, 5}) helper.assertTrue(
                                    at(states, settings, x, 9, z).is(name.equals("1x2_c_stairs") ? Blocks.BIRCH_PLANKS : Blocks.COBBLESTONE)
                                            && at(states, settings, x, 10, z).is(Blocks.BIRCH_STAIRS)
                                            && at(states, settings, x, 11, z).isAir() && at(states, settings, x, 12, z).isAir(),
                            "Fill missing support beneath the native corner stairs without adding posts above the landing");
                    for (int x = 2; x <= 4; x++) helper.assertTrue(at(states, settings, x, 10, z).is(Blocks.BIRCH_STAIRS)
                                    && at(states, settings, x, 11, z).isAir(),
                            "Keep the three-wide central ascending treads and their headroom");
                }
            }
        }
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void serializedStairConnectionsPreserveLandingSupportCarpetAndTreads(GameTestHelper helper) {
        var level = helper.getLevel();
        BlockPos base = helper.absolutePos(new BlockPos(4, 2, 4));
        BlockPos tread = base.east(), landing = base.east(2), carpet = landing.above(), obstruction = base.east(3);
        level.setBlock(tread, Blocks.COBBLESTONE_STAIRS.defaultBlockState(), 18);
        level.setBlock(landing, Blocks.COBBLESTONE.defaultBlockState(), 18);
        level.setBlock(carpet, Blocks.RED_CARPET.defaultBlockState(), 18);
        level.setBlock(obstruction, Blocks.OAK_PLANKS.defaultBlockState(), 18);
        BlockPos white = base.south(2);
        level.setBlock(white.below(), Blocks.BIRCH_PLANKS.defaultBlockState(), 18);
        level.setBlock(white, Blocks.WHITE_CARPET.defaultBlockState(), 18);
        BoundingBox bounds = BoundingBox.fromCorners(base.offset(-1, -1, -1), base.offset(4, 3, 3));
        var context = StructurePieceSerializationContext.fromLevel(level);
        var piece = new MansionEnvelopePiece(bounds, false, List.of(new MansionEnvelopePiece.Passage(bounds, true, true)), List.of(white));
        var restored = new MansionEnvelopePiece(context, piece.createTag(context));
        restored.postProcess(level, level.structureManager(), level.getChunkSource().getGenerator(), RandomSource.create(0), bounds, new ChunkPos(base), base);
        helper.assertTrue(level.getBlockState(tread).is(Blocks.COBBLESTONE_STAIRS)
                        && level.getBlockState(landing).is(Blocks.COBBLESTONE)
                        && level.getBlockState(carpet).is(Blocks.RED_CARPET),
                "Passage clearing must preserve the whole native stair landing, not only isolated stair blocks");
        helper.assertTrue(level.isEmptyBlock(obstruction), "A genuine wall obstruction still needs its opening");
        helper.assertTrue(level.getBlockState(white).is(Blocks.RED_CARPET), "Saved corridor intersections must repair internal white borders");
        helper.succeed();
    }

    private static BlockState at(Map<BlockPos, BlockState> states, StructurePlaceSettings settings, int x, int y, int z) {
        return states.get(StructureTemplate.calculateRelativePosition(settings, new BlockPos(x, y, z)));
    }

    private static List<StructureTemplate.StructureBlockInfo> blocks(StructureTemplate template) {
        CompoundTag data = template.save(new CompoundTag());
        List<BlockState> palette = new ArrayList<>();
        for (Tag entry : data.getList("palette", Tag.TAG_COMPOUND)) palette.add(NbtUtils.readBlockState(BuiltInRegistries.BLOCK.asLookup(), (CompoundTag) entry));
        List<StructureTemplate.StructureBlockInfo> result = new ArrayList<>();
        for (Tag entry : data.getList("blocks", Tag.TAG_COMPOUND)) {
            CompoundTag block = (CompoundTag) entry;
            var pos = block.getList("pos", Tag.TAG_INT);
            result.add(new StructureTemplate.StructureBlockInfo(new BlockPos(pos.getInt(0), pos.getInt(1), pos.getInt(2)),
                    palette.get(block.getInt("state")), block.contains("nbt") ? block.getCompound("nbt") : null));
        }
        return result;
    }
}

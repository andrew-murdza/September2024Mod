package net.amurdza.examplemod.gametest;

import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.worldgen.PlainsSurfaceWater;
import net.amurdza.examplemod.worldgen.PlainsWaterProof;
import net.amurdza.examplemod.worldgen.structure.MansionBasementPiece;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Font;
import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.util.HashMap;

@GameTestHolder(AOEMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class MansionBasementTests {
    @GameTest(template = "plant_gameplay_empty", timeoutTicks = 600)
    public static void basementSurvivesChunkClippingAndWorldCleanup(GameTestHelper helper) throws Exception {
        // Use the ordinary test dimension to avoid generating a region of unrelated Plains terrain.
        var level = helper.getLevel();
        var entrance = helper.absolutePos(new BlockPos(128, 0, 128)).atY(68);
        // Keep this exhaustive previous-blueprint regression after introducing v3.
        var original = new MansionBasementPiece(entrance, Rotation.CLOCKWISE_90, 2);
        var serial = StructurePieceSerializationContext.fromLevel(level);
        var piece = (MansionBasementPiece) MansionBasementPiece.load(serial, original.createTag(serial));
        var box = piece.getBoundingBox();
        // Different selected batches shift helper origins. Reset the complete owned
        // fixture, including cells unused by the new compact footprint, so old rooms
        // and opened chests from an overlapping earlier layout cannot pollute it.
        for (BlockPos pos : BlockPos.betweenClosed(box.minX(), box.minY(), box.minZ(), box.maxX(), box.maxY(), box.maxZ())) {
            if (!level.getBlockState(pos).isAir()) {
                if (level.getBlockEntity(pos) != null) level.getChunkAt(pos).removeBlockEntity(pos);
                level.setBlock(pos, Blocks.AIR.defaultBlockState(), 18);
            }
        }
        for (int cx = box.maxX() >> 4; cx >= box.minX() >> 4; cx--) for (int cz = box.minZ() >> 4; cz <= box.maxZ() >> 4; cz++) {
            var chunk = new ChunkPos(cx, cz);
            var clip = new BoundingBox(chunk.getMinBlockX(), level.getMinBuildHeight(), chunk.getMinBlockZ(),
                    chunk.getMaxBlockX(), level.getMaxBuildHeight() - 1, chunk.getMaxBlockZ());
            piece.postProcess(level, level.structureManager(), level.getChunkSource().getGenerator(),
                    RandomSource.create(72), clip, chunk, entrance);
        }
        var counts = new HashMap<Block, Integer>();
        for (int x = -30; x <= 29; x++) for (int z = 14; z <= 65; z++) {
            var pos = piece.world(x, 1, z);
            var state = level.getBlockState(pos);
            counts.merge(state.getBlock(), 1, Integer::sum);
            if (state.is(Blocks.ATTACHED_PUMPKIN_STEM)) {
                var fruit = pos.relative(state.getValue(BlockStateProperties.HORIZONTAL_FACING));
                helper.assertTrue(level.getBlockState(fruit).is(Blocks.PUMPKIN), "Detached fruit at " + pos);
            }
            if (state.is(Blocks.CHEST)) {
                var partner = level.getBlockState(pos.relative(ChestBlock.getConnectedDirection(state)));
                helper.assertTrue(state.getValue(ChestBlock.TYPE) != ChestType.SINGLE && partner.is(Blocks.CHEST)
                        && state.getValue(ChestBlock.TYPE) != partner.getValue(ChestBlock.TYPE), "Missing double chest at " + pos + ": " + state + ", partner=" + partner);
            }
        }
        for (Block crop : new Block[]{Blocks.WHEAT, Blocks.CARROTS, Blocks.POTATOES, Blocks.BEETROOTS,
                net.minecraftforge.registries.ForgeRegistries.BLOCKS.getValue(new net.minecraft.resources.ResourceLocation("farmersdelight", "cabbages"))}) {
            helper.assertTrue(counts.getOrDefault(crop, 0) == 96, "Expected two 48-plant rooms of " + crop + ": " + counts.get(crop));
        }
        helper.assertTrue(counts.getOrDefault(Blocks.ATTACHED_PUMPKIN_STEM, 0) == 24 && counts.getOrDefault(Blocks.PUMPKIN, 0) == 24,
                "The two 4x6 pumpkin plots must have one attached stem per fruit");
        helper.assertTrue(counts.getOrDefault(Blocks.CHEST, 0) == 34 && counts.getOrDefault(Blocks.ANVIL, 0) == 14,
                "The granary must have 13 double chests, and the smithy four double chests and fourteen anvils");
        helper.assertTrue(counts.getOrDefault(Blocks.HAY_BLOCK, 0) == 8 && counts.getOrDefault(Blocks.OAK_FENCE_GATE, 0) == 4
                        && counts.getOrDefault(Blocks.SUGAR_CANE, 0) == 18, "Coop and cane layout");
        for (int step = 0; step < 10; step++) {
            helper.assertTrue(level.getBlockState(piece.world(0, 10 - step, step)).is(Blocks.STONE_BRICK_STAIRS)
                    && level.isEmptyBlock(piece.world(0, 12 - step, step)), "Blocked staircase at " + step);
        }
        helper.assertTrue(level.getBlockState(piece.firstDoor()).is(Blocks.IRON_DOOR)
                && level.getBlockState(piece.secondDoor()).is(Blocks.IRON_DOOR), "Both halves of the paired entrance must be present");
        for (int z : new int[]{25, 30, 34, 39}) {
            for (int side = 28; side < 44; side++) for (int y = 1; y <= 3; y++)
                helper.assertTrue(level.getBlockState(piece.world(-1-side, y, z)).getCollisionShape(level, piece.world(-1-side, y, z)).isEmpty(),
                        "The west mine must remain a clear sixteen-block horizontal tunnel at " + side + "/" + y + "/" + z);
        }
        for (int z : new int[]{34, 39}) {
            for (int side = 28; side < 44; side++) helper.assertTrue(level.isEmptyBlock(piece.world(side, 1, z)),
                    "Incomplete eastern horizontal mine at " + z + "/" + side);
        }
        helper.assertTrue(!piece.isBuiltCell(piece.world(44, 2, 34)), "The natural stone end of a mine must remain mineable");
        for (int z = 16; z <= 21; z++) {
            helper.assertTrue(level.getBlockState(piece.world(-22, 1, z)).is(Blocks.PUMPKIN)
                            && level.getBlockState(piece.world(-24, 1, z)).is(Blocks.PUMPKIN),
                    "Both pumpkin rows adjoining the water must be fruit, with mirrored stems behind them");
        }
        helper.assertTrue(level.getBlockState(piece.world(-23, 0, 36)).is(Blocks.WATER), "Mushrooms require the same central irrigation channel");
        for (int across = 0; across < 8; across++)
            helper.assertTrue(level.getBlockState(piece.world(27, 0, 15 + across)).is(Blocks.STONE_BRICKS), "Hay needs a brick foundation");
        for (int z : new int[]{23, 32, 41}) helper.assertTrue(level.getBlockState(piece.world(-8, 1, z)).is(Blocks.STONE_BRICKS)
                        && !level.getBlockState(piece.world(-8, 1, z - 1)).is(Blocks.STONE_BRICKS)
                        && !level.getBlockState(piece.world(-8, 1, z + 1)).is(Blocks.STONE_BRICKS), "Row dividers must be one block thick");
        helper.assertTrue(level.isEmptyBlock(piece.world(16, 1, 27)) && level.isEmptyBlock(piece.world(17, 1, 27)),
                "The former transverse gallery must be one direct doorway through the shared wall");
        helper.assertTrue(level.getBlockState(piece.world(-21, 0, 45)).is(Blocks.SUGAR_CANE)
                        && level.getBlockState(piece.world(-21, 0, 45)).getValue(BlockStateProperties.WATERLOGGED),
                "Cane must be rooted underwater over the sand floor");
        var water = piece.world(-11, 0, 18);
        helper.assertTrue(level.getBlockState(water.below()).is(Blocks.STONE_BRICKS)
                && PlainsWaterProof.contains(level.getChunkAt(water), water), "Channel floor and water reservation must survive reload");
        var owner = new ChunkPos(water);
        PlainsSurfaceWater.drainDisconnected(level, owner.getMinBlockX(), owner.getMinBlockZ());
        helper.assertTrue(level.getBlockState(water).is(Blocks.WATER), "Enclosed irrigation drained during the Plains cleanup pass");
        String preview = System.getenv("AOE_MANSION_PLAN");
        if (preview != null && !preview.isBlank()) drawPlan(level, piece, Path.of(preview));
        var views = new java.util.ArrayList<java.util.Map<String, Object>>();
        for (int[] view : new int[][]{{0, 2, 16, 0}, {-13, 2, 34, 90}, {22, 2, 44, -90}, {0, 2, 55, 0}}) {
            var camera = piece.world(view[0], view[1], view[2]);
            var forward = piece.world(0, 0, 1).subtract(piece.world(0, 0, 0));
            float yaw = (float) Math.toDegrees(Math.atan2(-forward.getX(), forward.getZ())) + view[3];
            views.add(java.util.Map.of("label", "mansion-cellar-" + views.size(), "dimension", level.dimension().location().toString(),
                    "x", camera.getX() + .5, "y", camera.getY() + .1, "z", camera.getZ() + .5, "yaw", yaw, "pitch", 10));
        }
        java.nio.file.Files.writeString(Path.of("mansion-basement-views.json"), new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(views));
        helper.succeed();
    }

    private static void drawPlan(ServerLevel level, MansionBasementPiece piece, Path path) throws Exception {
        int scale = 11, left = 56, top = 65;
        var image = new BufferedImage(1120, 830, BufferedImage.TYPE_INT_RGB);
        var g = image.createGraphics();
        g.setColor(new Color(0x192126)); g.fillRect(0, 0, image.getWidth(), image.getHeight());
        g.setColor(Color.WHITE); g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 24));
        g.drawString("Plains mansion basement", left, 38);
        for (int x = -45; x <= 44; x++) for (int z = 0; z <= 65; z++) {
            var pos = piece.world(x, 1, z);
            var wall = level.getBlockState(pos); var soil = level.getBlockState(pos.below());
            int color = wall.is(Blocks.STONE_BRICKS) ? 0x59616b : 0xcccbc4;
            if (!piece.containsAuthoredSpace(pos)) color = 0x192126;
            else if (z < 10 && x >= -2 && x <= 1) color = 0xb7c9d7;
            else if (soil.is(Blocks.WATER)) color = 0x427dba;
            else if (soil.is(Blocks.PODZOL)) color = 0x815a45;
            else if (soil.is(Blocks.GRASS_BLOCK)) color = 0x7e9450;
            else if (wall.is(Blocks.PUMPKIN)) color = 0xc47d36;
            else if (wall.is(Blocks.ATTACHED_PUMPKIN_STEM)) color = 0x96884c;
            else if (wall.is(Blocks.SUGAR_CANE)) color = 0x98b961;
            else if (soil.is(Blocks.FARMLAND)) color = wall.is(Blocks.WHEAT) ? 0xc4b75c : 0x67824a;
            else if (wall.is(Blocks.CHEST)) color = 0xb3834d;
            else if (wall.is(Blocks.FURNACE) || wall.is(Blocks.BLAST_FURNACE) || wall.is(Blocks.ANVIL)) color = 0x3b454c;
            else if (wall.is(Blocks.HAY_BLOCK)) color = 0xc2a23d;
            else if (wall.is(Blocks.OAK_FENCE) || wall.is(Blocks.CRAFTING_TABLE)) color = 0x906549;
            else if (wall.is(Blocks.IRON_DOOR)) color = 0xdce4e6;
            else if (soil.is(Blocks.LADDER)) color = 0x493e34;
            g.setColor(new Color(color)); g.fillRect(left + (x + 45) * scale, top + z * scale, scale, scale);
        }
        String[][] labels = {{"Pumpkins","Wheat","Cabbage","Chicken coop"},
                {"Cabbage","Carrots","Beetroot","Granary"},{"Mushrooms","Beetroot","Wheat","Potatoes"},
                {"Sugar cane","Potatoes","Carrots","Kitchen"}};
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 13));
        for (int r = 0; r < 4; r++) for (int c = 0; c < 4; c++) {
            int x = left + (new int[]{22, 34, 55, 67}[c]) * scale;
            int y = top + (19 + r*9) * scale, w = g.getFontMetrics().stringWidth(labels[r][c]);
            g.setColor(new Color(0x253034)); g.fillRoundRect(x-w/2-4, y-13, w+8, 18, 4, 4);
            g.setColor(Color.WHITE); g.drawString(labels[r][c], x-w/2, y);
        }
        g.setColor(Color.WHITE); g.drawString("Stairs and landing", left + 38*scale, top + 6*scale);
        g.drawString("Blacksmith", left + 40*scale, top + 62*scale);
        g.dispose(); ImageIO.write(image, "png", path.toFile());
    }
}

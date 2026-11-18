package net.amurdza.examplemod.gametest;

import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.worldgen.structure.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.pieces.PiecesContainer;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.structure.structures.WoodlandMansionPieces.WoodlandMansionPiece;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.List;

@GameTestHolder(AOEMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class MansionNovember15RoundTwoTests {
    @GameTest(template = "plant_gameplay_empty", timeoutTicks = 600)
    public static void rearrangedRoomsKeepMirroredCropsConnectedPensAndInwardLighting(GameTestHelper h) {
        var serial = StructurePieceSerializationContext.fromLevel(h.getLevel());
        for (var rotation : Rotation.values()) {
            var original = new MansionBasementPiece(new BlockPos(8, 150, 8), rotation, 5);
            var piece = (MansionBasementPiece) MansionBasementPiece.load(serial, original.createTag(serial));
            h.assertTrue(piece.layoutVersion() == 5, "The authored cellar requires its own saved blueprint version");
            var blocks = MansionBasementV3Tests.place(h, piece);
            java.util.function.Function<BlockPos, BlockState> at = pos -> blocks.getOrDefault(pos, Blocks.AIR.defaultBlockState());
            for (int row = 0; row < 5; row++) for (boolean outer : new boolean[]{false, true}) for (boolean east : new boolean[]{false, true}) {
                var room = MansionBasementLayoutV5.room(east, row, outer);
                int side = east ? 1 : -1, firstX = outer ? 19 : 7, firstZ = 14 + row * 12;
                int lamps = 0, crops = 0, podzol = 0, submergedCane = 0;
                for (int depth = 0; depth < 11; depth++) for (int across = 0; across < 11; across++) {
                    var pos = piece.world(side * (firstX + depth), 1, firstZ + across);
                    if (at.apply(pos.above(3)).is(Blocks.LANTERN)) lamps++;
                    if (at.apply(pos).getBlock() instanceof CropBlock) crops++;
                    if (at.apply(pos.below()).is(Blocks.PODZOL)) podzol++;
                    if (at.apply(pos.below()).is(Blocks.SUGAR_CANE)
                            && at.apply(pos.below()).getValue(BlockStateProperties.WATERLOGGED)) submergedCane++;
                    h.assertTrue(at.apply(pos.above(5)).is(Blocks.STONE_BRICKS), "A room has a hole in its six-high ceiling");
                }
                h.assertTrue(lamps == 5, "Each 11x11 room needs exactly five lanterns: " + room + "/" + lamps);
                for (int depth : new int[]{2, 8}) for (int across : new int[]{2, 8})
                    h.assertTrue(at.apply(piece.world(side * (firstX + depth), 4, firstZ + across)).is(Blocks.LANTERN),
                            "Corner lanterns must sit three diagonal block steps inward from the walls");
                if (room == MansionBasementLayoutV5.Room.MUSHROOM) h.assertTrue(podzol == 64, "Preserve the player's irrigated mushroom quadrants");
                if (room == MansionBasementLayoutV5.Room.CANE) h.assertTrue(submergedCane == 24, "Preserve all three submerged cane rows");
                if (!outer) {
                    h.assertTrue(crops == 64, "Each of the ten paired crop rooms needs its complete authored 64 plants: " + room + "/" + crops);
                    h.assertTrue(room == MansionBasementLayoutV5.room(!east, row, outer), "Matching crops must face each other across the hall");
                }
                for (int a = 4; a <= 6; a++) for (int y = 1; y <= 3; y++) {
                    for (int depth : new int[]{-1, 0}) assertPassable(h, at.apply(piece.world(side * (firstX + depth), y, firstZ + a)), "Room entrance " + room);
                    if (!outer) for (int depth : new int[]{10, 11})
                        assertPassable(h, at.apply(piece.world(side * (firstX + depth), y, firstZ + a)), "Connection to outer room " + room);
                }
                if (row < 4 && MansionBasementLayoutV5.sideEntrance(room)
                        && MansionBasementLayoutV5.sideEntrance(MansionBasementLayoutV5.room(east, row + 1, outer)))
                    for (int d = 4; d <= 6; d++) for (int a = 10; a <= 12; a++) for (int y = 1; y <= 3; y++)
                        assertPassable(h, at.apply(piece.world(side * (firstX + d), y, firstZ + a)), "Side doorway behind " + room);
            }
            for (int row = 0; row < 5; row++) {
                h.assertTrue(MansionBasementLayoutV5.pen(MansionBasementLayoutV5.room(true, row, true)) == (row < 4),
                        "Four pens must occupy consecutive outer rooms, followed by the smith");
                h.assertTrue(!MansionBasementLayoutV5.pen(MansionBasementLayoutV5.room(false, row, true)),
                        "The opposite outer column must contain the three farms, kitchen and granary");
                for (int d = 0; d < 11; d++) for (int a = 0; a < 11; a++) {
                    var pos = piece.world(19 + d, 0, 14 + row * 12 + a);
                    if (row < 4 && at.apply(pos.above()).is(Blocks.HAY_BLOCK))
                        h.assertTrue(at.apply(pos).is(Blocks.STONE_BRICKS), "Hay bales must keep their brick foundation");
                    if (row < 4 && !at.apply(pos.above()).is(Blocks.HAY_BLOCK))
                        h.assertTrue(!at.apply(pos).is(Blocks.STONE_BRICKS), "Grass must fill the gaps between hay, including removed doorway hay");
                }
                if (row < 4 && (!MansionBasementLayoutV5.sideEntrance(MansionBasementLayoutV5.room(false, row, true))
                        || !MansionBasementLayoutV5.sideEntrance(MansionBasementLayoutV5.room(false, row + 1, true))))
                    for (int x = -29; x <= -19; x++) for (int y = 1; y <= 3; y++)
                        h.assertTrue(!at.apply(piece.world(x, y, 25 + row * 12)).isAir(),
                                "Kitchen and granary side walls must stay closed");
            }
            for (int x = -5; x <= 5; x++) for (int z = 14; z <= 72; z++) {
                h.assertTrue(at.apply(piece.world(x, 1, z)).isAir(), "The entire central hall must be eleven blocks wide");
                int light = 0;
                for (int lx : new int[]{-3, 0, 3}) for (int lz = 16; lz <= 72; lz += 4)
                    if (at.apply(piece.world(lx, 4, lz)).is(Blocks.LANTERN))
                        light = Math.max(light, 15 - Math.abs(x - lx) - Math.abs(z - lz) - 3);
                h.assertTrue(light >= 8, "Hall floor-air illumination falls below eight at " + x + "/" + z + ": " + light);
            }
            for (int x : new int[]{-2, 2}) h.assertTrue(at.apply(piece.world(x, 8, 6)).is(Blocks.WALL_TORCH),
                    "The authored staircase needs a wall torch on each side");
            for (int z = 6; z < 10; z++) h.assertTrue(at.apply(piece.world(0, 15 - z, z)).is(Blocks.STONE_BRICKS)
                    && at.apply(piece.world(0, 14 - z, z)).isAir(), "The stair ceiling must be one block higher");
            for (int step = 0; step < 10; step++) for (int x = -2; x <= 2; x++)
                h.assertTrue(at.apply(piece.world(x, 10 - step, step)).is(Blocks.STONE_BRICK_STAIRS)
                                && at.apply(piece.world(x, 12 - step, step)).isAir(), "All five authored stair lanes need ascending headroom");
            for (var entry : blocks.entrySet()) if (entry.getValue().is(Blocks.CHEST)
                    && entry.getValue().getValue(ChestBlock.TYPE) != ChestType.SINGLE) {
                var partner = at.apply(entry.getKey().relative(ChestBlock.getConnectedDirection(entry.getValue())));
                h.assertTrue(partner.is(Blocks.CHEST) && partner.getValue(ChestBlock.TYPE) != entry.getValue().getValue(ChestBlock.TYPE),
                        "A doorway removed only half of a rotated double chest at " + entry.getKey());
            }
            h.assertTrue(MansionBasementLayoutV5.animals().size() == 16
                    && MansionBasementLayoutV5.animals().stream().map(MansionBasementLayoutV5.Animal::id).distinct().count() == 4,
                    "Four separate pens must each receive their own four animals");
            for (int x = -30; x <= 30; x++) h.assertTrue(at.apply(piece.world(x, 1, 73)).is(Blocks.STONE_BRICKS),
                    "There must be no outward room doorway through the rear perimeter");
        }
        h.succeed();
    }

    @GameTest(template = "plant_gameplay_empty", timeoutTicks = 240)
    public static void raisedEntranceCeilingAndAuthoredLightsContainWaterAfterFluidTicks(GameTestHelper h) {
        var level = h.getLevel();
        var cellar = new MansionBasementPiece(h.absolutePos(new BlockPos(1024, 0, 1024)).atY(180), Rotation.NONE, 5);
        var low = cellar.world(-8, -2, 7);
        var high = cellar.world(8, 8, 19);
        var clip = BoundingBox.fromCorners(low, high);
        var forced = new java.util.ArrayList<ChunkPos>();
        for (int x = clip.minX() >> 4; x <= clip.maxX() >> 4; x++) for (int z = clip.minZ() >> 4; z <= clip.maxZ() >> 4; z++) {
            var chunk = new ChunkPos(x, z);
            if (!level.getForcedChunks().contains(chunk.toLong())) {
                level.setChunkForced(x, z, true);
                forced.add(chunk);
            }
        }
        for (var pos : BlockPos.betweenClosed(low, high)) {
            boolean edge = pos.getX() == low.getX() || pos.getX() == high.getX() || pos.getZ() == low.getZ()
                    || pos.getZ() == high.getZ() || pos.getY() == low.getY() || pos.getY() == high.getY();
            level.setBlock(pos, (edge ? Blocks.STONE : Blocks.AIR).defaultBlockState(), 18);
        }
        for (int x = clip.minX() >> 4; x <= clip.maxX() >> 4; x++) for (int z = clip.minZ() >> 4; z <= clip.maxZ() >> 4; z++) {
            var chunk = new ChunkPos(x, z);
            var chunkClip = new BoundingBox(Math.max(clip.minX(), chunk.getMinBlockX()), clip.minY(), Math.max(clip.minZ(), chunk.getMinBlockZ()),
                    Math.min(clip.maxX(), chunk.getMaxBlockX()), clip.maxY(), Math.min(clip.maxZ(), chunk.getMaxBlockZ()));
            cellar.postProcess(level, level.structureManager(), level.getChunkSource().getGenerator(), net.minecraft.util.RandomSource.create(0),
                    chunkClip, chunk, cellar.keyAnchor());
        }
        for (int x : new int[]{-4, 4}) for (int y = 1; y <= 3; y++) {
            var water = cellar.world(x, y, 8);
            level.setBlock(water, Blocks.WATER.defaultBlockState(), 3);
            level.scheduleTick(water, net.minecraft.world.level.material.Fluids.WATER, 5);
        }
        var roofWater = cellar.world(0, 7, 12);
        level.setBlock(roofWater, Blocks.WATER.defaultBlockState(), 3);
        level.scheduleTick(roofWater, net.minecraft.world.level.material.Fluids.WATER, 5);
        for (int x = -5; x <= 5; x++) for (int z = 10; z <= 12; z++) {
            h.assertTrue(level.getBlockState(cellar.world(x, 6, z)).is(Blocks.STONE_BRICKS), "Landing ceiling must match the six-high central hall");
            h.assertTrue(level.getBlockState(cellar.world(x, 5, z)).isAir()
                    || level.getBlockState(cellar.world(x, 5, z)).is(Blocks.CHAIN), "The previous low ceiling must be removed");
        }
        for (int x : new int[]{-3, 3}) h.assertTrue(level.getBlockState(cellar.world(x, 4, 11)).is(Blocks.LANTERN),
                "Two authored lanterns must hang outside the locked door");
        h.runAfterDelay(80, () -> {
            try {
                h.assertTrue(!level.getFluidState(cellar.world(0, 7, 11)).isEmpty(), "The fixture must execute real water propagation");
                for (int x = -5; x <= 5; x++) for (int z = 10; z <= 18; z++) for (int y = 1; y <= 3; y++) {
                    var point = cellar.world(x, y, z);
                    h.assertTrue(level.getFluidState(point).isEmpty(), "Water crossed a widened authored landing shoulder or the locked header at " + point);
                }
                for (int x = -5; x <= 5; x++) for (int z = 14; z <= 18; z++) {
                    var point = cellar.world(x, 1, z);
                    int light = level.getBrightness(net.minecraft.world.level.LightLayer.BLOCK, point);
                    h.assertTrue(light >= 8, "Actual hallway floor-air block light must be at least eight: " + point + " = " + light);
                }
                h.succeed();
            } finally {
                for (var chunk : forced) level.setChunkForced(chunk.x, chunk.z, false);
            }
        });
    }

    @GameTest(template = "plant_gameplay_empty", timeoutTicks = 300)
    public static void eachRoomTileHasALoweredChandelierWithoutRemovingFurniture(GameTestHelper h) {
        var level = h.getLevel().getServer().getLevel(net.amurdza.examplemod.util.GameplayDimensions.PLAINS);
        var names = new java.util.HashSet<>(MansionRoomCatalogue.NORMAL);
        names.add(MansionRoomCatalogue.FALLBACK);
        names.remove("1x2_d2");
        for (String name : names) {
            var template = level.getStructureManager().getOrCreate(new net.minecraft.resources.ResourceLocation("minecraft", "woodland_mansion/" + MansionRoomCatalogue.compact(name)));
            var before = template.save(new CompoundTag());
            var palette = before.getList("palette", net.minecraft.nbt.Tag.TAG_COMPOUND);
            var original = new java.util.ArrayList<net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.StructureBlockInfo>();
            for (var entry : before.getList("blocks", net.minecraft.nbt.Tag.TAG_COMPOUND)) {
                var data = (CompoundTag) entry;
                var pos = data.getList("pos", net.minecraft.nbt.Tag.TAG_INT);
                original.add(new net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.StructureBlockInfo(
                        new BlockPos(pos.getInt(0), pos.getInt(1), pos.getInt(2)),
                        net.minecraft.nbt.NbtUtils.readBlockState(level.holderLookup(Registries.BLOCK), palette.getCompound(data.getInt("state"))),
                        data.contains("nbt") ? data.getCompound("nbt") : null));
            }
            for (var rotation : Rotation.values()) for (var mirror : Mirror.values()) {
                var settings = new net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings()
                        .setRotation(rotation).setMirror(mirror).addProcessor(IllagerMansionProcessor.INSTANCE);
                var blocks = new java.util.HashMap<BlockPos, BlockState>();
                net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.processBlockInfos(
                        level, BlockPos.ZERO, BlockPos.ZERO, settings, original, template).forEach(info -> blocks.put(info.pos(), info.state()));
                for (int x = 3; x < template.getSize().getX(); x += 8) for (int z = 3; z < template.getSize().getZ(); z += 8) {
                    var center = new BlockPos(x, 4, z);
                    h.assertTrue(blocks.get(net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.calculateRelativePosition(settings, center)).is(Blocks.OAK_FENCE),
                            "Every room tile needs a wooden chandelier ending four blocks above its floor: " + name + "/" + center);
                    h.assertTrue(!blocks.get(net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.calculateRelativePosition(settings, center.below())).is(Blocks.OAK_FENCE),
                            "The former chandelier bottom must be clear after raising it: " + name + "/" + center.below());
                    int torches = 0;
                    for (var direction : net.minecraft.core.Direction.Plane.HORIZONTAL) {
                        var point = net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.calculateRelativePosition(settings, center.relative(direction).above());
                        if (blocks.get(point).is(Blocks.TORCH)) torches++;
                    }
                    h.assertTrue(torches >= 2, "The chandelier must retain working lights while fitting the room's furniture: " + name);
                }
                for (var info : original) if (info.state().is(Blocks.BOOKSHELF) || info.state().is(Blocks.WHITE_WOOL)) {
                    var point = net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.calculateRelativePosition(settings, info.pos());
                    h.assertTrue(blocks.get(point).is(info.state().getBlock()), "Lighting removed a bookcase or bed at " + name + "/" + info.pos());
                }
            }
            h.assertTrue(template.save(new CompoundTag()).equals(before), "Processing must not mutate the shared native room template");
        }
        h.succeed();
    }

    private static void assertPassable(GameTestHelper h, BlockState state, String detail) {
        h.assertTrue(state.isAir() || state.getBlock() instanceof FenceGateBlock, detail + " is blocked by " + state);
    }

}

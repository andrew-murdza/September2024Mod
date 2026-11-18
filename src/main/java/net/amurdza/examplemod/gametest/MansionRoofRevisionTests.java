package net.amurdza.examplemod.gametest;

import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.util.GameplayDimensions;
import net.amurdza.examplemod.worldgen.structure.IllagerMansionStructure;
import net.amurdza.examplemod.worldgen.structure.MansionRoofLayout;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.chunk.UpgradeData;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePiecesBuilder;
import net.minecraft.world.level.levelgen.structure.structures.WoodlandMansionPieces.WoodlandMansionPiece;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayList;
import java.util.List;

@GameTestHolder(AOEMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class MansionRoofRevisionTests {
    @GameTest(template = "plant_gameplay_empty", timeoutTicks = 300)
    public static void roofOffsetsMoveThreeDownAndOutAndPersistInNativePieceSaves(GameTestHelper h) {
        var level = h.getLevel();
        var manager = level.getStructureManager();
        var serial = StructurePieceSerializationContext.fromLevel(level);
        for (Rotation rotation : Rotation.values()) {
            for (String name : List.of("roof", "roof_front", "roof_corner", "roof_inner_corner")) {
                var origin = new BlockPos(24, 200, 56);
                var original = new WoodlandMansionPiece(manager, name, origin, rotation);
                var revised = MansionRoofLayout.adapt(manager, name, original);
                var offset = switch (name) {
                    case "roof" -> new BlockPos(-3, -5, -3);
                    case "roof_corner" -> new BlockPos(3, -5, 3);
                    default -> new BlockPos(3, -5, -3);
                };
                h.assertTrue(revised.templatePosition().equals(origin.offset(offset.rotate(rotation))),
                        "Roof must move three down from the former drop-two position and outward along its rotated axes");
                var restored = new WoodlandMansionPiece(manager, revised.createTag(serial));
                h.assertTrue(restored.template() == revised.template() && restored.templatePosition().equals(revised.templatePosition())
                                && restored.getRotation() == rotation,
                        "Roof reload must retain its versioned template, expanded position and rotation");
                if (name.equals("roof")) {
                    h.assertTrue(revised.template().getSize().equals(new net.minecraft.core.Vec3i(14, 2, 14))
                                    && revised.template().filterBlocks(BlockPos.ZERO, new StructurePlaceSettings(), Blocks.AIR).isEmpty(),
                            "The expanded flat plate must contain no AIR capable of cutting the continuous ceiling");
                }
            }
        }
        h.succeed();
    }

    @GameTest(template = "plant_gameplay_empty", timeoutTicks = 800)
    public static void placedRoofsKeepTheFullCeilingAndJoinEveryEaveAcrossChunksInAllRotations(GameTestHelper h) {
        var level = h.getLevel().getServer().getLevel(GameplayDimensions.PLAINS);
        var manager = level.getStructureManager();
        var generator = level.getChunkSource().getGenerator();
        var ceiling = manager.getOrCreate(new net.minecraft.resources.ResourceLocation("minecraft", "woodland_mansion/aoe_v6_ceiling"));
        var front = manager.getOrCreate(new net.minecraft.resources.ResourceLocation("minecraft", "woodland_mansion/" + MansionRoofLayout.FRONT));
        var biome = level.registryAccess().registryOrThrow(Registries.BIOME).getHolderOrThrow(
                ResourceKey.create(Registries.BIOME, AOEMod.makeID("plains")));
        int sample = 0;
        for (Rotation rotation : Rotation.values()) {
            var center = new ChunkPos(4200 + sample++ * 32, 4200);
            var origin = center.getMiddleBlockPosition(150);
            var builder = new StructurePiecesBuilder();
            IllagerMansionStructure.addMansionPieces(manager, origin, rotation, builder, RandomSource.create(17));
            var roofs = new ArrayList<WoodlandMansionPiece>();
            var ceilings = new ArrayList<WoodlandMansionPiece>();
            int previousOrder = -1;
            for (var piece : builder.build().pieces()) if (piece instanceof WoodlandMansionPiece upper) {
                if (upper.template() == ceiling) ceilings.add(upper);
                if (MansionRoofLayout.isRoof(upper)) {
                    int order = MansionRoofLayout.placementOrder(upper);
                    h.assertTrue(order >= previousOrder, "All expanded flat tiles must precede fronts, and fronts must precede corners");
                    previousOrder = order;
                    roofs.add(upper);
                }
            }
            h.assertTrue(ceilings.size() == 121 && roofs.size() == 169, "Exercise the complete eleven-by-eleven roof with all forty-four fronts and four corners");
            var bounds = BoundingBox.encapsulatingBoxes(roofs.stream().map(piece -> piece.getBoundingBox()).toList()).orElseThrow();
            var chunks = new ArrayList<ChunkAccess>();
            for (int z = center.z - 4; z <= center.z + 4; z++) for (int x = center.x - 4; x <= center.x + 4; x++) {
                var chunk = new ProtoChunk(new ChunkPos(x, z), UpgradeData.EMPTY, level,
                        level.registryAccess().registryOrThrow(Registries.BIOME), null);
                chunk.fillBiomesFromNoise((qx, qy, qz, sampler) -> biome, level.getChunkSource().randomState().sampler());
                chunk.setStatus(ChunkStatus.CARVERS);
                chunks.add(chunk);
            }
            var region = new WorldGenRegion(level, chunks, ChunkStatus.FEATURES, 4);
            var structures = level.structureManager().forWorldGenRegion(region);
            var ordered = new ArrayList<>(ceilings);
            ordered.addAll(roofs);
            for (var chunk : chunks) {
                var pos = chunk.getPos();
                var clip = new BoundingBox(pos.getMinBlockX(), 186, pos.getMinBlockZ(), pos.getMaxBlockX(), 196, pos.getMaxBlockZ());
                if (!bounds.intersects(clip)) continue;
                for (var piece : ordered) if (piece.getBoundingBox().intersects(clip))
                    piece.postProcess(region, structures, generator, RandomSource.create(0), clip, pos, origin);
            }
            for (var tile : ceilings) {
                var box = tile.getBoundingBox();
                for (int x = box.minX(); x <= box.maxX(); x++) for (int z = box.minZ(); z <= box.maxZ(); z++) {
                    h.assertTrue(region.getBlockState(new BlockPos(x, 190, z)).is(Blocks.OAK_PLANKS)
                                    && region.getBlockState(new BlockPos(x, 191, z)).is(Blocks.OAK_PLANKS),
                            "Lowered roof AIR damaged the continuous interior oak40/oak41 ceiling at " + x + "/" + z);
                    var cap = region.getBlockState(new BlockPos(x, 192, z));
                    h.assertTrue(cap.is(Blocks.OAK_SLAB) && cap.getValue(BlockStateProperties.SLAB_TYPE) == SlabType.BOTTOM,
                            "The whole lowered flat roof must have a continuous bottom-slab cap at relative Y42");
                }
            }
            for (int x = bounds.minX(); x <= bounds.maxX(); x++) for (int z = bounds.minZ(); z <= bounds.maxZ(); z++) {
                boolean covered = false;
                for (int y = 188; y <= 192; y++) covered |= !region.isEmptyBlock(new BlockPos(x, y, z));
                h.assertTrue(covered, "Expanded roof has a hole where its plate, front strips or corners join: " + rotation + "/" + x + "/" + z);
            }
            int lowestStairs = 0;
            for (var piece : roofs) if (piece.template() == front) {
                for (var stair : front.filterBlocks(BlockPos.ZERO, new StructurePlaceSettings(), Blocks.DARK_OAK_STAIRS)) {
                    if (stair.pos().getY() != 1) continue;
                    var pos = net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate
                            .calculateRelativePosition(piece.placeSettings(), stair.pos()).offset(piece.templatePosition());
                    h.assertTrue(pos.getY() == 189 && region.getBlockState(pos).is(Blocks.OAK_STAIRS)
                                    && region.isEmptyBlock(pos.above()),
                            "The lowest eave must be three lower, with no old slab or extended flat tile above its slope: " + pos);
                    lowestStairs++;
                }
            }
            h.assertTrue(lowestStairs == 616, "Check all forty-four extended fourteen-block eave strips, including their corner overlaps");
        }
        h.succeed();
    }
}

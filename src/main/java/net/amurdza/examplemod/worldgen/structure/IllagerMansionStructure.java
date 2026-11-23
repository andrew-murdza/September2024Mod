package net.amurdza.examplemod.worldgen.structure;

import com.mojang.serialization.Codec;
import net.amurdza.examplemod.registry.ModStructures;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.structures.WoodlandMansionPieces;
import net.minecraft.world.level.levelgen.structure.structures.WoodlandMansionStructure;
import net.minecraft.world.level.levelgen.structure.pieces.PiecesContainer;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePiecesBuilder;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Vanilla mansion rooms and residents, with one persistent lord in an interior room. */
public final class IllagerMansionStructure extends WoodlandMansionStructure {
    public static final Codec<IllagerMansionStructure> CODEC = simpleCodec(IllagerMansionStructure::new);

    public IllagerMansionStructure(StructureSettings settings) { super(settings); }

    @Override public Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
        Rotation rotation = Rotation.getRandom(context.random());
        BlockPos origin = getLowestYIn5by5BoxOffset7Blocks(context, rotation);
        if (origin.getY() < context.chunkGenerator().getSeaLevel()) return Optional.empty();
        var plan = new StructurePiecesBuilder();
        addMansionPieces(context.structureTemplateManager(), origin.atY(0), rotation, plan, context.random());
        var pieces = plan.build().pieces();
        var house = BoundingBox.encapsulatingBoxes(pieces.stream()
                .filter(WoodlandMansionPieces.WoodlandMansionPiece.class::isInstance)
                .map(piece -> piece.getBoundingBox()).toList()).orElseThrow();
        int floor = MansionSiteHeight.highestGround(context, house) + 1;
        if (floor + pieces.stream().mapToInt(piece -> piece.getBoundingBox().maxY()).max().orElse(0)
                >= context.heightAccessor().getMaxBuildHeight()) return Optional.empty();
        return Optional.of(new GenerationStub(origin.atY(floor), builder -> pieces.forEach(piece -> {
            piece.move(0, floor, 0);
            builder.addPiece(piece);
        })));
    }

    public static void addMansionPieces(StructureTemplateManager manager, BlockPos origin, Rotation rotation,
                                        StructurePiecesBuilder builder, RandomSource random) {
        var pieces = new ArrayList<WoodlandMansionPieces.WoodlandMansionPiece>();
        MansionUpperLayout.generate(manager, origin, rotation, pieces, random);
        // Vanilla starts at the entrance. Center the estate on the start chunk
        // so its house and flat forest fit inside the structure-reference radius.
        // The outer ramp continues through the chunk-local terrain pass.
        var footprint = BoundingBox.encapsulatingBoxes(pieces.stream().map(p -> p.getBoundingBox()).toList()).orElseThrow();
        BlockPos midpoint = footprint.getCenter();
        pieces.forEach(piece -> {
            piece.move(origin.getX() - midpoint.getX(), 0, origin.getZ() - midpoint.getZ());
        });
        var envelope = BoundingBox.encapsulatingBoxes(pieces.stream().map(p -> p.getBoundingBox()).toList()).orElseThrow();
        builder.addPiece(new MansionEnvelopePiece(envelope, true, List.of()));
        var end = MansionUpperLayout.basementLanding(manager, pieces);
        var basement = new MansionBasementPiece(end.position(), end.rotation());
        var carpets = MansionRoomAccess.carpetPlan(manager, pieces);
        var passages = MansionRoomAccess.plan(manager, pieces);
        var roofSlabs = MansionRoomAccess.roofSlabs(manager, pieces);
        pieces.forEach(builder::addPiece);
        builder.addPiece(basement);
        builder.addPiece(new MansionEnvelopePiece(envelope, false, passages,
                carpets.red(), carpets.white(), roofSlabs, List.of()).oakFloors());
        var room = pieces.stream().filter(piece -> MansionResidentialPlan.isMapRoom(manager, piece)).findFirst().orElseThrow();
        var encounterId = new java.util.UUID(random.nextLong(), random.nextLong());
        int expected = MansionResidentsPiece.DEFENDERS;
        basement.encounter(encounterId, expected);
        for (var piece : pieces) ((MansionEncounterAccess) piece).aoemod$encounter(new MansionEncounterData(
                encounterId, basement.keyAnchor().subtract(piece.templatePosition()), expected, java.util.Map.of(), true));
        builder.addPiece(new MansionResidentsPiece(MansionResidentialPlan.roomBounds(manager, pieces), encounterId, basement.keyAnchor()));
        var boss = new MansionBossPiece(room.getBoundingBox(), basement.keyAnchor());
        boss.encounter(encounterId, expected, MansionResidentsPiece.RESIDENTS);
        builder.addPiece(boss);
    }

    @Override public BoundingBox adjustBoundingBox(BoundingBox box) {
        return new BoundingBox(box.minX() - MansionGrounds.OUTER_MARGIN, box.minY(), box.minZ() - MansionGrounds.OUTER_MARGIN,
                box.maxX() + MansionGrounds.OUTER_MARGIN, box.maxY(), box.maxZ() + MansionGrounds.OUTER_MARGIN);
    }

    @Override
    public void afterPlace(WorldGenLevel level, StructureManager manager, ChunkGenerator generator,
            RandomSource random, BoundingBox chunk, ChunkPos chunkPos, PiecesContainer pieces) {
        var rooms = pieces.pieces().stream()
                .filter(WoodlandMansionPieces.WoodlandMansionPiece.class::isInstance)
                .map(piece -> piece.getBoundingBox()).toList();
        if (rooms.isEmpty()) return;
        int floorY = rooms.stream().mapToInt(BoundingBox::minY).min().orElseThrow();
        var basement = pieces.pieces().stream().filter(MansionBasementPiece.class::isInstance)
                .map(MansionBasementPiece.class::cast).findFirst().orElse(null);
        if (basement != null) {
            BossStructureBounds.mansion(level.getLevel(), basement, rooms);
            // /place structure places pieces directly, without adding natural structure references.
            if (level instanceof net.minecraft.server.level.ServerLevel server && chunk.isInside(basement.keyAnchor()))
                PlacedMansions.get(server).record(basement, rooms);
            stripExtraChests(level, pieces, chunk);
            MansionGrounds.place(level, generator, chunk, rooms, basement, basement.groundsY());
            if (basement.layoutVersion() >= 2) fillFoundation(level, chunk, rooms, basement, basement.mainFloorY());
        }
        var oldBasement = pieces.pieces().stream().filter(LegacyMansionBasementPiece.class::isInstance)
                .map(piece -> piece.getBoundingBox()).findFirst().orElse(null);
        var overlapping = rooms.stream().filter(room -> room.intersects(chunk)).toList();
        if (overlapping.isEmpty()) return;
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        int terrainTop = Math.min(chunk.maxY(), level.getChunk(chunkPos.x, chunkPos.z).getHighestSectionPosition() + 15);
        // Templates replace terrain only inside their own bounds. A hillside
        // higher than the roof otherwise survives as a grass-and-dirt mound.
        // Use the highest piece in each covered column so upper rooms and roof
        // details survive, while courtyards and neighboring terrain stay intact.
        for (int x = chunk.minX(); x <= chunk.maxX(); x++) {
            for (int z = chunk.minZ(); z <= chunk.maxZ(); z++) {
                int roofY = Integer.MIN_VALUE;
                for (BoundingBox room : overlapping) {
                    if (x >= room.minX() && x <= room.maxX() && z >= room.minZ() && z <= room.maxZ()) {
                        roofY = Math.max(roofY, room.maxY());
                    }
                }
                if (roofY == Integer.MIN_VALUE) continue;
                for (int y = Math.max(roofY + 1, chunk.minY()); y <= terrainTop; y++) {
                    cursor.set(x, y, z);
                    if (!level.getBlockState(cursor).isAir()) level.setBlock(cursor, Blocks.AIR.defaultBlockState(), 2);
                }
            }
        }
        // Follow the ground-floor rooms, not the mansion's rectangular envelope:
        // courtyards and upper-storey overhangs must not become cobblestone towers.
        // Unlike vanilla's air/liquid test, leaves, snow and aquatic plants do
        // not stop these supports above the actual ground.
        for (BoundingBox room : overlapping) {
            if (room.minY() != floorY) continue;
            for (int x = Math.max(room.minX(), chunk.minX()); x <= Math.min(room.maxX(), chunk.maxX()); x++) {
                for (int z = Math.max(room.minZ(), chunk.minZ()); z <= Math.min(room.maxZ(), chunk.maxZ()); z++) {
                    for (int y = Math.min(floorY - 1, chunk.maxY()); y >= Math.max(level.getMinBuildHeight(), chunk.minY()); y--) {
                        cursor.set(x, y, z);
                        if (basement != null && basement.isStairwell(cursor)) break;
                        if (oldBasement != null && oldBasement.isInside(cursor) && x == oldBasement.getCenter().getX()
                                && z == oldBasement.getCenter().getZ()) break;
                        var state = level.getBlockState(cursor);
                        if (state.getFluidState().isEmpty() && !state.is(BlockTags.LEAVES)
                                && state.isCollisionShapeFullBlock(level, cursor)) break;
                        level.setBlock(cursor, (basement != null && basement.layoutVersion() >= 7
                                ? Blocks.STONE : Blocks.COBBLESTONE).defaultBlockState(), 2);
                    }
                }
            }
        }
    }

    private static void fillFoundation(WorldGenLevel level, BoundingBox clip, List<BoundingBox> rooms,
                                       MansionBasementPiece basement, int floor) {
        var house = BoundingBox.encapsulatingBoxes(rooms).orElseThrow();
        var bounds = new BoundingBox(Math.min(house.minX(), basement.getBoundingBox().minX()), level.getMinBuildHeight(),
                Math.min(house.minZ(), basement.getBoundingBox().minZ()), Math.max(house.maxX(), basement.getBoundingBox().maxX()),
                floor - 1, Math.max(house.maxZ(), basement.getBoundingBox().maxZ()));
        var cursor = new BlockPos.MutableBlockPos();
        for (int x = Math.max(bounds.minX(), clip.minX()); x <= Math.min(bounds.maxX(), clip.maxX()); x++) {
            for (int z = Math.max(bounds.minZ(), clip.minZ()); z <= Math.min(bounds.maxZ(), clip.maxZ()); z++) {
                int cellarBottom = basement.lowestFloorY() - 2;
                cursor.set(x, cellarBottom, z);
                boolean underCellar = basement.isBasementBrick(cursor);
                if (!underCellar) {
                    // Mine floors start one layer above the deeper room slabs.
                    cursor.setY(++cellarBottom);
                    underCellar = basement.isBasementBrick(cursor);
                }
                if (!underCellar && basement.layoutVersion() >= 11) {
                    cellarBottom = basement.keyAnchor().getY() - 2;
                    cursor.setY(cellarBottom);
                    underCellar = basement.isBasementBrick(cursor);
                    if (!underCellar) {
                        cursor.setY(++cellarBottom);
                        underCellar = basement.isBasementBrick(cursor);
                    }
                }
                boolean underHouse = x >= house.minX() && x <= house.maxX() && z >= house.minZ() && z <= house.maxZ();
                if (!underCellar && !underHouse) continue;
                // Support the complete cellar slab independently of the already-solid house floor.
                int top = underCellar ? cellarBottom - 1 : floor - 1;
                for (int y = Math.min(top, clip.maxY()); y >= Math.max(level.getMinBuildHeight(), clip.minY()); y--) {
                    cursor.set(x, y, z);
                    if (basement.containsAuthoredSpace(cursor)) continue;
                    var state = level.getBlockState(cursor);
                    boolean solid = state.getFluidState().isEmpty() && !state.is(BlockTags.LEAVES) && !state.is(BlockTags.LOGS)
                            && state.isCollisionShapeFullBlock(level, cursor);
                    if (solid && basement.layoutVersion() < 3) break;
                    // A thin natural ledge may conceal a cavern beneath the slab.
                    // New estates fill every underlying void, rather than stopping
                    // at that first solid ledge and leaving a hollow foundation.
                    if (!solid) level.setBlock(cursor, (basement.layoutVersion() >= 7
                            ? Blocks.STONE : Blocks.COBBLESTONE).defaultBlockState(), 18);
                }
                if (underCellar && (underHouse || basement.layoutVersion() >= 7)) {
                    for (int y = basement.lowestFloorY() + (basement.layoutVersion() >= 7 ? 5 : 7); y < floor; y++) {
                        cursor.set(x, y, z);
                        if (clip.isInside(cursor) && !basement.containsAuthoredSpace(cursor)) {
                            var material = basement.layoutVersion() < 7 ? Blocks.COBBLESTONE
                                    : y == basement.groundsY() ? Blocks.GRASS_BLOCK : Blocks.STONE;
                            level.setBlock(cursor, material.defaultBlockState(), 18);
                        }
                    }
                }
            }
        }
    }

    private static void stripExtraChests(WorldGenLevel level, PiecesContainer pieces, BoundingBox clip) {
        var templates = level.getLevel().getStructureManager();
        var allowed = List.of("1x1_as1", "1x2_s1", "1x2_a9", "2x2_a2", "1x2_b1", "1x2_b2", "2x2_a1").stream()
                .map(name -> templates.getOrCreate(new ResourceLocation("minecraft", "woodland_mansion/" + name))).toList();
        for (var structurePiece : pieces.pieces()) {
            if (!(structurePiece instanceof WoodlandMansionPieces.WoodlandMansionPiece piece)
                    || !clip.intersects(piece.getBoundingBox()) || allowed.contains(piece.template())) continue;
            var settings = piece.placeSettings().copy().setBoundingBox(clip);
            for (var kind : List.of(Blocks.STRUCTURE_BLOCK, Blocks.CHEST, Blocks.TRAPPED_CHEST)) {
                for (var info : piece.template().filterBlocks(piece.templatePosition(), settings, kind)) {
                    if (kind == Blocks.STRUCTURE_BLOCK && (info.nbt() == null || !info.nbt().getString("metadata").startsWith("Chest"))) continue;
                    var state = level.getBlockState(info.pos());
                    if (state.is(Blocks.CHEST) || state.is(Blocks.TRAPPED_CHEST)) {
                        // Clear the block entity first so a native fixed-content chest cannot spill its items.
                        level.getChunk(info.pos().getX() >> 4, info.pos().getZ() >> 4).removeBlockEntity(info.pos());
                        level.setBlock(info.pos(), Blocks.AIR.defaultBlockState(), 2);
                    }
                }
            }
        }
    }

    @Override public StructureType<?> type() { return ModStructures.ILLAGER_MANSION.get(); }
}

package net.amurdza.examplemod.gametest;

import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.worldgen.structure.IllagerMansionStructure;
import net.amurdza.examplemod.worldgen.structure.MansionBasementPiece;
import net.amurdza.examplemod.worldgen.structure.MansionBossPiece;
import net.amurdza.examplemod.worldgen.structure.MansionSiteHeight;
import net.amurdza.examplemod.worldgen.structure.PlacedMansions;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.PiecesContainer;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePiecesBuilder;
import net.minecraft.world.level.levelgen.structure.structures.WoodlandMansionPieces.WoodlandMansionPiece;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

@GameTestHolder(AOEMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class MansionPlainsDocumentTerrainTests {
    @GameTest(template = "plant_gameplay_empty")
    public static void estateElevationFindsSingleColumnSummitsAndIgnoresSquareCorners(GameTestHelper h) {
        var house = new BoundingBox(-7, 0, 11, 72, 0, 90);
        // The forest circle is centered at (32.5,50.5), with a radius of 103.5.
        // Neither peak lies on a chunk grid, at the origin, or at a house corner.
        for (int[] peak : new int[][]{{130, 79}, {135, 51}, {-66, 19}, {32, -52}}) {
            int[] visits = {0};
            int highest = MansionSiteHeight.highestGround(house, 8, (x, z) -> {
                h.assertTrue(Math.hypot(x - 32.5, z - 50.5) <= 103.5, "Terrain outside the flat forest circle must never be sampled");
                visits[0]++;
                return x == peak[0] && z == peak[1] ? 213 : 71;
            });
            h.assertTrue(highest == 213 && visits[0] > 33000, "A one-column summit anywhere in the forest circle must determine the mansion elevation");
        }
        h.assertTrue(MansionSiteHeight.highestGround(house, 8, (x, z) -> x >= 125 && z >= 143 ? 300 : 71) == 71,
                "A high hill in the enclosing square's corner must not raise the circular estate");
        h.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void estateElevationIncludesTheForestBoundaryButIgnoresEveryHigherRampPeak(GameTestHelper h) {
        // An odd rectangular footprint gives an exact integer center and a
        // radius of 104: the longer house half-span 40 plus 64 forest blocks.
        var house = new BoundingBox(-40, 0, -20, 40, 0, 20);
        for (int[] direction : new int[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1}}) {
            int edgeX = direction[0] * 104, edgeZ = direction[1] * 104;
            int highest = MansionSiteHeight.highestGround(house, 8, (x, z) -> {
                h.assertTrue(x * x + z * z <= 104 * 104, "Even the first ramp column lies outside the height survey");
                return x == edgeX && z == edgeZ ? 187 : 71;
            });
            h.assertTrue(highest == 187, "A summit exactly on the forest boundary must set the estate height in every direction");
            for (int beyond : new int[]{1, 12, 160}) {
                int peakX = direction[0] * (104 + beyond), peakZ = direction[1] * (104 + beyond);
                h.assertTrue(MansionSiteHeight.highestGround(house, 8, (x, z) ->
                                x == peakX && z == peakZ ? 300 : 71) == 71,
                        "A higher peak " + beyond + " blocks beyond the forest must not raise or lengthen the estate's height survey");
            }
        }
        h.assertTrue(MansionSiteHeight.highestGround(house, 8, (x, z) ->
                        x * x + z * z > 104 * 104 ? 300 : 71) == 71,
                "An entire higher surrounding ramp region must leave the forest's selected height unchanged");
        h.succeed();
    }

    @GameTest(template = "plant_gameplay_empty", timeoutTicks = 400)
    public static void clippedFoundationFillsUnderAndAboveEveryMineWithoutSealingItsTunnels(GameTestHelper h) {
        var level = h.getLevel();
        var structure = level.registryAccess().registryOrThrow(Registries.STRUCTURE).get(AOEMod.makeID("illager_mansion"));
        var generator = level.getChunkSource().getGenerator();
        var fixtureChunk = level.getChunkAt(h.absolutePos(BlockPos.ZERO));
        for (Rotation rotation : Rotation.values()) for (boolean walls : new boolean[]{true, false}) {
            var entrance = new BlockPos(8, walls ? 180 : 179, 8);
            var cellar = new MansionBasementPiece(entrance, rotation, 7, walls);
            var room = new WoodlandMansionPiece(level.getStructureManager(), "corridor_floor", entrance, rotation);
            var pieces = new PiecesContainer(List.of(room, cellar));
            // Mine caps, the final stone column and an open tunnel all lie
            // beyond this tiny house, exposing the former short-foundation bug.
            for (int[] column : new int[][]{{-48, 14}, {-48, 28}, {-48, 40}, {-47, 16}, {-46, 16}}) {
                var center = cellar.world(column[0], 0, column[1]);
                var clip = new BoundingBox(center.getX(), center.getY() - 8, center.getZ(),
                        center.getX(), cellar.mainFloorY() + 1, center.getZ());
                Map<BlockPos, BlockState> blocks = new HashMap<>();
                blocks.put(center.below(8), Blocks.STONE.defaultBlockState());
                WorldGenLevel source = (WorldGenLevel) Proxy.newProxyInstance(MansionPlainsDocumentTerrainTests.class.getClassLoader(),
                        new Class<?>[]{WorldGenLevel.class}, (proxy, method, args) -> {
                            switch (method.getName()) {
                                case "getBlockState": return blocks.getOrDefault(args[0], Blocks.AIR.defaultBlockState());
                                case "getBlockEntity": return null;
                                case "getChunk": return fixtureChunk;
                                case "getHeight": return blocks.entrySet().stream()
                                        .filter(e -> !e.getValue().isAir() && e.getKey().getX() == (int) args[1] && e.getKey().getZ() == (int) args[2])
                                        .mapToInt(e -> e.getKey().getY() + 1).max().orElse(level.getMinBuildHeight());
                                case "setBlock": {
                                    var pos = ((BlockPos) args[0]).immutable();
                                    h.assertTrue(clip.isInside(pos), "Foundation cleanup must honor its generating column: " + pos);
                                    blocks.put(pos, (BlockState) args[1]);
                                    return true;
                                }
                            }
                            try { return method.invoke(level, args); }
                            catch (InvocationTargetException failure) { throw failure.getCause(); }
                        });
                var chunk = new ChunkPos(center);
                cellar.postProcess(source, level.structureManager(), generator, RandomSource.create(42), clip, chunk, entrance);
                var authored = new HashMap<>(blocks);
                for (int pass = 0; pass < 2; pass++) {
                    structure.afterPlace(source, level.structureManager(), generator, RandomSource.create(42), clip, chunk, pieces);
                    for (int y = -7; y <= -2; y++) h.assertTrue(blocks.get(center.above(y)).is(Blocks.STONE),
                            "No void may survive below a mine's one-layer-higher floor: " + rotation + "/" + center + "/" + y);
                    for (int y = center.getY() + 6; y <= cellar.groundsY(); y++)
                        h.assertTrue(blocks.get(center.atY(y)).is(y == cellar.groundsY() ? Blocks.GRASS_BLOCK : Blocks.STONE),
                                "The air pocket above the distant mine roof must be filled through the grass surface");
                    authored.forEach((pos, state) -> h.assertTrue(state.equals(blocks.get(pos)),
                            "Foundation repair changed authored mine stone, backing or tunnel air at " + pos));
                }
            }
        }
        h.succeed();
    }

    @GameTest(template = "plant_gameplay_empty", timeoutTicks = 600)
    public static void newMansionsKeepCobblestoneAndSavedFacadesRemainCompatible(GameTestHelper h) {
        var manager = h.getLevel().getStructureManager();
        var serial = StructurePieceSerializationContext.fromLevel(h.getLevel());
        Map<StructureTemplate, Integer> wallCounts = new IdentityHashMap<>();
        var settings = new StructurePlaceSettings();
        for (Rotation rotation : Rotation.values()) {
            var replacementIndex = new PlacedMansions();
            BoundingBox previousBounds = null;
            for (int seed = 0; seed < 4; seed++) {
                var builder = new StructurePiecesBuilder();
                var origin = new BlockPos(0, 150, 0);
                IllagerMansionStructure.addMansionPieces(manager, origin, rotation, builder, RandomSource.create(seed));
                var pieces = builder.build().pieces();
                var cellar = pieces.stream().filter(MansionBasementPiece.class::isInstance).map(MansionBasementPiece.class::cast).findFirst().orElseThrow();
                boolean walls = cellar.decorativeWalls();
                h.assertTrue(walls, "Every new mansion must retain the cobblestone facade and raised platform");
                int totalWalls = 0;
                for (var piece : pieces) if (piece instanceof WoodlandMansionPiece upper) {
                    int count = wallCounts.computeIfAbsent(upper.template(), template -> template.filterBlocks(BlockPos.ZERO, settings, Blocks.COBBLESTONE_WALL).size());
                    totalWalls += count;
                    var restored = new WoodlandMansionPiece(manager, upper.createTag(serial));
                    h.assertTrue(restored.template() == upper.template() && restored.templatePosition().equals(upper.templatePosition())
                            && restored.getRotation() == upper.getRotation(), "Saved upper pieces must preserve their chosen facade and placement");
                }
                h.assertTrue(totalWalls > 0, "New mansions must retain decorative cobblestone walls");
                h.assertTrue(cellar.mainFloorY() == 150 && cellar.groundsY() == 149,
                        "The mansion floor must remain one block above the lawn");
                var restored = (MansionBasementPiece) MansionBasementPiece.load(serial, cellar.createTag(serial));
                h.assertTrue(restored.decorativeWalls() == walls && restored.mainFloorY() == cellar.mainFloorY()
                        && restored.groundsY() == cellar.groundsY(), "A reload must not change the chosen variant's floor/ground offset");
                // Replacing the same estate must retire its former protection.
                var savedRooms = pieces.stream().filter(WoodlandMansionPiece.class::isInstance).map(piece -> {
                    var box = piece.getBoundingBox();
                    return new BoundingBox(box.minX(), box.minY(), box.minZ(), box.maxX(), box.maxY(), box.maxZ());
                }).toList();
                replacementIndex.record(restored, savedRooms);
                h.assertTrue(replacementIndex.save(new CompoundTag()).getList("Mansions", Tag.TAG_COMPOUND).size() == 1,
                        "Replacing an estate must not retain the old protection record");
                var refreshed = replacementIndex.consumeReplacedBounds(restored.keyAnchor());
                if (previousBounds != null) h.assertTrue(refreshed != null
                                && refreshed.isInside(new BlockPos(previousBounds.minX(), previousBounds.minY(), previousBounds.minZ()))
                                && refreshed.isInside(new BlockPos(previousBounds.maxX(), previousBounds.maxY(), previousBounds.maxZ())),
                        "Replacing a layout must invalidate the complete previous mansion footprint");
                previousBounds = BoundingBox.encapsulatingBoxes(savedRooms).orElseThrow().encapsulate(restored.getBoundingBox());
                // Natural starts plan at Y0, then raise every piece together.
                // A boss and its selected room must never share a mutable box.
                Map<StructurePiece, BoundingBox> beforeMove = new IdentityHashMap<>();
                Map<WoodlandMansionPiece, BlockPos> templatePositions = new IdentityHashMap<>();
                for (var piece : pieces) {
                    var box = piece.getBoundingBox();
                    beforeMove.put(piece, new BoundingBox(box.minX(), box.minY(), box.minZ(), box.maxX(), box.maxY(), box.maxZ()));
                    if (piece instanceof WoodlandMansionPiece upper) templatePositions.put(upper, upper.templatePosition());
                }
                var anchorBeforeMove = cellar.keyAnchor();
                pieces.forEach(piece -> piece.move(0, 150, 0));
                beforeMove.forEach((piece, before) -> {
                    var after = piece.getBoundingBox();
                    h.assertTrue(after.minX() == before.minX() && after.maxX() == before.maxX()
                                    && after.minZ() == before.minZ() && after.maxZ() == before.maxZ()
                                    && after.minY() == before.minY() + 150 && after.maxY() == before.maxY() + 150,
                            "Moving the whole plan must translate every room and boss box exactly once");
                    if (piece instanceof WoodlandMansionPiece upper)
                        h.assertTrue(upper.templatePosition().equals(templatePositions.get(upper).above(150)),
                                "A shifted room's template origin must remain aligned with its clipping box");
                    if (piece instanceof MansionBossPiece)
                        h.assertTrue(BlockPos.of(piece.createTag(serial).getLong("KeyAnchor")).equals(anchorBeforeMove.above(150)),
                                "Natural elevation must shift the lord's saved key anchor exactly once");
                });
                h.assertTrue(cellar.keyAnchor().equals(anchorBeforeMove.above(150)), "The raised cellar key must still match its lord");

            }
            var legacyPlain = new MansionBasementPiece(new BlockPos(0, 149, 0), rotation, 7, false);
            var loadedPlain = (MansionBasementPiece) MansionBasementPiece.load(serial, legacyPlain.createTag(serial));
            h.assertTrue(!loadedPlain.decorativeWalls() && loadedPlain.mainFloorY() == 149 && loadedPlain.groundsY() == 149,
                    "Already-saved plain mansions must retain their original floor height");
        }
        h.succeed();
    }
}

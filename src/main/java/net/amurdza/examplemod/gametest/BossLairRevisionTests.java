package net.amurdza.examplemod.gametest;

import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.registry.ModEntities;
import net.amurdza.examplemod.util.GameplayDimensions;
import net.amurdza.examplemod.worldgen.DimensionTerrain;
import net.amurdza.examplemod.worldgen.TropicalLayout;
import net.amurdza.examplemod.worldgen.structure.BossLairPiece;
import net.amurdza.examplemod.worldgen.structure.IslandBossPlacement;
import net.amurdza.examplemod.worldgen.structure.IllagerMansionStructure;
import net.amurdza.examplemod.worldgen.structure.MansionBossPiece;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePiecesBuilder;
import net.minecraft.world.level.levelgen.structure.structures.WoodlandMansionPieces;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(AOEMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class BossLairRevisionTests {
    @GameTest(template = "plant_gameplay_empty", timeoutTicks = 300)
    public static void everyMansionLayoutHasOneLordInARealRoom(GameTestHelper helper) {
        var level = helper.getLevel();
        BlockPos origin = helper.absolutePos(new BlockPos(8, 96, 8));
        for (int seed = 0; seed < 32; seed++) {
            var builder = new StructurePiecesBuilder();
            IllagerMansionStructure.addMansionPieces(level.getStructureManager(), origin,
                    Rotation.values()[seed % 4], builder, RandomSource.create(seed));
            var pieces = builder.build().pieces();
            helper.assertTrue(pieces.stream().filter(net.amurdza.examplemod.worldgen.structure.MansionBasementPiece.class::isInstance)
                    .count() == 1, "Every rotated mansion layout must have one reachable basement");
            var upperFarms = java.util.List.of("1x2_a2", "1x2_a4", "1x2_a7", "1x2_a8").stream()
                    .map(name -> level.getStructureManager().getOrCreate(new ResourceLocation("minecraft", "woodland_mansion/" + name))).toList();
            helper.assertTrue(pieces.stream().noneMatch(piece -> piece instanceof WoodlandMansionPieces.WoodlandMansionPiece nativeRoom
                    && upperFarms.contains(nativeRoom.template())), "Agricultural rooms must only be present underground");
            var bosses = pieces.stream().filter(MansionBossPiece.class::isInstance)
                    .map(MansionBossPiece.class::cast).toList();
            helper.assertTrue(bosses.size() == 1, "Mansion seed " + seed + " has " + bosses.size() + " lord pieces");
            var boss = bosses.get(0);
            var roomBox = boss.getBoundingBox();
            var room = pieces.stream().filter(WoodlandMansionPieces.WoodlandMansionPiece.class::isInstance)
                    .filter(piece -> {
                        var box = piece.getBoundingBox();
                        return box.minX() == roomBox.minX() && box.minY() == roomBox.minY() && box.minZ() == roomBox.minZ()
                                && box.maxX() == roomBox.maxX() && box.maxY() == roomBox.maxY() && box.maxZ() == roomBox.maxZ();
                    }).findFirst().orElseThrow(() -> new AssertionError("Lord piece must belong to a native mansion room"));
            if (seed != 0) continue;
            // Place a real selected template, so the test also catches room selection
            // that gives the lord a wall, missing floor, or insufficient headroom.
            for (int cx = Math.floorDiv(roomBox.minX(), 16); cx <= Math.floorDiv(roomBox.maxX(), 16); cx++) {
                for (int cz = Math.floorDiv(roomBox.minZ(), 16); cz <= Math.floorDiv(roomBox.maxZ(), 16); cz++) {
                    var chunk = new ChunkPos(cx, cz);
                    var clip = new BoundingBox(chunk.getMinBlockX(), level.getMinBuildHeight(), chunk.getMinBlockZ(),
                            chunk.getMaxBlockX(), level.getMaxBuildHeight() - 1, chunk.getMaxBlockZ());
                    room.postProcess(level, level.structureManager(), level.getChunkSource().getGenerator(),
                            RandomSource.create(0), clip, chunk, origin);
                }
            }
            var centerChunk = new ChunkPos(roomBox.getCenter());
            var clip = new BoundingBox(centerChunk.getMinBlockX(), level.getMinBuildHeight(), centerChunk.getMinBlockZ(),
                    centerChunk.getMaxBlockX(), level.getMaxBuildHeight() - 1, centerChunk.getMaxBlockZ());
            for (int repeat = 0; repeat < 2; repeat++) {
                boss.postProcess(level, level.structureManager(), level.getChunkSource().getGenerator(),
                        RandomSource.create(0), clip, centerChunk, origin);
            }
            var lords = level.getEntities(ModEntities.ILLAGER_LORD.get(), net.minecraft.world.phys.AABB.of(roomBox), mob -> true);
            helper.assertTrue(lords.size() == 1, "Repeated room placement must create exactly one lord, found " + lords.size());
            var lord = lords.get(0);
            helper.assertTrue(net.amurdza.examplemod.util.StructureSpawnedMobData.isMarked(lord), "Mansion lord must persist as a structure boss");
            helper.assertTrue(roomBox.isInside(lord.blockPosition()) && level.noCollision(lord), "Lord must stand inside an unobstructed room");
            lords.forEach(net.minecraft.world.entity.Entity::discard);
        }
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty", timeoutTicks = 100)
    public static void eachIslandHasOneCaveInTheMiddleOfItsRainforest(GameTestHelper helper) {
        TropicalLayout layout = new TropicalLayout(817263);
        java.util.Set<net.minecraft.world.level.ChunkPos> candidates = new java.util.HashSet<>();
        for (int x = -10; x <= 10; x++) for (int z = -10; z <= 10; z++) {
            var island = layout.island(x, z);
            BlockPos center = IslandBossPlacement.center(island);
            var sample = layout.sample(center.getX(), center.getZ());
            helper.assertTrue(sample.island().equals(island) && sample.biome() == 2
                            && Math.abs(sample.distance() - 210) < 1.5
                            && layout.sample(island.x(), island.z()).biome() == 3,
                    "Every island needs a cherry center and a cave halfway across the rainforest band");
            helper.assertTrue(candidates.add(new net.minecraft.world.level.ChunkPos(center)), "Two islands share a cave");
        }
        for (long seed = 0; seed < 4; seed++) {
            var cave = new BossLairPiece("spider", new BlockPos(0, 75, 0), seed);
            var mouth = cave.branchLayout().entrance();
            helper.assertTrue(mouth.getY() == 1, "Horizontal mouth must meet the original ground level");
            helper.assertTrue(cave.branchLayout().branches().size() == 4, "Each branch needs its two cave pockets");
            helper.assertTrue(cave.branchLayout().residents().get(0).getY() == -33,
                    "The long branches must descend into the deeper boss chamber");
            int sx = (seed & 1) == 0 ? 1 : -1, sz = (seed & 2) == 0 ? 1 : -1;
            helper.assertTrue(cave.containsSpiderInterior(new BlockPos(44 * sx, 79, 44 * sz)),
                    "Covered approach must still have walking headroom");
            helper.assertFalse(cave.containsSpiderInterior(new BlockPos(30 * sx, 80, 30 * sz)),
                    "Hill above the descending passage must be solid, not the former hollow dome");
        }
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty", timeoutTicks = 200)
    public static void bossStructuresResolveTheirSpawns(GameTestHelper helper) {
        var registry = helper.getLevel().registryAccess().registryOrThrow(Registries.STRUCTURE);
        for (String id : new String[]{"spider_cave", "yeti_cave", "wither_cave", "illager_mansion", "orc_camp"}) {
            helper.assertTrue(registry.get(new ResourceLocation("aoemod", id)) != null, "Missing boss structure " + id);
        }
        var types = net.minecraftforge.registries.ForgeRegistries.ENTITY_TYPES;
        for (String id : new String[]{"aoemod:spider_queen", "aoemod:illager_lord", "aoemod:breeze",
                "twilightforest:alpha_yeti", "orcz:bigorcboss", "orcz:orcshield", "orcz:orcsavagemace"}) {
            helper.assertTrue(types.containsKey(new ResourceLocation(id)), "Missing boss/support type " + id);
        }
        helper.assertTrue(Blocks.OBSIDIAN.defaultBlockState().is(net.minecraft.tags.BlockTags.WITHER_IMMUNE),
                "Wither cave obsidian can be destroyed by its boss");
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty", timeoutTicks = 100)
    public static void witherRoofsHaveNoIsolatedOneBlockTips(GameTestHelper helper) {
        var layout = new net.amurdza.examplemod.worldgen.structure.BranchedBossCaves("wither", 12, 1);
        for (var point : java.util.List.of(new BlockPos(0, 0, 24), new BlockPos(15, 0, 1),
                new BlockPos(-15, 0, 1), new BlockPos(0, 0, -22))) {
            int top = layout.column(point.getX(), point.getZ()).top();
            for (var direction : net.minecraft.core.Direction.Plane.HORIZONTAL) {
                var neighbor = point.relative(direction);
                helper.assertTrue(layout.column(neighbor.getX(), neighbor.getZ()).top() >= top,
                        "Wither room roof retains a lone raised center block at " + point);
            }
        }
        helper.succeed();
    }
}

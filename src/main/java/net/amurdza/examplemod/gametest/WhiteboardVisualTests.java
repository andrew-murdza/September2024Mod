package net.amurdza.examplemod.gametest;

import com.google.gson.GsonBuilder;
import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.util.GameplayDimensions;
import net.amurdza.examplemod.worldgen.structure.BossLairPiece;
import net.amurdza.examplemod.worldgen.structure.MansionBasementPiece;
import net.amurdza.examplemod.worldgen.structure.SavannaOrcCampPiece;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import twilightforest.init.TFEntities;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** Stages the actual pieces and walking model in a disposable, explicitly enabled screenshot world. */
@GameTestHolder(AOEMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class WhiteboardVisualTests {
    private record View(String label, String dimension, double x, double y, double z, float yaw, float pitch) { }

    @GameTest(template = "plant_gameplay_empty", timeoutTicks = 2400)
    public static void exportWhiteboardRevisionViews(GameTestHelper helper) throws Exception {
        if (!Boolean.getBoolean("aoemod.whiteboardVisual") && !Boolean.parseBoolean(System.getenv("AOEMOD_WHITEBOARD_VISUAL"))) {
            helper.succeed();
            return;
        }
        var server = helper.getLevel().getServer();
        var plains = server.getLevel(GameplayDimensions.PLAINS);
        var snowy = server.getLevel(GameplayDimensions.SNOWY);
        var savanna = server.getLevel(GameplayDimensions.SAVANNA);
        List<View> views = new ArrayList<>();
        for (var level : List.of(plains, snowy, savanna)) {
            level.getGameRules().getRule(GameRules.RULE_RANDOMTICKING).set(0, server);
            level.getGameRules().getRule(GameRules.RULE_MOBGRIEFING).set(false, server);
        }

        BlockPos hatch = new BlockPos(224, 128, 256);
        var basement = new MansionBasementPiece(hatch);
        place(plains, basement, hatch);
        BlockPos floor = basement.keyAnchor();
        helper.assertTrue(plains.getBlockState(hatch).is(Blocks.STONE_BRICK_STAIRS)
                        && plains.getBlockState(floor.offset(-11, 0, 18)).is(Blocks.WATER)
                        && plains.getBlockState(floor.offset(30, 1, 27)).is(Blocks.CHEST),
                "Mansion staircase, irrigated farm and granary must be present before capture");
        view(views, "mansion-aisle", plains, floor.offset(-1, 1, 15), floor.offset(0, 2, 50));
        view(views, "mansion-wheat-and-carrots", plains, floor.offset(-4, 2, 18), floor.offset(-14, 1, 19));
        view(views, "mansion-chicken-pen", plains, floor.offset(18, 2, 18), floor.offset(28, 1, 20));
        view(views, "mansion-pantry", plains, floor.offset(20, 2, 28), floor.offset(30, 1, 29));
        view(views, "mansion-mine", plains, floor.offset(-27, 2, 26), floor.offset(-31, -2, 26));

        // Use the same Y datum for all preview columns; structures still run their ordinary
        // per-chunk generation path. Clear the scene of naturally generated treetops by
        // placing the fixture just above the tallest local canopy.
        BlockPos campCenter = aboveCanopy(savanna, 224, 256);
        var camp = new SavannaOrcCampPiece(savanna.getStructureManager(), campCenter);
        place(savanna, camp, campCenter);
        view(views, "orc-camp-plan", savanna, campCenter.offset(2, 71, 22), campCenter.above(2));
        view(views, "orc-camp-center", savanna, campCenter.offset(8, 14, 22), campCenter.above(2));
        view(views, "orc-gate", savanna, campCenter.offset(7, 7, 48), campCenter.offset(0, 3, 34));

        BlockPos yetiCenter = aboveCanopy(snowy, 224, 256);
        var yeti = new BossLairPiece("yeti", yetiCenter, 67);
        place(snowy, yeti, yetiCenter);
        helper.assertTrue(snowy.getBlockState(yetiCenter.offset(5, 18, 5)).is(Blocks.BLUE_ICE)
                        && snowy.getBlockState(yetiCenter.offset(5, 15, 5)).isAir()
                        && snowy.getBlockState(yetiCenter.offset(0, 0, -32)).is(Blocks.SNOW_BLOCK),
                "The flat ceiling and north approach must survive actual chunk placement");
        view(views, "yeti-north-entrance", snowy, yetiCenter.offset(2, 4, -43), yetiCenter.offset(0, 5, -16));
        view(views, "yeti-interior", snowy, yetiCenter.offset(-19, 4, -19), yetiCenter.offset(6, 7, 6));

        var knight = TFEntities.UPPER_GOBLIN_KNIGHT.get().create(snowy);
        var knightPos = yetiCenter.offset(-3, 1, 12);
        knight.moveTo(knightPos.getX() + .5, knightPos.getY(), knightPos.getZ() + .5, 0, 0);
        knight.setHasShield(true);
        knight.setNoAi(true);
        knight.setPersistenceRequired();
        snowy.addFreshEntity(knight);
        view(views, "goblin-shield-front", snowy, knightPos.offset(-3, 0, 4), knightPos.above());
        view(views, "goblin-shield-profile", snowy, knightPos.offset(4, 0, 1), knightPos.above());
        view(views, "goblin-back", snowy, knightPos.offset(2, 0, -4), knightPos.above());

        var output = Path.of("whiteboard-views.json").toAbsolutePath();
        Files.writeString(output, new GsonBuilder().setPrettyPrinting().create().toJson(views));
        server.saveEverything(true, true, true);
        helper.succeed();
    }

    private static BlockPos aboveCanopy(ServerLevel level, int x, int z) {
        int highest = level.getSeaLevel();
        for (int dx = -48; dx <= 48; dx += 8) for (int dz = -48; dz <= 48; dz += 8) {
            highest = Math.max(highest, level.getHeight(Heightmap.Types.WORLD_SURFACE, x + dx, z + dz));
        }
        return new BlockPos(x, highest + 2, z);
    }

    private static void place(ServerLevel level, StructurePiece piece, BlockPos pivot) {
        var box = piece.getBoundingBox();
        for (int x = box.minX() >> 4; x <= box.maxX() >> 4; x++) for (int z = box.minZ() >> 4; z <= box.maxZ() >> 4; z++) {
            var chunk = new ChunkPos(x, z);
            level.getChunk(x, z);
            var clip = new BoundingBox(chunk.getMinBlockX(), level.getMinBuildHeight(), chunk.getMinBlockZ(),
                    chunk.getMaxBlockX(), level.getMaxBuildHeight() - 1, chunk.getMaxBlockZ());
            piece.postProcess(level, level.structureManager(), level.getChunkSource().getGenerator(),
                    RandomSource.create(8181), clip, chunk, pivot);
        }
        for (var mob : level.getEntitiesOfClass(Mob.class, AABB.of(box))) mob.setNoAi(true);
    }

    private static void view(List<View> views, String name, ServerLevel level, BlockPos feet, BlockPos target) {
        Vec3 eye = Vec3.atBottomCenterOf(feet).add(0, 1.62, 0);
        Vec3 delta = Vec3.atCenterOf(target).subtract(eye);
        views.add(new View(name, level.dimension().location().toString(), eye.x, feet.getY(), eye.z,
                (float) Math.toDegrees(Math.atan2(-delta.x, delta.z)),
                (float) -Math.toDegrees(Math.atan2(delta.y, delta.horizontalDistance()))));
    }
}

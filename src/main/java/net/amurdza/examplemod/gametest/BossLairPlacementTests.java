package net.amurdza.examplemod.gametest;

import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.util.GameplayDimensions;
import net.amurdza.examplemod.worldgen.DimensionTerrain;
import net.amurdza.examplemod.worldgen.structure.BossLairPiece;
import net.amurdza.examplemod.worldgen.structure.IslandBossPlacement;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/** Bounded candidates from the real random-spread grids, without generating or writing chunks. */
@GameTestHolder(AOEMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class BossLairPlacementTests {
    private BossLairPlacementTests() { }

    @GameTest(template = "plant_gameplay_empty", timeoutTicks = 600)
    public static void naturalSpiderMouthMeetsGroundAndKeepsItsCoveredApproach(GameTestHelper helper) {
        var level = helper.getLevel().getServer().getLevel(GameplayDimensions.TROPICAL);
        var generator = (NoiseBasedChunkGenerator) level.getChunkSource().getGenerator();
        var random = level.getChunkSource().randomState();
        var structure = level.registryAccess().registryOrThrow(Registries.STRUCTURE)
                .get(new ResourceLocation("aoemod", "spider_cave"));
        var islands = DimensionTerrain.tropicalLayout(random.router().temperature());
        int checked = 0;
        search: for (int ix = -4; ix <= 4; ix++) for (int iz = -4; iz <= 4; iz++) {
            var island = islands.island(ix, iz);
            var chunk = new ChunkPos(IslandBossPlacement.center(island));
            var context = new Structure.GenerationContext(level.registryAccess(), generator, generator.getBiomeSource(), random,
                    level.getStructureManager(), level.getSeed(), chunk, level, structure.biomes()::contains);
            var stub = structure.findValidGenerationPoint(context).orElseThrow();
            var cave = (BossLairPiece) stub.getPiecesBuilder().build().pieces().get(0);
            BlockPos center = stub.position(), mouth = cave.branchLayout().entrance();
            int sx = Integer.signum(mouth.getX()), sz = Integer.signum(mouth.getZ());
            for (int across = -1; across <= 1; across++) {
                int x = center.getX() + (58 + across) * sx, z = center.getZ() + (58 - across) * sz;
                int ground = generator.getBaseHeight(x, z, Heightmap.Types.WORLD_SURFACE_WG, level, random) - 1;
                var column = generator.getBaseColumn(x, z, level, random);
                helper.assertTrue(Math.abs(ground - center.getY()) <= 1 && !column.getBlock(ground).isAir()
                                && column.getBlock(ground).getFluidState().isEmpty()
                                && column.getBlock(ground + 1).isAir() && column.getBlock(ground + 2).isAir(),
                        "The entire three-wide natural approach must meet the horizontal spider mouth within one block: "
                                + center + " outside=" + ground);
            }

            // Exercise the real piece writer against native generated columns.
            // Only this small entrance slice is written to an in-memory view;
            // a flat synthetic floor would conceal the original floating mouth.
            java.util.Map<BlockPos, BlockState> writes = new java.util.HashMap<>();
            java.util.Map<Long, NoiseColumn> columns = new java.util.HashMap<>();
            java.util.function.Function<BlockPos, BlockState> read = pos -> {
                var written = writes.get(pos);
                if (written != null) return written;
                long key = ChunkPos.asLong(pos.getX(), pos.getZ());
                return columns.computeIfAbsent(key, ignored -> generator.getBaseColumn(pos.getX(), pos.getZ(), level, random))
                        .getBlock(pos.getY());
            };
            var view = (WorldGenLevel) java.lang.reflect.Proxy.newProxyInstance(WorldGenLevel.class.getClassLoader(),
                    new Class<?>[]{WorldGenLevel.class}, (proxy, method, args) -> switch (method.getName()) {
                        case "getBlockState" -> read.apply((BlockPos) args[0]);
                        case "setBlock" -> { writes.put(((BlockPos) args[0]).immutable(), (BlockState) args[1]); yield true; }
                        case "getLevel" -> level;
                        default -> throw new UnsupportedOperationException("Unexpected entrance-slice access: " + method.getName());
                    });
            int x1 = center.getX() + 42 * sx, x2 = center.getX() + 58 * sx;
            int z1 = center.getZ() + 42 * sz, z2 = center.getZ() + 58 * sz;
            var clip = new BoundingBox(Math.min(x1, x2), center.getY() - 8, Math.min(z1, z2),
                    Math.max(x1, x2), center.getY() + 20, Math.max(z1, z2));
            cave.postProcess(view, level.structureManager(), generator, RandomSource.create(197), clip, chunk, center);
            for (int along = 54; along <= 57; along++) for (int across = -1; across <= 1; across++) {
                BlockPos floor = center.offset((along + across) * sx, 0, (along - across) * sz);
                helper.assertTrue(!read.apply(floor).isAir() && read.apply(floor.above()).isAir()
                                && read.apply(floor.above(2)).isAir(),
                        "The generated three-wide mouth must stay horizontal and walkable at " + floor);
            }
            BlockPos covered = center.offset(44 * sx, 0, 44 * sz);
            helper.assertTrue(read.apply(covered.above()).isAir() && read.apply(covered.above(2)).isAir(),
                    "The approach inside the hill must keep walking clearance");
            for (int y = 7; y <= BossLairPiece.spiderRoof(44, 44); y++) {
                helper.assertFalse(read.apply(covered.above(y)).isAir(),
                        "The covered approach at local 44,44 must not have a ceiling hole at " + y);
            }
            if (++checked == 8) break search;
        }
        helper.assertTrue(checked == 8, "Exercise eight naturally positioned spider lairs");
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty", timeoutTicks = 2400)
    public static void naturalYetiAndWitherGridsAcceptDryCandidates(GameTestHelper helper) {
        for (String kind : new String[]{"yeti", "wither"}) {
            var level = helper.getLevel().getServer().getLevel(kind.equals("yeti") ? GameplayDimensions.SNOWY : GameplayDimensions.NETHER);
            var generator = (NoiseBasedChunkGenerator) level.getChunkSource().getGenerator();
            var random = level.getChunkSource().randomState();
            var structure = level.registryAccess().registryOrThrow(Registries.STRUCTURE)
                    .get(new ResourceLocation("aoemod", kind + "_cave"));
            var set = level.registryAccess().registryOrThrow(Registries.STRUCTURE_SET)
                    .get(new ResourceLocation("aoemod", kind + "_caves"));
            var placement = set.placement();
            int checked = 0, accepted = 0;
            search: for (int regionX = -8; regionX < 8; regionX++) for (int regionZ = -8; regionZ < 8; regionZ++) {
                var chunk = placement instanceof RandomSpreadStructurePlacement spread
                        ? spread.getPotentialStructureChunk(level.getSeed(), regionX * spread.spacing(), regionZ * spread.spacing())
                        : net.amurdza.examplemod.worldgen.structure.PlateauLairPlacement.candidate(random, regionZ, regionX);
                var context = new Structure.GenerationContext(level.registryAccess(), generator, generator.getBiomeSource(), random,
                        level.getStructureManager(), level.getSeed(), chunk, level, structure.biomes()::contains);
                var candidate = structure.findValidGenerationPoint(context);
                checked++;
                if (candidate.isEmpty()) continue;
                var pos = candidate.orElseThrow().position();
                var column = generator.getBaseColumn(pos.getX(), pos.getZ(), level, random);
                helper.assertTrue(column.getBlock(pos.getY()).getFluidState().isEmpty()
                        && column.getBlock(pos.getY() + 1).getFluidState().isEmpty(), "Boss lair cannot be placed in a lake: " + kind);
                if (kind.equals("yeti")) {
                    helper.assertTrue(Math.abs(pos.getX() - chunk.getMiddleBlockX()) <= 64
                                    && Math.abs(pos.getZ() - chunk.getMiddleBlockZ()) <= 64,
                            "Moving toward the plateau center must preserve structure-reference reach");
                    for (int dx = -24; dx <= 24; dx += 4) for (int dz = -24; dz <= 24; dz += 4) {
                        double ridge = random.router().ridges().compute(new net.minecraft.world.level.levelgen.DensityFunction.SinglePointContext(
                                pos.getX() + dx, 0, pos.getZ() + dz));
                        helper.assertTrue(Math.abs(ridge) >= .85,
                                "The complete 49-block chamber must stay within the unchanged plateau contour");
                    }
                    for (int dx = -44; dx <= 44; dx += 4) for (int dz = -44; dz <= 44; dz += 4) {
                        int y = net.amurdza.examplemod.worldgen.SnowyOceanFloor.floor(random, pos.getX() + dx, pos.getZ() + dz);
                        helper.assertTrue(y <= pos.getY(), "The outer apron must start at or above its natural terrain");
                        helper.assertTrue(generator.getBiomeSource().getNoiseBiome((pos.getX() + dx) >> 2, y >> 2,
                                (pos.getZ() + dz) >> 2, random.sampler()).is(new ResourceLocation("aoemod:snowy_forest")),
                                "Yeti hillside must stay entirely in snowy forest");
                    }
                }
                accepted++;
                if (accepted == 3) break search;
            }
            System.out.println("AOEMod lair placement audit: " + kind + " accepted=" + accepted + " candidates=" + checked);
            helper.assertTrue(accepted > 0, "No " + kind + " lair accepted among " + checked + " natural grid candidates");
        }
        helper.succeed();
    }
}

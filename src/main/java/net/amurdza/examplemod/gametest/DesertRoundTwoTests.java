package net.amurdza.examplemod.gametest;

import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.util.GameplayDimensions;
import net.amurdza.examplemod.worldgen.ShoreSugarCane;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.HashMap;

@GameTestHolder(AOEMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class DesertRoundTwoTests {
    @GameTest(template = "plant_gameplay_empty", timeoutTicks = 400)
    public static void actualDesertSurfaceBuilderKeepsOpenCaveEntrancesStone(GameTestHelper helper) {
        var level = helper.getLevel().getServer().getLevel(GameplayDimensions.DESERT);
        var generator = (net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator) level.getChunkSource().getGenerator();
        var random = net.minecraft.world.level.levelgen.RandomState.create(generator.generatorSettings().value(),
                level.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.NOISE), 12345);
        var terrain = net.amurdza.examplemod.worldgen.NetherRampDecorationGuard.terrain(random).orElseThrow();
        var route = terrain.desertOutlet(0, 0);
        BlockPos mouth = null;
        for (int distance = 20; distance < route.length(); distance++) {
            int x = (int) Math.round(route.xAt(distance)), z = (int) Math.round(route.zAt(distance));
            var point = new net.minecraft.world.level.levelgen.DensityFunction.SinglePointContext(x, 0, z);
            double natural = net.amurdza.examplemod.worldgen.DimensionTerrain.surfaceHeight("desert",
                    terrain.land().compute(point), terrain.detail().compute(point), 0, 0);
            double floor = route.floorAt(distance);
            if (natural - floor > 3 && natural - floor < 5) { mouth = new BlockPos(x, (int) floor, z); break; }
        }
        helper.assertTrue(mouth != null, "Seeded cave must have an exposed exit beneath its original surface");
        var chunkPos = new net.minecraft.world.level.ChunkPos(mouth);
        var biomes = level.registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.BIOME);
        var chunk = new net.minecraft.world.level.chunk.ProtoChunk(chunkPos, net.minecraft.world.level.chunk.UpgradeData.EMPTY, level, biomes, null);
        for (int x = chunkPos.getMinBlockX(); x <= chunkPos.getMaxBlockX(); x++) for (int z = chunkPos.getMinBlockZ(); z <= chunkPos.getMaxBlockZ(); z++) {
            var column = generator.getBaseColumn(x, z, level, random);
            for (int y = -32; y <= 100; y++) if (!column.getBlock(y).isAir()) chunk.setBlockState(new BlockPos(x, y, z), column.getBlock(y), false);
        }
        net.minecraft.world.level.levelgen.Heightmap.primeHeightmaps(chunk, java.util.Set.of(
                net.minecraft.world.level.levelgen.Heightmap.Types.OCEAN_FLOOR_WG, net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE_WG));
        random.surfaceSystem().buildSurface(random, level.getBiomeManager(), biomes, false,
                new net.minecraft.world.level.levelgen.WorldGenerationContext(generator, level), chunk, null,
                new net.amurdza.examplemod.worldgen.surface_rule.DimensionSurfaceRule("desert"));
        int checked = 0;
        for (int x = chunkPos.getMinBlockX(); x <= chunkPos.getMaxBlockX(); x++) for (int z = chunkPos.getMinBlockZ(); z <= chunkPos.getMaxBlockZ(); z++) {
            int top = chunk.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.OCEAN_FLOOR_WG, x & 15, z & 15);
            if (top >= 16 && terrain.isDesertEntranceRock(x, top, z)) {
                helper.assertTrue(chunk.getBlockState(new BlockPos(x, top, z)).is(Blocks.STONE),
                        "Open cave mouths must remain stone instead of sand or terracotta at " + x + "," + top + "," + z);
                checked++;
            }
        }
        helper.assertTrue(checked > 5, "Exercise multiple exposed entrance columns");
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void bullKelpManualPlacementAndBonemealKeepAnEntireWaterBlockAbove(GameTestHelper helper) {
        var level = helper.getLevel().getServer().getLevel(GameplayDimensions.DESERT);
        var head = ForgeRegistries.BLOCKS.getValue(new ResourceLocation("hybrid_aquatic:bull_kelp"));
        var player = FakePlayerFactory.getMinecraft(level);
        BlockPos root = new BlockPos(18120, 220, 18120);
        for (int depth = 1; depth <= 4; depth++) {
            level.setBlock(root.below(), Blocks.SAND.defaultBlockState(), 18);
            for (int y = 0; y < 6; y++) level.setBlock(root.above(y), (y < depth ? Blocks.WATER : Blocks.AIR).defaultBlockState(), 18);
            BlockState placed = head.getStateForPlacement(new BlockPlaceContext(level, player, InteractionHand.MAIN_HAND,
                    new ItemStack(head.asItem()), new BlockHitResult(Vec3.atCenterOf(root.below()), Direction.UP, root.below(), false)));
            helper.assertTrue((placed != null) == (depth >= 3), "Manual kelp placement must reject depth " + depth + " when shallower than three");
            if (placed == null) continue;
            helper.assertTrue(placed.getValue(BlockStateProperties.AGE_25) == 25, "Player-placed kelp must also start mature");
            level.setBlock(root, placed, 18);
            BlockPos tip = root;
            for (int count = 0; count < depth + 2; count++) {
                BlockState state = level.getBlockState(tip);
                ((BonemealableBlock) head).performBonemeal(level, RandomSource.create(count), tip, state);
                if (level.getBlockState(tip.above()).is(head)) tip = tip.above();
            }
            helper.assertTrue(tip.getY() == root.getY() + depth - 2
                            && level.getBlockState(tip.above()).is(Blocks.WATER)
                            && level.getBlockState(tip).getValue(BlockStateProperties.AGE_25) == 25,
                    "Bonemeal must stop one whole water block beneath the surface and leave a mature tip");
            var surfaceContext = new BlockPlaceContext(level, player, InteractionHand.MAIN_HAND, new ItemStack(head.asItem()),
                    new BlockHitResult(Vec3.atCenterOf(tip), Direction.UP, tip, false));
            helper.assertTrue(head.getStateForPlacement(surfaceContext) == null, "Manual extension cannot place a head in the surface water cell");
        }
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void sandyFarmlandRevertsToSandWhenDryOrCovered(GameTestHelper helper) {
        var level = helper.getLevel().getServer().getLevel(GameplayDimensions.DESERT);
        var block = ForgeRegistries.BLOCKS.getValue(new ResourceLocation("biomeswevegone:sandy_farmland"));
        helper.assertTrue(block != null && block != Blocks.AIR, "Original sandy farmland must be registered");
        BlockPos pos = helper.absolutePos(BlockPos.ZERO).atY(230);
        level.setBlock(pos.below(), Blocks.STONE.defaultBlockState(), 18);
        level.setBlock(pos.above(), Blocks.AIR.defaultBlockState(), 18);
        level.setBlock(pos, block.defaultBlockState(), 18);
        block.randomTick(level.getBlockState(pos), level, pos, RandomSource.create(4));
        helper.assertTrue(level.getBlockState(pos).is(Blocks.SAND), "Dry farmland must revert to sand");
        level.setBlock(pos, block.defaultBlockState(), 18);
        level.setBlock(pos.above(), Blocks.STONE.defaultBlockState(), 18);
        block.tick(level.getBlockState(pos), level, pos, RandomSource.create(4));
        helper.assertTrue(level.getBlockState(pos).is(Blocks.SAND), "Covered farmland must revert to sand");
        level.setBlock(pos.above(), Blocks.AIR.defaultBlockState(), 18);
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void submergedCaneAcceptsDiagonalShoreAndRejectsOpenLakeInterior(GameTestHelper helper) {
        var level = helper.getLevel().getServer().getLevel(GameplayDimensions.DESERT);
        var blocks = new HashMap<BlockPos, BlockState>();
        var world = AquaticPlantGenerationTests.fixture(helper, "desert", blocks, 1, level,
                Blocks.SAND.defaultBlockState(), Blocks.WATER.defaultBlockState());
        BlockPos root = new BlockPos(8, 101, 8);
        helper.assertFalse(ShoreSugarCane.placeStalk(world, root), "A submerged lake-interior stalk must not generate");
        blocks.put(root.offset(1, 0, 1), Blocks.SAND.defaultBlockState());
        helper.assertTrue(ShoreSugarCane.placeStalk(world, root), "A diagonal dry shore must be sufficient for submerged cane");
        helper.assertTrue(blocks.get(root).is(Blocks.SUGAR_CANE) && blocks.get(root).getFluidState().isSource(), "Preserve the original waterlogged root");
        helper.succeed();
    }
}

package net.amurdza.examplemod.gametest;

import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.util.GameplayDimensions;
import net.amurdza.examplemod.worldgen.BeeNestTrees;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BeehiveBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.function.ToIntBiFunction;

@GameTestHolder(AOEMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class BeeNestTreesTests {
    @GameTest(template = "plant_gameplay_empty", timeoutTicks = 2400)
    public static void fullyDecoratedPlainsAndSavannaTilesContainTheirOccupiedBeeTree(GameTestHelper helper) {
        // Fresh signed tiles keep persisted chunks from concealing changes to
        // the feature ordering on repeated runs. Only owner chunks are loaded.
        int tileBase = 32 + (int) Math.floorMod(System.currentTimeMillis() / 1000, 4096);
        for (boolean savanna : new boolean[]{false, true}) {
            ServerLevel level = helper.getLevel().getServer().getLevel(savanna ? GameplayDimensions.SAVANNA : GameplayDimensions.PLAINS);
            helper.assertTrue(level != null, "The bee-tree dimension must be loaded");
            for (int sign : new int[]{-1, 1}) {
                int tileX = sign * tileBase, tileZ = -sign * (tileBase + 7);
                BlockPos site = BeeNestTrees.site(level, tileX, tileZ);
                for (int attempt = 1; site == null && attempt < 32; attempt++) {
                    tileX += sign;
                    site = BeeNestTrees.site(level, tileX, tileZ);
                }
                helper.assertTrue(site != null, "The sampled signed tiles must include a dry bee-tree site: "
                        + level.dimension().location() + ", tile=" + tileX + "," + tileZ);
                AOEMod.LOGGER.info("Checking fully decorated bee tree in {} at {} (seed {}, tile {},{})",
                        level.dimension().location(), site, level.getSeed(), tileX, tileZ);
                // FULL status runs the actual biome features, reservation and
                // DocumentedFloraFeature wiring, without manually placing a tree.
                var chunk = level.getChunk(site);
                int nests = 0;
                for (int x = chunk.getPos().getMinBlockX(); x <= chunk.getPos().getMaxBlockX(); x++) {
                    for (int z = chunk.getPos().getMinBlockZ(); z <= chunk.getPos().getMaxBlockZ(); z++) {
                        for (int y = site.getY() - 3; y <= site.getY() + 16; y++) {
                            BlockPos pos = new BlockPos(x, y, z);
                            if (!chunk.getBlockState(pos).is(Blocks.BEE_NEST)) continue;
                            nests++;
                            helper.assertTrue(chunk.getBlockEntity(pos) instanceof BeehiveBlockEntity hive && !hive.isEmpty(),
                                    "Natural chunk decoration must retain an occupied bee nest at " + pos);
                            boolean attached = false;
                            for (Direction direction : Direction.values()) {
                                BlockPos neighbor = pos.relative(direction);
                                if (chunk.getPos().equals(new net.minecraft.world.level.ChunkPos(neighbor))
                                        && chunk.getBlockState(neighbor).is(savanna ? Blocks.ACACIA_LOG : Blocks.BIRCH_LOG)) {
                                    attached = true;
                                }
                            }
                            helper.assertTrue(attached, "The generated nest must attach to its biome's tree logs at " + pos);
                        }
                    }
                }
                helper.assertTrue(nests == 1, "Exactly one live bee-tree nest must decorate its reserved owner chunk: "
                        + level.dimension().location() + ", tile=" + tileX + "," + tileZ + ", nests=" + nests);
            }
        }
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void everyTileSelectsOneStableDryChunkIncludingNegativeCoordinates(GameTestHelper helper) {
        for (long seed : new long[]{0, 7, 19457}) {
            for (int tileX = -3; tileX <= 3; tileX++) for (int tileZ = -3; tileZ <= 3; tileZ++) {
                int dryChunkX = tileX * 18 + Math.floorMod(tileZ * 3, 18);
                int dryChunkZ = tileZ * 18 + Math.floorMod(tileX * 7, 18);
                ToIntBiFunction<Integer, Integer> height = (x, z) ->
                        Math.floorDiv(x, 16) == dryChunkX && Math.floorDiv(z, 16) == dryChunkZ ? 66 : 60;
                BlockPos first = BeeNestTrees.plan(seed, tileX, tileZ, 63, height);
                helper.assertTrue(first != null && first.equals(BeeNestTrees.plan(seed, tileX, tileZ, 63, height)),
                        "Tile bee-tree planning must be deterministic");
                helper.assertTrue(Math.floorDiv(first.getX(), 288) == tileX && Math.floorDiv(first.getZ(), 288) == tileZ,
                        "Negative coordinates must remain inside their own 288-square tile");
                helper.assertTrue(Math.floorDiv(first.getX(), 16) == dryChunkX && Math.floorDiv(first.getZ(), 16) == dryChunkZ,
                        "The plan must find the only dry owner chunk instead of choosing water");
            }
        }
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty", timeoutTicks = 400)
    public static void reservedPlainsAndSavannaTreesCommitOneOccupiedNest(GameTestHelper helper) {
        for (boolean savanna : new boolean[]{false, true}) {
            ServerLevel real = helper.getLevel().getServer().getLevel(savanna ? GameplayDimensions.SAVANNA : GameplayDimensions.PLAINS);
            helper.assertTrue(real != null, "The bee-tree dimension must be loaded");
            for (int tile : new int[]{-1, 0, 1}) {
                BlockPos site = BeeNestTrees.site(real, tile, -tile);
                helper.assertTrue(site != null, "Generated Plains and Savanna tiles must have a dry bee-tree site");
                Map<BlockPos, BlockState> blocks = new HashMap<>();
                Map<BlockPos, BlockEntity> entities = new HashMap<>();
                WorldGenLevel level = fixture(real, savanna ? "savanna" : "plains", site.getY(), blocks, entities);
                int chunkX = Math.floorDiv(site.getX(), 16), chunkZ = Math.floorDiv(site.getZ(), 16);
                helper.assertTrue(BeeNestTrees.ownsChunk(level, chunkX, chunkZ)
                                && !BeeNestTrees.ownsChunk(level, chunkX == tile * 18 ? chunkX + 1 : chunkX - 1, chunkZ),
                        "Exactly the selected chunk owns the tile's bee tree");
                var context = new FeaturePlaceContext<>(Optional.empty(), level, real.getChunkSource().getGenerator(),
                        RandomSource.create(81), new BlockPos(chunkX * 16, site.getY(), chunkZ * 16),
                        NoneFeatureConfiguration.INSTANCE);
                helper.assertTrue(BeeNestTrees.place(context), "The reserved native bee tree must generate and commit");
                long nests = blocks.values().stream().filter(state -> state.is(Blocks.BEE_NEST)).count();
                helper.assertTrue(nests == 1 && entities.values().stream()
                                .anyMatch(entity -> entity instanceof BeehiveBlockEntity hive && !hive.isEmpty()),
                        "The committed native nest must contain bees after the buffer commits");
                helper.assertTrue(blocks.values().stream().anyMatch(state -> state.is(savanna ? Blocks.ACACIA_LOG : Blocks.BIRCH_LOG)),
                        "The nest must belong to the configured biome-appropriate tree");
                Map<BlockPos, BlockState> firstTree = Map.copyOf(blocks);
                helper.assertTrue(BeeNestTrees.place(context) && blocks.equals(firstTree),
                        "Re-entering the same owner chunk cannot add a second bee tree");
            }
        }
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void entirelyWetTilesRemainWaterWithoutBeeTrees(GameTestHelper helper) {
        for (boolean savanna : new boolean[]{false, true}) {
            ServerLevel real = helper.getLevel().getServer().getLevel(savanna ? GameplayDimensions.SAVANNA : GameplayDimensions.PLAINS);
            int sea = real.getChunkSource().getGenerator().getSeaLevel();
            BlockPos planned = BeeNestTrees.plan(real.getSeed(), -8, 7, sea, (x, z) -> sea - 6);
            helper.assertTrue(planned == null, "An entirely wet tile must not reserve a bee-tree site");
            BlockPos wetSite = new BlockPos(-8 * 288 + 8, sea, 7 * 288 + 8);
            Map<BlockPos, BlockState> blocks = new HashMap<>();
            Map<BlockPos, BlockEntity> entities = new HashMap<>();
            WorldGenLevel level = fixture(real, savanna ? "savanna" : "plains", sea - 6, sea, blocks, entities);
            var context = new FeaturePlaceContext<>(Optional.empty(), level, real.getChunkSource().getGenerator(),
                    RandomSource.create(91), wetSite, NoneFeatureConfiguration.INSTANCE);
            helper.assertFalse(BeeNestTrees.placeAt(context, wetSite), "A submerged site must reject tree placement");
            helper.assertTrue(blocks.isEmpty() && entities.isEmpty(), "Rejected wet sites must not add soil, logs or bee nests");
            helper.assertTrue(level.getBlockState(wetSite.below()).is(Blocks.WATER),
                    "The water must remain untouched");
        }
        helper.succeed();
    }

    private static WorldGenLevel fixture(ServerLevel real, String biomeName, int floorY,
                                          Map<BlockPos, BlockState> blocks, Map<BlockPos, BlockEntity> entities) {
        return fixture(real, biomeName, floorY, Integer.MIN_VALUE, blocks, entities);
    }

    private static WorldGenLevel fixture(ServerLevel real, String biomeName, int floorY, int waterSurface,
                                          Map<BlockPos, BlockState> blocks, Map<BlockPos, BlockEntity> entities) {
        var biome = real.registryAccess().registryOrThrow(Registries.BIOME).getHolderOrThrow(
                ResourceKey.create(Registries.BIOME, AOEMod.makeID(biomeName)));
        java.util.function.Function<BlockPos, BlockState> stateAt = pos -> blocks.getOrDefault(pos,
                pos.getY() < floorY ? Blocks.GRASS_BLOCK.defaultBlockState()
                        : pos.getY() < waterSurface ? Blocks.WATER.defaultBlockState() : Blocks.AIR.defaultBlockState());
        return (WorldGenLevel) Proxy.newProxyInstance(BeeNestTreesTests.class.getClassLoader(),
                new Class<?>[]{WorldGenLevel.class}, (proxy, method, args) -> {
                    switch (method.getName()) {
                        case "getLevel": return real;
                        case "getBiome": return biome;
                        case "getHeight": return args == null || args.length == 0 ? real.getHeight() : floorY;
                        case "getBlockState": return stateAt.apply((BlockPos) args[0]);
                        case "getFluidState": return stateAt.apply((BlockPos) args[0]).getFluidState();
                        case "isStateAtPosition": return ((Predicate<BlockState>) args[1]).test(stateAt.apply((BlockPos) args[0]));
                        case "isFluidAtPosition": return ((Predicate<net.minecraft.world.level.material.FluidState>) args[1])
                                .test(stateAt.apply((BlockPos) args[0]).getFluidState());
                        case "isEmptyBlock": return stateAt.apply((BlockPos) args[0]).isAir();
                        case "ensureCanWrite": return true;
                        case "getBlockEntity": return args.length == 1 ? entities.get(args[0]) : Optional.ofNullable(entities.get(args[0]));
                        case "setBlock": {
                            BlockPos pos = ((BlockPos) args[0]).immutable();
                            BlockState state = (BlockState) args[1];
                            blocks.put(pos, state);
                            entities.remove(pos);
                            if (state.getBlock() instanceof EntityBlock block) {
                                BlockEntity entity = block.newBlockEntity(pos, state);
                                if (entity != null) {
                                    entity.setLevel(real);
                                    entities.put(pos, entity);
                                }
                            }
                            return true;
                        }
                        default:
                            try {
                                return method.invoke(real, args);
                            } catch (InvocationTargetException exception) {
                                throw exception.getCause();
                            }
                    }
                });
    }
}

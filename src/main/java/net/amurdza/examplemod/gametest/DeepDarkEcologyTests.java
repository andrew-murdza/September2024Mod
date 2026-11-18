package net.amurdza.examplemod.gametest;

import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.util.GameplayDimensions;
import net.amurdza.examplemod.worldgen.DeepDarkEcology;
import net.amurdza.examplemod.worldgen.DeepDarkVegetation;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.MultifaceBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.chunk.UpgradeData;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.MultifaceGrowthConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

@GameTestHolder(AOEMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class DeepDarkEcologyTests {
    @GameTest(template = "plant_gameplay_empty")
    public static void floorsHaveEndStoneBackingAndCeilingsAreDeepslate(GameTestHelper helper) {
        Map<BlockPos, BlockState> blocks = new HashMap<>();
        WorldGenLevel world = fixture(helper, blocks, p -> p.getX() >= 8 || p.getY() <= -26 || p.getY() >= -4
                ? Blocks.STONE.defaultBlockState() : Blocks.AIR.defaultBlockState());
        DeepDarkEcology.prepareSurfaces(world, 0, 0);
        for (int depth = 0; depth < 3; depth++) {
            var expected = depth == 0 ? Blocks.SCULK : Blocks.END_STONE;
            helper.assertTrue(world.getBlockState(new BlockPos(4, -26 - depth, 4)).is(expected), "Wrong floor layer " + depth);
            helper.assertTrue(world.getBlockState(new BlockPos(4, -4 + depth, 4)).is(Blocks.DEEPSLATE), "Wrong ceiling layer " + depth);
        }
        helper.assertTrue(world.getBlockState(new BlockPos(8, -15, 4)).is(Blocks.STONE), "Vertical cave walls must remain stone");
        helper.assertTrue(world.getBlockState(new BlockPos(4, -29, 4)).is(Blocks.STONE), "Floor skin must stop after three blocks");
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void exposedBackingKeepsEntireLedgeDeepslateAndWallsCannotBeCoated(GameTestHelper helper) {
        Map<BlockPos, BlockState> blocks = new HashMap<>();
        WorldGenLevel world = fixture(helper, blocks, p -> p.equals(new BlockPos(5, -27, 4))
                ? Blocks.AIR.defaultBlockState() : Blocks.STONE.defaultBlockState());
        BlockPos floor = new BlockPos(4, -26, 4);
        BlockState vein = Blocks.SCULK_VEIN.defaultBlockState()
                .setValue(MultifaceBlock.getFaceProperty(Direction.DOWN), true);
        blocks.put(floor.above(), vein);
        DeepDarkEcology.coatSurface(world, floor, Direction.DOWN);
        helper.assertTrue(world.getBlockState(floor).is(Blocks.DEEPSLATE),
                "A stair tread with exposed supporting rock must remain deepslate");
        for (int y = -27; y >= -28; y--) helper.assertTrue(world.getBlockState(new BlockPos(4, y, 4)).is(Blocks.DEEPSLATE),
                "Exposed stair backing must remain deepslate along with the tread");
        helper.assertTrue(world.getBlockState(floor.above()).equals(vein),
                "A vein attached to a deepslate ledge must retain its valid floor face");
        int before = blocks.size();
        DeepDarkEcology.coatSurface(world, new BlockPos(8, -15, 4), Direction.EAST);
        helper.assertTrue(blocks.size() == before, "Horizontal coating calls must not create wall patches");
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void thinStairShelvesKeepTheirDeepslateTop(GameTestHelper helper) {
        Map<BlockPos, BlockState> blocks = new HashMap<>();
        WorldGenLevel world = fixture(helper, blocks, p -> p.getY() == -16
                ? Blocks.DEEPSLATE.defaultBlockState() : Blocks.AIR.defaultBlockState());
        DeepDarkEcology.prepareSurfaces(world, 0, 0);
        helper.assertTrue(world.getBlockState(new BlockPos(4, -16, 4)).is(Blocks.DEEPSLATE),
                "A thin shelf with an exposed underside must not become sculk");
        helper.assertTrue(world.getBlockState(new BlockPos(4, -17, 4)).isAir(), "Coating must not fill the cavity under a thin step");
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void backingExposureChecksEverySideButAllowsExposedTreadFaces(GameTestHelper helper) {
        BlockPos floor = new BlockPos(4, -16, 4);
        for (int depth : new int[]{1}) for (Direction side : Direction.Plane.HORIZONTAL) {
            BlockPos opening = floor.below(depth).relative(side);
            for (var openBlock : new net.minecraft.world.level.block.Block[]{Blocks.AIR, Blocks.WATER}) {
                Map<BlockPos, BlockState> blocks = new HashMap<>();
                WorldGenLevel world = fixture(helper, blocks, p -> p.equals(opening)
                        ? openBlock.defaultBlockState() : Blocks.DEEPSLATE.defaultBlockState());
                DeepDarkEcology.coatSurface(world, floor, Direction.DOWN);
                for (int layer = 0; layer <= 2; layer++) helper.assertTrue(world.getBlockState(floor.below(layer)).is(Blocks.DEEPSLATE),
                        "Exposed backing must reject the entire sculk skin: depth=" + depth + ", side=" + side + ", opening=" + openBlock);
            }
        }
        Map<BlockPos, BlockState> undersideBlocks = new HashMap<>();
        WorldGenLevel undersideWorld = fixture(helper, undersideBlocks, p -> p.equals(floor.below(3))
                ? Blocks.AIR.defaultBlockState() : Blocks.DEEPSLATE.defaultBlockState());
        DeepDarkEcology.coatSurface(undersideWorld, floor, Direction.DOWN);
        helper.assertTrue(undersideWorld.getBlockState(floor).is(Blocks.SCULK)
                        && undersideWorld.getBlockState(floor.below()).is(Blocks.END_STONE)
                        && undersideWorld.getBlockState(floor.below(2)).is(Blocks.DEEPSLATE),
                "A buried immediate support allows sculk; a deeper exposed layer must stay deepslate");
        Map<BlockPos, BlockState> blocks = new HashMap<>();
        WorldGenLevel world = fixture(helper, blocks, p -> p.getY() < floor.getY() || p.equals(floor)
                ? Blocks.DEEPSLATE.defaultBlockState() : Blocks.AIR.defaultBlockState());
        DeepDarkEcology.coatSurface(world, floor, Direction.DOWN);
        helper.assertTrue(world.getBlockState(floor).is(Blocks.SCULK),
                "Exposed sides of the tread itself must not exclude a floor whose backing is completely buried");
        helper.assertTrue(world.getBlockState(floor.below()).is(Blocks.END_STONE)
                        && world.getBlockState(floor.below(2)).is(Blocks.END_STONE),
                "Completely buried backing must retain both end-stone layers");
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void lowestBuriedFloorsDoNotTreatOutsideWorldAsCaveAir(GameTestHelper helper) {
        Map<BlockPos, BlockState> blocks = new HashMap<>();
        WorldGenLevel world = fixture(helper, blocks, p -> p.getY() < -32 || p.getY() > -30
                ? Blocks.AIR.defaultBlockState() : Blocks.DEEPSLATE.defaultBlockState());
        DeepDarkEcology.coatSurface(world, new BlockPos(4, -30, 4), Direction.DOWN);
        helper.assertTrue(world.getBlockState(new BlockPos(4, -30, 4)).is(Blocks.SCULK)
                        && world.getBlockState(new BlockPos(4, -32, 4)).is(Blocks.END_STONE),
                "The world boundary is not an exposed underside of the lowest supported floor");
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void floodedFloorsRetainSculkAndBuriedEndStone(GameTestHelper helper) {
        Map<BlockPos, BlockState> blocks = new HashMap<>();
        WorldGenLevel world = fixture(helper, blocks, p -> p.getY() <= -26 || p.getY() >= -4
                ? Blocks.STONE.defaultBlockState() : Blocks.WATER.defaultBlockState());
        DeepDarkEcology.prepareSurfaces(world, 0, 0);
        for (int depth = 0; depth < 3; depth++) {
            var expected = depth == 0 ? Blocks.SCULK : Blocks.END_STONE;
            helper.assertTrue(world.getBlockState(new BlockPos(4, -26 - depth, 4)).is(expected), "Flooded floor lost its skin");
            helper.assertTrue(world.getBlockState(new BlockPos(4, -4 + depth, 4)).is(Blocks.DEEPSLATE), "Flooded ceiling lost its skin");
        }
        helper.assertTrue(world.getBlockState(new BlockPos(4, -25, 4)).is(Blocks.WATER), "Coating must preserve the cave water");
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void neighbourVeinsKeepDeepslateCeilingFacesButLoseSculkFloorFaces(GameTestHelper helper) {
        Map<BlockPos, BlockState> blocks = new HashMap<>();
        WorldGenLevel world = fixture(helper, blocks, p -> p.getX() >= 8 || p.getY() >= -4 || p.getY() <= -26
                ? Blocks.STONE.defaultBlockState() : Blocks.AIR.defaultBlockState());
        BlockState ceilingVein = Blocks.SCULK_VEIN.defaultBlockState()
                .setValue(MultifaceBlock.getFaceProperty(Direction.UP), true);
        BlockState floorVein = Blocks.SCULK_VEIN.defaultBlockState()
                .setValue(MultifaceBlock.getFaceProperty(Direction.DOWN), true);
        BlockPos ceiling = new BlockPos(4, -5, 4), dry = new BlockPos(4, -25, 4);
        BlockPos wet = new BlockPos(5, -25, 4), wall = new BlockPos(7, -25, 4);
        blocks.put(ceiling, ceilingVein);
        blocks.put(dry, floorVein);
        blocks.put(wet, floorVein.setValue(BlockStateProperties.WATERLOGGED, true));
        blocks.put(wall, floorVein.setValue(MultifaceBlock.getFaceProperty(Direction.EAST), true));
        DeepDarkEcology.prepareSurfaces(world, 0, 0);
        helper.assertTrue(world.getBlockState(ceiling.above()).is(Blocks.DEEPSLATE)
                        && world.getBlockState(ceiling).equals(ceilingVein),
                "Veins retain their valid faces against the new deepslate ceiling");
        helper.assertTrue(world.getBlockState(dry).isAir(), "A vein against a newly sculk-coated floor must lose its last face");
        helper.assertTrue(world.getBlockState(wet).is(Blocks.WATER), "Removing a submerged vein's last face must preserve water");
        BlockState remaining = world.getBlockState(wall);
        helper.assertTrue(remaining.is(Blocks.SCULK_VEIN)
                        && remaining.getValue(MultifaceBlock.getFaceProperty(Direction.EAST))
                        && !remaining.getValue(MultifaceBlock.getFaceProperty(Direction.DOWN)),
                "Coating must retain valid wall faces while removing faces against sculk");
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void chorusAttemptsEachLayerWithCatalystRootsAndRejectsCrampedFloors(GameTestHelper helper) {
        Map<BlockPos, BlockState> blocks = new HashMap<>();
        WorldGenLevel world = fixture(helper, blocks, p -> {
            int y = p.getY();
            if (y == -26 || y == -9) return Blocks.SCULK.defaultBlockState();
            return y < -26 || y >= -12 && y < -9 || y >= -1 ? Blocks.STONE.defaultBlockState() : Blocks.AIR.defaultBlockState();
        });
        helper.assertTrue(DeepDarkVegetation.place(new FeaturePlaceContext<>(Optional.empty(), world,
                world.getLevel().getChunkSource().getGenerator(), RandomSource.create(7), BlockPos.ZERO,
                NoneFeatureConfiguration.INSTANCE)), "Chorus must grow on suitable cave layers");
        for (int floorY : new int[]{-26, -9}) {
            var catalyst = blocks.entrySet().stream().filter(entry -> entry.getKey().getY() == floorY
                    && entry.getValue().is(Blocks.SCULK_CATALYST)).map(Map.Entry::getKey).findFirst();
            helper.assertTrue(catalyst.isPresent(), "Each tree needs its own catalyst root, including floors less than thirty blocks apart");
            helper.assertTrue(world.getBlockState(catalyst.orElseThrow().above()).is(Blocks.CHORUS_PLANT), "Generate stems, not isolated flowers");
        }
        long roots = blocks.values().stream().filter(state -> state.is(Blocks.SCULK_CATALYST)).count();
        helper.assertTrue(roots == 2, "One attempt per layer must produce exactly two roots in this fixture");
        BlockPos cramped = new BlockPos(2, -25, 2);
        blocks.put(cramped.above(2), Blocks.STONE.defaultBlockState());
        helper.assertFalse(DeepDarkVegetation.placeTree(world, cramped, RandomSource.create(8)), "Cramped roots must fail before placing a catalyst");
        helper.assertTrue(world.getBlockState(cramped.below()).is(Blocks.SCULK), "Failed trees must not leave orphan catalysts");
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void loadedDeepDarkDisablesLegacyPatchesAndKeepsLichenGeometry(GameTestHelper helper) {
        var registries = helper.getLevel().registryAccess();
        var biome = registries.registryOrThrow(Registries.BIOME).get(new ResourceLocation("aoemod:deep_dark"));
        for (var step : biome.getGenerationSettings().features()) for (var holder : step) {
            String name = holder.unwrapKey().map(key -> key.location().toString()).orElse("");
            helper.assertFalse(name.contains("glow_lichen") || name.contains("sculk_patch") || name.equals("minecraft:sculk_vein"),
                    "Legacy feature bypasses the new deep-dark ecology: " + name);
            helper.assertFalse(holder.value().feature().value().feature() instanceof net.minecraft.world.level.levelgen.feature.SculkPatchFeature,
                    "An anonymous or renamed native sculk patch bypasses the new deep-dark ecology: " + name);
        }
        var vanilla = (MultifaceGrowthConfiguration) registries.registryOrThrow(Registries.PLACED_FEATURE)
                .get(new ResourceLocation("minecraft:glow_lichen")).feature().value().config();
        var veins = (MultifaceGrowthConfiguration) registries.registryOrThrow(Registries.PLACED_FEATURE)
                .get(new ResourceLocation("aoemod:deep_dark/sculk_vein")).feature().value().config();
        helper.assertTrue(veins.placeBlock == Blocks.SCULK_VEIN, "The retained vein feature must place sculk veins");
        helper.assertTrue(veins.searchRange == vanilla.searchRange && veins.chanceOfSpreading == vanilla.chanceOfSpreading
                        && veins.canPlaceOnCeiling == vanilla.canPlaceOnCeiling && veins.canPlaceOnWall == vanilla.canPlaceOnWall
                        && veins.canPlaceOnFloor == vanilla.canPlaceOnFloor,
                "Veins must retain vanilla lichen geometry and spreading rules");
        helper.assertFalse(veins.canBePlacedOn.contains(Blocks.SCULK.builtInRegistryHolder()), "Veins cannot coat sculk");
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty", timeoutTicks = 400)
    public static void fullDecorationPlacesFloorDevicesAndNonSculkWallVeins(GameTestHelper helper) {
        int sensors = 0, shriekers = 0, veins = 0;
        var veinFeature = helper.getLevel().registryAccess().registryOrThrow(Registries.PLACED_FEATURE)
                .get(new ResourceLocation("aoemod:deep_dark/sculk_vein"));
        // Test scheduled patches on half-width floors, including the fallback
        // search when the selected column meets the cave wall.
        for (int seed = 0; seed < 40 && (sensors == 0 || shriekers == 0); seed++) {
            long worldSeed = helper.getLevel().getServer().getLevel(GameplayDimensions.DESERT).getSeed();
            BlockPos scheduled = DeepDarkVegetation.patchAnchor(worldSeed, seed, 0);
            int minX = Math.floorDiv(scheduled.getX(), 16) * 16;
            int minZ = Math.floorDiv(scheduled.getZ(), 16) * 16;
            BlockPos origin = new BlockPos(minX, 0, minZ);
            Map<BlockPos, BlockState> blocks = new HashMap<>();
            WorldGenLevel world = fixture(helper, blocks, p -> p.getX() >= minX + 8 || p.getY() <= -26 || p.getY() >= -4
                    ? Blocks.STONE.defaultBlockState() : Blocks.AIR.defaultBlockState());
            var generator = helper.getLevel().getServer().getLevel(GameplayDimensions.DESERT).getChunkSource().getGenerator();
            DeepDarkEcology.prepareSurfaces(world, minX, minZ);
            DeepDarkVegetation.place(new FeaturePlaceContext<>(Optional.empty(), world, generator, RandomSource.create(seed),
                    origin, NoneFeatureConfiguration.INSTANCE));
            veinFeature.placeWithBiomeCheck(world, generator, RandomSource.create(seed ^ 0x591aL), origin);
            int devices = 0;
            for (var entry : blocks.entrySet()) {
                var state = entry.getValue();
                if (state.is(Blocks.SCULK_SENSOR) || state.is(Blocks.SCULK_SHRIEKER)) {
                    helper.assertTrue(world.getBlockState(entry.getKey().below()).is(Blocks.SCULK), "Sculk devices must stand on a cave floor");
                    devices++;
                    if (state.is(Blocks.SCULK_SENSOR)) sensors++; else shriekers++;
                }
                if (state.is(Blocks.SCULK_VEIN)) {
                    veins++;
                    for (Direction face : Direction.values()) if (state.getValue(MultifaceBlock.getFaceProperty(face))) {
                        helper.assertFalse(world.getBlockState(entry.getKey().relative(face)).is(Blocks.SCULK), "Veins must not attach to sculk skins");
                    }
                }
                helper.assertFalse(state.is(Blocks.GLOW_LICHEN), "Deep-dark decoration must not generate glow lichen");
            }
            helper.assertTrue(devices == 5, "A selected decoration pass must keep its complete five-device patch");
        }
        helper.assertTrue(sensors >= 5 && shriekers >= 5 && veins > 0,
                "Complete ecology pass must generate all floor/wall features: " + sensors + "/" + shriekers + "/" + veins);
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty", timeoutTicks = 1200)
    public static void freshGeneratedCaveWallsReceiveNativeVeins(GameTestHelper helper) {
        var level = helper.getLevel().getServer().getLevel(GameplayDimensions.DESERT);
        var density = level.getChunkSource().randomState().router().finalDensity();
        int chunks = 0, veins = 0;
        // Select cave-wall terrain using only the density field, before decoration.
        // This exercises the generator's registered feature pipeline, not direct placement.
        for (int candidate = 0; candidate < 1024 && chunks < 12; candidate++) {
            int region = candidate / 256, local = candidate % 256;
            int chunkX = 70100 + region * 4096 + local % 16, chunkZ = -70100 + local / 16;
            boolean rock = false, cave = false;
            for (int dx : new int[]{2, 8, 13}) for (int dz : new int[]{2, 8, 13}) {
                double value = density.compute(new DensityFunction.SinglePointContext(chunkX * 16 + dx, -16, chunkZ * 16 + dz));
                rock |= value > 0;
                cave |= value <= 0;
            }
            if (!(rock && cave)) continue;
            var chunk = level.getChunk(chunkX, chunkZ);
            chunks++;
            BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
            for (int dx = 0; dx < 16; dx++) for (int dz = 0; dz < 16; dz++) for (int y = -31; y < 0; y++) {
                pos.set(chunkX * 16 + dx, y, chunkZ * 16 + dz);
                if (!GameplayDimensions.biomeIs(level, pos, "deep_dark")) continue;
                BlockState state = chunk.getBlockState(pos);
                helper.assertFalse(state.is(Blocks.GLOW_LICHEN), "Fresh deep-dark caves must not retain glow lichen");
                if (!state.is(Blocks.SCULK_VEIN)) continue;
                veins++;
                for (Direction face : Direction.values()) if (state.getValue(MultifaceBlock.getFaceProperty(face))) {
                    helper.assertFalse(level.getBlockState(pos.relative(face)).is(Blocks.SCULK),
                            "Native veins must not attach to the generated sculk skin at " + pos);
                }
            }
        }
        helper.assertTrue(chunks > 0 && veins > 0,
                "Native generation must decorate fresh cave walls: sampled chunks=" + chunks + ", veins=" + veins);
        System.out.println("Deep-dark generated-cave audit: " + chunks + " wall chunks, " + veins + " vein blocks");
        helper.succeed();
    }

    private static WorldGenLevel fixture(GameTestHelper helper, Map<BlockPos, BlockState> blocks,
                                         Function<BlockPos, BlockState> terrain) {
        var level = helper.getLevel().getServer().getLevel(GameplayDimensions.DESERT);
        var biome = level.registryAccess().registryOrThrow(Registries.BIOME).getHolderOrThrow(
                ResourceKey.create(Registries.BIOME, new ResourceLocation("aoemod:deep_dark")));
        Function<BlockPos, BlockState> at = pos -> blocks.getOrDefault(pos, terrain.apply(pos));
        Map<Long, ProtoChunk> chunks = new HashMap<>();
        java.util.function.BiFunction<Integer, Integer, ProtoChunk> chunkAt = (x, z) -> chunks.computeIfAbsent(ChunkPos.asLong(x, z), ignored ->
                new ProtoChunk(new ChunkPos(x, z), UpgradeData.EMPTY, level, level.registryAccess().registryOrThrow(Registries.BIOME), null) {
                    @Override public BlockState getBlockState(BlockPos pos) { return at.apply(pos); }
                    @Override public BlockState setBlockState(BlockPos pos, BlockState next, boolean moving) {
                        BlockState previous = at.apply(pos); blocks.put(pos.immutable(), next); return previous;
                    }
                });
        return (WorldGenLevel) Proxy.newProxyInstance(DeepDarkEcologyTests.class.getClassLoader(),
                new Class<?>[]{WorldGenLevel.class}, (proxy, method, args) -> {
                    switch (method.getName()) {
                        case "getLevel": return level;
                        case "getBiome": return biome;
                        case "getMinBuildHeight": return -32;
                        case "getMaxBuildHeight": return 16;
                        case "getHeight": return args == null || args.length == 0 ? 48 : 64;
                        case "getBlockState": return at.apply((BlockPos) args[0]);
                        case "getFluidState": return at.apply((BlockPos) args[0]).getFluidState();
                        case "isEmptyBlock": return at.apply((BlockPos) args[0]).isAir();
                        case "setBlock": blocks.put(((BlockPos) args[0]).immutable(), (BlockState) args[1]); return true;
                        case "levelEvent", "gameEvent", "scheduleTick": return null;
                        case "getBlockTicks", "getFluidTicks": return net.minecraft.world.ticks.BlackholeTickAccess.emptyLevelList();
                        case "getBlockEntity": return null;
                        case "getChunk": {
                            if (args[0] instanceof BlockPos pos) return chunkAt.apply(Math.floorDiv(pos.getX(), 16), Math.floorDiv(pos.getZ(), 16));
                            return chunkAt.apply((Integer) args[0], (Integer) args[1]);
                        }
                    }
                    try { return method.invoke(level, args); }
                    catch (InvocationTargetException exception) { throw exception.getCause(); }
                });
    }
}


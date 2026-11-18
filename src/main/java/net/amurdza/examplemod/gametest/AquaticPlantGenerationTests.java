package net.amurdza.examplemod.gametest;

import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.util.GameplayDimensions;
import net.amurdza.examplemod.worldgen.AquaticPlantPlacement;
import net.amurdza.examplemod.worldgen.DocumentedFloraFeature;
import net.amurdza.examplemod.worldgen.TropicalCoralReefs;
import net.amurdza.examplemod.worldgen.TropicalVegetation;
import net.amurdza.examplemod.worldgen.feature.features.SugarCaneFeature;
import net.amurdza.examplemod.worldgen.feature.configs.KelpFeatureConfig;
import net.amurdza.examplemod.worldgen.feature.features.KelpFeature;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.registries.ForgeRegistries;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@GameTestHolder(AOEMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class AquaticPlantGenerationTests {
    private static final BlockPos ROOT = new BlockPos(8, 101, 8);

    @GameTest(template = "plant_gameplay_empty")
    public static void kelpHasNativeStalksAndGrowingHeadsInEveryHabitat(GameTestHelper helper) {
        record Habitat(ResourceKey<Level> dimension, String biome, String kelp, boolean lava) { }
        Habitat[] habitats = {
                new Habitat(GameplayDimensions.TROPICAL, "rainforest", "minecraft:kelp", false),
                new Habitat(GameplayDimensions.SAVANNA, "savanna", "minecraft:kelp", false),
                new Habitat(GameplayDimensions.SNOWY, "snowy_forest", "minecraft:kelp", false),
                new Habitat(GameplayDimensions.SNOWY, "cold_ocean", "minecraft:kelp", false),
                new Habitat(GameplayDimensions.DESERT, "desert", "hybrid_aquatic:bull_kelp", false),
                new Habitat(GameplayDimensions.NETHER, "crimson_forest", "netherdepthsupgrade:crimson_kelp", true),
                new Habitat(GameplayDimensions.NETHER, "warped_forest", "netherdepthsupgrade:warped_kelp", true)
        };
        for (Habitat habitat : habitats) {
            Set<Integer> heights = new HashSet<>();
            for (int seed = 0; seed < 32; seed++) {
                Map<BlockPos, BlockState> blocks = new HashMap<>();
                WorldGenLevel world = fixture(helper, habitat.biome(), blocks, 14,
                        helper.getLevel().getServer().getLevel(habitat.dimension()),
                        habitat.lava() ? Blocks.MAGMA_BLOCK.defaultBlockState() : Blocks.SAND.defaultBlockState(),
                        (habitat.lava() ? Blocks.LAVA : Blocks.WATER).defaultBlockState());
                helper.assertTrue(DocumentedFloraFeature.placePlant(world, ROOT, habitat.kelp(), RandomSource.create(seed)),
                        habitat.kelp() + " must place a complete native column in " + habitat.dimension().location());
                int height = blocks.size();
                heights.add(height);
                helper.assertTrue(habitat.kelp().equals("hybrid_aquatic:bull_kelp") ? height == 13 : height >= 2 && height <= 11, "Deep-water kelp retains its original 2–11-block height range");
                for (int y = 0; y < height; y++) {
                    BlockPos pos = ROOT.above(y);
                    BlockState state = blocks.get(pos);
                    String expected = habitat.kelp() + (y == height - 1 ? "" : "_plant");
                    helper.assertTrue(state != null && state.is(ForgeRegistries.BLOCKS.getValue(new ResourceLocation(expected))),
                            "Every column needs contiguous native body blocks and exactly one head: " + expected);
                    helper.assertTrue(state.canSurvive(world, pos), "Every kelp section must retain native support");
                    helper.assertTrue(state.getFluidState().is(habitat.lava() ? FluidTags.LAVA : FluidTags.WATER),
                            "Kelp must preserve the existing fluid in each section");
                }
                BlockState head = blocks.get(ROOT.above(height - 1));
                int age = head.getValue(BlockStateProperties.AGE_25);
                helper.assertTrue(habitat.kelp().equals("hybrid_aquatic:bull_kelp") ? age == 25 && !head.isRandomlyTicking()
                                : age >= 20 && age <= 23 && head.isRandomlyTicking(),
                        "Generated kelp heads retain original age 20–23, rather than the non-growing maximum age 25");
            }
            helper.assertTrue(habitat.kelp().equals("hybrid_aquatic:bull_kelp") ? heights.equals(Set.of(13)) : heights.size() >= 8, "Kelp height must vary naturally across seeds: " + habitat.kelp());
        }
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void matureBullKelpCannotResumeGrowing(GameTestHelper helper) {
        ServerLevel desert = helper.getLevel().getServer().getLevel(GameplayDimensions.DESERT);
        var head = ForgeRegistries.BLOCKS.getValue(new ResourceLocation("hybrid_aquatic", "bull_kelp"));
        BlockPos root = helper.absolutePos(BlockPos.ZERO).atY(220);
        desert.setBlock(root.below(), Blocks.SAND.defaultBlockState(), 18);
        desert.setBlock(root, head.defaultBlockState().setValue(BlockStateProperties.AGE_25, 25), 18);
        desert.setBlock(root.above(), Blocks.WATER.defaultBlockState(), 18);
        desert.setBlock(root.above(2), Blocks.WATER.defaultBlockState(), 18);
        helper.assertFalse(desert.getBlockState(root).isRandomlyTicking(), "Mature bull kelp must stop receiving growth ticks");
        for (int tick = 0; tick < 64; tick++) desert.getBlockState(root).randomTick(desert, root, RandomSource.create(tick));
        helper.assertTrue(desert.getBlockState(root.above()).is(Blocks.WATER)
                        && desert.getBlockState(root).getValue(BlockStateProperties.AGE_25) == 25,
                "Even an explicitly invoked tick must preserve the mature generated head and its water clearance");
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void kelpStopsBelowTheExistingSurfaceAndPreservesObstacles(GameTestHelper helper) {
        for (int depth = 0; depth <= 5; depth++) {
            Map<BlockPos, BlockState> blocks = new HashMap<>();
            WorldGenLevel world = fixture(helper, "rainforest", blocks, depth);
            boolean placed = DocumentedFloraFeature.placePlant(world, ROOT, "minecraft:kelp", RandomSource.create(42));
            helper.assertTrue(placed == (depth >= 2), "Natural kelp requires two existing source-fluid cells");
            if (placed) {
                helper.assertTrue(blocks.size() == Math.min(2 + RandomSource.create(42).nextInt(10), depth - 1),
                        "Shallow kelp truncates its stalk one block below the original water surface");
                helper.assertTrue(world.getBlockState(ROOT.above(depth - 1)).is(Blocks.WATER)
                                && world.getBlockState(ROOT.above(depth)).isAir(),
                        "Kelp must not create water at or above the original surface");
            } else helper.assertTrue(blocks.isEmpty(), "Rejected shallow kelp must leave the world untouched");
        }
        Map<BlockPos, BlockState> blocked = new HashMap<>();
        blocked.put(ROOT.above(3), Blocks.STONE.defaultBlockState());
        WorldGenLevel world = fixture(helper, "rainforest", blocked, 14);
        helper.assertTrue(DocumentedFloraFeature.placePlant(world, ROOT, "minecraft:kelp", RandomSource.create(1)),
                "Kelp can shorten its column below an obstruction");
        helper.assertTrue(blocked.get(ROOT.above(3)).is(Blocks.STONE) && blocked.size() == 3,
                "The obstruction remains untouched and the shortened column ends with a head");
        helper.assertTrue(blocked.get(ROOT.above()).is(Blocks.KELP), "A shortened stalk must still have its native head");
        Map<BlockPos, BlockState> unsupported = new HashMap<>();
        unsupported.put(ROOT.below(), Blocks.AIR.defaultBlockState());
        helper.assertFalse(AquaticPlantPlacement.placeKelp(fixture(helper, "rainforest", unsupported, 14), ROOT,
                        "minecraft:kelp", RandomSource.create(42)), "Unsupported kelp must reject the entire column");
        helper.assertTrue(unsupported.size() == 1, "An invalid root must not leave orphaned kelp sections");
        Map<BlockPos, BlockState> flowing = new HashMap<>();
        flowing.put(ROOT, Blocks.WATER.defaultBlockState().setValue(BlockStateProperties.LEVEL, 1));
        helper.assertFalse(AquaticPlantPlacement.placeKelp(fixture(helper, "rainforest", flowing, 14), ROOT,
                        "minecraft:kelp", RandomSource.create(1)), "Kelp must not turn flowing fluid into new source blocks");
        helper.assertTrue(flowing.size() == 1 && !flowing.get(ROOT).getFluidState().isSource(),
                "Rejecting flowing water must preserve its original fluid level");
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void configuredAndDocumentedKelpShareTheOriginalColumnBehavior(GameTestHelper helper) {
        KelpFeature feature = new KelpFeature(KelpFeatureConfig.CODEC);
        for (int seed = 0; seed < 32; seed++) {
            Map<BlockPos, BlockState> documentedBlocks = new HashMap<>();
            Map<BlockPos, BlockState> configuredBlocks = new HashMap<>();
            WorldGenLevel documented = fixture(helper, "rainforest", documentedBlocks, 14);
            WorldGenLevel configured = fixture(helper, "rainforest", configuredBlocks, 14);
            helper.assertTrue(DocumentedFloraFeature.placePlant(documented, ROOT, "minecraft:kelp", RandomSource.create(seed)),
                    "Documented flora must create the whole kelp stalk");
            helper.assertTrue(feature.place(new FeaturePlaceContext<>(Optional.empty(), configured,
                            configured.getLevel().getChunkSource().getGenerator(), RandomSource.create(seed), ROOT,
                            new KelpFeatureConfig(Blocks.KELP.defaultBlockState(), Blocks.KELP_PLANT.defaultBlockState(), false))),
                    "Legacy configured features must continue creating kelp stalks");
            helper.assertTrue(configuredBlocks.equals(documentedBlocks),
                    "Both world-generation entry points must use the same native stalk and randomized head age");
        }
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void tallSeagrassNeverCreatesAnUpperWaterSource(GameTestHelper helper) {
        Map<BlockPos, BlockState> blocks = new HashMap<>();
        WorldGenLevel shallow = fixture(helper, "mangrove_swamp", blocks, 1);
        helper.assertTrue(DocumentedFloraFeature.placePlant(shallow, ROOT, "minecraft:tall_seagrass", RandomSource.create(1)),
                "A one-block tropical pool substitutes short seagrass or sea pickles for tall seagrass");
        helper.assertTrue(blocks.size() == 1 && !blocks.get(ROOT).is(Blocks.TALL_SEAGRASS)
                        && shallow.getBlockState(ROOT.above()).isAir(),
                "The shallow substitute must not create an upper water source or a partial lower half");
        blocks.clear();
        helper.assertTrue(DocumentedFloraFeature.placePlant(shallow, ROOT, "minecraft:seagrass", RandomSource.create(1)),
                "Ordinary seagrass remains valid in one block of water");
        blocks.clear();
        WorldGenLevel twoDeep = fixture(helper, "mangrove_swamp", blocks, 2);
        helper.assertTrue(DocumentedFloraFeature.placePlant(twoDeep, ROOT, "minecraft:tall_seagrass", RandomSource.create(1))
                        && blocks.size() == 1 && !blocks.get(ROOT).is(Blocks.TALL_SEAGRASS),
                "Two-deep tropical pools also use a one-block substitute");
        blocks.clear();
        WorldGenLevel deep = fixture(helper, "mangrove_swamp", blocks, 3);
        helper.assertTrue(DocumentedFloraFeature.placePlant(deep, ROOT, "minecraft:tall_seagrass", RandomSource.create(1)),
                "Three existing source-water cells accept actual tall seagrass below the surface");
        helper.assertTrue(blocks.size() == 2
                        && blocks.get(ROOT).getValue(BlockStateProperties.DOUBLE_BLOCK_HALF) == DoubleBlockHalf.LOWER
                        && blocks.get(ROOT.above()).getValue(BlockStateProperties.DOUBLE_BLOCK_HALF) == DoubleBlockHalf.UPPER,
                "Tall seagrass must be a complete native two-part plant");
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void mangroveShorePlantsKeepWaterOnlyAtTheirRoots(GameTestHelper helper) {
        for (String id : new String[]{"minecraft:sugar_cane", "minecraft:small_dripleaf", "minecraft:big_dripleaf",
                "upgrade_aquatic:tall_pickerelweed", "farmersdelight:wild_rice"}) {
            Map<BlockPos, BlockState> blocks = new HashMap<>();
            WorldGenLevel world = fixture(helper, "mangrove_swamp", blocks, 1);
            helper.assertTrue(DocumentedFloraFeature.placePlant(world, ROOT, id, RandomSource.create(42)),
                    id + " must generate naturally in a one-block shallow pool on native valid soil");
            helper.assertTrue(blocks.get(ROOT).getFluidState().is(FluidTags.WATER),
                    "The rooted lower cell must preserve its original water");
            helper.assertTrue(blocks.containsKey(ROOT.above()) && blocks.get(ROOT.above()).getFluidState().isEmpty(),
                    "The above-water plant must emerge into air without inventing fluid");
            for (var entry : blocks.entrySet()) {
                helper.assertTrue(entry.getValue().canSurvive(world, entry.getKey()), id + " must retain native survival/support");
                if (entry.getKey().getY() > ROOT.getY()) helper.assertTrue(entry.getValue().getFluidState().isEmpty(),
                        id + " must never carry water above the existing surface");
            }
        }
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void emergentCaneAndDripleafRejectDeepWaterAndBlockedUpperCells(GameTestHelper helper) {
        for (String id : new String[]{"minecraft:sugar_cane", "minecraft:small_dripleaf", "minecraft:big_dripleaf", "farmersdelight:wild_rice"}) {
            Map<BlockPos, BlockState> blocks = new HashMap<>();
            WorldGenLevel deep = fixture(helper, "mangrove_swamp", blocks, 2);
            helper.assertFalse(DocumentedFloraFeature.placePlant(deep, ROOT, id, RandomSource.create(42)),
                    id + " must root in exactly one water block, rather than deep submerged columns");
            helper.assertTrue(blocks.isEmpty(), "An invalid wet column must leave the original water unchanged");
        }
        Map<BlockPos, BlockState> blocked = new HashMap<>();
        blocked.put(ROOT.above(), Blocks.STONE.defaultBlockState());
        WorldGenLevel world = fixture(helper, "mangrove_swamp", blocked, 1);
        helper.assertFalse(DocumentedFloraFeature.placePlant(world, ROOT, "upgrade_aquatic:tall_pickerelweed", RandomSource.create(5)),
                "Native tall pickerelweed must reject blocked upper space atomically");
        helper.assertTrue(blocked.size() == 1 && world.getBlockState(ROOT).is(Blocks.WATER),
                "Rejected tall pickerelweed must not leave a lower half or change the pool");
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void pickerelweedCanKeepBothExistingUnderwaterHalves(GameTestHelper helper) {
        Map<BlockPos, BlockState> blocks = new HashMap<>();
        WorldGenLevel world = fixture(helper, "mangrove_swamp", blocks, 2);
        helper.assertTrue(DocumentedFloraFeature.placePlant(world, ROOT, "upgrade_aquatic:tall_pickerelweed", RandomSource.create(5)),
                "Pickerelweed must retain its native valid submerged two-cell placement");
        helper.assertTrue(blocks.size() == 2 && blocks.values().stream().allMatch(state -> state.getFluidState().is(FluidTags.WATER)),
                "Both underwater halves preserve only their own preexisting water");
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void naturalShorePlantsRejectDryOrigins(GameTestHelper helper) {
        for (String id : new String[]{"minecraft:sugar_cane", "minecraft:small_dripleaf", "minecraft:big_dripleaf",
                "upgrade_aquatic:pickerelweed", "upgrade_aquatic:tall_pickerelweed", "farmersdelight:wild_rice"}) {
            Map<BlockPos, BlockState> blocks = new HashMap<>();
            WorldGenLevel dry = fixture(helper, "mangrove_swamp", blocks, 0);
            helper.assertFalse(DocumentedFloraFeature.placePlant(dry, ROOT, id, RandomSource.create(42)),
                    id + " must generate only with an existing water cell at its rooted origin");
            helper.assertTrue(blocks.isEmpty(), "Rejected dry shoreline generation must leave the floor and air unchanged");
        }
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void connectedWetCropsReplacePicklesAndRespectOtherShorePlants(GameTestHelper helper) {
        for (String id : new String[]{"farmersdelight:wild_rice", "minecraft:sugar_cane"}) {
            Map<BlockPos, BlockState> blocks = new HashMap<>();
            var world = fixture(helper, "mangrove_swamp", blocks, 1);
            var pickles = Blocks.SEA_PICKLE.defaultBlockState().setValue(BlockStateProperties.WATERLOGGED, true);
            for (int x = -2; x <= 3; x++) for (int z = -2; z <= 3; z++) blocks.put(ROOT.offset(x, 0, z), pickles);
            helper.assertTrue(TropicalVegetation.placeWetPatch(world, ROOT, id, RandomSource.create(212)) == 5,
                    "A seabed of pickles must accept a complete five-root crop patch");
            var crop = ForgeRegistries.BLOCKS.getValue(new ResourceLocation(id));
            Set<BlockPos> reached = new HashSet<>();
            var pending = new java.util.ArrayDeque<BlockPos>();
            pending.add(ROOT);
            while (!pending.isEmpty()) {
                BlockPos pos = pending.remove();
                if (!world.getBlockState(pos).is(crop) || !reached.add(pos)) continue;
                for (var dir : net.minecraft.core.Direction.Plane.HORIZONTAL) pending.add(pos.relative(dir));
            }
            helper.assertTrue(reached.size() == 5, "All five roots must connect by shared sides");
            blocks.clear();
            blocks.put(ROOT.east(), Blocks.BIG_DRIPLEAF_STEM.defaultBlockState());
            helper.assertTrue(TropicalVegetation.placeWetPatch(world, ROOT, id, RandomSource.create(212)) == 0
                    && blocks.size() == 1, "Overlapping crops must be rejected without partial writes");
        }
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void riceAndBothDripleavesFormCompleteShallowSixBySixPatches(GameTestHelper helper) {
        for (String id : new String[]{"farmersdelight:wild_rice", "minecraft:small_dripleaf", "minecraft:big_dripleaf"}) {
            Map<BlockPos, BlockState> blocks = new HashMap<>();
            WorldGenLevel world = fixture(helper, "mangrove_swamp", blocks, 1);
            int roots = TropicalVegetation.placeWetPatch(world, ROOT, id, RandomSource.create(77182));
            helper.assertTrue(roots >= 4 && roots <= 7,
                    "The actual aquatic plant must occur in a complete shallow patch: " + id + "; roots=" + roots);
            if (id.equals("farmersdelight:wild_rice")) helper.assertTrue(roots == 5, "Rice patches contain exactly five roots");
            if (id.equals("minecraft:big_dripleaf")) {
                var bases = blocks.keySet().stream().filter(pos -> pos.getY() == ROOT.getY()).toList();
                for (int i = 0; i < bases.size(); i++) for (int j = i + 1; j < bases.size(); j++) {
                    helper.assertTrue(Math.max(Math.abs(bases.get(i).getX() - bases.get(j).getX()),
                                    Math.abs(bases.get(i).getZ() - bases.get(j).getZ())) > 1,
                            "Large dripleaves must leave a walking gap, including diagonals");
                }
            }
            helper.assertTrue(blocks.keySet().stream().allMatch(pos -> pos.getX() >= ROOT.getX() - 2
                            && pos.getX() <= ROOT.getX() + 3 && pos.getZ() >= ROOT.getZ() - 2 && pos.getZ() <= ROOT.getZ() + 3),
                    "Aquatic patch footprints must remain within exactly six by six blocks");
            for (var entry : blocks.entrySet()) {
                helper.assertTrue(entry.getValue().canSurvive(world, entry.getKey()), "Each aquatic patch section retains native support");
                helper.assertTrue(entry.getKey().getY() == ROOT.getY()
                                ? entry.getValue().getFluidState().is(FluidTags.WATER) : entry.getValue().getFluidState().isEmpty(),
                        "Shallow rice and dripleaf have one wet root cell and only dry cells above it");
            }
            Map<BlockPos, BlockState> deepBlocks = new HashMap<>();
            helper.assertTrue(TropicalVegetation.placeWetPatch(fixture(helper, "mangrove_swamp", deepBlocks, 2), ROOT,
                            id, RandomSource.create(77182)) == 0 && deepBlocks.isEmpty(),
                    "Emergent patches cannot migrate into two-block-deep water");
        }
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void naturalSugarCaneIsAlwaysExactlyThreeBlocks(GameTestHelper helper) {
        for (int seed = 0; seed < 64; seed++) {
            Map<BlockPos, BlockState> blocks = new HashMap<>();
            WorldGenLevel world = fixture(helper, "mangrove_swamp", blocks, 1);
            helper.assertTrue(DocumentedFloraFeature.placePlant(world, ROOT, "minecraft:sugar_cane", RandomSource.create(seed)),
                    "Shallow cane must place successfully");
            helper.assertTrue(blocks.size() == 3 && blocks.containsKey(ROOT.above(2)),
                    "Sugarcane columns must always be three blocks tall");
        }
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void pickerelweedPatchesAreContinuousWithoutEnclosedGaps(GameTestHelper helper) {
        for (String id : new String[]{"upgrade_aquatic:pickerelweed", "upgrade_aquatic:tall_pickerelweed"}) {
            for (int seed = 0; seed < 32; seed++) {
                Map<BlockPos, BlockState> blocks = new HashMap<>();
                WorldGenLevel world = fixture(helper, "mangrove_swamp", blocks, seed % 2 == 0 ? 1 : 3);
                int count = TropicalVegetation.placeWetPatch(world, ROOT, id, RandomSource.create(seed));
                Set<BlockPos> roots = new HashSet<>();
                blocks.forEach((pos, state) -> { if (pos.getY() == ROOT.getY()) roots.add(pos); });
                helper.assertTrue(count >= 10 && count <= 18 && count == roots.size(),
                        "Continuous pickerelweed must create ten to eighteen actual roots: " + id + "; count=" + count);
                if (seed % 2 == 0) helper.assertTrue(roots.stream().allMatch(pos ->
                                ForgeRegistries.BLOCKS.getKey(world.getBlockState(pos).getBlock()).getPath().equals("tall_pickerelweed")),
                        "Every shallow pickerelweed root uses the tall form");
                else helper.assertTrue(roots.stream().map(pos -> ForgeRegistries.BLOCKS.getKey(world.getBlockState(pos).getBlock()).getPath())
                                .distinct().count() == 2,
                        "Deep pickerelweed patches mix both native forms");
                helper.assertTrue(roots.stream().allMatch(pos -> pos.getX() >= ROOT.getX() - 2 && pos.getX() <= ROOT.getX() + 3
                                && pos.getZ() >= ROOT.getZ() - 2 && pos.getZ() <= ROOT.getZ() + 3),
                        "The continuous footprint must remain within six by six");
                Set<BlockPos> reached = new HashSet<>();
                java.util.ArrayDeque<BlockPos> queue = new java.util.ArrayDeque<>();
                queue.add(roots.iterator().next());
                while (!queue.isEmpty()) {
                    BlockPos pos = queue.removeFirst();
                    if (!reached.add(pos)) continue;
                    for (var direction : net.minecraft.core.Direction.Plane.HORIZONTAL) {
                        BlockPos next = pos.relative(direction);
                        if (roots.contains(next) && !reached.contains(next)) queue.add(next);
                    }
                }
                helper.assertTrue(reached.equals(roots), "Every pickerelweed root must join the patch by a face, rather than a diagonal");
                // Flood the complement from a padded outside border. Every empty
                // cell must remain reachable, so the patch encloses no holes.
                reached.clear();
                queue.add(ROOT.offset(-3, 0, -3));
                while (!queue.isEmpty()) {
                    BlockPos pos = queue.removeFirst();
                    if (roots.contains(pos) || !reached.add(pos)) continue;
                    for (var direction : net.minecraft.core.Direction.Plane.HORIZONTAL) {
                        BlockPos next = pos.relative(direction);
                        if (next.getX() >= ROOT.getX() - 3 && next.getX() <= ROOT.getX() + 4
                                && next.getZ() >= ROOT.getZ() - 3 && next.getZ() <= ROOT.getZ() + 4
                                && !reached.contains(next)) queue.add(next);
                    }
                }
                helper.assertTrue(reached.size() + roots.size() == 64, "No empty cell may be enclosed inside pickerelweed");
            }
        }
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void tropicalBigDripleafAlwaysEmergesAsTwoBlocks(GameTestHelper helper) {
        for (int seed = 0; seed < 64; seed++) {
            Map<BlockPos, BlockState> blocks = new HashMap<>();
            WorldGenLevel world = fixture(helper, "mangrove_swamp", blocks, 1);
            helper.assertTrue(DocumentedFloraFeature.placePlant(world, ROOT, "minecraft:big_dripleaf", RandomSource.create(seed)),
                    "A valid shallow pool must produce the full dripleaf column");
            helper.assertTrue(blocks.size() == 2 && blocks.get(ROOT).is(Blocks.BIG_DRIPLEAF_STEM)
                            && blocks.get(ROOT.above()).is(Blocks.BIG_DRIPLEAF),
                    "Tropical big dripleaf has exactly one wet stem and one dry leaf block");
        }
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void legacyCanePassCannotRestoreDryCustomPlantsButRetainsExternalBehavior(GameTestHelper helper) {
        SugarCaneFeature feature = new SugarCaneFeature(NoneFeatureConfiguration.CODEC);
        Map<BlockPos, BlockState> customBlocks = new HashMap<>();
        WorldGenLevel custom = fixture(helper, "mangrove_swamp", customBlocks, 0);
        helper.assertFalse(feature.place(new FeaturePlaceContext<>(Optional.empty(), custom,
                        custom.getLevel().getChunkSource().getGenerator(), RandomSource.create(42), ROOT,
                        NoneFeatureConfiguration.INSTANCE)),
                "The legacy dry river-bank feature must be disabled in custom dimensions");
        helper.assertTrue(customBlocks.isEmpty(), "The disabled legacy pass must not place dry cane");

        // Chunk seven faces the old repeated river on its west side. Keep this
        // existing external-world path functional while custom worlds use water roots.
        Map<BlockPos, BlockState> externalBlocks = new HashMap<>();
        externalBlocks.put(new BlockPos(111, 100, 6), Blocks.WATER.defaultBlockState());
        ServerLevel overworld = helper.getLevel().getServer().overworld();
        WorldGenLevel external = fixture(helper, "mangrove_swamp", externalBlocks, 0, overworld);
        BlockPos bank = new BlockPos(112, 101, 6);
        helper.assertTrue(feature.place(new FeaturePlaceContext<>(Optional.empty(), external,
                        overworld.getChunkSource().getGenerator(), RandomSource.create(42), bank,
                        NoneFeatureConfiguration.INSTANCE)),
                "External worlds must retain their existing dry-bank cane feature");
        for (int height = 0; height < 3; height++) {
            helper.assertTrue(external.getBlockState(bank.above(height)).is(Blocks.SUGAR_CANE)
                            && external.getFluidState(bank.above(height)).isEmpty(),
                    "The retained external path must create its native three-block dry cane column");
        }
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void nativeGiantCoralShapesStaySubmergedAndUseOnlyDocumentedFamilies(GameTestHelper helper) {
        var reef = helper.getLevel().registryAccess().registryOrThrow(Registries.BIOME)
                .get(new ResourceLocation("aoemod:coral_reef"));
        int plants = net.minecraft.world.level.levelgen.GenerationStep.Decoration.VEGETAL_DECORATION.ordinal();
        helper.assertFalse(reef.getGenerationSettings().features().get(plants).stream().anyMatch(feature ->
                        feature.unwrapKey().map(key -> key.location().toString().equals("minecraft:warm_ocean_vegetation")).orElse(false)),
                "The native global-tag coral pass must not duplicate our colonies or reintroduce undocumented species");
        Set<String> families = new HashSet<>();
        for (int shape = 0; shape < 3; shape++) {
            for (int seed = 0; seed < 24; seed++) {
                Map<BlockPos, BlockState> blocks = new HashMap<>();
                WorldGenLevel world = fixture(helper, "coral_reef", blocks, 12);
                helper.assertTrue(TropicalCoralReefs.placeColony(world, ROOT, RandomSource.create(seed * 1000003L), shape),
                        "Native giant coral shape " + shape + " must place its real body and decorations in deep water");
                helper.assertFalse(blocks.isEmpty(), "Successful coral generation must create actual blocks");
                for (var entry : blocks.entrySet()) {
                    BlockState state = entry.getValue();
                    String id = ForgeRegistries.BLOCKS.getKey(state.getBlock()).toString();
                    helper.assertTrue(TropicalCoralReefs.isDocumentedCoral(state)
                                    && state.is(BlockTags.create(new ResourceLocation("aoemod:dimension_sources/tropical")))
                                    && !id.contains("branch_coral"),
                            "Every body, fan and plant must belong to a documented tropical coral family: " + id);
                    helper.assertTrue(entry.getKey().getY() >= 101 && entry.getKey().getY() <= 112,
                            "A coral shape cannot replace soil or invent water above the actual surface");
                    helper.assertTrue(state.canSurvive(world, entry.getKey()), "Coral decorations require native support");
                    if (id.endsWith("_coral_block")) families.add(id);
                }
            }
        }
        helper.assertTrue(families.size() == 8, "All eight documented giant coral families must remain represented: " + families);
        Map<BlockPos, BlockState> blocks = new HashMap<>();
        helper.assertFalse(TropicalCoralReefs.placeColony(fixture(helper, "coral_reef", blocks, 6), ROOT, RandomSource.create(1), 0),
                "Giant coral origins must have at least seven actual water cells");
        helper.assertTrue(blocks.isEmpty(), "Shallow water must remain unchanged by a rejected giant coral");
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void giantCoralOriginsUseEveryChunksCentralEightByEight(GameTestHelper helper) {
        Set<BlockPos> anchors = new HashSet<>();
        for (int x = -8; x <= 8; x++) for (int z = -8; z <= 8; z++) {
            for (BlockPos anchor : TropicalCoralReefs.anchorsForChunk(77182, x, z)) {
                helper.assertTrue(Math.floorDiv(anchor.getX(), 16) == x && Math.floorDiv(anchor.getZ(), 16) == z,
                        "A giant coral must belong to the chunk that contains its origin, including negative chunks");
                helper.assertTrue(anchors.add(anchor), "A colony origin must not be generated by two neighboring chunks");
            }
        }
        helper.assertTrue(anchors.size() == 289,
                "Every eligible chunk owns exactly one coral origin");
        for (BlockPos anchor : anchors) {
            helper.assertTrue(Math.floorMod(anchor.getX(), 16) >= 4 && Math.floorMod(anchor.getX(), 16) <= 11
                            && Math.floorMod(anchor.getZ(), 16) >= 4 && Math.floorMod(anchor.getZ(), 16) <= 11,
                    "Coral trunks must remain in their chunk's central eight by eight cells");
        }
        for (BlockPos first : anchors) for (BlockPos second : anchors) {
            if (!first.equals(second)) helper.assertTrue(first.distSqr(second) >= 81,
                    "Neighboring giant coral skeletons retain a gap despite independent central-cell jitter");
        }
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void coralTreesAlwaysHaveFourBranchesAndMixedSurfaceLife(GameTestHelper helper) {
        int matching = 0;
        int coralDecorations = 0;
        int pickles = 0;
        int seagrass = 0;
        int tallGrass = 0;
        Map<String, Integer> decorationFamilies = new HashMap<>();
        for (int seed = 0; seed < 160; seed++) {
            Map<BlockPos, BlockState> blocks = new HashMap<>();
            WorldGenLevel world = fixture(helper, "coral_reef", blocks, 7);
            helper.assertTrue(TropicalCoralReefs.placeColony(world, ROOT, RandomSource.create(seed), seed % 3),
                    "Every legacy shape choice must now create the same complete tree in seven source-water blocks");
            String body = ForgeRegistries.BLOCKS.getKey(blocks.get(ROOT).getBlock()).toString().replace("_block", "");
            for (net.minecraft.core.Direction direction : net.minecraft.core.Direction.Plane.HORIZONTAL) {
                helper.assertTrue(blocks.entrySet().stream().anyMatch(entry ->
                                ForgeRegistries.BLOCKS.getKey(entry.getValue().getBlock()).getPath().endsWith("_coral_block")
                                        && entry.getKey().getX() == ROOT.relative(direction, 2).getX()
                                        && entry.getKey().getZ() == ROOT.relative(direction, 2).getZ()),
                        "Every coral tree must include an outer branch in all four directions");
            }
            for (BlockState state : blocks.values()) {
                String id = ForgeRegistries.BLOCKS.getKey(state.getBlock()).toString();
                if (id.endsWith("_coral_block")) continue;
                if (id.contains("_coral")) {
                    coralDecorations++;
                    decorationFamilies.merge(id.replace("_wall_fan", "").replace("_fan", ""), 1, Integer::sum);
                }
                if (id.equals(body) || id.equals(body + "_fan") || id.equals(body + "_wall_fan")) matching++;
                if (state.is(Blocks.SEA_PICKLE)) pickles++;
                if (state.is(Blocks.SEAGRASS)) seagrass++;
                if (state.is(Blocks.TALL_SEAGRASS)) tallGrass++;
            }
            int exposedTops = 0;
            for (var entry : blocks.entrySet()) {
                String id = ForgeRegistries.BLOCKS.getKey(entry.getValue().getBlock()).toString();
                if (!id.endsWith("_coral_block")) continue;
                BlockState above = world.getBlockState(entry.getKey().above());
                if (ForgeRegistries.BLOCKS.getKey(above.getBlock()).getPath().endsWith("_coral_block")) continue;
                exposedTops++;
                helper.assertTrue(TropicalCoralReefs.isDocumentedCoral(above) && !above.is(Blocks.WATER),
                        "Each exposed upper face must carry an actual aquatic decoration");
            }
            helper.assertTrue(exposedTops == 9, "Every complete coral tree has nine occupied upper faces");
            for (net.minecraft.core.Direction direction : net.minecraft.core.Direction.Plane.HORIZONTAL) {
                helper.assertTrue(ForgeRegistries.BLOCKS.getKey(world.getBlockState(ROOT.above(3).relative(direction)).getBlock())
                                .getPath().endsWith("_coral_block"),
                        "Each branch junction is four blocks above the seafloor");
            }
        }
        double ownFraction = (double) matching / coralDecorations;
        helper.assertTrue(ownFraction >= 0.08 && ownFraction <= 0.17,
                "The body's family must receive the same one-in-eight share as each other coral family; actual=" + ownFraction);
        helper.assertTrue(decorationFamilies.size() == 8,
                "All eight allowed coral families must decorate actual trees");
        for (var family : decorationFamilies.entrySet()) {
            double share = (double) family.getValue() / coralDecorations;
            helper.assertTrue(share >= 0.085 && share <= 0.165,
                    "No coral family receives a favored decoration share: " + family.getKey() + "=" + share);
        }
        helper.assertTrue(pickles > 300 && seagrass == 0 && tallGrass == 0,
                "Former top seagrass choices must now become four-pickle decorations");
        helper.succeed();
    }

    static WorldGenLevel fixture(GameTestHelper helper, String biomeName,
                                          Map<BlockPos, BlockState> blocks, int waterDepth) {
        var tropical = helper.getLevel().getServer().getLevel(GameplayDimensions.TROPICAL);
        return fixture(helper, biomeName, blocks, waterDepth, tropical);
    }

    static WorldGenLevel fixture(GameTestHelper helper, String biomeName,
                                          Map<BlockPos, BlockState> blocks, int waterDepth, ServerLevel world) {
        return fixture(helper, biomeName, blocks, waterDepth, world,
                Blocks.MOSS_BLOCK.defaultBlockState(), Blocks.WATER.defaultBlockState());
    }

    static WorldGenLevel fixture(GameTestHelper helper, String biomeName,
                                          Map<BlockPos, BlockState> blocks, int waterDepth, ServerLevel world,
                                          BlockState floor, BlockState fluid) {
        var tropical = helper.getLevel().getServer().getLevel(GameplayDimensions.TROPICAL);
        var biome = tropical.registryAccess().registryOrThrow(Registries.BIOME).getHolderOrThrow(
                ResourceKey.create(Registries.BIOME, new ResourceLocation("aoemod", biomeName)));
        java.util.function.Function<BlockPos, BlockState> stateAt = pos -> blocks.getOrDefault(pos,
                pos.getY() <= 100 ? floor : pos.getY() <= 100 + waterDepth ? fluid : Blocks.AIR.defaultBlockState());
        return (WorldGenLevel) Proxy.newProxyInstance(AquaticPlantGenerationTests.class.getClassLoader(),
                new Class<?>[]{WorldGenLevel.class}, (proxy, method, args) -> {
                    switch (method.getName()) {
                        case "getLevel": return world;
                        case "getBiome": return biome;
                        case "getHeight": return args == null || args.length == 0 ? tropical.getHeight()
                                : args[0] == Heightmap.Types.OCEAN_FLOOR_WG ? 101 : 101 + waterDepth;
                        case "getBlockState": return stateAt.apply((BlockPos) args[0]);
                        case "getFluidState": return stateAt.apply((BlockPos) args[0]).getFluidState();
                        case "isStateAtPosition": return ((java.util.function.Predicate<BlockState>) args[1]).test(stateAt.apply((BlockPos) args[0]));
                        case "isFluidAtPosition": return ((java.util.function.Predicate<net.minecraft.world.level.material.FluidState>) args[1])
                                .test(stateAt.apply((BlockPos) args[0]).getFluidState());
                        case "isEmptyBlock": return stateAt.apply((BlockPos) args[0]).isAir();
                        case "setBlock": blocks.put(((BlockPos) args[0]).immutable(), (BlockState) args[1]); return true;
                        default:
                            try { return method.invoke(world, args); }
                            catch (InvocationTargetException failure) { throw failure.getCause(); }
                    }
                });
    }
}

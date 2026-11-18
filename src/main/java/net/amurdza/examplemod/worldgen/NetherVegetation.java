package net.amurdza.examplemod.worldgen;

import net.amurdza.examplemod.config.CaveStructureConfig;
import net.amurdza.examplemod.util.GameplayDimensions;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Original AoE cave-floor mixtures, sampled independently on the split Nether's actual floors. */
public final class NetherVegetation {
    private static final double OLD_PILLAR_RADIUS = CaveStructureConfig.NETHER_CAVE_CENTRAL_PILLAR_DIAMETER / 2.0;
    private static final double OLD_RING_RADIUS = OLD_PILLAR_RADIUS + CaveStructureConfig.NETHER_CAVE_HORIZONTAL_RADIUS;
    private static final double OLD_RING_AREA = Math.PI * (OLD_RING_RADIUS * OLD_RING_RADIUS - OLD_PILLAR_RADIUS * OLD_PILLAR_RADIUS);
    private static final double OLD_OUTER_INNER_RADIUS = OLD_RING_RADIUS + 2 * CaveStructureConfig.NETHER_CAVE_LIQUID_RADIUS;
    private static final double OLD_OUTER_RADIUS = OLD_OUTER_INNER_RADIUS + CaveStructureConfig.NETHER_CAVE_HORIZONTAL_RADIUS;
    private static final double OLD_DRY_AREA = OLD_RING_AREA
            + Math.PI * (OLD_OUTER_RADIUS * OLD_OUTER_RADIUS - OLD_OUTER_INNER_RADIUS * OLD_OUTER_INNER_RADIUS);
    // Trees used the same density on both banks; all seven flower patches belonged to the inner bank.
    public static final double PALE_OAK_PER_COLUMN = 24.0 / OLD_RING_AREA;
    public static final int EYEBLOSSOMS_PER_PATCH = 5;
    // One third of the previous thirty floor-hit draws; keep five-flower patches.
    public static final int EYEBLOSSOM_PATCH_ATTEMPTS = 10;

    private NetherVegetation() { }

    public static boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        if (!GameplayDimensions.is(level, GameplayDimensions.NETHER)) return false;
        int minX = Math.floorDiv(context.origin().getX(), 16) * 16;
        int minZ = Math.floorDiv(context.origin().getZ(), 16) * 16;
        Map<String, List<BlockPos>> floors = new HashMap<>();
        for (int x = minX; x < minX + 16; x++) for (int z = minZ; z < minZ + 16; z++) {
            for (int y = level.getMinBuildHeight() + 1; y < level.getMaxBuildHeight() - 1; y++) {
                BlockPos pos = new BlockPos(x, y, z);
                if (!level.isEmptyBlock(pos)) continue;
                BlockState ground = level.getBlockState(pos.below());
                String biome = ground.is(Blocks.CRIMSON_NYLIUM) ? "crimson_forest"
                        : ground.is(Blocks.WARPED_NYLIUM) ? "warped_forest"
                        : ground.is(Blocks.SOUL_SAND) || ground.is(Blocks.SOUL_SOIL) ? "soul_sand_valley"
                        : ground.is(Blocks.BASALT) || ground.is(Blocks.BLACKSTONE) ? "basalt_deltas" : "";
                if (!biome.isEmpty() && GameplayDimensions.biomeIs(level, pos, biome)) {
                    floors.computeIfAbsent(biome, ignored -> new ArrayList<>()).add(pos);
                }
            }
        }
        boolean placed = placeLavaBed(context, minX, minZ);
        List<BlockPos> valley = floors.getOrDefault("soul_sand_valley", List.of());
        if (!valley.isEmpty()) {
            placed |= NetherCropClusters.placeChunk(level, Math.floorDiv(minX, 16), Math.floorDiv(minZ, 16));
            placed |= placeValley(context, valley);
        }
        for (String biome : List.of("crimson_forest", "warped_forest")) {
            List<BlockPos> sites = floors.getOrDefault(biome, List.of());
            if (!sites.isEmpty() && giantFungusChunk(level.getSeed(), minX / 16, minZ / 16, biome)) {
                String fungus = biome.equals("crimson_forest") ? "crimson" : "warped";
                placed |= placeTree(context, sites, "aoemod:giant_mushrooms/" + fungus + "_fungus_tree_worldgen");
            }
        }
        RandomSource random = context.random();
        for (String biome : List.of("crimson_forest", "warped_forest", "soul_sand_valley")) {
            for (BlockPos pos : floors.getOrDefault(biome, List.of())) {
                String plant = commonPlant(biome, random);
                if (!plant.isEmpty() && level.isEmptyBlock(pos)) {
                    placed |= DocumentedFloraFeature.placePlant(level, pos, plant, random);
                }
            }
        }
        placed |= PowderCanePatches.placeChunk(level, minX / 16, minZ / 16);
        return placed;
    }

    /** Reuse the pre-split basalt lava-bed feature, including its original per-column lottery. */
    private static boolean placeLavaBed(FeaturePlaceContext<NoneFeatureConfiguration> context, int minX, int minZ) {
        var feature = context.level().registryAccess().registryOrThrow(Registries.CONFIGURED_FEATURE)
                .get(new ResourceLocation("aoemod:basalt_deltas/water/water_plants_full"));
        if (feature == null) return false;
        boolean placed = false;
        for (int x = minX; x < minX + 16; x++) for (int z = minZ; z < minZ + 16; z++) {
            for (int y = 1; y < 12; y++) {
                BlockPos pos = new BlockPos(x, y, z);
                if (context.level().getBlockState(pos).is(Blocks.LAVA)
                        && context.level().getBlockState(pos.below()).isFaceSturdy(context.level(), pos.below(), Direction.UP)) {
                    placed |= feature.place(context.level(), context.chunkGenerator(), context.random(), pos);
                }
            }
        }
        return placed;
    }

    /** Copied probabilities from the pre-split AoE forest_floor and grass configured features. */
    public static String commonPlant(String biome, RandomSource random) {
        return switch (biome) {
            case "crimson_forest" -> {
                if (random.nextDouble() >= .125) yield "";
                int species = random.nextInt(100);
                yield species < 96 ? "minecraft:crimson_roots" : "minecraft:crimson_fungus";
            }
            case "warped_forest" -> {
                if (random.nextDouble() >= .30) yield "";
                int species = random.nextInt(100);
                yield species < 35 ? "minecraft:warped_roots" : species < 99 ? "minecraft:nether_sprouts" : "minecraft:warped_fungus";
            }
            case "soul_sand_valley" -> random.nextDouble() < .08 ? "nourished_nether:soul_weeds" : "";
            default -> "";
        };
    }

    /** A Bernoulli roll per biome/chunk; no enforced count in an arbitrary region. */
    public static boolean giantFungusChunk(long seed, int chunkX, int chunkZ, String biome) {
        long hash = TropicalLayout.mix(seed ^ biome.hashCode() ^ (chunkX * 0x632be59bd9b4e019L)
                ^ (chunkZ * 0x9e3779b97f4a7c15L));
        return (hash & 1) == 0;
    }

    private static boolean placeValley(FeaturePlaceContext<NoneFeatureConfiguration> context, List<BlockPos> floors) {
        RandomSource random = context.random();
        int trees = 0;
        for (BlockPos ignored : floors) {
            if (random.nextDouble() < PALE_OAK_PER_COLUMN) trees++;
        }
        boolean placed = false;
        for (int i = 0; i < trees; i++) placed |= placeTree(context, floors, "aoemod:soul_sand_valley/pale_oak");
        placed |= placeEyeblossomPatches(context.level(), context.origin(), random) > 0;
        placed |= SoulCreakingHearts.place(context.level(), random, context.origin());
        return placed;
    }

    /** Count complete patches; a missed floor never retries until it finds land. */
    public static int placeEyeblossomPatches(WorldGenLevel level, BlockPos origin, RandomSource random) {
        int minX = Math.floorDiv(origin.getX(), 16) * 16;
        int minZ = Math.floorDiv(origin.getZ(), 16) * 16;
        int height = level.getMaxBuildHeight() - level.getMinBuildHeight();
        int patches = 0;
        for (int attempt = 0; attempt < EYEBLOSSOM_PATCH_ATTEMPTS; attempt++) {
            int x = minX + random.nextInt(16), z = minZ + random.nextInt(16);
            int y = level.getMinBuildHeight() + random.nextInt(height);
            if (placeEyeblossomPatch(level, new BlockPos(x, y, z), random) == EYEBLOSSOMS_PER_PATCH) patches++;
        }
        return patches;
    }

    public static int placeEyeblossomPatch(WorldGenLevel level, BlockPos anchor, RandomSource random) {
        // Like the vanilla vine feature, the sampled origin itself must be on
        // its biome's floor. Nearby stepped roots cannot rescue a missed origin.
        BlockState support = level.getBlockState(anchor.below());
        if (!level.isEmptyBlock(anchor) || !(support.is(Blocks.SOUL_SAND) || support.is(Blocks.SOUL_SOIL))
                || !GameplayDimensions.biomeIs(level, anchor, "soul_sand_valley")) return 0;
        List<BlockPos> candidates = new ArrayList<>();
        for (int dx = -2; dx <= 3; dx++) for (int dz = -2; dz <= 3; dz++) {
            for (int dy : new int[]{0, 1, -1, 2, -2}) {
                BlockPos pos = anchor.offset(dx, dy, dz);
                BlockState ground = level.getBlockState(pos.below());
                if (level.isEmptyBlock(pos) && (ground.is(Blocks.SOUL_SAND) || ground.is(Blocks.SOUL_SOIL))
                        && GameplayDimensions.biomeIs(level, pos, "soul_sand_valley")) {
                    candidates.add(pos);
                    break;
                }
            }
        }
        shuffle(candidates, random);
        GrowthPlacementBuffer buffer = new GrowthPlacementBuffer(level);
        int planted = 0;
        for (BlockPos pos : candidates) {
            if (DocumentedFloraFeature.placePlant(buffer, pos, "aoemod:closed_eyeblossom", random)
                    && ++planted == EYEBLOSSOMS_PER_PATCH) return buffer.commit() ? planted : 0;
        }
        return 0;
    }

    private static boolean placeTree(FeaturePlaceContext<NoneFeatureConfiguration> context, List<BlockPos> floors, String id) {
        var feature = context.level().registryAccess().registryOrThrow(Registries.CONFIGURED_FEATURE).get(new ResourceLocation(id));
        if (feature == null) return false;
        List<BlockPos> sites = new ArrayList<>(floors);
        shuffle(sites, context.random());
        for (BlockPos pos : sites.subList(0, Math.min(32, sites.size()))) {
            if (context.level().isEmptyBlock(pos) && feature.place(context.level(), context.chunkGenerator(), context.random(), pos)) return true;
        }
        return false;
    }

    private static <T> void shuffle(List<T> values, RandomSource random) {
        for (int i = values.size() - 1; i > 0; i--) {
            int other = random.nextInt(i + 1);
            T value = values.get(i);
            values.set(i, values.get(other));
            values.set(other, value);
        }
    }
}

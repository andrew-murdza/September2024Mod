package net.amurdza.examplemod.worldgen;

import com.google.gson.JsonParser;
import net.amurdza.examplemod.util.GameplayDimensions;
import net.amurdza.examplemod.worldgen.structure.SavannaOrcCampPiece;
import net.amurdza.examplemod.worldgen.structure.SavannaOrcVillage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.FeatureSorter;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.XoroshiroRandomSource;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.BitSet;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.function.Predicate;
import java.util.function.Supplier;

/** Native Savanna vegetation uses the raised ground, while authored gardens and buildings stay intact. */
public final class SavannaCampVegetation {
    private static final ThreadLocal<Predicate<BlockPos>> PLACEMENT = new ThreadLocal<>();

    private SavannaCampVegetation() { }

    private static final class Reservations {
        private static final BitSet[] COLUMNS = load("orc_village_vegetation", SavannaOrcVillage.SIZE);
        private static final BitSet[] V10 = load("orc_village_v10_vegetation", 197);
        private static final BitSet[] V9 = load("orc_village_v9_vegetation", 197);
        private static final BitSet[] V8 = load("orc_village_v8_vegetation", 201);
        private static final BitSet[] V7 = load("orc_village_v7_vegetation", 215);
        private static final BitSet[] V6 = load("orc_village_v6_vegetation", 193);
        private static final BitSet[] LEGACY = load("orc_village_v5_vegetation", 125);

        private static BitSet[] load(String name, int expectedSize) {
            String path = "/data/aoemod/structure_data/" + name + ".json";
            try (var stream = SavannaCampVegetation.class.getResourceAsStream(path)) {
                if (stream == null) throw new IllegalStateException("Missing camp planting mask: " + path);
                var data = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
                int size = data.get("size").getAsInt();
                if (size != expectedSize) throw new IllegalStateException("Camp planting mask size: " + size);
                BitSet[] result = {new BitSet(size * size), new BitSet(size * size), new BitSet(size * size)};
                String[] keys = {"reserved_columns", "tree_reserved_columns", "mud_pond_columns"};
                for (int mask = 0; mask < keys.length; mask++) {
                    if (!data.has(keys[mask])) continue;
                    for (var value : data.getAsJsonArray(keys[mask])) {
                        int index = value.getAsInt();
                        if (index < 0 || index >= size * size) throw new IllegalStateException("Invalid camp planting column: " + index);
                        result[mask].set(index);
                    }
                }
                return result;
            } catch (java.io.IOException exception) {
                throw new IllegalStateException("Cannot read camp planting mask", exception);
            }
        }
    }

    public static boolean reservedLocal(int x, int z, boolean tree) {
        return x >= 0 && z >= 0 && x < SavannaOrcVillage.SIZE && z < SavannaOrcVillage.SIZE
                && Reservations.COLUMNS[tree ? 1 : 0].get(z * SavannaOrcVillage.SIZE + x);
    }

    public static boolean reserved(SavannaOrcCampPiece piece, BlockPos pos, boolean tree) {
        int x = pos.getX() - piece.templatePosition().getX(), z = pos.getZ() - piece.templatePosition().getZ();
        if (piece.isDefendedVillage()) return reservedLocal(x, z, tree);
        if (piece.isShallowGateVillage()) return x >= 0 && z >= 0 && x < 197 && z < 197
                && Reservations.V10[tree ? 1 : 0].get(z * 197 + x);
        if (piece.isRevisedSquareVillage()) return x >= 0 && z >= 0 && x < 197 && z < 197
                && Reservations.V9[tree ? 1 : 0].get(z * 197 + x);
        if (piece.isCompactSquareVillage()) return x >= 0 && z >= 0 && x < 201 && z < 201
                && Reservations.V8[tree ? 1 : 0].get(z * 201 + x);
        if (piece.isExpandedSquareVillage()) return x >= 0 && z >= 0 && x < 215 && z < 215
                && Reservations.V7[tree ? 1 : 0].get(z * 215 + x);
        if (piece.isSquareVillage()) return x >= 0 && z >= 0 && x < 193 && z < 193
                && Reservations.V6[tree ? 1 : 0].get(z * 193 + x);
        return piece.isCircularVillage() && x >= 0 && z >= 0 && x < 125 && z < 125
                && Reservations.LEGACY[tree ? 1 : 0].get(z * 125 + x);
    }

    public static boolean isLegacyV10MudPond(int x, int z) {
        return x >= 0 && z >= 0 && x < 197 && z < 197 && Reservations.V10[2].get(z * 197 + x);
    }

    public static boolean isLegacyV7MudPond(int x, int z) {
        return x >= 0 && z >= 0 && x < 215 && z < 215 && Reservations.V7[2].get(z * 215 + x);
    }

    public static boolean isLegacyV8MudPond(int x, int z) {
        return x >= 0 && z >= 0 && x < 201 && z < 201 && Reservations.V8[2].get(z * 201 + x);
    }

    public static boolean isLegacyV6MudPond(int x, int z) {
        return x >= 0 && z >= 0 && x < 193 && z < 193 && Reservations.V6[2].get(z * 193 + x);
    }

    public static boolean isMudPond(int x, int z) {
        return x >= 0 && z >= 0 && x < SavannaOrcVillage.SIZE && z < SavannaOrcVillage.SIZE
                && Reservations.COLUMNS[2].get(z * SavannaOrcVillage.SIZE + x);
    }

    /** No remote chunk loads: FEATURES can use available start owners; SPAWN uses the persisted mask. */
    public static boolean reserved(WorldGenLevel level, BlockPos pos, boolean tree) {
        while (level instanceof GrowthPlacementBuffer buffer) level = buffer.sourceLevel();
        if (!GameplayDimensions.is(level, GameplayDimensions.SAVANNA)) return false;
        var chunk = level instanceof WorldGenRegion region
                ? region.getChunk(pos.getX() >> 4, pos.getZ() >> 4, ChunkStatus.STRUCTURE_REFERENCES, false)
                : level.getChunk(pos);
        if (chunk == null) return false;
        if (SavannaCampColumns.vegetationRecorded(chunk)) return SavannaCampColumns.reservesVegetation(chunk, pos, tree);
        var camp = level.registryAccess().registryOrThrow(Registries.STRUCTURE).get(new ResourceLocation("aoemod:orc_camp"));
        if (camp == null) return false;
        if (level instanceof WorldGenRegion region) {
            for (long reference : chunk.getReferencesForStructure(camp)) {
                var owner = new ChunkPos(reference);
                var startChunk = region.getChunk(owner.x, owner.z, ChunkStatus.STRUCTURE_STARTS, false);
                if (startChunk == null) continue;
                var start = startChunk.getStartForStructure(camp);
                if (start != null && start.isValid()) for (var piece : start.getPieces())
                    if (piece instanceof SavannaOrcCampPiece village && reserved(village, pos, tree)) return true;
            }
        } else {
            for (var start : level.getLevel().structureManager().startsForStructure(new ChunkPos(pos), s -> s == camp))
                for (var piece : start.getPieces())
                    if (piece instanceof SavannaOrcCampPiece village && reserved(village, pos, tree)) return true;
        }
        return false;
    }

    /** Only feature placement is guarded; template writes and later player edits retain their normal rules. */
    public static boolean isVegetationFeature(WorldGenLevel level, PlacedFeature feature) {
        if (!GameplayDimensions.is(level, GameplayDimensions.SAVANNA)) return false;
        var biome = level.registryAccess().registryOrThrow(Registries.BIOME).get(new ResourceLocation("aoemod:savanna"));
        int step = GenerationStep.Decoration.VEGETAL_DECORATION.ordinal();
        return biome != null && biome.getGenerationSettings().features().size() > step
                && biome.getGenerationSettings().features().get(step).stream().anyMatch(holder -> holder.value() == feature);
    }

    public static <T> T natural(WorldGenLevel level, Supplier<T> action) {
        if (!GameplayDimensions.is(level, GameplayDimensions.SAVANNA) || PLACEMENT.get() != null) return action.get();
        PLACEMENT.set(pos -> !reserved(level, pos, false));
        try { return action.get(); }
        finally { PLACEMENT.remove(); }
    }

    public static boolean allows(WorldGenLevel level, BlockPos pos) {
        Predicate<BlockPos> allowed = PLACEMENT.get();
        return allowed == null || !GameplayDimensions.is(level, GameplayDimensions.SAVANNA) || allowed.test(pos);
    }

    /** Reject a complete tree if any trunk, root, branch, or leaf would enter a pen or authored column. */
    public static boolean allowsTree(WorldGenLevel level, BlockPos pos) {
        return PLACEMENT.get() == null || !GameplayDimensions.is(level, GameplayDimensions.SAVANNA)
                || allows(level, pos) && !reserved(level, pos, true);
    }

    /** CARVERS and later stages no longer update the two generation-only surface snapshots. */
    public static void prepareMoundChunk(WorldGenLevel level, ChunkPos chunk) {
        Heightmap.primeHeightmaps(level.getChunk(chunk.x, chunk.z),
                EnumSet.of(Heightmap.Types.WORLD_SURFACE_WG, Heightmap.Types.OCEAN_FLOOR_WG));
    }

    public static void prepareDecorationChunk(ServerLevel level, ChunkPos chunk) {
        prepareMoundChunk(level, chunk);
    }

    /** /place runs after normal chunk decoration, so replay that same feature list after the entire mound exists. */
    public static void decoratePlaced(ServerLevel level, SavannaOrcCampPiece piece) {
        if (!GameplayDimensions.is(level, GameplayDimensions.SAVANNA) || !piece.usesNativeVegetation()) return;
        var generator = level.getChunkSource().getGenerator();
        int step = GenerationStep.Decoration.VEGETAL_DECORATION.ordinal();
        var sorted = FeatureSorter.buildFeaturesPerStep(List.copyOf(generator.getBiomeSource().possibleBiomes()),
                biome -> generator.getBiomeGenerationSettings(biome).features(), true);
        if (sorted.size() <= step) return;
        var features = sorted.get(step);
        var biome = level.registryAccess().registryOrThrow(Registries.BIOME).getHolderOrThrow(
                net.minecraft.resources.ResourceKey.create(Registries.BIOME, new ResourceLocation("aoemod:savanna")));
        var settings = generator.getBiomeGenerationSettings(biome).features();
        if (settings.size() <= step) return;
        int[] indexes = settings.get(step).stream().mapToInt(holder -> features.indexMapping().applyAsInt(holder.value()))
                .distinct().sorted().toArray();
        var bounds = piece.getBoundingBox();
        var chunks = new ArrayList<ChunkPos>();
        for (int cx = bounds.minX() >> 4; cx <= bounds.maxX() >> 4; cx++) {
            for (int cz = bounds.minZ() >> 4; cz <= bounds.maxZ() >> 4; cz++) {
                BlockPos origin = new BlockPos(cx * 16, level.getMinBuildHeight(), cz * 16);
                boolean affected = false;
                for (int x = 0; x < 16 && !affected; x++) for (int z = 0; z < 16 && !affected; z++) {
                    affected = piece.hasMoundColumn(origin.offset(x, 0, z));
                }
                if (affected) {
                    var chunk = new ChunkPos(cx, cz);
                    chunks.add(chunk);
                    // Refresh every neighbor before any feature can sample across
                    // a chunk edge, then keep the same pre-tree snapshot as nature.
                    prepareDecorationChunk(level, chunk);
                }
            }
        }
        var random = new WorldgenRandom(new XoroshiroRandomSource(0));
        Predicate<BlockPos> previous = PLACEMENT.get();
        PLACEMENT.set(pos -> piece.hasMoundColumn(pos) && !reserved(level, pos, false));
        try {
            for (var chunk : chunks) {
                BlockPos origin = chunk.getWorldPosition().atY(level.getMinBuildHeight());
                long seed = random.setDecorationSeed(level.getSeed(), origin.getX(), origin.getZ());
                for (int index : indexes) {
                    random.setFeatureSeed(seed, index, step);
                    features.features().get(index).placeWithBiomeCheck(level, generator, random, origin);
                }
                SavannaCampTrees.placeChunk(level, chunk, piece);
            }
        } finally {
            if (previous == null) PLACEMENT.remove(); else PLACEMENT.set(previous);
        }
    }
}

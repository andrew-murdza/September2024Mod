package net.amurdza.examplemod.worldgen.structure;

import net.amurdza.examplemod.util.GameplayDimensions;
import net.amurdza.examplemod.worldgen.CropTerrainHeights;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;
import net.minecraft.world.level.levelgen.structure.structures.WoodlandMansionPieces.WoodlandMansionPiece;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.WeakHashMap;

/** A one-to-one earth ramp, independent of vanilla's eight-chunk structure references. */
public final class MansionRampTerrain {
    // Eleven native eight-block room cells plus roof/entrance projections fit in
    // this half-width. This is a candidate-search bound, never a grading radius.
    private static final int MAX_HOUSE_RADIUS = 64;
    private static final int FLAGS = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;
    private static final Map<ServerLevel, Map<Key, Optional<Site>>> CACHE = new WeakHashMap<>();

    private MansionRampTerrain() { }

    public record Site(ChunkPos owner, BoundingBox house, int groundY, int layoutVersion) {
        public Site(ChunkPos owner, BoundingBox house, int groundY) { this(owner, house, groundY, 8); }
        public MansionEstateShape shape() { return new MansionEstateShape(house, layoutVersion); }
        public double centerX() { return (house.minX() + house.maxX()) / 2.0; }
        public double centerZ() { return (house.minZ() + house.maxZ()) / 2.0; }
        public double flatRadius() {
            return shape().halfWidth();
        }
        public double distanceBeyondFlat(int x, int z) {
            return shape().distanceBeyondFlat(x, z);
        }
        public boolean reaches(ChunkPos chunk, int minY) {
            double x = Math.max(chunk.getMinBlockX(), Math.min(centerX(), chunk.getMaxBlockX()));
            double z = Math.max(chunk.getMinBlockZ(), Math.min(centerZ(), chunk.getMaxBlockZ()));
            return (shape().square() ? Math.max(Math.abs(x - centerX()), Math.abs(z - centerZ()))
                    : Math.hypot(x - centerX(), z - centerZ())) < flatRadius() + groundY - minY;
        }
    }

    private record Key(Structure structure, ChunkPos owner) { }

    public static int targetHeight(int groundY, double distanceBeyondFlat, int naturalY) {
        return Math.max(naturalY, groundY - (int) Math.floor(Math.max(0, distanceBeyondFlat)));
    }

    /** Called before biome decoration, so plants use the completed ramp's heightmaps. */
    public static void prepare(WorldGenRegion region, ChunkAccess chunk, ChunkGenerator generator) {
        if (!GameplayDimensions.is(region, GameplayDimensions.PLAINS)) return;
        var pos = chunk.getPos();
        var clip = new BoundingBox(pos.getMinBlockX(), region.getMinBuildHeight(), pos.getMinBlockZ(),
                pos.getMaxBlockX(), region.getMaxBuildHeight() - 1, pos.getMaxBlockZ());
        for (var site : sitesForChunk(region, pos, generator)) {
            place(region, clip, site);
            if (site.shape().widened()) MansionGrounds.placeTrees(region, clip, site.house, site.groundY, site.layoutVersion);
        }
    }

    /** Seeded planning reads noise and templates only, never chunks outside the supplied region. */
    public static List<Site> sitesForChunk(WorldGenRegion region, ChunkPos target, ChunkGenerator generator) {
        return sitesForChunk(region.getLevel(), target, generator,
                (x, z) -> region.getChunk(x, z, ChunkStatus.STRUCTURE_STARTS, false));
    }

    public static List<Site> sitesForChunk(ServerLevel level, ChunkPos target, ChunkGenerator generator) {
        return sitesForChunk(level, target, generator, (x, z) -> level.getChunkSource().getChunkNow(x, z));
    }

    private static List<Site> sitesForChunk(ServerLevel level, ChunkPos target, ChunkGenerator generator,
                                           java.util.function.BiFunction<Integer, Integer, ChunkAccess> availableOwner) {
        var state = level.getChunkSource().getGeneratorState();
        // A cone cannot affect a column once it falls below the world's minimum
        // height. Add one chunk for the candidate's center offset within its chunk.
        int range = (MAX_HOUSE_RADIUS + MansionEstateShape.WIDE_MARGIN + level.getHeight() + 15) / 16 + 1;
        var result = new ArrayList<Site>();
        for (var holder : state.possibleStructureSets()) {
            var set = holder.value();
            // The shipped mansion set has exactly one entry. Do not create
            // phantom estates if a datapack combines it with competing structures.
            if (set.structures().size() != 1 || !(set.structures().get(0).structure().value() instanceof IllagerMansionStructure mansion)
                    || !(set.placement() instanceof RandomSpreadStructurePlacement placement)) continue;
            int spacing = placement.spacing();
            for (int gx = Math.floorDiv(target.x - range, spacing); gx <= Math.floorDiv(target.x + range, spacing); gx++) {
                for (int gz = Math.floorDiv(target.z - range, spacing); gz <= Math.floorDiv(target.z + range, spacing); gz++) {
                    var candidate = placement.getPotentialStructureChunk(state.getLevelSeed(), gx * spacing, gz * spacing);
                    if (Math.abs(candidate.x - target.x) > range || Math.abs(candidate.z - target.z) > range
                            || !placement.isStructureChunk(state, candidate.x, candidate.z)) continue;
                    double dx = Math.max(0, Math.abs(candidate.getBlockX(7) - target.getMiddleBlockX()) - 8);
                    double dz = Math.max(0, Math.abs(candidate.getBlockZ(7) - target.getMiddleBlockZ()) - 8);
                    if (Math.max(dx, dz) >= MAX_HOUSE_RADIUS + MansionEstateShape.WIDE_MARGIN + level.getHeight()) continue;
                    var owner = availableOwner.apply(candidate.x, candidate.z);
                    Optional<Site> site;
                    if (owner != null) {
                        // Loaded saved starts are authoritative, including old
                        // layouts whose original twelve-block ramps must survive.
                        site = fromStart(owner.getStartForStructure(mansion));
                    } else {
                        site = predict(level, generator, mansion, candidate);
                    }
                    site.filter(value -> value.reaches(target, level.getMinBuildHeight())).ifPresent(result::add);
                }
            }
        }
        return result;
    }

    private static Optional<Site> predict(ServerLevel level, ChunkGenerator generator, IllagerMansionStructure mansion, ChunkPos candidate) {
        Map<Key, Optional<Site>> cache;
        synchronized (CACHE) {
            cache = CACHE.computeIfAbsent(level, unused -> new LinkedHashMap<>(128, 0.75F, true) {
                @Override protected boolean removeEldestEntry(Map.Entry<Key, Optional<Site>> entry) { return size() > 128; }
            });
        }
        synchronized (cache) {
            return cache.computeIfAbsent(new Key(mansion, candidate), unused -> fromStart(mansion.generate(
                    level.registryAccess(), generator, generator.getBiomeSource(), level.getChunkSource().randomState(),
                    level.getStructureManager(), level.getSeed(), candidate, 0, level, mansion.biomes()::contains)));
        }
    }

    public static Optional<Site> fromStart(StructureStart start) {
        if (start == null || !start.isValid() || !(start.getStructure() instanceof IllagerMansionStructure)) return Optional.empty();
        var basement = start.getPieces().stream().filter(MansionBasementPiece.class::isInstance)
                .map(MansionBasementPiece.class::cast).findFirst().orElse(null);
        if (basement == null || !basement.gentleRamp()) return Optional.empty();
        return BoundingBox.encapsulatingBoxes(start.getPieces().stream().filter(WoodlandMansionPiece.class::isInstance)
                .map(piece -> piece.getBoundingBox()).toList()).map(house -> new Site(start.getChunkPos(), house, basement.groundsY(), basement.layoutVersion()));
    }

    /** All reads and writes stay in this horizontal and vertical clip. */
    public static void place(WorldGenLevel level, BoundingBox clip, Site site) {
        var cursor = new BlockPos.MutableBlockPos();
        var map = level instanceof ServerLevel ? Heightmap.Types.WORLD_SURFACE : Heightmap.Types.WORLD_SURFACE_WG;
        int minY = Math.max(level.getMinBuildHeight(), clip.minY());
        for (int x = clip.minX(); x <= clip.maxX(); x++) for (int z = clip.minZ(); z <= clip.maxZ(); z++) {
            double distance = site.distanceBeyondFlat(x, z);
            boolean flat = distance <= 0;
            // Native afterPlace owns the house, cellar and referenced core.
            // The new 128-block forest also needs its outer, unreferenced chunks.
            boolean core = Math.abs((x >> 4) - site.owner.x) <= 8 && Math.abs((z >> 4) - site.owner.z) <= 8
                    && x >= site.house.minX() - MansionGrounds.OUTER_MARGIN && x <= site.house.maxX() + MansionGrounds.OUTER_MARGIN
                    && z >= site.house.minZ() - MansionGrounds.OUTER_MARGIN && z <= site.house.maxZ() + MansionGrounds.OUTER_MARGIN;
            if (flat && (!site.shape().widened() || core) || site.groundY - (int) Math.floor(Math.max(0, distance)) <= minY) continue;
            int top = Math.min(level.getHeight(map, x, z) - 1, clip.maxY());
            int natural = top;
            int waterTop = minY - 1;
            while (natural > minY) {
                var block = level.getBlockState(cursor.set(x, natural, z));
                if (block.getFluidState().is(FluidTags.WATER)) waterTop = Math.max(waterTop, natural);
                if (block.blocksMotion() && !block.is(BlockTags.LEAVES) && !block.is(BlockTags.LOGS)) break;
                natural--;
            }
            int target = Math.min(clip.maxY(), flat ? site.groundY : targetHeight(site.groundY, distance, natural));
            if (target == natural && level.getBlockState(cursor.set(x, natural, z)).is(Blocks.GRASS_BLOCK)
                    || !flat && target <= natural) continue;
            for (int y = top; y > target; y--) level.setBlock(cursor.set(x, y, z), Blocks.AIR.defaultBlockState(), FLAGS);
            for (int y = Math.max(minY, Math.min(natural, target - 3)); y <= target; y++) {
                var block = y == target ? Blocks.GRASS_BLOCK : y >= target - 3 ? Blocks.DIRT : Blocks.STONE;
                level.setBlock(cursor.set(x, y, z), block.defaultBlockState(), FLAGS);
            }
            for (int y = target + 1; !flat && y <= waterTop; y++)
                level.setBlock(cursor.set(x, y, z), Blocks.WATER.defaultBlockState(), FLAGS);
        }
    }

    /** /place does not create structure references; complete its ramp from the actual start. */
    public static void decoratePlaced(ServerLevel level, StructureStart start) {
        fromStart(start).ifPresent(site -> {
            var generator = level.getChunkSource().getGenerator();
            var heights = generator instanceof NoiseBasedChunkGenerator noise
                    ? new CropTerrainHeights(level, noise, level.getChunkSource().randomState()) : null;
            double radius = site.flatRadius() + site.groundY - level.getMinBuildHeight();
            int minX = (int) Math.floor(site.centerX() - radius) >> 4, maxX = (int) Math.ceil(site.centerX() + radius) >> 4;
            int minZ = (int) Math.floor(site.centerZ() - radius) >> 4, maxZ = (int) Math.ceil(site.centerZ() + radius) >> 4;
            for (int cx = minX; cx <= maxX; cx++) for (int cz = minZ; cz <= maxZ; cz++) {
                var pos = new ChunkPos(cx, cz);
                if (!site.reaches(pos, level.getMinBuildHeight())) continue;
                // A very conservative world-height horizon may contain hundreds
                // of unaffected chunks. Sample their ungenerated noise instead
                // of loading every chunk just to discover the ramp is below it.
                if (level.getChunkSource().getChunkNow(cx, cz) == null && heights != null) {
                    boolean needed = false;
                    for (int x = pos.getMinBlockX(); x <= pos.getMaxBlockX() && !needed; x++) {
                        for (int z = pos.getMinBlockZ(); z <= pos.getMaxBlockZ(); z++) {
                            double distance = site.distanceBeyondFlat(x, z);
                            if (distance <= 0 && site.shape().widened()
                                    || distance > 0 && targetHeight(site.groundY, distance, heights.height(x, z) - 1) >= heights.height(x, z)) {
                                needed = true;
                                break;
                            }
                        }
                    }
                    if (!needed) continue;
                }
                var clip = new BoundingBox(pos.getMinBlockX(), level.getMinBuildHeight(), pos.getMinBlockZ(),
                        pos.getMaxBlockX(), level.getMaxBuildHeight() - 1, pos.getMaxBlockZ());
                place(level, clip, site);
                if (site.shape().widened()) {
                    MansionGrounds.placeTrees(level, clip, site.house, site.groundY, site.layoutVersion);
                    MansionGroundCover.place(level, clip, site);
                }
            }
        });
    }
}

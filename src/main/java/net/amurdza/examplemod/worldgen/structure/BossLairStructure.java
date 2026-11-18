package net.amurdza.examplemod.worldgen.structure;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.amurdza.examplemod.registry.ModStructures;
import net.amurdza.examplemod.worldgen.DimensionTerrain;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.pieces.PiecesContainer;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;
import twilightforest.world.components.structures.YetiCaveComponent;

import java.util.Optional;

/** Boss arenas are structures, so terrain and ordinary surface decoration surround them naturally. */
public final class BossLairStructure extends Structure {
    // The camp floor is highest terrain + 4 and its existing foundation fills
    // 14 blocks below that floor. Ten blocks of relief therefore still join
    // every dry terrain column, and fit inside the 20-block entrance apron.
    private static final int MAX_ORC_TERRAIN_RELIEF = 14 - 4;
    public static final Codec<BossLairStructure> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            settingsCodec(instance), Codec.STRING.fieldOf("kind").forGetter(value -> value.kind)
    ).apply(instance, BossLairStructure::new));
    private final String kind;
    public static final int YETI_PLATEAU_RADIUS = 24;
    public static final int YETI_MAX_SHOULDER_DROP = 2;

    public BossLairStructure(StructureSettings settings, String kind) {
        super(settings);
        if (!java.util.Set.of("spider", "yeti", "wither", "orc").contains(kind)) {
            throw new IllegalArgumentException("Unknown boss lair: " + kind);
        }
        this.kind = kind;
    }

    @Override
    public Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
        int x = context.chunkPos().getMiddleBlockX(), z = context.chunkPos().getMiddleBlockZ();
        if (kind.equals("spider")) {
            var island = DimensionTerrain.tropicalLayout(context.randomState().router().temperature()).nearest(x, z);
            var cave = IslandBossPlacement.center(island);
            if (!new ChunkPos(cave).equals(context.chunkPos())) return Optional.empty();
            x = cave.getX();
            z = cave.getZ();
        }
        if (kind.equals("yeti")) {
            var plateau = PlateauLairPlacement.center(context.randomState(), context.chunkPos());
            if (plateau == null) return Optional.empty();
            x = plateau.getX();
            z = plateau.getZ();
        }
        int y = kind.equals("yeti")
                ? net.amurdza.examplemod.worldgen.SnowyOceanFloor.floor(context.randomState(), x, z)
                : context.chunkGenerator().getBaseHeight(x, z, Heightmap.Types.WORLD_SURFACE_WG,
                        context.heightAccessor(), context.randomState()) - 1;
        if (kind.equals("orc")) {
            var heights = new java.util.HashMap<Long, Integer>();
            // The placement chooses a high divide before selecting the owner
            // chunk. Do not shift a255-wide structure beyond vanilla's eight-
            // chunk reference window while searching from an unrelated start.
            return findOrcSite(context, x, z, heights).map(ground -> new GenerationStub(ground,
                    builder -> builder.addPiece(new SavannaOrcCampPiece(context.structureTemplateManager(), ground))));
        }
        if (kind.equals("wither")) {
            BlockPos site = null;
            for (int[] offset : nearbyOffsets(80)) {
                var candidate = findWitherSite(context, x + offset[0], z + offset[1]);
                if (candidate.isPresent()) { site = candidate.orElseThrow(); break; }
            }
            if (site == null) return Optional.empty();
            x = site.getX(); y = site.getY(); z = site.getZ();
        } else if (y < context.chunkGenerator().getSeaLevel() || y + 30 >= context.heightAccessor().getMaxBuildHeight()) {
            return Optional.empty();
        }
        long randomSeed = context.random().nextLong();
        if (kind.equals("spider")) {
            // Orient the diagonal approach roughly tangentially so its mouth stays in rainforest.
            var island = DimensionTerrain.tropicalLayout(context.randomState().router().temperature()).nearest(x, z);
            int sx = (randomSeed & 1) == 0 ? 1 : -1;
            int sz = (x - island.x()) * (z - island.z()) > 0 ? -sx : sx;
            randomSeed = (randomSeed & ~2L) | (sz < 0 ? 2L : 0L);
            // The rainforest slopes down from its center. Anchor the horizontal
            // mouth to the real outside approach, not the taller island summit.
            y = context.chunkGenerator().getBaseHeight(x + 58 * sx, z + 58 * sz,
                    Heightmap.Types.WORLD_SURFACE_WG, context.heightAccessor(), context.randomState()) - 1;
        }
        long seed = randomSeed;
        BlockPos center = new BlockPos(x, y, z);
        return Optional.of(new GenerationStub(center, builder -> builder.addPiece(new BossLairPiece(kind, center, seed))));
    }

    private static java.util.List<int[]> nearbyOffsets(int radius) {
        java.util.List<int[]> offsets = new java.util.ArrayList<>();
        for (int dx = -radius; dx <= radius; dx += 16) {
            for (int dz = -radius; dz <= radius; dz += 16) offsets.add(new int[]{dx, dz});
        }
        offsets.sort(java.util.Comparator.comparingInt(a -> a[0] * a[0] + a[1] * a[1]));
        return offsets;
    }

    private static Optional<BlockPos> findOrcSite(GenerationContext context, int x, int z, java.util.Map<Long, Integer> heights) {
        int lowest = Integer.MAX_VALUE, highest = Integer.MIN_VALUE;
        // Include every side and corner of the complete apron, not only the
        // central buildings, so the larger mound cannot extend into a river.
        int extent = SavannaOrcVillage.CENTER + SavannaOrcVillage.MARGIN;
        for (int dx = -extent; dx <= extent; dx += 4) {
            for (int dz = -extent; dz <= extent; dz += 4) {
                int sx = x + dx, sz = z + dz;
                long key = ((long) sx << 32) ^ (sz & 0xffffffffL);
                int height = heights.computeIfAbsent(key, ignored -> context.chunkGenerator().getBaseHeight(sx, sz,
                        Heightmap.Types.WORLD_SURFACE_WG, context.heightAccessor(), context.randomState()) - 1);
                if (height < context.chunkGenerator().getSeaLevel()
                        || !context.biomeSource().getNoiseBiome((x + dx) >> 2, height >> 2, (z + dz) >> 2,
                        context.randomState().sampler()).is(new net.minecraft.resources.ResourceLocation("aoemod", "savanna"))) {
                    return Optional.empty();
                }
                lowest = Math.min(lowest, height);
                highest = Math.max(highest, height);
                if (highest - lowest > MAX_ORC_TERRAIN_RELIEF) return Optional.empty();
            }
        }
        return highest + 20 >= context.heightAccessor().getMaxBuildHeight() ? Optional.empty()
                : Optional.of(new BlockPos(x, highest + 2, z));
    }

    private static Optional<BlockPos> findWitherSite(GenerationContext context, int x, int z) {
        if (!context.biomeSource().getNoiseBiome(x >> 2, 2, z >> 2, context.randomState().sampler())
                .is(new net.minecraft.resources.ResourceLocation("aoemod", "basalt_deltas"))) return Optional.empty();
        var terrain = net.amurdza.examplemod.worldgen.NetherRampDecorationGuard.terrain(context.randomState()).orElse(null);
        if (terrain != null) {
            var columnContext = new DensityFunction.SinglePointContext(x, 0, z);
            double corridor = DimensionTerrain.netherRampDistance(z, terrain.rampDetail().compute(columnContext));
            double rampFloor = DimensionTerrain.netherRampFloor(x, terrain.rampBasin().compute(columnContext));
            if (corridor < 51 || corridor < DimensionTerrain.STAIR_ENTRANCE_REACH + 39
                    && Math.abs(rampFloor - 18) < 22) return Optional.empty();
        }
        var column = context.chunkGenerator().getBaseColumn(x, z, context.heightAccessor(), context.randomState());
        for (int y = 16; y > context.heightAccessor().getMinBuildHeight() + 3; y--) {
            if (column.getBlock(y).isAir() || !column.getBlock(y).getFluidState().isEmpty()) continue;
            boolean clear = true;
            for (int dy = 1; dy <= 14; dy++) if (!column.getBlock(y + dy).isAir()) { clear = false; break; }
            if (!clear) continue;
            for (int dx : new int[]{-3, 0, 3}) {
                var entrance = context.chunkGenerator().getBaseColumn(x + dx, z + 40,
                        context.heightAccessor(), context.randomState());
                boolean walkingFloor = false;
                for (int floor = y - 1; floor <= y + 1; floor++) {
                    if (!entrance.getBlock(floor).isAir() && entrance.getBlock(floor).getFluidState().isEmpty()
                            && entrance.getBlock(floor + 1).isAir() && entrance.getBlock(floor + 2).isAir()) walkingFloor = true;
                }
                if (!walkingFloor) { clear = false; break; }
            }
            if (clear) return Optional.of(new BlockPos(x, y, z));
        }
        return Optional.empty();
    }

    @Override public StructureType<?> type() { return ModStructures.BOSS_LAIR.get(); }

    @Override
    public void afterPlace(WorldGenLevel level, StructureManager structures, ChunkGenerator generator, RandomSource random,
                           BoundingBox chunk, ChunkPos chunkPos, PiecesContainer pieces) {
        if (!kind.equals("yeti") || pieces.pieces().stream().noneMatch(YetiCaveComponent.class::isInstance)) return;
        for (var piece : pieces.pieces()) {
            if (piece instanceof BossLairPiece envelope) envelope.finishYetiInterior(level, chunk);
        }
    }
}

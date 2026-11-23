package net.amurdza.examplemod.worldgen.structure;

import net.amurdza.examplemod.registry.ModStructures;
import net.amurdza.examplemod.util.StructureSpawnedMobData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.monster.MagmaCube;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraftforge.registries.ForgeRegistries;
import twilightforest.init.TFBlocks;

/** Chunk-clipped geometry and persistent, one-time residents; never writes a neighboring chunk. */
public final class BossLairPiece extends StructurePiece {
    // The enlarged box contains the longer underground branches, not a hollow dome.
    public static final int SPIDER_RADIUS = 64;
    public static final int SPIDER_HEIGHT = 16;
    private final String kind;
    private final BlockPos center;
    private final long seed;
    private int spawned;
    private final boolean expandedSpider;
    private final java.util.BitSet spiderResidents = new java.util.BitSet();
    private final BranchedBossCaves branches;

    public BossLairPiece(String kind, BlockPos center, long seed) {
        super(ModStructures.BOSS_LAIR_PIECE.get(), 0, bounds(kind, center));
        this.kind = kind;
        this.center = center;
        this.seed = seed;
        expandedSpider = kind.equals("spider");
        branches = kind.equals("spider") || kind.equals("wither") ? new BranchedBossCaves(kind, center.getY(), seed) : null;
    }

    public BossLairPiece(StructurePieceSerializationContext context, CompoundTag tag) {
        super(ModStructures.BOSS_LAIR_PIECE.get(), tag);
        kind = tag.getString("LairKind");
        center = BlockPos.of(tag.getLong("LairCenter"));
        seed = tag.getLong("LairSeed");
        spawned = tag.getInt("ResidentsPlaced");
        // Unversioned starts keep their original geometry and seven resident flags.
        expandedSpider = kind.equals("spider") && tag.getInt("SpiderCaveVersion") >= 1;
        spiderResidents.or(java.util.BitSet.valueOf(tag.getLongArray("SpiderResidents")));
        branches = kind.equals("spider") || kind.equals("wither")
                ? new BranchedBossCaves(kind, center.getY(), seed, expandedSpider) : null;
        boundingBox = bounds(kind, center);
    }

    private static int radius(String kind) {
        return switch (kind) { case "yeti" -> 44; case "wither" -> 18; case "orc" -> 9; default -> SPIDER_RADIUS; };
    }

    private static BoundingBox bounds(String kind, BlockPos center) {
        if (kind.equals("wither")) return new BoundingBox(center.getX() - 25, Math.max(1, center.getY() - 6), center.getZ() - 38,
                center.getX() + 25, center.getY() + 18, center.getZ() + 39);
        int radius = radius(kind);
        return new BoundingBox(center.getX() - radius, center.getY() - (kind.equals("spider") ? 37 : 5), center.getZ() - radius,
                center.getX() + radius, center.getY() + 28, center.getZ() + radius);
    }

    @Override protected synchronized void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
        tag.putString("LairKind", kind);
        tag.putLong("LairCenter", center.asLong());
        tag.putLong("LairSeed", seed);
        tag.putInt("ResidentsPlaced", spawned);
        if (expandedSpider) {
            tag.putInt("SpiderCaveVersion", 1);
            tag.putLongArray("SpiderResidents", spiderResidents.toLongArray());
        }
    }

    public static int spiderRoof(int dx, int dz) {
        double distance = Math.hypot(dx - 30, dz - 30);
        int roof = distance > 34 ? -1 : (int) Math.floor(SPIDER_HEIGHT * (1 - Math.pow(distance / 34, 4)));
        // The rounded tunnel cap extends beyond its center at (54,54).
        // Carry the surround past both terminal wall columns at (59,54)
        // and (54,59), so neither remains separated from the hill by a slit.
        if (dx + dz >= 88 && dx + dz <= 114 && Math.abs(dx - dz) <= 10) roof = Math.max(roof, 8);
        return roof;
    }

    public static boolean spiderEntrance(int dx, int dz, int dy, long seed) {
        int sx = (seed & 1) == 0 ? 1 : -1, sz = (seed & 2) == 0 ? 1 : -1;
        return dy > 0 && dy <= 6 && dx * sx + dz * sz >= 104
                && Math.abs(dx * sx - dz * sz) <= 5;
    }

    /** Open entrance columns still belong to the arena, even where they can see the sky. */
    public boolean containsSpiderInterior(BlockPos pos) {
        if (!kind.equals("spider")) return false;
        int dx = pos.getX() - center.getX(), dz = pos.getZ() - center.getZ(), dy = pos.getY() - center.getY();
        var branch = branches.column(dx, dz);
        if (branch != null && branch.hasAir() && dy > branch.floor() && dy <= branch.ceiling()) return true;
        return false;
    }

    public static boolean isInsideSpiderCave(WorldGenLevel level, BlockPos pos) {
        var structure = level.registryAccess().registryOrThrow(Registries.STRUCTURE)
                .get(new ResourceLocation("aoemod", "spider_cave"));
        if (structure == null) return false;
        // Read the region's already available starts, not ServerLevel's manager:
        // loading feature-stage neighbors through the server could deadlock worldgen.
        var current = level.getChunk(pos.getX() >> 4, pos.getZ() >> 4, ChunkStatus.STRUCTURE_REFERENCES, false);
        if (current == null) return false;
        for (long reference : current.getReferencesForStructure(structure)) {
            var chunk = new ChunkPos(reference);
            var owner = level.getChunk(chunk.x, chunk.z, ChunkStatus.STRUCTURE_STARTS, false);
            if (owner == null) continue;
            var start = owner.getStartForStructure(structure);
            if (start == null || !start.isValid()) continue;
            for (var piece : start.getPieces()) {
                if (piece instanceof BossLairPiece cave && cave.containsSpiderInterior(pos)) return true;
            }
        }
        return false;
    }

    @Override
    public void postProcess(WorldGenLevel level, StructureManager structures, ChunkGenerator generator,
                            RandomSource random, BoundingBox chunk, ChunkPos chunkPos, BlockPos pivot) {
        registerBoundary(level.getLevel());
        for (int x = Math.max(chunk.minX(), boundingBox.minX()); x <= Math.min(chunk.maxX(), boundingBox.maxX()); x++) {
            for (int z = Math.max(chunk.minZ(), boundingBox.minZ()); z <= Math.min(chunk.maxZ(), boundingBox.maxZ()); z++) {
                int dx = x - center.getX(), dz = z - center.getZ();
                if (kind.equals("yeti")) placeYetiColumn(level, chunk, dx, dz);
                else if (kind.equals("orc")) placeOrcColumn(level, chunk, dx, dz);
                else if (kind.equals("spider")) placeSpiderHillColumn(level, chunk, dx, dz);
                if (branches != null) placeBranchColumn(level, chunk, dx, dz);
            }
        }
        if (expandedSpider) SpiderCaveDecoration.place(level, chunk, center, seed, branches);
        if (branches != null || kind.equals("yeti")) placeLoot(level, chunk);
        if (kind.equals("yeti")) placeYetiSpawner(level, chunk);
        placeResidents(level, chunk);
    }

    public void registerBoundary(net.minecraft.server.level.ServerLevel level) {
        BossStructureBounds.get(level).record("lair:" + kind + ":" + center.asLong(),
                new net.minecraft.world.phys.AABB(boundingBox.minX(), boundingBox.minY(), boundingBox.minZ(),
                        boundingBox.maxX() + 1, boundingBox.maxY() + 1, boundingBox.maxZ() + 1), null);
    }

    public BranchedBossCaves branchLayout() { return branches; }
    public boolean expandedSpider() { return expandedSpider; }

    private void placeBranchColumn(WorldGenLevel level, BoundingBox chunk, int dx, int dz) {
        var column = branches.column(dx, dz);
        if (column == null) return;
        var shell = (kind.equals("spider") ? Blocks.MOSS_BLOCK : Blocks.OBSIDIAN).defaultBlockState();
        for (int dy = column.bottom(); dy <= column.top(); dy++) {
            boolean air = column.hasAir() && dy > column.floor() && dy <= column.ceiling();
            put(level, chunk, dx, dy, dz, air ? Blocks.CAVE_AIR.defaultBlockState() : shell);
        }
        if (kind.equals("wither")) {
            // The low basalt galleries may contain lava beneath the approach.
            // Every shell column reaches solid support without changing bedrock.
            for (int y = center.getY() + column.bottom() - 1; y > level.getMinBuildHeight(); y--) {
                BlockPos pos = new BlockPos(center.getX() + dx, y, center.getZ() + dz);
                var existing = level.getBlockState(pos);
                if (existing.getFluidState().isEmpty() && existing.isCollisionShapeFullBlock(level, pos)) break;
                if (chunk.isInside(pos)) level.setBlock(pos, shell, 2);
            }
        }
    }

    private void put(WorldGenLevel level, BoundingBox chunk, int dx, int dy, int dz, BlockState state) {
        BlockPos pos = center.offset(dx, dy, dz);
        if (chunk.isInside(pos) && !level.getBlockState(pos).is(Blocks.CHEST)) level.setBlock(pos, state, 2);
    }

    private void placeSpiderHillColumn(WorldGenLevel level, BoundingBox chunk, int dx, int dz) {
        int sx = (seed & 1) == 0 ? 1 : -1, sz = (seed & 2) == 0 ? 1 : -1;
        int roof = spiderRoof(dx * sx, dz * sz);
        if (roof < 0) return;
        // Solid, steep terrain over the entrance. All interior air is now owned
        // exclusively by the two tunnels and their four pockets and boss room.
        for (int dy = -8; dy <= roof; dy++) put(level, chunk, dx, dy, dz, Blocks.MOSS_BLOCK.defaultBlockState());
    }

    private void placeLoot(WorldGenLevel level, BoundingBox chunk) {
        var positions = branches == null ? java.util.List.of(new BlockPos(-15, 1, -15), new BlockPos(15, 1, -15))
                : branches.lootPositions();
        ResourceLocation loot = new ResourceLocation("aoemod", "chests/" + kind + "_lair");
        for (BlockPos local : positions) {
            BlockPos pos = center.offset(local);
            if (!chunk.isInside(pos)) continue;
            if (level.getBlockState(pos).is(Blocks.CHEST)) continue;
            if (!level.getBlockState(pos).isAir() || !level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), Direction.UP)) continue;
            Direction facing = Direction.SOUTH;
            if (expandedSpider) {
                int sx = (seed & 1) == 0 ? 1 : -1, sz = (seed & 2) == 0 ? 1 : -1;
                var authored = new BlockPos(local.getX() * sx, local.getY(), local.getZ() * sz);
                Direction original = SpiderCaveLayout.chestFacing(authored);
                facing = Direction.fromDelta(original.getStepX() * sx, 0, original.getStepZ() * sz);
            }
            level.setBlock(pos, Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, facing), 2);
            if (level.getBlockEntity(pos) instanceof ChestBlockEntity chest) chest.setLootTable(loot, seed ^ pos.asLong());
        }
    }

    private void placeYetiColumn(WorldGenLevel level, BoundingBox chunk, int dx, int dz) {
        int edge = Math.max(Math.abs(dx), Math.abs(dz));
        int across = Math.min(Math.abs(dx), Math.abs(dz));
        int surface = yetiHillHeight(edge);
        boolean chamber = edge <= 24;
        boolean entrance = edge > 24 && across <= 8;
        int clearHeight = chamber ? 17 : entrance ? yetiGateHeight(across) : 0;
        for (int dy = -3; dy <= Math.max(surface, clearHeight); dy++) {
            if (dx == 0 && dz == 0 && dy == 1) continue;
            // The flat chamber has six solid ice roof layers (18..23), capped by
            // one snow layer at24. The rest of the buried hill is also blue ice.
            // Keep snow on the exterior and the gate's exposed sides/underside.
            boolean snowSkin = dy == surface;
            for (Direction direction : Direction.Plane.HORIZONTAL) {
                int nx = dx + direction.getStepX(), nz = dz + direction.getStepZ();
                int neighborEdge = Math.max(Math.abs(nx), Math.abs(nz));
                int neighborAcross = Math.min(Math.abs(nx), Math.abs(nz));
                if (dy > yetiHillHeight(neighborEdge)
                        || neighborEdge > 24 && neighborAcross <= 8 && dy <= yetiGateHeight(neighborAcross)) {
                    snowSkin = true;
                }
            }
            boolean gateLining = entrance && dy == clearHeight + 1
                    || edge == 25 && across <= 9 && dy <= 17;
            boolean blue = dy > 0 && !snowSkin && !gateLining;
            put(level, chunk, dx, dy, dz, (dy > 0 && dy <= clearHeight ? Blocks.CAVE_AIR
                    : blue ? Blocks.BLUE_ICE : Blocks.SNOW_BLOCK).defaultBlockState());
        }
        for (int y = center.getY() - 4; y >= Math.max(level.getMinBuildHeight(), chunk.minY()); y--) {
            BlockPos pos = new BlockPos(center.getX() + dx, y, center.getZ() + dz);
            var existing = level.getBlockState(pos);
            if (existing.getFluidState().isEmpty() && !existing.is(BlockTags.LEAVES)
                    && existing.isCollisionShapeFullBlock(level, pos)) break;
            level.setBlock(pos, Blocks.SNOW_BLOCK.defaultBlockState(), 2);
        }
    }

    private static int yetiGateHeight(int across) {
        // The two single-block shoulders belong to the reference arch. Its
        // central16-block opening sits below the raised chamber ceiling.
        return across == 6 ? 15 : Math.min(16, 40 - 4 * across);
    }

    private static int yetiHillHeight(int edge) {
        double blend = Math.max(0, (edge - 24.0) / 20);
        return (int) Math.round(24 * (1 - blend * blend * (3 - 2 * blend)));
    }

    private synchronized void placeYetiSpawner(WorldGenLevel level, BoundingBox chunk) {
        BlockPos pos = center.above();
        if ((spawned & 16) == 0 && chunk.isInside(pos)) {
            level.setBlock(pos, TFBlocks.ALPHA_YETI_BOSS_SPAWNER.get().defaultBlockState(), 2);
            spawned |= 16;
        }
    }

    /** Old saved starts can still include the native spike-decorating component. */
    public void finishYetiInterior(WorldGenLevel level, BoundingBox chunk) {
        if (!kind.equals("yeti")) return;
        for (int x = Math.max(chunk.minX(), boundingBox.minX()); x <= Math.min(chunk.maxX(), boundingBox.maxX()); x++) {
            for (int z = Math.max(chunk.minZ(), boundingBox.minZ()); z <= Math.min(chunk.maxZ(), boundingBox.maxZ()); z++) {
                int dx = x - center.getX(), dz = z - center.getZ();
                placeYetiColumn(level, chunk, dx, dz);
                int surface = yetiHillHeight(Math.max(Math.abs(dx), Math.abs(dz)));
                for (int dy = surface + 1; dy <= 28; dy++) {
                    BlockPos pos = center.offset(dx, dy, dz);
                    if (!chunk.isInside(pos)) continue;
                    var block = level.getBlockState(pos);
                    if (block.is(Blocks.STONE) || block.is(Blocks.PACKED_ICE) || block.is(Blocks.BLUE_ICE) || block.is(Blocks.ICE))
                        level.setBlock(pos, Blocks.CAVE_AIR.defaultBlockState(), 2);
                }
            }
        }
        placeLoot(level, chunk);
    }

    private void placeOrcColumn(WorldGenLevel level, BoundingBox chunk, int dx, int dz) {
        // A small open camp keeps the savanna skyline intact.
        if (Math.abs(dx) > 7 || Math.abs(dz) > 7) return;
        if (Math.abs(dx) == 7 && Math.abs(dz) == 7) {
            for (int dy = 1; dy <= 3; dy++) put(level, chunk, dx, dy, dz, Blocks.ACACIA_LOG.defaultBlockState());
        }
    }

    private synchronized void placeResidents(WorldGenLevel level, BoundingBox chunk) {
        if (expandedSpider) {
            placeSpiderResidents(level, chunk);
            return;
        }
        String[] ids = switch (kind) {
            case "spider" -> new String[]{"aoemod:spider_queen", "minecraft:spider", "minecraft:cave_spider",
                    "minecraft:spider", "minecraft:cave_spider", "minecraft:spider", "minecraft:cave_spider"};
            case "wither" -> new String[]{"minecraft:wither", "minecraft:wither_skeleton", "minecraft:magma_cube",
                    "minecraft:wither_skeleton", "minecraft:magma_cube", "minecraft:wither_skeleton", "minecraft:magma_cube"};
            // The central native Alpha Yeti spawner handles the boss.
            case "yeti" -> new String[]{"twilightforest:yeti", "aoemod:breeze", "twilightforest:yeti", "aoemod:breeze"};
            default -> new String[]{"orcz:orcshield", "orcz:orcsavagemace"};
        };
        for (int i = 0; i < ids.length; i++) {
            if ((spawned & (1 << i)) != 0) continue;
            double angle = i * Math.PI / 3;
            int distance = i == 0 && !kind.equals("yeti") ? 0 : kind.equals("orc") ? 4 : 9;
            BlockPos pos = center.offset((int) Math.round(Math.cos(angle) * distance), 1,
                    (int) Math.round(Math.sin(angle) * distance));
            if (branches != null) pos = center.offset(branches.residents().get(i));
            if (!chunk.isInside(pos)) continue;
            if (kind.equals("orc")) {
                pos = level.getHeightmapPos(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, pos);
            }
            if (spawn(level, ids[i], pos)) spawned |= 1 << i;
        }
    }

    private void placeSpiderResidents(WorldGenLevel level, BoundingBox chunk) {
        var roster = SpiderCaveLayout.residents();
        int sx = (seed & 1) == 0 ? 1 : -1, sz = (seed & 2) == 0 ? 1 : -1;
        for (int index = 0; index < roster.size(); index++) {
            if (spiderResidents.get(index)) continue;
            var resident = roster.get(index);
            BlockPos local = resident.pos();
            BlockPos pos = center.offset(local.getX() * sx, local.getY(), local.getZ() * sz);
            if (!chunk.isInside(pos)) continue;
            if (spawn(level, resident.id(), pos, mob -> {
                // Spider finalization can randomly create a jockey; the authored roster has no riders.
                for (var passenger : java.util.List.copyOf(mob.getPassengers())) passenger.discard();
                // A fixed small size keeps the roster deterministic and avoids split reinforcements.
                if (mob instanceof net.minecraft.world.entity.monster.Slime slime) slime.setSize(1, true);
                if (mob instanceof net.minecraft.world.entity.AgeableMob animal) animal.setAge(0);
            })) spiderResidents.set(index);
        }
    }

    public static boolean spawn(WorldGenLevel level, String id, BlockPos pos) {
        return spawn(level, id, pos, mob -> { });
    }

    static boolean spawn(WorldGenLevel level, String id, BlockPos pos, java.util.function.Consumer<Mob> configure) {
        var type = ForgeRegistries.ENTITY_TYPES.getValue(new ResourceLocation(id));
        if (type == null || !(type.create(level.getLevel()) instanceof Mob mob)) return false;
        mob.moveTo(pos.getX() + .5, pos.getY(), pos.getZ() + .5, 0, 0);
        mob.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), MobSpawnType.STRUCTURE, null, null);
        if (mob instanceof net.minecraft.world.entity.animal.Sheep sheep) sheep.setColor(net.minecraft.world.item.DyeColor.BROWN);
        if (mob instanceof MagmaCube cube) cube.setSize(2, true);
        StructureSpawnedMobData.mark(mob, pos);
        configure.accept(mob);
        level.addFreshEntityWithPassengers(mob);
        return true;
    }
}

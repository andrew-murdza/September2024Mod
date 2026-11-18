package net.amurdza.examplemod.worldgen.structure;

import net.amurdza.examplemod.registry.ModStructures;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;

import java.util.ArrayList;
import java.util.List;

/** Clears the native template gaps before building, then applies only missing connections afterward. */
public final class MansionEnvelopePiece extends StructurePiece {
    public record Passage(BoundingBox box, boolean clear, boolean stairConnection) {
        public Passage(BoundingBox box, boolean clear) { this(box, clear, false); }
    }
    public record UpperStair(BlockPos landing, Direction outward) { }
    private final boolean prepare;
    private final List<Passage> passages;
    private final List<BlockPos> redCarpetRepairs;
    private final List<BlockPos> whiteCarpetRepairs;
    private final List<BlockPos> roofSlabs;
    private final List<UpperStair> upperStairs;
    private boolean oakFloors;

    public MansionEnvelopePiece oakFloors() { oakFloors = true; return this; }

    public MansionEnvelopePiece(BoundingBox bounds, boolean prepare, List<Passage> passages) {
        this(bounds, prepare, passages, List.of());
    }

    public MansionEnvelopePiece(BoundingBox bounds, boolean prepare, List<Passage> passages, List<BlockPos> redCarpetRepairs) {
        this(bounds, prepare, passages, redCarpetRepairs, List.of(), List.of(), List.of());
    }

    public MansionEnvelopePiece(BoundingBox bounds, boolean prepare, List<Passage> passages, List<BlockPos> redCarpetRepairs,
            List<BlockPos> whiteCarpetRepairs, List<BlockPos> roofSlabs, List<UpperStair> upperStairs) {
        super(ModStructures.MANSION_ENVELOPE_PIECE.get(), 0, new BoundingBox(bounds.minX(), bounds.minY(), bounds.minZ(),
                bounds.maxX(), bounds.maxY(), bounds.maxZ()));
        this.prepare = prepare;
        this.passages = List.copyOf(passages);
        this.redCarpetRepairs = new ArrayList<>(redCarpetRepairs);
        this.whiteCarpetRepairs = new ArrayList<>(whiteCarpetRepairs);
        this.roofSlabs = new ArrayList<>(roofSlabs);
        this.upperStairs = new ArrayList<>(upperStairs);
    }

    public MansionEnvelopePiece(StructurePieceSerializationContext context, CompoundTag tag) {
        super(ModStructures.MANSION_ENVELOPE_PIECE.get(), tag);
        prepare = tag.getBoolean("Prepare");
        oakFloors = tag.getBoolean("OakFloors");
        var saved = new ArrayList<Passage>();
        for (var element : tag.getList("Passages", Tag.TAG_COMPOUND)) {
            var data = (CompoundTag) element;
            int[] b = data.getIntArray("Box");
            if (b.length == 6) saved.add(new Passage(new BoundingBox(b[0], b[1], b[2], b[3], b[4], b[5]), data.getBoolean("Clear"), data.getBoolean("StairConnection")));
        }
        passages = List.copyOf(saved);
        redCarpetRepairs = new ArrayList<>();
        for (var element : tag.getList("RedCarpetRepairs", Tag.TAG_INT_ARRAY)) {
            int[] pos = ((net.minecraft.nbt.IntArrayTag) element).getAsIntArray();
            if (pos.length == 3) redCarpetRepairs.add(new BlockPos(pos[0], pos[1], pos[2]));
        }
        whiteCarpetRepairs = readPositions(tag, "WhiteCarpetRepairs");
        roofSlabs = readPositions(tag, "RoofSlabs");
        upperStairs = new ArrayList<>();
        for (var element : tag.getList("UpperStairs", Tag.TAG_COMPOUND)) {
            var data = (CompoundTag) element;
            int[] pos = data.getIntArray("Landing");
            Direction direction = Direction.byName(data.getString("Outward"));
            if (pos.length == 3 && direction != null && direction.getAxis().isHorizontal())
                upperStairs.add(new UpperStair(new BlockPos(pos[0], pos[1], pos[2]), direction));
        }
    }

    @Override protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
        tag.putBoolean("Prepare", prepare);
        tag.putBoolean("OakFloors", oakFloors);
        var saved = new ListTag();
        for (var passage : passages) {
            var data = new CompoundTag();
            var b = passage.box();
            data.putIntArray("Box", new int[]{b.minX(), b.minY(), b.minZ(), b.maxX(), b.maxY(), b.maxZ()});
            data.putBoolean("Clear", passage.clear());
            data.putBoolean("StairConnection", passage.stairConnection());
            saved.add(data);
        }
        tag.put("Passages", saved);
        var carpet = new ListTag();
        for (BlockPos pos : redCarpetRepairs) carpet.add(new net.minecraft.nbt.IntArrayTag(new int[]{pos.getX(), pos.getY(), pos.getZ()}));
        tag.put("RedCarpetRepairs", carpet);
        savePositions(tag, "WhiteCarpetRepairs", whiteCarpetRepairs);
        savePositions(tag, "RoofSlabs", roofSlabs);
        var stairs = new ListTag();
        for (var stair : upperStairs) {
            var data = new CompoundTag();
            data.putIntArray("Landing", new int[]{stair.landing().getX(), stair.landing().getY(), stair.landing().getZ()});
            data.putString("Outward", stair.outward().getName());
            stairs.add(data);
        }
        tag.put("UpperStairs", stairs);
    }

    @Override public void move(int x, int y, int z) {
        super.move(x, y, z);
        passages.forEach(p -> p.box().move(x, y, z));
        redCarpetRepairs.replaceAll(pos -> pos.offset(x, y, z));
        whiteCarpetRepairs.replaceAll(pos -> pos.offset(x, y, z));
        roofSlabs.replaceAll(pos -> pos.offset(x, y, z));
        upperStairs.replaceAll(stair -> new UpperStair(stair.landing().offset(x, y, z), stair.outward()));
    }

    @Override public void postProcess(WorldGenLevel level, StructureManager manager, ChunkGenerator generator,
            RandomSource random, BoundingBox clip, ChunkPos chunk, BlockPos pivot) {
        if (prepare) {
            for (int x = Math.max(clip.minX(), boundingBox.minX()); x <= Math.min(clip.maxX(), boundingBox.maxX()); x++)
                for (int z = Math.max(clip.minZ(), boundingBox.minZ()); z <= Math.min(clip.maxZ(), boundingBox.maxZ()); z++)
                    for (int y = Math.max(clip.minY(), boundingBox.minY()); y <= Math.min(clip.maxY(), boundingBox.maxY()); y++) {
                        var pos = new BlockPos(x, y, z);
                        if (!level.getBlockState(pos).isAir()) level.setBlock(pos, Blocks.AIR.defaultBlockState(), 18);
                    }
            return;
        }
        for (var passage : passages) {
            var box = passage.box();
            if (!box.intersects(clip)) continue;
            for (BlockPos pos : BlockPos.betweenClosed(box.minX(), box.minY(), box.minZ(), box.maxX(), box.maxY(), box.maxZ())) {
                if (!clip.isInside(pos)) continue;
                if (pos.getY() == box.minY()) {
                    // The native upper landing has ascending stairs one block below the
                    // third-floor deck. Their air is headroom, not a missing floor tile.
                    boolean stairHeadroom = ascendingStair(level.getBlockState(pos.below()));
                    if (!stairHeadroom && (level.getBlockState(pos).isAir() || !level.getFluidState(pos).isEmpty()))
                        level.setBlock(pos, (oakFloors ? Blocks.OAK_PLANKS : Blocks.BIRCH_PLANKS).defaultBlockState(), 18);
                } else if (passage.clear() && !ascendingStair(level.getBlockState(pos))
                        && !level.getBlockState(pos).is(net.minecraft.tags.BlockTags.WOOL_CARPETS)
                        && !level.getBlockState(pos).is(net.minecraft.tags.BlockTags.WOOL)
                        && !level.getBlockState(pos).is(Blocks.BOOKSHELF)
                        && !(passage.stairConnection() && level.getBlockState(pos).is(Blocks.COBBLESTONE))) {
                    // A room connection can meet the first turning treads of a native stair.
                    // Removing those treads leaves a two-block rise below the remaining flight.
                    level.setBlock(pos, Blocks.AIR.defaultBlockState(), 18);
                }
            }
        }
        for (BlockPos pos : redCarpetRepairs) if (clip.isInside(pos) && level.getBlockState(pos).is(Blocks.WHITE_CARPET))
            level.setBlock(pos, Blocks.RED_CARPET.defaultBlockState(), 18);
        for (BlockPos pos : whiteCarpetRepairs) if (clip.isInside(pos) && level.getBlockState(pos).is(Blocks.RED_CARPET))
            level.setBlock(pos, Blocks.WHITE_CARPET.defaultBlockState(), 18);
        for (BlockPos pos : roofSlabs) if (clip.isInside(pos))
            level.setBlock(pos, Blocks.OAK_SLAB.defaultBlockState(), 18);
        for (var stair : upperStairs) {
            var tread = Blocks.COBBLESTONE_STAIRS.defaultBlockState()
                    .setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING, stair.outward());
            for (int step = 1; step <= 3; step++) for (int across = -2; across <= 2; across++) {
                BlockPos pos = stair.landing().relative(stair.outward(), step)
                        .relative(stair.outward().getClockWise(), across).above(step);
                for (int y = stair.landing().getY(); y < pos.getY(); y++) if (clip.isInside(pos.atY(y)))
                    level.setBlock(pos.atY(y), Blocks.COBBLESTONE.defaultBlockState(), 18);
                if (clip.isInside(pos)) level.setBlock(pos, tread, 18);
                // Remove the raised hall deck from the lower treads' headroom.
                for (int y = pos.getY() + 1; y <= stair.landing().getY() + 6; y++) if (clip.isInside(pos.atY(y)))
                    level.setBlock(pos.atY(y), Blocks.AIR.defaultBlockState(), 18);
            }
        }
    }

    private static List<BlockPos> readPositions(CompoundTag tag, String name) {
        var positions = new ArrayList<BlockPos>();
        for (var element : tag.getList(name, Tag.TAG_INT_ARRAY)) {
            int[] pos = ((net.minecraft.nbt.IntArrayTag) element).getAsIntArray();
            if (pos.length == 3) positions.add(new BlockPos(pos[0], pos[1], pos[2]));
        }
        return positions;
    }

    private static void savePositions(CompoundTag tag, String name, List<BlockPos> positions) {
        var list = new ListTag();
        for (BlockPos pos : positions) list.add(new net.minecraft.nbt.IntArrayTag(new int[]{pos.getX(), pos.getY(), pos.getZ()}));
        tag.put(name, list);
    }
    private static boolean ascendingStair(net.minecraft.world.level.block.state.BlockState state) {
        return state.getBlock() instanceof net.minecraft.world.level.block.StairBlock
                && state.getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.HALF)
                == net.minecraft.world.level.block.state.properties.Half.BOTTOM;
    }

}

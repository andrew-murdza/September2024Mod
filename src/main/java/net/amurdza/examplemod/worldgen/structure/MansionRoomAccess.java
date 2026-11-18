package net.amurdza.examplemod.worldgen.structure;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.structures.WoodlandMansionPieces.WoodlandMansionPiece;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

/** Connects only disconnected native room components; existing prison doors and furniture survive. */
public final class MansionRoomAccess {
    private record Cell(BlockPos pos, BlockState state) { }
    private record Node(BoundingBox box, boolean hall, boolean stairs) { }
    private record Edge(int first, int second, BoundingBox box, boolean open, int cost) { }
    private static final Map<StructureTemplate, List<Cell>> CELLS = new WeakHashMap<>();
    private MansionRoomAccess() { }

    public record Carpets(List<BlockPos> red, List<BlockPos> white) { }

    /** Compatibility entry point for callers that only need the red interior repairs. */
    public static List<BlockPos> carpetRepairs(StructureTemplateManager manager, List<WoodlandMansionPiece> pieces) {
        return carpetPlan(manager, pieces).red();
    }

    /** A continuous one-cell white border, including the diagonal corners of corridor junctions. */
    public static Carpets carpetPlan(StructureTemplateManager manager, List<WoodlandMansionPiece> pieces) {
        Set<StructureTemplate> corridors = new HashSet<>();
        for (String name : List.of("entrance", "corridor_floor", "carpet_north", "carpet_east",
                "carpet_south_1", "carpet_south_2", "carpet_west_1", "carpet_west_2"))
            {
                manager.get(new ResourceLocation("minecraft", "woodland_mansion/" + name)).ifPresent(corridors::add);
                manager.get(new ResourceLocation("minecraft", "woodland_mansion/" + MansionRoomCatalogue.compact(name))).ifPresent(corridors::add);
                manager.get(new ResourceLocation("minecraft", "woodland_mansion/" + MansionRoomCatalogue.newTemplate(name))).ifPresent(corridors::add);
            }
        var voxels = new HashMap<BlockPos, BlockState>();
        var candidates = new HashSet<BlockPos>();
        for (var piece : pieces) for (var cell : cells(piece.template())) {
            BlockPos pos = StructureTemplate.calculateRelativePosition(piece.placeSettings(), cell.pos()).offset(piece.templatePosition());
            voxels.put(pos, cell.state());
            if (corridors.contains(piece.template()) && (cell.state().is(Blocks.WHITE_CARPET) || cell.state().is(Blocks.RED_CARPET))) candidates.add(pos);
        }
        var red = new ArrayList<BlockPos>();
        var white = new ArrayList<BlockPos>();
        for (BlockPos pos : candidates) {
            BlockState state = voxels.getOrDefault(pos, Blocks.AIR.defaultBlockState());
            if (!state.is(Blocks.WHITE_CARPET) && !state.is(Blocks.RED_CARPET)) continue;
            boolean interior = true;
            for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++)
                if (!voxels.getOrDefault(pos.offset(dx, 0, dz), Blocks.AIR.defaultBlockState()).is(BlockTags.WOOL_CARPETS)) interior = false;
            boolean whiteBorder = !interior && wideCarpetBorder(pos, voxels);
            if (!whiteBorder && state.is(Blocks.WHITE_CARPET)) red.add(pos);
            if (whiteBorder && state.is(Blocks.RED_CARPET)) white.add(pos);
        }
        red.sort(Comparator.comparingLong(BlockPos::asLong));
        white.sort(Comparator.comparingLong(BlockPos::asLong));
        return new Carpets(List.copyOf(red), List.copyOf(white));
    }

    /** Replace only exposed full roof tops; native sloping stairs are already spawn-proof. */
    public static List<BlockPos> roofSlabs(StructureTemplateManager manager, List<WoodlandMansionPiece> pieces) {
        var roofs = new HashSet<StructureTemplate>();
        for (String name : List.of("roof", "roof_front", "roof_corner", "roof_inner_corner"))
            manager.get(new ResourceLocation("minecraft", "woodland_mansion/" + name)).ifPresent(roofs::add);
        var voxels = new HashMap<BlockPos, BlockState>();
        for (var piece : pieces) if (roofs.contains(piece.template())) for (var cell : cells(piece.template())) {
            var pos = StructureTemplate.calculateRelativePosition(piece.placeSettings(), cell.pos()).offset(piece.templatePosition());
            voxels.put(pos, cell.state());
        }
        var tops = new HashMap<BlockPos, BlockPos>();
        voxels.forEach((pos, state) -> {
            if (state.isAir()) return;
            tops.merge(pos.atY(0), pos, (a, b) -> a.getY() >= b.getY() ? a : b);
        });
        return tops.values().stream().filter(pos -> net.minecraft.world.level.block.Block.isFaceFull(
                        voxels.get(pos).getCollisionShape(EmptyBlockGetter.INSTANCE, pos), net.minecraft.core.Direction.UP))
                .sorted(Comparator.comparingLong(BlockPos::asLong)).toList();
    }

    public static List<MansionEnvelopePiece.Passage> plan(StructureTemplateManager manager, List<WoodlandMansionPiece> pieces) {
        var halls = Set.of(manager.getOrCreate(new ResourceLocation("minecraft", "woodland_mansion/corridor_floor")),
                manager.getOrCreate(new ResourceLocation("minecraft", "woodland_mansion/" + MansionRoomCatalogue.newTemplate("corridor_floor"))));
        var rooms = new HashSet<StructureTemplate>();
        for (String prefix : List.of("1x1_a", "1x1_as", "1x1_b", "1x2_a", "1x2_b", "1x2_c", "1x2_d", "1x2_s", "1x2_se", "2x2_a", "2x2_b", "2x2_s"))
            for (int i = 1; i <= 9; i++) {
                manager.get(new ResourceLocation("minecraft", "woodland_mansion/" + prefix + i)).ifPresent(rooms::add);
                manager.get(new ResourceLocation("minecraft", "woodland_mansion/" + MansionRoomCatalogue.compact(prefix + i))).ifPresent(rooms::add);
                manager.get(new ResourceLocation("minecraft", "woodland_mansion/" + MansionRoomCatalogue.newTemplate(prefix + i))).ifPresent(rooms::add);
            }
        var singleStair = manager.getOrCreate(new ResourceLocation("minecraft", "woodland_mansion/" + MansionUpperLayout.SINGLE_STAIRS));
        var stairs = new HashSet<>(Set.of(singleStair, manager.getOrCreate(new ResourceLocation("minecraft", "woodland_mansion/1x2_c_stairs")),
                manager.getOrCreate(new ResourceLocation("minecraft", "woodland_mansion/1x2_d_stairs")),
                manager.getOrCreate(new ResourceLocation("minecraft", "woodland_mansion/" + MansionRoomCatalogue.compact("1x2_d_stairs")))));
        for (String name : List.of("stair_1x1", "1x2_c_stairs", "1x2_d_stairs"))
            manager.get(new ResourceLocation("minecraft", "woodland_mansion/" + MansionRoomCatalogue.newTemplate(name))).ifPresent(stairs::add);
        rooms.addAll(stairs);
        rooms.add(manager.getOrCreate(new ResourceLocation("minecraft", "woodland_mansion/aoe_compact_1x2_d1_v3")));
        rooms.add(manager.getOrCreate(new ResourceLocation("minecraft", "woodland_mansion/aoe_compact_1x2_c4_v4")));
        var floors = pieces.stream().filter(p -> halls.contains(p.template())).map(p -> p.templatePosition().getY()).distinct().sorted().toList();
        var voxels = new HashMap<BlockPos, BlockState>();
        for (var piece : pieces) {
            for (var cell : cells(piece.template())) {
                var pos = StructureTemplate.calculateRelativePosition(piece.placeSettings(), cell.pos()).offset(piece.templatePosition());
                if (floors.stream().noneMatch(y -> pos.getY() >= y && pos.getY() <= y + 3)) continue;
                voxels.put(pos, cell.state());
            }
        }
        var result = new ArrayList<MansionEnvelopePiece.Passage>();
        for (int floor : floors) {
            var nodes = new ArrayList<Node>();
            for (var piece : pieces) {
                boolean isHall = halls.contains(piece.template());
                if (!isHall && !rooms.contains(piece.template())) continue;
                boolean atFloor = piece.templatePosition().getY() == floor;
                if (!atFloor && !(stairs.contains(piece.template()) && piece.getBoundingBox().minY() < floor
                        && piece.getBoundingBox().maxY() >= floor + 2)) continue;
                nodes.add(new Node(piece.getBoundingBox(), isHall, stairs.contains(piece.template())));
            }
            var edges = new ArrayList<Edge>();
            int[] parent = new int[nodes.size()];
            for (int i = 0; i < parent.length; i++) parent[i] = i;
            for (int i = 0; i < nodes.size(); i++) for (int j = i + 1; j < nodes.size(); j++) {
                var edge = edge(i, j, nodes.get(i), nodes.get(j), floor, voxels);
                // Authored straight flights have exactly two deliberate door
                // planes. A side opening would cut through their supporting
                // wedge and can strand the upper flight behind solid wood.
                if (edge == null || !edge.open() && (nodes.get(i).stairs() || nodes.get(j).stairs())) continue;
                edges.add(edge);
                if (edge.open()) {
                    parent[find(parent, i)] = find(parent, j);
                    result.add(new MansionEnvelopePiece.Passage(edge.box(), false, nodes.get(i).stairs() || nodes.get(j).stairs()));
                }
            }
            // A globally connected room can still be reached only through its
            // neighbor. Give every hall-adjacent room its own direct doorway.
            for (int i = 0; i < nodes.size(); i++) {
                if (nodes.get(i).hall() || nodes.get(i).stairs()) continue;
                Edge best = null;
                for (var candidate : edges) {
                    int other = candidate.first() == i ? candidate.second() : candidate.second() == i ? candidate.first() : -1;
                    if (other < 0 || !nodes.get(other).hall()) continue;
                    if (best == null || candidate.open() && !best.open()
                            || candidate.open() == best.open() && candidate.cost() < best.cost()) best = candidate;
                }
                if (best != null && !best.open()) {
                    parent[find(parent, best.first())] = find(parent, best.second());
                    result.add(new MansionEnvelopePiece.Passage(best.box(), true));
                }
            }
            edges.sort(Comparator.comparingInt(Edge::cost));
            for (var edge : edges) {
                int first = find(parent, edge.first()), second = find(parent, edge.second());
                if (first == second) continue;
                parent[first] = second;
                result.add(new MansionEnvelopePiece.Passage(edge.box(), true,
                        nodes.get(edge.first()).stairs() || nodes.get(edge.second()).stairs()));
            }
        }
        return result;
    }

    private static boolean wideCarpetBorder(BlockPos pos, Map<BlockPos, BlockState> voxels) {
        boolean allNeighbors = true, xRun = false, zRun = false;
        for (var direction : net.minecraft.core.Direction.Plane.HORIZONTAL) {
            boolean outside = !voxels.getOrDefault(pos.relative(direction.getOpposite()), Blocks.AIR.defaultBlockState()).is(BlockTags.WOOL_CARPETS);
            allNeighbors &= !outside;
            boolean span = true;
            for (int step = 1; step <= 4; step++)
                span &= voxels.getOrDefault(pos.relative(direction, step), Blocks.AIR.defaultBlockState()).is(BlockTags.WOOL_CARPETS);
            if (outside && span) return true; // white + three red + white
            if (span && direction.getAxis() == net.minecraft.core.Direction.Axis.X) xRun = true;
            if (span && direction.getAxis() == net.minecraft.core.Direction.Axis.Z) zRun = true;
        }
        // Preserve the diagonal white join where two full-width halls meet.
        return allNeighbors && xRun && zRun;
    }

    private static int find(int[] parent, int value) {
        while (parent[value] != value) value = parent[value];
        return value;
    }

    private static Edge edge(int i, int j, Node first, Node second, int floor, Map<BlockPos, BlockState> voxels) {
        BoundingBox a = first.box(), b = second.box();
        int min, max, lo, hi;
        boolean xAxis;
        if (a.maxX() <= b.minX() && b.minX() - a.maxX() <= 3) { lo = a.maxX() - 1; hi = b.minX() + 1; xAxis = true; }
        else if (b.maxX() <= a.minX() && a.minX() - b.maxX() <= 3) { lo = b.maxX() - 1; hi = a.minX() + 1; xAxis = true; }
        else if (a.maxZ() <= b.minZ() && b.minZ() - a.maxZ() <= 3) { lo = a.maxZ() - 1; hi = b.minZ() + 1; xAxis = false; }
        else if (b.maxZ() <= a.minZ() && a.minZ() - b.maxZ() <= 3) { lo = b.maxZ() - 1; hi = a.minZ() + 1; xAxis = false; }
        else return null;
        min = xAxis ? Math.max(a.minZ(), b.minZ()) : Math.max(a.minX(), b.minX());
        max = xAxis ? Math.min(a.maxZ(), b.maxZ()) : Math.min(a.maxX(), b.maxX());
        if (max - min < 2) return null;
        Edge best = null;
        for (int center = min + 1; center < max; center++) {
            int obstruction = 0;
            boolean open = true;
            for (int along = lo; along <= hi; along++) {
                for (int y = floor + 1; y <= floor + 2; y++) {
                    var state = voxels.getOrDefault(xAxis ? new BlockPos(along, y, center) : new BlockPos(center, y, along), Blocks.AIR.defaultBlockState());
                    if (!walkable(state)) open = false;
                    // Score the complete opening that Envelope will clear, not
                    // just its center; a clear center can pass beside a bed.
                    for (int offset = -1; offset <= 1; offset++) {
                        var side = voxels.getOrDefault(xAxis ? new BlockPos(along, y, center + offset)
                                : new BlockPos(center + offset, y, along), Blocks.AIR.defaultBlockState());
                        if (!walkable(side)) obstruction += side.is(BlockTags.WOOL) || side.is(Blocks.BOOKSHELF) ? 1000 : 1;
                    }
                }
            }
            var box = xAxis ? new BoundingBox(lo, floor, center - 1, hi, floor + 3, center + 1)
                    : new BoundingBox(center - 1, floor, lo, center + 1, floor + 3, hi);
            int cost = obstruction * 100 + (first.hall() || second.hall() ? 0 : 50) + Math.abs(2 * center - min - max);
            if (best == null || cost < best.cost()) best = new Edge(i, j, box, open, cost);
        }
        return best;
    }

    private static boolean walkable(BlockState state) {
        return state.isAir() || state.getBlock() instanceof DoorBlock || state.is(BlockTags.WOOL_CARPETS)
                || state.getBlock() instanceof net.minecraft.world.level.block.StairBlock
                    && state.getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.HALF)
                        == net.minecraft.world.level.block.state.properties.Half.BOTTOM
                || state.is(Blocks.STRUCTURE_BLOCK) || state.getCollisionShape(EmptyBlockGetter.INSTANCE, BlockPos.ZERO).isEmpty();
    }

    private static synchronized List<Cell> cells(StructureTemplate template) {
        return CELLS.computeIfAbsent(template, source -> {
            var data = source.save(new CompoundTag());
            var palette = data.getList("palette", Tag.TAG_COMPOUND);
            var states = new ArrayList<BlockState>();
            for (var entry : palette) states.add(NbtUtils.readBlockState(BuiltInRegistries.BLOCK.asLookup(), (CompoundTag) entry));
            var result = new ArrayList<Cell>();
            for (var entry : data.getList("blocks", Tag.TAG_COMPOUND)) {
                var block = (CompoundTag) entry;
                var pos = block.getList("pos", Tag.TAG_INT);
                result.add(new Cell(new BlockPos(pos.getInt(0), pos.getInt(1), pos.getInt(2)), states.get(block.getInt("state"))));
            }
            return List.copyOf(result);
        });
    }
}

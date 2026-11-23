package net.amurdza.examplemod.worldgen.structure;

import net.minecraft.core.BlockPos;

import java.util.ArrayList;
import java.util.List;

/** Shared authored room/corridor geometry; both branches descend gently and rejoin at the boss. */
public final class BranchedBossCaves {
    public record Column(int bottom, int top, int floor, int ceiling) {
        public boolean hasAir() { return floor < ceiling; }
    }
    private record Point(int x, int floor, int z) { }
    private record Passage(Point from, Point to, int radius, int height, boolean room) { }
    private static final int RADIUS = 64;
    private static final int WIDTH = RADIUS * 2 + 1;
    private static final java.util.Map<Integer, Column[]> SHAPES = new java.util.concurrent.ConcurrentHashMap<>();
    private final Column[] shape;
    private final List<Passage> passages = new ArrayList<>();
    private final List<BlockPos> residents = new ArrayList<>();
    private final int sx, sz;
    private final boolean spider, expandedSpider;
    private final int witherDepth;
    private final BlockPos entrance;
    private final List<BlockPos> branches;

    public BranchedBossCaves(String kind, int centerY, long seed) {
        this(kind, centerY, seed, true);
    }

    public BranchedBossCaves(String kind, int centerY, long seed, boolean expandedSpider) {
        spider = kind.equals("spider");
        this.expandedSpider = spider && expandedSpider;
        witherDepth = spider ? 0 : Math.min(4, Math.max(0, centerY - 5));
        sx = spider && (seed & 1) != 0 ? -1 : 1;
        sz = spider && (seed & 2) != 0 ? -1 : 1;
        if (this.expandedSpider) {
            path(54, 0, 54, 44, 0, 44, 3, 6);
            path(44, 0, 44, 24, -6, 24, 3, 6);
            // Finish each ramp outside its larger room before entering on a flat landing.
            path(24, -6, 24, 32, -14, 6, 3, 6);
            path(32, -14, 6, 32, -14, -10, 3, 6);
            path(24, -6, 24, 6, -14, 32, 3, 6);
            path(6, -14, 32, -10, -14, 32, 3, 6);
            path(32, -14, -10, 32, -14, -25, 3, 6);
            path(32, -14, -25, 20, -22, -35, 3, 6);
            path(20, -22, -35, 5, -22, -38, 3, 6);
            path(-10, -14, 32, -25, -14, 32, 3, 6);
            path(-25, -14, 32, -35, -22, 20, 3, 6);
            path(-35, -22, 20, -38, -22, 5, 3, 6);
            path(5, -22, -38, -10, -22, -38, 3, 6);
            path(-10, -22, -38, -23, -34, -51, 3, 6);
            path(-23, -34, -51, -28, -34, -28, 3, 6);
            path(-38, -22, 5, -38, -22, -10, 3, 6);
            path(-38, -22, -10, -51, -34, -23, 3, 6);
            path(-51, -34, -23, -28, -34, -28, 3, 6);
            // A silk nursery connects the two upper galleries without another steep descent.
            path(32, -14, -10, 0, -14, 0, 3, 6);
            path(-10, -14, 32, 0, -14, 0, 3, 6);
            for (var chamber : SpiderCaveLayout.chambers()) {
                var point = chamber.center();
                room(point.getX(), point.getY(), point.getZ(), chamber.radius(), chamber.height());
            }
            for (var resident : SpiderCaveLayout.residents()) {
                var point = resident.pos();
                residents.add(local(point.getX(), point.getY(), point.getZ()));
            }
            entrance = local(54, 1, 54);
            branches = SpiderCaveLayout.chambers().stream().filter(chamber -> chamber.theme() != SpiderCaveLayout.Theme.QUEEN)
                    .map(chamber -> local(chamber.center().getX(), chamber.center().getY() + 1, chamber.center().getZ())).toList();
        } else if (spider) {
            path(54, 0, 54, 44, 0, 44, 3, 6);
            path(44, 0, 44, 24, -6, 24, 3, 6);
            path(24, -6, 24, 34, -12, 8, 3, 6);
            path(34, -12, 8, 32, -14, -10, 3, 6);
            path(24, -6, 24, 8, -12, 34, 3, 6);
            path(8, -12, 34, -10, -14, 32, 3, 6);
            path(32, -14, -10, 32, -14, -18, 3, 6);
            path(32, -14, -18, 20, -22, -35, 3, 6);
            path(20, -22, -35, 5, -22, -38, 3, 6);
            path(-10, -14, 32, -18, -14, 32, 3, 6);
            path(-18, -14, 32, -35, -22, 20, 3, 6);
            path(-35, -22, 20, -38, -22, 5, 3, 6);
            // Finish each descent outside the boss room's radius before turning
            // inward. Overlapping a descending ramp with the deeper flat room
            // would replace its last steps with an impassable two-block ledge.
            path(5, -22, -38, -7, -22, -38, 3, 6);
            path(-7, -22, -38, -22, -34, -48, 3, 6);
            path(-22, -34, -48, -28, -34, -28, 3, 6);
            path(-38, -22, 5, -38, -22, -7, 3, 6);
            path(-38, -22, -7, -48, -34, -22, 3, 6);
            path(-48, -34, -22, -28, -34, -28, 3, 6);
            room(32, -14, -10, 7, 8);
            room(-10, -14, 32, 7, 8);
            room(5, -22, -38, 8, 9);
            room(-38, -22, 5, 8, 9);
            room(-28, -34, -28, 14, 11);
            addResidents(new int[][]{{-28, -33, -28}, {32, -13, -10}, {-10, -13, 32},
                    {5, -21, -38}, {-38, -21, 5}, {-35, -33, -28}, {-28, -33, -35}});
            entrance = local(54, 1, 54);
            branches = List.of(local(32, -13, -10), local(-10, -13, 32),
                    local(5, -21, -38), local(-38, -21, 5));
        } else {
            int depth = Math.min(4, Math.max(0, centerY - 5));
            room(0, 0, 24, 8, 8);
            // Continue beyond the owning piece so its southern edge is an open
            // mouth, rather than the sealed end cap of a rounded passage.
            path(0, 0, 44, 0, 0, 15, 3, 7);
            path(0, 0, 15, 15, -depth / 2, 1, 3, 7);
            path(0, 0, 15, -15, -depth / 2, 1, 3, 7);
            path(15, -depth / 2, 1, 15, -depth, -10, 3, 7);
            path(-15, -depth / 2, 1, -15, -depth, -10, 3, 7);
            path(15, -depth, -10, 0, -depth, -22, 3, 7);
            path(-15, -depth, -10, 0, -depth, -22, 3, 7);
            room(15, -depth / 2, 1, 6, 8);
            room(-15, -depth / 2, 1, 6, 8);
            room(0, -depth, -22, 14, 14);
            addResidents(new int[][]{{0, 1 - depth, -22}, {15, 1 - depth / 2, 1}, {-15, 1 - depth / 2, 1},
                    {15, 1 - depth, -10}, {-15, 1 - depth, -10}, {7, 1 - depth, -22}, {-7, 1 - depth, -22}});
            entrance = new BlockPos(0, 1, 39);
            branches = List.of(new BlockPos(15, 1 - depth / 2, 1), new BlockPos(-15, 1 - depth / 2, 1));
        }
        // Mirroring is applied at lookup. All instances share one generated mesh per descent depth.
        shape = SHAPES.computeIfAbsent(this.expandedSpider ? -2 : spider ? -1 : witherDepth, unused -> buildShape());
    }

    private BlockPos local(int x, int y, int z) { return new BlockPos(x * sx, y, z * sz); }
    private void addResidents(int[][] positions) {
        for (int[] pos : positions) residents.add(local(pos[0], pos[1], pos[2]));
    }
    private void path(int x1, int f1, int z1, int x2, int f2, int z2, int radius, int height) {
        passages.add(new Passage(new Point(x1, f1, z1), new Point(x2, f2, z2), radius, height, false));
    }
    private void room(int x, int floor, int z, int radius, int height) {
        var point = new Point(x, floor, z);
        passages.add(new Passage(point, point, radius, height, true));
    }
    public List<BlockPos> residents() { return List.copyOf(residents); }
    public BlockPos entrance() { return entrance; }
    public List<BlockPos> branches() { return branches; }
    public List<BlockPos> lootPositions() {
        if (expandedSpider) return SpiderCaveLayout.lootPositions().stream()
                .map(pos -> local(pos.getX(), pos.getY(), pos.getZ())).toList();
        // Keep two blocks between the boss-room chest and the wide spider
        // helper at x=-35; adjacent blocks overlap their actual collision boxes.
        return spider ? List.of(local(36, -13, -10), local(-10, -13, 36),
                local(9, -21, -38), local(-38, -21, 9), local(-37, -33, -28))
                : List.of(new BlockPos(18, 1 - witherDepth / 2, 1), new BlockPos(-18, 1 - witherDepth / 2, 1),
                new BlockPos(9, 1 - witherDepth, -24));
    }

    public Column column(int x, int z) {
        if (x < -RADIUS || x > RADIUS || z < -RADIUS || z > RADIUS) return null;
        return shape[(z * sz + RADIUS) * WIDTH + x * sx + RADIUS];
    }

    private Column[] buildShape() {
        Column[] raw = new Column[WIDTH * WIDTH];
        int minY = Integer.MAX_VALUE, maxY = Integer.MIN_VALUE;
        boolean[] fixedInterior = new boolean[raw.length];
        for (int i = 0; i < raw.length; i++) {
            int x = i % WIDTH - RADIUS, z = i / WIDTH - RADIUS;
            raw[i] = rawColumn(x, z);
            if (raw[i] != null) {
                minY = Math.min(minY, raw[i].bottom);
                maxY = Math.max(maxY, raw[i].top);
            }
            // Preserve the boss room's original cardinal wall indentations at every height.
            fixedInterior[i] = !spider && x * x + (z + 22) * (z + 22) <= 15 * 15;
        }
        Column[] result = new Column[raw.length];
        boolean[] shell = new boolean[raw.length], air = new boolean[raw.length];
        for (int y = minY; y <= maxY; y++) {
            for (int i = 0; i < raw.length; i++) {
                var c = raw[i];
                shell[i] = c != null && y >= c.bottom && y <= c.top;
                air[i] = c != null && y > c.floor && y <= c.ceiling;
            }
            if (!spider) trimTips(shell, null);
            trimTips(air, fixedInterior);
            // A solid cell with air on three sides is a lone jutting step or wall tooth.
            boolean filled;
            do {
                filled = false;
                for (int i = WIDTH + 1; i < raw.length - WIDTH - 1; i++) {
                    if (shell[i] && !air[i] && !fixedInterior[i] && neighbors(air, i) >= 3) {
                        air[i] = true;
                        filled = true;
                    }
                }
            } while (filled);
            for (int i = 0; i < raw.length; i++) {
                if (!shell[i]) continue;
                Column previous = result[i];
                int bottom = previous == null ? y : previous.bottom;
                int floor = previous == null ? Integer.MAX_VALUE : previous.floor;
                int ceiling = previous == null ? Integer.MIN_VALUE : previous.ceiling;
                if (air[i]) {
                    floor = Math.min(floor, y - 1);
                    ceiling = y;
                }
                result[i] = new Column(bottom, y, floor, ceiling);
            }
        }
        if (expandedSpider) {
            for (int z = -RADIUS; z <= RADIUS; z++) for (int x = -RADIUS; x <= RADIUS; x++) {
                if (!SpiderCaveLayout.isFarmPit(x, z)) continue;
                int index = (z + RADIUS) * WIDTH + x + RADIUS;
                var column = result[index];
                result[index] = new Column(column.bottom() - 2, column.top(), column.floor() - 2, column.ceiling());
            }
        }
        return result;
    }

    private static void trimTips(boolean[] cells, boolean[] fixed) {
        boolean trimmed;
        do {
            trimmed = false;
            for (int i = WIDTH + 1; i < cells.length - WIDTH - 1; i++) {
                if (cells[i] && (fixed == null || !fixed[i]) && neighbors(cells, i) < 2) {
                    cells[i] = false;
                    trimmed = true;
                }
            }
        } while (trimmed);
    }

    private static int neighbors(boolean[] cells, int i) {
        return (cells[i - 1] ? 1 : 0) + (cells[i + 1] ? 1 : 0)
                + (cells[i - WIDTH] ? 1 : 0) + (cells[i + WIDTH] ? 1 : 0);
    }

    private Column rawColumn(int x, int z) {
        int bottom = Integer.MAX_VALUE, top = Integer.MIN_VALUE, floor = Integer.MAX_VALUE, ceiling = Integer.MIN_VALUE;
        double nearestTunnel = Double.POSITIVE_INFINITY;
        int tunnelFloor = Integer.MAX_VALUE;
        for (Passage passage : passages) {
            double dx = passage.to.x - passage.from.x, dz = passage.to.z - passage.from.z;
            double t = dx == 0 && dz == 0 ? 0 : Math.max(0, Math.min(1,
                    ((x - passage.from.x) * dx + (z - passage.from.z) * dz) / (dx * dx + dz * dz)));
            double distance = Math.hypot(x - passage.from.x - t * dx, z - passage.from.z - t * dz);
            if (distance > passage.radius + 2) continue;
            int f = (int) Math.floor(passage.from.floor + t * (passage.to.floor - passage.from.floor));
            int h = passage.room ? Math.max(3, (int) Math.floor(passage.height * Math.sqrt(Math.max(0,
                    1 - distance * distance / (passage.radius * passage.radius))))) : passage.height;
            bottom = Math.min(bottom, f - 2);
            // The radius14/height14 Wither dome drops by three blocks between
            // a few adjacent columns. Three roof layers seal those steep steps;
            // two leave single-block windows above the lower column's shell.
            int roof = !spider && passage.room ? 3 : 2;
            // Each of the four Wither rooms rounds up at exactly its center,
            // leaving a lone top block. Two layers seal those flat center cells;
            // retain three at the steep roof edges where the extra layer is needed.
            if (!spider && passage.room && distance < .001) roof = 2;
            top = Math.max(top, f + h + roof);
            if (distance <= passage.radius) {
                if (spider && !passage.room) {
                    // Rounded caps overlap at every turn. Taking their lowest
                    // floor cuts off the preceding ramp's last few steps.
                    if (distance < nearestTunnel) {
                        nearestTunnel = distance;
                        tunnelFloor = f;
                    }
                } else floor = Math.min(floor, f);
                ceiling = Math.max(ceiling, f + h);
            }
        }
        floor = Math.min(floor, tunnelFloor);
        if (spider && !expandedSpider && (Math.abs(x + 28) == 13 && Math.abs(z + 28) <= 1
                || Math.abs(z + 28) == 13 && Math.abs(x + 28) <= 1)) {
            // The three center columns of each cardinal wall ended one block
            // higher than their neighbors, creating the four recessed notches.
            ceiling = Math.min(ceiling, -31);
        }
        if (spider && bottom != Integer.MAX_VALUE && x + z >= 108 && Math.abs(x - z) <= 5) {
            // Open the rounded first tunnel's cap into the existing hill mouth.
            floor = Math.min(floor, 0);
            ceiling = Math.max(ceiling, 6);
        }
        return bottom == Integer.MAX_VALUE ? null : new Column(bottom, top, floor, ceiling);
    }
}

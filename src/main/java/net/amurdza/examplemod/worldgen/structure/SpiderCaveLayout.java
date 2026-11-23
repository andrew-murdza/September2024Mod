package net.amurdza.examplemod.worldgen.structure;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

import java.util.ArrayList;
import java.util.List;

/** Unmirrored authored plan shared by cave geometry, decoration and the finite resident roster. */
public final class SpiderCaveLayout {
    public enum Theme { SLIME, MUSHROOM, GARDEN, BROOD, LARDER, QUEEN }
    public record Chamber(BlockPos center, int radius, int height, Theme theme) { }
    public record Resident(String id, BlockPos pos) { }

    private static final List<Chamber> CHAMBERS = List.of(
            new Chamber(new BlockPos(32, -14, -10), 12, 11, Theme.SLIME),
            new Chamber(new BlockPos(-10, -14, 32), 12, 11, Theme.GARDEN),
            new Chamber(new BlockPos(5, -22, -38), 12, 12, Theme.MUSHROOM),
            new Chamber(new BlockPos(-38, -22, 5), 12, 11, Theme.LARDER),
            new Chamber(new BlockPos(0, -14, 0), 12, 11, Theme.BROOD),
            new Chamber(new BlockPos(-28, -34, -28), 15, 13, Theme.QUEEN));
    private static final List<BlockPos> LOOT = List.of(
            new BlockPos(43, -13, -10), new BlockPos(-10, -13, 43),
            new BlockPos(5, -21, -49), new BlockPos(-49, -21, 5),
            new BlockPos(0, -13, -11), new BlockPos(-14, -33, -28));
    private static final List<Direction> CHEST_FACINGS = List.of(
            Direction.WEST, Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.SOUTH, Direction.WEST);
    private static final List<Resident> RESIDENTS = createResidents();

    private SpiderCaveLayout() { }

    public static List<Chamber> chambers() { return CHAMBERS; }
    public static List<Resident> residents() { return RESIDENTS; }
    public static List<BlockPos> lootPositions() { return LOOT; }

    /** Chests sit against their chamber wall and open toward its interior. */
    public static Direction chestFacing(BlockPos local) {
        int index = LOOT.indexOf(local);
        if (index < 0) throw new IllegalArgumentException("Unknown spider chest: " + local);
        return CHEST_FACINGS.get(index);
    }

    /** Two two-block-deep livestock pits west of the larder's continuous north/south route. */
    public static boolean isFarmPit(int x, int z) {
        return x >= -46 && x <= -40 && x != -43 && z >= 2 && z <= 8;
    }

    /** Leave spawn pads, chest access and a cross through each chamber free of solid ornaments. */
    public static boolean reserved(int x, int z) {
        for (var resident : RESIDENTS) {
            int margin = resident.id().equals("aoemod:spider_queen") ? 2 : 1;
            if (Math.abs(x - resident.pos().getX()) <= margin
                    && Math.abs(z - resident.pos().getZ()) <= margin) return true;
        }
        for (var chest : LOOT) {
            if (Math.abs(x - chest.getX()) <= 1 && Math.abs(z - chest.getZ()) <= 1) return true;
        }
        for (var chamber : CHAMBERS) {
            int dx = x - chamber.center().getX(), dz = z - chamber.center().getZ();
            if (dx * dx + dz * dz <= chamber.radius() * chamber.radius()
                    && (Math.abs(dx) <= 1 || Math.abs(dz) <= 1)) return true;
        }
        return false;
    }

    private static List<Resident> createResidents() {
        var result = new ArrayList<Resident>();
        result.add(new Resident("aoemod:spider_queen", new BlockPos(-28, -33, -28)));
        for (var chamber : CHAMBERS) {
            if (chamber.theme() == Theme.LARDER) {
                // Keep all guards out of the recessed western farming pit.
                int[][] guards = {{-1, -8}, {3, -7}, {6, -5}, {8, -1}, {8, 3}, {5, 6}, {1, 8}, {-3, 7}};
                for (int i = 0; i < guards.length; i++) {
                    result.add(new Resident(i % 2 == 0 ? "minecraft:spider" : "minecraft:cave_spider",
                            chamber.center().offset(guards[i][0], 1, guards[i][1])));
                }
                continue;
            }
            int radius = chamber.theme() == Theme.QUEEN ? 10 : 8;
            for (int i = 0; i < 12; i++) {
                double angle = i * Math.PI / 6;
                String id = chamber.theme() == Theme.SLIME
                        ? (i % 2 == 0 ? "minecraft:slime" : "twilightforest:slime_beetle")
                        : switch (i) { case 8, 9 -> "aoemod:bogged"; case 10 -> "minecraft:slime";
                            case 11 -> "twilightforest:slime_beetle";
                            default -> i % 2 == 0 ? "minecraft:spider" : "minecraft:cave_spider"; };
                result.add(new Resident(id, chamber.center().offset((int) Math.round(Math.cos(angle) * radius),
                        1, (int) Math.round(Math.sin(angle) * radius))));
            }
        }
        for (int x : new int[]{-45, -41}) for (int z : new int[]{3, 6})
            result.add(new Resident("aoemod:arthropleura", new BlockPos(x, -23, z)));
        return List.copyOf(result);
    }
}
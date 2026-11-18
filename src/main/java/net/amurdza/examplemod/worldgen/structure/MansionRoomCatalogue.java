package net.amurdza.examplemod.worldgen.structure;

import net.minecraft.util.RandomSource;

import java.util.List;
import java.util.Set;

/** The requested native rooms, shared between floors without mixing doorway orientations. */
public final class MansionRoomCatalogue {
    public static final List<String> SMALL = List.of("1x1_a3", "1x1_b2", "1x1_b3");
    public static final List<String> GROUND_SMALL = List.of("1x1_a1", "1x1_a3", "1x1_a5", "1x1_b2", "1x1_b3");
    public static final List<String> SIDE = List.of("1x2_c1", "1x2_c3", "1x2_c4");
    public static final List<String> FRONT = List.of("1x2_d1");
    // The sole map room is reserved explicitly by the residential planner.
    public static final List<String> LARGE = List.of("2x2_b1", "2x2_b2", "2x2_b3");
    public static final Set<String> NORMAL = Set.of("1x1_a1", "1x1_a3", "1x1_a5", "1x1_b2", "1x1_b3",
            "1x2_c1", "1x2_c3", "1x2_c4", "1x2_d1", "2x2_b1", "2x2_b2", "2x2_b3", "2x2_b4");
    public static final String STAIRS = "1x2_d_stairs";
    public static final String FALLBACK = "1x1_as4";
    public static final String MAP_ROOM = "aoe_v6_2x2_b4";
    public static final String BEDROOM_LARGE = "aoe_v6_2x2_b3";
    public static final String ENTRANCE = "aoe_v6_entrance";
    public static final String ENTRANCE_PLATFORM = "aoe_v6_entrance_platform";
    public static final net.minecraft.core.BlockPos ENTRANCE_PLATFORM_OFFSET = new net.minecraft.core.BlockPos(-2, 0, -4);

    private MansionRoomCatalogue() { }

    public static String replacement(String nativeName, boolean firstFloor, RandomSource random) {
        if (nativeName.endsWith("_stairs")) return compact(STAIRS);
        // A native enclosed one-cell pocket has no authored doorway. Keep the
        // explicitly allowed plain pillar room there; RoomAccess opens it later.
        if (nativeName.startsWith("1x1_as")) return compact(FALLBACK);
        if (nativeName.startsWith("1x1_")) return compact(choose(firstFloor ? GROUND_SMALL : SMALL, random));
        if (nativeName.startsWith("1x2_a") || nativeName.startsWith("1x2_c")) return compact(choose(SIDE, random));
        if (nativeName.startsWith("1x2_")) return compact(choose(FRONT, random));
        if (nativeName.startsWith("2x2_")) return compact(choose(LARGE, random));
        return switch (nativeName) {
            case "indoors_wall_2" -> "indoors_wall_1";
            case "indoors_door_2" -> "indoors_door_1";
            case "carpet_south_2" -> "carpet_south_1";
            case "carpet_west_2" -> "carpet_west_1";
            case "wall_flat" -> "wall_window";
            case "entrance", "roof", "roof_front", "roof_corner", "roof_inner_corner" -> compact(nativeName);
            default -> nativeName;
        };
    }

    public static String compact(String nativeName) {
        return nativeName.equals("entrance") ? "aoe_compact_entrance_v2" : "aoe_compact_" + nativeName;
    }
    public static String nativeName(String name) {
        if (name.startsWith("aoe_plain_")) name = name.substring("aoe_plain_".length());
        if (name.startsWith("aoe_v6_")) return name.substring("aoe_v6_".length());
        if (name.equals("aoe_compact_entrance_v2")) return "entrance";
        if (name.equals("aoe_compact_1x2_d1_v3")) return "1x2_d1";
        if (name.equals("aoe_compact_1x2_c4_v4")) return "1x2_c4";
        if (name.equals("aoe_compact_wall_corner_v3")) return "wall_corner";
        if (name.startsWith("aoe_compact_roof") && name.endsWith("_v3")) return name.substring("aoe_compact_".length(), name.length() - 3);
        return name.startsWith("aoe_compact_") ? name.substring("aoe_compact_".length()).replaceFirst("_v[0-9]+$", "") : name;
    }

    /** New pieces use separate oak-only resources; saved older identities stay intact. */
    public static String newTemplate(String name) {
        String nativeName = nativeName(name);
        if (nativeName.equals("1x1_b4")) nativeName = "1x1_b3";
        return "aoe_v6_" + nativeName;
    }

    /** The requested effective count: a two-wide bed counts one, a three-wide bed two. */
    public static int bedCapacity(String name) {
        return switch (nativeName(name)) {
            case "1x1_b3" -> 1;
            case "1x2_c3" -> 3;
            case "1x2_d1" -> 2;
            case "2x2_b3" -> name.startsWith("aoe_v6_") ? 8 : 0;
            default -> 0;
        };
    }

    private static String choose(List<String> choices, RandomSource random) { return choices.get(random.nextInt(choices.size())); }
}

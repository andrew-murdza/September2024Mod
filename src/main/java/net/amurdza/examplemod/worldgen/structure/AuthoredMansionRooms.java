package net.amurdza.examplemod.worldgen.structure;

import com.google.gson.JsonParser;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraftforge.registries.ForgeRegistries;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/** Bounded block-state captures from the player's November15 basement; no inventories or entities are imported. */
final class AuthoredMansionRooms {
    private static final Map<String, BlockState[]> ROOMS = load();
    private AuthoredMansionRooms() { }

    static BlockState state(String room, int depth, int y, int across) {
        if (depth < -1 || depth > 11 || across < -1 || across > 11 || y < -1 || y > 6) return Blocks.AIR.defaultBlockState();
        return ROOMS.get(room)[(y + 1) * 169 + (across + 1) * 13 + depth + 1];
    }

    private static Map<String, BlockState[]> load() {
        String path = "/data/aoemod/structure_data/mansion_basement_rooms_v4.json";
        try (var input = AuthoredMansionRooms.class.getResourceAsStream(path)) {
            if (input == null) throw new IllegalStateException("Missing authored basement room capture");
            var data = JsonParser.parseReader(new InputStreamReader(input, StandardCharsets.UTF_8)).getAsJsonObject();
            var palette = new java.util.ArrayList<BlockState>();
            for (var element : data.getAsJsonArray("palette")) {
                var entry = element.getAsJsonObject();
                var block = ForgeRegistries.BLOCKS.getValue(new ResourceLocation(entry.get("Name").getAsString()));
                if (block == null) throw new IllegalStateException("Missing captured block: " + entry.get("Name"));
                BlockState state = block.defaultBlockState();
                if (entry.has("Properties")) for (var property : entry.getAsJsonObject("Properties").entrySet()) {
                    var definition = block.getStateDefinition().getProperty(property.getKey());
                    if (definition == null) throw new IllegalStateException("Missing captured property: " + property.getKey());
                    state = property(state, definition, property.getValue().getAsString());
                }
                palette.add(state);
            }
            var rooms = new HashMap<String, BlockState[]>();
            for (var entry : data.getAsJsonObject("rooms").entrySet()) {
                var room = entry.getValue().getAsJsonObject();
                var blocks = room.getAsJsonArray("blocks");
                if (blocks.size() != 13 * 13 * 8) throw new IllegalStateException("Invalid captured room size: " + entry.getKey());
                BlockState[] states = new BlockState[blocks.size()];
                for (int i = 0; i < states.length; i++) {
                    BlockState state = palette.get(blocks.get(i).getAsInt());
                    states[i] = switch (room.get("transform").getAsString()) {
                        case "rotate_180" -> state.rotate(Rotation.CLOCKWISE_180);
                        case "rotate_90" -> state.rotate(Rotation.CLOCKWISE_90);
                        case "mirror_z" -> state.mirror(Mirror.LEFT_RIGHT);
                        default -> throw new IllegalStateException("Invalid captured room transform");
                    };
                }
                rooms.put(entry.getKey(), states);
            }
            return Map.copyOf(rooms);
        } catch (java.io.IOException failure) {
            throw new IllegalStateException("Cannot read the authored basement", failure);
        }
    }

    private static <T extends Comparable<T>> BlockState property(BlockState state, Property<T> property, String value) {
        return state.setValue(property, property.getValue(value).orElseThrow());
    }
}

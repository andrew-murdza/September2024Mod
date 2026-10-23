package net.amurdza.examplemod.portal;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.Map;

/** Stored in the Overworld data directory, shared by all dimensions and preserved across restarts. */
public final class PortalLinks extends SavedData {
    private final Map<String, Endpoint> links = new HashMap<>();

    public static PortalLinks get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(PortalLinks::load, PortalLinks::new,
                "aoemod_wooden_portals");
    }

    public Endpoint target(Endpoint source) {
        return links.get(source.key());
    }

    public void connect(Endpoint source, Endpoint target) {
        links.put(source.key(), target);
        links.put(target.key(), source);
        setDirty();
    }

    public void disconnect(Endpoint endpoint) {
        links.remove(endpoint.key());
        links.entrySet().removeIf(entry -> entry.getValue().equals(endpoint));
        setDirty();
    }

    public static PortalLinks load(CompoundTag tag) {
        PortalLinks data = new PortalLinks();
        ListTag entries = tag.getList("Links", Tag.TAG_COMPOUND);
        for (int i = 0; i < entries.size(); i++) {
            CompoundTag entry = entries.getCompound(i);
            Endpoint endpoint = Endpoint.load(entry.getCompound("Target"));
            if (endpoint != null) {
                data.links.put(entry.getString("Source"), endpoint);
            }
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag entries = new ListTag();
        links.forEach((key, target) -> {
            CompoundTag entry = new CompoundTag();
            entry.putString("Source", key);
            entry.put("Target", target.save());
            entries.add(entry);
        });
        tag.put("Links", entries);
        return tag;
    }

    public record Endpoint(ResourceKey<Level> dimension, PortalFrame frame) {
        public String key() {
            return dimension.location() + "|" + frame.bottomLeft().asLong() + "|" + frame.axis().getName();
        }

        public CompoundTag save() {
            CompoundTag tag = new CompoundTag();
            tag.putString("Dimension", dimension.location().toString());
            tag.putLong("Position", frame.bottomLeft().asLong());
            tag.putString("Axis", frame.axis().getName());
            tag.putInt("Width", frame.width());
            tag.putInt("Height", frame.height());
            return tag;
        }

        public static Endpoint load(CompoundTag tag) {
            ResourceLocation id = ResourceLocation.tryParse(tag.getString("Dimension"));
            if (id == null || (!tag.getString("Axis").equals("x") && !tag.getString("Axis").equals("z"))) {
                return null;
            }
            ResourceKey<Level> dimension = ResourceKey.create(Registries.DIMENSION, id);
            if (PortalDestination.fromDimension(dimension).isEmpty()) {
                return null;
            }
            try {
                return new Endpoint(dimension, new PortalFrame(BlockPos.of(tag.getLong("Position")),
                        Direction.Axis.byName(tag.getString("Axis")), tag.getInt("Width"), tag.getInt("Height")));
            } catch (IllegalArgumentException exception) {
                return null;
            }
        }
    }
}

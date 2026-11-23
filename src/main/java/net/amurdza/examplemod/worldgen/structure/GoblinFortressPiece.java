package net.amurdza.examplemod.worldgen.structure;

import net.amurdza.examplemod.registry.ModStructures;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.UUID;

/** A chunk-clipped fortress with one saved encounter and an immutable resident plan. */
public final class GoblinFortressPiece extends StructurePiece {
    public static final String ENCOUNTER_KIND = "goblin_fortress";
    public static final int DEFENDER_COUNT = 193;
    public static final List<String> HOSTILES = List.of("twilightforest:upper_goblin_knight",
            "twilightforest:blockchain_goblin", "twilightforest:redcap", "minecraft:stray",
            "aoemod:breeze", "twilightforest:yeti");
    private BlockPos origin;
    private final Direction inward;
    private final UUID encounter;
    private final long seed;
    private final BitSet lootPlaced = new BitSet();
    private List<GoblinFortressSpawns.Plan> residentPlan;

    public GoblinFortressPiece(BlockPos origin, Direction inward, long seed) {
        super(ModStructures.GOBLIN_FORTRESS_PIECE.get(), 0, GoblinFortressLayout.bounds(origin, inward));
        this.origin = origin.immutable(); this.inward = inward; this.seed = seed;
        encounter = new UUID(seed, seed ^ origin.asLong() ^ 0x474f424c494eL);
    }

    public GoblinFortressPiece(StructurePieceSerializationContext context, CompoundTag tag) {
        super(ModStructures.GOBLIN_FORTRESS_PIECE.get(), tag);
        origin = BlockPos.of(tag.getLong("Origin")); seed = tag.getLong("Seed");
        inward = Direction.from2DDataValue(tag.getInt("Inward")); encounter = tag.getUUID("Encounter");
        lootPlaced.or(BitSet.valueOf(tag.getLongArray("LootPlaced")));
        boundingBox = GoblinFortressLayout.bounds(origin, inward);
    }

    public UUID encounterId() { return encounter; }
    public BlockPos origin() { return origin; }
    public Direction inward() { return inward; }
    public BlockPos world(BlockPos local) { return GoblinFortressLayout.transform(origin, inward, local); }
    public BlockPos world(int x, int y, int z) { return world(new BlockPos(x, y, z)); }

    @Override protected synchronized void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
        tag.putLong("Origin", origin.asLong()); tag.putLong("Seed", seed);
        tag.putInt("Inward", inward.get2DDataValue()); tag.putUUID("Encounter", encounter);
        tag.putLongArray("LootPlaced", lootPlaced.toLongArray());
    }

    @Override public void move(int x, int y, int z) {
        super.move(x, y, z); origin = origin.offset(x, y, z); residentPlan = null;
    }

    public void registerBoundary(ServerLevel level) {
        AuthoredEncounterProgress.get(level).register(encounter, origin, DEFENDER_COUNT, ENCOUNTER_KIND);
        BossStructureBounds.get(level).record(ENCOUNTER_KIND + ":" + encounter, AABB.of(boundingBox), encounter);
    }

    private BoundingBox roomBounds(GoblinFortressLayout.Room room) {
        var a = world(room.minX() + 1, room.floor() + 1, room.minZ() + 1);
        var b = world(room.maxX() - 1, room.floor() + 8, room.maxZ() - 1);
        return BoundingBox.fromCorners(a, b);
    }

    public synchronized List<GoblinFortressSpawns.Plan> residents() {
        if (residentPlan != null) return residentPlan;
        var result = new ArrayList<GoblinFortressSpawns.Plan>();
        var iceRooms = GoblinFortressLayout.rooms().stream()
                .filter(r -> r.name().equals("yeti_gallery") || r.name().equals("ice_store")).toList();
        var rooms = GoblinFortressLayout.rooms().stream()
                .filter(r -> !r.name().equals("mooshrooms") && !iceRooms.contains(r)).toList();
        for (int index = 0; index < DEFENDER_COUNT - 1; index++) {
            boolean yeti = index % HOSTILES.size() == 5;
            int ordinal = yeti ? index / HOSTILES.size() : index;
            var choices = yeti ? iceRooms : rooms;
            var room = choices.get(ordinal % choices.size());
            var slots = GoblinFortressLayout.spawnPositions(room);
            if (slots.isEmpty()) throw new IllegalStateException("No clear resident floor in " + room.name());
            var local = slots.get((ordinal / choices.size()) % slots.size());
            result.add(new GoblinFortressSpawns.Plan(encounter, origin, index, HOSTILES.get(index % HOSTILES.size()),
                    world(local), roomBounds(room), false, false));
        }
        result.add(new GoblinFortressSpawns.Plan(encounter, origin, DEFENDER_COUNT - 1, "twilightforest:alpha_yeti",
                world(GoblinFortressLayout.boss()), BoundingBox.fromCorners(world(-13, 1, 63), world(13, 15, 90)), false, false));
        int index = DEFENDER_COUNT;
        for (var pen : GoblinFortressLayout.pens()) {
            String type = switch (pen.name()) {
                case "dappled_yaks" -> "aoemod:yak";
                case "dappled_foxes", "snowy_foxes" -> "minecraft:fox";
                case "dappled_rabbits", "snowy_rabbits" -> "minecraft:rabbit";
                case "snowy_goats" -> "minecraft:goat";
                case "snowy_reindeer" -> "biodiverse:reindeer";
                case "snowy_polar_bears" -> "minecraft:polar_bear";
                case "mooshrooms" -> "minecraft:mooshroom";
                default -> throw new IllegalStateException("Unknown livestock pen " + pen.name());
            };
            var slots = GoblinFortressLayout.spawnPositions(pen);
            for (int animal = 0; animal < Math.min(4, slots.size()); animal++) {
                result.add(new GoblinFortressSpawns.Plan(encounter, origin, index++, type,
                        world(slots.get(animal)), roomBounds(pen), true, false));
            }
        }
        // The sheltered outside-wall fishery is part of the same protected settlement.
        for (int fish = 0; fish < 6; fish++) {
            int side = fish % 2 == 0 ? -1 : 1;
            result.add(new GoblinFortressSpawns.Plan(encounter, origin, index++, "minecraft:salmon",
                    world(side * 23, -2, -82 + (fish / 2) * 7),
                    BoundingBox.fromCorners(world(side * 12, -4, -86), world(side * 34, -1, -63)), true, true));
        }
        residentPlan = List.copyOf(result);
        return residentPlan;
    }

    @Override public void postProcess(WorldGenLevel level, StructureManager manager, ChunkGenerator generator,
            RandomSource random, BoundingBox clip, ChunkPos chunk, BlockPos pivot) {
        // Each column belongs to this chunk. Furnishings are selected by local coordinates, independent of visit order.
        for (int x = -80; x <= 80; x++) for (int z = -92; z <= 92; z++) {
            var pos = world(x, 0, z);
            if (pos.getX() >= clip.minX() && pos.getX() <= clip.maxX()
                    && pos.getZ() >= clip.minZ() && pos.getZ() <= clip.maxZ())
                GoblinFortressLayout.placeColumn(level, clip, origin, inward, x, z);
        }
        for (int index = 0; index < GoblinFortressLayout.lootPositions().size(); index++) {
            var local = GoblinFortressLayout.lootPositions().get(index);
            var pos = world(local);
            if (!clip.isInside(pos)) continue;
            if (!(level.getBlockEntity(pos) instanceof ChestBlockEntity))
                level.setBlock(pos, Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, inward.getOpposite()), 2);
            synchronized (this) {
                if (!lootPlaced.get(index) && level.getBlockEntity(pos) instanceof ChestBlockEntity chest) {
                    chest.setLootTable(new ResourceLocation("aoemod:chests/yeti_lair"), seed ^ pos.asLong());
                    lootPlaced.set(index);
                }
            }
        }
        var plans = residents().stream().filter(plan -> clip.isInside(plan.preferred())).toList();
        var server = level.getLevel();
        Runnable register = () -> {
            registerBoundary(server);
            for (var plan : plans) GoblinFortressSpawns.get(server).plan(server, plan);
        };
        if (level instanceof ServerLevel && server.getServer().isSameThread()) register.run();
        else server.getServer().execute(register);
    }
}

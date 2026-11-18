package net.amurdza.examplemod.worldgen.structure;

import com.blackgear.vanillabackport.common.level.entities.decoration.Cushion;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.BitSet;

/** Archived version-four cubic village geometry for partially generated saved camps. */
public final class LegacySavannaOrcVillageV4 {
    public static final int CENTER = 62, RADIUS = 60, SIZE = 125, MARGIN = 20;
    private static final int[][] DISTANCES = distances();
    private static final int[][] SEATS = {{-3,-4},{-3,4},{3,4},{0,-2},{-2,0},{0,2},{2,0},{3,-4}};
    private static final String[] ANIMALS = {"alexsmobs:bison", "minecraft:sheep", "alexsmobs:gazelle", "biodiverse:oryx",
            "alexsmobs:emu", "alexsmobs:kangaroo", "twilightforest:boar", "biodiverse:wildebeest"};

    private LegacySavannaOrcVillageV4() { }

    public static boolean contains(int x, int z) {
        long dx = Math.abs((long) x - CENTER), dz = Math.abs((long) z - CENTER);
        if (dx > RADIUS || dz > RADIUS) return false;
        return dx * dx * dx + dz * dz * dz <= (long) RADIUS * RADIUS * RADIUS;
    }

    public static BoundingBox bounds(BlockPos origin) {
        return new BoundingBox(origin.getX() - MARGIN, origin.getY() - 14, origin.getZ() - MARGIN,
                origin.getX() + SIZE - 1 + MARGIN, origin.getY() + 17, origin.getZ() + SIZE - 1 + MARGIN);
    }

    private static int[][] distances() {
        int side = SIZE + 2 * MARGIN;
        int[][] result = new int[side][side];
        for (int x = 0; x < side; x++) for (int z = 0; z < side; z++) {
            int d = contains(x - MARGIN, z - MARGIN) ? 0 : side * 2;
            if (x > 0) d = Math.min(d, result[x - 1][z] + 1);
            if (z > 0) d = Math.min(d, result[x][z - 1] + 1);
            result[x][z] = d;
        }
        for (int x = side - 1; x >= 0; x--) for (int z = side - 1; z >= 0; z--) {
            if (x + 1 < side) result[x][z] = Math.min(result[x][z], result[x + 1][z] + 1);
            if (z + 1 < side) result[x][z] = Math.min(result[x][z], result[x][z + 1] + 1);
        }
        return result;
    }

    public static void placeGround(WorldGenLevel level, BoundingBox chunk, BlockPos origin) {
        int floor = origin.getY() + 2;
        var generator = level.getLevel().getChunkSource().getGenerator();
        var randomState = level.getLevel().getChunkSource().randomState();
        for (int x = Math.max(-MARGIN, chunk.minX() - origin.getX()); x < Math.min(SIZE + MARGIN, chunk.maxX() - origin.getX() + 1); x++) {
            for (int z = Math.max(-MARGIN, chunk.minZ() - origin.getZ()); z < Math.min(SIZE + MARGIN, chunk.maxZ() - origin.getZ() + 1); z++) {
                int distance = DISTANCES[x + MARGIN][z + MARGIN];
                boolean path = (Math.abs(x - CENTER) <= 3 || Math.abs(z - CENTER) <= 3);
                int rampStep = path ? Math.max(0, Math.max(Math.abs(x - CENTER), Math.abs(z - CENTER)) - (RADIUS - 1)) : distance;
                if ((path ? rampStep : distance) > MARGIN) continue;
                int wx = origin.getX() + x, wz = origin.getZ() + z;
                int natural = generator.getBaseHeight(wx, wz, Heightmap.Types.WORLD_SURFACE_WG, level, randomState) - 1;
                int surface = natural;
                if (path && rampStep > 0) {
                    // All seven cells of a tread use the same height. The cubic
                    // boundary's single extra center cell must not form a spine.
                    for (int across = -3; across <= 3; across++) {
                        int px = Math.abs(x - CENTER) <= 3 ? origin.getX() + CENTER + across : wx;
                        int pz = Math.abs(z - CENTER) <= 3 ? origin.getZ() + CENTER + across : wz;
                        surface = Math.max(surface, generator.getBaseHeight(px, pz, Heightmap.Types.WORLD_SURFACE_WG, level, randomState) - 1);
                    }
                }
                int top = rampStep == 0 ? floor : Math.max(surface, floor - rampStep);
                if (distance > 0 && !path && top <= natural) continue;
                if (distance > 0 || path && rampStep > 0) {
                    level.setBlock(new BlockPos(wx, top, wz), (path ? Blocks.DIRT_PATH : Blocks.GRASS_BLOCK).defaultBlockState(), 2);
                    for (int y = top + 1; y <= floor + 3; y++) level.setBlock(new BlockPos(wx, y, wz), Blocks.AIR.defaultBlockState(), 2);
                }
                boolean wet = level.getFluidState(new BlockPos(wx, top, wz)).is(net.minecraft.tags.FluidTags.WATER);
                for (int depth = 1; depth <= 14; depth++) {
                    var material = wet || depth > 4 ? Blocks.MUD : depth <= 2 ? Blocks.ROOTED_DIRT : Blocks.DIRT;
                    level.setBlock(new BlockPos(wx, top - depth, wz), material.defaultBlockState(), 2);
                }
            }
        }
        // The template contains the player's complete two crop patterns, including
        // mature multi-block corn, pumpkins and submerged cane; do not repaint it.
    }

    public static void populate(WorldGenLevel level, BoundingBox chunk, BlockPos origin,
                                BitSet residents, BitSet cushions, BitSet animals) {
        int house = 0;
        for (int cx : new int[]{-21, 0, 21}) for (int cz : new int[]{-21, 0, 21}) {
            for (int i = 0; i < 4; i++) {
                int index = house * 4 + i;
                BlockPos pos = origin.offset(CENTER + cx + (i % 2 == 0 ? -2 : 2), 3, CENTER + cz + (i < 2 ? -2 : 2));
                String id = house == 4 && i == 0 ? "orcz:bigorcboss" : i % 2 == 0 ? "orcz:orcshield" : "orcz:orcsavagemace";
                if (!residents.get(index) && chunk.isInside(pos) && BossLairPiece.spawn(level, id, pos)) residents.set(index);
            }
            for (int i = 0; i < SEATS.length; i++) {
                int index = house * SEATS.length + i;
                BlockPos pos = origin.offset(CENTER + cx + SEATS[i][0], 3, CENTER + cz + SEATS[i][1]);
                if (cushions.get(index) || !chunk.isInside(pos)) continue;
                var type = ForgeRegistries.ENTITY_TYPES.getValue(new ResourceLocation("minecraft:cushion"));
                if (type == null || !(type.create(level.getLevel()) instanceof Cushion cushion)) continue;
                var shape = level.getBlockState(pos.below()).getShape(level, pos.below());
                if (shape.isEmpty()) continue;
                cushion.setColor(DyeColor.RED);
                cushion.setPos(pos.getX() + .5, pos.getY() - 1 + shape.max(Direction.Axis.Y), pos.getZ() + .5);
                if (level.addFreshEntity(cushion)) cushions.set(index);
            }
            house++;
        }
        int herd = 0;
        for (int sz : new int[]{-1, 1}) for (int sx : new int[]{-1, 1}) for (boolean swap : new boolean[]{false, true}) {
            for (int i = 0; i < 4; i++) {
                int a = i % 2 == 0 ? 46 : 50;
                int b = i < 2 ? 12 : 15;
                BlockPos pos = origin.offset(CENTER + sx * (swap ? b : a), 3, CENTER + sz * (swap ? a : b));
                int index = herd * 4 + i;
                if (!animals.get(index) && chunk.isInside(pos) && BossLairPiece.spawn(level, ANIMALS[herd], pos)) animals.set(index);
            }
            herd++;
        }
    }
}

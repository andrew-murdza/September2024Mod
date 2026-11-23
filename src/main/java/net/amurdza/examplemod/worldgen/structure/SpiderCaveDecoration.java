package net.amurdza.examplemod.worldgen.structure;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HugeMushroomBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

/** Authored cave gardens and farming chambers, written entirely inside the owning chunk. */
public final class SpiderCaveDecoration {
    private SpiderCaveDecoration() { }

    public static void place(WorldGenLevel level, BoundingBox chunk, BlockPos center,
                             long seed, BranchedBossCaves layout) {
        var writer = new Writer(level, chunk, center, seed, layout);
        // All decisions use the unmirrored layout, never an adjacent chunk's current blocks.
        for (var room : SpiderCaveLayout.chambers()) decorateFloor(writer, room);
        for (var room : SpiderCaveLayout.chambers()) decorateChamber(writer, room);
        decorateCeilings(writer);
    }

    private static void decorateFloor(Writer writer, SpiderCaveLayout.Chamber room) {
        int cx = room.center().getX(), cz = room.center().getZ(), floor = room.center().getY();
        for (int dx = -room.radius(); dx <= room.radius(); dx++) {
            for (int dz = -room.radius(); dz <= room.radius(); dz++) {
                int x = cx + dx, z = cz + dz;
                var column = writer.column(x, z);
                if (column == null || !column.hasAir() || dx * dx + dz * dz > room.radius() * room.radius()) continue;
                int y = column.floor();
                if (y != floor && !SpiderCaveLayout.isFarmPit(x, z)) continue;
                int pattern = Math.floorMod(x * 43 + z * 71, 29);
                if (room.theme() == SpiderCaveLayout.Theme.SLIME && dx * dx + dz * dz <= 81) {
                    // The whole central habitat is visible green slime, with moss islands at the doors.
                    if (Math.abs(dx) > 2 || Math.abs(dz) > 2) writer.put(x, y, z, Blocks.SLIME_BLOCK.defaultBlockState());
                } else if (room.theme() == SpiderCaveLayout.Theme.MUSHROOM && pattern < 13) {
                    writer.put(x, y, z, Blocks.MYCELIUM.defaultBlockState());
                } else if (room.theme() == SpiderCaveLayout.Theme.GARDEN && pattern < 17) {
                    writer.put(x, y, z, Blocks.GRASS_BLOCK.defaultBlockState());
                } else if ((room.theme() == SpiderCaveLayout.Theme.BROOD || room.theme() == SpiderCaveLayout.Theme.QUEEN)
                        && dx * dx + dz * dz > (room.radius() - 3) * (room.radius() - 3) && pattern < 9) {
                    writer.put(x, y, z, Blocks.WHITE_WOOL.defaultBlockState());
                }
                if (room.theme() == SpiderCaveLayout.Theme.LARDER) {
                    boolean rim = dx >= -9 && dx <= -1 && dz >= -4 && dz <= 4
                            && (dx == -9 || dx == -1 || Math.abs(dz) == 4);
                    if (rim || dx == -5 && Math.abs(dz) <= 3) writer.put(x, floor, z, Blocks.BONE_BLOCK.defaultBlockState());
                }
                if (SpiderCaveLayout.reserved(x, z) || SpiderCaveLayout.isFarmPit(x, z)) continue;
                if (column.ceiling() - y < 3) continue;
                switch (room.theme()) {
                    case GARDEN -> {
                        Block plant = switch (pattern) {
                            case 0, 1, 2 -> Blocks.FERN;
                            case 3, 4, 5, 6 -> Blocks.GRASS;
                            case 7 -> Blocks.AZALEA;
                            case 8 -> Blocks.FLOWERING_AZALEA;
                            case 9 -> Blocks.DANDELION;
                            case 10 -> Blocks.POPPY;
                            default -> null;
                        };
                        if (plant != null) writer.interior(x, y + 1, z, plant.defaultBlockState());
                    }
                    case MUSHROOM -> {
                        if (pattern < 5) {
                            writer.put(x, y, z, Blocks.MYCELIUM.defaultBlockState());
                            writer.interior(x, y + 1, z, (pattern % 2 == 0 ? Blocks.RED_MUSHROOM : Blocks.BROWN_MUSHROOM).defaultBlockState());
                        }
                        if (pattern == 7 || pattern == 8) {
                            writer.put(x, y, z, Blocks.MOSS_BLOCK.defaultBlockState());
                            writer.interior(x, y + 1, z, Blocks.FERN.defaultBlockState());
                        }
                    }
                    case LARDER -> {
                        if (pattern == 4 && dx > 2) writer.interior(x, y + 1, z, Blocks.BONE_BLOCK.defaultBlockState());
                        if (pattern == 9 && dx > 2) writer.interior(x, y + 1, z, Blocks.WHITE_WOOL.defaultBlockState());
                    }
                    case BROOD, QUEEN -> {
                        if (dx * dx + dz * dz >= (room.radius() - 4) * (room.radius() - 4)) {
                            if (pattern < 3) writer.interior(x, y + 1, z, Blocks.BONE_BLOCK.defaultBlockState());
                            else if (pattern < 7) writer.interior(x, y + 1, z, Blocks.COBWEB.defaultBlockState());
                        }
                    }
                    default -> {
                        if (pattern == 0) {
                            writer.put(x, y, z, Blocks.MOSS_BLOCK.defaultBlockState());
                            writer.interior(x, y + 1, z, Blocks.FERN.defaultBlockState());
                        }
                    }
                }
            }
        }
    }

    private static void decorateChamber(Writer writer, SpiderCaveLayout.Chamber room) {
        if (room.theme() == SpiderCaveLayout.Theme.MUSHROOM) {
            hugeMushroom(writer, room, -4, -4, true);
            hugeMushroom(writer, room, 4, 4, false);
        }
        if (room.theme() == SpiderCaveLayout.Theme.GARDEN) azaleaTree(writer, room, 4, 4);
        if (room.theme() == SpiderCaveLayout.Theme.BROOD || room.theme() == SpiderCaveLayout.Theme.QUEEN) {
            // Hanging silk sacs leave the central fight floor and all authored spawn pads clear.
            for (int[] offset : new int[][]{{-5, -4}, {5, -4}, {-5, 4}, {5, 4}}) {
                int x = room.center().getX() + offset[0], z = room.center().getZ() + offset[1];
                var column = writer.column(x, z);
                if (column == null || !column.hasAir() || column.ceiling() - column.floor() < 7) continue;
                int bottom = column.ceiling() - 2;
                writer.interior(x, bottom, z, Blocks.WHITE_WOOL.defaultBlockState());
                writer.interior(x, bottom + 1, z, Blocks.WHITE_WOOL.defaultBlockState());
                writer.interior(x, bottom + 2, z, Blocks.COBWEB.defaultBlockState());
                writer.interior(x + 1, bottom + 1, z, Blocks.COBWEB.defaultBlockState());
                writer.interior(x - 1, bottom + 1, z, Blocks.COBWEB.defaultBlockState());
            }
        }
    }

    private static void hugeMushroom(Writer writer, SpiderCaveLayout.Chamber room, int dx, int dz, boolean red) {
        int x = room.center().getX() + dx, z = room.center().getZ() + dz, floor = room.center().getY();
        if (!writer.featureFits(x, floor, z, 2, 7)) return;
        writer.put(x, floor, z, Blocks.MYCELIUM.defaultBlockState());
        BlockState stem = Blocks.MUSHROOM_STEM.defaultBlockState()
                .setValue(HugeMushroomBlock.UP, false).setValue(HugeMushroomBlock.DOWN, false);
        for (int y = 1; y <= (red ? 5 : 6); y++) writer.interior(x, floor + y, z, stem);
        if (red) {
            // A curved red cap, with the lower skirt still four blocks above the floor.
            for (int ox = -2; ox <= 2; ox++) for (int oz = -2; oz <= 2; oz++) {
                if (Math.abs(ox) == 2 && Math.abs(oz) == 2) continue;
                int top = Math.max(Math.abs(ox), Math.abs(oz)) == 2 ? 5 : 6;
                writer.interior(x + ox, floor + top, z + oz, cap(Blocks.RED_MUSHROOM_BLOCK, ox, oz, top == 6 ? 1 : 2));
                if (Math.max(Math.abs(ox), Math.abs(oz)) == 2)
                    writer.interior(x + ox, floor + 4, z + oz, cap(Blocks.RED_MUSHROOM_BLOCK, ox, oz, 2));
            }
        } else {
            for (int ox = -2; ox <= 2; ox++) for (int oz = -2; oz <= 2; oz++) {
                if (Math.abs(ox) == 2 && Math.abs(oz) == 2) continue;
                writer.interior(x + ox, floor + 6, z + oz, cap(Blocks.BROWN_MUSHROOM_BLOCK, ox, oz, 2));
            }
        }
    }

    private static BlockState cap(Block block, int x, int z, int radius) {
        return block.defaultBlockState().setValue(HugeMushroomBlock.UP, true).setValue(HugeMushroomBlock.DOWN, false)
                .setValue(HugeMushroomBlock.NORTH, z == -radius || radius > 1 && z == 1 - radius && Math.abs(x) == radius)
                .setValue(HugeMushroomBlock.SOUTH, z == radius || radius > 1 && z == radius - 1 && Math.abs(x) == radius)
                .setValue(HugeMushroomBlock.WEST, x == -radius || radius > 1 && x == 1 - radius && Math.abs(z) == radius)
                .setValue(HugeMushroomBlock.EAST, x == radius || radius > 1 && x == radius - 1 && Math.abs(z) == radius);
    }

    private static void azaleaTree(Writer writer, SpiderCaveLayout.Chamber room, int dx, int dz) {
        int x = room.center().getX() + dx, z = room.center().getZ() + dz, floor = room.center().getY();
        if (!writer.featureFits(x, floor, z, 2, 7)) return;
        writer.put(x, floor, z, Blocks.ROOTED_DIRT.defaultBlockState());
        // Vanilla azalea trees use oak wood; their exclusive azalea canopy identifies the tree.
        for (int y = 1; y <= 4; y++) writer.interior(x, floor + y, z, Blocks.OAK_LOG.defaultBlockState());
        for (int ox = -2; ox <= 2; ox++) for (int oz = -2; oz <= 2; oz++) {
            if (Math.abs(ox) + Math.abs(oz) > 3) continue;
            for (int y = 4; y <= 5; y++) {
                if (ox == 0 && oz == 0 && y == 4) continue;
                var block = Math.floorMod(ox + oz * 3, 3) == 0 ? Blocks.FLOWERING_AZALEA_LEAVES : Blocks.AZALEA_LEAVES;
                writer.interior(x + ox, floor + y, z + oz,
                        block.defaultBlockState().setValue(BlockStateProperties.PERSISTENT, true));
            }
        }
        writer.interior(x, floor + 6, z, Blocks.FLOWERING_AZALEA_LEAVES.defaultBlockState()
                .setValue(BlockStateProperties.PERSISTENT, true));
    }

    private static void decorateCeilings(Writer writer) {
        for (int x = -64; x <= 64; x++) for (int z = -64; z <= 64; z++) {
            var column = writer.column(x, z);
            if (column == null || !column.hasAir() || column.ceiling() - column.floor() < 4 || x + z >= 76) continue;
            // Flush ceiling lights give every gallery light without narrowing its walking width.
            if (Math.floorMod(x + z * 2, 11) == 0 && Math.floorMod(x - z, 4) == 0) {
                Block light = switch (Math.floorMod(Math.floorDiv(x, 4) + Math.floorDiv(z, 4), 3)) {
                    case 0 -> Blocks.OCHRE_FROGLIGHT;
                    case 1 -> Blocks.VERDANT_FROGLIGHT;
                    default -> Blocks.PEARLESCENT_FROGLIGHT;
                };
                writer.put(x, column.ceiling() + 1, z, light.defaultBlockState());
            }
            if (Math.floorMod(x * 3 + z, 13) == 0 && Math.floorMod(x - z, 3) == 0) {
                int bottom = Math.max(column.floor() + 5, column.ceiling() - 2);
                int first = column.ceiling();
                if (bottom > first || !writer.airColumn(x, bottom, first, z)) continue;
                for (int y = first; y >= bottom; y--) {
                    BlockState state = (y == bottom ? Blocks.CAVE_VINES : Blocks.CAVE_VINES_PLANT).defaultBlockState()
                            .setValue(BlockStateProperties.BERRIES, true);
                    // The pack adds waterlogging to cave vines; its default is not necessarily dry.
                    if (state.hasProperty(BlockStateProperties.WATERLOGGED))
                        state = state.setValue(BlockStateProperties.WATERLOGGED, false);
                    if (y == bottom) state = state.setValue(BlockStateProperties.AGE_25, 25);
                    writer.interior(x, y, z, state);
                }
            } else if (Math.floorMod(x * 7 + z, 31) == 0 && Math.floorMod(x + z, 5) == 0
                    && writer.airColumn(x, column.ceiling(), column.ceiling(), z)) {
                writer.interior(x, column.ceiling(), z, Blocks.SPORE_BLOSSOM.defaultBlockState());
            }
        }
    }

    private static final class Writer {
        private final WorldGenLevel level;
        private final BoundingBox chunk;
        private final BlockPos center;
        private final BranchedBossCaves layout;
        private final int sx, sz;

        private Writer(WorldGenLevel level, BoundingBox chunk, BlockPos center, long seed, BranchedBossCaves layout) {
            this.level = level;
            this.chunk = chunk;
            this.center = center;
            this.layout = layout;
            sx = (seed & 1) == 0 ? 1 : -1;
            sz = (seed & 2) == 0 ? 1 : -1;
        }

        private BranchedBossCaves.Column column(int x, int z) { return layout.column(x * sx, z * sz); }

        private boolean featureFits(int x, int floor, int z, int radius, int height) {
            if (SpiderCaveLayout.reserved(x, z)) return false;
            for (int ox = -radius; ox <= radius; ox++) for (int oz = -radius; oz <= radius; oz++) {
                var column = column(x + ox, z + oz);
                if (column == null || !column.hasAir() || column.floor() != floor || column.ceiling() < floor + height) return false;
            }
            return true;
        }

        private boolean airColumn(int x, int bottom, int top, int z) {
            for (int y = bottom; y <= top; y++) {
                BlockPos pos = center.offset(x * sx, y, z * sz);
                // Every cell in a vertical column has the same chunk owner.
                if (!chunk.isInside(pos) || !level.getBlockState(pos).isAir()) return false;
            }
            return true;
        }

        private void interior(int x, int y, int z, BlockState state) {
            var column = column(x, z);
            if (column == null || !column.hasAir() || y <= column.floor() || y > column.ceiling()) return;
            if (y <= column.floor() + 3 && SpiderCaveLayout.reserved(x, z)) return;
            put(x, y, z, state);
        }

        private void put(int x, int y, int z, BlockState state) {
            BlockPos pos = center.offset(x * sx, y, z * sz);
            if (!chunk.isInside(pos) || level.getBlockState(pos).is(Blocks.CHEST)) return;
            if (sx < 0) state = state.mirror(Mirror.FRONT_BACK);
            if (sz < 0) state = state.mirror(Mirror.LEFT_RIGHT);
            level.setBlock(pos, state, 2);
        }
    }
}

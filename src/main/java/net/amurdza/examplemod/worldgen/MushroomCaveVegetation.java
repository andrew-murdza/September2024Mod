package net.amurdza.examplemod.worldgen;

import net.amurdza.examplemod.util.GameplayDimensions;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HugeMushroomBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;

/** The original full-size patch in every suitable chunk, with offset giant stems. */
public final class MushroomCaveVegetation {
    public static final int SMALL_MUSHROOMS = 9;
    private static final List<ResourceLocation> SMALL = List.of(
            new ResourceLocation("minecraft:brown_mushroom"), new ResourceLocation("minecraft:red_mushroom"),
            new ResourceLocation("quark:glow_shroom"), new ResourceLocation("twilightforest:mushgloom"));
    private static final List<ResourceLocation> GIANT = List.of(
            new ResourceLocation("aoemod:giant_mushrooms/huge_brown_mushroom"),
            new ResourceLocation("aoemod:giant_mushrooms/huge_red_mushroom"),
            new ResourceLocation("aoemod:giant_mushrooms/huge_glow_shroom"),
            new ResourceLocation("twilightforest:mushroom/big_mushgloom"));

    public record Plan(boolean selected, int species, long seed) { }

    private MushroomCaveVegetation() { }

    public static Plan plan(long seed, ChunkPos chunk) {
        long mixed = mix(seed ^ chunk.x * 0x632BE59BD9B4E019L ^ chunk.z * 0x9E3779B97F4A7C15L ^ 0x4D555348524F4F4DL);
        return new Plan(true, (int) ((mixed >>> 2) & 3), mixed);
    }

    public static boolean place(WorldGenLevel level, ChunkGenerator generator, ChunkPos chunk) {
        if (!GameplayDimensions.is(level, GameplayDimensions.SNOWY)) return false;
        MushroomCaveFloors.prepare(level, chunk.getMinBlockX(), chunk.getMinBlockZ());
        Plan plan = plan(level.getSeed(), chunk);
        List<BlockPos> floors = new ArrayList<>();
        for (int x = chunk.getMinBlockX(); x <= chunk.getMaxBlockX(); x++) {
            for (int z = chunk.getMinBlockZ(); z <= chunk.getMaxBlockZ(); z++) {
                for (int y = -32; y < 0; y++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    // A repeated pass must not add another giant or nine more
                    // small mushrooms on a different vertical cave layer.
                    if (isMushroom(level.getBlockState(pos))) return false;
                    if (level.isEmptyBlock(pos) && level.getBlockState(pos.below()).is(Blocks.MYCELIUM)
                            && GameplayDimensions.biomeIs(level, pos, "mushroom_caves")) floors.add(pos);
                }
            }
        }
        if (floors.isEmpty()) return false;
        RandomSource random = RandomSource.create(plan.seed());
        shuffle(floors, random);
        BlockPos anchor = floors.get(0);
        boolean giantPlaced = false;
        int attempts = 0;
        var giant = level.registryAccess().registryOrThrow(Registries.CONFIGURED_FEATURE).get(GIANT.get(plan.species()));
        for (BlockPos floor : floors) {
            // Preserve the existing offset range and complete cap clearance.
            if ((floor.getX() & 15) < 3 || (floor.getX() & 15) > 12
                    || (floor.getZ() & 15) < 3 || (floor.getZ() & 15) > 12) continue;
            if (++attempts > 32) break;
            GrowthPlacementBuffer buffer = new GrowthPlacementBuffer(level);
            if (giant != null && giant.place(buffer, generator, random, floor)
                    && buffer.isValid((pos, state) -> new ChunkPos(pos).equals(chunk)) && buffer.commit()) {
                anchor = floor;
                giantPlaced = true;
                break;
            }
        }
        BlockState small = ForgeRegistries.BLOCKS.getValue(SMALL.get(plan.species())).defaultBlockState();
        int placed = 0;
        // Random positions across the chunk square, with one nearby floor per
        // column: no cell centers or evenly spaced grid of small mushrooms.
        java.util.Set<Long> columns = new java.util.HashSet<>();
        for (BlockPos floor : floors) {
            if (placed >= SMALL_MUSHROOMS) break;
            long column = ((long) floor.getX() << 32) ^ (floor.getZ() & 0xffffffffL);
            if (Math.abs(floor.getY() - anchor.getY()) > 8 || columns.contains(column)
                    || !level.isEmptyBlock(floor) || !small.canSurvive(level, floor)) continue;
            if (level.setBlock(floor, small, Block.UPDATE_CLIENTS)) {
                columns.add(column);
                placed++;
            }
        }
        return giantPlaced || placed > 0;
    }

    public static boolean isMushroom(BlockState state) {
        ResourceLocation id = ForgeRegistries.BLOCKS.getKey(state.getBlock());
        return state.getBlock() instanceof HugeMushroomBlock || SMALL.contains(id)
                || id != null && id.getNamespace().equals("quark") && id.getPath().startsWith("glow_shroom");
    }

    private static <T> void shuffle(List<T> values, RandomSource random) {
        for (int i = values.size() - 1; i > 0; i--) {
            int other = random.nextInt(i + 1);
            T value = values.get(i);
            values.set(i, values.get(other));
            values.set(other, value);
        }
    }

    private static long mix(long value) {
        value = (value ^ (value >>> 30)) * 0xBF58476D1CE4E5B9L;
        value = (value ^ (value >>> 27)) * 0x94D049BB133111EBL;
        return value ^ (value >>> 31);
    }
}

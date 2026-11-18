package net.amurdza.examplemod.worldgen.structure;

import net.amurdza.examplemod.registry.ModStructures;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.StemBlock;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraftforge.registries.ForgeRegistries;

/** Completes unfinished chunks of a saved mansion whose earlier layout uses a hatch. */
final class LegacyMansionBasementPiece extends StructurePiece {
    private static final int HALF_WIDTH = 14;
    private static final int HALF_LENGTH = 17;
    private static final ResourceLocation PANTRY = new ResourceLocation("aoemod", "chests/illager_mansion_pantry");
    private static final Block[] MINE_ORES = {Blocks.COAL_ORE, Blocks.IRON_ORE, Blocks.GOLD_ORE, Blocks.COPPER_ORE};
    private int chickens;

    LegacyMansionBasementPiece(StructurePieceSerializationContext context, CompoundTag tag) {
        super(ModStructures.MANSION_BASEMENT_PIECE.get(), tag);
        chickens = tag.getInt("Chickens");
    }

    @Override protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
        tag.putInt("Chickens", chickens);
    }

    @Override public void postProcess(WorldGenLevel level, StructureManager manager, ChunkGenerator generator,
            RandomSource random, BoundingBox clip, ChunkPos chunk, BlockPos pivot) {
        BlockPos center = boundingBox.getCenter().atY(boundingBox.minY());
        BlockPos aboveHatch = center.above(10);
        if (clip.isInside(aboveHatch) && level.getBlockState(aboveHatch).is(net.minecraft.tags.BlockTags.WOOL_CARPETS)) {
            level.setBlock(aboveHatch, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
        }
        Block cabbage = ForgeRegistries.BLOCKS.getValue(new ResourceLocation("farmersdelight", "cabbages"));
        for (int wx = Math.max(clip.minX(), boundingBox.minX()); wx <= Math.min(clip.maxX(), boundingBox.maxX()); wx++) {
            for (int wz = Math.max(clip.minZ(), boundingBox.minZ()); wz <= Math.min(clip.maxZ(), boundingBox.maxZ()); wz++) {
                int x = wx - center.getX(), z = wz - center.getZ();
                for (int y = 0; y <= 9; y++) {
                    BlockState state = cell(x, y, z, cabbage);
                    if (state == null) continue;
                    BlockPos pos = new BlockPos(wx, center.getY() + y, wz);
                    if (!clip.isInside(pos)) continue;
                    if (state.is(Blocks.CHEST) && level.getBlockEntity(pos) instanceof ChestBlockEntity) continue;
                    level.setBlock(pos, state, Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
                    if (state.is(Blocks.CHEST) && level.getBlockEntity(pos) instanceof ChestBlockEntity chest) {
                        chest.setLootTable(PANTRY, level.getSeed() ^ pos.asLong());
                    }
                }
            }
        }
        for (int i = 0; i < 4; i++) {
            BlockPos pos = center.offset(6 + i, 1, 4);
            if ((chickens & 1 << i) == 0 && clip.isInside(pos)
                    && BossLairPiece.spawn(level, "minecraft:chicken", pos)) chickens |= 1 << i;
        }
    }

    private static BlockState cell(int x, int y, int z, Block cabbage) {
        if (x == 0 && z == 0 && y > 0) {
            return y == 9 ? Blocks.OAK_TRAPDOOR.defaultBlockState().setValue(BlockStateProperties.HALF,
                    net.minecraft.world.level.block.state.properties.Half.TOP)
                    : Blocks.LADDER.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH);
        }
        if (x == 0 && z == 1 && y > 0 && y < 9) return Blocks.OAK_LOG.defaultBlockState();
        if (Math.abs(x) >= HALF_WIDTH && Math.abs(z + 5) <= 1) return shaft(x, y, z + 5, x < 0 ? 6 : 9);
        if (x >= HALF_WIDTH && Math.abs(z - 14) <= 1) return shaft(x, y, z - 14, 4);
        if (Math.abs(x) > HALF_WIDTH || y > 6) return null;
        int ax = Math.abs(x);
        if (y == 6) return Blocks.STONE_BRICKS.defaultBlockState();
        if (ax == HALF_WIDTH || Math.abs(z) == HALF_LENGTH) return Blocks.STONE_BRICKS.defaultBlockState();
        // Three cross passages open into the central aisle. Their doorways also lead to the mine faces.
        int room = Math.min(3, (z + 16) / 8);
        int row = z + 16 - 8 * room;
        boolean divider = ax == 3 && (row < 2 || row > 4 || y > 3)
                || ax > 3 && row == 0 && z > -16;
        if (y > 0 && divider) return (y == 4 && ax != 3 ? Blocks.OAK_LOG : Blocks.STONE_BRICKS).defaultBlockState();
        if (y == 5 && ax <= 1 && x == -1 && row == 4) return Blocks.CHAIN.defaultBlockState();
        if (y == 4 && x == -1 && row == 4) return Blocks.LANTERN.defaultBlockState().setValue(BlockStateProperties.HANGING, true);
        if (ax >= 4 && row >= 1) {
            BlockState contents = roomContents(x, y, room, row, cabbage);
            if (contents != null) return contents;
        }
        return (y == 0 ? Blocks.STONE_BRICKS : Blocks.AIR).defaultBlockState();
    }

    private static BlockState roomContents(int x, int y, int room, int row, Block cabbage) {
        int ax = Math.abs(x);
        if ((x < 0 || room == 0) && (ax == 6 || ax == 11) && (row == 2 || row == 6)) {
            if (y == 4 || y == 5) return Blocks.CHAIN.defaultBlockState();
            if (y == 3) return Blocks.LANTERN.defaultBlockState().setValue(BlockStateProperties.HANGING, true);
        }
        if (y == 5 && ax == 9 && row == 4 && !(x > 0 && room == 1)) {
            return Blocks.LANTERN.defaultBlockState().setValue(BlockStateProperties.HANGING, true);
        }
        if (x < 0 || room == 0) {
            if (row < 2 || row > 6 || ax < 5 || ax > 12) return null;
            if (y == 0 && ax == 8) return Blocks.WATER.defaultBlockState();
            if (ax == 8) return null;
            if (x > 0) {
                if (y == 0) return Blocks.DIRT.defaultBlockState();
                if (y <= 2 && (ax == 7 || ax == 9)) return Blocks.SUGAR_CANE.defaultBlockState();
                return null;
            }
            Block crop = switch (room) {
                case 0 -> ax < 8 ? Blocks.WHEAT : Blocks.CARROTS;
                case 1 -> ax < 8 ? Blocks.POTATOES : Blocks.BEETROOTS;
                case 2 -> cabbage;
                default -> Blocks.PUMPKIN_STEM;
            };
            if (y == 0) return Blocks.FARMLAND.defaultBlockState().setValue(BlockStateProperties.MOISTURE, 7);
            if (y == 1 && crop instanceof CropBlock plant) return plant.getStateForAge(plant.getMaxAge());
            if (y == 1 && crop == Blocks.PUMPKIN_STEM) {
                return ax % 2 == 0 ? Blocks.PUMPKIN.defaultBlockState()
                        : Blocks.PUMPKIN_STEM.defaultBlockState().setValue(StemBlock.AGE, 7);
            }
        } else if (room == 1) {
            if (ax >= 6 && ax <= 12 && row >= 2 && row <= 6) {
                if (y == 0) return Blocks.MYCELIUM.defaultBlockState();
                if (y == 1 && (ax + row) % 3 == 0) return (ax < 9 ? Blocks.RED_MUSHROOM : Blocks.BROWN_MUSHROOM).defaultBlockState();
            }
        } else if (room == 2) {
            if (y == 1 && ax == 5 && row == 4) return Blocks.OAK_FENCE_GATE.defaultBlockState()
                    .setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.WEST);
            if (y == 1 && ax == 5 && row != 4) return Blocks.OAK_FENCE.defaultBlockState()
                    .setValue(BlockStateProperties.NORTH, true).setValue(BlockStateProperties.SOUTH, true);
            if (ax > 5 && y == 0) return Blocks.DIRT.defaultBlockState();
            if (y == 1 && ax == 12 && row == 5) return Blocks.HAY_BLOCK.defaultBlockState();
        } else if (ax == 9 && y > 0 && (row != 4 || y > 3)) {
            return Blocks.OAK_PLANKS.defaultBlockState();
        } else if (y == 1) {
            if (row == 2 && (ax == 6 || ax == 8)) return Blocks.FURNACE.defaultBlockState();
            if (row == 3 && ax == 6) return Blocks.CRAFTING_TABLE.defaultBlockState();
            if (ax == 10 && (row == 3 || row == 5 || row == 7)) return Blocks.CHEST.defaultBlockState();
            if (ax == 5 && row == 6) return Blocks.BARREL.defaultBlockState();
        }
        return null;
    }

    private static BlockState shaft(int x, int y, int lane, int length) {
        int depth = Math.abs(x) - HALF_WIDTH;
        if (depth > length || y > 4) return null;
        if (y == 0 || y == 4 || depth == length || lane != 0) {
            if (depth > 0 && y == 2 && (depth + (lane + 1) * 2) % 3 == 0) {
                return MINE_ORES[Math.floorMod(depth + lane + length, MINE_ORES.length)].defaultBlockState();
            }
            return Blocks.STONE.defaultBlockState();
        }
        if (depth == 0 && y == 3) return Blocks.OAK_LOG.defaultBlockState();
        if (depth == 2 && y == 1) return Blocks.TORCH.defaultBlockState();
        return Blocks.AIR.defaultBlockState();
    }
}

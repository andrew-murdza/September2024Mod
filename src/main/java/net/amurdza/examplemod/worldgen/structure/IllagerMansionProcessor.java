package net.amurdza.examplemod.worldgen.structure;

import com.mojang.serialization.Codec;
import net.amurdza.examplemod.registry.ModStructures;
import net.amurdza.examplemod.util.GameplayDimensions;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.VineBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorType;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Converts native mansion rooms to materials available in the Plains dimension. */
public final class IllagerMansionProcessor extends StructureProcessor {
    public static final IllagerMansionProcessor INSTANCE = new IllagerMansionProcessor();
    public static final Codec<IllagerMansionProcessor> CODEC = Codec.unit(INSTANCE);
    private static final Map<Block, Block> REPLACEMENTS = Map.ofEntries(
            Map.entry(Blocks.DARK_OAK_DOOR, Blocks.OAK_DOOR),
            Map.entry(Blocks.DARK_OAK_FENCE, Blocks.OAK_FENCE),
            Map.entry(Blocks.DARK_OAK_FENCE_GATE, Blocks.OAK_FENCE_GATE),
            Map.entry(Blocks.DARK_OAK_LEAVES, Blocks.OAK_LEAVES),
            Map.entry(Blocks.DARK_OAK_LOG, Blocks.OAK_LOG),
            Map.entry(Blocks.DARK_OAK_PLANKS, Blocks.OAK_PLANKS),
            Map.entry(Blocks.DARK_OAK_SAPLING, Blocks.OAK_SAPLING),
            Map.entry(Blocks.DARK_OAK_STAIRS, Blocks.OAK_STAIRS),
            Map.entry(Blocks.DIAMOND_BLOCK, Blocks.GOLD_BLOCK),
            Map.entry(Blocks.LAPIS_BLOCK, Blocks.BLUE_WOOL),
            Map.entry(Blocks.OBSIDIAN, Blocks.STONE_BRICKS),
            Map.entry(Blocks.LAVA, Blocks.WATER),
            Map.entry(Blocks.MELON, Blocks.PUMPKIN),
            Map.entry(Blocks.ATTACHED_MELON_STEM, Blocks.ATTACHED_PUMPKIN_STEM),
            Map.entry(Blocks.LILY_PAD, Blocks.AIR),
            Map.entry(Blocks.REDSTONE_WIRE, Blocks.AIR),
            Map.entry(Blocks.POTTED_ALLIUM, Blocks.POTTED_LILY_OF_THE_VALLEY),
            Map.entry(Blocks.POTTED_BLUE_ORCHID, Blocks.POTTED_CORNFLOWER),
            Map.entry(Blocks.POTTED_RED_TULIP, Blocks.POTTED_POPPY),
            Map.entry(Blocks.POTTED_WHITE_TULIP, Blocks.POTTED_AZURE_BLUET));

    private IllagerMansionProcessor() { }

    @Override
    public StructureTemplate.StructureBlockInfo processBlock(LevelReader level, BlockPos origin, BlockPos pivot,
            StructureTemplate.StructureBlockInfo original, StructureTemplate.StructureBlockInfo current,
            StructurePlaceSettings settings) {
        if (!GameplayDimensions.is(level, GameplayDimensions.PLAINS)) return current;
        BlockState state = current.state();
        Block replacement = REPLACEMENTS.get(state.getBlock());
        // States are still in template orientation here. Vanilla mirrors and
        // rotates them after processing, including door halves, hinges and stems.
        if (replacement != null) state = replacement.withPropertiesOf(state);
        else if (state.is(Blocks.VINE)) {
            for (Direction attachment : Direction.Plane.HORIZONTAL) {
                if (state.getValue(VineBlock.getPropertyForFace(attachment))) {
                    state = Blocks.LADDER.defaultBlockState()
                            .setValue(BlockStateProperties.HORIZONTAL_FACING, attachment.getOpposite());
                    break;
                }
            }
            // A ceiling-only vine has no horizontal support for a ladder.
            if (state.is(Blocks.VINE)) state = Blocks.AIR.defaultBlockState();
        }
        CompoundTag nbt = current.nbt();
        if (state.is(Blocks.CHEST) && nbt != null && nbt.contains("Items", Tag.TAG_LIST)) {
            nbt = nbt.copy();
            for (Tag item : nbt.getList("Items", Tag.TAG_COMPOUND)) {
                CompoundTag stack = (CompoundTag) item;
                switch (stack.getString("id")) {
                    case "minecraft:allium" -> stack.putString("id", "minecraft:lily_of_the_valley");
                    case "minecraft:dark_oak_sapling" -> stack.putString("id", "minecraft:oak_sapling");
                    default -> { }
                }
            }
        }
        // Native storage rooms contain empty physical chests without a Chest
        // marker. Give those generated rewards a table while retaining fixed
        // flower/sapling inventories and any explicitly authored loot table.
        if ((state.is(Blocks.CHEST) || state.is(Blocks.TRAPPED_CHEST))
                && (nbt == null || !nbt.contains("LootTable", Tag.TAG_STRING)
                && nbt.getList("Items", Tag.TAG_COMPOUND).isEmpty())) {
            nbt = nbt == null ? new CompoundTag() : nbt.copy();
            nbt.remove("Items");
            nbt.putString("LootTable", "aoemod:chests/illager_mansion");
        }
        return new StructureTemplate.StructureBlockInfo(current.pos(), state, nbt);
    }

    @Override
    public List<StructureTemplate.StructureBlockInfo> finalizeProcessing(ServerLevelAccessor level,
            BlockPos origin, BlockPos pivot, List<StructureTemplate.StructureBlockInfo> original,
            List<StructureTemplate.StructureBlockInfo> processed, StructurePlaceSettings settings) {
        if (!GameplayDimensions.is(level, GameplayDimensions.PLAINS)) return processed;
        Map<BlockPos, BlockState> local = new HashMap<>();
        original.forEach(info -> local.put(info.pos(), info.state()));
        List<StructureTemplate.StructureBlockInfo> result = new ArrayList<>(processed);
        Map<BlockPos, Integer> indices = new HashMap<>();
        for (int i = 0; i < result.size(); i++) indices.put(result.get(i).pos(), i);
        repairWoodwork(local, result, indices, settings, origin);
        // Close the one-block slot above high, inverted decorative trim.
        // Functional ascending stairs keep their complete air tunnel.
        for (var info : original) {
            var state = info.state();
            if (info.pos().getY() >= 4 && state.getBlock() instanceof net.minecraft.world.level.block.StairBlock
                    && state.getValue(BlockStateProperties.HALF) == net.minecraft.world.level.block.state.properties.Half.TOP
                    && (state.is(Blocks.OAK_STAIRS) || state.is(Blocks.DARK_OAK_STAIRS))
                    && local.getOrDefault(info.pos().above(), Blocks.STONE.defaultBlockState()).isAir())
                replace(result, indices, settings, origin, info.pos().above(), Blocks.OAK_PLANKS);
        }
        // The extracted vanilla platform and doorway retain vanilla trim.
        // Its decorative stairs are intentional, including those whose next
        // support belongs to the separately placed doorway template.
        int width = 1 + original.stream().mapToInt(info -> info.pos().getX()).max().orElse(-1);
        int height = 1 + original.stream().mapToInt(info -> info.pos().getY()).max().orElse(-1);
        int depth = 1 + original.stream().mapToInt(info -> info.pos().getZ()).max().orElse(-1);
        boolean entranceTrim = width == 11 && height == 2 && depth == 6
                || width == 7 && height == 8 && depth == 2;
        for (var info : original) {
            if (entranceTrim || !info.state().is(Blocks.COBBLESTONE_STAIRS) || functionalStair(info.pos(), info.state(), local)) continue;
            var world = StructureTemplate.calculateRelativePosition(settings, info.pos()).offset(origin);
            Integer index = indices.get(world);
            if (index != null) result.set(index, new StructureTemplate.StructureBlockInfo(world, Blocks.COBBLESTONE.defaultBlockState(), null));
        }
        repairStairLandings(local, result, indices, settings, origin);
        placeRoomChandeliers(local, result, indices, settings, origin);
        if (original.stream().noneMatch(info -> info.state().is(Blocks.REDSTONE_WIRE))) return result;
        // The native paired-door prison routes one lever through dust. Give
        // each door a direct control on the same entrance side of its stone post.
        // Other prison rooms already have direct controls and contain no dust.
        for (var info : original) {
            BlockState door = info.state();
            if (!door.is(Blocks.IRON_DOOR)
                    || door.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF) != DoubleBlockHalf.UPPER) continue;
            Direction entrance = door.getValue(BlockStateProperties.HORIZONTAL_FACING).getOpposite();
            for (Direction side : new Direction[]{entrance.getClockWise(), entrance.getCounterClockWise()}) {
                BlockPos support = info.pos().relative(side);
                BlockState wall = local.get(support);
                if (wall == null || !wall.isCollisionShapeFullBlock(EmptyBlockGetter.INSTANCE, support)) continue;
                BlockPos control = support.relative(entrance);
                BlockState previous = local.get(control);
                if (previous == null || !previous.isAir()) continue;
                BlockPos world = StructureTemplate.calculateRelativePosition(settings, control).offset(origin);
                Integer index = indices.get(world);
                if (index == null) continue;
                BlockState lever = Blocks.LEVER.defaultBlockState().setValue(BlockStateProperties.ATTACH_FACE, AttachFace.WALL)
                        .setValue(BlockStateProperties.HORIZONTAL_FACING, entrance);
                result.set(index, new StructureTemplate.StructureBlockInfo(world, lever, null));
            }
        }
        return result;
    }

    private static void repairWoodwork(Map<BlockPos, BlockState> nativeBlocks,
            List<StructureTemplate.StructureBlockInfo> result, Map<BlockPos, Integer> indices,
            StructurePlaceSettings settings, BlockPos origin) {
        for (var entry : nativeBlocks.entrySet()) {
            BlockPos pos = entry.getKey();
            BlockState state = entry.getValue();
            if (!state.is(Blocks.DARK_OAK_PLANKS)) continue;
            for (Direction side : new Direction[]{Direction.EAST, Direction.SOUTH}) {
                Direction[] arms = {Direction.UP, Direction.DOWN, side, side.getOpposite()};
                BlockState log = nativeBlocks.get(pos.relative(side));
                if (log == null || !log.is(Blocks.DARK_OAK_LOG)) continue;
                boolean cross = true;
                for (Direction arm : arms) {
                    BlockState neighbor = nativeBlocks.get(pos.relative(arm));
                    if (neighbor == null || !neighbor.is(Blocks.DARK_OAK_LOG)
                            || neighbor.getValue(BlockStateProperties.AXIS) != log.getValue(BlockStateProperties.AXIS)) cross = false;
                }
                if (!cross) continue;
                BlockState oak = Blocks.OAK_LOG.withPropertiesOf(log);
                replace(result, indices, settings, origin, pos, oak);
                // Both the old one-block arms and ordinary two-block arms now
                // use the same five-block cross, including its solid log center.
                for (Direction arm : arms) {
                    BlockPos tip = pos.relative(arm, 2);
                    BlockState old = nativeBlocks.get(tip);
                    if (old != null && old.is(Blocks.DARK_OAK_PLANKS)) replace(result, indices, settings, origin, tip, oak);
                }
                break;
            }
        }
    }

    /** A native room tile is seven blocks inside the shared wall, on an eight-block pitch. */
    private static void placeRoomChandeliers(Map<BlockPos, BlockState> nativeBlocks,
            List<StructureTemplate.StructureBlockInfo> result, Map<BlockPos, Integer> indices,
            StructurePlaceSettings settings, BlockPos origin) {
        int width = 1 + nativeBlocks.keySet().stream().mapToInt(BlockPos::getX).max().orElse(-1);
        int depth = 1 + nativeBlocks.keySet().stream().mapToInt(BlockPos::getZ).max().orElse(-1);
        int height = 1 + nativeBlocks.keySet().stream().mapToInt(BlockPos::getY).max().orElse(-1);
        if ((width != 7 && width != 15) || (depth != 7 && depth != 15) || (height != 8 && height != 11)) return;
        // Corridor tiles have the same volume as small rooms, but their red carpet identifies the walkway.
        if (is(nativeBlocks, 3, 1, 3, Blocks.RED_CARPET)) return;
        // Remove the old suspended fence chandeliers, including their transverse
        // ceiling bars, while leaving low furniture and the perimeter torch brackets.
        for (var entry : nativeBlocks.entrySet()) {
            var pos = entry.getKey();
            if (pos.getY() < 3 || pos.getY() < 7 && (pos.getX() == 0 || pos.getX() == width - 1
                    || pos.getZ() == 0 || pos.getZ() == depth - 1)) continue;
            if (entry.getValue().is(Blocks.DARK_OAK_FENCE) || entry.getValue().is(Blocks.OAK_FENCE)
                    || entry.getValue().is(Blocks.TORCH)) replace(result, indices, settings, origin, pos, Blocks.AIR);
        }
        for (int x = 3; x < width; x += 8) for (int z = 3; z < depth; z += 8) {
            // Raise the wooden room chandelier one block; basement lanterns
            // use their separately versioned authored blueprint.
            var center = new BlockPos(x, 4, z);
            var fence = Blocks.OAK_FENCE.defaultBlockState();
            for (Direction arm : Direction.Plane.HORIZONTAL) {
                var tip = center.relative(arm);
                // The library has a narrow aisle between tall shelves. Keep its
                // books intact and use only the arms that fit the authored space.
                if (!chandelierSpace(nativeBlocks.get(tip)) || !chandelierSpace(nativeBlocks.get(tip.above()))) continue;
                fence = fence.setValue(net.minecraft.world.level.block.PipeBlock.PROPERTY_BY_DIRECTION.get(arm), true);
                var connected = Blocks.OAK_FENCE.defaultBlockState()
                        .setValue(net.minecraft.world.level.block.PipeBlock.PROPERTY_BY_DIRECTION.get(arm.getOpposite()), true);
                replace(result, indices, settings, origin, tip, connected);
                replace(result, indices, settings, origin, tip.above(), Blocks.TORCH);
            }
            replace(result, indices, settings, origin, center, fence);
            for (int y = 5; y <= 7; y++) {
                var stem = new BlockPos(x, y, z);
                if (!chandelierSpace(nativeBlocks.get(stem))) break;
                replace(result, indices, settings, origin, stem, Blocks.OAK_FENCE);
            }
        }
    }

    private static boolean chandelierSpace(BlockState state) {
        return state != null && (state.isAir() || state.is(Blocks.OAK_FENCE) || state.is(Blocks.DARK_OAK_FENCE) || state.is(Blocks.TORCH));
    }

    private static void repairStairLandings(Map<BlockPos, BlockState> nativeBlocks,
            List<StructureTemplate.StructureBlockInfo> result, Map<BlockPos, Integer> indices,
            StructurePlaceSettings settings, BlockPos origin) {
        // These signatures identify only the bundled entrance and upper stair rooms.
        // Apply in local coordinates so every native rotation and mirror receives the same repair.
        if (nativeBlocks.containsKey(new BlockPos(20, 15, 15)) && is(nativeBlocks, 7, 9, 0, Blocks.COBBLESTONE)
                && is(nativeBlocks, 7, 9, 14, Blocks.COBBLESTONE)) {
            for (int z : new int[]{0, 14}) replace(result, indices, settings, origin, new BlockPos(7, 9, z), Blocks.AIR);
            // Fill the inset oak patch behind the split stair landing to match its stone surround.
            for (int y = 6; y <= 8; y++) for (int z = 5; z <= 9; z++)
                replace(result, indices, settings, origin, new BlockPos(1, y, z), Blocks.COBBLESTONE);
        }
        if (is(nativeBlocks, 6, 21, 14, Blocks.AIR) && is(nativeBlocks, 6, 1, 3, Blocks.COBBLESTONE_STAIRS)) {
            // Only this room omits the two supports beneath its corner stair blocks.
            // Fill at the native landing's Y9 floor; Y11 is walkable headroom above those stairs.
            for (int x : new int[]{1, 5})
                replace(result, indices, settings, origin, new BlockPos(x, 9, 13), Blocks.BIRCH_PLANKS);
        }
    }

    private static boolean is(Map<BlockPos, BlockState> blocks, int x, int y, int z, Block block) {
        BlockState state = blocks.get(new BlockPos(x, y, z));
        return state != null && state.is(block);
    }

    private static void replace(List<StructureTemplate.StructureBlockInfo> result, Map<BlockPos, Integer> indices,
            StructurePlaceSettings settings, BlockPos origin, BlockPos local, Block block) {
        replace(result, indices, settings, origin, local, block.defaultBlockState());
    }

    private static void replace(List<StructureTemplate.StructureBlockInfo> result, Map<BlockPos, Integer> indices,
            StructurePlaceSettings settings, BlockPos origin, BlockPos local, BlockState state) {
        BlockPos world = StructureTemplate.calculateRelativePosition(settings, local).offset(origin);
        Integer index = indices.get(world);
        if (index != null) result.set(index, new StructureTemplate.StructureBlockInfo(world, state, null));
    }

    private static boolean functionalStair(BlockPos pos, BlockState state, Map<BlockPos, BlockState> blocks) {
        if (state.getValue(BlockStateProperties.HALF) == net.minecraft.world.level.block.state.properties.Half.TOP) return false;
        var facing = state.getValue(BlockStateProperties.HORIZONTAL_FACING);
        var above = blocks.get(pos.relative(facing).above());
        var below = blocks.get(pos.relative(facing.getOpposite()).below());
        if (above != null && above.is(Blocks.COBBLESTONE_STAIRS) || below != null && below.is(Blocks.COBBLESTONE_STAIRS)) return true;
        var upperFloor = blocks.get(pos.relative(facing));
        var lowerFloor = blocks.get(pos.relative(facing.getOpposite()).below());
        return upperFloor != null && upperFloor.isCollisionShapeFullBlock(EmptyBlockGetter.INSTANCE, BlockPos.ZERO)
                // A side-entry tread can start at the template edge, where its approach
                // floor belongs to the adjacent hall; a turn can follow another stair.
                && (lowerFloor == null || lowerFloor.isCollisionShapeFullBlock(EmptyBlockGetter.INSTANCE, BlockPos.ZERO)
                    || lowerFloor.getBlock() instanceof net.minecraft.world.level.block.StairBlock)
                && clear(blocks.get(pos.relative(facing).above())) && clear(blocks.get(pos.relative(facing).above(2)))
                && clear(blocks.get(pos.relative(facing.getOpposite())));
    }

    private static boolean clear(BlockState state) {
        return state == null || state.isAir() || state.is(net.minecraft.tags.BlockTags.WOOL_CARPETS);
    }

    @Override
    protected StructureProcessorType<?> getType() { return ModStructures.MANSION_PROCESSOR.get(); }
}

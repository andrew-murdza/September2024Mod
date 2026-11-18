package net.amurdza.examplemod.worldgen.structure;

import com.mojang.serialization.Codec;
import net.amurdza.examplemod.block.AcaciaPalisadeBlock;
import net.amurdza.examplemod.registry.ModStructures;
import net.amurdza.examplemod.registry.ModBlocks;
import net.amurdza.examplemod.util.GameplayDimensions;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.WallSide;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorType;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Applies the Savanna palette to native Orcz camps before template chunk clipping. */
public final class SavannaOrcCampProcessor extends StructureProcessor {
    public static final SavannaOrcCampProcessor INSTANCE = new SavannaOrcCampProcessor();
    public static final Codec<SavannaOrcCampProcessor> CODEC = Codec.unit(INSTANCE);
    public static final ResourceLocation LOOT_TABLE = new ResourceLocation("aoemod", "chests/orc_camp");
    private static final Set<Block> GROUND = Set.of(Blocks.DIRT, Blocks.GRASS_BLOCK, Blocks.COARSE_DIRT,
            Blocks.ROOTED_DIRT, Blocks.PODZOL, Blocks.DIRT_PATH, Blocks.MUD, Blocks.STONE, Blocks.COBBLESTONE,
            Blocks.MOSSY_COBBLESTONE, Blocks.GRAVEL, Blocks.ANDESITE, Blocks.DIORITE, Blocks.GRANITE,
            Blocks.DEEPSLATE, Blocks.COBBLED_DEEPSLATE, Blocks.INFESTED_STONE, Blocks.SAND, Blocks.RED_SAND,
            Blocks.SANDSTONE, Blocks.SMOOTH_SANDSTONE, Blocks.RED_SANDSTONE, Blocks.SMOOTH_RED_SANDSTONE,
            Blocks.CUT_RED_SANDSTONE, Blocks.CHISELED_RED_SANDSTONE, Blocks.POLISHED_ANDESITE,
            Blocks.POLISHED_DIORITE, Blocks.TERRACOTTA);

    private SavannaOrcCampProcessor() { }

    @Override
    public StructureTemplate.StructureBlockInfo processBlock(LevelReader level, BlockPos origin, BlockPos pivot,
            StructureTemplate.StructureBlockInfo original, StructureTemplate.StructureBlockInfo current,
            StructurePlaceSettings settings) {
        // Vanilla runs processors before clipping. Keep whole vertical columns
        // and a four-block horizontal halo for the ground/bush/campfire repairs
        // below, without allocating the entire village for every chunk.
        var clip = settings.getBoundingBox();
        if (clip != null && (current.pos().getX() < clip.minX() - 4 || current.pos().getX() > clip.maxX() + 4
                || current.pos().getZ() < clip.minZ() - 4 || current.pos().getZ() > clip.maxZ() + 4)) return null;
        if (!GameplayDimensions.is(level, GameplayDimensions.SAVANNA)) return current;
        BlockState state = convertMaterial(current.state());
        CompoundTag data = current.nbt();
        if (state.is(Blocks.CHEST)) {
            data = data == null ? new CompoundTag() : data.copy();
            data.remove("Items");
            data.putString("LootTable", LOOT_TABLE.toString());
            if (level instanceof WorldGenLevel world) {
                var persistent = data.getCompound("ForgeData");
                persistent.putInt(OrcCampStock.STOCK_TAG, OrcCampStock.selection(world.getSeed(), origin));
                if (persistent.contains(OrcVillageLoot.INDEX)) {
                    persistent.putLong(OrcVillageLoot.SEED, world.getSeed() ^ net.minecraft.util.Mth.getSeed(origin));
                    persistent.putLong(OrcVillageLoot.ORIGIN, origin.asLong());
                }
                data.put("ForgeData", persistent);
            }
        }
        return new StructureTemplate.StructureBlockInfo(current.pos(), state, data);
    }

    private static BlockState convertMaterial(BlockState state) {
        ResourceLocation id = ForgeRegistries.BLOCKS.getKey(state.getBlock());
        String path = id.getPath();
        if (id.getNamespace().equals("orcz") && path.startsWith("orcsymbol")) return Blocks.AIR.defaultBlockState();
        if (id.getNamespace().equals("orcz") && path.startsWith("orc_wood")) {
            return (path.endsWith("_slab") ? block("minecraft", "black_wool_slab")
                    : path.endsWith("_stairs") ? block("minecraft", "black_wool_stairs") : Blocks.BLACK_WOOL).withPropertiesOf(state);
        }
        if (id.getNamespace().equals("orcz") && path.startsWith("scrap")
                || state.is(Blocks.IRON_ORE) || state.is(Blocks.COAL_ORE)
                || state.is(Blocks.DEEPSLATE_IRON_ORE) || state.is(Blocks.DEEPSLATE_COAL_ORE)) {
            return Blocks.BONE_BLOCK.defaultBlockState();
        }
        if (path.contains("quartz")) {
            return block("alexscaves", path.endsWith("_stairs") ? "smooth_bone_stairs"
                    : path.endsWith("_slab") ? "smooth_bone_slab"
                    : path.endsWith("_wall") ? "smooth_bone_wall" : "smooth_bone").withPropertiesOf(state);
        }
        if (state.is(Blocks.BLACKSTONE)) return block("quark", "charcoal_block").defaultBlockState();
        if (state.is(Blocks.DANDELION)) return Blocks.GRASS.defaultBlockState();
        // Preserve vanilla wall states, including tall sides beneath stacked walls.
        if (id.getNamespace().equals("orcz") && (path.equals("palisadedarkoak") || path.equals("pallisade"))) {
            return normalPalisade(ModBlocks.ACACIA_PALISADE.get().withPropertiesOf(state));
        }
        if (state.is(ModBlocks.ACACIA_PALISADE.get())) return normalPalisade(state);
        if (state.is(BlockTags.LOGS)) {
            return (path.startsWith("stripped_")
                    ? path.endsWith("_wood") || path.endsWith("_hyphae") ? Blocks.STRIPPED_ACACIA_WOOD : Blocks.STRIPPED_ACACIA_LOG
                    : path.endsWith("_wood") || path.endsWith("_hyphae") ? Blocks.ACACIA_WOOD : Blocks.ACACIA_LOG).withPropertiesOf(state);
        }
        if (state.is(BlockTags.LEAVES) || id.getNamespace().equals("orcz") && path.equals("fakeleaf")) return Blocks.ACACIA_LEAVES.withPropertiesOf(state);
        if (state.is(BlockTags.PLANKS)) return Blocks.ACACIA_PLANKS.withPropertiesOf(state);
        if (state.is(BlockTags.WOODEN_SLABS)) return Blocks.ACACIA_SLAB.withPropertiesOf(state);
        if (state.is(BlockTags.WOODEN_STAIRS)) return Blocks.ACACIA_STAIRS.withPropertiesOf(state);
        if (state.is(BlockTags.WOODEN_FENCES)) return Blocks.ACACIA_FENCE.withPropertiesOf(state);
        if (state.is(BlockTags.FENCE_GATES)) return Blocks.ACACIA_FENCE_GATE.withPropertiesOf(state);
        if (state.is(BlockTags.WOODEN_BUTTONS) || state.is(Blocks.STONE_BUTTON)) return Blocks.ACACIA_BUTTON.withPropertiesOf(state);
        if (state.is(BlockTags.WOODEN_PRESSURE_PLATES)) return Blocks.ACACIA_PRESSURE_PLATE.withPropertiesOf(state);
        if (state.is(BlockTags.WOODEN_DOORS)) return Blocks.ACACIA_DOOR.withPropertiesOf(state);
        if (state.is(BlockTags.WOODEN_TRAPDOORS)) return Blocks.ACACIA_TRAPDOOR.withPropertiesOf(state);
        if (state.is(BlockTags.SAPLINGS)) return Blocks.ACACIA_SAPLING.withPropertiesOf(state);
        if (state.is(Blocks.COBBLESTONE_SLAB) || state.is(Blocks.MOSSY_COBBLESTONE_SLAB)) return Blocks.ACACIA_SLAB.withPropertiesOf(state);
        if (state.is(BlockTags.WALLS)) return acaciaFence(state);
        if (state.is(Blocks.STONE_BRICKS)) return block("alexscaves", "smooth_bone").defaultBlockState();
        return state;
    }

    public static BlockState normalPalisade(BlockState state) {
        return AcaciaPalisadeBlock.normalConnections(state);
    }

    @Override
    public List<StructureTemplate.StructureBlockInfo> finalizeProcessing(ServerLevelAccessor level,
            BlockPos origin, BlockPos pivot, List<StructureTemplate.StructureBlockInfo> original,
            List<StructureTemplate.StructureBlockInfo> processed, StructurePlaceSettings settings) {
        if (!GameplayDimensions.is(level, GameplayDimensions.SAVANNA)) return processed;
        Map<BlockPos, BlockState> local = new HashMap<>();
        Map<Long, Integer> groundTops = new HashMap<>();
        for (var info : original) {
            local.put(info.pos(), info.state());
            if (GROUND.contains(info.state().getBlock())) groundTops.merge(column(info.pos()), info.pos().getY(), Math::max);
        }
        Set<BlockPos> seats = Set.copyOf(cushionSeats(original));
        Map<BlockPos, Integer> indices = new HashMap<>();
        List<StructureTemplate.StructureBlockInfo> result = new ArrayList<>(processed);
        for (int i = 0; i < result.size(); i++) indices.put(result.get(i).pos(), i);
        for (var info : original) {
            BlockPos localPos = info.pos();
            Block replacement;
            if (processedPalisade(info.state())) {
                BlockPos world = StructureTemplate.calculateRelativePosition(settings, localPos).offset(origin);
                Integer index = indices.get(world);
                if (index != null) {
                    var placed = result.get(index);
                    result.set(index, new StructureTemplate.StructureBlockInfo(world, placed.state(), placed.nbt()));
                }
                continue;
            }
            if (seats.contains(localPos)) replacement = Blocks.AIR;
            else if (info.state().is(Blocks.DIRT_PATH)) replacement = Blocks.DIRT_PATH;
            else if (info.state().is(Blocks.PODZOL)) replacement = Blocks.PODZOL;
            else if (info.state().is(Blocks.MUD)) replacement = Blocks.MUD;
            else if (GROUND.contains(info.state().getBlock())) {
                int depth = groundTops.get(column(localPos)) - localPos.getY();
                BlockState above = local.get(localPos.above());
                boolean exposed = above == null || !above.isSolidRender(EmptyBlockGetter.INSTANCE, localPos.above());
                replacement = above != null && above.getFluidState().is(FluidTags.WATER) ? Blocks.MUD
                        : depth == 0 && above != null && above.is(Blocks.ACACIA_LOG) ? Blocks.DIRT
                        : depth == 0 && exposed ? Blocks.GRASS_BLOCK
                        : depth <= 2 ? Blocks.ROOTED_DIRT : depth <= 4 ? Blocks.DIRT : Blocks.MUD;
            } else if (isBush(info.state()) && enclosedByGround(level, localPos, local, origin, settings)) {
                // Buried perimeter leaves otherwise leave a dark, leaf-shaped hole in the camp lawn.
                replacement = Blocks.GRASS_BLOCK;
            } else continue;
            BlockPos world = StructureTemplate.calculateRelativePosition(settings, localPos).offset(origin);
            Integer index = indices.get(world);
            if (index != null) result.set(index, new StructureTemplate.StructureBlockInfo(world, replacement.defaultBlockState(), null));
        }
        return result;
    }

    private static BlockState acaciaFence(BlockState wall) {
        BlockState fence = Blocks.ACACIA_FENCE.withPropertiesOf(wall);
        if (wall.hasProperty(WallBlock.NORTH_WALL)) fence = fence.setValue(FenceBlock.NORTH, wall.getValue(WallBlock.NORTH_WALL) != WallSide.NONE);
        if (wall.hasProperty(WallBlock.SOUTH_WALL)) fence = fence.setValue(FenceBlock.SOUTH, wall.getValue(WallBlock.SOUTH_WALL) != WallSide.NONE);
        if (wall.hasProperty(WallBlock.WEST_WALL)) fence = fence.setValue(FenceBlock.WEST, wall.getValue(WallBlock.WEST_WALL) != WallSide.NONE);
        if (wall.hasProperty(WallBlock.EAST_WALL)) fence = fence.setValue(FenceBlock.EAST, wall.getValue(WallBlock.EAST_WALL) != WallSide.NONE);
        return fence;
    }

    private static boolean processedPalisade(BlockState state) {
        if (!state.hasProperty(WallBlock.NORTH_WALL)) return false;
        if (state.is(ModBlocks.ACACIA_PALISADE.get())) return true;
        var id = ForgeRegistries.BLOCKS.getKey(state.getBlock());
        return id.getNamespace().equals("orcz") && (id.getPath().equals("palisadedarkoak") || id.getPath().equals("pallisade"));
    }

    private static boolean isBush(BlockState state) {
        return state.is(BlockTags.LEAVES) || new ResourceLocation("orcz", "fakeleaf").equals(ForgeRegistries.BLOCKS.getKey(state.getBlock()));
    }

    private static boolean enclosedByGround(LevelReader level, BlockPos pos, Map<BlockPos, BlockState> local,
                                            BlockPos origin, StructurePlaceSettings settings) {
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            BlockPos neighbor = pos.relative(direction);
            BlockState state = local.get(neighbor);
            if (state == null) {
                BlockPos world = StructureTemplate.calculateRelativePosition(settings, neighbor).offset(origin);
                if (!level.hasChunkAt(world)) return false;
                state = level.getBlockState(world);
            }
            if (!GROUND.contains(state.getBlock())) return false;
        }
        return true;
    }

    public static List<BlockPos> cushionSeats(StructureTemplate template) {
        var settings = new StructurePlaceSettings();
        List<StructureTemplate.StructureBlockInfo> candidates = new ArrayList<>(template.filterBlocks(BlockPos.ZERO, settings, Blocks.CAMPFIRE));
        candidates.addAll(template.filterBlocks(BlockPos.ZERO, settings, Blocks.SOUL_CAMPFIRE));
        candidates.addAll(template.filterBlocks(BlockPos.ZERO, settings, Blocks.SMOOTH_STONE_SLAB));
        return cushionSeats(candidates);
    }

    private static List<BlockPos> cushionSeats(List<StructureTemplate.StructureBlockInfo> blocks) {
        List<BlockPos> fires = blocks.stream().filter(info -> info.state().is(Blocks.CAMPFIRE) || info.state().is(Blocks.SOUL_CAMPFIRE))
                .map(StructureTemplate.StructureBlockInfo::pos).toList();
        return blocks.stream().filter(info -> info.state().is(Blocks.SMOOTH_STONE_SLAB))
                .map(StructureTemplate.StructureBlockInfo::pos)
                .filter(pos -> fires.stream().anyMatch(fire -> pos.getY() == fire.getY() && pos.distManhattan(fire) <= 4)).toList();
    }

    private static long column(BlockPos pos) { return (long) pos.getX() << 32 ^ pos.getZ() & 0xffffffffL; }

    private static Block block(String namespace, String path) {
        return ForgeRegistries.BLOCKS.getValue(new ResourceLocation(namespace, path));
    }

    @Override
    protected StructureProcessorType<?> getType() { return ModStructures.SAVANNA_ORC_CAMP_PROCESSOR.get(); }
}

package net.amurdza.examplemod.worldgen;

import net.amurdza.examplemod.util.GameplayDimensions;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.MultifaceBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.feature.SculkPatchFeature;
import net.minecraftforge.common.Tags;
import net.minecraftforge.common.world.ModifiableBiomeInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/** Horizontal cave skins applied after carving and before deep-dark plants and veins. */
public final class DeepDarkEcology {
    private DeepDarkEcology() { }

    public static void removeLegacyFeatures(ModifiableBiomeInfo.BiomeInfo.Builder builder) {
        Set<String> removed = Set.of("minecraft:sculk_patch_deep_dark", "minecraft:sculk_patch_ancient_city",
                "minecraft:sculk_vein", "minecraft:glow_lichen", "aoemod:glow_lichen");
        for (GenerationStep.Decoration step : GenerationStep.Decoration.values()) {
            builder.getGenerationSettings().getFeatures(step).removeIf(feature -> feature.value().feature().value().feature() instanceof SculkPatchFeature
                    || feature.unwrapKey().map(key -> removed.contains(key.location().toString())).orElse(false));
        }
    }

    public static void prepareSurfaces(WorldGenLevel level, int minX, int minZ) {
        if (!GameplayDimensions.is(level, GameplayDimensions.DESERT)) return;
        List<BlockPos> floors = new ArrayList<>(), ceilings = new ArrayList<>();
        // Discover before writing so thin shelves are not classified differently
        // according to column order. Coat dry and flooded horizontal interfaces.
        for (int x = minX; x < minX + 16; x++) for (int z = minZ; z < minZ + 16; z++) {
            for (int y = level.getMinBuildHeight(); y < level.getMaxBuildHeight(); y++) {
                BlockPos open = new BlockPos(x, y, z);
                BlockState state = level.getBlockState(open);
                if (!(state.isAir() || state.is(Blocks.WATER) || state.is(Blocks.SCULK_VEIN)) || !isHabitat(level, open)) continue;
                if (naturalSurface(level.getBlockState(open.below()))) floors.add(open.below());
                if (naturalSurface(level.getBlockState(open.above()))) ceilings.add(open.above());
            }
        }
        ceilings.forEach(pos -> coatSurface(level, pos, Direction.UP));
        // Thin steps can be both a ceiling and a floor. The floor pass still
        // requires buried backing, so an unsupported shelf stays deepslate.
        floors.forEach(pos -> coatSurface(level, pos, Direction.DOWN));
    }

    /** Also used by the retained spiral-cave structure. Horizontal walls stay stone. */
    public static void coatSurface(WorldGenLevel level, BlockPos surface, Direction intoRock) {
        if (intoRock.getAxis() != Direction.Axis.Y || !naturalSurface(level.getBlockState(surface))) return;
        // A sculk floor needs its immediate supporting block hidden inside solid rock.
        // Exposed ledges keep the entire skin dark, including their tread.
        if (intoRock == Direction.UP || backingWouldBeExposed(level, surface.below())) {
            level.setBlock(surface, Blocks.DEEPSLATE.defaultBlockState(), Block.UPDATE_CLIENTS);
            for (int depth = 1; depth <= 2; depth++) {
                BlockPos pos = surface.relative(intoRock, depth);
                BlockState state = level.getBlockState(pos);
                if (!naturalSurface(state) || state.is(Blocks.SCULK)) break;
                level.setBlock(pos, Blocks.DEEPSLATE.defaultBlockState(), Block.UPDATE_CLIENTS);
            }
            return;
        }
        level.setBlock(surface, Blocks.SCULK.defaultBlockState(), Block.UPDATE_CLIENTS);
        // An earlier neighbour's patch can cross into this undecorated chunk.
        // Remove only its newly invalid faces, retaining valid wall attachments.
        for (Direction side : Direction.values()) {
            BlockPos veinPos = surface.relative(side);
            BlockState vein = level.getBlockState(veinPos);
            if (!vein.is(Blocks.SCULK_VEIN)) continue;
            var face = MultifaceBlock.getFaceProperty(side.getOpposite());
            if (!vein.getValue(face)) continue;
            BlockState remaining = vein.setValue(face, false);
            boolean attached = false;
            for (Direction direction : Direction.values()) attached |= remaining.getValue(MultifaceBlock.getFaceProperty(direction));
            if (!attached) remaining = (vein.getValue(BlockStateProperties.WATERLOGGED) ? Blocks.WATER : Blocks.AIR).defaultBlockState();
            level.setBlock(veinPos, remaining, Block.UPDATE_CLIENTS);
        }
        for (int depth = 1; depth <= 2; depth++) {
            BlockPos pos = surface.relative(intoRock, depth);
            BlockState state = level.getBlockState(pos);
            if (!naturalSurface(state) || state.is(Blocks.SCULK)) break;
            // A deeper layer can meet an adjacent stair even when the first
            // support is buried. Keep that exposed layer dark without rejecting the tread.
            if (backingWouldBeExposed(level, pos)) break;
            level.setBlock(pos, Blocks.END_STONE.defaultBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    private static boolean isHabitat(WorldGenLevel level, BlockPos pos) {
        // BiomeManager's fuzzy quart interpolation used to move this boundary
        // up or down according to X/Z. The authored Desert stratum ends at Y0.
        return pos.getY() >= level.getMinBuildHeight() && pos.getY() < 0;
    }

    private static boolean backingWouldBeExposed(WorldGenLevel level, BlockPos pos) {
        // The dimension boundary is sealed terrain, not an open cave below its floor.
        if (pos.getY() < level.getMinBuildHeight()) return false;
        if (!naturalSurface(level.getBlockState(pos))) return true;
        for (Direction side : Direction.values()) {
            BlockPos neighbor = pos.relative(side);
            if (neighbor.getY() < level.getMinBuildHeight()) continue;
            if (!level.getBlockState(neighbor).isFaceSturdy(level, neighbor, side.getOpposite())) return true;
        }
        return false;
    }

    private static boolean naturalSurface(BlockState state) {
        return state.is(BlockTags.BASE_STONE_OVERWORLD) || state.is(Tags.Blocks.ORES)
                || state.is(Blocks.SCULK) || state.is(Blocks.END_STONE)
                || state.is(Blocks.CALCITE) || state.is(Blocks.DRIPSTONE_BLOCK)
                || state.is(Blocks.DIRT) || state.is(Blocks.GRAVEL);
    }
}

package net.amurdza.examplemod.worldgen.feature.features;

import com.mojang.serialization.Codec;
import net.amurdza.examplemod.worldgen.feature.configs.RainforestTreeFeatureConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.WeepingVinesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.treedecorators.TreeDecorator;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorType;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

public class RainforestTreeFeature extends Feature<RainforestTreeFeatureConfig> {
    private static final BlockState[] CORAL_PATTERN = new BlockState[]{
            Blocks.BUBBLE_CORAL_BLOCK.defaultBlockState(),
            Blocks.HORN_CORAL_BLOCK.defaultBlockState(),
            Blocks.FIRE_CORAL_BLOCK.defaultBlockState(),
            Blocks.TUBE_CORAL_BLOCK.defaultBlockState(),
            Blocks.BRAIN_CORAL_BLOCK.defaultBlockState(),
            Blocks.BRAIN_CORAL_BLOCK.defaultBlockState(),
            Blocks.TUBE_CORAL_BLOCK.defaultBlockState(),
            Blocks.FIRE_CORAL_BLOCK.defaultBlockState(),
            Blocks.HORN_CORAL_BLOCK.defaultBlockState(),
            Blocks.BUBBLE_CORAL_BLOCK.defaultBlockState()
    };

    public RainforestTreeFeature(Codec<RainforestTreeFeatureConfig> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<RainforestTreeFeatureConfig> context) {
        WorldGenLevel level = context.level();
        RandomSource random = context.random();
        RainforestTreeFeatureConfig config = context.config();
        BlockPos origin = findNearestValidOrigin(level, context.origin(), config);

        if (origin == null) {
            return false;
        }

        StructureTemplateManager templateManager =
                level.getLevel().getServer().getStructureManager();

        Optional<StructureTemplate> optionalTemplate =
                templateManager.get(config.canopyTemplate());

        if (optionalTemplate.isEmpty()) {
            return false;
        }

        StructureTemplate template = optionalTemplate.get();
        Vec3i size = template.getSize();

        int sizeX = size.getX();
        int sizeZ = size.getZ();

        // Require square canopy templates. Even templates center over a 2x2 trunk;
        // odd templates center on the feature origin.
        if (sizeX <= 0 || sizeZ <= 0 || sizeX != sizeZ) {
            return false;
        }

        int canopyBottomY = config.topY()
                .map(topY -> topY - size.getY() + 1)
                .orElseGet(() -> origin.getY() + config.heightProvider().sample(random));

        int trunkHeight = canopyBottomY - origin.getY();
        if (trunkHeight <= 0) {
            return false;
        }

        int trunkWidth = config.trunkWidth();

        // Ground check: every block under the trunk footprint must satisfy valid_ground.
        for (int dx = 0; dx < trunkWidth; dx++) {
            for (int dz = 0; dz < trunkWidth; dz++) {
                BlockPos groundPos = origin.offset(dx, -1, dz);
                if (!isValidTreeGround(level, groundPos, config)) {
                    return false;
                }
            }
        }

        BlockPos canopyOrigin = canopyOrigin(origin, canopyBottomY, sizeX, trunkWidth);
        if (sizeX == 5 && !hasAirTopMargin(level, origin, canopyOrigin, size)) {
            return false;
        }

        Set<BlockPos> logPositions = new HashSet<>();
        Set<BlockPos> leafPositions = new HashSet<>();
        // Place trunk here
        for (int y = 0; y < trunkHeight; y++) {
            for (int dx = 0; dx < trunkWidth; dx++) {
                for (int dz = 0; dz < trunkWidth; dz++) {
                    BlockPos trunkPos = origin.offset(dx, y, dz);
                    BlockState trunkState = config.logProvider().getState(random, trunkPos);

                    level.setBlock(trunkPos, trunkState, 19);
                    logPositions.add(trunkPos.immutable());
                }
            }
        }

        if (config.placeWaterRing()) {
            replaceWaterRing(level, origin, trunkWidth, trunkHeight);
        }

        StructurePlaceSettings settings = new StructurePlaceSettings()
                .setIgnoreEntities(true)
                .addProcessor(new ProviderReplacingTemplateProcessor(config, random, logPositions, leafPositions));

        boolean placedCanopy = template.placeInWorld(level, canopyOrigin, canopyOrigin, settings, random, 19);
        if (!placedCanopy) {
            return false;
        }

        // Run vanilla-style tree decorators.
        if (!config.decorators().isEmpty()) {
            TreeDecorator.Context decoratorContext = new TreeDecorator.Context(
                    level,
                    (pos, state) -> level.setBlock(pos, state, 19),
                    random,
                    logPositions,
                    leafPositions,
                    Set.of()
            );

            for (TreeDecorator decorator : config.decorators()) {
                decorator.place(decoratorContext);
            }
        }

        return true;
    }

    private static BlockPos canopyOrigin(BlockPos origin, int canopyBottomY, int sizeX, int trunkWidth) {
        int offset;
        if (trunkWidth == 2 && sizeX % 2 == 0) {
            offset = 1 - sizeX / 2;
        } else {
            offset = -sizeX / 2;
        }

        return new BlockPos(origin.getX() + offset, canopyBottomY, origin.getZ() + offset);
    }

    private static @Nullable BlockPos findNearestValidOrigin(
            WorldGenLevel level,
            BlockPos origin,
            RainforestTreeFeatureConfig config
    ) {
        for (int yOffset = 0; yOffset <= 8; yOffset++) {
            BlockPos up = origin.above(yOffset);
            if (hasValidTrunkGround(level, up, config)) {
                return up;
            }

            if (yOffset == 0) {
                continue;
            }

            BlockPos down = origin.below(yOffset);
            if (hasValidTrunkGround(level, down, config)) {
                return down;
            }
        }

        return null;
    }

    private static boolean hasValidTrunkGround(
            WorldGenLevel level,
            BlockPos origin,
            RainforestTreeFeatureConfig config
    ) {
        for (int dx = 0; dx < config.trunkWidth(); dx++) {
            for (int dz = 0; dz < config.trunkWidth(); dz++) {
                if (!isValidTreeGround(level, origin.offset(dx, -1, dz), config)) {
                    return false;
                }
            }
        }

        return true;
    }

    private static boolean isValidTreeGround(
            WorldGenLevel level,
            BlockPos pos,
            RainforestTreeFeatureConfig config
    ) {
        BlockState state = level.getBlockState(pos);

        return config.validGround().test(level, pos)
                || state.is(Blocks.MOSS_BLOCK)
                || state.is(BlockTags.CORAL_BLOCKS);
    }

    private static boolean hasAirTopMargin(WorldGenLevel level, BlockPos origin, BlockPos canopyOrigin, Vec3i size) {
        int topY = canopyOrigin.getY() + size.getY() - 1;
        int bottomY = Math.max(canopyOrigin.getY(), topY - 3);
        BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();

        for (int y = bottomY; y <= topY; y++) {
            for (int dx = -3; dx <= 3; dx++) {
                for (int dz = -3; dz <= 3; dz++) {
                    mutable.set(origin.getX() + dx, y, origin.getZ() + dz);
                    if (!level.getBlockState(mutable).isAir()) {
                        return false;
                    }
                }
            }
        }

        return true;
    }

    private static void replaceWaterRing(WorldGenLevel level, BlockPos origin, int trunkWidth, int trunkHeight) {
        int minDx = -1;
        int maxDx = trunkWidth;
        int minDz = -1;
        int maxDz = trunkWidth;
        int minY = origin.getY();
        int maxY = origin.getY() + Math.max(0, trunkHeight - 1);

        for (int dz = minDz; dz <= maxDz; dz++) {
            for (int dx = minDx; dx <= maxDx; dx++) {
                boolean trunkBlock = dx >= 0 && dx < trunkWidth && dz >= 0 && dz < trunkWidth;
                if (trunkBlock) {
                    continue;
                }

                int topWaterY = Integer.MIN_VALUE;
                for (int y = maxY; y >= minY; y--) {
                    BlockPos pos = new BlockPos(origin.getX() + dx, y, origin.getZ() + dz);
                    if (isWater(level, pos)) {
                        topWaterY = y;
                        break;
                    }
                }

                if (topWaterY == Integer.MIN_VALUE) {
                    continue;
                }

                for (int y = topWaterY; y >= minY; y--) {
                    BlockPos pos = new BlockPos(origin.getX() + dx, y, origin.getZ() + dz);
                    if (!isWater(level, pos)) {
                        continue;
                    }

                    if (hasOutsideHorizontalWaterNeighbor(level, origin, pos, trunkWidth)) {
                        int patternIndex = topWaterY - y;
                        BlockState replacement = CORAL_PATTERN[Math.floorMod(patternIndex, CORAL_PATTERN.length)];
                        level.setBlock(pos, replacement, 19);
                    } else {
                        level.setBlock(pos, Blocks.MOSS_BLOCK.defaultBlockState(), 19);
                    }
                }
            }
        }
    }

    private static boolean hasOutsideHorizontalWaterNeighbor(WorldGenLevel level, BlockPos origin, BlockPos pos, int trunkWidth) {
        int minX = origin.getX() - 1;
        int maxX = origin.getX() + trunkWidth;
        int minZ = origin.getZ() - 1;
        int maxZ = origin.getZ() + trunkWidth;

        for (Direction direction : Direction.Plane.HORIZONTAL) {
            BlockPos neighbor = pos.relative(direction);
            boolean outsideArea = neighbor.getX() < minX
                    || neighbor.getX() > maxX
                    || neighbor.getZ() < minZ
                    || neighbor.getZ() > maxZ;

            if (outsideArea && isWater(level, neighbor)) {
                return true;
            }
        }

        return false;
    }

    private static boolean isWater(LevelReader level, BlockPos pos) {
        return level.getFluidState(pos).is(FluidTags.WATER);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static BlockState copyAllSharedProperties(BlockState from, BlockState to) {
        for (Property property : from.getProperties()) {
            if (to.hasProperty(property)) {
                Comparable value = from.getValue(property);
                to = to.setValue(property, value);
            }
        }

        return to;
    }

    private static boolean isTemplateLog(BlockState state) {
        return state.is(BlockTags.LOGS)
                || state.is(Blocks.CRIMSON_STEM)
                || state.is(Blocks.WARPED_STEM);
    }

    private static boolean isTemplateAir(BlockState state) {
        return state.isAir() || state.is(Blocks.STRUCTURE_VOID);
    }

    private static boolean isWeepingVine(BlockState state) {
        return state.is(Blocks.WEEPING_VINES) || state.is(Blocks.WEEPING_VINES_PLANT);
    }

    private static class ProviderReplacingTemplateProcessor extends StructureProcessor {
        private final RainforestTreeFeatureConfig config;
        private final RandomSource random;
        private final Set<BlockPos> logPositions;
        private final Set<BlockPos> leafPositions;

        private ProviderReplacingTemplateProcessor(
                RainforestTreeFeatureConfig config,
                RandomSource random,
                Set<BlockPos> logPositions,
                Set<BlockPos> leafPositions
        ) {
            this.config = config;
            this.random = random;
            this.logPositions = logPositions;
            this.leafPositions = leafPositions;
        }

        @Override
        protected StructureProcessorType<?> getType() {
            return StructureProcessorType.NOP;
        }

        @Override
        public @Nullable StructureTemplate.StructureBlockInfo process(
                LevelReader level,
                BlockPos offset,
                BlockPos pos,
                StructureTemplate.StructureBlockInfo originalInfo,
                StructureTemplate.StructureBlockInfo transformedInfo,
                StructurePlaceSettings settings,
                @Nullable StructureTemplate template
        ) {
            BlockState templateState = transformedInfo.state();
            BlockPos worldPos = transformedInfo.pos();

            if (isTemplateAir(templateState)) {
                return null;
            }

            if (isWeepingVine(templateState)) {
                if (!this.config.includeWeepingVines()) {
                    return null;
                } else {
                    if (templateState.hasProperty(WeepingVinesBlock.AGE)) {
                        templateState = templateState.setValue(WeepingVinesBlock.AGE, 25);
                    }
                    return new StructureTemplate.StructureBlockInfo(worldPos, templateState, transformedInfo.nbt());
                }
            }

            if (templateState.is(Blocks.SHROOMLIGHT)) {
                return transformedInfo;
            }

            BlockState newState;
            if (isTemplateLog(templateState)) {
                newState = this.config.logProvider().getState(this.random, worldPos);
                newState = copyAllSharedProperties(templateState, newState);
                this.logPositions.add(worldPos.immutable());
            } else {
                newState = this.config.leavesProvider().getState(this.random, worldPos);
                newState = copyAllSharedProperties(templateState, newState);

                if (newState.hasProperty(LeavesBlock.PERSISTENT)) {
                    newState = newState.setValue(LeavesBlock.PERSISTENT, false);
                }

                this.leafPositions.add(worldPos.immutable());
            }

            return new StructureTemplate.StructureBlockInfo(worldPos, newState, transformedInfo.nbt());
        }
    }
}

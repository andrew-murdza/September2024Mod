package net.amurdza.examplemod.worldgen.feature.features;

import com.mojang.serialization.Codec;
import net.amurdza.examplemod.worldgen.feature.configs.RainforestTreeFeatureConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.treedecorators.TreeDecorator;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public class RainforestTreeFeature extends Feature<RainforestTreeFeatureConfig> {

    public RainforestTreeFeature(Codec<RainforestTreeFeatureConfig> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<RainforestTreeFeatureConfig> context) {
        WorldGenLevel level = context.level();
        RandomSource random = context.random();
        BlockPos origin = context.origin();
        RainforestTreeFeatureConfig config = context.config();

        int trunkHeight = config.height().sample(random);
        if (trunkHeight <= 0) {
            return false;
        }

        // Ground check:
        // [0,1] x {y0 - 1} x [0,1] must all satisfy valid_ground.
        for (int dx = 0; dx <= 1; dx++) {
            for (int dz = 0; dz <= 1; dz++) {
                BlockPos groundPos = origin.offset(dx, -1, dz);
                if (!config.validGround().test(level, groundPos)) {
                    return false;
                }
            }
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

        // Require 2n x m x 2n.
        if (sizeX <= 0 || sizeZ <= 0 || sizeX != sizeZ || sizeX % 2 != 0) {
            return false;
        }

        int n = sizeX / 2;

        // Template local 0..2n-1 maps to world 1-n..n.
        // Bottom of canopy is at y0 + trunkHeight.
        BlockPos canopyOrigin = origin.offset(1 - n, trunkHeight, 1 - n);

        Set<BlockPos> logPositions = new HashSet<>();
        Set<BlockPos> leafPositions = new HashSet<>();
        // Place trunk here
        for (int y = 0; y < trunkHeight; y++) {
            for (int dx = 0; dx <= 1; dx++) {
                for (int dz = 0; dz <= 1; dz++) {
                    BlockPos trunkPos = origin.offset(dx, y, dz);
                    BlockState trunkState = config.logProvider().getState(random, trunkPos);

                    level.setBlock(trunkPos, trunkState, 19);
                    logPositions.add(trunkPos.immutable());
                }
            }
        }

        StructurePlaceSettings settings = new StructurePlaceSettings()
                .setIgnoreEntities(true);

        List<StructureTemplate.StructureBlockInfo> templateLogs =
                template.filterBlocks(canopyOrigin, settings, Blocks.OAK_LOG);

        List<StructureTemplate.StructureBlockInfo> templateLeaves =
                template.filterBlocks(canopyOrigin, settings, Blocks.OAK_LEAVES);

        for (StructureTemplate.StructureBlockInfo info : templateLogs) {
            BlockPos worldPos = info.pos();
            BlockState templateState = info.state();

            BlockState newState = config.logProvider().getState(random, worldPos);
            newState = copyAllSharedProperties(templateState, newState);

            level.setBlock(worldPos, newState, 19);
            logPositions.add(worldPos.immutable());
        }

        for (StructureTemplate.StructureBlockInfo info : templateLeaves) {
            BlockPos worldPos = info.pos();
            BlockState templateState = info.state();

            BlockState newState = config.leavesProvider().getState(random, worldPos);
            newState = copyAllSharedProperties(templateState, newState);

            if (newState.hasProperty(LeavesBlock.PERSISTENT)) {
                newState = newState.setValue(LeavesBlock.PERSISTENT, false);
            }

            level.setBlock(worldPos, newState, 19);
            leafPositions.add(worldPos.immutable());
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
}
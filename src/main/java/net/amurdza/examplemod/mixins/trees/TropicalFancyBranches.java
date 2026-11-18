package net.amurdza.examplemod.mixins.trees;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.amurdza.examplemod.util.GameplayDimensions;
import net.amurdza.examplemod.worldgen.TreeLogConnections;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.LevelSimulatedReader;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.TreeFeature;
import net.minecraft.world.level.levelgen.feature.configurations.TreeConfiguration;
import net.minecraft.world.level.levelgen.feature.foliageplacers.FoliagePlacer;
import net.minecraft.world.level.levelgen.feature.trunkplacers.FancyTrunkPlacer;
import net.minecraft.world.level.levelgen.feature.trunkplacers.TrunkPlacer;
import org.spongepowered.asm.mixin.Mixin;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.function.BiConsumer;

/** Connect the final native skeleton once, keeping the acacia rule for redundant lower logs. */
@Mixin(FancyTrunkPlacer.class)
public abstract class TropicalFancyBranches extends TrunkPlacer {
    protected TropicalFancyBranches(int height, int randomHeightA, int randomHeightB) {
        super(height, randomHeightA, randomHeightB);
    }

    @WrapMethod(method = "placeTrunk")
    private List<FoliagePlacer.FoliageAttachment> aoemod$connectNativeBranches(LevelSimulatedReader level,
            BiConsumer<BlockPos, BlockState> setter, RandomSource random, int height, BlockPos origin,
            TreeConfiguration config, Operation<List<FoliagePlacer.FoliageAttachment>> original) {
        if (!(level instanceof LevelReader reader) || !GameplayDimensions.is(reader, GameplayDimensions.TROPICAL)) {
            return original.call(level, setter, random, height, origin, config);
        }
        var pending = new LinkedHashMap<BlockPos, BlockState>();
        BiConsumer<BlockPos, BlockState> stage = (pos, state) -> pending.put(pos.immutable(),
                state.trySetValue(RotatedPillarBlock.AXIS, Direction.Axis.Y));
        var foliage = original.call(level, stage, random, height, origin, config);
        var logs = new LinkedHashSet<BlockPos>();
        pending.forEach((pos, state) -> { if (state.is(BlockTags.LOGS)) logs.add(pos); });
        for (BlockPos bridge : TreeLogConnections.connectors(logs, pos -> TreeFeature.validTreePos(level, pos))) {
            if (placeLog(level, stage, random, bridge, config)) logs.add(bridge);
        }
        var retained = new LinkedHashSet<BlockPos>();
        // Preserve the upright trunk and the tips that carry foliage clusters.
        logs.stream().filter(pos -> pos.getX() == origin.getX() && pos.getZ() == origin.getZ()).forEach(retained::add);
        foliage.forEach(attachment -> retained.add(attachment.pos()));
        var finalLogs = TreeLogConnections.withoutRedundantLowerLogs(logs, retained);
        if (finalLogs.stream().anyMatch(pos -> !TreeLogConnections.connected(finalLogs, origin, pos))) return List.of();
        pending.forEach((pos, state) -> {
            if (!state.is(BlockTags.LOGS) || finalLogs.contains(pos)) setter.accept(pos, state);
        });
        return foliage;
    }
}

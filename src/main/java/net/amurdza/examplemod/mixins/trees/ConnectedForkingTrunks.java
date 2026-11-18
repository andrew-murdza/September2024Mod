package net.amurdza.examplemod.mixins.trees;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.amurdza.examplemod.worldgen.TreeLogConnections;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelSimulatedReader;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.TreeFeature;
import net.minecraft.world.level.levelgen.feature.configurations.TreeConfiguration;
import net.minecraft.world.level.levelgen.feature.foliageplacers.FoliagePlacer;
import net.minecraft.world.level.levelgen.feature.trunkplacers.ForkingTrunkPlacer;
import net.minecraft.world.level.levelgen.feature.trunkplacers.TrunkPlacer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.function.BiConsumer;

/** Keep acacias face-connected without low bends or redundant hanging logs. */
@Mixin(ForkingTrunkPlacer.class)
public abstract class ConnectedForkingTrunks extends TrunkPlacer {
    protected ConnectedForkingTrunks(int height, int a, int b) { super(height, a, b); }

    @ModifyExpressionValue(method = "placeTrunk", at = @At(value = "INVOKE", target =
            "Lnet/minecraft/util/RandomSource;nextInt(I)I", ordinal = 0), require = 1)
    private int aoemod$straightenOneBlockBase(int bendRoll, @Local(argsOnly = true) int height) {
        // Vanilla starts bending at height - roll - 1. Move that start beyond
        // the last trunk block when it would leave only a one-block stump.
        return height - bendRoll - 1 <= 1 ? -1 : bendRoll;
    }

    @ModifyExpressionValue(method = "placeTrunk", at = @At(value = "INVOKE", target =
            "Lnet/minecraft/util/RandomSource;nextInt(I)I", ordinal = 2), require = 1)
    private int aoemod$skipOneBlockSideFork(int forkRoll, @Local(argsOnly = true) int height,
                                          @Local(ordinal = 1) int bendStart) {
        // The secondary fork starts at bendStart - roll - 1. A low fork
        // must not recreate the one-block stump beside an otherwise valid trunk.
        return bendStart - forkRoll - 1 <= 1 ? bendStart - height - 1 : forkRoll;
    }

    @WrapMethod(method = "placeTrunk")
    private List<FoliagePlacer.FoliageAttachment> aoemod$connectNativeForks(LevelSimulatedReader level,
            BiConsumer<BlockPos, BlockState> setter, RandomSource random, int height, BlockPos origin,
            TreeConfiguration config, Operation<List<FoliagePlacer.FoliageAttachment>> original) {
        var pending = new LinkedHashMap<BlockPos, BlockState>();
        // Native forking limbs do not cross; stage their writes until the final
        // log graph is known so removed logs never reach TreeFeature's decorators.
        var foliage = original.call(level, (BiConsumer<BlockPos, BlockState>) (pos, state) -> {
            pending.put(pos.immutable(), state);
        }, random, height, origin, config);
        var logs = new LinkedHashSet<BlockPos>();
        pending.forEach((pos, state) -> { if (state.is(BlockTags.LOGS)) logs.add(pos); });
        for (BlockPos bridge : TreeLogConnections.connectors(logs, pos -> TreeFeature.validTreePos(level, pos),
                pos -> level.isStateAtPosition(pos, state -> state.is(BlockTags.LEAVES))
                        || foliage.stream().anyMatch(attachment -> {
                            BlockPos crown = attachment.pos();
                            // Acacia's two lower canopy layers surround this attachment.
                            return pos.getY() >= crown.getY() - 1 && pos.getY() <= crown.getY()
                                    && Math.abs(pos.getX() - crown.getX()) <= 2
                                    && Math.abs(pos.getZ() - crown.getZ()) <= 2;
                        }))) {
            if (placeLog(level, (pos, state) -> pending.put(pos.immutable(), state), random, bridge, config)) {
                logs.add(bridge);
            }
        }
        var retained = new LinkedHashSet<BlockPos>();
        retained.add(origin);
        foliage.forEach(attachment -> retained.add(attachment.pos().below()));
        var finalLogs = TreeLogConnections.withoutRedundantLowerLogs(logs, retained);
        pending.forEach((pos, state) -> {
            if (!state.is(BlockTags.LOGS) || finalLogs.contains(pos)) setter.accept(pos, state);
        });
        return foliage;
    }
}

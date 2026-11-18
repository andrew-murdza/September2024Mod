package net.amurdza.examplemod.mixins.trees;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.amurdza.examplemod.util.GameplayDimensions;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.feature.treedecorators.TreeDecorator;
import net.minecraft.world.level.levelgen.feature.treedecorators.TrunkVineDecorator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Mixin(TrunkVineDecorator.class)
public class TropicalJungleTrunkVines {
    @Redirect(method = "place", at = @At(value = "INVOKE", target =
            "Lnet/minecraft/world/level/levelgen/feature/treedecorators/TreeDecorator$Context;logs()Lit/unimi/dsi/fastutil/objects/ObjectArrayList;"), require = 1)
    private ObjectArrayList<BlockPos> aoemod$keepGiantJungleVinesOnMainTrunk(TreeDecorator.Context context) {
        ObjectArrayList<BlockPos> logs = context.logs();
        if (logs.isEmpty() || !(context.level() instanceof LevelReader level)
                || !GameplayDimensions.is(level, GameplayDimensions.TROPICAL)) return logs;
        int baseY = logs.stream().mapToInt(BlockPos::getY).min().orElseThrow();
        List<BlockPos> base = logs.stream().filter(pos -> pos.getY() == baseY).toList();
        if (base.size() != 4 || base.stream().anyMatch(pos -> !level.getBlockState(pos).is(Blocks.JUNGLE_LOG))) return logs;
        int minX = base.stream().mapToInt(BlockPos::getX).min().orElseThrow();
        int minZ = base.stream().mapToInt(BlockPos::getZ).min().orElseThrow();
        if (base.stream().anyMatch(pos -> pos.getX() < minX || pos.getX() > minX + 1
                || pos.getZ() < minZ || pos.getZ() > minZ + 1)) return logs;
        Set<Long> columns = base.stream().map(pos -> ((long) pos.getX() << 32) ^ (pos.getZ() & 0xffffffffL))
                .collect(Collectors.toSet());
        return new ObjectArrayList<>(logs.stream()
                .filter(pos -> columns.contains(((long) pos.getX() << 32) ^ (pos.getZ() & 0xffffffffL))).toList());
    }
}

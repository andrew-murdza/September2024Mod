package net.amurdza.examplemod.mixins.trees;

import net.amurdza.examplemod.registry.ModConfiguredFeatures;
import net.amurdza.examplemod.util.ModTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.AzaleaBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Optional;

@Mixin(AzaleaBlock.class)
public class AzaleaRainforestTree {
    @Inject(method = "performBonemeal", at = @At("HEAD"), cancellable = true)
    private void aoemod$growRainforestAzaleaTree(ServerLevel level, RandomSource random, BlockPos pos, BlockState state, CallbackInfo ci) {
        if (!level.getBiome(pos).is(ModTags.Biomes.rainforestBiomes)) {
            return;
        }

        ci.cancel();

        Optional<BlockPos> base = aoemod$findTwoByTwoAzaleas(level, pos);
        if (base.isEmpty()) {
            return;
        }

        aoemod$placeTwoByTwoFeature(level, base.get(), random, ModConfiguredFeatures.AZALEA_LARGE_TREE_FROM_SAPLING);
    }

    @Unique
    private static Optional<BlockPos> aoemod$findTwoByTwoAzaleas(ServerLevel level, BlockPos pos) {
        for (int dx = 0; dx >= -1; dx--) {
            for (int dz = 0; dz >= -1; dz--) {
                BlockPos base = pos.offset(dx, 0, dz);
                if (aoemod$isTwoByTwoAzaleas(level, base)) {
                    return Optional.of(base);
                }
            }
        }

        return Optional.empty();
    }

    @Unique
    private static boolean aoemod$isTwoByTwoAzaleas(ServerLevel level, BlockPos base) {
        for (int dx = 0; dx <= 1; dx++) {
            for (int dz = 0; dz <= 1; dz++) {
                Block block = level.getBlockState(base.offset(dx, 0, dz)).getBlock();
                if (block != Blocks.AZALEA && block != Blocks.FLOWERING_AZALEA) {
                    return false;
                }
            }
        }

        return true;
    }

    @Unique
    private static void aoemod$placeTwoByTwoFeature(
            ServerLevel level,
            BlockPos base,
            RandomSource random,
            ResourceKey<ConfiguredFeature<?, ?>> featureKey
    ) {
        BlockState[] oldStates = new BlockState[]{
                level.getBlockState(base),
                level.getBlockState(base.east()),
                level.getBlockState(base.south()),
                level.getBlockState(base.east().south())
        };

        level.setBlock(base, Blocks.AIR.defaultBlockState(), 4);
        level.setBlock(base.east(), Blocks.AIR.defaultBlockState(), 4);
        level.setBlock(base.south(), Blocks.AIR.defaultBlockState(), 4);
        level.setBlock(base.east().south(), Blocks.AIR.defaultBlockState(), 4);

        ConfiguredFeature<?, ?> tree = level.registryAccess()
                .registryOrThrow(Registries.CONFIGURED_FEATURE)
                .get(featureKey);

        boolean placed = tree != null
                && tree.place(level, level.getChunkSource().getGenerator(), random, base);

        if (!placed) {
            level.setBlock(base, oldStates[0], 4);
            level.setBlock(base.east(), oldStates[1], 4);
            level.setBlock(base.south(), oldStates[2], 4);
            level.setBlock(base.east().south(), oldStates[3], 4);
        }
    }
}

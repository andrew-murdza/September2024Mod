package net.amurdza.examplemod.mixins.othermods.biomeswevegone;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;
import net.potionstudios.biomeswevegone.world.level.levelgen.feature.configured.BWGOverworldTreeConfiguredFeatures;
import org.spongepowered.asm.mixin.Debug;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Debug(export = true)
@Mixin(BWGOverworldTreeConfiguredFeatures.class)
public class ReplacementTrees {
    @ModifyArg(
            method = "lambda$static$0",
            at = @At(
                    value = "INVOKE",
                    target = "Ldev/corgitaco/ohthetreesyoullgrow/world/level/levelgen/feature/configurations/TreeFromStructureNBTConfig;<init>(Lnet/minecraft/resources/ResourceLocation;Lnet/minecraft/resources/ResourceLocation;Lnet/minecraft/util/valueproviders/IntProvider;Lnet/minecraft/world/level/levelgen/feature/stateproviders/BlockStateProvider;Lnet/minecraft/world/level/levelgen/feature/stateproviders/BlockStateProvider;Lnet/minecraft/world/level/block/Block;Lnet/minecraft/world/level/block/Block;Lnet/minecraft/tags/TagKey;I)V"
            ),
            index = 3 // the Blocks.OAK_LOG parameter position in that ctor
    )
    private static BlockStateProvider replaceLog(BlockStateProvider logProvider) {
        return BlockStateProvider.simple(Blocks.ACACIA_LOG);
    }
    @ModifyArg(
            method = "lambda$static$0",
            at = @At(
                    value = "INVOKE",
                    target = "Ldev/corgitaco/ohthetreesyoullgrow/world/level/levelgen/feature/configurations/TreeFromStructureNBTConfig;<init>(Lnet/minecraft/resources/ResourceLocation;Lnet/minecraft/resources/ResourceLocation;Lnet/minecraft/util/valueproviders/IntProvider;Lnet/minecraft/world/level/levelgen/feature/stateproviders/BlockStateProvider;Lnet/minecraft/world/level/levelgen/feature/stateproviders/BlockStateProvider;Lnet/minecraft/world/level/block/Block;Lnet/minecraft/world/level/block/Block;Lnet/minecraft/tags/TagKey;I)V"
            ),
            index = 4 // the Blocks.OAK_LOG parameter position in that ctor
    )
    private static BlockStateProvider replaceLeaves1(BlockStateProvider logProvider) {
        return BlockStateProvider.simple(Blocks.ACACIA_LEAVES);
    }
    @ModifyArg(
            method = "lambda$static$1",
            at = @At(
                    value = "INVOKE",
                    target = "Ldev/corgitaco/ohthetreesyoullgrow/world/level/levelgen/feature/configurations/TreeFromStructureNBTConfig;<init>(Lnet/minecraft/resources/ResourceLocation;Lnet/minecraft/resources/ResourceLocation;Lnet/minecraft/util/valueproviders/IntProvider;Lnet/minecraft/world/level/levelgen/feature/stateproviders/BlockStateProvider;Lnet/minecraft/world/level/levelgen/feature/stateproviders/BlockStateProvider;Lnet/minecraft/world/level/block/Block;Lnet/minecraft/world/level/block/Block;Lnet/minecraft/tags/TagKey;I)V"
            ),
            index = 3 // the Blocks.OAK_LOG parameter position in that ctor
    )
    private static BlockStateProvider replaceLog2(BlockStateProvider logProvider) {
        return BlockStateProvider.simple(Blocks.ACACIA_LOG);
    }
    @ModifyArg(
            method = "lambda$static$1",
            at = @At(
                    value = "INVOKE",
                    target = "Ldev/corgitaco/ohthetreesyoullgrow/world/level/levelgen/feature/configurations/TreeFromStructureNBTConfig;<init>(Lnet/minecraft/resources/ResourceLocation;Lnet/minecraft/resources/ResourceLocation;Lnet/minecraft/util/valueproviders/IntProvider;Lnet/minecraft/world/level/levelgen/feature/stateproviders/BlockStateProvider;Lnet/minecraft/world/level/levelgen/feature/stateproviders/BlockStateProvider;Lnet/minecraft/world/level/block/Block;Lnet/minecraft/world/level/block/Block;Lnet/minecraft/tags/TagKey;I)V"
            ),
            index = 4 // the Blocks.OAK_LOG parameter position in that ctor
    )
    private static BlockStateProvider replaceLeaves2(BlockStateProvider logProvider) {
        return BlockStateProvider.simple(Blocks.ACACIA_LEAVES);
    }
}

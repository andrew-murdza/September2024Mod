package net.amurdza.examplemod.mixins.worldgen;

import net.amurdza.examplemod.Config;
import net.amurdza.examplemod.block.ModBlocks;
import net.amurdza.examplemod.util.RandomCollection;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.levelgen.feature.treedecorators.TreeDecorator;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(TreeDecorator.Context.class)
public class GrapeGenerator {
    @Shadow @Final private RandomSource random;

    @Redirect(method = "placeVine",at=@At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/Block;defaultBlockState()Lnet/minecraft/world/level/block/state/BlockState;"))
    private BlockState hi(Block instance){
        RandomCollection<BlockState> randomCollection=new RandomCollection<>();
        randomCollection.add(1, ModBlocks.GRAPE_VINE.get().defaultBlockState().setValue(BlockStateProperties.BERRIES,true));
        randomCollection.add(Config.GLOW_LICHEN_TRUNK_CHANCE, Blocks.GLOW_LICHEN.defaultBlockState());
        randomCollection.add(Config.VINE_TRUNK_CHANCE, Blocks.VINE.defaultBlockState());
        return randomCollection.next(random);
    }
}

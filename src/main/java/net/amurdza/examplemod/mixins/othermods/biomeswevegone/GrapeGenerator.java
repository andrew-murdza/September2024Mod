package net.amurdza.examplemod.mixins.othermods.biomeswevegone;

import dev.corgitaco.ohthetreesyoullgrow.world.level.levelgen.feature.configurations.treedecorators.TYGTrunkVineDecorator;
import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.Config;
import net.amurdza.examplemod.block.ModBlocks;
import net.amurdza.examplemod.util.RandomCollection;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.VineBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.levelgen.feature.treedecorators.TreeDecorator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(TYGTrunkVineDecorator.class)
public class GrapeGenerator {
    @Redirect(method = "placeVine",at= @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/VineBlock;defaultBlockState()Lnet/minecraft/world/level/block/state/BlockState;"))
    private BlockState hi(VineBlock instance, TreeDecorator.Context context, BlockPos blockPos, BooleanProperty booleanProperty){
        RandomCollection<BlockState> randomCollection=new RandomCollection<>();
        randomCollection.add(1,ModBlocks.GRAPE_VINE.get().defaultBlockState().setValue(BlockStateProperties.BERRIES,true));
        randomCollection.add(Config.GLOW_LICHEN_TRUNK_CHANCE, Blocks.GLOW_LICHEN.defaultBlockState());
        randomCollection.add(Config.VINE_TRUNK_CHANCE, Blocks.VINE.defaultBlockState());
        return randomCollection.next(context.random());
    }
}

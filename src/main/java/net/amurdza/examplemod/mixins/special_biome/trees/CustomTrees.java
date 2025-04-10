package net.amurdza.examplemod.mixins.special_biome.trees;

import net.amurdza.examplemod.util.Helper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.features.TreeFeatures;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.grower.AbstractTreeGrower;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.HashMap;

@Mixin(SaplingBlock.class)
public abstract class CustomTrees extends Block {
    @Shadow @Final private AbstractTreeGrower treeGrower;

    @Unique
    private static HashMap<Block, ResourceKey<ConfiguredFeature<?,?>>> map=new HashMap<>();
    static {
        map.put(Blocks.OAK_SAPLING, TreeFeatures.OAK_BEES_005);
    }


    public CustomTrees(Properties pProperties) {
        super(pProperties);
    }

    @Redirect(method = "randomTick",at= @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/SaplingBlock;advanceTree(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/util/RandomSource;)V"))
    private void hi(SaplingBlock instance, ServerLevel pLevel, BlockPos pPos, BlockState pState, RandomSource pRandom){
        if(!Helper.isSpecialBiome(pLevel,pPos)||defaultBlockState().getBlock()!=Blocks.OAK_SAPLING){
            instance.advanceTree(pLevel,pPos,pState,pRandom);
        }
        else{
            boolean flag=true;
            for(int i=0;i<2;i++){
                for(int j=0;j<2;j++){
                    Block block=pLevel.getBlockState(pPos.offset(i,0,j)).getBlock();
                    if(!block.defaultBlockState().equals(defaultBlockState())){
                        flag=false;
                    }
                }
            }
            if(flag&&map.containsKey(defaultBlockState().getBlock())){
                BlockPos pos=pPos.offset(-7,0,-7);
                ConfiguredFeature<?,?> tree=pLevel.registryAccess().registryOrThrow(Registries.CONFIGURED_FEATURE)
                        .getOrThrow(map.get(defaultBlockState().getBlock()));
                tree.place(pLevel,pLevel.getChunkSource().getGenerator(),pRandom,pos);
            }
        }
    }
}

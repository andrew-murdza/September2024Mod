package net.amurdza.examplemod.mixins;

import net.amurdza.examplemod.Config;
import net.amurdza.examplemod.Helper;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChorusFlowerBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ChorusFlowerBlock.class)
public class ChorusFlowerGrowth {
    @ModifyArg(method = "randomTick", at = @At(value = "INVOKE",target = "Lnet/minecraft/world/level/block/ChorusFlowerBlock;placeGrownFlower(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;I)V",ordinal = 1),index = 2)
    public int grow(Level world, BlockPos pos, int age){
        if(Helper.isSpecialBiome(world,pos)){
            return Helper.withChance(world, Config.PLACE_CHORUS_FLOWER_CHANCE)?age-1:age;
        }
        return age;
    }
    @Redirect(method = "randomTick",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/util/RandomSource;nextInt(I)I",ordinal = 1))
    private int nextInt(RandomSource random, int n, BlockState state, ServerLevel world, BlockPos pos, RandomSource random1){
        if(Helper.isSpecialBiome(world,pos)){
            return Helper.withChanceToInt(world,Config.CHORUS_FLOWER_GROW_CHANCE);
        }
        return random.nextInt(n);
    }

    @Redirect(method = "randomTick",at=@At(value = "INVOKE",target = "Lnet/minecraft/world/level/block/state/BlockState;is(Lnet/minecraft/world/level/block/Block;)Z",ordinal = 0))
    private boolean hi(BlockState instance, Block block){
        return aOEMod1_20_1V2$isSoil(instance,block);
    }
    @Redirect(method = "randomTick",at=@At(value = "INVOKE",target = "Lnet/minecraft/world/level/block/state/BlockState;is(Lnet/minecraft/world/level/block/Block;)Z",ordinal = 3))
    private boolean hi1(BlockState instance, Block block){
        return aOEMod1_20_1V2$isSoil(instance,block);
    }

    @Redirect(method = "canSurvive",at=@At(value = "INVOKE",target = "Lnet/minecraft/world/level/block/state/BlockState;is(Lnet/minecraft/world/level/block/Block;)Z",ordinal = 1))
    private boolean hi2(BlockState instance, Block block){
        return aOEMod1_20_1V2$isSoil(instance,block);
    }

    @Unique
    private boolean aOEMod1_20_1V2$isSoil(BlockState instance, Block block){
        return instance.is(block)||instance.is(Blocks.SCULK)||instance.is(Blocks.SCULK_CATALYST);
    }
}
